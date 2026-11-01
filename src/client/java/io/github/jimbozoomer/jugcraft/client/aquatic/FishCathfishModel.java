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

public class FishCathfishModel
extends FishBaseModel {
    private final ModelPart head;
    private final ModelPart headFront;
    private final ModelPart body;
    private final ModelPart topFin;
    private final ModelPart leftFin;
    private final ModelPart rightFin;
    private final ModelPart bottomFin;
    private final ModelPart tail;
    private final ModelPart leftWhisker;
    private final ModelPart rightWhisker;

    public FishCathfishModel(ModelPart part) {
        super(part);
        this.head = part.getChild("head");
        this.headFront = part.getChild("head_front");
        this.body = part.getChild("body");
        this.topFin = part.getChild("top_fin");
        this.leftFin = part.getChild("left_fin");
        this.rightFin = part.getChild("right_fin");
        this.bottomFin = part.getChild("bottom_fin");
        this.tail = part.getChild("tail");
        this.leftWhisker = part.getChild("left_whisker");
        this.rightWhisker = part.getChild("right_whisker");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition modelDefinition = new MeshDefinition();
        PartDefinition def = modelDefinition.getRoot();
        def.addOrReplaceChild("head", CubeListBuilder.create().texOffs(11, 0).addBox(-1.0f, -2.0f, -3.0f, 4.0f, 3.0f, 3.0f), PartPose.offset((float)-1.0f, (float)22.5f, (float)0.0f));
        def.addOrReplaceChild("head_front", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, -2.0f, -1.0f, 2.0f, 3.0f, 1.0f), PartPose.offset((float)0.0f, (float)22.5f, (float)-3.0f));
        def.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, -2.0f, 0.0f, 2.0f, 4.0f, 7.0f), PartPose.offset((float)0.0f, (float)22.0f, (float)0.0f));
        def.addOrReplaceChild("top_fin", CubeListBuilder.create().texOffs(20, 7).addBox(0.0f, -1.0f, 0.0f, 0.0f, 2.0f, 6.0f), PartPose.offset((float)0.0f, (float)19.0f, (float)0.0f));
        def.addOrReplaceChild("left_fin", CubeListBuilder.create().texOffs(26, 2).addBox(0.0f, 0.0f, -1.0f, 2.0f, 0.0f, 2.0f), PartPose.offsetAndRotation((float)1.0f, (float)21.9f, (float)1.5f, (float)-1.5707964f, (float)0.0f, (float)0.7853982f));
        def.addOrReplaceChild("right_fin", CubeListBuilder.create().texOffs(26, 4).addBox(-2.0f, 0.0f, -1.0f, 2.0f, 0.0f, 2.0f), PartPose.offsetAndRotation((float)-1.0f, (float)22.0f, (float)1.0f, (float)-1.5707964f, (float)0.0f, (float)-0.7853982f));
        def.addOrReplaceChild("bottom_fin", CubeListBuilder.create().texOffs(20, 5).addBox(0.0f, -1.0f, 0.0f, 0.0f, 2.0f, 6.0f), PartPose.offset((float)0.0f, (float)25.0f, (float)0.0f));
        def.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(22, 3).addBox(0.0f, -2.0f, 0.0f, 0.0f, 4.0f, 4.0f), PartPose.offset((float)0.0f, (float)22.0f, (float)7.0f));
        def.addOrReplaceChild("left_whisker", CubeListBuilder.create().texOffs(28, 15).addBox(0.0f, -1.0f, -0.5f, 2.0f, 2.0f, 0.0f), PartPose.offset((float)2.0f, (float)22.5f, (float)-2.0f));
        def.addOrReplaceChild("right_whisker", CubeListBuilder.create().texOffs(28, 17).addBox(-2.0f, -1.0f, -0.5f, 2.0f, 2.0f, 0.0f), PartPose.offset((float)-2.0f, (float)22.5f, (float)-2.0f));
        return LayerDefinition.create((MeshDefinition)modelDefinition, (int)32, (int)32);
    }

    public void setupAnim(AquaFishRenderState renderState) {
        super.setupAnim(renderState);
        float f = renderState.isInWater ? 1.0f : 1.5f;
        this.tail.yRot = -f * 0.45f * Mth.sin((double)(0.6f * renderState.ageInTicks));
    }
}
