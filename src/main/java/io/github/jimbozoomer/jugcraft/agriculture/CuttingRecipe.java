package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * A Cutting Board recipe (type {@code jugcraft:cutting}): what is set on the board, the tool that cuts it, and what it
 * is cut into, for example {@code {"type": "jugcraft:cutting", "ingredient": "minecraft:porkchop",
 * "tool": "#jugcraft:knives", "results": [{"id": "jugcraft:bacon", "count": 2}]}}. Data packs can add their own.
 */
public class CuttingRecipe implements Recipe<SingleRecipeInput> {
	public static final MapCodec<CuttingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
			Ingredient.CODEC.fieldOf("ingredient").forGetter(CuttingRecipe::ingredient),
			Ingredient.CODEC.fieldOf("tool").forGetter(CuttingRecipe::tool),
			ItemStackTemplate.CODEC.listOf().fieldOf("results").forGetter(CuttingRecipe::results)
	).apply(i, CuttingRecipe::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, CuttingRecipe> STREAM_CODEC = StreamCodec.composite(
			Recipe.CommonInfo.STREAM_CODEC, recipe -> recipe.commonInfo,
			Ingredient.CONTENTS_STREAM_CODEC, CuttingRecipe::ingredient,
			Ingredient.CONTENTS_STREAM_CODEC, CuttingRecipe::tool,
			ItemStackTemplate.STREAM_CODEC.apply(ByteBufCodecs.list()), CuttingRecipe::results,
			CuttingRecipe::new);

	private static @Nullable List<CuttingRecipe> cache;

	private final Recipe.CommonInfo commonInfo;
	private final Ingredient ingredient;
	private final Ingredient tool;
	private final List<ItemStackTemplate> results;

	public CuttingRecipe(Recipe.CommonInfo commonInfo, Ingredient ingredient, Ingredient tool, List<ItemStackTemplate> results) {
		this.commonInfo = commonInfo;
		this.ingredient = ingredient;
		this.tool = tool;
		this.results = List.copyOf(results);
	}

	public Ingredient ingredient() {
		return ingredient;
	}

	public Ingredient tool() {
		return tool;
	}

	public List<ItemStackTemplate> results() {
		return results;
	}

	/** Fresh stacks of everything this recipe cuts its ingredient into. */
	public List<ItemStack> cut() {
		List<ItemStack> out = new ArrayList<>();
		for (ItemStackTemplate result : results) {
			out.add(result.create());
		}
		return out;
	}

	@Override
	public boolean matches(SingleRecipeInput input, Level level) {
		return ingredient.test(input.item());
	}

	@Override
	public ItemStack assemble(SingleRecipeInput input) {
		return results.isEmpty() ? ItemStack.EMPTY : results.getFirst().create();
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
	public RecipeSerializer<CuttingRecipe> getSerializer() {
		return JugcraftAgriculture.CUTTING_SERIALIZER;
	}

	@Override
	public RecipeType<CuttingRecipe> getType() {
		return JugcraftAgriculture.CUTTING;
	}

	@Override
	public PlacementInfo placementInfo() {
		return PlacementInfo.NOT_PLACEABLE;
	}

	@Override
	public RecipeBookCategory recipeBookCategory() {
		return RecipeBookCategories.CRAFTING_MISC;
	}

	// ---------------------------------------------------------------- lookup

	/** Recipes come from data packs: the cached list is rebuilt after every reload. */
	static void registerReloadListener() {
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> clearCache());
		ServerLifecycleEvents.SERVER_STARTED.register(server -> clearCache());
	}

	private static synchronized void clearCache() {
		cache = null;
	}

	private static synchronized List<CuttingRecipe> recipes(MinecraftServer server) {
		if (cache == null) {
			List<CuttingRecipe> list = new ArrayList<>();
			for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
				if (holder.value() instanceof CuttingRecipe recipe) {
					list.add(recipe);
				}
			}
			cache = List.copyOf(list);
		}
		return cache;
	}

	/** The recipe that cuts {@code item} with {@code tool}, if any. */
	public static Optional<CuttingRecipe> find(MinecraftServer server, ItemStack item, ItemStack tool) {
		for (CuttingRecipe recipe : recipes(server)) {
			if (recipe.ingredient.test(item) && recipe.tool.test(tool)) {
				return Optional.of(recipe);
			}
		}
		return Optional.empty();
	}
}
