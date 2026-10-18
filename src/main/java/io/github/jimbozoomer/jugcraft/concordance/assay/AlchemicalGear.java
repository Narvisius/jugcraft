package io.github.jimbozoomer.jugcraft.concordance.assay;

import com.mojang.serialization.MapCodec;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketDropRule;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.trinket.Wayfaring;
import io.github.jimbozoomer.jugcraft.mixin.AlchemicalItemAccess;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Equipment paid from the existing personal Assay ledger, independent of spellcasting and Focus. */
public final class AlchemicalGear {
    public static final int INTERVAL = 20, REPAIR = 4, TARGETS = 8, SCAN_LIMIT = 32;
    public static final double RADIUS = 6.0;
    public static Item STONE, TALISMAN, BAND;
    public static DataComponentType<Unit> ACTIVE;
    public static MenuType<TransmutationMenu> MENU;
    private AlchemicalGear() { }

    public static void register() {
        ACTIVE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("alchemy_active"),
                DataComponentType.<Unit>builder().persistent(MapCodec.unitCodec(Unit.INSTANCE)).networkSynchronized(Unit.STREAM_CODEC).build());
        MENU = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("transmutation"),
                new MenuType<>(TransmutationMenu::new, FeatureFlags.DEFAULT_FLAGS));
        STONE = item("philosophers_stone");
        TALISMAN = item("repair_talisman");
        BAND = item("black_hole_band");
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(STONE); entries.accept(TALISMAN); entries.accept(BAND);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers())
                if (player.tickCount % INTERVAL == 0) pulse(player);
        });
    }

    private static Item item(String id) {
        var key = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
        return Registry.register(BuiltInRegistries.ITEM, key, new GearItem(id,
                new Item.Properties().setId(key).stacksTo(1).rarity(Rarity.RARE)));
    }

    public static boolean ready(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && Assaying.enabled()
                && Assaying.problems(player.level()).isEmpty();
    }

    public static boolean active(Player player, Item item) {
        return active(player.getMainHandItem(), item) || active(player.getOffhandItem(), item)
                || Wayfaring.worn(player, stack -> active(stack, item)) != null;
    }

    private static boolean active(ItemStack stack, Item item) {
        return stack.is(item) && stack.has(ACTIVE);
    }

    /** Once per player, not once per duplicate item. No scans when disabled, unequipped or out of fuel. */
    public static void pulse(ServerPlayer player) {
        boolean repair = active(player, TALISMAN), gather = active(player, BAND);
        if ((!repair && !gather) || !ready(player) || AlchemyPower.available(player) == 0) return;
        if (repair) repair(player);
        if (gather && AlchemyPower.available(player) > 0) gather(player);
    }

    private static void repair(ServerPlayer player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isDamageableItem() || !stack.isDamaged() || !stack.has(DataComponents.REPAIRABLE)
                    || stack.has(DataComponents.UNBREAKABLE)) continue;
            int points = (int) Math.min(Math.min(REPAIR, stack.getDamageValue()), AlchemyPower.available(player));
            if (points <= 0) return;
            stack.setDamageValue(stack.getDamageValue() - points);
            AlchemyPower.pay(player, points);
            player.getInventory().setChanged();
            return;
        }
    }

    private static boolean hasRoom(Player player, ItemStack stack) {
        for (int i = 0; i < 36; i++) {
            ItemStack in = player.getInventory().getItem(i);
            if (in.isEmpty() || ItemStack.isSameItemSameComponents(in, stack) && in.getCount() < in.getMaxStackSize()) return true;
        }
        return false;
    }

    private static void gather(ServerPlayer player) {
        var level = player.level();
        // Bound even the intermediate collection; expensive visibility/protection checks run on at most 32 drops.
        var drops = new ArrayList<ItemEntity>();
        level.getEntities(EntityTypeTest.forClass(ItemEntity.class), player.getBoundingBox().inflate(RADIUS),
                item -> true, drops, SCAN_LIMIT);
        int moved = 0;
        for (ItemEntity drop : drops) {
            if (moved >= TARGETS || AlchemyPower.available(player) == 0) break;
            UUID target = ((AlchemicalItemAccess) drop).jugcraft$pickupTarget();
            if (!drop.isAlive() || drop.hasPickUpDelay() || drop.distanceToSqr(player) > RADIUS * RADIUS
                    || target != null && !target.equals(player.getUUID())
                    || drop.getOwner() != null && !drop.getOwner().getUUID().equals(player.getUUID())
                    || !hasRoom(player, drop.getItem()) || !Authority.mayChange(level, player, drop.blockPosition())) continue;
            Vec3 toward = player.position().add(0, 0.6, 0).subtract(drop.position());
            if (toward.lengthSqr() < 0.4 || level.clip(new ClipContext(drop.position().add(0, 0.1, 0),
                    player.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) continue;
            drop.setDeltaMovement(toward.normalize().scale(0.55));
            drop.syncVelocity = true;
            AlchemyPower.pay(player, 1);
            moved++;
        }
    }

    private static final class GearItem extends Item implements TrinketCallback {
        private final String id;
        GearItem(String id, Properties properties) { super(properties); this.id = id; }

        @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (player instanceof ServerPlayer server && RateGate.allow(server, "alchemical_gear", 4)) {
                if (!ready(server)) {
                    server.sendOverlayMessage(Component.translatable(!Assaying.enabled() ? "message.jugcraft.concordance.disabled"
                            : "message.jugcraft.alchemy.unavailable"));
                    return InteractionResult.FAIL;
                }
                if (this == STONE) {
                    if(server.isShiftKeyDown()) PhilosopherTransforms.crafting(server);
                    else server.openMenu(new SimpleMenuProvider((id, inv, p) -> new AlchemyPatternMenu(id, inv, () -> server.getInventory().contains(s -> s.is(STONE)) || server.getOffhandItem().is(STONE), ItemStack.EMPTY), Component.translatable("screen.jugcraft.alchemy.title")));
                }
                else {
                    ItemStack stack = player.getItemInHand(hand);
                    boolean on = !stack.has(ACTIVE);
                    if (on) stack.set(ACTIVE, Unit.INSTANCE); else stack.remove(ACTIVE);
                    server.sendOverlayMessage(Component.translatable("message.jugcraft.alchemy.toggle", stack.getHoverName(),
                            Component.translatable("screen.jugcraft.alchemy." + (on ? "on" : "off")), AlchemyPower.available(player)));
                }
            }
            return InteractionResult.SUCCESS;
        }

        @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext c) {
            if(this!=STONE||!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.PASS;
            if(!ready(p)||!RateGate.allow(p,"stone_transform",6))return InteractionResult.FAIL;
            if(p.isShiftKeyDown()){
                int charge=(AlchemyArtifacts.mode(c.getItemInHand())+1)%3;AlchemyArtifacts.setMode(c.getItemInHand(),charge);
                p.sendOverlayMessage(Component.translatable("message.jugcraft.stone.charge",charge*2+1,charge*2+1));
            }else PhilosopherTransforms.transform(p,c.getItemInHand(),c.getClickedPos(),c.getClickedFace());
            return InteractionResult.SUCCESS;
        }
        @Override public boolean canEquip(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity) {
            return entity instanceof Player && this != STONE;
        }
        @Override public TrinketDropRule getDropRule(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity) {
            return slot.cosmetic() ? TrinketDropRule.KEEP : TrinketCallback.super.getDropRule(stack, slot, entity);
        }
        @Override public boolean isFoil(ItemStack stack) { return stack.has(ACTIVE) || super.isFoil(stack); }
        @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                Consumer<Component> text, TooltipFlag flag) {
            text.accept(Component.translatable("tooltip.jugcraft." + id).withStyle(ChatFormatting.GRAY));
            if (this != STONE) text.accept(Component.translatable("screen.jugcraft.alchemy." + (stack.has(ACTIVE) ? "on" : "off"))
                    .withStyle(stack.has(ACTIVE) ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        }
    }
}
