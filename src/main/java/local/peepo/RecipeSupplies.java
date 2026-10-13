package local.peepo;

import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/** Bounded recipe snapshots, up to 128 views per assigned source. Never used by a ticker. */
public final class RecipeSupplies {
    public record Part(Ingredient ingredient,int count){}
    static int viewLimit(Storage<ItemVariant> source){return source instanceof CompanionSupplies.SharedStorage shared?shared.viewLimit:128;}
    public static List<ItemStack> available(Storage<ItemVariant> source){
        var result=new ArrayList<ItemStack>();if(source==null)return result;
        try(var tx=Transaction.openOuter()){
            int views=0;for(var view:source){if(++views>viewLimit(source))break;if(view.isResourceBlank())continue;
                var variant=view.getResource();long taken=view.extract(variant,Math.min(64,view.getAmount()),tx);
                if(taken>0)result.add(variant.toStack((int)taken));
            }
        }
        return result;
    }
    public static boolean completes(List<Part> parts,List<ItemStack> installed,List<ItemStack> supplies){
        if(parts.size()>64)return false;
        // No unrelated installed ingredients may be stranded by switching plans.
        for(var stack:installed)if(!stack.isEmpty() && parts.stream().noneMatch(p->p.ingredient.test(stack)))return false;
        var stacks=new ArrayList<ItemStack>();for(var s:installed)if(!s.isEmpty())stacks.add(s.copy());for(var s:supplies)stacks.add(s.copy());
        // Narrow ingredients first so a broad tag does not consume a uniquely required material.
        var ordered=new ArrayList<>(parts);ordered.sort(Comparator.comparingLong(p->stacks.stream().filter(p.ingredient::test).count()));
        for(var part:ordered){int left=part.count;for(var s:stacks)if(left>0 && part.ingredient.test(s)){int n=Math.min(left,s.getCount());s.shrink(n);left-=n;}if(left>0)return false;}
        return true;
    }
}
