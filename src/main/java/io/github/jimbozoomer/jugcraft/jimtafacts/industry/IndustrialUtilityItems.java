package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.jimtafacts.JimtaContainer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tools.Chargeable;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.Level;

/** Original hand tools bridge early manual production into the same factory materials. */
public final class IndustrialUtilityItems {
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    private IndustrialUtilityItems() {}
    public static void register() {
        for (String name : List.of("forge_hammer", "wire_cutters", "lathing_tool", "toolbox")) {
            var key = ResourceKey.create(Registries.ITEM, Jugcraft.id("jimta_" + name));
            var props = new Item.Properties().setId(key).stacksTo(1);
            if (!name.equals("toolbox")) props.durability(name.equals("forge_hammer") ? 80 : name.equals("lathing_tool") ? 50 : 60);
            ITEMS.put(name, Registry.register(BuiltInRegistries.ITEM, key, new Utility(props, name)));
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> ITEMS.values().forEach(out::accept));
    }
    public static ItemStack product(String tool, ItemStack input) {
        if (input.isEmpty()) return ItemStack.EMPTY;
        var id = BuiltInRegistries.ITEM.getKey(input.getItem());
        if (tool.equals("lathing_tool")) return ItemStack.EMPTY;
        String name = id.getPath(), suffix = tool.equals("forge_hammer") ? "_ingot" : "_plate";
        if (!(id.getNamespace().equals("minecraft") || id.getNamespace().equals("jugcraft")) || !name.endsWith(suffix)) return ItemStack.EMPTY;
        String metal = name.substring(0, name.length() - suffix.length());
        if (!Set.of("copper", "iron", "tin", "bronze", "lead", "silver", "gold", "steel").contains(metal)) return ItemStack.EMPTY;
        var result = Jugcraft.id(metal + (tool.equals("forge_hammer") ? "_plate" : "_wire"));
        return BuiltInRegistries.ITEM.containsKey(result) ? new ItemStack(BuiltInRegistries.ITEM.getValue(result), tool.equals("forge_hammer") ? 1 : 2) : ItemStack.EMPTY;
    }
    static final class Utility extends Item {
        final String kind;
        Utility(Properties p, String kind) { super(p); this.kind = kind; }
        @Override public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.Entity entity, EquipmentSlot slot) {
            if (kind.equals("toolbox") && stack.has(DataComponents.CUSTOM_MODEL_DATA)
                    && (!(entity instanceof ServerPlayer p) || !(p.containerMenu instanceof ToolboxMenu menu) || menu.held != stack)) stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        }
        @Override public InteractionResult use(Level l, Player p, InteractionHand hand) {
            if (!(p instanceof ServerPlayer player)) return InteractionResult.SUCCESS;
            if (p.isSpectator() || !p.isAlive()) return InteractionResult.FAIL;
            var held = p.getItemInHand(hand);
            if (kind.equals("lathing_tool")) { p.sendOverlayMessage(Component.literal("Fit this tool in a Kinetic Lathe, supply shaft power, then cut the five blank segments.")); return InteractionResult.SUCCESS; }
            if (kind.equals("toolbox")) {
                player.openMenu(new SimpleMenuProvider((id, inv, who) -> new ToolboxMenu(id, inv, hand, held), Component.literal("Toolbox · Nine tools")));
                return InteractionResult.SUCCESS;
            }
            if (player.getCooldowns().isOnCooldown(held)) return InteractionResult.FAIL;
            var input = p.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            int cost = kind.equals("forge_hammer") ? 2 : 1; var output = product(kind, input);
            if (output.isEmpty() || input.getCount() < cost) {
                p.sendOverlayMessage(Component.literal(kind.equals("forge_hammer") ? "Hold two matching ingots in the other hand to hammer a plate." : kind.equals("lathing_tool") ? "Hold a wooden or iron turning blank in your other hand." : "Hold a copper or silver plate in the other hand to cut two wires."));
                return InteractionResult.FAIL;
            }
            input.shrink(cost);
            held.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            player.getInventory().placeItemBackInInventory(output, net.minecraft.util.Prediction.SERVER_ONLY);
            player.getCooldowns().addCooldown(held, 5); player.getInventory().setChanged();
            return InteractionResult.SUCCESS;
        }
        @Override public void appendHoverText(ItemStack s, TooltipContext ctx, TooltipDisplay display, java.util.function.Consumer<Component> out, TooltipFlag flag) {
            out.accept(Component.literal(kind.equals("toolbox") ? "Nine tools; open in either hand. Stored tools retain their charge, wear and upgrades."
                    : kind.equals("lathing_tool") ? "Kinetic Lathe tool: one durability per cut. Fifty uses."
                    : kind.equals("forge_hammer") ? "Use with two ingots in your other hand → one plate. One durability per operation."
                    : "Use with a matching plate in your other hand → two wires. One durability per operation."));
        }
    }
    public static boolean storable(ItemStack s) {
        return !s.isEmpty() && s.getCount() == 1 && s.getMaxStackSize() == 1
                && !s.has(DataComponents.CONTAINER) && !s.has(DataComponents.BUNDLE_CONTENTS)
                && s.getItem() != ITEMS.get("toolbox")
                && (s.isDamageableItem() || s.has(DataComponents.TOOL) || s.getItem() instanceof Chargeable);
    }
    public static final class ToolboxMenu extends AbstractContainerMenu {
        private final InteractionHand hand;
        private final ItemStack held;
        private final JimtaContainer tools = new JimtaContainer(9);
        ToolboxMenu(int id, Inventory inv, InteractionHand hand, ItemStack held) {
            super(MenuType.GENERIC_9x1, id); this.hand = hand; this.held = held;
            held.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(1F), List.of(), List.of(), List.of()));
            held.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(tools.getItems());
            tools.addListener(changed -> { held.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(tools.getItems())); inv.setChanged(); });
            for (int col = 0; col < 9; col++) addSlot(new Slot(tools, col, 8 + col * 18, 18) {
                @Override public boolean mayPlace(ItemStack s) { return storable(s); }
                @Override public int getMaxStackSize() { return 1; }
            });
            for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) playerSlot(inv, 9 + row * 9 + col, 8 + col * 18, 49 + row * 18);
            for (int col = 0; col < 9; col++) playerSlot(inv, col, 8 + col * 18, 107);
        }
        private void playerSlot(Inventory inv, int index, int x, int y) {
            addSlot(new Slot(inv, index, x, y) {
                @Override public boolean mayPickup(Player p) { return getItem() != held; }
                @Override public boolean mayPlace(ItemStack s) { return getItem() != held; }
            });
        }
        @Override public boolean stillValid(Player p) { return p.isAlive() && !p.isSpectator() && !held.isEmpty() && p.getItemInHand(hand) == held; }
        @Override public void removed(Player p) { super.removed(p); held.remove(DataComponents.CUSTOM_MODEL_DATA); }
        @Override public void clicked(int slot, int button, ContainerInput input, Player p) {
            if (!stillValid(p) || input == ContainerInput.SWAP && (button == 40 && hand == InteractionHand.OFF_HAND
                    || button == p.getInventory().getSelectedSlot() && hand == InteractionHand.MAIN_HAND)) return;
            super.clicked(slot, button, input, p);
        }
        @Override public ItemStack quickMoveStack(Player p, int i) {
            if (!stillValid(p) || i < 0 || i >= slots.size()) return ItemStack.EMPTY;
            var slot = slots.get(i); var stack = slot.getItem();
            if (stack.isEmpty() || stack == held) return ItemStack.EMPTY;
            var copy = stack.copy();
            if (i < 9 ? !moveItemStackTo(stack, 9, slots.size(), true) : !storable(stack) || !moveItemStackTo(stack, 0, 9, false)) return ItemStack.EMPTY;
            slot.setByPlayer(stack.isEmpty() ? ItemStack.EMPTY : stack); slot.setChanged(); return copy;
        }
    }
}
