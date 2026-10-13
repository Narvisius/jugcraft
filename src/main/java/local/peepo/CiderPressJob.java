package local.peepo;

import java.util.UUID;
import io.github.jimbozoomer.jugcraft.agriculture.CiderPressBlockEntity;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.*;

/** One helper works the existing paced grinder/screw, using the general two-arm clip. */
public final class CiderPressJob implements CompanionJob {
    private final CiderPressBlockEntity press;
    private UUID worker;
    private Vec3 entrance;
    private long lease,nextSpace;
    private float checkedHeight;
    private long nextClearance;
    public CiderPressJob(CiderPressBlockEntity press){this.press=press;}
    public CiderPressJob prepare(PeepoEntity npc){
        expire();long now=npc.level().getGameTime();
        if(worker==null && (now>=nextSpace || checkedHeight!=npc.getBbHeight())){
            nextSpace=now+20;checkedHeight=npc.getBbHeight();entrance=null;
            for(var side:Direction.Plane.HORIZONTAL)for(int dy=0;dy>=-1;dy--){
                var p=Vec3.atBottomCenterOf(stationPosition().relative(side).offset(0,dy,0));
                if(npc.navigationMemory.failed(npc,stationPosition(),p))continue;
                var floor=BlockPos.containing(p).below();
                if(!npc.level().hasChunkAt(floor) || !npc.level().getBlockState(floor).isFaceSturdy(npc.level(),floor,Direction.UP))continue;
                if(!CompanionHazards.safeAt(npc,p) || !npc.level().noCollision(new AABB(p.x-.24,p.y,p.z-.24,p.x+.24,p.y+npc.getBbHeight(),p.z+.24)))continue;
                if(entrance==null || npc.position().distanceToSqr(p)<npc.position().distanceToSqr(entrance))entrance=p;
            }
        }
        return this;
    }
    private PeepoEntity occupant(){return press.getLevel() instanceof ServerLevel l && worker!=null && l.getEntity(worker) instanceof PeepoEntity p?p:null;}
    private void expire(){if(worker!=null && (press.isRemoved() || occupant()==null || !occupant().isAlive() || press.getLevel().getGameTime()>lease))removed();}
    public void removed(){var p=occupant();if(p!=null)p.setWorkAnimation(WorkAnimation.NONE,stationPosition());worker=null;nextSpace=0;}
    public Kind kind(){return Kind.WORK;}
    public void approachFailed(PeepoEntity npc){nextSpace=nextClearance=0;entrance=null;}
    public BlockPos stationPosition(){return press.getBlockPos();}
    public Vec3 approachPosition(){return entrance==null?Vec3.atBottomCenterOf(stationPosition()):entrance;}
    public boolean availableTo(PeepoEntity p){expire();return !press.isRemoved() && (worker==null || worker.equals(p.getUUID()));}
    public boolean isOccupant(PeepoEntity p){return worker!=null && worker.equals(p.getUUID()) && entrance!=null && p.position().distanceToSqr(entrance)<.64;}
    public CompanionStatus workStatus(PeepoEntity p){return status(p,false);}
    public CompanionStatus planningFacts(PeepoEntity p){return status(p,true);}
    private CompanionStatus status(PeepoEntity p,boolean planning){
        if(!availableTo(p))return CompanionStatus.OCCUPIED;
        if(!p.orders.tamed() || !p.assignments.assignedWork(stationPosition()) || !CompanionJobs.permitted(p,stationPosition()))return CompanionStatus.FORBIDDEN;
        if(entrance==null)return CompanionStatus.BLOCKED;
        if(p.level().getGameTime()>=nextClearance){
            nextClearance=p.level().getGameTime()+20;
            var floor=BlockPos.containing(entrance).below();
            if(!p.level().hasChunkAt(floor) || !p.level().getBlockState(floor).isFaceSturdy(p.level(),floor,Direction.UP)
                || !CompanionHazards.safeAt(p,entrance) || !p.level().noCollision(new AABB(entrance.x-.24,entrance.y,entrance.z-.24,entrance.x+.24,entrance.y+p.getBbHeight(),entrance.z+.24))){entrance=null;nextSpace=0;return CompanionStatus.BLOCKED;}
        }
        return planning?CompanionReadiness.shared(press,press::companionStatus):press.companionStatus();
    }
    public boolean claim(PeepoEntity p){if(workStatus(p)!=CompanionStatus.READY)return false;worker=p.getUUID();lease=p.level().getGameTime()+100;return true;}
    public boolean occupy(PeepoEntity p){
        if(!isOccupant(p))return false;lease=p.level().getGameTime()+100;
        double dx=stationPosition().getX()+.5-entrance.x,dz=stationPosition().getZ()+.5-entrance.z;
        float yaw=(float)Math.toDegrees(Math.atan2(-dx,dz));p.setYRot(yaw);p.yBodyRot=yaw;p.setYHeadRot(yaw);return true;
    }
    public void release(PeepoEntity p){if(worker!=null && worker.equals(p.getUUID()))removed();}
    public CompanionStatus work(PeepoEntity p){return isOccupant(p) && p.preferences.canWork() && p.orders.station(this)?press.assist(p):CompanionStatus.IDLE;}
}
