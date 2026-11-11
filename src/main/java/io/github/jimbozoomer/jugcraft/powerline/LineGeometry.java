package io.github.jimbozoomer.jugcraft.powerline;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Where a power line support's conductors are, on both sides (power lines round 2, 20 October 2026; the approved
 * redesign, claude/jugcraft-power-lines-redesign.md, "Turns and corners"): the supports' data written by
 * tools/line_models.py from the approved generators (assets/jugcraft/power_lines/supports.json), and the rules that
 * turn a support to its line and pick its body:
 *
 * <ul>
 * <li>which way it faces: one wire, toward it; two, the mean travel direction ({@code u_next - u_prev}), so its arms
 * bisect the corner (the old rule folded a wire leaving the other way onto the line only past 90 degrees, so at a
 * right-angle corner the arms ran along the line); three or more, the old fold;
 * <li>its body: the tangent (suspension) body on a straight line or a turn of up to {@link #ANGLE_LIMIT} degrees,
 * otherwise its family's angle (dead-end) body: at a line end, a sharper turn, a tap (three or more wires), and, but
 * for wooden poles, where its line runs into a box (the slack span into a substation is strained off both ends);
 * <li>on an angle body each span gets its own strain assembly (or, on wooden poles, its own double dead-end arm and
 * down guy) turned to the span, and each phase's conductors end where that assembly ends: so every connection has its
 * own conductor points.
 * </ul>
 *
 * <p>Part 2 (Justin, 20 October 2026: "Power lines part 2"): the superconducting pylon (always its one body; its
 * cradle sleeves bent round each turn, never tighter than a 2.2 m radius, the cable leaving each mouth along its span)
 * and the boxes (their models; a Unit Substation's and a Converter Station's strain assemblies turned to each line on
 * the gantry, a superconducting line's cable on the substation's cryogenic terminations). A wooden pole's or monopole's
 * wire to a Service Pedestal or Pad-Mount Transformer is its service, not part of its line: it does not turn the pole
 * or pick its body; the pole carries a pole-mount transformer (the drop leaves its spool) or a riser on the side
 * toward the box. <b>The box facing rule</b> (approved with the turn sheets): a box faces its first line in
 * 90-degree steps, its gantry (or weatherhead) toward it; a Unit Substation whose first line is superconducting
 * faces it with its front, where the terminations stand. Its frame turns with it.
 *
 * <p>Coordinates: a support's own frame has its anchor's bottom centre at the origin, +z the way it faces (local x
 * then runs across the line); the world point is the anchor's bottom centre plus the local point turned by the yaw.
 * The switchyard's line gantry keeps its attach points ({@link LineKind#attachPoints(LineKind)}).
 */
public final class LineGeometry {
	/** Degrees of turn a tangent support takes (line_supports.ANGLE_LIMIT). */
	public static final double ANGLE_LIMIT = 15.0;
	private static final String FILE = "/assets/jugcraft/power_lines/supports.json";

	private LineGeometry() {
	}

	// ------------------------------------------------------------------ the data

	/**
	 * One support's data: sub-conductors per phase, its wires, its plate text, its two bodies; what it carries for a
	 * box on each side (+1, -1: local x): the pole-mount transformer (wooden poles) and the riser; the
	 * superconducting pylon's cradle sleeves.
	 */
	public record Support(String key, int n, WireSpec wire, String plate, Body tangent, Body angle, Map<Integer, Xfmr> xfmr,
			Map<Integer, Riser> riser, @Nullable Sleeve sleeve) {
	}

	/** A pole-mount transformer: its model, the service drop's point on its spool, where the line's lead lands. */
	public record Xfmr(String model, Vec3 drop, @Nullable Vec3 lead) {
	}

	/** A riser: its model, where the three leads from the line land (on the cutouts, along the line). */
	public record Riser(String model, List<Vec3> leads) {
	}

	/** A superconducting cradle's sleeve: half its length (straight), its radius. */
	public record Sleeve(double half, double r) {
	}

	/** A box's jumper from a strain assembly's pad: its control point and where it lands (box frame). */
	public record Jumper(Vec3 control, Vec3 target) {
	}

	/**
	 * A box's data: its model, plate text, shield-wire points, plates, wire points (the pedestal's weatherhead), its
	 * gantry's strain assembly, hinges and jumpers, its cryogenic terminations (model, vessels), frame cells (as it
	 * faces +z) and the pedestal's meter display.
	 */
	public record Box(String model, String plate, List<Vec3> shield, List<Plate> plates, List<Vec3> attach, @Nullable String kit,
			List<Vec3> hinges, List<Jumper> jumpers, @Nullable String cryo, List<Vec3> vessels, List<BlockPos> frames, @Nullable Plate meter) {
	}

	/** Conductor radius, sag (of the span's length at mid-span), shield wire radius, spacer kind (null, rod, damper). */
	public record WireSpec(double r, double sag, double shieldR, @Nullable String spacer) {
	}

	/** Legs of a lattice body: half-width at the footings, at the waist, the waist's height. */
	public record Legs(double base, double waist, double waistY) {
	}

	/** A number plate: corners (top-left, bottom-left, bottom-right, top-right) in the body's frame, size. */
	public record Plate(Vec3[] corners, double w, double h) {
	}

	/**
	 * A body: its model, conductor points (tangent: sub-conductors in phase order), shield-wire points, legs, plates;
	 * an angle body: "strain" (an assembly per span on each hinge) or "dist" (a wooden dead-end arm per span), its
	 * kit(s), jumper radius and the hinges whose jumpers go round the peak (hinge index: clearance radius, sag).
	 */
	public record Body(String model, String kind, List<Vec3> attach, List<Vec3> shield, @Nullable Legs legs, List<Plate> plates,
			@Nullable String kit, List<String> kits, List<Vec3> hinges, double jr, Map<Integer, double[]> arc, double[] pole) {
		public boolean angle() {
			return !kind.equals("tangent");
		}
	}

	/** A strain assembly or dead-end arm pointing along +z: its conductor ends and jumper pads (and the arm's height). */
	public record Kit(String model, List<Vec3> ends, List<Vec3> pads, double y) {
	}

	private static volatile @Nullable Data data;

	private record Data(Map<String, Support> supports, Map<String, Kit> kits, Map<String, Box> boxes, JsonObject raw) {
	}

	private static Data data() {
		Data d = data;
		if (d == null) {
			synchronized (LineGeometry.class) {
				d = data;
				if (d == null) {
					d = load();
					data = d;
				}
			}
		}
		return d;
	}

	/** The raw exported data (the client reads the texture regions and the plate letters from it). */
	public static JsonObject raw() {
		return data().raw();
	}

	private static Data load() {
		Map<String, Support> supports = new HashMap<>();
		Map<String, Kit> kits = new HashMap<>();
		Map<String, Box> boxes = new HashMap<>();
		JsonObject root = new JsonObject();
		try (InputStream in = LineGeometry.class.getResourceAsStream(FILE)) {
			if (in == null) {
				throw new IllegalStateException("missing " + FILE);
			}
			root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("kits").entrySet()) {
				JsonObject k = e.getValue().getAsJsonObject();
				kits.put(e.getKey(), new Kit(k.get("model").getAsString(), points(k.getAsJsonArray("ends")), points(k.getAsJsonArray("pads")),
						k.has("y") ? k.get("y").getAsDouble() : 0));
			}
			for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("supports").entrySet()) {
				JsonObject s = e.getValue().getAsJsonObject();
				JsonObject w = s.getAsJsonObject("wire");
				WireSpec wire = new WireSpec(w.get("r").getAsDouble(), w.get("sag").getAsDouble(), w.get("shield_r").getAsDouble(),
						w.get("spacer").isJsonNull() ? null : w.get("spacer").getAsString());
				Map<Integer, Xfmr> xfmr = new HashMap<>();
				if (s.has("xfmr")) {
					for (Map.Entry<String, JsonElement> x : s.getAsJsonObject("xfmr").entrySet()) {
						JsonObject o = x.getValue().getAsJsonObject();
						xfmr.put(Integer.parseInt(x.getKey()), new Xfmr(o.get("model").getAsString(), point(o.getAsJsonArray("drop")),
								o.get("lead").isJsonNull() ? null : point(o.getAsJsonArray("lead"))));
					}
				}
				Map<Integer, Riser> riser = new HashMap<>();
				if (s.has("riser")) {
					for (Map.Entry<String, JsonElement> x : s.getAsJsonObject("riser").entrySet()) {
						JsonObject o = x.getValue().getAsJsonObject();
						riser.put(Integer.parseInt(x.getKey()), new Riser(o.get("model").getAsString(), points(o.getAsJsonArray("leads"))));
					}
				}
				Sleeve sleeve = null;
				if (s.has("sleeve")) {
					JsonObject o = s.getAsJsonObject("sleeve");
					sleeve = new Sleeve(o.get("half").getAsDouble(), o.get("r").getAsDouble());
				}
				supports.put(e.getKey(), new Support(e.getKey(), s.get("n").getAsInt(), wire, s.get("plate").getAsString(),
						body(s.getAsJsonObject("tangent"), "tangent"), body(s.getAsJsonObject("angle"), null), xfmr, riser, sleeve));
			}
			if (root.has("boxes")) {
				for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("boxes").entrySet()) {
					boxes.put(e.getKey(), box(e.getValue().getAsJsonObject()));
				}
			}
		} catch (Exception e) {
			Jugcraft.LOGGER.error("Could not read the power line supports' data", e);
		}
		return new Data(supports, kits, boxes, root);
	}

	private static Box box(JsonObject o) {
		List<Plate> plates = new ArrayList<>();
		for (JsonElement p : o.getAsJsonArray("plates")) {
			plates.add(plate(p.getAsJsonObject()));
		}
		List<Jumper> jumpers = new ArrayList<>();
		if (o.has("jumpers")) {
			for (JsonElement j : o.getAsJsonArray("jumpers")) {
				JsonObject jo = j.getAsJsonObject();
				jumpers.add(new Jumper(point(jo.getAsJsonArray("control")), point(jo.getAsJsonArray("target"))));
			}
		}
		String cryo = null;
		List<Vec3> vessels = List.of();
		if (o.has("cryo")) {
			JsonObject c = o.getAsJsonObject("cryo");
			cryo = c.get("model").getAsString();
			vessels = points(c.getAsJsonArray("vessels"));
		}
		List<BlockPos> frames = new ArrayList<>();
		for (JsonElement f : o.getAsJsonArray("frames")) {
			JsonArray a = f.getAsJsonArray();
			frames.add(new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()));
		}
		return new Box(o.get("model").getAsString(), o.get("plate").getAsString(), points(o.getAsJsonArray("shield")), plates,
				o.has("attach") ? points(o.getAsJsonArray("attach")) : List.of(), o.has("kit") ? o.get("kit").getAsString() : null,
				o.has("hinges") ? points(o.getAsJsonArray("hinges")) : List.of(), jumpers, cryo, vessels, frames,
				o.has("meter") ? plate(o.getAsJsonObject("meter")) : null);
	}

	private static Plate plate(JsonObject po) {
		List<Vec3> c = points(po.getAsJsonArray("corners"));
		return new Plate(c.toArray(Vec3[]::new), po.get("w").getAsDouble(), po.get("h").getAsDouble());
	}

	private static Body body(JsonObject o, @Nullable String kind) {
		Legs legs = null;
		if (o.has("legs") && o.get("legs").isJsonObject()) {
			JsonObject l = o.getAsJsonObject("legs");
			legs = new Legs(l.get("base").getAsDouble(), l.get("waist").getAsDouble(), l.get("waist_y").getAsDouble());
		}
		List<Plate> plates = new ArrayList<>();
		if (o.has("plates")) {
			for (JsonElement p : o.getAsJsonArray("plates")) {
				plates.add(plate(p.getAsJsonObject()));
			}
		}
		List<String> kits = new ArrayList<>();
		if (o.has("kits")) {
			for (JsonElement k : o.getAsJsonArray("kits")) {
				kits.add(k.getAsString());
			}
		}
		Map<Integer, double[]> arc = new HashMap<>();
		if (o.has("arc")) {
			for (Map.Entry<String, JsonElement> a : o.getAsJsonObject("arc").entrySet()) {
				JsonArray v = a.getValue().getAsJsonArray();
				arc.put(Integer.parseInt(a.getKey()), new double[] {v.get(0).getAsDouble(), v.get(1).getAsDouble()});
			}
		}
		double[] pole = new double[0];
		if (o.has("pole")) {
			JsonObject p = o.getAsJsonObject("pole");
			pole = new double[] {p.get("r0").getAsDouble(), p.get("r1").getAsDouble(), p.get("top").getAsDouble()};
		}
		return new Body(o.get("model").getAsString(), kind != null ? kind : o.get("kind").getAsString(),
				o.has("attach") ? points(o.getAsJsonArray("attach")) : List.of(), points(o.getAsJsonArray("shield")), legs, plates,
				o.has("kit") ? o.get("kit").getAsString() : null, kits, o.has("hinges") ? points(o.getAsJsonArray("hinges")) : List.of(),
				o.has("jr") ? o.get("jr").getAsDouble() : 0.016, arc, pole);
	}

	private static List<Vec3> points(JsonArray a) {
		List<Vec3> out = new ArrayList<>();
		for (JsonElement e : a) {
			out.add(point(e.getAsJsonArray()));
		}
		return out;
	}

	private static Vec3 point(JsonArray p) {
		return new Vec3(p.get(0).getAsDouble(), p.get(1).getAsDouble(), p.get(2).getAsDouble());
	}

	/** A support kind's data, or null (boxes; the line gantry, drawn by its site). */
	public static @Nullable Support support(LineKind kind) {
		return kind.model == null || kind.isBox() ? null : data().supports().get(kind.model);
	}

	/** A box kind's data, or null (supports; the line gantry, drawn by its site). */
	public static @Nullable Box box(LineKind kind) {
		return kind.model == null || !kind.isBox() ? null : data().boxes().get(kind.model);
	}

	/** Whether a structure is drawn from its model (everything but the switchyard's line gantry). */
	public static boolean modelled(@Nullable LineKind kind) {
		return kind != null && (support(kind) != null || box(kind) != null);
	}

	/**
	 * Whether a pole's wire to {@code other} is its service (a Service Pedestal's drop, a Pad-Mount Transformer's riser
	 * and buried cable), not part of its line: it does not turn the pole or pick its body.
	 */
	public static boolean service(LineKind pole, @Nullable LineKind other) {
		return other != null && (pole.family == LineKind.Family.WOOD || pole.family == LineKind.Family.MONOPOLE)
				&& (other == LineKind.SERVICE_PEDESTAL || other == LineKind.PAD_MOUNT_TRANSFORMER);
	}

	public static @Nullable Kit kit(String name) {
		return data().kits().get(name);
	}

	// ------------------------------------------------------------------ facing and body

	/**
	 * The way a structure faces (radians; 0 faces +z, positive turns toward +x): one wire, toward it; two, the mean
	 * travel direction (the second's direction less the first's), so the arms bisect the corner (when both leave the
	 * same way, toward them); three or more, their mean with wires leaving the other way folded onto it.
	 */
	public static double yaw(BlockPos self, List<BlockPos> connections) {
		List<double[]> u = new ArrayList<>();
		for (BlockPos other : connections) {
			double dx = other.getX() - self.getX(), dz = other.getZ() - self.getZ();
			double len = Math.hypot(dx, dz);
			if (len > 1e-6) {
				u.add(new double[] {dx / len, dz / len});
			}
		}
		if (u.isEmpty()) {
			return 0;
		}
		if (u.size() == 1) {
			return Math.atan2(u.get(0)[0], u.get(0)[1]);
		}
		if (u.size() == 2) {
			double vx = u.get(1)[0] - u.get(0)[0], vz = u.get(1)[1] - u.get(0)[1];
			if (Math.hypot(vx, vz) < 1e-6) {
				return Math.atan2(u.get(0)[0], u.get(0)[1]);
			}
			return Math.atan2(vx, vz);
		}
		double ax = 0, az = 0;
		for (double[] d : u) {
			double dx = d[0], dz = d[1];
			if (ax * dx + az * dz <= 0 && (ax != 0 || az != 0)) {
				dx = -dx;
				dz = -dz;
			}
			ax += dx;
			az += dz;
		}
		return ax == 0 && az == 0 ? 0 : Math.atan2(ax, az);
	}

	/** How far a line turns at a support with two wires (degrees; 0 straight on). */
	public static double deviation(BlockPos self, BlockPos a, BlockPos b) {
		double ix = self.getX() - a.getX(), iz = self.getZ() - a.getZ();
		double ox = b.getX() - self.getX(), oz = b.getZ() - self.getZ();
		double li = Math.hypot(ix, iz), lo = Math.hypot(ox, oz);
		if (li < 1e-6 || lo < 1e-6) {
			return 180;
		}
		double c = (ix * ox + iz * oz) / (li * lo);
		return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, c))));
	}

	/** Whether a support stands as its angle (dead-end) body with these wires (kinds: what stands at a position, or null). */
	public static boolean angleBody(LineKind kind, BlockPos self, List<BlockPos> connections, Function<BlockPos, @Nullable LineKind> kinds) {
		if (connections.size() != 2) {
			return connections.size() > 0;
		}
		if (kind.family != LineKind.Family.WOOD) {
			for (BlockPos other : connections) {
				LineKind k = kinds.apply(other);
				if (k != null && k.isBox()) {
					return true;
				}
			}
		}
		return deviation(self, connections.get(0), connections.get(1)) > ANGLE_LIMIT;
	}

	// ------------------------------------------------------------------ the layout

	/** Kits on a box's gantry turn to their line at most this far from straight out (radians: 60 degrees). */
	static final double BOX_KIT_LIMIT = Math.toRadians(60);
	/** Never bend a superconducting cable tighter than this radius round a turn (sc_scene.sleeve_arc). */
	static final double SLEEVE_MIN_RADIUS = 2.2;

	/**
	 * A structure as it stands with its wires: which way it faces, which body, and for each wire (in connection order)
	 * its phases' sub-conductor points (and on an angle body the jumper pads), in the structure's own frame.
	 */
	public static final class Layout {
		public final LineKind kind;
		public final BlockPos anchor;
		public final double yaw, cos, sin;
		public final @Nullable Support support;
		public final @Nullable Box box;
		public final @Nullable Body body;
		public final List<BlockPos> connections;
		/** Per wire: its direction in the structure's frame (horizontal, unit). */
		public final List<Vec3> dirs = new ArrayList<>();
		/** Per wire: its angle in the structure's frame (radians, as {@link #yaw}). */
		public final List<Double> turns = new ArrayList<>();
		/** Per wire: how far its strain assemblies (or wooden dead-end arm) turn (radians; NaN: none). */
		public final List<Double> kitTurns = new ArrayList<>();
		/** Per wire: phases, each its sub-conductors' points (local). */
		public final List<List<List<Vec3>>> phases = new ArrayList<>();
		/** Per wire: each phase's jumper pads (angle bodies and boxes' gantries only; local). */
		public final List<List<List<Vec3>>> pads = new ArrayList<>();
		/** The shield-wire points (local; the superconducting pylon's messenger horns). */
		public final List<Vec3> shield;
		/** On a pole wired to a box: the side (+1, -1: local x) its pole-mount transformer or riser hangs on, or 0. */
		public int xfmrSide, riserSide;
		/**
		 * Where a pole's transformer or riser leads are taken from (local): on its tangent body the line's conductors
		 * (the lead tapped off along them, {@link #leadAlong}), on its angle body the jumper pads of its first span's
		 * dead-ends ({@link #leadFromPads}).
		 */
		public List<List<Vec3>> leadPhases = List.of();
		public Vec3 leadAlong = new Vec3(0, 0, 1);
		public boolean leadFromPads;
		/** A superconducting pylon's cradle sleeves, each mouth to mouth (local). */
		public final List<List<Vec3>> sleeves = new ArrayList<>();
		/** A Unit Substation's cryogenic terminations: the way their cable-entry sleeves face (local), or null. */
		public @Nullable Vec3 vesselDir;

		Layout(LineKind kind, BlockPos anchor, double yaw, @Nullable Support support, @Nullable Box box, @Nullable Body body,
				List<BlockPos> connections, List<Vec3> shield) {
			this.kind = kind;
			this.anchor = anchor;
			this.yaw = yaw;
			this.cos = Math.cos(yaw);
			this.sin = Math.sin(yaw);
			this.support = support;
			this.box = box;
			this.body = body;
			this.connections = connections;
			this.shield = shield;
		}

		public boolean angle() {
			return body != null && body.angle();
		}

		/** The model its body is drawn from (a support's body, a box), or null. */
		public @Nullable String model() {
			return body != null ? body.model() : box != null ? box.model() : null;
		}

		/** A local point in the world. */
		public Vec3 world(Vec3 p) {
			return new Vec3(anchor.getX() + 0.5 + p.x * cos + p.z * sin, anchor.getY() + p.y, anchor.getZ() + 0.5 - p.x * sin + p.z * cos);
		}

		/** A local direction in the world. */
		public Vec3 worldDir(Vec3 d) {
			return new Vec3(d.x * cos + d.z * sin, d.y, -d.x * sin + d.z * cos);
		}

		/** The phases at the end of the wire to {@code other}, in the world (empty if not wired to it). */
		public List<List<Vec3>> phasesTo(BlockPos other) {
			int i = connections.indexOf(other);
			List<List<List<Vec3>>> src = phases;
			List<List<Vec3>> local = i >= 0 && i < src.size() ? src.get(i) : !src.isEmpty() && connections.isEmpty() ? src.get(0) : List.of();
			List<List<Vec3>> out = new ArrayList<>();
			for (List<Vec3> ph : local) {
				List<Vec3> w = new ArrayList<>();
				for (Vec3 p : ph) {
					w.add(world(p));
				}
				out.add(w);
			}
			return out;
		}

		public List<Vec3> shieldWorld() {
			List<Vec3> out = new ArrayList<>();
			for (Vec3 p : shield) {
				out.add(world(p));
			}
			return out;
		}

		/** The footing corners of a lattice body (local x, z), or empty. */
		public List<double[]> legCorners() {
			List<double[]> out = new ArrayList<>();
			if (body != null && body.legs() != null) {
				double b = body.legs().base();
				for (int sz : new int[] {-1, 1}) {
					for (int sx : new int[] {-1, 1}) {
						out.add(new double[] {sx * b, sz * b});
					}
				}
			}
			return out;
		}

		/** A local horizontal offset as world block offset (rounded to the cell it lies in). */
		public BlockPos cellOf(double x, double y, double z) {
			double wx = x * cos + z * sin, wz = -x * sin + z * cos;
			return new BlockPos((int) Math.floor(wx + 0.5), (int) Math.floor(y), (int) Math.floor(wz + 0.5));
		}
	}

	/** Turns a point about the vertical axis by {@code angle} (radians, as {@link #yaw}). */
	public static Vec3 turn(Vec3 p, double angle) {
		double c = Math.cos(angle), s = Math.sin(angle);
		return new Vec3(p.x * c + p.z * s, p.y, -p.x * s + p.z * c);
	}

	/** A structure's layout with its wires (kinds: what stands at a position, null if unknown). */
	public static Layout layout(LineKind kind, BlockPos anchor, List<BlockPos> connections, Function<BlockPos, @Nullable LineKind> kinds) {
		return layout(kind, anchor, connections, kinds, Double.NaN);
	}

	/** The way a box faces when its block says so (a line gantry turns with its plant): NaN if not. */
	public static double boxYaw(net.minecraft.world.level.block.state.BlockState state) {
		if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
			return -Math.toRadians(state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING).toYRot());
		}
		return Double.NaN;
	}

	/** {@link #layout(LineKind, BlockPos, List, Function)} reading a box's facing from the level (if there). */
	public static Layout layout(net.minecraft.world.level.@Nullable Level level, LineKind kind, BlockPos anchor, List<BlockPos> connections,
			Function<BlockPos, @Nullable LineKind> kinds) {
		double yaw = Double.NaN;
		if (level != null && kind.isBox() && level.isLoaded(anchor)) {
			yaw = boxYaw(level.getBlockState(anchor));
		}
		return layout(kind, anchor, connections, kinds, yaw);
	}

	/**
	 * The way a box faces with these wires (the box facing rule): its first line's direction in 90-degree steps; a Unit
	 * Substation turns its front (its cryogenic terminations) to a first line that is superconducting.
	 */
	public static double boxFacing(LineKind kind, BlockPos anchor, List<BlockPos> connections, Function<BlockPos, @Nullable LineKind> kinds) {
		if (connections.isEmpty()) {
			return 0;
		}
		BlockPos o = connections.get(0);
		double dx = o.getX() - anchor.getX(), dz = o.getZ() - anchor.getZ();
		if (Math.hypot(dx, dz) < 1e-6) {
			return 0;
		}
		double yaw = Math.atan2(dx, dz);
		if (kind == LineKind.UNIT_SUBSTATION && kinds.apply(o) == LineKind.SUPERCONDUCTING_PYLON) {
			yaw += Math.PI;
		}
		long steps = Math.floorMod(Math.round(yaw / (Math.PI / 2)), 4L);
		return steps == 3 ? -Math.PI / 2 : steps * (Math.PI / 2);
	}

	/** A structure's layout, a box turned by {@code boxYaw} (NaN: by the box facing rule). */
	public static Layout layout(LineKind kind, BlockPos anchor, List<BlockPos> connections, Function<BlockPos, @Nullable LineKind> kinds,
			double boxYaw) {
		if (kind.isBox()) {
			double yaw = !Double.isNaN(boxYaw) ? boxYaw : kind == LineKind.LINE_GANTRY ? 0 : boxFacing(kind, anchor, connections, kinds);
			return boxLayout(kind, anchor, connections, kinds, yaw);
		}
		// The line's wires: a pole's service to a box is not part of its line.
		List<BlockPos> line = new ArrayList<>();
		for (BlockPos c : connections) {
			if (!service(kind, kinds.apply(c))) {
				line.add(c);
			}
		}
		double yaw = yaw(anchor, line.isEmpty() ? connections : line);
		Support s = support(kind);
		if (s == null) {
			// No data (it failed to load): the stage 1 attach points.
			Layout l = new Layout(kind, anchor, yaw, null, null, null, connections, List.of());
			for (int i = 0; i < Math.max(1, connections.size()); i++) {
				List<List<Vec3>> phases = new ArrayList<>();
				for (Vec3 p : kind.attachPoints()) {
					phases.add(List.of(p));
				}
				l.phases.add(phases);
				l.pads.add(List.of());
				l.kitTurns.add(Double.NaN);
				addDir(l, connections, i);
			}
			return l;
		}
		boolean supercon = kind.family == LineKind.Family.SUPERCONDUCTING;
		boolean angle = !supercon && angleBody(kind, anchor, line, kinds);
		Body body = angle ? s.angle() : s.tangent();
		Layout l = new Layout(kind, anchor, yaw, s, null, body, connections, body.shield());
		for (int i = 0; i < Math.max(1, connections.size()); i++) {
			addDir(l, connections, i);
		}
		// Which side a pole carries its transformer and its riser (toward the first box of each; never both on one side).
		for (int i = 0; i < connections.size(); i++) {
			LineKind other = kinds.apply(connections.get(i));
			if (!service(kind, other)) {
				continue;
			}
			int side = l.dirs.get(i).x >= 0 ? 1 : -1;
			if (other == LineKind.SERVICE_PEDESTAL && l.xfmrSide == 0 && s.xfmr().containsKey(side)) {
				l.xfmrSide = side == l.riserSide ? -side : side;
			} else if (other == LineKind.PAD_MOUNT_TRANSFORMER && l.riserSide == 0 && s.riser().containsKey(side)) {
				l.riserSide = side == l.xfmrSide ? -side : side;
			}
		}
		List<List<Vec3>> tangent = new ArrayList<>();
		for (int q = 0; q + s.n() <= body.attach().size(); q += s.n()) {
			tangent.add(body.attach().subList(q, q + s.n()));
		}
		List<List<List<Vec3>>> mouths = supercon ? sleeves(l, s, body) : List.of();
		int j = 0;
		for (int i = 0; i < Math.max(1, connections.size()); i++) {
			LineKind other = i < connections.size() ? kinds.apply(connections.get(i)) : null;
			if (service(kind, other)) {
				// The service: the drop from the transformer's spool (a pad-mount's cable is buried: no wire).
				Xfmr x = other == LineKind.SERVICE_PEDESTAL ? s.xfmr().get(l.xfmrSide) : null;
				l.phases.add(x == null ? List.of() : List.of(List.of(x.drop())));
				l.pads.add(List.of());
				l.kitTurns.add(Double.NaN);
				continue;
			}
			if (supercon) {
				l.phases.add(mouths.get(Math.min(i, mouths.size() - 1)));
				l.pads.add(List.of());
				l.kitTurns.add(Double.NaN);
			} else if (!angle) {
				l.phases.add(tangent);
				l.pads.add(List.of());
				l.kitTurns.add(Double.NaN);
			} else {
				double turn = l.turns.get(i);
				List<List<Vec3>> phases = new ArrayList<>();
				List<List<Vec3>> pads = new ArrayList<>();
				if (body.kind().equals("dist")) {
					Kit k = kit(body.kits().get(Math.min(j, body.kits().size() - 1)));
					if (k != null) {
						for (int p = 0; p < k.ends().size(); p++) {
							phases.add(List.of(turn(k.ends().get(p), turn)));
							pads.add(List.of(turn(k.pads().get(p), turn)));
						}
					}
				} else {
					Kit k = body.kit() == null ? null : kit(body.kit());
					if (k != null) {
						kitPhases(k, body.hinges(), turn, phases, pads);
					}
				}
				l.phases.add(phases);
				l.pads.add(pads);
				l.kitTurns.add(turn);
			}
			if (l.leadPhases.isEmpty() && i < connections.size()) {
				l.leadPhases = angle ? l.pads.get(i) : l.phases.get(i);
				l.leadFromPads = angle;
			}
			j++;
		}
		if (l.leadPhases.isEmpty()) {
			l.leadPhases = tangent;
		}
		return l;
	}

	/** A strain assembly's ends and pads at each hinge, turned by {@code turn}. */
	private static void kitPhases(Kit k, List<Vec3> hinges, double turn, List<List<Vec3>> phases, List<List<Vec3>> pads) {
		for (Vec3 h : hinges) {
			List<Vec3> e = new ArrayList<>(), pd = new ArrayList<>();
			for (Vec3 p : k.ends()) {
				e.add(h.add(turn(p, turn)));
			}
			for (Vec3 p : k.pads()) {
				pd.add(h.add(turn(p, turn)));
			}
			phases.add(e);
			pads.add(pd);
		}
	}

	/**
	 * A superconducting pylon's sleeves (into the layout) and, per wire, each cable's mouth: with two wires each cradle's
	 * sleeve is bent round the turn (its back mouth toward the first wire, its front toward the second); with one, or
	 * none, straight along the line; at a tap, a straight sleeve out to each wire.
	 */
	private static List<List<List<Vec3>>> sleeves(Layout l, Support s, Body body) {
		double half = s.sleeve() == null ? 0.8 : s.sleeve().half();
		int n = l.connections.size();
		List<List<List<Vec3>>> out = new ArrayList<>();
		for (int i = 0; i < Math.max(1, n); i++) {
			out.add(new ArrayList<>());
		}
		boolean bent = n == 2 && l.dirs.get(0).add(l.dirs.get(1)).lengthSqr() < 4 - 1e-6 && l.dirs.get(1).subtract(l.dirs.get(0)).lengthSqr() > 1e-6;
		for (Vec3 c : body.attach()) {
			if (bent) {
				List<Vec3> arc = sleeveArc(c, l.dirs.get(0).scale(-1), l.dirs.get(1), half);
				l.sleeves.add(arc);
				out.get(0).add(List.of(arc.getFirst()));
				out.get(1).add(List.of(arc.getLast()));
			} else if (n <= 1) {
				Vec3 d = l.dirs.get(0);
				List<Vec3> arc = sleeveArc(c, d, d, half);
				l.sleeves.add(arc);
				out.get(0).add(List.of(arc.getLast()));
			} else {
				for (int i = 0; i < n; i++) {
					Vec3 d = l.dirs.get(i);
					List<Vec3> arc = new ArrayList<>();
					for (int k = 0; k <= 6; k++) {
						double t = k / 6.0;
						arc.add(c.add(d.scale(half * t)).subtract(0, 0.02 * t * t, 0));
					}
					l.sleeves.add(arc);
					out.get(i).add(List.of(arc.getLast()));
				}
			}
		}
		return out;
	}

	/**
	 * A cradle's sleeve bent round the turn at {@code c} (sc_scene.sleeve_arc): from the back mouth to the front, its
	 * tangent the incoming direction at the back, the outgoing at the front, the bisector at {@code c}; longer on sharp
	 * turns so the cable is never bent tighter than {@link #SLEEVE_MIN_RADIUS}; its ends drooping 2 cm with the cable.
	 */
	public static List<Vec3> sleeveArc(Vec3 c, Vec3 uIn, Vec3 uOut, double half0) {
		Vec3 bis = uIn.add(uOut).normalize();
		double phi = Math.acos(Math.max(-1, Math.min(1, uIn.dot(uOut)))) / 2;
		double half = Math.max(half0, SLEEVE_MIN_RADIUS * phi);
		double turn = uIn.z * uOut.x - uIn.x * uOut.z;
		Vec3 left = new Vec3(bis.z, 0, -bis.x).scale(turn < 0 ? -1 : 1);
		List<Vec3> out = new ArrayList<>();
		int n = 12;
		for (int i = 0; i <= n; i++) {
			double s = -half + 2 * half * i / n;
			Vec3 p;
			if (phi < 1e-4) {
				p = c.add(bis.scale(s));
			} else {
				double r = half / phi, a = s / r;
				p = c.add(bis.scale(r * Math.sin(a))).add(left.scale(r * (1 - Math.cos(a))));
			}
			out.add(p.subtract(0, 0.02 * (s / half) * (s / half), 0));
		}
		return out;
	}

	/**
	 * A box's layout: the Service Pedestal's drop lands on its weatherhead; a Pad-Mount Transformer's cable is buried
	 * (no wire); a Unit Substation or Converter Station dead-ends each line on its gantry with its own strain assemblies
	 * turned to it (at most {@link #BOX_KIT_LIMIT} from straight out); a superconducting line lands on the substation's
	 * cryogenic terminations, their sleeves turned to it. The switchyard's line gantry: its attach points.
	 */
	private static Layout boxLayout(LineKind kind, BlockPos anchor, List<BlockPos> connections, Function<BlockPos, @Nullable LineKind> kinds,
			double yaw) {
		Box box = box(kind);
		if (box == null) {
			Layout l = new Layout(kind, anchor, yaw, null, null, null, connections, List.of());
			int per = kind.conductorsPerPhase();
			for (int i = 0; i < Math.max(1, connections.size()); i++) {
				LineKind partner = i < connections.size() ? kinds.apply(connections.get(i)) : null;
				List<List<Vec3>> phases = new ArrayList<>();
				List<Vec3> points = kind.attachPoints(partner == null ? kind : partner);
				for (int q = 0; q + per <= points.size(); q += per) {
					phases.add(points.subList(q, q + per));
				}
				l.phases.add(phases);
				l.pads.add(List.of());
				l.kitTurns.add(Double.NaN);
				addDir(l, connections, i);
			}
			return l;
		}
		Layout l = new Layout(kind, anchor, yaw, null, box, null, connections, box.shield());
		Kit kit = box.kit() == null ? null : kit(box.kit());
		for (int i = 0; i < Math.max(1, connections.size()); i++) {
			addDir(l, connections, i);
			LineKind partner = i < connections.size() ? kinds.apply(connections.get(i)) : null;
			List<List<Vec3>> phases = new ArrayList<>();
			List<List<Vec3>> pads = new ArrayList<>();
			double kitTurn = Double.NaN;
			if (!box.attach().isEmpty()) {
				phases.add(List.of(box.attach().getFirst()));
			} else if (partner == LineKind.SUPERCONDUCTING_PYLON && !box.vessels().isEmpty()) {
				if (l.vesselDir == null) {
					// The cable-entry sleeves swivel to the line, out of the yard's front half.
					Vec3 d = l.dirs.get(i);
					if (d.z > -0.25) {
						double x = Math.sqrt(1 - 0.0625) * (d.x < 0 ? -1 : 1);
						d = new Vec3(x, 0, -0.25);
					}
					l.vesselDir = d;
				}
				for (Vec3 v : box.vessels()) {
					phases.add(List.of(v.add(l.vesselDir.scale(0.77))));
				}
			} else if (kit != null && kind != LineKind.PAD_MOUNT_TRANSFORMER) {
				kitTurn = Math.max(-BOX_KIT_LIMIT, Math.min(BOX_KIT_LIMIT, l.turns.get(i)));
				kitPhases(kit, box.hinges(), kitTurn, phases, pads);
			}
			l.phases.add(phases);
			l.pads.add(pads);
			l.kitTurns.add(kitTurn);
		}
		return l;
	}

	private static void addDir(Layout l, List<BlockPos> connections, int i) {
		if (i >= connections.size()) {
			l.dirs.add(new Vec3(0, 0, 1));
			l.turns.add(0.0);
			return;
		}
		BlockPos o = connections.get(i);
		double dx = o.getX() - l.anchor.getX(), dz = o.getZ() - l.anchor.getZ();
		double len = Math.max(1e-6, Math.hypot(dx, dz));
		dx /= len;
		dz /= len;
		// world -> local: turn by -yaw
		double lx = dx * l.cos - dz * l.sin, lz = dx * l.sin + dz * l.cos;
		l.dirs.add(new Vec3(lx, 0, lz));
		l.turns.add(Math.atan2(lx, lz));
	}

	// ------------------------------------------------------------------ the collision frame

	/**
	 * The frame cells a structure fills with this layout (offsets from the anchor): a pole's column; a lattice body's four
	 * legs from the footings to the waist, turned with it; a box's fence and equipment, turned with it.
	 */
	public static List<BlockPos> frames(Layout l) {
		LineKind kind = l.kind;
		List<BlockPos> out = new ArrayList<>();
		switch (kind.family) {
			case WOOD, MONOPOLE, SUPERCONDUCTING -> {
				for (int y = 1; y < kind.height; y++) {
					out.add(new BlockPos(0, y, 0));
				}
			}
			case LATTICE, UHV, UHVDC -> {
				Legs legs = l.body == null ? null : l.body.legs();
				if (legs == null) {
					return kind.frames();
				}
				for (int y = 0; y < (int) Math.ceil(legs.waistY()); y++) {
					double half = legs.base() - (legs.base() - legs.waist()) * Math.min(1.0, (y + 0.5) / legs.waistY());
					for (int sz : new int[] {-1, 1}) {
						for (int sx : new int[] {-1, 1}) {
							BlockPos c = l.cellOf(sx * half, y + 0.5, sz * half);
							if (!c.equals(BlockPos.ZERO) && !out.contains(c)) {
								out.add(c);
							}
						}
					}
				}
			}
			case BOX -> {
				if (l.box == null) {
					return kind.frames();
				}
				// The yard's fence and its equipment, turned with the box.
				for (BlockPos f : l.box.frames()) {
					BlockPos c = l.cellOf(f.getX(), f.getY() + 0.5, f.getZ());
					if (!c.equals(BlockPos.ZERO) && !out.contains(c)) {
						out.add(c);
					}
				}
			}
		}
		return out;
	}
}
