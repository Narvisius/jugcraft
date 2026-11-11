package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

/**
 * Quads a power line structure's renderer builds itself (power lines round 2): wires and their spacers, jumpers,
 * pilot strings, line-end tails, leg extensions, plate letters. Positions are in blocks from the anchor block's
 * corner; each quad has its UVs (in client/LineModels' texture), colour, normal and light (or -1: by height, from the
 * structure's light samples). Built when a structure's wires change and kept until they change again.
 */
final class LineMesh {
	private float[] pos = new float[12 * 64], uv = new float[8 * 64], nrm = new float[3 * 64];
	private int[] colour = new int[64], light = new int[64];
	private int count;

	int size() {
		return count;
	}

	private void grow() {
		int n = colour.length * 2;
		pos = java.util.Arrays.copyOf(pos, n * 12);
		uv = java.util.Arrays.copyOf(uv, n * 8);
		nrm = java.util.Arrays.copyOf(nrm, n * 3);
		colour = java.util.Arrays.copyOf(colour, n);
		light = java.util.Arrays.copyOf(light, n);
	}

	/** One quad; corners in the order the game draws its front (counter-clockwise seen from in front). */
	void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Vec3 n, float[] uvs, int col, int lit) {
		if (count == colour.length) {
			grow();
		}
		Vec3[] p = {a, b, c, d};
		for (int i = 0; i < 4; i++) {
			pos[count * 12 + i * 3] = (float) p[i].x;
			pos[count * 12 + i * 3 + 1] = (float) p[i].y;
			pos[count * 12 + i * 3 + 2] = (float) p[i].z;
		}
		System.arraycopy(uvs, 0, uv, count * 8, 8);
		nrm[count * 3] = (float) n.x;
		nrm[count * 3 + 1] = (float) n.y;
		nrm[count * 3 + 2] = (float) n.z;
		colour[count] = col;
		light[count] = lit;
		count++;
	}

	/** UVs of a whole region (u0, v0, u1, v1) over a quad, corner by corner as {@link #quad} takes them. */
	static float[] uvs(float[] r) {
		return new float[] {r[0], r[1], r[0], r[3], r[2], r[3], r[2], r[1]};
	}

	/** The white cell at every corner (flat colours). */
	static float[] flat() {
		float[] w = LineModels.white();
		return new float[] {w[0], w[1], w[0], w[1], w[0], w[1], w[0], w[1]};
	}

	void draw(PoseStack.Pose pose, VertexConsumer c, int[] lights, int tint) {
		LineModels.Xf x = new LineModels.Xf(pose);
		for (int q = 0; q < count; q++) {
			x.normal(nrm[q * 3], nrm[q * 3 + 1], nrm[q * 3 + 2]);
			int col = LineModels.multiply(colour[q], tint);
			for (int i = 0; i < 4; i++) {
				int o = q * 12 + i * 3;
				float y = pos[o + 1];
				int l = light[q] == -1 ? lights[Math.max(0, Math.min(lights.length - 1, (int) (y / LineModels.STEP)))] : light[q];
				x.vertex(c, pos[o], y, pos[o + 2], col, uv[q * 8 + i * 2], uv[q * 8 + i * 2 + 1], l);
			}
		}
	}

	// ------------------------------------------------------------------ shapes

	/** The frame at a point of a curve: a unit normal and binormal round the tangent, carried along so it does not twist. */
	private static Vec3[] frame(Vec3 tangent, Vec3 previous) {
		Vec3 t = tangent.normalize();
		Vec3 n = previous == null ? (Math.abs(t.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)) : previous;
		n = n.subtract(t.scale(n.dot(t)));
		if (n.lengthSqr() < 1e-10) {
			n = Math.abs(t.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
			n = n.subtract(t.scale(n.dot(t)));
		}
		n = n.normalize();
		return new Vec3[] {t, n, t.cross(n)};
	}

	/** A continuous tube along points, flat colour (wires, jumpers). */
	void tube(List<Vec3> points, double radius, int sides, int col, int lit) {
		int n = points.size();
		if (n < 2) {
			return;
		}
		Vec3[][] ring = new Vec3[n][sides];
		Vec3[][] rad = new Vec3[n][sides];
		Vec3 normal = null;
		for (int i = 0; i < n; i++) {
			Vec3 t = i == 0 ? points.get(1).subtract(points.get(0))
					: i == n - 1 ? points.get(i).subtract(points.get(i - 1)) : points.get(i + 1).subtract(points.get(i - 1));
			if (t.lengthSqr() < 1e-12) {
				t = new Vec3(0, 1, 0);
			}
			Vec3[] f = frame(t, normal);
			normal = f[1];
			for (int k = 0; k < sides; k++) {
				double a = Math.PI * 2 * k / sides;
				rad[i][k] = f[1].scale(Math.cos(a)).add(f[2].scale(Math.sin(a)));
				ring[i][k] = points.get(i).add(rad[i][k].scale(radius));
			}
		}
		float[] w = flat();
		for (int i = 0; i + 1 < n; i++) {
			for (int k = 0; k < sides; k++) {
				int k1 = (k + 1) % sides;
				Vec3 nn = rad[i][k].add(rad[i][k1]).add(rad[i + 1][k]).add(rad[i + 1][k1]).normalize();
				quad(ring[i][k], ring[i][k1], ring[i + 1][k1], ring[i + 1][k], nn, w, col, lit);
			}
		}
	}

	/** A short capped cylinder from a to b (flat colour). */
	void cylinder(Vec3 a, Vec3 b, double r, int sides, int col, int lit) {
		Vec3 axis = b.subtract(a);
		if (axis.lengthSqr() < 1e-12) {
			return;
		}
		tube(List.of(a, b), r, sides, col, lit);
		Vec3 u = axis.normalize();
		cap(a, u.scale(-1), r, sides, col, lit);
		cap(b, u, r, sides, col, lit);
	}

	private void cap(Vec3 c, Vec3 out, double r, int sides, int col, int lit) {
		Vec3[] f = frame(out, null);
		float[] w = flat();
		Vec3[] ring = new Vec3[sides];
		for (int k = 0; k < sides; k++) {
			double a = Math.PI * 2 * k / sides;
			ring[k] = c.add(f[1].scale(Math.cos(a) * r)).add(f[2].scale(Math.sin(a) * r));
		}
		for (int k = 0; k + 1 < sides; k += 2) {
			quad(c, ring[k], ring[(k + 1) % sides], ring[(k + 2) % sides], out, w, col, lit);
		}
	}

	/** A box about centre c with axes u, v (w = u x v) and half sizes (flat colour). */
	void obox(Vec3 c, Vec3 u, Vec3 v, double hu, double hv, double hw, int col, int lit) {
		Vec3 w = u.cross(v);
		Vec3[] p = new Vec3[8];
		for (int i = 0; i < 8; i++) {
			p[i] = c.add(u.scale(hu * ((i & 1) == 0 ? -1 : 1))).add(v.scale(hv * ((i & 2) == 0 ? -1 : 1))).add(w.scale(hw * ((i & 4) == 0 ? -1 : 1)));
		}
		float[] f = flat();
		// faces: -u, +u, -v, +v, -w, +w (each wound counter-clockwise seen from outside)
		quad(p[0], p[4], p[6], p[2], u.scale(-1), f, col, lit);
		quad(p[1], p[3], p[7], p[5], u, f, col, lit);
		quad(p[0], p[1], p[5], p[4], v.scale(-1), f, col, lit);
		quad(p[2], p[6], p[7], p[3], v, f, col, lit);
		quad(p[0], p[2], p[3], p[1], w.scale(-1), f, col, lit);
		quad(p[4], p[5], p[7], p[6], w, f, col, lit);
	}

	/** A square bar from a to b, any direction (flat colour). */
	void bar(Vec3 a, Vec3 b, double width, int col, int lit) {
		Vec3 d = b.subtract(a);
		double len = d.length();
		if (len < 1e-6) {
			return;
		}
		Vec3 u = d.scale(1 / len);
		Vec3 ref = Math.abs(u.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
		Vec3 v = ref.cross(u).normalize();
		obox(a.add(b).scale(0.5), u, v, len / 2, width / 2, width / 2, col, lit);
	}
}
