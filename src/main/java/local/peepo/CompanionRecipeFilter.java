package local.peepo;

import java.util.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.*;

/** Nine output allow-list entries. Only station-approved identity components survive; ghosts are never cargo. */
public final class CompanionRecipeFilter {
    private final ItemStack[] items=new ItemStack[9];
    private final List<net.minecraft.core.component.DataComponentType<?>> identity;
    public final CompanionProductionStock stock=new CompanionProductionStock(this);
    private boolean initialized;
    private int revision;
    public int revision(){return revision;}
    public CompanionRecipeFilter(){this(false);}
    public CompanionRecipeFilter(boolean potions){this(potions?List.of(net.minecraft.core.component.DataComponents.POTION_CONTENTS):List.of());}
    public CompanionRecipeFilter(List<net.minecraft.core.component.DataComponentType<?>> identity){this.identity=List.copyOf(identity);Arrays.fill(items,ItemStack.EMPTY);}
    public void initialize(ItemStack legacy){if(!initialized){initialized=true;if(!legacy.isEmpty())items[0]=new ItemStack(legacy.getItem());}}
    public ItemStack get(int slot){return items[slot].copy();}
    /** The same identity used by ghost filters, excluding names/freshness but preserving potion type. */
    public net.fabricmc.fabric.api.transfer.v1.item.ItemVariant stockKey(ItemStack stack){
        if(stack.isEmpty())return net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.blank();
        var key=new ItemStack(stack.getItem());
        for(var component:identity)copyComponent(stack,key,component);
        return net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(key);
    }
    private static <T> void copyComponent(ItemStack from,ItemStack to,net.minecraft.core.component.DataComponentType<T> type){var value=from.get(type);if(value!=null)to.set(type,value);}
    public boolean empty(){for(var item:items)if(!item.isEmpty())return false;return true;}
    public boolean allows(ItemStack output){return empty() || Arrays.stream(items).anyMatch(i->!i.isEmpty() && i.is(output.getItem()) && identity.stream().allMatch(c->Objects.equals(i.get(c),output.get(c))));}
    public void set(int slot,ItemStack item){revision++;initialized=true;items[slot]=stockKey(item).toStack(1);stock.filterChanged();}
    public void save(ValueOutput out){if(initialized)out.store("CompanionFilter",ItemStack.OPTIONAL_CODEC.listOf(),Arrays.asList(items));stock.save(out);}
    public void load(ValueInput in){Arrays.fill(items,ItemStack.EMPTY);var saved=in.read("CompanionFilter",ItemStack.OPTIONAL_CODEC.listOf());initialized=saved.isPresent();saved.ifPresent(list->{for(int i=0;i<Math.min(9,list.size());i++)set(i,list.get(i));});stock.load(in);}
}
