package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.LurkingEyesBlock;
import io.github.jimbozoomer.jugcraft.agriculture.MourningAngelBlock;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Lurking Eyes: two glowing eyes on the face of the block they peer out of, at full brightness, each pair at
 * its own spot on the face. Only at night, only to a viewer at least {@link LurkingEyesBlock#HIDE_DISTANCE} away, and
 * not while they blink. The texture holds one eye, 6 by 4 pixels, at its top left.
 */
public class LurkingEyesRenderer implements BlockEntityRenderer<DecorationBlockEntity, LurkingEyesRenderer.State> {
	private static final RenderType EYES = RenderTypes.entityCutout(Jugcraft.id("textures/entity/lurking_eyes.png"));
	private static final int FULL_BRIGHT = 0xF000F0;
	/** An eye's size and the gap between the two, in pixels. */
	private static final float EYE_WIDTH = 3.0F;
	private static final float EYE_HEIGHT = 2.0F;
	private static final float GAP = 2.0F;

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		boolean showing;
		float offsetX;
		float offsetY;
	}

	public LurkingEyesRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity eyes, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(eyes, state, crumbling);
		BlockState block = eyes.getBlockState();
		state.showing = false;
		if (!(block.getBlock() instanceof LurkingEyesBlock) || eyes.getLevel() == null) {
			return;
		}
		state.facing = block.getValue(LurkingEyesBlock.FACING);
		double distance = camera.distanceTo(Vec3.atCenterOf(eyes.getBlockPos()));
		state.showing = LurkingEyesBlock.showing(MourningAngelBlock.night(eyes.getLevel()), distance)
				&& !LurkingEyesBlock.blinking(eyes.getBlockPos(), eyes.getLevel().getGameTime());
		int hash = eyes.getBlockPos().hashCode();
		state.offsetX = ((hash >> 4) & 7) - 3.5F;
		state.offsetY = ((hash >> 8) & 5) - 2.5F;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.showing) {
			return;
		}
		float cx = 8.0F + state.offsetX;
		float cy = 8.0F + state.offsetY;
		pose.pushPose();
		// Drawn for eyes looking north, on the face of the block to the south; then turned to the real facing.
		pose.translate(0.5F, 0.5F, 0.5F);
		switch (state.facing) {
			case UP -> pose.rotateDegrees(Axis.XP, 90);
			case DOWN -> pose.rotateDegrees(Axis.XP, -90);
			default -> pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		}
		pose.translate(-0.5F, -0.5F, -0.5F);
		collector.submitCustomGeometry(pose, EYES, (matrix, buffer) -> {
			float z = 15.9F / 16;
			eye(buffer, matrix, (cx - GAP / 2 - EYE_WIDTH) / 16, cy / 16, z);
			eye(buffer, matrix, (cx + GAP / 2) / 16, cy / 16, z);
		});
		pose.popPose();
	}

	/** One eye facing north, its left edge at x and its bottom at y. */
	private static void eye(VertexConsumer buffer, PoseStack.Pose matrix, float x, float y, float z) {
		float w = EYE_WIDTH / 16;
		float h = EYE_HEIGHT / 16;
		// Corners in the order the north faces of other renderers use; seen from the north, x runs right to left.
		float[][] corners = {{x, y + h}, {x, y}, {x + w, y}, {x + w, y + h}};
		float[][] uv = {{6.0F / 16, 0}, {6.0F / 16, 4.0F / 16}, {0, 4.0F / 16}, {0, 0}};
		for (int i = 0; i < 4; i++) {
			buffer.addVertex(matrix, corners[i][0], corners[i][1], z).setColor(0xFFFFFFFF).setUv(uv[i][0], uv[i][1])
					.setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(matrix, 0, 0, -1);
		}
	}
}
