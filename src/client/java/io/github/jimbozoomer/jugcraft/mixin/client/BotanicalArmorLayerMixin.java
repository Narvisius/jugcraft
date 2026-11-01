package io.github.jimbozoomer.jugcraft.mixin.client;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.client.BotanicalArmorRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(HumanoidArmorLayer.class)
public abstract class BotanicalArmorLayerMixin {
    @Inject(method="renderArmorPiece",at=@At("HEAD"),cancellable=true)
    private void jugcraft$originalBotanicalArmor(PoseStack pose,SubmitNodeCollector collector,ItemStack stack,EquipmentSlot slot,int light,HumanoidRenderState state,CallbackInfo ci){if(io.github.jimbozoomer.jugcraft.client.WildArmorRenderer.draw(pose,collector,stack,slot,light,state)||BotanicalArmorRenderer.draw(pose,collector,stack,slot,light,state))ci.cancel();}
}
