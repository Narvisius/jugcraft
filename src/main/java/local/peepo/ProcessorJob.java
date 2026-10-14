package local.peepo;

import java.util.UUID;
import net.minecraft.world.level.block.entity.BlockEntity;
import io.github.jimbozoomer.jugcraft.machine.form.FormMachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.form.FormCell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** One exclusive ground-level helper position in a processor team. No worker scans. */
public final class ProcessorJob implements CompanionJob {
    private final BlockEntity machine;
    private final ProcessorHost host;
    private float entranceYaw=Float.NaN;
    private final int slot;
    private long requested=-1000, lastSpent=-1000;
    private UUID worker;
    private long lease, nextEntrance, nextClearance;
    private Vec3 entrance;
    private float checkedHeight;
    private boolean clearance;

    public ProcessorJob(ProcessorHost host,int slot) { this.host=host;this.machine=host.companionBlock();this.slot=slot; }
    @Override public WorkAnimation animation(){return WorkAnimation.processor(host.kind(),slot);}
    public boolean requestedAt(long now){return worker!=null && requested>=now-1 && requested<=now;}
    public boolean assignedTo(PeepoEntity npc){return worker!=null && worker.equals(npc.getUUID());}
    public Vec3 reservedPosition(){expire();return worker==null?null:entrance;}

    public ProcessorJob prepare(PeepoEntity npc) {
        expire();
        long now=npc.level().getGameTime();
        if(worker==null && now>=nextEntrance) {
            nextEntrance=now+20; nextClearance=0; entrance=null; entranceYaw=Float.NaN;
            if(machine instanceof FormMachineBlockEntity form){prepareForm(npc,form);return this;}
            // Bounded candidates along the front of the real footprint, plus its controller sides.
            // Arc furnace controller is one block above its casing base; dy=-1 reaches that floor.
            var state=machine.getBlockState();
            var front=state.getValue(io.github.jimbozoomer.jugcraft.machine.MachineBlock.FACING);
            for(int across=-3;across<=1;across++)for(int dy=-1;dy<=0;dy++) {
                if(host.kind()==io.github.jimbozoomer.jugcraft.machine.MachineKind.ARC_FURNACE && Math.abs(across)>1)continue;
                var anchor=stationPosition().relative(front.getClockWise(),across);
                // Do not stand beside a nonexistent part of a narrow/compact machine.
                var inside=anchor;
                if(!npc.level().hasChunkAt(inside))continue;
                if(host.kind()!=io.github.jimbozoomer.jugcraft.machine.MachineKind.ARC_FURNACE
                    && !CompanionAssignments.canonical(npc.level(),inside).equals(stationPosition()))continue;
                var point=new Vec3(anchor.getX()+.5+front.getStepX()*.85,
                    anchor.getY()+dy,anchor.getZ()+.5+front.getStepZ()*.85);
                consider(npc,point);
            }
            for(var side:Direction.Plane.HORIZONTAL)for(int dy=-1;dy<=0;dy++) {
                var point=new Vec3(stationPosition().getX()+.5+side.getStepX()*.85,
                    stationPosition().getY()+dy,stationPosition().getZ()+.5+side.getStepZ()*.85);
                consider(npc,point);
            }
        }
        return this;
    }
    /** Prefer the actual front row; if obstructed, try the other exterior ground faces. At most 48 points. */
    private void prepareForm(PeepoEntity npc,FormMachineBlockEntity machine){
        var form=machine.form();
        var front=machine.getBlockState().getValue(io.github.jimbozoomer.jugcraft.machine.MachineBlock.FACING);
        for(int column=0;column<form.width();column++)formPoint(npc,machine,column,0,front);
        if(entrance!=null)return;
        for(int row=0;row<form.depth();row++){
            formPoint(npc,machine,0,row,front.getClockWise());
            formPoint(npc,machine,form.width()-1,row,front.getCounterClockWise());
        }
        for(int column=0;column<form.width();column++)formPoint(npc,machine,column,form.depth()-1,front.getOpposite());
    }
    private void formPoint(PeepoEntity npc,FormMachineBlockEntity machine,int column,int row,Direction outward){
        var form=machine.form();int cell=form.cellIndex(column,row,0);
        if(form.cell(cell)!=FormCell.STRUCTURE)return;
        var front=machine.getBlockState().getValue(io.github.jimbozoomer.jugcraft.machine.MachineBlock.FACING);
        var anchor=form.cellPos(stationPosition(),front,cell);
        if(!npc.level().hasChunkAt(anchor) || !CompanionAssignments.canonical(npc.level(),anchor).equals(stationPosition()))return;
        for(int dy=0;dy>=-1;dy--){
            var point=new Vec3(anchor.getX()+.5+outward.getStepX()*.85,anchor.getY()+dy,
                anchor.getZ()+.5+outward.getStepZ()*.85);
            var previous=entrance;consider(npc,point);
            if(entrance!=previous)entranceYaw=outward.getOpposite().toYRot();
        }
    }
    private void consider(PeepoEntity npc,Vec3 point){
        if(npc.navigationMemory.failed(npc,stationPosition(),point))return;
        if(!host.helperPositionAvailable(slot,point) || !clearAt(npc,point))return;
        if(entrance==null || npc.position().distanceToSqr(point)<npc.position().distanceToSqr(entrance))entrance=point;
    }
    private boolean clearAt(PeepoEntity npc,Vec3 point) {
        var floor=BlockPos.containing(point).below();
        double radius=npc.getBbWidth()/2+.01;
        var bounds=new AABB(point.x-radius,point.y,point.z-radius,point.x+radius,point.y+npc.getBbHeight(),point.z+radius);
        for(int dx=-1;dx<=1;dx+=2)for(int dz=-1;dz<=1;dz+=2)
            if(!npc.level().hasChunkAt(BlockPos.containing(point.x+dx*radius,point.y,point.z+dz*radius)))return false;
        return npc.level().hasChunkAt(floor) && npc.level().getBlockState(floor).isFaceSturdy(npc.level(),floor,Direction.UP)
            && CompanionHazards.safeAt(npc,point) && npc.level().noCollision(bounds);
    }
    private boolean roomFor(PeepoEntity npc) {
        if(entrance==null)return false;
        long now=npc.level().getGameTime();
        if(now>=nextClearance || checkedHeight!=npc.getBbHeight()) {
            nextClearance=now+20;checkedHeight=npc.getBbHeight();clearance=host.helperPositionAvailable(slot,entrance) && clearAt(npc,entrance);
        }
        return clearance;
    }
    private PeepoEntity occupant() {
        return machine.getLevel() instanceof ServerLevel level && worker!=null
            && level.getEntity(worker) instanceof PeepoEntity npc ? npc : null;
    }
    private void expire() {
        if(worker==null)return;
        var npc=occupant();
        if(machine.isRemoved() || npc==null || !npc.isAlive() || machine.getLevel().getGameTime()>lease)removed();
    }
    public void removed() {
        var npc=occupant();
        if(npc!=null && npc.isUsingJobAt(stationPosition()))npc.setWorkAnimation(WorkAnimation.NONE,stationPosition());
        worker=null;requested=-1000;nextEntrance=0;host.resetCompanionEffort();
    }
    public Kind kind(){return Kind.WORK;}
    public void approachFailed(PeepoEntity npc){nextEntrance=nextClearance=0;entrance=null;}
    public BlockPos stationPosition(){return machine.getBlockPos();}
    public Vec3 approachPosition(){return entrance==null?Vec3.atBottomCenterOf(stationPosition().north()):entrance;}
    public boolean availableTo(PeepoEntity npc){expire();return !machine.isRemoved() && (worker==null || worker.equals(npc.getUUID()));}
    public boolean isOccupant(PeepoEntity npc){
        return worker!=null && worker.equals(npc.getUUID()) && npc.isAlive() && npc.level()==machine.getLevel()
            && npc.level().getGameTime()<=lease && entrance!=null && npc.position().distanceToSqr(entrance)<.81;
    }
    public CompanionStatus workStatus(PeepoEntity npc){return status(npc,false);}
    public CompanionStatus planningFacts(PeepoEntity npc){return status(npc,true);}
    private CompanionStatus status(PeepoEntity npc,boolean planning){
        if(!availableTo(npc))return CompanionStatus.OCCUPIED;
        if(host.companionLocked() || !npc.orders.tamed() || !npc.assignments.assignedWork(stationPosition())
            || !CompanionJobs.permitted(npc,stationPosition()))return CompanionStatus.FORBIDDEN;
        if(!roomFor(npc))return CompanionStatus.BLOCKED;
        return planning?CompanionReadiness.shared(machine,host::companionStatus):host.companionStatus();
    }
    public boolean claim(PeepoEntity npc){
        if(workStatus(npc)!=CompanionStatus.READY || !host.helperPositionAvailable(slot,entrance))return false;
        worker=npc.getUUID();lease=npc.level().getGameTime()+100;return true;
    }
    public boolean occupy(PeepoEntity npc){
        if(!availableTo(npc) || !isOccupant(npc) || !roomFor(npc))return false;
        lease=npc.level().getGameTime()+100;
        var front=machine.getBlockState().getValue(io.github.jimbozoomer.jugcraft.machine.MachineBlock.FACING);
        double dx=entrance.x-stationPosition().getX()-.5,dz=entrance.z-stationPosition().getZ()-.5;
        float yaw=!Float.isNaN(entranceYaw)?entranceYaw:dx*front.getStepX()+dz*front.getStepZ()>.8 ? front.getOpposite().toYRot()
            : (float)Math.toDegrees(Math.atan2(dx,-dz));
        // Remain on ordinary solid ground; no no-gravity mount or unload teleport needed.
        if(npc.position().distanceToSqr(entrance)>1.0E-6)npc.snapTo(entrance.x,entrance.y,entrance.z,yaw,0);
        npc.setYRot(yaw);npc.yBodyRot=yaw;npc.setYHeadRot(yaw);npc.getNavigation().stop();
        return true;
    }
    public void release(PeepoEntity npc){if(worker!=null && worker.equals(npc.getUUID()))removed();}
    public CompanionStatus work(PeepoEntity npc){
        if(!npc.preferences.canWork() || !npc.orders.station(this) || !npc.assignments.assignedWork(stationPosition()))return CompanionStatus.IDLE;
        if(!isOccupant(npc))return CompanionStatus.IDLE;
        // Register presence; only the machine's validated production step may spend reserve.
        requested=npc.level().getGameTime();
        return host.companionStatus()==CompanionStatus.READY?CompanionStatus.WORKING:host.companionStatus();
    }
    /** Called at most once per productive machine tick, after all recipe/resource gates pass. */
    public int contribute(long now,int effortPerQuarter){return contribute(now,effortPerQuarter,1);}
    /** One debit for all assisted lanes, never one duplicated presence claim per lane. */
    public int contribute(long now,int effortPerQuarter,int workUnits){
        expire();
        var npc=occupant();
        if(npc==null || lastSpent==now || requested<now-1 || requested>now || !isOccupant(npc)
            || !npc.isUsingJobAt(stationPosition()) || !npc.orders.tamed() || !npc.orders.station(this)
            || !npc.assignments.assignedWork(stationPosition()) || !roomFor(npc)
            || !npc.preferences.canWork() || host.companionLocked() || !CompanionJobs.permitted(npc,stationPosition()))return 0;
        int quarters=host.companionHelperCount()==2?1:2;
        int cost=quarters*Math.clamp(effortPerQuarter,1,
            io.github.jimbozoomer.jugcraft.machine.MachineCompanionEffort.MAX_PER_QUARTER)*Math.clamp(workUnits,1,4);
        if(!npc.hasWorkEnergy(cost))return 0;
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){
            if(npc.extractEnergy(cost,tx)!=cost)return 0;
            tx.commit();
        }
        lastSpent=now;return quarters;
    }

}
