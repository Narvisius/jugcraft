package io.github.jimbozoomer.jugcraft.tower;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * A Faced Wall with the Neon Line along its face (Justin's first-floor look-over, 10 November 2026: the pillars' light
 * line carried round the rooms at the pillars' height; tools/faced_walls.py): {@link #FACING} is the face onto the hall,
 * in the block's material at its {@link TowerPanelBlock#PHASE} with the line's groove and glass tube in it (the Neon
 * Line's own body and fittings: tools/neon_3d.py); every other face is the room's paint, so the room behind a wall one
 * block thick never sees the line. The tube runs on into a line beside it ({@link #LEFT}, {@link #RIGHT}, as you look at
 * the face: another faced line, or a {@link NeonLineBlock}, which takes this one as a line too and turns a corner onto
 * it) and ends in a cap where there is none. It works that out from its neighbours (as tools/faced_walls.shape writes the
 * tower's), and like a Neon Line it lets its neighbours draw their faces against it, so the groove's end at a plain
 * block is closed by that block's face.
 */
public final class FacedLineBlock extends Block {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	/** A line beside it on the left, as you look at the face (clockwise of the facing, seen from above). */
	public static final BooleanProperty LEFT = BooleanProperty.create("left");
	/** A line beside it on the right, as you look at the face. */
	public static final BooleanProperty RIGHT = BooleanProperty.create("right");

	public FacedLineBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LEFT, false).setValue(RIGHT, false)
				.setValue(TowerPanelBlock.PHASE, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LEFT, RIGHT, TowerPanelBlock.PHASE);
	}

	/** Whether a faced line's tube runs on into {@code neighbour}: a line of either kind. */
	public static boolean joins(BlockState neighbour) {
		return neighbour.getBlock() instanceof FacedLineBlock || neighbour.getBlock() instanceof NeonLineBlock;
	}

	/** The state's ends from the blocks beside {@code pos} along its wall (its facing and phase kept). */
	public static BlockState shaped(BlockState state, BlockGetter level, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		return state.setValue(LEFT, joins(level.getBlockState(pos.relative(facing.getClockWise()))))
				.setValue(RIGHT, joins(level.getBlockState(pos.relative(facing.getCounterClockWise()))));
	}

	/** Its faced side toward whoever places it (they stand in the hall). */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
		return shaped(TowerPanelBlock.at(state, context.getClickedPos()), context.getLevel(), context.getClickedPos());
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		return direction.getAxis().isHorizontal() ? shaped(state, level, pos) : state;
	}

	/** (Left and right are told from the face, so they turn with it.) */
	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	/** Turned to face the mirrored way; a mirror swaps what is on its left and its right. */
	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		if (mirror == Mirror.NONE) {
			return state;
		}
		return state.rotate(mirror.getRotation(state.getValue(FACING))).setValue(LEFT, state.getValue(RIGHT))
				.setValue(RIGHT, state.getValue(LEFT));
	}
}
