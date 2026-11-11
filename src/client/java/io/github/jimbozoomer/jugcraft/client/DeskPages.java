package io.github.jimbozoomer.jugcraft.client;

import static io.github.jimbozoomer.jugcraft.client.DeskScreen.AMBER;
import static io.github.jimbozoomer.jugcraft.client.DeskScreen.BG;
import static io.github.jimbozoomer.jugcraft.client.DeskScreen.CYAN;
import static io.github.jimbozoomer.jugcraft.client.DeskScreen.DIM;
import static io.github.jimbozoomer.jugcraft.client.DeskScreen.GREEN;
import static io.github.jimbozoomer.jugcraft.client.DeskScreen.GRID;
import static io.github.jimbozoomer.jugcraft.client.DeskScreen.H;
import static io.github.jimbozoomer.jugcraft.client.DeskScreen.LOCKED;
import static io.github.jimbozoomer.jugcraft.client.DeskScreen.OFF;
import static io.github.jimbozoomer.jugcraft.client.DeskScreen.RED;
import static io.github.jimbozoomer.jugcraft.client.DeskScreen.ROW;
import static io.github.jimbozoomer.jugcraft.client.DeskScreen.TILE;
import static io.github.jimbozoomer.jugcraft.client.DeskScreen.TXT;
import static io.github.jimbozoomer.jugcraft.client.DeskScreen.W;
import static io.github.jimbozoomer.jugcraft.client.DeskScreen.WHITE;
import static io.github.jimbozoomer.jugcraft.client.DeskScreen.dark;
import static io.github.jimbozoomer.jugcraft.client.DeskScreen.light;
import static io.github.jimbozoomer.jugcraft.client.DeskScreen.rgb;
import static io.github.jimbozoomer.jugcraft.client.DeskData.n;
import static io.github.jimbozoomer.jugcraft.client.DeskData.two;

import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.ServerRoom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.joml.Vector3f;

/**
 * The fifteen desk roles' pages, three each (the left, middle and right monitors as the operator faces them), as the
 * approved mockup sheet drew them (Tri-Pillar work list item 14, claude/src/dronetower/tools/mockups/ds_roles.py),
 * every figure live: see {@link DeskData}.
 */
final class DeskPages {
	interface Page {
		void draw(DeskScreen s, DeskData d);
	}

	/** The roles (as tower/ConsoleDeskBlock ROLES), and each one's short code for the title bar. */
	static final String[] NAMES = {"FLIGHT DIRECTOR", "AIRSPACE", "FLEET", "DISPATCH", "MISSIONS", "TOWER BUILD", "POWER", "SUPPLY", "DOCKS",
			"MAINTENANCE", "DATA HALL", "COMMS", "RECEPTION", "SECURITY", "MAP TABLE"};
	static final String[] CODES = {"FLT DIR", "AIRSPACE", "FLEET", "DISPATCH", "MISSIONS", "TOWER", "POWER", "SUPPLY", "DOCKS", "MAINT",
			"DATA HALL", "COMMS", "WELCOME", "SECURITY", "MAP"};
	static final Page[][] PAGES = {
			{DeskPages::fdSummary, DeskPages::fdAirspace, DeskPages::fdAlerts},
			{DeskPages::asTraffic, DeskPages::asRadar, DeskPages::asSectors},
			{DeskPages::flRoster, DeskPages::flDrones, DeskPages::flHangars},
			{DeskPages::dsReady, DeskPages::dsPads, DeskPages::dsRecovery},
			{DeskPages::msJobs, DeskPages::msMaterials, DeskPages::msProgress},
			{DeskPages::tbTiers, DeskPages::tbCurrent, DeskPages::tbCapacity},
			{DeskPages::pwStorage, DeskPages::pwTrend, DeskPages::pwLoads},
			{DeskPages::spPackager, DeskPages::spPickups, DeskPages::spStock},
			{DeskPages::dkDocks, DeskPages::dkDoors, DeskPages::dkTraffic},
			{DeskPages::mtBay, DeskPages::mtParts, DeskPages::mtHours},
			{DeskPages::itUps, DeskPages::itRacks, DeskPages::itCooling},
			{DeskPages::cmWeather, DeskPages::cmLog, DeskPages::cmNetwork},
			{DeskPages::rcDirectory, DeskPages::rcBoard, DeskPages::rcTower},
			{DeskPages::scAccess, DeskPages::scPlan, DeskPages::scAlarms},
			{DeskPages::mpRange, DeskPages::mpLegend, DeskPages::mpDrones}};
	/** The hall's radius (tools/tripillar_geometry.py HEX_R): flat sides north and south, corners east and west. */
	static final double HALL_R = 52;

	private DeskPages() {
	}

	/** Draws role {@code role}'s page {@code page} (0 to 2), or what the desk shows with no terminal to read. */
	static void draw(DeskScreen s, int role, int page, DeskData d) {
		role = Math.max(0, Math.min(PAGES.length - 1, role));
		page = Math.max(0, Math.min(2, page));
		s.clear();
		s.background(BG);
		if (d == null || !d.linked()) {
			s.rect(0, 0, W, 12, DeskScreen.HEAD);
			s.rect(0, 12, W, 0.6, AMBER);
			s.text(4, 2.5, CODES[role], AMBER, 1.0);
			s.text(W / 2, 58, d == null ? "NO TERMINAL LINK" : "SYNCHRONIZING...", d == null ? RED : AMBER, 1.3, "center");
			s.text(W / 2, 76, d == null ? "PIN THIS DESK TO A DEPOT TERMINAL" : "WAITING FOR THE DEPOT", DIM, 0.75, "center");
			s.footer(NAMES[role], (page + 1) + "/3", false);
			return;
		}
		PAGES[role][page].draw(s, d);
	}

	// ------------------------------------------------------------------ shared pieces

	private static String pct(double f) {
		return Math.round(Math.max(0, f) * 100) + "%";
	}

	/** The drone tiers a roster shows: those with docks or drones (the lowest first), at most {@code most} of the highest. */
	private static List<Integer> tiers(DeskData d, int most) {
		List<Integer> out = new ArrayList<>();
		for (int t = 1; t <= 10; t++) {
			if (d.cap[t] > 0 || d.home[t] + d.out[t] > 0) {
				out.add(t);
			}
		}
		if (out.isEmpty()) {
			out.add(1);
		}
		return out.subList(Math.max(0, out.size() - most), out.size());
	}

	/** The radar's outer ring: 256 blocks, or the next doubling that holds the farthest drone. */
	private static int range(DeskData d) {
		double far = 0;
		for (DeskData.Flight f : d.flights) {
			far = Math.max(far, f.dist());
		}
		int r = 256;
		while (r < far * 1.05 && r < 8192) {
			r *= 2;
		}
		return r;
	}

	/** The radar's sweep (degrees): one turn every two seconds. */
	private static double sweep(DeskData d) {
		return (d.now / 40.0 * 360) % 360;
	}

	private static void radar(DeskScreen s, double cx, double cy, double r, DeskData d, int range, boolean labels) {
		s.dot(cx, cy, r, rgb(10, 22, 24));
		for (int k = 0; k < 3; k++) {
			double rr = r * (k + 1) / 3;
			s.ring(cx, cy, rr, rgb(28, 60, 62), 0.45);
			if (labels) {
				s.text(cx + 1.5, cy - rr + 1, String.valueOf(range * (1 << k) / 4), rgb(60, 100, 104), 0.5);
			}
		}
		s.group(() -> {
			s.line(cx - r, cy, cx + r, cy, rgb(28, 60, 62), 0.35);
			s.line(cx, cy - r, cx, cy + r, rgb(28, 60, 62), 0.35);
		});
		double a = Math.toRadians(sweep(d));
		// the sweep (its fading trail one layer), the lines out to the drones (one layer over it), the drones, the tower
		s.group(() -> {
			for (int k = 0; k < 10; k++) {
				double aa = a - k * 0.05;
				s.line(cx, cy, cx + Math.cos(aa) * r, cy + Math.sin(aa) * r, rgb(40, 120 - k * 8, 110 - k * 8), 0.6);
			}
		});
		double scale = r / range;
		List<double[]> at = new ArrayList<>();
		for (DeskData.Flight f : d.flights) {
			double dist = Math.hypot(f.x(), f.z()), k = dist * scale > r - 1.5 ? (r - 1.5) / (dist * scale) : 1;
			at.add(new double[] {cx + f.x() * scale * k, cy + f.z() * scale * k, DeskScreen.tier(f.tier())});
		}
		s.group(() -> {
			for (double[] p : at) {
				s.line(cx, cy, p[0], p[1], dark((int) p[2], 4), 0.4);
			}
		});
		for (double[] p : at) {
			s.dot(p[0], p[1], 1.4, (int) p[2]);
		}
		s.dot(cx, cy, 2.0, AMBER);
	}

	private static void tierRows(DeskScreen s, double x, double y, double w, DeskData d) {
		for (int t = 1; t <= 9; t++) {
			double yy = y + (t - 1) * 10.5;
			int c = DeskScreen.tier(t);
			boolean got = t <= d.maxTier && (d.cap[t] > 0 || d.home[t] + d.out[t] > 0);
			s.rect(x, yy, 3, 7, got ? c : GRID);
			s.text(x + 5, yy, "T" + t, got ? c : DIM, 0.85);
			if (got) {
				int n = d.home[t] + d.out[t], cap = Math.max(d.cap[t], n);
				s.bar(x + 22, yy + 1, w - 64, 5, (double) n / Math.max(1, cap), c);
				s.text(x + w, yy, d.home[t] + "+" + d.out[t] + "/" + cap, TXT, 0.75, "right");
			} else {
				s.text(x + 22, yy, t <= d.maxTier ? "NO DOCKS YET" : "LOCKED", rgb(60, 72, 74), 0.7);
			}
		}
	}

	private static void spark(DeskScreen s, double x, double y, double w, double h, float[] vals, boolean[] have, int c, double lo, double hi) {
		for (int k = 1; k < 4; k++) {
			s.line(x, y + h * k / 4, x + w, y + h * k / 4, rgb(24, 34, 38), 0.3);
		}
		int n = vals.length;
		double[] px = new double[n], py = new double[n];
		for (int i = 0; i < n; i++) {
			px[i] = x + w * i / (n - 1);
			py[i] = y + h - h * Math.max(0, Math.min(1, (vals[i] - lo) / Math.max(1e-9, hi - lo)));
		}
		for (int i = 0; i + 1 < n; i++) {
			if (have[i] && have[i + 1]) {
				s.patch(px[i], py[i], px[i], y + h, px[i + 1], y + h, px[i + 1], py[i + 1], dark(c, 5));
			}
		}
		s.group(() -> {
			for (int i = 0; i + 1 < n; i++) {
				if (have[i] && have[i + 1]) {
					s.line(px[i], py[i], px[i + 1], py[i + 1], c, 0.8);
				}
			}
		});
	}

	/** A recorder's trace, oldest first, and which samples were seen. */
	private static float[] trace(float[] ring, long[] slots, long current, boolean[] have) {
		float[] out = new float[ring.length];
		for (int i = 0; i < ring.length; i++) {
			long slot = current - (ring.length - 1) + i;
			int k = (int) Math.floorMod(slot, (long) ring.length);
			have[i] = slot >= 0 && slots[k] == slot;
			out[i] = have[i] ? ring[k] : 0;
		}
		return out;
	}

	private static void panel(DeskScreen s, double y, double h, int accent) {
		s.rect(4, y, W - 8, h, ROW);
		s.rect(4, y, 2.2, h, accent);
	}

	// ================================================================ WEST: flight director, airspace, fleet, dispatch, missions, build

	static void fdSummary(DeskScreen s, DeskData d) {
		s.header("FLT DIR", "FLEET SUMMARY", d);
		s.big(6, 17, "FLEET", d.fleet + "/" + d.max, WHITE, "LINKED / DOCKS");
		s.big(86, 17, "AIRBORNE", String.valueOf(d.air), AMBER, "IN FLIGHT NOW");
		s.big(146, 17, "ACTIVE", String.valueOf(d.active), CYAN, "ON JOBS");
		s.big(200, 17, "DOCKED", String.valueOf(d.docked), GREEN, "ON STANDBY");
		s.rect(4, 52, W - 8, 0.4, GRID);
		List<Integer> tiers = tiers(d, 3);
		double step = tiers.size() <= 2 ? 16 : 12.5;
		for (int i = 0; i < tiers.size(); i++) {
			int t = tiers.get(i);
			double y = 58 + i * step;
			int c = DeskScreen.tier(t);
			int n = d.home[t] + d.out[t], cap = Math.max(1, Math.max(d.cap[t], n));
			s.text(6, y, "T" + t, c, 1.1);
			s.text(24, y + 1, DeskData.tierName(t), DIM, 0.68);
			s.bar(24, y + 9, 150, 3.5, (double) d.home[t] / cap, c);
			s.rect(24 + 150.0 * d.home[t] / cap, y + 9, 150.0 * d.out[t] / cap, 3.5, light(c, 90));
			s.text(W - 6, y + 2, d.home[t] + " HOME  " + d.out[t] + " OUT  " + Math.max(0, d.cap[t] - n) + " FREE", TXT, 0.72, "right");
		}
		int locked = Math.max(0, 9 - d.maxTier);
		s.text(6, 96, "LOCKED TIERS", DIM, 0.65);
		if (locked == 0) {
			s.text(6, 105.5, "ALL NINE DRONE TIERS UNLOCKED", GREEN, 0.75);
		}
		double bw = locked == 0 ? 0 : Math.min(30, (W - 12) / locked - 3);
		for (int k = 0; k < locked; k++) {
			int t = d.maxTier + 1 + k;
			s.rect(6 + k * (bw + 3), 104, bw, 9, TILE);
			s.text(6 + k * (bw + 3) + bw / 2, 105.5, "T" + t, rgb(70, 84, 86), 0.7, "center");
		}
		if (d.building > 0) {
			s.text(6, 119, "BUILDING: T" + d.building + " " + name(d, d.building) + "  " + pct(d.progress), AMBER, 0.75);
			s.bar(6, 128, W - 12, 4, d.progress, AMBER);
		} else if (d.tier < 9) {
			s.text(6, 119, "NEXT: T" + (d.tier + 1) + " " + name(d, d.tier + 1) + "  UPGRADE AT THE CORE", TXT, 0.75);
			s.bar(6, 128, W - 12, 4, 0, AMBER);
		} else {
			s.text(6, 119, "THE TOWER IS COMPLETE", GREEN, 0.75);
			s.bar(6, 128, W - 12, 4, 1, GREEN);
		}
		s.footer("DEPOT LINK OK  " + d.mode + " MODE", "1/3", true);
	}

	private static String name(DeskData d, int tier) {
		return tier >= 1 && tier <= 9 && d.tierNames[tier - 1] != null ? d.tierNames[tier - 1] : "TIER " + tier;
	}

	static void fdAirspace(DeskScreen s, DeskData d) {
		s.header("FLT DIR", "AIRSPACE", d);
		int range = range(d);
		radar(s, 72, 78, 58, d, range, true);
		s.text(140, 18, "IN FLIGHT", DIM, 0.68);
		List<String[]> rows = new ArrayList<>();
		List<int[]> colours = new ArrayList<>();
		for (DeskData.Flight f : d.flights) {
			if (rows.size() >= 7) {
				break;
			}
			rows.add(new String[] {"#" + two(f.id()), "T" + f.tier(), f.state().substring(0, Math.min(4, f.state().length())), Math.round(f.dist()) + "M"});
			colours.add(new int[] {DeskScreen.tier(f.tier()), TXT, f.returning() ? GREEN : AMBER, TXT});
		}
		if (rows.isEmpty()) {
			s.text(140, 28, "NONE: ALL DOCKED", DIM, 0.7);
		}
		s.rows(140, 28, rows, colours, new double[] {140, 162, 178, W - 6}, new String[] {"left", "left", "left", "right"}, 0.7, 9);
		s.text(140, 97, "RANGE RINGS " + range / 4 + " / " + range / 2 + " / " + range, DIM, 0.6);
		s.footer("SCAN 2.0 S  TRACKS " + d.flights.size(), "2/3", true);
	}

	static void fdAlerts(DeskScreen s, DeskData d) {
		s.header("FLT DIR", "ALERTS", d);
		List<Object[]> items = new ArrayList<>();
		if (d.missing.isEmpty()) {
			items.add(new Object[] {"SUPPLY", "NOTHING SHORT FOR THE OPEN JOBS", GREEN});
		} else {
			StringBuilder names = new StringBuilder();
			for (DeskData.Item m : d.missing) {
				names.append(names.isEmpty() ? "" : ", ").append(m.name());
			}
			items.add(new Object[] {"SUPPLY SHORT", d.missing.size() + " ITEMS: " + names, AMBER});
		}
		long reserve = d.reserveSeconds();
		items.add(d.lowPower ? new Object[] {"POWER", "LOW POWER  " + d.load + "% LOAD", RED}
				: new Object[] {"POWER", d.load + "% LOAD  RESERVE " + (reserve > 0 ? DeskData.minutes(reserve) : "FULL"), d.load > 90 ? AMBER : GREEN});
		items.add(d.pickups.isEmpty() ? new Object[] {"PICKUP", "NO SUPPLY PICKUP IN USE", RED}
				: new Object[] {"PICKUP", d.pickups.size() + (d.pickups.size() == 1 ? " PICKUP" : " PICKUPS") + " ONLINE", GREEN});
		items.add(!d.packager ? new Object[] {"PACKAGER", "NO PACKAGER LINKED", RED}
				: new Object[] {"PACKAGER", d.cargo == null ? "ONLINE" : d.cargo.occupiedSlots() + " / " + d.cargo.totalSlots() + " SLOTS", GREEN});
		int[] st = bayCounts(d);
		items.add(d.bays.isEmpty() ? new Object[] {"HANGAR DOORS", "NO HANGAR BAYS YET", CYAN}
				: new Object[] {"HANGAR DOORS", st[1] + " HOME  " + st[2] + " OUT  " + st[0] + " EMPTY", GREEN});
		items.add(d.building > 0 ? new Object[] {"BUILD", "TIER " + d.building + " IN PROGRESS " + pct(d.progress), CYAN}
				: d.tier >= 9 ? new Object[] {"BUILD", "ALL NINE TIERS BUILT", GREEN}
				: new Object[] {"BUILD", "TIER " + d.tier + " BUILT  NEXT: TIER " + (d.tier + 1), CYAN});
		int warn = 0, alarms = 0;
		for (int i = 0; i < items.size(); i++) {
			Object[] it = items.get(i);
			int c = (int) it[2];
			double y = 18 + i * 19;
			panel(s, y - 2, 16, c);
			s.text(10, y, (String) it[0], c, 0.85);
			s.text(10, y + 8.5, s.fit((String) it[1], W - 50, 0.66), TXT, 0.66);
			s.text(W - 8, y, c == AMBER ? "WARN" : c == RED ? "ALARM" : c == GREEN ? "OK" : "INFO", c, 0.7, "right");
			warn += c == AMBER ? 1 : 0;
			alarms += c == RED ? 1 : 0;
		}
		s.footer(warn + (warn == 1 ? " WARNING  " : " WARNINGS  ") + alarms + (alarms == 1 ? " ALARM" : " ALARMS"), "3/3", warn + alarms == 0);
	}

	/** Hangar bays empty, home, out. */
	private static int[] bayCounts(DeskData d) {
		int[] out = new int[3];
		for (DeskData.Bay b : d.bays) {
			out[Math.max(0, Math.min(2, b.status()))]++;
		}
		return out;
	}

	static void asTraffic(DeskScreen s, DeskData d) {
		s.header("AIRSPACE", "TRAFFIC", d);
		double[] cols = {6, 30, 48, 100, W - 6};
		String[] align = {"left", "left", "left", "left", "right"};
		s.rows(6, 17, List.<String[]>of(new String[] {"ID", "TIER", "STATE", "ROUTE", "HOME IN"}), List.<int[]>of(new int[] {DIM, DIM, DIM, DIM, DIM}),
				cols, align, 0.62, 9.5);
		List<String[]> rows = new ArrayList<>();
		List<int[]> colours = new ArrayList<>();
		for (DeskData.Flight f : d.flights) {
			if (rows.size() >= 10) {
				break;
			}
			rows.add(new String[] {"#" + two(f.id()), "T" + f.tier(), f.state(),
					f.returning() ? "HOME" : f.workBearing() + " " + Math.round(f.workDist()) + " M", f.eta() + " S"});
			colours.add(new int[] {DeskScreen.tier(f.tier()), TXT, f.returning() ? GREEN : AMBER, TXT, WHITE});
		}
		s.rows(6, 28, rows, colours, cols, align, 0.72, 10.5);
		for (int i = 0; i < rows.size(); i++) {
			DeskData.Flight f = d.flights.get(i);
			s.bar(100, 28 + i * 10.5 + 7.2, 60, 1.2, f.progress(), DeskScreen.tier(f.tier()));
		}
		if (rows.isEmpty()) {
			s.text(W / 2, 60, "NO TRAFFIC: EVERY DRONE IS DOCKED", DIM, 0.8, "center");
		}
		int charging = 0;
		for (DeskData.Flight f : d.flights) {
			charging += f.state().equals("CHARGE") ? 1 : 0;
		}
		s.footer(d.flights.size() + " TRACKS  " + d.air + " AIRBORNE  " + charging + " CHARGING", d.flights.size() > 10 ? "10 SHOWN  1/3" : "1/3", true);
	}

	static void asRadar(DeskScreen s, DeskData d) {
		int range = range(d);
		s.header("AIRSPACE", "RADAR  " + range + " M", d);
		radar(s, W / 2, 77, 57, d, range, true);
		s.text(W / 2, 13.5, "N", DIM, 0.7, "center");
		s.text(W / 2 + 64, 74, "E", DIM, 0.7, "center");
		s.text(W / 2, 136, "S", DIM, 0.7, "center");
		s.text(W / 2 - 64, 74, "W", DIM, 0.7, "center");
		List<Integer> seen = new ArrayList<>();
		for (DeskData.Flight f : d.flights) {
			if (!seen.contains(f.tier())) {
				seen.add(f.tier());
			}
		}
		seen.sort(Integer::compare);
		for (int i = 0; i < Math.min(8, seen.size()); i++) {
			s.dot(8, 21.5 + i * 9, 1.6, DeskScreen.tier(seen.get(i)));
			s.text(12, 18 + i * 9, "T" + seen.get(i), DeskScreen.tier(seen.get(i)), 0.75);
		}
		s.text(W - 6, 18, "TOWER", AMBER, 0.7, "right");
		s.text(W - 6, 27, d.air + " AIRBORNE", TXT, 0.62, "right");
		s.footer("SWEEP 2.0 S", "2/3", true);
	}

	static void asSectors(DeskScreen s, DeskData d) {
		s.header("AIRSPACE", "SECTORS", d);
		int[] counts = new int[8];
		double[] far = new double[8];
		int top = 4;
		for (DeskData.Flight f : d.flights) {
			int k = DeskData.sector(f.x(), f.z());
			counts[k]++;
			far[k] = Math.max(far[k], f.dist());
			top = Math.max(top, counts[k]);
		}
		int range = range(d);
		for (int i = 0; i < 8; i++) {
			double y = 18 + i * 14.5;
			s.text(6, y + 1, DeskData.BEARINGS[i], WHITE, 0.9);
			s.text(26, y, "DRONES", DIM, 0.55);
			s.bar(26, y + 6, 90, 4, (double) counts[i] / top, CYAN);
			s.text(122, y + 5, String.valueOf(counts[i]), CYAN, 0.75);
			s.text(140, y, "FARTHEST", DIM, 0.55);
			s.bar(140, y + 6, 70, 4, far[i] / range, AMBER);
			s.text(W - 6, y + 5, far[i] > 0 ? Math.round(far[i]) + " M" : "-", AMBER, 0.68, "right");
		}
		s.footer("BY BEARING FROM THE CORE", "3/3", true);
	}

	static void flRoster(DeskScreen s, DeskData d) {
		s.header("FLEET", "TIER ROSTER", d);
		s.text(W - 6, 15.5, "HOME+OUT/DOCKS", DIM, 0.55, "right");
		tierRows(s, 6, 24, W - 12, d);
		s.footer(d.maxTier + " OF 9 TIERS UNLOCKED", "1/3", true);
	}

	static void flDrones(DeskScreen s, DeskData d) {
		s.header("FLEET", "DRONES", d);
		s.text(6, 16, "IN FLIGHT", DIM, 0.62);
		List<String[]> rows = new ArrayList<>();
		List<int[]> colours = new ArrayList<>();
		for (DeskData.Flight f : d.flights) {
			if (rows.size() >= 7) {
				break;
			}
			rows.add(new String[] {"#" + two(f.id()), "T" + f.tier(), f.state(), pct(f.progress())});
			colours.add(new int[] {DeskScreen.tier(f.tier()), TXT, f.returning() ? GREEN : AMBER, WHITE});
		}
		s.rows(6, 25, rows, colours, new double[] {6, 30, 48, W - 6}, new String[] {"left", "left", "left", "right"}, 0.7, 9.5);
		for (int i = 0; i < rows.size(); i++) {
			DeskData.Flight f = d.flights.get(i);
			s.bar(120, 25 + i * 9.5 + 2, 90, 3, f.progress(), DeskScreen.tier(f.tier()));
		}
		if (rows.isEmpty()) {
			s.text(6, 26, "NONE", DIM, 0.7);
		}
		s.text(6, 96, "DOCKED", DIM, 0.62);
		int[] pillar = new int[3];
		for (DeskData.Bay b : d.bays) {
			if (b.status() == 1 && b.pillar() >= 0 && b.pillar() < 3) {
				pillar[b.pillar()]++;
			}
		}
		String[] names = {"PILLAR A", "PILLAR B", "PILLAR C", "ROOF PADS"};
		int[] got = {pillar[0], pillar[1], pillar[2], d.roofDocked};
		for (int k = 0; k < 4; k++) {
			double x = 6 + k * 59;
			s.rect(x, 105, 56, 20, TILE);
			s.text(x + 5, 108, names[k], TXT, 0.62);
			s.text(x + 51, 115, String.valueOf(got[k]), got[k] > 0 ? GREEN : DIM, 1.0, "right");
		}
		s.footer(d.fleet + " DRONES  " + d.air + " FLYING  " + d.docked + " DOCKED", "2/3", true);
	}

	static void flHangars(DeskScreen s, DeskData d) {
		s.header("FLEET", "HANGARS", d);
		int[] st = bayCounts(d);
		if (d.bays.isEmpty()) {
			s.text(W / 2, 54, "NO HANGAR BAYS YET", WHITE, 1.1, "center");
			s.text(W / 2, 70, "TIER 1 AND 2 DRONES DOCK ON THE ROOF PADS", DIM, 0.68, "center");
			s.text(W / 2, 80, "THE HANGAR ROWS COME WITH " + d.hangarTier, DIM, 0.68, "center");
		}
		for (int k = 0; k < 3; k++) {
			List<DeskData.Bay> mine = new ArrayList<>();
			for (DeskData.Bay b : d.bays) {
				if (b.pillar() == k) {
					mine.add(b);
				}
			}
			if (mine.isEmpty()) {
				continue;
			}
			double x = 8 + k * 80;
			s.text(x, 17, "PILLAR " + "ABC".charAt(k), WHITE, 0.8);
			int cols = mine.size() <= 8 ? 2 : 3, rows = (mine.size() + cols - 1) / cols;
			double cw = cols == 2 ? 30 : 21, gap = cols == 2 ? 34 : 23.5, ch = Math.min(20, 104.0 / rows - 4);
			for (int i = 0; i < mine.size(); i++) {
				DeskData.Bay b = mine.get(i);
				double yy = 28 + (i / cols) * (ch + 4), xx = x + (i % cols) * gap;
				int c = b.status() == 1 ? GREEN : b.status() == 2 ? AMBER : rgb(30, 40, 44);
				s.rect(xx, yy, cw, ch, TILE);
				s.frame(xx, yy, cw, ch, c);
				s.text(xx + cw / 2, yy + 3, b.name(), WHITE, 0.72, "center");
				if (ch >= 16) {
					s.text(xx + cw / 2, yy + 12, b.status() == 1 ? "HOME" : b.status() == 2 ? "OUT" : "EMPTY", c, 0.56, "center");
				}
			}
		}
		s.footer(st[1] + " HOME  " + st[2] + " OUT  " + st[0] + " EMPTY", "3/3", true);
	}

	static void dsReady(DeskScreen s, DeskData d) {
		s.header("DISPATCH", "READY TO LAUNCH", d);
		List<Integer> tiers = tiers(d, 2);
		for (int i = 0; i < tiers.size(); i++) {
			int t = tiers.get(i);
			double y = 18 + i * 34;
			int c = DeskScreen.tier(t);
			int cap = Math.max(1, Math.max(d.cap[t], d.home[t] + d.out[t]));
			panel(s, y - 2, 30, c);
			s.text(10, y, "T" + t, c, 1.3);
			s.text(34, y + 1, DeskData.tierName(t), TXT, 0.75);
			s.text(W - 8, y, d.home[t] + " READY", GREEN, 1.0, "right");
			s.text(34, y + 11, d.out[t] + " OUT  " + Math.max(0, d.cap[t] - d.home[t] - d.out[t]) + " DOCKS FREE", DIM, 0.62);
			s.bar(34, y + 20, 170, 3, (double) d.home[t] / cap, c);
		}
		s.kv(6, 92, W - 12, "LAUNCHES (LAST 60 MIN)", String.valueOf(d.rec.lastHour(d.rec.launches, d.gameTime)), WHITE, 0.9);
		s.kv(6, 106, W - 12, "LANDINGS (LAST 60 MIN)", String.valueOf(d.rec.lastHour(d.rec.landings, d.gameTime)), WHITE, 0.9);
		s.kv(6, 120, W - 12, "OPEN JOBS WAITING", n(d.targets), d.targets > 0 ? AMBER : GREEN, 0.9);
		s.footer("DRONES LAUNCH THEMSELVES FOR JOBS", "1/3", true);
	}

	static void dsPads(DeskScreen s, DeskData d) {
		s.header("DISPATCH", "PADS + PICKUP", d);
		s.text(6, 16, "ROOF PADS  " + d.pads.size(), DIM, 0.62);
		int n = Math.min(12, d.pads.size()), cols = n <= 6 ? 3 : 4, rows = Math.max(1, (n + cols - 1) / cols);
		double pitchX = cols == 3 ? 60 : 45, pitchY = rows <= 2 ? 46 : 120.0 / rows, bw = cols == 3 ? 44 : 38, bh = Math.min(36, pitchY - 8);
		int used = 0;
		for (int i = 0; i < n; i++) {
			double cx = 34 + (i % cols) * pitchX - (cols == 3 ? 0 : 6), cy = 26 + bh / 2 + (i / cols) * pitchY;
			boolean occ = d.pads.get(i)[2] > 0;
			used += occ ? 1 : 0;
			s.rect(cx - bw / 2, cy - bh / 2, bw, bh, TILE);
			s.frame(cx - bw / 2, cy - bh / 2, bw, bh, occ ? rgb(60, 66, 40) : GRID);
			s.ring(cx, cy - 2, Math.min(9, bh / 2 - 4), occ ? AMBER : rgb(60, 70, 72), 0.6);
			s.text(cx, cy - 5.5, "H", occ ? AMBER : rgb(60, 70, 72), 1.0, "center");
			s.text(cx - bw / 2 + 3, cy - bh / 2 + 3, "P" + (i + 1), WHITE, 0.6);
			if (bh >= 30) {
				s.text(cx, cy + 12.5, occ ? (int) d.pads.get(i)[2] + " DOCKED" : "CLEAR", occ ? GREEN : DIM, 0.5, "center");
			}
		}
		if (n == 0) {
			s.text(94, 60, "NO OPEN PADS", DIM, 0.8, "center");
		}
		boolean pick = !d.pickups.isEmpty();
		s.rect(194, 30, 46, 50, TILE);
		s.frame(194, 30, 46, 50, pick ? CYAN : RED);
		s.text(217, 34, d.pickups.size() > 1 ? "PICKUPS" : "PICKUP", CYAN, 0.65, "center");
		s.text(217, 48, pick ? "ONLINE" : "NONE", pick ? GREEN : RED, 0.7, "center");
		s.text(217, 62, d.cargo == null ? "-" : d.cargo.occupiedSlots() + "/" + d.cargo.totalSlots(), WHITE, 0.8, "center");
		s.text(217, 71, "SLOTS", DIM, 0.5, "center");
		s.footer(used + " OF " + n + " PADS IN USE", "2/3", true);
	}

	static void dsRecovery(DeskScreen s, DeskData d) {
		s.header("DISPATCH", "RECOVERY", d);
		List<DeskData.Flight> inbound = new ArrayList<>(d.flights);
		inbound.removeIf(f -> f.state().equals("CHARGE"));
		inbound.sort(Comparator.comparingInt((DeskData.Flight f) -> f.returning() ? 0 : 1).thenComparingInt(DeskData.Flight::eta));
		int most = 1;
		for (DeskData.Flight f : inbound) {
			most = Math.max(most, f.eta());
		}
		for (int i = 0; i < Math.min(5, inbound.size()); i++) {
			DeskData.Flight f = inbound.get(i);
			double y = 18 + i * 24;
			s.rect(4, y - 2, W - 8, 21, ROW);
			s.text(8, y, "#" + two(f.id()), DeskScreen.tier(f.tier()), 1.0);
			s.text(36, y + 1, "T" + f.tier() + "  " + Math.round(f.dist()) + " M OUT  " + f.state(), TXT, 0.7);
			s.text(W - 8, y, f.eta() + " S", WHITE, 1.0, "right");
			s.text(36, y + 10, "TO " + f.home(), DIM, 0.6);
			s.bar(120, y + 11, 70, 3, 1 - (double) f.eta() / most, f.returning() ? GREEN : AMBER);
		}
		if (inbound.isEmpty()) {
			s.text(W / 2, 60, "NOTHING IN THE AIR", DIM, 0.9, "center");
		}
		DeskData.Flight next = inbound.stream().filter(DeskData.Flight::returning).findFirst().orElse(null);
		s.footer(next == null ? "NO DRONES INBOUND" : "NEXT LANDING " + next.eta() + " S  " + next.home(), "3/3", true);
	}

	static void msJobs(DeskScreen s, DeskData d) {
		s.header("MISSIONS", "JOBS", d);
		double reach = d.targets <= 0 ? 1 : 1 - (double) d.covered / d.targets;
		s.gauge(52, 60, 34, reach, CYAN, "REACHABLE");
		s.kv(104, 22, 134, "OPEN TARGETS", n(d.targets), WHITE, 1.0);
		s.kv(104, 36, 134, "NO WAY IN", n(d.covered), d.covered > 0 ? AMBER : GREEN, 1.0);
		s.kv(104, 50, 134, "DRONES ON JOBS", String.valueOf(d.active), CYAN, 1.0);
		s.kv(104, 64, 134, "DONE (60 MIN)", String.valueOf(d.rec.lastHour(d.rec.landings, d.gameTime)), TXT, 1.0);
		s.kv(104, 78, 134, "DRONES FREE", String.valueOf(d.docked), GREEN, 1.0);
		boolean shortOf = !d.missing.isEmpty();
		panel(s, 102, 22, shortOf ? AMBER : GREEN);
		s.text(10, 105, shortOf ? "MATERIALS  " + d.missing.size() + (d.missing.size() == 1 ? " KIND SHORT" : " KINDS SHORT") : "MATERIALS  ALL IN STOCK",
				shortOf ? AMBER : GREEN, 0.8);
		s.text(10, 114.5, shortOf ? "SEE MATERIALS NEEDED" : "NOTHING HOLDING THE JOBS UP", DIM, 0.6);
		s.footer("BUILD RADIUS " + io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity.BUILD_RADIUS_CHUNKS + " CHUNKS", "1/3", true);
	}

	static void msMaterials(DeskScreen s, DeskData d) {
		s.header("MISSIONS", "MATERIALS NEEDED", d);
		for (int i = 0; i < Math.min(3, d.missing.size()); i++) {
			DeskData.Item m = d.missing.get(i);
			int have = 0;
			for (DeskData.Item st : d.stock) {
				if (st.id().equals(m.id())) {
					have = st.count();
				}
			}
			double y = 18 + i * 22;
			panel(s, y - 2, 19, AMBER);
			s.text(10, y, s.fit(m.name(), 180, 0.9), WHITE, 0.9);
			s.text(W - 8, y, n(m.count()), AMBER, 1.0, "right");
			s.text(10, y + 9.5, "SHORT FOR OPEN JOBS  PACKAGER HAS " + n(have), DIM, 0.58);
		}
		if (d.missing.isEmpty()) {
			s.rect(4, 18, W - 8, 40, rgb(13, 24, 18));
			s.text(W / 2, 28, "NOTHING SHORT", GREEN, 1.2, "center");
			s.text(W / 2, 44, "EVERY OPEN JOB HAS ITS MATERIALS", TXT, 0.65, "center");
		} else if (d.missing.size() > 3) {
			s.text(W - 8, 84, "+" + (d.missing.size() - 3) + " MORE", AMBER, 0.62, "right");
		}
		s.text(6, 90, "WHERE TO GET THEM", DIM, 0.62);
		s.text(6, 99, "PUT THEM IN THE PACKAGER OR THE CORE'S STORE", TXT, 0.62);
		s.footer(d.missing.size() + (d.missing.size() == 1 ? " ITEM SHORT" : " ITEMS SHORT"), "2/3", d.missing.isEmpty());
	}

	static void msProgress(DeskScreen s, DeskData d) {
		s.header("MISSIONS", "OPEN TARGETS  60 MIN", d);
		boolean[] have = new boolean[60];
		float[] vals = trace(d.rec.open, d.rec.openSlot, d.gameTime / 1200, have);
		float lo = Float.MAX_VALUE, hi = 0;
		for (int i = 0; i < 60; i++) {
			if (have[i]) {
				lo = Math.min(lo, vals[i]);
				hi = Math.max(hi, vals[i]);
			}
		}
		if (lo > hi) {
			lo = 0;
			hi = 1;
		}
		double pad = Math.max(4, (hi - lo) * 0.2);
		spark(s, 8, 22, W - 16, 90, vals, have, CYAN, Math.max(0, lo - pad), hi + pad);
		s.text(8, 116, "OPEN TARGETS  NOW " + n(d.targets), CYAN, 0.65);
		s.text(W - 8, 116, d.rec.lastHour(d.rec.landings, d.gameTime) + " DONE (60 MIN)", WHITE, 0.65, "right");
		s.text(8, 125, "-60 MIN", DIM, 0.55);
		s.text(W - 8, 125, "NOW", DIM, 0.55, "right");
		s.footer("RECORDED HERE SINCE " + d.rec.since, "3/3", true);
	}

	static void tbTiers(DeskScreen s, DeskData d) {
		s.header("TOWER", "TIERS", d);
		for (int i = 0; i < 9; i++) {
			int t = i + 1;
			double y = 16 + i * 13.2;
			boolean built = t <= d.tier, now = t == d.building;
			int c = built ? GREEN : now ? AMBER : LOCKED;
			s.rect(6, y, 16, 10, built || now ? c : rgb(20, 26, 28));
			s.text(14, y + 1.5, String.valueOf(t), built || now ? BG : DIM, 0.75, "center");
			s.text(28, y + 1.5, s.fit(name(d, t), 118, 0.68), built || now ? WHITE : DIM, 0.68);
			if (now) {
				s.bar(150, y + 3, 64, 4, d.progress, AMBER);
				s.text(W - 6, y + 1.5, pct(d.progress), AMBER, 0.68, "right");
			} else {
				s.text(W - 6, y + 1.5, built ? "BUILT" : "LOCKED", c, 0.6, "right");
			}
		}
		s.footer(d.building > 0 ? "TIER " + d.building + " OF 9 BUILDING" : d.tier + " OF 9 TIERS BUILT", "1/3", true);
	}

	static void tbCurrent(DeskScreen s, DeskData d) {
		if (d.building <= 0 || d.buildTotal <= 0) {
			int next = Math.min(9, d.tier + 1);
			s.header("TOWER", d.tier >= 9 ? "COMPLETE" : "NEXT TIER", d);
			s.text(6, 18, d.tier >= 9 ? "ALL NINE TIERS BUILT" : "TIER " + next + "  " + name(d, next), WHITE, 1.1);
			s.gauge(52, 72, 30, d.tier >= 9 ? 1 : 0, d.tier >= 9 ? GREEN : AMBER, d.tier >= 9 ? "BUILT" : "WAITING");
			s.kv(100, 40, 138, "TIERS BUILT", d.tier + " OF 9", WHITE);
			s.kv(100, 52, 138, "DOCKS NOW", String.valueOf(d.max), TXT);
			if (d.tier < 9) {
				s.kv(100, 64, 138, "DOCKS AFTER", String.valueOf(d.tierCaps[next - 1]), DeskScreen.tier(next));
				s.text(100, 88, "UPGRADE IT AT THE TOWER CORE", DIM, 0.62);
			}
			s.kv(100, 76, 138, "IN THE CORE'S STORE", n(d.stored) + " ITEMS", GREEN);
			s.footer(d.tier >= 9 ? "THE CROWN IS ON" : "NOTHING BUILDING", "2/3", true);
			return;
		}
		s.header("TOWER", "BUILDING TIER " + d.building, d);
		s.text(6, 18, s.fit(name(d, d.building), W - 12, 1.1), WHITE, 1.1);
		s.gauge(52, 72, 30, d.progress, AMBER, "PLACED");
		int placed = Math.round(d.progress * d.buildTotal);
		s.kv(100, 40, 138, "BLOCKS PLACED", n(placed), WHITE);
		s.kv(100, 52, 138, "BLOCKS LEFT", n(d.buildTotal - placed), TXT);
		s.kv(100, 64, 138, "DRONES ON JOBS", String.valueOf(d.active), AMBER);
		s.kv(100, 76, 138, "IN THE CORE'S STORE", n(d.stored), GREEN);
		s.kv(100, 88, 138, "NEW DOCKS", d.newDocks + (d.newDockTier > 0 ? "  T" + d.newDockTier : ""), d.newDockTier > 0 ? DeskScreen.tier(d.newDockTier) : DIM);
		double rate = d.rec.buildRate();
		s.text(6, 112, "ETA AT THIS RATE", DIM, 0.62);
		s.text(W - 6, 112, rate > 0 ? DeskData.minutes((long) ((1 - d.progress) / rate / 20)) : "MEASURING", WHITE, 0.9, "right");
		s.footer(d.missing.isEmpty() ? "CORE STORE OK" : "SOME MATERIALS SHORT", "2/3", d.missing.isEmpty());
	}

	static void tbCapacity(DeskScreen s, DeskData d) {
		s.header("TOWER", "CAPACITY", d);
		double base = 120;
		int top = 1;
		for (int c : d.tierCaps) {
			top = Math.max(top, c);
		}
		for (int i = 0; i < 9; i++) {
			double x = 10 + i * 25.5, h = 96.0 * d.tierCaps[i] / top, prev = 96.0 * (i > 0 ? d.tierCaps[i - 1] : 0) / top;
			boolean built = i + 1 <= d.tier;
			int c = built ? DeskScreen.tier(i + 1) : i + 1 == d.building ? dark(AMBER, 2) : rgb(40, 50, 52);
			s.rect(x, base - h, 18, Math.max(0, h - prev), c);
			s.rect(x, base - prev, 18, prev, rgb(24, 32, 34));
			s.text(x + 9, base + 3, "T" + (i + 1), built ? c : DIM, 0.6, "center");
			s.text(x + 9, base - h - 8, String.valueOf(d.tierCaps[i]), built ? TXT : DIM, 0.55, "center");
		}
		s.footer(d.max + " DOCKS NOW  " + d.tierCaps[8] + " AT THE CROWN", "3/3", true);
	}

	// ================================================================ EAST: power, supply, docks, maintenance, data hall, comms

	static void pwStorage(DeskScreen s, DeskData d) {
		s.header("POWER", "STORAGE", d);
		double f = d.capacity <= 0 ? 0 : (double) d.energy / d.capacity;
		s.rect(14, 24, 60, 100, TILE);
		s.frame(14, 24, 60, 100, GRID);
		s.rect(34, 20, 20, 4, GRID);
		for (int k = 0; k < 10; k++) {
			if ((k + 0.5) / 10 < f) {
				s.rect(18, 120 - (k + 1) * 9.6 + 1, 52, 8.2, f > 0.3 ? GREEN : AMBER);
			}
		}
		s.text(44, 30, pct(f), WHITE, 1.4, "center");
		long reserve = d.reserveSeconds();
		s.kv(86, 26, 152, "STORED", String.format(Locale.ROOT, "%.2f M JE", d.energy / 1e6), WHITE, 1.0);
		s.kv(86, 42, 152, "CAPACITY", String.format(Locale.ROOT, "%.2f M JE", d.capacity / 1e6), TXT, 1.0);
		s.kv(86, 58, 152, "DRAW", n(d.draw) + " JE/T", AMBER, 1.0);
		s.kv(86, 74, 152, "RESERVE", reserve > 0 ? DeskData.minutes(reserve) : "-", reserve > 600 || reserve == 0 ? GREEN : AMBER, 1.0);
		s.kv(86, 90, 152, "LOW POWER", d.lowPower ? "YES" : "NO", d.lowPower ? RED : GREEN, 1.0);
		s.footer("TERMINAL INPUT " + n(DroneTerminalBlockEntity.ENERGY_INPUT) + " JE/T", "1/3", !d.lowPower);
	}

	static void pwTrend(DeskScreen s, DeskData d) {
		s.header("POWER", "DRAW  10 MIN", d);
		boolean[] have = new boolean[60];
		float[] vals = trace(d.rec.draw, d.rec.drawSlot, d.gameTime / 200, have);
		float peak = 0, sum = 0;
		int count = 0;
		for (int i = 0; i < 60; i++) {
			if (have[i]) {
				peak = Math.max(peak, vals[i]);
				sum += vals[i];
				count++;
			}
		}
		spark(s, 8, 22, W - 16, 86, vals, have, AMBER, 0, Math.max(100, peak * 1.25));
		s.text(8, 112, "JE/T", DIM, 0.6);
		s.text(W - 8, 112, "NOW " + n(d.draw), WHITE, 0.75, "right");
		s.text(8, 122, "PEAK " + n((long) peak) + "   MEAN " + n(count == 0 ? 0 : (long) (sum / count)), TXT, 0.62);
		s.footer("SAMPLED EVERY 10 S  SINCE " + d.rec.since, "2/3", true);
	}

	static void pwLoads(DeskScreen s, DeskData d) {
		s.header("POWER", "LOADS", d);
		double rated = DroneTerminalBlockEntity.ENERGY_INPUT;
		Object[][] rows = {{"FLEET WORKING", d.work, AMBER}, {"FLEET STANDING BY", d.standby, CYAN}, {"DRAWING NOW", d.draw, WHITE}};
		for (int i = 0; i < rows.length; i++) {
			double y = 20 + i * 20;
			long v = (long) rows[i][1];
			int c = (int) rows[i][2];
			s.text(6, y, (String) rows[i][0], TXT, 0.75);
			s.text(W - 6, y, n(v) + " JE/T", c, 0.8, "right");
			s.bar(6, y + 9, W - 12, 4, v / rated, c);
		}
		s.text(6, 104, "LOAD", DIM, 0.62);
		s.bar(6, 112, W - 12, 6, d.load / 100.0, d.load > 100 ? RED : AMBER);
		s.text(W - 6, 121, d.load + "% OF THE TERMINAL'S INPUT", WHITE, 0.66, "right");
		s.footer(d.lowPower ? "LOW POWER" : "NO SHEDDING", "3/3", !d.lowPower);
	}

	static void spPackager(DeskScreen s, DeskData d) {
		s.header("SUPPLY", "PACKAGER", d);
		int items = d.cargo == null ? 0 : d.cargo.items(), used = d.cargo == null ? 0 : d.cargo.occupiedSlots(),
				slots = d.cargo == null ? 0 : d.cargo.totalSlots();
		s.gauge(52, 62, 32, slots <= 0 ? 0 : (double) used / slots, CYAN, "SLOTS");
		s.kv(100, 24, 138, "ITEMS", d.cargo != null && d.cargo.unlimited() ? "TEST STORE" : n(items), WHITE, 1.0);
		s.kv(100, 40, 138, "SLOTS", used + " / " + slots, TXT, 1.0);
		s.kv(100, 56, 138, "STATUS", d.packager ? "ONLINE" : "NOT LINKED", d.packager ? GREEN : RED, 1.0);
		s.kv(100, 72, 138, "KINDS SHORT", String.valueOf(d.missing.size()), d.missing.isEmpty() ? GREEN : AMBER, 0.85);
		s.text(6, 104, "SLOTS", DIM, 0.62);
		int shown = Math.min(81, slots);
		for (int k = 0; k < shown; k++) {
			s.rect(6 + (k % 27) * 8.6, 112 + (k / 27) * 7, 7.4, 5.4, k < used ? CYAN : GRID);
		}
		s.footer(d.packagerAt == null ? "NO PACKAGER" : "PACKAGER AT " + d.packagerAt, "1/3", d.packager);
	}

	static void spPickups(DeskScreen s, DeskData d) {
		s.header("SUPPLY", "PICKUPS", d);
		for (int i = 0; i < Math.min(4, d.pickups.size()); i++) {
			int[] p = d.pickups.get(i);
			double yy = 18 + i * 28;
			panel(s, yy - 2, 25, GREEN);
			s.text(10, yy, "SUPPLY PICKUP " + (i + 1), WHITE, 0.9);
			s.text(10, yy + 10, "X " + p[0] + "  Y " + p[1] + "  Z " + p[2], DIM, 0.62);
			s.text(W - 8, yy, "ONLINE", GREEN, 0.8, "right");
			s.text(W - 8, yy + 10, Math.round(Math.hypot(p[0] + 1.5 - d.cx, p[2] + 1.5 - d.cz)) + " M FROM THE CORE", TXT, 0.62, "right");
		}
		if (d.pickups.isEmpty()) {
			panel(s, 16, 25, RED);
			s.text(10, 18, "NO SUPPLY PICKUP IN USE", RED, 0.9);
			s.text(10, 28, "THE DRONES FETCH EVERY CRATE FROM ONE", DIM, 0.62);
		}
		s.footer(d.pickups.size() + (d.pickups.size() == 1 ? " PICKUP" : " PICKUPS"), "2/3", !d.pickups.isEmpty());
	}

	static void spStock(DeskScreen s, DeskData d) {
		s.header("SUPPLY", "STOCK", d);
		int mx = 1;
		for (DeskData.Item it : d.stock) {
			mx = Math.max(mx, it.count());
		}
		for (int i = 0; i < Math.min(12, d.stock.size()); i++) {
			DeskData.Item it = d.stock.get(i);
			double x = 6 + (i / 6) * 120, y = 18 + (i % 6) * 20;
			s.text(x, y, s.fit(it.name(), 80, 0.6), TXT, 0.6);
			s.text(x + 114, y, n(it.count()), WHITE, 0.6, "right");
			s.bar(x, y + 7.5, 114, 3.5, Math.sqrt((double) it.count() / mx), CYAN);
		}
		if (d.stock.isEmpty()) {
			s.text(W / 2, 60, "THE STORE IS EMPTY", DIM, 0.9, "center");
		}
		s.footer(d.stock.size() + " KINDS TRACKED", "3/3", !d.stock.isEmpty());
	}

	static void dkDocks(DeskScreen s, DeskData d) {
		s.header("DOCKS", "OCCUPANCY", d);
		List<Integer> tiers = new ArrayList<>();
		int rowsNeeded = 0;
		for (int t = 1; t <= 10; t++) {
			if (d.cap[t] > 0) {
				tiers.add(t);
				rowsNeeded += (d.cap[t] + 11) / 12;
			}
		}
		double cellH = Math.min(12, Math.max(5, (112 - tiers.size() * 14.0) / Math.max(1, rowsNeeded)));
		double k = 0;
		int total = 0, home = 0, out = 0;
		for (int t : tiers) {
			s.text(6, 17 + k, "T" + t + " DOCKS", DeskScreen.tier(t), 0.75);
			int cap = d.cap[t];
			total += cap;
			home += Math.min(cap, d.home[t]);
			out += Math.min(cap, d.out[t]);
			for (int i = 0; i < cap; i++) {
				double x = 6 + (i % 12) * 19.5, y = 26 + k + (i / 12) * cellH;
				int st = i < d.home[t] ? 1 : i < d.home[t] + d.out[t] ? 2 : 0;
				s.rect(x, y, 17, cellH - 3, st == 1 ? rgb(30, 70, 44) : st == 2 ? rgb(80, 56, 20) : rgb(18, 24, 26));
				if (cellH >= 10) {
					s.text(x + 8.5, y + 1.8, two(i + 1), st == 1 ? GREEN : st == 2 ? AMBER : DIM, 0.52, "center");
				}
			}
			k += cellH * ((cap + 11) / 12) + 14;
		}
		if (tiers.isEmpty()) {
			s.text(W / 2, 60, "NO DOCKS YET", DIM, 0.9, "center");
		}
		s.footer(total + " DOCKS  " + home + " HOME  " + out + " OUT  " + Math.max(0, total - home - out) + " FREE", "1/3", true);
	}

	static void dkDoors(DeskScreen s, DeskData d) {
		s.header("DOCKS", "HANGAR DOORS", d);
		int open = 0;
		for (int k = 0; k < 3; k++) {
			List<DeskData.Bay> mine = new ArrayList<>();
			for (DeskData.Bay b : d.bays) {
				if (b.pillar() == k) {
					mine.add(b);
				}
			}
			double y = 20 + k * 38;
			s.text(6, y, "PILLAR " + "ABC".charAt(k), WHITE, 0.8);
			if (mine.isEmpty()) {
				s.text(60, y, "NO BAYS YET", DIM, 0.62);
				continue;
			}
			double pitch = Math.min(29.5, (W - 12.0) / mine.size()), bw = pitch - 3.5;
			for (int i = 0; i < mine.size(); i++) {
				DeskData.Bay b = mine.get(i);
				double x = 6 + i * pitch, o = Math.max(0, Math.min(1, b.open()));
				int c = o > 0.05 ? AMBER : b.status() == 1 ? GREEN : rgb(40, 50, 52);
				open += o > 0.05 ? 1 : 0;
				s.rect(x, y + 10, bw, 18, TILE);
				// the shutter: rolled up as far as the door is open
				s.rect(x + 2, y + 12, bw - 4, 14 * (1 - o), dark(c, 2));
				s.text(x + bw / 2, y + 30, o > 0.95 ? "OPEN" : o > 0.05 ? "MOVING" : "SHUT", c, 0.5, "center");
			}
		}
		s.footer(open + (open == 1 ? " DOOR OPEN FOR A FLIGHT" : " DOORS OPEN FOR FLIGHTS"), "2/3", true);
	}

	static void dkTraffic(DeskScreen s, DeskData d) {
		s.header("DOCKS", "PAD TRAFFIC  60 MIN", d);
		int[] up = d.rec.bins(d.rec.launches, d.gameTime, 12), down = d.rec.bins(d.rec.landings, d.gameTime, 12);
		int top = 4, moves = 0;
		for (int i = 0; i < 12; i++) {
			top = Math.max(top, Math.max(up[i], down[i]));
			moves += up[i] + down[i];
		}
		for (int i = 0; i < 12; i++) {
			double x = 10 + i * 19;
			s.rect(x, 110 - 88.0 * up[i] / top, 7, 88.0 * up[i] / top, AMBER);
			s.rect(x + 8, 110 - 88.0 * down[i] / top, 7, 88.0 * down[i] / top, GREEN);
			s.text(x + 7.5, 113, "-" + (60 - i * 5), DIM, 0.45, "center");
		}
		s.text(6, 17, "LAUNCHES", AMBER, 0.65);
		s.text(60, 17, "LANDINGS", GREEN, 0.65);
		s.text(W - 6, 17, moves + " MOVES", WHITE, 0.7, "right");
		s.footer("5-MIN BINS  RECORDED HERE SINCE " + d.rec.since, "3/3", true);
	}

	static void mtBay(DeskScreen s, DeskData d) {
		s.header("MAINT", "SERVICE BAY", d);
		s.rect(6, 18, 110, 96, rgb(12, 18, 20));
		s.frame(6, 18, 110, 96, GRID);
		int standTier = d.standTier;
		s.rect(41, 46, 40, 40, rgb(40, 50, 52));
		for (double[] fan : new double[][] {{38, 43}, {84, 43}, {38, 89}, {84, 89}}) {
			s.ring(fan[0], fan[1], 9, DeskScreen.tier(standTier), 0.8);
		}
		s.text(61, 20, s.fit("T" + standTier + " " + DeskData.tierName(standTier), 104, 0.6), DeskScreen.tier(standTier), 0.6, "center");
		if (d.armTime == null) {
			s.text(124, 40, "NO SERVICE ARM", DIM, 0.8);
			s.footer("NO ARM IN THIS BAY", "1/3", false);
			return;
		}
		double t = d.armTime;
		boolean held = d.armHeld;
		double c = ((t % ServiceArm.LENGTH) + ServiceArm.LENGTH) % ServiceArm.LENGTH;
		int i = ServiceArm.CYCLE.length - 1;
		while (i > 0 && ServiceArm.ARRIVE[i] > c) {
			i--;
		}
		ServiceArm.Stop stop = ServiceArm.CYCLE[i], next = ServiceArm.CYCLE[(i + 1) % ServiceArm.CYCLE.length];
		boolean holding = c - ServiceArm.ARRIVE[i] < stop.hold();
		String doing = holding ? (stop.spin() ? "DRIVING THE NUT" : stop.light() ? "CAMERA PASS" : stop.name().toUpperCase(Locale.ROOT))
				: "TO " + next.name().toUpperCase(Locale.ROOT);
		ServiceArm.Pose pose = ServiceArm.pose(t);
		Vector3f tip = ServiceArm.tip(new double[] {pose.j1(), pose.j2(), pose.fore(), pose.wrist()});
		// the arm seen from above, facing west to the drone (its front -x on the plan, its right north)
		double bx = 98, by = 66, k = 13.7;
		s.ring(bx, by, 26, rgb(60, 80, 84), 0.4);
		s.frame(bx - 11.6, by - 11.6, 23.3, 23.3, held ? RED : rgb(120, 100, 30));
		s.line(bx, by, bx - tip.x() * k, by - tip.z() * k, AMBER, 1.0);
		s.dot(bx - tip.x() * k, by - tip.z() * k, 1.4, pose.light() ? WHITE : AMBER);
		s.dot(bx, by, 2.5, AMBER);
		s.kv(124, 22, 114, "TASK", "FAN SERVICE", WHITE, 0.8);
		s.kv(124, 36, 114, "STEP", (i + 1) + " OF " + ServiceArm.CYCLE.length, AMBER, 0.8);
		s.kv(124, 50, 114, "CYCLE", String.format(Locale.ROOT, "%.1f / %.1f S", c, ServiceArm.LENGTH), TXT, 0.8);
		s.kv(124, 64, 114, "ARM", held ? "HELD" : "RUNNING", held ? AMBER : GREEN, 0.8);
		s.kv(124, 78, 114, "ZONE", held ? "OCCUPIED" : "CLEAR", held ? RED : GREEN, 0.8);
		s.text(124, 90, s.fit(doing, 114, 0.62), TXT, 0.62);
		s.bar(124, 100, 114, 4, c / ServiceArm.LENGTH, AMBER);
		s.footer(held ? "ARM HELD: SOMEONE IS IN ITS SQUARE" : "ARM STOPS IF SOMEONE STEPS IN", "1/3", !held);
	}

	static void mtParts(DeskScreen s, DeskData d) {
		s.header("MAINT", "PARTS + PACKS", d);
		if (d.packSeconds == null) {
			s.text(W / 2, 60, "NO BATTERY STATION", DIM, 0.9, "center");
			s.footer("NO PARTS WALL", "2/3", false);
			return;
		}
		double sec = d.packSeconds;
		s.text(6, 17, "SMART PACKS  BATTERY STATION", DIM, 0.62);
		int ready = 0;
		for (int i = 0; i < 8; i++) {
			int l = DronePartsShelvingRenderer.packLevel(i, sec);
			boolean ok = DronePartsShelvingRenderer.packReady(i, sec);
			ready += ok ? 1 : 0;
			double x = 6 + i * 29.5;
			s.rect(x, 26, 26, 30, TILE);
			for (int k = 0; k < 4; k++) {
				int c = DronePartsShelvingRenderer.gaugeColour(i, k, sec);
				s.rect(x + 3, 51 - k * 6, 20, 4.4, c == DronePartsShelvingRenderer.GREEN ? GREEN : OFF);
			}
			s.text(x + 13, 58, ok ? "READY" : "CHG", ok ? GREEN : AMBER, 0.5, "center");
		}
		s.text(6, 72, "QUAD BALANCE CHARGER  4 CHANNELS", DIM, 0.62);
		for (int i = 0; i < 4; i++) {
			double v = DronePartsShelvingRenderer.channelFill(i, sec), y = 81 + i * 11;
			s.text(6, y, "CH" + (i + 1), TXT, 0.65);
			s.bar(28, y + 1, 170, 5, v, v >= 1 ? GREEN : AMBER);
			s.text(W - 6, y, v >= 1 ? "DONE" : pct(v), WHITE, 0.65, "right");
		}
		s.footer(ready + " OF 8 PACKS READY", "2/3", ready >= 4);
	}

	static void mtHours(DeskScreen s, DeskData d) {
		s.header("MAINT", "FLIGHTS LOGGED", d);
		List<Integer> tiers = tiers(d, 2);
		int top = 10;
		for (int t : tiers) {
			top = Math.max(top, d.rec.byTier[t]);
		}
		for (int i = 0; i < tiers.size(); i++) {
			int t = tiers.get(i);
			double y = 22 + i * 30;
			s.text(6, y, "T" + t, DeskScreen.tier(t), 1.4);
			s.text(34, y + 2, DeskData.tierName(t), DIM, 0.62);
			s.text(W - 6, y, String.valueOf(d.rec.byTier[t]), WHITE, 1.4, "right");
			s.bar(34, y + 11, 160, 4, (double) d.rec.byTier[t] / top, DeskScreen.tier(t));
		}
		long secs = d.rec.flightTicks / 20;
		s.kv(6, 88, W - 12, "FLIGHT TIME", secs >= 60 ? DeskData.minutes(secs) : secs + " S", WHITE, 0.9);
		s.kv(6, 102, W - 12, "LONGEST REACH", d.rec.longestId < 0 ? "-" : "#" + two(d.rec.longestId) + "  " + Math.round(d.rec.longest) + " M", TXT, 0.9);
		s.kv(6, 116, W - 12, "SINCE", d.rec.since, DIM, 0.8);
		s.footer("COUNTED BY THESE DESKS", "3/3", true);
	}

	/** The data hall's UPS lamps, its load (kW) and input voltage, as the hall's own UPS screen (client/EdgTelemetry.srv). */
	private static double hallKw(DeskData d, boolean out) {
		return out ? 41.6 + 2.5 * d.serverLoad() + 0.4 * Math.sin(d.now / 200 * 1.9) : 0;
	}

	static void itUps(DeskScreen s, DeskData d) {
		s.header("DATA HALL", "UPS-1", d);
		DeskData.Hall hub = d.hall;
		if (hub == null) {
			s.text(W / 2, 60, "NO UPS FOUND", DIM, 0.9, "center");
			s.footer("NO DATA HALL", "1/3", false);
			return;
		}
		int bits = hub.bits();
		boolean out = (bits & ServerRoom.OUT_ON) != 0, batt = (bits & ServerRoom.ON_BATT) != 0, input = (bits & ServerRoom.IN_OK) != 0;
		double kw = hallKw(d, out), charge = hub.battery();
		s.text(6, 17, !out ? "UPS-1   OFF" : batt ? "UPS-1   ON BATTERY" : "UPS-1   ONLINE", !out ? RED : batt ? AMBER : GREEN, 1.1);
		s.kv(6, 32, 110, "INPUT", input ? String.format(Locale.ROOT, "%.0f V", 478 + 2 * Math.sin(d.now / 300 * 2.1)) : "0 V", WHITE);
		s.kv(6, 44, 110, "OUTPUT", out ? "480 V" : "0 V", WHITE);
		s.kv(6, 56, 110, "LOAD", out ? String.format(Locale.ROOT, "%.0f %%", kw / 70 * 100) : "0 %", AMBER);
		s.kv(6, 68, 110, "BATTERY", String.format(Locale.ROOT, "%.0f %%", charge * 100), charge > 0.5 ? GREEN : AMBER);
		s.kv(6, 80, 110, "RUNTIME", Math.round(charge * ServerRoom.RUNTIME / 60.0) + " MIN", TXT);
		String[] lamps = {"RECT", "INV", "BATT", "BYP", "LOAD"};
		boolean[] on = {input, out, batt, false, out};
		int[] colours = {GREEN, GREEN, AMBER, AMBER, GREEN};
		for (int i = 0; i < 5; i++) {
			double x = 132 + i * 22;
			s.dot(x + 6, 40, 4, on[i] ? colours[i] : rgb(40, 46, 48));
			s.text(x + 6, 48, lamps[i], DIM, 0.5, "center");
		}
		double amps = kw * 1000 / (Math.sqrt(3) * 480 * 0.96);
		s.text(132, 64, String.format(Locale.ROOT, "PDU  %.1f KW", kw), TXT, 0.75);
		s.text(132, 76, String.format(Locale.ROOT, "A %.0f  B %.0f  C %.0f A", amps * 1.02, amps * 0.95, amps * 1.04), DIM, 0.62);
		s.footer("FROM THE DATA HALL'S UPS", "1/3", out && !batt);
	}

	static void itRacks(DeskScreen s, DeskData d) {
		s.header("DATA HALL", "RACKS", d);
		if (d.racks.isEmpty()) {
			s.text(W / 2, 60, "NO RACKS FOUND", DIM, 0.9, "center");
			s.footer("NO DATA HALL", "2/3", false);
			return;
		}
		DeskData.Hall hub = d.hall;
		boolean powered = hub == null || (hub.bits() & ServerRoom.OUT_ON) != 0;
		double temp = hub == null ? ServerRoom.ROOM : hub.temp(), load = d.serverLoad();
		long blink = (long) (d.now / 6);
		for (int r = 0; r < 2; r++) {
			int j = 0;
			String rowName = "";
			for (int idx = 0; idx < d.racks.size(); idx++) {
				DeskData.Rack rack = d.racks.get(idx);
				if (rack.row() != r || j >= 8) {
					continue;
				}
				rowName = rowName.isEmpty() ? rack.label().substring(0, 1) : rowName;
				double x = 6 + j * 29.5, y = 28 + r * 58;
				s.rect(x, y, 26, 42, TILE);
				s.frame(x, y, 26, 42, GRID);
				s.text(x + 13, y + 2, rack.label(), WHITE, 0.55, "center");
				s.text(x + 13, y + 10, rack.kind(), DIM, 0.5, "center");
				for (int k = 0; k < 6; k++) {
					double hash = Math.abs(Math.sin((idx * 7 + k * 3 + 1) * 12.9898 + blink * 0.37 * (k + 1)) * 43758.5453) % 1;
					boolean on = powered && hash < 0.35 + 0.6 * load;
					s.rect(x + 4 + (k % 3) * 6.5, y + 20 + (k / 3) * 6, 4.5, 3, on ? GREEN : OFF);
				}
				s.text(x + 13, y + 34, String.format(Locale.ROOT, "%.0fC", temp + 1.5 + (idx * 7 % 5) * 0.4), TXT, 0.5, "center");
				j++;
			}
			if (j > 0) {
				s.text(6, 18 + r * 58, "ROW " + rowName, WHITE, 0.75);
			}
		}
		s.footer(d.racks.size() + " RACKS  " + (powered ? "ALL UP" : "UPS OUTPUT OFF"), "2/3", powered);
	}

	static void itCooling(DeskScreen s, DeskData d) {
		s.header("DATA HALL", "COOLING + FIRE", d);
		DeskData.Hall hub = d.hall;
		if (hub == null) {
			s.text(W / 2, 60, "NO DATA HALL", DIM, 0.9, "center");
			s.footer("NO DATA HALL", "3/3", false);
			return;
		}
		int bits = hub.bits();
		double temp = hub.temp();
		for (int i = 0; i < 2; i++) {
			boolean fed = (bits & (i == 0 ? ServerRoom.CRAC1_FED : ServerRoom.CRAC2_FED)) != 0;
			boolean run = (bits & (i == 0 ? ServerRoom.CRAC1_RUN : ServerRoom.CRAC2_RUN)) != 0;
			double ret = temp + 2.8, sup = run ? temp - 6.7 : temp, y = 18 + i * 28;
			s.text(6, y, "CRAC-" + (i + 1), WHITE, 0.85);
			s.text(70, y, run ? "COOLING" : fed ? "STANDBY" : "NO POWER", run ? GREEN : fed ? AMBER : RED, 0.7);
			s.text(6, y + 10, String.format(Locale.ROOT, "RETURN %.1f C   SUPPLY %.1f C", ret, sup), TXT, 0.68);
			s.bar(6, y + 19, 150, 3, (ret - 15) / 20, AMBER);
		}
		s.text(6, 78, String.format(Locale.ROOT, "ROOM %.1f C  SET %.1f C  RH %.0f%%", temp, ServerRoom.SETPOINT, 45 + Math.sin(d.now / 600 * 1.3) * 1.5),
				temp > ServerRoom.HIGH_TEMP ? RED : TXT, 0.7);
		int phase = hub.phase(), flags = hub.flags();
		boolean normal = phase == ServerRoom.NORMAL;
		int c = normal ? GREEN : phase == ServerRoom.DRILL ? AMBER : RED;
		s.rect(4, 92, W - 8, 34, normal ? ROW : dark(c, 5));
		s.rect(4, 92, 2.2, 34, c);
		if (normal) {
			s.text(10, 95, "FM-200  SYSTEM NORMAL", GREEN, 0.85);
		} else {
			String[] lines = ServerRoom.display(phase, flags, hub.since(), d.now, hub.releasedAt());
			s.text(10, 95, "FM-200  " + lines[0], c, 0.85);
			s.text(150, 95, lines[1], c, 0.6);
		}
		double psi = hub.psi() * (273 + temp) / (273 + ServerRoom.ROOM);
		int cylinders = d.cylinders;
		s.text(10, 106, String.format(Locale.ROOT, "AGENT %.0f PSIG  %d CYLINDERS", psi, cylinders), TXT, 0.68);
		s.text(10, 116, (flags & ServerRoom.NO_DETECTORS) != 0 ? "NO DETECTORS FOUND" : normal ? "DETECTORS ONLINE  NO ALARM" : "DETECTORS IN ALARM",
				(flags & ServerRoom.NO_DETECTORS) != 0 ? AMBER : DIM, 0.62);
		s.footer(phase == ServerRoom.RELEASED ? "AGENT RELEASED" : "CLEAN AGENT ARMED", "3/3", normal);
	}

	static void cmWeather(DeskScreen s, DeskData d) {
		s.header("COMMS", "WEATHER + SKY", d);
		boolean day = d.dayTime < 12000;
		if (d.raining) {
			s.dot(38, 40, 9, rgb(90, 104, 112));
			s.dot(50, 38, 11, rgb(110, 124, 132));
			s.rect(30, 40, 30, 8, rgb(100, 114, 122));
			for (int k = 0; k < 5; k++) {
				double x = 32 + k * 6, y = 52 + (d.now / 3 + k * 5) % 8;
				s.line(x, y, x - 1.5, y + 4, CYAN, 0.6);
			}
			if (d.thundering && (long) (d.now / 7) % 9 == 0) {
				s.line(new double[] {46, 50, 42, 58, 47, 58, 43, 66}, rgb(255, 240, 150), 1.0);
			}
		} else if (day) {
			s.dot(44, 42, 13, rgb(255, 214, 120));
		} else {
			s.dot(44, 42, 11, rgb(210, 220, 230));
			s.dot(49, 39, 9, BG);
		}
		String sky = d.thundering ? "STORM" : d.raining ? "RAIN" : day ? "CLEAR" : "CLEAR NIGHT";
		s.text(44, 60, sky, WHITE, 1.1, "center");
		s.text(44, 72, "RAIN " + Math.round(d.rain * 100) + "%", DIM, 0.62, "center");
		long left = day ? 12000 - d.dayTime : 24000 - d.dayTime;
		s.kv(96, 22, 142, "TIME", d.clock + "  DAY " + d.dayNumber, WHITE);
		s.kv(96, 36, 142, day ? "SUNSET" : "SUNRISE", "IN " + left / 1000 + " H " + left % 1000 * 60 / 1000 + " MIN", AMBER);
		s.kv(96, 50, 142, "MOON", DeskData.MOON[Math.floorMod(d.moon, 8)], TXT, 0.75);
		s.kv(96, 64, 142, "THUNDER", d.thundering ? "STORM OVERHEAD" : "NONE", d.thundering ? AMBER : GREEN);
		s.kv(96, 78, 142, "FLYING", "OK", GREEN);
		s.line(14, 134, W - 14, 134, GRID, 0.6);
		s.arc(W / 2, 134, 38, 180, 360, rgb(60, 80, 84), 0.6);
		double f = day ? d.dayTime / 12000.0 : (d.dayTime - 12000) / 12000.0;
		double a = Math.toRadians(180 + 180 * f);
		s.dot(W / 2 + Math.cos(a) * 38, 134 + Math.sin(a) * 38, 3, day ? AMBER : rgb(210, 220, 230));
		s.text(20, 126, day ? "06:00" : "18:00", DIM, 0.5);
		s.text(W - 20, 126, day ? "18:00" : "06:00", DIM, 0.5, "right");
		s.footer("FROM THE SKY OVER THE TOWER", "1/3", true);
	}

	static void cmLog(DeskScreen s, DeskData d) {
		s.header("COMMS", "EVENT LOG", d);
		int i = 0;
		for (DeskData.Event e : d.rec.events) {
			if (i >= 8) {
				break;
			}
			double y = 17 + i * 15;
			s.text(6, y, e.time(), DIM, 0.7);
			s.dot(38, y + 3, 1.6, e.colour());
			s.text(44, y, s.fit(e.text(), W - 50, 0.7), TXT, 0.7);
			i++;
		}
		if (i == 0) {
			s.text(W / 2, 56, "NOTHING YET", DIM, 0.9, "center");
			s.text(W / 2, 70, "RECORDING SINCE " + d.rec.since, DIM, 0.62, "center");
		}
		s.footer("NEWEST FIRST", "2/3", true);
	}

	static void cmNetwork(DeskScreen s, DeskData d) {
		s.header("COMMS", "NETWORK", d);
		boolean table = d.holoTable, hall = d.hall != null;
		Object[][] nodes = {{"TERMINAL", 40, 40, d.linked()}, {"CORE", 123, 30, d.coreLinked}, {"PACKAGER", 206, 40, d.packager},
				{"PICKUP", 206, 100, !d.pickups.isEmpty()}, {"HOLO TABLE", 40, 100, table}, {"DATA HALL", 123, 112, hall}};
		int down = 0;
		s.group(() -> {
			for (Object[] node : nodes) {
				boolean up = (boolean) node[3];
				s.line(123, 70, (int) node[1], (int) node[2], up ? rgb(40, 90, 80) : rgb(90, 40, 36), 0.6);
			}
		});
		s.dot(123, 70, 6, AMBER);
		s.text(123, 79, "DEPOT", AMBER, 0.6, "center");
		for (Object[] node : nodes) {
			boolean up = (boolean) node[3];
			int x = (int) node[1], y = (int) node[2];
			down += up ? 0 : 1;
			s.rect(x - 22, y - 7, 44, 14, rgb(14, 22, 24));
			s.frame(x - 22, y - 7, 44, 14, up ? GREEN : RED);
			s.text(x, y - 3, (String) node[0], up ? WHITE : RED, 0.55, "center");
		}
		s.footer(down == 0 ? "ALL LINKS UP  " + d.mode + " MODE" : down + (down == 1 ? " LINK DOWN" : " LINKS DOWN"), "3/3", down == 0);
	}

	// ================================================================ reception, security, the map table's desk

	static void rcDirectory(DeskScreen s, DeskData d) {
		s.header("WELCOME", "DIRECTORY", d);
		String[][] rows = {{"HOLO COMMAND TABLE", "AHEAD"}, {"FLIGHT OPS WEST", "LEFT"}, {"FLIGHT OPS EAST", "RIGHT"}, {"DATA HALL", "WEST CORNER"},
				{"SERVICE BAY", "NORTH-EAST"}, {"LOUNGE", "WEST OF LOBBY"}, {"TOWER MODEL", "NORTH-WEST"}, {"STAIRS + GALLERY", "PILLAR FEET"}};
		for (int i = 0; i < rows.length; i++) {
			double y = 17 + i * 14;
			s.text(6, y, rows[i][0], i == 0 ? AMBER : TXT, 0.8);
			s.text(W - 6, y, rows[i][1], DIM, 0.7, "right");
			s.rect(6, y + 10, W - 12, 0.4, GRID);
		}
		s.footer("YOU ARE AT RECEPTION", "1/3", true);
	}

	static void rcBoard(DeskScreen s, DeskData d) {
		s.header("WELCOME", "DEPARTURES + ARRIVALS", d);
		double[] cols = {6, 40, 64, W - 6};
		String[] align = {"left", "left", "left", "right"};
		s.rows(6, 17, List.<String[]>of(new String[] {"TIME", "ID", "TO / FROM", "STATUS"}), List.<int[]>of(new int[] {DIM, DIM, DIM, DIM}), cols, align,
				0.6, 11);
		List<Object[]> board = new ArrayList<>();
		for (DeskData.Flight f : d.flights) {
			String where, status;
			int colour;
			double when;
			switch (f.state()) {
				case "PICKUP" -> { where = "TO THE PICKUP"; status = "DEPARTED"; colour = GREEN; when = -f.elapsed(); }
				case "LOADING" -> { where = "AT THE PICKUP"; status = "LOADING"; colour = AMBER; when = -f.elapsed(); }
				case "OUTBOUND" -> { where = "SECTOR " + f.workBearing() + "  " + Math.round(f.workDist()) + " M"; status = "EN ROUTE"; colour = GREEN;
					when = -f.elapsed(); }
				case "WORKING" -> { where = "SECTOR " + f.workBearing() + "  " + Math.round(f.workDist()) + " M"; status = "WORKING"; colour = AMBER;
					when = -f.elapsed(); }
				case "RETURN" -> { where = "FROM " + f.workBearing(); status = "LANDING"; colour = GREEN; when = f.eta(); }
				default -> { where = "AT " + f.home(); status = "CHARGING"; colour = AMBER; when = f.eta(); }
			}
			board.add(new Object[] {d.clockIn(when), "#" + two(f.id()), where, status, colour, DeskScreen.tier(f.tier()), when});
		}
		board.sort(Comparator.comparingDouble(o -> (double) o[6]));
		List<String[]> rows = new ArrayList<>();
		List<int[]> colours = new ArrayList<>();
		for (Object[] b : board) {
			if (rows.size() >= 7) {
				break;
			}
			rows.add(new String[] {(String) b[0], (String) b[1], (String) b[2], (String) b[3]});
			colours.add(new int[] {WHITE, (int) b[5], TXT, (int) b[4]});
		}
		for (DeskData.Event e : d.rec.events) {
			if (rows.size() >= 7) {
				break;
			}
			if (e.text().contains("LANDED") && d.gameTime - e.tick() < 2400) {
				String[] w = e.text().split(" ");
				rows.add(new String[] {e.time(), w.length > 1 ? w[1] : "", "ARRIVED " + e.text().substring(e.text().indexOf("LANDED") + 6).trim(), "ARRIVED"});
				colours.add(new int[] {WHITE, TXT, TXT, DIM});
			}
		}
		s.rows(6, 28, rows, colours, cols, align, 0.75, 11);
		if (rows.isEmpty()) {
			s.text(W / 2, 64, "NO FLIGHTS RIGHT NOW", DIM, 0.85, "center");
		}
		s.footer("LIVE FROM THE DEPOT", "2/3", true);
	}

	static void rcTower(DeskScreen s, DeskData d) {
		s.header("WELCOME", "THE TOWER", d);
		for (int i = 0; i < 9; i++) {
			double y = 128 - (i + 1) * 12, w = 60 - i * 3;
			int c = i + 1 <= d.tier ? GREEN : i + 1 == d.building ? AMBER : rgb(34, 44, 46);
			s.rect(50 - w / 2, y, w, 11, rgb(16, 24, 26));
			s.frame(50 - w / 2, y, w, 11, c);
			if (i + 1 == d.building) {
				s.rect(50 - w / 2 + 1, y + 1, (w - 2) * d.progress, 9, rgb(90, 60, 24));
			}
		}
		s.rect(14, 128, 72, 4, rgb(40, 50, 52));
		double power = d.capacity <= 0 ? 0 : (double) d.energy / d.capacity;
		s.kv(104, 22, 134, "TIER", d.tier + " OF 9", WHITE, 1.0);
		s.kv(104, 38, 134, "DRONES", d.fleet + " / " + d.max, WHITE, 1.0);
		s.kv(104, 54, 134, "IN FLIGHT", String.valueOf(d.air), AMBER, 1.0);
		s.kv(104, 70, 134, "BUILDING", d.building > 0 ? "TIER " + d.building + "  " + pct(d.progress) : "NOTHING", d.building > 0 ? AMBER : DIM, 0.9);
		s.kv(104, 86, 134, "POWER", pct(power), power > 0.3 ? GREEN : AMBER, 1.0);
		s.footer("TRI-PILLAR DRONE TOWER", "3/3", true);
	}

	static void scAccess(DeskScreen s, DeskData d) {
		s.header("SECURITY", "ACCESS", d);
		s.text(6, 17, "SPEED GATES  LOBBY", DIM, 0.62);
		int n = Math.min(4, d.gates.size());
		for (int i = 0; i < n; i++) {
			DeskData.Gate g = d.gates.get(i);
			double x = 6 + i * 59;
			s.rect(x, 26, 54, 40, TILE);
			s.frame(x, 26, 54, 40, g.open() ? GREEN : GRID);
			s.text(x + 27, 29, "LANE " + (i + 1), WHITE, 0.62, "center");
			s.text(x + 27, 40, g.in() ? "IN" : "OUT", g.in() ? CYAN : AMBER, 0.9, "center");
			s.text(x + 27, 54, g.open() ? "OPEN" : "SHUT", g.open() ? GREEN : DIM, 0.62, "center");
		}
		if (n == 0) {
			s.text(W / 2, 42, "NO SPEED GATES", DIM, 0.8, "center");
		}
		s.text(6, 74, "DOORS", DIM, 0.62);
		for (int i = 0; i < Math.min(4, d.doors.size()); i++) {
			DeskData.Door door = d.doors.get(i);
			double y = 83 + i * 12;
			s.text(6, y, door.name(), TXT, 0.68);
			s.text(W - 6, y, door.open() ? "OPEN" : "SHUT", door.open() ? GREEN : DIM, 0.68, "right");
		}
		s.footer(d.readers ? "ALL READERS ONLINE" : "NO CARD READERS", "1/3", true);
	}

	static void scPlan(DeskScreen s, DeskData d) {
		s.header("SECURITY", "GROUND FLOOR", d);
		double cx = W / 2, cy = 78, r = 62, k = r / HALL_R;
		double[] hex = new double[14];
		for (int i = 0; i <= 6; i++) {
			hex[2 * i] = cx + r * Math.cos(Math.toRadians(i * 60));
			hex[2 * i + 1] = cy + r * Math.sin(Math.toRadians(i * 60));
		}
		s.line(hex, rgb(60, 90, 96), 0.8);
		s.ring(cx, cy, 14, AMBER, 0.6);
		for (int a : new int[] {90, 210, 330}) {
			s.dot(cx + 34 * Math.cos(Math.toRadians(a)), cy + 34 * Math.sin(Math.toRadians(a)), 7, rgb(40, 50, 52));
		}
		boolean fire = d.hall != null && d.hall.phase() != ServerRoom.NORMAL;
		Object[][] zones = {{"DATA HALL", d.zoneHall, fire ? RED : GREEN}, {"OPS W", d.zoneOpsWest, CYAN}, {"OPS E", d.zoneOpsEast, CYAN},
				{"BAY", d.zoneBay, AMBER}, {"LOBBY", d.zoneLobby, TXT}, {"MODEL", d.zoneMap, TXT}};
		for (Object[] z : zones) {
			double[] at = (double[]) z[1];
			if (at == null) {
				continue;
			}
			double x = cx + at[0] * k, y = cy + at[1] * k;
			s.rect(x - 15, y - 5, 30, 10, rgb(14, 22, 24));
			s.text(x, y - 2.5, (String) z[0], (int) z[2], 0.5, "center");
		}
		int inside = 0;
		for (double[] p : d.people) {
			if (Math.abs(p[1]) < HALL_R * 0.87 && Math.abs(p[0]) * 0.87 + Math.abs(p[1]) / 2 < HALL_R * 0.87) {
				s.dot(cx + p[0] * k, cy + p[1] * k, 1.6, WHITE);
				inside++;
			}
		}
		s.text(W - 6, 16, inside + (inside == 1 ? " PERSON IN" : " PEOPLE IN"), WHITE, 0.6, "right");
		s.footer(fire ? "DATA HALL FIRE ALARM" : "NO ALARMS", "2/3", !fire);
	}

	static void scAlarms(DeskScreen s, DeskData d) {
		s.header("SECURITY", "ALARMS", d);
		List<String> active = new ArrayList<>();
		DeskData.Hall hub = d.hall;
		if (hub != null && hub.phase() != ServerRoom.NORMAL) {
			active.add(hub.phase() == ServerRoom.DRILL ? "FM-200 DRILL" : hub.phase() == ServerRoom.RELEASED ? "FM-200 RELEASED" : "FM-200 ALARM");
		}
		if (hub != null && (hub.bits() & ServerRoom.ON_BATT) != 0) {
			active.add("UPS-1 ON BATTERY");
		}
		if (hub != null && hub.temp() > ServerRoom.HIGH_TEMP) {
			active.add("DATA HALL HOT");
		}
		if (d.lowPower) {
			active.add("DEPOT LOW POWER");
		}
		if (active.isEmpty()) {
			s.rect(4, 18, W - 8, 40, rgb(13, 24, 18));
			s.text(W / 2, 26, "NO ACTIVE ALARMS", GREEN, 1.2, "center");
			s.text(W / 2, 42, "FM-200  NORMAL   GATES  NORMAL", TXT, 0.65, "center");
		} else {
			s.rect(4, 18, W - 8, 40, rgb(40, 14, 12));
			s.text(W / 2, 22, active.size() + (active.size() == 1 ? " ACTIVE ALARM" : " ACTIVE ALARMS"), RED, 1.1, "center");
			for (int i = 0; i < Math.min(3, active.size()); i++) {
				s.text(W / 2, 34 + i * 8, active.get(i), WHITE, 0.62, "center");
			}
		}
		s.text(6, 66, "HISTORY", DIM, 0.62);
		int i = 0;
		for (DeskData.Event e : d.rec.security) {
			if (i >= 5) {
				break;
			}
			double y = 75 + i * 12;
			s.text(6, y, e.time(), DIM, 0.66);
			s.text(40, y, s.fit(e.text(), W - 46, 0.66), TXT, 0.66);
			i++;
		}
		if (i == 0) {
			s.text(6, 75, "NOTHING SINCE " + d.rec.since, DIM, 0.66);
		}
		s.footer("SECURITY DESK", "3/3", active.isEmpty());
	}

	static void mpRange(DeskScreen s, DeskData d) {
		s.header("MAP", "RANGE", d);
		int view = d.mapView;
		String[] names = {"SITE", "LAND", "RADIUS"};
		for (int i = 0; i < 3; i++) {
			double y = 20 + i * 24;
			boolean on = i == view;
			s.rect(4, y - 2, W - 8, 20, on ? rgb(30, 22, 12) : ROW);
			s.text(10, y + 1, names[i], on ? AMBER : TXT, 1.0);
			s.text(W - 8, y + 1, Math.round(d.mapViews[i]) + " M", on ? WHITE : DIM, 0.9, "right");
		}
		DeskData.Flight far = null;
		for (DeskData.Flight f : d.flights) {
			if (far == null || f.dist() > far.dist()) {
				far = f;
			}
		}
		s.text(6, 96, far == null ? "NO DRONES OUT: SHOWING THE SITE" : "FOLLOWING THE FARTHEST DRONE", DIM, 0.62);
		if (far != null) {
			s.text(6, 106, "#" + two(far.id()) + "  T" + far.tier() + "  " + Math.round(far.dist()) + " M  " + far.bearing(), AMBER, 0.8);
		}
		s.text(6, 120, "VIEW NOW " + Math.round(d.mapRadius) + " M", TXT, 0.7);
		s.footer("ZOOM IS AUTOMATIC", "1/3", true);
	}

	static void mpLegend(DeskScreen s, DeskData d) {
		s.header("MAP", "LEGEND", d);
		Object[][] rows = {{AMBER, "LAND  CONTOURS 8 M"}, {CYAN, "WATER"}, {WHITE, "TOWER + PILLARS"}, {GREEN, "DRONE"}, {AMBER, "ROUTE HOME"},
				{rgb(90, 100, 104), "CHUNK GRID 16 M"}, {RED, "BUILD LIMIT RING"}};
		for (int i = 0; i < rows.length; i++) {
			double y = 18 + i * 14.5;
			s.rect(8, y, 14, 7, (int) rows[i][0]);
			s.text(30, y, (String) rows[i][1], TXT, 0.75);
		}
		s.text(W - 6, 18, d.flights.size() + " DRONES OUT", d.flights.isEmpty() ? DIM : AMBER, 0.7, "right");
		s.footer("1:12 HOLOGRAM ABOVE", "2/3", true);
	}

	static void mpDrones(DeskScreen s, DeskData d) {
		double radius = d.mapRadius;
		s.header("MAP", "DRONES  " + Math.round(radius) + " M", d);
		double cx = W / 2, cy = 80, r = 60;
		s.rect(cx - r, cy - r, 2 * r, 2 * r, rgb(12, 18, 18));
		s.group(() -> {
			for (int k = -4; k <= 4; k++) {
				s.line(cx + k * r / 4, cy - r, cx + k * r / 4, cy + r, rgb(26, 36, 38), 0.3);
				s.line(cx - r, cy + k * r / 4, cx + r, cy + k * r / 4, rgb(26, 36, 38), 0.3);
			}
		});
		List<double[]> at = new ArrayList<>();
		for (DeskData.Flight f : d.flights) {
			double k = Math.min(1, (r - 2) / Math.max(1e-6, Math.max(Math.abs(f.x()), Math.abs(f.z())) / radius * r));
			at.add(new double[] {cx + f.x() / radius * r * k, cy + f.z() / radius * r * k, DeskScreen.tier(f.tier()), f.id()});
		}
		s.group(() -> {
			for (double[] p : at) {
				s.line(cx, cy, p[0], p[1], dark((int) p[2], 3), 0.4);
			}
		});
		for (double[] p : at) {
			s.dot(p[0], p[1], 1.6, (int) p[2]);
			s.text(p[0] + 3, p[1] - 3, "#" + two((int) p[3]), (int) p[2], 0.45);
		}
		s.dot(cx, cy, 2.2, WHITE);
		s.text(6, 18, d.flights.size() + " IN FLIGHT", AMBER, 0.7);
		s.text(W - 6, 18, "GRID " + Math.round(radius / 4) + " M", DIM, 0.6, "right");
		s.footer("AS THE MAP TABLE SHOWS THEM", "3/3", true);
	}
}
