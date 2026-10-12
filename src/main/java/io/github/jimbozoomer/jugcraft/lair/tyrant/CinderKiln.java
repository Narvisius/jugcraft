package io.github.jimbozoomer.jugcraft.lair.tyrant;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Where things are in the Cinder Kiln's template, for the Cinder Tyrant's fight (tools/cinder_kiln.py: BOWL, BOWL_CENTRE,
 * BOWL_RADIUS, CRUCIBLE, RUN, CHANNEL, LIP, SHELVES, SHELF_HALF and the sluices' wheels, from sluice_blocks;
 * tools/check_mod_data.py keeps them in step). Positions are in the template, from an instance's origin.
 */
public final class CinderKiln {
	/** The bowl's layer: its cracked basalt, troughs, channel and the crucible's top are these blocks, stood on a block up. */
	public static final int BOWL = 5;
	/** The bowl's centre, x and z (block corners), and its radius: the arena of cracked basalt. */
	public static final double BOWL_X = 35.5;
	public static final double BOWL_Z = 37.5;
	public static final double BOWL_RADIUS = 22.0;
	/** The crucible: its centre (x, z), the pool's radius, and how deep its slag is. */
	public static final double CRUCIBLE_X = 35.5;
	public static final double CRUCIBLE_Z = 25.5;
	public static final double CRUCIBLE_RADIUS = 4.5;
	public static final int CRUCIBLE_DEPTH = 3;
	/** The heat channel: its columns (x) and rows (z), from the slag fall to the crucible. */
	public static final int RUN_WEST = 34;
	public static final int RUN_EAST = 36;
	public static final int CHANNEL_NORTH = 14;
	public static final int CHANNEL_SOUTH = 21;
	/** The forge's lip: its top layer, stood on a block up, and the row he stands on it in the Eruption. */
	public static final int LIP = 8;
	public static final double LIP_Z = 10.5;
	/** The four shelves (x, z of each one's middle) and their half-size: a rounded square a block high. */
	public static final List<double[]> SHELVES = List.of(new double[] {24.5, 26.5}, new double[] {46.5, 26.5},
			new double[] {24.5, 48.5}, new double[] {46.5, 48.5});
	public static final double SHELF_HALF = 3.2;
	/** The sluices' wheels, west, east and south: any block of a gate finds the rest of it (SluiceGateBlock#gate). */
	public static final List<BlockPos> WHEELS = List.of(new BlockPos(7, 7, 35), new BlockPos(63, 7, 39), new BlockPos(33, 7, 65));
	/** Where he waits sunk in the crucible (his feet), facing south: only his crest shows over the slag. */
	public static final double SUNK_Y = 3.5;
	public static final double SUNK_Z = 26.5;
	/** Where he crawls out to over the crucible's rim as he wakes, and lands coming down from the Eruption. */
	public static final double OUT_Z = 33.5;

	private CinderKiln() {
	}

	/** How far (x, z) is from the bowl's centre. */
	public static double fromCentre(double x, double z) {
		return Math.hypot(x - BOWL_X, z - BOWL_Z);
	}

	/** Whether block column (x, z) is in the bowl, the arena of cracked basalt (as tools/cinder_kiln.py in_bowl). */
	public static boolean inBowl(int x, int z) {
		return fromCentre(x + 0.5, z + 0.5) <= BOWL_RADIUS;
	}

	/** Whether block column (x, z) is in the crucible's pool. */
	public static boolean inCrucible(int x, int z) {
		return Math.hypot(x + 0.5 - CRUCIBLE_X, z + 0.5 - CRUCIBLE_Z) <= CRUCIBLE_RADIUS;
	}

	/** Whether block column (x, z) is the heat channel. */
	public static boolean inChannel(int x, int z) {
		return RUN_WEST <= x && x <= RUN_EAST && CHANNEL_NORTH <= z && z <= CHANNEL_SOUTH;
	}

	/** Whether (x, z) is over a shelf: a rounded square round its middle (as tools/cinder_kiln.py shelf). */
	public static boolean onShelf(double x, double z) {
		for (double[] shelf : SHELVES) {
			double dx = (x - shelf[0]) / SHELF_HALF;
			double dz = (z - shelf[1]) / SHELF_HALF;
			if (dx * dx * dx * dx + dz * dz * dz * dz <= 1.0) {
				return true;
			}
		}
		return false;
	}

	/** The bowl's centre, where players stand on it, from an instance's origin. */
	public static Vec3 centre() {
		return new Vec3(BOWL_X, BOWL + 1, BOWL_Z);
	}

	/** Where he waits sunk in the crucible. */
	public static Vec3 sunk() {
		return new Vec3(CRUCIBLE_X, SUNK_Y, SUNK_Z);
	}

	/** Where he crawls out to as he wakes, south of the crucible's rim, and comes down to from the Eruption. */
	public static Vec3 out() {
		return new Vec3(CRUCIBLE_X, BOWL + 1, OUT_Z);
	}

	/** Where he stands on the forge's lip in the Eruption, over the slag running across it. */
	public static Vec3 lip() {
		return new Vec3(CRUCIBLE_X, LIP + 1, LIP_Z);
	}

	/** Where a Cinderling crawls out of the Eruption's surge: on the channel's west bank or its east, halfway along. */
	public static Vec3 bank(boolean west) {
		return new Vec3(west ? RUN_WEST - 1.5 : RUN_EAST + 2.5, BOWL + 1, (CHANNEL_NORTH + CHANNEL_SOUTH) / 2.0);
	}
}
