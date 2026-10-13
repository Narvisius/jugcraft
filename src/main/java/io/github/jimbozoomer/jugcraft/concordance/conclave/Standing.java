package io.github.jimbozoomer.jugcraft.concordance.conclave;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A player's standing in the Starbound Conclave (roadmap step 23): whether they have sworn its oath and when, when they
 * last contributed (their obligation), their renown in all and by tradition, the kinds of contribution they have made,
 * how many times each contribution was recognised ({@code awarded}, by key) and when each recurring one (a commission,
 * a practice) was last done ({@code recent}). Renown is standing, never a currency: nothing spends it.
 */
public record Standing(boolean member, long joined, long lastContribution, int renown, Map<String, Integer> traditions, Set<String> kinds,
		Map<String, Integer> awarded, Map<String, Long> recent) {
	public static final Standing NONE = new Standing(false, 0L, 0L, 0, Map.of(), Set.of(), Map.of(), Map.of());

	public Standing {
		traditions = Collections.unmodifiableMap(new TreeMap<>(traditions));
		kinds = Collections.unmodifiableSet(new TreeSet<>(kinds));
		awarded = Collections.unmodifiableMap(new TreeMap<>(awarded));
		recent = Collections.unmodifiableMap(new TreeMap<>(recent));
	}

	public int awarded(String key) {
		return awarded.getOrDefault(key, 0);
	}

	/** When {@code key} was last done, or {@link Long#MIN_VALUE} if never. */
	public long recent(String key) {
		return recent.getOrDefault(key, Long.MIN_VALUE);
	}

	public int tradition(String tradition) {
		return traditions.getOrDefault(tradition, 0);
	}

	/** This standing with the oath sworn at {@code now}. */
	public Standing sworn(long now) {
		return new Standing(true, now, now, renown, traditions, kinds, awarded, recent);
	}

	/** This standing with one more recognised contribution: its renown, tradition, kind and key, at {@code now}. */
	public Standing add(Kind kind, String tradition, String key, int points, long now) {
		Map<String, Integer> byTradition = new TreeMap<>(traditions);
		byTradition.merge(tradition, points, Integer::sum);
		Set<String> allKinds = new TreeSet<>(kinds);
		allKinds.add(kind.id);
		Map<String, Integer> counts = new TreeMap<>(awarded);
		counts.merge(key, 1, Integer::sum);
		return new Standing(member, joined, now, Math.addExact(renown, points), byTradition, allKinds, counts, recent);
	}

	/** This standing having contributed at {@code now} without renown yet (a share of a project): its obligation met. */
	public Standing contributed(long now) {
		return member ? new Standing(true, joined, Math.max(lastContribution, now), renown, traditions, kinds, awarded, recent) : this;
	}

	/** This standing with {@code key} counted once more without renown (a limit's tally). */
	public Standing count(String key) {
		Map<String, Integer> counts = new TreeMap<>(awarded);
		counts.merge(key, 1, Integer::sum);
		return new Standing(member, joined, lastContribution, renown, traditions, kinds, counts, recent);
	}

	/** This standing with {@code key} last done at {@code now}. */
	public Standing did(String key, long now) {
		Map<String, Long> times = new TreeMap<>(recent);
		times.put(key, now);
		return new Standing(member, joined, lastContribution, renown, traditions, kinds, awarded, times);
	}
}
