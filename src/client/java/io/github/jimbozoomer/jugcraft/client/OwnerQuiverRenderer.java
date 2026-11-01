package io.github.jimbozoomer.jugcraft.client;
import com.mojang.math.Axis;
import eu.pb4.trinkets.api.client.TrinketRendererRegistry;
import io.github.jimbozoomer.jugcraft.menagerie.*;
import io.github.jimbozoomer.jugcraft.concordance.trinket.WornDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemDisplayContext;
/** Supplied held mesh, source back/hip/thigh placement and native dyed-color tint. */
final class OwnerQuiverRenderer {
 static void register(){TrinketRendererRegistry.registerRenderer(OwnerSupplementaries.ITEMS.get("quiver"),context->(stack,access,source,pose,out,light,state,yaw,pitch)->{
  var wearer=access.inventory().getAttachment().getEntity();int mode=OwnerQuiver.displayMode(stack);if(mode==3||state.isInvisible||wearer.isSpectator()||WornDisplay.hidden(wearer)||!(source instanceof HumanoidModel<?> body)||wearer.getMainHandItem()==stack||wearer.getOffhandItem()==stack)return;
  pose.pushPose();body.body.translateAndRotate(pose);boolean flip=wearer.getMainArm()==HumanoidArm.RIGHT;double armor=wearer.getItemBySlot(mode==2?EquipmentSlot.LEGS:EquipmentSlot.CHEST).isEmpty()?0:.0625;
  if(mode==2){if(wearer.isCrouching())pose.translate(0,-.125,-.275);var leg=flip?body.leftLeg:body.rightLeg;float old=leg.xRot;try{leg.xRot*=.3F;leg.translateAndRotate(pose);}finally{leg.xRot=old;}pose.translate((flip?1:-1)*(.1885+armor),-.0625,-.15625);}
  else{pose.translate(0,.1,.1885+armor);pose.rotateDegrees(Axis.YP,90);if(flip)pose.scale(-1,1,-1);pose.translate(0,mode==1?.4:0,mode==1?-.1875:-.125);if(mode==1)pose.rotateDegrees(Axis.XP,22.5F);}
  var item=new ItemStackRenderState();Minecraft.getInstance().getItemModelResolver().updateForLiving(item,stack,ItemDisplayContext.HEAD,wearer);item.submit(pose,out,light,OverlayTexture.NO_OVERLAY,state.outlineColor);pose.popPose();
 });}
}
