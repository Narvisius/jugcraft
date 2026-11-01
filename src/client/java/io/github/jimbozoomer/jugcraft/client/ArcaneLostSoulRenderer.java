package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.arcane.ArcaneLostSoul;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

final class ArcaneLostSoulRenderer extends EntityRenderer<ArcaneLostSoul,ArcaneLostSoulRenderer.State> {
    private final ModelPart root=ArcaneLostSoulModel.createBodyLayer().bakeRoot();
    private final KeyframeAnimation idle=ArcaneLostSoulModel.LOST_SOUL_STILL.bake(root),fear=ArcaneLostSoulModel.LOST_SOUL_FEAR.bake(root);
    ArcaneLostSoulRenderer(EntityRendererProvider.Context c){super(c);shadowRadius=0;}
    static void register(){ArcaneLostSoul.TYPES.values().forEach(type->EntityRendererRegistry.register(type,ArcaneLostSoulRenderer::new));}
    static final class State extends EntityRenderState {String kind;boolean scared;float yaw;long time;}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(ArcaneLostSoul e,State s,float partial){super.extractRenderState(e,s,partial);s.kind=e.kind();s.scared=e.scared();s.yaw=e.getYRot();s.time=(long)((e.tickCount+partial)*50);}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){root.getAllParts().forEach(ModelPart::resetPose);(s.scared?fear:idle).apply(s.time,1);pose.pushPose();pose.translate(0,1.5,0);pose.rotate(Axis.YP.rotationDegrees(180-s.yaw));pose.scale(-1,-1,1);out.order(0).submitModelPart(root,pose,RenderTypes.entityCutout(Jugcraft.id("textures/newest/forbidden/entity/lost_soul/"+s.kind+".png")),s.lightCoords,OverlayTexture.NO_OVERLAY,null,-1,0);pose.popPose();super.submit(s,pose,out,camera);}
}
