package io.github.jimbozoomer.jugcraft.lair.king;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.LoopType;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.lair.Crowfield;
import io.github.jimbozoomer.jugcraft.lair.Crowfield.Hill;
import io.github.jimbozoomer.jugcraft.lair.Encounter;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.KingsGourdBlock;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairBosses;
import io.github.jimbozoomer.jugcraft.lair.LairExitBlock;
import io.github.jimbozoomer.jugcraft.lair.LairInstance;
import io.github.jimbozoomer.jugcraft.lair.LairRewards;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.weapons.ArmItem;
import io.github.jimbozoomer.jugcraft.weapons.ArmVariants;
import io.github.jimbozoomer.jugcraft.weapons.JugcraftArms;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Pumpkin King (docs/features/pumpkin-king.md): the Crowfield's boss, the oldest scarecrow, the one the crows answer:
 * three times the height of a man, stuffed with straw into a patched harvest coat of burlap and sacking, hands of
 * twisted corn husk, a crown of dried vines and corn ears, and for a head a giant carved pumpkin with a fire inside. He
 * hangs on his post at the field's centre, arms out on the crossbar, his head dark, and wakes when someone who may fight
 * him steps through the farm gate into the field, or strikes him: he tears free of the post (unhurt), his health scaled
 * for the party, his head bursts into flame, and the crows burst out of the corn.
 *
 * <p>Straw burns: a blow of fire (burning, a fire weapon, a burning arrow) does {@value #FIRE} times its damage. His
 * rule, the Harvest: every {@value #HARVEST_EVERY} ticks ({@value #HALLOWED_EVERY} in the Hallowed Harvest,
 * {@value #LAST_EVERY} in the Last Harvest) he raises his arms and the crows cry ({@value #CALL_TICKS} ticks), and
 * {@value #REAPING_GOURDS} King's Gourds ({@value #HALLOWED_GOURDS} from the Crowning on) start to ripen: the one nearest
 * the player he fights and the next round the ring. Any player may pick a ripening gourd ({@link KingsGourdBlock}); one
 * that ripens ({@value #RIPEN_TICKS} ticks) lights, and {@value #SPLIT_TICKS} ticks later splits: a Jack-o'-Straw
 * ({@link JackOStrawEntity}, {@value #JACKS_MAX} at most) climbs out, or its fire feeds him. Every gourd of a harvest
 * picked before any lights leaves him bereft: he sags ({@value #STAGGER_TICKS} ticks), his fire gutters, his Jacks stand
 * dazed, and for {@value #BEREFT_TICKS} ticks every blow does {@value #BEREFT} times its damage (fire
 * {@value #BEREFT_FIRE}). At {@value #CROWN_AT} of his health, the Crowning: he climbs back onto his post, his head flares
 * white (unhurt) and two Jacks climb out of the rows; the Hallowed Harvest follows, with the Vine Snare and the Head Roll
 * besides. Below {@value #LAST_AT}, the Last Harvest: he calls it more often, his fire white.
 *
 * <p>Every attack has a wind-up you can read, a strike and a recovery ({@link Attack}); the numbers are here and in
 * tools/pumpkin_king.py. Poison, Wither, drowning, knockback and falls never harm or move him. Each player who qualified
 * in his fight ({@link Encounter}: hurt him or his Jacks, healed or warded others, or picked a gourd of a harvest that
 * left him bereft) is paid when he falls ({@link PumpkinKingLoot}, through lair/LairRewards). Left alone he hangs himself
 * back on his post, healed; his Jacks crumble, his gourds go green, and who hurt him is forgotten. Bound to a lair
 * instance he vanishes with it; one with no instance (a test's) keeps to the field laid out from the origin he was given.
 * The only blocks he changes are his gourds and, on his fall, the Grey Mist by his post.
 */
public class PumpkinKingEntity extends Monster implements GeoEntity {
	// ---------------------------------------------------------------- numbers (tools/pumpkin_king.py)
	public static final float HEALTH = 400.0F;
	public static final double ARMOR = 6.0;
	public static final double LEASH = 26.0;
	public static final double PARTY_STEP = 0.5;
	public static final double PARTY_MAX = 2.5;
	public static final int WAKE_TICKS = 50;
	public static final int ABANDON_TICKS = 200;
	public static final int GLOBAL_COOLDOWN = 16;
	public static final double SPEED = 0.2;
	public static final double KEEP_DISTANCE = 2.5;
	public static final double STEP = 2.1;
	public static final float CROWN_AT = 0.5F;
	public static final float LAST_AT = 0.25F;
	public static final int CROWNING_TICKS = 40;
	public static final int CROWN_JACKS = 2;
	public static final float FIRE = 1.5F;
	public static final float BEREFT = 1.5F;
	public static final float BEREFT_FIRE = 2.0F;
	public static final double DOWN_AT = 5.0;
	public static final int HARVEST_EVERY = 440;
	public static final int HALLOWED_EVERY = 360;
	public static final int LAST_EVERY = 300;
	public static final int CALL_TICKS = 30;
	public static final int RIPEN_TICKS = 200;
	public static final int SWELL_TICKS = 100;
	public static final int SPLIT_TICKS = 30;
	public static final int REGROW_TICKS = 400;
	public static final int REAPING_GOURDS = 2;
	public static final int HALLOWED_GOURDS = 3;
	public static final int BEREFT_TICKS = 160;
	public static final int STAGGER_TICKS = 40;
	public static final int JACKS_MAX = 4;
	public static final float FEED = 0.04F;
	public static final float SWEEP_DAMAGE = 8.0F;
	public static final double SWEEP_REACH = 4.5;
	public static final double SWEEP_HALF_ANGLE = 75.0;
	public static final float TOSS_DAMAGE = 7.0F;
	public static final double TOSS_SPLASH = 3.0;
	public static final int TOSS_FIRE_SECONDS = 3;
	public static final double TOSS_RANGE = 20.0;
	public static final int TOSS_FLIGHT = 12;
	public static final float STORM_DAMAGE = 2.0F;
	public static final double STORM_LENGTH = 14.0;
	public static final double STORM_HALF_WIDTH = 1.5;
	public static final int STORM_EVERY = 10;
	public static final int STORM_BLIND_TICKS = 40;
	public static final float SNARE_DAMAGE = 5.0F;
	public static final int SNARE_ROOT_TICKS = 30;
	public static final double SNARE_HALF_WIDTH = 1.0;
	public static final double SNARE_HALF_LENGTH = 2.5;
	public static final float ROLL_DAMAGE = 10.0F;
	public static final double ROLL_LENGTH = 18.0;
	public static final double ROLL_HALF_WIDTH = 1.0;
	public static final double ROLL_THROW = 0.9;
	public static final int EXPERIENCE = 300;
	/** Where the Crowning's Jacks climb out of the rows: (x, z) in the field's template, on the east-west arm of the cross. */
	public static final double[][] CROWN_JACK_SPOTS = {{25.5, 32.5}, {43.5, 32.5}};
	/** Ticks his step down off the mound, or his climb back onto the post, takes. */
	private static final int STEP_TICKS = 12;
	/** Waking: the tick at which the crows burst out of the corn. */
	private static final int WAKE_CROWS = 20;
	/** The crows: near black, in a flock. */
	private static final DustParticleOptions CROW = new DustParticleOptions(0x14121A, 1.4F);

	/**
	 * His attacks: wind-up, the active part and recovery in ticks, the cooldown, the target's nearest and farthest distance,
	 * and the phases he uses it in (the Reaping; the Hallowed and the Last Harvest). The wind-up is what players read; the
	 * strike lands as it ends. The Gourd Toss's active ticks are its pumpkin's flight, the Crow Storm's the crows' stream
	 * and the Head Roll's the head's roll.
	 */
	public enum Attack {
		SCYTHE_SWEEP(16, 1, 14, 40, 0.0, 4.5, true, true),
		GOURD_TOSS(20, 12, 10, 100, 5.0, 20.0, true, true),
		CROW_STORM(24, 40, 12, 160, 0.0, 14.0, true, true),
		VINE_SNARE(24, 1, 12, 140, 0.0, 16.0, false, true),
		HEAD_ROLL(30, 24, 14, 200, 4.0, 18.0, false, true);

		public final int windup;
		public final int active;
		public final int recovery;
		public final int cooldown;
		public final double near;
		public final double far;
		public final boolean reaping;
		public final boolean hallowed;

		Attack(int windup, int active, int recovery, int cooldown, double near, double far, boolean reaping, boolean hallowed) {
			this.windup = windup;
			this.active = active;
			this.recovery = recovery;
			this.cooldown = cooldown;
			this.near = near;
			this.far = far;
			this.reaping = reaping;
			this.hallowed = hallowed;
		}

		public int length() {
			return windup + active + recovery;
		}
	}

	/** The fight's phases. */
	public enum Phase {
		WAITING, WAKING, REAPING, CROWNING, HALLOWED, LAST
	}

	/** What his body is doing, synced for his animations. */
	public enum Action {
		HANG, TEAR_FREE, IDLE, SWEEP_WINDUP, SWEEP, TOSS_WINDUP, TOSS, STORM_WINDUP, STORM, CALL, SNARE_WINDUP, SNARE, ROLL_WINDUP,
		ROLL, STAGGER, CROWN
	}

	/** The fire in his head, synced: dark (waiting), lit, white (the Crowning and the Last Harvest), guttering (bereft). */
	public enum Fire {
		DARK, LIT, WHITE, GUTTER
	}

	/** The steps he makes over the ground, unstopped by it: down off his mound, or back onto his post. */
	private enum Flight {
		NONE, DOWN, ONTO_POST
	}

	private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(PumpkinKingEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> FIRE_STATE = SynchedEntityData.defineId(PumpkinKingEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> HEADLESS = SynchedEntityData.defineId(PumpkinKingEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> HOLDING = SynchedEntityData.defineId(PumpkinKingEntity.class, EntityDataSerializers.BOOLEAN);
	static final RawAnimation[] BODY = new RawAnimation[Action.values().length];
	static final RawAnimation[] FIRES = new RawAnimation[Fire.values().length];
	static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.pumpkin_king.walk");
	static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.pumpkin_king.death");
	static final RawAnimation HEAD_ON = RawAnimation.begin().thenLoop("animation.pumpkin_king.head_on");
	static final RawAnimation HEAD_OFF = RawAnimation.begin().thenLoop("animation.pumpkin_king.head_off");
	static final RawAnimation HELD_NONE = RawAnimation.begin().thenLoop("animation.pumpkin_king.held_none");
	static final RawAnimation HELD_PUMPKIN = RawAnimation.begin().thenLoop("animation.pumpkin_king.held_pumpkin");

	static {
		String prefix = "animation.pumpkin_king.";
		BODY[Action.HANG.ordinal()] = RawAnimation.begin().thenLoop(prefix + "hang");
		BODY[Action.TEAR_FREE.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "tear_free");
		BODY[Action.IDLE.ordinal()] = RawAnimation.begin().thenLoop(prefix + "idle");
		BODY[Action.SWEEP_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "sweep_windup");
		BODY[Action.SWEEP.ordinal()] = RawAnimation.begin().then(prefix + "sweep", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.TOSS_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "toss_windup");
		BODY[Action.TOSS.ordinal()] = RawAnimation.begin().then(prefix + "toss", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.STORM_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "storm_windup");
		BODY[Action.STORM.ordinal()] = RawAnimation.begin().thenLoop(prefix + "storm");
		BODY[Action.CALL.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "call");
		BODY[Action.SNARE_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "snare_windup");
		BODY[Action.SNARE.ordinal()] = RawAnimation.begin().then(prefix + "snare", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.ROLL_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "roll_windup");
		BODY[Action.ROLL.ordinal()] = RawAnimation.begin().thenLoop(prefix + "roll");
		BODY[Action.STAGGER.ordinal()] = RawAnimation.begin().thenLoop(prefix + "stagger");
		BODY[Action.CROWN.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "crown");
		FIRES[Fire.DARK.ordinal()] = RawAnimation.begin().thenLoop(prefix + "fire_dark");
		FIRES[Fire.LIT.ordinal()] = RawAnimation.begin().thenLoop(prefix + "fire_lit");
		FIRES[Fire.WHITE.ordinal()] = RawAnimation.begin().thenLoop(prefix + "fire_white");
		FIRES[Fire.GUTTER.ordinal()] = RawAnimation.begin().thenLoop(prefix + "fire_gutter");
	}

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private final ServerBossEvent bossBar = new ServerBossEvent(UUID.randomUUID(), Component.translatable("entity.jugcraft.pumpkin_king"),
			BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);
	/** The lair instance he belongs to (null for one put down by a test or a command), and the field's origin. */
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
	/** Who takes part in this fight, and how (Concordance Chunk A); not saved. */
	private final Encounter encounter = new Encounter(this, this::centre);
	// His rule, the Harvest.
	private int harvestIn = HARVEST_EVERY;
	/** Ticks into his call (-1 when he is not calling), and the hills he calls. */
	private int callTicks = -1;
	private final List<Hill> called = new ArrayList<>();
	/** The hills of the harvest ripening now (empty: none), and how far it has ripened. */
	private final List<Hill> harvest = new ArrayList<>();
	private int ripenTicks;
	private final Set<Hill> picked = new HashSet<>();
	/** Whether a gourd of this harvest has lit, and so it can no longer leave him bereft. */
	private boolean burnt;
	private final Set<UUID> pickers = new HashSet<>();
	/** How many harvests he has called this fight (each one's deeds are named by it). */
	private int harvests;
	/** A lit gourd splits when its count runs out; a bare hill sprouts a green gourd when its count does. */
	private final Map<Hill, Integer> splitIn = new EnumMap<>(Hill.class);
	private final Map<Hill, Integer> regrowIn = new EnumMap<>(Hill.class);
	private int bereftTicks;
	// His attacks' marks: where the toss lands or the snare lashes, and the storm's or the roll's line.
	private Vec3 mark = Vec3.ZERO;
	private Vec3 lineFrom = Vec3.ZERO;
	private Vec3 lineWay = new Vec3(0.0, 0.0, 1.0);
	private double lineLength;
	// His Jacks, and his head while it rolls.
	private final List<UUID> jacks = new ArrayList<>();
	private @Nullable UUID head;
	// The step under way.
	private Flight flight = Flight.NONE;
	private Vec3 flightFrom = Vec3.ZERO;
	private Vec3 flightTo = Vec3.ZERO;
	private int flightTicks;
	private int flightLength;
	private double flightArc;
	/** Set when he is loaded fighting: he is set down off his mound, and his gourds are green again (the harvest is not saved). */
	private boolean landOnLoad;
	private boolean greenOnLoad;

	public PumpkinKingEntity(EntityType<? extends PumpkinKingEntity> type, Level level) {
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
	 * Hangs him on the post of {@code instance}'s field. Called as the instance is placed. Placing the template puts back
	 * only its own blocks, so the Grey Mist a fallen King opened by the post before still stands: it goes first.
	 */
	public static PumpkinKingEntity summon(ServerLevel level, LairInstance instance) {
		BlockPos origin = instance.origin();
		for (BlockPos cell : Crowfield.postExitCells()) {
			BlockPos at = cell.offset(origin);
			if (level.getBlockState(at).is(JugcraftLairs.LAIR_EXIT)) {
				level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		return summon(level, origin, instance);
	}

	/** Hangs him on the post of the field laid out from {@code origin} (tests pass no instance). */
	public static PumpkinKingEntity summon(ServerLevel level, BlockPos origin, @Nullable LairInstance instance) {
		PumpkinKingEntity king = new PumpkinKingEntity(JugcraftPumpkinKing.PUMPKIN_KING, level);
		king.origin = origin.immutable();
		if (instance != null) {
			king.instance = instance.id;
			king.slot = instance.slot;
		}
		king.noPhysics = true;
		Vec3 waiting = king.waiting();
		king.snapTo(waiting.x, waiting.y, waiting.z, 0.0F, 0.0F);
		king.face(0.0F);
		level.addFreshEntity(king);
		return king;
	}

	// ---------------------------------------------------------------- where things are

	/** The field's middle, on its floor, for the field laid out from {@code origin}. */
	public static Vec3 centre(BlockPos origin) {
		return Crowfield.centre().add(origin.getX(), origin.getY(), origin.getZ());
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

	/** Where he hangs on his post, facing the farmyard. */
	public Vec3 waiting() {
		Vec3 wait = Crowfield.waiting();
		return at(wait.x, wait.y, wait.z);
	}

	/** The field's floor, where he and his foes stand. */
	public double floorY() {
		return origin.getY() + Crowfield.STAND;
	}

	/** Where he steps down off his mound: onto the arm of the cross (of the four) nearest {@code toward}, {@value #DOWN_AT} out. */
	public Vec3 downSpot(@Nullable Entity toward) {
		double x = 0.0;
		double z = 1.0;  // the farmyard's arm, south, when nobody is about
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
		return at(Crowfield.CENTRE_X + x * DOWN_AT, Crowfield.STAND, Crowfield.CENTRE_Z + z * DOWN_AT);
	}

	/** Whether (x, z) (in the world) is in his field. */
	public boolean inField(double x, double z) {
		return Crowfield.inField(x - origin.getX(), z - origin.getZ());
	}

	/** Whether {@code entity} is in the field, where he fights (never the farmyard behind the fence). */
	public boolean inArena(Entity entity) {
		double y = entity.getY() - origin.getY();
		return inField(entity.getX(), entity.getZ()) && y >= Crowfield.STAND - 2 && y <= Crowfield.STAND + 8;
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

	public int harvestIn() {
		return harvestIn;
	}

	/** Sets the ticks to his next harvest (for tests). */
	public void harvestIn(int ticks) {
		harvestIn = ticks;
	}

	/** The hills of the harvest ripening now (empty when none is). */
	public List<Hill> harvest() {
		return List.copyOf(harvest);
	}

	/** The hills he is calling, while he calls. */
	public List<Hill> calling() {
		return callTicks >= 0 ? List.copyOf(called) : List.of();
	}

	public int ripenTicks() {
		return ripenTicks;
	}

	/** Sets how far the harvest has ripened (for tests). */
	public void ripenTicks(int ticks) {
		ripenTicks = ticks;
	}

	public int bereftTicks() {
		return bereftTicks;
	}

	/** Whether he is bereft of his harvest: his fire guttering, every blow biting deeper, his Jacks dazed. */
	public boolean bereft() {
		return bereftTicks > 0;
	}

	/** Whether he is up off his post: his Jacks run while he is. */
	public boolean awake() {
		return phase != Phase.WAITING;
	}

	/** Whether he is fighting (down off his post, not waiting, waking or crowned). */
	public boolean fighting() {
		return phase == Phase.REAPING || phase == Phase.HALLOWED || phase == Phase.LAST;
	}

	public List<UUID> jacks() {
		return List.copyOf(jacks);
	}

	/** Where the Gourd Toss lands or the Vine Snare lashes. */
	public Vec3 mark() {
		return mark;
	}

	/** The Crow Storm's or the Head Roll's line: where it starts, its way (flat, of length 1) and its length. */
	public Vec3 lineFrom() {
		return lineFrom;
	}

	public Vec3 lineWay() {
		return lineWay;
	}

	public double lineLength() {
		return lineLength;
	}

	/** What his bar says. */
	public Component bar() {
		return bossBar.getName();
	}

	public Action action() {
		int id = entityData.get(ACTION);
		return id >= 0 && id < Action.values().length ? Action.values()[id] : Action.IDLE;
	}

	private void setAction(Action action) {
		entityData.set(ACTION, action.ordinal());
	}

	/** The fire in his head (synced). */
	public Fire fire() {
		int id = entityData.get(FIRE_STATE);
		return id >= 0 && id < Fire.values().length ? Fire.values()[id] : Fire.DARK;
	}

	private void setFire(Fire fire) {
		entityData.set(FIRE_STATE, fire.ordinal());
	}

	/** His fire as it burns out of a bereft spell: white in the Last Harvest, else lit. */
	private Fire burning() {
		return phase == Phase.LAST ? Fire.WHITE : Fire.LIT;
	}

	/** Whether his head is off his shoulders, rolling (synced). */
	public boolean headless() {
		return entityData.get(HEADLESS);
	}

	/** Whether he holds a pumpkin overhead to toss (synced). */
	public boolean holding() {
		return entityData.get(HOLDING);
	}

	public @Nullable UUID instanceId() {
		return instance;
	}

	/** Damage he deals: the attack's own, times {@code lairs.boss_damage} ({@link LairBosses}). */
	public static float damage(float base) {
		return LairBosses.damage(base);
	}

	/** His health for a party of {@code players}: half as much again for each beyond the first, at most 2.5 times. */
	public static double partyScale(int players) {
		return LairBosses.partyScale(players, PARTY_STEP, PARTY_MAX);
	}

	/** The ticks between his harvests in this phase. */
	public int harvestEvery() {
		return phase == Phase.LAST ? LAST_EVERY : phase == Phase.HALLOWED || phase == Phase.CROWNING ? HALLOWED_EVERY : HARVEST_EVERY;
	}

	/** How many gourds he calls in this phase. */
	public int gourds() {
		return phase == Phase.REAPING || phase == Phase.WAKING || phase == Phase.WAITING ? REAPING_GOURDS : HALLOWED_GOURDS;
	}

	/** A vanilla sound by its id, or {@code fallback} if this game has none so named. */
	static SoundEvent sound(String id, SoundEvent fallback) {
		return BuiltInRegistries.SOUND_EVENT.getOptional(Identifier.withDefaultNamespace(id)).orElse(fallback);
	}

	/**
	 * Whether {@code source} is fire, which straw dreads: burning (any fire damage), a burning arrow or other projectile,
	 * or a blow struck in hand with a fire weapon (Fire Aspect, or an arm that sets alight: the Ember boon or the Ignite
	 * trait).
	 */
	public static boolean fiery(DamageSource source) {
		if (source.is(DamageTypeTags.IS_FIRE)) {
			return true;
		}
		Entity direct = source.getDirectEntity();
		if (direct instanceof Projectile projectile) {
			return projectile.isOnFire();
		}
		if (!(direct instanceof LivingEntity attacker)) {
			return false;
		}
		ItemStack weapon = attacker.getMainHandItem();
		if (weapon.getItem() instanceof ArmItem arm && (arm.boon() == ArmVariants.Boon.EMBER || arm.trait() == JugcraftArms.Trait.IGNITE)) {
			return true;
		}
		Holder<Enchantment> fireAspect = attacker.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FIRE_ASPECT);
		return EnchantmentHelper.getItemEnchantmentLevel(fireAspect, weapon) > 0;
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
		if (greenOnLoad) {
			greenOnLoad = false;
			greenAll(level);
		}
		if (phase != Phase.WAITING && isAlive() && !encounter.underway()) {
			encounter.begin();  // a fight back from a save keeps a fresh record of who takes part
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
		if (awake() && phase != Phase.WAKING) {
			tickHarvest(level);
		}
		switch (phase) {
			case WAITING -> waiting(level);
			case WAKING -> waking(level);
			case CROWNING -> crowning(level);
			case REAPING, HALLOWED, LAST -> fight(level);
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
		setAction(Action.HANG);
		if (tickCount % 10 == 0 && intruder(level) != null) {
			wake(level);
		}
	}

	/** Someone who may fight him, through the farm gate into the field. */
	private @Nullable Player intruder(ServerLevel level) {
		for (Player player : level.players()) {
			if (LairBosses.eligible(player) && inArena(player)) {
				return player;
			}
		}
		return null;
	}

	/** He tears free of his post: his health scaled for the players about, his bar shown, his head bursting into flame. */
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
		setAction(Action.TEAR_FREE);
		setFire(Fire.LIT);
		encounter.begin();
		bossBar.setName(barName());
		bossBar.setVisible(true);
		harvestIn = HARVEST_EVERY;
		level.playSound(null, getX(), getY(), getZ(), sound("entity.blaze.shoot", SoundEvents.PARROT_AMBIENT), SoundSource.HOSTILE, 3.0F, 0.5F);
		level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 4.7, getZ(), 30, 0.5, 0.4, 0.5, 0.05);
		tell(level, "message.jugcraft.pumpkin_king.wakes");
	}

	/** Waking: he tears free, the crows burst out of the corn, and he steps down off his mound toward whoever is nearest. */
	private void waking(ServerLevel level) {
		if (flight == Flight.NONE) {
			noPhysics = true;
			setDeltaMovement(Vec3.ZERO);
		}
		if (phaseTicks == WAKE_CROWS) {
			crowsRise(level);
		}
		if (phaseTicks >= WAKE_TICKS) {
			phase = Phase.REAPING;
			phaseTicks = 0;
			globalCooldown = GLOBAL_COOLDOWN;
			stepDown(level);
		}
	}

	/** The crows burst up out of the corn all round the field, crying. */
	private void crowsRise(ServerLevel level) {
		Vec3 centre = centre();
		for (int i = 0; i < 16; i++) {
			double rad = Math.toRadians(22.5 * i);
			level.sendParticles(CROW, centre.x + Math.sin(rad) * 23.0, floorY() + 3.5, centre.z - Math.cos(rad) * 23.0, 6, 0.8, 0.6, 0.8, 0.05);
		}
		for (int i = 0; i < 3; i++) {
			level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.PARROT_AMBIENT, SoundSource.HOSTILE, 4.0F, 0.4F + 0.05F * i);
		}
		level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 3.0F, 1.6F);
	}

	/** He steps down off his mound onto the arm of the cross nearest whoever is nearest. */
	private void stepDown(ServerLevel level) {
		setAction(Action.IDLE);
		fly(Flight.DOWN, downSpot(nearest(level)), STEP_TICKS, 1.0);
	}

	/** Players in his field or its farmyard: the party he scales for. */
	public List<ServerPlayer> present(ServerLevel level) {
		List<ServerPlayer> out = new ArrayList<>();
		for (ServerPlayer player : level.players()) {
			double x = player.getX() - origin.getX();
			double z = player.getZ() - origin.getZ();
			double y = player.getY() - origin.getY();
			if (LairBosses.eligible(player) && (Crowfield.inField(x, z) || Crowfield.inYard(x, z)) && y >= Crowfield.STAND - 2
					&& y <= Crowfield.STAND + 8) {
				out.add(player);
			}
		}
		return out;
	}

	/** Players who may fight him and stand in his field: those he chooses among, and those his blows can reach. */
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

	// ---------------------------------------------------------------- steps

	/** He steps to {@code to} over {@code ticks} ticks, rising {@code arc} blocks at the middle, through anything. */
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

	private void tickFlight(ServerLevel level) {
		flightTicks++;
		double share = Math.min(1.0, flightTicks / (double) flightLength);
		Vec3 along = flightFrom.add(flightTo.subtract(flightFrom).scale(share));
		double y = along.y + flightArc * 4.0 * share * (1.0 - share);
		setDeltaMovement(Vec3.ZERO);
		setPos(along.x, y, along.z);
		if (flight == Flight.DOWN) {
			face(flightTo.subtract(flightFrom));
		}
		if (share >= 1.0) {
			Flight done = flight;
			flight = Flight.NONE;
			if (done == Flight.DOWN) {
				noPhysics = false;
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 2.0F, 0.7F);
			} else {
				face(0.0F);
			}
		}
	}

	/** Whether he is stepping down off his mound or climbing back onto his post. */
	public boolean flying() {
		return flight != Flight.NONE;
	}

	// ---------------------------------------------------------------- his rule: the Harvest

	/**
	 * The hills he calls for a harvest of {@code count} gourds against {@code target}: the one nearest them, then on round
	 * the ring toward whichever of its two neighbours is nearer them.
	 */
	public List<Hill> run(Entity target, int count) {
		double x = target.getX() - origin.getX();
		double z = target.getZ() - origin.getZ();
		Hill first = Crowfield.nearestHill(x, z);
		boolean clockwise = distance(first.next(), x, z) <= distance(first.previous(), x, z);
		List<Hill> out = new ArrayList<>();
		Hill hill = first;
		for (int i = 0; i < count; i++) {
			out.add(hill);
			hill = clockwise ? hill.next() : hill.previous();
		}
		return out;
	}

	private static double distance(Hill hill, double x, double z) {
		BlockPos at = Crowfield.HILLS[hill.ordinal()];
		return Math.hypot(x - (at.getX() + 0.5), z - (at.getZ() + 0.5));
	}

	/** He calls the harvest on the gourds round {@code target}: he raises his arms and the crows cry ({@value #CALL_TICKS} ticks). */
	public void callHarvest(ServerLevel level, Player target) {
		attack = null;
		attackTicks = 0;
		callTicks = 0;
		called.clear();
		called.addAll(run(target, gourds()));
		foe = target.getUUID();
		hold();
		face(target);
		setAction(Action.CALL);
		tell(level, "message.jugcraft.pumpkin_king.harvest", called.get(0).display());
		for (int i = 0; i < 3; i++) {
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.PARROT_AMBIENT, SoundSource.HOSTILE, 3.0F, 0.45F + 0.1F * i);
		}
	}

	/** His call, a tick on: crows rise over the gourds he calls; at its end they begin to ripen. */
	private void calling(ServerLevel level, @Nullable Player target) {
		callTicks++;
		hold();
		if (target != null) {
			face(target);
		}
		if (callTicks % 5 == 0) {
			for (Hill hill : called) {
				BlockPos at = hill.at(origin);
				level.sendParticles(CROW, at.getX() + 0.5, at.getY() + 3.0, at.getZ() + 0.5, 4, 0.6, 0.4, 0.6, 0.05);
			}
		}
		if (callTicks >= CALL_TICKS) {
			callTicks = -1;
			ripen(level, called);
			setAction(Action.IDLE);
			globalCooldown = GLOBAL_COOLDOWN;
		}
	}

	/**
	 * The gourds of {@code hills} begin to ripen, swelling: a bare hill sprouts one at once for him. The harvest's clock
	 * starts, and nothing of it is picked yet.
	 */
	public void ripen(ServerLevel level, List<Hill> hills) {
		harvest.clear();
		harvest.addAll(hills);
		ripenTicks = 0;
		picked.clear();
		pickers.clear();
		burnt = false;
		harvests++;
		for (Hill hill : hills) {
			splitIn.remove(hill);
			regrowIn.remove(hill);
			KingsGourdBlock.setStage(level, hill.at(origin), KingsGourdBlock.SWELLING);
		}
		harvestIn = harvestEvery();
		if (!bereft()) {
			bossBar.setName(Component.translatable("message.jugcraft.pumpkin_king.harvest_bar"));
		}
	}

	/** The harvest, the splitting of lit gourds and the sprouting of bare hills, a tick on. */
	private void tickHarvest(ServerLevel level) {
		for (Iterator<Map.Entry<Hill, Integer>> it = splitIn.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Hill, Integer> entry = it.next();
			int left = entry.getValue() - 1;
			if (left > 0) {
				entry.setValue(left);
				continue;
			}
			Hill hill = entry.getKey();  // an EnumMap entry cannot be read once removed
			it.remove();
			split(level, hill);
		}
		for (Iterator<Map.Entry<Hill, Integer>> it = regrowIn.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Hill, Integer> entry = it.next();
			int left = entry.getValue() - 1;
			if (left > 0) {
				entry.setValue(left);
				continue;
			}
			BlockPos at = entry.getKey().at(origin);
			it.remove();
			if (level.getBlockState(at).getBlock() instanceof KingsGourdBlock
					&& level.getBlockState(at).getValue(KingsGourdBlock.STAGE) == KingsGourdBlock.PICKED) {
				KingsGourdBlock.setStage(level, at, KingsGourdBlock.GREEN);
				level.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 6, 0.4, 0.3, 0.4, 0.0);
			}
		}
		if (bereftTicks > 0 && --bereftTicks == 0) {
			if (fire() == Fire.GUTTER) {
				setFire(burning());
			}
			bossBar.setName(harvest.isEmpty() ? barName() : Component.translatable("message.jugcraft.pumpkin_king.harvest_bar"));
		}
		if (harvest.isEmpty()) {
			return;
		}
		ripenTicks++;
		if (ripenTicks == SWELL_TICKS) {
			for (Hill hill : harvest) {
				if (!picked.contains(hill)) {
					KingsGourdBlock.setStage(level, hill.at(origin), KingsGourdBlock.RIPENING);
				}
			}
		}
		if (ripenTicks % 5 == 0) {
			drawCrows(level);
		}
		if (ripenTicks >= RIPEN_TICKS) {
			for (Hill hill : harvest) {
				if (!picked.contains(hill)) {
					light(level, hill);
				}
			}
			endHarvest();
		}
	}

	/** Crows circling over each gourd still ripening. */
	private void drawCrows(ServerLevel level) {
		for (Hill hill : harvest) {
			if (picked.contains(hill)) {
				continue;
			}
			BlockPos at = hill.at(origin);
			for (int i = 0; i < 3; i++) {
				double rad = Math.toRadians(120.0 * i + ripenTicks * 9.0);
				level.sendParticles(CROW, at.getX() + 0.5 + Math.cos(rad) * 1.3, at.getY() + 2.6, at.getZ() + 0.5 + Math.sin(rad) * 1.3, 1, 0.05, 0.05,
						0.05, 0.0);
			}
			if (ripenTicks % 40 == 0) {
				level.playSound(null, at, SoundEvents.PARROT_AMBIENT, SoundSource.HOSTILE, 1.0F, 0.5F);
			}
		}
	}

	/** A gourd ripens through: its face lights, and it will split. */
	private void light(ServerLevel level, Hill hill) {
		BlockPos at = hill.at(origin);
		KingsGourdBlock.setStage(level, at, KingsGourdBlock.LIT);
		splitIn.put(hill, SPLIT_TICKS);
		burnt = true;
		level.playSound(null, at, sound("item.firecharge.use", SoundEvents.PUMPKIN_CARVE), SoundSource.HOSTILE, 1.2F, 0.7F);
		level.sendParticles(ParticleTypes.FLAME, at.getX() + 0.5, at.getY() + 0.6, at.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
	}

	/**
	 * A lit gourd splits open, leaving its hill bare: a Jack-o'-Straw climbs out of it, or, with {@value #JACKS_MAX} about,
	 * its fire feeds him {@value #FEED} of his health.
	 */
	private void split(ServerLevel level, Hill hill) {
		BlockPos at = hill.at(origin);
		BlockState state = level.getBlockState(at);
		if (!(state.getBlock() instanceof KingsGourdBlock) || state.getValue(KingsGourdBlock.STAGE) != KingsGourdBlock.LIT) {
			return;
		}
		KingsGourdBlock.setStage(level, at, KingsGourdBlock.PICKED);
		regrowIn.put(hill, REGROW_TICKS);
		level.playSound(null, at, SoundEvents.PUMPKIN_CARVE, SoundSource.HOSTILE, 1.2F, 0.5F);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PUMPKIN.defaultBlockState()), at.getX() + 0.5, at.getY() + 0.5,
				at.getZ() + 0.5, 20, 0.4, 0.3, 0.4, 0.1);
		Direction front = state.getValue(HorizontalDirectionalBlock.FACING);
		BlockPos out = at.relative(front);
		if (callJack(level, new Vec3(out.getX() + 0.5, at.getY(), out.getZ() + 0.5)) != null) {
			tell(level, "message.jugcraft.pumpkin_king.jack");
		} else {
			heal(getMaxHealth() * FEED);
			level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 4.7, getZ(), 16, 0.4, 0.4, 0.4, 0.04);
			tell(level, "message.jugcraft.pumpkin_king.feeds");
		}
	}

	/** The harvest is over (ripened through, or every gourd of it picked). */
	private void endHarvest() {
		harvest.clear();
		ripenTicks = 0;
		if (!bereft()) {
			bossBar.setName(barName());
		}
	}

	/**
	 * {@code player} picked the gourd at {@code pos} (its block has already come off the vine): if it is one of his, its
	 * hill will sprout again; if it is of the harvest ripening now, the picker takes part in it, and the harvest's last gourd
	 * picked before any lit leaves him bereft. Returns whether this field's King answered it.
	 */
	public boolean gourdPicked(ServerLevel level, BlockPos pos, Player player) {
		Hill hill = Crowfield.hillAt(origin, pos);
		if (hill == null) {
			return false;
		}
		regrowIn.put(hill, REGROW_TICKS);
		if (!harvest.contains(hill) || !picked.add(hill)) {
			return true;
		}
		pickers.add(player.getUUID());
		player.sendOverlayMessage(Component.translatable("message.jugcraft.pumpkin_king.picked", picked.size(), harvest.size()));
		if (picked.containsAll(harvest)) {
			if (!burnt) {
				bereave(level);
			}
			endHarvest();
		}
		return true;
	}

	/**
	 * Every gourd of a harvest picked before any lit: he is bereft. He sags ({@value #STAGGER_TICKS} ticks), his fire
	 * gutters, and his Jacks stand dazed for {@value #BEREFT_TICKS}; whoever picked a gourd of it takes part.
	 */
	public void bereave(ServerLevel level) {
		bereftTicks = BEREFT_TICKS;
		if (phase != Phase.CROWNING) {
			stagger(STAGGER_TICKS);
			callTicks = -1;
		}
		setFire(Fire.GUTTER);
		bossBar.setName(Component.translatable("message.jugcraft.pumpkin_king.bereft_bar"));
		tell(level, "message.jugcraft.pumpkin_king.bereft");
		for (UUID id : pickers) {
			ServerPlayer picker = level.getServer().getPlayerList().getPlayer(id);
			if (picker != null) {
				encounter.deed(picker, "bereft:" + harvests + ":" + id);
			}
		}
		level.playSound(null, getX(), getY(), getZ(), sound("block.fire.extinguish", SoundEvents.PUMPKIN_CARVE), SoundSource.HOSTILE, 3.0F, 0.6F);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 4.7, getZ(), 20, 0.4, 0.4, 0.4, 0.02);
	}

	/** His gourds go back to green, all of them, and nothing is ripening, splitting or bare. */
	public void greenAll(ServerLevel level) {
		for (Hill hill : Hill.values()) {
			KingsGourdBlock.setStage(level, hill.at(origin), KingsGourdBlock.GREEN);
		}
		harvest.clear();
		called.clear();
		callTicks = -1;
		ripenTicks = 0;
		picked.clear();
		pickers.clear();
		burnt = false;
		splitIn.clear();
		regrowIn.clear();
	}

	/** His bar's name, out of a harvest or a bereft spell. */
	private Component barName() {
		return switch (phase) {
			case CROWNING, HALLOWED -> Component.translatable("message.jugcraft.pumpkin_king.hallowed_bar");
			case LAST -> Component.translatable("message.jugcraft.pumpkin_king.last_bar");
			default -> Component.translatable("entity.jugcraft.pumpkin_king");
		};
	}

	// ---------------------------------------------------------------- fighting

	private void fight(ServerLevel level) {
		Player target = target(level);
		if (attack != null) {
			if (target == null && attack != Attack.HEAD_ROLL) {
				endAttack(level);
			} else {
				attackTicks++;
				perform(level, target);
				return;
			}
		}
		if (callTicks >= 0) {
			calling(level, target);
			return;
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
		keepToTheField(level);
		if (phase == Phase.REAPING && getHealth() <= getMaxHealth() * CROWN_AT) {
			crown(level);
			return;
		}
		if (phase == Phase.HALLOWED && getHealth() <= getMaxHealth() * LAST_AT) {
			lastHarvest(level);
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
		if (harvestIn > 0) {
			harvestIn--;
		}
		if (harvestIn <= 0 && harvest.isEmpty()) {
			callHarvest(level, target);
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

	/** Strayed farther than his leash from the field's middle, or fallen below its floor, he is set back in it. */
	private void keepToTheField(ServerLevel level) {
		Vec3 centre = centre();
		double dx = getX() - centre.x;
		double dz = getZ() - centre.z;
		if (dx * dx + dz * dz <= LEASH * LEASH && getY() > centre.y - 3.0) {
			return;
		}
		level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 2.5, getZ(), 20, 0.8, 1.5, 0.8, 0.02);
		endAttack(level);
		noPhysics = false;
		Vec3 down = downSpot(target(level));
		teleportTo(down.x, down.y, down.z);
		level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 2.5, getZ(), 20, 0.8, 1.5, 0.8, 0.02);
	}

	/** His target: the one he has while they are still in his field, else the nearest who may fight him there. */
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

	/** Whom his Jacks are after: his foe while they are in his field, else the nearest there to {@code jack}. */
	public @Nullable Player jackTarget(ServerLevel level, Entity jack) {
		Player held = foe(level);
		if (held != null && LairBosses.eligible(held) && inArena(held)) {
			return held;
		}
		Player nearest = null;
		double best = Double.MAX_VALUE;
		for (ServerPlayer player : nearby(level)) {
			double distance = player.distanceToSqr(jack);
			if (distance < best) {
				best = distance;
				nearest = player;
			}
		}
		return nearest;
	}

	/** Strides toward a point {@value #KEEP_DISTANCE} blocks short of {@code target}. */
	private void approach(Player target) {
		Vec3 to = target.position().subtract(position());
		double flat = Math.sqrt(to.x * to.x + to.z * to.z);
		Vec3 goal = flat > KEEP_DISTANCE + 0.5
				? new Vec3(target.getX() - to.x / flat * KEEP_DISTANCE, getY(), target.getZ() - to.z / flat * KEEP_DISTANCE)
				: position();
		move(goal, SPEED);
	}

	/** Strides toward {@code goal} (never past his leash) at {@code speed}, pressed down onto the floor. */
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

	/** Holds still where he is, pressed down onto what he stands on (or still in the air, stepping). */
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

	/** His right hand, where a pumpkin is lifted from and his head is set down: before him, and up. */
	public Vec3 hand() {
		Vec3 facing = facing();
		return position().add(facing.scale(0.8)).add(0.0, 5.6, 0.0);
	}

	/** Picks an attack he can use now on {@code target}, or null. */
	private @Nullable Attack choose(Player target) {
		double distance = target.distanceTo(this);
		List<Attack> ready = new ArrayList<>();
		for (Attack candidate : Attack.values()) {
			boolean phaseAllows = phase == Phase.REAPING ? candidate.reaping : candidate.hallowed;
			if (!phaseAllows || cooldowns[candidate.ordinal()] > 0 || distance < candidate.near || distance > candidate.far) {
				continue;
			}
			ready.add(candidate);
			if (candidate == Attack.SCYTHE_SWEEP) {
				ready.add(candidate);  // close in, his scythe comes most often
			}
		}
		return ready.isEmpty() ? null : ready.get(getRandom().nextInt(ready.size()));
	}

	/** Starts {@code next} on {@code target}: its wind-up begins, and what it aims at is fixed. */
	public void begin(ServerLevel level, Attack next, Player target) {
		attack = next;
		attackTicks = 0;
		foe = target.getUUID();
		cooldowns[next.ordinal()] = next.cooldown;
		hold();
		face(target);
		switch (next) {
			case SCYTHE_SWEEP -> {
				setAction(Action.SWEEP_WINDUP);
				level.playSound(null, getX(), getY(), getZ(), sound("item.trident.riptide_1", SoundEvents.PLAYER_ATTACK_SWEEP), SoundSource.HOSTILE,
						1.5F, 0.5F);
			}
			case GOURD_TOSS -> {
				setAction(Action.TOSS_WINDUP);
				entityData.set(HOLDING, true);
				Vec3 to = target.position().subtract(position());
				double flat = Math.hypot(to.x, to.z);
				Vec3 at = flat <= TOSS_RANGE ? target.position() : position().add(to.x / flat * TOSS_RANGE, 0.0, to.z / flat * TOSS_RANGE);
				mark = new Vec3(at.x, floorY(), at.z);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.CROP_BREAK, SoundSource.HOSTILE, 1.5F, 0.6F);
			}
			case CROW_STORM -> {
				setAction(Action.STORM_WINDUP);
				line(target, STORM_LENGTH);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 2.0F, 1.4F);
			}
			case VINE_SNARE -> {
				setAction(Action.SNARE_WINDUP);
				mark = new Vec3(target.getX(), floorY(), target.getZ());
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.CROP_BREAK, SoundSource.HOSTILE, 2.0F, 0.4F);
			}
			case HEAD_ROLL -> {
				setAction(Action.ROLL_WINDUP);
				line(target, ROLL_LENGTH);
				level.playSound(null, getX(), getY(), getZ(), sound("entity.blaze.shoot", SoundEvents.PARROT_AMBIENT), SoundSource.HOSTILE, 2.0F, 0.6F);
			}
		}
	}

	/** Fixes a line from his feet toward {@code target}, {@code length} long but never out of the field. */
	private void line(Player target, double length) {
		Vec3 to = new Vec3(target.getX() - getX(), 0.0, target.getZ() - getZ());
		lineWay = to.lengthSqr() < 1.0E-4 ? facing() : to.normalize();
		lineFrom = new Vec3(getX(), floorY(), getZ());
		double inside = 0.0;
		while (inside < length && inField(lineFrom.x + lineWay.x * (inside + 0.5), lineFrom.z + lineWay.z * (inside + 0.5))) {
			inside += 0.5;
		}
		lineLength = Math.max(1.0, inside);
	}

	/** Whether {@code player} stands in the fixed line (its whole length, {@code halfWidth} either side). */
	private boolean inLine(Player player, double halfWidth) {
		Vec3 to = new Vec3(player.getX() - lineFrom.x, 0.0, player.getZ() - lineFrom.z);
		double along = to.x * lineWay.x + to.z * lineWay.z;
		double across = Math.abs(to.x * lineWay.z - to.z * lineWay.x);
		return along >= 0.0 && along <= lineLength && across <= halfWidth + player.getBbWidth() / 2.0 && Math.abs(player.getY() - lineFrom.y) <= 3.0;
	}

	private void perform(ServerLevel level, @Nullable Player target) {
		Attack current = attack;
		if (current == null) {
			return;
		}
		int t = attackTicks;
		hold();
		switch (current) {
			case SCYTHE_SWEEP -> {
				if (t < current.windup) {
					if (target != null) {
						face(target);
					}
				} else if (t == current.windup) {
					setAction(Action.SWEEP);
					scytheSweep(level);
				}
			}
			case GOURD_TOSS -> {
				if (t < current.windup) {
					face(mark.subtract(position()));
					if (t % 2 == 0) {
						drawRing(level, mark, TOSS_SPLASH, ParticleTypes.SMALL_FLAME);
					}
				} else if (t == current.windup) {
					setAction(Action.TOSS);
					entityData.set(HOLDING, false);
					KingsPumpkinEntity.toss(level, this, hand(), mark, TOSS_FLIGHT);
				}
			}
			case CROW_STORM -> {
				if (t < current.windup) {
					if (t % 3 == 0) {
						drawLine(level, CROW, 0.6, STORM_HALF_WIDTH);
					}
				} else if (t == current.windup) {
					setAction(Action.STORM);
				}
				if (t >= current.windup && t < current.windup + current.active) {
					if (t % 2 == 0) {
						drawLine(level, CROW, 1.6, STORM_HALF_WIDTH);
					}
					if ((t - current.windup) % STORM_EVERY == 0) {
						crowStorm(level);
					}
				}
			}
			case VINE_SNARE -> {
				if (t < current.windup) {
					drawRipple(level, t / (double) current.windup);
				} else if (t == current.windup) {
					setAction(Action.SNARE);
					vineSnare(level);
				}
			}
			case HEAD_ROLL -> {
				if (t < current.windup) {
					if (t % 3 == 0) {
						drawLine(level, ParticleTypes.SMALL_FLAME, 0.4, ROLL_HALF_WIDTH);
					}
				} else if (t == current.windup) {
					setAction(Action.ROLL);
					entityData.set(HEADLESS, true);
					head = KingsHeadEntity.roll(level, this, lineFrom, lineWay, lineLength, current.active).getUUID();
				} else if (t == current.windup + current.active) {
					headBack(level);
				}
			}
		}
		if (attack == current && attackTicks >= current.length()) {
			endAttack(level);
		}
	}

	/** The attack under way is over or spoiled: his head back on his shoulders, his hands empty. */
	private void endAttack(ServerLevel level) {
		if (attack == null) {
			return;
		}
		attack = null;
		attackTicks = 0;
		globalCooldown = GLOBAL_COOLDOWN;
		entityData.set(HOLDING, false);
		headBack(level);
		Action action = action();
		if (action == Action.STORM || action == Action.ROLL || action.name().endsWith("_WINDUP")) {
			setAction(Action.IDLE);
		}
	}

	/** His head, if it is rolling, comes back to his shoulders. */
	private void headBack(ServerLevel level) {
		if (head != null && level.getEntity(head) instanceof KingsHeadEntity rolling) {
			rolling.discard();
		}
		head = null;
		entityData.set(HEADLESS, false);
	}

	/** A ring of {@code particle} round {@code at}, {@code radius} across. */
	private static void drawRing(ServerLevel level, Vec3 at, double radius, net.minecraft.core.particles.ParticleOptions particle) {
		for (int i = 0; i < 16; i++) {
			double rad = Math.toRadians(22.5 * i);
			level.sendParticles(particle, at.x + Math.cos(rad) * radius, at.y + 0.1, at.z + Math.sin(rad) * radius, 1, 0.0, 0.02, 0.0, 0.0);
		}
	}

	/** {@code particle} along the fixed line, {@code halfWidth} either side, about {@code density} a block. */
	private void drawLine(ServerLevel level, net.minecraft.core.particles.ParticleOptions particle, double density, double halfWidth) {
		int count = Math.max(1, (int) Math.round(lineLength * density));
		for (int i = 0; i < count; i++) {
			double along = getRandom().nextDouble() * lineLength;
			double across = (getRandom().nextDouble() * 2.0 - 1.0) * halfWidth;
			double x = lineFrom.x + lineWay.x * along + lineWay.z * across;
			double z = lineFrom.z + lineWay.z * along - lineWay.x * across;
			level.sendParticles(particle, x, lineFrom.y + 0.3 + getRandom().nextDouble() * 1.4, z, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	/** The Vine Snare's ripple: the vines stir along the furrow from his feet toward the mark, {@code share} of the way. */
	private void drawRipple(ServerLevel level, double share) {
		double z = getZ() + (mark.z - getZ()) * share;
		BlockParticleOption roots = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState());
		level.sendParticles(roots, mark.x, mark.y + 0.1, z, 3, 0.2, 0.05, 0.3, 0.05);
		if (share > 0.5) {
			level.sendParticles(roots, mark.x, mark.y + 0.1, mark.z, 2, SNARE_HALF_WIDTH / 2.0, 0.05, SNARE_HALF_LENGTH / 2.0, 0.02);
		}
	}

	/**
	 * Scythe Sweep: every player within {@value #SWEEP_REACH} blocks before him (within {@value #SWEEP_HALF_ANGLE} degrees
	 * either side of his face) takes {@value #SWEEP_DAMAGE}. Returns how many it hit.
	 */
	public int scytheSweep(ServerLevel level) {
		int hit = 0;
		Vec3 facing = facing();
		for (ServerPlayer player : nearby(level)) {
			Vec3 to = player.position().subtract(position());
			double flat = Math.sqrt(to.x * to.x + to.z * to.z);
			if (flat > SWEEP_REACH + player.getBbWidth() / 2.0 || Math.abs(to.y) > 3.0) {
				continue;
			}
			double cos = flat < 1.0E-3 ? 1.0 : (to.x * facing.x + to.z * facing.z) / flat;
			if (cos < Math.cos(Math.toRadians(SWEEP_HALF_ANGLE))) {
				continue;  // behind him or to his side
			}
			if (player.hurtServer(level, damageSources().mobAttack(this), damage(SWEEP_DAMAGE))) {
				hit++;
			}
		}
		Vec3 blade = position().add(facing.scale(2.5)).add(0.0, 1.5, 0.0);
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, blade.x, blade.y, blade.z, 3, 1.2, 0.2, 1.2, 0.0);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 2.0F, 0.5F);
		return hit;
	}

	/**
	 * Crow Storm, one beat of it: every player in the line takes {@value #STORM_DAMAGE} and is blinded for
	 * {@value #STORM_BLIND_TICKS} ticks. Returns how many it hit.
	 */
	public int crowStorm(ServerLevel level) {
		int hit = 0;
		for (ServerPlayer player : nearby(level)) {
			if (!inLine(player, STORM_HALF_WIDTH)) {
				continue;
			}
			if (player.hurtServer(level, damageSources().mobAttack(this), damage(STORM_DAMAGE))) {
				hit++;
				player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, STORM_BLIND_TICKS, 0), this);
			}
		}
		Vec3 mid = lineFrom.add(lineWay.scale(lineLength / 2.0));
		level.playSound(null, mid.x, mid.y, mid.z, SoundEvents.PARROT_AMBIENT, SoundSource.HOSTILE, 2.0F, 0.4F + getRandom().nextFloat() * 0.2F);
		return hit;
	}

	/**
	 * Vine Snare: the vines lash up along the furrow at the mark ({@value #SNARE_HALF_LENGTH} either way along it,
	 * {@value #SNARE_HALF_WIDTH} across): {@value #SNARE_DAMAGE}, and rooted (Slowness IV, {@value #SNARE_ROOT_TICKS}
	 * ticks). Returns how many it caught.
	 */
	public int vineSnare(ServerLevel level) {
		int hit = 0;
		for (ServerPlayer player : nearby(level)) {
			double across = Math.abs(player.getX() - mark.x);
			double along = Math.abs(player.getZ() - mark.z);
			if (across > SNARE_HALF_WIDTH + player.getBbWidth() / 2.0 || along > SNARE_HALF_LENGTH || Math.abs(player.getY() - mark.y) > 3.0) {
				continue;
			}
			if (player.hurtServer(level, damageSources().mobAttack(this), damage(SNARE_DAMAGE))) {
				hit++;
				player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SNARE_ROOT_TICKS, 3), this);
			}
		}
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.VINE.defaultBlockState()), mark.x, mark.y + 0.6, mark.z, 40,
				SNARE_HALF_WIDTH / 2.0, 0.5, SNARE_HALF_LENGTH / 2.0, 0.1);
		level.playSound(null, mark.x, mark.y, mark.z, SoundEvents.CROP_BREAK, SoundSource.HOSTILE, 2.0F, 0.5F);
		return hit;
	}

	/** He reels: any attack under way is spoiled, and he does nothing for {@code ticks}. */
	public void stagger(int ticks) {
		attack = null;
		attackTicks = 0;
		entityData.set(HOLDING, false);
		if (level() instanceof ServerLevel level) {
			headBack(level);
		}
		staggerTicks = Math.max(staggerTicks, ticks);
		setAction(Action.STAGGER);
	}

	/** Sets {@code player} moving at {@code motion}, and tells their client (a player's own motion is theirs to move). */
	static void push(ServerPlayer player, Vec3 motion) {
		player.setDeltaMovement(motion);
		player.connection.send(new ClientboundSetEntityMotionPacket(player));
	}

	// ---------------------------------------------------------------- the Crowning, and the Last Harvest

	/** The Crowning: he climbs back onto his post, his head flaring white, unhurt; two Jacks climb out of the rows. */
	public void crown(ServerLevel level) {
		endAttack(level);
		staggerTicks = 0;
		callTicks = -1;
		phase = Phase.CROWNING;
		phaseTicks = 0;
		setAction(Action.CROWN);
		setFire(Fire.WHITE);
		fly(Flight.ONTO_POST, waiting(), STEP_TICKS, 1.5);
		for (int i = 0; i < CROWN_JACKS && i < CROWN_JACK_SPOTS.length; i++) {
			callJack(level, at(CROWN_JACK_SPOTS[i][0], Crowfield.STAND, CROWN_JACK_SPOTS[i][1]));
		}
		bossBar.setName(barName());
		tell(level, "message.jugcraft.pumpkin_king.crowning");
		level.playSound(null, getX(), getY(), getZ(), sound("entity.blaze.shoot", SoundEvents.PARROT_AMBIENT), SoundSource.HOSTILE, 4.0F, 0.4F);
	}

	/** The Crowning, a tick on: he flares on his post, then steps down into the Hallowed Harvest. */
	private void crowning(ServerLevel level) {
		if (!flying()) {
			setDeltaMovement(Vec3.ZERO);
			if (phaseTicks % 4 == 0) {
				level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 4.7, getZ(), 4, 0.5, 0.4, 0.5, 0.05);
			}
		}
		if (phaseTicks >= CROWNING_TICKS && !flying()) {
			phase = Phase.HALLOWED;
			phaseTicks = 0;
			harvestIn = Math.min(harvestIn, HALLOWED_EVERY);
			globalCooldown = GLOBAL_COOLDOWN;
			if (!bereft()) {
				setFire(Fire.LIT);
			}
			bossBar.setName(barName());
			stepDown(level);
		}
	}

	/** The Last Harvest: he calls it more often, and his fire burns white. */
	public void lastHarvest(ServerLevel level) {
		phase = Phase.LAST;
		phaseTicks = 0;
		harvestIn = Math.min(harvestIn, LAST_EVERY);
		if (!bereft()) {
			setFire(Fire.WHITE);
			bossBar.setName(barName());
		}
		tell(level, "message.jugcraft.pumpkin_king.last");
	}

	// ---------------------------------------------------------------- his Jacks

	/** A Jack-o'-Straw climbs out at {@code at}, if he has fewer than {@value #JACKS_MAX}; returns it, or null. */
	public @Nullable JackOStrawEntity callJack(ServerLevel level, Vec3 at) {
		prune(level);
		if (jacks.size() >= JACKS_MAX) {
			return null;
		}
		JackOStrawEntity jack = JackOStrawEntity.climb(level, this, at);
		jacks.add(jack.getUUID());
		return jack;
	}

	/** Forgets the fallen Jacks. */
	private void prune(ServerLevel level) {
		jacks.removeIf(id -> !(level.getEntity(id) instanceof JackOStrawEntity jack) || !jack.isAlive());
	}

	/** Whoever hurt him or his Jacks, or whose tamed creature did, has taken part. */
	public void took(ServerPlayer player, float amount) {
		encounter.damage(player, amount);
	}

	/** This fight: who took part and how. */
	public Encounter encounter() {
		return encounter;
	}

	/**
	 * His participants still near his field: everyone who qualified in his fight ({@link Encounter}) and is alive within
	 * {@value Encounter#REACH} blocks.
	 */
	public List<ServerPlayer> participants(ServerLevel level) {
		List<ServerPlayer> out = new ArrayList<>();
		Vec3 centre = centre();
		for (UUID id : encounter.qualified()) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
			if (player != null && player.isAlive() && player.level() == level && player.position().distanceTo(centre) <= Encounter.REACH) {
				out.add(player);
			}
		}
		return out;
	}

	// ---------------------------------------------------------------- abandoned, and defeated

	/** Left alone: he hangs himself back on his post, healed; his Jacks crumble, his gourds go green, and who hurt him is forgotten. */
	public void reset(ServerLevel level) {
		dismiss(level);
		greenAll(level);
		phase = Phase.WAITING;
		phaseTicks = 0;
		attack = null;
		abandoned = 0;
		staggerTicks = 0;
		bereftTicks = 0;
		foe = null;
		flight = Flight.NONE;
		harvestIn = HARVEST_EVERY;
		encounter.abandon();
		Arrays.fill(cooldowns, 0);
		setHealth(getMaxHealth());
		entityData.set(HOLDING, false);
		setFire(Fire.DARK);
		noPhysics = true;
		Vec3 waiting = waiting();
		teleportTo(waiting.x, waiting.y, waiting.z);
		face(0.0F);
		setAction(Action.HANG);
		bossBar.setName(Component.translatable("entity.jugcraft.pumpkin_king"));
		bossBar.setVisible(false);
		tell(level, "message.jugcraft.pumpkin_king.reset");
	}

	/** His Jacks crumble to straw, and his head, if it rolls, is back. */
	private void dismiss(ServerLevel level) {
		for (UUID id : List.copyOf(jacks)) {
			if (level.getEntity(id) instanceof JackOStrawEntity jack) {
				jack.crumble(level);
			}
		}
		jacks.clear();
		headBack(level);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level) {
			// A killing blow gets here inside super.hurtServer, before hurtServer's own took(): its striker (or the owner of
			// the tamed creature that struck) has taken part before his fall is paid. Any amount qualifies.
			if (Encounter.striker(source) instanceof ServerPlayer player) {
				took(player, Float.MIN_VALUE);
			}
			defeated(level);
		}
	}

	/**
	 * He has fallen: his Jacks crumble, his gourds go green, each participant is paid, his lair's instance ends (it closes
	 * once everyone has left), and Grey Mist opens by his post.
	 */
	private void defeated(ServerLevel level) {
		// However often his fall is reported, it is answered once: one reward each (Concordance Chunk A).
		if (!encounter.claim()) {
			return;
		}
		dismiss(level);
		greenAll(level);
		LairRewards.pay(level, PumpkinKingLoot.BOSS, encounter, position(), centre(), EXPERIENCE);
		setFire(Fire.DARK);
		level.playSound(null, getX(), getY(), getZ(), sound("block.fire.extinguish", SoundEvents.PUMPKIN_CARVE), SoundSource.HOSTILE, 4.0F, 0.5F);
		LairInstance open = lairInstance(level);
		if (open != null) {
			Lairs.end(open);
			BlockState mist = JugcraftLairs.LAIR_EXIT.defaultBlockState().setValue(LairExitBlock.AXIS, Direction.Axis.X);
			for (BlockPos cell : Crowfield.postExitCells()) {
				level.setBlock(cell.offset(origin), mist, Block.UPDATE_ALL);
			}
		}
		bossBar.setVisible(false);
		tell(level, "message.jugcraft.pumpkin_king.defeated");
	}

	/** Tells everyone near his field. */
	void tell(ServerLevel level, String key, Object... args) {
		Vec3 centre = centre();
		for (ServerPlayer player : level.players()) {
			if (player.position().distanceTo(centre) <= 64.0) {
				player.sendSystemMessage(Component.translatable(key, args));
			}
		}
	}

	// ---------------------------------------------------------------- what he is immune to, and what burns him

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
		if (phase == Phase.WAKING || phase == Phase.CROWNING) {
			return false;
		}
		boolean fire = fiery(source);
		float dealt = amount * (bereft() ? (fire ? BEREFT_FIRE : BEREFT) : (fire ? FIRE : 1.0F));
		boolean hurt = super.hurtServer(level, source, dealt);
		if (hurt && Encounter.striker(source) instanceof ServerPlayer player) {
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
	protected @Nullable SoundEvent getAmbientSound() {
		return phase == Phase.WAITING ? null : SoundEvents.PARROT_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return sound("block.grass.break", SoundEvents.CROP_BREAK);
	}

	@Override
	protected SoundEvent getDeathSound() {
		return sound("block.fire.extinguish", SoundEvents.PUMPKIN_CARVE);
	}

	@Override
	public float getVoicePitch() {
		return 0.5F;
	}

	// ---------------------------------------------------------------- saving and syncing

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ACTION, Action.HANG.ordinal());
		builder.define(FIRE_STATE, Fire.DARK.ordinal());
		builder.define(HEADLESS, false);
		builder.define(HOLDING, false);
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
		origin = input.read("origin", BlockPos.CODEC).orElse(BlockPos.containing(getX() - Crowfield.WAIT_X, getY() - Crowfield.WAIT_Y,
				getZ() - Crowfield.WAIT_Z));
		try {
			phase = Phase.valueOf(input.getStringOr("phase", Phase.WAITING.name()));
		} catch (IllegalArgumentException e) {
			phase = Phase.WAITING;
		}
		if (phase == Phase.WAKING || phase == Phase.CROWNING) {
			// A passing moment is not resumed: he fights on.
			phase = phase == Phase.WAKING ? Phase.REAPING : Phase.HALLOWED;
		}
		// His harvest, his attacks and his Jacks are not saved: loaded fighting, he is set down off his mound, his gourds green.
		landOnLoad = fighting();
		greenOnLoad = phase != Phase.WAITING;
		noPhysics = phase == Phase.WAITING;
		entityData.set(FIRE_STATE, (phase == Phase.WAITING ? Fire.DARK : phase == Phase.LAST ? Fire.WHITE : Fire.LIT).ordinal());
		entityData.set(HEADLESS, false);
		entityData.set(HOLDING, false);
		bossBar.setVisible(phase != Phase.WAITING);
		bossBar.setName(barName());
	}

	// ---------------------------------------------------------------- his animations

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<PumpkinKingEntity>("body", 3, test -> {
			PumpkinKingEntity king = test.animatable();
			if (king.isDeadOrDying()) {
				return test.setAndContinue(DEATH);
			}
			Action action = king.action();
			return test.setAndContinue(action == Action.IDLE && test.isMoving() ? WALK : BODY[action.ordinal()]);
		}));
		controllers.add(new AnimationController<PumpkinKingEntity>("fire", 2, test -> test.setAndContinue(
				test.animatable().isDeadOrDying() ? FIRES[Fire.DARK.ordinal()] : FIRES[test.animatable().fire().ordinal()])));
		controllers.add(new AnimationController<PumpkinKingEntity>("head", 0,
				test -> test.setAndContinue(test.animatable().headless() && !test.animatable().isDeadOrDying() ? HEAD_OFF : HEAD_ON)));
		controllers.add(new AnimationController<PumpkinKingEntity>("held", 0,
				test -> test.setAndContinue(test.animatable().holding() ? HELD_PUMPKIN : HELD_NONE)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
