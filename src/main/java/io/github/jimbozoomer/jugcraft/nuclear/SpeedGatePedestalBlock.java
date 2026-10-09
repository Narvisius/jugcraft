package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One half of a speed gate's pedestal (SpeedGateBlock): a stainless cabinet two blocks long, lengthwise along its
 * lanes ({@link #AXIS}), the doors' housing over the line between its halves with a slot each side of the line down
 * both long faces. It has no item of its own: it comes with a gate and goes with the last gate that uses it, and taking
 * it away takes the gates beside it down (each dropping its item).
 */
public class SpeedGatePedestalBlock extends Block {
	public enum Half implements StringRepresentable {
		A, B;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
	public static final EnumProperty<Half> HALF = EnumProperty.create("half", Half.class);
	/** Half b, axis z (the south half: the line along its north edge): body, and the housing over the line. */
	private static final VoxelShape B_Z = Shapes.or(Block.box(2, 0, 0, 14, 15, 16), Block.box(2, 15, 0, 14, 24, 4.4));
	private static final VoxelShape A_Z = Shapes.or(Block.box(2, 0, 0, 14, 15, 16), Block.box(2, 15, 11.6, 14, 24, 16));
	private static final VoxelShape B_X = Shapes.or(Block.box(0, 0, 2, 16, 15, 14), Block.box(11.6, 15, 2, 16, 24, 14));
	private static final VoxelShape A_X = Shapes.or(Block.box(0, 0, 2, 16, 15, 14), Block.box(0, 15, 2, 4.4, 24, 14));

	public SpeedGatePedestalBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Z).setValue(HALF, Half.A));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS, HALF);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		boolean b = state.getValue(HALF) == Half.B;
		// Axis x: the models turned a quarter (half b, the south half lengthwise z, becomes the west half).
		return state.getValue(AXIS) == Direction.Axis.Z ? (b ? B_Z : A_Z) : (b ? B_X : A_X);
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return true;
	}

	@Override
	protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
		return 1.0F;
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(PlantInterior.BLOCKS.get("speed_gate"));
	}

	/** Which side of the pair's middle (the gates' door line) this half is on: b south or west, a north or east. */
	private static Direction side(BlockState state) {
		boolean b = state.getValue(HALF) == Half.B;
		return state.getValue(AXIS) == Direction.Axis.Z ? (b ? Direction.SOUTH : Direction.NORTH) : (b ? Direction.WEST : Direction.EAST);
	}

	private static BlockState withSide(BlockState state, Direction side) {
		return state.setValue(AXIS, side.getAxis())
				.setValue(HALF, side == Direction.SOUTH || side == Direction.WEST ? Half.B : Half.A);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return withSide(state, rotation.rotate(side(state)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return withSide(state, mirror.mirror(side(state)));
	}

	/** The gates this pedestal stands beside (their lane blocks). */
	private static java.util.List<BlockPos> lanes(LevelReader level, BlockPos pedestal) {
		Block gate = PlantInterior.BLOCKS.get("speed_gate");
		java.util.List<BlockPos> out = new java.util.ArrayList<>();
		for (Direction side : Direction.Plane.HORIZONTAL) {
			for (int along = -1; along <= 1; along++) {
				BlockPos lane = pedestal.relative(side).relative(side.getClockWise(), along);
				BlockState state = level.getBlockState(lane);
				if (state.is(gate) && SpeedGateBlock.pedestals(lane, state.getValue(SpeedGateBlock.FACING)).contains(pedestal)) {
					out.add(lane);
				}
			}
		}
		return out;
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide()) {
			for (BlockPos lane : lanes(level, pos)) {
				level.destroyBlock(lane, !player.preventsBlockDrops(), player);
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	/** However a pedestal goes otherwise (an explosion, a command), the gates beside it come down with it. */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (SpeedGateBlock.clearing() || level.getBlockState(pos).is(this)) {
			return;
		}
		for (BlockPos lane : lanes(level, pos)) {
			level.destroyBlock(lane, true);
		}
	}
}
