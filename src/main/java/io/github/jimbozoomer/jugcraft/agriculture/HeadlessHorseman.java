package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Headless Horseman: a headless rider on a black horse, summoned at midnight during the Halloween event
 * ({@link HorsemanSummoning}). He charges and strikes, and throws {@link FlamingPumpkin}s at whoever he hunts
 * ({@value #THROW_RANGE} blocks at most). At half health he is enraged: faster, throwing three at a time, twice as
 * often. He keeps to his arena, {@value #ARENA_RADIUS} blocks around where he was summoned (he rides back, and
 * lets go of a target that runs far beyond it), and shows a boss bar. He remembers who summoned him and goes back for
 * them whenever they are in his arena.
 *
 * <p>He rides off, leaving nothing, at dawn, when the event ends, or when nobody has been within
 * {@value #LEAVE_RANGE} blocks of his arena for {@value #LONELY_TICKS} ticks. Killed by a player he drops his
 * lantern and cloak (loot table {@code jugcraft:entities/headless_horseman}), and everyone near earns the Headless
 * Horseman advancement. He is fire-proof, saved with the world (his arena, rage and loneliness too), and never
 * appears in peaceful.
 */
public class HeadlessHorseman extends Monster implements RangedAttackMob {
	public static final int ARENA_RADIUS = 32;
	public static final int LEAVE_RANGE = 48;
	public static final int LONELY_TICKS = 600;
	public static final int THROW_COOLDOWN = 60;
	public static final int ENRAGED_THROW_COOLDOWN = 30;
	public static final double THROW_RANGE = 28.0;
	public static final double MIN_THROW_RANGE = 4.0;
	public static final double ENRAGED_SPEED_BONUS = 0.25;
	public static final int MAX_HEALTH = 160;

	private final ServerBossEvent bossBar = new ServerBossEvent(UUID.randomUUID(),
			Component.translatable("entity.jugcraft.headless_horseman"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
	private BlockPos home;
	/** Whoever summoned him: he goes for them whenever they are in his arena and can be attacked. */
	private @Nullable UUID quarry;
	private boolean enraged;
	private int lonely;
	private int throwCooldown = THROW_COOLDOWN;

	public HeadlessHorseman(EntityType<? extends HeadlessHorseman> type, Level level) {
		super(type, level);
		xpReward = 50;
		bossBar.setDarkenScreen(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, MAX_HEALTH).add(Attributes.MOVEMENT_SPEED, 0.32)
				.add(Attributes.ATTACK_DAMAGE, 9.0).add(Attributes.ARMOR, 8.0).add(Attributes.FOLLOW_RANGE, 40.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.8).add(Attributes.STEP_HEIGHT, 1.1);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	/** The centre of his arena: where he was summoned. */
	public BlockPos home() {
		return home == null ? blockPosition() : home;
	}

	public void setHome(BlockPos pos) {
		home = pos.immutable();
	}

	public boolean enraged() {
		return enraged;
	}

	public Optional<UUID> quarry() {
		return Optional.ofNullable(quarry);
	}

	/** Sets who he hunts, and goes for them now if he can (a player who has only just appeared cannot be attacked yet). */
	public void hunt(Player player) {
		quarry = player.getUUID();
		setTarget(player);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (home == null) {
			home = blockPosition();
		}
		bossBar.setProgress(getHealth() / getMaxHealth());
		if (tickCount % 20 == 0) {
			lonely = level.getNearestPlayer(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, LEAVE_RANGE, true) == null ? lonely + 20 : 0;
			if (shouldLeave(level.getOverworldClockTime())) {
				rideOff(level);
				return;
			}
		}
		keepToArena();
		if (getTarget() == null && quarry != null && tickCount % 20 == 0) {
			Player summoner = level.getPlayerByUUID(quarry);
			if (summoner != null && summoner.distanceToSqr(Vec3.atBottomCenterOf(home())) <= (ARENA_RADIUS + 8) * (ARENA_RADIUS + 8)) {
				setTarget(summoner);
			}
		}
		updateRage(level);
		LivingEntity target = getTarget();
		if (--throwCooldown <= 0 && target != null && target.isAlive()) {
			double distance = distanceTo(target);
			if (distance >= MIN_THROW_RANGE && distance <= THROW_RANGE && hasLineOfSight(target)) {
				performRangedAttack(target, 1.0F);
				throwCooldown = enraged ? ENRAGED_THROW_COOLDOWN : THROW_COOLDOWN;
			}
		}
	}

	/** At dawn ({@code dayTime} on the overworld clock), when the event ends, or after a long while with nobody near, he leaves. */
	public boolean shouldLeave(long dayTime) {
		return !Wisps.night(dayTime) || lonely >= LONELY_TICKS;
	}

	/** How long nobody has been near his arena, in ticks. */
	public int lonely() {
		return lonely;
	}

	public void setLonely(int ticks) {
		lonely = ticks;
	}

	/** He rides back into his arena, and gives up on a target that has run far beyond it. */
	private void keepToArena() {
		BlockPos centre = home();
		double away = distanceToSqr(Vec3.atBottomCenterOf(centre));
		if (away > ARENA_RADIUS * ARENA_RADIUS * 2.25) {
			teleportTo(centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5);
			return;
		}
		LivingEntity target = getTarget();
		if (target != null && target.distanceToSqr(Vec3.atBottomCenterOf(centre)) > (ARENA_RADIUS + 8) * (ARENA_RADIUS + 8)) {
			setTarget(null);
		}
		if (away > ARENA_RADIUS * ARENA_RADIUS && getTarget() == null) {
			getNavigation().moveTo(centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5, 1.2);
		}
	}

	/** At half health or less he is enraged, once. Returns whether he is enraged. */
	public boolean updateRage(ServerLevel level) {
		if (!enraged && getHealth() <= getMaxHealth() / 2.0F) {
			enrage(level);
		}
		return enraged;
	}

	private void enrage(ServerLevel level) {
		enraged = true;
		var speed = getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.addOrReplacePermanentModifier(new AttributeModifier(Jugcraft.id("horseman_rage"), ENRAGED_SPEED_BONUS,
					AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		}
		level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1.5, getZ(), 40, 1.0, 1.0, 1.0, 0.05);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 2.0F, 0.5F);
		bossBar.setColor(BossEvent.BossBarColor.YELLOW);
	}

	/** Throws a flaming pumpkin at {@code target} (three in a fan when enraged). */
	@Override
	public void performRangedAttack(LivingEntity target, float power) {
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		int count = enraged ? 3 : 1;
		for (int i = 0; i < count; i++) {
			FlamingPumpkin pumpkin = new FlamingPumpkin(level, this);
			double dx = target.getX() - getX();
			double dz = target.getZ() - getZ();
			double dy = target.getY(0.5) - pumpkin.getY();
			double flat = Math.sqrt(dx * dx + dz * dz);
			double spread = (i - (count - 1) / 2.0) * 0.25;
			pumpkin.shoot(dx - dz * spread, dy + flat * 0.2, dz + dx * spread, 1.1F, 3.0F);
			level.addFreshEntity(pumpkin);
		}
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.0F, 0.7F);
	}

	/** Leaves in a puff of smoke and a cackle, with nothing left behind. */
	public void rideOff(ServerLevel level) {
		level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0, getZ(), 40, 0.8, 1.0, 0.8, 0.02);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 2.0F, 0.6F);
		for (ServerPlayer player : level.getPlayers(p -> p.distanceToSqr(this) < LEAVE_RANGE * LEAVE_RANGE)) {
			player.sendSystemMessage(Component.translatable("message.jugcraft.horseman.rides_off"));
		}
		discard();
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level) {
			// The advancement is for beating him: only when a player struck the killing blow (or shot it).
			boolean byPlayer = source.getEntity() instanceof Player;
			for (ServerPlayer player : level.getPlayers(p -> p.distanceToSqr(this) < LEAVE_RANGE * LEAVE_RANGE)) {
				player.sendSystemMessage(Component.translatable("message.jugcraft.horseman.defeated"));
				if (byPlayer) {
					TrickOrTreat.award(player, "headless_horseman");
				}
			}
		}
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		bossBar.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		bossBar.removePlayer(player);
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SKELETON_HORSE_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SKELETON_HORSE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SKELETON_HORSE_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.HORSE_GALLOP, 0.2F, 0.9F);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.store("home", BlockPos.CODEC, home());
		if (quarry != null) {
			output.store("quarry", UUIDUtil.CODEC, quarry);
		}
		output.putBoolean("enraged", enraged);
		output.putInt("lonely", lonely);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		home = input.read("home", BlockPos.CODEC).orElse(null);
		quarry = input.read("quarry", UUIDUtil.CODEC).orElse(null);
		enraged = input.getBooleanOr("enraged", false);
		lonely = input.getIntOr("lonely", 0);
		if (enraged) {
			bossBar.setColor(BossEvent.BossBarColor.YELLOW);
		}
	}
}
