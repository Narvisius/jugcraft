package io.github.jimbozoomer.jugcraft.lair;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Where the Moonlit Ring's parts lie (docs/features/moonlit-ring.md), relative to its template's corner: the numbers of
 * tools/moonlit_ring.py, checked against it by tools/check_mod_data.py. An instance's part is at
 * {@code Lair.MOONLIT_RING.origin(slot)} offset by these.
 */
public final class MoonlitRing {
	/** The forest floor's top layer, and where anyone on it stands. */
	public static final int SOIL = 4;
	public static final int STAND = 5;
	/** The clearing's middle (x and z, block corners), where the Alpha's leash is measured from, and its radius. */
	public static final double CENTRE_X = 37.5;
	public static final double CENTRE_Z = 36.5;
	public static final double RING = 22.0;
	/** How far out from the centre each bell clearing's middle lies, and the clearing's radius. */
	public static final double GLADE_AT = 26.0;
	public static final double GLADE_RADIUS = 3.6;
	/** Each Warden Bell, in the order of {@link Bell}. */
	public static final BlockPos[] BELLS = {new BlockPos(8, 7, 36), new BlockPos(37, 7, 7), new BlockPos(66, 7, 36)};
	/** The Hunters' Camp's open cells, inclusive: west to east (x) and north to south (z). */
	public static final int CAMP_WEST = 31;
	public static final int CAMP_EAST = 43;
	public static final int CAMP_NORTH = 61;
	public static final int CAMP_SOUTH = 70;
	/** Where the Alpha waits on the Howling Stone's top, facing north to the moon (part 2). */
	public static final double WAIT_X = 37.5;
	public static final double WAIT_Y = 8.0;
	public static final double WAIT_Z = 36.5;
	/** The Grey Mist at the end of the trail behind the camp, the way home: three columns, three high from the floor. */
	public static final int EXIT_WEST = 36;
	public static final int EXIT_EAST = 38;
	public static final int EXIT_Z = 72;
	/**
	 * The Howling Stone's steps: cells whose middle is within STONE_CORE of the centre stand three blocks high, within
	 * STONE_STEP two, within STONE_FOOT one (tools/moonlit_ring.py STONE). The Alpha bounds over it as he runs.
	 */
	public static final double STONE_CORE = 1.6;
	public static final double STONE_STEP = 2.8;
	public static final double STONE_FOOT = 3.8;
	/** Where the Grey Mist opens on the Howling Stone's top when the Alpha falls: three columns, two high. */
	public static final int STONE_EXIT_WEST = 36;
	public static final int STONE_EXIT_EAST = 38;
	public static final int STONE_EXIT_Y = 8;
	public static final int STONE_EXIT_Z = 36;

	/** The three Warden Bells, west, north and east (tools/lairs.py BELL_NAMES). */
	public enum Bell {
		WEST(-1, 0), NORTH(0, -1), EAST(1, 0);

		/** The way out from the centre to this bell's clearing. */
		public final int wayX;
		public final int wayZ;

		Bell(int wayX, int wayZ) {
			this.wayX = wayX;
			this.wayZ = wayZ;
		}

		public String title() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}

		/** The bell's name in words ("the west bell"). */
		public Component display() {
			return Component.translatable("lair.jugcraft.moonlit_ring.bell." + title());
		}

		public BlockPos at(BlockPos origin) {
			return origin.offset(BELLS[ordinal()]);
		}

		/** The middle of this bell's clearing (x, z, in the template). */
		public double gladeX() {
			return CENTRE_X + wayX * GLADE_AT;
		}

		public double gladeZ() {
			return CENTRE_Z + wayZ * GLADE_AT;
		}

		/** Whether (x, z) (in the template) is in this bell's clearing. */
		public boolean inGlade(double x, double z) {
			return Math.hypot(x - gladeX(), z - gladeZ()) <= GLADE_RADIUS;
		}
	}

	private MoonlitRing() {
	}

	/** The clearing's middle, on its floor, from an instance's origin. */
	public static Vec3 centre() {
		return new Vec3(CENTRE_X, STAND, CENTRE_Z);
	}

	/** Where the Alpha waits on the Howling Stone. */
	public static Vec3 waiting() {
		return new Vec3(WAIT_X, WAIT_Y, WAIT_Z);
	}

	/** Whether (x, z) (in the template) is in the clearing, the arena. */
	public static boolean inRing(double x, double z) {
		return Math.hypot(x - CENTRE_X, z - CENTRE_Z) <= RING;
	}

	/** How high the Howling Stone stands over the floor at (x, z) (in the template): 0 off it. */
	public static int stoneHeight(double x, double z) {
		double cellX = Math.floor(x) + 0.5;
		double cellZ = Math.floor(z) + 0.5;
		double from = Math.hypot(cellX - CENTRE_X, cellZ - CENTRE_Z);
		return from <= STONE_CORE ? 3 : from <= STONE_STEP ? 2 : from <= STONE_FOOT ? 1 : 0;
	}

	/** Whether (x, z) (in the template) is in the Hunters' Camp, where nobody is hunted. */
	public static boolean inCamp(double x, double z) {
		return x >= CAMP_WEST && x < CAMP_EAST + 1 && z >= CAMP_NORTH && z < CAMP_SOUTH + 1;
	}

	/** The bell clearing (x, z) (in the template) is in, or null. */
	public static @Nullable Bell gladeAt(double x, double z) {
		for (Bell bell : Bell.values()) {
			if (bell.inGlade(x, z)) {
				return bell;
			}
		}
		return null;
	}

	/** The bell at {@code pos} in the instance at {@code origin}, or null. */
	public static @Nullable Bell bellAt(BlockPos origin, BlockPos pos) {
		for (Bell bell : Bell.values()) {
			if (bell.at(origin).equals(pos)) {
				return bell;
			}
		}
		return null;
	}

	/** The cells of the Grey Mist that opens on the Howling Stone when the Alpha falls. */
	public static List<BlockPos> stoneExitCells() {
		List<BlockPos> out = new ArrayList<>();
		for (int y = STONE_EXIT_Y; y <= STONE_EXIT_Y + 1; y++) {
			for (int x = STONE_EXIT_WEST; x <= STONE_EXIT_EAST; x++) {
				out.add(new BlockPos(x, y, STONE_EXIT_Z));
			}
		}
		return out;
	}

	/** The cells of the Grey Mist behind the camp. */
	public static List<BlockPos> exitCells() {
		List<BlockPos> out = new ArrayList<>();
		for (int y = STAND; y <= STAND + 2; y++) {
			for (int x = EXIT_WEST; x <= EXIT_EAST; x++) {
				out.add(new BlockPos(x, y, EXIT_Z));
			}
		}
		return out;
	}
}
