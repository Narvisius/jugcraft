package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import io.github.jimbozoomer.jugcraft.machine.*;
import io.github.jimbozoomer.jugcraft.machine.form.FormMachineBlockEntity;
import io.github.jimbozoomer.jugcraft.workshop.WorkshopBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.entity.*;

/** Stock accounting for native cooking and recipe processors. No new processing loop or inventory. */
final class CompanionStockRecipes {
    record Recipe(List<RecipeSupplies.Part> parts,List<ItemStack> results) {
        boolean ingredient(ItemStack stack){return parts.stream().anyMatch(p->p.ingredient().test(stack));}
    }
    static boolean supports(BlockEntity block){
        return block instanceof SkilletBlockEntity || block instanceof KitchenStoveBlockEntity
            || block instanceof AbstractFurnaceBlockEntity || block instanceof CampfireBlockEntity
            || block instanceof WorkshopBlockEntity || CompanionCraftingStock.supports(block)
            || block instanceof FormMachineBlockEntity form && form.companionPort.stockTargets()
            || block instanceof MachineBlockEntity machine && machine.companionPort.supportsStockTargets();
    }
    private static boolean cooking(BlockEntity block){return block instanceof SkilletBlockEntity || block instanceof KitchenStoveBlockEntity || block instanceof AbstractFurnaceBlockEntity || block instanceof CampfireBlockEntity;}
    private static <T extends AbstractCookingRecipe> ItemStack cooked(ServerLevel level,RecipeType<T> type,ItemStack raw){
        if(raw.isEmpty())return ItemStack.EMPTY;
        var input=new SingleRecipeInput(raw.copyWithCount(1));
        return level.getServer().getRecipeManager().getRecipeFor(type,input,level).map(r->r.value().assemble(input)).orElse(ItemStack.EMPTY);
    }
    static ItemStack cooked(BlockEntity block,ItemStack raw){
        if(!(block.getLevel() instanceof ServerLevel level))return ItemStack.EMPTY;
        if(block instanceof SmokerBlockEntity)return cooked(level,RecipeType.SMOKING,raw);
        if(block instanceof BlastFurnaceBlockEntity)return cooked(level,RecipeType.BLASTING,raw);
        if(block instanceof AbstractFurnaceBlockEntity)return cooked(level,RecipeType.SMELTING,raw);
        return cooked(level,RecipeType.CAMPFIRE_COOKING,raw);
    }
    static Recipe recipe(BlockEntity block,ItemStack raw){
        if(CompanionCraftingStock.supports(block)){
            var plan=CompanionCraftingStock.plan(block);
            return plan==null?null:new Recipe(plan.pattern().stream().filter(s->!s.isEmpty()).map(s->new RecipeSupplies.Part(Ingredient.of(s.getItem()),1)).toList(),List.of(plan.result()));
        }
        if(cooking(block)){
            var result=cooked(block,raw);
            return result.isEmpty()?null:new Recipe(List.of(new RecipeSupplies.Part(Ingredient.of(raw.getItem()),1)),List.of(result));
        }
        if(block instanceof WorkshopBlockEntity workshop){
            var entry=workshop.selection();if(entry==null)return null;var r=entry.recipe();
            return new Recipe(r.parts().stream().map(p->new RecipeSupplies.Part(p.ingredient(),p.count())).toList(),List.of(r.result().create()));
        }
        if(block instanceof FormMachineBlockEntity form){
            var entry=form.companionPort.stockRecipe();if(entry==null)return null;var r=entry.recipe();
            return new Recipe(r.items().stream().map(p->new RecipeSupplies.Part(p.ingredient(),p.count())).toList(),r.results().stream().map(p->p.create()).toList());
        }
        if(block instanceof MachineBlockEntity machine){
            var r=machine.companionPort.selection();
            return r==null?null:new Recipe(r.parts().stream().map(p->new RecipeSupplies.Part(p.ingredient(),p.count())).toList(),r.results());
        }
        return null;
    }
    static List<ItemStack> inputs(BlockEntity block){
        if(block instanceof FormMachineBlockEntity form)return form.companionPort.installed();
        if(CompanionCraftingStock.supports(block))return CompanionCraftingStock.inputs(block);
        if(block instanceof SkilletBlockEntity pan)return List.of(pan.raw());
        if(block instanceof KitchenStoveBlockEntity stove)return stove.items();
        if(block instanceof AbstractFurnaceBlockEntity furnace)return List.of(furnace.getItem(0));
        if(block instanceof CampfireBlockEntity fire)return fire.getItems();
        if(block instanceof WorkshopBlockEntity workshop)return List.of(workshop.getItem(0),workshop.getItem(1),workshop.getItem(2));
        var result=new ArrayList<ItemStack>();
        if(block instanceof MachineBlockEntity machine)for(int i=0;i<machine.kind().outputSlot();i++)result.add(machine.getItem(i));
        return result;
    }
    static List<ItemStack> committed(BlockEntity block){
        if(block instanceof FormMachineBlockEntity form){var results=new ArrayList<ItemStack>();for(int i=0;i<form.form().profile().lanes();i++)if(form.lane(i)!=null)for(var p:form.lane(i).itemResults())results.add(p.stack());return results;}
        if(block instanceof WorkshopBlockEntity workshop && workshop.lane()!=null)
            return workshop.lane().itemResults().stream().map(p->p.stack()).toList();
        return List.of();
    }
    static boolean busy(BlockEntity block){return inputs(block).stream().anyMatch(s->!s.isEmpty()) || !committed(block).isEmpty();}
    static int count(List<ItemStack> stacks,Item item){return stacks.stream().filter(s->s.is(item)).mapToInt(ItemStack::getCount).sum();}
    static int finished(BlockEntity block,Item item){
        if(block instanceof CraftingTableCompanionBlockEntity table){int n=0;for(var view:table.outputs())if(!view.isResourceBlank() && view.getResource().getItem()==item)n+=(int)view.getAmount();return n;}
        if(block instanceof FormMachineBlockEntity form){int n=0;for(int i=form.form().firstOutputSlot();i<form.form().firstSocketSlot();i++)if(form.container().getItem(i).is(item))n+=form.container().getItem(i).getCount();return n;}
        Storage<ItemVariant> storage=null;
        var kitchen=KitchenCompanionPort.of(block);
        if(kitchen!=null)storage=kitchen.outputStorage();
        else if(block instanceof AbstractFurnaceBlockEntity || block instanceof CampfireBlockEntity || block instanceof CrafterBlockEntity)storage=VanillaCompanionPort.of(block).outputs();
        else if(block instanceof WorkshopBlockEntity workshop)return workshop.getItem(WorkshopBlockEntity.OUTPUT).is(item)?workshop.getItem(WorkshopBlockEntity.OUTPUT).getCount():0;
        else if(block instanceof MachineBlockEntity machine)storage=machine.companionPort.outputs();
        int n=0;if(storage!=null)for(var view:storage)if(!view.isResourceBlank() && view.getResource().getItem()==item)n+=(int)Math.min(1_000_000,view.getAmount());
        return n;
    }
    private static List<ItemStack> materials(BlockEntity block,Recipe recipe,List<ItemStack> incoming){
        var result=new ArrayList<ItemStack>();
        for(var stack:inputs(block))if(recipe.ingredient(stack))result.add(stack);
        for(var stack:incoming)if(recipe.ingredient(stack))result.add(stack);
        return result;
    }
    private static int batches(Recipe recipe,List<ItemStack> materials){
        int low=0,high=384;
        while(low<high){int n=(low+high+1)/2;
            var parts=recipe.parts.stream().map(p->new RecipeSupplies.Part(p.ingredient(),p.count()*n)).toList();
            if(RecipeSupplies.completes(parts,materials,List.of()))low=n;else high=n-1;
        }return low;
    }
    static int loadedBatches(BlockEntity block){var r=recipe(block,ItemStack.EMPTY);return r==null?0:batches(r,materials(block,r,List.of()));}
    static int pending(PeepoEntity npc,BlockEntity block,Item item,List<ItemStack> incoming,CompanionProductionStock stock){
        if(CompanionCraftingStock.supports(block)){
            var plan=CompanionCraftingStock.plan(block);
            return plan!=null && plan.result().is(item)?plan.result().getCount()*plan.batches(incoming):0;
        }
        int n=count(committed(block),item);
        if(cooking(block)){
            for(var raw:inputs(block)){var product=cooked(block,raw);if(product.is(item))n+=product.getCount()*raw.getCount();}
            for(var raw:incoming){var product=cooked(block,raw);if(!product.is(item))continue;
                int amount=product.getCount()*raw.getCount();
                // A log manifest may contain both smelting inputs and one log for the fuel slot.
                if(block instanceof AbstractFurnaceBlockEntity && raw.has(net.minecraft.core.component.DataComponents.COOKING_FUEL)){
                    int goal=stock.inputGoal(npc,block,product);if(goal!=Integer.MAX_VALUE)amount=Math.min(amount,Math.max(0,goal-n));
                }
                n+=amount;
            }
        }else{
            var recipe=recipe(block,ItemStack.EMPTY);
            if(recipe!=null)n+=count(recipe.results,item)*batches(recipe,materials(block,recipe,incoming));
        }
        return n;
    }
    static int ingredientRoom(PeepoEntity npc,BlockEntity block,ItemStack raw,CompanionProductionStock stock){
        var product=cooked(block,raw);if(product.isEmpty())return 0;
        int goal=stock.inputGoal(npc,block,product);if(goal==Integer.MAX_VALUE)return Integer.MAX_VALUE;
        int installed=0;for(var stack:inputs(block)){var result=cooked(block,stack);if(result.is(product.getItem()))installed+=result.getCount()*stack.getCount();}
        return (Math.max(0,goal-installed)+product.getCount()-1)/product.getCount();
    }
    static int desiredBatches(PeepoEntity npc,BlockEntity block,Recipe recipe,CompanionProductionStock stock){
        int goal=0;
        for(var product:recipe.results)if(CompanionFilters.get(block).allows(product)){
            int amount=stock.inputGoal(npc,block,product);
            if(amount==Integer.MAX_VALUE)return Integer.MAX_VALUE;
            amount=Math.max(0,amount-count(committed(block),product.getItem()));
            goal=Math.max(goal,(amount+product.getCount()-1)/product.getCount());
        }
        return Math.min(384,goal);
    }
    /** The native input cap still applies; targets only reduce new pickups, never committed deliveries. */
    static int limit(PeepoEntity npc,BlockEntity block,ItemStack raw,int nativeNeed,int auxiliary,CompanionProductionStock stock){
        if(nativeNeed<=0)return 0;
        if(CompanionCraftingStock.supports(block))return CompanionCraftingStock.limit(npc,block,raw,nativeNeed,stock);
        var recipe=recipe(block,raw);var incoming=stock.incoming();
        if(block instanceof FormMachineBlockEntity && recipe!=null && recipe.results().isEmpty())return nativeNeed; // Fluid targets remain native.
        int utility=0;
        if(auxiliary>0){
            boolean needed=busy(block) || !incoming.isEmpty() || recipe!=null && recipe.results.stream().anyMatch(s->CompanionFilters.get(block).allows(s) && stock.wants(npc,block,s));
            if(needed)utility=Math.max(0,auxiliary-incoming.stream().filter(s->ItemStack.isSameItemSameComponents(s,raw)).mapToInt(ItemStack::getCount).sum());
        }
        if(recipe==null || !recipe.ingredient(raw))return Math.min(nativeNeed,utility);
        if(cooking(block)){
            var product=recipe.results.getFirst();int goal=stock.inputGoal(npc,block,product);
            if(goal==Integer.MAX_VALUE)return nativeNeed;
            int remaining=Math.max(0,goal-pending(npc,block,product.getItem(),incoming,stock));
            return Math.min(nativeNeed,utility+(remaining+product.getCount()-1)/product.getCount());
        }
        int goal=desiredBatches(npc,block,recipe,stock);
        if(goal==Integer.MAX_VALUE)return nativeNeed;
        if(goal==0)return Math.min(nativeNeed,utility);
        var held=materials(block,recipe,incoming).stream().map(ItemStack::copy).toList();
        var parts=new ArrayList<>(recipe.parts);parts.sort(Comparator.comparingLong(p->held.stream().filter(p.ingredient()::test).count()));
        int missing=0;
        for(var part:parts){int left=part.count()*goal;
            for(var stack:held)if(left>0 && part.ingredient().test(stack)){int take=Math.min(left,stack.getCount());left-=take;stack.shrink(take);}
            if(part.ingredient().test(raw))missing=Math.min(raw.getMaxStackSize(),missing+left);
        }
        return Math.min(nativeNeed,missing+utility);
    }
    static void prepare(PeepoEntity npc,BlockEntity block,Storage<ItemVariant> source,CompanionProductionStock stock){
        if(!stock.incoming().isEmpty())return;
        // Crafters follow the player's exact grid; stock priorities must never select another pattern.
        if(block instanceof CrafterBlockEntity){VanillaCompanionPort.of(block).prepare(source);return;}
        if(busy(block)){
            if(block instanceof FormMachineBlockEntity form && form.companionPort.stockRecipe()==null)form.companionPort.prepare(source,null);
            // Recover a selected native plan after a filter edit or manually installed ingredients.
            if(block instanceof MachineBlockEntity machine && (machine.companionPort.plan()==null || machine.kind()==MachineKind.ELECTROPLATING_BATH && machine.companionPort.selection()==null))machine.companionPort.prepare(source);
            else if(block instanceof AbstractFurnaceBlockEntity || block instanceof CampfireBlockEntity){var port=VanillaCompanionPort.of(block);if(port.plan()==null)port.prepare(source);}
            return;
        }
        var priorities=new HashMap<Item,Double>();
        java.util.function.ToDoubleFunction<ItemStack> priority=s->priorities.computeIfAbsent(s.getItem(),item->stock.wants(npc,block,s)?stock.priority(npc,block,s):-2);
        if(block instanceof WorkshopBlockEntity workshop)workshop.companion.prepare(source,priority);
        else if(block instanceof CraftingTableCompanionBlockEntity table)table.prepare(source,priority);
        else if(block instanceof FormMachineBlockEntity form)form.companionPort.prepare(source,priority);
        else if(block instanceof MachineBlockEntity machine)machine.companionPort.prepare(source,priority);
        else if(block instanceof AbstractFurnaceBlockEntity || block instanceof CampfireBlockEntity)VanillaCompanionPort.of(block).prepare(source,priority);
    }
}
