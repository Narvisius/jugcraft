package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipe;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipes;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.chemistry.PetroItems;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * PVAc wood glue (docs/features/pvac-glue.md): the Chemical Reactor joins 500 mB of ethylene and 250 mB of oxygen with two
 * cider vinegar into 500 mB of vinyl acetate; the Polymerization Reactor turns that into four pots of glue; and glue binds
 * three paper into a book and makes a piston sticky.
 */
public class PvacGlueGameTests {
	@GameTest
	public void vinylAcetatePolymerizesIntoGlue(GameTestHelper helper) {
		FluidRecipe monomer = recipe(helper, MachineKind.CHEMICAL_REACTOR, "chemical_reaction/vinyl_acetate");
		helper.assertTrue(monomer.fluidResults().getFirst().fluid() == PetroFluids.VINYL_ACETATE.fluid()
				&& monomer.fluidResults().getFirst().amount() == 500, "The reactor gives 500 mB of vinyl acetate");
		ItemStack glue = recipe(helper, MachineKind.POLYMERIZATION_REACTOR, "polymerization/vinyl_acetate").results().getFirst().create();
		helper.assertTrue(glue.is(PetroItems.PVAC_GLUE) && glue.getCount() == 4, "Vinyl acetate polymerizes into " + glue);
		helper.succeed();
	}

	@GameTest
	public void glueBindsBooksAndStickyPistons(GameTestHelper helper) {
		ItemStack paper = new ItemStack(Items.PAPER);
		ItemStack glue = new ItemStack(PetroItems.PVAC_GLUE);
		helper.assertTrue(crafts(helper, CraftingInput.of(2, 2, List.of(paper, paper, paper, glue))).is(Items.BOOK),
				"Three paper and glue do not make a book");
		helper.assertTrue(crafts(helper, CraftingInput.of(2, 1, List.of(new ItemStack(Items.PISTON), glue))).is(Items.STICKY_PISTON),
				"A piston and glue do not make a sticky piston");
		helper.succeed();
	}

	private static ItemStack crafts(GameTestHelper helper, CraftingInput input) {
		RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
		Optional<RecipeHolder<CraftingRecipe>> recipe = crafting.getRecipeFor(input, helper.getLevel());
		return recipe.map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
	}

	private static FluidRecipe recipe(GameTestHelper helper, MachineKind kind, String path) {
		FluidRecipes.Entry entry = FluidRecipes.entries(helper.getLevel().getServer(), kind).stream()
				.filter(e -> e.id().equals(Jugcraft.id(path))).findFirst().orElse(null);
		helper.assertTrue(entry != null, "No recipe " + path);
		return entry.recipe();
	}
}
