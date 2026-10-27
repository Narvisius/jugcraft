package local.peepo;

import java.util.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;

/** Ordinary native recipes, indexed once per recipe-manager identity; never a per-tick registry scan. */
final class CraftingTableRecipes {
    record Entry(Identifier id,CraftingRecipe recipe,List<Optional<Ingredient>> slots,ItemStack icon) {}
    private static final Map<RecipeManager,CraftingTableRecipes> CACHES=new WeakHashMap<>();
    final List<Entry> all;
    final Map<Identifier,Entry> ids;
    final Map<Item,List<Entry>> products;
    static CraftingTableRecipes of(RecipeManager manager){return CACHES.computeIfAbsent(manager,CraftingTableRecipes::new);}
    private CraftingTableRecipes(RecipeManager manager){
        var entries=new ArrayList<Entry>();
        for(var holder:manager.getRecipes()){
            if(!(holder.value() instanceof CraftingRecipe recipe) || recipe.isSpecial())continue;
            var slots=new ArrayList<Optional<Ingredient>>(Collections.nCopies(9,Optional.empty()));
            if(recipe instanceof ShapedRecipe shaped){
                if(shaped.getWidth()>3 || shaped.getHeight()>3)continue;
                var ingredients=shaped.getIngredients();
                for(int y=0;y<shaped.getHeight();y++)for(int x=0;x<shaped.getWidth();x++)slots.set(y*3+x,ingredients.get(y*shaped.getWidth()+x));
            }else if(recipe instanceof ShapelessRecipe){
                var ingredients=recipe.placementInfo().ingredients();if(ingredients.isEmpty() || ingredients.size()>9)continue;
                for(int i=0;i<ingredients.size();i++)slots.set(i,Optional.of(ingredients.get(i)));
            }else continue; // Complex player-context recipes need an explicit adapter.
            var example=new ArrayList<ItemStack>(9);boolean valid=true;
            for(var ingredient:slots){
                var item=ingredient.flatMap(i->i.items().findFirst());
                if(ingredient.isPresent() && item.isEmpty()){valid=false;break;}
                example.add(item.map(ItemStack::new).orElse(ItemStack.EMPTY));
            }
            if(!valid)continue;
            var result=recipe.assemble(CraftingInput.of(3,3,example));
            if(!result.isEmpty())entries.add(new Entry(holder.id().identifier(),recipe,List.copyOf(slots),result.copy()));
        }
        entries.sort(Comparator.comparing(e->e.id().toString()));all=List.copyOf(entries);
        var byId=new HashMap<Identifier,Entry>();var byProduct=new HashMap<Item,List<Entry>>();
        for(var e:all){byId.put(e.id(),e);byProduct.computeIfAbsent(e.icon().getItem(),k->new ArrayList<>()).add(e);}
        ids=Map.copyOf(byId);var frozen=new HashMap<Item,List<Entry>>();byProduct.forEach((item,list)->frozen.put(item,List.copyOf(list)));products=Map.copyOf(frozen);
    }
    List<Entry> choices(CompanionRecipeFilter filter){
        if(filter.empty())return all;
        var choices=new LinkedHashSet<Entry>();for(int i=0;i<9;i++)choices.addAll(products.getOrDefault(filter.get(i).getItem(),List.of()));return new ArrayList<>(choices);
    }
    /** Real stacks supply the pattern, including components; overlapping tags use bounded backtracking. */
    static List<ItemStack> pattern(Entry entry,List<ItemStack> supplies){
        var available=supplies.stream().map(ItemStack::copy).toList();
        var pattern=new ArrayList<ItemStack>(Collections.nCopies(9,ItemStack.EMPTY));var order=new ArrayList<Integer>();
        for(int i=0;i<9;i++)if(entry.slots().get(i).isPresent())order.add(i);
        order.sort(Comparator.comparingLong(i->available.stream().filter(entry.slots().get(i).orElseThrow()::test).count()));
        return select(entry,available,pattern,order,0,new int[]{512})?List.copyOf(pattern):List.of();
    }
    private static boolean select(Entry entry,List<ItemStack> available,List<ItemStack> pattern,List<Integer> order,int depth,int[] budget){
        if(depth==order.size())return true;if(--budget[0]<0)return false;
        int slot=order.get(depth);var ingredient=entry.slots().get(slot).orElseThrow();
        for(var stack:available)if(!stack.isEmpty() && ingredient.test(stack)){
            pattern.set(slot,stack.copyWithCount(1));stack.shrink(1);
            if(select(entry,available,pattern,order,depth+1,budget))return true;
            stack.grow(1);pattern.set(slot,ItemStack.EMPTY);
        }
        return false;
    }
}
