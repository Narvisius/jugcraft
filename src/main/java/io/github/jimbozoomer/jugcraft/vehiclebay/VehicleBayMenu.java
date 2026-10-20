package io.github.jimbozoomer.jugcraft.vehiclebay;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** All changes are bounded menu buttons, checked against the open, nearby server container. */
public class VehicleBayMenu extends AbstractContainerMenu {
    private final Container container;
    public final ContainerData data;
    public VehicleBayMenu(int id, Inventory inv) { this(id, inv, new SimpleContainer(VehicleBayBlockEntity.SLOTS), new SimpleContainerData(VehicleBayBlockEntity.DATA)); }
    public VehicleBayMenu(int id, Inventory inv, Container container, ContainerData data) {
        super(VehicleBays.MENU, id); this.container = container; this.data = data;
        for (int i = 0; i < 9; i++) addSlot(filtered(i, 12 + i % 3 * 18, 100 + i / 3 * 18));
        addSlot(filtered(9, 83, 118)); addSlot(filtered(10, 286, 118));
        addStandardInventorySlots(inv, 77, 158); addDataSlots(data);
    }
    private Slot filtered(int index, int x, int y) {
        return new Slot(container, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return !busy() && index != 10 && (index != 9 || VehiclePlans.plan(stack) >= 0) && container.canPlaceItem(index, stack); }
            @Override public boolean mayPickup(Player player) { return !busy() && super.mayPickup(player); }
            @Override public int getMaxStackSize() { return index == 9 ? 1 : super.getMaxStackSize(); }
        };
    }
    public boolean busy() { return data.get(4) > 0; }
    public int plan() { return Math.clamp(data.get(0), 0, 3); }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (!(container instanceof VehicleBayBlockEntity bay) || !stillValid(player) || !player.mayBuild()) return false;
        boolean changed;
        if (id == 30) changed = bay.start();
        else if (id == 31) { bay.cancel(); changed = true; }
        else changed = bay.select(id);
        broadcastChanges(); return changed;
    }
    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || busy() || !stillValid(player)) return ItemStack.EMPTY;
        var slot = slots.get(index); if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        var stack = slot.getItem(); var original = stack.copy();
        boolean moved = index < 11 ? moveItemStackTo(stack, 11, slots.size(), true) : moveItemStackTo(stack, 0, 9, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack); return original;
    }
}
