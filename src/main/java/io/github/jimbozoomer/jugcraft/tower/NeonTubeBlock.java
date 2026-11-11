package io.github.jimbozoomer.jugcraft.tower;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Neon Tube (work list item 11, tools/neon_lines.py): a thin neon white tube on a dark clip rail standing in the
 * corner of a cell against a wall, in the end rods' place up the pillars' corners. {@link #FACING} points away from its
 * wall; {@link #SIDE} is the corner it stands in (left or right as you face the wall's way out). Placed against a wall,
 * it takes the half of the face the player clicked.
 */
public final class NeonTubeBlock extends Block {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<DoorHingeSide> SIDE = EnumProperty.create("side", DoorHingeSide.class);
	private static final VoxelShape[] SHAPES = new VoxelShape[8];

	public NeonTubeBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(SIDE, DoorHingeSide.LEFT));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, SIDE);
	}

	/** The rail and tube's box (tools/neon_lines.py TUBE_RAIL and TUBE_CORE), facing north in the west corner, turned. */
	static VoxelShape shape(Direction facing, DoorHingeSide side) {
		int k = facing.get2DDataValue() * 2 + side.ordinal();
		VoxelShape cached = SHAPES[k];
		if (cached != null) {
			return cached;
		}
		double x0 = 0, x1 = 2.0, z0 = 13.6, z1 = 16;
		if (side == DoorHingeSide.RIGHT) {
			double a = 16 - x1;
			x1 = 16 - x0;
			x0 = a;
		}
		// turn about the cell's middle from north to the facing (as the blockstate's y rotation turns the model)
		double[] p = turn(x0, z0, facing), q = turn(x1, z1, facing);
		VoxelShape made = Shapes.box(Math.min(p[0], q[0]) / 16, 0, Math.min(p[1], q[1]) / 16, Math.max(p[0], q[0]) / 16, 1,
				Math.max(p[1], q[1]) / 16);
		SHAPES[k] = made;
		return made;
	}

	private static double[] turn(double x, double z, Direction facing) {
		return switch (facing) {
			case EAST -> new double[] {16 - z, x};
			case SOUTH -> new double[] {16 - x, 16 - z};
			case WEST -> new double[] {z, 16 - x};
			default -> new double[] {x, z};
		};
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape(state.getValue(FACING), state.getValue(SIDE));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		Direction facing = face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite();
		Vec3 hit = context.getClickLocation().subtract(Vec3.atCenterOf(context.getClickedPos()));
		Direction left = facing.getCounterClockWise();
		// clicked on the wall's left half (as you face the way out), it stands in the left corner
		boolean onLeft = hit.x * left.getStepX() + hit.z * left.getStepZ() > 0;
		return defaultBlockState().setValue(FACING, facing).setValue(SIDE, onLeft ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		BlockState turned = state.rotate(mirror.getRotation(state.getValue(FACING)));
		return mirror == Mirror.NONE ? turned
				: turned.setValue(SIDE, state.getValue(SIDE) == DoorHingeSide.LEFT ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT);
	}
}
