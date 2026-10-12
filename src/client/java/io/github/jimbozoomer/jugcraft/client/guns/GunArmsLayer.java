package io.github.jimbozoomer.jugcraft.client.guns;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.GeoLocator;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.guns.GunItem;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The player's arms in their own first-person view of a gun, drawn at the gun model's right_arm and left_arm bones
 * (tools/guns.py places them; the owner's animations move them). Each bone's pivot is the hand; the arm runs from it
 * toward the bone's "&lt;side&gt;_shoulder" locator (down, back and out from the gun), twelve pixels with the fist two
 * past the pivot. The arms are the player model's own (the skin's arm and sleeve, wide or slim).
 * <p>
 * The owner's animations were made for another mod's arms, and a few bring a hand so near the eye that an arm running
 * from it toward its shoulder comes within a hand's breadth of the camera, or holds it: drawn, the arm's sleeve or its
 * inside fills the screen (the Trench Lobber's pump, in a CI screenshot of 9 October 2026).
 * An arm that close is left out for those frames, as vanilla leaves out a thrown item just leaving the eye.
 * <p>
 * Slice 9F, the aiming polish: aiming slides the gun's sight onto the middle of the screen, which brings the grip under
 * the eye, from a fifth of a block from it (the Coach Gun) to three quarters (the Rattler Pistol). There the fist, four
 * pixels across, covered the lower middle of the view, and on the guns whose grip comes nearest, half the screen. So
 * the arms shrink about the hands as the aim comes in, to {@link #AIMED_SIZE} at full aim, and the gun's sight picture
 * stays clear. A gun without sights, which stays at the hip, keeps them as they are.
 * <p>
 * Slice 10G, two guns at once: a gun drawn in a left hand (the other hand's gun, or a left-handed player's) is the same
 * model, not its mirror image, so arms drawn as for the right hand ran in across the screen from it. There each arm is
 * the other side's, running out to its own shoulder: the shoulder's way mirrored across the gun. The other hand's gun
 * has only its grip hand's arm: the other arm holds the main gun.
 */
public final class GunArmsLayer extends GeoRenderLayer<GunItem, GeoItemRenderer.RenderData, GeoRenderState> {
	/** The player model's arm runs from y -2 (shoulder) to 10 (fist); this puts the fist 2 px past the bone's pivot. */
	private static final float FIST = 8.0F;
	/** The player model's arm box, in pixels: from y -2 to 10 and z -2 to 2, its sleeve a quarter pixel bigger all round. */
	private static final float SHOULDER_END = -2.0F;
	private static final float FIST_END = 10.0F;
	private static final float HALF_DEPTH = 2.0F;
	private static final float SLEEVE = 0.25F;
	/** An arm nearer the eye than this, in blocks, is left out (first person; the camera is at the view's origin). */
	static final float NEAR_EYE = 0.1F;
	/** The arms' size at full aim down a gun's sights, against their size at the hip (slice 9F). */
	static final float AIMED_SIZE = 0.5F;
	/** The size the last arm was drawn at (for the client game tests); NaN before any. */
	private static float lastSize = Float.NaN;
	private static ModelPart[] arms;
	private static ModelPart[] slimArms;

	GunArmsLayer(GunRenderer renderer) {
		super(renderer);
	}

	@Override
	public void addPerBoneRender(RenderPassInfo<GeoRenderState> info, BiConsumer<GeoBone, PerBoneRender<GeoRenderState>> consumer) {
		GunRenderer.View view = info.getGeckolibData(GunRenderer.VIEW);
		if (view == null || !info.willRender()) {
			return;
		}
		float size = GunRenderer.sight(info, view).isPresent() ? Mth.lerp(view.aim(), 1.0F, AIMED_SIZE) : 1.0F;
		for (int side = 0; side < 2; side++) {
			boolean right = side == 0;
			if (!right && view.offHand()) {
				continue; // slice 10G: that arm holds the main gun
			}
			info.model().getBone(right ? "right_arm" : "left_arm")
					.ifPresent(bone -> consumer.accept(bone, (pass, posed, tasks) -> arm(pass, tasks, view, bone, right, size)));
		}
	}

	/** The size the last arm was drawn at, 1 at the hip ({@link #AIMED_SIZE}); NaN before any (for the client game tests). */
	public static float lastSize() {
		return lastSize;
	}

	/**
	 * The arm at this bone: the right_arm bone's ({@code right}) is the grip hand's. In a left hand (slice 10G) each is
	 * drawn as the other side's arm, running to its shoulder mirrored across the gun.
	 */
	private static void arm(RenderPassInfo<GeoRenderState> pass, net.minecraft.client.renderer.SubmitNodeCollector tasks,
			GunRenderer.View view, GeoBone bone, boolean right, float size) {
		boolean rightPart = right != view.left();
		ModelPart part = parts(view.slim())[rightPart ? 0 : 1];
		// Centre the arm on the bone: a wide arm's box spans x -3..1 (right) or -1..3 (left), a slim one's -2..1 or -1..2.
		float centre = view.slim() ? 0.5F : 1.0F;
		PoseStack poseStack = pass.poseStack();
		poseStack.pushPose();
		// The arm runs along -y; turn that onto the way to the shoulder (the bone's pivot and locators are both in the
		// model's rest space, so their difference is a direction in the posed bone's own frame).
		String name = right ? "right_shoulder" : "left_shoulder";
		for (GeoLocator shoulder : bone.locators()) {
			if (shoulder.name().equals(name)) {
				float x = (shoulder.offsetX() - bone.pivotX()) * (view.left() ? -1.0F : 1.0F);
				float y = shoulder.offsetY() - bone.pivotY();
				float z = shoulder.offsetZ() - bone.pivotZ();
				if (x * x + y * y + z * z > 1.0E-4F) {
					poseStack.rotate(new Quaternionf().rotationTo(0.0F, -1.0F, 0.0F, x, y, z));
				}
			}
		}
		// Smaller about the hand as the aim comes in (slice 9F).
		poseStack.scale(size, size, size);
		lastSize = size;
		// Here the arm's box runs about the bone's axis: half its width (wide 2, slim 1.5) across x and z.
		if (byTheEye(poseStack.last().pose(), view.slim() ? 1.5F : 2.0F)) {
			poseStack.popPose();
			return;
		}
		poseStack.translate((rightPart ? centre : -centre) / 16.0F, -FIST / 16.0F, 0.0F);
		int light = pass.packedLight();
		tasks.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(view.skin()), (pose, buffer) -> {
			PoseStack local = new PoseStack();
			local.last().set(pose);
			part.render(local, buffer, light, OverlayTexture.NO_OVERLAY);
		});
		poseStack.popPose();
	}

	/**
	 * Whether the arm's box, sleeve and all, comes within {@link #NEAR_EYE} of the eye, in this pose: the arm's own frame,
	 * centred on the bone's axis with its shoulder end toward -y, before it is moved along it to put the fist past the hand.
	 * An arm an animation squashes to nothing (to hide it) draws nothing whichever way this answers.
	 */
	static boolean byTheEye(Matrix4f pose, float halfWidth) {
		Vector3f eye = new Matrix4f(pose).invert().transformPosition(new Vector3f());
		float across = halfWidth + SLEEVE;
		float deep = HALF_DEPTH + SLEEVE;
		Vector3f nearest = new Vector3f(Mth.clamp(eye.x() * 16.0F, -across, across),
				Mth.clamp(eye.y() * 16.0F, SHOULDER_END - FIST - SLEEVE, FIST_END - FIST + SLEEVE),
				Mth.clamp(eye.z() * 16.0F, -deep, deep)).div(16.0F);
		return pose.transformPosition(nearest).length() < NEAR_EYE;
	}

	/** The player model's right and left arm (with their sleeves), posed at the origin. */
	private static ModelPart[] parts(boolean slim) {
		if (slim ? slimArms == null : arms == null) {
			ModelPart root = Minecraft.getInstance().getEntityModels().bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER);
			ModelPart[] baked = {root.getChild("right_arm"), root.getChild("left_arm")};
			for (ModelPart part : baked) {
				part.x = 0.0F;
				part.y = 0.0F;
				part.z = 0.0F;
				part.xRot = 0.0F;
				part.yRot = 0.0F;
				part.zRot = 0.0F;
			}
			if (slim) {
				slimArms = baked;
			} else {
				arms = baked;
			}
		}
		return slim ? slimArms : arms;
	}
}
