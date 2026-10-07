package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Small drawing helpers for the decorations' block entity renderers: textured boxes and single quads, with corners in
 * the order the generated quads use (tools/decor6_data.py FACE_CORNERS), so they face the way their normals point.
 * Positions are in blocks.
 */
final class DecorDraw {
	/** Each face's corners as 0/1 picks of the box's low and high x, y, z, and its normal. */
	private static final int[][][] CORNERS = {
			{{0, 0, 0}, {0, 1, 0}, {1, 1, 0}, {1, 0, 0}}, // north
			{{1, 0, 1}, {1, 1, 1}, {0, 1, 1}, {0, 0, 1}}, // south
			{{0, 0, 1}, {0, 1, 1}, {0, 1, 0}, {0, 0, 0}}, // west
			{{1, 0, 0}, {1, 1, 0}, {1, 1, 1}, {1, 0, 1}}, // east
			{{0, 1, 0}, {0, 1, 1}, {1, 1, 1}, {1, 1, 0}}, // up
			{{0, 0, 1}, {0, 0, 0}, {1, 0, 0}, {1, 0, 1}}}; // down
	private static final float[][] NORMALS = {{0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}, {0, 1, 0}, {0, -1, 0}};
	static final int NORTH = 1;
	static final int SOUTH = 2;
	static final int WEST = 4;
	static final int EAST = 8;
	static final int UP = 16;
	static final int DOWN = 32;
	static final int ALL = 63;

	private DecorDraw() {
	}

	/** A box from (x0, y0, z0) to (x1, y1, z1), each face showing the texture's (u0, v0)-(u1, v1). */
	static void box(VertexConsumer buffer, PoseStack.Pose matrix, float x0, float y0, float z0, float x1, float y1, float z1, float u0, float v0,
			float u1, float v1, int color, int light, int faces) {
		float[] lo = {x0, y0, z0};
		float[] hi = {x1, y1, z1};
		for (int face = 0; face < 6; face++) {
			if ((faces & (1 << face)) == 0) {
				continue;
			}
			boolean flat = face >= 4;
			float[][] uv = flat ? new float[][] {{u0, v0}, {u0, v1}, {u1, v1}, {u1, v0}} : new float[][] {{u1, v1}, {u1, v0}, {u0, v0}, {u0, v1}};
			for (int i = 0; i < 4; i++) {
				int[] pick = CORNERS[face][i];
				buffer.addVertex(matrix, pick[0] == 1 ? hi[0] : lo[0], pick[1] == 1 ? hi[1] : lo[1], pick[2] == 1 ? hi[2] : lo[2]).setColor(color)
						.setUv(uv[i][0], uv[i][1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
						.setNormal(matrix, NORMALS[face][0], NORMALS[face][1], NORMALS[face][2]);
			}
		}
	}

	/** One quad through four corners {x, y, z, u, v}, in the order that faces it toward its normal. */
	static void quad(VertexConsumer buffer, PoseStack.Pose matrix, float[][] corners, float nx, float ny, float nz, int color, int light) {
		for (float[] c : corners) {
			buffer.addVertex(matrix, c[0], c[1], c[2]).setColor(color).setUv(c[3], c[4]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
					.setNormal(matrix, nx, ny, nz);
		}
	}

	/** How far (pixels) each side of a two-sided plane stands off the plane's middle (see {@link #twoSided}). */
	static final float TWO_SIDED_LIFT = 0.05F;

	/**
	 * A plane seen from both sides, for the render types that do not cull back faces in 26.3 (entityCutout,
	 * entityTranslucent): the quad through {@code corners} ({x, y, z, u, v}, positions in blocks) lit with normal (nx, ny,
	 * nz) and lifted {@code liftPixels} out along the plane's normal on that side, then the same quad reversed, lit the
	 * opposite way and lifted as far the other way. An exact reversed twin on one plane would draw at the same depth as
	 * the front from both sides and flicker (docs/ART_DIRECTION.md, Rules for everything).
	 */
	static void twoSided(VertexConsumer buffer, PoseStack.Pose matrix, float[][] corners, float nx, float ny, float nz, int color, int light,
			float liftPixels) {
		twoSided(buffer, matrix, corners, nx, ny, nz, color, light, liftPixels, false);
	}

	/**
	 * As {@link #twoSided}, but the back shows the picture the same way round as the front (its u flipped across the
	 * quad's u range) rather than mirrored, as a flame or a sign seen from behind should.
	 */
	static void twoSidedReadable(VertexConsumer buffer, PoseStack.Pose matrix, float[][] corners, float nx, float ny, float nz, int color,
			int light, float liftPixels) {
		twoSided(buffer, matrix, corners, nx, ny, nz, color, light, liftPixels, true);
	}

	private static void twoSided(VertexConsumer buffer, PoseStack.Pose matrix, float[][] corners, float nx, float ny, float nz, int color,
			int light, float liftPixels, boolean readableBack) {
		float[] lift = lift(corners, nx, ny, nz, liftPixels / 16.0F);
		float uSum = 0.0F;
		if (readableBack) {
			float uMin = corners[0][3];
			float uMax = corners[0][3];
			for (float[] c : corners) {
				uMin = Math.min(uMin, c[3]);
				uMax = Math.max(uMax, c[3]);
			}
			uSum = uMin + uMax;
		}
		for (int k = 0; k < 4; k++) {
			float[] c = corners[k];
			buffer.addVertex(matrix, c[0] + lift[0], c[1] + lift[1], c[2] + lift[2]).setColor(color).setUv(c[3], c[4])
					.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix, nx, ny, nz);
		}
		for (int k = 3; k >= 0; k--) {
			float[] c = corners[k];
			float u = readableBack ? uSum - c[3] : c[3];
			buffer.addVertex(matrix, c[0] - lift[0], c[1] - lift[1], c[2] - lift[2]).setColor(color).setUv(u, c[4])
					.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix, -nx, -ny, -nz);
		}
	}

	/**
	 * The offset that lifts a quad through {@code corners} ({x, y, z, ...}) {@code distance} off its plane on the side
	 * (nx, ny, nz) faces: along the plane's own normal (from its diagonals), so the lift always leaves the plane even when
	 * the lighting normal lies in it.
	 */
	static float[] lift(float[][] corners, float nx, float ny, float nz, float distance) {
		float ax = corners[2][0] - corners[0][0], ay = corners[2][1] - corners[0][1], az = corners[2][2] - corners[0][2];
		float bx = corners[3][0] - corners[1][0], by = corners[3][1] - corners[1][1], bz = corners[3][2] - corners[1][2];
		float px = ay * bz - az * by, py = az * bx - ax * bz, pz = ax * by - ay * bx;
		float length = (float) Math.sqrt(px * px + py * py + pz * pz);
		if (length < 1.0E-12F) {
			px = nx;
			py = ny;
			pz = nz;
			length = (float) Math.sqrt(px * px + py * py + pz * pz);
			if (length < 1.0E-12F) {
				return new float[] {0, 0, 0};
			}
		}
		float scale = distance / length * (px * nx + py * ny + pz * nz < 0 ? -1 : 1);
		return new float[] {px * scale, py * scale, pz * scale};
	}
}
