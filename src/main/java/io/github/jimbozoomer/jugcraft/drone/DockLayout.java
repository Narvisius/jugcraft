package io.github.jimbozoomer.jugcraft.drone;

import java.util.ArrayList;
import java.util.List;

/**
 * Where each linked drone parks. No Minecraft types.
 *
 * <p>A pad has four quarter slots round its charger port. A small drone takes one quarter, a medium one
 * a half (two quarters on the same side) and a large one the whole pad. Drones are placed largest
 * first, first fit, which always fits when the fleet's total slots fit ({@link DroneFleet} checks
 * that), because the sizes are 4, 2 and 1.
 */
public final class DockLayout {
	/** Distance in blocks from the pad centre to the centre of a quarter slot. */
	public static final double QUARTER = 1.25;

	/** Parking spot of one drone: which pad, and the offset from that pad's centre in blocks. */
	public record Dock(int pad, double dx, double dz) {
	}

	private DockLayout() {
	}

	/** One dock per drone, in fleet order. Drones that don't fit (should not happen) park on pad 0's centre. */
	public static List<Dock> assign(List<DroneTier> fleet, int pads) {
		Dock[] docks = new Dock[fleet.size()];
		int[] used = new int[Math.max(pads, 0)]; // bit per quarter: 1 NW, 2 NE, 4 SW, 8 SE
		for (DroneSize size : new DroneSize[] {DroneSize.LARGE, DroneSize.MEDIUM, DroneSize.SMALL}) {
			for (int i = 0; i < fleet.size(); i++) {
				if (fleet.get(i).size() == size) {
					docks[i] = place(used, size);
				}
			}
		}
		List<Dock> result = new ArrayList<>(docks.length);
		for (Dock dock : docks) {
			result.add(dock == null ? new Dock(0, 0, 0) : dock);
		}
		return result;
	}

	/**
	 * Takes a free slot of {@code size} on one of the pads ({@code used} holds a bit per quarter per pad, as in
	 * {@link #assign}) and returns it, or null when no pad has room. Used for a tower depot's ground pads.
	 */
	public static @org.jspecify.annotations.Nullable Dock place(int[] used, DroneSize size) {
		for (int pad = 0; pad < used.length; pad++) {
			switch (size) {
				case LARGE -> {
					if (used[pad] == 0) {
						used[pad] = 15;
						return new Dock(pad, 0, 0);
					}
				}
				case MEDIUM -> {
					if ((used[pad] & 3) == 0) {
						used[pad] |= 3;
						return new Dock(pad, 0, -QUARTER);
					}
					if ((used[pad] & 12) == 0) {
						used[pad] |= 12;
						return new Dock(pad, 0, QUARTER);
					}
				}
				case SMALL -> {
					for (int q = 0; q < 4; q++) {
						if ((used[pad] & (1 << q)) == 0) {
							used[pad] |= 1 << q;
							return new Dock(pad, q % 2 == 0 ? -QUARTER : QUARTER, q < 2 ? -QUARTER : QUARTER);
						}
					}
				}
			}
		}
		return null;
	}
}
