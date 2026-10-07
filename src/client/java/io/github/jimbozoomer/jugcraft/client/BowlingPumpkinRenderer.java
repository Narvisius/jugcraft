package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.BowlingPumpkin;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;

/**
 * Draws a rolling Bowling Pumpkin turning over the way it rolls, as far as it has rolled ({@link BowlingPumpkin#spin},
 * kept by the entity on the client), from the quads of its item model.
 */
public class BowlingPumpkinRenderer extends EntityRenderer<BowlingPumpkin, BowlingPumpkinRenderer.State> {
	public static final class State extends EntityRenderState {
		float spin;
		float heading;
	}

	public BowlingPumpkinRenderer(EntityRendererProvider.Context context) {
		super(context);
		shadowRadius = BowlingPumpkin.RADIUS;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(BowlingPumpkin pumpkin, State state, float partialTick) {
		super.extractRenderState(pumpkin, state, partialTick);
		state.spin = Mth.lerp(partialTick, pumpkin.spinO, pumpkin.spin);
		state.heading = pumpkin.heading;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel model = DecorQuads.get("bowling_pumpkin");
		if (model != null) {
			pose.pushPose();
			pose.translate(0.0F, BowlingPumpkin.RADIUS, 0.0F);
			pose.rotateDegrees(Axis.YP, (float) Math.toDegrees(state.heading));
			pose.rotateDegrees(Axis.XP, (float) Math.toDegrees(state.spin));
			pose.translate(-0.5F, -BowlingPumpkin.RADIUS, -0.5F);
			model.submit(pose, collector, state.lightCoords);
			pose.popPose();
		}
		super.submit(state, pose, collector, camera);
	}
}
