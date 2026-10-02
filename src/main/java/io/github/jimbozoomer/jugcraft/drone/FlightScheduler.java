package io.github.jimbozoomer.jugcraft.drone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Timed drone flights. No Minecraft types: {@code T} is whatever a stop carries (the depot uses a
 * block position and the item to deliver).
 *
 * <p>A flight is not a mob. It is a record of progress along a planned route: take off, fly to the
 * cargo packager, visit each stop, fly home, land and recharge on the pad's charger port. Each tick every active
 * flight advances by {@code rate} progress units ({@link #FULL_RATE} at full power), so the server's
 * cost is one comparison per flight per tick, however many drones fly. Low power lowers the rate:
 * flights slow down but never drop cargo.
 *
 * <p>Departures are limited to {@link #MAX_LAUNCHES_PER_TICK} per tick so a 100-drone fleet ramps
 * up over a second instead of planning 100 routes in one tick.
 */
public final class FlightScheduler<T> {
	/** Progress units per tick at full power. One tick of flying at full power = 1000 units. */
	public static final long FULL_RATE = 1000;
	public static final int MAX_LAUNCHES_PER_TICK = 5;
	/** Ticks spent collecting the crate at the packager. */
	public static final int PICKUP_TICKS = 10;
	/** Ticks hovering over each stop while the material drops. */
	public static final int DROP_TICKS = 8;
	/** Ticks charging on the pad's charger port after landing (powered by the depot, so it slows on low power too). */
	public static final int RECHARGE_TICKS = 40;

	/** Receives flight events on the server thread. */
	public interface Listener<T> {
		/** The drone is over stop {@code index}; fill the blueprint position now. */
		void onDeliver(Flight<T> flight, int index);

		/** The drone is back on its pad and free for another job. */
		void onLanded(Flight<T> flight);
	}

	/** One stop: what is delivered there. */
	public record Stop<T>(T cargo) {
	}

	/** One active flight. Progress and timings are in progress units (ticks x {@link #FULL_RATE}). */
	public static final class Flight<T> {
		private int drone;
		private final List<Stop<T>> stops;
		private final long[] arriveAt;
		private final boolean[] delivered;
		private final long total;
		private long progress;

		Flight(int drone, List<Stop<T>> stops, long[] arriveAt, boolean[] delivered, long total, long progress) {
			this.drone = drone;
			this.stops = stops;
			this.arriveAt = arriveAt;
			this.delivered = delivered;
			this.total = total;
			this.progress = progress;
		}

		public int drone() {
			return drone;
		}

		public List<Stop<T>> stops() {
			return stops;
		}

		public long arriveAt(int index) {
			return arriveAt[index];
		}

		public boolean delivered(int index) {
			return delivered[index];
		}

		public long total() {
			return total;
		}

		public long progress() {
			return progress;
		}

		/** Fraction of the whole trip done, 0 to 1 (for display). */
		public double fraction() {
			return total == 0 ? 1 : Math.min(1.0, (double) progress / total);
		}
	}

	private final Map<Integer, Flight<T>> flights = new TreeMap<>();
	private int launchesThisTick;

	/** Call once at the start of each tick before launching. */
	public void startTick() {
		launchesThisTick = 0;
	}

	public boolean canLaunch() {
		return launchesThisTick < MAX_LAUNCHES_PER_TICK;
	}

	public boolean isFlying(int drone) {
		return flights.containsKey(drone);
	}

	public int activeFlights() {
		return flights.size();
	}

	public Collection<Flight<T>> flights() {
		return Collections.unmodifiableCollection(flights.values());
	}

	/**
	 * Starts a flight. {@code legBlocks} are the route lengths in blocks: pad to packager, packager to
	 * stop 1, stop 1 to stop 2 ..., last stop back to the pad (so {@code stops.size() + 2} entries).
	 *
	 * @return the flight, or null if the drone is already flying, the launch limit was reached or the
	 *         legs don't match the stops
	 */
	public Flight<T> launch(int drone, int speedBlocksPerSecond, double[] legBlocks, List<Stop<T>> stops) {
		if (flights.containsKey(drone) || !canLaunch() || stops.isEmpty() || legBlocks.length != stops.size() + 2
				|| speedBlocksPerSecond <= 0) {
			return null;
		}
		long time = legTicks(legBlocks[0], speedBlocksPerSecond) + PICKUP_TICKS;
		long[] arriveAt = new long[stops.size()];
		for (int i = 0; i < stops.size(); i++) {
			time += legTicks(legBlocks[i + 1], speedBlocksPerSecond);
			arriveAt[i] = time * FULL_RATE;
			time += DROP_TICKS;
		}
		time += legTicks(legBlocks[stops.size() + 1], speedBlocksPerSecond) + RECHARGE_TICKS;
		Flight<T> flight = new Flight<>(drone, List.copyOf(stops), arriveAt, new boolean[stops.size()], time * FULL_RATE, 0);
		flights.put(drone, flight);
		launchesThisTick++;
		return flight;
	}

	/** Ticks to fly {@code blocks} at {@code speed} blocks per second (20 ticks per second), at least 1. */
	public static long legTicks(double blocks, int speed) {
		return Math.max(1, (long) Math.ceil(blocks * 20.0 / speed));
	}

	/**
	 * Advances every flight by {@code rate} units (0 to {@link #FULL_RATE}) and reports deliveries and
	 * landings. Deliveries are always reported before the landing of the same flight.
	 */
	public void tick(long rate, Listener<T> listener) {
		long step = Math.max(0, Math.min(FULL_RATE, rate));
		if (step == 0) {
			return;
		}
		List<Flight<T>> landed = new ArrayList<>();
		for (Flight<T> flight : flights.values()) {
			flight.progress = Math.min(flight.total, flight.progress + step);
			for (int i = 0; i < flight.stops.size(); i++) {
				if (!flight.delivered[i] && flight.progress >= flight.arriveAt[i]) {
					flight.delivered[i] = true;
					listener.onDeliver(flight, i);
				}
			}
			if (flight.progress >= flight.total) {
				landed.add(flight);
			}
		}
		for (Flight<T> flight : landed) {
			flights.remove(flight.drone);
			listener.onLanded(flight);
		}
	}

	/** Ends every flight at once (depot broken or feature switched off), delivering nothing. */
	public List<Flight<T>> abortAll() {
		List<Flight<T>> all = new ArrayList<>(flights.values());
		flights.clear();
		return all;
	}

	/** Drone {@code removed} was unlinked: renumber the flights of drones after it. */
	public void droneRemoved(int removed) {
		if (flights.containsKey(removed)) {
			throw new IllegalStateException("Drone " + removed + " is still flying");
		}
		List<Flight<T>> moved = new ArrayList<>();
		for (Iterator<Map.Entry<Integer, Flight<T>>> it = flights.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Integer, Flight<T>> entry = it.next();
			if (entry.getKey() > removed) {
				moved.add(entry.getValue());
				it.remove();
			}
		}
		for (Flight<T> flight : moved) {
			flight.drone--;
			flights.put(flight.drone, flight);
		}
	}

	/** Restores a saved flight. Invalid data (overlapping drone, bad timings) is rejected. */
	public boolean restore(int drone, List<Stop<T>> stops, long[] arriveAt, boolean[] delivered, long total, long progress) {
		if (flights.containsKey(drone) || stops.isEmpty() || arriveAt.length != stops.size()
				|| delivered.length != stops.size() || total <= 0 || progress < 0 || progress > total) {
			return false;
		}
		flights.put(drone, new Flight<>(drone, List.copyOf(stops), arriveAt.clone(), delivered.clone(), total, progress));
		return true;
	}
}
