package io.github.jimbozoomer.jugcraft.concordance.resource;

import java.math.BigInteger;

/**
 * The Prima Materia value of one unit of an eligible material, as an exact fraction of grains
 * ({@code numerator / denominator}). Values are added up exactly and rounded down once, on the total, so splitting a
 * batch never earns more than the whole and nothing is ever rounded up into existence.
 */
public record PrimaValue(long numerator, long denominator) {
	public PrimaValue {
		if (numerator < 0 || denominator < 1) {
			throw new IllegalArgumentException("bad Prima Materia value");
		}
	}

	/** The whole grains {@code count} units are worth, rounded down. */
	public long grains(long count) {
		if (count < 0) {
			throw new IllegalArgumentException("negative count");
		}
		return BigInteger.valueOf(numerator).multiply(BigInteger.valueOf(count)).divide(BigInteger.valueOf(denominator))
				.longValueExact();
	}
}
