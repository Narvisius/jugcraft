package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Ember Bed (Halloween decorations batch 17): a block-high hearth of sooty stones round a bed of glowing embers, with
 * a draft hole in each side. It is a heat source for the Horned Skull Cauldron, the Bubbling Cauldron and the Cooking Pot
 * (the {@code jugcraft:heat_sources} block tag), gives light {@value #LIGHT}, crackles, and, like a magma block, burns
 * whoever walks on it without sneaking.
 */
public class EmberBedBlock extends Block {
	public static final int LIGHT = 9;

	public EmberBedBlock(Properties properties) {
		super(properties);
	}

	@Override
	public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
		if (level instanceof ServerLevel server && !entity.isSteppingCarefully() && entity instanceof LivingEntity living && !living.fireImmune()) {
			living.hurtServer(server, server.damageSources().hotFloor(), 1.0F);
		}
		super.stepOn(level, pos, state, entity);
	}

	/** Sparks spit from the draft holes, smoke curls off the top, and the embers crackle. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(3) == 0) {
			int side = random.nextInt(4);
			double x = pos.getX() + (side == 0 ? -0.02 : side == 1 ? 1.02 : 0.5 + (random.nextDouble() - 0.5) * 0.25);
			double z = pos.getZ() + (side == 2 ? -0.02 : side == 3 ? 1.02 : 0.5 + (random.nextDouble() - 0.5) * 0.25);
			level.addParticle(ParticleTypes.SMALL_FLAME, x, pos.getY() + 0.15 + random.nextDouble() * 0.15, z, 0.0, 0.01, 0.0);
		}
		if (random.nextInt(6) == 0 && !level.getBlockState(pos.above()).isSolid()) {
			level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.85, pos.getZ() + 0.3 + random.nextDouble() * 0.4,
					0.0, 0.02, 0.0);
		}
		if (random.nextInt(10) == 0) {
			level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS,
					0.6F + random.nextFloat() * 0.3F, 0.6F + random.nextFloat() * 0.6F, false);
		}
	}
}
