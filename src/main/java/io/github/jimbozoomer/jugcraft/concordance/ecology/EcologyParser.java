package io.github.jimbozoomer.jugcraft.concordance.ecology;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.jimbozoomer.jugcraft.concordance.rules.Json;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Reads organisms and disturbance sources strictly ({@link Json}): one bad file is refused with a message naming it
 * and its field, and the rest load.
 * <pre>
 * {"schema": 1, "role": "crop", "block": "jugcraft:sunpetal", "item": "jugcraft:sunpetal", "growth": 2.0,
 *  "cost": 1, "fix": 0, "replant": 1, "produce": 2, "chaff": 1,
 *  "niche": {"light": [10, 13, 15, 15], "moisture": [2, 4, 7, 7]}}
 * {"schema": 1, "role": "producer", "block": "jugcraft:verdant_heart", "cost": 1, "thriving": 2, "tolerating": 1,
 *  "niche": {...}}
 * {"schema": 1, "blocks": ["jugcraft:ley_pylon"], "value": 2}
 * </pre>
 * A niche range is {@code [least, ideal from, ideal to, most]}; a factor left out is any value.
 */
public final class EcologyParser {
	/** The most nutrients one step may cost or fix, and the most a harvest or a beat may yield. */
	public static final int MAX_COST = 8;
	public static final int MAX_YIELD = 8;

	private final List<String> problems = new ArrayList<>();

	public List<String> problems() {
		return Collections.unmodifiableList(problems);
	}

	public @Nullable Organism organism(String id, JsonElement json) {
		try {
			JsonObject object = Json.object(json, "organism");
			Json.schema(object);
			Organism.Role role = Json.choice(object, "role", "crop", "producer").equals("crop") ? Organism.Role.CROP : Organism.Role.PRODUCER;
			Niche niche = niche(Json.object(Json.member(object, "niche"), "niche"));
			if (role == Organism.Role.CROP) {
				Json.only(object, "schema", "role", "block", "item", "growth", "cost", "fix", "replant", "produce", "chaff", "niche");
				int cost = Json.range(object, "cost", 0, MAX_COST);
				int fix = Json.optional(object, "fix", 0, MAX_COST, 0);
				if (cost == 0 && fix == 0) {
					throw new Json.Invalid("a crop either costs nutrients or fixes them");
				}
				if (cost > 0 && fix > 0) {
					throw new Json.Invalid("a crop costs nutrients or fixes them, not both");
				}
				if (niche.range(Factor.NUTRIENTS).min() < cost) {
					throw new Json.Invalid("niche.nutrients must need at least the cost (" + cost + ")");
				}
				return new Organism(id, role, Json.id(object, "block"), Json.id(object, "item"), Json.decimal(object, "growth", 0.25, 8.0),
						cost, fix, Json.range(object, "replant", 0, Organism.STAGES - 1), Json.range(object, "produce", 1, MAX_YIELD),
						Json.range(object, "chaff", 0, MAX_YIELD), 0, 0, niche);
			}
			Json.only(object, "schema", "role", "block", "cost", "thriving", "tolerating", "niche");
			int cost = Json.range(object, "cost", 1, MAX_COST);
			int thriving = Json.range(object, "thriving", 1, MAX_YIELD);
			int tolerating = Json.range(object, "tolerating", 0, thriving);
			if (niche.range(Factor.NUTRIENTS).min() < cost) {
				throw new Json.Invalid("niche.nutrients must need at least the cost (" + cost + ")");
			}
			return new Organism(id, role, Json.id(object, "block"), null, 1.0, cost, 0, 0, 0, 0, thriving, tolerating, niche);
		} catch (Json.Invalid | IllegalStateException | UnsupportedOperationException | IllegalArgumentException problem) {
			problems.add("organism " + id + ": " + problem.getMessage());
			return null;
		}
	}

	public @Nullable Disturbance disturbance(String id, JsonElement json) {
		try {
			JsonObject object = Json.object(json, "disturbance");
			Json.only(object, "schema", "blocks", "value");
			Json.schema(object);
			List<String> blocks = Json.ids(object, "blocks", true);
			if (blocks.isEmpty()) {
				throw new Json.Invalid("\"blocks\" is empty");
			}
			return new Disturbance(id, blocks, Json.range(object, "value", 1, Sampler.MAX_DISTURBANCE));
		} catch (Json.Invalid | IllegalStateException | UnsupportedOperationException | IllegalArgumentException problem) {
			problems.add("disturbance " + id + ": " + problem.getMessage());
			return null;
		}
	}

	private static Niche niche(JsonObject object) {
		Map<Factor, Niche.Range> ranges = new EnumMap<>(Factor.class);
		for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
			Factor factor = Factor.fromId(entry.getKey());
			if (factor == null) {
				throw new Json.Invalid("niche: \"" + entry.getKey() + "\" is not a factor (moisture, light, nutrients, diversity, disturbance)");
			}
			if (!entry.getValue().isJsonArray() || entry.getValue().getAsJsonArray().size() != 4) {
				throw new Json.Invalid("niche." + factor.id + " must be [least, ideal from, ideal to, most]");
			}
			JsonArray values = entry.getValue().getAsJsonArray();
			int[] v = new int[4];
			for (int i = 0; i < 4; i++) {
				v[i] = Json.intValue(values.get(i), "niche." + factor.id, 0, factor.max);
			}
			try {
				ranges.put(factor, new Niche.Range(v[0], v[1], v[2], v[3]));
			} catch (IllegalArgumentException wrong) {
				throw new Json.Invalid("niche." + factor.id + ": " + wrong.getMessage());
			}
		}
		return new Niche(ranges);
	}
}
