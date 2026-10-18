package io.github.jimbozoomer.jugcraft.concordance.assay;

import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Real block inventory; no client-provided quantities, target IDs, balances or free ghost samples. */
public final class AlchemicalWorkshopMenu extends AbstractContainerMenu {
    public final boolean collector;
    private final Player owner;
    private final Container contents;
    private final @Nullable AlchemicalWorkshopBlockEntity workshop;
    private final SimpleContainerData data = new SimpleContainerData(15);

    public AlchemicalWorkshopMenu(int id, Inventory inventory, boolean collector) {
        this(id, inventory, collector, new SimpleContainer(11), null);
    }
    public AlchemicalWorkshopMenu(int id, Inventory inventory, AlchemicalWorkshopBlockEntity workshop) {
        this(id, inventory, workshop.collector(), workshop, workshop);
    }
    private AlchemicalWorkshopMenu(int id, Inventory inventory, boolean collector, Container contents, @Nullable AlchemicalWorkshopBlockEntity workshop) {
        super(collector ? AlchemicalWorkshops.COLLECTOR_MENU : AlchemicalWorkshops.CONDENSER_MENU, id);
        this.collector = collector; this.contents = contents; this.workshop = workshop; owner = inventory.player;
        addSlot(new Slot(contents, 0, 30, 67) {
            @Override public boolean mayPlace(ItemStack stack) { return KleinStars.capacity(stack) > 0; }
            @Override public int getMaxStackSize() { return 1; }
        });
        {
            addSlot(new Slot(contents, 1, 142, 67) {
                @Override public boolean isActive(){return !collector;}
                // Dedicated clients have no server datapack catalogue. Let their prediction proceed;
                // the real server container remains the only item validator.
                @Override public boolean mayPlace(ItemStack stack) { return workshop == null || contents.canPlaceItem(1, stack); }
                @Override public int getMaxStackSize() { return 1; }
            });
            for (int i = 2; i < 5; i++) addSlot(new Slot(contents, i, 30 + (i-2)*18, 94) {
                @Override public boolean mayPlace(ItemStack stack) { return workshop == null || contents.canPlaceItem(2, stack); }
            });
            for (int i = 5; i < 11; i++) addSlot(new Slot(contents, i, 252 + (i-5)%3*18, 61 + (i-5)/3*18) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }
        if(workshop!=null&&owner instanceof ServerPlayer)workshop.viewers++;
        addStandardInventorySlots(inventory, 93, 158); addDataSlots(data); refresh();
    }
    private void put(int offset, long value) { data.set(offset, (int)value & 65535); data.set(offset+1, (int)(value >>> 16)); }
    private long number(int offset) { return (data.get(offset)&65535L) | ((data.get(offset+1)&65535L)<<16); }
    public long essence() { return number(0); }
    public long capacity() { return number(2); }
    public long cost() { return number(4); }
    public long wallet() { return number(6); }
    public int fuelTarget(){return data.get(14);}
    public int tier() { return data.get(8); }
    public int status() { return data.get(9); }
    public boolean paused() { return data.get(10) != 0; }
    public int rate() { return data.get(11); }
    public int sunlight() { return data.get(12); }
    public boolean canOperate() { return data.get(13) != 0; }
    private void refresh() {
        if (workshop == null || !(owner instanceof ServerPlayer server)) return;
        put(0, workshop.essence()); put(2, workshop.capacity()); put(4, collector ? 0 : workshop.cost()); put(6, Assaying.balance(owner));
        data.set(14,workshop.fuelTarget());data.set(8, workshop.tier()); data.set(9, workshop.status()); data.set(10, workshop.paused() ? 1 : 0);
        data.set(11, workshop.rate()); data.set(12, workshop.sunlight()); data.set(13, stillValid(server) && AlchemicalGear.ready(server) ? 1 : 0);
    }
    @Override public void broadcastChanges() { refresh(); super.broadcastChanges(); }
    @Override public boolean stillValid(Player player) { return player == owner && (workshop == null ? !(player instanceof ServerPlayer) : workshop.stillValid(player)); }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (player != owner || !(player instanceof ServerPlayer server) || workshop == null || id < 0 || id > 3
                || !stillValid(server) || !AlchemicalGear.ready(server) || !RateGate.allow(server, "alchemy_workshop", 4)) return false;
        if(id==3){if(!collector)return false;workshop.setFuelTarget((workshop.fuelTarget()+1)%4);}
        else if (id == 0) workshop.setPaused(!workshop.paused());
        else if (id == 1) {
            int moved = (int)Math.min(Assaying.balance(player), workshop.capacity() - workshop.essence());
            Assaying.setBalance(player, Assaying.balance(player) - moved); workshop.setEssence(workshop.essence() + moved);
        } else {
            int moved = (int)Math.min(workshop.essence(), Assay.MAX_BALANCE - Assaying.balance(player));
            Assaying.setBalance(player, Assaying.balance(player) + moved); workshop.setEssence(workshop.essence() - moved);
        }
        broadcastChanges(); return true;
    }
    @Override public void removed(Player player){if(workshop!=null&&player instanceof ServerPlayer)workshop.viewers=Math.max(0,workshop.viewers-1);super.removed(player);}
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        int end = 11;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index < end) { if (!moveItemStackTo(stack, end, slots.size(), true)) return ItemStack.EMPTY; }
        else if (KleinStars.capacity(stack) > 0) { if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY; }
        else if (!moveItemStackTo(stack, 2, 5, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack); return copy;
    }
}
