// Original supplied mesh and animation, with native package/API adaptation.
package io.github.jimbozoomer.jugcraft.client.aquatic;

import io.github.jimbozoomer.jugcraft.client.aquatic.FishBaseModel;
import io.github.jimbozoomer.jugcraft.client.aquatic.AquaFishRenderState;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public class JellyfishModel
extends FishBaseModel {
    private final ModelPart head;
    private final ModelPart tentaclesMain;
    private final ModelPart tentaclesLeft;
    private final ModelPart tentaclesRight;
    private final ModelPart frill;
    private final ModelPart heart;

    public JellyfishModel(ModelPart part) {
        super(part);
        this.head = part.getChild("head");
        this.tentaclesMain = part.getChild("tentacles_main");
        this.tentaclesLeft = part.getChild("tentacles_left");
        this.tentaclesRight = part.getChild("tentacles_right");
        this.frill = part.getChild("frill");
        this.heart = part.getChild("heart");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition modelDefinition = new MeshDefinition();
        PartDefinition def = modelDefinition.getRoot();
        def.addOrReplaceChild("head", CubeListBuilder.create().texOffs(14, 0).addBox(-2.5f, -2.5f, -5.0f, 5.0f, 5.0f, 5.0f), PartPose.offset((float)0.0f, (float)21.0f, (float)-3.0f));
        def.addOrReplaceChild("tentacles_main", CubeListBuilder.create().texOffs(0, 0).addBox(0.0f, -3.0f, 0.0f, 0.0f, 6.0f, 14.0f), PartPose.offset((float)0.0f, (float)21.0f, (float)-3.5f));
        def.addOrReplaceChild("tentacles_left", CubeListBuilder.create().texOffs(0, 8).addBox(2.0f, -3.0f, 0.0f, 0.0f, 6.0f, 12.0f), PartPose.offset((float)0.0f, (float)21.0f, (float)-3.5f));
        def.addOrReplaceChild("tentacles_right", CubeListBuilder.create().texOffs(0, 14).addBox(-2.0f, -3.0f, 0.0f, 0.0f, 6.0f, 12.0f), PartPose.offset((float)0.0f, (float)21.0f, (float)-3.5f));
        def.addOrReplaceChild("frill", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0f, -3.0f, 0.0f, 6.0f, 6.0f, 1.0f), PartPose.offset((float)0.0f, (float)21.0f, (float)-5.0f));
        def.addOrReplaceChild("heart", CubeListBuilder.create().texOffs(34, 0).addBox(-1.5f, -1.5f, -2.0f, 3.0f, 3.0f, 3.0f), PartPose.offset((float)0.0f, (float)21.0f, (float)-3.0f));
        return LayerDefinition.create((MeshDefinition)modelDefinition, (int)64, (int)32);
    }

    public void setupAnim(AquaFishRenderState renderState) {
        float stillMovement = 0.1f;
        if (!renderState.isInWater) {
            stillMovement = 0.05f;
        }
        float walkAnimationPos = renderState.walkAnimationPos;
        float walkAnimationSpeed = renderState.walkAnimationSpeed;
        this.tentaclesLeft.yRot = -stillMovement * 0.25f * Mth.sin((double)(0.3f * renderState.ageInTicks)) + Mth.cos((double)(walkAnimationPos * 0.4662f)) * 0.5f * walkAnimationSpeed;
        this.tentaclesMain.yRot = -stillMovement * 0.25f * Mth.sin((double)(0.3f * renderState.ageInTicks)) + Mth.cos((double)(walkAnimationPos * 0.4662f)) * 0.5f * walkAnimationSpeed;
        this.tentaclesRight.yRot = -stillMovement * 0.25f * Mth.sin((double)(0.3f * renderState.ageInTicks)) + Mth.cos((double)(walkAnimationPos * 0.4662f)) * 0.5f * walkAnimationSpeed;
    }
}
