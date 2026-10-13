package io.github.jimbozoomer.jugcraft.client;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import io.github.jimbozoomer.jugcraft.lair.tatterlace.JugcraftTatterlace;
import io.github.jimbozoomer.jugcraft.lair.tatterlace.RollingSpoolEntity;
import io.github.jimbozoomer.jugcraft.lair.tatterlace.SpiderlingEntity;
import io.github.jimbozoomer.jugcraft.lair.tatterlace.TatterEggSacEntity;
import io.github.jimbozoomer.jugcraft.lair.tatterlace.TatterlaceEntity;
import io.github.jimbozoomer.jugcraft.lair.tatterlace.TossedThimbleEntity;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.AABB;

/**
 * Madame Tatterlace and her fight on the client (docs/features/tatterlace.md): GeckoLib draws her, her tossed thimbles,
 * her rolling spools, her egg sacs and her spiderlings from their own models, playing the clips for what the server
 * synced, with their glowmasks (eyes, gem, red cuffs) drawn full bright. Her legs reach far past her and her dragline far
 * above her, so she is kept drawn while any of her can be on screen. Binding Threads and Lace Snares are their own
 * particles. Nothing here decides anything.
 */
final class TatterlaceClient {
	private TatterlaceClient() {
	}

	static void register() {
		EntityRendererRegistry.register(JugcraftTatterlace.TATTERLACE, context -> new GeoEntityRenderer<TatterlaceEntity, EntityRenderState>(context,
				JugcraftTatterlace.TATTERLACE) {
			@Override
			protected AABB getBoundingBoxForCulling(TatterlaceEntity tatterlace, float partialTick) {
				return tatterlace.getBoundingBox().inflate(2.5, 6.0, 2.5);
			}
		}.withRenderLayer(AutoGlowingGeoLayer::new));
		EntityRendererRegistry.register(JugcraftTatterlace.TOSSED_THIMBLE, context -> new GeoEntityRenderer<TossedThimbleEntity, EntityRenderState>(
				context, JugcraftTatterlace.TOSSED_THIMBLE));
		EntityRendererRegistry.register(JugcraftTatterlace.ROLLING_SPOOL, context -> new GeoEntityRenderer<RollingSpoolEntity, EntityRenderState>(
				context, JugcraftTatterlace.ROLLING_SPOOL));
		EntityRendererRegistry.register(JugcraftTatterlace.TATTER_EGG_SAC, context -> new GeoEntityRenderer<TatterEggSacEntity, EntityRenderState>(
				context, JugcraftTatterlace.TATTER_EGG_SAC));
		EntityRendererRegistry.register(JugcraftTatterlace.TATTER_SPIDERLING, context -> new GeoEntityRenderer<SpiderlingEntity, EntityRenderState>(
				context, JugcraftTatterlace.TATTER_SPIDERLING).withRenderLayer(AutoGlowingGeoLayer::new));
		EntityRendererRegistry.register(JugcraftTatterlace.BINDING_THREAD, NoopRenderer::new);
		EntityRendererRegistry.register(JugcraftTatterlace.LACE_SNARE, NoopRenderer::new);
	}
}
