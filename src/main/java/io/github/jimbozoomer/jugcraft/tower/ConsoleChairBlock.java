package io.github.jimbozoomer.jugcraft.tower;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Console Chair (work list item 6, with the Tower Control Console): a 24/7 operator chair (five-star base on twin
 * castors, gas lift, padded seat, tall mesh back with wings, headrest, padded arms) facing any of 16 directions, as you
 * face when you place it. It is drawn pulled up {@link #PUSH} towards where it faces (to its desk) by
 * client/ConsoleChairRenderer from tools/tower_control_console.py's mesh. Placed on a Raised Floor it takes the floor's
 * cell and carries the floor on ({@link #PLATFORM}: the floor tile is its block model); breaking it then gives both
 * back. Use it to sit; sneak to stand up.
 */
public final class ConsoleChairBlock extends Block implements EntityBlock, Sittable {
	public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;
	public static final BooleanProperty PLATFORM = BooleanProperty.create("platform");
	/** How far forward of its cell's centre the chair stands, and the seat's top over the chair's floor (blocks). */
	public static final double PUSH = 0.40, SEAT = 0.497;
	private static final VoxelShape[][] SHAPES = new VoxelShape[2][16];

	static {
		for (int p = 0; p < 2; p++) {
			double floor = p == 1 ? 3 : 0;
			for (int rot = 0; rot < 16; rot++) {
				double yaw = Math.toRadians(rot * 22.5);
				double fx = -Math.sin(yaw), fz = Math.cos(yaw);
				double cx = 8 + PUSH * 16 * fx, cz = 8 + PUSH * 16 * fz;
				VoxelShape seat = clamped(cx - 4.2, floor, cz - 4.2, cx + 4.2, floor + SEAT * 16, cz + 4.2);
				double bx = cx - fx * 4.5, bz = cz - fz * 4.5;
				VoxelShape back = clamped(bx - 3, floor + SEAT * 16, bz - 3, bx + 3, 16, bz + 3);
				VoxelShape shape = Shapes.or(seat, back);
				if (p == 1) {
					shape = Shapes.or(shape, Block.box(0, 0, 0, 16, 3, 16));
				}
				SHAPES[p][rot] = shape.optimize();
			}
		}
	}

	private static VoxelShape clamped(double x0, double y0, double z0, double x1, double y1, double z1) {
		return Block.box(Math.max(0, x0), y0, Math.max(0, z0), Math.min(16, x1), Math.min(16, y1), Math.min(16, z1));
	}

	public ConsoleChairBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(ROTATION, 0).setValue(PLATFORM, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ROTATION, PLATFORM);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		boolean onFloor = context.getLevel().getBlockState(context.getClickedPos()).getBlock() instanceof RaisedFloorBlock;
		return defaultBlockState().setValue(ROTATION, RotationSegment.convertToSegment(context.getRotation()))
				.setValue(PLATFORM, onFloor);
	}

	@Override
	public double seatHeight() {
		return SEAT;
	}

	/** Where a sitter's seat is: the chair's centre (pushed forward) at the cushion's top. */
	public static Vec3 seat(BlockPos pos, BlockState state) {
		double yaw = Math.toRadians(state.getValue(ROTATION) * 22.5);
		return new Vec3(pos.getX() + 0.5 - Math.sin(yaw) * PUSH, pos.getY() + (state.getValue(PLATFORM) ? 3 / 16.0 : 0) + SEAT,
				pos.getZ() + 0.5 + Math.cos(yaw) * PUSH);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			SeatEntity.sit(level, pos, player, seat(pos, state), state.getValue(ROTATION) * 22.5F);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ConsoleChairBlockEntity(pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES[state.getValue(PLATFORM) ? 1 : 0][state.getValue(ROTATION)];
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(ROTATION, rotation.rotate(state.getValue(ROTATION), 16));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.setValue(ROTATION, mirror.mirror(state.getValue(ROTATION), 16));
	}

	/** The chair's block entity: nothing to keep, it only gives the chair a renderer. */
	public static final class ConsoleChairBlockEntity extends BlockEntity {
		public ConsoleChairBlockEntity(BlockPos pos, BlockState state) {
			super(JugcraftTower.CONSOLE_CHAIR_ENTITY, pos, state);
		}
	}
}
