package io.github.jimbozoomer.jugcraft.mixin.client;
import io.github.jimbozoomer.jugcraft.client.OwnerAntiqueMaps;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemFrameRenderer.class)
public abstract class OwnerAntiqueFrameMixin {
 @Inject(method="extractRenderState(Lnet/minecraft/world/entity/decoration/ItemFrame;Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;F)V",at=@At("TAIL"))
 private void jugcraft$map(ItemFrame frame,ItemFrameRenderState state,float partial,CallbackInfo ci){if(state.mapId!=null)OwnerAntiqueMaps.apply(frame.getItem(),state.mapRenderState);}
}
