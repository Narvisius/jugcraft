package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.Candies;
import io.github.jimbozoomer.jugcraft.agriculture.CandyFlavour;
import io.github.jimbozoomer.jugcraft.agriculture.CandyKind;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipes;
import io.github.jimbozoomer.jugcraft.chemistry.PetroItems;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Sour candy and corn syrup (docs/features/sour-candy.md): the Chemical Reactor crystallizes citric acid from four lemons
 * (two) or four oranges (one) in 250 mB of water, and hydrolyses four corn with 50 mB of hydrochloric acid into a bottle
 * of glucose syrup; the syrup crafts into three sugar and gives back its bottle; and citric acid is the Candy Kettle's
 * Sour flavour, whose candy gives Jump Boost.
 */
public class SourCandyGameTests {
	@GameTest
	public void theReactorMakesCitricAcidAndGlucoseSyrup(GameTestHelper helper) {
		List<FluidRecipes.Entry> reactor = FluidRecipes.entries(helper.getLevel().getServer(), MachineKind.CHEMICAL_REACTOR);
		helper.assertTrue(makes(reactor, "citric_acid_lemon", PetroItems.CITRIC_ACID, 2), "Four lemons give two citric acid");
		helper.assertTrue(makes(reactor, "citric_acid_orange", PetroItems.CITRIC_ACID, 1), "Four oranges give one citric acid");
		helper.assertTrue(makes(reactor, "glucose_syrup", PetroItems.GLUCOSE_SYRUP, 1), "Four corn give a glucose syrup");
		helper.succeed();
	}

	@GameTest
	public void glucoseSyrupCraftsIntoSugar(GameTestHelper helper) {
		RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
		CraftingInput input = CraftingInput.of(1, 1, List.of(new ItemStack(PetroItems.GLUCOSE_SYRUP)));
		Optional<RecipeHolder<CraftingRecipe>> recipe = crafting.getRecipeFor(input, helper.getLevel());
		helper.assertTrue(recipe.isPresent(), "Glucose syrup does not craft");
		ItemStack sugar = recipe.get().value().assemble(input);
		helper.assertTrue(sugar.is(Items.SUGAR) && sugar.getCount() == 3, "Glucose syrup gives " + sugar);
		NonNullList<ItemStack> left = recipe.get().value().getRemainingItems(input);
		helper.assertTrue(left.getFirst().is(Items.GLASS_BOTTLE), "The bottle is not given back: " + left);
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void citricAcidMakesSourCandy(GameTestHelper helper) {
		helper.assertTrue(CandyFlavour.of(new ItemStack(PetroItems.CITRIC_ACID)) == CandyFlavour.SOUR, "Citric acid is not the Sour flavour");
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 1, 2));
		ItemStack candy = Candies.make(CandyKind.HARD_CANDY, List.of(CandyFlavour.SOUR), List.of(Candies.NATURAL), 1);
		candy.finishUsingItem(helper.getLevel(), pig);
		helper.assertTrue(pig.hasEffect(MobEffects.JUMP_BOOST), "Sour candy gives no Jump Boost");
		helper.succeed();
	}

	private static boolean makes(List<FluidRecipes.Entry> recipes, String name, Item item, int count) {
		return recipes.stream().filter(entry -> entry.id().equals(Jugcraft.id("chemical_reaction/" + name))).findFirst()
				.map(entry -> {
					ItemStack result = entry.recipe().results().getFirst().create();
					return result.is(item) && result.getCount() == count;
				}).orElse(false);
	}
}
