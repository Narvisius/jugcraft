package io.github.jimbozoomer.jugcraft.concordance.assay;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;

/** Late alchemical equipment, using the same essence ledger and ordinary durable equipment contracts. */
public final class ExtendedAlchemyEquipment {
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static final ToolMaterial RED_MATERIAL = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
            8192, 12.0F, 5.0F, 14, TagKey.create(Registries.ITEM, Jugcraft.id("repairs_red_matter_tools")));
    public static final String[] ARMOR_SETS = {"dark_matter", "red_matter", "gem"};
    private static final ArmorType[] ARMOR_TYPES = {ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS};
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] PIECES = {"helmet", "chestplate", "leggings", "boots"};
    private ExtendedAlchemyEquipment() { }

    public static void register() {
        for (String id : new String[]{"red_matter", "iron_band"}) item(id, new Item.Properties().rarity(Rarity.UNCOMMON));
        for (String material : new String[]{"dark_matter", "red_matter", "alchemical_coal", "mobius_fuel", "aeternalis_fuel"}) {
            String id = material + "_block";
            var key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(id));
            Block block = Registry.register(BuiltInRegistries.BLOCK, key, new Block(BlockBehaviour.Properties.of()
                    .setId(key).strength(5, 30).sound(SoundType.METAL).requiresCorrectToolForDrops()));
            BLOCKS.put(id, block);
            var itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
            var properties = new Item.Properties().setId(itemKey).useBlockDescriptionPrefix();
            int burn = switch (material) { case "alchemical_coal" -> 57600; case "mobius_fuel" -> 115200; case "aeternalis_fuel" -> 230400; default -> 0; };
            if (burn > 0) properties.component(DataComponents.COOKING_FUEL,
                    new CookingFuel(new ResolvableInt.Constant(burn), new ResolvableFloat.Constant(1)));
            ITEMS.put(id, Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, properties)));
        }
        for (ExtendedMatterTool.Kind kind : ExtendedMatterTool.Kind.values()) {
            tool("red_matter", kind, true);
            if (kind == ExtendedMatterTool.Kind.SWORD || kind == ExtendedMatterTool.Kind.SHOVEL
                    || kind == ExtendedMatterTool.Kind.HOE || kind == ExtendedMatterTool.Kind.SHEARS) tool("dark_matter", kind, false);
        }
        for (int tier = 0; tier < ARMOR_SETS.length; tier++) {
            String set = ARMOR_SETS[tier];
            ArmorMaterial material = new ArmorMaterial(64 + tier * 16,
                    Map.of(ArmorType.HELMET, 3, ArmorType.CHESTPLATE, 8, ArmorType.LEGGINGS, 6, ArmorType.BOOTS, 3, ArmorType.BODY, 8),
                    12 + tier * 3, SoundEvents.ARMOR_EQUIP_NETHERITE, 3 + tier, 0.1F,
                    TagKey.create(Registries.ITEM, Jugcraft.id("repairs_" + set + "_armor")),
                    ResourceKey.create(EquipmentAssets.ROOT_ID, Jugcraft.id("alchemy/" + set)));
            for (int piece = 0; piece < PIECES.length; piece++) {
                String id = set + "_" + PIECES[piece];
                var key = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
                ITEMS.put(id, Registry.register(BuiltInRegistries.ITEM, key,
                        new AlchemicalArmor(new Item.Properties().setId(key).humanoidArmor(material, ARMOR_TYPES[piece])
                                .fireResistant().rarity(tier == 2 ? Rarity.EPIC : Rarity.RARE).component(AlchemicalGear.ACTIVE, Unit.INSTANCE))));
            }
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> ITEMS.values().forEach(entries::accept));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) tickArmor(player);
        });
    }

    private static void item(String id, Item.Properties properties) {
        var key = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
        ITEMS.put(id, Registry.register(BuiltInRegistries.ITEM, key, new Described(properties.setId(key))));
    }

    private static void tool(String tier, ExtendedMatterTool.Kind kind, boolean red) {
        String id = tier + "_" + kind.id;
        var key = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
        var p = new Item.Properties().setId(key).rarity(Rarity.RARE).fireResistant().component(DarkMatter.MODE, 0);
        var material = red ? RED_MATERIAL : DarkMatter.MATERIAL;
        switch (kind) {
            case SWORD -> p.sword(material, 3, -2.4F);
            case KATAR -> p.tool(material, ExtendedMatterTool.KATAR_BLOCKS, 5, -2.8F, 1);
            case MORNING_STAR -> p.tool(material, ExtendedMatterTool.MORNING_STAR_BLOCKS, 4, -3, 1);
            case AXE -> p.axe(material, 5, -3.0F);
            case SHOVEL -> p.shovel(material, 1.5F, -3);
            case HOE -> p.hoe(material, -4, 0);
            case SHEARS -> p.durability(red ? 8192 : 4096).enchantable(12).repairable(material.repairItems())
                    .component(DataComponents.TOOL, ShearsItem.createToolProperties());
            default -> p.pickaxe(material, kind == ExtendedMatterTool.Kind.HAMMER ? 3 : 1, -2.7F);
        }
        ITEMS.put(id, Registry.register(BuiltInRegistries.ITEM, key, new ExtendedMatterTool(p, kind, red)));
    }

    /** Falling may start and finish between one-second pulses, so boots are checked every server tick. */
    public static void tickArmor(ServerPlayer player) {
        if (player.tickCount % 20 == 0) pulseArmor(player); else pulseBoots(player);
    }

    private static void pulseBoots(ServerPlayer player) {
        if (player.fallDistance > 2 && enabled(player, EquipmentSlot.FEET, "gem_boots") && AlchemicalGear.ready(player))
            ward(player, MobEffects.SLOW_FALLING, 4);
    }

    /** Optional wards stop with low essence, a disabled piece, or no Assay; ordinary armor still protects. */
    public static void pulseArmor(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || !AlchemicalGear.ready(player)) return;
        int full = -1;
        for (int tier = 0; tier < ARMOR_SETS.length; tier++) {
            boolean wearing = true;
            for (int i = 0; i < PIECES.length; i++) wearing &= enabled(player, SLOTS[i], ARMOR_SETS[tier] + "_" + PIECES[i]);
            if (wearing) full = tier;
        }
        // Resistance is a set ward, not invulnerability; ordinary armor and all damage callbacks remain in force.
        if (full >= 0) ward(player, MobEffects.RESISTANCE, full == 0 ? 8 : 12);
        if (full == 1 && (player.isInLava() || player.isOnFire())) ward(player, MobEffects.FIRE_RESISTANCE, 8);
        if (enabled(player, EquipmentSlot.HEAD, "gem_helmet") && player.isUnderWater()) ward(player, MobEffects.WATER_BREATHING, 4);
        if (enabled(player, EquipmentSlot.CHEST, "gem_chestplate") && (player.isOnFire() || player.isInLava())) ward(player, MobEffects.FIRE_RESISTANCE, 8);
        if (enabled(player, EquipmentSlot.LEGS, "gem_leggings") && player.isSprinting()) ward(player, MobEffects.SPEED, 4);
        GemAbilities.pulse(player);
        pulseBoots(player);
    }

    private static boolean enabled(ServerPlayer player, EquipmentSlot slot, String id) {
        var stack = player.getItemBySlot(slot);
        return stack.is(ITEMS.get(id)) && stack.has(AlchemicalGear.ACTIVE);
    }

    private static void ward(ServerPlayer player, Holder<MobEffect> effect, int cost) {
        var previous = player.getEffect(effect);
        if (previous != null && (previous.getAmplifier() > 0 || previous.getDuration() > 40) || AlchemyPower.available(player) < cost) return;
        if (player.addEffect(new MobEffectInstance(effect, 60, 0, true, false, true))) AlchemyPower.pay(player, cost);
    }

    private static class Described extends Item {
        Described(Properties p) { super(p); }
        @Override public void appendHoverText(ItemStack stack, TooltipContext c, TooltipDisplay d, Consumer<Component> text, TooltipFlag f) {
            text.accept(Component.translatable(getDescriptionId().replace("item.", "tooltip.")).withStyle(ChatFormatting.GRAY));
        }
    }

    private static final class AlchemicalArmor extends Described {
        AlchemicalArmor(Properties p) { super(p); }
        @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!player.isShiftKeyDown()) return super.use(level, player, hand);
            if (player instanceof ServerPlayer server) {
                var stack = player.getItemInHand(hand);
                if (stack.has(AlchemicalGear.ACTIVE)) stack.remove(AlchemicalGear.ACTIVE); else stack.set(AlchemicalGear.ACTIVE, Unit.INSTANCE);
                server.sendOverlayMessage(Component.translatable("message.jugcraft.alchemy_armor.toggle", stack.getHoverName(),
                        Component.translatable(stack.has(AlchemicalGear.ACTIVE) ? "screen.jugcraft.alchemy.on" : "screen.jugcraft.alchemy.off")));
            }
            return InteractionResult.SUCCESS;
        }
        @Override public void appendHoverText(ItemStack stack, TooltipContext c, TooltipDisplay d, Consumer<Component> text, TooltipFlag f) {
            super.appendHoverText(stack, c, d, text, f);
            text.accept(Component.translatable("tooltip.jugcraft.alchemy_armor.controls",
                    Component.translatable(stack.has(AlchemicalGear.ACTIVE) ? "screen.jugcraft.alchemy.on" : "screen.jugcraft.alchemy.off")).withStyle(ChatFormatting.AQUA));
        }
    }
}
