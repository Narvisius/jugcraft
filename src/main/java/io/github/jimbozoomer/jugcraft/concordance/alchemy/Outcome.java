package io.github.jimbozoomer.jugcraft.concordance.alchemy;

import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * What a dose does, worked out from what it carries, with a reason for each effect a player can check against their
 * own process: which property, how much of it a part holds against the threshold, and which ingredients (or searing
 * stirs) put it there. Below a threshold the property is named as too weak, so nothing is mysterious.
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
		for (Axis axis : Axis.values()) {
			Property property = catalog.property(axis.id);
			long amount = dose.get(axis);
			if (property == null || amount == 0) {
				continue;
			}
			judge(property, amount, new Text.Ref("principle", axis.id), sources(axis, catalog, history), effects, reasons);
		}
		Property fouling = catalog.property(Property.CONTAMINANT);
		if (fouling != null && contaminant > 0) {
			judge(fouling, contaminant, new Text.Ref("alchemy", Property.CONTAMINANT), foulers(catalog, history), effects, reasons);
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

	/** The items whose ingredients carry {@code axis}, in the order they first went in. */
	private static List<Text.Ref> sources(Axis axis, AlchemyCatalog catalog, List<Operation> history) {
		Set<String> items = new LinkedHashSet<>();
		for (Operation operation : history) {
			if (operation instanceof Operation.Add add) {
				Ingredient ingredient = catalog.ingredient(add.item());
				if (ingredient != null && ingredient.properties().get(axis) > 0) {
					items.add(add.item());
				}
			}
		}
		return refs(items, false);
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
