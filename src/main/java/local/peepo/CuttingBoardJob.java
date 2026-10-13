package local.peepo;

import java.util.UUID;
import io.github.jimbozoomer.jugcraft.agriculture.CuttingBoardBlock;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.*;

/** A front-facing, two-handed cut. One real knife wear/recipe operation per forty work ticks. */
public final class CuttingBoardJob implements CompanionJob {
    private final BlockEntity board;
    private final KitchenCompanionPort kitchen;
    private int effort;
    private UUID worker;
    private Vec3 entrance,standing;
    private Direction facing;
    private boolean mounted;
    private long lease,nextSpace,nextClearance;
    private float checkedHeight;
    public CuttingBoardJob(KitchenCompanionPort kitchen,BlockEntity board){this.board=board;this.kitchen=kitchen;}
    public CuttingBoardJob prepare(PeepoEntity npc){
        expire();long now=npc.level().getGameTime();
        if(worker==null && (now>=nextSpace || checkedHeight!=npc.getBbHeight())){
            nextSpace=now+20;nextClearance=0;checkedHeight=npc.getBbHeight();entrance=standing=null;
            facing=board.getBlockState().getValue(CuttingBoardBlock.FACING);
            for(int dy=0;dy>=-1;dy--){
                var point=Vec3.atBottomCenterOf(stationPosition().relative(facing).offset(0,dy,0));
                if(npc.navigationMemory.failed(npc,stationPosition(),point) || !clearEntry(npc,point))continue;
                // Floor boards are worked beside their front. Counter-height boards are reached
                // from below, like the pot, with the companion standing on the front edge.
                double radius=dy==0?.80:.38;
                double y=dy==0?point.y:stationPosition().getY()+1.0/16+.001;
                var at=new Vec3(stationPosition().getX()+.5+facing.getStepX()*radius,y,
                    stationPosition().getZ()+.5+facing.getStepZ()*radius);
                if(!clearAt(npc,at))continue;
                entrance=point;standing=at;break;
            }
        }
        return this;
    }
    private boolean clearAt(PeepoEntity p,Vec3 at){
        var bounds=p.getBoundingBox().move(at.subtract(p.position()));
        for(int dx:new int[]{-1,1})for(int dz:new int[]{-1,1})
            if(!p.level().hasChunkAt(BlockPos.containing(at.x+dx*p.getBbWidth()/2,at.y,at.z+dz*p.getBbWidth()/2)))return false;
        return CompanionHazards.safeAt(p,at) && p.level().noCollision(bounds);
    }
    private boolean clearEntry(PeepoEntity p,Vec3 at){
        var floor=BlockPos.containing(at).below();
        return p.level().hasChunkAt(floor) && p.level().getBlockState(floor).isFaceSturdy(p.level(),floor,Direction.UP) && clearAt(p,at);
    }
    private PeepoEntity occupant(){return board.getLevel() instanceof ServerLevel l && worker!=null && l.getEntity(worker) instanceof PeepoEntity p?p:null;}
    private void expire(){if(worker!=null && (board.isRemoved() || occupant()==null || !occupant().isAlive() || board.getLevel().getGameTime()>lease))removed();}
    public void removed(){
        var p=occupant();if(p!=null){p.setWorkAnimation(WorkAnimation.NONE,stationPosition());if(mounted){p.setNoGravity(false);p.setDeltaMovement(Vec3.ZERO);p.leaveCompanionBed();}}
        worker=null;mounted=false;nextSpace=nextClearance=0;effort=0;
    }
    public Kind kind(){return Kind.WORK;}
    @Override public WorkAnimation animation(){return WorkAnimation.CHOP;}
    public void approachFailed(PeepoEntity npc){nextSpace=nextClearance=0;entrance=standing=null;}
    public BlockPos stationPosition(){return board.getBlockPos();}
    public Vec3 approachPosition(){return mounted?standing:entrance==null?Vec3.atBottomCenterOf(stationPosition()):entrance;}
    public boolean availableTo(PeepoEntity p){expire();return !board.isRemoved() && (worker==null || worker.equals(p.getUUID()));}
    public boolean isOccupant(PeepoEntity p){return worker!=null && worker.equals(p.getUUID()) && entrance!=null && p.position().distanceToSqr(approachPosition())<.64;}
    public CompanionStatus workStatus(PeepoEntity p){
        if(!availableTo(p))return CompanionStatus.OCCUPIED;
        if(!p.orders.tamed() || !p.assignments.assignedWork(stationPosition()) || !CompanionJobs.permitted(p,stationPosition()))return CompanionStatus.FORBIDDEN;
        if(entrance==null || standing==null || board.getBlockState().getValue(CuttingBoardBlock.FACING)!=facing)return CompanionStatus.BLOCKED;
        if(p.level().getGameTime()>=nextClearance){
            nextClearance=p.level().getGameTime()+20;
            if(!clearEntry(p,entrance) || !clearAt(p,standing)){entrance=null;nextSpace=0;return CompanionStatus.BLOCKED;}
        }
        return kitchen.kitchenStatus(p);
    }
    public boolean claim(PeepoEntity p){if(workStatus(p)!=CompanionStatus.READY)return false;worker=p.getUUID();lease=p.level().getGameTime()+100;return true;}
    public boolean occupy(PeepoEntity p){
        if(!isOccupant(p) || !availableTo(p))return false;lease=p.level().getGameTime()+100;
        if(!mounted){if(!clearAt(p,standing))return false;p.setBedExit(BlockPos.containing(entrance));}
        mounted=true;p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.resetFallDistance();p.getNavigation().stop();
        CompanionMotion.position(p,standing,facing.getOpposite().toYRot());return true;
    }
    public void release(PeepoEntity p){if(worker!=null && worker.equals(p.getUUID()))removed();}
    public CompanionStatus work(PeepoEntity p){
        if(!isOccupant(p) || !p.preferences.canWork() || !p.orders.station(this))return CompanionStatus.IDLE;
        var status=workStatus(p);if(status!=CompanionStatus.READY){effort=0;return status;}
        if(++effort<40)return CompanionStatus.WORKING;effort=0;
        return kitchen.cut(p)?CompanionStatus.WORKING:CompanionStatus.RECOVERING;
    }
}
