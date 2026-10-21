package io.github.jimbozoomer.jugcraft.lair.sentinel;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.LoopType;
import com.geckolib.util.GeckoLibUtil;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A spike of red rock (docs/features/cydonian-sentinel.md): the Cydonian Sentinel's Quake Line bursts a row of these up out
 * of the crater's floor, one every {@value CydonianSentinelEntity#QUAKE_STEP} blocks; they stand through the attack's
 * active ticks, then crumble back down for {@value #SINK} ticks and are gone. What the spikes do is decided as they burst
 * ({@link CydonianSentinelEntity#quakeStrike}): they are what players see, and nothing else. They cannot be harmed, go
 * with it, and are never saved.
 */
public class QuakeSpikeEntity extends Entity implements GeoEntity {
	public static final int SINK = 10;
	private static final EntityDataAccessor<Boolean> SINKING = SynchedEntityData.defineId(QuakeSpikeEntity.class, EntityDataSerializers.BOOLEAN);
	private static final RawAnimation STAND = RawAnimation.begin().then("animation.quake_spike.burst", LoopType.PLAY_ONCE)
			.thenLoop("animation.quake_spike.stand");
	private static final RawAnimation SINK_DOWN = RawAnimation.begin().thenPlayAndHold("animation.quake_spike.sink");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID owner;
	private int active;

	public QuakeSpikeEntity(EntityType<? extends QuakeSpikeEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	/** A spike of {@code sentinel}'s bursts up at {@code at} (on the floor) and stands {@code active} ticks. */
	public static QuakeSpikeEntity burst(ServerLevel level, CydonianSentinelEntity sentinel, Vec3 at, int active) {
		QuakeSpikeEntity spike = new QuakeSpikeEntity(JugcraftSentinel.QUAKE_SPIKE, level);
		spike.owner = sentinel.getUUID();
		spike.active = active;
		spike.snapTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
		level.addFreshEntity(spike);
		level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, at.x, at.y + 0.2, at.z, 2, 0.4, 0.05, 0.4, 0.01);
		return spike;
	}

	public boolean sinking() {
		return entityData.get(SINKING);
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		boolean bound = owner != null && level.getEntity(owner) instanceof CydonianSentinelEntity sentinel && sentinel.isAlive();
		if (!bound || tickCount > active + SINK) {
			discard();
			return;
		}
		if (tickCount > active && !sinking()) {
			entityData.set(SINKING, true);
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
		builder.define(SINKING, false);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<QuakeSpikeEntity>("main", 0,
				test -> test.setAndContinue(test.animatable().sinking() ? SINK_DOWN : STAND)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
