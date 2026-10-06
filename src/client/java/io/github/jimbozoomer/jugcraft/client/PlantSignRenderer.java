package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.nuclear.PlantSignBlock;
import io.github.jimbozoomer.jugcraft.nuclear.PlantSignBlockEntity;
import net.minecraft.client.gui.Font;
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
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

/**
 * Building name signs and room plaques (nuclear/PlantSignBlock): a dark brushed panel with an aluminium edge, hung on
 * the wall behind the block and as wide as its name, the letters lit so they read at night.
 */
public class PlantSignRenderer implements BlockEntityRenderer<PlantSignBlockEntity, PlantSignRenderer.State> {
	private static final RenderType PANEL = RenderTypes.entityCutout(NuclearRenderer.TEXTURE);
	private final Font font;

	public static class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		boolean big;
		FormattedCharSequence text = FormattedCharSequence.EMPTY;
		int width;
		int light;
	}

	public PlantSignRenderer(BlockEntityRendererProvider.Context context) {
		this.font = context.font();
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
	public void extractRenderState(PlantSignBlockEntity sign, State state, float partialTick, Vec3 cameraPos,
			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(sign, state, crumbling);
		var blockState = sign.getBlockState();
		if (!(blockState.getBlock() instanceof PlantSignBlock block)) {
			return;
		}
		state.facing = blockState.getValue(PlantSignBlock.FACING);
		state.big = block.big();
		Component name = Component.translatable("sign.jugcraft." + blockState.getValue(PlantSignBlock.LABEL).getSerializedName());
		state.text = name.getVisualOrderText();
		state.width = font.width(name);
		state.light = sign.getLevel() == null ? LightCoordsUtil.FULL_BRIGHT : LightCoordsUtil.getLightCoords(sign.getLevel(), sign.getBlockPos());
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		// Text pixels per block, the panel's margin and height (blocks), its thickness.
		float scale = state.big ? 1 / 10F : 1 / 44F;
		float margin = state.big ? 0.45F : 0.06F;
		if (!state.big) {
			// A room plaque stays inside its own block (at most 0.86 wide), the lettering smaller for long names.
			scale = Math.min(scale, (0.86F - 2 * margin) / Math.max(1, state.width));
		}
		float height = state.big ? 1.35F : 0.34F;
		float thick = state.big ? 0.12F : 0.04F;
		float half = state.width * scale / 2 + margin;
		pose.pushPose();
		pose.translate(0.5, 0.5, 0.5);
		pose.mulPose(new org.joml.Matrix4f().rotationY((float) Math.toRadians(-state.facing.toYRot())));
		// Local frame: x to a reader's right, y up, z out from the wall; the wall is at z = -0.5.
		float back = -0.5F, front = -0.5F + thick, top = height / 2, bottom = -height / 2;
		int light = state.light;
		collector.submitCustomGeometry(pose, PANEL, (p, consumer) -> {
			float[] face = NuclearRenderer.SIGN_PANEL, edge = NuclearRenderer.SIGN_EDGE;
			quad(consumer, p, -half, top, front, -half, bottom, front, half, bottom, front, half, top, front, 0, 0, 1, face, light);
			float e = state.big ? 0.06F : 0.015F;
			// The aluminium frame round the face, and the panel's edges.
			quad(consumer, p, -half, top, front + 0.002F, -half, top - e, front + 0.002F, half, top - e, front + 0.002F, half, top, front + 0.002F, 0, 0, 1, edge, light);
			quad(consumer, p, -half, bottom + e, front + 0.002F, -half, bottom, front + 0.002F, half, bottom, front + 0.002F, half, bottom + e, front + 0.002F, 0, 0, 1, edge, light);
			quad(consumer, p, -half, top, front + 0.002F, -half, bottom, front + 0.002F, -half + e, bottom, front + 0.002F, -half + e, top, front + 0.002F, 0, 0, 1, edge, light);
			quad(consumer, p, half - e, top, front + 0.002F, half - e, bottom, front + 0.002F, half, bottom, front + 0.002F, half, top, front + 0.002F, 0, 0, 1, edge, light);
			quad(consumer, p, -half, top, back, -half, top, front, half, top, front, half, top, back, 0, 1, 0, edge, light);
			quad(consumer, p, -half, bottom, front, -half, bottom, back, half, bottom, back, half, bottom, front, 0, -1, 0, edge, light);
			quad(consumer, p, -half, top, back, -half, bottom, back, -half, bottom, front, -half, top, front, -1, 0, 0, edge, light);
			quad(consumer, p, half, top, front, half, bottom, front, half, bottom, back, half, top, back, 1, 0, 0, edge, light);
		});
		pose.translate(0, 0, front + 0.006F);
		pose.scale(scale, -scale, scale);
		collector.submitText(pose, -state.width / 2F, -4.5F, state.text, false, Font.DisplayMode.POLYGON_OFFSET, LightCoordsUtil.FULL_BRIGHT,
				0xFFF2F2EE, 0, 0);
		pose.popPose();
	}

	private static void quad(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1,
			float x2, float y2, float z2, float x3, float y3, float z3, float nx, float ny, float nz, float[] uv, int light) {
		vertex(consumer, pose, x0, y0, z0, uv[0], uv[1], nx, ny, nz, light);
		vertex(consumer, pose, x1, y1, z1, uv[0], uv[3], nx, ny, nz, light);
		vertex(consumer, pose, x2, y2, z2, uv[2], uv[3], nx, ny, nz, light);
		vertex(consumer, pose, x3, y3, z3, uv[2], uv[1], nx, ny, nz, light);
	}

	private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, float nx, float ny, float nz,
			int light) {
		consumer.addVertex(pose, x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
	}
}
