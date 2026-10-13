package io.github.jimbozoomer.jugcraft.concordance.compose;

/**
 * What an instrument can hold, from {@code data/<ns>/concordance/instrument/<name>.json}: the capacity its
 * components may take, and the most a spell inscribed on it may do (distinct targets, work, triggered branches and
 * ticks of duration). Each is at most the grammar's absolute limit ({@link Grammar}), which no data can raise.
 */
public record Instrument(String id, String item, int capacity, int targets, int work, int branches, int duration) {
	public Text.Ref ref() {
		return new Text.Ref("item", item);
	}
}
