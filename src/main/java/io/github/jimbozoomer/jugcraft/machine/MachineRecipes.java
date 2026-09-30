package io.github.jimbozoomer.jugcraft.machine;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Crusher and arc furnace recipes, generated into jugcraft/machine_recipes.json from
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

	private static Map<MachineKind, Map<Item, Recipe>> recipes;

	private MachineRecipes() {
	}

	public static Optional<Recipe> find(MachineKind kind, ItemStack input) {
		if (recipes == null) {
			recipes = load();
		}
		return Optional.ofNullable(recipes.getOrDefault(kind, Map.of()).get(input.getItem()));
	}

	private static Map<MachineKind, Map<Item, Recipe>> load() {
		Map<MachineKind, Map<Item, Recipe>> result = new EnumMap<>(MachineKind.class);
		try (InputStream stream = MachineRecipes.class.getResourceAsStream("/jugcraft/machine_recipes.json")) {
			if (stream == null) {
				Jugcraft.LOGGER.error("Missing jugcraft/machine_recipes.json");
				return result;
			}
			JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
			result.put(MachineKind.CRUSHER, parse(root, "crusher"));
			result.put(MachineKind.ARC_FURNACE, parse(root, "arc_furnace"));
		} catch (Exception e) {
			Jugcraft.LOGGER.error("Could not load machine recipes", e);
		}
		return result;
	}

	private static Map<Item, Recipe> parse(JsonObject root, String machine) {
		Map<Item, Recipe> map = new HashMap<>();
		for (JsonElement element : root.getAsJsonArray(machine)) {
			JsonObject recipe = element.getAsJsonObject();
			boolean enabled = true;
			for (JsonElement feature : recipe.getAsJsonArray("features")) {
				enabled &= JugcraftConfig.isFeatureEnabled(feature.getAsString());
			}
			Item input = item(recipe.get("input").getAsString());
			Item output = item(recipe.get("output").getAsString());
			if (!enabled || input == Items.AIR || output == Items.AIR) {
				continue;
			}
			map.put(input, new Recipe(new ItemStack(output, recipe.get("count").getAsInt()), recipe.get("ticks").getAsInt()));
		}
		return map;
	}

	private static Item item(String id) {
		return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
	}
}
