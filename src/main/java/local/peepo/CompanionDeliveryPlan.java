package local.peepo;

import java.util.*;
import java.util.function.BiFunction;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.*;
import net.minecraft.world.item.ItemStack;

/** Bounded pickup-time simulation. The destination is always rolled back before physical pickup. */
final class CompanionDeliveryPlan {
    static List<ItemStack> create(Storage<ItemVariant> source,Storage<ItemVariant> destination,int slots,
            BiFunction<ItemStack,TransactionContext,Integer> needed,CompanionSupplies.Route limits){
        var result=new ArrayList<ItemStack>();
        if(source==null || destination==null || slots<=0)return result;
        var views=new ArrayList<StorageView<ItemVariant>>();
        for(var view:source){if(views.size()==128)break;views.add(view);}
        var sourceCounts=limits!=null && limits.leave>0?counts(source):null;
        var destinationCounts=limits!=null && limits.keep>0?counts(destination):null;
        if(limits!=null && (limits.leave>0 && sourceCounts==null || limits.keep>0 && destinationCounts==null))return result;
        try(var scope=io.github.jimbozoomer.jugcraft.machine.MachineItemAutomation.companionTransfer();var simulation=Transaction.openOuter()){
            // Later inputs may unlock earlier ones (a raw pie enables fuel). At most eight passes.
            for(int pass=0;pass<8;pass++){
                boolean changed=false;
                for(var view:views){
                    if(view.isResourceBlank() || view.getAmount()<=0)continue;
                    var variant=view.getResource();var stack=variant.toStack(1);
                    if(limits!=null && !limits.accepts(stack))continue;
                    ItemStack merge=null;
                    for(var prior:result)if(ItemStack.isSameItemSameComponents(prior,stack) && prior.getCount()<prior.getMaxStackSize()){merge=prior;break;}
                    if(merge==null && result.size()>=slots)continue;
                    int max=stack.getMaxStackSize()-(merge==null?0:merge.getCount());
                    max=(int)Math.min(max,view.getAmount());
                    if(sourceCounts!=null)max=Math.min(max,Math.max(0,sourceCounts.getOrDefault(stack.getItem(),0)-limits.leave));
                    if(destinationCounts!=null)max=Math.min(max,Math.max(0,limits.keep-destinationCounts.getOrDefault(stack.getItem(),0)));
                    if(needed!=null)max=Math.min(max,needed.apply(stack,simulation));
                    if(max<=0)continue;
                    int amount;
                    try(var step=simulation.openNested()){
                        amount=(int)destination.insert(variant,max,step);
                        if(amount<=0 || view.extract(variant,amount,step)!=amount)continue;
                        step.commit();
                    }
                    if(merge!=null)merge.grow(amount);else result.add(stack.copyWithCount(amount));
                    if(sourceCounts!=null)sourceCounts.merge(stack.getItem(),-amount,Integer::sum);
                    if(destinationCounts!=null)destinationCounts.merge(stack.getItem(),amount,Integer::sum);
                    changed=true;
                }
                if(!changed)break;
            }
        }
        return result;
    }
    private static Map<net.minecraft.world.item.Item,Integer> counts(Storage<ItemVariant> storage){
        var result=new HashMap<net.minecraft.world.item.Item,Integer>();int views=0;
        for(var view:storage){
            if(++views>128)return null;
            if(!view.isResourceBlank())result.merge(view.getResource().getItem(),(int)Math.min(1_000_000,view.getAmount()),(a,b)->Math.min(1_000_000,a+b));
        }
        return result;
    }
}
