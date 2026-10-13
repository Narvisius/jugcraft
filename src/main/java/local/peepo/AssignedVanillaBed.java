package local.peepo;

import net.minecraft.core.*;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;

/** A vanilla bed adapter only created for an explicit home; shares seat reservations. */
final class AssignedVanillaBed implements CompanionStation {
    final BlockPos pos;
    private final PeepoEntity npc;
    private final Direction facing;
    private final Vec3 entry;
    private boolean mounted;
    private long nextClearance;
    private boolean clearance;
    private AssignedVanillaBed(PeepoEntity npc,BlockPos pos,Direction facing,Vec3 entry){this.npc=npc;this.pos=pos;this.facing=facing;this.entry=entry;}
    static AssignedVanillaBed create(PeepoEntity npc,BlockPos pos){
        var level=npc.level();if(!level.hasChunkAt(pos))return null;
        var state=level.getBlockState(pos);if(!(state.getBlock() instanceof BedBlock))return null;
        Direction facing=state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        BlockPos foot=pos.relative(facing.getOpposite());
        for(Direction side:new Direction[]{facing.getClockWise(),facing.getCounterClockWise(),facing.getOpposite()}){
            var p=foot.relative(side);var at=Vec3.atBottomCenterOf(p);
            if(level.hasChunkAt(p) && level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP)
                && CompanionHazards.safeAt(npc,at) && level.noCollision(npc,new AABB(at.x-.22,at.y,at.z-.22,at.x+.22,at.y+1,at.z+.22)))return new AssignedVanillaBed(npc,pos,facing,at);
        }
        return null;
    }
    public Kind kind(){return Kind.BED;}
    public BlockPos stationPosition(){return pos;}
    private BlockPos foot(){return pos.relative(facing.getOpposite());}
    private Vec3 pillow(){return new Vec3(pos.getX()+.5-facing.getStepX()*.2,pos.getY()+.5625,pos.getZ()+.5-facing.getStepZ()*.2);}
    public Vec3 approachPosition(){return mounted?pillow():entry;}
    public boolean availableTo(PeepoEntity p){
        var level=p.level();if(!p.isRestNight() || !level.hasChunkAt(pos)||!level.hasChunkAt(foot()))return false;
        var state=level.getBlockState(pos);var bottom=level.getBlockState(foot());
        return state.getBlock() instanceof BedBlock && bottom.is(state.getBlock())
            && state.getValue(BlockStateProperties.HORIZONTAL_FACING)==facing && state.getValue(BlockStateProperties.BED_PART)==BedPart.HEAD
            && !state.getValue(BlockStateProperties.OCCUPIED) && CompanionSeats.externalAvailable(p,pos) && CompanionSeats.externalAvailable(p,foot())
            && clear(p);
    }
    private boolean clear(PeepoEntity p){
        if(!mounted || p.level().getGameTime()>=nextClearance){
            nextClearance=p.level().getGameTime()+20;
            clearance=p.level().noCollision(p,new AABB(pos.getX()+.15,pos.getY()+.57,pos.getZ()+.15,pos.getX()+.85,pos.getY()+1.15,pos.getZ()+.85));
        }return clearance;
    }
    public boolean claim(PeepoEntity p){if(!availableTo(p))return false;CompanionSeats.reserveExternal(p,pos,240);CompanionSeats.reserveExternal(p,foot(),240);return true;}
    public boolean occupy(PeepoEntity p){
        if(!availableTo(p)||!p.setRestMode(CompanionEnergy.Rest.SLEEPING))return false;
        CompanionSeats.reserveExternal(p,pos,40);CompanionSeats.reserveExternal(p,foot(),40);mounted=true;
        Vec3 at=pillow();float yaw=facing.getOpposite().toYRot();CompanionMotion.position(p,at,yaw);
        p.setDeltaMovement(Vec3.ZERO);p.resetFallDistance();p.setBedExit(BlockPos.containing(entry));return true;
    }
    public void release(PeepoEntity p){CompanionSeats.releaseExternal(p,pos);CompanionSeats.releaseExternal(p,foot());if(mounted){p.setRestMode(CompanionEnergy.Rest.NONE);p.leaveCompanionBed();}mounted=false;}
}
