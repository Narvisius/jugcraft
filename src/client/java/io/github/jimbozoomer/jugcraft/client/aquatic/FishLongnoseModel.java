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

public class FishLongnoseModel
extends FishBaseModel {
    private final ModelPart nose;
    private final ModelPart finRight;
    private final ModelPart finLeft;
    private final ModelPart bodyFront;
    private final ModelPart bodyRear;

    public FishLongnoseModel(ModelPart part) {
        super(part);
        this.nose = part.getChild("nose");
        this.finRight = part.getChild("fin_right");
        this.finLeft = part.getChild("fin_left");
        this.bodyFront = part.getChild("body_front");
        this.bodyRear = part.getChild("body_rear");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition modelDefinition = new MeshDefinition();
        PartDefinition def = modelDefinition.getRoot();
        PartDefinition front = def.addOrReplaceChild("body_front", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5f, -2.5f, 0.0f, 3.0f, 4.0f, 8.0f), PartPose.offset((float)0.0f, (float)20.0f, (float)0.0f));
        PartDefinition rear = def.addOrReplaceChild("body_rear", CubeListBuilder.create().texOffs(0, 13).addBox(-1.5f, -2.5f, 0.0f, 3.0f, 4.0f, 8.0f), PartPose.offset((float)0.0f, (float)20.0f, (float)8.0f));
        def.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(22, 0).addBox(-1.0f, -2.0f, -3.0f, 2.0f, 2.0f, 3.0f), PartPose.offset((float)0.0f, (float)20.5f, (float)0.0f));
        def.addOrReplaceChild("fin_right", CubeListBuilder.create().texOffs(-4, 0).addBox(-2.0f, 0.0f, 0.0f, 2.0f, 0.0f, 2.0f), PartPose.offsetAndRotation((float)-1.5f, (float)21.5f, (float)4.0f, (float)0.0f, (float)0.0f, (float)-0.7853982f));
        def.addOrReplaceChild("fin_left", CubeListBuilder.create().texOffs(0, 0).addBox(0.0f, 0.0f, 0.0f, 2.0f, 0.0f, 2.0f), PartPose.offsetAndRotation((float)1.5f, (float)21.5f, (float)4.0f, (float)0.0f, (float)0.0f, (float)0.7853982f));
        front.addOrReplaceChild("fin_top_front", CubeListBuilder.create().texOffs(2, 1).addBox(0.0f, 0.0f, 0.0f, 0.0f, 2.0f, 3.0f), PartPose.offset((float)0.0f, (float)-4.5f, (float)5.0f));
        front.addOrReplaceChild("fin_front_bottom", CubeListBuilder.create().texOffs(0, 25).addBox(0.0f, 0.0f, 0.0f, 0.0f, 2.0f, 3.0f), PartPose.offset((float)0.0f, (float)1.5f, (float)5.0f));
        rear.addOrReplaceChild("fin_rear_bottom", CubeListBuilder.create().texOffs(0, 22).addBox(0.0f, 0.0f, 1.0f, 0.0f, 2.0f, 4.0f), PartPose.offset((float)0.0f, (float)1.5f, (float)-1.0f));
        rear.addOrReplaceChild("fin_top_rear", CubeListBuilder.create().texOffs(0, 2).addBox(0.0f, 0.0f, 1.0f, 0.0f, 2.0f, 4.0f), PartPose.offset((float)0.0f, (float)-4.5f, (float)-1.0f));
        rear.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(20, 10).addBox(0.0f, -2.5f, 0.0f, 0.0f, 4.0f, 6.0f), PartPose.offset((float)0.0f, (float)0.0f, (float)8.0f));
        return LayerDefinition.create((MeshDefinition)modelDefinition, (int)32, (int)32);
    }

    public void setupAnim(AquaFishRenderState renderState) {
        super.setupAnim(renderState);
        float f = 1.0f;
        float f1 = 1.0f;
        if (!renderState.isInWater) {
            f = 1.3f;
            f1 = 1.7f;
        }
        float moveAmount = 0.15f;
        this.bodyRear.yRot = -f * moveAmount * Mth.sin((double)(f1 * 0.6f * renderState.ageInTicks));
    }
}
