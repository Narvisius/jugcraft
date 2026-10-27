package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.machine.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.entity.*;

/** Bounded accounting of the configured grid; crafting and its remainders stay native. */
final class CompanionCraftingStock {
    record Plan(List<ItemStack> pattern,List<ItemStack> installed,ItemStack result) {
        int missing(ItemStack item,int batches){
            int n=0;
            for(int i=0;i<pattern.size();i++)if(ItemStack.isSameItemSameComponents(pattern.get(i),item))
                n+=Math.max(0,batches-installed.get(i).getCount());
            return n;
        }
        int batches(List<ItemStack> incoming){
            var available=new HashMap<ItemVariant,Integer>();
            for(var stack:incoming)if(!stack.isEmpty())available.merge(ItemVariant.of(stack),stack.getCount(),Integer::sum);
            int low=0,high=64;
            while(low<high){
                int n=(low+high+1)/2;var needs=new HashMap<ItemVariant,Integer>();
                for(int i=0;i<pattern.size();i++)if(!pattern.get(i).isEmpty())
                    needs.merge(ItemVariant.of(pattern.get(i)),Math.max(0,n-installed.get(i).getCount()),Integer::sum);
                if(needs.entrySet().stream().allMatch(e->e.getValue()<=available.getOrDefault(e.getKey(),0)))low=n;else high=n-1;
            }
            return low;
        }
    }
    static boolean supports(BlockEntity block){return block instanceof CraftingTableCompanionBlockEntity || block instanceof CrafterBlockEntity || block instanceof MachineBlockEntity m && m.kind()==MachineKind.AUTO_CRAFTER;}
    static List<ItemStack> inputs(BlockEntity block){
        if(block instanceof CraftingTableCompanionBlockEntity table)return table.installed();
        var inputs=new ArrayList<ItemStack>(9);
        for(int i=0;i<9;i++){
            var stack=block instanceof CrafterBlockEntity c?c.getItem(i):((MachineBlockEntity)block).getItem(i);
            // The Auto-Crafter's last item in each occupied slot is its retained pattern, not a pending craft.
            inputs.add(stack.copyWithCount(Math.max(0,stack.getCount()-(block instanceof MachineBlockEntity?1:0))));
        }
        return inputs;
    }
    static Plan plan(BlockEntity block){
        if(!supports(block) || !(block.getLevel() instanceof ServerLevel level))return null;
        if(block instanceof CraftingTableCompanionBlockEntity table){var plan=table.plan();return plan==null?null:new Plan(plan.pattern(),table.installed(),plan.result());}
        List<ItemStack> pattern;
        if(block instanceof CrafterBlockEntity){pattern=VanillaCompanionPort.of(block).craftingPattern();if(pattern.isEmpty())return null;}
        else{pattern=new ArrayList<>(9);for(int i=0;i<9;i++)pattern.add(((MachineBlockEntity)block).getItem(i).copyWithCount(1));}
        var grid=CraftingInput.of(3,3,pattern);
        var result=CrafterBlock.getPotentialResults(level,grid).map(r->r.value().assemble(grid)).orElse(ItemStack.EMPTY);
        return result.isEmpty()?null:new Plan(pattern,inputs(block),result);
    }
    static int limit(PeepoEntity npc,BlockEntity block,ItemStack raw,int nativeNeed,CompanionProductionStock stock){
        var plan=plan(block);if(plan==null || !CompanionFilters.get(block).allows(plan.result))return 0;
        int goal=stock.inputGoal(npc,block,plan.result);if(goal==Integer.MAX_VALUE)return nativeNeed;
        int batches=(goal+plan.result.getCount()-1)/plan.result.getCount();
        int incoming=stock.incoming().stream().filter(s->ItemStack.isSameItemSameComponents(s,raw)).mapToInt(ItemStack::getCount).sum();
        return Math.min(nativeNeed,Math.max(0,plan.missing(raw,batches)-incoming));
    }
}
