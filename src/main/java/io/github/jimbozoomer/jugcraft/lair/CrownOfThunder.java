package io.github.jimbozoomer.jugcraft.lair;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Where the Crown of Thunder's parts lie (docs/features/crown-of-thunder.md), relative to its template's corner: the
 * numbers of tools/crown_of_thunder.py, checked against it by tools/check_mod_data.py. An instance's part is at
 * {@code Lair.CROWN_OF_THUNDER.origin(slot)} offset by these.
 */
public final class CrownOfThunder {
	/** The summit's top layer, and where anyone on it stands. */
	public static final int PAVE = 30;
	public static final int STAND = 31;
	/** The summit's middle (x and z, block corners), where the Roc's leash is measured from, and its radius. */
	public static final double CENTRE_X = 32.5;
	public static final double CENTRE_Z = 32.5;
	public static final double SUMMIT = 18.0;
	/** Each Storm Rod's base, in the order of {@link Rod}: each rises {@value #ROD_HEIGHT} blocks off its plinth. */
	public static final BlockPos[] RODS = {new BlockPos(32, 32, 19), new BlockPos(45, 32, 32), new BlockPos(32, 32, 45), new BlockPos(19, 32, 32)};
	public static final int ROD_HEIGHT = 3;
	/**
	 * The ledges below the summit's east and west edges: their top layer, how far out they reach from the middle, and how
	 * far either side of due east and due west (degrees). A player thrown off the summit there lands on one and climbs
	 * back by its steps.
	 */
	public static final int LEDGE_TOP = 24;
	public static final double LEDGE_OUTER = 23.0;
	public static final int LEDGE_HALF_ANGLE = 34;
	/** The terrace below the summit's south edge, where players arrive: its top layer and its cells, inclusive. */
	public static final int TERRACE_TOP = 28;
	public static final int TERRACE_WEST = 27;
	public static final int TERRACE_EAST = 38;
	public static final int TERRACE_NORTH = 51;
	public static final int TERRACE_SOUTH = 58;
	/** The nest's floor, on its crag beyond the summit's north edge. */
	public static final int NEST_TOP = 34;
	/** Where the Roc waits in his nest, facing south over the summit (part 2). */
	public static final double WAIT_X = 32.5;
	public static final double WAIT_Y = 35.0;
	public static final double WAIT_Z = 8.5;
	/** The Grey Mist in its arch on the terrace's south edge, the way home: three columns, three high from the terrace. */
	public static final int EXIT_WEST = 31;
	public static final int EXIT_EAST = 33;
	public static final int EXIT_Z = 58;

	/** The four Storm Rods, north, east, south and west of the summit's middle (tools/crown_of_thunder.py RODS). */
	public enum Rod {
		NORTH, EAST, SOUTH, WEST;

		public String title() {
			return name().toLowerCase(Locale.ROOT);
		}

		/** The rod's name in words ("the north rod"). */
		public Component display() {
			return Component.translatable("lair.jugcraft.crown_of_thunder.rod." + title());
		}

		/** The rod's base, on its plinth. */
		public BlockPos base(BlockPos origin) {
			return origin.offset(RODS[ordinal()]);
		}

		/** The rod's blocks, base to tip. */
		public List<BlockPos> parts(BlockPos origin) {
			List<BlockPos> out = new ArrayList<>();
			for (int i = 0; i < ROD_HEIGHT; i++) {
				out.add(base(origin).above(i));
			}
			return out;
		}

		/** The rod's middle (x and z, in the template), where a lane passing near it is measured to (part 2). */
		public double x() {
			return RODS[ordinal()].getX() + 0.5;
		}

		public double z() {
			return RODS[ordinal()].getZ() + 0.5;
		}
	}

	private CrownOfThunder() {
	}

	/** The summit's middle, on its floor, from an instance's origin. */
	public static Vec3 centre() {
		return new Vec3(CENTRE_X, STAND, CENTRE_Z);
	}

	/** Where the Roc waits in his nest. */
	public static Vec3 waiting() {
		return new Vec3(WAIT_X, WAIT_Y, WAIT_Z);
	}

	/** Whether (x, z) (in the template) is on the summit, the arena. */
	public static boolean onSummit(double x, double z) {
		return Math.hypot(x - CENTRE_X, z - CENTRE_Z) <= SUMMIT;
	}

	/** Whether (x, z) (in the template) is over one of the ledges. */
	public static boolean onLedge(double x, double z) {
		double from = Math.hypot(x - CENTRE_X, z - CENTRE_Z);
		if (from <= SUMMIT || from > LEDGE_OUTER) {
			return false;
		}
		// Degrees clockwise from north (low z), as tools/crown_of_thunder.py measures a bearing.
		double bearing = (Math.toDegrees(Math.atan2(x - CENTRE_X, -(z - CENTRE_Z))) + 360.0) % 360.0;
		return Math.abs(bearing - 90.0) <= LEDGE_HALF_ANGLE || Math.abs(bearing - 270.0) <= LEDGE_HALF_ANGLE;
	}

	/** Whether (x, z) (in the template) is on the terrace, below the summit. */
	public static boolean onTerrace(double x, double z) {
		return x >= TERRACE_WEST && x < TERRACE_EAST + 1 && z >= TERRACE_NORTH && z < TERRACE_SOUTH + 1 && !onSummit(x, z);
	}

	/** Charges or calms every part of {@code rod} in the instance at {@code origin}; whether any part changed. */
	public static boolean charge(Level level, BlockPos origin, Rod rod, boolean charged) {
		boolean changed = false;
		for (BlockPos pos : rod.parts(origin)) {
			changed |= StormRodBlock.setCharged(level, pos, charged);
		}
		return changed;
	}

	/** The rod charged in the instance at {@code origin} (the first, base up), or null. */
	public static @Nullable Rod charged(Level level, BlockPos origin) {
		for (Rod rod : Rod.values()) {
			for (BlockPos pos : rod.parts(origin)) {
				if (level.getBlockState(pos).getBlock() instanceof StormRodBlock && level.getBlockState(pos).getValue(StormRodBlock.CHARGED)) {
					return rod;
				}
			}
		}
		return null;
	}

	/** The cells of the Grey Mist on the terrace. */
	public static List<BlockPos> exitCells() {
		List<BlockPos> out = new ArrayList<>();
		for (int y = TERRACE_TOP + 1; y <= TERRACE_TOP + 3; y++) {
			for (int x = EXIT_WEST; x <= EXIT_EAST; x++) {
				out.add(new BlockPos(x, y, EXIT_Z));
			}
		}
		return out;
	}
}
