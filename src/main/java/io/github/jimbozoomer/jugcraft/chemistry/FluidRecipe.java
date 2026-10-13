package io.github.jimbozoomer.jugcraft.chemistry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.machine.MachineInput;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.machine.MachineCompanionEffort;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.level.material.Fluid;

/**
 * A fluid processing recipe: items and fluids in, fluids and items out. Data-driven, for example
 * {@code {"type": "jugcraft:water_treatment", "items": [{"ingredient": "jugcraft:oil_sand"}],
 * "fluids": [{"fluid": "minecraft:water", "amount": 500}], "fluid_results": [{"fluid": "jugcraft:crude_oil", "amount": 400}],
 * "results": [{"id": "minecraft:sand"}], "time": 200}}.
 *
 * <p>Positions matter: the n-th item ingredient goes in the n-th item input slot, the n-th fluid in the n-th input
 * tank; results go to the output slots and tanks in order. Amounts are millibuckets.
 *
 * <p>Industrial machine forms (docs/features/industrial-machine-foundation.md) add two optional fields: a
 * {@code "capability"} the form running it must declare, and a reusable {@code "tool"} (an ingredient) one of the
 * form's tool sockets must hold, which the work never uses up. A recipe with either runs only in machine forms; the
 * original one-model machines keep exactly the recipes they always had.
 */
public class FluidRecipe implements Recipe<MachineInput> {
	/** An item ingredient and how many are used per operation. */
	public record ItemPart(Ingredient ingredient, int count) {
		static final Codec<ItemPart> CODEC = RecordCodecBuilder.create(i -> i.group(
				Ingredient.CODEC.fieldOf("ingredient").forGetter(ItemPart::ingredient),
				ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(ItemPart::count)
		).apply(i, ItemPart::new));
		static final StreamCodec<RegistryFriendlyByteBuf, ItemPart> STREAM_CODEC = StreamCodec.composite(
				Ingredient.CONTENTS_STREAM_CODEC, ItemPart::ingredient, ByteBufCodecs.VAR_INT, ItemPart::count, ItemPart::new);
	}

	/**
	 * A fluid and an amount in millibuckets. A fluid result may name the output {@code tank} it goes to (batch 24: a
	 * machine with several jobs keeps each product in its own tank); -1, the default, means the result's position.
	 */
	public record FluidAmount(Fluid fluid, int amount, int tank) {
		static final Codec<FluidAmount> CODEC = RecordCodecBuilder.create(i -> i.group(
				BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(FluidAmount::fluid),
				ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(FluidAmount::amount),
				Codec.INT.optionalFieldOf("tank", -1).forGetter(FluidAmount::tank)
		).apply(i, FluidAmount::new));
		static final StreamCodec<RegistryFriendlyByteBuf, FluidAmount> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.registry(Registries.FLUID), FluidAmount::fluid, ByteBufCodecs.VAR_INT, FluidAmount::amount,
				ByteBufCodecs.VAR_INT, FluidAmount::tank, FluidAmount::new);

		public FluidAmount(Fluid fluid, int amount) {
			this(fluid, amount, -1);
		}
	}

	private final MachineKind machine;
	private final Recipe.CommonInfo commonInfo;
	private final List<ItemPart> items;
	private final List<FluidAmount> fluids;
	private final List<FluidAmount> fluidResults;
	private final List<ItemStackTemplate> results;
	private final int time;
	private final int companionEffort;
	private final String capability;
	private final Optional<Ingredient> tool;

	public FluidRecipe(MachineKind machine, Recipe.CommonInfo commonInfo, List<ItemPart> items, List<FluidAmount> fluids,
			List<FluidAmount> fluidResults, List<ItemStackTemplate> results, int time) {
		this(machine, commonInfo, items, fluids, fluidResults, results, time, 0);
	}

	public FluidRecipe(MachineKind machine, Recipe.CommonInfo commonInfo, List<ItemPart> items, List<FluidAmount> fluids,
			List<FluidAmount> fluidResults, List<ItemStackTemplate> results, int time, int companionEffort) {
		this(machine, commonInfo, items, fluids, fluidResults, results, time, companionEffort, "", Optional.empty());
	}

	public FluidRecipe(MachineKind machine, Recipe.CommonInfo commonInfo, List<ItemPart> items, List<FluidAmount> fluids,
			List<FluidAmount> fluidResults, List<ItemStackTemplate> results, int time, int companionEffort, String capability,
			Optional<Ingredient> tool) {
		this.machine = machine;
		this.commonInfo = commonInfo;
		this.items = List.copyOf(items);
		this.fluids = List.copyOf(fluids);
		this.fluidResults = List.copyOf(fluidResults);
		this.results = List.copyOf(results);
		this.time = time;
		this.companionEffort = Math.clamp(companionEffort, 0, MachineCompanionEffort.MAX_PER_QUARTER);
		this.capability = capability;
		this.tool = tool;
	}

	public MachineKind machine() {
		return machine;
	}

	public List<ItemPart> items() {
		return items;
	}

	public List<FluidAmount> fluids() {
		return fluids;
	}

	public List<FluidAmount> fluidResults() {
		return fluidResults;
	}

	public List<ItemStackTemplate> results() {
		return results;
	}

	public int time() {
		return time;
	}

	public int companionEffort() { return companionEffort; }

	/** The process capability a machine form must declare to run this recipe; empty for the original machines. */
	public String capability() {
		return capability;
	}

	/** A reusable tool a form's socket must hold while this recipe runs (never used up), if any. */
	public Optional<Ingredient> tool() {
		return tool;
	}

	/** Whether the original one-model machine of its kind runs it: no capability and no tool. */
	public boolean legacy() {
		return capability.isEmpty() && tool.isEmpty();
	}

	/** Whether the item inputs hold this recipe's ingredients, each in its own slot, in order. */
	public boolean itemsMatch(List<ItemStack> inputs) {
		for (int slot = 0; slot < inputs.size(); slot++) {
			ItemStack stack = inputs.get(slot);
			if (slot < items.size()) {
				ItemPart part = items.get(slot);
				if (!part.ingredient().test(stack) || stack.getCount() < part.count()) {
					return false;
				}
			} else if (!stack.isEmpty()) {
				return false;
			}
		}
		return items.size() <= inputs.size();
	}

	/** Whether the input tanks hold this recipe's fluids, in order. */
	public boolean fluidsMatch(FluidTanks tanks) {
		if (fluids.size() > tanks.spec().inputTanks().size()) {
			return false;
		}
		for (int i = 0; i < fluids.size(); i++) {
			if (!tanks.input(i).has(fluids.get(i).fluid(), fluids.get(i).amount())) {
				return false;
			}
		}
		return true;
	}

	/** The output tank fluid result {@code index} goes to: its own {@link FluidAmount#tank}, or else its position. */
	public int resultTank(int index) {
		int tank = fluidResults.get(index).tank();
		return tank >= 0 ? tank : index;
	}

	/** Whether every fluid result has room in its output tank. */
	public boolean fluidResultsFit(FluidTanks tanks) {
		for (int i = 0; i < fluidResults.size(); i++) {
			if (resultTank(i) >= tanks.spec().outputTanks().size()
					|| !tanks.output(resultTank(i)).fits(fluidResults.get(i).fluid(), fluidResults.get(i).amount())) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean matches(MachineInput input, Level level) {
		return itemsMatch(input.stacks());
	}

	@Override
	public ItemStack assemble(MachineInput input) {
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
	public RecipeSerializer<FluidRecipe> getSerializer() {
		return FluidRecipes.serializer(machine);
	}

	@Override
	public RecipeType<FluidRecipe> getType() {
		return FluidRecipes.type(machine);
	}

	@Override
	public PlacementInfo placementInfo() {
		return PlacementInfo.NOT_PLACEABLE;
	}

	@Override
	public RecipeBookCategory recipeBookCategory() {
		return RecipeBookCategories.CRAFTING_MISC;
	}

	/** The fields added after the first fluid recipes, sent to clients together. */
	private record Extras(int companionEffort, String capability, Optional<Ingredient> tool) {
		static final StreamCodec<RegistryFriendlyByteBuf, Extras> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Extras::companionEffort,
				ByteBufCodecs.STRING_UTF8, Extras::capability,
				ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC), Extras::tool,
				Extras::new);
	}

	static RecipeSerializer<FluidRecipe> serializer(MachineKind machine) {
		MapCodec<FluidRecipe> codec = RecordCodecBuilder.mapCodec(i -> i.group(
				Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
				ItemPart.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(FluidRecipe::items),
				FluidAmount.CODEC.listOf().optionalFieldOf("fluids", List.of()).forGetter(FluidRecipe::fluids),
				FluidAmount.CODEC.listOf().optionalFieldOf("fluid_results", List.of()).forGetter(FluidRecipe::fluidResults),
				ItemStackTemplate.CODEC.listOf().optionalFieldOf("results", List.of()).forGetter(FluidRecipe::results),
				ExtraCodecs.POSITIVE_INT.optionalFieldOf("time", 200).forGetter(FluidRecipe::time),
				MachineCompanionEffort.CODEC.optionalFieldOf("companion_effort_per_quarter", 0).forGetter(FluidRecipe::companionEffort),
				Codec.STRING.optionalFieldOf("capability", "").forGetter(FluidRecipe::capability),
				Ingredient.CODEC.optionalFieldOf("tool").forGetter(FluidRecipe::tool)
		).apply(i, (info, items, fluids, fluidResults, results, time, effort, capability, tool) ->
				new FluidRecipe(machine, info, items, fluids, fluidResults, results, time, effort, capability, tool)));
		StreamCodec<RegistryFriendlyByteBuf, FluidRecipe> stream = StreamCodec.composite(
				Recipe.CommonInfo.STREAM_CODEC, recipe -> recipe.commonInfo,
				ItemPart.STREAM_CODEC.apply(ByteBufCodecs.list()), FluidRecipe::items,
				FluidAmount.STREAM_CODEC.apply(ByteBufCodecs.list()), FluidRecipe::fluids,
				FluidAmount.STREAM_CODEC.apply(ByteBufCodecs.list()), FluidRecipe::fluidResults,
				ItemStackTemplate.STREAM_CODEC.apply(ByteBufCodecs.list()), FluidRecipe::results,
				ByteBufCodecs.VAR_INT, FluidRecipe::time,
				Extras.STREAM_CODEC, recipe -> new Extras(recipe.companionEffort, recipe.capability, recipe.tool),
				(info, items, fluids, fluidResults, results, time, extras) -> new FluidRecipe(machine, info, items, fluids,
						fluidResults, results, time, extras.companionEffort(), extras.capability(), extras.tool()));
		return new RecipeSerializer<>(codec, stream);
	}
}
