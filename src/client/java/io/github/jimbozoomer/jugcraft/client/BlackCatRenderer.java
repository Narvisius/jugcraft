package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.BlackCatBlock;
import io.github.jimbozoomer.jugcraft.agriculture.BlackCatBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.MourningAngelBlock;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Black Cat Figure's tail and eyes: sitting, the tail curls round its paws and swishes
 * ({@link BlackCatBlock#swish}); hissing, it stands straight up. At night its eyes glow green, at full brightness,
 * wherever its head is in that pose. Coordinates are in pixels, for a cat facing north.
 */
public class BlackCatRenderer implements BlockEntityRenderer<BlackCatBlockEntity, BlackCatRenderer.State> {
	private static final RenderType EYES = RenderTypes.entityCutout(Jugcraft.id("textures/entity/black_cat_eyes.png"));
	private static final int FULL_BRIGHT = 0xF000F0;
	/** The root of the tail, about which it swishes. */
	private static final float TAIL_X = 8.0F;
	private static final float TAIL_Z = 11.5F;
	/** The eyes: {left x, right x, bottom y, front z} sitting and hissing. */
	private static final float[] SITTING_EYES = {6.25F, 8.75F, 9.25F, 3.95F};
	private static final float[] HISSING_EYES = {6.25F, 8.75F, 6.25F, 1.95F};

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		boolean hissing;
		boolean night;
		float swish;
	}

	public BlackCatRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(BlackCatBlockEntity cat, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(cat, state, crumbling);
		BlockState block = cat.getBlockState();
		if (!(block.getBlock() instanceof BlackCatBlock) || cat.getLevel() == null) {
			return;
		}
		state.facing = block.getValue(BlackCatBlock.FACING);
		state.hissing = block.getValue(BlackCatBlock.HISSING);
		state.night = MourningAngelBlock.night(cat.getLevel());
		state.swish = BlackCatBlock.swish(cat.getBlockPos(), state.hissing, cat.getLevel().getGameTime() % 24000 + partialTick);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel tail = DecorQuads.get(state.hissing ? "black_cat_tail_up" : "black_cat_tail");
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		if (tail != null) {
			pose.pushPose();
			pose.translate(TAIL_X / 16, 0.0F, TAIL_Z / 16);
			pose.rotateDegrees(Axis.YP, state.swish);
			pose.translate(-TAIL_X / 16, 0.0F, -TAIL_Z / 16);
			tail.submit(pose, collector, state.lightCoords);
			pose.popPose();
		}
		if (state.night) {
			float[] eyes = state.hissing ? HISSING_EYES : SITTING_EYES;
			collector.submitCustomGeometry(pose, EYES, (matrix, buffer) -> {
				float z = eyes[3] / 16;
				float y0 = eyes[2] / 16;
				float y1 = (eyes[2] + 1.0F) / 16;
				for (float x : new float[] {eyes[0], eyes[1]}) {
					float x0 = x / 16;
					float x1 = (x + 1.0F) / 16;
					DecorDraw.quad(buffer, matrix, new float[][] {{x0, y0, z, 1, 1}, {x0, y1, z, 1, 0}, {x1, y1, z, 0, 0}, {x1, y0, z, 0, 1}},
							0, 0, -1, 0xFFFFFFFF, FULL_BRIGHT);
				}
			});
		}
		pose.popPose();
	}
}
