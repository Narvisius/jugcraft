package io.github.jimbozoomer.jugcraft.machine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Crusher, arc furnace and alloy smelter recipes, generated into jugcraft/machine_recipes.json from
 * tools/machines.py (where they are audited). Recipes whose feature switches are off,
 * or whose items do not exist, are skipped. Loaded lazily after registries are frozen.
 */
public final class MachineRecipes {
	public record Recipe(ItemStack output, int ticks) {
		@Override
		public ItemStack output() {
			return output.copy();
		}
	}

	/** A two-ingredient alloy recipe; ingredients may sit in either input slot. */
	public record AlloyRecipe(Item first, int firstCount, Item second, int secondCount, ItemStack output, int ticks) {
		@Override
		public ItemStack output() {
			return output.copy();
		}
	}

	/** An alloy recipe matched against the two input slots, with how much to take from each. */
	public record AlloyMatch(AlloyRecipe recipe, int takeFromSlot0, int takeFromSlot1) {
	}

	private static Map<MachineKind, Map<Item, Recipe>> recipes;
	private static List<AlloyRecipe> alloys;

	private MachineRecipes() {
	}

	public static Optional<AlloyMatch> findAlloy(ItemStack slot0, ItemStack slot1) {
		if (recipes == null) {
			recipes = load();
		}
		for (AlloyRecipe alloy : alloys) {
			if (covers(slot0, alloy.first(), alloy.firstCount()) && covers(slot1, alloy.second(), alloy.secondCount())) {
				return Optional.of(new AlloyMatch(alloy, alloy.firstCount(), alloy.secondCount()));
			}
			if (covers(slot0, alloy.second(), alloy.secondCount()) && covers(slot1, alloy.first(), alloy.firstCount())) {
				return Optional.of(new AlloyMatch(alloy, alloy.secondCount(), alloy.firstCount()));
			}
		}
		return Optional.empty();
	}

	/** Whether an item could be part of any alloy recipe (for slot and hopper filtering). */
	public static boolean isAlloyIngredient(ItemStack stack) {
		if (recipes == null) {
			recipes = load();
		}
		for (AlloyRecipe alloy : alloys) {
			if (stack.is(alloy.first()) || stack.is(alloy.second())) {
				return true;
			}
		}
		return false;
	}

	private static boolean covers(ItemStack stack, Item item, int count) {
		return stack.is(item) && stack.getCount() >= count;
	}

	public static Optional<Recipe> find(MachineKind kind, ItemStack input) {
		if (recipes == null) {
			recipes = load();
		}
		return Optional.ofNullable(recipes.getOrDefault(kind, Map.of()).get(input.getItem()));
	}

	private static Map<MachineKind, Map<Item, Recipe>> load() {
		Map<MachineKind, Map<Item, Recipe>> result = new EnumMap<>(MachineKind.class);
		alloys = new ArrayList<>();
		try (InputStream stream = MachineRecipes.class.getResourceAsStream("/jugcraft/machine_recipes.json")) {
			if (stream == null) {
				Jugcraft.LOGGER.error("Missing jugcraft/machine_recipes.json");
				return result;
			}
			JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
			result.put(MachineKind.CRUSHER, parse(root, "crusher"));
			result.put(MachineKind.ARC_FURNACE, parse(root, "arc_furnace"));
			alloys = parseAlloys(root);
		} catch (Exception e) {
			Jugcraft.LOGGER.error("Could not load machine recipes", e);
		}
		return result;
	}

	private static Map<Item, Recipe> parse(JsonObject root, String machine) {
		Map<Item, Recipe> map = new HashMap<>();
		for (JsonElement element : root.getAsJsonArray(machine)) {
			JsonObject recipe = element.getAsJsonObject();
			boolean enabled = enabled(recipe);
			Item input = item(recipe.get("input").getAsString());
			Item output = item(recipe.get("output").getAsString());
			if (!enabled || input == Items.AIR || output == Items.AIR) {
				continue;
			}
			map.put(input, new Recipe(new ItemStack(output, recipe.get("count").getAsInt()), recipe.get("ticks").getAsInt()));
		}
		return map;
	}

	private static List<AlloyRecipe> parseAlloys(JsonObject root) {
		List<AlloyRecipe> list = new ArrayList<>();
		for (JsonElement element : root.getAsJsonArray("alloy_smelter")) {
			JsonObject recipe = element.getAsJsonObject();
			if (!enabled(recipe)) {
				continue;
			}
			JsonArray inputs = recipe.getAsJsonArray("inputs");
			JsonArray first = inputs.get(0).getAsJsonArray();
			JsonArray second = inputs.get(1).getAsJsonArray();
			Item a = item(first.get(0).getAsString());
			Item b = item(second.get(0).getAsString());
			Item output = item(recipe.get("output").getAsString());
			if (a == Items.AIR || b == Items.AIR || output == Items.AIR) {
				continue;
			}
			list.add(new AlloyRecipe(a, first.get(1).getAsInt(), b, second.get(1).getAsInt(),
					new ItemStack(output, recipe.get("count").getAsInt()), recipe.get("ticks").getAsInt()));
		}
		return list;
	}

	private static boolean enabled(JsonObject recipe) {
		for (JsonElement feature : recipe.getAsJsonArray("features")) {
			if (!JugcraftConfig.isFeatureEnabled(feature.getAsString())) {
				return false;
			}
		}
		return true;
	}

	private static Item item(String id) {
		return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
	}
}
