package io.github.jimbozoomer.jugcraft.concordance.alchemy;

import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * One step of an alchemical process, in the canonical text a formula records: {@code water <parts>},
 * {@code add <item> <preparation>} or {@code stir <band>}. A mixture's history is its operations in order, and
 * replaying them from nothing makes the same mixture.
 */
public sealed interface Operation {
	Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

	String text();

	/** Pours in {@code parts} parts of water (a bottle each; a bucket is three). */
	record Water(int parts) implements Operation {
		public Water {
			if (parts < 1 || parts > Mixture.MAX_PARTS) {
				throw new IllegalArgumentException("water 1.." + Mixture.MAX_PARTS);
			}
		}

		@Override
		public String text() {
			return "water " + parts;
		}
	}

	/** Adds one of an ingredient (by item id), prepared as {@code preparation}. */
	record Add(String item, String preparation) implements Operation {
		public Add {
			if (!ID.matcher(item).matches() || !ID.matcher(preparation).matches()) {
				throw new IllegalArgumentException("add needs an item id and a preparation id");
			}
		}

		@Override
		public String text() {
			return "add " + item + " " + preparation;
		}
	}

	/** Stirs once at the temperature band the mixture is in. */
	record Stir(Band band) implements Operation {
		@Override
		public String text() {
			return "stir " + band.id;
		}
	}

	/** Reads an operation from its canonical text, or null if it is not one. */
	static @Nullable Operation parse(String text) {
		String[] words = text.trim().split(" ");
		try {
			return switch (words[0]) {
				case "water" -> words.length == 2 ? new Water(Integer.parseInt(words[1])) : null;
				case "add" -> words.length == 3 ? new Add(words[1], words[2]) : null;
				case "stir" -> words.length == 2 && Band.fromId(words[1]) != null ? new Stir(Band.fromId(words[1])) : null;
				default -> null;
			};
		} catch (IllegalArgumentException invalid) {
			return null;
		}
	}
}
