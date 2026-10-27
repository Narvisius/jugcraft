package local.peepo.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import local.peepo.WorkAnimation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Client-only line/bobber geometry; no additional networked entities. */
final class FishingLayer extends RenderLayer<PeepoState,PeepoModel> {
    FishingLayer(PeepoRenderer parent){super(parent);}
    @Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,PeepoState s,float yaw,float pitch){
        if(s.isInvisible || !s.work.isFishing() || s.fishingRod.isEmpty())return;
        var p=s.toolPose;
        pose.pushPose();pose.translate(p.x/16,p.y/16,p.z/16);
        pose.scale(FishingPose.ROD_SCALE,FishingPose.ROD_SCALE,FishingPose.ROD_SCALE);
        pose.rotateDegrees(Axis.XP,p.xRot*Mth.RAD_TO_DEG);pose.rotateDegrees(Axis.YP,90);pose.rotateDegrees(Axis.ZP,-45);pose.rotateDegrees(Axis.XP,180);pose.translate(.25,.25,0);
        s.fishingRod.submit(pose,collector,light,OverlayTexture.NO_OVERLAY,s.outlineColor);pose.popPose();
        float tip=11.31F*FishingPose.ROD_SCALE;
        float tx=p.x/16,ty=(p.y-tip*Mth.cos(p.xRot))/16,tz=(p.z-tip*Mth.sin(p.xRot))/16;
        float travel=s.work==WorkAnimation.FISH_CAST?FishingPose.smooth((s.fishTime-8)/12):s.work==WorkAnimation.FISH_REEL?1-FishingPose.smooth(s.fishTime/20):1;
        float bx=Mth.lerp(travel,tx,s.fishX/16),by=Mth.lerp(travel,ty,s.fishY/16),bz=Mth.lerp(travel,tz,s.fishZ/16);
        if(s.work==WorkAnimation.FISH_CAST)by-=Mth.sin(travel*Mth.PI)*.5F;
        else if(s.work==WorkAnimation.FISH_BITE)by+=.035F+Mth.sin(s.fishTime*1.6F)*.025F;
        else if(s.work==WorkAnimation.FISH_WAIT)by+=Mth.sin(s.ageInTicks*.14F)*.012F;
        final float x=bx,y=by,z=bz;
        collector.submitCustomGeometry(pose,RenderTypes.lines(),(matrix,c)->{
            for(int i=0;i<12;i++){
                float a=i/12F,b=(i+1)/12F;
                line(matrix,c,Mth.lerp(a,tx,x),Mth.lerp(a,ty,y)+Mth.sin(a*Mth.PI)*.10F,Mth.lerp(a,tz,z),Mth.lerp(b,tx,x),Mth.lerp(b,ty,y)+Mth.sin(b*Mth.PI)*.10F,Mth.lerp(b,tz,z));
            }
        });
        collector.submitCustomGeometry(pose,RenderTypes.entityCutout(Identifier.withDefaultNamespace("textures/entity/fishing/fishing_hook.png")),(matrix,c)->{
            for(int side=0;side<2;side++)for(int back=0;back<2;back++){
                for(int i=0;i<4;i++){
                    int corner=back==0?i:3-i;float u=corner==0 || corner==3?0:1,v=corner<2?0:1;
                    c.addVertex(matrix,x+(side==0?(u-.5F)*.125F:0),y+(v-.5F)*.125F,z+(side==1?(u-.5F)*.125F:0)).setColor(0xFFFFFFFF).setUv(u,v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix,side==1?1:0,0,side==0?1:0);
                }
            }
        });
    }
    private static void line(PoseStack.Pose p,VertexConsumer c,float x,float y,float z,float a,float b,float d){
        float nx=a-x,ny=b-y,nz=d-z,length=Mth.sqrt(nx*nx+ny*ny+nz*nz);if(length<.0001F)return;
        c.addVertex(p,x,y,z).setColor(0xFF303030).setNormal(p,nx/length,ny/length,nz/length).setLineWidth(1);
        c.addVertex(p,a,b,d).setColor(0xFF303030).setNormal(p,nx/length,ny/length,nz/length).setLineWidth(1);
    }
}
