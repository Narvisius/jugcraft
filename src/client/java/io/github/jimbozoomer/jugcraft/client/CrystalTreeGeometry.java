package io.github.jimbozoomer.jugcraft.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A Crystal Tree grown from its seed (the placed block's position), so every tree placed is its own (Justin, 22 October
 * 2026: "make a different variant for each tree you put down"). Ported from the approved mockup's generator
 * (claude/src/dronetower/tools/mockups/lt3_model.py); tools/crystal_tree.py makes the planter, the bench and the atlas.
 *
 * <ul>
 * <li>the trunk: four dark strands braided 1.25 turns round a core, splaying into the ground, gold veins in the grooves;
 * they part at {@link #SPLIT_Y};</li>
 * <li>the limbs: 8 to 12 off the strands' tops, each grown step by step with its own wander (now and then a kink),
 * bending over as it goes, kept over the planter and under the hall's ceiling; side branches off them, three levels
 * down, at random;</li>
 * <li>the roots: each strand splits into two that wander out, rolling over and under the basalt, with rootlets;</li>
 * <li>the threads: thin dark woody threads hanging from the branches at every length (no two ending level), faceted
 * gold crystals on short spurs along each and a bigger drop at its end, each crystal with a glowing core and a soft
 * glow (a camera-facing halo the renderer turns);</li>
 * <li>the ground: a few dark basalt rocks and clumps of glowing bead moss.</li>
 * </ul>
 * Everything is in the master cell's frame (the planter's middle over x, z 0.5; the floor at y 0). Each glowing vertex
 * carries its path distance from the roots ({@code s}), so the renderer can send pulses of light up the veins, out the
 * limbs and down through the crystals; each thread's vertices carry the thread and how far down it they are, so it can
 * sway.
 */
final class CrystalTreeGeometry {
	/** Floats a vertex: bark (x y z u v nx ny nz thread weight), veins (x y z u v s), glow (x y z u v nx ny nz s base thread weight). */
	static final int BARK = 10, VEIN = 6, GLOW = 12;
	/** Floats a halo (x y z radius s base thread weight) and a thread (phase dirx dirz amplitude). */
	static final int HALO = 8, THREAD = 4;
	static final double CX = 0.5, CZ = 0.5, GRAVEL_Y = 0.70, R_IN = 3.33, SPLIT_Y = 4.05;
	static final double CROWN_R = 3.4, CEILING = 8.3, LOWEST = 2.25, BARK_V = 1.6;
	private static final double[] LIGHT = unit(new double[] {0.4, 0.8, -0.45});

	final float[] bark, veins, glow, halos, threads;
	/** The bark split by quads: what never moves (no thread; baked into the chunk) and what sways with the threads. */
	final float[] barkStill, barkSway;

	private CrystalTreeGeometry(Builder b) {
		bark = b.bark.toArray();
		veins = b.veins.toArray();
		glow = b.glow.toArray();
		halos = b.halos.toArray();
		threads = b.threads.toArray();
		float[][] split = split(bark);
		barkStill = split[0];
		barkSway = split[1];
	}

	/** Bark quads none of whose corners hang from a thread, and the rest. */
	static float[][] split(float[] bark) {
		final int Q = 4 * BARK;
		int still = 0;
		for (int o = 0; o + Q <= bark.length; o += Q) {
			if (stillQuad(bark, o)) {
				still++;
			}
		}
		int quads = bark.length / Q;
		float[] a = new float[still * Q], b = new float[(quads - still) * Q];
		int ia = 0, ib = 0;
		for (int o = 0; o + Q <= bark.length; o += Q) {
			if (stillQuad(bark, o)) {
				System.arraycopy(bark, o, a, ia, Q);
				ia += Q;
			} else {
				System.arraycopy(bark, o, b, ib, Q);
				ib += Q;
			}
		}
		return new float[][] {a, b};
	}

	private static boolean stillQuad(float[] bark, int o) {
		for (int k = 0; k < 4; k++) {
			if ((int) bark[o + k * BARK + 8] >= 0) {
				return false;
			}
		}
		return true;
	}

	int quads() {
		return (bark.length / BARK + veins.length / VEIN + glow.length / GLOW) / 4;
	}

	/** The tree for {@code seed}, its uvs from {@code tiles} (u0 v0 u1 v1 per tile). */
	static CrystalTreeGeometry grow(long seed, Map<String, float[]> tiles) {
		Builder b = new Builder(new Random(seed), tiles);
		b.ground();
		b.roots();
		List<Top> tops = b.trunk();
		List<Branch> branches = b.crown(tops);
		for (Branch br : branches) {
			b.tube(br.pts, br.rad, br.level == 0 ? 8 : br.level == 1 ? 6 : 5, true, -1, null);
			if (br.level == 0) {
				List<double[]> vein = new ArrayList<>();
				double[] vs = new double[br.pts.size() - 2];
				for (int i = 1; i < br.pts.size() - 1; i++) {
					vein.add(add(br.pts.get(i), new double[] {0, br.rad[i] * 0.9, 0}));
					vs[i - 1] = br.s[i];
				}
				b.glowTube(vein, vs, 0.01, 4);
			}
		}
		b.hangThreads(branches);
		return new CrystalTreeGeometry(b);
	}

	/** Where a pulse is: how bright a vein or a crystal is at path distance s, t seconds in (0.35 between pulses, 1 at a crest). */
	static float pulse(double s, double t) {
		double x = (s - t * 0.9) / 3.2;
		double f = x - Math.floor(x);
		return (float) (0.35 + 0.65 * Math.exp(-Math.pow((f - 0.5) / 0.09, 2)));
	}

	// ------------------------------------------------------------------ small vector helpers

	static double[] add(double[] a, double[] b) {
		return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
	}

	static double[] sub(double[] a, double[] b) {
		return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
	}

	static double[] mul(double[] a, double k) {
		return new double[] {a[0] * k, a[1] * k, a[2] * k};
	}

	static double dot(double[] a, double[] b) {
		return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
	}

	static double[] cross(double[] a, double[] b) {
		return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
	}

	static double len(double[] a) {
		return Math.sqrt(dot(a, a));
	}

	static double[] unit(double[] a) {
		double l = len(a);
		return l < 1e-12 ? new double[] {0, 1, 0} : mul(a, 1 / l);
	}

	/** v turned by ang about axis (Rodrigues). */
	static double[] rot(double[] v, double[] axis, double ang) {
		double[] k = unit(axis);
		double c = Math.cos(ang), s = Math.sin(ang);
		return add(add(mul(v, c), mul(cross(k, v), s)), mul(k, dot(k, v) * (1 - c)));
	}

	private static final double[] UP = {0, 1, 0};

	private record Top(double[] p, double[] d, double s, double r, int k) {
	}

	private record Branch(List<double[]> pts, double[] rad, double[] s, int level) {
	}

	/** A growing float list. */
	static final class Floats {
		private float[] data = new float[1024];
		private int size;

		void add(double... v) {
			if (size + v.length > data.length) {
				data = Arrays.copyOf(data, Math.max(data.length * 2, size + v.length));
			}
			for (double x : v) {
				data[size++] = (float) x;
			}
		}

		int size() {
			return size;
		}

		float[] toArray() {
			return Arrays.copyOf(data, size);
		}
	}

	// ------------------------------------------------------------------ growing

	private static final class Builder {
		final Random rnd;
		final Map<String, float[]> tiles;
		final Floats bark = new Floats(), veins = new Floats(), glow = new Floats(), halos = new Floats(), threads = new Floats();

		Builder(Random rnd, Map<String, float[]> tiles) {
			this.rnd = rnd;
			this.tiles = tiles;
		}

		double uniform(double a, double b) {
			return a + (b - a) * rnd.nextDouble();
		}

		double normal(double sd) {
			return rnd.nextGaussian() * sd;
		}

		float[] tile(String name) {
			float[] t = tiles.get(name);
			return t == null ? new float[] {0, 0, 1 / 1024f, 1 / 512f} : t;
		}

		double u(float[] t, double f) {
			return t[0] + (t[2] - t[0]) * Math.min(1, Math.max(0, f));
		}

		double v(float[] t, double f) {
			return t[1] + (t[3] - t[1]) * Math.min(1, Math.max(0, f));
		}

		/** Four corners in the order the game draws a front face (anticlockwise seen from where {@code n} points). */
		double[][] facing(double[][] c, double[] n, double[][] uv) {
			double[] e1 = sub(c[1], c[0]), e2 = sub(c[2], c[0]);
			double[] f = cross(e1, e2);
			if (len(f) < 1e-12) {
				f = cross(sub(c[2], c[0]), sub(c[3], c[0]));
			}
			if (dot(f, n) < 0) {
				double[][] r = {c[0], c[3], c[2], c[1]};
				double[][] ruv = {uv[0], uv[3], uv[2], uv[1]};
				System.arraycopy(ruv, 0, uv, 0, 4);
				return r;
			}
			return c;
		}

		// ---- bark

		/** One bark-type quad (corners, their normals and uv fractions), of tile {@code tileName}, swaying with a thread. */
		void barkQuad(double[][] c, double[][] n, double[][] uvf, String tileName, int thread, double[] weight) {
			float[] t = tile(tileName);
			double[] mean = unit(add(add(n[0], n[1]), add(n[2], n[3])));
			double[][] uv = uvf.clone();
			int[] order = {0, 1, 2, 3};
			double[][] cc = facing(c, mean, uv);
			if (cc != c) {
				order = new int[] {0, 3, 2, 1};
			}
			for (int k = 0; k < 4; k++) {
				int i = order[k];
				bark.add(cc[k][0], cc[k][1], cc[k][2], u(t, uv[k][0]), v(t, uv[k][1]), n[i][0], n[i][1], n[i][2], thread,
						weight == null ? 0 : weight[i]);
			}
		}

		/**
		 * A branch: rings round each point, turned smoothly along it; the bark tiles round it (one or two tiles round)
		 * and along it (1.6 m a tile, a segment split where it runs past a tile's edge), so no seam shows.
		 */
		void tube(List<double[]> pts, double[] rad, int sides, boolean cap, int thread, double[] weights) {
			int n = pts.size();
			double[][] dirs = new double[n][];
			for (int i = 0; i < n; i++) {
				dirs[i] = unit(sub(pts.get(Math.min(i + 1, n - 1)), pts.get(Math.max(i - 1, 0))));
			}
			double[] uRef = Math.abs(dirs[0][1]) > 0.9 ? new double[] {1, 0, 0} : unit(cross(dirs[0], UP));
			int round = 2 * Math.PI * rad[0] > 1.6 ? 2 : 1;
			// points, with extra ones where the bark's v passes a whole tile
			List<double[]> P = new ArrayList<>();
			List<double[]> D = new ArrayList<>();
			List<Double> R = new ArrayList<>(), V = new ArrayList<>(), W = new ArrayList<>();
			double v = 0;
			P.add(pts.get(0));
			D.add(dirs[0]);
			R.add(rad[0]);
			V.add(0.0);
			W.add(weights == null ? 0 : weights[0]);
			for (int i = 0; i < n - 1; i++) {
				double L = len(sub(pts.get(i + 1), pts.get(i)));
				if (L < 1e-9) {
					continue;
				}
				double a = v / BARK_V, b = (v + L) / BARK_V;
				for (int k = (int) Math.floor(a) + 1; k < b - 1e-6; k++) {
					double t = (k * BARK_V - v) / L;
					P.add(add(pts.get(i), mul(sub(pts.get(i + 1), pts.get(i)), t)));
					D.add(unit(add(mul(dirs[i], 1 - t), mul(dirs[i + 1], t))));
					R.add(rad[i] + (rad[i + 1] - rad[i]) * t);
					V.add(k * BARK_V);
					W.add(weights == null ? 0 : weights[i] + (weights[i + 1] - weights[i]) * t);
				}
				P.add(pts.get(i + 1));
				D.add(dirs[i + 1]);
				R.add(rad[i + 1]);
				V.add(v + L);
				W.add(weights == null ? 0 : weights[i + 1]);
				v += L;
			}
			int m = P.size();
			double[][][] rings = new double[m][sides][];
			double[][][] norms = new double[m][sides][];
			for (int i = 0; i < m; i++) {
				double[] d = D.get(i);
				double[] u = sub(uRef, mul(d, dot(uRef, d)));
				u = len(u) > 1e-6 ? unit(u) : unit(cross(d, Math.abs(d[1]) > 0.9 ? new double[] {1, 0, 0} : UP));
				double[] w = cross(d, u);
				for (int k = 0; k < sides; k++) {
					double ang = 2 * Math.PI * k / sides;
					double[] radial = add(mul(u, Math.cos(ang)), mul(w, Math.sin(ang)));
					norms[i][k] = radial;
					rings[i][k] = add(P.get(i), mul(radial, R.get(i)));
				}
			}
			double uw = (double) round / sides;
			for (int i = 0; i < m - 1; i++) {
				double va = (V.get(i) / BARK_V) % 1.0;
				double vb = va + (V.get(i + 1) - V.get(i)) / BARK_V;
				if (vb > 1 + 1e-6) {
					va = 0;
					vb = (V.get(i + 1) - V.get(i)) / BARK_V;
				}
				vb = Math.min(vb, 1);
				double wi = W.get(i), wj = W.get(i + 1);
				for (int k = 0; k < sides; k++) {
					int k1 = (k + 1) % sides;
					double u0 = (k * uw) % 1.0;
					barkQuad(new double[][] {rings[i + 1][k], rings[i + 1][k1], rings[i][k1], rings[i][k]},
							new double[][] {norms[i + 1][k], norms[i + 1][k1], norms[i][k1], norms[i][k]},
							new double[][] {{u0, vb}, {u0 + uw, vb}, {u0 + uw, va}, {u0, va}}, "dark_bark", thread, new double[] {wj, wj, wi, wi});
				}
			}
			if (cap) {
				double[] c = P.get(m - 1), d = D.get(m - 1);
				double w = W.get(m - 1);
				for (int k = 0; k < sides; k++) {
					int k1 = (k + 1) % sides;
					barkQuad(new double[][] {rings[m - 1][k], rings[m - 1][k1], c, c}, new double[][] {d, d, d, d},
							new double[][] {{0.4, 0.45}, {0.6, 0.45}, {0.5, 0.5}, {0.5, 0.5}}, "dark_bark", thread, new double[] {w, w, w, w});
				}
			}
		}

		// ---- veins (full bright, pulsing)

		void glowTube(List<double[]> pts, double[] s, double r, int sides) {
			int n = pts.size();
			if (n < 2) {
				return;
			}
			float[] t = tile("vein");
			double cu = (t[0] + t[2]) / 2, cv = (t[1] + t[3]) / 2;
			double[][][] rings = new double[n][sides][];
			for (int i = 0; i < n; i++) {
				double[] d = unit(sub(pts.get(Math.min(i + 1, n - 1)), pts.get(Math.max(i - 1, 0))));
				double[] u = unit(cross(d, Math.abs(d[1]) > 0.9 ? new double[] {1, 0, 0} : UP));
				double[] w = cross(d, u);
				for (int k = 0; k < sides; k++) {
					double ang = 2 * Math.PI * k / sides;
					rings[i][k] = add(pts.get(i), mul(add(mul(u, Math.cos(ang)), mul(w, Math.sin(ang))), r));
				}
			}
			for (int i = 0; i < n - 1; i++) {
				for (int k = 0; k < sides; k++) {
					int k1 = (k + 1) % sides;
					double[][] c = {rings[i + 1][k], rings[i + 1][k1], rings[i][k1], rings[i][k]};
					double[] out = sub(mul(add(rings[i][k], rings[i][k1]), 0.5), pts.get(i));
					double[][] uv = {{0, 0}, {0, 0}, {0, 0}, {0, 0}};
					double[] sv = {s[i + 1], s[i + 1], s[i], s[i]};
					double[][] cc = facing(c, out, uv);
					if (cc != c) {
						sv = new double[] {sv[0], sv[3], sv[2], sv[1]};
					}
					for (int q = 0; q < 4; q++) {
						veins.add(cc[q][0], cc[q][1], cc[q][2], cu, cv, sv[q]);
					}
				}
			}
		}

		// ---- glow quads (crystals, cores, beads)

		void glowQuad(double[][] c, double[] n, String tileName, double[][] uvf, double s, double base, int thread, double weight) {
			float[] t = tile(tileName);
			double[][] uv = uvf.clone();
			double[][] cc = facing(c, n, uv);
			for (int k = 0; k < 4; k++) {
				glow.add(cc[k][0], cc[k][1], cc[k][2], u(t, uv[k][0]), v(t, uv[k][1]), n[0], n[1], n[2], s, base, thread, weight);
			}
		}

		static final double[][] FULL = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};

		/**
		 * A faceted gold crystal hanging from {@code top} along {@code axis}: a short pointed cap, a prism, a long point
		 * below (a pendant drop), each facet as bright as it faces the key light; a glowing core inside; a halo.
		 */
		void crystal(double[] top, double[] axis, double length, double radius, double s, int thread, double weight) {
			axis = unit(axis);
			double[] u = unit(cross(axis, Math.abs(axis[0]) < 0.9 ? new double[] {1, 0, 0} : UP));
			double[] w = cross(axis, u);
			int sides = 6;
			double twist = rnd.nextDouble() * Math.PI;
			double[][] r1 = new double[sides][], r2 = new double[sides][];
			double[] c1 = add(top, mul(axis, length * 0.16)), c2 = add(top, mul(axis, length * 0.42)), bottom = add(top, mul(axis, length));
			for (int i = 0; i < sides; i++) {
				double a = twist + 2 * Math.PI * i / sides;
				double[] radial = add(mul(u, Math.cos(a)), mul(w, Math.sin(a)));
				r1[i] = add(c1, mul(radial, radius));
				r2[i] = add(c2, mul(radial, radius * 0.96));
			}
			double[] centre = add(top, mul(axis, length * 0.4));
			for (int i = 0; i < sides; i++) {
				int j = (i + 1) % sides;
				double[][][] faces = {{r1[i], r1[j], top, top}, {r1[i], r1[j], r2[j], r2[i]}, {r2[i], r2[j], bottom, bottom}};
				for (double[][] f : faces) {
					double[] mid = mul(add(add(f[0], f[1]), f[2]), 1 / 3.0);
					double[] n = unit(sub(mid, centre));
					double[] fn = cross(sub(f[1], f[0]), sub(f[2], f[0]));
					if (len(fn) > 1e-12) {
						fn = unit(fn);
						n = dot(fn, n) < 0 ? mul(fn, -1) : fn;
					}
					double l = Math.max(0, dot(n, LIGHT));
					double lit = 0.62 + 0.38 * l + 0.25 * Math.pow(l, 8);
					glowQuad(f, n, "crystal", FULL, s, lit, thread, weight);
				}
			}
			// the glowing core: two crossed quads (the glow pass draws both faces of a quad)
			double[] core = add(top, mul(axis, length * 0.38));
			double rc = radius * 0.55;
			for (int turn = 0; turn < 2; turn++) {
				double[] uu = rot(u, axis, turn * Math.PI / 2);
				double[][] q = {sub(sub(core, mul(uu, rc)), mul(axis, rc * 1.4)), sub(add(core, mul(uu, rc)), mul(axis, rc * 1.4)),
						add(add(core, mul(uu, rc)), mul(axis, rc * 1.4)), add(sub(core, mul(uu, rc)), mul(axis, rc * 1.4))};
				double[] nrm = cross(axis, uu);
				glowQuad(q, nrm, "pod", FULL, s, 1.0, thread, weight);
			}
			halos.add(core[0], core[1], core[2], Math.max(length * 0.95, radius * 3.2), s, 1.0, thread, weight);
		}

		// ---- the trunk, the roots, the crown

		List<Top> trunk() {
			List<Top> tops = new ArrayList<>();
			int n = 22;
			double twist = 1.25 * 2 * Math.PI / (SPLIT_Y - GRAVEL_Y);
			double[] ys = new double[n];
			for (int i = 0; i < n; i++) {
				ys[i] = GRAVEL_Y - 0.08 + (SPLIT_Y - GRAVEL_Y + 0.08) * i / (n - 1);
			}
			List<double[]> core = new ArrayList<>();
			double[] coreR = new double[n];
			for (int i = 0; i < n; i++) {
				core.add(new double[] {CX, ys[i], CZ});
				coreR[i] = 0.17;
			}
			tube(core, coreR, 12, true, -1, null);
			for (int k = 0; k < 4; k++) {
				List<double[]> pts = new ArrayList<>();
				double[] rad = new double[n];
				for (int i = 0; i < n; i++) {
					double y = ys[i], a = k * Math.PI / 2 + twist * (y - GRAVEL_Y), r = rr(y);
					pts.add(new double[] {CX + r * Math.cos(a), y, CZ + r * Math.sin(a)});
					rad[i] = 0.165 + 0.05 * Math.exp(-(y - GRAVEL_Y) / 0.3) - 0.03 * (y - GRAVEL_Y) / (SPLIT_Y - GRAVEL_Y);
				}
				tube(pts, rad, 10, true, -1, null);
				double[] s = lengths(pts);
				tops.add(new Top(pts.get(n - 1), unit(sub(pts.get(n - 1), pts.get(n - 2))), s[n - 1], rad[n - 1], k));
				// the vein in the groove between this strand and the next
				List<double[]> vein = new ArrayList<>();
				double a0 = k * Math.PI / 2 + Math.PI / 4;
				for (int i = 1; i < n - 1; i++) {
					double y = ys[i], a = a0 + twist * (y - GRAVEL_Y), r = rr(y) + 0.115;
					vein.add(new double[] {CX + r * Math.cos(a), y, CZ + r * Math.sin(a)});
				}
				glowTube(vein, lengths(vein), 0.016, 5);
			}
			return tops;
		}

		/** How far a trunk strand is from the axis at height y: splayed into the ground, parting near the top. */
		static double rr(double y) {
			double base = 0.15 + 0.22 * Math.exp(-(y - GRAVEL_Y) / 0.28);
			double top = 0.16 * Math.pow(Math.max(0, (y - (SPLIT_Y - 0.6)) / 0.6), 2);
			return base + top;
		}

		static double[] lengths(List<double[]> pts) {
			double[] s = new double[pts.size()];
			for (int i = 1; i < pts.size(); i++) {
				s[i] = s[i - 1] + len(sub(pts.get(i), pts.get(i - 1)));
			}
			return s;
		}

		void roots() {
			for (int k = 0; k < 4; k++) {
				double a0 = k * Math.PI / 2;
				for (int j = 0; j < 2; j++) {
					double az = a0 + (j - 0.5) * uniform(0.5, 0.9) + uniform(-0.2, 0.2);
					double length = uniform(1.3, 2.8);
					int n = (int) (length / 0.12);
					List<double[]> pts = new ArrayList<>();
					List<Double> rad = new ArrayList<>();
					pts.add(new double[] {CX + 0.3 * Math.cos(az), GRAVEL_Y + 0.3, CZ + 0.3 * Math.sin(az)});
					rad.add(0.15);
					double phase = uniform(0, 2 * Math.PI), heading = az;
					for (int i = 0; i < n; i++) {
						double f = (i + 1.0) / n;
						heading += normal(0.12);
						double r = 0.3 + length * f;
						double y = GRAVEL_Y + 0.30 * Math.pow(1 - f, 1.6) + 0.07 * Math.sin(phase + f * 9) * (1 - 0.6 * f) - 0.05 * f;
						double[] q = {CX + 0.3 * Math.cos(az) + (r - 0.3) * Math.cos(heading), y, CZ + 0.3 * Math.sin(az) + (r - 0.3) * Math.sin(heading)};
						if (Math.hypot(q[0] - CX, q[2] - CZ) > R_IN - 0.15) {
							break;
						}
						pts.add(q);
						rad.add(Math.max(0.018, 0.15 * Math.pow(1 - f, 1.3) + 0.02));
					}
					if (pts.size() < 2) {
						continue;
					}
					// rounded off (a root bends; it doesn't zigzag)
					double[] radA = rad.stream().mapToDouble(Double::doubleValue).toArray();
					for (int pass = 0; pass < 2; pass++) {
						List<double[]> sp = new ArrayList<>();
						List<Double> sr = new ArrayList<>();
						sp.add(pts.get(0));
						sr.add(radA[0]);
						for (int i = 0; i < pts.size() - 1; i++) {
							double[] a = pts.get(i), c = pts.get(i + 1);
							sp.add(add(mul(a, 0.75), mul(c, 0.25)));
							sp.add(add(mul(a, 0.25), mul(c, 0.75)));
							sr.add(radA[i] * 0.75 + radA[i + 1] * 0.25);
							sr.add(radA[i] * 0.25 + radA[i + 1] * 0.75);
						}
						sp.add(pts.get(pts.size() - 1));
						sr.add(radA[radA.length - 1]);
						pts = sp;
						radA = sr.stream().mapToDouble(Double::doubleValue).toArray();
					}
					tube(pts, radA, 8, true, -1, null);
					List<double[]> vein = new ArrayList<>();
					for (int i = 1; i < pts.size() - 1; i += 2) {
						vein.add(add(pts.get(i), new double[] {0, radA[i] * 0.9, 0}));
					}
					glowTube(vein, lengths(vein), 0.011, 4);
					// rootlets
					for (int i = 12; i < pts.size() - 8; i += 12) {
						if (rnd.nextDouble() >= 0.55) {
							continue;
						}
						double h = Math.atan2(pts.get(i + 4)[2] - pts.get(i)[2], pts.get(i + 4)[0] - pts.get(i)[0])
								+ (rnd.nextBoolean() ? 1 : -1) * uniform(0.6, 1.2);
						List<double[]> lp = new ArrayList<>();
						List<Double> lr = new ArrayList<>();
						lp.add(pts.get(i));
						lr.add(radA[i] * 0.6);
						double[] q = pts.get(i).clone();
						int steps = 3 + rnd.nextInt(3);
						for (int m = 0; m < steps; m++) {
							h += normal(0.25);
							q = add(q, new double[] {Math.cos(h) * 0.11, 0, Math.sin(h) * 0.11});
							q[1] = GRAVEL_Y + 0.02 * Math.sin(m * 1.7) - 0.01 * m;
							lp.add(q.clone());
							lr.add(Math.max(0.01, lr.get(lr.size() - 1) * 0.8));
						}
						tube(lp, lr.stream().mapToDouble(Double::doubleValue).toArray(), 6, true, -1, null);
					}
				}
			}
		}

		List<Branch> crown(List<Top> tops) {
			List<Branch> out = new ArrayList<>();
			for (Top top : tops) {
				double a = top.k * Math.PI / 2 + 1.25 * 2 * Math.PI;
				double[] p = {CX + 0.31 * Math.cos(a), SPLIT_Y, CZ + 0.31 * Math.sin(a)};
				int count = rnd.nextDouble() < 0.35 ? 3 : 2;
				for (int j = 0; j < count; j++) {
					double az = a + uniform(-0.85, 0.85);
					double elev = Math.toRadians(uniform(36, 62));
					double[] d = {Math.cos(az) * Math.cos(elev), Math.sin(elev), Math.sin(az) * Math.cos(elev)};
					double length = uniform(2.4, 4.6), r0 = 0.13 + uniform(-0.02, 0.02);
					Limb limb = limb(p, d, length, r0, 0.035, uniform(0.45, 0.85), 0.22);
					double s0 = top.s + 0.4;
					double[] s = lengths(limb.pts);
					for (int i = 0; i < s.length; i++) {
						s[i] += s0;
					}
					Branch br = new Branch(limb.pts, limb.rad, s, 0);
					out.add(br);
					children(out, br, 1);
				}
			}
			return out;
		}

		private record Limb(List<double[]> pts, double[] rad) {
		}

		/**
		 * A branch grown step by step: each step it wanders a little (more at a kink now and then), bends over (droop,
		 * radians a metre), and turns back in if it would leave the crown or reach the ceiling.
		 */
		Limb limb(double[] p, double[] d, double length, double r0, double r1, double droop, double wander) {
			double step = 0.22;
			int n = Math.max(2, (int) (length / step));
			List<double[]> pts = new ArrayList<>();
			double[] rad = new double[n + 1];
			pts.add(p.clone());
			rad[0] = r0;
			double[] q = p.clone();
			for (int i = 0; i < n; i++) {
				double[] side = len(cross(d, UP)) > 1e-3 ? unit(cross(d, UP)) : new double[] {1, 0, 0};
				double kink = rnd.nextDouble() < 0.12 ? 2.6 : 1.0;
				d = rot(d, UP, normal(wander) * kink);
				d = rot(d, side, normal(wander * 0.7) * kink - droop * step);
				double[] next = add(q, mul(d, step));
				double r = Math.hypot(next[0] - CX, next[2] - CZ);
				if (r > CROWN_R) {
					double[] inward = {-(next[0] - CX) / r, 0, -(next[2] - CZ) / r};
					d = unit(add(add(d, mul(inward, 0.8)), new double[] {0, -0.2, 0}));
					next = add(q, mul(d, step));
				}
				if (next[1] > CEILING) {
					d = unit(sub(d, new double[] {0, 0.9, 0}));
					next = add(q, mul(d, step));
				}
				// a branch may bow over, but not plunge (nor hang down by the trunk)
				if (d[1] < -0.55 || next[1] < LOWEST + 0.9) {
					d = unit(add(d, new double[] {0, next[1] < LOWEST + 0.9 ? 0.9 : 0.5, 0}));
					next = add(q, mul(d, step));
				}
				d = unit(d);
				q = next;
				pts.add(q);
				rad[i + 1] = r0 + (r1 - r0) * (i + 1) / n;
			}
			return new Limb(pts, rad);
		}

		void children(List<Branch> out, Branch parent, int level) {
			if (level > 3) {
				return;
			}
			double chance = level == 1 ? 0.42 : level == 2 ? 0.30 : 0.22;
			List<double[]> pts = parent.pts;
			for (int i = 2; i < pts.size() - 1; i++) {
				if (rnd.nextDouble() > chance) {
					continue;
				}
				double[] dPar = unit(sub(pts.get(i + 1), pts.get(i)));
				double[] side = len(cross(dPar, UP)) > 1e-3 ? unit(cross(dPar, UP)) : new double[] {1, 0, 0};
				double[] d = rot(dPar, UP, (rnd.nextBoolean() ? 1 : -1) * Math.toRadians(uniform(25, 60)));
				d = rot(d, side, -Math.toRadians(uniform(-25, 20)));
				double remaining = (pts.size() - i) * 0.22;
				double length = remaining * uniform(0.35, 0.85);
				if (length < 0.35) {
					continue;
				}
				double r0 = parent.rad[i] * 0.72;
				Limb limb = limb(sub(pts.get(i), mul(dPar, parent.rad[i] * 0.5)), unit(d), length, r0, 0.02, uniform(0.3, 0.8), 0.26);
				double[] s = lengths(limb.pts);
				for (int k = 0; k < s.length; k++) {
					s[k] += parent.s[i];
				}
				Branch br = new Branch(limb.pts, limb.rad, s, level);
				out.add(br);
				children(out, br, level + 1);
			}
		}

		// ---- the threads and their crystals

		/** About this many threads a tree, however many branches it grew (so a big tree costs no more to draw). */
		static final int THREADS = 520;

		void hangThreads(List<Branch> branches) {
			double total = 0;
			for (Branch br : branches) {
				double[] L = lengths(br.pts);
				total += L[L.length - 1];
			}
			double spread = Math.max(1, total * 0.8 / 0.115 / THREADS);
			for (Branch br : branches) {
				double[] L = lengths(br.pts);
				double d = uniform(0.3, 0.6);
				while (d < L[L.length - 1]) {
					int i = 0;
					while (i < L.length - 2 && L[i + 1] < d) {
						i++;
					}
					double f = (d - L[i]) / Math.max(L[i + 1] - L[i], 1e-6);
					double[] p = add(br.pts.get(i), mul(sub(br.pts.get(i + 1), br.pts.get(i)), f));
					double rHere = br.rad[i] + (br.rad[i + 1] - br.rad[i]) * f;
					if (Math.hypot(p[0] - CX, p[2] - CZ) > 0.9 && p[1] > LOWEST + 0.5) {
						double[] top = sub(p, new double[] {0, rHere * 0.8, 0});
						double pick = rnd.nextDouble();
						double length = pick < 0.4 ? uniform(0.3, 0.9) : pick < 0.8 ? uniform(0.8, 1.8) : uniform(1.6, 3.2);
						if (length > top[1] - LOWEST) {
							length = (top[1] - LOWEST) * uniform(0.55, 1.0);
						}
						if (length > 0.25) {
							thread(top, br.s[i] + (br.s[i + 1] - br.s[i]) * f, length);
						}
					}
					d += uniform(0.08, 0.15) * spread;
				}
			}
		}

		/**
		 * A thin dark woody thread hanging from a branch, in a gentle curve with small kinks, crystals hung along it on
		 * short spurs and a bigger drop at its end. Its sway: a phase, a direction and how far, kept in {@link #threads}.
		 */
		void thread(double[] top, double s0, double length) {
			int index = threads.size() / THREAD;
			double dir = uniform(0, 2 * Math.PI);
			threads.add(uniform(0, 2 * Math.PI), Math.cos(dir), Math.sin(dir), 0.02 + 0.025 * length);
			int n = Math.max(3, (int) (length / 0.18));
			double[] sway = {normal(1) * 0.05, 0, normal(1) * 0.05};
			List<double[]> pts = new ArrayList<>();
			double[] w = new double[n + 1];
			pts.add(top.clone());
			for (int i = 0; i < n; i++) {
				double f = (i + 1.0) / n;
				pts.add(new double[] {top[0] + sway[0] * Math.sin(Math.PI * f * 0.9) + normal(0.012), top[1] - length * f,
						top[2] + sway[2] * Math.sin(Math.PI * f * 0.9) + normal(0.012)});
				w[i + 1] = f;
			}
			double[] rad = new double[n + 1];
			for (int k = 0; k <= n; k++) {
				rad[k] = 0.011 * (1 - 0.45 * k / n);
			}
			tube(pts, rad, 3, false, index, w);
			double[] s = lengths(pts);
			int k = 1 + rnd.nextInt(2);
			while (k < n - 1) {
				double[] side = unit(new double[] {normal(1), 0, normal(1)});
				double[] spurTop = add(pts.get(k), mul(side, 0.025));
				List<double[]> spur = List.of(pts.get(k), add(spurTop, new double[] {0, 0.005, 0}));
				tube(spur, new double[] {0.006, 0.005}, 3, false, index, new double[] {w[k], w[k]});
				double size = uniform(0.06, 0.11);
				crystal(spurTop, add(new double[] {0, -1, 0}, mul(side, 0.05)), size, size * 0.28, s0 + s[k], index, w[k]);
				k += 1 + rnd.nextInt(2);
			}
			double size = uniform(0.12, 0.2);
			crystal(pts.get(n), new double[] {0, -1, 0}, size, size * 0.26, s0 + s[n], index, 1.0);
		}

		// ---- the ground

		void ground() {
			for (int k = 0; k < 3; k++) {
				double a = uniform(0, 2 * Math.PI), r = uniform(1.6, 2.8);
				rock(new double[] {CX + r * Math.cos(a), GRAVEL_Y - 0.05, CZ + r * Math.sin(a)}, uniform(0.22, 0.36));
			}
			for (int k = 0; k < 8; k++) {
				double a = uniform(0, 2 * Math.PI), r = uniform(0.9, 3.0);
				moss(new double[] {CX + r * Math.cos(a), GRAVEL_Y, CZ + r * Math.sin(a)});
			}
		}

		/** A dark basalt rock sunk in the gravel: a squashed, lumpy dome. */
		void rock(double[] c, double size) {
			int nLat = 7, nLon = 14;
			double[][][] p = new double[nLat + 1][nLon][];
			double seedPhase = uniform(0, 10);
			for (int i = 0; i <= nLat; i++) {
				double th = Math.PI / 2 * i / nLat;
				for (int j = 0; j < nLon; j++) {
					double ph = 2 * Math.PI * j / nLon;
					double r = size * (1 + 0.10 * Math.sin(3 * ph + seedPhase) + 0.06 * Math.cos(5 * ph + 2 * th + seedPhase) + uniform(-0.03, 0.03));
					p[i][j] = new double[] {c[0] + r * Math.sin(th) * Math.cos(ph) * 1.25, c[1] + r * Math.cos(th) * 0.62, c[2] + r * Math.sin(th) * Math.sin(ph)};
				}
			}
			double topY = c[1];
			for (int j = 0; j < nLon; j++) {
				topY = Math.max(topY, p[1][j][1]);
			}
			double[] top = {c[0], topY + size * 0.015, c[2]};
			for (int i = 0; i < nLat; i++) {
				for (int j = 0; j < nLon; j++) {
					int j1 = (j + 1) % nLon;
					double[][] q = i == 0 ? new double[][] {p[1][j], p[1][j1], top, top} : new double[][] {p[i][j], p[i][j1], p[i + 1][j1], p[i + 1][j]};
					double[] mid = mul(add(add(q[0], q[1]), q[2]), 1 / 3.0);
					double[] n = unit(sub(mid, c));
					double fu = 0.07 * j % 0.5;
					barkQuad(q, new double[][] {n, n, n, n}, new double[][] {{0.2 + fu, 0.1 * i}, {0.3 + fu, 0.1 * i}, {0.3 + fu, 0.1 * i + 0.1}, {0.2 + fu, 0.1 * i + 0.1}},
							"basalt_rock", -1, null);
				}
			}
		}

		/** A clump of glowing bead moss: fine dark stems, each holding up a small round glowing bead. */
		void moss(double[] c) {
			for (int k = 0; k < 26; k++) {
				double a = uniform(0, 2 * Math.PI), r = uniform(0, 0.22);
				double[] base = {c[0] + r * Math.cos(a), GRAVEL_Y, c[2] + r * Math.sin(a)};
				double h = uniform(0.07, 0.2), lean = uniform(0, 0.04);
				double[] tip = {base[0] + Math.cos(a) * lean, GRAVEL_Y + h, base[2] + Math.sin(a) * lean};
				double[] u = {-Math.sin(a) * 0.004, 0, Math.cos(a) * 0.004};
				double[] n = unit(cross(UP, u));
				double[][] stem = {sub(tip, u), add(tip, u), add(base, u), sub(base, u)};
				barkQuad(stem, new double[][] {n, n, n, n}, new double[][] {{0.4, 0.4}, {0.6, 0.4}, {0.6, 0.6}, {0.4, 0.6}}, "stem", -1, null);
				double rb = uniform(0.012, 0.022);
				for (int turn = 0; turn < 2; turn++) {
					double[] uu = turn == 0 ? new double[] {1, 0, 0} : new double[] {0, 0, 1};
					double[][] q = {add(sub(tip, mul(uu, rb)), new double[] {0, rb, 0}), add(add(tip, mul(uu, rb)), new double[] {0, rb, 0}),
							sub(add(tip, mul(uu, rb)), new double[] {0, rb, 0}), sub(sub(tip, mul(uu, rb)), new double[] {0, rb, 0})};
					double[] nrm = cross(UP, uu);
					glowQuad(q, nrm, "pod", FULL, -1, 0.8, -1, 0);
				}
			}
		}
	}
}
