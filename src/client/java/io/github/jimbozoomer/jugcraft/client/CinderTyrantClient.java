package io.github.jimbozoomer.jugcraft.client;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import io.github.jimbozoomer.jugcraft.lair.tyrant.CinderTyrantEntity;
import io.github.jimbozoomer.jugcraft.lair.tyrant.CinderlingEntity;
import io.github.jimbozoomer.jugcraft.lair.tyrant.FallingCinderEntity;
import io.github.jimbozoomer.jugcraft.lair.tyrant.JugcraftTyrant;
import io.github.jimbozoomer.jugcraft.lair.tyrant.MagmaGobEntity;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.AABB;

/**
 * The Cinder Tyrant and his fight on the client (docs/features/cinder-tyrant.md): GeckoLib draws him, his Cinderlings, his
 * gobs of magma and his falling cinders from their own models, playing the clips for what the server synced, and each
 * one's glowmask (his eyes, his cracks and seams while they burn; the Cinderlings' cores; the gobs and the cinders whole)
 * drawn full bright. His tail reaches well behind him, so he is kept drawn while any of him can be on screen. Nothing here
 * decides anything.
 */
final class CinderTyrantClient {
	private CinderTyrantClient() {
	}

	static void register() {
		EntityRendererRegistry.register(JugcraftTyrant.CINDER_TYRANT, context -> new GeoEntityRenderer<CinderTyrantEntity, EntityRenderState>(
				context, JugcraftTyrant.CINDER_TYRANT) {
			@Override
			protected AABB getBoundingBoxForCulling(CinderTyrantEntity tyrant, float partialTick) {
				return tyrant.getBoundingBox().inflate(3.5, 1.5, 3.5);
			}
		}.withRenderLayer(AutoGlowingGeoLayer::new));
		EntityRendererRegistry.register(JugcraftTyrant.CINDERLING, context -> new GeoEntityRenderer<CinderlingEntity, EntityRenderState>(
				context, JugcraftTyrant.CINDERLING).withRenderLayer(AutoGlowingGeoLayer::new));
		EntityRendererRegistry.register(JugcraftTyrant.MAGMA_GOB, context -> new GeoEntityRenderer<MagmaGobEntity, EntityRenderState>(
				context, JugcraftTyrant.MAGMA_GOB).withRenderLayer(AutoGlowingGeoLayer::new));
		EntityRendererRegistry.register(JugcraftTyrant.FALLING_CINDER, context -> new GeoEntityRenderer<FallingCinderEntity, EntityRenderState>(
				context, JugcraftTyrant.FALLING_CINDER).withRenderLayer(AutoGlowingGeoLayer::new));
	}
}
