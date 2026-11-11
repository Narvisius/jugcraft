package io.github.jimbozoomer.jugcraft.tower;

import java.util.EnumSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Passive cable support: adjacent segments form continuous channels without block entities. */
public final class TowerCableTrayBlock extends HorizontalDirectionalBlock {
	public enum TrayShape implements StringRepresentable {
		STRAIGHT, CORNER, TEE, CROSS, END;
		@Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
	}
	public static final EnumProperty<TrayShape> SHAPE = EnumProperty.create("shape", TrayShape.class);

	public TowerCableTrayBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SHAPE, TrayShape.STRAIGHT));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, SHAPE);
	}

	/** North model keeps the original X-axis straight. Rotation is clockwise in blockstate JSON. */
	public static Set<Direction> connections(BlockState state) {
		EnumSet<Direction> arms = switch (state.getValue(SHAPE)) {
			case STRAIGHT -> EnumSet.of(Direction.WEST, Direction.EAST);
			case CORNER -> EnumSet.of(Direction.WEST, Direction.NORTH);
			case TEE -> EnumSet.of(Direction.WEST, Direction.EAST, Direction.NORTH);
			case CROSS -> EnumSet.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
			case END -> EnumSet.of(Direction.WEST);
		};
		int turns = switch (state.getValue(FACING)) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
		EnumSet<Direction> turned = EnumSet.noneOf(Direction.class);
		for (Direction arm : arms) {
			for (int i = 0; i < turns; i++) arm = arm.getClockWise();
			turned.add(arm);
		}
		return turned;
	}

	private BlockState connected(BlockGetter level, BlockPos pos, BlockState original) {
		EnumSet<Direction> arms = EnumSet.noneOf(Direction.class);
		for (Direction direction : Direction.Plane.HORIZONTAL)
			if (level.getBlockState(pos.relative(direction)).is(this)) arms.add(direction);
		if (arms.isEmpty()) return original.setValue(SHAPE, TrayShape.STRAIGHT);
		TrayShape shape = switch (arms.size()) { case 1 -> TrayShape.END; case 3 -> TrayShape.TEE; case 4 -> TrayShape.CROSS;
			default -> arms.contains(Direction.NORTH) && arms.contains(Direction.SOUTH)
					|| arms.contains(Direction.WEST) && arms.contains(Direction.EAST) ? TrayShape.STRAIGHT : TrayShape.CORNER; };
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			BlockState candidate = original.setValue(SHAPE, shape).setValue(FACING, facing);
			if (connections(candidate).equals(arms)) return candidate;
		}
		throw new IllegalStateException("Unrepresentable horizontal cable-tray junction");
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return connected(context.getLevel(), context.getClickedPos(), defaultBlockState().setValue(FACING, context.getHorizontalDirection()));
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
			Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		return direction.getAxis().isHorizontal() ? connected(level, pos, state) : state;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		VoxelShape shape = Block.box(4, 12, 4, 12, 16, 12);
		for (Direction arm : connections(state)) shape = Shapes.or(shape, switch (arm) {
			case WEST -> Block.box(0, 12, 4, 4, 16, 12);
			case EAST -> Block.box(12, 12, 4, 16, 16, 12);
			case NORTH -> Block.box(4, 12, 0, 12, 16, 4);
			case SOUTH -> Block.box(4, 12, 12, 12, 16, 16);
			default -> Shapes.empty();
		});
		return shape.optimize();
	}
}
