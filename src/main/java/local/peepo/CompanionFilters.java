package local.peepo;

import io.github.jimbozoomer.jugcraft.agriculture.*;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerLevel;

/** Shared server validation for the inventory's output filter editor. */
public final class CompanionFilters {
    public static CompanionRecipeFilter get(BlockEntity block){
        if(block instanceof HearthOvenBlockEntity oven)return oven.filter();
        if(block instanceof CookingPotBlockEntity pot)return pot.filter();
        if(block instanceof MachineBlockEntity machine)return machine.companionPort.filter();
        var kitchen=KitchenCompanionPort.of(block);return kitchen==null?null:kitchen.filter;
    }
    public static boolean set(BlockEntity block,int slot,ItemStack icon){
        if(slot<0 || slot>=9 || !(block.getLevel() instanceof ServerLevel level))return false;
        ItemStack value=icon;
        if(block instanceof HearthOvenBlockEntity){
            var filling=HearthOvenBlockEntity.selectedFilling(icon);if(!icon.isEmpty() && filling==null)return false;
            value=filling==null?ItemStack.EMPTY:new ItemStack(JugcraftAgriculture.item(filling.pie()));
        }else if(block instanceof CookingPotBlockEntity){
            if(!icon.isEmpty() && CookingPotRecipe.catalog(level.getServer()).values().stream().noneMatch(r->r.output().create().is(icon.getItem())))return false;
        }else if(block instanceof MachineBlockEntity machine){if(!machine.companionPort.acceptsFilter(icon))return false;}
        else {var kitchen=KitchenCompanionPort.of(block);if(kitchen==null || !kitchen.acceptsFilter(icon))return false;}
        var filter=get(block);if(filter==null)return false;filter.set(slot,value);
        if(block instanceof CookingPotBlockEntity pot)pot.companionFilterChanged();
        if(block instanceof MachineBlockEntity machine)machine.companionPort.filterChanged();
        block.setChanged();return true;
    }
}
