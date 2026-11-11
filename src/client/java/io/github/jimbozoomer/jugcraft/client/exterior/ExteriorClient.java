package io.github.jimbozoomer.jugcraft.client.exterior;

import io.github.jimbozoomer.jugcraft.exterior.JugcraftExterior;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;

/** The realistic exterior's client side: the steel stairs' stringers and handrails. */
public final class ExteriorClient {
	private ExteriorClient() {
	}

	public static void register() {
		BlockEntityRendererRegistry.register(JugcraftExterior.STAIR_ENTITY, StairRenderer::new);
	}
}
