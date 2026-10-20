package io.github.jimbozoomer.jugcraft.menagerie;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Uses vanilla's saved cooking progress, inventory, sided automation and synchronized furnace menu. */
public final class HouseholdAppliance extends AbstractFurnaceBlockEntity {
    private final boolean freezer;
    public HouseholdAppliance(BlockPos p,BlockState s){
        super(Household.APPLIANCE,p,s,HouseholdCooking.TYPES.get(((HouseholdBlock)s.getBlock()).kind));
        freezer=((HouseholdBlock)s.getBlock()).kind.equals("freezer");
    }
    @Override protected Component getDefaultName(){return Component.translatable(getBlockState().getBlock().getDescriptionId());}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory){return new HouseholdMenu(id,inventory,this,dataAccess,freezer?1:0);}
    @Override protected int getBurnDuration(ServerLevel l,ItemStack fuel){
        if(!freezer)return super.getBurnDuration(l,fuel);
        return fuel.is(Items.SNOWBALL)?400:fuel.is(Items.SNOW_BLOCK)?1600:fuel.is(Items.ICE)?3200:0;
    }
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return slot==1&&freezer?stack.is(Items.SNOWBALL)||stack.is(Items.SNOW_BLOCK)||stack.is(Items.ICE):super.canPlaceItem(slot,stack);}
}
