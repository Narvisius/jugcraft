package io.github.jimbozoomer.jugcraft.lair.dreadnought;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.lair.LairBosses;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A Sparker (docs/features/iron-dreadnought.md): a little coil crawler the Iron Dreadnought drops from the hatch in his
 * belly. It scuttles at the nearest player who may fight him; within {@value #REACH} blocks it stops and crackles for
 * {@value #FUSE} ticks, then discharges: {@value #DAMAGE} to everyone within {@value #REACH} blocks (an electric arc,
 * which the Grounding Strap halves), and it is spent. Struck first it dies ({@value #HEALTH} health), and whoever hurts
 * it takes part in his fight. It burns out after {@value #LIFE} ticks, or when he falls, resets or is gone; it drops
 * nothing, gives no experience, and is never saved.
 */
public class SparkerEntity extends Monster implements GeoEntity {
	public static final float HEALTH = 6.0F;
	public static final float DAMAGE = 4.0F;
	public static final double SPEED = 0.3;
	public static final double ARMOR = 0.0;
	public static final double REACH = 1.5;
	public static final int FUSE = 12;
	public static final int LIFE = 600;
	/** Ticks it tumbles as it drops from his belly, before it scuttles. */
	private static final int DROP = 10;

	private static final EntityDataAccessor<Boolean> FUSING = SynchedEntityData.defineId(SparkerEntity.class, EntityDataSerializers.BOOLEAN);
	private static final RawAnimation DROPPING = RawAnimation.begin().thenPlayAndHold("animation.sparker.drop");
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.sparker.idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.sparker.walk");
	private static final RawAnimation CRACKLE = RawAnimation.begin().thenLoop("animation.sparker.fuse");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID dreadnought;
	private int fuse = -1;

	public SparkerEntity(EntityType<? extends SparkerEntity> type, Level level) {
		super(type, level);
		xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.ATTACK_DAMAGE, DAMAGE)
				.add(Attributes.MOVEMENT_SPEED, SPEED).add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.ARMOR, ARMOR);
	}

	/** A Sparker drops from {@code dreadnought}'s belly at {@code at}. */
	public static SparkerEntity drop(ServerLevel level, IronDreadnoughtEntity dreadnought, Vec3 at) {
		SparkerEntity sparker = new SparkerEntity(JugcraftDreadnought.SPARKER, level);
		sparker.dreadnought = dreadnought.getUUID();
		sparker.snapTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
		level.addFreshEntity(sparker);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 0.4, at.z, 8, 0.3, 0.2, 0.3, 0.1);
		return sparker;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
	}

	/** Whether it crackles, about to discharge. */
	public boolean fusing() {
		return entityData.get(FUSING);
	}

	/** The Dreadnought it serves, if he is still about. */
	public @Nullable IronDreadnoughtEntity dreadnought(ServerLevel level) {
		return dreadnought != null && level.getEntity(dreadnought) instanceof IronDreadnoughtEntity found && found.isAlive() ? found : null;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		IronDreadnoughtEntity master = dreadnought(level);
		if (master == null || master.phase() == IronDreadnoughtEntity.Phase.WAITING || tickCount > LIFE) {
			burnOut(level);
			return;
		}
		if (tickCount % 6 == 0) {
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.9, getZ(), 1, 0.08, 0.08, 0.08, 0.02);
		}
		if (tickCount < DROP) {
			return;
		}
		if (fuse >= 0) {
			getNavigation().stop();
			if (++fuse >= FUSE) {
				discharge(level, master);
			} else if (fuse % 3 == 0) {
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.8, getZ(), 4, 0.3, 0.3, 0.3, 0.1);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 0.6F, 1.8F);
			}
			return;
		}
		Player target = nearest(level);
		if (target == null) {
			getNavigation().stop();
			return;
		}
		if (target.distanceTo(this) <= REACH) {
			light();
		} else if (tickCount % 10 == 0) {
			getNavigation().moveTo(target, 1.0);
		}
	}

	/** The nearest player who may fight its master, within 24 blocks. */
	private @Nullable Player nearest(ServerLevel level) {
		Player nearest = null;
		double best = 24.0 * 24.0;
		for (Player player : level.players()) {
			double distance = player.distanceToSqr(this);
			if (LairBosses.eligible(player) && distance < best) {
				best = distance;
				nearest = player;
			}
		}
		return nearest;
	}

	/** It stops and begins to crackle. */
	public void light() {
		if (fuse < 0) {
			fuse = 0;
			entityData.set(FUSING, true);
		}
	}

	/** It discharges: {@value #DAMAGE} to everyone who may fight its master within {@value #REACH} blocks, and it is spent. */
	public int discharge(ServerLevel level, IronDreadnoughtEntity master) {
		int hit = 0;
		for (Player player : level.players()) {
			if (LairBosses.eligible(player) && player.distanceTo(this) <= REACH + player.getBbWidth() / 2.0) {
				if (player.hurtServer(level, GroundingStrap.arc(level, this, master), IronDreadnoughtEntity.damage(DAMAGE))) {
					hit++;
				}
			}
		}
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.5, getZ(), 30, 0.8, 0.5, 0.8, 0.3);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 1.0F, 1.8F);
		discard();
		return hit;
	}

	/** It fizzles out. */
	public void burnOut(ServerLevel level) {
		level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.4, getZ(), 8, 0.2, 0.2, 0.2, 0.02);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.5F, 1.6F);
		discard();
	}

	/** Whoever hurts it takes part in the Dreadnought's fight. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hurt = super.hurtServer(level, source, amount);
		IronDreadnoughtEntity master = dreadnought(level);
		if (hurt && master != null && source.getEntity() instanceof ServerPlayer player) {
			master.took(player, amount);
		}
		return hurt;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean shouldDropExperience() {
		return false;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SILVERFISH_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.IRON_GOLEM_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.IRON_GOLEM_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 2.0F;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(FUSING, false);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (dreadnought != null) {
			output.store("dreadnought", UUIDUtil.CODEC, dreadnought);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		dreadnought = input.read("dreadnought", UUIDUtil.CODEC).orElse(null);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<SparkerEntity>("main", 2, test -> {
			SparkerEntity sparker = test.animatable();
			if (sparker.tickCount < DROP) {
				return test.setAndContinue(DROPPING);
			}
			return test.setAndContinue(sparker.fusing() ? CRACKLE : test.isMoving() ? WALK : IDLE);
		}));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
