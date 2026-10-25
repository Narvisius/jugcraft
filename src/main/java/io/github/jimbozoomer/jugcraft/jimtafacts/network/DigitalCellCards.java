package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tools.*;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Explicit cell overflow disposal and terminal display partitions, with real removable cards. */
public final class DigitalCellCards {
    public static final String[] ITEMS = {"energy_card", "void_card", "view_cell"};
    public static DataComponentType<Boolean> VOID;
    private DigitalCellCards() {}
    public static void register() {
        VOID = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("jimta_void_overflow"), DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());
        for (var name : ITEMS) {
            var key = ResourceKey.create(Registries.ITEM, Jugcraft.id("jimta_" + name));
            DigitalStorage.ITEMS.put(name, Registry.register(BuiltInRegistries.ITEM, key, new Card(new Item.Properties().setId(key).stacksTo(name.equals("view_cell") ? 1 : 64), name)));
        }
    }
    public static boolean configurable(ItemStack stack) { return !DigitalCreativeCell.creative(stack)&&(stack.getItem() instanceof DigitalStorage.CellItem || stack.getItem() instanceof FluidCells.Cell || stack.is(DigitalStorage.ITEMS.get("view_cell"))); }
    public static boolean voids(ItemStack cell, ItemStack sample) {
        return cell.getOrDefault(VOID, false) && !cell.getOrDefault(DigitalWorkshops.FILTERS, java.util.List.of()).isEmpty()
                && !DigitalWorkshops.filtered(cell, sample) && sample.getItem().canFitInsideContainerItems() && !(sample.getItem() instanceof DigitalStorage.CellItem) && !(sample.getItem() instanceof FluidCells.Cell);
    }
    public static boolean visible(DigitalNode node, ItemStack sample) { return node == null || !node.getItem(0).is(DigitalStorage.ITEMS.get("view_cell")) || !DigitalWorkshops.filtered(node.getItem(0), sample); }
    public static final class Card extends Item {
        final String kind;
        Card(Properties p, String kind) { super(p); this.kind = kind; }
        @Override public InteractionResult useOn(UseOnContext c) {
            var n=DigitalParts.pointed(c);
            if (kind.equals("view_cell") && c.getPlayer() instanceof ServerPlayer p && n!=null && java.util.Set.of("terminal", "crafting_terminal", "pattern_encoding_terminal").contains(n.kind()) && n.permitted(p)) {
                if (!n.getItem(0).isEmpty()) { p.sendOverlayMessage(Component.literal("Sneak-use the terminal with an empty hand to remove its view cell first.")); return InteractionResult.FAIL; }
                n.setItem(0, c.getItemInHand().split(1)); p.sendOverlayMessage(Component.literal("View cell installed. It filters this terminal's display, not access permissions.")); return InteractionResult.SUCCESS;
            }
            return super.useOn(c);
        }
        @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!(player instanceof ServerPlayer p)) return InteractionResult.SUCCESS;
            var card = p.getItemInHand(hand); var other = p.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            if (kind.equals("energy_card")) {
                var result = ToolUpgrades.fit(level, other, ToolUpgrades.Kind.CAPACITY);
                if (result == ToolUpgrades.Result.FITTED) { card.consume(1, p); p.sendOverlayMessage(Component.literal("Capacity upgrade fitted permanently (+100% base capacity; maximum two).")); return InteractionResult.SUCCESS; }
                p.sendOverlayMessage(Component.literal("Hold a powered tool in the other hand. It can take up to two capacity cards."));
            } else if (kind.equals("void_card") && other.getItem() instanceof DigitalStorage.CellItem && !DigitalCreativeCell.creative(other)) {
                if (other.getOrDefault(VOID, false)) {
                    if (!p.isShiftKeyDown()) { p.sendOverlayMessage(Component.literal("Overflow card already installed. Sneak-use to remove it.")); return InteractionResult.FAIL; }
                    other.remove(VOID); p.getInventory().placeItemBackInInventory(new ItemStack(this), net.minecraft.util.Prediction.SERVER_ONLY);
                } else {
                    if (other.getOrDefault(DigitalWorkshops.FILTERS, java.util.List.of()).isEmpty()) { p.sendOverlayMessage(Component.literal("Configure this cell's allowed items first. Only matching excess items will be destroyed.")); return InteractionResult.FAIL; }
                    other.set(VOID, true); card.shrink(1);
                }
                p.getInventory().setChanged(); return InteractionResult.SUCCESS;
            } else if (kind.equals("view_cell")) p.sendOverlayMessage(Component.literal("Configure in the Cell Workbench, then use on a terminal. Sneak-use an empty hand on the terminal to remove."));
            return InteractionResult.PASS;
        }
        @Override public void appendHoverText(ItemStack s, TooltipContext c, net.minecraft.world.item.component.TooltipDisplay d, java.util.function.Consumer<Component> out, TooltipFlag flag) {
            out.accept(Component.literal(switch (kind) { case "void_card" -> "Other hand: filtered item cell. Explicitly destroys excess matching items when full; sneak-use removes card."; case "energy_card" -> "Other hand: powered tool. Permanently adds its base JE capacity again (max 2)."; default -> "Configure in Cell Workbench. Use on a terminal to filter its displayed item types."; }));
        }
    }
}
