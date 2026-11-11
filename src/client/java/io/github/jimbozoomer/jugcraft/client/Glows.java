package io.github.jimbozoomer.jugcraft.client;

import net.minecraft.util.Mth;

/**
 * Glows drawn on the eyes layer. In 26.x that layer lays its picture over the scene by its opacity; it does not add
 * light to it (RenderPipelines.EYES blends TRANSLUCENT). So a glow's strength has to be its opacity, in the colour it
 * is drawn in and in its texture alike: a texture that is bright on black with no see-through part draws its black
 * (the plasma bolt was a black lump in the game for that, Justin's playtest, 10 November 2026; its textures now carry
 * their brightness as opacity, tools/glow_alpha.py, which also checks every glow texture for it).
 */
public final class Glows {
	private Glows() {
	}

	/**
	 * Light of colour r, g, b at strength k, as the colour to draw it in (ARGB): its hue at full brightness, as opaque
	 * as it is strong. Over the dark it looks as added light would; over the bright it tints instead of darkening.
	 */
	public static int added(float r, float g, float b, float k) {
		float m = Math.max(r, Math.max(g, b));
		if (m <= 0 || k <= 0) {
			return 0;
		}
		int a = (int) (Mth.clamp(k * m, 0, 1) * 255);
		return a << 24 | (int) (Mth.clamp(r / m, 0, 1) * 255) << 16 | (int) (Mth.clamp(g / m, 0, 1) * 255) << 8 | (int) (Mth.clamp(b / m, 0, 1) * 255);
	}
}
