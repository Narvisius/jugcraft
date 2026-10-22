package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.server.ViewerWorldRules;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class ResidentTargetMixin {
    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void jugcraft$resident(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (ViewerWorldRules.blocksAttack((LivingEntity)(Object)this, target)) cir.setReturnValue(false);
    }
}
