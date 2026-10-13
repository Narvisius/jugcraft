package io.github.jimbozoomer.jugcraft.concordance.artifice;

import java.util.List;

/**
 * A rune (roadmap step 19, data: concordance/rune): a fixed property inscribed with its ingredient (consumed), the
 * capacity it uses, and the substrates it can be inscribed on (empty: any). Runes last until the artifice is salvaged.
 */
public record Rune(String id, String item, Stat stat, int cost, List<String> substrates) {
}
