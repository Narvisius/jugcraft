package io.github.jimbozoomer.jugcraft;

import io.github.jimbozoomer.jugcraft.mixin.LinearInterpolationHandlerInvoker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LinearInterpolationHandler;

/** Vanilla network smoothing for Jugcraft vehicles. Minecraft 26.3's Entity.commonTick advances the handler once,
 * before tick(), after saving the previous position and rotation. Never advance it again from a vehicle's tick. */
public final class SmoothFlight {
	private SmoothFlight() {
	}

	/** The handler an entity eases with: linear, over vanilla's default number of ticks ({@code DEFAULT_INTERPOLATION_STEPS}). */
	public static LinearInterpolationHandler handler(Entity entity) {
		return LinearInterpolationHandlerInvoker.jugcraft$create(entity, LinearInterpolationHandler.DEFAULT_INTERPOLATION_STEPS);
	}

	/** Whether the entity eases with a linear handler (the client game test checks the override took). */
	public static boolean isEasing(Entity entity) {
		return entity.getInterpolation() instanceof LinearInterpolationHandler;
	}

}
