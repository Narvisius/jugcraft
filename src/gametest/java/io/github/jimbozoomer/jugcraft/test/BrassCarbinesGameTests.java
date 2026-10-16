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
 * In-game tests for slice 14B's guns (docs/features/guns.md), through the server's entry points as
 * {@link GunsGameTests} does: the Holler Carbine and the Minuet Carbine, slice 14A's Holler and Minuet Pistols made
 * over into carbines, and the Grange Carbine, fed from a box magazine. All three are held in both hands. Shooters face
 * south (+z).
 */
public class BrassCarbinesGameTests {
	/** The slice's guns, in the order the creative tab shows them. */
	static final List<String> GUNS = List.of("holler_carbine", "minuet_carbine", "grange_carbine");

	/**
	 * Each is registered with its numbers and its recipe loads, one pull a shot, firing bullets from a magazine, held in
	 * both hands and never a pistol-carbine. Each conversion fires the rounds its pistol fires, as many a magazine,
	 * reaches further and strays less aimed; the Holler Carbine fires quicker than the Holler Pistol and hits less hard,
	 * the Minuet Carbine hits harder than the Minuet Pistol. The Holler Carbine takes no attachment (the owner made none
	 * for it); the Minuet Carbine takes the stocks and the four sights; the Grange Carbine fires rifle rounds, twelve a
	 * magazine, and takes every attachment the owner made for it, nineteen.
	 */
	@GameTest
	public void brassCarbinesAreRegistered(GameTestHelper helper) {
		for (String name : GUNS) {
			GunSpec spec = JugcraftGuns.SPECS.get(name);
			GunItem gun = JugcraftGuns.GUNS.get(name);
			helper.assertTrue(spec != null && gun != null && gun.spec() == spec, name + " is not registered with its numbers");
			helper.assertTrue(!spec.auto() && !JugcraftGuns.ignites(gun) && !JugcraftGuns.levitates(gun), name + " is not one pull a shot");
			helper.assertTrue(JugcraftGuns.shot(gun).equals("bullet") && !spec.byShell(), name + " does not fire bullets from a magazine");
			helper.assertTrue(!JugcraftGuns.ONE_HANDED.contains(name) && !JugcraftGuns.CARBINES.contains(name),
					name + " is not held in both hands");
			helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(name))).isPresent(),
					"The " + name + " recipe does not load");
		}
		for (String[] pair : new String[][] {{"holler_carbine", "holler_pistol"}, {"minuet_carbine", "minuet_pistol"}}) {
			GunSpec carbine = JugcraftGuns.SPECS.get(pair[0]);
			GunSpec pistol = JugcraftGuns.SPECS.get(pair[1]);
			helper.assertTrue(carbine.ammo().equals(pistol.ammo()) && carbine.capacity() == pistol.capacity(),
					"The " + pair[0] + " does not fire the " + pair[1] + "'s rounds, as many a magazine");
			helper.assertTrue(carbine.range() > pistol.range() && carbine.aimSpread() < pistol.aimSpread(),
					"The " + pair[0] + " does not reach further and stray less aimed than the " + pair[1]);
		}
		GunSpec holler = JugcraftGuns.SPECS.get("holler_carbine");
		GunSpec hollerPistol = JugcraftGuns.SPECS.get("holler_pistol");
		helper.assertTrue(holler.interval() < hollerPistol.interval() && holler.damage() < hollerPistol.damage(),
				"The Holler Carbine does not fire quicker, and hit less hard, than the Holler Pistol");
		helper.assertTrue(JugcraftGuns.SPECS.get("minuet_carbine").damage() > JugcraftGuns.SPECS.get("minuet_pistol").damage(),
				"The Minuet Carbine does not hit harder than the Minuet Pistol");
		GunSpec grange = JugcraftGuns.SPECS.get("grange_carbine");
		helper.assertTrue(grange.ammo().equals("rifle_round") && grange.capacity() == 12,
				"The Grange Carbine is not a magazine of twelve rifle rounds");

		List<String> hollerTakes = JugcraftGuns.ACCEPTS.getOrDefault("holler_carbine", List.of());
		helper.assertTrue(hollerTakes.isEmpty(), "The Holler Carbine takes " + hollerTakes);
		List<String> minuetTakes = JugcraftGuns.ACCEPTS.get("minuet_carbine");
		helper.assertTrue(minuetTakes.containsAll(BrassSidearmsGameTests.STOCKS) && minuetTakes.containsAll(BrassSidearmsGameTests.SIGHTS)
				&& minuetTakes.size() == 7, "The Minuet Carbine takes " + minuetTakes);
		List<String> grangeTakes = JugcraftGuns.ACCEPTS.get("grange_carbine");
		helper.assertTrue(grangeTakes.containsAll(BrassSidearmsGameTests.BARREL) && grangeTakes.containsAll(BrassSidearmsGameTests.STOCKS)
				&& grangeTakes.containsAll(List.of("extended_magazine", "speed_magazine", "light_grip", "tactical_grip"))
				&& grangeTakes.containsAll(FrontierGunsIVGameTests.BAYONETS) && grangeTakes.containsAll(BrassSidearmsGameTests.SIGHTS)
				&& grangeTakes.size() == 19, "The Grange Carbine takes " + grangeTakes);
		helper.succeed();
	}

	/**
	 * An empty Grange Carbine with twenty rifle rounds in the inventory changes its magazine: nothing is in before its
	 * reload's time, twelve rounds after it, and eight are left. A shot then takes its damage from the pig.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 120)
	public void grangeCarbineChangesItsMagazine(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = sturdyPig(helper, new BlockPos(1, 2, 4));
		GunSpec spec = JugcraftGuns.SPECS.get("grange_carbine");
		ServerPlayer shooter = GunsGameTests.shooter(helper, "grange_carbine", 0, pig, GameType.SURVIVAL);
		shooter.getInventory().setItem(1, new ItemStack(JugcraftGuns.ROUNDS.get("rifle_round"), 20));
		helper.assertTrue(GunShots.reload(shooter), "The empty Grange Carbine did not start to reload");
		helper.runAfterDelay(spec.reload() - 3, () -> helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 0,
				"The Grange Carbine's magazine was in before its reload's time"));
		helper.runAfterDelay(spec.reload() + 2, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 12, "The Grange Carbine loaded "
					+ GunItem.loaded(shooter.getMainHandItem()) + " rounds, not twelve");
			helper.assertTrue(GunShots.count(shooter.getInventory(), JugcraftGuns.ROUNDS.get("rifle_round")) == 8,
					"The reload did not leave eight rounds in the inventory");
			helper.assertTrue(GunShots.fire(shooter), "The loaded Grange Carbine did not fire");
			helper.assertTrue(Math.abs(200.0F - pig.getHealth() - spec.damage()) < 1.0E-3F,
					"The shot took " + (200.0F - pig.getHealth()) + ", not " + spec.damage());
			helper.succeed();
		});
	}

	/**
	 * An empty Minuet Carbine with ten light rounds and ten rifle rounds in the inventory loads the light rounds and
	 * leaves the rifle rounds. A shot then takes its damage from the pig.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 120)
	public void minuetCarbineLoadsLightRounds(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = sturdyPig(helper, new BlockPos(1, 2, 4));
		GunSpec spec = JugcraftGuns.SPECS.get("minuet_carbine");
		ServerPlayer shooter = GunsGameTests.shooter(helper, "minuet_carbine", 0, pig, GameType.SURVIVAL);
		shooter.getInventory().setItem(1, new ItemStack(JugcraftGuns.ROUNDS.get("rifle_round"), 10));
		shooter.getInventory().setItem(2, new ItemStack(JugcraftGuns.ROUNDS.get("light_round"), 10));
		helper.assertTrue(GunShots.reload(shooter), "The empty Minuet Carbine did not start to reload");
		helper.runAfterDelay(spec.reload() + 2, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 10, "The Minuet Carbine loaded "
					+ GunItem.loaded(shooter.getMainHandItem()) + " rounds, not ten");
			helper.assertTrue(GunShots.count(shooter.getInventory(), JugcraftGuns.ROUNDS.get("light_round")) == 0
					&& GunShots.count(shooter.getInventory(), JugcraftGuns.ROUNDS.get("rifle_round")) == 10,
					"The reload did not take the light rounds and leave the rifle rounds");
			helper.assertTrue(GunShots.fire(shooter), "The loaded Minuet Carbine did not fire");
			helper.assertTrue(Math.abs(200.0F - pig.getHealth() - spec.damage()) < 1.0E-3F,
					"The shot took " + (200.0F - pig.getHealth()) + ", not " + spec.damage());
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
