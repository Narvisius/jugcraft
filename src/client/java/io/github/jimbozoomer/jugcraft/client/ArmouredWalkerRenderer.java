package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.walker.ArmouredWalker;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;

/**
 * Draws the Armoured Walker (batch 58) from the parts tools/armoured_walker.py exports
 * (assets/jugcraft/armoured_walker_quads.json), facing the way it walks; see {@link ArmouredWalkerParts}.
 */
public class ArmouredWalkerRenderer extends EntityRenderer<ArmouredWalker, ArmouredWalkerRenderer.State> {
	public static final class State extends EntityRenderState {
		float yRot;
		float stride;
		float ram;
	}

	public ArmouredWalkerRenderer(EntityRendererProvider.Context context) {
		super(context);
		shadowRadius = 1.5F;
	}

	/** The arms and barrel reach past the hitbox. */
	@Override
	protected AABB getBoundingBoxForCulling(ArmouredWalker walker, float partialTick) {
		return walker.getBoundingBox().inflate(1.5, 0.5, 1.5);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(ArmouredWalker walker, State state, float partialTick) {
		super.extractRenderState(walker, state, partialTick);
		state.yRot = walker.getYRot(partialTick);
		state.stride = walker.stride();
		state.ram = ArmouredWalkerParts.ram(walker.sinceSwing() + partialTick);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		pose.pushPose();
		pose.rotateDegrees(Axis.YP, -state.yRot);
		ArmouredWalkerParts.draw("armoured_walker", pose, collector, state.lightCoords, state.stride, state.ram);
		pose.popPose();
		super.submit(state, pose, collector, camera);
	}
}
