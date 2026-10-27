package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.workshop.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.storage.base.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Recipe-aware deliveries into the native work lane. Tools and ceramics are real, retained inventory. */
public final class WorkshopCompanion {
    private final WorkshopBlockEntity station;
    public final CompanionRecipeFilter filter=new CompanionRecipeFilter();
    public final StarterWorkshopJob job;
    public WorkshopCompanion(WorkshopBlockEntity station){this.station=station;job=new StarterWorkshopJob(station,this);}

    public boolean accessible(PeepoEntity npc){
        if(station.isRemoved() || station.isLocked() || station.getLevel()!=npc.level() || !(npc.level() instanceof ServerLevel)
            || !npc.orders.tamed() || !npc.assignments.assignedWork(station.getBlockPos()))return false;
        var block=(WorkshopBlock)station.getBlockState().getBlock();
        if(!block.complete(npc.level(),station.getBlockPos(),station.getBlockState()))return false;
        var facing=station.getBlockState().getValue(WorkshopBlock.FACING);
        for(int i=0;i<block.footprint().size();i++){
            BlockPos at=block.footprint().partPos(station.getBlockPos(),facing,i);
            if(!CompanionJobs.permitted(npc,at) || !npc.level().getWorldBorder().isWithinBounds(at))return false;
        }
        return true;
    }
    public boolean acceptsFilter(ItemStack stack){return stack.isEmpty() || station.catalog().entries().stream().anyMatch(e->e.recipe().result().create().is(stack.getItem()));}
    private WorkshopCatalog.Entry choice(){
        if(!(station.getLevel() instanceof ServerLevel) || station.isRemoved())return null;
        var selected=station.selection();
        return selected!=null && filter.allows(selected.recipe().result().create())?selected:null;
    }
    private List<ItemStack> installed(){return List.of(station.getItem(0),station.getItem(1),station.getItem(2));}
    private boolean compatible(WorkshopRecipe recipe){
        return installed().stream().allMatch(s->s.isEmpty() || recipe.parts().stream().anyMatch(p->p.ingredient().test(s)));
    }
    private boolean complete(WorkshopRecipe recipe,List<ItemStack> supplies){
        return RecipeSupplies.completes(recipe.parts().stream().map(p->new RecipeSupplies.Part(p.ingredient(),p.count())).toList(),installed(),supplies);
    }
    private boolean toolAvailable(WorkshopRecipe recipe,List<ItemStack> supplies){
        var tool=recipe.toolStack();return tool.isEmpty() || ItemStack.isSameItemSameComponents(tool,station.getItem(WorkshopBlockEntity.TOOL))
            || supplies.stream().anyMatch(s->ItemStack.isSameItemSameComponents(s,tool));
    }
    void prepareInstalled(){if(!filter.stock.enabled() || filter.stock.incoming().isEmpty() && installed().stream().anyMatch(s->!s.isEmpty()))prepare(null);}
    /** Called only by the existing budgeted route planner, never by a machine ticker. */
    private void prepare(Storage<ItemVariant> source){
        prepare(source,s->0);
    }
    public void prepare(Storage<ItemVariant> source,java.util.function.ToDoubleFunction<ItemStack> priority){
        if(station.busy() || station.playerWorking())return;
        var supplies=RecipeSupplies.available(source);
        var current=choice();
        var entries=new ArrayList<>(station.catalog().entries());
        entries.sort(Comparator.<WorkshopCatalog.Entry>comparingDouble(e->priority.applyAsDouble(e.recipe().result().create())).reversed()
            .thenComparingInt(e->current!=null && current.id().equals(e.id())?0:1));
        if(current!=null && !entries.isEmpty() && entries.getFirst().id().equals(current.id()) && priority.applyAsDouble(current.recipe().result().create())>-2
            && complete(current.recipe(),supplies) && toolAvailable(current.recipe(),supplies))return;
        WorkshopCatalog.Entry fallback=null,needsTool=null;
        for(var entry:entries){
            var recipe=entry.recipe();
            if(!filter.allows(recipe.result().create()) || priority.applyAsDouble(recipe.result().create())<=-2)continue;
            if(fallback==null)fallback=entry;
            if(!compatible(recipe))continue;
            if(complete(recipe,supplies)){
                if(toolAvailable(recipe,supplies)){station.select(entry.id());CompanionReadiness.invalidate(station);return;}
                if(needsTool==null)needsTool=entry;
            }
        }
        if(needsTool!=null){station.select(needsTool.id());CompanionReadiness.invalidate(station);return;}
        if(current==null && fallback!=null){station.select(fallback.id());CompanionReadiness.invalidate(station);}
    }
    public CompanionStatus status(){
        // A committed batch finishes even if its filter changes; only subsequent deliveries are filtered.
        if(!station.busy() && choice()==null)return CompanionStatus.RECIPE_MISSING;
        return switch(station.status()){
            case 0 -> station.station()==0?CompanionStatus.OCCUPIED:CompanionStatus.IDLE;
            case 1 -> CompanionStatus.NO_INPUT;
            case 2 -> CompanionStatus.MACHINE_TOOL;
            case 3 -> CompanionStatus.NEEDS_CERAMICS;
            case 4 -> CompanionStatus.NO_HEAT;
            case 5 -> CompanionStatus.FULL;
            case 6 -> CompanionStatus.READY;
            case 7 -> CompanionStatus.MACHINE_PAUSED;
            default -> CompanionStatus.MACHINE_UNFORMED;
        };
    }
    private int[] needs(ItemStack stack){
        int[] needs=new int[WorkshopBlockEntity.SLOTS];var selected=choice();
        if(stack.isEmpty() || selected==null || station.playerWorking() || !io.github.jimbozoomer.jugcraft.config.JugcraftConfig.isFeatureEnabled("machines"))return needs;
        var recipe=selected.recipe();
        // Keep a balanced batch of up to eight crafts instead of filling all slots with the first ingredient.
        int batches=Math.max(1,Math.min(8,recipe.result().create().getMaxStackSize()/recipe.result().create().getCount()));
        for(var part:recipe.parts())batches=Math.min(batches,Math.max(1,64/part.count()));
        for(var part:recipe.parts()){
            if(!part.ingredient().test(stack))continue;
            int held=0;for(int i=0;i<3;i++)if(part.ingredient().test(station.getItem(i)))held+=station.getItem(i).getCount();
            int missing=Math.max(0,part.count()*batches-held);
            for(int pass=0;pass<2 && missing>0;pass++)for(int i=0;i<3 && missing>0;i++){
                var present=station.getItem(i);
                if(present.isEmpty()!=(pass==1) || !station.canPlaceItem(i,stack)
                    || !present.isEmpty() && !ItemStack.isSameItemSameComponents(present,stack))continue;
                int n=Math.min(missing,stack.getMaxStackSize()-present.getCount());needs[i]=Math.max(needs[i],n);missing-=n;
            }
        }
        var tool=recipe.toolStack();
        if(!tool.isEmpty() && ItemStack.isSameItemSameComponents(tool,stack) && station.getItem(WorkshopBlockEntity.TOOL).isEmpty() && !station.locked(WorkshopBlockEntity.TOOL))needs[WorkshopBlockEntity.TOOL]=1;
        if(recipe.heated() && station.fuelTicks(stack)>0){
            var fuel=station.getItem(WorkshopBlockEntity.FUEL);
            // Ingredients that also burn belong in the recipe slots first.
            if(Arrays.stream(needs,0,3).sum()==0 && (fuel.isEmpty() || ItemStack.isSameItemSameComponents(fuel,stack)))
                needs[WorkshopBlockEntity.FUEL]=Math.max(0,Math.min(8,(recipe.time()*batches+station.fuelTicks(stack)-1)/station.fuelTicks(stack))-fuel.getCount());
        }
        for(int slot:new int[]{WorkshopBlockEntity.LINING,WorkshopBlockEntity.CRUCIBLE})
            if(station.getItem(slot).isEmpty() && station.canPlaceItem(slot,stack))needs[slot]=1;
        return needs;
    }
    private boolean outputSlot(int slot){
        if(slot==WorkshopBlockEntity.OUTPUT)return true;
        var stack=station.getItem(slot);if(stack.isEmpty())return false;
        if(slot==WorkshopBlockEntity.FUEL)return station.fuelTicks(stack)==0;
        if(station.busy() || station.playerWorking())return false;
        var selected=choice();if(selected==null)return false;
        if(slot==WorkshopBlockEntity.TOOL)return !ItemStack.isSameItemSameComponents(stack,selected.recipe().toolStack());
        return slot<3 && selected.recipe().parts().stream().noneMatch(p->p.ingredient().test(stack));
    }
    public CompanionLogistics.Port forCompanion(PeepoEntity npc){return new CompanionLogistics.Port(){
        public void prepare(Storage<ItemVariant> source){if(accessible(npc))WorkshopCompanion.this.prepare(source);}
        public Identifier plan(){var c=choice();return c==null?null:c.id();}
        public int needed(ItemStack stack){return accessible(npc)?Arrays.stream(needs(stack)).sum():0;}
        public int stockAuxiliaryNeed(ItemStack stack){
            if(!accessible(npc))return 0;var n=needs(stack);
            var recipe=choice();int fuel=n[WorkshopBlockEntity.FUEL];
            if(fuel>0 && recipe!=null){
                int batches=CompanionStockRecipes.desiredBatches(npc,station,CompanionStockRecipes.recipe(station,ItemStack.EMPTY),filter.stock);
                int ticks=recipe.recipe().time()*Math.min(8,Math.max(batches,CompanionStockRecipes.loadedBatches(station)));
                if(station.lane()!=null)ticks+=station.lane().duration()-station.lane().progress();
                ticks=Math.max(0,ticks-station.fuelCredit());
                int duration=station.fuelTicks(stack);
                fuel=Math.min(fuel,Math.max(0,(ticks+duration-1)/duration-station.getItem(WorkshopBlockEntity.FUEL).getCount()));
            }
            return n[WorkshopBlockEntity.TOOL]+fuel+n[WorkshopBlockEntity.LINING]+n[WorkshopBlockEntity.CRUCIBLE];
        }
        public CompanionStatus status(){return accessible(npc)?WorkshopCompanion.this.status():CompanionStatus.FORBIDDEN;}
        public Storage<ItemVariant> inputs(){return new InsertionOnlyStorage<>(){
            public long insert(ItemVariant item,long amount,TransactionContext tx){
                StoragePreconditions.notBlankNotNegative(item,amount);if(!accessible(npc) || amount==0)return 0;
                var stack=item.toStack(1);var slots=needs(stack);var storage=ContainerStorage.of(station,null);long moved=0;
                for(int i=0;i<slots.length && moved<amount;i++)if(slots[i]>0)
                    moved+=storage.getSlot(i).insert(item,Math.min(amount-moved,slots[i]),tx);
                return moved;
            }
        };}
        public Storage<ItemVariant> outputs(){
            var slots=new ArrayList<Storage<ItemVariant>>();
            for(int i=0;i<WorkshopBlockEntity.SLOTS;i++){
                final int slot=i;
                slots.add(new SingleSlotStorage<>(){
                    private boolean allowed(){return accessible(npc) && outputSlot(slot);}
                    public boolean supportsInsertion(){return false;}
                    public long insert(ItemVariant item,long amount,TransactionContext tx){return 0;}
                    public long extract(ItemVariant item,long amount,TransactionContext tx){StoragePreconditions.notBlankNotNegative(item,amount);return allowed()?ContainerStorage.of(station,null).getSlot(slot).extract(item,amount,tx):0;}
                    public ItemVariant getResource(){return allowed()?ItemVariant.of(station.getItem(slot)):ItemVariant.blank();}
                    public boolean isResourceBlank(){return getResource().isBlank();}
                    public long getAmount(){return allowed()?station.getItem(slot).getCount():0;}
                    public long getCapacity(){return 64;}
                });
            }
            return new CombinedStorage<>(slots);
        }
    };}
}
