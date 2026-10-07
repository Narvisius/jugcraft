package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.GiantFakeSpiderBlock;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Giant Fake Spider: a silk thread from the knot down {@link GiantFakeSpiderBlock#DROP} blocks to the spider,
 * the whole swinging slowly from the knot ({@link GiantFakeSpiderBlock#sway}) and the spider turning a little on its
 * thread.
 */
public class GiantFakeSpiderRenderer implements BlockEntityRenderer<DecorationBlockEntity, GiantFakeSpiderRenderer.State> {
	private static final RenderType SILK = RenderTypes.entitySolid(Jugcraft.id("textures/block/giant_fake_spider_silk.png"));
	/** The knot's underside, where the thread leaves it, in blocks above the block's floor. */
	private static final float KNOT = 11.5F / 16;
	private static final float THREAD = 0.25F / 16;

	public static final class State extends BlockEntityRenderState {
		int drop = 1;
		float swayX;
		float swayZ;
		float twist;
	}

	public GiantFakeSpiderRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity spider, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(spider, state, crumbling);
		BlockState block = spider.getBlockState();
		if (!(block.getBlock() instanceof GiantFakeSpiderBlock) || spider.getLevel() == null) {
			return;
		}
		state.drop = block.getValue(GiantFakeSpiderBlock.DROP);
		float time = spider.getLevel().getGameTime() % 24000 + partialTick;
		float[] sway = GiantFakeSpiderBlock.sway(spider.getBlockPos(), time);
		state.swayX = sway[0];
		state.swayZ = sway[1];
		state.twist = 25.0F * (float) Math.sin(time / (GiantFakeSpiderBlock.SWAY_PERIOD * 2.3) * Math.PI * 2 + spider.getBlockPos().hashCode());
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel body = DecorQuads.get("giant_fake_spider");
		float length = state.drop - 0.5F;
		int light = state.lightCoords;
		pose.pushPose();
		pose.translate(0.5F, KNOT, 0.5F);
		pose.rotateDegrees(Axis.XP, state.swayX);
		pose.rotateDegrees(Axis.ZP, state.swayZ);
		collector.submitCustomGeometry(pose, SILK, (matrix, buffer) -> {
			// Two crossed strips, each seen from both sides, so the thread shows from every side.
			thread(buffer, matrix, light, length, new float[][] {{-THREAD, 0}, {THREAD, 0}}, 0, -1);
			thread(buffer, matrix, light, length, new float[][] {{THREAD, 0}, {-THREAD, 0}}, 0, 1);
			thread(buffer, matrix, light, length, new float[][] {{0, THREAD}, {0, -THREAD}}, -1, 0);
			thread(buffer, matrix, light, length, new float[][] {{0, -THREAD}, {0, THREAD}}, 1, 0);
		});
		if (body != null) {
			pose.translate(0.0F, -length, 0.0F);
			pose.rotateDegrees(Axis.YP, state.twist);
			pose.translate(-0.5F, 0.0F, -0.5F);
			body.submit(pose, collector, light);
		}
		pose.popPose();
	}

	/**
	 * One strip of the thread from the knot down {@code length} blocks, between two (x, z) edge points given in the
	 * order that faces it toward its normal (nx, 0, nz).
	 */
	private static void thread(VertexConsumer buffer, PoseStack.Pose matrix, int light, float length, float[][] edge, float nx, float nz) {
		float[][] corners = {{edge[0][0], 0, edge[0][1]}, {edge[0][0], -length, edge[0][1]}, {edge[1][0], -length, edge[1][1]},
				{edge[1][0], 0, edge[1][1]}};
		for (float[] c : corners) {
			buffer.addVertex(matrix, c[0], c[1], c[2]).setColor(0xFFFFFFFF).setUv(0.5F, 0.5F)
					.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix, nx, 0, nz);
		}
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true; // The spider hangs blocks below its knot.
	}
}
