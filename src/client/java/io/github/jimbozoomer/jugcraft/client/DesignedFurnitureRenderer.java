package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.furniture.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public final class DesignedFurnitureRenderer implements BlockEntityRenderer<DesignedFurnitureEntity,DesignedFurnitureRenderer.State> {
    private static final String[] TEXTURES={"oak_planks","spruce_planks","birch_planks","iron_block","white_wool","glass","stone","purple_wool"};
    public static final class State extends BlockEntityRenderState {FurnitureDesign design=FurnitureDesign.stool();int turns;}
    public DesignedFurnitureRenderer(BlockEntityRendererProvider.Context c){}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(DesignedFurnitureEntity e,State s,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){BlockEntityRenderState.extractBase(e,s,overlay);s.design=e.design();s.turns=FurnitureDesign.turns(e.getBlockState().getValue(DesignedFurnitureBlock.FACING));}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        pose.pushPose();pose.translate(.5,0,.5);pose.rotateDegrees(Axis.YP,-90*s.turns);pose.translate(-.5,0,-.5);
        for(var p:s.design.parts()){
            var texture=Identifier.withDefaultNamespace("textures/block/"+TEXTURES[p.material()]+".png");
            var type=p.material()==5?RenderTypes.entityTranslucent(texture):RenderTypes.entityCutout(texture);
            int light=s.lightCoords;
            collector.submitCustomGeometry(pose,type,(matrix,b)->{
                float x=p.x()/16F,y=p.y()/16F,z=p.z()/16F,X=(p.x()+p.w())/16F,Y=(p.y()+p.h())/16F,Z=(p.z()+p.d())/16F;
                float[][][] faces={{{x,y,z},{x,Y,z},{X,Y,z},{X,y,z}},{{X,y,Z},{X,Y,Z},{x,Y,Z},{x,y,Z}},{{x,y,Z},{x,Y,Z},{x,Y,z},{x,y,z}},{{X,y,z},{X,Y,z},{X,Y,Z},{X,y,Z}},{{x,Y,z},{x,Y,Z},{X,Y,Z},{X,Y,z}},{{x,y,Z},{x,y,z},{X,y,z},{X,y,Z}}};
                int[][] normals={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};
                for(int face=0;face<(p.sprite()?2:6);face++)for(int i=0;i<4;i++){var v=faces[face][i];var n=normals[face];b.addVertex(matrix,v[0],v[1],v[2]).setColor(0xFFFFFFFF).setUv(i<2?0:1,i==0||i==3?1:0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix,n[0],n[1],n[2]);}
            });
        }pose.popPose();
    }
}
