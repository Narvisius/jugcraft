package io.github.jimbozoomer.jugcraft.client;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import io.github.jimbozoomer.jugcraft.lair.dreadnought.HotRivetEntity;
import io.github.jimbozoomer.jugcraft.lair.dreadnought.IronDreadnoughtEntity;
import io.github.jimbozoomer.jugcraft.lair.dreadnought.JugcraftDreadnought;
import io.github.jimbozoomer.jugcraft.lair.dreadnought.MortarShellEntity;
import io.github.jimbozoomer.jugcraft.lair.dreadnought.SparkerEntity;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.AABB;

/**
 * The Iron Dreadnought and his fight on the client (docs/features/iron-dreadnought.md): GeckoLib draws him, his Sparkers,
 * his hot rivets and his mortar shells from their own models, playing the clips for what the server synced, their
 * glowmasks (his searchlight's lens, his coils' balls and arcs, the furnaces behind his vents and their seams; the
 * Sparker's coil and lenses; the rivet's heat; the shell's fuse) drawn full bright. His shoulders and arms reach past his
 * box and his coils over it, so he is kept drawn while any of him can be on screen. His charge's threads, his arcs and the
 * mortar's red mark are the server's particles. Nothing here decides anything.
 */
final class IronDreadnoughtClient {
	/** His shadow, wide as his feet's spread. */
	static final float SHADOW = 2.0F;

	private IronDreadnoughtClient() {
	}

	static void register() {
		EntityRendererRegistry.register(JugcraftDreadnought.IRON_DREADNOUGHT, context -> new GeoEntityRenderer<IronDreadnoughtEntity, EntityRenderState>(
				context, JugcraftDreadnought.IRON_DREADNOUGHT) {
			{
				shadowRadius = SHADOW;
			}

			@Override
			protected AABB getBoundingBoxForCulling(IronDreadnoughtEntity dreadnought, float partialTick) {
				return dreadnought.getBoundingBox().inflate(1.5, 1.0, 1.5);
			}
		}.withRenderLayer(AutoGlowingGeoLayer::new));
		EntityRendererRegistry.register(JugcraftDreadnought.SPARKER, context -> new GeoEntityRenderer<SparkerEntity, EntityRenderState>(
				context, JugcraftDreadnought.SPARKER).withRenderLayer(AutoGlowingGeoLayer::new));
		EntityRendererRegistry.register(JugcraftDreadnought.HOT_RIVET, context -> new GeoEntityRenderer<HotRivetEntity, EntityRenderState>(
				context, JugcraftDreadnought.HOT_RIVET).withRenderLayer(AutoGlowingGeoLayer::new));
		EntityRendererRegistry.register(JugcraftDreadnought.MORTAR_SHELL, context -> new GeoEntityRenderer<MortarShellEntity, EntityRenderState>(
				context, JugcraftDreadnought.MORTAR_SHELL).withRenderLayer(AutoGlowingGeoLayer::new));
	}
}
