package io.github.jimbozoomer.jugcraft.weapons;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A flashbang (batch 31): a blinding flash and a bang, and no damage. Players who can see it go blind for a few seconds
 * (less if they were looking away; not at all behind a gas mask's tinted lenses); mobs that can see it lose their target
 * and stagger, slowed and weakened. Walls shield from it.
 */
public final class Flash {
	/** A player looking away from the flash is blinded this fraction as long. */
	private static final int LOOKING_AWAY_DIVISOR = 3;

	private Flash() {
	}

	/** Sets off a flash at {@code center}; returns how many living things it dazzled. */
	public static int detonate(ServerLevel level, Vec3 center, @Nullable Entity owner) {
		level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 40, 0.3, 0.3, 0.3, 0.35);
		level.sendParticles(ParticleTypes.FIREWORK, center.x, center.y, center.z, 30, 0.2, 0.2, 0.2, 0.25);
		level.playSound(null, center.x, center.y, center.z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 3.0F, 1.6F);
		level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 1.8F);
		double radius = FieldChemistry.FLASH_RADIUS;
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
				target -> target.isAlive() && !target.isSpectator() && target.getType() != EntityTypes.ARMOR_STAND
						&& target.distanceToSqr(center) <= radius * radius);
		int dazzled = 0;
		for (LivingEntity target : targets) {
			if (ServerExplosion.getSeenPercent(center, target) <= 0.0F || !FieldChemistry.mayAffect(owner, target)) {
				continue;
			}
			if (target instanceof Player player) {
				if (player.getItemBySlot(EquipmentSlot.HEAD).is(FieldChemistry.GAS_MASK)) {
					continue;
				}
				Vec3 toFlash = center.subtract(player.getEyePosition()).normalize();
				boolean facing = player.getViewVector(1.0F).dot(toFlash) > 0.2;
				int ticks = facing ? FieldChemistry.FLASH_BLIND_TICKS : FieldChemistry.FLASH_BLIND_TICKS / LOOKING_AWAY_DIVISOR;
				player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks, 0));
				player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, ticks, 0));
			} else {
				if (target instanceof Mob mob) {
					mob.setTarget(null);
				}
				target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, FieldChemistry.FLASH_STUN_TICKS, 3));
				target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, FieldChemistry.FLASH_STUN_TICKS, 1));
			}
			dazzled++;
		}
		return dazzled;
	}
}
