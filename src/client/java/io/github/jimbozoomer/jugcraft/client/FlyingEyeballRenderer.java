package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.FlyingEyeballBlock;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Flying Eyeball hovering in its block: it bobs ({@link FlyingEyeballBlock#bob}), beats its bat wings
 * ({@link FlyingEyeballBlock#flap}), swept back from its sides, and turns to stare at the nearest player within
 * {@link FlyingEyeballBlock#WATCH_RANGE} blocks, at most {@link FlyingEyeballBlock#TURN_SPEED} degrees a tick; with
 * nobody near, it looks idly about. Its iris is drawn glowing. Models from decor16_quads.json (tools/decor16_data.py):
 * the eyeball centred on (8, 8, 8) pixels with its iris to the north, and each wing hinged at the origin.
 */
public class FlyingEyeballRenderer implements BlockEntityRenderer<DecorationBlockEntity, FlyingEyeballRenderer.State> {
	private static final int FULL_BRIGHT = 0xF000F0;
	/** Where the wings are hinged, from the eye's middle (pixels), and how far they sweep back. */
	private static final float[] LEFT_HINGE = {-3.2F, 1.0F, 0.6F};
	private static final float[] RIGHT_HINGE = {3.2F, 1.0F, 0.6F};
	private static final float SWEEP = 28.0F;
	private static final float MAX_PITCH = 55.0F;
	/** Each eyeball's look as this client last left it: {yaw, pitch, game time}. */
	private final Map<DecorationBlockEntity, double[]> looks = new WeakHashMap<>();

	public static final class State extends BlockEntityRenderState {
		float bob;
		float flap;
		float yaw;
		float pitch;
	}

	public FlyingEyeballRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity eyeball, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(eyeball, state, crumbling);
		Level level = eyeball.getLevel();
		if (level == null) {
			return;
		}
		BlockPos pos = eyeball.getBlockPos();
		double now = level.getGameTime() + (double) partialTick;
		float time = (float) (now % 24000.0);
		state.bob = FlyingEyeballBlock.bob(pos, time);
		state.flap = FlyingEyeballBlock.flap(pos, time);
		Vec3 eye = Vec3.atBottomCenterOf(pos).add(0.0, (FlyingEyeballBlock.CENTRE_Y + state.bob) / 16, 0.0);
		Vec3 nearest = null;
		double best = FlyingEyeballBlock.WATCH_RANGE * FlyingEyeballBlock.WATCH_RANGE;
		for (Player player : level.players()) {
			Vec3 eyes = player.getEyePosition(partialTick);
			double distance = eyes.distanceToSqr(eye);
			if (!player.isSpectator() && distance <= best) {
				best = distance;
				nearest = eyes;
			}
		}
		float targetYaw;
		float targetPitch;
		if (nearest != null) {
			Vec3 to = nearest.subtract(eye);
			// The model looks north (-z); turning it by this about y points it along `to`.
			targetYaw = (float) Math.toDegrees(Math.atan2(-to.x, -to.z));
			targetPitch = Mth.clamp((float) Math.toDegrees(Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z))), -MAX_PITCH, MAX_PITCH);
		} else {
			float phase = (pos.hashCode() & 0xFF) / 40.0F;
			targetYaw = 50.0F * Mth.sin(time * 0.017F + phase);
			targetPitch = 12.0F * Mth.sin(time * 0.029F + phase);
		}
		float startYaw = targetYaw;
		float startPitch = targetPitch;
		double[] look = looks.computeIfAbsent(eyeball, e -> new double[] {startYaw, startPitch, now});
		float step = FlyingEyeballBlock.TURN_SPEED * (float) Math.max(0.0, Math.min(20.0, now - look[2]));
		look[0] = (float) look[0] + Mth.clamp(Mth.wrapDegrees(targetYaw - (float) look[0]), -step, step);
		look[1] = (float) look[1] + Mth.clamp(targetPitch - (float) look[1], -step, step);
		look[2] = now;
		state.yaw = (float) look[0];
		state.pitch = (float) look[1];
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel body = DecorQuads.get("flying_eyeball_body");
		QuadModel iris = DecorQuads.get("flying_eyeball_iris");
		QuadModel left = DecorQuads.get("flying_eyeball_wing_left");
		QuadModel right = DecorQuads.get("flying_eyeball_wing_right");
		pose.pushPose();
		pose.translate(0.5F, (FlyingEyeballBlock.CENTRE_Y + state.bob) / 16, 0.5F);
		pose.rotateDegrees(Axis.YP, state.yaw);
		// Each wing swept back and beating: the left's tip rises as it turns the negative way about z, the right's the positive.
		wing(pose, collector, left, LEFT_HINGE, SWEEP, -state.flap, state.lightCoords);
		wing(pose, collector, right, RIGHT_HINGE, -SWEEP, state.flap, state.lightCoords);
		pose.rotateDegrees(Axis.XP, state.pitch);
		pose.translate(-0.5F, -0.5F, -0.5F);
		if (body != null) {
			body.submit(pose, collector, state.lightCoords);
		}
		if (iris != null) {
			iris.submit(pose, collector, FULL_BRIGHT);
		}
		pose.popPose();
	}

	private static void wing(PoseStack pose, SubmitNodeCollector collector, @Nullable QuadModel wing, float[] hinge, float sweep, float flap,
			int light) {
		if (wing == null) {
			return;
		}
		pose.pushPose();
		pose.translate(hinge[0] / 16, hinge[1] / 16, hinge[2] / 16);
		pose.rotateDegrees(Axis.YP, sweep);
		pose.rotateDegrees(Axis.ZP, flap);
		wing.submit(pose, collector, light);
		pose.popPose();
	}
}
