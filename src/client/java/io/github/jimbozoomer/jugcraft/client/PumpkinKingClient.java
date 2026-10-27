package io.github.jimbozoomer.jugcraft.client;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import io.github.jimbozoomer.jugcraft.lair.king.JackOStrawEntity;
import io.github.jimbozoomer.jugcraft.lair.king.JugcraftPumpkinKing;
import io.github.jimbozoomer.jugcraft.lair.king.KingsHeadEntity;
import io.github.jimbozoomer.jugcraft.lair.king.KingsPumpkinEntity;
import io.github.jimbozoomer.jugcraft.lair.king.PumpkinKingEntity;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.AABB;

/**
 * The Pumpkin King and his fight on the client (docs/features/pumpkin-king.md): GeckoLib draws him, his Jack-o'-Straws,
 * his rolling head and the pumpkin he tosses from their own models, playing the clips for what the server synced, with
 * their glowmasks (the fire in his face, orange or white-hot; his Jacks' carved faces; the head's and the pumpkin's fire)
 * drawn full bright. His arms, his scythe and, hanging on his post, his whole body reach far past his box, so he is kept
 * drawn while any of him can be on screen. The crows, the ember rings, the vines and the lines his attacks follow are the
 * server's particles. Nothing here decides anything.
 */
final class PumpkinKingClient {
	/** His shadow, wide as his stride. */
	static final float SHADOW = 1.3F;

	private PumpkinKingClient() {
	}

	static void register() {
		EntityRendererRegistry.register(JugcraftPumpkinKing.PUMPKIN_KING, context -> new GeoEntityRenderer<PumpkinKingEntity, EntityRenderState>(
				context, JugcraftPumpkinKing.PUMPKIN_KING) {
			{
				shadowRadius = SHADOW;
			}

			@Override
			protected AABB getBoundingBoxForCulling(PumpkinKingEntity king, float partialTick) {
				return king.getBoundingBox().inflate(4.0, 2.5, 4.0);
			}
		}.withRenderLayer(AutoGlowingGeoLayer::new));
		EntityRendererRegistry.register(JugcraftPumpkinKing.JACK_O_STRAW, context -> new GeoEntityRenderer<JackOStrawEntity, EntityRenderState>(
				context, JugcraftPumpkinKing.JACK_O_STRAW).withRenderLayer(AutoGlowingGeoLayer::new));
		EntityRendererRegistry.register(JugcraftPumpkinKing.KINGS_HEAD, context -> new GeoEntityRenderer<KingsHeadEntity, EntityRenderState>(
				context, JugcraftPumpkinKing.KINGS_HEAD).withRenderLayer(AutoGlowingGeoLayer::new));
		EntityRendererRegistry.register(JugcraftPumpkinKing.KINGS_PUMPKIN, context -> new GeoEntityRenderer<KingsPumpkinEntity, EntityRenderState>(
				context, JugcraftPumpkinKing.KINGS_PUMPKIN).withRenderLayer(AutoGlowingGeoLayer::new));
	}
}
