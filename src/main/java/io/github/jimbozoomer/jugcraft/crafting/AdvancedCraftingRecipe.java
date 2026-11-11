package io.github.jimbozoomer.jugcraft.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * A shaped recipe for the Advanced Crafting Table's 6 x 6 grid (jugcraft:advanced_crafting), written like a vanilla
 * shaped recipe, for example
 * {@code {"type": "jugcraft:advanced_crafting", "pattern": ["R    R", " B  B ", "  FC  ", "  PP  ", " B  B ", "R    R"],
 * "key": {"R": "jugcraft:rotor_assembly", "B": "#c:plates/bronze", ...}, "result": {"id": "jugcraft:drone_t1"}}}.
 * The pattern need not fill the grid but is always bigger than 3 x 3 (wider or taller than three), so no recipe of the
 * vanilla crafting table's (or another mod's) can be the same; it may sit anywhere on the grid, and mirrored. A space
 * is an empty cell.
 */
public class AdvancedCraftingRecipe implements Recipe<AdvancedCraftingInput> {
	public static final int MAX = AdvancedCraftingInput.SIDE;

	private final Recipe.CommonInfo commonInfo;
	private final List<String> pattern;
	private final Map<String, Ingredient> key;
	private final ItemStackTemplate result;
	private final int width;
	private final int height;
	private final @Nullable Ingredient[] cells;

	public AdvancedCraftingRecipe(Recipe.CommonInfo commonInfo, List<String> pattern, Map<String, Ingredient> key, ItemStackTemplate result) {
		this.commonInfo = commonInfo;
		this.pattern = List.copyOf(pattern);
		this.key = Map.copyOf(key);
		this.result = result;
		this.height = pattern.size();
		this.width = pattern.isEmpty() ? 0 : pattern.get(0).length();
		this.cells = new Ingredient[width * height];
		for (int y = 0; y < height; y++) {
			String row = pattern.get(y);
			for (int x = 0; x < width && x < row.length(); x++) {
				char c = row.charAt(x);
				cells[y * width + x] = c == ' ' ? null : this.key.get(String.valueOf(c));
			}
		}
	}

	public List<String> pattern() {
		return pattern;
	}

	public Map<String, Ingredient> key() {
		return key;
	}

	public ItemStackTemplate result() {
		return result;
	}

	public int width() {
		return width;
	}

	public int height() {
		return height;
	}

	/** Whether the pattern is well formed: 1 to 6 rows of one length, 1 to 6, bigger than 3 x 3, every symbol keyed. */
	private DataResult<AdvancedCraftingRecipe> check() {
		if (height < 1 || height > MAX) {
			return DataResult.error(() -> "advanced_crafting: the pattern needs 1 to " + MAX + " rows, not " + height);
		}
		for (String row : pattern) {
			if (row.length() != width || width < 1 || width > MAX) {
				return DataResult.error(() -> "advanced_crafting: every row must be the same length, 1 to " + MAX);
			}
		}
		if (width <= 3 && height <= 3) {
			return DataResult.error(() -> "advanced_crafting: a pattern of " + width + " x " + height + " fits a crafting table; it must be "
					+ "wider or taller than 3");
		}
		Set<String> used = new HashSet<>();
		for (String row : pattern) {
			for (char c : row.toCharArray()) {
				if (c != ' ') {
					String s = String.valueOf(c);
					if (!key.containsKey(s)) {
						return DataResult.error(() -> "advanced_crafting: the symbol '" + s + "' has no key");
					}
					used.add(s);
				}
			}
		}
		for (String s : key.keySet()) {
			if (s.length() != 1 || s.equals(" ")) {
				return DataResult.error(() -> "advanced_crafting: a key must be one character other than a space, not '" + s + "'");
			}
			if (!used.contains(s)) {
				return DataResult.error(() -> "advanced_crafting: the key '" + s + "' is not in the pattern");
			}
		}
		return DataResult.success(this);
	}

	@Override
	public boolean matches(AdvancedCraftingInput input, Level level) {
		int minX = MAX, minY = MAX, maxX = -1, maxY = -1;
		for (int y = 0; y < MAX; y++) {
			for (int x = 0; x < MAX; x++) {
				if (!input.at(x, y).isEmpty()) {
					minX = Math.min(minX, x);
					minY = Math.min(minY, y);
					maxX = Math.max(maxX, x);
					maxY = Math.max(maxY, y);
				}
			}
		}
		if (maxX < 0 || maxX - minX + 1 != width || maxY - minY + 1 != height) {
			return false;
		}
		return fits(input, minX, minY, false) || fits(input, minX, minY, true);
	}

	/** Whether the grid holds the pattern with its top-left at (ox, oy), mirrored left to right or not. */
	private boolean fits(AdvancedCraftingInput input, int ox, int oy, boolean mirror) {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				Ingredient ingredient = cells[y * width + (mirror ? width - 1 - x : x)];
				ItemStack stack = input.at(ox + x, oy + y);
				if (ingredient == null ? !stack.isEmpty() : !ingredient.test(stack)) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public ItemStack assemble(AdvancedCraftingInput input) {
		return result.create();
	}

	@Override
	public boolean showNotification() {
		return commonInfo.showNotification();
	}

	@Override
	public String group() {
		return "";
	}

	@Override
	public RecipeSerializer<AdvancedCraftingRecipe> getSerializer() {
		return JugcraftCrafting.SERIALIZER;
	}

	@Override
	public RecipeType<AdvancedCraftingRecipe> getType() {
		return JugcraftCrafting.ADVANCED;
	}

	@Override
	public PlacementInfo placementInfo() {
		return PlacementInfo.NOT_PLACEABLE;
	}

	@Override
	public RecipeBookCategory recipeBookCategory() {
		return RecipeBookCategories.CRAFTING_MISC;
	}

	static RecipeSerializer<AdvancedCraftingRecipe> serializer() {
		MapCodec<AdvancedCraftingRecipe> codec = RecordCodecBuilder.<AdvancedCraftingRecipe>mapCodec(i -> i.group(
				Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
				Codec.STRING.listOf().fieldOf("pattern").forGetter(AdvancedCraftingRecipe::pattern),
				Codec.unboundedMap(Codec.STRING, Ingredient.CODEC).fieldOf("key").forGetter(AdvancedCraftingRecipe::key),
				ItemStackTemplate.CODEC.fieldOf("result").forGetter(AdvancedCraftingRecipe::result)
		).apply(i, AdvancedCraftingRecipe::new)).validate(AdvancedCraftingRecipe::check);
		StreamCodec<RegistryFriendlyByteBuf, AdvancedCraftingRecipe> stream = StreamCodec.composite(
				Recipe.CommonInfo.STREAM_CODEC, recipe -> recipe.commonInfo,
				ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), AdvancedCraftingRecipe::pattern,
				ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, Ingredient.CONTENTS_STREAM_CODEC), recipe -> new HashMap<>(recipe.key()),
				ItemStackTemplate.STREAM_CODEC, AdvancedCraftingRecipe::result,
				AdvancedCraftingRecipe::new);
		return new RecipeSerializer<>(codec, stream);
	}
}
