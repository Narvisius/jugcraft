package io.github.jimbozoomer.jugcraft.concordance.artifice;

import java.util.List;

/**
 * A material an artifice is forged from (roadmap step 19, data: concordance/substrate): the item and how many of it
 * forge one; how many come back from salvage (always fewer, so forging and salvaging never gain); its resonance
 * capacity, sockets and rune slots; its durability and how much one of its items repairs; and the affixes that can be
 * rolled on it, in the order the roll draws from.
 */
public record Substrate(String id, String item, int cost, int salvage, int capacity, int sockets, int runes, int durability, int repair,
		List<String> affixes) {
}
