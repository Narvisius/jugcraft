package io.github.jimbozoomer.jugcraft.concordance.equivalence;

import java.math.BigInteger;

/**
 * An exact rational number (roadmap step 21): values, gains and cycle yields are added and compared exactly, and
 * rounded only where an amount of grains is paid ({@link #floor}, {@link #ceil}). Always in lowest terms with a positive
 * denominator.
 */
public record Exact(BigInteger numerator, BigInteger denominator) implements Comparable<Exact> {
	public static final Exact ZERO = new Exact(BigInteger.ZERO, BigInteger.ONE);
	public static final Exact ONE = new Exact(BigInteger.ONE, BigInteger.ONE);

	public Exact {
		if (denominator.signum() == 0) {
			throw new ArithmeticException("zero denominator");
		}
		if (denominator.signum() < 0) {
			numerator = numerator.negate();
			denominator = denominator.negate();
		}
		BigInteger gcd = numerator.gcd(denominator);
		if (!gcd.equals(BigInteger.ONE) && gcd.signum() != 0) {
			numerator = numerator.divide(gcd);
			denominator = denominator.divide(gcd);
		}
	}

	public static Exact of(long value) {
		return new Exact(BigInteger.valueOf(value), BigInteger.ONE);
	}

	public static Exact of(long numerator, long denominator) {
		return new Exact(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
	}

	public Exact plus(Exact other) {
		return new Exact(numerator.multiply(other.denominator).add(other.numerator.multiply(denominator)), denominator.multiply(other.denominator));
	}

	public Exact minus(Exact other) {
		return plus(other.negate());
	}

	public Exact negate() {
		return new Exact(numerator.negate(), denominator);
	}

	public Exact times(Exact other) {
		return new Exact(numerator.multiply(other.numerator), denominator.multiply(other.denominator));
	}

	public Exact times(long factor) {
		return new Exact(numerator.multiply(BigInteger.valueOf(factor)), denominator);
	}

	public Exact divide(Exact other) {
		if (other.signum() == 0) {
			throw new ArithmeticException("divide by zero");
		}
		return new Exact(numerator.multiply(other.denominator), denominator.multiply(other.numerator));
	}

	public int signum() {
		return numerator.signum();
	}

	/** The greatest whole number not above it. */
	public BigInteger floor() {
		BigInteger[] parts = numerator.divideAndRemainder(denominator);
		return parts[1].signum() < 0 ? parts[0].subtract(BigInteger.ONE) : parts[0];
	}

	/** The least whole number not below it. */
	public BigInteger ceil() {
		BigInteger[] parts = numerator.divideAndRemainder(denominator);
		return parts[1].signum() > 0 ? parts[0].add(BigInteger.ONE) : parts[0];
	}

	@Override
	public int compareTo(Exact other) {
		return numerator.multiply(other.denominator).compareTo(other.numerator.multiply(denominator));
	}

	@Override
	public String toString() {
		return denominator.equals(BigInteger.ONE) ? numerator.toString() : numerator + "/" + denominator;
	}
}
