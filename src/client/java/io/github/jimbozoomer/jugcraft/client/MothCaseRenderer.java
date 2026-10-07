package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.MothCaseBlock;
import io.github.jimbozoomer.jugcraft.agriculture.MourningAngelBlock;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the moths pinned in a Moth Display Case: a big one in the middle and two smaller below either side
 * (tools/decor17_data.py MOTHS), wings spread flat by day. At night they come alive, each slowly opening and closing its
 * wings out from the backing, out of step with the others.
 */
public class MothCaseRenderer implements BlockEntityRenderer<DecorationBlockEntity, MothCaseRenderer.State> {
	/** Each moth: {x, y, z, scale}, pixels, for a case facing north. */
	private static final float[][] MOTHS = {{8.0F, 8.6F, 15.3F, 1.0F}, {4.2F, 5.4F, 15.3F, 0.55F}, {11.8F, 5.4F, 15.3F, 0.55F}};
	private static final float FLUTTER = 55.0F;

	public static final class State extends BlockEntityRenderState {
		String moth = "luna";
		Direction facing = Direction.NORTH;
		boolean night;
		float time;
	}

	public MothCaseRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity box, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(box, state, crumbling);
		BlockState block = box.getBlockState();
		if (!block.hasProperty(MothCaseBlock.MOTH) || box.getLevel() == null) {
			return;
		}
		state.moth = block.getValue(MothCaseBlock.MOTH).getSerializedName();
		state.facing = block.getValue(MothCaseBlock.FACING);
		state.night = MourningAngelBlock.night(box.getLevel());
		state.time = (box.getLevel().getGameTime() % 24000) + partialTick;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel body = DecorQuads.get("moth_" + state.moth + "_body");
		QuadModel left = DecorQuads.get("moth_" + state.moth + "_wing_left");
		QuadModel right = DecorQuads.get("moth_" + state.moth + "_wing_right");
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		for (int i = 0; i < MOTHS.length; i++) {
			float[] m = MOTHS[i];
			float open = state.night ? FLUTTER * (0.5F + 0.5F * Mth.sin(state.time * 0.11F + i * 2.3F)) : 0.0F;
			pose.pushPose();
			pose.translate(m[0] / 16, m[1] / 16, m[2] / 16);
			pose.scale(m[3], m[3], m[3]);
			if (body != null) {
				body.submit(pose, collector, state.lightCoords);
			}
			wing(left, open, pose, collector, state.lightCoords);
			wing(right, -open, pose, collector, state.lightCoords);
			pose.popPose();
		}
		pose.popPose();
	}

	/** A wing folded out from the backing (toward the glass) about the body's line by {@code angle} degrees. */
	private static void wing(@Nullable QuadModel wing, float angle, PoseStack pose, SubmitNodeCollector collector, int light) {
		if (wing == null) {
			return;
		}
		pose.pushPose();
		pose.rotateDegrees(Axis.YP, angle);
		wing.submit(pose, collector, light);
		pose.popPose();
	}
}
