package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.CreepyDollBlock;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Creepy Doll's head, which never turns while it is watched. This client remembers when it last saw each
 * doll (on screen, within 32 blocks); when it looks back after {@link CreepyDollBlock#UNSEEN_TICKS} ticks or more,
 * the head has turned ({@link CreepyDollBlock#glance}): toward the viewer, or now and then far off to one side.
 */
public class CreepyDollRenderer implements BlockEntityRenderer<DecorationBlockEntity, CreepyDollRenderer.State> {
	/** The pivot of the head (the neck), in pixels. */
	private static final float NECK_Y = 8.0F;
	private static final float NECK_Z = 8.5F;
	/** How far off the middle of the view the doll may be and still count as seen (cosine of the angle). */
	private static final double SEEN_COSINE = Math.cos(Math.toRadians(50.0));
	/** Each doll's head as this client last left it: {yaw, game time last seen, glances}. */
	private final Map<DecorationBlockEntity, double[]> heads = new WeakHashMap<>();

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		float yaw;
	}

	public CreepyDollRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DecorationBlockEntity doll, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(doll, state, crumbling);
		BlockState block = doll.getBlockState();
		if (!(block.getBlock() instanceof CreepyDollBlock) || doll.getLevel() == null) {
			return;
		}
		state.facing = block.getValue(CreepyDollBlock.FACING);
		Vec3 head = Vec3.atBottomCenterOf(doll.getBlockPos()).add(0.0, NECK_Y / 16, 0.0);
		Vec3 toDoll = head.subtract(camera);
		Entity viewer = Minecraft.getInstance().getCameraEntity();
		boolean seen = toDoll.length() < 32.0 && (viewer == null || toDoll.normalize().dot(viewer.getViewVector(partialTick)) > SEEN_COSINE);
		long now = doll.getLevel().getGameTime();
		double[] look = heads.computeIfAbsent(doll, d -> new double[] {0.0, now, 0.0});
		if (seen) {
			if (now - (long) look[1] > CreepyDollBlock.UNSEEN_TICKS) {
				look[2]++;
				float toward = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-toDoll.x, toDoll.z)) - state.facing.toYRot() + 180.0F);
				look[0] = CreepyDollBlock.glance(doll.getBlockPos(), (int) look[2], toward);
			}
			look[1] = now;
		}
		state.yaw = (float) look[0];
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel head = DecorQuads.get("creepy_doll_head");
		if (head == null) {
			return;
		}
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		pose.translate(0.5F, NECK_Y / 16, NECK_Z / 16);
		pose.rotateDegrees(Axis.YP, -state.yaw);
		pose.translate(-0.5F, -NECK_Y / 16, -NECK_Z / 16);
		head.submit(pose, collector, state.lightCoords);
		pose.popPose();
	}
}
