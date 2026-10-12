package io.github.jimbozoomer.jugcraft.lair.yeti;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.LoopType;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairBosses;
import io.github.jimbozoomer.jugcraft.lair.LairExitBlock;
import io.github.jimbozoomer.jugcraft.lair.LairInstance;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Yeti King (docs/features/yeti-king.md): the Glacier Hall's boss, a white ape twice a player's height with a crown of
 * blue ice. He waits slumped on his throne, and wakes when someone who may fight him steps onto the frozen lake or strikes
 * him: he roars and leaps down onto the ice, his health scaled for the party.
 *
 * <p>His rule: the snow is his. His Ground Slam blasts the lake's drift snow bare to glare ice round where he lands, for
 * {@value #GLARE_TICKS} ticks before it drifts back; never the trampled snow round a column, at the ramp's foot, before his
 * dais or on the dens' paths, which is another block. Phase 1, the Hunt: Maul Swipe, Boulder Throw, Ground Slam, Frost
 * Breath and Avalanche Charge (into an ice column he stuns himself and takes a third more damage). At half health, the
 * King's Roar: he bounds back onto his dais and roars, unhurt, a blizzard chills everyone near and two whelps come out of
 * the dens. Phase 2, the Blizzard: no more Frost Breath, but Icicle Fall, Glacial Spikes and Kin Call. Below a fifth, the
 * Fury of the Peaks: his crown blazes, every cooldown is a third shorter, his slam bares a wider ring, and a charge that
 * does not stun him is followed at once by another.
 *
 * <p>Every attack has a wind-up you can read, a strike and a recovery ({@link Attack}); the numbers are here, in one place,
 * and in tools/yeti_king.py. Each participant (whoever hurt him or his whelps) gets their own loot ({@link YetiKingLoot});
 * left alone he climbs back to his throne, healed, and the snow drifts back. Bound to a lair instance he vanishes with
 * it; one with no instance (a test's) keeps to the hall laid out from the origin he was given. He walks on his own snow:
 * he does not slide on his ice, freeze, fall or stick in webs.
 */
public class YetiKingEntity extends Monster implements GeoEntity {
	// ---------------------------------------------------------------- numbers (tools/yeti_king.py)
	public static final float HEALTH = 420.0F;
	public static final double ARMOR = 10.0;
	public static final double LEASH = 30.0;
	public static final double PARTY_STEP = 0.5;
	public static final double PARTY_MAX = 2.5;
	public static final int WAKE_TICKS = 40;
	public static final int ABANDON_TICKS = 200;
	public static final int GLOBAL_COOLDOWN = 20;
	public static final double SPEED = 0.24;
	public static final double KEEP_DISTANCE = 2.5;
	public static final float ROAR_AT = 0.5F;
	public static final float FURY_AT = 0.2F;
	public static final double FURY_COOLDOWN = 0.6667;
	public static final float SWIPE_DAMAGE = 12.0F;
	public static final double SWIPE_REACH = 4.0;
	public static final double SWIPE_HALF_ANGLE = 60.0;
	public static final double SWIPE_KNOCKBACK = 1.0;
	public static final float BOULDER_DAMAGE = 10.0F;
	public static final double BOULDER_RADIUS = 2.5;
	public static final float SLAM_DAMAGE = 14.0F;
	public static final double SLAM_RADIUS = 3.0;
	public static final double SLAM_PUSH_RADIUS = 6.0;
	public static final double SLAM_KNOCKBACK = 1.4;
	public static final double RING = 7.0;
	public static final double FURY_RING = 10.0;
	public static final int GLARE_TICKS = 240;
	public static final double BREATH_REACH = 6.0;
	public static final double BREATH_HALF_ANGLE = 30.0;
	public static final float BREATH_DAMAGE = 2.0F;
	public static final int BREATH_EVERY = 5;
	public static final int BREATH_FROST = 30;
	public static final double CHARGE_SPEED = 0.7;
	public static final float CHARGE_DAMAGE = 12.0F;
	public static final double CHARGE_KNOCKBACK = 2.0;
	public static final int STUN_TICKS = 60;
	public static final double STUN_TAKEN = 1.3333;
	public static final int ROAR_TICKS = 60;
	public static final int BLIZZARD_FROST = 120;
	public static final int ICICLES = 8;
	public static final double ICICLE_AREA = 3.0;
	public static final int SPIKE_LENGTH = 12;
	public static final float SPIKE_DAMAGE = 10.0F;
	public static final double SPIKE_LIFT = 0.8;
	public static final int KIN = 2;
	public static final int MAX_WHELPS = 4;
	public static final int EXPERIENCE = 300;
	/** How long a leap onto or off his dais takes, and how high a leap rises over the straight line. */
	private static final int LEAP_TICKS = 16;
	private static final double LEAP_RISE = 4.0;
	/** How near his path a player must be for his charge to strike them, and how near a column's foot stuns him. */
	private static final double CHARGE_REACH = 1.6;
	private static final double COLUMN_REACH = GlacierHall.COLUMN_RADIUS + GlacierHall.COLUMN_FOOT + 1.2;
	/** The most frost he leaves on anyone: a little past freezing them through, so the freeze bites for a moment. */
	private static final int FROST_CAP = 200;

	/**
	 * His attacks: wind-up, the active part and recovery in ticks, the cooldown, the target's nearest and farthest distance,
	 * and the phases he uses it in (the Hunt, the Blizzard, the Fury of the Peaks). The wind-up is what players read; the
	 * strike lands as it ends.
	 */
	public enum Attack {
		MAUL_SWIPE(12, 2, 10, 40, 0.0, 4.5, true, true, true),
		BOULDER_THROW(16, 1, 10, 120, 6.0, 24.0, true, true, true),
		GROUND_SLAM(16, 16, 14, 200, 4.0, 16.0, true, true, true),
		FROST_BREATH(20, 30, 10, 160, 0.0, 6.0, true, false, false),
		AVALANCHE_CHARGE(16, 24, 10, 240, 6.0, 28.0, true, true, true),
		ICICLE_FALL(20, 16, 10, 160, 0.0, 30.0, false, true, true),
		GLACIAL_SPIKES(16, 12, 10, 180, 3.0, 14.0, false, true, true),
		KIN_CALL(20, 1, 10, 300, 0.0, 40.0, false, true, true);

		public final int windup;
		public final int active;
		public final int recovery;
		public final int cooldown;
		public final double near;
		public final double far;
		public final boolean hunt;
		public final boolean blizzard;
		public final boolean fury;

		Attack(int windup, int active, int recovery, int cooldown, double near, double far, boolean hunt, boolean blizzard, boolean fury) {
			this.windup = windup;
			this.active = active;
			this.recovery = recovery;
			this.cooldown = cooldown;
			this.near = near;
			this.far = far;
			this.hunt = hunt;
			this.blizzard = blizzard;
			this.fury = fury;
		}

		public int length() {
			return windup + active + recovery;
		}
	}

	/** The fight's phases. */
	public enum Phase {
		WAITING, WAKING, HUNT, ROARING, BLIZZARD, FURY
	}

	/** What his body is doing, synced for his animations. */
	public enum Action {
		THRONE, WAKE, LEAP, IDLE, SWIPE_WINDUP, SWIPE, BOULDER_WINDUP, THROW, SLAM_WINDUP, SLAM, BREATH_WINDUP, BREATH,
		CHARGE_WINDUP, CHARGE, STUNNED, ROAR, ICICLES, SPIKES_WINDUP, SPIKES, CALL
	}

	/** A patch of the lake his slam has bared: its cells (drift snow before), and when the snow drifts back. */
	private record Patch(List<BlockPos> cells, long driftAt) {
	}

	private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(YetiKingEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> FURIOUS = SynchedEntityData.defineId(YetiKingEntity.class, EntityDataSerializers.BOOLEAN);
	private static final RawAnimation[] BODY = new RawAnimation[Action.values().length];
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.yeti_king.walk");
	private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.yeti_king.death");
	private static final RawAnimation CROWN_CALM = RawAnimation.begin().thenLoop("animation.yeti_king.crown_calm");
	private static final RawAnimation CROWN_BLAZING = RawAnimation.begin().thenLoop("animation.yeti_king.crown_blazing");

	static {
		String prefix = "animation.yeti_king.";
		BODY[Action.THRONE.ordinal()] = RawAnimation.begin().thenLoop(prefix + "throne");
		BODY[Action.WAKE.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "wake");
		BODY[Action.LEAP.ordinal()] = RawAnimation.begin().thenLoop(prefix + "leap");
		BODY[Action.IDLE.ordinal()] = RawAnimation.begin().thenLoop(prefix + "idle");
		BODY[Action.SWIPE_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "swipe_windup");
		BODY[Action.SWIPE.ordinal()] = RawAnimation.begin().then(prefix + "swipe", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.BOULDER_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "boulder_windup");
		BODY[Action.THROW.ordinal()] = RawAnimation.begin().then(prefix + "throw", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.SLAM_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "slam_windup");
		BODY[Action.SLAM.ordinal()] = RawAnimation.begin().then(prefix + "slam", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.BREATH_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "breath_windup");
		BODY[Action.BREATH.ordinal()] = RawAnimation.begin().thenLoop(prefix + "breath");
		BODY[Action.CHARGE_WINDUP.ordinal()] = RawAnimation.begin().thenLoop(prefix + "charge_windup");
		BODY[Action.CHARGE.ordinal()] = RawAnimation.begin().thenLoop(prefix + "charge");
		BODY[Action.STUNNED.ordinal()] = RawAnimation.begin().thenLoop(prefix + "stunned");
		BODY[Action.ROAR.ordinal()] = RawAnimation.begin().thenLoop(prefix + "roar");
		BODY[Action.ICICLES.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "icicles");
		BODY[Action.SPIKES_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "spikes_windup");
		BODY[Action.SPIKES.ordinal()] = RawAnimation.begin().then(prefix + "spikes", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.CALL.ordinal()] = RawAnimation.begin().thenLoop(prefix + "call");
	}

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private final ServerBossEvent bossBar = new ServerBossEvent(UUID.randomUUID(), Component.translatable("entity.jugcraft.yeti_king"),
			BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
	/** The lair instance he belongs to (null for one put down by a test or a command), and the hall's origin. */
	private @Nullable UUID instance;
	private int slot = -1;
	private BlockPos origin = BlockPos.ZERO;
	private Phase phase = Phase.WAITING;
	private int phaseTicks;
	private @Nullable Attack attack;
	private int attackTicks;
	private int globalCooldown;
	private final int[] cooldowns = new int[Attack.values().length];
	private @Nullable UUID foe;
	private int abandoned;
	private int stunned;
	private final Map<UUID, Float> damageBy = new HashMap<>();
	private final List<UUID> whelps = new ArrayList<>();
	/** His thrown and fallen things (boulders, icicles, spikes), gone with him. */
	private final List<UUID> things = new ArrayList<>();
	private final List<Patch> bared = new ArrayList<>();
	/** A leap under way: from, to, and the tick it began (phase ticks or attack ticks, as the leap's owner counts). */
	private Vec3 leapFrom = Vec3.ZERO;
	private Vec3 leapTo = Vec3.ZERO;
	private Vec3 charge = Vec3.ZERO;
	private boolean chained;
	private final Set<UUID> trampled = new HashSet<>();
	private final List<Vec3> icicles = new ArrayList<>();
	private final List<Vec3> spikes = new ArrayList<>();
	/** Set when he is loaded from a save: the snow drifts back over what he had bared (saved with him) on his first tick. */
	private boolean driftOnLoad;

	public YetiKingEntity(EntityType<? extends YetiKingEntity> type, Level level) {
		super(type, level);
		xpReward = 0;
		setNoGravity(true);
		setPersistenceRequired();
		bossBar.setDarkenScreen(true);
		bossBar.setVisible(false);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.ARMOR, ARMOR)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 48.0).add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.ATTACK_DAMAGE, SWIPE_DAMAGE);
	}

	/**
	 * Seats him on the throne of {@code instance}'s Glacier Hall. Called as the instance is placed. Placing the template puts
	 * back only its own blocks, so the Grey Mist a fallen King opened in this slot before still stands where the template has
	 * none: it goes first.
	 */
	public static YetiKingEntity summon(ServerLevel level, LairInstance instance) {
		BlockPos origin = instance.origin();
		for (BlockPos at : exitCells(centre(origin))) {
			if (level.getBlockState(at).is(JugcraftLairs.LAIR_EXIT)) {
				level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		return summon(level, origin, instance);
	}

	/** Seats him on the throne of the hall laid out from {@code origin} (tests pass no instance). */
	public static YetiKingEntity summon(ServerLevel level, BlockPos origin, @Nullable LairInstance instance) {
		YetiKingEntity king = new YetiKingEntity(JugcraftYeti.YETI_KING, level);
		king.origin = origin.immutable();
		if (instance != null) {
			king.instance = instance.id;
			king.slot = instance.slot;
		}
		Vec3 throne = king.throne();
		king.snapTo(throne.x, throne.y, throne.z, 0.0F, 0.0F);
		king.face(0.0F);
		level.addFreshEntity(king);
		return king;
	}

	// ---------------------------------------------------------------- where things are

	/** The lake's centre, where it is stood on, for the hall laid out from {@code origin}. */
	public static Vec3 centre(BlockPos origin) {
		return GlacierHall.centre().add(origin.getX(), origin.getY(), origin.getZ());
	}

	public Vec3 centre() {
		return centre(origin);
	}

	public BlockPos origin() {
		return origin;
	}

	private Vec3 at(Vec3 template) {
		return template.add(origin.getX(), origin.getY(), origin.getZ());
	}

	/** Where he waits on his throne. */
	public Vec3 throne() {
		return at(GlacierHall.throne());
	}

	/** The height of the lake's surface, where he and the players stand. */
	public double floorY() {
		return origin.getY() + GlacierHall.LAKE + 1;
	}

	// ---------------------------------------------------------------- state

	public Phase phase() {
		return phase;
	}

	public @Nullable Attack attack() {
		return attack;
	}

	public int attackTicks() {
		return attackTicks;
	}

	public int stunnedTicks() {
		return stunned;
	}

	public Action action() {
		int id = entityData.get(ACTION);
		return id >= 0 && id < Action.values().length ? Action.values()[id] : Action.IDLE;
	}

	private void setAction(Action action) {
		entityData.set(ACTION, action.ordinal());
	}

	public boolean furious() {
		return entityData.get(FURIOUS);
	}

	public @Nullable UUID instanceId() {
		return instance;
	}

	/** Whether he is fighting (on the lake, not waiting, waking or roaring). */
	public boolean fighting() {
		return phase == Phase.HUNT || phase == Phase.BLIZZARD || phase == Phase.FURY;
	}

	/** Damage he deals: the attack's own, times {@code lairs.boss_damage} ({@link LairBosses}). */
	public static float damage(float base) {
		return LairBosses.damage(base);
	}

	/** His health for a party of {@code players}: half as much again for each beyond the first, at most 2.5 times. */
	public static double partyScale(int players) {
		return LairBosses.partyScale(players, PARTY_STEP, PARTY_MAX);
	}

	/** A cooldown as it is in this phase: a third shorter in the Fury of the Peaks. */
	public int cooldown(int ticks) {
		return furious() ? (int) Math.round(ticks * FURY_COOLDOWN) : ticks;
	}

	/** How far his slam bares the snow round where he lands. */
	public double ring() {
		return furious() ? FURY_RING : RING;
	}

	// ---------------------------------------------------------------- the server's tick

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (!bound(level)) {
			discard();
			return;
		}
		if (driftOnLoad) {
			driftOnLoad = false;
			driftAll(level);
		}
		resetFallDistance();
		setTicksFrozen(0);
		bossBar.setProgress(getHealth() / getMaxHealth());
		prune(level);
		driftDue(level);
		for (int i = 0; i < cooldowns.length; i++) {
			if (cooldowns[i] > 0) {
				cooldowns[i]--;
			}
		}
		phaseTicks++;
		switch (phase) {
			case WAITING -> waiting(level);
			case WAKING -> waking(level);
			case HUNT, BLIZZARD, FURY -> fight(level);
			case ROARING -> roaring(level);
		}
	}

	/** Whether his instance (if he has one) is still open. */
	private boolean bound(ServerLevel level) {
		return instance == null || lairInstance(level) != null;
	}

	private @Nullable LairInstance lairInstance(ServerLevel level) {
		Lair lair = Lair.of(level.dimension());
		LairInstance open = lair == null || instance == null ? null : Lairs.instance(lair, slot);
		return open != null && open.id.equals(instance) ? open : null;
	}

	private void waiting(ServerLevel level) {
		setDeltaMovement(Vec3.ZERO);
		Vec3 throne = throne();
		if (position().distanceToSqr(throne) > 0.01) {
			setPos(throne.x, throne.y, throne.z);
		}
		face(0.0F);
		setAction(Action.THRONE);
		if (tickCount % 10 == 0 && intruder(level) != null) {
			wake(level);
		}
	}

	/** Someone who may fight him, standing on the lake. */
	private @Nullable Player intruder(ServerLevel level) {
		Vec3 centre = centre();
		for (Player player : level.players()) {
			double dx = player.getX() - centre.x;
			double dz = player.getZ() - centre.z;
			if (LairBosses.eligible(player) && dx * dx + dz * dz <= GlacierHall.LAKE_RADIUS * GlacierHall.LAKE_RADIUS
					&& Math.abs(player.getY() - centre.y) < 3.0) {
				return player;
			}
		}
		return null;
	}

	/** He rises from his throne with a roar: his health scaled for the players near the lake, his bar shown. */
	public void wake(ServerLevel level) {
		if (phase != Phase.WAITING) {
			return;
		}
		double scale = partyScale(Math.max(1, nearby(level).size()));
		AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
		if (health != null) {
			health.setBaseValue(HEALTH * scale);
		}
		setHealth(getMaxHealth());
		phase = Phase.WAKING;
		phaseTicks = 0;
		setAction(Action.WAKE);
		bossBar.setVisible(true);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 3.0F, 0.7F);
		tell(level, "message.jugcraft.yeti_king.wakes");
	}

	/** Waking: half the time roaring on his throne, then a leap down onto the lake below his dais. */
	private void waking(ServerLevel level) {
		int half = WAKE_TICKS / 2;
		if (phaseTicks < half) {
			setDeltaMovement(Vec3.ZERO);
			if (phaseTicks % 4 == 0) {
				level.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 3.0, getZ(), 6, 1.2, 0.6, 1.2, 0.05);
			}
			return;
		}
		if (phaseTicks == half) {
			setAction(Action.LEAP);
			leapFrom = position();
			leapTo = at(GlacierHall.below());
		}
		leap(phaseTicks - half, WAKE_TICKS - half);
		if (phaseTicks >= WAKE_TICKS) {
			landed(level);
			phase = Phase.HUNT;
			phaseTicks = 0;
			globalCooldown = GLOBAL_COOLDOWN;
			setAction(Action.IDLE);
		}
	}

	/** One tick of a leap from {@link #leapFrom} to {@link #leapTo}, {@code t} ticks into its {@code length}. */
	private void leap(int t, int length) {
		double share = Math.min(1.0, (t + 1) / (double) length);
		Vec3 along = leapFrom.add(leapTo.subtract(leapFrom).scale(share));
		Vec3 goal = along.add(0.0, LEAP_RISE * 4.0 * share * (1.0 - share), 0.0);
		setDeltaMovement(goal.subtract(position()));
		Vec3 to = leapTo.subtract(leapFrom);
		if (to.x * to.x + to.z * to.z > 1.0E-4) {
			face((float) (Math.toDegrees(Math.atan2(to.z, to.x)) - 90.0));
		}
	}

	/** He comes down on the ice with a thud. */
	private void landed(ServerLevel level) {
		teleportTo(leapTo.x, leapTo.y, leapTo.z);
		level.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.2, getZ(), 30, 1.5, 0.2, 1.5, 0.1);
		level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.2, getZ(), 10, 1.2, 0.1, 1.2, 0.02);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.HOSTILE, 1.5F, 0.7F);
	}

	/** Players near enough to his lake to be fighting (or about to). */
	public List<ServerPlayer> nearby(ServerLevel level) {
		List<ServerPlayer> out = new ArrayList<>();
		Vec3 centre = centre();
		for (ServerPlayer player : level.players()) {
			if (LairBosses.eligible(player) && player.position().distanceTo(centre) <= LEASH + 8.0) {
				out.add(player);
			}
		}
		return out;
	}

	// ---------------------------------------------------------------- fighting

	private void fight(ServerLevel level) {
		keepToTheHall(level);
		if (stunned > 0) {
			stunned--;
			setDeltaMovement(0.0, floorY() - getY(), 0.0);
			setAction(stunned > 0 ? Action.STUNNED : Action.IDLE);
			if (stunned % 6 == 0) {
				level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 3.6, getZ(), 4, 0.4, 0.1, 0.4, 0.05);
			}
			return;
		}
		Player target = target(level);
		if (target == null) {
			attack = null;
			setAction(Action.IDLE);
			move(centre(), SPEED);
			if (++abandoned >= ABANDON_TICKS) {
				reset(level);
			}
			return;
		}
		abandoned = 0;
		if (attack == null && phase == Phase.HUNT && getHealth() <= getMaxHealth() * ROAR_AT) {
			startRoar(level);
			return;
		}
		if (attack == null && phase == Phase.BLIZZARD && getHealth() <= getMaxHealth() * FURY_AT) {
			startFury(level);
			return;
		}
		if (attack != null) {
			attackTicks++;
			perform(level, target);
			return;
		}
		face(target);
		if (globalCooldown > 0) {
			globalCooldown--;
			approach(target);
			return;
		}
		Attack next = choose(target);
		if (next == null) {
			approach(target);
		} else {
			begin(level, next, target);
		}
	}

	/** Strayed farther than his leash from the lake's centre, or fallen below it, he bounds back to the lake. */
	private void keepToTheHall(ServerLevel level) {
		Vec3 centre = centre();
		double dx = getX() - centre.x;
		double dz = getZ() - centre.z;
		if (dx * dx + dz * dz <= LEASH * LEASH && getY() > floorY() - 2.0) {
			return;
		}
		level.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 1.5, getZ(), 20, 0.8, 1.0, 0.8, 0.05);
		attack = null;
		teleportTo(centre.x, floorY(), centre.z);
		level.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 1.5, getZ(), 20, 0.8, 1.0, 0.8, 0.05);
	}

	/** His target: the one he has while they are still in reach of his lake, else the nearest who may fight him. */
	private @Nullable Player target(ServerLevel level) {
		Vec3 centre = centre();
		if (foe != null) {
			Player held = level.getPlayerByUUID(foe);
			if (held != null && LairBosses.eligible(held) && held.position().distanceTo(centre) <= LEASH + 8.0) {
				return held;
			}
		}
		Player nearest = null;
		double best = Double.MAX_VALUE;
		for (ServerPlayer player : nearby(level)) {
			double distance = player.distanceToSqr(this);
			if (distance < best) {
				best = distance;
				nearest = player;
			}
		}
		foe = nearest == null ? null : nearest.getUUID();
		return nearest;
	}

	/** Lopes toward a point {@value #KEEP_DISTANCE} blocks short of {@code target}. */
	private void approach(Player target) {
		Vec3 to = target.position().subtract(position());
		double flat = Math.sqrt(to.x * to.x + to.z * to.z);
		Vec3 goal = flat > KEEP_DISTANCE + 0.5
				? new Vec3(target.getX() - to.x / flat * KEEP_DISTANCE, floorY(), target.getZ() - to.z / flat * KEEP_DISTANCE)
				: new Vec3(getX(), floorY(), getZ());
		move(goal, SPEED);
	}

	/** Moves toward {@code goal} at {@code speed}, kept on the lake's surface and within his leash. */
	private void move(Vec3 goal, double speed) {
		Vec3 centre = centre();
		double x = goal.x;
		double z = goal.z;
		double dx = x - centre.x;
		double dz = z - centre.z;
		double flat = Math.sqrt(dx * dx + dz * dz);
		if (flat > LEASH) {
			x = centre.x + dx / flat * LEASH;
			z = centre.z + dz / flat * LEASH;
		}
		Vec3 to = new Vec3(x - getX(), 0.0, z - getZ());
		double length = to.length();
		Vec3 step = length < 0.05 ? Vec3.ZERO : to.scale(Math.min(speed, length) / length);
		setDeltaMovement(step.x, floorY() - getY(), step.z);
	}

	private void face(Entity target) {
		double dx = target.getX() - getX();
		double dz = target.getZ() - getZ();
		if (dx * dx + dz * dz > 1.0E-4) {
			face((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
		}
	}

	private void face(float yaw) {
		setYRot(yaw);
		setYBodyRot(yaw);
		setYHeadRot(yaw);
	}

	/** The way he faces, flat. */
	public Vec3 facing() {
		double yaw = Math.toRadians(getYRot());
		return new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
	}

	/** Picks an attack he can use now on {@code target}, or null. */
	private @Nullable Attack choose(Player target) {
		double distance = target.distanceTo(this);
		List<Attack> ready = new ArrayList<>();
		for (Attack candidate : Attack.values()) {
			boolean phaseAllows = switch (phase) {
				case HUNT -> candidate.hunt;
				case BLIZZARD -> candidate.blizzard;
				case FURY -> candidate.fury;
				default -> false;
			};
			if (!phaseAllows || cooldowns[candidate.ordinal()] > 0 || distance < candidate.near || distance > candidate.far) {
				continue;
			}
			if (candidate == Attack.KIN_CALL && whelps.size() >= MAX_WHELPS) {
				continue;
			}
			ready.add(candidate);
			if (candidate == Attack.MAUL_SWIPE) {
				ready.add(candidate);  // up close, his fists come most often
				ready.add(candidate);
			}
		}
		return ready.isEmpty() ? null : ready.get(getRandom().nextInt(ready.size()));
	}

	/** Starts {@code next} on {@code target}: its wind-up begins. */
	public void begin(ServerLevel level, Attack next, Player target) {
		attack = next;
		attackTicks = 0;
		foe = target.getUUID();
		cooldowns[next.ordinal()] = cooldown(next.cooldown);
		setDeltaMovement(Vec3.ZERO);
		face(target);
		switch (next) {
			case MAUL_SWIPE -> {
				setAction(Action.SWIPE_WINDUP);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.POLAR_BEAR_WARNING, SoundSource.HOSTILE, 1.5F, 0.6F);
			}
			case BOULDER_THROW -> setAction(Action.BOULDER_WINDUP);
			case GROUND_SLAM -> {
				setAction(Action.SLAM_WINDUP);
				leapTo = landing(target.position());
			}
			case FROST_BREATH -> setAction(Action.BREATH_WINDUP);
			case AVALANCHE_CHARGE -> {
				setAction(Action.CHARGE_WINDUP);
				chained = false;
			}
			case ICICLE_FALL -> {
				setAction(Action.ICICLES);
				icicles.clear();
				for (int i = 0; i < ICICLES; i++) {
					double angle = getRandom().nextDouble() * Math.PI * 2.0;
					double reach = Math.sqrt(getRandom().nextDouble()) * ICICLE_AREA;
					icicles.add(new Vec3(target.getX() + Math.cos(angle) * reach, floorY(), target.getZ() + Math.sin(angle) * reach));
				}
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2.0F, 0.9F);
			}
			case GLACIAL_SPIKES -> {
				setAction(Action.SPIKES_WINDUP);
				spikes.clear();
				Vec3 to = target.position().subtract(position());
				double flat = Math.sqrt(to.x * to.x + to.z * to.z);
				Vec3 way = flat < 1.0E-3 ? facing() : new Vec3(to.x / flat, 0.0, to.z / flat);
				for (int i = 1; i <= SPIKE_LENGTH; i++) {
					spikes.add(new Vec3(getX() + way.x * (i + 1.0), floorY(), getZ() + way.z * (i + 1.0)));
				}
			}
			case KIN_CALL -> {
				setAction(Action.CALL);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.POLAR_BEAR_WARNING, SoundSource.HOSTILE, 3.0F, 0.5F);
			}
		}
	}

	private void perform(ServerLevel level, Player target) {
		Attack current = attack;
		if (current == null) {
			return;
		}
		int t = attackTicks;
		if (current != Attack.GROUND_SLAM && current != Attack.AVALANCHE_CHARGE) {
			setDeltaMovement(0.0, floorY() - getY(), 0.0);
		}
		switch (current) {
			case MAUL_SWIPE -> {
				if (t < current.windup) {
					face(target);
				} else if (t == current.windup) {
					setAction(Action.SWIPE);
					swipe(level);
				}
			}
			case BOULDER_THROW -> {
				if (t < current.windup) {
					face(target);
					if (t % 4 == 0) {
						level.sendParticles(ParticleTypes.ITEM_SNOWBALL, getX(), floorY() + 0.2, getZ(), 6, 0.8, 0.1, 0.8, 0.05);
					}
				} else if (t == current.windup) {
					setAction(Action.THROW);
					things.add(HurledBoulderEntity.hurl(level, this, target).getUUID());
				}
			}
			case GROUND_SLAM -> groundSlam(level, t);
			case FROST_BREATH -> frostBreath(level, target, t);
			case AVALANCHE_CHARGE -> charge(level, target, t);
			case ICICLE_FALL -> icicleFall(level, t - current.windup);
			case GLACIAL_SPIKES -> glacialSpikes(level, t - current.windup);
			case KIN_CALL -> {
				if (t == current.windup) {
					callKin(level, KIN);
				}
			}
		}
		if (attack == current && attackTicks >= current.length()) {
			attack = null;
			attackTicks = 0;
			globalCooldown = cooldown(GLOBAL_COOLDOWN);
			Action action = action();
			if (action != Action.SWIPE && action != Action.THROW && action != Action.SLAM && action != Action.SPIKES && action != Action.STUNNED) {
				setAction(Action.IDLE);
			}
		}
	}

	/** Maul Swipe: every player within {@value #SWIPE_REACH} blocks and {@value #SWIPE_HALF_ANGLE} degrees of where he faces. */
	public int swipe(ServerLevel level) {
		int hit = 0;
		Vec3 facing = facing();
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(SWIPE_REACH + 1.5, 2.0, SWIPE_REACH + 1.5))) {
			if (!LairBosses.eligible(player)) {
				continue;
			}
			Vec3 to = player.position().subtract(position());
			double flat = Math.sqrt(to.x * to.x + to.z * to.z);
			if (flat > SWIPE_REACH + getBbWidth() / 2.0 + player.getBbWidth() / 2.0 || Math.abs(to.y) > 3.0) {
				continue;
			}
			double cos = flat < 1.0E-3 ? 1.0 : (to.x * facing.x + to.z * facing.z) / flat;
			if (cos < Math.cos(Math.toRadians(SWIPE_HALF_ANGLE))) {
				continue;
			}
			if (player.hurtServer(level, damageSources().mobAttack(this), damage(SWIPE_DAMAGE))) {
				hit++;
				push(player, flat < 1.0E-3 ? facing : new Vec3(to.x / flat, 0.0, to.z / flat), SWIPE_KNOCKBACK, 0.3);
			}
		}
		Vec3 front = position().add(facing.scale(2.4)).add(0.0, 1.6, 0.0);
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, front.x, front.y, front.z, 2, 0.6, 0.2, 0.6, 0.0);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5F, 0.6F);
		return hit;
	}

	/** Pushes {@code player} along {@code way} (flat) by {@code strength}, and up by {@code up}, and tells their client. */
	static void push(ServerPlayer player, Vec3 way, double strength, double up) {
		player.push(way.x * strength, up, way.z * strength);
		player.connection.send(new ClientboundSetEntityMotionPacket(player));
	}

	/** Where his slam comes down: where the foe stood, kept within his leash. */
	private Vec3 landing(Vec3 at) {
		Vec3 centre = centre();
		double dx = at.x - centre.x;
		double dz = at.z - centre.z;
		double flat = Math.sqrt(dx * dx + dz * dz);
		double reach = Math.min(flat, LEASH - 2.0);
		return flat < 1.0E-3 ? new Vec3(centre.x, floorY(), centre.z)
				: new Vec3(centre.x + dx / flat * reach, floorY(), centre.z + dz / flat * reach);
	}

	/**
	 * Ground Slam, {@code t} ticks into it: crouched through the wind-up while the ring he will bare frosts over; then a leap
	 * onto where his foe stood; then he comes down ({@link #slamLands}).
	 */
	private void groundSlam(ServerLevel level, int t) {
		Attack slam = Attack.GROUND_SLAM;
		if (t < slam.windup) {
			setDeltaMovement(0.0, floorY() - getY(), 0.0);
			if (t % 4 == 0) {
				ring(level, leapTo, ring(), ParticleTypes.SNOWFLAKE, 24);
			}
			if (t == slam.windup - 1) {
				leapFrom = position();
				setAction(Action.SLAM);
			}
		} else if (t < slam.windup + slam.active) {
			leap(t - slam.windup, slam.active);
		} else if (t == slam.windup + slam.active) {
			teleportTo(leapTo.x, leapTo.y, leapTo.z);
			slamLands(level);
		} else {
			setDeltaMovement(0.0, floorY() - getY(), 0.0);
		}
	}

	/**
	 * He lands: {@value #SLAM_DAMAGE} damage within {@value #SLAM_RADIUS} blocks, a knockback within
	 * {@value #SLAM_PUSH_RADIUS}, and the drift snow blasted bare round him ({@link #bare}).
	 */
	public int slamLands(ServerLevel level) {
		int hit = 0;
		for (ServerPlayer player : nearby(level)) {
			Vec3 to = player.position().subtract(position());
			double flat = Math.sqrt(to.x * to.x + to.z * to.z);
			if (flat > SLAM_PUSH_RADIUS || Math.abs(to.y) > 3.0) {
				continue;
			}
			Vec3 way = flat < 1.0E-3 ? facing() : new Vec3(to.x / flat, 0.0, to.z / flat);
			if (flat <= SLAM_RADIUS && player.hurtServer(level, damageSources().mobAttack(this), damage(SLAM_DAMAGE))) {
				hit++;
			}
			push(player, way, SLAM_KNOCKBACK, 0.4);
		}
		bare(level, position(), ring());
		level.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.3, getZ(), 60, ring() / 2.0, 0.3, ring() / 2.0, 0.15);
		level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 20, 2.0, 0.2, 2.0, 0.05);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.HOSTILE, 3.0F, 0.6F);
		return hit;
	}

	/**
	 * Frost Breath, {@code t} ticks into it: he draws in a breath, frost gathering at his jaws; then a cone of freezing breath,
	 * {@value #BREATH_DAMAGE} damage every {@value #BREATH_EVERY} ticks and the frost to every player in it.
	 */
	private void frostBreath(ServerLevel level, Player target, int t) {
		Attack breath = Attack.FROST_BREATH;
		Vec3 jaws = jaws();
		if (t < breath.windup) {
			face(target);
			if (t % 3 == 0) {
				level.sendParticles(ParticleTypes.SNOWFLAKE, jaws.x, jaws.y, jaws.z, 2, 0.2, 0.2, 0.2, 0.0);
			}
			if (t == breath.windup - 1) {
				setAction(Action.BREATH);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.BREEZE_IDLE_AIR, SoundSource.HOSTILE, 2.0F, 0.5F);
			}
			return;
		}
		if (t >= breath.windup + breath.active) {
			setAction(Action.IDLE);
			return;
		}
		Vec3 facing = facing();
		for (int i = 0; i < 4; i++) {
			double along = 1.0 + getRandom().nextDouble() * BREATH_REACH;
			Vec3 spot = jaws.add(facing.scale(along)).add(0.0, -along * 0.15, 0.0);
			level.sendParticles(ParticleTypes.SNOWFLAKE, spot.x, spot.y, spot.z, 2, along * 0.2, 0.2, along * 0.2, 0.02);
			level.sendParticles(ParticleTypes.CLOUD, spot.x, spot.y, spot.z, 1, along * 0.15, 0.1, along * 0.15, 0.01);
		}
		if ((t - breath.windup) % BREATH_EVERY == 0) {
			breathe(level);
		}
	}

	/** One pulse of his breath: every player in the cone takes {@value #BREATH_DAMAGE} damage and the frost. */
	public int breathe(ServerLevel level) {
		int hit = 0;
		Vec3 facing = facing();
		for (ServerPlayer player : nearby(level)) {
			Vec3 to = player.position().subtract(position());
			double flat = Math.sqrt(to.x * to.x + to.z * to.z);
			if (flat > BREATH_REACH + 1.0 || Math.abs(to.y) > 3.0) {
				continue;
			}
			double cos = flat < 1.0E-3 ? 1.0 : (to.x * facing.x + to.z * facing.z) / flat;
			if (cos < Math.cos(Math.toRadians(BREATH_HALF_ANGLE))) {
				continue;
			}
			if (player.hurtServer(level, damageSources().mobAttack(this), damage(BREATH_DAMAGE))) {
				hit++;
			}
			chill(player, BREATH_FROST, true);
		}
		return hit;
	}

	/** Where his breath leaves him: his jaws. */
	public Vec3 jaws() {
		return position().add(facing().scale(1.4)).add(0.0, 2.9, 0.0);
	}

	/**
	 * Frost on {@code player}: {@code ticks} more of the ticks that freeze them (to {@code stack} past freezing them through,
	 * or else never past just short of it), unless they hold a Yeti Mitten in their offhand.
	 */
	public static void chill(ServerPlayer player, int ticks, boolean stack) {
		if (YetiMitten.warm(player)) {
			return;
		}
		int frozen = player.getTicksFrozen();
		int cap = stack ? FROST_CAP : player.getTicksRequiredToFreeze() - 1;
		player.setTicksFrozen(Math.max(frozen, Math.min(cap, (stack ? frozen : 0) + ticks)));
	}

	/**
	 * Avalanche Charge, {@code t} ticks into it: on all fours pawing the snow through the wind-up; then a straight run of up
	 * to {@value #CHARGE_SPEED} blocks a tick, {@value #CHARGE_DAMAGE} damage and a heavy knockback to each player he passes.
	 * Into an ice column he stuns himself for {@value #STUN_TICKS} ticks; in the Fury of the Peaks a charge that does not
	 * stun him is followed at once by another.
	 */
	private void charge(ServerLevel level, Player target, int t) {
		Attack run = Attack.AVALANCHE_CHARGE;
		if (t < run.windup) {
			face(target);
			setDeltaMovement(0.0, floorY() - getY(), 0.0);
			if (t % 3 == 0) {
				level.sendParticles(ParticleTypes.ITEM_SNOWBALL, getX(), floorY() + 0.1, getZ(), 6, 0.6, 0.05, 0.6, 0.1);
			}
			if (t == run.windup - 1) {
				Vec3 to = target.position().subtract(position());
				double flat = Math.sqrt(to.x * to.x + to.z * to.z);
				charge = flat < 1.0E-3 ? facing() : new Vec3(to.x / flat, 0.0, to.z / flat);
				trampled.clear();
				setAction(Action.CHARGE);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.HORSE_GALLOP, SoundSource.HOSTILE, 2.0F, 0.5F);
			}
			return;
		}
		if (t >= run.windup + run.active) {
			setDeltaMovement(0.0, floorY() - getY(), 0.0);
			if (t == run.windup + run.active) {
				endCharge(level, target);
			}
			return;
		}
		Vec3 next = position().add(charge.scale(CHARGE_SPEED));
		if (GlacierHall.columnDistance(next.x - origin.getX(), next.z - origin.getZ()) <= COLUMN_REACH) {
			stun(level);
			return;
		}
		Vec3 centre = centre();
		if (Math.hypot(next.x - centre.x, next.z - centre.z) > LEASH - 1.0) {
			attackTicks = run.windup + run.active - 1;  // the run ends at his leash
			setDeltaMovement(0.0, floorY() - getY(), 0.0);
			return;
		}
		setDeltaMovement(charge.x * CHARGE_SPEED, floorY() - getY(), charge.z * CHARGE_SPEED);
		trample(level);
		if (t % 2 == 0) {
			level.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.2, getZ(), 6, 0.8, 0.1, 0.8, 0.05);
		}
	}

	/** Strikes every player he runs into on this charge, once each. */
	public int trample(ServerLevel level) {
		int hit = 0;
		for (ServerPlayer player : nearby(level)) {
			Vec3 to = player.position().subtract(position());
			double flat = Math.sqrt(to.x * to.x + to.z * to.z);
			if (flat > CHARGE_REACH + getBbWidth() / 2.0 || Math.abs(to.y) > 3.0 || !trampled.add(player.getUUID())) {
				continue;
			}
			if (player.hurtServer(level, damageSources().mobAttack(this), damage(CHARGE_DAMAGE))) {
				hit++;
			}
			Vec3 side = flat < 1.0E-3 ? charge : new Vec3(to.x / flat, 0.0, to.z / flat);
			push(player, charge.add(side).normalize(), CHARGE_KNOCKBACK, 0.5);
		}
		return hit;
	}

	/** A charge that did not stun him ends; in the Fury of the Peaks the first is followed at once by another. */
	private void endCharge(ServerLevel level, Player target) {
		if (phase == Phase.FURY && !chained) {
			attackTicks = 0;
			chained = true;
			setAction(Action.CHARGE_WINDUP);
			face(target);
		}
	}

	/** Into an ice column: he crashes, reels for {@value #STUN_TICKS} ticks and takes a third more damage meanwhile. */
	public void stun(ServerLevel level) {
		attack = null;
		attackTicks = 0;
		stunned = STUN_TICKS;
		globalCooldown = cooldown(GLOBAL_COOLDOWN);
		setDeltaMovement(0.0, floorY() - getY(), 0.0);
		setAction(Action.STUNNED);
		level.sendParticles(ParticleTypes.ITEM_SNOWBALL, getX(), getY() + 2.0, getZ(), 30, 1.0, 1.0, 1.0, 0.2);
		level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 3.6, getZ(), 12, 0.6, 0.2, 0.6, 0.1);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.5F, 0.5F);
		level.playSound(null, getX(), getY(), getZ(), SoundType.GLASS.getBreakSound(), SoundSource.HOSTILE, 2.0F, 0.6F);
		tell(level, "message.jugcraft.yeti_king.stunned");
	}

	/**
	 * Icicle Fall, {@code t} ticks after its wind-up: during the wind-up each icicle's shadow spreads where it will fall; then
	 * over the active ticks the icicles fall one by one from the vault ({@link FallingIcicleEntity}).
	 */
	private void icicleFall(ServerLevel level, int t) {
		Attack fall = Attack.ICICLE_FALL;
		if (t < 0) {
			if ((t + fall.windup) % 4 == 0) {
				for (Vec3 spot : icicles) {
					level.sendParticles(ParticleTypes.SMOKE, spot.x, spot.y + 0.05, spot.z, 2, 0.15, 0.0, 0.15, 0.0);
				}
			}
			return;
		}
		for (int i = 0; i < icicles.size(); i++) {
			if (t == i * fall.active / icicles.size()) {
				things.add(FallingIcicleEntity.drop(level, this, icicles.get(i)).getUUID());
			}
		}
	}

	public List<Vec3> icicles() {
		return List.copyOf(icicles);
	}

	/**
	 * Glacial Spikes, {@code t} ticks after its wind-up: cracks race along the line to his foe during the wind-up; then a
	 * spike of ice bursts up from it each tick, {@value #SPIKE_DAMAGE} damage and thrown up ({@link GlacialSpikeEntity}).
	 */
	private void glacialSpikes(ServerLevel level, int t) {
		Attack burst = Attack.GLACIAL_SPIKES;
		if (t < 0) {
			int reached = (t + burst.windup) * spikes.size() / burst.windup;
			for (int i = 0; i < Math.min(reached, spikes.size()); i += 2) {
				Vec3 crack = spikes.get(i);
				level.sendParticles(ParticleTypes.ITEM_SNOWBALL, crack.x, crack.y + 0.05, crack.z, 1, 0.1, 0.0, 0.1, 0.0);
			}
			if (t == -1) {
				setAction(Action.SPIKES);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.HOSTILE, 2.0F, 0.9F);
			}
			return;
		}
		if (t < spikes.size()) {
			things.add(GlacialSpikeEntity.burst(level, this, spikes.get(t)).getUUID());
		}
	}

	public List<Vec3> spikes() {
		return List.copyOf(spikes);
	}

	/** Up to {@code count} whelps climb out of the dens, never more than {@value #MAX_WHELPS} at once. */
	public int callKin(ServerLevel level, int count) {
		List<Vec3> dens = GlacierHall.dens();
		int out = 0;
		for (int i = 0; i < count && whelps.size() < MAX_WHELPS; i++) {
			Vec3 den = at(dens.get(i % dens.size()));
			YetiWhelpEntity whelp = YetiWhelpEntity.climbOut(level, this, den);
			whelps.add(whelp.getUUID());
			out++;
		}
		return out;
	}

	// ---------------------------------------------------------------- his rule: the snow is his

	/**
	 * Blasts the drift snow bare round {@code at}: every cell of the lake within {@code radius} blocks that is drift snow
	 * becomes glare ice, for {@value #GLARE_TICKS} ticks before the snow drifts back. Trampled snow, glare ice the wind
	 * has bared and the columns are left as they are.
	 */
	public List<BlockPos> bare(ServerLevel level, Vec3 at, double radius) {
		List<BlockPos> cells = new ArrayList<>();
		int y = origin.getY() + GlacierHall.LAKE;
		int r = (int) Math.ceil(radius);
		BlockState glare = JugcraftLairs.GLARE_ICE.defaultBlockState();
		for (int x = (int) Math.floor(at.x) - r; x <= (int) Math.floor(at.x) + r; x++) {
			for (int z = (int) Math.floor(at.z) - r; z <= (int) Math.floor(at.z) + r; z++) {
				if (Math.hypot(x + 0.5 - at.x, z + 0.5 - at.z) > radius) {
					continue;
				}
				BlockPos cell = new BlockPos(x, y, z);
				if (level.isLoaded(cell) && level.getBlockState(cell).is(JugcraftLairs.DRIFT_SNOW)) {
					level.setBlock(cell, glare, Block.UPDATE_CLIENTS);
					cells.add(cell);
				}
			}
		}
		if (!cells.isEmpty()) {
			bared.add(new Patch(cells, level.getGameTime() + GLARE_TICKS));
		}
		return cells;
	}

	/** The snow drifts back over every patch whose time has come. */
	private void driftDue(ServerLevel level) {
		long now = level.getGameTime();
		bared.removeIf(patch -> {
			if (now < patch.driftAt()) {
				return false;
			}
			drift(level, patch);
			return true;
		});
	}

	private void drift(ServerLevel level, Patch patch) {
		BlockState snow = JugcraftLairs.DRIFT_SNOW.defaultBlockState();
		for (BlockPos cell : patch.cells()) {
			if (level.isLoaded(cell) && level.getBlockState(cell).is(JugcraftLairs.GLARE_ICE)) {
				level.setBlock(cell, snow, Block.UPDATE_CLIENTS);
			}
		}
		if (!patch.cells().isEmpty()) {
			BlockPos first = patch.cells().getFirst();
			level.sendParticles(ParticleTypes.SNOWFLAKE, first.getX() + 0.5, first.getY() + 1.2, first.getZ() + 0.5, 12, 2.0, 0.3, 2.0, 0.02);
		}
	}

	/** The snow drifts back over everything he has bared, at once. */
	public void driftAll(ServerLevel level) {
		for (Patch patch : bared) {
			drift(level, patch);
		}
		bared.clear();
	}

	/** How many patches of his lake are bare now. */
	public int baredPatches() {
		return bared.size();
	}

	// ---------------------------------------------------------------- the King's Roar, and the Fury of the Peaks

	/**
	 * The King's Roar: he bounds back onto his dais and roars for {@value #ROAR_TICKS} ticks, unhurt; a blizzard chills
	 * everyone near and two whelps climb out of the dens; then he leaps back down onto the lake.
	 */
	public void startRoar(ServerLevel level) {
		attack = null;
		phase = Phase.ROARING;
		phaseTicks = 0;
		setAction(Action.LEAP);
		leapFrom = position();
		leapTo = at(GlacierHall.roar());
		tell(level, "message.jugcraft.yeti_king.roar");
	}

	private void roaring(ServerLevel level) {
		if (phaseTicks <= LEAP_TICKS) {
			leap(phaseTicks - 1, LEAP_TICKS);
			if (phaseTicks == LEAP_TICKS) {
				teleportTo(leapTo.x, leapTo.y, leapTo.z);
				face(0.0F);
				setAction(Action.ROAR);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 4.0F, 0.6F);
				blizzard(level);
				callKin(level, KIN);
			}
			return;
		}
		int roared = phaseTicks - LEAP_TICKS;
		if (roared < ROAR_TICKS) {
			setDeltaMovement(Vec3.ZERO);
			if (roared % 2 == 0) {
				for (ServerPlayer player : nearby(level)) {
					level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1.5, player.getZ(), 6, 3.0, 1.5, 3.0, 0.08);
				}
			}
			if (roared == ROAR_TICKS - 1) {
				setAction(Action.LEAP);
				leapFrom = position();
				leapTo = at(GlacierHall.below());
			}
			return;
		}
		int down = roared - ROAR_TICKS;
		leap(down, LEAP_TICKS);
		if (down >= LEAP_TICKS - 1) {
			landed(level);
			phase = Phase.BLIZZARD;
			phaseTicks = 0;
			globalCooldown = GLOBAL_COOLDOWN;
			setAction(Action.IDLE);
		}
	}

	/** The blizzard of the King's Roar: everyone near is chilled, the frost for a moment. */
	public void blizzard(ServerLevel level) {
		for (ServerPlayer player : nearby(level)) {
			chill(player, BLIZZARD_FROST, false);
		}
	}

	/** The Fury of the Peaks: his crown blazes and his eyes burn blue, and his cooldowns shorten by a third. */
	public void startFury(ServerLevel level) {
		attack = null;
		entityData.set(FURIOUS, true);
		phase = Phase.FURY;
		phaseTicks = 0;
		setAction(Action.IDLE);
		globalCooldown = cooldown(GLOBAL_COOLDOWN);
		level.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 3.0, getZ(), 40, 1.5, 1.0, 1.5, 0.1);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 3.0F, 0.8F);
		tell(level, "message.jugcraft.yeti_king.fury");
	}

	// ---------------------------------------------------------------- his whelps and his things

	public List<UUID> whelps() {
		return List.copyOf(whelps);
	}

	/** Forgets what is gone. */
	private void prune(ServerLevel level) {
		whelps.removeIf(id -> !(level.getEntity(id) instanceof YetiWhelpEntity whelp) || !whelp.isAlive());
		things.removeIf(id -> level.getEntity(id) == null);
	}

	/** Whoever hurt him or his whelps has taken part. */
	public void took(ServerPlayer player, float amount) {
		damageBy.merge(player.getUUID(), amount, Float::sum);
	}

	/** His participants: everyone who hurt him or his whelps and is still near his lake. */
	public List<ServerPlayer> participants(ServerLevel level) {
		List<ServerPlayer> out = new ArrayList<>();
		Vec3 centre = centre();
		for (ServerPlayer player : level.players()) {
			if (player.isAlive() && damageBy.getOrDefault(player.getUUID(), 0.0F) > 0.0F && player.position().distanceTo(centre) <= 64.0) {
				out.add(player);
			}
		}
		return out;
	}

	/** Who is fighting him, as his whelps need to know. */
	public @Nullable Player foe(ServerLevel level) {
		return foe == null ? null : level.getPlayerByUUID(foe);
	}

	// ---------------------------------------------------------------- abandoned, and defeated

	/** Left alone: he climbs back onto his throne, healed; the snow drifts back, his whelps flee and his things are gone. */
	public void reset(ServerLevel level) {
		dismiss(level);
		driftAll(level);
		entityData.set(FURIOUS, false);
		phase = Phase.WAITING;
		phaseTicks = 0;
		attack = null;
		abandoned = 0;
		stunned = 0;
		foe = null;
		damageBy.clear();
		Arrays.fill(cooldowns, 0);
		setHealth(getMaxHealth());
		Vec3 throne = throne();
		teleportTo(throne.x, throne.y, throne.z);
		face(0.0F);
		setAction(Action.THRONE);
		bossBar.setVisible(false);
		tell(level, "message.jugcraft.yeti_king.reset");
	}

	/** His whelps flee into the snow, and his boulders, icicles and spikes are gone. */
	private void dismiss(ServerLevel level) {
		for (UUID id : List.copyOf(whelps)) {
			if (level.getEntity(id) instanceof YetiWhelpEntity whelp) {
				whelp.flee(level);
			}
		}
		whelps.clear();
		for (UUID id : List.copyOf(things)) {
			Entity thing = level.getEntity(id);
			if (thing != null) {
				thing.discard();
			}
		}
		things.clear();
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level) {
			defeated(level);
		}
	}

	/**
	 * He has fallen: his whelps flee and his things are gone, the snow drifts back, each participant gets their loot, his
	 * lair's instance ends (it closes once everyone has left) and a gate of Grey Mist opens in the middle of the lake.
	 */
	private void defeated(ServerLevel level) {
		dismiss(level);
		driftAll(level);
		YetiKingLoot.reward(level, this, participants(level));
		LairInstance open = lairInstance(level);
		if (open != null) {
			Lairs.end(open);
			BlockState mist = JugcraftLairs.LAIR_EXIT.defaultBlockState().setValue(LairExitBlock.AXIS, Direction.Axis.X);
			for (BlockPos at : exitCells(centre())) {
				level.setBlock(at, mist, Block.UPDATE_ALL);
			}
		}
		bossBar.setVisible(false);
		tell(level, "message.jugcraft.yeti_king.defeated");
	}

	/** Where the Grey Mist opens when he falls: two blocks wide and two high, standing on the middle of the lake. */
	public static List<BlockPos> exitCells(Vec3 centre) {
		BlockPos at = BlockPos.containing(centre.x, centre.y, centre.z);
		return List.of(at.west(), at, at.west().above(), at.above());
	}

	/** Tells everyone near his lake. */
	private void tell(ServerLevel level, String key) {
		Vec3 centre = centre();
		for (ServerPlayer player : level.players()) {
			if (player.position().distanceTo(centre) <= LEASH + 32.0) {
				player.sendSystemMessage(Component.translatable(key));
			}
		}
	}

	// ---------------------------------------------------------------- what he is immune to

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return super.hurtServer(level, source, amount);
		}
		if (phase == Phase.WAITING) {
			if (source.getEntity() instanceof Player player && LairBosses.eligible(player)) {
				wake(level);
			}
			return false;
		}
		if (phase == Phase.WAKING || phase == Phase.ROARING || source.is(DamageTypeTags.IS_FREEZING)) {
			return false;
		}
		float taken = stunned > 0 ? (float) (amount * STUN_TAKEN) : amount;
		boolean hurt = super.hurtServer(level, source, taken);
		if (hurt && source.getEntity() instanceof ServerPlayer player) {
			took(player, taken);
		}
		return hurt;
	}

	@Override
	public boolean canFreeze() {
		return false;
	}

	@Override
	public void makeStuckInBlock(BlockState state, Vec3 multiplier) {
		// No web or powder snow holds him: the glacier is his.
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
	protected boolean canRide(Entity vehicle) {
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
	protected SoundEvent getAmbientSound() {
		return phase == Phase.WAITING ? null : SoundEvents.POLAR_BEAR_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.POLAR_BEAR_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.POLAR_BEAR_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.5F;
	}

	// ---------------------------------------------------------------- particles

	static void ring(ServerLevel level, Vec3 centre, double radius, ParticleOptions particle, int points) {
		for (int i = 0; i < points; i++) {
			double angle = Math.PI * 2.0 * i / points;
			level.sendParticles(particle, centre.x + Math.cos(angle) * radius, centre.y + 0.1, centre.z + Math.sin(angle) * radius, 1, 0.0, 0.02,
					0.0, 0.0);
		}
	}

	// ---------------------------------------------------------------- saving and syncing

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ACTION, Action.THRONE.ordinal());
		builder.define(FURIOUS, false);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (instance != null) {
			output.store("instance", UUIDUtil.CODEC, instance);
			output.putInt("slot", slot);
		}
		output.store("origin", BlockPos.CODEC, origin);
		output.putString("phase", phase.name());
		output.putBoolean("furious", furious());
		// The lake he has bared, so the snow drifts back over it when he is loaded again.
		List<BlockPos> bare = new ArrayList<>();
		for (Patch patch : bared) {
			bare.addAll(patch.cells());
		}
		if (!bare.isEmpty()) {
			output.store("bared", BlockPos.CODEC.listOf(), bare);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		instance = input.read("instance", UUIDUtil.CODEC).orElse(null);
		slot = input.getIntOr("slot", -1);
		origin = input.read("origin", BlockPos.CODEC).orElse(BlockPos.containing(getX() - GlacierHall.THRONE_X, getY() - GlacierHall.SEAT_TOP,
				getZ() - GlacierHall.THRONE_Z));
		try {
			phase = Phase.valueOf(input.getStringOr("phase", Phase.WAITING.name()));
		} catch (IllegalArgumentException e) {
			phase = Phase.WAITING;
		}
		if (phase == Phase.WAKING) {
			phase = Phase.HUNT;  // a passing moment is not resumed
		} else if (phase == Phase.ROARING) {
			phase = Phase.BLIZZARD;
		}
		entityData.set(FURIOUS, input.getBooleanOr("furious", false));
		List<BlockPos> bare = input.read("bared", BlockPos.CODEC.listOf()).orElse(List.of());
		if (!bare.isEmpty()) {
			bared.add(new Patch(new ArrayList<>(bare), 0L));
		}
		driftOnLoad = true;
		bossBar.setVisible(phase != Phase.WAITING);
	}

	// ---------------------------------------------------------------- his animations

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<YetiKingEntity>("body", 4, test -> {
			YetiKingEntity king = test.animatable();
			if (king.isDeadOrDying()) {
				return test.setAndContinue(DEATH);
			}
			Action action = king.action();
			return test.setAndContinue(action == Action.IDLE && test.isMoving() ? WALK : BODY[action.ordinal()]);
		}));
		controllers.add(new AnimationController<YetiKingEntity>("crown", 8,
				test -> test.setAndContinue(test.animatable().furious() ? CROWN_BLAZING : CROWN_CALM)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
