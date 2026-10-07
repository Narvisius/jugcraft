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
 * A werewolf, hunched and huge: a deep barrel chest leaning forward over a narrow waist, a shaggy hump of mane across
 * the shoulders and a ruff at the throat; a wolf's head thrust forward (a long muzzle with a black nose, fangs over an
 * open jaw, cheek tufts, tall pricked ears); long heavy arms, upper arm, forearm and a hand of four hooked claws, hanging
 * to below its knees; legs bent like a wolf's hind legs (thigh forward, shin back, a long hock forward again onto a
 * clawed paw); and a bushy tail in three parts drooping behind. It lopes with its arms swinging; hunting, it hunches
 * lower, raises its claws and snaps. The same parts and boxes are in tools/werewolf_model.py (the checker compares them),
 * from which tools/werewolf_textures.py paints one 128 by 128 texture for each kind of werewolf.
 */
public class WerewolfModel extends EntityModel<WerewolfRenderer.State> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Jugcraft.id("werewolf"), "main");
	/** How far forward the body leans standing, and hunting. */
	private static final float LEAN = 0.5F;
	private static final float HUNT_LEAN = 0.75F;
	private static final float HIP = 4.7F;
	private static final float THIGH = -0.5F;
	private static final float SHIN = 1.15F;
	private static final float HOCK = -1.0F;
	private static final float PAW = 0.35F;

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart rightEar;
	private final ModelPart leftEar;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightForearm;
	private final ModelPart leftForearm;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;
	private final ModelPart rightShin;
	private final ModelPart leftShin;
	private final ModelPart rightHock;
	private final ModelPart leftHock;
	private final ModelPart rightPaw;
	private final ModelPart leftPaw;
	private final ModelPart tail;
	private final ModelPart tailMid;
	private final ModelPart tailTip;

	public WerewolfModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		head = body.getChild("head");
		jaw = head.getChild("jaw");
		rightEar = head.getChild("right_ear");
		leftEar = head.getChild("left_ear");
		rightArm = body.getChild("right_arm");
		leftArm = body.getChild("left_arm");
		rightForearm = rightArm.getChild("forearm");
		leftForearm = leftArm.getChild("forearm");
		rightLeg = root.getChild("right_leg");
		leftLeg = root.getChild("left_leg");
		rightShin = rightLeg.getChild("shin");
		leftShin = leftLeg.getChild("shin");
		rightHock = rightShin.getChild("hock");
		leftHock = leftShin.getChild("hock");
		rightPaw = rightHock.getChild("paw");
		leftPaw = leftHock.getChild("paw");
		tail = body.getChild("tail");
		tailMid = tail.getChild("mid");
		tailTip = tailMid.getChild("tip");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// The body, pivoting at the hips: waist, barrel chest, the hump of mane over the shoulders, the ruff at the throat.
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 23).addBox(-5.0F, -7.0F, -4.0F, 10.0F, 7.0F, 8.0F)
				.texOffs(0, 0).addBox(-7.0F, -18.0F, -5.5F, 14.0F, 12.0F, 11.0F)
				.texOffs(50, 0).addBox(-7.5F, -21.0F, -2.5F, 15.0F, 7.0F, 9.0F)
				.texOffs(0, 38).addBox(-6.0F, -19.0F, -8.0F, 12.0F, 5.0F, 5.0F),
				PartPose.offsetAndRotation(0.0F, HIP, 1.0F, LEAN, 0.0F, 0.0F));

		// The head, thrust forward from the top of the chest: skull, long muzzle, cheek tufts, upper fangs.
		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(50, 16).addBox(-4.5F, -8.0F, -7.0F, 9.0F, 8.0F, 8.0F)
				.texOffs(84, 17).addBox(-3.0F, -5.0F, -14.0F, 6.0F, 3.0F, 7.0F)
				.texOffs(106, 0).addBox(-5.5F, -5.0F, -6.0F, 2.0F, 4.0F, 4.0F)
				.texOffs(106, 0).mirror().addBox(3.5F, -5.0F, -6.0F, 2.0F, 4.0F, 4.0F).mirror(false)
				.texOffs(118, 0).addBox(-2.7F, -2.0F, -13.8F, 1.0F, 2.0F, 1.0F)
				.texOffs(118, 0).addBox(1.7F, -2.0F, -13.8F, 1.0F, 2.0F, 1.0F),
				PartPose.offsetAndRotation(0.0F, -19.0F, -6.0F, -LEAN, 0.0F, 0.0F));
		// The jaw, hinged under the back of the muzzle and hanging open, its lower fangs pointing up.
		head.addOrReplaceChild("jaw", CubeListBuilder.create()
				.texOffs(98, 8).addBox(-2.5F, 0.0F, -6.5F, 5.0F, 2.0F, 7.0F)
				.texOffs(122, 4).addBox(-2.3F, -1.5F, -6.3F, 1.0F, 2.0F, 1.0F)
				.texOffs(122, 4).addBox(1.3F, -1.5F, -6.3F, 1.0F, 2.0F, 1.0F),
				PartPose.offsetAndRotation(0.0F, -2.0F, -7.5F, 0.35F, 0.0F, 0.0F));
		// Tall pointed ears, splayed a little.
		for (int side = -1; side <= 1; side += 2) {
			boolean left = side > 0;
			head.addOrReplaceChild(left ? "left_ear" : "right_ear", CubeListBuilder.create().mirror(left)
					.texOffs(98, 0).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F)
					.texOffs(118, 3).addBox(-0.5F, -6.5F, -0.5F, 1.0F, 2.0F, 1.0F),
					PartPose.offsetAndRotation(side * 2.8F, -7.5F, -2.0F, -0.1F, 0.0F, side * 0.2F));
		}

		// Arms: a heavy shoulder and upper arm, the forearm from the elbow, and a hand of four claws.
		for (int side = -1; side <= 1; side += 2) {
			boolean left = side > 0;
			PartDefinition arm = body.addOrReplaceChild(left ? "left_arm" : "right_arm", CubeListBuilder.create().mirror(left)
					.texOffs(36, 32).addBox(-3.0F, -3.0F, -3.5F, 6.0F, 6.0F, 7.0F)
					.texOffs(62, 32).addBox(-2.5F, 2.0F, -2.5F, 5.0F, 10.0F, 5.0F),
					PartPose.offset(side * 8.5F, -16.0F, -2.5F));
			PartDefinition forearm = arm.addOrReplaceChild("forearm", CubeListBuilder.create().mirror(left)
					.texOffs(82, 32).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 10.0F, 5.0F), PartPose.offset(0.0F, 11.5F, 0.0F));
			forearm.addOrReplaceChild("hand", CubeListBuilder.create().mirror(left)
					.texOffs(102, 32).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 3.0F, 5.0F)
					.texOffs(122, 0).addBox(-2.5F, 2.0F, -2.7F, 1.0F, 3.0F, 1.0F)
					.texOffs(122, 0).addBox(-1.25F, 2.0F, -2.7F, 1.0F, 3.0F, 1.0F)
					.texOffs(122, 0).addBox(0.0F, 2.0F, -2.7F, 1.0F, 3.0F, 1.0F)
					.texOffs(122, 0).addBox(1.25F, 2.0F, -2.7F, 1.0F, 3.0F, 1.0F),
					PartPose.offset(0.0F, 9.5F, 0.0F));
		}

		// Legs bent like a wolf's hind legs: thigh forward, shin back, the long hock forward again, a clawed paw.
		for (int side = -1; side <= 1; side += 2) {
			boolean left = side > 0;
			PartDefinition thigh = root.addOrReplaceChild(left ? "left_leg" : "right_leg", CubeListBuilder.create().mirror(left)
					.texOffs(0, 48).addBox(-3.0F, -1.0F, -3.5F, 6.0F, 9.0F, 7.0F),
					PartPose.offsetAndRotation(side * 4.5F, HIP, 2.0F, THIGH, 0.0F, 0.0F));
			PartDefinition shin = thigh.addOrReplaceChild("shin", CubeListBuilder.create().mirror(left)
					.texOffs(26, 48).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 7.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, SHIN, 0.0F, 0.0F));
			PartDefinition hock = shin.addOrReplaceChild("hock", CubeListBuilder.create().mirror(left)
					.texOffs(46, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 6.5F, 0.0F, HOCK, 0.0F, 0.0F));
			hock.addOrReplaceChild("paw", CubeListBuilder.create().mirror(left)
					.texOffs(62, 48).addBox(-2.5F, -0.5F, -4.5F, 5.0F, 2.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, PAW, 0.0F, 0.0F));
		}

		// The tail: base, a bushy middle and a tip, drooping behind.
		PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(84, 48).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 5.0F),
				PartPose.offsetAndRotation(0.0F, -1.0F, 4.5F, -1.2F, 0.0F, 0.0F));
		PartDefinition mid = tail.addOrReplaceChild("mid", CubeListBuilder.create().texOffs(100, 48).addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 7.0F),
				PartPose.offsetAndRotation(0.0F, 0.0F, 4.5F, 0.15F, 0.0F, 0.0F));
		mid.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(0, 64).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 6.0F),
				PartPose.offsetAndRotation(0.0F, 0.0F, 6.5F, 0.15F, 0.0F, 0.0F));
		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(WerewolfRenderer.State state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float walk = state.walkAnimationPos;
		float speed = Math.min(1.0F, state.walkAnimationSpeed);
		float lean = state.hunting ? HUNT_LEAN : LEAN;
		body.xRot = lean + 0.04F * Mth.sin(t * 0.08F);
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD - lean * 0.85F;
		jaw.xRot = state.hunting ? 0.55F + 0.2F * Mth.sin(t * 0.7F) : 0.32F + 0.04F * Mth.sin(t * 0.1F);
		// The ears twitch now and then.
		float twitch = Mth.sin(t * 0.05F) > 0.9F ? 0.15F * Mth.sin(t * 1.3F) : 0.0F;
		rightEar.zRot = -0.2F + twitch;
		leftEar.zRot = 0.2F - twitch;

		// Legs: the thighs swing; shin, hock and paw follow so the paw lands flat.
		float stride = Mth.cos(walk * 0.6662F) * 0.8F * speed;
		rightLeg.xRot = THIGH + stride;
		leftLeg.xRot = THIGH - stride;
		float flex = 0.25F * speed;
		rightShin.xRot = SHIN + Math.max(0.0F, -Mth.sin(walk * 0.6662F)) * flex * 2.0F;
		leftShin.xRot = SHIN + Math.max(0.0F, Mth.sin(walk * 0.6662F)) * flex * 2.0F;
		rightHock.xRot = HOCK;
		leftHock.xRot = HOCK;
		rightPaw.xRot = PAW - stride - (rightShin.xRot - SHIN);
		leftPaw.xRot = PAW + stride - (leftShin.xRot - SHIN);

		// Arms hang forward and down from the leaning shoulders, swinging as it lopes; hunting, they rise, claws out.
		float swing = Mth.cos(walk * 0.6662F + Mth.PI) * 0.7F * speed;
		if (state.hunting) {
			float rake = 0.3F * Mth.sin(t * 0.5F);
			rightArm.xRot = -1.25F - lean * 0.4F + rake;
			leftArm.xRot = -1.25F - lean * 0.4F - rake;
			rightForearm.xRot = -0.55F;
			leftForearm.xRot = -0.55F;
		} else {
			rightArm.xRot = -lean * 0.75F + swing;
			leftArm.xRot = -lean * 0.75F - swing;
			rightForearm.xRot = -0.3F;
			leftForearm.xRot = -0.3F;
		}
		rightArm.zRot = 0.1F;
		leftArm.zRot = -0.1F;

		// The tail keeps its droop however far it hunches.
		tail.xRot = -0.7F - lean + 0.08F * Mth.sin(t * 0.1F);
		tail.yRot = 0.3F * Mth.sin(walk * 0.6662F) * speed + 0.1F * Mth.sin(t * 0.07F);
		tailMid.yRot = tail.yRot * 0.6F;
		tailTip.yRot = tail.yRot * 0.4F;
	}
}
