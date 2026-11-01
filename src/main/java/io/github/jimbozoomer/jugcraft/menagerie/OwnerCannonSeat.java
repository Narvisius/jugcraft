package io.github.jimbozoomer.jugcraft.menagerie;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** A voluntary, one-person cannon seat; breaking/unloading its cannon releases the occupant. */
public final class OwnerCannonSeat extends Entity {
    private static final net.minecraft.network.syncher.EntityDataAccessor<BlockPos> ANCHOR=SynchedEntityData.defineId(OwnerCannonSeat.class,net.minecraft.network.syncher.EntityDataSerializers.BLOCK_POS);
    public BlockPos anchor=BlockPos.ZERO;
    public void anchor(BlockPos pos){anchor=pos.immutable();entityData.set(ANCHOR,anchor);}
    public OwnerCannonSeat(EntityType<? extends OwnerCannonSeat> type,Level level){super(type,level);noPhysics=true;}
    @Override public void tick(){super.tick();if(level().isClientSide())anchor=entityData.get(ANCHOR);if(level() instanceof ServerLevel level){
        if(getPassengers().isEmpty()||!level.hasChunkAt(anchor)||!(level.getBlockEntity(anchor) instanceof OwnerSupplementData d)||!d.block().function.equals("cannon")){ejectPassengers();discard();return;}
        d.cannonRider=getFirstPassenger().getUUID();setYRot(d.cannonYaw);setXRot(d.cannonPitch);
    }}
    @Override protected boolean canAddPassenger(Entity entity){return getPassengers().isEmpty();}
    @Override protected Vec3 getPassengerAttachmentPoint(Entity entity,EntityDimensions dimensions,float scale){return Vec3.ZERO;}
    @Override protected void removePassenger(Entity passenger){super.removePassenger(passenger);if(level().getBlockEntity(anchor) instanceof OwnerSupplementData d){d.cannonRider=null;d.changed();}passenger.resetFallDistance();}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){builder.define(ANCHOR,BlockPos.ZERO);}
    @Override protected void addAdditionalSaveData(ValueOutput out){out.store("Cannon",BlockPos.CODEC,anchor);}
    @Override protected void readAdditionalSaveData(ValueInput in){anchor(in.read("Cannon",BlockPos.CODEC).orElse(BlockPos.ZERO));}
    @Override public boolean hurtServer(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float damage){return false;}
}
