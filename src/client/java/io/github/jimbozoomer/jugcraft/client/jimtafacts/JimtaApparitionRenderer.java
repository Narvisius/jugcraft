package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaApparition;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Numeric source IceSpikeRenderer geometry and DevourJawRenderer keyframe equations. */
public final class JimtaApparitionRenderer extends EntityRenderer<JimtaApparition,JimtaApparitionRenderer.State> {
    public static final class State extends EntityRenderState{float time,size,yaw,offset;boolean jaw;int wait;}
    private final Form model;private final boolean jaw;
    public JimtaApparitionRenderer(EntityRendererProvider.Context c,boolean jaw){super(c);this.jaw=jaw;model=new Form(jaw?c.bakeLayer(ModelLayers.EVOKER_FANGS):Form.createBodyLayer().bakeRoot(),jaw);}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(JimtaApparition e,State s,float partial){super.extractRenderState(e,s,partial);s.time=e.age()+partial;s.size=e.size();s.yaw=e.getYRot();s.jaw=jaw;s.wait=e.waitTicks();float t=s.time-s.wait;s.offset=t<0?-1:t<6?(float)(Math.sin(t/6*Math.PI)/Math.PI+t/6-1):t<46?0:-(float)(Math.sin((Math.clamp((t-46)/30,0,1)+1)*Math.PI)/Math.PI+Math.clamp((t-46)/30,0,1));}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        if(s.jaw&&s.time<5)return;pose.pushPose();pose.rotateDegrees(Axis.YP,-s.yaw);if(s.jaw)pose.scale(-1.85F,-1.85F,1.85F);else{float scale=(s.size-1)*.25F+1;pose.scale(scale,-scale,scale);pose.translate(0,-s.offset*68/16F,0);}
        collector.order(0).submitModel(model,s,pose,RenderTypes.entityCutout(Jugcraft.id("textures/jimtafacts/spellbooks/entity/"+(s.jaw?"devour_jaw":"ice_spike")+".png")),s.lightCoords,OverlayTexture.NO_OVERLAY,-1,null,s.outlineColor);pose.popPose();super.submit(s,pose,collector,camera);
    }
    private static final class Form extends EntityModel<State>{
        private final boolean jaw;
        Form(ModelPart root,boolean jaw){super(root);this.jaw=jaw;}
        @Override public void setupAnim(State state){super.setupAnim(state);root.resetPose();if(jaw){float t=state.time-5;float f=1-(float)Math.pow(Math.clamp(t/8,0,1),4);var upper=root.getChild("upper_jaw");var lower=root.getChild("lower_jaw");upper.zRot=(float)Math.PI-f*.35F*(float)Math.PI;lower.zRot=(float)Math.PI+f*.35F*(float)Math.PI;float lift=.5F*(float)Math.cos(Math.PI*.5*(t/8-1))+.5F;upper.y=lower.y=root.getChild("base").y=-18*lift*lift+16;}
            else{var bottom=root.getChild("bottom");var middle=root.getChild("middle");var top=root.getChild("top");bottom.visible=state.size>=3;top.visible=state.size>=2;int ypos=26;if(bottom.visible)bottom.y=ypos-=26;middle.y=ypos-=22;if(top.visible)top.y=ypos-=22;}}
        public static LayerDefinition createBodyLayer() {
            MeshDefinition meshdefinition = new MeshDefinition();
            PartDefinition partdefinition = meshdefinition.getRoot();
            PartDefinition bottom = partdefinition.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, -25.0f, -9.0f, 10.0f, 24.0f, 10.0f, new CubeDeformation(0.0f)), PartPose.offset(-4.0f, 25.0f, 4.0f));
            PartDefinition cube_r1 = bottom.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(40, 3).addBox(-5.0f, -10.0f, -1.0f, 6.0f, 10.0f, 6.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(8.0f, 0.0f, -8.0f, 0.3295f, -0.1172f, 0.3295f));
            PartDefinition cube_r2 = bottom.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(40, 3).addBox(-5.0f, -10.0f, -1.0f, 6.0f, 10.0f, 6.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, -2.812f, 0.1172f, 2.812f));
            PartDefinition middle = partdefinition.addOrReplaceChild("middle", CubeListBuilder.create().texOffs(0, 34).addBox(-1.0f, -25.0f, -1.0f, 8.0f, 22.0f, 8.0f, new CubeDeformation(0.0f)), PartPose.offset(-3.0f, 3.0f, -3.0f));
            PartDefinition cube_r3 = middle.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(40, 3).addBox(-5.0f, -10.0f, -1.0f, 6.0f, 8.0f, 6.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(6.0f, 0.0f, 6.0f, -1.3526f, -1.3526f, 1.5708f));
            PartDefinition cube_r4 = middle.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(40, 3).addBox(-5.0f, -10.0f, -1.0f, 6.0f, 8.0f, 6.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 1.789f, 1.3526f, 1.5708f));
            PartDefinition top = partdefinition.addOrReplaceChild("top", CubeListBuilder.create().texOffs(39, 38).addBox(-1.0f, -25.0f, -3.0f, 4.0f, 22.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offset(-1.0f, -19.0f, 1.0f));
            PartDefinition cube_r5 = top.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(40, 3).addBox(-5.0f, -10.0f, -1.0f, 6.0f, 8.0f, 6.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(2.0f, 0.0f, -2.0f, 0.1719f, -0.0302f, 0.1719f));
            PartDefinition cube_r6 = top.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(40, 3).addBox(-5.0f, -10.0f, -1.0f, 6.0f, 8.0f, 6.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, -2.9697f, 0.0302f, 2.9697f));
            return LayerDefinition.create(meshdefinition, (int)64, (int)64);
        }

    }
}
