package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.loot.OwnerLootEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;

final class OwnerLootEntityRenderers {
    static void register(){EntityRendererRegistry.register(OwnerLootEntities.CART,CartRenderer::new);EntityRendererRegistry.register(OwnerLootEntities.FRAME,FrameRenderer::new);}
    static final class CartState extends MinecartRenderState {final OwnerLootRenderer.State chest=new OwnerLootRenderer.State();}
    static final class CartRenderer extends AbstractMinecartRenderer<OwnerLootEntities.Cart,CartState> {
        private final OwnerLootRenderer chest;
        CartRenderer(EntityRendererProvider.Context c){super(c,ModelLayers.CHEST_MINECART);chest=new OwnerLootRenderer(Minecraft.getInstance().getEntityModels());}
        @Override public CartState createRenderState(){return new CartState();}
        @Override public void extractRenderState(OwnerLootEntities.Cart cart,CartState s,float partial){super.extractRenderState(cart,s,partial);var p=Minecraft.getInstance().player;s.chest.seen=p!=null&&cart.seen(p.getUUID());s.chest.facing=Direction.SOUTH;}
        @Override protected void submitMinecartContents(CartState s,BlockModelRenderState block,PoseStack pose,SubmitNodeCollector out,int light){chest.draw(s.chest,pose,out,light,OverlayTexture.NO_OVERLAY,s.outlineColor);}
    }
    static final class FrameState extends ItemFrameRenderState {final ItemStackRenderState frame=new ItemStackRenderState();}
    static final class FrameRenderer extends ItemFrameRenderer<OwnerLootEntities.Frame> {
        private final ItemModelResolver items;
        FrameRenderer(EntityRendererProvider.Context c){super(c);items=c.getItemModelResolver();}
        @Override public FrameState createRenderState(){return new FrameState();}
        @Override public void extractRenderState(OwnerLootEntities.Frame frame,ItemFrameRenderState raw,float partial){
            super.extractRenderState(frame,raw,partial);var s=(FrameState)raw;s.frameModel.clear();s.frame.clear();
            if(s.isInvisible)return;var p=Minecraft.getInstance().player;var stack=new ItemStack(OwnerLootEntities.FRAME_ITEM);
            if(p!=null&&frame.seen(p.getUUID()))stack.set(DataComponents.ITEM_MODEL,Jugcraft.id("personal_loot_item_frame_open"));
            items.updateForTopItem(s.frame,stack,ItemDisplayContext.NONE,frame.level(),null,frame.getId());
        }
        @Override public void submit(ItemFrameRenderState raw,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){
            // Keep native item/map rendering, rotation, lighting and labels; replace only the supplied frame surface.
            super.submit(raw,pose,out,camera);var s=(FrameState)raw;if(s.frame.isEmpty())return;
            pose.pushPose();var d=s.direction;var o=getRenderOffset(s);pose.translate(-o.x,-o.y,-o.z);pose.translate(d.getStepX()*.46875,d.getStepY()*.46875,d.getStepZ()*.46875);
            pose.rotateDegrees(Axis.XP,d.getAxis().isHorizontal()?0:-90*d.getAxisDirection().getStep());pose.rotateDegrees(Axis.YP,d.getAxis().isHorizontal()?180-d.toYRot():180);
            s.frame.submit(pose,out,s.lightCoords,OverlayTexture.NO_OVERLAY,s.outlineColor);pose.popPose();
        }
    }
}
