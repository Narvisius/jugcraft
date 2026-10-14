package io.github.jimbozoomer.jugcraft.mixin.client;

import io.github.jimbozoomer.jugcraft.walker.HowitzerWalker;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hide the enclosed pilot, including equipment and name tag, without changing gameplay invisibility. */
@Mixin(EntityRenderer.class)
public abstract class HowitzerPilotRendererMixin {
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private void jugcraft$hideEnclosedPilot(Entity entity, Frustum frustum, double x, double y, double z, float partialTick,
			CallbackInfoReturnable<Boolean> info) {
		if (entity instanceof Player && entity.getVehicle() instanceof HowitzerWalker walker
				&& walker.getFirstPassenger() == entity) {
			info.setReturnValue(false);
		}
	}
}
