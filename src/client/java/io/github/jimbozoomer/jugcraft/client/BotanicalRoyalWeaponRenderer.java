package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.botany.BotanicalRoyalWeapon;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
final class BotanicalRoyalWeaponRenderer extends EntityRenderer<BotanicalRoyalWeapon,BotanicalRoyalWeaponRenderer.State> {
    private final ItemModelResolver models;
    static final class State extends EntityRenderState {final ItemStackRenderState item=new ItemStackRenderState();float yaw,pitch;boolean flying;}
    BotanicalRoyalWeaponRenderer(EntityRendererProvider.Context c){super(c);models=c.getItemModelResolver();}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(BotanicalRoyalWeapon entity,State s,float partial){super.extractRenderState(entity,s,partial);models.updateForTopItem(s.item,entity.getItem(),ItemDisplayContext.NONE,entity.level(),null,entity.getId());s.yaw=entity.getYRot(partial);s.pitch=entity.getXRot(partial);s.flying=entity.getDeltaMovement().lengthSqr()>.1;}
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        pose.pushPose();pose.rotateDegrees(Axis.YP,s.yaw);pose.rotateDegrees(Axis.XP,90-s.pitch);pose.rotateDegrees(Axis.ZP,45);pose.scale(1.5F,1.5F,1.5F);s.item.submit(pose,collector,net.minecraft.util.LightCoordsUtil.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,0);pose.popPose();
        if(!s.flying){pose.pushPose();pose.rotate(camera.orientation);pose.rotateDegrees(Axis.ZP,s.ageInTicks*2);float size=Math.min(1,s.ageInTicks/10);pose.scale(size,size,size);
            collector.submitCustomGeometry(pose,net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucentEmissive(Jugcraft.id("textures/newest/jugtania/misc/babylon.png")),(m,v)->{for(float[] q:new float[][]{{-1,-1,0,1},{1,-1,1,1},{1,1,1,0},{-1,1,0,0}})v.addVertex(m,q[0],q[1],.05F).setColor(-1).setUv(q[2],q[3]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(net.minecraft.util.LightCoordsUtil.FULL_BRIGHT).setNormal(m,0,0,1);});pose.popPose();
        }super.submit(s,pose,collector,camera);
    }
}
