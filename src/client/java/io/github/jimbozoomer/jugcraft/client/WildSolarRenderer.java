package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.wilds.WildSolar;
import java.util.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Original sunstrike/beam atlases and four-shell nova, captured before render submission. */
final class WildSolarRenderer extends EntityRenderer<WildSolar,WildSolarRenderer.State>{
    private final ModelPart nova;
    WildSolarRenderer(EntityRendererProvider.Context c){super(c);nova=RecoveredWildlifeModel.bake(RecoveredWildlifeModel.read("recovered/wilds_modelsupernova"));}
    static final class State extends EntityRenderState{int kind;float age,height;boolean firstPerson;net.minecraft.core.Direction side;Vec3 end=Vec3.ZERO;final List<float[][]> scorch=new ArrayList<>();}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(WildSolar e,State s,float partial){super.extractRenderState(e,s,partial);s.kind=e.kind();s.age=e.age()+partial;s.firstPerson=e.owner()==net.minecraft.client.Minecraft.getInstance().player&&net.minecraft.client.Minecraft.getInstance().options.getCameraType().isFirstPerson();s.side=null;s.end=Vec3.ZERO;if(e.kind()==WildSolar.BEAM){var hit=e.beamHit();s.end=hit.getLocation().subtract(e.position());if(hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK)s.side=hit.getDirection();}s.height=(float)Math.min(256,e.level().getMaxY()-e.getY());s.scorch.clear();
        if(s.kind==WildSolar.STRIKE&&s.age>43)for(var p:BlockPos.betweenClosed(e.blockPosition().offset(-2,-2,-2),e.blockPosition().offset(2,0,2))){if(!e.level().hasChunkAt(p)||!e.level().getBlockState(p).isSolidRender()||!e.level().getBlockState(p.above()).isAir())continue;float x0=(float)Math.max(-1.2,p.getX()-e.getX()),z0=(float)Math.max(-1.2,p.getZ()-e.getZ()),x1=(float)Math.min(1.2,p.getX()+1-e.getX()),z1=(float)Math.min(1.2,p.getZ()+1-e.getZ()),y=(float)(p.getY()+1-e.getY()+.015625);if(x1<=x0||z1<=z0)continue;float[][] q={{x0,y,z0},{x0,y,z1},{x1,y,z1},{x1,y,z0}};float[][] out=new float[4][];for(int i=0;i<4;i++)out[i]=new float[]{q[i][0],q[i][1],q[i][2],(q[i][0]/2.4F+.5F)*.0625F+.75F,(q[i][2]/2.4F+.5F)*.5F+.5F};s.scorch.add(out);}
    }
    @Override public boolean shouldRender(WildSolar e,net.minecraft.client.renderer.culling.Frustum f,double x,double y,double z,float partial){return e.shouldRenderAtSqrDistance(x*x+y*y+z*z);}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){
        if(s.kind==WildSolar.NOVA){float fraction=Math.clamp(s.age/40,0,1),scale=(float)(Math.sqrt(fraction)*3+.05*Math.cos(s.age*3));int alpha=(int)(255*Math.max(.00001,(1-Math.pow(fraction,4))*.4));pose.pushPose();pose.translate(0,1,0);pose.scale(-1,-1,1);var type=RenderTypes.entityTranslucentEmissive(Jugcraft.id("textures/newest/wilds/effects/super_nova_"+((int)s.age%16+1)+".png"));for(int i=4;i>=1;i--){var part=nova.getChild("body"+i);part.resetPose();part.xScale=part.yScale=part.zScale=scale*(1-(i-1)*.2F);out.order(5-i).submitModelPart(part,pose,type,15728880,OverlayTexture.NO_OVERLAY,null,(alpha<<24)|0xFFFFFF,0);}pose.popPose();return;}
        var quads=new ArrayList<float[][]>();float opacity=1;
        if(s.kind==WildSolar.STRIKE){if(s.age>43){quads.addAll(s.scorch);opacity=.7F*Math.min(1,(1243-s.age)/80);}else{boolean drawing=s.age<35;float frac=drawing?s.age/35:(s.age-35)/8;int frame=Math.min(10,(int)(frac*11));float u=frame/16F,v=drawing?0:.5F;quads.add(flat(1.6F,u,v,u+.0625F,v+.5F));opacity=drawing?Math.min(1,frac*2)*.7F:1;float width=drawing?2-1.75F*frac:1;int beam=drawing?0:Math.min(31,(int)(frac*32));quads.add(new float[][]{{-width,0,0,.875F,beam/32F},{-width,s.height,0,.875F,(beam+1)/32F},{width,s.height,0,1,(beam+1)/32F},{width,0,0,1,beam/32F}});quads.add(new float[][]{{0,0,-width,.875F,beam/32F},{0,s.height,-width,.875F,(beam+1)/32F},{0,s.height,width,1,(beam+1)/32F},{0,0,width,1,beam/32F}});}}
        else{
            Vec3 axis=s.end.normalize(),right=axis.cross(new Vec3(0,1,0)).normalize();if(right.lengthSqr()<.1)right=new Vec3(1,0,0);Vec3 up=axis.cross(right).normalize();
            int frame=Math.clamp((int)((Math.min(3,s.age)-1)*2),0,6);float v=.5F+frame/32F;
            // Keep the caster's view open, as in the supplied first-person beam.
            for(var side:s.firstPerson?List.of(right):List.of(right,up)){
                Vec3 start=s.firstPerson?axis.scale(-1):Vec3.ZERO;
                quads.add(new float[][]{point(start.subtract(side),0,v),point(s.end.subtract(side),0,v+1/32F),point(s.end.add(side),.078125F,v+1/32F),point(start.add(side),.078125F,v)});
            }
            var cr=new org.joml.Vector3f(1,0,0).rotate(camera.orientation);var cu=new org.joml.Vector3f(0,1,0).rotate(camera.orientation);
            Vec3 cameraRight=new Vec3(cr.x,cr.y,cr.z),cameraUp=new Vec3(cu.x,cu.y,cu.z);
            if(!s.firstPerson)quads.add(cap(Vec3.ZERO,cameraRight,cameraUp,frame));
            quads.add(cap(s.end,cameraRight,cameraUp,frame));
            if(s.side!=null){Vec3 normal=new Vec3(s.side.getStepX(),s.side.getStepY(),s.side.getStepZ());Vec3 tangent=normal.cross(new Vec3(0,1,0));if(tangent.lengthSqr()<.1)tangent=new Vec3(1,0,0);tangent=tangent.normalize();quads.add(cap(s.end.add(normal.scale(.01)),tangent,normal.cross(tangent),frame));}
        }
        var captured=List.copyOf(quads);int color=((int)(Math.clamp(opacity,0,1)*255)<<24)|0xFFFFFF;var texture=Jugcraft.id("textures/newest/wilds/effects/"+(s.kind==WildSolar.STRIKE?"sunstrike":"solar_beam")+".png");out.submitCustomGeometry(pose,RenderTypes.entityTranslucentEmissive(texture),(matrix,buffer)->{for(var q:captured)for(var v:q)buffer.addVertex(matrix,v[0],v[1],v[2]).setColor(color).setUv(v[3],v[4]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(matrix,0,1,0);});
    }
    private static float[][] cap(Vec3 center,Vec3 right,Vec3 up,int frame){
        right=right.scale(1.3);up=up.scale(1.3);float u=frame/16F;
        return new float[][]{point(center.subtract(right).subtract(up),u,0),point(center.subtract(right).add(up),u,.5F),point(center.add(right).add(up),u+.0625F,.5F),point(center.add(right).subtract(up),u+.0625F,0)};
    }
    private static float[] point(Vec3 p,float u,float v){return new float[]{(float)p.x,(float)p.y,(float)p.z,u,v};}
    private static float[][] flat(float r,float u0,float v0,float u1,float v1){return new float[][]{{-r,.01F,-r,u0,v0},{-r,.01F,r,u0,v1},{r,.01F,r,u1,v1},{r,.01F,-r,u1,v0}};}
}
