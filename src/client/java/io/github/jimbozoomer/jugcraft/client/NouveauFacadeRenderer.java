package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauWeaves;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class NouveauFacadeRenderer implements BlockEntityRenderer<NouveauWeaves.Weave,NouveauFacadeRenderer.State> {
    public static final class State extends BlockEntityRenderState {final ItemStackRenderState facade=new ItemStackRenderState();boolean visible;NouveauSkyView.View sky;}
    public NouveauFacadeRenderer(BlockEntityRendererProvider.Context context){}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(NouveauWeaves.Weave be,State s,float partial,Vec3 camera,ModelFeatureRenderer.@Nullable CrumblingOverlay overlay){
        BlockEntityRenderState.extractBase(be,s,overlay);var viewer=Minecraft.getInstance().player;boolean sight=viewer!=null&&viewer.hasEffect(io.github.jimbozoomer.jugcraft.nouveau.NouveauStatuses.EFFECTS.get("magic_find"));s.visible=(!be.kind().equals("sky_block")||be.showFacade())&&(!be.kind().equals("ghostweave")||!be.hidden()||sight)&&(!be.kind().equals("falseweave")||!sight);
        s.sky=be.kind().equals("sky_block")&&!be.showFacade()?NouveauSkyView.capture(camera.subtract(Vec3.atLowerCornerOf(be.getBlockPos()))):null;
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(s.facade,new ItemStack(be.facade().getBlock()),ItemDisplayContext.NONE,be.getLevel(),null,0);
    }
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){if(s.sky!=null){NouveauSkyView.draw(s.sky,pose,collector);return;}if(!s.visible)return;pose.pushPose();pose.translate(.5,.5,.5);s.facade.submit(pose,collector,s.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();}
}
