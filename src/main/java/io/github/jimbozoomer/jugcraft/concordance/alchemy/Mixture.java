package io.github.jimbozoomer.jugcraft.concordance.alchemy;

import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A mixture in a crucible (roadmap step 13), and the whole of the alchemical process as pure, deterministic steps:
 * <ul>
 * <li>{@code parts}: its volume, in parts of water (a bottle each; at most {@value #MAX_PARTS});</li>
 * <li>{@code pending}: properties that went in but have not dissolved yet;</li>
 * <li>{@code dissolved}: properties extracted into the water, which are what a dose carries;</li>
 * <li>{@code contaminant}: what fouls it (some ingredients bring it; searing stirs make it);</li>
 * <li>{@code drawn}: doses already bottled from it; {@code history}: every operation since it was empty.</li>
 * </ul>
 * Adding an ingredient applies its preparation (an explicit transformation: scaled, part dissolved at once, the rest
 * pending). Stirring dissolves a share of what is pending that depends on the temperature band ({@link Band}); a
 * searing stir also keeps only {@value #SEARING_KEEP} per thousand of the dissolved Radiance and Verdance and adds
 * {@value #SEARING_CONTAMINANT} milli-units of contaminant. Bottling draws one part's share of everything. All amounts
 * are whole milli-units with floor division, so identical operations always give identical mixtures and doses.
 */
public record Mixture(int parts, Vector pending, Vector dissolved, long contaminant, int drawn, List<Operation> history) {
	public static final int MAX_PARTS = 6;
	public static final int MAX_OPERATIONS = 32;
	public static final long SEARING_KEEP = 850;
	public static final long SEARING_CONTAMINANT = 500;
	public static final Mixture EMPTY = new Mixture(0, Vector.ZERO, Vector.ZERO, 0L, 0, List.of());

	public Mixture {
		history = List.copyOf(history);
		if (parts < 0 || parts > MAX_PARTS || contaminant < 0 || drawn < 0) {
			throw new IllegalArgumentException("parts 0.." + MAX_PARTS + ", no negative amounts");
		}
	}

	public boolean isEmpty() {
		return parts == 0;
	}

	/** The result of an operation: the new mixture, or why it cannot be done (nothing changes). */
	public sealed interface Step {
	}

	public record Applied(Mixture mixture) implements Step {
	}

	public record Refused(Text reason) implements Step {
	}

	public Step apply(Operation operation, AlchemyCatalog catalog) {
		if (history.size() >= MAX_OPERATIONS) {
			return new Refused(Text.of("alchemy.too_long", MAX_OPERATIONS));
		}
		List<Operation> next = new ArrayList<>(history);
		next.add(operation);
		return switch (operation) {
			case Operation.Water water -> parts + water.parts() > MAX_PARTS
					? new Refused(Text.of("alchemy.overflowing", MAX_PARTS))
					: new Applied(new Mixture(parts + water.parts(), pending, dissolved, contaminant, drawn, next));
			case Operation.Add add -> {
				Ingredient ingredient = catalog.ingredient(add.item());
				Preparation preparation = catalog.preparation(add.preparation());
				if (ingredient == null || preparation == null) {
					yield new Refused(Text.of("alchemy.not_ingredient", new Text.Ref("item", add.item())));
				}
				if (parts == 0) {
					yield new Refused(Text.of("alchemy.no_water"));
				}
				Vector prepared = ingredient.properties().scale(preparation.scale());
				Vector ready = prepared.scale(preparation.ready());
				yield new Applied(new Mixture(parts, pending.plus(prepared.minus(ready)), dissolved.plus(ready),
						contaminant + ingredient.contaminant() + preparation.contaminant(), drawn, next));
			}
			case Operation.Stir stir -> {
				if (parts == 0) {
					yield new Refused(Text.of("alchemy.no_water"));
				}
				Vector moved = pending.scale(stir.band().dissolve);
				Vector nowDissolved = dissolved.plus(moved);
				long fouled = contaminant;
				if (stir.band() == Band.SEARING) {
					nowDissolved = nowDissolved.scale(SEARING_KEEP, Axis.RADIANCE, Axis.VERDANCE);
					fouled += SEARING_CONTAMINANT;
				}
				yield new Applied(new Mixture(parts, pending.minus(moved), nowDissolved, fouled, drawn, next));
			}
		};
	}

	/** One dose bottled: what it carries, and the mixture left (empty, with no history, after the last part). */
	public record Bottled(Mixture rest, Vector dose, long contaminant) {
	}

	public @Nullable Bottled bottle() {
		if (parts == 0) {
			return null;
		}
		Vector dose = dissolved.divide(parts);
		long fouled = contaminant / parts;
		Mixture rest = parts == 1 ? EMPTY
				: new Mixture(parts - 1, pending.minus(pending.divide(parts)), dissolved.minus(dose), contaminant - fouled, drawn + 1, history);
		return new Bottled(rest, dose, fouled);
	}

	/** What one part holds dissolved now: what a dose would carry. */
	public Vector concentration() {
		return parts == 0 ? Vector.ZERO : dissolved.divide(parts);
	}

	public long contaminantConcentration() {
		return parts == 0 ? 0L : contaminant / parts;
	}

	/** What a dose bottled now would do, and why. */
	public Outcome outcome(AlchemyCatalog catalog) {
		return Outcome.of(concentration(), contaminantConcentration(), catalog, history);
	}
}
