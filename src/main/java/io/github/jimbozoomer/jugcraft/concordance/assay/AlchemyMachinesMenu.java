package io.github.jimbozoomer.jugcraft.concordance.assay;

import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** One native menu contract: real persistent slots and server-validated fixed actions. */
public final class AlchemyMachinesMenu extends AbstractContainerMenu {
    private final Player owner;
    private final Container contents;
    private final @Nullable AlchemyMachinesBlockEntity machine;
    private final SimpleContainerData data=new SimpleContainerData(15);
    public AlchemyMachinesMenu(int id,Inventory inventory){this(id,inventory,new SimpleContainer(7),null);}
    public AlchemyMachinesMenu(int id,Inventory inventory,AlchemyMachinesBlockEntity machine){this(id,inventory,machine,machine);}
    private AlchemyMachinesMenu(int id,Inventory inventory,Container contents,@Nullable AlchemyMachinesBlockEntity machine){
        super(AlchemyMachines.MENU,id);owner=inventory.player;this.contents=contents;this.machine=machine;refresh();
        addSlot(new Slot(contents,0,30,70){@Override public boolean isActive(){return kind()!=AlchemyMachines.Kind.NOVA;}@Override public boolean mayPlace(ItemStack s){return KleinStars.capacity(s)>0&&kind()!=AlchemyMachines.Kind.NOVA;}@Override public int getMaxStackSize(){return 1;}});
        for(int i=1;i<4;i++){final int index=i;
            addSlot(new Slot(contents,i,120+(i-1)*18,70){
                @Override public boolean isActive(){return kind()==AlchemyMachines.Kind.RELAY||kind()==AlchemyMachines.Kind.FURNACE||kind()==AlchemyMachines.Kind.PEDESTAL&&index==1;}
                @Override public boolean mayPlace(ItemStack s){return isActive()&&(machine==null||contents.canPlaceItem(index,s));}
                @Override public int getMaxStackSize(){return kind()==AlchemyMachines.Kind.PEDESTAL?1:64;}
            });
        }
        for(int i=4;i<7;i++)addSlot(new Slot(contents,i,240+(i-4)*18,70){
            @Override public boolean isActive(){return kind()==AlchemyMachines.Kind.FURNACE;}
            @Override public boolean mayPlace(ItemStack s){return false;}
            @Override public void onTake(Player p,ItemStack s){super.onTake(p,s);if(p instanceof ServerPlayer player&&machine!=null)machine.claimExperience(player);}
        });
        addStandardInventorySlots(inventory,93,158);addDataSlots(data);
    }
    private void put(int offset,long value){data.set(offset,(int)value&65535);data.set(offset+1,(int)(value>>>16));}
    private long number(int offset){return(data.get(offset)&65535L)|((data.get(offset+1)&65535L)<<16);}
    public long essence(){return number(0);}public long capacity(){return number(2);}public long wallet(){return number(4);}
    public AlchemyMachines.Kind kind(){return AlchemyMachines.Kind.values()[Math.clamp(data.get(6),0,AlchemyMachines.Kind.values().length-1)];}
    public int tier(){return data.get(7);}public int status(){return data.get(8);}public boolean paused(){return data.get(9)!=0;}
    public int progress(){return data.get(10);}public boolean canOperate(){return data.get(11)!=0;}
    public int range(){return data.get(12);} public int lastTargets(){return data.get(14);} public int lastSpent(){return data.get(13);}
    public net.minecraft.core.BlockPos position(){return machine==null?net.minecraft.core.BlockPos.ZERO:machine.getBlockPos();}
    private void refresh(){if(machine==null||!(owner instanceof ServerPlayer player))return;data.set(14,machine.lastTargets());data.set(12,machine.range());data.set(13,machine.lastSpent());put(0,machine.essence());put(2,machine.capacity());put(4,Assaying.balance(player));data.set(6,machine.kind().ordinal());data.set(7,machine.tier());data.set(8,machine.status());data.set(9,machine.paused()?1:0);data.set(10,machine.progress());data.set(11,stillValid(player)&&AlchemicalGear.ready(player)?1:0);}
    @Override public void broadcastChanges(){refresh();super.broadcastChanges();}
    @Override public boolean stillValid(Player player){return player==owner&&(machine==null?!(player instanceof ServerPlayer):machine.stillValid(player));}
    @Override public boolean clickMenuButton(Player p,int id){
        if(p!=owner||!(p instanceof ServerPlayer player)||machine==null||id<0||id>4||!stillValid(p)||!AlchemicalGear.ready(player)||!RateGate.allow(player,"alchemy_machine",4))return false;
        if(id==4){if(kind()!=AlchemyMachines.Kind.PEDESTAL)return false;machine.cycleRange();broadcastChanges();return true;}
        if(id==3){if(kind()!=AlchemyMachines.Kind.NOVA)return false;boolean result=machine.activateNova(player);broadcastChanges();return result;}
        if(kind()==AlchemyMachines.Kind.NOVA)return false;
        if(id==0)machine.setPaused(!machine.paused());
        else if(id==1){int moved=(int)Math.min(Assaying.balance(p),machine.capacity()-machine.essence());Assaying.setBalance(p,Assaying.balance(p)-moved);machine.setEssence(machine.essence()+moved);}
        else{int moved=(int)Math.min(machine.essence(),Assay.MAX_BALANCE-Assaying.balance(p));Assaying.setBalance(p,Assaying.balance(p)+moved);machine.setEssence(machine.essence()-moved);}
        broadcastChanges();return true;
    }
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(!stillValid(p)||index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.isActive()||!slot.hasItem())return ItemStack.EMPTY;
        var stack=slot.getItem();var copy=stack.copy();
        if(index<7){if(!moveItemStackTo(stack,7,slots.size(),true))return ItemStack.EMPTY;}
        else if(KleinStars.capacity(stack)>0){if(!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,1,4,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,stack);return copy;
    }
}
