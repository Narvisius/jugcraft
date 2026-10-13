package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A cake (tools/cakes.py) set down: a block wide and {@value #HEIGHT} texels tall, its front to whoever set it down. It is
 * eaten, or cut with a knife, a quarter at a time as a pie is ({@link PieBlock}): the front right quarter first, as the
 * owner's INTERIOR drawing shows the cake cut, then the front left, the back left and the back right. Only a whole cake can
 * be picked up again. A burnt cake (no filling) is eaten as a burnt pie is. The owner's square pies and tarts
 * (tools/pies_and_tarts.py) are set down the same way, each its own {@link #height} tall.
 */
public class CakeBlock extends PieBlock {
	/** A cake's height in texels. */
	public static final int HEIGHT = 9;
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	/** The quarters of a cake facing north (x0, z0, x1, z1), in the order they are taken: the front right (the north-west), the front left, the back left, the back right. */
	static final int[][] QUARTERS = {{0, 0, 8, 8}, {8, 0, 16, 8}, {8, 8, 16, 16}, {0, 8, 8, 16}};
	/** How tall it stands, in texels. */
	public final int height;
	private final Map<Direction, VoxelShape[]> shapes = new EnumMap<>(Direction.class);

	/** A cake, {@value #HEIGHT} texels tall. */
	public CakeBlock(@Nullable PieFilling filling, Properties properties) {
		this(filling, HEIGHT, properties);
	}

	/** A cake or a square pie or tart, {@code height} texels tall. */
	public CakeBlock(@Nullable PieFilling filling, int height, Properties properties) {
		super(filling, properties);
		this.height = height;
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			VoxelShape[] byBites = new VoxelShape[SLICES];
			for (int bites = 0; bites < SLICES; bites++) {
				VoxelShape shape = Shapes.empty();
				for (int quarter = bites; quarter < SLICES; quarter++) {
					shape = Shapes.or(shape, quarter(quarter, facing, height));
				}
				byBites[bites] = shape.optimize();
			}
			shapes.put(facing, byBites);
		}
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
	}

	/** Quarter {@code index} of a cake facing {@code facing}, {@value #HEIGHT} texels tall. */
	public static VoxelShape quarter(int index, Direction facing) {
		return quarter(index, facing, HEIGHT);
	}

	/** Quarter {@code index} of a bake {@code height} texels tall facing {@code facing}: the north-facing quarter turned clockwise, as the blockstate turns the model. */
	public static VoxelShape quarter(int index, Direction facing, int height) {
		int[] q = QUARTERS[index];
		int x0 = q[0], z0 = q[1], x1 = q[2], z1 = q[3];
		int turns = switch (facing) {
			case EAST -> 1;
			case SOUTH -> 2;
			case WEST -> 3;
			default -> 0;
		};
		for (int i = 0; i < turns; i++) {
			int nx0 = 16 - z1, nx1 = 16 - z0, nz0 = x0, nz1 = x1;
			x0 = nx0;
			x1 = nx1;
			z0 = nz0;
			z1 = nz1;
		}
		return Block.box(x0, 0, z0, x1, height, z1);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapes.get(state.getValue(FACING))[state.getValue(BITES)];
	}

	/** Set down with its front to the player. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING);
	}
}
