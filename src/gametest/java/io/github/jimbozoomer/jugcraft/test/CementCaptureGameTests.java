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
 * Cement with CO2 capture (docs/features/cement-co2-capture.md): the Chemical Reactor burns four cement mix into four
 * cement and keeps the 250 mB of carbon dioxide the lime gives off.
 */
public class CementCaptureGameTests {
	@GameTest
	public void reactorBurnsCementAndKeepsTheCarbonDioxide(GameTestHelper helper) {
		FluidRecipe recipe = FluidRecipes.entries(helper.getLevel().getServer(), MachineKind.CHEMICAL_REACTOR).stream()
				.filter(e -> e.id().equals(Jugcraft.id("chemical_reaction/cement_with_co2_capture"))).findFirst()
				.orElseThrow(() -> helper.assertionException("No cement capture recipe")).recipe();
		helper.assertTrue(recipe.itemsMatch(List.of(new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("cement_mix")), 4))),
				"The reactor does not take four cement mix");
		ItemStack cement = recipe.results().getFirst().create();
		helper.assertTrue(cement.is(BuiltInRegistries.ITEM.getValue(Jugcraft.id("cement"))) && cement.getCount() == 4, "The reactor gives " + cement);
		helper.assertTrue(recipe.fluidResults().size() == 1 && recipe.fluidResults().getFirst().fluid() == PetroFluids.CARBON_DIOXIDE.fluid()
				&& recipe.fluidResults().getFirst().amount() == 250, "The reactor keeps " + recipe.fluidResults());
		helper.succeed();
	}
}
