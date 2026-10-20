package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipes;
import io.github.jimbozoomer.jugcraft.chemistry.PetroItems;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Boric acid (docs/features/boric-acid.md): the Chemical Reactor frees two boric acid from a borax with 250 mB of
 * sulfuric acid; three boric acid in four tungsten plates and two steel plates make a control rod assembly; and two
 * of those go into every reactor pressure vessel.
 */
public class BoricAcidGameTests {
	@GameTest
	public void boraxGivesBoricAcid(GameTestHelper helper) {
		List<FluidRecipes.Entry> reactor = FluidRecipes.entries(helper.getLevel().getServer(), MachineKind.CHEMICAL_REACTOR);
		FluidRecipes.Entry entry = reactor.stream().filter(e -> e.id().equals(Jugcraft.id("chemical_reaction/boric_acid"))).findFirst()
				.orElse(null);
		helper.assertTrue(entry != null, "No boric acid recipe in the Chemical Reactor");
		ItemStack result = entry.recipe().results().getFirst().create();
		helper.assertTrue(result.is(PetroItems.BORIC_ACID) && result.getCount() == 2, "A borax gives " + result);
		helper.assertTrue(entry.recipe().fluids().getFirst().amount() == 250, "It takes 250 mB of acid");
		helper.succeed();
	}

	@GameTest
	public void controlRodsGoIntoTheReactorVessel(GameTestHelper helper) {
		RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
		ItemStack tungsten = stack("tungsten_plate");
		ItemStack steel = stack("steel_plate");
		ItemStack boric = new ItemStack(PetroItems.BORIC_ACID);
		CraftingInput rods = CraftingInput.of(3, 3, List.of(tungsten, boric, tungsten, tungsten, boric, tungsten, steel, boric, steel));
		Optional<RecipeHolder<CraftingRecipe>> built = crafting.getRecipeFor(rods, helper.getLevel());
		helper.assertTrue(built.isPresent() && built.get().value().assemble(rods).is(PetroItems.CONTROL_ROD_ASSEMBLY),
				"Boric acid, tungsten and steel do not make a control rod assembly");
		ItemStack rod = new ItemStack(PetroItems.CONTROL_ROD_ASSEMBLY);
		ItemStack block = stack("steel_block");
		CraftingInput vessel = CraftingInput.of(3, 3, List.of(tungsten, stack("processor"), tungsten, rod, stack("leu_fuel_assembly"), rod,
				block, block, block));
		built = crafting.getRecipeFor(vessel, helper.getLevel());
		helper.assertTrue(built.isPresent() && built.get().value().assemble(vessel).is(item("reactor_pressure_vessel")),
				"Two control rod assemblies do not complete the reactor pressure vessel");
		CraftingInput without = CraftingInput.of(3, 3, List.of(tungsten, stack("processor"), tungsten, block, stack("leu_fuel_assembly"), block,
				block, block, block));
		helper.assertTrue(crafting.getRecipeFor(without, helper.getLevel()).isEmpty(), "The vessel is still made without control rods");
		helper.succeed();
	}

	/** Boron in solar cells (docs/features/boron-solar.md): two silicon and a boric acid give two boron-doped silicon,
	 * and two of those go into the Advanced Solar Panel in place of its aluminum plates. */
	@GameTest
	public void dopedSiliconGoesIntoTheAdvancedSolarPanel(GameTestHelper helper) {
		RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
		ItemStack silicon = stack("silicon");
		CraftingInput dope = CraftingInput.of(3, 1, List.of(silicon, silicon, new ItemStack(PetroItems.BORIC_ACID)));
		Optional<RecipeHolder<CraftingRecipe>> doped = crafting.getRecipeFor(dope, helper.getLevel());
		ItemStack wafers = doped.map(r -> r.value().assemble(dope)).orElse(ItemStack.EMPTY);
		helper.assertTrue(wafers.is(PetroItems.BORON_DOPED_SILICON) && wafers.getCount() == 2, "Silicon and boric acid give " + wafers);
		ItemStack panel = stack("solar_panel");
		ItemStack wafer = new ItemStack(PetroItems.BORON_DOPED_SILICON);
		CraftingInput advanced = CraftingInput.of(3, 3, List.of(panel, panel, panel, wafer, stack("processor"), wafer,
				stack("aluminum_cable"), stack("titanium_ingot"), stack("aluminum_cable")));
		Optional<RecipeHolder<CraftingRecipe>> built = crafting.getRecipeFor(advanced, helper.getLevel());
		helper.assertTrue(built.isPresent() && built.get().value().assemble(advanced).is(item("advanced_solar_panel")),
				"Doped silicon does not complete the Advanced Solar Panel");
		helper.succeed();
	}

	private static Item item(String path) {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id(path));
	}

	private static ItemStack stack(String path) {
		return new ItemStack(item(path));
	}
}
