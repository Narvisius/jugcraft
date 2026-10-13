package local.peepo;

import java.util.UUID;
import io.github.jimbozoomer.jugcraft.kinetic.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

/** One assigned operator; actual accepted KE is paid from its reserve, with no banked free turns. */
public final class CrankJob implements CompanionJob {
    private final HandCrankBlockEntity crank;
    private UUID worker;
    private long lease,nextCheck,nextSpace,lastWorked=-1,idleUntil;
    private boolean pausedFull;
    private int demand;
    private Vec3 entrance;
    private float height;
    public CrankJob(HandCrankBlockEntity crank){this.crank=crank;}
    public CrankJob prepare(PeepoEntity npc){
        expire();
        if(worker==null && (npc.level().getGameTime()>=nextSpace || height!=npc.getBbHeight())){
            nextSpace=npc.level().getGameTime()+20;height=npc.getBbHeight();entrance=null;
            Direction face=crank.getBlockState().getValue(HandCrankBlock.FACING);
            for(Direction side:Direction.Plane.HORIZONTAL)for(int dy=0;dy>=-1;dy--){
                // Right as seen looking into the exposed handle. Align with the handle's
                // axial center, so the operator faces across its orbit instead of into the shaft.
                if(face.getAxis().isHorizontal() && side!=face.getClockWise())continue;
                var handle=handleCenter();
                var p=new Vec3(handle.x,stationPosition().getY()+dy,handle.z)
                    .add(side.getStepX()*.75,0,side.getStepZ()*.75);
                if(npc.navigationMemory.failed(npc,stationPosition(),p))continue;
                var floor=BlockPos.containing(p).below();
                var box=new AABB(p.x-.24,p.y,p.z-.24,p.x+.24,p.y+height+.6-dy,p.z+.24);
                if(!npc.level().hasChunkAt(floor) || !npc.level().getBlockState(floor).isFaceSturdy(npc.level(),floor,Direction.UP)
                    || !CompanionHazards.safeAt(npc,p) || !npc.level().noCollision(box))continue;
                if(entrance==null || npc.position().distanceToSqr(p)<npc.position().distanceToSqr(entrance))entrance=p;
            }
        }
        return this;
    }
    private PeepoEntity occupant(){return crank.getLevel() instanceof ServerLevel l && worker!=null && l.getEntity(worker) instanceof PeepoEntity p?p:null;}
    private void expire(){if(worker!=null && (crank.isRemoved() || occupant()==null || !occupant().isAlive() || crank.getLevel().getGameTime()>lease))removed();}
    public void removed(){var p=occupant();if(p!=null)p.setWorkAnimation(WorkAnimation.NONE,stationPosition());worker=null;nextSpace=0;}
    public Kind kind(){return Kind.WORK;}
    public void approachFailed(PeepoEntity npc){nextSpace=nextCheck=0;entrance=null;}
    public WorkAnimation animation(){return WorkAnimation.CRANK;}
    public BlockPos stationPosition(){return crank.getBlockPos();}
    public Vec3 approachPosition(){return entrance==null?Vec3.atBottomCenterOf(stationPosition()):entrance;}
    public boolean availableTo(PeepoEntity p){expire();return !crank.isRemoved() && (worker==null || worker.equals(p.getUUID()));}
    public boolean isOccupant(PeepoEntity p){return worker!=null && worker.equals(p.getUUID()) && entrance!=null && p.position().distanceToSqr(entrance)<.64;}
    public CompanionStatus workStatus(PeepoEntity p){
        if(!availableTo(p) || crank.manualTurning())return CompanionStatus.OCCUPIED;
        if(!p.orders.tamed() || !p.assignments.assignedWork(stationPosition()) || !CompanionJobs.permitted(p,stationPosition()))return CompanionStatus.FORBIDDEN;
        if(entrance==null)return CompanionStatus.BLOCKED;
        long now=p.level().getGameTime();
        if(now<idleUntil)return CompanionStatus.FULL;
        if(now>=nextCheck){
            nextCheck=now+20;
            var floor=BlockPos.containing(entrance).below();
            if(!p.level().hasChunkAt(floor) || !p.level().getBlockState(floor).isFaceSturdy(p.level(),floor,Direction.UP)
                || !CompanionHazards.safeAt(p,entrance) || !p.level().noCollision(new AABB(entrance.x-.24,entrance.y,entrance.z-.24,entrance.x+.24,stationPosition().getY()+p.getBbHeight()+.6,entrance.z+.24))){
                entrance=null;nextSpace=0;return CompanionStatus.BLOCKED;
            }
            demand=KineticNetworks.companionDemand((ServerLevel)p.level(),stationPosition(),crank.getBlockState().getValue(HandCrankBlock.FACING),pausedFull);
            pausedFull=demand==2;
        }
        return demand==2?CompanionStatus.FULL:demand==0?CompanionStatus.NO_INPUT:CompanionStatus.READY;
    }
    public boolean claim(PeepoEntity p){if(workStatus(p)!=CompanionStatus.READY)return false;worker=p.getUUID();lease=p.level().getGameTime()+100;return true;}
    private Vec3 handleCenter(){
        var outward=crank.getBlockState().getValue(HandCrankBlock.FACING).getOpposite();
        return Vec3.atCenterOf(stationPosition()).add(outward.getStepX()*3.5/16,0,outward.getStepZ()*3.5/16);
    }
    public boolean occupy(PeepoEntity p){
        if(!isOccupant(p))return false;lease=p.level().getGameTime()+100;
        var handle=handleCenter();
        double dx=handle.x-entrance.x,dz=handle.z-entrance.z;
        float yaw=(float)Math.toDegrees(Math.atan2(-dx,dz));
        if(p.position().distanceToSqr(entrance)>1.0E-6)p.snapTo(entrance.x,entrance.y,entrance.z,yaw,0);
        p.setYRot(yaw);p.yBodyRot=yaw;p.setYHeadRot(yaw);return true;
    }
    public void release(PeepoEntity p){if(worker!=null && worker.equals(p.getUUID()))removed();}
    public CompanionStatus work(PeepoEntity p){
        if(!isOccupant(p) || !p.preferences.canWork() || !p.orders.station(this))return CompanionStatus.IDLE;
        var status=workStatus(p);if(status!=CompanionStatus.READY)return status;
        long now=p.level().getGameTime();if(lastWorked==now)return CompanionStatus.WORKING;lastWorked=now;
        int offered;
        try(var tx=Transaction.openOuter()){offered=p.extractEnergy((int)HandCrankBlockEntity.OUTPUT,tx);}
        if(offered<=0)return CompanionStatus.RECOVERING;
        // Server callbacks are synchronous: simulation above reserves no items or energy between ticks.
        int taken=(int)KineticNetworks.push((ServerLevel)p.level(),stationPosition(),crank.getBlockState().getValue(HandCrankBlock.FACING),offered);
        if(taken<=0){idleUntil=now+80;return CompanionStatus.FULL;}
        try(var tx=Transaction.openOuter()){p.extractEnergy(taken,tx);tx.commit();}
        crank.companionTurned();return CompanionStatus.WORKING;
    }
}
