package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.agriculture.GourdCanteenItem;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.*;
import net.minecraft.core.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.*;

/** Four explicit source jobs at most. Filled containers use existing cargo/provenance and porter routes. */
public final class CompanionWater {
    public static final Identifier WATER=Identifier.withDefaultNamespace("water");
    public static final int WORK_TICKS=40,ENERGY_PER_TICK=4,INPUT_LIMIT=4;
    private final PeepoEntity npc;
    private final Map<BlockPos,Job> jobs=new HashMap<>();
    CompanionWater(PeepoEntity npc){this.npc=npc;}
    public static boolean source(Level level,BlockPos pos){return level.hasChunkAt(pos) && level.getBlockState(pos).is(Blocks.WATER) && level.getFluidState(pos).isSource();}
    private static boolean input(ItemStack stack){return stack.is(Items.GLASS_BOTTLE) || stack.is(Items.BUCKET)
        || stack.getItem() instanceof GourdCanteenItem && GourdCanteenItem.water(stack)<GourdCanteenItem.CAPACITY;}
    private static ItemStack filled(ItemStack stack){
        if(stack.is(Items.BUCKET))return new ItemStack(Items.WATER_BUCKET);
        if(stack.is(Items.GLASS_BOTTLE))return PotionContents.createItemStack(Items.POTION,Potions.WATER);
        if(stack.getItem() instanceof GourdCanteenItem){var result=stack.copyWithCount(1);result.set(JugcraftAgriculture.CANTEEN_WATER,GourdCanteenItem.CAPACITY);return result;}
        return ItemStack.EMPTY;
    }
    private boolean enabled(){return npc.level() instanceof ServerLevel server && server.getGameRules().get(GameRules.MOB_GRIEFING);}
    private boolean assigned(CompanionAssignments.Target target){int row=npc.assignments.workPriority(target.at().pos());return row<5 && target.equals(npc.assignments.get(row)) && target.present(npc.level());}
    public Job job(BlockPos pos){
        int row=npc.assignments.workPriority(pos);var target=row<5?npc.assignments.get(row):null;
        if(target==null || !target.water() || !target.present(npc.level()))return null;
        jobs.entrySet().removeIf(e->{int i=npc.assignments.workPriority(e.getKey());return i>=5 || !e.getValue().target.equals(npc.assignments.get(i));});
        return jobs.computeIfAbsent(pos.immutable(),p->new Job(target)).prepare();
    }
    private int carriedInputs(){int n=0;for(int i=0;i<8;i++)if(!npc.transport.reserved(i) && input(npc.belongings.getItem(i)))n+=npc.belongings.getItem(i).getCount();return n;}
    CompanionLogistics.Port port(CompanionAssignments.Target target){return new CompanionLogistics.Port(){
        public Identifier plan(){return PeepoMod.id("water_collection");}
        public int needed(ItemStack stack){return enabled() && assigned(target) && source(npc.level(),target.at().pos()) && input(stack)?Math.max(0,INPUT_LIMIT-carriedInputs()):0;}
        private final Storage<ItemVariant> inputs=new InsertionOnlyStorage<>(){
            public long insert(ItemVariant item,long amount,TransactionContext tx){
                StoragePreconditions.notBlankNotNegative(item,amount);var stack=item.toStack(1);int n=(int)Math.min(amount,needed(stack));
                return n<=0?0:npc.food.store(stack.copyWithCount(n),tx);
            }
        };
        public Storage<ItemVariant> inputs(){return inputs;}
        public Storage<ItemVariant> outputs(){return Storage.empty();} // Filled containers are already marked cargo.
    };}
    public final class Job implements CompanionJob {
        private final CompanionAssignments.Target target;
        private Vec3 entrance;
        private long nextApproach,nextRoom;
        private int progress;
        private boolean room,claimed;
        Job(CompanionAssignments.Target target){this.target=target;}
        public Kind kind(){return Kind.WORK;}
        public WorkAnimation animation(){return WorkAnimation.WATER_FILL;}
        public BlockPos stationPosition(){return target.at().pos();}
        public Vec3 approachPosition(){return entrance==null?Vec3.atBottomCenterOf(stationPosition().above()):entrance;}
        // Reach the dry bank before stopping; ordinary workstation reach can stop a short NPC in the water.
        public double arrivalDistanceSquared(){return .04;}
        public boolean timeCritical(){return progress>0;}
        private Job prepare(){
            long now=npc.level().getGameTime();if(claimed || now<nextApproach)return this;
            nextApproach=now+20;
            if(!CompanionBudget.search(npc))return this;
            entrance=null;
            // Only 24 dry-bank candidates. No nearby-water search or forced chunks.
            for(var side:Direction.Plane.HORIZONTAL)for(int distance=1;distance<=2;distance++)for(int dy=0;dy<=2;dy++){
                var point=Vec3.atBottomCenterOf(stationPosition().relative(side,distance).above(dy));
                if(npc.navigationMemory.failed(npc,stationPosition(),point) || !safe(point))continue;
                if(entrance==null || point.distanceToSqr(npc.position())<entrance.distanceToSqr(npc.position()))entrance=point;
            }
            return this;
        }
        private boolean safe(Vec3 point){
            var feet=BlockPos.containing(point);var floor=feet.below();var level=npc.level();
            if(!CompanionJobs.permitted(npc,floor) || !level.hasChunkAt(feet.above()) || !level.hasChunkAt(stationPosition())
                || !level.getFluidState(feet).isEmpty() || !level.getBlockState(floor).isFaceSturdy(level,floor,Direction.UP)
                || !CompanionHazards.safeAt(npc,point))return false;
            double r=npc.getBbWidth()/2+.02;
            if(!level.noCollision(new AABB(point.x-r,point.y,point.z-r,point.x+r,point.y+npc.getBbHeight(),point.z+r)))return false;
            var eye=point.add(0,npc.getEyeHeight(),0);var water=Vec3.atCenterOf(stationPosition()).add(0,.35,0);
            if(eye.distanceToSqr(water)>6.25)return false;
            var hit=level.clip(new ClipContext(eye,water,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,npc));
            return hit.getType()==HitResult.Type.MISS || hit.getBlockPos().equals(stationPosition());
        }
        private int slot(){for(int i=0;i<8;i++)if(!npc.transport.reserved(i) && input(npc.belongings.getItem(i)))return i;return -1;}
        private boolean convert(int slot,TransactionContext tx){
            var held=npc.belongings.getItem(slot);if(npc.transport.reserved(slot) || !input(held))return false;
            var result=filled(held);return npc.food.takeCargo(slot,held,1,tx)==1 && npc.garden.storeHarvest(result,target.at(),tx);
        }
        private int fittingSlot(){
            for(int i=0;i<8;i++)if(!npc.transport.reserved(i) && input(npc.belongings.getItem(i)))try(var tx=Transaction.openOuter()){if(convert(i,tx))return i;}
            return -1;
        }
        public boolean availableTo(PeepoEntity other){return other==npc && assigned(target);}
        public CompanionStatus workStatus(PeepoEntity other){
            if(!availableTo(other))return CompanionStatus.MISSING;
            if(!CompanionJobs.permitted(npc,stationPosition()))return CompanionStatus.FORBIDDEN;
            if(!enabled())return CompanionStatus.GARDEN_DISABLED;
            if(!source(npc.level(),stationPosition()))return CompanionStatus.NO_WATER_SOURCE;
            if(slot()<0)return CompanionStatus.NEEDS_CONTAINERS;
            if(entrance==null)return CompanionStatus.BLOCKED;
            if(npc.level().getGameTime()>=nextRoom){room=fittingSlot()>=0;nextRoom=npc.level().getGameTime()+20;}
            return room?CompanionStatus.READY:CompanionStatus.FULL;
        }
        public boolean claim(PeepoEntity other){if(workStatus(other)!=CompanionStatus.READY)return false;claimed=true;return true;}
        public boolean occupy(PeepoEntity other){return other==npc && claimed && assigned(target) && entrance!=null && npc.position().distanceToSqr(entrance)<=.64 && safe(npc.position());}
        public void release(PeepoEntity other){if(other==npc){claimed=false;progress=0;nextRoom=0;}}
        public void approachFailed(PeepoEntity other){release(other);entrance=null;nextApproach=0;}
        public CompanionStatus work(PeepoEntity other){
            if(!npc.preferences.canWork() || !npc.orders.station(this) || !occupy(other))return CompanionStatus.IDLE;
            var status=workStatus(other);if(status!=CompanionStatus.READY)return status;
            int displaySlot=slot();if(displaySlot>=0)npc.setWorkPie(npc.belongings.getItem(displaySlot));
            boolean finishing=progress+1>=WORK_TICKS;boolean bucket=false;
            int selected=finishing?fittingSlot():-1;if(finishing && selected<0)return CompanionStatus.FULL;
            try(var tx=Transaction.openOuter()){
                if(npc.extractEnergy(ENERGY_PER_TICK,tx)!=ENERGY_PER_TICK)return CompanionStatus.RECOVERING;
                if(finishing){
                    int slot=selected;
                    bucket=npc.belongings.getItem(slot).is(Items.BUCKET);
                    if(!convert(slot,tx))return CompanionStatus.FULL;
                    // Plain source water only. Native bucket pickup removes the source; normal fluid ticks may renew it.
                    if(bucket){var state=npc.level().getBlockState(stationPosition());
                        if(!source(npc.level(),stationPosition()) || !((BucketPickup)state.getBlock()).pickupBlock(npc,npc.level(),stationPosition(),state).is(Items.WATER_BUCKET))return CompanionStatus.NO_WATER_SOURCE;}
                }
                tx.commit();
            }
            float yaw=(float)Math.toDegrees(Math.atan2(-(stationPosition().getX()+.5-npc.getX()),stationPosition().getZ()+.5-npc.getZ()));
            npc.setYRot(yaw);npc.yBodyRot=yaw;npc.setYHeadRot(yaw);
            if(++progress<WORK_TICKS)return CompanionStatus.WORKING;
            progress=0;nextRoom=0;
            npc.level().playSound(null,stationPosition(),bucket?SoundEvents.BUCKET_FILL:SoundEvents.BOTTLE_FILL,SoundSource.BLOCKS,.8F,1);
            npc.level().gameEvent(npc,GameEvent.FLUID_PICKUP,stationPosition());
            npc.transport.workChanged();npc.readiness.clear();return CompanionStatus.IDLE;
        }
    }
}
