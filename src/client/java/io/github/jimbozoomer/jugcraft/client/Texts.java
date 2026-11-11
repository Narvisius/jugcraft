package io.github.jimbozoomer.jugcraft.client;

import java.util.HashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Plain strings laid out for drawing ({@code Texts.of(s)}), kept so the displays don't
 * lay out the same words again every frame (Justin's JFR, 8 November 2026: the busiest garbage after the holo buffers).
 * The same text every time; at most {@link #MAX} kept (all dropped when full, so changing numbers don't pile up).
 *
 * <p>(From r186 to r196 this laid a string out by calling itself: the replace that moved every display over to
 * {@code Texts.of} rewrote the two lines in here too, so the first display drawn overflowed the stack and the game
 * closed. Justin's first run since r180, 10 November 2026; fixed in r197.)
 */
public final class Texts {
	static final int MAX = 4096;
	private static final HashMap<String, FormattedCharSequence> CACHE = new HashMap<>();

	private Texts() {
	}

	/** {@code s} laid out for drawing. */
	public static FormattedCharSequence of(String s) {
		if (!Minecraft.getInstance().isSameThread()) {
			return Component.literal(s).getVisualOrderText();
		}
		FormattedCharSequence t = CACHE.get(s);
		if (t == null) {
			if (CACHE.size() >= MAX) {
				CACHE.clear();
			}
			t = Component.literal(s).getVisualOrderText();
			CACHE.put(s, t);
		}
		return t;
	}
}
