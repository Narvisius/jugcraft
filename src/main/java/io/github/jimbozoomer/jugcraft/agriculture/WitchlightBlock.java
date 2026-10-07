package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Witchlight Path Stake and the Hanging Witchlight (Halloween decorations batch 19, the Witchlight Lantern Path): a
 * purple-glass lantern with a gold cap on a knee-high stake, or on a chain from a ceiling. They wake as a player comes
 * ({@link Witchlights}). The wisp inside and the glow are drawn by the client (client/WitchlightRenderer.java).
 */
public class WitchlightBlock extends Block implements EntityBlock {
	/** How the lantern is held. */
	public enum Mount {
		STAKE, HANGING
	}

	private static final VoxelShape STAKE_SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 14.0, 11.0);
	private static final VoxelShape HANGING_SHAPE = Block.box(5.0, 2.0, 5.0, 11.0, 16.0, 11.0);
	private final Mount mount;

	public WitchlightBlock(Properties properties, Mount mount) {
		super(properties);
		this.mount = mount;
		registerDefaultState(stateDefinition.any().setValue(Witchlights.LIT, false).setValue(Witchlights.COLOUR, Witchlights.Colour.PURPLE));
	}

	public Mount mount() {
		return mount;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return mount == Mount.STAKE ? STAKE_SHAPE : HANGING_SHAPE;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		if (mount == Mount.HANGING) {
			return Block.canSupportCenter(level, pos.above(), Direction.DOWN) || level.getBlockState(pos.above()).is(Blocks.IRON_CHAIN);
		}
		return Block.canSupportCenter(level, pos.below(), Direction.UP);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().canSurvive(context.getLevel(), context.getClickedPos()) ? defaultBlockState() : null;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		Direction support = mount == Mount.HANGING ? Direction.UP : Direction.DOWN;
		return direction == support && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		return Witchlights.dye(level, pos, state, player, stack);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new WitchlightBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != JugcraftAgriculture.WITCHLIGHT_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((WitchlightBlockEntity) entity).serverTick((ServerLevel) tickLevel, pos, tickState);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(Witchlights.LIT, Witchlights.COLOUR);
	}
}
