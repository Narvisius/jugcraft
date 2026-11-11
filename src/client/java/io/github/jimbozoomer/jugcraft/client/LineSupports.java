package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.powerline.LineGeometry;
import io.github.jimbozoomer.jugcraft.powerline.LineKind;
import io.github.jimbozoomer.jugcraft.powerline.LineWires;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a power line structure as approved in the round 2 redesign (claude/jugcraft-power-lines-redesign.md): its body
 * (tangent or angle, LineGeometry) from client/LineModels, turned to its line; on a slope each lattice footing set on
 * the ground under its leg with a leg extension; on an angle body a strain assembly (or a wooden dead-end arm with its
 * down guy) turned to each span, and the jumpers that join each phase's assemblies, drawn as tools/line_supports.py
 * draws them (place_kits): a bundle's loops as a ribbon so its sub-conductors never cross, round the peak at the
 * phase clearance, held on a pilot string when long, hugging a wooden pole nearest phase innermost; at a line end the
 * slack tails tied back under the hinge. The structure's number goes on its plates in the plates' own letters.
 *
 * <p>Part 2: the superconducting pylon's cradle sleeves bent round its turn (tools/sc_scene.py); the boxes' models with
 * a strain assembly per line on their gantries and the jumpers down from them, a Unit Substation's cryogenic
 * terminations when a superconducting line lands there (their cable-entry sleeves turned to it), the Service
 * Pedestal's meter reading what it has delivered (kWh) and its current; a pole's pole-mount transformer (for a
 * pedestal) or riser (for a pad-mount) on the side toward the box, with the leads from the line to their cutouts.
 *
 * <p>Also the wires a structure owns (each span drawn once, by the structure at the lower position): every
 * sub-conductor of each phase in a smooth sag, the shield wires, and bundle spacers (or spacer-dampers) at the thirds;
 * a superconducting cable with its glowing inspection stripe (lit while it carries power) under its messenger on
 * droppers every 4 m; a service drop as triplex, two insulated conductors twisted round the bare neutral.
 */
final class LineSupports {
	private LineSupports() {
	}

	private static final Vec3 UP = new Vec3(0, 1, 0);

	/** What a structure draws, built when its wires change: its layout, footings' drops, kits, and its own quads. */
	static final class Built {
		final List<Object> key;
		final LineGeometry.Layout layout;
		final int[] drops;
		/** Kit placements: model, turn (radians, in the structure's frame), position (structure frame). */
		final List<Object[]> kits = new ArrayList<>();
		/** Models drawn with the body (a substation's cryogenic terminations, a pole's transformer or riser). */
		final List<String> parts = new ArrayList<>();
		final LineMesh mesh = new LineMesh();
		/** What it draws reaches no further than this (the world), for frustum culling. */
		net.minecraft.world.phys.AABB bounds;

		Built(List<Object> key, LineGeometry.Layout layout, int[] drops) {
			this.key = key;
			this.layout = layout;
			this.drops = drops;
		}
	}

	/**
	 * A far end's layout and what its span needs; owned: this structure draws the span (else it is only told of it: a
	 * substation fits fence penetrations where a superconducting cable leaves its yard).
	 */
	record Far(LineGeometry.Layout layout, int light, boolean owned) {
		Far(LineGeometry.Layout layout, int light) {
			this(layout, light, true);
		}
	}

	/** What a structure shows that changes while it stands: its line's load (0 to 255), a pedestal's delivered energy. */
	record Live(int load, long delivered) {
		static final Live NONE = new Live(0, 0);
	}

	static Built build(List<Object> key, LineGeometry.Layout l, int[] drops, List<Far> owned, int lod) {
		return build(key, l, drops, owned, lod, Live.NONE);
	}

	/**
	 * Builds what a structure draws: its layout, the drops under its legs, its kits and jumpers, what it carries for a
	 * box, and the wires to each far end it owns.
	 */
	static Built build(List<Object> key, LineGeometry.Layout l, int[] drops, List<Far> owned, int lod, Live live) {
		Built b = new Built(key, l, drops);
		BlockPos a = l.anchor;
		Vec3 origin = new Vec3(a.getX(), a.getY(), a.getZ());
		LineGeometry.Support s = l.support;
		if (s != null && l.body != null) {
			LineGeometry.Body body = l.body;
			if (l.angle()) {
				for (int i = 0; i < l.connections.size(); i++) {
					double turn = l.kitTurns.get(i);
					if (Double.isNaN(turn)) {
						continue;
					}
					if (body.kind().equals("dist")) {
						int j = lineIndex(l, i);
						LineGeometry.Kit k = LineGeometry.kit(body.kits().get(Math.min(j, body.kits().size() - 1)));
						if (k != null) {
							b.kits.add(new Object[] {k.model(), turn, Vec3.ZERO});
						}
					} else if (body.kit() != null) {
						for (Vec3 h : body.hinges()) {
							b.kits.add(new Object[] {body.kit(), turn, h});
						}
					}
				}
				if (lod < 2) {
					jumpers(b.mesh, l, origin);
				}
			}
			legExtensions(b.mesh, l, origin, drops);
			if (lod == 0) {
				plates(b.mesh, l, origin, s.plate(), body.plates());
			}
			if (!l.sleeves.isEmpty()) {
				sleeves(b.mesh, l, origin, lod);
			}
			if (l.xfmrSide != 0) {
				LineGeometry.Xfmr x = s.xfmr().get(l.xfmrSide);
				b.parts.add(x.model());
				if (lod < 2 && x.lead() != null) {
					xfmrLead(b.mesh, l, origin, x);
				}
			}
			if (l.riserSide != 0) {
				LineGeometry.Riser r = s.riser().get(l.riserSide);
				b.parts.add(r.model());
				if (lod < 2) {
					riserLeads(b.mesh, l, origin, r);
				}
			}
		} else if (l.box != null) {
			LineGeometry.Box box = l.box;
			for (int i = 0; i < l.kitTurns.size(); i++) {
				double turn = l.kitTurns.get(i);
				if (!Double.isNaN(turn) && box.kit() != null) {
					for (Vec3 h : box.hinges()) {
						b.kits.add(new Object[] {box.kit(), turn, h});
					}
				}
			}
			if (lod < 2) {
				boxJumpers(b.mesh, l, origin);
			}
			if (l.vesselDir != null && box.cryo() != null) {
				b.parts.add(box.cryo());
				vesselSleeves(b.mesh, l, origin, lod);
			}
			if (lod == 0) {
				plates(b.mesh, l, origin, box.plate(), box.plates());
				if (box.meter() != null) {
					meter(b.mesh, l, origin, box.meter(), live);
				}
			}
		}
		// Its reach: the body and its kits round the anchor, and every span it owns to the far end.
		double r = 2, top = Math.max(2, l.kind.height + 1), bottom = -13;
		String model = l.model();
		if (model != null) {
			LineModels.Model body = LineModels.get(model);
			if (body != null) {
				r = Math.max(r, body.radius() + 1.5);
				top = Math.max(top, body.top() + 1);
			}
			for (Object[] k : b.kits) {
				LineModels.Model kit = LineModels.get((String) k[0]);
				Vec3 at = (Vec3) k[2];
				if (kit != null) {
					r = Math.max(r, Math.hypot(at.x, at.z) + kit.radius() + 1);
				}
			}
		}
		net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(a.getX() + 0.5 - r, a.getY() + bottom, a.getZ() + 0.5 - r,
				a.getX() + 0.5 + r, a.getY() + top, a.getZ() + 0.5 + r);
		for (Far far : owned) {
			LineWires.Span span = LineWires.span(l, far.layout());
			LineKind ka = l.kind, kb = far.layout().kind;
			if (l.box != null && kb == LineKind.SUPERCONDUCTING_PYLON && lod < 2) {
				penetrations(b.mesh, l, origin, span);
			}
			if (!far.owned()) {
				continue;
			}
			if (ka == LineKind.SUPERCONDUCTING_PYLON || kb == LineKind.SUPERCONDUCTING_PYLON) {
				cables(b.mesh, span, origin, far.light(), lod, live.load() > 0);
			} else if (ka == LineKind.SERVICE_PEDESTAL || kb == LineKind.SERVICE_PEDESTAL) {
				triplex(b.mesh, span, origin, far.light(), lod);
			} else {
				wires(b.mesh, span, origin, far.light(), lod);
			}
			for (List<LineWires.Wire> ph : span.phases()) {
				for (LineWires.Wire w : ph) {
					box = box.minmax(new net.minecraft.world.phys.AABB(w.a(), w.b()).expandTowards(0, -w.sag() - 1, 0).inflate(1));
				}
			}
		}
		b.bounds = box;
		return b;
	}

	/** Which of a support's line wires (its service drops left out) connection {@code i} is. */
	private static int lineIndex(LineGeometry.Layout l, int i) {
		int j = 0;
		for (int k = 0; k < i; k++) {
			if (!Double.isNaN(l.kitTurns.get(k))) {
				j++;
			}
		}
		return j;
	}

	/** Draws a built structure's body, feet, kits, parts and quads at a level of detail. */
	static void draw(Built b, int lod, PoseStack.Pose pose, VertexConsumer c, int[] lights, boolean blink, int tint) {
		LineGeometry.Layout l = b.layout;
		float cos = (float) l.cos, sin = (float) l.sin;
		String model = l.model();
		if (model != null) {
			LineModels.Model body = LineModels.get(model);
			if (body != null) {
				LineModels.draw(body.level(lod), pose, c, cos, sin, 0.5F, 0, 0.5F, lights, blink, tint);
			}
			for (String part : b.parts) {
				LineModels.Model m = LineModels.get(part);
				if (m != null) {
					LineModels.draw(m.level(lod), pose, c, cos, sin, 0.5F, 0, 0.5F, lights, blink, tint);
				}
			}
			LineGeometry.Legs legs = l.body == null ? null : l.body.legs();
			if (legs != null) {
				List<double[]> corners = l.legCorners();
				for (int k = 0; k < 4; k++) {
					LineModels.Model foot = LineModels.get(model + "_foot" + k);
					if (foot == null) {
						continue;
					}
					int d = b.drops[k];
					double out = d * (legs.base() - legs.waist()) / legs.waistY();
					double[] cr = corners.get(k);
					double fx = Math.signum(cr[0]) * out, fz = Math.signum(cr[1]) * out;
					float tx = (float) (fx * cos + fz * sin) + 0.5F, tz = (float) (-fx * sin + fz * cos) + 0.5F;
					LineModels.draw(foot.level(lod), pose, c, cos, sin, tx, -d, tz, lights, blink, tint);
				}
			}
			for (Object[] k : b.kits) {
				LineModels.Model kit = LineModels.get((String) k[0]);
				if (kit == null) {
					continue;
				}
				double turn = (double) k[1];
				Vec3 at = (Vec3) k[2];
				double ang = l.yaw + turn;
				float kc = (float) Math.cos(ang), ks = (float) Math.sin(ang);
				Vec3 w = l.worldDir(at);
				LineModels.draw(kit.level(lod), pose, c, kc, ks, (float) w.x + 0.5F, (float) w.y, (float) w.z + 0.5F, lights, blink, tint);
			}
		}
		b.mesh.draw(pose, c, lights, tint);
	}

	// ------------------------------------------------------------------ wires

	private static void wires(LineMesh m, LineWires.Span span, Vec3 origin, int light, int lod) {
		LineGeometry.WireSpec spec = span.spec();
		double r = spec == null ? 0.02 : spec.r();
		int sides = lod == 0 ? 6 : 4;
		int conductor = LineModels.colour("aluminium");
		for (List<LineWires.Wire> phase : span.phases()) {
			for (LineWires.Wire w : phase) {
				m.tube(curve(w, origin), r, sides, conductor, light);
			}
			if (spec != null && spec.spacer() != null && phase.size() > 1 && lod < 2) {
				spacers(m, phase, origin, spec.spacer().equals("damper"), light);
			}
		}
		if (spec != null && spec.shieldR() > 0) {
			int galv = LineModels.colour("galv");
			for (LineWires.Wire w : span.shields()) {
				m.tube(curve(w, origin), spec.shieldR(), lod == 0 ? 5 : 4, galv, light);
			}
		}
	}

	/**
	 * A superconducting span (tools/sc_scene.py line_ops): each cable (its cryostat jacket) with the inspection stripe
	 * along its top, glowing while the line carries power; the messenger above it; a dropper every 4 m from the messenger
	 * to a clamp on the cable.
	 */
	private static void cables(LineMesh m, LineWires.Span span, Vec3 origin, int light, int lod, boolean live) {
		int jacket = LineModels.colour("cryo_jacket"), glow = LineModels.colour("sc_glow"), alu = LineModels.colour("aluminium");
		List<LineWires.Wire> cables = span.conductors();
		for (LineWires.Wire w : cables) {
			List<Vec3> pts = curve(w, origin);
			m.tube(pts, 0.08, lod == 0 ? 10 : lod == 1 ? 6 : 4, jacket, light);
			if (lod < 2) {
				List<Vec3> stripe = new ArrayList<>();
				for (Vec3 p : pts) {
					stripe.add(p.add(0, 0.078, 0));
				}
				m.tube(stripe, 0.012, 4, live ? glow : jacket, live ? LightCoordsUtil.FULL_BRIGHT : light);
			}
		}
		for (LineWires.Wire ms : span.shields()) {
			m.tube(curve(ms, origin), 0.01, lod == 0 ? 6 : 4, alu, light);
			if (lod > 0 || cables.isEmpty()) {
				continue;
			}
			// The cable this messenger carries: the one under it.
			LineWires.Wire cb = cables.getFirst();
			double best = Double.MAX_VALUE;
			for (LineWires.Wire w : cables) {
				double d = horizontal(w.a(), ms.a()) + horizontal(w.b(), ms.b());
				if (d < best) {
					best = d;
					cb = w;
				}
			}
			double len = horizontal(cb.a(), cb.b());
			int n = (int) (len / 4);
			Vec3 along = cb.b().subtract(cb.a()).normalize();
			for (int j = 1; j < n; j++) {
				double t = j / (double) n;
				Vec3 pm = ms.at(t).subtract(origin), pc = cb.at(t).subtract(origin);
				m.tube(List.of(pm, pc.add(0, 0.1, 0)), 0.006, 4, alu, light);
				m.cylinder(pc.subtract(along.scale(0.03)), pc.add(along.scale(0.03)), 0.092, 10, alu, light);
			}
		}
	}

	private static double horizontal(Vec3 a, Vec3 b) {
		return Math.hypot(a.x - b.x, a.z - b.z);
	}

	/**
	 * A service drop (tools/box_scene.py triplex_ops): the bare aluminium neutral messenger with its two insulated
	 * conductors twisted round it (half a metre a turn); from afar one dark cable.
	 */
	private static void triplex(LineMesh m, LineWires.Span span, Vec3 origin, int light, int lod) {
		int alu = LineModels.colour("aluminium"), ins = LineModels.colour("ground_molding");
		for (LineWires.Wire w : span.conductors()) {
			Vec3 a = w.a(), c = w.b();
			double len = a.distanceTo(c);
			if (lod > 0) {
				m.tube(curve(w, origin), 0.014, 4, ins, light);
				continue;
			}
			Vec3 u = c.subtract(a).normalize();
			Vec3 n1 = u.cross(UP);
			n1 = n1.lengthSqr() < 1e-9 ? new Vec3(1, 0, 0) : n1.normalize();
			Vec3 n2 = u.cross(n1);
			int n = Math.max(60, (int) (len / 0.08));
			List<Vec3> pts = new ArrayList<>();
			for (int i = 0; i < n; i++) {
				pts.add(w.at(i / (double) (n - 1)).subtract(origin));
			}
			m.tube(pts, 0.005, 5, alu, light);
			for (double ph : new double[] {0, Math.PI}) {
				List<Vec3> hp = new ArrayList<>();
				for (int i = 0; i < n; i++) {
					double t = i / (double) (n - 1) * len / 0.5 * 2 * Math.PI + ph;
					hp.add(pts.get(i).add(n1.scale(Math.cos(t) * 0.012)).add(n2.scale(Math.sin(t) * 0.012)));
				}
				m.tube(hp, 0.007, 5, ins, light);
			}
		}
	}

	/** A wire's curve, from the anchor's corner (a point about every three blocks). */
	private static List<Vec3> curve(LineWires.Wire w, Vec3 origin) {
		int n = Math.max(8, Math.min(48, (int) (w.a().distanceTo(w.b()) / 3)));
		List<Vec3> pts = new ArrayList<>();
		for (int i = 0; i <= n; i++) {
			pts.add(w.at(i / (double) n).subtract(origin));
		}
		return pts;
	}

	/** Spacers at the thirds: rods round the bundle with a clamp on each sub-conductor, or spacer-dampers (hub, arms). */
	private static void spacers(LineMesh m, List<LineWires.Wire> phase, Vec3 origin, boolean damper, int light) {
		int alu = LineModels.colour("aluminium"), galv = LineModels.colour("galv"), poly = LineModels.colour("polymer");
		LineWires.Wire w0 = phase.get(0);
		Vec3 along = w0.b().subtract(w0.a()).multiply(1, 0, 1).normalize();
		for (double t : new double[] {1 / 3.0, 2 / 3.0}) {
			List<Vec3> pts = new ArrayList<>();
			for (LineWires.Wire w : phase) {
				pts.add(w.at(t).subtract(origin));
			}
			if (damper) {
				Vec3 hub = Vec3.ZERO;
				for (Vec3 p : pts) {
					hub = hub.add(p);
				}
				hub = hub.scale(1.0 / pts.size());
				m.cylinder(hub.subtract(along.scale(0.04)), hub.add(along.scale(0.04)), 0.09, 10, alu, light);
				for (Vec3 e : pts) {
					m.tube(List.of(hub, e), 0.02, 5, alu, light);
					m.cylinder(e.subtract(along.scale(0.06)), e.add(along.scale(0.06)), 0.028, 8, poly, light);
				}
				continue;
			}
			// a ring round the bundle in order round its centre
			Vec3 c = Vec3.ZERO;
			for (Vec3 p : pts) {
				c = c.add(p);
			}
			Vec3 centre = c.scale(1.0 / pts.size());
			Vec3 side = UP.cross(along).normalize();
			List<Vec3> ring = new ArrayList<>(pts);
			ring.sort(Comparator.comparingDouble(p -> Math.atan2(p.subtract(centre).y, p.subtract(centre).dot(side))));
			int edges = ring.size() > 2 ? ring.size() : 1;
			for (int k = 0; k < edges; k++) {
				m.tube(List.of(ring.get(k), ring.get((k + 1) % ring.size())), 0.018, 5, alu, light);
			}
			for (Vec3 e : pts) {
				m.cylinder(e.subtract(along.scale(0.05)), e.add(along.scale(0.05)), 0.026, 8, galv, light);
			}
		}
	}

	// ------------------------------------------------------------------ jumpers (line_supports.place_kits)

	private static Vec3 centroid(List<Vec3> ps) {
		Vec3 c = Vec3.ZERO;
		for (Vec3 p : ps) {
			c = c.add(p);
		}
		return c.scale(1.0 / ps.size());
	}

	/** The jumpers of an angle body (in the structure's frame, then into the world from the anchor's corner). */
	private static void jumpers(LineMesh m, LineGeometry.Layout l, Vec3 origin) {
		LineGeometry.Body body = l.body;
		int alu = LineModels.colour("aluminium");
		int n = l.connections.size();
		List<List<Vec3>> curves = new ArrayList<>();
		List<Double> radii = new ArrayList<>();
		if (body.kind().equals("dist")) {
			for (int mm = 1; mm < n; mm++) {
				Vec3 outside = outside(l.dirs.get(0), l.dirs.get(mm));
				List<List<Vec3>> a = sideOrder(l.dirs.get(0), l.pads.get(0), true), bb = sideOrder(l.dirs.get(mm), l.pads.get(mm), false);
				int pairs = Math.min(a.size(), bb.size());
				Integer[] rank = new Integer[pairs];
				for (int i = 0; i < pairs; i++) {
					rank[i] = i;
				}
				final int m0 = mm;
				java.util.Arrays.sort(rank, Comparator.comparingDouble(i -> hyp(a.get(i).get(0)) + hyp(bb.get(i).get(0))));
				for (int k = 0; k < pairs; k++) {
					int i = rank[k];
					for (List<Vec3> c : arc(a.get(i), bb.get(i), Vec3.ZERO, outside, 0.3 + 0.15 * (m0 - 1), 0.32, 0.42 + 0.08 * (m0 - 1) + 0.14 * k)) {
						curves.add(c);
						radii.add(0.008);
					}
				}
			}
		} else {
			for (int mm = 1; mm < n; mm++) {
				Vec3 outside = outside(l.dirs.get(0), l.dirs.get(mm));
				List<List<Vec3>> pa = l.pads.get(0), pb = l.pads.get(mm);
				for (int i = 0; i < Math.min(pa.size(), pb.size()); i++) {
					double[] arcSpec = body.arc().get(i);
					List<List<Vec3>> cs;
					if (arcSpec != null) {
						cs = arc(pa.get(i), pb.get(i), body.hinges().get(i), outside, arcSpec[1], arcSpec[0], Double.NaN);
					} else if (centroid(pa.get(i)).distanceTo(centroid(pb.get(i))) > 4.0) {
						cs = pilot(m, l, origin, body.hinges().get(i), pa.get(i), pb.get(i));
					} else {
						cs = jumper(pa.get(i), pb.get(i));
					}
					for (List<Vec3> c : cs) {
						curves.add(c);
						radii.add(body.jr());
					}
				}
			}
			if (n == 1) {
				// A line end: the slack tails tied back under the hinge.
				for (List<Vec3> pads : l.pads.get(0)) {
					for (Vec3 p : pads) {
						curves.add(List.of(p, p.subtract(0, 0.5, 0), p.subtract(0, 0.9, 0)));
						radii.add(body.jr());
					}
				}
			}
		}
		for (int i = 0; i < curves.size(); i++) {
			List<Vec3> w = new ArrayList<>();
			for (Vec3 p : curves.get(i)) {
				w.add(l.world(p).subtract(origin));
			}
			m.tube(w, radii.get(i), 6, alu, -1);
		}
	}

	private static double hyp(Vec3 p) {
		return Math.hypot(p.x, p.z);
	}

	/** Outside the turn (away from both spans): where a nearly straight-through jumper goes round. */
	private static Vec3 outside(Vec3 d0, Vec3 dm) {
		Vec3 o = d0.add(dm).scale(-1);
		if (o.lengthSqr() < 1e-12) {
			// the frame's side of the first span (line_supports._frame's, in the game's mirrored frame)
			return new Vec3(-d0.z, 0, d0.x);
		}
		return o;
	}

	/** A wooden arm's phases ordered left to right across travel (incoming: travel runs against d). */
	private static List<List<Vec3>> sideOrder(Vec3 d, List<List<Vec3>> pads, boolean incoming) {
		Vec3 s = new Vec3(-d.z, 0, d.x);
		Vec3 left = incoming ? s.scale(-1) : s;
		List<List<Vec3>> out = new ArrayList<>(pads);
		out.sort(Comparator.comparingDouble(p -> -p.get(0).dot(left)));
		return out;
	}

	private static List<Vec3> bezier(Vec3 p0, Vec3 p1, Vec3 p2, int n) {
		List<Vec3> out = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			double t = n == 1 ? 0 : i / (double) (n - 1);
			out.add(p0.scale((1 - t) * (1 - t)).add(p1.scale(2 * (1 - t) * t)).add(p2.scale(t * t)));
		}
		return out;
	}

	/** jumper(): the loops hanging below the line between the two assemblies. */
	private static List<List<Vec3>> jumper(List<Vec3> a, List<Vec3> b) {
		Vec3 ca = centroid(a), cb = centroid(b);
		double len = cb.distanceTo(ca);
		double sag = 0.25 + 0.3 * len;
		Vec3 mid = ca.add(cb).scale(0.5).subtract(0, sag, 0);
		return ribbon(a, b, bezier(ca, mid.scale(2).subtract(ca.add(cb).scale(0.5)), cb, 16));
	}

	/** jumper_arc(): round a vertical axis (a pole or the peak's mast), the shorter way unless nearly opposite. */
	private static List<List<Vec3>> arc(List<Vec3> a, List<Vec3> b, Vec3 axis, Vec3 outside, double sag, double minR, double hug) {
		Vec3 ca = centroid(a), cc = centroid(b);
		double thOut = Math.atan2(outside.z, outside.x);
		double ta = Math.atan2(ca.z - axis.z, ca.x - axis.x), tc = Math.atan2(cc.z - axis.z, cc.x - axis.x);
		double ra = Math.hypot(ca.x - axis.x, ca.z - axis.z), rc = Math.hypot(cc.x - axis.x, cc.z - axis.z);
		double dt = floorMod(tc - ta + Math.PI, 2 * Math.PI) - Math.PI;
		if (Math.abs(Math.abs(dt) - Math.PI) < Math.toRadians(10)) {
			double mid = floorMod(thOut - ta + Math.PI, 2 * Math.PI) - Math.PI;
			if ((mid > 0) != (dt > 0)) {
				dt = dt - Math.copySign(2 * Math.PI, dt);
			}
		}
		double spread = 0;
		for (Vec3 p : a) {
			spread = Math.max(spread, Math.hypot(p.x - ca.x, p.z - ca.z));
		}
		int n = Math.max(8, (int) (Math.abs(dt) / 0.2));
		List<Vec3> pts = new ArrayList<>();
		for (int i = 0; i <= n; i++) {
			double t = i / (double) n;
			double th = ta + dt * t;
			double rr = ra + (rc - ra) * t;
			if (!Double.isNaN(hug)) {
				double w = Math.sqrt(Math.sin(Math.PI * t));
				rr = rr * (1 - w) + (hug + spread) * w;
			}
			if (minR != 0) {
				rr = Math.max(minR + spread, rr);
			}
			double y = ca.y + (cc.y - ca.y) * t - sag * Math.sin(Math.PI * t);
			pts.add(new Vec3(axis.x + rr * Math.cos(th), y, axis.z + rr * Math.sin(th)));
		}
		pts.set(0, ca);
		pts.set(n, cc);
		return ribbon(a, b, pts);
	}

	private static double floorMod(double x, double m) {
		return x - Math.floor(x / m) * m;
	}

	/** jumper_pilot(): a long jumper held by a polymer pilot string hanging from the hinge, through a jumper yoke. */
	private static List<List<Vec3>> pilot(LineMesh m, LineGeometry.Layout l, Vec3 origin, Vec3 hinge, List<Vec3> a, List<Vec3> b) {
		Vec3 ca = centroid(a), cb = centroid(b);
		double span = ca.distanceTo(cb);
		double drop = 1.0 + 0.18 * span;
		Vec3 mid = ca.add(cb).scale(0.5);
		Vec3 p = new Vec3(hinge.x + (mid.x - hinge.x) * 0.35, hinge.y - drop, hinge.z + (mid.z - hinge.z) * 0.35);
		Vec3 top = hinge.subtract(0, 0.25, 0);
		Vec3 u = p.subtract(top).normalize();
		longrod(m, l, origin, top, p.subtract(u.scale(0.12)));
		Vec3 t = cb.subtract(ca).multiply(1, 0, 1);
		t = t.lengthSqr() < 1e-12 ? new Vec3(1, 0, 0) : t.normalize();
		Vec3 side = UP.cross(t);
		double hs = 0, hv = 0;
		for (Vec3 q : a) {
			Vec3 o = q.subtract(ca);
			hs = Math.max(hs, Math.abs(o.dot(side)));
			hv = Math.max(hv, Math.abs(o.y));
		}
		int galv = LineModels.colour("galv");
		m.obox(l.world(p).subtract(origin), l.worldDir(side), UP, hs + 0.08, hv + 0.04, 0.012, galv, -1);
		List<Vec3> centre = new ArrayList<>();
		Vec3[][] legs = {{ca, p}, {p, cb}};
		for (Vec3[] e : legs) {
			Vec3 mm = e[0].add(e[1]).scale(0.5).subtract(0, 0.25 + 0.06 * e[1].distanceTo(e[0]), 0);
			List<Vec3> seg = bezier(e[0], mm.scale(2).subtract(e[0].add(e[1]).scale(0.5)), e[1], 12);
			centre.addAll(centre.isEmpty() ? seg : seg.subList(1, seg.size()));
		}
		return ribbon(a, b, centre);
	}

	/** polymer_longrod(): end fittings, the rod, alternating sheds (pitch 8 cm), as the pilot string (no rings). */
	private static void longrod(LineMesh m, LineGeometry.Layout l, Vec3 origin, Vec3 top, Vec3 bottom) {
		int galv = LineModels.colour("galv"), poly = LineModels.colour("polymer");
		Vec3 t = l.world(top).subtract(origin), b = l.world(bottom).subtract(origin);
		Vec3 a = b.subtract(t);
		double len = a.length();
		a = a.scale(1 / len);
		m.cylinder(t, t.add(a.scale(0.18)), 0.035, 8, galv, -1);
		m.tube(List.of(t.add(a.scale(0.18)), b.subtract(a.scale(0.2))), 0.022, 8, poly, -1);
		double[] sheds = {0.07, 0.055};
		for (int i = 0; i < (int) ((len - 0.6) / 0.08); i++) {
			Vec3 c = t.add(a.scale(0.3 + i * 0.08));
			m.cylinder(c, c.add(a.scale(0.012)), sheds[i % 2], 12, poly, -1);
		}
		m.cylinder(b.subtract(a.scale(0.2)), b, 0.035, 8, galv, -1);
	}

	/**
	 * _ribbon(): a bundle's jumper loops round one centre line, each sub-conductor keeping its place across and up / down
	 * all the way, the ends paired by their places (every rotation and mirror tried, the best-matched order kept unless
	 * its loops would touch).
	 */
	static List<List<Vec3>> ribbon(List<Vec3> a, List<Vec3> b, List<Vec3> centre) {
		int nC = centre.size();
		List<Vec3> ls = new ArrayList<>();
		Vec3 prev = null;
		for (int i = 0; i < nC; i++) {
			Vec3 t = centre.get(Math.min(i + 1, nC - 1)).subtract(centre.get(Math.max(i - 1, 0))).multiply(1, 0, 1);
			if (t.lengthSqr() < 1e-12) {
				t = prev != null ? prev : new Vec3(1, 0, 0);
			}
			t = t.normalize();
			prev = t;
			ls.add(UP.cross(t));
		}
		Vec3 ca = centre.get(0), cb = centre.get(nC - 1);
		int n = Math.min(a.size(), b.size());
		double[][] pa = new double[n][], pb = new double[n][];
		for (int k = 0; k < n; k++) {
			pa[k] = new double[] {a.get(k).subtract(ca).dot(ls.get(0)), a.get(k).y - ca.y};
			pb[k] = new double[] {b.get(k).subtract(cb).dot(ls.get(nC - 1)), b.get(k).y - cb.y};
		}
		List<int[]> orders = new ArrayList<>();
		List<Double> costs = new ArrayList<>();
		for (int shift = 0; shift < n; shift++) {
			for (int rev = 0; rev < 2; rev++) {
				int[] order = new int[n];
				double cost = 0;
				for (int k = 0; k < n; k++) {
					order[k] = Math.floorMod(shift + (rev == 1 ? -k : k), n);
					double du = pa[k][0] - pb[order[k]][0], dv = pa[k][1] - pb[order[k]][1];
					cost += du * du + dv * dv;
				}
				orders.add(order);
				costs.add(cost);
			}
		}
		Integer[] idx = new Integer[orders.size()];
		for (int i = 0; i < idx.length; i++) {
			idx[i] = i;
		}
		java.util.Arrays.sort(idx, Comparator.comparingDouble(costs::get));
		List<List<Vec3>> best = null;
		double bestSep = -1;
		for (int oi : idx) {
			int[] order = orders.get(oi);
			List<List<Vec3>> curves = new ArrayList<>();
			for (int k = 0; k < n; k++) {
				Vec3 pA = a.get(k), pB = b.get(order[k]);
				double[] oa = pa[k], ob = pb[order[k]];
				Vec3 ra = pA.subtract(ca).subtract(ls.get(0).scale(oa[0])).subtract(UP.scale(oa[1]));
				Vec3 rb = pB.subtract(cb).subtract(ls.get(nC - 1).scale(ob[0])).subtract(UP.scale(ob[1]));
				List<Vec3> pts = new ArrayList<>();
				for (int i = 0; i < nC; i++) {
					double t = i / (double) (nC - 1);
					double ou = oa[0] + (ob[0] - oa[0]) * t, ov = oa[1] + (ob[1] - oa[1]) * t;
					pts.add(centre.get(i).add(ls.get(i).scale(ou)).add(UP.scale(ov)).add(ra.scale(1 - t)).add(rb.scale(t)));
				}
				curves.add(pts);
			}
			double sep = 9;
			for (int i = 0; i < n; i++) {
				for (int j = i + 1; j < n; j++) {
					sep = Math.min(sep, minGap(curves.get(i), curves.get(j)));
				}
			}
			if (sep >= 0.08) {
				return curves;
			}
			if (best == null || sep > bestSep) {
				best = curves;
				bestSep = sep;
			}
		}
		return best == null ? List.of() : best;
	}

	private static List<Vec3> resample(List<Vec3> pts, double step) {
		List<Vec3> out = new ArrayList<>();
		for (int i = 0; i + 1 < pts.size(); i++) {
			Vec3 p = pts.get(i), q = pts.get(i + 1);
			int n = Math.max(1, (int) (q.distanceTo(p) / step));
			for (int k = 0; k < n; k++) {
				out.add(p.add(q.subtract(p).scale(k / (double) n)));
			}
		}
		out.add(pts.getLast());
		return out;
	}

	private static double minGap(List<Vec3> p, List<Vec3> q) {
		List<Vec3> s = resample(p, 0.05), t = resample(q, 0.05);
		double best = Double.MAX_VALUE;
		for (Vec3 a : s) {
			for (Vec3 b : t) {
				best = Math.min(best, a.distanceToSqr(b));
			}
		}
		return Math.sqrt(best);
	}

	// ------------------------------------------------------------------ legs and plates

	/** A lattice leg carried on down to the ground on a slope (along its batter), from the body's footing level. */
	private static void legExtensions(LineMesh m, LineGeometry.Layout l, Vec3 origin, int[] drops) {
		LineGeometry.Legs legs = l.body.legs();
		if (legs == null) {
			return;
		}
		int galv = LineModels.colour("galv");
		List<double[]> corners = l.legCorners();
		java.util.function.DoubleUnaryOperator half = y -> legs.base() - (legs.base() - legs.waist()) * y / legs.waistY();
		for (int k = 0; k < 4; k++) {
			int d = drops[k];
			if (d <= 0) {
				continue;
			}
			double sx = Math.signum(corners.get(k)[0]), sz = Math.signum(corners.get(k)[1]);
			Vec3 a = new Vec3(sx * half.applyAsDouble(0.35), 0.35, sz * half.applyAsDouble(0.35));
			Vec3 b = new Vec3(sx * half.applyAsDouble(0.35 - d), 0.35 - d, sz * half.applyAsDouble(0.35 - d));
			m.bar(l.world(a).subtract(origin), l.world(b).subtract(origin), 0.24, galv, -1);
		}
	}

	/** The structure's own number (from where it stands): plates read "LINE ... TOWER 214" with it. */
	static String plateText(String format, BlockPos pos) {
		long h = (pos.getX() * 73856093L) ^ (pos.getZ() * 19349663L) ^ (pos.getY() * 83492791L);
		int n3 = 1 + (int) Math.floorMod(h, 499L);
		int n4 = 1000 + (int) Math.floorMod(h, 9000L);
		return format.replace("{n4}", Integer.toString(n4)).replace("{n}", Integer.toString(n3));
	}

	/** The letters on each number plate (the plate is drawn blank in the body; tools/line_models.py). */
	private static void plates(LineMesh m, LineGeometry.Layout l, Vec3 origin, String format, List<LineGeometry.Plate> plates) {
		String text = plateText(format, l.anchor);
		for (LineGeometry.Plate p : plates) {
			text(m, l, origin, p, text, 0, 1, 0.62, 1.4, -1);
		}
	}

	/**
	 * A line of letters across a plate (or a band of it, {@code b0} to {@code b1} of its height, from the top), centred,
	 * {@code size} of the band high, shrunk to leave {@code margin} band heights at the ends.
	 */
	private static void text(LineMesh m, LineGeometry.Layout l, Vec3 origin, LineGeometry.Plate p, String text, double b0, double b1, double size,
			double margin, int light) {
		float px = LineModels.glyphPx(), cell = LineModels.glyphCell();
		Vec3 tl0 = p.corners()[0], bl0 = p.corners()[1], tr0 = p.corners()[3];
		Vec3 right = tr0.subtract(tl0).normalize(), down = bl0.subtract(tl0).normalize();
		Vec3 normal = bl0.subtract(tl0).cross(tr0.subtract(tl0)).normalize();
		double w = p.w(), h = p.h() * (b1 - b0);
		Vec3 tl = tl0.add(down.scale(p.h() * b0));
		double fs = h * size;
		double adv = 0;
		for (char ch : text.toCharArray()) {
			float[] g = LineModels.glyph(ch);
			adv += g == null ? 0 : g[4];
		}
		while (adv * fs / px > w - h * margin && fs > 0.002) {
			fs *= 0.95;
		}
		double scale = fs / px;
		double s = (w - adv * scale) / 2;
		Vec3 lift = normal.scale(0.0015);
		for (char ch : text.toCharArray()) {
			float[] g = LineModels.glyph(ch);
			if (g == null) {
				continue;
			}
			if (ch != ' ') {
				double x0 = s - 2 * scale, x1 = x0 + g[5] * scale;
				double y0 = h / 2 - cell / 2 * scale, y1 = h / 2 + cell / 2 * scale;
				Vec3 a = tl.add(right.scale(x0)).add(down.scale(y0)).add(lift);
				Vec3 b = tl.add(right.scale(x0)).add(down.scale(y1)).add(lift);
				Vec3 c = tl.add(right.scale(x1)).add(down.scale(y1)).add(lift);
				Vec3 d = tl.add(right.scale(x1)).add(down.scale(y0)).add(lift);
				float[] uv = {g[0], g[1], g[0], g[3], g[2], g[3], g[2], g[1]};
				m.quad(l.world(a).subtract(origin), l.world(b).subtract(origin), l.world(c).subtract(origin), l.world(d).subtract(origin),
						l.worldDir(normal), uv, 0xFFFFFFFF, light);
			}
			s += g[4] * scale;
		}
	}

	/** The Service Pedestal's meter display: what it has delivered (kWh, six figures) over its voltage and current. */
	private static void meter(LineMesh m, LineGeometry.Layout l, Vec3 origin, LineGeometry.Plate display, Live live) {
		long kwh = (long) (live.delivered() * io.github.jimbozoomer.jugcraft.powerline.LineAnchorBlockEntity.KWH_PER_JE) % 1_000_000L;
		int amps = Math.round(live.load() / 255F * 200);
		// (on the lit display: as bright as it)
		text(m, l, origin, display, String.format("%06d kWh", kwh), 0.06, 0.54, 0.8, 0.5, LightCoordsUtil.FULL_BRIGHT);
		text(m, l, origin, display, "240 V  " + amps + " A", 0.52, 0.96, 0.7, 0.5, LightCoordsUtil.FULL_BRIGHT);
	}

	/** The meter's figures as the cache sees them (whole kWh, amps): it is rebuilt when they change. */
	static List<Object> meterKey(Live live) {
		return List.of((long) (live.delivered() * io.github.jimbozoomer.jugcraft.powerline.LineAnchorBlockEntity.KWH_PER_JE),
				Math.round(live.load() / 255F * 200));
	}

	// ------------------------------------------------------------------ part 2: sleeves, terminations, leads, box jumpers

	/**
	 * A superconducting pylon's cradle sleeves (sc_scene.line_ops): white composite bent along each sleeve's arc, the
	 * mouths in the cable's jacket with a lit status ring just inside each.
	 */
	private static void sleeves(LineMesh m, LineGeometry.Layout l, Vec3 origin, int lod) {
		int white = LineModels.colour("composite_white"), jacket = LineModels.colour("cryo_jacket"), glow = LineModels.colour("sc_glow");
		int sides = lod == 0 ? 18 : lod == 1 ? 10 : 6;
		for (List<Vec3> arc : l.sleeves) {
			List<Vec3> w = new ArrayList<>();
			for (Vec3 p : arc) {
				w.add(l.world(p).subtract(origin));
			}
			int n = w.size();
			boolean ends = n > 7;
			m.tube(ends ? w.subList(1, n - 1) : w.subList(0, n - 1), 0.12, sides, white, -1);
			if (ends) {
				m.cylinder(w.get(0), w.get(1), 0.124, sides, jacket, -1);
			} else {
				m.cylinder(w.get(0), w.get(0).subtract(w.get(1).subtract(w.get(0)).normalize().scale(0.05)), 0.12, sides, white, -1);
			}
			m.cylinder(w.get(n - 2), w.get(n - 1), 0.124, sides, jacket, -1);
			if (lod < 2) {
				Vec3[][] mouths = ends ? new Vec3[][] {{w.get(0), w.get(1)}, {w.get(n - 1), w.get(n - 2)}} : new Vec3[][] {{w.get(n - 1), w.get(n - 2)}};
				for (Vec3[] e : mouths) {
					Vec3 d = e[1].subtract(e[0]).normalize();
					m.cylinder(e[0].add(d.scale(0.16)), e[0].add(d.scale(0.19)), 0.123, sides, glow, LightCoordsUtil.FULL_BRIGHT);
				}
			}
		}
	}

	/**
	 * A Unit Substation's cryogenic terminations' cable-entry sleeves, swivelled to the superconducting line
	 * (line_supports.unit_substation): white composite out from the vessel, a lit ring at the mouth.
	 */
	private static void vesselSleeves(LineMesh m, LineGeometry.Layout l, Vec3 origin, int lod) {
		int white = LineModels.colour("composite_white"), glow = LineModels.colour("sc_glow");
		int sides = lod == 0 ? 16 : 8;
		Vec3 d = l.vesselDir;
		for (Vec3 v : l.box.vessels()) {
			Vec3 a = l.world(v.add(d.scale(0.3))).subtract(origin), b = l.world(v.add(d.scale(0.72))).subtract(origin);
			Vec3 c = l.world(v.add(d.scale(0.77))).subtract(origin);
			m.cylinder(a, b, 0.13, sides, white, -1);
			m.cylinder(b, c, 0.135, sides, glow, LightCoordsUtil.FULL_BRIGHT);
		}
	}

	/**
	 * Where a superconducting cable leaves a Unit Substation's yard through its chain-link fence: a penetration sleeve
	 * round the cable and a galvanized plate on the fence, the fabric cut back to it.
	 */
	private static void penetrations(LineMesh m, LineGeometry.Layout l, Vec3 origin, LineWires.Span span) {
		int galv = LineModels.colour("galv"), white = LineModels.colour("composite_white");
		Vec3 home = l.world(Vec3.ZERO);
		for (LineWires.Wire w : span.conductors()) {
			boolean fromA = w.a().distanceToSqr(home) <= w.b().distanceToSqr(home);
			java.util.function.DoubleFunction<Vec3> at = t -> local(l, w.at(fromA ? t : 1 - t));
			double lo = 0, hi = -1;
			for (int i = 1; i <= 200; i++) {
				double t = i * 0.0025;
				if (outsideYard(at.apply(t))) {
					hi = t;
					lo = t - 0.0025;
					break;
				}
			}
			if (hi < 0 || outsideYard(at.apply(lo))) {
				continue;
			}
			for (int k = 0; k < 30; k++) {
				double mid = (lo + hi) / 2;
				if (outsideYard(at.apply(mid))) {
					hi = mid;
				} else {
					lo = mid;
				}
			}
			Vec3 c = at.apply((lo + hi) / 2);
			if (c.y > FENCE_H + 0.5) {
				continue;                      // over the fence
			}
			Vec3 t = at.apply(Math.min(1, hi + 0.01)).subtract(at.apply(Math.max(0, lo - 0.01))).normalize();
			Vec3 cw = l.world(c).subtract(origin), tw = l.worldDir(t);
			m.cylinder(cw.subtract(tw.scale(0.16)), cw.add(tw.scale(0.16)), 0.105, 12, white, -1);
			// the plate on the fence (clear of its line posts: every 2 m, at odd x on the front and back runs, even z on
			// the sides; the yard's fence_run): on the front or back run its face is along x, on a side run along z
			boolean side = Math.abs(Math.abs(c.x) - YARD_HALF) < 1e-3;
			double along = side ? c.z : c.x + 1;
			double post = Math.abs(along - 2 * Math.round(along / 2));
			if (c.y + 0.16 < FENCE_H - 0.03 && c.y - 0.16 > 0.06 && post > 0.2) {
				Vec3 u = l.worldDir(side ? new Vec3(0, 0, 1) : new Vec3(1, 0, 0));
				m.obox(cw, u, UP, 0.16, 0.16, 0.012, galv, -1);
			}
		}
	}

	/** A Unit Substation's yard: its fence half-width, depth (from the anchor's fence line) and fence height. */
	private static final double YARD_HALF = 5, YARD_DEPTH = 10, FENCE_H = 2.1;

	private static boolean outsideYard(Vec3 p) {
		return p.z < 0 || p.z > YARD_DEPTH || Math.abs(p.x) > YARD_HALF;
	}

	/** A world point in a structure's own frame. */
	private static Vec3 local(LineGeometry.Layout l, Vec3 w) {
		double dx = w.x - (l.anchor.getX() + 0.5), dz = w.z - (l.anchor.getZ() + 0.5);
		return new Vec3(dx * l.cos - dz * l.sin, w.y - l.anchor.getY(), dx * l.sin + dz * l.cos);
	}

	/** The pole-mount transformer's lead (line_supports.pole_transformer): off the nearest phase, down to its cutout. */
	private static void xfmrLead(LineMesh m, LineGeometry.Layout l, Vec3 origin, LineGeometry.Xfmr x) {
		Vec3 lead = x.lead();
		Vec3 best = null;
		for (List<Vec3> ph : l.leadPhases) {
			for (Vec3 p : ph) {
				if (best == null || p.distanceToSqr(lead) < best.distanceToSqr(lead)) {
					best = p;
				}
			}
		}
		if (best == null) {
			return;
		}
		if (l.leadFromPads) {
			leadPath(m, l, origin, slack(best, lead));
			return;
		}
		Vec3 a = best.add(l.leadAlong.scale(0.35)).add(0, 0.03, 0);
		Vec3 b = a.add(-l.xfmrSide * 0.1, -0.2, 0);
		leadPath(m, l, origin, List.of(a, b, lead));
	}

	/** A lead from a dead-end's jumper pad, hanging slack to where it lands. */
	private static List<Vec3> slack(Vec3 pad, Vec3 to) {
		Vec3 mid = pad.add(to).scale(0.5).subtract(0, 0.12 + 0.08 * pad.distanceTo(to), 0);
		return bezier(pad, mid.scale(2).subtract(pad.add(to).scale(0.5)), to, 10);
	}

	/**
	 * The riser's three leads (line_supports.pole_riser): from the phases nearest its side, in order across the pole, down
	 * to the three cutouts; a lead from the far side of the pole goes round it.
	 */
	private static void riserLeads(LineMesh m, LineGeometry.Layout l, Vec3 origin, LineGeometry.Riser r) {
		int side = l.riserSide;
		List<Vec3> tops = new ArrayList<>();
		for (List<Vec3> ph : l.leadPhases) {
			if (!ph.isEmpty()) {
				tops.add(centroid(ph));
			}
		}
		tops.sort(Comparator.comparingDouble((Vec3 p) -> -side * p.x).thenComparingDouble(p -> -p.y));
		if (tops.size() > 3) {
			tops = new ArrayList<>(tops.subList(0, 3));
		}
		tops.sort(Comparator.comparingDouble((Vec3 p) -> -p.x).thenComparingDouble(p -> -p.y));
		for (int k = 0; k < Math.min(tops.size(), r.leads().size()); k++) {
			Vec3 t = tops.get(k), lead = r.leads().get(k);
			if (l.leadFromPads) {
				leadPath(m, l, origin, slack(t, lead));
				continue;
			}
			double off = lead.z >= 0 ? 0.4 : -0.4;
			Vec3 a = t.add(l.leadAlong.scale(off)).add(0, 0.03, 0);
			if (Math.signum(t.x) == side || Math.abs(t.x) < 0.05) {
				leadPath(m, l, origin, List.of(a, a.add(side * 0.2, -0.25, 0), lead));
			} else {
				leadPath(m, l, origin, List.of(a, new Vec3(side * 0.8, t.y - 0.6, a.z), lead));
			}
		}
	}

	private static void leadPath(LineMesh m, LineGeometry.Layout l, Vec3 origin, List<Vec3> local) {
		List<Vec3> w = new ArrayList<>();
		for (Vec3 p : local) {
			w.add(l.world(p).subtract(origin));
		}
		m.tube(w, 0.006, 6, LineModels.colour("copper_wire"), -1);
	}

	/**
	 * A box gantry's jumpers (line_supports.unit_substation, converter_station): from each strain assembly's pads, slack
	 * down to the disconnect's jaw (a substation) or gathered to the smoothing reactor's terminal (a converter station).
	 */
	private static void boxJumpers(LineMesh m, LineGeometry.Layout l, Vec3 origin) {
		LineGeometry.Box box = l.box;
		int alu = LineModels.colour("aluminium");
		double r = l.kind == LineKind.CONVERTER_STATION ? 0.016 : 0.014;
		for (int i = 0; i < l.pads.size(); i++) {
			List<List<Vec3>> pads = l.pads.get(i);
			for (int h = 0; h < pads.size() && h < box.jumpers().size(); h++) {
				LineGeometry.Jumper j = box.jumpers().get(h);
				List<Vec3> ps = pads.get(h);
				Vec3 mean = centroid(ps);
				for (Vec3 p : ps) {
					Vec3 control = ps.size() > 1 ? j.control().add(p.subtract(mean).scale(0.6)) : j.control();
					List<Vec3> w = new ArrayList<>();
					for (Vec3 q : bezier(p, control, j.target(), 14)) {
						w.add(l.world(q).subtract(origin));
					}
					m.tube(w, r, 6, alu, -1);
				}
			}
		}
	}

	// ------------------------------------------------------------------ the footings' ground

	/** How far below the anchor's level the ground under each lattice footing is (0 to 12), in legCorners order. */
	static int[] drops(net.minecraft.world.level.Level level, LineGeometry.Layout l) {
		int[] out = new int[4];
		List<double[]> corners = l.legCorners();
		for (int k = 0; k < corners.size() && k < 4; k++) {
			BlockPos foot = l.anchor.offset(l.cellOf(corners.get(k)[0], 0, corners.get(k)[1]));
			out[k] = groundDrop(level, foot);
		}
		return out;
	}

	static int groundDrop(net.minecraft.world.level.Level level, BlockPos foot) {
		for (int d = 0; d <= 12; d++) {
			BlockPos below = foot.below(d + 1);
			if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
				return d;
			}
		}
		return 12;
	}

	/** Whether a structure is drawn from its model (everything but the switchyard's line gantry, drawn by its site). */
	static boolean modelled(@Nullable LineKind kind) {
		return LineGeometry.modelled(kind);
	}
}
