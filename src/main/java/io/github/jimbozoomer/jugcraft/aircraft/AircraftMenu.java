package io.github.jimbozoomer.jugcraft.aircraft;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** One menu per supplied layout: even the 80-slot cargo airship keeps every slot. */
public final class AircraftMenu extends AbstractContainerMenu {
    public final AircraftSpec spec;
    private final Container inventory;
    private final Aircraft craft;
    private final ContainerData status;
    public AircraftMenu(int id,Inventory player,AircraftSpec spec) { this(id,player,spec,new SimpleContainer(spec.sockets().size()),null,new SimpleContainerData(2)); }
    public AircraftMenu(int id,Inventory player,Aircraft craft) { this(id,player,craft.spec(),craft.inventory(),craft,craft.status()); }
    private AircraftMenu(int id,Inventory player,AircraftSpec spec,Container inventory,Aircraft craft,ContainerData status) {
        super(JugcraftAircraft.MENUS.get(spec.name()),id);this.spec=spec;this.inventory=inventory;this.craft=craft;this.status=status;
        for(int i=0;i<spec.sockets().size();i++) {
            int index=i;var socket=spec.sockets().get(i);
            addSlot(new Slot(inventory,i,socket.x(),socket.y()) {
                @Override public boolean mayPlace(ItemStack stack) { return Aircraft.accepts(socket.type(),stack)&&stillValid(player.player); }
                @Override public boolean mayPickup(Player who) {return stillValid(who);}
                @Override public int getMaxStackSize() {return socket.type().equals("upgrade")||socket.type().equals("weapon")?1:64;}
            });
        }
        addStandardInventorySlots(player,(spec.width()-162)/2,spec.inventoryY());addDataSlots(status);
    }
    public int fuel() { return status.get(0); }
    public int engine() { return status.get(1); }
    @Override public boolean stillValid(Player player) {return craft==null||craft.mayUse(player)&&!craft.isRemoved()&&player.distanceToSqr(craft)<100
        &&(!(craft.level() instanceof net.minecraft.server.level.ServerLevel level)||io.github.jimbozoomer.jugcraft.concordance.Authority.mayChange(level,player,craft.blockPosition()));}
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(!stillValid(player)||index<0||index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem()||!slot.mayPickup(player))return ItemStack.EMPTY;
        var stack=slot.getItem();var before=stack.copy();int n=inventory.getContainerSize();
        if(!(index<n?moveItemStackTo(stack,n,slots.size(),true):moveItemStackTo(stack,0,n,false)))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return before;
    }
}
