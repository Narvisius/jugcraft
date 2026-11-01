package io.github.jimbozoomer.jugcraft.arcane;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class ArcaneClibanoMenu extends AbstractContainerMenu {
    private final Container contents;
    private final ContainerData data;
    public ArcaneClibanoMenu(int id,Inventory inv){this(id,inv,new SimpleContainer(7),new SimpleContainerData(18));}
    public ArcaneClibanoMenu(int id,Inventory inv,ArcaneClibano furnace){this(id,inv,furnace,new ContainerData(){public int get(int i){return furnace.data(i/2)>>>((i%2)*16)&65535;}public void set(int i,int v){}public int getCount(){return 18;}});}
    private ArcaneClibanoMenu(int id,Inventory inv,Container contents,ContainerData data){
        super(ForbiddenArcane.CLIBANO_MENU,id);this.contents=contents;this.data=data;addDataSlots(data);
        int[][] points={{18,20},{18,56},{53,56},{44,20},{62,20},{116,32},{142,28}};
        for(int i=0;i<7;i++){int slot=i;addSlot(new Slot(contents,i,points[i][0],points[i][1]){
            @Override public boolean mayPlace(ItemStack s){return slot<5&&contents.canPlaceItem(slot,s);}
            @Override public int getMaxStackSize(){return slot==0?1:64;}
            @Override public void onTake(Player player,ItemStack stack){super.onTake(player,stack);if(slot>=5&&player instanceof ServerPlayer p&&contents instanceof ArcaneClibano c)c.awardExperience(p);}
        });}
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,col+row*9+9,8+col*18,91+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+col*18,149));
    }
    public int value(int i){return data.get(i*2)&65535|(data.get(i*2+1)&65535)<<16;}
    @Override public boolean stillValid(Player p){return contents.stillValid(p);}
    @Override public boolean clickMenuButton(Player p,int button){return button==0&&p instanceof ServerPlayer sp&&contents instanceof ArcaneClibano c&&c.collectResidues(sp);}
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var original=stack.copy();
        if(index<7){if(!moveItemStackTo(stack,7,43,true))return ItemStack.EMPTY;}
        else {int start=ArcaneEnhancers.accepts(stack)?0:ArcaneClibano.soulType(stack)>0?1:stack.has(net.minecraft.core.component.DataComponents.COOKING_FUEL)?2:3;if(!moveItemStackTo(stack,start,start==3?5:start+1,false))return ItemStack.EMPTY;}
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,stack);return original;
    }
}
