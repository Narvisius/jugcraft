package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A natural-draft cooling tower's concrete as smooth quads (towers and pump houses build, 19 October 2026; Justin's
 * shell option B, "smooth"): the hyperbolic shell, its cornice and lintel ring beam, the basin's curb and floor. The same
 * math as tools/cooling_tower.py Tower.surfaces and Tower.cells: the profile is cut every half block along the meridian
 * and every half block round the ring (one grid for the whole tower), each cell belongs to the block holding its
 * middle, so neighbouring blocks meet exactly and the tower has no steps or seams.
 *
 * <p>Coordinates are the site's (blocks from the tower's axis on the grass; the generators' z mirrored, as every site's
 * is, tools/water_works.py), so the shell lines up with the tower's other pieces. Too many quads to store (743,000), so
 * each block's are worked out when its chunk is built ({@code client/SiteModel.java}); its collision comes from the
 * concrete's cross section at an eighth of a block.
 */
public final class TowerShell {
	public static final double CELL = 0.5;
	public static final TowerShell MEDIUM = new TowerShell(52.0, 31.0, 115.0, 149.2, 10.0,
			new double[][] {{316, 3, 12.6, 14.85}, {306, 3, 114.6, 116.6}});
	private static final String[] SURFACES = {"shell_out", "cornice", "shell_in", "lintel", "curb", "basin_floor"};
	private static final String[] MATS = {"shell", "cornice", "inside", "precast", "curb", "basin"};

	public final double r0, rt, yt, b, yLintel, yShell, yTop, rtp, ti, rIn, yCtop, rl;
	public final int nphi;
	/** Shell doors: first ring cell, cells across, sill, head. */
	private final double[][] doors;
	/** The curb is cut away for the cold-water outlet (x0, x1, z0, z1, in the generators' frame). */
	private static final double[][] NOTCHES = {{-2.5, 2.5, -53.2, -50.2}};

	/** Per block (site coordinates): its cells, packed (surface, segment, ring cell). */
	private volatile Map<Long, int[]> cells;
	private double[][][] points;      // per surface: [k] = {r, y, run}
	private double[][][] normals;     // per surface: [k] = {nr, ny} (smooth)
	private double[][][] segNormals;  // per surface: [k] = {nr, ny} of segment k
	private final Map<Long, VoxelShape> shapes = new ConcurrentHashMap<>();

	public TowerShell(double r0, double rt, double yt, double top, double lintel, double[][] doors) {
		this.r0 = r0;
		this.rt = rt;
		this.yt = yt;
		this.b = yt / Math.sqrt((r0 / rt) * (r0 / rt) - 1);
		this.yLintel = lintel;
		this.yShell = lintel + 1.6;
		this.yTop = top;
		this.rtp = rad(top);
		this.ti = thick(top);
		this.rIn = rtp - ti - 0.25;
		this.yCtop = top + 1.3;
		this.rl = rad(lintel);
		this.nphi = (int) Math.round(2 * Math.PI * r0 / CELL);
		this.doors = doors;
	}

	public double rad(double y) {
		double k = (y - yt) / b;
		return rt * Math.sqrt(1 + k * k);
	}

	public double thick(double y) {
		return 0.35 + 0.55 * Math.max(0.0, 1 - (y - yShell) / 30);
	}

	/** The profile polylines (r, y), each drawn so its outside is on its left going along it (Tower.surfaces). */
	private double[][][] surfaces() {
		List<double[]> ys = new ArrayList<>();
		int n = (int) Math.ceil((yTop - 0.3 - yShell) / 0.5);
		for (int i = 0; i < n; i++) {
			ys.add(new double[] {yShell + i * 0.5});
		}
		ys.add(new double[] {yTop - 0.3});
		double yl = yTop - 0.3;
		List<double[]> outer = new ArrayList<>(), inner = new ArrayList<>();
		for (double[] y : ys) {
			outer.add(new double[] {rad(y[0]), y[0]});
		}
		outer.add(new double[] {rad(yTop), yTop});
		for (int i = ys.size() - 1; i >= 0; i--) {
			double y = ys.get(i)[0];
			inner.add(new double[] {rad(y) - thick(y), y});
		}
		double R = rtp, L = rl;
		return new double[][][] {
				outer.toArray(new double[0][]),
				{{R, yTop}, {R + 0.55, yTop + 0.15}, {R + 0.55, yCtop}, {rIn, yCtop}, {rIn, yl}, {rad(yl) - thick(yl), yl}},
				inner.toArray(new double[0][]),
				{{rad(yShell) - thick(yShell), yShell}, {L - 0.95, yShell}, {L - 0.95, yLintel}, {L + 0.55, yLintel}, {L + 0.55, yShell},
						{rad(yShell), yShell}},
				{{53.0, 0.0}, {53.0, 1.0}, {50.4, 1.0}, {50.4, -2.0}},
				{{50.4, -2.0}, {0.0, -2.0}}};
	}

	private static boolean notched(double x, double z) {
		for (double[] n : NOTCHES) {
			if (n[0] < x && x < n[1] && n[2] < z && z < n[3]) {
				return true;
			}
		}
		return false;
	}

	private boolean doorCut(int i, double y) {
		for (double[] d : doors) {
			if (d[0] <= i && i < d[0] + d[1] && d[2] < y && y < d[3]) {
				return true;
			}
		}
		return false;
	}

	private static long key(int x, int y, int z) {
		return ((long) (x & 0x3FFFFF) << 42) | ((long) (y & 0xFFFFF) << 22) | (z & 0x3FFFFF);
	}

	/** Builds the cell index once (about 740,000 cells, a fraction of a second). */
	private Map<Long, int[]> cells() {
		Map<Long, int[]> c = cells;
		if (c != null) {
			return c;
		}
		synchronized (this) {
			if (cells != null) {
				return cells;
			}
			double[][][] surf = surfaces();
			points = new double[surf.length][][];
			normals = new double[surf.length][][];
			segNormals = new double[surf.length][][];
			Map<Long, List<Integer>> build = new HashMap<>();
			double dphi = 2 * Math.PI / nphi;
			for (int s = 0; s < surf.length; s++) {
				double[][] poly = surf[s];
				List<double[]> pts = new ArrayList<>();
				double run = 0;
				for (int k = 0; k + 1 < poly.length; k++) {
					double[] a = poly[k], q = poly[k + 1];
					double seg = Math.hypot(q[0] - a[0], q[1] - a[1]);
					int n = Math.max(1, (int) Math.ceil(seg / CELL - 1e-6));
					for (int j = 0; j < n; j++) {
						double t = j / (double) n;
						pts.add(new double[] {a[0] + (q[0] - a[0]) * t, a[1] + (q[1] - a[1]) * t, run + seg * t});
					}
					run += seg;
				}
				double[] last = poly[poly.length - 1];
				pts.add(new double[] {last[0], last[1], run});
				double[][] P = pts.toArray(new double[0][]);
				double[][] N = new double[P.length][];
				for (int k = 0; k < P.length; k++) {
					double[] a = P[Math.max(0, k - 1)], q = P[Math.min(P.length - 1, k + 1)];
					double dr = q[0] - a[0], dy = q[1] - a[1], ln = Math.hypot(dr, dy);
					if (ln == 0) ln = 1;
					N[k] = new double[] {dy / ln, -dr / ln};
				}
				double[][] SN = new double[P.length - 1][];
				for (int k = 0; k + 1 < P.length; k++) {
					double dr = P[k + 1][0] - P[k][0], dy = P[k + 1][1] - P[k][1], ln = Math.hypot(dr, dy);
					if (ln == 0) ln = 1;
					SN[k] = new double[] {dy / ln, -dr / ln};
				}
				points[s] = P;
				normals[s] = N;
				segNormals[s] = SN;
				boolean shell = SURFACES[s].startsWith("shell");
				for (int i = 0; i < nphi; i++) {
					double am = (i + 0.5) * dphi;
					double sa = Math.sin(am), ca = Math.cos(am);
					for (int k = 0; k + 1 < P.length; k++) {
						double[] p = P[k], q = P[k + 1], sn = SN[k];
						double rm = (p[0] + q[0]) / 2 - sn[0] * 0.02, ym = (p[1] + q[1]) / 2 - sn[1] * 0.02;
						if (s == 4 && notched(rm * sa, rm * ca)) {
							continue;
						}
						if (shell && doorCut(i, (p[1] + q[1]) / 2)) {
							continue;
						}
						int bx = (int) Math.floor(rm * sa), by = (int) Math.floor(ym), bz = (int) Math.floor(rm * ca);
						// the site's frame: the generators' z mirrored
						build.computeIfAbsent(key(bx, by, -bz - 1), x -> new ArrayList<>()).add((s << 28) | (k << 12) | i);
					}
				}
			}
			Map<Long, int[]> out = new HashMap<>(build.size() * 2);
			for (Map.Entry<Long, List<Integer>> e : build.entrySet()) {
				out.put(e.getKey(), e.getValue().stream().mapToInt(Integer::intValue).toArray());
			}
			cells = out;
			return out;
		}
	}

	/** Whether a block (site coordinates) holds any of the shell. */
	public boolean has(int x, int y, int z) {
		return cells().containsKey(key(x, y, z));
	}

	/** One quad: texture name; four corners x, y, z (blocks, from the block's corner), u, v (0..1); per-corner normals. */
	public record Quad(String texture, float[] v, float[] n) {
	}

	/** The block's quads, wound for the game (counter-clockwise from outside). */
	public List<Quad> quads(int x, int y, int z) {
		int[] list = cells().get(key(x, y, z));
		if (list == null) {
			return List.of();
		}
		double dphi = 2 * Math.PI / nphi;
		List<Quad> out = new ArrayList<>(list.length);
		int gz = -z - 1;                     // the block in the generators' frame
		for (int ref : list) {
			int s = ref >>> 28, k = (ref >>> 12) & 0xFFFF, i = ref & 0xFFF;
			double[] p = points[s][k], q = points[s][k + 1], sn = segNormals[s][k];
			boolean smooth = SURFACES[s].startsWith("shell");
			double[] n0 = smooth ? normals[s][k] : sn, n1 = smooth ? normals[s][k + 1] : sn;
			double a0 = i * dphi, a1 = (i + 1) * dphi;
			double[][] C = {{p[0], p[1], a0}, {p[0], p[1], a1}, {q[0], q[1], a1}, {q[0], q[1], a0}};
			double[][] NN = {n0, n0, n1, n1};
			double u0 = (i % 2) * 0.5, vv0 = p[2] % 1.0;
			float[] v = new float[20], n = new float[12];
			// the generators' order (counter-clockwise from outside there) reversed, the mirror having turned it about
			for (int c = 0; c < 4; c++) {
				int src = 3 - c;
				double r = C[src][0], yy = C[src][1], a = C[src][2];
				double gx = r * Math.sin(a), gzz = r * Math.cos(a);
				v[c * 5] = (float) (gx - x);
				v[c * 5 + 1] = (float) (yy - y);
				v[c * 5 + 2] = (float) (-gzz - z);
				double u = u0 + (src == 1 || src == 2 ? 0.5 : 0.0);
				double vv = vv0 + (src == 2 || src == 3 ? Math.min(CELL, 1.0 - vv0) : 0.0);
				v[c * 5 + 3] = (float) u;
				v[c * 5 + 4] = (float) (1 - vv);
				double nr = NN[src][0], ny = NN[src][1];
				n[c * 3] = (float) (nr * Math.sin(a));
				n[c * 3 + 1] = (float) ny;
				n[c * 3 + 2] = (float) (-nr * Math.cos(a));
			}
			out.add(new Quad(material(x, y, gz, MATS[s]), v, n));
		}
		return out;
	}

	/** The texture a cell uses (Tower.material): the shell's weathering by height, a variant by place. */
	private String material(int bx, int by, int gbz, String mat) {
		int v = Math.floorMod(bx * 7919L + gbz * 104729L, 6);
		switch (mat) {
			case "shell" -> {
				if (by >= yTop - 25) {
					return "ct_concrete_top" + Math.min(4, (int) ((yTop + 1 - by) / 5)) + "_" + v;
				}
				if (by < yShell + 16) {
					return "ct_concrete_low" + Math.max(0, Math.min(7, (int) ((yShell + 16 - by) / 2))) + "_" + v;
				}
				return "ct_concrete_mid_" + v;
			}
			case "inside" -> {
				return "ct_concrete_in_" + v;
			}
			case "cornice" -> {
				return "ct_concrete_flat_" + v;
			}
			case "precast" -> {
				return "ct_precast";
			}
			case "curb" -> {
				return "ct_precast_curb";
			}
			default -> {
				return "ct_basin";
			}
		}
	}

	/** The textures the shell uses. */
	public static List<String> textures() {
		List<String> out = new ArrayList<>();
		for (int v = 0; v < 6; v++) {
			for (int l = 0; l < 5; l++) out.add("ct_concrete_top" + l + "_" + v);
			for (int l = 0; l < 8; l++) out.add("ct_concrete_low" + l + "_" + v);
			out.add("ct_concrete_mid_" + v);
			out.add("ct_concrete_in_" + v);
			out.add("ct_concrete_flat_" + v);
		}
		out.add("ct_basin");
		out.add("ct_precast");
		out.add("ct_precast_curb");
		return out;
	}

	// ------------------------------------------------------------------ collision

	/** Whether a point (the generators' frame) is in the concrete. */
	private boolean solid(double x, double y, double z) {
		double r = Math.hypot(x, z);
		if (y < -2 && r < 50.4) {
			return y > -3.0;
		}
		if (r >= 50.4 && r <= 53.0 && y <= 1.0 && y > -3.0) {
			return !notched(x, z);
		}
		if (y >= yLintel && y <= yShell && r >= rl - 0.95 && r <= rl + 0.55) {
			return true;
		}
		if (y >= yShell && y <= yTop) {
			if (r <= rad(y) && r >= rad(y) - thick(y)) {
				double phi = Math.atan2(x, z);
				if (phi < 0) phi += 2 * Math.PI;
				return !doorCut((int) Math.floor(phi / (2 * Math.PI / nphi)), y);
			}
		}
		if (y >= yTop - 0.3 && y <= yCtop && r >= rIn && r <= rtp + 0.55) {
			return true;
		}
		return false;
	}

	/** The block's collision (site coordinates), from the concrete in it at an eighth of a block. */
	public VoxelShape shape(int x, int y, int z) {
		return shapes.computeIfAbsent(key(x, y, z), k -> {
			if (!has(x, y, z) && !solidAny(x, y, z)) {
				return Shapes.empty();
			}
			boolean[][][] v = new boolean[8][8][8];
			boolean any = false;
			for (int i = 0; i < 8; i++) {
				for (int j = 0; j < 8; j++) {
					for (int l = 0; l < 8; l++) {
						double px = x + (i + 0.5) / 8, py = y + (j + 0.5) / 8, pz = z + (l + 0.5) / 8;
						if (solid(px, py, -pz)) {
							v[i][j][l] = true;
							any = true;
						}
					}
				}
			}
			return any ? merge(v) : Shapes.empty();
		});
	}

	private boolean solidAny(int x, int y, int z) {
		return solid(x + 0.5, y + 0.5, -(z + 0.5));
	}

	/** Merges an 8 x 8 x 8 grid into boxes (greedy) and makes a shape of them. */
	static VoxelShape merge(boolean[][][] v) {
		int n = v.length;
		boolean[][][] left = new boolean[n][n][n];
		for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) left[i][j] = v[i][j].clone();
		VoxelShape shape = Shapes.empty();
		List<VoxelShape> parts = new ArrayList<>();
		for (int y = 0; y < n; y++) {
			for (int x = 0; x < n; x++) {
				for (int z = 0; z < n; z++) {
					if (!left[x][y][z]) continue;
					int x1 = x;
					while (x1 + 1 < n && left[x1 + 1][y][z]) x1++;
					int z1 = z;
					outer:
					while (z1 + 1 < n) {
						for (int a = x; a <= x1; a++) if (!left[a][y][z1 + 1]) break outer;
						z1++;
					}
					int y1 = y;
					outer2:
					while (y1 + 1 < n) {
						for (int a = x; a <= x1; a++) for (int c = z; c <= z1; c++) if (!left[a][y1 + 1][c]) break outer2;
						y1++;
					}
					for (int a = x; a <= x1; a++) for (int bb = y; bb <= y1; bb++) for (int c = z; c <= z1; c++) left[a][bb][c] = false;
					parts.add(Shapes.box(x / (double) n, y / (double) n, z / (double) n, (x1 + 1) / (double) n, (y1 + 1) / (double) n,
							(z1 + 1) / (double) n));
				}
			}
		}
		for (VoxelShape p : parts) {
			shape = Shapes.joinUnoptimized(shape, p, BooleanOp.OR);
		}
		return shape.optimize();
	}
}
