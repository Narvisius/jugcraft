package io.github.jimbozoomer.jugcraft.lair.sentinel;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.lair.LairBosses;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A Rust Shard (docs/features/cydonian-sentinel.md): a splinter of rust crystal round a red-hot heart, loosed by the
 * Cydonian Sentinel (its Shard Volley, its Storm Shards and its Shard Burst) and by its Shard Sentries. It flies straight
 * at the speed it was loosed at and strikes the first player who may fight the Sentinel for its damage; it shatters on
 * anything solid (a standing stone is cover) and is gone after {@value #LIFE} ticks. It cannot be harmed, goes with the
 * Sentinel, and is never saved.
 */
public class RustShardEntity extends Entity implements GeoEntity {
	public static final double VOLLEY_SPEED = 0.8;
	public static final double STORM_SPEED = 0.9;
	public static final double BURST_SPEED = 0.7;
	public static final double SENTRY_SPEED = 0.35;
	public static final int LIFE = 80;
	private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.rust_shard.fly");
	private static final DustParticleOptions TRAIL = new DustParticleOptions(0xD8401A, 0.8F);

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID owner;
	private Vec3 velocity = Vec3.ZERO;
	private float damage;
	private int life;

	public RustShardEntity(EntityType<? extends RustShardEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	/** {@code sentinel} (or one of its sentries) looses a shard at {@code from}, flying at {@code velocity}, for {@code damage}. */
	public static RustShardEntity loose(ServerLevel level, CydonianSentinelEntity sentinel, Vec3 from, Vec3 velocity, float damage) {
		RustShardEntity shard = new RustShardEntity(JugcraftSentinel.RUST_SHARD, level);
		shard.owner = sentinel.getUUID();
		shard.velocity = velocity;
		shard.damage = damage;
		shard.snapTo(from.x, from.y, from.z, 0.0F, 0.0F);
		shard.faceAlong();
		level.addFreshEntity(shard);
		return shard;
	}

	public Vec3 velocity() {
		return velocity;
	}

	public float damage() {
		return damage;
	}

	private @Nullable CydonianSentinelEntity sentinel(ServerLevel level) {
		return owner != null && level.getEntity(owner) instanceof CydonianSentinelEntity found && found.isAlive() ? found : null;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		CydonianSentinelEntity sentinel = sentinel(level);
		if (sentinel == null || ++life > LIFE) {
			shatter(level, false);
			return;
		}
		Player hit = hitAlong(level);
		if (hit != null) {
			hit.hurtServer(level, damageSources().mobProjectile(this, sentinel), CydonianSentinelEntity.damage(damage));
			shatter(level, true);
			return;
		}
		if (!level.noCollision(this, getBoundingBox().move(velocity))) {
			shatter(level, true);
			return;
		}
		Vec3 next = position().add(velocity);
		setPos(next.x, next.y, next.z);
		faceAlong();
		if (tickCount % 3 == 0) {
			level.sendParticles(TRAIL, getX(), getY() + 0.15, getZ(), 1, 0.04, 0.04, 0.04, 0.0);
		}
	}

	/** Turns it to point the way it flies. */
	private void faceAlong() {
		if (velocity.x * velocity.x + velocity.z * velocity.z > 1.0E-6) {
			setYRot((float) (Math.toDegrees(Math.atan2(velocity.z, velocity.x)) - 90.0));
		}
	}

	/** The first player it strikes on its way: one who may fight the Sentinel. */
	private @Nullable Player hitAlong(ServerLevel level) {
		AABB sweep = getBoundingBox().expandTowards(velocity).inflate(0.2);
		for (Player player : level.getEntitiesOfClass(Player.class, sweep, Player::isAlive)) {
			if (LairBosses.eligible(player)) {
				return player;
			}
		}
		return null;
	}

	/** It shatters, in a spray of red dust (and a chime, if it struck something). */
	private void shatter(ServerLevel level, boolean struck) {
		level.sendParticles(TRAIL, getX(), getY() + 0.15, getZ(), 8, 0.15, 0.15, 0.15, 0.03);
		if (struck) {
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 0.6F, 1.4F);
		}
		discard();
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

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<RustShardEntity>("main", 0, test -> test.setAndContinue(FLY)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
