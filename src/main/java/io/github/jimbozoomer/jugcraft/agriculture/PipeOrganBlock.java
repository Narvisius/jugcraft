package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Phantom Pipe Organ: a carved case {@value #WIDTH} blocks wide and {@value #HEIGHT} tall, a keyboard over a
 * pedalboard and a rank of tall pipes. It is one prop, like a large machine: every block of it is this block, {@link #PART}
 * saying which (counted along the case from its left as seen from behind, bottom row first); the bottom middle one is
 * the master, which alone has the {@link PipeOrganBlockEntity} and drops the organ. Breaking any block breaks it all.
 *
 * <p>Use any block of it, or give it a rising redstone signal, and it plays the opening of Bach's Toccata and Fugue in
 * D minor ({@link PipeOrganBlockEntity#TUNE}, public domain, arranged here for note-block sounds); use it again to stop.
 * Its keys go down by themselves as it plays (client/PipeOrganRenderer.java). At night it sometimes starts on its own
 * (a random tick, one time in {@value #PHANTOM_CHANCE}).
 */
public class PipeOrganBlock extends BaseEntityBlock {
	public static final int WIDTH = 3;
	public static final int HEIGHT = 2;
	public static final int MASTER = 1;
	public static final int PHANTOM_CHANCE = 4;
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, WIDTH * HEIGHT - 1);
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	/** On the master: whether it is playing (only then does it tick). */
	public static final BooleanProperty PLAYING = BooleanProperty.create("playing");
	/** On the master: whether any block of it has a redstone signal. */
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	/** The bottom row (the console, out to its keyboard) and the top row (the pipes), for an organ facing north. */
	private static final Map<Direction, VoxelShape> LOWER = shapes(0.0, 2.0, 16.0);
	private static final Map<Direction, VoxelShape> UPPER = shapes(0.0, 7.0, 15.5);

	public PipeOrganBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(PART, MASTER).setValue(FACING, Direction.NORTH).setValue(PLAYING, false)
				.setValue(POWERED, false));
	}

	/** A box from {@code z0} to the back, {@code height} tall, turned for each facing (the front faces the facing). */
	private static Map<Direction, VoxelShape> shapes(double y0, double z0, double height) {
		return Map.of(Direction.NORTH, Block.box(0.0, y0, z0, 16.0, height, 16.0), Direction.SOUTH, Block.box(0.0, y0, 0.0, 16.0, height, 16.0 - z0),
				Direction.EAST, Block.box(0.0, y0, 0.0, 16.0 - z0, height, 16.0), Direction.WEST, Block.box(z0, y0, 0.0, 16.0, height, 16.0));
	}

	public static int column(int part) {
		return part % WIDTH;
	}

	public static int row(int part) {
		return part / WIDTH;
	}

	/** Where part {@code part} of the organ with its master at {@code master}, facing {@code facing}, is. */
	public static BlockPos partPos(BlockPos master, Direction facing, int part) {
		return master.relative(facing.getClockWise(), column(part) - column(MASTER)).above(row(part));
	}

	/** Where the master of the organ this block belongs to is. */
	public static BlockPos masterPos(BlockPos pos, BlockState state) {
		int part = state.getValue(PART);
		return pos.relative(state.getValue(FACING).getClockWise(), column(MASTER) - column(part)).below(row(part));
	}

	public static @Nullable PipeOrganBlockEntity master(Level level, BlockPos pos, BlockState state) {
		return level.getBlockEntity(masterPos(pos, state)) instanceof PipeOrganBlockEntity organ ? organ : null;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(PART) == MASTER ? new PipeOrganBlockEntity(pos, state) : null;
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || state.getValue(PART) != MASTER || !state.getValue(PLAYING) || type != JugcraftAgriculture.PIPE_ORGAN_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((PipeOrganBlockEntity) entity).serverTick((ServerLevel) tickLevel, pos, tickState);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return (row(state.getValue(PART)) == 0 ? LOWER : UPPER).get(state.getValue(FACING));
	}

	/** Placed with its keyboard toward the player, its middle where they aimed: every block it needs must be free. */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection().getOpposite();
		Level level = context.getLevel();
		BlockPos master = context.getClickedPos();
		for (int part = 0; part < WIDTH * HEIGHT; part++) {
			BlockPos pos = partPos(master, facing, part);
			if (part != MASTER && (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)
					|| !level.getBlockState(pos).canBeReplaced(BlockPlaceContext.at(context, pos, Direction.UP)))) {
				return null;
			}
		}
		return defaultBlockState().setValue(FACING, facing).setValue(PART, MASTER);
	}

	/** Fills in the rest of the organ. */
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level.isClientSide()) {
			return;
		}
		Direction facing = state.getValue(FACING);
		for (int part = 0; part < WIDTH * HEIGHT; part++) {
			if (part != MASTER) {
				level.setBlock(partPos(pos, facing, part), state.setValue(PART, part), Block.UPDATE_ALL);
			}
		}
	}

	/** Breaking any block breaks the organ: the master drops it once, the rest go without drops. */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (level.getBlockState(pos).is(this)) {
			return; // Played or powered, not removed.
		}
		BlockPos master = masterPos(pos, state);
		Direction facing = state.getValue(FACING);
		if (state.getValue(PART) != MASTER) {
			BlockState masterState = level.getBlockState(master);
			if (masterState.is(this) && masterState.getValue(PART) == MASTER && masterState.getValue(FACING) == facing) {
				level.destroyBlock(master, true);
			}
			return;
		}
		for (int part = 0; part < WIDTH * HEIGHT; part++) {
			BlockPos partPos = partPos(master, facing, part);
			BlockState other = level.getBlockState(partPos);
			if (part != MASTER && other.is(this) && other.getValue(PART) == part && other.getValue(FACING) == facing) {
				level.removeBlock(partPos, false);
			}
		}
	}

	/** In creative, breaking any block takes the whole organ away without dropping it. */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && player.getAbilities().instabuild && state.getValue(PART) != MASTER) {
			BlockPos master = masterPos(pos, state);
			if (level.getBlockState(master).is(this)) {
				level.removeBlock(master, false);
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	/** Plays the organ, or stops it while it plays. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			BlockPos master = masterPos(pos, state);
			BlockState masterState = level.getBlockState(master);
			if (!masterState.is(this)) {
				return InteractionResult.PASS;
			}
			if (masterState.getValue(PLAYING)) {
				level.setBlock(master, masterState.setValue(PLAYING, false), Block.UPDATE_ALL);
				level.playSound(null, master, SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.5F, 0.6F);
			} else {
				play(level, master, masterState);
			}
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.SUCCESS;
	}

	/** A rising redstone signal at any block of it starts the tune. */
	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean moved) {
		super.neighborChanged(state, level, pos, neighbor, orientation, moved);
		if (level.isClientSide()) {
			return;
		}
		BlockPos master = masterPos(pos, state);
		BlockState masterState = level.getBlockState(master);
		if (!masterState.is(this) || masterState.getValue(PART) != MASTER) {
			return;
		}
		boolean powered = false;
		for (int part = 0; part < WIDTH * HEIGHT && !powered; part++) {
			powered = level.hasNeighborSignal(partPos(master, masterState.getValue(FACING), part));
		}
		if (powered == masterState.getValue(POWERED)) {
			return;
		}
		if (powered && !masterState.getValue(PLAYING)) {
			play(level, master, masterState.setValue(POWERED, true));
		} else {
			level.setBlock(master, masterState.setValue(POWERED, powered), Block.UPDATE_ALL);
		}
	}

	/** Starts the tune from the beginning (the master's state is {@code state}). */
	public static void play(Level level, BlockPos master, BlockState state) {
		level.setBlock(master, state.setValue(PLAYING, true), Block.UPDATE_ALL);
		if (level.getBlockEntity(master) instanceof PipeOrganBlockEntity organ) {
			organ.start();
		}
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return state.getValue(PART) == MASTER;
	}

	/** At night the organ sometimes plays by itself. */
	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!state.getValue(PLAYING) && MourningAngelBlock.night(level) && random.nextInt(PHANTOM_CHANCE) == 0) {
			play(level, pos, state);
		}
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
		builder.add(PART, FACING, PLAYING, POWERED);
	}
}
