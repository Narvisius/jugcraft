package io.github.jimbozoomer.jugcraft.warehouse;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * The drones' traffic control: the lane cells (where a drone's middle can be: over a lane tile, the space clear) and
 * who has booked which cell when. A drone books every cell it will be in for the ticks it is there before it moves
 * (driving: the cell it leaves and the one it enters, for the whole step), so two drones never meet; one standing
 * still books its cell till it moves again. A path is found once per move, in space and time (it may wait for a
 * cell to come free), with a bounded search.
 */
public final class Router {
	/** Ticks to drive one cell, to turn a quarter, and the shortest wait the search tries. */
	public static final int STEP = 8, TURN = 8, WAIT = 4;
	/** The search's limits: states looked at, and how far ahead (ticks). */
	private static final int MAX_STATES = 8000, HORIZON = 2400;
	public static final Direction[] HEADINGS = {Direction.SOUTH, Direction.EAST, Direction.NORTH, Direction.WEST};

	private LongSet cells = new LongOpenHashSet();
	/** Cells only their own drone may enter (its dock). */
	private final Long2ObjectOpenHashMap<Integer> owned = new Long2ObjectOpenHashMap<>();
	private final Long2ObjectOpenHashMap<List<Booking>> bookings = new Long2ObjectOpenHashMap<>();

	/** A cell taken by a drone from one tick to another (to Long.MAX_VALUE: till it moves). */
	public record Booking(long from, long to, int drone) {
	}

	/** One step of a path: the cell, the heading the drone has there, the tick it gets there. */
	public record Step(long cell, int heading, long time) {
	}

	public void setCells(LongSet cells, java.util.Map<Long, Integer> docks) {
		this.cells = cells;
		owned.clear();
		docks.forEach(owned::put);
	}

	public boolean isCell(long cell) {
		return cells.contains(cell);
	}

	public LongSet cells() {
		return cells;
	}

	public static int heading(Direction d) {
		return switch (d) {
			case EAST -> 1;
			case NORTH -> 2;
			case WEST -> 3;
			default -> 0;
		};
	}

	public static float yaw(int heading) {
		return switch (heading & 3) {
			case 1 -> 90;
			case 2 -> 180;
			case 3 -> -90;
			default -> 0;
		};
	}

	// ------------------------------------------------------------------ bookings

	public void book(long cell, long from, long to, int drone) {
		bookings.computeIfAbsent(cell, c -> new ArrayList<>()).add(new Booking(from, to, drone));
	}

	/** Lets go of everything a drone has booked (before it books its next move). */
	public void release(int drone) {
		Iterator<Long2ObjectOpenHashMap.Entry<List<Booking>>> it = bookings.long2ObjectEntrySet().fastIterator();
		while (it.hasNext()) {
			List<Booking> list = it.next().getValue();
			list.removeIf(b -> b.drone == drone);
			if (list.isEmpty()) {
				it.remove();
			}
		}
	}

	/** Forgets bookings that ended before now. */
	public void prune(long now) {
		Iterator<Long2ObjectOpenHashMap.Entry<List<Booking>>> it = bookings.long2ObjectEntrySet().fastIterator();
		while (it.hasNext()) {
			List<Booking> list = it.next().getValue();
			list.removeIf(b -> b.to < now);
			if (list.isEmpty()) {
				it.remove();
			}
		}
	}

	/** Whether a cell is free for a drone from one tick to another (its own bookings don't count). */
	public boolean free(long cell, long from, long to, int drone) {
		Integer owner = owned.get(cell);
		if (owner != null && owner != drone) {
			return false;
		}
		List<Booking> list = bookings.get(cell);
		if (list == null) {
			return true;
		}
		for (Booking b : list) {
			if (b.drone != drone && b.from <= to && from <= b.to) {
				return false;
			}
		}
		return true;
	}

	// ------------------------------------------------------------------ paths

	private record Node(long cell, int heading, long time, int cost, int estimate, Node parent) implements Comparable<Node> {
		@Override
		public int compareTo(Node o) {
			return Integer.compare(cost + estimate, o.cost + o.estimate);
		}
	}

	/**
	 * A path for a drone from a cell (facing heading, from tick start) to a goal cell, arriving facing goalHeading (or any:
	 * -1), and able to stay there (and in the extra cells) for stay ticks once there. The steps, the first its start; null
	 * when there is none within the search's limits.
	 */
	public List<Step> path(int drone, long start, int heading, long startTime, long goal, int goalHeading, int stay, long... extra) {
		if (!cells.contains(goal) && !owned.containsKey(goal)) {
			return null;
		}
		BlockPos g = BlockPos.of(goal);
		PriorityQueue<Node> open = new PriorityQueue<>();
		Set<Long> closed = new HashSet<>();
		open.add(new Node(start, heading, startTime, 0, estimate(start, g), null));
		int looked = 0;
		while (!open.isEmpty() && looked++ < MAX_STATES) {
			Node n = open.poll();
			long key = (n.cell * 4 + n.heading) * 4096 + (n.time - startTime) / WAIT;
			if (!closed.add(key)) {
				continue;
			}
			if (n.cell == goal) {
				int turn = goalHeading < 0 ? 0 : turns(n.heading, goalHeading) * TURN;
				long done = n.time + turn;
				if (free(goal, n.time, done + stay, drone) && extraFree(extra, done, done + stay, drone)) {
					List<Step> out = new ArrayList<>();
					if (turn > 0) {
						out.add(new Step(goal, goalHeading, done));
					}
					for (Node m = n; m != null; m = m.parent) {
						out.add(new Step(m.cell, m.heading, m.time));
					}
					Collections.reverse(out);
					return out;
				}
			}
			if (n.time - startTime > HORIZON) {
				continue;
			}
			// Wait where it is.
			if (free(n.cell, n.time, n.time + WAIT, drone)) {
				open.add(new Node(n.cell, n.heading, n.time + WAIT, n.cost + WAIT, n.estimate, n));
			}
			BlockPos p = BlockPos.of(n.cell);
			for (int h = 0; h < 4; h++) {
				long next = p.relative(HEADINGS[h]).asLong();
				if (!cells.contains(next) && !(next == goal && owned.containsKey(goal))) {
					continue;
				}
				int turn = turns(n.heading, h) * TURN;
				long leave = n.time + turn, arrive = leave + STEP;
				// Turning where it is, then the step: both cells for the step's length.
				if (!free(n.cell, n.time, arrive, drone) || !free(next, leave, arrive, drone)) {
					continue;
				}
				// A turn is a separate step (it stands at its cell, facing the new way).
				Node turned = turn > 0 ? new Node(n.cell, h, leave, n.cost + turn, n.estimate, n) : n;
				open.add(new Node(next, h, arrive, n.cost + turn + STEP, estimate(next, g), turned));
			}
		}
		return null;
	}

	private boolean extraFree(long[] extra, long from, long to, int drone) {
		for (long e : extra) {
			if (!free(e, from, to, drone)) {
				return false;
			}
		}
		return true;
	}

	/** Books a path's cells: each step's cell from when it gets there till it leaves (both cells while it drives). */
	public void bookPath(int drone, List<Step> path) {
		for (int i = 0; i < path.size(); i++) {
			Step s = path.get(i);
			long until = i + 1 < path.size() ? path.get(i + 1).time() : s.time();
			long from = i > 0 && path.get(i - 1).cell() != s.cell() ? path.get(i - 1).time() : s.time();
			book(s.cell(), from, until, drone);
		}
	}

	public static int turns(int from, int to) {
		int d = Math.floorMod(to - from, 4);
		return d == 3 ? 1 : d;
	}

	private static int estimate(long cell, BlockPos goal) {
		BlockPos p = BlockPos.of(cell);
		return (Math.abs(p.getX() - goal.getX()) + Math.abs(p.getZ() - goal.getZ())) * STEP;
	}
}
