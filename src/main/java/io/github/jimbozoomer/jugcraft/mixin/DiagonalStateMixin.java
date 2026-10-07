package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.diagonal.DiagonalConnections;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fences and bars (glass panes are bars too) get the four diagonal properties, and work out their diagonals wherever
 * they work out their straight connections: when placed and when a neighbour changes (see DiagonalConnections).
 */
@Mixin({FenceBlock.class, IronBarsBlock.class})
public abstract class DiagonalStateMixin {
	@Inject(method = "createBlockStateDefinition", at = @At("TAIL"))
	private void jugcraft$diagonalProperties(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo callback) {
		builder.add(DiagonalConnections.NORTH_EAST, DiagonalConnections.SOUTH_EAST, DiagonalConnections.SOUTH_WEST,
				DiagonalConnections.NORTH_WEST);
	}

	@Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
	private void jugcraft$placeDiagonals(BlockPlaceContext context, CallbackInfoReturnable<BlockState> callback) {
		BlockState state = callback.getReturnValue();
		if (state != null) {
			callback.setReturnValue(DiagonalConnections.withDiagonals(state, context.getLevel(), context.getClickedPos()));
		}
	}

	@Inject(method = "updateShape(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelReader;"
			+ "Lnet/minecraft/world/level/ScheduledTickAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;"
			+ "Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;)"
			+ "Lnet/minecraft/world/level/block/state/BlockState;", at = @At("RETURN"), cancellable = true)
	private void jugcraft$updateDiagonals(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighbourPos, BlockState neighbour, RandomSource random, CallbackInfoReturnable<BlockState> callback) {
		callback.setReturnValue(DiagonalConnections.withDiagonals(callback.getReturnValue(), level, pos));
	}
}
