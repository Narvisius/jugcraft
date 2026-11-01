package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.arcane.ArcaneDarkTrader;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Original rig, skins and keyframes; native render types remain compatible with Iris. */
final class ArcaneDarkTraderRenderer extends EntityRenderer<ArcaneDarkTrader,ArcaneDarkTraderRenderer.State> {
    private final ModelPart root=ArcaneDarkTraderModel.createBodyLayer().bakeRoot(),portal=ArcaneLightDoorModel.createBodyLayer().bakeRoot();
    private final KeyframeAnimation idle=ArcaneDarkTraderModel.STILL.bake(root),spawn=ArcaneDarkTraderModel.SPAWN.bake(root),escape=ArcaneDarkTraderModel.ESCAPE.bake(root),nose=ArcaneDarkTraderModel.NOSE.bake(root),moustache=ArcaneDarkTraderModel.MOUSTACHE.bake(root),greeting=ArcaneDarkTraderModel.RIGHT_HAND_UP.bake(root),door=ArcaneLightDoorModel.SPAWN.bake(portal);
    ArcaneDarkTraderRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=.5F;}
    static void register(){EntityRendererRegistry.register(ArcaneDarkTrader.TYPE,ArcaneDarkTraderRenderer::new);}
    static final class State extends EntityRenderState {String variant;int action;float yaw;long time,worldTime;}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(ArcaneDarkTrader e,State s,float partial){super.extractRenderState(e,s,partial);s.variant=e.variant();s.action=e.action();s.yaw=e.getYRot();s.time=(long)((e.elapsed()+partial)*50);s.worldTime=(long)((e.tickCount+partial)*50);}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){
        root.getAllParts().forEach(ModelPart::resetPose);
        if(s.action==1)spawn.apply(Math.max(0,s.time-600),1);
        else if(s.action==2)escape.apply(s.time,1);
        else{idle.apply(s.worldTime,1);switch(s.action){case 3->nose.apply(s.time,1);case 4->moustache.apply(s.time,1);case 5->greeting.apply(s.time,1);}}
        pose.pushPose();pose.translate(0,1.5,0);pose.rotate(Axis.YP.rotationDegrees(180-s.yaw));pose.scale(-1,-1,1);
        if(s.action!=1||s.time>=600)out.order(0).submitModelPart(root,pose,RenderTypes.entityCutout(Jugcraft.id("textures/newest/forbidden/entity/dark_trader/"+s.variant+".png")),s.lightCoords,OverlayTexture.NO_OVERLAY,null,-1,0);
        if(s.action==1&&s.time<=1000||s.action==2&&s.time>=1500){
            portal.getAllParts().forEach(ModelPart::resetPose);door.apply(s.action==1?s.time:Math.min(1000,s.time-1500),1);
            out.order(1).submitModelPart(portal,pose,RenderTypes.entityTranslucent(Jugcraft.id("textures/newest/forbidden/effect/quantum_light_door.png")),net.minecraft.util.LightCoordsUtil.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,null,-1,0);
        }
        pose.popPose();super.submit(s,pose,out,camera);
    }
}
