package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.tower.FurnitureBlock;
import io.github.jimbozoomer.jugcraft.tower.SeatEntity;
import io.github.jimbozoomer.jugcraft.tower.Sittable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Furniture that joins up side by side (sofas, desks, reception and kitchen counters, lab benches, benches): a row
 * of pieces of the same group facing the same way reads as one long piece, with end panels (sofa arms, desk legs)
 * only at the two ends. LEFT and RIGHT are as seen by whoever uses it. Some come with something different on top
 * each time one is placed (desks: a computer, paperwork, a lamp...), and seats can be sat on.
 */
public class RowFurnitureBlock extends FurnitureBlock implements Sittable {
	public static final BooleanProperty LEFT = BooleanProperty.create("left");
	public static final BooleanProperty RIGHT = BooleanProperty.create("right");
	/** The variant count of the block being built (read while its state definition is made). */
	private static int building;
	private final String group;
	private final double seat;
	private final int variants;
	private final IntegerProperty variant;

	public static RowFurnitureBlock create(Properties properties, String group, double seat, int variants, double[]... boxes) {
		building = variants;
		return new RowFurnitureBlock(properties, group, seat, variants, boxes);
	}

	private RowFurnitureBlock(Properties properties, String group, double seat, int variants, double[]... boxes) {
		super(properties, boxes);
		this.group = group;
		this.seat = seat;
		this.variants = variants;
		this.variant = variants > 1 ? VariantFurnitureBlock.property(variants) : null;
		BlockState state = defaultBlockState().setValue(LEFT, false).setValue(RIGHT, false);
		registerDefaultState(variant == null ? state : state.setValue(variant, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(LEFT, RIGHT);
		if (building > 1) {
			builder.add(VariantFurnitureBlock.property(building));
		}
	}

	@Override
	public double seatHeight() {
		return seat;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = joined(super.getStateForPlacement(context), context.getLevel(), context.getClickedPos());
		return variant == null ? state : state.setValue(variant, context.getLevel().getRandom().nextInt(variants));
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
		if (direction.getAxis() != Direction.Axis.Y) {
			return joined(state, level, pos);
		}
		return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbour, random);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (seat <= 0) {
			return super.useWithoutItem(state, level, pos, player, hit);
		}
		if (!level.isClientSide()) {
			SeatEntity.sit(level, pos, player, state.getValue(FACING), seat);
		}
		return InteractionResult.SUCCESS;
	}

	private BlockState joined(BlockState state, LevelReader level, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		return state.setValue(LEFT, joins(level.getBlockState(pos.relative(facing.getCounterClockWise())), facing))
				.setValue(RIGHT, joins(level.getBlockState(pos.relative(facing.getClockWise())), facing));
	}

	private boolean joins(BlockState other, Direction facing) {
		return other.getBlock() instanceof RowFurnitureBlock row && row.group.equals(group) && other.getValue(FACING) == facing;
	}
}
