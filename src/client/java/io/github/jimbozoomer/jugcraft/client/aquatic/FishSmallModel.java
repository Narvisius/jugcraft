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

public class FishSmallModel
extends FishBaseModel {
    private final ModelPart finRight;
    private final ModelPart finTop;
    private final ModelPart tail;
    private final ModelPart finLeft;
    private final ModelPart body;
    private final ModelPart finBottom;

    public FishSmallModel(ModelPart part) {
        super(part);
        this.finRight = part.getChild("fin_right");
        this.finTop = part.getChild("fin_top");
        this.tail = part.getChild("tail");
        this.finLeft = part.getChild("fin_left");
        this.body = part.getChild("body");
        this.finBottom = part.getChild("fin_bottom");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition modelDefinition = new MeshDefinition();
        PartDefinition def = modelDefinition.getRoot();
        def.addOrReplaceChild("fin_right", CubeListBuilder.create().texOffs(2, 16).addBox(-2.0f, -1.0f, 0.0f, 2.0f, 2.0f, 0.0f), PartPose.offsetAndRotation((float)-1.0f, (float)22.5f, (float)0.0f, (float)0.0f, (float)0.0f, (float)-0.7853982f));
        def.addOrReplaceChild("fin_top", CubeListBuilder.create().texOffs(10, -5).addBox(0.0f, -3.0f, 0.0f, 0.0f, 3.0f, 6.0f), PartPose.offset((float)0.0f, (float)20.5f, (float)-3.0f));
        def.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(22, -6).addBox(0.0f, -1.5f, 0.0f, 0.0f, 3.0f, 6.0f), PartPose.offset((float)0.0f, (float)22.0f, (float)3.0f));
        def.addOrReplaceChild("fin_left", CubeListBuilder.create().texOffs(2, 12).addBox(0.0f, -1.0f, 0.0f, 2.0f, 2.0f, 0.0f), PartPose.offsetAndRotation((float)1.0f, (float)22.5f, (float)0.0f, (float)0.0f, (float)0.0f, (float)0.7853982f));
        def.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, -1.5f, -3.0f, 2.0f, 3.0f, 6.0f), PartPose.offset((float)0.0f, (float)22.0f, (float)0.0f));
        def.addOrReplaceChild("fin_bottom", CubeListBuilder.create().texOffs(10, 5).addBox(0.0f, 0.0f, 0.0f, 0.0f, 3.0f, 6.0f), PartPose.offset((float)0.0f, (float)23.5f, (float)-3.0f));
        return LayerDefinition.create((MeshDefinition)modelDefinition, (int)32, (int)32);
    }

    public void setupAnim(AquaFishRenderState renderState) {
        super.setupAnim(renderState);
        float f = renderState.isInWater ? 1.0f : 1.5f;
        this.tail.yRot = -f * 0.45f * Mth.sin((double)(0.6f * renderState.ageInTicks));
    }
}
