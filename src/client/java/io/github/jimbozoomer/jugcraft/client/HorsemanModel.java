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
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/**
 * The Headless Horseman: a black horse with a flowing mane and tail, and on it a cloaked rider with nothing above
 * the collar but embers, holding up a jack o'lantern. Model space is in pixels, y down, the ground at 24, the front
 * toward -z. The texture's layout (128 x 64) is the {@code texOffs} below (texture: tools/night_textures.py).
 */
public class HorsemanModel extends EntityModel<LivingEntityRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Jugcraft.id("headless_horseman"), "main");

	private final ModelPart neck;
	private final ModelPart tail;
	private final ModelPart frontLeft;
	private final ModelPart frontRight;
	private final ModelPart backLeft;
	private final ModelPart backRight;
	private final ModelPart rightArm;
	private final ModelPart cloak;

	public HorsemanModel(ModelPart root) {
		super(root);
		neck = root.getChild("neck");
		tail = root.getChild("tail");
		frontLeft = root.getChild("front_left_leg");
		frontRight = root.getChild("front_right_leg");
		backLeft = root.getChild("back_left_leg");
		backRight = root.getChild("back_right_leg");
		ModelPart torso = root.getChild("torso");
		rightArm = torso.getChild("right_arm");
		cloak = torso.getChild("cloak");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// The horse.
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -5.0F, -11.0F, 10.0F, 10.0F, 22.0F),
				PartPose.offset(0.0F, 6.0F, 0.0F));
		PartDefinition neck = root.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(64, 0).addBox(-3.0F, -14.0F, -4.0F, 6.0F, 14.0F, 7.0F),
				PartPose.offsetAndRotation(0.0F, 4.0F, -9.0F, 0.5236F, 0.0F, 0.0F));
		neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(90, 0).addBox(-3.0F, -4.0F, -10.0F, 6.0F, 6.0F, 12.0F),
				PartPose.offset(0.0F, -13.0F, 0.0F));
		neck.addOrReplaceChild("mane", CubeListBuilder.create().texOffs(64, 22).addBox(-1.0F, -15.0F, 2.0F, 2.0F, 16.0F, 4.0F),
				PartPose.ZERO);
		root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(76, 22).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 14.0F, 4.0F),
				PartPose.offsetAndRotation(0.0F, 2.0F, 10.0F, 0.6F, 0.0F, 0.0F));
		CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 13.0F, 4.0F);
		root.addOrReplaceChild("front_left_leg", leg, PartPose.offset(3.0F, 11.0F, -8.0F));
		root.addOrReplaceChild("front_right_leg", leg, PartPose.offset(-3.0F, 11.0F, -8.0F));
		root.addOrReplaceChild("back_left_leg", leg, PartPose.offset(3.0F, 11.0F, 8.0F));
		root.addOrReplaceChild("back_right_leg", leg, PartPose.offset(-3.0F, 11.0F, 8.0F));
		// The rider, astride, headless.
		CubeListBuilder riderLeg = CubeListBuilder.create().texOffs(16, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F);
		root.addOrReplaceChild("right_rider_leg", riderLeg, PartPose.offsetAndRotation(-6.5F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F));
		root.addOrReplaceChild("left_rider_leg", riderLeg, PartPose.offsetAndRotation(6.5F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F));
		PartDefinition torso = root.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(32, 32).addBox(-4.0F, -12.0F, -2.0F, 8.0F, 12.0F, 4.0F),
				PartPose.offset(0.0F, 1.0F, 1.0F));
		torso.addOrReplaceChild("collar", CubeListBuilder.create().texOffs(90, 36).addBox(-4.0F, -14.0F, -2.5F, 8.0F, 2.0F, 5.0F), PartPose.ZERO);
		torso.addOrReplaceChild("cloak", CubeListBuilder.create().texOffs(90, 18).addBox(-4.5F, 0.0F, 0.0F, 9.0F, 15.0F, 1.0F),
				PartPose.offsetAndRotation(0.0F, -12.0F, 2.0F, 0.15F, 0.0F, 0.0F));
		PartDefinition arm = torso.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(56, 42).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
				PartPose.offsetAndRotation(-6.0F, -10.0F, 0.0F, -2.4F, 0.0F, 0.0F));
		arm.addOrReplaceChild("lantern", CubeListBuilder.create().texOffs(72, 44).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 5.0F, 5.0F),
				PartPose.offset(0.0F, 10.0F, 0.0F));
		torso.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(56, 42).mirror().addBox(-2.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
				PartPose.offsetAndRotation(6.0F, -10.0F, 0.0F, -0.7F, 0.0F, 0.0F));
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(LivingEntityRenderState state) {
		super.setupAnim(state);
		float swing = Mth.cos(state.walkAnimationPos * 0.6662F) * 1.2F * state.walkAnimationSpeed;
		frontLeft.xRot = swing;
		backRight.xRot = swing;
		frontRight.xRot = -swing;
		backLeft.xRot = -swing;
		neck.xRot = 0.5236F + Mth.sin(state.walkAnimationPos * 0.6662F) * 0.08F * state.walkAnimationSpeed;
		tail.xRot = 0.6F + state.walkAnimationSpeed * 0.5F;
		tail.zRot = Mth.sin(state.ageInTicks * 0.12F) * 0.15F;
		rightArm.zRot = Mth.sin(state.ageInTicks * 0.08F) * 0.06F;
		cloak.xRot = 0.15F + state.walkAnimationSpeed * 0.6F;
	}
}
