package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.botany.*;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Short saved spectral rail travel. Clients receive the collision mode; cargo never changes custody. */
@Mixin(AbstractMinecart.class)
abstract class BotanicalGhostCartMixin implements GhostCartAccess {
    @Unique private static final EntityDataAccessor<Boolean> jugcraft$ghost=SynchedEntityData.defineId(AbstractMinecart.class,EntityDataSerializers.BOOLEAN);
    @Unique private int jugcraft$floatTicks;
    @Unique private UUID jugcraft$actor;
    @Unique private Vec3 jugcraft$safe;
    @Unique private BlockPos jugcraft$origin;
    @Unique private boolean jugcraft$ownedCollision;
    @Unique private boolean jugcraft$recovering;
    @Unique private Vec3 jugcraft$hold;
    @Override public boolean jugcraft$isGhostCart(){return ((AbstractMinecart)(Object)this).getEntityData().get(jugcraft$ghost);}
    @Unique private boolean jugcraft$clear(AbstractMinecart cart,ServerLevel level,Vec3 position){
        var delta=position.subtract(cart.position());
        if(!level.noCollision(cart,cart.getBoundingBox().move(delta)))return false;
        for(var passenger:cart.getPassengers())if(!level.noCollision(passenger,passenger.getBoundingBox().move(delta)))return false;
        return true;
    }
    @Inject(method="defineSynchedData",at=@At("TAIL"))
    private void jugcraft$define(SynchedEntityData.Builder b,CallbackInfo ci){b.define(jugcraft$ghost,false);}
    @Unique private boolean jugcraft$exitAllowed(AbstractMinecart cart,ServerLevel level,Vec3 point){
        var player=Authority.answering(level,jugcraft$actor);
        if(player==null||jugcraft$origin==null)return false;
        var delta=point.subtract(cart.position());var box=cart.getBoundingBox().move(delta);
        for(var rider:cart.getPassengers())box=box.minmax(rider.getBoundingBox().move(delta));
        if(!level.getWorldBorder().isWithinBounds(box))return false;
        for(var at:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ)))
            if(!level.hasChunkAt(at)||!Authority.mayChange(level,player,at)||!ClaimTransfers.allowed(level,jugcraft$origin,at))return false;
        return jugcraft$clear(cart,level,point);
    }
    @Unique private void jugcraft$finish(AbstractMinecart cart,ServerLevel level){
        if(jugcraft$ownedCollision&&!jugcraft$clear(cart,level,cart.position())){
            Vec3 exit=null;
            if(jugcraft$safe!=null&&jugcraft$exitAllowed(cart,level,jugcraft$safe))exit=jugcraft$safe;
            // A player can build over the old checkpoint during the ride. Search only
            // loaded, permitted space; never delete blocks or eject cargo/passengers.
            if(exit==null&&jugcraft$safe!=null)search:for(int radius=1;radius<=4;radius++)for(int y=0;y<=4;y++)for(int side=0;side<8;side++){
                int x=switch(side){case 0,1,7->radius;case 3,4,5->-radius;default->0;};
                int z=switch(side){case 1,2,3->radius;case 5,6,7->-radius;default->0;};
                var at=jugcraft$safe.add(x,y,z);if(jugcraft$exitAllowed(cart,level,at)){exit=at;break search;}
            }
            if(exit==null){
                // Fail closed if the entire exit is obstructed. Hold position and retry
                // once a second rather than ending phasing inside solid blocks.
                jugcraft$recovering=true;jugcraft$hold=cart.position();jugcraft$floatTicks=1;cart.setDeltaMovement(Vec3.ZERO);return;
            }
            cart.setPos(exit);for(var rider:cart.getPassengers())cart.positionRider(rider);cart.setDeltaMovement(Vec3.ZERO);
        }
        jugcraft$recovering=false;jugcraft$floatTicks=0;if(jugcraft$ownedCollision)cart.noPhysics=false;jugcraft$ownedCollision=false;cart.getEntityData().set(jugcraft$ghost,false);
    }
    @Inject(method="tick",at=@At("HEAD"))
    private void jugcraft$before(CallbackInfo ci){
        var cart=(AbstractMinecart)(Object)this;
        if(!(cart.level() instanceof ServerLevel l)){boolean ghost=cart.getEntityData().get(jugcraft$ghost);if(ghost||jugcraft$ownedCollision)cart.noPhysics=ghost;jugcraft$ownedCollision=ghost;return;}
        if(jugcraft$recovering){jugcraft$hold=cart.position();cart.noPhysics=true;jugcraft$ownedCollision=true;cart.getEntityData().set(jugcraft$ghost,true);cart.setDeltaMovement(Vec3.ZERO);if(cart.tickCount%20==0)jugcraft$finish(cart,l);return;}
        var at=cart.getCurrentBlockPosOrRailBelow();var state=l.getBlockState(at);
        if(state.is(Jugtania.block("ghost_rail"))&&l.getBlockEntity(at) instanceof BotanicalGhostRail.Owner rail){
            var p=cart.getFirstPassenger() instanceof ServerPlayer rider?rider:rail.actor();
            if(p!=null&&Authority.mayChange(l,p,at)&&jugcraft$floatTicks==0){
                Vec3 safe=cart.position().add(0,.25,0);if(jugcraft$clear(cart,l,safe)){jugcraft$safe=safe;jugcraft$origin=at.immutable();jugcraft$actor=p.getUUID();jugcraft$floatTicks=20;}
            }
        }
        if(jugcraft$floatTicks<=0)return;
        var p=Authority.answering(l,jugcraft$actor);
        if(p==null||jugcraft$origin==null||cart.getFirstPassenger() instanceof ServerPlayer rider&&!rider.getUUID().equals(jugcraft$actor)||state.getBlock() instanceof BaseRailBlock&&!state.is(Jugtania.block("ghost_rail"))){jugcraft$finish(cart,l);return;}
        var velocity=cart.getDeltaMovement();var horizontal=new Vec3(velocity.x*1.4,0,velocity.z*1.4);if(horizontal.length()>1)horizontal=horizontal.normalize();var motion=horizontal.add(0,.2,0);
        var box=cart.getBoundingBox();for(var passenger:cart.getPassengers())box=box.minmax(passenger.getBoundingBox());box=box.expandTowards(motion).inflate(.05);
        for(var pos:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ))){
            if(!l.hasChunkAt(pos)||!l.getWorldBorder().isWithinBounds(pos)||!Authority.mayChange(l,p,pos)||!ClaimTransfers.allowed(l,jugcraft$origin,pos)||l.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.BARRIER)||l.getBlockState(pos).is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,io.github.jimbozoomer.jugcraft.Jugcraft.id("jugtania_ghost_rail_barrier")))){jugcraft$finish(cart,l);return;}
        }
        if(jugcraft$clear(cart,l,cart.position()))jugcraft$safe=cart.position();
        // No-physics is owned only for this finite ride; an unrelated cart mode is not reset.
        if(!cart.noPhysics||jugcraft$ownedCollision){cart.noPhysics=true;jugcraft$ownedCollision=true;cart.getEntityData().set(jugcraft$ghost,true);}
        cart.setDeltaMovement(motion);cart.fallDistance=0;jugcraft$floatTicks--;
        if(jugcraft$floatTicks==0)jugcraft$finish(cart,l);
    }
    @Inject(method="tick",at=@At("TAIL"))
    private void jugcraft$after(CallbackInfo ci){
        var cart=(AbstractMinecart)(Object)this;if(!(cart.level() instanceof ServerLevel l)||!jugcraft$ownedCollision)return;
        if(jugcraft$recovering){if(jugcraft$hold!=null)cart.setPos(jugcraft$hold);cart.setDeltaMovement(Vec3.ZERO);cart.fallDistance=0;for(var rider:cart.getPassengers())cart.positionRider(rider);return;}
        var p=Authority.answering(l,jugcraft$actor);var at=cart.blockPosition();
        if(p==null||!l.hasChunkAt(at)||!l.getWorldBorder().isWithinBounds(at)||!Authority.mayChange(l,p,at)||jugcraft$origin==null||!ClaimTransfers.allowed(l,jugcraft$origin,at)){
            if(jugcraft$safe!=null&&jugcraft$exitAllowed(cart,l,jugcraft$safe)){cart.setPos(jugcraft$safe);for(var rider:cart.getPassengers())cart.positionRider(rider);}
            cart.setDeltaMovement(Vec3.ZERO);jugcraft$finish(cart,l);
        }
    }
    @Inject(method="addAdditionalSaveData",at=@At("TAIL"))
    private void jugcraft$save(ValueOutput out,CallbackInfo ci){out.putBoolean("jugcraft_ghost_recovery",jugcraft$recovering);out.putInt("jugcraft_ghost_ticks",jugcraft$floatTicks);if(jugcraft$actor!=null)out.store("jugcraft_ghost_actor",UUIDUtil.CODEC,jugcraft$actor);if(jugcraft$safe!=null)out.store("jugcraft_ghost_safe",Vec3.CODEC,jugcraft$safe);if(jugcraft$origin!=null)out.store("jugcraft_ghost_origin",BlockPos.CODEC,jugcraft$origin);}
    @Inject(method="readAdditionalSaveData",at=@At("TAIL"))
    private void jugcraft$load(ValueInput in,CallbackInfo ci){jugcraft$recovering=in.getBooleanOr("jugcraft_ghost_recovery",false);jugcraft$floatTicks=Math.clamp(in.getIntOr("jugcraft_ghost_ticks",0),0,20);jugcraft$actor=in.read("jugcraft_ghost_actor",UUIDUtil.CODEC).orElse(null);jugcraft$safe=in.read("jugcraft_ghost_safe",Vec3.CODEC).orElse(null);jugcraft$origin=in.read("jugcraft_ghost_origin",BlockPos.CODEC).orElse(null);jugcraft$ownedCollision=jugcraft$floatTicks>0;}
}
