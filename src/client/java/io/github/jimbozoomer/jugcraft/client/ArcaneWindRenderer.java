package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.arcane.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.world.phys.Vec3;
/** Original animated whirlwind block mesh, rendered translucently with its atlas animation. */
final class ArcaneWindRenderer implements BlockEntityRenderer<ArcaneWind,ArcaneWindRenderer.State>{
    ArcaneWindRenderer(BlockEntityRendererProvider.Context context){}
    static void register(){BlockEntityRenderers.register(ForbiddenArcane.WIND,ArcaneWindRenderer::new);}
    static final class State extends BlockEntityRenderState{final List<BlockStateModelPart> parts=new ArrayList<>();}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(ArcaneWind entity,State state,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){
        BlockEntityRenderState.extractBase(entity,state,overlay);state.parts.clear();Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(entity.getBlockState()).collectParts(net.minecraft.util.RandomSource.create(0),state.parts);
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){out.submitBlockModel(pose,RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS),state.parts,new int[0],state.lightCoords,OverlayTexture.NO_OVERLAY,0);}
}
