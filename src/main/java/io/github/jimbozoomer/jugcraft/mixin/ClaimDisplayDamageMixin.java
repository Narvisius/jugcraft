package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.finance.LandRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** These display entities override damage before the living-entity damage event. */
@Mixin({ArmorStand.class, ItemFrame.class})
public abstract class ClaimDisplayDamageMixin {
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void jugcraft$claim(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source.getEntity() instanceof Player player && !LandRegistry.mayBuild(player, level, ((Entity)(Object)this).blockPosition())) cir.setReturnValue(false);
    }
}
