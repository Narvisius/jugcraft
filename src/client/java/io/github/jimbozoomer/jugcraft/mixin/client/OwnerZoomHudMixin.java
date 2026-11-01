package io.github.jimbozoomer.jugcraft.mixin.client;
import io.github.jimbozoomer.jugcraft.client.OwnerZoom;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Presentation-only zoom policies; never persists changes to Minecraft's HUD setting. */
@Mixin(Hud.class)
public abstract class OwnerZoomHudMixin {
    @Inject(method="extractRenderState",at=@At("HEAD"),cancellable=true)
    private void jugcraft$zoomHud(GuiGraphicsExtractor graphics,DeltaTracker tracker,CallbackInfo ci){if(OwnerZoom.hideHud())ci.cancel();}
    @Inject(method="extractSpyglassOverlay",at=@At("HEAD"),cancellable=true)
    private void jugcraft$spyglassFrame(GuiGraphicsExtractor graphics,float scale,CallbackInfo ci){if(OwnerZoom.hideSpyglassOverlay())ci.cancel();}
}
