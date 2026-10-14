package io.github.jimbozoomer.jugcraft.client;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import io.github.jimbozoomer.jugcraft.lair.hag.HagLikenessEntity;
import io.github.jimbozoomer.jugcraft.lair.hag.HagRootsEntity;
import io.github.jimbozoomer.jugcraft.lair.hag.JugcraftHag;
import io.github.jimbozoomer.jugcraft.lair.hag.MireGraspEntity;
import io.github.jimbozoomer.jugcraft.lair.hag.MireHagEntity;
import io.github.jimbozoomer.jugcraft.lair.hag.PoisonBrewEntity;
import io.github.jimbozoomer.jugcraft.lair.hag.RootKnotEntity;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.AABB;

/**
 * The Mire Hag and her fight on the client (docs/features/mire-hag.md): GeckoLib draws her, her Likenesses, her Root Knot,
 * her roots, her grasping hands and her brews from their own models, playing the clips for what the server synced, their
 * glowmasks (her eyes and her lantern's flame; the brew's venom) drawn full bright. Her Likenesses are drawn as she is,
 * from her model and her sheet, but cast no shadow: the one thing about them a sharp eye can catch. Her staff and lantern
 * reach out before her, so she is kept drawn while any of her can be on screen. Nothing here decides anything.
 */
final class MireHagClient {
	/** Her shadow, wide as her skirts; her Likenesses have none. */
	static final float SHADOW = 0.8F;

	private MireHagClient() {
	}

	static void register() {
		EntityRendererRegistry.register(JugcraftHag.MIRE_HAG, context -> new GeoEntityRenderer<MireHagEntity, EntityRenderState>(context,
				JugcraftHag.MIRE_HAG) {
			{
				shadowRadius = SHADOW;
			}

			@Override
			protected AABB getBoundingBoxForCulling(MireHagEntity hag, float partialTick) {
				return hag.getBoundingBox().inflate(1.5, 0.5, 1.5);
			}
		}.withRenderLayer(AutoGlowingGeoLayer::new));
		EntityRendererRegistry.register(JugcraftHag.HAG_LIKENESS, context -> new GeoEntityRenderer<HagLikenessEntity, EntityRenderState>(
				context, JugcraftHag.HAG_LIKENESS) {
			{
				shadowRadius = 0.0F;
			}

			@Override
			protected AABB getBoundingBoxForCulling(HagLikenessEntity likeness, float partialTick) {
				return likeness.getBoundingBox().inflate(1.5, 0.5, 1.5);
			}
		}.withRenderLayer(AutoGlowingGeoLayer::new));
		EntityRendererRegistry.register(JugcraftHag.ROOT_KNOT, context -> new GeoEntityRenderer<RootKnotEntity, EntityRenderState>(
				context, JugcraftHag.ROOT_KNOT));
		EntityRendererRegistry.register(JugcraftHag.HAG_ROOTS, context -> new GeoEntityRenderer<HagRootsEntity, EntityRenderState>(
				context, JugcraftHag.HAG_ROOTS));
		EntityRendererRegistry.register(JugcraftHag.MIRE_GRASP, context -> new GeoEntityRenderer<MireGraspEntity, EntityRenderState>(
				context, JugcraftHag.MIRE_GRASP) {
			@Override
			protected AABB getBoundingBoxForCulling(MireGraspEntity grasp, float partialTick) {
				return grasp.getBoundingBox().inflate(0.5, 0.5, 0.5);
			}
		});
		EntityRendererRegistry.register(JugcraftHag.POISON_BREW, context -> new GeoEntityRenderer<PoisonBrewEntity, EntityRenderState>(
				context, JugcraftHag.POISON_BREW).withRenderLayer(AutoGlowingGeoLayer::new));
	}
}
