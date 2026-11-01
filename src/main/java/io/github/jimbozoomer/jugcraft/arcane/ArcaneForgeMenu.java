package io.github.jimbozoomer.jugcraft.arcane;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class ArcaneForgeMenu extends AbstractContainerMenu {
    private final Container contents;
    private final ContainerData data;
    public ArcaneForgeMenu(int id,Inventory inv){this(id,inv,new SimpleContainer(9),new SimpleContainerData(10));}
    public ArcaneForgeMenu(int id,Inventory inv,ArcaneWorkshop workshop){this(id,inv,workshop,new ContainerData(){
        public int get(int index){return index<8?(workshop.essence(index/2)>>>((index%2)*16))&65535:index==8?workshop.tier():workshop.running()?Math.max(1,(int)(workshop.progress()*1000)):0;}
        public void set(int index,int value){}
        public int getCount(){return 10;}
    });}
    private ArcaneForgeMenu(int id,Inventory inv,Container contents,ContainerData data){
        super(ForbiddenArcane.FORGE_MENU,id);this.contents=contents;this.data=data;addDataSlots(data);
        int[][] points={{36,24},{36,46},{124,24},{124,46},{80,24},{-18,25},{-18,43},{178,25},{178,43}};
        for(int i=0;i<9;i++){int index=i;addSlot(new Slot(contents,i,points[i][0],points[i][1]){
            @Override public boolean mayPlace(ItemStack stack){return !running()&&contents.canPlaceItem(index,stack)&&(index<Math.min(4,tier())&&ArcaneEnhancers.accepts(stack)||index==4||index>=5&&ArcaneEssences.kind(stack)==index-5);}
            @Override public boolean mayPickup(Player p){return !running()&&super.mayPickup(p);}
            @Override public int getMaxStackSize(){return index<=4?1:64;}
        });}
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,col+row*9+9,8+col*18,84+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+col*18,142));
    }
    public int essence(int type){return (data.get(type*2)&65535)|((data.get(type*2+1)&65535)<<16);}
    public int tier(){return Math.clamp(data.get(8),1,5);}
    public boolean running(){return data.get(9)>0;}
    public int progress(){return Math.clamp(data.get(9),0,1000);}
    @Override public boolean stillValid(Player p){return contents.stillValid(p);}
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(index<0||index>=slots.size()||running())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem()||!slot.mayPickup(p))return ItemStack.EMPTY;
        var stack=slot.getItem();var original=stack.copy();
        if(index<9){if(!moveItemStackTo(stack,9,45,true))return ItemStack.EMPTY;}
        else{int type=ArcaneEssences.kind(stack);if(type>=0){if(!moveItemStackTo(stack,type+5,type+6,false))return ItemStack.EMPTY;}else if(ArcaneEnhancers.accepts(stack)){if(!moveItemStackTo(stack,0,Math.min(4,tier()),false))return ItemStack.EMPTY;}else if(!moveItemStackTo(stack,4,5,false))return ItemStack.EMPTY;}
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,stack);return original;
    }
}
