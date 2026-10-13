package io.github.jimbozoomer.jugcraft.concordance.artifice;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.jimbozoomer.jugcraft.concordance.rules.Json;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Reads artifice definitions strictly ({@link Json}), roadmap step 19:
 * <pre>
 * substrate: {"schema": 1, "item": "minecraft:gold_ingot", "cost": 4, "salvage": 2, "capacity": 7, "sockets": 2, "runes": 2,
 *             "durability": 120, "repair": 30, "affixes": ["jugcraft:arcane_power", ...]}
 * gem:       {"schema": 1, "item": "minecraft:amethyst_shard", "stat": {"attribute": "spell_power:arcane", "operation": "add_value",
 *             "amount": 1}, "cost": 2}
 * rune:      {"schema": 1, "item": "minecraft:glowstone_dust", "stat": {...}, "cost": 2, "substrates": ["jugcraft:gold"]}
 * affix:     {"schema": 1, "attribute": "minecraft:max_health", "operation": "add_value", "min": 1, "max": 4, "steps": 3,
 *             "cost": 2, "group": "vitality"}
 * </pre>
 * A salvage that would give back as much as forging costs, an affix whose range runs backwards, and unknown fields are
 * refused; unknown affix ids on a substrate are named by {@link #check}.
 */
public final class ArtificeParser {
	public static final int MAX_COST = 9;
	public static final int MAX_CAPACITY = 12;
	public static final int MAX_SOCKETS = 3;
	public static final int MAX_RUNES = 3;
	public static final int MAX_PART_COST = 6;
	public static final int MAX_STEPS = 100;
	public static final double MAX_AMOUNT = 100.0;

	private final List<String> problems = new ArrayList<>();

	public List<String> problems() {
		return Collections.unmodifiableList(problems);
	}

	public @Nullable Substrate substrate(String id, JsonElement json) {
		try {
			JsonObject object = Json.object(json, "substrate");
			Json.only(object, "schema", "item", "cost", "salvage", "capacity", "sockets", "runes", "durability", "repair", "affixes");
			Json.schema(object);
			int cost = Json.range(object, "cost", 1, MAX_COST);
			int durability = Json.range(object, "durability", 1, 4096);
			return new Substrate(id, Json.id(object, "item"), cost, Json.range(object, "salvage", 0, cost - 1),
					Json.range(object, "capacity", 1, MAX_CAPACITY), Json.range(object, "sockets", 0, MAX_SOCKETS),
					Json.range(object, "runes", 0, MAX_RUNES), durability, Json.range(object, "repair", 1, durability),
					Json.ids(object, "affixes", false));
		} catch (Json.Invalid | IllegalStateException | UnsupportedOperationException problem) {
			problems.add("substrate " + id + ": " + problem.getMessage());
			return null;
		}
	}

	public @Nullable Gem gem(String id, JsonElement json) {
		try {
			JsonObject object = Json.object(json, "gem");
			Json.only(object, "schema", "item", "stat", "cost");
			Json.schema(object);
			return new Gem(id, Json.id(object, "item"), stat(Json.object(Json.member(object, "stat"), "stat")),
					Json.range(object, "cost", 1, MAX_PART_COST));
		} catch (Json.Invalid | IllegalStateException | UnsupportedOperationException problem) {
			problems.add("gem " + id + ": " + problem.getMessage());
			return null;
		}
	}

	public @Nullable Rune rune(String id, JsonElement json) {
		try {
			JsonObject object = Json.object(json, "rune");
			Json.only(object, "schema", "item", "stat", "cost", "substrates");
			Json.schema(object);
			return new Rune(id, Json.id(object, "item"), stat(Json.object(Json.member(object, "stat"), "stat")),
					Json.range(object, "cost", 1, MAX_PART_COST), object.has("substrates") ? Json.ids(object, "substrates", false) : List.of());
		} catch (Json.Invalid | IllegalStateException | UnsupportedOperationException problem) {
			problems.add("rune " + id + ": " + problem.getMessage());
			return null;
		}
	}

	public @Nullable Affix affix(String id, JsonElement json) {
		try {
			JsonObject object = Json.object(json, "affix");
			Json.only(object, "schema", "attribute", "operation", "min", "max", "steps", "cost", "group");
			Json.schema(object);
			double min = Json.decimal(object, "min", -MAX_AMOUNT, MAX_AMOUNT);
			double max = Json.decimal(object, "max", min, MAX_AMOUNT);
			return new Affix(id, Json.id(object, "attribute"), operation(object), min, max, Json.range(object, "steps", 0, MAX_STEPS),
					Json.range(object, "cost", 1, MAX_PART_COST), Json.name(object, "group"));
		} catch (Json.Invalid | IllegalStateException | UnsupportedOperationException problem) {
			problems.add("affix " + id + ": " + problem.getMessage());
			return null;
		}
	}

	private static Stat stat(JsonObject object) {
		Json.only(object, "attribute", "operation", "amount");
		return new Stat(Json.id(object, "attribute"), operation(object), Json.decimal(object, "amount", -MAX_AMOUNT, MAX_AMOUNT));
	}

	private static Stat.Operation operation(JsonObject object) {
		return Stat.Operation.fromId(Json.choice(object, "operation", "add_value", "add_multiplied_base", "add_multiplied_total"));
	}

	/**
	 * Cross-checks a whole catalog: every affix a substrate names exists, every substrate a rune names exists, every
	 * substrate can roll at least one affix within its smallest capacity, and no two definitions share an item.
	 */
	public static List<String> check(Map<String, Substrate> substrates, Map<String, Gem> gems, Map<String, Rune> runes, Map<String, Affix> affixes) {
		List<String> problems = new ArrayList<>();
		for (Substrate substrate : substrates.values()) {
			for (String affix : substrate.affixes()) {
				if (!affixes.containsKey(affix)) {
					problems.add("substrate " + substrate.id() + ": names unknown affix " + affix);
				}
			}
			int least = substrate.capacity() + Quality.CRUDE.bonus;
			if (substrate.affixes().stream().map(affixes::get).noneMatch(affix -> affix != null && affix.cost() <= least)) {
				problems.add("substrate " + substrate.id() + ": a crude one could roll no affix (capacity " + least + ")");
			}
		}
		for (Rune rune : runes.values()) {
			for (String substrate : rune.substrates()) {
				if (!substrates.containsKey(substrate)) {
					problems.add("rune " + rune.id() + ": names unknown substrate " + substrate);
				}
			}
		}
		List<String> items = new ArrayList<>();
		substrates.values().forEach(substrate -> items.add(substrate.item()));
		gems.values().forEach(gem -> items.add(gem.item()));
		runes.values().forEach(rune -> items.add(rune.item()));
		for (String item : items) {
			if (items.indexOf(item) != items.lastIndexOf(item)) {
				problems.add("artifice: " + item + " is more than one substrate, gem or rune");
				break;
			}
		}
		return problems;
	}
}
