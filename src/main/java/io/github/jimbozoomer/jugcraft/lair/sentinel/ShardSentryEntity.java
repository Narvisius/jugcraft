package io.github.jimbozoomer.jugcraft.lair.sentinel;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.lair.LairBosses;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A Shard Sentry (docs/features/cydonian-sentinel.md): three fist-sized stones of red rock circling a red spark, that rise
 * from the Cydonian Sanctum's small craters when the Sentinel's shell cracks and at its Sentry Call. It rises for
 * {@value #RISE} ticks, unhurt, then floats {@value #HOVER} blocks over the floor after the nearest player, keeping
 * {@value #KEEP} away, and looses a slow shard ({@value #SHOT_DAMAGE}) every {@value #SHOT_EVERY} ticks at a player within
 * {@value #RANGE}. Whoever hurts one takes part in the Sentinel's fight. It crumbles when the Sentinel falls, resets or is
 * gone, drops nothing, gives no experience, and is never saved.
 */
public class ShardSentryEntity extends Monster implements GeoEntity {
	public static final float HEALTH = 16.0F;
	public static final double SPEED = 0.08;
	public static final double HOVER = 2.0;
	public static final double KEEP = 5.0;
	public static final int RISE = 20;
	public static final int SHOT_EVERY = 60;
	public static final float SHOT_DAMAGE = 3.0F;
	public static final double RANGE = 20.0;

	private static final EntityDataAccessor<Boolean> RISING = SynchedEntityData.defineId(ShardSentryEntity.class, EntityDataSerializers.BOOLEAN);
	private static final RawAnimation RISE_UP = RawAnimation.begin().thenPlayAndHold("animation.shard_sentry.rise");
	private static final RawAnimation HOVERING = RawAnimation.begin().thenLoop("animation.shard_sentry.hover");
	private static final DustParticleOptions SPARK = new DustParticleOptions(0xFF3A1A, 0.9F);

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID sentinel;
	private double floor;
	private int shot;
	private int lonely;

	public ShardSentryEntity(EntityType<? extends ShardSentryEntity> type, Level level) {
		super(type, level);
		xpReward = 0;
		setNoGravity(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.MOVEMENT_SPEED, SPEED)
				.add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
	}

	/** A Shard Sentry rises at {@code at} (a small crater's sunken floor), in {@code sentinel}'s service. */
	public static ShardSentryEntity rise(ServerLevel level, CydonianSentinelEntity sentinel, Vec3 at) {
		ShardSentryEntity sentry = new ShardSentryEntity(JugcraftSentinel.SHARD_SENTRY, level);
		sentry.sentinel = sentinel.getUUID();
		sentry.floor = sentinel.floorY();
		sentry.snapTo(at.x, sentinel.floorY() + HOVER, at.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
		sentry.entityData.set(RISING, true);
		sentry.setNoAi(true);
		level.addFreshEntity(sentry);
		level.sendParticles(SPARK, at.x, at.y + 0.3, at.z, 16, 0.4, 0.2, 0.4, 0.02);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 1.5F, 0.6F);
		return sentry;
	}

	public boolean rising() {
		return entityData.get(RISING);
	}

	/** The Sentinel it serves, if it is still about. */
	public @Nullable CydonianSentinelEntity sentinel(ServerLevel level) {
		return sentinel != null && level.getEntity(sentinel) instanceof CydonianSentinelEntity found && found.isAlive() ? found : null;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		if (rising() && tickCount >= RISE) {
			entityData.set(RISING, false);
			setNoAi(false);
		}
		CydonianSentinelEntity master = sentinel(level);
		if (tickCount % 20 == 0) {
			boolean serving = master != null && master.phase() != CydonianSentinelEntity.Phase.WAITING;
			lonely = serving ? 0 : lonely + 20;
			if (lonely > 40) {
				crumble(level);
				return;
			}
		}
		if (rising() || master == null) {
			setDeltaMovement(Vec3.ZERO);
			return;
		}
		Player target = nearest(level);
		drift(target);
		if (target != null && ++shot >= SHOT_EVERY) {
			shot = 0;
			loose(level, master, target);
		}
		if (tickCount % 6 == 0) {
			level.sendParticles(SPARK, getX(), getY() + 0.5, getZ(), 1, 0.1, 0.1, 0.1, 0.0);
		}
	}

	/** The nearest player who may fight the Sentinel, within {@value #RANGE}. */
	private @Nullable Player nearest(ServerLevel level) {
		Player best = null;
		double distance = RANGE * RANGE;
		for (Player player : level.players()) {
			double d = player.distanceToSqr(this);
			if (LairBosses.eligible(player) && d <= distance) {
				distance = d;
				best = player;
			}
		}
		return best;
	}

	/** Floats {@value #HOVER} over the floor toward a point {@value #KEEP} short of {@code target}. */
	private void drift(@Nullable Player target) {
		double up = Math.max(-0.1, Math.min(0.1, (floor + HOVER - getY()) * 0.2));
		if (target == null) {
			setDeltaMovement(0.0, up, 0.0);
			return;
		}
		Vec3 to = new Vec3(target.getX() - getX(), 0.0, target.getZ() - getZ());
		double length = to.length();
		Vec3 step = length <= KEEP ? Vec3.ZERO : to.scale(Math.min(SPEED, length - KEEP) / length);
		setDeltaMovement(step.x, up, step.z);
		setYRot((float) (Math.toDegrees(Math.atan2(to.z, to.x)) - 90.0));
	}

	/** It looses a slow shard at {@code target}. */
	public RustShardEntity loose(ServerLevel level, CydonianSentinelEntity master, Player target) {
		Vec3 from = position().add(0.0, 0.5, 0.0);
		Vec3 way = target.getBoundingBox().getCenter().subtract(from).normalize();
		RustShardEntity shard = RustShardEntity.loose(level, master, from.add(way.scale(0.6)), way.scale(RustShardEntity.SENTRY_SPEED), SHOT_DAMAGE);
		master.owns(shard);
		level.playSound(null, from.x, from.y, from.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.0F, 1.4F);
		return shard;
	}

	/** Falls apart into stones and dust. */
	public void crumble(ServerLevel level) {
		level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.5, getZ(), 10, 0.3, 0.3, 0.3, 0.02);
		level.sendParticles(SPARK, getX(), getY() + 0.5, getZ(), 8, 0.3, 0.3, 0.3, 0.02);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 1.0F, 1.2F);
		discard();
	}

	/** Unhurt while it rises; whoever hurts it takes part in the Sentinel's fight. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (rising()) {
			return false;
		}
		boolean hurt = super.hurtServer(level, source, amount);
		CydonianSentinelEntity master = sentinel(level);
		if (hurt && master != null && source.getEntity() instanceof ServerPlayer player) {
			master.took(player, amount);
		}
		return hurt;
	}

	@Override
	public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean shouldDropExperience() {
		return false;
	}

	@Override
	protected void doPush(Entity entity) {
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.STONE_HIT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.STONE_BREAK;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(RISING, false);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (sentinel != null) {
			output.store("sentinel", UUIDUtil.CODEC, sentinel);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		sentinel = input.read("sentinel", UUIDUtil.CODEC).orElse(null);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<ShardSentryEntity>("main", 4,
				test -> test.setAndContinue(test.animatable().rising() ? RISE_UP : HOVERING)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
