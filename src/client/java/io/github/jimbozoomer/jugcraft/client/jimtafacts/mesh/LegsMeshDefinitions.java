/* Owner-supplied Jimtafacts geometry, recovered from artifacts/client/item/mesh/LegsMeshDefinitions.class.
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

public final class LegsMeshDefinitions {
    private LegsMeshDefinitions() {
    }

    public static MeshDefinition createLegs(float delta, CubeListBuilder leftLeg, CubeListBuilder rightLeg) {
        CubeDeformation deformation = new CubeDeformation(delta);
        MeshDefinition mesh = HumanoidModel.createMesh((CubeDeformation)CubeDeformation.NONE, (float)0.0f);
        mesh.getRoot().addOrReplaceChild("left_leg", leftLeg.texOffs(0, 0).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, deformation), PartPose.offset((float)1.9f, (float)12.0f, (float)0.0f));
        mesh.getRoot().addOrReplaceChild("right_leg", rightLeg.texOffs(16, 0).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, deformation), PartPose.offset((float)-1.9f, (float)12.0f, (float)0.0f));
        mesh.getRoot().retainExactParts(Set.of("left_leg", "right_leg"));
        return mesh;
    }

    public static MeshDefinition createSleevedLegs(float delta, CubeListBuilder leftLeg, CubeListBuilder rightLeg) {
        CubeDeformation deformation = new CubeDeformation(delta + 0.25f);
        leftLeg.texOffs(0, 16);
        leftLeg.addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, deformation);
        rightLeg.texOffs(16, 16);
        rightLeg.addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, deformation);
        return LegsMeshDefinitions.createLegs(delta, leftLeg, rightLeg);
    }

    public static MeshDefinition createBoots(float delta) {
        return LegsMeshDefinitions.createBoots(delta, CubeListBuilder.create(), CubeListBuilder.create());
    }

    public static MeshDefinition createBoots(float delta, CubeListBuilder leftLeg, CubeListBuilder rightLeg) {
        CubeDeformation deformation = new CubeDeformation(delta, delta / 4.0f, delta / 4.0f);
        leftLeg.texOffs(0, 16);
        leftLeg.addBox(-2.0f, 9.0f + delta * 3.0f / 4.0f, -3.0f - delta * 5.0f / 4.0f, 4.0f, 3.0f, 1.0f, deformation);
        rightLeg.texOffs(16, 16);
        rightLeg.addBox(-2.0f, 9.0f + delta * 3.0f / 4.0f, -3.0f - delta * 5.0f / 4.0f, 4.0f, 3.0f, 1.0f, deformation);
        return LegsMeshDefinitions.createLegs(delta, leftLeg, rightLeg);
    }

    public static MeshDefinition createSlippers(CubeListBuilder leftLeg, CubeListBuilder rightLeg) {
        leftLeg.texOffs(32, 0);
        leftLeg.addBox(-2.5f, 8.51f, -7.01f, 5.0f, 4.0f, 5.0f);
        rightLeg.texOffs(32, 16);
        rightLeg.addBox(-2.5f, 8.51f, -7.0f, 5.0f, 4.0f, 5.0f);
        return LegsMeshDefinitions.createSleevedLegs(0.51f, leftLeg, rightLeg);
    }

    public static MeshDefinition createAquaDashers(float delta) {
        CubeListBuilder leftLeg = CubeListBuilder.create();
        CubeListBuilder rightLeg = CubeListBuilder.create();
        CubeDeformation deformation = new CubeDeformation(0.0f, delta, delta);
        leftLeg.texOffs(0, 16);
        leftLeg.addBox(2.0f + delta, 0.0f, 1.0f + delta * 3.0f / 2.0f, 0.0f, 12.0f, 4.0f, deformation);
        rightLeg.texOffs(16, 16);
        rightLeg.addBox(-2.0f - delta, 0.0f, 1.0f + delta * 3.0f / 2.0f, 0.0f, 12.0f, 4.0f, deformation);
        return LegsMeshDefinitions.createBoots(delta, leftLeg, rightLeg);
    }

    public static MeshDefinition createBunnyHoppers() {
        CubeListBuilder leftLeg = CubeListBuilder.create();
        CubeListBuilder rightLeg = CubeListBuilder.create();
        leftLeg.texOffs(32, 9);
        leftLeg.addBox(-0.5f, 10.0f, -7.5f, 1.0f, 1.0f, 1.0f);
        rightLeg.texOffs(32, 25);
        rightLeg.addBox(-0.5f, 10.0f, -7.5f, 1.0f, 1.0f, 1.0f);
        leftLeg.texOffs(52, 6);
        leftLeg.addBox(-1.0f, 9.0f, 2.0f, 2.0f, 2.0f, 2.0f);
        rightLeg.texOffs(52, 22);
        rightLeg.addBox(-1.0f, 9.0f, 2.0f, 2.0f, 2.0f, 2.0f);
        MeshDefinition mesh = LegsMeshDefinitions.createSlippers(leftLeg, rightLeg);
        mesh.getRoot().getChild("left_leg").addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(52, 0).addBox(-3.15f, 3.51f, -3.01f, 2.0f, 5.0f, 1.0f), PartPose.rotation((float)0.0f, (float)-0.2617994f, (float)0.0f));
        mesh.getRoot().getChild("right_leg").addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(52, 16).addBox(-3.15f, 3.51f, -3.0f, 2.0f, 5.0f, 1.0f), PartPose.rotation((float)0.0f, (float)-0.2617994f, (float)0.0f));
        mesh.getRoot().getChild("left_leg").addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(58, 0).addBox(1.15f, 3.51f, -3.01f, 2.0f, 5.0f, 1.0f), PartPose.rotation((float)0.0f, (float)0.2617994f, (float)0.0f));
        mesh.getRoot().getChild("right_leg").addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(58, 16).addBox(1.15f, 3.51f, -3.0f, 2.0f, 5.0f, 1.0f), PartPose.rotation((float)0.0f, (float)0.2617994f, (float)0.0f));
        return mesh;
    }

    public static MeshDefinition createFlippers() {
        CubeListBuilder leftLeg = CubeListBuilder.create();
        CubeListBuilder rightLeg = CubeListBuilder.create();
        leftLeg.texOffs(0, 16);
        leftLeg.addBox(-2.0f, 11.5f, -16.0f, 9.0f, 0.0f, 20.0f);
        rightLeg.texOffs(0, 36);
        rightLeg.addBox(-7.0f, 11.5f, -16.0f, 9.0f, 0.0f, 20.0f);
        return LegsMeshDefinitions.createLegs(0.5f, leftLeg, rightLeg);
    }

    public static MeshDefinition createKittySlippers() {
        CubeListBuilder leftLeg = CubeListBuilder.create();
        CubeListBuilder rightLeg = CubeListBuilder.create();
        leftLeg.texOffs(32, 9);
        leftLeg.addBox(-2.0f, 7.51f, -4.0f, 1.0f, 1.0f, 2.0f);
        rightLeg.texOffs(32, 25);
        rightLeg.addBox(-2.0f, 7.51f, -4.0f, 1.0f, 1.0f, 2.0f);
        leftLeg.texOffs(38, 9);
        leftLeg.addBox(1.0f, 7.51f, -4.0f, 1.0f, 1.0f, 2.0f);
        rightLeg.texOffs(38, 25);
        rightLeg.addBox(1.0f, 7.51f, -4.0f, 1.0f, 1.0f, 2.0f);
        leftLeg.texOffs(44, 9);
        leftLeg.addBox(-1.5f, 10.51f, -8.0f, 3.0f, 2.0f, 1.0f);
        rightLeg.texOffs(44, 25);
        rightLeg.addBox(-1.5f, 10.51f, -8.0f, 3.0f, 2.0f, 1.0f);
        return LegsMeshDefinitions.createSlippers(leftLeg, rightLeg);
    }

    public static MeshDefinition createSnowshoes() {
        CubeListBuilder leftLeg = CubeListBuilder.create();
        CubeListBuilder rightLeg = CubeListBuilder.create();
        leftLeg.texOffs(0, 16);
        leftLeg.addBox(-2.5f, 11.5f, -10.0f, 10.0f, 0.0f, 22.0f);
        rightLeg.texOffs(0, 38);
        rightLeg.addBox(-7.5f, 11.5f, -10.0f, 10.0f, 0.0f, 22.0f);
        return LegsMeshDefinitions.createLegs(0.5f, leftLeg, rightLeg);
    }

    public static MeshDefinition createSteadfastSpikes() {
        CubeListBuilder leftLeg = CubeListBuilder.create();
        CubeListBuilder rightLeg = CubeListBuilder.create();
        leftLeg.texOffs(32, 0);
        leftLeg.addBox(-1.5f, 9.0f, -7.0f, 1.0f, 3.0f, 5.0f);
        rightLeg.texOffs(43, 0);
        rightLeg.addBox(-1.5f, 9.0f, -7.0f, 1.0f, 3.0f, 5.0f);
        leftLeg.texOffs(32, 8);
        leftLeg.addBox(0.5f, 9.0f, -7.0f, 1.0f, 3.0f, 5.0f);
        rightLeg.texOffs(43, 8);
        rightLeg.addBox(0.5f, 9.0f, -7.0f, 1.0f, 3.0f, 5.0f);
        return LegsMeshDefinitions.createSleevedLegs(0.5f, leftLeg, rightLeg);
    }
}
