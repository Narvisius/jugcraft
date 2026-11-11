package io.github.jimbozoomer.jugcraft.client;

import java.util.Arrays;
import java.util.List;

/**
 * The plain geometry behind {@link HoloMapRenderer} (no game classes, so it can be checked on its own): the Tri-Pillar's
 * tower model meshed from its 3-block cells, and a buffer of coloured quads.
 */
public final class HoloGeometry {
	private HoloGeometry() { }

	static final int TOWER_CELL = 3;
	/** The tier lines (tools/tripillar_geometry.py TB): the model's cells across them are dark on their sides. */
	private static final int[] TIER_LINES = {20, 36, 55, 76, 100, 130, 162, 198, 238};
	/**
	 * The tower model's colours, lit from within rather than by the world (it is drawn with every normal up, so the game's
	 * shading leaves it alone): tops brightest, the two pairs of sides a little apart so its shape reads as it turns,
	 * undersides deeper; the tier lines dark bands round the sides; the tier being built pale and bright.
	 */
	private static final int MODEL_TOP = 0xFFFFB653, MODEL_SIDE_X = 0xFFF29A45, MODEL_SIDE_Z = 0xFFE0883A, MODEL_UNDER = 0xFFA45C25,
			MODEL_BAND = 0xFF6E3412, MODEL_FRESH_TOP = 0xFFFFE9B4, MODEL_FRESH = 0xFFFFD584;
	/** The control building (tools/tripillar_geometry.py HEX_R), and its entrance canopy to the south. */
	static final double HEX_R = 52.5;

	/** Which foundation and tier blocks the model shows: the building, its canopy and what stands outside it (not the yard's ground). */
	static boolean modelled(int x, int y, int z) {
		if (y < 0) return false;
		double ax = Math.abs(x), az = Math.abs(z), h = HEX_R * Math.sqrt(3) / 2; // as tripillar_geometry.inside_hex
		boolean hex = az <= h && ax * Math.sqrt(3) / 2 + az / 2 <= h;
		boolean canopy = x >= -9 && x <= 8 && z >= -54 && z <= -45;
		return hex || canopy || y >= 1;
	}

	/**
	 * The tower model as quads (cell units: cell (cx, cy, cz) spans cx..cx+1), its outer faces merged into rectangles of
	 * one colour (per quad: 4 corners x, y, z, then the colour's bits). The cells are {cx, cy, cz, new?} as
	 * HoloMapRenderer.towerCells makes them (of the {@link #modelled} blocks only).
	 */
	static float[] mesh(List<int[]> cells) {
		if (cells.isEmpty()) return new float[0];
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
		for (int[] c : cells) {
			minX = Math.min(minX, c[0]); maxX = Math.max(maxX, c[0]);
			minY = Math.min(minY, c[1]); maxY = Math.max(maxY, c[1]);
			minZ = Math.min(minZ, c[2]); maxZ = Math.max(maxZ, c[2]);
		}
		int[] dim = {maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1};
		byte[] grid = new byte[dim[0] * dim[1] * dim[2]];
		for (int[] c : cells) {
			int cy = c[1];
			boolean line = false;
			for (int l : TIER_LINES) {
				if (l >= cy * TOWER_CELL && l < cy * TOWER_CELL + TOWER_CELL) line = true;
			}
			grid[(c[0] - minX) + dim[0] * ((cy - minY) + dim[1] * (c[2] - minZ))] = (byte) ((c[3] == 1 ? 2 : 1) | (line ? 4 : 0));
		}
		Quads out = new Quads();
		int[] pos = new int[3];
		for (int axis = 0; axis < 3; axis++) {
			int u = (axis + 1) % 3, v = (axis + 2) % 3;
			int du = dim[u], dv = dim[v];
			int[] mask = new int[du * dv];
			for (int dir = -1; dir <= 1; dir += 2) {
				for (int i = 0; i < dim[axis]; i++) {
					for (int b = 0; b < dv; b++) {
						for (int a = 0; a < du; a++) {
							pos[axis] = i; pos[u] = a; pos[v] = b;
							int c = cell(grid, dim, pos[0], pos[1], pos[2]);
							int m = 0;
							if (c != 0) {
								pos[axis] = i + dir;
								if (cell(grid, dim, pos[0], pos[1], pos[2]) == 0) {
									m = (c & 3) == 2 ? 3 : axis != 1 && (c & 4) != 0 ? 2 : 1;
								}
							}
							mask[a + b * du] = m;
						}
					}
					// greedy: grow each face run along u, then along v while the whole run matches
					for (int b = 0; b < dv; b++) {
						for (int a = 0; a < du; ) {
							int m = mask[a + b * du];
							if (m == 0) {
								a++;
								continue;
							}
							int w = 1;
							while (a + w < du && mask[a + w + b * du] == m) w++;
							int h = 1;
							grow:
							while (b + h < dv) {
								for (int k = 0; k < w; k++) {
									if (mask[a + k + (b + h) * du] != m) break grow;
								}
								h++;
							}
							for (int hb = 0; hb < h; hb++) {
								Arrays.fill(mask, a + (b + hb) * du, a + w + (b + hb) * du, 0);
							}
							float plane = i + (dir > 0 ? 1 : 0);
							float[] base = {minX, minY, minZ};
							float[][] q = new float[4][3];
							int[][] uv = {{a, b}, {a + w, b}, {a + w, b + h}, {a, b + h}};
							for (int k = 0; k < 4; k++) {
								q[k][axis] = plane + base[axis];
								q[k][u] = uv[k][0] + base[u];
								q[k][v] = uv[k][1] + base[v];
							}
							float[] n = new float[3];
							n[axis] = dir;
							int color = m == 3 ? (axis == 1 && dir > 0 ? MODEL_FRESH_TOP : MODEL_FRESH)
									: m == 2 ? MODEL_BAND
									: axis == 1 ? (dir > 0 ? MODEL_TOP : MODEL_UNDER)
									: axis == 0 ? MODEL_SIDE_X : MODEL_SIDE_Z;
							out.facing(q[0][0], q[0][1], q[0][2], q[1][0], q[1][1], q[1][2], q[2][0], q[2][1], q[2][2], q[3][0], q[3][1], q[3][2],
									n[0], n[1], n[2], color, color, color, color);
							a += w;
						}
					}
				}
			}
		}
		return out.toArray();
	}

	static int cell(byte[] grid, int[] dim, int x, int y, int z) {
		if (x < 0 || y < 0 || z < 0 || x >= dim[0] || y >= dim[1] || z >= dim[2]) return 0;
		return grid[x + dim[0] * (y + dim[1] * z)];
	}

	/**
	 * Quads with a colour per corner, kept as floats: per quad 4 x (x, y, z, colour bits). The corners go round
	 * anticlockwise seen from the side the quad faces ({@link #facing} puts them that way).
	 */
	static final class Quads {
		static final int STRIDE = 16;
		private float[] data;
		private int size;
		/** A render state's: its buffers are {@link FrameArrays}, good until the next frame. */
		private final boolean frame;

		/** Quads to keep (a model worked out once). */
		Quads() {
			this(false);
		}

		private Quads(boolean frame) {
			this.frame = frame;
			this.data = frame ? FrameArrays.floats(256) : new float[256];
		}

		/** Quads for one frame's render state, their buffers used again next frame instead of thrown away. */
		static Quads perFrame() {
			return new Quads(true);
		}

		void clear() {
			size = 0;
		}

		int size() {
			return size / STRIDE;
		}

		/** The quads as an array of exactly their floats (a per-frame one's lasts until the next frame). */
		float[] toArray() {
			return frame ? FrameArrays.copy(data, size) : Arrays.copyOf(data, size);
		}

		private void put(float x, float y, float z, int color) {
			if (size + 4 > data.length) data = frame ? grow() : Arrays.copyOf(data, data.length * 2);
			data[size++] = x;
			data[size++] = y;
			data[size++] = z;
			data[size++] = Float.intBitsToFloat(color);
		}

		/** A per-frame buffer twice the size, with what is in it so far. */
		private float[] grow() {
			float[] bigger = FrameArrays.floats(data.length * 2);
			System.arraycopy(data, 0, bigger, 0, size);
			return bigger;
		}

		/** A quad facing (nx, ny, nz): the corners are taken in the given order or reversed, whichever faces that way. */
		void facing(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz,
				float nx, float ny, float nz, int ca, int cb, int cc, int cd) {
			// (b - a) x (c - b), against the wanted normal
			float ux = bx - ax, uy = by - ay, uz = bz - az, vx = cx - bx, vy = cy - by, vz = cz - bz;
			float wx = uy * vz - uz * vy, wy = uz * vx - ux * vz, wz = ux * vy - uy * vx;
			if (wx * nx + wy * ny + wz * nz >= 0) {
				put(ax, ay, az, ca); put(bx, by, bz, cb); put(cx, cy, cz, cc); put(dx, dy, dz, cd);
			} else {
				put(ax, ay, az, ca); put(dx, dy, dz, cd); put(cx, cy, cz, cc); put(bx, by, bz, cb);
			}
		}

		/** A flat strip from (x0, y0, z0) to (x1, y1, z1), {@code w} wide, facing up. */
		void ribbon(double x0, double y0, double z0, double x1, double y1, double z1, double w, int color) {
			double dx = x1 - x0, dz = z1 - z0, l = Math.hypot(dx, dz);
			if (l < 1e-6) return;
			float px = (float) (-dz / l * w / 2), pz = (float) (dx / l * w / 2);
			facing((float) x0 - px, (float) y0, (float) z0 - pz, (float) x1 - px, (float) y1, (float) z1 - pz, (float) x1 + px, (float) y1, (float) z1 + pz,
					(float) x0 + px, (float) y0, (float) z0 + pz, 0, 1, 0, color, color, color, color);
		}

		/** A flat ring of radius {@code r}, {@code w} wide, facing up. */
		void ring(double x, double y, double z, double r, double w, int segments, int color) {
			for (int k = 0; k < segments; k++) {
				double a0 = 2 * Math.PI * k / segments, a1 = 2 * Math.PI * (k + 1) / segments;
				double r0 = r - w / 2, r1 = r + w / 2;
				facing((float) (x + Math.sin(a0) * r0), (float) y, (float) (z - Math.cos(a0) * r0), (float) (x + Math.sin(a1) * r0), (float) y,
						(float) (z - Math.cos(a1) * r0), (float) (x + Math.sin(a1) * r1), (float) y, (float) (z - Math.cos(a1) * r1),
						(float) (x + Math.sin(a0) * r1), (float) y, (float) (z - Math.cos(a0) * r1), 0, 1, 0, color, color, color, color);
			}
		}

		void disc(double x, double y, double z, double r, int segments, int color) {
			disc(x, y, z, r, segments, color, false);
		}

		/** A flat disc facing up (or down), as fan quads from its middle; its rim is half as strong. */
		void disc(double x, double y, double z, double r, int segments, int color, boolean down) {
			int rim = (color >>> 25) << 24 | (color & 0x00FFFFFF);
			for (int k = 0; k < segments; k++) {
				double a0 = 2 * Math.PI * k / segments, a1 = 2 * Math.PI * (k + 1) / segments, am = (a0 + a1) / 2;
				facing((float) x, (float) y, (float) z, (float) (x + Math.sin(a0) * r), (float) y, (float) (z - Math.cos(a0) * r),
						(float) (x + Math.sin(am) * r), (float) y, (float) (z - Math.cos(am) * r), (float) (x + Math.sin(a1) * r), (float) y,
						(float) (z - Math.cos(a1) * r), 0, down ? -1 : 1, 0, color, rim, rim, rim);
			}
		}

		/** An upright open cylinder of light from y0 to y1, seen from outside and in; it fades towards the top. */
		void column(double x, double y0, double z, double r, double y1, int segments, int color) {
			int top = color & 0x00FFFFFF;
			for (int k = 0; k < segments; k++) {
				double a0 = 2 * Math.PI * k / segments, a1 = 2 * Math.PI * (k + 1) / segments;
				float x0 = (float) (x + Math.sin(a0) * r), z0 = (float) (z - Math.cos(a0) * r), x1 = (float) (x + Math.sin(a1) * r), z1 = (float) (z - Math.cos(a1) * r);
				float nx = (float) Math.sin((a0 + a1) / 2), nz = (float) -Math.cos((a0 + a1) / 2);
				facing(x0, (float) y0, z0, x1, (float) y0, z1, x1, (float) y1, z1, x0, (float) y1, z0, nx, 0, nz, color, color, top, top);
				facing(x0, (float) y0, z0, x1, (float) y0, z1, x1, (float) y1, z1, x0, (float) y1, z0, -nx, 0, -nz, color, color, top, top);
			}
		}

		/** A thin ray of light from one point to another: two crossed strips, each seen from both sides. */
		void ray(double x0, double y0, double z0, double x1, double y1, double z1, double w, int color) {
			double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0, l = Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (l < 1e-6) return;
			// two directions across the ray: one level, one at right angles to it and the ray
			double hx = -dz, hz = dx, hl = Math.hypot(hx, hz);
			if (hl < 1e-6) {
				hx = 1;
				hz = 0;
				hl = 1;
			}
			hx /= hl;
			hz /= hl;
			double vx = dy * hz, vy = dz * hx - dx * hz, vz = -dy * hx, vl = Math.sqrt(vx * vx + vy * vy + vz * vz);
			vx /= vl;
			vy /= vl;
			vz /= vl;
			int end = color & 0x00FFFFFF;
			for (double[] across : new double[][] {{hx, 0, hz}, {vx, vy, vz}}) {
				float ax = (float) (across[0] * w / 2), ay = (float) (across[1] * w / 2), az = (float) (across[2] * w / 2);
				// its normal: the ray crossed with the across direction
				float nx = (float) (dy * across[2] - dz * across[1]), ny = (float) (dz * across[0] - dx * across[2]), nz = (float) (dx * across[1] - dy * across[0]);
				for (int side = -1; side <= 1; side += 2) {
					facing((float) x0 - ax, (float) y0 - ay, (float) z0 - az, (float) x0 + ax, (float) y0 + ay, (float) z0 + az, (float) x1 + ax, (float) y1 + ay,
							(float) z1 + az, (float) x1 - ax, (float) y1 - ay, (float) z1 - az, nx * side, ny * side, nz * side, color, color, end, end);
				}
			}
		}
	}
}
