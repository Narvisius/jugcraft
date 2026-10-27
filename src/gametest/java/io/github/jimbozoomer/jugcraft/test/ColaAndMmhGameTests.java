package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipe;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipes;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.chemistry.PetroItems;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.machine.MachineRecipes;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
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
 * Cola and MMH (docs/features/cola-and-mmh.md): the Chemical Reactor purifies industrial phosphoric acid over charcoal
 * into a food-grade vial and carbonates cola syrup into three colas; the vial crafts with caramel, sugar, cinnamon,
 * vanilla and lemon into the syrup; cola gives Speed; the Bulk Chemical Reactor methylates hydrazine into MMH, the
 * reactor seals it into a tank, and the Rocket Workshop makes four storable bipropellant motors from it (hydrazine
 * still makes three).
 */
public class ColaAndMmhGameTests {
	@GameTest
	public void theReactorMakesFoodGradeAcidAndCola(GameTestHelper helper) {
		List<FluidRecipes.Entry> reactor = FluidRecipes.entries(helper.getLevel().getServer(), MachineKind.CHEMICAL_REACTOR);
		helper.assertTrue(makes(reactor, "phosphate_food_grade_acid", PetroItems.FOOD_GRADE_PHOSPHORIC_ACID, 1),
				"Industrial acid over charcoal gives one food-grade vial");
		helper.assertTrue(makes(reactor, "phosphate_cola", PetroItems.COLA, 3), "Carbonated cola syrup gives three colas");
		helper.assertTrue(makes(reactor, "cryo_mmh_tank", PetroItems.MMH_TANK, 1), "The reactor seals MMH into a tank");
		helper.succeed();
	}

	@GameTest
	public void theBulkReactorMethylatesHydrazine(GameTestHelper helper) {
		FluidRecipe mmh = recipe(FluidRecipes.entries(helper.getLevel().getServer(), MachineKind.CHEMICAL_REACTOR), "cryo_mmh")
				.orElseThrow(() -> helper.assertionException("No MMH recipe"));
		helper.assertTrue(mmh.capability().equals("jugcraft:storable_propellants"), "MMH is not a storable propellant job: " + mmh.capability());
		helper.assertTrue(mmh.fluids().stream().anyMatch(f -> f.fluid() == PetroFluids.HYDRAZINE.source())
				&& mmh.fluids().stream().anyMatch(f -> f.fluid() == PetroFluids.METHYL_CHLORIDE.fluid()), "MMH is not made from hydrazine and methyl chloride");
		helper.assertTrue(mmh.fluidResults().getFirst().fluid() == PetroFluids.MONOMETHYLHYDRAZINE.source()
				&& mmh.fluidResults().getFirst().amount() == 250, "MMH gives " + mmh.fluidResults());
		helper.succeed();
	}

	@GameTest
	public void foodGradeAcidCraftsIntoColaSyrup(GameTestHelper helper) {
		List<ItemStack> grid = List.of(new ItemStack(PetroItems.FOOD_GRADE_PHOSPHORIC_ACID), item("caramel"), new ItemStack(Items.SUGAR),
				new ItemStack(Items.SUGAR), item("cinnamon"), item("vanilla_pods"), item("lemon"), ItemStack.EMPTY, ItemStack.EMPTY);
		CraftingInput input = CraftingInput.of(3, 3, grid);
		RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
		Optional<RecipeHolder<CraftingRecipe>> recipe = crafting.getRecipeFor(input, helper.getLevel());
		helper.assertTrue(recipe.isPresent(), "The food-grade vial, caramel, sugar, cinnamon, vanilla and lemon do not craft");
		ItemStack syrup = recipe.get().value().assemble(input);
		helper.assertTrue(syrup.is(PetroItems.COLA_SYRUP) && syrup.getCount() == 1, "They craft " + syrup);
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void colaGivesSpeed(GameTestHelper helper) {
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 1, 2));
		new ItemStack(PetroItems.COLA).finishUsingItem(helper.getLevel(), pig);
		helper.assertTrue(pig.hasEffect(MobEffects.SPEED), "Cola gives no Speed");
		helper.succeed();
	}

	@GameTest
	public void mmhMakesFourBipropellantMotors(GameTestHelper helper) {
		ItemStack nozzles = item("rocket_nozzle");
		nozzles.setCount(2);
		helper.assertTrue(motors(helper, PetroItems.MMH_TANK, nozzles) == 4, "MMH and N2O4 do not make four motors");
		helper.assertTrue(motors(helper, PetroItems.HYDRAZINE_TANK, nozzles) == 3, "Hydrazine and N2O4 no longer make three motors");
		helper.succeed();
	}

	private static int motors(GameTestHelper helper, Item fuel, ItemStack nozzles) {
		List<ItemStack> inputs = List.of(new ItemStack(fuel), new ItemStack(PetroItems.N2O4_TANK), nozzles.copy());
		return MachineRecipes.findMulti(helper.getLevel(), MachineKind.ROCKET_WORKSHOP, inputs)
				.map(match -> match.recipe().output().create())
				.filter(motor -> motor.is(PetroItems.STORABLE_BIPROPELLANT_MOTOR))
				.map(ItemStack::getCount).orElse(0);
	}

	private static ItemStack item(String name) {
		return new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id(name)));
	}

	private static Optional<FluidRecipe> recipe(List<FluidRecipes.Entry> recipes, String name) {
		return recipes.stream().filter(entry -> entry.id().equals(Jugcraft.id("chemical_reaction/" + name))).findFirst().map(FluidRecipes.Entry::recipe);
	}

	private static boolean makes(List<FluidRecipes.Entry> recipes, String name, Item item, int count) {
		return recipe(recipes, name).map(recipe -> {
			ItemStack result = recipe.results().getFirst().create();
			return result.is(item) && result.getCount() == count;
		}).orElse(false);
	}
}
