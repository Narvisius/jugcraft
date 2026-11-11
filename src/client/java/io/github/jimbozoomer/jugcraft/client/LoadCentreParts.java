package io.github.jimbozoomer.jugcraft.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.NuclearRenderer.Frame;
import io.github.jimbozoomer.jugcraft.client.NuclearRenderer.Mesh;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind;
import io.github.jimbozoomer.jugcraft.nuclear.LoadCentre;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * The 480 V load centre as the game draws it (older rooms round 1, the switchgear room; tools/load_centre.py draws it,
 * nuclear/LoadCentre.java works it): its plates, labels and windows on its own texture
 * (textures/entity/load_centre_legends.png), drawn in a pass of their own; its live parts where the drawing says
 * ("live"): each breaker's lamps, its indicators' flags (contacts, spring, racking position, the trip unit's target),
 * its control switch's handle, its front (out by its racking position), the meters' pointers, the transformer's fans
 * turning, the fans' switch.
 */
final class LoadCentreParts {
	static final Identifier LEGEND_TEXTURE = Jugcraft.id("textures/entity/load_centre_legends.png");
	static final RenderType LEGEND_TYPE = RenderTypes.entityCutout(LEGEND_TEXTURE);
	private static final EquipmentKind KIND = EquipmentKind.SWITCHGEAR_LINEUP;
	private static final Map<Integer, Mesh> LEGENDS = new HashMap<>();
	private static final Map<Integer, Mesh[]> NAMED = new HashMap<>();
	private static final Vec3 N = new Vec3(0, 0, -1);

	private LoadCentreParts() {
	}

	/**
	 * What the lineup shows (from its block entity): its breakers, its load centre state, its bus's charge, the time
	 * (game ticks, with the partial tick) and the fans' turn.
	 */
	record View(int breakers, LoadCentre.State s, float charge, double time, double fanAngle) {
		boolean closed(int b) {
			return (breakers & 1 << b) != 0;
		}

		boolean signal(String name) {
			int dot = name.indexOf('.');
			String what = name.substring(dot + 1);
			if (name.startsWith("xf.")) {
				return switch (what) {
					case "fans" -> s.fans;
					case "alarm" -> s.alarm;
					case "trip" -> s.temp > LoadCentre.TRIP_TEMP;
					default -> false;
				};
			}
			int b = Integer.parseInt(name.substring(1, dot));
			boolean cp = s.controlPower(b);
			return switch (what) {
				case "closed" -> cp && closed(b);
				case "open" -> cp && !closed(b);
				case "trip" -> cp && s.target(b);
				case "status" -> cp && ((long) time / 20) % 2 == 0;
				case "pickup" -> cp && (s.pickup & 1 << b) != 0 && ((long) time / 5) % 2 == 0;
				default -> false;
			};
		}

		int state(String name) {
			int dot = name.indexOf('.');
			int b = Integer.parseInt(name.substring(1, dot));
			return switch (name.substring(dot + 1)) {
				case "contacts" -> closed(b) ? 1 : 0;
				case "spring" -> s.charged(b, (long) time) ? 1 : 0;
				case "rack" -> s.rack[b];
				case "target" -> s.target(b) ? 1 : 0;
				default -> 0;
			};
		}

		double value(String name) {
			if (name.equals("main.amps")) {
				return s.amps[0];
			}
			if (name.equals("main.volts")) {
				return s.volts;
			}
			int dot = name.indexOf('.');
			return name.startsWith("b") ? s.amps[Integer.parseInt(name.substring(1, dot))] : 0;
		}

		/** The words a text shows (the trip units' amps, the counters, the power monitor's lines, the temperatures). */
		String text(String id) {
			int dot = id.indexOf('.');
			String what = id.substring(dot + 1);
			if (id.startsWith("pm.")) {
				double kw = s.amps[0] * s.volts * 1.732 * 0.86 / 1000;
				return switch (what) {
					case "0" -> String.format("VOLTS L-L   %5d V", s.volts);
					case "1" -> String.format("AMPS AVG    %5d A", s.amps[0]);
					case "2" -> String.format("POWER       %5.0f kW", kw);
					default -> String.format("BUS CHARGE  %5.0f %%", charge * 100);
				};
			}
			if (id.startsWith("xf.t")) {
				return String.format("%.0f °C", s.phase(Integer.parseInt(what.substring(1))));
			}
			int b = Integer.parseInt(id.substring(1, dot));
			return switch (what) {
				case "lcd" -> s.controlPower(b) ? s.amps[b] + " A" : "";
				case "ops" -> String.format("%05d", s.ops[b]);
				default -> "";
			};
		}

		/** A part on breaker b's front: how far out its racking position has it (z, toward you). */
		double dz(JsonObject e) {
			if (!e.has("b")) {
				return 0;
			}
			int b = e.get("b").getAsInt();
			JsonArray dzs = front(b);
			return dzs == null ? 0 : dzs.get(s.rack[b]).getAsDouble();
		}
	}

	private static final Map<Integer, JsonArray> FRONT_DZ = new HashMap<>();

	private static JsonArray front(int b) {
		if (FRONT_DZ.isEmpty()) {
			JsonObject live = ControlBoards.live(KIND);
			if (live != null) {
				for (JsonElement e : live.getAsJsonArray("fronts")) {
					JsonObject f = e.getAsJsonObject();
					FRONT_DZ.put(f.get("b").getAsInt(), f.getAsJsonArray("dz"));
				}
			}
		}
		return FRONT_DZ.get(b);
	}

	/** The plates, labels and windows of the lineup and of its label (its anchor's variant), for the legend pass. */
	static Mesh legendMesh(int variant) {
		return LEGENDS.computeIfAbsent(variant, k -> {
			Mesh mesh = new Mesh();
			Frame f = new Frame(mesh, true);
			f.legends = true;
			ControlBoards.still(KIND, f);
			ControlBoards.state(KIND, "v" + variant, f, false);
			return mesh;
		});
	}

	/** The parts that differ by its label, in the sheet's materials (still, lit). */
	static Mesh[] namedMesh(int variant) {
		return NAMED.computeIfAbsent(variant, k -> {
			Mesh still = new Mesh(), lit = new Mesh();
			ControlBoards.state(KIND, "v" + variant, new Frame(still, true), false);
			ControlBoards.state(KIND, "v" + variant, new Frame(lit, true), true);
			return new Mesh[] {still, lit};
		});
	}

	/**
	 * The live parts as the view has them: still (the room's light), lit (full bright) in the sheet's materials, and
	 * leg (on the legend texture, in the room's light).
	 */
	static void live(Frame still, Frame lit, Frame leg, View v) {
		JsonObject live = ControlBoards.live(KIND);
		if (live == null) {
			return;
		}
		for (JsonElement el : live.getAsJsonArray("fronts")) {
			JsonObject fr = el.getAsJsonObject();
			int b = fr.get("b").getAsInt();
			double dz = fr.getAsJsonArray("dz").get(v.s().rack[b]).getAsDouble();
			still.oz = dz;
			leg.oz = dz;
			ControlBoards.ops(still, fr.getAsJsonArray("ops"));
			ControlBoards.ops(leg, fr.getAsJsonArray("ops"));
			still.oz = 0;
			leg.oz = 0;
		}
		for (JsonElement el : live.getAsJsonArray("lamps")) {
			JsonObject l = el.getAsJsonObject();
			boolean on = v.signal(l.get("s").getAsString());
			JsonElement mat = on ? l.get("on") : l.get("off");
			Frame f = ControlBoards.legend(mat) ? leg : on ? lit : still;
			quad(f, l.getAsJsonArray("q"), mat, v.dz(l));
		}
		for (JsonElement el : live.getAsJsonArray("flags")) {
			JsonObject fl = el.getAsJsonObject();
			JsonArray mats = fl.getAsJsonArray("m");
			JsonElement mat = mats.get(Math.min(mats.size() - 1, v.state(fl.get("s").getAsString())));
			quad(ControlBoards.legend(mat) ? leg : still, fl.getAsJsonArray("q"), mat, v.dz(fl));
		}
		for (JsonElement el : live.getAsJsonArray("handles")) {
			JsonObject h = el.getAsJsonObject();
			String id = h.get("id").getAsString();
			String key;
			if (id.equals("fans")) {
				key = v.s().fanManual ? "1" : "0";
			} else {
				int turn = v.s().handle(Integer.parseInt(id.substring(2)), (long) v.time());
				key = turn < 0 ? "t" : turn > 0 ? "c" : "n";
			}
			JsonObject ops = h.getAsJsonObject("ops");
			if (ops.has(key)) {
				ControlBoards.ops(still, ops.getAsJsonArray(key));
			}
		}
		float[] black = NuclearRenderer.swatch(0, 2);
		for (JsonElement el : live.getAsJsonArray("needles")) {
			JsonObject n = el.getAsJsonObject();
			double lo = n.get("lo").getAsDouble(), hi = n.get("hi").getAsDouble();
			double frac = Math.max(0, Math.min(1, (v.value(n.get("v").getAsString()) - lo) / (hi - lo)));
			double a0 = n.get("a0").getAsDouble(), a1 = n.get("a1").getAsDouble();
			double a = Math.toRadians(a0 + (a1 - a0) * frac);
			double cx = n.get("cx").getAsDouble(), cy = n.get("cy").getAsDouble(), z = n.get("z").getAsDouble(), r = n.get("r").getAsDouble();
			double ux = Math.cos(a), uy = Math.sin(a), nx = -uy * 0.0035, ny = ux * 0.0035;
			double tx = cx + ux * r, ty = cy + uy * r, bx = cx - ux * r * 0.15, by = cy - uy * r * 0.15;
			still.quad(new Vec3(bx + nx, by + ny, z), new Vec3(bx - nx, by - ny, z), new Vec3(tx - nx * 0.3, ty - ny * 0.3, z),
					new Vec3(tx + nx * 0.3, ty + ny * 0.3, z), N, black[0], black[1], black[2], black[3]);
			double h = 0.008;
			still.quad(new Vec3(cx - h, cy + h, z - 0.0005), new Vec3(cx - h, cy - h, z - 0.0005), new Vec3(cx + h, cy - h, z - 0.0005),
					new Vec3(cx + h, cy + h, z - 0.0005), N, black[0], black[1], black[2], black[3]);
		}
		float[] blade = NuclearRenderer.material("gunmetal");
		for (JsonElement el : live.getAsJsonArray("fans")) {
			JsonObject fan = el.getAsJsonObject();
			JsonArray c = fan.getAsJsonArray("c");
			double cx = c.get(0).getAsDouble(), cy = c.get(1).getAsDouble(), z = c.get(2).getAsDouble(), r = fan.get("r").getAsDouble() * 0.48;
			int n = fan.get("n").getAsInt();
			for (int k = 0; k < n; k++) {
				double a = Math.toRadians(v.fanAngle() + k * 360.0 / n);
				double ux = Math.cos(a), uy = Math.sin(a), vx = -uy, vy = ux;
				still.quad(new Vec3(cx + ux * 0.05 + vx * 0.02, cy + uy * 0.05 + vy * 0.02, z), new Vec3(cx + ux * 0.05 - vx * 0.02, cy + uy * 0.05 - vy * 0.02, z),
						new Vec3(cx + ux * r - vx * 0.05, cy + uy * r - vy * 0.05, z), new Vec3(cx + ux * r + vx * 0.05, cy + uy * r + vy * 0.05, z), N,
						blade[0], blade[1], blade[2], blade[3]);
			}
		}
	}

	private static void quad(Frame f, JsonArray q, JsonElement mat, double dz) {
		float[] uv = ControlBoards.region(mat);
		f.quad(new Vec3(q.get(0).getAsDouble(), q.get(1).getAsDouble(), q.get(2).getAsDouble() + dz),
				new Vec3(q.get(3).getAsDouble(), q.get(4).getAsDouble(), q.get(5).getAsDouble() + dz),
				new Vec3(q.get(6).getAsDouble(), q.get(7).getAsDouble(), q.get(8).getAsDouble() + dz),
				new Vec3(q.get(9).getAsDouble(), q.get(10).getAsDouble(), q.get(11).getAsDouble() + dz), N, uv[0], uv[1], uv[2], uv[3]);
	}

	/** A material by name (for a JSON-free caller). */
	static JsonElement mat(String name) {
		return new JsonPrimitive(name);
	}
}
