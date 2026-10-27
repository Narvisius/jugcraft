package io.github.jimbozoomer.jugcraft.storage;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Every physical slot has one stable network ID. Scrolling/search only change client slot coordinates. */
public final class StorageCrateMenu extends AbstractContainerMenu {
    public final StorageCrateTier tier;
    public final Container contents;
    private final Player owner;
    public StorageCrateMenu(int id, Inventory inventory, int tier) {
        this(id, inventory, StorageCrateTier.fromNetwork(tier), new SimpleContainer(StorageCrateTier.fromNetwork(tier).slots));
    }
    public StorageCrateMenu(int id, Inventory inventory, StorageCrateBlockEntity crate) { this(id, inventory, crate.tier(), crate); }
    private StorageCrateMenu(int id, Inventory inventory, StorageCrateTier tier, Container contents) {
        super(JugcraftStorage.STORAGE_CRATE_MENU, id);
        this.tier = tier; this.contents = contents; owner = inventory.player;
        for (int i = 0; i < tier.slots; i++) addSlot(new Slot(contents, i, 12 + i % 9 * 18, 48 + i / 9 * 18));
        addStandardInventorySlots(inventory, 12, 174);
    }
    public boolean isFor(StorageCrateBlockEntity crate) { return contents == crate; }
    @Override public boolean stillValid(Player player) {
        return player == owner && player.isAlive() && !player.isSpectator() && contents.stillValid(player);
    }
    @Override public void clicked(int slot, int button, ContainerInput input, Player player) {
        if (!stillValid(player) || slot >= slots.size() || slot < 0 && slot != SLOT_CLICKED_OUTSIDE) return;
        super.clicked(slot, button, input, player);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index < tier.slots) {
            if (!moveItemStackTo(stack, tier.slots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, tier.slots, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack); return copy;
    }
    /** Client presentation only. Index and backing container never change, including for hidden slots. */
    public void positionSlot(int index, int x, int y, boolean visible) {
        Slot old = slots.get(index);
        if (old.x == x && old.y == y && old.isActive() == visible) return;
        Slot replacement = new Slot(old.container, old.getContainerSlot(), x, y) {
            @Override public boolean isActive() { return visible; }
        };
        replacement.index = index; slots.set(index, replacement);
    }
}
