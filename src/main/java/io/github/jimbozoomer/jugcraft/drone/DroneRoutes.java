package io.github.jimbozoomer.jugcraft.drone;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Surface flight routes. Drones follow the terrain: they climb over hills and mountains with
 * {@link #CLEARANCE} blocks to spare and come back down, instead of flying through anything.
 *
 * <p>A route reads the height map every block along a straight line and the columns either side of it, at
 * most {@link #MAX_SAMPLES} steps per leg, and only in loaded chunks. Points under a roof, an overhang or the
 * tower's own floors are reached from the side: the drone comes down where the sky is open and flies in level
 * ({@link #approach}). Points with no such way in are "covered" and skipped (they are listed on the terminal).
 */
public final class DroneRoutes {
	public static final int CLEARANCE = 6;
	public static final int SAMPLE_STEP = 4;
	public static final int MAX_SAMPLES = 256;
	/** Every leg lifts at least this far above its higher end, even between neighbouring points. */
	public static final int MIN_HOP = 3;

	private DroneRoutes() {
	}

	/** True when nothing solid is above {@code pos}, so a drone can come straight down to it. */
	public static boolean openSky(ServerLevel level, BlockPos pos) {
		if (!level.isLoaded(pos)) {
			return false;
		}
		return level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) <= pos.getY();
	}

	/**
	 * Cruise height for a leg from {@code from} to {@code to}: {@link #CLEARANCE} blocks above the highest
	 * ground under the line between them and the columns either side of it (the drone is up to two blocks
	 * wide), checked every block (at most {@link #MAX_SAMPLES} steps), and never below either end. The drone
	 * climbs straight up to it, flies level and comes straight down ({@link FlightPath}), so it goes over
	 * walls, towers and hills, never through them. Every leg lifts at least {@link #MIN_HOP} blocks above its
	 * higher end. Chunks the leg crosses that are not loaded are flown over without looking (no chunk is ever
	 * loaded for a route, and nobody is there to see): only the two ends must be loaded. Returns NaN if one isn't.
	 */
	public static double cruiseHeight(ServerLevel level, Vec3 from, Vec3 to) {
		double dx = to.x - from.x;
		double dz = to.z - from.z;
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		int samples = (int) Math.min(MAX_SAMPLES, Math.max(1, Math.ceil(horizontal)));
		double cruise = Math.max(from.y, to.y) + MIN_HOP;
		// One block to each side of the line.
		double sideX = horizontal > 0 ? -dz / horizontal : 0;
		double sideZ = horizontal > 0 ? dx / horizontal : 0;
		for (int i = 0; i <= samples; i++) {
			double t = (double) i / samples;
			double px = from.x + dx * t;
			double pz = from.z + dz * t;
			for (int side = -1; side <= 1; side++) {
				int x = (int) Math.floor(px + sideX * side);
				int z = (int) Math.floor(pz + sideZ * side);
				if (!level.isLoaded(new BlockPos(x, (int) from.y, z))) {
					if ((i == 0 || i == samples) && side == 0) {
						return Double.NaN;
					}
					continue;
				}
				if (i > 0 && i < samples) {
					cruise = Math.max(cruise, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + CLEARANCE);
				}
			}
		}
		return cruise;
	}

	/** True when nothing is above the block the drone is in at {@code (x, y, z)}, so it can climb straight up from there. */
	private static boolean clearColumn(ServerLevel level, int x, int y, int z) {
		return level.isLoaded(new BlockPos(x, y, z)) && level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) <= y;
	}

	/** True when a drone can fly level from {@code (x0, z0)} to {@code (x1, z1)} at block height {@code y} (two blocks tall). */
	private static boolean clearLevel(ServerLevel level, double x0, double z0, double x1, double z1, int y) {
		double dx = x1 - x0, dz = z1 - z0;
		int steps = (int) Math.ceil(Math.sqrt(dx * dx + dz * dz) * 2);
		for (int i = 1; i <= steps; i++) {
			double t = (double) i / Math.max(1, steps);
			BlockPos at = BlockPos.containing(x0 + dx * t, y, z0 + dz * t);
			if (!level.isLoaded(at) || !level.getBlockState(at).getCollisionShape(level, at).isEmpty()
					|| !level.getBlockState(at.above()).getCollisionShape(level, at.above()).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	/** How far round a covered point the drone looks for open sky to come down through. */
	public static final int APPROACH_RANGE = 12;

	/**
	 * Where the drone climbs and comes down for a point under a roof, overhang or the tower's own floors: the
	 * nearest spot with open sky above it that it can reach by flying level at the point's height. Null when the
	 * point itself has open sky (straight up and down) or when no such spot is near ({@link #reachable} false).
	 */
	public static double @org.jspecify.annotations.Nullable [] approach(ServerLevel level, Vec3 point) {
		int y = (int) Math.floor(point.y);
		int px = (int) Math.floor(point.x), pz = (int) Math.floor(point.z);
		if (clearColumn(level, px, y, pz)) {
			return null;
		}
		for (int r = 1; r <= APPROACH_RANGE; r++) {
			double best = Double.MAX_VALUE;
			double[] found = null;
			for (int ox = -r; ox <= r; ox++) {
				for (int oz = -r; oz <= r; oz++) {
					if (Math.max(Math.abs(ox), Math.abs(oz)) != r) {
						continue;
					}
					double cx = px + ox + 0.5, cz = pz + oz + 0.5;
					double distance = ox * ox + oz * oz;
					if (distance < best && clearColumn(level, px + ox, y, pz + oz) && clearLevel(level, point.x, point.z, cx, cz, y)) {
						best = distance;
						found = new double[] {cx, cz};
					}
				}
			}
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	/** Can a drone get to {@code point} at all: open sky above it, or an {@link #approach} from the side. */
	public static boolean reachable(ServerLevel level, Vec3 point) {
		return clearColumn(level, (int) Math.floor(point.x), (int) Math.floor(point.y), (int) Math.floor(point.z))
				|| approach(level, point) != null;
	}
}
