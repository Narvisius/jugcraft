package io.github.jimbozoomer.jugcraft.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * The tower's glow layers without the per-frame sort (Justin's frame rate, 7 November 2026, his pick "Unsorted glows"):
 * the game's translucent emissive type sorts every one of its quads back to front before each frame is drawn, and with
 * the hologram's 35,000 quads, the trees' crystals and the screens' glows that sorting made most of the render thread's
 * garbage (about 290 MB a second) and the stutters when it was collected. This is the same type (the same pipeline,
 * blending, no depth writes, its order-independent pipelines) without the sort: glows are drawn in the order they were
 * made, which for light that doesn't hide what's behind it looks the same, overlaps a shade different at most.
 */
public final class GlowTypes {
	private static final Map<Identifier, RenderType> GLOW = new ConcurrentHashMap<>();

	private GlowTypes() {
	}

	/** {@code RenderTypes.entityTranslucentEmissive(texture)}, unsorted. */
	public static RenderType glow(Identifier texture) {
		return GLOW.computeIfAbsent(texture, t -> RenderType.create("jugcraft_glow_unsorted",
				RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE).setOitPipelines(RenderPipelines.OIT_ENTITY_EMISSIVE)
						.withTexture("Sampler0", t).useOverlay().createRenderSetup()));
	}
}
