package local.peepo;

import io.github.jimbozoomer.jugcraft.agriculture.*;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerLevel;

/** Shared server validation for the inventory's output filter editor. */
public final class CompanionFilters {
    public static CompanionRecipeFilter get(BlockEntity block){
        if(block instanceof CraftingTableCompanionBlockEntity table)return table.filter;
        var serving=ServingCompanion.of(block);if(serving!=null)return serving.filter;
        if(block instanceof CiderPressBlockEntity b)return b.companionFilter;
        if(block instanceof CanningKettleBlockEntity b)return b.companionFilter;
        if(block instanceof SpinningWheelBlockEntity b)return b.companionFilter;
        if(block instanceof io.github.jimbozoomer.jugcraft.concordance.garden.MulchMawBlockEntity maw)return maw.companionFilter;
        if(block instanceof FarmStandBlockEntity stand)return stand.companion.filter;
        if(block instanceof io.github.jimbozoomer.jugcraft.concordance.CrucibleBlockEntity crucible)return crucible.companion.filter;
        if(block instanceof io.github.jimbozoomer.jugcraft.workshop.WorkshopBlockEntity workshop)return workshop.companion.filter;
        var vanilla=VanillaCompanionPort.of(block);if(vanilla!=null)return vanilla.filter;
        if(block instanceof CandyKettleBlockEntity kettle)return kettle.companion.filter;
        if(block instanceof CiderBarrelBlockEntity barrel)return barrel.filter();
        if(block instanceof HearthOvenBlockEntity oven)return oven.filter();
        if(block instanceof CookingPotBlockEntity pot)return pot.filter();
        if(block instanceof io.github.jimbozoomer.jugcraft.machine.form.FormMachineBlockEntity form)return form.companionPort.filter;
        if(block instanceof MachineBlockEntity machine)return machine.companionPort.filter();
        var kitchen=KitchenCompanionPort.of(block);return kitchen==null?null:kitchen.filter;
    }
    public static boolean set(BlockEntity block,int slot,ItemStack icon){
        if(slot<0 || slot>=9 || !(block.getLevel() instanceof ServerLevel level))return false;
        ItemStack value=icon;
        if(block instanceof CraftingTableCompanionBlockEntity table){
            if(!table.acceptsFilter(icon))return false;
        }else if(block instanceof io.github.jimbozoomer.jugcraft.machine.form.FormMachineBlockEntity form){
            if(!form.form().companionAssistance() || !form.companionPort.acceptsFilter(icon))return false;
        }else if(block instanceof io.github.jimbozoomer.jugcraft.workshop.WorkshopBlockEntity workshop){
            if(!workshop.companion.acceptsFilter(icon))return false;
        }else if(block instanceof VanillaCompanionAccess access){if(!access.peepo$port().acceptsFilter(icon))return false;
        }else if(block instanceof io.github.jimbozoomer.jugcraft.concordance.garden.MulchMawBlockEntity){
            if(!icon.isEmpty() && io.github.jimbozoomer.jugcraft.concordance.garden.Garden.mulch(icon)<=0)return false;
        }else if(ServingCompanion.of(block) instanceof ServingCompanion serving){
            if(!serving.acceptsFilter(icon))return false;
        }else if(block instanceof CiderPressBlockEntity){
            if(!icon.isEmpty() && !icon.is(JugcraftAgriculture.item("sweet_cider")))return false;
        }else if(block instanceof CanningKettleBlockEntity){
            if(!icon.isEmpty() && !(icon.getItem() instanceof PreserveJarItem))return false;
            if(!icon.isEmpty()){value=icon.copyWithCount(1);PreserveJarItem.seal(value);}
        }else if(block instanceof SpinningWheelBlockEntity){
            if(!icon.isEmpty() && !icon.is(Knitting.yarn(0,1).getItem()))return false;
            if(!icon.isEmpty() && java.util.Arrays.stream(net.minecraft.world.item.DyeColor.values()).noneMatch(c->(c.getTextureDiffuseColor() & 0xFFFFFF)==Knitting.color(icon)))return false;
        }else if(block instanceof FarmStandBlockEntity){
            // Stock templates determine exact variants; this filter only limits which goods to restock.
        }else if(block instanceof io.github.jimbozoomer.jugcraft.concordance.CrucibleBlockEntity){
            if(!CrucibleCompanion.acceptsFilter(icon))return false;
        }else if(block instanceof CandyKettleBlockEntity){
            if(!CandyKettleCompanion.acceptsFilter(icon))return false;
        }else if(block instanceof CiderBarrelBlockEntity){
            if(!icon.isEmpty() && !icon.is(JugcraftAgriculture.item("sparkling_cider")) && !icon.is(JugcraftAgriculture.item("aged_cider")))return false;
        }else if(block instanceof HearthOvenBlockEntity){
            var filling=HearthOvenBlockEntity.selectedFilling(icon);if(!icon.isEmpty() && filling==null)return false;
            value=filling==null?ItemStack.EMPTY:new ItemStack(JugcraftAgriculture.item(filling.pie()));
        }else if(block instanceof CookingPotBlockEntity){
            if(!icon.isEmpty() && CookingPotRecipe.catalog(level.getServer()).values().stream().noneMatch(r->r.output().create().is(icon.getItem())))return false;
        }else if(block instanceof MachineBlockEntity machine){if(!machine.companionPort.acceptsFilter(icon))return false;}
        else {var kitchen=KitchenCompanionPort.of(block);if(kitchen==null || !kitchen.acceptsFilter(icon))return false;}
        var filter=get(block);if(filter==null)return false;filter.set(slot,value);
        if(block instanceof CraftingTableCompanionBlockEntity table)table.filterChanged();
        if(block instanceof CookingPotBlockEntity pot)pot.companionFilterChanged();
        if(block instanceof MachineBlockEntity machine)machine.companionPort.filterChanged();
        if(block instanceof io.github.jimbozoomer.jugcraft.machine.form.FormMachineBlockEntity form)form.companionPort.filterChanged();
        if(block instanceof CandyKettleBlockEntity kettle)kettle.companion.filterChanged();
        if(block instanceof VanillaCompanionAccess access)access.peepo$port().filterChanged();
        CompanionReadiness.invalidate(block);block.setChanged();return true;
    }
}
