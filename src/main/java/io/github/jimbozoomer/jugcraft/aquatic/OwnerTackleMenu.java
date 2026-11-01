package io.github.jimbozoomer.jugcraft.aquatic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Edits the actual held rod; all movement paths protect that rod from removal. */
public final class OwnerTackleMenu extends AbstractContainerMenu {
    private final Player owner;private final ItemStack rod;private final int locked;private boolean loading=true;
    private final Container parts;
    public OwnerTackleMenu(int id,Inventory inv){this(id,inv,ItemStack.EMPTY,-1);}
    public OwnerTackleMenu(int id,Inventory inv,ItemStack rod,int locked){
        super(OwnerAquaticGear.MENU,id);owner=inv.player;this.rod=rod;this.locked=locked;
        parts=new SimpleContainer(4){@Override public void setChanged(){super.setChanged();if(!loading&&owner instanceof ServerPlayer&&stillValid(owner)){var list=net.minecraft.core.NonNullList.withSize(4,ItemStack.EMPTY);for(int i=0;i<4;i++)list.set(i,getItem(i).copy());rod.set(OwnerAquaticGear.TACKLE,ItemContainerContents.fromItems(list));owner.getInventory().setChanged();}}};
        for(int i=0;i<4;i++){final int n=i;parts.setItem(i,OwnerAquaticGear.part(rod,i).copy());addSlot(new Slot(parts,i,26+i*36,36){@Override public int getMaxStackSize(){return 1;}@Override public boolean mayPlace(ItemStack s){return accepts(n,s);}});}
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)playerSlot(inv,9+row*9+col,8+col*18,85+row*18);
        for(int col=0;col<9;col++)playerSlot(inv,col,8+col*18,143);
        loading=false;
    }
    public static boolean accepts(int slot,ItemStack s){return switch(slot){case 0->s.getItem() instanceof OwnerAquaticGear.Hook;case 1->s.getItem() instanceof OwnerAquaticGear.Bait;case 2->s.is(OwnerAquaculture.ITEMS.get("fishing_line"));case 3->s.is(OwnerAquaculture.ITEMS.get("bobber"));default->false;};}
    private void playerSlot(Inventory inv,int index,int x,int y){addSlot(new Slot(inv,index,x,y){@Override public boolean mayPickup(Player p){return index!=locked;}@Override public boolean mayPlace(ItemStack s){return index!=locked;}});}
    @Override public boolean stillValid(Player p){return !(p instanceof ServerPlayer)||locked>=0&&p==owner&&p.isAlive()&&p.fishing==null&&p.getInventory().getItem(locked)==rod;}
    @Override public void clicked(int index,int button,ContainerInput input,Player p){if(!stillValid(p)||input==ContainerInput.SWAP&&button==locked)return;super.clicked(index,button,input,p);}
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(!stillValid(p)||index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem()||!slot.mayPickup(p))return ItemStack.EMPTY;var stack=slot.getItem();var copy=stack.copy();
        if(index<4){if(!moveItemStackTo(stack,4,slots.size(),true))return ItemStack.EMPTY;}
        else {boolean moved=false;for(int i=0;i<4;i++)if(accepts(i,stack)&&moveItemStackTo(stack,i,i+1,false)){moved=true;break;}if(!moved)return ItemStack.EMPTY;}
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,stack);return copy;
    }
}
