package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Maze corn: three blocks of tall, ripe corn planted close as a wall, which a Corn Maze Gate plants along its maze's
 * walls. Unlike corn in a field it can't be walked through, it doesn't grow and it needs no farmland: it stands on any
 * solid ground. A section ({@link #SECTION}: 0 at the bottom) stands only on the one below, so breaking the bottom
 * brings the whole stalk down; the bottom gives back the corn kernel it was planted from.
 */
public class MazeCornBlock extends Block {
	public static final IntegerProperty SECTION = IntegerProperty.create("section", 0, 2);
	private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

	public MazeCornBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(SECTION, 0));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		int section = state.getValue(SECTION);
		BlockState below = level.getBlockState(pos.below());
		return section == 0 ? below.isFaceSturdy(level, pos.below(), Direction.UP)
				: below.is(this) && below.getValue(SECTION) == section - 1;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos,
			BlockState neighbor, RandomSource random) {
		return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return true;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(SECTION);
	}
}
