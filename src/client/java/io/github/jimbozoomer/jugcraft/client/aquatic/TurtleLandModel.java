// Original supplied mesh and animation, with native package/API adaptation.
package io.github.jimbozoomer.jugcraft.client.aquatic;

import java.util.Set;

import net.minecraft.client.model.BabyModelTransform;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

public class TurtleLandModel
extends QuadrupedModel<LivingEntityRenderState> {
    public static final MeshTransformer BABY_TRANSFORMER = new BabyModelTransform(false, 1.1f, 1.5f, 2.0f, 2.0f, 24.0f, Set.of("head"));
    private final ModelPart tail;
    private final ModelPart shellTop;
    private final ModelPart belly;

    public TurtleLandModel(ModelPart part) {
        super(part);
        this.tail = part.getChild("tail");
        this.shellTop = part.getChild("shell_top");
        this.belly = part.getChild("belly");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition modelDefinition = new MeshDefinition();
        PartDefinition def = modelDefinition.getRoot();
        def.addOrReplaceChild("head", CubeListBuilder.create().texOffs(24, 0).addBox(-1.0f, -1.0f, -0.5f, 2.0f, 2.0f, 2.0f), PartPose.offset((float)0.0f, (float)22.2f, (float)-4.0f));
        def.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5f, -1.0f, -3.0f, 5.0f, 2.0f, 6.0f), PartPose.offset((float)0.0f, (float)22.0f, (float)0.0f));
        def.addOrReplaceChild("belly", CubeListBuilder.create().texOffs(0, 14).addBox(-2.0f, -0.5f, -2.5f, 4.0f, 1.0f, 5.0f), PartPose.offset((float)0.0f, (float)23.0f, (float)0.0f));
        def.addOrReplaceChild("shell_top", CubeListBuilder.create().texOffs(0, 8).addBox(-2.0f, -0.5f, -2.5f, 4.0f, 1.0f, 5.0f), PartPose.offset((float)0.0f, (float)20.5f, (float)0.0f));
        def.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(16, 3).addBox(-0.5f, -1.0f, -0.5f, 1.0f, 2.0f, 1.0f), PartPose.offsetAndRotation((float)-2.5f, (float)23.0f, (float)3.0f, (float)0.5235988f, (float)5.7595863f, (float)0.0f));
        def.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(16, 0).addBox(-0.5f, -1.0f, -0.5f, 1.0f, 2.0f, 1.0f), PartPose.offsetAndRotation((float)2.5f, (float)23.0f, (float)-3.0f, (float)-0.5235988f, (float)-0.5235988f, (float)0.0f));
        def.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(20, 3).addBox(-0.5f, -1.0f, -0.5f, 1.0f, 2.0f, 1.0f), PartPose.offsetAndRotation((float)-2.5f, (float)23.0f, (float)-3.0f, (float)-0.5235988f, (float)0.5235988f, (float)0.0f));
        def.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(20, 0).addBox(-0.5f, -1.0f, -0.5f, 1.0f, 2.0f, 1.0f), PartPose.offsetAndRotation((float)2.5f, (float)23.0f, (float)3.0f, (float)0.5235988f, (float)0.5235988f, (float)0.0f));
        def.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(26, 0).addBox(-0.5f, -0.5f, -0.5f, 1.0f, 1.0f, 1.0f), PartPose.offset((float)0.0f, (float)22.5f, (float)3.2f));
        return LayerDefinition.create((MeshDefinition)modelDefinition, (int)64, (int)32);
    }

    public void setupAnim(LivingEntityRenderState renderState) {
        float walkAnimationPos = renderState.walkAnimationPos;
        float walkAnimationSpeed = renderState.walkAnimationSpeed;
        this.head.xRot = renderState.xRot * ((float)Math.PI / 180);
        this.head.yRot = renderState.yRot * ((float)Math.PI / 180);
        this.rightHindLeg.xRot = 0.5235988f + Mth.cos((double)(walkAnimationPos * 5.0f)) * 1.4f * walkAnimationSpeed;
        this.leftHindLeg.xRot = -0.5235988f + -(Mth.cos((double)(walkAnimationPos * 5.0f)) * 1.4f * walkAnimationSpeed);
        this.rightFrontLeg.xRot = -0.5235988f + -(Mth.cos((double)(walkAnimationPos * 5.0f)) * 1.4f * walkAnimationSpeed);
        this.leftFrontLeg.xRot = 0.5235988f + Mth.cos((double)(walkAnimationPos * 5.0f)) * 1.4f * walkAnimationSpeed;
        this.tail.yRot = Mth.cos((double)(walkAnimationPos * 0.4662f)) * 0.6f * walkAnimationSpeed;
    }
}
