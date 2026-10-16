package io.github.jimbozoomer.jugcraft.lair.alpha;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.LoopType;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.agriculture.Werewolf;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairBosses;
import io.github.jimbozoomer.jugcraft.lair.LairExitBlock;
import io.github.jimbozoomer.jugcraft.lair.LairInstance;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.MoonlitRing;
import io.github.jimbozoomer.jugcraft.lair.MoonlitRing.Bell;
import io.github.jimbozoomer.jugcraft.lair.RakedTurfBlock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
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
 * The Alpha Werewolf (docs/features/alpha-werewolf.md): the Moonlit Ring's boss, the oldest of the werewolves, twice the
 * height of a man: a silver-grey mane over a hunched black back, a scarred muzzle, a torn ear, sickle claws and eyes that
 * burn moon-white. He crouches on the Howling Stone, head raised to the moon, eyes dark, and wakes when someone who may
 * fight him steps off the camp onto the ring's turf or strikes him: he rises and howls (unhurt), his health scaled for
 * the party, his eyes blaze, and his pack ({@link PackmateEntity}, {@value #PACK_SIZE} at most) lopes out of the trees.
 *
 * <p>His hide, his kind's: every blow but silver's does {@value #HIDE} of its damage; silver does full. His rule, the
 * Hunt: every {@value #HUNT_EVERY} ticks ({@value #FURY_HUNT_EVERY} in the Moon's Fury, {@value #LAST_HUNT_EVERY} in the
 * Last Hunt) he marks his quarry with a short howl and the bar names them; he bounds off to hide in one of the three bell
 * clearings, never the one nearest his quarry, tearing the clearing's moss into raked turf as he runs (the trail to where
 * he is); his pack drives the quarry; {@value #SHADOW_TICKS} ticks before the {@value #AMBUSH_TICKS}-tick ambush ends a
 * shadow spreads where the quarry stands, and he springs onto it: {@value #SPRING_DAMAGE} within {@value #SPRING_REACH}
 * blocks, knocked flat. The bell of his clearing rung before he springs flushes him: he staggers out along its spoke,
 * every blow does full damage (silver {@value #FLUSHED_SILVER} times) for {@value #FLUSHED_TICKS} ticks, and his pack
 * cowers for {@value #COWER_TICKS}. At {@value #GATHER_AT} of his health the Pack Gathers: he leaps onto the stone and
 * howls long (unhurt), and his pack is called back up to {@value #PACK_SIZE}; the Moon's Fury follows, with the Tree Leap
 * and the Hamstring Bite besides. Below {@value #LAST_AT}, the Last Hunt: in every ambush, {@value #SWITCH_AT} ticks in,
 * he slips through the trees to another clearing, leaving a second trail; his tells stay as long.
 *
 * <p>Every attack has a wind-up you can read, a strike and a recovery ({@link Attack}); the numbers are here and in
 * tools/alpha_werewolf.py. Poison, Wither, drowning, knockback and falls never harm or move him. Each participant
 * (whoever hurt him or his pack, or rang the bell that flushed him) gets their own loot ({@link AlphaWerewolfLoot}). Left
 * alone he slinks back to his stone, healed; his pack melts into the trees, his trail knits, and who hurt him is
 * forgotten. Bound to a lair instance he vanishes with it; one with no instance (a test's) keeps to the ring laid out from
 * the origin he was given. The only blocks he changes are the clearing's moss (raked turf, which knits back) and, on his
 * fall, the Grey Mist on the stone.
 */
public class AlphaWerewolfEntity extends Monster implements GeoEntity {
	// ---------------------------------------------------------------- numbers (tools/alpha_werewolf.py)
	public static final float HEALTH = 400.0F;
	public static final double ARMOR = 8.0;
	public static final double LEASH = 26.0;
	public static final double PARTY_STEP = 0.5;
	public static final double PARTY_MAX = 2.5;
	public static final int WAKE_TICKS = 50;
	public static final int ABANDON_TICKS = 200;
	public static final int GLOBAL_COOLDOWN = 16;
	public static final double SPEED = 0.22;
	public static final double KEEP_DISTANCE = 2.0;
	public static final double STEP = 1.1;
	public static final float GATHER_AT = 0.5F;
	public static final float LAST_AT = 0.25F;
	public static final int GATHER_TICKS = 40;
	public static final float HIDE = 0.5F;
	public static final float FLUSHED_SILVER = 1.5F;
	public static final int HUNT_EVERY = 480;
	public static final int FURY_HUNT_EVERY = 400;
	public static final int LAST_HUNT_EVERY = 320;
	public static final int MARK_TICKS = 20;
	public static final int RUN_TICKS = 30;
	public static final int AMBUSH_TICKS = 200;
	public static final int SHADOW_TICKS = 24;
	public static final int SPRING_TICKS = 12;
	public static final float SPRING_DAMAGE = 14.0F;
	public static final double SPRING_REACH = 2.5;
	public static final int KNOCKED_TICKS = 20;
	public static final int SWITCH_AT = 60;
	public static final int STAGGER_TICKS = 30;
	public static final int FLUSHED_TICKS = 160;
	public static final int COWER_TICKS = 80;
	public static final double FLUSHED_TO = 17.0;
	public static final double DOWN_AT = 6.0;
	public static final float RAKE_DAMAGE = 6.0F;
	public static final double RAKE_REACH = 3.0;
	public static final double RAKE_HALF_ANGLE = 70.0;
	public static final int RAKE_GAP = 6;
	public static final float POUNCE_DAMAGE = 9.0F;
	public static final double POUNCE_REACH = 2.0;
	public static final double POUNCE_RANGE = 12.0;
	public static final int WEAKNESS_TICKS = 120;
	public static final double HOWL_REACH = 10.0;
	public static final int PACK_SPEED_TICKS = 120;
	public static final int TREE_TICKS = 30;
	public static final float LEAP_DAMAGE = 10.0F;
	public static final double LEAP_REACH = 2.5;
	public static final double TREE_AT = 22.0;
	public static final double TREE_UP = 11.0;
	public static final double TREE_BEHIND = 2.0;
	public static final float BITE_DAMAGE = 8.0F;
	public static final double BITE_REACH = 3.0;
	public static final int HAMSTRING_TICKS = 40;
	public static final int PACK_SIZE = 3;
	public static final int EXPERIENCE = 300;
	/** Where his pack lopes out of the trees: degrees from north, clockwise, on the path round the clearing. */
	public static final double[] PACK_ANGLES = {-60.0, 0.0, 60.0};
	public static final double PACK_RADIUS = 20.0;
	/** Ticks his leap into the trees, or down off the stone, takes. */
	private static final int LEAP_TICKS = 10;
	/** The Pack Gathers: the tick of its clip at which he howls the pack back (tools/alpha_werewolf_models.py "gather"). */
	private static final int GATHER_HOWL = 18;
	/** Waking: the tick at which his pack lopes out of the trees. */
	private static final int WAKE_HOWL = 20;
	/** The least ticks between two "his hide turns the blow" words to one player. */
	private static final int HIDE_WORDS = 40;
	/** The shadow where he will land: near black, on the moss. */
	private static final DustParticleOptions SHADOW = new DustParticleOptions(0x0C0C14, 1.8F);

	/**
	 * His attacks: wind-up, the active part and recovery in ticks, the cooldown, the target's nearest and farthest distance,
	 * and the phases he uses it in (the Chase; the Moon's Fury and the Last Hunt). The wind-up is what players read; the
	 * strike lands as it ends. The Pounce's active ticks are its leap; the Tree Leap's wind-up is his time in the trees
	 * and the shadow behind his foe.
	 */
	public enum Attack {
		CLAW_RAKE(14, 8, 10, 30, 0.0, 3.0, true, true),
		POUNCE(24, 10, 12, 120, 4.0, 12.0, true, true),
		RENDING_HOWL(20, 1, 14, 240, 0.0, 10.0, true, true),
		TREE_LEAP(54, 1, 14, 200, 0.0, 30.0, false, true),
		HAMSTRING_BITE(18, 1, 12, 80, 0.0, 3.0, false, true);

		public final int windup;
		public final int active;
		public final int recovery;
		public final int cooldown;
		public final double near;
		public final double far;
		public final boolean chase;
		public final boolean fury;

		Attack(int windup, int active, int recovery, int cooldown, double near, double far, boolean chase, boolean fury) {
			this.windup = windup;
			this.active = active;
			this.recovery = recovery;
			this.cooldown = cooldown;
			this.near = near;
			this.far = far;
			this.chase = chase;
			this.fury = fury;
		}

		public int length() {
			return windup + active + recovery;
		}
	}

	/** The fight's phases. */
	public enum Phase {
		WAITING, WAKING, CHASE, GATHERING, FURY, LAST_HUNT
	}

	/** Where his Hunt is: marking his quarry, running to ground, hiding, slipping to another clearing, springing. */
	public enum Hunt {
		NONE, MARK, RUN, HIDE, SLIP, SPRING
	}

	/** What his body is doing, synced for his animations. */
	public enum Action {
		CROUCH, WAKE, IDLE, RUN, RAKE_WINDUP, RAKE, POUNCE_WINDUP, POUNCE, HOWL_WINDUP, HOWL, MARK, HIDE, SPRING, STAGGER, BITE_WINDUP,
		BITE, GATHER
	}

	/** The leaps and runs he makes over the ground, unstopped by it: where each ends, what follows. */
	private enum Flight {
		NONE, DOWN, ONTO_STONE, RUN, SLIP, STAGGER, POUNCE, INTO_TREES, DROP, SPRING
	}

	private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(AlphaWerewolfEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> EYES = SynchedEntityData.defineId(AlphaWerewolfEntity.class, EntityDataSerializers.BOOLEAN);
	static final RawAnimation[] BODY = new RawAnimation[Action.values().length];
	static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.alpha_werewolf.walk");
	static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.alpha_werewolf.death");
	static final RawAnimation EYES_DARK = RawAnimation.begin().thenLoop("animation.alpha_werewolf.eyes_dark");
	static final RawAnimation EYES_LIT = RawAnimation.begin().thenLoop("animation.alpha_werewolf.eyes_lit");

	static {
		String prefix = "animation.alpha_werewolf.";
		BODY[Action.CROUCH.ordinal()] = RawAnimation.begin().thenLoop(prefix + "crouch");
		BODY[Action.WAKE.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "wake");
		BODY[Action.IDLE.ordinal()] = RawAnimation.begin().thenLoop(prefix + "idle");
		BODY[Action.RUN.ordinal()] = RawAnimation.begin().thenLoop(prefix + "run");
		BODY[Action.RAKE_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "rake_windup");
		BODY[Action.RAKE.ordinal()] = RawAnimation.begin().then(prefix + "rake", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.POUNCE_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "pounce_windup");
		BODY[Action.POUNCE.ordinal()] = RawAnimation.begin().then(prefix + "pounce", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.HOWL_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "howl_windup");
		BODY[Action.HOWL.ordinal()] = RawAnimation.begin().then(prefix + "howl", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.MARK.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "mark");
		BODY[Action.HIDE.ordinal()] = RawAnimation.begin().thenLoop(prefix + "hide");
		BODY[Action.SPRING.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "spring");
		BODY[Action.STAGGER.ordinal()] = RawAnimation.begin().thenLoop(prefix + "stagger");
		BODY[Action.BITE_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "bite_windup");
		BODY[Action.BITE.ordinal()] = RawAnimation.begin().then(prefix + "bite", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.GATHER.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "gather");
	}

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private final ServerBossEvent bossBar = new ServerBossEvent(UUID.randomUUID(), Component.translatable("entity.jugcraft.alpha_werewolf"),
			BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.NOTCHED_10);
	/** The lair instance he belongs to (null for one put down by a test or a command), and the ring's origin. */
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
	/** Whoever rang the bell that flushed him: participants too. */
	private final Set<UUID> helpers = new HashSet<>();
	private final Map<UUID, Long> hideWords = new HashMap<>();
	// His rule, the Hunt.
	private Hunt hunt = Hunt.NONE;
	private int huntTicks;
	private int huntIn = HUNT_EVERY;
	private int ambushTicks;
	private @Nullable UUID quarry;
	/** The clearing he hides in (null while he is not hiding), and the one he is slipping to. */
	private @Nullable Bell glade;
	private @Nullable Bell slipTo;
	private int flushedTicks;
	private int cowerTicks;
	/** Where he will land (his spring's, his Pounce's or his Tree Leap's shadow), and the tick it began to spread. */
	private Vec3 shadow = Vec3.ZERO;
	private int shadowFrom = -1;
	private double shadowReach;
	/** In the trees (the Tree Leap), unseen, and where. */
	private boolean inTrees;
	// His pack, and the turf he has raked (when).
	private final List<UUID> pack = new ArrayList<>();
	private final Map<BlockPos, Long> raked = new HashMap<>();
	// The leap or run under way.
	private Flight flight = Flight.NONE;
	private Vec3 flightFrom = Vec3.ZERO;
	private Vec3 flightTo = Vec3.ZERO;
	private int flightTicks;
	private int flightLength;
	private double flightArc;
	/** Set when he is loaded in a passing moment, or fighting: he is set down at the stone's foot. */
	private boolean landOnLoad;

	public AlphaWerewolfEntity(EntityType<? extends AlphaWerewolfEntity> type, Level level) {
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
				.add(Attributes.ATTACK_DAMAGE, RAKE_DAMAGE).add(Attributes.STEP_HEIGHT, STEP);
	}

	/**
	 * Crouches him on the Howling Stone of {@code instance}'s ring. Called as the instance is placed. Placing the template
	 * puts back only its own blocks, so the Grey Mist a fallen Alpha opened on the stone before still stands: it goes first.
	 */
	public static AlphaWerewolfEntity summon(ServerLevel level, LairInstance instance) {
		BlockPos origin = instance.origin();
		for (BlockPos cell : MoonlitRing.stoneExitCells()) {
			BlockPos at = cell.offset(origin);
			if (level.getBlockState(at).is(JugcraftLairs.LAIR_EXIT)) {
				level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		return summon(level, origin, instance);
	}

	/** Crouches him on the stone of the ring laid out from {@code origin} (tests pass no instance). */
	public static AlphaWerewolfEntity summon(ServerLevel level, BlockPos origin, @Nullable LairInstance instance) {
		AlphaWerewolfEntity alpha = new AlphaWerewolfEntity(JugcraftAlpha.ALPHA_WEREWOLF, level);
		alpha.origin = origin.immutable();
		if (instance != null) {
			alpha.instance = instance.id;
			alpha.slot = instance.slot;
		}
		alpha.noPhysics = true;
		Vec3 waiting = alpha.waiting();
		alpha.snapTo(waiting.x, waiting.y, waiting.z, 180.0F, 0.0F);
		alpha.face(180.0F);
		level.addFreshEntity(alpha);
		return alpha;
	}

	// ---------------------------------------------------------------- where things are

	/** The clearing's middle, on its floor, for the ring laid out from {@code origin}. */
	public static Vec3 centre(BlockPos origin) {
		return MoonlitRing.centre().add(origin.getX(), origin.getY(), origin.getZ());
	}

	public Vec3 centre() {
		return centre(origin);
	}

	public BlockPos origin() {
		return origin;
	}

	private Vec3 at(double x, double y, double z) {
		return new Vec3(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
	}

	/** Where he waits on the Howling Stone. */
	public Vec3 waiting() {
		Vec3 wait = MoonlitRing.waiting();
		return at(wait.x, wait.y, wait.z);
	}

	/** The clearing's floor, where he and his foes stand. */
	public double floorY() {
		return origin.getY() + MoonlitRing.STAND;
	}

	/** Where he hides in {@code bell}'s clearing: its middle. */
	public Vec3 hideSpot(Bell bell) {
		return at(bell.gladeX(), MoonlitRing.STAND, bell.gladeZ());
	}

	/** Where he staggers out to, flushed from {@code bell}'s clearing: along its spoke, {@value #FLUSHED_TO} from the centre. */
	public Vec3 flushSpot(Bell bell) {
		return at(MoonlitRing.CENTRE_X + bell.wayX * FLUSHED_TO, MoonlitRing.STAND, MoonlitRing.CENTRE_Z + bell.wayZ * FLUSHED_TO);
	}

	/** Where he leaps down off the stone: onto the spoke (of the four) nearest {@code toward}, {@value #DOWN_AT} out. */
	public Vec3 downSpot(@Nullable Entity toward) {
		double x = 0.0;
		double z = 1.0;  // the camp's spoke, south, when nobody is about
		if (toward != null) {
			double dx = toward.getX() - centre().x;
			double dz = toward.getZ() - centre().z;
			if (Math.abs(dx) > Math.abs(dz)) {
				x = Math.signum(dx);
				z = 0.0;
			} else if (Math.abs(dz) > 1.0E-3) {
				z = Math.signum(dz);
			}
		}
		return at(MoonlitRing.CENTRE_X + x * DOWN_AT, MoonlitRing.STAND, MoonlitRing.CENTRE_Z + z * DOWN_AT);
	}

	/** Where Packmate {@code index} lopes out of the trees. */
	public Vec3 packSpot(int index) {
		double rad = Math.toRadians(PACK_ANGLES[index]);
		return at(MoonlitRing.CENTRE_X + Math.sin(rad) * PACK_RADIUS, MoonlitRing.STAND, MoonlitRing.CENTRE_Z - Math.cos(rad) * PACK_RADIUS);
	}

	/** Where he lurks in the canopy, on the side of the clearing toward {@code toward}. */
	public Vec3 treeSpot(Vec3 toward) {
		Vec3 centre = centre();
		Vec3 way = new Vec3(toward.x - centre.x, 0.0, toward.z - centre.z);
		way = way.lengthSqr() < 1.0E-4 ? facing() : way.normalize();
		return new Vec3(centre.x + way.x * TREE_AT, floorY() + TREE_UP, centre.z + way.z * TREE_AT);
	}

	/** Whether (x, z) is in the clearing. */
	public boolean inRing(double x, double z) {
		return MoonlitRing.inRing(x - origin.getX(), z - origin.getZ());
	}

	/** Whether {@code entity} is in his hunting ground: the clearing, its spokes and the bell clearings (not the camp). */
	public boolean inArena(Entity entity) {
		double x = entity.getX() - origin.getX();
		double z = entity.getZ() - origin.getZ();
		double y = entity.getY() - origin.getY();
		return Math.hypot(x - MoonlitRing.CENTRE_X, z - MoonlitRing.CENTRE_Z) <= MoonlitRing.GLADE_AT + MoonlitRing.GLADE_RADIUS + 1.0
				&& !MoonlitRing.inCamp(x, z) && y >= MoonlitRing.STAND - 2 && y <= MoonlitRing.STAND + 8;
	}

	/** Whether {@code player} has stepped off the camp onto the ring's turf. */
	public boolean onTurf(Player player) {
		double y = player.getY() - origin.getY();
		return inRing(player.getX(), player.getZ()) && y >= MoonlitRing.STAND - 1 && y <= MoonlitRing.STAND + 6;
	}

	// ---------------------------------------------------------------- state

	public Phase phase() {
		return phase;
	}

	public Hunt hunt() {
		return hunt;
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

	public int huntIn() {
		return huntIn;
	}

	/** Sets the ticks to his next hunt (for tests). */
	public void huntIn(int ticks) {
		huntIn = ticks;
	}

	public int ambushTicks() {
		return ambushTicks;
	}

	/** Sets how far into the ambush he is (for tests). */
	public void ambushTicks(int ticks) {
		ambushTicks = ticks;
	}

	/** What his bar says. */
	public Component bar() {
		return bossBar.getName();
	}

	/** The clearing he hides in, or null. */
	public @Nullable Bell glade() {
		return glade;
	}

	public int flushedTicks() {
		return flushedTicks;
	}

	public int cowerTicks() {
		return cowerTicks;
	}

	/** Whether his hide has failed: flushed out. */
	public boolean flushed() {
		return flushedTicks > 0;
	}

	/** Whether his pack cowers among the trees. */
	public boolean cowering() {
		return cowerTicks > 0;
	}

	/** Whether he is up off his stone: his pack runs while he is. */
	public boolean awake() {
		return phase != Phase.WAITING;
	}

	/** Whether he is out of reach: running to ground, hiding, slipping away, or in the trees. */
	public boolean hidden() {
		return hunt == Hunt.RUN || hunt == Hunt.HIDE || hunt == Hunt.SLIP || inTrees;
	}

	/** Where his shadow falls. */
	public Vec3 shadow() {
		return shadow;
	}

	public List<UUID> pack() {
		return List.copyOf(pack);
	}

	/** The cells of raked turf he has left. */
	public Set<BlockPos> raked() {
		return Set.copyOf(raked.keySet());
	}

	public Action action() {
		int id = entityData.get(ACTION);
		return id >= 0 && id < Action.values().length ? Action.values()[id] : Action.IDLE;
	}

	private void setAction(Action action) {
		entityData.set(ACTION, action.ordinal());
	}

	/** Whether his eyes burn (synced). */
	public boolean eyesLit() {
		return entityData.get(EYES);
	}

	public @Nullable UUID instanceId() {
		return instance;
	}

	/** Whether he is fighting (down off his stone, not waiting, waking or gathering his pack). */
	public boolean fighting() {
		return phase == Phase.CHASE || phase == Phase.FURY || phase == Phase.LAST_HUNT;
	}

	/** Damage he deals: the attack's own, times {@code lairs.boss_damage} ({@link LairBosses}). */
	public static float damage(float base) {
		return LairBosses.damage(base);
	}

	/** His health for a party of {@code players}: half as much again for each beyond the first, at most 2.5 times. */
	public static double partyScale(int players) {
		return LairBosses.partyScale(players, PARTY_STEP, PARTY_MAX);
	}

	/** The ticks between his hunts in this phase. */
	public int huntEvery() {
		return phase == Phase.LAST_HUNT ? LAST_HUNT_EVERY : phase == Phase.FURY ? FURY_HUNT_EVERY : HUNT_EVERY;
	}

	/** A vanilla sound by its id, or {@code fallback} if this game has none so named. */
	static SoundEvent sound(String id, SoundEvent fallback) {
		return BuiltInRegistries.SOUND_EVENT.getOptional(Identifier.withDefaultNamespace(id)).orElse(fallback);
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
			Vec3 down = downSpot(null);
			teleportTo(down.x, down.y, down.z);
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
		if (flight != Flight.NONE) {
			tickFlight(level);
		}
		switch (phase) {
			case WAITING -> waiting(level);
			case WAKING -> waking(level);
			case GATHERING -> gathering(level);
			case CHASE, FURY, LAST_HUNT -> fight(level);
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
		face(180.0F);
		setAction(Action.CROUCH);
		if (tickCount % 10 == 0 && intruder(level) != null) {
			wake(level);
		}
	}

	/** Someone who may fight him, stepped off the camp onto the ring's turf. */
	private @Nullable Player intruder(ServerLevel level) {
		for (Player player : level.players()) {
			if (LairBosses.eligible(player) && onTurf(player)) {
				return player;
			}
		}
		return null;
	}

	/** He rises and howls at the moon: his health scaled for the players in his ring, his bar shown, his eyes lit. */
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
		entityData.set(EYES, true);
		bossBar.setName(barName());
		bossBar.setVisible(true);
		huntIn = HUNT_EVERY;
		level.playSound(null, getX(), getY(), getZ(), sound("entity.wolf.howl", SoundEvents.ZOMBIE_AMBIENT), SoundSource.HOSTILE, 6.0F, 0.5F);
		tell(level, "message.jugcraft.alpha_werewolf.wakes");
	}

	/** Waking: he howls, his pack lopes out of the trees, and he leaps down off the stone toward whoever is nearest. */
	private void waking(ServerLevel level) {
		if (flight == Flight.NONE) {
			noPhysics = true;
			setDeltaMovement(Vec3.ZERO);
		}
		if (phaseTicks == WAKE_HOWL) {
			callPack(level);
		}
		if (phaseTicks >= WAKE_TICKS) {
			phase = Phase.CHASE;
			phaseTicks = 0;
			globalCooldown = GLOBAL_COOLDOWN;
			leapDown(level);
		}
	}

	/** He leaps down off the stone onto the spoke nearest whoever is nearest. */
	private void leapDown(ServerLevel level) {
		Player nearest = nearest(level);
		setAction(Action.SPRING);
		fly(Flight.DOWN, downSpot(nearest), LEAP_TICKS, 1.5);
	}

	/** Players near enough to his ring to be in his fight (the camp too): the party he scales for. */
	public List<ServerPlayer> present(ServerLevel level) {
		List<ServerPlayer> out = new ArrayList<>();
		Vec3 centre = centre();
		for (ServerPlayer player : level.players()) {
			if (LairBosses.eligible(player) && player.position().distanceTo(centre) <= MoonlitRing.GLADE_AT + 12.0) {
				out.add(player);
			}
		}
		return out;
	}

	/** Players who may fight him and stand in his hunting ground: those he chooses among, and those his blows can reach. */
	public List<ServerPlayer> nearby(ServerLevel level) {
		List<ServerPlayer> out = new ArrayList<>();
		for (ServerPlayer player : level.players()) {
			if (LairBosses.eligible(player) && inArena(player)) {
				out.add(player);
			}
		}
		return out;
	}

	private @Nullable Player nearest(ServerLevel level) {
		Player nearest = null;
		double best = Double.MAX_VALUE;
		for (ServerPlayer player : nearby(level)) {
			double distance = player.distanceToSqr(this);
			if (distance < best) {
				best = distance;
				nearest = player;
			}
		}
		return nearest;
	}

	// ---------------------------------------------------------------- leaps and runs

	/** He leaps or runs to {@code to} over {@code ticks} ticks, arcing {@code arc} blocks up at the middle, through anything. */
	private void fly(Flight kind, Vec3 to, int ticks, double arc) {
		flight = kind;
		flightFrom = position();
		flightTo = to;
		flightTicks = 0;
		flightLength = Math.max(1, ticks);
		flightArc = arc;
		noPhysics = true;
		setDeltaMovement(Vec3.ZERO);
	}

	/** The leap or run, a tick on; running, he bounds over the stone and tears the moss he crosses. */
	private void tickFlight(ServerLevel level) {
		flightTicks++;
		double share = Math.min(1.0, flightTicks / (double) flightLength);
		Vec3 along = flightFrom.add(flightTo.subtract(flightFrom).scale(share));
		double y = along.y + flightArc * 4.0 * share * (1.0 - share);
		if (flight == Flight.RUN || flight == Flight.SLIP) {
			y = Math.max(y, floorY() + MoonlitRing.stoneHeight(along.x - origin.getX(), along.z - origin.getZ()));
			tearTurf(level, position(), along);
		}
		setDeltaMovement(Vec3.ZERO);
		setPos(along.x, y, along.z);
		face(flightTo.subtract(flightFrom));
		if (share >= 1.0) {
			Flight done = flight;
			flight = Flight.NONE;
			landed(level, done);
		}
	}

	/** A leap or run is over. */
	private void landed(ServerLevel level, Flight done) {
		if (done != Flight.RUN && done != Flight.SLIP && done != Flight.INTO_TREES) {
			settle(level);
		}
		switch (done) {
			case DOWN, ONTO_STONE, STAGGER -> noPhysics = false;
			case RUN, SLIP -> arrived(level);
			case POUNCE -> {
				noPhysics = false;
				lands(level, POUNCE_DAMAGE, POUNCE_REACH, false);
			}
			case INTO_TREES -> {
				inTrees = true;
				setInvisible(true);
			}
			case DROP -> {
				noPhysics = false;
				lands(level, LEAP_DAMAGE, LEAP_REACH, false);
			}
			case SPRING -> {
				noPhysics = false;
				lands(level, SPRING_DAMAGE, SPRING_REACH, true);
				endHunt(level);
			}
			case NONE -> {
			}
		}
		if (done == Flight.DOWN && fighting()) {
			setAction(Action.IDLE);
		}
	}

	/** Come down where a log, a boulder or the stone stands, he is set on top of it (never left inside a block). */
	private void settle(ServerLevel level) {
		for (int up = 0; up < 4 && !level.noCollision(this, getBoundingBox()); up++) {
			setPos(getX(), Math.floor(getY()) + 1.0, getZ());
		}
	}

	/** Whether he is leaping or running. */
	public boolean flying() {
		return flight != Flight.NONE;
	}

	/**
	 * He tears the ring's floor between {@code from} and {@code to} (its moss, its trodden paths, its glades' earth) into
	 * raked turf ({@link RakedTurfBlock}, which knits back by itself to what it was): a trail two blocks wide. The plants
	 * on it stand; nothing else is touched. Returns the cells torn.
	 */
	public int tearTurf(ServerLevel level, Vec3 from, Vec3 to) {
		int torn = 0;
		double length = Math.hypot(to.x - from.x, to.z - from.z);
		int steps = Math.max(1, (int) Math.ceil(length / 0.5));
		int soil = origin.getY() + MoonlitRing.SOIL;
		for (int i = 0; i <= steps; i++) {
			double x = from.x + (to.x - from.x) * i / steps;
			double z = from.z + (to.z - from.z) * i / steps;
			for (int dx = -1; dx <= 1; dx += 2) {
				for (int dz = -1; dz <= 1; dz += 2) {
					BlockPos at = BlockPos.containing(x + dx * 0.5, soil, z + dz * 0.5);
					BlockState turf = RakedTurfBlock.tornFrom(JugcraftLairs.RAKED_TURF, level.getBlockState(at));
					if (turf != null) {
						level.setBlock(at, turf, Block.UPDATE_CLIENTS);
						raked.put(at.immutable(), level.getGameTime());
						torn++;
					}
				}
			}
		}
		if (torn > 0) {
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSS_BLOCK.defaultBlockState()), to.x, soil + 1.1, to.z, 6, 0.4,
					0.1, 0.4, 0.05);
		}
		return torn;
	}

	/** The raked turf he left knits back to what it was, all of it, now. */
	public void knitTurf(ServerLevel level) {
		for (BlockPos at : raked.keySet()) {
			BlockState state = level.isLoaded(at) ? level.getBlockState(at) : null;
			if (state != null && state.is(JugcraftLairs.RAKED_TURF)) {
				level.setBlock(at, RakedTurfBlock.knitsTo(state), Block.UPDATE_CLIENTS);
			}
		}
		raked.clear();
	}

	/** He lands on his shadow: {@code base} damage within {@code reach} of it; {@code knock}, those he hits are knocked flat. */
	private int lands(ServerLevel level, float base, double reach, boolean knock) {
		int hit = 0;
		Vec3 at = shadow;
		for (ServerPlayer player : nearby(level)) {
			double flat = Math.hypot(player.getX() - at.x, player.getZ() - at.z);
			if (flat > reach + player.getBbWidth() / 2.0 || Math.abs(player.getY() - at.y) > 3.0) {
				continue;
			}
			if (player.hurtServer(level, damageSources().mobAttack(this), damage(base))) {
				hit++;
				if (knock) {
					player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, KNOCKED_TICKS, 3), this);
				}
				Vec3 away = new Vec3(player.getX() - at.x, 0.0, player.getZ() - at.z);
				away = away.lengthSqr() < 1.0E-4 ? facing() : away.normalize();
				push(player, away.scale(0.6).add(0.0, 0.25, 0.0));
			}
		}
		shadowFrom = -1;
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSS_BLOCK.defaultBlockState()), at.x, at.y + 0.1, at.z, 30, reach / 2.0,
				0.1, reach / 2.0, 0.1);
		level.sendParticles(ParticleTypes.POOF, at.x, at.y + 0.3, at.z, 12, reach / 2.0, 0.2, reach / 2.0, 0.02);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 2.5F, 0.6F);
		return hit;
	}

	/** A shadow begins to spread at {@code at} (on the floor), {@code reach} blocks across when it has spread. */
	private void castShadow(Vec3 at, double reach) {
		shadow = new Vec3(at.x, Math.max(floorY(), at.y), at.z);
		shadowFrom = tickCount;
		shadowReach = reach;
	}

	/** Sets where he will land (for tests). */
	public void shadow(Vec3 at, double reach) {
		castShadow(at, reach);
	}

	/** The shadow spreading where he will land: darker and wider as he comes. */
	private void drawShadow(ServerLevel level) {
		if (shadowFrom < 0 || (tickCount - shadowFrom) % 2 != 0) {
			return;
		}
		double share = Math.min(1.0, 0.3 + (tickCount - shadowFrom) / (double) SHADOW_TICKS);
		double reach = shadowReach * share;
		for (int i = 0; i < 12; i++) {
			double rad = Math.toRadians(30.0 * i);
			level.sendParticles(SHADOW, shadow.x + Math.cos(rad) * reach, shadow.y + 0.1, shadow.z + Math.sin(rad) * reach, 1, 0.0, 0.0, 0.0, 0.0);
		}
		level.sendParticles(SHADOW, shadow.x, shadow.y + 0.1, shadow.z, 6, reach / 2.5, 0.0, reach / 2.5, 0.0);
	}

	// ---------------------------------------------------------------- his rule: the Hunt

	/**
	 * He marks {@code target} as his quarry: a short, rising howl ({@value #MARK_TICKS} ticks), the bar names them, and his
	 * pack turns on them.
	 */
	public void startHunt(ServerLevel level, Player target) {
		attack = null;
		attackTicks = 0;
		hunt = Hunt.MARK;
		huntTicks = 0;
		quarry = target.getUUID();
		foe = quarry;
		shadowFrom = -1;
		hold();
		face(target);
		setAction(Action.MARK);
		bossBar.setName(Component.translatable("message.jugcraft.alpha_werewolf.hunt_bar", target.getDisplayName()));
		tell(level, "message.jugcraft.alpha_werewolf.hunts", target.getDisplayName());
		level.playSound(null, getX(), getY(), getZ(), sound("entity.wolf.howl", SoundEvents.ZOMBIE_AMBIENT), SoundSource.HOSTILE, 4.0F, 0.9F);
	}

	/** His quarry, while they may still be hunted; else whoever is nearest in his hunting ground (the bar renamed). */
	public @Nullable Player quarry(ServerLevel level) {
		if (quarry != null) {
			Player held = level.getPlayerByUUID(quarry);
			if (held != null && LairBosses.eligible(held) && inArena(held)) {
				return held;
			}
		}
		Player next = nearest(level);
		quarry = next == null ? null : next.getUUID();
		if (next != null && hunt != Hunt.NONE) {
			bossBar.setName(Component.translatable("message.jugcraft.alpha_werewolf.hunt_bar", next.getDisplayName()));
		}
		return next;
	}

	/** The Hunt, a tick on. */
	private void tickHunt(ServerLevel level) {
		huntTicks++;
		Player prey = quarry(level);
		if (prey == null) {
			abandonHunt(level);
			return;
		}
		switch (hunt) {
			case MARK -> {
				hold();
				face(prey);
				if (huntTicks >= MARK_TICKS) {
					goToGround(level, prey);
				}
			}
			case HIDE -> ambush(level, prey);
			case SLIP, SPRING -> {
				ambushTicks++;
				drawShadow(level);
			}
			case RUN, NONE -> {
			}
		}
	}

	/** The clearings he may run to: every one but the one nearest {@code prey}. */
	public List<Bell> gladesFor(Player prey) {
		Bell nearest = null;
		double best = Double.MAX_VALUE;
		for (Bell bell : Bell.values()) {
			double distance = prey.position().distanceToSqr(hideSpot(bell));
			if (distance < best) {
				best = distance;
				nearest = bell;
			}
		}
		List<Bell> out = new ArrayList<>(List.of(Bell.values()));
		out.remove(nearest);
		return out;
	}

	/** He bounds off into the trees to hide in a clearing, never the one nearest his quarry, tearing the turf as he goes. */
	public void goToGround(ServerLevel level, Player prey) {
		List<Bell> glades = gladesFor(prey);
		runTo(level, glades.get(getRandom().nextInt(glades.size())), Flight.RUN);
		hunt = Hunt.RUN;
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 2.0F, 0.8F);
	}

	private void runTo(ServerLevel level, Bell bell, Flight kind) {
		slipTo = bell;
		glade = null;
		setAction(Action.RUN);
		fly(kind, hideSpot(bell), RUN_TICKS, 0.8);
	}

	/** He reaches the clearing he ran to, and lies in wait there (the ambush's clock starts when he first does). */
	private void arrived(ServerLevel level) {
		boolean first = hunt == Hunt.RUN;
		glade = slipTo;
		slipTo = null;
		hunt = Hunt.HIDE;
		setAction(Action.HIDE);
		face(centre().subtract(position()));
		if (first) {
			ambushTicks = 0;
			tell(level, "message.jugcraft.alpha_werewolf.hidden");
		}
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, JugcraftLairs.MOONWOOD_LEAVES.defaultBlockState()), getX(), getY() + 2.0,
				getZ(), 16, 1.0, 1.0, 1.0, 0.1);
	}

	/** Lying in wait: in the Last Hunt he slips away halfway; the shadow spreads under his quarry; then he springs. */
	private void ambush(ServerLevel level, Player prey) {
		ambushTicks++;
		hold();
		if (phase == Phase.LAST_HUNT && ambushTicks == SWITCH_AT && glade != null) {
			slip(level);
			return;
		}
		if (ambushTicks == AMBUSH_TICKS - SHADOW_TICKS) {
			castShadow(prey.position(), SPRING_REACH);
			level.playSound(null, prey.getX(), prey.getY(), prey.getZ(), sound("entity.wolf.growl", SoundEvents.ZOMBIE_AMBIENT), SoundSource.HOSTILE,
					2.5F, 0.5F);
		}
		drawShadow(level);
		if (ambushTicks >= AMBUSH_TICKS - SPRING_TICKS) {
			spring(level);
		}
	}

	/** The Last Hunt: he slips from his clearing through the trees to another, leaving a second trail. */
	public void slip(ServerLevel level) {
		List<Bell> others = new ArrayList<>(List.of(Bell.values()));
		others.remove(glade);
		runTo(level, others.get(getRandom().nextInt(others.size())), Flight.SLIP);
		hunt = Hunt.SLIP;
		tell(level, "message.jugcraft.alpha_werewolf.slips");
	}

	/** He springs from his clearing onto the shadow. */
	public void spring(ServerLevel level) {
		if (shadowFrom < 0) {
			Player prey = quarry(level);
			castShadow(prey != null ? prey.position() : downSpot(null), SPRING_REACH);
		}
		glade = null;
		hunt = Hunt.SPRING;
		setAction(Action.SPRING);
		fly(Flight.SPRING, shadow, SPRING_TICKS, 4.0);
		level.playSound(null, getX(), getY(), getZ(), sound("entity.wolf.growl", SoundEvents.ZOMBIE_AMBIENT), SoundSource.HOSTILE, 3.0F, 0.4F);
	}

	/** The Hunt is over: the next waits, and he fights on. */
	private void endHunt(ServerLevel level) {
		hunt = Hunt.NONE;
		huntTicks = 0;
		ambushTicks = 0;
		glade = null;
		slipTo = null;
		huntIn = huntEvery();
		globalCooldown = GLOBAL_COOLDOWN;
		setAction(Action.IDLE);
		bossBar.setName(barName());
	}

	/** Nobody left to hunt: he comes back out of the trees, and the hunt is off. */
	private void abandonHunt(ServerLevel level) {
		boolean away = hidden();
		Bell from = glade != null ? glade : slipTo;
		flight = Flight.NONE;
		endHunt(level);
		shadowFrom = -1;
		if (away) {
			Vec3 back = from != null ? flushSpot(from) : downSpot(null);
			teleportTo(back.x, back.y, back.z);
		}
		noPhysics = false;
	}

	/**
	 * {@code player} rang the bell at {@code pos}: if it is his clearing's and he lies in wait there, he is flushed; any
	 * other bell rung while he fights tolls over an empty clearing. Returns whether this fight answered it.
	 */
	public boolean bellRung(ServerLevel level, BlockPos pos, Player player) {
		if (!fighting()) {
			return false;
		}
		Bell bell = MoonlitRing.bellAt(origin, pos);
		if (bell == null) {
			return false;
		}
		if (hunt == Hunt.HIDE && glade == bell) {
			flush(level, bell, player);
		} else {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.alpha_werewolf.empty_bell"));
		}
		return true;
	}

	/**
	 * Flushed from {@code bell}'s clearing: he staggers out along its spoke for {@value #STAGGER_TICKS} ticks, his hide fails
	 * for {@value #FLUSHED_TICKS}, and his pack cowers for {@value #COWER_TICKS}. Whoever rang it takes part.
	 */
	public void flush(ServerLevel level, Bell bell, @Nullable Player ringer) {
		if (ringer != null) {
			helpers.add(ringer.getUUID());
		}
		hunt = Hunt.NONE;
		huntTicks = 0;
		ambushTicks = 0;
		glade = null;
		shadowFrom = -1;
		flushedTicks = FLUSHED_TICKS;
		cowerTicks = COWER_TICKS;
		staggerTicks = STAGGER_TICKS;
		huntIn = huntEvery();
		setAction(Action.STAGGER);
		fly(Flight.STAGGER, flushSpot(bell), STAGGER_TICKS, 0.6);
		bossBar.setName(Component.translatable("message.jugcraft.alpha_werewolf.flushed_bar"));
		tell(level, "message.jugcraft.alpha_werewolf.flushed");
		level.playSound(null, getX(), getY(), getZ(), sound("entity.wolf.whine", SoundEvents.ZOMBIE_AMBIENT), SoundSource.HOSTILE, 3.0F, 0.5F);
	}

	/** His bar's name, out of a hunt or a flush. */
	private Component barName() {
		return switch (phase) {
			case FURY -> Component.translatable("message.jugcraft.alpha_werewolf.fury_bar");
			case LAST_HUNT -> Component.translatable("message.jugcraft.alpha_werewolf.last_bar");
			default -> Component.translatable("entity.jugcraft.alpha_werewolf");
		};
	}

	// ---------------------------------------------------------------- fighting

	private void fight(ServerLevel level) {
		if (flushedTicks > 0 && --flushedTicks == 0 && hunt == Hunt.NONE) {
			bossBar.setName(barName());
		}
		if (cowerTicks > 0) {
			cowerTicks--;
		}
		if (hunt != Hunt.NONE) {
			tickHunt(level);
			return;
		}
		Player target = target(level);
		if (attack != null) {
			if (target == null && !flying() && !inTrees) {
				attack = null;
			} else {
				attackTicks++;
				perform(level, target);
				return;
			}
		}
		if (staggerTicks > 0) {
			staggerTicks--;
			hold();
			if (staggerTicks == 0) {
				setAction(Action.IDLE);
				globalCooldown = GLOBAL_COOLDOWN;
			}
			return;
		}
		if (flying()) {
			return;
		}
		keepToTheRing(level);
		if (phase == Phase.CHASE && getHealth() <= getMaxHealth() * GATHER_AT) {
			gather(level);
			return;
		}
		if (phase == Phase.FURY && getHealth() <= getMaxHealth() * LAST_AT) {
			lastHunt(level);
		}
		if (target == null) {
			setAction(Action.IDLE);
			move(centre(), SPEED);
			if (++abandoned >= ABANDON_TICKS) {
				reset(level);
			}
			return;
		}
		abandoned = 0;
		if (huntIn > 0) {
			huntIn--;
		}
		if (huntIn <= 0) {
			startHunt(level, target);
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

	/** Strayed farther than his leash from the clearing's middle, or fallen below its floor, he is set back in it. */
	private void keepToTheRing(ServerLevel level) {
		Vec3 centre = centre();
		double dx = getX() - centre.x;
		double dz = getZ() - centre.z;
		if (dx * dx + dz * dz <= LEASH * LEASH && getY() > centre.y - 3.0) {
			return;
		}
		level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1.5, getZ(), 20, 0.8, 1.0, 0.8, 0.02);
		attack = null;
		noPhysics = false;
		Vec3 down = downSpot(target(level));
		teleportTo(down.x, down.y, down.z);
		level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1.5, getZ(), 20, 0.8, 1.0, 0.8, 0.02);
	}

	/** His target: the one he has while they are still in his hunting ground, else the nearest who may fight him there. */
	private @Nullable Player target(ServerLevel level) {
		if (foe != null) {
			Player held = level.getPlayerByUUID(foe);
			if (held != null && LairBosses.eligible(held) && inArena(held)) {
				return held;
			}
		}
		Player nearest = nearest(level);
		foe = nearest == null ? null : nearest.getUUID();
		return nearest;
	}

	/** His foe now, if he has one. */
	public @Nullable Player foe(ServerLevel level) {
		return foe == null ? null : level.getPlayerByUUID(foe);
	}

	/** Whom his pack is after: his quarry in a hunt, his foe otherwise. */
	public @Nullable Player packTarget(ServerLevel level) {
		if (hunt != Hunt.NONE && quarry != null) {
			Player prey = level.getPlayerByUUID(quarry);
			if (prey != null && LairBosses.eligible(prey) && inArena(prey)) {
				return prey;
			}
		}
		Player held = foe(level);
		return held != null && LairBosses.eligible(held) && inArena(held) ? held : null;
	}

	/** Lopes toward a point {@value #KEEP_DISTANCE} blocks short of {@code target}. */
	private void approach(Player target) {
		Vec3 to = target.position().subtract(position());
		double flat = Math.sqrt(to.x * to.x + to.z * to.z);
		Vec3 goal = flat > KEEP_DISTANCE + 0.5
				? new Vec3(target.getX() - to.x / flat * KEEP_DISTANCE, getY(), target.getZ() - to.z / flat * KEEP_DISTANCE)
				: position();
		move(goal, SPEED);
	}

	/** Lopes toward {@code goal} (never past his leash) at {@code speed}, pressed down onto the floor (not while he leaps or hides). */
	private void move(Vec3 goal, double speed) {
		if (noPhysics) {
			setDeltaMovement(Vec3.ZERO);
			return;
		}
		Vec3 centre = centre();
		Vec3 out = new Vec3(goal.x - centre.x, 0.0, goal.z - centre.z);
		if (out.length() > LEASH - 1.0) {
			out = out.normalize().scale(LEASH - 1.0);
			goal = new Vec3(centre.x + out.x, goal.y, centre.z + out.z);
		}
		Vec3 to = new Vec3(goal.x - getX(), 0.0, goal.z - getZ());
		double length = to.length();
		Vec3 step = length < 0.05 ? Vec3.ZERO : to.scale(Math.min(speed, length) / length);
		setDeltaMovement(step.x, -0.3, step.z);
	}

	/** Holds still where he is, pressed down onto what he stands on (or still in the air, leaping or hiding). */
	private void hold() {
		setDeltaMovement(noPhysics ? Vec3.ZERO : new Vec3(0.0, -0.3, 0.0));
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

	/** Picks an attack he can use now on {@code target}, or null. */
	private @Nullable Attack choose(Player target) {
		double distance = target.distanceTo(this);
		List<Attack> ready = new ArrayList<>();
		for (Attack candidate : Attack.values()) {
			boolean phaseAllows = phase == Phase.CHASE ? candidate.chase : candidate.fury;
			if (!phaseAllows || cooldowns[candidate.ordinal()] > 0 || distance < candidate.near || distance > candidate.far) {
				continue;
			}
			ready.add(candidate);
			if (candidate == Attack.CLAW_RAKE) {
				ready.add(candidate);  // close in, his claws come most often
			}
		}
		return ready.isEmpty() ? null : ready.get(getRandom().nextInt(ready.size()));
	}

	/** Starts {@code next} on {@code target}: its wind-up begins. */
	public void begin(ServerLevel level, Attack next, Player target) {
		attack = next;
		attackTicks = 0;
		foe = target.getUUID();
		cooldowns[next.ordinal()] = next.cooldown;
		hold();
		face(target);
		switch (next) {
			case CLAW_RAKE -> {
				setAction(Action.RAKE_WINDUP);
				level.playSound(null, getX(), getY(), getZ(), sound("entity.wolf.growl", SoundEvents.ZOMBIE_AMBIENT), SoundSource.HOSTILE, 2.0F, 0.6F);
			}
			case POUNCE -> {
				setAction(Action.POUNCE_WINDUP);
				Vec3 to = target.position().subtract(position());
				double flat = Math.hypot(to.x, to.z);
				Vec3 mark = flat <= POUNCE_RANGE ? target.position() : position().add(to.x / flat * POUNCE_RANGE, 0.0, to.z / flat * POUNCE_RANGE);
				castShadow(mark, POUNCE_REACH);
			}
			case RENDING_HOWL -> {
				setAction(Action.HOWL_WINDUP);
				level.playSound(null, getX(), getY(), getZ(), sound("entity.wolf.growl", SoundEvents.ZOMBIE_AMBIENT), SoundSource.HOSTILE, 2.5F, 0.4F);
			}
			case TREE_LEAP -> {
				setAction(Action.SPRING);
				fly(Flight.INTO_TREES, treeSpot(target.position()), LEAP_TICKS, 2.0);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 2.0F, 0.9F);
			}
			case HAMSTRING_BITE -> {
				setAction(Action.BITE_WINDUP);
				level.playSound(null, getX(), getY(), getZ(), sound("entity.wolf.growl", SoundEvents.ZOMBIE_AMBIENT), SoundSource.HOSTILE, 2.0F, 0.8F);
			}
		}
	}

	private void perform(ServerLevel level, @Nullable Player target) {
		Attack current = attack;
		if (current == null) {
			return;
		}
		int t = attackTicks;
		hold();
		switch (current) {
			case CLAW_RAKE -> {
				if (t < current.windup) {
					if (target != null) {
						face(target);
					}
				} else if (t == current.windup) {
					setAction(Action.RAKE);
					clawRake(level);
				} else if (t == current.windup + RAKE_GAP) {
					clawRake(level);
				}
			}
			case POUNCE -> {
				if (t < current.windup) {
					face(shadow.subtract(position()));
					drawShadow(level);
				} else if (t == current.windup) {
					setAction(Action.POUNCE);
					fly(Flight.POUNCE, shadow, current.active, 2.5);
				} else if (flying()) {
					drawShadow(level);
				}
			}
			case RENDING_HOWL -> {
				if (t < current.windup) {
					if (t % 4 == 0) {
						level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 2.8, getZ(), 1, 0.3, 0.1, 0.3, 0.01);
					}
				} else if (t == current.windup) {
					setAction(Action.HOWL);
					rendingHowl(level);
				}
			}
			case TREE_LEAP -> treeLeap(level, current, t, target);
			case HAMSTRING_BITE -> {
				if (t < current.windup) {
					if (target != null) {
						face(target);
					}
				} else if (t == current.windup) {
					setAction(Action.BITE);
					if (target != null) {
						hamstring(level, target);
					}
				}
			}
		}
		if (attack == current && attackTicks >= current.length() && !flying()) {
			attack = null;
			attackTicks = 0;
			globalCooldown = GLOBAL_COOLDOWN;
			Action action = action();
			if (action == Action.SPRING || action.name().endsWith("_WINDUP")) {
				setAction(Action.IDLE);
			}
		}
	}

	/**
	 * Claw Rake: every player within {@value #RAKE_REACH} blocks before him (within {@value #RAKE_HALF_ANGLE} degrees either
	 * side of his face) takes {@value #RAKE_DAMAGE}. He rakes twice, {@value #RAKE_GAP} ticks apart.
	 */
	public int clawRake(ServerLevel level) {
		int hit = 0;
		Vec3 facing = facing();
		for (ServerPlayer player : nearby(level)) {
			Vec3 to = player.position().subtract(position());
			double flat = Math.sqrt(to.x * to.x + to.z * to.z);
			if (flat > RAKE_REACH + player.getBbWidth() / 2.0 || Math.abs(to.y) > 3.0) {
				continue;
			}
			double cos = flat < 1.0E-3 ? 1.0 : (to.x * facing.x + to.z * facing.z) / flat;
			if (cos < Math.cos(Math.toRadians(RAKE_HALF_ANGLE))) {
				continue;  // behind him or to his side
			}
			if (player.hurtServer(level, damageSources().mobAttack(this), damage(RAKE_DAMAGE))) {
				hit++;
			}
		}
		Vec3 claws = position().add(facing.scale(1.8)).add(0.0, 1.6, 0.0);
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, claws.x, claws.y, claws.z, 1, 0.0, 0.0, 0.0, 0.0);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5F, 0.6F);
		return hit;
	}

	/**
	 * Rending Howl: Weakness for {@value #WEAKNESS_TICKS} ticks on every player within {@value #HOWL_REACH} blocks, and his
	 * pack runs faster for {@value #PACK_SPEED_TICKS}. Returns how many players it weakened.
	 */
	public int rendingHowl(ServerLevel level) {
		int hit = 0;
		for (ServerPlayer player : nearby(level)) {
			if (player.distanceTo(this) <= HOWL_REACH && player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_TICKS, 0), this)) {
				hit++;
			}
		}
		for (UUID id : pack) {
			if (level.getEntity(id) instanceof PackmateEntity packmate) {
				packmate.addEffect(new MobEffectInstance(MobEffects.SPEED, PACK_SPEED_TICKS, 1), this);
			}
		}
		level.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + 2.8, getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		level.playSound(null, getX(), getY(), getZ(), sound("entity.wolf.howl", SoundEvents.ZOMBIE_AMBIENT), SoundSource.HOSTILE, 5.0F, 0.6F);
		return hit;
	}

	/**
	 * Tree Leap, {@code t} ticks into it: he leaps into the canopy on his foe's side and is gone, the leaves shivering where
	 * he is; {@value #TREE_TICKS} ticks in, a shadow spreads {@value #TREE_BEHIND} blocks behind his foe; the last
	 * {@value #SPRING_TICKS} ticks of the wind-up he drops onto it: {@value #LEAP_DAMAGE} within {@value #LEAP_REACH}.
	 */
	private void treeLeap(ServerLevel level, Attack current, int t, @Nullable Player target) {
		if (inTrees && t % 3 == 0) {
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, JugcraftLairs.MOONWOOD_LEAVES.defaultBlockState()), getX(), getY() + 1.0,
					getZ(), 6, 1.5, 0.8, 1.5, 0.05);
		}
		if (t == TREE_TICKS) {
			Vec3 behind = target != null ? behind(target) : downSpot(null);
			castShadow(behind, LEAP_REACH);
			Vec3 perch = treeSpot(behind);
			setPos(perch.x, perch.y, perch.z);
			level.playSound(null, perch.x, perch.y, perch.z, SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.HOSTILE, 2.0F, 0.7F);
		}
		if (t >= TREE_TICKS && t < current.windup) {
			drawShadow(level);
		}
		if (t == current.windup - SPRING_TICKS) {
			inTrees = false;
			setInvisible(false);
			setAction(Action.POUNCE);
			fly(Flight.DROP, shadow, SPRING_TICKS, 1.0);
		}
	}

	/** Where his Tree Leap's shadow falls: {@value #TREE_BEHIND} blocks behind {@code target}, in the clearing. */
	public Vec3 behind(Player target) {
		Vec3 look = target.getLookAngle();
		Vec3 back = new Vec3(look.x, 0.0, look.z);
		back = back.lengthSqr() < 1.0E-4 ? Vec3.ZERO : back.normalize().scale(-TREE_BEHIND);
		Vec3 at = target.position().add(back);
		return inRing(at.x, at.z) ? at : target.position();
	}

	/** Hamstring Bite: {@value #BITE_DAMAGE} to {@code target} within {@value #BITE_REACH}, and Slowness II. */
	public boolean hamstring(ServerLevel level, Player target) {
		double flat = Math.hypot(target.getX() - getX(), target.getZ() - getZ());
		level.playSound(null, getX(), getY(), getZ(), sound("entity.wolf.growl", SoundEvents.ZOMBIE_AMBIENT), SoundSource.HOSTILE, 2.0F, 1.0F);
		if (flat > BITE_REACH + target.getBbWidth() / 2.0 || Math.abs(target.getY() - getY()) > 3.0) {
			return false;
		}
		if (!target.hurtServer(level, damageSources().mobAttack(this), damage(BITE_DAMAGE))) {
			return false;
		}
		target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, HAMSTRING_TICKS, 1), this);
		return true;
	}

	/** He reels: any attack under way is spoiled, and he does nothing for {@code ticks}. */
	public void stagger(int ticks) {
		attack = null;
		attackTicks = 0;
		staggerTicks = Math.max(staggerTicks, ticks);
		setAction(Action.STAGGER);
	}

	/** Sets {@code player} moving at {@code motion}, and tells their client (a player's own motion is theirs to move). */
	static void push(ServerPlayer player, Vec3 motion) {
		player.setDeltaMovement(motion);
		player.connection.send(new ClientboundSetEntityMotionPacket(player));
	}

	// ---------------------------------------------------------------- the Pack Gathers, and the Last Hunt

	/** The Pack Gathers: he leaps onto the stone and howls long at the moon, unhurt; any attack or hunt is spent. */
	public void gather(ServerLevel level) {
		attack = null;
		staggerTicks = 0;
		if (inTrees) {
			inTrees = false;
			setInvisible(false);
		}
		hunt = Hunt.NONE;
		glade = null;
		shadowFrom = -1;
		phase = Phase.GATHERING;
		phaseTicks = 0;
		setAction(Action.GATHER);
		fly(Flight.ONTO_STONE, waiting(), 12, 2.5);
		tell(level, "message.jugcraft.alpha_werewolf.gathers");
	}

	/** The Pack Gathers, a tick on: he howls his pack back up to full, and leaps down into the Moon's Fury. */
	private void gathering(ServerLevel level) {
		if (!flying()) {
			setDeltaMovement(Vec3.ZERO);
		}
		if (phaseTicks == GATHER_HOWL) {
			callPack(level);
		}
		if (phaseTicks >= GATHER_TICKS) {
			phase = Phase.FURY;
			phaseTicks = 0;
			huntIn = Math.min(huntIn, FURY_HUNT_EVERY);
			globalCooldown = GLOBAL_COOLDOWN;
			bossBar.setName(barName());
			leapDown(level);
		}
	}

	/** The Last Hunt: he will change his route in every ambush, and hunts more often. */
	public void lastHunt(ServerLevel level) {
		phase = Phase.LAST_HUNT;
		phaseTicks = 0;
		huntIn = Math.min(huntIn, LAST_HUNT_EVERY);
		bossBar.setName(barName());
		tell(level, "message.jugcraft.alpha_werewolf.last_hunt");
	}

	// ---------------------------------------------------------------- his pack

	/** He howls his pack up to {@value #PACK_SIZE}: Packmates lope out of the trees where they are missing. */
	public int callPack(ServerLevel level) {
		prune(level);
		int added = 0;
		for (int i = 0; i < PACK_ANGLES.length && pack.size() < PACK_SIZE; i++) {
			pack.add(PackmateEntity.lope(level, this, packSpot(i)).getUUID());
			added++;
		}
		level.playSound(null, getX(), getY(), getZ(), sound("entity.wolf.howl", SoundEvents.ZOMBIE_AMBIENT), SoundSource.HOSTILE, 6.0F, 0.45F);
		return added;
	}

	/** Forgets the fallen, and the turf that has knit back. */
	private void prune(ServerLevel level) {
		pack.removeIf(id -> !(level.getEntity(id) instanceof PackmateEntity packmate) || !packmate.isAlive());
		if (tickCount % 20 == 0) {
			long now = level.getGameTime();
			Iterator<Map.Entry<BlockPos, Long>> it = raked.entrySet().iterator();
			while (it.hasNext()) {
				if (now - it.next().getValue() > RakedTurfBlock.KNIT_TICKS) {
					it.remove();
				}
			}
		}
	}

	/** Whoever hurt him or his pack has taken part. */
	public void took(ServerPlayer player, float amount) {
		damageBy.merge(player.getUUID(), amount, Float::sum);
	}

	/** His participants: everyone who hurt him or his pack, or rang the bell that flushed him, and is near. */
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

	/** Left alone: he slinks back to his stone, healed; his pack melts into the trees, his trail knits, and who hurt him is forgotten. */
	public void reset(ServerLevel level) {
		dismiss(level);
		knitTurf(level);
		phase = Phase.WAITING;
		phaseTicks = 0;
		attack = null;
		abandoned = 0;
		staggerTicks = 0;
		foe = null;
		quarry = null;
		hunt = Hunt.NONE;
		glade = null;
		slipTo = null;
		flight = Flight.NONE;
		inTrees = false;
		setInvisible(false);
		shadowFrom = -1;
		flushedTicks = 0;
		cowerTicks = 0;
		huntIn = HUNT_EVERY;
		damageBy.clear();
		helpers.clear();
		Arrays.fill(cooldowns, 0);
		setHealth(getMaxHealth());
		entityData.set(EYES, false);
		noPhysics = true;
		Vec3 waiting = waiting();
		teleportTo(waiting.x, waiting.y, waiting.z);
		face(180.0F);
		setAction(Action.CROUCH);
		bossBar.setName(Component.translatable("entity.jugcraft.alpha_werewolf"));
		bossBar.setVisible(false);
		tell(level, "message.jugcraft.alpha_werewolf.reset");
	}

	/** His pack melts into the trees. */
	private void dismiss(ServerLevel level) {
		for (UUID id : List.copyOf(pack)) {
			if (level.getEntity(id) instanceof PackmateEntity packmate) {
				packmate.meltAway(level);
			}
		}
		pack.clear();
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level) {
			defeated(level);
		}
	}

	/**
	 * He has fallen: his pack melts away, each participant gets their loot, his trail knits, his lair's instance ends (it
	 * closes once everyone has left), and Grey Mist opens on the Howling Stone.
	 */
	private void defeated(ServerLevel level) {
		dismiss(level);
		AlphaWerewolfLoot.reward(level, this, participants(level));
		knitTurf(level);
		inTrees = false;
		setInvisible(false);
		entityData.set(EYES, false);
		level.playSound(null, getX(), getY(), getZ(), sound("entity.wolf.howl", SoundEvents.ZOMBIE_AMBIENT), SoundSource.HOSTILE, 6.0F, 0.35F);
		LairInstance open = lairInstance(level);
		if (open != null) {
			Lairs.end(open);
			BlockState mist = JugcraftLairs.LAIR_EXIT.defaultBlockState().setValue(LairExitBlock.AXIS, Direction.Axis.X);
			for (BlockPos cell : MoonlitRing.stoneExitCells()) {
				level.setBlock(cell.offset(origin), mist, Block.UPDATE_ALL);
			}
		}
		bossBar.setVisible(false);
		tell(level, "message.jugcraft.alpha_werewolf.defeated");
	}

	/** Tells everyone near his ring. */
	void tell(ServerLevel level, String key, Object... args) {
		Vec3 centre = centre();
		for (ServerPlayer player : level.players()) {
			if (player.position().distanceTo(centre) <= 64.0) {
				player.sendSystemMessage(Component.translatable(key, args));
			}
		}
	}

	// ---------------------------------------------------------------- what he is immune to, and his hide

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
		if (phase == Phase.WAKING || phase == Phase.GATHERING || hidden()) {
			return false;
		}
		boolean silver = Werewolf.silver(source);
		float dealt;
		if (flushed()) {
			dealt = silver ? amount * FLUSHED_SILVER : amount;
		} else if (silver) {
			dealt = amount;
		} else {
			dealt = amount * HIDE;
			hideTurns(level, source);
		}
		boolean hurt = super.hurtServer(level, source, dealt);
		if (hurt && source.getEntity() instanceof ServerPlayer player) {
			took(player, dealt);
		}
		return hurt;
	}

	/** A blow that is not silver's on his hide: the striker is told why it bit so little (now and then). */
	private void hideTurns(ServerLevel level, DamageSource source) {
		if (source.getEntity() instanceof ServerPlayer player) {
			long now = level.getGameTime();
			Long last = hideWords.get(player.getUUID());
			if (last == null || now - last >= HIDE_WORDS || now < last) {
				hideWords.put(player.getUUID(), now);
				player.sendOverlayMessage(Component.translatable("message.jugcraft.alpha_werewolf.hide"));
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
	protected @Nullable SoundEvent getAmbientSound() {
		return phase == Phase.WAITING || hidden() ? null : sound("entity.wolf.growl", SoundEvents.ZOMBIE_AMBIENT);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return sound("entity.wolf.hurt", SoundEvents.ZOMBIE_HURT);
	}

	@Override
	protected SoundEvent getDeathSound() {
		return sound("entity.wolf.howl", SoundEvents.ZOMBIE_DEATH);
	}

	@Override
	public float getVoicePitch() {
		return 0.45F;
	}

	// ---------------------------------------------------------------- saving and syncing

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ACTION, Action.CROUCH.ordinal());
		builder.define(EYES, false);
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
		origin = input.read("origin", BlockPos.CODEC).orElse(BlockPos.containing(getX() - MoonlitRing.WAIT_X, getY() - MoonlitRing.WAIT_Y,
				getZ() - MoonlitRing.WAIT_Z));
		try {
			phase = Phase.valueOf(input.getStringOr("phase", Phase.WAITING.name()));
		} catch (IllegalArgumentException e) {
			phase = Phase.WAITING;
		}
		if (phase == Phase.WAKING || phase == Phase.GATHERING) {
			// A passing moment is not resumed: he fights on.
			phase = phase == Phase.WAKING ? Phase.CHASE : Phase.FURY;
		}
		// His hunt, his leaps and his pack are not saved: loaded fighting, he is set down at the stone's foot.
		landOnLoad = fighting();
		setInvisible(false);
		noPhysics = phase == Phase.WAITING;
		entityData.set(EYES, phase != Phase.WAITING);
		bossBar.setVisible(phase != Phase.WAITING);
		bossBar.setName(barName());
	}

	// ---------------------------------------------------------------- his animations

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<AlphaWerewolfEntity>("body", 3, test -> {
			AlphaWerewolfEntity alpha = test.animatable();
			if (alpha.isDeadOrDying()) {
				return test.setAndContinue(DEATH);
			}
			Action action = alpha.action();
			return test.setAndContinue(action == Action.IDLE && test.isMoving() ? WALK : BODY[action.ordinal()]);
		}));
		controllers.add(new AnimationController<AlphaWerewolfEntity>("eyes", 2,
				test -> test.setAndContinue(test.animatable().eyesLit() && !test.animatable().isDeadOrDying() ? EYES_LIT : EYES_DARK)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
