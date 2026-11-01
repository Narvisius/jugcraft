package io.github.jimbozoomer.jugcraft.mixin.client;
import io.github.jimbozoomer.jugcraft.enchanting.OwnerEnchantments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LightmapRenderStateExtractor.class)
public abstract class OwnerEnchantmentLightMixin {
    @Inject(method="extract(Lnet/minecraft/client/renderer/state/LightmapRenderState;F)V",at=@At("RETURN"))
    private void jugcraft$nightwalker(LightmapRenderState state,float partial,CallbackInfo ci){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.level.getMaxLocalRawBrightness(mc.player.blockPosition())>=8)return;
        int rank=OwnerEnchantments.level(mc.player,mc.player.getItemBySlot(EquipmentSlot.HEAD),"nightwalker");
        if(rank>0)state.brightness=Math.min(1,state.brightness+Math.min(12,rank*4)/255f);
    }
}
