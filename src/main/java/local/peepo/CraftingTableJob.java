package local.peepo;

import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.*;

/** One crafter standing on a checked tabletop; the table owns paid progress and real items. */
public final class CraftingTableJob implements CompanionJob {
    public static final double STANDING_OFFSET=.24;
    private final CraftingTableCompanionBlockEntity table;
    private UUID worker;
    private Vec3 entrance,standing;
    private Direction side;
    private boolean mounted,clearance;
    private long lease,nextSpace,nextClearance;
    private float checkedHeight,checkedWidth;

    public CraftingTableJob(CraftingTableCompanionBlockEntity table){this.table=table;}

    public CraftingTableJob prepare(PeepoEntity npc){
        expire();long now=npc.level().getGameTime();
        if(worker==null && (now>=nextSpace || checkedHeight!=npc.getBbHeight() || checkedWidth!=npc.getBbWidth())){
            nextSpace=now+20;nextClearance=0;clearance=false;checkedHeight=npc.getBbHeight();checkedWidth=npc.getBbWidth();entrance=standing=null;
            double distance=Math.max(.85,.5+npc.getBbWidth()/2+.08);
            for(var candidate:Direction.Plane.HORIZONTAL){
                var point=Vec3.atBottomCenterOf(stationPosition()).add(candidate.getStepX()*distance,0,candidate.getStepZ()*distance);
                var top=Vec3.atBottomCenterOf(stationPosition()).add(candidate.getStepX()*STANDING_OFFSET,1.001,candidate.getStepZ()*STANDING_OFFSET);
                if(npc.navigationMemory.failed(npc,stationPosition(),point) || !clearEntry(npc,point) || !clearAt(npc,top) || !clearLift(npc,point,point,top))continue;
                if(entrance==null || npc.position().distanceToSqr(point)<npc.position().distanceToSqr(entrance)){entrance=point;standing=top;side=candidate;}
            }
        }
        return this;
    }

    private boolean clearEntry(PeepoEntity npc,Vec3 point){
        if(point==null)return false;var floor=BlockPos.containing(point).below();
        return npc.level().hasChunkAt(floor) && CompanionJobs.permitted(npc,floor)
            && npc.level().getBlockState(floor).isFaceSturdy(npc.level(),floor,Direction.UP) && clearAt(npc,point);
    }
    private boolean clearAt(PeepoEntity npc,Vec3 point){
        return point!=null && clearBounds(npc,bounds(npc,point)) && CompanionHazards.safeAt(npc,point);
    }
    private AABB bounds(PeepoEntity npc,Vec3 point){
        double radius=npc.getBbWidth()/2+.01;
        return new AABB(point.x-radius,point.y,point.z-radius,point.x+radius,point.y+npc.getBbHeight(),point.z+radius);
    }
    private boolean clearBounds(PeepoEntity npc,AABB box){
        for(int x=net.minecraft.util.Mth.floor(box.minX);x<=net.minecraft.util.Mth.floor(box.maxX);x++)
            for(int z=net.minecraft.util.Mth.floor(box.minZ);z<=net.minecraft.util.Mth.floor(box.maxZ);z++){
                var at=new BlockPos(x,net.minecraft.util.Mth.floor(box.minY),z);
                if(!npc.level().hasChunkAt(at) || !CompanionJobs.permitted(npc,at))return false;
            }
        return npc.level().noCollision(box);
    }
    private boolean clearSweep(PeepoEntity npc,Vec3 from,Vec3 to){
        return clearBounds(npc,bounds(npc,from).minmax(bounds(npc,to)));
    }
    /** Lift outside the solid block first, then cross above its top; a diagonal sweep would hit the table. */
    private boolean clearLift(PeepoEntity npc,Vec3 from,Vec3 entry,Vec3 top){
        var raised=new Vec3(entry.x,top.y,entry.z);
        return clearSweep(npc,from,entry) && clearSweep(npc,entry,raised) && clearSweep(npc,raised,top);
    }

    private PeepoEntity occupant(){return table.getLevel() instanceof ServerLevel level && worker!=null
        && level.getEntity(worker) instanceof PeepoEntity npc?npc:null;}
    private void expire(){
        if(worker==null)return;var npc=occupant();
        if(npc==null || !npc.isAlive() || table.isRemoved() || table.getLevel().getGameTime()>lease)removed();
    }
    public void removed(){
        var npc=occupant();
        if(npc!=null){
            npc.setWorkAnimation(WorkAnimation.NONE,stationPosition());
            if(mounted){
                npc.setNoGravity(false);npc.setDeltaMovement(Vec3.ZERO);npc.resetFallDistance();
                if(!npc.isCompanionUnloading() && !npc.isRemoved() && entrance!=null && standing!=null
                    && clearEntry(npc,entrance) && clearLift(npc,entrance,entrance,npc.position())){
                    CompanionMotion.position(npc,entrance,npc.getYRot());npc.setBedExit(null);
                }else npc.leaveCompanionBed();
            }
        }
        worker=null;mounted=clearance=false;entrance=standing=null;nextSpace=nextClearance=0;
    }
    public Kind kind(){return Kind.WORK;}
    public WorkAnimation animation(){return WorkAnimation.CRAFT;}
    public BlockPos stationPosition(){return table.getBlockPos();}
    public Vec3 approachPosition(){return mounted && standing!=null?standing:entrance==null?Vec3.atBottomCenterOf(stationPosition()):entrance;}
    public double arrivalDistanceSquared(){return .04;}
    public void approachFailed(PeepoEntity npc){if(mounted)removed();else{entrance=standing=null;nextSpace=nextClearance=0;}}
    public boolean availableTo(PeepoEntity npc){
        expire();return !table.isRemoved() && table.getLevel()==npc.level() && (worker==null || worker.equals(npc.getUUID()));
    }
    private boolean present(PeepoEntity npc){return worker!=null && worker.equals(npc.getUUID()) && entrance!=null && standing!=null
        && npc.position().distanceToSqr(approachPosition())<=arrivalDistanceSquared() && CompanionHazards.safeAt(npc,npc.position());}
    public CompanionStatus workStatus(PeepoEntity npc){return status(npc,false);}
    public CompanionStatus planningFacts(PeepoEntity npc){return status(npc,true);}
    private CompanionStatus status(PeepoEntity npc,boolean planning){
        if(!table.accessible(npc))return CompanionStatus.FORBIDDEN;
        if(!availableTo(npc))return CompanionStatus.OCCUPIED;
        if(entrance==null || standing==null)return CompanionStatus.BLOCKED;
        if(!CompanionJobs.permitted(npc,BlockPos.containing(entrance)) || !CompanionJobs.permitted(npc,BlockPos.containing(standing)))return CompanionStatus.FORBIDDEN;
        long now=npc.level().getGameTime();
        if(now>=nextClearance || checkedHeight!=npc.getBbHeight() || checkedWidth!=npc.getBbWidth()){
            nextClearance=now+20;checkedHeight=npc.getBbHeight();checkedWidth=npc.getBbWidth();
            clearance=clearEntry(npc,entrance) && clearAt(npc,standing) && clearLift(npc,entrance,entrance,standing);
        }
        if(!clearance)return CompanionStatus.BLOCKED;
        return planning?CompanionReadiness.shared(table,table::status):table.status();
    }
    public boolean claim(PeepoEntity npc){
        if(workStatus(npc)!=CompanionStatus.READY)return false;
        worker=npc.getUUID();lease=npc.level().getGameTime()+100;return true;
    }
    public boolean occupy(PeepoEntity npc){
        if(!present(npc) || workStatus(npc)!=CompanionStatus.READY)return false;
        if(!npc.preferences.canWork() || npc.editingSettings() || !npc.orders.station(this)
            || !npc.hasWorkEnergy((int)Math.ceil(CraftingTableCompanionBlockEntity.EFFORT*(100-npc.uniforms.saving(CompanionUniforms.Type.MECHANIC))/100.0)))return false;
        if(!mounted){
            if(!clearEntry(npc,entrance) || !clearAt(npc,standing) || !clearLift(npc,npc.position(),entrance,standing))return false;
            npc.setBedExit(BlockPos.containing(entrance));
        }else if(!clearAt(npc,standing))return false;
        lease=npc.level().getGameTime()+100;mounted=true;
        npc.setNoGravity(true);npc.setDeltaMovement(Vec3.ZERO);npc.resetFallDistance();npc.getNavigation().stop();
        CompanionMotion.position(npc,standing,side.getOpposite().toYRot());npc.setWorkAnimation(WorkAnimation.CRAFT,stationPosition());return true;
    }
    public void release(PeepoEntity npc){if(worker!=null && worker.equals(npc.getUUID()))removed();}
    public CompanionStatus work(PeepoEntity npc){
        if(!mounted || !present(npc) || !npc.orders.tamed() || !npc.assignments.assignedWork(stationPosition())
            || !npc.preferences.canWork() || !npc.orders.station(this) || npc.editingSettings())return CompanionStatus.IDLE;
        var status=workStatus(npc);return status==CompanionStatus.READY?table.advance(npc):status;
    }
}
