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
 * In-game tests for slice 13B's frontier guns (docs/features/guns.md), through the server's entry points as
 * {@link GunsGameTests} does: the Fencepost Carbine, a lever-action carbine loaded a round at a time; the Waystation
 * Shotgun, a lever-action shotgun loaded a shell at a time; and the Highball Rifle, a single-shot rifle. Each lands its
 * shot on what it aims at. Shooters face south (+z).
 */
public class FrontierGunsIIGameTests {
	/** The slice's guns, in the order the creative tab shows them. */
	static final List<String> GUNS = List.of("fencepost_carbine", "waystation_shotgun", "highball_rifle");
	/** The bayonets. */
	static final List<String> BAYONETS = List.of("iron_bayonet", "steel_bayonet", "diamond_bayonet", "netherite_bayonet");

	/**
	 * Each is registered with its numbers and its recipe loads, held in both hands. The Fencepost Carbine fires rifle
	 * rounds, one bullet a pull, from a tube of seven loaded a round at a time; the Waystation Shotgun eight pellets a pull
	 * from buckshot shells, eight loaded a shell at a time; the Highball Rifle one heavy rifle round, loaded each time,
	 * reaching furthest of the three. The Fencepost Carbine takes the grips and the bayonets; the Waystation Shotgun the
	 * barrel attachments too; the Highball Rifle those, the scopes and the Laser Sight.
	 */
	@GameTest
	public void frontierGunsIIAreRegistered(GameTestHelper helper) {
		for (String name : GUNS) {
			GunSpec spec = JugcraftGuns.SPECS.get(name);
			GunItem gun = JugcraftGuns.GUNS.get(name);
			helper.assertTrue(spec != null && gun != null && gun.spec() == spec, name + " is not registered with its numbers");
			helper.assertTrue(JugcraftGuns.shot(gun).equals("bullet") && !JugcraftGuns.ignites(gun) && !JugcraftGuns.levitates(gun),
					name + " does not fire plain bullets");
			helper.assertTrue(!spec.auto() && !JugcraftGuns.ONE_HANDED.contains(name), name + " is not one pull a shot, held in both hands");
			helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(name))).isPresent(),
					"The " + name + " recipe does not load");
			helper.assertTrue(JugcraftGuns.ACCEPTS.get(name).containsAll(BAYONETS)
					&& JugcraftGuns.ACCEPTS.get(name).containsAll(List.of("light_grip", "tactical_grip")),
					"The " + name + " takes " + JugcraftGuns.ACCEPTS.get(name));
		}
		GunSpec carbine = JugcraftGuns.SPECS.get("fencepost_carbine");
		GunSpec shotgun = JugcraftGuns.SPECS.get("waystation_shotgun");
		GunSpec rifle = JugcraftGuns.SPECS.get("highball_rifle");
		helper.assertTrue(carbine.ammo().equals("rifle_round") && carbine.pellets() == 1 && carbine.capacity() == 7 && carbine.byShell(),
				"The Fencepost Carbine is not a carbine of seven rifle rounds loaded one at a time");
		helper.assertTrue(shotgun.ammo().equals("buckshot_shell") && shotgun.pellets() == 8 && shotgun.capacity() == 8 && shotgun.byShell(),
				"The Waystation Shotgun is not a shotgun of eight shells, eight pellets a shot, loaded one at a time");
		helper.assertTrue(rifle.ammo().equals("rifle_round") && rifle.pellets() == 1 && rifle.capacity() == 1 && !rifle.byShell(),
				"The Highball Rifle is not a single-shot rifle");
		helper.assertTrue(rifle.damage() > carbine.damage() && rifle.range() > carbine.range() && carbine.range() > shotgun.range(),
				"The Highball Rifle does not hit hardest and reach furthest, or the Waystation Shotgun not least");
		helper.assertTrue(!JugcraftGuns.ACCEPTS.get("fencepost_carbine").contains("silencer")
				&& JugcraftGuns.ACCEPTS.get("waystation_shotgun").contains("silencer")
				&& !JugcraftGuns.ACCEPTS.get("waystation_shotgun").contains("long_scope")
				&& JugcraftGuns.ACCEPTS.get("highball_rifle").containsAll(List.of("silencer", "long_scope", JugcraftGuns.LASER_SIGHT)),
				"The barrel attachments and scopes do not fit as the owner's parts allow");
		helper.succeed();
	}

	/**
	 * Side by side, each fires its last round at a pig three blocks off and its one shot takes one round's damage (the
	 * shotgun's most of its pellets'); each then loads from the inventory in its own time, the carbine and the shotgun a
	 * round at a time with one in after their first round's time, the rifle with nothing in halfway through its reload, and
	 * each full as it ends, two rounds left in the inventory.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 260)
	public void frontierGunsIIFireAndLoad(GameTestHelper helper) {
		NetherGunsGameTests.fireAndLoad(helper, GUNS);
	}
}
