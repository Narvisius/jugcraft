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
import net.minecraft.world.level.block.entity.BlockEntity;

/** Shared three-slot appliance menu; fuel rules are identical on the client and server. */
public final class HouseholdMenu extends AbstractContainerMenu {
    public static MenuType<HouseholdMenu> HEAT,COLD,CAPSID;
    private final Container inventory;
    private final ContainerData data;
    public final int kind;
    public static void register(){
        HEAT=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("household_heat"),new MenuType<>((id,inv)->new HouseholdMenu(id,inv,0),FeatureFlags.VANILLA_SET));
        COLD=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("household_cold"),new MenuType<>((id,inv)->new HouseholdMenu(id,inv,1),FeatureFlags.VANILLA_SET));
        CAPSID=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("beast_capsid"),new MenuType<>((id,inv)->new HouseholdMenu(id,inv,2),FeatureFlags.VANILLA_SET));
    }
    private HouseholdMenu(int id,Inventory inv,int kind){this(id,inv,new SimpleContainer(3),new SimpleContainerData(4),kind);}
    HouseholdMenu(int id,Inventory inv,Container contents,ContainerData data,int kind){
        super(kind==2?CAPSID:kind==1?COLD:HEAT,id);this.inventory=contents;this.data=data;this.kind=kind;
        checkContainerSize(contents,3);checkContainerDataCount(data,4);
        addSlot(new Slot(contents,0,44,29));
        addSlot(new Slot(contents,1,44,55){@Override public boolean mayPlace(ItemStack s){return fuel(s,inv.player);}});
        addSlot(kind<2?new FurnaceResultSlot(inv.player,contents,2,116,37):new Slot(contents,2,116,37){@Override public boolean mayPlace(ItemStack s){return false;}});
        addStandardInventorySlots(inv,8,84);addDataSlots(data);
    }
    private boolean fuel(ItemStack s,Player player){return kind==2?s.is(Items.AMETHYST_SHARD):kind==1?s.is(Items.SNOWBALL)||s.is(Items.SNOW_BLOCK)||s.is(Items.ICE):s.has(net.minecraft.core.component.DataComponents.COOKING_FUEL);}
    public int progress(int width){return data.get(3)<=0?0:Math.clamp(data.get(2)*width/data.get(3),0,width);}
    public boolean powered(){return data.get(0)>0;}
    @Override public boolean stillValid(Player p){return inventory.stillValid(p)&&(!(inventory instanceof BlockEntity be)||p.level()==be.getLevel()&&Menagerie.mayChange(p.level(),p,be.getBlockPos()));}
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(index<0||index>=slots.size()||!stillValid(p))return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var s=slot.getItem();var before=s.copy();
        boolean moved=index<3?moveItemStackTo(s,3,39,true):fuel(s,p)?moveItemStackTo(s,1,2,false):moveItemStackTo(s,0,1,false);
        if(!moved)return ItemStack.EMPTY;if(index==2)slot.onQuickCraft(s,before);if(s.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,s);return before;
    }
}
