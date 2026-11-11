package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.drone.DepotView;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneTier;
import io.github.jimbozoomer.jugcraft.drone.FlightPath;
import io.github.jimbozoomer.jugcraft.drone.FlightScheduler;
import io.github.jimbozoomer.jugcraft.drone.HoloTableBlock;
import io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.PlantDoorBlock;
import io.github.jimbozoomer.jugcraft.nuclear.ServerRoom;
import io.github.jimbozoomer.jugcraft.nuclear.SpeedGateBlock;
import io.github.jimbozoomer.jugcraft.tower.AisleContainmentDoorBlock;
import io.github.jimbozoomer.jugcraft.tower.DronePartsShelvingBlock;
import io.github.jimbozoomer.jugcraft.tower.HallFixtureBlock;
import io.github.jimbozoomer.jugcraft.tower.HoloCommandTableBlock;
import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.TowerData;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jspecify.annotations.Nullable;

/**
 * What the Console Desks' pages show (Tri-Pillar work list item 14), worked out once a frame for each tower terminal the
 * desks are pinned to: the depot's view as the terminal last sent it (its fleet, flights, hangar bays, power, supply,
 * jobs), the tower core's tiers and build, the sky, and the hall's own live pieces, found once in the tower's layout
 * ({@link Site}): the service arm, the parts wall's battery station, the data hall's UPS and racks, the lobby's speed
 * gates and doors, the map table. What the depot does not keep (traffic over the last hour, the power trace, the event
 * log) each terminal's desks record for themselves while someone is near enough to see them ({@link Recorder}).
 */
final class DeskData {
	/** One flight as the pages show it. state: PICKUP, LOADING, OUTBOUND, WORKING, RETURN, CHARGE. */
	record Flight(int id, int tier, String state, double x, double z, double dist, String bearing, double workX, double workZ, double workDist,
			String workBearing, int eta, double progress, String home, boolean airborne, double elapsed) {
		boolean returning() {
			return state.equals("RETURN") || state.equals("CHARGE");
		}
	}

	/** A hangar bay: its pillar (0 to 2, or -1), number, status (0 empty, 1 drone home, 2 drone out), how open its door is. */
	record Bay(int pillar, int number, int status, double open) {
		String name() {
			return (pillar >= 0 && pillar < 3 ? "ABC".charAt(pillar) + "" : "H") + number;
		}
	}

	record Item(String id, String name, int count) {
	}

	record Event(long tick, String time, String text, int colour) {
	}

	/** The data hall's UPS (the room's hub) as its own screens read it: its power bits, battery, the room's air, the agent. */
	record Hall(int bits, double battery, double temp, double psi, int phase, int flags, long since, long releasedAt) {
	}

	/** A rack in the data hall: its number (as its own label shows it), what it is (SRV, NET, SAN, PPC), its row (0, 1). */
	record Rack(String label, String kind, int row) {
	}

	/** A speed gate's lane (west to east): open, and whether it is the way in. */
	record Gate(boolean open, boolean in) {
	}

	record Door(String name, boolean open) {
	}

	static final String[] BEARINGS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
	static final String[] MOON = {"FULL MOON", "WANING GIBBOUS", "LAST QUARTER", "WANING CRESCENT", "NEW MOON", "WAXING CRESCENT",
			"FIRST QUARTER", "WAXING GIBBOUS"};

	final Level level;
	final BlockPos link;
	final @Nullable DepotView view;
	final @Nullable TowerCoreBlockEntity core;
	final boolean coreLinked;
	final double now;
	final long gameTime;
	final String clock;
	final long dayNumber, dayTime;
	final int tier, building;
	final float progress;
	final String mode;
	int fleet, max, air, active, docked, maxTier = 1;
	final int[] home = new int[11], out = new int[11], cap = new int[11];
	final List<Flight> flights = new ArrayList<>();
	final List<Bay> bays = new ArrayList<>();
	int roofPads, roofDocked;
	/** The roof's open pads (as the terminal counts them: docks with no bay door, within a pad's reach of each other share
	 * one): {x, z} from the tower's middle and the drones on it. */
	final List<double[]> pads = new ArrayList<>();
	long energy, capacity, draw, work, standby, load;
	boolean lowPower, packager;
	DepotView.@Nullable CargoView cargo;
	final List<int[]> pickups = new ArrayList<>();
	int targets, covered;
	final List<Item> missing = new ArrayList<>(), stock = new ArrayList<>();
	/** The layout's tiers (1 to 9): names, the drones each holds, the docks each adds by drone tier. */
	final String[] tierNames = new String[TowerData.TIERS];
	final int[] tierCaps = new int[TowerData.TIERS];
	final @Nullable TowerData layout;
	/** The middle of the tower (the core, or the terminal for a depot without one): flights are placed from it. */
	final double cx, cz;
	final boolean raining, thundering;
	final float rain;
	final int moon;
	final @Nullable Site site;
	final Recorder rec;
	/** The overworld's clock (ticks) the page's times are worked out from. */
	final long clockTicks;
	/** The hall's live pieces, read for the pages (DeskPages reads nothing else of the world). */
	@Nullable Hall hall;
	final List<Rack> racks = new ArrayList<>();
	int cylinders;
	/** The service arm's clock (seconds it has run: client/ServiceArm) and whether someone holds it, or null; its drone's tier. */
	@Nullable Double armTime;
	boolean armHeld;
	int standTier = 4;
	/** The parts wall's battery station's clock (client/DronePartsShelvingRenderer), or null. */
	@Nullable Double packSeconds;
	final List<Gate> gates = new ArrayList<>();
	final List<Door> doors = new ArrayList<>();
	boolean readers, holoTable;
	/** The Big Map Table's views (SITE, LAND, RADIUS: tower/MapScan VIEWS), the one it follows now and its radius. */
	final double[] mapViews = io.github.jimbozoomer.jugcraft.tower.MapScan.VIEWS.clone();
	int mapView;
	double mapRadius;
	/** Where the ground floor's zones are (x, z from the tower's middle), or null. */
	double @Nullable [] zoneHall, zoneOpsWest, zoneOpsEast, zoneBay, zoneLobby, zoneMap;
	/** Players on the ground floor (x, z from the tower's middle). */
	final List<double[]> people = new ArrayList<>();
	/** The tier being built: its blocks, the docks it adds and their highest drone tier; the first tier with hangar bays. */
	int buildTotal, newDocks, newDockTier;
	String hangarTier = "THE NEXT TIERS";
	/** Items in the core's store; where the packager is ("X Y Z"), or null. */
	long stored;
	@Nullable String packagerAt;

	private static final Map<Level, Map<BlockPos, DeskData>> FRAME = new WeakHashMap<>();
	private static final Map<Level, Map<BlockPos, Recorder>> RECORDERS = new WeakHashMap<>();
	private static final Map<Level, Map<Long, Site>> SITES = new WeakHashMap<>();

	/** The data for desks pinned to the terminal at {@code link}, now (worked out once per frame). */
	static DeskData get(Level level, BlockPos link, double now) {
		Map<BlockPos, DeskData> frame = FRAME.computeIfAbsent(level, k -> new HashMap<>());
		DeskData d = frame.get(link);
		if (d == null || d.now != now) {
			d = new DeskData(level, link, now);
			frame.put(link.immutable(), d);
		}
		return d;
	}

	private DeskData(Level level, BlockPos link, double now) {
		this.level = level;
		this.link = link.immutable();
		this.now = now;
		this.gameTime = (long) now;
		DepotView v = level.getBlockEntity(link) instanceof DroneTerminalBlockEntity terminal ? terminal.view() : null;
		this.view = v;
		TowerCoreBlockEntity c = v != null && v.towerPos != null && level.getBlockEntity(v.towerPos) instanceof TowerCoreBlockEntity t ? t : null;
		this.core = c;
		this.coreLinked = c != null;
		long day = level.getOverworldClockTime();
		this.clockTicks = day;
		long t = Math.floorMod(day + 6000, 24000);
		this.dayTime = Math.floorMod(day, 24000);
		this.clock = String.format(Locale.ROOT, "%02d:%02d", t / 1000, t % 1000 * 60 / 1000);
		this.dayNumber = Math.floorDiv(day, 24000) + 1;
		this.raining = level.isRaining();
		this.thundering = level.isThundering();
		this.rain = level.getRainLevel(1);
		this.moon = io.github.jimbozoomer.jugcraft.agriculture.GrandfatherClockBlock.moonPhase(day);
		this.tier = c != null ? c.tier() : v != null ? v.towerTier : 0;
		this.building = c != null ? c.building() : 0;
		this.progress = c != null ? c.buildProgress() : 0;
		this.mode = v == null ? "" : v.mode.toUpperCase(Locale.ROOT);
		this.layout = c != null ? c.layout() : null;
		if (layout != null) {
			for (int i = 1; i <= TowerData.TIERS && i <= layout.tiers.size(); i++) {
				TowerData.Tier step = layout.tier(i);
				tierNames[i - 1] = step.name.toUpperCase(Locale.ROOT);
				tierCaps[i - 1] = step.capacity;
				if (i <= tier) {
					for (TowerData.Dock dock : step.docks) {
						if (dock.tier() >= 1 && dock.tier() < cap.length) {
							cap[dock.tier()]++;
						}
					}
				}
			}
		}
		this.cx = c != null ? c.getBlockPos().getX() + 0.5 : link.getX() + 0.5;
		this.cz = c != null ? c.getBlockPos().getZ() + 0.5 : link.getZ() + 0.5;
		this.site = c != null && layout != null ? Site.of(level, c.getBlockPos(), c.layoutVersion(), layout) : null;
		if (v != null) {
			fill(v);
			if (layout != null && c != null) {
				pads(v, c);
			}
		}
		Map<BlockPos, Recorder> recorders = RECORDERS.computeIfAbsent(level, k -> new HashMap<>());
		this.rec = recorders.computeIfAbsent(this.link, k -> new Recorder());
		read();
		rec.update(this);
	}

	/** Reads the hall's live pieces and the tower's build for the pages. */
	private void read() {
		if (core != null) {
			for (int v : core.store().values()) {
				stored += v;
			}
		}
		if (cargo != null && cargo.packagerPos() != null) {
			BlockPos at = cargo.packagerPos();
			packagerAt = at.getX() + " " + at.getY() + " " + at.getZ();
		}
		if (layout != null) {
			if (building >= 1 && building <= layout.tiers.size()) {
				TowerData.Tier step = layout.tier(building);
				buildTotal = step.place.length;
				for (TowerData.Dock dock : step.docks) {
					newDocks++;
					newDockTier = Math.max(newDockTier, dock.tier());
				}
			}
			search:
			for (int i = 1; i <= layout.tiers.size(); i++) {
				for (TowerData.Dock dock : layout.tier(i).docks) {
					if (dock.door() != null) {
						hangarTier = "TIER " + i + " " + layout.tier(i).name.toUpperCase(Locale.ROOT);
						break search;
					}
				}
			}
		}
		people.addAll(players());
		// the map table's view, as client/HoloMapRenderer picks it: the smallest that holds the farthest drone and its stops
		double far = 0;
		for (Flight f : flights) {
			far = Math.max(far, Math.max(f.dist(), f.workDist()));
		}
		mapView = mapViews.length - 1;
		for (int i = 0; i < mapViews.length; i++) {
			if (far * 1.15 <= mapViews[i]) {
				mapView = i;
				break;
			}
		}
		mapRadius = mapViews[mapView];
		if (site == null) {
			return;
		}
		holoTable = site.holoTable != null;
		LivePanelBlockEntity h = hub();
		if (h != null) {
			// (The data hall only looks live, nuclear/ServerRoom.java: its power and air worked out here.)
			hall = new Hall(ServerRoom.bits(h, now), 1, ServerRoom.temp(h, now), h.srvPsi, h.srvPhase, h.srvFlags, h.srvSince, h.srvReleasedAt);
		}
		cylinders = site.cylinders.size();
		int row = -1, lastZ = Integer.MIN_VALUE;
		for (int i = 0; i < site.racks.size(); i++) {
			BlockPos offset = site.racks.get(i);
			if (offset.getZ() != lastZ) {
				row++;
				lastZ = offset.getZ();
			}
			BlockPos at = at(offset);
			String label = at != null && level.getBlockEntity(at) instanceof LivePanelBlockEntity rack && !rack.srvLabel.isEmpty() ? rack.srvLabel
					: "IT-" + two(i + 1);
			racks.add(new Rack(label, site.rackKinds.get(i), row));
		}
		BlockPos arm = at(site.arm);
		if (arm != null && level.isLoaded(arm) && level.getBlockEntity(arm) != null) {
			armTime = ServiceArm.clock(level.getBlockEntity(arm), level, arm, now);
			armHeld = ServiceArm.blocked(level, arm);
		}
		standTier = site.standTier;
		BlockPos battery = at(site.battery);
		if (battery != null) {
			packSeconds = now / 20.0 + (battery.asLong() & 1023);
		}
		for (int i = 0; i < site.gates.size(); i++) {
			BlockPos at = at(site.gates.get(i));
			gates.add(new Gate(at != null && level.isLoaded(at) && open(level.getBlockState(at)), site.gateIn.get(i)));
		}
		if (!site.entrance.isEmpty()) {
			doors.add(new Door("MAIN ENTRANCE", anyOpen(site.entrance)));
		}
		readers = !site.readerDoors.isEmpty();
		if (readers) {
			doors.add(new Door("DATA HALL  READER", anyOpen(site.readerDoors)));
		}
		for (int i = 0; i < site.aisleDoors.size(); i++) {
			doors.add(new Door("COLD AISLE " + site.aisleFacing.get(i).getSerializedName().toUpperCase(Locale.ROOT), anyOpen(List.of(site.aisleDoors.get(i)))));
		}
		zoneHall = site.hub == null ? null : new double[] {site.hub.getX() - 1.5, 0};
		zoneOpsWest = mean(site.desks, -1);
		zoneOpsEast = mean(site.desks, 1);
		zoneBay = site.arm == null ? null : new double[] {site.arm.getX() - 2, site.arm.getZ()};
		zoneLobby = site.gates.isEmpty() ? null : new double[] {0, site.gates.get(0).getZ()};
		zoneMap = site.mapTable == null ? null : new double[] {site.mapTable.getX(), site.mapTable.getZ()};
	}

	private boolean anyOpen(List<BlockPos> offsets) {
		for (BlockPos offset : offsets) {
			BlockPos at = at(offset);
			if (at != null && level.isLoaded(at) && open(level.getBlockState(at))) {
				return true;
			}
		}
		return false;
	}

	/** The middle of the ground floor's ops desks on one side (x < -10 or x > 10, south of the lobby), or null. */
	private static double @Nullable [] mean(List<BlockPos> desks, int side) {
		double x = 0, z = 0;
		int n = 0;
		for (BlockPos p : desks) {
			if (p.getX() * side > 10 && p.getZ() > -30) {
				x += p.getX() + 0.5;
				z += p.getZ() + 0.5;
				n++;
			}
		}
		return n == 0 ? null : new double[] {x / n, z / n};
	}

	private void fill(DepotView v) {
		fleet = v.docks.size() + v.flights.size();
		max = v.maxDrones;
		maxTier = Math.max(1, v.maxTier);
		air = v.airborne(now);
		active = v.active();
		docked = v.docks.size();
		energy = v.energy;
		capacity = v.capacity;
		draw = v.draw();
		work = v.workingDraw;
		standby = v.standbyDraw;
		load = v.loadPercent();
		lowPower = v.lowPower;
		packager = v.packager;
		cargo = v.cargo;
		pickups.addAll(v.pickups);
		targets = v.openTargets;
		covered = v.covered;
		v.missing.forEach((id, n) -> missing.add(item(id, n)));
		v.stock.forEach((id, n) -> stock.add(item(id, n)));
		roofPads = v.towerPads;
		// the hangar bays, and which flight belongs to which (the dock it left from)
		Map<Long, int[]> doorByDock = new HashMap<>();
		for (int[] door : v.doors) {
			doorByDock.put(key(door[8], door[9]), door);
		}
		for (DepotView.DockView dock : v.docks) {
			if (dock.tier() >= 1 && dock.tier() < home.length) {
				home[dock.tier()]++;
			}
			if (!doorByDock.containsKey(key((int) Math.round(dock.x() * 2), (int) Math.round(dock.z() * 2)))) {
				roofDocked++;
			}
		}
		Map<Long, Double> opening = new HashMap<>();
		for (DepotView.FlightView f : v.flights) {
			FlightPath path = f.path();
			double t = f.ticksAt(now, v.syncTime);
			long homeKey = key((int) Math.round(path.x(0) * 2), (int) Math.round(path.z(0) * 2));
			int[] door = doorByDock.get(homeKey);
			if (f.tier() >= 1 && f.tier() < out.length) {
				out[f.tier()]++;
			}
			flights.add(flight(f, path, t, door));
			if (door != null) {
				double cx = (door[0] + door[2] + 1) * .5, cz = (door[1] + door[3] + 1) * .5;
				double toPlane = door[6] != 0 ? Math.abs(cx - link.getX() - path.x(0)) : Math.abs(cz - link.getZ() - path.z(0));
				double width = door[6] != 0 ? door[3] - door[1] + 1 : door[2] - door[0] + 1;
				opening.merge(homeKey, DroneDepotRenderer.slidingDoorOpening(path, t, toPlane, width), Math::max);
			}
		}
		flights.sort(Comparator.comparingInt(Flight::id));
		for (int[] door : v.doors) {
			if (door.length >= 13) {
				bays.add(new Bay(door[10], door[11], door[12], opening.getOrDefault(key(door[8], door[9]), 0.0)));
			}
		}
		bays.sort(Comparator.comparingInt(Bay::pillar).thenComparingInt(Bay::number));
	}

	private void pads(DepotView v, TowerCoreBlockEntity c) {
		BlockPos core = c.getBlockPos();
		for (int t = 1; t <= tier && t <= layout.tiers.size(); t++) {
			for (TowerData.Dock dock : layout.tier(t).docks) {
				if (dock.door() != null) {
					continue;
				}
				boolean known = false;
				for (double[] pad : pads) {
					if (Math.abs(pad[0] - dock.x()) <= 4.5 && Math.abs(pad[1] - dock.z()) <= 4.5) {
						known = true;
						break;
					}
				}
				if (!known) {
					pads.add(new double[] {dock.x(), dock.z(), 0});
				}
			}
		}
		for (DepotView.DockView dock : v.docks) {
			double x = link.getX() + dock.x() - core.getX(), z = link.getZ() + dock.z() - core.getZ();
			for (double[] pad : pads) {
				if (Math.abs(pad[0] - x) <= 4.5 && Math.abs(pad[1] - z) <= 4.5) {
					pad[2]++;
					break;
				}
			}
		}
		pads.sort(Comparator.<double[]>comparingDouble(p -> p[1]).thenComparingDouble(p -> p[0]));
	}

	private static long key(int x2, int z2) {
		return BlockPos.asLong(x2, 0, z2);
	}

	private Flight flight(DepotView.FlightView f, FlightPath path, double t, int @Nullable [] door) {
		long total = path.totalTicks(), homeAt = total - FlightScheduler.RECHARGE_TICKS;
		int legs = path.legs();
		String state = t >= homeAt ? "CHARGE" : t < path.pickupArrive() ? "PICKUP" : t < path.pickupLeave() ? "LOADING"
				: legs > 1 && t < path.legEnd(1) ? "OUTBOUND" : t < path.legStart(legs - 1) ? "WORKING" : "RETURN";
		FlightPath.Pose pose = path.poseAt(t);
		double x = link.getX() + pose.x() - cx, z = link.getZ() + pose.z() - cz;
		int stop = path.points() > 3 ? 2 : 1;
		double wx = link.getX() + path.x(stop) - cx, wz = link.getZ() + path.z(stop) - cz;
		int eta = (int) Math.ceil(Math.max(0, (state.equals("CHARGE") ? total : homeAt) - t) / 20.0);
		String home = door == null ? "ROOF PAD" : "HANGAR " + new Bay(door[10], door[11], 0, 0).name();
		return new Flight(f.drone(), f.tier(), state, x, z, Math.hypot(x, z), bearing(x, z), wx, wz, Math.hypot(wx, wz), bearing(wx, wz), eta,
				Math.min(1, t / Math.max(1, total)), home, t < homeAt, t / 20.0);
	}

	/** The compass point from the tower's middle (north is -z). */
	static String bearing(double x, double z) {
		double a = (Math.toDegrees(Math.atan2(x, -z)) + 360 + 22.5) % 360;
		return BEARINGS[(int) (a / 45) % 8];
	}

	static int sector(double x, double z) {
		double a = (Math.toDegrees(Math.atan2(x, -z)) + 360 + 22.5) % 360;
		return (int) (a / 45) % 8;
	}

	private static Item item(String id, int count) {
		Identifier rid = Identifier.tryParse(id);
		ItemStack stack = rid == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.getValue(rid));
		String name = stack.isEmpty() ? id.substring(id.indexOf(':') + 1).replace('_', ' ') : stack.getHoverName().getString();
		return new Item(id, name.toUpperCase(Locale.ROOT), count);
	}

	boolean linked() {
		return view != null;
	}

	/** Seconds of power left at today's draw (0: no draw). */
	long reserveSeconds() {
		return draw <= 0 ? 0 : energy / draw / 20;
	}

	static String n(long v) {
		return String.format(Locale.ROOT, "%,d", v);
	}

	static String minutes(long seconds) {
		if (seconds >= 3600) {
			return seconds / 3600 + " H " + seconds / 60 % 60 + " MIN";
		}
		return seconds >= 60 ? seconds / 60 + " MIN" : seconds + " S";
	}

	/** A drone tier's name ("COURIER QUAD"). */
	static String tierName(int t) {
		return DroneTier.byNumber(t).map(d -> d.name().replace('_', ' ')).orElse("TIER " + t);
	}

	/** The clock time {@code seconds} from now ("14:36"). */
	String clockIn(double seconds) {
		long t = Math.floorMod(clockTicks + 6000 + (long) (seconds * 20), 24000);
		return String.format(Locale.ROOT, "%02d:%02d", t / 1000, t % 1000 * 60 / 1000);
	}

	/** A block in the hall's layout, absolute (null when the site has none). */
	@Nullable BlockPos at(@Nullable BlockPos offset) {
		return offset == null || core == null ? null : core.getBlockPos().offset(offset);
	}

	/** Players standing in the hall's ground floor, from the tower's middle (x, z). */
	private List<double[]> players() {
		List<double[]> out = new ArrayList<>();
		if (core == null) {
			return out;
		}
		double y0 = core.getBlockPos().getY();
		for (Player p : level.players()) {
			if (!p.isSpectator() && Math.hypot(p.getX() - cx, p.getZ() - cz) < 56 && p.getY() > y0 - 1 && p.getY() < y0 + 7) {
				out.add(new double[] {p.getX() - cx, p.getZ() - cz});
			}
		}
		return out;
	}

	// ================================================================== the hall's live pieces

	/** The pieces of a tower's hall the desks read, found once in its layout (offsets from the core). */
	static final class Site {
		@Nullable BlockPos arm, stand, battery, hub, mapTable, holoTable;
		int standTier = 4;
		final List<BlockPos> racks = new ArrayList<>(), cracs = new ArrayList<>(), cylinders = new ArrayList<>();
		final List<String> rackKinds = new ArrayList<>();
		/** Speed gate lanes west to east, and whether each is the way in. */
		final List<BlockPos> gates = new ArrayList<>();
		final List<Boolean> gateIn = new ArrayList<>();
		/** Doors' lower halves: the entrance's, the data hall's (with readers), the roof's. */
		final List<BlockPos> entrance = new ArrayList<>(), readerDoors = new ArrayList<>(), roofDoors = new ArrayList<>();
		/** The aisle containment doors and which way each one's outside faces. */
		final List<BlockPos> aisleDoors = new ArrayList<>();
		final List<Direction> aisleFacing = new ArrayList<>();
		/** The ground floor's desks. */
		final List<BlockPos> desks = new ArrayList<>();

		static Site of(Level level, BlockPos core, int version, TowerData layout) {
			Map<Long, Site> sites = SITES.computeIfAbsent(level, k -> new HashMap<>());
			return sites.computeIfAbsent(core.asLong() * 31 + version, k -> scan(layout));
		}

		private static Site scan(TowerData layout) {
			Site s = new Site();
			if (layout.foundation == null) {
				return s;
			}
			Block arm = JugcraftTower.BLOCKS.get("drone_service_arm");
			Map<Long, BlockPos> racksByColumn = new HashMap<>();
			for (int[] b : layout.foundation.place) {
				BlockState state = layout.state(b[3]);
				Block block = state.getBlock();
				BlockPos at = new BlockPos(b[0], b[1], b[2]);
				if (arm != null && block == arm) {
					s.arm = at;
				} else if (block instanceof HallFixtureBlock.Stand && state.getValue(HallFixtureBlock.Stand.STYLE) == HallFixtureBlock.Style.SERVICE) {
					s.stand = at;
					s.standTier = state.getValue(HallFixtureBlock.Stand.TIER);
				} else if (block instanceof DronePartsShelvingBlock shelf && shelf.isAnchor(state)
						&& state.getValue(DronePartsShelvingBlock.STOCK) == DronePartsShelvingBlock.Stock.A) {
					s.battery = at;
				} else if (block instanceof SpeedGateBlock && state.getValue(SpeedGateBlock.PART) == SpeedGateBlock.Part.LANE) {
					s.gates.add(at);
				} else if (block instanceof PlantDoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
					(state.getValue(PlantDoorBlock.READER) ? s.readerDoors : b[1] > 4 ? s.roofDoors : s.entrance).add(at);
				} else if (block instanceof AisleContainmentDoorBlock) {
					s.aisleDoors.add(at);
					s.aisleFacing.add(state.getValue(AisleContainmentDoorBlock.FACING));
				} else if (block instanceof HoloTableBlock table && table.isAnchor(state)) {
					if (b[0] == 0 && b[2] == 0) {
						s.holoTable = at;
					} else {
						s.mapTable = at;
					}
				} else if (block instanceof io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock && b[1] <= 2) {
					s.desks.add(at);
				} else if (block instanceof HoloCommandTableBlock) {
					s.holoTable = s.holoTable == null ? at : s.holoTable;
				} else {
					int kind = ServerRoom.kind(state);
					switch (kind) {
						case ServerRoom.HUB -> s.hub = at;
						case ServerRoom.COOLER -> s.cracs.add(at);
						case ServerRoom.CYLINDER -> s.cylinders.add(at);
						case ServerRoom.RACK -> {
							long column = BlockPos.asLong(b[0], 0, b[2]);
							BlockPos was = racksByColumn.get(column);
							if (was == null || b[1] < was.getY()) {
								racksByColumn.put(column, at);
							}
						}
						default -> {
						}
					}
				}
			}
			s.gates.sort(Comparator.comparingInt((BlockPos p) -> p.getX()));
			for (BlockPos g : s.gates) {
				BlockState state = null;
				for (int[] b : layout.foundation.place) {
					if (b[0] == g.getX() && b[1] == g.getY() && b[2] == g.getZ()) {
						state = layout.state(b[3]);
						break;
					}
				}
				// the entrance is on the north face: a lane facing south is the way in
				s.gateIn.add(state != null && state.getValue(SpeedGateBlock.FACING) == Direction.SOUTH);
			}
			List<BlockPos> racks = new ArrayList<>(racksByColumn.values());
			racks.sort(Comparator.comparingInt((BlockPos p) -> p.getZ()).thenComparingInt(p -> p.getX()));
			s.racks.addAll(racks);
			for (BlockPos r : racks) {
				for (int[] b : layout.foundation.place) {
					if (b[0] == r.getX() && b[1] == r.getY() && b[2] == r.getZ()) {
						String id = BuiltInRegistries.BLOCK.getKey(layout.state(b[3]).getBlock()).getPath();
						s.rackKinds.add(id.startsWith("network") ? "NET" : id.startsWith("san") ? "SAN" : id.startsWith("ppc") ? "PPC" : "SRV");
						break;
					}
				}
			}
			return s;
		}
	}

	/** The data hall's UPS (its hub), as the hall's own screens read it, or null. */
	@Nullable LivePanelBlockEntity hub() {
		BlockPos at = site == null ? null : at(site.hub);
		return at != null && level.getBlockEntity(at) instanceof LivePanelBlockEntity hub ? hub : null;
	}

	/** How busy the data hall's servers are now (0.2 to 0.95): by day more (as client/EdgTelemetry has the plant's). */
	double serverLoad() {
		double hour = Math.floorMod(clockTicks + 6000, 24000) / 1000.0;
		return Math.max(0.2, Math.min(0.95, 0.55 + 0.25 * Math.sin((hour - 9) / 24 * 2 * Math.PI) + 0.1 * Math.sin(now / 400 * 2.3 + 1.7)));
	}

	// ================================================================== what the desks record

	/**
	 * What a terminal's desks keep while they are looked at (the depot keeps no history): launches and landings over
	 * the last hour, the power draw every 10 seconds for 10 minutes, the open targets every minute for an hour, the
	 * build's progress (for its ETA), flights counted by tier, the event log and the security log. Slots no one saw stay
	 * empty.
	 */
	static final class Recorder {
		long start = -1;
		String since = "";
		long lastTick = -1;
		final Map<Integer, String> states = new HashMap<>();
		final Map<Integer, Double> reach = new HashMap<>();
		final ArrayDeque<long[]> launches = new ArrayDeque<>(), landings = new ArrayDeque<>();
		final float[] draw = new float[60], open = new float[60];
		final long[] drawSlot = new long[60], openSlot = new long[60];
		final ArrayDeque<double[]> build = new ArrayDeque<>();
		final int[] byTier = new int[11];
		long flightTicks;
		int longestId = -1;
		double longest;
		final ArrayDeque<Event> events = new ArrayDeque<>(), security = new ArrayDeque<>();
		final java.util.Set<String> short_ = new java.util.HashSet<>();
		boolean lowPower, pickup, batt;
		int pct = -1, phase;
		final Map<BlockPos, Boolean> gates = new HashMap<>(), doors = new HashMap<>();

		Recorder() {
			java.util.Arrays.fill(drawSlot, -1);
			java.util.Arrays.fill(openSlot, -1);
		}

		void update(DeskData d) {
			if (d.gameTime == lastTick || d.view == null) {
				return;
			}
			boolean first = start < 0;
			if (first) {
				start = d.gameTime;
				since = "DAY " + d.dayNumber + " " + d.clock;
			}
			lastTick = d.gameTime;
			// launches and landings, and each flight's longest reach
			java.util.Set<Integer> seen = new java.util.HashSet<>();
			for (Flight f : d.flights) {
				seen.add(f.id());
				String was = states.put(f.id(), f.state());
				reach.merge(f.id(), f.dist(), Math::max);
				if (!first && was == null) {
					launches.add(new long[] {d.gameTime, f.tier()});
					log(d, "T" + f.tier() + " #" + two(f.id()) + " OUTBOUND  " + f.workBearing() + " " + Math.round(f.workDist()) + " M",
							DeskScreen.AMBER);
				}
				if (!first && was != null && !was.equals("CHARGE") && f.state().equals("CHARGE")) {
					landings.add(new long[] {d.gameTime, f.tier()});
					if (f.tier() >= 1 && f.tier() < byTier.length) {
						byTier[f.tier()]++;
					}
					log(d, "T" + f.tier() + " #" + two(f.id()) + " LANDED  " + f.home(), DeskScreen.GREEN);
				}
			}
			for (java.util.Iterator<Map.Entry<Integer, String>> it = states.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<Integer, String> e = it.next();
				if (!seen.contains(e.getKey())) {
					double far = reach.getOrDefault(e.getKey(), 0.0);
					if (!first && !e.getValue().equals("CHARGE")) {
						landings.add(new long[] {d.gameTime, 0});
					}
					if (far > longest) {
						longest = far;
						longestId = e.getKey();
					}
					reach.remove(e.getKey());
					it.remove();
				}
			}
			for (Flight f : d.flights) {
				if (f.state().equals("CHARGE")) {
					continue;
				}
				flightTicks++;
			}
			while (!launches.isEmpty() && launches.peekFirst()[0] < d.gameTime - 72000) {
				launches.pollFirst();
			}
			while (!landings.isEmpty() && landings.peekFirst()[0] < d.gameTime - 72000) {
				landings.pollFirst();
			}
			long slot = d.gameTime / 200;
			draw[(int) (slot % 60)] = d.draw;
			drawSlot[(int) (slot % 60)] = slot;
			long minute = d.gameTime / 1200;
			open[(int) (minute % 60)] = d.targets;
			openSlot[(int) (minute % 60)] = minute;
			if (d.building > 0) {
				if (build.isEmpty() || d.gameTime - build.peekLast()[0] >= 100) {
					build.add(new double[] {d.gameTime, d.progress, d.building});
				}
				while (build.size() > 2 && (build.peekFirst()[2] != d.building || build.peekFirst()[0] < d.gameTime - 6000)) {
					build.pollFirst();
				}
			} else {
				build.clear();
			}
			// the event log: supply, power, the pickup, the build
			for (Item m : d.missing) {
				if (short_.add(m.id()) && !first) {
					log(d, "SUPPLY SHORT  " + m.name(), DeskScreen.RED);
				}
			}
			if (d.missing.isEmpty() && !short_.isEmpty()) {
				short_.clear();
				log(d, "SUPPLY OK  NOTHING SHORT", DeskScreen.GREEN);
			}
			if (d.lowPower != lowPower) {
				lowPower = d.lowPower;
				if (!first) {
					log(d, lowPower ? "LOW POWER" : "POWER RESTORED", lowPower ? DeskScreen.RED : DeskScreen.GREEN);
				}
			}
			if (!d.pickups.isEmpty() != pickup) {
				pickup = !d.pickups.isEmpty();
				if (!first) {
					log(d, pickup ? "PICKUP ONLINE" : "PICKUP OFFLINE", pickup ? DeskScreen.GREEN : DeskScreen.RED);
				}
			}
			int p = d.building > 0 ? (int) (d.progress * 10) : -1;
			if (p != pct) {
				if (!first && d.building > 0) {
					log(d, "TIER " + d.building + " BUILD " + p * 10 + "%", DeskScreen.WHITE);
				}
				pct = p;
			}
			security(d, first);
		}

		private void security(DeskData d, boolean first) {
			if (d.site == null) {
				return;
			}
			for (int i = 0; i < d.site.gates.size(); i++) {
				BlockPos at = d.at(d.site.gates.get(i));
				if (at == null || !d.level.isLoaded(at)) {
					continue;
				}
				BlockState state = d.level.getBlockState(at);
				boolean opened = state.getBlock() instanceof SpeedGateBlock && state.getValue(SpeedGateBlock.OPEN);
				Boolean was = gates.put(at, opened);
				if (!first && was != null && !was && opened) {
					alarm(d, "GATE " + (i + 1) + "  OPENED  " + (d.site.gateIn.get(i) ? "IN" : "OUT"));
				}
			}
			List<BlockPos> watched = new ArrayList<>(d.site.readerDoors);
			watched.addAll(d.site.aisleDoors);
			for (BlockPos offset : watched) {
				BlockPos at = d.at(offset);
				if (at == null || !d.level.isLoaded(at)) {
					continue;
				}
				boolean opened = DeskData.open(d.level.getBlockState(at));
				Boolean was = doors.put(at, opened);
				if (!first && was != null && !was && opened) {
					alarm(d, (d.site.readerDoors.contains(offset) ? "DATA HALL DOOR" : "COLD AISLE DOOR") + "  OPENED");
				}
			}
			LivePanelBlockEntity hub = d.hub();
			if (hub != null) {
				if (hub.srvPhase != phase) {
					if (!first) {
						alarm(d, "FM-200  " + switch (hub.srvPhase) {
							case ServerRoom.ALARM -> "ALARM";
							case ServerRoom.PRE_DISCHARGE -> "PRE-DISCHARGE";
							case ServerRoom.RELEASED -> "RELEASED";
							case ServerRoom.DRILL -> "DRILL";
							default -> "NORMAL";
						});
					}
					phase = hub.srvPhase;
				}
				boolean onBatt = (ServerRoom.bits(hub, d.now) & ServerRoom.ON_BATT) != 0;
				if (onBatt != batt) {
					if (!first) {
						alarm(d, onBatt ? "UPS-1  ON BATTERY" : "UPS-1  BACK ON MAINS");
					}
					batt = onBatt;
				}
			}
		}

		private void log(DeskData d, String text, int colour) {
			events.addFirst(new Event(d.gameTime, d.clock, text, colour));
			while (events.size() > 30) {
				events.pollLast();
			}
		}

		private void alarm(DeskData d, String text) {
			security.addFirst(new Event(d.gameTime, d.clock, text, DeskScreen.TXT));
			while (security.size() > 20) {
				security.pollLast();
			}
		}

		/** Launches or landings in each of the last {@code bins} 5-minute bins (oldest first). */
		int[] bins(ArrayDeque<long[]> events, long now, int bins) {
			int[] out = new int[bins];
			for (long[] e : events) {
				int back = (int) ((now - e[0]) / 6000);
				if (back >= 0 && back < bins) {
					out[bins - 1 - back]++;
				}
			}
			return out;
		}

		int lastHour(ArrayDeque<long[]> events, long now) {
			int n = 0;
			for (long[] e : events) {
				if (e[0] >= now - 72000) {
					n++;
				}
			}
			return n;
		}

		/** The build's rate (fraction a tick) over what was recorded of it, or 0. */
		double buildRate() {
			if (build.size() < 2) {
				return 0;
			}
			double[] a = build.peekFirst(), b = build.peekLast();
			return b[0] > a[0] && b[2] == a[2] ? Math.max(0, (b[1] - a[1]) / (b[0] - a[0])) : 0;
		}
	}

	static String two(int id) {
		return id < 10 ? "0" + id : String.valueOf(id);
	}

	/** A door, gate or containment door's state: open. */
	static boolean open(BlockState state) {
		if (state.getBlock() instanceof AisleContainmentDoorBlock) {
			return state.getValue(AisleContainmentDoorBlock.OPEN);
		}
		if (state.getBlock() instanceof DoorBlock) {
			return state.getValue(DoorBlock.OPEN);
		}
		if (state.getBlock() instanceof SpeedGateBlock) {
			return state.getValue(SpeedGateBlock.OPEN);
		}
		return false;
	}
}
