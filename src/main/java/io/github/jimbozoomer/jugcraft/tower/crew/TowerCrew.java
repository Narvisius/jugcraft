package io.github.jimbozoomer.jugcraft.tower.crew;

import io.github.jimbozoomer.jugcraft.tower.ConsoleChairBlock;
import io.github.jimbozoomer.jugcraft.tower.SeatEntity;
import io.github.jimbozoomer.jugcraft.tower.Sittable;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.TowerData;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * One of a Drone Tower's crew (Justin approved the take-2 crew sheet on 5 November 2026): a Minecraft-style person in
 * their job's uniform (tools/crew_skins.py), working one of the tower's crew posts (tools/tripillar_crew.py): sitting
 * at a console, standing at a station or walking a round, carrying what the job carries. Click one to hear a word from
 * them. They can be hurt and killed (the security officers fight back and see off monsters; everyone else runs); the
 * tower hires someone new for the post a while later ({@link TowerCrewCare}). They never despawn, can't be leashed, and
 * drop nothing. The client is told only how they look and what they are doing.
 *
 * <p>Their routine (the night shift and breaks sheet, approved 5 November 2026; {@link CrewPost#restAt}): at night a
 * third keep the watch at their posts and the rest go off a few at a time to their sleep spot on their own floor (a sofa,
 * a cafe chair, the recliner, the lower bunk) and sleep there till morning: dozing sitting up, laid back in the recliner,
 * or lying under a blanket. By day each takes a break now and then at a cafe, dining or lounge seat with a coffee. They
 * open the glass and steel doors on the way. Hurt, a sleeper wakes and stays up a while.
 *
 * <p>Security (the Narv-Tech uniforms sheet, approved 6 November 2026) wear power armour and a full helmet
 * ({@link #GEAR_EXO}, {@link #GEAR_HELMET}) and carry the {@link PlasmaRifle}: at a hostile mob they can see, or at a
 * player who hurts any of the crew near them, they stand, raise it and fire ({@link RifleGoal}). The gate guards carry the
 * {@link PlasmaBaton} holstered on the right thigh ({@link #GEAR_BATON}): at the same targets they draw it, the plasma
 * lighting round the rod, go in and strike, and put it away when it's over ({@link BatonGoal}).
 */
public class TowerCrew extends PathfinderMob implements PlasmaBlade.Wearer {
	public static final int GEAR_HEADSET = 1, GEAR_CAP = 2, GEAR_HARDHAT = 4, GEAR_BUN = 8, GEAR_PONYTAIL = 16;
	/** The eyelids (gear.json): not worn, shown while they sleep. */
	public static final int GEAR_EYELIDS = 32;
	/** Security's full helmet (in place of the head) and power armour (props.json): taken off to sleep; the armour's back
	 * frame is put by while they sit. Whoever wears the armour carries the {@link PlasmaRifle}. */
	public static final int GEAR_HELMET = 64, GEAR_EXO = 128;
	/** The gate guards' holster and plasma baton (props.json): whoever wears it carries the {@link PlasmaBaton}. */
	public static final int GEAR_BATON = 256;
	/** The blanket (props.json) is what a sleeper lying down is under: the client shows it for {@link #ACT_LIE}. */
	public static final int PROP_NONE = 0, PROP_TABLET = 1, PROP_CLIPBOARD = 2, PROP_RADIO = 3, PROP_TOOLBOX = 4, PROP_MUG = 5,
			PROP_TRAY = 6, PROP_CART = 7, PROP_BLANKET = 8, PROP_RIFLE = 9, PROP_BATON = 10;
	public static final int ACT_IDLE = 0, ACT_SIT = 2, ACT_TYPE = 3, ACT_READ = 4, ACT_RADIO = 5, ACT_COOK = 6, ACT_POINT = 7,
			ACT_PUSH = 8, ACT_TRAY = 9, ACT_MUG = 10, ACT_CARRY = 11, ACT_PILOT = 12, ACT_BENCH = 13, ACT_DOZE = 14, ACT_RECLINE = 15,
			ACT_LIE = 16, ACT_READY = 17, ACT_AIM = 18;
	/** The plasma baton: drawing it (and lighting it), on guard with it lit, a strike, putting it away. The client times
	 * each from when it began (PlasmaBaton's DRAW_TICKS, STRIKE_TICKS, HOLSTER_TICKS). */
	public static final int ACT_DRAW = 19, ACT_BATON = 20, ACT_STRIKE = 21, ACT_HOLSTER = 22;
	/** The power armour's plasma blade ({@link PlasmaBlade}): out and on guard, a thrust, a slash. The client times its
	 * deployment from when it first sees one of these, and each move from when it began. */
	public static final int ACT_BLADE = 23, ACT_THRUST = 24, ACT_SLASH = 25;
	/** The props and activities by name, as the posts give them (index = the number above). */
	static final List<String> PROPS = List.of("", "tablet", "clipboard", "radio", "toolbox", "mug", "tray", "cart", "blanket", "rifle", "baton");
	static final List<String> ACTS = List.of("idle", "walk", "sit", "type", "read", "radio", "cook", "point", "push", "tray", "mug", "carry",
			"pilot", "bench", "doze", "recline", "lie", "ready", "aim", "draw", "baton", "strike", "holster");
	/** How many things each job has to say (message.jugcraft.tower_crew.JOB.0 ..), on a break, and asleep. */
	public static final int LINES = 8, BREAK_LINES = 12, ASLEEP_LINES = 6;
	/**
	 * How far from their post a crew member may stray before walking back, and how far from where they are bound they
	 * must be to count as lost (knocked out of the tower, say) and be put back, in blocks. Nobody is put back where a
	 * player can see it happen (Justin: "I see the people like teleporting around a lot ... Just fix that").
	 */
	public static final double STRAY = 24, LOST = 160;
	/**
	 * How stuck a crew member may get (40 for each walk in a row that can't be planned or gets them no nearer) before
	 * they are put back at their post (out of sight only), and before they give up a spot on their rounds for the next.
	 */
	public static final int STUCK_TICKS = 1200, SKIP_TICKS = 160;
	/** How far (blocks) a player is looked for who might see one of the crew put back. */
	public static final double SEEN = 64;
	/** How far (blocks) a walk may be planned, and how much harder than the game's usual the path finder may look. */
	public static final float PATH_LENGTH = 96, PATH_EFFORT = 2.0F;
	/** Ticks a sleeper stays up once something wakes them (hurt). */
	public static final int WOKEN_TICKS = 1200;
	/** Giving way (the greetings sheet): a player met head on this near (blocks) is given way to, for at most WAY_TICKS,
	 * and not again for WAY_AGAIN. */
	public static final double WAY_REACH = 2.2;
	public static final int WAY_TICKS = 80, WAY_AGAIN = 200;

	private static final EntityDataAccessor<String> SKIN = SynchedEntityData.defineId(TowerCrew.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Integer> GEAR = SynchedEntityData.defineId(TowerCrew.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> SLIM = SynchedEntityData.defineId(TowerCrew.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> PROP = SynchedEntityData.defineId(TowerCrew.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> ACT = SynchedEntityData.defineId(TowerCrew.class, EntityDataSerializers.INT);

	private BlockPos core = BlockPos.ZERO;
	private String post = "";
	private String job = "controller";
	private @Nullable CrewPost postData;
	private int lastTalk = -100;
	/** What they last talked about and which line (not said twice running). */
	private String lastSaid = "";
	private int lastLine = -1;
	private @Nullable Player listener;
	private int listeningUntil;
	/** The seat they sit on (world) and the cell beside it they step out to: their post's chair or a rest seat. */
	private @Nullable BlockPos seatedAt, seatedStand;
	private int wokenUntil;
	/** The game time until which an armed crew member stays after a player who hurt one of the crew. */
	private long angerUntil;
	/** The client's own: the act it last saw and since when (its tick count), when the baton last lit, and its hum (a
	 * client sound instance). Unused on the server. */
	public int clientAct = -1, clientActSince, clientLitSince;
	public @Nullable Object clientHum;
	/** The clock a game test runs them by (ticks of the world's clock), or -1 for the world's. */
	private long testClock = -1;
	/** A game test's crew member, with no tower round them (a core position, no core): they don't leave for want of one. */
	private boolean testNoCore;
	/** Coming in off a drop ship (not saved): the way down its ramp (world), the next point of it, where to walk to
	 * after (the gates, or the roof door), the ship, and since when. */
	private @Nullable List<Vec3> arriveWay;
	private int arriveNext;
	private @Nullable Vec3 arriveTo;
	private @Nullable UUID arriveShip;
	private long arriveSince;
	/** Standing escort for a drop ship (not saved): the ship, where to stand, where to walk back to, how far along
	 * (0 going, 1 on guard, 2 back), since when. */
	private @Nullable UUID escortShip;
	private @Nullable Vec3 escortSpot, escortTo;
	private int escortStage;
	private long escortSince;
	/** Until this game time someone walking in from a drop ship isn't put back for being far from their post. */
	private long commuteUntil;
	/** A long walk under way (CrewRoutes; not saved): its way points, the one they are making for, where it leads, when it
	 * is planned again, and the way point the path finder was last sent to. */
	private @Nullable List<BlockPos> route;
	private int routeAt;
	private @Nullable BlockPos routeTo, viaNow;
	private int routeAgain;
	/** Riding in on a drop ship (C4; saved): the ship, their place aboard (world: a seat's cushion top, or where they stand
	 * in the aisle), the way they face, whether they sit; and how long (ticks) the ship has been nowhere to be found. */
	private @Nullable UUID aboardShip;
	private @Nullable Vec3 aboardAt;
	private float aboardYaw;
	private boolean aboardSeated;
	private int aboardMissing;
	/** A janitor's cart (C5; saved): whether they have it out of the custodian's closet, and the closet's two blocks of it
	 * as they were (to park it back as it stood). */
	private boolean hasCart;
	private @Nullable BlockState cartState0, cartState1;

	public TowerCrew(EntityType<? extends TowerCrew> type, Level level) {
		super(type, level);
		setPersistenceRequired();
		getNavigation().setRequiredPathLength(PATH_LENGTH);
		getNavigation().setMaxVisitedNodesMultiplier(PATH_EFFORT);
		getNavigation().setCanOpenDoors(true);
	}

	/** Walking round the mod's pieces and out from under the service cradles (CrewPaths). */
	@Override
	protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
		return new CrewPaths.Navigation(this, level);
	}

	/** Hopping in place (Justin, 6 November 2026: "one of the dudes is sitting there just jumping up and down"): where and
	 * when the last hop was, how many in a row went nowhere, until when they don't hop at all, and how many hops were
	 * refused since their walk last looked (counted as a stuck walk). */
	private @Nullable Vec3 lastHop;
	private int lastHopAt, hopsInPlace, noHopUntil, refusedHops;

	/**
	 * A hop up (the game makes a walker hop at a step, or when it is pressed into a cell with something in it): one that
	 * got them nowhere twice running is not tried again for five seconds, their walk is planned afresh, and it counts as a
	 * stuck walk; in water they swim as ever.
	 */
	@Override
	public void jumpFromGround() {
		if (!isInWater() && !isInLava()) {
			if (tickCount < noHopUntil) {
				return;
			}
			if (lastHop != null && tickCount - lastHopAt < 40 && lastHop.distanceToSqr(getX(), lastHop.y, getZ()) < 0.36) {
				if (++hopsInPlace >= 2) {
					hopsInPlace = 0;
					noHopUntil = tickCount + 100;
					refusedHops++;
					getNavigation().stop();
					return;
				}
			} else {
				hopsInPlace = 0;
			}
			lastHop = position();
			lastHopAt = tickCount;
		}
		super.jumpFromGround();
	}

	/** Whether a player (not a spectator) near enough could see them now. */
	boolean seen() {
		for (Player p : level().players()) {
			if (!p.isSpectator() && p.distanceToSqr(this) < SEEN * SEEN && p.hasLineOfSight(this)) {
				return true;
			}
		}
		return false;
	}

	public static AttributeSupplier.Builder attributes() {
		return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(SKIN, "controller_1");
		builder.define(GEAR, 0);
		builder.define(SLIM, false);
		builder.define(PROP, PROP_NONE);
		builder.define(ACT, ACT_IDLE);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(0, new AboardGoal());
		goalSelector.addGoal(0, new ArriveGoal());
		goalSelector.addGoal(2, new EscortGoal());
		goalSelector.addGoal(1, new PanicGoal(this, 1.3) {
			@Override
			public boolean canUse() {
				return !guard() && super.canUse();
			}
		});
		goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1, true) {
			@Override
			public boolean canUse() {
				return guard() && !armed() && !batoned() && super.canUse();
			}
		});
		goalSelector.addGoal(1, new RifleGoal());
		goalSelector.addGoal(1, new BatonGoal());
		// the glass and steel doors (tower/PlantDoorBlock): opened on the way, their closers shut them behind
		goalSelector.addGoal(1, new OpenDoorGoal(this, false));
		goalSelector.addGoal(2, new ListenGoal());
		goalSelector.addGoal(2, new GiveWayGoal());
		goalSelector.addGoal(3, new WorkGoal());
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F) {
			@Override
			public boolean canUse() {
				return !asleep(act()) && super.canUse();
			}
		});
		goalSelector.addGoal(7, new RandomLookAroundGoal(this) {
			@Override
			public boolean canUse() {
				return !asleep(act()) && super.canUse();
			}
		});
		targetSelector.addGoal(1, new HurtByTargetGoal(this) {
			@Override
			public boolean canUse() {
				return guard() && super.canUse();
			}
		});
		// whatever is going for a player or one of the crew near them, seen or not (they go to see it); then any hostile mob
		// they see (Hostiles: every monster, the game's and every mod's). Justin, 11 November 2026: "these are hostile mobs,
		// let's kill them"
		targetSelector.addGoal(2, new DefendGoal());
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false, (e, level) -> Hostiles.hostile(e)) {
			@Override
			public boolean canUse() {
				return guard() && super.canUse();
			}
		});
	}

	/** How far (blocks) the armed crew look for a mob going for a player or one of their own. */
	public static final double DEFEND_RANGE = PlasmaRifle.RANGE;

	/**
	 * The armed crew see to a mob going for a player (in survival) or one of the crew near them, the nearest first, whether
	 * or not they can see it from where they stand (they go to where they can): before any other mob they might see.
	 */
	private class DefendGoal extends TargetGoal {
		private @Nullable Mob threat;

		DefendGoal() {
			super(TowerCrew.this, false, false);
			setFlags(EnumSet.of(Goal.Flag.TARGET));
		}

		@Override
		public boolean canUse() {
			if (!guard() || (tickCount + getId()) % 10 != 0 || !(level() instanceof ServerLevel level)) {
				return false;
			}
			threat = null;
			double best = Double.MAX_VALUE;
			for (Mob m : level.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(DEFEND_RANGE, 8, DEFEND_RANGE),
					m -> Hostiles.attacking(m) && Hostiles.hostile(m))) {
				double d = distanceToSqr(m);
				if (d < best && d <= DEFEND_RANGE * DEFEND_RANGE) {
					best = d;
					threat = m;
				}
			}
			return threat != null && threat != getTarget();
		}

		@Override
		public void start() {
			setTarget(threat);
			super.start();
		}
	}

	/** Sets a new hand up for their post (at hiring). */
	public void setUp(BlockPos core, CrewPost post, CrewRoster.Hire hire) {
		this.core = core.immutable();
		this.post = post.id();
		this.job = post.job();
		this.postData = post;
		entityData.set(SKIN, hire.look().skin());
		entityData.set(GEAR, hire.look().gear());
		entityData.set(SLIM, hire.look().slim());
		entityData.set(PROP, shown(post));
		setCustomName(Component.literal(hire.name()));
	}

	/** Dresses a stand-in as {@code look}: the client's, drawn in a drop ship's seats as it comes in (client/CrewShipRenderer). */
	public void dressAs(CrewRoster.Look look) {
		entityData.set(SKIN, look.skin());
		entityData.set(GEAR, look.gear());
		entityData.set(SLIM, look.slim());
		entityData.set(PROP, PROP_NONE);
		entityData.set(ACT, ACT_IDLE);
	}

	// ---------------------------------------------------------------- what the client is told

	public String skin() {
		return entityData.get(SKIN);
	}

	public int gear() {
		return entityData.get(GEAR);
	}

	public boolean slim() {
		return entityData.get(SLIM);
	}

	public int heldProp() {
		return entityData.get(PROP);
	}

	@Override
	public int act() {
		return entityData.get(ACT);
	}

	private void setAct(int act) {
		entityData.set(ACT, act);
	}

	/** Whether {@code act} is one of sleeping: dozing sitting up, laid back in a recliner, lying down. */
	public static boolean asleep(int act) {
		return act == ACT_DOZE || act == ACT_RECLINE || act == ACT_LIE;
	}

	// ---------------------------------------------------------------- the routine

	/** The world's clock (ticks since the first morning), or a game test's. */
	public long clock() {
		return testClock >= 0 ? testClock : level().getOverworldClockTime();
	}

	/** Runs them by {@code clock} instead of the world's clock (game tests); -1 for the world's again. */
	public void setTestClock(long clock) {
		testClock = clock;
	}

	/** For the game tests: a crew member set up with no tower core where their core should be stays all the same. */
	public void setTestNoCore() {
		testNoCore = true;
	}

	/** What their routine has them doing now: a rest (asleep, on a break) or null, at their post. */
	public CrewPost.@Nullable Rest rest() {
		CrewPost p = post();
		return p == null || tickCount < wokenUntil ? null : p.restAt(clock());
	}

	// ---------------------------------------------------------------- the post

	public BlockPos core() {
		return core;
	}

	public String postId() {
		return post;
	}

	public String job() {
		return job;
	}

	/** Security officers and gate guards fight; everyone else (the pilots, engineers, medic and manager too) runs. */
	public boolean guard() {
		return job.equals("security") || job.equals("gate_guard");
	}

	/** Whether they wear the power armour, and so carry the plasma rifle (security). */
	public boolean armed() {
		return (gear() & GEAR_EXO) != 0;
	}

	/** Whether they wear the holster, and so carry the plasma baton (the gate guards). */
	public boolean batoned() {
		return (gear() & GEAR_BATON) != 0 && !armed();
	}

	/** Whether the baton is lit in their hand (drawn, on guard or striking): what the client hums for. */
	public static boolean lit(int act) {
		return act == ACT_DRAW || act == ACT_BATON || act == ACT_STRIKE;
	}

	public @Nullable CrewPost post() {
		if (postData == null || !postData.id().equals(post)) {
			postData = null;
			for (CrewPost p : TowerData.get().crew()) {
				if (p.id().equals(post)) {
					postData = p;
					break;
				}
			}
		}
		return postData;
	}

	/** Their post's prop as they have it now: a janitor's cart only once it's out of the custodian's closet (C5). */
	private int shown(CrewPost post) {
		int prop = prop(post);
		return prop == PROP_CART && post.cart() != null && !hasCart ? PROP_NONE : prop;
	}

	private static int prop(CrewPost post) {
		int i = post.prop() == null ? 0 : PROPS.indexOf(post.prop());
		return Math.max(0, i);
	}

	private static int act(String name) {
		int i = ACTS.indexOf(name);
		return i < 0 || i == 1 ? ACT_IDLE : i;
	}

	/** What a crew member does while walking with what they carry. */
	private int walking(CrewPost post) {
		return switch (shown(post)) {
			case PROP_TABLET, PROP_CLIPBOARD -> ACT_READ;
			case PROP_TOOLBOX -> ACT_CARRY;
			case PROP_TRAY -> ACT_TRAY;
			case PROP_CART -> ACT_PUSH;
			default -> ACT_IDLE;
		};
	}

	private BlockPos world(BlockPos relative) {
		return core.offset(relative);
	}

	/** Where a spot of their post is in the world: from the core; for a patrol outside the tower, on the ground there. */
	private BlockPos place(CrewPost p, BlockPos relative) {
		BlockPos at = world(relative);
		return p.outside() ? ground(level(), at) : at;
	}

	/** The top of the ground (not leaves) at a point's column, if it is loaded; else the point. */
	public static BlockPos ground(Level level, BlockPos at) {
		if (!level.isLoaded(at)) {
			return at;
		}
		return level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at);
	}

	/** The top of what stands in a floor cell (a raised floor tile's 3 px), so feet go on it, not in it. */
	public static double floor(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		double top = state.getCollisionShape(level, pos).isEmpty() ? 0 : state.getCollisionShape(level, pos).max(Direction.Axis.Y);
		return pos.getY() + (top < 0.6 ? top : 0);
	}

	/** How often they have been put back (not saved; the game tests watch it: a walk that works needs none). */
	private int putBacks;

	public int putBacks() {
		return putBacks;
	}

	/** Puts them where they are bound (their post's home, or the cell beside their rest seat), lost or stuck. */
	private void putBack(BlockPos at) {
		putBacks++;
		getNavigation().stop();
		snapTo(at.getX() + 0.5, floor(level(), at), at.getZ() + 0.5, getYRot(), 0);
	}

	// ---------------------------------------------------------------- talking

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}
		if (!(player instanceof ServerPlayer server)) {
			return InteractionResult.SUCCESS;
		}
		listener = player;
		listeningUntil = tickCount + 80;
		if (tickCount - lastTalk >= 20) {
			lastTalk = tickCount;
			// asleep, they mumble; on a break, they talk about the break; else about the job
			CrewPost.Rest rest = rest();
			String what = asleep(act()) ? "asleep" : rest != null && isPassenger() ? "break" : job;
			int lines = what.equals("asleep") ? ASLEEP_LINES : what.equals("break") ? BREAK_LINES : LINES;
			// a different line from the last one they said (Justin: "give them more lines than just that")
			int line = getRandom().nextInt(lines);
			if (what.equals(lastSaid) && line == lastLine) {
				line = (line + 1 + getRandom().nextInt(lines - 1)) % lines;
			}
			lastSaid = what;
			lastLine = line;
			server.sendSystemMessage(Component.translatable("message.jugcraft.tower_crew.says", getDisplayName(),
					Component.translatable("crew.jugcraft.job." + job), Component.translatable("message.jugcraft.tower_crew." + what + "." + line)));
		}
		return InteractionResult.SUCCESS;
	}

	// ---------------------------------------------------------------- life and death

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean asleep = asleep(act());
		boolean hurt = super.hurtServer(level, source, amount);
		if (hurt && asleep) {
			wokenUntil = tickCount + WOKEN_TICKS;        // woken: up a while before they settle again
		}
		if (hurt && isPassenger()) {
			stopRiding();
		}
		if (hurt && source.getEntity() instanceof Player p && !p.isCreative() && !p.isSpectator()) {
			CrewShip.alert(level, this, p);              // and any drop ship near turns its guns on them
			// a player hurt one of the crew: the armed crew near (rifle or baton) see to them a while
			for (TowerCrew c : level.getEntitiesOfClass(TowerCrew.class, getBoundingBox().inflate(PlasmaRifle.RANGE), c -> c.armed() || c.batoned())) {
				c.angerUntil = level.getGameTime() + PlasmaRifle.ANGER_TICKS;
				if (!(c.getTarget() instanceof Player)) {
					c.setTarget(p);
				}
			}
		}
		return hurt;
	}

	@Override
	public void stopRiding() {
		boolean seated = isPassenger();
		super.stopRiding();
		if (seated && level() instanceof ServerLevel && aboardShip == null) {
			CrewPost p = post();
			BlockPos stand = seatedStand != null ? seatedStand : p != null && p.stand() != null ? world(p.stand()) : null;
			if (stand != null) {
				// out of the seat to the cell beside it (the seat would put them on top of it)
				snapTo(stand.getX() + 0.5, floor(level(), stand), stand.getZ() + 0.5, getYRot(), getXRot());
			}
			if (asleep(act())) {
				setAct(ACT_IDLE);
			}
		}
		seatedAt = null;
		seatedStand = null;
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		parkCartNow();                                  // (their cart back in the closet, not lost with them)
		if (level() instanceof ServerLevel level && !post.isEmpty()) {
			TowerCrewState.get(level).vacate(core, post, getUUID(), level.getGameTime());
		}
	}

	/** Sent away (the post gone, or someone hired in their place): a cart they had out goes back in the closet. */
	@Override
	public void remove(RemovalReason reason) {
		if (reason == RemovalReason.DISCARDED && level() instanceof ServerLevel level && level.getBlockEntity(core) instanceof TowerCoreBlockEntity) {
			parkCartNow();
		}
		super.remove(reason);
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (getTarget() instanceof Player p && (level.getGameTime() > angerUntil || !p.isAlive() || p.isCreative() || p.isSpectator()
				|| distanceToSqr(p) > (PlasmaRifle.RANGE + 4) * (PlasmaRifle.RANGE + 4))) {
			setTarget(null);                       // they let a player go after a while, or once out of range
		}
		if (tickCount % 100 != 0) {
			return;
		}
		// The tower gone (its core broken), the post gone from the design, or someone hired in their place while they
		// were missing: they leave.
		boolean coreLoaded = level.isLoaded(core);
		if (coreLoaded && !testNoCore && !(level.getBlockEntity(core) instanceof TowerCoreBlockEntity) || post() == null) {
			discard();
			return;
		}
		TowerCrewState.Slot slot = TowerCrewState.get(level).slot(core, post);
		if (slot != null) {
			Optional<UUID> recorded = slot.crew();
			if (recorded.isPresent() && !recorded.get().equals(getUUID())) {
				discard();
			} else if (recorded.isEmpty() && slot.vacant() >= 0 && isAlive()) {
				TowerCrewState.get(level).found(core, post, getUUID());
			}
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.store("core", BlockPos.CODEC, core);
		output.putString("post", post);
		output.putString("job", job);
		output.putString("skin", skin());
		output.putInt("gear", gear());
		output.putBoolean("slim", slim());
		if (aboardShip != null && aboardAt != null) {
			output.store("aboard_ship", net.minecraft.core.UUIDUtil.CODEC, aboardShip);
			output.store("aboard_at", Vec3.CODEC, aboardAt);
			output.putFloat("aboard_yaw", aboardYaw);
			output.putBoolean("aboard_seated", aboardSeated);
		}
		if (hasCart) {
			output.putBoolean("has_cart", true);
			if (cartState0 != null && cartState1 != null) {
				output.store("cart_0", BlockState.CODEC, cartState0);
				output.store("cart_1", BlockState.CODEC, cartState1);
			}
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		core = input.read("core", BlockPos.CODEC).orElse(BlockPos.ZERO);
		post = input.getStringOr("post", "");
		job = input.getStringOr("job", "controller");
		String skin = input.getStringOr("skin", "controller_1");
		entityData.set(SKIN, skin);
		// the gear as the pool has the skin now (a uniform redrawn with new gear: security's helmet and armour)
		CrewRoster.Look look = CrewRoster.get().look(skin);
		entityData.set(GEAR, look != null ? look.gear() : input.getIntOr("gear", 0));
		entityData.set(SLIM, look != null ? look.slim() : input.getBooleanOr("slim", false));
		aboardShip = input.read("aboard_ship", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
		aboardAt = input.read("aboard_at", Vec3.CODEC).orElse(null);
		aboardYaw = input.getFloatOr("aboard_yaw", 0.0F);
		aboardSeated = input.getBooleanOr("aboard_seated", false);
		if (aboardShip == null || aboardAt == null) {
			aboardShip = null;
			aboardAt = null;
			setNoGravity(false);                        // (saved coming down a ramp, whose walk isn't kept: on their feet)
		} else {
			noPhysics = true;
			setNoGravity(true);
		}
		hasCart = input.getBooleanOr("has_cart", false);
		cartState0 = input.read("cart_0", BlockState.CODEC).orElse(null);
		cartState1 = input.read("cart_1", BlockState.CODEC).orElse(null);
		CrewPost p = post();
		entityData.set(PROP, p == null ? PROP_NONE : shown(p));
	}

	// ---------------------------------------------------------------- goals

	/**
	 * Security in power armour at their target (a hostile mob they saw, or a player who hurt one of the crew): up out of
	 * their chair (awake if they slept), turned to it, the rifle raised ({@link #ACT_AIM}), a bolt every
	 * {@value PlasmaRifle#COOLDOWN} ticks while they see it within {@value PlasmaRifle#RANGE} blocks and nobody else is in
	 * the way; closer if they can't see it, and back to work when it's gone (or unseen for 5 seconds).
	 */
	private class RifleGoal extends Goal {
		private int cooldown, unseen, blocked, side = 1;
		private final PlasmaBlade.Fight blade = new PlasmaBlade.Fight();

		RifleGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity t = getTarget();
			return armed() && t != null && t.isAlive() && level() instanceof ServerLevel && !(t instanceof Player p && (p.isCreative() || p.isSpectator()))
					&& distanceToSqr(t) < (PlasmaRifle.RANGE + 8) * (PlasmaRifle.RANGE + 8);
		}

		@Override
		public boolean canContinueToUse() {
			return canUse() && unseen < 100;
		}

		@Override
		public void start() {
			if (asleep(act())) {
				wokenUntil = tickCount + WOKEN_TICKS;
			}
			if (isPassenger()) {
				stopRiding();
			}
			getNavigation().stop();
			unseen = 0;
			blocked = 0;
			cooldown = 10;                          // a moment to raise it
			setAct(ACT_AIM);
			entityData.set(PROP, PROP_RIFLE);
		}

		@Override
		public void stop() {
			getNavigation().stop();
			if (level() instanceof ServerLevel level) {
				blade.stow(level, TowerCrew.this);
			}
			setAct(ACT_IDLE);
			CrewPost p = post();
			entityData.set(PROP, p == null ? PROP_NONE : shown(p));
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity t = getTarget();
			if (t == null || !(level() instanceof ServerLevel level)) {
				return;
			}
			getLookControl().setLookAt(t, 60F, 60F);
			boolean sees = getSensing().hasLineOfSight(t);
			// too close to shoot: the armour's plasma blade
			if (blade.tick(level, TowerCrew.this, t, sees, true, TowerCrew.this::setAct)) {
				unseen = sees ? 0 : unseen + 1;
				cooldown = Math.max(cooldown, 10);          // (the rifle back up before it fires again)
				return;
			}
			setAct(ACT_AIM);
			double range2 = PlasmaRifle.RANGE * PlasmaRifle.RANGE;
			if (sees && distanceToSqr(t) <= range2) {
				unseen = 0;
				if (blocked < PlasmaRifle.BLOCKED_TICKS) {
					getNavigation().stop();
				} else if (getNavigation().isDone()) {
					// someone's been in the way a while: over to where the line's clear (the other side first next time)
					Vec3 to = PlasmaRifle.aside(level, TowerCrew.this, t, side);
					getNavigation().moveTo(to.x, to.y, to.z, 1.1);
					side = -side;
					blocked = PlasmaRifle.BLOCKED_TICKS / 2;
				}
				// square on to it, the rifle along the look
				float yaw = (float) (Mth.atan2(t.getZ() - getZ(), t.getX() - getX()) * Mth.RAD_TO_DEG) - 90F;
				setYRot(yaw);
				setYBodyRot(yaw);
			} else {
				unseen++;
				if (getNavigation().isDone() || unseen % 20 == 1) {
					getNavigation().moveTo(t, 1.0);
				}
			}
			if (--cooldown <= 0 && sees && distanceToSqr(t) <= range2) {
				Vec3 aim = new Vec3(t.getX(), t.getY(0.6), t.getZ());
				Vec3 eye = getEyePosition();
				Vec3 muzzle = eye.add(aim.subtract(eye).normalize()).add(0, -0.12, 0);
				if (PlasmaRifle.clearShot(level, TowerCrew.this, muzzle, aim, t)) {
					PlasmaRifle.fire(level, TowerCrew.this, muzzle, aim, t);
					cooldown = PlasmaRifle.COOLDOWN;
					blocked = 0;
				} else {
					cooldown = 5;
					blocked += 5;                       // someone's in the way: hold, look again shortly
				}
			}
		}
	}

	/**
	 * A gate guard with the plasma baton at their target (a hostile mob they saw, or a player who hurt one of the crew): up
	 * out of their chair (awake if they slept), the baton drawn as they go for it ({@link #ACT_DRAW}: the holster's catch,
	 * the plasma lighting with its sound), on guard ({@link #ACT_BATON}) closing in, and in reach a strike
	 * ({@link #ACT_STRIKE}: the swing's sound, the blow landing halfway, {@value PlasmaBaton#DAMAGE} damage and a sear) every
	 * {@value PlasmaBaton#STRIKE_TICKS} + {@value PlasmaBaton#COOLDOWN} ticks. When the target's gone (dead, a player let go,
	 * unseen for 5 seconds) they put it away ({@link #ACT_HOLSTER}: the power-down) and go back to work.
	 */
	private class BatonGoal extends Goal {
		private static final int DRAW = 0, FIGHT = 1, HOLSTER = 2, DONE = 3;
		private int phase = DONE, t, strike, cooldown, unseen;

		BatonGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		private boolean valid(@Nullable LivingEntity target) {
			return target != null && target.isAlive() && !(target instanceof Player p && (p.isCreative() || p.isSpectator()))
					&& distanceToSqr(target) < (PlasmaRifle.RANGE + 8) * (PlasmaRifle.RANGE + 8);
		}

		@Override
		public boolean canUse() {
			return batoned() && level() instanceof ServerLevel && valid(getTarget());
		}

		@Override
		public boolean canContinueToUse() {
			return phase != DONE;
		}

		@Override
		public void start() {
			if (asleep(act())) {
				wokenUntil = tickCount + WOKEN_TICKS;
			}
			if (isPassenger()) {
				stopRiding();
			}
			getNavigation().stop();
			phase = DRAW;
			t = 0;
			strike = 0;
			cooldown = 0;
			unseen = 0;
			setAct(ACT_DRAW);
			entityData.set(PROP, PROP_BATON);
		}

		@Override
		public void stop() {
			phase = DONE;
			getNavigation().stop();
			setAct(ACT_IDLE);
			CrewPost p = post();
			entityData.set(PROP, p == null ? PROP_NONE : shown(p));
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		private void holster(ServerLevel level) {
			phase = HOLSTER;
			t = 0;
			getNavigation().stop();
			setAct(ACT_HOLSTER);
			PlasmaBaton.play(level, TowerCrew.this, PlasmaBaton.OFF, 1.0F);
		}

		/** Turned to it and going for it, or standing in reach. */
		private void close(LivingEntity target, boolean reach) {
			getLookControl().setLookAt(target, 30F, 30F);
			if (reach) {
				getNavigation().stop();
			} else if (getNavigation().isDone() || tickCount % 10 == 0) {
				getNavigation().moveTo(target, 1.15);
			}
		}

		@Override
		public void tick() {
			if (!(level() instanceof ServerLevel level)) {
				return;
			}
			LivingEntity target = getTarget();
			boolean ok = valid(target);
			if (phase == HOLSTER) {
				if (++t >= PlasmaBaton.HOLSTER_TICKS) {
					phase = DONE;
				}
				return;
			}
			if (!ok || unseen >= PlasmaBaton.UNSEEN_TICKS) {
				holster(level);
				return;
			}
			boolean sees = getSensing().hasLineOfSight(target);
			unseen = sees ? 0 : unseen + 1;
			boolean reach = isWithinMeleeAttackRange(target);
			if (phase == DRAW) {
				t++;
				if (t == PlasmaBaton.DRAW_CATCH) {
					PlasmaBaton.play(level, TowerCrew.this, PlasmaBaton.CATCH, 0.25F);
				} else if (t == PlasmaBaton.DRAW_IGNITE) {
					PlasmaBaton.play(level, TowerCrew.this, PlasmaBaton.IGNITE, 1.0F);
				}
				close(target, reach);
				if (t >= PlasmaBaton.DRAW_TICKS) {
					phase = FIGHT;
					setAct(ACT_BATON);
				}
				return;
			}
			close(target, reach);
			if (strike > 0) {
				strike++;
				if (strike == PlasmaBaton.STRIKE_HIT && reach) {
					// the blow: the plasma sears in (the hurt knocks it back from them)
					if (target.hurtServer(level, damageSources().mobAttack(TowerCrew.this), PlasmaBaton.DAMAGE)) {
						setLastHurtMob(target);
					}
					PlasmaBaton.hitEffects(level, target, SoundSource.HOSTILE);
					// what the rod chopped down through (straight down, a touch inward), on it and on any foe in its way
					PlasmaWounds.sweep(level, TowerCrew.this, target, PlasmaWounds.swing(TowerCrew.this, target, null, 6F, PlasmaBaton.SWING_INNER,
							PlasmaBaton.SWING_OUTER, PlasmaBaton.SWING_HALF, PlasmaBaton.EDGE), damageSources().mobAttack(TowerCrew.this), PlasmaBaton.DAMAGE);
				}
				if (strike >= PlasmaBaton.STRIKE_TICKS) {
					strike = 0;
					cooldown = PlasmaBaton.COOLDOWN;
					setAct(ACT_BATON);
				}
			} else if (--cooldown <= 0 && reach && sees) {
				strike = 1;
				setAct(ACT_STRIKE);
				PlasmaBaton.play(level, TowerCrew.this, PlasmaBaton.SWING, 1.0F);
			}
		}
	}

	/** Stops to look at whoever spoke to them (seated crew stay in their chairs; a sleeper sleeps on). */
	private class ListenGoal extends Goal {
		ListenGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return listener != null && listener.isAlive() && tickCount < listeningUntil && distanceToSqr(listener) < 64 && getTarget() == null
					&& !asleep(act());
		}

		@Override
		public void start() {
			getNavigation().stop();
		}

		@Override
		public void tick() {
			if (listener != null) {
				getLookControl().setLookAt(listener, 30, 30);
			}
		}

		@Override
		public void stop() {
			listener = null;
		}
	}

	/**
	 * Gives way to a player met head on in a corridor (the greetings sheet): walking, with a player within WAY_REACH
	 * ahead, they step to a free cell beside them (the side away from the player), turn to them, and walk on once the
	 * player is past (or after WAY_TICKS). The player's own screen has them nod or wave as they turn (client/CrewGreetings).
	 */
	private class GiveWayGoal extends Goal {
		/** Who they give way to: a player (stepping to the side away from them), or another of the crew met head on (each
		 * keeping to their right, so they pass; Justin, 6 November 2026: "these two guys out here that are just like
		 * walking into each other"). */
		private @Nullable LivingEntity player;
		private @Nullable BlockPos aside;
		private Vec3 heading = Vec3.ZERO;
		private int until, nextCheck;

		GiveWayGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (tickCount < nextCheck || isPassenger() || getTarget() != null || getNavigation().isDone() || !(level() instanceof ServerLevel)) {
				return false;
			}
			nextCheck = tickCount + 10;
			Vec3 moving = getDeltaMovement().multiply(1, 0, 1);
			if (moving.lengthSqr() < 1.0E-4) {
				return false;
			}
			Vec3 dir = moving.normalize();
			LivingEntity p = level().getNearestPlayer(TowerCrew.this, WAY_REACH);
			boolean crew = false;
			if (p == null || p.isSpectator() || ahead(p, dir) < 0.7) {
				p = null;
				double best = 0.7;
				for (TowerCrew c : level().getEntitiesOfClass(TowerCrew.class, getBoundingBox().inflate(WAY_REACH),
						c -> c != TowerCrew.this && c.isAlive() && !c.isPassenger())) {
					double a = ahead(c, dir);
					if (a >= best) {
						best = a;
						p = c;
					}
				}
				crew = p != null;
			}
			if (p == null) {
				return false;
			}
			Vec3 to = p.position().subtract(position()).multiply(1, 0, 1);
			BlockPos cell = crew ? rightCell(dir) : sideCell(dir, to);
			if (cell == null) {
				return false;
			}
			player = p;
			aside = cell;
			heading = dir;
			return true;
		}

		/** How squarely {@code e} is ahead of someone going {@code dir} (1 dead ahead; below 0.7 not in the way). */
		private double ahead(LivingEntity e, Vec3 dir) {
			Vec3 to = e.position().subtract(position()).multiply(1, 0, 1);
			return to.lengthSqr() < 1.0E-4 ? 1 : to.normalize().dot(dir);
		}

		/** A free floor cell to their right (else their left): one of the crew keeps right to pass another. */
		private @Nullable BlockPos rightCell(Vec3 dir) {
			int sx = (int) Math.round(-dir.z), sz = (int) Math.round(dir.x);   // to their left
			if (sx == 0 && sz == 0) {
				return null;
			}
			BlockPos here = blockPosition();
			for (int side : new int[] {-1, 1}) {
				BlockPos cell = here.offset(sx * side, 0, sz * side);
				if (free(cell)) {
					return cell;
				}
			}
			return null;
		}

		/** A free floor cell beside them, the side away from the player first; null if neither is free. */
		private @Nullable BlockPos sideCell(Vec3 dir, Vec3 to) {
			int sx = (int) Math.round(-dir.z), sz = (int) Math.round(dir.x);   // to their left
			if (sx == 0 && sz == 0) {
				return null;
			}
			boolean playerLeft = to.x * sx + to.z * sz > 0;
			BlockPos here = blockPosition();
			for (int side : playerLeft ? new int[] {-1, 1} : new int[] {1, -1}) {
				BlockPos cell = here.offset(sx * side, 0, sz * side);
				if (free(cell)) {
					return cell;
				}
			}
			return null;
		}

		/** Room to stand: nothing to bump into at the feet (bar a raised floor tile) and the head, something under. */
		private boolean free(BlockPos cell) {
			Level level = level();
			var feet = level.getBlockState(cell).getCollisionShape(level, cell);
			if (!feet.isEmpty() && feet.max(Direction.Axis.Y) > 0.6) {
				return false;
			}
			if (!level.getBlockState(cell.above()).getCollisionShape(level, cell.above()).isEmpty()) {
				return false;
			}
			return !feet.isEmpty() || !level.getBlockState(cell.below()).getCollisionShape(level, cell.below()).isEmpty();
		}

		@Override
		public void start() {
			until = tickCount + WAY_TICKS;
			if (aside != null) {
				// a path right onto the cell: the cell beside is already "near enough" for moveTo's one-block reach, which
				// gave a path of just where they stood, so they never stepped aside
				var path = getNavigation().createPath(aside, 0);
				if (path != null) {
					getNavigation().moveTo(path, 0.7);
				}
			}
		}

		@Override
		public boolean canContinueToUse() {
			if (player == null || !player.isAlive() || tickCount > until || getTarget() != null || distanceToSqr(player) > 9.0) {
				return false;
			}
			// on once they're past
			return player.position().subtract(position()).dot(heading) > -0.5;
		}

		@Override
		public void tick() {
			if (player != null) {
				getLookControl().setLookAt(player, 30, 30);
			}
		}

		@Override
		public void stop() {
			player = null;
			aside = null;
			nextCheck = tickCount + WAY_AGAIN;
			getNavigation().stop();
		}
	}

	/**
	 * The work of their post: to the chair and sit (or stand beside it while it is taken); to a station's spots in turn,
	 * a while at each, facing the work; or round a patrol's spots, a pause at each. And their routine's rests: off to the
	 * rest's seat and settle there (asleep, or with a coffee) till it is over.
	 */
	/**
	 * Put aboard drop ship {@code ship} as it touches down (C4: "crew already seated in the ship before it lands"), before
	 * they're in the world: to sit at {@code at} facing {@code yaw} (a seat's cushion top), or stand there ({@code seated}
	 * false: the aisle), till the ship calls them off ({@link #disembark}).
	 */
	public void board(Vec3 at, float yaw, boolean seated, UUID ship) {
		aboardShip = ship;
		aboardAt = at;
		aboardYaw = yaw;
		aboardSeated = seated;
		aboardMissing = 0;
		noPhysics = true;
		setNoGravity(true);
	}

	/** Into their place aboard: a seat of their own at its point (the ship's seats are drawn, not blocks), or standing there. */
	public void seatAboard() {
		Vec3 at = aboardAt;
		if (at == null || isPassenger() || !(level() instanceof ServerLevel level)) {
			return;
		}
		setYRot(aboardYaw);
		setYBodyRot(aboardYaw);
		setYHeadRot(aboardYaw);
		if (!aboardSeated) {
			snapTo(at.x, at.y, at.z, aboardYaw, 0.0F);
			setDeltaMovement(Vec3.ZERO);
			return;
		}
		SeatEntity seat = new SeatEntity(io.github.jimbozoomer.jugcraft.tower.JugcraftTower.SEAT, level);
		seat.snapTo(at.x, at.y, at.z, aboardYaw, 0.0F);
		level.addFreshEntity(seat);
		if (!startRiding(seat, true, true)) {
			seat.discard();
		}
	}

	/** Whether a janitor has their cart out of the custodian's closet. */
	public boolean hasCart() {
		return hasCart;
	}

	/** Whether they're riding in on a drop ship, waiting to be called off. */
	public boolean aboard() {
		return aboardShip != null;
	}

	/** Called off drop ship {@code ship}: up out of their seat and along {@code way} (world: into the aisle, aft along it and
	 * down the ramp), then on foot to {@code to}. */
	public void disembark(List<Vec3> way, Vec3 to, UUID ship) {
		stopRiding();                                   // (still aboard: not put beside a post's chair)
		aboardShip = null;
		aboardAt = null;
		Vec3 first = way.get(0);
		snapTo(first.x, first.y, first.z, getYRot(), 0.0F);
		arrive(way, to, ship);
	}

	/** Off a ship that's gone (or never to be found again): on their feet on the ground below, and to work. */
	private void leaveShip() {
		stopRiding();
		aboardShip = null;
		aboardAt = null;
		noPhysics = false;
		setNoGravity(false);
		BlockPos g = ground(level(), blockPosition());
		snapTo(g.getX() + 0.5, floor(level(), g), g.getZ() + 0.5, getYRot(), 0.0F);
		commuteUntil = level().getGameTime() + 4800;
	}

	/** Aboard a drop ship: in their seat (or their place in the aisle), still, till called off; off it if it's gone. */
	private class AboardGoal extends Goal {
		AboardGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			return aboardShip != null && level() instanceof ServerLevel;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			getNavigation().stop();
			if (ship(aboardShip) == null) {
				if (++aboardMissing > 200) {
					leaveShip();
				}
				return;
			}
			aboardMissing = 0;
			if (!isPassenger()) {
				seatAboard();
			}
			setYBodyRot(aboardYaw);
			setAct(ACT_IDLE);
		}
	}

	/** Brought by a drop ship: down its ramp along {@code way} (world points), then on foot to {@code to}, then to work. */
	public void arrive(List<Vec3> way, Vec3 to, UUID ship) {
		arriveWay = List.copyOf(way);
		arriveNext = 1;
		arriveTo = to;
		arriveShip = ship;
		arriveSince = level().getGameTime();
		noPhysics = true;
		setNoGravity(true);
	}

	/** Whether they're still on a drop ship's ramp. */
	public boolean onRamp() {
		return arriveWay != null && arriveNext < arriveWay.size();
	}

	/** Called out to stand guard at {@code spot} while drop ship {@code ship} lets its crew off, then back to {@code to}. */
	public void escort(UUID ship, Vec3 spot, Vec3 to) {
		escortShip = ship;
		escortSpot = spot;
		escortTo = to;
		escortStage = 0;
		escortSince = level().getGameTime();
	}

	public boolean escorting() {
		return escortShip != null;
	}

	private @Nullable CrewShip ship(@Nullable UUID id) {
		return id != null && level() instanceof ServerLevel level && level.getEntity(id) instanceof CrewShip s && s.isAlive() ? s : null;
	}

	/**
	 * Off a drop ship: walked down the ramp (the ship is drawn, not solid, so they're moved along it), then on foot to the
	 * gates (or the roof door), then to work as anyone does (not put back for being far from their post for a while). The
	 * security who came with them (off first) stand guard at the ramp's foot till the ship lifts off.
	 */
	private class ArriveGoal extends Goal {
		ArriveGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			return arriveWay != null && level() instanceof ServerLevel;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			List<Vec3> way = arriveWay;
			if (way == null) {
				return;
			}
			long now = level().getGameTime();
			if (arriveNext < way.size()) {
				getNavigation().stop();
				Vec3 to = way.get(arriveNext);
				Vec3 d = to.subtract(position());
				double len = d.length();
				if (len <= CrewShip.WALK) {
					setPos(to);
					arriveNext++;
					if (arriveNext >= way.size()) {
						noPhysics = false;
						setNoGravity(false);
						CrewShip s = ship(arriveShip);
						if (job.equals("security") && s != null && s.guarded() && level() instanceof ServerLevel level) {
							int i = s.nextEscort();
							if (i < CrewShip.ESCORTS) {
								// the ship's own security hold the ramp's foot, facing out, till it lifts off (C4)
								escort(s.getUUID(), s.escortSpot(level, i), arriveTo);
								release(now);
							}
						}
					}
				} else {
					Vec3 step = d.scale(CrewShip.WALK / len);
					setPos(position().add(step));
					float yaw = (float) (Mth.atan2(step.z, step.x) * Mth.RAD_TO_DEG) - 90.0F;
					setYRot(yaw);
					setYBodyRot(yaw);
					setYHeadRot(yaw);
				}
				setDeltaMovement(Vec3.ZERO);
				return;
			}
			Vec3 to = arriveTo;
			if (to == null || horizontalSqTo(to) < 2.5 * 2.5 || now - arriveSince > 2400) {
				release(now);
				return;
			}
			walkLong(to, 0.9);
		}

		private void release(long now) {
			arriveWay = null;
			arriveShip = null;
			commuteUntil = now + 4800;
			noPhysics = false;
			setNoGravity(false);
		}

		@Override
		public void stop() {
			if (arriveWay != null && !onRamp()) {
				getNavigation().stop();
			}
		}
	}

	private double horizontalSqTo(Vec3 p) {
		double dx = p.x - getX(), dz = p.z - getZ();
		return dx * dx + dz * dz;
	}

	/**
	 * The next way point of a long walk to {@code at}: over the tower's walk map (CrewRoutes), which knows the way round
	 * the building where the game's path finder, looking a bounded way, only knows the straight line (and left them
	 * outside the wall nearest their post, or on the floor under it). Null when {@code at} is near on this floor (walked
	 * straight to), or the map has no way to give (yet): then they walk as the path finder says.
	 */
	private @Nullable BlockPos wayPoint(BlockPos at) {
		double dx = at.getX() + 0.5 - getX(), dz = at.getZ() + 0.5 - getZ();
		if (dx * dx + dz * dz < CrewRoutes.LONG * CrewRoutes.LONG && Math.abs(getY() - at.getY()) < 1.5) {
			route = null;
			routeTo = null;
			return null;
		}
		if (route == null || !at.equals(routeTo) || tickCount >= routeAgain) {
			boolean same = route != null && at.equals(routeTo);
			routeTo = at.immutable();
			List<BlockPos> planned = CrewRoutes.plan(this, core, at);
			boolean none = planned != null && planned.isEmpty();
			if (none) {
				planned = null;
			}
			if (planned != null || !same || none) {
				route = planned;
				routeAt = 0;
			}
			// a way is kept half a minute; with the map still being made (or a busy tick) they ask again shortly; where the
			// map has no way (they are off it, out past its edge) after a few seconds
			routeAgain = tickCount + (planned != null ? 600 : none ? 100 : 10 + getRandom().nextInt(10));
		}
		List<BlockPos> way = route;
		if (way == null) {
			return null;
		}
		while (routeAt < way.size() - 1 && reached(way.get(routeAt))) {
			routeAt++;
		}
		return way.get(routeAt);
	}

	private boolean reached(BlockPos w) {
		double dx = w.getX() + 0.5 - getX(), dz = w.getZ() + 0.5 - getZ();
		return dx * dx + dz * dz < 2.25 && Math.abs(getY() - w.getY()) < 1.2;
	}

	/** A long walk that isn't getting on (a way point they can't get to): planned again from where they stand. */
	private void rethinkRoute() {
		routeAgain = 0;
	}

	/** Walks towards {@code to}: by the walk map's way points when it's a long walk, else as the path finder says. */
	private void walkLong(Vec3 to, double speed) {
		BlockPos via = wayPoint(BlockPos.containing(to));
		if (via != null) {
			if (!via.equals(viaNow) || getNavigation().isDone() && tickCount % 20 == 0) {
				if (via.equals(viaNow)) {
					rethinkRoute();
				}
				viaNow = via;
				getNavigation().moveTo(via.getX() + 0.5, floor(level(), via), via.getZ() + 0.5, speed);
			}
			return;
		}
		viaNow = null;
		if (getNavigation().isDone() && tickCount % 20 == 0) {
			getNavigation().moveTo(to.x, to.y, to.z, speed);
		}
	}

	/** Out to a drop ship's ramp to stand guard (rifle ready) while its crew come off and till it lifts off, then back in. */
	private class EscortGoal extends Goal {
		EscortGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return escortShip != null && !isPassenger() && level() instanceof ServerLevel;
		}

		@Override
		public void start() {
			if (heldProp() == PROP_CART) {
				entityData.set(PROP, PROP_NONE);
			}
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			long now = level().getGameTime();
			CrewShip s = ship(escortShip);
			Vec3 spot = escortSpot, to = escortTo;
			if (spot == null || to == null) {
				escortShip = null;
				return;
			}
			if (escortStage < 2 && (s == null || !s.guarded() || now - escortSince > 3600)) {
				escortStage = 2;
				escortSince = now;
				setAct(ACT_IDLE);
			}
			switch (escortStage) {
				case 0 -> {
					if (horizontalSqTo(spot) < 1.0 || now - escortSince > 1200) {
						escortStage = 1;
						getNavigation().stop();
					} else if (getNavigation().isDone() && horizontalSqTo(spot) < 9.0) {
						// the last step straight in (a walk ends as soon as it's within a block, which may be short of it)
						getMoveControl().setWantedPosition(spot.x, spot.y, spot.z, 1.0);
						if (now - escortSince > 200 && horizontalSqTo(spot) < 2.5 * 2.5) {
							escortStage = 1;            // (as near as they can get)
						}
					} else if (getNavigation().isDone() && tickCount % 20 == 0) {
						getNavigation().moveTo(spot.x, spot.y, spot.z, 1.0);
					}
				}
				case 1 -> {
					getNavigation().stop();
					setAct(armed() ? ACT_READY : ACT_IDLE);
					if (s != null) {
						// facing out, away from the ship
						double dx = getX() - s.getX(), dz = getZ() - s.getZ();
						float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
						setYRot(yaw);
						setYBodyRot(yaw);
						setYHeadRot(yaw);
					}
				}
				default -> {
					if (horizontalSqTo(to) < 2.5 * 2.5 || now - escortSince > 2400) {
						escortShip = null;
						commuteUntil = now + 4800;
					} else {
						walkLong(to, 0.9);
					}
				}
			}
		}

		@Override
		public void stop() {
			if (act() == ACT_READY) {
				setAct(ACT_IDLE);
			}
		}
	}

	private class WorkGoal extends Goal {
		private int spot = -1;
		private boolean arrived;
		private int until, retryAt, stuck, lookAgain, fails;
		private CrewPost.@Nullable Rest resting;

		WorkGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return post() != null && getTarget() == null && level() instanceof ServerLevel;
		}

		@Override
		public void start() {
			arrived = false;
			retryAt = 0;
			CrewPost p = post();
			if (p != null) {
				resting = rest();
				entityData.set(PROP, carried(p, resting));
			}
		}

		/** What they have with them: their post's prop at work (a janitor's cart once it's out of the closet), a coffee on a
		 * break, nothing asleep. */
		private int carried(CrewPost p, CrewPost.@Nullable Rest r) {
			if (r == null) {
				return shown(p);
			}
			return r.act().equals("mug") ? PROP_MUG : PROP_NONE;
		}

		@Override
		public void stop() {
			getNavigation().stop();
			if (!isPassenger()) {
				setAct(ACT_IDLE);
			}
			if (heldProp() == PROP_CART) {
				entityData.set(PROP, PROP_NONE);   // a cart doesn't come along on a run or a chase
			}
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			CrewPost p = post();
			if (p == null) {
				return;
			}
			CrewPost.Rest r = rest();
			if (!java.util.Objects.equals(r, resting)) {
				// off to rest, or back to work
				resting = r;
				arrived = false;
				spot = -1;
				stuck = 0;
				retryAt = 0;
				fails = 0;
				lastSq = Double.MAX_VALUE;
				entityData.set(PROP, carried(p, r));
			}
			if (!isPassenger()) {
				// Lost (far out of the tower) or stuck a long while: put back where they are bound, but only out of
				// everyone's sight; while someone could see, they keep trying to walk it.
				BlockPos target = r != null ? world(r.stand()) : place(p, p.home());
				double away = distanceToSqr(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
				boolean lost = away > LOST * LOST && level().getGameTime() >= commuteUntil;
				if ((lost || stuck > STUCK_TICKS) && tickCount >= lookAgain) {
					lookAgain = tickCount + 100;
					if (!seen()) {
						putBack(target);
						stuck = 0;
						arrived = false;
						return;
					}
				}
			}
			if (p.cart() != null && p.cartStand() != null) {
				// a janitor's cart (C5, Justin: "custodian room only"): fetched from the custodian's closet before work, and
				// parked back there before bed
				if (r != null && r.equals(p.sleep()) && hasCart) {
					cart(p, false);
					return;
				}
				if (r == null && !hasCart && tickCount >= cartAgain) {
					cart(p, true);
					return;
				}
			}
			if (r != null) {
				settle(r);
				return;
			}
			// back to work from a rest seat (a sofa they slept on, a cafe chair): up out of it first (Justin, 7 November
			// 2026: after "time set day" the night's sleepers were "just sitting around on the couch in the middle of the
			// day"; anyone whose post is a station or a patrol stayed seated, only walking on once nobody could see them)
			if (!p.seated() && getVehicle() instanceof io.github.jimbozoomer.jugcraft.tower.SeatEntity) {
				stopRiding();
				return;
			}
			switch (p.kind()) {
				case "seat" -> seat(p);
				case "station" -> station(p);
				default -> patrol(p);
			}
		}

		/** To the rest's seat and settle there: asleep (as the rest says) or with a coffee. */
		private void settle(CrewPost.Rest r) {
			BlockPos cell = world(r.seat());
			if (isPassenger()) {
				if (!cell.equals(seatedAt)) {
					stopRiding();               // up out of their work chair first
					return;
				}
				if (level().getBlockState(cell).isAir()) {
					stopRiding();               // the seat is gone from under them
					return;
				}
				int act = act(r.act());
				setAct(act);
				if (asleep(act)) {
					// still, facing the way the seat does
					setYRot(r.yaw());
					setYBodyRot(r.yaw());
					setYHeadRot(r.yaw());
					setXRot(0);
				}
				return;
			}
			double d = distanceToSqr(cell.getX() + 0.5, cell.getY(), cell.getZ() + 0.5);
			if (d < 6.0) {
				getNavigation().stop();
				if (tickCount >= retryAt) {
					retryAt = tickCount + 40;
					Vec3 at = r.at().add(core.getX(), core.getY(), core.getZ());
					if (!sitAt(cell, at, r.yaw(), world(r.stand()))) {
						// taken: wait beside it
						setAct(ACT_IDLE);
						getLookControl().setLookAt(cell.getX() + 0.5, cell.getY() + 1.0, cell.getZ() + 0.5);
					}
				}
				return;
			}
			setAct(ACT_IDLE);
			walkTo(world(r.stand()), 0.7);
		}

		/**
		 * To the custodian's closet, to the cell before their cart, and take it out ({@code take}) or park it back. A cart
		 * that isn't there to take (or a cell to park in that's filled), or a closet they can't get to, and they go without
		 * (and try again later).
		 */
		private void cart(CrewPost p, boolean take) {
			BlockPos stand = world(p.cartStand()), cell = world(p.cart());
			entityData.set(PROP, take ? PROP_NONE : PROP_CART);
			if (take && !cartAt(cell)) {
				cartAgain = tickCount + 1200;           // nothing there to take: on without it a while
				return;
			}
			if (horizontalSq(stand) < 0.5 && Math.abs(getY() - floor(level(), stand)) < 1.0) {
				getNavigation().stop();
				getLookControl().setLookAt(cell.getX() + 0.5, cell.getY() + 0.5, cell.getZ() + 0.5);
				if (take) {
					takeCart(cell);
				} else {
					parkCartNow();
				}
				entityData.set(PROP, carried(p, rest()));
				arrived = false;
				spot = -1;
				stuck = 0;
				return;
			}
			if (stuck > STUCK_TICKS / 2) {
				stuck = 0;
				if (take) {
					cartAgain = tickCount + 2400;       // can't get to the closet now
				} else {
					parkCartNow();                      // (out of reach: back in its place all the same)
				}
				return;
			}
			setAct(take ? ACT_IDLE : ACT_PUSH);
			walkTo(stand, take ? 0.7 : 0.5);
		}

		private int cartAgain;

		private void seat(CrewPost p) {
			BlockPos chair = world(p.seat() == null ? p.home() : p.seat());
			if (isPassenger()) {
				if (level().getBlockState(chair).isAir() || seatedAt != null && !seatedAt.equals(chair)) {
					stopRiding();               // the chair is gone from under them, or they are still on a rest seat
					return;
				}
				setAct(act(p.act()));
				return;
			}
			BlockPos stand = world(p.home());
			double d = distanceToSqr(chair.getX() + 0.5, chair.getY(), chair.getZ() + 0.5);
			if (d < 6.0) {
				getNavigation().stop();
				if (tickCount >= retryAt) {
					retryAt = tickCount + 40;
					if (!sit(chair)) {
						// taken: wait beside it
						setAct(ACT_IDLE);
						getLookControl().setLookAt(chair.getX() + 0.5, chair.getY() + 1.0, chair.getZ() + 0.5);
					}
				}
				return;
			}
			setAct(ACT_IDLE);
			walkTo(stand, 0.8);
		}

		private void station(CrewPost p) {
			List<CrewPost.Spot> spots = p.spots();
			if (spots.isEmpty()) {
				return;
			}
			if (spot < 0 || arrived && tickCount >= until) {
				int next = getRandom().nextInt(spots.size());
				if (spots.size() > 1 && next == spot) {
					next = (next + 1) % spots.size();
				}
				spot = next;
				arrived = false;
			}
			CrewPost.Spot s = spots.get(Math.min(spot, spots.size() - 1));
			BlockPos at = world(s.pos());
			if (!arrived) {
				setAct(walking(p));
				if (horizontalSq(at) < 0.5 && onFloorOf(at)) {
					arrived = true;
					stuck = 0;
					getNavigation().stop();
					until = tickCount + 400 + getRandom().nextInt(800);
				} else if (spots.size() > 1 && (stuck > SKIP_TICKS || horizontalSq(at) < 6.0 && crowded(at))) {
					// can't get to that spot now (someone in the way, or standing on it): another one instead
					spot = (spot + 1 + getRandom().nextInt(spots.size() - 1)) % spots.size();
					stuck = 0;
					getNavigation().stop();
				} else {
					walkTo(at, 0.7);
				}
				return;
			}
			if (!Float.isNaN(s.yaw())) {
				setYRot(s.yaw());
				setYBodyRot(s.yaw());
				float rad = s.yaw() * Mth.DEG_TO_RAD;
				getLookControl().setLookAt(getX() - Mth.sin(rad) * 2, getEyeY() - 0.5, getZ() + Mth.cos(rad) * 2);
			}
			int work = act(p.act());
			// the gate guard is on the radio now and then
			if (work == ACT_RADIO && (tickCount / 200) % 3 != 0) {
				work = ACT_IDLE;
			}
			setAct(work);
		}

		private void patrol(CrewPost p) {
			List<CrewPost.Spot> spots = p.spots();
			if (spots.isEmpty()) {
				return;
			}
			if (spot < 0 || spot >= spots.size()) {
				spot = nearest(spots);
				arrived = false;
			}
			BlockPos at = place(p, spots.get(spot).pos());
			if (arrived) {
				setAct(walking(p));
				if (tickCount >= until) {
					spot = (spot + 1) % spots.size();
					arrived = false;
				}
				return;
			}
			setAct(walking(p));
			if (horizontalSq(at) < 1.0 && onFloorOf(at)) {
				arrived = true;
				stuck = 0;
				getNavigation().stop();
				// someone else of the crew already standing there: on at once, no standing in each other
				until = tickCount + (crowded(at) ? 0 : 60 + getRandom().nextInt(100));
			} else if (horizontalSq(at) < 6.0 && crowded(at) && spots.size() > 1) {
				spot = (spot + 1) % spots.size();
				getNavigation().stop();
			} else if (stuck > SKIP_TICKS && spots.size() > 1) {
				// can't get to this point of the round now: on to the next
				spot = (spot + 1) % spots.size();
				stuck = 0;
				getNavigation().stop();
			} else {
				walkTo(at, shown(p) == PROP_CART ? 0.5 : 0.65);
			}
		}

		/**
		 * Walks to {@code at}: a path planned there (as far as one goes) and walked; planned again whenever it runs out
		 * short. Each walk that can't be planned, or that ends without getting nearer, counts towards giving up (the
		 * spot, or, out of sight, being put back), and they wait a little longer before the next try.
		 */
		private void walkTo(BlockPos at, double speed) {
			if (!at.equals(lastTarget)) {
				lastTarget = at.immutable();
				lastSq = Double.MAX_VALUE;
				fails = 0;
			}
			if (refusedHops > 0) {
				stuck += 40 * refusedHops;               // hopping in place: as good as a walk that got nowhere
				fails += refusedHops;
				refusedHops = 0;
			}
			double near = horizontalSq(at);
			if (getNavigation().isDone() && near < 3.0 && near > 0.04 && Math.abs(getY() - floor(level(), at)) < 0.6) {
				// the last step straight in: a path ends as soon as it is within a block of where it's going, so it can
				// leave them a block short of their spot, where they stood idle until put back minutes later (found
				// 7 November 2026, testing the crew getting up off the sofas after "time set day")
				getMoveControl().setWantedPosition(at.getX() + 0.5, floor(level(), at), at.getZ() + 0.5, speed);
				stuck += 2;
				return;
			}
			// a long walk (far, or to another floor): by the tower's walk map, way point to way point (CrewRoutes); not the
			// patrols outside, whose round is laid out point to point round the building (the map's shortest way from one
			// point to the next can be through it)
			CrewPost mine = post();
			BlockPos via = mine != null && mine.outside() ? null : wayPoint(at);
			if (via != null) {
				boolean on = !via.equals(viaNow);
				if (on || getNavigation().isDone() && tickCount >= retryAt) {
					viaNow = via;
					getNavigation().setMaxVisitedNodesMultiplier(PATH_EFFORT);
					boolean planned = getNavigation().moveTo(via.getX() + 0.5, floor(level(), via), via.getZ() + 0.5, speed);
					if (on && planned) {
						fails = 0;               // on to the next way point: they are getting there
						stuck = 0;
					} else {
						fails++;
						stuck += 40;
						rethinkRoute();
					}
					lastSq = Double.MAX_VALUE;
					retryAt = tickCount + 40 + Math.min(fails, 6) * 20;
				}
				return;
			}
			viaNow = null;
			if (getNavigation().isDone() && tickCount >= retryAt) {
				getNavigation().setMaxVisitedNodesMultiplier(PATH_EFFORT);
				boolean planned = getNavigation().moveTo(at.getX() + 0.5, floor(level(), at), at.getZ() + 0.5, speed);
				double now = horizontalSq(at);
				if (!planned || now >= lastSq - 0.25) {
					fails++;
					stuck += 40;
				} else {
					fails = 0;
					stuck = 0;
				}
				lastSq = now;
				retryAt = tickCount + 40 + Math.min(fails, 6) * 20;
			}
		}

		private double lastSq = Double.MAX_VALUE;
		private @Nullable BlockPos lastTarget;

		private int nearest(List<CrewPost.Spot> spots) {
			int best = 0;
			double bestD = Double.MAX_VALUE;
			for (int i = 0; i < spots.size(); i++) {
				double d = horizontalSq(world(spots.get(i).pos()));
				if (d < bestD) {
					bestD = d;
					best = i;
				}
			}
			return best;
		}

		/** Whether another of the crew stands on the spot (within most of a block of its middle). */
		private boolean crowded(BlockPos at) {
			return !level().getEntitiesOfClass(TowerCrew.class, new AABB(at).inflate(0.4, 0.5, 0.4), c -> c != TowerCrew.this && c.isAlive()).isEmpty();
		}

		private double horizontalSq(BlockPos at) {
			double dx = at.getX() + 0.5 - getX(), dz = at.getZ() + 0.5 - getZ();
			return dx * dx + dz * dz;
		}

		/** Whether they stand on the floor {@code at} is on (not the one under or over it: a spot upstairs isn't reached
		 * from the hall below it, where the upstairs crew used to stop, 10 November 2026). */
		private boolean onFloorOf(BlockPos at) {
			return Math.abs(getY() - floor(level(), at)) < 1.5;
		}
	}

	/** Whether a janitor's cart (its first part) stands at {@code cell}. */
	private boolean cartAt(BlockPos cell) {
		return isCart(level().getBlockState(cell)) && level().getBlockState(cell).toString().contains("part=0");
	}

	private static boolean isCart(BlockState state) {
		return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().equals("janitor_cart");
	}

	/** The cart's second cell, beside its first (the way the cart's room kit lays its parts out: to its facing's right). */
	private static BlockPos cartPart1(BlockPos cell, BlockState first) {
		Direction facing = first.hasProperty(HorizontalDirectionalBlock.FACING) ? first.getValue(HorizontalDirectionalBlock.FACING) : Direction.SOUTH;
		return cell.relative(facing.getClockWise());
	}

	/** Takes the cart at {@code cell} out of the closet: its two blocks lifted (kept as they were), and it's in hand. */
	private void takeCart(BlockPos cell) {
		BlockState first = level().getBlockState(cell);
		BlockPos other = cartPart1(cell, first);
		BlockState second = level().getBlockState(other);
		cartState0 = first;
		cartState1 = isCart(second) ? second : null;
		int flags = net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE;
		if (cartState1 != null) {
			level().setBlock(other, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), flags);
		}
		level().setBlock(cell, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), flags);
		hasCart = true;
	}

	/** Their cart (if they have it out) back in its place in the closet as it stood, there and then. */
	private void parkCartNow() {
		if (!hasCart || !(level() instanceof ServerLevel)) {
			return;
		}
		hasCart = false;
		CrewPost p = post();
		BlockState first = cartState0, second = cartState1;
		cartState0 = null;
		cartState1 = null;
		if (p == null || p.cart() == null || first == null) {
			return;
		}
		BlockPos cell = world(p.cart()), other = cartPart1(cell, first);
		int flags = net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE;
		if (level().getBlockState(cell).canBeReplaced() && (second == null || level().getBlockState(other).canBeReplaced())) {
			level().setBlock(cell, first, flags);
			if (second != null) {
				level().setBlock(other, second, flags);
			}
		}
	}

	/**
	 * Sits on the chair at {@code chair} as a player would (the chair's own seat and facing; a room kit chair or a Pilot
	 * Simulator at the seat point its post gives); false if it is taken.
	 */
	boolean sit(BlockPos chair) {
		BlockState state = level().getBlockState(chair);
		Vec3 at;
		float yaw;
		CrewPost p = post();
		BlockPos stand = p != null ? world(p.home()) : chair;
		if (p != null && p.seatAt() != null) {
			if (state.isAir()) {
				return false;
			}
			at = p.seatAt().add(core.getX(), core.getY(), core.getZ());
			yaw = p.seatYaw();
		} else if (state.getBlock() instanceof ConsoleChairBlock) {
			at = ConsoleChairBlock.seat(chair, state);
			yaw = state.getValue(ConsoleChairBlock.ROTATION) * 22.5F;
		} else if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
			double height = state.getBlock() instanceof Sittable sittable ? sittable.seatHeight() : SeatEntity.OPERATOR_SEAT_HEIGHT;
			at = new Vec3(chair.getX() + 0.5, chair.getY() + height, chair.getZ() + 0.5);
			yaw = state.getValue(HorizontalDirectionalBlock.FACING).toYRot();
		} else {
			return false;
		}
		return sitAt(chair, at, yaw, stand);
	}

	/** Sits on the seat at {@code cell} at {@code at}, facing {@code yaw}, to step out to {@code stand}; false if taken. */
	boolean sitAt(BlockPos cell, Vec3 at, float yaw, BlockPos stand) {
		if (level().getBlockState(cell).isAir() || !SeatEntity.seat(level(), cell, this, at, yaw)) {
			return false;
		}
		setYBodyRot(yaw);
		seatedAt = cell.immutable();
		seatedStand = stand.immutable();
		return true;
	}
}
