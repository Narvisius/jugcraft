package local.peepo;

import java.util.UUID;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** A pot owns one short-lived reservation. No ticking worker search or saved entity reference. */
public final class CookingPotJob implements CompanionJob {
    private final CookingPotBlockEntity pot;
    private UUID worker;
    private long lease, nextEntrance;
    private Vec3 entrance;
    private Direction rimSide = Direction.NORTH;
    private double rimHeight = WorkAnimation.STIR_HEIGHT;
    private boolean mounted;
    private long nextClearance;
    private float checkedHeight;
    private boolean clearance;
    public CookingPotJob(CookingPotBlockEntity pot) { this.pot = pot; }
    public CookingPotJob prepare(PeepoEntity npc) {
        expire();
        if (worker == null && npc.level().getGameTime() >= nextEntrance) {
            nextEntrance = npc.level().getGameTime() + 20;
            entrance = null;
            nextClearance = 0;
            updateRimHeight();
            // The fixed pot model's bail lies in the east-west plane (z=7.5..8.5).
            // Its east/west lugs and uprights must never be used as standing rim positions.
            for (var side : new Direction[]{Direction.NORTH,Direction.SOUTH}) for (int dy = -1; dy <= 0; dy++) {
                var pos = stationPosition().relative(side).offset(0, dy, 0);
                if (!npc.level().hasChunkAt(pos) || !npc.level().getBlockState(pos.below()).isFaceSturdy(npc.level(), pos.below(), Direction.UP)) continue;
                var point = Vec3.atBottomCenterOf(pos);
                if(npc.navigationMemory.failed(npc,stationPosition(),point))continue;
                // Check the real companion size at both the lower entrance and this side of the rim.
                if (!clearAt(npc, point) || !clearAt(npc, rimPosition(side))) continue;
                if (entrance == null || npc.position().distanceToSqr(point) < npc.position().distanceToSqr(entrance)) {
                    entrance = point; rimSide = side;
                }
            }
        }
        return this;
    }
    private PeepoEntity occupant() {
        return pot.getLevel() instanceof ServerLevel level && worker != null && level.getEntity(worker) instanceof PeepoEntity npc ? npc : null;
    }
    private void expire() {
        if (worker == null) return;
        var npc = occupant();
        if (pot.isRemoved() || pot.getLevel().getGameTime() > lease || npc == null || !npc.isAlive()) removed();
    }
    public void removed() {
        var npc = occupant();
        if (npc != null) {
            npc.setWorkAnimation(WorkAnimation.NONE, stationPosition());
            if (mounted) { npc.setNoGravity(false); npc.setDeltaMovement(Vec3.ZERO); npc.leaveCompanionBed(); }
        }
        worker = null; mounted = false; nextEntrance = nextClearance = 0;
    }
    public Kind kind() { return Kind.WORK; }
    public void approachFailed(PeepoEntity npc){nextEntrance=nextClearance=0;entrance=null;}
    public BlockPos stationPosition() { return pot.getBlockPos(); }
    public Vec3 approachPosition() {
        var npc = mounted ? occupant() : null;
        return npc != null ? npc.position() : entrance == null ? Vec3.atBottomCenterOf(stationPosition().north()) : entrance;
    }
    public boolean availableTo(PeepoEntity npc) { expire(); return !pot.isRemoved() && (worker == null || worker.equals(npc.getUUID())); }
    public boolean isOccupant(PeepoEntity npc) {
        return worker != null && worker.equals(npc.getUUID()) && npc.level() == pot.getLevel()
            && npc.level().getGameTime() <= lease && (mounted ? stationPosition().distToCenterSqr(npc.position()) < 4 : npc.position().distanceToSqr(approachPosition()) < .81);
    }
    public CompanionStatus workStatus(PeepoEntity npc) { return status(npc,false); }
    public CompanionStatus planningFacts(PeepoEntity npc) { return status(npc,true); }
    private CompanionStatus status(PeepoEntity npc,boolean planning) {
        if (!availableTo(npc)) return CompanionStatus.OCCUPIED;
        if (pot.isLocked() || !npc.orders.tamed() || !npc.assignments.assignedWork(stationPosition()) || !CompanionJobs.permitted(npc, stationPosition())) return CompanionStatus.FORBIDDEN;
        if (entrance == null || !roomFor(npc)) return CompanionStatus.BLOCKED;
        return planning?CompanionReadiness.shared(pot,pot::cookingStatus):pot.cookingStatus();
    }
    private boolean roomFor(PeepoEntity npc) {
        long now=npc.level().getGameTime();
        if(now<nextClearance && checkedHeight==npc.getBbHeight())return clearance;
        nextClearance=now+20;checkedHeight=npc.getBbHeight();
        updateRimHeight();
        if (!mounted) {
            if (entrance == null || !clearAt(npc, entrance)) return clearance=false;
            var floor=BlockPos.containing(entrance).below();
            if(!npc.level().hasChunkAt(floor) || !npc.level().getBlockState(floor).isFaceSturdy(npc.level(),floor,Direction.UP))return clearance=false;
        }
        // Stationary rim position, including Jughead's jug; at most once per second.
        return clearance=clearAt(npc, rimPosition());
    }
    private boolean clearAt(PeepoEntity npc, Vec3 point) {
        var bounds=npc.getBoundingBox().move(point.subtract(npc.position()));
        for(int x=BlockPos.containing(bounds.minX,0,0).getX();x<=BlockPos.containing(bounds.maxX,0,0).getX();x++)
            for(int z=BlockPos.containing(0,0,bounds.minZ).getZ();z<=BlockPos.containing(0,0,bounds.maxZ).getZ();z++)
                if(!npc.level().hasChunkAt(new BlockPos(x,stationPosition().getY(),z)))return false;
        return CompanionHazards.safeAt(npc,point) && npc.level().noCollision(bounds);
    }
    private void updateRimHeight() {
        var shape=pot.getBlockState().getCollisionShape(pot.getLevel(),stationPosition());
        rimHeight=(shape.isEmpty()?WorkAnimation.STIR_HEIGHT:shape.max(Direction.Axis.Y))+.001;
    }
    private Vec3 rimPosition() { return rimPosition(rimSide); }
    private Vec3 rimPosition(Direction side) {
        var pos=stationPosition();
        return new Vec3(pos.getX()+.5+side.getStepX()*WorkAnimation.STIR_RADIUS,
            pos.getY()+rimHeight,pos.getZ()+.5+side.getStepZ()*WorkAnimation.STIR_RADIUS);
    }
    public boolean claim(PeepoEntity npc) {
        if (workStatus(npc) != CompanionStatus.READY) return false;
        worker = npc.getUUID(); lease = npc.level().getGameTime() + 100; return true;
    }
    public boolean occupy(PeepoEntity npc) {
        if (!isOccupant(npc) || !availableTo(npc)) return false;
        lease = npc.level().getGameTime() + 100;
        var at=rimPosition();
        if(!roomFor(npc))return false;
        if(!mounted)npc.setBedExit(BlockPos.containing(entrance)); // Existing persisted safe-exit recovery also handles a save while stirring.
        mounted=true;npc.setNoGravity(true);npc.resetFallDistance();
        float yaw = rimSide.getOpposite().toYRot();
        // Only correct displacement; spoon motion is client-side and needs no position updates.
        if(npc.position().distanceToSqr(at)>1.0E-8)npc.snapTo(at.x, at.y, at.z, yaw, 0);
        npc.setYRot(yaw); npc.setXRot(0);
        npc.yBodyRot = yaw; npc.setYHeadRot(yaw); npc.setDeltaMovement(Vec3.ZERO); npc.getNavigation().stop();
        return true;
    }
    public void release(PeepoEntity npc) { if (worker != null && worker.equals(npc.getUUID())) removed(); }
    public CompanionStatus work(PeepoEntity npc) {
        if (!npc.preferences.canWork() || !npc.orders.station(this) || !pot.assist(npc)) return pot.cookingStatus();
        return CompanionStatus.WORKING;
    }
    @Override public WorkAnimation animation() { return WorkAnimation.STIR; }
}
