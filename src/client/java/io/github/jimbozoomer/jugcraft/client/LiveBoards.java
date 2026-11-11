package io.github.jimbozoomer.jugcraft.client;

import static io.github.jimbozoomer.jugcraft.client.NuclearRenderer.v;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.NuclearRenderer.Frame;
import io.github.jimbozoomer.jugcraft.client.NuclearRenderer.Mesh;
import io.github.jimbozoomer.jugcraft.nuclear.BoardPlant;
import io.github.jimbozoomer.jugcraft.nuclear.ControlBoard;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind;
import io.github.jimbozoomer.jugcraft.nuclear.ReactorCore;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

/**
 * The control room's live boards drawn (round 27; tools/board_live.py lays them out, nuclear/ControlBoard.java and
 * nuclear/BoardPlant.java work out what they show): their annunciator windows lit, flashing or dark, their lamps, the
 * meters' pointers swinging to their readings (each with a little wander while live), the controllers' bar graphs,
 * the seven-segment figures, the recorders' traces, the switches' handles where they were last turned. The legends,
 * scales and nameplates are on their own texture (textures/entity/control_board_legends.png), drawn in a pass of
 * their own; a window's legend is black on white, coloured as it is drawn: lit, the lamp's colour behind it; unlit,
 * frosted white in the room's light.
 */
final class LiveBoards {
	static final Identifier LEGEND_TEXTURE = Jugcraft.id("textures/entity/control_board_legends.png");
	static final RenderType LEGEND_TYPE = RenderTypes.entityCutout(LEGEND_TEXTURE);
	/** Round 32: the plant display wall's own legend texture (its words, plates and colours: tools/display_wall.py). */
	static final Identifier WALL_TEXTURE = Jugcraft.id("textures/entity/display_wall_legends.png");
	static final RenderType WALL_TYPE = RenderTypes.entityCutout(WALL_TEXTURE);
	/** The windows' colours (tools/board_live.py TINTS and UNLIT_TINT). */
	static final int TINT_WHITE = argb(1.0, 0.98, 0.93), TINT_AMBER = argb(1.0, 0.74, 0.24), TINT_RED = argb(0.98, 0.28, 0.2),
			TINT_UNLIT = argb(0.80, 0.80, 0.78);
	/** How fast a pointer swings to a new reading (seconds to most of the way). */
	static final double SWING = 0.35;
	private static final Mesh[][] LEGENDS = new Mesh[EquipmentKind.values().length][2];

	private LiveBoards() {
	}

	private static int argb(double r, double g, double b) {
		return 0xFF000000 | (int) Math.round(r * 255) << 16 | (int) Math.round(g * 255) << 8 | (int) Math.round(b * 255);
	}

	/** A board's nameplates, scales and window frames on the legend texture: its still ones, or its lit ones. */
	static Mesh legendMesh(EquipmentKind kind, boolean lit) {
		int l = lit ? 1 : 0;
		if (LEGENDS[kind.ordinal()][l] == null) {
			Mesh mesh = new Mesh();
			Frame f = new Frame(mesh, true);
			f.legends = true;
			if (lit) {
				ControlBoards.lit(kind, f);
			} else {
				ControlBoards.still(kind, f);
			}
			LEGENDS[kind.ordinal()][l] = mesh;
		}
		return LEGENDS[kind.ordinal()][l];
	}

	// ------------------------------------------------------------------ readings

	static boolean signal(ControlBoard.State b, String name, double clock) {
		if (name.startsWith("!")) {
			return !signal(b, name.substring(1), clock);
		}
		return switch (name) {
			case "ann.unacked" -> b.unacked();
			case "ann.cleared" -> b.cleared();
			case "ann.test" -> clock < b.testUntil;
			default -> b.plant.signal(name, clock);
		};
	}

	/** A meter's small wander while it reads something (a live instrument is never quite still). */
	static double wander(String name, double clock, double span) {
		double h = (name.hashCode() & 0xFFFF) * 0.001;
		return span * (0.0035 * Math.sin(clock * 0.05 + h) + 0.0015 * Math.sin(clock * 0.31 + 2 * h));
	}

	/**
	 * Moves each pointer and figure toward its reading (called as the board is drawn): the needles', then the digital
	 * indicators', then the bar graphs', in the board's order.
	 */
	static void readings(ControlBoard.State b, EquipmentKind kind, double clock) {
		JsonObject live = ControlBoards.live(kind);
		if (live == null) {
			return;
		}
		JsonArray needles = live.getAsJsonArray("needles"), digits = live.getAsJsonArray("digits"), bars = live.getAsJsonArray("bars");
		int n = needles.size() + digits.size() + bars.size();
		long nanos = System.nanoTime();
		double dt = b.shownNanos == 0 ? 10 : Math.min(1, (nanos - b.shownNanos) / 1.0E9);
		b.shownNanos = nanos;
		boolean fresh = b.shown == null || b.shown.length != n;
		if (fresh) {
			b.shown = new float[n];
		}
		double k = fresh ? 1 : Math.min(1, dt / SWING);
		int i = 0;
		for (JsonArray list : new JsonArray[] {needles, digits, bars}) {
			for (JsonElement e : list) {
				JsonObject o = e.getAsJsonObject();
				String name = o.get("v").getAsString();
				double target = b.plant.value(name, clock);
				double span = o.has("hi") ? o.get("hi").getAsDouble() - o.get("lo").getAsDouble() : Math.max(1, Math.abs(target));
				// (Not the rod banks' step counters and positions, nor AUTO's setpoint: counted, not measured.)
				if (target != 0 && b.plant.present && !name.startsWith("bank.") && !name.equals("rod.sp")) {
					target += wander(name, clock, list == digits ? span * 0.3 : span);
				}
				b.shown[i] += (float) ((target - b.shown[i]) * k);
				i++;
			}
		}
	}

	// ------------------------------------------------------------------ the sheet's parts

	/** The board's live parts in the sheet's materials: lamps, pointers, bar graphs, figures, traces, handles. */
	static void draw(Frame still, Frame lit, ControlBoard.State b, EquipmentKind kind, double clock) {
		JsonObject live = ControlBoards.live(kind);
		if (live == null || b.shown == null) {
			return;
		}
		BoardPlant p = b.plant;
		for (JsonElement e : live.getAsJsonArray("lamps")) {
			JsonObject lamp = e.getAsJsonObject();
			if (lamp.has("c")) {
				continue;
			}
			boolean on = signal(b, lamp.get("s").getAsString(), clock);
			JsonElement mat = on ? lamp.get("on") : lamp.get("off");
			if (ControlBoards.legend(mat)) {
				continue;
			}
			boolean bright = on || lamp.has("off_lit") && lamp.get("off_lit").getAsBoolean();
			JsonArray q = lamp.getAsJsonArray("q");
			ControlBoards.face(bright ? lit : still, corner(q, 0), corner(q, 3), corner(q, 6), corner(q, 9), mat);
		}
		float[] red = NuclearRenderer.swatch(0, 0), black = NuclearRenderer.swatch(0, 2), white = NuclearRenderer.swatch(3, 1),
				dgrey = NuclearRenderer.swatch(1, 2), green = NuclearRenderer.swatch(3, 0);
		int i = 0;
		for (JsonElement e : live.getAsJsonArray("needles")) {
			JsonObject nd = e.getAsJsonObject();
			double value = b.shown[i++];
			double lo = nd.get("lo").getAsDouble(), hi = nd.get("hi").getAsDouble(), z = nd.get("z").getAsDouble();
			double f = Math.max(0, Math.min(1, (value - lo) / (hi - lo)));
			String type = nd.get("t").getAsString();
			if (type.equals("edge")) {
				double y = d(nd, "y0") + f * (d(nd, "y1") - d(nd, "y0"));
				rect(still, d(nd, "x0"), y - 0.0025, d(nd, "x1"), y + 0.0025, z, red);
				continue;
			}
			double angle = type.equals("sync") ? 90 - p.syncPhase(clock) : d(nd, "a0") + f * (d(nd, "a1") - d(nd, "a0"));
			double a = Math.toRadians(angle), cx = d(nd, "cx"), cy = d(nd, "cy"), r = d(nd, "r");
			double ux = Math.cos(a), uy = Math.sin(a), wx = -uy * 0.003, wy = ux * 0.003;
			float[] uv = nd.has("white") && nd.get("white").getAsBoolean() ? white : black;
			still.quad(v(cx - ux * 0.02 + wx, cy - uy * 0.02 + wy, z), v(cx - ux * 0.02 - wx, cy - uy * 0.02 - wy, z),
					v(cx + ux * r - wx * 0.4, cy + uy * r - wy * 0.4, z), v(cx + ux * r + wx * 0.4, cy + uy * r + wy * 0.4, z), v(0, 0, -1),
					uv[0], uv[1], uv[2], uv[3]);
			rect(still, cx - 0.008, cy - 0.008, cx + 0.008, cy + 0.008, z - 0.001, dgrey);
		}
		JsonArray digits = live.getAsJsonArray("digits");
		int firstDigit = i;
		i += digits.size();
		for (JsonElement e : live.getAsJsonArray("bars")) {
			JsonObject bar = e.getAsJsonObject();
			double f = Math.max(0, Math.min(1, (b.shown[i++] - d(bar, "lo")) / (d(bar, "hi") - d(bar, "lo"))));
			// Twenty lights up the window (round 30: a rod's column on the rod position display has its own count).
			int count = bar.has("n") ? bar.get("n").getAsInt() : 20;
			double y0 = d(bar, "y0"), y1 = d(bar, "y1"), step = (y1 - y0) / count;
			for (int k = 0; k < count; k++) {
				boolean on = (k + 0.5) / count <= f;
				double ya = y0 + k * step;
				rect(on ? lit : still, d(bar, "x0"), ya + step * 0.15, d(bar, "x1"), ya + step * 0.85, d(bar, "z"), on ? green : black);
			}
		}
		i = firstDigit;
		for (JsonElement e : digits) {
			JsonObject dg = e.getAsJsonObject();
			double value = b.shown[i++];
			if (!p.present && value == 0) {
				continue;
			}
			sevenSegment(lit, d(dg, "x0"), d(dg, "y0"), d(dg, "x1"), d(dg, "y1"), d(dg, "z"), figures(value, dg.get("n").getAsInt(), dg.get("dp").getAsInt()), red);
		}
		// The recorders: each pen's trace down the chart (the newest at the top, where the pen writes) and its pen.
		int pen = 0;
		for (JsonElement e : live.getAsJsonArray("pens")) {
			JsonObject rec = e.getAsJsonObject();
			double x0 = d(rec, "x0"), x1 = d(rec, "x1"), y0 = d(rec, "y0"), y1 = d(rec, "y1"), z = d(rec, "z");
			for (JsonElement q : rec.getAsJsonArray("pens")) {
				JsonObject pn = q.getAsJsonObject();
				float[] uv = ControlBoards.region(pn.get("c"));
				int n = ControlBoard.SAMPLES;
				double px = Double.NaN, py = 0;
				for (int k = 0; k < n; k++) {
					int idx = pen * n + Math.floorMod(b.head + k, n);
					int sample = idx < b.history.length ? b.history[idx] : 0;
					if (sample == 0) {
						continue;
					}
					double x = x0 + (sample - 1) / 65534.0 * (x1 - x0);
					double y = y1 - (n - 1 - k) / (double) (n - 1) * (y1 - y0);
					if (!Double.isNaN(px)) {
						rect(lit, px - 0.0022, py, px + 0.0022, y, z, uv);
						rect(lit, Math.min(px, x) - 0.0022, y - 0.0022, Math.max(px, x) + 0.0022, y + 0.0022, z, uv);
					}
					px = x;
					py = y;
				}
				double now = (p.value(pn.get("v").getAsString(), clock) - d(pn, "lo")) / (d(pn, "hi") - d(pn, "lo"));
				double mx = x0 + Math.max(0, Math.min(1, now)) * (x1 - x0);
				rect(lit, mx - 0.006, y1, mx + 0.006, y1 + 0.01, z - 0.001, uv);
				pen++;
			}
		}
		// The switches' handles where they were last turned, and the turbine trip's guard.
		for (JsonElement e : live.getAsJsonArray("handles")) {
			JsonObject h = e.getAsJsonObject();
			String id = h.get("id").getAsString();
			String state = switch (id) {
				case "turbine_trip" -> p.present && p.turbineTripped ? "0" : "1";
				case "turbine_guard", "si_a_guard", "rsp_trip_a_guard", "rx_trip_a_guard" -> clock < b.guardUntil ? "1" : "0";
				case "si_b_guard", "rsp_trip_b_guard", "rx_trip_b_guard" -> clock < b.guardBUntil ? "1" : "0";
				case "transfer" -> p.present && p.remote ? "1" : "0";
				case "rsp_trip_a", "rsp_trip_b", "rx_trip_a", "rx_trip_b" -> p.present && p.scrammed ? "0" : "1";
				// Round 30: the reactor console's rod control (MAN or AUTO), its lever (held, or back at HOLD), the
				// reactor protection switch.
				case "rod_mode" -> p.present && p.rodAuto ? "1" : "0";
				case "rod_lever" -> clock < b.leverUntil ? b.lever > 0 ? "1" : "0" : "c";
				case "rps" -> !p.present || p.protection ? "1" : "0";
				case "si_a", "si_b" -> p.siOn() ? "0" : "1";
				case "gen_bkr" -> p.online() ? "1" : "0";
				// (where they were turned: a pump turned on but without power on its bus still has its switch at START)
				case "afw_a" -> p.afwOn(0) ? "1" : "0";
				case "afw_b" -> p.afwOn(1) ? "1" : "0";
				case "afw_td" -> p.afwOn(2) ? "1" : "0";
				// Diesels make real power: the RES XFMR breakers to the safety buses (closed: turned to CLOSE).
				case "res_a" -> (p.resOpen & 1) == 0 ? "1" : "0";
				case "res_b" -> (p.resOpen & 2) == 0 ? "1" : "0";
				// Turbine hall part 2: the main feed pumps' switches (where they were last turned, as their pumps run).
				case "mfp_a" -> p.mfpInService(0) ? "1" : "0";
				case "mfp_b" -> p.mfpInService(1) ? "1" : "0";
				default -> id.startsWith("rcp") ? p.rcpRunning(id.charAt(3) - '1') || !p.present ? "1" : "0"
						: id.startsWith("msiv") ? p.msivOpen(id.charAt(4) - '1') || !p.sg(id.charAt(4) - '1') ? "1" : "0"
						: id.startsWith("fcv") ? (p.afwShut & 1 << id.charAt(3) - '1') == 0 ? "1" : "0"
						: id.startsWith("arv") ? (p.arvManual & 1 << id.charAt(3) - '1') != 0 ? "1" : "0" : "c";
			};
			JsonObject ops = h.getAsJsonObject("ops");
			if (ops.has(state)) {
				ControlBoards.ops(still, ops.getAsJsonArray(state));
			}
		}
	}

	private static Vec3 corner(JsonArray q, int at) {
		return v(q.get(at).getAsDouble(), q.get(at + 1).getAsDouble(), q.get(at + 2).getAsDouble());
	}

	private static double d(JsonObject o, String key) {
		return o.get(key).getAsDouble();
	}

	/** A flat rectangle facing the operators at depth z. */
	static void rect(Frame f, double x0, double y0, double x1, double y1, double z, float[] uv) {
		f.quad(v(x0, y1, z), v(x0, y0, z), v(x1, y0, z), v(x1, y1, z), v(0, 0, -1), uv[0], uv[1], uv[2], uv[3]);
	}

	/**
	 * A reading as a digital indicator shows it: right-aligned in n figures, with dp decimals; dp -1 scientific (a
	 * radiation monitor's: a figure, its decimal, E and the power of ten, 2.3E-1; nothing when it reads nothing).
	 */
	static String figures(double value, int n, int dp) {
		if (dp < 0) {
			if (!(value > 0)) {
				return "";
			}
			int e = (int) Math.floor(Math.log10(value) + 1e-9);
			double m = value / Math.pow(10, e);
			if (m >= 9.95) {
				m /= 10;
				e++;
			}
			return String.format(Locale.ROOT, "%.1fE%s%d", m, e < 0 ? "-" : " ", Math.abs(e));
		}
		String s = dp > 0 ? String.format(Locale.ROOT, "%." + dp + "f", value) : String.valueOf(Math.round(value));
		int width = n + (dp > 0 ? 1 : 0);
		return s.length() > width ? s.substring(s.length() - width) : " ".repeat(width - s.length()) + s;
	}

	private static final String[] SEGMENTS = {"abcdef", "bc", "abged", "abgcd", "fgbc", "afgcd", "afgedc", "abc", "abcdefg", "abcdfg"};

	/** Seven-segment figures (and the decimal point) in the window x0..x1, y0..y1, lit red. */
	static void sevenSegment(Frame f, double x0, double y0, double x1, double y1, double z, String text, float[] uv) {
		int count = 0;
		for (char c : text.toCharArray()) {
			if (c != '.') {
				count++;
			}
		}
		double cw = (x1 - x0) / Math.max(1, count);
		double t = Math.min(cw, y1 - y0) * 0.14, ym = (y0 + y1) / 2;
		int pos = 0;
		for (char c : text.toCharArray()) {
			if (c == '.') {
				double dx = x0 + pos * cw - t * 0.6;
				rect(f, dx - t / 2, y0, dx + t / 2, y0 + t, z, uv);
				continue;
			}
			double a0 = x0 + pos * cw + cw * 0.15, a1 = x0 + (pos + 1) * cw - cw * 0.15;
			String segs = c >= '0' && c <= '9' ? SEGMENTS[c - '0'] : c == '-' ? "g" : c == 'E' ? "adefg" : "";
			for (char s : segs.toCharArray()) {
				switch (s) {
					case 'a' -> rect(f, a0, y1 - t, a1, y1, z, uv);
					case 'g' -> rect(f, a0, ym - t / 2, a1, ym + t / 2, z, uv);
					case 'd' -> rect(f, a0, y0, a1, y0 + t, z, uv);
					case 'f' -> rect(f, a0, ym, a0 + t, y1, z, uv);
					case 'b' -> rect(f, a1 - t, ym, a1, y1, z, uv);
					case 'e' -> rect(f, a0, y0, a0 + t, ym, z, uv);
					case 'c' -> rect(f, a1 - t, y0, a1, ym, z, uv);
					default -> {
					}
				}
			}
			pos++;
		}
	}

	// ------------------------------------------------------------------ the legend texture's parts

	/**
	 * The legend pass: the board's plates and scales, then its windows and legend lenses coloured by state (lit, the
	 * lamp's colour at full brightness; unlit, frosted in the room's light), and the unlit lamps' lenses.
	 */
	static void legends(PoseStack.Pose pose, VertexConsumer consumer, Direction facing, int light, EquipmentKind kind, ControlBoard.State b,
			double clock) {
		legendMesh(kind, false).replay(pose, consumer, facing, light);
		legendMesh(kind, true).replay(pose, consumer, facing, LightCoordsUtil.FULL_BRIGHT);
		JsonObject live = ControlBoards.live(kind);
		if (live == null || b == null) {
			return;
		}
		Mesh unlit = new Mesh(), whites = new Mesh(), ambers = new Mesh(), reds = new Mesh(), lenses = new Mesh();
		Frame fu = legendFrame(unlit), fw = legendFrame(whites), fa = legendFrame(ambers), fr = legendFrame(reds), fl = legendFrame(lenses);
		boolean test = clock < b.testUntil;
		boolean fast = ((long) (clock / 4)) % 2 == 0, slow = ((long) (clock / 10)) % 2 == 0;
		JsonArray windows = live.getAsJsonArray("windows");
		for (int i = 0; i < windows.size(); i++) {
			JsonObject w = windows.get(i).getAsJsonObject();
			boolean on;
			if (w.get("k").getAsInt() == 1) {
				on = signal(b, w.get("id").getAsString(), clock);
			} else {
				byte state = i < b.windows.length ? b.windows[i] : ControlBoard.CLEAR;
				on = switch (state) {
					case ControlBoard.ALERT -> fast;
					case ControlBoard.ACKED -> true;
					case ControlBoard.RINGBACK -> slow;
					default -> false;
				};
			}
			on |= test;
			JsonArray q = w.getAsJsonArray("q");
			Frame f = !on ? fu : switch (w.get("c").getAsString()) {
				case "red" -> fr;
				case "amber" -> fa;
				default -> fw;
			};
			float[] uv = ControlBoards.region(w.get("uv"));
			rect(f, q.get(0).getAsDouble(), q.get(1).getAsDouble(), q.get(2).getAsDouble(), q.get(3).getAsDouble(), q.get(4).getAsDouble(), uv);
		}
		for (JsonElement e : live.getAsJsonArray("lamps")) {
			JsonObject lamp = e.getAsJsonObject();
			boolean on = signal(b, lamp.get("s").getAsString(), clock);
			JsonArray q = lamp.getAsJsonArray("q");
			if (lamp.has("c")) {
				Frame f = !on ? fu : switch (lamp.get("c").getAsString()) {
					case "red" -> fr;
					case "amber" -> fa;
					default -> fw;
				};
				ControlBoards.face(f, corner(q, 0), corner(q, 3), corner(q, 6), corner(q, 9), lamp.get("on"));
				continue;
			}
			JsonElement mat = on ? lamp.get("on") : lamp.get("off");
			if (ControlBoards.legend(mat)) {
				ControlBoards.face(fl, corner(q, 0), corner(q, 3), corner(q, 6), corner(q, 9), mat);
			}
		}
		if (unlit.vertices() > 0) unlit.replay(pose, consumer, facing, light, TINT_UNLIT);
		if (lenses.vertices() > 0) lenses.replay(pose, consumer, facing, light);
		if (whites.vertices() > 0) whites.replay(pose, consumer, facing, LightCoordsUtil.FULL_BRIGHT, TINT_WHITE);
		if (ambers.vertices() > 0) ambers.replay(pose, consumer, facing, LightCoordsUtil.FULL_BRIGHT, TINT_AMBER);
		if (reds.vertices() > 0) reds.replay(pose, consumer, facing, LightCoordsUtil.FULL_BRIGHT, TINT_RED);
	}

	// ------------------------------------------------------------------ round 32: the plant display wall

	/**
	 * The display wall's pass on its own texture (tools/display_wall.py): its cabinet's plates in the room's light, its
	 * picture, then its live parts at full brightness as the projectors show them: the mimic's symbols (red running or
	 * open, green stopped or shut) and lines (bright while they carry flow, dim while not), the safety functions'
	 * tiles in their colours, the vessels' levels, the trends across their charts and the banner's alarms (flashing
	 * fast until acknowledged at their board, slowly once cleared until reset). Its words are written by the renderer.
	 */
	static void wall(PoseStack.Pose pose, VertexConsumer consumer, Direction facing, int light, EquipmentKind kind, ControlBoard.State b, double clock) {
		legendMesh(kind, false).replay(pose, consumer, facing, light);
		legendMesh(kind, true).replay(pose, consumer, facing, LightCoordsUtil.FULL_BRIGHT);
		JsonObject live = ControlBoards.live(kind);
		if (live == null || b == null) {
			return;
		}
		BoardPlant p = b.plant;
		Mesh mesh = new Mesh();
		Frame f = legendFrame(mesh);
		for (String key : new String[] {"lamps", "flows"}) {
			for (JsonElement e : live.getAsJsonArray(key)) {
				JsonObject o = e.getAsJsonObject();
				boolean on = signal(b, o.get("s").getAsString(), clock);
				JsonArray q = o.getAsJsonArray("q");
				ControlBoards.face(f, corner(q, 0), corner(q, 3), corner(q, 6), corner(q, 9), on ? o.get("on") : o.get("off"));
			}
		}
		for (JsonElement e : live.getAsJsonArray("fills")) {
			JsonObject o = e.getAsJsonObject();
			String id = o.get("id").getAsString();
			int state = id.startsWith("dw.csf") ? io.github.jimbozoomer.jugcraft.nuclear.DisplayWall.csf(p, id.charAt(6) - '0', clock) : 0;
			JsonArray q = o.getAsJsonArray("q"), mats = o.getAsJsonArray("m");
			ControlBoards.face(f, corner(q, 0), corner(q, 3), corner(q, 6), corner(q, 9), mats.get(Math.min(state, mats.size() - 1)));
		}
		for (JsonElement e : live.getAsJsonArray("levels")) {
			JsonObject o = e.getAsJsonObject();
			double frac = Math.max(0, Math.min(1, (p.value(o.get("v").getAsString(), clock) - d(o, "lo")) / (d(o, "hi") - d(o, "lo"))));
			if (frac > 0.002) {
				rect(f, d(o, "x0"), d(o, "y0"), d(o, "x1"), d(o, "y0") + frac * (d(o, "y1") - d(o, "y0")), d(o, "z"), ControlBoards.region(o.get("m")));
			}
		}
		// The trends: each pen's samples left (five minutes ago) to right (now), stepping as a recorder's pen does.
		int pen = 0;
		for (JsonElement e : live.getAsJsonArray("pens")) {
			JsonObject rec = e.getAsJsonObject();
			double x0 = d(rec, "x0"), x1 = d(rec, "x1"), y0 = d(rec, "y0"), y1 = d(rec, "y1"), z = d(rec, "z"), t = 0.003;
			for (JsonElement q : rec.getAsJsonArray("pens")) {
				float[] uv = ControlBoards.region(q.getAsJsonObject().get("c"));
				int n = ControlBoard.SAMPLES;
				double px = Double.NaN, py = 0;
				for (int k = 0; k < n; k++) {
					int idx = pen * n + Math.floorMod(b.head + k, n);
					int sample = idx < b.history.length ? b.history[idx] : 0;
					if (sample == 0) {
						continue;
					}
					double x = x0 + k / (double) (n - 1) * (x1 - x0), y = y0 + (sample - 1) / 65534.0 * (y1 - y0);
					if (!Double.isNaN(px)) {
						rect(f, px, py - t, x, py + t, z, uv);
						if (Math.abs(y - py) > 1e-4) {
							rect(f, x - t, Math.min(py, y) - t, x + t, Math.max(py, y) + t, z - 0.0005, uv);
						}
					}
					px = x;
					py = y;
				}
				pen++;
			}
		}
		// The banner: its slots' colours by the alarms' states.
		boolean fast = ((long) (clock / 4)) % 2 == 0, slow = ((long) (clock / 10)) % 2 == 0;
		for (JsonElement e : live.getAsJsonArray("alarms")) {
			JsonObject a = e.getAsJsonObject();
			JsonArray slots = a.getAsJsonArray("slots");
			JsonObject mats = a.getAsJsonObject("mats");
			for (int i = 0; i < Math.min(slots.size(), b.wallAlarms.length); i++) {
				JsonArray q = slots.get(i).getAsJsonArray();
				String colour = wallColour(b.wallAlarms[i]);
				byte state = io.github.jimbozoomer.jugcraft.nuclear.DisplayWall.state(b.wallAlarms[i]);
				boolean lit = state == ControlBoard.ACKED || state == ControlBoard.ALERT && fast;
				if (state == ControlBoard.RINGBACK && !slow) {
					continue;
				}
				rect(f, q.get(0).getAsDouble(), q.get(1).getAsDouble(), q.get(2).getAsDouble(), q.get(3).getAsDouble(), d(a, "z"),
						ControlBoards.region(mats.get(lit ? colour : colour + "_dim")));
			}
		}
		if (mesh.vertices() > 0) {
			mesh.replay(pose, consumer, facing, LightCoordsUtil.FULL_BRIGHT);
		}
	}

	/** A banner alarm's window as its board's layout has it (null if the layout has none). */
	static @org.jspecify.annotations.Nullable JsonObject wallWindow(int packed) {
		EquipmentKind kind = io.github.jimbozoomer.jugcraft.nuclear.DisplayWall.kind(packed);
		JsonObject live = ControlBoards.live(kind);
		int i = io.github.jimbozoomer.jugcraft.nuclear.DisplayWall.window(packed);
		if (live == null || i >= live.getAsJsonArray("windows").size()) {
			return null;
		}
		return live.getAsJsonArray("windows").get(i).getAsJsonObject();
	}

	/** A banner alarm's colour: its window's (a trip red, an alarm amber). */
	static String wallColour(int packed) {
		JsonObject w = wallWindow(packed);
		return w != null && w.get("c").getAsString().equals("red") ? "red" : "amber";
	}

	/** A banner alarm's legend, a line or two (its window's engraving, its lines run together). */
	static List<String> wallLegend(int packed) {
		JsonObject w = wallWindow(packed);
		String[] words = w == null || !w.has("t") ? new String[] {"ALARM"} : w.get("t").getAsString().split("\\|");
		String one = String.join(" ", words);
		if (one.length() <= 18 || words.length < 2) {
			return List.of(one);
		}
		int cut = (words.length + 1) / 2;
		return List.of(String.join(" ", java.util.Arrays.copyOfRange(words, 0, cut)), String.join(" ", java.util.Arrays.copyOfRange(words, cut, words.length)));
	}

	private static Frame legendFrame(Mesh mesh) {
		Frame f = new Frame(mesh, true);
		f.legends = true;
		return f;
	}

	// ------------------------------------------------------------------ the plant computer's screens

	/** A line on a screen: its words, the figure right-aligned after them (or empty), its colour. */
	record Line(String text, String value, int colour) {
	}

	static final int GREEN = 0xFF5AE678, AMBER = 0xFFF0BE3C, RED = 0xFFFA5040, WHITE = 0xFFE6E6DC;

	/** What a screen shows (its id: "rcs", "ehc", "gen" or, round 28, "rad"). */
	static List<Line> screen(String id, BoardPlant p, double clock) {
		List<Line> out = new ArrayList<>();
		if (!p.present) {
			out.add(new Line("NO REACTOR IN RANGE", "", WHITE));
			return out;
		}
		switch (id) {
			case "rx" -> {
				// The reactor control console's reactor page (round 30): power and startup rate, rod control, the banks,
				// the shutdown banks and the trip, the fuel (or what is not right).
				out.add(new Line(p.scrammed ? "REACTOR TRIPPED" : "REACTOR SUMMARY", "UNIT 1", p.scrammed ? RED : GREEN));
				out.add(new Line("NIS POWER " + f1(p.prAverage()) + " %", "SUR " + (p.sur >= 0 ? "+" : "") + f1(p.sur), alarmed(p.alarm("sur_hi", clock))));
				String motion = p.rodsMoved < -1e-6 && !p.scrammed ? "OUT" : p.rodsMoved > 1e-6 && !p.scrammed ? "IN" : "";
				out.add(new Line("RODS " + (p.rodAuto ? "AUTO SP " + f1(p.autoSetpoint * 100) + " %" : "MAN " + motion),
						f0(p.rodSpeed()) + " SPM", p.rodAuto || motion.isEmpty() ? GREEN : AMBER));
				StringBuilder banks = new StringBuilder("BANKS");
				for (int k = 4; k < 8; k++) {
					banks.append(' ').append((char) ('A' + k - 4)).append(f0(p.bankSteps(k)));
				}
				out.add(new Line(banks.toString(), "", GREEN));
				out.add(new Line("SD BANKS " + f0(p.sdBanks), p.scrammed ? "TRIP " + switch (p.tripCause) {
					case ReactorCore.TRIP_OTDT -> "OT DELTA-T";
					case ReactorCore.TRIP_FLOW1 -> "LO FLOW 1 LP";
					case ReactorCore.TRIP_FLOW2 -> "LO FLOW 2 LP";
					case ReactorCore.TRIP_TURBINE -> "TURB TRIP";
					case ReactorCore.TRIP_SI -> "SAFETY INJ";
					case ReactorCore.TRIP_SG_LO -> "SG LO-LO LVL";
					default -> "MANUAL";
				} : "TRIP NONE", p.scrammed ? RED : GREEN));
				if (p.remote) {
					out.add(new Line("CONTROL AT REMOTE PANEL", "", AMBER));
				} else if (!p.protection) {
					out.add(new Line("REACTOR PROTECTION BYPASSED", "", AMBER));
				} else if (p.alarm("ir_hi", clock) || p.alarm("pr_lo_hi", clock)) {
					out.add(new Line("IR / PR LO TRIP NOT BLOCKED", "", RED));
				} else if (p.alarm("sr_hi", clock)) {
					out.add(new Line("SR TRIP NOT BLOCKED", "", RED));
				} else {
					out.add(new Line("FUEL " + p.fuelLoaded + "/" + p.coreSize, "BURNUP " + f0(p.burnup * 100) + " %",
							p.fuelLoaded < p.coreSize ? AMBER : GREEN));
				}
				return out;
			}
			case "rsp" -> {
				// The remote shutdown panel's unit status (round 29): the reactor, the generators, where control is.
				out.add(new Line("UNIT STATUS", "", 0xFF4FC8E8));
				if (p.scrammed) {
					out.add(new Line("REACTOR TRIPPED", "", RED));
					out.add(new Line("DECAY HEAT", String.format(Locale.ROOT, "%.1f%%", p.heat * 100), AMBER));
				} else {
					out.add(new Line("POWER", String.format(Locale.ROOT, "%.1f%%", p.fission * 100), GREEN));
					out.add(new Line("CORE TEMP", f0(p.temperature) + " C", GREEN));
				}
				StringBuilder levels = new StringBuilder("SG LVL");
				boolean low = false;
				for (int k = 0; k < 4; k++) {
					levels.append(' ').append(p.sg(k) ? f0(p.nr(k) * 100) : "--");
					low |= p.sg(k) && p.nr(k) < 0.35;
				}
				out.add(new Line(levels + " %", "", low ? AMBER : GREEN));
				out.add(new Line("CONTROL:", p.remote ? "THIS PANEL" : "CONTROL ROOM", p.remote ? AMBER : GREEN));
				return out;
			}
			case "rad" -> {
				// The radiation monitors (round 28): three rows of four readings, then the control room's HVAC.
				out.add(new Line("RADIATION MONITORS", "UNIT 1", GREEN));
				for (int row = 0; row < 3; row++) {
					StringBuilder text = new StringBuilder();
					boolean alert = false, high = false;
					for (int i = 0; i < 4; i++) {
						int n = row * 4 + i + 1;
						String fig = p.radFound(n) ? figures(p.radReading(n), 4, -1).replace(' ', '+') : "FAIL";
						text.append(i > 0 ? "  " : "").append(n).append(' ').append(fig);
						alert |= p.signal("rm" + n + ".alert", clock);
						high |= p.signal("rm" + n + ".high", clock);
					}
					out.add(new Line(text.toString(), "", high ? RED : alert ? AMBER : GREEN));
				}
				out.add(!p.hvFound ? new Line("CR HVAC", "NOT FOUND", WHITE)
						: new Line("CR HVAC " + (p.hvEmergency() ? "EMERGENCY" : "NORMAL"), p.hvEmergency() ? "CREFS A+B" : "TRAIN A",
								p.hvEmergency() ? AMBER : GREEN));
				return out;
			}
			case "rcs" -> {
				out.add(new Line("RCS SUMMARY", "UNIT 1", GREEN));
				out.add(new Line("TAVG " + f1(p.value("rcs.tavg", clock)) + " F", "TREF " + f1(p.value("rcs.tref", clock)), alarmed(p.alarm("tavg_dev", clock))));
				out.add(new Line("PZR " + f0(p.pzrPress()) + " PSIG", "LVL " + f0(p.pzrLevel()) + " %",
						alarmed(p.alarm("pzr_p_hi", clock) || p.alarm("pzr_p_lo", clock))));
				StringBuilder flows = new StringBuilder("FLOW");
				boolean low = false;
				for (int k = 0; k < 4; k++) {
					flows.append(' ').append(p.pumps && k < p.loops ? f0(p.loopFlow[k] * 100) : "--");
					low |= p.loopLow(k);
				}
				out.add(new Line(flows + " %", "", alarmed(low)));
				if (p.scrammed) {
					out.add(new Line("RX TRIPPED", switch (p.tripCause) {
						case ReactorCore.TRIP_OTDT -> "OT DELTA-T";
						case ReactorCore.TRIP_FLOW1 -> "LO FLOW 1 LP";
						case ReactorCore.TRIP_FLOW2 -> "LO FLOW 2 LP";
						case ReactorCore.TRIP_TURBINE -> "TURB TRIP";
						case ReactorCore.TRIP_SI -> "SAFETY INJ";
						case ReactorCore.TRIP_SG_LO -> "SG LO-LO LVL";
						default -> "MANUAL";
					}, RED));
				} else {
					out.add(new Line("RX POWER " + f0(p.fission * 100) + " %", "RODS IN " + f0(p.rods) + " %", GREEN));
				}
			}
			case "ehc" -> {
				out.add(new Line("DEH TURBINE CONTROL", "", GREEN));
				out.add(new Line("SPEED", f0(p.turbineSpeed * 1800) + " RPM", p.latched() && p.turbineSpeed < 0.99 ? AMBER : GREEN));
				out.add(new Line("LOAD", f0(p.value("gen.mw", clock)) + " MW", GREEN));
				out.add(new Line("VALVES", p.latched() ? "SV OPEN  CV " + f0(p.value("ehc.valve_pos", clock)) + " %" : "SV CV SHUT", p.latched() ? GREEN : AMBER));
				if (!p.steamToPower) {
					out.add(new Line("NO TURBINE", "", WHITE));
				} else if (!p.latched()) {
					// Turbine hall part 1: on its gear, or (the gear out) standing still, its rotor bowing if hot.
					out.add(new Line("TRIPPED", p.turningGear && p.turbineSpeed <= ReactorCore.TURNING_GEAR + 1e-4 ? "TURN GEAR"
							: p.turbineSpeed < ReactorCore.ZERO_SPEED ? p.rotorBowing() ? "STOPPED  ECCEN HI" : "STOPPED" : "COASTING", RED));
				} else if (p.online()) {
					out.add(new Line("OPER AUTO", "ON LINE", GREEN));
				} else {
					out.add(new Line("LATCHED", p.turbineSpeed < 0.99 ? "ACCELERATING" : "AT 1800 RPM", AMBER));
				}
			}
			default -> {
				double mw = p.value("gen.mw", clock), mvar = p.value("gen.mvar", clock), mva = Math.hypot(mw, mvar);
				out.add(new Line("GENERATOR", "UNIT 1", GREEN));
				out.add(new Line("MW " + f0(mw), "MVAR " + f0(mvar), GREEN));
				out.add(new Line("KV " + f1(p.value("gen.kv", clock)), "KA " + f1(p.value("gen.ka", clock)), GREEN));
				out.add(new Line("HZ " + String.format(Locale.ROOT, "%.2f", p.value("gen.hz", clock)), "PF " + String.format(Locale.ROOT, "%.2f",
						mva > 0 ? mw / mva : 0), p.online() ? GREEN : AMBER));
				out.add(p.online() ? new Line("GEN BKR CLOSED", "", GREEN) : p.syncArmed ? new Line("SYNCHRONIZING", "", AMBER)
						: new Line("GEN BKR OPEN", p.field() ? "" : "FIELD OPEN", RED));
			}
		}
		return out;
	}

	private static int alarmed(boolean alarm) {
		return alarm ? AMBER : GREEN;
	}

	private static String f0(double v) {
		return String.valueOf(Math.round(v));
	}

	private static String f1(double v) {
		return String.format(Locale.ROOT, "%.1f", v);
	}
}
