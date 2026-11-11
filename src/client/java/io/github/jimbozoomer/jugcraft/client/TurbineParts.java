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
import io.github.jimbozoomer.jugcraft.nuclear.TurbineHall;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * The main turbine-generator and its moisture separator reheaters as the game has them (turbine hall part 1,
 * tools/turbine_set.py): their plates, signs and gauge faces on their own texture (textures/entity/turbine_legends.png),
 * drawn in a pass of their own; their live parts where the drawing says they are ("live"): the front standard's speed
 * figures, gauges and TRIPPED / LATCHED lamps, the manual trip's handle (pulled out once tripped) and its guard (up for
 * five seconds once lifted), the throttle, governor and intercept valves' stems with their collars and pointers riding
 * up as they open, the turning gear's lever and lamps, the shaft's mark and the exciter's diode wheel turning (a blur at
 * speed), the reheaters' shell pressure and reheat temperature. Turbine hall part 2 (tools/feed_train.py): the feed
 * train's and the oil plant's gauges, lamps, figures (a main feed pump's speed, the hydrogen's purity), sight glasses and
 * hotwell glass, and the pumps' couplings turning while they run (TurbineHall works out what they show).
 */
final class TurbineParts {
	static final Identifier LEGEND_TEXTURE = Jugcraft.id("textures/entity/turbine_legends.png");
	static final RenderType LEGEND_TYPE = RenderTypes.entityCutout(LEGEND_TEXTURE);
	private static final Map<Integer, Mesh> LEGENDS = new HashMap<>();
	private static final Map<Integer, Mesh[]> NAMED = new HashMap<>();

	private TurbineParts() {
	}

	/**
	 * What the live parts show: the turbine's speed (rpm), load (fraction of rated), its {@link TurbineHall} flags, how far
	 * its trip guard is lifted (0..1) and its shaft's turn as drawn (degrees).
	 */
	record Readings(double rpm, double load, int flags, double guard, double angle) {
		static final Readings STILL = new Readings(0, 0, 0, 0, 0);

		boolean on(int flag) {
			return (flags & flag) != 0;
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
	 * The client's turn of the turbine's shaft (degrees): at the turning gear's 3 rpm it is seen to turn; faster, the
	 * mark and the wheel are drawn as a blur instead.
	 */
	static void spin(EquipmentBlockEntity e, float rpm) {
		long nanos = System.nanoTime();
		float dt = e.animNanos == 0 ? 0 : Math.min(0.1F, (nanos - e.animNanos) / 1.0E9F);
		e.animNanos = nanos;
		e.animAngle = (e.animAngle + Math.min(rpm, 40F) * 6F * dt) % 360F;
	}

	/** A piece's live parts: the unlit ones ({@code still}) and the lit ones ({@code lit}). */
	static void live(Frame still, Frame lit, EquipmentKind kind, Readings r) {
		JsonObject live = ControlBoards.live(kind);
		if (live == null) {
			return;
		}
		JsonObject mats = live.has("mats") ? live.getAsJsonObject("mats") : new JsonObject();
		float[] needle = mats.has("needle") ? ControlBoards.region(mats.get("needle")) : RED_IRON;
		for (JsonElement e : live.getAsJsonArray("gauges")) {
			JsonObject g = e.getAsJsonObject();
			JsonArray c = g.getAsJsonArray("c"), n = g.getAsJsonArray("n");
			double value = Math.max(0, Math.min(1, gauge(g.get("name").getAsString(), r)));
			double x = c.get(0).getAsDouble(), y = c.get(1).getAsDouble(), z = c.get(2).getAsDouble(), len = g.get("len").getAsDouble();
			if (n.get(2).getAsDouble() != 0) {
				DieselParts.needle(still, x, y, z, len, value, needle);
			} else {
				sideNeedle(still, x, y, z, len, value, (int) Math.signum(n.get(0).getAsDouble()), needle);
			}
		}
		for (JsonElement e : live.getAsJsonArray("lamps")) {
			JsonObject lamp = e.getAsJsonObject();
			JsonArray c = lamp.getAsJsonArray("c"), n = lamp.getAsJsonArray("n");
			boolean on = lamp(lamp.get("name").getAsString(), r);
			double x = c.get(0).getAsDouble(), y = c.get(1).getAsDouble(), z = c.get(2).getAsDouble(), s = lamp.get("r").getAsDouble();
			float[] uv = ControlBoards.region(lamp.get(on ? "on" : "off"));
			Frame f = on ? lit : still;
			if (n.get(2).getAsDouble() != 0) {
				PlantDecor.front(f, x - s, y - s, x + s, y + s, z, uv);
			} else {
				sideQuad(f, x, y - s, y + s, z - s, z + s, (int) Math.signum(n.get(0).getAsDouble()), uv);
			}
		}
		if (live.has("digits")) {
			for (JsonElement e : live.getAsJsonArray("digits")) {
				JsonObject d = e.getAsJsonObject();
				JsonArray c = d.getAsJsonArray("c");
				double x = c.get(0).getAsDouble(), y = c.get(1).getAsDouble(), z = c.get(2).getAsDouble();
				double w = d.get("w").getAsDouble(), h = d.get("h").getAsDouble();
				String text = switch (d.get("name").getAsString()) {
					case "mfp_speed" -> r.on(TurbineHall.RUN) ? String.format("%4d", Math.round(r.rpm())) : "   0";
					case "h2_purity" -> String.format(java.util.Locale.ROOT, "%4.1f", TurbineHall.value("H2 PURITY", r.flags(), r.rpm(), r.load()));
					default -> r.on(TurbineHall.PRESENT) ? String.format("%4d", Math.round(r.rpm())) : "   0";
				};
				LiveBoards.sevenSegment(lit, x - w / 2 + 0.03, y - h / 2 + 0.02, x + w / 2 - 0.03, y + h / 2 - 0.02, z, text, LAMP_RED);
			}
		}
		if (live.has("stems")) {
			float[] stem = mats.has("stem") ? ControlBoards.region(mats.get("stem")) : STAINLESS;
			float[] pointer = mats.has("pointer") ? ControlBoards.region(mats.get("pointer")) : RED_IRON;
			for (JsonElement e : live.getAsJsonArray("stems")) {
				stem(still, e.getAsJsonObject(), open(e.getAsJsonObject().get("name").getAsString(), r), stem, pointer);
			}
		}
		if (live.has("trip")) {
			trip(still, live.getAsJsonObject("trip"), r);
		}
		if (live.has("lever")) {
			lever(still, live.getAsJsonObject("lever"), r.on(TurbineHall.TURNING_GEAR));
		}
		if (live.has("wheel")) {
			wheel(still, live.getAsJsonObject("wheel"), r);
		}
		if (live.has("levels")) {
			float[] liquid = mats.has("water") ? ControlBoards.region(mats.get("water")) : WATER;
			for (JsonElement e : live.getAsJsonArray("levels")) {
				JsonObject l = e.getAsJsonObject();
				double y0 = l.get("y0").getAsDouble(), y1 = l.get("y1").getAsDouble();
				double top = y0 + (y1 - y0) * Math.max(0, Math.min(1, TurbineHall.level(l.get("name").getAsString(), r.flags())));
				if (l.has("z")) {
					PlantDecor.front(still, l.get("x0").getAsDouble(), y0, l.get("x1").getAsDouble(), top, l.get("z").getAsDouble(), liquid);
				} else {
					sideQuad(still, l.get("x").getAsDouble(), y0, top, l.get("z0").getAsDouble(), l.get("z1").getAsDouble(), l.get("side").getAsInt(), liquid);
				}
			}
		}
		if (live.has("couplings")) {
			for (JsonElement e : live.getAsJsonArray("couplings")) {
				coupling(still, e.getAsJsonObject(), mats, r);
			}
		}
		if (live.has("shaft")) {
			JsonObject sh = live.getAsJsonObject("shaft");
			JsonArray c = sh.getAsJsonArray("c");
			shaftMark(still, c.get(0).getAsDouble(), c.get(1).getAsDouble(), sh.get("z0").getAsDouble(), sh.get("z1").getAsDouble(),
					sh.get("r").getAsDouble(), r, mats.has("mark") ? ControlBoards.region(mats.get("mark")) : PAINT_WHITE);
		}
	}

	/**
	 * A gauge's reading as a share of its dial: the front standard's bearing oil (0 to 60 psig: the lube oil pumps run
	 * whenever the turbine is there), auto stop oil (0 to 150: held up while latched), EHC fluid (0 to 2,000); a
	 * reheater's shell pressure (0 to 250 psig) and reheat steam temperature (0 to 600 degrees F).
	 */
	static double gauge(String name, Readings r) {
		boolean present = r.on(TurbineHall.PRESENT);
		return switch (name) {
			case "BRG OIL" -> present ? 24 / 60.0 : 0;
			case "AUTO STOP OIL" -> r.on(TurbineHall.LATCHED) ? 125 / 150.0 : 0;
			case "EHC FLUID" -> present ? 1600 / 2000.0 : 0;
			case "SHELL PRESS" -> TurbineHall.shellPsig(r.load(), r.flags()) / 250;
			case "REHEAT OUT" -> TurbineHall.reheatF(r.load(), r.flags()) / 600;
			default -> {
				// Turbine hall part 2's gauges: their readings on their dials.
				double[] d = TurbineHall.dial(name);
				yield d == null ? 0 : (TurbineHall.value(name, r.flags(), r.rpm(), r.load()) - d[0]) / (d[1] - d[0]);
			}
		};
	}

	static boolean lamp(String name, Readings r) {
		return switch (name) {
			case "tripped" -> r.on(TurbineHall.PRESENT) && !r.on(TurbineHall.LATCHED);
			case "latched" -> r.on(TurbineHall.LATCHED);
			case "tg_engaged" -> r.on(TurbineHall.TURNING_GEAR);
			case "tg_motor" -> r.on(TurbineHall.TG_MOTOR);
			default -> TurbineHall.lamp(name, r.flags(), r.rpm());
		};
	}

	/**
	 * How far a valve stands open (0..1): the throttle valves and the intercept valves full open while latched, the
	 * governor valves following the load (a crack open rolling at speed off line).
	 */
	static double open(String name, Readings r) {
		if (!r.on(TurbineHall.LATCHED)) {
			return 0;
		}
		if (name.startsWith("gv")) {
			return r.on(TurbineHall.ONLINE) ? 0.15 + 0.85 * Math.min(1, r.load()) : r.rpm() > 100 ? 0.08 : 0.03;
		}
		return 1;
	}

	/**
	 * A valve's stem in its yoke: the stem itself, its connector (a dark collar) riding up the travel as the valve opens,
	 * and its red pointer out to the scale on the yoke post; an intercept valve's actuator stem along its axis with its
	 * collar.
	 */
	static void stem(Frame f, JsonObject s, double open, float[] stem, float[] pointer) {
		JsonArray c = s.getAsJsonArray("c");
		double x = c.get(0).getAsDouble(), y = c.get(1).getAsDouble(), z = c.get(2).getAsDouble(), r = s.get("r").getAsDouble();
		double top = s.get("top").getAsDouble(), travel = s.get("travel").getAsDouble();
		if (s.has("axis")) {
			JsonArray a = s.getAsJsonArray("axis");
			Vec3 axis = v(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble());
			Vec3 from = v(x, y, z), to = from.add(axis.scale(top - 0.04));
			f.cylinder(from, to, r, 8, stem, true);
			Vec3 m = from.add(axis.scale(0.1 + 0.15 * open));
			f.cylinder(m.subtract(axis.scale(0.02)), m.add(axis.scale(0.02)), r + 0.03, 8, pointer, true);
			return;
		}
		f.cylinder(v(x, y, z), v(x, top - 0.02, z), r, 8, stem, true);
		double cy = y + 0.12 + travel * open * 2.5;
		f.box(x - r - 0.02, cy - 0.03, z - r - 0.02, x + r + 0.02, cy + 0.03, z + r + 0.02, GUNMETAL);
		JsonArray p = s.getAsJsonArray("pointer");
		double px = p.get(0).getAsDouble(), pz = p.get(1).getAsDouble();
		f.box(Math.min(x, px), cy - 0.012, Math.min(z, pz) - 0.01, Math.max(x, px) + 0.01, cy + 0.012, Math.max(z, pz) + 0.01, pointer);
	}

	/**
	 * The manual trip: its red T-handle on its rod, in its housing while latched and pulled out once tripped; the yellow
	 * guard over it, hinged along its top, swung up and out while lifted.
	 */
	static void trip(Frame f, JsonObject t, Readings r) {
		JsonArray c = t.getAsJsonArray("c"), g = t.getAsJsonArray("guard");
		JsonObject mats = t.getAsJsonObject("mats");
		double x = c.get(0).getAsDouble(), y = c.get(1).getAsDouble(), z = c.get(2).getAsDouble();
		double out = r.on(TurbineHall.PRESENT) && !r.on(TurbineHall.LATCHED) ? 0.18 : 0;
		float[] rod = ControlBoards.region(mats.get("rod")), handle = ControlBoards.region(mats.get("handle"));
		f.cylinder(v(x, y, z), v(x, y, z - 0.08 - out), 0.025, 8, rod, true);
		f.box(x - 0.15, y - 0.03, z - 0.12 - out, x + 0.15, y + 0.03, z - 0.08 - out, handle);
		double gx = g.get(0).getAsDouble(), gy = g.get(1).getAsDouble(), gz = g.get(2).getAsDouble();
		double w = t.get("w").getAsDouble(), h = t.get("h").getAsDouble(), ang = Math.toRadians(100 * r.guard());
		double dy = -Math.cos(ang) * h, dz = -Math.sin(ang) * h;
		Vec3 down = v(0, dy / h, dz / h), across = v(1, 0, 0);
		ControlBoards.oriented(f, v(gx + w / 2, gy + dy / 2, gz + dz / 2), across, down, w / 2, h / 2, 0.012, mats.get("guard"));
		f.cylinder(v(gx, gy, gz), v(gx + w, gy, gz), 0.02, 8, ControlBoards.region(mats.get("hinge")), true);
	}

	/** The turning gear's lever on its boss: forward (toward the generator) engaged, back disengaged; its red knob. */
	static void lever(Frame f, JsonObject l, boolean engaged) {
		JsonArray p = l.getAsJsonArray("pivot");
		JsonObject mats = l.getAsJsonObject("mats");
		double x = p.get(0).getAsDouble(), y = p.get(1).getAsDouble(), z = p.get(2).getAsDouble(), len = l.get("len").getAsDouble();
		double ang = Math.toRadians(engaged ? 35 : -35);
		Vec3 d = v(0, Math.cos(ang), Math.sin(ang));
		f.cylinder(v(x, y, z), v(x - 0.06, y, z), 0.07, 12, ControlBoards.region(mats.get("boss")), true);
		Vec3 base = v(x - 0.05, y, z);
		ControlBoards.oriented(f, base.add(d.scale(len / 2)), d, v(1, 0, 0), len / 2, 0.025, 0.02, mats.get("arm"));
		Vec3 k = base.add(d.scale(len));
		f.cylinder(v(k.x + 0.02, k.y, k.z), v(k.x - 0.1, k.y, k.z), 0.05, 10, ControlBoards.region(mats.get("knob")), true);
	}

	/**
	 * The exciter's rotating rectifier seen through its port: its wheel, the diodes and fuses round its rim turning with
	 * the shaft; at speed a dark blur.
	 */
	static void wheel(Frame f, JsonObject w, Readings r) {
		JsonArray c = w.getAsJsonArray("c");
		JsonObject mats = w.getAsJsonObject("mats");
		double x = c.get(0).getAsDouble(), y = c.get(1).getAsDouble(), z = c.get(2).getAsDouble(), rad = w.get("r").getAsDouble();
		Vec3 n = v(0, 0, 1);
		f.disc(v(x, y, z), n, rad, 24, ControlBoards.region(mats.get("wheel")));
		if (r.rpm() > 40) {
			f.disc(v(x, y, z + 0.004), n, rad * 0.86, 24, BLACK);
			f.disc(v(x, y, z + 0.006), n, rad * 0.6, 24, ControlBoards.region(mats.get("wheel")));
			return;
		}
		double th = Math.toRadians(r.angle());
		float[] diode = ControlBoards.region(mats.get("diode")), fuse = ControlBoards.region(mats.get("fuse"));
		for (int k = 0; k < 12; k++) {
			double a = th + Math.PI * 2 * k / 12, cx = x + Math.cos(a) * rad * 0.75, cy = y + Math.sin(a) * rad * 0.75;
			f.box(cx - 0.04, cy - 0.04, z, cx + 0.04, cy + 0.04, z + 0.04, k % 2 == 0 ? diode : fuse);
		}
	}

	/**
	 * A pump's coupling between its motor and its pump (turbine hall part 2): the two hubs, the spacer between, the bolts
	 * round the hubs' flanges and a white mark across the spacer, turning with the shaft; while it runs, a blur (the bolts
	 * a ring, the mark a band).
	 */
	static void coupling(Frame f, JsonObject cp, JsonObject mats, Readings r) {
		JsonArray c = cp.getAsJsonArray("c"), a = cp.getAsJsonArray("axis");
		Vec3 o = v(c.get(0).getAsDouble(), c.get(1).getAsDouble(), c.get(2).getAsDouble());
		Vec3 ax = v(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble());
		double len = cp.get("len").getAsDouble(), rad = cp.get("r").getAsDouble();
		Vec3 end = o.add(ax.scale(len));
		float[] hub = ControlBoards.region(mats.get("hub")), spacer = ControlBoards.region(mats.get("spacer"));
		float[] bolt = ControlBoards.region(mats.get("bolt")), mark = ControlBoards.region(mats.get("mark"));
		f.cylinder(o, o.add(ax.scale(0.08)), rad, 16, hub, true);
		f.cylinder(end.subtract(ax.scale(0.08)), end, rad, 16, hub, true);
		f.cylinder(o.add(ax.scale(0.08)), end.subtract(ax.scale(0.08)), rad * 0.5, 12, spacer, false);
		Vec3 u = Math.abs(ax.x) < 0.9 ? v(1, 0, 0) : v(0, 1, 0);
		u = u.subtract(ax.scale(u.dot(ax))).normalize();
		Vec3 w = ax.cross(u);
		if (r.on(TurbineHall.RUN)) {
			for (double t : new double[] {0.04, len - 0.04}) {
				Vec3 m = o.add(ax.scale(t));
				f.cylinder(m.subtract(ax.scale(0.03)), m.add(ax.scale(0.03)), rad * 0.82, 16, bolt, false);
			}
			f.cylinder(o.add(ax.scale(len / 2 - 0.02)), o.add(ax.scale(len / 2 + 0.02)), rad * 0.5 + 0.004, 12, mark, false);
			return;
		}
		double th = Math.toRadians(r.angle());
		for (int k = 0; k < 6; k++) {
			double ang = th + Math.PI * 2 * k / 6;
			Vec3 q = o.add(u.scale(Math.cos(ang) * rad * 0.78)).add(w.scale(Math.sin(ang) * rad * 0.78));
			f.cylinder(q.subtract(ax.scale(0.03)), q.add(ax.scale(len + 0.03)), 0.025, 6, bolt, true);
		}
		Vec3 mk = o.add(u.scale(Math.cos(th) * (rad * 0.5 + 0.004))).add(w.scale(Math.sin(th) * (rad * 0.5 + 0.004)));
		f.cylinder(mk.add(ax.scale(0.1)), mk.add(ax.scale(len - 0.1)), 0.012, 4, mark, false);
	}

	/** The shaft's white mark between the front standard and the HP: seen to turn on the turning gear, a ring at speed. */
	static void shaftMark(Frame f, double cx, double cy, double z0, double z1, double r, Readings rd, float[] uv) {
		if (rd.rpm() > 40) {
			f.cylinder(v(cx, cy, z0 + 0.1), v(cx, cy, z0 + 0.14), r + 0.004, 16, uv, false);
			return;
		}
		double th = Math.toRadians(rd.angle()), rr = r + 0.004, mx = cx + rr * Math.cos(th), my = cy + rr * Math.sin(th);
		f.box(mx - 0.02, my - 0.02, z0 + 0.05, mx + 0.02, my + 0.02, z1 - 0.05, uv);
	}

	/** A needle on a gauge facing along x ({@code side} -1: facing -x), swept 270 degrees about straight up. */
	static void sideNeedle(Frame f, double x, double cy, double cz, double len, double value, int side, float[] uv) {
		double a = Math.toRadians(90 + 135 - 270 * value);
		// Seen from outside the side, "right" is -z on a -x face and +z on a +x face (as the boards' boxes draw a side).
		double dz = side * Math.cos(a), dy = Math.sin(a), w = len * 0.06;
		double pz = -dy * w, py = dz * w;
		Vec3 n = v(side, 0, 0);
		Vec3 a0 = v(x, cy + py - dy * len * 0.18, cz + pz - dz * len * 0.18), a1 = v(x, cy - py - dy * len * 0.18, cz - pz - dz * len * 0.18);
		Vec3 b1 = v(x, cy + dy * len - py * 0.3, cz + dz * len - pz * 0.3), b0 = v(x, cy + dy * len + py * 0.3, cz + dz * len + pz * 0.3);
		// (Drawn both ways round: a thin needle is never culled.)
		f.quad(a0, a1, b1, b0, n, uv[0], uv[1], uv[2], uv[3]);
		f.quad(b0, b1, a1, a0, n, uv[0], uv[1], uv[2], uv[3]);
		f.disc(v(x + side * 0.002, cy, cz), n, len * 0.12, 8, BLACK);
	}

	/** A lamp's lens on a side ({@code side} -1: facing -x) at x, from y0 to y1 and z0 to z1. */
	static void sideQuad(Frame f, double x, double y0, double y1, double z0, double z1, int side, float[] uv) {
		if (side < 0) {
			f.quad(v(x, y1, z1), v(x, y0, z1), v(x, y0, z0), v(x, y1, z0), v(-1, 0, 0), uv[0], uv[1], uv[2], uv[3]);
		} else {
			f.quad(v(x, y1, z0), v(x, y0, z0), v(x, y0, z1), v(x, y1, z1), v(1, 0, 0), uv[0], uv[1], uv[2], uv[3]);
		}
	}
}
