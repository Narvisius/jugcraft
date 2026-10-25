package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.ConcordanceClientOptions;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.WorkshopEngines;
import java.util.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** Direct source engine_base/mj_dynamo element dimensions, UVs and triangular piston extension. */
public final class WorkshopEngineRenderer implements BlockEntityRenderer<WorkshopEngines.Node,WorkshopEngineRenderer.State> {
    private final Map<WorkshopEngines.Node,double[]> motion=new WeakHashMap<>();
    public WorkshopEngineRenderer(BlockEntityRendererProvider.Context context){}
    public static void register(){BlockEntityRenderers.register(WorkshopEngines.TYPE,WorkshopEngineRenderer::new);}
    public static final class State extends BlockEntityRenderState {String kind;int stage;Direction facing;float extension;}
    public State createRenderState(){return new State();}
    public void extractRenderState(WorkshopEngines.Node n,State s,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){BlockEntityRenderState.extractBase(n,s,overlay);s.kind=n.kind();s.stage=n.getBlockState().getValue(WorkshopEngines.STAGE);s.facing=n.facing();
        double now=n.getLevel()==null?0:n.getLevel().getGameTime()+partial;double[] phase=motion.computeIfAbsent(n,key->new double[]{now,0});
        if(n.getBlockState().getValue(WorkshopEngines.LIT)&&!ConcordanceClientOptions.reducedMotion())phase[1]=(phase[1]+Math.clamp(now-phase[0],0,2)*(.01+.006*s.stage))%1;
        phase[0]=now;s.extension=(float)Math.min(phase[1],1-phase[1])*15.99F;
    }
    public void submit(State s,PoseStack pose,SubmitNodeCollector out,net.minecraft.client.renderer.state.level.CameraRenderState camera){
        pose.pushPose();pose.translate(.5,.5,.5);switch(s.facing){case DOWN->pose.rotateDegrees(Axis.XP,180);case NORTH->pose.rotateDegrees(Axis.XP,-90);case SOUTH->pose.rotateDegrees(Axis.XP,90);case WEST->pose.rotateDegrees(Axis.ZP,90);case EAST->pose.rotateDegrees(Axis.ZP,-90);default->{}}pose.translate(-.5,-.5,-.5);
        String base="textures/block/jimtafacts/engines/"+s.kind+"_",trunk="textures/block/jimtafacts/engines/trunk_"+new String[]{"blue","green","yellow","red","overheat"}[s.stage]+".png";
        box(out,pose,base+"back.png",base+"side.png",0,0,0,16,4,16,new float[]{0,0,16,16},new float[]{0,0,16,4},s.lightCoords,false);
        boolean dynamo=s.kind.equals("kinetic_dynamo");
        box(out,pose,base+(dynamo?"front":"back")+".png",base+(dynamo?"front":"side")+".png",dynamo?2:0,4+s.extension,dynamo?2:0,dynamo?14:16,8+s.extension,dynamo?14:16,
            dynamo?new float[]{0,0,12,12}:new float[]{0,0,16,16},dynamo?new float[]{0,12,12,16}:new float[]{0,0,16,4},s.lightCoords,false);
        box(out,pose,trunk,trunk,4,4,4,12,16,12,new float[]{0,0,8,8},new float[]{8,0,16,12},s.lightCoords,false);
        if(s.extension>.001F)box(out,pose,"textures/block/jimtafacts/engines/chamber.png","textures/block/jimtafacts/engines/chamber.png",3,4,3,13,4+s.extension,13,new float[]{0,0,0,0},new float[]{3,s.extension,13,0},s.lightCoords,true);
        pose.popPose();
    }
    private static void box(SubmitNodeCollector out,PoseStack pose,String ends,String sides,float x,float y,float z,float X,float Y,float Z,float[] endUv,float[] sideUv,int light,boolean wallsOnly){
        for(Direction face:Direction.values()){boolean end=face.getAxis()==Direction.Axis.Y;if(wallsOnly&&end)continue;var uv=end?endUv:sideUv;var texture=Jugcraft.id(end?ends:sides);
            float[][] points=switch(face){case NORTH->new float[][]{{X,Y,z},{X,y,z},{x,y,z},{x,Y,z}};case SOUTH->new float[][]{{x,Y,Z},{x,y,Z},{X,y,Z},{X,Y,Z}};
                case WEST->new float[][]{{x,Y,z},{x,y,z},{x,y,Z},{x,Y,Z}};case EAST->new float[][]{{X,Y,Z},{X,y,Z},{X,y,z},{X,Y,z}};
                case UP->new float[][]{{x,Y,z},{x,Y,Z},{X,Y,Z},{X,Y,z}};case DOWN->new float[][]{{x,y,Z},{x,y,z},{X,y,z},{X,y,Z}};};
            out.submitCustomGeometry(pose,RenderTypes.entityCutout(texture),(matrix,vertices)->{for(int i=0;i<4;i++)vertices.addVertex(matrix,points[i][0]/16,points[i][1]/16,points[i][2]/16).setColor(0xffffffff)
                .setUv((i<2?uv[0]:uv[2])/16,(i==0||i==3?uv[1]:uv[3])/16).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix,face.getStepX(),face.getStepY(),face.getStepZ());});
        }
    }
}
