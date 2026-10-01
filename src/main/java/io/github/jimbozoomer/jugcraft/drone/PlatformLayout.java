package io.github.jimbozoomer.jugcraft.drone;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The shape of a drone depot's landing platform, worked out from the blocks on one level. No
 * Minecraft types, so it can be tested on its own.
 *
 * <p>A platform is a connected area (sides touching) of base blocks. Plates placed on the base mark
 * cells as {@link Cell#PAD} (landing pad plates) or {@link Cell#PICKUP} (supply pickup plates). A group
 * of plates touching each other forms only if it is exactly one square of one kind: 5x5 for a landing
 * pad, 3x3 for a supply pickup. So pads must be separated by at least one bare base block. Each pad
 * holds {@link DroneSize#SLOTS_PER_PAD} slots. The smallest platform is 9x9 with one pad in the middle;
 * players extend it in any direction and add pads, up to {@link #MAX_CELLS} blocks, which is enough
 * room for {@link #MAX_DRONES} drones of any size (100 small drones: a 33x33 base with a 5x5 grid of pads).
 */
public final class PlatformLayout {
	public static final int PAD_SIZE = 5;
	public static final int PICKUP_SIZE = 3;
	public static final int MIN_SIDE = 9;
	/** Hard cap on drones per depot. Small drones need 25 pads for this; large ones need 100. */
	public static final int MAX_DRONES = 100;
	/** Largest platform scanned (64 x 64). The scan stops here, so a huge field of blocks costs nothing extra. */
	public static final int MAX_CELLS = 64 * 64;

	public enum Cell {
		NONE,
		PLATFORM,
		PAD,
		PICKUP
	}

	/** Looks up what is at a column (x, z) on the platform's level. */
	@FunctionalInterface
	public interface CellLookup {
		Cell at(int x, int z);
	}

	public enum Status {
		OK,
		NO_PLATFORM,
		TOO_SMALL,
		NO_PAD,
		TOO_LARGE
	}

	private final Status status;
	private final Set<Long> cells;
	private final List<long[]> pads;
	private final List<long[]> pickups;
	private final int minX;
	private final int minZ;
	private final int maxX;
	private final int maxZ;

	private PlatformLayout(Status status, Set<Long> cells, List<long[]> pads, List<long[]> pickups, int minX, int minZ, int maxX, int maxZ) {
		this.status = status;
		this.cells = cells;
		this.pads = pads;
		this.pickups = pickups;
		this.minX = minX;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxZ = maxZ;
	}

	public static final PlatformLayout EMPTY = new PlatformLayout(Status.NO_PLATFORM, Set.of(), List.of(), List.of(), 0, 0, -1, -1);

	/**
	 * Flood-fills from {@code (seedX, seedZ)} through platform and pad blocks, then groups pad blocks
	 * into whole 5x5 pads. Reads at most {@link #MAX_CELLS} cells plus their edges.
	 */
	public static PlatformLayout scan(int seedX, int seedZ, CellLookup lookup) {
		if (lookup.at(seedX, seedZ) == Cell.NONE) {
			return EMPTY;
		}
		Map<Long, Cell> found = new HashMap<>();
		ArrayDeque<long[]> queue = new ArrayDeque<>();
		found.put(key(seedX, seedZ), lookup.at(seedX, seedZ));
		queue.add(new long[] {seedX, seedZ});
		boolean truncated = false;
		int minX = seedX, maxX = seedX, minZ = seedZ, maxZ = seedZ;
		while (!queue.isEmpty()) {
			long[] cell = queue.poll();
			int x = (int) cell[0], z = (int) cell[1];
			minX = Math.min(minX, x);
			maxX = Math.max(maxX, x);
			minZ = Math.min(minZ, z);
			maxZ = Math.max(maxZ, z);
			int[][] neighbors = {{x + 1, z}, {x - 1, z}, {x, z + 1}, {x, z - 1}};
			for (int[] n : neighbors) {
				long k = key(n[0], n[1]);
				if (found.containsKey(k)) {
					continue;
				}
				Cell type = lookup.at(n[0], n[1]);
				if (type == Cell.NONE) {
					continue;
				}
				if (found.size() >= MAX_CELLS) {
					truncated = true;
					continue;
				}
				found.put(k, type);
				queue.add(new long[] {n[0], n[1]});
			}
		}

		List<long[]> pads = new ArrayList<>();
		List<long[]> pickups = new ArrayList<>();
		findSquares(found, pads, pickups);
		Set<Long> cells = Collections.unmodifiableSet(new HashSet<>(found.keySet()));
		Status status;
		if (truncated) {
			status = Status.TOO_LARGE;
		} else if (maxX - minX + 1 < MIN_SIDE || maxZ - minZ + 1 < MIN_SIDE) {
			status = Status.TOO_SMALL;
		} else if (pads.isEmpty()) {
			status = Status.NO_PAD;
		} else {
			status = Status.OK;
		}
		return new PlatformLayout(status, cells, List.copyOf(pads), List.copyOf(pickups), minX, minZ, maxX, maxZ);
	}

	/**
	 * Groups touching plates (pad and pickup plates together) and keeps each group that is exactly one
	 * square of one kind: 5x5 pad plates or 3x3 pickup plates. Anything else (a group that is too small,
	 * too big, not square, mixed, or two pads touching) forms nothing. Results are sorted by corner (z,
	 * then x), so they never depend on hash order.
	 */
	private static void findSquares(Map<Long, Cell> found, List<long[]> pads, List<long[]> pickups) {
		Set<Long> seen = new HashSet<>();
		List<Long> keys = new ArrayList<>(found.keySet());
		keys.sort((a, b) -> z(a) != z(b) ? Integer.compare(z(a), z(b)) : Integer.compare(x(a), x(b)));
		for (long start : keys) {
			Cell kind = found.get(start);
			if ((kind != Cell.PAD && kind != Cell.PICKUP) || !seen.add(start)) {
				continue;
			}
			// Flood through touching plates of either kind.
			List<Long> group = new ArrayList<>();
			boolean mixed = false;
			ArrayDeque<Long> queue = new ArrayDeque<>();
			queue.add(start);
			int gx0 = Integer.MAX_VALUE, gz0 = Integer.MAX_VALUE, gx1 = Integer.MIN_VALUE, gz1 = Integer.MIN_VALUE;
			while (!queue.isEmpty()) {
				long k = queue.poll();
				group.add(k);
				mixed |= found.get(k) != kind;
				int x = x(k), z = z(k);
				gx0 = Math.min(gx0, x);
				gz0 = Math.min(gz0, z);
				gx1 = Math.max(gx1, x);
				gz1 = Math.max(gz1, z);
				for (long n : new long[] {key(x + 1, z), key(x - 1, z), key(x, z + 1), key(x, z - 1)}) {
					Cell type = found.get(n);
					if ((type == Cell.PAD || type == Cell.PICKUP) && seen.add(n)) {
						queue.add(n);
					}
				}
			}
			int size = kind == Cell.PAD ? PAD_SIZE : PICKUP_SIZE;
			if (!mixed && group.size() == size * size && gx1 - gx0 + 1 == size && gz1 - gz0 + 1 == size) {
				(kind == Cell.PAD ? pads : pickups).add(new long[] {gx0, gz0});
			}
		}
	}

	/** Whole 3x3 supply pickups, as their north-west corners {x, z}. */
	public List<long[]> pickups() {
		return pickups;
	}

	public Status status() {
		return status;
	}

	public boolean isValid() {
		return status == Status.OK;
	}

	/** Whole 5x5 pads, as their north-west corners {x, z}. */
	public List<long[]> pads() {
		return pads;
	}

	public int padCount() {
		return pads.size();
	}

	/** Landing slots: 4 per pad. A small drone uses 1, medium 2, large 4. */
	public int slots() {
		return isValid() ? pads.size() * DroneSize.SLOTS_PER_PAD : 0;
	}

	/** Every platform cell as a packed key; unpack with {@link #keyX} and {@link #keyZ}. */
	public Set<Long> cellKeys() {
		return cells;
	}

	public static int keyX(long key) {
		return x(key);
	}

	public static int keyZ(long key) {
		return z(key);
	}

	public int cellCount() {
		return cells.size();
	}

	public boolean contains(int x, int z) {
		return cells.contains(key(x, z));
	}

	public int minX() {
		return minX;
	}

	public int minZ() {
		return minZ;
	}

	public int width() {
		return maxX - minX + 1;
	}

	public int depth() {
		return maxZ - minZ + 1;
	}

	/** Centre of the platform, used as the drones' home point. */
	public double centerX() {
		return (minX + maxX + 1) / 2.0;
	}

	public double centerZ() {
		return (minZ + maxZ + 1) / 2.0;
	}

	/** Pads needed for {@code drones} drones of one size, capped by {@link #MAX_DRONES}. */
	public static int padsNeeded(int drones, DroneSize size) {
		int slots = Math.min(drones, MAX_DRONES) * size.slots();
		return (slots + DroneSize.SLOTS_PER_PAD - 1) / DroneSize.SLOTS_PER_PAD;
	}

	static long key(int x, int z) {
		return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
	}

	static int x(long key) {
		return (int) (key >> 32);
	}

	static int z(long key) {
		return (int) key;
	}
}
