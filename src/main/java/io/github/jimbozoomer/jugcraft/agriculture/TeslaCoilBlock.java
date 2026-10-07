package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Tesla Coil: a mad scientist's coil two blocks tall, a copper winding on a riveted iron base under a polished
 * toroid. It runs on the electric network (cables connect to its base; {@link TeslaCoilBlockEntity}): switched on by
 * hand ({@link #ENABLED}) and with power, it hums and glows ({@link #ACTIVE}, light {@value #LIGHT}) and throws
 * harmless purple arcs to other running coils nearby, crackling, or into the air if it stands alone. The arcs are
 * drawn by the client (client/TeslaCoilRenderer.java).
 */
public class TeslaCoilBlock extends TallDecorationBlock implements EntityBlock {
	public static final int LIGHT = 8;
	public static final BooleanProperty ENABLED = BooleanProperty.create("enabled");
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	public TeslaCoilBlock(Properties properties) {
		super(properties, Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0), Block.box(2.0, 0.0, 2.0, 14.0, 15.0, 14.0));
		registerDefaultState(defaultBlockState().setValue(ENABLED, false).setValue(ACTIVE, false));
	}

	public static int light(BlockState state) {
		return state.getValue(ACTIVE) ? LIGHT : 0;
	}

	/** The lower half, which holds the power. */
	public static BlockPos lower(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
	}

	/** Sets whether the coil with its lower half at {@code lower} runs (both halves). */
	static void setActive(Level level, BlockPos lower, BlockState state, boolean active) {
		setBoth(level, lower, state.setValue(ACTIVE, active));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new TeslaCoilBlockEntity(pos, state) : null;
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != JugcraftAgriculture.TESLA_COIL_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((TeslaCoilBlockEntity) entity).serverTick((ServerLevel) tickLevel);
	}

	/** Arcs reach clients as block events, which the lower half's block entity records. */
	@Override
	protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
		super.triggerEvent(state, level, pos, id, param);
		BlockEntity entity = level.getBlockEntity(pos);
		return entity != null && entity.triggerEvent(id, param);
	}

	/** Switches it on or off. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			BlockPos lower = lower(pos, state);
			BlockState base = level.getBlockState(lower);
			if (!base.is(this)) {
				return InteractionResult.PASS;
			}
			boolean on = !base.getValue(ENABLED);
			setBoth(level, lower, base.setValue(ENABLED, on));
			level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4F, on ? 0.9F : 0.7F);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.tesla_coil." + (on ? "on" : "off")));
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.SUCCESS;
	}

	/** A running coil spits a spark from its toroid now and then. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(ACTIVE) && state.getValue(HALF) == DoubleBlockHalf.UPPER && random.nextInt(3) == 0) {
			double angle = random.nextDouble() * Math.PI * 2;
			level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5 + Math.cos(angle) * 0.4, pos.getY() + 0.85,
					pos.getZ() + 0.5 + Math.sin(angle) * 0.4, 0.0, 0.0, 0.0);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(ENABLED, ACTIVE);
	}
}
