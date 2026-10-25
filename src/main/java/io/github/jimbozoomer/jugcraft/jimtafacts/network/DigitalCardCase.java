package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.jimtafacts.JimtaContainer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;

/** A real nine-slot upgrade wallet in the held network tool. No nested containers or copied inventory. */
public final class DigitalCardCase {
    private DigitalCardCase() {}
    public static void open(ServerPlayer player, InteractionHand hand) {
        var held = player.getItemInHand(hand);
        player.openMenu(new SimpleMenuProvider((id, inv, who) -> new Menu(id, inv, hand, held), net.minecraft.network.chat.Component.literal("Network Tool · Upgrade Cards")));
    }
    public static boolean card(ItemStack item) {
        return DigitalUpgrades.ITEMS.stream().anyMatch(id -> item.is(DigitalStorage.ITEMS.get(id)))
                || item.is(DigitalStorage.ITEMS.get("energy_card")) || item.is(DigitalStorage.ITEMS.get("void_card"));
    }
    public static final class Menu extends AbstractContainerMenu {
        private final InteractionHand hand;
        private final ItemStack held;
        private final JimtaContainer cards = new JimtaContainer(9);
        Menu(int id, Inventory inv, InteractionHand hand, ItemStack held) {
            super(MenuType.GENERIC_9x1, id); this.hand = hand; this.held = held;
            held.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(cards.getItems());
            cards.addListener(changed -> { held.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(cards.getItems())); inv.setChanged(); });
            for (int col = 0; col < 9; col++) addSlot(new Slot(cards, col, 8 + col * 18, 18) {
                @Override public boolean mayPlace(ItemStack s) { return card(s); }
            });
            for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) playerSlot(inv, 9 + row * 9 + col, 8 + col * 18, 49 + row * 18);
            for (int col = 0; col < 9; col++) playerSlot(inv, col, 8 + col * 18, 107);
        }
        private void playerSlot(Inventory inventory, int index, int x, int y) {
            addSlot(new Slot(inventory, index, x, y) {
                @Override public boolean mayPickup(Player p) { return getItem() != held; }
                @Override public boolean mayPlace(ItemStack s) { return getItem() != held; }
            });
        }
        @Override public boolean stillValid(Player p) { return p.isAlive() && !p.isSpectator() && p.getItemInHand(hand) == held && !held.isEmpty(); }
        @Override public void clicked(int slot, int button, ContainerInput type, Player p) {
            if (!stillValid(p) || type == ContainerInput.SWAP && (button == 40 && hand == InteractionHand.OFF_HAND || button == p.getInventory().getSelectedSlot() && hand == InteractionHand.MAIN_HAND)) return;
            super.clicked(slot, button, type, p);
        }
        @Override public ItemStack quickMoveStack(Player p, int index) {
            if (!stillValid(p) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
            var slot = slots.get(index); var stack = slot.getItem(); if (stack.isEmpty() || stack == held) return ItemStack.EMPTY; var copy = stack.copy();
            if (index < 9 ? !moveItemStackTo(stack, 9, slots.size(), true) : !card(stack) || !moveItemStackTo(stack, 0, 9, false)) return ItemStack.EMPTY;
            slot.setByPlayer(stack.isEmpty() ? ItemStack.EMPTY : stack); slot.setChanged(); return copy;
        }
    }
}
