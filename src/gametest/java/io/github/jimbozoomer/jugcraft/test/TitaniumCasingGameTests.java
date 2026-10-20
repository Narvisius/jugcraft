package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.machine.MachineRecipes;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/**
 * Titanium rocket casings (docs/features/titanium-rocket-casing.md): the rocket workshop makes two rocket casings from two
 * titanium plates.
 */
public class TitaniumCasingGameTests {
	@GameTest
	public void workshopMakesCasingsFromTitanium(GameTestHelper helper) {
		List<ItemStack> inputs = List.of(new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("titanium_plate")), 2));
		MachineRecipes.MultiMatch match = MachineRecipes.findMulti(helper.getLevel(), MachineKind.ROCKET_WORKSHOP, inputs)
				.orElseThrow(() -> helper.assertionException("The rocket workshop does not take titanium plates"));
		ItemStack casings = match.recipe().output().create();
		helper.assertTrue(casings.is(BuiltInRegistries.ITEM.getValue(Jugcraft.id("rocket_casing"))) && casings.getCount() == 2,
				"Two titanium plates make " + casings);
		helper.succeed();
	}
}
