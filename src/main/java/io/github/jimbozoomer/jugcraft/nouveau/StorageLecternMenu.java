package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** A page is a live view of one real chest, never an inventory snapshot that can be written back twice. */
public final class StorageLecternMenu extends AbstractContainerMenu {
    public static final int SLOTS=27;
    private final Player viewer;
    private final @Nullable StorageLectern lectern;
    private final Container contents;
    private final ContainerData data=new SimpleContainerData(4);
    public StorageLecternMenu(int id,Inventory inv){this(id,inv,null);}
    public StorageLecternMenu(int id,Inventory inv,@Nullable StorageLectern lectern){
        super(JugNouveau.MENU,id);this.viewer=inv.player;this.lectern=lectern;
        contents=lectern==null?new SimpleContainer(SLOTS):new LiveChest();
        for(int row=0;row<3;row++)for(int col=0;col<9;col++){
            final int index=row*9+col;
            addSlot(new Slot(contents,index,20+col*18,44+row*18){
                @Override public boolean mayPlace(ItemStack stack){return stillValid(viewer)&&contents.canPlaceItem(index,stack);}
                @Override public boolean mayPickup(Player p){return stillValid(p)&&available(index);}
            });
        }
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,9+row*9+col,20+col*18,115+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,20+col*18,173));
        refresh();addDataSlots(data);
    }
    public int page(){return data.get(0);}
    public int pages(){return data.get(1);}
    public int wyrms(){return data.get(2);}
    public boolean online(){return data.get(3)>0;}
    private @Nullable Container target(){
        return lectern!=null&&viewer instanceof ServerPlayer p&&stillValid(viewer)?lectern.linkedContainer(p,page()):null;
    }
    private boolean available(int slot){var c=target();return lectern==null?online():c!=null&&slot>=0&&slot<c.getContainerSize();}
    private void refresh(){
        if(lectern==null)return;
        data.set(1,lectern.links().size());data.set(0,Math.clamp(page(),0,Math.max(0,pages()-1)));data.set(2,lectern.wyrms().size());data.set(3,target()!=null?1:0);
    }
    @Override public void broadcastChanges(){refresh();super.broadcastChanges();}
    @Override public boolean stillValid(Player p){
        if(p!=viewer||!p.isAlive()||p.isSpectator())return false;
        if(!(p instanceof ServerPlayer server))return true;
        return lectern!=null&&!lectern.isRemoved()&&lectern.getLevel()==p.level()&&p.level().isLoaded(lectern.getBlockPos())
            &&p.level().getBlockEntity(lectern.getBlockPos())==lectern&&p.distanceToSqr(Vec3.atCenterOf(lectern.getBlockPos()))<=64&&lectern.mayUse(server);
    }
    @Override public boolean clickMenuButton(Player p,int action){
        if(!(p instanceof ServerPlayer server)||!stillValid(p)||lectern==null||action<0||action>2||!getCarried().isEmpty()
            ||!RateGate.allow(server,"nouveau_page",4))return false;
        if(action==0&&page()>0)data.set(0,page()-1);
        else if(action==1&&page()+1<pages())data.set(0,page()+1);
        else if(action==2&&page()<lectern.links().size())lectern.unlink(lectern.links().get(page()));
        else return false;
        refresh();broadcastFullState();return true;
    }
    @Override public void clicked(int slot,int button,ContainerInput input,Player p){if(stillValid(p))super.clicked(slot,button,input,p);}
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(!stillValid(p)||index<0||index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.hasItem()||!slot.mayPickup(p))return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),copy=stack.copy();
        if(index<SLOTS){if(!moveItemStackTo(stack,SLOTS,slots.size(),true))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,0,SLOTS,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,stack);return copy;
    }
    private final class LiveChest implements Container {
        @Override public int getContainerSize(){return SLOTS;}
        @Override public boolean isEmpty(){var c=target();return c==null||c.isEmpty();}
        @Override public ItemStack getItem(int slot){var c=target();return c!=null&&slot>=0&&slot<c.getContainerSize()?c.getItem(slot):ItemStack.EMPTY;}
        @Override public ItemStack removeItem(int slot,int count){var c=target();return c!=null&&slot>=0&&slot<c.getContainerSize()?c.removeItem(slot,count):ItemStack.EMPTY;}
        @Override public ItemStack removeItemNoUpdate(int slot){var c=target();return c!=null&&slot>=0&&slot<c.getContainerSize()?c.removeItemNoUpdate(slot):ItemStack.EMPTY;}
        @Override public void setItem(int slot,ItemStack stack){var c=target();if(c!=null&&slot>=0&&slot<c.getContainerSize())c.setItem(slot,stack);}
        @Override public boolean canPlaceItem(int slot,ItemStack stack){var c=target();return c!=null&&slot>=0&&slot<c.getContainerSize()&&c.canPlaceItem(slot,stack);}
        @Override public void setChanged(){var c=target();if(c!=null)c.setChanged();}
        @Override public boolean stillValid(Player p){return StorageLecternMenu.this.stillValid(p);}
        @Override public void clearContent(){/* A view never owns or discards the linked chest's contents. */}
    }
}
