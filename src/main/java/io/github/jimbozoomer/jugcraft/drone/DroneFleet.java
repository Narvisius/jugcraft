package io.github.jimbozoomer.jugcraft.drone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The drones linked to one depot, and the depot's pooled power numbers. No Minecraft types.
 *
 * <p>Power is pooled, as designed: the depot does not track power per drone. It caches two numbers
 * and recalculates them only when a drone is linked or unlinked:
 * <ul>
 * <li>standby draw = {@link #STANDBY_BASE} + {@link #STANDBY_PER_DRONE} per drone, while idle;</li>
 * <li>working draw = the sum of each drone's tier upkeep, while any job is active.</li>
 * </ul>
 */
public final class DroneFleet {
	public static final int STANDBY_BASE = 2;
	public static final int STANDBY_PER_DRONE = 1;

	public enum LinkResult {
		OK,
		NOT_AVAILABLE,
		FLEET_FULL,
		NO_PAD_ROOM
	}

	/** Most drones any depot can hold: a finished Drone Tower (109 in hangars plus 4 on each of the 8 ground pads). */
	public static final int MAX_FLEET = 133;

	private final List<DroneTier> drones = new ArrayList<>();
	private int slotsUsed;
	private long standbyDraw = STANDBY_BASE;
	private long workingDraw;

	/** Links a drone if the platform has room: {@code slots} is the platform's total landing slots. */
	public LinkResult link(DroneTier tier, int slots) {
		return link(tier, slots, PlatformLayout.MAX_DRONES);
	}

	/** Links a drone with a cap of {@code max} drones (a tower depot: its tower's capacity, at most {@link #MAX_FLEET}). */
	public LinkResult link(DroneTier tier, int slots, int max) {
		if (drones.size() >= Math.min(max, MAX_FLEET)) {
			return LinkResult.FLEET_FULL;
		}
		if (slotsUsed + tier.size().slots() > slots) {
			return LinkResult.NO_PAD_ROOM;
		}
		drones.add(tier);
		recalculate();
		return LinkResult.OK;
	}

	/** Unlinks the drone at {@code index} and returns it. The caller makes sure it is not flying. */
	public DroneTier remove(int index) {
		DroneTier removed = drones.remove(index);
		recalculate();
		return removed;
	}

	/** Removes every drone (the depot was broken) and returns them. */
	public List<DroneTier> clear() {
		List<DroneTier> all = List.copyOf(drones);
		drones.clear();
		recalculate();
		return all;
	}

	/** Replaces the roster when loading; drones beyond the cap are dropped from the list and returned. */
	public List<DroneTier> load(List<DroneTier> saved) {
		drones.clear();
		List<DroneTier> overflow = new ArrayList<>();
		for (DroneTier tier : saved) {
			if (drones.size() < MAX_FLEET) {
				drones.add(tier);
			} else {
				overflow.add(tier);
			}
		}
		recalculate();
		return overflow;
	}

	private void recalculate() {
		slotsUsed = 0;
		long working = 0;
		for (DroneTier tier : drones) {
			slotsUsed += tier.size().slots();
			working += tier.upkeep();
		}
		workingDraw = working;
		// Creative Drones draw nothing; a fleet of only those needs no power at all.
		long powered = drones.stream().filter(tier -> !tier.creative()).count();
		standbyDraw = powered == 0 ? 0 : STANDBY_BASE + STANDBY_PER_DRONE * powered;
	}

	public List<DroneTier> drones() {
		return Collections.unmodifiableList(drones);
	}

	public DroneTier get(int index) {
		return drones.get(index);
	}

	public int size() {
		return drones.size();
	}

	public int slotsUsed() {
		return slotsUsed;
	}

	/** Cached: JE per tick while idle. */
	public long standbyDraw() {
		return standbyDraw;
	}

	/** Cached: JE per tick while any job is active. */
	public long workingDraw() {
		return workingDraw;
	}

	/** Drone count per tier, for the status display. */
	public Map<DroneTier, Integer> countByTier() {
		Map<DroneTier, Integer> counts = new EnumMap<>(DroneTier.class);
		for (DroneTier tier : drones) {
			counts.merge(tier, 1, Integer::sum);
		}
		return counts;
	}
}
