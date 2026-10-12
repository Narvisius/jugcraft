package io.github.jimbozoomer.jugcraft.lair.tyrant;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.LoopType;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairBosses;
import io.github.jimbozoomer.jugcraft.lair.LairExitBlock;
import io.github.jimbozoomer.jugcraft.lair.LairInstance;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.SluiceGateBlock;
import io.github.jimbozoomer.jugcraft.lair.TroughStoneBlock;
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
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Cinder Tyrant (docs/features/cinder-tyrant.md): the Cinder Kiln's boss, a salamander lord the size of a cart,
 * armoured in plates of obsidian with magma glowing in their seams, a crest of obsidian spikes down his spine and a tail
 * ending in a club of basalt. He waits sunk in the crucible, only his crest over the slag, and wakes when someone who may
 * fight him steps down onto the bowl's floor or strikes him: he rises roaring from the slag and crawls out over the
 * crucible's rim, his health scaled for the party.
 *
 * <p>His rule: the heat is his. While his seams glow he is hot, and every blow on him does {@value #HOT_TAKEN} of its
 * damage. A Body Slam that lands in a flooded trough (a sluice turned by hand) quenches him for {@value #QUENCH_TICKS}
 * ticks: his seams go dark, blows do {@value #QUENCH_TAKEN} of theirs, and he crawls slower; then his seams flare back.
 * Phase 1, the Kiln: Tail Sweep, Ember Spit, Body Slam, Kiln Breath and Mantle Shed (his Cinderlings). At half health,
 * the Eruption: he leaps up onto the forge's lip and roars, unhurt, while the heat channel surges over its banks and two
 * Cinderlings crawl out of the slag. Phase 2, the Eruption: Cinder Rain and the Lava Wave besides, one sluice at a time
 * choked with slag (the choke moves every {@value #CHOKE_TICKS} ticks), and the channel surging every
 * {@value #SURGE_EVERY} ticks. Below a fifth, the Molten Heart: white-hot cores swell from his seams, every cooldown and
 * his pause between attacks are a third shorter, he spits {@value #HEART_GOBS} gobs and breathes wider.
 *
 * <p>His fire (the breath, the wave, the fire patches) is the mod's flame damage and burning, which Fire Resistance stops;
 * his blows (the sweep, the slam, the gobs' and cinders' impact, his Cinderlings' bites) it never does. Every attack has a
 * wind-up you can read, a strike and a recovery ({@link Attack}); the numbers are here, in one place, and in
 * tools/cinder_tyrant.py. Each participant (whoever hurt him or his Cinderlings) gets their own loot
 * ({@link CinderTyrantLoot}); left alone he sinks back into the crucible, healed, the troughs drain, the sluices reset and
 * the slag ebbs. Bound to a lair instance he vanishes with it; one with no instance (a test's) keeps to the kiln laid out
 * from the origin he was given. The only blocks he changes are the kiln's own (the surge's slag, the choke, the troughs'
 * water, the cooled crucible), and every one comes back.
 */
public class CinderTyrantEntity extends Monster implements GeoEntity {
	// ---------------------------------------------------------------- numbers (tools/cinder_tyrant.py)
	public static final float HEALTH = 440.0F;
	public static final double ARMOR = 10.0;
	public static final double LEASH = 30.0;
	public static final double PARTY_STEP = 0.5;
	public static final double PARTY_MAX = 2.5;
	public static final int WAKE_TICKS = 40;
	public static final int ABANDON_TICKS = 200;
	public static final int GLOBAL_COOLDOWN = 20;
	public static final double SPEED = 0.18;
	public static final double KEEP_DISTANCE = 3.0;
	public static final double STEP = 1.1;
	public static final float ERUPT_AT = 0.5F;
	public static final float HEART_AT = 0.2F;
	public static final double HEART_COOLDOWN = 0.6667;
	public static final double HOT_TAKEN = 0.5;
	public static final int QUENCH_TICKS = 160;
	public static final double QUENCH_TAKEN = 1.25;
	public static final double QUENCH_SPEED = 0.5;
	public static final double QUENCH_MARGIN = 0.25;
	public static final int REHEAT_TICKS = 20;
	public static final float SWEEP_DAMAGE = 10.0F;
	public static final double SWEEP_REACH = 4.0;
	public static final double SWEEP_FRONT = 45.0;
	public static final double SWEEP_KNOCKBACK = 1.0;
	public static final int SWEEP_BURN = 3;
	public static final int SPIT_GOBS = 3;
	public static final int HEART_GOBS = 5;
	public static final double SPIT_SPREAD = 3.0;
	public static final float SLAM_DAMAGE = 12.0F;
	public static final double SLAM_RADIUS = 3.0;
	public static final double SLAM_PUSH_RADIUS = 5.0;
	public static final double SLAM_KNOCKBACK = 1.4;
	public static final double SLAM_RISE = 3.0;
	public static final double BREATH_REACH = 8.0;
	public static final double BREATH_HALF_ANGLE = 30.0;
	public static final double HEART_BREATH_HALF_ANGLE = 45.0;
	public static final float BREATH_DAMAGE = 3.0F;
	public static final int BREATH_EVERY = 10;
	public static final int BREATH_BURN = 3;
	public static final int SHED = 2;
	public static final int MAX_CINDERLINGS = 4;
	public static final int ERUPT_TICKS = 60;
	public static final int ERUPT_CINDERLINGS = 2;
	public static final int ERUPT_LEAP = 20;
	public static final int SURGE_WARN = 20;
	public static final int SURGE_TICKS = 80;
	public static final int SURGE_SPILL = 2;
	public static final int SURGE_EVERY = 600;
	public static final int CHOKE_TICKS = 400;
	public static final int CINDERS = 6;
	public static final double CINDER_AREA = 3.0;
	public static final int CINDER_EVERY = 4;
	public static final double WAVE_REACH = 12.0;
	public static final float WAVE_DAMAGE = 8.0F;
	public static final int WAVE_BURN = 4;
	public static final int PATCH_TICKS = 80;
	public static final double PATCH_RADIUS = 1.0;
	public static final float PATCH_DAMAGE = 1.0F;
	public static final int PATCH_EVERY = 10;
	public static final int PATCH_BURN = 2;
	public static final int MAX_PATCHES = 16;
	public static final int EXPERIENCE = 300;
	/** How high a leap (out of the crucible, onto the lip and off it) rises over the straight line. */
	private static final double LEAP_RISE = 2.0;
	/** How far from the bowl's centre he goes for his foe: the bowl and its ring, short of the wall. */
	private static final double ROAM = CinderKiln.BOWL_RADIUS + 3.0;

	/**
	 * His attacks: wind-up, the active part and recovery in ticks, the cooldown, the target's nearest and farthest distance,
	 * and the phases he uses it in (the Kiln, the Eruption, the Molten Heart). The wind-up is what players read; the strike
	 * lands as it ends.
	 */
	public enum Attack {
		TAIL_SWEEP(16, 4, 10, 50, 0.0, 4.5, true, true, true),
		EMBER_SPIT(24, 1, 12, 120, 4.0, 24.0, true, true, true),
		BODY_SLAM(20, 16, 14, 180, 3.0, 16.0, true, true, true),
		KILN_BREATH(30, 30, 10, 160, 0.0, 8.0, true, true, true),
		MANTLE_SHED(40, 1, 10, 300, 0.0, 40.0, true, true, true),
		CINDER_RAIN(24, 24, 10, 160, 0.0, 30.0, false, true, true),
		LAVA_WAVE(30, 40, 10, 220, 0.0, 12.0, false, true, true);

		public final int windup;
		public final int active;
		public final int recovery;
		public final int cooldown;
		public final double near;
		public final double far;
		public final boolean kiln;
		public final boolean eruption;
		public final boolean heart;

		Attack(int windup, int active, int recovery, int cooldown, double near, double far, boolean kiln, boolean eruption, boolean heart) {
			this.windup = windup;
			this.active = active;
			this.recovery = recovery;
			this.cooldown = cooldown;
			this.near = near;
			this.far = far;
			this.kiln = kiln;
			this.eruption = eruption;
			this.heart = heart;
		}

		public int length() {
			return windup + active + recovery;
		}
	}

	/** The fight's phases. */
	public enum Phase {
		WAITING, WAKING, KILN, ERUPTING, ERUPTION, HEART
	}

	/** What his body is doing, synced for his animations. */
	public enum Action {
		SUNK, RISE, LEAP, IDLE, SWEEP_WINDUP, SWEEP, SPIT_WINDUP, SPIT, SLAM_WINDUP, SLAM, BREATH_WINDUP, BREATH, SHED_WINDUP, SHED,
		ROAR, WAVE_WINDUP, WAVE
	}

	/** His heat, synced for his seams: hot, quenched dark, or flaring back. */
	public enum Heat {
		HOT, QUENCHED, REHEATING
	}

	/** A patch of fire where a gob or a cinder burst, and the game time it goes out. */
	public record FirePatch(Vec3 at, long until) {
	}

	private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(CinderTyrantEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> HEAT = SynchedEntityData.defineId(CinderTyrantEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> MOLTEN = SynchedEntityData.defineId(CinderTyrantEntity.class, EntityDataSerializers.BOOLEAN);
	private static final RawAnimation[] BODY = new RawAnimation[Action.values().length];
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.cinder_tyrant.walk");
	private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.cinder_tyrant.death");
	private static final RawAnimation HEAT_HOT = RawAnimation.begin().thenLoop("animation.cinder_tyrant.heat_hot");
	private static final RawAnimation HEAT_QUENCHED = RawAnimation.begin().thenLoop("animation.cinder_tyrant.heat_quenched");
	private static final RawAnimation HEAT_REHEAT = RawAnimation.begin().thenPlayAndHold("animation.cinder_tyrant.heat_reheat");
	private static final RawAnimation HEAT_HEART = RawAnimation.begin().thenLoop("animation.cinder_tyrant.heat_heart");

	static {
		String prefix = "animation.cinder_tyrant.";
		BODY[Action.SUNK.ordinal()] = RawAnimation.begin().thenLoop(prefix + "sunk");
		BODY[Action.RISE.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "rise");
		BODY[Action.LEAP.ordinal()] = RawAnimation.begin().thenLoop(prefix + "leap");
		BODY[Action.IDLE.ordinal()] = RawAnimation.begin().thenLoop(prefix + "idle");
		BODY[Action.SWEEP_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "sweep_windup");
		BODY[Action.SWEEP.ordinal()] = RawAnimation.begin().then(prefix + "sweep", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.SPIT_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "spit_windup");
		BODY[Action.SPIT.ordinal()] = RawAnimation.begin().then(prefix + "spit", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.SLAM_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "slam_windup");
		BODY[Action.SLAM.ordinal()] = RawAnimation.begin().then(prefix + "slam", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.BREATH_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "breath_windup");
		BODY[Action.BREATH.ordinal()] = RawAnimation.begin().thenLoop(prefix + "breath");
		BODY[Action.SHED_WINDUP.ordinal()] = RawAnimation.begin().thenLoop(prefix + "shed_windup");
		BODY[Action.SHED.ordinal()] = RawAnimation.begin().then(prefix + "shed", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.ROAR.ordinal()] = RawAnimation.begin().thenLoop(prefix + "roar");
		BODY[Action.WAVE_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "wave_windup");
		BODY[Action.WAVE.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "wave");
	}

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private final ServerBossEvent bossBar = new ServerBossEvent(UUID.randomUUID(), Component.translatable("entity.jugcraft.cinder_tyrant"),
			BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
	/** The lair instance he belongs to (null for one put down by a test or a command), and the kiln's origin. */
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
	/** Ticks left quenched, and of his seams flaring back after. */
	private int quenched;
	private int reheating;
	private final Map<UUID, Float> damageBy = new HashMap<>();
	private final List<UUID> cinderlings = new ArrayList<>();
	/** His spat and fallen things (gobs, cinders), gone with him. */
	private final List<UUID> things = new ArrayList<>();
	private final List<FirePatch> patches = new ArrayList<>();
	/** Where an Ember Spit's gobs or a Cinder Rain's cinders will land. */
	private final List<Vec3> marks = new ArrayList<>();
	/** A leap under way: from and to. */
	private Vec3 leapFrom = Vec3.ZERO;
	private Vec3 leapTo = Vec3.ZERO;
	/** His Lava Wave: where it rolls out from, how far it has rolled, and whom it has passed. */
	private Vec3 waveFrom = Vec3.ZERO;
	private double waveReach;
	private final Set<UUID> waveHit = new HashSet<>();
	/** The channel's surge: when it spills (game time, or -1), when it ebbs, and the cells it has spilled over. */
	private long surgeSpills = -1L;
	private long surgeEbbs = -1L;
	private final List<BlockPos> surged = new ArrayList<>();
	/** The Eruption's clocks: the next surge, the next move of the choke, and which sluice is choked (-1: none). */
	private long nextSurge = -1L;
	private long nextChoke = -1L;
	private int choked = -1;
	/** Set when he is loaded from a save: the slag ebbs from what it spilled over and the sluices clear on his first tick. */
	private boolean restoreOnLoad;
	/** Set when he is loaded in the middle of a leap (waking, or the Eruption): he lands where it would have ended. */
	private boolean landOnLoad;

	public CinderTyrantEntity(EntityType<? extends CinderTyrantEntity> type, Level level) {
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
	 * Sinks him in the crucible of {@code instance}'s Cinder Kiln. Called as the instance is placed. Placing the template
	 * puts back only its own blocks, so the Grey Mist a fallen Tyrant opened in this slot before still stands where the
	 * template has none: it goes first.
	 */
	public static CinderTyrantEntity summon(ServerLevel level, LairInstance instance) {
		BlockPos origin = instance.origin();
		for (BlockPos at : exitCells(centre(origin))) {
			if (level.getBlockState(at).is(JugcraftLairs.LAIR_EXIT)) {
				level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		return summon(level, origin, instance);
	}

	/** Sinks him in the crucible of the kiln laid out from {@code origin} (tests pass no instance). */
	public static CinderTyrantEntity summon(ServerLevel level, BlockPos origin, @Nullable LairInstance instance) {
		CinderTyrantEntity tyrant = new CinderTyrantEntity(JugcraftTyrant.CINDER_TYRANT, level);
		tyrant.origin = origin.immutable();
		if (instance != null) {
			tyrant.instance = instance.id;
			tyrant.slot = instance.slot;
		}
		tyrant.noPhysics = true;
		Vec3 sunk = tyrant.sunk();
		tyrant.snapTo(sunk.x, sunk.y, sunk.z, 0.0F, 0.0F);
		tyrant.face(0.0F);
		level.addFreshEntity(tyrant);
		return tyrant;
	}

	// ---------------------------------------------------------------- where things are

	/** The bowl's centre, where it is stood on, for the kiln laid out from {@code origin}. */
	public static Vec3 centre(BlockPos origin) {
		return CinderKiln.centre().add(origin.getX(), origin.getY(), origin.getZ());
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

	/** Where he waits sunk in the crucible. */
	public Vec3 sunk() {
		return at(CinderKiln.sunk());
	}

	/** The height of the bowl's floor, where he and the players stand. */
	public double floorY() {
		return origin.getY() + CinderKiln.BOWL + 1;
	}

	/** Where his jaws are, which his gobs and his breath leave from. */
	public Vec3 jaws() {
		return position().add(facing().scale(MagmaGobEntity.MOUTH_AHEAD)).add(0.0, MagmaGobEntity.MOUTH_UP, 0.0);
	}

	/** The sluices' wheels in his kiln: west, east and south. */
	public List<BlockPos> wheels() {
		return CinderKiln.WHEELS.stream().map(wheel -> wheel.offset(origin)).toList();
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

	public Heat heat() {
		int id = entityData.get(HEAT);
		return id >= 0 && id < Heat.values().length ? Heat.values()[id] : Heat.HOT;
	}

	private void setHeat(Heat heat) {
		entityData.set(HEAT, heat.ordinal());
	}

	/** How many more ticks he stays quenched (0 while hot). */
	public int quenchedTicks() {
		return quenched;
	}

	/** Whether he is in the Molten Heart. */
	public boolean molten() {
		return entityData.get(MOLTEN);
	}

	public @Nullable UUID instanceId() {
		return instance;
	}

	/** Which sluice is choked with slag (0 west, 1 east, 2 south), or -1. */
	public int choked() {
		return choked;
	}

	/** The cells the channel's surge has spilled over, now. */
	public List<BlockPos> surged() {
		return List.copyOf(surged);
	}

	/** The fire patches burning now. */
	public List<FirePatch> patches() {
		return List.copyOf(patches);
	}

	/** Where his Ember Spit's gobs or his Cinder Rain's cinders will land. */
	public List<Vec3> marks() {
		return List.copyOf(marks);
	}

	/** Whether he is fighting (on the floor, not waiting, waking or erupting). */
	public boolean fighting() {
		return phase == Phase.KILN || phase == Phase.ERUPTION || phase == Phase.HEART;
	}

	/** Damage he deals: the attack's own, times {@code lairs.boss_damage} ({@link LairBosses}). */
	public static float damage(float base) {
		return LairBosses.damage(base);
	}

	/** His health for a party of {@code players}: half as much again for each beyond the first, at most 2.5 times. */
	public static double partyScale(int players) {
		return LairBosses.partyScale(players, PARTY_STEP, PARTY_MAX);
	}

	/** A cooldown as it is in this phase: a third shorter in the Molten Heart. */
	public int cooldown(int ticks) {
		return molten() ? (int) Math.round(ticks * HEART_COOLDOWN) : ticks;
	}

	/** The share of a blow's damage he takes: half while hot, a quarter more while quenched. */
	public double taken() {
		return quenched > 0 ? QUENCH_TAKEN : HOT_TAKEN;
	}

	/** His fire: the mod's flame damage (a fire damage type, which Fire Resistance stops), from him. */
	private DamageSource flame(ServerLevel level) {
		return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(JugcraftGuns.FLAME_DAMAGE), this, this);
	}

	// ---------------------------------------------------------------- the server's tick

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (!bound(level)) {
			discard();
			return;
		}
		if (restoreOnLoad) {
			restoreOnLoad = false;
			ebb(level);
			clearSluices(level);
		}
		if (landOnLoad) {
			landOnLoad = false;
			leapTo = at(CinderKiln.out());
			landed(level);
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
		cool(level);
		burnPatches(level);
		surging(level);
		switch (phase) {
			case WAITING -> waiting(level);
			case WAKING -> waking(level);
			case KILN, ERUPTION, HEART -> {
				if (phase != Phase.KILN) {
					eruptionClocks(level);
				}
				fight(level);
			}
			case ERUPTING -> erupting(level);
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
		Vec3 sunk = sunk();
		if (position().distanceToSqr(sunk) > 0.01) {
			setPos(sunk.x, sunk.y, sunk.z);
		}
		face(0.0F);
		setAction(Action.SUNK);
		if (tickCount % 20 == 0) {
			level.sendParticles(ParticleTypes.LAVA, getX(), floorY(), getZ() + 1.0, 1, 1.0, 0.0, 2.0, 0.0);
		}
		if (tickCount % 10 == 0 && intruder(level) != null) {
			wake(level);
		}
	}

	/** Someone who may fight him, standing on the bowl's floor. */
	private @Nullable Player intruder(ServerLevel level) {
		Vec3 centre = centre();
		for (Player player : level.players()) {
			double dx = player.getX() - centre.x;
			double dz = player.getZ() - centre.z;
			if (LairBosses.eligible(player) && dx * dx + dz * dz <= CinderKiln.BOWL_RADIUS * CinderKiln.BOWL_RADIUS
					&& Math.abs(player.getY() - centre.y) < 1.5) {
				return player;
			}
		}
		return null;
	}

	/** He rises roaring from the slag: his health scaled for the players near the bowl, his bar shown. */
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
		setAction(Action.RISE);
		bossBar.setVisible(true);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 3.0F, 0.6F);
		tell(level, "message.jugcraft.cinder_tyrant.wakes");
	}

	/** Waking: half the time rising out of the slag, roaring, then a crawl out over the crucible's rim. */
	private void waking(ServerLevel level) {
		noPhysics = true;
		int half = WAKE_TICKS / 2;
		if (phaseTicks <= half) {
			Vec3 sunk = sunk();
			double y = sunk.y + (floorY() - 0.1 - sunk.y) * Math.min(1.0, phaseTicks / (double) half);
			setDeltaMovement(Vec3.ZERO);
			setPos(sunk.x, y, sunk.z);
			if (phaseTicks % 2 == 0) {
				level.sendParticles(ParticleTypes.LAVA, getX(), floorY(), getZ(), 4, 1.5, 0.1, 2.0, 0.0);
				level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), floorY() + 0.5, getZ(), 4, 1.5, 0.4, 2.0, 0.02);
			}
			if (phaseTicks == half) {
				setAction(Action.LEAP);
				leapFrom = position();
				leapTo = at(CinderKiln.out());
			}
			return;
		}
		leap(phaseTicks - half - 1, WAKE_TICKS - half);
		if (phaseTicks >= WAKE_TICKS) {
			landed(level);
			phase = Phase.KILN;
			phaseTicks = 0;
			globalCooldown = GLOBAL_COOLDOWN;
			setAction(Action.IDLE);
		}
	}

	/** One tick of a leap from {@link #leapFrom} to {@link #leapTo}, {@code t} ticks into its {@code length}. */
	private void leap(int t, int length) {
		noPhysics = true;
		double share = Math.min(1.0, (t + 1) / (double) length);
		Vec3 along = leapFrom.add(leapTo.subtract(leapFrom).scale(share));
		Vec3 goal = along.add(0.0, LEAP_RISE * 4.0 * share * (1.0 - share), 0.0);
		setDeltaMovement(Vec3.ZERO);
		setPos(goal.x, goal.y, goal.z);
		Vec3 to = leapTo.subtract(leapFrom);
		if (to.x * to.x + to.z * to.z > 1.0E-4) {
			face((float) (Math.toDegrees(Math.atan2(to.z, to.x)) - 90.0));
		}
	}

	/** He comes down on the floor with a thud, and is solid again. */
	private void landed(ServerLevel level) {
		teleportTo(leapTo.x, leapTo.y, leapTo.z);
		noPhysics = false;
		level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.2, getZ(), 12, 1.5, 0.2, 1.5, 0.0);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.3, getZ(), 12, 1.5, 0.2, 1.5, 0.02);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.HOSTILE, 1.5F, 0.6F);
	}

	/** Players near enough to his bowl to be fighting (or about to). */
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
		keepToTheKiln(level);
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
		if (attack == null && phase == Phase.KILN && getHealth() <= getMaxHealth() * ERUPT_AT) {
			startEruption(level);
			return;
		}
		if (attack == null && phase == Phase.ERUPTION && getHealth() <= getMaxHealth() * HEART_AT) {
			startHeart(level);
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

	/** Strayed farther than his leash from the bowl's centre, or fallen below it, he bounds back to the bowl. */
	private void keepToTheKiln(ServerLevel level) {
		Vec3 centre = centre();
		double dx = getX() - centre.x;
		double dz = getZ() - centre.z;
		if (dx * dx + dz * dz <= LEASH * LEASH && getY() > floorY() - 2.0) {
			return;
		}
		level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0, getZ(), 20, 1.0, 0.8, 1.0, 0.03);
		attack = null;
		noPhysics = false;
		teleportTo(centre.x, floorY(), centre.z);
		level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.5, getZ(), 12, 1.0, 0.4, 1.0, 0.0);
	}

	/** His target: the one he has while they are still in reach of his bowl, else the nearest who may fight him. */
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

	/** His pace: slower while quenched. */
	public double speed() {
		return quenched > 0 ? SPEED * QUENCH_SPEED : SPEED;
	}

	/** Crawls toward a point {@value #KEEP_DISTANCE} blocks short of {@code target}. */
	private void approach(Player target) {
		Vec3 to = target.position().subtract(position());
		double flat = Math.sqrt(to.x * to.x + to.z * to.z);
		Vec3 goal = flat > KEEP_DISTANCE + 0.5
				? new Vec3(target.getX() - to.x / flat * KEEP_DISTANCE, floorY(), target.getZ() - to.z / flat * KEEP_DISTANCE)
				: new Vec3(getX(), floorY(), getZ());
		move(goal, speed());
	}

	/** Moves toward {@code goal} at {@code speed}, pressed down onto the floor (he steps up onto rims and shelves) and kept in the bowl. */
	private void move(Vec3 goal, double speed) {
		Vec3 centre = centre();
		double x = goal.x;
		double z = goal.z;
		double dx = x - centre.x;
		double dz = z - centre.z;
		double flat = Math.sqrt(dx * dx + dz * dz);
		if (flat > ROAM) {
			x = centre.x + dx / flat * ROAM;
			z = centre.z + dz / flat * ROAM;
		}
		Vec3 to = new Vec3(x - getX(), 0.0, z - getZ());
		double length = to.length();
		Vec3 step = length < 0.05 ? Vec3.ZERO : to.scale(Math.min(speed, length) / length);
		setDeltaMovement(step.x, Math.min(0.0, floorY() - getY()) - 0.2, step.z);
	}

	/** Holds still where he is, pressed down onto the floor. */
	private void hold() {
		setDeltaMovement(0.0, Math.min(0.0, floorY() - getY()) - 0.2, 0.0);
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
				case KILN -> candidate.kiln;
				case ERUPTION -> candidate.eruption;
				case HEART -> candidate.heart;
				default -> false;
			};
			if (!phaseAllows || cooldowns[candidate.ordinal()] > 0 || distance < candidate.near || distance > candidate.far) {
				continue;
			}
			if (candidate == Attack.MANTLE_SHED && cinderlings.size() >= MAX_CINDERLINGS) {
				continue;
			}
			ready.add(candidate);
			if (candidate == Attack.TAIL_SWEEP) {
				ready.add(candidate);  // close in, his tail comes most often
				ready.add(candidate);
			}
		}
		return ready.isEmpty() ? null : ready.get(getRandom().nextInt(ready.size()));
	}

	/** Starts {@code next} on {@code target}: its wind-up begins, from the floor (a slam cut short in the air lands). */
	public void begin(ServerLevel level, Attack next, Player target) {
		attack = next;
		attackTicks = 0;
		noPhysics = false;
		foe = target.getUUID();
		cooldowns[next.ordinal()] = cooldown(next.cooldown);
		hold();
		face(target);
		switch (next) {
			case TAIL_SWEEP -> {
				// He coils sideways to his foe, his tail raised, to swing it round through them.
				face(getYRot() + (getRandom().nextBoolean() ? 90.0F : -90.0F));
				setAction(Action.SWEEP_WINDUP);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_AMBIENT, SoundSource.HOSTILE, 2.0F, 0.5F);
			}
			case EMBER_SPIT -> {
				setAction(Action.SPIT_WINDUP);
				spread(target, molten() ? HEART_GOBS : SPIT_GOBS, SPIT_SPREAD);
			}
			case BODY_SLAM -> {
				setAction(Action.SLAM_WINDUP);
				leapTo = landing(target.position());
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_AMBIENT, SoundSource.HOSTILE, 2.0F, 0.7F);
			}
			case KILN_BREATH -> setAction(Action.BREATH_WINDUP);
			case MANTLE_SHED -> {
				setAction(Action.SHED_WINDUP);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.HOSTILE, 2.0F, 0.5F);
			}
			case CINDER_RAIN -> {
				setAction(Action.ROAR);
				spread(target, CINDERS, CINDER_AREA);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2.0F, 0.8F);
			}
			case LAVA_WAVE -> {
				setAction(Action.WAVE_WINDUP);
				waveFrom = new Vec3(getX(), floorY(), getZ());
				waveReach = 0.0;
				waveHit.clear();
			}
		}
	}

	/** The marks of an Ember Spit or a Cinder Rain: the first under {@code target}, the rest scattered within {@code area}. */
	private void spread(Player target, int count, double area) {
		marks.clear();
		marks.add(new Vec3(target.getX(), floorY(), target.getZ()));
		for (int i = 1; i < count; i++) {
			double angle = getRandom().nextDouble() * Math.PI * 2.0;
			double reach = (0.4 + 0.6 * Math.sqrt(getRandom().nextDouble())) * area;
			marks.add(new Vec3(target.getX() + Math.cos(angle) * reach, floorY(), target.getZ() + Math.sin(angle) * reach));
		}
	}

	private void perform(ServerLevel level, Player target) {
		Attack current = attack;
		if (current == null) {
			return;
		}
		int t = attackTicks;
		if (current != Attack.BODY_SLAM) {
			hold();
		}
		switch (current) {
			case TAIL_SWEEP -> {
				if (t < current.windup) {
					if (t % 4 == 0) {
						Vec3 tail = position().subtract(facing().scale(3.5)).add(0.0, 1.2, 0.0);
						level.sendParticles(ParticleTypes.LAVA, tail.x, tail.y, tail.z, 2, 0.4, 0.3, 0.4, 0.0);
					}
				} else if (t == current.windup) {
					setAction(Action.SWEEP);
					sweep(level);
				}
			}
			case EMBER_SPIT -> {
				if (t < current.windup) {
					face(target);
					if (t % 4 == 0) {
						glowMarks(level);
						Vec3 jaws = jaws();
						level.sendParticles(ParticleTypes.FLAME, jaws.x, jaws.y, jaws.z, 2, 0.2, 0.2, 0.2, 0.01);
					}
				} else if (t == current.windup) {
					setAction(Action.SPIT);
					for (Vec3 mark : marks) {
						things.add(MagmaGobEntity.spit(level, this, mark).getUUID());
					}
					level.playSound(null, getX(), getY(), getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.0F, 0.5F);
				}
			}
			case BODY_SLAM -> bodySlam(level, t);
			case KILN_BREATH -> kilnBreath(level, target, t);
			case MANTLE_SHED -> {
				if (t < current.windup) {
					if (t % 3 == 0) {
						level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1.6, getZ(), 6, 1.2, 0.4, 1.6, 0.02);
						level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 1.6, getZ(), 2, 1.2, 0.4, 1.6, 0.0);
					}
				} else if (t == current.windup) {
					setAction(Action.SHED);
					shed(level, SHED);
				}
			}
			case CINDER_RAIN -> cinderRain(level, t - current.windup);
			case LAVA_WAVE -> lavaWave(level, t);
		}
		if (attack == current && attackTicks >= current.length()) {
			attack = null;
			attackTicks = 0;
			globalCooldown = cooldown(GLOBAL_COOLDOWN);
			Action action = action();
			if (action != Action.SWEEP && action != Action.SPIT && action != Action.SLAM && action != Action.SHED) {
				setAction(Action.IDLE);
			}
		}
	}

	/** The glowing marks on the floor where gobs or cinders will land. */
	private void glowMarks(ServerLevel level) {
		for (Vec3 mark : marks) {
			level.sendParticles(ParticleTypes.FLAME, mark.x, mark.y + 0.05, mark.z, 3, 0.35, 0.0, 0.35, 0.0);
		}
	}

	/**
	 * Tail Sweep: every player within {@value #SWEEP_REACH} blocks round his back and flanks (not the {@value #SWEEP_FRONT}
	 * degrees either side of his head) takes {@value #SWEEP_DAMAGE} damage, is knocked away, and burns for
	 * {@value #SWEEP_BURN} seconds.
	 */
	public int sweep(ServerLevel level) {
		int hit = 0;
		Vec3 facing = facing();
		for (ServerPlayer player : nearby(level)) {
			Vec3 to = player.position().subtract(position());
			double flat = Math.sqrt(to.x * to.x + to.z * to.z);
			if (flat > SWEEP_REACH + getBbWidth() / 2.0 + player.getBbWidth() / 2.0 || Math.abs(to.y) > 3.0) {
				continue;
			}
			double cos = flat < 1.0E-3 ? -1.0 : (to.x * facing.x + to.z * facing.z) / flat;
			if (cos > Math.cos(Math.toRadians(SWEEP_FRONT))) {
				continue;  // before his head: his tail does not reach there
			}
			if (player.hurtServer(level, damageSources().mobAttack(this), damage(SWEEP_DAMAGE))) {
				hit++;
				player.igniteForSeconds(SWEEP_BURN);
				push(player, flat < 1.0E-3 ? facing.scale(-1.0) : new Vec3(to.x / flat, 0.0, to.z / flat), SWEEP_KNOCKBACK, 0.3);
			}
		}
		Vec3 behind = position().subtract(facing.scale(2.0)).add(0.0, 1.0, 0.0);
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, behind.x, behind.y, behind.z, 3, 1.5, 0.2, 1.5, 0.0);
		level.sendParticles(ParticleTypes.FLAME, behind.x, behind.y, behind.z, 12, 2.0, 0.3, 2.0, 0.02);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5F, 0.5F);
		return hit;
	}

	/** Pushes {@code player} along {@code way} (flat) by {@code strength}, and up by {@code up}, and tells their client. */
	static void push(ServerPlayer player, Vec3 way, double strength, double up) {
		player.push(way.x * strength, up, way.z * strength);
		player.connection.send(new ClientboundSetEntityMotionPacket(player));
	}

	/** Where his slam comes down: where the foe stood, kept within the bowl he roams. */
	private Vec3 landing(Vec3 at) {
		Vec3 centre = centre();
		double dx = at.x - centre.x;
		double dz = at.z - centre.z;
		double flat = Math.sqrt(dx * dx + dz * dz);
		double reach = Math.min(flat, ROAM - 1.0);
		return flat < 1.0E-3 ? new Vec3(centre.x, floorY(), centre.z)
				: new Vec3(centre.x + dx / flat * reach, floorY(), centre.z + dz / flat * reach);
	}

	/** Where his Body Slam will land, while it winds up and flies. */
	public Vec3 slamLanding() {
		return leapTo;
	}

	/**
	 * Body Slam, {@code t} ticks into it: he rears up and his plates flare through the wind-up, the ground glowing where he
	 * will land; then a leap onto where his foe stood; then he comes down ({@link #slamLands}).
	 */
	private void bodySlam(ServerLevel level, int t) {
		Attack slam = Attack.BODY_SLAM;
		if (t < slam.windup) {
			hold();
			if (t % 4 == 0) {
				ring(level, leapTo, SLAM_RADIUS, ParticleTypes.FLAME, 20);
				level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 2.0, getZ(), 6, 1.2, 0.4, 1.6, 0.02);
			}
			if (t == slam.windup - 1) {
				leapFrom = position();
				setAction(Action.SLAM);
			}
		} else if (t < slam.windup + slam.active) {
			int into = t - slam.windup;
			double share = Math.min(1.0, (into + 1) / (double) slam.active);
			Vec3 along = leapFrom.add(leapTo.subtract(leapFrom).scale(share));
			noPhysics = true;
			setDeltaMovement(Vec3.ZERO);
			setPos(along.x, along.y + SLAM_RISE * 4.0 * share * (1.0 - share), along.z);
		} else if (t == slam.windup + slam.active) {
			teleportTo(leapTo.x, leapTo.y, leapTo.z);
			noPhysics = false;
			slamLands(level);
		} else {
			hold();
		}
	}

	/**
	 * He lands: {@value #SLAM_DAMAGE} damage within {@value #SLAM_RADIUS} blocks and a knockback within
	 * {@value #SLAM_PUSH_RADIUS}; and if he came down in a flooded trough, the water bursts into steam and he is quenched.
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
		level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.3, getZ(), 20, 1.6, 0.2, 1.6, 0.0);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.3, getZ(), 16, 2.0, 0.2, 2.0, 0.03);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.HOSTILE, 3.0F, 0.5F);
		if (inFloodedTrough(level)) {
			quench(level);
		}
		return hit;
	}

	/** Whether any trough stone under him (his feet, and {@value #QUENCH_MARGIN} round them) is flooded. */
	public boolean inFloodedTrough(ServerLevel level) {
		AABB box = getBoundingBox().inflate(QUENCH_MARGIN, 0.0, QUENCH_MARGIN);
		int y = origin.getY() + CinderKiln.BOWL;
		for (int x = (int) Math.floor(box.minX); x <= (int) Math.floor(box.maxX); x++) {
			for (int z = (int) Math.floor(box.minZ); z <= (int) Math.floor(box.maxZ); z++) {
				BlockState state = level.getBlockState(new BlockPos(x, y, z));
				if (state.getBlock() instanceof TroughStoneBlock && state.getValue(TroughStoneBlock.FLOODED)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Kiln Breath, {@code t} ticks into it: he draws in, and a glowing sector spreads on the floor before him; then a blast of
	 * kiln heat through it, {@value #BREATH_DAMAGE} fire damage every {@value #BREATH_EVERY} ticks to every player in it, who
	 * burns.
	 */
	private void kilnBreath(ServerLevel level, Player target, int t) {
		Attack breath = Attack.KILN_BREATH;
		Vec3 facing = facing();
		double half = Math.toRadians(breathHalfAngle());
		if (t < breath.windup) {
			if (t < breath.windup / 3) {
				face(target);
			}
			if (t % 3 == 0) {
				double reach = BREATH_REACH * Math.min(1.0, (t + 3) / (double) breath.windup);
				for (int i = 0; i < 6; i++) {
					double along = 1.5 + getRandom().nextDouble() * (reach - 1.5);
					double angle = (getRandom().nextDouble() * 2.0 - 1.0) * half;
					Vec3 way = rotate(facing, angle);
					level.sendParticles(ParticleTypes.FLAME, getX() + way.x * along, floorY() + 0.05, getZ() + way.z * along, 1, 0.1, 0.0, 0.1, 0.0);
				}
			}
			if (t == breath.windup - 1) {
				setAction(Action.BREATH);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.0F, 0.4F);
			}
			return;
		}
		if (t >= breath.windup + breath.active) {
			setAction(Action.IDLE);
			return;
		}
		Vec3 jaws = jaws();
		for (int i = 0; i < 4; i++) {
			double along = 1.0 + getRandom().nextDouble() * BREATH_REACH;
			Vec3 way = rotate(facing, (getRandom().nextDouble() * 2.0 - 1.0) * half);
			level.sendParticles(ParticleTypes.FLAME, jaws.x + way.x * along, jaws.y - along * 0.08, jaws.z + way.z * along, 2, along * 0.1, 0.15,
					along * 0.1, 0.01);
			level.sendParticles(ParticleTypes.LARGE_SMOKE, jaws.x + way.x * along, jaws.y, jaws.z + way.z * along, 1, 0.2, 0.2, 0.2, 0.01);
		}
		if ((t - breath.windup) % BREATH_EVERY == 0) {
			breathe(level);
		}
	}

	/** How wide his breath sweeps either side of his head: wider in the Molten Heart. */
	public double breathHalfAngle() {
		return molten() ? HEART_BREATH_HALF_ANGLE : BREATH_HALF_ANGLE;
	}

	/** One pulse of his breath: every player in the sector takes {@value #BREATH_DAMAGE} fire damage and burns. */
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
			if (cos < Math.cos(Math.toRadians(breathHalfAngle()))) {
				continue;
			}
			if (player.hurtServer(level, flame(level), damage(BREATH_DAMAGE))) {
				hit++;
			}
			player.igniteForSeconds(BREATH_BURN);
		}
		return hit;
	}

	private static Vec3 rotate(Vec3 way, double radians) {
		double cos = Math.cos(radians);
		double sin = Math.sin(radians);
		return new Vec3(way.x * cos - way.z * sin, 0.0, way.x * sin + way.z * cos);
	}

	/** Up to {@code count} Cinderlings crawl off him, never more than {@value #MAX_CINDERLINGS} at once. */
	public int shed(ServerLevel level, int count) {
		int out = 0;
		for (int i = 0; i < count && cinderlings.size() < MAX_CINDERLINGS; i++) {
			double angle = getRandom().nextDouble() * Math.PI * 2.0;
			Vec3 at = new Vec3(getX() + Math.cos(angle) * 2.2, floorY(), getZ() + Math.sin(angle) * 2.2);
			cinderlings.add(CinderlingEntity.crawlOut(level, this, at).getUUID());
			out++;
		}
		return out;
	}

	/**
	 * Cinder Rain, {@code t} ticks after its wind-up: the marks glow round his foe through the wind-up; then over the active
	 * ticks the cinders fall one by one from the vent ({@link FallingCinderEntity}).
	 */
	private void cinderRain(ServerLevel level, int t) {
		if (t < 0) {
			if ((t + Attack.CINDER_RAIN.windup) % 4 == 0) {
				glowMarks(level);
			}
			return;
		}
		for (int i = 0; i < marks.size(); i++) {
			if (t == i * CINDER_EVERY) {
				things.add(FallingCinderEntity.drop(level, this, marks.get(i)).getUUID());
			}
		}
		if (t == 0) {
			setAction(Action.IDLE);
		}
	}

	/**
	 * Lava Wave, {@code t} ticks into it: he beats his tail on the floor and the floor round him cracks and glows through the
	 * wind-up; then a low ring of slag rolls out from him to {@value #WAVE_REACH} blocks over the active ticks.
	 */
	private void lavaWave(ServerLevel level, int t) {
		Attack wave = Attack.LAVA_WAVE;
		if (t < wave.windup) {
			if (t % 3 == 0) {
				ring(level, waveFrom, 1.5 + 1.5 * t / wave.windup, ParticleTypes.LAVA, 12);
			}
			if (t % 8 == 4) {
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.HOSTILE, 1.0F, 0.8F);
			}
			if (t == wave.windup - 1) {
				setAction(Action.WAVE);
			}
			return;
		}
		int into = t - wave.windup;
		if (into >= wave.active) {
			return;
		}
		double before = waveReach;
		waveReach = WAVE_REACH * (into + 1) / wave.active;
		ring(level, waveFrom, waveReach, ParticleTypes.FLAME, Math.max(12, (int) (waveReach * 6)));
		if (into % 2 == 0) {
			ring(level, waveFrom, waveReach, ParticleTypes.LAVA, Math.max(6, (int) (waveReach * 2)));
		}
		rollWave(level, before, waveReach);
	}

	/**
	 * The wave rolls from {@code from} to {@code to} blocks: every player it passes on the floor takes {@value #WAVE_DAMAGE}
	 * fire damage and burns, once a wave; whoever is in the air, or a block up (a shelf, the crucible's rim), it passes
	 * under.
	 */
	public int rollWave(ServerLevel level, double from, double to) {
		int hit = 0;
		for (ServerPlayer player : nearby(level)) {
			double distance = Math.hypot(player.getX() - waveFrom.x, player.getZ() - waveFrom.z);
			double half = player.getBbWidth() / 2.0;
			if (distance + half < from || distance - half > to || waveHit.contains(player.getUUID())) {
				continue;
			}
			if (!player.onGround() || player.getY() > floorY() + 0.5) {
				continue;  // jumped it, or above it
			}
			waveHit.add(player.getUUID());
			if (player.hurtServer(level, flame(level), damage(WAVE_DAMAGE))) {
				hit++;
			}
			player.igniteForSeconds(WAVE_BURN);
		}
		return hit;
	}

	/** Starts a Lava Wave from {@code from}, its ring not yet rolled (for tests: the wave rolls with {@link #rollWave}). */
	public void waveFrom(Vec3 from) {
		waveFrom = from;
		waveReach = 0.0;
		waveHit.clear();
	}

	// ---------------------------------------------------------------- his rule: the heat is his

	/** Quenched: his seams go dark for {@value #QUENCH_TICKS} ticks, steam bursts from the trough. */
	public void quench(ServerLevel level) {
		boolean fresh = quenched == 0;
		quenched = QUENCH_TICKS;
		reheating = 0;
		setHeat(Heat.QUENCHED);
		level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.5, getZ(), 40, 1.8, 0.6, 1.8, 0.08);
		level.sendParticles(ParticleTypes.WHITE_SMOKE, getX(), getY() + 1.5, getZ(), 20, 1.4, 0.8, 1.4, 0.04);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 3.0F, 0.6F);
		if (fresh) {
			tell(level, "message.jugcraft.cinder_tyrant.quenched");
		}
	}

	/** His heat comes back: quenched, he counts down; then his seams flare back over {@value #REHEAT_TICKS} ticks. */
	private void cool(ServerLevel level) {
		if (quenched > 0) {
			if (--quenched == 0) {
				reheating = REHEAT_TICKS;
				setHeat(Heat.REHEATING);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 2.0F, 0.5F);
			} else if (quenched % 10 == 0) {
				level.sendParticles(ParticleTypes.WHITE_SMOKE, getX(), getY() + 1.8, getZ(), 3, 1.2, 0.3, 1.6, 0.01);
			}
		} else if (reheating > 0 && --reheating == 0) {
			setHeat(Heat.HOT);
		}
	}

	// ---------------------------------------------------------------- fire patches

	/** A patch of fire bursts at {@code at} (on the floor) for {@value #PATCH_TICKS} ticks; the oldest goes out past {@value #MAX_PATCHES}. */
	public void firePatch(ServerLevel level, Vec3 at) {
		if (patches.size() >= MAX_PATCHES) {
			patches.removeFirst();
		}
		patches.add(new FirePatch(at, level.getGameTime() + PATCH_TICKS));
	}

	/** The patches burn: flames on each, and whoever stands in one burns. Spent patches go out. */
	private void burnPatches(ServerLevel level) {
		long now = level.getGameTime();
		patches.removeIf(patch -> now >= patch.until());
		if (patches.isEmpty()) {
			return;
		}
		if (tickCount % 2 == 0) {
			for (FirePatch patch : patches) {
				level.sendParticles(ParticleTypes.FLAME, patch.at().x, patch.at().y + 0.1, patch.at().z, 2, PATCH_RADIUS * 0.5, 0.05, PATCH_RADIUS * 0.5, 0.01);
			}
		}
		if (tickCount % PATCH_EVERY == 0) {
			scorch(level);
		}
	}

	/** Every player standing in a fire patch takes {@value #PATCH_DAMAGE} fire damage and burns. */
	public int scorch(ServerLevel level) {
		int hit = 0;
		for (ServerPlayer player : nearby(level)) {
			for (FirePatch patch : patches) {
				double dx = player.getX() - patch.at().x;
				double dz = player.getZ() - patch.at().z;
				if (dx * dx + dz * dz <= (PATCH_RADIUS + player.getBbWidth() / 2.0) * (PATCH_RADIUS + player.getBbWidth() / 2.0)
						&& Math.abs(player.getY() - patch.at().y) < 1.5) {
					if (player.hurtServer(level, flame(level), damage(PATCH_DAMAGE))) {
						hit++;
					}
					player.igniteForSeconds(PATCH_BURN);
					break;
				}
			}
		}
		return hit;
	}

	// ---------------------------------------------------------------- the Eruption, the surges and the choke

	/**
	 * The Eruption: he leaps up onto the forge's lip and roars for {@value #ERUPT_TICKS} ticks, unhurt, cinders spitting from
	 * the vent; the heat channel surges and {@value #ERUPT_CINDERLINGS} Cinderlings crawl out of the slag; then he comes down
	 * into the bowl.
	 */
	public void startEruption(ServerLevel level) {
		attack = null;
		phase = Phase.ERUPTING;
		phaseTicks = 0;
		setAction(Action.LEAP);
		leapFrom = position();
		leapTo = at(CinderKiln.lip());
		tell(level, "message.jugcraft.cinder_tyrant.eruption");
	}

	private void erupting(ServerLevel level) {
		if (phaseTicks <= ERUPT_LEAP) {
			leap(phaseTicks - 1, ERUPT_LEAP);
			if (phaseTicks == ERUPT_LEAP) {
				setPos(leapTo.x, leapTo.y, leapTo.z);
				face(0.0F);
				setAction(Action.ROAR);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 4.0F, 0.5F);
				startSurge(level);
				for (int i = 0; i < ERUPT_CINDERLINGS && cinderlings.size() < MAX_CINDERLINGS; i++) {
					cinderlings.add(CinderlingEntity.crawlOut(level, this, at(CinderKiln.bank(i % 2 == 0))).getUUID());
				}
			}
			return;
		}
		int roared = phaseTicks - ERUPT_LEAP;
		if (roared < ERUPT_TICKS) {
			setDeltaMovement(Vec3.ZERO);
			if (roared % 2 == 0) {
				Vec3 vent = at(new Vec3(CinderKiln.BOWL_X, CinderKiln.BOWL + 28, CinderKiln.BOWL_Z));
				level.sendParticles(ParticleTypes.LAVA, vent.x, vent.y, vent.z, 4, 2.0, 1.0, 2.0, 0.0);
				level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 2.0, getZ(), 4, 1.2, 0.6, 1.6, 0.03);
			}
			if (roared == ERUPT_TICKS - 1) {
				setAction(Action.LEAP);
				leapFrom = position();
				leapTo = at(CinderKiln.out());
			}
			return;
		}
		int down = roared - ERUPT_TICKS;
		leap(down, ERUPT_LEAP);
		if (down >= ERUPT_LEAP - 1) {
			landed(level);
			phase = Phase.ERUPTION;
			phaseTicks = 0;
			long now = level.getGameTime();
			nextSurge = now + SURGE_EVERY;
			nextChoke = now;
			globalCooldown = GLOBAL_COOLDOWN;
			setAction(Action.IDLE);
		}
	}

	/** In the Eruption and the Molten Heart: the choke moves on, and the channel surges again, when their time comes. */
	private void eruptionClocks(ServerLevel level) {
		long now = level.getGameTime();
		if (nextChoke >= 0 && now >= nextChoke) {
			moveChoke(level);
			nextChoke = now + CHOKE_TICKS;
		}
		if (nextSurge >= 0 && now >= nextSurge) {
			startSurge(level);
			nextSurge = now + SURGE_EVERY;
		}
	}

	/**
	 * The choke moves to the next sluice (west, east, south, round again) that is not open (its flood is not cut short),
	 * and the sluice it leaves is clear to turn again. With the others open it stays where it is.
	 */
	public int moveChoke(ServerLevel level) {
		List<BlockPos> wheels = wheels();
		for (int step = 1; step <= wheels.size(); step++) {
			int next = ((choked < 0 ? -1 : choked) + step) % wheels.size();
			if (next == choked) {
				break;
			}
			BlockState state = level.getBlockState(wheels.get(next));
			if (!(state.getBlock() instanceof SluiceGateBlock) || state.getValue(SluiceGateBlock.FLOW) == SluiceGateBlock.Flow.OPEN) {
				continue;
			}
			if (choked >= 0) {
				SluiceGateBlock.choke(level, wheels.get(choked), false);
			}
			SluiceGateBlock.choke(level, wheels.get(next), true);
			choked = next;
			BlockPos wheel = wheels.get(next);
			level.sendParticles(ParticleTypes.LAVA, wheel.getX() + 0.5, wheel.getY() + 1.0, wheel.getZ() + 0.5, 10, 0.8, 0.8, 0.8, 0.0);
			break;
		}
		return choked;
	}

	/** Every sluice clear and ready to turn, and every trough drained (a fight that ends, resets or reloads). */
	private void clearSluices(ServerLevel level) {
		for (BlockPos wheel : wheels()) {
			if (level.isLoaded(wheel)) {
				SluiceGateBlock.reset(level, wheel);
			}
		}
		choked = -1;
	}

	/** The heat channel surges, announced: {@value #SURGE_WARN} ticks later its slag spills over its banks. */
	public void startSurge(ServerLevel level) {
		if (surgeSpills >= 0 || !surged.isEmpty()) {
			return;
		}
		surgeSpills = level.getGameTime() + SURGE_WARN;
		level.playSound(null, at(CinderKiln.centre()).x, floorY(), origin.getZ() + CinderKiln.CHANNEL_NORTH, SoundEvents.BUCKET_FILL_LAVA,
				SoundSource.HOSTILE, 3.0F, 0.5F);
		tell(level, "message.jugcraft.cinder_tyrant.surge");
	}

	/** The surge's clock: the channel bubbling while it is announced, the spill when it comes, and the ebb after. */
	private void surging(ServerLevel level) {
		long now = level.getGameTime();
		if (surgeSpills >= 0) {
			if (now < surgeSpills) {
				if (tickCount % 3 == 0) {
					for (int z = CinderKiln.CHANNEL_NORTH; z <= CinderKiln.CHANNEL_SOUTH; z += 2) {
						level.sendParticles(ParticleTypes.LAVA, origin.getX() + CinderKiln.RUN_WEST + 1.5, floorY(), origin.getZ() + z + 0.5, 1, 1.0, 0.0,
								0.5, 0.0);
					}
				}
				return;
			}
			spill(level);
		}
		if (surgeEbbs >= 0 && now >= surgeEbbs) {
			ebb(level);
		}
	}

	/**
	 * The slag spills over the channel's banks: every cell of floor within {@value #SURGE_SPILL} blocks of the channel (its
	 * cracked basalt, or the rough basalt beyond the bowl) with open air over it becomes molten slag for
	 * {@value #SURGE_TICKS} ticks. The shelves, the troughs and the crucible's rim are no floor of that kind, and stay dry.
	 */
	public List<BlockPos> spill(ServerLevel level) {
		surgeSpills = -1L;
		if (!surged.isEmpty()) {
			return List.of();
		}
		BlockState slag = JugcraftLairs.MOLTEN_SLAG.defaultBlockState();
		int y = origin.getY() + CinderKiln.BOWL;
		for (int x = CinderKiln.RUN_WEST - SURGE_SPILL; x <= CinderKiln.RUN_EAST + SURGE_SPILL; x++) {
			for (int z = CinderKiln.CHANNEL_NORTH; z <= CinderKiln.CHANNEL_SOUTH; z++) {
				if (CinderKiln.inChannel(x, z) || CinderKiln.inCrucible(x, z)) {
					continue;
				}
				BlockPos cell = new BlockPos(origin.getX() + x, y, origin.getZ() + z);
				BlockState state = level.getBlockState(cell);
				if (level.isLoaded(cell) && (state.is(JugcraftLairs.CRACKED_BASALT) || state.is(Blocks.BASALT))
						&& level.getBlockState(cell.above()).isAir()) {
					level.setBlock(cell, slag, Block.UPDATE_CLIENTS);
					surged.add(cell);
				}
			}
		}
		surgeEbbs = level.getGameTime() + SURGE_TICKS;
		level.playSound(null, origin.getX() + CinderKiln.BOWL_X, floorY(), origin.getZ() + CinderKiln.CHANNEL_SOUTH, SoundEvents.BUCKET_EMPTY,
				SoundSource.HOSTILE, 2.0F, 0.4F);
		return List.copyOf(surged);
	}

	/** The surge ebbs: the slag it spilled is floor again, cracked basalt in the bowl and rough basalt beyond it. */
	public void ebb(ServerLevel level) {
		for (BlockPos cell : surged) {
			if (level.isLoaded(cell) && level.getBlockState(cell).is(JugcraftLairs.MOLTEN_SLAG)) {
				boolean bowl = CinderKiln.inBowl(cell.getX() - origin.getX(), cell.getZ() - origin.getZ());
				level.setBlock(cell, bowl ? JugcraftLairs.CRACKED_BASALT.defaultBlockState() : Blocks.BASALT.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		if (!surged.isEmpty()) {
			BlockPos first = surged.getFirst();
			level.sendParticles(ParticleTypes.LARGE_SMOKE, first.getX() + 0.5, first.getY() + 1.2, first.getZ() + 0.5, 12, 2.0, 0.3, 3.0, 0.02);
		}
		surged.clear();
		surgeSpills = -1L;
		surgeEbbs = -1L;
	}

	/** The Molten Heart: white-hot cores swell from his seams, and his cooldowns shorten by a third. */
	public void startHeart(ServerLevel level) {
		attack = null;
		entityData.set(MOLTEN, true);
		phase = Phase.HEART;
		phaseTicks = 0;
		setAction(Action.IDLE);
		globalCooldown = cooldown(GLOBAL_COOLDOWN);
		level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 2.0, getZ(), 40, 1.5, 0.8, 2.0, 0.08);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 3.0F, 0.7F);
		tell(level, "message.jugcraft.cinder_tyrant.heart");
	}

	// ---------------------------------------------------------------- his Cinderlings and his things

	public List<UUID> cinderlings() {
		return List.copyOf(cinderlings);
	}

	/** Forgets what is gone. */
	private void prune(ServerLevel level) {
		cinderlings.removeIf(id -> !(level.getEntity(id) instanceof CinderlingEntity ling) || !ling.isAlive());
		things.removeIf(id -> level.getEntity(id) == null);
	}

	/** Whoever hurt him or his Cinderlings has taken part. */
	public void took(ServerPlayer player, float amount) {
		damageBy.merge(player.getUUID(), amount, Float::sum);
	}

	/** His participants: everyone who hurt him or his Cinderlings and is still near his bowl. */
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
	 * Left alone: he sinks back into the crucible, healed; the troughs drain, the sluices reset, the slag ebbs, his
	 * Cinderlings crumble, his things and fire are gone, and who hurt him is forgotten.
	 */
	public void reset(ServerLevel level) {
		dismiss(level);
		ebb(level);
		clearSluices(level);
		entityData.set(MOLTEN, false);
		quenched = 0;
		reheating = 0;
		setHeat(Heat.HOT);
		phase = Phase.WAITING;
		phaseTicks = 0;
		attack = null;
		abandoned = 0;
		foe = null;
		nextSurge = -1L;
		nextChoke = -1L;
		damageBy.clear();
		Arrays.fill(cooldowns, 0);
		setHealth(getMaxHealth());
		noPhysics = true;
		Vec3 sunk = sunk();
		teleportTo(sunk.x, sunk.y, sunk.z);
		face(0.0F);
		setAction(Action.SUNK);
		bossBar.setVisible(false);
		tell(level, "message.jugcraft.cinder_tyrant.reset");
	}

	/** His Cinderlings crumble, his gobs and cinders are gone, and his fire patches go out. */
	private void dismiss(ServerLevel level) {
		for (UUID id : List.copyOf(cinderlings)) {
			if (level.getEntity(id) instanceof CinderlingEntity ling) {
				ling.crumble(level);
			}
		}
		cinderlings.clear();
		for (UUID id : List.copyOf(things)) {
			Entity thing = level.getEntity(id);
			if (thing != null) {
				thing.discard();
			}
		}
		things.clear();
		patches.clear();
		marks.clear();
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level) {
			defeated(level);
		}
	}

	/**
	 * He has fallen: his Cinderlings crumble and his things are gone, the slag ebbs, the troughs drain and the sluices clear,
	 * each participant gets their loot, his lair's instance ends (it closes once everyone has left), the crucible's slag
	 * cools to obsidian, black volcanic glass, and a gate of Grey Mist opens in the middle of the bowl.
	 */
	private void defeated(ServerLevel level) {
		dismiss(level);
		ebb(level);
		clearSluices(level);
		CinderTyrantLoot.reward(level, this, participants(level));
		coolCrucible(level);
		LairInstance open = lairInstance(level);
		if (open != null) {
			Lairs.end(open);
			BlockState mist = JugcraftLairs.LAIR_EXIT.defaultBlockState().setValue(LairExitBlock.AXIS, Direction.Axis.X);
			for (BlockPos at : exitCells(centre())) {
				level.setBlock(at, mist, Block.UPDATE_ALL);
			}
		}
		bossBar.setVisible(false);
		tell(level, "message.jugcraft.cinder_tyrant.defeated");
	}

	/** The crucible's slag cools: its top layer becomes obsidian, black glass to walk on. Returns the cells cooled. */
	public int coolCrucible(ServerLevel level) {
		int cooled = 0;
		int y = origin.getY() + CinderKiln.BOWL;
		int r = (int) Math.ceil(CinderKiln.CRUCIBLE_RADIUS) + 1;
		for (int x = (int) Math.floor(CinderKiln.CRUCIBLE_X) - r; x <= (int) Math.floor(CinderKiln.CRUCIBLE_X) + r; x++) {
			for (int z = (int) Math.floor(CinderKiln.CRUCIBLE_Z) - r; z <= (int) Math.floor(CinderKiln.CRUCIBLE_Z) + r; z++) {
				BlockPos cell = new BlockPos(origin.getX() + x, y, origin.getZ() + z);
				if (CinderKiln.inCrucible(x, z) && level.getBlockState(cell).is(JugcraftLairs.MOLTEN_SLAG)) {
					level.setBlock(cell, Blocks.OBSIDIAN.defaultBlockState(), Block.UPDATE_CLIENTS);
					cooled++;
				}
			}
		}
		level.sendParticles(ParticleTypes.LARGE_SMOKE, origin.getX() + CinderKiln.CRUCIBLE_X, y + 1.2, origin.getZ() + CinderKiln.CRUCIBLE_Z, 30, 2.5,
				0.5, 2.5, 0.03);
		level.playSound(null, origin.getX() + CinderKiln.CRUCIBLE_X, y + 1.0, origin.getZ() + CinderKiln.CRUCIBLE_Z, SoundEvents.FIRE_EXTINGUISH,
				SoundSource.HOSTILE, 3.0F, 0.5F);
		return cooled;
	}

	/** Where the Grey Mist opens when he falls: two blocks wide and two high, standing in the middle of the bowl. */
	public static List<BlockPos> exitCells(Vec3 centre) {
		BlockPos at = BlockPos.containing(centre.x, centre.y, centre.z);
		return List.of(at.west(), at, at.west().above(), at.above());
	}

	/** Tells everyone near his bowl. */
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
		if (phase == Phase.WAKING || phase == Phase.ERUPTING || source.is(DamageTypeTags.IS_FIRE)) {
			return false;
		}
		float taken = (float) (amount * taken());
		boolean hurt = super.hurtServer(level, source, taken);
		if (hurt && source.getEntity() instanceof ServerPlayer player) {
			took(player, taken);
		}
		return hurt;
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
		return phase == Phase.WAITING ? null : SoundEvents.GHAST_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.IRON_GOLEM_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.WITHER_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.4F;
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
		builder.define(ACTION, Action.SUNK.ordinal());
		builder.define(HEAT, Heat.HOT.ordinal());
		builder.define(MOLTEN, false);
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
		output.putBoolean("molten", molten());
		// The floor the surge has spilled over, so the slag ebbs from it when he is loaded again.
		if (!surged.isEmpty()) {
			output.store("surged", BlockPos.CODEC.listOf(), List.copyOf(surged));
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		instance = input.read("instance", UUIDUtil.CODEC).orElse(null);
		slot = input.getIntOr("slot", -1);
		origin = input.read("origin", BlockPos.CODEC).orElse(BlockPos.containing(getX() - CinderKiln.CRUCIBLE_X, getY() - CinderKiln.SUNK_Y,
				getZ() - CinderKiln.SUNK_Z));
		try {
			phase = Phase.valueOf(input.getStringOr("phase", Phase.WAITING.name()));
		} catch (IllegalArgumentException e) {
			phase = Phase.WAITING;
		}
		if (phase == Phase.WAKING || phase == Phase.ERUPTING) {
			// A passing moment is not resumed: he lands where its leap would have ended, and fights on.
			phase = phase == Phase.WAKING ? Phase.KILN : Phase.ERUPTION;
			landOnLoad = true;
		}
		noPhysics = phase == Phase.WAITING;
		entityData.set(MOLTEN, input.getBooleanOr("molten", false));
		surged.addAll(input.read("surged", BlockPos.CODEC.listOf()).orElse(List.of()));
		if (phase == Phase.ERUPTION || phase == Phase.HEART) {
			nextChoke = 0L;
			nextSurge = 0L;
		}
		restoreOnLoad = true;
		bossBar.setVisible(phase != Phase.WAITING);
	}

	// ---------------------------------------------------------------- his animations

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<CinderTyrantEntity>("body", 4, test -> {
			CinderTyrantEntity tyrant = test.animatable();
			if (tyrant.isDeadOrDying()) {
				return test.setAndContinue(DEATH);
			}
			Action action = tyrant.action();
			return test.setAndContinue(action == Action.IDLE && test.isMoving() ? WALK : BODY[action.ordinal()]);
		}));
		controllers.add(new AnimationController<CinderTyrantEntity>("heat", 4, test -> {
			CinderTyrantEntity tyrant = test.animatable();
			return test.setAndContinue(switch (tyrant.heat()) {
				case QUENCHED -> HEAT_QUENCHED;
				case REHEATING -> HEAT_REHEAT;
				default -> tyrant.molten() ? HEAT_HEART : HEAT_HOT;
			});
		}));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
