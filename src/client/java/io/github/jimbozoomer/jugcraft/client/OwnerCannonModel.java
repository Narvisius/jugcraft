package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.menagerie.OwnerCannon;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Exact supplied 64x64 cannon mesh and barrel deformation. */
final class OwnerCannonModel {
    private final ModelPart root,legs,pivot,head;
    OwnerCannonModel(){
        var mesh=new MeshDefinition();var r=mesh.getRoot();
        var legs=r.addOrReplaceChild("legs",CubeListBuilder.create().texOffs(0,0).addBox(6,-4,-3,2,10,6).texOffs(48,0).addBox(-8,-4,-3,2,10,6),PartPose.ZERO);
        var pivot=legs.addOrReplaceChild("head_pivot",CubeListBuilder.create(),PartPose.offset(0,-1,0));
        pivot.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,46).addBox(-6,-6,-6.5F,12,12,6).texOffs(0,18).addBox(-6,-6,-6.5F,12,12,13,new CubeDeformation(-.3125F)),PartPose.ZERO);
        r.addOrReplaceChild("base",CubeListBuilder.create().texOffs(0,0).addBox(-8,6,-8,16,2,16),PartPose.ZERO);
        root=LayerDefinition.create(mesh,64,64).bakeRoot();this.legs=root.getChild("legs");this.pivot=this.legs.getChild("head_pivot");head=this.pivot.getChild("head");
    }
    void draw(OwnerSupplementRenderer.State state,PoseStack pose,SubmitNodeCollector out){
        root.getAllParts().forEach(ModelPart::resetPose);var local=OwnerCannon.mounted(OwnerCannon.direction(state.cannonYaw,state.cannonPitch),state.cannonMount,true);legs.yRot=(float)Math.toRadians(OwnerCannon.yaw(local));pivot.xRot=(float)Math.toRadians(OwnerCannon.pitch(local));head.yRot=(float)Math.PI;
        float elapsed=OwnerCannon.COOLDOWN-state.cannonCooldown;float squish=state.cannonCooldown<=0?0:Math.max(0,1-elapsed/6)*.2F;float wobble=state.cannonFuse>0?(float)Math.sin(state.time*Math.PI*2/3)*.005F:0;
        head.xScale=head.yScale=1+squish*.7F+wobble;head.zScale=1-squish;head.z=1-squish*5.675F;
        var texture=Jugcraft.id("textures/newest/supps/block/cannon"+(state.cannonFuse>0?"_lit":state.cannonLoaded?"_gunpowder":"")+".png");
        pose.pushPose();pose.translate(.5,.5,.5);switch(state.cannonMount){case DOWN->pose.rotateDegrees(Axis.XP,180);case NORTH->pose.rotateDegrees(Axis.XP,-90);case SOUTH->pose.rotateDegrees(Axis.XP,90);case EAST->pose.rotateDegrees(Axis.ZP,-90);case WEST->pose.rotateDegrees(Axis.ZP,90);default->{}}pose.rotateDegrees(Axis.XP,180);out.submitModelPart(root,pose,RenderTypes.entityCutout(texture),state.lightCoords,OverlayTexture.NO_OVERLAY,null);pose.popPose();
    }
}
