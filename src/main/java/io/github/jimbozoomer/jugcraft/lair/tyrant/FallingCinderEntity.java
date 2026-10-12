package io.github.jimbozoomer.jugcraft.lair.tyrant;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
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
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A cinder the Cinder Tyrant's roar shakes from the vent in his Cinder Rain: it drops from {@value #HEIGHT} blocks over its
 * mark, which glows on the floor below, and bursts where it lands, {@value #DAMAGE} damage to a player within
 * {@value #REACH} block of it, leaving a patch of fire ({@link CinderTyrantEntity#firePatch}). Its blow is a projectile's,
 * which Fire Resistance does not stop. It cannot be stopped or harmed and is never saved.
 */
public class FallingCinderEntity extends Entity implements GeoEntity {
	public static final double HEIGHT = 12.0;
	public static final double FALL_SPEED = 0.9;
	public static final double REACH = 1.0;
	public static final float DAMAGE = 6.0F;
	private static final RawAnimation FALL = RawAnimation.begin().thenLoop("animation.falling_cinder.fall");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID owner;
	private Vec3 mark = Vec3.ZERO;

	public FallingCinderEntity(EntityType<? extends FallingCinderEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	/** Drops a cinder from over {@code mark} (on the floor), for {@code tyrant}. */
	public static FallingCinderEntity drop(ServerLevel level, CinderTyrantEntity tyrant, Vec3 mark) {
		FallingCinderEntity cinder = new FallingCinderEntity(JugcraftTyrant.FALLING_CINDER, level);
		cinder.owner = tyrant.getUUID();
		cinder.mark = mark;
		cinder.snapTo(mark.x, mark.y + HEIGHT, mark.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
		level.addFreshEntity(cinder);
		return cinder;
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
		if (tyrant == null || tickCount > 60) {
			discard();
			return;
		}
		double y = getY() - FALL_SPEED;
		if (tickCount % 3 == 0) {
			level.sendParticles(ParticleTypes.FLAME, mark.x, mark.y + 0.05, mark.z, 2, 0.25, 0.0, 0.25, 0.0);
		}
		if (y <= mark.y) {
			setPos(mark.x, mark.y, mark.z);
			burst(level, tyrant);
			return;
		}
		setPos(mark.x, y, mark.z);
	}

	/** It bursts on the floor: {@value #DAMAGE} damage to each player within {@value #REACH} block, and a patch of fire. */
	public int burst(ServerLevel level, CinderTyrantEntity tyrant) {
		int hit = 0;
		for (ServerPlayer player : tyrant.nearby(level)) {
			double dx = player.getX() - mark.x;
			double dz = player.getZ() - mark.z;
			if (dx * dx + dz * dz <= (REACH + player.getBbWidth() / 2.0) * (REACH + player.getBbWidth() / 2.0)
					&& Math.abs(player.getY() - mark.y) < 2.5
					&& player.hurtServer(level, damageSources().mobProjectile(this, tyrant), CinderTyrantEntity.damage(DAMAGE))) {
				hit++;
			}
		}
		tyrant.firePatch(level, mark);
		level.sendParticles(ParticleTypes.LAVA, mark.x, mark.y + 0.2, mark.z, 8, 0.4, 0.2, 0.4, 0.0);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, mark.x, mark.y + 0.3, mark.z, 6, 0.4, 0.3, 0.4, 0.02);
		level.playSound(null, mark.x, mark.y, mark.z, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.0F, 1.0F);
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
		controllers.add(new AnimationController<FallingCinderEntity>("main", 0, test -> test.setAndContinue(FALL)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
