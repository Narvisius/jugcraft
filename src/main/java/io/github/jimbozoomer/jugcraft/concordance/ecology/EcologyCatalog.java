package io.github.jimbozoomer.jugcraft.concordance.ecology;

import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.jspecify.annotations.Nullable;

/**
 * The loaded organisms (by id, and found by block and by item), disturbance sources (roadmap step 14) and grafts
 * (Concordance Chunk C, C-09: by id, and found by host and donor's item).
 */
public final class EcologyCatalog {
	public static final EcologyCatalog EMPTY = new EcologyCatalog(Map.of(), Map.of(), Map.of());

	private final Map<String, Organism> organisms;
	private final Map<String, Disturbance> disturbances;
	private final Map<String, Graft> grafts;
	private final Map<String, Organism> byBlock = new HashMap<>();
	private final Map<String, Organism> byItem = new HashMap<>();
	/** Each graft by its host and its donor's item ("host|item"): what the gesture looks up. */
	private final Map<String, Graft> byDonor = new HashMap<>();
	/** Each graft's host niche with its ranges in place, worked out once. */
	private final Map<String, Niche> grafted = new HashMap<>();

	public EcologyCatalog(Map<String, Organism> organisms, Map<String, Disturbance> disturbances, Map<String, Graft> grafts) {
		this.organisms = Collections.unmodifiableMap(new TreeMap<>(organisms));
		this.disturbances = Collections.unmodifiableMap(new TreeMap<>(disturbances));
		this.grafts = Collections.unmodifiableMap(new TreeMap<>(grafts));
		for (Organism organism : this.organisms.values()) {
			byBlock.put(organism.block(), organism);
			if (organism.item() != null) {
				byItem.put(organism.item(), organism);
			}
		}
		for (Graft graft : this.grafts.values()) {
			Organism host = this.organisms.get(graft.host());
			Organism donor = this.organisms.get(graft.donor());
			if (host != null && donor != null && donor.item() != null) {
				byDonor.put(graft.host() + "|" + donor.item(), graft);
				grafted.put(graft.id(), graft.apply(host.niche()));
			}
		}
	}

	public Map<String, Organism> organisms() {
		return organisms;
	}

	public Map<String, Disturbance> disturbances() {
		return disturbances;
	}

	public Map<String, Graft> grafts() {
		return grafts;
	}

	public @Nullable Organism organism(String id) {
		return organisms.get(id);
	}

	/** The organism a block is (a crop at any age, or a producer), or null. */
	public @Nullable Organism byBlock(String block) {
		return byBlock.get(block);
	}

	/** The crop an item plants, or null. */
	public @Nullable Organism byItem(String item) {
		return byItem.get(item);
	}

	/**
	 * Whether {@code kinds} (the plants round one, {@link Sampler.Area#kinds}, by block) holds another crop that speeds
	 * {@code organism}: a mixed bed (Concordance Chunk C, C-08). A neighbour that is not a crop costing nutrients, or is the
	 * organism itself, never counts (the rules report it), and a fixer is never sped.
	 */
	public boolean mixed(Organism organism, Set<String> kinds) {
		if (organism.fix() > 0) {
			return false;
		}
		for (String id : organism.neighbours().organisms()) {
			Organism neighbour = organisms.get(id);
			if (neighbour != null && neighbour.crop() && neighbour.fix() == 0 && !neighbour.id().equals(organism.id())
					&& kinds.contains(neighbour.block())) {
				return true;
			}
		}
		return false;
	}

	// ---------------------------------------------------------------- grafts (Concordance Chunk C, C-09, C-P62 to C-P65)

	/** A graft by id, or null ("" names none). */
	public @Nullable Graft graft(String id) {
		return grafts.get(id);
	}

	/** The graft {@code host}'s crop takes from a donor's {@code item}, or null. */
	public @Nullable Graft graft(String host, String item) {
		return byDonor.get(host + "|" + item);
	}

	/**
	 * The niche {@code organism} grows by on a bed carrying {@code graft} ("" for none): the graft's when it is loaded and
	 * this organism is its host, otherwise the organism's own. The one answer every reader of a bed's niche uses.
	 */
	public Niche niche(Organism organism, String graft) {
		Graft held = grafts.get(graft);
		Niche niche = held != null && held.host().equals(organism.id()) ? grafted.get(held.id()) : null;
		return niche != null ? niche : organism.niche();
	}

	/** A graft's lines for a reader (Graft.lines), or none when the rules do not know it or its crops. */
	public List<Text> graftLines(String id) {
		Graft graft = grafts.get(id);
		Organism host = graft == null ? null : organisms.get(graft.host());
		Organism donor = graft == null ? null : organisms.get(graft.donor());
		return host == null || donor == null ? List.of() : graft.lines(host, donor);
	}
}
