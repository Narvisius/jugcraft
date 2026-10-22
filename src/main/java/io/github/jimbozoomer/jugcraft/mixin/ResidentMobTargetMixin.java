package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.server.ViewerWorldRules;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class ResidentMobTargetMixin {
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void jugcraft$resident(LivingEntity target, CallbackInfo ci) {
        if (target != null && ViewerWorldRules.blocksAttack((Mob)(Object)this, target)) {
            ((Mob)(Object)this).setTarget(null);
            ci.cancel();
        }
    }
}
