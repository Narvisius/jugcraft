package io.github.jimbozoomer.jugcraft.concordance.rules;

import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The shared definitions of the Arcane Concordance, as loaded from {@code data/<ns>/concordance/}. These are the same
 * for every player; what one player has learned is a {@link Knowledge}. tools/concordance.py writes Jugcraft's own,
 * and a data pack may add or replace them: {@link RulesParser} rejects malformed ones with a diagnostic.
 */
public final class Definitions {
	private Definitions() {
	}

	/** One prerequisite: another entry that must have reached a state before this one can progress. */
	public record Requirement(String research, ResearchState state) {
	}

	/** What reaching a state makes available. */
	public record Unlocks(List<String> invocations, List<String> workings, List<String> rituals) {
		public static final Unlocks NONE = new Unlocks(List.of(), List.of(), List.of());
	}

	/**
	 * A research entry. {@code states} maps each state, in order from {@link ResearchState#ENCOUNTERED}, to its
	 * alternatives; an entry need not define every state (one without a mastery has no {@code mastered}).
	 */
	public record Research(String id, int schema, String principle, String tradition, String stage, String icon,
			List<Requirement> requires, Map<ResearchState, List<EvidenceRule>> states, Map<ResearchState, Unlocks> unlocks) {
		public ResearchState highest() {
			ResearchState highest = ResearchState.NONE;
			for (ResearchState state : states.keySet()) {
				if (state.ordinal() > highest.ordinal()) {
					highest = state;
				}
			}
			return highest;
		}
	}

	/**
	 * An authored invocation (roadmap step 10): the Spell Engine spell it casts, the research that teaches it, its
	 * Focus cost, its tactical role, and what it does, written as a composition in the shared grammar
	 * ({@code composition}) and compiled under the same limits as a player's. {@code tunings} are the modifiers a player
	 * may join to it, one at a time, on their instrument; {@code work} and {@code persists} are the most work one cast
	 * may spend and the most ticks anything it makes may last, declared by the author and checked against every
	 * compiled form when the rules are built.
	 */
	public record Invocation(String id, int schema, String spell, String principle, String research, ResearchState state,
			int focus, int masteredFocus, Role role, String composition, List<String> tunings, int work, int persists) {
		public Invocation {
			tunings = List.copyOf(tunings);
		}

		/** The Focus it costs a player whose research has reached {@code reached}, before any tuning. */
		public int cost(ResearchState reached) {
			return reached == ResearchState.MASTERED ? masteredFocus : focus;
		}
	}

	/** What an invocation is for in a fight or an expedition. Each invocation has one. */
	public enum Role {
		DAMAGE("damage"),
		DEFENSE("defense"),
		MOVEMENT("movement"),
		SUPPORT("support"),
		INVESTIGATION("investigation"),
		UTILITY("utility");

		public final String id;

		Role(String id) {
			this.id = id;
		}

		public static @Nullable Role fromId(String id) {
			for (Role role : values()) {
				if (role.id.equals(id)) {
					return role;
				}
			}
			return null;
		}
	}

	public enum WorkingType {
		CRAFT("craft"),
		INFUSE("infuse"),
		CHANNEL("channel");

		public final String id;

		WorkingType(String id) {
			this.id = id;
		}

		public static @Nullable WorkingType fromId(String id) {
			for (WorkingType type : values()) {
				if (type.id.equals(id)) {
					return type;
				}
			}
			return null;
		}
	}

	/**
	 * A process at a station. {@code CRAFT} turns {@code work} plus one {@code specimen} into {@code result} holding
	 * {@code radiance}; {@code INFUSE} adds one specimen's value from {@code specimens} to the work; {@code CHANNEL}
	 * runs one batch of its {@code conversion} (a {@code data/<ns>/concordance/conversion} recipe), turning
	 * {@code focus} of the player's Focus into {@code radiance} in the work. For a channel, {@code focus} and
	 * {@code radiance} are copied from the conversion when the rules are built, so the recipe is the one source.
	 */
	public record Working(String id, int schema, WorkingType type, String station, String research, ResearchState state,
			String work, @Nullable String specimen, @Nullable String result, int radiance, Map<String, Integer> specimens,
			int focus, @Nullable String conversion) {
		Working withChannel(int focusCost, int radianceGain) {
			return new Working(id, schema, type, station, research, state, work, specimen, result, radianceGain, specimens,
					focusCost, conversion);
		}
	}
}
