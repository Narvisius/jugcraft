package io.github.jimbozoomer.jugcraft.lair.vesperine;

import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * Where things are in the Hollow Acre's template, for Vesperine's fight (tools/hollow_acre.py: ARENA_CENTRE,
 * ARENA_RADIUS, SURFACE, THRONE, WARDS, FIELD and PATH; tools/check_mod_data.py keeps them in step). Positions are in the
 * template, from an instance's origin.
 */
public final class HollowAcre {
	/** The Mown Circle's centre, x and z (block corners), and its floor: players stand at y {@value #FLOOR}. */
	public static final double ARENA_X = 32.0;
	public static final double ARENA_Z = 36.0;
	public static final int FLOOR = 17;
	public static final int ARENA_RADIUS = 20;
	/** The Bone Throne's lower half, facing south. */
	public static final BlockPos THRONE = new BlockPos(31, 17, 13);
	/** The four soul braziers on the circle's diagonals that ward it. */
	public static final List<BlockPos> WARDS = List.of(new BlockPos(17, 17, 21), new BlockPos(46, 17, 21), new BlockPos(17, 17, 50),
			new BlockPos(46, 17, 50));
	/** The black wheat's rows (z) and the path through it (x). */
	public static final int FIELD_NORTH = 57;
	public static final int FIELD_SOUTH = 66;
	public static final int PATH_WEST = 30;
	public static final int PATH_EAST = 33;

	private HollowAcre() {
	}
}
