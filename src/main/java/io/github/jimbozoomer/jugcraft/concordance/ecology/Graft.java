package io.github.jimbozoomer.jugcraft.concordance.ecology;

import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A graft (Concordance Chunk C, C-09, C-P62 to C-P65; data: {@code data/<ns>/concordance/graft/}): what a Verdant Bed carries
 * once a Greenwarden has worked a {@link #donor} crop's item into the {@link #host} crop growing in it. On that bed it puts
 * its ranges ({@link #overrides}) in place of the host's own, and does nothing else: never what the host costs, yields or is
 * in a crucible, and nothing for any other organism there. A bed carries at most one (plan 03:81's pilot).
 */
public record Graft(String id, String host, String donor, Map<Factor, Niche.Range> overrides) {
	public Graft {
		EnumMap<Factor, Niche.Range> copy = new EnumMap<>(Factor.class);
		copy.putAll(overrides);
		overrides = Collections.unmodifiableMap(copy);
	}

	/** {@code base} with this graft's ranges in place of its own. */
	public Niche apply(Niche base) {
		EnumMap<Factor, Niche.Range> ranges = new EnumMap<>(Factor.class);
		ranges.putAll(base.ranges());
		ranges.putAll(overrides);
		return new Niche(ranges);
	}

	/**
	 * Its lines for a reader: its name with its host and donor ({@code compose.jugcraft.ecology.graft}), then each range it
	 * changes against the host's own ({@code ecology.graft.range}: factor, least, most, ideal from, ideal to, then the host's).
	 */
	public List<Text> lines(Organism host, Organism donor) {
		List<Text> lines = new ArrayList<>();
		lines.add(Text.of("ecology.graft", new Text.Ref("graft", id), new Text.Ref("item", String.valueOf(host.item())),
				new Text.Ref("item", String.valueOf(donor.item()))));
		for (Map.Entry<Factor, Niche.Range> entry : overrides.entrySet()) {
			Niche.Range now = entry.getValue();
			Niche.Range was = host.niche().range(entry.getKey());
			lines.add(Text.of("ecology.graft.range", new Text.Ref("factor", entry.getKey().id), now.min(), now.max(), now.idealMin(),
					now.idealMax(), was.min(), was.max(), was.idealMin(), was.idealMax()));
		}
		return lines;
	}
}
