package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.concordance.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Recheck permissions at impact, before native damage AND tipped/spectral effects. */
@Mixin(AbstractArrow.class)
public abstract class AircraftArrowMixin {
    @Inject(method="onHitEntity",at=@At("HEAD"),cancellable=true)
    private void jugcraft$aircraftImpact(EntityHitResult hit,CallbackInfo ci){
        var arrow=(AbstractArrow)(Object)this;
        if(!arrow.entityTags().contains("jugcraft_aircraft_arrow"))return;
        if(!(arrow.getOwner() instanceof ServerPlayer pilot)||!(hit.getEntity() instanceof LivingEntity target)
            ||pilot.getRootVehicle()==target.getRootVehicle()||!ConcordanceEffects.mayHarm(pilot,target)||!Authority.mayStrike(pilot,target)){
            ci.cancel();arrow.discard();
        }
    }
}
