package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A decoration two blocks long lying on the ground, placed like a bed (Halloween decorations batch 18: the Stone
 * Sarcophagi and the Colossal Femur): from the block the player aims at, its head the next block the way they look
 * ({@link #FACING}). Breaking either half breaks both, and it drops once, from its head ({@link #PART}); nothing drops in
 * creative. Its hitboxes are given in pixels for a head facing north (the head the north block, the foot the south).
 */
public class LongDecorationBlock extends HorizontalDirectionalBlock {
	public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;
	private final Map<Direction, VoxelShape> headShapes = new EnumMap<>(Direction.class);
	private final Map<Direction, VoxelShape> footShapes = new EnumMap<>(Direction.class);

	public LongDecorationBlock(Properties properties, double[][] head, double[][] foot) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, BedPart.FOOT));
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			headShapes.put(facing, shape(head, facing));
			footShapes.put(facing, shape(foot, facing));
		}
	}

	/** Boxes in pixels for a block facing north, turned to {@code facing} the way a blockstate turns its model. */
	public static VoxelShape shape(double[][] boxes, Direction facing) {
		VoxelShape shape = Shapes.empty();
		for (double[] box : boxes) {
			shape = Shapes.or(shape, turned(box, facing));
		}
		return shape.optimize();
	}

	/** A box {x0, y0, z0, x1, y1, z1} in pixels, turned from north to {@code facing} (clockwise, seen from above). */
	public static VoxelShape turned(double[] b, Direction facing) {
		double x0 = b[0], z0 = b[2], x1 = b[3], z1 = b[5];
		int turns = switch (facing) {
			case EAST -> 1;
			case SOUTH -> 2;
			case WEST -> 3;
			default -> 0;
		};
		for (int i = 0; i < turns; i++) {
			double nx0 = 16 - z1, nx1 = 16 - z0, nz0 = x0, nz1 = x1;
			x0 = nx0;
			x1 = nx1;
			z0 = nz0;
			z1 = nz1;
		}
		return Block.box(Math.max(0, x0), b[1], Math.max(0, z0), Math.min(16, x1), Math.min(16, b[4]), Math.min(16, z1));
	}

	/** The head half's position, from either half. */
	public static BlockPos head(BlockState state, BlockPos pos) {
		return state.getValue(PART) == BedPart.HEAD ? pos : pos.relative(state.getValue(FACING));
	}

	/** The other half's position. */
	public static BlockPos other(BlockState state, BlockPos pos) {
		return pos.relative(towardOther(state));
	}

	private static Direction towardOther(BlockState state) {
		return state.getValue(PART) == BedPart.FOOT ? state.getValue(FACING) : state.getValue(FACING).getOpposite();
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection();
		BlockPos head = context.getClickedPos().relative(facing);
		Level level = context.getLevel();
		return level.getBlockState(head).canBeReplaced(context) && level.getWorldBorder().isWithinBounds(head)
				? defaultBlockState().setValue(FACING, facing) : null;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!level.isClientSide()) {
			level.setBlock(pos.relative(state.getValue(FACING)), state.setValue(PART, BedPart.HEAD), Block.UPDATE_ALL);
		}
	}

	/** Without its other half it goes too (and the head drops the decoration as it goes). */
	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		if (direction == towardOther(state)) {
			return neighbor.is(this) && neighbor.getValue(PART) != state.getValue(PART) && neighbor.getValue(FACING) == state.getValue(FACING)
					? state : Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
	}

	/** In creative, breaking the foot takes the head too without dropping it. */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && player.isCreative() && state.getValue(PART) == BedPart.FOOT) {
			BlockPos head = other(state, pos);
			BlockState headState = level.getBlockState(head);
			if (headState.is(this) && headState.getValue(PART) == BedPart.HEAD) {
				level.setBlock(head, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
				level.levelEvent(player, 2001, head, Block.getId(headState));
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return (state.getValue(PART) == BedPart.HEAD ? headShapes : footShapes).get(state.getValue(FACING));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, PART);
	}
}
