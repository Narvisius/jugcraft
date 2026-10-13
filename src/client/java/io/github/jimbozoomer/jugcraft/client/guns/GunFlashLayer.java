package io.github.jimbozoomer.jugcraft.client.guns;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.GeoLocator;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.guns.GunItem;
import java.util.function.BiConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Quaternionf;

/**
 * The muzzle flash (slice 6): for two ticks after a shot ({@link GunEffects#FLASH_TICKS}), one of the owner's four flash frames
 * (textures/item/guns/flash/) at the gun's muzzle locator, or at a fitted muzzle brake's or extended barrel's own
 * ("muzzle_&lt;attachment&gt;"), full bright. The barrel points along the bone's -z: the frame faces back down it (what
 * the shooter sees, a star) and two crossed copies stretch forward along it (what anyone beside them sees). Each shot
 * picks its frame and its turn about the barrel from its time; the flash shows full, then swells and fades.
 * {@link GunRenderer} decides when there is one ({@link GunRenderer#FLASH}); a silencer hides it. An energy weapon's
 * discharge (slice 8D) is the same frames tinted ({@link GunLooks#FLASH_TINTS}).
 */
final class GunFlashLayer extends GeoRenderLayer<GunItem, GeoItemRenderer.RenderData, GeoRenderState> {
	private static final RenderType[] FRAMES = new RenderType[4];

	static {
		for (int i = 0; i < FRAMES.length; i++) {
			FRAMES[i] = RenderTypes.entityTranslucentEmissive(Jugcraft.id("textures/item/guns/flash/flash_" + i + ".png"));
		}
	}

	GunFlashLayer(GunRenderer renderer) {
		super(renderer);
	}

	@Override
	public void addPerBoneRender(RenderPassInfo<GeoRenderState> info, BiConsumer<GeoBone, PerBoneRender<GeoRenderState>> consumer) {
		GunRenderer.Flash flash = info.getGeckolibData(GunRenderer.FLASH);
		if (flash == null || !info.willRender()) {
			return;
		}
		GeoLocator muzzle = info.model().getLocator(flash.locator()).or(() -> info.model().getLocator("muzzle")).orElse(null);
		if (muzzle == null) {
			return;
		}
		GeoBone bone = muzzle.parent();
		consumer.accept(bone, (pass, posed, tasks) -> draw(pass, tasks, flash, bone, muzzle));
	}

	private static void draw(RenderPassInfo<GeoRenderState> pass, SubmitNodeCollector tasks, GunRenderer.Flash flash, GeoBone bone,
			GeoLocator muzzle) {
		PoseStack poseStack = pass.poseStack();
		poseStack.pushPose();
		// The per-bone pose stands at the bone's pivot; the locator and the pivot are both in the model's rest space.
		poseStack.translate((muzzle.offsetX() - bone.pivotX()) / 16.0F, (muzzle.offsetY() - bone.pivotY()) / 16.0F,
				(muzzle.offsetZ() - bone.pivotZ()) / 16.0F);
		poseStack.rotate(new Quaternionf().rotationZ(Math.floorMod(flash.shot(), 360) * 2.39996F));
		// Full for the first tick it shows, swelling a little and fading over the second.
		float swell = 0.8F + 0.4F * Math.clamp(flash.age() - 1.0F, 0.0F, 1.0F);
		float half = flash.size() / 32.0F * swell;
		float length = half * 3.0F;
		int alpha = (int) (255.0F * Math.clamp(GunEffects.FLASH_TICKS - flash.age(), 0.0F, 1.0F));
		int color = alpha << 24 | flash.tint() & 0xFFFFFF;
		GunEffects.flashDrawn();
		tasks.submitCustomGeometry(poseStack, FRAMES[Math.floorMod(flash.shot(), FRAMES.length)], (pose, buffer) -> {
			// Facing back down the barrel, a little in front of the muzzle.
			quad(pose, buffer, color, -half, -half, -0.02F, half, -half, -0.02F, half, half, -0.02F, -half, half, -0.02F, 0.0F, 0.0F, 1.0F);
			// Along the barrel, upright and flat.
			quad(pose, buffer, color, 0.0F, -half, 0.0F, 0.0F, -half, -length, 0.0F, half, -length, 0.0F, half, 0.0F, 1.0F, 0.0F, 0.0F);
			quad(pose, buffer, color, -half, 0.0F, 0.0F, -half, 0.0F, -length, half, 0.0F, -length, half, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F);
		});
		poseStack.popPose();
	}

	/** One quad, both sides, the whole frame on it. */
	private static void quad(PoseStack.Pose pose, VertexConsumer buffer, int color, float x0, float y0, float z0, float x1, float y1,
			float z1, float x2, float y2, float z2, float x3, float y3, float z3, float nx, float ny, float nz) {
		vertex(pose, buffer, color, x0, y0, z0, 0.0F, 1.0F, nx, ny, nz);
		vertex(pose, buffer, color, x1, y1, z1, 1.0F, 1.0F, nx, ny, nz);
		vertex(pose, buffer, color, x2, y2, z2, 1.0F, 0.0F, nx, ny, nz);
		vertex(pose, buffer, color, x3, y3, z3, 0.0F, 0.0F, nx, ny, nz);
		vertex(pose, buffer, color, x3, y3, z3, 0.0F, 0.0F, -nx, -ny, -nz);
		vertex(pose, buffer, color, x2, y2, z2, 1.0F, 0.0F, -nx, -ny, -nz);
		vertex(pose, buffer, color, x1, y1, z1, 1.0F, 1.0F, -nx, -ny, -nz);
		vertex(pose, buffer, color, x0, y0, z0, 0.0F, 1.0F, -nx, -ny, -nz);
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, int color, float x, float y, float z, float u, float v, float nx,
			float ny, float nz) {
		buffer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, nx, ny, nz);
	}
}
