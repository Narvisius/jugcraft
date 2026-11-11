package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.tower.MapScan;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.MapColor;
import org.jspecify.annotations.Nullable;

/**
 * The client's copy of each Big Map Table's map (tower/MapScan) and the map's mesh, for client/BigMapTableRenderer. A map
 * that arrives is turned into quads off the render thread, once; the renderer draws the last finished mesh until the next
 * one is ready, so a new map never stalls a frame. Everything is dropped when the client changes level.
 *
 * <p>The mesh is in the table's own space (its master cell's corner at the origin, its middle at 0.5, 0.5): the world round
 * the map's middle, one scale across and up ({@link MapScan#MAP_R} over the view; lower only while a first scan has not
 * yet widened its view to fit), every sample a column from the map's floor to its ground and each span a block of its
 * own at its true height. Tops take the block's map colour, sides darker, undersides darker still; water and glass are
 * see-through; and the outer {@link #FADE} of the map fades out to nothing.
 */
public final class BigMap {
	private BigMap() { }

	/** The map's floor over the table's base (its well's glass is at 0.846) and its fading edge. */
	public static final double FLOOR = 0.90, FADE = 0.2;
	/** Shades of the sides (east and west, north and south) and the undersides, lit from over the table. */
	static final double SHADE_X = 0.8, SHADE_Z = 0.66, SHADE_UNDER = 0.5;
	/** Glass on the map: a pale blue, half see-through. */
	static final int GLASS_RGB = 0xBAD6E4, GLASS_ALPHA = 0x80, WATER_RGB = 0x3A6CDE, WATER_ALPHA = 0xD8;
	/** Floats a quad: four corners of x, y, z and colour (ARGB as float bits). */
	public static final int STRIDE = 16;

	/** From this far from the table's middle (blocks) the coarse mesh is drawn. */
	public static final double FAR = 11;
	/** The faces' directions: +x, -x, +y, -y, +z, -z. */
	public static final int DIRECTIONS = 6;

	/**
	 * A map's quads, the opaque and the see-through, each sorted by the way they face and, within that, by where their plane
	 * is, so a frame draws only those facing the camera ({@link #front}); and what the overlays need to stand on the map.
	 */
	public static final class Mesh {
		public final MapScan.Grid grid;
		/** By direction: the quads, and each one's plane (its x, y or z), ascending. */
		public final float[][] solid, solidPlanes, glow, glowPlanes;
		/** Blocks to table units across and up; the lowest ground in view. */
		public final double s, v;
		public final int lo;
		/** The same map at twice the step, drawn from further off ({@link #FAR}); null for a coarse mesh itself. */
		public @Nullable Mesh coarse;

		Mesh(MapScan.Grid grid, float[][] solid, float[][] solidPlanes, float[][] glow, float[][] glowPlanes, double s, double v, int lo) {
			this.grid = grid;
			this.solid = solid;
			this.solidPlanes = solidPlanes;
			this.glow = glow;
			this.glowPlanes = glowPlanes;
			this.s = s;
			this.v = v;
			this.lo = lo;
		}

		/**
		 * The quads of direction {@code dir} facing a camera at {@code cam} along that direction's axis (table space): {from,
		 * to} as quad indices.
		 */
		public static int[] front(int dir, float[] planes, double cam) {
			int lo = 0, hi = planes.length;
			while (lo < hi) {
				int mid = (lo + hi) >>> 1;
				if (planes[mid] < cam) lo = mid + 1;
				else hi = mid;
			}
			return dir % 2 == 0 ? new int[] {0, lo} : new int[] {lo, planes.length};
		}

		public int quads() {
			int n = 0;
			for (float[] q : solid) n += q.length / STRIDE;
			for (float[] q : glow) n += q.length / STRIDE;
			return n;
		}

		/** The table y of world height y. */
		public double y(double worldY) {
			return FLOOR + (worldY - lo) * v;
		}

		/** The ground's top (world y, the block over it) at (dx, dz) blocks from the map's middle, by the nearest sample. */
		public int groundAt(double dx, double dz) {
			int n = grid.n;
			int i = (int) Math.round(dx / grid.step) + n / 2, j = (int) Math.round(dz / grid.step) + n / 2;
			i = Math.max(0, Math.min(n - 1, i));
			j = Math.max(0, Math.min(n - 1, j));
			short g = grid.ground[j * n + i];
			return g == Short.MIN_VALUE ? lo : g + 1;
		}
	}

	private static final class Entry {
		MapScan.@Nullable Grid grid;
		volatile @Nullable Mesh mesh;
		/** The game time the last map came in. */
		long received;
		int version;
	}

	private static final Map<BlockPos, Entry> MAPS = new HashMap<>();
	private static @Nullable ClientLevel level;
	private static final ExecutorService BUILDER = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "Jugcraft big map mesher");
		t.setDaemon(true);
		return t;
	});

	private static void checkLevel() {
		ClientLevel now = Minecraft.getInstance().level;
		if (now != level) {
			MAPS.clear();
			level = now;
		}
	}

	/** A map from the server (on the client thread): kept, and its mesh made off the render thread. */
	public static void receive(MapScan.Payload payload) {
		checkLevel();
		Entry entry = MAPS.computeIfAbsent(payload.table().immutable(), k -> new Entry());
		MapScan.Grid grid = payload.grid();
		entry.grid = grid;
		entry.received = level == null ? 0 : level.getGameTime();
		int version = ++entry.version;
		BUILDER.execute(() -> {
			Mesh mesh = build(grid);
			mesh.coarse = build(grid.coarser(), mesh.lo, mesh.v);
			if (entry.version == version) {
				entry.mesh = mesh;
			}
		});
	}

	/** The table's map as last made into a mesh, or null. */
	public static @Nullable Mesh mesh(BlockPos table) {
		checkLevel();
		Entry entry = MAPS.get(table);
		return entry == null ? null : entry.mesh;
	}

	/** When the table's last map came in (game time), or Long.MIN_VALUE. */
	public static long received(BlockPos table) {
		checkLevel();
		Entry entry = MAPS.get(table);
		return entry == null ? Long.MIN_VALUE : entry.received;
	}

	// ------------------------------------------------------------------ the mesh

	/** A growing list of quads. */
	static final class Quads {
		float[] data = new float[STRIDE * 1024];
		int size;

		void quad(double ax, double ay, double az, double bx, double by, double bz, double cx, double cy, double cz, double dx, double dy,
				double dz, int colour) {
			if (size + STRIDE > data.length) {
				data = Arrays.copyOf(data, data.length * 2);
			}
			float c = Float.intBitsToFloat(colour);
			float[] d = data;
			int o = size;
			d[o] = (float) ax; d[o + 1] = (float) ay; d[o + 2] = (float) az; d[o + 3] = c;
			d[o + 4] = (float) bx; d[o + 5] = (float) by; d[o + 6] = (float) bz; d[o + 7] = c;
			d[o + 8] = (float) cx; d[o + 9] = (float) cy; d[o + 10] = (float) cz; d[o + 11] = c;
			d[o + 12] = (float) dx; d[o + 13] = (float) dy; d[o + 14] = (float) dz; d[o + 15] = c;
			size += STRIDE;
		}

		float[] toArray() {
			return Arrays.copyOf(data, size);
		}

		/** A quad facing up (its corners taken in whichever order faces up). */
		void up(double ax, double ay, double az, double bx, double by, double bz, double cx, double cy, double cz, double dx, double dy,
				double dz, int colour) {
			// (b - a) x (c - b), its y
			double ny = (bz - az) * (cx - bx) - (bx - ax) * (cz - bz);
			if (ny >= 0) {
				quad(ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, colour);
			} else {
				quad(ax, ay, az, dx, dy, dz, cx, cy, cz, bx, by, bz, colour);
			}
		}

		/** A flat strip from (x0, y0, z0) to (x1, y1, z1), w wide, facing up. */
		void ribbon(double x0, double y0, double z0, double x1, double y1, double z1, double w, int colour) {
			double dx = x1 - x0, dz = z1 - z0, l = Math.hypot(dx, dz);
			if (l < 1e-6) return;
			double px = -dz / l * w / 2, pz = dx / l * w / 2;
			up(x0 - px, y0, z0 - pz, x1 - px, y1, z1 - pz, x1 + px, y1, z1 + pz, x0 + px, y0, z0 + pz, colour);
		}

		/** A flat ring of radius r, w wide, facing up. */
		void ringFlat(double x, double y, double z, double r, double w, int segments, int colour) {
			for (int k = 0; k < segments; k++) {
				double a0 = 2 * Math.PI * k / segments, a1 = 2 * Math.PI * (k + 1) / segments, r0 = r - w / 2, r1 = r + w / 2;
				up(x + Math.sin(a0) * r0, y, z - Math.cos(a0) * r0, x + Math.sin(a1) * r0, y, z - Math.cos(a1) * r0, x + Math.sin(a1) * r1, y,
						z - Math.cos(a1) * r1, x + Math.sin(a0) * r1, y, z - Math.cos(a0) * r1, colour);
			}
		}
	}

	/** RGB of a map colour id ({@link MapScan#GLASS} is glass; water a softer blue than a paper map's, as take 3 had it). */
	static int rgb(int id) {
		if (id == MapScan.GLASS) {
			return GLASS_RGB;
		}
		if (id == MapColor.WATER.id) {
			return WATER_RGB;
		}
		return MapColor.byId(id & 63).col;
	}

	static int argb(int rgb, double shade, int alpha) {
		int r = (int) Math.min(255, ((rgb >> 16) & 255) * shade), g = (int) Math.min(255, ((rgb >> 8) & 255) * shade),
				b = (int) Math.min(255, (rgb & 255) * shade);
		return alpha << 24 | r << 16 | g << 8 | b;
	}

	/** Steps of see-through the map's faces take (so the fading edge merges into runs like the rest). */
	static final int ALPHA_STEPS = 32;

	/** An alpha (0 to 255) to the nearest step; a step under 5 is nothing. */
	static int quantize(double a) {
		int q = (int) Math.round(Math.max(0, Math.min(255, a)) / 255 * (ALPHA_STEPS - 1));
		int out = Math.round(q * 255F / (ALPHA_STEPS - 1));
		return out < 5 ? 0 : out;
	}

	/** A face's alpha: its sample's, less for water and glass. */
	static int faceAlpha(int id, int alpha) {
		if (alpha == 0) return 0;
		return id == MapColor.WATER.id ? quantize(alpha * WATER_ALPHA / 255.0) : id == MapScan.GLASS ? quantize(alpha * GLASS_ALPHA / 255.0) : alpha;
	}

	/** How opaque a sample r blocks from the middle of a map of radius view is: 1 inside, fading out over the outer FADE. */
	static double fade(double r, double view) {
		double a = Math.max(0, Math.min(1, (view - r) / (FADE * view)));
		return Math.pow(a, 1.3);
	}

	/** The map's quads (see the class's comment). */
	static Mesh build(MapScan.Grid g) {
		int[] range = g.range();
		double s = MapScan.MAP_R / g.view;
		return build(g, range[0], Math.min(s, MapScan.HEADROOM / Math.max(1, range[1] - range[0])));
	}

	/** The map's quads with its lowest ground at lo and v table units a block up (a coarse mesh takes its fine one's). */
	static Mesh build(MapScan.Grid g, int lo, double v) {
		int n = g.n, half = n / 2;
		double s = MapScan.MAP_R / g.view;
		Quads solid = new Quads(), glow = new Quads();
		double cell = g.step * s, hc = cell / 2;
		// per sample: its alpha (0 outside the view, in ALPHA_STEPS so runs of it merge), its ground's top (world y, the
		// block over the ground)
		int[] alpha = new int[n * n];
		int[] top = new int[n * n];
		for (int j = 0; j < n; j++) {
			for (int i = 0; i < n; i++) {
				int k = j * n + i;
				short gr = g.ground[k];
				double a = gr == Short.MIN_VALUE ? 0 : fade(Math.hypot((i - half) * (double) g.step, (j - half) * (double) g.step), g.view);
				alpha[k] = quantize(a * 255);
				top[k] = alpha[k] == 0 ? lo : gr + 1;
			}
		}
		// the ground's tops: runs along each row of the same height, colour and see-through, and runs the same as the row
		// before's grown into rectangles
		Map<Long, double[]> open = new HashMap<>(), next = new HashMap<>();
		for (int j = 0; j <= n; j++) {
			double z0 = 0.5 + (j - half) * cell - hc;
			int run = -1, runTop = 0, runColour = 0, runAlpha = 0;
			for (int i = 0; j < n && i <= n; i++) {
				int k = j * n + i;
				int a = i < n ? faceAlpha(g.top[k], alpha[k]) : 0;
				if (run >= 0 && (a != runAlpha || top[k] != runTop || g.top[k] != runColour)) {
					long key = ((((long) run * 512 + i) * 16384 + runTop + 8192) * 64 + (runColour & 63)) * 256 + runAlpha;
					double[] rect = open.remove(key);
					if (rect == null) {
						rect = new double[] {0.5 + (run - half) * cell - hc, 0.5 + (i - half) * cell - hc, z0, 0,
								FLOOR + (runTop - lo) * v, runColour, runAlpha};
					}
					rect[3] = z0 + cell;
					next.put(key, rect);
					run = -1;
				}
				if (i < n && a > 0 && run < 0) {
					run = i;
					runTop = top[k];
					runColour = g.top[k];
					runAlpha = a;
				}
			}
			// what did not grow this row is finished
			for (double[] r : open.values()) {
				int ra = (int) r[6];
				(ra == 255 ? solid : glow).quad(r[0], r[4], r[2], r[0], r[4], r[3], r[1], r[4], r[3], r[1], r[4], r[2], argb(rgb((int) r[5]), 1, ra));
			}
			Map<Long, double[]> t = open;
			open = next;
			next = t;
			next.clear();
		}
		// the ground's sides where the next sample's ground is lower (or out of view): north and south faces merged along
		// the rows, east and west along the columns
		for (int dir = 0; dir < 4; dir++) {
			int di = dir == 0 ? 1 : dir == 1 ? -1 : 0, dj = dir == 2 ? 1 : dir == 3 ? -1 : 0;
			boolean alongRows = dj != 0;
			double shade = di != 0 ? SHADE_X : SHADE_Z;
			for (int line = 0; line < n; line++) {
				int run = -1, runLow = 0, runHigh = 0, runColour = 0, runAlpha = 0;
				for (int t = 0; t <= n; t++) {
					int i = alongRows ? t : line, j = alongRows ? line : t;
					int k = j * n + i;
					int low = 0, high = 0, a = 0;
					if (t < n && alpha[k] > 0) {
						int ni = i + di, nj = j + dj;
						low = Math.max(lo, ni >= 0 && ni < n && nj >= 0 && nj < n ? top[nj * n + ni] : lo);
						high = top[k];
						a = low < high ? faceAlpha(g.side[k], alpha[k]) : 0;
					}
					if (run >= 0 && (a != runAlpha || low != runLow || high != runHigh || g.side[k] != runColour)) {
						side(runAlpha == 255 ? solid : glow, run, t, line, alongRows, di, dj, half, cell, hc, runLow, runHigh, lo, v,
								argb(rgb(runColour), shade, runAlpha));
						run = -1;
					}
					if (t < n && a > 0 && run < 0) {
						run = t;
						runLow = low;
						runHigh = high;
						runColour = g.side[k];
						runAlpha = a;
					}
				}
			}
		}
		// the spans: each a block of its own at its height (its underside where there is air under it)
		int sp = 0;
		for (int k = 0; k < n * n; k++) {
			int count = g.count[k];
			if (count == 0) continue;
			int i = k % n, j = k / n;
			int a0 = alpha[k];
			if (a0 == 0) {
				sp += count;
				continue;
			}
			double x0 = 0.5 + (i - half) * cell - hc, x1 = x0 + cell, z0 = 0.5 + (j - half) * cell - hc, z1 = z0 + cell;
			for (int c = 0; c < count; c++, sp++) {
				int b = Math.max(g.y0[sp], top[k]), t = g.y1[sp];
				if (t <= b) continue;
				double ya = FLOOR + (b - lo) * v, yb = FLOOR + (t - lo) * v;
				int topId = g.spanTop[sp], sideId = g.spanSide[sp];
				int at = faceAlpha(topId, a0), as = faceAlpha(sideId, a0);
				Quads q = at < 255 || as < 255 ? glow : solid;
				q.quad(x0, yb, z0, x0, yb, z1, x1, yb, z1, x1, yb, z0, argb(rgb(topId), 1, at));
				if (g.y0[sp] > top[k]) {
					q.quad(x0, ya, z0, x1, ya, z0, x1, ya, z1, x0, ya, z1, argb(rgb(sideId), SHADE_UNDER, as));
				}
				int sx = argb(rgb(sideId), SHADE_X, as), sz = argb(rgb(sideId), SHADE_Z, as);
				q.quad(x1, ya, z0, x1, yb, z0, x1, yb, z1, x1, ya, z1, sx);
				q.quad(x0, ya, z1, x0, yb, z1, x0, yb, z0, x0, ya, z0, sx);
				q.quad(x1, ya, z1, x1, yb, z1, x0, yb, z1, x0, ya, z1, sz);
				q.quad(x0, ya, z0, x0, yb, z0, x1, yb, z0, x1, ya, z0, sz);
			}
		}
		float[][] solidByDir = new float[DIRECTIONS][], solidPlanes = new float[DIRECTIONS][], glowByDir = new float[DIRECTIONS][],
				glowPlanes = new float[DIRECTIONS][];
		sort(solid, solidByDir, solidPlanes);
		sort(glow, glowByDir, glowPlanes);
		return new Mesh(g, solidByDir, solidPlanes, glowByDir, glowPlanes, s, v, lo);
	}

	/** Splits quads by the way they face (all the map's faces are square to the axes) and sorts each lot by its plane. */
	static void sort(Quads q, float[][] byDir, float[][] planes) {
		int count = q.size / STRIDE;
		int[] dir = new int[count];
		float[] plane = new float[count];
		int[] per = new int[DIRECTIONS];
		float[] d = q.data;
		for (int k = 0; k < count; k++) {
			int o = k * STRIDE;
			float ux = d[o + 4] - d[o], uy = d[o + 5] - d[o + 1], uz = d[o + 6] - d[o + 2];
			float wx = d[o + 8] - d[o + 4], wy = d[o + 9] - d[o + 5], wz = d[o + 10] - d[o + 6];
			float nx = uy * wz - uz * wy, ny = uz * wx - ux * wz, nz = ux * wy - uy * wx;
			float ax = Math.abs(nx), ay = Math.abs(ny), az = Math.abs(nz);
			int axis = ax >= ay && ax >= az ? 0 : ay >= az ? 1 : 2;
			float sign = axis == 0 ? nx : axis == 1 ? ny : nz;
			dir[k] = axis * 2 + (sign >= 0 ? 0 : 1);
			plane[k] = d[o + axis];
			per[dir[k]]++;
		}
		Integer[][] order = new Integer[DIRECTIONS][];
		int[] fill = new int[DIRECTIONS];
		for (int k = 0; k < DIRECTIONS; k++) order[k] = new Integer[per[k]];
		for (int k = 0; k < count; k++) order[dir[k]][fill[dir[k]]++] = k;
		for (int k = 0; k < DIRECTIONS; k++) {
			Arrays.sort(order[k], (a, b) -> Float.compare(plane[a], plane[b]));
			float[] out = new float[per[k] * STRIDE], at = new float[per[k]];
			for (int m = 0; m < per[k]; m++) {
				System.arraycopy(d, order[k][m] * STRIDE, out, m * STRIDE, STRIDE);
				at[m] = plane[order[k][m]];
			}
			byDir[k] = out;
			planes[k] = at;
		}
	}

	/**
	 * A side face of samples {@code from} to {@code to} (exclusive) along a row (or column) {@code line}, on their (di, dj)
	 * side, from world height low to high.
	 */
	private static void side(Quads q, int from, int to, int line, boolean alongRows, int di, int dj, int half, double cell,
			double hc, int low, int high, int lo, double v, int colour) {
		double ya = FLOOR + (low - lo) * v, yb = FLOOR + (high - lo) * v;
		if (alongRows) {
			double x0 = 0.5 + (from - half) * cell - hc, x1 = 0.5 + (to - half) * cell - hc;
			double z = 0.5 + (line - half) * cell + dj * hc;
			if (dj > 0) {
				q.quad(x1, ya, z, x1, yb, z, x0, yb, z, x0, ya, z, colour);
			} else {
				q.quad(x0, ya, z, x0, yb, z, x1, yb, z, x1, ya, z, colour);
			}
		} else {
			double z0 = 0.5 + (from - half) * cell - hc, z1 = 0.5 + (to - half) * cell - hc;
			double x = 0.5 + (line - half) * cell + di * hc;
			if (di > 0) {
				q.quad(x, ya, z0, x, yb, z0, x, yb, z1, x, ya, z1, colour);
			} else {
				q.quad(x, ya, z1, x, yb, z1, x, yb, z0, x, ya, z0, colour);
			}
		}
	}
}
