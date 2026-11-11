package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DepotView;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.FlightPath;
import io.github.jimbozoomer.jugcraft.drone.FlightScheduler;
import io.github.jimbozoomer.jugcraft.tower.CurvedVideoWallBlock;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.TowerData;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The Curved Video Wall (tower/CurvedVideoWallBlock; work list item 7, approved 22 October 2026): its mesh from
 * tools/curved_video_wall.py (the surround, the dark LED cabinets), turned round its axis to the master's aim, and on its
 * 30 facets the board it shows, live from the depot it is pinned to, laid out as on the approved mockup (a 2400 x 1000
 * pixel canvas, a facet each 80 pixels across):
 * <ul>
 * <li>OPERATIONS: the site from above (the hall, the pillars, the roof pads, every drone docked or out with its track;
 * the map widens to take in the farthest), the tower by tier (built, the tier building and how far, still to come),
 * status tiles (fleet, airborne, jobs open, covered), the alerts with when each began, a ticker;</li>
 * <li>FLEET BOARD: every flight (drone, tier, model, where it is going, what it is doing, when it will be home, how far
 * through), every hangar bay of each pillar home, out or empty, a ticker of drones going out and coming home;</li>
 * <li>LOGISTICS: what the supply store holds most of, the battery and the draw over the last in-game day, the two
 * exchanges (net energy and cargo coming in), what the tower is short of, a ticker.</li>
 * </ul>
 * The board is drawn opaque and full bright over the cabinets, in layers a few thousandths of a block apart; the
 * cabinet seams go over it faintly, as on a real wall.
 */
public final class CurvedVideoWallRenderer implements BlockEntityRenderer<DepotDisplayBlockEntity, CurvedVideoWallRenderer.State> {
	static final String MESH_NAME = "curved_video_wall";
	static final Identifier TEXTURE = Jugcraft.id("textures/entity/curved_video_wall.png");
	static final Identifier TOWER = Jugcraft.id("textures/entity/curved_video_wall_tower.png");
	private static final RenderType MESH = RenderTypes.entityCutout(TEXTURE);
	private static final RenderType BOARD = RenderTypes.entityCutout(DroneDepotRenderer.TEXTURE);
	private static final RenderType PICTURE = RenderTypes.entityCutout(TOWER);
	private static final RenderType SEAMS = GlowTypes.glow(DroneDepotRenderer.TEXTURE);
	/** The canvas, its facets (cabinet columns) and cabinet rows; the live kind of a facet in the mesh. */
	static final int W = 2400, H = 1000, COLS = 30, ROWS = 22, FACET_PX = W / COLS, FACET = 6;
	/** Layers over the cabinets (blocks): background, panels, tiles, marks, top; text; the seams over everything. */
	static final float L_BG = 0.003f, L_PANEL = 0.006f, L_TILE = 0.009f, L_MARK = 0.012f, L_TOP = 0.015f, L_TEXT = 0.02f, L_SEAM = 0.024f;
	static final int BG = 0xFF05070A, HEADER = 0xFF10161C, PANEL = 0xFF0C1117, PANEL_HEAD = 0xFF121920, EDGE = 0xFF26303A,
			AMBER = 0xFFFFB04A, AMBER_HI = 0xFFFFD696, AMBER_DIM = 0xFFE6963C, TEXT = 0xFFD6E0E8, TITLE = 0xFFFFEED6, DIM = 0xFF788896,
			BLUE = 0xFF6EC8FF, GREEN = 0xFF5AE678, RED = 0xFFFF5A4A, TRACK = 0xFF242C34, GRID = 0xFF16202A, STRIPE = 0xFF0A0E13,
			CLOCK = 0xFFFF9646, TICKER_BG = 0xFF0E0C08, TICKER_LINE = 0xFF5A3C18, EMPTY = 0xFF3A444E;
	private static final String[] TIER_NAMES = {"ROOF PADS", "PILLAR FEET", "SMALL HANGAR ROWS", "MEDIUM HANGARS", "LOGISTICS LEVELS",
			"COMPOSITE LEVELS", "LARGE HANGARS", "HEAVY LEVELS", "CROWN"};

	private final Font font;

	public CurvedVideoWallRenderer(BlockEntityRendererProvider.Context context) {
		this(context.font());
	}

	CurvedVideoWallRenderer(Font font) {
		this.font = font;
	}

	/** A run of text on one facet: its top-left in the facet's pixels, its colour and size (canvas pixels a font pixel). */
	record Text(int facet, String s, float x, float y, int color, float scale) {
	}

	public static final class State extends BlockEntityRenderState {
		int aim = -1;
		final HoloGeometry.Quads board = HoloGeometry.Quads.perFrame();
		final HoloGeometry.Quads seams = HoloGeometry.Quads.perFrame();
		final List<float[]> picture = new ArrayList<>();
		final List<Text> texts = new ArrayList<>();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public int getViewDistance() {
		return 96;
	}

	/** The wall reaches 15 blocks from its master cell. */
	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	// ------------------------------------------------------------------ facets

	/** A facet's frame: canvas pixels (x right from the facet's left edge, y down from the display's top) to the mesh's. */
	static final class Facet {
		final Vector3f tl, r, d, n;
		final float kx, ky;

		Facet(float[][] c) {
			tl = new Vector3f(c[0][0], c[0][1], c[0][2]);
			Vector3f tr = new Vector3f(c[1][0], c[1][1], c[1][2]), bl = new Vector3f(c[3][0], c[3][1], c[3][2]);
			r = tr.sub(tl, new Vector3f());
			float w = r.length();
			r.normalize();
			d = bl.sub(tl, new Vector3f());
			float h = d.length();
			d.normalize();
			n = new Vector3f(d).cross(r).normalize();
			kx = w / FACET_PX;
			ky = h / H;
		}

		float[] at(float x, float y, float lift) {
			return new float[] {tl.x + r.x * x * kx + d.x * y * ky + n.x * lift, tl.y + r.y * x * kx + d.y * y * ky + n.y * lift,
					tl.z + r.z * x * kx + d.z * y * ky + n.z * lift};
		}
	}

	private static Facet[] facets;

	/** The 30 facets, from the mesh's live faces (column by column), or null without the mesh. */
	static Facet[] facets() {
		if (facets == null) {
			ConsoleMesh mesh = ConsoleMesh.get(MESH_NAME);
			if (mesh == null) {
				return null;
			}
			Facet[] out = new Facet[COLS];
			for (int i = 0; i < mesh.live.length; i++) {
				if (mesh.liveKind[i] == FACET && mesh.liveId[i] >= 0 && mesh.liveId[i] < COLS) {
					out[mesh.liveId[i]] = new Facet(mesh.live[i]);
				}
			}
			for (Facet f : out) {
				if (f == null) {
					return null;
				}
			}
			facets = out;
		}
		return facets;
	}

	// ------------------------------------------------------------------ the tower picture's tier rows

	private static float[] tierRows;
	private static int[] towerColumns = {13, 139};
	private static final int TOWER_W = 142, TOWER_H = 434;

	/** Rows of the tower picture (from its top) where each tier line is (TB 0 to 9) and the picture's top. */
	static float[] tierRows() {
		if (tierRows == null) {
			float[] rows = {434.0f, 400.87f, 374.37f, 342.89f, 308.11f, 268.35f, 218.66f, 165.65f, 106.02f, 39.76f, 0.0f};
			try {
				Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(Jugcraft.id("tower_mesh/curved_video_wall.json"));
				if (resource.isPresent()) {
					try (InputStream in = resource.get().open()) {
						var json = com.google.gson.JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
						var tower = json.getAsJsonObject("tower");
						var list = tower.getAsJsonArray("tier_rows");
						float[] read = new float[list.size()];
						for (int i = 0; i < read.length; i++) {
							read[i] = list.get(i).getAsFloat();
						}
						if (read.length == 11) {
							rows = read;
						}
						var cols = tower.getAsJsonArray("columns");
						towerColumns = new int[] {cols.get(0).getAsInt(), cols.get(1).getAsInt()};
					}
				}
			} catch (Exception e) {
				Jugcraft.LOGGER.warn("Could not read the video wall's tower picture rows", e);
			}
			tierRows = rows;
		}
		return tierRows;
	}

	// ------------------------------------------------------------------ the canvas

	/** Draws on the 2400 x 1000 canvas, splitting every shape and run of text at the facets' edges. */
	static final class Canvas {
		/** Characters' widths in {@link #WIDTH_FONT} (render thread only). */
		private static final it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap WIDTHS = new it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap();
		private static Font WIDTH_FONT;
		final Facet[] f;
		final State state;
		final Font font;

		Canvas(Facet[] f, State state, Font font) {
			this.f = f;
			this.state = state;
			this.font = font;
		}

		private static int col(float x) {
			return Math.max(0, Math.min(COLS - 1, (int) Math.floor(x / FACET_PX)));
		}

		private void quad(HoloGeometry.Quads q, int i, float a, float y0, float b, float y1, int color, float lift) {
			Facet fa = f[i];
			float[] p = fa.at(a, y0, lift), s = fa.at(b, y0, lift), t = fa.at(b, y1, lift), u = fa.at(a, y1, lift);
			q.facing(p[0], p[1], p[2], s[0], s[1], s[2], t[0], t[1], t[2], u[0], u[1], u[2], fa.n.x, fa.n.y, fa.n.z, color, color, color, color);
		}

		void rect(float x0, float y0, float x1, float y1, int color, float lift) {
			rect(state.board, x0, y0, x1, y1, color, lift);
		}

		void rect(HoloGeometry.Quads q, float x0, float y0, float x1, float y1, int color, float lift) {
			x0 = Math.max(0, x0);
			x1 = Math.min(W, x1);
			y0 = Math.max(0, y0);
			y1 = Math.min(H, y1);
			if (x1 - x0 < 0.05f || y1 - y0 < 0.05f) {
				return;
			}
			for (int i = col(x0); i <= col(x1 - 0.01f); i++) {
				float a = Math.max(x0, i * FACET_PX) - i * FACET_PX, b = Math.min(x1, (i + 1) * FACET_PX) - i * FACET_PX;
				if (b - a > 0.01f) {
					quad(q, i, a, y0, b, y1, color, lift);
				}
			}
		}

		/** An outline {@code w} pixels wide inside the box. */
		void box(float x0, float y0, float x1, float y1, float w, int color, float lift) {
			rect(x0, y0, x1, y0 + w, color, lift);
			rect(x0, y1 - w, x1, y1, color, lift);
			rect(x0, y0 + w, x0 + w, y1 - w, color, lift);
			rect(x1 - w, y0 + w, x1, y1 - w, color, lift);
		}

		/** A straight stroke {@code w} pixels wide, split where it crosses a facet's edge. */
		void line(float x0, float y0, float x1, float y1, float w, int color, float lift) {
			float dx = x1 - x0, dy = y1 - y0, l = (float) Math.hypot(dx, dy);
			if (l < 0.01f) {
				return;
			}
			float px = -dy / l * w / 2, py = dx / l * w / 2;
			List<Float> cuts = new ArrayList<>();
			cuts.add(0f);
			if (Math.abs(dx) > 1e-4f) {
				int a = (int) Math.floor(Math.min(x0, x1) / FACET_PX) + 1, b = (int) Math.floor(Math.max(x0, x1) / FACET_PX);
				for (int k = a; k <= b; k++) {
					float t = (k * FACET_PX - x0) / dx;
					if (t > 0 && t < 1) {
						cuts.add(t);
					}
				}
			}
			cuts.add(1f);
			cuts.sort(null);
			for (int c = 0; c + 1 < cuts.size(); c++) {
				float ta = cuts.get(c), tb = cuts.get(c + 1);
				float xa = x0 + dx * ta, ya = y0 + dy * ta, xb = x0 + dx * tb, yb = y0 + dy * tb;
				float mx = (xa + xb) / 2;
				if (mx < 0 || mx > W || Math.max(ya, yb) < 0 || Math.min(ya, yb) > H) {
					continue;
				}
				int i = col(mx);
				float o = i * FACET_PX;
				Facet fa = f[i];
				float[] p = fa.at(xa + px - o, ya + py, lift), s = fa.at(xb + px - o, yb + py, lift), t = fa.at(xb - px - o, yb - py, lift),
						u = fa.at(xa - px - o, ya - py, lift);
				state.board.facing(p[0], p[1], p[2], s[0], s[1], s[2], t[0], t[1], t[2], u[0], u[1], u[2], fa.n.x, fa.n.y, fa.n.z, color, color, color,
						color);
			}
		}

		void polyline(float[] xs, float[] ys, int n, float w, int color, float lift) {
			for (int k = 0; k + 1 < n; k++) {
				line(xs[k], ys[k], xs[k + 1], ys[k + 1], w, color, lift);
			}
		}

		/** A filled dot (an octagon) of radius {@code r} pixels. */
		void dot(float x, float y, float r, int color, float lift) {
			float c = r * 0.414f;
			rect(x - r, y - c, x + r, y + c, color, lift);
			rect(x - c, y - r, x + c, y - c, color, lift);
			rect(x - c, y + c, x + c, y + r, color, lift);
			for (int k = 0; k < 4; k++) {
				float sx = k % 2 == 0 ? 1 : -1, sy = k < 2 ? 1 : -1;
				line(x + sx * c, y + sy * r * 0.82f, x + sx * r * 0.82f, y + sy * c, r * 0.5f, color, lift);
			}
		}

		float width(String s, float scale) {
			return font.width(s) * scale;
		}

		/** One character's width in the font (kept per character: no string per character every frame). */
		private float charWidth(int cp, String s, int at, int n) {
			if (WIDTH_FONT != font) {
				WIDTHS.clear();
				WIDTH_FONT = font;
			}
			float w = WIDTHS.getOrDefault(cp, Float.NaN);
			if (w != w) {
				w = font.width(s.substring(at, at + n));
				WIDTHS.put(cp, w);
			}
			return w;
		}

		/**
		 * Text with its capitals' middle at {@code y}: left (align 0), right (1) or centred (2) on {@code x}; split into
		 * runs, a run a facet (by where each character's middle falls), clipped to the canvas.
		 */
		void text(String s, float x, float y, int color, int align, float scale) {
			if (s == null || s.isEmpty()) {
				return;
			}
			float w = width(s, scale);
			float x0 = align == 1 ? x - w : align == 2 ? x - w / 2 : x;
			float top = y - 3.5f * scale;
			StringBuilder run = new StringBuilder();
			int cur = -1;
			float runX = 0, cx = x0;
			for (int i = 0; i < s.length(); ) {
				int cp = s.codePointAt(i);
				int n = Character.charCount(cp);
				float cw = charWidth(cp, s, i, n) * scale;
				i += n;
				float mid = cx + cw / 2;
				int facet = mid < 0 || mid > W ? -1 : col(mid);
				if (facet != cur) {
					if (cur >= 0 && run.length() > 0) {
						state.texts.add(new Text(cur, run.toString(), runX - cur * FACET_PX, top, color, scale));
					}
					run.setLength(0);
					cur = facet;
					runX = cx;
				}
				if (facet >= 0) {
					run.appendCodePoint(cp);
				}
				cx += cw;
			}
			if (cur >= 0 && run.length() > 0) {
				state.texts.add(new Text(cur, run.toString(), runX - cur * FACET_PX, top, color, scale));
			}
		}

		/** Part of the tower picture (texels u0..u1, v0..v1 of its 142 x 434) over the box, tinted. */
		void picture(float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, int color) {
			if (x1 - x0 < 0.05f || y1 - y0 < 0.05f) {
				return;
			}
			for (int i = col(x0); i <= col(x1 - 0.01f); i++) {
				float a = Math.max(x0, i * FACET_PX), b = Math.min(x1, (i + 1) * FACET_PX);
				if (b - a < 0.01f) {
					continue;
				}
				float ua = u0 + (u1 - u0) * (a - x0) / (x1 - x0), ub = u0 + (u1 - u0) * (b - x0) / (x1 - x0);
				Facet fa = f[i];
				float o = i * FACET_PX;
				float[] p = fa.at(a - o, y0, L_TILE), s = fa.at(b - o, y0, L_TILE), t = fa.at(b - o, y1, L_TILE), u = fa.at(a - o, y1, L_TILE);
				state.picture.add(new float[] {p[0], p[1], p[2], ua / TOWER_W, v0 / TOWER_H, s[0], s[1], s[2], ub / TOWER_W, v0 / TOWER_H, t[0], t[1], t[2],
						ub / TOWER_W, v1 / TOWER_H, u[0], u[1], u[2], ua / TOWER_W, v1 / TOWER_H, Float.intBitsToFloat(color), fa.n.x, fa.n.y, fa.n.z});
			}
		}

		/** The cabinets' seams, faintly over everything: 30 columns of 22 cabinets. */
		void seams() {
			int seam = 0x2E000000;
			for (int i = 1; i < COLS; i++) {
				rect(state.seams, i * FACET_PX - 1, 0, i * FACET_PX + 1, H, seam, L_SEAM);
			}
			for (int j = 1; j < ROWS; j++) {
				float y = j * (float) H / ROWS;
				rect(state.seams, 0, y - 1, W, y + 1, seam, L_SEAM);
			}
		}
	}

	// ------------------------------------------------------------------ live state kept per wall (client side)

	/** What a wall has seen: alerts and when each began, drones going out and coming home, power and cargo history. */
	static final class Memory {
		final LinkedHashMap<String, long[]> alerts = new LinkedHashMap<>();
		final Map<String, Integer> alertColor = new HashMap<>();
		final List<String> events = new ArrayList<>();
		final List<Integer> eventColor = new ArrayList<>();
		final Map<Integer, String> out = new HashMap<>();
		boolean seenFlights;
		long firstSeen = -1, lastSample = Long.MIN_VALUE, lastRate = Long.MIN_VALUE;
		/** Battery fraction and draw every 250 ticks (one in-game day), oldest first. */
		final List<float[]> power = new ArrayList<>();
		long rateEnergy, rateItems, rateTime;
		double energyRate, itemRate;
	}

	private static final Map<BlockPos, Memory> MEMORY = new HashMap<>();

	// ------------------------------------------------------------------ the live board

	@Override
	public void extractRenderState(DepotDisplayBlockEntity wall, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(wall, state, crumbling);
		state.aim = -1;
		state.board.clear();
		state.seams.clear();
		state.picture.clear();
		state.texts.clear();
		BlockState block = wall.getBlockState();
		Level level = wall.getLevel();
		Facet[] f = facets();
		if (!(block.getBlock() instanceof CurvedVideoWallBlock) || level == null || f == null || ConsoleMesh.get(MESH_NAME) == null) {
			return;
		}
		state.aim = block.getValue(CurvedVideoWallBlock.AIM);
		double now = level.getGameTime() + partialTick;
		BlockPos link = wall.terminal();
		DepotView view = link != null && level.getBlockEntity(link) instanceof DroneTerminalBlockEntity terminal ? terminal.view() : null;
		TowerCoreBlockEntity core = view != null && view.towerPos != null && level.getBlockEntity(view.towerPos) instanceof TowerCoreBlockEntity c
				? c : null;
		Canvas canvas = new Canvas(f, state, font);
		Memory memory = MEMORY.computeIfAbsent(wall.getBlockPos().immutable(), p -> new Memory());
		new Painter(canvas, view, core, link, memory, now, level.getOverworldClockTime(), level.getGameTime()).draw(block.getValue(CurvedVideoWallBlock.BOARD));
		canvas.seams();
	}

	/** One frame of a board. */
	private static final class Painter {
		final Canvas c;
		final DepotView view;
		final TowerCoreBlockEntity core;
		final BlockPos link;
		final Memory memory;
		final double now;
		final long gameTime;
		final String clock, hhmm;
		final long dayNumber, dayTime;
		final int fleet, airborne;

		Painter(Canvas c, DepotView view, TowerCoreBlockEntity core, BlockPos link, Memory memory, double now, long day, long gameTime) {
			this.c = c;
			this.view = view;
			this.core = core;
			this.link = link;
			this.memory = memory;
			this.now = now;
			this.gameTime = gameTime;
			this.dayTime = day;
			long t = Math.floorMod(day + 6000, 24000);
			long h = t / 1000, m = t % 1000 * 60 / 1000, s = t % 1000 * 3600 / 1000 % 60;
			this.clock = String.format(Locale.ROOT, "%02d:%02d:%02d", h, m, s);
			this.hhmm = String.format(Locale.ROOT, "%02d:%02d", h, m);
			this.dayNumber = Math.floorDiv(day, 24000) + 1;
			this.fleet = view == null ? 0 : view.docks.size() + view.flights.size();
			this.airborne = view == null ? 0 : view.airborne(now);
		}

		void draw(CurvedVideoWallBlock.Board board) {
			String title = switch (board) {
				case OPERATIONS -> "TRI-PILLAR \u00B7 OPERATIONS";
				case FLEET -> "FLEET BOARD";
				case LOGISTICS -> "LOGISTICS";
			};
			if (view == null) {
				header(title, "NO DEPOT LINK");
				c.text("NO DEPOT LINK", W / 2f, 430, RED, 2, 7f);
				c.text("Place the wall within 32 blocks of a drone terminal, or build it into a Drone Tower", W / 2f, 520, DIM, 2, 2.6f);
				double pulse = 0.5 + 0.5 * Math.sin(now * 0.08);
				c.rect(W / 2f - 300, 590, W / 2f - 300 + (float) (600 * pulse), 596, AMBER, L_TILE);
				return;
			}
			remember();
			switch (board) {
				case OPERATIONS -> operations(title);
				case FLEET -> fleetBoard(title);
				case LOGISTICS -> logistics(title);
			}
		}

		// -------------------------------------------------------------- shared pieces

		void header(String left, String middle) {
			c.rect(0, 0, W, H, BG, L_BG);
			c.rect(0, 0, W, 62, HEADER, L_PANEL);
			c.rect(0, 62, W, 66, AMBER, L_PANEL);
			c.text(left, 24, 32, AMBER_HI, 0, 3.4f);
			c.text(middle, W / 2f, 32, TEXT, 2, 2.6f);
			c.text(clock + "   DAY " + dayNumber, W - 24, 32, CLOCK, 1, 3.0f);
		}

		void panel(float x0, float y0, float x1, float y1, String title) {
			c.box(x0, y0, x1, y1, 2, EDGE, L_PANEL);
			c.rect(x0 + 2, y0 + 2, x1 - 2, y1 - 2, PANEL, L_PANEL);
			c.rect(x0 + 2, y0 + 2, x1 - 2, y0 + 34, PANEL_HEAD, L_TILE);
			c.rect(x0 + 2, y0 + 34, x1 - 2, y0 + 36, AMBER, L_TILE);
			c.text(title, x0 + 14, y0 + 18, TITLE, 0, 2.3f);
		}

		void ticker(List<String> items) {
			c.rect(0, H - 70, W, H, TICKER_BG, L_PANEL);
			c.rect(0, H - 70, W, H - 67, TICKER_LINE, L_TILE);
			if (items.isEmpty()) {
				return;
			}
			float scale = 2.6f, gap = 60, mark = 14;
			float period = 0;
			for (String s : items) {
				period += mark + 12 + c.width(s, scale) + gap;
			}
			float shift = (float) (now * 1.6 % period);
			float x = 20 - shift;
			while (x < W) {
				for (String s : items) {
					float w = mark + 12 + c.width(s, scale) + gap;
					if (x + w > 0 && x < W) {
						c.rect(Math.max(0, x), H - 42, Math.min(W, x + mark), H - 28, AMBER, L_MARK);
						c.text(s, x + mark + 12, H - 35, AMBER, 0, scale);
					}
					x += w;
					if (x >= W) {
						break;
					}
				}
			}
		}

		void bar(float x0, float y0, float x1, float y1, float fraction, int color) {
			c.rect(x0, y0, x1, y1, TRACK, L_MARK);
			c.rect(x0, y0, x0 + (x1 - x0) * Math.max(0, Math.min(1, fraction)), y1, color, L_TOP);
		}

		int pct() {
			return view.capacity <= 0 ? 0 : (int) (view.energy * 100 / view.capacity);
		}

		String n(long v) {
			String s = Long.toString(Math.abs(v));
			StringBuilder b = new StringBuilder();
			for (int i = 0; i < s.length(); i++) {
				if (i > 0 && (s.length() - i) % 3 == 0) {
					b.append(' ');
				}
				b.append(s.charAt(i));
			}
			return (v < 0 ? "-" : "") + b;
		}

		String tierName(int tier) {
			return tier >= 1 && tier <= TIER_NAMES.length ? TIER_NAMES[tier - 1] : "";
		}

		/** The drone tier's model (its item's name less " Drone"). */
		String model(int tier) {
			String s = Component.translatable("item.jugcraft.drone_t" + Math.max(1, Math.min(9, tier))).getString();
			return s.endsWith(" Drone") ? s.substring(0, s.length() - 6) : s;
		}

		String droneName(DepotView.FlightView flight) {
			return String.format(Locale.ROOT, "T%d-%02d", flight.tier(), flight.drone() + 1);
		}

		/** Where a flight's last drop is, from the tower (or the terminal): "312 m NE". */
		String job(DepotView.FlightView flight) {
			FlightPath path = flight.path();
			int k = Math.max(0, path.points() - 2);
			double x = link.getX() + path.x(k), z = link.getZ() + path.z(k);
			double ox = core != null ? core.getBlockPos().getX() + 0.5 : link.getX() + 0.5, oz = core != null ? core.getBlockPos().getZ() + 0.5
					: link.getZ() + 0.5;
			double dx = x - ox, dz = z - oz;
			String[] names = {"E", "SE", "S", "SW", "W", "NW", "N", "NE"};
			double a = Math.toDegrees(Math.atan2(dz, dx));
			int i = (int) Math.floorMod(Math.round(a / 45), 8L);
			return n(Math.round(Math.hypot(dx, dz))) + " m " + names[i];
		}

		/** What a flight is doing now, and its colour. */
		Object[] phase(DepotView.FlightView flight) {
			FlightPath path = flight.path();
			double t = flight.ticksAt(now, view.syncTime);
			if (t >= path.totalTicks() - FlightScheduler.RECHARGE_TICKS) {
				return new Object[] {"CHARGING", GREEN};
			}
			if (t < path.pickupArrive()) {
				return new Object[] {"TO PICKUP", BLUE};
			}
			if (t < path.pickupLeave()) {
				return new Object[] {"LOADING", AMBER_HI};
			}
			if (t < path.lastDrop()) {
				return new Object[] {"CARRYING", AMBER};
			}
			return new Object[] {"RETURNING", BLUE};
		}

		String eta(DepotView.FlightView flight) {
			double left = (flight.path().totalTicks() - flight.ticksAt(now, view.syncTime)) / 20.0;
			long s = Math.max(0, Math.round(left));
			return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
		}

		/** The alerts now, each with its colour (the console's ALERTS page's list). */
		LinkedHashMap<String, Integer> alertsNow() {
			LinkedHashMap<String, Integer> out = new LinkedHashMap<>();
			if (view.lowPower) {
				out.put("Low power, battery at " + pct() + " %", RED);
			}
			if (!view.packager) {
				out.put("No cargo packager", RED);
			}
			if (!view.missing.isEmpty()) {
				out.put("Short of " + view.missing.size() + (view.missing.size() == 1 ? " kind" : " kinds") + " of material", AMBER);
			}
			if (view.pickups.isEmpty()) {
				out.put("No supply pickup", AMBER);
			}
			if (core != null && core.converting()) {
				out.put("Tower being refitted", AMBER);
			} else if (core != null && core.buildingFoundation()) {
				out.put("Foundation building", AMBER);
			} else if (core != null && core.building() > 0) {
				out.put("Tier " + core.building() + " building", AMBER);
			}
			if (fleet >= view.maxDrones && view.maxDrones > 0) {
				out.put("Fleet full, " + fleet + " drones", AMBER);
			}
			if (!view.flights.isEmpty()) {
				out.put(view.flights.size() + (view.flights.size() == 1 ? " drone out" : " drones out"), GREEN);
			}
			return out;
		}

		/** Keeps the wall's memory: when each alert began, drones going out and home, the power and cargo history. */
		void remember() {
			Memory m = memory;
			if (m.firstSeen < 0) {
				m.firstSeen = dayTime;
			}
			LinkedHashMap<String, Integer> nowAlerts = alertsNow();
			m.alerts.keySet().retainAll(nowAlerts.keySet());
			for (Map.Entry<String, Integer> e : nowAlerts.entrySet()) {
				m.alerts.putIfAbsent(e.getKey(), new long[] {dayTime});
				m.alertColor.put(e.getKey(), e.getValue());
			}
			Map<Integer, String> current = new HashMap<>();
			for (DepotView.FlightView flight : view.flights) {
				current.put(flight.drone(), droneName(flight));
				if (m.seenFlights && !m.out.containsKey(flight.drone())) {
					event(droneName(flight) + " OUT  " + job(flight), BLUE);
				}
			}
			if (m.seenFlights) {
				for (Map.Entry<Integer, String> e : m.out.entrySet()) {
					if (!current.containsKey(e.getKey())) {
						event(e.getValue() + " HOME", GREEN);
					}
				}
			}
			m.out.clear();
			m.out.putAll(current);
			m.seenFlights = true;
			if (gameTime - m.lastSample >= 250 || gameTime < m.lastSample) {
				m.lastSample = gameTime;
				m.power.add(new float[] {view.capacity <= 0 ? 0 : (float) view.energy / view.capacity, view.draw()});
				while (m.power.size() > 96) {
					m.power.remove(0);
				}
			}
			if (gameTime - m.lastRate >= 100 || gameTime < m.lastRate) {
				long items = view.cargo == null ? 0 : Math.max(0, view.cargo.items());
				if (m.lastRate != Long.MIN_VALUE && gameTime > m.rateTime) {
					double dt = gameTime - m.rateTime;
					m.energyRate = (view.energy - m.rateEnergy) / dt;
					m.itemRate = (items - m.rateItems) / dt * 1200;
				}
				m.lastRate = gameTime;
				m.rateTime = gameTime;
				m.rateEnergy = view.energy;
				m.rateItems = items;
			}
		}

		void event(String text, int color) {
			memory.events.add(0, text);
			memory.eventColor.add(0, color);
			while (memory.events.size() > 8) {
				memory.events.remove(memory.events.size() - 1);
				memory.eventColor.remove(memory.eventColor.size() - 1);
			}
		}

		String at(long day) {
			long t = Math.floorMod(day + 6000, 24000);
			return String.format(Locale.ROOT, "%02d:%02d", t / 1000, t % 1000 * 60 / 1000);
		}

		// -------------------------------------------------------------- OPERATIONS

		String towerLine() {
			if (core == null) {
				return "NO TOWER";
			}
			int pct = Math.round(core.buildProgress() * 100);
			if (core.converting()) {
				return "REFITTING  " + pct + " %";
			}
			if (core.buildingFoundation()) {
				return "BUILDING THE FOUNDATION  " + pct + " %";
			}
			String line = core.tier() >= 1 ? "TIER " + core.tier() + " " + tierName(core.tier()) : "FOUNDATION";
			if (core.building() > 0) {
				return line + "  \u2014  BUILDING TIER " + core.building() + "  " + pct + " %";
			}
			return line + (core.tier() >= TowerData.TIERS ? "  \u2014  ALL TIERS BUILT" : "");
		}

		void operations(String title) {
			header(title, towerLine());
			panel(16, 84, 820, H - 86, "SITE");
			site(16, 84, 820, H - 86);
			panel(836, 84, 1560, H - 86, "TOWER");
			tower(836, 84, 1560, H - 86);
			panel(1576, 84, W - 16, 560, "STATUS");
			int jobs = view.openTargets + view.covered;
			tiles(1576, 84, W - 16, 560, new Object[][] {
					{"FLEET", fleet + " / " + view.maxDrones, "T1-T" + view.maxTier, view.maxDrones <= 0 ? 0f : (float) fleet / view.maxDrones, AMBER_HI},
					{"AIRBORNE", String.valueOf(airborne), "of " + fleet, fleet <= 0 ? 0f : (float) airborne / fleet, BLUE},
					{"JOBS OPEN", n(view.openTargets), view.pickups.size() + (view.pickups.size() == 1 ? " pickup" : " pickups"), null, AMBER},
					{"COVERED", (jobs <= 0 ? 0 : view.covered * 100 / jobs) + " %", n(view.covered) + " done", jobs <= 0 ? 0f : (float) view.covered / jobs,
							GREEN}});
			panel(1576, 576, W - 16, H - 86, "ALERTS");
			alerts(1576, 576, W - 16, H - 86);
			List<String> items = new ArrayList<>();
			items.add(towerLine());
			items.add(airborne + (airborne == 1 ? " DRONE AIRBORNE" : " DRONES AIRBORNE"));
			items.add("FLEET " + fleet + " / " + view.maxDrones);
			items.add("BATTERY " + pct() + " %");
			items.add("JOBS " + n(view.openTargets) + " OPEN");
			items.add(view.missing.isEmpty() ? "SUPPLY OK" : "SHORT " + view.missing.size() + " KINDS");
			if (!memory.events.isEmpty()) {
				items.add(memory.events.get(0));
			}
			ticker(items);
		}

		/** The site from above: the hall, pillars and pads, the drones docked and out (with their tracks). */
		void site(float x0, float y0, float x1, float y1) {
			float cx = (x0 + x1) / 2, cy = (y0 + 36 + y1) / 2 + 6;
			float size = Math.min(x1 - x0, y1 - y0 - 36) - 70;
			double ox = core != null ? core.getBlockPos().getX() + 0.5 : link.getX() + 0.5;
			double oz = core != null ? core.getBlockPos().getZ() + 0.5 : link.getZ() + 0.5;
			double far = 60;
			for (DepotView.FlightView flight : view.flights) {
				FlightPath.Pose pose = flight.path().poseAt(flight.ticksAt(now, view.syncTime));
				double dx = link.getX() + pose.x() - ox, dz = link.getZ() + pose.z() - oz;
				far = Math.max(far, Math.hypot(dx, dz) * 1.08);
			}
			double[] ranges = {60, 120, 250, 500, 1000, 2000, 4000, 8000};
			double range = ranges[ranges.length - 1];
			for (double r : ranges) {
				if (far <= r) {
					range = r;
					break;
				}
			}
			float k = (float) (size / (2 * range));
			double step = range / 6;
			for (int g = -6; g <= 6; g++) {
				float gx = cx + (float) (g * step) * k, gy = cy + (float) (g * step) * k;
				c.line(gx, y0 + 40, gx, y1 - 6, 1.5f, GRID, L_TILE);
				c.line(x0 + 6, gy, x1 - 6, gy, 1.5f, GRID, L_TILE);
			}
			// the hall's hexagon (flat to north and south), the rotunda, the pillar feet, the roof pads
			float[] hx = new float[7], hz = new float[7];
			for (int i = 0; i <= 6; i++) {
				double a = Math.toRadians(60 * i);
				hx[i] = cx + (float) (52 * Math.cos(a)) * k;
				hz[i] = cy + (float) (52 * Math.sin(a)) * k;
			}
			c.polyline(hx, hz, 7, Math.max(2f, Math.min(4f, 52 * k * 0.08f)), AMBER, L_MARK);
			ring(cx, cy, 15 * k, 2, 0xFF785A32);
			int[][] pillars = {{0, 17}, {15, -8}, {-15, -8}};
			for (int[] p : pillars) {
				octagon(cx + (p[0]) * k, cy + (p[1]) * k, 9.5f * k, 3 * k, 0xFFFFAA46, 0xFF3C2812);
			}
			int[][] pads = {{22, 24}, {33, 5}, {8, -38}, {-14, -38}, {-39, 5}, {-28, 24}};
			for (int[] p : pads) {
				float px = cx + (p[0] + 3) * k, pz = cy + (p[1] + 3) * k;
				c.box(px - 3.5f * k, pz - 3.5f * k, px + 3.5f * k, pz + 3.5f * k, 2, 0xFFA0C8DC, L_MARK);
			}
			// docked drones
			for (DepotView.DockView dock : view.docks) {
				float x = cx + (float) (link.getX() + dock.x() - ox) * k, y = cy + (float) (link.getZ() + dock.z() - oz) * k;
				if (x > x0 + 6 && x < x1 - 6 && y > y0 + 40 && y < y1 - 6) {
					c.rect(x - 3, y - 3, x + 3, y + 3, 0xFFE8ECF0, L_TOP);
				}
			}
			// drones out: a track behind each, the drone a dot (amber while it carries a crate); off the map, at its edge
			for (DepotView.FlightView flight : view.flights) {
				FlightPath path = flight.path();
				double t = flight.ticksAt(now, view.syncTime);
				FlightPath.Pose pose = path.poseAt(t);
				boolean carrying = pose.cable() > 0 || t >= path.pickupLeave() && t < path.lastDrop();
				int color = carrying ? AMBER : BLUE;
				int trail = carrying ? 0xFF8C6128 : 0xFF3C6E8C;
				float[] xs = new float[16], ys = new float[16];
				int count = 0;
				for (int s = 15; s >= 0; s--) {
					double ts = Math.max(0, t - s * 10);
					FlightPath.Pose p = path.poseAt(ts);
					float x = cx + (float) (link.getX() + p.x() - ox) * k, y = cy + (float) (link.getZ() + p.z() - oz) * k;
					xs[count] = Math.max(x0 + 6, Math.min(x1 - 6, x));
					ys[count] = Math.max(y0 + 40, Math.min(y1 - 6, y));
					count++;
				}
				c.polyline(xs, ys, count, 2.5f, trail, L_MARK);
				float x = cx + (float) (link.getX() + pose.x() - ox) * k, y = cy + (float) (link.getZ() + pose.z() - oz) * k;
				boolean inside = x > x0 + 10 && x < x1 - 10 && y > y0 + 44 && y < y1 - 10;
				x = Math.max(x0 + 10, Math.min(x1 - 10, x));
				y = Math.max(y0 + 44, Math.min(y1 - 10, y));
				c.dot(x, y, inside ? 7 : 5, color, L_TOP);
			}
			// north and the scale
			c.text("N", x1 - 24, y0 + 58, TEXT, 1, 2.3f);
			c.line(x1 - 36, y0 + 84, x1 - 36, y0 + 64, 3, TEXT, L_MARK);
			c.line(x1 - 36, y0 + 62, x1 - 42, y0 + 72, 3, TEXT, L_MARK);
			c.line(x1 - 36, y0 + 62, x1 - 30, y0 + 72, 3, TEXT, L_MARK);
			double bar = range >= 1000 ? range / 4 : range >= 250 ? range / 5 : range * 5 / 12;
			bar = bar >= 1000 ? Math.round(bar / 500) * 500 : bar >= 100 ? Math.round(bar / 50) * 50 : Math.round(bar / 10) * 10;
			c.line(x0 + 20, y1 - 24, x0 + 20 + (float) bar * k, y1 - 24, 3, TEXT, L_MARK);
			c.text(n(Math.round(bar)) + " m", x0 + 28 + (float) bar * k, y1 - 24, DIM, 0, 1.9f);
			if (view.flights.isEmpty()) {
				c.text("ALL DRONES HOME", x0 + 20, y0 + 58, GREEN, 0, 1.9f);
			} else {
				c.text(view.flights.size() + " OUT", x0 + 20, y0 + 58, BLUE, 0, 1.9f);
			}
		}

		void ring(float x, float y, float r, float w, int color) {
			int n = 48;
			for (int i = 0; i < n; i++) {
				double a0 = 2 * Math.PI * i / n, a1 = 2 * Math.PI * (i + 1) / n;
				c.line(x + (float) Math.cos(a0) * r, y + (float) Math.sin(a0) * r, x + (float) Math.cos(a1) * r, y + (float) Math.sin(a1) * r, w, color,
						L_MARK);
			}
		}

		/** A pillar foot: a square with chamfered corners, filled, outlined. */
		void octagon(float x, float y, float h, float ch, int outline, int fill) {
			c.rect(x - h, y - h + ch, x + h, y + h - ch, fill, L_TILE);
			c.rect(x - h + ch, y - h, x + h - ch, y - h + ch, fill, L_TILE);
			c.rect(x - h + ch, y + h - ch, x + h - ch, y + h, fill, L_TILE);
			float[] xs = {x - h + ch, x + h - ch, x + h, x + h, x + h - ch, x - h + ch, x - h, x - h, x - h + ch};
			float[] ys = {y - h, y - h, y - h + ch, y + h - ch, y + h, y + h, y + h - ch, y - h + ch, y - h};
			c.polyline(xs, ys, 9, 2, outline, L_MARK);
		}

		/** The tower by tier: its picture tinted (built, building and how far, still to come), and each tier's line. */
		void tower(float x0, float y0, float x1, float y1) {
			float[] rows = tierRows();
			float ph = y1 - y0 - 70;
			float pw = TOWER_W * ph / TOWER_H;
			float ex = x0 + 40, ey = y0 + 52;
			int built = core == null ? 0 : core.tier();
			int building = core == null ? 0 : core.building();
			boolean foundation = core != null && core.buildingFoundation();
			float progress = core == null ? 0 : core.buildProgress();
			float u0 = towerColumns[0], u1 = towerColumns[1];
			float px0 = ex + pw * u0 / TOWER_W, px1 = ex + pw * u1 / TOWER_W;
			for (int t = 1; t <= 9; t++) {
				float vb = rows[t - 1], vt = t == 9 ? rows[10] : rows[t];
				float yb = ey + ph * vb / TOWER_H, yt = ey + ph * vt / TOWER_H;
				if (t <= built && !foundation) {
					c.picture(px0, yt, px1, yb, u0, vt, u1, vb, AMBER);
				} else if (t == building && !foundation) {
					float vm = vb + (vt - vb) * progress, ym = yb + (yt - yb) * progress;
					c.picture(px0, ym, px1, yb, u0, vm, u1, vb, 0xFFFFE2AA);
					c.picture(px0, yt, px1, ym, u0, vt, u1, vm, 0xFF5A3E1A);
				} else {
					c.picture(px0, yt, px1, yb, u0, vt, u1, vb, 0xFF2E3A46);
				}
			}
			float lx = ex + pw + 40;
			for (int t = 9; t >= 1; t--) {
				float ty = y0 + 70 + (9 - t) * (ph - 30) / 9;
				boolean done = t <= built && !foundation;
				boolean current = t == building && !foundation;
				int color = done ? AMBER : current ? AMBER_HI : DIM;
				c.text(String.valueOf(t), lx, ty, color, 0, 3.0f);
				c.text(tierName(t), lx + 40, ty, color, 0, 2.2f);
				if (done) {
					c.line(x1 - 52, ty + 1, x1 - 42, ty + 10, 4, AMBER, L_MARK);
					c.line(x1 - 43, ty + 10, x1 - 26, ty - 10, 4, AMBER, L_MARK);
				} else if (current) {
					c.text(Math.round(progress * 100) + " %", x1 - 24, ty, AMBER_HI, 1, 2.4f);
					bar(lx + 40, ty + 18, x1 - 24, ty + 24, progress, AMBER);
				} else {
					c.rect(x1 - 46, ty - 2, x1 - 30, ty + 2, DIM, L_MARK);
				}
			}
			if (foundation) {
				c.text("FOUNDATION " + Math.round(progress * 100) + " %", ex + pw / 2, y1 - 24, AMBER_HI, 2, 2.2f);
			}
		}

		/** Four status tiles, two by two: label, value, a note, a bar. */
		void tiles(float x0, float y0, float x1, float y1, Object[][] tiles) {
			float cw = (x1 - x0 - 30) / 2, ch = (y1 - y0 - 46 - 20) / 2;
			for (int i = 0; i < tiles.length; i++) {
				Object[] t = tiles[i];
				float tx = x0 + 10 + (i % 2) * (cw + 10), ty = y0 + 46 + (i / 2) * (ch + 10);
				c.box(tx, ty, tx + cw, ty + ch, 2, EDGE, L_TILE);
				c.rect(tx + 2, ty + 2, tx + cw - 2, ty + ch - 2, 0xFF10161D, L_TILE);
				int color = (Integer) t[4];
				c.text((String) t[0], tx + 16, ty + 24, DIM, 0, 1.9f);
				c.text((String) t[1], tx + 16, ty + ch * 0.52f, color, 0, 5.2f);
				c.text((String) t[2], tx + cw - 16, ty + ch * 0.52f + 6, DIM, 1, 1.9f);
				if (t[3] != null) {
					bar(tx + 16, ty + ch - 26, tx + cw - 16, ty + ch - 18, (Float) t[3], color);
				}
			}
		}

		void alerts(float x0, float y0, float x1, float y1) {
			List<Map.Entry<String, long[]>> list = new ArrayList<>(memory.alerts.entrySet());
			java.util.Collections.reverse(list);
			int row = 0;
			for (Map.Entry<String, long[]> e : list) {
				if (row >= 7) {
					break;
				}
				float y = y0 + 60 + row * 38;
				c.text(at(e.getValue()[0]), x0 + 16, y, DIM, 0, 2.1f);
				c.rect(x0 + 100, y - 9, x0 + 112, y + 9, memory.alertColor.getOrDefault(e.getKey(), AMBER), L_MARK);
				c.text(e.getKey(), x0 + 126, y, TEXT, 0, 2.1f);
				row++;
			}
			if (row < 7) {
				float y = y0 + 60 + row * 38;
				c.text(at(memory.firstSeen), x0 + 16, y, DIM, 0, 2.1f);
				c.rect(x0 + 100, y - 9, x0 + 112, y + 9, DIM, L_MARK);
				c.text(list.isEmpty() ? "All systems normal" : "Watch began", x0 + 126, y, list.isEmpty() ? GREEN : DIM, 0, 2.1f);
			}
		}

		// -------------------------------------------------------------- FLEET BOARD

		void fleetBoard(String title) {
			int home = view.docks.size();
			header(title, fleet + " DRONES  \u00B7  " + airborne + " AIRBORNE  \u00B7  " + home + " HOME");
			panel(16, 84, 1500, H - 86, "FLIGHTS");
			String[] names = {"DRONE", "TIER", "MODEL", "GOING TO", "STATE", "HOME IN", "FLIGHT"};
			float[] cols = {46, 236, 346, 656, 996, 1180, 1300};
			for (int i = 0; i < names.length; i++) {
				c.text(names[i], cols[i], 140, DIM, 0, 2.0f);
			}
			List<DepotView.FlightView> flights = new ArrayList<>(view.flights);
			flights.sort((a, b) -> Double.compare(a.path().totalTicks() - a.ticksAt(now, view.syncTime), b.path().totalTicks() - b.ticksAt(now, view.syncTime)));
			int rows = 17;
			// the flights, soonest home first; the last row says how many more when they don't all fit; then drones at home
			boolean over = flights.size() > rows;
			int flightRows = over ? rows - 1 : flights.size();
			int homeRows = over ? 0 : Math.min(Math.min(home, 4), rows - flightRows);
			for (int r = 0; r < rows; r++) {
				float y = 178 + r * 40;
				if (r % 2 == 1) {
					c.rect(20, y - 18, 1496, y + 18, STRIPE, L_TILE);
				}
				if (r < flightRows) {
					DepotView.FlightView flight = flights.get(r);
					Object[] phase = phase(flight);
					c.text(droneName(flight), cols[0], y, TEXT, 0, 2.2f);
					c.text("T" + flight.tier(), cols[1], y, AMBER, 0, 2.2f);
					c.text(model(flight.tier()), cols[2], y, TEXT, 0, 2.2f);
					c.text(job(flight), cols[3], y, TEXT, 0, 2.2f);
					c.text((String) phase[0], cols[4], y, (Integer) phase[1], 0, 2.0f);
					c.text(eta(flight), cols[5], y, TEXT, 0, 2.2f);
					double t = flight.ticksAt(now, view.syncTime);
					float done = (float) (t / Math.max(1, flight.path().totalTicks()));
					bar(cols[6], y - 7, 1476, y + 7, done, done > 0.85f ? GREEN : BLUE);
				} else if (over && r == flightRows) {
					c.text("+ " + (flights.size() - flightRows) + " more out", cols[0], y, DIM, 0, 2.1f);
				} else if (r - flightRows < homeRows) {
					DepotView.DockView dock = view.docks.get(r - flightRows);
					c.text("T" + dock.tier() + " \u00B7 home", cols[0], y, DIM, 0, 2.2f);
					c.text("T" + dock.tier(), cols[1], y, AMBER_DIM, 0, 2.2f);
					c.text(model(dock.tier()), cols[2], y, DIM, 0, 2.2f);
					c.text("\u2014", cols[3], y, DIM, 0, 2.2f);
					c.text("DOCKED", cols[4], y, DIM, 0, 2.0f);
					c.text("\u2014", cols[5], y, DIM, 0, 2.2f);
					bar(cols[6], y - 7, 1476, y + 7, 1, 0xFF3C5A46);
				}
			}
			if (flights.isEmpty() && home == 0) {
				c.text("No drones yet", cols[0], 178, DIM, 0, 2.2f);
			}
			panel(1516, 84, W - 16, H - 86, "BAYS");
			bays(1516, 84, W - 16, H - 86);
			List<String> items = new ArrayList<>(memory.events);
			items.add("FLEET " + fleet + " / " + view.maxDrones);
			items.add(airborne + " AIRBORNE");
			ticker(items);
		}

		/** Every hangar bay of each pillar, home (green), out (amber) or empty; the roof pads' count. */
		void bays(float x0, float y0, float x1, float y1) {
			List<List<Integer>> pillars = new ArrayList<>();
			for (int p = 0; p < 3; p++) {
				pillars.add(new ArrayList<>());
			}
			for (int[] d : view.doors) {
				int pillar = d.length > 10 ? d[10] : -1;
				int status = d.length > 12 ? d[12] : 0;
				if (pillar >= 0 && pillar < 3) {
					pillars.get(pillar).add(status);
				}
			}
			String[] names = {"PILLAR A", "PILLAR B", "PILLAR C"};
			float pitch = 41, sq = 34;
			for (int p = 0; p < 3; p++) {
				List<Integer> bays = pillars.get(p);
				float gy = y0 + 66 + p * 236;
				int home = 0, out = 0;
				for (int s : bays) {
					if (s == 1) home++;
					else if (s == 2) out++;
				}
				c.text(names[p], x0 + 24, gy, AMBER, 0, 2.2f);
				c.text(bays.isEmpty() ? "no bays yet" : home + " home  \u00B7  " + out + " out  \u00B7  " + (bays.size() - home - out) + " empty",
						x1 - 24, gy, DIM, 1, 1.9f);
				int per = 20;
				int rowsNeeded = Math.max(1, (bays.size() + per - 1) / per);
				float s = rowsNeeded <= 4 ? sq : sq * 4 / rowsNeeded, pt = rowsNeeded <= 4 ? pitch : pitch * 4 / rowsNeeded;
				for (int i = 0; i < bays.size(); i++) {
					float bx = x0 + 24 + (i % per) * pitch, by = gy + 22 + (i / per) * (pt + 4);
					int status = bays.get(i);
					int color = status == 1 ? GREEN : status == 2 ? AMBER : EMPTY;
					c.rect(bx, by, bx + sq, by + s, color, L_MARK);
				}
			}
			float ly = y1 - 34;
			c.rect(x0 + 24, ly - 10, x0 + 44, ly + 10, GREEN, L_MARK);
			c.text("HOME", x0 + 54, ly, DIM, 0, 2.0f);
			c.rect(x0 + 160, ly - 10, x0 + 180, ly + 10, AMBER, L_MARK);
			c.text("OUT", x0 + 190, ly, DIM, 0, 2.0f);
			c.rect(x0 + 280, ly - 10, x0 + 300, ly + 10, EMPTY, L_MARK);
			c.text("EMPTY", x0 + 310, ly, DIM, 0, 2.0f);
			c.text("ROOF PADS " + view.towerPads, x1 - 24, ly, DIM, 1, 2.0f);
		}

		// -------------------------------------------------------------- LOGISTICS

		String itemName(String id) {
			Identifier key = Identifier.tryParse(id);
			if (key != null && BuiltInRegistries.ITEM.containsKey(key)) {
				return new ItemStack(BuiltInRegistries.ITEM.getValue(key)).getHoverName().getString();
			}
			String name = id.substring(id.indexOf(':') + 1).replace('_', ' ');
			return name.isEmpty() ? id : Character.toUpperCase(name.charAt(0)) + name.substring(1);
		}

		void logistics(String title) {
			DepotView.CargoView cargo = view.cargo;
			int items = cargo == null ? 0 : Math.max(0, cargo.items()), occupied = cargo == null ? 0 : Math.max(0, cargo.occupiedSlots()),
					total = cargo == null ? 0 : Math.max(0, cargo.totalSlots());
			header(title, (cargo != null && cargo.unlimited() ? "TEST STORE" : "SUPPLY " + n(items) + " ITEMS") + "  \u00B7  " + occupied + " / " + total
					+ " SLOTS  \u00B7  BATTERY " + pct() + " %");
			panel(16, 84, 900, H - 86, "SUPPLY");
			supply(16, 84, 900, H - 86);
			panel(916, 84, 1700, 560, "POWER  \u00B7  LAST DAY");
			power(916, 84, 1700, 560);
			panel(916, 576, 1700, H - 86, "EXCHANGES");
			exchanges(916, 576, 1700, H - 86, items);
			panel(1716, 84, W - 16, H - 86, "SHORT OF");
			shortOf(1716, 84, W - 16, H - 86);
			List<String> ticker = new ArrayList<>();
			ticker.add("STORE " + n(items) + " ITEMS");
			ticker.add("SLOTS " + occupied + " / " + total);
			ticker.add("BATTERY " + pct() + " %");
			int k = 0;
			for (Map.Entry<String, Integer> e : view.missing.entrySet()) {
				if (k++ >= 3) {
					break;
				}
				ticker.add(itemName(e.getKey()).toUpperCase(Locale.ROOT) + " SHORT " + n(e.getValue()));
			}
			ticker.add("CARGO " + (memory.itemRate >= 0 ? "+" : "") + n(Math.round(memory.itemRate)) + " / MIN");
			ticker(ticker);
		}

		void supply(float x0, float y0, float x1, float y1) {
			if (!view.packager || view.cargo == null || view.cargo.items() < 0) {
				c.text("No supply store linked", (x0 + x1) / 2, (y0 + y1) / 2, DIM, 2, 2.6f);
				return;
			}
			if (view.stock.isEmpty()) {
				c.text(view.cargo.unlimited() ? "Test store: everything, without end" : "The store is empty", (x0 + x1) / 2, (y0 + y1) / 2, DIM, 2, 2.6f);
				return;
			}
			int most = 1;
			for (int v : view.stock.values()) {
				most = Math.max(most, v);
			}
			int i = 0;
			for (Map.Entry<String, Integer> e : view.stock.entrySet()) {
				float y = y0 + 66 + i * 56;
				String name = itemName(e.getKey());
				while (name.length() > 4 && c.width(name, 2.2f) > 300) {
					name = name.substring(0, name.length() - 2).stripTrailing() + "\u2026";
				}
				c.text(name, x0 + 24, y, TEXT, 0, 2.2f);
				c.rect(x0 + 344, y - 12, x0 + 344 + 440f * e.getValue() / most, y + 12, i % 2 == 0 ? AMBER : AMBER_DIM, L_MARK);
				c.text(n(e.getValue()), x1 - 20, y, AMBER_HI, 1, 2.2f);
				i++;
			}
		}

		void power(float x0, float y0, float x1, float y1) {
			float gx0 = x0 + 24, gx1 = x1 - 20, gy0 = y0 + 60, gy1 = y1 - 70;
			for (int g = 0; g <= 4; g++) {
				float y = gy0 + (gy1 - gy0) * g / 4;
				c.line(gx0, y, gx1, y, 1.5f, GRID, L_TILE);
				c.text((100 - g * 25) + "", gx0 - 4, y, 0xFF4E5A66, 1, 1.5f);
			}
			List<float[]> history = new ArrayList<>(memory.power);
			history.add(new float[] {view.capacity <= 0 ? 0 : (float) view.energy / view.capacity, view.draw()});
			float most = 1;
			for (float[] h : history) {
				most = Math.max(most, h[1]);
			}
			int n = history.size();
			float dx = (gx1 - gx0) / 96;
			float[] bx = new float[n], by = new float[n], dy = new float[n];
			for (int i = 0; i < n; i++) {
				bx[i] = gx1 - (n - 1 - i) * dx;
				by[i] = gy1 - (gy1 - gy0) * history.get(i)[0];
				dy[i] = gy1 - (gy1 - gy0) * 0.9f * history.get(i)[1] / most;
			}
			if (n >= 2) {
				c.polyline(bx, dy, n, 3, BLUE, L_MARK);
				c.polyline(bx, by, n, 4, AMBER, L_TOP);
			}
			c.dot(bx[n - 1], by[n - 1], 6, AMBER_HI, L_TOP);
			c.text(pct() + " %", x1 - 20, y0 + 18, AMBER_HI, 1, 2.3f);
			c.text(n(view.draw()) + " JE/t", x1 - 130, y0 + 18, BLUE, 1, 2.0f);
			float ly = y1 - 34;
			c.rect(gx0, ly - 2, gx0 + 30, ly + 2, AMBER, L_MARK);
			c.text("battery", gx0 + 40, ly, DIM, 0, 2.0f);
			c.rect(gx0 + 180, ly - 2, gx0 + 210, ly + 2, BLUE, L_MARK);
			c.text("draw", gx0 + 220, ly, DIM, 0, 2.0f);
			c.text("load " + view.loadPercent() + " %", x1 - 20, ly, DIM, 1, 2.0f);
		}

		void exchanges(float x0, float y0, float x1, float y1, int items) {
			double e = memory.energyRate, it = memory.itemRate;
			String energyState = view.lowPower ? "LOW" : pct() >= 99 ? "FULL" : e > 0.5 ? "CHARGING" : e < -0.5 ? "DRAINING" : "STEADY";
			int energyColor = view.lowPower ? RED : pct() >= 99 || e > 0.5 ? GREEN : e < -0.5 ? AMBER : DIM;
			String cargoState = view.cargo == null ? "NO STORE" : it > 0.5 ? "IN" : it < -0.5 ? "OUT" : "STEADY";
			int cargoColor = view.cargo == null ? DIM : it > 0.5 ? GREEN : it < -0.5 ? AMBER : DIM;
			Object[][] rows = {
					{"ENERGY EXCHANGE", energyState, energyColor, "NET " + (e >= 0 ? "+" : "") + n(Math.round(e)) + " JE/t   \u00B7   BATTERY " + pct() + " %"},
					{"CARGO EXCHANGE", cargoState, cargoColor, "NET " + (it >= 0 ? "+" : "") + n(Math.round(it)) + " / MIN   \u00B7   STORE " + n(items)}};
			for (int i = 0; i < rows.length; i++) {
				float y = y0 + 64 + i * 112;
				c.box(x0 + 20, y - 30, x1 - 20, y + 70, 2, EDGE, L_TILE);
				c.text((String) rows[i][0], x0 + 40, y, TITLE, 0, 2.6f);
				c.text((String) rows[i][1], x1 - 40, y, (Integer) rows[i][2], 1, 2.4f);
				c.text((String) rows[i][3], x0 + 40, y + 40, AMBER_HI, 0, 2.2f);
			}
		}

		void shortOf(float x0, float y0, float x1, float y1) {
			if (view.missing.isEmpty()) {
				c.text("NOTHING SHORT", (x0 + x1) / 2, (y0 + y1) / 2, GREEN, 2, 3.0f);
				return;
			}
			int i = 0, rows = 12;
			for (Map.Entry<String, Integer> e : view.missing.entrySet()) {
				if (i == rows - 1 && view.missing.size() > rows) {
					c.text("+ " + (view.missing.size() - i) + " more kinds", x0 + 24, y0 + 66 + i * 60, DIM, 0, 2.2f);
					break;
				}
				float y = y0 + 66 + i * 60;
				String name = itemName(e.getKey());
				while (name.length() > 3 && c.width(name, 2.4f) > (x1 - x0) - 170) {
					name = name.substring(0, name.length() - 2).stripTrailing() + "\u2026";
				}
				c.text(name, x0 + 24, y, AMBER, 0, 2.4f);
				c.text(n(e.getValue()), x1 - 24, y, AMBER, 1, 2.4f);
				i++;
			}
		}
	}

	// ------------------------------------------------------------------ drawing

	@Override
	public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
		ConsoleMesh mesh = ConsoleMesh.get(MESH_NAME);
		Facet[] f = facets();
		if (mesh == null || f == null || state.aim < 0) {
			return;
		}
		int[] v = CurvedVideoWallBlock.AIMS[state.aim];
		poses.pushPose();
		poses.translate(v[0] + 0.5, 0, v[1] + 0.5);
		poses.mulPose(new Matrix4f().rotationY((float) (Math.PI / 2 - CurvedVideoWallBlock.centre(state.aim))));
		int light = state.lightCoords;
		collector.submitCustomGeometry(poses, MESH, (pose, consumer) -> {
			DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
			ConsoleMesh.emit(g, mesh.quads[ConsoleMesh.SOLID], light, false);
			ConsoleMesh.emit(g, mesh.quads[ConsoleMesh.LIT], LightCoordsUtil.FULL_BRIGHT, true);
		});
		if (state.board.size() > 0) {
			float[] quads = state.board.toArray();
			collector.submitCustomGeometry(poses, BOARD, (pose, consumer) -> HoloMapRenderer.emitLit(new DroneDepotRenderer.Geometry(pose, consumer),
					quads, LightCoordsUtil.FULL_BRIGHT, 255));
		}
		if (!state.picture.isEmpty()) {
			List<float[]> picture = List.copyOf(state.picture);
			collector.submitCustomGeometry(poses, PICTURE, (pose, consumer) -> {
				DroneDepotRenderer.Geometry g = new DroneDepotRenderer.Geometry(pose, consumer);
				for (float[] q : picture) {
					int color = Float.floatToRawIntBits(q[20]);
					// counter-clockwise as the viewer sees it: top-left, bottom-left, bottom-right, top-right
					for (int k : new int[] {0, 3, 2, 1}) {
						g.meshVertex(q[k * 5], q[k * 5 + 1], q[k * 5 + 2], q[k * 5 + 3], q[k * 5 + 4], 0, 1, 0, color, LightCoordsUtil.FULL_BRIGHT);
					}
				}
			});
		}
		if (state.seams.size() > 0) {
			float[] quads = state.seams.toArray();
			collector.submitCustomGeometry(poses, SEAMS, (pose, consumer) -> HoloMapRenderer.emitLit(new DroneDepotRenderer.Geometry(pose, consumer),
					quads, LightCoordsUtil.FULL_BRIGHT, 0x2E));
		}
		for (Text t : state.texts) {
			Facet fa = f[t.facet()];
			float[] o = fa.at(t.x(), t.y(), L_TEXT);
			float sx = fa.kx * t.scale(), sy = fa.ky * t.scale();
			poses.pushPose();
			poses.mulPose(new Matrix4f(fa.r.x * sx, fa.r.y * sx, fa.r.z * sx, 0, fa.d.x * sy, fa.d.y * sy, fa.d.z * sy, 0, fa.n.x * sy, fa.n.y * sy,
					fa.n.z * sy, 0, o[0], o[1], o[2], 1));
			collector.submitText(poses, 0, 0, Texts.of(t.s()), false, Font.DisplayMode.POLYGON_OFFSET,
					LightCoordsUtil.FULL_BRIGHT, t.color(), 0, 0);
			poses.popPose();
		}
		poses.popPose();
	}
}
