package io.github.jimbozoomer.jugcraft.client;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import io.github.jimbozoomer.jugcraft.lair.lich.CryptLichEntity;
import io.github.jimbozoomer.jugcraft.lair.lich.JugcraftLich;
import io.github.jimbozoomer.jugcraft.lair.lich.LichBonesEntity;
import io.github.jimbozoomer.jugcraft.lair.lich.RestlessDeadEntity;
import io.github.jimbozoomer.jugcraft.lair.lich.SoulBoltEntity;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.AABB;

/**
 * The Crypt Lich and his fight on the client (docs/features/crypt-lich.md): GeckoLib draws him, his Restless Dead, his
 * Soul Bolts and his bones from their own models, playing the clips for what the server synced, their glowmasks (his eyes,
 * his crown's soul-fire, the skull in his hand and his staff's gem; the dead's eyes; the bolt) drawn full bright. His
 * staff reaches up over his crown and out before him, so he is kept drawn while any of him can be on screen. His chains
 * and his beam are the server's particles. Nothing here decides anything.
 */
final class CryptLichClient {
	/** His shadow, wide as his robe's hem. */
	static final float SHADOW = 0.7F;

	private CryptLichClient() {
	}

	static void register() {
		EntityRendererRegistry.register(JugcraftLich.CRYPT_LICH, context -> new GeoEntityRenderer<CryptLichEntity, EntityRenderState>(context,
				JugcraftLich.CRYPT_LICH) {
			{
				shadowRadius = SHADOW;
			}

			@Override
			protected AABB getBoundingBoxForCulling(CryptLichEntity lich, float partialTick) {
				return lich.getBoundingBox().inflate(1.5, 1.0, 1.5);
			}
		}.withRenderLayer(AutoGlowingGeoLayer::new));
		EntityRendererRegistry.register(JugcraftLich.RESTLESS_DEAD, context -> new GeoEntityRenderer<RestlessDeadEntity, EntityRenderState>(
				context, JugcraftLich.RESTLESS_DEAD).withRenderLayer(AutoGlowingGeoLayer::new));
		EntityRendererRegistry.register(JugcraftLich.SOUL_BOLT, context -> new GeoEntityRenderer<SoulBoltEntity, EntityRenderState>(
				context, JugcraftLich.SOUL_BOLT).withRenderLayer(AutoGlowingGeoLayer::new));
		EntityRendererRegistry.register(JugcraftLich.LICH_BONES, context -> new GeoEntityRenderer<LichBonesEntity, EntityRenderState>(
				context, JugcraftLich.LICH_BONES) {
			@Override
			protected AABB getBoundingBoxForCulling(LichBonesEntity bones, float partialTick) {
				return bones.getBoundingBox().inflate(1.0, 0.5, 1.0);
			}
		});
	}
}
