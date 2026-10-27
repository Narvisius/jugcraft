package io.github.jimbozoomer.jugcraft.workshop;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Uses vanilla menu buttons/slot synchronization; no client-supplied recipes or work amounts. */
public final class WorkshopMenu extends AbstractContainerMenu {
    public static final int PULSE = 0, RELEASE = 1, CANCEL = 2, SELECT = 16;
    public static final int[][] POSITIONS = {{132,55},{152,55},{172,55},{132,87},{172,87},{252,55},{212,87},{252,87},{132,87}};
    private final Container container;
    private final ContainerData data;
    private final @Nullable WorkshopBlockEntity machine;
    public final WorkshopCatalog catalog;
    public WorkshopMenu(int id, Inventory inventory, WorkshopCatalog catalog) {
        this(id, inventory, new SimpleContainer(WorkshopBlockEntity.SLOTS), null, new SimpleContainerData(WorkshopBlockEntity.DATA_COUNT), catalog);
    }
    public WorkshopMenu(int id, Inventory inventory, WorkshopBlockEntity machine, ContainerData data, WorkshopCatalog catalog) {
        this(id, inventory, machine, machine, data, catalog);
    }
    private WorkshopMenu(int id, Inventory inventory, Container container, @Nullable WorkshopBlockEntity machine, ContainerData data, WorkshopCatalog catalog) {
        super(JugcraftWorkshop.MENU, id); this.container = container; this.machine = machine; this.data = data; this.catalog = catalog;
        for (int i = 0; i < WorkshopBlockEntity.SLOTS; i++) {
            final int index = i;
            addSlot(new Slot(container, i, POSITIONS[i][0], POSITIONS[i][1]) {
                @Override public boolean mayPlace(ItemStack stack) {
                    if (!isActive() || index == WorkshopBlockEntity.OUTPUT || locked(index)) return false;
                    return machine == null || container.canPlaceItem(index, stack);
                }
                @Override public boolean mayPickup(Player player) { return !locked(index) && stillValid(player); }
                @Override public int getMaxStackSize() { return index == 3 || index >= 6 ? 1 : 64; }
                @Override public boolean isActive() { return (index != 3 || catalog.station() == 0) && (index < 6 || catalog.station() == 2); }
            });
        }
        addStandardInventorySlots(inventory, 116, 146); addDataSlots(data);
    }
    public @Nullable WorkshopBlockEntity machine() { return machine; }
    public int data(int i) { return data.get(i); }
    public int heat() { return (data(2) & 65535) | (data(3) << 16); }
    public boolean busy() { return data(6) != 0; }
    private boolean locked(int slot) { return busy() && (slot == 3 || slot >= 6); }
    public boolean currentCatalog() { return machine == null || machine.catalog() == catalog; }
    @Override public boolean stillValid(Player player) { return machine == null || machine.mayOperate(player); }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (machine == null || !(player instanceof ServerPlayer server) || server.containerMenu != this || !stillValid(player)) return false;
        if (id == RELEASE) { machine.release(player); return true; }
        if (!currentCatalog()) return false;
        if (id == PULSE) return machine.pulse(server);
        if (id == CANCEL) return machine.cancel();
        int index = id - SELECT;
        return index >= 0 && index < catalog.entries().size() && machine.select(catalog.entries().get(index).id());
    }
    @Override public void removed(Player player) { if (machine != null) machine.release(player); super.removed(player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        boolean moved = index < WorkshopBlockEntity.SLOTS ? moveItemStackTo(stack, WorkshopBlockEntity.SLOTS, slots.size(), true) : moveItemStackTo(stack, 3, WorkshopBlockEntity.SLOTS, false) || moveItemStackTo(stack, 0, 3, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack); return original;
    }
}
