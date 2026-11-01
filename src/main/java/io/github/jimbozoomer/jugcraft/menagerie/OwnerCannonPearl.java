package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** A cannon's pearl moves its firing machine, never the operator. No anonymous world edits or chunk tickets. */
public final class OwnerCannonPearl extends ThrowableItemProjectile {
    private BlockPos source;private UUID identity,boat;private int life;
    public OwnerCannonPearl(EntityType<? extends OwnerCannonPearl> type,Level level){super(type,level);}
    @Override protected Item getDefaultItem(){return Items.ENDER_PEARL;}
    public void source(OwnerSupplementData cannon){source=cannon.getBlockPos().immutable();identity=UUID.randomUUID();cannon.cannonPearl=identity;cannon.setChanged();}
    public void boat(OwnerCannonBoat cannon){boat=cannon.getUUID();}
    @Override public void tick(){
        if(!level().isClientSide()){
            if(++life>300||getDeltaMovement().lengthSqr()>100){discard();return;}
            var next=position().add(getDeltaMovement());for(int i=0;i<=8;i++){var at=BlockPos.containing(position().lerp(next,i/8D));if(!level().hasChunkAt(at)||!level().getWorldBorder().isWithinBounds(at)){discard();return;}}
        }super.tick();
    }
    @Override protected void onHitEntity(EntityHitResult hit){discard();}
    @Override protected void onHitBlock(BlockHitResult hit){
        if(!(level() instanceof ServerLevel level))return;
        if(getOwner() instanceof ServerPlayer actor&&actor.isAlive()&&!actor.isSpectator()){
            var target=hit.getBlockPos().relative(hit.getDirection());
            if(boat!=null)moveBoat(level,actor,target);else moveCannon(level,actor,target,hit.getDirection());
        }discard();
    }
    private void moveBoat(ServerLevel level,ServerPlayer actor,BlockPos target){
        if(!(level.getEntity(boat) instanceof OwnerCannonBoat vessel)||!vessel.pearlAllowed(actor)||!OwnerAssemblyMotion.allowed(level,actor,target)||vessel.distanceToSqr(Vec3.atCenterOf(target))>65536)return;
        Vec3 at=Vec3.atBottomCenterOf(target);var bounds=vessel.getBoundingBox().move(at.subtract(vessel.position()));
        // Check passenger space too, before the native vehicle teleport carries its riders.
        if(!clear(level,actor,bounds,vessel))return;
        for(var passenger:vessel.getIndirectPassengers())if(!clear(level,actor,passenger.getBoundingBox().move(at.subtract(vessel.position())),vessel))return;
        if(vessel.teleportTo(level,at.x,at.y,at.z,Set.of(),vessel.getYRot(),vessel.getXRot(),false)){vessel.setDeltaMovement(Vec3.ZERO);vessel.resetFallDistance();effect(level,target);}
    }
    private boolean clear(ServerLevel level,ServerPlayer actor,AABB bounds,Entity moving){
        if(!level.getWorldBorder().isWithinBounds(bounds)||!level.noCollision(moving,bounds))return false;
        for(var at:BlockPos.betweenClosed(BlockPos.containing(bounds.minX,bounds.minY,bounds.minZ),BlockPos.containing(bounds.maxX,bounds.maxY,bounds.maxZ)))if(!OwnerAssemblyMotion.allowed(level,actor,at))return false;
        return true;
    }
    private void moveCannon(ServerLevel level,ServerPlayer actor,BlockPos target,Direction mount){
        if(source==null||identity==null||source.equals(target)||!OwnerAssemblyMotion.allowed(level,actor,source)||!OwnerAssemblyMotion.allowed(level,actor,target))return;
        if(!(level.getBlockEntity(source) instanceof OwnerSupplementData cannon)||!cannon.block().function.equals("cannon")||!identity.equals(cannon.cannonPearl)||!cannon.mayEdit(actor)||OwnerCannon.rider(cannon)!=null)return;
        // Match the existing assembly contract: one chunk is one save unit, including its inventory.
        if(!ChunkPos.containing(source).equals(ChunkPos.containing(target))){actor.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Cannon relocation must stay within its current chunk; boat pearls can travel farther."));return;}
        if(!level.getBlockState(target).isAir()||!level.getBlockState(target.relative(mount.getOpposite())).isFaceSturdy(level,target.relative(mount.getOpposite()),mount)||!level.getEntities(this,new AABB(target),e->!e.isSpectator()).isEmpty())return;
        var before=OwnerAssemblyMotion.capture(level,source);var destination=OwnerAssemblyMotion.capture(level,target);
        var placed=HouseholdBlock.set(before.state(),"facing",mount.getName());
        var relocated=net.minecraft.world.level.block.entity.BlockEntity.loadStatic(target,placed,before.data(),level.registryAccess());
        if(!(relocated instanceof OwnerSupplementData moved))return;
        moved.cannonPearl=null;moved.cannonFuse=0;moved.cannonRider=null;moved.cannonCooldown=Math.max(moved.cannonCooldown,OwnerCannon.COOLDOWN);
        int flags=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_MOVE_BY_PISTON;
        try{
            OwnerAssemblyMotion.detach(level,List.of(source));
            if(!level.setBlock(source,Blocks.AIR.defaultBlockState(),flags)||!level.setBlock(target,placed,flags))throw new IllegalStateException("Cannon relocation refused");
            level.setBlockEntity(moved);moved.setChanged();
        }catch(RuntimeException failure){
            OwnerAssemblyMotion.detach(level,List.of(source,target));OwnerAssemblyMotion.restore(level,source,before);OwnerAssemblyMotion.restore(level,target,destination);return;
        }finally{level.updateNeighborsAt(source,level.getBlockState(source).getBlock());level.updateNeighborsAt(target,level.getBlockState(target).getBlock());}
        effect(level,source);effect(level,target);
    }
    private static void effect(ServerLevel level,BlockPos at){level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,at.getX()+.5,at.getY()+.5,at.getZ()+.5,24,.4,.4,.4,.1);level.playSound(null,at,net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,net.minecraft.sounds.SoundSource.BLOCKS,1,1);}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putInt("FlightAge",life);if(source!=null)out.store("Source",BlockPos.CODEC,source);if(identity!=null)out.store("Identity",UUIDUtil.CODEC,identity);if(boat!=null)out.store("Boat",UUIDUtil.CODEC,boat);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);life=Math.clamp(in.getIntOr("FlightAge",0),0,301);source=in.read("Source",BlockPos.CODEC).orElse(null);identity=in.read("Identity",UUIDUtil.CODEC).orElse(null);boat=in.read("Boat",UUIDUtil.CODEC).orElse(null);}
}
