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
 * A wild turkey: a deep round body with a full breast, folded wings, long legs with spread toes, a bare neck and small
 * head with its beak, a wattle under it and (on a tom) the snood hanging over it; and a tail of {@value #FEATHERS}
 * feathers. Walking, its legs stride and its head bobs. Folded, the tail lies back along the line of the body; strutting,
 * a tom's tail rises and spreads into a fan, his breast puffs out, his wings droop to the ground and his neck draws back.
 * A hen's tail is shorter and never fans. Texture: tools/turkey_textures.py, whose box layout matches this one (64 by 64).
 */
public class TurkeyModel extends EntityModel<TurkeyRenderer.State> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Jugcraft.id("turkey"), "main");
	public static final int FEATHERS = 9;

	private final ModelPart body;
	private final ModelPart breast;
	private final ModelPart neck;
	private final ModelPart head;
	private final ModelPart snood;
	private final ModelPart wattle;
	private final ModelPart rightWing;
	private final ModelPart leftWing;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;
	private final ModelPart[] feathers = new ModelPart[FEATHERS];

	public TurkeyModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		breast = body.getChild("breast");
		neck = body.getChild("neck");
		head = neck.getChild("head");
		snood = head.getChild("snood");
		wattle = head.getChild("wattle");
		rightWing = body.getChild("right_wing");
		leftWing = body.getChild("left_wing");
		rightLeg = root.getChild("right_leg");
		leftLeg = root.getChild("left_leg");
		ModelPart tail = body.getChild("tail");
		for (int i = 0; i < FEATHERS; i++) {
			feathers[i] = tail.getChild("feather_" + i);
		}
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// The body, 8 wide, 7 deep and 10 long, its underside 7 pixels off the ground; the breast juts out in front.
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -5.0F, 8.0F, 7.0F, 10.0F),
				PartPose.offset(0.0F, 14.0F, 1.0F));
		body.addOrReplaceChild("breast", CubeListBuilder.create().texOffs(0, 17).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 3.0F),
				PartPose.offset(0.0F, 0.0F, -5.0F));
		// The neck rises from the front of the body; the head on it, the beak forward, the wattle under and the snood over.
		PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(20, 17).addBox(-1.0F, -7.0F, -1.0F, 2.0F, 7.0F, 2.0F),
				PartPose.offset(0.0F, -2.0F, -6.0F));
		PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(28, 17).addBox(-1.5F, -3.0F, -2.5F, 3.0F, 3.0F, 4.0F)
				.texOffs(42, 17).addBox(-0.5F, -2.0F, -4.5F, 1.0F, 1.0F, 2.0F),
				PartPose.offset(0.0F, -7.0F, 0.0F));
		head.addOrReplaceChild("wattle", CubeListBuilder.create().texOffs(48, 17).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F),
				PartPose.offset(0.0F, -1.0F, -2.0F));
		head.addOrReplaceChild("snood", CubeListBuilder.create().texOffs(52, 17).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F),
				PartPose.offset(0.0F, -2.5F, -3.5F));
		// Wings folded against the sides.
		body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(36, 0).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 5.0F, 8.0F),
				PartPose.offset(-4.0F, -3.0F, -3.0F));
		body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(36, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 5.0F, 8.0F),
				PartPose.offset(4.0F, -3.0F, -3.0F));
		// The tail: feathers pivoting at the rump, each standing up from its quill.
		PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offset(0.0F, -2.0F, 5.0F));
		for (int i = 0; i < FEATHERS; i++) {
			tail.addOrReplaceChild("feather_" + i, CubeListBuilder.create().texOffs(16, 26).addBox(-1.0F, -12.0F, -0.5F, 2.0F, 12.0F, 1.0F),
					PartPose.offset(0.0F, 0.0F, 0.01F * i));
		}
		// Long legs, toes spread.
		root.addOrReplaceChild("right_leg", CubeListBuilder.create()
				.texOffs(0, 26).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 7.0F, 1.0F)
				.texOffs(4, 26).addBox(-1.5F, 6.0F, -2.5F, 3.0F, 1.0F, 3.0F),
				PartPose.offset(-2.0F, 17.0F, 1.0F));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create()
				.texOffs(0, 26).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 7.0F, 1.0F)
				.texOffs(4, 26).addBox(-1.5F, 6.0F, -2.5F, 3.0F, 1.0F, 3.0F),
				PartPose.offset(2.0F, 17.0F, 1.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(TurkeyRenderer.State state) {
		super.setupAnim(state);
		float walk = state.walkAnimationPos;
		float speed = Math.min(1.0F, state.walkAnimationSpeed);
		float fan = state.tom ? state.fan : 0.0F;
		rightLeg.xRot = Mth.cos(walk * 0.6662F) * 1.3F * speed;
		leftLeg.xRot = Mth.cos(walk * 0.6662F + Mth.PI) * 1.3F * speed;
		// The head bobs as it walks, and turns and looks as any mob's does; strutting, the neck draws back.
		neck.xRot = 0.15F + 0.25F * Mth.sin(walk * 1.3324F) * speed - 0.45F * fan;
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD - neck.xRot;
		snood.visible = state.tom;
		wattle.yScale = state.tom ? 1.0F : 0.5F;
		// Strutting: the body tips back, the breast puffs, the wings droop to drag on the ground.
		body.xRot = -0.15F * fan;
		breast.yScale = 1.0F + 0.25F * fan;
		breast.zScale = 1.0F + 0.5F * fan;
		rightWing.zRot = 0.35F * fan;
		leftWing.zRot = -0.35F * fan;
		rightWing.xRot = -0.2F * fan;
		leftWing.xRot = -0.2F * fan;
		// The tail: folded back along the body (shorter on a hen), or raised and spread in a fan.
		for (int i = 0; i < FEATHERS; i++) {
			float spread = i - (FEATHERS - 1) / 2.0F;
			ModelPart feather = feathers[i];
			feather.xRot = Mth.lerp(fan, -1.35F, -0.12F);
			feather.zRot = spread * Mth.lerp(fan, 0.05F, 0.33F);
			feather.yScale = state.tom ? 1.0F : 0.6F;
		}
	}
}
