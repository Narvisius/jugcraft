package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaAreaEntity;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Source ice-tomb mesh, source firefly frames, vanilla fire atlas and source gravity textures. */
public final class JimtaAreaRenderer extends EntityRenderer<JimtaAreaEntity,JimtaAreaRenderer.State>{
    public static final class State extends EntityRenderState{String kind;float time,yaw;}
    private final ModelPart tomb;
    public JimtaAreaRenderer(EntityRendererProvider.Context c){super(c);var mesh=new MeshDefinition();mesh.getRoot().addOrReplaceChild("model",CubeListBuilder.create().texOffs(0,0).addBox(-8,-36,-8,16,36,16).texOffs(40,67).addBox(4,-9,4,6,9,6).texOffs(0,52).addBox(1,-24,-11,10,24,10).texOffs(64,0).addBox(-8,-36,-8,16,36,16,new CubeDeformation(-.01F)).texOffs(40,52).addBox(-10,-9,-10,6,9,6).texOffs(0,86).addBox(-11,-24,1,10,24,10),PartPose.offset(0,24,0));tomb=LayerDefinition.create(mesh,128,128).bakeRoot();}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(JimtaAreaEntity e,State s,float partial){super.extractRenderState(e,s,partial);s.kind=e.kind();s.time=e.age()+partial;s.yaw=e.getYRot();}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        pose.pushPose();
        switch(s.kind){
            case "ice_tomb"->{pose.rotateDegrees(Axis.YP,-s.yaw);pose.scale(1,-1,-1);pose.translate(0,-1.501,0);collector.order(0).submitModelPart(tomb,pose,RenderTypes.entityTranslucent(tex("entity/ice_tomb/ice_tomb")),s.lightCoords,OverlayTexture.NO_OVERLAY,null);collector.order(1).submitModelPart(tomb,pose,RenderTypes.entityTranslucent(tex("entity/ice_tomb/ice_tomb_cull")),s.lightCoords,OverlayTexture.NO_OVERLAY,null);}
            case "firefly_swarm"->{for(int i=0;i<16;i++){pose.pushPose();float a=s.time*.06F+i*2.399F;pose.translate(Math.sin(a)*.8,Math.sin(a*2+i)*.6+.5,Math.cos(a)*.8);pose.rotate(camera.orientation);quad(pose,collector,tex("particle/firefly_"+(((int)s.time+i*7)%16<8?"on":"off")),.08F,-.08F,.08F,0,1,-1);pose.popPose();}}
            case "wall_of_fire"->{pose.rotateDegrees(Axis.YP,-s.yaw);float frame=((int)s.time%32)/32F;for(int i=0;i<2;i++){quad(pose,collector,Identifier.withDefaultNamespace("textures/block/fire_0.png"),.45F,0,3,frame,frame+1/32F,-1);pose.rotateDegrees(Axis.YP,90);}}
            case "sunbeam"->{float progress=Math.clamp(s.time/15,0,1),radius=Math.max(.005F,2.5F*(1-progress*progress));for(int i=0;i<4;i++){quad(pose,collector,Identifier.withDefaultNamespace("textures/entity/beacon_beam.png"),radius*.5F,0,250,0,1,0xffffd988);pose.rotateDegrees(Axis.YP,90);}}
            case "black_hole","gravity_fissure"->{float radius=s.kind.equals("black_hole")?6:3.5F;pose.rotate(camera.orientation);quad(pose,collector,tex("entity/black_hole/black_hole"),radius*.15F,-radius*.15F,radius*.15F,0,1,-1);pose.rotateDegrees(Axis.ZP,s.time*5/radius);for(int i=0;i<10;i++){pose.pushPose();pose.rotateDegrees(Axis.ZP,i*36);quad(pose,collector,tex("entity/black_hole/beam"),.15F,0,radius*.6F,0,1,0x80ff44ff);pose.popPose();}}
            default->{} // Fang swirl uses the original animated vanilla fang renderer for each emitted tooth.
        }
        pose.popPose();super.submit(s,pose,collector,camera);
    }
    private static Identifier tex(String path){return Jugcraft.id("textures/jimtafacts/spellbooks/"+path+".png");}
    private static void quad(PoseStack pose,SubmitNodeCollector collector,Identifier texture,float half,float bottom,float top,float v0,float v1,int color){collector.submitCustomGeometry(pose,RenderTypes.entityTranslucentEmissive(texture),(matrix,v)->{float[][] a={{-half,bottom,0,v1},{half,bottom,1,v1},{half,top,1,v0},{-half,top,0,v0}};for(int side=0;side<2;side++)for(int i=0;i<4;i++){var p=a[side==0?i:3-i];v.addVertex(matrix,p[0],p[1],0).setColor(color).setUv(p[2],p[3]).setLight(0xf000f0).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(matrix,0,0,side==0?1:-1);}});}
}
