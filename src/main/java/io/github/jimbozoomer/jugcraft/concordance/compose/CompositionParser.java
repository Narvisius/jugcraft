package io.github.jimbozoomer.jugcraft.concordance.compose;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Reads the written form of a spell: words separated by spaces, each a component name with any modifiers joined by
 * {@code +}, and {@code then} before a branch. For example {@code ray+extend struck sear then here creatures dazzle}.
 * <p>
 * This is the whole language: no numbers, no variables, no loops, nothing evaluated. Anything else is refused with a
 * reason, and the input is bounded ({@value Grammar#MAX_TEXT} characters, {@value Grammar#MAX_NAMES} names) before it
 * is looked at further. A name without a namespace means a Jugcraft component.
 */
public final class CompositionParser {
	private static final Pattern NAME = Pattern.compile("[a-z0-9_]+|[a-z0-9_.-]+:[a-z0-9_./-]+");
	private static final Pattern ALLOWED = Pattern.compile("[a-z0-9_:/.+\\- ]*");
	public static final String THEN = "then";

	private CompositionParser() {
	}

	public record Parsed(@Nullable Composition composition, List<Text> problems) {
		public Parsed {
			problems = List.copyOf(problems);
		}
	}

	public static Parsed parse(@Nullable String input) {
		if (input == null || input.isBlank()) {
			return failed(Text.of("problem.empty"));
		}
		if (input.length() > Grammar.MAX_TEXT) {
			return failed(Text.of("problem.too_long", Grammar.MAX_TEXT));
		}
		String text = input.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
		if (!ALLOWED.matcher(text).matches()) {
			for (char c : text.toCharArray()) {
				if (!ALLOWED.matcher(String.valueOf(c)).matches()) {
					return failed(Text.of("problem.character", String.valueOf(c)));
				}
			}
		}
		List<List<Composition.Part>> levels = new ArrayList<>();
		levels.add(new ArrayList<>());
		int names = 0;
		for (String word : text.split(" ")) {
			if (word.equals(THEN)) {
				if (levels.getLast().isEmpty()) {
					return failed(Text.of("problem.then_empty"));
				}
				levels.add(new ArrayList<>());
				continue;
			}
			String[] pieces = word.split("\\+", -1);
			List<String> ids = new ArrayList<>();
			for (String piece : pieces) {
				if (piece.isEmpty()) {
					return failed(Text.of("problem.empty_name", word));
				}
				if (!NAME.matcher(piece).matches() || piece.equals(THEN)) {
					return failed(Text.of("problem.bad_name", piece));
				}
				ids.add(piece.indexOf(':') < 0 ? "jugcraft:" + piece : piece);
			}
			names += ids.size();
			if (names > Grammar.MAX_NAMES) {
				return failed(Text.of("problem.too_many_names", Grammar.MAX_NAMES));
			}
			levels.getLast().add(new Composition.Part(ids.getFirst(), ids.subList(1, ids.size())));
		}
		if (levels.getLast().isEmpty()) {
			return failed(Text.of("problem.then_empty"));
		}
		Composition composition = null;
		for (int i = levels.size() - 1; i >= 0; i--) {
			composition = new Composition(levels.get(i), composition);
		}
		return new Parsed(composition, List.of());
	}

	private static Parsed failed(Text problem) {
		return new Parsed(null, List.of(problem));
	}
}
