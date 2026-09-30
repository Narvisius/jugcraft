package io.github.jimbozoomer.jugcraft.machine;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;

/**
 * A one-input machine recipe (crusher, arc furnace, metal press, wire drawer). Data-driven:
 * data/<namespace>/recipe/<type>/<name>.json, for example
 * {@code {"type": "jugcraft:crushing", "ingredient": "jugcraft:tin_ore", "result": {"id": "jugcraft:raw_tin", "count": 2}, "time": 160}}.
 * The ingredient may be an item, a list or a tag ({@code "#c:ores/tin"}); time is in ticks.
 */
public class MachineRecipe extends SingleItemRecipe {
	private final MachineKind machine;
	private final int time;

	public MachineRecipe(MachineKind machine, Recipe.CommonInfo commonInfo, Ingredient ingredient, ItemStackTemplate result, int time) {
		super(commonInfo, ingredient, result);
		this.machine = machine;
		this.time = time;
	}

	public MachineKind machine() {
		return machine;
	}

	/** Ticks one operation takes. */
	public int time() {
		return time;
	}

	/** What one operation makes. */
	public ItemStackTemplate output() {
		return result();
	}

	@Override
	public RecipeSerializer<MachineRecipe> getSerializer() {
		return MachineRecipeTypes.singleSerializer(machine);
	}

	@Override
	public RecipeType<MachineRecipe> getType() {
		return MachineRecipeTypes.single(machine);
	}

	@Override
	public String group() {
		return "";
	}

	@Override
	public RecipeBookCategory recipeBookCategory() {
		return RecipeBookCategories.CRAFTING_MISC;
	}

	static RecipeSerializer<MachineRecipe> serializer(MachineKind machine) {
		MapCodec<MachineRecipe> codec = RecordCodecBuilder.mapCodec(i -> i.group(
				Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
				Ingredient.CODEC.fieldOf("ingredient").forGetter(SingleItemRecipe::input),
				ItemStackTemplate.CODEC.fieldOf("result").forGetter(MachineRecipe::output),
				ExtraCodecs.POSITIVE_INT.optionalFieldOf("time", 200).forGetter(MachineRecipe::time)
		).apply(i, (info, ingredient, result, time) -> new MachineRecipe(machine, info, ingredient, result, time)));
		StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> stream = StreamCodec.composite(
				Recipe.CommonInfo.STREAM_CODEC, recipe -> recipe.commonInfo,
				Ingredient.CONTENTS_STREAM_CODEC, SingleItemRecipe::input,
				ItemStackTemplate.STREAM_CODEC, MachineRecipe::output,
				ByteBufCodecs.VAR_INT, MachineRecipe::time,
				(info, ingredient, result, time) -> new MachineRecipe(machine, info, ingredient, result, time));
		return new RecipeSerializer<>(codec, stream);
	}
}
