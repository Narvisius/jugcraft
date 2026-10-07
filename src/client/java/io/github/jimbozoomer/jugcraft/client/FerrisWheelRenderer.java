package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.agriculture.FerrisWheel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the Ferris wheel (fall addition 27) from the quads tools/ferris_wheel_data.py writes
 * (assets/jugcraft/ferris_wheel_quads.json): its frame standing over the booth; the wheel turned to its angle about the
 * hub, a sixteenth of it drawn sixteen times round, with the hub plates and a pivot bar for each car; its lights, at full
 * brightness while it is lit; and each car hanging upright from its pivot in its colour, swaying a little as the wheel
 * turns. All of it is turned to the wheel's facing.
 *
 * <p>The entity stands in its booth block, where there is no light, so the frame takes its light from just above the
 * booth, the wheel from its hub, and each car from where it hangs: a car at the top in the sun, one at the bottom by the
 * lamps round about.
 */
public class FerrisWheelRenderer extends EntityRenderer<FerrisWheel, FerrisWheelRenderer.State> {
	private static final String[] COLOURS = {"pumpkin", "cranberry", "mustard", "spruce"};
	private static final int SECTIONS = 16;

	public static final class State extends EntityRenderState {
		float angle;
		int facing;
		boolean lit;
		float sway;
		int frameLight;
		int wheelLight;
		final int[] carLight = new int[FerrisWheel.CARS];
	}

	public FerrisWheelRenderer(EntityRendererProvider.Context context) {
		super(context);
		shadowRadius = 0.0F;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	/** The whole wheel and its frame, not just the entity's small box at its foot. */
	@Override
	protected AABB getBoundingBoxForCulling(FerrisWheel wheel, float partialTick) {
		return wheel.extent();
	}

	@Override
	public void extractRenderState(FerrisWheel wheel, State state, float partialTick) {
		super.extractRenderState(wheel, state, partialTick);
		state.angle = wheel.angle(partialTick);
		state.facing = FerrisWheel.facingDegrees(wheel.facing());
		state.lit = wheel.lit();
		float speed = wheel.speed() / FerrisWheel.FULL_SPEED;
		state.sway = speed <= 0.0F ? 0.0F : 2.0F * speed * Mth.sin((wheel.tickCount + partialTick) * 0.07F);
		Level level = wheel.level();
		state.frameLight = LightCoordsUtil.getLightCoords(level, wheel.booth().above());
		state.wheelLight = LightCoordsUtil.getLightCoords(level, BlockPos.containing(wheel.toWorld(0.0, FerrisWheel.HUB, 0.0)));
		for (int car = 0; car < FerrisWheel.CARS; car++) {
			Vec3 at = FerrisWheel.pivot(car, state.angle);
			state.carLight[car] = LightCoordsUtil.getLightCoords(level, BlockPos.containing(wheel.toWorld(at.x, at.y - 0.6, at.z)));
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		QuadModel frame = DecorQuads.get("ferris_wheel_frame");
		QuadModel section = DecorQuads.get("ferris_wheel_section");
		QuadModel hub = DecorQuads.get("ferris_wheel_hub");
		QuadModel pivot = DecorQuads.get("ferris_wheel_pivot");
		QuadModel lights = DecorQuads.get("ferris_wheel_lights");
		int light = state.wheelLight;
		int glow = state.lit ? LightCoordsUtil.FULL_BRIGHT : light;
		pose.pushPose();
		pose.rotateDegrees(Axis.YP, -state.facing);
		if (frame != null) {
			frame.submit(pose, collector, state.frameLight);
		}
		// The wheel, turned about its hub.
		pose.pushPose();
		pose.translate(0.0F, (float) FerrisWheel.HUB, 0.0F);
		pose.rotateDegrees(Axis.ZP, (float) Math.toDegrees(state.angle));
		for (int k = 0; k < SECTIONS; k++) {
			pose.pushPose();
			pose.rotateDegrees(Axis.ZP, k * 360.0F / SECTIONS);
			if (section != null) {
				section.submit(pose, collector, light);
			}
			if (lights != null) {
				lights.submit(pose, collector, glow);
			}
			pose.popPose();
		}
		if (hub != null) {
			hub.submit(pose, collector, light);
		}
		for (int car = 0; car < FerrisWheel.CARS; car++) {
			pose.pushPose();
			pose.rotateDegrees(Axis.ZP, (float) Math.toDegrees(FerrisWheel.carAngle(car, 0.0F)) - 90.0F);
			if (pivot != null) {
				pivot.submit(pose, collector, light);
			}
			pose.popPose();
		}
		pose.popPose();
		// The cars, hanging upright from their pivots.
		for (int car = 0; car < FerrisWheel.CARS; car++) {
			QuadModel model = DecorQuads.get("ferris_wheel_car_" + COLOURS[car % COLOURS.length]);
			if (model == null) {
				continue;
			}
			Vec3 at = FerrisWheel.pivot(car, state.angle);
			pose.pushPose();
			pose.translate((float) at.x, (float) at.y, (float) at.z);
			pose.rotateDegrees(Axis.ZP, state.sway * (car % 2 == 0 ? 1.0F : -1.0F));
			model.submit(pose, collector, state.carLight[car]);
			pose.popPose();
		}
		pose.popPose();
		super.submit(state, pose, collector, camera);
	}
}
