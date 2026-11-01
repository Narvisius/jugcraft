package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.wilds.WildFoliaath;
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

/** Original adult/juvenile/infant meshes, UVs and recovered attack/eat/hurt/death keyframes. */
final class WildFoliaathRenderer extends EntityRenderer<WildFoliaath,WildFoliaathRenderer.State> {
    private final ModelPart root;
    private final java.util.function.Function<String,ModelPart> bones;
    private final Map<String,KeyframeAnimation> clips=new HashMap<>();
    private final boolean baby;
    WildFoliaathRenderer(EntityRendererProvider.Context c,boolean baby){super(c);this.baby=baby;var data=RecoveredWildlifeModel.read("recovered/wilds_modelfoliaath"+(baby?"baby":""));root=RecoveredWildlifeModel.bake(data);bones=RecoveredWildlifeModel.recoveredLookup(root);data.getAsJsonObject("actions").entrySet().forEach(e->clips.put(e.getKey(),RecoveredWildlifeModel.recoveredAnimation(e.getValue().getAsJsonObject(),root)));shadowRadius=baby?.2F:.5F;}
    static void register(){EntityRendererRegistry.register(WildFoliaath.ADULT,c->new WildFoliaathRenderer(c,false));EntityRendererRegistry.register(WildFoliaath.BABY,c->new WildFoliaathRenderer(c,true));}
    static final class State extends EntityRenderState {float age,active,actionTime,yaw;int growth,action,death;}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(WildFoliaath e,State s,float partial){super.extractRenderState(e,s,partial);s.age=e.tickCount+partial;s.active=e.activation()/30F;s.actionTime=e.actionTime(partial);s.growth=e.growth();s.action=e.action();s.death=e.deathTime;s.yaw=e.getYHeadRot();}
    private void wave(String bone,float age,float speed,float degree,float phase,float amount){var p=bones.apply(bone);if(p!=null)p.xRot+=(float)Math.cos(age*speed+phase)*degree*amount;}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){
        root.getAllParts().forEach(p->{p.resetPose();p.visible=true;});float active=(float)Math.sqrt(Math.sin(Math.clamp(s.active,0,1)*Math.PI/2));
        if(baby){bones.apply("infantBase").visible=s.growth<600;bones.apply("juvenileBase").visible=s.growth>=600;bones.apply("mouth1").zRot+=.5F*active;bones.apply("mouth2").zRot-=.5F*active;for(int i=1;i<=4;i++)wave("juvenileLeaf"+i,s.age,1,.07F,0,active);}
        else{
            var stem=bones.apply("stem1Joint");stem.xScale=stem.yScale=stem.zScale=s.death>0?1:active;stem.yRot+=s.yaw*(float)Math.PI/180;
            for(int i=1;i<=4;i++){var leaf=bones.apply("bigLeaf"+i+"Base");leaf.y=(leaf.y-3.5F)*1.25F;leaf.xScale=leaf.yScale=leaf.zScale=1.25F;wave("bigLeaf"+i+"Base",s.age,.45F,.13F,0,active);wave("bigLeaf"+i+"End",s.age,.45F,.13F,2,active);}
            wave("stem1Base",s.age,.45F,.05F,0,active);bones.apply("stem1Base").zRot+=(float)Math.cos(s.age*.225)*.15F*active;wave("stem2",s.age,.45F,.05F,.5F,active);wave("stem3",s.age,.45F,.07F,1,active);wave("stem4",s.age,.45F,.05F,1.5F,active);wave("headBase",s.age,.45F,-.15F,1.3F,active);bones.apply("headBase").yRot+=(float)Math.cos(s.age*.225)*.15F*active;for(int i=1;i<=8;i++)wave("leaf"+i+"Head",s.age,.45F,.15F,3,active);
        }
        String action=s.death>0?"die_animation":s.action==1?"attack_animation":s.action==2?"eat_animation":s.action==3?"hurt_animation":"";var clip=clips.get(action);if(clip!=null)clip.apply((long)((s.death>0?s.death:s.actionTime)*50),1);
        pose.pushPose();pose.translate(0,1.5,0);pose.rotate(Axis.YP.rotationDegrees(180));pose.scale(-1,-1,1);out.order(0).submitModelPart(root,pose,RenderTypes.entityCutout(Jugcraft.id("textures/newest/wilds/entity/foliaath"+(baby?"_baby":"")+".png")),s.lightCoords,OverlayTexture.NO_OVERLAY,null,-1,0);WildFrozenVisuals.submit(root,s,pose,out);pose.popPose();super.submit(s,pose,out,camera);
    }
}
