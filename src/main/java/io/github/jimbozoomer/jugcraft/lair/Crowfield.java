package io.github.jimbozoomer.jugcraft.lair;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Where the Crowfield's parts lie (docs/features/crowfield.md), relative to its template's corner: the numbers of
 * tools/crowfield.py, checked against it by tools/check_mod_data.py. An instance's part is at
 * {@code Lair.CROWFIELD.origin(slot)} offset by these.
 */
public final class Crowfield {
	/** The floor's top layer, and where anyone on it stands. */
	public static final int SOIL = 4;
	public static final int STAND = 5;
	/** The field's middle (x and z, block corners), where the King's leash is measured from. */
	public static final double CENTRE_X = 34.5;
	public static final double CENTRE_Z = 32.5;
	/** The field's cells, inclusive: west to east (x) and north to south (z). */
	public static final int FIELD_WEST = 12;
	public static final int FIELD_EAST = 56;
	public static final int FIELD_NORTH = 10;
	public static final int FIELD_SOUTH = 54;
	/** How far apart the King's hills are, in their three-by-three grid round his post. */
	public static final int HILL_SPACING = 13;
	/** Each King's Gourd, in the order of {@link Hill} (ring order). */
	public static final BlockPos[] HILLS = {new BlockPos(21, 5, 19), new BlockPos(34, 5, 19), new BlockPos(47, 5, 19), new BlockPos(47, 5, 32),
			new BlockPos(47, 5, 45), new BlockPos(34, 5, 45), new BlockPos(21, 5, 45), new BlockPos(21, 5, 32)};
	/** The farmyard's open cells, inclusive, and the fence between it and the field with the farm gate in it. */
	public static final int YARD_WEST = 26;
	public static final int YARD_EAST = 42;
	public static final int YARD_NORTH = 56;
	public static final int YARD_SOUTH = 64;
	public static final int FENCE_Z = 55;
	public static final int GATE_WEST = 33;
	public static final int GATE_EAST = 35;
	/** Where the Pumpkin King hangs on his post, facing south to the farmyard (part 2). */
	public static final double WAIT_X = 34.5;
	public static final double WAIT_Y = 6.0;
	public static final double WAIT_Z = 33.5;
	/** The Grey Mist at the end of the lane behind the farmyard, the way home: three columns, three high from the floor. */
	public static final int EXIT_WEST = 33;
	public static final int EXIT_EAST = 35;
	public static final int EXIT_Z = 68;
	/** Where the Grey Mist opens at the post when the King falls: three columns, two high, south of the mound. */
	public static final int POST_EXIT_WEST = 33;
	public static final int POST_EXIT_EAST = 35;
	public static final int POST_EXIT_Y = 5;
	public static final int POST_EXIT_Z = 35;

	/** The King's hills, in ring order: clockwise from the north-west, seen from above (tools/lairs.py HILL_NAMES). */
	public enum Hill {
		NORTH_WEST(-1, -1), NORTH(0, -1), NORTH_EAST(1, -1), EAST(1, 0), SOUTH_EAST(1, 1), SOUTH(0, 1), SOUTH_WEST(-1, 1), WEST(-1, 0);

		/** The hill's place in the grid round the post. */
		public final int gridX;
		public final int gridZ;

		Hill(int gridX, int gridZ) {
			this.gridX = gridX;
			this.gridZ = gridZ;
		}

		public String title() {
			return name().toLowerCase(Locale.ROOT);
		}

		/** The hill's name in words ("the north-west hill"). */
		public Component display() {
			return Component.translatable("lair.jugcraft.crowfield.hill." + title());
		}

		/** Where its gourd grows, in the instance at {@code origin}. */
		public BlockPos at(BlockPos origin) {
			return origin.offset(HILLS[ordinal()]);
		}

		/** The next hill round the ring (clockwise), and the one before. */
		public Hill next() {
			return values()[(ordinal() + 1) % values().length];
		}

		public Hill previous() {
			return values()[(ordinal() + values().length - 1) % values().length];
		}
	}

	private Crowfield() {
	}

	/** The field's middle, on its floor, from an instance's origin. */
	public static Vec3 centre() {
		return new Vec3(CENTRE_X, STAND, CENTRE_Z);
	}

	/** Where the King hangs on his post. */
	public static Vec3 waiting() {
		return new Vec3(WAIT_X, WAIT_Y, WAIT_Z);
	}

	/** Whether (x, z) (in the template) is in the field, the arena. */
	public static boolean inField(double x, double z) {
		return x >= FIELD_WEST && x < FIELD_EAST + 1 && z >= FIELD_NORTH && z < FIELD_SOUTH + 1;
	}

	/** Whether (x, z) (in the template) is in the farmyard, behind the fence, where nobody is fought. */
	public static boolean inYard(double x, double z) {
		return x >= YARD_WEST && x < YARD_EAST + 1 && z >= YARD_NORTH && z < YARD_SOUTH + 1;
	}

	/** The hill whose gourd is at {@code pos} in the instance at {@code origin}, or null. */
	public static @Nullable Hill hillAt(BlockPos origin, BlockPos pos) {
		for (Hill hill : Hill.values()) {
			if (hill.at(origin).equals(pos)) {
				return hill;
			}
		}
		return null;
	}

	/** The hill nearest (x, z) (in the template). */
	public static Hill nearestHill(double x, double z) {
		Hill best = Hill.NORTH;
		double nearest = Double.MAX_VALUE;
		for (Hill hill : Hill.values()) {
			BlockPos at = HILLS[hill.ordinal()];
			double d = Math.hypot(x - (at.getX() + 0.5), z - (at.getZ() + 0.5));
			if (d < nearest) {
				nearest = d;
				best = hill;
			}
		}
		return best;
	}

	/** The cells of the Grey Mist that opens at the post when the King falls. */
	public static List<BlockPos> postExitCells() {
		List<BlockPos> out = new ArrayList<>();
		for (int y = POST_EXIT_Y; y <= POST_EXIT_Y + 1; y++) {
			for (int x = POST_EXIT_WEST; x <= POST_EXIT_EAST; x++) {
				out.add(new BlockPos(x, y, POST_EXIT_Z));
			}
		}
		return out;
	}

	/** The cells of the Grey Mist at the end of the lane. */
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
