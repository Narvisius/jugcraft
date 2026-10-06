package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.nuclear.CoolingTowerBlock;
import io.github.jimbozoomer.jugcraft.nuclear.CoolingTowerBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

/**
 * The fans of a mechanical-draft cooling bank, drawn from its controller: in each cell's stack a hub and seven blades,
 * turning as fast as the tower is busy (slowly idling when it is not).
 */
public class CoolingTowerRenderer implements BlockEntityRenderer<CoolingTowerBlockEntity, CoolingTowerRenderer.State> {
	private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(NuclearRenderer.TEXTURE);

	public static class State extends BlockEntityRenderState {
		CoolingTowerBlock.Tower tower = CoolingTowerBlock.Tower.NATURAL;
		Direction facing = Direction.SOUTH;
		float angle;
		boolean formed;
	}

	public CoolingTowerRenderer(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 160;
	}

	@Override
	public void extractRenderState(CoolingTowerBlockEntity tower, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(tower, state, crumbling);
		state.tower = tower.tower();
		state.facing = tower.getBlockState().getValue(CoolingTowerBlock.FACING);
		state.angle = tower.fanAngle(partialTick);
		state.formed = tower.formed();
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.tower.fanHeight <= 0 || !state.formed) {
			return;
		}
		Direction along = state.facing.getClockWise();
		collector.submitCustomGeometry(pose, RENDER_TYPE, (p, consumer) -> {
			for (int i = 0; i < state.tower.fans.length; i++) {
				int fan = state.tower.fans[i];
				double cx = 0.5 + along.getStepX() * fan, cz = 0.5 + along.getStepZ() * fan, y = state.tower.fanHeight + 0.55;
				// Neighbouring fans turn a little out of step.
				double turn = Math.toRadians(state.angle + i * 23);
				box(p, consumer, cx - 0.45, y - 0.35, cz - 0.45, cx + 0.45, y + 0.25, cz + 0.45, NuclearRenderer.CARBON_STEEL);
				for (int b = 0; b < 7; b++) {
					double a = turn + b * Math.PI * 2 / 7;
					blade(p, consumer, cx, y, cz, a, 0.4, 4.8, 0.55);
				}
			}
		});
	}

	/** One blade: a thin pitched plate from r0 to r1 at angle a, both sides. */
	private static void blade(PoseStack.Pose p, VertexConsumer c, double cx, double y, double cz, double a, double r0, double r1, double w) {
		double ux = Math.cos(a), uz = Math.sin(a), vx = -uz, vz = ux;
		double pitch = 0.18;
		float[] uv = NuclearRenderer.PAINT_GREY;
		double[][] q = {
				{cx + ux * r0 - vx * w / 2, y - pitch, cz + uz * r0 - vz * w / 2},
				{cx + ux * r1 - vx * w / 2, y - pitch, cz + uz * r1 - vz * w / 2},
				{cx + ux * r1 + vx * w / 2, y + pitch, cz + uz * r1 + vz * w / 2},
				{cx + ux * r0 + vx * w / 2, y + pitch, cz + uz * r0 + vz * w / 2}};
		quad(p, c, q[0], q[1], q[2], q[3], uv, 0, 1, 0);
		quad(p, c, q[3], q[2], q[1], q[0], uv, 0, -1, 0);
	}

	private static void box(PoseStack.Pose p, VertexConsumer c, double x0, double y0, double z0, double x1, double y1, double z1, float[] uv) {
		quad(p, c, new double[] {x0, y1, z0}, new double[] {x0, y1, z1}, new double[] {x1, y1, z1}, new double[] {x1, y1, z0}, uv, 0, 1, 0);
		quad(p, c, new double[] {x0, y0, z1}, new double[] {x0, y0, z0}, new double[] {x1, y0, z0}, new double[] {x1, y0, z1}, uv, 0, -1, 0);
		quad(p, c, new double[] {x0, y1, z0}, new double[] {x1, y1, z0}, new double[] {x1, y0, z0}, new double[] {x0, y0, z0}, uv, 0, 0, -1);
		quad(p, c, new double[] {x1, y1, z1}, new double[] {x0, y1, z1}, new double[] {x0, y0, z1}, new double[] {x1, y0, z1}, uv, 0, 0, 1);
		quad(p, c, new double[] {x0, y1, z1}, new double[] {x0, y1, z0}, new double[] {x0, y0, z0}, new double[] {x0, y0, z1}, uv, -1, 0, 0);
		quad(p, c, new double[] {x1, y1, z0}, new double[] {x1, y1, z1}, new double[] {x1, y0, z1}, new double[] {x1, y0, z0}, uv, 1, 0, 0);
	}

	private static void quad(PoseStack.Pose p, VertexConsumer c, double[] a, double[] b, double[] d, double[] e, float[] uv, float nx, float ny, float nz) {
		vertex(p, c, a, uv[0], uv[1], nx, ny, nz);
		vertex(p, c, b, uv[0], uv[3], nx, ny, nz);
		vertex(p, c, d, uv[2], uv[3], nx, ny, nz);
		vertex(p, c, e, uv[2], uv[1], nx, ny, nz);
	}

	private static void vertex(PoseStack.Pose p, VertexConsumer c, double[] v, float u, float w, float nx, float ny, float nz) {
		c.addVertex(p, (float) v[0], (float) v[1], (float) v[2]).setColor(0xFFFFFFFF).setUv(u, w).setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(p, nx, ny, nz);
	}
}
