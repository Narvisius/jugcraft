package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A plant hanging in strands (tools/plants.py kind "hanging"), the graveyard flora's shroud moss: it hangs from leaves,
 * from the sturdy underside of a block, or from more of itself, and falls when what holds it goes. The lowest block of a
 * strand is its tip (a shorter, tapering look). Bone meal lengthens a strand by a block, up to {@link #maxLength} blocks.
 */
public class HangingPlantBlock extends Block implements BonemealableBlock {
	public static final BooleanProperty TIP = BlockStateProperties.TIP;
	private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);
	private static final VoxelShape TIP_SHAPE = Block.box(1.0, 4.0, 1.0, 15.0, 16.0, 15.0);
	private final int maxLength;

	public HangingPlantBlock(Properties properties, int maxLength) {
		super(properties);
		this.maxLength = maxLength;
		registerDefaultState(stateDefinition.any().setValue(TIP, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(TIP);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(TIP) ? TIP_SHAPE : SHAPE;
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return true;
	}

	/** Held by leaves, a sturdy underside or more of itself above. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockPos above = pos.above();
		BlockState holder = level.getBlockState(above);
		return holder.is(this) || holder.is(BlockTags.LEAVES) || holder.isFaceSturdy(level, above, Direction.DOWN);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState().setValue(TIP, !context.getLevel().getBlockState(context.getClickedPos().below()).is(this));
		return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (direction == Direction.UP && !state.canSurvive(level, pos)) {
			ticks.scheduleTick(pos, this, 1);
		}
		return direction == Direction.DOWN ? state.setValue(TIP, !neighborState.is(this)) : state;
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!state.canSurvive(level, pos)) {
			level.destroyBlock(pos, true);
		}
	}

	/** How many blocks long the strand through `pos` is, and where its tip is. */
	private BlockPos tip(BlockGetter level, BlockPos pos) {
		BlockPos at = pos;
		while (level.getBlockState(at.below()).is(this)) {
			at = at.below();
		}
		return at;
	}

	private int length(BlockGetter level, BlockPos pos) {
		BlockPos top = pos;
		while (level.getBlockState(top.above()).is(this)) {
			top = top.above();
		}
		return top.getY() - tip(level, pos).getY() + 1;
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
		return level.getBlockState(tip(level, pos).below()).isAir() && length(level, pos) < maxLength;
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		return true;
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		BlockPos below = tip(level, pos).below();
		if (level.getBlockState(below).isAir() && length(level, pos) < maxLength) {
			level.setBlockAndUpdate(below, defaultBlockState().setValue(TIP, true));
		}
	}
}
