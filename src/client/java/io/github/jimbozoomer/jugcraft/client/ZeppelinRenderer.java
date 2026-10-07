package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.airship.Zeppelin;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;

/**
 * Draws the zeppelin from the quads tools/zeppelin.py exports (assets/jugcraft/zeppelin_quads.json): the envelope and
 * gondola facing the way the airship flies, and a propeller behind each engine pod, spinning while the engines work.
 */
public class ZeppelinRenderer extends EntityRenderer<Zeppelin, ZeppelinRenderer.State> {
	/** Propeller hubs (pixels, before turning with the airship): keep in sync with PROPS in tools/zeppelin.py. */
	private static final float[][] PROPS = {{-34, 26, -19}, {34, 26, -19}};
	/** Degrees a propeller turns each tick while the engines work. */
	private static final float PROP_SPIN = 47.0F;

	public static final class State extends EntityRenderState {
		float yRot;
		boolean engine;
	}

	public ZeppelinRenderer(EntityRendererProvider.Context context) {
		super(context);
		shadowRadius = 2.5F;
	}

	/** The envelope is much longer than the hitbox. */
	@Override
	protected AABB getBoundingBoxForCulling(Zeppelin zeppelin, float partialTick) {
		return zeppelin.getBoundingBox().inflate(5.0, 0.5, 5.0);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(Zeppelin zeppelin, State state, float partialTick) {
		super.extractRenderState(zeppelin, state, partialTick);
		state.yRot = zeppelin.getYRot(partialTick);
		state.engine = zeppelin.engineOn();
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel body = DecorQuads.get("zeppelin_body");
		QuadModel propeller = DecorQuads.get("zeppelin_propeller");
		pose.pushPose();
		pose.rotateDegrees(Axis.YP, -state.yRot);
		if (body != null) {
			body.submit(pose, collector, state.lightCoords);
		}
		if (propeller != null) {
			float spin = state.engine ? state.ageInTicks * PROP_SPIN : 0.0F;
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
