// Original supplied mesh and animation, with native package/API adaptation.
package io.github.jimbozoomer.jugcraft.client.aquatic;

import io.github.jimbozoomer.jugcraft.client.aquatic.FishBaseModel;
import io.github.jimbozoomer.jugcraft.client.aquatic.AquaFishRenderState;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public class AquaTropicalFishBModel
extends FishBaseModel {
    private final ModelPart tail;

    public AquaTropicalFishBModel(ModelPart root) {
        super(root);
        this.tail = this.root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer(CubeDeformation cubeDeformation) {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 20).addBox(-1.0f, -3.0f, -3.0f, 2.0f, 6.0f, 6.0f, cubeDeformation), PartPose.offset((float)0.0f, (float)19.0f, (float)0.0f));
        partdefinition.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(21, 16).addBox(0.0f, -3.0f, 0.0f, 0.0f, 6.0f, 5.0f, cubeDeformation), PartPose.offset((float)0.0f, (float)19.0f, (float)3.0f));
        partdefinition.addOrReplaceChild("right_fin", CubeListBuilder.create().texOffs(2, 16).addBox(-2.0f, 0.0f, 0.0f, 2.0f, 2.0f, 0.0f, cubeDeformation), PartPose.offsetAndRotation((float)-1.0f, (float)20.0f, (float)0.0f, (float)0.0f, (float)0.7853982f, (float)0.0f));
        partdefinition.addOrReplaceChild("left_fin", CubeListBuilder.create().texOffs(2, 12).addBox(0.0f, 0.0f, 0.0f, 2.0f, 2.0f, 0.0f, cubeDeformation), PartPose.offsetAndRotation((float)1.0f, (float)20.0f, (float)0.0f, (float)0.0f, (float)-0.7853982f, (float)0.0f));
        partdefinition.addOrReplaceChild("top_fin", CubeListBuilder.create().texOffs(20, 11).addBox(0.0f, -4.0f, 0.0f, 0.0f, 4.0f, 6.0f, cubeDeformation), PartPose.offset((float)0.0f, (float)16.0f, (float)-3.0f));
        partdefinition.addOrReplaceChild("bottom_fin", CubeListBuilder.create().texOffs(20, 21).addBox(0.0f, 0.0f, 0.0f, 0.0f, 4.0f, 6.0f, cubeDeformation), PartPose.offset((float)0.0f, (float)22.0f, (float)-3.0f));
        return LayerDefinition.create((MeshDefinition)meshdefinition, (int)32, (int)32);
    }

    public void setupAnim(AquaFishRenderState renderState) {
        super.setupAnim(renderState);
        float f = renderState.isInWater ? 1.0f : 1.5f;
        this.tail.yRot = -f * 0.45f * Mth.sin((double)(0.6f * renderState.ageInTicks));
    }
}
