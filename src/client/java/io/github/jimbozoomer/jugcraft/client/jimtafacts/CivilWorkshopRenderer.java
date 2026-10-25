package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import com.mojang.blaze3d.vertex.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.ConcordanceClientOptions;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.CivilWorkshops;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Source RenderQuarry rail radius, drill dimensions and UV rows, following real native work positions.
 * Source: owner-library/originals/Oct23/Jimtafacts/buildcraft reborn by jimbo.
 * No source executable is loaded; the renderer receives geometry/status only, never machine inventory. */
public final class CivilWorkshopRenderer implements BlockEntityRenderer<CivilWorkshops.Node,CivilWorkshopRenderer.State> {
    private static final Identifier FRAME=Jugcraft.id("textures/block/jimtafacts/civil/buildcraftbuilders/blocks/frame/default.png");
    private static final Identifier DRILL=Jugcraft.id("textures/block/jimtafacts/civil/buildcraftbuilders/blocks/quarry/drill.png");
    public CivilWorkshopRenderer(BlockEntityRendererProvider.Context context) {}
    public static void register(){BlockEntityRenderers.register(CivilWorkshops.ENTITY,CivilWorkshopRenderer::new);}
    public static final class State extends BlockEntityRenderState { boolean visible; Vec3 min,max,head; }
    @Override public State createRenderState(){return new State();}
    @Override public boolean shouldRenderOffScreen(){return true;}
    @Override public int getViewDistance(){return 128;}
    @Override public void extractRenderState(CivilWorkshops.Node node,State s,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){
        BlockEntityRenderState.extractBase(node,s,overlay);s.visible=false;
        if(!node.kind().equals("quarry")||node.minimum==null||node.maximum==null||node.minimum.distSqr(node.maximum)>3072)return;
        Vec3 origin=Vec3.atLowerCornerOf(node.getBlockPos());
        s.min=Vec3.atLowerCornerOf(node.minimum).subtract(origin);s.max=Vec3.atLowerCornerOf(node.maximum).subtract(origin);
        Vec3 target=Vec3.atLowerCornerOf(node.visibleHead==null?node.maximum:node.visibleHead);
        Vec3 previous=Vec3.atLowerCornerOf(node.visibleFrom==null?node.maximum:node.visibleFrom);
        double ticks=node.getLevel()==null?0:node.getLevel().getGameTime()+partial;
        double f=ConcordanceClientOptions.reducedMotion()?1:Math.clamp((ticks-node.visibleAt)/5,0,1);
        s.head=previous.lerp(target,f*f*(3-2*f)).subtract(origin);s.visible=true;
    }
    @Override public void submit(State s,PoseStack stack,SubmitNodeCollector nodes,net.minecraft.client.renderer.state.level.CameraRenderState camera){
        if(!s.visible)return;
        nodes.submitCustomGeometry(stack,RenderTypes.entityCutout(FRAME),(pose,out)->{
            double x0=s.min.x+.25,x1=s.max.x+.75,z0=s.min.z+.25,z1=s.max.z+.75,top=s.max.y+1.5;
            // Four fixed upper rails and four supports surround the actual selected work volume.
            beam(out,pose,new Vec3(x0,top,z0),new Vec3(x1,top,z0),s.lightCoords,false);
            beam(out,pose,new Vec3(x0,top,z1),new Vec3(x1,top,z1),s.lightCoords,false);
            beam(out,pose,new Vec3(x0,top,z0),new Vec3(x0,top,z1),s.lightCoords,false);
            beam(out,pose,new Vec3(x1,top,z0),new Vec3(x1,top,z1),s.lightCoords,false);
            // Supports end at the selected top surface rather than floating below the gantry.
            for(double x:new double[]{x0,x1})for(double z:new double[]{z0,z1})beam(out,pose,new Vec3(x,s.max.y,z),new Vec3(x,top,z),s.lightCoords,false);
            double hx=s.head.x+.5,hz=s.head.z+.5;
            beam(out,pose,new Vec3(hx,top,z0),new Vec3(hx,top,z1),s.lightCoords,false);
            beam(out,pose,new Vec3(x0,top,hz),new Vec3(x1,top,hz),s.lightCoords,false);
            beam(out,pose,new Vec3(hx,s.head.y+1.25,hz),new Vec3(hx,top,hz),s.lightCoords,false);
        });
        nodes.submitCustomGeometry(stack,RenderTypes.entityCutout(DRILL),(pose,out)->beam(out,pose,s.head.add(.5,.25,.5),s.head.add(.5,1.25,.5),s.lightCoords,true));
    }
    private static void beam(VertexConsumer out,PoseStack.Pose pose,Vec3 a,Vec3 b,int light,boolean drill){
        int axis=Math.abs(a.x-b.x)>.001?0:Math.abs(a.y-b.y)>.001?1:2;
        double[] start={a.x,a.y,a.z},end={b.x,b.y,b.z};double low=Math.min(start[axis],end[axis]),high=Math.max(start[axis],end[axis]);
        // Split long rails into repeated texture tiles, not a single stretched white stripe.
        for(double p=low;p<high;p+=1){double[] lo={a.x-.0625,a.y-.0625,a.z-.0625},hi={a.x+.0625,a.y+.0625,a.z+.0625};lo[axis]=p;hi[axis]=Math.min(high,p+1);cube(out,pose,lo,hi,light,drill);}
    }
    private static void cube(VertexConsumer out,PoseStack.Pose pose,double[] lo,double[] hi,int light,boolean drill){
        double x=lo[0],y=lo[1],z=lo[2],X=hi[0],Y=hi[1],Z=hi[2];
        face(out,pose,Direction.NORTH,new double[][]{{X,y,z},{x,y,z},{x,Y,z},{X,Y,z}},light,drill);
        face(out,pose,Direction.SOUTH,new double[][]{{x,y,Z},{X,y,Z},{X,Y,Z},{x,Y,Z}},light,drill);
        face(out,pose,Direction.WEST,new double[][]{{x,y,z},{x,y,Z},{x,Y,Z},{x,Y,z}},light,drill);
        face(out,pose,Direction.EAST,new double[][]{{X,y,Z},{X,y,z},{X,Y,z},{X,Y,Z}},light,drill);
        face(out,pose,Direction.UP,new double[][]{{x,Y,Z},{X,Y,Z},{X,Y,z},{x,Y,z}},light,drill);
        face(out,pose,Direction.DOWN,new double[][]{{x,y,z},{X,y,z},{X,y,Z},{x,y,Z}},light,drill);
    }
    private static void face(VertexConsumer out,PoseStack.Pose pose,Direction face,double[][] points,int light,boolean drill){
        float bottom=drill?0:.25F,top=drill?.25F:.75F;float[][] uv={{0,top},{1,top},{1,bottom},{0,bottom}};
        for(int i=0;i<4;i++)out.addVertex(pose,(float)points[i][0],(float)points[i][1],(float)points[i][2]).setColor(-1).setUv(uv[i][0],uv[i][1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,face.getStepX(),face.getStepY(),face.getStepZ());
    }
}
