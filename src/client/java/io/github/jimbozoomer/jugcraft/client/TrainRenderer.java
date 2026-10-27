package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.train.TrainCar;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;

/**
 * Draws a train car from the parts tools/trains.py exports (assets/jugcraft/train_quads.json): the body, facing and
 * leaning the way its bogies sit on the line, and its wheels, a pair at each axle, turning as far as the car has rolled.
 * The axles' places come from tools/trains.py's AXLES (kept in step by tools/check_mod_data.py).
 */
public class TrainRenderer<T extends TrainCar> extends EntityRenderer<T, TrainRenderer.State> {
	/** The wheels' radius and their distance from the middle across the car, in pixels: keep in step with tools/trains.py. */
	public static final float WHEEL_RADIUS = 5.0F;
	public static final float WHEEL_X = 4.5F;

	public static final class State extends EntityRenderState {
		float yRot;
		float xRot;
		float wheelRun;
	}

	private final String body;
	/** Each axle's place along the car, in pixels from its middle (forwards positive). */
	private final float[] axles;
	private final double length;

	public TrainRenderer(EntityRendererProvider.Context context, String body, double length, float... axles) {
		super(context);
		this.body = body;
		this.length = length;
		this.axles = axles;
		shadowRadius = 0.8F;
	}

	@Override
	protected AABB getBoundingBoxForCulling(T car, float partialTick) {
		return car.getBoundingBox().inflate(length / 2, 0.5, length / 2);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(T car, State state, float partialTick) {
		super.extractRenderState(car, state, partialTick);
		state.yRot = car.getYRot(partialTick);
		state.xRot = car.getXRot(partialTick);
		state.wheelRun = car.wheelRun(partialTick);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		pose.pushPose();
		pose.rotateDegrees(Axis.YP, -state.yRot);
		pose.rotateDegrees(Axis.XP, state.xRot);
		QuadModel model = DecorQuads.get(body);
		if (model != null) {
			model.submit(pose, collector, state.lightCoords);
		}
		QuadModel wheel = DecorQuads.get("train_wheel");
		if (wheel != null) {
			float turn = (float) Math.toDegrees(state.wheelRun * 16.0F / WHEEL_RADIUS);
			for (float z : axles) {
				for (int side = -1; side <= 1; side += 2) {
					pose.pushPose();
					pose.translate(side * WHEEL_X / 16.0F, WHEEL_RADIUS / 16.0F, z / 16.0F);
					pose.rotateDegrees(Axis.XP, turn);
					wheel.submit(pose, collector, state.lightCoords);
					pose.popPose();
				}
			}
		}
		pose.popPose();
		super.submit(state, pose, collector, camera);
	}
}
