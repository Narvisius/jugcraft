package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.raiders.RaiderWalker;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/**
 * Draws the Raider Walker (batch 57): the Armoured Walker's parts (batch 58, the owner's model) in raider paint
 * (assets/jugcraft/raider_quads.json, tools/raiders.py), drawn and animated as {@link ArmouredWalkerParts} does. Its
 * piston rams when it strikes; dying, it topples over sideways.
 */
public class RaiderWalkerRenderer extends EntityRenderer<RaiderWalker, RaiderWalkerRenderer.State> {
	public static final class State extends EntityRenderState {
		float yRot;
		float stride;
		float ram;
		float dying;
	}

	public RaiderWalkerRenderer(EntityRendererProvider.Context context) {
		super(context);
		shadowRadius = 1.5F;
	}

	@Override
	protected AABB getBoundingBoxForCulling(RaiderWalker walker, float partialTick) {
		return walker.getBoundingBox().inflate(1.5, 0.5, 1.5);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(RaiderWalker walker, State state, float partialTick) {
		super.extractRenderState(walker, state, partialTick);
		state.yRot = Mth.rotLerp(partialTick, walker.yBodyRotO, walker.yBodyRot);
		state.stride = walker.stride();
		state.ram = ArmouredWalkerParts.ram(walker.sinceSwing() + partialTick);
		state.dying = walker.deathTime > 0 ? Math.min(1.0F, (walker.deathTime + partialTick) / 20.0F) : 0.0F;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		pose.pushPose();
		pose.rotateDegrees(Axis.YP, -state.yRot);
		if (state.dying > 0) {
			pose.rotateDegrees(Axis.ZP, state.dying * state.dying * 80.0F);
		}
		ArmouredWalkerParts.draw("raider_walker", pose, collector, state.lightCoords, state.stride, state.ram);
		pose.popPose();
		super.submit(state, pose, collector, camera);
	}
}
