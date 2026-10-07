package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Mummy Sarcophagus: a painted case two blocks tall, standing up, gold and lapis on sandstone. Use it, or give it
 * a rising redstone signal, and its lid grinds open ({@link #OPEN}) and the mummy inside lurches out at you with its
 * arms raised, groaning; {@value #OPEN_TICKS} ticks later it shuffles back and the lid shuts. The lid and the mummy
 * are drawn by the client (client/MummySarcophagusRenderer.java, from the lower half's block entity).
 */
public class MummySarcophagusBlock extends TallDecorationBlock implements EntityBlock {
	public static final int OPEN_TICKS = 80;
	public static final float LID_DEGREES = 100.0F;
	/** How far the mummy lurches out, in pixels. */
	public static final float LURCH = 5.0F;
	/** How fast the lid swings (degrees a tick) and the mummy moves (pixels a tick). */
	public static final float LID_SPEED = 12.0F;
	public static final float LURCH_SPEED = 0.8F;
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	public MummySarcophagusBlock(Properties properties) {
		super(properties, Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0), Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0));
		registerDefaultState(defaultBlockState().setValue(OPEN, false).setValue(POWERED, false));
	}

	public static BlockPos lower(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
	}

	/** {@code current} moved toward {@code target} by at most {@code step}. */
	public static float approach(float current, float target, float step) {
		return current + Mth.clamp(target - current, -step, step);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new DecorationBlockEntity(JugcraftAgriculture.SARCOPHAGUS_ENTITY, pos, state) : null;
	}

	/** Opens the sarcophagus with a half at {@code pos}, if it is shut, and sets it to shut again. */
	public static boolean open(Level level, BlockPos pos, BlockState state) {
		BlockPos lower = lower(pos, state);
		BlockState base = level.getBlockState(lower);
		if (!(base.getBlock() instanceof MummySarcophagusBlock) || base.getValue(OPEN)) {
			return false;
		}
		setBoth(level, lower, base.setValue(OPEN, true));
		level.playSound(null, lower, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.6F, 0.5F);
		level.playSound(null, lower.above(), SoundEvents.ZOMBIE_AMBIENT, SoundSource.BLOCKS, 1.0F, 0.5F);
		level.scheduleTick(lower, base.getBlock(), OPEN_TICKS);
		return true;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && open(level, pos, state)) {
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.SUCCESS;
	}

	/** The mummy goes back and the lid shuts. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(HALF) == DoubleBlockHalf.LOWER && state.getValue(OPEN)) {
			setBoth(level, pos, state.setValue(OPEN, false));
			level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.8F, 0.6F);
		}
	}

	/** A rising redstone signal at either half opens it. */
	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean moved) {
		super.neighborChanged(state, level, pos, neighbor, orientation, moved);
		if (level.isClientSide()) {
			return;
		}
		BlockPos lower = lower(pos, state);
		BlockState base = level.getBlockState(lower);
		if (!base.is(this)) {
			return;
		}
		boolean powered = level.hasNeighborSignal(lower) || level.hasNeighborSignal(lower.above());
		if (powered == base.getValue(POWERED)) {
			return;
		}
		setBoth(level, lower, base.setValue(POWERED, powered));
		if (powered) {
			open(level, lower, level.getBlockState(lower));
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(OPEN, POWERED);
	}
}
