package io.github.jimbozoomer.jugcraft.lair;

import io.github.jimbozoomer.jugcraft.lair.tyrant.SalamanderCharm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Molten Slag, the Cinder Kiln's (docs/features/cinder-kiln.md): the forge's pool, the slag falling over the forge's lip,
 * the heat channel and the crucible. It glows, and whoever stands in it burns, sneaking or not: {@value #DAMAGE} hot-floor
 * damage, as a magma block deals, and burning for {@value #BURN_SECONDS} seconds. A fire-immune creature, anyone under
 * Fire Resistance or holding a Salamander Charm, and a player who cannot be hurt stand in it unharmed. Feet sink into it a little: it is
 * {@value #TOP} pixels high underfoot, though it is drawn as a whole block.
 */
public class MoltenSlagBlock extends Block {
	public static final float DAMAGE = 1.0F;
	public static final int BURN_SECONDS = 3;
	public static final int TOP = 14;
	private static final VoxelShape UNDERFOOT = Block.box(0, 0, 0, 16, TOP, 16);

	public MoltenSlagBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return UNDERFOOT;
	}

	@Override
	public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
		if (level instanceof ServerLevel server && entity instanceof LivingEntity living) {
			burn(server, living);
		}
		super.stepOn(level, pos, state, entity);
	}

	/** Burns {@code living}, standing in the slag, unless the slag cannot harm it. Returns whether it did. */
	public static boolean burn(ServerLevel level, LivingEntity living) {
		if (!harms(living)) {
			return false;
		}
		living.hurtServer(level, level.damageSources().hotFloor(), DAMAGE);
		living.igniteForSeconds(BURN_SECONDS);
		return true;
	}

	/**
	 * Whether the slag harms {@code living}: not a fire-immune creature, nor anyone under Fire Resistance, holding a
	 * Salamander Charm ({@link SalamanderCharm}) or unhurtable.
	 */
	public static boolean harms(LivingEntity living) {
		if (living.fireImmune() || living.hasEffect(MobEffects.FIRE_RESISTANCE) || SalamanderCharm.warded(living)) {
			return false;
		}
		return !(living instanceof Player player) || !player.getAbilities().invulnerable;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!level.getBlockState(pos.above()).isAir()) {
			return;
		}
		if (random.nextInt(40) == 0) {
			level.addParticle(ParticleTypes.LAVA, pos.getX() + random.nextDouble(), pos.getY() + 1.0, pos.getZ() + random.nextDouble(), 0.0,
					0.0, 0.0);
			level.playLocalSound(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS,
					0.4F + random.nextFloat() * 0.3F, 0.6F + random.nextFloat() * 0.3F, false);
		}
		if (random.nextInt(12) == 0) {
			level.addParticle(ParticleTypes.SMOKE, pos.getX() + random.nextDouble(), pos.getY() + 1.0, pos.getZ() + random.nextDouble(), 0.0,
					0.04, 0.0);
		}
	}
}
