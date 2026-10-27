package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.jimtafacts.network.SpatialReservation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A newly constructed entity whose initial position was rejected must not spawn at its constructor origin. */
@Mixin(ServerLevel.class)
public abstract class SpatialSpawnReservationMixin {
    @Inject(method={"addFreshEntity","addWithUUID"},at=@At("HEAD"),cancellable=true)
    private void jugcraft$spatialSpawn(Entity entity,CallbackInfoReturnable<Boolean> result){
        if(SpatialReservation.rejected(entity)||SpatialReservation.intersects((ServerLevel)(Object)this,entity.getBoundingBox()))result.setReturnValue(false);
    }
    @Inject(method="tickNonPassenger",at=@At("HEAD"),cancellable=true)
    private void jugcraft$freezeResident(Entity entity,org.spongepowered.asm.mixin.injection.callback.CallbackInfo info){
        if(SpatialReservation.reserved((ServerLevel)(Object)this,entity.blockPosition()))info.cancel();
    }
}
