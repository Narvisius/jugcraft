package io.github.jimbozoomer.jugcraft.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Resourcify 1.8.7's idle cached workers otherwise outlive Minecraft's shutdown watchdog. */
@Pseudo
@Mixin(targets = "dev.dediamondpro.resourcify.util.MultiThreading", remap = false)
public abstract class ResourcifyWorkerMixin {
    @Inject(method = "createThread", at = @At("RETURN"), remap = false)
    private void jugcraft$backgroundWorker(Runnable task, CallbackInfoReturnable<Thread> cir) {
        // Called by the factory before start; preserve upstream's context classloader.
        cir.getReturnValue().setDaemon(true);
    }
}
