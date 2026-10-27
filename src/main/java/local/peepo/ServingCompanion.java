package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.*;

/** Physical restocking and bottling. Transactions include visible stock and punch level. */
public final class ServingCompanion extends SnapshotParticipant<ServingCompanion.Snapshot> {
    private final BlockEntity station;
    private int pendingServings=-1;
    private final ItemStack[] dishes={ItemStack.EMPTY,ItemStack.EMPTY};
    public final CompanionRecipeFilter filter=new CompanionRecipeFilter();
    public ServingCompanion(BlockEntity station){this.station=station;}
    public static ServingCompanion of(BlockEntity block){
        if(block instanceof FeastTableBlockEntity table)return table.companion;
        if(block instanceof CandyBowlBlockEntity bowl)return bowl.companion;
        if(block instanceof PunchBowlBlockEntity bowl)return bowl.companion;
        return null;
    }
    public boolean hasOutput(){return station instanceof PunchBowlBlockEntity;}
    public boolean allowed(PeepoEntity npc){return !station.isRemoved() && station.getLevel() instanceof ServerLevel
        && (!(station instanceof CandyBowlBlockEntity bowl) || npc.orders.foodAccess(bowl.owner(),false));}
    public boolean acceptsFilter(ItemStack stack){return stack.isEmpty() || station instanceof PunchBowlBlockEntity && stack.is(JugcraftAgriculture.item("witchs_brew_punch")) || (station instanceof FeastTableBlockEntity?FeastTableBlockEntity.food(stack):station instanceof CandyBowlBlockEntity && stack.is(CandyBagItem.TREATS));}
    public void observe(){if(station instanceof FeastTableBlockEntity table)for(int i=0;i<2;i++){
        var dish=table.dish(i);if(!dish.isEmpty())dishes[i]=dish.copyWithCount(1);
    }}
    public void save(ValueOutput out){filter.save(out);out.store("CompanionDishes",ItemStack.OPTIONAL_CODEC.listOf(),Arrays.asList(dishes));}
    public void load(ValueInput in){filter.load(in);Arrays.fill(dishes,ItemStack.EMPTY);
        in.read("CompanionDishes",ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(list->{for(int i=0;i<Math.min(2,list.size());i++)dishes[i]=list.get(i).copyWithCount(1);});observe();}
    record Snapshot(List<ItemStack> stock,List<ItemStack> templates,int servings){}
    protected Snapshot createSnapshot(){
        List<ItemStack> stock=station instanceof FeastTableBlockEntity table?List.of(table.dish(0).copy(),table.dish(1).copy())
            :station instanceof CandyBowlBlockEntity bowl?bowl.companionStock():List.of(((PunchBowlBlockEntity)station).bottled.copy());
        return new Snapshot(stock,Arrays.stream(dishes).map(ItemStack::copy).toList(),pendingServings);
    }
    protected void readSnapshot(Snapshot snapshot){
        if(station instanceof FeastTableBlockEntity table)table.restoreCompanionStock(snapshot.stock());
        else if(station instanceof CandyBowlBlockEntity bowl)bowl.restoreCompanionStock(snapshot.stock());
        else {((PunchBowlBlockEntity)station).bottled=snapshot.stock().getFirst().copy();pendingServings=snapshot.servings();}
        for(int i=0;i<2;i++)dishes[i]=snapshot.templates().get(i).copy();
    }
    protected void onFinalCommit(){
        if(pendingServings>=0 && station.getLevel()!=null){
            int before=station.getBlockState().getValue(PunchBowlBlock.SERVINGS_LEFT),after=pendingServings;pendingServings=-1;
            station.getLevel().setBlock(station.getBlockPos(),station.getBlockState().setValue(PunchBowlBlock.SERVINGS_LEFT,after),Block.UPDATE_ALL);
            station.getLevel().playSound(null,station.getBlockPos(),after<before?net.minecraft.sounds.SoundEvents.BOTTLE_FILL:net.minecraft.sounds.SoundEvents.BREWING_STAND_BREW,net.minecraft.sounds.SoundSource.BLOCKS,.7F,1);
        }
        if(station instanceof FeastTableBlockEntity table)table.companionChanged();
        else if(station instanceof CandyBowlBlockEntity bowl)bowl.companionChanged();
        station.setChanged();CompanionReadiness.invalidate(station);
    }
    public int servings(){return hasOutput()?pendingServings>=0?pendingServings:station.getBlockState().getValue(PunchBowlBlock.SERVINGS_LEFT):0;}
    private void servings(int count){pendingServings=count;}
    private int room(int slot,ItemStack candidate){
        var table=(FeastTableBlockEntity)station;var held=table.dish(slot);var template=dishes[slot];
        return !template.isEmpty() && filter.allows(template) && ItemStack.isSameItemSameComponents(template,candidate)
            && (held.isEmpty() || ItemStack.isSameItemSameComponents(held,candidate))?Math.max(0,FeastTableBlockEntity.SERVINGS-held.getCount()):0;
    }
    private int needed(ItemStack stack){
        if(stack.isEmpty())return 0;
        if(station instanceof FeastTableBlockEntity)return room(0,stack)+room(1,stack);
        if(station instanceof CandyBowlBlockEntity bowl)return stack.is(CandyBagItem.TREATS) && filter.allows(stack)?Math.max(0,CandyBowlBlockEntity.CAPACITY-bowl.count()):0;
        var bowl=(PunchBowlBlockEntity)station;
        int space=Math.max(0,Math.min(12,new ItemStack(JugcraftAgriculture.item("witchs_brew_punch")).getMaxStackSize())-bowl.bottled.getCount());
        if(space==0)return 0;
        if(stack.is(Items.GLASS_BOTTLE))return Math.min(space,servings());
        // Never spend a berry for a truncated single serving.
        return filter.allows(new ItemStack(JugcraftAgriculture.item("witchs_brew_punch"))) && stack.is(PunchBowlBlock.INGREDIENTS)?Math.max(0,(Math.min(PunchBowlBlock.SERVINGS,space)-servings())/PunchBowlBlock.PER_BERRY):0;
    }
    public CompanionLogistics.Port forCompanion(PeepoEntity npc){return new CompanionLogistics.Port(){
        public void prepare(Storage<ItemVariant> source){if(allowed(npc))observe();}
        public Identifier plan(){return allowed(npc)?PeepoMod.id("serving_station"):null;}
        public int needed(ItemStack stack){return allowed(npc)?ServingCompanion.this.needed(stack):0;}
        private final Storage<ItemVariant> input=new InsertionOnlyStorage<>(){
            public long insert(ItemVariant item,long amount,TransactionContext tx){
                StoragePreconditions.notBlankNotNegative(item,amount);if(!allowed(npc))return 0;
                var stack=item.toStack(1);int n=(int)Math.min(amount,needed(stack));if(n<=0)return 0;updateSnapshots(tx);
                if(station instanceof FeastTableBlockEntity table){int moved=0;for(int i=0;i<2;i++){int add=Math.min(n-moved,room(i,stack));if(add>0)moved+=table.serve(i,stack.copyWithCount(add),false);}return moved;}
                if(station instanceof CandyBowlBlockEntity bowl)return bowl.add(stack.copyWithCount(n),false);
                var bowl=(PunchBowlBlockEntity)station;
                if(stack.is(Items.GLASS_BOTTLE)){bowl.bottled=new ItemStack(JugcraftAgriculture.item("witchs_brew_punch"),bowl.bottled.getCount()+n);servings(servings()-n);}
                else servings(servings()+n*PunchBowlBlock.PER_BERRY);
                return n;
            }
        };
        private final Output output=new Output(npc);
        public Storage<ItemVariant> inputs(){return input;}
        public Storage<ItemVariant> outputs(){return hasOutput()?output:Storage.empty();}
        public CompanionStatus status(){
            if(!allowed(npc))return CompanionStatus.FORBIDDEN;
            if(station instanceof FeastTableBlockEntity){observe();boolean configured=false;for(int i=0;i<2;i++)if(!dishes[i].isEmpty() && filter.allows(dishes[i])){configured=true;if(room(i,dishes[i])>0)return CompanionStatus.NO_INPUT;}return configured?CompanionStatus.FULL:CompanionStatus.SERVING_SETUP;}
            if(station instanceof CandyBowlBlockEntity bowl)return bowl.count()>=CandyBowlBlockEntity.CAPACITY?CompanionStatus.FULL:CompanionStatus.NO_INPUT;
            return !((PunchBowlBlockEntity)station).bottled.isEmpty()?CompanionStatus.FULL:servings()>0?CompanionStatus.NEEDS_BOTTLES:CompanionStatus.NO_INPUT;
        }
    };}
    private final class Output implements Storage<ItemVariant>,StorageView<ItemVariant>{
        private final PeepoEntity npc;Output(PeepoEntity npc){this.npc=npc;}
        private ItemStack stack(){return allowed(npc) && station instanceof PunchBowlBlockEntity bowl?bowl.bottled:ItemStack.EMPTY;}
        public boolean supportsInsertion(){return false;}
        public long insert(ItemVariant item,long amount,TransactionContext tx){return 0;}
        public long extract(ItemVariant item,long amount,TransactionContext tx){
            StoragePreconditions.notBlankNotNegative(item,amount);var stack=stack();if(!item.matches(stack))return 0;
            int n=(int)Math.min(amount,stack.getCount());if(n>0){updateSnapshots(tx);((PunchBowlBlockEntity)station).bottled=stack.copyWithCount(stack.getCount()-n);}return n;
        }
        public boolean isResourceBlank(){return stack().isEmpty();}
        public ItemVariant getResource(){return ItemVariant.of(stack());}
        public long getAmount(){return stack().getCount();}
        public long getCapacity(){return 12;}
        public Iterator<StorageView<ItemVariant>> iterator(){return Collections.<StorageView<ItemVariant>>singleton(this).iterator();}
    }
}
