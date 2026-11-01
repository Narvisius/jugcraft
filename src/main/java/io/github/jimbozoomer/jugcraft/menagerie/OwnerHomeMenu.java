package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class OwnerHomeMenu extends AbstractContainerMenu {
    public static MenuType<OwnerHomeMenu> COUNTER, DRAWER;
    public final int capacity;
    private final Container contents;
    static void register() {
        COUNTER=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("owner_home_counter"),new MenuType<>((id,inv)->new OwnerHomeMenu(id,inv,new SimpleContainer(24)),FeatureFlags.VANILLA_SET));
        DRAWER=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("owner_home_drawer"),new MenuType<>((id,inv)->new OwnerHomeMenu(id,inv,new SimpleContainer(9)),FeatureFlags.VANILLA_SET));
    }
    OwnerHomeMenu(int id, Inventory inv, Container content) {
        super(content.getContainerSize()==24?COUNTER:DRAWER,id);contents=content;capacity=content.getContainerSize();content.startOpen(inv.player);
        int width=capacity==24?8:3, start=capacity==24?17:62;
        for(int i=0;i<capacity;i++)addSlot(new Slot(content,i,start+i%width*18,20+i/width*18));
        addStandardInventorySlots(inv,8,88);
    }
    @Override public boolean stillValid(Player p) { return contents.stillValid(p); }
    @Override public void removed(Player p) { super.removed(p);contents.stopOpen(p); }
    @Override public ItemStack quickMoveStack(Player p,int i) {
        if(!stillValid(p)||i<0||i>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(i);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var copy=stack.copy();
        if(!(i<capacity?moveItemStackTo(stack,capacity,slots.size(),true):moveItemStackTo(stack,0,capacity,false)))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,stack);return copy;
    }
}
