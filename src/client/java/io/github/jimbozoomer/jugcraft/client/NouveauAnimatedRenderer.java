package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauAnimatedBlock;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.*;

public final class NouveauAnimatedRenderer extends EntityRenderer<NouveauAnimatedBlock,NouveauAnimatedRenderer.State> {
    public static final class State extends EntityRenderState {final ItemStackRenderState block=new ItemStackRenderState();}
    private final ItemModelResolver resolver;
    public NouveauAnimatedRenderer(EntityRendererProvider.Context c){super(c);resolver=c.getItemModelResolver();shadowRadius=.5F;}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(NouveauAnimatedBlock e,State s,float partial){super.extractRenderState(e,s,partial);resolver.updateForTopItem(s.block,new ItemStack(e.block().getBlock()),ItemDisplayContext.NONE,e.level(),null,e.getId());}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){pose.pushPose();pose.translate(0,.5+Math.sin(s.ageInTicks*.1)*.04,0);s.block.submit(pose,collector,s.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();super.submit(s,pose,collector,camera);}
}
