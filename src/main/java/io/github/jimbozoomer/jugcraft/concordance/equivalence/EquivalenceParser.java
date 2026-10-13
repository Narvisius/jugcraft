package io.github.jimbozoomer.jugcraft.concordance.equivalence;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.jimbozoomer.jugcraft.concordance.resource.PrimaValue;
import io.github.jimbozoomer.jugcraft.concordance.rules.Json;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Reads materials and transmutations strictly ({@link Json}), roadmap step 21:
 * <pre>
 * material:      {"schema": 1, "item": "minecraft:iron_ingot", "value": {"grains": 256, "per": 1}, "dissolve": true, "form": true}
 * transmutation: {"schema": 1, "kind": "recipe", "via": "crafting", "inputs": {"minecraft:iron_ingot": 9},
 *                 "outputs": {"minecraft:iron_block": 1}, "returns": {}, "byproducts": {}, "catalysts": {},
 *                 "pattern": ["###", "###", "###"], "key": {"#": "minecraft:iron_ingot"}}
 * </pre>
 * Refused: a value of nothing or above {@value #MAX_GRAINS} grains a unit, a transmutation that gives nothing, a recipe
 * that takes nothing, counts out of bounds, and unknown fields. {@link #check} then refuses the catalogue as a whole if
 * the conversion graph is not closed over valued materials or if any recipe could gain value ({@link CycleAudit}).
 */
public final class EquivalenceParser {
	public static final int MAX_GRAINS = 100_000;
	public static final int MAX_PER = 64;
	public static final int MAX_COUNT = 64;

	private final List<String> problems = new ArrayList<>();

	public List<String> problems() {
		return Collections.unmodifiableList(problems);
	}

	public @Nullable Material material(String id, JsonElement json) {
		try {
			JsonObject object = Json.object(json, "material");
			Json.only(object, "schema", "item", "value", "dissolve", "form");
			Json.schema(object);
			JsonObject value = Json.object(Json.member(object, "value"), "value");
			Json.only(value, "grains", "per");
			PrimaValue prima = new PrimaValue(Json.range(value, "grains", 1, MAX_GRAINS), Json.range(value, "per", 1, MAX_PER));
			return new Material(Json.id(object, "item"), prima, Json.bool(object, "dissolve", true), Json.bool(object, "form", true));
		} catch (Json.Invalid | IllegalStateException | UnsupportedOperationException | IllegalArgumentException problem) {
			problems.add("material " + id + ": " + problem.getMessage());
			return null;
		}
	}

	public @Nullable Transmutation transmutation(String id, JsonElement json) {
		try {
			JsonObject object = Json.object(json, "transmutation");
			Json.only(object, "schema", "kind", "via", "inputs", "outputs", "returns", "byproducts", "catalysts", "pattern", "key");
			Json.schema(object);
			Transmutation.Kind kind = Json.choice(object, "kind", "recipe", "source").equals("source") ? Transmutation.Kind.SOURCE
					: Transmutation.Kind.RECIPE;
			Map<String, Integer> inputs = counts(object, "inputs");
			Map<String, Integer> outputs = counts(object, "outputs");
			if (outputs.isEmpty()) {
				throw new Json.Invalid("a transmutation must make something");
			}
			if (kind == Transmutation.Kind.RECIPE && inputs.isEmpty()) {
				throw new Json.Invalid("a recipe must take something (something from nothing is a source)");
			}
			List<String> pattern = new ArrayList<>();
			if (object.has("pattern")) {
				for (JsonElement row : Json.array(object, "pattern")) {
					if (!row.isJsonPrimitive() || row.getAsString().length() > 3) {
						throw new Json.Invalid("\"pattern\" rows are strings of up to three keys");
					}
					pattern.add(row.getAsString());
				}
			}
			Map<String, String> key = new LinkedHashMap<>();
			if (object.has("key")) {
				JsonObject keys = Json.object(Json.member(object, "key"), "key");
				for (String symbol : keys.keySet()) {
					if (symbol.length() != 1) {
						throw new Json.Invalid("\"key\" symbols are single characters");
					}
					key.put(symbol, Json.id(keys, symbol));
				}
			}
			return new Transmutation(id, kind, Json.name(object, "via"), inputs, outputs, counts(object, "returns"), counts(object, "byproducts"),
					counts(object, "catalysts"), List.copyOf(pattern), key);
		} catch (Json.Invalid | IllegalStateException | UnsupportedOperationException problem) {
			problems.add("transmutation " + id + ": " + problem.getMessage());
			return null;
		}
	}

	/** Materials keyed by their item (the catalogue's own key), from materials keyed by their file. */
	public static Map<String, Material> byItem(Map<String, Material> materials) {
		Map<String, Material> valued = new LinkedHashMap<>();
		materials.values().forEach(material -> valued.putIfAbsent(material.item(), material));
		return valued;
	}

	private static Map<String, Integer> counts(JsonObject object, String field) {
		Map<String, Integer> counts = new LinkedHashMap<>();
		if (!object.has(field)) {
			return counts;
		}
		JsonObject map = Json.object(Json.member(object, field), field);
		for (String item : map.keySet()) {
			Json.checkId(item, field);
			counts.put(item, Json.range(map, item, 1, MAX_COUNT));
		}
		return counts;
	}

	/**
	 * The catalogue as a whole: one material per item; every item a transmutation takes, makes, returns or yields as a
	 * byproduct has a value; and no recipe (nor the scale's own dissolving and forming) gains value. Returns problems.
	 */
	public static List<String> check(Map<String, Material> materials, Map<String, Transmutation> transmutations) {
		List<String> problems = new ArrayList<>();
		Map<String, String> byItem = new LinkedHashMap<>();
		for (Map.Entry<String, Material> entry : materials.entrySet()) {
			String previous = byItem.put(entry.getValue().item(), entry.getKey());
			if (previous != null) {
				problems.add("materials " + previous + " and " + entry.getKey() + " both value " + entry.getValue().item());
			}
		}
		EquivalenceCatalog catalog = new EquivalenceCatalog(byItem(materials), transmutations, List.of());
		for (CycleAudit.Finding finding : CycleAudit.audit(catalog)) {
			problems.add("equivalence: " + finding);
		}
		return problems;
	}
}
