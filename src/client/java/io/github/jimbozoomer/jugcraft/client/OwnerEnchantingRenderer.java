package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.enchanting.OwnerEnchanting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.EnchantTableRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.phys.Vec3;

/** Preserves vanilla's animated book and shows the actual saved enchanting item. */
final class OwnerEnchantingRenderer extends EnchantTableRenderer {
    OwnerEnchantingRenderer(BlockEntityRendererProvider.Context context){super(context);}
    private static final class State extends EnchantTableRenderState { final ItemStackRenderState item=new ItemStackRenderState(); }
    @Override public EnchantTableRenderState createRenderState(){return new State();}
    @Override public void extractRenderState(EnchantingTableBlockEntity be,EnchantTableRenderState base,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){
        super.extractRenderState(be,base,partial,camera,overlay);var state=(State)base;state.item.clear();
        if(be instanceof OwnerEnchanting.Storage storage)Minecraft.getInstance().getItemModelResolver().updateForTopItem(state.item,storage.jugcraft$enchantingInventory().getItem(0),ItemDisplayContext.GROUND,be.getLevel(),null,0);
    }
    @Override public void submit(EnchantTableRenderState base,PoseStack pose,SubmitNodeCollector out,CameraRenderState camera){
        super.submit(base,pose,out,camera);var state=(State)base;pose.pushPose();pose.translate(.5,1.6+Math.sin(base.time*.05)*.04,.5);pose.rotateDegrees(Axis.YP,base.time*2);pose.scale(.7F,.7F,.7F);state.item.submit(pose,out,base.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();
    }
}
