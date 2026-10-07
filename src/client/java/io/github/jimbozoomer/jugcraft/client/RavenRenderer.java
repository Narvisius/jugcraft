package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.RavenPerchBlock;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the raven on its perch: its body and tail, its head turned toward the nearest player within
 * {@link RavenPerchBlock#WATCH_RANGE} blocks (slowly, at most {@link RavenPerchBlock#MAX_TURN} degrees), and its
 * wings, lifted a little while it ruffles and beating hard for a moment when it is used. Coordinates are in pixels,
 * for a raven facing north.
 */
public class RavenRenderer implements BlockEntityRenderer<DecorationBlockEntity, RavenRenderer.State> {
	/** The neck, about which the head turns. */
	private static final float NECK_Y = 14.25F;
	private static final float NECK_Z = 7.0F;
	/** Where each wing joins the body (its top edge), x for the left and right wings. */
	private static final float WING_Y = 14.5F;
	private static final float LEFT_X = 6.5F;
	private static final float RIGHT_X = 9.5F;
	private static final float TURN_SPEED = 6.0F;
	/** Each raven's head as this client last left it: {yaw, game time}. */
	private final Map<DecorationBlockEntity, double[]> heads = new WeakHashMap<>();

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		float yaw;
		float wings;
	}

	public RavenRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity raven, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(raven, state, crumbling);
		BlockState block = raven.getBlockState();
		Level level = raven.getLevel();
		if (!(block.getBlock() instanceof RavenPerchBlock) || level == null) {
			return;
		}
		state.facing = block.getValue(RavenPerchBlock.FACING);
		Vec3 head = Vec3.atBottomCenterOf(raven.getBlockPos()).add(0.0, NECK_Y / 16, 0.0);
		Vec3 nearest = null;
		double best = RavenPerchBlock.WATCH_RANGE * RavenPerchBlock.WATCH_RANGE;
		for (Player player : level.players()) {
			Vec3 eyes = player.getEyePosition(partialTick);
			double distance = eyes.distanceToSqr(head);
			if (!player.isSpectator() && distance <= best) {
				best = distance;
				nearest = eyes;
			}
		}
		float target = 0.0F;
		if (nearest != null) {
			float angle = (float) Math.toDegrees(Math.atan2(nearest.x - head.x, -(nearest.z - head.z)));
			target = Mth.clamp(Mth.wrapDegrees(angle - state.facing.toYRot() + 180.0F), -RavenPerchBlock.MAX_TURN, RavenPerchBlock.MAX_TURN);
		}
		double now = level.getGameTime() + (double) partialTick;
		float start = target;
		double[] look = heads.computeIfAbsent(raven, r -> new double[] {start, now});
		float elapsed = (float) Math.max(0.0, Math.min(20.0, now - look[1]));
		look[0] = (float) look[0] + Mth.clamp(target - (float) look[0], -TURN_SPEED * elapsed, TURN_SPEED * elapsed);
		look[1] = now;
		state.yaw = (float) look[0];
		long sinceFlap = level.getGameTime() - raven.marked();
		if (sinceFlap >= 0 && sinceFlap < RavenPerchBlock.FLAP_TICKS) {
			state.wings = 20.0F + 45.0F * Math.abs(Mth.sin((float) now * 1.6F));
		} else if (RavenPerchBlock.ruffling(raven.getBlockPos(), level.getGameTime())) {
			state.wings = 12.0F + 4.0F * Mth.sin((float) now * 3.0F);
		} else {
			state.wings = 0.0F;
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel body = DecorQuads.get("raven_body");
		QuadModel head = DecorQuads.get("raven_head");
		QuadModel left = DecorQuads.get("raven_left_wing");
		QuadModel right = DecorQuads.get("raven_right_wing");
		int light = state.lightCoords;
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		if (body != null) {
			body.submit(pose, collector, light);
		}
		if (left != null) {
			pose.pushPose();
			pose.translate(LEFT_X / 16, WING_Y / 16, 0.0F);
			pose.rotateDegrees(Axis.ZP, -state.wings);
			pose.translate(-LEFT_X / 16, -WING_Y / 16, 0.0F);
			left.submit(pose, collector, light);
			pose.popPose();
		}
		if (right != null) {
			pose.pushPose();
			pose.translate(RIGHT_X / 16, WING_Y / 16, 0.0F);
			pose.rotateDegrees(Axis.ZP, state.wings);
			pose.translate(-RIGHT_X / 16, -WING_Y / 16, 0.0F);
			right.submit(pose, collector, light);
			pose.popPose();
		}
		if (head != null) {
			pose.translate(0.5F, NECK_Y / 16, NECK_Z / 16);
			pose.rotateDegrees(Axis.YP, -state.yaw);
			pose.translate(-0.5F, -NECK_Y / 16, -NECK_Z / 16);
			head.submit(pose, collector, light);
		}
		pose.popPose();
	}
}
