package io.github.jimbozoomer.jugcraft.backpack;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Actual counts never enter a vanilla ItemContainerContents or a carried-slot packet. */
public final class OwnerBackpackInventory extends SimpleContainer {
    public final ItemStack carrier;
    private final Runnable changed;
    private final NonNullList<ItemStack> contents;
    private boolean loading=true;
    public OwnerBackpackInventory(ItemStack carrier,Runnable changed){
        super(OwnerBackpacks.CAPACITY[OwnerBackpacks.tier(carrier)]);this.carrier=carrier;this.changed=changed;
        contents=NonNullList.withSize(getContainerSize(),ItemStack.EMPTY);
        var saved=carrier.get(OwnerBackpacks.CONTENTS);
        if(saved!=null){for(int i=0;i<Math.min(contents.size(),saved.size());i++)contents.set(i,saved.get(i).stack());}
        else carrier.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).copyInto(contents);
        loading=false;
    }
    @Override public ItemStack getItem(int slot){return contents.get(slot);}
    @Override public ItemStack removeItem(int slot,int count){if(OwnerBackpackInfinity.retained(carrier,contents.get(slot)))return contents.get(slot).copyWithCount(Math.min(Math.max(0,count),contents.get(slot).getMaxStackSize()));var result=contents.get(slot).split(Math.max(0,count));if(!result.isEmpty())setChanged();return result;}
    @Override public ItemStack removeItemNoUpdate(int slot){var result=contents.get(slot);contents.set(slot,ItemStack.EMPTY);return result;}
    @Override public void setItem(int slot,ItemStack item){contents.set(slot,item);setChanged();}
    @Override public boolean isEmpty(){return contents.stream().allMatch(ItemStack::isEmpty);}
    @Override public void clearContent(){contents.clear();setChanged();}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return !OwnerBackpackMobs.occupied(carrier,slot)&&OwnerBackpacks.allows(carrier,stack);}
    @Override public void setChanged(){if(!loading){
        carrier.set(OwnerBackpacks.CONTENTS,contents.stream().map(s->new OwnerBackpackStacks.Entry(s,s.getCount())).toList());
        carrier.remove(DataComponents.CONTAINER);changed.run();
    }}
    public ItemStack insert(ItemStack incoming){
        if(!OwnerBackpacks.allows(carrier,incoming))return incoming;
        var left=incoming.copy();int limit=OwnerBackpackStacks.limit(carrier,incoming);
        for(int pass=0;pass<2;pass++)for(int i=0;i<getContainerSize()&&!left.isEmpty();i++){
            if(OwnerBackpackMobs.occupied(carrier,i))continue;
            var present=getItem(i);
            if(pass==0&&!present.isEmpty()&&ItemStack.isSameItemSameComponents(present,left)){
                int moved=Math.min(left.getCount(),Math.max(0,limit-present.getCount()));if(moved>0){present.grow(moved);left.shrink(moved);setChanged();}
            }else if(pass==1&&present.isEmpty()){int moved=Math.min(left.getCount(),limit);setItem(i,left.copyWithCount(moved));left.shrink(moved);}
        }
        return left;
    }
}
