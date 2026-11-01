package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.wilds.WildWroughtnaut;
import java.util.*;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Supplied 74-part mesh, full animation clips, original gait, and emissive eyes. */
final class WildWroughtRenderer extends EntityRenderer<WildWroughtnaut,WildWroughtRenderer.State> {
    private final Rig solid=new Rig(false),eyes=new Rig(true);
    private static final class Rig {
        final ModelPart root;final WildWroughtPose pose;final Map<String,KeyframeAnimation> clips=new HashMap<>();
        Rig(boolean eyes){var data=RecoveredWildlifeModel.read("recovered/wilds_modelwroughtnaut");root=RecoveredWildlifeModel.bake(data);pose=new WildWroughtPose(root);data.getAsJsonObject("actions").entrySet().forEach(e->clips.put(e.getKey(),RecoveredWildlifeModel.animation(e.getValue().getAsJsonObject()).bake(root)));if(eyes){root.getAllParts().forEach(p->p.skipDraw=true);root.createPartLookup().apply("eyeLeft").skipDraw=false;root.createPartLookup().apply("eyeRight").skipDraw=false;}}
        void animate(State s){root.getAllParts().forEach(ModelPart::resetPose);pose.apply(s.active,s.death>0?9:s.action,s.age,s.walk,0,0);String name=s.death>0?"die_animation":switch(s.action){case 1->"attack_animation";case 2->"attack_twice_animation";case 3->"attack_thrice_animation";case 4->"vertical_attack_animation";case 5->"stomp_attack_animation";case 6->"activate_animation";case 7->"deactivate_animation";case 8->"hurt_animation";default->"";};var clip=clips.get(name);if(clip!=null)clip.apply((long)((s.death>0?s.death:s.time)*50),1);}
    }
    WildWroughtRenderer(EntityRendererProvider.Context c){super(c);shadowRadius=1;}
    static void register(){EntityRendererRegistry.register(WildWroughtnaut.TYPE,WildWroughtRenderer::new);}
    static final class State extends EntityRenderState {float age,yaw,time,walk;int action,death;boolean active;}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(WildWroughtnaut e,State s,float partial){super.extractRenderState(e,s,partial);s.age=e.tickCount+partial;s.yaw=net.minecraft.util.Mth.rotLerp(partial,e.yRotO,e.getYRot());s.action=e.action();s.time=e.actionTime(partial);s.active=e.active();s.death=e.deathTime;s.walk=s.action==0&&s.death==0?Math.clamp((float)e.getDeltaMovement().horizontalDistance()*7,0,1):0;}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){solid.animate(s);eyes.animate(s);pose.pushPose();pose.translate(0,1.5,0);pose.rotate(Axis.YP.rotationDegrees(180-s.yaw));pose.scale(-1,-1,1);var texture=Jugcraft.id("textures/newest/wilds/entity/wroughtnaut.png");out.order(0).submitModelPart(solid.root,pose,RenderTypes.entityCutout(texture),s.lightCoords,OverlayTexture.NO_OVERLAY,null,-1,0);if(s.active&&s.death<110)out.order(1).submitModelPart(eyes.root,pose,RenderTypes.eyes(texture),net.minecraft.util.LightCoordsUtil.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,null,-1,0);pose.popPose();super.submit(s,pose,out,camera);}
}
