package io.github.jimbozoomer.jugcraft.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LinearInterpolationHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Makes a {@link LinearInterpolationHandler} for an entity vanilla gives none, the balloons that ease after the server
 * (SmoothFlight): in 26.3 its only constructor, (Entity, int), is private.
 */
@Mixin(LinearInterpolationHandler.class)
public interface LinearInterpolationHandlerInvoker {
	@Invoker("<init>")
	static LinearInterpolationHandler jugcraft$create(Entity entity, int steps) {
		throw new AssertionError("Replaced by Mixin");
	}
}
