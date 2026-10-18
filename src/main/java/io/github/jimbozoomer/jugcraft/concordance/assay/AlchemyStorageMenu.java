package io.github.jimbozoomer.jugcraft.concordance.assay;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jspecify.annotations.Nullable;

/** The exact physical bag remains in a locked inventory slot; every mutation immediately updates its component. */
public final class AlchemyStorageMenu extends AbstractContainerMenu {
    public static final int SIZE=54;
    private final Player owner;
    private final Container contents;
    private final @Nullable AlchemyStorageBlockEntity chest;
    private final @Nullable ItemStack carrier;
    private final SimpleContainerData data=new SimpleContainerData(1);
    private boolean loading=true;
    public AlchemyStorageMenu(int id,Inventory inv){this(id,inv,null,null,-1);}
    public AlchemyStorageMenu(int id,Inventory inv,AlchemyStorageBlockEntity chest){this(id,inv,chest,null,-1);}
    public AlchemyStorageMenu(int id,Inventory inv,InteractionHand hand,ItemStack bag){this(id,inv,null,bag,hand==InteractionHand.MAIN_HAND?inv.getSelectedSlot():Inventory.SLOT_OFFHAND);}
    private AlchemyStorageMenu(int id,Inventory inv,@Nullable AlchemyStorageBlockEntity chest,@Nullable ItemStack carrier,int locked){
        super(AlchemyStorage.MENU,id);owner=inv.player;this.chest=chest;this.carrier=carrier;data.set(0,locked);
        contents=chest!=null?chest:new SimpleContainer(SIZE){
            @Override public void setChanged(){super.setChanged();saveBag();}
            @Override public boolean canPlaceItem(int slot,ItemStack stack){return AlchemyStorage.canStore(stack);}
        };
        if(carrier!=null){
            NonNullList<ItemStack> stored=NonNullList.withSize(SIZE,ItemStack.EMPTY);
            carrier.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).copyInto(stored);
            for(int i=0;i<SIZE;i++)contents.setItem(i,stored.get(i));
        }
        for(int row=0;row<6;row++)for(int col=0;col<9;col++)addSlot(new Slot(contents,row*9+col,27+col*18,29+row*18){
            @Override public boolean mayPlace(ItemStack stack){return AlchemyStorage.canStore(stack);}
            @Override public boolean mayPickup(Player player){return stillValid(player);}
        });
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)inventorySlot(inv,9+row*9+col,27+col*18,148+row*18);
        for(int col=0;col<9;col++)inventorySlot(inv,col,27+col*18,206);
        loading=false;if(chest!=null&&owner instanceof ServerPlayer)chest.viewers++;addDataSlots(data);
    }
    private void inventorySlot(Inventory inv,int index,int x,int y){addSlot(new Slot(inv,index,x,y){
        @Override public boolean mayPickup(Player player){return index!=lockedSlot()&&stillValid(player);}
        @Override public boolean mayPlace(ItemStack stack){return index!=lockedSlot();}
    });}
    public int lockedSlot(){return data.get(0);}
    public Container contents(){return contents;}
    private void saveBag(){
        if(loading||carrier==null||!(owner instanceof ServerPlayer)||owner.getInventory().getItem(lockedSlot())!=carrier)return;
        var saved=NonNullList.withSize(SIZE,ItemStack.EMPTY);for(int i=0;i<SIZE;i++)saved.set(i,contents.getItem(i));
        carrier.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(saved));owner.getInventory().setChanged();
    }
    @Override public boolean clickMenuButton(Player player,int action){
        if(!(player instanceof ServerPlayer p)||!stillValid(player)||action<0||action>2
            ||!io.github.jimbozoomer.jugcraft.concordance.RateGate.allow(p,"alchemy_sort",4))return false;
        if(action==2){
            // Explicit pause for artifacts in this physical container, never an inventory-wide toggle.
            for(int i=0;i<SIZE;i++)contents.getItem(i).remove(AlchemicalGear.ACTIVE);
        }else{
            var packed=new java.util.ArrayList<ItemStack>();
            for(int i=0;i<SIZE;i++){
                var remainder=contents.getItem(i).copy();if(remainder.isEmpty())continue;
                for(var existing:packed)if(ItemStack.isSameItemSameComponents(existing,remainder)){
                    int moved=Math.min(remainder.getCount(),existing.getMaxStackSize()-existing.getCount());existing.grow(moved);remainder.shrink(moved);
                    if(remainder.isEmpty())break;
                }
                if(!remainder.isEmpty())packed.add(remainder);
            }
            if(action==0)packed.sort(java.util.Comparator.comparing(s->net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString()));
            loading=true;for(int i=0;i<SIZE;i++)contents.setItem(i,i<packed.size()?packed.get(i):ItemStack.EMPTY);loading=false;
        }
        contents.setChanged();saveBag();broadcastChanges();return true;
    }
    @Override public boolean stillValid(Player player){
        if(player!=owner||!player.isAlive()||player.isSpectator())return false;
        if(!(player instanceof ServerPlayer))return true;
        return chest!=null?chest.stillValid(player):carrier!=null&&lockedSlot()>=0&&player.getInventory().getItem(lockedSlot())==carrier&&AlchemyStorage.bag(carrier);
    }
    @Override public void clicked(int slot,int button,ContainerInput input,Player player){
        if(!stillValid(player)||input==ContainerInput.SWAP&&button==lockedSlot())return;
        if(slot>=0&&slot<slots.size()&&slots.get(slot).container==player.getInventory()&&slots.get(slot).getContainerSlot()==lockedSlot())return;
        super.clicked(slot,button,input,player);saveBag();
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(!stillValid(player)||index<0||index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.hasItem()||!slot.mayPickup(player))return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),copy=stack.copy();
        if(index<SIZE){if(!moveItemStackTo(stack,SIZE,slots.size(),true))return ItemStack.EMPTY;}
        else if(!AlchemyStorage.canStore(stack)||!moveItemStackTo(stack,0,SIZE,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);saveBag();return copy;
    }
    @Override public void removed(Player player){saveBag();if(chest!=null&&player instanceof ServerPlayer)chest.viewers=Math.max(0,chest.viewers-1);super.removed(player);}
}
