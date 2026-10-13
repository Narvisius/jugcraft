package io.github.jimbozoomer.jugcraft.lair.vesperine;

import io.github.jimbozoomer.jugcraft.weapons.HarvestBoon;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Vesper Scythe's pale crescent (its Harvest boon): it flies straight on along its wielder's look for
 * {@value HarvestBoon#CRESCENT_RANGE} blocks, through walls of air but not stone, striking each foe it passes once for
 * {@value HarvestBoon#CRESCENT_DAMAGE} damage as the wielder's blow. Drawn as its own particles, and never saved.
 */
public class ReapingCrescentEntity extends Entity {
	public static final double SPEED = 1.0;

	private @Nullable UUID owner;
	private Vec3 direction = Vec3.ZERO;
	private double travelled;
	private final Set<UUID> struck = new HashSet<>();

	public ReapingCrescentEntity(EntityType<? extends ReapingCrescentEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	/** {@code wielder} looses a crescent along their look. */
	public static ReapingCrescentEntity loose(ServerLevel level, LivingEntity wielder) {
		ReapingCrescentEntity crescent = new ReapingCrescentEntity(JugcraftVesperine.REAPING_CRESCENT, level);
		crescent.owner = wielder.getUUID();
		Vec3 look = wielder.getLookAngle();
		crescent.direction = new Vec3(look.x, 0.0, look.z).lengthSqr() < 1.0E-6 ? look : new Vec3(look.x, 0.0, look.z).normalize();
		Vec3 from = wielder.getEyePosition().add(crescent.direction.scale(0.8)).subtract(0.0, 0.5, 0.0);
		crescent.snapTo(from.x, from.y, from.z, wielder.getYRot(), 0.0F);
		level.addFreshEntity(crescent);
		level.playSound(null, from.x, from.y, from.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 0.5F);
		return crescent;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		Entity wielder = owner == null ? null : level.getEntity(owner);
		if (!(wielder instanceof LivingEntity living) || travelled >= HarvestBoon.CRESCENT_RANGE
				|| !level.noCollision(this, getBoundingBox().move(direction.scale(SPEED)).deflate(0.2))) {
			level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.3, getZ(), 8, 0.3, 0.2, 0.3, 0.02);
			discard();
			return;
		}
		travelled += SPEED;
		setPos(getX() + direction.x * SPEED, getY(), getZ() + direction.z * SPEED);
		DamageSource source = living instanceof net.minecraft.world.entity.player.Player player ? damageSources().playerAttack(player)
				: damageSources().mobAttack(living);
		for (LivingEntity foe : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.6), LivingEntity::isAlive)) {
			if (foe == living || !struck.add(foe.getUUID())) {
				continue;
			}
			if (living instanceof ServerPlayer player && !HarvestBoon.mayStrike(player, level, foe)) {
				continue;
			}
			foe.hurtServer(level, source, HarvestBoon.CRESCENT_DAMAGE);
		}
		Vec3 across = new Vec3(-direction.z, 0.0, direction.x);
		for (int i = -3; i <= 3; i++) {
			double bend = 0.25 * (1.0 - (i * i) / 9.0);
			Vec3 at = position().add(across.scale(i * 0.3)).add(direction.scale(bend));
			level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 0.3, at.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}
}
