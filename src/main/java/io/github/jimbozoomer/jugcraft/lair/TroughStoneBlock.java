package io.github.jimbozoomer.jugcraft.lair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Trough Stone, the Cinder Kiln's quench troughs (docs/features/cinder-kiln.md): paving sunk a little below the floor
 * ({@value #TOP} pixels high), in a strip from under each sluice gate toward the bowl's middle. While its sluice is open
 * it is {@link #FLOODED} ({@link SluiceGateBlock}), and the water puts out anyone burning who stands in it.
 */
public class TroughStoneBlock extends Block {
	public static final BooleanProperty FLOODED = BooleanProperty.create("flooded");
	public static final int TOP = 13;
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, TOP, 16);

	public TroughStoneBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FLOODED, false));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected boolean useShapeForLightOcclusion(BlockState state) {
		return true;
	}

	@Override
	public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
		if (state.getValue(FLOODED) && level instanceof ServerLevel server) {
			douse(server, pos, entity);
		}
		super.stepOn(level, pos, state, entity);
	}

	/** Puts out {@code entity} standing in a flooded trough, with a hiss of steam. Returns whether it was burning. */
	public static boolean douse(ServerLevel level, BlockPos pos, Entity entity) {
		if (entity.getRemainingFireTicks() <= 0) {
			return false;
		}
		entity.setRemainingFireTicks(0);
		level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.5F, 1.8F);
		level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.3, entity.getZ(), 6, 0.25, 0.1, 0.25, 0.02);
		return true;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(FLOODED) && random.nextInt(6) == 0) {
			level.addParticle(ParticleTypes.SPLASH, pos.getX() + random.nextDouble(), pos.getY() + 0.95, pos.getZ() + random.nextDouble(), 0.0,
					0.0, 0.0);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FLOODED);
	}
}
