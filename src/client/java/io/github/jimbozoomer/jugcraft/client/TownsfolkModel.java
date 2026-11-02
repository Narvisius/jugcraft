package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.town.TownRoutine;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * A townsperson's body: the same boxes as a player's (classic arms), with the outer layer (hat, jacket, sleeves,
 * trousers) on each part so the town's skins (tools/town_skins.py, in the player skin layout) show hats, hoods, collars
 * and aprons in relief. It moves as any humanoid does, and at their place each resident is posed for what they do there
 * ({@link TownRoutine.Activity}): kneading, counting, tending, sweeping, carrying, lighting lamps, fiddling, keeping
 * watch, preaching, holding forth, chatting, raising a mug, or dozing at the counter. Poses fade out as they walk off.
 */
public class TownsfolkModel extends HumanoidModel<TownsfolkRenderer.State> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Jugcraft.id("townsfolk"), "main");
	/**
	 * The townsfolk's own layer ({@link TownsfolkRenderer}): the same body, with classic and slim arms to choose between
	 * per person, for custom NPCs wearing a player's skin ({@link #createSkinnableLayer()}).
	 */
	public static final ModelLayerLocation SKINNABLE_LAYER = new ModelLayerLocation(Jugcraft.id("townsfolk"), "skinnable");

	/** The arms' classic and slim versions (null in {@link #createLayer()}, which has classic arms only). */
	private final @Nullable ModelPart rightWide, rightSlim, leftWide, leftSlim;

	public TownsfolkModel(ModelPart root) {
		super(root);
		boolean skinnable = rightArm.hasChild("wide");
		rightWide = skinnable ? rightArm.getChild("wide") : null;
		rightSlim = skinnable ? rightArm.getChild("slim") : null;
		leftWide = skinnable ? leftArm.getChild("wide") : null;
		leftSlim = skinnable ? leftArm.getChild("slim") : null;
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		CubeDeformation none = CubeDeformation.NONE;
		CubeDeformation outer = new CubeDeformation(0.25F);
		PartDefinition head = root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, none), PartPose.ZERO);
		head.addOrReplaceChild("hat",
				CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F)), PartPose.ZERO);
		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, none), PartPose.ZERO);
		body.addOrReplaceChild("jacket",
				CubeListBuilder.create().texOffs(16, 32).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, outer), PartPose.ZERO);
		PartDefinition rightArm = root.addOrReplaceChild("right_arm",
				CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, none), PartPose.offset(-5.0F, 2.0F, 0.0F));
		rightArm.addOrReplaceChild("right_sleeve",
				CubeListBuilder.create().texOffs(40, 32).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, outer), PartPose.ZERO);
		PartDefinition leftArm = root.addOrReplaceChild("left_arm",
				CubeListBuilder.create().texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, none), PartPose.offset(5.0F, 2.0F, 0.0F));
		leftArm.addOrReplaceChild("left_sleeve",
				CubeListBuilder.create().texOffs(48, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, outer), PartPose.ZERO);
		PartDefinition rightLeg = root.addOrReplaceChild("right_leg",
				CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, none), PartPose.offset(-1.9F, 12.0F, 0.0F));
		rightLeg.addOrReplaceChild("right_pants",
				CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, outer), PartPose.ZERO);
		PartDefinition leftLeg = root.addOrReplaceChild("left_leg",
				CubeListBuilder.create().texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, none), PartPose.offset(1.9F, 12.0F, 0.0F));
		leftLeg.addOrReplaceChild("left_pants",
				CubeListBuilder.create().texOffs(0, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, outer), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 64);
	}

	/**
	 * {@link #createLayer()} with both classic (4 px) and slim (3 px) arms, as a player model has; the classic ones show
	 * unless the state says slim ({@code TownsfolkRenderer.State.slim}).
	 */
	public static LayerDefinition createSkinnableLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		CubeDeformation none = CubeDeformation.NONE;
		CubeDeformation outer = new CubeDeformation(0.25F);
		PartDefinition head = root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, none), PartPose.ZERO);
		head.addOrReplaceChild("hat",
				CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F)), PartPose.ZERO);
		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, none), PartPose.ZERO);
		body.addOrReplaceChild("jacket",
				CubeListBuilder.create().texOffs(16, 32).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, outer), PartPose.ZERO);
		PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
		rightArm.addOrReplaceChild("wide",
				CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, none), PartPose.ZERO)
				.addOrReplaceChild("right_sleeve",
						CubeListBuilder.create().texOffs(40, 32).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, outer), PartPose.ZERO);
		rightArm.addOrReplaceChild("slim",
				CubeListBuilder.create().texOffs(40, 16).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, none), PartPose.offset(0.0F, 0.5F, 0.0F))
				.addOrReplaceChild("right_sleeve",
						CubeListBuilder.create().texOffs(40, 32).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, outer), PartPose.ZERO);
		PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
		leftArm.addOrReplaceChild("wide",
				CubeListBuilder.create().texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, none), PartPose.ZERO)
				.addOrReplaceChild("left_sleeve",
						CubeListBuilder.create().texOffs(48, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, outer), PartPose.ZERO);
		leftArm.addOrReplaceChild("slim",
				CubeListBuilder.create().texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, none), PartPose.offset(0.0F, 0.5F, 0.0F))
				.addOrReplaceChild("left_sleeve",
						CubeListBuilder.create().texOffs(48, 48).addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, outer), PartPose.ZERO);
		PartDefinition rightLeg = root.addOrReplaceChild("right_leg",
				CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, none), PartPose.offset(-1.9F, 12.0F, 0.0F));
		rightLeg.addOrReplaceChild("right_pants",
				CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, outer), PartPose.ZERO);
		PartDefinition leftLeg = root.addOrReplaceChild("left_leg",
				CubeListBuilder.create().texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, none), PartPose.offset(1.9F, 12.0F, 0.0F));
		leftLeg.addOrReplaceChild("left_pants",
				CubeListBuilder.create().texOffs(0, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, outer), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(TownsfolkRenderer.State state) {
		super.setupAnim(state);
		if (rightWide != null && rightSlim != null && leftWide != null && leftSlim != null) {
			// Set every frame: one model draws everyone.
			rightWide.visible = leftWide.visible = !state.slim;
			rightSlim.visible = leftSlim.visible = state.slim;
		}
		if (state.activity == TownRoutine.Activity.NONE) {
			return;
		}
		// Full pose standing still, none at a walk (the server only says what they do while they stand at their place,
		// but a step can begin before it says they stopped).
		float w = 1.0F - Mth.clamp(state.walkAnimationSpeed * 3.0F, 0.0F, 1.0F);
		float t = state.ageInTicks + state.phase * 20.0F;
		switch (state.activity) {
			case SERVE -> {
				float gesture = Math.max(0.0F, Mth.sin(t * 0.08F)) * 0.35F;
				pose(rightArm, w, -0.45F - gesture, -0.25F, 0.0F);
				pose(leftArm, w, -0.45F, 0.25F, 0.0F);
			}
			case KNEAD -> {
				float press = Mth.sin(t * 0.45F) * 0.22F;
				pose(rightArm, w, -0.95F + press, -0.3F, 0.0F);
				pose(leftArm, w, -0.95F - press, 0.3F, 0.0F);
				head.xRot = Mth.lerp(w, head.xRot, 0.45F);
			}
			case COUNT -> {
				pose(rightArm, w, -0.7F - Math.abs(Mth.sin(t * 0.35F)) * 0.25F, -0.2F, 0.0F);
				pose(leftArm, w, -0.6F, 0.25F, 0.0F);
				head.xRot = Mth.lerp(w, head.xRot, 0.35F);
			}
			case TEND -> {
				pose(rightArm, w, -0.9F + Mth.sin(t * 0.2F) * 0.2F, -0.1F, 0.0F);
				pose(leftArm, w, -0.7F, 0.1F, 0.0F);
				head.xRot = Mth.lerp(w, head.xRot, 0.5F);
			}
			case SWEEP -> {
				float sway = Mth.sin(t * 0.22F) * 0.45F;
				pose(rightArm, w, -0.55F, -0.35F + sway, 0.0F);
				pose(leftArm, w, -0.75F, 0.35F + sway, 0.0F);
				head.xRot = Mth.lerp(w, head.xRot, 0.3F);
			}
			case CARRY -> {
				float bob = Mth.sin(t * 0.3F) * 0.04F;
				pose(rightArm, w, -1.15F + bob, -0.18F, 0.0F);
				pose(leftArm, w, -1.15F + bob, 0.18F, 0.0F);
			}
			case LIGHT -> {
				pose(rightArm, w, -2.7F + Mth.sin(t * 0.15F) * 0.08F, 0.0F, 0.1F);
				pose(leftArm, w, -0.2F, 0.0F, -0.1F);
				head.xRot = Mth.lerp(w, head.xRot, -0.55F);
			}
			case PLAY -> {
				pose(leftArm, w, -1.95F, 0.55F, 0.0F);
				pose(rightArm, w, -1.15F, -0.45F + Mth.sin(t * 0.55F) * 0.35F, 0.0F);
				head.xRot = Mth.lerp(w, head.xRot, 0.15F);
				head.zRot = Mth.lerp(w, head.zRot, 0.22F);
			}
			case WATCH -> {
				pose(leftArm, w, 0.45F, 0.0F, 0.1F);
				pose(rightArm, w, -0.15F, 0.0F, 0.05F);
				head.yRot = Mth.lerp(w, head.yRot, head.yRot + Mth.sin(t * 0.025F) * 0.6F);
			}
			case PREACH -> {
				pose(rightArm, w, -0.85F, -0.35F, 0.0F);
				pose(leftArm, w, -0.5F - Math.max(0.0F, Mth.sin(t * 0.05F)) * 1.5F, 0.15F, -0.15F);
				head.xRot = Mth.lerp(w, head.xRot, 0.1F);
			}
			case ADDRESS -> {
				pose(rightArm, w, -1.0F + Mth.sin(t * 0.08F) * 0.35F, 0.0F, 0.45F);
				pose(leftArm, w, -0.25F, 0.0F, -0.55F);
			}
			case CHAT -> {
				float a = Math.max(0.0F, Mth.sin(t * 0.12F)), b = Math.max(0.0F, Mth.sin(t * 0.12F + 2.5F));
				pose(rightArm, w, -0.35F - a * 0.7F, -0.15F, 0.05F);
				pose(leftArm, w, -0.35F - b * 0.6F, 0.15F, -0.05F);
				head.xRot = Mth.lerp(w, head.xRot, head.xRot + Mth.sin(t * 0.09F) * 0.07F);
			}
			case TOAST -> {
				float raise = Math.max(0.0F, Mth.sin(t * 0.06F));
				pose(rightArm, w, -1.35F - raise * 1.4F, -0.1F, 0.1F);
				pose(leftArm, w, -0.3F, 0.15F, -0.05F);
				head.xRot = Mth.lerp(w, head.xRot, -0.2F * raise);
			}
			case DOZE -> {
				pose(rightArm, w, -0.85F, -0.55F, 0.0F);
				pose(leftArm, w, -0.85F, 0.55F, 0.0F);
				head.xRot = Mth.lerp(w, head.xRot, 0.65F + Mth.sin(t * 0.05F) * 0.04F);
			}
			default -> {
			}
		}
	}

	/** Moves a limb from its walking pose towards a held one by {@code w}. */
	private static void pose(ModelPart part, float w, float x, float y, float z) {
		part.xRot = Mth.lerp(w, part.xRot, x);
		part.yRot = Mth.lerp(w, part.yRot, y);
		part.zRot = Mth.lerp(w, part.zRot, z);
	}
}
