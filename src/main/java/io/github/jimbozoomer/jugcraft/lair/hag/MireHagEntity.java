package io.github.jimbozoomer.jugcraft.lair.hag;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.LoopType;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.lair.FenLampBlock;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairBosses;
import io.github.jimbozoomer.jugcraft.lair.LairBrazierBlock;
import io.github.jimbozoomer.jugcraft.lair.LairExitBlock;
import io.github.jimbozoomer.jugcraft.lair.LairInstance;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Mire Hag (docs/features/mire-hag.md): the Lantern Fen's boss, a crone of the deep fen bent nearly double and a head
 * taller than a man even so, in a shawl of grey moss over rotted sackcloth, eyes of marsh-light green, leaning on a staff
 * of bog oak with a little lantern swinging from its crook. She waits on her porch bent over her cauldron, and wakes when
 * someone who may fight her comes into the marsh or strikes her: she looks up, the lamps gutter, and she steps down into
 * the water as the mist rises, unhurt, her health scaled for the party.
 *
 * <p>Her rule: the light finds her. Her Likenesses ({@link HagLikenessEntity}) walk the fen with her, {@value #LIKENESSES}
 * at first: each looks as she does and mirrors her, but casts no shadow and leaves no ripple; anything striking one bursts
 * it into marsh gas, and another forms from the mist {@value #LIKENESS_RETURN} ticks later. Within {@value #LAMP_REACH}
 * blocks of a burning fen lamp her Likenesses gutter and fade, and she glows: her outline shows. A player lighting a lamp
 * that has gone out shows her for {@value #REVEAL_TICKS} ticks wherever she is, and bursts every Likeness in its light.
 * Phase 1, the Lure: the Clawing Swipe, Root Lanes, the Poison Brew and the Likeness Exchange. At {@value #DARK_AT} of
 * her health, Lights Out: she cackles, unhurt, two lamps gutter out, and a Root Knot ({@link RootKnotEntity}) crawls along
 * the ring toward a third. Phase 2, the Drowned Dark: up to {@value #DARK_LIKENESSES} Likenesses, and the Mire Grasp and
 * the Snuff besides. Below {@value #LAST_AT}, the Last Light: her lantern flares, her Likenesses burst and come no more,
 * her outline shows, and every cooldown and her pause between attacks are a third shorter.
 *
 * <p>Every attack has a wind-up you can read, a strike and a recovery ({@link Attack}); the numbers are here, in one place,
 * and in tools/mire_hag.py. Poison, drowning, knockback and falls never harm her, and she wades as if the water were dry
 * land. Each participant (whoever hurt her, her Likenesses or the Root Knot) gets their own loot ({@link MireHagLoot}).
 * Left alone she climbs back onto her porch, healed: her Likenesses burst, the lamps relight, her pools, roots and hands
 * are gone, and who hurt her is forgotten. Bound to a lair instance she vanishes with it; one with no instance (a test's)
 * keeps to the fen laid out from the origin she was given. The only blocks she changes are the fen's own lamps, and every
 * one burns again.
 */
public class MireHagEntity extends Monster implements GeoEntity {
	// ---------------------------------------------------------------- numbers (tools/mire_hag.py)
	public static final float HEALTH = 360.0F;
	public static final double ARMOR = 6.0;
	public static final double LEASH = 30.0;
	public static final double PARTY_STEP = 0.5;
	public static final double PARTY_MAX = 2.5;
	public static final int WAKE_TICKS = 40;
	public static final double WAKE_RADIUS = 18.0;
	public static final double WAKE_NEAR = 6.0;
	public static final int ABANDON_TICKS = 200;
	public static final int GLOBAL_COOLDOWN = 20;
	public static final double SPEED = 0.15;
	public static final double KEEP_DISTANCE = 2.5;
	public static final double STEP = 1.25;
	public static final float DARK_AT = 0.55F;
	public static final float LAST_AT = 0.2F;
	public static final double LAST_COOLDOWN = 0.6667;
	public static final int LIKENESSES = 2;
	public static final int DARK_LIKENESSES = 4;
	public static final double LIKENESS_SPREAD = 50.0;
	public static final int LIKENESS_RETURN = 200;
	public static final double LAMP_REACH = 8.0;
	public static final int REVEAL_TICKS = 100;
	public static final float SWIPE_DAMAGE = 8.0F;
	public static final double SWIPE_REACH = 3.0;
	public static final double SWIPE_HALF_ANGLE = 60.0;
	public static final int LANES = 3;
	public static final double LANE_LENGTH = 12.0;
	public static final double LANE_WIDTH = 0.75;
	public static final double LANE_SPREAD = 20.0;
	public static final float LANE_DAMAGE = 6.0F;
	public static final int ROOT_HOLD = 30;
	public static final double POOL_RADIUS = 1.5;
	public static final int POOL_TICKS = 120;
	public static final int POOL_EVERY = 10;
	public static final int POISON_TICKS = 60;
	public static final int MAX_POOLS = 6;
	public static final int DARK_TICKS = 60;
	public static final int DARK_SNUFFS = 2;
	public static final double GRASP_RADIUS = 1.5;
	public static final float GRASP_DAMAGE = 6.0F;
	public static final int GRASP_SLOW = 40;
	public static final int GRASP_SLOW_AMPLIFIER = 1;
	public static final int EXPERIENCE = 300;
	/** How far from the marsh's middle she (and her Likenesses) go: the marsh, short of its banks. */
	static final double ROAM = LanternFen.MARSH_RADIUS - 1.0;
	/** A root clump of her Root Lanes every this many blocks along each lane. */
	public static final double CLUMP_STEP = 1.5;
	/** How high her step down off the porch rises over the straight line. */
	private static final double STEP_DOWN_RISE = 1.5;
	/** Waking: ticks spent looking up on the porch before she steps down. */
	private static final int LOOK_UP = 15;
	private static final DustParticleOptions VENOM = new DustParticleOptions(0x6DBA27, 1.4F);
	private static final DustParticleOptions MARSH_GAS = new DustParticleOptions(0x9AB57A, 1.6F);

	/**
	 * Her attacks: wind-up, the active part and recovery in ticks, the cooldown, the target's nearest and farthest distance,
	 * and the phases she uses it in (the Lure; the Drowned Dark and the Last Light). The wind-up is what players read; the
	 * strike lands as it ends.
	 */
	public enum Attack {
		CLAWING_SWIPE(12, 4, 10, 30, 0.0, 3.5, true, true),
		ROOT_LANES(24, 30, 10, 140, 2.0, 12.0, true, true),
		POISON_BREW(16, 1, 12, 120, 3.0, 16.0, true, true),
		LIKENESS_EXCHANGE(40, 1, 10, 300, 0.0, 30.0, true, true),
		MIRE_GRASP(24, 20, 10, 160, 0.0, 16.0, false, true),
		SNUFF(30, 1, 10, 400, 0.0, 40.0, false, true);

		public final int windup;
		public final int active;
		public final int recovery;
		public final int cooldown;
		public final double near;
		public final double far;
		public final boolean lure;
		public final boolean drowned;

		Attack(int windup, int active, int recovery, int cooldown, double near, double far, boolean lure, boolean drowned) {
			this.windup = windup;
			this.active = active;
			this.recovery = recovery;
			this.cooldown = cooldown;
			this.near = near;
			this.far = far;
			this.lure = lure;
			this.drowned = drowned;
		}

		public int length() {
			return windup + active + recovery;
		}
	}

	/** The fight's phases. */
	public enum Phase {
		WAITING, WAKING, LURE, DARKENING, DROWNED, LAST
	}

	/** What her body is doing, synced for her animations (and mirrored by her Likenesses). */
	public enum Action {
		STIR, WAKE, IDLE, SWIPE_WINDUP, SWIPE, LANES_WINDUP, LANES, BREW_WINDUP, BREW, EXCHANGE_WINDUP, EXCHANGE, CACKLE, GRASP_WINDUP,
		GRASP, SNUFF_WINDUP, SNUFF
	}

	/** Her lantern's flame, synced: burning, flaring in the Last Light, or out. */
	public enum Lantern {
		LIT, FLARE, OUT
	}

	/** A pool of her venom where a brew shattered, and the game time it sinks into the water. */
	public record VenomPool(Vec3 at, long until) {
	}

	private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(MireHagEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> LANTERN = SynchedEntityData.defineId(MireHagEntity.class, EntityDataSerializers.INT);
	static final RawAnimation[] BODY = new RawAnimation[Action.values().length];
	static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.mire_hag.walk");
	static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.mire_hag.death");
	static final RawAnimation[] FLAME = {RawAnimation.begin().thenLoop("animation.mire_hag.lantern_lit"),
			RawAnimation.begin().thenLoop("animation.mire_hag.lantern_flare"), RawAnimation.begin().thenLoop("animation.mire_hag.lantern_out")};

	static {
		String prefix = "animation.mire_hag.";
		BODY[Action.STIR.ordinal()] = RawAnimation.begin().thenLoop(prefix + "stir");
		BODY[Action.WAKE.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "wake");
		BODY[Action.IDLE.ordinal()] = RawAnimation.begin().thenLoop(prefix + "idle");
		BODY[Action.SWIPE_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "swipe_windup");
		BODY[Action.SWIPE.ordinal()] = RawAnimation.begin().then(prefix + "swipe", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.LANES_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "lanes_windup");
		BODY[Action.LANES.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "lanes");
		BODY[Action.BREW_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "brew_windup");
		BODY[Action.BREW.ordinal()] = RawAnimation.begin().then(prefix + "brew", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.EXCHANGE_WINDUP.ordinal()] = RawAnimation.begin().thenLoop(prefix + "exchange_windup");
		BODY[Action.EXCHANGE.ordinal()] = RawAnimation.begin().then(prefix + "exchange", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.CACKLE.ordinal()] = RawAnimation.begin().thenLoop(prefix + "cackle");
		BODY[Action.GRASP_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "grasp_windup");
		BODY[Action.GRASP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "grasp");
		BODY[Action.SNUFF_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "snuff_windup");
		BODY[Action.SNUFF.ordinal()] = RawAnimation.begin().then(prefix + "snuff", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
	}

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private final ServerBossEvent bossBar = new ServerBossEvent(UUID.randomUUID(), Component.translatable("entity.jugcraft.mire_hag"),
			BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_10);
	/** The lair instance she belongs to (null for one put down by a test or a command), and the fen's origin. */
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
	/** Ticks left that lighting a lamp shows her. */
	private int revealTicks;
	/** Her last attack and whom it was on: Root Lanes never twice running on one player. */
	private @Nullable Attack lastAttack;
	private @Nullable UUID lastFoe;
	private final Map<UUID, Float> damageBy = new HashMap<>();
	/** Her Likenesses, by the slot each walks in round her foe, and the game times burst ones form again. */
	private final Map<UUID, Integer> likenesses = new HashMap<>();
	private final List<Long> returns = new ArrayList<>();
	private @Nullable UUID knot;
	/** Her roots, hands and brews in flight, gone with her. */
	private final List<UUID> things = new ArrayList<>();
	private final List<VenomPool> pools = new ArrayList<>();
	/** Where a Root Lanes runs from and its three ways; where a brew or a grasp lands; the lamp a Snuff puts out. */
	private Vec3 lanesFrom = Vec3.ZERO;
	private final List<Vec3> lanes = new ArrayList<>();
	private Vec3 mark = Vec3.ZERO;
	private @Nullable BlockPos snuffLamp;
	/** Stepping down off the porch: from and to. */
	private Vec3 stepFrom = Vec3.ZERO;
	/** Sidestepping round a stump or a post: ticks left, and which way. */
	private int sidestep;
	private boolean sidestepLeft;
	/** Set when she is loaded in the middle of a passing moment (waking, Lights Out): she lands and fights on. */
	private boolean landOnLoad;

	public MireHagEntity(EntityType<? extends MireHagEntity> type, Level level) {
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
				.add(Attributes.ATTACK_DAMAGE, SWIPE_DAMAGE).add(Attributes.STEP_HEIGHT, STEP);
	}

	/**
	 * Seats her on the porch of {@code instance}'s Lantern Fen. Called as the instance is placed. Placing the template puts
	 * back only its own blocks, so the Grey Mist a fallen Hag opened in this slot before still stands over the water: it goes
	 * first, and the water's layer is water again.
	 */
	public static MireHagEntity summon(ServerLevel level, LairInstance instance) {
		BlockPos origin = instance.origin();
		for (BlockPos cell : LanternFen.exitCells()) {
			BlockPos at = cell.offset(origin);
			if (level.getBlockState(at).is(JugcraftLairs.LAIR_EXIT)) {
				level.setBlock(at, cell.getY() == LanternFen.WATER ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(),
						Block.UPDATE_CLIENTS);
			}
		}
		return summon(level, origin, instance);
	}

	/** Seats her on the porch of the fen laid out from {@code origin} (tests pass no instance). */
	public static MireHagEntity summon(ServerLevel level, BlockPos origin, @Nullable LairInstance instance) {
		MireHagEntity hag = new MireHagEntity(JugcraftHag.MIRE_HAG, level);
		hag.origin = origin.immutable();
		if (instance != null) {
			hag.instance = instance.id;
			hag.slot = instance.slot;
		}
		hag.noPhysics = true;
		Vec3 waiting = hag.waiting();
		hag.snapTo(waiting.x, waiting.y, waiting.z, 0.0F, 0.0F);
		hag.face(0.0F);
		level.addFreshEntity(hag);
		return hag;
	}

	// ---------------------------------------------------------------- where things are

	/** The marsh's middle, where she wades, for the fen laid out from {@code origin}. */
	public static Vec3 centre(BlockPos origin) {
		return LanternFen.centre().add(origin.getX(), origin.getY(), origin.getZ());
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

	/** Where she waits on her porch. */
	public Vec3 waiting() {
		return at(LanternFen.waiting());
	}

	/** Where she comes down into the water as she wakes. */
	public Vec3 woke() {
		return at(LanternFen.woke());
	}

	/** The height of the water's surface over the marsh, where her roots and hands burst up. */
	public double surfaceY() {
		return origin.getY() + LanternFen.WATER + 0.85;
	}

	/** The fen's lamps, where they stand in the world. */
	public List<BlockPos> lamps() {
		return LanternFen.LAMPS.stream().map(lamp -> lamp.offset(origin)).toList();
	}

	/** Whether the lamp at {@code lamp} burns. */
	public static boolean burning(Level level, BlockPos lamp) {
		BlockState state = level.getBlockState(lamp);
		return state.getBlock() instanceof FenLampBlock && state.getValue(LairBrazierBlock.LIT);
	}

	/** Whether {@code at} is within {@value #LAMP_REACH} blocks (flat) of a burning lamp of her fen. */
	public boolean inLampLight(Level level, Vec3 at) {
		for (BlockPos lamp : lamps()) {
			if (burning(level, lamp) && Math.hypot(at.x - (lamp.getX() + 0.5), at.z - (lamp.getZ() + 0.5)) <= LAMP_REACH) {
				return true;
			}
		}
		return false;
	}

	/** Where her hand is as she lobs a brew, ahead of her and up (tools/mire_hag_models.py HAND_Z and HAND_Y). */
	public Vec3 hand() {
		return position().add(facing().scale(PoisonBrewEntity.HAND_AHEAD)).add(0.0, PoisonBrewEntity.HAND_UP, 0.0);
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

	public Action action() {
		int id = entityData.get(ACTION);
		return id >= 0 && id < Action.values().length ? Action.values()[id] : Action.IDLE;
	}

	private void setAction(Action action) {
		entityData.set(ACTION, action.ordinal());
	}

	public Lantern lantern() {
		int id = entityData.get(LANTERN);
		return id >= 0 && id < Lantern.values().length ? Lantern.values()[id] : Lantern.LIT;
	}

	private void setLantern(Lantern lantern) {
		entityData.set(LANTERN, lantern.ordinal());
	}

	public @Nullable UUID instanceId() {
		return instance;
	}

	/** Whether she is fighting (in the marsh, not waiting, waking or crying Lights Out). */
	public boolean fighting() {
		return phase == Phase.LURE || phase == Phase.DROWNED || phase == Phase.LAST;
	}

	/** Whether her outline shows: in a lamp's light, shown by a lamp lit again, or in the Last Light. */
	public boolean revealed() {
		return phase == Phase.LAST || revealTicks > 0 || inLampLight(level(), position());
	}

	public int revealTicks() {
		return revealTicks;
	}

	/** How many Likenesses walk with her at most in this phase. */
	public int likenessCap() {
		return switch (phase) {
			case LURE, WAKING -> LIKENESSES;
			case DARKENING, DROWNED -> DARK_LIKENESSES;
			default -> 0;
		};
	}

	public List<UUID> likenesses() {
		return List.copyOf(likenesses.keySet());
	}

	/** Whether the Likeness {@code id} walks with her. */
	public boolean walksWith(UUID id) {
		return likenesses.containsKey(id);
	}

	/** The game times burst Likenesses form again. */
	public List<Long> returns() {
		return List.copyOf(returns);
	}

	public @Nullable UUID knot() {
		return knot;
	}

	public List<VenomPool> pools() {
		return List.copyOf(pools);
	}

	/** Where a brew or a grasp will land. */
	public Vec3 mark() {
		return mark;
	}

	/** The three ways of a Root Lanes, flat and of unit length, from {@link #lanesFrom()}. */
	public List<Vec3> lanes() {
		return List.copyOf(lanes);
	}

	public Vec3 lanesFrom() {
		return lanesFrom;
	}

	/** Damage she deals: the attack's own, times {@code lairs.boss_damage} ({@link LairBosses}). */
	public static float damage(float base) {
		return LairBosses.damage(base);
	}

	/** Her health for a party of {@code players}: half as much again for each beyond the first, at most 2.5 times. */
	public static double partyScale(int players) {
		return LairBosses.partyScale(players, PARTY_STEP, PARTY_MAX);
	}

	/** A cooldown as it is in this phase: a third shorter in the Last Light. */
	public int cooldown(int ticks) {
		return phase == Phase.LAST ? (int) Math.round(ticks * LAST_COOLDOWN) : ticks;
	}

	/** Her pace. */
	public double speed() {
		return SPEED;
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
		if (revealTicks > 0) {
			revealTicks--;
		}
		phaseTicks++;
		venom(level);
		switch (phase) {
			case WAITING -> waiting(level);
			case WAKING -> waking(level);
			case DARKENING -> darkening(level);
			case LURE, DROWNED, LAST -> {
				light(level);
				reform(level);
				ripples(level);
				fight(level);
			}
		}
	}

	/** Whether her instance (if she has one) is still open. */
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
		setDeltaMovement(Vec3.ZERO);
		Vec3 waiting = waiting();
		if (position().distanceToSqr(waiting) > 0.01) {
			setPos(waiting.x, waiting.y, waiting.z);
		}
		face(0.0F);
		setAction(Action.STIR);
		if (tickCount % 10 == 0) {
			Vec3 cauldron = waiting.add(-2.0, 0.9, 0.5);
			level.sendParticles(ParticleTypes.BUBBLE_POP, cauldron.x, cauldron.y, cauldron.z, 2, 0.25, 0.05, 0.25, 0.0);
			if (intruder(level) != null) {
				wake(level);
			}
		}
	}

	/** Someone who may fight her, in the marsh (within {@value #WAKE_RADIUS} of its middle) or near her porch. */
	private @Nullable Player intruder(ServerLevel level) {
		Vec3 centre = centre();
		for (Player player : level.players()) {
			if (!LairBosses.eligible(player) || Math.abs(player.getY() - centre.y) > 6.0) {
				continue;
			}
			double flat = Math.hypot(player.getX() - centre.x, player.getZ() - centre.z);
			if (flat <= WAKE_RADIUS || player.position().distanceTo(position()) <= WAKE_NEAR) {
				return player;
			}
		}
		return null;
	}

	/** She looks up from her cauldron: her health scaled for the players near the marsh, her bar shown, the lamps guttering. */
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
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 3.0F, 0.6F);
		for (BlockPos lamp : lamps()) {
			level.sendParticles(ParticleTypes.SMOKE, lamp.getX() + 0.5, lamp.getY() + 0.8, lamp.getZ() + 0.5, 8, 0.2, 0.3, 0.2, 0.01);
		}
		Vec3 centre = centre();
		level.sendParticles(ParticleTypes.CLOUD, centre.x, centre.y + 0.4, centre.z, 60, 12.0, 0.2, 12.0, 0.01);
		tell(level, "message.jugcraft.mire_hag.wakes");
	}

	/** Waking: she looks up from the porch, then steps down into the water in an arc; two Likenesses form round her. */
	private void waking(ServerLevel level) {
		noPhysics = true;
		setDeltaMovement(Vec3.ZERO);
		if (phaseTicks <= LOOK_UP) {
			if (phaseTicks == LOOK_UP) {
				stepFrom = position();
			}
			return;
		}
		int glide = WAKE_TICKS - LOOK_UP;
		double share = Math.min(1.0, (phaseTicks - LOOK_UP) / (double) glide);
		Vec3 to = woke();
		Vec3 along = stepFrom.add(to.subtract(stepFrom).scale(share));
		setPos(along.x, along.y + STEP_DOWN_RISE * 4.0 * share * (1.0 - share), along.z);
		face(to.subtract(stepFrom));
		if (phaseTicks >= WAKE_TICKS) {
			teleportTo(to.x, to.y, to.z);
			noPhysics = false;
			level.sendParticles(ParticleTypes.SPLASH, getX(), surfaceY(), getZ(), 30, 0.8, 0.1, 0.8, 0.1);
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_SPLASH, SoundSource.HOSTILE, 1.5F, 0.7F);
			phase = Phase.LURE;
			phaseTicks = 0;
			globalCooldown = GLOBAL_COOLDOWN;
			setAction(Action.IDLE);
			for (int i = 0; i < LIKENESSES; i++) {
				returns.add(level.getGameTime());
			}
			reform(level);
		}
	}

	/** Players near enough to her marsh to be fighting (or about to). */
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

	// ---------------------------------------------------------------- her rule: the light finds her

	/**
	 * The light: in a lamp's light, or shown, her outline shows (Glowing, refreshed while it lasts); every Likeness in a
	 * lamp's light gutters and fades.
	 */
	private void light(ServerLevel level) {
		if (tickCount % 5 != 0) {
			return;
		}
		if (revealed()) {
			addEffect(new MobEffectInstance(MobEffects.GLOWING, 10, 0, false, false));
		}
		for (UUID id : List.copyOf(likenesses.keySet())) {
			if (level.getEntity(id) instanceof HagLikenessEntity likeness && inLampLight(level, likeness.position())) {
				burst(level, likeness);
			}
		}
	}

	/**
	 * A player lit a lamp that had gone out: she is shown for {@value #REVEAL_TICKS} ticks wherever she is, and every
	 * Likeness in that lamp's light bursts. Returns how many burst.
	 */
	public int lampLit(ServerLevel level, BlockPos lamp, @Nullable Player player) {
		if (!fighting() && phase != Phase.DARKENING) {
			return 0;
		}
		revealTicks = REVEAL_TICKS;
		addEffect(new MobEffectInstance(MobEffects.GLOWING, 10, 0, false, false));
		int burst = 0;
		for (UUID id : List.copyOf(likenesses.keySet())) {
			if (level.getEntity(id) instanceof HagLikenessEntity likeness
					&& Math.hypot(likeness.getX() - (lamp.getX() + 0.5), likeness.getZ() - (lamp.getZ() + 0.5)) <= LAMP_REACH) {
				burst(level, likeness);
				burst++;
			}
		}
		if (player != null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.mire_hag.revealed"));
		}
		return burst;
	}

	/** A Likeness bursts into marsh gas (struck, faded in the light, or exchanged away); another forms later. */
	public void burst(ServerLevel level, HagLikenessEntity likeness) {
		if (likenesses.remove(likeness.getUUID()) != null && phase != Phase.LAST) {
			returns.add(level.getGameTime() + LIKENESS_RETURN);
		}
		gas(level, likeness.position());
		likeness.discard();
	}

	/** A puff of marsh gas at {@code at}. */
	static void gas(ServerLevel level, Vec3 at) {
		level.sendParticles(MARSH_GAS, at.x, at.y + 1.0, at.z, 20, 0.4, 0.6, 0.4, 0.0);
		level.sendParticles(ParticleTypes.POOF, at.x, at.y + 1.0, at.z, 8, 0.3, 0.5, 0.3, 0.02);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.HOSTILE, 1.2F, 0.6F);
	}

	/** Burst Likenesses form again when their time comes, as many as this phase allows, out of the lamps' light. */
	private void reform(ServerLevel level) {
		int cap = likenessCap();
		while (likenesses.size() + returns.size() < cap) {
			returns.add(level.getGameTime());
		}
		if (returns.isEmpty() || tickCount % 5 != 0) {
			return;
		}
		long now = level.getGameTime();
		returns.sort(Long::compare);
		while (!returns.isEmpty() && returns.getFirst() <= now && likenesses.size() < cap) {
			int slot = freeSlot();
			Vec3 at = likenessSpot(level, slot);
			if (at == null) {
				returns.set(0, now + 20);
				break;
			}
			returns.removeFirst();
			HagLikenessEntity likeness = HagLikenessEntity.form(level, this, at, slot);
			likenesses.put(likeness.getUUID(), slot);
		}
		while (returns.size() + likenesses.size() > cap && !returns.isEmpty()) {
			returns.removeLast();
		}
	}

	private int freeSlot() {
		for (int slot = 1; slot <= DARK_LIKENESSES; slot++) {
			if (!likenesses.containsValue(slot)) {
				return slot;
			}
		}
		return 1;
	}

	/** The angle (degrees) round her foe that a Likeness in {@code slot} keeps from her: one side, the other, then wider. */
	public static double slotAngle(int slot) {
		return LIKENESS_SPREAD * ((slot + 1) / 2) * (slot % 2 == 1 ? 1.0 : -1.0);
	}

	/**
	 * Where a Likeness in {@code slot} walks: round her foe from her, turned by its slot's angle, at her distance; or round
	 * her if she has no foe. Out of the lamps' light and within the marsh, or null.
	 */
	public @Nullable Vec3 likenessSpot(ServerLevel level, int slot) {
		Player target = foe == null ? null : level.getPlayerByUUID(foe);
		Vec3 pivot = target != null ? target.position() : position();
		Vec3 offset = position().subtract(pivot);
		double flat = Math.hypot(offset.x, offset.z);
		if (flat < 2.0) {
			offset = facing().scale(-4.0);
			flat = 4.0;
		}
		for (int tries = 0; tries < 6; tries++) {
			double angle = Math.toRadians(slotAngle(slot) + tries * 25.0 * (slot % 2 == 1 ? 1 : -1));
			Vec3 way = rotate(new Vec3(offset.x / flat, 0.0, offset.z / flat), angle);
			Vec3 spot = new Vec3(pivot.x + way.x * flat, centre().y, pivot.z + way.z * flat);
			if (LanternFen.fromCentre(spot.x - origin.getX(), spot.z - origin.getZ()) <= ROAM && !inLampLight(level, spot)) {
				return spot;
			}
		}
		return null;
	}

	/** The water rings round her feet as she wades (her Likenesses leave none). */
	private void ripples(ServerLevel level) {
		if (tickCount % 4 == 0 && isInWater()) {
			ring(level, new Vec3(getX(), surfaceY() + 0.1, getZ()), 0.8 + 0.3 * ((tickCount / 4) % 3), ParticleTypes.SPLASH, 10);
		}
	}

	// ---------------------------------------------------------------- fighting

	private void fight(ServerLevel level) {
		keepToTheFen(level);
		Player target = target(level);
		if (target == null) {
			attack = null;
			setAction(Action.IDLE);
			move(centre(), speed());
			if (++abandoned >= ABANDON_TICKS) {
				reset(level);
			}
			return;
		}
		abandoned = 0;
		if (attack == null && phase == Phase.LURE && getHealth() <= getMaxHealth() * DARK_AT) {
			lightsOut(level, target);
			return;
		}
		if (attack == null && phase == Phase.DROWNED && getHealth() <= getMaxHealth() * LAST_AT) {
			lastLight(level);
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

	/** Strayed farther than her leash from the marsh's middle, or fallen below the mud, she glides back to it. */
	private void keepToTheFen(ServerLevel level) {
		Vec3 centre = centre();
		double dx = getX() - centre.x;
		double dz = getZ() - centre.z;
		if (dx * dx + dz * dz <= LEASH * LEASH && getY() > centre.y - 3.0) {
			return;
		}
		level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1.0, getZ(), 20, 0.6, 0.8, 0.6, 0.02);
		attack = null;
		noPhysics = false;
		teleportTo(centre.x, centre.y, centre.z);
		level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1.0, getZ(), 20, 0.6, 0.8, 0.6, 0.02);
	}

	/** Her target: the one she has while they are still in reach of her marsh, else the nearest who may fight her. */
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

	/** Her foe now, if she has one. */
	public @Nullable Player foe(ServerLevel level) {
		return foe == null ? null : level.getPlayerByUUID(foe);
	}

	/** Wades toward a point {@value #KEEP_DISTANCE} blocks short of {@code target}. */
	private void approach(Player target) {
		Vec3 to = target.position().subtract(position());
		double flat = Math.sqrt(to.x * to.x + to.z * to.z);
		Vec3 goal = flat > KEEP_DISTANCE + 0.5
				? new Vec3(target.getX() - to.x / flat * KEEP_DISTANCE, getY(), target.getZ() - to.z / flat * KEEP_DISTANCE)
				: position();
		move(goal, speed());
	}

	/** Wades toward {@code goal} at {@code speed}, kept in the marsh; round a stump or a post, she steps aside a moment. */
	private void move(Vec3 goal, double speed) {
		Vec3 step = wade(this, goal, speed, centre(), ROAM);
		if (horizontalCollision && step.lengthSqr() > 1.0E-4 && sidestep == 0) {
			sidestep = 10;
			sidestepLeft = getRandom().nextBoolean();
		}
		if (sidestep > 0) {
			sidestep--;
			step = rotate(step, Math.toRadians(sidestepLeft ? 70.0 : -70.0));
		}
		setDeltaMovement(step.x, -0.3, step.z);
	}

	/**
	 * The step a wader takes toward {@code goal} at {@code speed}, the goal kept within {@code roam} of {@code centre}:
	 * hers and her Likenesses'. They wade as if the water were dry land.
	 */
	static Vec3 wade(Entity wader, Vec3 goal, double speed, Vec3 centre, double roam) {
		double x = goal.x;
		double z = goal.z;
		double dx = x - centre.x;
		double dz = z - centre.z;
		double flat = Math.sqrt(dx * dx + dz * dz);
		if (flat > roam) {
			x = centre.x + dx / flat * roam;
			z = centre.z + dz / flat * roam;
		}
		Vec3 to = new Vec3(x - wader.getX(), 0.0, z - wader.getZ());
		double length = to.length();
		return length < 0.05 ? Vec3.ZERO : to.scale(Math.min(speed, length) / length);
	}

	/** Holds still where she is, pressed down onto what she stands on. */
	private void hold() {
		setDeltaMovement(0.0, -0.3, 0.0);
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

	/** The way she faces, flat. */
	public Vec3 facing() {
		double yaw = Math.toRadians(getYRot());
		return new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
	}

	/** Picks an attack she can use now on {@code target}, or null. */
	private @Nullable Attack choose(ServerLevel level, Player target) {
		double distance = target.distanceTo(this);
		List<Attack> ready = new ArrayList<>();
		for (Attack candidate : Attack.values()) {
			boolean phaseAllows = phase == Phase.LURE ? candidate.lure : candidate.drowned;
			if (!phaseAllows || cooldowns[candidate.ordinal()] > 0 || distance < candidate.near || distance > candidate.far) {
				continue;
			}
			if (candidate == Attack.ROOT_LANES && lastAttack == Attack.ROOT_LANES && target.getUUID().equals(lastFoe)) {
				continue;  // never twice running on one player
			}
			if (candidate == Attack.LIKENESS_EXCHANGE && (phase == Phase.LAST || likenesses.isEmpty() && returns.isEmpty())) {
				continue;
			}
			if (candidate == Attack.SNUFF && nearestLitLamp(level, position()) == null) {
				continue;
			}
			ready.add(candidate);
			if (candidate == Attack.CLAWING_SWIPE) {
				ready.add(candidate);  // close in, her claws come most often
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
		lastAttack = next;
		lastFoe = target.getUUID();
		cooldowns[next.ordinal()] = cooldown(next.cooldown);
		hold();
		face(target);
		switch (next) {
			case CLAWING_SWIPE -> {
				setAction(Action.SWIPE_WINDUP);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.WITCH_AMBIENT, SoundSource.HOSTILE, 1.5F, 0.7F);
			}
			case ROOT_LANES -> {
				setAction(Action.LANES_WINDUP);
				lanesFrom = new Vec3(getX(), surfaceY(), getZ());
				lanes.clear();
				Vec3 to = target.position().subtract(position());
				double flat = Math.hypot(to.x, to.z);
				Vec3 way = flat < 1.0E-3 ? facing() : new Vec3(to.x / flat, 0.0, to.z / flat);
				for (int i = 0; i < LANES; i++) {
					lanes.add(rotate(way, Math.toRadians(LANE_SPREAD * (i - (LANES - 1) / 2.0))));
				}
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 2.0F, 0.5F);
			}
			case POISON_BREW -> {
				setAction(Action.BREW_WINDUP);
				mark = new Vec3(target.getX(), target.getY(), target.getZ());
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.BOTTLE_FILL, SoundSource.HOSTILE, 1.5F, 0.6F);
			}
			case LIKENESS_EXCHANGE -> {
				setAction(Action.EXCHANGE_WINDUP);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 2.0F, 0.9F);
			}
			case MIRE_GRASP -> {
				setAction(Action.GRASP_WINDUP);
				mark = new Vec3(target.getX(), surfaceY(), target.getZ());
				level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.MUD_PLACE, SoundSource.HOSTILE, 2.0F, 0.5F);
			}
			case SNUFF -> {
				setAction(Action.SNUFF_WINDUP);
				snuffLamp = nearestLitLamp(level, position());
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.WITCH_AMBIENT, SoundSource.HOSTILE, 1.5F, 0.5F);
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
			case CLAWING_SWIPE -> {
				if (t < current.windup) {
					if (t % 4 == 0) {
						Vec3 claws = position().add(facing().scale(1.2)).add(0.0, 1.6, 0.0);
						level.sendParticles(ParticleTypes.WITCH, claws.x, claws.y, claws.z, 2, 0.3, 0.3, 0.3, 0.0);
					}
				} else if (t == current.windup) {
					setAction(Action.SWIPE);
					swipe(level);
				}
			}
			case ROOT_LANES -> rootLanes(level, t);
			case POISON_BREW -> {
				if (t < current.windup) {
					face(target);
					if (t % 4 == 0) {
						ring(level, mark, POOL_RADIUS, VENOM, 10);
					}
				} else if (t == current.windup) {
					setAction(Action.BREW);
					things.add(PoisonBrewEntity.lob(level, this, mark).getUUID());
					level.playSound(null, getX(), getY(), getZ(), SoundEvents.WITCH_THROW, SoundSource.HOSTILE, 1.5F, 0.6F);
				}
			}
			case LIKENESS_EXCHANGE -> {
				if (t < current.windup) {
					if (t % 4 == 0) {
						shimmer(level, position());
						for (UUID id : likenesses.keySet()) {
							if (level.getEntity(id) instanceof HagLikenessEntity likeness) {
								shimmer(level, likeness.position());
							}
						}
					}
				} else if (t == current.windup) {
					setAction(Action.EXCHANGE);
					exchange(level);
				}
			}
			case MIRE_GRASP -> {
				if (t < current.windup) {
					if (t % 3 == 0) {
						ring(level, mark, GRASP_RADIUS, ParticleTypes.SPLASH, 14);
						ring(level, mark, GRASP_RADIUS * 0.6, ParticleTypes.BUBBLE_POP, 8);
					}
				} else if (t == current.windup) {
					setAction(Action.GRASP);
					things.add(MireGraspEntity.rise(level, this, mark, current.active).getUUID());
					grasp(level);
				}
			}
			case SNUFF -> {
				if (t < current.windup) {
					if (snuffLamp != null && t % 3 == 0) {
						level.sendParticles(ParticleTypes.SMOKE, snuffLamp.getX() + 0.5, snuffLamp.getY() + 0.9, snuffLamp.getZ() + 0.5, 3, 0.15, 0.2,
								0.15, 0.01);
					}
				} else if (t == current.windup) {
					setAction(Action.SNUFF);
					if (snuffLamp != null) {
						FenLampBlock.snuff(level, snuffLamp);
					}
				}
			}
		}
		if (attack == current && attackTicks >= current.length()) {
			attack = null;
			attackTicks = 0;
			globalCooldown = cooldown(GLOBAL_COOLDOWN);
			Action action = action();
			if (action == Action.LANES || action == Action.GRASP || action.name().endsWith("_WINDUP")) {
				setAction(Action.IDLE);
			}
		}
	}

	/**
	 * Clawing Swipe: every player within {@value #SWIPE_REACH} blocks before her (within {@value #SWIPE_HALF_ANGLE}
	 * degrees either side of her face) takes {@value #SWIPE_DAMAGE} damage.
	 */
	public int swipe(ServerLevel level) {
		int hit = 0;
		Vec3 facing = facing();
		for (ServerPlayer player : nearby(level)) {
			Vec3 to = player.position().subtract(position());
			double flat = Math.sqrt(to.x * to.x + to.z * to.z);
			if (flat > SWIPE_REACH + getBbWidth() / 2.0 + player.getBbWidth() / 2.0 || Math.abs(to.y) > 2.5) {
				continue;
			}
			double cos = flat < 1.0E-3 ? 1.0 : (to.x * facing.x + to.z * facing.z) / flat;
			if (cos < Math.cos(Math.toRadians(SWIPE_HALF_ANGLE))) {
				continue;  // behind her or to her side
			}
			if (player.hurtServer(level, damageSources().mobAttack(this), damage(SWIPE_DAMAGE))) {
				hit++;
			}
		}
		Vec3 front = position().add(facing.scale(1.6)).add(0.0, 1.2, 0.0);
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, front.x, front.y, front.z, 2, 0.6, 0.2, 0.6, 0.0);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5F, 0.6F);
		return hit;
	}

	/**
	 * Root Lanes, {@code t} ticks into it: through the wind-up, lines of bubbles race out along her three lanes from where
	 * she plunged her staff; as it ends the roots burst up along them all ({@link #roots}), clumps every
	 * {@value #CLUMP_STEP} blocks, and stand through the active ticks.
	 */
	private void rootLanes(ServerLevel level, int t) {
		Attack lanesAttack = Attack.ROOT_LANES;
		if (t < lanesAttack.windup) {
			double reach = LANE_LENGTH * (t + 1) / lanesAttack.windup;
			if (t % 2 == 0) {
				for (Vec3 way : lanes) {
					Vec3 at = lanesFrom.add(way.scale(reach));
					level.sendParticles(ParticleTypes.BUBBLE_POP, at.x, at.y + 0.1, at.z, 3, 0.2, 0.0, 0.2, 0.0);
					level.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.1, at.z, 2, 0.2, 0.0, 0.2, 0.0);
				}
			}
			return;
		}
		if (t == lanesAttack.windup) {
			setAction(Action.LANES);
			for (Vec3 way : lanes) {
				for (double along = CLUMP_STEP; along <= LANE_LENGTH + 1.0E-6; along += CLUMP_STEP) {
					Vec3 at = lanesFrom.add(way.scale(along));
					things.add(HagRootsEntity.burst(level, this, at, lanesAttack.active).getUUID());
				}
			}
			roots(level);
		}
	}

	/**
	 * The roots burst along every lane: each player within {@value #LANE_WIDTH} blocks of a lane (flat, beyond their own
	 * half-width) and not high over the water takes {@value #LANE_DAMAGE} damage and is held fast for
	 * {@value #ROOT_HOLD} ticks.
	 */
	public int roots(ServerLevel level) {
		int hit = 0;
		for (ServerPlayer player : nearby(level)) {
			if (player.getY() > surfaceY() + 2.5 || player.getY() < surfaceY() - 2.0) {
				continue;
			}
			for (Vec3 way : lanes) {
				if (onLane(player.position(), way) <= LANE_WIDTH + player.getBbWidth() / 2.0) {
					if (player.hurtServer(level, damageSources().mobAttack(this), damage(LANE_DAMAGE))) {
						hit++;
					}
					HagRootsEntity.hold(player, ROOT_HOLD);
					break;
				}
			}
		}
		level.playSound(null, lanesFrom.x, lanesFrom.y, lanesFrom.z, SoundEvents.ROOTED_DIRT_PLACE, SoundSource.HOSTILE, 3.0F, 0.5F);
		return hit;
	}

	/** How far (flat) {@code at} is from the lane running {@value #LANE_LENGTH} blocks along {@code way} from where she plunged her staff. */
	public double onLane(Vec3 at, Vec3 way) {
		double dx = at.x - lanesFrom.x;
		double dz = at.z - lanesFrom.z;
		double along = Math.clamp(dx * way.x + dz * way.z, 0.0, LANE_LENGTH);
		return Math.hypot(dx - way.x * along, dz - way.z * along);
	}

	/** Sets a Root Lanes up from {@code from} toward {@code toward}, as its wind-up would (for tests). */
	public void lanesFrom(Vec3 from, Vec3 toward) {
		lanesFrom = from;
		lanes.clear();
		Vec3 to = toward.subtract(from);
		double flat = Math.hypot(to.x, to.z);
		Vec3 way = flat < 1.0E-3 ? facing() : new Vec3(to.x / flat, 0.0, to.z / flat);
		for (int i = 0; i < LANES; i++) {
			lanes.add(rotate(way, Math.toRadians(LANE_SPREAD * (i - (LANES - 1) / 2.0))));
		}
	}

	/** Mire Grasp: everyone within {@value #GRASP_RADIUS} blocks of its mark takes {@value #GRASP_DAMAGE} damage and is slowed. */
	public int grasp(ServerLevel level) {
		int hit = 0;
		for (ServerPlayer player : nearby(level)) {
			double flat = Math.hypot(player.getX() - mark.x, player.getZ() - mark.z);
			if (flat > GRASP_RADIUS + player.getBbWidth() / 2.0 || Math.abs(player.getY() - mark.y) > 2.5) {
				continue;
			}
			if (player.hurtServer(level, damageSources().mobAttack(this), damage(GRASP_DAMAGE))) {
				hit++;
			}
			player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, GRASP_SLOW, GRASP_SLOW_AMPLIFIER), this);
		}
		level.playSound(null, mark.x, mark.y, mark.z, SoundEvents.MUD_PLACE, SoundSource.HOSTILE, 3.0F, 0.4F);
		return hit;
	}

	/** Sets where a brew or a grasp lands (for tests). */
	public void mark(Vec3 at) {
		mark = at;
	}

	/**
	 * Likeness Exchange: she trades places with one of her Likenesses (if any walks with her) in a burst of mist, and every
	 * Likeness burst comes back now. Returns whether she moved.
	 */
	public boolean exchange(ServerLevel level) {
		List<HagLikenessEntity> walking = new ArrayList<>();
		for (UUID id : likenesses.keySet()) {
			if (level.getEntity(id) instanceof HagLikenessEntity likeness) {
				walking.add(likeness);
			}
		}
		boolean moved = false;
		if (!walking.isEmpty()) {
			HagLikenessEntity other = walking.get(getRandom().nextInt(walking.size()));
			Vec3 mine = position();
			Vec3 theirs = other.position();
			level.sendParticles(ParticleTypes.CLOUD, mine.x, mine.y + 1.0, mine.z, 30, 0.6, 0.8, 0.6, 0.03);
			level.sendParticles(ParticleTypes.CLOUD, theirs.x, theirs.y + 1.0, theirs.z, 30, 0.6, 0.8, 0.6, 0.03);
			teleportTo(theirs.x, theirs.y, theirs.z);
			other.teleportTo(mine.x, mine.y, mine.z);
			moved = true;
		}
		long now = level.getGameTime();
		returns.replaceAll(time -> Math.min(time, now));
		reform(level);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 2.0F, 0.8F);
		return moved;
	}

	private static void shimmer(ServerLevel level, Vec3 at) {
		level.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 1.0, at.z, 4, 0.4, 0.7, 0.4, 0.01);
		level.sendParticles(ParticleTypes.GLOW, at.x, at.y + 1.4, at.z, 1, 0.3, 0.5, 0.3, 0.0);
	}

	/** The lit lamp of her fen nearest {@code at}, or null when none burns. */
	public @Nullable BlockPos nearestLitLamp(Level level, Vec3 at) {
		BlockPos nearest = null;
		double best = Double.MAX_VALUE;
		for (BlockPos lamp : lamps()) {
			double distance = Math.hypot(at.x - (lamp.getX() + 0.5), at.z - (lamp.getZ() + 0.5));
			if (burning(level, lamp) && distance < best) {
				best = distance;
				nearest = lamp;
			}
		}
		return nearest;
	}

	// ---------------------------------------------------------------- her brews' pools

	/** A pool of venom spreads at {@code at} for {@value #POOL_TICKS} ticks; the oldest sinks past {@value #MAX_POOLS}. */
	public void venomPool(ServerLevel level, Vec3 at) {
		if (pools.size() >= MAX_POOLS) {
			pools.removeFirst();
		}
		pools.add(new VenomPool(at, level.getGameTime() + POOL_TICKS));
	}

	/** The pools: venom on each, and whoever stands in one is poisoned. Spent pools sink. */
	private void venom(ServerLevel level) {
		long now = level.getGameTime();
		pools.removeIf(pool -> now >= pool.until());
		if (pools.isEmpty()) {
			return;
		}
		if (tickCount % 3 == 0) {
			for (VenomPool pool : pools) {
				level.sendParticles(VENOM, pool.at().x, pool.at().y + 0.1, pool.at().z, 4, POOL_RADIUS * 0.5, 0.02, POOL_RADIUS * 0.5, 0.0);
			}
		}
		if (tickCount % POOL_EVERY == 0) {
			poison(level);
		}
	}

	/** Everyone standing in a pool is poisoned for {@value #POISON_TICKS} ticks (Poison I, which never kills). */
	public int poison(ServerLevel level) {
		int poisoned = 0;
		for (ServerPlayer player : nearby(level)) {
			for (VenomPool pool : pools) {
				double reach = POOL_RADIUS + player.getBbWidth() / 2.0;
				if (Math.hypot(player.getX() - pool.at().x, player.getZ() - pool.at().z) <= reach
						&& Math.abs(player.getY() - pool.at().y) < 1.5) {
					player.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_TICKS, 0), this);
					poisoned++;
					break;
				}
			}
		}
		return poisoned;
	}

	// ---------------------------------------------------------------- Lights Out, and the Last Light

	/**
	 * Lights Out: she cackles for {@value #DARK_TICKS} ticks, unhurt; the {@value #DARK_SNUFFS} lit lamps nearest her foe
	 * gutter out, and a Root Knot bursts from the water on the ring and crawls toward the nearest lamp still burning.
	 */
	public void lightsOut(ServerLevel level, Player target) {
		attack = null;
		phase = Phase.DARKENING;
		phaseTicks = 0;
		setAction(Action.CACKLE);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 4.0F, 0.5F);
		tell(level, "message.jugcraft.mire_hag.lights_out");
		for (int i = 0; i < DARK_SNUFFS; i++) {
			BlockPos lamp = nearestLitLamp(level, target.position());
			if (lamp != null) {
				FenLampBlock.snuff(level, lamp);
			}
		}
		BlockPos third = nearestLitLamp(level, target.position());
		if (third != null && knot == null) {
			knot = RootKnotEntity.burst(level, this, third, getRandom().nextBoolean()).getUUID();
			tell(level, "message.jugcraft.mire_hag.knot");
		}
	}

	/** Lights Out holds: she cackles, unhurt, and then the Drowned Dark begins. */
	private void darkening(ServerLevel level) {
		hold();
		if (phaseTicks % 6 == 0) {
			level.sendParticles(ParticleTypes.WITCH, getX(), getY() + 2.0, getZ(), 4, 0.5, 0.4, 0.5, 0.0);
		}
		if (phaseTicks >= DARK_TICKS) {
			phase = Phase.DROWNED;
			phaseTicks = 0;
			globalCooldown = GLOBAL_COOLDOWN;
			setAction(Action.IDLE);
			reform(level);
		}
	}

	/** The Last Light: her lantern flares, every Likeness bursts and none comes back, her outline shows. */
	public void lastLight(ServerLevel level) {
		attack = null;
		phase = Phase.LAST;
		phaseTicks = 0;
		setLantern(Lantern.FLARE);
		returns.clear();
		for (UUID id : List.copyOf(likenesses.keySet())) {
			if (level.getEntity(id) instanceof HagLikenessEntity likeness) {
				burst(level, likeness);
			}
		}
		likenesses.clear();
		globalCooldown = cooldown(GLOBAL_COOLDOWN);
		setAction(Action.IDLE);
		level.sendParticles(ParticleTypes.GLOW, getX(), getY() + 2.4, getZ(), 30, 0.6, 0.6, 0.6, 0.05);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 3.0F, 0.4F);
		tell(level, "message.jugcraft.mire_hag.last_light");
	}

	// ---------------------------------------------------------------- her things

	/** Forgets what is gone. */
	private void prune(ServerLevel level) {
		likenesses.keySet().removeIf(id -> !(level.getEntity(id) instanceof HagLikenessEntity likeness) || !likeness.isAlive());
		things.removeIf(id -> level.getEntity(id) == null);
		if (knot != null && !(level.getEntity(knot) instanceof RootKnotEntity found && found.isAlive())) {
			knot = null;
		}
	}

	/** Whoever hurt her, her Likenesses or the Root Knot has taken part. */
	public void took(ServerPlayer player, float amount) {
		damageBy.merge(player.getUUID(), amount, Float::sum);
	}

	/** Her participants: everyone who hurt her, her Likenesses or the Root Knot and is still near her marsh. */
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
	 * Left alone: she climbs back onto her porch, healed; her Likenesses burst, the lamps relight, her pools, roots, hands and
	 * knot are gone, and who hurt her is forgotten.
	 */
	public void reset(ServerLevel level) {
		dismiss(level);
		for (BlockPos lamp : lamps()) {
			if (level.isLoaded(lamp)) {
				FenLampBlock.light(level, lamp);
			}
		}
		phase = Phase.WAITING;
		phaseTicks = 0;
		attack = null;
		abandoned = 0;
		foe = null;
		lastAttack = null;
		lastFoe = null;
		revealTicks = 0;
		damageBy.clear();
		Arrays.fill(cooldowns, 0);
		setHealth(getMaxHealth());
		removeEffect(MobEffects.GLOWING);
		setLantern(Lantern.LIT);
		noPhysics = true;
		Vec3 waiting = waiting();
		teleportTo(waiting.x, waiting.y, waiting.z);
		face(0.0F);
		setAction(Action.STIR);
		bossBar.setVisible(false);
		tell(level, "message.jugcraft.mire_hag.reset");
	}

	/** Her Likenesses burst, her roots, hands, brews and knot are gone, and her pools sink. */
	private void dismiss(ServerLevel level) {
		for (UUID id : List.copyOf(likenesses.keySet())) {
			if (level.getEntity(id) instanceof HagLikenessEntity likeness) {
				gas(level, likeness.position());
				likeness.discard();
			}
		}
		likenesses.clear();
		returns.clear();
		for (UUID id : List.copyOf(things)) {
			Entity thing = level.getEntity(id);
			if (thing != null) {
				thing.discard();
			}
		}
		things.clear();
		if (knot != null && level.getEntity(knot) instanceof RootKnotEntity found) {
			found.sink(level, false);
		}
		knot = null;
		pools.clear();
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level) {
			defeated(level);
		}
	}

	/**
	 * She has fallen: her Likenesses burst and her things are gone, each participant gets their loot, her lair's instance
	 * ends (it closes once everyone has left), her lantern goes out, the mist lifts off the marsh, and a gate of Grey Mist
	 * opens in its middle.
	 */
	private void defeated(ServerLevel level) {
		dismiss(level);
		MireHagLoot.reward(level, this, participants(level));
		setLantern(Lantern.OUT);
		Vec3 centre = centre();
		level.sendParticles(ParticleTypes.CLOUD, centre.x, centre.y + 1.0, centre.z, 80, 14.0, 0.4, 14.0, 0.04);
		LairInstance open = lairInstance(level);
		if (open != null) {
			Lairs.end(open);
			BlockState mist = JugcraftLairs.LAIR_EXIT.defaultBlockState().setValue(LairExitBlock.AXIS, Direction.Axis.X);
			for (BlockPos cell : LanternFen.exitCells()) {
				level.setBlock(cell.offset(origin), mist, Block.UPDATE_ALL);
			}
		}
		bossBar.setVisible(false);
		tell(level, "message.jugcraft.mire_hag.defeated");
	}

	/** Tells everyone near her marsh. */
	void tell(ServerLevel level, String key) {
		Vec3 centre = centre();
		for (ServerPlayer player : level.players()) {
			if (player.position().distanceTo(centre) <= LEASH + 32.0) {
				player.sendSystemMessage(Component.translatable(key));
			}
		}
	}

	// ---------------------------------------------------------------- what she is immune to

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
		if (phase == Phase.WAKING || phase == Phase.DARKENING) {
			return false;
		}
		boolean hurt = super.hurtServer(level, source, amount);
		if (hurt && source.getEntity() instanceof ServerPlayer player) {
			took(player, amount);
		}
		return hurt;
	}

	@Override
	public boolean canBeAffected(MobEffectInstance effect) {
		return !effect.is(MobEffects.POISON) && super.canBeAffected(effect);
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
		return phase == Phase.WAITING ? null : SoundEvents.WITCH_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.WITCH_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.WITCH_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.6F;
	}

	// ---------------------------------------------------------------- particles and ways

	static void ring(ServerLevel level, Vec3 centre, double radius, ParticleOptions particle, int points) {
		for (int i = 0; i < points; i++) {
			double angle = Math.PI * 2.0 * i / points;
			level.sendParticles(particle, centre.x + Math.cos(angle) * radius, centre.y + 0.1, centre.z + Math.sin(angle) * radius, 1, 0.0, 0.02,
					0.0, 0.0);
		}
	}

	static Vec3 rotate(Vec3 way, double radians) {
		double cos = Math.cos(radians);
		double sin = Math.sin(radians);
		return new Vec3(way.x * cos - way.z * sin, 0.0, way.x * sin + way.z * cos);
	}

	// ---------------------------------------------------------------- saving and syncing

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ACTION, Action.STIR.ordinal());
		builder.define(LANTERN, Lantern.LIT.ordinal());
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
		origin = input.read("origin", BlockPos.CODEC).orElse(BlockPos.containing(getX() - LanternFen.WAIT_X, getY() - LanternFen.WAIT_Y,
				getZ() - LanternFen.WAIT_Z));
		try {
			phase = Phase.valueOf(input.getStringOr("phase", Phase.WAITING.name()));
		} catch (IllegalArgumentException e) {
			phase = Phase.WAITING;
		}
		if (phase == Phase.WAKING || phase == Phase.DARKENING) {
			// A passing moment is not resumed: she comes down into the water, and fights on.
			landOnLoad = phase == Phase.WAKING;
			phase = phase == Phase.WAKING ? Phase.LURE : Phase.DROWNED;
		}
		noPhysics = phase == Phase.WAITING;
		setLantern(phase == Phase.LAST ? Lantern.FLARE : Lantern.LIT);
		bossBar.setVisible(phase != Phase.WAITING);
	}

	// ---------------------------------------------------------------- her animations

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<MireHagEntity>("body", 4, test -> {
			MireHagEntity hag = test.animatable();
			if (hag.isDeadOrDying()) {
				return test.setAndContinue(DEATH);
			}
			Action action = hag.action();
			return test.setAndContinue(action == Action.IDLE && test.isMoving() ? WALK : BODY[action.ordinal()]);
		}));
		controllers.add(new AnimationController<MireHagEntity>("lantern", 4,
				test -> test.setAndContinue(test.animatable().isDeadOrDying() ? FLAME[Lantern.OUT.ordinal()] : FLAME[test.animatable().lantern().ordinal()])));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
