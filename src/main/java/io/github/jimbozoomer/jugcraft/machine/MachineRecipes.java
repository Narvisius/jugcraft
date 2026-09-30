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
 * Recipes for Jugcraft's own processors, generated into jugcraft/machine_recipes.json from
 * tools/machines.py (where they are audited for metal conservation). Single-input machines
 * map one item to one result; multi-input machines (alloy smelter, circuit assembler) match
 * several ingredient stacks in any slot order. Recipes whose feature switches are off, or
 * whose items do not exist, are skipped. Loaded lazily after registries are frozen.
 */
public final class MachineRecipes {
	public record Recipe(ItemStack output, int ticks) {
		@Override
		public ItemStack output() {
			return output.copy();
		}
	}

	public record Ingredient(Item item, int count) {
	}

	public record MultiRecipe(List<Ingredient> ingredients, ItemStack output, int ticks) {
		@Override
		public ItemStack output() {
			return output.copy();
		}
	}

	/** A multi-input recipe matched against the input slots, with how many items to take from each slot. */
	public record MultiMatch(MultiRecipe recipe, int[] take) {
	}

	private static Map<MachineKind, Map<Item, Recipe>> single;
	private static Map<MachineKind, List<MultiRecipe>> multi;

	private MachineRecipes() {
	}

	public static Optional<Recipe> find(MachineKind kind, ItemStack input) {
		ensureLoaded();
		return Optional.ofNullable(single.getOrDefault(kind, Map.of()).get(input.getItem()));
	}

	/** Finds a multi-input recipe whose every ingredient sits (in enough quantity) in its own input slot; other slots must be empty. */
	public static Optional<MultiMatch> findMulti(MachineKind kind, List<ItemStack> inputs) {
		ensureLoaded();
		for (MultiRecipe recipe : multi.getOrDefault(kind, List.of())) {
			int[] take = new int[inputs.size()];
			if (assign(recipe.ingredients(), 0, inputs, new boolean[inputs.size()], take)) {
				boolean extras = false;
				for (int slot = 0; slot < inputs.size(); slot++) {
					extras |= take[slot] == 0 && !inputs.get(slot).isEmpty();
				}
				if (!extras) {
					return Optional.of(new MultiMatch(recipe, take));
				}
			}
		}
		return Optional.empty();
	}

	/** Whether an item appears in any of this machine's multi-input recipes (for slot and hopper filtering). */
	public static boolean isMultiIngredient(MachineKind kind, ItemStack stack) {
		ensureLoaded();
		for (MultiRecipe recipe : multi.getOrDefault(kind, List.of())) {
			for (Ingredient ingredient : recipe.ingredients()) {
				if (stack.is(ingredient.item())) {
					return true;
				}
			}
		}
		return false;
	}

	/** Backtracking assignment of ingredients to distinct slots (at most a few slots, so this stays tiny). */
	private static boolean assign(List<Ingredient> ingredients, int index, List<ItemStack> inputs, boolean[] used, int[] take) {
		if (index == ingredients.size()) {
			return true;
		}
		Ingredient ingredient = ingredients.get(index);
		for (int slot = 0; slot < inputs.size(); slot++) {
			ItemStack stack = inputs.get(slot);
			if (!used[slot] && stack.is(ingredient.item()) && stack.getCount() >= ingredient.count()) {
				used[slot] = true;
				take[slot] = ingredient.count();
				if (assign(ingredients, index + 1, inputs, used, take)) {
					return true;
				}
				used[slot] = false;
				take[slot] = 0;
			}
		}
		return false;
	}

	private static void ensureLoaded() {
		if (single != null) {
			return;
		}
		single = new EnumMap<>(MachineKind.class);
		multi = new EnumMap<>(MachineKind.class);
		try (InputStream stream = MachineRecipes.class.getResourceAsStream("/jugcraft/machine_recipes.json")) {
			if (stream == null) {
				Jugcraft.LOGGER.error("Missing jugcraft/machine_recipes.json");
				return;
			}
			JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
			for (MachineKind kind : MachineKind.values()) {
				String key = kind.recipeKey();
				if (key == null || !root.has(key)) {
					continue;
				}
				if (kind.isMultiInput()) {
					multi.put(kind, parseMulti(root.getAsJsonArray(key)));
				} else {
					single.put(kind, parseSingle(root.getAsJsonArray(key)));
				}
			}
		} catch (Exception e) {
			Jugcraft.LOGGER.error("Could not load machine recipes", e);
		}
	}

	private static Map<Item, Recipe> parseSingle(JsonArray recipes) {
		Map<Item, Recipe> map = new HashMap<>();
		for (JsonElement element : recipes) {
			JsonObject recipe = element.getAsJsonObject();
			Item input = item(recipe.get("input").getAsString());
			Item output = item(recipe.get("output").getAsString());
			if (!enabled(recipe) || input == Items.AIR || output == Items.AIR) {
				continue;
			}
			map.put(input, new Recipe(new ItemStack(output, recipe.get("count").getAsInt()), recipe.get("ticks").getAsInt()));
		}
		return map;
	}

	private static List<MultiRecipe> parseMulti(JsonArray recipes) {
		List<MultiRecipe> list = new ArrayList<>();
		for (JsonElement element : recipes) {
			JsonObject recipe = element.getAsJsonObject();
			if (!enabled(recipe)) {
				continue;
			}
			List<Ingredient> ingredients = new ArrayList<>();
			boolean valid = true;
			for (JsonElement input : recipe.getAsJsonArray("inputs")) {
				JsonArray pair = input.getAsJsonArray();
				Item item = item(pair.get(0).getAsString());
				valid &= item != Items.AIR;
				ingredients.add(new Ingredient(item, pair.get(1).getAsInt()));
			}
			Item output = item(recipe.get("output").getAsString());
			if (!valid || output == Items.AIR) {
				continue;
			}
			list.add(new MultiRecipe(List.copyOf(ingredients), new ItemStack(output, recipe.get("count").getAsInt()),
					recipe.get("ticks").getAsInt()));
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
