package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.guns.GunItem;
import io.github.jimbozoomer.jugcraft.guns.GunSpec;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;

/**
 * In-game tests for slice 13C's frontier guns (docs/features/guns.md), through the server's entry points as
 * {@link GunsGameTests} does: the Flatboat Gun, a single-shot punt gun of sixteen pellets; the Windlass Musket, a
 * flintlock repeater fed paper cartridges from a magazine; and the Millstone Rifle, a heavy lever-action rifle loaded a
 * round at a time. Each lands its shot on what it aims at. Shooters face south (+z).
 */
public class FrontierGunsIIIGameTests {
	/** The slice's guns, in the order the creative tab shows them. */
	static final List<String> GUNS = List.of("flatboat_gun", "windlass_musket", "millstone_rifle");
	/** The bayonets. */
	static final List<String> BAYONETS = List.of("iron_bayonet", "steel_bayonet", "diamond_bayonet", "netherite_bayonet");
	/** The scopes and the Laser Sight. */
	static final List<String> OPTICS = List.of("long_scope", "medium_scope", "reflex_sight", JugcraftGuns.LASER_SIGHT);

	/**
	 * Each is registered with its numbers and its recipe loads, fires plain bullets one pull a shot and is held in both
	 * hands. The Flatboat Gun fires sixteen pellets of buckshot, one shell to a load, and takes the scopes and the Laser
	 * Sight only; the Windlass Musket paper cartridges from a magazine of seven, and takes the Light and Vertical Grips and
	 * the bayonets but no scope; the Millstone Rifle rifle rounds, eight loaded a round at a time, hitting harder and
	 * working slower than the Drover Rifle, and takes what the Drover Rifle takes.
	 */
	@GameTest
	public void frontierGunsIIIAreRegistered(GameTestHelper helper) {
		for (String name : GUNS) {
			GunSpec spec = JugcraftGuns.SPECS.get(name);
			GunItem gun = JugcraftGuns.GUNS.get(name);
			helper.assertTrue(spec != null && gun != null && gun.spec() == spec, name + " is not registered with its numbers");
			helper.assertTrue(JugcraftGuns.shot(gun).equals("bullet") && !JugcraftGuns.ignites(gun) && !JugcraftGuns.levitates(gun),
					name + " does not fire plain bullets");
			helper.assertTrue(!spec.auto() && !JugcraftGuns.ONE_HANDED.contains(name), name + " is not one pull a shot, held in both hands");
			helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(name))).isPresent(),
					"The " + name + " recipe does not load");
		}
		GunSpec flatboat = JugcraftGuns.SPECS.get("flatboat_gun");
		GunSpec windlass = JugcraftGuns.SPECS.get("windlass_musket");
		GunSpec millstone = JugcraftGuns.SPECS.get("millstone_rifle");
		GunSpec drover = JugcraftGuns.SPECS.get("drover_rifle");
		helper.assertTrue(flatboat.ammo().equals("buckshot_shell") && flatboat.pellets() == 16 && flatboat.capacity() == 1
				&& !flatboat.byShell(), "The Flatboat Gun is not a single shot of sixteen pellets of buckshot");
		helper.assertTrue(windlass.ammo().equals("paper_cartridge") && windlass.pellets() == 1 && windlass.capacity() == 7
				&& !windlass.byShell(), "The Windlass Musket is not a repeater of seven paper cartridges, loaded a magazine at a time");
		helper.assertTrue(millstone.ammo().equals("rifle_round") && millstone.pellets() == 1 && millstone.capacity() == 8
				&& millstone.byShell(), "The Millstone Rifle is not a rifle of eight rifle rounds loaded one at a time");
		helper.assertTrue(millstone.damage() > drover.damage() && millstone.interval() > drover.interval(),
				"The Millstone Rifle does not hit harder and work slower than the Drover Rifle");
		helper.assertTrue(millstone.range() > windlass.range() && windlass.range() > flatboat.range(),
				"The Millstone Rifle does not reach furthest and the Flatboat Gun least");
		helper.assertTrue(JugcraftGuns.ACCEPTS.get("flatboat_gun").equals(OPTICS),
				"The Flatboat Gun takes " + JugcraftGuns.ACCEPTS.get("flatboat_gun"));
		helper.assertTrue(JugcraftGuns.ACCEPTS.get("windlass_musket").containsAll(BAYONETS)
				&& JugcraftGuns.ACCEPTS.get("windlass_musket").containsAll(List.of("light_grip", "vertical_grip"))
				&& JugcraftGuns.ACCEPTS.get("windlass_musket").stream().noneMatch(OPTICS::contains),
				"The Windlass Musket takes " + JugcraftGuns.ACCEPTS.get("windlass_musket"));
		helper.assertTrue(JugcraftGuns.ACCEPTS.get("millstone_rifle").equals(JugcraftGuns.ACCEPTS.get("drover_rifle")),
				"The Millstone Rifle takes " + JugcraftGuns.ACCEPTS.get("millstone_rifle"));
		helper.succeed();
	}

	/**
	 * Side by side, each fires its last round at a pig three blocks off and its one shot takes one round's damage (the
	 * Flatboat Gun's most of its pellets'); each then loads from the inventory in its own time, the punt gun and the
	 * musket with nothing in halfway through their reloads, the rifle a round at a time with one in after its first
	 * round's time, and each full as it ends, two rounds left in the inventory.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 200)
	public void frontierGunsIIIFireAndLoad(GameTestHelper helper) {
		NetherGunsGameTests.fireAndLoad(helper, GUNS);
	}
}
