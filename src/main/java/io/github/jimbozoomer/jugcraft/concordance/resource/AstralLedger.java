package io.github.jimbozoomer.jugcraft.concordance.resource;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Which celestial events have paid Astral Resonance (roadmap steps 7 and 15), so that one alignment never pays twice
 * however the world's clock is moved. For each pattern it keeps the latest occurrence claimed and the game time of the
 * claim, and a new claim must pass two tests:
 * <ol>
 * <li>its occurrence must be later than the last one claimed ({@link Outcome#ALREADY_CLAIMED} otherwise): turning the
 * clock back brings back only occurrences that have paid;</li>
 * <li>at least {@code minGap} game ticks must have passed since the last claim ({@link Outcome#TOO_SOON} otherwise):
 * game time runs only forward with the world and no command moves it, so turning the clock forward skips to a later
 * occurrence but cannot make it pay sooner than the world has actually run.</li>
 * </ol>
 * Immutable: a claim returns the ledger after it. The occurrences are counted from day zero of the world's clock, so
 * they are the same on every client, after every save, for every observer.
 */
public record AstralLedger(Map<String, Claim> claims) {
	public static final AstralLedger EMPTY = new AstralLedger(Map.of());

	public AstralLedger {
		claims = Collections.unmodifiableMap(new TreeMap<>(claims));
	}

	/** The latest occurrence of a pattern that paid, and the game time it did. */
	public record Claim(long occurrence, long gameTime) {
	}

	public enum Outcome {
		GRANTED, ALREADY_CLAIMED, TOO_SOON
	}

	public record Result(AstralLedger ledger, Outcome outcome) {
	}

	public Result claim(String pattern, long occurrence, long gameTime, long minGap) {
		Claim last = claims.get(pattern);
		if (last != null) {
			if (occurrence <= last.occurrence()) {
				return new Result(this, Outcome.ALREADY_CLAIMED);
			}
			if (gameTime < last.gameTime() || gameTime - last.gameTime() < minGap) {
				return new Result(this, Outcome.TOO_SOON);
			}
		}
		Map<String, Claim> copy = new TreeMap<>(claims);
		copy.put(pattern, new Claim(occurrence, gameTime));
		return new Result(new AstralLedger(copy), Outcome.GRANTED);
	}

	/** Whether this pattern has ever paid (so it may be recalled). */
	public boolean claimed(String pattern) {
		return claims.containsKey(pattern);
	}
}
