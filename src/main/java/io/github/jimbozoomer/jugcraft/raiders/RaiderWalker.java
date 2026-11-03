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
import java.util.EnumSet;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
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
import net.minecraft.world.level.ServerLevelAccessor;
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
 *
 * <p>Its driver ({@link RaiderCrew}) sits head and shoulders out of the open hatch on its roof. Without its driver a
 * crewed walker stalls, smokes and goes down; when it falls, the driver climbs out and fights on foot. It charges only
 * a defender it can reach on foot; one it cannot (on a wall, behind a gate) it engages from a standoff
 * ({@value #STANDOFF} blocks), in sight, with its hull launcher, rather than grinding against the wall.
 */
public class RaiderWalker extends Monster implements Raider {
	private static final EntityDataAccessor<Integer> PUNCHED_AT = SynchedEntityData.defineId(RaiderWalker.class, EntityDataSerializers.INT);
	/** Where it engages a defender it cannot reach: this far off, within its launcher's reach. */
	static final int STANDOFF = 14;
	/** Ticks between its checks whether its target can be reached on foot. */
	static final int REACH_CHECK_TICKS = 20;
	private final RaidMember member = new RaidMember();
	private int launch = JugcraftRaiders.WALKER_LAUNCH_COOLDOWN;
	/** Whether it has had a crew (saved): a walker that has lost its crew fails; one that never had one walks as before. */
	private boolean crewed;
	private int abandoned;
	private int reachCheckedAt = -REACH_CHECK_TICKS;
	private @Nullable LivingEntity reachChecked;
	private boolean reachable;
	/** Client side: blocks walked, for the legs' swing. */
	private final io.github.jimbozoomer.jugcraft.walker.WalkerStride gait = new io.github.jimbozoomer.jugcraft.walker.WalkerStride();
	private int swungAt = -100;

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
	public boolean canAttack(net.minecraft.world.entity.LivingEntity target) {
		return RaiderTargets.allowed(target) && super.canAttack(target);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		// A crewed walker that has lost its driver (stalled) does nothing but smoke until it goes down.
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true) {
			@Override
			public boolean canUse() {
				return !stalled() && reachesTarget() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return !stalled() && reachesTarget() && super.canContinueToUse();
			}
		});
		goalSelector.addGoal(3, new StandoffGoal());
		goalSelector.addGoal(4, new MarchGoal(this, () -> stalled() ? null : member.objective, 1.0));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6) {
			@Override
			public boolean canUse() {
				return !stalled() && super.canUse();
			}
		});
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
		targetSelector.addGoal(1, new HurtByTargetGoal(this, RaiderInfantry.class, RaiderWalker.class, RaiderBlimp.class));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, io.github.jimbozoomer.jugcraft.town.Townsfolk.class, true));
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			gait.tick(this);
			if (tickCount % 4 == 0) {
				// Exhaust from the stacks on its back.
				Vec3 back = Vec3.directionFromRotation(0, yBodyRot).scale(-1.1);
				level().addParticle(ParticleTypes.SMOKE, getX() + back.x, getY() + 4.5, getZ() + back.z, 0, 0.05, 0);
			}
		}
	}

	public boolean crewed() {
		return crewed;
	}

	/** Whether it had a driver and has lost them: it stands, smoking, until it goes down (RaiderCrew.abandoned). */
	public boolean stalled() {
		return crewed && RaiderCrew.crew(this) == null;
	}

	void setCrewed(boolean crewed) {
		this.crewed = crewed;
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return passenger instanceof RaiderInfantry && getPassengers().isEmpty();
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		return RaiderCrew.WALKER_SEAT.yRot(-getYRot() * net.minecraft.util.Mth.DEG_TO_RAD);
	}

	/** The walker walks itself (its driver is crew, not a rider the game hands control to). */
	@Override
	public @Nullable LivingEntity getControllingPassenger() {
		return null;
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData group) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, group);
		RaiderCrew.crewOnSpawn(level.getLevel(), this, reason);
		return data;
	}

	/**
	 * Whether its target can be reached on foot (a path that gets there, or it is already in reach), checked at most
	 * every {@value #REACH_CHECK_TICKS} ticks for a given target.
	 */
	boolean reachesTarget() {
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive()) {
			return false;
		}
		if (target != reachChecked || tickCount - reachCheckedAt >= REACH_CHECK_TICKS) {
			reachChecked = target;
			reachCheckedAt = tickCount;
			Path path = getNavigation().createPath(target, 0);
			reachable = path != null && path.canReach() || distanceToSqr(target) < 16.0;
		}
		return reachable;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (member.checkRaid(level, this)) {
			return;
		}
		if (stalled()) {
			// No driver: it stalls where it stands, smoking, and goes down.
			getNavigation().stop();
			RaiderCrew.abandoned(level, this, ++abandoned);
			return;
		}
		abandoned = 0;
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
		if (stalled()) {
			return false;
		}
		entityData.set(PUNCHED_AT, tickCount);
		level.playSound(null, getX(), getY() + 2, getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.6F, 0.6F);
		return super.doHurtTarget(level, target);
	}

	public float stride(float partialTick) {
		return gait.phase(partialTick);
	}

	public float strideWeight(float partialTick) {
		return gait.weight(partialTick);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (PUNCHED_AT.equals(key) && level().isClientSide() && entityData.get(PUNCHED_AT) >= 0) {
			swungAt = tickCount;
		}
	}

	/** Ticks since it last punched (for the arm's swing). */
	public int sinceSwing() {
		return tickCount - swungAt;
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level) {
			RaiderCrew.bailOut(this);
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
		output.putBoolean("crewed", crewed);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		member.load(input);
		crewed = input.getBooleanOr("crewed", false);
	}

	/**
	 * A defender it cannot reach on foot: it walks to a standoff {@value #STANDOFF} blocks from them, on its side, holds
	 * there facing them while its launcher fires, and backs off if they are nearer than the launcher's least range.
	 * If it gets no nearer its standoff for a while (a wall, a ditch), it tries a spot further round.
	 */
	private final class StandoffGoal extends Goal {
		private int repath;
		private double nearest;
		private int stalls;
		private float swing;

		StandoffGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = getTarget();
			return !stalled() && target != null && target.isAlive() && !reachesTarget();
		}

		@Override
		public boolean canContinueToUse() {
			return canUse();
		}

		@Override
		public void start() {
			repath = 0;
			nearest = Double.MAX_VALUE;
			stalls = 0;
			swing = 0;
		}

		@Override
		public void stop() {
			getNavigation().stop();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity target = getTarget();
			if (target == null) {
				return;
			}
			getLookControl().setLookAt(target, 10.0F, 10.0F);
			if (--repath > 0) {
				return;
			}
			repath = 20;
			Vec3 from = target.position();
			Vec3 away = position().subtract(from);
			Vec3 flat = new Vec3(away.x, 0, away.z);
			double distance = flat.length();
			if (distance >= JugcraftRaiders.WALKER_LAUNCH_MIN && distance <= JugcraftRaiders.WALKER_LAUNCH_MAX - 4 && hasLineOfSight(target)) {
				getNavigation().stop();   // in reach and in sight: hold and let the launcher work
				stalls = 0;
				nearest = Double.MAX_VALUE;
				return;
			}
			Vec3 dir = distance < 1.0E-3 ? new Vec3(1, 0, 0) : flat.scale(1.0 / distance);
			Vec3 spot = from.add(dir.yRot(swing * net.minecraft.util.Mth.DEG_TO_RAD).scale(STANDOFF));
			double gap = position().distanceToSqr(spot.x, getY(), spot.z);
			if (gap < nearest - 1.0) {
				nearest = gap;
				stalls = 0;
			} else if (++stalls >= 6) {
				// No nearer for six seconds: try a spot further round.
				swing = swing >= 0 ? -swing - 35.0F : -swing;
				nearest = Double.MAX_VALUE;
				stalls = 0;
			}
			int x = (int) Math.floor(spot.x);
			int z = (int) Math.floor(spot.z);
			BlockPos to = new BlockPos(x, level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
			if (level().isLoaded(to)) {
				getNavigation().moveTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5, 0.9);
			}
		}
	}
}
