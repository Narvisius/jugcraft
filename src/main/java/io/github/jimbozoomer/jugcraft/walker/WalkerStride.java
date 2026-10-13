package io.github.jimbozoomer.jugcraft.walker;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/** Client-only gait state, driven by the actual eased movement, including turning on the spot. */
public final class WalkerStride {
	private float previousPhase;
	private float phase;
	private float previousWeight;
	private float weight;

	public void tick(Entity walker) {
		previousPhase = phase;
		previousWeight = weight;
		double distance = Math.hypot(walker.getX() - walker.xo, walker.getZ() - walker.zo);
		float turn = Math.abs(Mth.wrapDegrees(walker.getYRot() - walker.yRotO));
		// A teleport is not a stride. Normal walking/turning drives the legs; rest eases them back under the hull.
		float step = distance > 1.0 || turn > 45.0F ? 0 : (float) distance + turn * Mth.DEG_TO_RAD * 0.75F;
		double yaw = walker.getYRot() * Mth.DEG_TO_RAD;
		double forward = -(walker.getX() - walker.xo) * Math.sin(yaw) + (walker.getZ() - walker.zo) * Math.cos(yaw);
		phase += forward < -0.001 ? -step : step;
		weight = Mth.lerp(0.35F, weight, Mth.clamp(step / 0.15F, 0.0F, 1.0F));
	}

	public float phase(float partialTick) {
		return Mth.lerp(partialTick, previousPhase, phase);
	}

	public float weight(float partialTick) {
		return Mth.lerp(partialTick, previousWeight, weight);
	}
}
