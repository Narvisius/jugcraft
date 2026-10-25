/* Owner-supplied Jimtafacts geometry, recovered from artifacts/client/item/mesh/BodyMeshDefinitions.class.
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

public final class BodyMeshDefinitions {
    private BodyMeshDefinitions() {
    }

    public static MeshDefinition createNecklace(CubeListBuilder body) {
        MeshDefinition mesh = HumanoidModel.createMesh((CubeDeformation)CubeDeformation.NONE, (float)0.0f);
        mesh.getRoot().addOrReplaceChild("body", body.texOffs(0, 0).addBox(-8.0f, -0.5f, -4.5f, 16.0f, 25.0f, 9.0f), PartPose.ZERO.withScale(0.51f));
        mesh.getRoot().retainExactParts(Set.of("body"));
        return mesh;
    }

    public static MeshDefinition createCenteredNecklace(CubeListBuilder body) {
        MeshDefinition mesh = HumanoidModel.createMesh((CubeDeformation)CubeDeformation.NONE, (float)0.0f);
        mesh.getRoot().addOrReplaceChild("body", body.texOffs(0, 0).addBox(-8.5f, -0.5f, -4.5f, 17.0f, 25.0f, 9.0f), PartPose.ZERO.withScale(0.51f));
        mesh.getRoot().retainExactParts(Set.of("body"));
        return mesh;
    }

    public static MeshDefinition createCharmOfSinking() {
        CubeListBuilder body = CubeListBuilder.create();
        body.texOffs(50, 0);
        body.addBox(-1.0f, 3.5f, -5.0f, 2.0f, 4.0f, 1.0f);
        return BodyMeshDefinitions.createNecklace(body);
    }

    public static MeshDefinition createCrossNecklace() {
        CubeListBuilder body = CubeListBuilder.create();
        body.texOffs(52, 0);
        body.addBox(-0.5f, 4.5f, -5.0f, 1.0f, 4.0f, 1.0f);
        body.texOffs(56, 0);
        body.addBox(-1.5f, 5.5f, -5.0f, 3.0f, 1.0f, 1.0f);
        return BodyMeshDefinitions.createCenteredNecklace(body);
    }

    public static MeshDefinition createPanicNecklace() {
        CubeListBuilder body = CubeListBuilder.create();
        body.texOffs(52, 0);
        body.addBox(-2.5f, 5.5f, -5.0f, 2.0f, 2.0f, 1.0f);
        body.texOffs(58, 0);
        body.addBox(0.5f, 5.5f, -5.0f, 2.0f, 2.0f, 1.0f);
        body.texOffs(52, 3);
        body.addBox(-1.5f, 6.5f, -5.0f, 3.0f, 2.0f, 1.0f);
        body.texOffs(60, 4);
        body.addBox(-0.5f, 8.5f, -5.0f, 1.0f, 1.0f, 1.0f);
        return BodyMeshDefinitions.createCenteredNecklace(body);
    }

    public static MeshDefinition createCharmOfShrinking() {
        CubeListBuilder body = CubeListBuilder.create();
        body.texOffs(52, 0);
        body.addBox(-1.5f, 4.5f, -5.0f, 3.0f, 2.0f, 1.0f);
        body.texOffs(52, 3);
        body.addBox(-0.5f, 6.5f, -5.0f, 1.0f, 1.0f, 1.0f);
        return BodyMeshDefinitions.createCenteredNecklace(body);
    }

    public static MeshDefinition createPendant() {
        CubeListBuilder body = CubeListBuilder.create();
        body.texOffs(50, 0);
        body.addBox(-1.0f, 4.5f, -5.0f, 2.0f, 2.0f, 1.0f);
        return BodyMeshDefinitions.createNecklace(body);
    }

    public static MeshDefinition createScarf() {
        MeshDefinition mesh = HumanoidModel.createMesh((CubeDeformation)new CubeDeformation(0.51f), (float)0.0f);
        mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-6.01f, -2.0f, -4.0f, 12.0f, 6.0f, 8.0f), PartPose.ZERO);
        mesh.getRoot().getChild("body").addOrReplaceChild("cloak", CubeListBuilder.create().texOffs(32, 0).addBox(-5.0f, 0.0f, 0.0f, 5.0f, 12.0f, 2.0f), PartPose.offset((float)0.0f, (float)0.0f, (float)1.99f));
        mesh.getRoot().getChild("head").clearRecursively();
        mesh.getRoot().retainPartsAndChildren(Set.of("body", "head"));
        return mesh;
    }
}
