package io.github.jimbozoomer.jugcraft.lair.vesperine;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.LoopType;
import com.geckolib.util.GeckoLibUtil;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A Grave Thrall: a skeleton with soul-flame eyes that Vesperine's Grave Call brings clawing up out of the soil. It
 * climbs out for {@value #EMERGE_TICKS} ticks, then goes for the nearest player. It crumbles when she falls, resets or
 * is gone, drops nothing and gives no experience, and is never saved.
 */
public class GraveThrallEntity extends Monster implements GeoEntity {
	public static final float HEALTH = 20.0F;
	public static final float DAMAGE = 4.0F;
	public static final int EMERGE_TICKS = 20;

	private static final EntityDataAccessor<Boolean> EMERGING = SynchedEntityData.defineId(GraveThrallEntity.class, EntityDataSerializers.BOOLEAN);
	private static final RawAnimation EMERGE = RawAnimation.begin().thenPlayAndHold("animation.grave_thrall.emerge");
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.grave_thrall.idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.grave_thrall.walk");
	private static final RawAnimation ATTACK = RawAnimation.begin().then("animation.grave_thrall.attack", LoopType.PLAY_ONCE);

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID boss;
	private int lonely;

	public GraveThrallEntity(EntityType<? extends GraveThrallEntity> type, Level level) {
		super(type, level);
		xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.ATTACK_DAMAGE, DAMAGE)
				.add(Attributes.MOVEMENT_SPEED, 0.26).add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.ARMOR, 2.0);
	}

	/** A thrall climbs out of the soil at {@code spot}, in {@code boss}'s service. */
	public static GraveThrallEntity rise(ServerLevel level, VesperineEntity boss, Vec3 spot) {
		GraveThrallEntity thrall = new GraveThrallEntity(JugcraftVesperine.GRAVE_THRALL, level);
		thrall.boss = boss.getUUID();
		thrall.snapTo(spot.x, spot.y, spot.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
		thrall.entityData.set(EMERGING, true);
		thrall.setNoAi(true);
		level.addFreshEntity(thrall);
		level.sendParticles(ParticleTypes.SOUL, spot.x, spot.y + 0.2, spot.z, 10, 0.3, 0.2, 0.3, 0.02);
		level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.BONE_BLOCK_BREAK, SoundSource.HOSTILE, 1.0F, 0.6F);
		return thrall;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
		targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	public boolean emerging() {
		return entityData.get(EMERGING);
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		if (emerging() && tickCount >= EMERGE_TICKS) {
			entityData.set(EMERGING, false);
			setNoAi(false);
		}
		if (tickCount % 20 == 0) {
			boolean serving = boss != null && level.getEntity(boss) instanceof VesperineEntity vesperine && vesperine.isAlive()
					&& vesperine.phase() != VesperineEntity.Phase.SEATED;
			lonely = serving ? 0 : lonely + 20;
			if (lonely > 40) {
				crumble(level);
				return;
			}
		}
		if (tickCount % 10 == 0) {
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getEyeY(), getZ(), 1, 0.15, 0.05, 0.15, 0.0);
		}
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit) {
			triggerAnim("main", "attack");
		}
		return hit;
	}

	/** Falls to bones and dust. */
	public void crumble(ServerLevel level) {
		level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.8, getZ(), 16, 0.3, 0.6, 0.3, 0.02);
		level.sendParticles(ParticleTypes.SOUL, getX(), getY() + 0.8, getZ(), 6, 0.3, 0.6, 0.3, 0.02);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.SKELETON_DEATH, SoundSource.HOSTILE, 0.8F, 1.2F);
		discard();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return !emerging() && super.hurtServer(level, source, amount);
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
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(EMERGING, false);
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
		AnimationController<GraveThrallEntity> main = new AnimationController<GraveThrallEntity>("main", 4,
				test -> test.setAndContinue(test.animatable().emerging() ? EMERGE : test.isMoving() ? WALK : IDLE));
		main.triggerableAnim("attack", ATTACK);
		controllers.add(main);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
