package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Lightning Harness (Halloween decorations batch 19, the Reanimation Rig): a cage of copper coils hung from a ceiling
 * (or a chain) with two brass electrode arms on chains. It fires when a redstone signal of {@value #PULSE} or more
 * reaches it, when a running Tesla Coil within {@value #COIL_RANGE} blocks throws an arc, or when lightning strikes
 * within {@value #STRIKE_REACH} blocks of it; at most once a second ({@value #COOLDOWN_TICKS} ticks). Firing cracks
 * arcs between its electrodes and down to what lies below (the client draws them for {@value #ARC_TICKS} ticks), and
 * wakes the patient of a Lab Table up to {@value #TABLE_REACH} blocks below ({@link LabTableBlock#wake}).
 */
public class LightningHarnessBlock extends BaseEntityBlock {
	public static final int PULSE = 13;
	public static final int COIL_RANGE = 8;
	public static final int STRIKE_REACH = 4;
	public static final int TABLE_REACH = 4;
	public static final int CHECK_TICKS = 5;
	public static final int ARC_TICKS = 12;
	public static final int COOLDOWN_TICKS = 20;
	/** The block event that tells clients it fired; its parameter is how far down its arcs reach (0 for none). */
	public static final int EVENT_FIRE = 1;
	/** Whether it has a strong enough signal (it fires as the signal comes, not while it lasts). */
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	private static final VoxelShape SHAPE = Block.box(1.0, 4.0, 1.0, 15.0, 16.0, 15.0);

	public LightningHarnessBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(POWERED, false));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/** It hangs from the underside of a block or from a chain. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockState above = level.getBlockState(pos.above());
		return Block.canSupportCenter(level, pos.above(), Direction.DOWN) || above.is(Blocks.IRON_CHAIN);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState();
		return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		return direction == Direction.UP && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
	}

	/** A strong signal arriving fires it. */
	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean moved) {
		super.neighborChanged(state, level, pos, neighbor, orientation, moved);
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		boolean strong = level.getBestNeighborSignal(pos) >= PULSE;
		if (strong == state.getValue(POWERED)) {
			return;
		}
		level.setBlock(pos, state.setValue(POWERED, strong), Block.UPDATE_CLIENTS);
		if (strong && level.getBlockEntity(pos) instanceof LightningHarnessBlockEntity harness) {
			harness.fire(server, pos);
		}
	}

	/** How far below it the first Lab Table lies, 1 to {@value #TABLE_REACH} blocks, or 0 for none. */
	public static int tableBelow(BlockGetter level, BlockPos pos) {
		for (int down = 1; down <= TABLE_REACH; down++) {
			if (level.getBlockState(pos.below(down)).getBlock() instanceof LabTableBlock) {
				return down;
			}
		}
		return 0;
	}

	/**
	 * Fires the harness at {@code pos} now (the caller has checked its cooldown): arcs, a crack of thunder, and the
	 * patient below sits up.
	 */
	static void discharge(ServerLevel level, BlockPos pos) {
		int down = tableBelow(level, pos);
		level.blockEvent(pos, level.getBlockState(pos).getBlock(), EVENT_FIRE, down);
		level.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS, 0.7F, 1.4F + level.getRandom().nextFloat() * 0.3F);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 20, 0.35, 0.2, 0.35, 0.08);
		level.gameEvent(null, GameEvent.BLOCK_ACTIVATE, pos);
		if (down > 0) {
			BlockPos table = pos.below(down);
			LabTableBlock.wake(level, LabTableBlock.foot(table, level.getBlockState(table)));
		}
	}

	/** Clients mark when it fired and how far its arcs reach, for the renderer. */
	@Override
	protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
		if (id == EVENT_FIRE && level.getBlockEntity(pos) instanceof LightningHarnessBlockEntity harness) {
			harness.fired(level.getGameTime(), param);
			return true;
		}
		return super.triggerEvent(state, level, pos, id, param);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new LightningHarnessBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != JugcraftAgriculture.LIGHTNING_HARNESS_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((LightningHarnessBlockEntity) entity).serverTick((ServerLevel) tickLevel, pos);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(POWERED);
	}
}
