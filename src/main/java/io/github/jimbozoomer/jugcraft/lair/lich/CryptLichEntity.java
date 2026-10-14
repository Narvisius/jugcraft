package io.github.jimbozoomer.jugcraft.lair.lich;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.LoopType;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.lair.CryptArchive;
import io.github.jimbozoomer.jugcraft.lair.GraveSealBlock;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairBosses;
import io.github.jimbozoomer.jugcraft.lair.LairExitBlock;
import io.github.jimbozoomer.jugcraft.lair.LairInstance;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.NameStoneBlock;
import io.github.jimbozoomer.jugcraft.lair.NameStoneBlock.Glow;
import io.github.jimbozoomer.jugcraft.lair.NameStoneBlock.Rune;
import io.github.jimbozoomer.jugcraft.lair.PhylacteryBlock;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
 * The Crypt Lich (docs/features/crypt-lich.md): the Archive of Last Names' boss, a crowned skeleton a head taller than a
 * man in a scholar's robe gone to mould, chains of tarnished gold across it, his crown of black iron and his eyes cold
 * blue fire, his staff a thighbone bound to a sceptre. He waits on his throne, his crown dark, and wakes when someone who
 * may fight him steps onto the hall's floor or strikes him: he rises and comes down the apse's steps, unhurt, his health
 * scaled for the party.
 *
 * <p>His rule: the phylactery. Three soul chains run from the grave seals to his phylactery, and a fourth from it to him;
 * while they hold he is warded, and every blow, arrow and spell is turned aside. One name at a time is lit: its stone
 * burns, the bar names it. Using the lit name's seal breaks it and its chain, and the next name lights; using a dark
 * name's seal raises a Restless Dead from that alcove's sarcophagus. With all three broken the phylactery cracks: he is
 * exposed for {@value #EXPOSED_TICKS} ticks, staggered for the first {@value #STAGGER_TICKS}; then the chains reforge
 * over {@value #REFORGE_TICKS} ticks and the names light again in a new order. Phase 1, the Warded King: the Grave Sweep,
 * Bone Lanes, the Soul Bolt and Raise the Dead. At {@value #ERASE_AT} of his health, the Erasure: he shrieks, unhurt, the
 * chains reforge at once, and from now on whenever they reforge one name is scraped from its stone: it comes last, and
 * its seal holds until someone reads it back at the Lectern of Last Names or {@value #SCRAPED_TICKS} ticks pass. Phase
 * 2, the Unwritten: two lanes at once, the Soul Drain and the Grave Pull besides. Below {@value #LAST_AT}, the Last Name:
 * his phylactery shatters, he is exposed to the end, his cooldowns and his pause are a third shorter, and his dead rise
 * every {@value #LAST_RAISE} ticks.
 *
 * <p>Every attack has a wind-up you can read, a strike and a recovery ({@link Attack}); the numbers are here, in one place,
 * and in tools/crypt_lich.py. Wither, poison, drowning, knockback and falls never harm him. Each participant (whoever
 * hurt him or his dead, broke one of his seals or read a name back) gets their own loot ({@link CryptLichLoot}). Left
 * alone he climbs back to his throne, healed: his dead crumble, the seals mend, every name is cut dark again, the
 * phylactery is whole, and who hurt him is forgotten. Bound to a lair instance he vanishes with it; one with no instance
 * (a test's) keeps to the archive laid out from the origin he was given. The only blocks he changes are the archive's
 * seals, name-stones and phylactery, and on his fall the Grey Mist on the dais.
 */
public class CryptLichEntity extends Monster implements GeoEntity {
	// ---------------------------------------------------------------- numbers (tools/crypt_lich.py)
	public static final float HEALTH = 400.0F;
	public static final double ARMOR = 8.0;
	public static final double LEASH = 30.0;
	public static final double PARTY_STEP = 0.5;
	public static final double PARTY_MAX = 2.5;
	public static final int WAKE_TICKS = 40;
	public static final int ABANDON_TICKS = 200;
	public static final int GLOBAL_COOLDOWN = 20;
	public static final double SPEED = 0.16;
	public static final double KEEP_DISTANCE = 3.0;
	public static final double STEP = 1.0;
	public static final float ERASE_AT = 0.5F;
	public static final float LAST_AT = 0.2F;
	public static final double LAST_COOLDOWN = 0.6667;
	public static final int EXPOSED_TICKS = 240;
	public static final int STAGGER_TICKS = 40;
	public static final int REFORGE_TICKS = 30;
	public static final int REFORGE_STEP = 10;
	public static final int SCRAPED_TICKS = 600;
	public static final int DEAD_CAP = 4;
	public static final int RAISED = 2;
	public static final float SWEEP_DAMAGE = 7.0F;
	public static final double SWEEP_REACH = 3.5;
	public static final double SWEEP_HALF_ANGLE = 70.0;
	public static final double SWEEP_THROW = 1.2;
	public static final float LANE_DAMAGE = 6.0F;
	public static final double LANE_TOSS = 0.7;
	public static final double CLUMP_STEP = 2.0;
	public static final float BOLT_DAMAGE = 6.0F;
	public static final int WITHER_TICKS = 60;
	public static final float REFLECTED_DAMAGE = 10.0F;
	public static final int BOLT_STAGGER = 40;
	public static final int ERASE_TICKS = 40;
	public static final float DRAIN = 1.0F;
	public static final int DRAIN_EVERY = 10;
	public static final double DRAIN_RANGE = 16.0;
	public static final double PULL_RADIUS = 5.0;
	public static final double PULL_DARK = 60.0;
	public static final int PULL_BURST = 10;
	public static final float PULL_DAMAGE = 8.0F;
	public static final double PULL_REACH = 3.5;
	public static final int LAST_RAISE = 240;
	public static final int EXPERIENCE = 300;
	/** Waking: ticks spent rising from the throne before he comes down the steps. */
	private static final int RISE = 15;
	/** How high his way down the apse's steps arcs over the straight line. */
	private static final double STEP_DOWN_RISE = 1.0;
	/** The least ticks between two "warded" words to one player. */
	private static final int WARDED_WORDS = 40;

	/**
	 * His attacks: wind-up, the active part and recovery in ticks, the cooldown, the target's nearest and farthest distance,
	 * and the phases he uses it in (the Warded King; the Unwritten and the Last Name). The wind-up is what players read;
	 * the strike lands as it ends.
	 */
	public enum Attack {
		GRAVE_SWEEP(14, 4, 10, 40, 0.0, 4.0, true, true),
		BONE_LANES(24, 20, 10, 160, 0.0, 40.0, true, true),
		SOUL_BOLT(16, 1, 12, 100, 4.0, 24.0, true, true),
		RAISE_DEAD(30, 1, 10, 300, 0.0, 40.0, true, true),
		SOUL_DRAIN(20, 60, 10, 240, 0.0, 16.0, false, true),
		GRAVE_PULL(30, 10, 14, 280, 0.0, 5.0, false, true);

		public final int windup;
		public final int active;
		public final int recovery;
		public final int cooldown;
		public final double near;
		public final double far;
		public final boolean warded;
		public final boolean unwritten;

		Attack(int windup, int active, int recovery, int cooldown, double near, double far, boolean warded, boolean unwritten) {
			this.windup = windup;
			this.active = active;
			this.recovery = recovery;
			this.cooldown = cooldown;
			this.near = near;
			this.far = far;
			this.warded = warded;
			this.unwritten = unwritten;
		}

		public int length() {
			return windup + active + recovery;
		}
	}

	/** The fight's phases. */
	public enum Phase {
		WAITING, WAKING, WARDED_KING, ERASURE, UNWRITTEN, LAST_NAME
	}

	/**
	 * His ward: the chains holding (warded), all three broken (exposed), the chains reforging (warded still), or the
	 * phylactery shattered in the Last Name (exposed to the end).
	 */
	public enum Ward {
		CHAINED, EXPOSED, REFORGING, GONE
	}

	/** What his body is doing, synced for his animations. */
	public enum Action {
		THRONE, WAKE, IDLE, SWEEP_WINDUP, SWEEP, LANES_WINDUP, LANES, BOLT_WINDUP, BOLT, RAISE_WINDUP, RAISE, STAGGER, SHRIEK,
		DRAIN_WINDUP, DRAIN, PULL_WINDUP, PULL
	}

	/** The soul-fire on his crown, synced: dark while he waits, burning, or flaring in the Last Name. */
	public enum Crown {
		DARK, LIT, FLARE
	}

	private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(CryptLichEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> CROWN = SynchedEntityData.defineId(CryptLichEntity.class, EntityDataSerializers.INT);
	static final RawAnimation[] BODY = new RawAnimation[Action.values().length];
	static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.crypt_lich.walk");
	static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.crypt_lich.death");
	static final RawAnimation[] FIRE = {RawAnimation.begin().thenLoop("animation.crypt_lich.crown_dark"),
			RawAnimation.begin().thenLoop("animation.crypt_lich.crown_lit"), RawAnimation.begin().thenLoop("animation.crypt_lich.crown_flare")};
	static final RawAnimation HAND_EMPTY = RawAnimation.begin().thenLoop("animation.crypt_lich.hand_empty");
	static final RawAnimation HAND_SKULL = RawAnimation.begin().thenPlayAndHold("animation.crypt_lich.hand_skull");

	static {
		String prefix = "animation.crypt_lich.";
		BODY[Action.THRONE.ordinal()] = RawAnimation.begin().thenLoop(prefix + "throne");
		BODY[Action.WAKE.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "wake");
		BODY[Action.IDLE.ordinal()] = RawAnimation.begin().thenLoop(prefix + "idle");
		BODY[Action.SWEEP_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "sweep_windup");
		BODY[Action.SWEEP.ordinal()] = RawAnimation.begin().then(prefix + "sweep", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.LANES_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "lanes_windup");
		BODY[Action.LANES.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "lanes");
		BODY[Action.BOLT_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "bolt_windup");
		BODY[Action.BOLT.ordinal()] = RawAnimation.begin().then(prefix + "bolt", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.RAISE_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "raise_windup");
		BODY[Action.RAISE.ordinal()] = RawAnimation.begin().then(prefix + "raise", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.STAGGER.ordinal()] = RawAnimation.begin().thenLoop(prefix + "stagger");
		BODY[Action.SHRIEK.ordinal()] = RawAnimation.begin().thenLoop(prefix + "shriek");
		BODY[Action.DRAIN_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "drain_windup");
		BODY[Action.DRAIN.ordinal()] = RawAnimation.begin().thenLoop(prefix + "drain");
		BODY[Action.PULL_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "pull_windup");
		BODY[Action.PULL.ordinal()] = RawAnimation.begin().then(prefix + "pull", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
	}

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private final ServerBossEvent bossBar = new ServerBossEvent(UUID.randomUUID(), Component.translatable("entity.jugcraft.crypt_lich"),
			BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
	/** The lair instance he belongs to (null for one put down by a test or a command), and the archive's origin. */
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
	/** Whoever broke one of his seals or read a name back: participants too. */
	private final Set<UUID> helpers = new HashSet<>();
	private final Map<UUID, Long> wardedWords = new HashMap<>();
	// His ward.
	private Ward ward = Ward.CHAINED;
	private int wardTicks;
	/** The order the names light in, and how many have been broken this round (the lit one is order[broken]). */
	private final List<Rune> order = new ArrayList<>(List.of(Rune.values()));
	private int broken;
	/** What the time the scraped name was lit holds while it is not lit (any game time, even one before 0, is a time). */
	private static final long NOT_LIT = Long.MIN_VALUE;
	/** The name scraped from its stone this round (from the Erasure on), and the game time it was lit (or {@link #NOT_LIT}). */
	private @Nullable Rune scraped;
	private long scrapedLitAt = NOT_LIT;
	// His dead and his things.
	private final List<UUID> dead = new ArrayList<>();
	private final List<UUID> things = new ArrayList<>();
	private int lastRaise;
	// The attack under way.
	private final List<Rune> lanes = new ArrayList<>();
	private double pullDark;
	private final Set<UUID> pulled = new HashSet<>();
	/** Stepping down from the throne: from where. */
	private Vec3 stepFrom = Vec3.ZERO;
	/** Set when he is loaded in a passing moment (waking, the Erasure), or fighting: he lands and his chains reforge. */
	private boolean landOnLoad;
	private boolean rebind;

	public CryptLichEntity(EntityType<? extends CryptLichEntity> type, Level level) {
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
				.add(Attributes.ATTACK_DAMAGE, SWEEP_DAMAGE).add(Attributes.STEP_HEIGHT, STEP);
	}

	/**
	 * Seats him on the throne of {@code instance}'s archive. Called as the instance is placed. Placing the template puts
	 * back only its own blocks, so the Grey Mist a fallen Lich opened on the dais before still stands beside the
	 * phylactery: it goes first.
	 */
	public static CryptLichEntity summon(ServerLevel level, LairInstance instance) {
		BlockPos origin = instance.origin();
		for (BlockPos cell : CryptArchive.exitCells()) {
			BlockPos at = cell.offset(origin);
			if (level.getBlockState(at).is(JugcraftLairs.LAIR_EXIT)) {
				level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		return summon(level, origin, instance);
	}

	/** Seats him on the throne of the archive laid out from {@code origin} (tests pass no instance). */
	public static CryptLichEntity summon(ServerLevel level, BlockPos origin, @Nullable LairInstance instance) {
		CryptLichEntity lich = new CryptLichEntity(JugcraftLich.CRYPT_LICH, level);
		lich.origin = origin.immutable();
		if (instance != null) {
			lich.instance = instance.id;
			lich.slot = instance.slot;
		}
		lich.noPhysics = true;
		Vec3 waiting = lich.waiting();
		lich.snapTo(waiting.x, waiting.y, waiting.z, 0.0F, 0.0F);
		lich.face(0.0F);
		level.addFreshEntity(lich);
		return lich;
	}

	// ---------------------------------------------------------------- where things are

	/** The dais's middle, on the hall's floor, for the archive laid out from {@code origin}. */
	public static Vec3 centre(BlockPos origin) {
		return CryptArchive.centre().add(origin.getX(), origin.getY(), origin.getZ());
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
	public Vec3 waiting() {
		return at(CryptArchive.waiting());
	}

	/** Where he comes down to the hall's floor as he wakes. */
	public Vec3 woke() {
		return at(CryptArchive.woke());
	}

	/** The hall's floor, where he and his foes stand. */
	public double floorY() {
		return origin.getY() + CryptArchive.STAND;
	}

	public BlockPos seal(Rune rune) {
		return CryptArchive.seal(origin, rune);
	}

	public BlockPos stone(Rune rune) {
		return CryptArchive.stone(origin, rune);
	}

	public BlockPos phylactery() {
		return origin.offset(CryptArchive.PHYLACTERY);
	}

	public BlockPos lectern() {
		return origin.offset(CryptArchive.LECTERN);
	}

	/** Where his left hand is as he looses a bolt, ahead of him and up (tools/crypt_lich_models.py HAND_Z and HAND_Y). */
	public Vec3 hand() {
		return position().add(facing().scale(SoulBoltEntity.HAND_AHEAD)).add(0.0, SoulBoltEntity.HAND_UP, 0.0);
	}

	/** Whether {@code player} stands on the hall's floor (off the stair and the landing). */
	public boolean onHallFloor(Player player) {
		double x = player.getX() - origin.getX();
		double z = player.getZ() - origin.getZ();
		double y = player.getY() - origin.getY();
		return x >= CryptArchive.HALL_WEST && x <= CryptArchive.HALL_EAST + 1 && z >= CryptArchive.HALL_NORTH
				&& z <= CryptArchive.HALL_SOUTH + 1 && y >= CryptArchive.STAND - 1 && y <= CryptArchive.STAND + 4;
	}

	// ---------------------------------------------------------------- state

	public Phase phase() {
		return phase;
	}

	public Ward ward() {
		return ward;
	}

	/** Whether his chains turn every blow aside now. */
	public boolean warded() {
		return ward == Ward.CHAINED || ward == Ward.REFORGING;
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

	/** The name lit now (the next seal to break), or null when no chain holds. */
	public @Nullable Rune lit() {
		return ward == Ward.CHAINED && broken < order.size() ? order.get(broken) : null;
	}

	/** The order the names light in this round. */
	public List<Rune> order() {
		return List.copyOf(order);
	}

	public @Nullable Rune scraped() {
		return scraped;
	}

	/** Sets when the scraped name was lit (for tests: a time long past, so that it burns back on his next tick). */
	public void scrapedSince(long time) {
		scrapedLitAt = time;
	}

	public List<UUID> dead() {
		return List.copyOf(dead);
	}

	public List<Rune> lanes() {
		return List.copyOf(lanes);
	}

	public Action action() {
		int id = entityData.get(ACTION);
		return id >= 0 && id < Action.values().length ? Action.values()[id] : Action.IDLE;
	}

	private void setAction(Action action) {
		entityData.set(ACTION, action.ordinal());
	}

	public Crown crown() {
		int id = entityData.get(CROWN);
		return id >= 0 && id < Crown.values().length ? Crown.values()[id] : Crown.DARK;
	}

	private void setCrown(Crown crown) {
		entityData.set(CROWN, crown.ordinal());
	}

	public @Nullable UUID instanceId() {
		return instance;
	}

	/** Whether he is fighting (down in the hall, not waiting, waking or shrieking). */
	public boolean fighting() {
		return phase == Phase.WARDED_KING || phase == Phase.UNWRITTEN || phase == Phase.LAST_NAME;
	}

	/** Damage he deals: the attack's own, times {@code lairs.boss_damage} ({@link LairBosses}). */
	public static float damage(float base) {
		return LairBosses.damage(base);
	}

	/** His health for a party of {@code players}: half as much again for each beyond the first, at most 2.5 times. */
	public static double partyScale(int players) {
		return LairBosses.partyScale(players, PARTY_STEP, PARTY_MAX);
	}

	/** A cooldown as it is in this phase: a third shorter in the Last Name. */
	public int cooldown(int ticks) {
		return phase == Phase.LAST_NAME ? (int) Math.round(ticks * LAST_COOLDOWN) : ticks;
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
		if (rebind) {
			rebind = false;
			if (phase == Phase.LAST_NAME) {
				ward = Ward.GONE;
			} else if (fighting()) {
				forge(level, phase == Phase.UNWRITTEN);
			}
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
			case ERASURE -> erasure(level);
			case WARDED_KING, UNWRITTEN, LAST_NAME -> {
				tickWard(level);
				fight(level);
			}
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
		noPhysics = true;
		setDeltaMovement(Vec3.ZERO);
		Vec3 waiting = waiting();
		if (position().distanceToSqr(waiting) > 0.01) {
			setPos(waiting.x, waiting.y, waiting.z);
		}
		face(0.0F);
		setAction(Action.THRONE);
		if (tickCount % 10 == 0 && intruder(level) != null) {
			wake(level);
		}
	}

	/** Someone who may fight him, standing on the hall's floor. */
	private @Nullable Player intruder(ServerLevel level) {
		for (Player player : level.players()) {
			if (LairBosses.eligible(player) && onHallFloor(player)) {
				return player;
			}
		}
		return null;
	}

	/** He rises from his throne: his health scaled for the players in his hall, his bar shown, soul-fire down the lanes. */
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
		setCrown(Crown.LIT);
		bossBar.setVisible(true);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.WITHER_SKELETON_AMBIENT, SoundSource.HOSTILE, 3.0F, 0.5F);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 3.0F, 0.6F);
		for (Rune rune : Rune.values()) {
			laneFire(level, rune, ParticleTypes.SOUL_FIRE_FLAME, 2);
		}
		tell(level, "message.jugcraft.crypt_lich.wakes");
	}

	/** Waking: he rises from the throne, then comes down the apse's steps in an arc; his chains burn, and a name lights. */
	private void waking(ServerLevel level) {
		noPhysics = true;
		setDeltaMovement(Vec3.ZERO);
		if (phaseTicks <= RISE) {
			if (phaseTicks == RISE) {
				stepFrom = position();
			}
			return;
		}
		int glide = WAKE_TICKS - RISE;
		double share = Math.min(1.0, (phaseTicks - RISE) / (double) glide);
		Vec3 to = woke();
		Vec3 along = stepFrom.add(to.subtract(stepFrom).scale(share));
		setPos(along.x, along.y + STEP_DOWN_RISE * 4.0 * share * (1.0 - share), along.z);
		face(to.subtract(stepFrom));
		if (phaseTicks >= WAKE_TICKS) {
			teleportTo(to.x, to.y, to.z);
			noPhysics = false;
			phase = Phase.WARDED_KING;
			phaseTicks = 0;
			globalCooldown = GLOBAL_COOLDOWN;
			setAction(Action.IDLE);
			forge(level, false);
		}
	}

	/** Players near enough to his hall to be fighting (or about to). */
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

	// ---------------------------------------------------------------- his rule: the phylactery

	/**
	 * The chains are forged whole: every seal whole, the phylactery whole, the names dark, then a new order, the first
	 * name lit. With {@code scrape}, the name that comes last is scraped from its stone.
	 */
	public void forge(ServerLevel level, boolean scrape) {
		for (Rune rune : Rune.values()) {
			GraveSealBlock.setBroken(level, seal(rune), false);
			NameStoneBlock.setGlow(level, stone(rune), Glow.DARK);
		}
		PhylacteryBlock.setCracked(level, phylactery(), false);
		java.util.Collections.shuffle(order, new java.util.Random(getRandom().nextLong()));
		broken = 0;
		scraped = scrape ? order.get(order.size() - 1) : null;
		scrapedLitAt = NOT_LIT;
		if (scraped != null) {
			NameStoneBlock.setGlow(level, stone(scraped), Glow.ERASED);
		}
		ward = Ward.CHAINED;
		wardTicks = 0;
		lightNext(level);
	}

	/** The name order[broken] lights: its stone burns (a scraped one stays blank), and the bar names it. */
	private void lightNext(ServerLevel level) {
		Rune next = lit();
		if (next == null) {
			return;
		}
		if (next == scraped) {
			scrapedLitAt = level.getGameTime();
		} else {
			NameStoneBlock.setGlow(level, stone(next), Glow.LIT);
		}
		bossBar.setName(Component.translatable("message.jugcraft.crypt_lich.answers", next.display()));
		level.playSound(null, stone(next), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 2.0F, 0.8F);
	}

	/** His ward ticks on: the chains drawn while they hold, a scraped name burning back, the exposure running out, the reforging. */
	private void tickWard(ServerLevel level) {
		switch (ward) {
			case CHAINED -> {
				Rune lit = lit();
				if (lit != null && lit == scraped && scrapedLitAt != NOT_LIT && level.getGameTime() - scrapedLitAt >= SCRAPED_TICKS) {
					restore(level, null);
				}
				if (tickCount % 5 == 0) {
					chains(level);
				}
			}
			case EXPOSED -> {
				wardTicks--;
				if (wardTicks <= 0) {
					ward = Ward.REFORGING;
					wardTicks = 0;
					java.util.Collections.shuffle(order, new java.util.Random(getRandom().nextLong()));
				}
			}
			case REFORGING -> {
				if (wardTicks % REFORGE_STEP == 0 && wardTicks / REFORGE_STEP < order.size()) {
					Rune mend = order.get(wardTicks / REFORGE_STEP);
					BlockPos at = seal(mend);
					GraveSealBlock.setBroken(level, at, false);
					level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
					level.playSound(null, at, SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 1.5F, 0.6F);
				}
				wardTicks++;
				if (wardTicks >= REFORGE_TICKS) {
					forge(level, phase == Phase.UNWRITTEN);
					tell(level, "message.jugcraft.crypt_lich.reforged");
				}
			}
			case GONE -> {
			}
		}
	}

	/** The chains drawn in soul-fire: from each whole seal to the phylactery, and from it to him; the lit seal burns. */
	private void chains(ServerLevel level) {
		Vec3 urn = Vec3.atCenterOf(phylactery());
		for (Rune rune : Rune.values()) {
			BlockPos at = seal(rune);
			BlockState state = level.getBlockState(at);
			if (state.getBlock() instanceof GraveSealBlock && !state.getValue(GraveSealBlock.BROKEN)) {
				line(level, Vec3.atCenterOf(at), urn, ParticleTypes.SOUL_FIRE_FLAME, 2.0);
			}
		}
		line(level, urn, position().add(0.0, 1.6, 0.0), ParticleTypes.SOUL_FIRE_FLAME, 2.0);
		Rune lit = lit();
		if (lit != null) {
			BlockPos at = seal(lit);
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 3, 0.2, 0.25, 0.2, 0.01);
		}
	}

	/**
	 * {@code player} used the seal at {@code pos} (with {@code state}): the lit name's seal breaks its chain; a dark name's
	 * holds, and a Restless Dead climbs out of that alcove's sarcophagus. Returns whether this fight answered the use.
	 */
	public boolean sealUsed(ServerLevel level, BlockPos pos, BlockState state, Player player) {
		if (!fighting() && phase != Phase.ERASURE) {
			return false;
		}
		Rune rune = state.getValue(GraveSealBlock.RUNE);
		if (!seal(rune).equals(pos)) {
			return false;
		}
		if (ward != Ward.CHAINED || state.getValue(GraveSealBlock.BROKEN)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.crypt_lich.seal_broken"));
			return true;
		}
		if (rune != lit()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.crypt_lich.seal_holds"));
			level.playSound(null, pos, SoundEvents.CHAIN_HIT, SoundSource.HOSTILE, 1.5F, 0.5F);
			raise(level, CryptArchive.sarcophagusDead(rune).add(origin.getX(), origin.getY(), origin.getZ()));
			return true;
		}
		BlockState carved = level.getBlockState(stone(rune));
		if (carved.getBlock() instanceof NameStoneBlock && carved.getValue(NameStoneBlock.GLOW) == Glow.ERASED) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.crypt_lich.seal_scraped"));
			level.playSound(null, pos, SoundEvents.CHAIN_HIT, SoundSource.HOSTILE, 1.5F, 0.5F);
			return true;
		}
		breakSeal(level, rune);
		helpers.add(player.getUUID());
		return true;
	}

	/** The lit name's seal breaks and its chain snaps; the next name lights, or, with all three broken, he is exposed. */
	public void breakSeal(ServerLevel level, Rune rune) {
		BlockPos at = seal(rune);
		GraveSealBlock.setBroken(level, at, true);
		NameStoneBlock.setGlow(level, stone(rune), Glow.DARK);
		level.sendParticles(ParticleTypes.SOUL, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 16, 0.3, 0.3, 0.3, 0.04);
		level.playSound(null, at, SoundEvents.CHAIN_BREAK, SoundSource.HOSTILE, 2.5F, 0.6F);
		tellNamed(level, "message.jugcraft.crypt_lich.chain_broken", rune);
		if (rune == scraped) {
			scraped = null;
			scrapedLitAt = NOT_LIT;
		}
		broken++;
		if (broken >= order.size()) {
			expose(level);
		} else {
			lightNext(level);
		}
	}

	/** All three chains broken: the phylactery cracks and the fourth chain snaps; he is exposed, staggered at first. */
	public void expose(ServerLevel level) {
		ward = Ward.EXPOSED;
		wardTicks = EXPOSED_TICKS;
		stagger(STAGGER_TICKS);
		PhylacteryBlock.setCracked(level, phylactery(), true);
		BlockPos urn = phylactery();
		level.sendParticles(ParticleTypes.SOUL, urn.getX() + 0.5, urn.getY() + 0.6, urn.getZ() + 0.5, 30, 0.3, 0.4, 0.3, 0.05);
		level.playSound(null, urn, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 3.0F, 0.5F);
		bossBar.setName(Component.translatable("message.jugcraft.crypt_lich.exposed_bar"));
		tell(level, "message.jugcraft.crypt_lich.exposed");
	}

	/** He reels: any attack under way is spoiled, and he does nothing for {@code ticks}. */
	public void stagger(int ticks) {
		attack = null;
		attackTicks = 0;
		staggerTicks = Math.max(staggerTicks, ticks);
		setAction(Action.STAGGER);
	}

	/**
	 * Someone read the names back at the lectern ({@code reader}, or nobody when the scraped name burns back by itself):
	 * the lit name, if it was the scraped one, burns on its stone again, and its seal will break.
	 */
	public void restore(ServerLevel level, @Nullable Player reader) {
		if (reader != null) {
			helpers.add(reader.getUUID());
		}
		Rune lit = lit();
		if (scraped == null) {
			return;
		}
		if (lit == scraped) {
			NameStoneBlock.setGlow(level, stone(scraped), Glow.LIT);
		} else {
			NameStoneBlock.setGlow(level, stone(scraped), Glow.DARK);
		}
		scraped = null;
		scrapedLitAt = NOT_LIT;
		tell(level, "message.jugcraft.crypt_lich.restored");
	}

	/** A name was read back at the lectern of his archive by {@code player}, restoring {@code restored}. */
	public void lecternRead(ServerLevel level, Player player, List<Rune> restored) {
		helpers.add(player.getUUID());
		if (scraped != null && restored.contains(scraped)) {
			restore(level, player);
		}
	}

	// ---------------------------------------------------------------- fighting

	private void fight(ServerLevel level) {
		keepToTheHall(level);
		if (phase == Phase.WARDED_KING && getHealth() <= getMaxHealth() * ERASE_AT) {
			erase(level);
			return;
		}
		if (phase == Phase.UNWRITTEN && getHealth() <= getMaxHealth() * LAST_AT) {
			lastName(level);
			return;
		}
		Player target = target(level);
		if (target == null) {
			attack = null;
			setAction(staggerTicks > 0 ? Action.STAGGER : Action.IDLE);
			move(centre(), SPEED);
			if (++abandoned >= ABANDON_TICKS) {
				reset(level);
			}
			return;
		}
		abandoned = 0;
		if (phase == Phase.LAST_NAME && ++lastRaise >= LAST_RAISE) {
			lastRaise = 0;
			raiseAt(level, target);
		}
		if (staggerTicks > 0) {
			staggerTicks--;
			hold();
			if (staggerTicks == 0) {
				setAction(Action.IDLE);
				globalCooldown = cooldown(GLOBAL_COOLDOWN);
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

	/** Strayed farther than his leash from the dais, or fallen below the floor, he is drawn back to it in a burst of souls. */
	private void keepToTheHall(ServerLevel level) {
		Vec3 centre = centre();
		double dx = getX() - centre.x;
		double dz = getZ() - centre.z;
		if (dx * dx + dz * dz <= LEASH * LEASH && getY() > centre.y - 3.0) {
			return;
		}
		level.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1.0, getZ(), 20, 0.6, 0.8, 0.6, 0.02);
		attack = null;
		noPhysics = false;
		Vec3 back = centre.add(0.0, 0.0, 4.0);
		teleportTo(back.x, back.y, back.z);
		level.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1.0, getZ(), 20, 0.6, 0.8, 0.6, 0.02);
	}

	/** His target: the one he has while they are still in reach of his hall, else the nearest who may fight him. */
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

	/** His foe now, if he has one. */
	public @Nullable Player foe(ServerLevel level) {
		return foe == null ? null : level.getPlayerByUUID(foe);
	}

	/** Glides toward a point {@value #KEEP_DISTANCE} blocks short of {@code target}. */
	private void approach(Player target) {
		Vec3 to = target.position().subtract(position());
		double flat = Math.sqrt(to.x * to.x + to.z * to.z);
		Vec3 goal = flat > KEEP_DISTANCE + 0.5
				? new Vec3(target.getX() - to.x / flat * KEEP_DISTANCE, getY(), target.getZ() - to.z / flat * KEEP_DISTANCE)
				: position();
		move(goal, SPEED);
	}

	/** Glides toward {@code goal} at {@code speed}, pressed down onto the floor. */
	private void move(Vec3 goal, double speed) {
		Vec3 to = new Vec3(goal.x - getX(), 0.0, goal.z - getZ());
		double length = to.length();
		Vec3 step = length < 0.05 ? Vec3.ZERO : to.scale(Math.min(speed, length) / length);
		setDeltaMovement(step.x, -0.3, step.z);
	}

	/** Holds still where he is, pressed down onto what he stands on. */
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

	/** The way he faces, flat. */
	public Vec3 facing() {
		double yaw = Math.toRadians(getYRot());
		return new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
	}

	/** Whether nothing solid stands between his eyes and {@code target}'s (a pillar breaks his Soul Drain). */
	public boolean sees(ServerLevel level, Entity target) {
		Vec3 from = getEyePosition();
		Vec3 to = target.getEyePosition();
		return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() != HitResult.Type.BLOCK;
	}

	/** Picks an attack he can use now on {@code target}, or null. */
	private @Nullable Attack choose(ServerLevel level, Player target) {
		double distance = target.distanceTo(this);
		List<Attack> ready = new ArrayList<>();
		for (Attack candidate : Attack.values()) {
			boolean phaseAllows = phase == Phase.WARDED_KING ? candidate.warded : candidate.unwritten;
			if (!phaseAllows || cooldowns[candidate.ordinal()] > 0 || distance < candidate.near || distance > candidate.far) {
				continue;
			}
			if (candidate == Attack.RAISE_DEAD && dead.size() >= DEAD_CAP) {
				continue;
			}
			if (candidate == Attack.SOUL_DRAIN && !sees(level, target)) {
				continue;
			}
			ready.add(candidate);
			if (candidate == Attack.GRAVE_SWEEP) {
				ready.add(candidate);  // close in, his staff comes most often
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
		hold();
		face(target);
		switch (next) {
			case GRAVE_SWEEP -> {
				setAction(Action.SWEEP_WINDUP);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.WITHER_SKELETON_AMBIENT, SoundSource.HOSTILE, 1.5F, 0.7F);
			}
			case BONE_LANES -> {
				setAction(Action.LANES_WINDUP);
				callLanes(level, target, phase == Phase.WARDED_KING ? 1 : 2);
			}
			case SOUL_BOLT -> {
				setAction(Action.BOLT_WINDUP);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1.5F, 1.2F);
			}
			case RAISE_DEAD -> {
				setAction(Action.RAISE_WINDUP);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 2.5F, 0.5F);
			}
			case SOUL_DRAIN -> {
				setAction(Action.DRAIN_WINDUP);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.5F, 0.5F);
			}
			case GRAVE_PULL -> {
				setAction(Action.PULL_WINDUP);
				pullDark = getRandom().nextDouble() * 360.0;
				pulled.clear();
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 2.5F, 0.5F);
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
			case GRAVE_SWEEP -> {
				if (t < current.windup) {
					if (t % 4 == 0) {
						Vec3 staff = position().add(facing().scale(-0.4)).add(0.0, 3.0, 0.0);
						level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, staff.x, staff.y, staff.z, 2, 0.2, 0.2, 0.2, 0.0);
					}
				} else if (t == current.windup) {
					setAction(Action.SWEEP);
					sweep(level);
				}
			}
			case BONE_LANES -> boneLanes(level, t);
			case SOUL_BOLT -> {
				if (t < current.windup) {
					face(target);
					if (t % 4 == 0) {
						Vec3 hand = hand();
						level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, hand.x, hand.y, hand.z, 2, 0.1, 0.1, 0.1, 0.0);
					}
				} else if (t == current.windup) {
					setAction(Action.BOLT);
					things.add(SoulBoltEntity.loose(level, this, target).getUUID());
				}
			}
			case RAISE_DEAD -> {
				if (t < current.windup) {
					if (t % 3 == 0) {
						for (int grave : graves(target.position(), RAISED)) {
							Vec3 at = CryptArchive.grave(grave).add(origin.getX(), origin.getY(), origin.getZ());
							level.sendParticles(ParticleTypes.SOUL, at.x, at.y + 0.1, at.z, 2, 0.4, 0.05, 0.7, 0.0);
						}
					}
				} else if (t == current.windup) {
					setAction(Action.RAISE);
					raiseAt(level, target);
				}
			}
			case SOUL_DRAIN -> drain(level, target, t);
			case GRAVE_PULL -> gravePull(level, t);
		}
		if (attack == current && attackTicks >= current.length()) {
			attack = null;
			attackTicks = 0;
			globalCooldown = cooldown(GLOBAL_COOLDOWN);
			Action action = action();
			if (action == Action.LANES || action == Action.DRAIN || action.name().endsWith("_WINDUP")) {
				setAction(Action.IDLE);
			}
		}
	}

	/**
	 * Grave Sweep: every player within {@value #SWEEP_REACH} blocks before him (within {@value #SWEEP_HALF_ANGLE} degrees
	 * either side of his face) takes {@value #SWEEP_DAMAGE} damage and is thrown back.
	 */
	public int sweep(ServerLevel level) {
		int hit = 0;
		Vec3 facing = facing();
		for (ServerPlayer player : nearby(level)) {
			Vec3 to = player.position().subtract(position());
			double flat = Math.sqrt(to.x * to.x + to.z * to.z);
			if (flat > SWEEP_REACH + getBbWidth() / 2.0 + player.getBbWidth() / 2.0 || Math.abs(to.y) > 2.5) {
				continue;
			}
			double cos = flat < 1.0E-3 ? 1.0 : (to.x * facing.x + to.z * facing.z) / flat;
			if (cos < Math.cos(Math.toRadians(SWEEP_HALF_ANGLE))) {
				continue;  // behind him or to his side
			}
			if (player.hurtServer(level, damageSources().mobAttack(this), damage(SWEEP_DAMAGE))) {
				hit++;
				Vec3 away = flat < 1.0E-3 ? facing : new Vec3(to.x / flat, 0.0, to.z / flat);
				push(player, away.scale(SWEEP_THROW).add(0.0, 0.35, 0.0));
			}
		}
		Vec3 front = position().add(facing.scale(1.8)).add(0.0, 1.2, 0.0);
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, front.x, front.y, front.z, 3, 0.8, 0.2, 0.8, 0.0);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5F, 0.5F);
		return hit;
	}

	/** Sets {@code player} moving at {@code motion}, and tells their client (a player's own motion is theirs to move). */
	static void push(ServerPlayer player, Vec3 motion) {
		player.setDeltaMovement(motion);
		player.connection.send(new ClientboundSetEntityMotionPacket(player));
	}

	/**
	 * Bone Lanes: he calls the {@code count} names whose lanes lie nearest {@code target}; those lanes crack with blue fire
	 * through the wind-up.
	 */
	public void callLanes(ServerLevel level, Player target, int count) {
		lanes.clear();
		List<Rune> byDistance = new ArrayList<>(List.of(Rune.values()));
		double x = target.getX() - origin.getX();
		double z = target.getZ() - origin.getZ();
		byDistance.sort((a, b) -> Double.compare(CryptArchive.fromLane(a, x, z), CryptArchive.fromLane(b, x, z)));
		for (int i = 0; i < Math.min(count, byDistance.size()); i++) {
			lanes.add(byDistance.get(i));
			tellNamed(level, "message.jugcraft.crypt_lich.calls", byDistance.get(i));
		}
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.BONE_BLOCK_HIT, SoundSource.HOSTILE, 2.5F, 0.5F);
	}

	/** Sets the lanes of a Bone Lanes, as its wind-up would (for tests). */
	public void lanes(List<Rune> called) {
		lanes.clear();
		lanes.addAll(called);
	}

	/**
	 * Bone Lanes, {@code t} ticks into it: through the wind-up the called lanes crack with blue fire; as it ends, bones
	 * burst up the whole of each ({@link #bones}), a clump every {@value #CLUMP_STEP} blocks down its middle, standing
	 * through the active ticks.
	 */
	private void boneLanes(ServerLevel level, int t) {
		Attack lanesAttack = Attack.BONE_LANES;
		if (t < lanesAttack.windup) {
			if (t % 2 == 0) {
				for (Rune rune : lanes) {
					laneFire(level, rune, ParticleTypes.SOUL_FIRE_FLAME, 1);
				}
			}
			return;
		}
		if (t == lanesAttack.windup) {
			setAction(Action.LANES);
			for (Rune rune : lanes) {
				double[] line = CryptArchive.laneLine(rune);
				double length = Math.hypot(line[2] - line[0], line[3] - line[1]);
				for (double along = 0.0; along <= length + 1.0E-6; along += CLUMP_STEP) {
					double share = length < 1.0E-6 ? 0.0 : along / length;
					Vec3 at = new Vec3(origin.getX() + line[0] + (line[2] - line[0]) * share, floorY(),
							origin.getZ() + line[1] + (line[3] - line[1]) * share);
					things.add(LichBonesEntity.burst(level, this, at, lanesAttack.active).getUUID());
				}
			}
			bones(level);
		}
	}

	/**
	 * The bones burst up every called lane: each player standing on one (or reaching over its edge) and not high over the
	 * floor takes {@value #LANE_DAMAGE} damage and is tossed up.
	 */
	public int bones(ServerLevel level) {
		int hit = 0;
		for (ServerPlayer player : nearby(level)) {
			if (player.getY() > floorY() + 2.5 || player.getY() < floorY() - 1.0) {
				continue;
			}
			double x = player.getX() - origin.getX();
			double z = player.getZ() - origin.getZ();
			for (Rune rune : lanes) {
				if (CryptArchive.onLane(rune, x, z, player.getBbWidth() / 2.0)) {
					if (player.hurtServer(level, damageSources().mobAttack(this), damage(LANE_DAMAGE))) {
						hit++;
						push(player, new Vec3(player.getDeltaMovement().x, LANE_TOSS, player.getDeltaMovement().z));
					}
					break;
				}
			}
		}
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.BONE_BLOCK_BREAK, SoundSource.HOSTILE, 3.0F, 0.5F);
		return hit;
	}

	/** Blue fire flickers down the lane to {@code rune}'s alcove, {@code count} flames a cell. */
	private void laneFire(ServerLevel level, Rune rune, ParticleOptions particle, int count) {
		int[] lane = CryptArchive.LANES[rune.ordinal()];
		for (int x = lane[0]; x <= lane[1]; x += 2) {
			for (int z = lane[2]; z <= lane[3]; z += 2) {
				level.sendParticles(particle, origin.getX() + x + 0.5, floorY() + 0.1, origin.getZ() + z + 0.5, count, 0.4, 0.02, 0.4, 0.0);
			}
		}
	}

	/** The {@code count} floor graves nearest {@code to} (indexes of {@link CryptArchive#GRAVES}). */
	public List<Integer> graves(Vec3 to, int count) {
		List<Integer> out = new ArrayList<>();
		for (int i = 0; i < CryptArchive.GRAVES.length; i++) {
			out.add(i);
		}
		out.sort((a, b) -> Double.compare(CryptArchive.grave(a).add(origin.getX(), origin.getY(), origin.getZ()).distanceToSqr(to),
				CryptArchive.grave(b).add(origin.getX(), origin.getY(), origin.getZ()).distanceToSqr(to)));
		return out.subList(0, Math.min(count, out.size()));
	}

	/** Raise the Dead: {@value #RAISED} Restless Dead climb out of the graves nearest {@code target}, no more than the cap allows. */
	public int raiseAt(ServerLevel level, Player target) {
		int raised = 0;
		for (int grave : graves(target.position(), RAISED)) {
			if (raise(level, CryptArchive.grave(grave).add(origin.getX(), origin.getY(), origin.getZ())) != null) {
				raised++;
			}
		}
		return raised;
	}

	/** One Restless Dead climbs out at {@code at}, unless {@value #DEAD_CAP} already walk; returns it, or null. */
	public @Nullable RestlessDeadEntity raise(ServerLevel level, Vec3 at) {
		if (dead.size() >= DEAD_CAP) {
			return null;
		}
		RestlessDeadEntity risen = RestlessDeadEntity.rise(level, this, at);
		dead.add(risen.getUUID());
		return risen;
	}

	/**
	 * Soul Drain, {@code t} ticks into it: through the wind-up a thin beam joins him to his foe; then every
	 * {@value #DRAIN_EVERY} ticks it takes {@value #DRAIN} health from them and heals him as much. A block between them or
	 * {@value #DRAIN_RANGE} blocks breaks it, and the attack ends.
	 */
	private void drain(ServerLevel level, Player target, int t) {
		Attack drainAttack = Attack.SOUL_DRAIN;
		face(target);
		if (t > drainAttack.windup + drainAttack.active) {
			return;
		}
		if (target.distanceTo(this) > DRAIN_RANGE || !sees(level, target)) {
			snap(level, target);
			return;
		}
		if (t % 2 == 0) {
			line(level, hand(), target.position().add(0.0, 1.0, 0.0), t < drainAttack.windup ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SOUL, 0.8);
		}
		if (t == drainAttack.windup) {
			setAction(Action.DRAIN);
		}
		if (t > drainAttack.windup && (t - drainAttack.windup) % DRAIN_EVERY == 0) {
			drainOnce(level, target);
		}
	}

	/** One draught of his Soul Drain: {@value #DRAIN} from {@code target}, and he is healed as much. Returns what he took. */
	public float drainOnce(ServerLevel level, Player target) {
		float amount = damage(DRAIN);
		if (!target.hurtServer(level, damageSources().magic(), amount)) {
			return 0.0F;
		}
		heal(amount);
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.5F, 1.2F);
		return amount;
	}

	/** The beam of his Soul Drain breaks: the attack ends at once. */
	private void snap(ServerLevel level, Player target) {
		Vec3 mid = position().add(target.position()).scale(0.5).add(0.0, 1.5, 0.0);
		level.sendParticles(ParticleTypes.SMOKE, mid.x, mid.y, mid.z, 12, 0.4, 0.4, 0.4, 0.02);
		attack = null;
		attackTicks = 0;
		globalCooldown = cooldown(GLOBAL_COOLDOWN);
		setAction(Action.IDLE);
	}

	/**
	 * Grave Pull, {@code t} ticks into it: through the wind-up a ring of soul-fire {@value #PULL_RADIUS} blocks round him
	 * flares, one {@value #PULL_DARK}-degree part of it dark; as it ends everyone in the ring but those in the dark part is
	 * dragged to him ({@link #pull}), and {@value #PULL_BURST} ticks later bones burst round his feet ({@link #pullBurst}).
	 */
	private void gravePull(ServerLevel level, int t) {
		Attack pullAttack = Attack.GRAVE_PULL;
		if (t < pullAttack.windup) {
			if (t % 3 == 0) {
				ring(level, position().add(0.0, 0.1, 0.0), PULL_RADIUS, 36);
			}
			return;
		}
		if (t == pullAttack.windup) {
			setAction(Action.PULL);
			pull(level);
		}
		if (t == pullAttack.windup + PULL_BURST) {
			pullBurst(level);
		}
	}

	/** The ring of the Grave Pull: soul-fire round him, smoke in its dark part. */
	private void ring(ServerLevel level, Vec3 centre, double radius, int points) {
		for (int i = 0; i < points; i++) {
			double angle = 360.0 * i / points;
			double rad = Math.toRadians(angle);
			ParticleOptions particle = inDark(angle) ? ParticleTypes.SMOKE : ParticleTypes.SOUL_FIRE_FLAME;
			level.sendParticles(particle, centre.x + Math.cos(rad) * radius, centre.y, centre.z + Math.sin(rad) * radius, 1, 0.0, 0.02, 0.0, 0.0);
		}
	}

	/** Whether the angle (degrees, as {@link Math#atan2} gives it from z over x) lies in the Grave Pull's dark part. */
	public boolean inDark(double angle) {
		double off = ((angle - pullDark) % 360.0 + 540.0) % 360.0 - 180.0;
		return Math.abs(off) <= PULL_DARK / 2.0;
	}

	/** Sets where the Grave Pull's dark part lies (degrees), as its wind-up would (for tests). */
	public void pullDark(double angle) {
		pullDark = angle;
	}

	/** Everyone within {@value #PULL_RADIUS} blocks of him but those in the ring's dark part is dragged to him. */
	public int pull(ServerLevel level) {
		int dragged = 0;
		for (ServerPlayer player : nearby(level)) {
			Vec3 from = position().subtract(player.position());
			double flat = Math.hypot(from.x, from.z);
			double angle = Math.toDegrees(Math.atan2(-from.z, -from.x));
			if (flat > PULL_RADIUS + player.getBbWidth() / 2.0 || Math.abs(from.y) > 2.5 || inDark(angle) || flat < 1.0E-3) {
				continue;
			}
			double speed = Math.min(1.6, (flat - 1.2) * 0.35 + 0.2);
			push(player, new Vec3(from.x / flat * speed, 0.3, from.z / flat * speed));
			pulled.add(player.getUUID());
			dragged++;
		}
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 2.5F, 0.5F);
		return dragged;
	}

	/** The bones burst round his feet: {@value #PULL_DAMAGE} damage to everyone within {@value #PULL_REACH} blocks. */
	public int pullBurst(ServerLevel level) {
		int hit = 0;
		for (int i = 0; i < 6; i++) {
			double rad = Math.toRadians(60.0 * i);
			things.add(LichBonesEntity.burst(level, this, new Vec3(getX() + Math.cos(rad) * 2.0, floorY(), getZ() + Math.sin(rad) * 2.0),
					Attack.GRAVE_PULL.recovery).getUUID());
		}
		for (ServerPlayer player : nearby(level)) {
			double flat = Math.hypot(player.getX() - getX(), player.getZ() - getZ());
			if (flat > PULL_REACH + player.getBbWidth() / 2.0 || Math.abs(player.getY() - getY()) > 2.5) {
				continue;
			}
			if (player.hurtServer(level, damageSources().mobAttack(this), damage(PULL_DAMAGE))) {
				hit++;
			}
		}
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.BONE_BLOCK_BREAK, SoundSource.HOSTILE, 3.0F, 0.4F);
		return hit;
	}

	// ---------------------------------------------------------------- the Erasure, and the Last Name

	/**
	 * The Erasure: he shrieks for {@value #ERASE_TICKS} ticks, unhurt; the chains reforge at once, the name that comes last
	 * scraped from its stone; then the Unwritten.
	 */
	public void erase(ServerLevel level) {
		attack = null;
		staggerTicks = 0;
		phase = Phase.ERASURE;
		phaseTicks = 0;
		setAction(Action.SHRIEK);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.GHAST_SCREAM, SoundSource.HOSTILE, 3.0F, 0.5F);
		forge(level, true);
		tell(level, "message.jugcraft.crypt_lich.erasure");
	}

	/** The Erasure holds: he shrieks, unhurt, and then the Unwritten begins. */
	private void erasure(ServerLevel level) {
		hold();
		tickWard(level);
		if (phaseTicks % 6 == 0) {
			level.sendParticles(ParticleTypes.SOUL, getX(), getY() + 2.2, getZ(), 6, 0.5, 0.4, 0.5, 0.03);
		}
		if (phaseTicks >= ERASE_TICKS) {
			phase = Phase.UNWRITTEN;
			phaseTicks = 0;
			globalCooldown = GLOBAL_COOLDOWN;
			setAction(Action.IDLE);
		}
	}

	/** The Last Name: his phylactery shatters, the chains are gone for good, and he is exposed to the end. */
	public void lastName(ServerLevel level) {
		attack = null;
		staggerTicks = 0;
		phase = Phase.LAST_NAME;
		phaseTicks = 0;
		ward = Ward.GONE;
		lastRaise = 0;
		scraped = null;
		for (Rune rune : Rune.values()) {
			NameStoneBlock.setGlow(level, stone(rune), Glow.DARK);
		}
		PhylacteryBlock.setCracked(level, phylactery(), true);
		BlockPos urn = phylactery();
		level.sendParticles(ParticleTypes.SOUL, urn.getX() + 0.5, urn.getY() + 0.6, urn.getZ() + 0.5, 40, 0.4, 0.5, 0.4, 0.08);
		setCrown(Crown.FLARE);
		globalCooldown = cooldown(GLOBAL_COOLDOWN);
		setAction(Action.IDLE);
		bossBar.setName(Component.translatable("message.jugcraft.crypt_lich.last_bar"));
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.GHAST_SCREAM, SoundSource.HOSTILE, 3.0F, 0.4F);
		level.playSound(null, urn, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 3.0F, 0.4F);
		tell(level, "message.jugcraft.crypt_lich.last_name");
	}

	// ---------------------------------------------------------------- his things

	/** Forgets what is gone. */
	private void prune(ServerLevel level) {
		dead.removeIf(id -> !(level.getEntity(id) instanceof RestlessDeadEntity risen) || !risen.isAlive());
		things.removeIf(id -> level.getEntity(id) == null);
	}

	/** Whoever hurt him or his dead has taken part. */
	public void took(ServerPlayer player, float amount) {
		damageBy.merge(player.getUUID(), amount, Float::sum);
	}

	/** His participants: everyone who hurt him or his dead, broke a seal or read a name back, and is still near his hall. */
	public List<ServerPlayer> participants(ServerLevel level) {
		List<ServerPlayer> out = new ArrayList<>();
		Vec3 centre = centre();
		for (ServerPlayer player : level.players()) {
			boolean part = damageBy.getOrDefault(player.getUUID(), 0.0F) > 0.0F || helpers.contains(player.getUUID());
			if (player.isAlive() && part && player.position().distanceTo(centre) <= 64.0) {
				out.add(player);
			}
		}
		return out;
	}

	// ---------------------------------------------------------------- abandoned, and defeated

	/**
	 * Left alone: he climbs back to his throne, healed; his dead crumble, his bolts and bones are gone, the seals mend, the
	 * names are cut dark again, the phylactery is whole, and who hurt him is forgotten.
	 */
	public void reset(ServerLevel level) {
		dismiss(level);
		for (Rune rune : Rune.values()) {
			if (level.isLoaded(seal(rune))) {
				GraveSealBlock.setBroken(level, seal(rune), false);
				NameStoneBlock.setGlow(level, stone(rune), Glow.DARK);
			}
		}
		if (level.isLoaded(phylactery())) {
			PhylacteryBlock.setCracked(level, phylactery(), false);
		}
		phase = Phase.WAITING;
		phaseTicks = 0;
		attack = null;
		abandoned = 0;
		staggerTicks = 0;
		foe = null;
		ward = Ward.CHAINED;
		wardTicks = 0;
		broken = 0;
		scraped = null;
		scrapedLitAt = NOT_LIT;
		damageBy.clear();
		helpers.clear();
		Arrays.fill(cooldowns, 0);
		setHealth(getMaxHealth());
		setCrown(Crown.DARK);
		noPhysics = true;
		Vec3 waiting = waiting();
		teleportTo(waiting.x, waiting.y, waiting.z);
		face(0.0F);
		setAction(Action.THRONE);
		bossBar.setName(Component.translatable("entity.jugcraft.crypt_lich"));
		bossBar.setVisible(false);
		tell(level, "message.jugcraft.crypt_lich.reset");
	}

	/** His dead crumble, and his bolts and bones are gone. */
	private void dismiss(ServerLevel level) {
		for (UUID id : List.copyOf(dead)) {
			if (level.getEntity(id) instanceof RestlessDeadEntity risen) {
				risen.crumble(level);
			}
		}
		dead.clear();
		for (UUID id : List.copyOf(things)) {
			Entity thing = level.getEntity(id);
			if (thing != null) {
				thing.discard();
			}
		}
		things.clear();
		lanes.clear();
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level) {
			defeated(level);
		}
	}

	/**
	 * He has fallen: his dead crumble and his things are gone, each participant gets their loot, his lair's instance ends
	 * (it closes once everyone has left), the soul-fire goes out down the lanes, and Grey Mist rises on the dais where his
	 * phylactery stood.
	 */
	private void defeated(ServerLevel level) {
		dismiss(level);
		CryptLichLoot.reward(level, this, participants(level));
		setCrown(Crown.DARK);
		for (Rune rune : Rune.values()) {
			NameStoneBlock.setGlow(level, stone(rune), Glow.DARK);
			laneFire(level, rune, ParticleTypes.SMOKE, 2);
		}
		LairInstance open = lairInstance(level);
		if (open != null) {
			Lairs.end(open);
			BlockState mist = JugcraftLairs.LAIR_EXIT.defaultBlockState().setValue(LairExitBlock.AXIS, Direction.Axis.X);
			for (BlockPos cell : CryptArchive.exitCells()) {
				level.setBlock(cell.offset(origin), mist, Block.UPDATE_ALL);
			}
		} else {
			PhylacteryBlock.setCracked(level, phylactery(), true);
		}
		bossBar.setVisible(false);
		tell(level, "message.jugcraft.crypt_lich.defeated");
	}

	/** Tells everyone near his hall. */
	void tell(ServerLevel level, String key) {
		Vec3 centre = centre();
		for (ServerPlayer player : level.players()) {
			if (player.position().distanceTo(centre) <= LEASH + 32.0) {
				player.sendSystemMessage(Component.translatable(key));
			}
		}
	}

	/** Tells everyone near his hall, naming {@code rune}'s name. */
	private void tellNamed(ServerLevel level, String key, Rune rune) {
		Vec3 centre = centre();
		for (ServerPlayer player : level.players()) {
			if (player.position().distanceTo(centre) <= LEASH + 32.0) {
				player.sendSystemMessage(Component.translatable(key, rune.display()));
			}
		}
	}

	// ---------------------------------------------------------------- what he is immune to, and his ward

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
		if (phase == Phase.WAKING || phase == Phase.ERASURE) {
			return false;
		}
		if (warded()) {
			turnAside(level, source);
			return false;
		}
		boolean hurt = super.hurtServer(level, source, amount);
		if (hurt && source.getEntity() instanceof ServerPlayer player) {
			took(player, amount);
		}
		return hurt;
	}

	/** A blow on him while his chains hold: they flare, and it does nothing; the striker is told why (now and then). */
	private void turnAside(ServerLevel level, DamageSource source) {
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 1.4, getZ(), 10, 0.5, 0.7, 0.5, 0.02);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_HIT, SoundSource.HOSTILE, 1.2F, 0.6F);
		if (source.getEntity() instanceof ServerPlayer player) {
			long now = level.getGameTime();
			Long last = wardedWords.get(player.getUUID());
			if (last == null || now - last >= WARDED_WORDS || now < last) {
				wardedWords.put(player.getUUID(), now);
				player.sendOverlayMessage(Component.translatable("message.jugcraft.crypt_lich.warded"));
			}
		}
	}

	/**
	 * His own Soul Bolt, struck back by {@code reflector}, reaches him: warded, it staggers him for {@value #BOLT_STAGGER}
	 * ticks and spoils his attack; exposed, it hurts him for {@value #REFLECTED_DAMAGE}. Returns whether it hurt him.
	 */
	public boolean boltReturned(ServerLevel level, SoulBoltEntity bolt, @Nullable Player reflector) {
		if (reflector instanceof ServerPlayer player) {
			helpers.add(player.getUUID());
		}
		if (!fighting()) {
			return false;
		}
		if (warded()) {
			stagger(BOLT_STAGGER);
			level.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1.6, getZ(), 16, 0.4, 0.5, 0.4, 0.04);
			return false;
		}
		DamageSource source = reflector == null ? damageSources().magic() : damageSources().thrown(bolt, reflector);
		return hurtServer(level, source, REFLECTED_DAMAGE);
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
		return phase == Phase.WAITING ? null : SoundEvents.WITHER_SKELETON_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.WITHER_SKELETON_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.WITHER_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.7F;
	}

	// ---------------------------------------------------------------- particles

	/** Particles along the line from {@code from} to {@code to}, one every {@code step} blocks. */
	static void line(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle, double step) {
		Vec3 way = to.subtract(from);
		double length = way.length();
		int points = Math.max(1, (int) Math.ceil(length / step));
		for (int i = 0; i <= points; i++) {
			Vec3 at = from.add(way.scale(i / (double) points));
			level.sendParticles(particle, at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0.0);
		}
	}

	// ---------------------------------------------------------------- saving and syncing

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ACTION, Action.THRONE.ordinal());
		builder.define(CROWN, Crown.DARK.ordinal());
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
		origin = input.read("origin", BlockPos.CODEC).orElse(BlockPos.containing(getX() - CryptArchive.WAIT_X, getY() - CryptArchive.WAIT_Y,
				getZ() - CryptArchive.WAIT_Z));
		try {
			phase = Phase.valueOf(input.getStringOr("phase", Phase.WAITING.name()));
		} catch (IllegalArgumentException e) {
			phase = Phase.WAITING;
		}
		if (phase == Phase.WAKING || phase == Phase.ERASURE) {
			// A passing moment is not resumed: he comes down to the floor, and fights on.
			landOnLoad = phase == Phase.WAKING;
			phase = phase == Phase.WAKING ? Phase.WARDED_KING : Phase.UNWRITTEN;
		}
		// His chains are not saved: loaded fighting, he forges them anew (the Last Name's stay gone).
		rebind = fighting();
		noPhysics = phase == Phase.WAITING;
		setCrown(phase == Phase.WAITING ? Crown.DARK : phase == Phase.LAST_NAME ? Crown.FLARE : Crown.LIT);
		bossBar.setVisible(phase != Phase.WAITING);
	}

	// ---------------------------------------------------------------- his animations

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<CryptLichEntity>("body", 4, test -> {
			CryptLichEntity lich = test.animatable();
			if (lich.isDeadOrDying()) {
				return test.setAndContinue(DEATH);
			}
			Action action = lich.action();
			return test.setAndContinue(action == Action.IDLE && test.isMoving() ? WALK : BODY[action.ordinal()]);
		}));
		controllers.add(new AnimationController<CryptLichEntity>("crown", 4,
				test -> test.setAndContinue(test.animatable().isDeadOrDying() ? FIRE[Crown.DARK.ordinal()] : FIRE[test.animatable().crown().ordinal()])));
		controllers.add(new AnimationController<CryptLichEntity>("hand", 2,
				test -> test.setAndContinue(test.animatable().action() == Action.BOLT_WINDUP ? HAND_SKULL : HAND_EMPTY)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
