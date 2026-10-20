package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

/** Shared native input, fuel, nine outputs and sealed waste handling. */
public final class CavernDeviceMenu extends AbstractContainerMenu {
    public static MenuType<CavernDeviceMenu> TYPE;
    private final Container contents;
    private final ContainerData data;
    static void register(){TYPE=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("cavern_device"),new MenuType<>(CavernDeviceMenu::new,FeatureFlags.VANILLA_SET));}
    public CavernDeviceMenu(int id,Inventory inv){this(id,inv,new SimpleContainer(13),new SimpleContainerData(5));}
    CavernDeviceMenu(int id,Inventory inv,Container contents,ContainerData data){
        super(TYPE,id);this.contents=contents;this.data=data;checkContainerSize(contents,13);checkContainerDataCount(data,5);
        addSlot(new Slot(contents,0,17,38){@Override public boolean mayPlace(ItemStack s){return contents.canPlaceItem(0,s);}});addSlot(new Slot(contents,1,17,65){@Override public boolean mayPlace(ItemStack s){return contents.canPlaceItem(1,s);}});
        for(int i=0;i<9;i++)addSlot(new Slot(contents,i+2,107+i%3*18,29+i/3*18){@Override public boolean mayPlace(ItemStack s){return false;}});
        addSlot(new Slot(contents,11,53,65){@Override public boolean mayPlace(ItemStack s){return contents.canPlaceItem(11,s);}});
        addSlot(new Slot(contents,12,77,65){@Override public boolean mayPlace(ItemStack s){return false;}});
        addStandardInventorySlots(inv,8,140);addDataSlots(data);
    }
    public int status(){return data.get(3);}public int kind(){return data.get(4);}public int fuel(){return data.get(0);}public int progress(){return data.get(2)==0?0:Math.clamp(data.get(1)*100/data.get(2),0,100);}
    @Override public boolean stillValid(Player p){return contents.stillValid(p)&&(!(contents instanceof CavernDevice b)||b.getLevel()==p.level()&&Menagerie.mayChange(p.level(),p,b.getBlockPos()));}
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(index<0||index>=slots.size()||!stillValid(p))return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var s=slot.getItem();var before=s.copy();
        boolean moved;
        if(index<13)moved=moveItemStackTo(s,13,49,true);
        else if(s.is(Menagerie.ITEMS.get("cavern_metal_barrel")))moved=moveItemStackTo(s,11,12,false);
        else if(s.is(Menagerie.ITEMS.get("cavern_uranium_rod"))||s.is(Items.REDSTONE)||s.is(Items.SUGAR)||s.is(Items.DIAMOND))moved=moveItemStackTo(s,1,2,false);
        else moved=moveItemStackTo(s,0,1,false);
        if(!moved)return ItemStack.EMPTY;if(s.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,s);return before;
    }
}
