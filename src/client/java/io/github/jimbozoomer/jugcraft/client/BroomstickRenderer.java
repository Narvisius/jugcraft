package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.Broomstick;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * A flying broomstick (fall addition 22): an ash handle with a turned knob at its tip, bound with two bands of purple
 * cord, and a bundle of straw bristles flaring out behind. It points the way it flies and tilts up as it climbs and down
 * as it dives; at rest it bobs gently. Drawn in one grain texture, tinted for wood, cord and straw; a dry broom's straw
 * is greyer.
 */
public class BroomstickRenderer extends EntityRenderer<Broomstick, BroomstickRenderer.State> {
	private static final RenderType GRAIN = RenderTypes.entityCutout(Jugcraft.id("textures/entity/flying_broomstick.png"));
	private static final int WOOD = 0xFF7A5634;
	private static final int KNOB = 0xFF5C3E22;
	private static final int CORD = 0xFF4A2D63;
	private static final int STRAW = 0xFFD2B060;
	private static final int STRAW_TIPS = 0xFFE4C878;
	private static final int DRY_STRAW = 0xFF9C9070;

	public static final class State extends EntityRenderState {
		float yaw;
		float tilt;
		float time;
		boolean dry;
	}

	public BroomstickRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(Broomstick broom, State state, float partialTick) {
		super.extractRenderState(broom, state, partialTick);
		state.yaw = Mth.rotLerp(partialTick, broom.yRotO, broom.getYRot());
		double rise = broom.getY() - broom.yo;
		double across = Math.hypot(broom.getX() - broom.xo, broom.getZ() - broom.zo);
		state.tilt = across + Math.abs(rise) < 0.02 ? 0.0F : (float) Mth.clamp(Math.toDegrees(Math.atan2(rise, Math.max(across, 0.15))), -30.0, 30.0);
		state.time = broom.tickCount + partialTick;
		state.dry = broom.dry();
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		int light = state.lightCoords;
		int straw = state.dry ? DRY_STRAW : STRAW;
		int tips = state.dry ? DRY_STRAW : STRAW_TIPS;
		pose.pushPose();
		pose.translate(0.0F, (float) Broomstick.HANDLE_Y + Mth.sin(state.time * 0.1F) * 0.03F, 0.0F);
		pose.rotateDegrees(Axis.YP, 180.0F - state.yaw);
		pose.rotateDegrees(Axis.XP, state.tilt);
		collector.submitCustomGeometry(pose, GRAIN, (matrix, buffer) -> {
			// Forward is -z. The handle and its knob.
			box(buffer, matrix, -0.7F, -0.7F, -14.0F, 0.7F, 0.7F, 5.0F, WOOD, light);
			box(buffer, matrix, -1.0F, -1.0F, -15.2F, 1.0F, 1.0F, -13.8F, KNOB, light);
			// The bristles, flaring out, bound by two bands of cord.
			box(buffer, matrix, -1.8F, -1.8F, 4.0F, 1.8F, 1.8F, 8.0F, straw, light);
			box(buffer, matrix, -2.6F, -2.4F, 8.0F, 2.6F, 2.2F, 11.5F, straw, light);
			box(buffer, matrix, -3.4F, -3.0F, 11.5F, 3.4F, 2.6F, 15.0F, tips, light);
			box(buffer, matrix, -2.1F, -2.1F, 4.6F, 2.1F, 2.1F, 5.6F, CORD, light);
			box(buffer, matrix, -2.1F, -2.1F, 6.6F, 2.1F, 2.1F, 7.4F, CORD, light);
		});
		pose.popPose();
		super.submit(state, pose, collector, camera);
	}

	/** A box in pixels, every face showing the whole grain texture in {@code argb}. */
	private static void box(VertexConsumer buffer, PoseStack.Pose matrix, float x0, float y0, float z0, float x1, float y1, float z1, int argb,
			int light) {
		face(buffer, matrix, argb, light, 0, 1, 0, new float[][] {{x0, y1, z0}, {x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}});
		face(buffer, matrix, argb, light, 0, -1, 0, new float[][] {{x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1}});
		face(buffer, matrix, argb, light, 0, 0, -1, new float[][] {{x0, y0, z0}, {x0, y1, z0}, {x1, y1, z0}, {x1, y0, z0}});
		face(buffer, matrix, argb, light, 0, 0, 1, new float[][] {{x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}});
		face(buffer, matrix, argb, light, -1, 0, 0, new float[][] {{x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}, {x0, y1, z0}});
		face(buffer, matrix, argb, light, 1, 0, 0, new float[][] {{x1, y0, z0}, {x1, y1, z0}, {x1, y1, z1}, {x1, y0, z1}});
	}

	/** A quad facing along (nx, ny, nz), wound counter-clockwise seen from that side whatever the order of its corners. */
	private static void face(VertexConsumer buffer, PoseStack.Pose matrix, int argb, int light, float nx, float ny, float nz, float[][] corners) {
		float[] a = corners[0];
		float[] b = corners[1];
		float[] c = corners[2];
		float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
		float vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
		float facing = (uy * vz - uz * vy) * nx + (uz * vx - ux * vz) * ny + (ux * vy - uy * vx) * nz;
		float[][] uv = {{0.0F, 0.0F}, {1.0F, 0.0F}, {1.0F, 1.0F}, {0.0F, 1.0F}};
		for (int k = 0; k < 4; k++) {
			int i = facing >= 0 ? k : (4 - k) % 4;
			buffer.addVertex(matrix, corners[i][0] / 16.0F, corners[i][1] / 16.0F, corners[i][2] / 16.0F).setColor(argb)
					.setUv(uv[i][0], uv[i][1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix, nx, ny, nz);
		}
	}
}
