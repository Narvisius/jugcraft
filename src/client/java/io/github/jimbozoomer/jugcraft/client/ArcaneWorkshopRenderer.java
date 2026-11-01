package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.arcane.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

final class ArcaneWorkshopRenderer implements BlockEntityRenderer<ArcaneWorkshop,ArcaneWorkshopRenderer.State> {
    private final ItemModelResolver resolver;
    ArcaneWorkshopRenderer(BlockEntityRendererProvider.Context c){resolver=c.itemModelResolver();}
    static void register(){BlockEntityRenderers.register(ForbiddenArcane.WORKSHOP,ArcaneWorkshopRenderer::new);}
    static final class State extends BlockEntityRenderState {final ItemStackRenderState item=new ItemStackRenderState();float angle,height;}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(ArcaneWorkshop be,State s,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){BlockEntityRenderState.extractBase(be,s,overlay);resolver.updateForTopItem(s.item,be.displayed(),ItemDisplayContext.GROUND,be.getLevel(),null,0);s.angle=((be.getLevel()==null?0:be.getLevel().getGameTime())%3600+partial)*2;s.height=be.forge()?1.25F:1.05F;if(be.running())s.height+=.25F;}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){pose.pushPose();pose.translate(.5,s.height,.5);pose.rotate(Axis.YP.rotationDegrees(s.angle));s.item.submit(pose,collector,s.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();}
}
