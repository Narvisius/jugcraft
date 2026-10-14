package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotRecipe;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

/** Balanced bulk pot loads, planned once at pickup with bounded, rolled-back storage snapshots. */
final class CookingPotSupplies {
    static int missing(CookingPotBlockEntity pot,ItemStack candidate){
        var plan=pot.supplyPlan().orElse(null);if(plan==null)return 0;
        int[] counts=new int[CookingPotBlockEntity.INPUTS];
        for(int i=0;i<counts.length;i++)counts[i]=pot.getItem(i).getCount();
        int missing=0;
        for(var part:plan.ingredients()){
            int left=part.count();
            for(int i=0;i<counts.length && left>0;i++)if(part.ingredient().test(pot.getItem(i))){
                int take=Math.min(left,counts[i]);left-=take;counts[i]-=take;
            }
            if(part.ingredient().test(candidate))missing=Math.min(candidate.getMaxStackSize(),missing+Math.min(candidate.getMaxStackSize(),left));
        }
        return missing;
    }
    static List<ItemStack> create(CookingPotBlockEntity pot,Storage<ItemVariant> source,Storage<ItemVariant> linked,int slots){
        var plan=pot.supplyPlan().orElse(null);
        if(plan==null || source==null || slots<=0 || plan.ingredients().size()>64)return List.of();
        var installed=new ArrayList<ItemStack>();
        for(int i=0;i<CookingPotBlockEntity.INPUTS;i++)installed.add(pot.getItem(i).copy());
        for(var stack:installed)if(!stack.isEmpty() && plan.ingredients().stream().noneMatch(p->p.ingredient().test(stack)))return List.of();
        var available=RecipeSupplies.available(linked==null?source:linked);
        var local=RecipeSupplies.available(source);
        var parts=new ArrayList<>(plan.ingredients());
        // Reserve narrow ingredients before broad alternatives, consuming installed stock first.
        parts.sort(Comparator.comparingLong(p->available.stream().filter(p.ingredient()::test).count()
            +installed.stream().filter(p.ingredient()::test).count()));
        long units=parts.stream().mapToLong(CookingPotRecipe.Part::count).sum();
        if(units<=0 || units>384)return List.of();
        var inputs=ContainerStorage.of(pot,Direction.UP);
        List<ItemStack> best=List.of();
        // Six 64-item input slots bound the search to at most nine candidate batch sizes.
        int low=1,high=(int)(384/units);
        while(low<=high){
            int batches=(low+high)/2;
            var wanted=allocate(parts,installed,available,batches);
            var pickup=wanted==null?null:localShare(wanted,local,slots);
            if(pickup!=null && fits(inputs,wanted)){
                best=pickup;low=batches+1;
            }else high=batches-1;
        }
        // Keep partial, multi-visit recipes possible if another ingredient has just run out.
        return best.isEmpty()?CompanionDeliveryPlan.create(source,inputs,slots,(stack,tx)->missing(pot,stack),null):best;
    }
    private static List<ItemStack> allocate(List<CookingPotRecipe.Part> parts,List<ItemStack> installed,List<ItemStack> available,int batches){
        var stock=new ArrayList<ItemStack>();
        for(var stack:installed)stock.add(stack.copy());
        for(var stack:available)stock.add(stack.copy());
        var wanted=new ArrayList<ItemStack>();
        for(var part:parts){
            int left=part.count()*batches;
            for(int i=0;i<stock.size() && left>0;i++){
                var stack=stock.get(i);if(stack.isEmpty() || !part.ingredient().test(stack))continue;
                int take=Math.min(left,stack.getCount());
                if(i>=installed.size())add(wanted,stack,take);
                stack.shrink(take);left-=take;
            }
            if(left>0)return null;
        }
        return wanted;
    }
    private static List<ItemStack> localShare(List<ItemStack> wanted,List<ItemStack> available,int slots){
        var remaining=available.stream().map(ItemStack::copy).toList();
        var result=new ArrayList<ItemStack>();
        for(var need:wanted){
            int left=need.getCount();
            for(var stack:remaining)if(left>0 && ItemStack.isSameItemSameComponents(stack,need)){
                int room=(slots-result.size())*stack.getMaxStackSize();
                for(var prior:result)if(ItemStack.isSameItemSameComponents(prior,stack))room+=prior.getMaxStackSize()-prior.getCount();
                int take=Math.min(room,Math.min(left,stack.getCount()));
                if(take<=0)continue;
                add(result,stack,take);stack.shrink(take);left-=take;
            }
        }
        return result;
    }
    private static boolean fits(Storage<ItemVariant> inputs,List<ItemStack> wanted){
        try(var tx=Transaction.openOuter()){
            for(var stack:wanted)if(inputs.insert(ItemVariant.of(stack),stack.getCount(),tx)!=stack.getCount())return false;
            return true;
        }
    }
    private static void add(List<ItemStack> result,ItemStack stack,int amount){
        for(var prior:result)if(ItemStack.isSameItemSameComponents(prior,stack)){
            int n=Math.min(amount,prior.getMaxStackSize()-prior.getCount());prior.grow(n);amount-=n;
            if(amount==0)return;
        }
        while(amount>0){int n=Math.min(amount,stack.getMaxStackSize());result.add(stack.copyWithCount(n));amount-=n;}
    }
}
