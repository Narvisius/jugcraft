package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.Pinata;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;

/**
 * Draws a piñata (fall addition 28) from the quads tools/pinata_data.py writes (assets/jugcraft/pinata_quads.json): its
 * rope from the knot under the block it hangs from, and its body, torn once it has taken half its hits. It sways
 * gently, and each swing at it sets it swinging on its rope (harder for a hit than a glancing blow), the swing dying
 * away over a couple of seconds.
 */
public class PinataRenderer extends EntityRenderer<Pinata, PinataRenderer.State> {
	/** When each piñata was last swung at (client ticks), its swing count then, and how hard. */
	private static final Map<Pinata, float[]> SWINGS = new WeakHashMap<>();

	public static final class State extends EntityRenderState {
		Pinata.Kind kind = Pinata.Kind.PUMPKIN;
		boolean torn;
		float swingX;
		float swingZ;
	}

	public PinataRenderer(EntityRendererProvider.Context context) {
		super(context);
		shadowRadius = 0.0F;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(Pinata pinata, State state, float partialTick) {
		super.extractRenderState(pinata, state, partialTick);
		state.kind = pinata.kind();
		state.torn = pinata.torn();
		float now = pinata.tickCount + partialTick;
		float[] swing = SWINGS.computeIfAbsent(pinata, p -> new float[] {-1000.0F, p.swings(), 0.0F});
		if (pinata.swings() != (int) swing[1]) {
			swing[0] = now;
			swing[1] = pinata.swings();
			swing[2] = pinata.strongSwing() ? 28.0F : 10.0F;
		}
		float since = now - swing[0];
		float amplitude = swing[2] * (float) Math.exp(-since / 22.0F);
		// The way it swings turns a little with each swing, so it doesn't swing the same way every time.
		float heading = swing[1] * 2.1F;
		float sway = 1.5F * Mth.sin(now * 0.05F);
		float swingNow = amplitude * Mth.sin(since * 0.32F);
		state.swingX = swingNow * Mth.cos(heading) + sway;
		state.swingZ = swingNow * Mth.sin(heading);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel body = DecorQuads.get("pinata_" + state.kind.id + (state.torn ? "_torn" : ""));
		QuadModel rope = DecorQuads.get(state.kind == Pinata.Kind.STAR ? "pinata_rope_star" : "pinata_rope");
		pose.pushPose();
		// Swing about the knot the rope is tied with.
		pose.translate(0.0F, (float) Pinata.DROP, 0.0F);
		pose.rotateDegrees(Axis.XP, state.swingX);
		pose.rotateDegrees(Axis.ZP, state.swingZ);
		if (rope != null) {
			rope.submit(pose, collector, state.lightCoords);
		}
		if (body != null) {
			body.submit(pose, collector, state.lightCoords);
		}
		pose.popPose();
		super.submit(state, pose, collector, camera);
	}
}
