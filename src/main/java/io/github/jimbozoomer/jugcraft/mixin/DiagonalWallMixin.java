package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.diagonal.DiagonalConnections;
import io.github.jimbozoomer.jugcraft.diagonal.DiagonalWalls;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A vanilla wall that joins a wall diagonally becomes its diagonal wall (DiagonalWalls.settle) wherever vanilla works
 * out the wall's state: when it is placed and when a neighbour changes. Its diagonal neighbours look again whenever it
 * is placed, broken or changed. The wall's own states and shapes are left as vanilla's. WallBlock also has a private
 * updateShape of its own, so the public one is named in full.
 */
@Mixin(WallBlock.class)
public abstract class DiagonalWallMixin {
	@Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
	private void jugcraft$placeDiagonalWall(BlockPlaceContext context, CallbackInfoReturnable<BlockState> callback) {
		BlockState state = callback.getReturnValue();
		if (state != null) {
			callback.setReturnValue(DiagonalWalls.settle(state, context.getLevel(), context.getClickedPos()));
		}
	}

	@Inject(method = "updateShape(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelReader;"
			+ "Lnet/minecraft/world/level/ScheduledTickAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;"
			+ "Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;)"
			+ "Lnet/minecraft/world/level/block/state/BlockState;", at = @At("RETURN"), cancellable = true)
	private void jugcraft$updateDiagonalWall(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighbourPos, BlockState neighbour, RandomSource random, CallbackInfoReturnable<BlockState> callback) {
		callback.setReturnValue(DiagonalWalls.settle(callback.getReturnValue(), level, pos));
	}

	/**
	 * Added to WallBlock, where it overrides BlockBehaviour's empty method: the game calls it for a block's old and new
	 * state whenever it is set without UPDATE_KNOWN_SHAPE.
	 */
	protected void updateIndirectNeighbourShapes(BlockState state, LevelAccessor level, BlockPos pos, int flags, int recursionLeft) {
		DiagonalConnections.updateDiagonalNeighbours(level, pos, flags, recursionLeft);
	}
}
