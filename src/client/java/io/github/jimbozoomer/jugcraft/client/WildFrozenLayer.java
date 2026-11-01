package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
/** Original frozen skin wraps the actual creature mesh, not a replacement ice cube. */
public final class WildFrozenLayer extends RenderLayer<LivingEntityRenderState,EntityModel<LivingEntityRenderState>> {
    public WildFrozenLayer(RenderLayerParent<LivingEntityRenderState,EntityModel<LivingEntityRenderState>> parent){super(parent);}
    @Override public void submit(PoseStack pose,SubmitNodeCollector out,int light,LivingEntityRenderState state,float yaw,float pitch){
        if(!((WildFrozenState)state).jugcraft$frozen()||state.isInvisible)return;
        pose.pushPose();pose.scale(1.002F,1.002F,1.002F);
        out.order(1).submitModel(getParentModel(),state,pose,RenderTypes.entityTranslucent(Jugcraft.id("textures/newest/wilds/entity/frozen.png")),light,OverlayTexture.NO_OVERLAY,0xDDFFFFFF,null,state.outlineColor);
        pose.popPose();
    }
}
