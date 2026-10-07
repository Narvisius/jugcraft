package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Ghost hunting: where restless spirits ({@link RestlessSpirit}) come from, and what reveals them. Graves stir at night:
 * a gravestone or grave mound, on each of its random ticks (vanilla's, about once a minute at the default rate), raises
 * a spirit above it {@value #STIR_CHANCE} of the time, at night on the overworld clock, while mobs may spawn (the
 * {@code spawn_mobs} game rule) and the agriculture feature is on, and only while fewer than {@value #NEAR_CAP} spirits
 * are within {@value #NEAR_RANGE} blocks. A player holding a Spirit Lantern (in either hand) reveals every spirit within
 * {@value #REVEAL_RADIUS} blocks, to everyone. Random ticks only reach chunks near players, so this adds no work of its
 * own.
 */
public final class Spirits {
	public static final float STIR_CHANCE = 0.25F;
	public static final int NEAR_CAP = 3;
	public static final int NEAR_RANGE = 16;
	public static final int REVEAL_RADIUS = 12;

	private Spirits() {
	}

	/** Whether {@code player} holds a Spirit Lantern in either hand. */
	public static boolean holdsLantern(Player player) {
		return player.isHolding(JugcraftAgriculture.item("spirit_lantern"));
	}

	/** A grave's random tick: now and then, at night, a spirit rises from it. */
	public static void stir(ServerLevel level, BlockPos grave, RandomSource random) {
		stir(level, grave, random, 1.0F);
	}

	/** As {@link #stir(ServerLevel, BlockPos, RandomSource)}, {@code factor} times as often (a neglected headstone stirs more). */
	public static void stir(ServerLevel level, BlockPos grave, RandomSource random, float factor) {
		if (random.nextFloat() < STIR_CHANCE * factor && level.getGameRules().get(GameRules.SPAWN_MOBS)) {
			rise(level, grave, MourningAngelBlock.night(level));
		}
	}

	/**
	 * Raises a spirit over the grave at {@code grave} if it is {@code night}, the feature is on, fewer than
	 * {@value #NEAR_CAP} are near and there is room above the grave; returns it, or null.
	 */
	public static @Nullable RestlessSpirit rise(ServerLevel level, BlockPos grave, boolean night) {
		if (!night || !JugcraftConfig.isFeatureEnabled(JugcraftAgriculture.FEATURE)
				|| level.getEntitiesOfClass(RestlessSpirit.class, new AABB(grave).inflate(NEAR_RANGE)).size() >= NEAR_CAP) {
			return null;
		}
		BlockPos spot = grave.above();
		for (int i = 0; i < 2 && !level.getBlockState(spot).getCollisionShape(level, spot).isEmpty(); i++) {
			spot = spot.above();
		}
		if (!level.getBlockState(spot).getCollisionShape(level, spot).isEmpty()
				|| !level.getBlockState(spot.above()).getCollisionShape(level, spot.above()).isEmpty()) {
			return null;
		}
		RestlessSpirit spirit = JugcraftAgriculture.RESTLESS_SPIRIT.create(level, EntitySpawnReason.EVENT);
		if (spirit == null) {
			return null;
		}
		spirit.setHome(grave);
		spirit.snapTo(spot.getX() + 0.5, spot.getY() + 0.1, spot.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
		if (!level.addFreshEntity(spirit)) {
			return null;
		}
		// Only the grave shows it: a breath of soul light and a low moan, heard and seen by all.
		level.sendParticles(ParticleTypes.SOUL, spot.getX() + 0.5, spot.getY() + 0.2, spot.getZ() + 0.5, 6, 0.25, 0.1, 0.25, 0.01);
		level.playSound(null, grave, SoundEvents.GHAST_AMBIENT, SoundSource.NEUTRAL, 0.25F, 1.4F);
		return spirit;
	}
}
