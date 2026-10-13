package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A bundle of herbs hung to dry (garden crops, herbs and spices, part b; tools/herbs.py BUNDLE): four sprigs of one herb
 * tied with string, hung from the underside of a block (a beam, a ceiling, anything a lantern could hang from). On each
 * random tick it dries one time in {@value #DRY_CHANCE}, turning {@link #DRIED}; it drops if its block goes. Broken fresh
 * it gives the bundle back, broken dried it gives four Dried Herbs (its loot table): drying changes the herbs, not how
 * many.
 */
public class HerbBundleBlock extends Block {
	public static final BooleanProperty DRIED = BooleanProperty.create("dried");
	/** One random tick in this many dries it: about six minutes on average at the default tick speed. */
	public static final int DRY_CHANCE = 5;
	private static final VoxelShape SHAPE = Block.box(5.0, 2.0, 5.0, 11.0, 16.0, 11.0);

	public HerbBundleBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(DRIED, false));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState();
		return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return Block.canSupportCenter(level, pos.above(), Direction.DOWN);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (direction == Direction.UP && !state.canSurvive(level, pos)) {
			return Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return !state.getValue(DRIED);
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!state.getValue(DRIED) && random.nextInt(DRY_CHANCE) == 0) {
			dry(level, pos, state);
		}
	}

	/** Dries the bundle at {@code pos} now (what a random tick does one time in {@value #DRY_CHANCE}). */
	public static void dry(ServerLevel level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state.setValue(DRIED, true), Block.UPDATE_CLIENTS);
		level.gameEvent(null, GameEvent.BLOCK_CHANGE, pos);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(DRIED);
	}
}
