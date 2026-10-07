package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Boxes and planes in a block's space, in pixels, drawn in one colour over a 16x16 texture that each face reads where a
 * block model's face would (from its position), for renderers that tint what they draw (the Aura Candle, the wax in the
 * Wax Melting Pot). Each face is wound to face along its normal whatever the order of its corners. The entity cutout and
 * translucent render types these renderers use do not cull back faces in 26.3, so a box's faces show from inside too, and
 * a flame's plane is drawn as two sides lifted off its middle ({@link #plane}), never as one plane drawn twice.
 */
final class TintedBoxes {
	private TintedBoxes() {
	}

	static void box(VertexConsumer buffer, PoseStack.Pose matrix, float x0, float y0, float z0, float x1, float y1, float z1, int argb, int light) {
		face(buffer, matrix, argb, light, 0, 1, 0, new float[][] {{x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}},
				new float[][] {{x0, z0}, {x1, z0}, {x1, z1}, {x0, z1}});
		face(buffer, matrix, argb, light, 0, -1, 0, new float[][] {{x0, y0, z1}, {x1, y0, z1}, {x1, y0, z0}, {x0, y0, z0}},
				new float[][] {{x0, 16 - z1}, {x1, 16 - z1}, {x1, 16 - z0}, {x0, 16 - z0}});
		face(buffer, matrix, argb, light, 0, 0, -1, new float[][] {{x1, y1, z0}, {x0, y1, z0}, {x0, y0, z0}, {x1, y0, z0}},
				new float[][] {{16 - x1, 16 - y1}, {16 - x0, 16 - y1}, {16 - x0, 16 - y0}, {16 - x1, 16 - y0}});
		face(buffer, matrix, argb, light, 0, 0, 1, new float[][] {{x0, y1, z1}, {x1, y1, z1}, {x1, y0, z1}, {x0, y0, z1}},
				new float[][] {{x0, 16 - y1}, {x1, 16 - y1}, {x1, 16 - y0}, {x0, 16 - y0}});
		face(buffer, matrix, argb, light, -1, 0, 0, new float[][] {{x0, y1, z0}, {x0, y1, z1}, {x0, y0, z1}, {x0, y0, z0}},
				new float[][] {{z0, 16 - y1}, {z1, 16 - y1}, {z1, 16 - y0}, {z0, 16 - y0}});
		face(buffer, matrix, argb, light, 1, 0, 0, new float[][] {{x1, y1, z1}, {x1, y1, z0}, {x1, y0, z0}, {x1, y0, z1}},
				new float[][] {{16 - z1, 16 - y1}, {16 - z0, 16 - y1}, {16 - z0, 16 - y0}, {16 - z1, 16 - y0}});
	}

	/** A flat square facing up at height {@code y}, from ({@code x0}, {@code z0}) to ({@code x1}, {@code z1}). */
	static void top(VertexConsumer buffer, PoseStack.Pose matrix, float x0, float z0, float x1, float z1, float y, int argb, int light) {
		face(buffer, matrix, argb, light, 0, 1, 0, new float[][] {{x0, y, z0}, {x1, y, z0}, {x1, y, z1}, {x0, y, z1}},
				new float[][] {{x0, z0}, {x1, z0}, {x1, z1}, {x0, z1}});
	}

	/**
	 * A flat quad facing up, its corners at {@code corners} (in pixels) showing the texture's corners at {@code uv} (0 to 1
	 * across the whole texture), for a picture laid on a surface (the Spirit Board's face, the planchette).
	 */
	static void flat(VertexConsumer buffer, PoseStack.Pose matrix, float[][] corners, float[][] uv, int argb, int light) {
		float[][] scaled = new float[4][];
		for (int k = 0; k < 4; k++) {
			scaled[k] = new float[] {uv[k][0] * 16.0F, uv[k][1] * 16.0F};
		}
		face(buffer, matrix, argb, light, 0, 1, 0, corners, scaled);
	}

	/**
	 * A plane standing upright through the middle of the block, turned {@code turn} about the vertical, {@code width} wide and
	 * from {@code y0} to {@code y1}, showing the whole texture from both sides (for flames), each side lifted
	 * {@link DecorDraw#TWO_SIDED_LIFT} pixels off the middle.
	 */
	static void plane(VertexConsumer buffer, PoseStack.Pose matrix, double turn, float width, float y0, float y1, int argb, int light) {
		plane(buffer, matrix, turn, width, y0, y1, argb, light, DecorDraw.TWO_SIDED_LIFT);
	}

	/**
	 * As {@link #plane}, with each side drawn {@code lift} pixels out from the middle on its own side (never 0: the two sides
	 * would share one plane and flicker), so a smaller plane drawn with a bigger lift shows in front of a bigger one through
	 * the same middle, from either side (a flame's bright core).
	 */
	static void plane(VertexConsumer buffer, PoseStack.Pose matrix, double turn, float width, float y0, float y1, int argb, int light,
			float lift) {
		float dx = (float) Math.cos(turn) * width / 2;
		float dz = (float) Math.sin(turn) * width / 2;
		float nx = (float) -Math.sin(turn);
		float nz = (float) Math.cos(turn);
		float[][] uv = {{0, 0}, {16, 0}, {16, 16}, {0, 16}};
		for (int side = 1; side >= -1; side -= 2) {
			float ox = 8 + side * nx * lift;
			float oz = 8 + side * nz * lift;
			float[][] corners = {{ox - dx, y1, oz - dz}, {ox + dx, y1, oz + dz}, {ox + dx, y0, oz + dz}, {ox - dx, y0, oz - dz}};
			face(buffer, matrix, argb, light, side * nx, 0, side * nz, corners, uv);
		}
	}

	/** Draws a quad facing along (nx, ny, nz): counter-clockwise seen from that side, reversing its corners if need be. */
	private static void face(VertexConsumer buffer, PoseStack.Pose matrix, int argb, int light, float nx, float ny, float nz, float[][] corners,
			float[][] uv) {
		float[] a = corners[0];
		float[] b = corners[1];
		float[] c = corners[2];
		float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
		float vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
		float facing = (uy * vz - uz * vy) * nx + (uz * vx - ux * vz) * ny + (ux * vy - uy * vx) * nz;
		for (int k = 0; k < 4; k++) {
			int i = facing >= 0 ? k : (4 - k) % 4;
			buffer.addVertex(matrix, corners[i][0] / 16.0F, corners[i][1] / 16.0F, corners[i][2] / 16.0F).setColor(argb)
					.setUv(uv[i][0] / 16.0F, uv[i][1] / 16.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix, nx, ny, nz);
		}
	}
}
