package io.github.jimbozoomer.jugcraft.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.nuclear.SiteAnchor;
import io.github.jimbozoomer.jugcraft.nuclear.SiteData;
import java.util.Locale;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;

/**
 * A compiled site's live parts (towers and pump houses build), drawn by its anchor's renderer: for each state the game
 * drives, the group its value draws (a pump's running lamps and gauge, a valve's lever, the lights, a door), in the
 * site's frame turned with it; and its LED displays' figures, drawn in glyphs from the game's numbers. Glowing quads at
 * full light, the rest lit as the group's middle is.
 */
public final class SiteLive {
	private static final RenderType CUTOUT = RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS);
	private static final RenderType TRANSLUCENT = RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
	private static final int FULL = LightCoordsUtil.FULL_BRIGHT;

	private SiteLive() {
	}

	/** What the renderer draws this frame. */
	public static final class State {
		public SiteData data;
		public Direction facing = Direction.SOUTH;
		public SiteData.Group[] groups = new SiteData.Group[0];
		public int[] lights = new int[0];
		public String[] displays = new String[0];
		/** Each display's light: full for an LED's lit figures, the world's where it stands for printed ones (a counter's). */
		public int[] displayLights = new int[0];
	}

	/** Takes the anchor's values now (main thread). */
	public static void extract(SiteAnchor anchor, Level level, BlockPos at, State st) {
		String key = anchor.siteKey();
		SiteData d = key == null ? null : SiteData.get(key);
		st.data = d;
		if (d == null) {
			return;
		}
		st.facing = anchor.siteFacing();
		SiteData.Group[][] table = anchor.groups();
		if (st.groups.length != table.length) {
			st.groups = new SiteData.Group[table.length];
			st.lights = new int[table.length];
		}
		for (int v = 0; v < table.length; v++) {
			SiteData.Variable var = v < d.variables.size() ? d.variables.get(v) : null;
			// (a joint variable, a figure several states change together, takes its value from its members')
			int value = var != null && var.members() != null ? var.joint(anchor::value, d.variables) : anchor.value(v);
			SiteData.Group g = value >= 0 && value < table[v].length ? table[v][value] : null;
			st.groups[v] = g;
			if (g != null && g.quads() > 0) {
				float[] c = centre(g);
				st.lights[v] = LightCoordsUtil.getLightCoords(level, world(d, st.facing, at, c[0], c[1], c[2]));
			}
		}
		JsonArray ds = d.live.getAsJsonArray("displays");
		if (st.displays.length != ds.size()) {
			st.displays = new String[ds.size()];
			st.displayLights = new int[ds.size()];
		}
		for (int i = 0; i < ds.size(); i++) {
			JsonObject o = ds.get(i).getAsJsonObject();
			double value = anchor.display(o.get("key").getAsString());
			int chars = o.get("chars").getAsInt();
			String s = Double.isNaN(value) ? "--.-" : String.format(Locale.ROOT, o.get("format").getAsString(), value);
			if (s.length() > chars) s = s.substring(s.length() - chars);
			while (s.length() < chars) s = " " + s;
			st.displays[i] = s;
			st.displayLights[i] = FULL;
			if (o.has("lit") && !o.get("lit").getAsBoolean()) {
				JsonArray cs = o.getAsJsonArray("corners");
				double cx = 0, cy = 0, cz = 0;
				for (int k = 0; k < 4; k++) {
					JsonArray c = cs.get(k).getAsJsonArray();
					cx += c.get(0).getAsDouble() / 4;
					cy += c.get(1).getAsDouble() / 4;
					cz += c.get(2).getAsDouble() / 4;
				}
				st.displayLights[i] = LightCoordsUtil.getLightCoords(level, world(d, st.facing, at, cx, cy, cz));
			}
		}
	}

	private static float[] centre(SiteData.Group g) {
		float[] v = g.verts();
		double x = 0, y = 0, z = 0;
		int n = v.length / 5;
		for (int i = 0; i < v.length; i += 5) {
			x += v[i];
			y += v[i + 1];
			z += v[i + 2];
		}
		return new float[] {(float) (x / n), (float) (y / n), (float) (z / n)};
	}

	/** The world block holding a point of the site. */
	private static BlockPos world(SiteData d, Direction facing, BlockPos anchor, double x, double y, double z) {
		double[] t = turn(facing, x - d.ax - 0.5, z - d.az - 0.5);
		return BlockPos.containing(anchor.getX() + 0.5 + t[0], anchor.getY() + (y - d.ay), anchor.getZ() + 0.5 + t[1]);
	}

	private static double[] turn(Direction facing, double x, double z) {
		return switch (facing) {
			case WEST -> new double[] {-z, x};
			case NORTH -> new double[] {-x, -z};
			case EAST -> new double[] {z, -x};
			default -> new double[] {x, z};
		};
	}

	/** Draws the live parts (the pose at the anchor block's corner). */
	public static void submit(State st, PoseStack pose, SubmitNodeCollector collector) {
		SiteData d = st.data;
		if (d == null) {
			return;
		}
		for (int pass = 0; pass < 2; pass++) {
			boolean translucent = pass == 1;
			collector.submitCustomGeometry(pose, translucent ? TRANSLUCENT : CUTOUT, (p, consumer) -> {
				for (int v = 0; v < st.groups.length; v++) {
					SiteData.Group g = st.groups[v];
					if (g != null && g.quads() > 0) {
						draw(d, st.facing, g, st.lights[v], translucent, p, consumer);
					}
				}
				if (!translucent) {
					displays(d, st, p, consumer);
				}
			});
		}
	}

	private static void draw(SiteData d, Direction facing, SiteData.Group g, int light, boolean translucent, PoseStack.Pose pose,
			VertexConsumer consumer) {
		float[] v = g.verts();
		for (int q = 0; q < g.quads(); q++) {
			int s = g.sprite()[q] & 0xFFFF;
			if ((d.layers[s] == 2) != translucent) continue;
			Material.Baked mat = SiteModel.sprite(d.sprites[s]);
			if (mat == null) continue;
			TextureAtlasSprite sp = mat.sprite();
			int l = g.lit()[q] != 0 ? FULL : light;
			float nx = g.normal()[q * 3] / 127F, ny = g.normal()[q * 3 + 1] / 127F, nz = g.normal()[q * 3 + 2] / 127F;
			double[] tn = turn(facing, nx, nz);
			for (int c = 0; c < 4; c++) {
				int o = q * 20 + c * 5;
				double[] t = turn(facing, v[o] - d.ax - 0.5, v[o + 2] - d.az - 0.5);
				float u = sp.getU0() + (sp.getU1() - sp.getU0()) * v[o + 3];
				float vv = sp.getV0() + (sp.getV1() - sp.getV0()) * v[o + 4];
				consumer.addVertex(pose, (float) (t[0] + 0.5), v[o + 1] - d.ay, (float) (t[1] + 0.5)).setColor(0xFFFFFFFF).setUv(u, vv)
						.setOverlay(OverlayTexture.NO_OVERLAY).setLight(l).setNormal(pose, (float) tn[0], ny, (float) tn[1]);
			}
		}
	}

	/** The LED displays' figures: each character's glyph in its place in the window, lit. */
	private static void displays(SiteData d, State st, PoseStack.Pose pose, VertexConsumer consumer) {
		JsonArray ds = d.live.getAsJsonArray("displays");
		for (int i = 0; i < ds.size() && i < st.displays.length; i++) {
			JsonObject o = ds.get(i).getAsJsonObject();
			Material.Baked mat = SiteModel.sprite(o.get("glyphs").getAsString());
			if (mat == null) continue;
			TextureAtlasSprite sp = mat.sprite();
			JsonArray cs = o.getAsJsonArray("corners");
			double[][] c = new double[4][];
			for (int k = 0; k < 4; k++) {
				JsonArray p = cs.get(k).getAsJsonArray();
				c[k] = new double[] {p.get(0).getAsDouble(), p.get(1).getAsDouble(), p.get(2).getAsDouble()};
			}
			String alphabet = o.get("alphabet").getAsString();
			double gw = o.get("glyph_w").getAsDouble(), gh = o.get("glyph_h").getAsDouble();
			JsonArray cells = o.getAsJsonArray("cells");
			String text = st.displays[i];
			for (int k = 0; k < text.length() && k < cells.size(); k++) {
				int idx = alphabet.indexOf(text.charAt(k));
				if (idx < 0 || text.charAt(k) == ' ') continue;
				JsonArray cell = cells.get(k).getAsJsonArray();
				double a0 = cell.get(0).getAsDouble(), b0 = cell.get(1).getAsDouble(), a1 = cell.get(2).getAsDouble(), b1 = cell.get(3).getAsDouble();
				double[][] q = {lerp(c, a0, b0), lerp(c, a0, b1), lerp(c, a1, b1), lerp(c, a1, b0)};
				double[][] uv = {{idx * gw, 0}, {idx * gw, gh}, {(idx + 1) * gw, gh}, {(idx + 1) * gw, 0}};
				double[] n = normal(c);
				double[] tn = turn(st.facing, n[0], n[2]);
				for (int k2 = 0; k2 < 4; k2++) {
					double[] t = turn(st.facing, q[k2][0] - d.ax - 0.5, q[k2][2] - d.az - 0.5);
					float u = sp.getU0() + (sp.getU1() - sp.getU0()) * (float) uv[k2][0];
					float vv = sp.getV0() + (sp.getV1() - sp.getV0()) * (float) uv[k2][1];
					int light = i < st.displayLights.length ? st.displayLights[i] : FULL;
					consumer.addVertex(pose, (float) (t[0] + 0.5), (float) (q[k2][1] - d.ay), (float) (t[1] + 0.5)).setColor(0xFFFFFFFF)
							.setUv(u, vv).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, (float) tn[0], (float) n[1], (float) tn[1]);
				}
			}
		}
	}

	/** A point of the window: a across (left to right), b down (top to bottom); corners tl, bl, br, tr. */
	private static double[] lerp(double[][] c, double a, double b) {
		double[] top = new double[3], bottom = new double[3], out = new double[3];
		for (int i = 0; i < 3; i++) {
			top[i] = c[0][i] + (c[3][i] - c[0][i]) * a;
			bottom[i] = c[1][i] + (c[2][i] - c[1][i]) * a;
			out[i] = top[i] + (bottom[i] - top[i]) * b;
		}
		return out;
	}

	private static double[] normal(double[][] c) {
		double[] a = {c[3][0] - c[0][0], c[3][1] - c[0][1], c[3][2] - c[0][2]};
		double[] b = {c[1][0] - c[0][0], c[1][1] - c[0][1], c[1][2] - c[0][2]};
		double[] n = {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
		double l = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
		// (the site's frame is the generators' mirrored, which turns their normal about)
		return l == 0 ? new double[] {0, 1, 0} : new double[] {-n[0] / l, -n[1] / l, -n[2] / l};
	}
}
