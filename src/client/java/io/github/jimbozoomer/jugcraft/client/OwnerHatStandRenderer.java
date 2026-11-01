package io.github.jimbozoomer.jugcraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.menagerie.OwnerHatStand;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.model.object.armorstand.ArmorStandArmorModel;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.*;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
/** Supplied hat-stand UV layout, stem/base geometry, real helmet and skull layers. */
final class OwnerHatStandRenderer extends LivingEntityRenderer<OwnerHatStand,ArmorStandRenderState,ArmorStandArmorModel> {
 private final ArmorStandRenderer extractor;
 static final class State extends ArmorStandRenderState {int dance;float animationTime;}
 OwnerHatStandRenderer(EntityRendererProvider.Context c){super(c,new Model(),0);extractor=new ArmorStandRenderer(c);addLayer(new HumanoidArmorLayer<>(this,ArmorModelSet.bake(ModelLayers.ARMOR_STAND_ARMOR,c.getModelSet(),ArmorStandArmorModel::new),c.getEquipmentRenderer()));addLayer(new CustomHeadLayer<>(this,c.getModelSet(),c.getPlayerSkinRenderCache()));}
 @Override public ArmorStandRenderState createRenderState(){return new State();}
 @Override public void extractRenderState(OwnerHatStand e,ArmorStandRenderState s,float partial){extractor.extractRenderState(e,s,partial);s.isSmall=false;if(s instanceof State state){state.dance=e.dance();state.animationTime=e.animationTime(partial);}}
 @Override public net.minecraft.resources.Identifier getTextureLocation(ArmorStandRenderState s){return Jugcraft.id("textures/newest/supps/entity/hat_stand.png");}
 @Override protected void setupRotations(ArmorStandRenderState s,PoseStack pose,float bodyRot,float scale){pose.rotateDegrees(Axis.YP,180-s.yRot);if(s.wiggle>0)pose.rotateDegrees(Axis.YP,(float)Math.sin(s.wiggle/1.5*Math.PI)*3);}
 static final class Model extends ArmorStandArmorModel {
  private final ModelPart base,neck,joint,dummy;
  private final net.minecraft.client.animation.KeyframeAnimation dance;
  Model(){super(mesh());base=root().getChild("base_plate");joint=root().getChild("neck_joint");neck=joint.getChild("neck");dummy=joint.getChild("dummy_head");dance=OwnerHatStandAnimations.DEFAULT.bake(root());}
  static ModelPart mesh(){var mesh=new MeshDefinition();var r=mesh.getRoot();for(String n:new String[]{"body","right_arm","left_arm","right_leg","left_leg"})r.addOrReplaceChild(n,CubeListBuilder.create(),PartPose.ZERO);
   r.addOrReplaceChild("base_plate",CubeListBuilder.create().texOffs(0,16).addBox(-6,3,-6,12,1,12),PartPose.offset(0,20,0));r.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-4,-8,-4,8,8,8),PartPose.offset(0,20,0)).addOrReplaceChild("hat",CubeListBuilder.create(),PartPose.ZERO);var joint=r.addOrReplaceChild("neck_joint",CubeListBuilder.create(),PartPose.offset(0,23,0));joint.addOrReplaceChild("neck",CubeListBuilder.create().texOffs(0,0).addBox(-1,-3,-1,2,3,2),PartPose.ZERO);joint.addOrReplaceChild("dummy_head",CubeListBuilder.create(),PartPose.offset(0,-3,0));return LayerDefinition.create(mesh,64,32).bakeRoot();}
  @Override public void setupAnim(ArmorStandRenderState s){super.setupAnim(s);joint.resetPose();neck.resetPose();dummy.resetPose();joint.xRot=head.xRot;joint.zRot=head.zRot;
   if(s instanceof State state&&state.dance!=0){dance.apply((long)(state.animationTime*50*1.24F),1);if(state.dance==1)dummy.y+=9;}
   var pivot=new org.joml.Vector3f(dummy.x,dummy.y,dummy.z).rotate(new org.joml.Quaternionf().rotationZYX(joint.zRot,joint.yRot,joint.xRot)).add(joint.x,joint.y,joint.z);
   head.x=pivot.x;head.y=pivot.y;head.z=pivot.z;head.xRot=joint.xRot+dummy.xRot;head.yRot=joint.yRot+dummy.yRot;head.zRot=joint.zRot+dummy.zRot;
   neck.yScale=Math.max(.01F,-dummy.y/3);hat.resetPose();base.visible=s.showBasePlate;}
 }
}
