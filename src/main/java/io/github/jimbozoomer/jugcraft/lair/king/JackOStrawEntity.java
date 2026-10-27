package io.github.jimbozoomer.jugcraft.lair.king;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.lair.Encounter;
import io.github.jimbozoomer.jugcraft.lair.LairBosses;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A Jack-o'-Straw (docs/features/pumpkin-king.md): one of the Pumpkin King's little straw men, a carved gourd for a head,
 * lit inside. It climbs out of a gourd that ripened through (or out of the rows at his Crowning), and harries whoever he
 * is after ({@link PumpkinKingEntity#jackTarget}): a swipe of {@value #DAMAGE} within {@value #REACH} blocks, every
 * {@value #SWIPE_COOLDOWN} ticks; quick on its feet. While he is bereft it stands dazed, swiping at nobody. Straw burns:
 * fire does {@value #FIRE} times its damage. Whoever hurts it takes part in his fight. It crumbles to straw when he is
 * gone, back on his post or fallen; it drops nothing, gives no experience, and is never saved.
 */
public class JackOStrawEntity extends Monster implements GeoEntity {
	public static final float HEALTH = 20.0F;
	public static final float DAMAGE = 3.0F;
	public static final double SPEED = 0.3;
	public static final double REACH = 1.5;
	public static final int SWIPE_COOLDOWN = 20;
	public static final float FIRE = 2.0F;
	/** Ticks it takes to climb out, unhurried and harmless. */
	public static final int CLIMB_TICKS = 20;
	/** Ticks its swipe is shown. */
	private static final int SWIPE_TICKS = 8;

	/** What it is doing, synced for its animations. */
	public enum Action {
		CLIMB, IDLE, SWIPE, DAZED
	}

	private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(JackOStrawEntity.class, EntityDataSerializers.INT);
	private static final RawAnimation[] CLIPS = new RawAnimation[Action.values().length];
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.jack_o_straw.walk");
	private static final RawAnimation CRUMBLE = RawAnimation.begin().thenPlayAndHold("animation.jack_o_straw.crumble");

	static {
		String prefix = "animation.jack_o_straw.";
		CLIPS[Action.CLIMB.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "climb");
		CLIPS[Action.IDLE.ordinal()] = RawAnimation.begin().thenLoop(prefix + "idle");
		CLIPS[Action.SWIPE.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "swipe");
		CLIPS[Action.DAZED.ordinal()] = RawAnimation.begin().thenLoop(prefix + "dazed");
	}

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID king;
	private int swipeIn;
	private int climbTicks;
	private int swipeShown;

	public JackOStrawEntity(EntityType<? extends JackOStrawEntity> type, Level level) {
		super(type, level);
		xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.ATTACK_DAMAGE, DAMAGE)
				.add(Attributes.MOVEMENT_SPEED, SPEED).add(Attributes.FOLLOW_RANGE, 48.0).add(Attributes.STEP_HEIGHT, 1.0);
	}

	/** A Jack-o'-Straw climbs out at {@code at}, one of {@code king}'s, turned toward his post. */
	public static JackOStrawEntity climb(ServerLevel level, PumpkinKingEntity king, Vec3 at) {
		JackOStrawEntity jack = new JackOStrawEntity(JugcraftPumpkinKing.JACK_O_STRAW, level);
		jack.king = king.getUUID();
		jack.climbTicks = CLIMB_TICKS;
		jack.entityData.set(ACTION, Action.CLIMB.ordinal());
		Vec3 to = king.centre().subtract(at);
		jack.snapTo(at.x, at.y, at.z, (float) (Math.toDegrees(Math.atan2(to.z, to.x)) - 90.0), 0.0F);
		level.addFreshEntity(jack);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.HAY_BLOCK.defaultBlockState()), at.x, at.y + 0.5, at.z, 12, 0.3,
				0.4, 0.3, 0.05);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.CROP_BREAK, SoundSource.HOSTILE, 1.0F, 1.3F);
		return jack;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
	}

	/** The King it serves, if he is still about. */
	public @Nullable PumpkinKingEntity king(ServerLevel level) {
		return king != null && level.getEntity(king) instanceof PumpkinKingEntity found && found.isAlive() ? found : null;
	}

	public Action action() {
		int id = entityData.get(ACTION);
		return id >= 0 && id < Action.values().length ? Action.values()[id] : Action.IDLE;
	}

	private void setAction(Action action) {
		entityData.set(ACTION, action.ordinal());
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		PumpkinKingEntity master = king(level);
		if (master == null || !master.awake()) {
			crumble(level);
			return;
		}
		if (swipeIn > 0) {
			swipeIn--;
		}
		if (climbTicks > 0) {
			climbTicks--;
			getNavigation().stop();
			setAction(climbTicks == 0 ? Action.IDLE : Action.CLIMB);
			return;
		}
		if (master.bereft()) {
			setAggressive(false);
			getNavigation().stop();
			setAction(Action.DAZED);
			return;
		}
		if (swipeShown > 0 && --swipeShown == 0 || action() == Action.DAZED) {
			setAction(Action.IDLE);
		}
		Player target = master.jackTarget(level, this);
		if (target == null || distanceToSqr(target) > 48.0 * 48.0) {
			setAggressive(false);
			getNavigation().stop();
			return;
		}
		setAggressive(true);
		getLookControl().setLookAt(target);
		if (distanceTo(target) <= REACH + target.getBbWidth() / 2.0) {
			getNavigation().stop();
			if (swipeIn == 0) {
				swipe(level, target);
			}
		} else if (tickCount % 10 == 0 || getNavigation().isDone()) {
			getNavigation().moveTo(target, 1.0);
		}
	}

	/** It swipes at {@code target}: {@value #DAMAGE}, times {@code lairs.boss_damage}. Returns whether it hurt them. */
	public boolean swipe(ServerLevel level, Player target) {
		swipeIn = SWIPE_COOLDOWN;
		swipeShown = SWIPE_TICKS;
		setAction(Action.SWIPE);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.6F, 1.6F);
		return target.hurtServer(level, damageSources().mobAttack(this), LairBosses.damage(DAMAGE));
	}

	/** It crumbles to straw. */
	public void crumble(ServerLevel level) {
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.HAY_BLOCK.defaultBlockState()), getX(), getY() + 0.6, getZ(), 16, 0.3,
				0.4, 0.3, 0.05);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.CROP_BREAK, SoundSource.HOSTILE, 1.0F, 0.9F);
		discard();
	}

	/** Straw burns: fire does {@value #FIRE} times; climbing out it cannot be hurt; whoever hurts it takes part in his fight. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (climbTicks > 0 && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return false;
		}
		float dealt = PumpkinKingEntity.fiery(source) ? amount * FIRE : amount;
		boolean hurt = super.hurtServer(level, source, dealt);
		PumpkinKingEntity master = king(level);
		if (hurt && master != null && Encounter.striker(source) instanceof ServerPlayer player) {
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
		return PumpkinKingEntity.sound("block.grass.step", SoundEvents.CROP_BREAK);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return PumpkinKingEntity.sound("block.grass.break", SoundEvents.CROP_BREAK);
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.CROP_BREAK;
	}

	@Override
	public float getVoicePitch() {
		return 1.3F + random.nextFloat() * 0.2F;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ACTION, Action.CLIMB.ordinal());
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (king != null) {
			output.store("king", UUIDUtil.CODEC, king);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		king = input.read("king", UUIDUtil.CODEC).orElse(null);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<JackOStrawEntity>("main", 2, test -> {
			JackOStrawEntity jack = test.animatable();
			if (jack.isDeadOrDying()) {
				return test.setAndContinue(CRUMBLE);
			}
			Action action = jack.action();
			return test.setAndContinue(action == Action.IDLE && test.isMoving() ? WALK : CLIPS[action.ordinal()]);
		}));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
