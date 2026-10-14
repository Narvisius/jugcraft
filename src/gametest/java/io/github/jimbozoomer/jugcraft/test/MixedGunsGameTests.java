package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.guns.GunItem;
import io.github.jimbozoomer.jugcraft.guns.GunShots;
import io.github.jimbozoomer.jugcraft.guns.GunSpec;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import java.util.ArrayList;
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
 * In-game tests for slice 12C's mixed set (docs/features/guns.md), through the server's entry points as
 * {@link GunsGameTests} does: the Tuppence Pistol and the Goshawk Revolver, each held in one hand and loaded a round at a
 * time, and the Lament Rifle, a bolt-action rifle loaded a magazine at a time; each lands its shot on what it aims at.
 * Shooters face south (+z).
 */
public class MixedGunsGameTests {
	/** The slice's guns, in the order the creative tab shows them. */
	static final List<String> GUNS = List.of("tuppence_pistol", "goshawk_revolver", "lament_rifle");
	/** The stocks. */
	static final List<String> STOCKS = List.of("light_stock", "weighted_stock", "wooden_stock");
	/** The scopes and the Laser Sight. */
	static final List<String> OPTICS = List.of("long_scope", "medium_scope", "reflex_sight", "laser_sight");

	/**
	 * Each is registered with its numbers and its recipe loads, and fires plain bullets, one a shot. The Tuppence Pistol
	 * fires light rounds from a tube of eight and the Goshawk Revolver rifle rounds from a cylinder of six, each held in
	 * one hand (so the two are two guns at once) and loaded a round at a time; the Lament Rifle, held in both, rifle rounds
	 * from a magazine of five, loaded a magazine at a time. The Tuppence Pistol takes the stocks, the scopes and the Laser
	 * Sight; the Lament Rifle the stocks, the grips, the bayonets, the scopes and the Laser Sight; the Goshawk Revolver,
	 * for which the owner made no parts, nothing.
	 */
	@GameTest
	public void mixedGunsAreRegistered(GameTestHelper helper) {
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
		GunSpec tuppence = JugcraftGuns.SPECS.get("tuppence_pistol");
		GunSpec goshawk = JugcraftGuns.SPECS.get("goshawk_revolver");
		GunSpec lament = JugcraftGuns.SPECS.get("lament_rifle");
		helper.assertTrue(tuppence.ammo().equals("light_round") && tuppence.capacity() == 8 && tuppence.byShell()
				&& JugcraftGuns.ONE_HANDED.contains("tuppence_pistol"),
				"The Tuppence Pistol is not a pistol held in one hand, of eight light rounds loaded one at a time");
		helper.assertTrue(goshawk.ammo().equals("rifle_round") && goshawk.capacity() == 6 && goshawk.byShell()
				&& JugcraftGuns.ONE_HANDED.contains("goshawk_revolver"),
				"The Goshawk Revolver is not a revolver held in one hand, of six rifle rounds loaded one at a time");
		helper.assertTrue(lament.ammo().equals("rifle_round") && lament.capacity() == 5 && !lament.byShell()
				&& !JugcraftGuns.ONE_HANDED.contains("lament_rifle"),
				"The Lament Rifle is not a rifle of five rifle rounds, loaded a magazine at a time");
		helper.assertTrue(lament.damage() > goshawk.damage() && lament.range() > goshawk.range(),
				"The Lament Rifle does not hit harder and reach further than the Goshawk Revolver");
		List<String> tuppenceTakes = new ArrayList<>(STOCKS);
		tuppenceTakes.addAll(OPTICS);
		helper.assertTrue(JugcraftGuns.ACCEPTS.get("tuppence_pistol").equals(tuppenceTakes),
				"The Tuppence Pistol takes " + JugcraftGuns.ACCEPTS.get("tuppence_pistol"));
		helper.assertTrue(JugcraftGuns.ACCEPTS.get("lament_rifle").containsAll(STOCKS)
				&& JugcraftGuns.ACCEPTS.get("lament_rifle").containsAll(OPTICS)
				&& JugcraftGuns.ACCEPTS.get("lament_rifle").containsAll(List.of("light_grip", "vertical_grip", "iron_bayonet",
						"steel_bayonet", "diamond_bayonet", "netherite_bayonet")),
				"The Lament Rifle takes " + JugcraftGuns.ACCEPTS.get("lament_rifle"));
		helper.assertTrue(JugcraftGuns.ACCEPTS.getOrDefault("goshawk_revolver", List.of()).isEmpty(),
				"The Goshawk Revolver takes " + JugcraftGuns.ACCEPTS.get("goshawk_revolver"));
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemInHand(InteractionHand.MAIN_HAND, loaded("tuppence_pistol", 0));
		player.setItemInHand(InteractionHand.OFF_HAND, loaded("goshawk_revolver", 0));
		helper.assertTrue(GunItem.dual(player), "A Tuppence Pistol and a Goshawk Revolver are not two guns at once");
		helper.succeed();
	}

	/**
	 * Side by side, each fires its last round at a pig three blocks off and its one shot takes one round's damage; each
	 * then loads from the inventory in its own time, the two loaded a round at a time with one in after its first round's
	 * time, the rifle with nothing in halfway through its reload, and each full as it ends, two rounds left in the
	 * inventory.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 200)
	public void mixedGunsFireAndLoad(GameTestHelper helper) {
		NetherGunsGameTests.fireAndLoad(helper, GUNS);
	}

	/**
	 * A Tuppence Pistol in the main hand and a Goshawk Revolver in the other: each fires from its own hand and lands its
	 * shot, spending a round of its own.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 20)
	public void tuppenceAndGoshawkFireFromBothHands(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = NetherGunsGameTests.sturdyPig(helper, new BlockPos(1, 2, 4));
		ServerPlayer shooter = GunsGameTests.shooter(helper, "tuppence_pistol", 8, pig, GameType.SURVIVAL);
		ItemStack other = loaded("goshawk_revolver", 6);
		shooter.setItemInHand(InteractionHand.OFF_HAND, other);
		helper.assertTrue(GunShots.fire(shooter), "The main hand's Tuppence Pistol did not fire");
		helper.assertTrue(GunShots.fire(shooter, InteractionHand.OFF_HAND), "The other hand's Goshawk Revolver did not fire");
		float expected = JugcraftGuns.SPECS.get("tuppence_pistol").damage() + JugcraftGuns.SPECS.get("goshawk_revolver").damage();
		helper.assertTrue(Math.abs(200.0F - pig.getHealth() - expected) < 1.0E-3F,
				"Two shots took " + (200.0F - pig.getHealth()) + " from the pig, not " + expected);
		helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 7 && GunItem.loaded(other) == 5,
				"Each gun did not spend a round of its own: " + GunItem.loaded(shooter.getMainHandItem()) + " and " + GunItem.loaded(other));
		helper.succeed();
	}

	private static ItemStack loaded(String gun, int rounds) {
		ItemStack stack = new ItemStack(JugcraftGuns.GUNS.get(gun));
		GunItem.setLoaded(stack, rounds);
		return stack;
	}
}
