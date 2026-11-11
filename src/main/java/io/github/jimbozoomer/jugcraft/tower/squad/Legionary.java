package io.github.jimbozoomer.jugcraft.tower.squad;

import io.github.jimbozoomer.jugcraft.tower.crew.CrewPaths;
import io.github.jimbozoomer.jugcraft.tower.crew.CrewRoster;
import io.github.jimbozoomer.jugcraft.tower.crew.CrewShip;
import io.github.jimbozoomer.jugcraft.tower.crew.Hostiles;
import io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBlade;
import io.github.jimbozoomer.jugcraft.tower.crew.PlasmaRifle;
import io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A soldier of a drop pod squad (sheets 4 and 5): a Narv-Tech trooper in the security detail's power armour and full
 * helmet with the NT-7, out of a pod (DropPod), dazed a moment, then walking in to form up on the squad's leader (the
 * first pod's, a crest on the helmet by rank: {@link JugcraftSquads#RANKS}), who follows the player who called them down.
 * They fight what threatens them: hostile mobs they see, whoever hurts them, one of the squad or the player; never the
 * tower crew, tamed animals or each other, and players only where the server allows players to fight. If the leader
 * falls the next of them takes the crest.
 *
 * <p>The leader's orders (release (b); sheet 4's orders panel, client/SquadOrdersScreen, {@link SquadOrders}): FOLLOW ME
 * (as above), HOLD THIS POSITION (a ring round where he stood, facing out, engaging what comes in range but not chasing
 * it off), PATROL AREA (the leader walks the ground round that spot, in the radius picked, the squad in the wedge
 * behind him, pausing at each point) and SEND HOME (a drop ship comes for them: they wait where they are till it's down,
 * gather off the ramp's end, and are called up it one by one to a seat, the leader last; {@link CrewShip#setUpPickup}).
 * A squad is out for one in-game day ({@link #FIELD_TICKS}) from its call-in: a minute before, their player is told, and
 * when it's up the leader sends them home himself (once any fight is over).
 */
public class Legionary extends PathfinderMob implements PlasmaBlade.Wearer {
	/** The gear bit of each rank's crest (tools/crew_skins.py CRESTS: on the props layer, the optio's with his plumes). */
	public static final int[] CRESTS = {512, 1024, 2048};
	/** Ticks they stand dazed out of the pod; how far behind the leader the squad keeps (blocks a rank), how far the
	 * leader lets the player get before following, and how near he walks up to them. */
	public static final int DAZE_TICKS = 60;
	public static final double FILE = 2.2, LEAD_SLACK = 8, LEAD_NEAR = 4.5;
	/** Ticks a squad stays after a player who hurt one of them or the player they follow (as the crew do). */
	public static final int ANGER_TICKS = PlasmaRifle.ANGER_TICKS;
	/** Things they say when spoken to (message.jugcraft.legionary.N), the leader his own (message.jugcraft.legionary.lead.N). */
	public static final int LINES = 6;
	/** The leader's orders, and the patrol's radius (blocks: at least, at most, its step, at first). */
	public static final int FOLLOW = 0, HOLD = 1, PATROL = 2, HOME = 3;
	public static final int PATROL_MIN = 50, PATROL_MAX = 400, PATROL_STEP = 25, PATROL_RADIUS = 200;
	/** Holding, the ring's radius round the spot (blocks); patrolling, how long (ticks) the leader pauses at a point (at
	 * least, more), and gives up on reaching one; boarding, the walk up the ramp (blocks a tick) and how long (ticks) to
	 * reach the ramp's end before giving up. */
	public static final double RING = 3.5, BOARD_WALK = 0.14;
	public static final int PAUSE = 40, PAUSE_MORE = 100, PATROL_STUCK = 600, BOARD_GIVE_UP = 1200;
	/** A squad's time in the field (ticks from its call-in: one in-game day), how long before it's up their player is told,
	 * and how often (ticks) the leader tries to call the ship once it's up (not in a fight; somewhere it can land). Justin,
	 * 11 November 2026: "the legionaries will automatically call a drop ship to return one in game day after they're called
	 * in". */
	public static final int FIELD_TICKS = 24000, RECALL_WARNING = 1200, RECALL_RETRY = 100, RECALL_NO_ZONE = 600;
	private static final long UNKNOWN = Long.MIN_VALUE;
	/**
	 * Following (Justin, 11 November 2026: "I flew around for a second ... killed some mobs, they killed some mobs, and now
	 * they're just not following me ... players might have an ability to fly later on, so I don't want anything to interfere
	 * with ... these guys following them if they're set to follow"): a fight is let go once their player is further than
	 * {@value #LEASH} blocks from them, so they keep up (they take on what's near the player again when they're back with
	 * them); and further than {@value #CATCH_UP} blocks, or falling behind and not gaining for
	 * {@value #CATCH_UP_STUCK} looks {@value #CATCH_UP_CHECK} ticks apart (a wall, a cliff, water between them, a player
	 * flying or riding faster than they can run), they're brought up to their player: set down behind them where there's
	 * room to stand, on the ground under them if they're flying.
	 */
	public static final double LEASH = 20, CATCH_UP = 40;
	public static final int CATCH_UP_CHECK = 20, CATCH_UP_STUCK = 6;
	/** How far (blocks) they take a target on, and let it go beyond: the rifle's reach and a little to close in. */
	public static final double ENGAGE = PlasmaRifle.RANGE + 12;

	private static final EntityDataAccessor<String> SKIN = SynchedEntityData.defineId(Legionary.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Integer> GEAR = SynchedEntityData.defineId(Legionary.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> SLIM = SynchedEntityData.defineId(Legionary.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> ACT = SynchedEntityData.defineId(Legionary.class, EntityDataSerializers.INT);

	private @Nullable UUID owner, squad;
	private int squadIndex, place;
	private boolean leader;
	private int dazedUntil;
	private long angerUntil;
	private String given = "";
	private int lastTalk = -100, lastLine = -1;
	/** The squad's leader as last found (not saved), and when it was looked for. */
	private @Nullable Legionary leaderSeen;
	private int leaderLooked = -100;
	/** Their order (the squad's: the leader gives it to all of them), its spot (holding, patrolling, waiting for the ship),
	 * the patrol's radius, and the ship come for them. */
	private int order = FOLLOW, radius = PATROL_RADIUS;
	private @Nullable Vec3 anchor;
	private @Nullable UUID ship;
	/** Patrolling: the point the leader's making for, since when, and till when he pauses (not saved). */
	private @Nullable Vec3 patrolTo;
	private long patrolSince, pauseUntil;
	/** Called aboard: the way up the ramp to their seat, how far along, since when (not saved: called again if lost). */
	private @Nullable List<Vec3> boardWay;
	private int boardNext;
	private long boardSince;
	/** When the squad was called in (the game tick of its pods' launch; {@link #UNKNOWN}: not known yet, a soldier from
	 * before this was kept: his day starts when he's next about), and whether their player's been told it's nearly up. */
	private long calledAt = UNKNOWN;
	private boolean warned;
	/** When the leader next tries to call the ship (not saved): soon after a fight, a while after finding nowhere to land. */
	private long nextRecall;
	/** Following: how far behind their player they were at the last look, and how many looks they've not gained (not saved). */
	private double behindWas = -1;
	private int behindLooks;
	/** The client's own: the act it last saw and since when, and when it last saw them dazed (unused on the server). */
	public int clientAct = -1, clientActSince, clientDazed = -1000;

	public Legionary(EntityType<? extends Legionary> type, Level level) {
		super(type, level);
		setPersistenceRequired();
		getNavigation().setRequiredPathLength(TowerCrew.PATH_LENGTH);
		getNavigation().setCanOpenDoors(true);
	}

	/** Walking round the mod's pieces as the crew do (in the tower after the player). */
	@Override
	protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
		return new CrewPaths.Navigation(this, level);
	}

	public static AttributeSupplier.Builder attributes() {
		return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.ARMOR, 12.0).add(Attributes.ARMOR_TOUGHNESS, 4.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.4).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 4.0)
				.add(Attributes.FOLLOW_RANGE, 40.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(SKIN, "security_1");
		builder.define(GEAR, TowerCrew.GEAR_HELMET | TowerCrew.GEAR_EXO);
		builder.define(SLIM, false);
		builder.define(ACT, TowerCrew.ACT_IDLE);
	}

	/** Out of the pod: whose they are, their squad, its size, whether they lead it, and their place in it. */
	public void setUp(UUID owner, UUID squad, int squadIndex, boolean leader, int place) {
		this.owner = owner;
		this.squad = squad;
		this.squadIndex = squadIndex;
		this.place = place;
		CrewRoster.Hire hire = CrewRoster.get().hire("security", "legion:" + squad, place, squad.getMostSignificantBits(), Set.of());
		entityData.set(SKIN, hire.look().skin());
		entityData.set(SLIM, hire.look().slim());
		given = hire.name();
		dazedUntil = tickCount + DAZE_TICKS + getRandom().nextInt(20);
		setLeader(leader);
	}

	private void setLeader(boolean lead) {
		leader = lead;
		int gear = TowerCrew.GEAR_HELMET | TowerCrew.GEAR_EXO;
		if (lead) {
			gear |= CRESTS[Mth.clamp(squadIndex, 0, CRESTS.length - 1)];
		}
		entityData.set(GEAR, gear);
		String rank = lead ? JugcraftSquads.RANKS[Mth.clamp(squadIndex, 0, JugcraftSquads.RANKS.length - 1)] : "legionary";
		setCustomName(Component.translatable("entity.jugcraft.legionary.rank." + rank, given));
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

	@Override
	public int act() {
		return entityData.get(ACT);
	}

	private void setAct(int act) {
		entityData.set(ACT, act);
	}

	public @Nullable UUID owner() {
		return owner;
	}

	public @Nullable UUID squad() {
		return squad;
	}

	public boolean leader() {
		return leader;
	}

	public int place() {
		return place;
	}

	public boolean dazed() {
		return tickCount < dazedUntil;
	}

	public String given() {
		return given;
	}

	/** Their rank's key: the leader's by the squad's size, else legionary. */
	public String rankKey() {
		return leader ? JugcraftSquads.RANKS[Mth.clamp(squadIndex, 0, JugcraftSquads.RANKS.length - 1)] : "legionary";
	}

	public int squadIndex() {
		return squadIndex;
	}

	public int order() {
		return order;
	}

	public int radius() {
		return radius;
	}

	public @Nullable Vec3 anchor() {
		return anchor;
	}

	/** Whether they're in a fight (a target, or hurt a moment ago). */
	public boolean inCombat() {
		return getTarget() != null && getTarget().isAlive() || getLastHurtByMob() != null;
	}

	/** The squad's call-in (its pods' launch, game tick): their day in the field counts from it. */
	public void setCalledAt(long tick) {
		calledAt = tick;
	}

	public long calledAt() {
		return calledAt;
	}

	/** Whether their player's been told the squad's day is nearly up. */
	public boolean warned() {
		return warned;
	}

	/** The game tick their day in the field is up and the ship's called home (-1: not known yet). */
	public long recallAt() {
		return calledAt == UNKNOWN ? -1 : calledAt + FIELD_TICKS;
	}

	/**
	 * The leader's watch on the squad's day in the field: a minute before it's up their player is told; once it's up he
	 * calls a drop ship home ({@link SquadOrders#order}, as SEND HOME does), waiting out a fight and trying again till
	 * the ship's on its way (somewhere it can land). A squad already sent home is left be.
	 */
	private void fieldDay(ServerLevel level) {
		long now = level.getGameTime();
		if (calledAt == UNKNOWN) {
			calledAt = now;
		}
		if (!leader || order == HOME || boardWay != null) {
			return;
		}
		long left = calledAt + FIELD_TICKS - now;
		if (left <= RECALL_WARNING && !warned) {
			for (Legionary l : squadNear()) {
				l.warned = true;
			}
			tell(level, Component.translatable("message.jugcraft.squad.recall_soon", getDisplayName()));
		}
		if (left <= 0 && now >= nextRecall) {
			int result = SquadOrders.order(level, this, HOME, radius, null);
			if (result == SquadOrders.OK) {
				tell(level, Component.translatable("message.jugcraft.squad.recall_called", getDisplayName()));
			}
			nextRecall = now + (result == SquadOrders.NO_ZONE ? RECALL_NO_ZONE : RECALL_RETRY);
		}
	}

	/** A line in their player's chat (if they're on). */
	private void tell(ServerLevel level, Component message) {
		ServerPlayer p = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
		if (p != null) {
			p.sendSystemMessage(message);
		}
	}

	/** Gives them an order (the leader's, to each of the squad): its spot, the patrol's radius, the ship come for them. */
	public void command(int order, int radius, @Nullable Vec3 anchor, @Nullable UUID ship) {
		this.order = order;
		this.radius = Mth.clamp(radius, PATROL_MIN, PATROL_MAX);
		this.anchor = anchor;
		this.ship = ship;
		patrolTo = null;
		pauseUntil = 0;
		getNavigation().stop();
	}

	/** Whether they're sent home to ship {@code id}. */
	public boolean goingHome(UUID id) {
		return order == HOME && id.equals(ship);
	}

	/** Called aboard ship {@code id}: up {@code way} (from past the ramp's end to their seat). */
	public void embark(UUID id, List<Vec3> way) {
		ship = id;
		boardWay = way;
		boardNext = 0;
		boardSince = level().getGameTime();
	}

	/** Whether they're on their way aboard. */
	public boolean boarding() {
		return boardWay != null;
	}

	/** Whether they're on their way aboard and still on foot to the ramp. */
	public boolean toRamp() {
		return boardWay != null && boardNext == 0;
	}

	private @Nullable CrewShip pickupShip() {
		return ship != null && level() instanceof ServerLevel level && level.getEntity(ship) instanceof CrewShip s && s.isAlive() ? s : null;
	}

	// ---------------------------------------------------------------- the squad

	/** Whether {@code e} is one of this one's own squad. */
	public boolean squadmate(Entity e) {
		return e instanceof Legionary l && squad != null && squad.equals(l.squad);
	}

	/** The squad's members near (64 blocks), this one too. */
	public List<Legionary> squadNear() {
		return level().getEntitiesOfClass(Legionary.class, getBoundingBox().inflate(64), this::squadmate);
	}

	/** The leader of the squad (this one if it leads); if he's gone, the next of them in order takes the crest. */
	public @Nullable Legionary leaderOf() {
		if (leader) {
			return this;
		}
		if (leaderSeen != null && leaderSeen.isAlive() && leaderSeen.leader && tickCount - leaderLooked < 40) {
			return leaderSeen;
		}
		leaderLooked = tickCount;
		leaderSeen = null;
		Legionary first = this;
		for (Legionary l : squadNear()) {
			if (l.leader && l.isAlive()) {
				return leaderSeen = l;
			}
			if (l.place < first.place) {
				first = l;
			}
		}
		if (first == this && level() instanceof ServerLevel && tickCount > DAZE_TICKS + 60) {
			setLeader(true);                                // the leader fallen: the next in line leads
			return this;
		}
		return first.leader ? first : null;
	}

	/** The player they follow, if on and in this world. */
	public @Nullable Player ownerHere() {
		return owner == null ? null : level().getPlayerByUUID(owner);
	}

	/** Whether they may fight {@code e}: never the crew, tamed animals, each other or the player they follow; players only
	 * where the server allows it (and not in creative or spectating). */
	public boolean mayFight(LivingEntity e) {
		if (e == this || e instanceof Legionary || e instanceof TowerCrew || e instanceof TamableAnimal t && t.isTame() || !e.isAlive()) {
			return false;
		}
		if (e instanceof Player p) {
			return !p.getUUID().equals(owner) && !p.isCreative() && !p.isSpectator() && level() instanceof ServerLevel s && s.isPvpAllowed();
		}
		return true;
	}

	/** Whether they may take on a fight now: following, only while they're keeping up with their player. */
	public boolean keepingUp() {
		if (order != FOLLOW || owner == null) {
			return true;
		}
		Player p = ownerHere();
		return p == null || gap(p) <= LEASH;
	}

	/** Whether {@code e} is a fight for them now: following, one near their player (within the rifle's reach of them),
	 * while they're keeping up; otherwise anywhere in reach. */
	public boolean fightHere(LivingEntity e) {
		if (!keepingUp()) {
			return false;
		}
		Player p = order == FOLLOW && owner != null ? ownerHere() : null;
		return p == null || e.distanceToSqr(p) <= PlasmaRifle.RANGE * PlasmaRifle.RANGE;
	}

	/** How far {@code p} is from them: across the ground while the player's off it (flying, falling, riding high). */
	private double gap(Player p) {
		if (p.onGround() || p.isInWater()) {
			return distanceTo(p);
		}
		double dx = p.getX() - getX(), dz = p.getZ() - getZ();
		return Math.sqrt(dx * dx + dz * dz);
	}

	/** {@code attacker} hurt one of them or their player: the squad sees to them (players a while). */
	void provoked(ServerLevel level, LivingEntity attacker) {
		if (!mayFight(attacker)) {
			return;
		}
		for (Legionary l : squadNear()) {
			if (l.dazed()) {
				continue;
			}
			if (attacker instanceof Player) {
				l.angerUntil = level.getGameTime() + ANGER_TICKS;
			}
			if (l.getTarget() == null || !l.getTarget().isAlive() || attacker instanceof Player && !(l.getTarget() instanceof Player)) {
				l.setTarget(attacker);
			}
		}
	}

	// ---------------------------------------------------------------- goals

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(0, new DazedGoal());
		goalSelector.addGoal(0, new BoardGoal());
		goalSelector.addGoal(1, new RifleGoal());
		goalSelector.addGoal(1, new OpenDoorGoal(this, false));
		goalSelector.addGoal(3, new SquadGoal());
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new GuardGoal());
		// hostile mobs (tower/crew/Hostiles: every monster, and whatever is going for a player, the crew or them) in reach
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
				(e, level) -> Hostiles.hostile(e) && mayFight(e) && distanceToSqr(e) <= ENGAGE * ENGAGE && fightHere(e)));
	}

	/** Out of the pod: a few seconds on their feet, swaying, looking round, before they move. */
	private class DazedGoal extends Goal {
		DazedGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			return dazed();
		}

		@Override
		public void start() {
			getNavigation().stop();
			setAct(TowerCrew.ACT_IDLE);
		}

		@Override
		public void tick() {
			if (getRandom().nextInt(15) == 0) {
				double a = getRandom().nextDouble() * Mth.TWO_PI;
				getLookControl().setLookAt(getX() + Math.cos(a) * 4, getEyeY() + getRandom().nextDouble() * 1.6 - 1.1, getZ() + Math.sin(a) * 4);
			}
		}

		@Override
		public void stop() {
			setAct(TowerCrew.ACT_READY);
		}
	}

	/** Who hurt the player they follow, or the squad's targets: taken on (see {@link #provoked}). */
	private class GuardGoal extends TargetGoal {
		private int seen;

		GuardGoal() {
			super(Legionary.this, false);
			setFlags(EnumSet.of(Goal.Flag.TARGET));
		}

		@Override
		public boolean canUse() {
			Player p = ownerHere();
			if (p == null || dazed()) {
				return false;
			}
			LivingEntity a = p.getLastHurtByMob();
			return a != null && p.getLastHurtByMobTimestamp() != seen && mayFight(a) && a.distanceToSqr(Legionary.this) < 48 * 48 && keepingUp();
		}

		@Override
		public void start() {
			Player p = ownerHere();
			if (p != null && level() instanceof ServerLevel level) {
				seen = p.getLastHurtByMobTimestamp();
				LivingEntity a = p.getLastHurtByMob();
				if (a != null) {
					provoked(level, a);
				}
			}
			super.start();
		}

		@Override
		public boolean canContinueToUse() {
			return false;
		}
	}

	/**
	 * At their target: turned to it, the rifle raised ({@link TowerCrew#ACT_AIM}), a bolt every
	 * {@value PlasmaRifle#COOLDOWN} ticks while they see it within {@value PlasmaRifle#RANGE} blocks and nobody of theirs
	 * (or any player, or the crew) is in the way; closer if they can't see it; back to the squad when it's gone.
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
			return !dazed() && (boardWay == null || boardNext == 0) && t != null && t.isAlive() && level() instanceof ServerLevel && mayFight(t)
					&& distanceToSqr(t) <= ENGAGE * ENGAGE;
		}

		@Override
		public boolean canContinueToUse() {
			return canUse() && unseen < 100;
		}

		@Override
		public void start() {
			getNavigation().stop();
			unseen = 0;
			blocked = 0;
			cooldown = 8 + getRandom().nextInt(8);          // raising it (and not all at once)
			setAct(TowerCrew.ACT_AIM);
		}

		@Override
		public void stop() {
			getNavigation().stop();
			if (level() instanceof ServerLevel level) {
				blade.stow(level, Legionary.this);
			}
			setAct(TowerCrew.ACT_READY);
			if (unseen >= 100) {
				setTarget(null);
			}
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
			// too close to shoot: the armour's plasma blade (holding, they stand their ground with it)
			if (blade.tick(level, Legionary.this, t, sees, order != HOLD, Legionary.this::setAct)) {
				unseen = sees ? 0 : unseen + 1;
				cooldown = Math.max(cooldown, 10);          // (the rifle back up before it fires again)
				return;
			}
			setAct(TowerCrew.ACT_AIM);
			double range2 = PlasmaRifle.RANGE * PlasmaRifle.RANGE;
			if (sees && distanceToSqr(t) <= range2) {
				unseen = 0;
				if (blocked < PlasmaRifle.BLOCKED_TICKS || order == HOLD) {
					getNavigation().stop();                 // (holding, the ring stays as it is)
				} else if (getNavigation().isDone()) {
					// someone's been in the way a while: over to where the line's clear (the other side first next time)
					Vec3 to = PlasmaRifle.aside(level, Legionary.this, t, side);
					getNavigation().moveTo(to.x, to.y, to.z, 1.1);
					side = -side;
					blocked = PlasmaRifle.BLOCKED_TICKS / 2;
				}
				float yaw = (float) (Mth.atan2(t.getZ() - getZ(), t.getX() - getX()) * Mth.RAD_TO_DEG) - 90F;
				setYRot(yaw);
				setYBodyRot(yaw);
			} else {
				unseen++;
				// (holding, they don't go after it: the ring stays where it is)
				if (order != HOLD && (getNavigation().isDone() || unseen % 20 == 1)) {
					getNavigation().moveTo(t, 1.15);
				}
			}
			if (--cooldown <= 0 && sees && distanceToSqr(t) <= range2) {
				Vec3 aim = new Vec3(t.getX(), t.getY(0.6), t.getZ());
				Vec3 eye = getEyePosition();
				Vec3 muzzle = eye.add(aim.subtract(eye).normalize()).add(0, -0.12, 0);
				if (PlasmaRifle.clearShot(level, Legionary.this, muzzle, aim, t)) {
					PlasmaRifle.fire(level, Legionary.this, muzzle, aim, t);
					cooldown = PlasmaRifle.COOLDOWN + getRandom().nextInt(6);
					blocked = 0;
				} else {
					cooldown = 5;
					blocked += 5;
				}
			}
		}
	}

	/**
	 * With the squad, by their order. The leader: following, keeps near the player (walking up when they're
	 * {@value #LEAD_SLACK} blocks off, faster further, holding where he is if the player's gone); holding, stands on the
	 * spot; patrolling, walks from point to point round the spot within the radius, pausing at each; sent home, waits on
	 * the spot till the ship's down, then goes to just off its ramp's end. The rest: holding, a ring round the spot,
	 * facing out; otherwise a wedge behind him, two abreast a rank, {@value #FILE} blocks apart.
	 */
	private class SquadGoal extends Goal {
		private int replan;

		SquadGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return !dazed() && boardWay == null && getTarget() == null;
		}

		@Override
		public boolean canContinueToUse() {
			return canUse();
		}

		@Override
		public void stop() {
			getNavigation().stop();
		}

		/** Where the leader should be now (or null: where he is). */
		private @Nullable Vec3 lead() {
			long now = level().getGameTime();
			switch (order) {
				case HOLD -> {
					return anchor == null || anchor.distanceToSqr(position()) < 2.0 * 2.0 ? null : anchor;
				}
				case PATROL -> {
					if (anchor == null) {
						return null;
					}
					if (patrolTo != null && (patrolTo.distanceToSqr(position()) < 3.0 * 3.0 || now - patrolSince > PATROL_STUCK)) {
						patrolTo = null;
						pauseUntil = now + PAUSE + getRandom().nextInt(PAUSE_MORE);
					}
					if (patrolTo == null && now >= pauseUntil) {
						patrolTo = patrolPoint();
						patrolSince = now;
					}
					return patrolTo;
				}
				case HOME -> {
					CrewShip s = pickupShip();
					if (s == null || s.phase() < io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.OPEN) {
						return anchor == null || anchor.distanceToSqr(position()) < 2.0 * 2.0 ? null : anchor;
					}
					// just off the ramp's end, out from the ship
					List<Vec3> off = s.wayOff();
					Vec3 foot = off.get(off.size() - 1), out = foot.subtract(off.get(off.size() - 2)).multiply(1, 0, 1).normalize();
					Vec3 at = foot.add(out.scale(5));
					return at.distanceToSqr(position()) < 2.0 * 2.0 ? null : at;
				}
				default -> {
					Player p = ownerHere();
					if (p == null || p.isSpectator()) {
						return null;
					}
					double d = gap(p);
					if (d < LEAD_SLACK && getNavigation().isDone() || d < LEAD_NEAR) {
						return null;
					}
					if (!p.onGround() && !p.isInWater()) {
						// flying (or riding, or falling): the ground under them
						Integer y = standY(level(), Mth.floor(p.getX()), Mth.floor(p.getY()), Mth.floor(p.getZ()));
						return y == null ? p.position() : new Vec3(p.getX(), y, p.getZ());
					}
					return p.position();
				}
			}
		}

		/** A point to patrol to: within the radius of the spot, on open ground that's loaded (null: none found this time). */
		private @Nullable Vec3 patrolPoint() {
			if (anchor == null) {
				return null;
			}
			for (int tries = 0; tries < 12; tries++) {
				double a = getRandom().nextDouble() * Mth.TWO_PI, r = radius * Math.sqrt(getRandom().nextDouble());
				int x = Mth.floor(anchor.x + Math.cos(a) * r), z = Mth.floor(anchor.z + Math.sin(a) * r);
				net.minecraft.core.BlockPos col = new net.minecraft.core.BlockPos(x, Mth.floor(anchor.y), z);
				if (!level().hasChunkAt(col)) {
					continue;
				}
				int top = level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
				net.minecraft.core.BlockPos below = new net.minecraft.core.BlockPos(x, top - 1, z);
				if (level().getFluidState(below).isEmpty() && level().getFluidState(below.above()).isEmpty()) {
					return new Vec3(x + 0.5, top, z + 0.5);
				}
			}
			return null;
		}

		/** Where one of the rest should be (or null: where they are), and the way they face standing there (or null). */
		private @Nullable Vec3 follower(Legionary lead, Vec3[] face) {
			int k = Math.max(1, place);
			if (order == HOLD && anchor != null) {
				// the ring: evenly round the spot, facing out
				double a = Mth.TWO_PI * (k - 1) / 6.0;
				Vec3 at = anchor.add(Math.cos(a) * RING, 0, Math.sin(a) * RING);
				face[0] = at.add(Math.cos(a) * 6, 1.4, Math.sin(a) * 6);
				return at.distanceToSqr(position()) < 1.5 * 1.5 ? null : at;
			}
			// the wedge: rank r = (place + 1) / 2 behind, left and right in turn, on the leader's heading
			int rank = (k + 1) / 2;
			int side = k % 2 == 1 ? -1 : 1;
			float yaw = lead.getYRot() * Mth.DEG_TO_RAD;
			double fx = -Mth.sin(yaw), fz = Mth.cos(yaw);         // the leader's heading
			double rx = -fz, rz = fx;                              // his right
			double back = FILE * rank, across = side * FILE * 0.8 * rank;
			Vec3 at = new Vec3(lead.getX() - fx * back + rx * across, lead.getY(), lead.getZ() - fz * back + rz * across);
			return at.distanceToSqr(position()) < 2.5 * 2.5 ? null : at;
		}

		@Override
		public void tick() {
			if (--replan > 0) {
				return;
			}
			replan = 10;
			Vec3 g;
			Vec3[] face = new Vec3[1];
			if (leader) {
				g = lead();
			} else {
				Legionary lead = leaderOf();
				g = lead == null || lead == Legionary.this ? null : follower(lead, face);
			}
			if (g == null) {
				getNavigation().stop();
				if (face[0] != null) {
					getLookControl().setLookAt(face[0].x, face[0].y, face[0].z);
				}
				return;
			}
			double d = g.distanceTo(position());
			double speed = order == PATROL && leader ? 0.75 : d > 20 ? 1.3 : d > 8 ? 1.1 : 0.9;
			getNavigation().moveTo(g.x, g.y, g.z, speed);
		}
	}

	/**
	 * Called aboard a pickup ship: on foot to just past its ramp's end, then up the ramp, along the deck and the aisle to
	 * their seat (the hull is solid, so they're moved along the way: as the crew come off), and aboard: the ship has them
	 * from there. One who can't reach the ramp in a minute is left behind, holding where they are.
	 */
	private class BoardGoal extends Goal {
		private int replan;

		BoardGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			return boardWay != null && !dazed() && level() instanceof ServerLevel;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			List<Vec3> way = boardWay;
			if (way == null || way.isEmpty()) {
				return;
			}
			CrewShip s = pickupShip();
			long now = level().getGameTime();
			if (s == null || s.phase() > io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.UNLOAD) {
				leave();                                    // the ship's gone without them
				return;
			}
			if (boardNext == 0) {
				// on foot to the ramp's end (near enough will do: the ramp's own block may be in the way of the last step)
				Vec3 start = way.get(0);
				double dx = start.x - getX(), dz = start.z - getZ();
				if (dx * dx + dz * dz > 2.5 * 2.5 || Math.abs(start.y - getY()) > 1.5) {
					if (now - boardSince > BOARD_GIVE_UP) {
						leave();                            // can't get there: left behind
						return;
					}
					if (getNavigation().isDone() || --replan <= 0) {
						replan = 20;
						getNavigation().moveTo(start.x, start.y, start.z, 1.0);
					}
					return;
				}
				getNavigation().stop();
				noPhysics = true;
				setNoGravity(true);
			}
			Vec3 to = way.get(boardNext);
			Vec3 d = to.subtract(position());
			double len = d.length();
			if (len <= BOARD_WALK) {
				setPos(to);
				if (++boardNext >= way.size()) {
					boardWay = null;
					noPhysics = false;
					setNoGravity(false);
					s.aboard(Legionary.this);
				}
			} else {
				Vec3 step = d.scale(BOARD_WALK / len);
				setPos(position().add(step));
				float yaw = (float) (Mth.atan2(step.z, step.x) * Mth.RAD_TO_DEG) - 90.0F;
				setYRot(yaw);
				setYBodyRot(yaw);
				setYHeadRot(yaw);
			}
			setDeltaMovement(Vec3.ZERO);
		}

		/** Off the way: on their feet, holding where they are. */
		private void leave() {
			boardWay = null;
			noPhysics = false;
			setNoGravity(false);
			command(HOLD, radius, position(), null);
		}
	}

	// ---------------------------------------------------------------- life and death

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}
		if (player instanceof ServerPlayer server && leader && player.getUUID().equals(owner)) {
			SquadOrders.page(server, this, true, "");          // his orders (sheet 4's panel)
			return InteractionResult.SUCCESS;
		}
		if (player instanceof ServerPlayer server && tickCount - lastTalk >= 20) {
			lastTalk = tickCount;
			int line = getRandom().nextInt(LINES);
			if (line == lastLine) {
				line = (line + 1) % LINES;
			}
			lastLine = line;
			String what = player.getUUID().equals(owner) ? leader ? "lead" : "own" : "other";
			server.sendSystemMessage(Component.translatable("message.jugcraft.legionary.says", getDisplayName(),
					Component.translatable("message.jugcraft.legionary." + what + "." + line)));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.getEntity() instanceof Legionary l && squadmate(l) || source.getEntity() instanceof Player p && p.getUUID().equals(owner)
				&& !p.isCreative()) {
			return false;                                   // (no harm from their own: a squadmate's stray bolt, their player's)
		}
		boolean hurt = super.hurtServer(level, source, amount);
		if (hurt && source.getEntity() instanceof LivingEntity attacker) {
			dazedUntil = 0;
			provoked(level, attacker);
		}
		return hurt;
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level && owner != null) {
			SquadAccounts.get(level).back(owner, getUUID());
		}
	}

	@Override
	public void remove(RemovalReason reason) {
		if (reason.shouldDestroy() && level() instanceof ServerLevel level && owner != null) {
			SquadAccounts.get(level).back(owner, getUUID());
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

	/** Following: falling behind their player and not gaining, or far behind, they're brought up to them (see {@link #LEASH}). */
	private void keepUp(ServerLevel level) {
		Player p = ownerHere();
		if (p == null || p.isSpectator()) {
			behindWas = -1;
			behindLooks = 0;
			return;
		}
		double d = gap(p);
		if (d <= LEASH) {
			behindWas = -1;
			behindLooks = 0;
			return;
		}
		behindLooks = behindWas >= 0 && d > behindWas - 1.0 ? behindLooks + 1 : 0;
		behindWas = d;
		if (d > CATCH_UP || behindLooks >= CATCH_UP_STUCK) {
			Vec3 at = catchUpSpot(level, p);
			if (at != null) {
				setTarget(null);
				getNavigation().stop();
				snapTo(at.x, at.y, at.z, p.getYRot(), 0F);
				setYHeadRot(p.getYRot());
				setYBodyRot(p.getYRot());
				setDeltaMovement(Vec3.ZERO);
				resetFallDistance();
				behindWas = -1;
				behindLooks = 0;
			}
		}
	}

	/**
	 * Where to set them down by {@code p}, catching up: behind the player (fanning out from straight behind, each soldier
	 * a little apart by their place), 3 to 9 blocks off, where a soldier can stand; null if there's nowhere near.
	 */
	public @Nullable Vec3 catchUpSpot(Level level, Player p) {
		float yaw = p.getYRot() * Mth.DEG_TO_RAD;
		double behind = Math.atan2(Mth.cos(yaw), Mth.sin(yaw)) + Math.PI;   // the player looks along (-sin, cos)
		int k = Math.max(0, place);
		for (int tries = 0; tries < 24; tries++) {
			double a = behind + (k % 2 == 0 ? 1 : -1) * (k / 2) * 0.3 + (tries % 2 == 0 ? 1 : -1) * (tries / 2) * 0.35;
			double r = 3.0 + (k % 4) * 0.8 + tries / 8;
			int x = Mth.floor(p.getX() + Math.cos(a) * r), z = Mth.floor(p.getZ() + Math.sin(a) * r);
			Integer y = standY(level, x, Mth.floor(p.getY()), z);
			if (y != null) {
				return new Vec3(x + 0.5, y, z + 0.5);
			}
		}
		return null;
	}

	/**
	 * The level a soldier stands at in column {@code x z}: the first place from just over {@code from} down to 6 below
	 * where they can stand; failing that (the player flying, or up on something) the ground at the column's top. Null:
	 * none (water, lava, nothing to stand on, not loaded).
	 */
	private @Nullable Integer standY(Level level, int x, int from, int z) {
		BlockPos at = new BlockPos(x, from, z);
		if (!level.hasChunkAt(at)) {
			return null;
		}
		for (int y = from + 1; y >= from - 6; y--) {
			if (canStand(level, at.atY(y))) {
				return y;
			}
		}
		int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		return top < from - 6 && canStand(level, at.atY(top)) ? top : null;
	}

	/** Whether a soldier can stand at {@code at}: firm ground under them (not in water or lava) and room. */
	private boolean canStand(Level level, BlockPos at) {
		if (WalkNodeEvaluator.getPathTypeStatic(this, at) != PathType.WALKABLE) {
			return false;
		}
		AABB box = getDimensions(getPose()).makeBoundingBox(new Vec3(at.getX() + 0.5, at.getY(), at.getZ() + 0.5));
		return level.noCollision(this, box) && !level.containsAnyLiquid(box);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		LivingEntity t = getTarget();
		if (t != null && (!t.isAlive() || !mayFight(t) || distanceToSqr(t) > ENGAGE * ENGAGE
				|| t instanceof Player && level.getGameTime() > angerUntil || !fightHere(t))) {
			setTarget(null);                                // let go: gone, out of reach, a player after a while, or ours leaving
		}
		if (!dazed() && act() == TowerCrew.ACT_IDLE) {
			setAct(TowerCrew.ACT_READY);
		}
		// wounds close slowly out of a fight (the suit's medkit)
		if (t == null && tickCount % 40 == 0 && getHealth() < getMaxHealth() && tickCount - getLastHurtByMobTimestamp() > 200) {
			heal(1.0F);
		}
		if (tickCount % 20 == 0 && !leader) {
			Legionary lead = leaderOf();                    // (the crest passed on if the leader fell)
			if (lead != null && lead != this && boardWay == null && (lead.order != order || !java.util.Objects.equals(lead.anchor, anchor))) {
				command(lead.order, lead.radius, lead.anchor, lead.ship);   // one who missed an order (out of earshot) takes the leader's
			}
		}
		if (order == FOLLOW && owner != null && !dazed() && boardWay == null && tickCount % CATCH_UP_CHECK == 0) {
			keepUp(level);
		}
		if (tickCount % 100 == 50 && owner != null && !SquadAccounts.get(level).listed(owner, getUUID())) {
			discard();                                      // recalled (left out of reach while their player called another)
		}
		if (owner != null && !dazed()) {
			fieldDay(level);                                // their day in the field: the ship called home when it's up
		}
		// holding: what's gone out of range of the spot is let go (they don't leave the ring for it)
		if (order == HOLD && anchor != null && t != null && t.distanceToSqr(anchor) > (PlasmaRifle.RANGE + 4) * (PlasmaRifle.RANGE + 4)) {
			setTarget(null);
		}
		// sent home and the ship gone (or never come): they hold where they are
		if (order == HOME && boardWay == null && tickCount % 40 == 0 && pickupShip() == null) {
			command(HOLD, radius, position(), null);
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (owner != null) {
			output.store("owner", UUIDUtil.CODEC, owner);
		}
		if (squad != null) {
			output.store("squad", UUIDUtil.CODEC, squad);
		}
		output.putInt("squad_index", squadIndex);
		output.putInt("place", place);
		output.putBoolean("leader", leader);
		output.putString("given", given);
		output.putString("skin", skin());
		output.putBoolean("slim", slim());
		output.putInt("order", order);
		output.putInt("radius", radius);
		if (anchor != null) {
			output.store("anchor", Vec3.CODEC, anchor);
		}
		if (ship != null) {
			output.store("ship", UUIDUtil.CODEC, ship);
		}
		if (calledAt != UNKNOWN) {
			output.putLong("called_at", calledAt);
		}
		output.putBoolean("warned", warned);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
		squad = input.read("squad", UUIDUtil.CODEC).orElse(null);
		squadIndex = input.getIntOr("squad_index", 0);
		place = input.getIntOr("place", 0);
		given = input.getStringOr("given", "");
		entityData.set(SKIN, input.getStringOr("skin", "security_1"));
		entityData.set(SLIM, input.getBooleanOr("slim", false));
		setLeader(input.getBooleanOr("leader", false));
		setAct(TowerCrew.ACT_READY);
		order = input.getIntOr("order", FOLLOW);
		radius = Mth.clamp(input.getIntOr("radius", PATROL_RADIUS), PATROL_MIN, PATROL_MAX);
		anchor = input.read("anchor", Vec3.CODEC).orElse(null);
		ship = input.read("ship", UUIDUtil.CODEC).orElse(null);
		calledAt = input.getLongOr("called_at", UNKNOWN);
		warned = input.getBooleanOr("warned", false);
	}

}
