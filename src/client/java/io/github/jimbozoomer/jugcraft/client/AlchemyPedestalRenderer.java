package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.concordance.assay.AlchemyMachines;
import io.github.jimbozoomer.jugcraft.concordance.assay.AlchemyMachinesBlock;
import io.github.jimbozoomer.jugcraft.concordance.assay.AlchemyMachinesBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The supplied artifact lies directly on the six-pixel top plate, with no bobbing or unsupported floating parts. */
public final class AlchemyPedestalRenderer implements BlockEntityRenderer<AlchemyMachinesBlockEntity,AlchemyPedestalRenderer.State> {
    private static final float SUPPORT_Y=10F/16F, DISPLAY_WIDTH=.34F;
    private final ItemModelResolver itemModels;
    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState artifact=new ItemStackRenderState();
        float rotation,scale,centerX,centerY,minZ;
    }
    public AlchemyPedestalRenderer(BlockEntityRendererProvider.Context context){itemModels=context.itemModelResolver();}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(AlchemyMachinesBlockEntity machine,State state,float partialTick,Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling){
        BlockEntityRenderState.extractBase(machine,state,crumbling);state.artifact.clear();
        if(machine.kind()!=AlchemyMachines.Kind.PEDESTAL||machine.getLevel()==null)return;
        var display=machine.displayArtifact();if(display.isEmpty())return;
        itemModels.updateForTopItem(state.artifact,display,ItemDisplayContext.FIXED,machine.getLevel(),null,(int)machine.getBlockPos().asLong());
        var box=state.artifact.getModelBoundingBox();
        double span=Math.max(box.maxX-box.minX,box.maxY-box.minY);
        state.scale=(float)(DISPLAY_WIDTH/Math.max(.01,span));
        state.centerX=(float)((box.minX+box.maxX)*.5);state.centerY=(float)((box.minY+box.maxY)*.5);state.minZ=(float)box.minZ;
        state.rotation=machine.getBlockState().getValue(AlchemyMachinesBlock.FACING).toYRot();
        if(!ConcordanceClientOptions.reducedMotion()&&machine.getBlockState().getValue(AlchemyMachinesBlock.ACTIVE))
            state.rotation+=(machine.getLevel().getGameTime()%3600+partialTick)*.1F;
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        if(state.artifact.isEmpty())return;
        pose.pushPose();
        // Normalize to the actual baked sprite extents; its lowest face remains on the support even as it turns.
        pose.translate(.5F,SUPPORT_Y+.001F,.5F);pose.rotateDegrees(Axis.YP,state.rotation);pose.rotateDegrees(Axis.XP,-90);
        pose.scale(state.scale,state.scale,state.scale);pose.translate(-state.centerX,-state.centerY,-state.minZ);
        state.artifact.submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();
    }
}
