package io.github.jimbozoomer.jugcraft.lair.tatterlace;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
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
import net.minecraft.world.entity.LivingEntity;
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
 * One of Madame Tatterlace's brood: a small, quick spider out of her egg sacs, {@value #HEALTH} health, whose bite deals
 * {@value #DAMAGE} damage and poisons for {@value #POISON_TICKS} ticks. It goes for the nearest player. It shrivels when
 * she falls, resets or is gone, drops nothing and gives no experience, and is never saved.
 */
public class SpiderlingEntity extends Monster implements GeoEntity {
	public static final float HEALTH = 6.0F;
	public static final float DAMAGE = 2.0F;
	public static final int POISON_TICKS = 40;
	public static final double SPEED = 0.35;

	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.tatter_spiderling.walk");
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.tatter_spiderling.idle");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID boss;

	public SpiderlingEntity(EntityType<? extends SpiderlingEntity> type, Level level) {
		super(type, level);
		xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.ATTACK_DAMAGE, DAMAGE)
				.add(Attributes.MOVEMENT_SPEED, SPEED).add(Attributes.FOLLOW_RANGE, 32.0);
	}

	/** A spiderling comes out of an egg sac at {@code at}, in {@code mother}'s service. */
	public static SpiderlingEntity hatch(ServerLevel level, TatterlaceEntity mother, Vec3 at) {
		SpiderlingEntity spiderling = new SpiderlingEntity(JugcraftTatterlace.TATTER_SPIDERLING, level);
		spiderling.boss = mother.getUUID();
		spiderling.snapTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
		level.addFreshEntity(spiderling);
		mother.hatched(spiderling);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.SPIDER_AMBIENT, SoundSource.HOSTILE, 0.8F, 1.8F);
		return spiderling;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
		targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level) || tickCount % 20 != 0) {
			return;
		}
		boolean serving = boss != null && level.getEntity(boss) instanceof TatterlaceEntity mother && mother.isAlive()
				&& mother.phase() != TatterlaceEntity.Phase.WAITING;
		if (!serving) {
			shrivel(level);
		}
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit && target instanceof LivingEntity living) {
			living.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_TICKS, 0), this);
		}
		return hit;
	}

	/** Curls up and shrivels away. */
	public void shrivel(ServerLevel level) {
		level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.2, getZ(), 6, 0.2, 0.1, 0.2, 0.01);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.SPIDER_DEATH, SoundSource.HOSTILE, 0.5F, 1.8F);
		discard();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hurt = super.hurtServer(level, source, amount);
		if (hurt && source.getEntity() instanceof ServerPlayer player && boss != null && level.getEntity(boss) instanceof TatterlaceEntity mother) {
			mother.took(player, amount);
		}
		return hurt;
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
		return SoundEvents.SPIDER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SPIDER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SPIDER_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 1.8F;
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
		controllers.add(new AnimationController<SpiderlingEntity>("main", 2, test -> test.setAndContinue(test.isMoving() ? WALK : IDLE)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
