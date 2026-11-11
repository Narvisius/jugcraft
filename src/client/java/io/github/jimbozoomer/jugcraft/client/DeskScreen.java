package io.github.jimbozoomer.jugcraft.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.Font;

/**
 * One Console Desk monitor's page as it is drawn (Tri-Pillar work list item 14; laid out as the approved mockup sheet,
 * claude/src/dronetower/tools/mockups/ds_pages.py and ds_roles.py): in screen units, 246 across and 149 down the glass
 * (18 to a model pixel), y down; flat colour shapes (rectangles, lines, dots, rings, arcs) and lines of the game's font
 * (capitals 7 units high at scale 1). ConsoleDeskRenderer draws it on the monitor's glass.
 *
 * <p>Shapes are drawn a little off the glass, each one a layer above whatever it overlaps that was drawn before it (a
 * grid of one-unit cells keeps each cell's top layer), so none ever shares a plane with another; text goes over them all.
 */
final class DeskScreen {
	static final float W = 246, H = 149;
	/** Colours (ARGB), as ds_pages.py has them. */
	static final int BG = rgb(9, 13, 15), PANEL = rgb(16, 24, 27), GRID = rgb(32, 44, 48), TXT = rgb(195, 219, 220), DIM = rgb(111, 132, 135),
			AMBER = rgb(255, 186, 116), GREEN = rgb(91, 227, 138), RED = rgb(255, 91, 77), CYAN = rgb(95, 212, 232), WHITE = rgb(236, 240, 240),
			HEAD = rgb(43, 28, 12), ROW = rgb(13, 19, 21), TILE = rgb(14, 20, 22), OFF = rgb(30, 40, 42), LOCKED = rgb(60, 70, 72);
	/** Each drone tier's colour (T1 to T9, then the Creative Drone). */
	static final int[] TIER = {rgb(120, 200, 255), rgb(110, 230, 160), rgb(250, 220, 90), rgb(255, 160, 70), rgb(240, 100, 90),
			rgb(200, 120, 240), rgb(120, 140, 255), rgb(90, 220, 220), rgb(255, 255, 255), rgb(255, 120, 200)};
	/** The layer grid's cells (screen units), and how far in from a shape's edges its cells are taken: more than a cell, so
	 * two shapes that only touch never share one. */
	static final float CELL = 0.5F, EDGE = 0.55F;
	static final int GW = (int) Math.ceil(W / CELL), GH = (int) Math.ceil(H / CELL), MAX_LAYER = 10;
	/** A quad's floats: four corners (x, y), its colour, its layer. */
	static final int STRIDE = 10;

	/** A line of text: x as aligned (0 from x, 1 centred on it, 2 ending at it), y its capitals' top. */
	record Text(float x, float y, String s, int colour, float scale, int align) {
	}

	private final Font font;
	/** How far off it is seen: 0 every line of text; 1 no small print; 2 only the big lines (titles, figures); 3 no text. */
	final int detail;
	/** Each cell's top layer while a page is drawn (pages are drawn one at a time, on the render thread: one grid for all). */
	private static final byte[] LAYERS = new byte[GW * GH];
	private final byte[] grid = LAYERS;
	/** The shape being drawn (begin to end): its quads, the cells under it. */
	private final List<float[]> pending = new ArrayList<>();
	private final IntList cells = new IntList();
	private int depth;
	float[] quads = new float[STRIDE * 256];
	int size;
	final List<Text> texts = new ArrayList<>();

	DeskScreen(Font font, int detail) {
		this.font = font;
		this.detail = detail;
	}

	/**
	 * The render type the screens' pictures are drawn with: opaque, writing depth (each shape hides what it lies over
	 * by its layer's height off the glass; a translucent type is blended order-independently in 26.3, and a picture's
	 * overlapping layers then show through one another from some angles), full bright.
	 */
	static final net.minecraft.client.renderer.rendertype.RenderType SCREEN =
			net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(DroneDepotRenderer.TEXTURE);

	/**
	 * The normal to give a screen's quads (in the pose's frame): straight up in the world, which the game's entity
	 * lighting lights fully, so the picture shows at its own colours whichever way the screen faces.
	 */
	static float[] litNormal(com.mojang.blaze3d.vertex.PoseStack.Pose pose) {
		org.joml.Matrix3f n = pose.normal();
		float x = n.m01(), y = n.m11(), z = n.m21();
		float l = (float) Math.sqrt(x * x + y * y + z * z);
		return l < 1e-6F ? new float[] {0, 1, 0} : new float[] {x / l, y / l, z / l};
	}

	static int rgb(int r, int g, int b) {
		return 0xFF000000 | r << 16 | g << 8 | b;
	}

	/** A colour darkened (divided by {@code k}). */
	static int dark(int argb, int k) {
		return 0xFF000000 | (argb >> 16 & 0xFF) / k << 16 | (argb >> 8 & 0xFF) / k << 8 | (argb & 0xFF) / k;
	}

	/** A colour brightened by {@code add} on each channel. */
	static int light(int argb, int add) {
		return 0xFF000000 | Math.min(255, (argb >> 16 & 0xFF) + add) << 16 | Math.min(255, (argb >> 8 & 0xFF) + add) << 8
				| Math.min(255, (argb & 0xFF) + add);
	}

	static int tier(int t) {
		return TIER[Math.max(0, Math.min(TIER.length - 1, t - 1))];
	}

	void clear() {
		size = 0;
		texts.clear();
		Arrays.fill(grid, (byte) 0);
		depth = 0;
		pending.clear();
	}

	/** A growing list of ints. */
	private static final class IntList {
		int[] data = new int[1024];
		int size;

		void add(int v) {
			if (size == data.length) {
				data = Arrays.copyOf(data, size * 2);
			}
			data[size++] = v;
		}

		int get(int i) {
			return data[i];
		}

		int size() {
			return size;
		}

		void clear() {
			size = 0;
		}
	}

	int count() {
		return size / STRIDE;
	}

	// ------------------------------------------------------------------ layers

	/**
	 * Starts a shape made of several quads (a line through points, an arc, a disc, or a group of shapes the page wants on
	 * one layer: the radar's sweep): they all go on one layer, one above the top layer under any of them. Shapes not in
	 * a group are each their own.
	 */
	void begin() {
		if (depth++ == 0) {
			pending.clear();
			cells.clear();
		}
	}

	/** Ends a shape begun with {@link #begin}: its quads go on its layer, and the cells under it are marked with it. */
	void end() {
		if (--depth > 0) {
			return;
		}
		int top = 0;
		for (int i = 0; i < cells.size(); i++) {
			top = Math.max(top, grid[cells.get(i)]);
		}
		int layer = Math.min(MAX_LAYER, top + 1);
		for (int i = 0; i < cells.size(); i++) {
			grid[cells.get(i)] = (byte) layer;
		}
		for (float[] q : pending) {
			put(q[0], q[1], q[2], q[3], q[4], q[5], q[6], q[7], Float.floatToRawIntBits(q[8]), layer);
		}
		pending.clear();
	}

	/** Shapes drawn by {@code shapes} all on one layer. */
	void group(Runnable shapes) {
		begin();
		shapes.run();
		end();
	}

	/**
	 * The grid cells a quad covers, taken a little in from its edges (so shapes that only touch, a trace's steps or a row
	 * of bars, stay on one layer): an upright rectangle's at once, any other quad's by points across it.
	 */
	private void cover(float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy) {
		boolean upright = ax == bx && cx == dx && ay == dy && by == cy || ay == by && cy == dy && ax == dx && bx == cx;
		if (upright) {
			float x0 = Math.min(Math.min(ax, bx), cx), x1 = Math.max(Math.max(ax, bx), cx);
			float y0 = Math.min(Math.min(ay, by), cy), y1 = Math.max(Math.max(ay, by), cy);
			// a rectangle thinner than two EDGEs is taken along its middle
			float xa = x1 - x0 > 2 * EDGE ? x0 + EDGE : (x0 + x1) / 2, xb = x1 - x0 > 2 * EDGE ? x1 - EDGE : xa;
			float ya = y1 - y0 > 2 * EDGE ? y0 + EDGE : (y0 + y1) / 2, yb = y1 - y0 > 2 * EDGE ? y1 - EDGE : ya;
			int i0 = Math.max(0, (int) Math.floor(xa / CELL)), i1 = Math.min(GW - 1, (int) Math.floor(xb / CELL));
			int j0 = Math.max(0, (int) Math.floor(ya / CELL)), j1 = Math.min(GH - 1, (int) Math.floor(yb / CELL));
			for (int j = j0; j <= j1; j++) {
				for (int i = i0; i <= i1; i++) {
					mark(j * GW + i);
				}
			}
			return;
		}
		// along a-b (and d-c) and along a-d (and b-c), every half unit, the edges left out by EDGE
		float lu = Math.max((float) Math.hypot(bx - ax, by - ay), (float) Math.hypot(cx - dx, cy - dy));
		float lv = Math.max((float) Math.hypot(dx - ax, dy - ay), (float) Math.hypot(cx - bx, cy - by));
		int nu = Math.max(1, (int) Math.ceil(lu / 0.5F)), nv = Math.max(1, (int) Math.ceil(lv / 0.5F));
		float eu = lu > 2 * EDGE ? EDGE / lu : 0.5F, ev = lv > 2 * EDGE ? EDGE / lv : 0.5F;
		nu = eu == 0.5F ? 0 : nu;
		nv = ev == 0.5F ? 0 : nv;
		for (int a = 0; a <= nu; a++) {
			float u = nu == 0 ? 0.5F : eu + (1 - 2 * eu) * a / nu;
			for (int b = 0; b <= nv; b++) {
				float v = nv == 0 ? 0.5F : ev + (1 - 2 * ev) * b / nv;
				float x = (1 - v) * ((1 - u) * ax + u * bx) + v * ((1 - u) * dx + u * cx);
				float y = (1 - v) * ((1 - u) * ay + u * by) + v * ((1 - u) * dy + u * cy);
				int i = (int) Math.floor(x / CELL), j = (int) Math.floor(y / CELL);
				if (i >= 0 && i < GW && j >= 0 && j < GH) {
					mark(j * GW + i);
				}
			}
		}
	}

	private void mark(int cell) {
		cells.add(cell);
	}

	/** A quad (any four corners going round it), drawn facing whoever looks at the screen. */
	private void put(float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy, int colour, int layer) {
		if (size + STRIDE > quads.length) {
			quads = Arrays.copyOf(quads, quads.length * 2);
		}
		// facing the viewer: round anticlockwise as they see it (y down, so the shoelace sum comes out negative)
		float area = (ax * by - bx * ay) + (bx * cy - cx * by) + (cx * dy - dx * cy) + (dx * ay - ax * dy);
		float[] q = area <= 0 ? new float[] {ax, ay, bx, by, cx, cy, dx, dy} : new float[] {ax, ay, dx, dy, cx, cy, bx, by};
		System.arraycopy(q, 0, quads, size, 8);
		quads[size + 8] = Float.intBitsToFloat(colour);
		quads[size + 9] = layer;
		size += STRIDE;
	}

	private void quad(float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy, int colour) {
		float x0 = Math.min(Math.min(ax, bx), Math.min(cx, dx)), x1 = Math.max(Math.max(ax, bx), Math.max(cx, dx));
		float y0 = Math.min(Math.min(ay, by), Math.min(cy, dy)), y1 = Math.max(Math.max(ay, by), Math.max(cy, dy));
		if (x1 < 0 || y1 < 0 || x0 > W || y0 > H) {
			return;
		}
		begin();
		pending.add(new float[] {ax, ay, bx, by, cx, cy, dx, dy, Float.intBitsToFloat(colour)});
		cover(ax, ay, bx, by, cx, cy, dx, dy);
		end();
	}

	// ------------------------------------------------------------------ shapes (as ds_pages.Screen)

	/** The page's background: on the glass itself (layer 0). */
	void background(int colour) {
		put(0, 0, 0, H, W, H, W, 0, colour, 0);
	}

	void rect(double x, double y, double w, double h, int colour) {
		if (w <= 0 || h <= 0) {
			return;
		}
		float x0 = (float) Math.max(0, x), y0 = (float) Math.max(0, y), x1 = (float) Math.min(W, x + w), y1 = (float) Math.min(H, y + h);
		if (x1 <= x0 || y1 <= y0) {
			return;
		}
		quad(x0, y0, x0, y1, x1, y1, x1, y0, colour);
	}

	void frame(double x, double y, double w, double h, int colour) {
		double t = 0.5;
		rect(x, y, w, t, colour);
		rect(x, y + h - t, w, t, colour);
		rect(x, y + t, t, h - 2 * t, colour);
		rect(x + w - t, y + t, t, h - 2 * t, colour);
	}

	/** A line through the points, {@code w} wide; long segments go as pieces, so each takes its own layer. */
	void line(double[] pts, int colour, double w) {
		begin();
		for (int i = 0; i + 3 < pts.length; i += 2) {
			double ax = pts[i], ay = pts[i + 1], bx = pts[i + 2], by = pts[i + 3];
			double len = Math.hypot(bx - ax, by - ay);
			if (len < 1e-4) {
				continue;
			}
			int pieces = Math.max(1, (int) Math.ceil(len / 8));
			double nx = -(by - ay) / len * w / 2, ny = (bx - ax) / len * w / 2;
			for (int k = 0; k < pieces; k++) {
				double sx = ax + (bx - ax) * k / pieces, sy = ay + (by - ay) * k / pieces;
				double ex = ax + (bx - ax) * (k + 1) / pieces, ey = ay + (by - ay) * (k + 1) / pieces;
				quad((float) (sx + nx), (float) (sy + ny), (float) (sx - nx), (float) (sy - ny), (float) (ex - nx), (float) (ey - ny),
						(float) (ex + nx), (float) (ey + ny), colour);
			}
		}
		end();
	}

	void line(double ax, double ay, double bx, double by, int colour, double w) {
		line(new double[] {ax, ay, bx, by}, colour, w);
	}

	void dot(double x, double y, double r, int colour) {
		int n = r < 2 ? 8 : r < 6 ? 12 : 20;
		begin();
		for (int k = 0; k < n; k += 2) {
			double a0 = 2 * Math.PI * k / n, a1 = 2 * Math.PI * (k + 1) / n, a2 = 2 * Math.PI * (k + 2) / n;
			quad((float) x, (float) y, (float) (x + r * Math.cos(a0)), (float) (y + r * Math.sin(a0)), (float) (x + r * Math.cos(a1)),
					(float) (y + r * Math.sin(a1)), (float) (x + r * Math.cos(a2)), (float) (y + r * Math.sin(a2)), colour);
		}
		end();
	}

	/** A circle's outline {@code w} wide, inside radius {@code r} (as PIL draws one). */
	void ring(double x, double y, double r, int colour, double w) {
		arc(x, y, r, 0, 360, colour, w);
	}

	/**
	 * Part of a circle's outline, {@code w} wide inside radius {@code r}, from {@code a0} to {@code a1} degrees (0 at
	 * three o'clock, going clockwise as y runs down: as PIL's arc).
	 */
	void arc(double x, double y, double r, double a0, double a1, int colour, double w) {
		double ro = r, ri = Math.max(0, r - w);
		begin();
		int n = Math.max(4, (int) Math.ceil((a1 - a0) / 360 * (r < 10 ? 24 : r < 30 ? 40 : 64)));
		for (int k = 0; k < n; k++) {
			double s = Math.toRadians(a0 + (a1 - a0) * k / n), e = Math.toRadians(a0 + (a1 - a0) * (k + 1) / n);
			quad((float) (x + ro * Math.cos(s)), (float) (y + ro * Math.sin(s)), (float) (x + ri * Math.cos(s)), (float) (y + ri * Math.sin(s)),
					(float) (x + ri * Math.cos(e)), (float) (y + ri * Math.sin(e)), (float) (x + ro * Math.cos(e)), (float) (y + ro * Math.sin(e)),
					colour);
		}
		end();
	}

	/** A four-sided patch (a trace's fill under one step). */
	void patch(double ax, double ay, double bx, double by, double cx, double cy, double dx, double dy, int colour) {
		quad((float) ax, (float) ay, (float) bx, (float) by, (float) cx, (float) cy, (float) dx, (float) dy, colour);
	}

	/** y: the line's top (its capitals from y to y + 7 x scale); align "left", "right" or "center". */
	void text(double x, double y, String s, int colour, double scale, String align) {
		if (s == null || s.isEmpty() || detail >= 3 || detail == 2 && scale < 0.95 || detail == 1 && scale < 0.66) {
			return;
		}
		texts.add(new Text((float) x, (float) y, s, colour, (float) scale, align.equals("right") ? 2 : align.equals("center") ? 1 : 0));
	}

	void text(double x, double y, String s, int colour, double scale) {
		text(x, y, s, colour, scale, "left");
	}

	double width(String s, double scale) {
		return font == null ? s.length() * 6 * scale : font.width(s) * scale;
	}

	/** A string cut to fit {@code w} units at {@code scale}. */
	String fit(String s, double w, double scale) {
		if (width(s, scale) <= w) {
			return s;
		}
		String out = s;
		while (out.length() > 1 && width(out + ".", scale) > w) {
			out = out.substring(0, out.length() - 1);
		}
		return out.trim() + ".";
	}

	void bar(double x, double y, double w, double h, double frac, int colour, int bg) {
		rect(x, y, w, h, bg);
		if (frac > 0) {
			rect(x, y, Math.max(0.5, w * Math.min(1, frac)), h, colour);
		}
	}

	void bar(double x, double y, double w, double h, double frac, int colour) {
		bar(x, y, w, h, frac, colour, GRID);
	}

	/** The title bar: the role's code, the page's title, the tier and the clock. */
	void header(String code, String title, DeskData d) {
		rect(0, 0, W, 12, HEAD);
		rect(0, 12, W, 0.6, AMBER);
		text(4, 2.5, code, AMBER, 1.0);
		text(4 + width(code, 1) + 4, 2.5, title, WHITE, 1.0);
		text(W - 4, 2.5, "T" + d.tier + "  " + d.clock, DIM, 1.0, "right");
	}

	void footer(String left, String right, boolean ok) {
		rect(0, H - 9, W, 9, PANEL);
		dot(5, H - 4.5, 1.6, ok ? GREEN : AMBER);
		text(9, H - 7, left, DIM, 0.72);
		if (right != null && !right.isEmpty()) {
			text(W - 4, H - 7, right, DIM, 0.72, "right");
		}
	}

	void kv(double x, double y, double w, String k, String v, int colour, double scale) {
		text(x, y, k, DIM, scale * 0.86);
		text(x + w, y, v, colour, scale, "right");
	}

	void kv(double x, double y, double w, String k, String v, int colour) {
		kv(x, y, w, k, v, colour, 0.85);
	}

	/** A big figure under its label, with an optional line under it. */
	void big(double x, double y, String label, String value, int colour, String sub) {
		text(x, y, label, DIM, 0.7);
		text(x, y + 8, value, colour, 1.9);
		if (sub != null) {
			text(x, y + 24, sub, DIM, 0.62);
		}
	}

	/** A round gauge: three quarters of a circle, filled to {@code frac}, the percentage in the middle. */
	void gauge(double cx, double cy, double r, double frac, int colour, String label) {
		frac = Math.max(0, Math.min(1, frac));
		arc(cx, cy, r, 135, 405, GRID, 3.2);
		if (frac > 0) {
			arc(cx, cy, r - 0.05, 135, 135 + 270 * frac, colour, 3.1);
		}
		text(cx, cy - 6, Math.round(frac * 100) + "%", WHITE, 1.4, "center");
		text(cx, cy + 6, label, DIM, 0.62, "center");
	}

	/** Rows of columns: each column (x, align), each cell (text, colour); every other row on a darker stripe. */
	void rows(double x, double y, List<String[]> cells, List<int[]> colours, double[] cx, String[] align, double scale, double step) {
		for (int i = 0; i < cells.size(); i++) {
			double yy = y + i * step;
			if (i % 2 == 0) {
				rect(x - 2, yy - 1.5, (W - 4) - (x - 2), step, ROW);
			}
			String[] row = cells.get(i);
			for (int k = 0; k < row.length && k < cx.length; k++) {
				text(cx[k], yy, row[k], colours.get(i)[k], scale, align[k]);
			}
		}
	}
}
