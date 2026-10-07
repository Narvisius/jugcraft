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
 * A squirrel, a third of a block long: a slim body, a round head with tufted ears, small paws, and a great bushy tail
 * in two parts that curls up over its back. It bounds as it runs (the body rocks and the tail streams out), sits up
 * when still with its tail curled, flicking it now and then, and holds an acorn in its forepaws when it carries one
 * (drawn by {@link SquirrelRenderer}). Texture: tools/squirrel_textures.py, whose box layout matches this one (32 by 32).
 */
public class SquirrelModel extends EntityModel<SquirrelRenderer.State> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Jugcraft.id("squirrel"), "main");

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart tail;
	private final ModelPart tailTip;
	private final ModelPart frontRight;
	private final ModelPart frontLeft;
	private final ModelPart backRight;
	private final ModelPart backLeft;

	public SquirrelModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		head = root.getChild("head");
		tail = body.getChild("tail");
		tailTip = tail.getChild("tip");
		frontRight = root.getChild("front_right");
		frontLeft = root.getChild("front_left");
		backRight = root.getChild("back_right");
		backLeft = root.getChild("back_left");
	}

	public ModelPart head() {
		return head;
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -3.5F, 4.0F, 4.0F, 7.0F),
				PartPose.offset(0.0F, 20.0F, 0.0F));
		root.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 11).addBox(-2.0F, -3.0F, -3.5F, 4.0F, 4.0F, 4.0F)
				.texOffs(16, 11).addBox(-1.0F, -1.5F, -4.5F, 2.0F, 2.0F, 1.0F)
				.texOffs(22, 11).addBox(-2.0F, -4.5F, -1.5F, 1.0F, 2.0F, 1.0F)
				.texOffs(22, 11).addBox(1.0F, -4.5F, -1.5F, 1.0F, 2.0F, 1.0F),
				PartPose.offset(0.0F, 18.5F, -3.5F));
		PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 19).addBox(-1.5F, -6.0F, -1.5F, 3.0F, 6.0F, 3.0F),
				PartPose.offsetAndRotation(0.0F, -1.0F, 3.5F, -0.85F, 0.0F, 0.0F));
		tail.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(12, 19).addBox(-2.0F, -5.0F, -2.0F, 4.0F, 5.0F, 4.0F),
				PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 1.55F, 0.0F, 0.0F));
		for (int side = -1; side <= 1; side += 2) {
			root.addOrReplaceChild(side < 0 ? "front_right" : "front_left",
					CubeListBuilder.create().texOffs(24, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F),
					PartPose.offset(side * 1.2F, 21.0F, -2.5F));
			root.addOrReplaceChild(side < 0 ? "back_right" : "back_left",
					CubeListBuilder.create().texOffs(22, 4).addBox(-1.0F, 0.0F, -1.5F, 2.0F, 2.0F, 3.0F),
					PartPose.offset(side * 1.4F, 22.0F, 2.0F));
		}
		return LayerDefinition.create(mesh, 32, 32);
	}

	@Override
	public void setupAnim(SquirrelRenderer.State state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float walk = state.walkAnimationPos;
		float speed = Math.min(1.0F, state.walkAnimationSpeed);
		float bound = Mth.cos(walk * 1.2F) * speed;
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD;
		// Bounding: the body rocks, fore and hind legs together.
		body.xRot = 0.2F * bound;
		frontRight.xRot = frontLeft.xRot = 0.9F * bound;
		backRight.xRot = backLeft.xRot = -0.9F * bound;
		// Running, the tail streams out behind; still, it curls up over the back and flicks.
		float still = 1.0F - speed;
		float flick = still * 0.15F * Mth.sin(t * 0.3F) * (Mth.sin(t * 0.05F) > 0.6F ? 1.0F : 0.0F);
		// At rest the tail sweeps up and back at about 50 degrees and its tip curls forward over the back, the S of a
		// squirrel's tail; running, it streams out nearly level.
		tail.xRot = Mth.lerp(still, -1.35F, -0.85F) + flick;
		tailTip.xRot = Mth.lerp(still, 0.3F, 1.55F);
		if (state.carrying) {
			// Sitting up a little, forepaws together at the chin round the acorn.
			frontRight.xRot = frontLeft.xRot = -0.9F;
		}
	}
}
