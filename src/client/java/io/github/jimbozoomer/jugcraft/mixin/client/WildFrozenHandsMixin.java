package io.github.jimbozoomer.jugcraft.mixin.client;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.wilds.WildCold;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AvatarRenderer.class)
public abstract class WildFrozenHandsMixin {
    @Inject(method="renderHand",at=@At("TAIL"))
    private void jugcraft$frozenArm(PoseStack pose,SubmitNodeCollector out,int light,Identifier skin,ModelPart arm,boolean sleeve,CallbackInfo ci){
        var player=Minecraft.getInstance().player;if(player==null||!WildCold.frozen(player))return;
        pose.pushPose();pose.scale(1.002F,1.002F,1.002F);
        out.order(1).submitModelPart(arm,pose,RenderTypes.entityTranslucent(Jugcraft.id("textures/newest/wilds/entity/frozen.png")),light,OverlayTexture.NO_OVERLAY,null,0xDDFFFFFF,0);pose.popPose();
    }
}
