package io.github.jimbozoomer.jugcraft.concordance.compose;

import java.util.List;

/**
 * A line of player-facing text from the compiler (a problem or a line of explanation), kept as a translation key and
 * its arguments so the game can show it in the player's language. The key is {@code compose.jugcraft.<key>};
 * arguments are numbers, plain strings (seconds, already formatted) or {@link Ref}s to named things.
 */
public record Text(String key, List<Object> args) {
	public static final String PREFIX = "compose.jugcraft.";

	public Text {
		args = List.copyOf(args);
	}

	public static Text of(String key, Object... args) {
		return new Text(key, List.of(args));
	}

	public String translationKey() {
		return PREFIX + key;
	}

	/**
	 * A named thing, shown by its translated name: a {@code component}, {@code research} entry, research {@code state},
	 * grammar {@code slot}, modifier {@code aspect}, {@code effect} operation, {@code status} effect or {@code item}.
	 */
	public record Ref(String kind, String id) {
	}

	public static Ref component(String id) {
		return new Ref("component", id);
	}

	/** Ticks as seconds, without a trailing ".0": 30 gives "1.5", 40 gives "2". */
	public static String seconds(int ticks) {
		return ticks % 20 == 0 ? Integer.toString(ticks / 20) : Double.toString(ticks / 20.0).replaceAll("0+$", "");
	}
}
