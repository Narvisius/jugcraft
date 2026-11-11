package io.github.jimbozoomer.jugcraft.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.powerline.LineGeometry;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import org.jspecify.annotations.Nullable;

/**
 * The power line supports' models (power lines round 2; tools/line_models.py from the approved generators): each
 * support's bodies, footings and strain assemblies as quads in the game's frame, in three levels of detail (all of it
 * textured; small parts left out and each face one flat colour from {@link #LOD_NEAR} blocks; only the main members
 * from {@link #LOD_MID}), drawn from one texture. Read once each, when first drawn.
 */
public final class LineModels {
	public static final Identifier TEXTURE = Jugcraft.id("textures/entity/line_models.png");
	/** Beyond these distances (blocks) a structure is drawn at the next level of detail. */
	public static final double LOD_NEAR = 48, LOD_MID = 128;

	private static final Map<String, Model> MODELS = new HashMap<>();
	private static float[] white;

	private LineModels() {
	}

	/** One level of a model: per quad its corners (x, y, z), UVs, normal, colour and flags (1 lit, 2 blinking). */
	public static final class Level {
		final int count;
		final float[] pos, uv, nrm;
		final int[] colour;
		final byte[] flags;

		Level(int count) {
			this.count = count;
			pos = new float[count * 12];
			uv = new float[count * 8];
			nrm = new float[count * 3];
			colour = new int[count];
			flags = new byte[count];
		}
	}

	/** A model: its levels of detail, and how far it reaches from its origin (across, above, below; blocks). */
	public record Model(String name, Level[] levels, float radius, float top, float bottom) {
		public Level level(int lod) {
			return levels[Math.min(lod, levels.length - 1)];
		}
	}

	/** A texture region of the parts drawn in code (LineGeometry's data: "aluminium", "galv", "polymer", "white"...). */
	public static float[] region(String name) {
		JsonArray a = LineGeometry.raw().getAsJsonObject("regions").getAsJsonArray(name);
		return new float[] {a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat(), a.get(3).getAsFloat()};
	}

	/** A material's average colour (ARGB), for the flat-coloured parts drawn in code (wires, jumpers). */
	public static int colour(String name) {
		JsonArray a = LineGeometry.raw().getAsJsonObject("colours").getAsJsonArray(name);
		return 0xFF000000 | a.get(0).getAsInt() << 16 | a.get(1).getAsInt() << 8 | a.get(2).getAsInt();
	}

	/** The centre of the white cell (flat colours are drawn tinted from it). */
	static float[] white() {
		if (white == null) {
			float[] r = region("white");
			white = new float[] {(r[0] + r[2]) / 2, (r[1] + r[3]) / 2};
		}
		return white;
	}

	/** A plate letter's cell: u0, v0, u1, v1, advance (px at the glyphs' size), cell width (px); null if not drawn. */
	static float @Nullable [] glyph(char c) {
		JsonObject g = LineGeometry.raw().getAsJsonObject("glyphs").getAsJsonObject("chars");
		JsonElement e = g.get(String.valueOf(c));
		if (e == null) {
			return null;
		}
		JsonArray a = e.getAsJsonArray();
		float[] out = new float[6];
		for (int i = 0; i < 6; i++) {
			out[i] = a.get(i).getAsFloat();
		}
		return out;
	}

	static float glyphPx() {
		return LineGeometry.raw().getAsJsonObject("glyphs").get("px").getAsFloat();
	}

	static float glyphCell() {
		return LineGeometry.raw().getAsJsonObject("glyphs").get("cell").getAsFloat();
	}

	/** A model by name, read on first use (null if there is none). */
	public static synchronized @Nullable Model get(String name) {
		if (MODELS.containsKey(name)) {
			return MODELS.get(name);
		}
		Model m = null;
		try (InputStream raw = LineModels.class.getResourceAsStream("/assets/jugcraft/power_lines/models/" + name + ".bin")) {
			if (raw != null) {
				m = read(name, new DataInputStream(new BufferedInputStream(raw, 1 << 16)));
			}
		} catch (Exception e) {
			Jugcraft.LOGGER.error("Could not read power line model {}", name, e);
		}
		MODELS.put(name, m);
		return m;
	}

	private static Model read(String name, DataInputStream in) throws java.io.IOException {
		byte[] magic = new byte[4];
		in.readFully(magic);
		if (magic[0] != 'J' || magic[1] != 'L' || magic[2] != 'M' || magic[3] != '1') {
			throw new java.io.IOException("not a line model");
		}
		int levels = in.readUnsignedByte();
		Level[] out = new Level[levels];
		float[] w = white();
		for (int lv = 0; lv < levels; lv++) {
			int n = in.readInt();
			Level l = new Level(n);
			for (int q = 0; q < n; q++) {
				int flags = in.readUnsignedByte();
				for (int i = 0; i < 12; i++) {
					l.pos[q * 12 + i] = in.readShort() / 256F;
				}
				for (int i = 0; i < 3; i++) {
					l.nrm[q * 3 + i] = in.readByte() / 127F;
				}
				if ((flags & 4) != 0) {
					int r = in.readUnsignedByte(), g = in.readUnsignedByte(), b = in.readUnsignedByte();
					l.colour[q] = 0xFF000000 | r << 16 | g << 8 | b;
					for (int i = 0; i < 4; i++) {
						l.uv[q * 8 + i * 2] = w[0];
						l.uv[q * 8 + i * 2 + 1] = w[1];
					}
				} else {
					l.colour[q] = 0xFFFFFFFF;
					for (int i = 0; i < 8; i++) {
						l.uv[q * 8 + i] = in.readUnsignedShort() / 65535F;
					}
				}
				l.flags[q] = (byte) (flags & 3);
			}
			out[lv] = l;
		}
		float radius = 0, top = 0, bottom = 0;
		if (levels > 0) {
			Level l = out[0];
			for (int i = 0; i < l.count * 4; i++) {
				float x = l.pos[i * 3], y = l.pos[i * 3 + 1], z = l.pos[i * 3 + 2];
				radius = Math.max(radius, (float) Math.sqrt(x * x + z * z));
				top = Math.max(top, y);
				bottom = Math.min(bottom, y);
			}
		}
		return new Model(name, out, radius, top, bottom);
	}

	/** Light by height above a structure's anchor: samples every {@link #STEP} blocks. */
	public static final int STEP = 8;

	/**
	 * Draws a model's level: each corner turned about the vertical by (cos, sin) (as LineGeometry: 0 faces +z) then
	 * moved by (tx, ty, tz), in the pose (blocks from the anchor block's corner). lights: the light every {@link #STEP}
	 * blocks up from the anchor (by the corner's height); blinkOn: whether blinking lamps are lit now; tint: multiplied
	 * into every colour.
	 */
	static void draw(Level l, PoseStack.Pose pose, VertexConsumer c, float cos, float sin, float tx, float ty, float tz, int[] lights,
			boolean blinkOn, int tint) {
		Xf x = new Xf(pose);
		for (int q = 0; q < l.count; q++) {
			int f = l.flags[q];
			boolean lit = (f & 1) != 0 && ((f & 2) == 0 || blinkOn);
			float nx = l.nrm[q * 3], ny = l.nrm[q * 3 + 1], nz = l.nrm[q * 3 + 2];
			float wnx = nx * cos + nz * sin, wnz = -nx * sin + nz * cos;
			x.normal(wnx, ny, wnz);
			int col = multiply(l.colour[q], tint);
			for (int i = 0; i < 4; i++) {
				int o = q * 12 + i * 3;
				float px = l.pos[o], py = l.pos[o + 1], pz = l.pos[o + 2];
				float wx = px * cos + pz * sin + tx, wy = py + ty, wz = -px * sin + pz * cos + tz;
				int light = lit ? LightCoordsUtil.FULL_BRIGHT : lights[Math.max(0, Math.min(lights.length - 1, (int) (wy / STEP)))];
				x.vertex(c, wx, wy, wz, col, l.uv[q * 8 + i * 2], l.uv[q * 8 + i * 2 + 1], light);
			}
		}
	}

	/** A pose's transform taken apart, for writing many vertices quickly (the buffer's one-call vertex). */
	static final class Xf {
		final float m00, m01, m02, m10, m11, m12, m20, m21, m22, m30, m31, m32;
		final float n00, n01, n02, n10, n11, n12, n20, n21, n22;
		float nx, ny, nz;

		Xf(PoseStack.Pose pose) {
			org.joml.Matrix4f m = pose.pose();
			m00 = m.m00(); m01 = m.m01(); m02 = m.m02();
			m10 = m.m10(); m11 = m.m11(); m12 = m.m12();
			m20 = m.m20(); m21 = m.m21(); m22 = m.m22();
			m30 = m.m30(); m31 = m.m31(); m32 = m.m32();
			org.joml.Matrix3f n = pose.normal();
			n00 = n.m00(); n01 = n.m01(); n02 = n.m02();
			n10 = n.m10(); n11 = n.m11(); n12 = n.m12();
			n20 = n.m20(); n21 = n.m21(); n22 = n.m22();
		}

		void normal(float x, float y, float z) {
			float a = n00 * x + n10 * y + n20 * z, b = n01 * x + n11 * y + n21 * z, d = n02 * x + n12 * y + n22 * z;
			float len = (float) Math.sqrt(a * a + b * b + d * d);
			if (len > 1e-6F) {
				a /= len;
				b /= len;
				d /= len;
			}
			nx = a;
			ny = b;
			nz = d;
		}

		void vertex(VertexConsumer c, float x, float y, float z, int colour, float u, float v, int light) {
			c.addVertex(m00 * x + m10 * y + m20 * z + m30, m01 * x + m11 * y + m21 * z + m31, m02 * x + m12 * y + m22 * z + m32, colour, u, v,
					OverlayTexture.NO_OVERLAY, light, nx, ny, nz);
		}
	}

	static int multiply(int a, int b) {
		if (b == 0xFFFFFFFF) {
			return a;
		}
		int r = ((a >> 16 & 255) * (b >> 16 & 255)) / 255, g = ((a >> 8 & 255) * (b >> 8 & 255)) / 255, bl = ((a & 255) * (b & 255)) / 255;
		return (a & 0xFF000000) | r << 16 | g << 8 | bl;
	}
}
