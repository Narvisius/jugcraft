package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.*;
import local.peepo.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.storage.base.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.*;

/** A real reusable tray, staged ingredients and four output slots. No fake player or off-screen work. */
public final class CandyKettleCompanion extends SnapshotParticipant<List<ItemStack>> implements CompanionLogistics.Port {
    private static final int BASE=0,SUGAR=1,TRAY=2,STICKS=3,FLAVOUR=4,DYE=6,OUTPUT=7,SLOTS=11;
    private static final CandyKind[] PREFERENCE={CandyKind.HARD_CANDY,CandyKind.FUDGE,CandyKind.CANDY_CORN,
        CandyKind.CREAM_CARAMEL,CandyKind.TOFFEE,CandyKind.CARAMEL,CandyKind.SALT_WATER_TAFFY,
        CandyKind.LOLLIPOP,CandyKind.ROCK_CANDY,CandyKind.BURNT_SUGAR};
    private final CandyKettleBlockEntity kettle;
    private final ItemStack[] items=new ItemStack[SLOTS];
    public final CompanionRecipeFilter filter=new CompanionRecipeFilter();
    private CandyKind planned,active,trayTarget;
    private int sugarGoal=4,pullTicks;
    private boolean returnInputs;
    private long lastWorked=Long.MIN_VALUE;
    public CandyKettleCompanion(CandyKettleBlockEntity kettle){this.kettle=kettle;Arrays.fill(items,ItemStack.EMPTY);}
    private long now(){return kettle.getLevel().getGameTime();}
    private boolean live(){return !kettle.isRemoved() && kettle.getLevel() instanceof ServerLevel;}
    private CandyBatch trayBatch(){return CandyTrayItem.batch(items[TRAY]);}
    public boolean tending(){var batch=trayBatch();return live() && (active!=null && kettle.batch() || batch!=null && batch.pullable(now()));}
    private boolean hasOutputs(){for(int i=OUTPUT;i<SLOTS;i++)if(!items[i].isEmpty())return true;return false;}
    private boolean staged(){for(int i=0;i<OUTPUT;i++)if(i!=TRAY && !items[i].isEmpty())return true;return false;}
    private void changed(){kettle.setChanged();CompanionReadiness.invalidate(kettle);}
    protected List<ItemStack> createSnapshot(){return Arrays.stream(items).map(ItemStack::copy).toList();}
    protected void readSnapshot(List<ItemStack> snapshot){for(int i=0;i<SLOTS;i++)items[i]=snapshot.get(i).copy();}
    protected void onFinalCommit(){changed();}
    public static boolean acceptsFilter(ItemStack icon){return icon.isEmpty() || Arrays.stream(CandyKind.values()).anyMatch(k->icon.is(k.item()));}
    public void filterChanged(){
        // A batch already heating/setting finishes honestly. Unused staging is returned before choosing a different recipe.
        if(!kettle.batch() && trayBatch()==null){planned=null;returnInputs=staged();}
        changed();
    }
    private static CandyKind kettleKind(CandyKind kind){return kind==CandyKind.LOLLIPOP?CandyKind.HARD_CANDY:kind;}
    private static CandyBase baseFor(CandyKind kind){
        return switch(kind){case FUDGE,CREAM_CARAMEL,TOFFEE->CandyBase.CREAM;default->CandyBase.SYRUP;};
    }
    private static CandyBase baseOf(ItemStack item){return CandyKettleBlock.waterBottle(item)?CandyBase.SYRUP:item.is(Items.MILK_BUCKET)?CandyBase.CREAM:null;}
    private static int count(List<ItemStack> stacks,java.util.function.Predicate<ItemStack> match){
        int n=0;for(var s:stacks)if(match.test(s))n+=s.getCount();return n;
    }
    public void prepare(Storage<ItemVariant> source){
        prepare(source,s->Integer.MAX_VALUE,s->0);
    }
    public ItemStack stockProduct(){var kind=trayTarget!=null?trayTarget:active!=null?active:planned;return kind==null?ItemStack.EMPTY:new ItemStack(kind.item());}
    public boolean stockBusy(){return staged() || kettle.batch() || trayBatch()!=null;}
    public int stockPending(List<ItemStack> incoming){
        var batch=trayBatch();if(batch!=null)return batch.pieces();
        int sugar=kettle.sugar()+items[SUGAR].getCount()+count(incoming,s->s.is(Items.SUGAR));
        return sugar*CandyKettleBlockEntity.PIECES_PER_SUGAR;
    }
    public void prepare(Storage<ItemVariant> source,java.util.function.ToIntFunction<ItemStack> goal,java.util.function.ToDoubleFunction<ItemStack> priority){
        if(!live() || active!=null || trayBatch()!=null || hasOutputs())return;
        if(returnInputs){if(staged())return;returnInputs=false;}
        if(planned!=null && (staged() || kettle.batch()) && filter.allows(new ItemStack(planned.item())))return;
        var available=new ArrayList<>(RecipeSupplies.available(source));
        for(int i=0;i<OUTPUT;i++)if(!items[i].isEmpty())available.add(items[i]);
        planned=null;
        var choices=new ArrayList<>(List.of(PREFERENCE));choices.sort(Comparator.comparingDouble((CandyKind k)->priority.applyAsDouble(new ItemStack(k.item()))).reversed());
        for(var kind:choices){
            if(!filter.allows(new ItemStack(kind.item())))continue;
            int wanted=goal.applyAsInt(new ItemStack(kind.item()));if(wanted<=0)continue;
            var base=baseFor(kind);
            if(kettle.base()!=null && kettle.base()!=base)continue;
            if(kettle.base()==null && count(available,s->baseOf(s)==base)==0)continue;
            if(count(available,s->s.getItem() instanceof CandyTrayItem && CandyTrayItem.batch(s)==null)==0)continue;
            int sugar=kettle.batch()?kettle.sugar():Math.min(CandyKettleBlockEntity.MAX_SUGAR,kettle.sugar()+count(available,s->s.is(Items.SUGAR)));
            if(wanted!=Integer.MAX_VALUE && !kettle.batch())sugar=Math.min(sugar,(wanted+CandyKettleBlockEntity.PIECES_PER_SUGAR-1)/CandyKettleBlockEntity.PIECES_PER_SUGAR);
            if(kind==CandyKind.LOLLIPOP)sugar=Math.min(sugar,count(available,s->s.is(Items.STICK))/CandyKettleBlockEntity.PIECES_PER_SUGAR);
            if(sugar<=0 || kettle.batch() && sugar<kettle.sugar())continue;
            planned=kind;sugarGoal=sugar;changed();break;
        }
    }
    public Identifier plan(){return planned==null?null:Identifier.fromNamespaceAndPath("jugcraft","candy_kettle/"+planned.getSerializedName());}
    private int slot(ItemStack s){
        if(s.getItem() instanceof CandyTrayItem && CandyTrayItem.batch(s)==null)return TRAY;
        if(planned==null)return -1;
        if(baseOf(s)==baseFor(planned))return BASE;
        if(s.is(Items.SUGAR))return SUGAR;
        if(s.is(Items.STICK) && planned==CandyKind.LOLLIPOP)return STICKS;
        if(ScarecrowBlock.dyeColor(s)!=null)return DYE;
        var flavour=CandyFlavour.of(s);if(flavour==null)return -1;
        if(kettle.flavours().contains(flavour))return -1;
        for(int i=FLAVOUR;i<DYE;i++)if(CandyFlavour.of(items[i])==flavour)return -1;
        int flavours=kettle.flavours().size();for(int i=FLAVOUR;i<DYE;i++)if(!items[i].isEmpty())flavours++;
        if(flavours>=CandyKettleBlockEntity.MAX_FLAVOURS)return -1;
        for(int i=FLAVOUR;i<DYE;i++)if(items[i].isEmpty())return i;
        return -1;
    }
    private int limit(int slot){return slot==SUGAR?Math.max(0,sugarGoal-kettle.sugar()):slot==STICKS?sugarGoal*CandyKettleBlockEntity.PIECES_PER_SUGAR:1;}
    public int needed(ItemStack stack){
        if(!live() || returnInputs || hasOutputs() || trayBatch()!=null || active!=null || planned==null)return 0;
        int slot=slot(stack);if(slot<0)return 0;
        if(kettle.batch() && slot!=TRAY && slot!=STICKS)return 0;
        if(kettle.base()!=null && slot==BASE)return 0;
        if(!kettle.batch() && !kettle.cool())return 0;
        return items[slot].isEmpty() || ItemStack.isSameItemSameComponents(items[slot],stack)?Math.max(0,limit(slot)-items[slot].getCount()):0;
    }
    private final Storage<ItemVariant> input=new InsertionOnlyStorage<>(){
        public long insert(ItemVariant item,long amount,TransactionContext tx){
            StoragePreconditions.notBlankNotNegative(item,amount);var stack=item.toStack(1);
            int count=(int)Math.min(amount,needed(stack));if(count<=0)return 0;
            int slot=slot(stack);updateSnapshots(tx);
            if(items[slot].isEmpty())items[slot]=item.toStack(count);else items[slot].grow(count);
            return count;
        }
    };
    private final Storage<ItemVariant> output=new CombinedStorage<>(java.util.stream.IntStream.range(0,SLOTS).mapToObj(Slot::new).toList());
    private final class Slot implements SingleSlotStorage<ItemVariant>{
        private final int slot;Slot(int slot){this.slot=slot;}
        private boolean exposed(){return live() && !tending() && (slot>=OUTPUT || returnInputs && slot!=TRAY);}
        public long insert(ItemVariant item,long amount,TransactionContext tx){return 0;}
        public boolean supportsInsertion(){return false;}
        public long extract(ItemVariant item,long amount,TransactionContext tx){
            StoragePreconditions.notBlankNotNegative(item,amount);
            if(!exposed() || !item.equals(getResource()))return 0;
            int count=(int)Math.min(amount,getAmount());if(count>0){updateSnapshots(tx);items[slot].shrink(count);}return count;
        }
        public ItemVariant getResource(){return exposed()?ItemVariant.of(items[slot]):ItemVariant.blank();}
        public boolean isResourceBlank(){return getResource().isBlank();}
        public long getAmount(){return exposed()?items[slot].getCount():0;}
        public long getCapacity(){return 64;}
    }
    public Storage<ItemVariant> inputs(){return input;}
    public Storage<ItemVariant> outputs(){return output;}
    private boolean ingredientsReady(){
        if(planned==null || items[TRAY].isEmpty())return false;
        int sugar=kettle.batch()?kettle.sugar():sugarGoal;
        if(planned==CandyKind.LOLLIPOP && items[STICKS].getCount()<sugar*CandyKettleBlockEntity.PIECES_PER_SUGAR)return false;
        return kettle.batch() || (kettle.base()!=null || baseOf(items[BASE])==baseFor(planned)) && items[SUGAR].getCount()+kettle.sugar()>=sugarGoal;
    }
    public CompanionStatus status(){
        if(!live())return CompanionStatus.MISSING;
        if(returnInputs && staged())return CompanionStatus.FULL;
        var batch=trayBatch();
        if(batch!=null)return batch.pullable(now()) || batch.ready(now())?CompanionStatus.READY:CompanionStatus.CANDY_SETTING;
        // Remainders can wait in Output while the same cook watches a live batch.
        if(kettle.batch()){
            if(active==null && !ingredientsReady())return CompanionStatus.NO_INPUT;
            if(kettle.makes()!=null && reached(active==null?planned:active))return CompanionStatus.READY;
            return CookingPotBlockEntity.isHeated(kettle.getLevel(),kettle.getBlockPos())?CompanionStatus.READY:CompanionStatus.NO_HEAT;
        }
        if(hasOutputs())return CompanionStatus.FULL;
        if(!kettle.cool())return CompanionStatus.COOLING;
        if(!ingredientsReady())return planned==null?CompanionStatus.RECIPE_MISSING:CompanionStatus.NO_INPUT;
        return CookingPotBlockEntity.isHeated(kettle.getLevel(),kettle.getBlockPos())?CompanionStatus.READY:CompanionStatus.NO_HEAT;
    }
    private boolean reached(CandyKind target){
        if(target==null)return false;
        for(var stage:CandyStage.values())if(kettle.base().makes(stage)==kettleKind(target))return kettle.cooked()>=stage.from;
        return kettle.makes()!=null;
    }
    /** Bounded output preflight, preserving every real remainder and candy component. */
    private boolean store(List<ItemStack> products,boolean apply){
        if(products.isEmpty())return true;
        var copy=Arrays.stream(items).map(ItemStack::copy).toArray(ItemStack[]::new);
        for(var product:products){var left=product.copy();
            for(int i=OUTPUT;i<SLOTS && !left.isEmpty();i++)if(copy[i].isEmpty() || ItemStack.isSameItemSameComponents(copy[i],left)){
                int count=Math.min(left.getCount(),left.getMaxStackSize()-copy[i].getCount());
                if(copy[i].isEmpty())copy[i]=left.copyWithCount(count);else copy[i].grow(count);left.shrink(count);
            }
            if(!left.isEmpty())return false;
        }
        if(apply)for(int i=OUTPUT;i<SLOTS;i++)items[i]=copy[i];return true;
    }
    private boolean[] usedFlavours(){
        boolean[] used=new boolean[2];var flavours=new ArrayList<>(kettle.flavours());
        for(int i=0;i<2;i++){var flavour=CandyFlavour.of(items[FLAVOUR+i]);
            if(flavour!=null && !flavours.contains(flavour) && flavours.size()<CandyKettleBlockEntity.MAX_FLAVOURS){used[i]=true;flavours.add(flavour);}}
        return used;
    }
    private List<ItemStack> remainders(){
        var result=new ArrayList<ItemStack>();
        if(kettle.base()==null && !items[BASE].isEmpty())result.add(new ItemStack(baseOf(items[BASE])==CandyBase.SYRUP?Items.GLASS_BOTTLE:Items.BUCKET));
        var used=usedFlavours();
        for(int i=FLAVOUR;i<=DYE;i++)if(!items[i].isEmpty() && (i==DYE || used[i-FLAVOUR])){
            var remainder=items[i].getItem().getCraftingRemainder();if(remainder!=null)result.add(remainder.create());
        }
        return result;
    }
    public CompanionStatus assist(PeepoEntity npc){
        if(!live() || !kettle.companionJob.isOccupant(npc) || !CompanionJobs.permitted(npc,kettle.getBlockPos()))return CompanionStatus.FORBIDDEN;
        var status=status();if(status!=CompanionStatus.READY)return status;
        if(lastWorked==now())return CompanionStatus.WORKING;
        var batch=trayBatch();
        List<ItemStack> products=List.of();
        if(batch!=null && batch.ready(now())){
            var kind=batch.sets(now());
            if(trayTarget==CandyKind.LOLLIPOP && kind==CandyKind.HARD_CANDY && items[STICKS].getCount()>=batch.pieces())kind=CandyKind.LOLLIPOP;
            products=List.of(Candies.make(kind,batch.flavours(),batch.colors(),batch.pieces()));
        }else if(batch==null && !kettle.batch())products=remainders();
        boolean mutated=false;
        try(var tx=Transaction.openOuter()){
            if(batch!=null && batch.ready(now()) && !batch.pullable(now()))products=npc.uniforms.chefResults(products,tx);
            if(!store(products,false))return CompanionStatus.FULL;
            if(npc.extractWorkEnergy(local.peepo.CompanionUniforms.Type.CHEF,4,tx)!=4)return CompanionStatus.RECOVERING;
            updateSnapshots(tx);
            lastWorked=now();
        if(batch!=null){
            if(batch.pullable(now())){
                if(++pullTicks>=npc.uniforms.duration(local.peepo.CompanionUniforms.Type.CHEF,CandyTrayItem.PULL_TICKS)){mutated=true;pullTicks=0;CandyTrayItem.fill(items[TRAY],batch.pull());sound(SoundEvents.HONEY_BLOCK_SLIDE);}
            }else if(batch.ready(now())){
                mutated=true;store(products,true);
                if(products.getFirst().is(CandyKind.LOLLIPOP.item()))items[STICKS].shrink(batch.pieces());
                items[TRAY]=new ItemStack(items[TRAY].getItem());trayTarget=null;planned=null;pullTicks=0;
                returnInputs=staged();sound(SoundEvents.HONEY_BLOCK_BREAK);
            }
        }else if(!kettle.batch()){
            mutated=true;store(products,true);active=planned;
            int sugarAdded=Math.max(0,sugarGoal-kettle.sugar());boolean addBase=kettle.base()==null;var used=usedFlavours();
            kettle.loadCompanionBatch(addBase?baseOf(items[BASE]):kettle.base(),sugarAdded,
                used[0]?items[FLAVOUR]:ItemStack.EMPTY,used[1]?items[FLAVOUR+1]:ItemStack.EMPTY,items[DYE]);
            if(addBase)items[BASE]=ItemStack.EMPTY;items[SUGAR].shrink(sugarAdded);
            for(int i=0;i<2;i++)if(used[i])items[FLAVOUR+i]=ItemStack.EMPTY;
            items[DYE]=ItemStack.EMPTY;
            sound(SoundEvents.BREWING_STAND_BREW);
        }else{
            if(active==null){active=planned;mutated=true;}
            if(reached(active) && kettle.makes()!=null){
                mutated=true;trayTarget=active;pullTicks=0;
                CandyTrayItem.fill(items[TRAY],new CandyBatch(kettle.makes(),kettle.flavours(),List.of(kettle.color()),kettle.sugar()*CandyKettleBlockEntity.PIECES_PER_SUGAR,now(),0));
                kettle.clear();sound(SoundEvents.HONEY_BLOCK_PLACE);
            }
        }
            tx.commit();
        }
        if(mutated)changed();return CompanionStatus.WORKING;
    }
    private void sound(net.minecraft.sounds.SoundEvent sound){kettle.getLevel().playSound(null,kettle.getBlockPos(),sound,SoundSource.BLOCKS,.7f,1f);}
    void kettleCleared(){active=null;}
    public void collect(Player player){
        for(int i=0;i<SLOTS;i++)if(!items[i].isEmpty())player.getInventory().add(items[i]);
        if(items[TRAY].isEmpty())trayTarget=null;
        if(!kettle.batch()){planned=null;returnInputs=staged();}changed();
    }
    void drop(BlockPos pos){
        if(kettle.getLevel() instanceof ServerLevel server)for(int i=0;i<SLOTS;i++)if(!items[i].isEmpty()){
            Block.popResource(server,pos,items[i]);items[i]=ItemStack.EMPTY;
        }
    }
    void save(ValueOutput out){
        out.store("CompanionCandyItems",ItemStack.OPTIONAL_CODEC.listOf(),Arrays.asList(items));filter.save(out);
        if(planned!=null)out.store("CompanionCandyPlan",CandyKind.CODEC,planned);
        if(active!=null)out.store("CompanionCandyActive",CandyKind.CODEC,active);
        if(trayTarget!=null)out.store("CompanionCandyTrayTarget",CandyKind.CODEC,trayTarget);
        out.putInt("CompanionCandySugar",sugarGoal);out.putBoolean("CompanionCandyReturn",returnInputs);
    }
    void load(ValueInput in){
        Arrays.fill(items,ItemStack.EMPTY);in.read("CompanionCandyItems",ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(list->{for(int i=0;i<Math.min(SLOTS,list.size());i++)items[i]=list.get(i);});
        filter.load(in);planned=in.read("CompanionCandyPlan",CandyKind.CODEC).orElse(null);
        active=in.read("CompanionCandyActive",CandyKind.CODEC).orElse(null);trayTarget=in.read("CompanionCandyTrayTarget",CandyKind.CODEC).orElse(null);
        sugarGoal=Math.clamp(in.getIntOr("CompanionCandySugar",4),1,4);returnInputs=in.getBooleanOr("CompanionCandyReturn",false);pullTicks=0;
    }
}
