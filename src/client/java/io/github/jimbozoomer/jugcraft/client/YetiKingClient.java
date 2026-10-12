package io.github.jimbozoomer.jugcraft.client;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import io.github.jimbozoomer.jugcraft.lair.yeti.FallingIcicleEntity;
import io.github.jimbozoomer.jugcraft.lair.yeti.GlacialSpikeEntity;
import io.github.jimbozoomer.jugcraft.lair.yeti.HurledBoulderEntity;
import io.github.jimbozoomer.jugcraft.lair.yeti.JugcraftYeti;
import io.github.jimbozoomer.jugcraft.lair.yeti.YetiKingEntity;
import io.github.jimbozoomer.jugcraft.lair.yeti.YetiWhelpEntity;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.AABB;

/**
 * The Yeti King and his fight on the client (docs/features/yeti-king.md): GeckoLib draws him, his whelps, his hurled
 * boulders, his falling icicles and his glacial spikes from their own models, playing the clips for what the server
 * synced, his glowmask (his eyes, and his crown when it blazes) drawn full bright. His long arms swing well past him, so he
 * is kept drawn while any of him can be on screen. Nothing here decides anything.
 */
final class YetiKingClient {
	private YetiKingClient() {
	}

	static void register() {
		EntityRendererRegistry.register(JugcraftYeti.YETI_KING, context -> new GeoEntityRenderer<YetiKingEntity, EntityRenderState>(context,
				JugcraftYeti.YETI_KING) {
			@Override
			protected AABB getBoundingBoxForCulling(YetiKingEntity king, float partialTick) {
				return king.getBoundingBox().inflate(2.5, 1.5, 2.5);
			}
		}.withRenderLayer(AutoGlowingGeoLayer::new));
		EntityRendererRegistry.register(JugcraftYeti.YETI_WHELP, context -> new GeoEntityRenderer<YetiWhelpEntity, EntityRenderState>(
				context, JugcraftYeti.YETI_WHELP));
		EntityRendererRegistry.register(JugcraftYeti.HURLED_BOULDER, context -> new GeoEntityRenderer<HurledBoulderEntity, EntityRenderState>(
				context, JugcraftYeti.HURLED_BOULDER));
		EntityRendererRegistry.register(JugcraftYeti.FALLING_ICICLE, context -> new GeoEntityRenderer<FallingIcicleEntity, EntityRenderState>(
				context, JugcraftYeti.FALLING_ICICLE));
		EntityRendererRegistry.register(JugcraftYeti.GLACIAL_SPIKE, context -> new GeoEntityRenderer<GlacialSpikeEntity, EntityRenderState>(
				context, JugcraftYeti.GLACIAL_SPIKE));
	}
}
