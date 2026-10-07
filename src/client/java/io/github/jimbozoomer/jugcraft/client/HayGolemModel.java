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
 * The Hay Golem: a straw man about two and a half blocks tall. A hay-bale body bound with twine, a straw collar, arms and
 * legs of straw bundles tied at the wrists and ankles with straw spilling out below, and a head (drawn by
 * {@link HayGolemRenderer}: the carved pumpkin it wears) sitting on the collar. It walks with a stiff swing of its
 * legs and arms, sways a little standing, and bends over a crop with both arms down while it harvests. Texture:
 * tools/hay_golem_textures.py, whose box layout matches this one (64 by 64).
 */
public class HayGolemModel extends EntityModel<HayGolemRenderer.State> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Jugcraft.id("hay_golem"), "main");
	/** Where the head sits, in model pixels (the feet are at 24): the top of the collar. */
	public static final float NECK = -4.0F;

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;

	public HayGolemModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		head = body.getChild("head");
		rightArm = body.getChild("right_arm");
		leftArm = body.getChild("left_arm");
		rightLeg = root.getChild("right_leg");
		leftLeg = root.getChild("left_leg");
	}

	/** The head's joint: the layer that draws the pumpkin hangs it from here. */
	public ModelPart head() {
		return head;
	}

	/** The body (and everything on it) pivots at the hips, 12 pixels up. */
	public ModelPart body() {
		return body;
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// The body: a hay bale 12 wide, 14 tall and 8 deep from the hips up, and a straw collar on top of it.
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-6.0F, -14.0F, -4.0F, 12.0F, 14.0F, 8.0F)
				.texOffs(0, 22).addBox(-5.0F, -16.0F, -3.0F, 10.0F, 2.0F, 6.0F),
				PartPose.offset(0.0F, 12.0F, 0.0F));
		// The head's joint, on top of the collar: the pumpkin sits on it.
		body.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, NECK - 12.0F, 0.0F));
		// Arms: straw bundles hanging from the shoulders, a tuft of straw spilling from each wrist.
		body.addOrReplaceChild("right_arm", CubeListBuilder.create()
				.texOffs(40, 0).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 13.0F, 4.0F)
				.texOffs(40, 18).addBox(-2.5F, 12.0F, -2.5F, 5.0F, 3.0F, 5.0F),
				PartPose.offset(-8.0F, -13.0F, 0.0F));
		body.addOrReplaceChild("left_arm", CubeListBuilder.create()
				.texOffs(40, 0).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 13.0F, 4.0F)
				.texOffs(40, 18).addBox(-2.5F, 12.0F, -2.5F, 5.0F, 3.0F, 5.0F),
				PartPose.offset(8.0F, -13.0F, 0.0F));
		// Legs: thicker bundles, tied at the ankle, straw splayed over the ground.
		root.addOrReplaceChild("right_leg", CubeListBuilder.create()
				.texOffs(0, 30).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 10.0F, 5.0F)
				.texOffs(20, 30).addBox(-3.0F, 10.0F, -3.0F, 6.0F, 2.0F, 6.0F),
				PartPose.offset(-3.0F, 12.0F, 0.0F));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create()
				.texOffs(0, 30).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 10.0F, 5.0F)
				.texOffs(20, 30).addBox(-3.0F, 10.0F, -3.0F, 6.0F, 2.0F, 6.0F),
				PartPose.offset(3.0F, 12.0F, 0.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(HayGolemRenderer.State state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float walk = state.walkAnimationPos;
		float speed = Math.min(1.0F, state.walkAnimationSpeed);
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD;
		// A stiff walk: legs swing, arms swing against them.
		rightLeg.xRot = Mth.cos(walk * 0.6662F) * 1.1F * speed;
		leftLeg.xRot = Mth.cos(walk * 0.6662F + Mth.PI) * 1.1F * speed;
		rightArm.xRot = Mth.cos(walk * 0.6662F + Mth.PI) * 0.7F * speed;
		leftArm.xRot = Mth.cos(walk * 0.6662F) * 0.7F * speed;
		// Standing, it sways and its arms hang a little out from its sides.
		body.zRot = 0.03F * Mth.sin(t * 0.05F);
		rightArm.zRot = 0.08F + 0.03F * Mth.sin(t * 0.07F);
		leftArm.zRot = -0.08F - 0.03F * Mth.sin(t * 0.07F);
		if (state.working) {
			// Bent over a crop, both arms down at it, pulling.
			body.xRot = 0.45F;
			float pull = 0.25F * Mth.sin(t * 0.9F);
			rightArm.xRot = -0.9F + pull;
			leftArm.xRot = -0.9F - pull;
		}
	}
}
