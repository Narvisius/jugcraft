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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/**
 * In-game tests for slice 14C's guns (docs/features/guns.md), through the server's entry points as
 * {@link GunsGameTests} does: the Sprocket Gun, an automatic rifle fed from a pan magazine on its top; the Navvy
 * Shotgun, an automatic shotgun fed from a box magazine; and the Freeholder Rifle, a quick-firing rifle. All three are
 * held in both hands. Shooters face south (+z).
 */
public class BrassQuickFirersGameTests {
	/** The slice's guns, in the order the creative tab shows them. */
	static final List<String> GUNS = List.of("sprocket_gun", "navvy_shotgun", "freeholder_rifle");

	/**
	 * Each is registered with its numbers and its recipe loads, firing bullets from a magazine, held in both hands. The
	 * Sprocket Gun fires rifle rounds as long as the trigger is held, forty a pan, and takes every attachment the owner
	 * made for it, the four sights among them. The Navvy Shotgun fires six pellets a shell as long as the trigger is
	 * held, eight shells a magazine, the only automatic gun that fires shells; it takes every attachment the owner made
	 * for it and no scope. The Freeholder Rifle fires rifle rounds a pull at a time, twenty a magazine, quicker than any
	 * other gun that fires rifle rounds a pull at a time, and takes both magazines and every sight but the Long Scope.
	 */
	@GameTest
	public void brassQuickFirersAreRegistered(GameTestHelper helper) {
		for (String name : GUNS) {
			GunSpec spec = JugcraftGuns.SPECS.get(name);
			GunItem gun = JugcraftGuns.GUNS.get(name);
			helper.assertTrue(spec != null && gun != null && gun.spec() == spec, name + " is not registered with its numbers");
			helper.assertTrue(!JugcraftGuns.ignites(gun) && !JugcraftGuns.levitates(gun), name + " sets alight or lifts");
			helper.assertTrue(JugcraftGuns.shot(gun).equals("bullet") && !spec.byShell(), name + " does not fire bullets from a magazine");
			helper.assertTrue(!JugcraftGuns.ONE_HANDED.contains(name) && !JugcraftGuns.CARBINES.contains(name),
					name + " is not held in both hands");
			helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(name))).isPresent(),
					"The " + name + " recipe does not load");
		}
		GunSpec sprocket = JugcraftGuns.SPECS.get("sprocket_gun");
		GunSpec navvy = JugcraftGuns.SPECS.get("navvy_shotgun");
		GunSpec freeholder = JugcraftGuns.SPECS.get("freeholder_rifle");
		helper.assertTrue(sprocket.ammo().equals("rifle_round") && sprocket.auto() && sprocket.pellets() == 1 && sprocket.capacity() == 40,
				"The Sprocket Gun is not an automatic rifle of forty rifle rounds");
		helper.assertTrue(navvy.ammo().equals("buckshot_shell") && navvy.auto() && navvy.pellets() == 6 && navvy.capacity() == 8,
				"The Navvy Shotgun is not an automatic shotgun of eight shells, six pellets a shell");
		helper.assertTrue(freeholder.ammo().equals("rifle_round") && !freeholder.auto() && freeholder.capacity() == 20,
				"The Freeholder Rifle is not a rifle of twenty rifle rounds, a pull a shot");
		JugcraftGuns.SPECS.forEach((name, spec) -> {
			helper.assertTrue(name.equals("navvy_shotgun") || !(spec.auto() && spec.ammo().equals("buckshot_shell")),
					"The " + name + " is an automatic gun that fires shells too");
			helper.assertTrue(name.equals("freeholder_rifle") || spec.auto() || !spec.ammo().equals("rifle_round")
					|| spec.interval() > freeholder.interval(), "The " + name + " fires rifle rounds a pull at a time as quickly as the Freeholder Rifle");
		});

		List<String> sprocketTakes = JugcraftGuns.ACCEPTS.get("sprocket_gun");
		helper.assertTrue(sprocketTakes.containsAll(BrassSidearmsGameTests.BARREL) && sprocketTakes.containsAll(BrassSidearmsGameTests.STOCKS)
				&& sprocketTakes.containsAll(List.of("light_grip", "tactical_grip")) && sprocketTakes.containsAll(FrontierGunsIVGameTests.BAYONETS)
				&& sprocketTakes.containsAll(BrassSidearmsGameTests.SIGHTS) && sprocketTakes.size() == 17, "The Sprocket Gun takes " + sprocketTakes);
		List<String> navvyTakes = JugcraftGuns.ACCEPTS.get("navvy_shotgun");
		helper.assertTrue(navvyTakes.containsAll(BrassSidearmsGameTests.BARREL) && navvyTakes.containsAll(BrassSidearmsGameTests.STOCKS)
				&& navvyTakes.containsAll(List.of("extended_magazine", "speed_magazine", "light_grip", "tactical_grip"))
				&& navvyTakes.containsAll(FrontierGunsIVGameTests.BAYONETS) && !navvyTakes.contains("reflex_sight")
				&& navvyTakes.size() == 15, "The Navvy Shotgun takes " + navvyTakes);
		List<String> freeholderTakes = JugcraftGuns.ACCEPTS.get("freeholder_rifle");
		helper.assertTrue(freeholderTakes.equals(List.of("extended_magazine", "speed_magazine", "medium_scope", "reflex_sight", "laser_sight")),
				"The Freeholder Rifle takes " + freeholderTakes);
		helper.succeed();
	}

	/**
	 * An empty Sprocket Gun with fifty rifle rounds in the inventory changes its pan: nothing is in before its reload's
	 * time, forty rounds after it, and ten are left. A shot then takes its damage from the pig.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 140)
	public void sprocketGunChangesItsPan(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = sturdyPig(helper, new BlockPos(1, 2, 4));
		GunSpec spec = JugcraftGuns.SPECS.get("sprocket_gun");
		ServerPlayer shooter = GunsGameTests.shooter(helper, "sprocket_gun", 0, pig, GameType.SURVIVAL);
		shooter.getInventory().setItem(1, new ItemStack(JugcraftGuns.ROUNDS.get("rifle_round"), 50));
		helper.assertTrue(GunShots.reload(shooter), "The empty Sprocket Gun did not start to reload");
		helper.runAfterDelay(spec.reload() - 3, () -> helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 0,
				"The Sprocket Gun's pan was in before its reload's time"));
		helper.runAfterDelay(spec.reload() + 2, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 40, "The Sprocket Gun loaded "
					+ GunItem.loaded(shooter.getMainHandItem()) + " rounds, not forty");
			helper.assertTrue(GunShots.count(shooter.getInventory(), JugcraftGuns.ROUNDS.get("rifle_round")) == 10,
					"The reload did not leave ten rounds in the inventory");
			helper.assertTrue(GunShots.fire(shooter), "The loaded Sprocket Gun did not fire");
			helper.assertTrue(Math.abs(200.0F - pig.getHealth() - spec.damage()) < 1.0E-3F,
					"The shot took " + (200.0F - pig.getHealth()) + ", not " + spec.damage());
			helper.succeed();
		});
	}

	/**
	 * An empty Navvy Shotgun with twelve buckshot shells in the inventory changes its magazine: eight shells go in and
	 * four are left. A shot then spends one shell, and at close range its pellets land together on the pig.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 120)
	public void navvyShotgunChangesItsMagazine(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = sturdyPig(helper, new BlockPos(1, 2, 4));
		GunSpec spec = JugcraftGuns.SPECS.get("navvy_shotgun");
		ServerPlayer shooter = GunsGameTests.shooter(helper, "navvy_shotgun", 0, pig, GameType.SURVIVAL);
		shooter.getInventory().setItem(1, new ItemStack(JugcraftGuns.ROUNDS.get("buckshot_shell"), 12));
		helper.assertTrue(GunShots.reload(shooter), "The empty Navvy Shotgun did not start to reload");
		helper.runAfterDelay(spec.reload() + 2, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 8, "The Navvy Shotgun loaded "
					+ GunItem.loaded(shooter.getMainHandItem()) + " shells, not eight");
			helper.assertTrue(GunShots.count(shooter.getInventory(), JugcraftGuns.ROUNDS.get("buckshot_shell")) == 4,
					"The reload did not leave four shells in the inventory");
			helper.assertTrue(GunShots.fire(shooter), "The loaded Navvy Shotgun did not fire");
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 7, "The shot did not spend one shell");
			float taken = 200.0F - pig.getHealth();
			helper.assertTrue(taken >= spec.damage() * (spec.pellets() - 2),
					"At close range the pellets took only " + taken + " (one pellet is " + spec.damage() + ")");
			helper.succeed();
		});
	}

	/** A still pig of 200 health, so that no shot kills it. */
	private static Mob sturdyPig(GameTestHelper helper, BlockPos pos) {
		Mob pig = GunsGameTests.pig(helper, pos);
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
		pig.setHealth(200.0F);
		return pig;
	}
}
