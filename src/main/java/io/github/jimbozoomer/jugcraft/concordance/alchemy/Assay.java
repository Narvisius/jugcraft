package io.github.jimbozoomer.jugcraft.concordance.alchemy;

import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * What sampling a mixture tells a player, at three levels of information (roadmap step 13):
 * <ol>
 * <li>a spoon: the temperature band, the volume, the strongest property and whether it is murky;</li>
 * <li>an assay glass: the temperature, every property dissolved and still pending, a part's share, and the
 * contaminant;</li>
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
		out.add(Text.of("alchemy.assay.contaminant", Vector.units(mixture.contaminantConcentration())));
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
}
