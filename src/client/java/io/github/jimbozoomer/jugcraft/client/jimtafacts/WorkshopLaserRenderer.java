package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import com.mojang.blaze3d.vertex.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.ConcordanceClientOptions;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.WorkshopLaser;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Eight original beam frames from the supplied 16x128 sprite, rendered only during actual energy transfer. */
public final class WorkshopLaserRenderer implements BlockEntityRenderer<WorkshopLaser.Node,WorkshopLaserRenderer.State> {
    private static final Identifier TEXTURE=Jugcraft.id("textures/block/jimtafacts/laser/beam.png");
    public WorkshopLaserRenderer(BlockEntityRendererProvider.Context context){}
    public static void register(){BlockEntityRenderers.register(WorkshopLaser.TYPE,WorkshopLaserRenderer::new);}
    public static final class State extends BlockEntityRenderState {int distance,frame;Direction direction;}
    @Override public State createRenderState(){return new State();}
    @Override public boolean shouldRenderOffScreen(){return true;}
    @Override public int getViewDistance(){return 64;}
    @Override public void extractRenderState(WorkshopLaser.Node n,State s,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){
        BlockEntityRenderState.extractBase(n,s,overlay);s.distance=n.beamDistance;s.direction=n.getBlockState().getValue(WorkshopLaser.FACING);
        s.frame=ConcordanceClientOptions.reducedMotion()||n.getLevel()==null?0:(int)(n.getLevel().getGameTime()%8);
    }
    @Override public void submit(State s,PoseStack stack,SubmitNodeCollector nodes,net.minecraft.client.renderer.state.level.CameraRenderState camera){
        if(s.distance<=0||s.distance>16)return;
        nodes.submitCustomGeometry(stack,RenderTypes.entityCutout(TEXTURE),(pose,out)->{
            double[] start={.5,.5,.5};int axis=s.direction.getAxis()==Direction.Axis.X?0:s.direction.getAxis()==Direction.Axis.Y?1:2;
            int sign=s.direction.getStepX()+s.direction.getStepY()+s.direction.getStepZ();
            for(double offset=.3125;offset<s.distance;offset+=1){double a=.5+sign*offset,b=.5+sign*Math.min(s.distance,offset+1);double[] lo={.46875,.46875,.46875},hi={.53125,.53125,.53125};lo[axis]=Math.min(a,b);hi[axis]=Math.max(a,b);cube(out,pose,lo,hi,s.frame);}
        });
    }
    private static void cube(VertexConsumer out,PoseStack.Pose pose,double[] lo,double[] hi,int frame){
        double x=lo[0],y=lo[1],z=lo[2],X=hi[0],Y=hi[1],Z=hi[2];
        face(out,pose,Direction.NORTH,new double[][]{{X,y,z},{x,y,z},{x,Y,z},{X,Y,z}},frame);
        face(out,pose,Direction.SOUTH,new double[][]{{x,y,Z},{X,y,Z},{X,Y,Z},{x,Y,Z}},frame);
        face(out,pose,Direction.WEST,new double[][]{{x,y,z},{x,y,Z},{x,Y,Z},{x,Y,z}},frame);
        face(out,pose,Direction.EAST,new double[][]{{X,y,Z},{X,y,z},{X,Y,z},{X,Y,Z}},frame);
        face(out,pose,Direction.UP,new double[][]{{x,Y,Z},{X,Y,Z},{X,Y,z},{x,Y,z}},frame);
        face(out,pose,Direction.DOWN,new double[][]{{x,y,z},{X,y,z},{X,y,Z},{x,y,Z}},frame);
    }
    private static void face(VertexConsumer out,PoseStack.Pose pose,Direction normal,double[][] points,int frame){
        float a=frame/8F,b=(frame+1)/8F;float[][] uv={{0,b},{1,b},{1,a},{0,a}};
        for(int i=0;i<4;i++)out.addVertex(pose,(float)points[i][0],(float)points[i][1],(float)points[i][2]).setColor(-1).setUv(uv[i][0],uv[i][1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose,normal.getStepX(),normal.getStepY(),normal.getStepZ());
    }
}
