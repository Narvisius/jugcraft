package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.blueprint.Blueprint;
import io.github.jimbozoomer.jugcraft.blueprint.BlueprintItem;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind;
import io.github.jimbozoomer.jugcraft.nuclear.JugcraftNuclear;
import io.github.jimbozoomer.jugcraft.nuclear.PlantInterior;
import io.github.jimbozoomer.jugcraft.nuclear.StorageTanks;
import io.github.jimbozoomer.jugcraft.nuclear.TankKitItem;
import io.github.jimbozoomer.jugcraft.placement.StructureReach;
import io.github.jimbozoomer.jugcraft.powerline.JugcraftPowerLines;
import io.github.jimbozoomer.jugcraft.powerline.LineKind;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * The placing reach (Justin, October 8 2026; placement/StructureReach.java): an MBS can be placed as far away as any
 * block can, plus twice its longest side, while it is in the main hand: a blueprint, a tank kit, a piece of plant
 * equipment, a power line support, a wall screen. Anything else keeps the ordinary reach, and the longer reach is for
 * placing only.
 */
public class StructureReachGameTests {
	private static final String ARENA = "jugcraft-test:drone_tower";

	private static double hold(GameTestHelper helper, Player player, ItemStack stack) {
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		StructureReach.update(player);
		return StructureReach.bonus(player);
	}

	private static void expect(GameTestHelper helper, String what, double got, double wanted) {
		helper.assertTrue(Math.abs(got - wanted) < 1e-6, what + ": reach bonus " + got + ", wanted " + wanted);
	}

	@GameTest(structure = ARENA, maxTicks = 20)
	public void reachIsTwiceTheLongestSide(GameTestHelper helper) {
		if (TestFocus.skip("blueprints")) {
			helper.succeed();
			return;
		}
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		double ordinary = player.blockInteractionRange();
		expect(helper, "a dirt block", hold(helper, player, new ItemStack(Items.DIRT)), 0);
		// The turbine-generator is 50 blocks long.
		expect(helper, "the turbine-generator", hold(helper, player, new ItemStack(JugcraftNuclear.EQUIPMENT_ITEMS.get(EquipmentKind.TURBINE_GENERATOR))), 100);
		helper.assertTrue(Math.abs(player.blockInteractionRange() - (ordinary + 100)) < 1e-6,
				"holding the turbine-generator, the reach is " + player.blockInteractionRange() + ", not " + (ordinary + 100));
		// A lattice tower is 56 blocks tall.
		expect(helper, "a lattice tower", hold(helper, player, new ItemStack(JugcraftPowerLines.ITEMS.get(LineKind.LATTICE_TOWER))), 112);
		// The plant status screen is 3 blocks across and 2 high.
		expect(helper, "a status screen", hold(helper, player, new ItemStack(PlantInterior.BLOCKS.get("status_screen"))), 6);
		// A large tank kit: twice its blueprint's longest side.
		Blueprint large = TankKitItem.blueprint("general_large");
		helper.assertTrue(large != null, "the large general tank kit has no blueprint");
		expect(helper, "a large tank kit", hold(helper, player, new ItemStack(StorageTanks.ITEMS.get("tank_kit_general_large"))),
				2 * Math.max(large.sizeX, Math.max(large.sizeY, large.sizeZ)));
		// The Small Church blueprint is 19 blocks tall: placed up to 38 blocks further away than a block.
		ItemStack church = BlueprintItem.stack("small_church");
		expect(helper, "the Small Church blueprint", hold(helper, player, church), 38);
		helper.assertTrue(Math.abs(StructureReach.range(player, church) - (ordinary + 38)) < 1e-6,
				"the Small Church's placing reach is " + StructureReach.range(player, church) + ", not " + (ordinary + 38));
		// Put away, the reach is back to normal.
		expect(helper, "an empty hand", hold(helper, player, ItemStack.EMPTY), 0);
		helper.assertTrue(Math.abs(player.blockInteractionRange() - ordinary) < 1e-6, "the reach did not go back to normal");
		helper.succeed();
	}

	@GameTest(structure = ARENA, maxTicks = 20)
	public void farBlocksAreForPlacingOnly(GameTestHelper helper) {
		if (TestFocus.skip("blueprints")) {
			helper.succeed();
			return;
		}
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		hold(helper, player, new ItemStack(JugcraftNuclear.EQUIPMENT_ITEMS.get(EquipmentKind.TURBINE_GENERATOR)));
		BlockPos far = player.blockPosition().offset(30, 0, 0);
		helper.assertTrue(player.isWithinBlockInteractionRange(far, 0), "30 blocks away is out of the turbine-generator's placing reach");
		helper.assertFalse(StructureReach.withinOrdinaryReach(player, far), "30 blocks away counts as ordinary reach");
		helper.assertTrue(StructureReach.withinOrdinaryReach(player, player.blockPosition().offset(2, 0, 0)), "2 blocks away is out of ordinary reach");
		helper.succeed();
	}
}
