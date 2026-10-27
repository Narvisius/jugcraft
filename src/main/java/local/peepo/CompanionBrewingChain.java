package local.peepo;

import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.storage.*;
import io.github.jimbozoomer.jugcraft.mixin.CompanionBrewingAccessor;

/** A bounded, saved batch plan. Vanilla owns the actual brewing and ingredient consumption. */
final class CompanionBrewingChain {
    private record Step(Identifier id, BrewingRecipe recipe) {}
    private static List<Step> recipes;
    private static final Map<Identifier,BrewingRecipe> BY_ID=new HashMap<>();
    private static final Map<ItemVariant,List<Step>> EDGES=new HashMap<>();
    static void clearCache(){recipes=null;EDGES.clear();BY_ID.clear();}
    private final BrewingStandBlockEntity stand;
    private final CompanionRecipeFilter filter;
    private final Runnable changed;
    private List<Identifier> chain=List.of();
    private int step,bottles=3;
    private ItemStack base=ItemStack.EMPTY;
    private long nextSearch;
    private boolean loaded;
    void transferred(){if(count()>0 && !chain.isEmpty())loaded=true;}
    CompanionBrewingChain(BrewingStandBlockEntity stand,CompanionRecipeFilter filter,Runnable changed){this.stand=stand;this.filter=filter;this.changed=changed;}
    private List<Step> recipes(){
        if(recipes==null && stand.getLevel() instanceof ServerLevel level)recipes=level.getServer().getRecipeManager().getRecipes().stream()
            .filter(h->h.value() instanceof BrewingRecipe).map(h->new Step(h.id().identifier(),(BrewingRecipe)h.value()))
            .sorted(Comparator.comparing(s->s.id.toString())).peek(s->BY_ID.put(s.id,s.recipe)).toList();
        return recipes==null?List.of():recipes;
    }
    private BrewingRecipe recipe(int index){if(index<0 || index>=chain.size())return null;recipes();return BY_ID.get(chain.get(index));}
    private List<Step> edges(ItemStack item){
        var key=ItemVariant.of(item);var found=EDGES.get(key);if(found!=null)return found;
        found=recipes().stream().filter(r->r.recipe.getInput().test(item)).toList();if(EDGES.size()<4096)EDGES.put(key,found);return found;
    }
    private int count(){int n=0;for(int i=0;i<3;i++)if(!stand.getItem(i).isEmpty())n++;return n;}
    private boolean idle(){return ((CompanionBrewingAccessor)stand).peepo$brewTime()==0;}
    private boolean all(java.util.function.Predicate<ItemStack> predicate){for(int i=0;i<3;i++)if(!stand.getItem(i).isEmpty() && !predicate.test(stand.getItem(i)))return false;return count()>0;}
    Identifier plan(){return chain.isEmpty()?null:chain.getLast();}
    private void clear(){chain=List.of();step=0;loaded=false;base=ItemStack.EMPTY;changed.run();}
    void filterChanged(){if(count()==0 && idle())clear();nextSearch=0;}
    private boolean finished(){var last=recipe(chain.size()-1);return last!=null && step==chain.size()-1 && all(s->ItemStack.isSameItemSameComponents(s,last.getOutput().create()));}
    boolean output(int slot){return idle() && finished() && !stand.getItem(slot).isEmpty();}
    boolean reagentOutput(){
        var item=stand.getItem(3);if(item.isEmpty() || !idle())return false;
        if(!stand.canPlaceItem(3,item))return true;
        if(chain.isEmpty())return false;
        if(loaded && count()==0)return true;
        var current=recipe(step);return finished() || current!=null && !current.getReagent().test(item);
    }
    void prepare(Storage<ItemVariant> source){
        prepare(source,s->Integer.MAX_VALUE,s->0);
    }
    ItemStack stockProduct(){var last=recipe(chain.size()-1);return last==null?ItemStack.EMPTY:last.getOutput().create();}
    boolean stockBusy(){return count()>0;}
    int stockPending(List<ItemStack> incoming){
        if(finished())return 0;
        return count()+incoming.stream().filter(s->ItemStack.isSameItemSameComponents(s,base)).mapToInt(ItemStack::getCount).sum();
    }
    boolean startingBottle(ItemStack item){return step==0 && ItemStack.isSameItemSameComponents(item,base);}
    void prepare(Storage<ItemVariant> source,java.util.function.ToIntFunction<ItemStack> goal,java.util.function.ToDoubleFunction<ItemStack> priority){
        if(!idle())return;
        if(!chain.isEmpty()){
            // Never mutate this state from a transfer simulation; prepare runs before transport probes.
            var current=recipe(step);
            if(current==null)return; // Removed datapack recipe: preserve the real contents for manual recovery.
            if(count()>0){
                loaded=true;
                if(step<chain.size()-1 && all(s->ItemStack.isSameItemSameComponents(s,current.getOutput().create()))){step++;changed.run();}
                return;
            }
            if(loaded || step>0 || base.isEmpty()){if(!stand.getItem(3).isEmpty())return;clear();}
            else if(goal.applyAsInt(stockProduct())<=0)clear();else return;
        }
        long now=stand.getLevel().getGameTime();if(now<nextSearch)return;nextSearch=now+40;
        var supplies=RecipeSupplies.available(source);
        var starts=new LinkedHashMap<ItemVariant,ItemStack>();
        for(int i=0;i<3;i++){var s=stand.getItem(i);if(!s.isEmpty())starts.putIfAbsent(ItemVariant.of(s),s.copyWithCount(1));}
        boolean installed=count()>0;
        if(installed && starts.size()!=1)return;
        if(!installed)for(var s:supplies)if(!edges(s).isEmpty())starts.putIfAbsent(ItemVariant.of(s),s.copyWithCount(1));
        boolean fuel=((CompanionBrewingAccessor)stand).peepo$fuel()>0 || stand.getItem(4).has(DataComponents.BREWING_FUEL) || supplies.stream().anyMatch(s->s.has(DataComponents.BREWING_FUEL));
        if(!fuel)return;
        record Node(ItemStack value,ItemStack base,List<Identifier> path,List<ItemStack> remaining,Set<ItemVariant> visited){}
        record Choice(List<Identifier> path,ItemStack base,int bottles,double priority){}
        Choice best=null;
        var queue=new ArrayDeque<Node>();
        for(var start:starts.values()){
            var stock=new ArrayList<ItemStack>();for(var s:supplies)stock.add(s.copy());
            // Reserve the starting bottles so a broad reagent predicate cannot also consume them.
            if(!installed){int left=Math.min(3,stock.stream().filter(s->ItemStack.isSameItemSameComponents(s,start)).mapToInt(ItemStack::getCount).sum());for(var s:stock)if(left>0 && ItemStack.isSameItemSameComponents(s,start)){int n=Math.min(left,s.getCount());s.shrink(n);left-=n;}}
            if(((CompanionBrewingAccessor)stand).peepo$fuel()==0 && !stand.getItem(4).has(DataComponents.BREWING_FUEL)){
                boolean reserved=false;for(var s:stock)if(!s.isEmpty() && s.has(DataComponents.BREWING_FUEL)){s.shrink(1);reserved=true;break;}
                if(!reserved)continue;
            }
            queue.add(new Node(start,start,List.of(),stock,Set.of(ItemVariant.of(start))));
        }
        int budget=2048,nodes=0;
        while(!queue.isEmpty() && nodes++<256 && budget>0){
            var node=queue.removeFirst();if(node.path.size()>=8)continue;
            for(var edge:edges(node.value)){
                if(--budget<0)break;
                var reagent=stand.getItem(3);boolean useInstalled=node.path.isEmpty() && !reagent.isEmpty();
                if(useInstalled && !edge.recipe.getReagent().test(reagent))continue;
                var remaining=new ArrayList<ItemStack>();for(var s:node.remaining)remaining.add(s.copy());
                if(!useInstalled){boolean found=false;for(var s:remaining)if(!s.isEmpty() && edge.recipe.getReagent().test(s)){s.shrink(1);found=true;break;}if(!found)continue;}
                var out=edge.recipe.getOutput().create();var variant=ItemVariant.of(out);if(node.visited.contains(variant))continue;
                var path=new ArrayList<>(node.path);path.add(edge.id);
                if(filter.allows(out)){
                    int wanted=installed?count():goal.applyAsInt(out);
                    int available=installed?count():supplies.stream().filter(s->ItemStack.isSameItemSameComponents(s,node.base)).mapToInt(ItemStack::getCount).sum();
                    double score=priority.applyAsDouble(out);
                    if(wanted==Integer.MAX_VALUE && score==0){chain=List.copyOf(path);step=0;loaded=installed;base=node.base.copyWithCount(1);bottles=Math.clamp(available,1,3);changed.run();return;}
                    if(wanted>0 && (best==null || score>best.priority))best=new Choice(List.copyOf(path),node.base.copyWithCount(1),Math.clamp(Math.min(wanted,available),1,3),score);
                }
                if(!filter.empty() && queue.size()<256){var visited=new HashSet<>(node.visited);visited.add(variant);queue.addLast(new Node(out,node.base,path,remaining,visited));}
            }
        }
        if(best!=null){chain=best.path;step=0;loaded=installed;base=best.base;bottles=best.bottles;changed.run();}
    }
    int[] needs(ItemStack item){
        int[] need=new int[9];var current=recipe(step);if(current==null || !idle() || finished())return need;
        // Intermediate bottles must stay together; never top up a later step with another potion.
        if(step==0 && ItemStack.isSameItemSameComponents(item,base) && (count()==0 || all(current.getInput()))){
            int left=bottles-count();for(int i=0;i<3 && left>0;i++)if(stand.getItem(i).isEmpty() && stand.canPlaceItem(i,item)){need[i]=1;left--;}
        }
        if(!all(current.getInput()) || step==0 && count()<bottles)return need;
        if(current.getReagent().test(item) && stand.getItem(3).isEmpty() && stand.canPlaceItem(3,item))need[3]=1;
        if(item.has(DataComponents.BREWING_FUEL) && ((CompanionBrewingAccessor)stand).peepo$fuel()==0 && stand.getItem(4).isEmpty() && stand.canPlaceItem(4,item))need[4]=1;
        return need;
    }
    CompanionStatus status(){
        var current=recipe(step);if(current==null)return CompanionStatus.RECIPE_MISSING;
        if(!idle())return CompanionStatus.WORKING;
        if(count()>0 && !all(current.getInput()) && !all(s->ItemStack.isSameItemSameComponents(s,current.getOutput().create())))return CompanionStatus.RECIPE_MISSING;
        return CompanionStatus.IDLE;
    }
    void save(ValueOutput out){out.putBoolean("PeepoBrewLoaded",loaded);out.store("PeepoBrewChain",Identifier.CODEC.listOf(),chain);out.putInt("PeepoBrewStep",step);out.putInt("PeepoBrewBottles",bottles);out.store("PeepoBrewBase",ItemStack.OPTIONAL_CODEC,base);}
    void load(ValueInput in,Identifier legacy){
        loaded=in.getBooleanOr("PeepoBrewLoaded",legacy!=null);
        chain=in.read("PeepoBrewChain",Identifier.CODEC.listOf()).filter(l->l.size()<=8).orElse(legacy==null?List.of():List.of(legacy));
        step=Math.clamp(in.getIntOr("PeepoBrewStep",0),0,Math.max(0,chain.size()-1));bottles=Math.clamp(in.getIntOr("PeepoBrewBottles",3),1,3);
        base=in.read("PeepoBrewBase",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        if(base.isEmpty() && !chain.isEmpty() && count()>0){
            bottles=count();loaded=true;for(int i=0;i<3;i++)if(!stand.getItem(i).isEmpty()){base=stand.getItem(i).copyWithCount(1);break;}
        }
    }
}
