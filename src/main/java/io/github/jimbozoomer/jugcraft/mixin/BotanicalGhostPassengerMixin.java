package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.botany.GhostCartAccess;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Passengers share only their active spectral cart's in-wall exemption. */
@Mixin(Entity.class)
abstract class BotanicalGhostPassengerMixin {
    @Inject(method="isInWall",at=@At("HEAD"),cancellable=true)
    private void jugcraft$ghostPassenger(CallbackInfoReturnable<Boolean> cir){
        var entity=(Entity)(Object)this;
        if(entity.getVehicle() instanceof GhostCartAccess cart&&cart.jugcraft$isGhostCart())cir.setReturnValue(false);
    }
}
