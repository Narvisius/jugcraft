package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.tower.crew.CrewPaths;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Walkers go round the mod's pieces rather than into them (tower/crew/CrewPaths.obstacle): a window, a desk, a shelf or a
 * part of a big piece taller than a step is in the way, as a whole block is. Only the game's own default answer is
 * changed; a block that says for itself whether it can be walked through (a gate, a door) still says so. Not required:
 * should a game update move this, the game still starts and walkers just bump into the pieces again.
 */
@Mixin(BlockBehaviour.class)
public abstract class PiecePathMixin {
	@Inject(method = "isPathfindable", at = @At("HEAD"), cancellable = true, require = 0)
	private void jugcraft$piecesInTheWay(BlockState state, PathComputationType type, CallbackInfoReturnable<Boolean> cir) {
		if (type == PathComputationType.LAND && CrewPaths.obstacle(state)) {
			cir.setReturnValue(false);
		}
	}
}
