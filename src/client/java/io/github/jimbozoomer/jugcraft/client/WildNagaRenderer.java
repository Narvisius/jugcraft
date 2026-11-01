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
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Original Naga geometry/keyframes plus authored wing folding, membrane scaling and jaw controls. */
final class WildNagaRenderer extends EntityRenderer<WildNaga,WildNagaRenderer.State> {
    private final ModelPart root;
    private final java.util.function.Function<String,ModelPart> bones;
    private final WildNagaPose controls;
    private final Map<String,String> parents=new HashMap<>();
    private final Map<WildNaga,WildNagaTail> tails=new WeakHashMap<>();
    private final Map<String,KeyframeAnimation> clips=new HashMap<>();
    WildNagaRenderer(EntityRendererProvider.Context c){super(c);var data=RecoveredWildlifeModel.read("recovered/wilds_modelnaga");root=RecoveredWildlifeModel.bake(data);bones=RecoveredWildlifeModel.recoveredLookup(root);controls=new WildNagaPose(root);for(var part:data.getAsJsonArray("parts")){var p=part.getAsJsonObject();parents.put(p.get("name").getAsString(),p.get("parent").getAsString());}data.getAsJsonObject("actions").entrySet().forEach(e->clips.put(e.getKey(),RecoveredWildlifeModel.recoveredAnimation(e.getValue().getAsJsonObject(),root)));shadowRadius=0;}
    static void register(){EntityRendererRegistry.register(WildNaga.TYPE,WildNagaRenderer::new);EntityRendererRegistry.register(WildPoisonBall.TYPE,PoisonRenderer::new);}
    static final class State extends EntityRenderState {float age,yaw,turn,pitch,time;int action,phase,death,tick;float partial;boolean grounded,frozen;net.minecraft.world.phys.Vec3 position;WildNagaTail tail;}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(WildNaga e,State s,float partial){super.extractRenderState(e,s,partial);s.age=e.tickCount+partial;s.yaw=net.minecraft.util.Mth.rotLerp(partial,e.yRotO,e.getYRot());s.turn=net.minecraft.util.Mth.wrapDegrees(e.getYRot()-e.yRotO);s.pitch=(float)Math.atan2(e.getDeltaMovement().y,Math.max(.001,e.getDeltaMovement().horizontalDistance()));s.action=e.action();s.phase=e.phase();s.time=e.actionTime(partial);s.death=e.deathTime;s.tick=e.tickCount;s.partial=partial;s.position=e.getPosition(partial);s.grounded=e.onGround();s.frozen=WildCold.frozen(e);s.tail=tails.computeIfAbsent(e,k->new WildNagaTail());}
    private void flap(String name,float speed,float degree,float offset,float frame,boolean invert){bones.apply(name).zRot+=(invert?-1:1)*(float)Math.cos(frame*speed+offset)*degree;}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){root.getAllParts().forEach(ModelPart::resetPose);controls.modelCorrections();String action=s.death>0?(s.phase==2?"die_ground_animation":"die_air_animation"):switch(s.action){case WildNaga.FLAP->"flap_animation";case WildNaga.SPIT->"spit_animation";case WildNaga.SWOOP->"swoop_animation";case WildNaga.FALL->"hurt_to_fall_animation";case WildNaga.LAND->"land_animation";case WildNaga.RISE->"get_up_animation";default->"";};var clip=clips.get(action);if(clip!=null)clip.apply((long)((s.death>0?s.death:s.time)*50),1);
        if(s.phase==0&&s.death==0&&s.action!=WildNaga.FALL&&s.action!=WildNaga.SWOOP){float speed=.27F,degree=s.action==WildNaga.SPIT?.5F:.8F;for(String side:List.of("L","R")){boolean left=side.equals("L");flap("shoulder1_"+side,speed,degree,0,s.age,left);flap("lowerArmJoint_"+side,speed,degree*.7F,-.6F,s.age,left);flap("handJoint_"+side,speed,degree*.6F,-1.2F,s.age,left);flap("backWing_"+side,speed,degree,-.5F,s.age,left);}bones.apply("root").xRot-=s.pitch*.6F;bones.apply("body").zRot-=Math.clamp(s.turn,-8,8)*(float)Math.PI/90;}
        controls.jawControls();controls.wingFoldControls();s.tail.apply(s.tick,s.partial,s.position,s.yaw,s.grounded,s.frozen,parents,bones);controls.computeWingWebbing();pose.pushPose();pose.translate(0,1.5,0);pose.rotate(Axis.YP.rotationDegrees(180-s.yaw));pose.scale(-1,-1,1);out.order(0).submitModelPart(root,pose,RenderTypes.entityCutout(Jugcraft.id("textures/newest/wilds/entity/naga.png")),s.lightCoords,OverlayTexture.NO_OVERLAY,null,-1,0);WildFrozenVisuals.submit(root,s,pose,out);pose.popPose();super.submit(s,pose,out,camera);
    }
    static final class PoisonRenderer extends EntityRenderer<WildPoisonBall,PoisonRenderer.State> {
        private final ModelPart inner,outer;
        PoisonRenderer(EntityRendererProvider.Context c){super(c);var root=RecoveredWildlifeModel.bake(RecoveredWildlifeModel.read("recovered/wilds_modelpoisonball"));inner=root.getChild("inner");outer=root.getChild("outer");}
        static final class State extends EntityRenderState {float pitch,yaw;}
        @Override public State createRenderState(){return new State();}
        @Override public void extractRenderState(WildPoisonBall e,State s,float partial){super.extractRenderState(e,s,partial);var motion=e.getDeltaMovement();s.pitch=(float)(-Math.asin(Math.clamp(motion.y/Math.max(.001,motion.length()),-1,1))+Math.PI/2);s.yaw=(float)-Math.atan2(motion.x,motion.z);}
        @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){inner.resetPose();outer.resetPose();inner.xRot=outer.xRot=s.pitch;pose.pushPose();pose.rotate(Axis.YP.rotation(s.yaw));var texture=Jugcraft.id("textures/newest/wilds/effects/poison_ball.png");out.order(0).submitModelPart(inner,pose,RenderTypes.entityTranslucent(texture),s.lightCoords,OverlayTexture.NO_OVERLAY,null,-1,0);out.order(1).submitModelPart(outer,pose,RenderTypes.entityTranslucent(texture),s.lightCoords,OverlayTexture.NO_OVERLAY,null,0x99FFFFFF,0);pose.popPose();}
    }
}
