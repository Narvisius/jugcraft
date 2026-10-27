package io.github.jimbozoomer.jugcraft.concordance.diagnostic;

import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Why something waits or refuses (plan 03:9; Concordance Chunk C, C-01, C-P6): a stable {@link Code} and its
 * parameters, never prose. Worded through {@link #text()} (compose.jugcraft.diagnostic.<id>), which ComposeText turns
 * into a translatable component on either side; carried to a client as {@link #encode()}d text (Jade's snapshot) and
 * read back by {@link #decode}, never worked out again there (03:9). A parameter is a whole number, a short string, a
 * named thing ({@link Text.Ref}) or a line ({@link Text}) of those; at most {@value #MAX_ARGS}. Pure Java.
 */
public record Diagnostic(Code code, List<Object> args) {
	public static final String PREFIX = "diagnostic.";
	public static final int MAX_ARGS = 4;
	/** The longest encoding sent (Jade); a longer one is sent as its code alone. */
	public static final int MAX_ENCODED = 1024;
	/** The longest string parameter sent. */
	public static final int MAX_WORD = 64;

	public Diagnostic {
		Objects.requireNonNull(code, "code");
		args = List.copyOf(args);
		if (args.size() > MAX_ARGS) {
			throw new IllegalArgumentException(code.id + ": at most " + MAX_ARGS + " parameters");
		}
		for (Object arg : args) {
			if (!plain(arg) && !(arg instanceof Text line && line.args().size() <= MAX_ARGS && line.args().stream().allMatch(Diagnostic::plain))) {
				throw new IllegalArgumentException(code.id + ": a parameter is a number, a string, a named thing or a line of those: " + arg);
			}
		}
	}

	private static boolean plain(Object arg) {
		return arg instanceof Integer || arg instanceof String || arg instanceof Text.Ref;
	}

	public static Diagnostic of(Code code, Object... args) {
		return new Diagnostic(code, List.of(args));
	}

	/** In the player's words: compose.jugcraft.diagnostic.<id>, with the parameters. */
	public Text text() {
		return new Text(PREFIX + code.id, args);
	}

	public static Diagnostic missingIngredient(int count, Text what) {
		return of(Code.MISSING_INGREDIENT, count, what);
	}

	public static Diagnostic wrongPreparation(Text wanted, Text found) {
		return of(Code.WRONG_PREPARATION, wanted, found);
	}

	public static Diagnostic wrongHeat(String wanted, String now) {
		return of(Code.WRONG_HEAT, band(wanted), band(now));
	}

	public static Diagnostic needsWater(int parts, int held) {
		return of(Code.NEEDS_WATER, parts, held);
	}

	public static Diagnostic held(String band, int seconds) {
		return of(Code.HELD, band(band), seconds);
	}

	public static Diagnostic incompatible(int step, Text operation, Text reason) {
		return of(Code.INCOMPATIBLE, step, operation, reason);
	}

	/** C-06: what it lacks, and in how many seconds it asks the couriers again, their quota for its setter being full. */
	public static Diagnostic courierQuota(int count, Text what, int seconds) {
		return of(Code.COURIER_QUOTA, count, what, seconds);
	}

	private static Text.Ref band(String id) {
		return new Text.Ref("band", id);
	}

	/**
	 * For a client: "code|#n|@kind=id|'word|!key|...|." (a line's parameters follow its "!key" and "." closes it). Longer
	 * than {@value #MAX_ENCODED}, the code alone.
	 */
	public String encode() {
		StringBuilder out = new StringBuilder(code.id);
		append(out, args);
		return out.length() <= MAX_ENCODED ? out.toString() : code.id;
	}

	private static void append(StringBuilder out, List<Object> args) {
		for (Object arg : args) {
			out.append('|');
			switch (arg) {
				case Integer number -> out.append('#').append(number);
				case Text.Ref ref -> out.append('@').append(clean(ref.kind(), MAX_WORD)).append('=').append(clean(ref.id(), MAX_ENCODED));
				case Text line -> {
					out.append('!').append(clean(line.key(), MAX_WORD));
					append(out, line.args());
					out.append("|.");
				}
				default -> out.append('\'').append(clean(String.valueOf(arg), MAX_WORD));
			}
		}
	}

	private static String clean(String word, int max) {
		String out = word.replace('|', '/').replace('\n', ' ');
		return out.length() > max ? out.substring(0, max) : out;
	}

	/** Read back from encode(); null for "", an unknown code or anything malformed (a client then shows no line). */
	public static @Nullable Diagnostic decode(String encoded) {
		if (encoded.isEmpty() || encoded.length() > MAX_ENCODED) {
			return null;
		}
		String[] fields = encoded.split("\\|", -1);
		Code code = Code.fromId(fields[0]);
		if (code == null) {
			return null;
		}
		List<Object> args = new ArrayList<>();
		String key = null;
		List<Object> inner = new ArrayList<>();
		for (int i = 1; i < fields.length; i++) {
			String field = fields[i];
			if (field.equals(".")) {
				if (key == null) {
					return null;
				}
				args.add(new Text(key, inner));
				key = null;
				inner = new ArrayList<>();
				continue;
			}
			if (field.isEmpty()) {
				return null;
			}
			List<Object> into = key == null ? args : inner;
			String rest = field.substring(1);
			switch (field.charAt(0)) {
				case '#' -> {
					try {
						into.add(Integer.parseInt(rest));
					} catch (NumberFormatException bad) {
						return null;
					}
				}
				case '@' -> {
					int at = rest.indexOf('=');
					if (at <= 0) {
						return null;
					}
					into.add(new Text.Ref(rest.substring(0, at), rest.substring(at + 1)));
				}
				case '\'' -> into.add(rest);
				case '!' -> {
					if (key != null || rest.isEmpty()) {
						return null;
					}
					key = rest;
				}
				default -> {
					return null;
				}
			}
		}
		if (key != null) {
			return null;
		}
		try {
			return new Diagnostic(code, args);
		} catch (IllegalArgumentException bad) {
			return null;
		}
	}
}
