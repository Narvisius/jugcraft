package io.github.jimbozoomer.jugcraft.lair.sentinel;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.LoopType;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.lair.CydonianSanctum;
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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Cydonian Sentinel (docs/features/cydonian-sentinel.md): the Cydonian Sanctum's boss, a construct four blocks tall
 * of red stone plates and rusted iron, built round a floating core of red light; its arms pillars, its face a carved
 * visor, the Ancient Altar's red star cut into its chest. It waits on its plinth on the crater's north rim, a statue with
 * its core dark, and wakes when someone who may fight it steps onto the crater's floor or strikes it: its core flares and
 * it steps down, unhurt, its health scaled for the party. It keeps to its crater.
 *
 * <p>Its rule: the Red Storm. Every {@value #STORM_FIRST} ticks at first, then every phase's own while ({@link #stormEvery}),
 * it plants its arms for {@value #STORM_TELL} ticks: the sky reddens with dust streaming in from one side, and the bar
 * names the side. Then for {@value #STORM_TICKS} ticks a scouring wind crosses the crater from it: whoever is not
 * sheltered (rock within {@value #LEE} blocks upwind of them: in the lee of a standing stone, or under the rim on the
 * windward side) takes {@value #STORM_DAMAGE} every {@value #STORM_HIT_EVERY} ticks and is pushed downwind. Its weak
 * point: the core. As it charges its Core Lance its chest plates open: while they are, its core takes
 * {@value #CORE_MULTIPLIER} times the damage and its armour does not count, and a blow of {@value #STAGGER_DAMAGE} or more
 * in the wind-up staggers it and the lance fails.
 *
 * <p>Phase 1, the Sentinel: the Hammerfall, the Shard Volley, the Quake Line and the Core Lance. At {@value #CRACK_AT} of its
 * health, the Shell Cracks: it roars, unhurt, and two Shard Sentries rise from the small craters. Phase 2, the Red
 * Storm: the Sentry Call besides, and its storms carry its shards. At {@value #UNBOUND_AT}, the Unbound Core: its shell
 * bursts apart, unhurt, and the core floats free, armour {@value #CORE_ARMOR}, faster, with the Sweeping Lance, the Shard
 * Burst and the Starfall (after which it sinks to the floor, dimmed: the moment for blows). Below {@value #GIANT_AT}, the
 * Red Giant: its cooldowns and its pause are a third shorter, its storms come oftener, and its Starfall marks two places
 * round each player.
 *
 * <p>Every attack has a wind-up you can read, a strike and a recovery ({@link Attack}); the numbers are here, in one place,
 * and in tools/cydonian_sentinel.py. Knockback, falls, fire, drowning, poison and Wither never harm it. Each participant
 * (whoever hurt it or its sentries) gets their own loot ({@link SentinelLoot}). Left alone it strides back to its
 * plinth and reforms, healed: its sentries crumble, its shards, spikes and meteors are gone, and who hurt it is
 * forgotten. Bound to a lair instance it vanishes with it; one with no instance (a test's) keeps to the crater laid out
 * from the origin it was given. The only blocks it changes are the Red Mist it opens in the crater's middle when it falls.
 */
public class CydonianSentinelEntity extends Monster implements GeoEntity {
	// ---------------------------------------------------------------- numbers (tools/cydonian_sentinel.py)
	public static final float HEALTH = 480.0F;
	public static final double ARMOR = 12.0;
	public static final double CORE_ARMOR = 4.0;
	public static final double LEASH = 30.0;
	public static final double PARTY_STEP = 0.5;
	public static final double PARTY_MAX = 2.5;
	public static final int WAKE_TICKS = 50;
	public static final int ABANDON_TICKS = 200;
	public static final int GLOBAL_COOLDOWN = 20;
	public static final double SPEED = 0.12;
	public static final double CORE_SPEED = 0.2;
	public static final double KEEP_DISTANCE = 3.0;
	public static final double STEP = 1.0;
	public static final float CRACK_AT = 0.6F;
	public static final float UNBOUND_AT = 0.25F;
	public static final float GIANT_AT = 0.1F;
	public static final double GIANT_COOLDOWN = 0.6667;
	public static final int CRACK_TICKS = 60;
	public static final int BURST_TICKS = 60;
	public static final double CORE_HOVER = 2.0;
	public static final int STORM_FIRST = 300;
	public static final int STORM_EVERY = 700;
	public static final int STORM_EVERY_STORM = 500;
	public static final int STORM_EVERY_UNBOUND = 400;
	public static final int STORM_EVERY_GIANT = 300;
	public static final int STORM_TELL = 50;
	public static final int STORM_TICKS = 120;
	public static final float STORM_DAMAGE = 2.0F;
	public static final int STORM_HIT_EVERY = 10;
	public static final double STORM_PUSH = 0.3;
	public static final double LEE = 8.0;
	public static final double SHELTER_REACH = 12.0;
	public static final int STORM_SHARDS = 12;
	public static final int STORM_SHARD_AT = 20;
	public static final int SENTRY_CAP = 4;
	public static final int SENTRIES_RISE = 2;
	public static final float HAMMER_DAMAGE = 14.0F;
	public static final double HAMMER_AHEAD = 2.0;
	public static final double HAMMER_REACH = 3.0;
	public static final double HAMMER_KNOCK = 6.0;
	public static final double HAMMER_THROW = 1.1;
	public static final int VOLLEY_SHARDS = 5;
	public static final double VOLLEY_FAN = 40.0;
	public static final float VOLLEY_DAMAGE = 5.0F;
	public static final float QUAKE_DAMAGE = 10.0F;
	public static final double QUAKE_LENGTH = 14.0;
	public static final double QUAKE_WIDTH = 1.2;
	public static final double QUAKE_TOSS = 0.8;
	public static final double QUAKE_STEP = 2.0;
	public static final float LANCE_DAMAGE = 4.0F;
	public static final int LANCE_EVERY = 5;
	public static final double LANCE_LENGTH = 16.0;
	public static final double LANCE_WIDTH = 0.9;
	public static final float CORE_MULTIPLIER = 2.0F;
	public static final float STAGGER_DAMAGE = 6.0F;
	public static final int STAGGER_TICKS = 40;
	public static final double SWEEP_ANGLE = 90.0;
	public static final int BURST_SHARDS = 12;
	public static final float BURST_DAMAGE = 6.0F;
	public static final float STARFALL_DAMAGE = 10.0F;
	public static final double STARFALL_RADIUS = 2.0;
	public static final int STARFALL_MARKS = 1;
	public static final int GIANT_MARKS = 2;
	public static final double MARK_SPREAD = 3.0;
	public static final double METEOR_HEIGHT = 16.0;
	public static final int METEOR_FALL = 20;
	public static final int EXPERIENCE = 400;
	/** Where its lances leave from: its core, in its shell (over its feet, and before them) and unbound (tools/cydonian_sentinel.py LANCE). */
	public static final double CORE_UP = 2.5625;
	public static final double CORE_AHEAD = 0.375;
	public static final double UNBOUND_UP = 0.75;
	/** The Red Storm's damage type (data/jugcraft/damage_type/red_storm.json). */
	public static final ResourceKey<DamageType> STORM_DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, Jugcraft.id("red_storm"));
	/** Its body's size unbound: the floating core. */
	private static final EntityDimensions UNBOUND_SIZE = EntityDimensions.fixed(1.5F, 1.5F).withEyeHeight(0.75F);
	/** Waking: ticks spent stirring on the plinth before it steps down. */
	private static final int STIR = 20;
	/** How high its step down from the plinth arcs over the straight line. */
	private static final double STEP_DOWN_RISE = 0.8;
	private static final DustParticleOptions RED_DUST = new DustParticleOptions(0xB0502A, 1.6F);
	private static final DustParticleOptions CORE_LIGHT = new DustParticleOptions(0xFF3A1A, 1.5F);
	private static final DustParticleOptions MARK = new DustParticleOptions(0xE0281A, 1.2F);

	/**
	 * Its attacks: wind-up, the active part and recovery in ticks, the cooldown, the target's nearest and farthest
	 * distance, and the phases it uses it in (the Sentinel; the Red Storm; the Unbound Core and the Red Giant). The wind-up
	 * is what players read; the strike lands as it ends.
	 */
	public enum Attack {
		HAMMERFALL(16, 4, 12, 60, 0.0, 4.5, true, true, false),
		SHARD_VOLLEY(14, 1, 12, 100, 4.0, 24.0, true, true, false),
		QUAKE_LINE(20, 10, 12, 140, 0.0, 14.0, true, true, false),
		CORE_LANCE(24, 30, 14, 220, 3.0, 16.0, true, true, false),
		SENTRY_CALL(20, 1, 10, 400, 0.0, 40.0, false, true, false),
		SWEEPING_LANCE(20, 40, 14, 160, 0.0, 16.0, false, false, true),
		SHARD_BURST(16, 1, 14, 140, 0.0, 24.0, false, false, true),
		STARFALL(40, 1, 60, 260, 0.0, 40.0, false, false, true);

		public final int windup;
		public final int active;
		public final int recovery;
		public final int cooldown;
		public final double near;
		public final double far;
		public final boolean sentinel;
		public final boolean storm;
		public final boolean core;

		Attack(int windup, int active, int recovery, int cooldown, double near, double far, boolean sentinel, boolean storm,
				boolean core) {
			this.windup = windup;
			this.active = active;
			this.recovery = recovery;
			this.cooldown = cooldown;
			this.near = near;
			this.far = far;
			this.sentinel = sentinel;
			this.storm = storm;
			this.core = core;
		}

		public int length() {
			return windup + active + recovery;
		}
	}

	/** The fight's phases. */
	public enum Phase {
		WAITING, WAKING, SENTINEL, CRACKING, RED_STORM, BURSTING, UNBOUND, RED_GIANT
	}

	/** Its form, synced: its shell whole, or its core unbound. */
	public enum Form {
		SHELL, CORE
	}

	/** Its core's light, synced: dark while it waits, lit, flaring, opened, unbound, white-hot, or dimmed. */
	public enum Glow {
		DARK, LIT, FLARE, OPEN, UNBOUND, GIANT, DIM
	}

	/** What its body is doing, synced for its animations. */
	public enum Action {
		STATUE, WAKE, IDLE, HAMMER_WINDUP, HAMMER, VOLLEY_WINDUP, VOLLEY, QUAKE_WINDUP, QUAKE, LANCE_WINDUP, LANCE, STAGGER,
		CALL_WINDUP, CALL, PLANT, PLANTED, ROAR, BURST, CORE_IDLE, SWEEP_WINDUP, SWEEP, BURST_WINDUP, SHARD_BURST,
		STARFALL_WINDUP, SUNK
	}

	/** The side its storm comes from, and the way it blows (downwind, flat). */
	public enum Wind {
		NORTH(0, 1), EAST(-1, 0), SOUTH(0, -1), WEST(1, 0);

		public final int dx;
		public final int dz;

		Wind(int dx, int dz) {
			this.dx = dx;
			this.dz = dz;
		}

		/** The way it blows. */
		public Vec3 blows() {
			return new Vec3(dx, 0.0, dz);
		}

		/** "the east" and so on, as the bar names it. */
		public Component display() {
			return Component.translatable("message.jugcraft.cydonian_sentinel.wind." + name().toLowerCase(Locale.ROOT));
		}
	}

	/** Its storm: calm, its tell, or blowing. */
	public enum Storm {
		CALM, TELL, BLOWING
	}

	private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(CydonianSentinelEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> GLOW = SynchedEntityData.defineId(CydonianSentinelEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> FORM = SynchedEntityData.defineId(CydonianSentinelEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> PLATES = SynchedEntityData.defineId(CydonianSentinelEntity.class, EntityDataSerializers.BOOLEAN);
	static final RawAnimation[] BODY = new RawAnimation[Action.values().length];
	static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.cydonian_sentinel.walk");
	static final RawAnimation CORE_MOVE = RawAnimation.begin().thenLoop("animation.cydonian_sentinel.core_move");
	static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.cydonian_sentinel.death");
	static final RawAnimation CORE_DEATH = RawAnimation.begin().thenPlayAndHold("animation.cydonian_sentinel.core_death");
	static final RawAnimation[] GLOWS = {RawAnimation.begin().thenLoop("animation.cydonian_sentinel.core_dark"),
			RawAnimation.begin().thenLoop("animation.cydonian_sentinel.core_lit"), RawAnimation.begin().thenLoop("animation.cydonian_sentinel.core_flare"),
			RawAnimation.begin().thenLoop("animation.cydonian_sentinel.core_open"), RawAnimation.begin().thenLoop("animation.cydonian_sentinel.core_unbound"),
			RawAnimation.begin().thenLoop("animation.cydonian_sentinel.core_giant"), RawAnimation.begin().thenLoop("animation.cydonian_sentinel.core_dim")};
	static final RawAnimation PLATES_SHUT = RawAnimation.begin().thenLoop("animation.cydonian_sentinel.plates_shut");
	static final RawAnimation PLATES_OPEN = RawAnimation.begin().thenLoop("animation.cydonian_sentinel.plates_open");
	static final RawAnimation FORM_SHELL = RawAnimation.begin().thenLoop("animation.cydonian_sentinel.form_shell");
	static final RawAnimation FORM_CORE = RawAnimation.begin().thenLoop("animation.cydonian_sentinel.form_core");

	static {
		String prefix = "animation.cydonian_sentinel.";
		BODY[Action.STATUE.ordinal()] = RawAnimation.begin().thenLoop(prefix + "statue");
		BODY[Action.WAKE.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "wake");
		BODY[Action.IDLE.ordinal()] = RawAnimation.begin().thenLoop(prefix + "idle");
		BODY[Action.HAMMER_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "hammer_windup");
		BODY[Action.HAMMER.ordinal()] = RawAnimation.begin().then(prefix + "hammer", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.VOLLEY_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "volley_windup");
		BODY[Action.VOLLEY.ordinal()] = RawAnimation.begin().then(prefix + "volley", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.QUAKE_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "quake_windup");
		BODY[Action.QUAKE.ordinal()] = RawAnimation.begin().then(prefix + "quake", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.LANCE_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "lance_windup");
		BODY[Action.LANCE.ordinal()] = RawAnimation.begin().thenLoop(prefix + "lance");
		BODY[Action.STAGGER.ordinal()] = RawAnimation.begin().thenLoop(prefix + "stagger");
		BODY[Action.CALL_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "call_windup");
		BODY[Action.CALL.ordinal()] = RawAnimation.begin().then(prefix + "call", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.PLANT.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "plant");
		BODY[Action.PLANTED.ordinal()] = RawAnimation.begin().thenLoop(prefix + "planted");
		BODY[Action.ROAR.ordinal()] = RawAnimation.begin().thenLoop(prefix + "roar");
		BODY[Action.BURST.ordinal()] = RawAnimation.begin().thenLoop(prefix + "burst");
		BODY[Action.CORE_IDLE.ordinal()] = RawAnimation.begin().thenLoop(prefix + "core_idle");
		BODY[Action.SWEEP_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "sweep_windup");
		BODY[Action.SWEEP.ordinal()] = RawAnimation.begin().thenLoop(prefix + "sweep");
		BODY[Action.BURST_WINDUP.ordinal()] = RawAnimation.begin().thenLoop(prefix + "burst_windup");
		BODY[Action.SHARD_BURST.ordinal()] = RawAnimation.begin().then(prefix + "shard_burst", LoopType.PLAY_ONCE).thenLoop(prefix + "core_idle");
		BODY[Action.STARFALL_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "starfall_windup");
		BODY[Action.SUNK.ordinal()] = RawAnimation.begin().thenLoop(prefix + "sunk");
	}

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private final ServerBossEvent bossBar = new ServerBossEvent(UUID.randomUUID(), Component.translatable("entity.jugcraft.cydonian_sentinel"),
			BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
	/** The lair instance it belongs to (null for one put down by a test or a command), and the Sanctum's origin. */
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
	private int staggerTicks;
	private final Map<UUID, Float> damageBy = new HashMap<>();
	// Its storm.
	private Storm storm = Storm.CALM;
	private Wind wind = Wind.EAST;
	private int stormTimer = STORM_FIRST;
	private int stormTicks;
	// Its sentries and its things (shards, spikes, meteors).
	private final List<UUID> sentries = new ArrayList<>();
	private final List<UUID> things = new ArrayList<>();
	// The attack under way.
	private Vec3 aim = new Vec3(0.0, 0.0, 1.0);
	private double sweepFrom;
	private Vec3 quakeFrom = Vec3.ZERO;
	private final List<Vec3> marks = new ArrayList<>();
	private boolean sunk;
	/** Stepping down from the plinth: from where. */
	private Vec3 stepFrom = Vec3.ZERO;
	/** Set when it is loaded in a passing moment (waking): it lands on the crater's floor and fights on. */
	private boolean landOnLoad;

	public CydonianSentinelEntity(EntityType<? extends CydonianSentinelEntity> type, Level level) {
		super(type, level);
		xpReward = 0;
		setPersistenceRequired();
		bossBar.setDarkenScreen(true);
		bossBar.setVisible(false);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.ARMOR, ARMOR)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 48.0).add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.ATTACK_DAMAGE, HAMMER_DAMAGE).add(Attributes.STEP_HEIGHT, STEP);
	}

	/**
	 * Sets it on the plinth of {@code instance}'s Sanctum. Called as the instance is placed. Placing the template puts
	 * back only its own blocks, so the Red Mist a fallen Sentinel opened in the crater's middle before still stands: it
	 * goes first.
	 */
	public static CydonianSentinelEntity summon(ServerLevel level, LairInstance instance) {
		BlockPos origin = instance.origin();
		for (BlockPos at : CydonianSanctum.craterMistCells(origin)) {
			if (level.getBlockState(at).is(JugcraftLairs.RED_MIST)) {
				level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		return summon(level, origin, instance);
	}

	/** Sets it on the plinth of the Sanctum laid out from {@code origin} (tests pass no instance). */
	public static CydonianSentinelEntity summon(ServerLevel level, BlockPos origin, @Nullable LairInstance instance) {
		CydonianSentinelEntity sentinel = new CydonianSentinelEntity(JugcraftSentinel.CYDONIAN_SENTINEL, level);
		sentinel.origin = origin.immutable();
		if (instance != null) {
			sentinel.instance = instance.id;
			sentinel.slot = instance.slot;
		}
		Vec3 waiting = sentinel.waiting();
		sentinel.snapTo(waiting.x, waiting.y, waiting.z, 0.0F, 0.0F);
		sentinel.face(0.0F);
		level.addFreshEntity(sentinel);
		return sentinel;
	}

	// ---------------------------------------------------------------- where things are

	/** The crater's middle, on its floor, for the Sanctum laid out from {@code origin}. */
	public static Vec3 centre(BlockPos origin) {
		return CydonianSanctum.centre().add(origin.getX(), origin.getY(), origin.getZ());
	}

	public Vec3 centre() {
		return centre(origin);
	}

	public BlockPos origin() {
		return origin;
	}

	/** Where it waits on its plinth. */
	public Vec3 waiting() {
		return new Vec3(origin.getX() + CydonianSanctum.WAIT_X, origin.getY() + CydonianSanctum.WAIT_Y, origin.getZ() + CydonianSanctum.WAIT_Z);
	}

	/** Where it comes down to the crater's floor as it wakes. */
	public Vec3 woke() {
		return new Vec3(origin.getX() + CydonianSanctum.WOKE_X, floorY(), origin.getZ() + CydonianSanctum.WOKE_Z);
	}

	/** The crater's floor, where it and its foes stand. */
	public double floorY() {
		return origin.getY() + CydonianSanctum.STAND;
	}

	/** The middle of the small crater {@code pit} (an index of {@link CydonianSanctum#PITS}), on its sunken floor. */
	public Vec3 pit(int pit) {
		int[] cell = CydonianSanctum.PITS[pit];
		return new Vec3(origin.getX() + cell[0] + 0.5, floorY() - 1.0, origin.getZ() + cell[1] + 0.5);
	}

	/** Where its lances leave from: its core, before it in its chest, or (unbound) where it floats. */
	public Vec3 core() {
		if (form() == Form.CORE) {
			return position().add(0.0, UNBOUND_UP, 0.0);
		}
		return position().add(facing().scale(CORE_AHEAD)).add(0.0, CORE_UP, 0.0);
	}

	// ---------------------------------------------------------------- state

	public Phase phase() {
		return phase;
	}

	public Storm storm() {
		return storm;
	}

	public Wind wind() {
		return wind;
	}

	public @Nullable Attack attack() {
		return attack;
	}

	public int attackTicks() {
		return attackTicks;
	}

	public int staggerTicks() {
		return staggerTicks;
	}

	public boolean sunk() {
		return sunk;
	}

	public List<Vec3> marks() {
		return List.copyOf(marks);
	}

	public List<UUID> sentries() {
		return List.copyOf(sentries);
	}

	public Action action() {
		int id = entityData.get(ACTION);
		return id >= 0 && id < Action.values().length ? Action.values()[id] : Action.IDLE;
	}

	private void setAction(Action action) {
		entityData.set(ACTION, action.ordinal());
	}

	public Glow glow() {
		int id = entityData.get(GLOW);
		return id >= 0 && id < Glow.values().length ? Glow.values()[id] : Glow.DARK;
	}

	private void setGlow(Glow glow) {
		entityData.set(GLOW, glow.ordinal());
	}

	public Form form() {
		return entityData.get(FORM) == Form.CORE.ordinal() ? Form.CORE : Form.SHELL;
	}

	/** Whether its chest plates are open: its core takes double damage, and its armour does not count. */
	public boolean platesOpen() {
		return entityData.get(PLATES);
	}

	public @Nullable UUID instanceId() {
		return instance;
	}

	/** Whether it is fighting (not waiting, waking, cracking or bursting). */
	public boolean fighting() {
		return phase == Phase.SENTINEL || phase == Phase.RED_STORM || phase == Phase.UNBOUND || phase == Phase.RED_GIANT;
	}

	/** Whether it fights unbound (the Unbound Core, or the Red Giant). */
	public boolean unbound() {
		return phase == Phase.UNBOUND || phase == Phase.RED_GIANT;
	}

	/** Damage it deals: the attack's own, times {@code lairs.boss_damage} ({@link LairBosses}). */
	public static float damage(float base) {
		return LairBosses.damage(base);
	}

	/** Its health for a party of {@code players}: half as much again for each beyond the first, at most 2.5 times. */
	public static double partyScale(int players) {
		return LairBosses.partyScale(players, PARTY_STEP, PARTY_MAX);
	}

	/** A cooldown as it is in this phase: a third shorter in the Red Giant. */
	public int cooldown(int ticks) {
		return phase == Phase.RED_GIANT ? (int) Math.round(ticks * GIANT_COOLDOWN) : ticks;
	}

	/** How long between its storms in this phase. */
	public int stormEvery() {
		return switch (phase) {
			case RED_STORM, BURSTING -> STORM_EVERY_STORM;
			case UNBOUND -> STORM_EVERY_UNBOUND;
			case RED_GIANT -> STORM_EVERY_GIANT;
			default -> STORM_EVERY;
		};
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		return form() == Form.CORE ? UNBOUND_SIZE : super.getDefaultDimensions(pose);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
		super.onSyncedDataUpdated(data);
		if (FORM.equals(data)) {
			refreshDimensions();
		}
	}

	// ---------------------------------------------------------------- the server's tick

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (!bound(level)) {
			dismiss(level);
			discard();
			return;
		}
		if (landOnLoad) {
			landOnLoad = false;
			Vec3 woke = woke();
			teleportTo(woke.x, woke.y, woke.z);
			noPhysics = false;
		}
		resetFallDistance();
		bossBar.setProgress(getHealth() / getMaxHealth());
		prune(level);
		for (int i = 0; i < cooldowns.length; i++) {
			if (cooldowns[i] > 0) {
				cooldowns[i]--;
			}
		}
		phaseTicks++;
		switch (phase) {
			case WAITING -> waiting(level);
			case WAKING -> waking(level);
			case CRACKING -> {
				stormTick(level);
				cracking(level);
			}
			case BURSTING -> {
				stormTick(level);
				bursting(level);
			}
			case SENTINEL, RED_STORM, UNBOUND, RED_GIANT -> {
				stormTick(level);
				fight(level);
			}
		}
	}

	/** Whether its instance (if it has one) is still open. */
	private boolean bound(ServerLevel level) {
		return instance == null || lairInstance(level) != null;
	}

	private @Nullable LairInstance lairInstance(ServerLevel level) {
		Lair lair = Lair.of(level.dimension());
		LairInstance open = lair == null || instance == null ? null : Lairs.instance(lair, slot);
		return open != null && open.id.equals(instance) ? open : null;
	}

	private void waiting(ServerLevel level) {
		noPhysics = true;
		setNoGravity(true);
		setDeltaMovement(Vec3.ZERO);
		Vec3 waiting = waiting();
		if (position().distanceToSqr(waiting) > 0.01) {
			setPos(waiting.x, waiting.y, waiting.z);
		}
		face(0.0F);
		setAction(Action.STATUE);
		if (tickCount % 10 == 0 && intruder(level) != null) {
			wake(level);
		}
	}

	/** Someone who may fight it, standing on the crater's floor. */
	private @Nullable Player intruder(ServerLevel level) {
		for (Player player : level.players()) {
			if (LairBosses.eligible(player) && CydonianSanctum.onCraterFloor(origin, player.getX(), player.getY(), player.getZ())) {
				return player;
			}
		}
		return null;
	}

	/** It wakes: its health scaled for the players in its crater, its bar shown, its core flaring. */
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
		setGlow(Glow.FLARE);
		bossBar.setVisible(true);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 3.0F, 0.5F);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_GOLEM_HURT, SoundSource.HOSTILE, 3.0F, 0.4F);
		Vec3 core = core();
		level.sendParticles(CORE_LIGHT, core.x, core.y, core.z, 30, 0.6, 0.6, 0.6, 0.05);
		tell(level, "message.jugcraft.cydonian_sentinel.wakes");
	}

	/** Waking: it stirs on its plinth, then steps down onto the crater's floor in an arc. */
	private void waking(ServerLevel level) {
		noPhysics = true;
		setDeltaMovement(Vec3.ZERO);
		if (phaseTicks <= STIR) {
			if (phaseTicks == STIR) {
				stepFrom = position();
			}
			return;
		}
		int stride = WAKE_TICKS - STIR;
		double share = Math.min(1.0, (phaseTicks - STIR) / (double) stride);
		Vec3 to = woke();
		Vec3 along = stepFrom.add(to.subtract(stepFrom).scale(share));
		setPos(along.x, along.y + STEP_DOWN_RISE * 4.0 * share * (1.0 - share), along.z);
		if (phaseTicks % 10 == 0) {
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_GOLEM_STEP, SoundSource.HOSTILE, 2.0F, 0.5F);
		}
		if (phaseTicks >= WAKE_TICKS) {
			teleportTo(to.x, to.y, to.z);
			noPhysics = false;
			setNoGravity(false);
			phase = Phase.SENTINEL;
			phaseTicks = 0;
			globalCooldown = GLOBAL_COOLDOWN;
			stormTimer = STORM_FIRST;
			setAction(Action.IDLE);
			setGlow(Glow.LIT);
		}
	}

	/** Players near enough to its crater to be fighting (or about to). */
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

	// ---------------------------------------------------------------- its rule: the Red Storm

	/** Its storm ticks on: counting down to its tell, the tell, the storm blowing. */
	private void stormTick(ServerLevel level) {
		switch (storm) {
			case CALM -> {
				if (stormTimer > 0) {
					stormTimer--;
				}
				if (stormTimer <= 0 && fighting() && attack == null && staggerTicks == 0 && !sunk) {
					callStorm(level, Wind.values()[getRandom().nextInt(Wind.values().length)]);
				}
			}
			case TELL -> {
				stormTicks++;
				if (stormTicks % 2 == 0) {
					dust(level, 10, 0.4);
				}
				if (stormTicks >= STORM_TELL) {
					storm = Storm.BLOWING;
					stormTicks = 0;
					if (action() == Action.PLANT || action() == Action.PLANTED) {
						setAction(Action.IDLE);
					}
					level.playSound(null, getX(), getY(), getZ(), SoundEvents.BREEZE_IDLE_AIR, SoundSource.HOSTILE, 2.0F, 0.5F);
				}
			}
			case BLOWING -> {
				stormTicks++;
				dust(level, 24, 1.2);
				if (stormTicks % STORM_HIT_EVERY == 0) {
					scour(level);
				}
				if (stormTicks == STORM_SHARD_AT && (phase == Phase.RED_STORM || unbound())) {
					stormShards(level);
				}
				if (stormTicks >= STORM_TICKS) {
					calm(level);
				}
			}
		}
	}

	/**
	 * It calls its storm from {@code from}: it plants its arms (or, unbound, holds still) for {@value #STORM_TELL} ticks, the
	 * sky reddening with dust streaming in from that side, and the bar names the side.
	 */
	public void callStorm(ServerLevel level, Wind from) {
		wind = from;
		storm = Storm.TELL;
		stormTicks = 0;
		breakOff();
		if (form() == Form.SHELL) {
			setAction(Action.PLANT);
		}
		bossBar.setName(Component.translatable("message.jugcraft.cydonian_sentinel.storm", from.display()));
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2.5F, 0.6F);
		Vec3 centre = centre();
		for (ServerPlayer player : level.players()) {
			if (player.position().distanceTo(centre) <= LEASH + 32.0) {
				player.sendSystemMessage(Component.translatable("message.jugcraft.cydonian_sentinel.storm_tell", from.display()));
			}
		}
	}

	/** The storm passes: its bar names it again, and the next comes after this phase's while. */
	public void calm(ServerLevel level) {
		storm = Storm.CALM;
		stormTicks = 0;
		stormTimer = stormEvery();
		bossBar.setName(barName());
		tell(level, "message.jugcraft.cydonian_sentinel.storm_passes");
	}

	/** The red dust: streaming in from the windward side (its tell), or scouring the crater (the storm), {@code count} motes. */
	private void dust(ServerLevel level, int count, double speed) {
		Vec3 centre = centre();
		Vec3 blows = wind.blows();
		Vec3 across = new Vec3(-blows.z, 0.0, blows.x);
		for (int i = 0; i < count; i++) {
			double along = storm == Storm.TELL ? -CydonianSanctum.CRATER + getRandom().nextDouble() * 6.0
					: (getRandom().nextDouble() * 2.0 - 1.0) * CydonianSanctum.CRATER;
			double side = (getRandom().nextDouble() * 2.0 - 1.0) * CydonianSanctum.CRATER;
			Vec3 at = centre.add(blows.scale(along)).add(across.scale(side)).add(0.0, 0.3 + getRandom().nextDouble() * 4.0, 0.0);
			level.sendParticles(RED_DUST, at.x, at.y, at.z, 0, blows.x, 0.02, blows.z, speed);
		}
	}

	/**
	 * Whether {@code player} is sheltered from a storm blowing {@code from}: rock (a standing stone, or the rim) stands
	 * within {@value #LEE} blocks upwind of their chest.
	 */
	public boolean sheltered(ServerLevel level, Player player, Wind from) {
		Vec3 chest = player.position().add(0.0, 1.0, 0.0);
		Vec3 upwind = chest.subtract(from.blows().scale(LEE));
		return level.clip(new ClipContext(chest, upwind, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.BLOCK;
	}

	/** The storm scours the crater: everyone in it and not sheltered takes its damage and is pushed downwind. */
	public int scour(ServerLevel level) {
		Holder<DamageType> type = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(STORM_DAMAGE_TYPE);
		int hit = 0;
		for (ServerPlayer player : nearby(level)) {
			if (!CydonianSanctum.inCrater(origin, player.getX(), player.getY(), player.getZ()) || sheltered(level, player, wind)) {
				continue;
			}
			if (player.hurtServer(level, new DamageSource(type), damage(STORM_DAMAGE))) {
				hit++;
				push(player, player.getDeltaMovement().add(wind.blows().scale(STORM_PUSH)));
			}
		}
		return hit;
	}

	/**
	 * From the Red Storm on, its volley rides the wind: {@value #STORM_SHARDS} shards loosed from the windward edge of the
	 * crater's floor, spread across its width, flying downwind across it. A standing stone stops them.
	 */
	public int stormShards(ServerLevel level) {
		Vec3 centre = centre().add(0.0, 1.2, 0.0);
		Vec3 blows = wind.blows();
		Vec3 across = new Vec3(-blows.z, 0.0, blows.x);
		int loosed = 0;
		for (int i = 0; i < STORM_SHARDS; i++) {
			double side = -CydonianSanctum.CRATER + 2.0 * CydonianSanctum.CRATER * (i + 0.5) / STORM_SHARDS;
			double edge = Math.sqrt(Math.max(0.0, CydonianSanctum.CRATER * CydonianSanctum.CRATER - side * side)) - 1.0;
			Vec3 from = centre.add(across.scale(side)).subtract(blows.scale(edge));
			things.add(RustShardEntity.loose(level, this, from, blows.scale(RustShardEntity.STORM_SPEED), VOLLEY_DAMAGE).getUUID());
			loosed++;
		}
		level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.BREEZE_IDLE_AIR, SoundSource.HOSTILE, 2.5F, 1.4F);
		return loosed;
	}

	/** The bar's own name now: the Sentinel's, unbound's or the Red Giant's. */
	private Component barName() {
		if (phase == Phase.RED_GIANT) {
			return Component.translatable("message.jugcraft.cydonian_sentinel.giant_bar");
		}
		if (phase == Phase.UNBOUND || phase == Phase.BURSTING && phaseTicks > BURST_TICKS) {
			return Component.translatable("message.jugcraft.cydonian_sentinel.unbound_bar");
		}
		return Component.translatable("entity.jugcraft.cydonian_sentinel");
	}

	// ---------------------------------------------------------------- fighting

	private void fight(ServerLevel level) {
		keepToTheCrater(level);
		if (phase == Phase.SENTINEL && getHealth() <= getMaxHealth() * CRACK_AT) {
			crack(level);
			return;
		}
		if (phase == Phase.RED_STORM && getHealth() <= getMaxHealth() * UNBOUND_AT) {
			burst(level);
			return;
		}
		if (phase == Phase.UNBOUND && getHealth() <= getMaxHealth() * GIANT_AT) {
			redGiant(level);
		}
		Player target = target(level);
		if (target == null) {
			breakOff();
			setAction(staggerTicks > 0 ? Action.STAGGER : form() == Form.CORE ? Action.CORE_IDLE : Action.IDLE);
			move(centre(), speed());
			if (++abandoned >= ABANDON_TICKS) {
				reset(level);
			}
			return;
		}
		abandoned = 0;
		if (staggerTicks > 0) {
			staggerTicks--;
			hold();
			if (staggerTicks == 0) {
				shut();
				setAction(Action.IDLE);
				globalCooldown = cooldown(GLOBAL_COOLDOWN);
			}
			return;
		}
		if (storm == Storm.TELL) {
			hold();
			face(target);
			if (form() == Form.SHELL && action() == Action.PLANT && stormTicks > 20) {
				setAction(Action.PLANTED);
			}
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
		Attack next = choose(level, target);
		if (next == null) {
			approach(target);
		} else {
			begin(level, next, target);
		}
	}

	/** Its pace: striding in its shell, floating faster unbound. */
	private double speed() {
		return form() == Form.CORE ? CORE_SPEED : SPEED;
	}

	/** Strayed farther than its leash from the crater's middle, or fallen below the floor, it strides back to it in a burst of dust. */
	private void keepToTheCrater(ServerLevel level) {
		Vec3 centre = centre();
		double dx = getX() - centre.x;
		double dz = getZ() - centre.z;
		if (dx * dx + dz * dz <= LEASH * LEASH && getY() > centre.y - 3.0) {
			return;
		}
		level.sendParticles(RED_DUST, getX(), getY() + 1.0, getZ(), 20, 0.6, 0.8, 0.6, 0.02);
		breakOff();
		Vec3 back = woke();
		teleportTo(back.x, back.y + (form() == Form.CORE ? CORE_HOVER : 0.0), back.z);
		level.sendParticles(RED_DUST, getX(), getY() + 1.0, getZ(), 20, 0.6, 0.8, 0.6, 0.02);
	}

	/** Its target: the one it has while they are still in reach of its crater, else the nearest who may fight it. */
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

	/** Its foe now, if it has one. */
	public @Nullable Player foe(ServerLevel level) {
		return foe == null ? null : level.getPlayerByUUID(foe);
	}

	/** Moves toward a point {@value #KEEP_DISTANCE} blocks short of {@code target}. */
	private void approach(Player target) {
		Vec3 to = target.position().subtract(position());
		double flat = Math.sqrt(to.x * to.x + to.z * to.z);
		Vec3 goal = flat > KEEP_DISTANCE + 0.5
				? new Vec3(target.getX() - to.x / flat * KEEP_DISTANCE, getY(), target.getZ() - to.z / flat * KEEP_DISTANCE)
				: position();
		move(goal, speed());
	}

	/** Moves toward {@code goal} at {@code speed}: striding on the floor in its shell, floating over it unbound. */
	private void move(Vec3 goal, double speed) {
		Vec3 to = new Vec3(goal.x - getX(), 0.0, goal.z - getZ());
		double length = to.length();
		Vec3 step = length < 0.05 ? Vec3.ZERO : to.scale(Math.min(speed, length) / length);
		setDeltaMovement(step.x, vertical(), step.z);
	}

	/** Holds still where it is (on the floor, or floating). */
	private void hold() {
		setDeltaMovement(0.0, vertical(), 0.0);
	}

	/** Its rise or fall this tick: falling in its shell; unbound, easing to its height (or to the floor while sunk). */
	private double vertical() {
		if (form() == Form.SHELL) {
			return Math.min(getDeltaMovement().y, 0.0);
		}
		double want = floorY() + (sunk ? 0.0 : CORE_HOVER);
		return Math.max(-0.3, Math.min(0.3, (want - getY()) * 0.25));
	}

	private void face(Entity target) {
		face(target.position().subtract(position()));
	}

	private void face(Vec3 way) {
		if (way.x * way.x + way.z * way.z > 1.0E-4) {
			face((float) (Math.toDegrees(Math.atan2(way.z, way.x)) - 90.0));
		}
	}

	private void face(float yaw) {
		setYRot(yaw);
		setYBodyRot(yaw);
		setYHeadRot(yaw);
	}

	/** The way it faces, flat. */
	public Vec3 facing() {
		double yaw = Math.toRadians(getYRot());
		return new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
	}

	/** Picks an attack it can use now on {@code target}, or null. */
	private @Nullable Attack choose(ServerLevel level, Player target) {
		double distance = target.distanceTo(this);
		List<Attack> ready = new ArrayList<>();
		for (Attack candidate : Attack.values()) {
			boolean phaseAllows = switch (phase) {
				case SENTINEL -> candidate.sentinel;
				case RED_STORM -> candidate.storm;
				default -> candidate.core;
			};
			if (!phaseAllows || cooldowns[candidate.ordinal()] > 0 || distance < candidate.near || distance > candidate.far) {
				continue;
			}
			if (candidate == Attack.SENTRY_CALL && sentries.size() >= SENTRY_CAP) {
				continue;
			}
			ready.add(candidate);
			if (candidate == Attack.HAMMERFALL) {
				ready.add(candidate);  // close in, its fists come most often
			}
		}
		return ready.isEmpty() ? null : ready.get(getRandom().nextInt(ready.size()));
	}

	/** Starts {@code next} on {@code target}: its wind-up begins. */
	public void begin(ServerLevel level, Attack next, Player target) {
		breakOff();
		attack = next;
		attackTicks = 0;
		foe = target.getUUID();
		cooldowns[next.ordinal()] = cooldown(next.cooldown);
		hold();
		face(target);
		switch (next) {
			case HAMMERFALL -> {
				setAction(Action.HAMMER_WINDUP);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 2.0F, 0.5F);
			}
			case SHARD_VOLLEY -> {
				setAction(Action.VOLLEY_WINDUP);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 2.0F, 0.5F);
			}
			case QUAKE_LINE -> {
				setAction(Action.QUAKE_WINDUP);
				quakeFrom = new Vec3(getX(), floorY(), getZ());
				aim = flat(target.position().subtract(position()));
			}
			case CORE_LANCE -> {
				setAction(Action.LANCE_WINDUP);
				open();
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 2.5F, 0.6F);
			}
			case SENTRY_CALL -> {
				setAction(Action.CALL_WINDUP);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_GOLEM_HURT, SoundSource.HOSTILE, 2.0F, 0.6F);
			}
			case SWEEPING_LANCE -> {
				setAction(Action.SWEEP_WINDUP);
				Vec3 to = target.position().subtract(position());
				sweepFrom = Math.toDegrees(Math.atan2(to.z, to.x)) - SWEEP_ANGLE / 2.0;
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 2.5F, 0.8F);
			}
			case SHARD_BURST -> {
				setAction(Action.BURST_WINDUP);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 3.0F, 1.6F);
			}
			case STARFALL -> {
				setAction(Action.STARFALL_WINDUP);
				markStarfall(level);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 3.0F, 1.2F);
			}
		}
	}

	private void perform(ServerLevel level, Player target) {
		Attack current = attack;
		if (current == null) {
			return;
		}
		int t = attackTicks;
		hold();
		switch (current) {
			case HAMMERFALL -> {
				if (t < current.windup) {
					if (t % 4 == 0) {
						Vec3 up = position().add(0.0, 4.6, 0.0);
						level.sendParticles(RED_DUST, up.x, up.y, up.z, 2, 0.4, 0.2, 0.4, 0.0);
					}
				} else if (t == current.windup) {
					setAction(Action.HAMMER);
					hammer(level);
				}
			}
			case SHARD_VOLLEY -> {
				if (t < current.windup) {
					face(target);
				} else if (t == current.windup) {
					setAction(Action.VOLLEY);
					volley(level, target);
				}
			}
			case QUAKE_LINE -> quake(level, t);
			case CORE_LANCE -> lance(level, target, t);
			case SENTRY_CALL -> {
				if (t < current.windup) {
					if (t % 3 == 0) {
						for (int pit : pits(target.position(), SENTRIES_RISE)) {
							Vec3 at = pit(pit);
							level.sendParticles(CORE_LIGHT, at.x, at.y + 0.2, at.z, 3, 0.6, 0.05, 0.6, 0.0);
						}
					}
				} else if (t == current.windup) {
					setAction(Action.CALL);
					callSentries(level, target);
				}
			}
			case SWEEPING_LANCE -> sweep(level, t);
			case SHARD_BURST -> {
				if (t == current.windup) {
					setAction(Action.SHARD_BURST);
					shardBurst(level);
				}
			}
			case STARFALL -> starfall(level, t);
		}
		if (attack == current && attackTicks >= current.length()) {
			attack = null;
			attackTicks = 0;
			globalCooldown = cooldown(GLOBAL_COOLDOWN);
			if (sunk) {
				sunk = false;
				setGlow(phase == Phase.RED_GIANT ? Glow.GIANT : Glow.UNBOUND);
			}
			if (current == Attack.CORE_LANCE) {
				shut();
			}
			Action action = action();
			if (action == Action.LANCE || action == Action.SWEEP || action == Action.SUNK || action.name().endsWith("_WINDUP")) {
				setAction(form() == Form.CORE ? Action.CORE_IDLE : Action.IDLE);
			}
		}
	}

	/**
	 * The attack under way is broken off before its end (its foe gone, drawn back to its crater, its storm called, or
	 * another begun): a Core Lance's plates swing shut, and a core sunk after its Starfall rises again, as they would have
	 * at the attack's end. A stagger keeps the plates open on purpose, and drops its attack itself.
	 */
	private void breakOff() {
		if (attack == Attack.CORE_LANCE) {
			shut();
		}
		if (sunk) {
			sunk = false;
			setGlow(phase == Phase.RED_GIANT ? Glow.GIANT : Glow.UNBOUND);
		}
		attack = null;
		attackTicks = 0;
	}

	/** The way to {@code to}, flat and of length one (or its facing, if {@code to} is straight up). */
	private Vec3 flat(Vec3 to) {
		Vec3 flat = new Vec3(to.x, 0.0, to.z);
		return flat.lengthSqr() < 1.0E-6 ? facing() : flat.normalize();
	}

	/**
	 * Hammerfall: both fists slam the floor {@value #HAMMER_AHEAD} blocks before it: {@value #HAMMER_DAMAGE} damage to
	 * everyone within {@value #HAMMER_REACH} blocks of where they land, and everyone within {@value #HAMMER_KNOCK} is thrown
	 * back. Returns how many it hurt.
	 */
	public int hammer(ServerLevel level) {
		Vec3 slam = new Vec3(getX(), floorY(), getZ()).add(facing().scale(HAMMER_AHEAD));
		int hit = 0;
		for (ServerPlayer player : nearby(level)) {
			Vec3 to = player.position().subtract(slam);
			double flat = Math.sqrt(to.x * to.x + to.z * to.z);
			if (flat > HAMMER_KNOCK + player.getBbWidth() / 2.0 || Math.abs(to.y) > 2.5) {
				continue;
			}
			boolean struck = flat <= HAMMER_REACH + player.getBbWidth() / 2.0
					&& player.hurtServer(level, damageSources().mobAttack(this), damage(HAMMER_DAMAGE));
			if (struck) {
				hit++;
			}
			Vec3 away = flat < 1.0E-3 ? facing() : new Vec3(to.x / flat, 0.0, to.z / flat);
			push(player, away.scale(HAMMER_THROW * (struck ? 1.0 : 0.6)).add(0.0, 0.4, 0.0));
		}
		level.sendParticles(ParticleTypes.EXPLOSION, slam.x, slam.y + 0.3, slam.z, 2, 0.6, 0.1, 0.6, 0.0);
		level.sendParticles(RED_DUST, slam.x, slam.y + 0.3, slam.z, 30, 2.0, 0.2, 2.0, 0.05);
		level.playSound(null, slam.x, slam.y, slam.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.6F, 0.6F);
		return hit;
	}

	/** Sets {@code player} moving at {@code motion}, and tells their client (a player's own motion is theirs to move). */
	static void push(ServerPlayer player, Vec3 motion) {
		player.setDeltaMovement(motion);
		player.connection.send(new ClientboundSetEntityMotionPacket(player));
	}

	/** Shard Volley: {@value #VOLLEY_SHARDS} shards in a fan {@value #VOLLEY_FAN} degrees wide at {@code target}. */
	public int volley(ServerLevel level, Player target) {
		Vec3 from = position().add(0.0, 2.2, 0.0).add(facing().scale(1.4));
		Vec3 at = target.getBoundingBox().getCenter().subtract(from);
		double yaw = Math.atan2(at.z, at.x);
		double pitch = Math.atan2(at.y, Math.sqrt(at.x * at.x + at.z * at.z));
		for (int i = 0; i < VOLLEY_SHARDS; i++) {
			double share = VOLLEY_SHARDS == 1 ? 0.5 : i / (double) (VOLLEY_SHARDS - 1);
			double a = yaw + Math.toRadians(VOLLEY_FAN * (share - 0.5));
			Vec3 way = new Vec3(Math.cos(a) * Math.cos(pitch), Math.sin(pitch), Math.sin(a) * Math.cos(pitch));
			things.add(RustShardEntity.loose(level, this, from, way.scale(RustShardEntity.VOLLEY_SPEED), VOLLEY_DAMAGE).getUUID());
		}
		level.playSound(null, from.x, from.y, from.z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 2.0F, 0.6F);
		return VOLLEY_SHARDS;
	}

	/**
	 * Quake Line, {@code t} ticks into it: through the wind-up a glowing crack runs from its feet at its foe; as it ends,
	 * spikes of red rock burst up all along the line, a clump every {@value #QUAKE_STEP} blocks ({@link #quakeStrike}).
	 */
	private void quake(ServerLevel level, int t) {
		Attack quake = Attack.QUAKE_LINE;
		if (t < quake.windup) {
			double reach = QUAKE_LENGTH * (t + 1) / quake.windup;
			for (double along = Math.max(0.0, reach - 2.0); along <= reach; along += 0.7) {
				Vec3 at = quakeFrom.add(aim.scale(along));
				level.sendParticles(CORE_LIGHT, at.x, at.y + 0.1, at.z, 1, 0.1, 0.02, 0.1, 0.0);
			}
			return;
		}
		if (t == quake.windup) {
			setAction(Action.QUAKE);
			for (double along = QUAKE_STEP; along <= QUAKE_LENGTH + 1.0E-6; along += QUAKE_STEP) {
				Vec3 at = quakeFrom.add(aim.scale(along));
				things.add(QuakeSpikeEntity.burst(level, this, at, quake.active).getUUID());
			}
			quakeStrike(level);
		}
	}

	/** Sets the line of a Quake Line, as its wind-up would (for tests): from {@code from} on the floor, along {@code way}. */
	public void quakeLine(Vec3 from, Vec3 way) {
		quakeFrom = new Vec3(from.x, floorY(), from.z);
		aim = flat(way);
	}

	/**
	 * The spikes burst up along the Quake Line: each player within {@value #QUAKE_WIDTH} blocks of it, and on the floor,
	 * takes {@value #QUAKE_DAMAGE} damage and is thrown up. Returns how many it hurt.
	 */
	public int quakeStrike(ServerLevel level) {
		int hit = 0;
		for (ServerPlayer player : nearby(level)) {
			if (player.getY() > floorY() + 2.5 || player.getY() < floorY() - 1.5) {
				continue;
			}
			Vec3 to = new Vec3(player.getX() - quakeFrom.x, 0.0, player.getZ() - quakeFrom.z);
			double along = to.dot(aim);
			double off = to.subtract(aim.scale(along)).length();
			if (along < 0.0 || along > QUAKE_LENGTH + 0.5 || off > QUAKE_WIDTH + player.getBbWidth() / 2.0) {
				continue;
			}
			if (player.hurtServer(level, damageSources().mobAttack(this), damage(QUAKE_DAMAGE))) {
				hit++;
				push(player, new Vec3(player.getDeltaMovement().x, QUAKE_TOSS, player.getDeltaMovement().z));
			}
		}
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 3.0F, 0.5F);
		return hit;
	}

	/**
	 * Core Lance, {@code t} ticks into it: through the wind-up its plates stand open and its core brightens, turning to its
	 * foe; as it ends it fixes its aim, and through the active ticks its beam burns along it every {@value #LANCE_EVERY}
	 * ticks ({@link #beam}).
	 */
	private void lance(ServerLevel level, Player target, int t) {
		Attack lance = Attack.CORE_LANCE;
		if (t < lance.windup) {
			face(target);
			if (t % 3 == 0) {
				Vec3 core = core();
				level.sendParticles(CORE_LIGHT, core.x, core.y, core.z, 3, 0.25, 0.25, 0.25, 0.0);
			}
			return;
		}
		if (t == lance.windup) {
			setAction(Action.LANCE);
			aim = target.getBoundingBox().getCenter().subtract(core()).normalize();
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 3.0F, 1.6F);
		}
		if (t < lance.windup + lance.active) {
			if (t % 2 == 0) {
				ray(level, core(), aim, reach(level, core(), aim));
			}
			if ((t - lance.windup) % LANCE_EVERY == 0) {
				beam(level, core(), aim);
			}
		}
	}

	/** Sets where its lance points, as its wind-up's end would (for tests). */
	public void aim(Vec3 way) {
		aim = way.normalize();
	}

	/** How far its beam from {@code from} along {@code way} reaches before rock stops it (at most {@value #LANCE_LENGTH}). */
	public double reach(ServerLevel level, Vec3 from, Vec3 way) {
		Vec3 to = from.add(way.scale(LANCE_LENGTH));
		HitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation().distanceTo(from) : LANCE_LENGTH;
	}

	/** One burn of its beam from {@code from} along {@code way}: {@value #LANCE_DAMAGE} to every player in it. Returns how many. */
	public int beam(ServerLevel level, Vec3 from, Vec3 way) {
		double length = reach(level, from, way);
		int hit = 0;
		for (ServerPlayer player : nearby(level)) {
			Vec3 to = player.getBoundingBox().getCenter().subtract(from);
			double along = to.dot(way);
			if (along < 0.0 || along > length) {
				continue;
			}
			double off = to.subtract(way.scale(along)).length();
			if (off > LANCE_WIDTH + player.getBbWidth() / 2.0) {
				continue;
			}
			if (player.hurtServer(level, damageSources().magic(), damage(LANCE_DAMAGE))) {
				hit++;
			}
		}
		return hit;
	}

	/** Red light along a beam, a mote every half block. */
	private void ray(ServerLevel level, Vec3 from, Vec3 way, double length) {
		for (double along = 0.5; along <= length; along += 0.5) {
			Vec3 at = from.add(way.scale(along));
			level.sendParticles(CORE_LIGHT, at.x, at.y, at.z, 1, 0.03, 0.03, 0.03, 0.0);
		}
	}

	/**
	 * Sweeping Lance, {@code t} ticks into it: through the wind-up its light gathers; then its beam sweeps
	 * {@value #SWEEP_ANGLE} degrees across where its foe stood, over the active ticks. Nothing staggers it.
	 */
	private void sweep(ServerLevel level, int t) {
		Attack sweep = Attack.SWEEPING_LANCE;
		if (t < sweep.windup) {
			if (t % 3 == 0) {
				Vec3 core = core();
				level.sendParticles(CORE_LIGHT, core.x, core.y, core.z, 4, 0.4, 0.4, 0.4, 0.0);
			}
			return;
		}
		if (t == sweep.windup) {
			setAction(Action.SWEEP);
		}
		if (t < sweep.windup + sweep.active) {
			double angle = Math.toRadians(sweepFrom + SWEEP_ANGLE * (t - sweep.windup) / (double) sweep.active);
			Vec3 way = new Vec3(Math.cos(angle), -0.04, Math.sin(angle)).normalize();
			face(way);
			if (t % 2 == 0) {
				ray(level, core(), way, reach(level, core(), way));
			}
			if ((t - sweep.windup) % LANCE_EVERY == 0) {
				beam(level, core(), way);
			}
		}
	}

	/** Shard Burst: {@value #BURST_SHARDS} shards fly outward all round it, level with its core. */
	public int shardBurst(ServerLevel level) {
		Vec3 from = core();
		for (int i = 0; i < BURST_SHARDS; i++) {
			double a = Math.toRadians(360.0 * i / BURST_SHARDS);
			Vec3 way = new Vec3(Math.cos(a), -0.02, Math.sin(a));
			things.add(RustShardEntity.loose(level, this, from.add(way.scale(1.0)), way.scale(RustShardEntity.BURST_SPEED), BURST_DAMAGE).getUUID());
		}
		level.sendParticles(CORE_LIGHT, from.x, from.y, from.z, 20, 0.6, 0.6, 0.6, 0.1);
		level.playSound(null, from.x, from.y, from.z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 3.0F, 0.5F);
		return BURST_SHARDS;
	}

	/** Starfall's marks: {@value #STARFALL_MARKS} (in the Red Giant {@value #GIANT_MARKS}) on the floor near each player in its crater. */
	public void markStarfall(ServerLevel level) {
		marks.clear();
		int each = phase == Phase.RED_GIANT ? GIANT_MARKS : STARFALL_MARKS;
		for (ServerPlayer player : nearby(level)) {
			if (!CydonianSanctum.inCrater(origin, player.getX(), player.getY(), player.getZ())) {
				continue;
			}
			for (int i = 0; i < each; i++) {
				double a = getRandom().nextDouble() * Math.PI * 2.0;
				double r = i == 0 ? getRandom().nextDouble() * MARK_SPREAD * 0.5 : MARK_SPREAD * (0.5 + 0.5 * getRandom().nextDouble());
				marks.add(new Vec3(player.getX() + Math.cos(a) * r, floorY(), player.getZ() + Math.sin(a) * r));
			}
		}
	}

	/** Sets the Starfall's marks, as its wind-up would (for tests). */
	public void marks(List<Vec3> at) {
		marks.clear();
		marks.addAll(at);
	}

	/**
	 * Starfall, {@code t} ticks into it: through the wind-up red marks glow on the floor; {@value #METEOR_FALL} ticks before
	 * it ends meteors start falling at them; as it ends they strike ({@link #starfallStrike}), and it sinks to the floor,
	 * dimmed, for the attack's recovery.
	 */
	private void starfall(ServerLevel level, int t) {
		Attack starfall = Attack.STARFALL;
		if (t < starfall.windup) {
			if (t % 4 == 0) {
				for (Vec3 mark : marks) {
					for (int i = 0; i < 8; i++) {
						double a = Math.PI * 2.0 * i / 8;
						level.sendParticles(MARK, mark.x + Math.cos(a) * STARFALL_RADIUS, mark.y + 0.1, mark.z + Math.sin(a) * STARFALL_RADIUS,
								1, 0.0, 0.0, 0.0, 0.0);
					}
				}
			}
			if (t == starfall.windup - METEOR_FALL) {
				for (Vec3 mark : marks) {
					things.add(SentinelMeteorEntity.fall(level, this, mark, METEOR_FALL).getUUID());
				}
			}
			return;
		}
		if (t == starfall.windup) {
			starfallStrike(level);
			sunk = true;
			setGlow(Glow.DIM);
			setAction(Action.SUNK);
		}
	}

	/** The meteors strike the marks: {@value #STARFALL_DAMAGE} to each player within {@value #STARFALL_RADIUS} of one (once). */
	public int starfallStrike(ServerLevel level) {
		Set<UUID> struck = new HashSet<>();
		for (Vec3 mark : marks) {
			level.sendParticles(ParticleTypes.EXPLOSION, mark.x, mark.y + 0.5, mark.z, 1, 0.0, 0.0, 0.0, 0.0);
			level.sendParticles(ParticleTypes.LAVA, mark.x, mark.y + 0.2, mark.z, 6, 0.8, 0.1, 0.8, 0.0);
			level.playSound(null, mark.x, mark.y, mark.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.4F, 0.7F);
			for (ServerPlayer player : nearby(level)) {
				double flat = Math.hypot(player.getX() - mark.x, player.getZ() - mark.z);
				if (struck.contains(player.getUUID()) || flat > STARFALL_RADIUS + player.getBbWidth() / 2.0
						|| Math.abs(player.getY() - mark.y) > 2.5) {
					continue;
				}
				if (player.hurtServer(level, damageSources().mobAttack(this), damage(STARFALL_DAMAGE))) {
					struck.add(player.getUUID());
				}
			}
		}
		return struck.size();
	}

	// ---------------------------------------------------------------- its weak point: the core

	/** Its chest plates swing open: its core takes double damage, and its armour does not count. */
	public void open() {
		entityData.set(PLATES, true);
		setGlow(Glow.OPEN);
		setArmor(0.0);
	}

	/** Its plates swing shut. */
	public void shut() {
		entityData.set(PLATES, false);
		if (phase != Phase.WAITING && form() == Form.SHELL) {
			setGlow(Glow.LIT);
		}
		setArmor(form() == Form.CORE ? CORE_ARMOR : ARMOR);
	}

	private void setArmor(double armor) {
		AttributeInstance shell = getAttribute(Attributes.ARMOR);
		if (shell != null) {
			shell.setBaseValue(armor);
		}
	}

	/** It reels: the attack under way is spoiled, and it does nothing for {@code ticks}. Its plates stay open meanwhile. */
	public void stagger(int ticks) {
		attack = null;
		attackTicks = 0;
		staggerTicks = Math.max(staggerTicks, ticks);
		setAction(Action.STAGGER);
	}

	// ---------------------------------------------------------------- the Shell Cracks, the Unbound Core, the Red Giant

	/** The Shell Cracks: it roars for {@value #CRACK_TICKS} ticks, unhurt, and its sentries rise; then the Red Storm. */
	public void crack(ServerLevel level) {
		attack = null;
		staggerTicks = 0;
		shut();
		phase = Phase.CRACKING;
		phaseTicks = 0;
		setAction(Action.ROAR);
		setGlow(Glow.FLARE);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 3.0F, 0.5F);
		Player target = target(level);
		raiseSentries(level, target == null ? centre() : target.position());
		tell(level, "message.jugcraft.cydonian_sentinel.cracks");
	}

	private void cracking(ServerLevel level) {
		hold();
		if (phaseTicks % 6 == 0) {
			level.sendParticles(RED_DUST, getX(), getY() + 2.5, getZ(), 10, 0.8, 1.2, 0.8, 0.03);
		}
		if (phaseTicks >= CRACK_TICKS) {
			phase = Phase.RED_STORM;
			phaseTicks = 0;
			globalCooldown = GLOBAL_COOLDOWN;
			setAction(Action.IDLE);
			setGlow(Glow.LIT);
		}
	}

	/** The Unbound Core: its shell bursts apart over {@value #BURST_TICKS} ticks, unhurt, and the core floats free. */
	public void burst(ServerLevel level) {
		attack = null;
		staggerTicks = 0;
		phase = Phase.BURSTING;
		phaseTicks = 0;
		entityData.set(PLATES, true);
		setAction(Action.BURST);
		setGlow(Glow.FLARE);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_GOLEM_DEATH, SoundSource.HOSTILE, 3.0F, 0.5F);
		tell(level, "message.jugcraft.cydonian_sentinel.unbound");
	}

	private void bursting(ServerLevel level) {
		hold();
		if (phaseTicks % 5 == 0) {
			level.sendParticles(CORE_LIGHT, getX(), getY() + 2.5, getZ(), 6, 0.8, 1.2, 0.8, 0.05);
		}
		if (phaseTicks >= BURST_TICKS) {
			unbind(level);
		}
	}

	/** Its shell is gone: the core floats free, its armour {@value #CORE_ARMOR}, faster. */
	public void unbind(ServerLevel level) {
		Vec3 core = core();
		level.sendParticles(ParticleTypes.EXPLOSION, core.x, core.y, core.z, 3, 0.8, 1.0, 0.8, 0.0);
		level.sendParticles(RED_DUST, core.x, core.y, core.z, 60, 1.2, 1.6, 1.2, 0.1);
		level.playSound(null, core.x, core.y, core.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.5F, 0.6F);
		entityData.set(FORM, Form.CORE.ordinal());
		refreshDimensions();
		entityData.set(PLATES, false);
		setNoGravity(true);
		setArmor(CORE_ARMOR);
		setPos(getX(), floorY() + CORE_HOVER, getZ());
		phase = Phase.UNBOUND;
		phaseTicks = 0;
		globalCooldown = GLOBAL_COOLDOWN;
		setAction(Action.CORE_IDLE);
		setGlow(Glow.UNBOUND);
		bossBar.setName(barName());
	}

	/** Red Giant: its cooldowns and its pause a third shorter, its storms oftener, its Starfall marking twice. */
	public void redGiant(ServerLevel level) {
		phase = Phase.RED_GIANT;
		phaseTicks = 0;
		stormTimer = Math.min(stormTimer, stormEvery());
		if (!sunk) {
			setGlow(Glow.GIANT);
		}
		if (storm == Storm.CALM) {
			bossBar.setName(barName());
		}
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 3.0F, 0.4F);
		tell(level, "message.jugcraft.cydonian_sentinel.giant");
	}

	// ---------------------------------------------------------------- its sentries and its things

	/** The {@code count} small craters nearest {@code to} (indexes of {@link CydonianSanctum#PITS}). */
	public List<Integer> pits(Vec3 to, int count) {
		List<Integer> out = new ArrayList<>();
		for (int i = 0; i < CydonianSanctum.PITS.length; i++) {
			out.add(i);
		}
		out.sort((a, b) -> Double.compare(pit(a).distanceToSqr(to), pit(b).distanceToSqr(to)));
		return out.subList(0, Math.min(count, out.size()));
	}

	/** Sentry Call: {@value #SENTRIES_RISE} Shard Sentries rise from the small craters nearest {@code target}. */
	public int callSentries(ServerLevel level, Player target) {
		return raiseSentries(level, target.position());
	}

	private int raiseSentries(ServerLevel level, Vec3 near) {
		int raised = 0;
		for (int pit : pits(near, SENTRIES_RISE)) {
			if (raise(level, pit(pit)) != null) {
				raised++;
			}
		}
		return raised;
	}

	/** One Shard Sentry rises at {@code at}, unless {@value #SENTRY_CAP} already float; returns it, or null. */
	public @Nullable ShardSentryEntity raise(ServerLevel level, Vec3 at) {
		if (sentries.size() >= SENTRY_CAP) {
			return null;
		}
		ShardSentryEntity sentry = ShardSentryEntity.rise(level, this, at);
		sentries.add(sentry.getUUID());
		return sentry;
	}

	/** Forgets what is gone. */
	private void prune(ServerLevel level) {
		sentries.removeIf(id -> !(level.getEntity(id) instanceof ShardSentryEntity sentry) || !sentry.isAlive());
		things.removeIf(id -> level.getEntity(id) == null);
	}

	/** A shard of its (or one of its sentries') was loosed: it is its own, and goes with it. */
	void owns(Entity thing) {
		things.add(thing.getUUID());
	}

	/** Whoever hurt it or its sentries has taken part. */
	public void took(ServerPlayer player, float amount) {
		damageBy.merge(player.getUUID(), amount, Float::sum);
	}

	/** Its participants: everyone who hurt it or its sentries, and is still near its crater. */
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

	// ---------------------------------------------------------------- abandoned, and defeated

	/**
	 * Left alone: it strides back to its plinth and reforms, healed; its sentries crumble, its shards, spikes and meteors are
	 * gone, its storm dies, and who hurt it is forgotten.
	 */
	public void reset(ServerLevel level) {
		dismiss(level);
		entityData.set(FORM, Form.SHELL.ordinal());
		refreshDimensions();
		entityData.set(PLATES, false);
		phase = Phase.WAITING;
		phaseTicks = 0;
		attack = null;
		abandoned = 0;
		staggerTicks = 0;
		sunk = false;
		foe = null;
		storm = Storm.CALM;
		stormTicks = 0;
		stormTimer = STORM_FIRST;
		damageBy.clear();
		marks.clear();
		Arrays.fill(cooldowns, 0);
		setArmor(ARMOR);
		AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
		if (health != null) {
			health.setBaseValue(HEALTH);
		}
		setHealth(getMaxHealth());
		setGlow(Glow.DARK);
		noPhysics = true;
		setNoGravity(true);
		Vec3 waiting = waiting();
		teleportTo(waiting.x, waiting.y, waiting.z);
		face(0.0F);
		setAction(Action.STATUE);
		bossBar.setName(Component.translatable("entity.jugcraft.cydonian_sentinel"));
		bossBar.setVisible(false);
		tell(level, "message.jugcraft.cydonian_sentinel.reset");
	}

	/** Its sentries crumble, and its shards, spikes and meteors are gone. */
	private void dismiss(ServerLevel level) {
		for (UUID id : List.copyOf(sentries)) {
			if (level.getEntity(id) instanceof ShardSentryEntity sentry) {
				sentry.crumble(level);
			}
		}
		sentries.clear();
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
	 * It has fallen: its sentries crumble and its things are gone, its storm dies, each participant gets their loot, its
	 * lair's instance ends (it closes once everyone has left), its core gutters out, and the Red Mist opens in the
	 * crater's middle: the way home.
	 */
	private void defeated(ServerLevel level) {
		dismiss(level);
		storm = Storm.CALM;
		SentinelLoot.reward(level, this, participants(level));
		setGlow(Glow.DIM);
		LairInstance open = lairInstance(level);
		if (open != null) {
			Lairs.end(open);
		}
		BlockState mist = JugcraftLairs.RED_MIST.defaultBlockState().setValue(LairExitBlock.AXIS, Direction.Axis.X);
		for (BlockPos at : CydonianSanctum.craterMistCells(origin)) {
			if (level.getBlockState(at).isAir()) {
				level.setBlock(at, mist, Block.UPDATE_ALL);
			}
		}
		Vec3 core = core();
		level.sendParticles(ParticleTypes.LARGE_SMOKE, core.x, core.y, core.z, 30, 0.6, 0.6, 0.6, 0.02);
		bossBar.setVisible(false);
		tell(level, "message.jugcraft.cydonian_sentinel.defeated");
	}

	/** Tells everyone near its crater. */
	void tell(ServerLevel level, String key) {
		Vec3 centre = centre();
		for (ServerPlayer player : level.players()) {
			if (player.position().distanceTo(centre) <= LEASH + 32.0) {
				player.sendSystemMessage(Component.translatable(key));
			}
		}
	}

	// ---------------------------------------------------------------- what it is immune to, and its core

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
		if (phase == Phase.WAKING || phase == Phase.CRACKING || phase == Phase.BURSTING) {
			return false;
		}
		float dealt = amount;
		if (platesOpen()) {
			dealt = amount * CORE_MULTIPLIER;
			if (attack == Attack.CORE_LANCE && attackTicks < Attack.CORE_LANCE.windup && amount >= STAGGER_DAMAGE
					&& source.getEntity() instanceof Player) {
				stagger(STAGGER_TICKS);
				level.sendParticles(CORE_LIGHT, getX(), getY() + CORE_UP, getZ(), 20, 0.4, 0.4, 0.4, 0.1);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.HOSTILE, 2.5F, 0.6F);
				tell(level, "message.jugcraft.cydonian_sentinel.staggered");
			}
		}
		boolean hurt = super.hurtServer(level, source, dealt);
		if (hurt && source.getEntity() instanceof ServerPlayer player) {
			took(player, dealt);
		}
		return hurt;
	}

	@Override
	public boolean canBeAffected(MobEffectInstance effect) {
		return !effect.is(MobEffects.WITHER) && !effect.is(MobEffects.POISON) && super.canBeAffected(effect);
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
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
		return phase == Phase.WAITING ? null : SoundEvents.BEACON_POWER_SELECT;
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
	public float getVoicePitch() {
		return 0.5F;
	}

	// ---------------------------------------------------------------- saving and syncing

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ACTION, Action.STATUE.ordinal());
		builder.define(GLOW, Glow.DARK.ordinal());
		builder.define(FORM, Form.SHELL.ordinal());
		builder.define(PLATES, false);
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
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		instance = input.read("instance", UUIDUtil.CODEC).orElse(null);
		slot = input.getIntOr("slot", -1);
		origin = input.read("origin", BlockPos.CODEC).orElse(BlockPos.containing(getX() - CydonianSanctum.WAIT_X,
				getY() - CydonianSanctum.WAIT_Y, getZ() - CydonianSanctum.WAIT_Z));
		try {
			phase = Phase.valueOf(input.getStringOr("phase", Phase.WAITING.name()));
		} catch (IllegalArgumentException e) {
			phase = Phase.WAITING;
		}
		// A passing moment is not resumed: waking, it comes down to the floor; cracking, its storm phase begins; bursting,
		// it is unbound.
		if (phase == Phase.WAKING) {
			landOnLoad = true;
			phase = Phase.SENTINEL;
		} else if (phase == Phase.CRACKING) {
			phase = Phase.RED_STORM;
		} else if (phase == Phase.BURSTING) {
			phase = Phase.UNBOUND;
		}
		boolean core = phase == Phase.UNBOUND || phase == Phase.RED_GIANT;
		entityData.set(FORM, (core ? Form.CORE : Form.SHELL).ordinal());
		noPhysics = phase == Phase.WAITING;
		setNoGravity(phase == Phase.WAITING || core);
		setArmor(core ? CORE_ARMOR : ARMOR);
		setGlow(phase == Phase.WAITING ? Glow.DARK : phase == Phase.RED_GIANT ? Glow.GIANT : core ? Glow.UNBOUND : Glow.LIT);
		setAction(phase == Phase.WAITING ? Action.STATUE : core ? Action.CORE_IDLE : Action.IDLE);
		bossBar.setVisible(phase != Phase.WAITING);
		bossBar.setName(barName());
	}

	// ---------------------------------------------------------------- its animations

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<CydonianSentinelEntity>("body", 4, test -> {
			CydonianSentinelEntity sentinel = test.animatable();
			if (sentinel.isDeadOrDying()) {
				return test.setAndContinue(sentinel.form() == Form.CORE ? CORE_DEATH : DEATH);
			}
			Action action = sentinel.action();
			if (action == Action.IDLE && test.isMoving()) {
				return test.setAndContinue(WALK);
			}
			if (action == Action.CORE_IDLE && test.isMoving()) {
				return test.setAndContinue(CORE_MOVE);
			}
			return test.setAndContinue(BODY[action.ordinal()]);
		}));
		controllers.add(new AnimationController<CydonianSentinelEntity>("core", 4,
				test -> test.setAndContinue(GLOWS[test.animatable().isDeadOrDying() ? Glow.DIM.ordinal() : test.animatable().glow().ordinal()])));
		controllers.add(new AnimationController<CydonianSentinelEntity>("plates", 3,
				test -> test.setAndContinue(test.animatable().platesOpen() ? PLATES_OPEN : PLATES_SHUT)));
		controllers.add(new AnimationController<CydonianSentinelEntity>("form", 2,
				test -> test.setAndContinue(test.animatable().form() == Form.CORE ? FORM_CORE : FORM_SHELL)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
