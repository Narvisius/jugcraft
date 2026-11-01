package io.github.jimbozoomer.jugcraft.client.aquatic;

import com.mojang.blaze3d.vertex.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.aquatic.OwnerAquaticGear;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.FishingHookRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.component.DyedItemColor;

/** Retains native hand position/culling; supplied layered bobber and hook skins with actual tackle dyes. */
public final class OwnerFishingHookRenderer extends FishingHookRenderer {
    public OwnerFishingHookRenderer(EntityRendererProvider.Context c){super(c);}
    public static final class State extends FishingHookRenderState {boolean aquatic,bobber;int lineColor,bobberColor;String hook="hook";}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(FishingHook hook,FishingHookRenderState state,float partial){
        super.extractRenderState(hook,state,partial);var s=(State)state;s.aquatic=false;
        var owner=hook.getPlayerOwner();if(owner==null)return;var rod=OwnerAquaticGear.held(owner);if(rod.isEmpty())return;s.aquatic=true;
        var line=OwnerAquaticGear.part(rod,2);var bobber=OwnerAquaticGear.part(rod,3);
        s.bobber=!bobber.isEmpty();s.lineColor=0xFF000000|DyedItemColor.getOrDefault(line,0);s.bobberColor=0xFF000000|DyedItemColor.getOrDefault(bobber,0xC12626);
        String part=OwnerAquaticGear.hook(rod);s.hook=part.isEmpty()?"hook":part+"_hook";
    }
    @Override public void submit(FishingHookRenderState state,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){
        var s=(State)state;if(!s.aquatic){super.submit(state,pose,out,camera);return;}
        pose.pushPose();pose.pushPose();pose.scale(.5f,.5f,.5f);pose.rotate(camera.orientation);
        quad(pose,out,s,"bobber/"+(s.bobber?"bobber_overlay":"bobber_vanilla"),s.bobber?s.bobberColor:-1);
        if(s.bobber)quad(pose,out,s,"bobber/bobber",-1);
        quad(pose,out,s,"hook/"+s.hook,-1);pose.popPose();
        float x=(float)s.lineOriginOffset.x,y=(float)s.lineOriginOffset.y,z=(float)s.lineOriginOffset.z;
        float width=Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth;
        int color=s.lineColor;
        out.submitCustomGeometry(pose,RenderTypes.lines(),(p,v)->{for(int i=0;i<16;i++){float a=i/16f,b=(i+1)/16f;line(v,p,x,y,z,a,b,width,color);line(v,p,x,y,z,b,a,width,color);}});
        pose.popPose();
    }
    private static void quad(PoseStack pose,SubmitNodeCollector out,State s,String texture,int color){
        int light=s.lightCoords;
        out.submitCustomGeometry(pose,RenderTypes.entityCutout(Jugcraft.id("textures/newest/aquatic/entity/rod/"+texture+".png")),(p,v)->{
            vertex(v,p,light,0,0,0,1,color);vertex(v,p,light,1,0,1,1,color);vertex(v,p,light,1,1,1,0,color);vertex(v,p,light,0,1,0,0,color);
        });
    }
    private static void vertex(VertexConsumer v,PoseStack.Pose p,int light,float x,float y,float u,float w,int color){v.addVertex(p,x-.5f,y-.5f,0).setColor(color).setUv(u,w).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p,0,1,0);}
    private static void line(VertexConsumer v,PoseStack.Pose p,float x,float y,float z,float a,float b,float width,int color){
        float px=x*a,py=y*(a*a+a)*.5f+.25f,pz=z*a,nx=x*b-px,ny=y*(b*b+b)*.5f+.25f-py,nz=z*b-pz;
        float len=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(len<.00001f)return;
        v.addVertex(p,px,py,pz).setColor(color).setNormal(p,nx/len,ny/len,nz/len).setLineWidth(width);
    }
}
