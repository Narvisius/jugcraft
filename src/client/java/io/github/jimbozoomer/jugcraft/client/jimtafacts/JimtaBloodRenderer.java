package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaBloodProjectile;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Source quad vertices, UVs, dark-red vertex colour and needle/slash animation timings. */
public final class JimtaBloodRenderer extends EntityRenderer<JimtaBloodProjectile,JimtaBloodRenderer.State> {
    public static final class State extends EntityRenderState{boolean slash,small;float time,yaw,pitch,width;}
    public JimtaBloodRenderer(EntityRendererProvider.Context context){super(context);}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(JimtaBloodProjectile e,State s,float partial){super.extractRenderState(e,s,partial);s.slash=e.slash();s.small=e.spell().equals("acupuncture");s.time=e.tickCount+partial;var motion=e.getDeltaMovement();s.yaw=(float)(-Math.toDegrees(Math.atan2(motion.z,motion.x))-90);s.pitch=(float)(90-Math.toDegrees(Math.atan2(motion.horizontalDistance(),motion.y)));s.width=s.slash?(float)Math.min(4,.6+s.time*.1):2.5F*(s.small?.4F:1F);}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        pose.pushPose();pose.rotateDegrees(Axis.YP,s.yaw);pose.rotateDegrees(Axis.XP,s.pitch);
        if(s.slash){
            pose.rotateDegrees(Axis.YP,-15);pose.rotateDegrees(Axis.ZP,-10);quad(s,pose,collector,4);
            pose.rotateDegrees(Axis.YP,30);pose.rotateDegrees(Axis.ZP,20);quad(s,pose,collector,0);
        }else{pose.rotateDegrees(Axis.ZP,s.time*40);pose.rotateDegrees(Axis.XP,45);quad(s,pose,collector,0);}
        pose.popPose();super.submit(s,pose,collector,camera);
    }
    private static void quad(State s,PoseStack pose,SubmitNodeCollector collector,int frameOffset){
        var texture=s.slash?Identifier.withDefaultNamespace("textures/particle/sweep_"+((int)(s.time/6)+frameOffset)%8+".png"):Jugcraft.id("textures/jimtafacts/spellbooks/entity/blood_needle/needle_5.png");
        float w=s.width/2;float[][] vertices=s.slash?new float[][]{{-w,-.1F,-w},{w,-.1F,-w},{w,-.1F,w},{-w,-.1F,w}}:new float[][]{{0,-w,-w},{0,w,-w},{0,w,w},{0,-w,w}};
        collector.submitCustomGeometry(pose,RenderTypes.entityCutout(texture),(matrix,buffer)->{
            float[][] uv={{0,1},{1,1},{1,0},{0,0}};for(int i=0;i<4;i++)buffer.addVertex(matrix,vertices[i][0],vertices[i][1],vertices[i][2]).setColor(0xFF5A000A).setUv(uv[i][0],uv[i][1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(s.lightCoords).setNormal(matrix,0,1,0);
        });
    }
}
