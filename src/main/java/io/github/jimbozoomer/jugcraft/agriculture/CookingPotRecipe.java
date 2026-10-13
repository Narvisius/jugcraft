package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
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
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * A Cooking Pot recipe (type {@code jugcraft:pot_cooking}), in the same format as Jugcraft's
 * multi-input machine recipes, for example
 * {@code {"type": "jugcraft:pot_cooking", "ingredients": [{"ingredient": "minecraft:bowl"},
 * {"ingredient": "jugcraft:tomato", "count": 2}, {"ingredient": "jugcraft:onion"}],
 * "result": {"id": "jugcraft:tomato_soup"}, "time": 200}}.
 * <p>
 * Ingredients may sit in any slot, and several of one ingredient may share a slot or be spread over
 * several, so players can stack ingredients and cook batch after batch. Every filled slot must hold
 * an ingredient of the recipe, so a stray item stops the pot rather than being cooked into something else.
 */
public class CookingPotRecipe implements Recipe<CookingPotRecipe.Input> {
	/** The pot's ingredient slots. */
	public record Input(List<ItemStack> stacks) implements RecipeInput {
		@Override
		public ItemStack getItem(int slot) {
			return stacks.get(slot);
		}

		@Override
		public int size() {
			return stacks.size();
		}
	}

	/** One ingredient and how many of it one batch uses. */
	public record Part(Ingredient ingredient, int count) {
		static final Codec<Part> CODEC = RecordCodecBuilder.create(i -> i.group(
				Ingredient.CODEC.fieldOf("ingredient").forGetter(Part::ingredient),
				ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(Part::count)
		).apply(i, Part::new));
		static final StreamCodec<RegistryFriendlyByteBuf, Part> STREAM_CODEC = StreamCodec.composite(
				Ingredient.CONTENTS_STREAM_CODEC, Part::ingredient, ByteBufCodecs.VAR_INT, Part::count, Part::new);
	}

	/** A recipe matched against the pot's slots, with how many items to take from each slot. */
	public record Match(CookingPotRecipe recipe, int[] take) {
	}

	public static final MapCodec<CookingPotRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
			Part.CODEC.listOf().fieldOf("ingredients").forGetter(CookingPotRecipe::parts),
			ItemStackTemplate.CODEC.fieldOf("result").forGetter(CookingPotRecipe::output),
			ExtraCodecs.POSITIVE_INT.optionalFieldOf("time", 200).forGetter(CookingPotRecipe::time)
	).apply(i, CookingPotRecipe::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, CookingPotRecipe> STREAM_CODEC = StreamCodec.composite(
			Recipe.CommonInfo.STREAM_CODEC, recipe -> recipe.commonInfo,
			Part.STREAM_CODEC.apply(ByteBufCodecs.list()), CookingPotRecipe::parts,
			ItemStackTemplate.STREAM_CODEC, CookingPotRecipe::output,
			ByteBufCodecs.VAR_INT, CookingPotRecipe::time,
			CookingPotRecipe::new);

	private static Object revision = new Object();
	private static final java.util.Map<MinecraftServer, java.util.Map<net.minecraft.resources.Identifier, CookingPotRecipe>> CATALOGS = new java.util.WeakHashMap<>();
	public static Object revision() { return revision; }
	public static java.util.Map<net.minecraft.resources.Identifier, CookingPotRecipe> catalog(MinecraftServer server) {
		return CATALOGS.computeIfAbsent(server, s -> {
			var entries = new java.util.TreeMap<net.minecraft.resources.Identifier, CookingPotRecipe>(java.util.Comparator.comparing(Object::toString));
			for (var holder : s.getRecipeManager().getRecipes()) if (holder.value() instanceof CookingPotRecipe pot)
				entries.put(holder.id().identifier(), pot);
			return java.util.Collections.unmodifiableMap(entries);
		});
	}
	public static List<CookingPotPlan> plans(MinecraftServer server) {
		return catalog(server).entrySet().stream().filter(e -> e.getValue().parts().size() <= 64).limit(CookingPotPlan.LIMIT)
			.map(e -> new CookingPotPlan(e.getKey(), e.getValue().output(), e.getValue().parts(), e.getValue().time())).toList();
	}
	public static Optional<Match> find(MinecraftServer server, List<ItemStack> slots, net.minecraft.resources.@Nullable Identifier selected) {
		if (selected == null) return find(server, slots);
		var recipe = catalog(server).get(selected);
		if (recipe == null) return Optional.empty();
		var take = recipe.take(new Input(slots));
		return take == null ? Optional.empty() : Optional.of(new Match(recipe, take));
	}

	private final Recipe.CommonInfo commonInfo;
	private final List<Part> parts;
	private final ItemStackTemplate result;
	private final int time;
	/** One entry per item a batch uses, parts in order: {bowl, tomato, tomato, onion}. */
	private final int[] units;

	public CookingPotRecipe(Recipe.CommonInfo commonInfo, List<Part> parts, ItemStackTemplate result, int time) {
		this.commonInfo = commonInfo;
		this.parts = List.copyOf(parts);
		this.result = result;
		this.time = time;
		int total = 0;
		for (Part part : this.parts) {
			total += part.count();
		}
		this.units = new int[total];
		int unit = 0;
		for (int index = 0; index < this.parts.size(); index++) {
			for (int n = 0; n < this.parts.get(index).count(); n++) {
				units[unit++] = index;
			}
		}
	}

	public List<Part> parts() {
		return parts;
	}

	public ItemStackTemplate output() {
		return result;
	}

	public int time() {
		return time;
	}

	/**
	 * How many items to take from each slot for one batch, or null when the slots don't hold this
	 * recipe: every filled slot must hold one of its ingredients, and every item a batch uses must be there.
	 */
	public int @Nullable [] take(Input input) {
		for (int slot = 0; slot < input.size(); slot++) {
			ItemStack stack = input.getItem(slot);
			if (!stack.isEmpty() && !uses(stack)) {
				return null;
			}
		}
		int[] take = new int[input.size()];
		return assign(0, input, take, new int[units.length]) ? take : null;
	}

	/**
	 * Backtracking: gives item {@code index} of the batch a slot with an item left in it. Items of the
	 * same part only try slots from the previous one's onward, so equal assignments are not retried.
	 */
	private boolean assign(int index, Input input, int[] take, int[] chosen) {
		if (index == units.length) {
			return true;
		}
		Ingredient ingredient = parts.get(units[index]).ingredient();
		int first = index > 0 && units[index - 1] == units[index] ? chosen[index - 1] : 0;
		for (int slot = first; slot < input.size(); slot++) {
			ItemStack stack = input.getItem(slot);
			if (take[slot] < stack.getCount() && ingredient.test(stack)) {
				take[slot]++;
				chosen[index] = slot;
				if (assign(index + 1, input, take, chosen)) {
					return true;
				}
				take[slot]--;
			}
		}
		return false;
	}

	/** Whether the stack could be one of this recipe's ingredients (slot and hopper filtering). */
	public boolean uses(ItemStack stack) {
		for (Part part : parts) {
			if (part.ingredient().test(stack)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean matches(Input input, Level level) {
		return take(input) != null;
	}

	@Override
	public ItemStack assemble(Input input) {
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
	public RecipeSerializer<CookingPotRecipe> getSerializer() {
		return JugcraftAgriculture.POT_SERIALIZER;
	}

	@Override
	public RecipeType<CookingPotRecipe> getType() {
		return JugcraftAgriculture.POT_COOKING;
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
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> clearCache());
	}

	private static synchronized void clearCache() {
		CATALOGS.clear(); revision = new Object();
	}

	private static java.util.Collection<CookingPotRecipe> recipes(MinecraftServer server) { return catalog(server).values(); }

	/** The recipe the pot's ingredient slots hold, if any. */
	public static Optional<Match> find(MinecraftServer server, List<ItemStack> slots) {
		Input input = new Input(slots);
		for (CookingPotRecipe recipe : recipes(server)) {
			int[] take = recipe.take(input);
			if (take != null) {
				return Optional.of(new Match(recipe, take));
			}
		}
		return Optional.empty();
	}

	/** Whether an item is an ingredient of any Cooking Pot recipe. */
	public static boolean isIngredient(MinecraftServer server, ItemStack stack) {
		for (CookingPotRecipe recipe : recipes(server)) {
			if (recipe.uses(stack)) {
				return true;
			}
		}
		return false;
	}
}
