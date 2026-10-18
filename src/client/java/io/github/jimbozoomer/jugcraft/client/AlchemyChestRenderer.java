package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.assay.*;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Owner-supplied 64px chest atlases on Minecraft's native hinged chest model. */
public final class AlchemyChestRenderer<T extends BlockEntity> implements BlockEntityRenderer<T,AlchemyChestRenderer.State> {
    private final ChestModel model=new ChestModel(ChestModel.createSingleBodyLayer().bakeRoot());
    public static final class State extends BlockEntityRenderState {float openness,yaw;boolean visible;Identifier texture;}
    public AlchemyChestRenderer(BlockEntityRendererProvider.Context context){}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(T be,State s,float partial,Vec3 camera,ModelFeatureRenderer.@Nullable CrumblingOverlay overlay){
        BlockEntityRenderState.extractBase(be,s,overlay);s.visible=true;
        AlchemyLid lid;
        if(be instanceof AlchemyStorageBlockEntity chest){lid=chest.lid;s.texture=Jugcraft.id("textures/block/alchemy/alchemical_chest.png");}
        else if(be instanceof AlchemicalWorkshopBlockEntity w&&!w.collector()){lid=w.lid;s.texture=Jugcraft.id("textures/block/alchemy/condenser_mk"+w.tier()+".png");}
        else {s.visible=false;return;}
        s.openness=ConcordanceClientOptions.reducedMotion()?(be.getBlockState().getValue(BlockStateProperties.OPEN)?1:0):lid.openness(partial);
        s.yaw=be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot();
    }
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        if(!s.visible)return;
        pose.pushPose();pose.translate(.5,0,.5);pose.rotateDegrees(Axis.YP,-s.yaw);pose.translate(-.5,0,-.5);
        float open=1-(float)Math.pow(1-s.openness,3);
        collector.submitModel(model,open,pose,s.texture,s.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();
    }
}
