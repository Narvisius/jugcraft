package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.SpinningWheelBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SpinningWheelBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * What moves on a Spinning Wheel (the block model is its bench, legs, treadle, posts and distaff), drawn for a wheel facing
 * north and turned to its facing: the big wheel, a rim of twelve felloes on six spokes round a hub, turning as the treadle
 * is worked; the skein of wool on the distaff, in its colour, shrinking as it is spun; and the yarn winding onto the
 * bobbin, growing as the skein shrinks.
 */
public class SpinningWheelRenderer implements BlockEntityRenderer<SpinningWheelBlockEntity, SpinningWheelRenderer.State> {
	private static final RenderType WOOD = RenderTypes.entityCutout(Jugcraft.id("textures/block/spinning_wheel_wood.png"));
	private static final RenderType WOOL = RenderTypes.entityCutout(Jugcraft.id("textures/block/spinning_wheel_wool.png"));
	private static final int WHITE = 0xFFFFFFFF;
	/** The wheel's centre (pixels), its radius to the outside of the rim, and the faces of its rim (along the axle). */
	private static final float CX = 8.0F;
	private static final float CY = 10.0F;
	private static final float RADIUS = 5.5F;
	private static final float Z0 = 9.0F;
	private static final float Z1 = 10.0F;
	private static final int FELLOES = 12;
	private static final int SPOKES = 6;
	/** Radians the wheel turns for each tick it spins. */
	private static final float SPEED = 0.35F;

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		float angle;
		int color = -1;
		int turns;
	}

	public SpinningWheelRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(SpinningWheelBlockEntity wheel, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(wheel, state, crumbling);
		state.facing = wheel.getBlockState().hasProperty(SpinningWheelBlock.FACING) ? wheel.getBlockState().getValue(SpinningWheelBlock.FACING)
				: Direction.NORTH;
		long time = wheel.getLevel() == null ? 0L : wheel.getLevel().getGameTime();
		state.angle = wheel.spin(time, partialTick) * SPEED;
		DyeColor wool = wheel.wool();
		state.color = wool == null ? -1 : wool.getTextureDiffuseColor() | 0xFF000000;
		state.turns = wheel.turns();
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		int light = state.lightCoords;
		float angle = state.angle;
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		collector.submitCustomGeometry(pose, WOOD, (matrix, buffer) -> {
			// The rim: felloes set end to end round the circle.
			float chord = (float) (2.0 * (RADIUS - 0.5) * Math.sin(Math.PI / FELLOES)) + 0.35F;
			for (int i = 0; i < FELLOES; i++) {
				double at = angle + i * 2.0 * Math.PI / FELLOES;
				rotatedBox(buffer, matrix, at, RADIUS - 0.5F, chord / 2.0F, 0.5F, Z0, Z1, light);
			}
			// The spokes, from the hub to the rim.
			for (int i = 0; i < SPOKES; i++) {
				double at = angle + i * Math.PI / SPOKES * 2.0 + Math.PI / FELLOES;
				rotatedBox(buffer, matrix, at, (RADIUS - 0.5F) / 2.0F, 0.25F, (RADIUS - 1.0F) / 2.0F, Z0 + 0.3F, Z1 - 0.3F, light);
			}
			// The hub, and the crank the footman turns.
			rotatedBox(buffer, matrix, angle, 0.0F, 0.9F, 0.9F, Z0 - 0.4F, Z1 + 0.4F, light);
			rotatedBox(buffer, matrix, angle, 1.2F, 0.3F, 1.2F, Z0 - 0.9F, Z0 - 0.4F, light);
		});
		// The skein on the distaff, smaller as it is spun; the yarn on the bobbin, fuller.
		if (state.color != -1) {
			int color = state.color;
			float left = 1.0F - (float) state.turns / SpinningWheelBlockEntity.TURNS;
			float half = 0.8F + 0.6F * left;
			float top = 13.0F + 1.8F * left;
			collector.submitCustomGeometry(pose, WOOL, (matrix, buffer) -> {
				TintedBoxes.box(buffer, matrix, 3.5F - half, 11.0F, 3.5F - half, 3.5F + half, top, 3.5F + half, color, light);
				if (state.turns > 0) {
					float wound = 0.3F + 0.5F * state.turns / SpinningWheelBlockEntity.TURNS;
					TintedBoxes.box(buffer, matrix, 11.0F, 8.75F - wound, 3.0F, 13.0F, 8.75F + wound, 5.5F, color, light);
				}
			});
		}
		pose.popPose();
	}

	/**
	 * A box in the wheel's plane, turned {@code at} radians about the axle: centred {@code distance} pixels out from the
	 * axle along that direction, {@code halfOut} each way along it and {@code halfAlong} each way across it, from {@code z0}
	 * to {@code z1}.
	 */
	private static void rotatedBox(VertexConsumer buffer, PoseStack.Pose matrix, double at, float distance, float halfAlong, float halfOut, float z0,
			float z1, int light) {
		float ox = (float) Math.cos(at);
		float oy = (float) Math.sin(at);
		// The box's two axes in the plane: out from the axle, and across (a quarter turn on).
		float ax = -oy;
		float ay = ox;
		float cx = CX + ox * distance;
		float cy = CY + oy * distance;
		float hOut = halfOut;
		float hAcross = halfAlong;
		float[][] corners = new float[4][];
		float[][] signs = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
		for (int i = 0; i < 4; i++) {
			float s = signs[i][0];
			float t = signs[i][1];
			corners[i] = new float[] {cx + ox * hOut * s + ax * hAcross * t, cy + oy * hOut * s + ay * hAcross * t};
		}
		// Front and back.
		face(buffer, matrix, light, 0, 0, -1, new float[][] {xyz(corners[0], z0), xyz(corners[1], z0), xyz(corners[2], z0), xyz(corners[3], z0)},
				hOut * 2, hAcross * 2);
		face(buffer, matrix, light, 0, 0, 1, new float[][] {xyz(corners[0], z1), xyz(corners[1], z1), xyz(corners[2], z1), xyz(corners[3], z1)},
				hOut * 2, hAcross * 2);
		// The four sides, each between two neighbouring corners.
		for (int i = 0; i < 4; i++) {
			float[] a = corners[i];
			float[] b = corners[(i + 1) % 4];
			float mx = (a[0] + b[0]) / 2 - cx;
			float my = (a[1] + b[1]) / 2 - cy;
			float length = (float) Math.sqrt(mx * mx + my * my);
			float edge = (float) Math.hypot(b[0] - a[0], b[1] - a[1]);
			face(buffer, matrix, light, mx / length, my / length, 0, new float[][] {xyz(a, z0), xyz(b, z0), xyz(b, z1), xyz(a, z1)}, edge, z1 - z0);
		}
	}

	private static float[] xyz(float[] xy, float z) {
		return new float[] {xy[0], xy[1], z};
	}

	/** A quad (pixel corners) facing along its normal, wound to suit, showing a {@code w} by {@code h} pixel patch of texture. */
	private static void face(VertexConsumer buffer, PoseStack.Pose matrix, int light, float nx, float ny, float nz, float[][] corners, float w,
			float h) {
		float[] a = corners[0];
		float[] b = corners[1];
		float[] c = corners[2];
		float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
		float vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
		float facing = (uy * vz - uz * vy) * nx + (uz * vx - ux * vz) * ny + (ux * vy - uy * vx) * nz;
		float u1 = Math.min(1.0F, w / 16.0F);
		float v1 = Math.min(1.0F, h / 16.0F);
		float[][] uv = {{0, 0}, {u1, 0}, {u1, v1}, {0, v1}};
		for (int k = 0; k < 4; k++) {
			int i = facing >= 0 ? k : (4 - k) % 4;
			buffer.addVertex(matrix, corners[i][0] / 16.0F, corners[i][1] / 16.0F, corners[i][2] / 16.0F).setColor(WHITE)
					.setUv(uv[i][0], uv[i][1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix, nx, ny, nz);
		}
	}
}
