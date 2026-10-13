package io.github.jimbozoomer.jugcraft.lair.vesperine;

import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A Grief Bolt: a slow homing bolt of soul fire a skull looses at Vesperine's foe. It strikes for {@value #DAMAGE} damage
 * and Slowness I ({@value #SLOW_TICKS} ticks). Hit it and it flies back at whoever sent it, faster, for
 * {@value #REFLECTED_DAMAGE} damage, as the hitter's blow (it reaches Vesperine instead once its skull is gone). It
 * bursts on anything solid and fizzles after {@value #LIFE} ticks; it is never saved, and is drawn as its own particles.
 */
public class GriefBoltEntity extends Entity {
	public static final float DAMAGE = 6.0F;
	public static final int SLOW_TICKS = 60;
	public static final float REFLECTED_DAMAGE = 12.0F;
	public static final double SPEED = 0.35;
	public static final double REFLECTED_SPEED = 0.6;
	public static final double TURN = 0.15;
	public static final int LIFE = 160;

	private @Nullable UUID shooter;
	private @Nullable UUID target;
	private @Nullable UUID reflector;
	private Vec3 velocity = Vec3.ZERO;
	private int life;

	public GriefBoltEntity(EntityType<? extends GriefBoltEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	/** {@code skull} looses a bolt at {@code target}. */
	public static GriefBoltEntity loose(ServerLevel level, ReaperSkullEntity skull, Entity target) {
		GriefBoltEntity bolt = new GriefBoltEntity(JugcraftVesperine.GRIEF_BOLT, level);
		bolt.shooter = skull.getUUID();
		bolt.target = target.getUUID();
		Vec3 from = skull.position().add(0.0, skull.getBbHeight() * 0.4, 0.0);
		bolt.velocity = target.getBoundingBox().getCenter().subtract(from).normalize().scale(SPEED);
		bolt.snapTo(from.x, from.y, from.z, 0.0F, 0.0F);
		level.addFreshEntity(bolt);
		level.playSound(null, from.x, from.y, from.z, SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 1.0F, 0.6F);
		return bolt;
	}

	public boolean reflected() {
		return reflector != null;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		if (++life > LIFE) {
			burst(level);
			return;
		}
		Entity goal = target == null ? null : level.getEntity(target);
		if (reflected() && (goal == null || !goal.isAlive())) {
			// Its skull is gone: it flies back at her instead.
			goal = shooter == null ? null : level.getEntity(shooter);
			goal = goal instanceof ReaperSkullEntity skull ? skull.boss(level) : goal;
		}
		double speed = reflected() ? REFLECTED_SPEED : SPEED;
		if (goal != null && goal.isAlive()) {
			Vec3 want = goal.getBoundingBox().getCenter().subtract(position()).normalize().scale(speed);
			velocity = velocity.add(want.subtract(velocity).scale(TURN)).normalize().scale(speed);
		}
		Vec3 next = position().add(velocity);
		LivingEntity hit = hitAlong(level, next);
		if (hit != null) {
			strike(level, hit);
			return;
		}
		if (!level.noCollision(this, getBoundingBox().move(velocity))) {
			burst(level);
			return;
		}
		setPos(next.x, next.y, next.z);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.25, getZ(), 2, 0.06, 0.06, 0.06, 0.0);
		level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.25, getZ(), 1, 0.04, 0.04, 0.04, 0.0);
	}

	/** What it strikes on its way to {@code next}: a player who may fight her, or (reflected) her and her skulls. */
	private @Nullable LivingEntity hitAlong(ServerLevel level, Vec3 next) {
		AABB sweep = getBoundingBox().expandTowards(velocity).inflate(0.3);
		for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, sweep, LivingEntity::isAlive)) {
			if (reflected()) {
				if (living instanceof ReaperSkullEntity || living instanceof VesperineEntity) {
					return living;
				}
			} else if (living instanceof Player player && VesperineEntity.eligible(player)) {
				return living;
			}
		}
		return null;
	}

	private void strike(ServerLevel level, LivingEntity hit) {
		if (reflected()) {
			Player by = level.getPlayerByUUID(reflector);
			DamageSource source = by == null ? damageSources().magic() : damageSources().thrown(this, by);
			hit.hurtServer(level, source, REFLECTED_DAMAGE);
		} else {
			Entity from = shooter == null ? null : level.getEntity(shooter);
			DamageSource source = from instanceof LivingEntity skull ? damageSources().mobProjectile(this, skull) : damageSources().magic();
			if (hit.hurtServer(level, source, VesperineEntity.damage(DAMAGE))) {
				hit.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SLOW_TICKS, 0), from);
			}
		}
		burst(level);
	}

	private void burst(ServerLevel level) {
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.25, getZ(), 12, 0.2, 0.2, 0.2, 0.04);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.25, getZ(), 4, 0.15, 0.15, 0.15, 0.01);
		discard();
	}

	/** Struck by a player: it turns and flies back at whoever sent it. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (reflected() || !(source.getEntity() instanceof ServerPlayer player)) {
			return false;
		}
		reflector = player.getUUID();
		target = shooter;
		Vec3 back = player.getLookAngle();
		velocity = back.normalize().scale(REFLECTED_SPEED);
		life = 0;
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5F, 0.6F);
		return true;
	}

	@Override
	public boolean isPickable() {
		return true;
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
