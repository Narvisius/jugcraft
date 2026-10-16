package io.github.jimbozoomer.jugcraft.lair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A ground terminal of the Foundry Trench (docs/features/foundry-trench.md): a copper plate riveted into the pit's floor
 * before each pylon's breaker, where a routed discharge is earthed. A lair fixture that cannot be broken or used; it is
 * floor, and is walked on. {@link #LIVE} while it earths a discharge, it burns violet, gives light {@value #LIGHT}
 * (tools/lairs.py TERMINAL) and sparks.
 */
public class GroundTerminalBlock extends Block {
	public static final BooleanProperty LIVE = BooleanProperty.create("live");
	public static final int LIGHT = 8;

	public GroundTerminalBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LIVE, false));
	}

	/** The light the terminal gives in {@code state}. */
	public static int light(BlockState state) {
		return state.getValue(LIVE) ? LIGHT : 0;
	}

	/** Lights or darkens the terminal at {@code pos}; whether it was a terminal and it changed. */
	public static boolean setLive(Level level, BlockPos pos, boolean live) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof GroundTerminalBlock) || state.getValue(LIVE) == live) {
			return false;
		}
		level.setBlock(pos, state.setValue(LIVE, live), Block.UPDATE_ALL);
		return true;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(LIVE)) {
			level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.1 + random.nextDouble() * 0.8, pos.getY() + 1.05,
					pos.getZ() + 0.1 + random.nextDouble() * 0.8, 0.0, 0.05, 0.0);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIVE);
	}
}
