package io.github.jimbozoomer.jugcraft.concordance;

import io.github.jimbozoomer.jugcraft.concordance.sign.Presentation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Kindled light: real world light (it lights blocks, stops mobs spawning and grows plants like any light) that the
 * server places and takes away again. It has no shape, no item and no drops; a block placed in its space replaces it.
 * <p>
 * Its lifetime is kept in the world itself, so it ends correctly through chunk unloads and restarts with nothing
 * remembered elsewhere: a Kindle's mote counts {@link #AGE} down one step every {@value #STEP_TICKS} ticks (scheduled
 * ticks are saved with the chunk) and goes out at zero. A lantern's {@link #TRAIL} light checks every
 * {@value #TRAIL_CHECK_TICKS} ticks whether a lit lantern is still carried beside it, and goes out when none is.
 * Keep the numbers equal to MOTE_LIGHT, TRAIL_LIGHT, MOTE_STEP_TICKS and TRAIL_CHECK_TICKS in tools/concordance.py.
 */
public class LumenMoteBlock extends Block {
	public static final int LIGHT = 14;
	public static final int TRAIL_LIGHT = 12;
	public static final int STEP_TICKS = 80;
	public static final int TRAIL_CHECK_TICKS = 10;
	public static final int MAX_STEPS = 15;
	/** Steps left before a Kindled mote goes out. */
	public static final IntegerProperty AGE = IntegerProperty.create("age", 0, MAX_STEPS);
	/** A lantern's following light rather than a Kindled mote. */
	public static final BooleanProperty TRAIL = BooleanProperty.create("trail");
	public LumenMoteBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(AGE, 0).setValue(TRAIL, false));
	}

	public static int light(BlockState state) {
		return state.getValue(TRAIL) ? TRAIL_LIGHT : LIGHT;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean movedByPiston) {
		super.onPlace(state, level, pos, old, movedByPiston);
		if (!old.is(this)) {
			level.scheduleTick(pos, this, state.getValue(TRAIL) ? TRAIL_CHECK_TICKS : STEP_TICKS);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(TRAIL)) {
			if (KindledLanternItem.litLanternNear(level, pos)) {
				level.scheduleTick(pos, this, TRAIL_CHECK_TICKS);
			} else {
				level.removeBlock(pos, false);
			}
			return;
		}
		int age = state.getValue(AGE);
		if (age <= 0) {
			level.removeBlock(pos, false);
			return;
		}
		// Only the age changes, not the light, so neighbours and the light engine have nothing to redo.
		level.setBlock(pos, state.setValue(AGE, age - 1), Block.UPDATE_CLIENTS);
		level.scheduleTick(pos, this, STEP_TICKS);
	}

	/** A slow sparkle so a mote can be found (client only; nothing is sent over the network). */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		int rate = state.getValue(TRAIL) ? 12 : 4;
		if (!Presentation.ambient(random, rate, rate * 4)) {
			return;
		}
		double x = pos.getX() + 0.3 + random.nextDouble() * 0.4;
		double y = pos.getY() + 0.3 + random.nextDouble() * 0.4;
		double z = pos.getZ() + 0.3 + random.nextDouble() * 0.4;
		level.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, Presentation.reducedMotion() ? 0.0 : 0.01, 0.0);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AGE, TRAIL);
	}
}
