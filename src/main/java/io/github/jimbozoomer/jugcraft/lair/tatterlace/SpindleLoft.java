package io.github.jimbozoomer.jugcraft.lair.tatterlace;

import java.util.List;
import net.minecraft.world.phys.Vec3;

/**
 * Where things are in the Spindle Loft's template, for Madame Tatterlace's fight (tools/spindle_loft.py: LACE, DOILY,
 * DOILY_RADIUS, SPOOLS, BARREL, SAFE_RING, SPOOL_TOP and lace_pattern; tools/tatterlace.py: SAC_RADIUS and SAC_ANGLES;
 * tools/check_mod_data.py keeps them in step, and checks that {@link #lace} finds the template's every lace cell).
 * Positions are in the template, from an instance's origin.
 */
public final class SpindleLoft {
	/** The doily's layer: its lace lies at the bottom of these blocks, and players stand {@value #LACE_TOP} above it. */
	public static final int LACE = 28;
	public static final double LACE_TOP = 0.125;
	/** The doily's centre, x and z (block corners), and its radius. */
	public static final double DOILY_X = 40.5;
	public static final double DOILY_Z = 36.5;
	public static final double DOILY_RADIUS = 20.5;
	/** The four spools' axles (x, z), whose barrels pierce the doily's rim: green, blue, beige, red. */
	public static final List<double[]> SPOOLS = List.of(new double[] {27.5, 23.5}, new double[] {53.5, 23.5},
			new double[] {27.5, 49.5}, new double[] {53.5, 49.5});
	/** A spool's barrel, and the ring of dense lace round it that is never unravelled. */
	public static final double BARREL = 3.5;
	public static final double SAFE_RING = 2.0;
	/** The white silk over the doily, where she waits and hangs: a square from the green spool's axle to the red's. */
	public static final int WEB = 42;
	/** The measuring tape's columns (x) and the row (z) where its foot meets the doily's rim. */
	public static final int TAPE_WEST = 39;
	public static final int TAPE_EAST = 41;
	public static final int TAPE_FOOT = 57;
	/** The doily's rings, from its centre out: each ring's outer radius and its pattern (0 band, 1 mesh, 2 flower); past
	 * the last, the edge (3). */
	private static final double[] RINGS = {2.5, 4.5, 9.5, 11.5, 15.5, 17.5, 19.5};
	private static final int[] PATTERNS = {2, 0, 1, 0, 2, 0, 1};
	/** Where Taking In the Seams spits her egg sacs: this far from the doily's centre, at these angles (degrees from east,
	 * towards south). */
	public static final double SAC_RADIUS = 17.5;
	public static final List<Integer> SAC_ANGLES = List.of(25, 70, 155, 205, 290, 335);

	private SpindleLoft() {
	}

	/** The pattern of the doily's ring {@code r} blocks from its centre. */
	public static int pattern(double r) {
		for (int i = 0; i < RINGS.length; i++) {
			if (r < RINGS[i]) {
				return PATTERNS[i];
			}
		}
		return 3;
	}

	/** How far (x, z) is from the nearest spool's axle. */
	public static double spoolDistance(double x, double z) {
		double near = Double.MAX_VALUE;
		for (double[] spool : SPOOLS) {
			near = Math.min(near, Math.hypot(x - spool[0], z - spool[1]));
		}
		return near;
	}

	/**
	 * The pattern of the lace at the template's cell (x, LACE, z), or -1 where the doily has none (past its rim, or a
	 * spool's barrel). A cell where something else lies across the lace (the thimble) also reads as lace here.
	 */
	public static int lace(int x, int z) {
		double r = Math.hypot(x + 0.5 - DOILY_X, z + 0.5 - DOILY_Z);
		if (r > DOILY_RADIUS) {
			return -1;
		}
		double near = spoolDistance(x + 0.5, z + 0.5);
		if (near <= BARREL) {
			return -1;
		}
		return near <= BARREL + SAFE_RING ? 0 : pattern(r);
	}

	/** Whether the lace at template cell (x, z) is in the dense ring round a spool, which is never unravelled. */
	public static boolean safe(int x, int z) {
		return spoolDistance(x + 0.5, z + 0.5) <= BARREL + SAFE_RING;
	}

	/** Whether template cell (x, z) is by the tape's foot, where whoever comes down the tape steps onto the lace. */
	public static boolean tapeFoot(int x, int z) {
		return x >= TAPE_WEST - 1 && x <= TAPE_EAST + 1 && z >= TAPE_FOOT - 3;
	}

	/** Where each egg sac sits, from an instance's origin: on the lace, round the doily. */
	public static List<Vec3> sacs() {
		return SAC_ANGLES.stream().map(angle -> new Vec3(DOILY_X + SAC_RADIUS * Math.cos(Math.toRadians(angle)), LACE + LACE_TOP,
				DOILY_Z + SAC_RADIUS * Math.sin(Math.toRadians(angle)))).toList();
	}
}
