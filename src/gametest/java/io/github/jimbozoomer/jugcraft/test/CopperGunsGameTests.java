package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.guns.GunItem;
import io.github.jimbozoomer.jugcraft.guns.GunShots;
import io.github.jimbozoomer.jugcraft.guns.GunSpec;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/**
 * In-game tests for the copper guns (slice 12B, docs/features/guns.md), through the server's entry points as
 * {@link GunsGameTests} does: the Penny Pistol, held in one hand, and the Magpie SMG are fed from magazines at their left
 * side, the Conductor Carbine from a magazine too; each lands its shot on what it aims at. Shooters face south (+z).
 */
public class CopperGunsGameTests {
	/** The copper guns, in the order the creative tab shows them. */
	static final List<String> GUNS = List.of("penny_pistol", "conductor_carbine", "magpie_smg");
	/** The barrels' attachments and the stocks. */
	static final List<String> BARRELS_AND_STOCKS = List.of("silencer", "baffled_silencer", "muzzle_brake", "extended_barrel",
			"light_stock", "weighted_stock", "wooden_stock");
	/** The grips, the bayonets, the scopes and the Laser Sight. */
	static final List<String> LONG_GUN = List.of("light_grip", "tactical_grip", "iron_bayonet", "steel_bayonet", "diamond_bayonet",
			"netherite_bayonet", "long_scope", "medium_scope", "reflex_sight", "laser_sight");

	/**
	 * Each is registered with its numbers and its recipe loads, and fires bullets that neither set alight nor lift what
	 * they hit. The Penny Pistol, held in one hand (so two of them are two guns at once), fires one light round a pull
	 * from a magazine of ten; the Conductor Carbine one rifle round a pull from eight; the Magpie SMG light rounds for as
	 * long as the trigger is held, from thirty-two. Each is loaded a magazine at a time. The Penny Pistol takes the barrels'
	 * attachments and the stocks only; the Conductor Carbine and the Magpie SMG those, the grips, the bayonets, the scopes
	 * and the Laser Sight, and the Conductor Carbine the magazines too.
	 */
	@GameTest
	public void copperGunsAreRegistered(GameTestHelper helper) {
		for (String name : GUNS) {
			GunSpec spec = JugcraftGuns.SPECS.get(name);
			GunItem gun = JugcraftGuns.GUNS.get(name);
			helper.assertTrue(spec != null && gun != null && gun.spec() == spec, name + " is not registered with its numbers");
			helper.assertTrue(JugcraftGuns.shot(gun).equals("bullet") && !JugcraftGuns.ignites(gun) && !JugcraftGuns.levitates(gun),
					name + " does not fire plain bullets");
			helper.assertTrue(spec.pellets() == 1 && !spec.byShell(), name + " is not one bullet a shot, loaded a magazine at a time");
			helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(name))).isPresent(),
					"The " + name + " recipe does not load");
			helper.assertTrue(JugcraftGuns.ACCEPTS.get(name).containsAll(BARRELS_AND_STOCKS),
					"The " + name + " does not take the barrels' attachments and the stocks");
		}
		GunSpec penny = JugcraftGuns.SPECS.get("penny_pistol");
		GunSpec conductor = JugcraftGuns.SPECS.get("conductor_carbine");
		GunSpec magpie = JugcraftGuns.SPECS.get("magpie_smg");
		helper.assertTrue(penny.ammo().equals("light_round") && penny.capacity() == 10 && !penny.auto()
				&& JugcraftGuns.ONE_HANDED.contains("penny_pistol"), "The Penny Pistol is not a pistol held in one hand, of ten light rounds");
		helper.assertTrue(conductor.ammo().equals("rifle_round") && conductor.capacity() == 8 && !conductor.auto()
				&& !JugcraftGuns.ONE_HANDED.contains("conductor_carbine"), "The Conductor Carbine is not a carbine of eight rifle rounds, one a pull");
		helper.assertTrue(magpie.ammo().equals("light_round") && magpie.capacity() == 32 && magpie.auto()
				&& !JugcraftGuns.ONE_HANDED.contains("magpie_smg"), "The Magpie SMG is not an automatic of thirty-two light rounds");
		helper.assertTrue(JugcraftGuns.ACCEPTS.get("penny_pistol").equals(BARRELS_AND_STOCKS),
				"The Penny Pistol takes " + JugcraftGuns.ACCEPTS.get("penny_pistol"));
		for (String name : List.of("conductor_carbine", "magpie_smg")) {
			helper.assertTrue(JugcraftGuns.ACCEPTS.get(name).containsAll(LONG_GUN), "The " + name + " does not take the grips, bayonets and scopes");
		}
		helper.assertTrue(JugcraftGuns.ACCEPTS.get("conductor_carbine").containsAll(List.of("extended_magazine", "speed_magazine"))
				&& !JugcraftGuns.ACCEPTS.get("magpie_smg").contains("extended_magazine"),
				"Of the copper guns, the magazines do not fit the Conductor Carbine alone");
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemInHand(InteractionHand.MAIN_HAND, loaded("penny_pistol", 0));
		player.setItemInHand(InteractionHand.OFF_HAND, loaded("penny_pistol", 0));
		helper.assertTrue(GunItem.dual(player), "A Penny Pistol in each hand is not two guns at once");
		helper.succeed();
	}

	/**
	 * Side by side, each fires its last round at a pig three blocks off and its one shot takes one round's damage; each
	 * then loads from the inventory, nothing in halfway through its reload and full as it ends, two rounds left in the
	 * inventory.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 200)
	public void copperGunsFireAndLoad(GameTestHelper helper) {
		NetherGunsGameTests.fireAndLoad(helper, GUNS);
	}

	/** A Penny Pistol in each hand: each fires from its own hand and lands its shot, spending a round of its own. */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 20)
	public void pennyPistolsFireFromBothHands(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = NetherGunsGameTests.sturdyPig(helper, new BlockPos(1, 2, 4));
		ServerPlayer shooter = GunsGameTests.shooter(helper, "penny_pistol", 10, pig, GameType.SURVIVAL);
		ItemStack other = loaded("penny_pistol", 10);
		shooter.setItemInHand(InteractionHand.OFF_HAND, other);
		helper.assertTrue(GunShots.fire(shooter), "The main hand's Penny Pistol did not fire");
		helper.assertTrue(GunShots.fire(shooter, InteractionHand.OFF_HAND), "The other hand's Penny Pistol did not fire");
		float expected = 2 * JugcraftGuns.SPECS.get("penny_pistol").damage();
		helper.assertTrue(Math.abs(200.0F - pig.getHealth() - expected) < 1.0E-3F,
				"Two shots took " + (200.0F - pig.getHealth()) + " from the pig, not " + expected);
		helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 9 && GunItem.loaded(other) == 9,
				"Each Penny Pistol did not spend a round of its own: " + GunItem.loaded(shooter.getMainHandItem()) + " and " + GunItem.loaded(other));
		helper.succeed();
	}

	private static ItemStack loaded(String gun, int rounds) {
		ItemStack stack = new ItemStack(JugcraftGuns.GUNS.get(gun));
		GunItem.setLoaded(stack, rounds);
		return stack;
	}
}
