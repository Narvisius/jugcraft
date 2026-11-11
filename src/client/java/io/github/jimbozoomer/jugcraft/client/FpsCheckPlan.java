package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.tower.crew.CrewShipSite;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * Where {@link FpsCheck} puts the camera (worked out on the server, from the world itself): the tower nearest the
 * player from outside, a view of each of its big fixtures (the ship parked outside, as while he plays), close in front
 * of a wall, a walk round the lobby and the floor above (Sight off, on, and with his vsync), and a drop ship parked
 * where one would land, near (inside the distance its bay is drawn to), between that and the distance the plainer model
 * takes over, far off, and from inside its bay (near and in its bay first without it), and two at night.
 */
public final class FpsCheckPlan {
	/**
	 * A camera position and the point it looks at, with or without the ship, at a time of day, profiled or not; for a
	 * walk, the points it moves through (camera and what it looks at, in turn) and for how long; whether what can't be
	 * seen is skipped ({@link Sight}, off for the "before" numbers) and whether his frame cap and vsync are on.
	 */
	public record View(String name, Vec3 cam, Vec3 look, boolean ship, long time, boolean profile, List<Vec3> path, double seconds, boolean sight,
			boolean vsync) {
		View(String name, Vec3 cam, Vec3 look, boolean ship, long time, boolean profile) {
			this(name, cam, look, ship, time, profile, List.of(), 0, true, false);
		}

		/** A walk through {@code path} (camera, look, camera, look...) over {@code seconds}. */
		static View walk(String name, List<Vec3> path, double seconds, boolean ship) {
			return new View(name, path.get(0), path.get(1), ship, 6000, false, List.copyOf(path), seconds, true, false);
		}

		public boolean walking() {
			return path.size() >= 4;
		}

		View named(String to, boolean sightOn, boolean vsyncOn) {
			return new View(to, cam, look, ship, time, false, path, seconds, sightOn, vsyncOn);
		}

		/**
		 * Where a walk's camera is and what it looks at {@code t} seconds in: along the path, easing in and out at each
		 * point, the head turning slowly from side to side as someone walking round would (up to 40 degrees each way).
		 */
		public Vec3[] at(double t) {
			int points = path.size() / 2;
			double f = Math.max(0, Math.min(1, t / seconds)) * (points - 1);
			int i = Math.min(points - 2, (int) f);
			double u = f - i, e = u * u * (3 - 2 * u);
			Vec3 cam = path.get(2 * i).lerp(path.get(2 * i + 2), e);
			Vec3 look = path.get(2 * i + 1).lerp(path.get(2 * i + 3), u);
			double turn = Math.toRadians(40) * Math.sin(2 * Math.PI * t / 7);
			Vec3 d = look.subtract(cam);
			double c = Math.cos(turn), s = Math.sin(turn);
			Vec3 turned = new Vec3(d.x * c - d.z * s, d.y, d.x * s + d.z * c);
			return new Vec3[] {cam, cam.add(turned)};
		}
	}

	/** The views, notes on how they were found, and where the ship parks and which way it faces. */
	public record Plan(List<View> views, String note, Vec3 land, float yaw) {
	}

	/** The fixtures given a view of their own (block entity names), in this order. */
	static final String[] FIXTURES = {"holo_projector", "drone_parts_shelving", "tower_control_console", "crystal_tree",
			"ops_tree", "big_map_table", "holo_command_table", "curved_video_wall", "hall", "drone_display_case"};

	private FpsCheckPlan() {
	}

	/** The views for the tower in {@code cores} nearest {@code me}; none if there is no tower. */
	public static Plan plan(ServerLevel level, Vec3 me, List<BlockPos> cores) {
		StringBuilder note = new StringBuilder();
		note.append("Player at ").append(fmt(me)).append("; towers: ").append(cores).append('\n');
		BlockPos core = null;
		for (BlockPos c : cores) {
			if (core == null || c.distToCenterSqr(me) < core.distToCenterSqr(me)) {
				core = c;
			}
		}
		List<View> views = new ArrayList<>();
		if (core == null) {
			note.append("No tower in this world.\n");
			return new Plan(views, note.toString(), Vec3.ZERO, 0);
		}
		note.append("Using the tower at ").append(core.toShortString()).append('\n');
		Vec3 c = Vec3.atBottomCenterOf(core);
		// outside
		views.add(new View("tower_overview", c.add(60, 45, -150), c.add(0, 30, 0), false, 6000, true));
		views.add(new View("tower_gates", c.add(10, 4, -82), c.add(0, 8, -50), false, 6000, false));
		// a view of each big fixture
		Map<String, BlockPos> found = fixtures(level, core);
		note.append("Fixtures found: ").append(found).append('\n');
		View lobby = null;
		for (Map.Entry<String, BlockPos> e : found.entrySet()) {
			Vec3 target = Vec3.atCenterOf(e.getValue()).add(0, 0.5, 0);
			Vec3 cam = cameraFor(level, target);
			if (cam == null) {
				note.append("  no clear spot to see the ").append(e.getKey()).append(" from\n");
				continue;
			}
			// the ship parked outside, as it is while he plays (his JFR: drawn even while he was inside)
			View v = new View("room_" + e.getKey(), cam, target, true, 6000, e.getKey().equals("holo_projector"));
			views.add(v);
			if (lobby == null) {
				lobby = v;
			}
		}
		// facing a wall from close by: what is drawn there is drawn unseen (with and without Sight)
		if (lobby != null) {
			Vec3[] wall = wallView(level, lobby.cam());
			if (wall != null) {
				View w = new View("wall_close", wall[0], wall[1], true, 6000, false);
				views.add(w.named("wall_close_sight_off", false, false));
				views.add(w);
			} else {
				note.append("  no wall close by to face\n");
			}
		}
		// a walk round the lobby and up to the floor above, like his (his JFR, 8 November 2026): through each fixture's
		// view in turn round the core, looking about; with Sight off, on, and on with his frame cap and vsync
		List<Vec3> walk = walkPath(views, core);
		if (walk.size() >= 6) {
			View w = View.walk("walk", walk, WALK_SECONDS, true);
			views.add(w.named("walk_sight_off", false, false));
			views.add(w);
			views.add(w.named("walk_his_vsync", true, true));
		} else {
			note.append("  too few fixtures for a walk\n");
		}
		// the drop ship: where it would land
		CrewShipSite.Site site = CrewShipSite.find(level, core, 9);
		Vec3 land;
		float yaw;
		if (site != null && !site.roof()) {
			land = site.land();
			yaw = site.yaw();
		} else {
			note.append("No landing site found (").append(site).append("); using 70 blocks out from the gates\n");
			land = c.add(0, 1, -126);
			yaw = 0;
		}
		note.append("Ship at ").append(fmt(land)).append(" facing ").append(String.format(Locale.ROOT, "%.1f", Math.toDegrees(yaw))).append('\n');
		Vec3 mid = local(land, yaw, 0, 5, -2);
		Vec3 dir = new Vec3(0.6, 0.25, 0.75).normalize();
		List<View> shipViews = new ArrayList<>();
		shipViews.add(new View("ship_near_22", local(land, yaw, dir.x * 22, 5 + dir.y * 22, -2 + dir.z * 22), mid, true, 6000, true));
		shipViews.add(new View("ship_mid_44", local(land, yaw, dir.x * 44, 5 + dir.y * 44, -2 + dir.z * 44), mid, true, 6000, false));
		shipViews.add(new View("ship_far_70", local(land, yaw, dir.x * 70, 5 + dir.y * 70, -2 + dir.z * 70), mid, true, 6000, false));
		shipViews.add(new View("ship_bay", local(land, yaw, 0, 4.95, 15), local(land, yaw, 0, 4.6, -10), true, 6000, true));
		for (View v : List.of(shipViews.get(0), shipViews.get(3))) { // near and in its bay without the ship first, to take its share out
			views.add(new View(v.name() + "_noship", v.cam(), v.look(), false, v.time(), false));
		}
		views.addAll(shipViews);
		views.add(new View("tower_overview_ship", c.add(60, 45, -150), c.add(0, 30, 0), true, 6000, false));
		views.add(new View("ship_near_22_night", shipViews.get(0).cam(), mid, true, 18000, false));
		if (lobby != null) {
			views.add(new View(lobby.name() + "_night", lobby.cam(), lobby.look(), true, 18000, false));
		}
		return new Plan(views, note.toString(), land, yaw);
	}

	/** How long a walk takes (seconds). */
	static final double WALK_SECONDS = 30;

	/**
	 * The walk: the fixtures' views on the core's floor in order round the core, then the ones on the floors above, and
	 * back to the first (camera, look, camera, look...).
	 */
	static List<Vec3> walkPath(List<View> views, BlockPos core) {
		Vec3 c = Vec3.atBottomCenterOf(core);
		List<View> floor = new ArrayList<>(), above = new ArrayList<>();
		for (View v : views) {
			if (!v.name().startsWith("room_")) {
				continue;
			}
			(v.cam().y - c.y < 5 ? floor : above).add(v);
		}
		java.util.Comparator<View> round = java.util.Comparator.comparingDouble(v -> Math.atan2(v.cam().z - c.z, v.cam().x - c.x));
		floor.sort(round);
		above.sort(round);
		List<Vec3> path = new ArrayList<>();
		for (View v : floor) {
			path.add(v.cam());
			path.add(v.look());
		}
		for (View v : above) {
			path.add(v.cam());
			path.add(v.look());
		}
		if (path.size() >= 4) {
			path.add(path.get(0));
			path.add(path.get(1));
		}
		return path;
	}

	/** From {@code from}, the nearest solid wall 4 to 20 blocks off at eye height: a camera 1.5 blocks from it and the point it faces. */
	static Vec3[] wallView(ServerLevel level, Vec3 from) {
		Vec3[] best = null;
		double bestD = Double.MAX_VALUE;
		for (int k = 0; k < 24; k++) {
			double a = Math.PI * 2 * k / 24;
			Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a));
			BlockHitResult hit = level.clip(new ClipContext(from, from.add(dir.scale(20)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
					CollisionContext.empty()));
			if (hit.getType() != HitResult.Type.BLOCK) {
				continue;
			}
			double d = hit.getLocation().distanceTo(from);
			if (d < 4 || d >= bestD || !level.getBlockState(hit.getBlockPos()).canOcclude()) {
				continue;
			}
			bestD = d;
			best = new Vec3[] {hit.getLocation().subtract(dir.scale(1.5)), hit.getLocation()};
		}
		return best;
	}

	/** The block entity of each fixture type within the tower's footprint nearest the core's level. */
	static Map<String, BlockPos> fixtures(ServerLevel level, BlockPos core) {
		Map<String, BlockPos> found = new LinkedHashMap<>();
		Map<String, Double> best = new LinkedHashMap<>();
		int r = 64;
		for (int cx = (core.getX() - r) >> 4; cx <= (core.getX() + r) >> 4; cx++) {
			for (int cz = (core.getZ() - r) >> 4; cz <= (core.getZ() + r) >> 4; cz++) {
				LevelChunk chunk = level.getChunk(cx, cz);
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					Identifier id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType());
					if (id == null) {
						continue;
					}
					for (String f : FIXTURES) {
						if (id.getPath().equals(f)) {
							// the lower floors are where people are most
							double d = Math.abs(be.getBlockPos().getY() - core.getY()) * 4 + be.getBlockPos().distSqr(core) / 400.0;
							if (!best.containsKey(f) || d < best.get(f)) {
								best.put(f, d);
								found.put(f, be.getBlockPos());
							}
						}
					}
				}
			}
		}
		Map<String, BlockPos> ordered = new LinkedHashMap<>();
		for (String f : FIXTURES) {
			if (found.containsKey(f)) {
				ordered.put(f, found.get(f));
			}
		}
		return ordered;
	}

	/** A spot 5 to 11.5 blocks from {@code target}, in the open, with a floor under it and a clear line to it. */
	static Vec3 cameraFor(ServerLevel level, Vec3 target) {
		double[] distances = {8, 6.5, 10, 5, 11.5};
		for (double d : distances) {
			for (int k = 0; k < 16; k++) {
				double a = Math.PI * 2 * k / 16;
				for (double up : new double[] {0.8, 2.0, -0.4}) {
					Vec3 cam = target.add(Math.cos(a) * d, up, Math.sin(a) * d);
					BlockPos at = BlockPos.containing(cam);
					if (!open(level, at) || !open(level, at.below()) && !open(level, at.above())) {
						continue;
					}
					boolean floor = false;
					for (int dy = 1; dy <= 4 && !floor; dy++) {
						floor = !open(level, at.below(dy));
					}
					if (!floor) {
						continue;
					}
					BlockHitResult hit = level.clip(new ClipContext(cam, target, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty()));
					if (hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceTo(target) < 2.0) {
						return cam;
					}
				}
			}
		}
		return null;
	}

	private static boolean open(ServerLevel level, BlockPos pos) {
		var state = level.getBlockState(pos);
		return state.isAir() || state.getCollisionShape(level, pos).isEmpty() && state.getFluidState().isEmpty();
	}

	/** A point of the ship's frame in the world (as CrewShipRenderer draws it: turned by its yaw about Y). */
	static Vec3 local(Vec3 land, float yaw, double x, double y, double z) {
		double c = Math.cos(yaw), s = Math.sin(yaw);
		return land.add(x * c + z * s, y, -x * s + z * c);
	}

	static String fmt(Vec3 v) {
		return String.format(Locale.ROOT, "(%.1f, %.1f, %.1f)", v.x, v.y, v.z);
	}
}
