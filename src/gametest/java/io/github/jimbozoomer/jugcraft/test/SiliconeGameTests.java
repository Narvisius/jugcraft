package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipe;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipes;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.chemistry.PetroItems;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

/**
 * Silicone (docs/features/silicone.md): the Chemical Reactor chlorinates 500 mB of methane with 500 mB of chlorine into
 * 500 mB of methyl chloride; one silicon with that and 250 mB of water gives two silicone rubber and 250 mB of
 * hydrochloric acid; and the Polymer Molding Press's gasket mold presses two silicone into three gaskets.
 */
public class SiliconeGameTests {
	@GameTest
	public void methaneAndChlorineMakeMethylChloride(GameTestHelper helper) {
		FluidRecipe recipe = recipe(helper, "chemical_reaction/methyl_chloride");
		helper.assertTrue(recipe.fluids().size() == 2 && recipe.fluids().get(0).fluid() == PetroFluids.METHANE.fluid()
				&& recipe.fluids().get(1).fluid() == PetroFluids.CHLORINE.fluid()
				&& recipe.fluids().get(0).amount() == 500 && recipe.fluids().get(1).amount() == 500,
				"Methyl chloride takes " + recipe.fluids());
		helper.assertTrue(recipe.fluidResults().size() == 1 && recipe.fluidResults().getFirst().fluid() == PetroFluids.METHYL_CHLORIDE.fluid()
				&& recipe.fluidResults().getFirst().amount() == 500, "Methyl chloride gives " + recipe.fluidResults());
		helper.succeed();
	}

	@GameTest
	public void siliconBecomesSiliconeAndAcid(GameTestHelper helper) {
		FluidRecipe recipe = recipe(helper, "chemical_reaction/silicone");
		helper.assertTrue(recipe.itemsMatch(List.of(new ItemStack(item("silicon")))), "Silicone does not take one silicon");
		helper.assertTrue(recipe.fluids().size() == 2 && recipe.fluids().get(0).fluid() == PetroFluids.METHYL_CHLORIDE.fluid()
				&& recipe.fluids().get(0).amount() == 500 && recipe.fluids().get(1).fluid() == Fluids.WATER
				&& recipe.fluids().get(1).amount() == 250, "Silicone takes " + recipe.fluids());
		ItemStack silicone = recipe.results().getFirst().create();
		helper.assertTrue(silicone.is(PetroItems.SILICONE) && silicone.getCount() == 2, "The reaction makes " + silicone);
		helper.assertTrue(recipe.fluidResults().size() == 1
				&& recipe.fluidResults().getFirst().fluid() == PetroFluids.HYDROCHLORIC_ACID.source()
				&& recipe.fluidResults().getFirst().amount() == 250, "The reaction gives back " + recipe.fluidResults());
		helper.succeed();
	}

	@GameTest
	public void gasketMoldPressesSilicone(GameTestHelper helper) {
		FluidRecipe recipe = recipe(helper, "chemical_reaction/mold_silicone_gasket");
		helper.assertTrue(recipe.capability().equals("jugcraft:polymer_molding"), "The press recipe's role is " + recipe.capability());
		helper.assertTrue(recipe.tool().isPresent() && recipe.tool().get().test(new ItemStack(item("gasket_mold"))),
				"The press recipe does not use the gasket mold");
		helper.assertTrue(recipe.itemsMatch(List.of(new ItemStack(PetroItems.SILICONE, 2))), "The press does not take two silicone");
		ItemStack gaskets = recipe.results().getFirst().create();
		helper.assertTrue(gaskets.is(item("gasket")) && gaskets.getCount() == 3, "The press makes " + gaskets);
		helper.succeed();
	}

	private static Item item(String path) {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id(path));
	}

	private static FluidRecipe recipe(GameTestHelper helper, String path) {
		return FluidRecipes.entries(helper.getLevel().getServer(), MachineKind.CHEMICAL_REACTOR).stream()
				.filter(e -> e.id().equals(Jugcraft.id(path))).findFirst()
				.orElseThrow(() -> helper.assertionException("No recipe " + path)).recipe();
	}
}
