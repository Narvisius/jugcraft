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
 * A Pumpkling's vine body: two stubby vine legs, two leafy vine arms from the pumpkin's sides and a curling tendril with
 * a leaf on top. The pumpkin itself (its head and body in one) is drawn by {@link PumpklingRenderer} on {@link #head()},
 * whose pivot is the middle of the pumpkin's underside. It hops as it walks (the whole of it bobs, legs and arms
 * swinging), turns its pumpkin to look about, and squats with its legs out in front when sitting. Texture:
 * tools/pumpkling_textures.py, laid out as these boxes (32 by 32).
 */
public class PumpklingModel extends EntityModel<PumpklingRenderer.State> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Jugcraft.id("pumpkling"), "main");
	/** The pumpkin's size in pixels. */
	public static final float PUMPKIN = 10.0F;
	private static final float BOTTOM = 19.0F;

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart tendril;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;
	private final ModelPart rightArm;
	private final ModelPart leftArm;

	public PumpklingModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		head = body.getChild("head");
		tendril = head.getChild("tendril");
		rightLeg = root.getChild("right_leg");
		leftLeg = root.getChild("left_leg");
		rightArm = body.getChild("right_arm");
		leftArm = body.getChild("left_arm");
	}

	public ModelPart body() {
		return body;
	}

	public ModelPart head() {
		return head;
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, BOTTOM, 0.0F));
		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
		// The tendril curls up from the top of the pumpkin, a leaf at its tip.
		head.addOrReplaceChild("tendril", CubeListBuilder.create()
				.texOffs(12, 0).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F)
				.texOffs(16, 0).addBox(0.5F, -4.5F, -1.5F, 3.0F, 0.0F, 3.0F),
				PartPose.offsetAndRotation(0.5F, -PUMPKIN, 0.5F, 0.0F, 0.6F, 0.35F));
		// Arms: thin vines from the pumpkin's sides, hanging and curling out, a broad leaf for a hand.
		for (int side = -1; side <= 1; side += 2) {
			body.addOrReplaceChild(side < 0 ? "right_arm" : "left_arm", CubeListBuilder.create()
					.texOffs(8, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F)
					.texOffs(0, 8).addBox(-1.5F, 5.0F, -2.0F, 3.0F, 0.0F, 4.0F),
					PartPose.offsetAndRotation(side * PUMPKIN / 2.0F, -PUMPKIN / 2.0F, 0.0F, 0.0F, 0.0F, side * 0.35F));
		}
		// Legs: stubby twisted vines, a curl of root for a foot.
		for (int side = -1; side <= 1; side += 2) {
			root.addOrReplaceChild(side < 0 ? "right_leg" : "left_leg", CubeListBuilder.create()
					.texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F)
					.texOffs(0, 13).addBox(-1.5F, 4.0F, -2.5F, 3.0F, 1.0F, 3.0F),
					PartPose.offset(side * 2.0F, BOTTOM, 0.0F));
		}
		return LayerDefinition.create(mesh, 32, 32);
	}

	@Override
	public void setupAnim(PumpklingRenderer.State state) {
		super.setupAnim(state);
		float walk = state.walkAnimationPos;
		float speed = Math.min(1.0F, state.walkAnimationSpeed);
		float t = state.ageInTicks;
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.5F;
		tendril.zRot = 0.35F + 0.12F * Mth.sin(t * 0.08F);
		if (state.sitting) {
			// Squatting: down on its bottom, legs out in front, arms resting.
			body.y = BOTTOM + 4.0F;
			rightLeg.y = leftLeg.y = BOTTOM + 3.0F;
			rightLeg.xRot = leftLeg.xRot = -1.45F;
			rightArm.xRot = leftArm.xRot = -0.3F;
			return;
		}
		// Hopping: the whole of it bobs up twice a stride, legs and arms swinging opposite.
		float hop = Math.abs(Mth.sin(walk * 0.6F)) * 2.0F * speed;
		body.y = BOTTOM - hop;
		rightLeg.y = leftLeg.y = BOTTOM - hop * 0.5F;
		float swing = Mth.cos(walk * 0.6F) * 0.9F * speed;
		rightLeg.xRot = swing;
		leftLeg.xRot = -swing;
		rightArm.xRot = -swing * 0.8F + 0.05F * Mth.sin(t * 0.1F);
		leftArm.xRot = swing * 0.8F - 0.05F * Mth.sin(t * 0.1F);
	}
}
