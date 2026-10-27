package io.github.jimbozoomer.jugcraft.concordance.alchemy;

import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import io.github.jimbozoomer.jugcraft.concordance.effect.Intent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

/**
 * What sampling a mixture tells a player, at three levels of information (roadmap step 13):
 * <ol>
 * <li>a spoon: the temperature band, the volume, the strongest property and whether it is murky;</li>
 * <li>an assay glass: the temperature, every property dissolved and still pending, a part's share, and the
 * contaminant, with what it takes from a property it fouls (Concordance Chunk C, C-02);</li>
 * <li>an assay glass in the hands of someone who has mastered the Alembic Arts: what a dose bottled now would do, and
 * why ({@link Outcome#reasons()}).</li>
 * </ol>
 */
public final class Assay {
	public static final int SPOON = 1;
	public static final int GLASS = 2;
	public static final int MASTERED = 3;

	private Assay() {
	}

	/**
	 * What a spoon, or a look into the vessel, tells (roadmap step 27: the crucible's liquid shows this and no more): its
	 * strongest dissolved property, if any, and whether it is murky.
	 */
	public record Look(@Nullable Axis taste, boolean murky) {
		public static final Look PLAIN = new Look(null, false);

		public static Look of(Mixture mixture, AlchemyCatalog catalog) {
			if (mixture.isEmpty()) {
				return PLAIN;
			}
			Property fouling = catalog.property(Property.CONTAMINANT);
			return new Look(mixture.concentration().dominant(), fouling != null && mixture.contaminantConcentration() * 2 >= fouling.threshold());
		}
	}

	public static List<Text> read(Mixture mixture, int temperature, int level, AlchemyCatalog catalog) {
		List<Text> out = new ArrayList<>();
		Band band = Band.of(temperature);
		if (mixture.isEmpty()) {
			out.add(Text.of("alchemy.assay.empty", new Text.Ref("band", band.id)));
			return out;
		}
		out.add(Text.of("alchemy.assay.state", new Text.Ref("band", band.id), mixture.parts()));
		Vector concentration = mixture.concentration();
		Look look = Look.of(mixture, catalog);
		out.add(look.taste() == null ? Text.of("alchemy.assay.plain") : Text.of("alchemy.assay.taste", new Text.Ref("principle", look.taste().id)));
		if (look.murky()) {
			out.add(Text.of("alchemy.assay.murky"));
		}
		if (level < GLASS) {
			return out;
		}
		out.add(Text.of("alchemy.assay.temperature", temperature));
		Vector pending = mixture.pending().divide(mixture.parts());
		for (Axis axis : Axis.values()) {
			if (concentration.get(axis) > 0 || pending.get(axis) > 0) {
				out.add(Text.of("alchemy.assay.axis", new Text.Ref("principle", axis.id), Vector.units(concentration.get(axis)),
						Vector.units(pending.get(axis))));
			}
		}
		long contaminant = mixture.contaminantConcentration();
		out.add(Text.of("alchemy.assay.contaminant", Vector.units(contaminant)));
		// What the contaminant takes from a property that declares fouling (Concordance Chunk C, C-02, C-P7).
		for (Axis axis : Axis.values()) {
			Property property = catalog.property(axis.id);
			long taken = property == null ? 0L : property.fouled(concentration.get(axis), contaminant);
			if (taken > 0) {
				out.add(Text.of("alchemy.assay.fouls", new Text.Ref("principle", axis.id), Vector.units(taken)));
			}
		}
		if (level < MASTERED) {
			return out;
		}
		Outcome outcome = mixture.outcome(catalog);
		if (outcome.effects().isEmpty()) {
			out.add(Text.of("alchemy.assay.nothing"));
		}
		out.addAll(outcome.reasons());
		return out;
	}

	/**
	 * What is never hidden (plan 03:54; Concordance Chunk C, C-01, C-P3, C-P25): the harmful statuses a dose would carry
	 * and the first searing stir, for anyone, whatever they know. Of {@code program}'s replay under these rules when there
	 * is a formula (its searing step counted from 1), otherwise of {@code mixture} as it is; nothing for an empty crucible
	 * with no formula. A formula these rules refuse has no outcome: only its searing step is named. When the mixture in
	 * the crucible is not the formula's own batch ({@code apart}: it was there before the formula was set, or the rules
	 * refuse the formula), a dose bottled from it by hand now would carry its own harm, so that is named too (each status
	 * once) and, if it was stirred searing, that it was. A status harms when its property says so or {@code harmful} (the
	 * server's judgement of the status: Alchemy.harmfulStatus, C-P29) does.
	 */
	public static List<Text> risks(@Nullable Formula program, Mixture mixture, boolean apart, Predicate<String> harmful, AlchemyCatalog catalog) {
		List<Text> out = new ArrayList<>();
		List<Operation> operations;
		List<Outcome> outcomes = new ArrayList<>();
		boolean live = false;
		if (program != null) {
			operations = program.operations();
			if (program.replay(catalog) instanceof Mixture.Applied applied) {
				outcomes.add(applied.mixture().outcome(catalog));
			}
			live = apart && !mixture.isEmpty();
			if (live) {
				outcomes.add(mixture.outcome(catalog));
			}
		} else if (!mixture.isEmpty()) {
			operations = mixture.history();
			outcomes.add(mixture.outcome(catalog));
		} else {
			return out;
		}
		List<Text.Ref> harms = new ArrayList<>();
		for (Outcome outcome : outcomes) {
			for (Outcome.Effect effect : outcome.effects()) {
				Text.Ref status = new Text.Ref("status", effect.property().status());
				if ((effect.property().intent() == Intent.HARMFUL || harmful.test(status.id())) && !harms.contains(status)) {
					harms.add(status);
				}
			}
		}
		if (!harms.isEmpty()) {
			out.add(Text.of("alchemy.risk.harm", harms));
		}
		int searing = firstSearing(operations);
		if (searing >= 0) {
			out.add(program != null ? Text.of("alchemy.risk.searing", searing + 1) : Text.of("alchemy.risk.seared"));
		}
		if (live && firstSearing(mixture.history()) >= 0) {
			out.add(Text.of("alchemy.risk.seared"));
		}
		return out;
	}

	/** The index of the first searing stir in {@code operations}, or -1. */
	private static int firstSearing(List<Operation> operations) {
		for (int i = 0; i < operations.size(); i++) {
			if (operations.get(i) instanceof Operation.Stir stir && stir.band() == Band.SEARING) {
				return i;
			}
		}
		return -1;
	}
}
