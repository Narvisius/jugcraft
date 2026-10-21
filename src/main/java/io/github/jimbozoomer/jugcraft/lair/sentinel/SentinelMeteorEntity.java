package io.github.jimbozoomer.jugcraft.lair.sentinel;

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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A meteor of the Cydonian Sentinel's Starfall (docs/features/cydonian-sentinel.md): a lump of red rock, its heart burning,
 * that falls from {@value CydonianSentinelEntity#METEOR_HEIGHT} blocks over a mark onto it, landing as the Starfall's
 * wind-up ends. What it does is decided as it lands ({@link CydonianSentinelEntity#starfallStrike}): it is what players
 * see, and nothing else. It cannot be harmed, goes with the Sentinel, and is never saved.
 */
public class SentinelMeteorEntity extends Entity implements GeoEntity {
	private static final RawAnimation FALL = RawAnimation.begin().thenLoop("animation.sentinel_meteor.fall");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID owner;
	private Vec3 mark = Vec3.ZERO;
	private int fall = 1;

	public SentinelMeteorEntity(EntityType<? extends SentinelMeteorEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	/** A meteor of {@code sentinel}'s starts falling onto {@code mark}, to land {@code ticks} ticks from now. */
	public static SentinelMeteorEntity fall(ServerLevel level, CydonianSentinelEntity sentinel, Vec3 mark, int ticks) {
		SentinelMeteorEntity meteor = new SentinelMeteorEntity(JugcraftSentinel.SENTINEL_METEOR, level);
		meteor.owner = sentinel.getUUID();
		meteor.mark = mark;
		meteor.fall = Math.max(1, ticks);
		meteor.snapTo(mark.x, mark.y + CydonianSentinelEntity.METEOR_HEIGHT, mark.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
		level.addFreshEntity(meteor);
		return meteor;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		boolean bound = owner != null && level.getEntity(owner) instanceof CydonianSentinelEntity sentinel && sentinel.isAlive();
		if (!bound || tickCount >= fall) {
			discard();
			return;
		}
		double share = Math.min(1.0, tickCount / (double) fall);
		setPos(mark.x, mark.y + CydonianSentinelEntity.METEOR_HEIGHT * (1.0 - share), mark.z);
		if (tickCount % 2 == 0) {
			level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.8, getZ(), 2, 0.15, 0.3, 0.15, 0.01);
			level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.4, getZ(), 1, 0.1, 0.2, 0.1, 0.0);
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
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<SentinelMeteorEntity>("main", 0, test -> test.setAndContinue(FALL)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
