package io.github.jimbozoomer.jugcraft.lair.tyrant;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.lair.LairBosses;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A gob of magma the Cinder Tyrant spits in his Ember Spit: it arcs from his jaws onto its mark and bursts where it lands
 * (or on the first player it strikes), {@value #DAMAGE} damage to every player within {@value #RADIUS} blocks, and leaves a
 * patch of fire there ({@link CinderTyrantEntity#firePatch}). Its blow is a projectile's, which Fire Resistance does not
 * stop; the fire it leaves is fire. It cannot be stopped or harmed, lasts at most {@value #LIFE} ticks, and is never saved.
 */
public class MagmaGobEntity extends Entity implements GeoEntity {
	public static final double SPEED = 0.8;
	public static final int LIFE = 80;
	public static final double GRAVITY = 0.04;
	public static final float DAMAGE = 8.0F;
	public static final double RADIUS = 1.5;
	/** Where his jaws are, ahead of his feet and over them (tools/cinder_tyrant_models.py MOUTH_Z and MOUTH_Y). */
	public static final double MOUTH_AHEAD = 2.75;
	public static final double MOUTH_UP = 0.75;
	private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("animation.magma_gob.spin");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID owner;
	private Vec3 velocity = Vec3.ZERO;
	private Vec3 mark = Vec3.ZERO;

	public MagmaGobEntity(EntityType<? extends MagmaGobEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	/** {@code tyrant} spits a gob of magma from his jaws onto {@code mark} (on the floor), in an arc. */
	public static MagmaGobEntity spit(ServerLevel level, CinderTyrantEntity tyrant, Vec3 mark) {
		MagmaGobEntity gob = new MagmaGobEntity(JugcraftTyrant.MAGMA_GOB, level);
		gob.owner = tyrant.getUUID();
		gob.mark = mark;
		Vec3 from = tyrant.jaws();
		double dx = mark.x - from.x;
		double dz = mark.z - from.z;
		double flat = Math.sqrt(dx * dx + dz * dz);
		double ticks = Math.max(8.0, flat / SPEED);
		gob.velocity = new Vec3(dx / ticks, (mark.y - from.y + 0.5 * GRAVITY * ticks * ticks) / ticks, dz / ticks);
		gob.snapTo(from.x, from.y, from.z, tyrant.getYRot(), 0.0F);
		level.addFreshEntity(gob);
		return gob;
	}

	public Vec3 mark() {
		return mark;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		CinderTyrantEntity tyrant = owner != null && level.getEntity(owner) instanceof CinderTyrantEntity found && found.isAlive() ? found : null;
		if (tyrant == null || tickCount > LIFE) {
			discard();
			return;
		}
		velocity = velocity.add(0.0, -GRAVITY, 0.0);
		Vec3 next = position().add(velocity);
		AABB sweep = getBoundingBox().expandTowards(velocity).inflate(0.2);
		boolean struck = !level.getEntitiesOfClass(ServerPlayer.class, sweep, LairBosses::eligible).isEmpty();
		if (struck || (next.y <= mark.y && velocity.y < 0.0)) {
			setPos(next.x, Math.max(mark.y, next.y), next.z);
			burst(level, tyrant);
			return;
		}
		setPos(next.x, next.y, next.z);
		level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.3, getZ(), 1, 0.1, 0.1, 0.1, 0.0);
		if (tickCount % 2 == 0) {
			level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.3, getZ(), 1, 0.1, 0.1, 0.1, 0.0);
		}
	}

	/** It bursts: {@value #DAMAGE} damage to every player within {@value #RADIUS} blocks, and a patch of fire where it lands. */
	public int burst(ServerLevel level, CinderTyrantEntity tyrant) {
		int hit = 0;
		for (ServerPlayer player : tyrant.nearby(level)) {
			if (player.position().distanceTo(position()) <= RADIUS
					&& player.hurtServer(level, damageSources().mobProjectile(this, tyrant), CinderTyrantEntity.damage(DAMAGE))) {
				hit++;
			}
		}
		tyrant.firePatch(level, new Vec3(getX(), tyrant.floorY(), getZ()));
		level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.2, getZ(), 12, 0.6, 0.2, 0.6, 0.0);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.3, getZ(), 8, 0.6, 0.3, 0.6, 0.02);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.5F, 0.6F);
		discard();
		return hit;
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
		controllers.add(new AnimationController<MagmaGobEntity>("main", 0, test -> test.setAndContinue(SPIN)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
