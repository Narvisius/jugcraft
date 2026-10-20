package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Aluminum's roles (docs/features/aluminum-roles.md): aluminum plates craft four aluminum cables (steel armour makes
 * three), a zeppelin and two heliostats, as the steel plates do.
 */
public class AluminumRolesGameTests {
	@GameTest
	public void aluminumSheathedCableMakesFour(GameTestHelper helper) {
		ItemStack wire = stack("aluminum_wire");
		ItemStack plate = stack("aluminum_plate");
		ItemStack silver = stack("silver_cable");
		ItemStack cables = crafts(helper, CraftingInput.of(3, 2, List.of(wire, plate, wire, silver, silver, silver)));
		helper.assertTrue(cables.is(item("aluminum_cable")) && cables.getCount() == 4, "Aluminum-sheathed cable crafts " + cables);
		helper.succeed();
	}

	@GameTest
	public void aluminumFramesAZeppelinAndBacksHeliostats(GameTestHelper helper) {
		ItemStack cell = stack("hydrogen_lift_cell");
		ItemStack plate = stack("aluminum_plate");
		ItemStack zeppelin = crafts(helper, CraftingInput.of(3, 3, List.of(cell, cell, cell, cell, stack("diesel_engine"), cell,
				plate, new ItemStack(Items.CHEST), plate)));
		helper.assertTrue(zeppelin.is(item("zeppelin")), "An aluminum-framed zeppelin crafts " + zeppelin);
		ItemStack glass = new ItemStack(Items.GLASS_PANE);
		ItemStack none = ItemStack.EMPTY;
		ItemStack heliostats = crafts(helper, CraftingInput.of(3, 3, List.of(glass, glass, glass, none, stack("electric_motor"), none,
				none, plate, none)));
		helper.assertTrue(heliostats.is(item("heliostat")) && heliostats.getCount() == 2, "An aluminum-backed heliostat crafts " + heliostats);
		helper.succeed();
	}

	private static Item item(String path) {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id(path));
	}

	private static ItemStack stack(String path) {
		return new ItemStack(item(path));
	}

	private static ItemStack crafts(GameTestHelper helper, CraftingInput input) {
		RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
		Optional<RecipeHolder<CraftingRecipe>> recipe = crafting.getRecipeFor(input, helper.getLevel());
		return recipe.map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
	}
}
