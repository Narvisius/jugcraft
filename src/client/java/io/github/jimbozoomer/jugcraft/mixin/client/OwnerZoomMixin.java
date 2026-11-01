package io.github.jimbozoomer.jugcraft.mixin.client;
import io.github.jimbozoomer.jugcraft.client.OwnerZoom;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Camera.class)
public abstract class OwnerZoomMixin {
    @Inject(method="calculateFov",at=@At("RETURN"),cancellable=true)
    private void jugcraft$zoom(float delta,CallbackInfoReturnable<Float> cir){cir.setReturnValue(OwnerZoom.fov(cir.getReturnValueF(),false));}
    @Inject(method="calculateHudFov",at=@At("RETURN"),cancellable=true)
    private void jugcraft$zoomHands(float delta,CallbackInfoReturnable<Float> cir){cir.setReturnValue(OwnerZoom.fov(cir.getReturnValueF(),true));}
}
