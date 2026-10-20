package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipe;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipes;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/**
 * Hydrogen from zinc (docs/features/zinc-hydrogen.md): the Chemical Reactor dissolves a zinc ingot in 500 mB of
 * hydrochloric acid and gives 500 mB of hydrogen.
 */
public class ZincHydrogenGameTests {
	@GameTest
	public void reactorDissolvesZincIntoHydrogen(GameTestHelper helper) {
		FluidRecipe recipe = FluidRecipes.entries(helper.getLevel().getServer(), MachineKind.CHEMICAL_REACTOR).stream()
				.filter(e -> e.id().equals(Jugcraft.id("chemical_reaction/hydrogen_from_zinc"))).findFirst()
				.orElseThrow(() -> helper.assertionException("No hydrogen-from-zinc recipe")).recipe();
		helper.assertTrue(recipe.itemsMatch(List.of(new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("zinc_ingot"))))),
				"The reactor does not take a zinc ingot");
		helper.assertTrue(recipe.fluids().size() == 1 && recipe.fluids().getFirst().fluid() == PetroFluids.HYDROCHLORIC_ACID.source()
				&& recipe.fluids().getFirst().amount() == 500, "The reactor takes " + recipe.fluids());
		helper.assertTrue(recipe.fluidResults().size() == 1 && recipe.fluidResults().getFirst().fluid() == PetroFluids.HYDROGEN.fluid()
				&& recipe.fluidResults().getFirst().amount() == 500, "The reactor gives " + recipe.fluidResults());
		helper.succeed();
	}
}
