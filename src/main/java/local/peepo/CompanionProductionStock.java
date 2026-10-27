package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.*;

/** Shared, optional production goals. Only assigned containers and registered deliveries are inspected. */
public final class CompanionProductionStock {
    public static final int MAX_TARGET=4096, MAX_DELIVERIES=64;
    private record Rule(int target,int restock,boolean filling) {}
    public record Delivery(boolean input,List<ItemStack> stacks) {}
    public record Count(int stored,int underway,boolean available) {}
    private final CompanionRecipeFilter filter;
    private final Map<ItemVariant,Rule> rules=new LinkedHashMap<>();
    private final List<CompanionAssignments.Target> outputs=new ArrayList<>();
    private final Map<UUID,Delivery> deliveries=new LinkedHashMap<>();
    private Map<ItemVariant,Integer> cached=Map.of();
    private long nextCount;
    private int revision;
    private boolean cacheValid;
    public CompanionProductionStock(CompanionRecipeFilter filter){this.filter=filter;}
    public static boolean supports(BlockEntity block){return block instanceof CookingPotBlockEntity || block instanceof HearthOvenBlockEntity || block instanceof CuttingBoardBlockEntity || CompanionStockRecipes.supports(block) || CompanionAdditionalStock.supports(block);}
    public static CompanionProductionStock of(BlockEntity block){return supports(block)?CompanionFilters.get(block).stock:null;}
    public static boolean supportsProduct(BlockEntity block,ItemStack icon){return !icon.isEmpty() && (!(block instanceof io.github.jimbozoomer.jugcraft.machine.form.FormMachineBlockEntity form) || form.companionPort.stockProduct(icon));}
    public boolean matches(ItemStack a,ItemStack b){return filter.stockKey(a).equals(filter.stockKey(b));}
    private static boolean finishedItem(BlockEntity block,ItemStack stack){return !(block instanceof io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity m) || m.kind()!=io.github.jimbozoomer.jugcraft.machine.MachineKind.ELECTROPLATING_BATH || stack.has(io.github.jimbozoomer.jugcraft.machine.Electroplating.PLATING);}
    public int revision(){return revision;}
    public boolean enabled(){return !rules.isEmpty();}
    public int target(ItemStack item){var r=rules.get(filter.stockKey(item));return r==null?0:r.target;}
    public int restock(ItemStack item){var r=rules.get(filter.stockKey(item));return r==null?0:r.restock;}
    public List<CompanionAssignments.Target> outputs(){return List.copyOf(outputs);}
    public void filterChanged(){rules.keySet().removeIf(item->!contains(item));revision++;invalidate();}
    private boolean contains(ItemVariant item){for(int i=0;i<9;i++)if(filter.stockKey(filter.get(i)).equals(item))return true;return false;}
    public void set(ItemStack item,int target,int restock){
        if(item.isEmpty() || !contains(filter.stockKey(item)) || target<0 || target>MAX_TARGET || restock<0 || target>0 && restock>=target)return;
        if(target==0)rules.remove(filter.stockKey(item));else rules.put(filter.stockKey(item),new Rule(target,restock,true));
        revision++;invalidate();
    }
    public void useOutputs(PeepoEntity npc,CompanionAssignments.Target work){
        outputs.clear();var seen=new HashSet<BlockPos>();
        for(var t:npc.assignments.supplies.containers(work,true))if(t.local(npc.level()) && seen.add(physical(npc,t)))outputs.add(t);
        revision++;invalidate();
    }
    public void invalidate(){nextCount=0;cacheValid=false;}
    public boolean countsOutput(PeepoEntity npc,CompanionAssignments.Target t){
        if(!enabled())return true;if(t==null || !t.local(npc.level()))return false;
        var pos=physical(npc,t);
        return outputs.stream().anyMatch(o->o.local(npc.level()) && physical(npc,o).equals(pos));
    }
    private static BlockPos physical(PeepoEntity npc,CompanionAssignments.Target t){
        return t.local(npc.level()) && npc.level().hasChunkAt(t.at().pos())?CompanionAssignments.canonical(npc.level(),t.at().pos()):t.at().pos();
    }
    static Storage<ItemVariant> countedStorage(PeepoEntity npc,CompanionAssignments.Target t){
        if(t==null || !t.present(npc.level()))return null;
        var state=npc.level().getBlockState(t.at().pos());
        if(state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock
            && state.getValue(net.minecraft.world.level.block.ChestBlock.TYPE)!=net.minecraft.world.level.block.state.properties.ChestType.SINGLE
            && !npc.level().hasChunkAt(t.at().pos().relative(net.minecraft.world.level.block.ChestBlock.getConnectedDirection(state))))return null;
        return CompanionStorage.find(npc,t);
    }
    private boolean accessible(PeepoEntity npc,BlockEntity block){
        if(outputs.isEmpty() || !(npc.level() instanceof ServerLevel) || !CompanionJobs.permitted(npc,block.getBlockPos()))return false;
        boolean route=false;
        var work=npc.assignments.get(npc.assignments.workPriority(block.getBlockPos()));
        for(var t:outputs){
            if(!t.present(npc.level()) || t.at().pos().distSqr(block.getBlockPos())>64*64 || countedStorage(npc,t)==null)return false;
            for(var link:npc.assignments.supplies.containers(work,true))if(physical(npc,link).equals(physical(npc,t)))route=true;
        }
        return route;
    }
    /** Refresh no more than once a second; a real pickup forces a fresh bounded count. */
    private boolean refresh(PeepoEntity npc,BlockEntity block){
        if(!accessible(npc,block))return false;
        long now=npc.level().getGameTime();
        if(now>=nextCount){
            nextCount=now+20;var tally=new HashMap<ItemVariant,Integer>();var seen=new HashSet<BlockPos>();boolean valid=true;
            for(var t:outputs){
                if(!seen.add(physical(npc,t)))continue;
                var storage=countedStorage(npc,t);if(storage==null){valid=false;break;}
                int views=0;for(var view:storage){if(++views>128){valid=false;break;}if(!view.isResourceBlank() && finishedItem(block,view.getResource().toStack(1)))add(tally,filter.stockKey(view.getResource().toStack(1)),view.getAmount());}
                if(!valid)break;
            }
            cached=Map.copyOf(tally);cacheValid=valid;
        }
        return cacheValid && reconcile(npc,block);
    }
    private boolean reconcile(PeepoEntity npc,BlockEntity block){
        var level=(ServerLevel)npc.level();boolean available=true,changed=false;
        for(var id:ProductionDeliveryCancellations.get(level.getServer()).take(GlobalPos.of(level.dimension(),block.getBlockPos())))try{changed|=deliveries.remove(UUID.fromString(id))!=null;}catch(IllegalArgumentException ignored){}
        for(var entry:new ArrayList<>(deliveries.entrySet())){
            var entity=level.getEntity(entry.getKey());
            if(entity==null){available=false;continue;} // Never forget cargo merely because its chunk unloaded.
            var current=entity instanceof PeepoEntity p && p.isAlive()?p.transport.stockDelivery(block.getBlockPos()):null;
            if(current==null){deliveries.remove(entry.getKey());changed=true;}
            else if(!same(entry.getValue(),current)){deliveries.put(entry.getKey(),current);changed=true;}
        }
        if(changed)block.setChanged();return available;
    }
    private static boolean same(Delivery a,Delivery b){
        if(a.input!=b.input || a.stacks.size()!=b.stacks.size())return false;
        for(int i=0;i<a.stacks.size();i++)if(!ItemStack.matches(a.stacks.get(i),b.stacks.get(i)))return false;return true;
    }
    public boolean canRegister(UUID worker){return deliveries.containsKey(worker) || deliveries.size()<MAX_DELIVERIES;}
    public void track(PeepoEntity npc,BlockEntity block){
        if(npc.level() instanceof ServerLevel level){
            for(var id:ProductionDeliveryCancellations.get(level.getServer()).take(GlobalPos.of(level.dimension(),block.getBlockPos())))try{deliveries.remove(UUID.fromString(id));}catch(IllegalArgumentException ignored){}
        }
        var delivery=npc.transport.stockDelivery(block.getBlockPos());
        if(delivery==null)deliveries.remove(npc.getUUID());else if(canRegister(npc.getUUID()))deliveries.put(npc.getUUID(),delivery);
        invalidate();block.setChanged();
    }
    public void forget(UUID worker,BlockEntity block){if(deliveries.remove(worker)!=null){invalidate();block.setChanged();}}
    public List<ItemStack> incoming(){
        var stacks=new ArrayList<ItemStack>();for(var d:deliveries.values())if(d.input)for(var s:d.stacks)stacks.add(s.copy());return stacks;
    }
    private static void add(Map<ItemVariant,Integer> counts,ItemVariant item,long amount){counts.merge(item,(int)Math.min(1_000_000,amount),(a,b)->Math.min(1_000_000,a+b));}
    private int count(List<ItemStack> stacks,ItemStack item){int n=0;for(var s:stacks)if(matches(item,s))n=Math.min(1_000_000,n+s.getCount());return n;}
    private int moving(BlockEntity block,ItemStack item){int n=0;for(var d:deliveries.values())if(!d.input)for(var s:d.stacks)if(finishedItem(block,s) && matches(item,s))n+=s.getCount();return n;}
    private int finished(BlockEntity block,ItemStack icon){
        if(CompanionAdditionalStock.supports(block))return CompanionAdditionalStock.finished(block,icon,this);
        if(block instanceof io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity m && m.kind()==io.github.jimbozoomer.jugcraft.machine.MachineKind.ELECTROPLATING_BATH){int n=0;for(var v:m.companionPort.outputs())if(!v.isResourceBlank() && finishedItem(block,v.getResource().toStack(1)) && matches(icon,v.getResource().toStack(1)))n+=(int)v.getAmount();return n;}
        Item item=icon.getItem();int n=0;
        if(block instanceof CookingPotBlockEntity pot){for(int i=CookingPotBlockEntity.RESULT;i<CookingPotBlockEntity.SLOTS;i++)if(pot.getItem(i).is(item))n+=pot.getItem(i).getCount();if(pot.companionBonusPending().is(item))n+=pot.companionBonusPending().getCount();}
        else if(block instanceof HearthOvenBlockEntity oven){if(oven.pie()!=null && oven.baked()>=HearthOvenBlockEntity.BAKED && oven.baked()<HearthOvenBlockEntity.BURNT && JugcraftAgriculture.item(oven.pie().pie())==item)n++;}
        else if(block instanceof CuttingBoardBlockEntity board){for(var v:board.companionKitchen.outputStorage())if(!v.isResourceBlank() && v.getResource().getItem()==item)n+=(int)v.getAmount();}
        else if(CompanionStockRecipes.supports(block))n=CompanionStockRecipes.finished(block,item);
        return n;
    }
    public List<ItemStack> potInputs(CookingPotBlockEntity pot){
        var in=new ArrayList<ItemStack>();for(int i=0;i<CookingPotBlockEntity.INPUTS;i++)in.add(pot.getItem(i).copy());in.addAll(incoming());return in;
    }
    public static int batches(CookingPotRecipe recipe,List<ItemStack> inputs){
        var parts=recipe.parts().stream().map(p->new RecipeSupplies.Part(p.ingredient(),p.count())).toList();
        int low=0,high=384;
        while(low<high){int mid=(low+high+1)/2;var scaled=parts.stream().map(p->new RecipeSupplies.Part(p.ingredient(),p.count()*mid)).toList();
            if(RecipeSupplies.completes(scaled,inputs,List.of()))low=mid;else high=mid-1;
        }return low;
    }
    private int pending(PeepoEntity npc,BlockEntity block,ItemStack icon){
        if(CompanionAdditionalStock.supports(block))return CompanionAdditionalStock.pending(block,icon,incoming(),this);
        Item item=icon.getItem();var in=incoming();int n=0;
        if(block instanceof CookingPotBlockEntity pot && npc.level() instanceof ServerLevel level){
            var recipe=pot.selectedRecipe()==null?null:CookingPotRecipe.catalog(level.getServer()).get(pot.selectedRecipe());
            if(recipe!=null && recipe.output().create().is(item))n=batches(recipe,potInputs(pot))*recipe.output().create().getCount();
        }else if(block instanceof HearthOvenBlockEntity oven){
            if(oven.pie()!=null && oven.baked()<HearthOvenBlockEntity.BAKED && JugcraftAgriculture.item(oven.pie().pie())==item)n++;
            for(var raw:in){var filling=HearthOvenBlockEntity.rawFilling(raw);if(filling!=null && JugcraftAgriculture.item(filling.pie())==item)n+=raw.getCount();}
        }else if(block instanceof CuttingBoardBlockEntity board){
            if(!board.item().isEmpty())in.add(board.item().copy());
            for(var raw:in){var r=CuttingRecipe.find(((ServerLevel)npc.level()).getServer(),raw,npc.assignedHand()).orElse(null);if(r!=null)for(var s:r.cut())if(s.is(item))n+=s.getCount()*raw.getCount();}
        }else if(CompanionStockRecipes.supports(block))n=CompanionStockRecipes.pending(npc,block,item,in,this);
        return n;
    }
    public Count count(PeepoEntity npc,BlockEntity block,ItemStack output){
        boolean available=refresh(npc,block);
        return new Count(cached.getOrDefault(filter.stockKey(output),0),finished(block,output)+moving(block,output)+pending(npc,block,output),available);
    }
    /** Number of products allowed in inputs + inbound supplies; finished/outbound goods are already stock. */
    public int inputGoal(PeepoEntity npc,BlockEntity block,ItemStack output){
        var rule=rules.get(filter.stockKey(output));if(rule==null)return Integer.MAX_VALUE;
        if(!refresh(npc,block) || !canRegister(npc.getUUID()))return 0;
        int stock=cached.getOrDefault(filter.stockKey(output),0)+finished(block,output)+moving(block,output);
        boolean filling=rule.filling;if(stock>=rule.target)filling=false;else if(stock<=rule.restock)filling=true;
        if(filling!=rule.filling){rules.put(filter.stockKey(output),new Rule(rule.target,rule.restock,filling));block.setChanged();}
        return filling?Math.max(0,rule.target-stock):0;
    }
    public boolean wants(PeepoEntity npc,BlockEntity block,ItemStack output){return inputGoal(npc,block,output)>pending(npc,block,output);}
    public double priority(PeepoEntity npc,BlockEntity block,ItemStack output){
        int target=target(output);if(target==0)return -1;
        int goal=inputGoal(npc,block,output);return goal<=0?-2:(double)Math.max(0,goal-pending(npc,block,output))/target;
    }
    public CompanionStatus idleStatus(PeepoEntity npc,BlockEntity block){
        if(!enabled())return CompanionStatus.NO_INPUT;
        if(!refresh(npc,block))return CompanionStatus.STOCK_UNAVAILABLE;
        for(int i=0;i<9;i++){var s=filter.get(i);if(!s.isEmpty() && wants(npc,block,s))return CompanionStatus.NO_INPUT;}
        return CompanionStatus.STOCKED;
    }
    public String detail(PeepoEntity npc,BlockEntity block){
        if(!enabled())return "";
        if(!refresh(npc,block))return "Stock unavailable; check counted Outputs, access and loaded deliveries.";
        ItemStack selected=ItemStack.EMPTY;double priority=-Double.MAX_VALUE;
        for(int i=0;i<9;i++){var s=filter.get(i);if(s.isEmpty() || target(s)==0)continue;double p=priority(npc,block,s);if(p>priority){selected=s;priority=p;}}
        if(selected.isEmpty())return "";var c=count(npc,block,selected);
        return (priority>0?"Restocking ":"Stocked: ")+selected.getHoverName().getString()+" "+c.stored+" / "+target(selected)+(c.underway>0?"; "+c.underway+" at station / on the way":"")+".";
    }
    public void save(ValueOutput out){
        if(rules.isEmpty() && outputs.isEmpty() && deliveries.isEmpty())return;
        var c=out.child("ProductionStock");int i=0;
        for(var e:rules.entrySet()){var r=c.child("Rule"+i++);r.store("Icon",ItemStack.CODEC,e.getKey().toStack(1));r.putInt("Target",e.getValue().target);r.putInt("Restock",e.getValue().restock);r.putBoolean("Filling",e.getValue().filling);}
        for(i=0;i<outputs.size();i++)CompanionTransport.saveTarget(c,"Output"+i,outputs.get(i));
        i=0;for(var e:deliveries.entrySet()){var d=c.child("Delivery"+i++);d.putString("Worker",e.getKey().toString());d.putBoolean("Input",e.getValue().input);d.store("Items",ItemStack.OPTIONAL_CODEC.listOf(),e.getValue().stacks);}
    }
    public void load(ValueInput in){
        rules.clear();outputs.clear();deliveries.clear();invalidate();revision++;
        var child=in.child("ProductionStock");if(child.isEmpty())return;var c=child.get();
        for(int i=0;i<9;i++){var v=c.child("Rule"+i);if(v.isEmpty())continue;var r=v.get();var icon=r.read("Icon",ItemStack.CODEC).orElseGet(()->r.read("Item",Identifier.CODEC).map(id->new ItemStack(BuiltInRegistries.ITEM.getValue(id))).orElse(ItemStack.EMPTY));if(icon.isEmpty())continue;var item=filter.stockKey(icon);int target=Math.clamp(r.getIntOr("Target",0),0,MAX_TARGET);if(target>0 && contains(item))rules.put(item,new Rule(target,Math.clamp(r.getIntOr("Restock",target/2),0,target-1),r.getBooleanOr("Filling",true)));}
        for(int i=0;i<4;i++){var t=CompanionTransport.loadTarget(c,"Output"+i);if(t!=null)outputs.add(t);}
        for(int i=0;i<MAX_DELIVERIES;i++){var d=c.child("Delivery"+i);if(d.isEmpty())continue;try{var id=UUID.fromString(d.get().getStringOr("Worker",""));var stacks=d.get().read("Items",ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());if(stacks.size()<=8)deliveries.put(id,new Delivery(d.get().getBooleanOr("Input",false),stacks.stream().filter(s->!s.isEmpty()).map(ItemStack::copy).toList()));}catch(IllegalArgumentException ignored){}}
    }
    private List<ItemStack> results(PeepoEntity npc,BlockEntity block,ItemStack raw){
        if(CompanionAdditionalStock.supports(block))return CompanionAdditionalStock.results(block,raw);
        if(block instanceof CookingPotBlockEntity pot)return pot.supplyPlan().map(p->List.of(p.output().create())).orElse(List.of());
        if(block instanceof HearthOvenBlockEntity){var filling=HearthOvenBlockEntity.rawFilling(raw);return filling==null?List.of():List.of(new ItemStack(JugcraftAgriculture.item(filling.pie())));}
        if(block instanceof CuttingBoardBlockEntity){var r=CuttingRecipe.find(((ServerLevel)npc.level()).getServer(),raw,npc.assignedHand()).orElse(null);return r==null?List.of():r.cut();}
        if(CompanionStockRecipes.supports(block)){var r=CompanionStockRecipes.recipe(block,raw);return r!=null && r.ingredient(raw)?r.results():List.of();}
        return List.of();
    }
    private boolean allowPickup(PeepoEntity npc,BlockEntity block,ItemStack raw){
        // Fuel for an already committed pie must remain available even when targets are satisfied.
        if(block instanceof HearthOvenBlockEntity && HearthOvenBlockEntity.rawFilling(raw)==null)return true;
        var results=results(npc,block,raw);
        return results.stream().anyMatch(s->filter.allows(s) && wants(npc,block,s));
    }
    public CompanionLogistics.Port wrap(PeepoEntity npc,BlockEntity block,CompanionAssignments.Target work,CompanionLogistics.Port base){
        if(!enabled())return base;
        return new CompanionLogistics.Port(){
            public Identifier plan(){return base.plan();}
            public void prepare(Storage<ItemVariant> source){
                if(CompanionAdditionalStock.supports(block)){refresh(npc,block);CompanionAdditionalStock.prepare(npc,block,source,base,CompanionProductionStock.this);}
                else if(block instanceof CookingPotBlockEntity pot){
                    refresh(npc,block);
                    boolean empty=true;for(int i=0;i<CookingPotBlockEntity.INPUTS;i++)if(!pot.getItem(i).isEmpty()){empty=false;break;}
                    if(incoming().isEmpty() && (empty || pot.selectedRecipe()==null)){
                        var priorities=new HashMap<Item,Double>();
                        pot.prepareCompanionRecipe(source,s->priorities.computeIfAbsent(s.getItem(),item->wants(npc,block,s)?priority(npc,block,s):-2));
                    }
                }else if(CompanionStockRecipes.supports(block)){refresh(npc,block);CompanionStockRecipes.prepare(npc,block,source,CompanionProductionStock.this);}
                else base.prepare(source);
            }
            public int needed(ItemStack stack){return base.needed(stack);} // Already committed cargo can finish.
            private boolean pickupAllowed(ItemStack stack){
                if(!allowPickup(npc,block,stack))return false;
                if(block instanceof CuttingBoardBlockEntity && !incoming().isEmpty())return false;
                return !(block instanceof HearthOvenBlockEntity && HearthOvenBlockEntity.rawFilling(stack)!=null && incoming().stream().anyMatch(s->HearthOvenBlockEntity.rawFilling(s)!=null));
            }
            public int pickupNeeded(ItemStack stack){
                if(CompanionAdditionalStock.supports(block))return CompanionAdditionalStock.limit(npc,block,stack,base.pickupNeeded(stack),CompanionProductionStock.this);
                if(CompanionStockRecipes.supports(block))return CompanionStockRecipes.limit(npc,block,stack,base.pickupNeeded(stack),base.stockAuxiliaryNeed(stack),CompanionProductionStock.this);
                if(!pickupAllowed(stack))return 0;
                return block instanceof CookingPotBlockEntity pot?CookingPotSupplies.missing(pot,stack,npc):base.pickupNeeded(stack);
            }
            public double supplyPriority(ItemStack raw){return results(npc,block,raw).stream().filter(filter::allows).mapToDouble(s->priority(npc,block,s)).max().orElse(-3);}
            public int needed(ItemStack stack,net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext tx){
                if(CompanionAdditionalStock.supports(block))return CompanionAdditionalStock.limit(npc,block,stack,base.needed(stack,tx),CompanionProductionStock.this);
                if(CompanionStockRecipes.supports(block))return CompanionStockRecipes.limit(npc,block,stack,base.needed(stack,tx),base.stockAuxiliaryNeed(stack),CompanionProductionStock.this);
                if(!pickupAllowed(stack))return 0;
                int needed=base.needed(stack,tx);
                return block instanceof CookingPotBlockEntity pot?Math.min(needed,CookingPotSupplies.missing(pot,stack,npc)):needed;
            }
            public Storage<ItemVariant> inputs(){return CompanionStockRecipes.supports(block)?base.stockInputs(raw->CompanionStockRecipes.ingredientRoom(npc,block,raw,CompanionProductionStock.this)):base.inputs();}
            public Storage<ItemVariant> outputs(){return base.outputs();}
            public List<ItemStack> supplyBatch(Storage<ItemVariant> source,int slots){
                invalidate();if(!refresh(npc,block) || !canRegister(npc.getUUID()))return List.of();
                if(block instanceof CookingPotBlockEntity pot)return CookingPotSupplies.create(pot,source,npc.assignments.supplies.combined(work,false),slots,npc);
                return CompanionDeliveryPlan.create(source,inputs(),slots,this::needed,null,this::supplyPriority);
            }
            public CompanionStatus status(){var state=base.status();return (CompanionStockRecipes.supports(block) || CompanionAdditionalStock.supports(block))?idleStatus(npc,block,state):state==CompanionStatus.NO_INPUT?idleStatus(npc,block):state;}
        };
    }
    public CompanionStatus idleStatus(PeepoEntity npc,BlockEntity block,CompanionStatus nativeStatus){
        if(!enabled() || (CompanionAdditionalStock.supports(block)?CompanionAdditionalStock.busy(block):CompanionStockRecipes.busy(block)))return nativeStatus;
        if(nativeStatus!=CompanionStatus.NO_INPUT && nativeStatus!=CompanionStatus.RECIPE_MISSING && nativeStatus!=CompanionStatus.IDLE
            && nativeStatus!=CompanionStatus.MACHINE_TOOL && nativeStatus!=CompanionStatus.NEEDS_CERAMICS && nativeStatus!=CompanionStatus.NEEDS_BOTTLES && nativeStatus!=CompanionStatus.NO_HEAT)return nativeStatus;
        var state=idleStatus(npc,block);return state==CompanionStatus.NO_INPUT?nativeStatus:state;
    }
}
