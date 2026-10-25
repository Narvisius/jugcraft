/* Owner-supplied Jimtafacts geometry, recovered from artifacts/client/item/mesh/HeadMeshDefinitions.class.
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

public final class HeadMeshDefinitions {
    private HeadMeshDefinitions() {
    }

    public static MeshDefinition createEmptyHat(CubeListBuilder head) {
        MeshDefinition mesh = HumanoidModel.createMesh((CubeDeformation)CubeDeformation.NONE, (float)0.0f);
        mesh.getRoot().addOrReplaceChild("head", head, PartPose.ZERO);
        mesh.getRoot().retainExactParts(Set.of("head"));
        return mesh;
    }

    public static MeshDefinition createHat(CubeListBuilder head) {
        CubeDeformation deformation = new CubeDeformation(0.51f);
        head.texOffs(0, 0);
        head.addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, deformation);
        return HeadMeshDefinitions.createEmptyHat(head);
    }

    public static MeshDefinition createDiagonalHat(CubeListBuilder head, CubeListBuilder diagonalParts, String partName) {
        MeshDefinition mesh = HeadMeshDefinitions.createHat(head);
        mesh.getRoot().getChild("head").addOrReplaceChild(partName, diagonalParts, PartPose.rotation((float)0.7853982f, (float)0.0f, (float)0.0f));
        return mesh;
    }

    public static MeshDefinition createDrinkingHat() {
        CubeListBuilder head = CubeListBuilder.create();
        CubeListBuilder straws = CubeListBuilder.create();
        head.texOffs(32, 11);
        head.addBox(-4.0f, -6.0f, -8.0f, 8.0f, 1.0f, 4.0f);
        head.texOffs(32, 0);
        head.addBox(4.0f, -11.0f, -1.0f, 3.0f, 6.0f, 3.0f);
        head.texOffs(44, 0);
        head.addBox(-7.0f, -11.0f, -1.0f, 3.0f, 6.0f, 3.0f);
        head.texOffs(32, 9);
        head.addBox(-6.0f, -1.0f, -5.0f, 12.0f, 1.0f, 1.0f);
        straws.texOffs(0, 16);
        straws.addBox(5.0f, -4.0f, -3.0f, 1.0f, 1.0f, 8.0f);
        straws.texOffs(18, 16);
        straws.addBox(-6.0f, -4.0f, -3.0f, 1.0f, 1.0f, 8.0f);
        return HeadMeshDefinitions.createDiagonalHat(head, straws, "straws");
    }

    public static MeshDefinition createNightVisionGoggles() {
        CubeListBuilder head = CubeListBuilder.create();
        head.texOffs(0, 21);
        head.addBox(-4.0f, -6.0f, -4.95f, 8.0f, 4.0f, 1.0f);
        head.texOffs(0, 16);
        head.addBox(1.5f, -5.0f, -7.95f, 2.0f, 2.0f, 3.0f);
        head.texOffs(10, 16);
        head.addBox(-3.5f, -5.0f, -7.95f, 2.0f, 2.0f, 3.0f);
        return HeadMeshDefinitions.createHat(head);
    }

    public static MeshDefinition createSnorkel() {
        CubeListBuilder head = CubeListBuilder.create();
        CubeListBuilder tube = CubeListBuilder.create();
        head.texOffs(32, 0);
        head.addBox(-2.0f, -1.5f, -6.0f, 8.0f, 2.0f, 2.0f);
        tube.texOffs(0, 16);
        tube.addBox(4.01f, -5.0f, -3.0f, 2.0f, 2.0f, 12.0f);
        return HeadMeshDefinitions.createDiagonalHat(head, tube, "tube");
    }

    public static MeshDefinition createSuperstitiousHat() {
        CubeListBuilder head = CubeListBuilder.create();
        head.texOffs(0, 0);
        head.addBox(-4.0f, -16.0f, -4.0f, 8.0f, 8.0f, 8.0f);
        head.texOffs(0, 16);
        head.addBox(-5.0f, -9.0f, -5.0f, 10.0f, 1.0f, 10.0f);
        return HeadMeshDefinitions.createEmptyHat(head);
    }

    public static MeshDefinition createBrimmedHat(CubeListBuilder head) {
        head.texOffs(0, 16);
        head.addBox(-8.0f, -5.125f, -8.0f, 16.0f, 0.0f, 16.0f);
        return HeadMeshDefinitions.createHat(head);
    }

    public static MeshDefinition createWhoopeeCushion() {
        CubeListBuilder head = CubeListBuilder.create();
        head.texOffs(0, 0);
        head.addBox(-3.0f, -10.0f, -3.0f, 6.0f, 2.0f, 6.0f);
        head.texOffs(0, 8);
        head.addBox(-2.0f, -9.25f, 3.0f, 4.0f, 0.0f, 4.0f);
        return HeadMeshDefinitions.createEmptyHat(head);
    }

    public static MeshDefinition createCowboyHat() {
        CubeListBuilder head = CubeListBuilder.create();
        CubeListBuilder leftBrim = CubeListBuilder.create();
        CubeListBuilder rightBrim = CubeListBuilder.create();
        head.texOffs(-16, 16);
        head.addBox(-6.0f, -5.125f, -8.0f, 12.0f, 0.0f, 16.0f);
        leftBrim.texOffs(8, 16);
        leftBrim.addBox(0.0f, 0.0f, 0.0f, 2.0f, 0.0f, 16.0f);
        rightBrim.texOffs(12, 16);
        rightBrim.addBox(0.0f, 0.0f, 0.0f, 2.0f, 0.0f, 16.0f);
        MeshDefinition mesh = HeadMeshDefinitions.createHat(head);
        mesh.getRoot().getChild("head").addOrReplaceChild("left_brim", leftBrim, PartPose.offsetAndRotation((float)6.0f, (float)-5.125f, (float)-8.0f, (float)0.0f, (float)0.0f, (float)-0.7853982f));
        mesh.getRoot().getChild("head").addOrReplaceChild("right_brim", rightBrim, PartPose.offsetAndRotation((float)-7.41421f, (float)-6.53921f, (float)-8.0f, (float)0.0f, (float)0.0f, (float)0.7853982f));
        return mesh;
    }

    public static MeshDefinition createAnglersHat() {
        CubeListBuilder head = CubeListBuilder.create();
        CubeDeformation deformation = new CubeDeformation(0.0f, 0.51f, 0.2505f);
        head.texOffs(24, -4);
        head.addBox(4.51f, -13.635f, 0.2505f, 0.0f, 8.0f, 4.0f, deformation);
        return HeadMeshDefinitions.createBrimmedHat(head);
    }
}
