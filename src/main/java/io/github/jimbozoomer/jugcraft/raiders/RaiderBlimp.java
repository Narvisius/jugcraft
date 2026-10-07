package io.github.jimbozoomer.jugcraft.raiders;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Raider Blimp (batch 57): a small airship in raider canvas. It cruises {@value JugcraftRaiders#BLIMP_CRUISE} blocks
 * over whoever it hunts (or its raid's objective, with nobody to hunt), never lower than 8 blocks over the ground under
 * it, and when it is within {@value JugcraftRaiders#BLIMP_BOMB_REACH} blocks of overhead drops a bomb every
 * {@value JugcraftRaiders#BLIMP_BOMB_COOLDOWN} ticks: a heavy {@link RaiderBomb}, damage only. It flies, so it takes no
 * fall damage; flak, arrows and anything else that reaches it bring it down.
 */
public class RaiderBlimp extends Monster implements Raider {
	private static final int CLEARANCE = 8;
	private final RaidMember member = new RaidMember();
	private int bomb = JugcraftRaiders.BLIMP_BOMB_COOLDOWN;

	public RaiderBlimp(EntityType<? extends RaiderBlimp> type, Level level) {
		super(type, level);
		setNoGravity(true);
		xpReward = 20;
	}

	public static AttributeSupplier.Builder attributes(double health, double damage, double armour, double speed) {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, health).add(Attributes.ATTACK_DAMAGE, damage)
				.add(Attributes.ARMOR, armour).add(Attributes.MOVEMENT_SPEED, speed).add(Attributes.FOLLOW_RANGE, 64.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
	}

	@Override
	protected void registerGoals() {
		targetSelector.addGoal(1, new HurtByTargetGoal(this, RaiderInfantry.class, RaiderWalker.class, RaiderBlimp.class));
		// It hunts from the air: it needs no line of sight.
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (member.checkRaid(level, this)) {
			return;
		}
		LivingEntity target = getTarget();
		if (target != null && !target.isAlive()) {
			setTarget(null);
			target = null;
		}
		Vec3 over = target != null ? target.position() : member.objective != null ? Vec3.atBottomCenterOf(member.objective) : null;
		if (over == null) {
			// Nothing to do: hold station, drifting gently down to cruising height over the ground.
			setDeltaMovement(getDeltaMovement().scale(0.8));
			return;
		}
		double ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, getBlockX(), getBlockZ());
		double cruise = Math.max(over.y + JugcraftRaiders.BLIMP_CRUISE, ground + CLEARANCE);
		Vec3 wanted = new Vec3(over.x, cruise, over.z).subtract(position());
		double speed = getAttributeValue(Attributes.MOVEMENT_SPEED);
		Vec3 velocity = wanted.lengthSqr() > speed * speed ? wanted.normalize().scale(speed) : wanted;
		setDeltaMovement(getDeltaMovement().add(velocity.subtract(getDeltaMovement()).scale(0.2)));
		if (velocity.horizontalDistanceSqr() > 1.0E-4) {
			float yaw = (float) (Math.atan2(velocity.z, velocity.x) * 180.0 / Math.PI) - 90.0F;
			setYRot(net.minecraft.util.Mth.approachDegrees(getYRot(), yaw, 3.0F));
			yBodyRot = getYRot();
			yHeadRot = getYRot();
		}
		double dx = over.x - getX();
		double dz = over.z - getZ();
		if (--bomb <= 0 && dx * dx + dz * dz <= JugcraftRaiders.BLIMP_BOMB_REACH * JugcraftRaiders.BLIMP_BOMB_REACH) {
			RaiderBomb drop = new RaiderBomb(level, this, true);
			drop.setPos(getX(), getY() - 0.4, getZ());
			drop.shoot(0, -1, 0, 0.3F, 2.0F);
			level.addFreshEntity(drop);
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.DISPENSER_LAUNCH, SoundSource.HOSTILE, 1.0F, 0.5F);
			bomb = JugcraftRaiders.BLIMP_BOMB_COOLDOWN;
		}
		if (getHealth() < getMaxHealth() / 2 && tickCount % 5 == 0) {
			level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 2.5, getZ(), 2, 0.6, 0.3, 0.6, 0.01);
		}
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 2.5, getZ(), 30, 1.2, 1.0, 1.2, 0.03);
			RaiderRaids.fallen(level, this);
		}
	}

	@Override
	public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	protected void checkFallDamage(double distance, boolean onGround, BlockState state, BlockPos pos) {
	}

	@Override
	public boolean isPushable() {
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
	protected @Nullable SoundEvent getAmbientSound() {
		return SoundEvents.MINECART_RIDING;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.WOOL_BREAK;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.GENERIC_EXPLODE.value();
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
