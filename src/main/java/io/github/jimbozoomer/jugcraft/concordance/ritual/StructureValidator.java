package io.github.jimbozoomer.jugcraft.concordance.ritual;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Checks a built structure against its {@link StructurePattern} and reports every fault, not just the first, so a
 * player (through Jade, the anchor or {@code /jugcraft concordance circle}) can see all that needs mending. Pure: the
 * world is read through a {@link Probe}, one call per part, so a check reads at most
 * {@link StructurePattern#MAX_PARTS} positions and never loads a chunk.
 */
public final class StructureValidator {
	private StructureValidator() {
	}

	public enum Problem {
		/** A channel or boundary position is empty. */
		MISSING("missing"),
		/** A channel or boundary position holds the wrong block. */
		INCOMPATIBLE("incompatible"),
		/** A clearance is not empty. */
		OBSTRUCTED("obstructed"),
		/** A channel holds less Ley Charge than a step draws. */
		UNPOWERED("unpowered"),
		/** A channel belongs to someone not taking part, who has not lent it. */
		FOREIGN("foreign"),
		/** The position is in a chunk that is not loaded, so nothing there can be known. */
		UNLOADED("unloaded");

		public final String id;

		Problem(String id) {
			this.id = id;
		}
	}

	public record Fault(StructurePattern.Part part, Problem problem) {
	}

	/**
	 * What the world holds at one part's position: whether it is loaded, empty (air), holds the block the part asks
	 * for, the Ley Charge it holds (channels) and whether the participants may draw on it (channels).
	 */
	public record Seen(boolean loaded, boolean empty, boolean matches, long ley, boolean permitted) {
		public static final Seen UNLOADED = new Seen(false, false, false, 0L, false);
	}

	@FunctionalInterface
	public interface Probe {
		Seen at(StructurePattern.Part part);
	}

	/**
	 * Every fault, in the pattern's order, and which channels are linked: bit {@code i} is set when channel {@code i}
	 * ({@link StructurePattern#channels()}) is in place, may be drawn on and holds a step's Ley Charge. Only linked
	 * channels are drawn as beams, so what players see is what the server validated.
	 */
	public record Report(String pattern, List<Fault> faults, int linked, int channels) {
		public Report {
			faults = List.copyOf(faults);
		}

		public boolean complete() {
			return faults.isEmpty();
		}

		public boolean has(Problem problem) {
			for (Fault fault : faults) {
				if (fault.problem() == problem) {
					return true;
				}
			}
			return false;
		}

		/** Whether any fault is at a part with this role (other than a missing charge). */
		public boolean broken(StructurePattern.Role role) {
			for (Fault fault : faults) {
				if (fault.part().role() == role && fault.problem() != Problem.UNPOWERED) {
					return true;
				}
			}
			return false;
		}
	}

	/** Checks the structure, requiring {@code leyPerChannel} in each channel (0 checks the shape alone). */
	public static Report check(StructurePattern pattern, Probe probe, long leyPerChannel) {
		List<Fault> faults = new ArrayList<>();
		int linked = 0;
		int channel = 0;
		for (StructurePattern.Part part : pattern.parts()) {
			Seen seen = probe.at(part);
			Problem problem = problem(part, seen, leyPerChannel);
			if (problem != null) {
				faults.add(new Fault(part, problem));
			}
			if (part.role() == StructurePattern.Role.CHANNEL) {
				if (problem == null) {
					linked |= 1 << channel;
				}
				channel++;
			}
		}
		return new Report(pattern.id(), faults, linked, channel);
	}

	private static @Nullable Problem problem(StructurePattern.Part part, Seen seen, long leyPerChannel) {
		if (!seen.loaded()) {
			return Problem.UNLOADED;
		}
		if (part.role() == StructurePattern.Role.CLEARANCE) {
			return seen.empty() ? null : Problem.OBSTRUCTED;
		}
		if (seen.empty()) {
			return Problem.MISSING;
		}
		if (!seen.matches()) {
			return Problem.INCOMPATIBLE;
		}
		if (part.role() == StructurePattern.Role.CHANNEL) {
			if (!seen.permitted()) {
				return Problem.FOREIGN;
			}
			if (seen.ley() < leyPerChannel) {
				return Problem.UNPOWERED;
			}
		}
		return null;
	}
}
