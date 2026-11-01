package io.github.jimbozoomer.jugcraft.client.aquatic;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
/** Original supplied station cuboids, UVs and pivots. */
public final class OwnerAquaticStationMeshes {
public static LayerDefinition tackle() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();
        PartDefinition lid = partDefinition.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0f, -3.0f, -8.0f, 14.0f, 3.0f, 8.0f), PartPose.offset((float)7.0f, (float)12.0f, (float)4.0f));
        partDefinition.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 11).addBox(0.0f, -6.0f, -8.0f, 14.0f, 6.0f, 8.0f), PartPose.offset((float)0.0f, (float)18.0f, (float)4.0f));
        lid.addOrReplaceChild("handle", CubeListBuilder.create().texOffs(36, 0).addBox(-2.0f, -4.0f, -5.0f, 4.0f, 1.0f, 2.0f), PartPose.offset((float)0.0f, (float)0.0f, (float)0.0f));
        return LayerDefinition.create((MeshDefinition)meshDefinition, (int)64, (int)32);
    }
public static LayerDefinition bounty() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 19).addBox(1.0f, 0.0f, 1.0f, 14.0f, 10.0f, 14.0f), PartPose.ZERO);
        partdefinition.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 0).addBox(1.0f, 0.0f, 0.0f, 14.0f, 5.0f, 14.0f), PartPose.offset((float)0.0f, (float)9.0f, (float)1.0f));
        return LayerDefinition.create((MeshDefinition)meshdefinition, (int)64, (int)64);
    }
}
