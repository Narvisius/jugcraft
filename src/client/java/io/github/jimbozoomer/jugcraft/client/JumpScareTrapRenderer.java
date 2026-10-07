package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.JumpScareTrapBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JumpScareTrapBlockEntity;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a Jump-Scare Trap's lid and, when it goes off, its ghost: the lid bursts open on its back hinge and the ghost
 * shoots up on its spring, overshooting and wobbling as it settles, then sinks back and the lid drops shut. The time
 * each trap last changed phase is kept here, as this client saw it; a trap first seen mid-scare is drawn settled.
 */
public class JumpScareTrapRenderer implements BlockEntityRenderer<JumpScareTrapBlockEntity, JumpScareTrapRenderer.State> {
	/** The lid's hinge: its back bottom edge (pixels). */
	private static final float HINGE_Y = 11.0F;
	private static final float HINGE_Z = 15.0F;
	private static final float LID_OPEN = 115.0F;
	/** The crate's floor, and how far above it the spring throws the ghost's feet (pixels). */
	private static final float FLOOR = 1.0F;
	private static final float RISE = 15.0F;
	/** How long the ghost takes to sink back, and the lid to shut after it (ticks). */
	private static final float SINK_TICKS = 6.0F;
	private static final float SHUT_TICKS = 6.0F;
	private static final long SETTLED = 100;

	private final Map<JumpScareTrapBlockEntity, long[]> changes = new WeakHashMap<>();

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		float lid;
		float ghost = -1.0F;
		float wobble;
	}

	public JumpScareTrapRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(JumpScareTrapBlockEntity trap, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(trap, state, crumbling);
		BlockState block = trap.getBlockState();
		Level level = trap.getLevel();
		state.lid = 0.0F;
		state.ghost = -1.0F;
		if (!(block.getBlock() instanceof JumpScareTrapBlock) || level == null) {
			return;
		}
		state.facing = block.getValue(JumpScareTrapBlock.FACING);
		JumpScareTrapBlock.Phase phase = block.getValue(JumpScareTrapBlock.PHASE);
		long now = level.getGameTime();
		long[] change = changes.get(trap);
		if (change == null) {
			change = new long[] {phase.ordinal(), now - SETTLED};
			changes.put(trap, change);
		} else if (change[0] != phase.ordinal()) {
			change[0] = phase.ordinal();
			change[1] = now;
		}
		float t = Math.min(SETTLED, now - change[1]) + partialTick;
		if (phase == JumpScareTrapBlock.Phase.POPPED) {
			state.lid = t < 2.5F ? LID_OPEN * t / 2.5F : LID_OPEN + 8.0F * (float) Math.exp(-(t - 2.5F) / 4.0F) * Mth.sin((t - 2.5F) * 1.2F);
			float spring = 1.0F - (float) Math.exp(-t / 2.2F) * Mth.cos(t * 1.1F);
			state.ghost = FLOOR + RISE * spring;
			state.wobble = 12.0F * (float) Math.exp(-t / 10.0F) * Mth.sin(t * 0.9F);
		} else if (phase == JumpScareTrapBlock.Phase.RESETTING) {
			state.lid = t < SINK_TICKS ? LID_OPEN : LID_OPEN * Math.max(0.0F, 1.0F - (t - SINK_TICKS) / SHUT_TICKS);
			if (t < SINK_TICKS) {
				state.ghost = FLOOR + RISE * (1.0F - t / SINK_TICKS);
				state.wobble = 0.0F;
			}
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel lid = DecorQuads.get("jump_scare_lid");
		QuadModel ghost = DecorQuads.get("jump_scare_ghost");
		QuadModel spring = DecorQuads.get("jump_scare_spring");
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		if (lid != null) {
			pose.pushPose();
			pose.translate(0.0F, HINGE_Y / 16, HINGE_Z / 16);
			pose.rotateDegrees(Axis.XP, state.lid);
			pose.translate(0.0F, -HINGE_Y / 16, -HINGE_Z / 16);
			lid.submit(pose, collector, state.lightCoords);
			pose.popPose();
		}
		if (state.ghost >= 0.0F) {
			float length = state.ghost - FLOOR;
			if (spring != null && length > 0.5F) {
				pose.pushPose();
				pose.translate(0.0F, FLOOR / 16, 0.0F);
				pose.scale(1.0F, length / 16, 1.0F);
				spring.submit(pose, collector, state.lightCoords);
				pose.popPose();
			}
			if (ghost != null) {
				pose.pushPose();
				pose.translate(0.5F, state.ghost / 16, 0.5F);
				pose.rotateDegrees(Axis.ZP, state.wobble);
				pose.translate(-0.5F, 0.0F, -0.5F);
				ghost.submit(pose, collector, state.lightCoords);
				pose.popPose();
			}
		}
		pose.popPose();
	}
}
