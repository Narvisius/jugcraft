package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.chemistry.PetroItems;
import io.github.jimbozoomer.jugcraft.farming.SprinklerBlockEntity;
import io.github.jimbozoomer.jugcraft.fluid.FluidNetworks;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.*;

/** Restocks native resources; no extra sprinkler ticks or growth multiplier. */
public final class SprinklerCompanion extends SnapshotParticipant<ItemStack> {
    public static final int FERTILIZER_STOCK=4,BUCKET_BUFFER=4;
    private static final long BUCKET=1000L*FluidNetworks.DROPLETS_PER_MB;
    private final SprinklerBlockEntity station;
    private ItemStack buckets=ItemStack.EMPTY;
    public SprinklerCompanion(SprinklerBlockEntity station){this.station=station;}
    protected ItemStack createSnapshot(){return buckets.copy();}
    protected void readSnapshot(ItemStack snapshot){buckets=snapshot.copy();}
    protected void onFinalCommit(){station.setChanged();CompanionReadiness.invalidate(station);}
    public void save(ValueOutput out){out.store("CompanionBuckets",ItemStack.OPTIONAL_CODEC,buckets);}
    public void load(ValueInput in){
        var saved=in.read("CompanionBuckets",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        buckets=saved.is(Items.BUCKET)?saved.copyWithCount(Math.min(BUCKET_BUFFER,saved.getCount())):ItemStack.EMPTY;
    }
    public ItemStack takeBuckets(){var result=buckets;buckets=ItemStack.EMPTY;if(!result.isEmpty())onFinalCommit();return result;}
    public void drop(){if(station.getLevel() instanceof ServerLevel level){var p=station.getBlockPos();Containers.dropItemStack(level,p.getX(),p.getY(),p.getZ(),takeBuckets());}}
    private boolean available(PeepoEntity npc){return station.getLevel()==npc.level() && npc.level() instanceof ServerLevel
        && !station.isRemoved() && CompanionJobs.permitted(npc,station.getBlockPos()) && npc.level().getBlockEntity(station.getBlockPos())==station;}
    private int needed(ItemStack stack){
        if(stack.is(PetroItems.FERTILIZER))return Math.max(0,FERTILIZER_STOCK-station.fertilizer());
        if(!stack.is(Items.WATER_BUCKET))return 0;
        var water=station.water();if(water.amount>0 && !water.variant.isOf(Fluids.WATER))return 0;
        return (int)Math.max(0,Math.min(BUCKET_BUFFER-buckets.getCount(),(SprinklerBlockEntity.TANK_MB*FluidNetworks.DROPLETS_PER_MB-water.amount)/BUCKET));
    }
    public CompanionLogistics.Port forCompanion(PeepoEntity npc){return new CompanionLogistics.Port(){
        public Identifier plan(){return PeepoMod.id("sprinkler");}
        public int needed(ItemStack stack){return available(npc)?SprinklerCompanion.this.needed(stack):0;}
        private final Storage<ItemVariant> inputs=new InsertionOnlyStorage<>(){
            public long insert(ItemVariant item,long amount,TransactionContext parent){
                StoragePreconditions.notBlankNotNegative(item,amount);
                int n=(int)Math.min(amount,needed(item.toStack(1)));if(n<=0)return 0;
                if(item.isOf(PetroItems.FERTILIZER))return station.fertilizerInput().insert(item,n,parent);
                // Water, returned bucket and delivered item participate in the same outer transaction.
                try(var tx=Transaction.openNested(parent)){
                    if(station.water().insert(FluidVariant.of(Fluids.WATER),BUCKET*n,tx)!=BUCKET*n)return 0;
                    updateSnapshots(tx);buckets=new ItemStack(Items.BUCKET,buckets.getCount()+n);tx.commit();return n;
                }
            }
        };
        private final Output outputs=new Output(npc);
        public Storage<ItemVariant> inputs(){return inputs;}
        public Storage<ItemVariant> outputs(){return outputs;}
        public CompanionStatus status(){
            if(!available(npc))return CompanionStatus.FORBIDDEN;
            if(buckets.getCount()>=BUCKET_BUFFER)return CompanionStatus.FULL;
            if(station.water().amount<SprinklerBlockEntity.WATER_PER_PULSE*FluidNetworks.DROPLETS_PER_MB)return CompanionStatus.NO_INPUT;
            return CompanionStatus.IDLE;
        }
    };}
    private final class Output implements Storage<ItemVariant>,StorageView<ItemVariant>{
        private final PeepoEntity npc;Output(PeepoEntity npc){this.npc=npc;}
        private ItemStack stack(){return available(npc)?buckets:ItemStack.EMPTY;}
        public boolean supportsInsertion(){return false;}
        public long insert(ItemVariant item,long amount,TransactionContext tx){return 0;}
        public long extract(ItemVariant item,long amount,TransactionContext tx){
            StoragePreconditions.notBlankNotNegative(item,amount);var held=stack();if(!item.matches(held))return 0;
            int n=(int)Math.min(amount,held.getCount());if(n>0){updateSnapshots(tx);buckets=held.copyWithCount(held.getCount()-n);}return n;
        }
        public boolean isResourceBlank(){return stack().isEmpty();}
        public ItemVariant getResource(){return ItemVariant.of(stack());}
        public long getAmount(){return stack().getCount();}
        public long getCapacity(){return BUCKET_BUFFER;}
        public Iterator<StorageView<ItemVariant>> iterator(){return Collections.<StorageView<ItemVariant>>singleton(this).iterator();}
    }
}
