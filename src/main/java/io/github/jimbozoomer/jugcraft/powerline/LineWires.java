package io.github.jimbozoomer.jugcraft.powerline;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * How the wires between two structures hang (power lines round 2: each end's conductors from its layout,
 * {@link LineGeometry}). Shared by the server (wire path checks, electrocution) and the client (drawing), so what you
 * see is what can shock you.
 *
 * <p>The phases are matched across the span as the approved turn sheets were drawn (line_supports' turn scene): at
 * each end they are put in order across the line (left to right as seen along it, then top to bottom), and matched
 * in that order; within a phase the sub-conductors are matched the same way; so no two wires of a span cross. When
 * one end has fewer phases, the order-keeping matching that keeps the ends closest wins; a phase of one conductor
 * (a box's terminal) takes all of a bundle's sub-conductors. A Pad-Mount Transformer is fed by a riser down the pole
 * and a buried cable, so it has no overhead wire; a Service Pedestal's drop (triplex) sags more than a line. A
 * superconducting cable hangs nearly straight from its messenger (the messenger sags 2 %); its last span into a
 * substation's termination has no messenger.
 */
public final class LineWires {
	/** Points per wire when sampling its curve. */
	public static final int SAMPLES = 16;
	/** A service drop's sag (of its length, at mid-span: tools/box_scene.py's triplex). */
	public static final double DROP_SAG = 0.025;
	/** A superconducting cable's messenger's sag (tools/sc_scene.py). */
	public static final double MESSENGER_SAG = 0.02;

	private LineWires() {
	}

	/** The way a structure faces (LineGeometry.yaw; kept for callers of the old name). */
	public static double yaw(BlockPos self, List<BlockPos> connections) {
		return LineGeometry.yaw(self, connections);
	}

	/** One wire: its end points and its sag at mid-span. */
	public record Wire(Vec3 a, Vec3 b, double sag) {
		public Vec3 at(double t) {
			return new Vec3(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t - 4 * sag * t * (1 - t), a.z + (b.z - a.z) * t);
		}
	}

	/** A span: each phase's sub-conductor wires (a bundle's together, for its spacers), and the shield wires. */
	public record Span(List<List<Wire>> phases, List<Wire> shields, LineGeometry.@Nullable WireSpec spec) {
		public List<Wire> conductors() {
			List<Wire> out = new ArrayList<>();
			phases.forEach(out::addAll);
			return out;
		}
	}

	/** What stands at a position in a level (null if nothing loaded there). */
	public static Function<BlockPos, @Nullable LineKind> kinds(@Nullable Level level) {
		return pos -> level == null ? null : LineRegistry.of(level).kindAt(pos);
	}

	/** The live conductors between two structures (no shield wires: they are grounded). */
	public static List<Wire> between(@Nullable Level level, BlockPos a, LineKind ka, List<BlockPos> ca, BlockPos b, LineKind kb, List<BlockPos> cb) {
		return span(level, a, ka, ca, b, kb, cb).conductors();
	}

	/** The span between two structures (each with its own wires, which set how it faces and stands). */
	public static Span span(@Nullable Level level, BlockPos a, LineKind ka, List<BlockPos> ca, BlockPos b, LineKind kb, List<BlockPos> cb) {
		Function<BlockPos, @Nullable LineKind> kinds = pos -> pos.equals(a) ? ka : pos.equals(b) ? kb : kinds(level).apply(pos);
		return span(LineGeometry.layout(level, ka, a, ca, kinds), LineGeometry.layout(level, kb, b, cb, kinds));
	}

	/** The span between two laid-out structures. */
	public static Span span(LineGeometry.Layout la, LineGeometry.Layout lb) {
		List<List<Wire>> phases = new ArrayList<>();
		List<Wire> shields = new ArrayList<>();
		LineKind ka = la.kind, kb = lb.kind;
		if (ka == LineKind.PAD_MOUNT_TRANSFORMER || kb == LineKind.PAD_MOUNT_TRANSFORMER) {
			return new Span(phases, shields, null);
		}
		List<List<Vec3>> pa = la.phasesTo(lb.anchor), pb = lb.phasesTo(la.anchor);
		if (pa.isEmpty() || pb.isEmpty()) {
			return new Span(phases, shields, null);
		}
		boolean supercon = ka == LineKind.SUPERCONDUCTING_PYLON || kb == LineKind.SUPERCONDUCTING_PYLON;
		LineGeometry.Support sup = supercon ? (ka == LineKind.SUPERCONDUCTING_PYLON ? la.support : lb.support)
				: la.support != null ? la.support : lb.support;
		LineGeometry.@Nullable WireSpec spec = sup == null ? null : sup.wire();
		boolean drop = ka == LineKind.SERVICE_PEDESTAL || kb == LineKind.SERVICE_PEDESTAL;
		double sagK = drop ? DROP_SAG : spec != null ? spec.sag() : 0.03;
		double shieldSag = supercon ? MESSENGER_SAG : sagK * 0.9;
		// Across the line, to put each end's conductors in order (left of travel from a to b positive).
		Vec3 a3 = Vec3.atBottomCenterOf(la.anchor), b3 = Vec3.atBottomCenterOf(lb.anchor);
		double lx = b3.x - a3.x, lz = b3.z - a3.z;
		double len = Math.max(1e-6, Math.hypot(lx, lz));
		double leftX = lz / len, leftZ = -lx / len;
		Vec3 mid = a3.add(b3).scale(0.5);
		java.util.function.ToDoubleFunction<Vec3> side = p -> (p.x - mid.x) * leftX + (p.z - mid.z) * leftZ;
		java.util.function.ToDoubleFunction<List<Vec3>> phSide = ph -> ph.stream().mapToDouble(side).average().orElse(0);
		java.util.function.ToDoubleFunction<List<Vec3>> phY = ph -> ph.stream().mapToDouble(p -> p.y).average().orElse(0);
		Comparator<List<Vec3>> phOrder = Comparator.comparingDouble((List<Vec3> ph) -> Math.round(phSide.applyAsDouble(ph) * 1000) / 1000.0)
				.thenComparingDouble(ph -> -phY.applyAsDouble(ph));
		List<List<Vec3>> sa = new ArrayList<>(pa), sb = new ArrayList<>(pb);
		sa.sort(phOrder);
		sb.sort(phOrder);
		int[][] match = match(sa, sb, (x, y) -> Math.abs(phSide.applyAsDouble(x) - phSide.applyAsDouble(y)) + 0.5 * Math.abs(phY.applyAsDouble(x) - phY.applyAsDouble(y)));
		Comparator<Vec3> subOrder = Comparator.comparingDouble((Vec3 p) -> Math.round(side.applyAsDouble(p) * 1000) / 1000.0).thenComparingDouble(p -> -p.y);
		for (int[] m : match) {
			List<Vec3> x = new ArrayList<>(sa.get(m[0])), y = new ArrayList<>(sb.get(m[1]));
			x.sort(subOrder);
			y.sort(subOrder);
			List<Wire> bundle = new ArrayList<>();
			if (x.size() == 1 || y.size() == 1) {
				// A terminal of one conductor takes every sub-conductor of a bundle.
				for (Vec3 p : x) {
					for (Vec3 q : y) {
						bundle.add(wire(p, q, sagK));
					}
				}
			} else {
				for (int[] s : match(x, y, (p, q) -> Math.abs(side.applyAsDouble(p) - side.applyAsDouble(q)) + 0.5 * Math.abs(p.y - q.y))) {
					bundle.add(wire(x.get(s[0]), y.get(s[1]), sagK));
				}
			}
			phases.add(bundle);
		}
		// The shield wires: in order across the line, one to one (or the first to the first).
		List<Vec3> ha = la.shieldWorld(), hb = lb.shieldWorld();
		Comparator<Vec3> across = Comparator.comparingDouble(side);
		ha.sort(across);
		hb.sort(across);
		if (!ha.isEmpty() && !hb.isEmpty() && !(supercon && (ka.isBox() || kb.isBox()))) {
			if (ha.size() == hb.size()) {
				for (int i = 0; i < ha.size(); i++) {
					shields.add(wire(ha.get(i), hb.get(i), shieldSag));
				}
			} else {
				shields.add(wire(ha.get(0), hb.get(0), shieldSag));
			}
		}
		return new Span(phases, shields, spec);
	}

	private static Wire wire(Vec3 p, Vec3 q, double sagK) {
		return new Wire(p, q, Math.hypot(q.x - p.x, q.z - p.z) * sagK);
	}

	/**
	 * The cheapest order-keeping matching of every item of the shorter list to a distinct item of the longer one
	 * ({index in a, index in b} pairs, in order).
	 */
	static <T> int[][] match(List<T> a, List<T> b, java.util.function.ToDoubleBiFunction<T, T> cost) {
		boolean swap = a.size() > b.size();
		List<T> few = swap ? b : a, many = swap ? a : b;
		int f = few.size(), m = many.size();
		double[][] best = new double[f + 1][m + 1];
		int[][] from = new int[f + 1][m + 1];
		for (int i = 1; i <= f; i++) {
			java.util.Arrays.fill(best[i], Double.MAX_VALUE);
			for (int j = i; j <= m; j++) {
				if (best[i][j - 1] < best[i][j]) {
					best[i][j] = best[i][j - 1];
					from[i][j] = -1;
				}
				double take = best[i - 1][j - 1] == Double.MAX_VALUE ? Double.MAX_VALUE : best[i - 1][j - 1] + cost.applyAsDouble(few.get(i - 1), many.get(j - 1));
				if (take < best[i][j]) {
					best[i][j] = take;
					from[i][j] = j - 1;
				}
			}
		}
		int[][] out = new int[f][];
		for (int i = f, j = m; i > 0; ) {
			if (from[i][j] >= 0) {
				out[i - 1] = swap ? new int[] {from[i][j], i - 1} : new int[] {i - 1, from[i][j]};
				i--;
				j--;
			} else {
				j--;
			}
		}
		return out;
	}

	/** Shortest distance from a point to a wire (sampled). */
	public static double distance(Wire wire, Vec3 point) {
		double best = Double.MAX_VALUE;
		Vec3 prev = wire.at(0);
		for (int i = 1; i <= SAMPLES; i++) {
			Vec3 next = wire.at(i / (double) SAMPLES);
			best = Math.min(best, segmentDistance(prev, next, point));
			prev = next;
		}
		return best;
	}

	private static double segmentDistance(Vec3 a, Vec3 b, Vec3 p) {
		Vec3 ab = b.subtract(a);
		double len2 = ab.lengthSqr();
		double t = len2 < 1e-9 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / len2));
		return a.add(ab.scale(t)).distanceTo(p);
	}
}
