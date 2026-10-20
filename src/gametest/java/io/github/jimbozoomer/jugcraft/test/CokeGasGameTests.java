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
import net.minecraft.world.item.Items;

/**
 * Coking with gas recovery (docs/features/coke-gas-recovery.md): the Chemical Reactor bakes four coal into four coke
 * and keeps 200 mB of methane.
 */
public class CokeGasGameTests {
	@GameTest
	public void reactorCokesCoalAndKeepsTheGas(GameTestHelper helper) {
		FluidRecipe recipe = FluidRecipes.entries(helper.getLevel().getServer(), MachineKind.CHEMICAL_REACTOR).stream()
				.filter(e -> e.id().equals(Jugcraft.id("chemical_reaction/coke_with_gas_recovery"))).findFirst()
				.orElseThrow(() -> helper.assertionException("No coke gas recovery recipe")).recipe();
		helper.assertTrue(recipe.itemsMatch(List.of(new ItemStack(Items.COAL, 4))), "The reactor does not take four coal");
		ItemStack coke = recipe.results().getFirst().create();
		helper.assertTrue(coke.is(BuiltInRegistries.ITEM.getValue(Jugcraft.id("coke"))) && coke.getCount() == 4, "The reactor gives " + coke);
		helper.assertTrue(recipe.fluidResults().size() == 1 && recipe.fluidResults().getFirst().fluid() == PetroFluids.METHANE.fluid()
				&& recipe.fluidResults().getFirst().amount() == 200, "The reactor keeps " + recipe.fluidResults());
		helper.succeed();
	}
}
