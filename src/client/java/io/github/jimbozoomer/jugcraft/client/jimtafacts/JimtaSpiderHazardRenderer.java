package io.github.jimbozoomer.jugcraft.client.jimtafacts;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaSpiderTomb;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
/** Uses the same recovered source numeric meshes as player tomb/spikes, with hostile gameplay kept separate. */
public final class JimtaSpiderHazardRenderer extends EntityRenderer<JimtaSpiderTomb,JimtaSpiderHazardRenderer.State>{
    public static final class State extends EntityRenderState {float time,yaw;int wait;}
    private final ModelPart model;private final boolean spike;
    public JimtaSpiderHazardRenderer(EntityRendererProvider.Context c,boolean spike){super(c);this.spike=spike;
        if(spike)model=spikeLayer().bakeRoot();else{var mesh=new MeshDefinition();mesh.getRoot().addOrReplaceChild("model",CubeListBuilder.create().texOffs(0,0).addBox(-8,-36,-8,16,36,16).texOffs(40,67).addBox(4,-9,4,6,9,6).texOffs(0,52).addBox(1,-24,-11,10,24,10).texOffs(64,0).addBox(-8,-36,-8,16,36,16,new CubeDeformation(-.01F)).texOffs(40,52).addBox(-10,-9,-10,6,9,6).texOffs(0,86).addBox(-11,-24,1,10,24,10),PartPose.offset(0,24,0));model=LayerDefinition.create(mesh,128,128).bakeRoot();}}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(JimtaSpiderTomb e,State s,float partial){super.extractRenderState(e,s,partial);s.time=e.tickCount+partial;s.yaw=e.getYRot();s.wait=e.waitTicks();}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        pose.pushPose();pose.rotateDegrees(Axis.YP,-s.yaw);
        if(spike){float t=s.time-s.wait,offset=t<0?-1:t<6?(float)(Math.sin(t/6*Math.PI)/Math.PI+t/6-1):t<46?0:-(float)(Math.sin((Math.clamp((t-46)/30,0,1)+1)*Math.PI)/Math.PI+Math.clamp((t-46)/30,0,1));pose.scale(1.25F,-1.25F,1.25F);pose.translate(0,-offset*68/16F,0);model.getChild("bottom").visible=false;model.getChild("middle").y=4;model.getChild("top").y=-18;collector.order(0).submitModelPart(model,pose,RenderTypes.entityCutout(Jugcraft.id("textures/jimtafacts/spellbooks/entity/ice_spike.png")),s.lightCoords,OverlayTexture.NO_OVERLAY,null);}
        else{pose.scale(1,-1,-1);pose.translate(0,-1.501,0);for(String tex:new String[]{"ice_tomb","ice_tomb_cull"})collector.order(0).submitModelPart(model,pose,RenderTypes.entityTranslucent(Jugcraft.id("textures/jimtafacts/spellbooks/entity/ice_tomb/"+tex+".png")),s.lightCoords,OverlayTexture.NO_OVERLAY,null);}
        pose.popPose();super.submit(s,pose,collector,camera);
    }
    public static LayerDefinition spikeLayer(){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        var bottom=root.addOrReplaceChild("bottom",CubeListBuilder.create().texOffs(0,0).addBox(-1,-25,-9,10,24,10),PartPose.offset(-4,25,4));
        bottom.addOrReplaceChild("cube_r1",CubeListBuilder.create().texOffs(40,3).addBox(-5,-10,-1,6,10,6),PartPose.offsetAndRotation(8,0,-8,.3295F,-.1172F,.3295F));bottom.addOrReplaceChild("cube_r2",CubeListBuilder.create().texOffs(40,3).addBox(-5,-10,-1,6,10,6),PartPose.offsetAndRotation(0,0,0,-2.812F,.1172F,2.812F));
        var middle=root.addOrReplaceChild("middle",CubeListBuilder.create().texOffs(0,34).addBox(-1,-25,-1,8,22,8),PartPose.offset(-3,3,-3));
        middle.addOrReplaceChild("cube_r3",CubeListBuilder.create().texOffs(40,3).addBox(-5,-10,-1,6,8,6),PartPose.offsetAndRotation(6,0,6,-1.3526F,-1.3526F,1.5708F));middle.addOrReplaceChild("cube_r4",CubeListBuilder.create().texOffs(40,3).addBox(-5,-10,-1,6,8,6),PartPose.offsetAndRotation(0,0,0,1.789F,1.3526F,1.5708F));
        var top=root.addOrReplaceChild("top",CubeListBuilder.create().texOffs(39,38).addBox(-1,-25,-3,4,22,4),PartPose.offset(-1,-19,1));
        top.addOrReplaceChild("cube_r5",CubeListBuilder.create().texOffs(40,3).addBox(-5,-10,-1,6,8,6),PartPose.offsetAndRotation(2,0,-2,.1719F,-.0302F,.1719F));top.addOrReplaceChild("cube_r6",CubeListBuilder.create().texOffs(40,3).addBox(-5,-10,-1,6,8,6),PartPose.offsetAndRotation(0,0,0,-2.9697F,.0302F,2.9697F));return LayerDefinition.create(mesh,64,64);
    }
}
