package io.github.jimbozoomer.jugcraft.lair.hag;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Where things are in the Lantern Fen's template, for the Mire Hag's fight (tools/lantern_fen.py: CENTRE, MUD, WATER,
 * STAND, MARSH_RADIUS, RING, LAMPS and LAMP_HEIGHT, WAIT, WOKE and MIDDLE_EXIT; tools/check_mod_data.py keeps them in
 * step). Positions are in the template, from an instance's origin.
 */
public final class LanternFen {
	/** The marsh's middle, x and z (block corners), and its radius: the arena of open water. */
	public static final double CENTRE_X = 40.0;
	public static final double CENTRE_Z = 43.0;
	public static final double MARSH_RADIUS = 22.0;
	/** The mud bed's top layer, the water's layer over it, and where anyone on a boardwalk or a bank stands. */
	public static final int MUD = 8;
	public static final int WATER = 9;
	public static final int STAND = 10;
	/** Mud is a block less two pixels high: whoever wades the marsh stands this far over the mud's layer. */
	public static final double MUD_TOP = 14.0 / 16.0;
	/** The ring of boardwalk round the marsh's middle: between these radii. */
	public static final double RING_INNER = 13.0;
	public static final double RING_OUTER = 15.0;
	/** The four fen lamps (north-west, north-east, south-west, south-east): each on its post on its platform. */
	public static final List<BlockPos> LAMPS = List.of(new BlockPos(30, 12, 33), new BlockPos(49, 12, 33), new BlockPos(30, 12, 52),
			new BlockPos(49, 12, 52));
	/** Where she waits on her porch, over her cauldron, facing south over the marsh. */
	public static final double WAIT_X = 40.5;
	public static final double WAIT_Y = 12.0;
	public static final double WAIT_Z = 17.5;
	/** Where she comes down into the water as she wakes. */
	public static final double WOKE_X = 42.5;
	public static final double WOKE_Z = 24.5;
	/** Where the Grey Mist opens when she falls: two columns, in the water's layer and over it. */
	public static final int EXIT_WEST = 39;
	public static final int EXIT_EAST = 40;
	public static final int EXIT_Z = 43;

	private LanternFen() {
	}

	/** The marsh's middle, at the water's surface where she wades, from an instance's origin. */
	public static Vec3 centre() {
		return new Vec3(CENTRE_X, MUD + MUD_TOP, CENTRE_Z);
	}

	/** Where she waits on her porch. */
	public static Vec3 waiting() {
		return new Vec3(WAIT_X, WAIT_Y, WAIT_Z);
	}

	/** Where she comes down into the water as she wakes. */
	public static Vec3 woke() {
		return new Vec3(WOKE_X, MUD + MUD_TOP, WOKE_Z);
	}

	/** How far (x, z) is from the marsh's middle. */
	public static double fromCentre(double x, double z) {
		return Math.hypot(x - CENTRE_X, z - CENTRE_Z);
	}

	/** The cells of the Grey Mist that opens when she falls. */
	public static List<BlockPos> exitCells() {
		return List.of(new BlockPos(EXIT_WEST, WATER, EXIT_Z), new BlockPos(EXIT_EAST, WATER, EXIT_Z), new BlockPos(EXIT_WEST, STAND, EXIT_Z),
				new BlockPos(EXIT_EAST, STAND, EXIT_Z));
	}
}
