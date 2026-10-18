package io.github.jimbozoomer.jugcraft.concordance.assay;

import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Six bounded transfer buttons; the client never supplies a balance, charge or arbitrary amount. */
public final class KleinStarMenu extends AbstractContainerMenu {
    private final Player owner;
    private final SimpleContainer contents=new SimpleContainer(1);
    private final SimpleContainerData data=new SimpleContainerData(7);
    public KleinStarMenu(int id,Inventory inventory){
        super(KleinStars.MENU,id);owner=inventory.player;
        addSlot(new Slot(contents,0,143,48){
            @Override public boolean mayPlace(ItemStack stack){return KleinStars.capacity(stack)>0;}
            @Override public int getMaxStackSize(){return 1;}
        });
        addStandardInventorySlots(inventory,70,150);addDataSlots(data);refresh();
    }
    private void put(int index,long n){data.set(index,(int)n&0xffff);data.set(index+1,(int)(n>>>16));}
    private long number(int index){return(data.get(index)&0xffffL)|((data.get(index+1)&0xffffL)<<16);}
    public long balance(){return number(0);}
    public int stored(){return(int)number(2);}
    public int capacity(){return(int)number(4);}
    public boolean canStore(){return(data.get(6)&1)!=0;}
    public boolean canWithdraw(){return(data.get(6)&2)!=0;}
    private void refresh(){
        if(!(owner instanceof ServerPlayer player))return;
        var star=contents.getItem(0);int stored=KleinStars.stored(star),capacity=KleinStars.capacity(star);long balance=Assaying.balance(player);
        boolean valid=stillValid(player)&&star.getCount()==1&&capacity>0;
        put(0,balance);put(2,stored);put(4,capacity);
        data.set(6,(valid&&balance>0&&stored<capacity?1:0)|(valid&&stored>0&&balance<Assay.MAX_BALANCE?2:0));
    }
    @Override public void broadcastChanges(){refresh();super.broadcastChanges();}
    @Override public boolean clickMenuButton(Player player,int id){
        if(player!=owner||!(player instanceof ServerPlayer server)||id<0||id>5||!stillValid(player)
                ||!RateGate.allow(server,"klein_star_transfer",4))return false;
        refresh();boolean deposit=id<3;
        if(deposit?!canStore():!canWithdraw())return false;
        long requested=switch(id%3){case 0->1024;case 1->16384;default->Long.MAX_VALUE;};
        var star=contents.getItem(0);int stored=KleinStars.stored(star);long balance=Assaying.balance(player);
        int amount=(int)Math.min(requested,deposit?Math.min(balance,KleinStars.capacity(star)-stored):Math.min(stored,Assay.MAX_BALANCE-balance));
        if(amount<=0)return false;
        KleinStars.setStored(star,stored+(deposit?amount:-amount));
        Assaying.setBalance(player,balance+(deposit?-amount:amount));contents.setChanged();broadcastChanges();return true;
    }
    @Override public boolean stillValid(Player player){return player==owner&&(!(player instanceof ServerPlayer server)||AlchemicalGear.ready(server));}
    @Override public void removed(Player player){super.removed(player);if(player instanceof ServerPlayer)clearContainer(player,contents);}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),copy=stack.copy();
        if(index==0){if(!moveItemStackTo(stack,1,slots.size(),true))return ItemStack.EMPTY;}
        else if(KleinStars.capacity(stack)==0||!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return copy;
    }
}
