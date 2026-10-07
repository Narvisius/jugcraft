package io.github.jimbozoomer.jugcraft.gear;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * A piece of gear's traits in its tooltip (docs/features/trait-details.md): each trait's name always, and while Shift is
 * held a brief description under it, wrapped to a narrow column. Without Shift, one dim line says to hold it, so a
 * tooltip stays short until the player asks for more.
 * <p>Items call it from {@code appendHoverText}, which only the client runs for a tooltip. This class is shared, so it
 * cannot read the keyboard itself: the client sets {@link #details} (JugcraftClient), and it is off everywhere else.
 * <pre>
 * TraitTooltips traits = TraitTooltips.of(tooltip);
 * traits.trait("tooltip.jugcraft.arms.dagger.trait", ChatFormatting.YELLOW, Component.translatable("tooltip.jugcraft.arms.dagger"));
 * traits.end();
 * </pre>
 */
public final class TraitTooltips {
	/** Whether descriptions are shown: the client sets this to "is Shift held"; elsewhere it stays off. */
	public static BooleanSupplier details = () -> false;
	/** The widest a description line runs, in characters, before it wraps at a space. */
	public static final int WIDTH = 38;
	/** A description line's indent under its trait's name. */
	private static final String INDENT = "  ";

	private final Consumer<Component> tooltip;
	private final boolean expanded;
	private boolean folded;

	private TraitTooltips(Consumer<Component> tooltip, boolean expanded) {
		this.tooltip = tooltip;
		this.expanded = expanded;
	}

	/** Starts a tooltip's traits, expanded if Shift is held now. */
	public static TraitTooltips of(Consumer<Component> tooltip) {
		return new TraitTooltips(tooltip, details.getAsBoolean());
	}

	/** Starts a tooltip's traits, expanded or not whatever the keyboard says (for tests). */
	public static TraitTooltips of(Consumer<Component> tooltip, boolean expanded) {
		return new TraitTooltips(tooltip, expanded);
	}

	/**
	 * A trait: its name (a translation key) in its colour, and, while Shift is held, each description under it, wrapped
	 * and indented in grey. A trait with no description is only its name.
	 */
	public TraitTooltips trait(String nameKey, ChatFormatting colour, Component... descriptions) {
		tooltip.accept(Component.translatable(nameKey).withStyle(colour));
		if (descriptions.length == 0) {
			return this;
		}
		if (!expanded) {
			folded = true;
			return this;
		}
		for (Component description : descriptions) {
			for (String line : wrap(description.getString(), WIDTH)) {
				tooltip.accept(Component.literal(INDENT + line).withStyle(ChatFormatting.GRAY));
			}
		}
		return this;
	}

	/** Ends the traits: without Shift, a dim line saying to hold it, if any trait has a description to show. */
	public void end() {
		if (folded) {
			tooltip.accept(Component.translatable("tooltip.jugcraft.hold_shift",
					Component.literal("Shift").withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY));
		}
	}

	/** Whether this tooltip shows descriptions (Shift held when it started). */
	public boolean expanded() {
		return expanded;
	}

	/**
	 * Splits text into lines of at most {@code width} characters, breaking at spaces; a single word longer than the
	 * width is cut. Runs of spaces collapse. Empty text gives no lines.
	 */
	public static List<String> wrap(String text, int width) {
		List<String> lines = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.trim().split("\\s+")) {
			if (word.isEmpty()) {
				continue;
			}
			while (word.length() > width) {
				if (!line.isEmpty()) {
					lines.add(line.toString());
					line.setLength(0);
				}
				lines.add(word.substring(0, width));
				word = word.substring(width);
			}
			if (!line.isEmpty() && line.length() + 1 + word.length() > width) {
				lines.add(line.toString());
				line.setLength(0);
			}
			if (!line.isEmpty()) {
				line.append(' ');
			}
			line.append(word);
		}
		if (!line.isEmpty()) {
			lines.add(line.toString());
		}
		return lines;
	}
}
