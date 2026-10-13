package io.github.jimbozoomer.jugcraft.lair.tyrant;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.lair.TroughStoneBlock;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A Cinderling, one of the Cinder Tyrant's: a shard of his mantle crawled off as a little salamander of glowing slag,
 * {@value #HEALTH} health, whose bite deals {@value #DAMAGE} damage and sets its foe burning for {@value #BURN_SECONDS}
 * seconds, and quick. It goes for the nearest player and is immune to fire (its type's). In a flooded trough it gutters
 * out. It crumbles to ash when he falls, resets or is gone, drops nothing, gives no experience, and is never saved.
 */
public class CinderlingEntity extends Monster implements GeoEntity {
	public static final float HEALTH = 14.0F;
	public static final float DAMAGE = 3.0F;
	public static final double SPEED = 0.34;
	public static final int BURN_SECONDS = 2;

	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.cinderling.walk");
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.cinderling.idle");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID tyrant;

	public CinderlingEntity(EntityType<? extends CinderlingEntity> type, Level level) {
		super(type, level);
		xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.ATTACK_DAMAGE, DAMAGE)
				.add(Attributes.MOVEMENT_SPEED, SPEED).add(Attributes.FOLLOW_RANGE, 40.0);
	}

	/** A Cinderling crawls out at {@code at}, in {@code tyrant}'s service. */
	public static CinderlingEntity crawlOut(ServerLevel level, CinderTyrantEntity tyrant, Vec3 at) {
		CinderlingEntity ling = new CinderlingEntity(JugcraftTyrant.CINDERLING, level);
		ling.tyrant = tyrant.getUUID();
		ling.snapTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
		level.addFreshEntity(ling);
		level.sendParticles(ParticleTypes.LAVA, at.x, at.y + 0.3, at.z, 6, 0.3, 0.2, 0.3, 0.0);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.4, at.z, 6, 0.3, 0.3, 0.3, 0.02);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 0.8F, 1.4F);
		return ling;
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
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		if (tickCount % 4 == 0) {
			level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.5, getZ(), 1, 0.15, 0.05, 0.15, 0.0);
		}
		BlockState under = level.getBlockState(BlockPos.containing(getX(), getY() - 0.2, getZ()));
		if (onGround() && under.getBlock() instanceof TroughStoneBlock && under.getValue(TroughStoneBlock.FLOODED)) {
			gutter(level);
			return;
		}
		if (tickCount % 20 != 0) {
			return;
		}
		boolean serving = tyrant != null && level.getEntity(tyrant) instanceof CinderTyrantEntity found && found.isAlive()
				&& found.phase() != CinderTyrantEntity.Phase.WAITING;
		if (!serving) {
			crumble(level);
		}
	}

	/** In a flooded trough it gutters out in a hiss of steam. */
	public void gutter(ServerLevel level) {
		level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 10, 0.3, 0.2, 0.3, 0.02);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.8F, 1.2F);
		discard();
	}

	/** It crumbles to ash and is gone. */
	public void crumble(ServerLevel level) {
		level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.3, getZ(), 8, 0.3, 0.2, 0.3, 0.01);
		level.sendParticles(ParticleTypes.WHITE_ASH, getX(), getY() + 0.3, getZ(), 12, 0.4, 0.3, 0.4, 0.02);
		discard();
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit && target instanceof LivingEntity living) {
			living.igniteForSeconds(BURN_SECONDS);
		}
		return hit;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hurt = super.hurtServer(level, source, amount);
		if (hurt && source.getEntity() instanceof ServerPlayer player && tyrant != null
				&& level.getEntity(tyrant) instanceof CinderTyrantEntity found) {
			found.took(player, amount);
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
		return SoundEvents.CAMPFIRE_CRACKLE;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.FIRE_EXTINGUISH;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.FIRE_EXTINGUISH;
	}

	@Override
	public float getVoicePitch() {
		return 1.4F;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (tyrant != null) {
			output.store("tyrant", UUIDUtil.CODEC, tyrant);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		tyrant = input.read("tyrant", UUIDUtil.CODEC).orElse(null);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<CinderlingEntity>("main", 2, test -> test.setAndContinue(test.isMoving() ? WALK : IDLE)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
