package io.github.jimbozoomer.jugcraft.client;

import java.util.Arrays;
import java.util.Random;

/**
 * The Holo Projector's hologram as quads (client/HoloProjectorRenderer draws them, in one sorted see-through batch with
 * {@link HoloHeadMesh}'s texture): the head turned to its yaw and pitch, its eyes on what it watches, the hair's cards,
 * the ring of letters and the light from the lens; as the approved sheet draws them (tools/mockups/hh_scene.py). No
 * Minecraft in here, so the offline harness can draw it too.
 */
final class HoloProjectorGeometry {
	private HoloProjectorGeometry() {
	}

	/** Blocks a head unit; the head's turning point over the floor (blocks) and in the head (head units). */
	static final float K = 3.4f, HEAD_Y = 5.2f, PIVOT_HY = 0.45f;
	/** How far the eyes turn from straight ahead. */
	static final float GAZE_MAX = (float) Math.toRadians(40);
	/** The ring: its radius, letter height, height over the floor, lean back, seconds a turn. */
	static final float RING_R = 3.4f, RING_H = 0.75f, RING_Y = 4.25f, RING_LEAN = (float) Math.toRadians(24), TURN_S = 24;
	/** The light: the lens's top, the cone's foot radius and spread, how far up it reaches (of the way to the neck). */
	static final float LENS_Y = 0.27f, CONE_R = 0.85f, CONE_SPREAD = 0.62f, REACH = 0.62f, EMIT_R = 1.12f;
	private static final float NECK = HEAD_Y - 0.62f * K;
	/** The light fixed to the head (head space). */
	private static final float LX = 0.35f / 0.983f, LY = 0.75f / 0.983f, LZ = 0.55f / 0.983f;
	/** The colours (r, g, b) by label: skin, lids, brows, lips, teeth, choker, hair, mouth. */
	private static final int[][] COLOUR = {{255, 210, 234}, {60, 26, 130}, {70, 30, 160}, {255, 140, 190}, {240, 240, 255}, {120, 70, 200},
			{112, 56, 228}, {70, 20, 90}};
	/** How solid each label is (times the base). */
	private static final float[] SOLID = {1.6f, 2.2f, 2.2f, 1.6f, 1.5f, 1.6f, 1.7f, 2.0f};
	private static final int[] WORDS = {196, 120, 255};
	/** The cards' opacity, against the head's (there is no depth pre-pass: layers of locks blend). */
	private static final float CARD_A = 0.7f;
	private static final int MOTES = 46;

	/** Quads: 4 corners each of x, y, z, u, v (floats) and argb (ints). */
	static final class Builder {
		static final int FLOATS = 5;
		float[] data;
		int[] colour;
		int size;
		int corners;

		Builder(float[] reuse, int[] reuseColour) {
			data = reuse != null && reuse.length > 0 ? reuse : new float[FLOATS * 4 * 4096];
			colour = reuseColour != null && reuseColour.length > 0 ? reuseColour : new int[4 * 4096];
		}

		void corner(float x, float y, float z, float u, float v, int argb) {
			if (size + FLOATS > data.length) {
				data = Arrays.copyOf(data, data.length * 2);
			}
			if (corners + 1 > colour.length) {
				colour = Arrays.copyOf(colour, colour.length * 2);
			}
			data[size++] = x;
			data[size++] = y;
			data[size++] = z;
			data[size++] = u;
			data[size++] = v;
			colour[corners++] = argb;
		}

		int quads() {
			return corners / 4;
		}
	}

	/** Per vertex of the head, worked out once a build: where it is (block frame) and its colour; when, by build. */
	private static float[] vpos = new float[0];
	private static int[] vcol = new int[0];
	private static int[] vstamp = new int[0];
	private static int stamp;

	private static int argb(float alpha, float r, float g, float b) {
		int a = Math.round(Math.max(0, Math.min(1, alpha)) * 255);
		return a << 24 | clamp(r) << 16 | clamp(g) << 8 | clamp(b);
	}

	private static int clamp(float c) {
		return Math.max(0, Math.min(255, Math.round(c)));
	}

	private static float scan(float worldY, double t) {
		return (float) (0.92 + 0.08 * Math.sin(worldY * 22 + t * 4.0));
	}

	private static float band(float worldY, double t) {
		double m = (worldY - t * 0.9) % 7.0;
		if (m < 0) {
			m += 7.0;
		}
		double d = m - 3.5;
		return (float) (Math.exp(-d * d / 0.02) * 0.5);
	}

	/**
	 * Everything see-through over the projector, in block-local coordinates (the master cell's corner at the origin, the
	 * floor at y 0): the head turned to (yaw, pitch), its eyes on (tx, ty, tz), seen from (cx, cy, cz); at time {@code t}
	 * seconds; {@code baseY} the block's height in the world (the scan lines run in world height).
	 */
	static void build(HoloHeadMesh mesh, Builder b, float yaw, float pitch, float tx, float ty, float tz, float cx, float cy, float cz,
			double t, int baseY) {
		float cyw = (float) Math.cos(yaw), syw = (float) Math.sin(yaw), cp = (float) Math.cos(pitch), sp = (float) Math.sin(pitch);
		// R = Ry(yaw) Rx(pitch), Rx tipping +z towards -y (looking down for pitch > 0)
		float[] r = {cyw, syw * sp, syw * cp, 0, cp, -sp, -syw, cyw * sp, cyw * cp};
		float px = 0.5f, py = HEAD_Y, pz = 0.5f;
		// the camera in head space
		float qx = (cx - px) / K, qy = (cy - py) / K, qz = (cz - pz) / K;
		float hx = r[0] * qx + r[3] * qy + r[6] * qz, hy = r[1] * qx + r[4] * qy + r[7] * qz + PIVOT_HY, hz = r[2] * qx + r[5] * qy + r[8] * qz;
		head(mesh, b, r, px, py, pz, cx, cy, cz, hx, hy, hz, t, baseY);
		eyes(mesh, b, r, px, py, pz, tx, ty, tz, cx, cy, cz, hx, hy, hz, t, baseY);
		cards(mesh, b, r, px, py, pz, cx, cy, cz, hx, hy, hz, t, baseY);
		ring(mesh, b, cx, cy, cz, t, baseY);
		light(b, cx, cy, cz, t);
	}

	private static final float[] WHITE_UV = {(HoloHeadMesh.WHITE[0] + 8f) / HoloHeadMesh.ATLAS_W, (HoloHeadMesh.WHITE[1] + 8f) / HoloHeadMesh.ATLAS_H};

	/** A head-space point into the block's frame. */
	private static void place(float[] r, float px, float py, float pz, float x, float y, float z, float[] out) {
		y -= PIVOT_HY;
		out[0] = px + (r[0] * x + r[1] * y + r[2] * z) * K;
		out[1] = py + (r[3] * x + r[4] * y + r[5] * z) * K;
		out[2] = pz + (r[6] * x + r[7] * y + r[8] * z) * K;
	}

	/** 1 - |n . v| for the unit normal n (block frame) at point p seen from the camera. */
	private static float rim(float nx, float ny, float nz, float x, float y, float z, float cx, float cy, float cz) {
		float vx = cx - x, vy = cy - y, vz = cz - z;
		float l = (float) Math.sqrt(vx * vx + vy * vy + vz * vz);
		if (l < 1e-6f) {
			return 0;
		}
		return 1 - Math.abs((nx * vx + ny * vy + nz * vz) / l);
	}

	private static void head(HoloHeadMesh m, Builder b, float[] r, float px, float py, float pz, float cx, float cy, float cz, float hx,
			float hy, float hz, double t, int baseY) {
		int nv = m.v.length / 3;
		if (vstamp.length != nv) {
			vpos = new float[nv * 3];
			vcol = new int[nv * 8];
			vstamp = new int[nv];
			stamp = 0;
		}
		stamp++;
		float[] p = new float[3];
		for (int tri = 0; tri < m.triCount; tri++) {
			if (!m.seen(tri, hx, hy, hz)) {
				continue;
			}
			int i0 = m.tri[tri * 3], i1 = m.tri[tri * 3 + 1], i2 = m.tri[tri * 3 + 2];
			int lab = Math.max(m.label[i0], Math.max(m.label[i1], m.label[i2]));
			for (int k = 0; k < 4; k++) {
				int i = m.tri[tri * 3 + Math.min(k, 2)];
				if (vstamp[i] != stamp) {
					vstamp[i] = stamp;
					place(r, px, py, pz, m.v[i * 3], m.v[i * 3 + 1], m.v[i * 3 + 2], p);
					vpos[i * 3] = p[0];
					vpos[i * 3 + 1] = p[1];
					vpos[i * 3 + 2] = p[2];
					// its colour under each label it can be drawn with (a triangle takes its corners' highest label)
					for (int l = 0; l < 8; l++) {
						vcol[i * 8 + l] = 0;
					}
				}
				if (vcol[i * 8 + lab] == 0) {
					vcol[i * 8 + lab] = vertexColour(m, i, lab, r, cx, cy, cz, t, baseY);
				}
				b.corner(vpos[i * 3], vpos[i * 3 + 1], vpos[i * 3 + 2], WHITE_UV[0], WHITE_UV[1], vcol[i * 8 + lab]);
			}
		}
	}

	/** Vertex i's colour drawn as label {@code lab}: see-through, brighter and more solid at the edges, lit from the head's
	 * light, the scan lines and the band, its fade; never 0 (0 means not worked out yet). */
	private static int vertexColour(HoloHeadMesh m, int i, int lab, float[] r, float cx, float cy, float cz, double t, int baseY) {
		float x = vpos[i * 3], y = vpos[i * 3 + 1], z = vpos[i * 3 + 2];
		float nx0 = m.n[i * 3], ny0 = m.n[i * 3 + 1], nz0 = m.n[i * 3 + 2];
		float nx = r[0] * nx0 + r[1] * ny0 + r[2] * nz0, ny = r[3] * nx0 + r[4] * ny0 + r[5] * nz0, nz = r[6] * nx0 + r[7] * ny0 + r[8] * nz0;
		float rr = rim(nx, ny, nz, x, y, z, cx, cy, cz);
		float alpha = (0.42f + 0.5f * rr * rr) * SOLID[lab] * m.fade[i];
		float lam = Math.max(0, nx0 * LX + ny0 * LY + nz0 * LZ);
		float wy = y + baseY;
		float bright = ((0.72f + 0.55f * rr) * (0.5f + 0.7f * lam) * scan(wy, t) + band(wy, t)) * m.shade[i];
		int[] c = COLOUR[lab];
		float cr = c[0], cg = c[1], cb = c[2];
		if (lab == HoloHeadMesh.SKIN && m.lip[i] > 0.02f) {
			float w = m.lip[i];
			int[] lips = COLOUR[HoloHeadMesh.LIPS];
			cr = cr * (1 - w) + lips[0] * w;
			cg = cg * (1 - w) + lips[1] * w;
			cb = cb * (1 - w) + lips[2] * w;
		}
		int out = argb(alpha, cr * bright, cg * bright, cb * bright);
		return out == 0 ? 1 : out;
	}

	private static void eyes(HoloHeadMesh m, Builder b, float[] r, float px, float py, float pz, float tx, float ty, float tz, float cx,
			float cy, float cz, float hx, float hy, float hz, double t, int baseY) {
		float[] p = new float[3];
		float[] e = new float[3];
		int[] tile = HoloHeadMesh.IRIS;
		float half = (tile[2] - 1) / 2f;
		float tileU = (tile[0] + tile[2] / 2f) / HoloHeadMesh.ATLAS_W, tileV = (tile[1] + tile[3] / 2f) / HoloHeadMesh.ATLAS_H;
		float tileMax = (float) Math.toRadians(HoloHeadMesh.IRIS_TILE_DEG);
		float cosMax = (float) Math.cos(GAZE_MAX);
		for (int eye = 0; eye < 2; eye++) {
			float[] c = m.eyes[eye];
			place(r, px, py, pz, c[0], c[1], c[2], e);
			// its gaze, in head space, turned no further than GAZE_MAX from straight ahead
			float gx = tx - e[0], gy = ty - e[1], gz = tz - e[2];
			float hgx = r[0] * gx + r[3] * gy + r[6] * gz, hgy = r[1] * gx + r[4] * gy + r[7] * gz, hgz = r[2] * gx + r[5] * gy + r[8] * gz;
			float gl = (float) Math.sqrt(hgx * hgx + hgy * hgy + hgz * hgz);
			if (gl < 1e-6f) {
				hgx = 0;
				hgy = 0;
				hgz = 1;
			} else {
				hgx /= gl;
				hgy /= gl;
				hgz /= gl;
			}
			if (hgz < cosMax) {
				// pull it back onto the cone round +z
				float s = (float) Math.sqrt(Math.max(1e-9, hgx * hgx + hgy * hgy));
				float sinMax = (float) Math.sin(GAZE_MAX);
				hgx = hgx / s * sinMax;
				hgy = hgy / s * sinMax;
				hgz = cosMax;
			}
			// two axes across the gaze for the iris tile
			float ax = hgz, ay = 0, az = -hgx;
			float al = (float) Math.sqrt(ax * ax + az * az);
			if (al < 1e-5f) {
				ax = 1;
				az = 0;
				al = 1;
			}
			ax /= al;
			az /= al;
			float bx = hgy * az - hgz * ay, by = hgz * ax - hgx * az, bz = hgx * ay - hgy * ax;
			int base = m.eyeBase(eye);
			for (int tri = 0; tri < m.eyeTriCount; tri++) {
				if (!m.seen(base + tri, hx, hy, hz)) {
					continue;
				}
				for (int k = 0; k < 4; k++) {
					int i = m.eyeTri[tri * 3 + Math.min(k, 2)];
					float ux = m.eyeV[i * 3], uy = m.eyeV[i * 3 + 1], uz = m.eyeV[i * 3 + 2];
					place(r, px, py, pz, c[0] + ux * m.eyeR, c[1] + uy * m.eyeR, c[2] + uz * m.eyeR, p);
					float nx = r[0] * ux + r[1] * uy + r[2] * uz, ny = r[3] * ux + r[4] * uy + r[5] * uz, nz = r[6] * ux + r[7] * uy + r[8] * uz;
					float rr = rim(nx, ny, nz, p[0], p[1], p[2], cx, cy, cz);
					float alpha = 0.55f + 0.4f * rr * rr;
					float wy = p[1] + baseY;
					float bright = (0.72f + 0.55f * rr) * 1.35f * scan(wy, t) + band(wy, t);
					// where it falls on the iris tile: its angle from the gaze, out along its way across it
					float cosT = Math.max(-1, Math.min(1, ux * hgx + uy * hgy + uz * hgz));
					float theta = Math.min((float) Math.acos(cosT), tileMax);
					float qx = ux - cosT * hgx, qy = uy - cosT * hgy, qz = uz - cosT * hgz;
					float ql = (float) Math.sqrt(qx * qx + qy * qy + qz * qz);
					float a = 0, bb = 0;
					if (ql > 1e-6f) {
						a = (qx * ax + qy * ay + qz * az) / ql;
						bb = (qx * bx + qy * by + qz * bz) / ql;
					}
					float rad = theta / tileMax * half;
					float u = tileU + a * rad / HoloHeadMesh.ATLAS_W, v = tileV + bb * rad / HoloHeadMesh.ATLAS_H;
					b.corner(p[0], p[1], p[2], u, v, argb(alpha, 255 * bright, 255 * bright, 255 * bright));
				}
			}
		}
	}

	private static void cards(HoloHeadMesh m, Builder b, float[] r, float px, float py, float pz, float cx, float cy, float cz, float hx,
			float hy, float hz, double t, int baseY) {
		float[] p = new float[3];
		int prim = m.cardBase();
		for (int card = 0; card < m.cardGroup.length; card++) {
			int[] tile = HoloHeadMesh.CARD_TILES[m.cardVar[card] % HoloHeadMesh.CARD_TILES.length];
			int[] c = COLOUR[m.cardGroup[card] == HoloHeadMesh.BROW_CARDS ? HoloHeadMesh.BROWS : HoloHeadMesh.HAIR];
			float u0 = (tile[0] + 0.5f) / HoloHeadMesh.ATLAS_W, u1 = (tile[0] + tile[2] - 0.5f) / HoloHeadMesh.ATLAS_W;
			int len = m.cardLen[card];
			for (int s = 0; s < len - 1; s++, prim++) {
				if (!m.seen(prim, hx, hy, hz)) {
					continue;
				}
				int a = (m.cardStart[card] + s) * 3, bb = a + 3;
				float nx0 = (m.cardN[a] + m.cardN[bb]) / 2, ny0 = (m.cardN[a + 1] + m.cardN[bb + 1]) / 2, nz0 = (m.cardN[a + 2] + m.cardN[bb + 2]) / 2;
				float nl = (float) Math.sqrt(nx0 * nx0 + ny0 * ny0 + nz0 * nz0);
				if (nl > 1e-6f) {
					nx0 /= nl;
					ny0 /= nl;
					nz0 /= nl;
				}
				float nx = r[0] * nx0 + r[1] * ny0 + r[2] * nz0, ny = r[3] * nx0 + r[4] * ny0 + r[5] * nz0, nz = r[6] * nx0 + r[7] * ny0 + r[8] * nz0;
				float lam = Math.max(0, nx0 * LX + ny0 * LY + nz0 * LZ);
				float v0 = (tile[1] + 0.5f + (float) s / (len - 1) * (tile[3] - 1)) / HoloHeadMesh.ATLAS_H;
				float v1 = (tile[1] + 0.5f + (float) (s + 1) / (len - 1) * (tile[3] - 1)) / HoloHeadMesh.ATLAS_H;
				for (int k = 0; k < 4; k++) {
					float[] side = k == 0 || k == 3 ? m.cardL : m.cardR;
					int at = k < 2 ? a : bb;
					place(r, px, py, pz, side[at], side[at + 1], side[at + 2], p);
					float rr = rim(nx, ny, nz, p[0], p[1], p[2], cx, cy, cz);
					float alpha = CARD_A * (0.42f * SOLID[HoloHeadMesh.HAIR] + 0.5f * rr * rr);
					float bright = (0.72f + 0.55f * rr) * (0.55f + 0.75f * lam) * scan(p[1] + baseY, t) * HoloHeadMesh.CARD_BRIGHT;
					b.corner(p[0], p[1], p[2], k == 0 || k == 3 ? u0 : u1, k < 2 ? v0 : v1, argb(alpha, c[0] * bright, c[1] * bright, c[2] * bright));
				}
			}
		}
	}

	private static void ring(HoloHeadMesh m, Builder b, float cx, float cy, float cz, double t, int baseY) {
		float ox = 0.5f, oy = RING_Y, oz = 0.5f;
		double scaleArc = 2 * Math.PI * RING_R / (m.ringLength * RING_H);
		double turn = (t / TURN_S) * 2 * Math.PI;
		float cl = (float) Math.cos(RING_LEAN), sl = (float) Math.sin(RING_LEAN);
		float[][] w = new float[3][3];
		for (int item = 0; item < m.ringGlyph.length; item++) {
			int g = m.ringGlyph[item];
			float adv = m.glyphAdvance[g];
			double a = (m.ringX[item] + adv / 2) * RING_H * scaleArc / RING_R + turn;
			float sa = (float) Math.sin(a), ca = (float) Math.cos(a);
			// right, up (leaning back) and face (out and down) of the letter
			float rx = ca, rz = -sa;
			float ux = sa * sl, uy = cl, uz = ca * sl;
			float fx = sa * cl, fy = -sl, fz = ca * cl;
			float bx = ox + sa * RING_R, by = oy, bz = oz + ca * RING_R;
			boolean facing = fx * (cx - bx) + fy * (cy - by) + fz * (cz - bz) > 0;
			float glow = facing ? 1.25f : 0.7f, base = facing ? 0.55f : 0.22f, rimA = facing ? 0.35f : 0.15f;
			float[] gv = m.glyphV[g];
			int[] gt = m.glyphTri[g];
			for (int tri = 0; tri < gt.length / 3; tri++) {
				for (int k = 0; k < 3; k++) {
					int i = gt[tri * 3 + k] * 3;
					float lx = (gv[i] - adv / 2) * RING_H, ly = gv[i + 1] * RING_H, lz = gv[i + 2] * RING_H;
					w[k][0] = bx + rx * lx + ux * ly + fx * lz;
					w[k][1] = by + uy * ly + fy * lz;
					w[k][2] = bz + rz * lx + uz * ly + fz * lz;
				}
				// its outward normal; only the triangles facing the camera are drawn (the letters are solids)
				float e1x = w[1][0] - w[0][0], e1y = w[1][1] - w[0][1], e1z = w[1][2] - w[0][2];
				float e2x = w[2][0] - w[0][0], e2y = w[2][1] - w[0][1], e2z = w[2][2] - w[0][2];
				float nx = e1y * e2z - e1z * e2y, ny = e1z * e2x - e1x * e2z, nz = e1x * e2y - e1y * e2x;
				float nl = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
				if (nl < 1e-9f) {
					continue;
				}
				nx /= nl;
				ny /= nl;
				nz /= nl;
				float mx = (w[0][0] + w[1][0] + w[2][0]) / 3, my = (w[0][1] + w[1][1] + w[2][1]) / 3, mz = (w[0][2] + w[1][2] + w[2][2]) / 3;
				if (nx * (cx - mx) + ny * (cy - my) + nz * (cz - mz) <= 0) {
					continue;
				}
				for (int k = 0; k < 4; k++) {
					float[] q = w[Math.min(k, 2)];
					float rr = rim(nx, ny, nz, q[0], q[1], q[2], cx, cy, cz);
					float alpha = base + rimA * rr * rr;
					float wy = q[1] + baseY;
					float bright = (0.72f + 0.55f * rr) * glow * scan(wy, t) + band(wy, t);
					b.corner(q[0], q[1], q[2], WHITE_UV[0], WHITE_UV[1], argb(alpha, WORDS[0] * bright, WORDS[1] * bright, WORDS[2] * bright));
				}
			}
		}
	}

	/** Billboard's sideways half-vector: across the line from a to b, square to the view. */
	private static float[] across(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float half) {
		float lx = bx - ax, ly = by - ay, lz = bz - az;
		float vx = cx - (ax + bx) / 2, vy = cy - (ay + by) / 2, vz = cz - (az + bz) / 2;
		float sx = ly * vz - lz * vy, sy = lz * vx - lx * vz, sz = lx * vy - ly * vx;
		float sl = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
		if (sl < 1e-6f) {
			return new float[] {half, 0, 0};
		}
		return new float[] {sx / sl * half, sy / sl * half, sz / sl * half};
	}

	private static void light(Builder b, float cx, float cy, float cz, double t) {
		float x0 = 0.5f, z0 = 0.5f, y0 = LENS_Y;
		float reach = REACH * (NECK - y0);
		float yt = y0 + reach, rt = CONE_R + CONE_SPREAD * reach;
		int[] st = HoloHeadMesh.STREAK;
		float su0 = (st[0] + 0.5f) / HoloHeadMesh.ATLAS_W, su1 = (st[0] + st[2] - 0.5f) / HoloHeadMesh.ATLAS_W;
		float svTop = (st[1] + 0.5f) / HoloHeadMesh.ATLAS_H, svBottom = (st[1] + st[3] - 0.5f) / HoloHeadMesh.ATLAS_H;
		int n = 32;
		for (int k = 0; k < n; k++) {
			double a0 = 2 * Math.PI * k / n, a1 = 2 * Math.PI * (k + 1) / n;
			float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
			b.corner(x0 + CONE_R * c0, y0, z0 + CONE_R * s0, su0, svBottom, 0xFFFFFFFF);
			b.corner(x0 + CONE_R * c1, y0, z0 + CONE_R * s1, su1, svBottom, 0xFFFFFFFF);
			b.corner(x0 + rt * c1, yt, z0 + rt * s1, su1, svTop, 0xFFFFFFFF);
			b.corner(x0 + rt * c0, yt, z0 + rt * s0, su0, svTop, 0xFFFFFFFF);
		}
		// the rays: one off each emitter, leaning out, fading away
		for (int k = 0; k < 12; k++) {
			double a = k * Math.PI / 6;
			float ex = x0 + (float) Math.cos(a) * EMIT_R, ey = y0 + 0.04f, ez = z0 + (float) Math.sin(a) * EMIT_R;
			float dx = (float) Math.cos(a) * 0.42f, dy = 1, dz = (float) Math.sin(a) * 0.42f;
			float dl = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
			float len = reach * 0.95f;
			float fx = ex + dx / dl * len, fy = ey + dy / dl * len, fz = ez + dz / dl * len;
			float[] s = across(ex, ey, ez, fx, fy, fz, cx, cy, cz, 0.016f);
			int near = argb(0.32f, 255, 235, 255), far = argb(0, 255, 235, 255);
			b.corner(ex - s[0], ey - s[1], ez - s[2], WHITE_UV[0], WHITE_UV[1], near);
			b.corner(ex + s[0], ey + s[1], ez + s[2], WHITE_UV[0], WHITE_UV[1], near);
			b.corner(fx + s[0], fy + s[1], fz + s[2], WHITE_UV[0], WHITE_UV[1], far);
			b.corner(fx - s[0], fy - s[1], fz - s[2], WHITE_UV[0], WHITE_UV[1], far);
		}
		// motes rising in the light and fading
		int[] mt = HoloHeadMesh.MOTE;
		float mu0 = (mt[0] + 0.5f) / HoloHeadMesh.ATLAS_W, mu1 = (mt[0] + mt[2] - 0.5f) / HoloHeadMesh.ATLAS_W;
		float mv0 = (mt[1] + 0.5f) / HoloHeadMesh.ATLAS_H, mv1 = (mt[1] + mt[3] - 0.5f) / HoloHeadMesh.ATLAS_H;
		Random rnd = new Random(5);
		for (int i = 0; i < MOTES; i++) {
			double h = (rnd.nextDouble() + t * 0.06) % 1.0;
			double rr = rnd.nextDouble(), aa = rnd.nextDouble() * 2 * Math.PI;
			float y = (float) (y0 + 0.1 + h * reach);
			float rad = (float) ((CONE_R + CONE_SPREAD * (y - y0)) * Math.sqrt(rr) * 0.95);
			float x = x0 + rad * (float) Math.cos(aa), z = z0 + rad * (float) Math.sin(aa);
			// a little square facing the camera
			float vx = cx - x, vz = cz - z;
			float vl = (float) Math.sqrt(vx * vx + vz * vz);
			float sx = vl < 1e-6f ? 1 : -vz / vl, sz = vl < 1e-6f ? 0 : vx / vl;
			float hs = 0.025f;
			int col = argb((float) (0.85 * Math.pow(1 - h, 1.2)), 242, 217, 255);
			b.corner(x - sx * hs, y + hs, z - sz * hs, mu0, mv0, col);
			b.corner(x + sx * hs, y + hs, z + sz * hs, mu1, mv0, col);
			b.corner(x + sx * hs, y - hs, z + sz * hs, mu1, mv1, col);
			b.corner(x - sx * hs, y - hs, z - sz * hs, mu0, mv1, col);
		}
	}

}
