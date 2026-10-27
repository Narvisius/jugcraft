package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.jimtafacts.network.SpatialReservation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Includes teleports and no-physics entities, not just ordinary movement collision. */
@Mixin(Entity.class)
public abstract class SpatialEntityReservationMixin {
    @Inject(method="move",at=@At("HEAD"),cancellable=true)
    private void jugcraft$spatialMove(net.minecraft.world.entity.MoverType type,net.minecraft.world.phys.Vec3 movement,CallbackInfo info){
        var entity=(Entity)(Object)this;
        if(SpatialReservation.intersects(entity.level(),entity.getBoundingBox().expandTowards(movement)))info.cancel();
    }
    @Inject(method={"setPos(DDD)V","setPosRaw(DDD)V"},at=@At("HEAD"),cancellable=true)
    private void jugcraft$spatialEntry(double x,double y,double z,CallbackInfo info){
        var entity=(Entity)(Object)this;
        boolean denied=SpatialReservation.intersects(entity.level(),entity.getBoundingBox().move(x-entity.getX(),y-entity.getY(),z-entity.getZ()));
        if(denied){SpatialReservation.rejectEntry(entity);info.cancel();}else SpatialReservation.rejected(entity,false);
    }
}
