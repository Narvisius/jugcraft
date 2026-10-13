package io.github.jimbozoomer.jugcraft.lair.vesperine;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Vesperine's scythe, thrown: it spins out up to {@value VesperineEntity#THROW_RANGE} blocks (or until it meets
 * something solid) and back to her hand, striking each player it passes once each way for {@value #DAMAGE} damage. She
 * is unarmed, and takes a quarter more damage, until it is back. It cannot be stopped or harmed, and is never saved.
 */
public class ThrownScytheEntity extends Entity implements GeoEntity {
	public static final float DAMAGE = 10.0F;
	public static final double SPEED = 0.9;
	public static final double REACH = 1.5;
	public static final int LIFE = 200;
	private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("animation.thrown_scythe.spin");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID owner;
	private Vec3 direction = Vec3.ZERO;
	private double travelled;
	private boolean returning;
	private final Set<UUID> struckOut = new HashSet<>();
	private final Set<UUID> struckBack = new HashSet<>();

	public ThrownScytheEntity(EntityType<? extends ThrownScytheEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	/** {@code boss} throws her scythe from {@code from} along {@code aim}. */
	public static ThrownScytheEntity launch(ServerLevel level, VesperineEntity boss, Vec3 from, Vec3 aim) {
		ThrownScytheEntity thrown = new ThrownScytheEntity(JugcraftVesperine.THROWN_SCYTHE, level);
		thrown.owner = boss.getUUID();
		thrown.direction = aim.lengthSqr() < 1.0E-6 ? boss.facing() : aim.normalize();
		thrown.snapTo(from.x, from.y - 0.25, from.z, boss.getYRot(), 0.0F);
		level.addFreshEntity(thrown);
		return thrown;
	}

	public boolean returning() {
		return returning;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		VesperineEntity boss = owner != null && level.getEntity(owner) instanceof VesperineEntity found && found.isAlive() ? found : null;
		if (boss == null || tickCount > LIFE) {
			if (boss != null) {
				boss.catchScythe();
			}
			discard();
			return;
		}
		Vec3 step;
		if (!returning) {
			step = direction.scale(SPEED);
			travelled += SPEED;
			if (travelled >= VesperineEntity.THROW_RANGE || !level.noCollision(this, getBoundingBox().move(step).deflate(0.3))) {
				returning = true;
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.TRIDENT_RETURN, SoundSource.HOSTILE, 1.0F, 0.6F);
			}
		} else {
			Vec3 to = boss.hand().subtract(position());
			if (to.length() <= 1.5) {
				boss.catchScythe();
				discard();
				return;
			}
			step = to.normalize().scale(SPEED);
		}
		setPos(getX() + step.x, getY() + step.y, getZ() + step.z);
		Set<UUID> struck = returning ? struckBack : struckOut;
		DamageSource source = damageSources().mobAttack(boss);
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(REACH))) {
			if (VesperineEntity.eligible(player) && struck.add(player.getUUID())) {
				player.hurtServer(level, source, VesperineEntity.damage(DAMAGE));
			}
		}
		if (tickCount % 4 == 0) {
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.6F, 1.6F);
		}
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.25, getZ(), 1, 0.3, 0.1, 0.3, 0.0);
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
		controllers.add(new AnimationController<ThrownScytheEntity>("main", 0, test -> test.setAndContinue(SPIN)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
