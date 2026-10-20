package io.github.jimbozoomer.jugcraft.test.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Keeps the pinned client gametest scheduler advancing while an integrated server shuts down. */
@Mixin(Minecraft.class)
public abstract class SingleplayerShutdownMixin {
	@WrapOperation(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/server/IntegratedServer;halt(Z)V"))
	private void jugcraftTest$queueShutdown(IntegratedServer server, boolean waitForServer, Operation<Void> original) {
		if (waitForServer) {
			throw new AssertionError("The client test shutdown adapter requires Minecraft's non-joining halt call");
		}
		// halt(false) still calls executeBlocking to remove players. On the render thread that can
		// wait forever for a server parked at Fabric's test barrier. Run the entire native halt on
		// the server instead; Minecraft's existing isShutdown loop then pumps Fabric's test phases
		// until player removal, saving and shutdown finish. Nothing is skipped or force-stopped.
		server.execute(() -> original.call(server, false));
	}
}
