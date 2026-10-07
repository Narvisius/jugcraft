package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.GrandfatherClockBlock;
import io.github.jimbozoomer.jugcraft.agriculture.GrandfatherClockBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Grandfather Clock's moving parts, from its lower half, for a clock facing north (decor19_quads.json): the hour
 * and minute hands on the dial set to the overworld clock ({@link GrandfatherClockBlock#clockHour},
 * {@link GrandfatherClockBlock#minutes}), the painted moon in the arch for tonight's phase ({@code clock_moon_0} to
 * {@code _7}), the pendulum swinging behind the case's window, and at midnight the pale face at the glass.
 */
public class GrandfatherClockRenderer implements BlockEntityRenderer<GrandfatherClockBlockEntity, GrandfatherClockRenderer.State> {
	/** The dial's centre and the pendulum's pivot, in pixels from the lower half's corner. */
	private static final float[] DIAL = {8.0F, 22.5F, 2.4F};
	private static final float[] PIVOT = {8.0F, 15.0F, 4.0F};
	private static final float SWING = 12.0F;

	public static final class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		float hourAngle;
		float minuteAngle;
		int phase;
		float swing;
		boolean face;
	}

	public GrandfatherClockRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(GrandfatherClockBlockEntity clock, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(clock, state, crumbling);
		BlockState block = clock.getBlockState();
		if (!(block.getBlock() instanceof GrandfatherClockBlock) || clock.getLevel() == null) {
			return;
		}
		state.facing = block.getValue(GrandfatherClockBlock.FACING);
		long day = clock.getLevel().getOverworldClockTime();
		int hour = GrandfatherClockBlock.clockHour(day);
		int minutes = GrandfatherClockBlock.minutes(day);
		state.hourAngle = (hour % 12) * 30.0F + minutes * 0.5F;
		state.minuteAngle = minutes * 6.0F;
		state.phase = GrandfatherClockBlock.moonPhase(day);
		float t = Math.floorMod(clock.getLevel().getGameTime(), 24000L) + partialTick;
		state.swing = SWING * Mth.sin(t * Mth.TWO_PI / 40.0F);
		state.face = GrandfatherClockBlock.midnight(day);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		pose.pushPose();
		pose.translate(0.5F, 0.0F, 0.5F);
		pose.rotateDegrees(Axis.YP, -RockingChairRenderer.yRotation(state.facing));
		pose.translate(-0.5F, 0.0F, -0.5F);
		hand(DecorQuads.get("clock_hour_hand"), state.hourAngle, pose, collector, state.lightCoords);
		hand(DecorQuads.get("clock_minute_hand"), state.minuteAngle, pose, collector, state.lightCoords);
		QuadModel moon = DecorQuads.get("clock_moon_" + state.phase);
		if (moon != null) {
			moon.submit(pose, collector, state.lightCoords);
		}
		QuadModel pendulum = DecorQuads.get("clock_pendulum");
		if (pendulum != null) {
			pose.pushPose();
			pose.translate(PIVOT[0] / 16, PIVOT[1] / 16, PIVOT[2] / 16);
			pose.rotateDegrees(Axis.ZP, state.swing);
			pose.translate(-PIVOT[0] / 16, -PIVOT[1] / 16, -PIVOT[2] / 16);
			pendulum.submit(pose, collector, state.lightCoords);
			pose.popPose();
		}
		QuadModel face = state.face ? DecorQuads.get("clock_ghost_face") : null;
		if (face != null) {
			face.submit(pose, collector, 0xF000F0);
		}
		pose.popPose();
	}

	/** A hand, drawn pointing at twelve, turned clockwise (as seen from the front) about the dial's centre. */
	private static void hand(@Nullable QuadModel hand, float angle, PoseStack pose, SubmitNodeCollector collector, int light) {
		if (hand == null) {
			return;
		}
		pose.pushPose();
		pose.translate(DIAL[0] / 16, DIAL[1] / 16, DIAL[2] / 16);
		pose.rotateDegrees(Axis.ZP, angle);
		pose.translate(-DIAL[0] / 16, -DIAL[1] / 16, -DIAL[2] / 16);
		hand.submit(pose, collector, light);
		pose.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true; // The dial is on the upper half.
	}
}
