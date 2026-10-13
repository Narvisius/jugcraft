package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

/**
 * A Tatami (the kitchen and cooking expansion's slice 4; tools/rice.py TATAMI): a block of woven straw. Set against the
 * side of a tatami that has no partner yet, it pairs with it into one two-block mat (the owner's even and odd halves);
 * {@link #FACING} then points at the partner. Alone, it shows the owner's single mat. Sneaking sets one down alone, and a
 * tatami whose partner goes stands alone again.
 */
public class TatamiBlock extends HorizontalDirectionalBlock {
	public static final BooleanProperty PAIRED = BooleanProperty.create("paired");

	public TatamiBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PAIRED, false));
	}

	/** Whether {@code state} is a tatami with no partner. */
	private boolean alone(BlockState state) {
		return state.is(this) && !state.getValue(PAIRED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction side = context.getClickedFace();
		BlockPos against = context.getClickedPos().relative(side.getOpposite());
		if (side.getAxis().isHorizontal() && !context.isSecondaryUseActive() && alone(context.getLevel().getBlockState(against))) {
			return defaultBlockState().setValue(FACING, side.getOpposite()).setValue(PAIRED, true);
		}
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}

	/** A paired tatami's partner turns to face it. */
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (state.getValue(PAIRED)) {
			BlockPos partner = pos.relative(state.getValue(FACING));
			if (alone(level.getBlockState(partner))) {
				level.setBlock(partner, defaultBlockState().setValue(FACING, state.getValue(FACING).getOpposite()).setValue(PAIRED, true),
						Block.UPDATE_ALL);
			}
		}
	}

	/** A paired tatami whose partner is no longer facing back stands alone. */
	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (state.getValue(PAIRED) && direction == state.getValue(FACING)
				&& !(neighborState.is(this) && neighborState.getValue(PAIRED) && neighborState.getValue(FACING) == direction.getOpposite())) {
			return state.setValue(PAIRED, false);
		}
		return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, PAIRED);
	}
}
