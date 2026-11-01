package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Supplied skull UVs, enderman inner jaw and emissive eye layers. */
final class OwnerMobHeadModel {
    private final ModelPart ender,spider;
    OwnerMobHeadModel(){
        var mesh=new MeshDefinition();mesh.getRoot().addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-4,-8,-4,8,8,8),PartPose.ZERO);spider=LayerDefinition.create(mesh,64,32).bakeRoot();
        var full=new MeshDefinition();full.getRoot().addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-4,-8,-4,8,8,8),PartPose.ZERO);full.getRoot().addOrReplaceChild("hat",CubeListBuilder.create().texOffs(0,16).addBox(-4,-8,-4,8,8,8,new CubeDeformation(-.5F)),PartPose.ZERO);ender=LayerDefinition.create(full,64,32).bakeRoot();
    }
    void draw(OwnerSupplementRenderer.State s,PoseStack pose,SubmitNodeCollector out){
        boolean isEnder=s.name.startsWith("enderman");var model=isEnder?ender:spider;model.getAllParts().forEach(ModelPart::resetPose);if(isEnder)model.getChild("head").y=s.headMouth*-6;
        String name=isEnder?"enderman":"spider";var texture=Jugcraft.id("textures/newest/supps/entity/"+name+"_head.png");
        pose.pushPose();pose.translate(.5,s.name.contains("wall")?.25:0,.5);if(s.name.contains("wall"))pose.translate(-s.direction.getStepX()*.25,0,-s.direction.getStepZ()*.25);pose.rotateDegrees(Axis.YP,s.headRotation);pose.scale(-1,-1,1);
        out.submitModelPart(model,pose,RenderTypes.entityCutout(texture),s.lightCoords,OverlayTexture.NO_OVERLAY,null);
        out.submitModelPart(model,pose,RenderTypes.eyes(Jugcraft.id("textures/newest/supps/entity/"+name+"_head_eyes.png")),15728880,OverlayTexture.NO_OVERLAY,null);pose.popPose();
    }
}
