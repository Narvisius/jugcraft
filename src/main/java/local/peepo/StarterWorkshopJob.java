package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.workshop.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** One leased bench operator; the native station tick owns progress, fuel and output. */
public final class StarterWorkshopJob implements CompanionJob {
    public static final int EFFORT=4;
    private final WorkshopBlockEntity station;
    private final WorkshopCompanion port;
    private UUID worker;
    private Vec3 entrance;
    private long lease,nextSpace,nextClearance,heartbeat=Long.MIN_VALUE,lastPaid=Long.MIN_VALUE;
    private float height;
    StarterWorkshopJob(WorkshopBlockEntity station,WorkshopCompanion port){this.station=station;this.port=port;}
    public StarterWorkshopJob prepare(PeepoEntity npc){
        expire();long now=npc.level().getGameTime();
        if(worker==null && (now>=nextSpace || height!=npc.getBbHeight())){
            nextSpace=now+20;nextClearance=0;height=npc.getBbHeight();entrance=null;
            if(port.accessible(npc))port.prepareInstalled();
            for(var pos:frontEntrances(npc,station)){
                var point=Vec3.atBottomCenterOf(pos);
                if(npc.navigationMemory.failed(npc,stationPosition(),point))continue;
                if(entrance==null || npc.position().distanceToSqr(point)<npc.position().distanceToSqr(entrance))entrance=point;
            }
        }
        return this;
    }
    static boolean bench(BlockState state){return state.getBlock() instanceof WorkshopBlock block && block.station==0;}
    /** Work and deliveries share the two exterior front cells, never the other bench half. */
    static List<BlockPos> frontEntrances(PeepoEntity npc,WorkshopBlockEntity station){
        var points=new ArrayList<BlockPos>(2);
        var facing=station.getBlockState().getValue(WorkshopBlock.FACING);
        var footprint=((WorkshopBlock)station.getBlockState().getBlock()).footprint();
        for(int i=0;i<footprint.size();i++){
            var pos=footprint.partPos(station.getBlockPos(),facing,i).relative(facing);
            if(clear(npc,Vec3.atBottomCenterOf(pos)))points.add(pos);
        }
        return points;
    }
    private static boolean clear(PeepoEntity npc,Vec3 point){
        if(point==null)return false;var feet=BlockPos.containing(point);var floor=feet.below();
        double r=npc.getBbWidth()/2+.01;
        return CompanionJobs.permitted(npc,feet) && CompanionJobs.permitted(npc,floor) && npc.level().hasChunkAt(feet.above())
            && npc.level().getBlockState(floor).isFaceSturdy(npc.level(),floor,Direction.UP) && CompanionHazards.safeAt(npc,point)
            && npc.level().noCollision(new AABB(point.x-r,point.y,point.z-r,point.x+r,point.y+npc.getBbHeight(),point.z+r));
    }
    private PeepoEntity occupant(){return station.getLevel() instanceof ServerLevel server && worker!=null && server.getEntity(worker) instanceof PeepoEntity p?p:null;}
    private void expire(){if(worker!=null){var p=occupant();if(p==null || !p.isAlive() || station.isRemoved() || station.getLevel().getGameTime()>lease)removed();}}
    public void removed(){var p=occupant();if(p!=null)p.setWorkAnimation(WorkAnimation.NONE,stationPosition());worker=null;entrance=null;nextSpace=0;heartbeat=Long.MIN_VALUE;}
    public Kind kind(){return Kind.WORK;}
    public WorkAnimation animation(){return station.getItem(WorkshopBlockEntity.TOOL).is(JugcraftWorkshop.ITEMS.get("workshop_hammer"))?WorkAnimation.MALLET:WorkAnimation.INTERACT;}
    public BlockPos stationPosition(){return station.getBlockPos();}
    public Vec3 approachPosition(){return entrance==null?Vec3.atBottomCenterOf(stationPosition()):entrance;}
    public double arrivalDistanceSquared(){return .16;}
    public void approachFailed(PeepoEntity npc){nextSpace=nextClearance=0;entrance=null;}
    public boolean availableTo(PeepoEntity npc){expire();return !station.isRemoved() && station.station()==0 && !station.playerWorking() && (worker==null || worker.equals(npc.getUUID()));}
    private boolean present(PeepoEntity npc){return worker!=null && worker.equals(npc.getUUID()) && entrance!=null && npc.position().distanceToSqr(entrance)<=arrivalDistanceSquared() && CompanionHazards.safeAt(npc,npc.position());}
    public CompanionStatus workStatus(PeepoEntity npc){
        if(!port.accessible(npc))return CompanionStatus.FORBIDDEN;
        if(!availableTo(npc))return CompanionStatus.OCCUPIED;
        if(entrance==null)return CompanionStatus.BLOCKED;
        if(!CompanionJobs.permitted(npc,BlockPos.containing(entrance)))return CompanionStatus.FORBIDDEN;
        long now=npc.level().getGameTime();
        if(now>=nextClearance){nextClearance=now+20;if(!clear(npc,entrance)){entrance=null;nextSpace=0;return CompanionStatus.BLOCKED;}}
        return port.status();
    }
    public boolean claim(PeepoEntity npc){if(workStatus(npc)!=CompanionStatus.READY)return false;worker=npc.getUUID();lease=npc.level().getGameTime()+100;return true;}
    public void release(PeepoEntity npc){if(worker!=null && worker.equals(npc.getUUID()))removed();}
    public boolean occupy(PeepoEntity npc){
        if(!present(npc) || workStatus(npc)!=CompanionStatus.READY)return false;lease=npc.level().getGameTime()+100;
        var facing=station.getBlockState().getValue(WorkshopBlock.FACING).getOpposite();
        float yaw=(float)Math.toDegrees(Math.atan2(-facing.getStepX(),facing.getStepZ()));npc.setYRot(yaw);npc.yBodyRot=yaw;npc.setYHeadRot(yaw);return true;
    }
    public CompanionStatus work(PeepoEntity npc){
        var status=workStatus(npc);if(status!=CompanionStatus.READY)return status;
        if(!present(npc) || !npc.preferences.canWork() || !npc.orders.station(this) || npc.editingSettings())return CompanionStatus.IDLE;
        heartbeat=npc.level().getGameTime();return CompanionStatus.WORKING;
    }
    public boolean operating(){
        var npc=occupant();if(npc==null || !present(npc))return false;
        long now=npc.level().getGameTime();return now>=heartbeat && now-heartbeat<=1 && port.accessible(npc)
            && npc.preferences.canWork() && npc.orders.station(this) && !npc.editingSettings();
    }
    /** Invoked only after native readiness succeeds. One ordinary step, charged once, no player spoofing. */
    public boolean consumeWork(){
        expire();var npc=occupant();if(npc==null)return false;long now=npc.level().getGameTime();
        if(now==lastPaid || now<heartbeat || now-heartbeat>1 || !present(npc) || npc.editingSettings()
            || !npc.preferences.canWork() || !npc.orders.station(this) || workStatus(npc)!=CompanionStatus.READY)return false;
        try(var tx=Transaction.openOuter()){
            int cost=Math.min(EFFORT,npc.getEnergy());if(cost<=0 || npc.extractWorkEnergy(CompanionUniforms.Type.MECHANIC,cost,tx)!=cost)return false;
            tx.commit();lastPaid=now;return true;
        }
    }
}
