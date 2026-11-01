package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.menagerie.*;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.monster.creeper.CreeperModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.*;

/** Supplied party hat follows the creeper head, including charging animation. */
public final class OwnerPartyHatLayer extends RenderLayer<CreeperRenderState,CreeperModel> {
    private static final RenderStateDataKey<ItemStackRenderState> HAT=RenderStateDataKey.create(()->"jugcraft:party_creeper_hat");
    private OwnerPartyHatLayer(RenderLayerParent<CreeperRenderState,CreeperModel> parent){super(parent);}
    static void register(){LivingEntityRenderLayerRegistrationCallback.EVENT.register((type,renderer,helper,context)->{
        if(renderer instanceof CreeperRenderer creeper)helper.register(new OwnerPartyHatLayer(creeper));
    });}
    public static void extract(Creeper creeper,CreeperRenderState state){
        if(!(creeper instanceof OwnerConfetti.PartyCreeper party)||!party.jugcraft$isParty()){state.setData(HAT,null);return;}
        var item=state.getData(HAT);if(item==null)item=new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(item,new ItemStack(OwnerSupplementaries.ITEMS.get("confetti_popper")),ItemDisplayContext.HEAD,creeper.level(),creeper,creeper.getId());state.setData(HAT,item);
    }
    @Override public void submit(PoseStack pose,SubmitNodeCollector out,int light,CreeperRenderState state,float yRot,float xRot){
        var hat=state.getData(HAT);if(hat==null||state.isInvisible)return;
        pose.pushPose();getParentModel().root().getChild("head").translateAndRotate(pose);
        pose.translate(0,-.25,0);pose.rotateDegrees(Axis.YP,180);pose.scale(.625F,-.625F,-.625F);
        hat.submit(pose,out,light,OverlayTexture.NO_OVERLAY,0);pose.popPose();
    }
}
