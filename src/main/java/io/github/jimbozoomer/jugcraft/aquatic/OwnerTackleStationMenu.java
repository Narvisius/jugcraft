package io.github.jimbozoomer.jugcraft.aquatic;
import net.minecraft.core.NonNullList;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;

/** Supplied 17 storage slots plus four sockets editing the stored rod itself. */
public final class OwnerTackleStationMenu extends AbstractContainerMenu {
    private final Container box;private final boolean client;private boolean refreshing;
    private final SimpleContainer parts=new SimpleContainer(4){@Override public void setChanged(){super.setChanged();flush();}};
    public OwnerTackleStationMenu(int id,Inventory inv,Container box){
        super(OwnerAquaticStations.MENU,id);this.box=box;client=inv.player.level().isClientSide();box.startOpen(inv.player);
        addSlot(new Slot(box,0,17,33){@Override public boolean mayPlace(ItemStack s){return s.getItem() instanceof OwnerAquaticGear.Rod;}});
        for(int i=0;i<4;i++){final int n=i;addSlot(new Slot(parts,i,53+27*i,33){@Override public boolean isActive(){return box.getItem(0).getItem() instanceof OwnerAquaticGear.Rod;}@Override public int getMaxStackSize(){return 1;}@Override public boolean mayPlace(ItemStack s){return isActive()&&OwnerTackleMenu.accepts(n,s);}@Override public boolean mayPickup(Player p){return isActive();}});}
        for(int i=0;i<16;i++)addSlot(new Slot(box,i+1,17+i%8*18,72+i/8*18){@Override public boolean mayPlace(ItemStack s){return supply(s);}});
        addStandardInventorySlots(inv,8,122);refresh();
    }
    public static boolean supply(ItemStack s){return s.getItem() instanceof OwnerAquaticGear.Bait||s.getItem() instanceof OwnerAquaticGear.Hook||s.is(OwnerAquaculture.ITEMS.get("fishing_line"))||s.is(OwnerAquaculture.ITEMS.get("bobber"));}
    private void refresh(){if(client)return;refreshing=true;for(int i=0;i<4;i++)parts.setItem(i,OwnerAquaticGear.part(box.getItem(0),i).copy());refreshing=false;}
    private void flush(){if(client||refreshing||box==null||!(box.getItem(0).getItem() instanceof OwnerAquaticGear.Rod))return;var list=NonNullList.withSize(4,ItemStack.EMPTY);for(int i=0;i<4;i++)list.set(i,parts.getItem(i).copy());box.getItem(0).set(OwnerAquaticGear.TACKLE,ItemContainerContents.fromItems(list));box.setChanged();}
    @Override public void broadcastChanges(){refresh();super.broadcastChanges();}
    @Override public void clicked(int i,int b,ContainerInput input,Player p){if(!stillValid(p))return;refresh();super.clicked(i,b,input,p);refresh();}
    @Override public boolean stillValid(Player p){return box.stillValid(p);}
    @Override public void removed(Player p){super.removed(p);box.stopOpen(p);}
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(!stillValid(p)||index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem()||!slot.mayPickup(p))return ItemStack.EMPTY;var stack=slot.getItem();var copy=stack.copy();
        boolean moved=index<21?moveItemStackTo(stack,21,slots.size(),true):stack.getItem() instanceof OwnerAquaticGear.Rod?moveItemStackTo(stack,0,1,false):moveItemStackTo(stack,5,21,false);
        if(!moved)return ItemStack.EMPTY;if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,stack);return copy;
    }
}
