package io.github.jimbozoomer.jugcraft.concordance.alchemy;

import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * What a dose does, worked out from what it carries, with a reason for each effect a player can check against their
 * own process: which property, how much of it a part holds against the threshold, and which ingredients (or searing
 * stirs, or, for Rime, the warmer stirs that thawed it) put it there. Below a threshold the property is named as too
 * weak, so nothing is mysterious. A property that declares fouling loses its share of the dose to the contaminant
 * first, with a reason naming what fouled it (Concordance Chunk C, C-02, C-P7).
 */
public record Outcome(List<Effect> effects, List<Text> reasons) {
	public Outcome {
		effects = List.copyOf(effects);
		reasons = List.copyOf(reasons);
	}

	/** One effect: its property, amplifier (0 is level I), duration in ticks, and the concentration it came from. */
	public record Effect(Property property, int amplifier, int ticks, long concentration) {
	}

	public static Outcome of(Vector dose, long contaminant, AlchemyCatalog catalog, List<Operation> history) {
		List<Effect> effects = new ArrayList<>();
		List<Text> reasons = new ArrayList<>();
		List<Text.Ref> fouledBy = null;
		for (Axis axis : Axis.values()) {
			Property property = catalog.property(axis.id);
			long amount = dose.get(axis);
			if (property == null || amount == 0) {
				continue;
			}
			// The yield rule (Concordance Chunk C, C-02, C-P7): contaminant takes its share of a property that declares
			// fouling before the level and duration are worked out, and the reason names what fouled it.
			long taken = property.fouled(amount, contaminant);
			if (taken > 0) {
				if (fouledBy == null) {
					fouledBy = foulers(catalog, history);
				}
				reasons.add(Text.of("alchemy.fouled", new Text.Ref("principle", axis.id), Vector.units(taken), Vector.units(contaminant), fouledBy));
				amount -= taken;
			}
			judge(property, amount, new Text.Ref("principle", axis.id), sources(axis, catalog, history), effects, reasons);
		}
		Property fouling = catalog.property(Property.CONTAMINANT);
		if (fouling != null && contaminant > 0) {
			judge(fouling, contaminant, new Text.Ref("alchemy", Property.CONTAMINANT), fouledBy != null ? fouledBy : foulers(catalog, history),
					effects, reasons);
		}
		return new Outcome(effects, reasons);
	}

	private static void judge(Property property, long amount, Text.Ref what, List<Text.Ref> sources, List<Effect> effects,
			List<Text> reasons) {
		int level = property.level(amount);
		if (level == 0) {
			reasons.add(Text.of("alchemy.too_weak", what, Vector.units(amount), Vector.units(property.threshold()), sources));
			return;
		}
		int ticks = property.ticks(amount);
		effects.add(new Effect(property, level - 1, ticks, amount));
		reasons.add(Text.of("alchemy.reason", new Text.Ref("status", property.status()), level, Text.seconds(ticks), what,
				Vector.units(amount), Vector.units(property.threshold()), sources));
	}

	/**
	 * The items whose ingredients carry {@code axis}, in the order they first went in; for Rime, then the stirs above the
	 * cold band that thawed some of it (B-08): only a stir after something carrying Rime went in, since a stir thaws only
	 * Rime already in the mixture (Mixture.apply).
	 */
	private static List<Text.Ref> sources(Axis axis, AlchemyCatalog catalog, List<Operation> history) {
		Set<String> items = new LinkedHashSet<>();
		boolean rimeIn = false;
		boolean thawed = false;
		for (Operation operation : history) {
			if (operation instanceof Operation.Add add) {
				Ingredient ingredient = catalog.ingredient(add.item());
				if (ingredient != null && ingredient.properties().get(axis) > 0) {
					items.add(add.item());
				}
				if (ingredient != null && ingredient.properties().get(Axis.RIME) > 0) {
					rimeIn = true;
				}
			} else if (axis == Axis.RIME && rimeIn && operation instanceof Operation.Stir stir && Mixture.rimeKeep(stir.band()) < 1000L) {
				thawed = true;
			}
		}
		List<Text.Ref> out = refs(items, false);
		if (thawed) {
			out.add(new Text.Ref("alchemy", "thawed"));
		}
		return out;
	}

	/** What fouled it: ingredients that bring contaminant, and searing stirs. */
	private static List<Text.Ref> foulers(AlchemyCatalog catalog, List<Operation> history) {
		Set<String> items = new LinkedHashSet<>();
		boolean searing = false;
		for (Operation operation : history) {
			if (operation instanceof Operation.Add add) {
				Ingredient ingredient = catalog.ingredient(add.item());
				Preparation preparation = catalog.preparation(add.preparation());
				if (ingredient != null && ingredient.contaminant() > 0 || preparation != null && preparation.contaminant() > 0) {
					items.add(add.item());
				}
			} else if (operation instanceof Operation.Stir stir && stir.band() == Band.SEARING) {
				searing = true;
			}
		}
		return refs(items, searing);
	}

	private static List<Text.Ref> refs(Set<String> items, boolean searing) {
		List<Text.Ref> out = new ArrayList<>();
		for (String item : items) {
			out.add(new Text.Ref("item", item));
		}
		if (searing) {
			out.add(new Text.Ref("alchemy", "searing"));
		}
		return out;
	}

	/**
	 * The outcome as one canonical key: its effects' statuses and levels, sorted ("none" if it does nothing). Two doses
	 * with the same key are the same outcome, which is what distinct practice counts.
	 */
	public String key() {
		List<String> parts = new ArrayList<>();
		for (Effect effect : effects) {
			parts.add(effect.property().status() + "@" + effect.amplifier());
		}
		parts.sort(String::compareTo);
		return parts.isEmpty() ? "none" : String.join("+", parts);
	}
}
