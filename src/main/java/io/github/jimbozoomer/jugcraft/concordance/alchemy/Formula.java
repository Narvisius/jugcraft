package io.github.jimbozoomer.jugcraft.concordance.alchemy;

import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Diagnostic;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A recorded process (roadmap step 13): the operations that made a mixture, from an empty crucible, as canonical data.
 * Written from a mixture nothing has been bottled from yet, and replayed operation by operation by a crucible's
 * automation, which makes the same mixture again (the process is deterministic). Validated when it is read: at most
 * {@link Mixture#MAX_OPERATIONS} operations, starting with water, each in canonical form, so a formula item can never
 * carry more than a player could do by hand.
 */
public record Formula(List<Operation> operations) {
	public Formula {
		operations = List.copyOf(operations);
		if (operations.isEmpty() || operations.size() > Mixture.MAX_OPERATIONS || !(operations.get(0) instanceof Operation.Water)) {
			throw new IllegalArgumentException("a formula is 1 to " + Mixture.MAX_OPERATIONS + " operations, starting with water");
		}
	}

	/** The formula of a mixture, or null if it cannot be recorded (empty, or a dose already drawn from it). */
	public static @Nullable Formula of(Mixture mixture) {
		if (mixture.isEmpty() || mixture.drawn() > 0 || mixture.history().isEmpty() || !(mixture.history().get(0) instanceof Operation.Water)) {
			return null;
		}
		return new Formula(mixture.history());
	}

	/** Replays every operation from an empty mixture: the mixture, or the first refusal with the index of its step. */
	public Mixture.Step replay(AlchemyCatalog catalog) {
		Mixture mixture = Mixture.EMPTY;
		for (int i = 0; i < operations.size(); i++) {
			Mixture.Step step = mixture.apply(operations.get(i), catalog);
			if (step instanceof Mixture.Refused refused) {
				return new Mixture.Refused(refused.reason(), i);
			}
			mixture = ((Mixture.Applied) step).mixture();
		}
		return new Mixture.Applied(mixture);
	}

	/** Why these rules cannot follow this formula (plan 03:54; Chunk C, C-01): its first refused step and why; null if none. */
	public @Nullable Diagnostic incompatibility(AlchemyCatalog catalog) {
		return replay(catalog) instanceof Mixture.Refused refused ? incompatible(refused) : null;
	}

	/** A refusal of one of this formula's steps, as the shared diagnostic: the step counted from 1, in the player's words. */
	public Diagnostic incompatible(Mixture.Refused refused) {
		int at = Math.clamp(refused.step(), 0, operations.size() - 1);
		return Diagnostic.incompatible(at + 1, operations.get(at).describe(), refused.reason());
	}

	/** The canonical text, operations joined by "; ". */
	public String text() {
		List<String> out = new ArrayList<>();
		for (Operation operation : operations) {
			out.add(operation.text());
		}
		return String.join("; ", out);
	}

	/** Reads a formula from its canonical text, or null if any part of it is not canonical. */
	public static @Nullable Formula parse(String text) {
		List<Operation> operations = new ArrayList<>();
		for (String part : text.split(";")) {
			Operation operation = Operation.parse(part);
			if (operation == null) {
				return null;
			}
			operations.add(operation);
		}
		try {
			Formula formula = new Formula(operations);
			return formula.text().equals(text) ? formula : null;
		} catch (IllegalArgumentException invalid) {
			return null;
		}
	}
}
