package io.github.jimbozoomer.jugcraft.mixin.client;

import io.github.jimbozoomer.jugcraft.diagonal.DiagonalConnections;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Breaking particles for a block with diagonals come from its shape without the diagonal arms. The game spawns at least
 * eight particles for each box of a block's outline, and a diagonal arm is a run of small boxes, so a fence with four
 * diagonals would burst into several hundred particles; without the arms it breaks like a plain fence. No API covers
 * the particles a broken block spawns.
 */
@Mixin(ClientLevel.class)
public abstract class DiagonalParticlesMixin {
	@Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
	private void jugcraft$breakWithoutArms(BlockPos pos, BlockState state, CallbackInfo callback) {
		if (DiagonalConnections.mask(state) != 0) {
			callback.cancel();
			((ClientLevel) (Object) this).addDestroyBlockEffect(pos, DiagonalConnections.withoutDiagonals(state));
		}
	}
}
