package io.github.jimbozoomer.jugcraft.lair.hag;

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
 * The Mire Hag's Mire Grasp (docs/features/mire-hag.md): six hands of mud reaching up out of the water in a ring round
 * where her foe stood. They rise as the grasp lands, clutch through the attack's active ticks, then sink for
 * {@value HagRootsEntity#SINK} ticks and are gone. What the grasp does is decided as it lands
 * ({@link MireHagEntity#grasp}): the hands are what players see. They cannot be harmed, go with her, and are never saved.
 */
public class MireGraspEntity extends Entity implements GeoEntity {
	private static final EntityDataAccessor<Boolean> SINKING = SynchedEntityData.defineId(MireGraspEntity.class, EntityDataSerializers.BOOLEAN);
	private static final RawAnimation HOLD = RawAnimation.begin().then("animation.mire_grasp.rise", LoopType.PLAY_ONCE)
			.thenLoop("animation.mire_grasp.hold");
	private static final RawAnimation SINK_DOWN = RawAnimation.begin().thenPlayAndHold("animation.mire_grasp.sink");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID owner;
	private int active;

	public MireGraspEntity(EntityType<? extends MireGraspEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	/** {@code hag}'s hands rise round {@code at} (on the water) and clutch for {@code active} ticks. */
	public static MireGraspEntity rise(ServerLevel level, MireHagEntity hag, Vec3 at, int active) {
		MireGraspEntity grasp = new MireGraspEntity(JugcraftHag.MIRE_GRASP, level);
		grasp.owner = hag.getUUID();
		grasp.active = active;
		grasp.snapTo(at.x, at.y - 0.2, at.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
		level.addFreshEntity(grasp);
		MireHagEntity.ring(level, at, MireHagEntity.GRASP_RADIUS, ParticleTypes.SPLASH, 18);
		return grasp;
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
		boolean bound = owner != null && level.getEntity(owner) instanceof MireHagEntity hag && hag.isAlive();
		if (!bound || tickCount > active + HagRootsEntity.SINK) {
			discard();
			return;
		}
		if (tickCount > active && !sinking()) {
			entityData.set(SINKING, true);
		}
		if (!sinking() && tickCount % 6 == 0) {
			level.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.3, getZ(), 4, MireHagEntity.GRASP_RADIUS * 0.7, 0.05,
					MireHagEntity.GRASP_RADIUS * 0.7, 0.05);
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
		controllers.add(new AnimationController<MireGraspEntity>("main", 0,
				test -> test.setAndContinue(test.animatable().sinking() ? SINK_DOWN : HOLD)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
