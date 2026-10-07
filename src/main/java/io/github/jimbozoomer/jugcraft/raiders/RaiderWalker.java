package io.github.jimbozoomer.jugcraft.raiders;

import io.github.jimbozoomer.jugcraft.town.Townsfolk;
import java.util.UUID;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
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
 * The Raider Walker (batch 57): the Armoured Walker (batch 58, the owner's model) in raider paint, its hatch shut. It
 * wades in and rams with its piston arm (a heavy blow that throws what it hits), and lobs grenades from its hull gun at
 * anything {@value JugcraftRaiders#WALKER_LAUNCH_MIN} to {@value JugcraftRaiders#WALKER_LAUNCH_MAX} blocks off, every
 * {@value JugcraftRaiders#WALKER_LAUNCH_COOLDOWN} ticks. It shrugs off knockback and climbs a block and a half. Raiders
 * never break blocks.
 */
public class RaiderWalker extends Monster implements Raider {
	private static final EntityDataAccessor<Integer> PUNCHED_AT = SynchedEntityData.defineId(RaiderWalker.class, EntityDataSerializers.INT);
	private final RaidMember member = new RaidMember();
	private int launch = JugcraftRaiders.WALKER_LAUNCH_COOLDOWN;
	/** Client side: blocks walked, for the legs' swing. */
	private float stride;

	public RaiderWalker(EntityType<? extends RaiderWalker> type, Level level) {
		super(type, level);
		xpReward = 30;
	}

	public static AttributeSupplier.Builder attributes(double health, double damage, double armour, double speed) {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, health).add(Attributes.ATTACK_DAMAGE, damage)
				.add(Attributes.ARMOR, armour).add(Attributes.MOVEMENT_SPEED, speed).add(Attributes.FOLLOW_RANGE, 40.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.ATTACK_KNOCKBACK, 1.5).add(Attributes.STEP_HEIGHT, 1.5);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(PUNCHED_AT, -100);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
		goalSelector.addGoal(4, new MarchGoal(this, () -> member.objective, 1.0));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
		targetSelector.addGoal(1, new HurtByTargetGoal(this, RaiderInfantry.class, RaiderWalker.class, RaiderBlimp.class));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Townsfolk.class, true));
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			stride += (float) Math.sqrt((getX() - xo) * (getX() - xo) + (getZ() - zo) * (getZ() - zo));
			if (tickCount % 4 == 0) {
				// Exhaust from the stacks on its back.
				Vec3 back = Vec3.directionFromRotation(0, yBodyRot).scale(-1.1);
				level().addParticle(ParticleTypes.SMOKE, getX() + back.x, getY() + 4.5, getZ() + back.z, 0, 0.05, 0);
			}
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (member.checkRaid(level, this)) {
			return;
		}
		LivingEntity target = getTarget();
		if (--launch <= 0 && target != null && target.isAlive()) {
			double distance = distanceTo(target);
			if (distance >= JugcraftRaiders.WALKER_LAUNCH_MIN && distance <= JugcraftRaiders.WALKER_LAUNCH_MAX && hasLineOfSight(target)) {
				Vec3 muzzle = position().add(io.github.jimbozoomer.jugcraft.walker.ArmouredWalker.MUZZLE.yRot(-yBodyRot * net.minecraft.util.Mth.DEG_TO_RAD));
				RaiderInfantry.throwAt(level, this, target, false, muzzle);
				level.sendParticles(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z, 6, 0.2, 0.2, 0.2, 0.02);
				level.playSound(null, muzzle.x, muzzle.y, muzzle.z, SoundEvents.DISPENSER_LAUNCH, SoundSource.HOSTILE, 1.5F, 0.6F);
				launch = JugcraftRaiders.WALKER_LAUNCH_COOLDOWN;
			}
		}
	}

	/** Its punch: the arm swings on every client. */
	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		entityData.set(PUNCHED_AT, tickCount);
		level.playSound(null, getX(), getY() + 2, getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.6F, 0.6F);
		return super.doHurtTarget(level, target);
	}

	public float stride() {
		return stride;
	}

	/** Ticks since it last punched (for the arm's swing). */
	public int sinceSwing() {
		return tickCount - entityData.get(PUNCHED_AT);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 2.5, getZ(), 30, 1.0, 1.2, 1.0, 0.03);
			RaiderRaids.fallen(level, this);
		}
	}

	@Override
	public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	public @Nullable UUID raid() {
		return member.raid;
	}

	@Override
	public @Nullable BlockPos objective() {
		return member.objective;
	}

	@Override
	public void joinRaid(UUID raid, BlockPos objective) {
		member.join(raid, objective);
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return !member.persistent() && super.removeWhenFarAway(distance);
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
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.IRON_GOLEM_STEP, 1.0F, 0.6F);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		member.save(output);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		member.load(input);
	}
}
