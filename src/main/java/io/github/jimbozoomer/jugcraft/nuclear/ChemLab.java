package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The administration building's water chemistry laboratory (the administration building part 2; tools/admin_rooms.py).
 * <ul>
 * <li>The plant chemistry status board reads the nearest reactor once a second and works out what the lab's samples
 * show: the reactor coolant's boron (from the fuel's burnup, borated for a cold shutdown), lithium (coordinated with it),
 * dissolved hydrogen, chloride, dose equivalent iodine and gross activity (an iodine spike after a trip; damaged fuel
 * takes them past their limits), the secondary side's cation conductivity, sodium, oxygen, hydrazine and pH (in
 * service while the plant makes steam). Each against its limit: NORMAL, ACTION 1 or TS LIMIT. The iodine and gross
 * activity come from the gamma spectroscopy station in reach once it has counted a sample that day.</li>
 * <li>The gamma spectroscopy station counts a sample (a click): {@link #COUNT_TICKS} ticks of live time on its
 * germanium detector while the spectrum builds on its screen; then its analysis report lists each nuclide's activity
 * and the dose equivalent iodine.</li>
 * </ul>
 */
public final class ChemLab {
	public static final String BOARD = "chem_status_board", GAMMA = "chem_gamma_spec";
	/** The board's rows (tools/admin_rooms.py CSB_RCS then CSB_SEC). */
	public static final int ROWS = 11, RCS_ROWS = 6;
	/** The station's nuclides (tools/admin_rooms.py GS_NUCLIDES) then the dose equivalent iodine and the gross activity. */
	public static final String[] NUCLIDES = {"I-131", "I-133", "Cs-137", "Co-58", "Co-60", "Xe-133"};
	public static final int DEI = 6, GROSS = 7;
	public static final int COUNT_TICKS = 600, LINK_REACH = 16;
	public static final String[] SAMPLES = {"RCS-A", "RCS-B", "LETDOWN", "RCS-A", "RCS-B"};
	/** The rows' routine sample times (the RCS's, the gamma's when the station has not counted, the secondary's). */
	static final String[] TIMES = {"06:10", "06:10", "06:15", "06:10", "06:30", "06:30", "08:00", "08:00", "08:00", "08:00", "08:00"};
	public static final int NORMAL = 0, ACTION = 1, LIMIT = 2, OUT = 3;
	static final String[] STATUS = {"NORMAL", "ACTION 1", "TS LIMIT", "NOT IN SVC"};

	private ChemLab() {
	}

	public static boolean isBoard(BlockState state) {
		return DcRoom.is(state, BOARD);
	}

	public static boolean isGamma(BlockState state) {
		return DcRoom.is(state, GAMMA);
	}

	/** A small steady wander (-1 to 1) for row i, changing every half minute. */
	static double wander(int i, long now) {
		long h = (now / 600) * 0x9E3779B97F4A7C15L + i * 0xC2B2AE3D27D4EB4FL;
		h ^= h >>> 33;
		h *= 0xFF51AFD7ED558CCDL;
		h ^= h >>> 33;
		return ((h >>> 11) * 0x1.0p-53) * 2 - 1;
	}

	/**
	 * The reactor coolant's activities (uCi/g) as the plant stands: the nuclides, then DEI and gross. At power they
	 * follow the power; after a trip the iodines spike (with the decay heat as the clock); damaged fuel puts them far up.
	 */
	public static double[] activities(ReactorCore core) {
		double[] a = new double[8];
		if (core == null) {
			return a;
		}
		double h = Math.max(0.15, Math.min(1, core.heat()));
		double spike = core.shutDown() || core.scrammed ? 1 + 40 * Math.min(1, core.decay / ReactorCore.DECAY_SHARE) : 1;
		double fail = core.fuelDamaged ? 1 : 0;
		a[0] = (1.1e-3 * h * spike) * (1 + 5000 * fail);
		a[1] = (2.4e-3 * h * spike) * (1 + 2000 * fail);
		a[2] = 4.0e-5 * (1 + 0.5 * (spike - 1) / 40) * (1 + 1000 * fail);
		a[3] = 3.1e-4;
		a[4] = 5.2e-5;
		a[5] = (8.8e-2 * h) * (1 + 500 * fail);
		a[DEI] = a[0] + 0.25 * a[1] + 0.02 * a[2];
		double sum = 0;
		for (int i = 0; i < 6; i++) {
			sum += a[i];
		}
		a[GROSS] = 0.27 * h + sum;
		return a;
	}

	/** The status of an activity against its limit: DEI action at 0.1, limit 1.0 uCi/g; gross action at 50, limit 100. */
	static int activityStatus(int which, double v) {
		double action = which == DEI ? 0.1 : 50, limit = which == DEI ? 1.0 : 100;
		return v >= limit ? LIMIT : v >= action ? ACTION : NORMAL;
	}

	// ------------------------------------------------------------------------------------------- the status board

	/** Once a second: the board's link (the station in reach), the plant's mode, its rows ("value|status|code|time"). */
	public static void boardTick(LivePanelBlockEntity b, ServerLevel level) {
		BlockPos here = b.getBlockPos();
		List<LivePanelBlockEntity> near = ControlBoard.panels(level, here, PlantInterior.BLOCKS.get(GAMMA), LINK_REACH);
		near.sort(java.util.Comparator.comparingDouble(p -> p.getBlockPos().distSqr(here)));
		LivePanelBlockEntity station = near.isEmpty() ? null : near.get(0);
		BlockPos link = station == null ? null : station.getBlockPos().immutable();
		EquipmentBlockEntity vessel = StatusScreenBlockEntity.nearestVessel(level, here);
		ReactorCore core = vessel == null ? null : vessel.core;
		long now = level.getGameTime();
		List<String> rows = new java.util.ArrayList<>();
		String mode = core == null ? "----" : OpsOffice.mode(RelayRoom.Reactor.of(core, 0));
		if (core == null) {
			for (int i = 0; i < ROWS; i++) {
				rows.add("----|NO DATA|" + OUT + "|----");
			}
		} else {
			double tempF = core.temperature * 9 / 5 + 32;
			boolean cold = !core.scrammed && core.shutDown() && tempF < 200;
			boolean steaming = !core.shutDown() && !core.scrammed && core.heat() > 0.05;
			double burnup = 0;
			for (double f : core.fuel) {
				burnup += Math.min(1, f);
			}
			burnup = core.fuel.isEmpty() ? 0 : burnup / core.fuel.size();
			double boron = cold ? 2050 : 1250 * (1 - burnup) + 10 + (core.shutDown() && !core.scrammed ? 150 : 0);
			boron += 3 * wander(0, now);
			double lithium = cold ? 0.6 : 0.6 + 2.6 * Math.min(1, boron / 1300);
			lithium += 0.02 * wander(1, now);
			rows.add(row(String.format(Locale.ROOT, "%d", Math.round(boron)), NORMAL, TIMES[0]));
			rows.add(row(String.format(Locale.ROOT, "%.2f", lithium), lithium < 0.5 || lithium > 3.5 ? ACTION : NORMAL, TIMES[1]));
			if (cold) {
				rows.add(row(String.format(Locale.ROOT, "%.1f", 4.0 + 0.3 * wander(2, now)), OUT, TIMES[2]));
			} else {
				double h2 = 35 + 2 * wander(2, now);
				rows.add(row(String.format(Locale.ROOT, "%.1f", h2), h2 < 25 || h2 > 50 ? ACTION : NORMAL, TIMES[2]));
			}
			rows.add(row(String.format(Locale.ROOT, "%d", Math.round(2 + 1.4 * Math.abs(wander(3, now)))), NORMAL, TIMES[3]));
			// The iodine and gross activity: the station's count of the day, else the plant as it stands.
			double[] act = activities(core);
			String gammaTime = TIMES[4];
			if (station != null && station.gsStart >= 0 && now - station.gsStart >= COUNT_TICKS && now - station.gsStart < 24_000
					&& station.gsAct.size() == 8) {
				act = station.gsAct.stream().mapToDouble(Double::doubleValue).toArray();
				gammaTime = TscRoom.clock(station.gsDay);
			}
			rows.add(row(String.format(Locale.ROOT, "%.1E", act[DEI]), activityStatus(DEI, act[DEI]), gammaTime));
			rows.add(row(String.format(Locale.ROOT, "%.2f", act[GROSS]), activityStatus(GROSS, act[GROSS]), gammaTime));
			if (steaming) {
				rows.add(row(String.format(Locale.ROOT, "%.3f", 0.081 + 0.008 * wander(6, now)), NORMAL, TIMES[6]));
				rows.add(row(String.format(Locale.ROOT, "%.1f", 0.6 + 0.1 * wander(7, now)), NORMAL, TIMES[7]));
				rows.add(row(String.format(Locale.ROOT, "%.1f", 2.1 + 0.4 * wander(8, now)), NORMAL, TIMES[8]));
				rows.add(row(String.format(Locale.ROOT, "%d", Math.round(86 + 6 * wander(9, now))), NORMAL, TIMES[9]));
				rows.add(row(String.format(Locale.ROOT, "%.2f", 9.82 + 0.03 * wander(10, now)), NORMAL, TIMES[10]));
			} else {
				for (int i = 6; i < ROWS; i++) {
					rows.add(row("----", OUT, "----"));
				}
			}
		}
		boolean changed = !rows.equals(b.chemRows) || !mode.equals(b.chemMode) || (link == null ? b.panel != null : !link.equals(b.panel));
		if (changed) {
			b.chemRows.clear();
			b.chemRows.addAll(rows);
			b.chemMode = mode;
			b.panel = link;
			b.changedAndSync(level);
		}
	}

	static String row(String value, int code, String time) {
		return value + "|" + STATUS[code] + "|" + code + "|" + time;
	}

	/** A board row: its value, status, status code, sample time. */
	public record Row(String value, String status, int code, String time) {
		public static Row of(String s) {
			String[] p = s.split("\\|", -1);
			return p.length < 4 ? new Row("----", "", OUT, "----") : new Row(p[0], p[1], parse(p[2]), p[3]);
		}

		private static int parse(String s) {
			try {
				return Integer.parseInt(s);
			} catch (NumberFormatException e) {
				return OUT;
			}
		}
	}

	public static List<Row> rows(LivePanelBlockEntity b) {
		List<Row> out = new java.util.ArrayList<>();
		for (String s : b.chemRows) {
			out.add(Row.of(s));
		}
		return out;
	}

	/** What a click on the board says: how many rows are in specification, at their action level, and the plant's mode. */
	static Component boardStatus(LivePanelBlockEntity b) {
		int ok = 0, act = 0;
		for (Row r : rows(b)) {
			if (r.code() == NORMAL) {
				ok++;
			} else if (r.code() == ACTION || r.code() == LIMIT) {
				act++;
			}
		}
		return Component.translatable("message.jugcraft.nuclear.chem_status", ok, act, b.chemMode.trim().isEmpty() ? "----" : b.chemMode.trim());
	}

	// ------------------------------------------------------------------------------------------- gamma spectroscopy

	/** Whether the station is counting at game time now. */
	public static boolean counting(LivePanelBlockEntity g, long now) {
		return g.gsStart >= 0 && now - g.gsStart < COUNT_TICKS;
	}

	/** A click: starts counting the next sample (the plant's coolant as it stands now), or says how far the count is. */
	static void click(LivePanelBlockEntity g, ServerLevel level, Player player) {
		long now = level.getGameTime();
		if (counting(g, now)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.gs_busy", now - g.gsStart, COUNT_TICKS));
			return;
		}
		start(g, level);
		player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.gs_start", g.gsSample).withStyle(ChatFormatting.AQUA));
	}

	/** Starts a count: the sample's name, its activities (from the nearest reactor), the clock. */
	public static void start(LivePanelBlockEntity g, ServerLevel level) {
		EquipmentBlockEntity vessel = StatusScreenBlockEntity.nearestVessel(level, g.getBlockPos());
		double[] a = activities(vessel == null ? null : vessel.core);
		g.gsAct.clear();
		for (double v : a) {
			g.gsAct.add(v);
		}
		g.gsSample = SAMPLES[Math.floorMod(g.gsCount, SAMPLES.length)];
		g.gsCount++;
		g.gsStart = level.getGameTime();
		g.gsDay = level.getOverworldClockTime();
		g.changedAndSync(level);
		level.playSound(null, g.getBlockPos(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.4F, 1.6F);
		level.playSound(null, g.getBlockPos(), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.4F, 1.6F);
	}

	/** The station each tick: when a count finishes, its report (synced once, a chime). */
	static void gammaTick(LivePanelBlockEntity g, ServerLevel level) {
		if (g.gsStart >= 0 && level.getGameTime() - g.gsStart == COUNT_TICKS) {
			g.changedAndSync(level);
			level.playSound(null, g.getBlockPos(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.5F, 1.4F);
		}
	}

	/** Where a nuclide's gamma lines are (keV) and how strong (of its peak's height). */
	public static final double[][] LINES = {{364, 1.0, 637, 0.35, 284, 0.3}, {530, 1.0}, {662, 1.0}, {811, 1.0}, {1173, 1.0, 1332, 0.95}, {81, 1.0}};
	public static final double SPECTRUM_KEV = 2048, K40 = 1461;

	/**
	 * The spectrum's bar k of n (0 to 1) when the count is a fraction f done: the falling Compton continuum, each
	 * nuclide's lines by its activity (log), the background's K-40 line; the noise of counting.
	 */
	public static double bar(List<Double> act, int k, int n, double f, long seed) {
		if (f <= 0) {
			return 0;
		}
		double e0 = k * SPECTRUM_KEV / n, e1 = (k + 1) * SPECTRUM_KEV / n, e = (e0 + e1) / 2;
		double sum = 0;
		for (int i = 0; i < Math.min(6, act.size()); i++) {
			sum += act.get(i);
		}
		double level = sum <= 0 ? 0.12 : Math.max(0.15, Math.min(0.85, 0.42 + 0.07 * Math.log10(sum)));
		double v = level * (0.9 * Math.exp(-e / 700.0) + 0.12);
		for (int i = 0; i < Math.min(6, act.size()); i++) {
			double a = act.get(i);
			if (a <= 0) {
				continue;
			}
			double h = Math.max(0, Math.min(0.62, 0.22 + 0.075 * Math.log10(a / 1e-6)));
			double[] lines = LINES[i];
			for (int j = 0; j + 1 < lines.length; j += 2) {
				if (lines[j] >= e0 && lines[j] < e1) {
					v = Math.max(v, v + h * lines[j + 1]);
				}
			}
		}
		if (K40 >= e0 && K40 < e1) {
			v += 0.14;
		}
		double noise = wander(k, seed * 600) * 0.03;
		return Math.max(0, Math.min(1, (v + noise) * Math.sqrt(Math.min(1, f))));
	}
}
