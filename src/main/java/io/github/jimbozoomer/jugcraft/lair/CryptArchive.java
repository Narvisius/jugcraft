package io.github.jimbozoomer.jugcraft.lair;

import io.github.jimbozoomer.jugcraft.lair.NameStoneBlock.Rune;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Where the Archive of Last Names' parts lie (docs/features/crypt-archive.md), relative to its template's corner: the
 * numbers of tools/crypt_archive.py, checked against it by tools/check_mod_data.py. An instance's part is at
 * {@code Lair.CRYPT_ARCHIVE.origin(slot)} offset by these.
 */
public final class CryptArchive {
	/** Where anyone in the hall stands, in the throne's apse and on the landing. */
	public static final int STAND = 5;
	public static final int APSE_STAND = 7;
	public static final int LANDING_STAND = 11;
	/** The hall's open cells, inclusive: west to east (x) and north to south (z). */
	public static final int HALL_WEST = 12;
	public static final int HALL_EAST = 52;
	public static final int HALL_NORTH = 14;
	public static final int HALL_SOUTH = 54;
	public static final BlockPos LECTERN = new BlockPos(32, 5, 53);
	public static final BlockPos PHYLACTERY = new BlockPos(32, 6, 34);
	public static final BlockPos THRONE = new BlockPos(32, 7, 9);
	/** Each name's grave seal, in the order of {@link Rune}: the Sexton's (spade), the Crowned's, the Scribe's. */
	public static final BlockPos[] SEALS = {new BlockPos(5, 6, 34), new BlockPos(32, 8, 5), new BlockPos(59, 6, 34)};
	/** Each name's name-stone, over its seal, in the same order. */
	public static final BlockPos[] STONES = {new BlockPos(5, 7, 34), new BlockPos(32, 9, 5), new BlockPos(59, 7, 34)};

	/** The dais's middle (x and z, block corners): where the Crypt Lich's leash is measured from. */
	public static final double CENTRE_X = 32.5;
	public static final double CENTRE_Z = 34.5;
	/** Where the Crypt Lich waits on his throne, facing south down the hall, and where he comes down to the floor as he wakes. */
	public static final double WAIT_X = 32.5;
	public static final double WAIT_Y = 7.0;
	public static final double WAIT_Z = 9.5;
	public static final double WOKE_X = 32.5;
	public static final double WOKE_Z = 16.5;
	/**
	 * The lanes the Lich calls by name: west to the Sexton's alcove, north to the Crowned's, east to the Scribe's (in the
	 * order of {@link Rune}), each its open cells inclusive (x0, x1, z0, z1). The south lane, to the lectern, he never calls.
	 */
	public static final int[][] LANES = {{12, 28, 33, 35}, {31, 33, 14, 30}, {36, 52, 33, 35}};
	/** The ledger stones in the floor where his dead rise: each one's north cell (x, z); each is two cells long (z). */
	public static final int[][] GRAVES = {{16, 17}, {25, 17}, {16, 24}, {25, 24}, {39, 17}, {48, 17}, {39, 24}, {48, 24}, {16, 42}, {25, 43},
			{16, 49}, {25, 49}, {39, 43}, {48, 42}, {39, 49}, {48, 49}};
	/** Where a Restless Dead climbs out of each name's sarcophagus, at its foot (x, z), in the order of {@link Rune}. */
	public static final double[][] SARCOPHAGUS_DEAD = {{8.5, 34.5}, {30.5, 7.5}, {56.5, 34.5}};
	/** Where the Grey Mist opens on the dais when the Lich falls: three columns, two high, where the phylactery stood. */
	public static final int EXIT_WEST = 31;
	public static final int EXIT_EAST = 33;
	public static final int EXIT_Z = 34;

	private CryptArchive() {
	}

	/** The dais's middle, on the hall's floor, from an instance's origin. */
	public static Vec3 centre() {
		return new Vec3(CENTRE_X, STAND, CENTRE_Z);
	}

	/** Where the Lich waits on his throne. */
	public static Vec3 waiting() {
		return new Vec3(WAIT_X, WAIT_Y, WAIT_Z);
	}

	/** Where the Lich comes down to the hall's floor as he wakes. */
	public static Vec3 woke() {
		return new Vec3(WOKE_X, STAND, WOKE_Z);
	}

	/** Where a dead climbs out of the sarcophagus of {@code rune}'s alcove (the Crowned's is on the raised apse). */
	public static Vec3 sarcophagusDead(Rune rune) {
		double[] at = SARCOPHAGUS_DEAD[rune.ordinal()];
		return new Vec3(at[0], rune == Rune.CROWN ? APSE_STAND : STAND, at[1]);
	}

	/** Where a dead rises from the ledger stone {@code grave}: the middle of its two cells. */
	public static Vec3 grave(int grave) {
		return new Vec3(GRAVES[grave][0] + 0.5, STAND, GRAVES[grave][1] + 1.0);
	}

	/** Whether (x, z) (in the template) lies on the lane to {@code rune}'s alcove, {@code margin} blocks wider each side. */
	public static boolean onLane(Rune rune, double x, double z, double margin) {
		int[] lane = LANES[rune.ordinal()];
		return x >= lane[0] - margin && x <= lane[1] + 1 + margin && z >= lane[2] - margin && z <= lane[3] + 1 + margin;
	}

	/** How far (x, z) is from the lane to {@code rune}'s alcove (0 on it). */
	public static double fromLane(Rune rune, double x, double z) {
		int[] lane = LANES[rune.ordinal()];
		double dx = Math.max(0.0, Math.max(lane[0] - x, x - (lane[1] + 1)));
		double dz = Math.max(0.0, Math.max(lane[2] - z, z - (lane[3] + 1)));
		return Math.hypot(dx, dz);
	}

	/** The middle line of the lane to {@code rune}'s alcove, from the dais's end to the alcove's: its two ends (x, z). */
	public static double[] laneLine(Rune rune) {
		int[] lane = LANES[rune.ordinal()];
		boolean alongX = lane[1] - lane[0] > lane[3] - lane[2];
		double midX = (lane[0] + lane[1] + 1) / 2.0;
		double midZ = (lane[2] + lane[3] + 1) / 2.0;
		return alongX ? new double[] {lane[0] + 0.5, midZ, lane[1] + 0.5, midZ} : new double[] {midX, lane[2] + 0.5, midX, lane[3] + 0.5};
	}

	/** The cells of the Grey Mist that opens on the dais when the Lich falls. */
	public static List<BlockPos> exitCells() {
		List<BlockPos> out = new ArrayList<>();
		for (int y = STAND + 1; y <= STAND + 2; y++) {
			for (int x = EXIT_WEST; x <= EXIT_EAST; x++) {
				out.add(new BlockPos(x, y, EXIT_Z));
			}
		}
		return out;
	}

	public static BlockPos seal(BlockPos origin, Rune rune) {
		return origin.offset(SEALS[rune.ordinal()]);
	}

	public static BlockPos stone(BlockPos origin, Rune rune) {
		return origin.offset(STONES[rune.ordinal()]);
	}

	/** The name-stone of {@code rune}, found from where the lectern stands. */
	public static BlockPos stoneFrom(BlockPos lectern, Rune rune) {
		return stone(lectern.subtract(LECTERN), rune);
	}
}
