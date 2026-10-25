package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.ConcordanceClientOptions;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialHeat;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/** Recovered KineticGeneratorRotor box dimensions, mirrored 32x256 UVs, blade pitch and source-facing transform. */
public final class IndustrialRotorRenderer implements BlockEntityRenderer<IndustrialHeat.Node,IndustrialRotorRenderer.State> {
    private final ModelPart[] models=new ModelPart[4];
    private final java.util.Map<IndustrialHeat.Node,double[]> motion=new java.util.WeakHashMap<>();
    public IndustrialRotorRenderer(BlockEntityRendererProvider.Context context){for(int i=0;i<4;i++)models[i]=layer(5+i*2).bakeRoot();}
    public static void register(){BlockEntityRenderers.register(IndustrialHeat.TYPE,IndustrialRotorRenderer::new);}
    public static final class State extends BlockEntityRenderState {int tier;Direction facing;float angle;}
    @Override public State createRenderState(){return new State();}
    @Override public boolean shouldRenderOffScreen(){return true;}
    @Override public int getViewDistance(){return 96;}
    @Override public void extractRenderState(IndustrialHeat.Node node,State s,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){
        BlockEntityRenderState.extractBase(node,s,overlay);s.tier=node.isRotor()?node.visibleRotor:-1;s.facing=node.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        boolean active=node.getBlockState().getValue(BlockStateProperties.LIT);
        double now=node.getLevel()==null?0:node.getLevel().getGameTime()+partial;
        double[] phase=motion.computeIfAbsent(node,n->new double[]{now,0});
        if(active&&!ConcordanceClientOptions.reducedMotion())phase[1]=(phase[1]+Math.clamp(now-phase[0],0,2)*3)%360;
        phase[0]=now;s.angle=(float)phase[1];
    }
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector collector,net.minecraft.client.renderer.state.level.CameraRenderState camera){
        if(s.tier<0||s.tier>=models.length)return;
        pose.pushPose();pose.translate(.5,.5,.5);
        int direction=s.facing==Direction.WEST?0:s.facing==Direction.NORTH?1:s.facing==Direction.EAST?2:3;
        pose.rotateDegrees(Axis.YP,-90*direction);pose.rotateDegrees(Axis.XP,s.angle);pose.translate(-.2,0,0);
        collector.order(0).submitModelPart(models[s.tier],pose,RenderTypes.entityCutout(Jugcraft.id("textures/block/jimtafacts/heat/rotor_"+IndustrialHeat.ROTOR_NAMES.get(s.tier)+".png")),s.lightCoords,OverlayTexture.NO_OVERLAY,null);
        pose.popPose();
    }
    private static LayerDefinition layer(int diameter){
        var mesh=new MeshDefinition();var root=mesh.getRoot();float[][] angles={{0,-.5F,0},{3.1F,.5F,0},{4.7F,0,.5F},{1.5F,0,-.5F}};
        for(int i=0;i<4;i++)root.addOrReplaceChild("blade_"+i,CubeListBuilder.create().texOffs(0,0).mirror().addBox(0,0,-4,1,diameter*8,8),PartPose.offsetAndRotation(-8,0,0,angles[i][0],angles[i][1],angles[i][2]));
        // Short native hub joins the recovered blades to the casing rather than leaving the source's display gap.
        root.addOrReplaceChild("hub",CubeListBuilder.create().texOffs(0,0).addBox(-8,-2,-2,4,4,4),PartPose.ZERO);
        return LayerDefinition.create(mesh,32,256);
    }
}
