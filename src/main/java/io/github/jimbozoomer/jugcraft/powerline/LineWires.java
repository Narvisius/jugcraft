package io.github.jimbozoomer.jugcraft.powerline;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Where a support's conductors are in the world and how the wires between two structures hang. Shared by the
 * server (wire path checks, electrocution) and the client (drawing), so what you see is what can shock you.
 */
public final class LineWires {
	/** Points per wire when sampling its curve. */
	public static final int SAMPLES = 16;

	private LineWires() {
	}

	/**
	 * Which way a support faces: its local z axis runs along its line (the mean direction to the structures it is
	 * wired to, with wires leaving the other way folded onto it), so its crossarms stand across the line.
	 */
	public static double yaw(BlockPos self, List<BlockPos> connections) {
		double ax = 0, az = 0;
		for (BlockPos other : connections) {
			double dx = other.getX() - self.getX(), dz = other.getZ() - self.getZ();
			double len = Math.hypot(dx, dz);
			if (len < 1e-6) {
				continue;
			}
			dx /= len;
			dz /= len;
			if (ax * dx + az * dz < 0) {
				dx = -dx;
				dz = -dz;
			}
			ax += dx;
			az += dz;
		}
		return ax == 0 && az == 0 ? 0 : Math.atan2(ax, az);
	}

	/** The structure's conductor points in world space, for wires to a structure of {@code partner}'s kind. Boxes keep their fixed orientation. */
	public static List<Vec3> conductors(BlockPos anchor, LineKind kind, List<BlockPos> connections, LineKind partner) {
		double yaw = kind.isBox() ? 0 : yaw(anchor, connections);
		double c = Math.cos(yaw), s = Math.sin(yaw);
		List<Vec3> out = new ArrayList<>();
		for (Vec3 p : kind.attachPoints(partner)) {
			// local x runs across the line (cos, -sin), local z along it (sin, cos)
			double wx = p.x * c + p.z * s;
			double wz = -p.x * s + p.z * c;
			out.add(new Vec3(anchor.getX() + 0.5 + wx, anchor.getY() + p.y, anchor.getZ() + 0.5 + wz));
		}
		return out;
	}

	/** One wire: its end points and its sag at mid-span. */
	public record Wire(Vec3 a, Vec3 b, double sag) {
		public Vec3 at(double t) {
			return new Vec3(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t - 4 * sag * t * (1 - t), a.z + (b.z - a.z) * t);
		}
	}

	/**
	 * The wires between two structures. Both ends' conductors are put in order across the line (left to right as
	 * seen along it, top to bottom on the same side) and matched in that order, so no two wires of a span ever
	 * cross; when one end has fewer conductors, the matching that keeps the ends closest in position wins. A
	 * Pad-Mount Transformer is fed by a riser down the pole and a buried cable, so it has no overhead wire.
	 */
	public static List<Wire> between(BlockPos a, LineKind ka, List<BlockPos> ca, BlockPos b, LineKind kb, List<BlockPos> cb) {
		List<Wire> wires = new ArrayList<>();
		if (ka == LineKind.PAD_MOUNT_TRANSFORMER || kb == LineKind.PAD_MOUNT_TRANSFORMER) {
			return wires;
		}
		List<Vec3> pa = conductors(a, ka, ca, kb);
		List<Vec3> pb = conductors(b, kb, cb, ka);
		if (pa.isEmpty() || pb.isEmpty()) {
			return wires;
		}
		double span = Math.sqrt(a.distSqr(b));
		boolean supercon = ka == LineKind.SUPERCONDUCTING_PYLON || kb == LineKind.SUPERCONDUCTING_PYLON;
		double sag = span * (supercon ? 0.01 : 0.03);
		// Across the line, to put each end's conductors in order.
		double lx = b.getX() - a.getX(), lz = b.getZ() - a.getZ();
		double len = Math.max(1e-6, Math.hypot(lx, lz));
		double px = -lz / len, pz = lx / len;
		Vec3 mid = Vec3.atCenterOf(a).add(Vec3.atCenterOf(b)).scale(0.5);
		java.util.function.ToDoubleFunction<Vec3> side = p -> (p.x - mid.x) * px + (p.z - mid.z) * pz;
		java.util.Comparator<Vec3> order = java.util.Comparator.comparingDouble(side::applyAsDouble).thenComparing(p -> -p.y);
		List<Vec3> sa = new ArrayList<>(pa);
		List<Vec3> sb = new ArrayList<>(pb);
		sa.sort(order);
		sb.sort(order);
		boolean swap = sa.size() > sb.size();
		List<Vec3> few = swap ? sb : sa;
		List<Vec3> many = swap ? sa : sb;
		int f = few.size(), m = many.size();
		// Cheapest order-keeping matching of every few-side conductor to a distinct many-side one.
		double[][] cost = new double[f][m];
		for (int i = 0; i < f; i++) {
			for (int j = 0; j < m; j++) {
				cost[i][j] = Math.abs(side.applyAsDouble(few.get(i)) - side.applyAsDouble(many.get(j))) + 0.5 * Math.abs(few.get(i).y - many.get(j).y);
			}
		}
		double[][] best = new double[f + 1][m + 1];
		int[][] from = new int[f + 1][m + 1];
		for (int i = 1; i <= f; i++) {
			java.util.Arrays.fill(best[i], Double.MAX_VALUE);
			for (int j = i; j <= m; j++) {
				// Either conductor j-1 of many is unused, or it takes few conductor i-1.
				if (best[i][j - 1] < best[i][j]) {
					best[i][j] = best[i][j - 1];
					from[i][j] = -1;
				}
				double take = best[i - 1][j - 1] == Double.MAX_VALUE ? Double.MAX_VALUE : best[i - 1][j - 1] + cost[i - 1][j - 1];
				if (take < best[i][j]) {
					best[i][j] = take;
					from[i][j] = j - 1;
				}
			}
		}
		int[] match = new int[f];
		for (int i = f, j = m; i > 0; ) {
			if (from[i][j] >= 0) {
				match[i - 1] = from[i][j];
				i--;
				j--;
			} else {
				j--;
			}
		}
		for (int i = 0; i < f; i++) {
			Vec3 p = few.get(i), q = many.get(match[i]);
			wires.add(swap ? new Wire(q, p, sag) : new Wire(p, q, sag));
		}
		return wires;
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
