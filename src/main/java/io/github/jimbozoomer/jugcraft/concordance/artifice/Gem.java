package io.github.jimbozoomer.jugcraft.concordance.artifice;

/** A gem (roadmap step 19, data: concordance/gem): the item set in a socket, what it gives and the capacity it uses. */
public record Gem(String id, String item, Stat stat, int cost) {
}
