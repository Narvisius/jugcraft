package io.github.jimbozoomer.jugcraft.mixin.client;
import io.github.jimbozoomer.jugcraft.client.OwnerAntiqueMaps;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.state.level.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class OwnerAntiqueHandsMixin {
 @Inject(method="submitHandsWithItems",at=@At("HEAD"))
 private void jugcraft$maps(float partial,PoseStack pose,SubmitNodeCollector out,PlayerRenderState player,FirstPersonHandsAndItemsRenderState state,CallbackInfo ci){
  if(state.hasMainHandMapData)OwnerAntiqueMaps.apply(state.mainHandItem,state.mainHandMapRenderState);
  if(state.hasOffHandMapData)OwnerAntiqueMaps.apply(state.offHandItem,state.offHandMapRenderState);
 }
}
