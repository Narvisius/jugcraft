package io.github.jimbozoomer.jugcraft.concordance.resource;

/**
 * An explicit conversion recipe: {@code fromAmount} of one type becomes {@code toAmount} of another, in whole batches.
 * Data-defined ({@code data/<ns>/concordance/conversion/}); nothing converts without one. {@link ConversionTable}
 * refuses recipes that would let a loop of conversions gain or break even.
 */
public record Conversion(String id, ResourceType from, long fromAmount, ResourceType to, long toAmount) {
	public Conversion {
		if (from.equals(to)) {
			throw new IllegalArgumentException("a conversion must change the type");
		}
		if (!from.kind().fungible || !to.kind().fungible) {
			throw new IllegalArgumentException("only amounts convert (Bound Will records never do)");
		}
		if (fromAmount < 1 || toAmount < 1) {
			throw new IllegalArgumentException("amounts must be at least 1");
		}
	}

	/** How many whole batches fit: limited by what is available, the room for the result and {@code maxBatches}. */
	public long batches(long available, long space, long maxBatches) {
		if (available < 0 || space < 0 || maxBatches < 0) {
			throw new IllegalArgumentException("negative amount");
		}
		return Math.min(maxBatches, Math.min(available / fromAmount, space / toAmount));
	}
}
