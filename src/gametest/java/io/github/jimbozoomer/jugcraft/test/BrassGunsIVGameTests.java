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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * In-game tests for slice 14D's guns (docs/features/guns.md), three more of the owner's treated-brass guns, through the
 * server's entry points as {@link GunsGameTests} does: the Quarrel Carbine, a repeating crossbow-gun whose bow throws
 * each round; the Hinterland Rifle, a two-pronged rifle of molten stone whose shots set what they hit alight; and the
 * Cloudburst Gun, a drum-fed twin-barrelled automatic. All three are held in both hands. Shooters face south (+z).
 */
public class BrassGunsIVGameTests {
	/** The slice's guns, in the order the creative tab shows them. */
	static final List<String> GUNS = List.of("quarrel_carbine", "hinterland_rifle", "cloudburst_gun");

	/**
	 * Each is registered with its numbers and its recipe loads, firing rifle rounds from a magazine, held in both hands.
	 * The Quarrel Carbine fires a pull at a time, eight a magazine, and takes both magazines, the three stocks and the
	 * four sights. The Hinterland Rifle fires a pull at a time, ten a magazine; its shots alone of the slice set what they
	 * hit alight, and it takes the Medium Scope and the Reflex Sight and nothing else. The Cloudburst Gun fires as long
	 * as the trigger is held, thirty a drum, and takes no attachment.
	 */
	@GameTest
	public void brassGunsIVAreRegistered(GameTestHelper helper) {
		for (String name : GUNS) {
			GunSpec spec = JugcraftGuns.SPECS.get(name);
			GunItem gun = JugcraftGuns.GUNS.get(name);
			helper.assertTrue(spec != null && gun != null && gun.spec() == spec, name + " is not registered with its numbers");
			helper.assertTrue(JugcraftGuns.ignites(gun) == name.equals("hinterland_rifle"),
					name + (JugcraftGuns.ignites(gun) ? " sets" : " does not set") + " what it hits alight");
			helper.assertTrue(!JugcraftGuns.levitates(gun), name + " lifts what it hits");
			helper.assertTrue(JugcraftGuns.shot(gun).equals("bullet") && !spec.byShell() && spec.pellets() == 1
					&& spec.ammo().equals("rifle_round"), name + " does not fire rifle rounds from a magazine");
			helper.assertTrue(!JugcraftGuns.ONE_HANDED.contains(name) && !JugcraftGuns.CARBINES.contains(name),
					name + " is not held in both hands");
			helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(name))).isPresent(),
					"The " + name + " recipe does not load");
		}
		GunSpec quarrel = JugcraftGuns.SPECS.get("quarrel_carbine");
		GunSpec hinterland = JugcraftGuns.SPECS.get("hinterland_rifle");
		GunSpec cloudburst = JugcraftGuns.SPECS.get("cloudburst_gun");
		helper.assertTrue(!quarrel.auto() && quarrel.capacity() == 8, "The Quarrel Carbine is not a rifle of eight rounds, a pull a shot");
		helper.assertTrue(!hinterland.auto() && hinterland.capacity() == 10, "The Hinterland Rifle is not a rifle of ten rounds, a pull a shot");
		helper.assertTrue(cloudburst.auto() && cloudburst.capacity() == 30, "The Cloudburst Gun is not an automatic of thirty rounds");

		List<String> quarrelTakes = JugcraftGuns.ACCEPTS.get("quarrel_carbine");
		helper.assertTrue(quarrelTakes.containsAll(List.of("extended_magazine", "speed_magazine"))
				&& quarrelTakes.containsAll(BrassSidearmsGameTests.STOCKS) && quarrelTakes.containsAll(BrassSidearmsGameTests.SIGHTS)
				&& quarrelTakes.size() == 9, "The Quarrel Carbine takes " + quarrelTakes);
		List<String> hinterlandTakes = JugcraftGuns.ACCEPTS.get("hinterland_rifle");
		helper.assertTrue(hinterlandTakes.equals(List.of("medium_scope", "reflex_sight")), "The Hinterland Rifle takes " + hinterlandTakes);
		helper.assertTrue(!JugcraftGuns.ACCEPTS.containsKey("cloudburst_gun"), "The Cloudburst Gun takes an attachment");
		helper.succeed();
	}

	/**
	 * A Hinterland Rifle and a Quarrel Carbine each fire at their own pig: the Hinterland Rifle's pig is set alight and
	 * hurt again by the fire within two seconds; the Quarrel Carbine's is neither. No block catches fire.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 100)
	public void hinterlandRifleSetsWhatItHitsAlight(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		List<String> fired = List.of("hinterland_rifle", "quarrel_carbine");
		float[] afterShot = new float[fired.size()];
		Mob[] pigs = new Mob[fired.size()];
		for (int i = 0; i < fired.size(); i++) {
			String name = fired.get(i);
			pigs[i] = NetherGunsGameTests.sturdyPig(helper, new BlockPos(1 + 4 * i, 2, 4));
			ServerPlayer shooter = NetherGunsGameTests.shooter(helper, name, new BlockPos(1 + 4 * i, 2, 1), pigs[i]);
			helper.assertTrue(GunShots.fire(shooter), "The " + name + " did not fire");
			afterShot[i] = pigs[i].getHealth();
			helper.assertTrue(afterShot[i] < 200.0F, "The " + name + "'s shot missed its pig");
			boolean burns = i == 0;
			helper.assertTrue((pigs[i].getRemainingFireTicks() > 0) == burns,
					"The " + name + "'s shot " + (burns ? "did not set" : "set") + " its pig alight");
		}
		// Fire hurts what burns once a second; two seconds on, the burning pig has been hurt again.
		helper.runAfterDelay(45, () -> {
			for (int i = 0; i < fired.size(); i++) {
				helper.assertTrue((pigs[i].getHealth() < afterShot[i]) == (i == 0), "The " + fired.get(i) + "'s pig went from "
						+ afterShot[i] + " to " + pigs[i].getHealth() + " after the shot");
			}
			for (int x = 0; x <= 15; x++) {
				for (int y = 1; y <= 5; y++) {
					for (int z = 0; z <= 15; z++) {
						helper.assertBlockNotPresent(Blocks.FIRE, new BlockPos(x, y, z));
					}
				}
			}
			helper.succeed();
		});
	}

	/**
	 * An empty Cloudburst Gun with forty rifle rounds in the inventory changes its drum: nothing is in before its
	 * reload's time, thirty rounds after it, and ten are left. A shot then spends one round and takes its damage from the
	 * pig.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 140)
	public void cloudburstGunChangesItsDrum(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = NetherGunsGameTests.sturdyPig(helper, new BlockPos(1, 2, 4));
		GunSpec spec = JugcraftGuns.SPECS.get("cloudburst_gun");
		ServerPlayer shooter = GunsGameTests.shooter(helper, "cloudburst_gun", 0, pig, GameType.SURVIVAL);
		shooter.getInventory().setItem(1, new ItemStack(JugcraftGuns.ROUNDS.get("rifle_round"), 40));
		helper.assertTrue(GunShots.reload(shooter), "The empty Cloudburst Gun did not start to reload");
		helper.runAfterDelay(spec.reload() - 3, () -> helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 0,
				"The Cloudburst Gun's drum was in before its reload's time"));
		helper.runAfterDelay(spec.reload() + 2, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 30, "The Cloudburst Gun loaded "
					+ GunItem.loaded(shooter.getMainHandItem()) + " rounds, not thirty");
			helper.assertTrue(GunShots.count(shooter.getInventory(), JugcraftGuns.ROUNDS.get("rifle_round")) == 10,
					"The reload did not leave ten rounds in the inventory");
			helper.assertTrue(GunShots.fire(shooter), "The loaded Cloudburst Gun did not fire");
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 29, "The shot did not spend one round");
			helper.assertTrue(Math.abs(200.0F - pig.getHealth() - spec.damage()) < 1.0E-3F,
					"The shot took " + (200.0F - pig.getHealth()) + ", not " + spec.damage());
			helper.succeed();
		});
	}
}
