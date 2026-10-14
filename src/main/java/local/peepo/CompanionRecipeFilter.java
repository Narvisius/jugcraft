package local.peepo;

import java.util.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.*;

/** Nine output allow-list entries. Ghosts retain item IDs only, plus potion contents for brewing; never real cargo. */
public final class CompanionRecipeFilter {
    private final ItemStack[] items=new ItemStack[9];
    private final boolean potions;
    private boolean initialized;
    private int revision;
    public int revision(){return revision;}
    public CompanionRecipeFilter(){this(false);}
    public CompanionRecipeFilter(boolean potions){this.potions=potions;Arrays.fill(items,ItemStack.EMPTY);}
    public void initialize(ItemStack legacy){if(!initialized){initialized=true;if(!legacy.isEmpty())items[0]=new ItemStack(legacy.getItem());}}
    public ItemStack get(int slot){return items[slot].copy();}
    public boolean empty(){for(var item:items)if(!item.isEmpty())return false;return true;}
    public boolean allows(ItemStack output){return empty() || Arrays.stream(items).anyMatch(i->!i.isEmpty() && i.is(output.getItem()) && (!potions || java.util.Objects.equals(i.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS),output.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS))));}
    public void set(int slot,ItemStack item){revision++;initialized=true;items[slot]=item.isEmpty()?ItemStack.EMPTY:new ItemStack(item.getItem());if(potions && !item.isEmpty() && item.has(net.minecraft.core.component.DataComponents.POTION_CONTENTS))items[slot].set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,item.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS));}
    public void save(ValueOutput out){if(initialized)out.store("CompanionFilter",ItemStack.OPTIONAL_CODEC.listOf(),Arrays.asList(items));}
    public void load(ValueInput in){Arrays.fill(items,ItemStack.EMPTY);var saved=in.read("CompanionFilter",ItemStack.OPTIONAL_CODEC.listOf());initialized=saved.isPresent();saved.ifPresent(list->{for(int i=0;i<Math.min(9,list.size());i++)set(i,list.get(i));});}
}
