package io.github.jimbozoomer.jugcraft.lair.dreadnought;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.LoopType;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.lair.DischargeBreakerBlock;
import io.github.jimbozoomer.jugcraft.lair.FoundryTrench;
import io.github.jimbozoomer.jugcraft.lair.FoundryTrench.Pylon;
import io.github.jimbozoomer.jugcraft.lair.GroundTerminalBlock;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairBosses;
import io.github.jimbozoomer.jugcraft.lair.LairExitBlock;
import io.github.jimbozoomer.jugcraft.lair.LairInstance;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.PylonCrownBlock;
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
import net.minecraft.core.particles.DustParticleOptions;
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
 * The Iron Dreadnought (docs/features/iron-dreadnought.md): the Foundry Trench's boss, a walking fortress of riveted
 * gunmetal on two piston legs, a tesla coil on each shoulder, a battery of rivet guns, a mortar in his back and a
 * searchlight for an eye. He stands dormant in his assembly gantry, his eye dark, and wakes when someone who may fight
 * him steps onto the pit's floor or strikes him: his boilers fire and he steps down out of the gantry, unhurt, his
 * health scaled for the party.
 *
 * <p>His rule: the discharge. While his vents are shut his plating turns most of every blow ({@value #PLATED} of it gets
 * through). Every {@value #DISCHARGE_EVERY} ticks he plants his legs and charges his coils for {@value #DISCHARGE_TICKS}
 * ticks: the pylon nearest his foe lights (two from the Torn Plate on), its crown violet and its breaker's lamp amber,
 * and the bar names it. Throwing a lit breaker routes the bolt into the ground: he overloads, his vents blow open for
 * {@value #OVERLOAD_TICKS} ticks (full damage), staggered for the first {@value #OVERLOAD_STAGGER}. Left, the discharge
 * runs out down the lit lanes: {@value #DISCHARGE_DAMAGE} to everyone within {@value #DISCHARGE_REACH} of one. The
 * breakers spring open again after. Phase 1, the Advance: the Piston Punch, the Rivet Fan, the Trench Charge (into a
 * block, he is stunned with a vent knocked open) and the Coil Arc. At {@value #TORN_AT} of his health, the Torn Plate: he
 * halts, unhurt, tears a plate from his hull and fouls the breaker of the pylon nearest his foe; a fouled breaker is
 * knocked loose by {@value #CLEAR_USES} uses, and whenever his vents shut after that he fouls another if none is. Phase
 * 2, the Overcharge: two pylons light, and the Mortar, the Blowdown and his Sparkers besides. Below {@value #RED_AT}, Red
 * Line: his vents jam open for good, his cooldowns and his pause are a third shorter, and he charges every
 * {@value #RED_DISCHARGE_EVERY} ticks.
 *
 * <p>Every attack has a wind-up you can read, a strike and a recovery ({@link Attack}); the numbers are here, in one place,
 * and in tools/iron_dreadnought.py. Poison, Wither, drowning, knockback and falls never harm him. Each participant (whoever
 * hurt him or his Sparkers, routed a discharge or worked at a fouled breaker) gets their own loot
 * ({@link IronDreadnoughtLoot}). Left alone he returns to his gantry, healed: his Sparkers burn out, the pylons go dark,
 * the breakers are cleared and open, and who hurt him is forgotten. Bound to a lair instance he vanishes with it; one with
 * no instance (a test's) keeps to the trench laid out from the origin he was given. The only blocks he changes are the
 * trench's crowns, breakers and terminals, and on his fall the Grey Mist in the gantry.
 */
public class IronDreadnoughtEntity extends Monster implements GeoEntity {
	// ---------------------------------------------------------------- numbers (tools/iron_dreadnought.py)
	public static final float HEALTH = 440.0F;
	public static final double ARMOR = 12.0;
	public static final double LEASH = 34.0;
	public static final double PARTY_STEP = 0.5;
	public static final double PARTY_MAX = 2.5;
	public static final int WAKE_TICKS = 50;
	public static final int ABANDON_TICKS = 200;
	public static final int GLOBAL_COOLDOWN = 20;
	public static final double SPEED = 0.14;
	public static final double KEEP_DISTANCE = 3.5;
	public static final double STEP = 0.6;
	public static final float TORN_AT = 0.5F;
	public static final float RED_AT = 0.2F;
	public static final double RED_COOLDOWN = 0.6667;
	public static final float PLATED = 0.25F;
	public static final int DISCHARGE_EVERY = 400;
	public static final int RED_DISCHARGE_EVERY = 240;
	public static final int DISCHARGE_TICKS = 90;
	public static final int OVERLOAD_TICKS = 120;
	public static final int OVERLOAD_STAGGER = 30;
	public static final float DISCHARGE_DAMAGE = 8.0F;
	public static final double DISCHARGE_REACH = 3.0;
	public static final int SPRING_TICKS = 20;
	public static final int TORN_TICKS = 40;
	public static final int CLEAR_USES = 3;
	public static final float PUNCH_DAMAGE = 9.0F;
	public static final double PUNCH_REACH = 4.5;
	public static final double PUNCH_HALF_ANGLE = 60.0;
	public static final double PUNCH_THROW = 1.6;
	public static final int RIVETS = 5;
	public static final double FAN_STEP = 10.0;
	public static final float RIVET_DAMAGE = 4.0F;
	public static final float CHARGE_DAMAGE = 10.0F;
	public static final double CHARGE_SPEED = 0.8;
	public static final double CHARGE_RANGE = 24.0;
	public static final double CHARGE_THROW = 1.4;
	public static final int STUN_TICKS = 40;
	public static final float ARC_DAMAGE = 5.0F;
	public static final int ARC_JUMPS = 3;
	public static final double ARC_JUMP = 4.0;
	public static final float MORTAR_DAMAGE = 10.0F;
	public static final double MORTAR_REACH = 3.0;
	public static final int SHELL_FLIGHT = 20;
	public static final float BLOWDOWN_DAMAGE = 6.0F;
	public static final double BLOWDOWN_REACH = 5.0;
	public static final int SLOW_TICKS = 40;
	public static final int SPARKER_CAP = 4;
	public static final int DROPPED = 2;
	public static final int EXPERIENCE = 300;
	/** The tops of his coils: either side of his middle and over his feet (tools/iron_dreadnought.py COILS). */
	public static final double COIL_SIDE = 2.0625;
	public static final double COIL_UP = 6.0;
	/** Waking: ticks his boilers fire before he steps down out of the gantry. */
	private static final int HEAVE = 15;
	/** The Torn Plate: the tick of its clip at which he hurls the plate (tools/iron_dreadnought_models.py "tear"). */
	private static final int TORN_HURL = 31;
	/** The least ticks between two "plated" words to one player. */
	private static final int PLATED_WORDS = 40;
	/** Violet, for his charge and his arcs; red, for the mortar's mark. */
	private static final DustParticleOptions VIOLET = new DustParticleOptions(0x9C62DC, 1.4F);
	private static final DustParticleOptions RED_SMOKE = new DustParticleOptions(0xC0281C, 1.6F);

	/**
	 * His attacks: wind-up, the active part and recovery in ticks, the cooldown, the target's nearest and farthest distance,
	 * and the phases he uses it in (the Advance; the Overcharge and Red Line). The wind-up is what players read; the strike
	 * lands as it ends. The Trench Charge's active ticks are the most he runs.
	 */
	public enum Attack {
		PISTON_PUNCH(16, 3, 12, 40, 0.0, 4.5, true, true),
		RIVET_FAN(18, 1, 12, 100, 4.0, 24.0, true, true),
		TRENCH_CHARGE(24, 30, 16, 200, 8.0, 24.0, true, true),
		COIL_ARC(20, 1, 10, 120, 0.0, 16.0, true, true),
		MORTAR(30, 1, 14, 160, 6.0, 32.0, false, true),
		BLOWDOWN(20, 4, 12, 140, 0.0, 5.0, false, true),
		SPARKERS(24, 1, 10, 300, 0.0, 40.0, false, true);

		public final int windup;
		public final int active;
		public final int recovery;
		public final int cooldown;
		public final double near;
		public final double far;
		public final boolean advance;
		public final boolean overcharge;

		Attack(int windup, int active, int recovery, int cooldown, double near, double far, boolean advance, boolean overcharge) {
			this.windup = windup;
			this.active = active;
			this.recovery = recovery;
			this.cooldown = cooldown;
			this.near = near;
			this.far = far;
			this.advance = advance;
			this.overcharge = overcharge;
		}

		public int length() {
			return windup + active + recovery;
		}
	}

	/** The fight's phases. */
	public enum Phase {
		WAITING, WAKING, ADVANCE, TORN_PLATE, OVERCHARGE, RED_LINE
	}

	/** What his body is doing, synced for his animations. */
	public enum Action {
		DORMANT, WAKE, IDLE, PUNCH_WINDUP, PUNCH, RIVETS_WINDUP, RIVETS, CHARGE_WINDUP, CHARGE, ARC_WINDUP, ARC, MORTAR_WINDUP,
		MORTAR, BLOWDOWN_WINDUP, BLOWDOWN, SPARKERS_WINDUP, SPARKERS, BRACE, REEL, TEAR
	}

	private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(IronDreadnoughtEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> EYE = SynchedEntityData.defineId(IronDreadnoughtEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> VENTS = SynchedEntityData.defineId(IronDreadnoughtEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> COILS = SynchedEntityData.defineId(IronDreadnoughtEntity.class, EntityDataSerializers.BOOLEAN);
	static final RawAnimation[] BODY = new RawAnimation[Action.values().length];
	static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.iron_dreadnought.walk");
	static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.iron_dreadnought.death");
	static final RawAnimation EYE_DARK = RawAnimation.begin().thenLoop("animation.iron_dreadnought.eye_dark");
	static final RawAnimation EYE_LIT = RawAnimation.begin().thenPlayAndHold("animation.iron_dreadnought.eye_lit");
	static final RawAnimation VENTS_SHUT = RawAnimation.begin().thenLoop("animation.iron_dreadnought.vents_shut");
	static final RawAnimation VENTS_OPEN = RawAnimation.begin().thenLoop("animation.iron_dreadnought.vents_open");
	static final RawAnimation COILS_QUIET = RawAnimation.begin().thenLoop("animation.iron_dreadnought.coils_quiet");
	static final RawAnimation COILS_CRACKLE = RawAnimation.begin().thenLoop("animation.iron_dreadnought.coils_crackle");

	static {
		String prefix = "animation.iron_dreadnought.";
		BODY[Action.DORMANT.ordinal()] = RawAnimation.begin().thenLoop(prefix + "dormant");
		BODY[Action.WAKE.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "wake");
		BODY[Action.IDLE.ordinal()] = RawAnimation.begin().thenLoop(prefix + "idle");
		BODY[Action.PUNCH_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "punch_windup");
		BODY[Action.PUNCH.ordinal()] = RawAnimation.begin().then(prefix + "punch", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.RIVETS_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "rivets_windup");
		BODY[Action.RIVETS.ordinal()] = RawAnimation.begin().then(prefix + "rivets", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.CHARGE_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "charge_windup");
		BODY[Action.CHARGE.ordinal()] = RawAnimation.begin().thenLoop(prefix + "charge");
		BODY[Action.ARC_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "arc_windup");
		BODY[Action.ARC.ordinal()] = RawAnimation.begin().then(prefix + "arc", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.MORTAR_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "mortar_windup");
		BODY[Action.MORTAR.ordinal()] = RawAnimation.begin().then(prefix + "mortar", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.BLOWDOWN_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "blowdown_windup");
		BODY[Action.BLOWDOWN.ordinal()] = RawAnimation.begin().then(prefix + "blowdown", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.SPARKERS_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "sparkers_windup");
		BODY[Action.SPARKERS.ordinal()] = RawAnimation.begin().then(prefix + "sparkers", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.BRACE.ordinal()] = RawAnimation.begin().thenLoop(prefix + "brace");
		BODY[Action.REEL.ordinal()] = RawAnimation.begin().thenLoop(prefix + "reel");
		BODY[Action.TEAR.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "tear");
	}

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private final ServerBossEvent bossBar = new ServerBossEvent(UUID.randomUUID(), Component.translatable("entity.jugcraft.iron_dreadnought"),
			BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);
	/** The lair instance he belongs to (null for one put down by a test or a command), and the trench's origin. */
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
	/** Whoever routed a discharge or worked at a fouled breaker: participants too. */
	private final Set<UUID> helpers = new HashSet<>();
	private final Map<UUID, Long> platedWords = new HashMap<>();
	// His rule.
	private int dischargeIn = DISCHARGE_EVERY;
	/** Ticks into charging his coils (0 while he is not charging), and the pylons lit for it. */
	private int charging;
	private final List<Pylon> lit = new ArrayList<>();
	private int overloadTicks;
	private int stunTicks;
	private int springIn;
	private @Nullable Pylon fouled;
	private int clearUses;
	// His Sparkers and his things.
	private final List<UUID> sparkers = new ArrayList<>();
	private final List<UUID> things = new ArrayList<>();
	// The attack under way.
	private Vec3 chargeWay = Vec3.ZERO;
	private Vec3 chargeFrom = Vec3.ZERO;
	private final Set<UUID> chargeHit = new HashSet<>();
	private Vec3 mark = Vec3.ZERO;
	/** Stepping down out of the gantry: from where. */
	private Vec3 stepFrom = Vec3.ZERO;
	/** Set when he is loaded in a passing moment (waking, the Torn Plate), or fighting: he lands and the pylons are reset. */
	private boolean landOnLoad;
	private boolean rebind;

	public IronDreadnoughtEntity(EntityType<? extends IronDreadnoughtEntity> type, Level level) {
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
				.add(Attributes.ATTACK_DAMAGE, PUNCH_DAMAGE).add(Attributes.STEP_HEIGHT, STEP);
	}

	/**
	 * Stands him in the assembly gantry of {@code instance}'s trench. Called as the instance is placed. Placing the template
	 * puts back only its own blocks, so the Grey Mist a fallen Dreadnought opened in the gantry before still stands: it goes
	 * first.
	 */
	public static IronDreadnoughtEntity summon(ServerLevel level, LairInstance instance) {
		BlockPos origin = instance.origin();
		for (BlockPos cell : FoundryTrench.exitCells()) {
			BlockPos at = cell.offset(origin);
			if (level.getBlockState(at).is(JugcraftLairs.LAIR_EXIT)) {
				level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		return summon(level, origin, instance);
	}

	/** Stands him in the gantry of the trench laid out from {@code origin} (tests pass no instance). */
	public static IronDreadnoughtEntity summon(ServerLevel level, BlockPos origin, @Nullable LairInstance instance) {
		IronDreadnoughtEntity dreadnought = new IronDreadnoughtEntity(JugcraftDreadnought.IRON_DREADNOUGHT, level);
		dreadnought.origin = origin.immutable();
		if (instance != null) {
			dreadnought.instance = instance.id;
			dreadnought.slot = instance.slot;
		}
		dreadnought.noPhysics = true;
		Vec3 waiting = dreadnought.waiting();
		dreadnought.snapTo(waiting.x, waiting.y, waiting.z, 0.0F, 0.0F);
		dreadnought.face(0.0F);
		level.addFreshEntity(dreadnought);
		return dreadnought;
	}

	// ---------------------------------------------------------------- where things are

	/** The pit's middle, on its floor, for the trench laid out from {@code origin}. */
	public static Vec3 centre(BlockPos origin) {
		return FoundryTrench.centre().add(origin.getX(), origin.getY(), origin.getZ());
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

	/** Where he waits in his gantry. */
	public Vec3 waiting() {
		return at(FoundryTrench.waiting());
	}

	/** Where he comes down to as he wakes. */
	public Vec3 woke() {
		return at(FoundryTrench.woke());
	}

	/** The pit's floor, where he and his foes stand. */
	public double floorY() {
		return origin.getY() + FoundryTrench.STAND;
	}

	public BlockPos breaker(Pylon pylon) {
		return pylon.breaker(origin);
	}

	public BlockPos crown(Pylon pylon) {
		return pylon.crown(origin);
	}

	public BlockPos terminal(Pylon pylon) {
		return pylon.terminal(origin);
	}

	/** The way to his left, flat (the model's +x). */
	public Vec3 left() {
		Vec3 facing = facing();
		return new Vec3(facing.z, 0.0, -facing.x);
	}

	/** The top of his coil on his left ({@code side} 1) or his right (-1). */
	public Vec3 coilTop(int side) {
		return position().add(left().scale(COIL_SIDE * side)).add(0.0, COIL_UP, 0.0);
	}

	/** The muzzles of his rivet battery, on his left pauldron's front (tools/iron_dreadnought_models.py BATTERY_*). */
	public Vec3 battery() {
		return position().add(facing().scale(HotRivetEntity.BATTERY_AHEAD)).add(left().scale(HotRivetEntity.BATTERY_SIDE))
				.add(0.0, HotRivetEntity.BATTERY_UP, 0.0);
	}

	/** The mortar's muzzle as it stands raised out of the hatch on his back. */
	public Vec3 mortarMuzzle() {
		return position().add(facing().scale(-MortarShellEntity.MORTAR_BACK)).add(0.0, MortarShellEntity.MORTAR_UP, 0.0);
	}

	/** Whether {@code entity} is in his arena (the pit or the gantry; the fire trench and the dugout are safe). */
	public boolean inArena(Entity entity) {
		return FoundryTrench.inArena(entity.getX() - origin.getX(), entity.getZ() - origin.getZ())
				&& entity.getY() - origin.getY() >= FoundryTrench.STAND - 2 && entity.getY() - origin.getY() <= FoundryTrench.GROUND;
	}

	/** Whether {@code player} stands on the pit's floor (off the ramp, the footings, the cover and the catwalks). */
	public boolean onPitFloor(Player player) {
		return FoundryTrench.onPitFloor(player.getX() - origin.getX(), player.getY() - origin.getY(), player.getZ() - origin.getZ());
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

	public int staggerTicks() {
		return staggerTicks;
	}

	/** Ticks into charging his coils, or 0. */
	public int charging() {
		return charging;
	}

	/** The pylons lit for the charge under way. */
	public List<Pylon> lit() {
		return List.copyOf(lit);
	}

	public int overloadTicks() {
		return overloadTicks;
	}

	public int stunTicks() {
		return stunTicks;
	}

	public int dischargeIn() {
		return dischargeIn;
	}

	/** Sets the ticks to his next charge (for tests). */
	public void dischargeIn(int ticks) {
		dischargeIn = ticks;
	}

	public @Nullable Pylon fouled() {
		return fouled;
	}

	public int clearUses() {
		return clearUses;
	}

	public List<UUID> sparkers() {
		return List.copyOf(sparkers);
	}

	/** Where the mortar's shell will land. */
	public Vec3 mark() {
		return mark;
	}

	/** Whether his vents are open: overloaded, stunned, or in Red Line. Every blow then does full damage. */
	public boolean ventsOpen() {
		return overloadTicks > 0 || stunTicks > 0 || phase == Phase.RED_LINE;
	}

	public Action action() {
		int id = entityData.get(ACTION);
		return id >= 0 && id < Action.values().length ? Action.values()[id] : Action.IDLE;
	}

	private void setAction(Action action) {
		entityData.set(ACTION, action.ordinal());
	}

	/** Whether his searchlight burns (synced). */
	public boolean eyeLit() {
		return entityData.get(EYE);
	}

	/** Whether his vents' flaps are open (synced). */
	public boolean ventsShown() {
		return entityData.get(VENTS);
	}

	/** Whether his coils crackle (synced). */
	public boolean coilsCrackling() {
		return entityData.get(COILS);
	}

	public @Nullable UUID instanceId() {
		return instance;
	}

	/** Whether he is fighting (down in the pit, not waiting, waking or tearing his plate). */
	public boolean fighting() {
		return phase == Phase.ADVANCE || phase == Phase.OVERCHARGE || phase == Phase.RED_LINE;
	}

	/** Damage he deals: the attack's own, times {@code lairs.boss_damage} ({@link LairBosses}). */
	public static float damage(float base) {
		return LairBosses.damage(base);
	}

	/** His health for a party of {@code players}: half as much again for each beyond the first, at most 2.5 times. */
	public static double partyScale(int players) {
		return LairBosses.partyScale(players, PARTY_STEP, PARTY_MAX);
	}

	/** A cooldown as it is in this phase: a third shorter in Red Line. */
	public int cooldown(int ticks) {
		return phase == Phase.RED_LINE ? (int) Math.round(ticks * RED_COOLDOWN) : ticks;
	}

	/** The ticks between his charges in this phase. */
	public int dischargeEvery() {
		return phase == Phase.RED_LINE ? RED_DISCHARGE_EVERY : DISCHARGE_EVERY;
	}

	/** How many pylons his charge lights in this phase: one in the Advance, two from the Torn Plate on. */
	public int pylonsLit() {
		return phase == Phase.ADVANCE ? 1 : 2;
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
			restoreFixtures(level);
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
			case TORN_PLATE -> tornPlate(level);
			case ADVANCE, OVERCHARGE, RED_LINE -> {
				tickRule(level);
				fight(level);
			}
		}
		entityData.set(VENTS, ventsOpen());
		Action action = action();
		entityData.set(COILS, charging > 0 || action == Action.ARC_WINDUP || action == Action.ARC);
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
		setAction(Action.DORMANT);
		if (tickCount % 10 == 0 && intruder(level) != null) {
			wake(level);
		}
	}

	/** Someone who may fight him, standing on the pit's floor. */
	private @Nullable Player intruder(ServerLevel level) {
		for (Player player : level.players()) {
			if (LairBosses.eligible(player) && onPitFloor(player)) {
				return player;
			}
		}
		return null;
	}

	/** His boilers fire: his health scaled for the players in his trench, his bar shown, his eye lit, smoke from his stacks. */
	public void wake(ServerLevel level) {
		if (phase != Phase.WAITING) {
			return;
		}
		double scale = partyScale(Math.max(1, present(level).size()));
		AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
		if (health != null) {
			health.setBaseValue(HEALTH * scale);
		}
		setHealth(getMaxHealth());
		phase = Phase.WAKING;
		phaseTicks = 0;
		setAction(Action.WAKE);
		entityData.set(EYE, true);
		bossBar.setVisible(true);
		dischargeIn = DISCHARGE_EVERY;
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 3.0F, 0.5F);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.PISTON_EXTEND, SoundSource.HOSTILE, 3.0F, 0.5F);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 5.5, getZ(), 30, 0.8, 0.6, 0.8, 0.04);
		tell(level, "message.jugcraft.iron_dreadnought.wakes");
	}

	/** Waking: his boilers fire, then he steps down out of the gantry onto the pit's floor. */
	private void waking(ServerLevel level) {
		noPhysics = true;
		setDeltaMovement(Vec3.ZERO);
		if (phaseTicks % 5 == 0) {
			stacksSmoke(level, 4);
		}
		if (phaseTicks <= HEAVE) {
			if (phaseTicks == HEAVE) {
				stepFrom = position();
			}
			return;
		}
		int walk = WAKE_TICKS - HEAVE;
		double share = Math.min(1.0, (phaseTicks - HEAVE) / (double) walk);
		Vec3 to = woke();
		Vec3 along = stepFrom.add(to.subtract(stepFrom).scale(share));
		setPos(along.x, along.y, along.z);
		face(to.subtract(stepFrom));
		if (phaseTicks % 10 == 0) {
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_GOLEM_STEP, SoundSource.HOSTILE, 2.5F, 0.4F);
		}
		if (phaseTicks >= WAKE_TICKS) {
			teleportTo(to.x, to.y, to.z);
			noPhysics = false;
			phase = Phase.ADVANCE;
			phaseTicks = 0;
			globalCooldown = GLOBAL_COOLDOWN;
			setAction(Action.IDLE);
		}
	}

	/** Players near enough to his trench to be in his fight (the dugout and the fire trench too): the party he scales for. */
	public List<ServerPlayer> present(ServerLevel level) {
		List<ServerPlayer> out = new ArrayList<>();
		Vec3 centre = centre();
		for (ServerPlayer player : level.players()) {
			if (LairBosses.eligible(player) && player.position().distanceTo(centre) <= LEASH + 8.0) {
				out.add(player);
			}
		}
		return out;
	}

	/** Players who may fight him and stand in his arena: those he chooses among, and those his blows can reach. */
	public List<ServerPlayer> nearby(ServerLevel level) {
		List<ServerPlayer> out = new ArrayList<>();
		for (ServerPlayer player : level.players()) {
			if (LairBosses.eligible(player) && inArena(player)) {
				out.add(player);
			}
		}
		return out;
	}

	// ---------------------------------------------------------------- his rule: the discharge

	/** His rule ticks on: the breakers springing open, the overload or the stun running out (and then his vents shut). */
	private void tickRule(ServerLevel level) {
		if (springIn > 0 && --springIn == 0) {
			spring(level);
		}
		if (overloadTicks > 0 && --overloadTicks == 0) {
			ventsShut(level);
		}
		if (stunTicks > 0 && --stunTicks == 0) {
			ventsShut(level);
		}
	}

	/** {@code count} pylons, the nearest to {@code target} first (by their ground terminals). */
	public List<Pylon> nearestPylons(Entity target, int count) {
		List<Pylon> byDistance = new ArrayList<>(List.of(Pylon.values()));
		double x = target.getX() - origin.getX();
		double z = target.getZ() - origin.getZ();
		byDistance.sort((a, b) -> Double.compare(a.distance(x, z), b.distance(x, z)));
		return byDistance.subList(0, Math.min(count, byDistance.size()));
	}

	/**
	 * He plants his legs and charges his coils: the pylons nearest {@code target} light (crowns violet, breakers' lamps
	 * amber), the bar names them, and the charge runs {@value #DISCHARGE_TICKS} ticks.
	 */
	public void startCharge(ServerLevel level, Entity target) {
		attack = null;
		attackTicks = 0;
		charging = 1;
		lit.clear();
		lit.addAll(nearestPylons(target, pylonsLit()));
		for (Pylon pylon : lit) {
			PylonCrownBlock.setLive(level, crown(pylon), true);
			DischargeBreakerBlock.set(level, breaker(pylon), DischargeBreakerBlock.LIVE, true);
		}
		setAction(Action.BRACE);
		Component names = pylonNames();
		bossBar.setName(Component.translatable("message.jugcraft.iron_dreadnought.discharge_bar", names));
		tell(level, "message.jugcraft.iron_dreadnought.charging", names);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 3.0F, 0.5F);
	}

	/** The lit pylons in words: "the west pylon", or "the west pylon and the south pylon". */
	private Component pylonNames() {
		if (lit.size() == 1) {
			return lit.get(0).display();
		}
		return Component.translatable("message.jugcraft.iron_dreadnought.and", lit.get(0).display(), lit.get(1).display());
	}

	/** The charge, a tick on: he holds, his coils draw violet threads to the lit crowns; when it ends, unrouted, it runs out. */
	private void tickCharge(ServerLevel level) {
		charging++;
		hold();
		if (charging % 3 == 0) {
			for (Pylon pylon : lit) {
				Vec3 crown = Vec3.atCenterOf(crown(pylon));
				line(level, coilTop(charging % 6 == 0 ? 1 : -1), crown, VIOLET, 1.5);
			}
		}
		if (charging % 20 == 0) {
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 2.5F, 0.5F + charging / 120.0F);
		}
		if (charging > DISCHARGE_TICKS) {
			unrouted(level);
		}
	}

	/**
	 * {@code player} used the breaker at {@code pos} (with {@code state}): a fouled one is worked at (and knocked loose after
	 * {@value #CLEAR_USES} uses); a lit one thrown while he charges routes the discharge. Returns whether this fight
	 * answered the use (else the breaker only says its pylon is dark).
	 */
	public boolean breakerUsed(ServerLevel level, BlockPos pos, BlockState state, Player player) {
		if (!fighting() && phase != Phase.TORN_PLATE) {
			return false;
		}
		Pylon pylon = FoundryTrench.breakerAt(origin, pos);
		if (pylon == null) {
			return false;
		}
		if (state.getValue(DischargeBreakerBlock.FOULED)) {
			helpers.add(player.getUUID());
			clearUses++;
			level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.8F, 1.4F);
			level.sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.1);
			if (clearUses >= CLEAR_USES) {
				clearFouled(level);
				player.sendOverlayMessage(Component.translatable("message.jugcraft.iron_dreadnought.cleared"));
			} else {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.iron_dreadnought.fouled", CLEAR_USES - clearUses));
			}
			return true;
		}
		if (charging > 0 && lit.contains(pylon) && !state.getValue(DischargeBreakerBlock.THROWN)) {
			route(level, pylon, player);
			return true;
		}
		return false;
	}

	/**
	 * The breaker of {@code pylon} slams home: the bolt is drawn from his coils into the crown, down the lattice and into
	 * the ground through the terminal, and the surge comes back into him: he overloads.
	 */
	public void route(ServerLevel level, Pylon pylon, @Nullable Player player) {
		if (player != null) {
			helpers.add(player.getUUID());
		}
		BlockPos breaker = breaker(pylon);
		DischargeBreakerBlock.set(level, breaker, DischargeBreakerBlock.THROWN, true);
		GroundTerminalBlock.setLive(level, terminal(pylon), true);
		Vec3 crown = Vec3.atCenterOf(crown(pylon));
		Vec3 ground = Vec3.atCenterOf(terminal(pylon)).add(0.0, 0.6, 0.0);
		line(level, coilTop(1), crown, ParticleTypes.ELECTRIC_SPARK, 0.6);
		line(level, coilTop(-1), crown, VIOLET, 0.8);
		line(level, crown, ground, ParticleTypes.ELECTRIC_SPARK, 0.5);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, ground.x, ground.y, ground.z, 30, 0.5, 0.2, 0.5, 0.2);
		level.playSound(null, breaker, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 3.0F, 0.8F);
		level.playSound(null, breaker, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.5F, 0.6F);
		tell(level, "message.jugcraft.iron_dreadnought.routed");
		endCharge(level);
		overload(level);
	}

	/**
	 * Unrouted, the discharge runs out down each lit pylon's lane: {@value #DISCHARGE_DAMAGE} to everyone within
	 * {@value #DISCHARGE_REACH} blocks of one (the Grounding Strap halves it); his plating holds. Returns how many it struck.
	 */
	public int unrouted(ServerLevel level) {
		int hit = 0;
		for (Pylon pylon : lit) {
			int[] lane = FoundryTrench.LANES[pylon.ordinal()];
			for (int x = lane[0]; x <= lane[1]; x += 2) {
				for (int z = lane[2]; z <= lane[3]; z += 2) {
					level.sendParticles(ParticleTypes.ELECTRIC_SPARK, origin.getX() + x + 0.5, floorY() + 0.3, origin.getZ() + z + 0.5, 3, 0.5,
							0.2, 0.5, 0.1);
				}
			}
			line(level, coilTop(1), Vec3.atCenterOf(crown(pylon)), ParticleTypes.ELECTRIC_SPARK, 0.6);
		}
		for (ServerPlayer player : nearby(level)) {
			if (player.getY() > floorY() + 3.0) {
				continue;
			}
			double x = player.getX() - origin.getX();
			double z = player.getZ() - origin.getZ();
			for (Pylon pylon : lit) {
				if (FoundryTrench.fromLane(pylon, x, z) <= DISCHARGE_REACH + player.getBbWidth() / 2.0) {
					if (player.hurtServer(level, GroundingStrap.arc(level, this, this), damage(DISCHARGE_DAMAGE))) {
						hit++;
					}
					break;
				}
			}
		}
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 3.0F, 1.2F);
		tell(level, "message.jugcraft.iron_dreadnought.unrouted");
		endCharge(level);
		return hit;
	}

	/** The charge is over: the lit crowns and lamps go dark, the breakers spring open soon, and the next charge waits. */
	private void endCharge(ServerLevel level) {
		for (Pylon pylon : lit) {
			PylonCrownBlock.setLive(level, crown(pylon), false);
			DischargeBreakerBlock.set(level, breaker(pylon), DischargeBreakerBlock.LIVE, false);
		}
		lit.clear();
		charging = 0;
		springIn = SPRING_TICKS;
		dischargeIn = dischargeEvery();
		globalCooldown = cooldown(GLOBAL_COOLDOWN);
		setAction(Action.IDLE);
		bossBar.setName(barName());
	}

	/** The breakers spring open and the terminals go dark. */
	private void spring(ServerLevel level) {
		for (Pylon pylon : Pylon.values()) {
			DischargeBreakerBlock.set(level, breaker(pylon), DischargeBreakerBlock.THROWN, false);
			GroundTerminalBlock.setLive(level, terminal(pylon), false);
		}
	}

	/** The surge comes back into him: his vents blow open for {@value #OVERLOAD_TICKS} ticks, and he reels at first. */
	public void overload(ServerLevel level) {
		overloadTicks = OVERLOAD_TICKS;
		stagger(OVERLOAD_STAGGER);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 3.0F, 0.6F);
		level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 3.0, getZ(), 30, 1.4, 1.0, 1.4, 0.05);
		bossBar.setName(Component.translatable("message.jugcraft.iron_dreadnought.overload_bar"));
	}

	/** His vents clang shut (unless they are jammed open, or still held open); in the Overcharge he fouls a breaker. */
	private void ventsShut(ServerLevel level) {
		if (ventsOpen()) {
			return;
		}
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_DOOR_CLOSE, SoundSource.HOSTILE, 2.0F, 0.5F);
		bossBar.setName(barName());
		if (phase == Phase.OVERCHARGE && fouled == null) {
			Player target = foe(level);
			foul(level, nearestPylons(target != null ? target : this, 1).get(0));
		}
	}

	/** A plate of his hull jams the breaker of {@code pylon}: it will not throw until it is knocked loose. */
	public void foul(ServerLevel level, Pylon pylon) {
		fouled = pylon;
		clearUses = 0;
		BlockPos at = breaker(pylon);
		DischargeBreakerBlock.set(level, at, DischargeBreakerBlock.FOULED, true);
		line(level, position().add(0.0, 4.0, 0.0), Vec3.atCenterOf(at), ParticleTypes.SMOKE, 0.8);
		level.sendParticles(ParticleTypes.CRIT, at.getX() + 0.5, at.getY() + 0.8, at.getZ() + 0.5, 16, 0.3, 0.3, 0.3, 0.2);
		level.playSound(null, at, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 2.0F, 0.6F);
	}

	/** The plate clangs loose: the fouled breaker is clear. */
	private void clearFouled(ServerLevel level) {
		if (fouled != null) {
			DischargeBreakerBlock.set(level, breaker(fouled), DischargeBreakerBlock.FOULED, false);
			BlockPos at = breaker(fouled);
			level.playSound(null, at, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.0F, 1.8F);
		}
		fouled = null;
		clearUses = 0;
	}

	/** His bar's name, out of a charge or an overload. */
	private Component barName() {
		return phase == Phase.RED_LINE ? Component.translatable("message.jugcraft.iron_dreadnought.red_bar")
				: Component.translatable("entity.jugcraft.iron_dreadnought");
	}

	// ---------------------------------------------------------------- fighting

	private void fight(ServerLevel level) {
		keepToThePit(level);
		if (phase == Phase.ADVANCE && getHealth() <= getMaxHealth() * TORN_AT) {
			tearPlate(level);
			return;
		}
		if (phase == Phase.OVERCHARGE && getHealth() <= getMaxHealth() * RED_AT) {
			redLine(level);
		}
		Player target = target(level);
		if (target == null) {
			if (charging > 0) {
				endCharge(level);
			}
			attack = null;
			setAction(staggerTicks > 0 ? Action.REEL : Action.IDLE);
			move(centre(), SPEED);
			if (++abandoned >= ABANDON_TICKS) {
				reset(level);
			}
			return;
		}
		abandoned = 0;
		if (charging > 0) {
			tickCharge(level);
			return;
		}
		if (overloadTicks == 0 && dischargeIn > 0) {
			dischargeIn--;
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
		if (dischargeIn <= 0 && overloadTicks == 0) {
			startCharge(level, target);
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

	/** Strayed farther than his leash from the pit's middle, or fallen below its floor, he is set back in it in a gust of steam. */
	private void keepToThePit(ServerLevel level) {
		Vec3 centre = centre();
		double dx = getX() - centre.x;
		double dz = getZ() - centre.z;
		if (dx * dx + dz * dz <= LEASH * LEASH && getY() > centre.y - 3.0) {
			return;
		}
		level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 2.0, getZ(), 30, 1.2, 1.4, 1.2, 0.02);
		attack = null;
		noPhysics = false;
		teleportTo(centre.x, centre.y, centre.z);
		level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 2.0, getZ(), 30, 1.2, 1.4, 1.2, 0.02);
	}

	/** His target: the one he has while they are still in his arena, else the nearest who may fight him there. */
	private @Nullable Player target(ServerLevel level) {
		if (foe != null) {
			Player held = level.getPlayerByUUID(foe);
			if (held != null && LairBosses.eligible(held) && inArena(held)) {
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

	/** Walks toward a point {@value #KEEP_DISTANCE} blocks short of {@code target}. */
	private void approach(Player target) {
		Vec3 to = target.position().subtract(position());
		double flat = Math.sqrt(to.x * to.x + to.z * to.z);
		Vec3 goal = flat > KEEP_DISTANCE + 0.5
				? new Vec3(target.getX() - to.x / flat * KEEP_DISTANCE, getY(), target.getZ() - to.z / flat * KEEP_DISTANCE)
				: position();
		move(goal, SPEED);
	}

	/** Walks toward {@code goal} at {@code speed}, pressed down onto the floor. */
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

	/** Whether nothing solid stands between his searchlight and {@code target}'s eyes. */
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
			boolean phaseAllows = phase == Phase.ADVANCE ? candidate.advance : candidate.overcharge;
			if (!phaseAllows || cooldowns[candidate.ordinal()] > 0 || distance < candidate.near || distance > candidate.far) {
				continue;
			}
			if (candidate == Attack.SPARKERS && sparkers.size() >= SPARKER_CAP) {
				continue;
			}
			if ((candidate == Attack.RIVET_FAN || candidate == Attack.COIL_ARC || candidate == Attack.TRENCH_CHARGE) && !sees(level, target)) {
				continue;
			}
			ready.add(candidate);
			if (candidate == Attack.PISTON_PUNCH) {
				ready.add(candidate);  // close in, his fist comes most often
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
			case PISTON_PUNCH -> {
				setAction(Action.PUNCH_WINDUP);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.PISTON_CONTRACT, SoundSource.HOSTILE, 2.0F, 0.5F);
			}
			case RIVET_FAN -> {
				setAction(Action.RIVETS_WINDUP);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.HOSTILE, 2.0F, 0.6F);
			}
			case TRENCH_CHARGE -> {
				setAction(Action.CHARGE_WINDUP);
				Vec3 to = target.position().subtract(position());
				double flat = Math.hypot(to.x, to.z);
				chargeWay = flat < 1.0E-3 ? facing() : new Vec3(to.x / flat, 0.0, to.z / flat);
				chargeHit.clear();
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 2.5F, 0.4F);
			}
			case COIL_ARC -> {
				setAction(Action.ARC_WINDUP);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 2.0F, 1.4F);
			}
			case MORTAR -> {
				setAction(Action.MORTAR_WINDUP);
				mark = new Vec3(target.getX(), floorY(), target.getZ());
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.HOSTILE, 2.5F, 0.4F);
			}
			case BLOWDOWN -> {
				setAction(Action.BLOWDOWN_WINDUP);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 2.5F, 0.4F);
			}
			case SPARKERS -> {
				setAction(Action.SPARKERS_WINDUP);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.HOSTILE, 2.5F, 0.8F);
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
			case PISTON_PUNCH -> {
				if (t < current.windup) {
					face(target);
					if (t % 4 == 0) {
						Vec3 elbow = position().add(left().scale(-2.2)).add(0.0, 3.0, 0.0);
						level.sendParticles(ParticleTypes.CLOUD, elbow.x, elbow.y, elbow.z, 2, 0.15, 0.15, 0.15, 0.01);
					}
				} else if (t == current.windup) {
					setAction(Action.PUNCH);
					punch(level);
				}
			}
			case RIVET_FAN -> {
				if (t < current.windup) {
					face(target);
				} else if (t == current.windup) {
					setAction(Action.RIVETS);
					fireRivets(level, target);
				}
			}
			case TRENCH_CHARGE -> trenchCharge(level, t);
			case COIL_ARC -> {
				if (t < current.windup) {
					face(target);
					if (t % 4 == 0) {
						level.sendParticles(ParticleTypes.ELECTRIC_SPARK, coilTop(1).x, coilTop(1).y, coilTop(1).z, 3, 0.2, 0.2, 0.2, 0.05);
						level.sendParticles(ParticleTypes.ELECTRIC_SPARK, coilTop(-1).x, coilTop(-1).y, coilTop(-1).z, 3, 0.2, 0.2, 0.2, 0.05);
					}
				} else if (t == current.windup) {
					setAction(Action.ARC);
					arc(level, target);
				}
			}
			case MORTAR -> {
				if (t < current.windup) {
					if (t % 3 == 0) {
						markSmoke(level);
					}
				} else if (t == current.windup) {
					setAction(Action.MORTAR);
					things.add(MortarShellEntity.lob(level, this, mortarMuzzle(), mark, SHELL_FLIGHT).getUUID());
				}
			}
			case BLOWDOWN -> {
				if (t < current.windup) {
					if (t % 3 == 0) {
						level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 4, 1.6, 0.1, 1.6, 0.01);
					}
				} else if (t == current.windup) {
					setAction(Action.BLOWDOWN);
					blowdown(level);
				} else if (t < current.windup + current.active) {
					steamRing(level);
				}
			}
			case SPARKERS -> {
				if (t < current.windup) {
					if (t % 4 == 0) {
						level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 2.2, getZ(), 3, 0.4, 0.1, 0.4, 0.05);
					}
				} else if (t == current.windup) {
					setAction(Action.SPARKERS);
					dropSparkers(level);
				}
			}
		}
		if (attack == current && attackTicks >= current.length()) {
			attack = null;
			attackTicks = 0;
			globalCooldown = cooldown(GLOBAL_COOLDOWN);
			Action action = action();
			if (action == Action.CHARGE || action.name().endsWith("_WINDUP")) {
				setAction(Action.IDLE);
			}
		}
	}

	/**
	 * Piston Punch: every player within {@value #PUNCH_REACH} blocks before him (within {@value #PUNCH_HALF_ANGLE} degrees
	 * either side of his face) takes {@value #PUNCH_DAMAGE} damage and is thrown back hard.
	 */
	public int punch(ServerLevel level) {
		int hit = 0;
		Vec3 facing = facing();
		for (ServerPlayer player : nearby(level)) {
			Vec3 to = player.position().subtract(position());
			double flat = Math.sqrt(to.x * to.x + to.z * to.z);
			if (flat > PUNCH_REACH + player.getBbWidth() / 2.0 || Math.abs(to.y) > 3.5) {
				continue;
			}
			double cos = flat < 1.0E-3 ? 1.0 : (to.x * facing.x + to.z * facing.z) / flat;
			if (cos < Math.cos(Math.toRadians(PUNCH_HALF_ANGLE))) {
				continue;  // behind him or to his side
			}
			if (player.hurtServer(level, damageSources().mobAttack(this), damage(PUNCH_DAMAGE))) {
				hit++;
				Vec3 away = flat < 1.0E-3 ? facing : new Vec3(to.x / flat, 0.0, to.z / flat);
				push(player, away.scale(PUNCH_THROW).add(0.0, 0.45, 0.0));
			}
		}
		Vec3 fist = position().add(facing.scale(3.0)).add(0.0, 1.5, 0.0);
		level.sendParticles(ParticleTypes.EXPLOSION, fist.x, fist.y, fist.z, 1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(ParticleTypes.CLOUD, fist.x, fist.y, fist.z, 8, 0.5, 0.3, 0.5, 0.05);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.PISTON_EXTEND, SoundSource.HOSTILE, 2.5F, 0.5F);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 2.0F, 0.6F);
		return hit;
	}

	/** Sets {@code player} moving at {@code motion}, and tells their client (a player's own motion is theirs to move). */
	static void push(ServerPlayer player, Vec3 motion) {
		player.setDeltaMovement(motion);
		player.connection.send(new ClientboundSetEntityMotionPacket(player));
	}

	/** Rivet Fan: {@value #RIVETS} hot rivets from his battery in a fan at {@code target}, {@value #FAN_STEP} degrees apart. */
	public int fireRivets(ServerLevel level, Entity target) {
		Vec3 from = battery();
		Vec3 aim = target.getBoundingBox().getCenter().subtract(from).normalize();
		for (int i = 0; i < RIVETS; i++) {
			double turn = Math.toRadians((i - (RIVETS - 1) / 2.0) * FAN_STEP);
			double cos = Math.cos(turn);
			double sin = Math.sin(turn);
			Vec3 way = new Vec3(aim.x * cos - aim.z * sin, aim.y, aim.x * sin + aim.z * cos).normalize();
			things.add(HotRivetEntity.fire(level, this, from, way).getUUID());
		}
		level.sendParticles(ParticleTypes.SMOKE, from.x, from.y, from.z, 10, 0.3, 0.2, 0.3, 0.02);
		level.playSound(null, from.x, from.y, from.z, SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 2.5F, 0.5F);
		return RIVETS;
	}

	/**
	 * Trench Charge, {@code t} ticks into it: through the wind-up his ram lowers and the dust kicks up ahead of him; then he
	 * runs straight on at {@value #CHARGE_SPEED} blocks a tick, at most {@value #CHARGE_RANGE} blocks. Everyone he runs into
	 * takes {@value #CHARGE_DAMAGE} damage and is thrown aside, once; run into a block, he is stunned ({@link #stun}).
	 */
	private void trenchCharge(ServerLevel level, int t) {
		Attack chargeAttack = Attack.TRENCH_CHARGE;
		if (t < chargeAttack.windup) {
			face(chargeWay);
			if (t % 3 == 0) {
				Vec3 ahead = position().add(chargeWay.scale(2.5));
				level.sendParticles(ParticleTypes.POOF, ahead.x, floorY() + 0.2, ahead.z, 4, 0.8, 0.05, 0.8, 0.02);
				stacksSmoke(level, 2);
			}
			return;
		}
		if (t == chargeAttack.windup) {
			setAction(Action.CHARGE);
			chargeFrom = position();
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.MINECART_RIDING, SoundSource.HOSTILE, 3.0F, 0.5F);
		}
		if (t >= chargeAttack.windup + chargeAttack.active) {
			return;  // recovery: he grinds to a halt
		}
		if (t > chargeAttack.windup && horizontalCollision) {
			stun(level);
			return;
		}
		chargeHits(level);
		double run = Math.hypot(getX() - chargeFrom.x, getZ() - chargeFrom.z);
		if (run >= CHARGE_RANGE) {
			attackTicks = chargeAttack.windup + chargeAttack.active;
			setAction(Action.IDLE);
			return;
		}
		face(chargeWay);
		setDeltaMovement(chargeWay.x * CHARGE_SPEED, -0.3, chargeWay.z * CHARGE_SPEED);
		if (t % 2 == 0) {
			level.sendParticles(ParticleTypes.POOF, getX(), floorY() + 0.2, getZ(), 3, 1.2, 0.05, 1.2, 0.02);
			level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 5.2, getZ(), 2, 0.4, 0.2, 0.4, 0.02);
		}
	}

	/** Sets the way his Trench Charge runs, as its wind-up would (for tests). */
	public void chargeWay(Vec3 way) {
		chargeWay = new Vec3(way.x, 0.0, way.z).normalize();
		chargeHit.clear();
	}

	/** The players he runs into on his charge: each takes {@value #CHARGE_DAMAGE} damage and is thrown aside, once. */
	public int chargeHits(ServerLevel level) {
		int hit = 0;
		for (ServerPlayer player : nearby(level)) {
			if (chargeHit.contains(player.getUUID()) || !getBoundingBox().inflate(0.4, 0.0, 0.4).intersects(player.getBoundingBox())) {
				continue;
			}
			chargeHit.add(player.getUUID());
			if (player.hurtServer(level, damageSources().mobAttack(this), damage(CHARGE_DAMAGE))) {
				hit++;
				Vec3 to = player.position().subtract(position());
				Vec3 side = left();
				double which = to.x * side.x + to.z * side.z >= 0.0 ? 1.0 : -1.0;
				push(player, side.scale(which * CHARGE_THROW).add(chargeWay.scale(0.6)).add(0.0, 0.5, 0.0));
				level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.5F, 0.6F);
			}
		}
		return hit;
	}

	/** He crashes into a block: stunned for {@value #STUN_TICKS} ticks, a vent knocked open (full damage meanwhile). */
	public void stun(ServerLevel level) {
		stunTicks = STUN_TICKS;
		stagger(STUN_TICKS);
		setAction(Action.REEL);
		Vec3 front = position().add(facing().scale(2.0)).add(0.0, 1.5, 0.0);
		level.sendParticles(ParticleTypes.EXPLOSION, front.x, front.y, front.z, 2, 0.6, 0.6, 0.6, 0.0);
		level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 3.0, getZ(), 16, 1.4, 0.8, 1.4, 0.04);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 3.0F, 0.4F);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_DOOR_OPEN, SoundSource.HOSTILE, 2.0F, 0.5F);
		tell(level, "message.jugcraft.iron_dreadnought.stunned");
	}

	/** He reels: any attack under way is spoiled, and he does nothing for {@code ticks}. */
	public void stagger(int ticks) {
		attack = null;
		attackTicks = 0;
		staggerTicks = Math.max(staggerTicks, ticks);
		setAction(Action.REEL);
	}

	/**
	 * Coil Arc: {@value #ARC_DAMAGE} to {@code target}, then the arc jumps to the nearest other player within
	 * {@value #ARC_JUMP} blocks of the last it struck, {@value #ARC_JUMPS} times at most. Returns how many it struck.
	 */
	public int arc(ServerLevel level, Player target) {
		int struck = 0;
		Set<UUID> hit = new HashSet<>();
		Vec3 from = coilTop(1);
		Player next = sees(level, target) && target.distanceTo(this) <= Attack.COIL_ARC.far + 1.0 ? target : null;
		for (int jump = 0; next != null && jump <= ARC_JUMPS; jump++) {
			Vec3 to = next.getBoundingBox().getCenter();
			line(level, from, to, ParticleTypes.ELECTRIC_SPARK, 0.4);
			line(level, from, to, VIOLET, 0.7);
			hit.add(next.getUUID());
			if (next.hurtServer(level, GroundingStrap.arc(level, this, this), damage(ARC_DAMAGE))) {
				struck++;
			}
			from = to;
			Player nearest = null;
			double best = ARC_JUMP * ARC_JUMP;
			for (ServerPlayer player : nearby(level)) {
				double distance = player.getBoundingBox().getCenter().distanceToSqr(to);
				if (!hit.contains(player.getUUID()) && distance <= best) {
					best = distance;
					nearest = player;
				}
			}
			next = nearest;
		}
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 2.0F, 1.6F);
		return struck;
	}

	/** Sets where the mortar's shell will land, as its wind-up would (for tests). */
	public void mark(Vec3 at) {
		mark = at;
	}

	/** Red smoke marks the ground where the shell will land, round the mark's edge and in its middle. */
	private void markSmoke(ServerLevel level) {
		for (int i = 0; i < 12; i++) {
			double rad = Math.toRadians(30.0 * i);
			level.sendParticles(RED_SMOKE, mark.x + Math.cos(rad) * MORTAR_REACH, mark.y + 0.15, mark.z + Math.sin(rad) * MORTAR_REACH, 1, 0.0,
					0.05, 0.0, 0.0);
		}
		level.sendParticles(RED_SMOKE, mark.x, mark.y + 0.4, mark.z, 4, 0.3, 0.3, 0.3, 0.0);
		level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, mark.x, mark.y + 0.2, mark.z, 1, 0.1, 0.0, 0.1, 0.01);
	}

	/**
	 * A mortar shell lands on {@code at}: {@value #MORTAR_DAMAGE} to everyone within {@value #MORTAR_REACH} blocks of it.
	 * Called by the shell as it lands; returns how many it struck.
	 */
	public int shellLands(ServerLevel level, MortarShellEntity shell, Vec3 at) {
		int hit = 0;
		for (ServerPlayer player : nearby(level)) {
			double flat = Math.hypot(player.getX() - at.x, player.getZ() - at.z);
			if (flat > MORTAR_REACH + player.getBbWidth() / 2.0 || Math.abs(player.getY() - at.y) > 3.0) {
				continue;
			}
			if (player.hurtServer(level, damageSources().explosion(shell, this), damage(MORTAR_DAMAGE))) {
				hit++;
			}
		}
		return hit;
	}

	/**
	 * Blowdown: a burst of scalding steam round him: {@value #BLOWDOWN_DAMAGE} to everyone within {@value #BLOWDOWN_REACH}
	 * blocks of his middle, slowed for {@value #SLOW_TICKS} ticks and pushed back.
	 */
	public int blowdown(ServerLevel level) {
		int hit = 0;
		for (ServerPlayer player : nearby(level)) {
			Vec3 to = player.position().subtract(position());
			double flat = Math.hypot(to.x, to.z);
			if (flat > BLOWDOWN_REACH + player.getBbWidth() / 2.0 || Math.abs(to.y) > 3.5) {
				continue;
			}
			if (player.hurtServer(level, damageSources().mobAttack(this), damage(BLOWDOWN_DAMAGE))) {
				hit++;
				player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SLOW_TICKS, 1), this);
				Vec3 away = flat < 1.0E-3 ? facing() : new Vec3(to.x / flat, 0.0, to.z / flat);
				push(player, away.scale(0.8).add(0.0, 0.3, 0.0));
			}
		}
		steamRing(level);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 3.0F, 0.3F);
		return hit;
	}

	/** Steam bursting out round his feet. */
	private void steamRing(ServerLevel level) {
		for (int i = 0; i < 16; i++) {
			double rad = Math.toRadians(22.5 * i);
			double reach = BLOWDOWN_REACH * (0.5 + getRandom().nextDouble() * 0.5);
			level.sendParticles(ParticleTypes.CLOUD, getX() + Math.cos(rad) * reach, getY() + 0.4, getZ() + Math.sin(rad) * reach, 1, 0.1, 0.2,
					0.1, 0.02);
		}
	}

	/** Sparkers: {@value #DROPPED} drop from the hatch in his belly, no more than his cap of {@value #SPARKER_CAP} allows. */
	public int dropSparkers(ServerLevel level) {
		int dropped = 0;
		for (int i = 0; i < DROPPED && sparkers.size() < SPARKER_CAP; i++) {
			Vec3 at = position().add(left().scale(i == 0 ? 0.8 : -0.8)).add(0.0, 0.2, 0.0);
			SparkerEntity sparker = SparkerEntity.drop(level, this, at);
			sparkers.add(sparker.getUUID());
			dropped++;
		}
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.HOSTILE, 2.0F, 0.8F);
		return dropped;
	}

	/** Smoke from his two stacks. */
	private void stacksSmoke(ServerLevel level, int count) {
		Vec3 back = position().add(facing().scale(-0.6)).add(0.0, 6.2, 0.0);
		Vec3 side = left().scale(0.75);
		for (int s = -1; s <= 1; s += 2) {
			Vec3 at = back.add(side.scale(s));
			level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, count, 0.15, 0.2, 0.15, 0.02);
		}
	}

	// ---------------------------------------------------------------- the Torn Plate, and Red Line

	/** The Torn Plate: he halts for {@value #TORN_TICKS} ticks, unhurt, and tears a plate from his hull; any charge is spent. */
	public void tearPlate(ServerLevel level) {
		if (charging > 0) {
			endCharge(level);
		}
		attack = null;
		staggerTicks = 0;
		phase = Phase.TORN_PLATE;
		phaseTicks = 0;
		setAction(Action.TEAR);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_DESTROY, SoundSource.HOSTILE, 3.0F, 0.5F);
		tell(level, "message.jugcraft.iron_dreadnought.torn");
	}

	/** The Torn Plate holds: he hurls the plate into the breaker of the pylon nearest his foe, and then the Overcharge begins. */
	private void tornPlate(ServerLevel level) {
		hold();
		if (phaseTicks == TORN_HURL) {
			Player target = foe(level);
			foul(level, nearestPylons(target != null ? target : this, 1).get(0));
		}
		if (phaseTicks >= TORN_TICKS) {
			phase = Phase.OVERCHARGE;
			phaseTicks = 0;
			globalCooldown = GLOBAL_COOLDOWN;
			setAction(Action.IDLE);
		}
	}

	/** Red Line: his boilers redline, his vents jam open for good, and he speeds up. */
	public void redLine(ServerLevel level) {
		phase = Phase.RED_LINE;
		phaseTicks = 0;
		dischargeIn = Math.min(dischargeIn, RED_DISCHARGE_EVERY);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 3.0F, 0.3F);
		level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 3.0, getZ(), 40, 1.6, 1.2, 1.6, 0.06);
		bossBar.setName(barName());
		tell(level, "message.jugcraft.iron_dreadnought.red_line");
	}

	// ---------------------------------------------------------------- his things

	/** Forgets what is gone. */
	private void prune(ServerLevel level) {
		sparkers.removeIf(id -> !(level.getEntity(id) instanceof SparkerEntity sparker) || !sparker.isAlive());
		things.removeIf(id -> level.getEntity(id) == null);
	}

	/** Whoever hurt him or his Sparkers has taken part. */
	public void took(ServerPlayer player, float amount) {
		damageBy.merge(player.getUUID(), amount, Float::sum);
	}

	/** His participants: everyone who hurt him or his Sparkers, routed a discharge or worked at a fouled breaker, and is near. */
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
	 * Left alone: he returns to his gantry, healed; his Sparkers burn out and his rivets and shells are gone, the pylons go
	 * dark, the breakers are cleared and open, and who hurt him is forgotten.
	 */
	public void reset(ServerLevel level) {
		dismiss(level);
		restoreFixtures(level);
		phase = Phase.WAITING;
		phaseTicks = 0;
		attack = null;
		abandoned = 0;
		staggerTicks = 0;
		foe = null;
		charging = 0;
		lit.clear();
		overloadTicks = 0;
		stunTicks = 0;
		springIn = 0;
		dischargeIn = DISCHARGE_EVERY;
		damageBy.clear();
		helpers.clear();
		Arrays.fill(cooldowns, 0);
		setHealth(getMaxHealth());
		entityData.set(EYE, false);
		noPhysics = true;
		Vec3 waiting = waiting();
		teleportTo(waiting.x, waiting.y, waiting.z);
		face(0.0F);
		setAction(Action.DORMANT);
		bossBar.setName(Component.translatable("entity.jugcraft.iron_dreadnought"));
		bossBar.setVisible(false);
		tell(level, "message.jugcraft.iron_dreadnought.reset");
	}

	/** Every pylon dark, every breaker open, clear and unlit, every terminal dark. */
	private void restoreFixtures(ServerLevel level) {
		for (Pylon pylon : Pylon.values()) {
			if (!level.isLoaded(breaker(pylon))) {
				continue;
			}
			PylonCrownBlock.setLive(level, crown(pylon), false);
			DischargeBreakerBlock.set(level, breaker(pylon), DischargeBreakerBlock.LIVE, false);
			DischargeBreakerBlock.set(level, breaker(pylon), DischargeBreakerBlock.THROWN, false);
			DischargeBreakerBlock.set(level, breaker(pylon), DischargeBreakerBlock.FOULED, false);
			GroundTerminalBlock.setLive(level, terminal(pylon), false);
		}
		fouled = null;
		clearUses = 0;
		lit.clear();
		charging = 0;
	}

	/** His Sparkers burn out, and his rivets and shells are gone. */
	private void dismiss(ServerLevel level) {
		for (UUID id : List.copyOf(sparkers)) {
			if (level.getEntity(id) instanceof SparkerEntity sparker) {
				sparker.burnOut(level);
			}
		}
		sparkers.clear();
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
	 * He has fallen: his Sparkers burn out and his things are gone, each participant gets their loot, the pylons go dark,
	 * his lair's instance ends (it closes once everyone has left), and Grey Mist opens in the gantry he came from.
	 */
	private void defeated(ServerLevel level) {
		dismiss(level);
		IronDreadnoughtLoot.reward(level, this, participants(level));
		restoreFixtures(level);
		entityData.set(EYE, false);
		level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 2.0, getZ(), 40, 1.6, 1.4, 1.6, 0.03);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 3.0F, 0.2F);
		LairInstance open = lairInstance(level);
		if (open != null) {
			Lairs.end(open);
			BlockState mist = JugcraftLairs.LAIR_EXIT.defaultBlockState().setValue(LairExitBlock.AXIS, Direction.Axis.X);
			for (BlockPos cell : FoundryTrench.exitCells()) {
				level.setBlock(cell.offset(origin), mist, Block.UPDATE_ALL);
			}
		}
		bossBar.setVisible(false);
		tell(level, "message.jugcraft.iron_dreadnought.defeated");
	}

	/** Tells everyone near his trench. */
	void tell(ServerLevel level, String key, Object... args) {
		Vec3 centre = centre();
		for (ServerPlayer player : level.players()) {
			if (player.position().distanceTo(centre) <= LEASH + 32.0) {
				player.sendSystemMessage(Component.translatable(key, args));
			}
		}
	}

	// ---------------------------------------------------------------- what he is immune to, and his plating

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
		if (phase == Phase.WAKING || phase == Phase.TORN_PLATE) {
			return false;
		}
		float dealt = amount;
		if (!ventsOpen()) {
			dealt = amount * PLATED;
			plated(level, source);
		}
		boolean hurt = super.hurtServer(level, source, dealt);
		if (hurt && source.getEntity() instanceof ServerPlayer player) {
			took(player, dealt);
		}
		return hurt;
	}

	/** A blow on his shut plating: sparks fly and most of it is turned; the striker is told why (now and then). */
	private void plated(ServerLevel level, DamageSource source) {
		level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 2.5, getZ(), 6, 1.0, 1.0, 1.0, 0.1);
		if (source.getEntity() instanceof ServerPlayer player) {
			long now = level.getGameTime();
			Long last = platedWords.get(player.getUUID());
			if (last == null || now - last >= PLATED_WORDS || now < last) {
				platedWords.put(player.getUUID(), now);
				player.sendOverlayMessage(Component.translatable("message.jugcraft.iron_dreadnought.plated"));
			}
		}
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
		return phase == Phase.WAITING ? null : SoundEvents.IRON_GOLEM_STEP;
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
		builder.define(ACTION, Action.DORMANT.ordinal());
		builder.define(EYE, false);
		builder.define(VENTS, false);
		builder.define(COILS, false);
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
		origin = input.read("origin", BlockPos.CODEC).orElse(BlockPos.containing(getX() - FoundryTrench.WAIT_X, getY() - FoundryTrench.WAIT_Y,
				getZ() - FoundryTrench.WAIT_Z));
		try {
			phase = Phase.valueOf(input.getStringOr("phase", Phase.WAITING.name()));
		} catch (IllegalArgumentException e) {
			phase = Phase.WAITING;
		}
		if (phase == Phase.WAKING || phase == Phase.TORN_PLATE) {
			// A passing moment is not resumed: he comes down to the floor, and fights on.
			landOnLoad = phase == Phase.WAKING;
			phase = phase == Phase.WAKING ? Phase.ADVANCE : Phase.OVERCHARGE;
		}
		// His charge, overload and fouled breaker are not saved: loaded fighting, the pylons are put back dark and clear.
		rebind = fighting();
		noPhysics = phase == Phase.WAITING;
		entityData.set(EYE, phase != Phase.WAITING);
		bossBar.setVisible(phase != Phase.WAITING);
		bossBar.setName(barName());
	}

	// ---------------------------------------------------------------- his animations

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<IronDreadnoughtEntity>("body", 4, test -> {
			IronDreadnoughtEntity dreadnought = test.animatable();
			if (dreadnought.isDeadOrDying()) {
				return test.setAndContinue(DEATH);
			}
			Action action = dreadnought.action();
			return test.setAndContinue(action == Action.IDLE && test.isMoving() ? WALK : BODY[action.ordinal()]);
		}));
		controllers.add(new AnimationController<IronDreadnoughtEntity>("eye", 2,
				test -> test.setAndContinue(test.animatable().eyeLit() && !test.animatable().isDeadOrDying() ? EYE_LIT : EYE_DARK)));
		controllers.add(new AnimationController<IronDreadnoughtEntity>("vents", 2,
				test -> test.setAndContinue(test.animatable().ventsShown() ? VENTS_OPEN : VENTS_SHUT)));
		controllers.add(new AnimationController<IronDreadnoughtEntity>("coils", 2,
				test -> test.setAndContinue(test.animatable().coilsCrackling() ? COILS_CRACKLE : COILS_QUIET)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
