package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.jimtafacts.network.SpatialReservation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class SpatialResidentDamageMixin {
    @Inject(method="hurtServer",at=@At("HEAD"),cancellable=true)
    private void jugcraft$holdResidentHealth(ServerLevel level,DamageSource source,float amount,CallbackInfoReturnable<Boolean> result){
        if(SpatialReservation.reserved(level,((LivingEntity)(Object)this).blockPosition()))result.setReturnValue(false);
    }
}
