package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.wilds.*;
import java.util.*;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.*;

/** Original Frostmaw mesh/clips and scalar pose controls, including crystal item sockets. */
final class WildFrostRenderer extends EntityRenderer<WildFrostmaw,WildFrostRenderer.State> {
    private final ModelPart root;private final WildFrostPose controls;private final Map<String,KeyframeAnimation> clips=new HashMap<>();
    private final Map<String,String> parents=new HashMap<>();private final java.util.function.Function<String,ModelPart> bones;private final ItemModelResolver items;
    WildFrostRenderer(EntityRendererProvider.Context c){super(c);items=c.getItemModelResolver();var data=RecoveredWildlifeModel.read("recovered/wilds_modelfrostmaw");root=RecoveredWildlifeModel.bake(data);bones=RecoveredWildlifeModel.recoveredLookup(root);controls=new WildFrostPose(root);data.getAsJsonObject("actions").entrySet().forEach(e->clips.put(e.getKey(),RecoveredWildlifeModel.recoveredAnimation(e.getValue().getAsJsonObject(),root)));for(var e:data.getAsJsonArray("parts")){var p=e.getAsJsonObject();parents.put(p.get("name").getAsString(),p.get("parent").getAsString());}shadowRadius=2.5F;}
    static void register(){EntityRendererRegistry.register(WildFrostmaw.TYPE,WildFrostRenderer::new);EntityRendererRegistry.register(WildIceBall.TYPE,IceBallRenderer::new);}
    static final class State extends EntityRenderState {float age,yaw,time,walk;int action,death;boolean active,crystal;final ItemStackRenderState item=new ItemStackRenderState();}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(WildFrostmaw e,State s,float partial){super.extractRenderState(e,s,partial);s.age=e.tickCount+partial;s.yaw=net.minecraft.util.Mth.rotLerp(partial,e.yRotO,e.getYRot());s.action=e.action();s.time=e.actionTime(partial);s.active=e.active();s.crystal=e.crystal();s.death=e.deathTime;s.walk=s.action==0&&s.death==0?Math.clamp((float)e.getDeltaMovement().horizontalDistance()*4,0,1):0;items.updateForTopItem(s.item,new ItemStack(OwnerWilds.item("ice_crystal")),ItemDisplayContext.GROUND,e.level(),e,e.getId());}
    private void socket(PoseStack pose,String name){var parent=parents.get(name);if(parent!=null&&!parent.equals("$mesh"))socket(pose,parent);bones.apply(name).translateAndRotate(pose);}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){
        root.getAllParts().forEach(ModelPart::resetPose);String action=s.death>0?"die_animation":switch(s.action){case 1->"swipe_animation";case 2->"swipe_twice_animation";case 3->"roar_animation";case 4->"ice_breath_animation";case 5->"ice_ball_animation";case 6->"activate_animation";case 7->"activate_no_crystal_animation";case 8->"deactivate_animation";case 9->"land_animation";case 10->"slam_animation";case 11->"dodge_animation";default->"";};var clip=clips.get(action);if(clip!=null)clip.apply((long)((s.death>0?s.death:s.time)*50),1);controls.apply(s.active,s.crystal,s.death>0?12:s.action,s.time,s.age,s.age,s.walk,0,0);
        if(s.death>50){bones.apply("eyeLidLeft").visible=true;bones.apply("eyeLidRight").visible=true;}
        pose.pushPose();pose.translate(0,1.5,0);pose.rotate(Axis.YP.rotationDegrees(180-s.yaw));pose.scale(-1,-1,1);out.order(0).submitModelPart(root,pose,RenderTypes.entityCutout(Jugcraft.id("textures/newest/wilds/entity/frostmaw.png")),s.lightCoords,OverlayTexture.NO_OVERLAY,null,-1,0);
        for(String name:List.of("iceCrystal","iceCrystalHand")){if(!s.crystal||!bones.apply(name).visible)continue;pose.pushPose();socket(pose,name);s.item.submit(pose,out,net.minecraft.util.LightCoordsUtil.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,0);pose.popPose();}pose.popPose();super.submit(s,pose,out,camera);
    }
    private static final class IceBallRenderer extends EntityRenderer<WildIceBall,IceBallRenderer.State> {
        private final ModelPart core,body1,body2;
        IceBallRenderer(EntityRendererProvider.Context c){super(c);var root=RecoveredWildlifeModel.bake(RecoveredWildlifeModel.read("recovered/wilds_modeliceball"));core=root.getChild("core");body1=root.getChild("body1");body2=root.getChild("body2");}
        static final class State extends EntityRenderState {float age;}
        @Override public State createRenderState(){return new State();}
        @Override public void extractRenderState(WildIceBall e,State s,float partial){super.extractRenderState(e,s,partial);s.age=e.tickCount+partial;}
        @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){core.resetPose();body1.resetPose();body2.resetPose();body1.zRot=-s.age*.4F;body2.xRot=.7854F;body2.yRot=body2.zRot=.7854F+s.age*.4F;core.zRot=s.age*.4F;pose.pushPose();pose.translate(0,1,0);pose.scale(-1,-1,1);var type=RenderTypes.entityTranslucent(Jugcraft.id("textures/newest/wilds/effects/ice_ball.png"));out.order(0).submitModelPart(core,pose,type,s.lightCoords,OverlayTexture.NO_OVERLAY,null,-1,0);out.order(1).submitModelPart(body1,pose,type,s.lightCoords,OverlayTexture.NO_OVERLAY,null,0xA6FFFFFF,0);out.order(1).submitModelPart(body2,pose,type,s.lightCoords,OverlayTexture.NO_OVERLAY,null,0xA6FFFFFF,0);pose.popPose();}
    }
}
