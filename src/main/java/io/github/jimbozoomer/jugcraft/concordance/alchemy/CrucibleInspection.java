package io.github.jimbozoomer.jugcraft.concordance.alchemy;

import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

/**
 * What the encyclopedia's Inspect page says of an Alembic Crucible (Concordance Chunk C, C-01, C-P3, C-P26), at three
 * levels: to anyone, what is seen (heat, band, what heats it, Preserve's hold, volume, tank, formula step, why it waits)
 * and the risk line ({@link Assay#risks}); with an Assay Glass and the Alembic Arts understood, the glass's measures; with
 * them mastered, a dose's outcome now and the formula's replay under the rules in force. Pure: it reads a State and
 * changes nothing; InspectPage writes the sections.
 */
public final class CrucibleInspection {
	private CrucibleInspection() {
	}

	/**
	 * The crucible as the server has it now; reason is the crucible's own (Diagnostic.text()), or null; apart, whether its
	 * mixture is not its formula's own batch (made before the formula was set, or under a formula the rules refuse:
	 * Assay.risks).
	 */
	public record State(int temperature, Heat.Below below, int heldTicks, Mixture mixture, int tankParts, int tankCapacity,
			@Nullable Formula program, int step, @Nullable Text reason, boolean apart) {
	}

	/** A heading (none for the first section) and its lines. */
	public record Section(@Nullable Text heading, List<Text> lines) {
		public Section {
			lines = List.copyOf(lines);
		}
	}

	/**
	 * The sections a reader at {@code level} (Assay's levels) is told: what is seen, the sample, and more with the glass.
	 * {@code harmful} is the server's judgement of a status, for the risk line (Assay.risks).
	 */
	public static List<Section> read(State state, int level, Predicate<String> harmful, AlchemyCatalog catalog) {
		List<Section> out = new ArrayList<>();
		out.add(new Section(null, seen(state, harmful, catalog)));
		Text sample = level >= Assay.MASTERED ? Text.of("alchemy.inspect.sample.mastered")
				: level >= Assay.GLASS ? Text.of("alchemy.inspect.sample.glass") : Text.of("alchemy.inspect.sample.spoon");
		out.add(new Section(sample, Assay.read(state.mixture(), state.temperature(), level, catalog)));
		if (level >= Assay.GLASS && !state.mixture().isEmpty()) {
			out.add(new Section(Text.of("alchemy.inspect.measures"), measured(state.mixture(), catalog)));
		}
		Formula program = state.program();
		if (level >= Assay.MASTERED && program != null && program.replay(catalog) instanceof Mixture.Applied applied) {
			Outcome outcome = applied.mixture().outcome(catalog);
			List<Text> preview = new ArrayList<>();
			if (outcome.effects().isEmpty()) {
				preview.add(Text.of("alchemy.assay.nothing"));
			}
			preview.addAll(outcome.reasons());
			out.add(new Section(Text.of("alchemy.inspect.preview"), preview));
		}
		return out;
	}

	/** What anyone in reach sees: never the makeup, but always the risk (plan 03:54). */
	static List<Text> seen(State state, Predicate<String> harmful, AlchemyCatalog catalog) {
		List<Text> out = new ArrayList<>();
		Band band = Band.of(state.temperature());
		out.add(Text.of("alchemy.inspect.heat", state.temperature(), new Text.Ref("band", band.id)));
		Text.Ref block = new Text.Ref("block", state.below().block());
		if (state.below().heat() == null) {
			out.add(Text.of("alchemy.inspect.unheated", block, Heat.AMBIENT));
		} else if (state.below().unlit()) {
			out.add(Text.of("alchemy.inspect.unlit", block, Heat.AMBIENT));
		} else {
			out.add(Text.of("alchemy.inspect.heated", block, state.below().target()));
		}
		if (state.heldTicks() > 0) {
			out.add(Text.of("alchemy.inspect.held", new Text.Ref("band", band.id), (state.heldTicks() + 19) / 20));
		}
		out.add(Text.of("alchemy.inspect.volume", state.mixture().parts(), Mixture.MAX_PARTS));
		out.add(Text.of("alchemy.inspect.tank", state.tankParts(), state.tankCapacity()));
		Formula program = state.program();
		if (program == null) {
			if (state.reason() == null) {
				out.add(Text.of("alchemy.inspect.no_formula"));
			}
		} else if (state.step() >= program.operations().size()) {
			out.add(Text.of("alchemy.inspect.step.bottle", program.operations().size()));
		} else {
			out.add(Text.of("alchemy.inspect.step", state.step() + 1, program.operations().size(), program.operations().get(state.step()).describe()));
		}
		if (state.reason() != null) {
			out.add(state.reason());
		}
		out.addAll(Assay.risks(program, state.mixture(), state.apart(), harmful, catalog));
		return out;
	}

	private static List<Text> measured(Mixture mixture, AlchemyCatalog catalog) {
		List<Text> out = new ArrayList<>();
		Property fouling = catalog.property(Property.CONTAMINANT);
		if (fouling != null) {
			// Murky once twice the contaminant reaches Nausea's threshold (Assay.Look), so from half of it, rounded up.
			out.add(Text.of("alchemy.inspect.thresholds", Vector.units(mixture.contaminantConcentration()),
					Vector.units((fouling.threshold() + 1) / 2), Vector.units(fouling.threshold())));
		}
		out.add(Text.of("alchemy.inspect.operations", mixture.history().size(), Mixture.MAX_OPERATIONS));
		if (mixture.drawn() > 0) {
			out.add(Text.of("alchemy.inspect.drawn", mixture.drawn()));
		}
		return out;
	}
}
