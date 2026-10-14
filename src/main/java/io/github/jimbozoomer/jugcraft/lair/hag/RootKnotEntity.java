package io.github.jimbozoomer.jugcraft.lair.hag;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.LoopType;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.lair.FenLampBlock;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Root Knot (docs/features/mire-hag.md): a writhing knot of fen-roots that bursts up through the boardwalk ring as the
 * Mire Hag cries Lights Out, {@value #START} blocks along the ring from the lamp still burning nearest her foe, and drags
 * itself along the ring toward it at {@value #SPEED} blocks a tick. Cut it down ({@value #HEALTH} health) and the lamp
 * burns on; let it come within {@value #REACH} blocks of the lamp's post and the lamp goes out. Whoever hurts it has taken
 * part in her fight. It never strikes anyone, sinks when she falls, resets or is gone, drops nothing and is never saved.
 */
public class RootKnotEntity extends Monster implements GeoEntity {
	public static final float HEALTH = 30.0F;
	public static final double SPEED = 0.06;
	public static final double START = 10.0;
	public static final double REACH = 2.0;
	/** It crawls along the ring's middle, between its inner and outer edges. */
	public static final double RING = (LanternFen.RING_INNER + LanternFen.RING_OUTER) / 2.0;
	private static final RawAnimation CRAWL = RawAnimation.begin().then("animation.root_knot.burst", LoopType.PLAY_ONCE)
			.thenLoop("animation.root_knot.crawl");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID hag;
	private @Nullable BlockPos lamp;
	/** Its angle round the marsh's middle (radians), and which way round it crawls (+1 or -1). */
	private double angle;
	private double way = 1.0;

	public RootKnotEntity(EntityType<? extends RootKnotEntity> type, Level level) {
		super(type, level);
		xpReward = 0;
		noPhysics = true;
		setNoGravity(true);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.MOVEMENT_SPEED, 0.1);
	}

	/**
	 * A knot bursts up through the ring {@value #START} blocks along it from {@code lamp}, clockwise of it (seen from above)
	 * or anticlockwise as {@code clockwiseOfLamp} says, and crawls back round toward it, in {@code hag}'s service.
	 */
	public static RootKnotEntity burst(ServerLevel level, MireHagEntity hag, BlockPos lamp, boolean clockwiseOfLamp) {
		RootKnotEntity knot = new RootKnotEntity(JugcraftHag.ROOT_KNOT, level);
		knot.hag = hag.getUUID();
		knot.lamp = lamp.immutable();
		Vec3 centre = hag.centre();
		// Angles from east toward south: growing, they go clockwise seen from above.
		double lampAngle = Math.atan2(lamp.getZ() + 0.5 - centre.z, lamp.getX() + 0.5 - centre.x);
		knot.way = clockwiseOfLamp ? -1.0 : 1.0;
		knot.angle = lampAngle - knot.way * START / RING;
		Vec3 at = knot.onRing(centre, hag.origin());
		knot.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		knot.faceAlong();
		level.addFreshEntity(knot);
		level.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.2, at.z, 24, 0.6, 0.1, 0.6, 0.1);
		level.sendParticles(ParticleTypes.BUBBLE_POP, at.x, at.y + 0.2, at.z, 12, 0.6, 0.1, 0.6, 0.0);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 2.0F, 0.6F);
		return knot;
	}

	/** The lamp it crawls for. */
	public @Nullable BlockPos lamp() {
		return lamp;
	}

	/** Where it is on the ring at its angle, on the boardwalk. */
	private Vec3 onRing(Vec3 centre, BlockPos origin) {
		return new Vec3(centre.x + Math.cos(angle) * RING, origin.getY() + LanternFen.STAND, centre.z + Math.sin(angle) * RING);
	}

	private void faceAlong() {
		// The way it crawls: along the ring's tangent.
		double dx = -Math.sin(angle) * way;
		double dz = Math.cos(angle) * way;
		float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
		setYRot(yaw);
		setYBodyRot(yaw);
		setYHeadRot(yaw);
	}

	/** How far (flat) it is from its lamp's post. */
	public double toLamp() {
		return lamp == null ? Double.MAX_VALUE : Math.hypot(getX() - (lamp.getX() + 0.5), getZ() - (lamp.getZ() + 0.5));
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		MireHagEntity found = hag != null && level.getEntity(hag) instanceof MireHagEntity serving && serving.isAlive() ? serving : null;
		if (found == null || !(found.fighting() || found.phase() == MireHagEntity.Phase.DARKENING) || lamp == null
				|| !MireHagEntity.burning(level, lamp)) {
			sink(level, false);
			return;
		}
		setDeltaMovement(Vec3.ZERO);
		if (toLamp() <= REACH) {
			sink(level, true);
			return;
		}
		angle += way * SPEED / RING;
		Vec3 at = onRing(found.centre(), found.origin());
		setPos(at.x, at.y, at.z);
		faceAlong();
		if (tickCount % 4 == 0) {
			level.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.1, getZ(), 3, 0.4, 0.05, 0.4, 0.05);
		}
		if (tickCount % 20 == 0) {
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.ROOTED_DIRT_PLACE, SoundSource.HOSTILE, 1.0F, 0.6F);
		}
	}

	/**
	 * It sinks back through the boards and is gone; {@code reached}: it came to its lamp, which goes out (and everyone near
	 * her marsh is told).
	 */
	public void sink(ServerLevel level, boolean reached) {
		if (reached && lamp != null) {
			FenLampBlock.snuff(level, lamp);
			if (hag != null && level.getEntity(hag) instanceof MireHagEntity found) {
				found.tell(level, "message.jugcraft.mire_hag.knot_snuffs");
			}
		}
		level.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.2, getZ(), 16, 0.5, 0.1, 0.5, 0.1);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 1.2F, 0.5F);
		discard();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hurt = super.hurtServer(level, source, amount);
		if (hurt && source.getEntity() instanceof ServerPlayer player && hag != null && level.getEntity(hag) instanceof MireHagEntity found) {
			found.took(player, amount);
		}
		return hurt;
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level && hag != null && level.getEntity(hag) instanceof MireHagEntity found) {
			level.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.3, getZ(), 20, 0.5, 0.2, 0.5, 0.1);
			found.tell(level, "message.jugcraft.mire_hag.knot_cut");
		}
	}

	@Override
	public boolean canBeAffected(MobEffectInstance effect) {
		return !effect.is(MobEffects.POISON) && super.canBeAffected(effect);
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	@Override
	public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity entity) {
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	protected boolean canRide(Entity vehicle) {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean shouldDropExperience() {
		return false;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ROOTED_DIRT_BREAK;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ROOTED_DIRT_BREAK;
	}

	@Override
	public float getVoicePitch() {
		return 0.6F;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<RootKnotEntity>("main", 2, test -> test.setAndContinue(CRAWL)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
