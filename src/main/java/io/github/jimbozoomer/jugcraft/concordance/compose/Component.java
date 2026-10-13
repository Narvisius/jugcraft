package io.github.jimbozoomer.jugcraft.concordance.compose;

import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.rules.Definitions;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * One word of the composition grammar, as loaded from {@code data/<ns>/concordance/component/<name>.json}: its slot,
 * the research state that lets a player use it, the instrument capacity it takes, the Focus it adds to a cast, and
 * what it does ({@link #part()}). The codex pages describing components are generated from the same tables in
 * tools/concordance.py, so the page and the rule cannot disagree.
 * <p>
 * An {@code authored} component belongs to an authored invocation (roadmap step 10): invocations are written in the
 * same grammar and compiled under the same limits, but a player cannot compose with these words themselves.
 */
public record Component(String id, Slot slot, Definitions.Requirement requires, int capacity, int focus, boolean authored, Part part) {
	public Text.Ref ref() {
		return Text.component(id);
	}

	/** What a component does; one kind per slot. */
	public sealed interface Part permits Delivery, Selection, Operation, Modifier, Termination {
	}

	/** How the spell leaves its caster. */
	public enum Form {
		/** Where the caster stands; after {@code then}, where the spell before it landed. */
		HERE,
		/** The first creature or surface within reach. */
		TOUCH,
		/** A straight line: the first creature or surface within range. */
		RAY;

		public String id() {
			return name().toLowerCase(Locale.ROOT);
		}

		public static @Nullable Form fromId(String id) {
			for (Form form : values()) {
				if (form.id().equals(id)) {
					return form;
				}
			}
			return null;
		}
	}

	/** @param range blocks (0 for {@link Form#HERE}) */
	public record Delivery(Form form, int range) implements Part {
	}

	/** What the spell chooses where it lands. */
	public enum Pick {
		/** The one thing the delivery reached: the creature, or else the block. */
		STRUCK,
		/** Living creatures within the radius, nearest first. */
		CREATURES,
		/** Blocks within the radius that the operation can act on, nearest first. */
		BLOCKS,
		/** The caster and the players in the caster's party within the radius, nearest first: for helpful operations. */
		ALLIES;

		public String id() {
			return name().toLowerCase(Locale.ROOT);
		}

		public static @Nullable Pick fromId(String id) {
			for (Pick pick : values()) {
				if (pick.id().equals(id)) {
					return pick;
				}
			}
			return null;
		}

		/** Whether operations on {@code on} can use this selection. */
		public boolean suits(EffectKind.On on) {
			return switch (this) {
				case STRUCK -> true;
				case CREATURES, ALLIES -> on == EffectKind.On.CREATURE;
				case BLOCKS -> on == EffectKind.On.BLOCK;
			};
		}
	}

	/**
	 * @param radius blocks around the landing point (0 for {@link Pick#STRUCK})
	 * @param targets the most it chooses
	 */
	public record Selection(Pick pick, int radius, int targets) implements Part {
	}

	/**
	 * @param principle the Principle it works with (it names the damage school, through tools/concordance.py)
	 * @param scaling damage added for each point of the caster's Spell Power in the effect's school above the base
	 *     (damage only; 0 for none). Applied once, by the cast that runs the plan ({@link Plan#scaled}).
	 */
	public record Operation(EffectSpec effect, String principle, double scaling) implements Part {
	}

	/** What a modifier changes. */
	public enum Aspect {
		/** An operation's strength, by a percentage (at least one more). */
		MAGNITUDE(Slot.OPERATION),
		/** An operation's time, by a percentage. */
		DURATION(Slot.OPERATION),
		/** A selection's radius, in blocks. */
		RADIUS(Slot.SELECTION),
		/** A delivery's range, in blocks. */
		RANGE(Slot.DELIVERY);

		public final Slot modifies;

		Aspect(Slot modifies) {
			this.modifies = modifies;
		}

		public String id() {
			return name().toLowerCase(Locale.ROOT);
		}

		public Text.Ref ref() {
			return new Text.Ref("aspect", id());
		}

		public static @Nullable Aspect fromId(String id) {
			for (Aspect aspect : values()) {
				if (aspect.id().equals(id)) {
					return aspect;
				}
			}
			return null;
		}
	}

	/** @param amount a percentage for magnitude and duration, blocks for radius and range */
	public record Modifier(Aspect aspect, int amount) implements Part {
	}

	/**
	 * @param pulses how many times the spell acts at the same place (1: once)
	 * @param interval ticks between pulses
	 */
	public record Termination(int pulses, int interval) implements Part {
	}
}
