package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.menagerie.OwnerSignDisplay;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.HangingSignRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.Vec3;

/** Reuses the native hanging-sign renderer and supplied item placement proportions. */
final class OwnerSignDisplayRenderer extends HangingSignRenderer {
    private final ItemModelResolver resolver;
    OwnerSignDisplayRenderer(BlockEntityRendererProvider.Context context){super(context);resolver=context.itemModelResolver();}
    static void register(){BlockEntityRenderers.register(net.minecraft.world.level.block.entity.BlockEntityTypes.HANGING_SIGN,OwnerSignDisplayRenderer::new);}
    static final class State extends HangingSignRenderState {final ItemStackRenderState front=new ItemStackRenderState(),back=new ItemStackRenderState();float angle,sway;boolean attached;
        final java.util.List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> moving=new java.util.ArrayList<>(),fixed=new java.util.ArrayList<>();}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(SignBlockEntity sign,HangingSignRenderState raw,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){
        super.extractRenderState(sign,raw,partial,camera,overlay);var state=(State)raw;var block=sign.getBlockState();
        state.angle=block.hasProperty(CeilingHangingSignBlock.ROTATION)?-block.getValue(CeilingHangingSignBlock.ROTATION)*22.5F:-block.getValue(WallHangingSignBlock.FACING).toYRot();
        state.attached=block.hasProperty(CeilingHangingSignBlock.ATTACHED)&&block.getValue(CeilingHangingSignBlock.ATTACHED);
        double yaw=Math.toRadians(state.angle);state.sway=OwnerHangingMotion.angle(sign,partial,new Vec3(Math.sin(yaw),0,Math.cos(yaw)),false);
        state.moving.clear();state.fixed.clear();var parts=new java.util.ArrayList<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart>();
        net.minecraft.client.Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(block).collectParts(net.minecraft.util.RandomSource.create(sign.getBlockPos().asLong()),parts);
        for(var part:parts){if(block.getBlock() instanceof WallHangingSignBlock){state.moving.add(split(part,false));state.fixed.add(split(part,true));}else state.moving.add(part);}
        ItemStack front=ItemStack.EMPTY,back=ItemStack.EMPTY;
        if(sign instanceof OwnerSignDisplay display){front=display.jugcraft$display(SignTextSlot.FRONT);back=display.jugcraft$display(SignTextSlot.BACK);}
        resolver.updateForTopItem(state.front,front,ItemDisplayContext.FIXED,sign.getLevel(),null,0);resolver.updateForTopItem(state.back,back,ItemDisplayContext.FIXED,sign.getLevel(),null,1);
        if(!front.isEmpty())state.frontText=SignText.EMPTY;if(!back.isEmpty())state.backText=SignText.EMPTY;
    }
    @Override public void submit(HangingSignRenderState raw,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        var state=(State)raw;
        var layer=net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);
        collector.submitBlockModel(pose,layer,state.fixed,new int[0],state.lightCoords,OverlayTexture.NO_OVERLAY,0);
        pose.pushPose();pose.translate(.5,state.attached?1:.875,.5);pose.rotateDegrees(Axis.YP,state.angle);pose.rotateDegrees(state.attached?Axis.YP:Axis.XP,state.sway);pose.rotateDegrees(Axis.YP,-state.angle);pose.translate(-.5,state.attached?-1:-.875,-.5);
        collector.submitBlockModel(pose,layer,state.moving,new int[0],state.lightCoords,OverlayTexture.NO_OVERLAY,0);
        super.submit(raw,pose,collector,camera);
        for(int side=0;side<2;side++){
            var item=side==0?state.front:state.back;if(item.isEmpty())continue;
            pose.pushPose();pose.translate(.5,.3125,.5);pose.rotateDegrees(Axis.YP,state.angle+side*180);pose.translate(0,0,.08);pose.scale(.5F,.5F,.5F);item.submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();
        }
        pose.popPose();
    }
    private static net.minecraft.client.renderer.block.dispatch.BlockStateModelPart split(net.minecraft.client.renderer.block.dispatch.BlockStateModelPart original,boolean fixed){
        return new net.minecraft.client.renderer.block.dispatch.BlockStateModelPart(){
            public java.util.List<net.minecraft.client.resources.model.geometry.BakedQuad> getQuads(net.minecraft.core.Direction face){return original.getQuads(face).stream().filter(q->{boolean bracket=true;for(int i=0;i<4;i++)if(q.position(i).y()<.8749F){bracket=false;break;}return bracket==fixed;}).toList();}
            public boolean useAmbientOcclusion(){return original.useAmbientOcclusion();}
            public net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial(){return original.particleMaterial();}
            public int materialFlags(){return original.materialFlags();}
        };
    }
}
