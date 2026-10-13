package io.github.jimbozoomer.jugcraft.concordance.artifice;

import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * One forged artifice's properties (roadmap step 19), saved on its item: its substrate, the quality and seed rolled when
 * it was forged (on the server, and saved before anyone saw them), how many times it has been reforged, its affixes,
 * runes and socketed gems (in socket order), and the player it is bonded to, if any. Reforging rolls from the seed and
 * the next count, so nothing a player does short of reforging (previewing, cancelling, reconnecting, reopening) ever
 * changes what the next roll will be. Immutable.
 */
public record Artifice(String substrate, Quality quality, long seed, int reforges, List<RolledAffix> affixes, List<String> runes,
		List<String> gems, @Nullable UUID bond) {
	public Artifice {
		affixes = List.copyOf(affixes);
		runes = List.copyOf(runes);
		gems = List.copyOf(gems);
	}

	Artifice withAffixes(List<RolledAffix> next) {
		return new Artifice(substrate, quality, seed, reforges, next, runes, gems, bond);
	}

	Artifice withRunes(List<String> next) {
		return new Artifice(substrate, quality, seed, reforges, affixes, next, gems, bond);
	}

	Artifice withGems(List<String> next) {
		return new Artifice(substrate, quality, seed, reforges, affixes, runes, next, bond);
	}

	Artifice withBond(@Nullable UUID next) {
		return new Artifice(substrate, quality, seed, reforges, affixes, runes, gems, next);
	}
}
