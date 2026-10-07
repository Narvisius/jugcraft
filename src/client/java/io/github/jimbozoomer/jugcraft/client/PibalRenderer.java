package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.Pibal;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;

/**
 * Draws a pibal (fall addition 29) from tools/hot_air_balloon_data.py's quads: a red latex balloon on its string, the
 * string swinging a little, lit a touch brighter than the sky round it so it can be followed in the dusk.
 */
public class PibalRenderer extends EntityRenderer<Pibal, PibalRenderer.State> {
	public static final class State extends EntityRenderState {
		float swing;
	}

	public PibalRenderer(EntityRendererProvider.Context context) {
		super(context);
		shadowRadius = 0.0F;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(Pibal pibal, State state, float partialTick) {
		super.extractRenderState(pibal, state, partialTick);
		state.swing = 6.0F * Mth.sin((pibal.tickCount + partialTick) * 0.11F + pibal.getId());
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel model = DecorQuads.get("pibal");
		if (model != null) {
			pose.pushPose();
			pose.translate(0.0F, 0.3F, 0.0F);
			pose.rotateDegrees(Axis.ZP, state.swing);
			int sky = (state.lightCoords >> 20) & 15;
			int block = Math.max((state.lightCoords >> 4) & 15, 6);
			model.submit(pose, collector, (sky << 20) | (block << 4));
			pose.popPose();
		}
		super.submit(state, pose, collector, camera);
	}
}
