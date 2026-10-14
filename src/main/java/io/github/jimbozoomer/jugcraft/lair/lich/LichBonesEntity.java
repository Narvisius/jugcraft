package io.github.jimbozoomer.jugcraft.lair.lich;

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
 * A clump of the Crypt Lich's bones (docs/features/crypt-lich.md): his Bone Lanes burst a row of these up the middle of
 * each lane he calls, a clump every {@value CryptLichEntity#CLUMP_STEP} blocks, and his Grave Pull a ring of them round his
 * feet; they stand through the attack's active ticks, then sink for {@value #SINK} ticks and are gone. What the bones do
 * is decided as they burst ({@link CryptLichEntity#bones}, {@link CryptLichEntity#pullBurst}): the clumps are what players
 * see, and nothing else. They cannot be harmed, go with him, and are never saved.
 */
public class LichBonesEntity extends Entity implements GeoEntity {
	public static final int SINK = 10;
	private static final EntityDataAccessor<Boolean> SINKING = SynchedEntityData.defineId(LichBonesEntity.class, EntityDataSerializers.BOOLEAN);
	private static final RawAnimation STAND = RawAnimation.begin().then("animation.lich_bones.burst", LoopType.PLAY_ONCE)
			.thenLoop("animation.lich_bones.stand");
	private static final RawAnimation SINK_DOWN = RawAnimation.begin().thenPlayAndHold("animation.lich_bones.sink");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID owner;
	private int active;

	public LichBonesEntity(EntityType<? extends LichBonesEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	/** A clump of {@code lich}'s bones bursts up at {@code at} (on the floor) and stands {@code active} ticks. */
	public static LichBonesEntity burst(ServerLevel level, CryptLichEntity lich, Vec3 at, int active) {
		LichBonesEntity bones = new LichBonesEntity(JugcraftLich.LICH_BONES, level);
		bones.owner = lich.getUUID();
		bones.active = active;
		bones.snapTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
		level.addFreshEntity(bones);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y + 0.1, at.z, 4, 0.5, 0.05, 0.5, 0.02);
		return bones;
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
		boolean bound = owner != null && level.getEntity(owner) instanceof CryptLichEntity lich && lich.isAlive();
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
		controllers.add(new AnimationController<LichBonesEntity>("main", 0,
				test -> test.setAndContinue(test.animatable().sinking() ? SINK_DOWN : STAND)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
