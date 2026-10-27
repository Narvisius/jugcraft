package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.fluid.ElectricPumpBlockEntity;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/** Original 1/16-radius intake tube and original tube UV regions; bounded to 32 blocks. */
public final class FactoryPumpRenderer implements BlockEntityRenderer<ElectricPumpBlockEntity,FactoryPumpRenderer.State> {
    public static final class State extends BlockEntityRenderState {float length;}
    public FactoryPumpRenderer(BlockEntityRendererProvider.Context c){}
    @Override public State createRenderState(){return new State();}
    @Override public boolean shouldRenderOffScreen(){return true;}
    @Override public void extractRenderState(ElectricPumpBlockEntity n,State s,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay crumble){BlockEntityRenderState.extractBase(n,s,crumble);s.length=n.factoryPump()?n.tubeLength(partial):0;}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){
        if(s.length<=0)return;
        out.submitCustomGeometry(pose,RenderTypes.entityCutout(Jugcraft.id("textures/block/jimtafacts/factory/pump/tube.png")),(matrix,b)->{
            float a=.4375F,z=.5625F;
            for(int i=0;i<Math.ceil(s.length);i++){
                float top=-i,bottom=-Math.min(i+1,s.length),v=(top-bottom)*.5F;
                quad(b,matrix,s.lightCoords,0,0,-1,0,v,a,bottom,a,a,top,a,z,top,a,z,bottom,a);
                quad(b,matrix,s.lightCoords,0,0,1,0,v,z,bottom,z,z,top,z,a,top,z,a,bottom,z);
                quad(b,matrix,s.lightCoords,-1,0,0,0,v,a,bottom,z,a,top,z,a,top,a,a,bottom,a);
                quad(b,matrix,s.lightCoords,1,0,0,0,v,z,bottom,a,z,top,a,z,top,z,z,bottom,z);
            }
            quad(b,matrix,s.lightCoords,0,-1,0,.5F,1,a,-s.length,a,z,-s.length,a,z,-s.length,z,a,-s.length,z);
        });
    }
    private static void quad(VertexConsumer b,PoseStack.Pose p,int light,float nx,float ny,float nz,float v0,float v1,float... c){
        for(int i=0;i<4;i++)b.addVertex(p,c[i*3],c[i*3+1],c[i*3+2]).setColor(-1).setUv(i<2?0:1,i==0||i==3?v1:v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p,nx,ny,nz);
    }
}
