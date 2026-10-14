package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.guns.GunItem;
import io.github.jimbozoomer.jugcraft.guns.GunShots;
import io.github.jimbozoomer.jugcraft.guns.GunSpec;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;

/**
 * In-game tests for the End guns (slice 12A, docs/features/guns.md), through the server's entry points as
 * {@link GunsGameTests} does: the Chitin SMG, the Hermit Rifle and the Borer are automatics loaded a magazine at a time;
 * each lands its shot on what it aims at and makes it float. Shooters face south (+z).
 */
public class EndGunsGameTests {
	/** The End guns, in the order the creative tab shows them. */
	static final List<String> GUNS = List.of("chitin_smg", "hermit_rifle", "borer");
	/** The scopes and the Laser Sight. */
	static final List<String> OPTICS = List.of("long_scope", "medium_scope", "reflex_sight", "laser_sight");

	/**
	 * Each is registered with its numbers and its recipe loads, and fires bullets that make what they hurt float; no other
	 * gun's do, and none of them sets what it hits alight. The Chitin SMG fires light rounds for as long as the trigger is
	 * held, from a magazine of twenty-four; the Hermit Rifle rifle rounds from twenty; the Borer rifle rounds from forty.
	 * The Chitin SMG takes the scopes and the Laser Sight; the Hermit Rifle the stocks too; the Borer, carried at the hip,
	 * nothing.
	 */
	@GameTest
	public void endGunsAreRegistered(GameTestHelper helper) {
		for (String name : GUNS) {
			GunSpec spec = JugcraftGuns.SPECS.get(name);
			GunItem gun = JugcraftGuns.GUNS.get(name);
			helper.assertTrue(spec != null && gun != null && gun.spec() == spec, name + " is not registered with its numbers");
			helper.assertTrue(JugcraftGuns.shot(gun).equals("bullet") && JugcraftGuns.levitates(gun) && !JugcraftGuns.ignites(gun),
					name + " does not fire bullets that make what they hit float, and only that");
			helper.assertTrue(spec.auto() && spec.pellets() == 1 && !spec.byShell() && !JugcraftGuns.ONE_HANDED.contains(name),
					name + " is not an automatic held in both hands, one bullet a shot, loaded a magazine at a time");
			helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(name))).isPresent(),
					"The " + name + " recipe does not load");
		}
		helper.assertTrue(JugcraftGuns.LEVITATING.equals(Set.copyOf(GUNS)), "Guns besides the End guns make what they hit float: "
				+ JugcraftGuns.LEVITATING);
		helper.assertTrue(Collections.disjoint(JugcraftGuns.LEVITATING, JugcraftGuns.INCENDIARY),
				"A gun both sets what it hits alight and makes it float");
		GunSpec chitin = JugcraftGuns.SPECS.get("chitin_smg");
		GunSpec hermit = JugcraftGuns.SPECS.get("hermit_rifle");
		GunSpec borer = JugcraftGuns.SPECS.get("borer");
		helper.assertTrue(chitin.ammo().equals("light_round") && chitin.capacity() == 24,
				"The Chitin SMG is not a machine gun of twenty-four light rounds");
		helper.assertTrue(hermit.ammo().equals("rifle_round") && hermit.capacity() == 20, "The Hermit Rifle is not a rifle of twenty rifle rounds");
		helper.assertTrue(borer.ammo().equals("rifle_round") && borer.capacity() == 40, "The Borer is not a machine gun of forty rifle rounds");
		helper.assertTrue(JugcraftGuns.ACCEPTS.get("chitin_smg").equals(OPTICS), "The Chitin SMG takes " + JugcraftGuns.ACCEPTS.get("chitin_smg"));
		List<String> hermitTakes = new ArrayList<>(List.of("light_stock", "weighted_stock", "wooden_stock"));
		hermitTakes.addAll(OPTICS);
		helper.assertTrue(JugcraftGuns.ACCEPTS.get("hermit_rifle").equals(hermitTakes),
				"The Hermit Rifle takes " + JugcraftGuns.ACCEPTS.get("hermit_rifle"));
		helper.assertTrue(JugcraftGuns.ACCEPTS.getOrDefault("borer", List.of()).isEmpty(), "The Borer takes " + JugcraftGuns.ACCEPTS.get("borer"));
		helper.succeed();
	}

	/**
	 * Side by side, each fires its last round at a pig three blocks off and its one shot takes one round's damage; each
	 * then loads from the inventory, nothing in halfway through its reload and full as it ends, two rounds left in the
	 * inventory.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 200)
	public void endGunsFireAndLoad(GameTestHelper helper) {
		NetherGunsGameTests.fireAndLoad(helper, GUNS);
	}

	/**
	 * Each End gun's shot makes its pig float, and it rises off the floor; an Undertone Rifle's shot (slice 10D) leaves its
	 * pig standing. When the spell ends each pig comes down again and stands on the floor, unhurt by so short a fall.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 120)
	public void endGunsMakeWhatTheyHitFloat(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		List<String> fired = new ArrayList<>(GUNS);
		fired.add("undertone_rifle");
		Mob[] pigs = new Mob[fired.size()];
		double[] floor = new double[fired.size()];
		float[] afterShot = new float[fired.size()];
		for (int i = 0; i < fired.size(); i++) {
			String name = fired.get(i);
			pigs[i] = NetherGunsGameTests.sturdyPig(helper, new BlockPos(1 + 4 * i, 2, 4));
			floor[i] = pigs[i].getY();
			ServerPlayer shooter = NetherGunsGameTests.shooter(helper, name, new BlockPos(1 + 4 * i, 2, 1), pigs[i]);
			helper.assertTrue(GunShots.fire(shooter), "The " + name + " did not fire");
			afterShot[i] = pigs[i].getHealth();
			helper.assertTrue(afterShot[i] < 200.0F, "The " + name + "'s shot missed its pig");
			boolean end = JugcraftGuns.LEVITATING.contains(name);
			helper.assertTrue(pigs[i].hasEffect(MobEffects.LEVITATION) == end,
					"The " + name + "'s shot " + (end ? "did not make" : "made") + " its pig float");
		}
		// Floating at the first level, a creature rises about half a block in its first three quarters of a second.
		helper.runAfterDelay(15, () -> {
			for (int i = 0; i < fired.size(); i++) {
				boolean end = JugcraftGuns.LEVITATING.contains(fired.get(i));
				double risen = pigs[i].getY() - floor[i];
				helper.assertTrue(end ? risen > 0.2 : Math.abs(risen) < 0.01, "The " + fired.get(i) + "'s pig rose " + risen + " blocks");
			}
		});
		helper.runAfterDelay(GunShots.LIFT_TICKS + 60, () -> {
			for (int i = 0; i < fired.size(); i++) {
				String name = fired.get(i);
				helper.assertFalse(pigs[i].hasEffect(MobEffects.LEVITATION), "The " + name + "'s pig was still floating");
				helper.assertTrue(pigs[i].onGround() && Math.abs(pigs[i].getY() - floor[i]) < 0.01,
						"The " + name + "'s pig had not come down to the floor: " + (pigs[i].getY() - floor[i]) + " blocks up");
				helper.assertTrue(Math.abs(pigs[i].getHealth() - afterShot[i]) < 1.0E-3F,
						"The " + name + "'s pig went from " + afterShot[i] + " to " + pigs[i].getHealth() + " after the shot");
			}
			helper.succeed();
		});
	}
}
