/* Owner-supplied Jimtafacts geometry, recovered from artifacts/client/item/mesh/BeltMeshDefinitions.class.
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

public final class BeltMeshDefinitions {
    private BeltMeshDefinitions() {
    }

    private static MeshDefinition createBelt(CubeListBuilder charm) {
        CubeDeformation deformation = new CubeDeformation(0.5f);
        MeshDefinition mesh = HumanoidModel.createMesh((CubeDeformation)CubeDeformation.NONE, (float)0.0f);
        mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f, deformation), PartPose.ZERO);
        mesh.getRoot().getChild("body").addOrReplaceChild("charm", charm, PartPose.ZERO);
        mesh.getRoot().retainPartsAndChildren(Set.of("body"));
        return mesh;
    }

    public static MeshDefinition createAntidoteVessel() {
        CubeListBuilder charm = CubeListBuilder.create();
        charm.texOffs(0, 16);
        charm.addBox(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f);
        charm.texOffs(0, 26);
        charm.addBox(-1.0f, -1.0f, -1.0f, 2.0f, 1.0f, 2.0f);
        return BeltMeshDefinitions.createBelt(charm);
    }

    public static MeshDefinition createCloudInABottle() {
        CubeListBuilder charm = CubeListBuilder.create();
        charm.texOffs(0, 16);
        charm.addBox(-2.0f, 0.0f, -2.0f, 4.0f, 5.0f, 4.0f);
        charm.texOffs(0, 25);
        charm.addBox(-1.0f, -1.0f, -1.0f, 2.0f, 1.0f, 2.0f);
        MeshDefinition mesh = BeltMeshDefinitions.createBelt(charm);
        mesh.getRoot().getChild("body").getChild("charm").addOrReplaceChild("cloud", CubeListBuilder.create().texOffs(8, 25).addBox(-1.0f, 1.5f, -1.0f, 2.0f, 2.0f, 2.0f), PartPose.ZERO);
        return mesh;
    }

    public static MeshDefinition createCrystalHeart() {
        CubeListBuilder charm = CubeListBuilder.create();
        charm.texOffs(0, 16);
        charm.addBox(-2.5f, 0.0f, 0.0f, 2.0f, 3.0f, 1.0f);
        charm.texOffs(6, 16);
        charm.addBox(0.5f, 0.0f, 0.0f, 2.0f, 3.0f, 1.0f);
        charm.texOffs(0, 20);
        charm.addBox(-0.5f, 1.0f, 0.0f, 1.0f, 4.0f, 1.0f);
        charm.texOffs(4, 20);
        charm.addBox(-1.5f, 3.0f, 0.0f, 1.0f, 1.0f, 1.0f);
        charm.texOffs(8, 20);
        charm.addBox(0.5f, 3.0f, 0.0f, 1.0f, 1.0f, 1.0f);
        return BeltMeshDefinitions.createBelt(charm);
    }

    public static MeshDefinition createHeliumFlamingo() {
        MeshDefinition mesh = HumanoidModel.createMesh((CubeDeformation)CubeDeformation.NONE, (float)0.0f);
        mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 36).addBox(-1.0f, 1.0f, -14.0f, 2.0f, 3.0f, 5.0f).texOffs(0, 18).addBox(4.0f, 9.0f, -7.0f, 4.0f, 4.0f, 14.0f).texOffs(0, 0).addBox(-8.0f, 9.0f, -7.0f, 4.0f, 4.0f, 14.0f).texOffs(36, 0).addBox(-4.0f, 9.0f, 3.0f, 8.0f, 4.0f, 4.0f).texOffs(36, 8).addBox(-4.0f, 9.0f, -7.0f, 8.0f, 4.0f, 4.0f).texOffs(0, 36).addBox(-2.0f, 1.0f, -9.0f, 4.0f, 11.0f, 4.0f), PartPose.ZERO);
        mesh.getRoot().retainExactParts(Set.of("body"));
        return mesh;
    }

    public static MeshDefinition createObsidianSkull() {
        CubeListBuilder charm = CubeListBuilder.create();
        charm.texOffs(0, 16);
        charm.addBox(-2.5f, 0.0f, 0.0f, 5.0f, 3.0f, 4.0f);
        charm.texOffs(18, 16);
        charm.addBox(-1.5f, 3.0f, 0.0f, 1.0f, 1.0f, 2.0f);
        charm.texOffs(18, 19);
        charm.addBox(0.5f, 3.0f, 0.0f, 1.0f, 1.0f, 2.0f);
        return BeltMeshDefinitions.createBelt(charm);
    }

    public static MeshDefinition createUniversalAttractor() {
        CubeListBuilder charm = CubeListBuilder.create();
        charm.texOffs(0, 16);
        charm.addBox(-2.5f, 0.0f, 0.0f, 5.0f, 2.0f, 1.0f);
        charm.texOffs(0, 19);
        charm.addBox(-2.5f, 2.0f, 0.0f, 2.0f, 4.0f, 1.0f);
        charm.texOffs(6, 19);
        charm.addBox(0.5f, 2.0f, 0.0f, 2.0f, 4.0f, 1.0f);
        return BeltMeshDefinitions.createBelt(charm);
    }

    public static MeshDefinition createChorusTotem() {
        CubeListBuilder charm = CubeListBuilder.create();
        charm.texOffs(0, 16);
        charm.addBox(-1.5f, -1.0f, -1.0f, 3.0f, 7.0f, 2.0f);
        charm.texOffs(10, 16);
        charm.addBox(-3.5f, 0.0f, 1.0f, 7.0f, 4.0f, 0.0f);
        charm.texOffs(0, 25);
        charm.addBox(-2.5f, 3.0f, -2.0f, 5.0f, 2.0f, 2.0f);
        charm.texOffs(10, 20);
        charm.addBox(-1.0f, 1.0f, -1.5f, 2.0f, 2.0f, 0.0f);
        return BeltMeshDefinitions.createBelt(charm);
    }

    public static MeshDefinition createWarpDrive() {
        CubeListBuilder charm = CubeListBuilder.create();
        charm.texOffs(0, 16);
        charm.addBox(-2.5f, 0.0f, -2.0f, 5.0f, 4.0f, 4.0f);
        charm.texOffs(0, 24);
        charm.addBox(-3.0f, 1.0f, -1.0f, 1.0f, 2.0f, 2.0f);
        charm.texOffs(6, 24);
        charm.addBox(2.0f, 1.0f, -1.0f, 1.0f, 2.0f, 2.0f);
        charm.texOffs(0, 28);
        charm.addBox(-2.5f, 1.0f, 0.0f, 5.0f, 2.0f, 0.0f);
        charm.texOffs(0, 30);
        charm.addBox(-2.5f, 2.0f, -1.0f, 5.0f, 0.0f, 2.0f);
        return BeltMeshDefinitions.createBelt(charm);
    }
}
