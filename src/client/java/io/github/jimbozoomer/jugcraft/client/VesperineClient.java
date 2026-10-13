package io.github.jimbozoomer.jugcraft.client;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import io.github.jimbozoomer.jugcraft.lair.vesperine.GraveThrallEntity;
import io.github.jimbozoomer.jugcraft.lair.vesperine.JugcraftVesperine;
import io.github.jimbozoomer.jugcraft.lair.vesperine.ReaperSkullEntity;
import io.github.jimbozoomer.jugcraft.lair.vesperine.ThrownScytheEntity;
import io.github.jimbozoomer.jugcraft.lair.vesperine.VesperineEntity;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.AABB;

/**
 * Vesperine and her fight on the client (docs/features/vesperine.md): GeckoLib draws her, her skulls, her thrown scythe
 * and her thralls from their own models, playing the clips for what the server synced, with their glowmasks (eyes, the
 * blade's edge, the halo) drawn full bright. Grief Bolts, the harvest's souls and the Vesper Scythe's crescent are their
 * own particles. Her scythe reaches two blocks past her and the thrown scythe spins wider than its hitbox, so both are
 * kept drawn while any of it can be on screen. Nothing here decides anything.
 */
final class VesperineClient {
	private VesperineClient() {
	}

	static void register() {
		EntityRendererRegistry.register(JugcraftVesperine.VESPERINE, context -> new GeoEntityRenderer<VesperineEntity, EntityRenderState>(context,
				JugcraftVesperine.VESPERINE) {
			@Override
			protected AABB getBoundingBoxForCulling(VesperineEntity vesperine, float partialTick) {
				return vesperine.getBoundingBox().inflate(2.5, 1.0, 2.5);
			}
		}.withRenderLayer(AutoGlowingGeoLayer::new));
		EntityRendererRegistry.register(JugcraftVesperine.DIRGE, context -> new GeoEntityRenderer<ReaperSkullEntity, EntityRenderState>(context,
				JugcraftVesperine.DIRGE).withRenderLayer(AutoGlowingGeoLayer::new));
		EntityRendererRegistry.register(JugcraftVesperine.REQUIEM, context -> new GeoEntityRenderer<ReaperSkullEntity, EntityRenderState>(context,
				JugcraftVesperine.REQUIEM).withRenderLayer(AutoGlowingGeoLayer::new));
		EntityRendererRegistry.register(JugcraftVesperine.THROWN_SCYTHE, context -> new GeoEntityRenderer<ThrownScytheEntity, EntityRenderState>(
				context, JugcraftVesperine.THROWN_SCYTHE) {
			@Override
			protected AABB getBoundingBoxForCulling(ThrownScytheEntity scythe, float partialTick) {
				return scythe.getBoundingBox().inflate(2.5, 0.5, 2.5);
			}
		}.withRenderLayer(AutoGlowingGeoLayer::new));
		EntityRendererRegistry.register(JugcraftVesperine.GRAVE_THRALL, context -> new GeoEntityRenderer<GraveThrallEntity, EntityRenderState>(
				context, JugcraftVesperine.GRAVE_THRALL).withRenderLayer(AutoGlowingGeoLayer::new));
		EntityRendererRegistry.register(JugcraftVesperine.GRIEF_BOLT, NoopRenderer::new);
		EntityRendererRegistry.register(JugcraftVesperine.HARVEST_SOUL, NoopRenderer::new);
		EntityRendererRegistry.register(JugcraftVesperine.REAPING_CRESCENT, NoopRenderer::new);
	}
}
