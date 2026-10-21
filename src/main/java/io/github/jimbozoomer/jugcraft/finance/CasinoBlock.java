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
 * A casino game in the world: a table (blackjack, craps, roulette) or a slot machine, placed from one item and made
 * of several blocks like the poker tables. {@link #PART} numbers the blocks ((k * depth + j) * width + i for block i
 * across, j away and k up); only the master (part 0) has the block entity that runs the game. The player who places it
 * owns it and is the house. Right-clicking any part opens the game.
 */
public class CasinoBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, 23);

	/** The games, each its own block, item and recipe (models from tools/exchange_casino.py). */
	public enum Game {
		BLACKJACK("blackjack_table", "Blackjack", 3, 2, 1, null),
		CRAPS("craps_table", "Craps", 4, 2, 1, null),
		ROULETTE("roulette_table", "American Roulette", 3, 2, 1, null),
		SLOT_FRUIT("slot_machine_classic", "Fruit Slots", 1, 1, 2, Slots.Machine.FRUIT),
		SLOT_CLOCKWORK("slot_machine_clockwork", "Clockwork Slots", 1, 1, 2, Slots.Machine.CLOCKWORK),
		SLOT_GEM("slot_machine_gem", "Gem Mine Slots", 1, 1, 2, Slots.Machine.GEM),
		SLOT_PROGRESSIVE("slot_machine_progressive", "Progressive Jackpot", 1, 1, 2, Slots.Machine.PROGRESSIVE),
		SLOT_NETHER("slot_machine_nether", "Nether Furnace", 1, 1, 2, Slots.Machine.NETHER),
		SLOT_TREASURE("slot_machine_treasure", "Sunken Treasure", 1, 1, 2, Slots.Machine.TREASURE),
		SLOT_CREEPER("slot_machine_creeper", "Creeper Blast", 1, 1, 2, Slots.Machine.CREEPER),
		SLOT_PUB("slot_machine_pub", "Pub Fruit", 1, 1, 2, Slots.Machine.PUB),
		ARCADE("casino_arcade", "Casino Arcade", 1, 1, 2, null),
		PLINKO("plinko_board", "Plinko", 2, 1, 2, null),
		PRIZE_WHEEL("prize_wheel", "Prize Wheel", 2, 1, 2, null),
		COIN_PUSHER("coin_pusher", "Coin Pusher", 2, 1, 2, null),
		BACCARAT("baccarat_table", "Baccarat", 3, 2, 1, null),
		LOTTERY("lottery_kiosk", "Lottery Kiosk", 1, 1, 2, null),
		DERBY("derby_table", "Derby Table", 4, 3, 2, null),
		SLOT_SHARK("slot_machine_shark", "Shark Frenzy", 1, 1, 2, Slots.Machine.SHARK),
		SLOT_DRAGON("slot_machine_dragon", "Golden Dragon", 1, 1, 2, Slots.Machine.DRAGON);

		public final String id;
		public final String title;
		public final int width;
		public final int depth;
		public final int height;
		public final Slots.Machine machine;

		Game(String id, String title, int width, int depth, int height, Slots.Machine machine) {
			this.id = id;
			this.title = title;
			this.width = width;
			this.depth = depth;
			this.height = height;
			this.machine = machine;
		}

		public int parts() {
			return width * depth * height;
		}

		public boolean slot() {
			return machine != null;
		}

		/** How brightly it lights its surroundings: machines glow like real ones, tables do not. */
		public int light() {
			if (slot()) {
				return this == SLOT_PROGRESSIVE || this == SLOT_NETHER ? 13 : 11;
			}
			return switch (this) {
				case ARCADE, PRIZE_WHEEL -> 12;
				case PLINKO, COIN_PUSHER -> 10;
				case LOTTERY -> 9;
				case DERBY -> 11;
				default -> 0;
			};
		}

		/** A machine (metal, mined with a pickaxe) rather than a wooden table. */
		public boolean machine() {
			return height > 1;
		}

		public static Game byId(int id) {
			return id >= 0 && id < values().length ? values()[id] : BLACKJACK;
		}
	}

	private final Game game;

	public CasinoBlock(Properties properties, Game game) {
		super(properties);
		this.game = game;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, 0));
	}

	public Game game() {
		return game;
	}

	public static BlockPos partPos(Game game, BlockPos master, Direction facing, int part) {
		int i = part % game.width;
		int j = (part / game.width) % game.depth;
		int k = part / (game.width * game.depth);
		return master.relative(facing.getClockWise(), i).relative(facing, j).above(k);
	}

	public static BlockPos masterPos(Game game, BlockPos pos, BlockState state) {
		Direction facing = state.getValue(FACING);
		int part = state.getValue(PART);
		int i = part % game.width;
		int j = (part / game.width) % game.depth;
		int k = part / (game.width * game.depth);
		return pos.relative(facing.getCounterClockWise(), i).relative(facing.getOpposite(), j).below(k);
	}

	public BlockPos masterPos(BlockPos pos, BlockState state) {
		return masterPos(game, pos, state);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(PART) == 0 ? new CasinoBlockEntity(pos, state) : null;
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel) || state.getValue(PART) != 0) {
			return null;
		}
		return createTickerHelper(type, ExchangeBlocks.CASINO_ENTITY,
				(tickLevel, pos, tickState, casino) -> casino.serverTick((ServerLevel) tickLevel));
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection();
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		for (int part = 1; part < game.parts(); part++) {
			BlockPos partPos = partPos(game, pos, facing, part);
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
		for (int part = 1; part < game.parts(); part++) {
			level.setBlock(partPos(game, pos, facing, part), state.setValue(PART, part), Block.UPDATE_ALL);
		}
		if (placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof CasinoBlockEntity casino) {
			casino.setOwner(player);
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (level.getBlockState(pos).is(this)) {
			return;
		}
		Direction facing = state.getValue(FACING);
		BlockPos master = masterPos(pos, state);
		for (int part = 0; part < game.parts(); part++) {
			BlockPos partPos = partPos(game, master, facing, part);
			BlockState other = level.getBlockState(partPos);
			if (!partPos.equals(pos) && other.is(this) && other.getValue(FACING) == facing && other.getValue(PART) == part) {
				level.removeBlock(partPos, false);
			}
		}
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && player instanceof ServerPlayer server
				&& level.getBlockEntity(masterPos(pos, state)) instanceof CasinoBlockEntity casino) {
			casino.open(server);
		}
		return InteractionResult.SUCCESS;
	}

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
