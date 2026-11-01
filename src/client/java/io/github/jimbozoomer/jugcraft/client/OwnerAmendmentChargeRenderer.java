package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.menagerie.OwnerAmendmentCharge;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.*;

/** Supplied charge item art tumbles in flight rather than constantly facing the camera. */
final class OwnerAmendmentChargeRenderer extends EntityRenderer<OwnerAmendmentCharge,OwnerAmendmentChargeRenderer.State> {
    private final ItemModelResolver resolver;
    OwnerAmendmentChargeRenderer(EntityRendererProvider.Context context){super(context);resolver=context.getItemModelResolver();}
    static final class State extends EntityRenderState {final ItemStackRenderState item=new ItemStackRenderState();float spin,yaw;}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(OwnerAmendmentCharge entity,State state,float partial){super.extractRenderState(entity,state,partial);state.spin=(entity.tickCount+partial)*14;state.yaw=entity.getYRot();resolver.updateForNonLiving(state.item,entity.cloud()?ItemStack.EMPTY:entity.getItem(),ItemDisplayContext.GROUND,entity);}
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        super.submit(state,pose,collector,camera);if(state.item.isEmpty())return;
        pose.pushPose();pose.rotateDegrees(Axis.YP,state.yaw);pose.rotateDegrees(Axis.XP,state.spin);pose.rotateDegrees(Axis.ZP,state.spin*.63F);pose.scale(.75F,.75F,.75F);state.item.submit(pose,collector,net.minecraft.util.LightCoordsUtil.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,0);pose.popPose();
    }
}
