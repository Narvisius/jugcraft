package io.github.jimbozoomer.jugcraft.mixin.client;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The chunk sections round the camera (client/Sight.java looks them up in the occlusion graph). */
@Mixin(LevelRenderer.class)
public interface SightLevelRendererAccessor {
	@Accessor("viewArea")
	ViewArea jugcraft$viewArea();
}
