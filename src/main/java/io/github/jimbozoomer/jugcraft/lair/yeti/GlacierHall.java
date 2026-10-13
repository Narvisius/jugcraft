package io.github.jimbozoomer.jugcraft.lair.yeti;

import java.util.List;
import net.minecraft.world.phys.Vec3;

/**
 * Where things are in the Glacier Hall's template, for the Yeti King's fight (tools/glacier_hall.py: LAKE, LAKE_CENTRE,
 * LAKE_RADIUS, COLUMNS, COLUMN_RADIUS, COLUMN_FOOT, STEP_TOP, THRONE and DENS; tools/check_mod_data.py keeps them in step).
 * Positions are in the template, from an instance's origin.
 */
public final class GlacierHall {
	/** The lake's layer: its drift snow, glare ice and trampled snow are these blocks, and players stand a block above. */
	public static final int LAKE = 4;
	/** The lake's centre, x and z (block corners), and its radius. */
	public static final double LAKE_X = 39.5;
	public static final double LAKE_Z = 36.5;
	public static final double LAKE_RADIUS = 20.5;
	/** The four ice columns rising from the lake (x, z), their radius at the lake and how much wider their foot is. */
	public static final List<double[]> COLUMNS = List.of(new double[] {26.5, 23.5}, new double[] {52.5, 23.5},
			new double[] {26.5, 49.5}, new double[] {52.5, 49.5});
	public static final double COLUMN_RADIUS = 2.6;
	public static final double COLUMN_FOOT = 1.4;
	/** Where the dais's top step is stood on, and the throne's seat on it: its middle (x, z) and the seat's top. */
	public static final int STEP_TOP = 8;
	public static final double THRONE_X = 39.5;
	public static final double THRONE_Z = 7.0;
	public static final int SEAT_TOP = 9;
	/** Where he roars from in the King's Roar: the top step's front, before his throne. */
	public static final double ROAR_Z = 10.5;
	/** The dens' floors in the west and east walls (x, z), where his kin come out. */
	public static final List<double[]> DENS = List.of(new double[] {5.5, 36.5}, new double[] {73.5, 36.5});

	private GlacierHall() {
	}

	/** How far (x, z) is from the lake's centre. */
	public static double fromCentre(double x, double z) {
		return Math.hypot(x - LAKE_X, z - LAKE_Z);
	}

	/** How far (x, z) is from the nearest ice column's centre. */
	public static double columnDistance(double x, double z) {
		double near = Double.MAX_VALUE;
		for (double[] column : COLUMNS) {
			near = Math.min(near, Math.hypot(x - column[0], z - column[1]));
		}
		return near;
	}

	/** The lake's centre, where players stand on it, from an instance's origin. */
	public static Vec3 centre() {
		return new Vec3(LAKE_X, LAKE + 1, LAKE_Z);
	}

	/** Where he waits on his throne, from an instance's origin. */
	public static Vec3 throne() {
		return new Vec3(THRONE_X, SEAT_TOP, THRONE_Z);
	}

	/** Where he lands when he wakes: on the lake below his dais. */
	public static Vec3 below() {
		return new Vec3(LAKE_X, LAKE + 1, LAKE_Z - LAKE_RADIUS + 4.0);
	}

	/** Where he roars from in the King's Roar. */
	public static Vec3 roar() {
		return new Vec3(THRONE_X, STEP_TOP, ROAR_Z);
	}

	/** Where each den's kin come out, from an instance's origin. */
	public static List<Vec3> dens() {
		return DENS.stream().map(den -> new Vec3(den[0], LAKE + 1, den[1])).toList();
	}
}
