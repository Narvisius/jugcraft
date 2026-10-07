package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A square wooden lattice for climbing crops. It stands on farmland (keeping it farmland), on any
 * sturdy top face, or on another trellis or climbing plant, so trellises stack. Climbing crop seeds
 * ({@link TrellisSeedItem}) are planted on a trellis standing on farmland, and the plant then grows
 * up into the trellis above it. Like a fence, it blocks movement.
 */
public class TrellisBlock extends Block {
	private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

	public TrellisBlock(Properties properties) {
		super(properties);
	}

	/** Whether a trellis (or a climbing plant, which replaces one) can stand on {@code below}. */
	public static boolean canStandOn(BlockState below, LevelReader level, BlockPos belowPos) {
		return below.is(BlockTags.SUPPORTS_CROPS) || below.getBlock() instanceof TrellisBlock
				|| below.getBlock() instanceof TallCropBlock tall && tall.crop().trellis
				|| below.isFaceSturdy(level, belowPos, Direction.UP);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return canStandOn(level.getBlockState(pos.below()), level, pos.below());
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
			return Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}
}
