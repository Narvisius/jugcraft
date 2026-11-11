package io.github.jimbozoomer.jugcraft.client;

import static io.github.jimbozoomer.jugcraft.client.NuclearRenderer.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.NuclearRenderer.Frame;
import io.github.jimbozoomer.jugcraft.client.NuclearRenderer.Mesh;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The diesel generator set and its starting air system as the game has them (round 31, tools/diesel_set.py): their
 * plates, signs and gauge faces on their own texture (textures/entity/diesel_legends.png), drawn in a pass of their
 * own; their live parts where the drawing says they are ("live"): the coupling and the generator's fan turning with
 * the shaft, the gauge needles at their readings (swept from 225 degrees round to -45 across the dial), the lamps, the
 * compressor's flywheel pulley turning behind its guard. Since round 31 part 3 also the room's own plant
 * (tools/diesel_room.py): the keep-warm skids', cooler's gauges and lamps, the expansion tank's sight glass and the
 * cooler's temperature valve pointer, all reading the generator the piece has joined; and the monorail's trolley, hoist
 * and hook where its pendant has driven them.
 */
final class DieselParts {
	static final Identifier LEGEND_TEXTURE = Jugcraft.id("textures/entity/diesel_legends.png");
	static final RenderType LEGEND_TYPE = RenderTypes.entityCutout(LEGEND_TEXTURE);
	private static final Map<Integer, Mesh> LEGENDS = new HashMap<>();
	private static final Map<Integer, Mesh[]> NAMED = new HashMap<>();
	/** Rated speed (a 14-pole 60 Hz generator) and the gauge board's full scales. */
	static final double RPM = 514;

	private DieselParts() {
	}

	/**
	 * What a piece's live parts show: running, its turning parts' angle (degrees), its pressure (psig), whether it is
	 * ready to start (the set) or joined to its set (the air pieces), and the set's engine readings (null: standing
	 * cold, as its item shows it).
	 */
	record Readings(boolean running, double angle, double air, boolean ready, boolean joined, EdgTelemetry.@Nullable Snapshot engine,
			double hoistX, double hoistDrop) {
		static final Readings STILL = new Readings(false, 0, 0, false, false, null, 2, 0);

		Readings(boolean running, double angle, double air, boolean ready, boolean joined, EdgTelemetry.@Nullable Snapshot engine) {
			this(running, angle, air, ready, joined, engine, 2, 0);
		}
	}

	/** A piece's plates, signs and gauge faces, its own and its label's (its anchor's variant), for the legend pass. */
	static Mesh legendMesh(EquipmentKind kind, int variant) {
		return LEGENDS.computeIfAbsent(kind.ordinal() << 4 | variant, k -> {
			Mesh mesh = new Mesh();
			Frame f = new Frame(mesh, true);
			f.legends = true;
			ControlBoards.still(kind, f);
			ControlBoards.state(kind, "v" + variant, f, false);
			return mesh;
		});
	}

	/** A piece's parts that differ by its label (still, lit). */
	static Mesh[] namedMesh(EquipmentKind kind, int variant) {
		return NAMED.computeIfAbsent(kind.ordinal() << 4 | variant, k -> {
			Mesh still = new Mesh(), lit = new Mesh();
			ControlBoards.state(kind, "v" + variant, new Frame(still, true), false);
			ControlBoards.state(kind, "v" + variant, new Frame(lit, true), true);
			return new Mesh[] {still, lit};
		});
	}

	/**
	 * The client's turn of a set's shaft or a compressor's pulley (degrees), its speed (0..1) brought up over
	 * {@code up} ticks after it starts and down over {@code down} after it stops; {@code turns} a second at full speed.
	 */
	static void spin(EquipmentBlockEntity e, Level level, float partialTick, float up, float down, float turns) {
		long nanos = System.nanoTime();
		float dt = e.animNanos == 0 ? 0 : Math.min(0.1F, (nanos - e.animNanos) / 1.0E9F);
		e.animNanos = nanos;
		float since = level.getGameTime() - e.runningSince + partialTick;
		e.animSpeed = e.running ? Math.min(1F, Math.max(e.animSpeed, since / up)) : Math.max(0F, e.animSpeed - dt * 20F / down);
		e.animAngle = (e.animAngle + e.animSpeed * 360F * turns * dt) % 360F;
	}

	/** A piece's live parts: what turns, the needles, the unlit lamps ({@code still}) and the lit ones ({@code lit}). */
	static void live(Frame still, Frame lit, EquipmentKind kind, Readings r) {
		JsonObject live = ControlBoards.live(kind);
		if (live == null) {
			return;
		}
		JsonObject mats = live.has("mats") ? live.getAsJsonObject("mats") : new JsonObject();
		if (live.has("coupling")) {
			JsonArray shaft = live.getAsJsonArray("shaft");
			double ax = shaft.get(0).getAsDouble(), ay = shaft.get(1).getAsDouble();
			coupling(still, ax, ay, live.getAsJsonObject("coupling"), r.angle());
			fan(still, ax, ay, live.getAsJsonObject("fan"), r.angle());
		}
		if (live.has("pulley")) {
			pulley(still, live.getAsJsonObject("pulley"), r.angle());
		}
		float[] needle = mats.has("needle") ? ControlBoards.region(mats.get("needle")) : RED_IRON;
		float[] pointer = mats.has("pointer") ? ControlBoards.region(mats.get("pointer")) : BLACK;
		for (JsonElement e : live.getAsJsonArray("gauges")) {
			JsonObject g = e.getAsJsonObject();
			JsonArray c = g.getAsJsonArray("c");
			double value = Math.max(0, Math.min(1, gauge(g.get("name").getAsString(), r)));
			double sweep = g.has("sweep") ? g.get("sweep").getAsDouble() : 270;
			needle(still, c.get(0).getAsDouble(), c.get(1).getAsDouble(), c.get(2).getAsDouble(), g.get("len").getAsDouble(), value, sweep,
					sweep == 270 ? needle : pointer);
		}
		if (live.has("levels")) {
			// A sight glass: the water in it up to its level.
			float[] water = mats.has("water") ? ControlBoards.region(mats.get("water")) : WATER;
			for (JsonElement e : live.getAsJsonArray("levels")) {
				JsonObject lv = e.getAsJsonObject();
				double y0 = lv.get("y0").getAsDouble(), y1 = lv.get("y1").getAsDouble();
				double top = y0 + (y1 - y0) * Math.max(0, Math.min(1, level(lv.get("name").getAsString(), r)));
				PlantDecor.front(still, lv.get("x0").getAsDouble(), y0, lv.get("x1").getAsDouble(), top, lv.get("z").getAsDouble(), water);
			}
		}
		if (live.has("hoist")) {
			hoist(still, live.getAsJsonObject("hoist"), mats, r.hoistX(), r.hoistDrop());
		}
		for (JsonElement e : live.getAsJsonArray("lamps")) {
			JsonObject lamp = e.getAsJsonObject();
			JsonArray c = lamp.getAsJsonArray("c");
			boolean on = lamp(lamp.get("name").getAsString(), r);
			double x = c.get(0).getAsDouble(), y = c.get(1).getAsDouble(), z = c.get(2).getAsDouble(), s = lamp.get("r").getAsDouble();
			PlantDecor.front(on ? lit : still, x - s, y - s, x + s, y + s, z, ControlBoards.region(lamp.get(on ? "on" : "off")));
		}
	}

	/**
	 * A gauge's reading as a share of its dial: the set's engine speed (0 to 600 rpm), lube oil pressure and fuel oil
	 * pressure (0 to 100 psig), lube oil and jacket water temperatures (50 to 250 degrees F), starting air (0 to 400
	 * psig); a receiver's air and a compressor's discharge (0 to 400 psig).
	 */
	static double gauge(String name, Readings r) {
		EdgTelemetry.Snapshot o = r.engine();
		return switch (name) {
			case "ENGINE SPEED" -> o == null ? 0 : o.rpm / 600;
			case "LUBE OIL PRESS" -> o == null ? 0 : o.lube * 14.5 / 100;
			case "LUBE OIL TEMP" -> o == null ? 0.3 : (o.jacket * 1.8 + 32 + 4 - 50) / 200;
			case "JACKET WATER TEMP" -> o == null ? 0.3 : (o.jacket * 1.8 + 32 - 50) / 200;
			case "FUEL OIL PRESS" -> o == null ? 0 : 48 * Math.min(1, o.rpm / RPM * 3) / 100;
			case "STARTING AIR PRESS", "AIR PRESS", "DISCH PRESS" -> r.air() / 400;
			default -> {
				// The room's own plant (round 31 part 3): psig on 0 to 100, degrees F on 50 to 250, the valve as it is.
				double v = room(name, r);
				yield name.equals("VALVE POSITION") ? v : name.endsWith("PRESS") ? v / 100 : (v - 50) / 200;
			}
		};
	}

	/**
	 * The room's own plant's gauges (round 31 part 3; degrees F, psig; the valve 0 all to bypass, 1 all to the cooler)
	 * from its generator: standing by, the keep-warm skids heat and circulate the jacket water and the lube oil (its
	 * prelube pump at 30 psig) and the cooler is all bypassed; running, the skids stand idle and the engine's own pumps
	 * drive the water through the cooler, its valve opening as the jacket water warms past 160 degrees F. Not joined:
	 * all at the room's 70 degrees F, nothing flowing.
	 */
	static double room(String name, Readings r) {
		EdgTelemetry.Snapshot o = r.engine();
		if (o == null || !r.joined()) {
			return name.endsWith("PRESS") ? 0 : name.equals("VALVE POSITION") ? 0 : 70;
		}
		double jw = o.jacket * 1.8 + 32, valve = o.running ? Math.max(0, Math.min(1, (jw - 160) / 25)) : 0;
		return switch (name) {
			case "KW OUT TEMP" -> o.running ? jw : jw + 8;
			case "KW IN TEMP" -> o.running ? jw : jw - 4;
			case "LO KW TEMP" -> o.running ? jw + 4 : jw - 6;
			case "LO KW PRESS" -> o.running ? o.lube * 14.5 : 30;
			case "JW IN TEMP" -> jw;
			case "JW OUT TEMP" -> o.running ? jw - 18 * valve : 95;
			case "SW IN TEMP" -> 72;
			case "SW OUT TEMP" -> o.running ? 72 + 22 * valve : 73;
			case "VALVE POSITION" -> valve;
			default -> 0;
		};
	}

	/** A sight glass's level (0..1): the expansion tank's rises as the jacket water warms. */
	static double level(String name, Readings r) {
		EdgTelemetry.Snapshot o = r.engine();
		double jw = o == null || !r.joined() ? 70 : o.jacket * 1.8 + 32;
		return 0.4 + 0.3 * Math.max(0, Math.min(1, (jw - 100) / 90));
	}

	/** Whether a lamp is lit: the set's RUNNING and READY TO START, a compressor's RUNNING and STOPPED, the dryer's POWER. */
	static boolean lamp(String name, Readings r) {
		return switch (name) {
			case "running" -> r.running();
			case "ready" -> !r.running() && r.ready();
			case "stopped" -> !r.running() && r.joined();
			case "power" -> r.joined();
			// The keep-warm skids (round 31 part 3): their pumps run while the engine stands by, the heaters on and off
			// on their thermostats.
			case "pump", "prelube" -> r.joined() && !r.running();
			case "heater" -> r.joined() && !r.running() && r.engine() != null && ((long) (r.engine().now / 400)) % 5 < 3;
			default -> false;
		};
	}

	/** A needle in the gauge's face plane (facing -z), from its hub out to {@code len}, at a share of the 270 degree sweep. */
	static void needle(Frame f, double cx, double cy, double z, double len, double value, float[] uv) {
		needle(f, cx, cy, z, len, value, 270, uv);
	}

	/** A needle (or a valve's position pointer) swept {@code sweep} degrees about straight up. */
	static void needle(Frame f, double cx, double cy, double z, double len, double value, double sweep, float[] uv) {
		double a = Math.toRadians(90 + sweep / 2 - sweep * value), dx = Math.cos(a), dy = Math.sin(a), w = len * 0.06;
		double px = -dy * w, py = dx * w;
		Vec3 n = v(0, 0, -1);
		f.quad(v(cx + px - dx * len * 0.18, cy + py - dy * len * 0.18, z), v(cx - px - dx * len * 0.18, cy - py - dy * len * 0.18, z),
				v(cx + dx * len - px * 0.3, cy + dy * len - py * 0.3, z), v(cx + dx * len + px * 0.3, cy + dy * len + py * 0.3, z), n, uv[0], uv[1], uv[2], uv[3]);
		f.disc(v(cx, cy, z - 0.002), n, len * 0.12, 8, BLACK);
	}

	/**
	 * The monorail's hoist (round 31 part 3) at {@code tx} along its beam, its hook {@code drop} blocks below its rest:
	 * the trolley's side plates either side of the beam, its four wheels on the bottom flange, the load bar under it and
	 * the collector arm out to the conductor bar; the electric chain hoist (its body, ribs, motor and fan cowl, gear
	 * case, control box, chain bag); the two falls of chain down to the hook block and its hook.
	 */
	static void hoist(Frame f, JsonObject h, JsonObject mats, double tx, double drop) {
		double rail = h.get("rail").getAsDouble(), z = h.get("z").getAsDouble(), fl = h.get("flange").getAsDouble(), bar = h.get("bar").getAsDouble();
		float[] body = mats.has("body") ? ControlBoards.region(mats.get("body")) : PAINT_YELLOW, dark = GUNMETAL;
		for (int s = -1; s <= 1; s += 2) {
			double za = z + s * (fl + 0.04);
			f.box(tx - 0.36, rail - 0.16, Math.min(za, za + s * 0.04), tx + 0.36, rail + 0.26, Math.max(za, za + s * 0.04), body);
			for (int k = -1; k <= 1; k += 2) {
				double x = tx + k * 0.2, zw = z + s * 0.1;
				f.cylinder(v(x, rail + 0.08, zw - 0.035), v(x, rail + 0.08, zw + 0.035), 0.08, 10, dark, true);
				f.cylinder(v(x, rail + 0.08, z + s * 0.06), v(x, rail + 0.08, z + s * (fl + 0.09)), 0.022, 6, STAINLESS, true);
			}
		}
		f.box(tx - 0.08, rail - 0.28, z - fl - 0.04, tx + 0.08, rail - 0.14, z + fl + 0.04, body);
		f.box(tx - 0.03, rail + 0.12, bar - 0.03, tx + 0.03, rail + 0.16, z - fl - 0.04, dark);
		f.box(tx - 0.04, rail - 0.4, z - 0.04, tx + 0.04, rail - 0.28, z + 0.04, dark);
		f.box(tx - 0.3, rail - 0.95, z - 0.24, tx + 0.3, rail - 0.4, z + 0.24, body);
		for (int k = 0; k < 5; k++) {
			double xr = tx - 0.24 + k * 0.12;
			f.box(xr - 0.012, rail - 0.93, z - 0.255, xr + 0.012, rail - 0.42, z + 0.255, body);
		}
		f.cylinder(v(tx - 0.3, rail - 0.67, z), v(tx - 0.62, rail - 0.67, z), 0.2, 14, body, false);
		f.cylinder(v(tx - 0.62, rail - 0.67, z), v(tx - 0.68, rail - 0.67, z), 0.17, 14, dark, true);
		f.box(tx + 0.3, rail - 0.9, z - 0.2, tx + 0.44, rail - 0.45, z + 0.2, body);
		f.box(tx - 0.15, rail - 0.82, z - 0.33, tx + 0.15, rail - 0.55, z - 0.24, dark);
		f.box(tx + 0.1, rail - 1.45, z + 0.25, tx + 0.38, rail - 0.95, z + 0.5, BLACK);
		double yh = rail - 1.3 - drop;
		for (int k = -1; k <= 1; k += 2) {
			double dz = k * 0.05;
			f.box(tx - 0.02, yh + 0.3, z + dz - 0.02, tx + 0.02, rail - 0.95, z + dz + 0.02, dark);
		}
		f.box(tx - 0.16, yh, z - 0.1, tx + 0.16, yh + 0.32, z + 0.1, body);
		f.cylinder(v(tx, yh, z), v(tx, yh - 0.14, z), 0.035, 8, STAINLESS, true);
		f.box(tx - 0.045, yh - 0.46, z - 0.035, tx + 0.045, yh - 0.14, z + 0.035, STAINLESS);
		f.box(tx - 0.045, yh - 0.55, z - 0.035, tx + 0.22, yh - 0.46, z + 0.035, STAINLESS);
		f.box(tx + 0.13, yh - 0.46, z - 0.035, tx + 0.22, yh - 0.3, z + 0.035, STAINLESS);
	}

	/** The set's coupling in its guard: two flanged hubs on a spacer, eight bolts through them, a white mark to see it turn. */
	static void coupling(Frame f, double ax, double ay, JsonObject c, double angle) {
		double z0 = c.get("z0").getAsDouble(), z1 = c.get("z1").getAsDouble(), r = c.get("r").getAsDouble(), turn = Math.toRadians(angle);
		f.cylinder(v(ax, ay, z0), v(ax, ay, z0 + 0.18), r, 16, STAINLESS_DARK, true);
		f.cylinder(v(ax, ay, z1 - 0.18), v(ax, ay, z1), r, 16, STAINLESS_DARK, true);
		f.cylinder(v(ax, ay, z0 + 0.18), v(ax, ay, z1 - 0.18), r * 0.55, 12, GUNMETAL, false);
		for (int k = 0; k < 8; k++) {
			double b = turn + Math.PI * 2 * k / 8, x = ax + r * 0.8 * Math.cos(b), y = ay + r * 0.8 * Math.sin(b);
			f.cylinder(v(x, y, z0 - 0.04), v(x, y, z1 + 0.04), 0.05, 6, STAINLESS, true);
		}
		double mx = ax + (r - 0.06) * Math.cos(turn), my = ay + (r - 0.06) * Math.sin(turn);
		f.box(mx - 0.05, my - 0.05, z0 - 0.012, mx + 0.05, my + 0.05, z0 + 0.15, PAINT_WHITE);
	}

	/** The generator's cooling fan behind its guard at the far end: six blades round a hub (one face each: nothing culls). */
	static void fan(Frame f, double ax, double ay, JsonObject fan, double angle) {
		double z = fan.get("z").getAsDouble(), r = fan.get("r").getAsDouble(), turn = Math.toRadians(angle);
		for (int k = 0; k < 6; k++) {
			double b = turn + Math.PI * 2 * k / 6, cx = Math.cos(b), cy = Math.sin(b), wx = -cy * 0.07, wy = cx * 0.07;
			f.quad(v(ax + cx * 0.14 - wx, ay + cy * 0.14 - wy, z), v(ax + cx * r - wx, ay + cy * r - wy, z), v(ax + cx * r + wx, ay + cy * r + wy, z),
					v(ax + cx * 0.14 + wx, ay + cy * 0.14 + wy, z), v(0, 0, 1), GUNMETAL[0], GUNMETAL[1], GUNMETAL[2], GUNMETAL[3]);
		}
		f.cylinder(v(ax, ay, z - 0.06), v(ax, ay, z + 0.06), 0.15, 10, GUNMETAL, true);
	}

	/** A compressor's flywheel pulley (its spokes blow over its coolers): rim, five spokes, hub, turning. */
	static void pulley(Frame f, JsonObject p, double angle) {
		JsonArray c = p.getAsJsonArray("c");
		double cx = c.get(0).getAsDouble(), cy = c.get(1).getAsDouble(), cz = c.get(2).getAsDouble(), r = p.get("r").getAsDouble(), w = p.get("w").getAsDouble();
		double front = cz - w / 2, turn = Math.toRadians(angle);
		f.cylinder(v(cx, cy, front), v(cx, cy, cz + w / 2), r, 20, GUNMETAL, false);
		f.disc(v(cx, cy, front - 0.004), v(0, 0, -1), r * 0.84, 20, BLACK);
		for (int k = 0; k < 5; k++) {
			double b = turn + Math.PI * 2 * k / 5, dx = Math.cos(b), dy = Math.sin(b), wx = -dy * 0.045, wy = dx * 0.045, z = front - 0.012;
			f.quad(v(cx + dx * 0.08 - wx, cy + dy * 0.08 - wy, z), v(cx + dx * 0.08 + wx, cy + dy * 0.08 + wy, z),
					v(cx + dx * r * 0.84 + wx, cy + dy * r * 0.84 + wy, z), v(cx + dx * r * 0.84 - wx, cy + dy * r * 0.84 - wy, z), v(0, 0, -1),
					GUNMETAL[0], GUNMETAL[1], GUNMETAL[2], GUNMETAL[3]);
		}
		f.cylinder(v(cx, cy, front - 0.05), v(cx, cy, front), 0.09, 10, STAINLESS_DARK, true);
	}
}
