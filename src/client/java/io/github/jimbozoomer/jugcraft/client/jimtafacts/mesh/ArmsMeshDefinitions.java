/* Owner-supplied Jimtafacts geometry, recovered from artifacts/client/item/mesh/ArmsMeshDefinitions.class.
 * Cube dimensions, UVs, deformation and part poses are retained; package adapted for Jugcraft.
 * Source and provenance: art/owner-library/originals/Oct23/Jimtafacts; see jimtafacts-wearables.md.
 * Integration assistance: OpenAI Codex (GPT-6).
 */
package io.github.jimbozoomer.jugcraft.client.jimtafacts.mesh;

import java.util.Set;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.world.entity.HumanoidArm;

public final class ArmsMeshDefinitions {
    private ArmsMeshDefinitions() {
    }

    public static MeshDefinition retainArm(MeshDefinition mesh, HumanoidArm arm) {
        mesh.getRoot().retainPartsAndChildren(Set.of(arm == HumanoidArm.LEFT ? "left_arm" : "right_arm"));
        return mesh;
    }

    private static MeshDefinition createEmptyArms(CubeListBuilder leftArm, CubeListBuilder rightArm, boolean hasSlimArms) {
        MeshDefinition mesh = HumanoidModel.createMesh((CubeDeformation)CubeDeformation.NONE, (float)0.0f);
        mesh.getRoot().addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset((float)5.0f, (float)2.0f, (float)0.0f));
        mesh.getRoot().addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset((float)-5.0f, (float)2.0f, (float)0.0f));
        float armWidth = hasSlimArms ? 3.0f : 4.0f;
        mesh.getRoot().getChild("left_arm").addOrReplaceChild("artifact", leftArm, PartPose.offset((float)(-1.0f + armWidth / 2.0f), (float)10.0f, (float)0.0f));
        mesh.getRoot().getChild("right_arm").addOrReplaceChild("artifact", rightArm, PartPose.offset((float)(1.0f - armWidth / 2.0f), (float)10.0f, (float)0.0f));
        return mesh;
    }

    private static MeshDefinition createArms(CubeListBuilder leftArm, CubeListBuilder rightArm, boolean hasSlimArms) {
        float armWidth = hasSlimArms ? 3.0f : 4.0f;
        leftArm.texOffs(0, 0);
        leftArm.addBox(-armWidth / 2.0f, -12.0f, -2.0f, armWidth, 12.0f, 4.0f, new CubeDeformation(0.5f));
        rightArm.texOffs(16, 0);
        rightArm.addBox(-armWidth / 2.0f, -12.0f, -2.0f, armWidth, 12.0f, 4.0f, new CubeDeformation(0.5f));
        return ArmsMeshDefinitions.createEmptyArms(leftArm, rightArm, hasSlimArms);
    }

    private static MeshDefinition createSleevedArms(CubeListBuilder leftArm, CubeListBuilder rightArm, boolean hasSlimArms) {
        float armWidth = hasSlimArms ? 3.0f : 4.0f;
        leftArm.texOffs(0, 16);
        leftArm.addBox(-armWidth / 2.0f, -12.0f, -2.0f, armWidth, 12.0f, 4.0f, new CubeDeformation(0.75f));
        rightArm.texOffs(16, 16);
        rightArm.addBox(-armWidth / 2.0f, -12.0f, -2.0f, armWidth, 12.0f, 4.0f, new CubeDeformation(0.75f));
        return ArmsMeshDefinitions.createArms(leftArm, rightArm, hasSlimArms);
    }

    public static MeshDefinition createGlove(boolean hasSlimArms) {
        return ArmsMeshDefinitions.createSleevedArms(CubeListBuilder.create(), CubeListBuilder.create(), hasSlimArms);
    }

    public static MeshDefinition createClaws(boolean hasSlimArms) {
        CubeListBuilder leftArm = CubeListBuilder.create();
        CubeListBuilder rightArm = CubeListBuilder.create();
        leftArm.texOffs(0, 0);
        leftArm.addBox(hasSlimArms ? -2.0f : -1.5f, 0.0f, -1.5f, 3.0f, 5.0f, 1.0f);
        rightArm.texOffs(8, 0);
        rightArm.addBox(hasSlimArms ? -1.0f : -1.5f, 0.0f, -1.5f, 3.0f, 5.0f, 1.0f);
        leftArm.texOffs(0, 6);
        leftArm.addBox(hasSlimArms ? -2.0f : -1.5f, 0.0f, 0.5f, 3.0f, 5.0f, 1.0f);
        rightArm.texOffs(8, 6);
        rightArm.addBox(hasSlimArms ? -1.0f : -1.5f, 0.0f, 0.5f, 3.0f, 5.0f, 1.0f);
        leftArm.texOffs(16, 0);
        leftArm.addBox(hasSlimArms ? 1.0f : 1.5f, 0.0f, -1.5f, 1.0f, 4.0f, 1.0f);
        rightArm.texOffs(20, 0);
        rightArm.addBox(hasSlimArms ? -2.0f : -2.5f, 0.0f, -1.5f, 1.0f, 4.0f, 1.0f);
        leftArm.texOffs(16, 6);
        leftArm.addBox(hasSlimArms ? 1.0f : 1.5f, 0.0f, 0.5f, 1.0f, 4.0f, 1.0f);
        rightArm.texOffs(20, 6);
        rightArm.addBox(hasSlimArms ? -2.0f : -2.5f, 0.0f, 0.5f, 1.0f, 4.0f, 1.0f);
        return ArmsMeshDefinitions.createEmptyArms(leftArm, rightArm, hasSlimArms);
    }

    public static MeshDefinition createGoldenHook(boolean hasSlimArms) {
        CubeListBuilder leftArm = CubeListBuilder.create();
        CubeListBuilder rightArm = CubeListBuilder.create();
        leftArm.texOffs(32, 0);
        leftArm.addBox(-2.5f, 2.0f, -0.5f, 5.0f, 5.0f, 1.0f);
        rightArm.texOffs(48, 0);
        rightArm.addBox(-2.5f, 2.0f, -0.5f, 5.0f, 5.0f, 1.0f);
        leftArm.texOffs(32, 6);
        leftArm.addBox(-0.5f, 0.0f, -0.5f, 1.0f, 2.0f, 1.0f);
        rightArm.texOffs(48, 6);
        rightArm.addBox(-0.5f, 0.0f, -0.5f, 1.0f, 2.0f, 1.0f);
        return ArmsMeshDefinitions.createSleevedArms(leftArm, rightArm, hasSlimArms);
    }

    public static MeshDefinition createBracelet(boolean hasSlimArms, int size, int h) {
        CubeListBuilder leftArm = CubeListBuilder.create();
        CubeListBuilder rightArm = CubeListBuilder.create();
        float armWidth = hasSlimArms ? (float)(size - 1) : (float)size;
        leftArm.texOffs(0, 0);
        leftArm.addBox(-1.0f - armWidth / 2.0f, (float)h, -1.0f - (float)size / 2.0f, armWidth + 2.0f, 2.0f, 1.0f);
        rightArm.texOffs(16, 0);
        rightArm.addBox(-1.0f - armWidth / 2.0f, (float)h, -1.0f - (float)size / 2.0f, armWidth + 2.0f, 2.0f, 1.0f);
        leftArm.texOffs(0, 3);
        leftArm.addBox(-1.0f - armWidth / 2.0f, (float)h, (float)size / 2.0f, armWidth + 2.0f, 2.0f, 1.0f);
        rightArm.texOffs(16, 3);
        rightArm.addBox(-1.0f - armWidth / 2.0f, (float)h, (float)size / 2.0f, armWidth + 2.0f, 2.0f, 1.0f);
        leftArm.texOffs(0, 6);
        leftArm.addBox(armWidth / 2.0f, (float)h, (float)(-size) / 2.0f, 1.0f, 2.0f, (float)size);
        rightArm.texOffs(16, 6);
        rightArm.addBox(armWidth / 2.0f, (float)h, (float)(-size) / 2.0f, 1.0f, 2.0f, (float)size);
        leftArm.texOffs(0, 12);
        leftArm.addBox(-1.0f - armWidth / 2.0f, (float)h, (float)(-size) / 2.0f, 1.0f, 2.0f, (float)size);
        rightArm.texOffs(16, 12);
        rightArm.addBox(-1.0f - armWidth / 2.0f, (float)h, (float)(-size) / 2.0f, 1.0f, 2.0f, (float)size);
        return ArmsMeshDefinitions.createEmptyArms(leftArm, rightArm, hasSlimArms);
    }

    public static MeshDefinition createPickaxeHeater(boolean hasSlimArms) {
        CubeListBuilder leftArm = CubeListBuilder.create();
        CubeListBuilder rightArm = CubeListBuilder.create();
        float armWidth = hasSlimArms ? 3.0f : 4.0f;
        float armDepth = 4.0f;
        float deformationY = 1.0833334f;
        CubeDeformation deformation = new CubeDeformation(0.0f, 0.083333336f, 0.25f);
        leftArm.texOffs(32, 0);
        leftArm.addBox(armWidth / 2.0f + 0.5f, 0.5f - 2.0f * deformationY - 1.0f, -armDepth / 4.0f, 1.0f, 2.0f, 2.0f, deformation);
        rightArm.texOffs(48, 0);
        rightArm.addBox(-armWidth / 2.0f - 1.5f, 0.5f - 2.0f * deformationY - 1.0f, -armDepth / 4.0f, 1.0f, 2.0f, 2.0f, deformation);
        return ArmsMeshDefinitions.createSleevedArms(leftArm, rightArm, hasSlimArms);
    }

    public static MeshDefinition createPocketPiston(boolean hasSlimArms) {
        CubeListBuilder leftArm = CubeListBuilder.create();
        CubeListBuilder rightArm = CubeListBuilder.create();
        CubeListBuilder leftPistonHead = CubeListBuilder.create();
        CubeListBuilder rightPistonHead = CubeListBuilder.create();
        float armWidth = hasSlimArms ? 3.0f : 4.0f;
        float armDepth = 4.0f;
        float d = 0.135f;
        CubeDeformation baseDeformation = new CubeDeformation(d * armWidth, d * 3.0f, d * armDepth);
        leftArm.texOffs(0, 0);
        leftArm.addBox(-armWidth / 2.0f, -3.0f, -armDepth / 2.0f, armWidth, 3.0f, armDepth, baseDeformation);
        rightArm.texOffs(16, 0);
        rightArm.addBox(-armWidth / 2.0f, -3.0f, -armDepth / 2.0f, armWidth, 3.0f, armDepth, baseDeformation);
        CubeDeformation rodDeformation = new CubeDeformation(d * armWidth / 2.0f, 0.0f, d * armDepth / 2.0f);
        leftPistonHead.texOffs(0, 12);
        leftPistonHead.addBox(-(armWidth - 2.0f) / 2.0f, -2.0f + d * 3.0f, -(armDepth - 2.0f) / 2.0f, armWidth - 2.0f, 2.0f, armDepth - 2.0f, rodDeformation);
        rightPistonHead.texOffs(16, 12);
        rightPistonHead.addBox(-(armWidth - 2.0f) / 2.0f, -2.0f + d * 3.0f, -(armDepth - 2.0f) / 2.0f, armWidth - 2.0f, 2.0f, armDepth - 2.0f, rodDeformation);
        CubeDeformation headDeformation = new CubeDeformation(d * armWidth, d, d * armDepth);
        leftPistonHead.texOffs(0, 7);
        leftPistonHead.addBox(-armWidth / 2.0f, d * 3.0f + d, -armDepth / 2.0f, armWidth, 1.0f, armDepth, headDeformation);
        rightPistonHead.texOffs(16, 7);
        rightPistonHead.addBox(-armWidth / 2.0f, d * 3.0f + d, -armDepth / 2.0f, armWidth, 1.0f, armDepth, headDeformation);
        MeshDefinition mesh = ArmsMeshDefinitions.createEmptyArms(leftArm, rightArm, hasSlimArms);
        mesh.getRoot().getChild("left_arm").getChild("artifact").addOrReplaceChild("piston_head", leftPistonHead, PartPose.ZERO);
        mesh.getRoot().getChild("right_arm").getChild("artifact").addOrReplaceChild("piston_head", rightPistonHead, PartPose.ZERO);
        return mesh;
    }
}
