package io.github.jimbozoomer.jugcraft.concordance.ritual;

import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * The lifecycle every ritual shares, as pure decisions the anchor carries out (roadmap step 12).
 * <p>
 * <b>Phases.</b> {@link Phase#IDLE} → (the leader starts it) {@link Phase#GATHERING} → (every participant has joined)
 * {@link Phase#CHANNELING} → (the last step) {@link Phase#COMPLETE} until its result is taken, then IDLE again. A
 * ritual whose result is effects returns to IDLE as it completes. Any interruption returns it to IDLE.
 * <p>
 * <b>The outcome contract.</b>
 * <ul>
 * <li>Offerings are <i>reserved</i> when the leader starts: the anchor locks its slots, so nothing can be added or
 * taken until the ritual ends.</li>
 * <li>Focus is paid by each participant as they join; Ley Charge is drawn from every channel at each step, all
 * channels or none. Neither is ever returned: they are what an attempt costs.</li>
 * <li>Offerings are <i>consumed</i> and the result is <i>committed</i> together, in one server tick, at the last step,
 * and nowhere else. A transformed item waits in the anchor's output slot.</li>
 * <li>An <i>interruption</i> at any point before that releases the reserved offerings (they stay in the anchor, or
 * drop where it stood if it was broken), commits nothing, and keeps what was spent; a containment failure also
 * deals backlash. See {@link Interruption#consequence()}.</li>
 * </ul>
 * So no path both returns the offerings and commits the result, and no path commits twice: the anchor leaves
 * CHANNELING in the same tick it commits.
 */
public final class RitualMachine {
	/** Ticks between steps (and between structure checks while a ritual runs). */
	public static final int STEP_TICKS = 40;
	/** How long the other participants have to join once the leader has started. */
	public static final int GATHER_TICKS = 600;
	/** How far (horizontally, from the anchor's centre) a participant may stand, beyond the structure's own reach. */
	public static final int PARTICIPANT_MARGIN = 4;
	/** How far above or below the anchor a participant may stand. */
	public static final int PARTICIPANT_HEIGHT = 3;

	private RitualMachine() {
	}

	public enum Phase {
		IDLE("idle"),
		GATHERING("gathering"),
		CHANNELING("channeling"),
		COMPLETE("complete");

		public final String id;

		Phase(String id) {
			this.id = id;
		}

		/** Whether offerings are reserved (slots locked) in this phase. */
		public boolean reserving() {
			return this == GATHERING || this == CHANNELING;
		}

		public static Phase fromId(String id) {
			for (Phase phase : values()) {
				if (phase.id.equals(id)) {
					return phase;
				}
			}
			return IDLE;
		}
	}

	private static final Map<Phase, Set<Phase>> TRANSITIONS = Map.of(
			Phase.IDLE, Set.of(Phase.GATHERING),
			Phase.GATHERING, Set.of(Phase.CHANNELING, Phase.IDLE),
			Phase.CHANNELING, Set.of(Phase.COMPLETE, Phase.IDLE),
			Phase.COMPLETE, Set.of(Phase.IDLE));

	/** Whether the machine ever moves from one phase straight to the other. */
	public static boolean allowed(Phase from, Phase to) {
		return TRANSITIONS.get(from).contains(to);
	}

	/** Where the reserved offerings go when a ritual is interrupted. */
	public enum Release {
		/** They stay in the anchor, unlocked, for the players to take back or try again with. */
		KEPT,
		/** The anchor is gone: they drop where it stood, once. */
		DROPPED
	}

	/**
	 * What an interruption does. For every interruption the result is not committed, and the Focus and Ley Charge
	 * already spent stay spent.
	 */
	public record Consequence(Release offerings, boolean backlash) {
	}

	/** Why a ritual stopped before completing. Every way a ritual can stop is one of these. */
	public enum Interruption {
		/** A channel or clearance is missing, wrong, obstructed or someone else's. */
		STRUCTURE("structure"),
		/** A boundary part is missing or wrong while the working runs: containment fails. */
		CONTAINMENT("containment"),
		/** A channel holds less Ley Charge than the step draws. */
		POWER("power"),
		/** A participant left (disconnected, died, moved away or to another dimension), or not all gathered in time. */
		PARTICIPANTS("participants"),
		/** A condition no longer holds (the light rose above the ritual's limit). */
		CONDITIONS("conditions"),
		/** Part of the structure is in a chunk that is not loaded, so it cannot be checked. */
		UNLOADED("unloaded"),
		/** The anchor stopped being run mid-ritual (its chunk unloaded, or the server stopped): found when it loads. */
		LAPSED("lapsed"),
		/** The leader called it off. */
		CANCELLED("cancelled"),
		/** The anchor was broken or removed. */
		REMOVED("removed"),
		/** The Concordance was turned off in the configuration. */
		DISABLED("disabled"),
		/** The rules no longer define the ritual (a data pack was removed and reloaded). */
		FORGOTTEN("forgotten"),
		/**
		 * The reserved offerings were found changed at completion, which the lock should make impossible: a guard
		 * against anything that got round it. Nothing is consumed and nothing is made.
		 */
		TAMPERED("tampered");

		public final String id;

		Interruption(String id) {
			this.id = id;
		}

		public Consequence consequence() {
			return new Consequence(this == REMOVED ? Release.DROPPED : Release.KEPT, this == CONTAINMENT);
		}

		public static @Nullable Interruption fromId(String id) {
			for (Interruption interruption : values()) {
				if (interruption.id.equals(id)) {
					return interruption;
				}
			}
			return null;
		}
	}

	/**
	 * What the anchor saw at a step: the structure report, how many participants have joined and how many of them
	 * are still present, and whether the ritual's conditions hold.
	 */
	public record Observation(StructureValidator.Report report, int joined, int present, boolean conditions) {
	}

	/**
	 * What stops a ritual now, or null if nothing does. When several things are wrong, the first of these is given:
	 * an unloaded part (nothing there can be known, so no backlash is risked on a guess), then containment, the rest
	 * of the structure, power, participants and conditions.
	 */
	public static @Nullable Interruption fault(Observation seen) {
		StructureValidator.Report report = seen.report();
		if (report.has(StructureValidator.Problem.UNLOADED)) {
			return Interruption.UNLOADED;
		}
		if (report.broken(StructurePattern.Role.BOUNDARY)) {
			return Interruption.CONTAINMENT;
		}
		if (report.broken(StructurePattern.Role.CHANNEL) || report.broken(StructurePattern.Role.CLEARANCE)) {
			return Interruption.STRUCTURE;
		}
		if (report.has(StructureValidator.Problem.UNPOWERED)) {
			return Interruption.POWER;
		}
		if (seen.present() < seen.joined()) {
			return Interruption.PARTICIPANTS;
		}
		if (!seen.conditions()) {
			return Interruption.CONDITIONS;
		}
		return null;
	}

	public sealed interface Decision {
	}

	/** Draw this step's Ley Charge and carry on. */
	public record Continue(int step) implements Decision {
	}

	/** Draw this step's Ley Charge, then consume the offerings and commit the result, in this tick. */
	public record Commit() implements Decision {
	}

	public record Interrupt(Interruption reason) implements Decision {
	}

	/**
	 * The decision at a step while channeling, {@code done} steps having completed. Every step checks the whole
	 * observation again; the last one commits.
	 */
	public static Decision step(RitualDefinition ritual, int done, Observation seen) {
		Interruption fault = fault(seen);
		if (fault != null) {
			return new Interrupt(fault);
		}
		return done + 1 >= ritual.steps() ? new Commit() : new Continue(done + 1);
	}

	/**
	 * While gathering: null to keep waiting, {@link Phase#CHANNELING} once everyone has joined, or an interruption if
	 * something broke or the others did not come in time.
	 */
	public static @Nullable Decision gather(RitualDefinition ritual, Observation seen, long waited) {
		Interruption fault = fault(seen);
		if (fault != null) {
			return new Interrupt(fault);
		}
		if (seen.joined() >= ritual.participants()) {
			return new Continue(0);
		}
		return waited >= GATHER_TICKS ? new Interrupt(Interruption.PARTICIPANTS) : null;
	}

	/** How far from the anchor's centre, horizontally, a participant may stand for a structure. */
	public static int participantRange(StructurePattern pattern) {
		return pattern.reach() + PARTICIPANT_MARGIN;
	}
}
