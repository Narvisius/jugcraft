package io.github.jimbozoomer.jugcraft.machine;

import java.util.*;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipe;
import local.peepo.CompanionLogistics;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.storage.base.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.storage.*;

/** Recipe-guided item port; fluids, power, catalysts and machine processing still belong to the machine. */
public final class MachineCompanionPort implements CompanionLogistics.Port {
    public record Part(Ingredient ingredient,int count){}
    public record Plan(Identifier id,List<Part> parts,List<ItemStack> results,boolean ordered,Recipe<?> recipe){}
    private static final Map<MachineKind,Map<Identifier,Plan>> CATALOG=new EnumMap<>(MachineKind.class);
    public static void clearCatalog(){CATALOG.clear();}
    private final MachineBlockEntity machine;
    private Identifier selected;
    public final local.peepo.CompanionRecipeFilter filter=new local.peepo.CompanionRecipeFilter();
    public local.peepo.CompanionRecipeFilter filter(){var old=selection();filter.initialize(old==null?ItemStack.EMPTY:old.results.getFirst());return filter;}
    public boolean acceptsFilter(ItemStack stack){return stack.isEmpty() || catalog().values().stream().anyMatch(p->p.results.stream().anyMatch(s->s.is(stack.getItem())));}
    public void filterChanged(){selected=null;machine.companionRecipeChanged();}
    @Override public void prepare(Storage<ItemVariant> source){
        var allowed=filter();var supplies=local.peepo.RecipeSupplies.available(source);
        var installed=new ArrayList<ItemStack>();for(int i=0;i<machine.kind().outputSlot();i++)installed.add(machine.getItem(i));
        var choices=new ArrayList<>(catalog().values());choices.sort(Comparator.comparingInt(p->p.id.equals(selected)?0:1));
        int inspected=0;for(var p:choices){if(++inspected>256)break;
            if(p.results.stream().noneMatch(allowed::allows) || allocation(p)==null)continue;
            if(p.recipe instanceof FluidRecipe fluid && (machine.tanks()==null || !fluid.fluidsMatch(machine.tanks())))continue;
            var parts=p.parts.stream().map(part->new local.peepo.RecipeSupplies.Part(part.ingredient,part.count)).toList();
            if(local.peepo.RecipeSupplies.completes(parts,installed,supplies)){select(p.id);return;}
        }
        if(installed.stream().allMatch(ItemStack::isEmpty))select((Identifier)null);
    }
    public MachineCompanionPort(MachineBlockEntity machine){this.machine=machine;}
    private Map<Identifier,Plan> catalog(){
        if(!(machine.getLevel() instanceof ServerLevel level))return Map.of();
        return CATALOG.computeIfAbsent(machine.kind(),kind->catalog(level.getServer(),kind));
    }
    private static Map<Identifier,Plan> catalog(MinecraftServer server,MachineKind kind){
        var plans=new ArrayList<Plan>();
        for(var holder:server.getRecipeManager().getRecipes()){
            var recipe=holder.value();var parts=new ArrayList<Part>();var results=new ArrayList<ItemStack>();boolean ordered=false;
            if(recipe instanceof MachineRecipe single && single.machine()==kind){
                parts.add(new Part(single.input(),1));results.add(single.output().create());
            }else if(recipe instanceof MultiMachineRecipe multi && multi.getType()==MachineRecipeTypes.multi(kind)){
                for(var part:multi.parts())parts.add(new Part(part.ingredient(),part.count()));results.add(multi.output().create());
            }else if(recipe instanceof SmeltingRecipe smelting && kind==MachineKind.ELECTRIC_FURNACE){
                parts.add(new Part(smelting.input(),1));results.add(smelting.assemble(new SingleRecipeInput(ItemStack.EMPTY)));
            }else if(recipe instanceof FluidRecipe fluid && fluid.machine()==kind && fluid.legacy()){
                ordered=true;for(var part:fluid.items())parts.add(new Part(part.ingredient(),part.count()));
                for(var result:fluid.results())results.add(result.create());
            }else continue;
            if(!parts.isEmpty() && parts.size()<=kind.outputSlot() && !results.isEmpty())plans.add(new Plan(holder.id().identifier(),List.copyOf(parts),List.copyOf(results),ordered,recipe));
        }
        plans.sort(Comparator.comparing(p->p.id().toString()));
        var catalog=new LinkedHashMap<Identifier,Plan>();for(var plan:plans)catalog.put(plan.id,plan);return Collections.unmodifiableMap(catalog);
    }
    public boolean selectable(){return !catalog().isEmpty();}
    public Plan selection(){return selected==null?null:catalog().get(selected);}
    public boolean locked(){return selected!=null || !filter().empty();}
    private boolean ready(Plan p){
        var slots=allocation(p);if(slots==null)return false;
        for(int i=0;i<slots.length;i++)if(machine.getItem(slots[i]).getCount()<p.parts.get(i).count)return false;
        return !(p.recipe instanceof FluidRecipe f) || machine.tanks()!=null && f.fluidsMatch(machine.tanks());
    }
    public Recipe<?> recipe(){
        var allowed=filter();var current=selection();
        if(current!=null && current.results.stream().anyMatch(allowed::allows) && ready(current))return current.recipe;
        int inspected=0;for(var p:catalog().values()){if(++inspected>256)break;
            if(p.results.stream().anyMatch(allowed::allows) && ready(p))return p.recipe;
        }return null;
    }
    private void select(Identifier id){if(Objects.equals(selected,id))return;selected=id;machine.companionRecipeChanged();}
    public boolean select(ItemStack output){
        if(output.isEmpty()){select((Identifier)null);return true;}
        var matches=catalog().values().stream().filter(p->p.results.stream().anyMatch(s->s.is(output.getItem()))).toList();
        if(matches.isEmpty())return false;
        int index=-1;for(int i=0;i<matches.size();i++)if(matches.get(i).id.equals(selected))index=i;
        select(matches.get((index+1)%matches.size()).id);return true;
    }
    public Identifier plan(){return selection()==null?null:selected;}
    /** Assign every ingredient its own slot; preserve existing stacks, including ambiguous tag recipes. */
    private int[] allocation(Plan plan){
        int[] slots=new int[plan.parts.size()];Arrays.fill(slots,-1);
        return assign(plan,0,slots,new boolean[machine.kind().outputSlot()])?slots:null;
    }
    private boolean assign(Plan plan,int index,int[] slots,boolean[] used){
        if(index==slots.length){for(int slot=0;slot<used.length;slot++)if(!used[slot] && !machine.getItem(slot).isEmpty())return false;return true;}
        // Existing stacks first. At most the machine's small fixed set of input slots.
        for(int pass=0;pass<2;pass++)for(int slot=0;slot<used.length;slot++){
            if(used[slot] || plan.ordered && slot!=index)continue;
            var held=machine.getItem(slot);if(held.isEmpty()!=(pass==1) || !held.isEmpty() && !plan.parts.get(index).ingredient.test(held))continue;
            used[slot]=true;slots[index]=slot;if(assign(plan,index+1,slots,used))return true;used[slot]=false;
        }
        return false;
    }
    private boolean inputAllowed(int slot,ItemStack stack){
        for(var side:net.minecraft.core.Direction.values())for(int exposed:machine.getSlotsForFace(side))
            if(exposed==slot && machine.canPlaceItemThroughFace(slot,stack,side))return true;
        return false;
    }
    private int missing(Plan plan,int part,int slot,ItemStack stack){
        var held=machine.getItem(slot);var ingredient=plan.parts.get(part);
        return ingredient.ingredient.test(stack) && (held.isEmpty() || ItemStack.isSameItemSameComponents(stack,held)) && inputAllowed(slot,stack)
            ?Math.max(0,Math.min(stack.getMaxStackSize(),ingredient.count)-held.getCount()):0;
    }
    public int needed(ItemStack stack){
        var plan=selection();if(plan==null)return 0;var slots=allocation(plan);if(slots==null)return 0;
        int total=0;for(int i=0;i<slots.length;i++)total=Math.min(32,total+missing(plan,i,slots[i],stack));return total;
    }
    public Storage<ItemVariant> inputs(){return new InsertionOnlyStorage<>(){
        public long insert(ItemVariant item,long amount,TransactionContext tx){
            StoragePreconditions.notBlankNotNegative(item,amount);var plan=selection();if(plan==null)return 0;
            var slots=allocation(plan);if(slots==null)return 0;long moved=0;var stack=item.toStack(1);var inventory=ContainerStorage.of(machine,null);
            for(int i=0;i<slots.length && moved<amount;i++){
                int need=missing(plan,i,slots[i],stack);
                if(need>0)moved+=inventory.getSlot(slots[i]).insert(item,Math.min(amount-moved,need),tx);
            }
            return moved;
        }
    };}
    public Storage<ItemVariant> outputs(){
        var inventory=ContainerStorage.of(machine,null);var outputs=new ArrayList<Storage<ItemVariant>>();
        for(int slot=machine.kind().outputSlot();slot<machine.kind().slots;slot++){
            boolean allowed=false;
            for(var side:net.minecraft.core.Direction.values())for(int exposed:machine.getSlotsForFace(side))
                if(exposed==slot && machine.canTakeItemThroughFace(slot,machine.getItem(slot),side))allowed=true;
            if(allowed)outputs.add(inventory.getSlot(slot));
        }
        return new CombinedStorage<>(outputs);
    }
    public void save(ValueOutput out){filter().save(out);if(selected!=null)out.store("CompanionSupplyRecipe",Identifier.CODEC,selected);}
    public void load(ValueInput in){filter.load(in);selected=in.read("CompanionSupplyRecipe",Identifier.CODEC).orElse(null);}
}
