package io.github.jimbozoomer.jugcraft.mixin.client;
import io.github.jimbozoomer.jugcraft.client.jimtafacts.JimtaWardClient;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(EntityRenderer.class)
public abstract class JimtaInvisibilityRendererMixin {
    @Inject(method="shouldRender",at=@At("HEAD"),cancellable=true)
    private void jimta$hiddenEquipment(Entity e,Frustum frustum,double x,double y,double z,float partial,CallbackInfoReturnable<Boolean> result){if(JimtaWardClient.hidden(e))result.setReturnValue(false);}
}
