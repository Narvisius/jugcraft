package io.github.jimbozoomer.jugcraft.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
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
 * A machine recipe with several ingredient stacks that may sit in any input slot, in any order
 * (alloy smelter, circuit assembler). Data-driven, for example
 * {@code {"type": "jugcraft:alloying", "ingredients": [{"ingredient": "minecraft:copper_ingot", "count": 3},
 * {"ingredient": "jugcraft:tin_ingot", "count": 1}], "result": {"id": "jugcraft:bronze_ingot", "count": 4}, "time": 200}}.
 * Every input slot not used by an ingredient must be empty.
 */
public class MultiMachineRecipe implements Recipe<MachineInput> {
	/** One ingredient stack: what it accepts and how many are used per operation. */
	public record Part(Ingredient ingredient, int count) {
		static final Codec<Part> CODEC = RecordCodecBuilder.create(i -> i.group(
				Ingredient.CODEC.fieldOf("ingredient").forGetter(Part::ingredient),
				ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(Part::count)
		).apply(i, Part::new));
		static final StreamCodec<RegistryFriendlyByteBuf, Part> STREAM_CODEC = StreamCodec.composite(
				Ingredient.CONTENTS_STREAM_CODEC, Part::ingredient, ByteBufCodecs.VAR_INT, Part::count, Part::new);
	}

	private final MachineKind machine;
	private final Recipe.CommonInfo commonInfo;
	private final List<Part> parts;
	private final ItemStackTemplate result;
	private final int time;
	private final int companionEffort;

	public MultiMachineRecipe(MachineKind machine, Recipe.CommonInfo commonInfo, List<Part> parts, ItemStackTemplate result, int time) {
		this(machine, commonInfo, parts, result, time, 0);
	}

	public MultiMachineRecipe(MachineKind machine, Recipe.CommonInfo commonInfo, List<Part> parts, ItemStackTemplate result, int time, int companionEffort) {
		this.machine = machine;
		this.commonInfo = commonInfo;
		this.parts = List.copyOf(parts);
		this.result = result;
		this.time = time;
		this.companionEffort = Math.clamp(companionEffort, 0, MachineCompanionEffort.MAX_PER_QUARTER);
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

	public int companionEffort() { return companionEffort; }

	/**
	 * How many items to take from each input slot for one operation, or null when the slots don't
	 * hold this recipe: each ingredient needs its own slot with enough items, and other slots must be empty.
	 */
	public int @Nullable [] take(MachineInput input) {
		int[] take = new int[input.size()];
		if (!assign(0, input, new boolean[input.size()], take)) {
			return null;
		}
		for (int slot = 0; slot < input.size(); slot++) {
			if (take[slot] == 0 && !input.getItem(slot).isEmpty()) {
				return null;
			}
		}
		return take;
	}

	/** Backtracking assignment of ingredients to distinct slots (at most a few slots, so this stays tiny). */
	private boolean assign(int index, MachineInput input, boolean[] used, int[] take) {
		if (index == parts.size()) {
			return true;
		}
		Part part = parts.get(index);
		for (int slot = 0; slot < input.size(); slot++) {
			ItemStack stack = input.getItem(slot);
			if (!used[slot] && part.ingredient().test(stack) && stack.getCount() >= part.count()) {
				used[slot] = true;
				take[slot] = part.count();
				if (assign(index + 1, input, used, take)) {
					return true;
				}
				used[slot] = false;
				take[slot] = 0;
			}
		}
		return false;
	}

	/** Whether the stack could be one of this recipe's ingredients (for slot and hopper filtering). */
	public boolean uses(ItemStack stack) {
		for (Part part : parts) {
			if (part.ingredient().test(stack)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean matches(MachineInput input, Level level) {
		return take(input) != null;
	}

	@Override
	public ItemStack assemble(MachineInput input) {
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
	public RecipeSerializer<MultiMachineRecipe> getSerializer() {
		return MachineRecipeTypes.multiSerializer(machine);
	}

	@Override
	public RecipeType<MultiMachineRecipe> getType() {
		return MachineRecipeTypes.multi(machine);
	}

	@Override
	public PlacementInfo placementInfo() {
		return PlacementInfo.NOT_PLACEABLE;
	}

	@Override
	public RecipeBookCategory recipeBookCategory() {
		return RecipeBookCategories.CRAFTING_MISC;
	}

	static RecipeSerializer<MultiMachineRecipe> serializer(MachineKind machine) {
		MapCodec<MultiMachineRecipe> codec = RecordCodecBuilder.mapCodec(i -> i.group(
				Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
				Part.CODEC.listOf().fieldOf("ingredients").forGetter(MultiMachineRecipe::parts),
				ItemStackTemplate.CODEC.fieldOf("result").forGetter(MultiMachineRecipe::output),
				ExtraCodecs.POSITIVE_INT.optionalFieldOf("time", 200).forGetter(MultiMachineRecipe::time),
				MachineCompanionEffort.CODEC.optionalFieldOf("companion_effort_per_quarter", 0).forGetter(MultiMachineRecipe::companionEffort)
		).apply(i, (info, parts, result, time, effort) -> new MultiMachineRecipe(machine, info, parts, result, time, effort)));
		StreamCodec<RegistryFriendlyByteBuf, MultiMachineRecipe> stream = StreamCodec.composite(
				Recipe.CommonInfo.STREAM_CODEC, recipe -> recipe.commonInfo,
				Part.STREAM_CODEC.apply(ByteBufCodecs.list()), MultiMachineRecipe::parts,
				ItemStackTemplate.STREAM_CODEC, MultiMachineRecipe::output,
				ByteBufCodecs.VAR_INT, MultiMachineRecipe::time,
				ByteBufCodecs.VAR_INT, MultiMachineRecipe::companionEffort,
				(info, parts, result, time, effort) -> new MultiMachineRecipe(machine, info, parts, result, time, effort));
		return new RecipeSerializer<>(codec, stream);
	}
}
