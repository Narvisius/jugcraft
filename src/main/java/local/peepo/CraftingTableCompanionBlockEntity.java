package local.peepo;

import java.util.*;
import java.util.function.ToDoubleFunction;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.storage.base.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

/** Hidden companion-only workspace. Vanilla player crafting is untouched; no independent block ticker. */
public final class CraftingTableCompanionBlockEntity extends BlockEntity {
    public static final int WORK_TICKS=60, EFFORT=4, BATCHES=8;
    public record Plan(Identifier id,List<ItemStack> pattern,List<RecipeSupplies.Part> parts,ItemStack result) {}
    public final CompanionRecipeFilter filter=new CompanionRecipeFilter();
    public final CraftingTableJob job=new CraftingTableJob(this);
    private final ItemStack[] input=new ItemStack[9],output=new ItemStack[12];
    private List<ItemStack> pattern=List.of();
    private Identifier selected;
    private int progress,cursor;
    private long lastAdvance=Long.MIN_VALUE;
    private CraftingTableRecipes catalog;
    private CraftingTableRecipes.Entry entry;
    private Plan cachedPlan;
    private long inventoryRevision,readinessRevision=-1;
    private CompanionStatus readiness;
    private final SnapshotParticipant<List<ItemStack>> inventory=new SnapshotParticipant<>(){
        protected List<ItemStack> createSnapshot(){var items=new ArrayList<ItemStack>(21);for(var s:input)items.add(s.copy());for(var s:output)items.add(s.copy());return items;}
        protected void readSnapshot(List<ItemStack> items){for(int i=0;i<9;i++)input[i]=items.get(i);for(int i=0;i<12;i++)output[i]=items.get(9+i);touch();}
        protected void onFinalCommit(){changed();}
    };
    public CraftingTableCompanionBlockEntity(BlockPos pos,BlockState state){super(CraftingTables.ENTITY,pos,state);Arrays.fill(input,ItemStack.EMPTY);Arrays.fill(output,ItemStack.EMPTY);}
    private boolean live(){return !isRemoved() && level instanceof ServerLevel && getBlockState().is(Blocks.CRAFTING_TABLE);}
    public boolean accessible(PeepoEntity npc){return live() && level==npc.level() && npc.orders.tamed() && npc.assignments.assignedWork(worldPosition) && CompanionJobs.permitted(npc,worldPosition);}
    private void changed(){setChanged();filter.stock.invalidate();CompanionReadiness.invalidate(this);}
    private void touch(){inventoryRevision++;readinessRevision=-1;}
    private CraftingTableRecipes recipes(){
        if(!(level instanceof ServerLevel server))return null;
        var current=CraftingTableRecipes.of(server.getServer().getRecipeManager());
        if(catalog!=current){if(catalog!=null){progress=0;changed();}catalog=current;entry=selected==null?null:current.ids.get(selected);cachedPlan=null;touch();}
        return current;
    }
    private CraftingTableRecipes.Entry choice(){var recipes=recipes();if(recipes==null || selected==null)return null;if(entry==null || !entry.id().equals(selected))entry=recipes.ids.get(selected);return entry;}
    public boolean acceptsFilter(ItemStack icon){var c=recipes();return icon.isEmpty() || c!=null && c.products.containsKey(icon.getItem());}
    public void filterChanged(){var plan=plan();if(!hasInputs() || plan==null || !filter.allows(plan.result())){selected=null;entry=null;pattern=List.of();progress=0;cachedPlan=null;}touch();changed();}
    public int progress(){return progress;}
    public List<ItemStack> installed(){return Arrays.stream(input).map(ItemStack::copy).toList();}
    public boolean hasInputs(){return Arrays.stream(input).anyMatch(s->!s.isEmpty());}
    public boolean hasOutput(){return Arrays.stream(output).anyMatch(s->!s.isEmpty());}
    public Plan plan(){
        var recipe=choice();if(recipe==null || pattern.size()!=9)return null;
        if(cachedPlan!=null)return cachedPlan;
        var grid=CraftingInput.of(3,3,pattern);if(!recipe.recipe().matches(grid,level))return null;
        var result=recipe.recipe().assemble(grid);if(result.isEmpty() || !filter.allows(result))return null;
        var parts=recipe.slots().stream().flatMap(Optional::stream).map(i->new RecipeSupplies.Part(i,1)).toList();
        cachedPlan=new Plan(recipe.id(),pattern.stream().map(ItemStack::copy).toList(),parts,result);return cachedPlan;
    }
    public void prepare(Storage<ItemVariant> source,ToDoubleFunction<ItemStack> priority){
        if(!live() || hasInputs() || !filter.stock.incoming().isEmpty())return;
        var choices=new ArrayList<>(recipes().choices(filter));if(choices.isEmpty()){selected=null;entry=null;pattern=List.of();cachedPlan=null;touch();return;}
        var scores=new HashMap<Item,Double>();ToDoubleFunction<CraftingTableRecipes.Entry> score=e->priority==null?0:scores.computeIfAbsent(e.icon().getItem(),i->priority.applyAsDouble(e.icon()));
        choices.sort(Comparator.<CraftingTableRecipes.Entry>comparingDouble(score).reversed().thenComparingInt(e->e.id().equals(selected)?0:1));
        var supplies=RecipeSupplies.available(source);int limit=Math.min(256,choices.size());
        // With no filter, continue a bounded page if a huge catalog has no available recipe on its first page.
        int start=filter.empty() && priority==null?Math.floorMod(cursor,choices.size()):0;
        for(int i=0;i<limit;i++){
            var candidate=choices.get((start+i)%choices.size());if(score.applyAsDouble(candidate)<=-2)continue;
            var layout=CraftingTableRecipes.pattern(candidate,supplies);if(layout.isEmpty())continue;
            var grid=CraftingInput.of(3,3,layout);if(!candidate.recipe().matches(grid,level) || !filter.allows(candidate.recipe().assemble(grid)))continue;
            if(!candidate.id().equals(selected) || !same(pattern,layout))progress=0;
            selected=candidate.id();entry=candidate;pattern=layout;cachedPlan=null;cursor=0;touch();changed();return;
        }
        cursor=(start+limit)%choices.size();
        // Retain a valid plan while physical deliveries travel, including Unlimited jobs.
        if(choice()==null){selected=null;entry=null;pattern=List.of();cachedPlan=null;touch();}
    }
    private static boolean same(List<ItemStack> a,List<ItemStack> b){if(a.size()!=b.size())return false;for(int i=0;i<a.size();i++)if(!ItemStack.matches(a.get(i),b.get(i)))return false;return true;}
    private int needed(ItemStack stack){
        if(!live() || plan()==null || stack.isEmpty())return 0;int n=0;
        for(int i=0;i<9;i++)if(ItemStack.isSameItemSameComponents(pattern.get(i),stack) && (input[i].isEmpty() || ItemStack.isSameItemSameComponents(input[i],stack)))n+=Math.max(0,Math.min(BATCHES,stack.getMaxStackSize())-input[i].getCount());return n;
    }
    private final Storage<ItemVariant> inputs=new InsertionOnlyStorage<>(){
        public long insert(ItemVariant variant,long amount,TransactionContext tx){
            StoragePreconditions.notBlankNotNegative(variant,amount);var stack=variant.toStack(1);long remaining=Math.min(amount,needed(stack));if(remaining<=0)return 0;
            inventory.updateSnapshots(tx);touch();long moved=0;
            for(int layer=1;layer<=Math.min(BATCHES,stack.getMaxStackSize()) && moved<remaining;layer++)for(int i=0;i<9 && moved<remaining;i++)
                if(ItemStack.isSameItemSameComponents(pattern.get(i),stack) && input[i].getCount()<layer && (input[i].isEmpty() || ItemStack.isSameItemSameComponents(input[i],stack))){
                    if(input[i].isEmpty())input[i]=stack.copy();else input[i].grow(1);moved++;
                }
            return moved;
        }
    };
    public Storage<ItemVariant> inputs(){return inputs;}
    private boolean obsolete(int slot){return !input[slot].isEmpty() && (plan()==null || !ItemStack.isSameItemSameComponents(pattern.get(slot),input[slot]));}
    private final class OutputSlot implements SingleSlotStorage<ItemVariant>{
        private final int slot;OutputSlot(int slot){this.slot=slot;}
        private ItemStack stack(){return slot<12?output[slot]:obsolete(slot-12)?input[slot-12]:ItemStack.EMPTY;}
        public boolean supportsInsertion(){return false;}
        public long insert(ItemVariant item,long amount,TransactionContext tx){return 0;}
        public long extract(ItemVariant item,long amount,TransactionContext tx){StoragePreconditions.notBlankNotNegative(item,amount);var stack=stack();if(!item.equals(ItemVariant.of(stack)))return 0;long n=Math.min(amount,stack.getCount());if(n>0){inventory.updateSnapshots(tx);touch();stack.shrink((int)n);}return n;}
        public ItemVariant getResource(){return ItemVariant.of(stack());}
        public boolean isResourceBlank(){return stack().isEmpty();}
        public long getAmount(){return stack().getCount();}
        public long getCapacity(){return 64;}
    }
    private final Storage<ItemVariant> outputs=new CombinedStorage<>(java.util.stream.IntStream.range(0,21).mapToObj(OutputSlot::new).toList());
    public Storage<ItemVariant> outputs(){return outputs;}
    public Storage<ItemVariant> outputStorage(){return outputs;}
    public CompanionLogistics.Port forCompanion(PeepoEntity npc){return new CompanionLogistics.Port(){
        public Identifier plan(){var p=CraftingTableCompanionBlockEntity.this.plan();return accessible(npc) && p!=null?p.id():null;}
        public void prepare(Storage<ItemVariant> source){if(accessible(npc))CraftingTableCompanionBlockEntity.this.prepare(source,null);}
        public int needed(ItemStack stack){return accessible(npc)?CraftingTableCompanionBlockEntity.this.needed(stack):0;}
        public Storage<ItemVariant> inputs(){return new InsertionOnlyStorage<>(){public long insert(ItemVariant item,long amount,TransactionContext tx){return accessible(npc)?CraftingTableCompanionBlockEntity.this.inputs.insert(item,amount,tx):0;}};}
        public Storage<ItemVariant> outputs(){return accessible(npc)?outputs:Storage.empty();}
        public CompanionStatus status(){return accessible(npc)?CraftingTableCompanionBlockEntity.this.status():CompanionStatus.FORBIDDEN;}
    };}
    private CraftingInput grid(){return CraftingInput.of(3,3,Arrays.stream(input).map(s->s.copyWithCount(1)).toList());}
    private boolean complete(){
        if(plan()==null)return false;
        for(int i=0;i<9;i++)if(pattern.get(i).isEmpty()?!input[i].isEmpty():input[i].isEmpty() || !ItemStack.isSameItemSameComponents(pattern.get(i),input[i]))return false;
        return true;
    }
    private List<ItemStack> products(){var e=choice();if(e==null || !complete())return List.of();var grid=grid();var result=e.recipe().assemble(grid);if(result.isEmpty())return List.of();var products=new ArrayList<ItemStack>();products.add(result);for(var s:e.recipe().getRemainingItems(grid))if(!s.isEmpty())products.add(s.copy());return products;}
    private ItemStack[] buffered(List<ItemStack> products){
        var copy=Arrays.stream(output).map(ItemStack::copy).toArray(ItemStack[]::new);
        for(var product:products){var left=product.copy();for(int pass=0;pass<2;pass++)for(int i=0;i<copy.length && !left.isEmpty();i++){
            if(copy[i].isEmpty()!=(pass==1) || !copy[i].isEmpty() && !ItemStack.isSameItemSameComponents(copy[i],left))continue;
            int n=Math.min(left.getCount(),left.getMaxStackSize()-copy[i].getCount());if(n<=0)continue;
            if(copy[i].isEmpty())copy[i]=left.copyWithCount(n);else copy[i].grow(n);left.shrink(n);
        }if(!left.isEmpty())return null;}return copy;
    }
    public CompanionStatus status(){
        if(!live())return CompanionStatus.FORBIDDEN;
        choice();if(readinessRevision==inventoryRevision)return readiness;
        if(plan()==null)readiness=hasInputs()?CompanionStatus.FULL:CompanionStatus.RECIPE_MISSING;
        else if(!complete())readiness=hasOutput()?CompanionStatus.FULL:CompanionStatus.NO_INPUT;
        else{var products=products();readiness=products.isEmpty()?CompanionStatus.RECIPE_MISSING:buffered(products)==null?CompanionStatus.FULL:CompanionStatus.READY;}
        readinessRevision=inventoryRevision;return readiness;
    }
    public CompanionStatus advance(PeepoEntity npc){
        if(!accessible(npc))return CompanionStatus.FORBIDDEN;
        if(!npc.preferences.canWork() || npc.editingSettings())return CompanionStatus.IDLE;
        var state=status();if(state!=CompanionStatus.READY)return state;
        long now=level.getGameTime();if(lastAdvance==now)return CompanionStatus.WORKING;
        ItemStack[] finished=null;
        if(progress==WORK_TICKS-1){var e=choice();var grid=grid();if(e==null || !e.recipe().matches(grid,level))return CompanionStatus.RECIPE_MISSING;var products=products();finished=buffered(products);if(products.isEmpty() || finished==null)return CompanionStatus.FULL;}
        if(!npc.hasWorkEnergy((int)Math.ceil(EFFORT*(100-npc.uniforms.saving(CompanionUniforms.Type.MECHANIC))/100.0)))return CompanionStatus.RECOVERING;
        try(var tx=Transaction.openOuter()){
            if(npc.extractWorkEnergy(CompanionUniforms.Type.MECHANIC,EFFORT,tx)!=EFFORT)return CompanionStatus.RECOVERING;
            tx.commit();
        }
        // The clock follows paid progress, so resumed crafts do not restart or speed up their visual phase.
        // During continuous work this timestamp and the one-item previews remain unchanged on the wire.
        var selectedPlan=plan();ItemStack first=ItemStack.EMPTY,second=ItemStack.EMPTY;
        for(var ingredient:selectedPlan.pattern())if(!ingredient.isEmpty()){
            if(first.isEmpty())first=ingredient;
            else if(second.isEmpty())second=ingredient;
            if(!second.isEmpty() && !ItemStack.isSameItemSameComponents(first,ingredient)){second=ingredient;break;}
        }
        npc.setCraftingVisuals(first,second,selectedPlan.result(),now-progress);
        lastAdvance=now;progress++;
        if(progress>=WORK_TICKS){
            for(var stack:input)if(!stack.isEmpty())stack.shrink(1);
            System.arraycopy(finished,0,output,0,output.length);progress=0;
            touch();
        }
        if(finished!=null)changed();else setChanged();return CompanionStatus.WORKING;
    }
    public boolean recoverable(){return hasInputs() || hasOutput();}
    public void recover(Player player){for(var stack:input)if(!stack.isEmpty())player.getInventory().add(stack);for(var stack:output)if(!stack.isEmpty())player.getInventory().add(stack);progress=0;touch();changed();}
    @Override public void preRemoveSideEffects(BlockPos pos,BlockState state){
        if(level instanceof ServerLevel){for(int i=0;i<9;i++){if(!input[i].isEmpty())Block.popResource(level,pos,input[i]);input[i]=ItemStack.EMPTY;}for(int i=0;i<12;i++){if(!output[i].isEmpty())Block.popResource(level,pos,output[i]);output[i]=ItemStack.EMPTY;}}
        super.preRemoveSideEffects(pos,state);
    }
    @Override public void setRemoved(){job.removed();super.setRemoved();}
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);filter.save(out);out.store("TableInputs",ItemStack.OPTIONAL_CODEC.listOf(),Arrays.asList(input));out.store("TableOutputs",ItemStack.OPTIONAL_CODEC.listOf(),Arrays.asList(output));out.store("TablePattern",ItemStack.OPTIONAL_CODEC.listOf(),pattern);if(selected!=null)out.store("TableRecipe",Identifier.CODEC,selected);out.putInt("TableProgress",progress);
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);filter.load(in);Arrays.fill(input,ItemStack.EMPTY);Arrays.fill(output,ItemStack.EMPTY);
        in.read("TableInputs",ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(items->{for(int i=0;i<Math.min(9,items.size());i++)input[i]=items.get(i).copy();});
        in.read("TableOutputs",ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(items->{for(int i=0;i<Math.min(12,items.size());i++)output[i]=items.get(i).copy();});
        var layout=in.read("TablePattern",ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());pattern=layout.size()==9?layout.stream().map(s->s.copyWithCount(1)).toList():List.of();
        selected=in.read("TableRecipe",Identifier.CODEC).orElse(null);progress=Math.clamp(in.getIntOr("TableProgress",0),0,WORK_TICKS-1);entry=null;catalog=null;cachedPlan=null;lastAdvance=Long.MIN_VALUE;touch();
    }
}
