package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A galvanised steel ladder (climbable, in #minecraft:climbable): stringers, round rungs, brackets standing it off the
 * wall. From the third length up it has a safety cage round the climber, as tall fixed ladders have; the cage is only
 * to look at and never gets in the way. The top length carries handholds over to the landing, and a ladder carries on
 * up from the one below it without a wall behind (up past a roof's edge to the gate in its railing).
 */
public class SteelLadderBlock extends LadderBlock {
	public static final BooleanProperty CAGE = BooleanProperty.create("cage");
	public static final BooleanProperty TOP = BooleanProperty.create("top");
	/** Facing north: the climbing face out to the north, the wall to the south; stringers and rungs 10.6 to 16. */
	private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Block.box(0, 0, 10.6, 16, 16, 16));

	public SteelLadderBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(CAGE, false).setValue(TOP, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(CAGE, TOP);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockState below = level.getBlockState(pos.below());
		return super.canSurvive(state, level, pos) || below.getBlock() instanceof SteelLadderBlock && below.getValue(FACING) == state.getValue(FACING);
	}

	/** The cage and top for a ladder at {@code pos}: caged from the third length up, the top where none goes on. */
	public static BlockState shaped(BlockState state, BlockGetter level, BlockPos pos) {
		boolean cage = level.getBlockState(pos.below()).getBlock() instanceof SteelLadderBlock
				&& level.getBlockState(pos.below(2)).getBlock() instanceof SteelLadderBlock;
		boolean top = !(level.getBlockState(pos.above()).getBlock() instanceof SteelLadderBlock);
		return state.setValue(CAGE, cage).setValue(TOP, top);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		if (state == null) {
			// Placed on top of a ladder with nothing to hang on: it carries on the ladder below.
			BlockState below = context.getLevel().getBlockState(context.getClickedPos().below());
			if (!(below.getBlock() instanceof SteelLadderBlock)) {
				return null;
			}
			state = defaultBlockState().setValue(FACING, below.getValue(FACING))
					.setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
		}
		return shaped(state, context.getLevel(), context.getClickedPos());
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		BlockState updated = super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
		if (!(updated.getBlock() instanceof SteelLadderBlock)) {
			return updated;
		}
		return direction.getAxis() == Direction.Axis.Y ? shaped(updated, level, pos) : updated;
	}
}
