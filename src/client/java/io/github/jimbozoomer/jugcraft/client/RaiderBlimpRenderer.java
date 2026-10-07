package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.raiders.RaiderBlimp;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/**
 * Draws the Raider Blimp (batch 57): the zeppelin's shape at {@value #SCALE} of its size in raider canvas
 * (assets/jugcraft/raider_quads.json, tools/raiders.py), its propellers always turning. Dying, it noses down.
 */
public class RaiderBlimpRenderer extends EntityRenderer<RaiderBlimp, RaiderBlimpRenderer.State> {
	/** The zeppelin's propeller hubs (ZeppelinRenderer) at the blimp's scale, BLIMP_SCALE in tools/raiders.py. */
	private static final float SCALE = 0.55F;
	private static final float[][] PROPS = {{-34 * SCALE, 26 * SCALE, -19 * SCALE}, {34 * SCALE, 26 * SCALE, -19 * SCALE}};
	private static final float PROP_SPIN = 47.0F;

	public static final class State extends EntityRenderState {
		float yRot;
		float dying;
	}

	public RaiderBlimpRenderer(EntityRendererProvider.Context context) {
		super(context);
		shadowRadius = 1.6F;
	}

	@Override
	protected AABB getBoundingBoxForCulling(RaiderBlimp blimp, float partialTick) {
		return blimp.getBoundingBox().inflate(3.0, 0.5, 3.0);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(RaiderBlimp blimp, State state, float partialTick) {
		super.extractRenderState(blimp, state, partialTick);
		state.yRot = Mth.rotLerp(partialTick, blimp.yBodyRotO, blimp.yBodyRot);
		state.dying = blimp.deathTime > 0 ? Math.min(1.0F, (blimp.deathTime + partialTick) / 20.0F) : 0.0F;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel body = DecorQuads.get("raider_blimp_body");
		QuadModel propeller = DecorQuads.get("raider_blimp_propeller");
		pose.pushPose();
		pose.rotateDegrees(Axis.YP, -state.yRot);
		if (state.dying > 0) {
			pose.rotateDegrees(Axis.XP, state.dying * 35.0F);
		}
		if (body != null) {
			body.submit(pose, collector, state.lightCoords);
		}
		if (propeller != null) {
			float spin = state.dying > 0 ? 0.0F : state.ageInTicks * PROP_SPIN;
			for (float[] hub : PROPS) {
				pose.pushPose();
				pose.translate(hub[0] / 16.0F, hub[1] / 16.0F, hub[2] / 16.0F);
				pose.rotateDegrees(Axis.ZP, hub[0] > 0 ? spin : -spin);
				propeller.submit(pose, collector, state.lightCoords);
				pose.popPose();
			}
		}
		pose.popPose();
		super.submit(state, pose, collector, camera);
	}
}
