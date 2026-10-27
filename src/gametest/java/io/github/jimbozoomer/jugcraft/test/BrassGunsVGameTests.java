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
 * In-game tests for slice 14E's guns (docs/features/guns.md), two more of the owner's treated-brass guns, through the
 * server's entry points as {@link GunsGameTests} does: the Firedrake, a short flamethrower with two fuel tanks slung
 * under it; and the Harrow, four barrels before a drum, whose pellets set what they hit alight. Both are held in both
 * hands. Shooters face south (+z).
 */
public class BrassGunsVGameTests {
	/** The slice's guns, in the order the creative tab shows them. */
	static final List<String> GUNS = List.of("firedrake", "harrow");

	/**
	 * Each is registered with its numbers and its recipe loads, held in both hands. The Firedrake fires bursts of flame
	 * as long as the trigger is held, sixty-four bursts of blaze powder, its jet reaching less far than the Stoker's and
	 * wider, and takes no attachment. The Harrow fires a buckshot shell a pull from a can of eight, four pellets a shell
	 * that set what they hit alight, and takes the three stocks.
	 */
	@GameTest
	public void brassGunsVAreRegistered(GameTestHelper helper) {
		for (String name : GUNS) {
			GunSpec spec = JugcraftGuns.SPECS.get(name);
			GunItem gun = JugcraftGuns.GUNS.get(name);
			helper.assertTrue(spec != null && gun != null && gun.spec() == spec, name + " is not registered with its numbers");
			helper.assertTrue(!JugcraftGuns.levitates(gun), name + " lifts what it hits");
			helper.assertTrue(!spec.byShell(), name + " is loaded a shell at a time");
			helper.assertTrue(!JugcraftGuns.ONE_HANDED.contains(name) && !JugcraftGuns.CARBINES.contains(name),
					name + " is not held in both hands");
			helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(name))).isPresent(),
					"The " + name + " recipe does not load");
		}
		GunItem firedrake = JugcraftGuns.GUNS.get("firedrake");
		GunSpec drake = JugcraftGuns.SPECS.get("firedrake");
		GunSpec stoker = JugcraftGuns.SPECS.get("stoker");
		helper.assertTrue(JugcraftGuns.shot(firedrake).equals(JugcraftGuns.FLAME) && drake.ammo().equals("minecraft:blaze_powder")
				&& drake.auto() && drake.capacity() == 64 && !JugcraftGuns.ignites(firedrake),
				"The Firedrake is not a flamethrower of sixty-four bursts of blaze powder");
		helper.assertTrue(drake.range() < stoker.range() && drake.hipSpread() > stoker.hipSpread(),
				"The Firedrake's jet does not reach less far than the Stoker's and wider");
		helper.assertTrue(!JugcraftGuns.ACCEPTS.containsKey("firedrake"), "The Firedrake takes an attachment");
		GunItem harrow = JugcraftGuns.GUNS.get("harrow");
		GunSpec harrowSpec = JugcraftGuns.SPECS.get("harrow");
		helper.assertTrue(JugcraftGuns.shot(harrow).equals("bullet") && JugcraftGuns.ignites(harrow)
				&& harrowSpec.ammo().equals("buckshot_shell") && harrowSpec.pellets() == 4 && harrowSpec.capacity() == 8
				&& !harrowSpec.auto(), "The Harrow is not a shotgun of four pellets that set alight, eight shells, a pull a shot");
		helper.assertTrue(JugcraftGuns.ACCEPTS.get("harrow").equals(BrassSidearmsGameTests.STOCKS),
				"The Harrow takes " + JugcraftGuns.ACCEPTS.get("harrow"));
		helper.succeed();
	}

	/**
	 * A Firedrake burst singes the pig four blocks ahead by the burst's damage and sets it alight; a pig eight blocks ahead
	 * (within the Stoker's reach, beyond the Firedrake's) and one beside the shooter (out of the jet) are untouched; no
	 * block catches fire.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 40)
	public void firedrakeBurnsCloseAtHand(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob ahead = GunsGameTests.pig(helper, new BlockPos(1, 2, 5));
		Mob beyond = GunsGameTests.pig(helper, new BlockPos(1, 2, 9));
		Mob beside = GunsGameTests.pig(helper, new BlockPos(5, 2, 1));
		GunSpec spec = JugcraftGuns.SPECS.get("firedrake");
		ServerPlayer shooter = GunsGameTests.shooter(helper, "firedrake", spec.capacity(), ahead, GameType.SURVIVAL);
		float max = ahead.getHealth();
		helper.assertTrue(GunShots.fire(shooter), "The loaded Firedrake did not fire");
		helper.assertTrue(Math.abs(max - ahead.getHealth() - spec.damage()) < 1.0E-3F,
				"The burst took " + (max - ahead.getHealth()) + " from the pig ahead, not " + spec.damage());
		helper.assertTrue(ahead.getRemainingFireTicks() > 0, "The burst did not set the pig ahead alight");
		helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth() && beyond.getRemainingFireTicks() <= 0,
				"The burst reached the pig eight blocks ahead");
		helper.assertTrue(beside.getHealth() == beside.getMaxHealth() && beside.getRemainingFireTicks() <= 0,
				"The burst reached the pig beside the shooter");
		helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == spec.capacity() - 1, "The burst did not spend its fuel");
		helper.runAfterDelay(10, () -> {
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
	 * A Harrow fires its last shell at a pig three blocks ahead: at that range most of its four pellets land, the pig is
	 * set alight and the fire hurts it again within two seconds, and no block catches fire.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 100)
	public void harrowSetsWhatItHitsAlight(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = NetherGunsGameTests.sturdyPig(helper, new BlockPos(1, 2, 4));
		GunSpec spec = JugcraftGuns.SPECS.get("harrow");
		ServerPlayer shooter = NetherGunsGameTests.shooter(helper, "harrow", new BlockPos(1, 2, 1), pig);
		helper.assertTrue(GunShots.fire(shooter), "The Harrow did not fire");
		float afterShot = pig.getHealth();
		float taken = 200.0F - afterShot;
		helper.assertTrue(taken >= spec.damage() * (spec.pellets() - 2) && taken <= spec.damage() * spec.pellets() + 1.0E-3F,
				"At close range the Harrow's pellets took " + taken + " (one pellet is " + spec.damage() + ")");
		helper.assertTrue(pig.getRemainingFireTicks() > 0, "The Harrow's shot did not set its pig alight");
		helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 0, "The Harrow did not spend its shell");
		// Fire hurts what burns once a second; two seconds on, the pig has been hurt again.
		helper.runAfterDelay(45, () -> {
			helper.assertTrue(pig.getHealth() < afterShot, "The fire did not hurt the pig again: " + pig.getHealth());
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
	 * An empty Harrow with ten buckshot shells in the inventory changes its can: nothing is in before its reload's time,
	 * eight shells after it, and two are left.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 100)
	public void harrowChangesItsCan(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = NetherGunsGameTests.sturdyPig(helper, new BlockPos(1, 2, 4));
		GunSpec spec = JugcraftGuns.SPECS.get("harrow");
		ServerPlayer shooter = GunsGameTests.shooter(helper, "harrow", 0, pig, GameType.SURVIVAL);
		shooter.getInventory().setItem(1, new ItemStack(JugcraftGuns.ROUNDS.get("buckshot_shell"), 10));
		helper.assertTrue(GunShots.reload(shooter), "The empty Harrow did not start to reload");
		helper.runAfterDelay(spec.reload() - 3, () -> helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 0,
				"The Harrow's can was in before its reload's time"));
		helper.runAfterDelay(spec.reload() + 2, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 8, "The Harrow loaded "
					+ GunItem.loaded(shooter.getMainHandItem()) + " shells, not eight");
			helper.assertTrue(GunShots.count(shooter.getInventory(), JugcraftGuns.ROUNDS.get("buckshot_shell")) == 2,
					"The reload did not leave two shells in the inventory");
			helper.succeed();
		});
	}
}
