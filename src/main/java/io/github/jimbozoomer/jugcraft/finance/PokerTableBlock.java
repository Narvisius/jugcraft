package io.github.jimbozoomer.jugcraft.finance;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A poker table: 4 blocks wide, 2 deep and 1 high, placed from one item. The player placing it stands at its near long
 * side; it reaches 4 blocks to their right and 2 away. {@link #PART} numbers the blocks (part = depth * 4 + across);
 * only the master (part 0) has the block entity that runs the game. Right-clicking any part opens the table's overlay.
 * Breaking any part removes the whole table, drops one item and cashes everyone out.
 */
public class PokerTableBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final int WIDTH = 4;
	public static final int DEPTH = 2;
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, WIDTH * DEPTH - 1);

	/** The three designs; each is its own block with its own item and recipe. */
	public enum Design {
		OVAL("poker_table_oval", "The Gilded Oval"),
		SALOON("poker_table_saloon", "The Saloon"),
		CLOCKWORK("poker_table_clockwork", "The Clockwork Dealer");

		public final String id;
		public final String title;

		Design(String id, String title) {
			this.id = id;
			this.title = title;
		}
	}

	private final Design design;

	public PokerTableBlock(Properties properties, Design design) {
		super(properties);
		this.design = design;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, 0));
	}

	public Design design() {
		return design;
	}

	/** Where a part sits, from the master: {@code across} blocks to the right, {@code depth} blocks away. */
	public static BlockPos partPos(BlockPos master, Direction facing, int part) {
		return master.relative(facing.getClockWise(), part % WIDTH).relative(facing, part / WIDTH);
	}

	public static BlockPos masterPos(BlockPos pos, BlockState state) {
		Direction facing = state.getValue(FACING);
		int part = state.getValue(PART);
		return pos.relative(facing.getCounterClockWise(), part % WIDTH).relative(facing.getOpposite(), part / WIDTH);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(PART) == 0 ? new PokerTableBlockEntity(pos, state) : null;
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel) || state.getValue(PART) != 0) {
			return null;
		}
		return createTickerHelper(type, ExchangeBlocks.POKER_TABLE_ENTITY,
				(tickLevel, pos, tickState, table) -> table.serverTick((ServerLevel) tickLevel));
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection();
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		for (int part = 1; part < WIDTH * DEPTH; part++) {
			BlockPos partPos = partPos(pos, facing, part);
			if (context.getPlayer() != null && !LandRegistry.mayBuild(context.getPlayer(), level, partPos)
                    || level.isOutsideBuildHeight(partPos) || !level.getWorldBorder().isWithinBounds(partPos)
					|| !level.getBlockState(partPos).canBeReplaced(BlockPlaceContext.at(context, partPos, Direction.UP))) {
				return null;
			}
		}
		return defaultBlockState().setValue(FACING, facing).setValue(PART, 0);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level.isClientSide()) {
			return;
		}
		Direction facing = state.getValue(FACING);
		for (int part = 1; part < WIDTH * DEPTH; part++) {
			level.setBlock(partPos(pos, facing, part), state.setValue(PART, part), Block.UPDATE_ALL);
		}
		if (placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof PokerTableBlockEntity table) {
			table.setOwner(player);
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (level.getBlockState(pos).is(this)) {
			return;
		}
		// Remove the other parts without drops: the part that was broken already dropped the item.
		Direction facing = state.getValue(FACING);
		BlockPos master = masterPos(pos, state);
		for (int part = 0; part < WIDTH * DEPTH; part++) {
			BlockPos partPos = partPos(master, facing, part);
			BlockState other = level.getBlockState(partPos);
			if (!partPos.equals(pos) && other.is(this) && other.getValue(FACING) == facing && other.getValue(PART) == part) {
				level.removeBlock(partPos, false);
			}
		}
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && player instanceof ServerPlayer server
				&& level.getBlockEntity(masterPos(pos, state)) instanceof PokerTableBlockEntity table) {
			table.open(server);
		}
		return InteractionResult.SUCCESS;
	}

	/** Rotating one part alone would tear the table apart, so structures leave it as placed. */
	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state;
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, PART);
	}
}
