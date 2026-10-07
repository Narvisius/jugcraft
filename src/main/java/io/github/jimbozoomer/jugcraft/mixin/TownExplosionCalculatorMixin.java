package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.town.TownProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Explosions without an entity source (beds, respawn anchors, commands) leave the town's protected blocks alone. */
@Mixin(ExplosionDamageCalculator.class)
public class TownExplosionCalculatorMixin {
	@Inject(method = "shouldBlockExplode", at = @At("HEAD"), cancellable = true)
	private void jugcraft$keepTown(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, float power,
			CallbackInfoReturnable<Boolean> cir) {
		if (TownProtection.shieldsBlock(explosion.level(), pos)) {
			cir.setReturnValue(false);
		}
	}
}
