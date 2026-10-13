package io.github.jimbozoomer.jugcraft.concordance.ritual;

import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.rules.EvidenceRule;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A ritual (roadmap step 12), from {@code data/<ns>/concordance/ritual/}: what it needs, what it costs and what it
 * makes. The lifecycle every ritual shares is {@link RitualMachine}'s; this says, for one ritual:
 * <ul>
 * <li>{@code structure}: the {@link StructurePattern} built around the anchor;</li>
 * <li>{@code research} and {@code state}: what each participant must have learned;</li>
 * <li>{@code participants}: how many players take part, each paying {@code focus} when they join;</li>
 * <li>{@code steps}: how many steps of {@link RitualMachine#STEP_TICKS} ticks it runs, each drawing {@code ley}
 * Ley Charge from every channel;</li>
 * <li>{@code maxLight}: the most light there may be above the anchor (null: any);</li>
 * <li>{@code offerings}: what is placed in the anchor, reserved at the start and consumed only when it completes;</li>
 * <li>{@code result}: what completing it makes;</li>
 * <li>{@code backlash}: the damage each participant takes if containment fails while it runs.</li>
 * </ul>
 */
public record RitualDefinition(String id, int schema, String structure, String research, ResearchState state, int participants,
		int focus, int steps, int ley, @Nullable Integer maxLight, List<Offering> offerings, Result result, int backlash) {
	public static final int MAX_PARTICIPANTS = 4;
	public static final int MAX_FOCUS = 100;
	public static final int MAX_STEPS = 30;
	public static final int MAX_LEY = 16;
	/** One offering per anchor slot at most. */
	public static final int MAX_OFFERINGS = 6;
	public static final int MAX_COUNT = 64;
	public static final int MAX_BACKLASH = 20;
	public static final int MAX_GRANTS = 4;
	public static final int MAX_RADIUS = 16;
	public static final int MAX_TARGETS = 16;

	public RitualDefinition {
		offerings = List.copyOf(offerings);
	}

	/**
	 * Something to place in the anchor: {@code count} of an item, or of anything in an item tag ({@code tag}).
	 */
	public record Offering(String item, boolean tag, int count) {
		public boolean matches(String itemId, EvidenceRule.TagLookup tags) {
			return tag ? tags.isIn(itemId, item) : item.equals(itemId);
		}

		/** As written in data: the id, or {@code #} and the tag. */
		public String spec() {
			return tag ? "#" + item : item;
		}
	}

	public sealed interface Result permits Transform, Effects {
	}

	/**
	 * The offering {@code from} (an item offered once) becomes {@code into}, keeping what was inscribed and tuned on it.
	 * The result waits in the anchor until a participant takes it.
	 */
	public record Transform(String from, String into) implements Result {
	}

	/** Effects applied once, through the shared effect executor, when the ritual completes. */
	public record Effects(List<Grant> grants) implements Result {
		public Effects {
			grants = List.copyOf(grants);
		}
	}

	public enum Target {
		/** Every participant. */
		PARTICIPANTS("participants"),
		/** Creatures (not players) within {@code radius} of the anchor, nearest first, at most {@code targets}. */
		CREATURES("creatures");

		public final String id;

		Target(String id) {
			this.id = id;
		}

		public static @Nullable Target fromId(String id) {
			for (Target target : values()) {
				if (target.id.equals(id)) {
					return target;
				}
			}
			return null;
		}
	}

	/** One effect and who it reaches. {@code principle} colours how it is shown. */
	public record Grant(Target target, int radius, int targets, EffectSpec effect, String principle) {
	}

	/** Ley Charge one complete run draws from a structure with {@code channels} channels. */
	public long leyPerRun(int channels) {
		return (long) ley * steps * channels;
	}

	/** How long it channels, in ticks. */
	public int ticks() {
		return steps * RitualMachine.STEP_TICKS;
	}

	/** Items offered, counted (for choosing between rituals whose offerings both match: the more specific wins). */
	public int offered() {
		int total = 0;
		for (Offering offering : offerings) {
			total += offering.count();
		}
		return total;
	}
}
