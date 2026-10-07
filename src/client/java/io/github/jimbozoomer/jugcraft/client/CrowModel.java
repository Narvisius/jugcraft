package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A crow: a sleek black body, a head with a stout beak, a fanned tail and two wings that spread and beat while it flies
 * and fold while it pecks (its head bobbing down). Texture: tools/crow_textures.py, whose box layout matches this one.
 */
public class CrowModel extends EntityModel<CrowRenderer.State> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Jugcraft.id("crow"), "main");

	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart leftWing;
	private final ModelPart rightWing;
	private final ModelPart tail;

	public CrowModel(ModelPart root) {
		super(root);
		head = root.getChild("head");
		body = root.getChild("body");
		leftWing = root.getChild("left_wing");
		rightWing = root.getChild("right_wing");
		tail = root.getChild("tail");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -3.0F, -3.0F, 3.0F, 3.0F, 3.0F),
				PartPose.offset(0.0F, 19.5F, -3.0F));
		head.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(12, 0).addBox(-0.5F, -1.75F, -5.0F, 1.0F, 1.0F, 2.0F), PartPose.ZERO);
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 6).addBox(-2.0F, -2.0F, -3.5F, 4.0F, 4.0F, 7.0F),
				PartPose.offset(0.0F, 20.0F, 0.0F));
		root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 17).addBox(0.0F, 0.0F, -3.0F, 1.0F, 3.0F, 6.0F),
				PartPose.offset(2.0F, 18.5F, 0.0F));
		root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(14, 17).addBox(-1.0F, 0.0F, -3.0F, 1.0F, 3.0F, 6.0F),
				PartPose.offset(-2.0F, 18.5F, 0.0F));
		root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(24, 0).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 1.0F, 3.0F),
				PartPose.offsetAndRotation(0.0F, 19.0F, 3.5F, -0.3F, 0.0F, 0.0F));
		PartDefinition legs = root.addOrReplaceChild("legs", CubeListBuilder.create(), PartPose.ZERO);
		legs.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(28, 6).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
				PartPose.offset(1.0F, 22.0F, 0.5F));
		legs.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(28, 6).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
				PartPose.offset(-1.0F, 22.0F, 0.5F));
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(CrowRenderer.State state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		if (state.pecking) {
			// Wings folded, head bobbing down at the crop.
			leftWing.zRot = -0.1F;
			rightWing.zRot = 0.1F;
			head.xRot = 0.7F + 0.35F * Mth.sin(t * 1.4F);
			body.xRot = 0.25F;
		} else {
			// Wings spread out from the sides and beating.
			float spread = 1.25F + 0.45F * Mth.sin(t * 1.1F);
			leftWing.zRot = -spread;
			rightWing.zRot = spread;
			head.xRot = state.xRot * Mth.DEG_TO_RAD;
			body.xRot = 0.0F;
		}
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		tail.xRot = -0.3F + 0.08F * Mth.sin(t * 0.5F);
	}
}
