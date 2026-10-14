package io.github.jimbozoomer.jugcraft.lair.hag;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.util.GeckoLibUtil;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A Likeness of the Mire Hag (docs/features/mire-hag.md): a shape of marsh-mist that looks as she does in every way, her
 * model and her sheet, her name, her health; it plays what she plays, holds still when she strikes and faces her foe as
 * she does, and walks in its own place round her foe as she walks. It casts no shadow and leaves no ripple in the water,
 * and it never hurts anyone. Anything striking it bursts it into marsh gas (whoever struck it has taken part in her fight);
 * in a burning lamp's light it gutters and fades ({@link MireHagEntity}). It goes with her, and is never saved.
 */
public class HagLikenessEntity extends Monster implements GeoEntity {
	private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(HagLikenessEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> LANTERN = SynchedEntityData.defineId(HagLikenessEntity.class, EntityDataSerializers.INT);

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID hag;
	private int slot = 1;
	/** Where it is walking to, looked for again every {@value #LOOK_EVERY} ticks. */
	private @Nullable Vec3 spot;
	/** Sidestepping round a stump or a post, as she does: ticks left, and which way. */
	private int sidestep;
	private boolean sidestepLeft;
	private static final int LOOK_EVERY = 5;
	/** How near the edge of a burning lamp's light it walks of its own accord: it keeps this far off. */
	public static final double SHY = 0.5;

	public HagLikenessEntity(EntityType<? extends HagLikenessEntity> type, Level level) {
		super(type, level);
		xpReward = 0;
		setNoGravity(true);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, MireHagEntity.HEALTH).add(Attributes.ARMOR, MireHagEntity.ARMOR)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.STEP_HEIGHT, MireHagEntity.STEP);
	}

	/** A Likeness forms out of the mist at {@code at}, walking in {@code slot} round {@code hag}'s foe. */
	public static HagLikenessEntity form(ServerLevel level, MireHagEntity hag, Vec3 at, int slot) {
		HagLikenessEntity likeness = new HagLikenessEntity(JugcraftHag.HAG_LIKENESS, level);
		likeness.hag = hag.getUUID();
		likeness.slot = slot;
		likeness.snapTo(at.x, at.y, at.z, hag.getYRot(), 0.0F);
		likeness.mirror(hag);
		level.addFreshEntity(likeness);
		level.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 1.0, at.z, 16, 0.4, 0.8, 0.4, 0.01);
		return likeness;
	}

	public int slot() {
		return slot;
	}

	public MireHagEntity.Action action() {
		int id = entityData.get(ACTION);
		return id >= 0 && id < MireHagEntity.Action.values().length ? MireHagEntity.Action.values()[id] : MireHagEntity.Action.IDLE;
	}

	public MireHagEntity.Lantern lantern() {
		int id = entityData.get(LANTERN);
		return id >= 0 && id < MireHagEntity.Lantern.values().length ? MireHagEntity.Lantern.values()[id] : MireHagEntity.Lantern.LIT;
	}

	/** The Hag it walks with, while she is here and alive. */
	public @Nullable MireHagEntity hag() {
		return hag != null && level() instanceof ServerLevel level && level.getEntity(hag) instanceof MireHagEntity found && found.isAlive()
				? found : null;
	}

	/** It looks as she does: her health, her body's action and her lantern's flame. */
	private void mirror(MireHagEntity found) {
		AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
		if (health != null && getMaxHealth() != found.getMaxHealth()) {
			health.setBaseValue(found.getMaxHealth());
		}
		setHealth(Math.max(1.0F, found.getHealth()));
		entityData.set(ACTION, found.action().ordinal());
		entityData.set(LANTERN, found.lantern().ordinal());
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		MireHagEntity found = hag();
		if (found == null || !found.walksWith(getUUID())) {
			MireHagEntity.gas(level, position());
			discard();
			return;
		}
		resetFallDistance();
		mirror(found);
		Player foe = found.foe(level);
		if (found.attack() != null || !found.fighting()) {
			// She strikes, or cackles: it holds still where it is, turned on her foe as she is.
			setDeltaMovement(0.0, -0.3, 0.0);
			if (foe != null) {
				face(foe.position().subtract(position()));
			}
			return;
		}
		if (spot == null || tickCount % LOOK_EVERY == 0) {
			spot = found.likenessSpot(level, slot);
		}
		Vec3 step = spot == null ? Vec3.ZERO : MireHagEntity.wade(this, spot, found.speed(), found.centre(), MireHagEntity.ROAM);
		if (horizontalCollision && step.lengthSqr() > 1.0E-4 && sidestep == 0) {
			sidestep = 10;
			sidestepLeft = getRandom().nextBoolean();
		}
		if (sidestep > 0) {
			sidestep--;
			step = MireHagEntity.rotate(step, Math.toRadians(sidestepLeft ? 70.0 : -70.0));
		}
		step = keepDark(level, found, step);
		setDeltaMovement(step.x, -0.3, step.z);
		if (step.lengthSqr() > 1.0E-4) {
			face(step);
		} else if (foe != null) {
			face(foe.position().subtract(position()));
		}
	}

	/**
	 * It never walks into a burning lamp's light of its own accord: a step that would take it nearer a lamp's light than
	 * {@value #SHY} blocks turns aside, away from the lamp first, a little and then more, until it skirts the light;
	 * if no way does, it waits where it is. Only a lamp lit again, her Exchange or a blow puts a Likeness in the light.
	 */
	private Vec3 keepDark(ServerLevel level, MireHagEntity found, Vec3 step) {
		BlockPos lamp = step.lengthSqr() > 1.0E-4 ? found.nearestLitLamp(level, position()) : null;
		if (lamp == null) {
			return step;
		}
		Vec3 light = Vec3.atBottomCenterOf(lamp);
		if (!intoLight(light, step)) {
			return step;
		}
		double away = step.x * (light.z - getZ()) - step.z * (light.x - getX()) >= 0.0 ? -1.0 : 1.0;
		for (double turn = 30.0; turn <= 90.0; turn += 30.0) {
			for (double side : new double[] {away, -away}) {
				Vec3 turned = MireHagEntity.rotate(step, Math.toRadians(turn * side));
				if (!intoLight(light, turned)) {
					return turned;
				}
			}
		}
		return Vec3.ZERO;
	}

	/** Whether {@code step} takes it nearer the lamp at {@code light}, to within {@value #SHY} blocks of its light. */
	private boolean intoLight(Vec3 light, Vec3 step) {
		double now = Math.hypot(getX() - light.x, getZ() - light.z);
		double then = Math.hypot(getX() + step.x - light.x, getZ() + step.z - light.z);
		return then < now && then <= MireHagEntity.LAMP_REACH + SHY;
	}

	private void face(Vec3 way) {
		if (way.x * way.x + way.z * way.z > 1.0E-4) {
			float yaw = (float) (Math.toDegrees(Math.atan2(way.z, way.x)) - 90.0);
			setYRot(yaw);
			setYBodyRot(yaw);
			setYHeadRot(yaw);
		}
	}

	/** Anything striking it bursts it; a player striking it has taken part in her fight. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return super.hurtServer(level, source, amount);
		}
		Entity striker = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
		if (striker == null) {
			return false;
		}
		MireHagEntity found = hag();
		if (found == null) {
			MireHagEntity.gas(level, position());
			discard();
			return true;
		}
		if (striker instanceof ServerPlayer player) {
			found.took(player, Math.max(amount, 1.0F));
		}
		found.burst(level, this);
		return true;
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
	protected SoundEvent getAmbientSound() {
		return SoundEvents.WITCH_AMBIENT;
	}

	@Override
	public float getVoicePitch() {
		return 0.6F;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ACTION, MireHagEntity.Action.IDLE.ordinal());
		builder.define(LANTERN, MireHagEntity.Lantern.LIT.ordinal());
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		// Her clips, played as she plays them.
		controllers.add(new AnimationController<HagLikenessEntity>("body", 4, test -> {
			MireHagEntity.Action action = test.animatable().action();
			return test.setAndContinue(action == MireHagEntity.Action.IDLE && test.isMoving() ? MireHagEntity.WALK
					: MireHagEntity.BODY[action.ordinal()]);
		}));
		controllers.add(new AnimationController<HagLikenessEntity>("lantern", 4,
				test -> test.setAndContinue(MireHagEntity.FLAME[test.animatable().lantern().ordinal()])));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
