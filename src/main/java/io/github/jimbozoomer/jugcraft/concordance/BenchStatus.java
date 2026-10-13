package io.github.jimbozoomer.jugcraft.concordance;

import net.minecraft.network.chat.Component;

/**
 * Whether one of the Lampwright's Bench's actions can run for the player looking at it, and if not why. The server
 * works it out for each player with the bench open and sends it in the menu's data, so the screen can grey out a
 * button and say what it needs; pressing the button makes the server work it out again before anything happens.
 */
public enum BenchStatus {
	READY(""),
	BUSY("busy"),
	NO_SPECIMEN("no_specimen"),
	NOT_SPECIMEN("not_specimen"),
	NOTHING_TO_LEARN("nothing_to_learn"),
	UNKNOWN_RESEARCH("unknown_research"),
	NO_LANTERN("no_lantern"),
	NO_KINDLED_LANTERN("no_kindled_lantern"),
	NO_AMETHYST("no_amethyst"),
	OVERFULL("overfull"),
	NO_FOCUS("no_focus"),
	DISABLED("disabled");

	private final String key;

	BenchStatus(String key) {
		this.key = key;
	}

	public boolean ready() {
		return this == READY;
	}

	public static BenchStatus fromIndex(int index) {
		BenchStatus[] values = values();
		return index >= 0 && index < values.length ? values[index] : DISABLED;
	}

	/** What the player is told; {@code focus} fills in the Focus a channel needs. */
	public Component message(int focus) {
		return this == NO_FOCUS ? Component.translatable("message.jugcraft.concordance.bench.no_focus", focus)
				: Component.translatable("message.jugcraft.concordance.bench." + key);
	}
}
