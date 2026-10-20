package io.github.jimbozoomer.jugcraft.client.chima;

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
 * Chima, the lion cub (docs/features/chima.md), drawn from its designer's sketches: nearly all head, a great round head
 * with a spiky mane all round it (squares stood on their corners, poking out of it at uneven angles and a few out of the
 * back), the face on its front; a small yellow body on four stubby legs; a thin tail curling up to a spiky little star.
 * It trots with its legs swinging and its head bobbing, wags its tail, looks about with its whole head, and sits with its
 * hind legs tucked forward.
 *
 * <p>Its five looks (its own, pumpkin, zombie, skeleton and shark) are textures on this one model, drawn at twice the
 * model's resolution by tools/chima_textures.py, whose box layout matches this one (128 by 64 texture units). A look
 * without spikes (the round pumpkin, skull and shark), stem (all but the pumpkin) or tail ball leaves them transparent.
 */
public class ChimaModel extends EntityModel<ChimaRenderer.State> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Jugcraft.id("chima"), "main");
	/** The middle of the head, above the neck. */
	private static final float HEAD_MIDDLE = -6.0F;
	/** The mane's spikes round the face: angle clockwise from straight up as you face it, size, reach from the middle. */
	private static final float[][] SPIKES = {{0, 5.5F, 6.6F}, {36, 5.0F, 6.9F}, {74, 5.5F, 7.0F}, {112, 4.5F, 6.9F}, {150, 4.5F, 6.6F},
			{210, 4.5F, 6.6F}, {248, 5.0F, 6.9F}, {286, 5.5F, 7.0F}, {324, 5.0F, 6.9F}};

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart tail;
	private final ModelPart tailEnd;
	private final ModelPart frontRight;
	private final ModelPart frontLeft;
	private final ModelPart backRight;
	private final ModelPart backLeft;

	public ChimaModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		head = root.getChild("head");
		tail = body.getChild("tail");
		tailEnd = tail.getChild("tail_end");
		frontRight = root.getChild("front_right");
		frontLeft = root.getChild("front_left");
		backRight = root.getChild("back_right");
		backLeft = root.getChild("back_left");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 37).addBox(-3.5F, -2.5F, -4.5F, 7.0F, 5.0F, 9.0F),
				PartPose.offset(0.0F, 18.5F, 0.0F));
		// The tail rises from the rump in two lengths, ending in a ball (the pumpkin's lantern, the zombie's stitched ball)
		// with two cubes turned through it for the star.
		PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(44, 37).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 5.0F),
				PartPose.offsetAndRotation(0.0F, -1.5F, 4.5F, 0.9F, 0.0F, 0.0F));
		PartDefinition tailEnd = tail.addOrReplaceChild("tail_end", CubeListBuilder.create().texOffs(44, 43).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 5.0F),
				PartPose.offsetAndRotation(0.0F, 0.0F, 4.8F, 0.75F, 0.0F, 0.0F));
		PartDefinition tuft = tailEnd.addOrReplaceChild("tuft", CubeListBuilder.create().texOffs(56, 37).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F),
				PartPose.offset(0.0F, 0.0F, 6.0F));
		tuft.addOrReplaceChild("tuft_side", CubeListBuilder.create().texOffs(56, 43).addBox(-1.75F, -1.75F, -1.75F, 3.5F, 3.5F, 3.5F),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, Mth.PI / 4.0F, 0.0F, 0.0F));
		tuft.addOrReplaceChild("tuft_back", CubeListBuilder.create().texOffs(56, 43).addBox(-1.75F, -1.75F, -1.75F, 3.5F, 3.5F, 3.5F),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, Mth.PI / 4.0F, 0.0F));
		// The head is rounded from three boxes: the face's own, one wider and one taller.
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-6.0F, -12.0F, -4.5F, 12.0F, 12.0F, 9.0F)
				.texOffs(0, 21).addBox(-7.0F, -10.0F, -4.0F, 14.0F, 8.0F, 8.0F)
				.texOffs(44, 0).addBox(-4.0F, -13.0F, -4.0F, 8.0F, 14.0F, 8.0F),
				PartPose.offset(0.0F, 15.5F, -2.5F));
		for (int i = 0; i < SPIKES.length; i++) {
			float angle = SPIKES[i][0] * Mth.DEG_TO_RAD;
			float size = SPIKES[i][1];
			float reach = SPIKES[i][2];
			float x = reach * (float) Math.sin(angle);
			float y = HEAD_MIDDLE - reach * (float) Math.cos(angle);
			// A square turned so one corner points straight out from the middle of the head.
			float turn = (float) Math.atan2(-Math.cos(angle), Math.sin(angle)) - Mth.PI / 4.0F;
			// Spikes on its right (the left as you face it) have their own texture, as the zombie's two halves differ.
			int u = x < -0.01F || i == 0 ? 44 : 68;
			head.addOrReplaceChild("spike_" + i, CubeListBuilder.create().texOffs(u, 22).addBox(-size / 2.0F, -size / 2.0F, -3.0F, size, size, 6.0F),
					PartPose.offsetAndRotation(x, y, 0.0F, 0.0F, 0.0F, turn));
		}
		// Three more out of the back, seen from the side: a crest, a ruff and in the middle the shark's fin.
		head.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(44, 22).addBox(-1.5F, -2.5F, -2.5F, 3.0F, 5.0F, 5.0F),
				PartPose.offsetAndRotation(-2.5F, -11.5F, 2.5F, Mth.PI / 4.0F, 0.0F, 0.0F));
		head.addOrReplaceChild("ruff", CubeListBuilder.create().texOffs(68, 22).addBox(-1.5F, -2.5F, -2.5F, 3.0F, 5.0F, 5.0F),
				PartPose.offsetAndRotation(2.5F, -2.0F, 3.5F, Mth.PI / 4.0F, 0.0F, 0.0F));
		head.addOrReplaceChild("fin", CubeListBuilder.create().texOffs(76, 0).addBox(-0.75F, -2.5F, -2.5F, 1.5F, 5.0F, 5.0F),
				PartPose.offsetAndRotation(0.0F, -7.0F, 4.2F, Mth.PI / 4.0F, 0.0F, 0.0F));
		head.addOrReplaceChild("stem", CubeListBuilder.create().texOffs(90, 0).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F),
				PartPose.offsetAndRotation(0.5F, -13.0F, 0.0F, 0.0F, 0.0F, 0.25F));
		// Front and hind legs have their own texture, as some looks colour them apart (the shark's blue front, sandy back).
		for (int side = -1; side <= 1; side += 2) {
			root.addOrReplaceChild(side < 0 ? "front_right" : "front_left",
					CubeListBuilder.create().texOffs(32, 37).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offset(side * 2.0F, 21.0F, -2.5F));
			root.addOrReplaceChild(side < 0 ? "back_right" : "back_left",
					CubeListBuilder.create().texOffs(32, 43).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offset(side * 2.0F, 21.0F, 2.5F));
		}
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(ChimaRenderer.State state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float walk = state.walkAnimationPos;
		float speed = Math.min(1.0F, state.walkAnimationSpeed);
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD;
		// Trotting: diagonal legs together and the big head bobbing; the tail wags, faster as it runs.
		float swing = Mth.cos(walk * 0.9F) * 1.0F * speed;
		frontRight.xRot = backLeft.xRot = swing;
		frontLeft.xRot = backRight.xRot = -swing;
		head.y += Math.abs(Mth.sin(walk * 0.9F)) * 0.6F * speed;
		tail.yRot = Mth.sin(t * (0.15F + 0.25F * speed)) * 0.35F;
		tail.xRot += 0.12F * Mth.sin(t * 0.08F);
		tailEnd.xRot += 0.2F * Mth.sin(t * 0.1F + 1.0F);
		if (state.sitting) {
			// Sitting: rump down and front up, head lowered, hind legs folded forward under it, tail laid back.
			body.y += 1.5F;
			body.xRot = -0.3F;
			head.y += 1.0F;
			backRight.y += 1.5F;
			backLeft.y += 1.5F;
			backRight.xRot = backLeft.xRot = -Mth.PI / 2.0F;
			frontRight.xRot = frontLeft.xRot = 0.0F;
			tail.xRot = 1.3F;
		}
	}
}
