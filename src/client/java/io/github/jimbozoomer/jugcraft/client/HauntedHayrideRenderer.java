package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.HauntedHayride;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Draws the Haunted Hayride: a plank wagon on four spoked wheels, hay bales along its sides for seats and a jack
 * o'lantern on a post at its front, lit (at full brightness). The wagon turns and tips with its rails, as a minecart
 * does; its quads lie along x, about the middle of the block.
 */
public class HauntedHayrideRenderer extends EntityRenderer<HauntedHayride, HauntedHayrideRenderer.State> {
	private static final int FULL_BRIGHT = 0xF000F0;

	public static final class State extends EntityRenderState {
		float yRot;
		float xRot;
	}

	public HauntedHayrideRenderer(EntityRendererProvider.Context context) {
		super(context);
		shadowRadius = 0.7F;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(HauntedHayride ride, State state, float partialTick) {
		super.extractRenderState(ride, state, partialTick);
		state.yRot = ride.getYRot(partialTick);
		state.xRot = ride.getViewXRot(partialTick);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel wagon = DecorQuads.get("hayride_wagon");
		QuadModel lantern = DecorQuads.get("hayride_lantern");
		pose.pushPose();
		pose.rotateDegrees(Axis.YP, 180.0F - state.yRot);
		pose.rotateDegrees(Axis.ZP, -state.xRot);
		pose.translate(-0.5F, 0.0F, -0.5F);
		if (wagon != null) {
			wagon.submit(pose, collector, state.lightCoords);
		}
		if (lantern != null) {
			lantern.submit(pose, collector, FULL_BRIGHT);
		}
		pose.popPose();
		super.submit(state, pose, collector, camera);
	}
}
