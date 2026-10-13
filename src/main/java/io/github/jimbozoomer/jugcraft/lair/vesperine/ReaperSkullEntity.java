package io.github.jimbozoomer.jugcraft.lair.vesperine;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Dirge and Requiem, Vesperine's two skulls ({@code jugcraft:dirge} drifts at her left shoulder, {@code jugcraft:requiem}
 * at her right). While both live she takes half damage. Each opens its jaw, its eyes flare, and it looses a slow homing
 * {@link GriefBoltEntity} at her foe every {@value #BOLT_INTERVAL} ticks, taking turns. They are part of her: they go
 * where she goes, cannot be hurt while she sits or tolls, and whoever hurts them has taken part in her fight. At the
 * Last Toll a slain skull re-forms at half its health.
 */
public class ReaperSkullEntity extends Monster implements GeoEntity {
	public static final float HEALTH = 80.0F;
	public static final int BOLT_INTERVAL = 80;
	public static final int JAW_TICKS = 10;
	public static final double FOLLOW_SPEED = 0.35;

	private static final EntityDataAccessor<Boolean> JAW = SynchedEntityData.defineId(ReaperSkullEntity.class, EntityDataSerializers.BOOLEAN);
	private static final RawAnimation FLOAT = RawAnimation.begin().thenLoop("animation.reaper_skull.float");
	private static final RawAnimation OPEN = RawAnimation.begin().thenPlayAndHold("animation.reaper_skull.open");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID boss;
	private int boltTimer;
	private int lonely;

	public ReaperSkullEntity(EntityType<? extends ReaperSkullEntity> type, Level level) {
		super(type, level);
		xpReward = 0;
		noPhysics = true;
		setNoGravity(true);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.ARMOR, 4.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 48.0);
	}

	/**
	 * Calls up a skull of {@code type} at {@code boss}'s shoulder, its health scaled for her party and then set to
	 * {@code share} of that (a skull re-formed at the Last Toll has half).
	 */
	public static ReaperSkullEntity summon(ServerLevel level, EntityType<ReaperSkullEntity> type, VesperineEntity boss, double scale,
			float share) {
		ReaperSkullEntity skull = new ReaperSkullEntity(type, level);
		skull.boss = boss.getUUID();
		skull.boltTimer = type == JugcraftVesperine.REQUIEM ? BOLT_INTERVAL / 2 : 0;  // they take turns
		Vec3 at = boss.shoulder(skull.left());
		skull.snapTo(at.x, at.y, at.z, boss.getYRot(), 0.0F);
		skull.scale(scale, share);
		level.addFreshEntity(skull);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.6, at.z, 20, 0.4, 0.4, 0.4, 0.02);
		return skull;
	}

	/** Scales its health for her party, then sets it to {@code share} of that. */
	public void scale(double scale, float share) {
		AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
		if (health != null) {
			health.setBaseValue(HEALTH * scale);
		}
		setHealth(getMaxHealth() * share);
	}

	/** Dirge drifts at her left shoulder, Requiem at her right. */
	public boolean left() {
		return getType() == JugcraftVesperine.DIRGE;
	}

	public boolean jawOpen() {
		return entityData.get(JAW);
	}

	public @Nullable VesperineEntity boss(ServerLevel level) {
		return boss != null && level.getEntity(boss) instanceof VesperineEntity vesperine && vesperine.isAlive() ? vesperine : null;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		VesperineEntity vesperine = boss(level);
		if (vesperine == null) {
			if (++lonely > 40) {
				crumble(level);  // she is gone: so are they
			}
			return;
		}
		lonely = 0;
		resetFallDistance();
		Vec3 to = vesperine.shoulder(left()).subtract(position());
		double length = to.length();
		setDeltaMovement(length < 0.05 ? Vec3.ZERO : to.scale(Math.min(FOLLOW_SPEED + length * 0.1, length) / length));
		Player foe = vesperine.foe(level);
		if (foe != null) {
			double dx = foe.getX() - getX();
			double dz = foe.getZ() - getZ();
			float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
			setYRot(yaw);
			setYBodyRot(yaw);
			setYHeadRot(yaw);
		} else {
			setYRot(vesperine.getYRot());
			setYBodyRot(vesperine.getYRot());
			setYHeadRot(vesperine.getYRot());
		}
		boolean firing = vesperine.fighting() && foe != null && vesperine.attack() != VesperineEntity.Attack.TWIN_BEAM;
		if (!firing) {
			entityData.set(JAW, vesperine.attack() == VesperineEntity.Attack.TWIN_BEAM);
			return;
		}
		boltTimer++;
		entityData.set(JAW, boltTimer >= BOLT_INTERVAL - JAW_TICKS);
		if (boltTimer >= BOLT_INTERVAL) {
			boltTimer = 0;
			entityData.set(JAW, false);
			GriefBoltEntity.loose(level, this, foe);
		}
	}

	/** Gone with a puff of black smoke (she has fallen or left). */
	public void crumble(ServerLevel level) {
		level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.6, getZ(), 30, 0.5, 0.5, 0.5, 0.02);
		level.sendParticles(ParticleTypes.SOUL, getX(), getY() + 0.6, getZ(), 10, 0.4, 0.4, 0.4, 0.02);
		discard();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return super.hurtServer(level, source, amount);
		}
		VesperineEntity vesperine = boss(level);
		if (vesperine != null && !vesperine.fighting()) {
			return false;  // she sits, rises or tolls: they cannot be harmed then
		}
		boolean hurt = super.hurtServer(level, source, amount);
		if (hurt && vesperine != null && source.getEntity() instanceof ServerPlayer player) {
			vesperine.took(player, amount);
		}
		return hurt;
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
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean shouldDropExperience() {
		return false;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SKELETON_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SKELETON_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SKELETON_DEATH;
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.6, getZ(), 30, 0.5, 0.5, 0.5, 0.02);
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.BONE_BLOCK_BREAK, SoundSource.HOSTILE, 2.0F, 0.6F);
		}
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(JAW, false);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (boss != null) {
			output.store("boss", UUIDUtil.CODEC, boss);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		boss = input.read("boss", UUIDUtil.CODEC).orElse(null);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<ReaperSkullEntity>("main", 3,
				test -> test.setAndContinue(test.animatable().jawOpen() ? OPEN : FLOAT)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
