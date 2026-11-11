package io.github.jimbozoomer.jugcraft.tower.crew;

import io.github.jimbozoomer.jugcraft.tower.TowerData;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.Node;
import org.jspecify.annotations.Nullable;

/**
 * The crew's long walks (Justin, 10 November 2026: "a bunch of NPCs that should have been inside walking outside ... I
 * only saw like 2 NPCs upstairs").
 *
 * <p>The game's path finder looks a bounded way ({@link TowerCrew#PATH_LENGTH} blocks, a few thousand cells). When it
 * doesn't find the whole walk it hands back the way to the cell nearest the goal in a straight line, and in a building
 * that is the wrong place to go: off a drop ship, the outside of the wall nearest the post (the cook stood outside the
 * kitchen's wall, a server outside the cafe's); back from a break downstairs, the floor right under an upstairs desk
 * (measured: put down outside the gates at midday, 28 of 47 were at their posts four minutes on, and nobody whose post
 * is upstairs had got upstairs).
 *
 * <p>So each tower has a walk map: every cell the crew can stand in round it, and every step the game lets them take from
 * one to the next, found by the crew's own path finder's rules ({@link CrewPaths.Evaluator}: the doors that open for
 * them, the gates' lanes, the stairs, not over the furniture), a few milliseconds a tick ({@link #BUDGET}), from every
 * post outwards to {@link #MARGIN} blocks round them; kept {@link #KEEP} ticks, then made again (and at once when the
 * tower is refitted or another tier of it is built). A long walk is planned over the map, which has no bound but its own size, and handed back as
 * way points {@link #STRIDE} cells apart: each a short walk the game's own path finder plans with ease
 * (TowerCrew.wayPoint). Nothing here knows the tower's rooms: it is whatever the path finder says can be walked, so it
 * holds for any layout, any tier, and whatever a player has built in the way.
 */
public final class CrewRoutes {
	/** How far round the posts the map reaches (blocks), and how far above and below them. */
	public static final int MARGIN = 40, ABOVE = 6, BELOW = 6;
	/** While a map is being made: at most this many cells a tick, and no longer than {@link #BUDGET} nanoseconds of one
	 * (the map round a Tri-Pillar is some 43,000 cells, about 0.6 s of work in all: spread thin, nobody sees it). A
	 * finished map is kept {@link #KEEP} ticks. */
	public static final int SLICE = 400, KEEP = 12000;
	public static final long BUDGET = 3_000_000L;
	/** Way points this many cells apart. */
	public static final int STRIDE = 6;
	/** A walk farther than this (blocks), or to another floor, is planned over the map. */
	public static final double LONG = 12;
	/** Walks planned a tick, over all the crew (the rest ask again a tick or two on). */
	public static final int PLANS_A_TICK = 4;

	private record Key(ResourceKey<Level> level, long core) {
	}

	/** A tower's walk map: each cell's steps on (as the path finder gives them), and the cells still to look at. */
	private static final class WalkMap {
		final Long2ObjectOpenHashMap<long[]> steps = new Long2ObjectOpenHashMap<>();
		final LongOpenHashSet seen = new LongOpenHashSet();
		final LongArrayFIFOQueue open = new LongArrayFIFOQueue();
		final BlockPos lo, hi;
		final int layout;
		final long since;
		long workedAt = Long.MIN_VALUE;
		/** What making it cost: nanoseconds in all, the dearest tick's, and the ticks it took. */
		long spent, dearest;
		int ticks;
		boolean done;

		WalkMap(BlockPos lo, BlockPos hi, int layout, long since) {
			this.lo = lo;
			this.hi = hi;
			this.layout = layout;
			this.since = since;
		}

		boolean inside(int x, int y, int z) {
			return x >= lo.getX() && x <= hi.getX() && y >= lo.getY() && y <= hi.getY() && z >= lo.getZ() && z <= hi.getZ();
		}
	}

	/** The map in use (finished) and the one being made to take its place. */
	private static final class Maps {
		@Nullable WalkMap ready, making;
	}

	private static final Map<Key, Maps> MAPS = new HashMap<>();
	private static long plansAt = Long.MIN_VALUE;
	private static int plans;

	private CrewRoutes() {
	}

	/** Forgets every map (the server stopped: another world's towers are other towers). */
	public static void clear() {
		MAPS.clear();
	}

	/** Forgets a tower's map (it was rebuilt or taken down): made again when next asked for. */
	public static void forget(Level level, BlockPos core) {
		MAPS.remove(new Key(level.dimension(), core.asLong()));
	}

	/**
	 * The way for {@code mob} from where it stands to {@code to} over the walk map of the tower at {@code core}: way
	 * points to walk to in turn, the last one the map's cell nearest {@code to}. Null when there is no way to give yet: the
	 * map is still being made (this call makes a little more of it), or too many walks were planned this tick. Empty when
	 * the finished map doesn't join the two places (one of them is off it, or walled off). Either way the caller walks
	 * as the game's path finder says, and asks again (soon for null, after a while for empty).
	 */
	public static @Nullable List<BlockPos> plan(TowerCrew mob, BlockPos core, BlockPos to) {
		if (!(mob.level() instanceof ServerLevel level) || !(mob.getNavigation().getNodeEvaluator() instanceof CrewPaths.Evaluator evaluator)) {
			return null;
		}
		long now = level.getGameTime();
		List<CrewPost> posts = TowerData.get().crew();
		if (posts.isEmpty()) {
			return null;
		}
		Maps maps = MAPS.computeIfAbsent(new Key(level.dimension(), core.asLong()), k -> new Maps());
		// the tower as it stands (its layout, how far it is built): a map made for another state of it is no use
		int layout = level.getBlockEntity(core) instanceof io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity tower
				? tower.layoutVersion() * 1000 + tower.tier() * 10 + tower.foundationState() : 0;
		if (maps.ready != null && maps.ready.layout != layout) {
			maps.ready = null;
		}
		if (maps.making != null && maps.making.layout != layout) {
			maps.making = null;
		}
		boolean stale = maps.ready == null || now - maps.ready.since > KEEP;
		if (stale && maps.making == null) {
			maps.making = begin(level, core, posts, layout, now);
		}
		if (maps.making != null) {
			work(level, mob, evaluator, maps.making, now);
			if (maps.making.done) {
				maps.ready = maps.making;
				maps.making = null;
			}
		}
		WalkMap map = maps.ready;
		if (map == null) {
			return null;
		}
		if (plansAt != now) {
			plansAt = now;
			plans = 0;
		}
		if (plans >= PLANS_A_TICK) {
			return null;
		}
		plans++;
		long from = nearest(map, mob.blockPosition()), goal = nearest(map, to);
		if (from == Long.MIN_VALUE || goal == Long.MIN_VALUE) {
			return List.of();
		}
		List<BlockPos> way = wayPoints(walk(map, from, goal));
		return way == null ? List.of() : way;
	}

	/** Whether the tower's map is made (the game tests wait for it). */
	public static boolean ready(Level level, BlockPos core) {
		Maps maps = MAPS.get(new Key(level.dimension(), core.asLong()));
		return maps != null && maps.ready != null;
	}

	/** How many cells the tower's finished map holds (0 with none). */
	public static int cells(Level level, BlockPos core) {
		Maps maps = MAPS.get(new Key(level.dimension(), core.asLong()));
		return maps == null || maps.ready == null ? 0 : maps.ready.steps.size();
	}

	/** The steps on from a cell of the tower's finished map (the game tests look at them); null if the cell isn't on it. */
	public static @Nullable List<BlockPos> steps(Level level, BlockPos core, BlockPos cell) {
		Maps maps = MAPS.get(new Key(level.dimension(), core.asLong()));
		long[] on = maps == null || maps.ready == null ? null : maps.ready.steps.get(cell.asLong());
		if (on == null) {
			return null;
		}
		List<BlockPos> out = new ArrayList<>();
		for (long to : on) {
			out.add(BlockPos.of(to));
		}
		return out;
	}

	private static WalkMap begin(ServerLevel level, BlockPos core, List<CrewPost> posts, int layout, long now) {
		int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
		List<BlockPos> seeds = new ArrayList<>();
		for (CrewPost post : posts) {
			BlockPos home = core.offset(post.home());
			if (post.outside()) {
				home = TowerCrew.ground(level, home);
			}
			seeds.add(home);
			x0 = Math.min(x0, home.getX());
			y0 = Math.min(y0, home.getY());
			z0 = Math.min(z0, home.getZ());
			x1 = Math.max(x1, home.getX());
			y1 = Math.max(y1, home.getY());
			z1 = Math.max(z1, home.getZ());
		}
		WalkMap map = new WalkMap(new BlockPos(x0 - MARGIN, Math.max(level.getMinY(), y0 - BELOW), z0 - MARGIN),
				new BlockPos(x1 + MARGIN, Math.min(level.getMaxY(), y1 + ABOVE), z1 + MARGIN), layout, now);
		for (BlockPos seed : seeds) {
			long cell = seed.asLong();
			if (map.seen.add(cell)) {
				map.open.enqueue(cell);
			}
		}
		return map;
	}

	/** Maps a slice more (once a tick): each open cell's steps on, as the path finder gives them for {@code mob}. */
	private static void work(ServerLevel level, TowerCrew mob, CrewPaths.Evaluator evaluator, WalkMap map, long now) {
		if (map.workedAt == now || map.done) {
			return;
		}
		map.workedAt = now;
		long began = System.nanoTime();
		Node[] next = new Node[32];
		evaluator.prepare(new PathNavigationRegion(level, map.lo, map.hi), mob);
		try {
			for (int n = 0; n < SLICE && !map.open.isEmpty(); n++) {
				if ((n & 15) == 15 && System.nanoTime() - began > BUDGET) {
					break;
				}
				long cell = map.open.dequeueLong();
				int count = evaluator.getNeighbors(next, evaluator.cell(BlockPos.getX(cell), BlockPos.getY(cell), BlockPos.getZ(cell)));
				long[] on = new long[count];
				int kept = 0;
				for (int i = 0; i < count; i++) {
					Node to = next[i];
					if (map.inside(to.x, to.y, to.z)) {
						long at = BlockPos.asLong(to.x, to.y, to.z);
						on[kept++] = at;
						if (map.seen.add(at)) {
							map.open.enqueue(at);
						}
					}
				}
				map.steps.put(cell, kept == count ? on : Arrays.copyOf(on, kept));
			}
		} finally {
			evaluator.done();
		}
		long took = System.nanoTime() - began;
		map.spent += took;
		map.dearest = Math.max(map.dearest, took);
		map.ticks++;
		if (map.open.isEmpty()) {
			map.done = true;
			map.seen.clear();
			io.github.jimbozoomer.jugcraft.Jugcraft.LOGGER.info("Tower crew: the walk map round {} is made: {} cells in {} ms over {} ticks (the dearest tick {} ms)",
					mob.core().toShortString(), map.steps.size(), map.spent / 1_000_000, map.ticks, String.format(java.util.Locale.ROOT, "%.1f", map.dearest / 1e6));
		}
	}

	/** The map's cell at {@code at}, or the nearest within two blocks of it (a chair's cell, a step); Long.MIN_VALUE if none. */
	private static long nearest(WalkMap map, BlockPos at) {
		long best = Long.MIN_VALUE;
		int bestD = Integer.MAX_VALUE;
		for (int dy = 0; dy <= 2; dy++) {
			for (int sy = dy == 0 ? 1 : -1; sy <= 1; sy += 2) {
				int y = at.getY() + dy * sy;
				for (int dx = -2; dx <= 2; dx++) {
					for (int dz = -2; dz <= 2; dz++) {
						int d = dx * dx + dz * dz + 4 * dy * dy;
						if (d < bestD) {
							long cell = BlockPos.asLong(at.getX() + dx, y, at.getZ() + dz);
							long[] on = map.steps.get(cell);
							if (on != null && on.length > 0) {
								best = cell;
								bestD = d;
							}
						}
					}
				}
			}
		}
		return best;
	}

	private record Open(long cell, float f) {
	}

	/** The shortest walk over the map from one cell to another (cells in order), or null if they aren't joined. */
	private static long @Nullable [] walk(WalkMap map, long from, long goal) {
		if (from == goal) {
			return new long[] {from};
		}
		int gx = BlockPos.getX(goal), gy = BlockPos.getY(goal), gz = BlockPos.getZ(goal);
		Long2FloatOpenHashMap cost = new Long2FloatOpenHashMap();
		cost.defaultReturnValue(Float.MAX_VALUE);
		Long2LongOpenHashMap came = new Long2LongOpenHashMap();
		PriorityQueue<Open> open = new PriorityQueue<>((a, b) -> Float.compare(a.f, b.f));
		cost.put(from, 0F);
		open.add(new Open(from, 0F));
		while (!open.isEmpty()) {
			Open o = open.poll();
			long cell = o.cell;
			if (cell == goal) {
				List<Long> back = new ArrayList<>();
				for (long c = goal; c != from; c = came.get(c)) {
					back.add(c);
				}
				back.add(from);
				long[] out = new long[back.size()];
				for (int i = 0; i < out.length; i++) {
					out[i] = back.get(out.length - 1 - i);
				}
				return out;
			}
			float g = cost.get(cell);
			int x = BlockPos.getX(cell), y = BlockPos.getY(cell), z = BlockPos.getZ(cell);
			if (o.f > g + heuristic(x, y, z, gx, gy, gz) + 1e-3F) {
				continue;                   // an older, dearer entry for this cell
			}
			long[] on = map.steps.get(cell);
			if (on == null) {
				continue;
			}
			for (long to : on) {
				int tx = BlockPos.getX(to), ty = BlockPos.getY(to), tz = BlockPos.getZ(to);
				float step = (float) Math.sqrt((tx - x) * (tx - x) + (ty - y) * (ty - y) + (tz - z) * (tz - z));
				float through = g + step;
				if (through < cost.get(to)) {
					cost.put(to, through);
					came.put(to, cell);
					open.add(new Open(to, through + heuristic(tx, ty, tz, gx, gy, gz)));
				}
			}
		}
		return null;
	}

	private static float heuristic(int x, int y, int z, int gx, int gy, int gz) {
		return (float) Math.sqrt((gx - x) * (gx - x) + (gy - y) * (gy - y) + (gz - z) * (gz - z));
	}

	/** A walk's way points: every {@link #STRIDE}th cell, each cell where the floor's height changes, and its last. */
	private static @Nullable List<BlockPos> wayPoints(long @Nullable [] cells) {
		if (cells == null) {
			return null;
		}
		List<BlockPos> out = new ArrayList<>();
		int last = 0;
		for (int i = 1; i < cells.length; i++) {
			boolean end = i == cells.length - 1;
			boolean rise = BlockPos.getY(cells[i]) != BlockPos.getY(cells[i - 1]);
			if (end || i - last >= STRIDE || rise && i - last >= 2) {
				out.add(BlockPos.of(cells[i]));
				last = i;
			}
		}
		if (out.isEmpty()) {
			out.add(BlockPos.of(cells[cells.length - 1]));
		}
		return out;
	}
}
