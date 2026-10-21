package io.github.jimbozoomer.jugcraft.client;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import io.github.jimbozoomer.jugcraft.lair.sentinel.CydonianSentinelEntity;
import io.github.jimbozoomer.jugcraft.lair.sentinel.JugcraftSentinel;
import io.github.jimbozoomer.jugcraft.lair.sentinel.QuakeSpikeEntity;
import io.github.jimbozoomer.jugcraft.lair.sentinel.RustShardEntity;
import io.github.jimbozoomer.jugcraft.lair.sentinel.SentinelMeteorEntity;
import io.github.jimbozoomer.jugcraft.lair.sentinel.ShardSentryEntity;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.AABB;

/**
 * The Cydonian Sentinel and its fight on the client (docs/features/cydonian-sentinel.md): GeckoLib draws it, its Shard
 * Sentries, its Rust Shards, its Quake Spikes and its meteors from their own models, playing the clips for what the server
 * synced (its body's clip, its core's light, its chest plates and its form), their glowmasks (its core, its visor's slit,
 * the star on its chest and its shards' edges; the sentries' sparks; the shards' red hearts; the meteors' fire) drawn full
 * bright. Its fists swing out and its shards orbit round it, so it is kept drawn while any of it can be on screen. The red
 * storm's dust and the lance's beam are the server's particles. Nothing here decides anything.
 */
final class CydonianSentinelClient {
	/** Its shadow, wide as its stance. */
	static final float SHADOW = 1.4F;

	private CydonianSentinelClient() {
	}

	static void register() {
		EntityRendererRegistry.register(JugcraftSentinel.CYDONIAN_SENTINEL, context -> new GeoEntityRenderer<CydonianSentinelEntity, EntityRenderState>(
				context, JugcraftSentinel.CYDONIAN_SENTINEL) {
			{
				shadowRadius = SHADOW;
			}

			@Override
			protected AABB getBoundingBoxForCulling(CydonianSentinelEntity sentinel, float partialTick) {
				return sentinel.getBoundingBox().inflate(2.0, 1.5, 2.0);
			}
		}.withRenderLayer(AutoGlowingGeoLayer::new));
		EntityRendererRegistry.register(JugcraftSentinel.SHARD_SENTRY, context -> new GeoEntityRenderer<ShardSentryEntity, EntityRenderState>(
				context, JugcraftSentinel.SHARD_SENTRY).withRenderLayer(AutoGlowingGeoLayer::new));
		EntityRendererRegistry.register(JugcraftSentinel.RUST_SHARD, context -> new GeoEntityRenderer<RustShardEntity, EntityRenderState>(
				context, JugcraftSentinel.RUST_SHARD).withRenderLayer(AutoGlowingGeoLayer::new));
		EntityRendererRegistry.register(JugcraftSentinel.QUAKE_SPIKE, context -> new GeoEntityRenderer<QuakeSpikeEntity, EntityRenderState>(
				context, JugcraftSentinel.QUAKE_SPIKE));
		EntityRendererRegistry.register(JugcraftSentinel.SENTINEL_METEOR, context -> new GeoEntityRenderer<SentinelMeteorEntity, EntityRenderState>(
				context, JugcraftSentinel.SENTINEL_METEOR) {
			@Override
			protected AABB getBoundingBoxForCulling(SentinelMeteorEntity meteor, float partialTick) {
				return meteor.getBoundingBox().inflate(0.5, 1.0, 0.5);
			}
		}.withRenderLayer(AutoGlowingGeoLayer::new));
	}
}
