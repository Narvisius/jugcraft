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
 * In-game tests for slice 13A's frontier guns (docs/features/guns.md), through the server's entry points as
 * {@link GunsGameTests} does: the Tabby Revolver, held in one hand and loaded a round at a time; the Lookout Rifle, a
 * lever-action rifle loaded a round at a time; and the Ammonite Carbine, loaded a magazine at a time. Each lands its shot
 * on what it aims at. Shooters face south (+z).
 */
public class FrontierGunsGameTests {
	/** The slice's guns, in the order the creative tab shows them. */
	static final List<String> GUNS = List.of("tabby_revolver", "lookout_rifle", "ammonite_carbine");
	/** The stocks. */
	static final List<String> STOCKS = List.of("light_stock", "weighted_stock", "wooden_stock");

	/**
	 * Each is registered with its numbers and its recipe loads, and fires plain bullets, one a pull. The Tabby Revolver
	 * fires light rounds from a cylinder of six, held in one hand (so two are two guns at once) and loaded a round at a
	 * time; the Lookout Rifle rifle rounds from a tube of eight, loaded a round at a time; the Ammonite Carbine rifle
	 * rounds from a magazine of sixteen. The Tabby Revolver takes the barrel attachments only; the Lookout Rifle the
	 * stocks, the Light and Tactical Grips, the bayonets, the scopes and the Laser Sight; the Ammonite Carbine the stocks
	 * only.
	 */
	@GameTest
	public void frontierGunsAreRegistered(GameTestHelper helper) {
		for (String name : GUNS) {
			GunSpec spec = JugcraftGuns.SPECS.get(name);
			GunItem gun = JugcraftGuns.GUNS.get(name);
			helper.assertTrue(spec != null && gun != null && gun.spec() == spec, name + " is not registered with its numbers");
			helper.assertTrue(JugcraftGuns.shot(gun).equals("bullet") && !JugcraftGuns.ignites(gun) && !JugcraftGuns.levitates(gun),
					name + " does not fire plain bullets");
			helper.assertTrue(spec.pellets() == 1 && !spec.auto(), name + " is not one bullet a pull");
			helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(name))).isPresent(),
					"The " + name + " recipe does not load");
		}
		GunSpec tabby = JugcraftGuns.SPECS.get("tabby_revolver");
		GunSpec lookout = JugcraftGuns.SPECS.get("lookout_rifle");
		GunSpec ammonite = JugcraftGuns.SPECS.get("ammonite_carbine");
		helper.assertTrue(tabby.ammo().equals("light_round") && tabby.capacity() == 6 && tabby.byShell()
				&& JugcraftGuns.ONE_HANDED.contains("tabby_revolver"),
				"The Tabby Revolver is not a revolver held in one hand, of six light rounds loaded one at a time");
		helper.assertTrue(lookout.ammo().equals("rifle_round") && lookout.capacity() == 8 && lookout.byShell()
				&& !JugcraftGuns.ONE_HANDED.contains("lookout_rifle"),
				"The Lookout Rifle is not a rifle of eight rifle rounds loaded one at a time");
		helper.assertTrue(ammonite.ammo().equals("rifle_round") && ammonite.capacity() == 16 && !ammonite.byShell()
				&& !JugcraftGuns.ONE_HANDED.contains("ammonite_carbine"),
				"The Ammonite Carbine is not a carbine of sixteen rifle rounds, loaded a magazine at a time");
		helper.assertTrue(lookout.range() > ammonite.range() && ammonite.range() > tabby.range(),
				"The Lookout Rifle does not reach furthest and the Tabby Revolver least");
		helper.assertTrue(JugcraftGuns.ACCEPTS.get("tabby_revolver").equals(List.of("silencer", "baffled_silencer", "muzzle_brake",
				"extended_barrel")), "The Tabby Revolver takes " + JugcraftGuns.ACCEPTS.get("tabby_revolver"));
		helper.assertTrue(JugcraftGuns.ACCEPTS.get("lookout_rifle").containsAll(STOCKS)
				&& JugcraftGuns.ACCEPTS.get("lookout_rifle").containsAll(List.of("light_grip", "tactical_grip", "iron_bayonet",
						"steel_bayonet", "diamond_bayonet", "netherite_bayonet", "long_scope", "medium_scope", "reflex_sight",
						JugcraftGuns.LASER_SIGHT)),
				"The Lookout Rifle takes " + JugcraftGuns.ACCEPTS.get("lookout_rifle"));
		helper.assertTrue(JugcraftGuns.ACCEPTS.get("ammonite_carbine").equals(STOCKS),
				"The Ammonite Carbine takes " + JugcraftGuns.ACCEPTS.get("ammonite_carbine"));
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemInHand(InteractionHand.MAIN_HAND, loaded("tabby_revolver", 0));
		player.setItemInHand(InteractionHand.OFF_HAND, loaded("tabby_revolver", 0));
		helper.assertTrue(GunItem.dual(player), "Two Tabby Revolvers are not two guns at once");
		helper.succeed();
	}

	/**
	 * Side by side, each fires its last round at a pig three blocks off and its one shot takes one round's damage; each
	 * then loads from the inventory in its own time, the revolver and the lever rifle a round at a time with one in after
	 * its first round's time, the carbine with nothing in halfway through its reload, and each full as it ends, two rounds
	 * left in the inventory.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 200)
	public void frontierGunsFireAndLoad(GameTestHelper helper) {
		NetherGunsGameTests.fireAndLoad(helper, GUNS);
	}

	/** A Tabby Revolver in each hand fires from each and lands both shots, each spending a round of its own. */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 20)
	public void tabbyRevolversFireFromBothHands(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = NetherGunsGameTests.sturdyPig(helper, new BlockPos(1, 2, 4));
		ServerPlayer shooter = GunsGameTests.shooter(helper, "tabby_revolver", 6, pig, GameType.SURVIVAL);
		ItemStack other = loaded("tabby_revolver", 6);
		shooter.setItemInHand(InteractionHand.OFF_HAND, other);
		helper.assertTrue(GunShots.fire(shooter), "The main hand's Tabby Revolver did not fire");
		helper.assertTrue(GunShots.fire(shooter, InteractionHand.OFF_HAND), "The other hand's Tabby Revolver did not fire");
		float expected = 2 * JugcraftGuns.SPECS.get("tabby_revolver").damage();
		helper.assertTrue(Math.abs(200.0F - pig.getHealth() - expected) < 1.0E-3F,
				"Two shots took " + (200.0F - pig.getHealth()) + " from the pig, not " + expected);
		helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 5 && GunItem.loaded(other) == 5,
				"Each Tabby Revolver did not spend a round of its own: " + GunItem.loaded(shooter.getMainHandItem()) + " and " + GunItem.loaded(other));
		helper.succeed();
	}

	private static ItemStack loaded(String gun, int rounds) {
		ItemStack stack = new ItemStack(JugcraftGuns.GUNS.get(gun));
		GunItem.setLoaded(stack, rounds);
		return stack;
	}
}
