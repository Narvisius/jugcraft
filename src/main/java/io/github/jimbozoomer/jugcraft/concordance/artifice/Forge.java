package io.github.jimbozoomer.jugcraft.concordance.artifice;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * The artificer's rules (roadmap step 19, docs/features/arcane-concordance-artifice.md): capacity and what uses it,
 * compatibility, and every process with exactly what it keeps and what it destroys. Pure and deterministic: the only
 * randomness is a seed the server rolls when an artifice is forged and saves on it before anyone sees the result, so
 * the same artifice always reforges to the same next roll, whatever is previewed, cancelled or reopened.
 * <p>
 * Capacity is the substrate's, plus the quality's bonus, plus {@value #BOND_CAPACITY} while bonded. Affixes, runes and
 * gems each use their cost of it; nothing is ever added beyond it.
 */
public final class Forge {
	/** Capacity a bond adds. */
	public static final int BOND_CAPACITY = 1;

	/** The parts of an artifice a process can keep or destroy. */
	public enum Part {
		SUBSTRATE, QUALITY, AFFIXES, RUNES, GEMS, BOND, DURABILITY
	}

	/** Every process, with what it keeps and what it destroys (the codex and the anvil's preview read this). */
	public enum Process {
		FORGE("forge", EnumSet.noneOf(Part.class), EnumSet.noneOf(Part.class)),
		REFORGE("reforge", EnumSet.of(Part.SUBSTRATE, Part.QUALITY, Part.RUNES, Part.GEMS, Part.BOND, Part.DURABILITY), EnumSet.of(Part.AFFIXES)),
		INSCRIBE("inscribe", EnumSet.allOf(Part.class), EnumSet.noneOf(Part.class)),
		SOCKET("socket", EnumSet.allOf(Part.class), EnumSet.noneOf(Part.class)),
		UNSOCKET("unsocket", EnumSet.allOf(Part.class), EnumSet.noneOf(Part.class)),
		BOND("bond", EnumSet.allOf(Part.class), EnumSet.noneOf(Part.class)),
		UNBOND("unbond", EnumSet.of(Part.SUBSTRATE, Part.QUALITY, Part.AFFIXES, Part.RUNES, Part.GEMS, Part.DURABILITY), EnumSet.of(Part.BOND)),
		REPAIR("repair", EnumSet.allOf(Part.class), EnumSet.noneOf(Part.class)),
		SALVAGE("salvage", EnumSet.of(Part.GEMS), EnumSet.of(Part.QUALITY, Part.AFFIXES, Part.RUNES, Part.BOND, Part.DURABILITY));

		public final String id;
		public final Set<Part> keeps;
		public final Set<Part> destroys;

		Process(String id, Set<Part> keeps, Set<Part> destroys) {
			this.id = id;
			this.keeps = keeps;
			this.destroys = destroys;
		}
	}

	/** A process's outcome: the artifice after it, or why it was refused (nothing changes then). */
	public record Result(@Nullable Artifice artifice, String refusal) {
		static Result ok(Artifice artifice) {
			return new Result(artifice, "");
		}

		static Result no(String refusal) {
			return new Result(null, refusal);
		}

		public boolean done() {
			return artifice != null;
		}
	}

	/** A gem taken out: the artifice after it and the gem given back whole. */
	public record Removal(@Nullable Artifice artifice, @Nullable String gem, String refusal) {
	}

	/** What salvage gives back: some of the substrate and every socketed gem. */
	public record Salvage(String substrate, int count, List<String> gems) {
	}

	private Forge() {
	}

	public static int capacity(Artifice artifice, Substrate substrate) {
		return Math.max(0, substrate.capacity() + artifice.quality().bonus + (artifice.bond() != null ? BOND_CAPACITY : 0));
	}

	/** The capacity its affixes, runes and gems use. */
	public static int used(Artifice artifice, ArtificeCatalog catalog) {
		int used = fixed(artifice, catalog);
		for (RolledAffix rolled : artifice.affixes()) {
			Affix affix = catalog.affix(rolled.affix());
			used += affix == null ? 0 : affix.cost();
		}
		return used;
	}

	/** The capacity its runes and gems use (what a reforge keeps). */
	static int fixed(Artifice artifice, ArtificeCatalog catalog) {
		int used = 0;
		for (String id : artifice.runes()) {
			Rune rune = catalog.rune(id);
			used += rune == null ? 0 : rune.cost();
		}
		for (String id : artifice.gems()) {
			Gem gem = catalog.gem(id);
			used += gem == null ? 0 : gem.cost();
		}
		return used;
	}

	/** A seed and a reforge count give one random sequence (SplittableRandom's algorithm is fixed by its specification). */
	static long mix(long seed, int reforges) {
		return seed ^ (0x9E3779B97F4A7C15L * (reforges + 1L));
	}

	// ---------------------------------------------------------------- processes

	/** A new artifice of {@code substrate}: its quality and affixes rolled from {@code seed}. */
	public static Artifice forge(Substrate substrate, ArtificeCatalog catalog, long seed) {
		SplittableRandom random = new SplittableRandom(mix(seed, 0));
		Artifice blank = new Artifice(substrate.id(), Quality.roll(random), seed, 0, List.of(), List.of(), List.of(), null);
		return blank.withAffixes(roll(blank, substrate, catalog, random));
	}

	/**
	 * Affixes for the capacity its runes and gems leave: at most its quality's number, one a group, each drawn from the
	 * substrate's list among those that still fit.
	 */
	static List<RolledAffix> roll(Artifice artifice, Substrate substrate, ArtificeCatalog catalog, SplittableRandom random) {
		int room = capacity(artifice, substrate) - fixed(artifice, catalog);
		List<RolledAffix> rolled = new ArrayList<>();
		Set<String> groups = new HashSet<>();
		for (int i = 0; i < artifice.quality().affixes; i++) {
			List<Affix> fit = new ArrayList<>();
			for (String id : substrate.affixes()) {
				Affix affix = catalog.affix(id);
				if (affix != null && affix.cost() <= room && !groups.contains(affix.group())) {
					fit.add(affix);
				}
			}
			if (fit.isEmpty()) {
				break;
			}
			Affix pick = fit.get(random.nextInt(fit.size()));
			rolled.add(new RolledAffix(pick.id(), pick.value(random.nextInt(pick.steps() + 1))));
			room -= pick.cost();
			groups.add(pick.group());
		}
		return List.copyOf(rolled);
	}

	/** Reforging: new affixes, from its own seed and its next reforge count; it keeps everything else. */
	public static Result reforge(Artifice artifice, Substrate substrate, ArtificeCatalog catalog) {
		int next = artifice.reforges() + 1;
		Artifice kept = new Artifice(artifice.substrate(), artifice.quality(), artifice.seed(), next, List.of(), artifice.runes(),
				artifice.gems(), artifice.bond());
		return Result.ok(kept.withAffixes(roll(kept, substrate, catalog, new SplittableRandom(mix(artifice.seed(), next)))));
	}

	public static Result inscribe(Artifice artifice, Substrate substrate, Rune rune, ArtificeCatalog catalog) {
		if (!rune.substrates().isEmpty() && !rune.substrates().contains(substrate.id())) {
			return Result.no("incompatible");
		}
		if (artifice.runes().contains(rune.id())) {
			return Result.no("already_inscribed");
		}
		if (artifice.runes().size() >= substrate.runes()) {
			return Result.no("no_rune_slot");
		}
		if (used(artifice, catalog) + rune.cost() > capacity(artifice, substrate)) {
			return Result.no("over_capacity");
		}
		List<String> runes = new ArrayList<>(artifice.runes());
		runes.add(rune.id());
		return Result.ok(artifice.withRunes(runes));
	}

	public static Result socket(Artifice artifice, Substrate substrate, Gem gem, ArtificeCatalog catalog) {
		if (artifice.gems().size() >= substrate.sockets()) {
			return Result.no("no_socket");
		}
		if (used(artifice, catalog) + gem.cost() > capacity(artifice, substrate)) {
			return Result.no("over_capacity");
		}
		List<String> gems = new ArrayList<>(artifice.gems());
		gems.add(gem.id());
		return Result.ok(artifice.withGems(gems));
	}

	/** Takes out the last socketed gem, given back whole; nothing else changes. */
	public static Removal unsocket(Artifice artifice) {
		if (artifice.gems().isEmpty()) {
			return new Removal(null, null, "no_gem");
		}
		List<String> gems = new ArrayList<>(artifice.gems());
		String gem = gems.remove(gems.size() - 1);
		return new Removal(artifice.withGems(gems), gem, "");
	}

	public static Result bond(Artifice artifice, UUID player) {
		if (artifice.bond() != null) {
			return Result.no(artifice.bond().equals(player) ? "already_bonded" : "bonded_to_another");
		}
		return Result.ok(artifice.withBond(player));
	}

	/** Unbonding by its bonded player; refused while what it holds needs the bond's capacity (take a gem out first). */
	public static Result unbond(Artifice artifice, Substrate substrate, UUID player, ArtificeCatalog catalog) {
		if (artifice.bond() == null) {
			return Result.no("not_bonded");
		}
		if (!artifice.bond().equals(player)) {
			return Result.no("bonded_to_another");
		}
		Artifice free = artifice.withBond(null);
		if (used(free, catalog) > capacity(free, substrate)) {
			return Result.no("over_capacity");
		}
		return Result.ok(free);
	}

	/** How many items of its substrate mend {@code damage} (each mends {@link Substrate#repair}). */
	public static int repairItems(int damage, Substrate substrate) {
		return damage <= 0 ? 0 : (damage + substrate.repair() - 1) / substrate.repair();
	}

	/** Salvage gives back {@link Substrate#salvage} of its substrate and every socketed gem; the rest is destroyed. */
	public static Salvage salvage(Artifice artifice, Substrate substrate) {
		return new Salvage(substrate.item(), substrate.salvage(), artifice.gems());
	}

	// ---------------------------------------------------------------- what it gives

	/** Every stat it gives, in order (affixes, runes, gems); parts the data no longer defines give nothing. */
	public static List<Stat> stats(Artifice artifice, ArtificeCatalog catalog) {
		List<Stat> stats = new ArrayList<>();
		for (RolledAffix rolled : artifice.affixes()) {
			Affix affix = catalog.affix(rolled.affix());
			if (affix != null) {
				stats.add(new Stat(affix.attribute(), affix.operation(), rolled.value()));
			}
		}
		for (String id : artifice.runes()) {
			Rune rune = catalog.rune(id);
			if (rune != null) {
				stats.add(rune.stat());
			}
		}
		for (String id : artifice.gems()) {
			Gem gem = catalog.gem(id);
			if (gem != null) {
				stats.add(gem.stat());
			}
		}
		return stats;
	}

	/** Whether its stats apply for {@code wearer}: an unbonded artifice serves anyone, a bonded one only its bond. */
	public static boolean serves(Artifice artifice, UUID wearer) {
		return artifice.bond() == null || artifice.bond().equals(wearer);
	}
}
