package io.github.jimbozoomer.jugcraft.lair.dreadnought;

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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A hot rivet (docs/features/iron-dreadnought.md): one of the five the Iron Dreadnought's battery fires in a fan. It flies
 * straight at {@value #SPEED} blocks a tick and strikes the first player who may fight him in its way for
 * {@value IronDreadnoughtEntity#RIVET_DAMAGE} damage. Anything solid stops it (cover is cover); it is gone after
 * {@value #LIFE} ticks, goes with him, and is never saved.
 */
public class HotRivetEntity extends Entity implements GeoEntity {
	public static final double SPEED = 1.1;
	public static final int LIFE = 30;
	/** Where it leaves his battery: ahead of his feet, to his left and over them (tools/iron_dreadnought.py RIVET). */
	public static final double BATTERY_AHEAD = 1.25;
	public static final double BATTERY_SIDE = 2.0625;
	public static final double BATTERY_UP = 4.375;
	private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.hot_rivet.fly");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID owner;
	private Vec3 velocity = Vec3.ZERO;
	private int life;

	public HotRivetEntity(EntityType<? extends HotRivetEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	/** {@code dreadnought} fires a rivet from {@code from}, flying {@code way}. */
	public static HotRivetEntity fire(ServerLevel level, IronDreadnoughtEntity dreadnought, Vec3 from, Vec3 way) {
		HotRivetEntity rivet = new HotRivetEntity(JugcraftDreadnought.HOT_RIVET, level);
		rivet.owner = dreadnought.getUUID();
		rivet.velocity = way.normalize().scale(SPEED);
		rivet.snapTo(from.x, from.y - 0.2, from.z, 0.0F, 0.0F);
		rivet.faceAlong();
		level.addFreshEntity(rivet);
		return rivet;
	}

	public Vec3 velocity() {
		return velocity;
	}

	private @Nullable IronDreadnoughtEntity dreadnought(ServerLevel level) {
		return owner != null && level.getEntity(owner) instanceof IronDreadnoughtEntity found && found.isAlive() ? found : null;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		IronDreadnoughtEntity dreadnought = dreadnought(level);
		if (dreadnought == null || ++life > LIFE) {
			discard();
			return;
		}
		Player hit = hitAlong(level);
		if (hit != null) {
			hit.hurtServer(level, damageSources().mobProjectile(this, dreadnought), IronDreadnoughtEntity.damage(IronDreadnoughtEntity.RIVET_DAMAGE));
			spark(level);
			return;
		}
		Vec3 here = position().add(0.0, 0.2, 0.0);
		HitResult wall = level.clip(new ClipContext(here, here.add(velocity), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		if (wall.getType() == HitResult.Type.BLOCK) {
			Vec3 at = wall.getLocation();
			setPos(at.x, at.y - 0.2, at.z);
			spark(level);
			return;
		}
		Vec3 next = position().add(velocity);
		setPos(next.x, next.y, next.z);
		faceAlong();
		if (tickCount % 2 == 0) {
			level.sendParticles(ParticleTypes.SMALL_FLAME, getX(), getY() + 0.2, getZ(), 1, 0.02, 0.02, 0.02, 0.0);
		}
	}

	/** Turns it to face the way it flies. */
	private void faceAlong() {
		if (velocity.x * velocity.x + velocity.z * velocity.z > 1.0E-6) {
			setYRot((float) (Math.toDegrees(Math.atan2(velocity.z, velocity.x)) - 90.0));
		}
	}

	/** The first player who may fight him in its way this tick. */
	private @Nullable Player hitAlong(ServerLevel level) {
		AABB sweep = getBoundingBox().expandTowards(velocity).inflate(0.15);
		for (Player player : level.getEntitiesOfClass(Player.class, sweep, Player::isAlive)) {
			if (LairBosses.eligible(player)) {
				return player;
			}
		}
		return null;
	}

	/** It strikes: sparks, a ring of hot metal, and it is gone. */
	private void spark(ServerLevel level) {
		level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.2, getZ(), 2, 0.05, 0.05, 0.05, 0.0);
		level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 0.2, getZ(), 6, 0.1, 0.1, 0.1, 0.2);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.4F, 1.8F);
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
		controllers.add(new AnimationController<HotRivetEntity>("main", 0, test -> test.setAndContinue(FLY)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
