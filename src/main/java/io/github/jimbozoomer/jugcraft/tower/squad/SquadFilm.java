package io.github.jimbozoomer.jugcraft.tower.squad;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The squad's film (Justin, 11 November 2026: "Make a mp4 clip of the video and audio of a 7 drop pod drop to them
 * forming up and all the pods blowing up ... have them also fight a warden, and have it show off the hole in its body as
 * they take it down"): the server's half of the scene the film camera (client/FilmShoot) shoots in a throwaway copy of
 * his world. Everything in it is the game's own: a squad of seven called down as a designator calls one
 * ({@link SquadDrop#launch}), ordered to hold a spot as the leader's panel orders it ({@link Legionary#command}), so each
 * pod blows itself apart once its soldier has walked clear; then a Warden comes up out of the ground, angry at them, and
 * they shoot it down with their rifles (the plasma wounds burned through it as they hit) or take their blades to it.
 *
 * <p>The ground is found near his tower: open, flat, dry and untouched (no leaves over it, nothing built), with the tower
 * behind the drop from where the camera starts; the pods' flare is put over the tower's side ({@link #flightSeed}), the
 * squad's spot on the near side well clear of every pod, and the Warden's on the side of that spot with the clearer walk.
 * The camera's places come with it (eye heights, clear of every pod's landing spot).
 */
public final class SquadFilm {
	/** The squad's size, and how far (blocks) the spot it holds, and the camera's places, keep from each pod's landing. */
	public static final int SIZE = 7;
	public static final double SPOT_CLEAR = 15, CAMERA_CLEAR = 12;
	/** How far (blocks) from the squad's spot the Warden comes up: just inside the reach of the nearest rifles, so they fire as it
	 * rises and roars (rising, it takes no harm, but the bolts still burn through it) and it has less ground to cover under fire. */
	public static final double WARDEN_OUT = 25;
	/** How far (blocks) the drop is put from the tower, at least and at most. */
	public static final double TOWER_NEAR = 110, TOWER_FAR = 240;

	public final Vec3 mark, out, across, anchor, warden;
	/** Which side (along {@link #across}) of the squad's spot the Warden comes up on. */
	public final int side;
	public final List<Vec3> spots;
	/** The seed of the flights' random source: it puts the flare over {@link #out} (the tower's side). */
	public final long flightSeed;
	/** The camera's places (eyes): looking up at the drop, at the landing, at the squad formed up, at the Warden rising. */
	public final Vec3 camSky, camGround, camForm, camRise;
	public final String note;

	// the scene as it goes (the server's thread writes; the client's reads)
	public volatile long launched = -1, formedAt = -1, wardenUp = -1, wardenEmerged = -1, wardenDied = -1;
	public volatile int wardenId = -1, podsLeft = -1, menOut, menAlive, menFormed, wardenHits;
	private @Nullable UUID owner;
	private float wardenHealth = -1;
	private final List<String> events = Collections.synchronizedList(new ArrayList<>());

	private SquadFilm(Vec3 mark, Vec3 out, Vec3 anchor, Vec3 warden, int side, List<Vec3> spots, long flightSeed, Vec3 camSky, Vec3 camGround,
			Vec3 camForm, Vec3 camRise, String note) {
		this.mark = mark;
		this.out = out;
		this.across = new Vec3(-out.z, 0, out.x);
		this.anchor = anchor;
		this.warden = warden;
		this.side = side;
		this.spots = List.copyOf(spots);
		this.flightSeed = flightSeed;
		this.camSky = camSky;
		this.camGround = camGround;
		this.camForm = camForm;
		this.camRise = camRise;
		this.note = note;
	}

	// ------------------------------------------------------------------ where

	/**
	 * Finds the ground for the scene near the tower nearest {@code near} (within 600 blocks; else round {@code near}),
	 * among the chunks already loaded; null if there's nowhere a squad can land. {@code note} gets what was looked at.
	 */
	public static @Nullable SquadFilm find(ServerLevel level, Vec3 near, List<BlockPos> towers, StringBuilder note) {
		BlockPos tower = null;
		for (BlockPos t : towers) {
			if (t.distToCenterSqr(near) < 600 * 600 && (tower == null || t.distToCenterSqr(near) < tower.distToCenterSqr(near))) {
				tower = t;
			}
		}
		Vec3 centre = tower != null ? Vec3.atBottomCenterOf(tower) : near;
		note.append(tower != null ? "Tower at " + fmt(centre) : "No tower within 600 blocks: round " + fmt(centre)).append('\n');
		double best = Double.POSITIVE_INFINITY;
		Vec3 bestMark = null, bestOut = null;
		int looked = 0;
		for (double r = tower != null ? TOWER_NEAR : 40; r <= (tower != null ? TOWER_FAR : 200); r += 10) {
			int n = Math.max(16, (int) (Mth.TWO_PI * r / 10));
			for (int i = 0; i < n; i++) {
				double a = Mth.TWO_PI * i / n;
				Vec3 m = new Vec3(centre.x + Math.cos(a) * r, 0, centre.z + Math.sin(a) * r);
				// the tower behind the drop from the camera's side (the flare over it); with no tower, any way
				Vec3[] ways = tower != null ? new Vec3[] {new Vec3(-Math.cos(a), 0, -Math.sin(a))}
						: new Vec3[] {new Vec3(1, 0, 0), new Vec3(0, 0, 1), new Vec3(-1, 0, 0), new Vec3(0, 0, -1)};
				for (Vec3 o : ways) {
					double s = score(level, m, o, towers) + r / 200;
					looked++;
					if (s < best) {
						best = s;
						bestMark = m;
						bestOut = o;
					}
				}
			}
		}
		note.append(String.format(Locale.ROOT, "Looked at %d places; the best scored %.2f%n", looked, best));
		if (bestMark == null || best > 1e6) {
			return null;
		}
		BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(bestMark));
		return at(level, new Vec3(top.getX() + 0.5, top.getY(), top.getZ() + 0.5), bestOut, note);
	}

	/**
	 * How poor a place {@code mark} is for the scene with the tower toward {@code out} (lower is better; huge: not
	 * loaded): how uneven the ground is over the whole of it (the pods, the squad's spot, the Warden's walk, the camera),
	 * with water, leaves over the ground, anything built, cliffs, and being near a tower counting against it.
	 */
	static double score(ServerLevel level, Vec3 mark, Vec3 out, List<BlockPos> towers) {
		for (BlockPos t : towers) {
			if (Math.hypot(t.getX() + 0.5 - mark.x, t.getZ() + 0.5 - mark.z) < 80) {
				return 1e9;
			}
		}
		Vec3 across = new Vec3(-out.z, 0, out.x);
		Integer base = top(level, mark.x, mark.z);
		if (base == null) {
			return 1e9;
		}
		double sum2 = 0;
		int n = 0, water = 0, leaves = 0, built = 0, cliff = 0;
		for (int u = -60; u <= 30; u += 3) {
			for (int v = -38; v <= 38; v += 3) {
				double x = mark.x + out.x * u + across.x * v, z = mark.z + out.z * u + across.z * v;
				Integer top = top(level, x, z);
				if (top == null) {
					return 1e9;
				}
				int bx = Mth.floor(x), bz = Mth.floor(z);
				int dy = top - base;
				sum2 += dy * dy;
				n++;
				if (Math.abs(dy) > 6) {
					cliff++;
				}
				if (level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz) != top) {
					leaves++;
				}
				BlockPos ground = new BlockPos(bx, top - 1, bz);
				BlockState g = level.getBlockState(ground);
				if (!level.getFluidState(ground).isEmpty() || !level.getFluidState(ground.above()).isEmpty()) {
					water++;
				} else if (!natural(g)) {
					built++;
				}
			}
		}
		return Math.sqrt(sum2 / n) + 40.0 * water / n + 25.0 * leaves / n + 60.0 * built / n + 100.0 * cliff / n;
	}

	/** Ground nature made (what a pod may land on and a squad walk over without it being someone's build). */
	private static boolean natural(BlockState g) {
		return g.is(BlockTags.DIRT) || g.is(BlockTags.SAND) || g.is(BlockTags.BASE_STONE_OVERWORLD) || g.is(BlockTags.TERRACOTTA)
				|| g.is(Blocks.GRAVEL) || g.is(Blocks.SNOW_BLOCK) || g.is(Blocks.SNOW) || g.is(Blocks.POWDER_SNOW) || g.is(Blocks.CLAY)
				|| g.is(Blocks.SANDSTONE) || g.is(Blocks.RED_SANDSTONE) || g.is(Blocks.ICE) || g.is(Blocks.PACKED_ICE) || g.is(Blocks.DIRT_PATH);
	}

	/** The ground's top (the first block over it, leaves aside) at (x, z), or null if its chunk isn't loaded. */
	private static @Nullable Integer top(ServerLevel level, double x, double z) {
		int bx = Mth.floor(x), bz = Mth.floor(z);
		if (level.getChunkSource().getChunkNow(bx >> 4, bz >> 4) == null) {
			return null;
		}
		return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
	}

	/**
	 * The scene with the drop marked at {@code mark} and the flare toward {@code out}: the pods' landing spots, the
	 * squad's spot behind them (toward the camera), the Warden's to one side of it, and the camera's places. Null if
	 * seven pods can't land round the mark.
	 */
	public static @Nullable SquadFilm at(ServerLevel level, Vec3 mark, Vec3 out, StringBuilder note) {
		out = new Vec3(out.x, 0, out.z).normalize();
		Vec3 across = new Vec3(-out.z, 0, out.x);
		List<Vec3> spots = SquadDrop.spots(level, mark, SIZE, RandomSource.create(Double.doubleToLongBits(mark.x * 31 + mark.z)));
		if (spots.size() < SIZE) {
			note.append("Only ").append(spots.size()).append(" places for pods round ").append(fmt(mark)).append('\n');
			return null;
		}
		// the squad's spot: behind the drop (toward the camera), clear of every pod
		Vec3 anchor = null;
		for (double d = 20; d <= 56 && anchor == null; d += 1) {
			Vec3 a = ground(level, mark.subtract(out.scale(d)), mark.y);
			if (a != null && clearOf(a, spots) >= SPOT_CLEAR) {
				anchor = a;
			}
		}
		if (anchor == null) {
			anchor = ground(level, mark.subtract(out.scale(40)), mark.y);
			if (anchor == null) {
				anchor = mark.subtract(out.scale(40));
			}
		}
		// the Warden's: to the side of the spot with the clearer walk in
		int side = 1;
		double bestWalk = Double.POSITIVE_INFINITY;
		for (int s : new int[] {1, -1}) {
			double w = walk(level, anchor, anchor.add(across.scale(s * WARDEN_OUT)));
			if (w < bestWalk) {
				bestWalk = w;
				side = s;
			}
		}
		Vec3 wardenAt = ground(level, anchor.add(across.scale(side * WARDEN_OUT)), anchor.y);
		if (wardenAt == null) {
			wardenAt = anchor.add(across.scale(side * WARDEN_OUT));
		}
		// the camera: looking up at the drop from behind the squad's spot (on the side away from the Warden); at the
		// landing from beside it; at the squad formed up with the Warden's ground behind them; low before the Warden
		Vec3 camSky = eye(level, clear(anchor.subtract(out.scale(8)).subtract(across.scale(side * 8)), mark, spots, CAMERA_CLEAR), 2.2);
		Vec3 camGround = eye(level, clear(mark.subtract(out.scale(13)).subtract(across.scale(side * 16)), mark, spots, 9), 1.9);
		Vec3 camForm = eye(level, clear(anchor.subtract(across.scale(side * 9)).subtract(out.scale(7)), mark, spots, 9), 2.6);
		Vec3 toSquad = anchor.subtract(wardenAt).multiply(1, 0, 1).normalize();
		Vec3 camRise = eye(level, wardenAt.add(toSquad.scale(7.5)).add(new Vec3(-toSquad.z, 0, toSquad.x).scale(2.5)), 1.1);
		long seed = flightSeed(out);
		String n = String.format(Locale.ROOT, "Mark %s, flare toward (%.2f, %.2f), squad's spot %s (%.1f from the nearest pod), Warden at %s (side %d,"
				+ " walk %.2f), flight seed %d%nPods land at %s%nCamera: sky %s, ground %s, form %s, rise %s%n", fmt(mark), out.x, out.z, fmt(anchor),
				clearOf(anchor, spots), fmt(wardenAt), side, bestWalk, seed, fmtAll(spots), fmt(camSky), fmt(camGround), fmt(camForm), fmt(camRise));
		note.append(n);
		return new SquadFilm(mark, out, anchor, wardenAt, side, spots, seed, camSky, camGround, camForm, camRise, note.toString());
	}

	/** The ground's top at the middle of the block at {@code p} (if loaded, dry and within 8 of {@code nearY}). */
	private static @Nullable Vec3 ground(ServerLevel level, Vec3 p, double nearY) {
		Integer top = top(level, p.x, p.z);
		if (top == null || Math.abs(top - nearY) > 8) {
			return null;
		}
		BlockPos below = new BlockPos(Mth.floor(p.x), top - 1, Mth.floor(p.z));
		if (!level.getFluidState(below).isEmpty() || !level.getFluidState(below.above()).isEmpty()) {
			return null;
		}
		return new Vec3(Mth.floor(p.x) + 0.5, top, Mth.floor(p.z) + 0.5);
	}

	/** How far (blocks, side to side) {@code p} is from the nearest of {@code spots}. */
	private static double clearOf(Vec3 p, List<Vec3> spots) {
		double d = Double.POSITIVE_INFINITY;
		for (Vec3 s : spots) {
			d = Math.min(d, Math.hypot(s.x - p.x, s.z - p.z));
		}
		return d;
	}

	/** {@code p}, moved out from the mark until it's {@code clear} blocks from every pod's spot. */
	private static Vec3 clear(Vec3 p, Vec3 mark, List<Vec3> spots, double clear) {
		Vec3 away = p.subtract(mark).multiply(1, 0, 1);
		away = away.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : away.normalize();
		for (int i = 0; i < 40 && clearOf(p, spots) < clear; i++) {
			p = p.add(away);
		}
		return p;
	}

	/** An eye {@code up} blocks over the ground at {@code p}, raised out of anything solid it would be in. */
	private static Vec3 eye(ServerLevel level, Vec3 p, double up) {
		Integer top = top(level, p.x, p.z);
		double y = (top == null ? p.y : level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(p.x), Mth.floor(p.z))) + up;
		Vec3 e = new Vec3(p.x, y, p.z);
		for (int i = 0; i < 8; i++) {
			BlockPos b = BlockPos.containing(e);
			if (level.getBlockState(b).getCollisionShape(level, b).isEmpty() && level.getBlockState(b.above()).getCollisionShape(level, b.above()).isEmpty()) {
				break;
			}
			e = e.add(0, 1, 0);
		}
		return e;
	}

	/** How poor a walk from {@code a} to {@code b} is: its ups and downs, with water and leaves counting against it. */
	private static double walk(ServerLevel level, Vec3 a, Vec3 b) {
		double cost = 0;
		Integer last = top(level, a.x, a.z);
		if (last == null) {
			return 1e9;
		}
		for (int i = 1; i <= 20; i++) {
			Vec3 p = a.lerp(b, i / 20.0);
			Integer top = top(level, p.x, p.z);
			if (top == null) {
				return 1e9;
			}
			cost += Math.abs(top - last) > 1 ? 4 : Math.abs(top - last);
			last = top;
			BlockPos below = new BlockPos(Mth.floor(p.x), top - 1, Mth.floor(p.z));
			if (!level.getFluidState(below).isEmpty()) {
				cost += 20;
			}
			if (level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(p.x), Mth.floor(p.z)) != top) {
				cost += 6;
			}
		}
		return cost;
	}

	/** A seed whose random source's first draw puts the squad's flare over {@code out} (SquadDrop.plan draws its way first). */
	static long flightSeed(Vec3 out) {
		double want = Math.atan2(out.z, out.x);
		long best = 1;
		double bestOff = 10;
		for (long seed = 1; seed < 400_000; seed++) {
			double a = RandomSource.create(seed).nextDouble() * Mth.TWO_PI;
			double off = Math.abs(Mth.wrapDegrees(Math.toDegrees(a - want)));
			if (off < bestOff) {
				bestOff = off;
				best = seed;
				if (off < 1.5) {
					break;
				}
			}
		}
		return best;
	}

	// ------------------------------------------------------------------ the scene

	/** Clears the ground for the scene: monsters and any squads or pods of the player's already there go. */
	public void clear(ServerLevel level, UUID player) {
		AABB box = new AABB(mark, mark).inflate(160, 80, 160);
		List<Entity> gone = new ArrayList<>();
		for (Entity e : level.getEntities((Entity) null, box, e -> e instanceof Enemy || e instanceof DropPod || e instanceof Legionary)) {
			gone.add(e);
		}
		gone.forEach(Entity::discard);
		SquadAccounts.get(level).clearField(player);
		event(level, "cleared " + gone.size() + " monsters, pods and soldiers");
	}

	/** The drop: seven pods called down for {@code player}, the leader's hatch toward the camera. */
	public void launch(ServerLevel level, UUID player) {
		owner = player;
		SquadDrop.launch(level, player, camSky, mark, spots, JugcraftSquads.index(SIZE), RandomSource.create(flightSeed));
		launched = level.getGameTime();
		event(level, "launched");
	}

	/** The squad's soldiers out (and alive) near the scene. */
	public List<Legionary> squad(ServerLevel level) {
		if (owner == null) {
			return List.of();
		}
		return level.getEntitiesOfClass(Legionary.class, new AABB(mark, mark).inflate(140, 60, 140), l -> owner.equals(l.owner()) && l.isAlive());
	}

	/** The pods still there. */
	public List<DropPod> pods(ServerLevel level) {
		if (owner == null) {
			return List.of();
		}
		return level.getEntitiesOfClass(DropPod.class, new AABB(mark, mark).inflate(80, 260, 80), p -> owner.equals(p.owner()) && p.start() == launched);
	}

	/** Each tick of the scene: soldiers out are ordered to hold the spot; the Warden's anger kept on them; the counts. */
	public void tick(ServerLevel level) {
		if (launched < 0) {
			return;
		}
		long now = level.getGameTime();
		List<Legionary> men = squad(level);
		int formed = 0;
		for (Legionary l : men) {
			if (l.order() != Legionary.HOLD || l.anchor() == null || l.anchor().distanceToSqr(anchor) > 0.01) {
				l.command(Legionary.HOLD, Legionary.PATROL_RADIUS, anchor, null);
			}
			if (Math.hypot(l.getX() - anchor.x, l.getZ() - anchor.z) < Legionary.RING + 2) {
				formed++;
			}
		}
		int out = Math.max(menOut, men.size());
		if (men.size() < menAlive && wardenUp >= 0) {
			event(level, "a soldier fell (" + men.size() + " left)");
		}
		menOut = out;
		menAlive = men.size();
		menFormed = formed;
		List<DropPod> standing = pods(level);
		int pods = standing.size();
		// a pod still waiting long after its soldier came out: what he's doing goes in the record, and he's given the
		// order again (it sends him on his way afresh)
		for (DropPod pd : standing) {
			long waiting = now - launched - pd.land() - DropPod.HATCH_AFTER - Legionary.DAZE_TICKS - 20;
			if (waiting > 200 && waiting % 100 == 0) {
				for (Legionary l : men) {
					if (l.place() == pd.place() && l.distanceTo(pd) < JugcraftSquads.CLEAR) {
						event(level, String.format(Locale.ROOT, "pod %d still waiting: its soldier %.1f from it, %.1f from his spot, target %s, dazed %s,"
								+ " path done %s; ordered again", pd.place(), l.distanceTo(pd), Math.hypot(l.getX() - anchor.x, l.getZ() - anchor.z),
								l.getTarget() == null ? "none" : l.getTarget().getType().toShortString(), l.dazed(), l.getNavigation().isDone()));
						l.command(Legionary.HOLD, Legionary.PATROL_RADIUS, anchor, null);
					}
				}
			}
		}
		if (pods != podsLeft && podsLeft >= 0) {
			event(level, pods < podsLeft ? "pods left: " + pods : "pods: " + pods);
		}
		podsLeft = pods;
		if (formedAt < 0 && pods == 0 && menOut >= SIZE && formed >= menAlive) {
			formedAt = now;
			event(level, "formed up: " + formed + " at the spot");
		}
		if (wardenUp >= 0 && wardenDied < 0) {
			Entity e = level.getEntity(wardenId);
			if (!(e instanceof Warden w) || !w.isAlive()) {
				wardenDied = now;
				event(level, "the Warden fell after " + wardenHits + " hits; " + menAlive + " of the squad standing");
				return;
			}
			if (wardenHealth >= 0 && w.getHealth() < wardenHealth) {
				wardenHits++;
				if (wardenHits == 1) {
					event(level, "first hit on the Warden");
				}
			}
			wardenHealth = w.getHealth();
			if (wardenEmerged < 0 && w.getPose() != Pose.EMERGING && now - wardenUp > 5) {
				wardenEmerged = now;
				event(level, "the Warden is up");
			}
			// once it's up: angry at the squad (their leader, or the nearest), kept so if it loses them
			if (wardenEmerged >= 0 && (now - wardenEmerged) % 40 == 0 && (w.getTarget() == null || !w.getTarget().isAlive())) {
				LivingEntity at = null;
				for (Legionary l : men) {
					if (at == null || l.leader() || l.distanceToSqr(w) < at.distanceToSqr(w) && !(at instanceof Legionary al && al.leader())) {
						at = l;
					}
				}
				if (at != null) {
					w.increaseAngerAt(at, 150, false);
				}
			}
		}
	}

	/** The Warden comes up out of the ground at its spot, facing the squad. */
	public @Nullable Warden raiseWarden(ServerLevel level) {
		Warden w = EntityTypes.WARDEN.create(level, EntitySpawnReason.TRIGGERED);
		if (w == null) {
			return null;
		}
		Vec3 to = anchor.subtract(warden);
		float yaw = (float) Math.toDegrees(Math.atan2(-to.x, to.z));
		w.snapTo(warden.x, warden.y, warden.z, yaw, 0);
		w.setYHeadRot(yaw);
		w.setYBodyRot(yaw);
		w.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(warden)), EntitySpawnReason.TRIGGERED, null);
		level.addFreshEntityWithPassengers(w);
		wardenId = w.getId();
		wardenUp = level.getGameTime();
		wardenHealth = w.getHealth();
		event(level, "the Warden comes up at " + fmt(warden));
		return w;
	}

	private void event(ServerLevel level, String what) {
		events.add((launched < 0 ? "-" : String.valueOf(level.getGameTime() - launched)) + " " + what);
	}

	/** What happened, tick by tick from the launch. */
	public List<String> events() {
		synchronized (events) {
			return new ArrayList<>(events);
		}
	}

	public static String fmt(Vec3 v) {
		return String.format(Locale.ROOT, "(%.1f, %.1f, %.1f)", v.x, v.y, v.z);
	}

	private static String fmtAll(List<Vec3> vs) {
		StringBuilder b = new StringBuilder();
		for (Vec3 v : vs) {
			b.append(b.length() == 0 ? "" : " ").append(fmt(v));
		}
		return b.toString();
	}
}
