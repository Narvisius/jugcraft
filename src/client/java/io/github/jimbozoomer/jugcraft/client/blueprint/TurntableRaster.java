package io.github.jimbozoomer.jugcraft.client.blueprint;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Draws a structure the way a drafter would, turning slowly: every block face that shows is filled in a pale
 * blueprint blue (tinted by the block's own colour) and shaded by which way it faces, and the outline and every
 * step in depth are inked in white. Plain Java (no game classes), so the Blueprint Table can redraw it into a
 * texture a few times a second and the tests can draw it anywhere.
 */
public final class TurntableRaster {
	/** A face per hidden-or-not side: origin offset and two edge axes (0 = x, 1 = y, 2 = z), and its normal. */
	private static final int[][] FACE_ORIGIN = {{0, 0, 0}, {0, 1, 0}, {0, 0, 0}, {0, 0, 1}, {0, 0, 0}, {1, 0, 0}};
	private static final int[][] FACE_AXES = {{0, 2}, {0, 2}, {0, 1}, {0, 1}, {2, 1}, {2, 1}};
	private static final int[][] NORMAL = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
	private static final int INK = 0xFFE8F0FC;
	private static final int TINT = 0x86B4F0;

	private final int width;
	private final int height;
	private final int[] pixels;
	private final float[] depth;
	/** Visible faces: x, y, z, side (0-5) and colour, five ints each. */
	private int[] faces = new int[0];
	private int faceCount;
	private float centreX;
	private float centreY;
	private float centreZ;
	/** Across the footprint corner to corner, and the height, in blocks. */
	private float diagonal = 1;
	private float tall = 1;

	public TurntableRaster(int width, int height) {
		this.width = width;
		this.height = height;
		this.pixels = new int[width * height];
		this.depth = new float[width * height];
	}

	public int width() {
		return width;
	}

	public int height() {
		return height;
	}

	/** The picture after {@link #draw}: ARGB, row by row, transparent where there is no building. */
	public int[] pixels() {
		return pixels;
	}

	/**
	 * Sets the structure: block positions (x, y, z three ints each) and each block's colour (RGB). Faces between
	 * two blocks are dropped, so only the outside (and the inside of rooms) is drawn.
	 */
	public void setBlocks(int[] xyz, int[] colours) {
		int n = colours.length;
		Set<Long> filled = new HashSet<>(n * 2);
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
		for (int i = 0; i < n; i++) {
			int x = xyz[i * 3], y = xyz[i * 3 + 1], z = xyz[i * 3 + 2];
			filled.add(key(x, y, z));
			minX = Math.min(minX, x);
			minY = Math.min(minY, y);
			minZ = Math.min(minZ, z);
			maxX = Math.max(maxX, x);
			maxY = Math.max(maxY, y);
			maxZ = Math.max(maxZ, z);
		}
		int[] out = new int[n * 6 * 5];
		int count = 0;
		for (int i = 0; i < n; i++) {
			int x = xyz[i * 3], y = xyz[i * 3 + 1], z = xyz[i * 3 + 2];
			for (int side = 0; side < 6; side++) {
				int[] d = NORMAL[side];
				if (!filled.contains(key(x + d[0], y + d[1], z + d[2]))) {
					int o = count * 5;
					out[o] = x;
					out[o + 1] = y;
					out[o + 2] = z;
					out[o + 3] = side;
					out[o + 4] = colours[i];
					count++;
				}
			}
		}
		faces = Arrays.copyOf(out, count * 5);
		faceCount = count;
		if (n == 0) {
			return;
		}
		centreX = (minX + maxX + 1) / 2f;
		centreY = (minY + maxY + 1) / 2f;
		centreZ = (minZ + maxZ + 1) / 2f;
		float sx = maxX - minX + 1, sz = maxZ - minZ + 1;
		diagonal = (float) Math.sqrt(sx * sx + sz * sz);
		tall = maxY - minY + 1;
	}

	private static long key(int x, int y, int z) {
		return ((long) (x & 0x1FFFFF) << 42) | ((long) (y & 0x1FFFFF) << 21) | (z & 0x1FFFFF);
	}

	/** Draws the structure turned by {@code yaw} radians, seen from {@code pitch} radians above. */
	public void draw(double yaw, double pitch) {
		Arrays.fill(pixels, 0);
		Arrays.fill(depth, Float.MAX_VALUE);
		// big enough to fill the frame at any turn: the footprint's diagonal across, its height (tilted) up and down
		float k = 0.92f * Math.min(width / diagonal, height / (tall * (float) Math.cos(pitch) + diagonal * (float) Math.sin(pitch)));
		float cy = (float) Math.cos(yaw), sy = (float) Math.sin(yaw), cp = (float) Math.cos(pitch), sp = (float) Math.sin(pitch);
		// world axis -> (screen x, screen y down, depth into the screen)
		float[][] m = new float[3][3];
		float[][] axes = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
		for (int a = 0; a < 3; a++) {
			float x = axes[a][0], y = axes[a][1], z = axes[a][2];
			float rx = x * cy + z * sy, rz = -x * sy + z * cy;
			m[a][0] = -rx * k;
			m[a][1] = -(y * cp + rz * sp) * k;
			m[a][2] = -y * sp + rz * cp;
		}
		float ox = width / 2f - (centreX * m[0][0] + centreY * m[1][0] + centreZ * m[2][0]);
		float oy = height / 2f - (centreX * m[0][1] + centreY * m[1][1] + centreZ * m[2][1]);
		float oz = -(centreX * m[0][2] + centreY * m[1][2] + centreZ * m[2][2]);
		// per side: edge vectors on screen, the inverse of their 2x2, whether it faces us, and its light
		float[][] side = new float[6][];
		for (int s = 0; s < 6; s++) {
			int a = FACE_AXES[s][0], b = FACE_AXES[s][1];
			float ex = m[a][0], ey = m[a][1], ez = m[a][2], fx = m[b][0], fy = m[b][1], fz = m[b][2];
			int[] n = NORMAL[s];
			float facing = n[0] * m[0][2] + n[1] * m[1][2] + n[2] * m[2][2];
			float det = ex * fy - fx * ey;
			if (facing >= -1e-4f || Math.abs(det) < 1e-6f) {
				continue;
			}
			float nx = n[0] * cy + n[2] * sy;
			float nzr = -n[0] * sy + n[2] * cy;
			float light = n[1] > 0 ? 1f : n[1] < 0 ? 0.5f : 0.68f + 0.22f * Math.max(0f, -0.55f * nx - 0.83f * nzr);
			side[s] = new float[] {ex, ey, ez, fx, fy, fz, fy / det, -fx / det, -ey / det, ex / det, light};
		}
		boolean bigFaces = k >= 5;
		for (int f = 0; f < faceCount; f++) {
			int o = f * 5;
			float[] sd = side[faces[o + 3]];
			if (sd == null) {
				continue;
			}
			int[] fo = FACE_ORIGIN[faces[o + 3]];
			float wx = faces[o] + fo[0], wy = faces[o + 1] + fo[1], wz = faces[o + 2] + fo[2];
			float px = ox + wx * m[0][0] + wy * m[1][0] + wz * m[2][0];
			float py = oy + wx * m[0][1] + wy * m[1][1] + wz * m[2][1];
			float pz = oz + wx * m[0][2] + wy * m[1][2] + wz * m[2][2];
			float x0 = Math.min(Math.min(px, px + sd[0]), Math.min(px + sd[3], px + sd[0] + sd[3]));
			float x1 = Math.max(Math.max(px, px + sd[0]), Math.max(px + sd[3], px + sd[0] + sd[3]));
			float y0 = Math.min(Math.min(py, py + sd[1]), Math.min(py + sd[4], py + sd[1] + sd[4]));
			float y1 = Math.max(Math.max(py, py + sd[1]), Math.max(py + sd[4], py + sd[1] + sd[4]));
			int ix0 = Math.max(0, (int) Math.floor(x0)), ix1 = Math.min(width - 1, (int) Math.ceil(x1));
			int iy0 = Math.max(0, (int) Math.floor(y0)), iy1 = Math.min(height - 1, (int) Math.ceil(y1));
			int colour = shade(faces[o + 4], sd[10]);
			float lenA = (float) Math.hypot(sd[0], sd[1]), lenB = (float) Math.hypot(sd[3], sd[4]);
			float edgeA = bigFaces ? 0.8f / Math.max(1f, lenA) : -1, edgeB = bigFaces ? 0.8f / Math.max(1f, lenB) : -1;
			for (int y = iy0; y <= iy1; y++) {
				float dy = y + 0.5f - py;
				for (int x = ix0; x <= ix1; x++) {
					float dx = x + 0.5f - px;
					float s = sd[6] * dx + sd[7] * dy;
					float t = sd[8] * dx + sd[9] * dy;
					if (s < 0 || s > 1 || t < 0 || t > 1) {
						continue;
					}
					float z = pz + s * sd[2] + t * sd[5];
					int i = y * width + x;
					if (z < depth[i]) {
						depth[i] = z;
						boolean edge = s < edgeA || s > 1 - edgeA || t < edgeB || t > 1 - edgeB;
						pixels[i] = edge ? mix(colour, INK, 0.55f) : colour;
					}
				}
			}
		}
		ink();
	}

	/** White ink along the outline and wherever the depth jumps (a wall in front of another). */
	private void ink() {
		boolean[] line = new boolean[pixels.length];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = y * width + x;
				if (pixels[i] == 0) {
					continue;
				}
				for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
					int nx = x + d[0], ny = y + d[1];
					if (nx < 0 || ny < 0 || nx >= width || ny >= height) {
						line[i] = true;
						break;
					}
					int j = ny * width + nx;
					if (pixels[j] == 0 || depth[j] - depth[i] > 1.2f) {
						line[i] = true;
						break;
					}
				}
			}
		}
		for (int i = 0; i < pixels.length; i++) {
			if (line[i]) {
				pixels[i] = INK;
			}
		}
	}

	private static int shade(int rgb, float light) {
		int r = ((rgb >> 16) & 255), g = ((rgb >> 8) & 255), b = rgb & 255;
		int tr = (TINT >> 16) & 255, tg = (TINT >> 8) & 255, tb = TINT & 255;
		r = (int) ((r * 0.3f + tr * 0.7f) * light);
		g = (int) ((g * 0.3f + tg * 0.7f) * light);
		b = (int) ((b * 0.3f + tb * 0.7f) * light);
		return 0xFF000000 | Math.min(255, r) << 16 | Math.min(255, g) << 8 | Math.min(255, b);
	}

	private static int mix(int a, int b, float t) {
		int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
		int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
		int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
		return 0xFF000000 | r << 16 | g << 8 | bl;
	}
}
