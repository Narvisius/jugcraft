package io.github.jimbozoomer.jugcraft.client.warehouse;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.material.Fluids;

/**
 * Part W4: a liquid seen in a level gauge's glass or a sight flow glass, drawn with the liquid's own (still) texture
 * and colour, as the storage tanks draw theirs (client/StorageTankRenderer.java).
 */
final class WhLiquid {
	private WhLiquid() {
	}

	static TextureAtlasSprite sprite(FluidVariant liquid) {
		return Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(liquid.getFluid().defaultFluidState()).stillMaterial().sprite();
	}

	static int colour(FluidVariant liquid, net.minecraft.core.BlockPos pos) {
		int rgb = (Minecraft.getInstance().level != null ? FluidVariantRendering.getColor(liquid, Minecraft.getInstance().level, pos)
				: FluidVariantRendering.getColor(liquid)) & 0xFFFFFF;
		return (liquid.getFluid().isSame(Fluids.WATER) ? 0xE0000000 : 0xFF000000) | rgb;
	}

	/** A face of liquid facing +z at z, from (x0, y0) to (x1, y1) (pixels of the current frame). */
	static void face(SubmitNodeCollector collector, PoseStack pose, float x0, float y0, float x1, float y1, float z, FluidVariant liquid, int light,
			net.minecraft.core.BlockPos pos) {
		if (liquid.isBlank() || y1 <= y0 || x1 <= x0) {
			return;
		}
		TextureAtlasSprite sprite = sprite(liquid);
		int colour = colour(liquid, pos);
		float u0 = sprite.getU(0), u1 = sprite.getU(Math.min(1, (x1 - x0) / 16F)), v0 = sprite.getV(0), v1 = sprite.getV(Math.min(1, (y1 - y0) / 16F));
		collector.submitCustomGeometry(pose, WhQuads.RENDER_TYPE, (p, consumer) -> {
			consumer.addVertex(p, x0, y0, z).setColor(colour).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 0, 1);
			consumer.addVertex(p, x1, y0, z).setColor(colour).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 0, 1);
			consumer.addVertex(p, x1, y1, z).setColor(colour).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 0, 1);
			consumer.addVertex(p, x0, y1, z).setColor(colour).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 0, 1);
		});
	}
}
