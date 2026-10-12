package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.guns.GunItem;
import io.github.jimbozoomer.jugcraft.guns.GunShots;
import io.github.jimbozoomer.jugcraft.guns.GunSpec;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the Nether guns (slices 11A and 11B, docs/features/guns.md), through the server's entry points as
 * {@link GunsGameTests} does: the Tusker and the Crackling are loaded a shell at a time, the Cinder Repeater and the
 * Goldbristle Carbine a magazine at a time, the Ashfall Pistol a tank at a time, the Bastion Rifle a round at a time;
 * each lands its shot on what it aims at and sets it alight. Shooters face south (+z).
 */
public class NetherGunsGameTests {
	/** Slice 11A's Nether guns, in the order the creative tab shows them. */
	static final List<String> FIRST = List.of("tusker", "cinder_repeater", "bastion_rifle");
	/** Slice 11B's, after them. */
	static final List<String> SECOND = List.of("ashfall_pistol", "goldbristle_carbine", "crackling");
	/** The Nether guns. */
	static final List<String> GUNS = List.of("tusker", "cinder_repeater", "bastion_rifle", "ashfall_pistol", "goldbristle_carbine",
			"crackling");
	/** The stocks, the grips, the barrels' attachments, the bayonets, the scopes and the Laser Sight. */
	static final List<String> FULL = List.of("light_stock", "weighted_stock", "wooden_stock", "light_grip", "tactical_grip", "silencer",
			"baffled_silencer", "muzzle_brake", "extended_barrel", "iron_bayonet", "steel_bayonet", "diamond_bayonet", "netherite_bayonet",
			"long_scope", "medium_scope", "reflex_sight", "laser_sight");

	/**
	 * Each is registered with its numbers and its recipe loads, and fires bullets that set what they hurt alight; no other
	 * gun's do. The Tusker fires eight pellets from six buckshot shells loaded a shell at a time; the Cinder Repeater fires
	 * light rounds for as long as the trigger is held, from a magazine of twenty; the Bastion Rifle fires one rifle round a
	 * pull from five loaded a round at a time. The Ashfall Pistol, held in one hand, fires one light round a pull from a
	 * tank of eight; the Goldbristle Carbine one rifle round a pull from a magazine of twelve; the Crackling six pellets
	 * from five buckshot shells loaded a shell at a time. The Tusker, the Cinder Repeater, the Bastion Rifle and the
	 * Goldbristle Carbine take the stocks, the grips, the bayonets, the barrels' attachments, the scopes and the Laser
	 * Sight, and the Cinder Repeater and the Goldbristle Carbine the magazines too. The Ashfall Pistol takes the stocks, the
	 * scopes and the Laser Sight only; the Crackling the stocks, the grips and the bayonets only, the barrels' attachments
	 * the owner made for it being too short for its barrel.
	 */
	@GameTest
	public void netherGunsAreRegistered(GameTestHelper helper) {
		for (String name : GUNS) {
			GunSpec spec = JugcraftGuns.SPECS.get(name);
			GunItem gun = JugcraftGuns.GUNS.get(name);
			helper.assertTrue(spec != null && gun != null && gun.spec() == spec, name + " is not registered with its numbers");
			helper.assertTrue(JugcraftGuns.shot(gun).equals("bullet") && JugcraftGuns.ignites(gun),
					name + " does not fire bullets that set what they hit alight");
			helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(name))).isPresent(),
					"The " + name + " recipe does not load");
		}
		for (String name : List.of("tusker", "cinder_repeater", "bastion_rifle", "goldbristle_carbine")) {
			helper.assertTrue(JugcraftGuns.ACCEPTS.getOrDefault(name, List.of()).containsAll(FULL),
					"The " + name + " does not take the stocks, grips, barrels, bayonets and scopes");
		}
		helper.assertTrue(JugcraftGuns.INCENDIARY.equals(Set.copyOf(GUNS)), "Guns besides the Nether guns set what they hit alight: "
				+ JugcraftGuns.INCENDIARY);
		GunSpec tusker = JugcraftGuns.SPECS.get("tusker");
		GunSpec cinder = JugcraftGuns.SPECS.get("cinder_repeater");
		GunSpec bastion = JugcraftGuns.SPECS.get("bastion_rifle");
		helper.assertTrue(tusker.ammo().equals("buckshot_shell") && tusker.pellets() == 8 && tusker.capacity() == 6 && tusker.byShell()
				&& !tusker.auto(), "The Tusker is not a shotgun of eight pellets and six shells loaded a shell at a time");
		helper.assertTrue(cinder.ammo().equals("light_round") && cinder.auto() && cinder.capacity() == 20 && !cinder.byShell(),
				"The Cinder Repeater is not an automatic of twenty light rounds");
		helper.assertTrue(bastion.ammo().equals("rifle_round") && bastion.pellets() == 1 && bastion.capacity() == 5 && bastion.byShell()
				&& !bastion.auto(), "The Bastion Rifle is not a rifle of five rounds, one a pull, loaded a round at a time");
		GunSpec ashfall = JugcraftGuns.SPECS.get("ashfall_pistol");
		GunSpec goldbristle = JugcraftGuns.SPECS.get("goldbristle_carbine");
		GunSpec crackling = JugcraftGuns.SPECS.get("crackling");
		helper.assertTrue(ashfall.ammo().equals("light_round") && ashfall.pellets() == 1 && ashfall.capacity() == 8 && !ashfall.byShell()
				&& !ashfall.auto() && JugcraftGuns.ONE_HANDED.contains("ashfall_pistol"),
				"The Ashfall Pistol is not a pistol held in one hand, of eight light rounds, one a pull");
		helper.assertTrue(goldbristle.ammo().equals("rifle_round") && goldbristle.pellets() == 1 && goldbristle.capacity() == 12
				&& !goldbristle.byShell() && !goldbristle.auto() && !JugcraftGuns.ONE_HANDED.contains("goldbristle_carbine"),
				"The Goldbristle Carbine is not a carbine of twelve rifle rounds, one a pull");
		helper.assertTrue(crackling.ammo().equals("buckshot_shell") && crackling.pellets() == 6 && crackling.capacity() == 5
				&& crackling.byShell() && !crackling.auto(), "The Crackling is not a shotgun of six pellets and five shells loaded a shell at a time");
		for (String name : List.of("cinder_repeater", "goldbristle_carbine")) {
			helper.assertTrue(JugcraftGuns.ACCEPTS.get(name).containsAll(List.of("extended_magazine", "speed_magazine")),
					"The " + name + " does not take the magazines");
		}
		helper.assertTrue(JugcraftGuns.ACCEPTS.get("ashfall_pistol").equals(List.of("light_stock", "weighted_stock", "wooden_stock",
				"long_scope", "medium_scope", "reflex_sight", "laser_sight")), "The Ashfall Pistol takes " + JugcraftGuns.ACCEPTS.get("ashfall_pistol"));
		helper.assertTrue(JugcraftGuns.ACCEPTS.get("crackling").equals(List.of("light_stock", "weighted_stock", "wooden_stock", "light_grip",
				"iron_bayonet", "steel_bayonet", "diamond_bayonet", "netherite_bayonet", "tactical_grip")),
				"The Crackling takes " + JugcraftGuns.ACCEPTS.get("crackling"));
		helper.succeed();
	}

	/**
	 * Side by side, each of slice 11A's guns fires its last round at a pig three blocks off: the Tusker's pellets land
	 * together, the Cinder Repeater's and the Bastion Rifle's one shot takes one round's damage. Each then loads from the
	 * inventory: the Cinder Repeater has nothing in halfway through its reload and is full as it ends; the Tusker and the
	 * Bastion Rifle have one round in after the first round's time and all of them after the reload. Two rounds are left
	 * in the inventory each time.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 200)
	public void netherGunsFireAndLoad(GameTestHelper helper) {
		fireAndLoad(helper, FIRST);
	}

	/**
	 * The same for slice 11B's guns: the Crackling's pellets land together, the Ashfall Pistol's and the Goldbristle
	 * Carbine's one shot takes one round's damage; the Ashfall Pistol and the Goldbristle Carbine have nothing in halfway
	 * through their reloads and are full as they end, the Crackling one shell in after the first shell's time and all five
	 * after its reload.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 200)
	public void secondNetherGunsFireAndLoad(GameTestHelper helper) {
		fireAndLoad(helper, SECOND);
	}

	/**
	 * Each of slice 11A's guns sets the pig it hits alight, and the fire goes on hurting it after the shot; an Undertone
	 * Rifle's shot (slice 10D) leaves its pig unlit and hurts it no more. No block catches fire, though the pellets that
	 * miss strike the stone behind.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 100)
	public void netherGunsSetWhatTheyHitAlight(GameTestHelper helper) {
		setAlight(helper, FIRST);
	}

	/** The same for slice 11B's guns. */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 100)
	public void secondNetherGunsSetWhatTheyHitAlight(GameTestHelper helper) {
		setAlight(helper, SECOND);
	}

	/** Each gun fires its last round at its own pig, then loads from the inventory in its own time. */
	private static void fireAndLoad(GameTestHelper helper, List<String> guns) {
		GunsGameTests.floor(helper);
		int longest = 0;
		for (int i = 0; i < guns.size(); i++) {
			String name = guns.get(i);
			GunSpec spec = JugcraftGuns.SPECS.get(name);
			Item rounds = JugcraftGuns.ROUNDS.get(spec.ammo());
			Mob pig = sturdyPig(helper, new BlockPos(1 + 4 * i, 2, 4));
			ServerPlayer shooter = shooter(helper, name, new BlockPos(1 + 4 * i, 2, 1), pig);
			helper.assertTrue(GunShots.fire(shooter), "The " + name + " did not fire");
			float taken = 200.0F - pig.getHealth();
			if (spec.pellets() > 1) {
				helper.assertTrue(taken >= spec.damage() * (spec.pellets() - 2),
						"At close range the " + name + "'s pellets took only " + taken + " (one pellet is " + spec.damage() + ")");
			} else {
				helper.assertTrue(Math.abs(taken - spec.damage()) < 1.0E-3F, "The " + name + "'s shot took " + taken + ", not " + spec.damage());
			}
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 0, "The " + name + " did not spend its round");
			shooter.getInventory().add(new ItemStack(rounds, spec.capacity() + 2));
			helper.assertTrue(GunShots.reload(shooter), "The " + name + "'s reload did not start");
			int ticks = spec.reloadTicks(spec.capacity());
			if (spec.byShell()) {
				helper.runAfterDelay(spec.shellStart() + spec.shellEach() + 1, () -> helper.assertTrue(
						GunItem.loaded(shooter.getMainHandItem()) == 1, "The " + name + " had not one round in after its first round's time"));
			} else {
				helper.runAfterDelay(ticks / 2, () -> helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 0,
						"The " + name + " had a round in halfway through its reload"));
			}
			helper.runAfterDelay(ticks + 2, () -> {
				helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == spec.capacity(), "The " + name + " holds "
						+ GunItem.loaded(shooter.getMainHandItem()) + " rounds after its reload, not " + spec.capacity());
				helper.assertTrue(GunShots.count(shooter.getInventory(), rounds) == 2,
						"The " + name + "'s reload did not take its rounds from the inventory");
				helper.assertFalse(GunShots.reloading(shooter), "The " + name + " was still reloading, full");
			});
			longest = Math.max(longest, ticks + 2);
		}
		helper.runAfterDelay(longest + 2, helper::succeed);
	}

	/**
	 * Each gun fires at its own pig, and an Undertone Rifle beside them at another: each Nether gun's pig is set alight and
	 * hurt again by the fire within two seconds; the Undertone Rifle's is neither. No block catches fire.
	 */
	private static void setAlight(GameTestHelper helper, List<String> guns) {
		GunsGameTests.floor(helper);
		List<String> fired = new ArrayList<>(guns);
		fired.add("undertone_rifle");
		float[] afterShot = new float[fired.size()];
		Mob[] pigs = new Mob[fired.size()];
		for (int i = 0; i < fired.size(); i++) {
			String name = fired.get(i);
			pigs[i] = sturdyPig(helper, new BlockPos(1 + 4 * i, 2, 4));
			ServerPlayer shooter = shooter(helper, name, new BlockPos(1 + 4 * i, 2, 1), pigs[i]);
			helper.assertTrue(GunShots.fire(shooter), "The " + name + " did not fire");
			afterShot[i] = pigs[i].getHealth();
			helper.assertTrue(afterShot[i] < 200.0F, "The " + name + "'s shot missed its pig");
			boolean nether = JugcraftGuns.INCENDIARY.contains(name);
			helper.assertTrue((pigs[i].getRemainingFireTicks() > 0) == nether,
					"The " + name + "'s shot " + (nether ? "did not set" : "set") + " its pig alight");
		}
		// Fire hurts what burns once a second; two seconds on, each burning pig has been hurt again.
		helper.runAfterDelay(45, () -> {
			for (int i = 0; i < fired.size(); i++) {
				boolean nether = JugcraftGuns.INCENDIARY.contains(fired.get(i));
				helper.assertTrue((pigs[i].getHealth() < afterShot[i]) == nether, "The " + fired.get(i) + "'s pig went from "
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

	/** A pig that stays put, with 200 health so that every shot and every second of fire shows. */
	private static Mob sturdyPig(GameTestHelper helper, BlockPos at) {
		Mob pig = GunsGameTests.pig(helper, at);
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
		pig.setHealth(200.0F);
		return pig;
	}

	/** A shooter standing at {@code at}, facing south with its look on the target's middle, its last round loaded. */
	private static ServerPlayer shooter(GameTestHelper helper, String gun, BlockPos at, Mob target) {
		ServerPlayer player = GunsGameTests.shooter(helper, gun, 1, target, GameType.SURVIVAL);
		BlockPos pos = helper.absolutePos(at);
		player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		Vec3 aim = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
		player.setXRot((float) Math.toDegrees(Math.atan2(-aim.y, Math.sqrt(aim.x * aim.x + aim.z * aim.z))));
		return player;
	}
}
