package io.github.jimbozoomer.jugcraft.menagerie;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
public final class OwnerSupplementMenu extends AbstractContainerMenu {
 public static final Map<Integer,MenuType<OwnerSupplementMenu>> TYPES=new LinkedHashMap<>();
 public static MenuType<OwnerSupplementMenu> NOTICE;
 public final Container source;public final int capacity;public final boolean notice;
 static void register(){for(int size:new int[]{1,6,9,12,27})TYPES.put(size,Registry.register(BuiltInRegistries.MENU,Jugcraft.id("supps_storage_"+size),new MenuType<>((id,inv)->new OwnerSupplementMenu(id,inv,new SimpleContainer(size),false),FeatureFlags.VANILLA_SET)));NOTICE=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("supps_notice"),new MenuType<>((id,inv)->new OwnerSupplementMenu(id,inv,new SimpleContainer(1),true),FeatureFlags.VANILLA_SET));}
 public OwnerSupplementMenu(int id,Inventory inv,Container source,boolean notice){
  super(notice?NOTICE:TYPES.get(source.getContainerSize()),id);this.source=source;this.capacity=source.getContainerSize();this.notice=notice;source.startOpen(inv.player);
  int columns=Math.min(9,capacity),start=(176-columns*18)/2;
  for(int i=0;i<capacity;i++)addSlot(new Slot(source,i,notice?-1000:start+i%columns*18,notice?-1000:22+i/columns*18){@Override public boolean mayPlace(ItemStack s){return !notice&&source.canPlaceItem(getContainerSlot(),s);}@Override public boolean mayPickup(Player p){return !notice&&super.mayPickup(p);}});
  if(!notice)addStandardInventorySlots(inv,8,88);
 }
 @Override public boolean stillValid(Player p){return source.stillValid(p);}
 @Override public void removed(Player p){super.removed(p);source.stopOpen(p);}
 @Override public ItemStack quickMoveStack(Player p,int slot){if(notice||!stillValid(p)||slot<0||slot>=slots.size())return ItemStack.EMPTY;var s=slots.get(slot);if(!s.hasItem())return ItemStack.EMPTY;var stack=s.getItem();var copy=stack.copy();if(!(slot<capacity?moveItemStackTo(stack,capacity,slots.size(),true):moveItemStackTo(stack,0,capacity,false)))return ItemStack.EMPTY;if(stack.isEmpty())s.setByPlayer(ItemStack.EMPTY);else s.setChanged();s.onTake(p,stack);return copy;}
}
