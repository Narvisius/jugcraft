package io.github.jimbozoomer.jugcraft.lair.alpha;

import io.github.jimbozoomer.jugcraft.agriculture.Werewolf;
import io.github.jimbozoomer.jugcraft.agriculture.Werewolves;
import io.github.jimbozoomer.jugcraft.lair.LairBosses;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A Packmate (docs/features/alpha-werewolf.md): a young werewolf of the Alpha's blood, drawn as the brown werewolves are
 * but smaller ({@value #SCALE} of their size). It harries whoever the Alpha's pack is after ({@link
 * AlphaWerewolfEntity#packTarget}: his quarry in a hunt, his foe otherwise): a bite of {@value #DAMAGE} within
 * {@value #REACH} blocks, every {@value #BITE_COOLDOWN} ticks. A sprig of wolfsbane in its target's hand keeps it from
 * biting them: it circles them and growls. While he is flushed out the pack cowers among the trees, running from the
 * nearest player and biting nobody. It has no hide (every blow does full damage, silver {@value #SILVER} times), and
 * whoever hurts it takes part in his fight. It melts back into the trees when he is gone, waiting on his stone or reset;
 * it drops nothing, gives no experience, and is never saved.
 */
public class PackmateEntity extends Monster {
	public static final float HEALTH = 24.0F;
	public static final float DAMAGE = 4.0F;
	public static final double SPEED = 0.32;
	public static final double REACH = 1.6;
	public static final int BITE_COOLDOWN = 20;
	public static final float SILVER = 1.5F;
	public static final double SCALE = 0.85;
	public static final double COWER_SPEED = 0.2;
	/** How far round its target it circles when wolfsbane keeps it off. */
	private static final double CIRCLE = 3.0;

	private @Nullable UUID alpha;
	private int biteIn;

	public PackmateEntity(EntityType<? extends PackmateEntity> type, Level level) {
		super(type, level);
		xpReward = 0;
		AttributeInstance scale = getAttribute(Attributes.SCALE);
		if (scale != null) {
			scale.setBaseValue(SCALE);
		}
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.ATTACK_DAMAGE, DAMAGE)
				.add(Attributes.MOVEMENT_SPEED, SPEED).add(Attributes.FOLLOW_RANGE, 48.0).add(Attributes.STEP_HEIGHT, 1.0);
	}

	/** A Packmate lopes out of the trees at {@code at}, one of {@code alpha}'s pack. */
	public static PackmateEntity lope(ServerLevel level, AlphaWerewolfEntity alpha, Vec3 at) {
		PackmateEntity packmate = new PackmateEntity(JugcraftAlpha.PACKMATE, level);
		packmate.alpha = alpha.getUUID();
		Vec3 to = alpha.centre().subtract(at);
		packmate.snapTo(at.x, at.y, at.z, (float) (Math.toDegrees(Math.atan2(to.z, to.x)) - 90.0), 0.0F);
		level.addFreshEntity(packmate);
		level.sendParticles(ParticleTypes.POOF, at.x, at.y + 0.6, at.z, 8, 0.4, 0.4, 0.4, 0.02);
		return packmate;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
	}

	/** The Alpha it runs with, if he is still about. */
	public @Nullable AlphaWerewolfEntity alpha(ServerLevel level) {
		return alpha != null && level.getEntity(alpha) instanceof AlphaWerewolfEntity found && found.isAlive() ? found : null;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		AlphaWerewolfEntity master = alpha(level);
		if (master == null || !master.awake()) {
			meltAway(level);
			return;
		}
		if (biteIn > 0) {
			biteIn--;
		}
		if (master.cowering()) {
			setAggressive(false);
			cower(level);
			return;
		}
		Player target = master.packTarget(level);
		if (target == null || distanceToSqr(target) > 48.0 * 48.0) {
			setAggressive(false);
			getNavigation().stop();
			return;
		}
		setAggressive(true);
		getLookControl().setLookAt(target);
		if (warded(target)) {
			circle(level, target);
			return;
		}
		if (distanceTo(target) <= REACH + target.getBbWidth() / 2.0) {
			getNavigation().stop();
			if (biteIn == 0) {
				bite(level, target);
			}
		} else if (tickCount % 10 == 0 || getNavigation().isDone()) {
			getNavigation().moveTo(target, 1.0);
		}
	}

	/** Whether {@code target} holds a sprig of wolfsbane, which keeps the pack from biting them (not the Alpha). */
	public static boolean warded(LivingEntity target) {
		return target.getMainHandItem().is(Werewolves.wolfsbane()) || target.getOffhandItem().is(Werewolves.wolfsbane());
	}

	/** It bites {@code target}: {@value #DAMAGE}, times {@code lairs.boss_damage}. Returns whether it hurt them. */
	public boolean bite(ServerLevel level, Player target) {
		biteIn = BITE_COOLDOWN;
		swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT);
		level.playSound(null, getX(), getY(), getZ(), AlphaWerewolfEntity.sound("entity.wolf.growl", SoundEvents.ZOMBIE_AMBIENT),
				SoundSource.HOSTILE, 1.0F, 1.2F);
		return target.hurtServer(level, damageSources().mobAttack(this), LairBosses.damage(DAMAGE));
	}

	/** Kept off by wolfsbane: it circles {@code target} {@value #CIRCLE} blocks out, growling now and then. */
	private void circle(ServerLevel level, Player target) {
		if (tickCount % 10 == 0 || getNavigation().isDone()) {
			double angle = Math.atan2(getZ() - target.getZ(), getX() - target.getX()) + 0.6;
			getNavigation().moveTo(target.getX() + Math.cos(angle) * CIRCLE, target.getY(), target.getZ() + Math.sin(angle) * CIRCLE, 1.0);
		}
		if (tickCount % 40 == 0) {
			level.playSound(null, getX(), getY(), getZ(), AlphaWerewolfEntity.sound("entity.wolf.growl", SoundEvents.ZOMBIE_AMBIENT),
					SoundSource.HOSTILE, 1.0F, 1.1F);
		}
	}

	/** Cowering: it runs from the nearest player, biting nobody. */
	private void cower(ServerLevel level) {
		if (tickCount % 10 != 0 && !getNavigation().isDone()) {
			return;
		}
		Player near = level.getNearestPlayer(this, 24.0);
		if (near == null) {
			getNavigation().stop();
			return;
		}
		Vec3 away = position().subtract(near.position());
		away = away.horizontalDistanceSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(away.x, 0.0, away.z).normalize();
		Vec3 to = position().add(away.scale(10.0));
		getNavigation().moveTo(to.x, to.y, to.z, COWER_SPEED / SPEED);
		if (tickCount % 30 == 0) {
			level.playSound(null, getX(), getY(), getZ(), AlphaWerewolfEntity.sound("entity.wolf.whine", SoundEvents.ZOMBIE_AMBIENT),
					SoundSource.HOSTILE, 0.8F, 1.2F);
		}
	}

	/** It melts back into the trees. */
	public void meltAway(ServerLevel level) {
		level.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.8, getZ(), 10, 0.3, 0.5, 0.3, 0.02);
		discard();
	}

	/** No hide: every blow does full damage, silver {@value #SILVER} times; whoever hurts it takes part in his fight. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		float dealt = Werewolf.silver(source) ? amount * SILVER : amount;
		boolean hurt = super.hurtServer(level, source, dealt);
		AlphaWerewolfEntity master = alpha(level);
		if (hurt && master != null && source.getEntity() instanceof ServerPlayer player) {
			master.took(player, dealt);
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
	public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return AlphaWerewolfEntity.sound("entity.wolf.growl", SoundEvents.ZOMBIE_AMBIENT);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return AlphaWerewolfEntity.sound("entity.wolf.hurt", SoundEvents.ZOMBIE_HURT);
	}

	@Override
	protected SoundEvent getDeathSound() {
		return AlphaWerewolfEntity.sound("entity.wolf.death", SoundEvents.ZOMBIE_DEATH);
	}

	@Override
	public float getVoicePitch() {
		return 0.8F + random.nextFloat() * 0.1F;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (alpha != null) {
			output.store("alpha", UUIDUtil.CODEC, alpha);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		alpha = input.read("alpha", UUIDUtil.CODEC).orElse(null);
	}
}
