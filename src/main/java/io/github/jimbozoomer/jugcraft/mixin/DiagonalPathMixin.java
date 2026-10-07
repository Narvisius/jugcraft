package io.github.jimbozoomer.jugcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.jimbozoomer.jugcraft.diagonal.DiagonalConnections;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Mobs do not path diagonally across a diagonal join. A walking mob's diagonal step passes between the two blocks beside
 * it, and vanilla lets a narrow mob squeeze between two fences there. When those two blocks are joined to each other
 * diagonally (DiagonalConnections), their arm closes that gap, so the step is refused and the mob walks round instead of
 * pushing against the arm. No API covers the walk evaluator's choice of steps.
 */
@Mixin(WalkNodeEvaluator.class)
public abstract class DiagonalPathMixin extends NodeEvaluator {
	@ModifyReturnValue(method = "isDiagonalValid", at = @At("RETURN"))
	private boolean jugcraft$notAcrossADiagonal(boolean valid, Node from, Node first, Node second) {
		if (!valid || first == null || second == null || first.y != second.y
				|| Math.abs(second.x - first.x) != 1 || Math.abs(second.z - first.z) != 1) {
			return valid;
		}
		return !DiagonalConnections.joinedAcross(currentContext.getBlockState(new BlockPos(first.x, first.y, first.z)),
				currentContext.getBlockState(new BlockPos(second.x, second.y, second.z)), second.x - first.x, second.z - first.z);
	}
}
