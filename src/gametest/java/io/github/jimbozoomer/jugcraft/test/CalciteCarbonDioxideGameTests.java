package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipe;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipes;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Carbon dioxide from calcite (docs/features/calcite-co2.md): the Chemical Reactor dissolves a calcite in 500 mB of
 * hydrochloric acid and gives 250 mB of carbon dioxide.
 */
public class CalciteCarbonDioxideGameTests {
	@GameTest
	public void reactorDissolvesCalciteIntoCarbonDioxide(GameTestHelper helper) {
		FluidRecipe recipe = FluidRecipes.entries(helper.getLevel().getServer(), MachineKind.CHEMICAL_REACTOR).stream()
				.filter(e -> e.id().equals(Jugcraft.id("chemical_reaction/carbon_dioxide_from_calcite"))).findFirst()
				.orElseThrow(() -> helper.assertionException("No carbon-dioxide-from-calcite recipe")).recipe();
		helper.assertTrue(recipe.itemsMatch(List.of(new ItemStack(Items.CALCITE))), "The reactor does not take a calcite");
		helper.assertTrue(recipe.fluids().size() == 1 && recipe.fluids().getFirst().fluid() == PetroFluids.HYDROCHLORIC_ACID.source()
				&& recipe.fluids().getFirst().amount() == 500, "The reactor takes " + recipe.fluids());
		helper.assertTrue(recipe.fluidResults().size() == 1 && recipe.fluidResults().getFirst().fluid() == PetroFluids.CARBON_DIOXIDE.fluid()
				&& recipe.fluidResults().getFirst().amount() == 250, "The reactor gives " + recipe.fluidResults());
		helper.succeed();
	}
}
