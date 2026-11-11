package io.github.jimbozoomer.jugcraft.client.compat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The Advanced Workbench's page in JEI (crafting/AdvancedCraftingRecipe, jugcraft:advanced_crafting): its recipes laid
 * out on a 6 x 6 grid as they go on the bench, the result beside them, the workbench as the station. Recipes are server
 * data in 26.x, so they come from assets/jugcraft/recipe_view.json "advanced_crafting" (tools/advanced_crafting.py
 * view, written by tools/recipe_view.py), as the machines' pages do.
 */
public final class JugcraftJeiAdvanced {
	public static final int SIDE = 6;
	public static final IRecipeType<Recipe> TYPE = IRecipeType.create(Jugcraft.id("advanced_crafting"), Recipe.class);
	private static final String STATION = "jugcraft:advanced_crafting_table";

	private JugcraftJeiAdvanced() {
	}

	/** One recipe: its cells row by row (null an empty cell; a tag or a list shows all its items) and what it makes. */
	public record Recipe(int width, int height, List<List<ItemStack>> cells, ItemStack output) {
	}

	public static Item station() {
		return item(STATION);
	}

	public static List<Recipe> load() {
		List<Recipe> out = new ArrayList<>();
		var resource = Minecraft.getInstance().getResourceManager().getResource(Jugcraft.id("recipe_view.json"));
		if (resource.isEmpty()) {
			return out;
		}
		try (Reader reader = resource.get().openAsReader()) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			if (!root.has("advanced_crafting")) {
				return out;
			}
			for (JsonElement element : root.getAsJsonArray("advanced_crafting")) {
				JsonObject row = element.getAsJsonObject();
				try {
					var pattern = row.getAsJsonArray("pattern");
					JsonObject key = row.getAsJsonObject("key");
					int height = pattern.size(), width = pattern.get(0).getAsString().length();
					List<List<ItemStack>> cells = new ArrayList<>();
					for (JsonElement line : pattern) {
						String s = line.getAsString();
						for (int x = 0; x < width; x++) {
							char c = x < s.length() ? s.charAt(x) : ' ';
							cells.add(c == ' ' ? null : stacks(key.get(String.valueOf(c))));
						}
					}
					var result = row.getAsJsonArray("out");
					out.add(new Recipe(width, height, cells, new ItemStack(item(result.get(0).getAsString()), result.get(1).getAsInt())));
				} catch (RuntimeException error) {
					// One recipe naming an item that isn't there (a feature switched off) must not take the rest with it.
					Jugcraft.LOGGER.debug("Skipped the Advanced Workbench recipe {} for JEI", row.get("id"), error);
				}
			}
		} catch (Exception error) {
			Jugcraft.LOGGER.warn("Could not read the Advanced Workbench recipes for JEI", error);
		}
		return out;
	}

	private static Item item(String id) {
		Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
		if (item == null || item == Items.AIR) {
			throw new IllegalArgumentException("Unknown JEI item " + id);
		}
		return item;
	}

	private static List<ItemStack> stacks(JsonElement ref) {
		if (ref == null) {
			throw new IllegalArgumentException("A pattern symbol without a key");
		}
		if (ref.isJsonArray()) {
			List<ItemStack> options = new ArrayList<>();
			for (JsonElement option : ref.getAsJsonArray()) {
				options.addAll(stacks(option));
			}
			return options;
		}
		String id = ref.getAsString();
		if (!id.startsWith("#")) {
			return List.of(new ItemStack(item(id)));
		}
		TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.parse(id.substring(1)));
		List<ItemStack> options = new ArrayList<>();
		BuiltInRegistries.ITEM.get(tag).ifPresent(items -> items.forEach(i -> options.add(new ItemStack(i))));
		if (options.isEmpty()) {
			throw new IllegalArgumentException("Empty JEI ingredient " + id);
		}
		return options;
	}

	/** The page: the 6 x 6 grid (the pattern at its top left, as it may sit anywhere on the bench), the arrow, the result. */
	public static final class Category implements IRecipeCategory<Recipe> {
		private static final int GRID = SIDE * 18, ARROW_X = GRID + 8, OUT_X = GRID + 40;
		private final IDrawable icon;

		public Category(IGuiHelper gui) {
			icon = gui.createDrawableItemLike(station());
		}

		@Override
		public IRecipeType<Recipe> getRecipeType() {
			return TYPE;
		}

		@Override
		public Component getTitle() {
			return new ItemStack(station()).getHoverName();
		}

		@Override
		public IDrawable getIcon() {
			return icon;
		}

		@Override
		public int getWidth() {
			return OUT_X + 26;
		}

		@Override
		public int getHeight() {
			return GRID;
		}

		@Override
		public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
			for (int y = 0; y < SIDE; y++) {
				for (int x = 0; x < SIDE; x++) {
					var slot = builder.addInputSlot(x * 18 + 1, y * 18 + 1).setStandardSlotBackground();
					List<ItemStack> cell = x < recipe.width() && y < recipe.height() ? recipe.cells().get(y * recipe.width() + x) : null;
					if (cell != null) {
						slot.addItemStacks(cell);
					}
				}
			}
			builder.addOutputSlot(OUT_X + 5, GRID / 2 - 8).setOutputSlotBackground().add(recipe.output());
		}

		@Override
		public void createRecipeExtras(IRecipeExtrasBuilder builder, Recipe recipe, IFocusGroup focuses) {
			builder.addRecipeArrowWidget().setPosition(ARROW_X, GRID / 2 - 8);
		}
	}
}
