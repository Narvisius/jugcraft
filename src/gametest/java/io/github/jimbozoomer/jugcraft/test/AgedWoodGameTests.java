package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipe;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipes;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Aged wood (docs/features/aged-wood.md): the Chemical Reactor ages eight pine or maple planks with 100 mB of nitric
 * acid into eight aged planks; aged planks are planks (they make sticks and anything else of planks) and craft their own
 * stairs and slabs.
 */
public class AgedWoodGameTests {
	@GameTest
	public void reactorAgesPineAndMaple(GameTestHelper helper) {
		for (String wood : List.of("pine", "maple")) {
			FluidRecipe recipe = FluidRecipes.entries(helper.getLevel().getServer(), MachineKind.CHEMICAL_REACTOR).stream()
					.filter(e -> e.id().equals(Jugcraft.id("chemical_reaction/aged_" + wood + "_planks"))).findFirst()
					.orElseThrow(() -> helper.assertionException("No aged " + wood + " recipe")).recipe();
			helper.assertTrue(recipe.itemsMatch(List.of(new ItemStack(item(wood + "_planks"), 8))), "The reactor does not take 8 " + wood + " planks");
			helper.assertTrue(recipe.fluids().size() == 1 && recipe.fluids().getFirst().fluid() == PetroFluids.NITRIC_ACID.source()
					&& recipe.fluids().getFirst().amount() == 100, "The " + wood + " treatment takes " + recipe.fluids());
			ItemStack result = recipe.results().getFirst().create();
			helper.assertTrue(result.is(item("aged_" + wood + "_planks")) && result.getCount() == 8, "The reactor gives " + result);
		}
		helper.succeed();
	}

	@GameTest
	public void agedPlanksCraftStairsSlabsAndCountAsPlanks(GameTestHelper helper) {
		for (String wood : List.of("pine", "maple")) {
			ItemStack planks = new ItemStack(item("aged_" + wood + "_planks"));
			helper.assertTrue(planks.is(ItemTags.PLANKS), "Aged " + wood + " planks are not planks");
			ItemStack none = ItemStack.EMPTY;
			ItemStack stairs = crafts(helper, CraftingInput.of(3, 3, List.of(planks, none, none, planks, planks, none, planks, planks, planks)));
			helper.assertTrue(stairs.is(item("aged_" + wood + "_stairs")) && stairs.getCount() == 4, "Six planks craft " + stairs);
			ItemStack slabs = crafts(helper, CraftingInput.of(3, 1, List.of(planks, planks, planks)));
			helper.assertTrue(slabs.is(item("aged_" + wood + "_slab")) && slabs.getCount() == 6, "Three planks craft " + slabs);
		}
		helper.succeed();
	}

	private static Item item(String path) {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id(path));
	}

	private static ItemStack crafts(GameTestHelper helper, CraftingInput input) {
		RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
		Optional<RecipeHolder<CraftingRecipe>> recipe = crafting.getRecipeFor(input, helper.getLevel());
		return recipe.map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
	}
}
