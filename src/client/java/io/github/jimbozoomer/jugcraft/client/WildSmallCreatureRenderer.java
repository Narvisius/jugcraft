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
import net.minecraft.world.entity.PathfinderMob;

/** Supplied Grottol mesh/gait and Lantern stem/transparent gel, including authored scale controllers. */
final class WildSmallCreatureRenderer<T extends PathfinderMob> extends EntityRenderer<T,WildSmallCreatureRenderer.State> {
    private final boolean lantern;
    private ModelPart root;
    private final ModelPart solid,gel;
    private java.util.function.Function<String,ModelPart> bones;
    private Map<String,KeyframeAnimation> clips=new HashMap<>();
    private final Map<String,KeyframeAnimation> solidClips=clips,gelClips=new HashMap<>();
    WildSmallCreatureRenderer(EntityRendererProvider.Context c,boolean lantern){super(c);this.lantern=lantern;var data=RecoveredWildlifeModel.read("recovered/wilds_model"+(lantern?"lantern":"grottol"));root=solid=RecoveredWildlifeModel.bake(data);gel=lantern?RecoveredWildlifeModel.bake(data):null;bones=RecoveredWildlifeModel.recoveredLookup(root);data.getAsJsonObject("actions").entrySet().forEach(e->clips.put(e.getKey(),RecoveredWildlifeModel.recoveredAnimation(e.getValue().getAsJsonObject(),root)));if(lantern)data.getAsJsonObject("actions").entrySet().forEach(e->gelClips.put(e.getKey(),RecoveredWildlifeModel.recoveredAnimation(e.getValue().getAsJsonObject(),gel)));shadowRadius=lantern?.4F:.6F;}
    static void register(){EntityRendererRegistry.register(WildGrottol.TYPE,c->new WildSmallCreatureRenderer<>(c,false));EntityRendererRegistry.register(WildLantern.TYPE,c->new WildSmallCreatureRenderer<>(c,true));}
    static final class State extends EntityRenderState {float age,yaw,head,walk,speed,actionTime;int action,death;boolean deep,pink;}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(T e,State s,float partial){super.extractRenderState(e,s,partial);s.age=e.tickCount+partial;s.yaw=net.minecraft.util.Mth.rotLerp(partial,e.yBodyRotO,e.yBodyRot);s.head=net.minecraft.util.Mth.wrapDegrees(e.getYHeadRot()-s.yaw);s.walk=e.walkAnimation.position(partial);s.speed=Math.min(.5F,e.walkAnimation.speed(partial));s.death=e.deathTime;if(e instanceof WildGrottol g){s.deep=g.deepslate();s.pink=g.blackpink();s.action=g.action();s.actionTime=g.actionTime(partial);}else if(e instanceof WildLantern l){s.actionTime=l.puffTime(partial);s.action=s.actionTime<28?1:0;}}
    private void pose(State s){root.getAllParts().forEach(p->{p.resetPose();p.visible=true;p.skipDraw=false;});
        if(lantern){bones.apply("body").y=11;bones.apply("center").y=11;for(int i=1;i<=4;i++){var b=bones.apply("bottomBit"+i);b.x=b.y=b.z=0;b.xRot=1.0471976F;b.yRot=(i==1?0:i==2?1.5707964F:i==3?(float)Math.PI:4.712389F);}bones.apply("body").xRot=bones.apply("center").xRot=bones.apply("bubbles").xRot=0;}
        else{bones.apply("head").yRot+=Math.clamp(s.head,-30,30)*(float)Math.PI/180;float amount=s.death>0?0:s.speed;for(String side:List.of("Left","Right"))for(int i=1;i<=3;i++){float phase=(i-1)*1.57F,sign=side.equals("Left")?1:-1;var joint=bones.apply("leg"+i+side+"Joint");joint.yRot+=((float)Math.cos(s.walk*1.5+phase)*.6F+sign*.1F)*amount;bones.apply("leg"+i+side+"Upper").zRot+=-((float)Math.cos(s.walk*1.5+phase+1.57)*.25F+sign*.2F)*amount;bones.apply("leg"+i+side+"Lower").zRot+=-(float)Math.cos(s.walk*1.5+phase+1.57)*.4F*amount;bones.apply("foot"+i+side).zRot+=(float)Math.cos(s.walk*1.5+phase+1.57)*.65F*amount;}}
        String action=s.death>0?"die_animation":s.action==1?(lantern?"puff_animation":"idle_animation"):s.action==2?"burrow_animation":"";var clip=clips.get(action);if(clip!=null)clip.apply((long)((s.death>0?s.death:s.actionTime)*50),1);
        if(lantern){var scale=bones.apply("scaleController");for(String name:List.of("body","bubbles","center")){var part=bones.apply(name);float factor=name.equals("center")?2:1;part.xScale=scale.x*factor;part.yScale=scale.y*factor;part.zScale=scale.z*factor;}}
    }
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){if(lantern&&s.death>=7)return;pose.pushPose();pose.translate(0,1.5,0);pose.rotate(Axis.YP.rotationDegrees(180-s.yaw));pose.scale(-1,-1,1);root=solid;bones=RecoveredWildlifeModel.recoveredLookup(root);clips=solidClips;pose(s);
        var texture=Jugcraft.id("textures/newest/wilds/entity/"+(lantern?"lantern":"grottol"+(s.deep?"_deepslate":"")+(s.pink?"_blackpink":""))+".png");
        if(!lantern)out.order(0).submitModelPart(root,pose,RenderTypes.entityCutout(texture),s.lightCoords,OverlayTexture.NO_OVERLAY,null,-1,0);
        else{
            // Separate complete meshes avoid delayed submit reading a mutated visibility mask.
            bones.apply("body").skipDraw=true;bones.apply("bubbles").visible=false;bones.apply("center").visible=false;bones.apply("bottomBits").visible=false;
            out.order(0).submitModelPart(root,pose,RenderTypes.entityCutout(texture),0xF000F0,OverlayTexture.NO_OVERLAY,null,-1,0);
            root=gel;bones=RecoveredWildlifeModel.recoveredLookup(root);clips=gelClips;pose(s);bones.apply("stem").visible=false;
            out.order(1).submitModelPart(root,pose,RenderTypes.entityTranslucent(texture),0xF000F0,OverlayTexture.NO_OVERLAY,null,0x99FFFFFF,0);
        }
        WildFrozenVisuals.submit(root,s,pose,out);pose.popPose();super.submit(s,pose,out,camera);
    }
}
