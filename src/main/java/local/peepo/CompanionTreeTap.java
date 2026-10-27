package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.agriculture.TreeTapBlock;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
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
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.*;

/** Explicit tap jobs only; native latex timing, bounded approach searches and marked physical cargo. */
public final class CompanionTreeTap {
    public static final int WORK_TICKS=40,ENERGY_PER_TICK=4,INPUT_LIMIT=8;
    private final PeepoEntity npc;
    private final Map<BlockPos,Job> jobs=new HashMap<>();
    CompanionTreeTap(PeepoEntity npc){this.npc=npc;}
    private record Lease(UUID owner,long until){}
    private static final Map<Level,LinkedHashMap<BlockPos,Lease>> LEASES=new WeakHashMap<>();
    private static boolean source(Level level,BlockPos pos){return level.hasChunkAt(pos) && level.getBlockState(pos).getBlock() instanceof TreeTapBlock;}
    private static boolean input(ItemStack stack){return stack.is(Items.GLASS_BOTTLE);}
    private boolean enabled(){return JugcraftConfig.isFeatureEnabled(JugcraftAgriculture.FEATURE) && npc.level() instanceof ServerLevel server && server.getGameRules().get(GameRules.MOB_GRIEFING);}
    private boolean assigned(CompanionAssignments.Target target){int row=npc.assignments.workPriority(target.at().pos());return row<5 && target.equals(npc.assignments.get(row)) && target.present(npc.level());}
    public Job job(BlockPos pos){
        int row=npc.assignments.workPriority(pos);var target=row<5?npc.assignments.get(row):null;
        if(target==null || !target.treeTap() || !target.present(npc.level()))return null;
        jobs.entrySet().removeIf(e->{int i=npc.assignments.workPriority(e.getKey());boolean stale=i>=5 || !e.getValue().target.equals(npc.assignments.get(i));if(stale)e.getValue().release(npc);return stale;});
        return jobs.computeIfAbsent(pos.immutable(),p->new Job(target)).prepare();
    }
    private int carriedInputs(){int n=0;for(int i=0;i<8;i++)if(!npc.transport.reserved(i) && input(npc.belongings.getItem(i)))n+=npc.belongings.getItem(i).getCount();return n;}
    CompanionLogistics.Port port(CompanionAssignments.Target target){return new CompanionLogistics.Port(){
        public Identifier plan(){return PeepoMod.id("tree_tap_collection");}
        public int needed(ItemStack stack){return enabled() && npc.orders.tamed() && assigned(target) && CompanionJobs.permitted(npc,target.at().pos()) && source(npc.level(),target.at().pos()) && input(stack)?Math.max(0,INPUT_LIMIT-carriedInputs()):0;}
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
        public WorkAnimation animation(){return WorkAnimation.INTERACT;}
        public BlockPos stationPosition(){return target.at().pos();}
        public Vec3 approachPosition(){return entrance==null?Vec3.atBottomCenterOf(stationPosition().above()):entrance;}
        // Reach the selected ground position before working beside a trunk.
        public double arrivalDistanceSquared(){return .04;}
        public boolean timeCritical(){return progress>0;}
        private Job prepare(){
            long now=npc.level().getGameTime();if(claimed || now<nextApproach)return this;
            nextApproach=now+20;
            if(!CompanionBudget.search(npc))return this;
            entrance=null;
            // Nine front/side positions, including two blocks below a high tap. Never stand inside its trunk.
            var front=npc.level().getBlockState(stationPosition()).getValue(TreeTapBlock.FACING);
            for(var side:new Direction[]{front,front.getClockWise(),front.getCounterClockWise()})for(int dy=0;dy>=-2;dy--){
                var point=Vec3.atBottomCenterOf(stationPosition().relative(side).offset(0,dy,0));
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
            var eye=point.add(0,npc.getEyeHeight(),0);var tap=Vec3.atCenterOf(stationPosition()).add(0,-.2,0);
            if(eye.distanceToSqr(tap)>6.25)return false;
            var hit=level.clip(new ClipContext(eye,tap,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,npc));
            return hit.getType()==HitResult.Type.MISS || hit.getBlockPos().equals(stationPosition());
        }
        private int slot(){for(int i=0;i<8;i++)if(!npc.transport.reserved(i) && input(npc.belongings.getItem(i)))return i;return -1;}
        private int available(){return source(npc.level(),stationPosition())?npc.level().getBlockState(stationPosition()).getValue(TreeTapBlock.LATEX)/TreeTapBlock.STEPS_PER_BOTTLE:0;}
        private boolean convert(int count,TransactionContext tx){
            int remaining=count;
            for(int i=0;i<8 && remaining>0;i++){
                var held=npc.belongings.getItem(i);
                if(npc.transport.reserved(i) || !input(held))continue;
                int n=Math.min(remaining,held.getCount());
                if(npc.food.takeCargo(i,held,n,tx)!=n)return false;
                remaining-=n;
            }
            return remaining==0 && npc.garden.storeHarvest(new ItemStack(JugcraftAgriculture.item("latex_bottle"),count),target.at(),tx);
        }
        private int fittingCount(){
            for(int n=Math.min(available(),carriedInputs());n>0;n--)try(var tx=Transaction.openOuter()){if(convert(n,tx))return n;}
            return 0;
        }
        private Lease lease(){var leases=LEASES.get(npc.level());return leases==null?null:leases.get(stationPosition());}
        public boolean availableTo(PeepoEntity other){var lease=lease();return other==npc && assigned(target) && (lease==null || lease.until<=npc.level().getGameTime() || lease.owner.equals(npc.getUUID()));}
        public CompanionStatus workStatus(PeepoEntity other){
            if(other!=npc || !assigned(target))return CompanionStatus.MISSING;
            if(!availableTo(other))return CompanionStatus.OCCUPIED;
            if(!npc.orders.tamed() || !CompanionJobs.permitted(npc,stationPosition()))return CompanionStatus.FORBIDDEN;
            if(!JugcraftConfig.isFeatureEnabled(JugcraftAgriculture.FEATURE))return CompanionStatus.TAPS_DISABLED;
            if(!enabled())return CompanionStatus.GARDEN_DISABLED;
            if(!source(npc.level(),stationPosition()))return CompanionStatus.MISSING;
            if(available()==0)return CompanionStatus.LATEX_WAIT;
            if(slot()<0)return CompanionStatus.NEEDS_BOTTLES;
            if(entrance==null)return CompanionStatus.BLOCKED;
            if(npc.level().getGameTime()>=nextRoom){room=fittingCount()>0;nextRoom=npc.level().getGameTime()+20;}
            return room?CompanionStatus.READY:CompanionStatus.FULL;
        }
        public boolean claim(PeepoEntity other){
            if(workStatus(other)!=CompanionStatus.READY)return false;
            var leases=LEASES.computeIfAbsent(npc.level(),l->new LinkedHashMap<>());long now=npc.level().getGameTime();
            if(leases.size()>=4096){
                var retained=new LinkedHashMap<BlockPos,Lease>();var it=leases.entrySet().iterator();int reads=0;
                while(it.hasNext() && reads++<64){var e=it.next();if(e.getValue().until>now)retained.put(e.getKey(),e.getValue());it.remove();}
                leases.putAll(retained);
                if(!leases.containsKey(stationPosition()) && leases.size()>=4096)return false;
            }
            leases.put(stationPosition(),new Lease(npc.getUUID(),now+100));claimed=true;return true;
        }
        public boolean occupy(PeepoEntity other){
            if(other!=npc || !claimed || !availableTo(other) || entrance==null || npc.position().distanceToSqr(entrance)>.64 || !safe(npc.position()))return false;
            LEASES.get(npc.level()).put(stationPosition(),new Lease(npc.getUUID(),npc.level().getGameTime()+100));return true;
        }
        public void release(PeepoEntity other){if(other==npc){var lease=lease();if(lease!=null && lease.owner.equals(npc.getUUID()))LEASES.get(npc.level()).remove(stationPosition());claimed=false;progress=0;nextRoom=0;}}
        public void approachFailed(PeepoEntity other){release(other);entrance=null;nextApproach=0;}
        public CompanionStatus work(PeepoEntity other){
            if(!npc.preferences.canWork() || !npc.orders.station(this) || !occupy(other))return CompanionStatus.IDLE;
            var status=workStatus(other);if(status!=CompanionStatus.READY)return status;
            int actionTicks=npc.uniforms.duration(CompanionUniforms.Type.ORCHARD_KEEPER,WORK_TICKS);
            boolean finishing=progress+1>=actionTicks;
            int count=finishing?fittingCount():0;if(finishing && count==0)return CompanionStatus.FULL;
            try(var tx=Transaction.openOuter()){
                if(npc.extractWorkEnergy(CompanionUniforms.Type.ORCHARD_KEEPER,ENERGY_PER_TICK,tx)!=ENERGY_PER_TICK)return CompanionStatus.RECOVERING;
                if(finishing){
                    if(!convert(count,tx))return CompanionStatus.FULL;
                    var state=npc.level().getBlockState(stationPosition());
                    if(!(state.getBlock() instanceof TreeTapBlock tap) || !tap.collectBottles((ServerLevel)npc.level(),stationPosition(),count,tx))return CompanionStatus.LATEX_WAIT;
                }
                tx.commit();
            }
            float yaw=(float)Math.toDegrees(Math.atan2(-(stationPosition().getX()+.5-npc.getX()),stationPosition().getZ()+.5-npc.getZ()));
            npc.setYRot(yaw);npc.yBodyRot=yaw;npc.setYHeadRot(yaw);
            if(++progress<actionTicks)return CompanionStatus.WORKING;
            progress=0;nextRoom=0;
            npc.level().playSound(null,stationPosition(),SoundEvents.BOTTLE_FILL,SoundSource.BLOCKS,.8F,1);
            npc.level().gameEvent(npc,GameEvent.BLOCK_CHANGE,stationPosition());
            npc.transport.workChanged();npc.readiness.clear();return CompanionStatus.IDLE;
        }
    }
}
