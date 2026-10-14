package local.peepo;

import java.util.UUID;
import io.github.jimbozoomer.jugcraft.agriculture.SpinningWheelBlockEntity;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.*;

/** One helper feeds yarn with both hands and pumps the treadle from the wheel front. */
public final class SpinningWheelJob implements CompanionJob {
    private final SpinningWheelBlockEntity wheel;
    private UUID worker;
    private Vec3 entrance,standing;
    private boolean operating;
    private long lease,nextSpace;
    private float checkedHeight;
    private long nextClearance;
    public SpinningWheelJob(SpinningWheelBlockEntity wheel){this.wheel=wheel;}
    public SpinningWheelJob prepare(PeepoEntity npc){
        expire();long now=npc.level().getGameTime();
        if(worker==null && (now>=nextSpace || checkedHeight!=npc.getBbHeight())){
            nextSpace=now+20;checkedHeight=npc.getBbHeight();entrance=standing=null;
            var front=wheel.getBlockState().getValue(io.github.jimbozoomer.jugcraft.agriculture.SpinningWheelBlock.FACING);
            for(double across:new double[]{0.0,.2,-.2}){
                var side=front.getClockWise();
                var p=Vec3.atBottomCenterOf(stationPosition()).add(front.getStepX()*1.25+side.getStepX()*across,0,front.getStepZ()*1.25+side.getStepZ()*across);
                if(npc.navigationMemory.failed(npc,stationPosition(),p))continue;
                var floor=BlockPos.containing(p).below();
                if(!npc.level().hasChunkAt(floor) || !npc.level().getBlockState(floor).isFaceSturdy(npc.level(),floor,Direction.UP))continue;
                if(!CompanionHazards.safeAt(npc,p) || !npc.level().noCollision(new AABB(p.x-.24,p.y,p.z-.24,p.x+.24,p.y+npc.getBbHeight(),p.z+.24)))continue;
                var work=p.add(-front.getStepX()*.53,0,-front.getStepZ()*.53);
                if(!clearAt(npc,work) || !clearPassage(npc,p,work))continue;
                if(entrance==null || npc.position().distanceToSqr(p)<npc.position().distanceToSqr(entrance)){entrance=p;standing=work;}
            }
        }
        return this;
    }
    private boolean clearAt(PeepoEntity npc,Vec3 point){
        if(point==null)return false;
        var floor=BlockPos.containing(point).below();
        return npc.level().hasChunkAt(floor) && npc.level().getBlockState(floor).isFaceSturdy(npc.level(),floor,Direction.UP)
            && CompanionHazards.safeAt(npc,point) && npc.level().noCollision(new AABB(point.x-.24,point.y,point.z-.24,point.x+.24,point.y+npc.getBbHeight(),point.z+.24));
    }
    private boolean clearPassage(PeepoEntity npc,Vec3 from,Vec3 to){
        // A conservative swept box prevents the last alignment from cutting through a corner.
        return npc.level().noCollision(new AABB(Math.min(from.x,to.x)-.24,Math.min(from.y,to.y),Math.min(from.z,to.z)-.24,
            Math.max(from.x,to.x)+.24,Math.max(from.y,to.y)+npc.getBbHeight(),Math.max(from.z,to.z)+.24));
    }
    private PeepoEntity occupant(){return wheel.getLevel() instanceof ServerLevel l && worker!=null && l.getEntity(worker) instanceof PeepoEntity p?p:null;}
    private void expire(){if(worker!=null && (wheel.isRemoved() || occupant()==null || !occupant().isAlive() || wheel.getLevel().getGameTime()>lease))removed();}
    public void removed(){var p=occupant();if(p!=null)p.setWorkAnimation(WorkAnimation.NONE,stationPosition());worker=null;operating=false;nextSpace=0;wheel.resetCompanionStroke();}
    public Kind kind(){return Kind.WORK;}
    public WorkAnimation animation(){return WorkAnimation.SPINNING;}
    public void approachFailed(PeepoEntity npc){nextSpace=nextClearance=0;entrance=standing=null;operating=false;}
    public BlockPos stationPosition(){return wheel.getBlockPos();}
    public Vec3 approachPosition(){return operating && standing!=null?standing:entrance==null?Vec3.atBottomCenterOf(stationPosition()):entrance;}
    public boolean availableTo(PeepoEntity p){expire();return !wheel.isRemoved() && (worker==null || worker.equals(p.getUUID()));}
    public boolean isOccupant(PeepoEntity p){return worker!=null && worker.equals(p.getUUID()) && entrance!=null && p.position().distanceToSqr(approachPosition())<.64;}
    public CompanionStatus workStatus(PeepoEntity p){return status(p,false);}
    public CompanionStatus planningFacts(PeepoEntity p){return status(p,true);}
    private CompanionStatus status(PeepoEntity p,boolean planning){
        if(!availableTo(p))return CompanionStatus.OCCUPIED;
        if(!p.orders.tamed() || !p.assignments.assignedWork(stationPosition()) || !CompanionJobs.permitted(p,stationPosition()))return CompanionStatus.FORBIDDEN;
        if(entrance==null || standing==null)return CompanionStatus.BLOCKED;
        if(p.level().getGameTime()>=nextClearance){
            nextClearance=p.level().getGameTime()+20;
            if(!clearAt(p,entrance) || !clearAt(p,standing) || !clearPassage(p,entrance,standing)){
                entrance=standing=null;nextSpace=0;return CompanionStatus.BLOCKED;
            }
        }
        return planning?CompanionReadiness.shared(wheel,wheel::companionStatus):wheel.companionStatus();
    }
    public boolean claim(PeepoEntity p){if(workStatus(p)!=CompanionStatus.READY)return false;worker=p.getUUID();lease=p.level().getGameTime()+100;return true;}
    public boolean occupy(PeepoEntity p){
        if(!isOccupant(p))return false;
        if(p.position().distanceToSqr(standing)>1.0E-6 && (!clearAt(p,standing) || !clearPassage(p,p.position(),standing)))return false;
        lease=p.level().getGameTime()+100;operating=true;
        var front=wheel.getBlockState().getValue(io.github.jimbozoomer.jugcraft.agriculture.SpinningWheelBlock.FACING);
        double dx=-front.getStepX(),dz=-front.getStepZ();
        float yaw=(float)Math.toDegrees(Math.atan2(-dx,dz));if(p.position().distanceToSqr(standing)>1.0E-6)p.snapTo(standing.x,standing.y,standing.z,yaw,0);
        p.setYRot(yaw);p.yBodyRot=yaw;p.setYHeadRot(yaw);return true;
    }
    public void release(PeepoEntity p){if(worker!=null && worker.equals(p.getUUID()))removed();}
    public CompanionStatus work(PeepoEntity p){return isOccupant(p) && p.preferences.canWork() && p.orders.station(this)?wheel.assist(p):CompanionStatus.IDLE;}
}
