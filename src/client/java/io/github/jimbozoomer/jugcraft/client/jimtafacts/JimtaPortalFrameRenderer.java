package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaPortalFrames;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

/** Source frame surface: 0.40625 offset, 16x32 pixels, ten horizontal frames at two ticks each. */
public final class JimtaPortalFrameRenderer implements BlockEntityRenderer<JimtaPortalFrames.Node,JimtaPortalFrameRenderer.State> {
    public static final class State extends BlockEntityRenderState {boolean active;Direction facing;int colour;float time;}
    public JimtaPortalFrameRenderer(BlockEntityRendererProvider.Context context){}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(JimtaPortalFrames.Node node,State s,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay crumble){BlockEntityRenderState.extractBase(node,s,crumble);s.active=node.connected()&&node.getBlockState().getValue(JimtaPortalFrames.HALF)==DoubleBlockHalf.LOWER;s.facing=node.getBlockState().getValue(JimtaPortalFrames.FACING);s.colour=node.colour();s.time=node.getLevel()==null?0:node.getLevel().getGameTime()+partial;}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        if(!s.active)return;pose.pushPose();pose.translate(.5-s.facing.getStepX()*.40625,0,.5-s.facing.getStepZ()*.40625);if(s.facing.getAxis()==Direction.Axis.X)pose.rotateDegrees(Axis.YP,90);
        float u=((int)s.time/2%10)/10F;int colour=0x64000000|(s.colour==-1?0xffffff:s.colour&0xffffff);
        var texture=Jugcraft.id("textures/jimtafacts/spellbooks/entity/portal/portal_square"+(s.colour==-1?"":"_color")+".png");
        collector.submitCustomGeometry(pose,RenderTypes.entityTranslucentEmissive(texture),(matrix,buffer)->{
            float[][] points={{-.5F,0,u,0},{.5F,0,u+.1F,0},{.5F,2,u+.1F,1},{-.5F,2,u,1}};
            for(int side=0;side<2;side++)for(int v=0;v<4;v++){var p=points[side==0?v:3-v];buffer.addVertex(matrix,p[0],p[1],0).setColor(colour).setUv(p[2],p[3]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xf000f0).setNormal(matrix,0,0,side==0?1:-1);}
        });pose.popPose();
    }
}
