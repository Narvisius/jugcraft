package io.github.jimbozoomer.jugcraft.concordance.alchemy;

import org.jspecify.annotations.Nullable;

/**
 * How an ingredient is prepared before it goes in (data: {@code data/<ns>/concordance/preparation/}): an explicit
 * transformation of its properties. The prepared amount is the ingredient's times {@code scale} per thousand; of that,
 * {@code ready} per thousand is dissolved the moment it goes in and the rest waits to be stirred out; and
 * {@code contaminant} milli-units are added. {@code tool} is the item that prepares it (null: used as it comes).
 */
public record Preparation(String id, @Nullable String tool, long scale, long ready, long contaminant) {
	public Preparation {
		if (scale < 0 || scale > 2000 || ready < 0 || ready > 1000 || contaminant < 0) {
			throw new IllegalArgumentException("scale 0..2000, ready 0..1000 per thousand, contaminant not negative");
		}
	}
}
