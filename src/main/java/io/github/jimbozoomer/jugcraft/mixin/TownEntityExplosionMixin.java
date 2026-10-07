package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.town.TownProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Explosions an entity causes (creepers, TNT, fireballs) leave the town's protected blocks alone. */
@Mixin(Entity.class)
public class TownEntityExplosionMixin {
	@Inject(method = "shouldBlockExplode", at = @At("HEAD"), cancellable = true)
	private void jugcraft$keepTown(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, float power,
			CallbackInfoReturnable<Boolean> cir) {
		if (TownProtection.shieldsBlock(explosion.level(), pos)) {
			cir.setReturnValue(false);
		}
	}
}
