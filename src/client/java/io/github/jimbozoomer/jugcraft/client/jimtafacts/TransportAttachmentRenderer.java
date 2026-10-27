package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.TransportAttachments;
import io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

/** Original blocker mesh, plus the shared facade's actual sampled block appearance. */
public final class TransportAttachmentRenderer<T extends net.minecraft.world.level.block.entity.BlockEntity & TransportAttachments.Host> implements BlockEntityRenderer<T,TransportAttachmentRenderer.State> {
    public TransportAttachmentRenderer(BlockEntityRendererProvider.Context c){}
    public static void register(){BlockEntityRenderers.register(TransportAttachments.TYPE,TransportAttachmentRenderer::new);
        BlockEntityRenderers.register(io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialTransportDevices.ENTITY,TransportAttachmentRenderer::new);
        BlockEntityRenderers.register(io.github.jimbozoomer.jugcraft.logistics.JugcraftLogistics.SORTER_ENTITY,TransportAttachmentRenderer::new);
        BlockEntityRenderers.register(io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids.FILTER_ENTITY,TransportAttachmentRenderer::new);}
    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState[] plugs=new ItemStackRenderState[6],covers=new ItemStackRenderState[6];
        final ItemStackRenderState paint=new ItemStackRenderState();
        float paintScale;
        final ItemStackRenderState[] wires=new ItemStackRenderState[16];
        final int[] wireArms=new int[16];
        final boolean[] hollow=new boolean[6];
        State(){for(int i=0;i<16;i++)wires[i]=new ItemStackRenderState();for(int i=0;i<6;i++){plugs[i]=new ItemStackRenderState();covers[i]=new ItemStackRenderState();}}
    }
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(T n,State s,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay crumble){
        BlockEntityRenderState.extractBase(n,s,crumble);var fitting=n.attachments();var mc=Minecraft.getInstance();var player=mc.player;
        s.paint.clear();s.paintScale=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(n.getBlockState().getBlock()).getPath().endsWith("_item")?1F:2F/3F;
        if(fitting.paint>=0){
            var marker=new ItemStack(io.github.jimbozoomer.jugcraft.jimtafacts.industry.FactoryPaintbrush.MARKER);
            marker.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,new net.minecraft.world.item.component.CustomModelData(java.util.List.of(),java.util.List.of(),java.util.List.of(),java.util.List.of(DyeColor.byId(fitting.paint).getTextureDiffuseColor())));
            mc.getItemModelResolver().updateForTopItem(s.paint,marker,ItemDisplayContext.NONE,n.getLevel(),null,0);
        }
        for(var colour:DyeColor.values()){
            int i=colour.ordinal();s.wires[i].clear();s.wireArms[i]=0;
            if((fitting.wires&(1<<i))==0)continue;
            var wire=new ItemStack(io.github.jimbozoomer.jugcraft.jimtafacts.industry.FactoryWires.ITEMS.get(colour));
            wire.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,new net.minecraft.world.item.component.CustomModelData(java.util.List.of(1F),java.util.List.of(),java.util.List.of(),java.util.List.of()));
            mc.getItemModelResolver().updateForTopItem(s.wires[i],wire,ItemDisplayContext.NONE,n.getLevel(),null,0);
            for(var face:Direction.values())if(io.github.jimbozoomer.jugcraft.jimtafacts.industry.FactoryWires.connects(n.getLevel(),n.getBlockPos(),face,i))s.wireArms[i]|=1<<face.ordinal();
        }
        boolean hidden=player!=null&&(player.getMainHandItem().is(DigitalStorage.ITEMS.get("network_tool"))||player.getOffhandItem().is(DigitalStorage.ITEMS.get("network_tool")));
        for(Direction side:Direction.values()){
            int i=side.ordinal();s.plugs[i].clear();s.covers[i].clear();
            s.hollow[i]=fitting.facadeModes[i]==io.github.jimbozoomer.jugcraft.jimtafacts.industry.TransportFacades.OPEN;
            if(fitting.powerFace==i)mc.getItemModelResolver().updateForTopItem(s.plugs[i],new ItemStack(TransportAttachments.POWER_ADAPTOR),ItemDisplayContext.NONE,n.getLevel(),null,0);
            else if(fitting.plugged(side))mc.getItemModelResolver().updateForTopItem(s.plugs[i],new ItemStack(TransportAttachments.BLOCKER),ItemDisplayContext.NONE,n.getLevel(),null,0);
            else if(fitting.lenses[i]!=0)mc.getItemModelResolver().updateForTopItem(s.plugs[i],new ItemStack(TransportAttachments.LENSES.get(fitting.lenses[i]-1)),ItemDisplayContext.NONE,n.getLevel(),null,0);
            var appearance=io.github.jimbozoomer.jugcraft.jimtafacts.industry.TransportFacades.appearance(fitting,i);
            if(!hidden&&!appearance.isAir())mc.getItemModelResolver().updateForTopItem(s.covers[i],new ItemStack(appearance.getBlock()),ItemDisplayContext.NONE,n.getLevel(),null,0);
        }
    }
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){
        for(int i=0;i<16;i++){
            // An installed but disconnected wire is a short seated segment, never a floating arm.
            if(s.wireArms[i]==0){pose.pushPose();pose.translate(.5,.5,.5);pose.scale(1,1,.2F);s.wires[i].submit(pose,out,s.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();}
            else for(var face:Direction.values())if((s.wireArms[i]&(1<<face.ordinal()))!=0){
                pose.pushPose();pose.translate(.5,.5,.5);
                switch(face){case SOUTH->pose.rotateDegrees(Axis.YP,180);case EAST->pose.rotateDegrees(Axis.YP,-90);case WEST->pose.rotateDegrees(Axis.YP,90);case UP->pose.rotateDegrees(Axis.XP,90);case DOWN->pose.rotateDegrees(Axis.XP,-90);default->{}}
                s.wires[i].submit(pose,out,s.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();
            }
        }
        pose.pushPose();pose.translate(.5,.5,.5);pose.scale(s.paintScale,s.paintScale,s.paintScale);
        s.paint.submit(pose,out,s.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();
        for(Direction side:Direction.values()){
            pose.pushPose();pose.translate(.5,.5,.5);
            // Supplied mesh faces west. Rotate around the pipe centre without changing its UVs or dimensions.
            switch(side){case NORTH->pose.rotateDegrees(Axis.YP,-90);case EAST->pose.rotateDegrees(Axis.YP,180);case SOUTH->pose.rotateDegrees(Axis.YP,90);case UP->pose.rotateDegrees(Axis.ZP,-90);case DOWN->pose.rotateDegrees(Axis.ZP,90);default->{}}
            s.plugs[side.ordinal()].submit(pose,out,s.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();
            pose.pushPose();pose.translate(.5,.5,.5);
            switch(side){case SOUTH->pose.rotateDegrees(Axis.YP,180);case EAST->pose.rotateDegrees(Axis.YP,-90);case WEST->pose.rotateDegrees(Axis.YP,90);case UP->pose.rotateDegrees(Axis.XP,90);case DOWN->pose.rotateDegrees(Axis.XP,-90);default->{}}
            pose.translate(0,0,-.4375);
            if(s.hollow[side.ordinal()]){
                // Four seated rails leave an eight-pixel opening, matching the server collision shape.
                rail(s.covers[side.ordinal()],pose,out,s.lightCoords,0,-.375F,1,.25F);
                rail(s.covers[side.ordinal()],pose,out,s.lightCoords,0,.375F,1,.25F);
                rail(s.covers[side.ordinal()],pose,out,s.lightCoords,-.375F,0,.25F,.5F);
                rail(s.covers[side.ordinal()],pose,out,s.lightCoords,.375F,0,.25F,.5F);
            }else {pose.scale(1,1,.125F);s.covers[side.ordinal()].submit(pose,out,s.lightCoords,OverlayTexture.NO_OVERLAY,0);}
            pose.popPose();
        }
    }
    private static void rail(ItemStackRenderState model,PoseStack pose,SubmitNodeCollector out,int light,float x,float y,float width,float height){
        pose.pushPose();pose.translate(x,y,0);pose.scale(width,height,.125F);model.submit(pose,out,light,OverlayTexture.NO_OVERLAY,0);pose.popPose();
    }
}
