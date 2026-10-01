package io.github.jimbozoomer.jugcraft.weapons;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A grenade's blast. It is not a Minecraft explosion: it never breaks, moves or ignites a block, and only hurts living
 * things (not armor stands, item frames, paintings or dropped items), so it cannot grief a base. Damage falls off
 * with distance and walls shield from it, as for a real explosion; blast protection counts.
 */
public final class Blast {
	/** Reach of the blast, in blocks. */
	public static final double RADIUS = 4.0;
	/** Damage at the centre with nothing in the way (eight hearts); none at the edge. */
	public static final float DAMAGE = 16.0F;
	/** Hits weaker than this are ignored. */
	private static final float MIN_DAMAGE = 0.5F;

	private Blast() {
	}

	/**
	 * Sets off a blast at {@code center}. {@code direct} is the grenade and {@code owner} who threw or fired it (either
	 * may be null). Returns how many living things it hurt.
	 */
	public static int detonate(ServerLevel level, Vec3 center, @Nullable Entity direct, @Nullable Entity owner) {
		level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.SMOKE, center.x, center.y, center.z, 12, 0.6, 0.4, 0.6, 0.02);
		level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS,
				2.0F, 1.2F + level.getRandom().nextFloat() * 0.2F);
		DamageSource source = level.damageSources().explosion(direct, owner);
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(RADIUS),
				target -> target.isAlive() && !target.isSpectator() && target.getType() != EntityTypes.ARMOR_STAND);
		int hurt = 0;
		for (LivingEntity target : targets) {
			double distance = Math.sqrt(target.distanceToSqr(center));
			if (distance > RADIUS) {
				continue;
			}
			float strength = (float) (1.0 - distance / RADIUS) * ServerExplosion.getSeenPercent(center, target);
			if (DAMAGE * strength < MIN_DAMAGE) {
				continue;
			}
			if (target.hurtServer(level, source, DAMAGE * strength)) {
				// Vanilla knocks the target back from the grenade as it hurts it.
				hurt++;
			}
		}
		return hurt;
	}
}
