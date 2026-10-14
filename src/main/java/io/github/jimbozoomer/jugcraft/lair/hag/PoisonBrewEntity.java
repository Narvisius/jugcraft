package io.github.jimbozoomer.jugcraft.lair.hag;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.lair.LairBosses;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
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
 * A flask of the Mire Hag's brew (docs/features/mire-hag.md): her Poison Brew lobs it from her hand onto where her foe
 * stood, in an arc, and it shatters where it lands (or on the first player it strikes) into a pool of venom
 * ({@link MireHagEntity#venomPool}). It hurts nobody itself. It cannot be stopped or harmed, lasts at most {@value #LIFE}
 * ticks, and is never saved.
 */
public class PoisonBrewEntity extends Entity implements GeoEntity {
	public static final double SPEED = 0.7;
	public static final int LIFE = 80;
	public static final double GRAVITY = 0.04;
	/** Where her hand is as she lobs it, ahead of her feet and over them (tools/mire_hag_models.py HAND_Z and HAND_Y). */
	public static final double HAND_AHEAD = 1.375;
	public static final double HAND_UP = 1.875;
	private static final DustParticleOptions VENOM = new DustParticleOptions(0x6DBA27, 1.0F);
	private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("animation.poison_brew.spin");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID owner;
	private Vec3 velocity = Vec3.ZERO;
	private Vec3 mark = Vec3.ZERO;

	public PoisonBrewEntity(EntityType<? extends PoisonBrewEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	/** {@code hag} lobs a flask from her hand onto {@code mark} (where her foe stood), in an arc. */
	public static PoisonBrewEntity lob(ServerLevel level, MireHagEntity hag, Vec3 mark) {
		PoisonBrewEntity brew = new PoisonBrewEntity(JugcraftHag.POISON_BREW, level);
		brew.owner = hag.getUUID();
		brew.mark = mark;
		Vec3 from = hag.hand();
		double dx = mark.x - from.x;
		double dz = mark.z - from.z;
		double flat = Math.sqrt(dx * dx + dz * dz);
		double ticks = Math.max(8.0, flat / SPEED);
		brew.velocity = new Vec3(dx / ticks, (mark.y - from.y + 0.5 * GRAVITY * ticks * ticks) / ticks, dz / ticks);
		brew.snapTo(from.x, from.y, from.z, hag.getYRot(), 0.0F);
		level.addFreshEntity(brew);
		return brew;
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
		MireHagEntity hag = owner != null && level.getEntity(owner) instanceof MireHagEntity found && found.isAlive() ? found : null;
		if (hag == null || tickCount > LIFE) {
			discard();
			return;
		}
		velocity = velocity.add(0.0, -GRAVITY, 0.0);
		Vec3 next = position().add(velocity);
		AABB sweep = getBoundingBox().expandTowards(velocity).inflate(0.2);
		boolean struck = !level.getEntitiesOfClass(ServerPlayer.class, sweep, LairBosses::eligible).isEmpty();
		if (struck || (next.y <= mark.y && velocity.y < 0.0)) {
			setPos(next.x, Math.max(mark.y, next.y), next.z);
			shatter(level, hag);
			return;
		}
		setPos(next.x, next.y, next.z);
		if (tickCount % 2 == 0) {
			level.sendParticles(VENOM, getX(), getY() + 0.2, getZ(), 1, 0.05, 0.05, 0.05, 0.0);
		}
	}

	/** It shatters: a pool of her venom spreads where it fell, at the height she aimed for. */
	public void shatter(ServerLevel level, MireHagEntity hag) {
		Vec3 at = new Vec3(getX(), mark.y, getZ());
		hag.venomPool(level, at);
		level.sendParticles(VENOM, at.x, at.y + 0.3, at.z, 24, 0.6, 0.2, 0.6, 0.0);
		level.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.2, at.z, 12, 0.6, 0.1, 0.6, 0.1);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.GLASS_HIT, SoundSource.HOSTILE, 1.5F, 0.7F);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.BOTTLE_EMPTY, SoundSource.HOSTILE, 1.5F, 0.6F);
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
		controllers.add(new AnimationController<PoisonBrewEntity>("main", 0, test -> test.setAndContinue(SPIN)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
