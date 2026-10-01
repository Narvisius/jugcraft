package io.github.jimbozoomer.jugcraft.drone;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * What a drone depot looks like from outside: the platform, pads, supply pickup, where each drone
 * docks, every flight's path and progress, power and jobs. The server sends it to players near the
 * terminal (in the block entity's update packet) when something changes; every client then draws the
 * same drones, pickup animation, wall screen and terminal screen from it, and moves the drones along
 * their paths by itself between updates. So all players see what the owner sees, and the server only
 * sends a small snapshot when a flight starts, a drone lands or the depot changes.
 */
public final class DepotView {
	/** One flight as the client needs it: which drone, its path, and where along it the drone is. */
	public record FlightView(int drone, int tier, FlightPath path, long progress, long total, long rate) {
		/** Ticks into the flight at game time {@code now} (fractional), predicted from the last update. */
		public double ticksAt(double now, long syncTime) {
			double units = progress + rate * Math.max(0, now - syncTime);
			return Math.min(total, units) / FlightScheduler.FULL_RATE;
		}
	}

	/** Where a docked drone rests: its tier and position (relative to the terminal block). */
	public record DockView(int tier, double x, double y, double z) {
	}

	public long syncTime;
	public String mode = "personal";
	public int status = PlatformLayout.Status.NO_PLATFORM.ordinal();
	public int minX;
	public int minZ;
	public int width;
	public int depth;
	public int platformY;
	public final List<int[]> pads = new ArrayList<>();
	public int[] pickup;
	/** Every supply pickup in use: {x, plate level, z} of its north-west plate (absolute). */
	public final List<int[]> pickups = new ArrayList<>();
	/** The Drone Tower's tier (0: no tower), its core (absolute, or null), the fleet limit and the highest drone tier. */
	public int towerTier;
	public net.minecraft.core.@org.jspecify.annotations.Nullable BlockPos towerPos;
	public int maxDrones = PlatformLayout.MAX_DRONES;
	public int maxTier = 1;
	public final List<DockView> docks = new ArrayList<>();
	public final List<FlightView> flights = new ArrayList<>();
	/**
	 * Tower hangar bay doors: {x0, z0, x1, z1, bottom y, height, out x, out z} (absolute; the opening's end
	 * cells and which way is outside), then the hangar's dock x and z relative to the terminal, times two.
	 */
	public final List<int[]> doors = new ArrayList<>();
	public int slotsUsed;
	public int slots;
	public long energy;
	public long capacity;
	public long workingDraw;
	public long standbyDraw;
	public boolean lowPower;
	public boolean packager;
	public int openTargets;
	public int covered;
	public final Map<String, Integer> missing = new LinkedHashMap<>();

	public PlatformLayout.Status layoutStatus() {
		PlatformLayout.Status[] values = PlatformLayout.Status.values();
		return status >= 0 && status < values.length ? values[status] : PlatformLayout.Status.NO_PLATFORM;
	}

	public int flying() {
		return flights.size();
	}

	public void write(ValueOutput out) {
		out.putLong("time", syncTime);
		out.putString("mode", mode);
		out.putIntArray("layout", new int[] {status, width, depth, platformY, slotsUsed, slots, openTargets, covered, minX, minZ});
		int[] padCorners = new int[pads.size() * 2];
		for (int i = 0; i < pads.size(); i++) {
			padCorners[i * 2] = pads.get(i)[0];
			padCorners[i * 2 + 1] = pads.get(i)[1];
		}
		out.putIntArray("pads", padCorners);
		if (pickup != null) {
			out.putIntArray("pickup", pickup);
		}
		int[] pickupData = new int[pickups.size() * 3];
		for (int i = 0; i < pickups.size(); i++) {
			System.arraycopy(pickups.get(i), 0, pickupData, i * 3, 3);
		}
		out.putIntArray("pickups", pickupData);
		out.putInt("tower", towerTier);
		if (towerPos != null) {
			out.putLong("tower_pos", towerPos.asLong());
		}
		out.putIntArray("limits", new int[] {maxDrones, maxTier});
		int[] dockData = new int[docks.size() * 4];
		for (int i = 0; i < docks.size(); i++) {
			DockView dock = docks.get(i);
			dockData[i * 4] = dock.tier();
			dockData[i * 4 + 1] = Float.floatToIntBits((float) dock.x());
			dockData[i * 4 + 2] = Float.floatToIntBits((float) dock.y());
			dockData[i * 4 + 3] = Float.floatToIntBits((float) dock.z());
		}
		out.putIntArray("docks", dockData);
		int[] doorData = new int[doors.size() * 10];
		for (int i = 0; i < doors.size(); i++) {
			System.arraycopy(doors.get(i), 0, doorData, i * 10, 10);
		}
		out.putIntArray("doors", doorData);
		out.putLong("energy", energy);
		out.putLong("capacity", capacity);
		out.putLong("working", workingDraw);
		out.putLong("standby", standbyDraw);
		out.putBoolean("low_power", lowPower);
		out.putBoolean("packager", packager);
		ValueOutput.ValueOutputList missingList = out.childrenList("missing");
		missing.forEach((id, count) -> {
			ValueOutput entry = missingList.addChild();
			entry.putString("id", id);
			entry.putInt("count", count);
		});
		ValueOutput.ValueOutputList flightList = out.childrenList("flights");
		for (FlightView flight : flights) {
			ValueOutput entry = flightList.addChild();
			entry.putIntArray("head", new int[] {flight.drone(), flight.tier(), flight.path().speed()});
			entry.putLong("progress", flight.progress());
			entry.putLong("total", flight.total());
			entry.putLong("rate", flight.rate());
			entry.putIntArray("path", encode(flight.path()));
		}
	}

	public static DepotView read(ValueInput in) {
		DepotView view = new DepotView();
		view.syncTime = in.getLongOr("time", 0L);
		view.mode = in.getStringOr("mode", "personal");
		int[] layout = in.getIntArray("layout").orElse(new int[8]);
		if (layout.length >= 8) {
			view.status = layout[0];
			view.width = layout[1];
			view.depth = layout[2];
			view.platformY = layout[3];
			view.slotsUsed = layout[4];
			view.slots = layout[5];
			view.openTargets = layout[6];
			view.covered = layout[7];
		}
		if (layout.length >= 10) {
			view.minX = layout[8];
			view.minZ = layout[9];
		}
		int[] padCorners = in.getIntArray("pads").orElse(new int[0]);
		for (int i = 0; i + 1 < padCorners.length; i += 2) {
			view.pads.add(new int[] {padCorners[i], padCorners[i + 1]});
		}
		view.pickup = in.getIntArray("pickup").filter(a -> a.length == 2).orElse(null);
		int[] pickupData = in.getIntArray("pickups").orElse(new int[0]);
		for (int i = 0; i + 2 < pickupData.length; i += 3) {
			view.pickups.add(new int[] {pickupData[i], pickupData[i + 1], pickupData[i + 2]});
		}
		view.towerTier = in.getIntOr("tower", 0);
		view.towerPos = in.getLong("tower_pos").map(net.minecraft.core.BlockPos::of).orElse(null);
		int[] limits = in.getIntArray("limits").orElse(new int[0]);
		if (limits.length >= 2) {
			view.maxDrones = limits[0];
			view.maxTier = limits[1];
		}
		int[] dockData = in.getIntArray("docks").orElse(new int[0]);
		for (int i = 0; i + 3 < dockData.length; i += 4) {
			view.docks.add(new DockView(dockData[i], Float.intBitsToFloat(dockData[i + 1]), Float.intBitsToFloat(dockData[i + 2]),
					Float.intBitsToFloat(dockData[i + 3])));
		}
		int[] doorData = in.getIntArray("doors").orElse(new int[0]);
		for (int i = 0; i + 9 < doorData.length; i += 10) {
			view.doors.add(java.util.Arrays.copyOfRange(doorData, i, i + 10));
		}
		view.energy = in.getLongOr("energy", 0L);
		view.capacity = in.getLongOr("capacity", 1L);
		view.workingDraw = in.getLongOr("working", 0L);
		view.standbyDraw = in.getLongOr("standby", 0L);
		view.lowPower = in.getBooleanOr("low_power", false);
		view.packager = in.getBooleanOr("packager", false);
		for (ValueInput entry : in.childrenListOrEmpty("missing")) {
			view.missing.put(entry.getStringOr("id", "?"), entry.getIntOr("count", 0));
		}
		for (ValueInput entry : in.childrenListOrEmpty("flights")) {
			int[] head = entry.getIntArray("head").orElse(new int[0]);
			FlightPath path = decode(entry.getIntArray("path").orElse(new int[0]), head.length >= 3 ? head[2] : 0);
			if (head.length >= 3 && path != null) {
				view.flights.add(new FlightView(head[0], head[1], path, entry.getLongOr("progress", 0L), entry.getLongOr("total", 1L),
						entry.getLongOr("rate", FlightScheduler.FULL_RATE)));
			}
		}
		return view;
	}

	/**
	 * Path as ints: point count, then x, y, z per point and one cruise height per leg (float bits), then each
	 * point's approach {x, z} (NaN where there is none).
	 */
	public static int[] encode(FlightPath path) {
		int n = path.points();
		int base = 1 + n * 3 + (n - 1);
		int[] data = new int[base + n * 2];
		data[0] = n;
		for (int i = 0; i < n; i++) {
			data[1 + i * 3] = Float.floatToIntBits((float) path.x(i));
			data[2 + i * 3] = Float.floatToIntBits((float) path.y(i));
			data[3 + i * 3] = Float.floatToIntBits((float) path.z(i));
		}
		for (int i = 0; i < n - 1; i++) {
			data[1 + n * 3 + i] = Float.floatToIntBits((float) path.cruise(i));
		}
		for (int i = 0; i < n; i++) {
			double[] a = path.approach(i);
			data[base + i * 2] = Float.floatToIntBits(a == null ? Float.NaN : (float) a[0]);
			data[base + i * 2 + 1] = Float.floatToIntBits(a == null ? Float.NaN : (float) a[1]);
		}
		return data;
	}

	/** Reverses {@link #encode} (also reads the older forms with only the hangar exits); null if malformed. */
	public static FlightPath decode(int[] data, int speed) {
		if (data.length < 1 || speed <= 0) {
			return null;
		}
		int n = data[0];
		int base = 1 + n * 3 + (n - 1);
		if (n < 3 || n > 1024 || (data.length != base && data.length != base + 4 && data.length != base + n * 2)) {
			return null;
		}
		double[][] points = new double[n][3];
		for (int i = 0; i < n; i++) {
			points[i][0] = Float.intBitsToFloat(data[1 + i * 3]);
			points[i][1] = Float.intBitsToFloat(data[2 + i * 3]);
			points[i][2] = Float.intBitsToFloat(data[3 + i * 3]);
		}
		double[] cruise = new double[n - 1];
		for (int i = 0; i < n - 1; i++) {
			cruise[i] = Float.intBitsToFloat(data[1 + n * 3 + i]);
		}
		double[][] approach = new double[n][];
		if (data.length == base + n * 2 && n * 2 != 4) {
			for (int i = 0; i < n; i++) {
				float ax = Float.intBitsToFloat(data[base + i * 2]), az = Float.intBitsToFloat(data[base + i * 2 + 1]);
				approach[i] = Float.isNaN(ax) ? null : new double[] {ax, az};
			}
		} else if (data.length == base + 4) {
			float ox = Float.intBitsToFloat(data[base]), oz = Float.intBitsToFloat(data[base + 1]);
			float ix = Float.intBitsToFloat(data[base + 2]), iz = Float.intBitsToFloat(data[base + 3]);
			approach[0] = Float.isNaN(ox) ? null : new double[] {ox, oz};
			approach[n - 1] = Float.isNaN(ix) ? null : new double[] {ix, iz};
		}
		return new FlightPath(points, cruise, speed, approach);
	}
}
