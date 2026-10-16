package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.PetroItems;
import io.github.jimbozoomer.jugcraft.guns.EnergyCellItem;
import io.github.jimbozoomer.jugcraft.guns.GunItem;
import io.github.jimbozoomer.jugcraft.guns.GunShots;
import io.github.jimbozoomer.jugcraft.guns.GunSpec;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import io.github.jimbozoomer.jugcraft.tools.Chargeable;
import io.github.jimbozoomer.jugcraft.weapons.GrenadeEntity;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * In-game tests for slice 13D's guns (docs/features/guns.md), through the server's entry points as
 * {@link GunsGameTests} does: the Fencepost Thumper, a revolving grenade launcher loaded a grenade at a time; the Parley
 * Revolver, held in one hand, or in both as a carbine with a stock fitted; and the Lantern Musket, an energy weapon whose
 * beam passes through every creature in its line. Shooters face south (+z).
 */
public class FrontierGunsIVGameTests {
	/** The slice's guns, in the order the creative tab shows them. */
	static final List<String> GUNS = List.of("fencepost_thumper", "parley_revolver", "lantern_musket");
	/** The bayonets. */
	static final List<String> BAYONETS = List.of("iron_bayonet", "steel_bayonet", "diamond_bayonet", "netherite_bayonet");
	/** The stocks. */
	static final List<String> STOCKS = List.of("light_stock", "weighted_stock", "wooden_stock");

	/**
	 * Each is registered with its numbers and its recipe loads, one pull a shot. The Fencepost Thumper lobs grenades, four
	 * loaded a grenade at a time, held in both hands, and takes the Light and Vertical Grips and the bayonets; the Parley
	 * Revolver fires rifle rounds, six loaded a round at a time, held in one hand but in both as a carbine, and takes the
	 * barrel attachments and the stocks; the Lantern Musket fires a beam on Energy Cells, three charges a magazine,
	 * reaching further than any energy weapon but the Solenoid Rifle, held in both hands, and takes nothing.
	 */
	@GameTest
	public void frontierGunsIVAreRegistered(GameTestHelper helper) {
		for (String name : GUNS) {
			GunSpec spec = JugcraftGuns.SPECS.get(name);
			GunItem gun = JugcraftGuns.GUNS.get(name);
			helper.assertTrue(spec != null && gun != null && gun.spec() == spec, name + " is not registered with its numbers");
			helper.assertTrue(!spec.auto() && !JugcraftGuns.ignites(gun) && !JugcraftGuns.levitates(gun), name + " is not one pull a shot");
			helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(name))).isPresent(),
					"The " + name + " recipe does not load");
		}
		GunItem thumper = JugcraftGuns.GUNS.get("fencepost_thumper");
		GunItem parley = JugcraftGuns.GUNS.get("parley_revolver");
		GunItem lantern = JugcraftGuns.GUNS.get("lantern_musket");
		helper.assertTrue(JugcraftGuns.shot(thumper).equals(JugcraftGuns.GRENADE) && JugcraftGuns.takesGrenades(thumper.spec())
				&& thumper.spec().capacity() == 4 && thumper.spec().byShell() && !JugcraftGuns.ONE_HANDED.contains("fencepost_thumper"),
				"The Fencepost Thumper is not a launcher of four grenades loaded one at a time, held in both hands");
		helper.assertTrue(JugcraftGuns.shot(parley).equals("bullet") && parley.spec().ammo().equals("rifle_round")
				&& parley.spec().capacity() == 6 && parley.spec().byShell() && JugcraftGuns.ONE_HANDED.contains("parley_revolver")
				&& JugcraftGuns.CARBINES.contains("parley_revolver"),
				"The Parley Revolver is not a one-handed revolver of six rifle rounds loaded one at a time that a stock makes a carbine");
		helper.assertTrue(JugcraftGuns.shot(lantern).equals(JugcraftGuns.BEAM) && JugcraftGuns.ammo(lantern.spec()) == JugcraftGuns.ENERGY_CELL
				&& JugcraftGuns.charge(lantern) > 0 && lantern.spec().capacity() == 3 && !lantern.spec().byShell()
				&& !JugcraftGuns.ONE_HANDED.contains("lantern_musket"),
				"The Lantern Musket is not a beam on Energy Cells, three charges a magazine, held in both hands");
		int solenoid = JugcraftGuns.SPECS.get("solenoid_rifle").range();
		for (String other : JugcraftGuns.CHARGE.keySet()) {
			helper.assertTrue(other.equals("solenoid_rifle") || other.equals("lantern_musket")
					|| JugcraftGuns.SPECS.get(other).range() < lantern.spec().range() && lantern.spec().range() < solenoid,
					"The " + other + " reaches as far as the Lantern Musket, or the Lantern Musket as far as the Solenoid Rifle");
		}
		List<String> thumperTakes = JugcraftGuns.ACCEPTS.get("fencepost_thumper");
		helper.assertTrue(thumperTakes.containsAll(BAYONETS) && thumperTakes.containsAll(List.of("light_grip", "vertical_grip"))
				&& thumperTakes.size() == 6, "The Fencepost Thumper takes " + thumperTakes);
		List<String> parleyTakes = JugcraftGuns.ACCEPTS.get("parley_revolver");
		helper.assertTrue(parleyTakes.containsAll(STOCKS) && parleyTakes.containsAll(JugcraftGuns.ACCEPTS.get("tabby_revolver"))
				&& parleyTakes.size() == 7, "The Parley Revolver takes " + parleyTakes);
		helper.assertTrue(!JugcraftGuns.ACCEPTS.containsKey("lantern_musket"), "The Lantern Musket takes an attachment");
		helper.succeed();
	}

	/**
	 * Without a stock the Parley Revolver is held in one hand: two of them are two guns at once, and the other hand's
	 * fires. With a stock fitted to one it is a carbine, held in both hands: the two are no longer two guns at once, the
	 * other hand's does not fire, and the stock's numbers apply.
	 */
	@GameTest
	public void parleyRevolverIsACarbineWithAStock(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ItemStack main = new ItemStack(JugcraftGuns.GUNS.get("parley_revolver"));
		ItemStack other = new ItemStack(JugcraftGuns.GUNS.get("parley_revolver"));
		GunItem.setLoaded(main, 6);
		GunItem.setLoaded(other, 6);
		player.setItemInHand(InteractionHand.MAIN_HAND, main);
		player.setItemInHand(InteractionHand.OFF_HAND, other);
		helper.assertTrue(GunItem.oneHanded(main) && !GunItem.carbine(main) && GunItem.dual(player),
				"Two Parley Revolvers without stocks are not two guns at once");
		helper.assertTrue(GunShots.fire(player, InteractionHand.OFF_HAND), "The other hand's Parley Revolver did not fire");
		GunSpec bare = GunItem.spec(main);
		GunItem.fit(main, "wooden_stock");
		helper.assertTrue(GunItem.carbine(main) && !GunItem.oneHanded(main) && !GunItem.dual(player),
				"A Parley Revolver with a stock is not a carbine held in both hands");
		helper.assertFalse(GunShots.fire(player, InteractionHand.OFF_HAND), "The other hand's gun fired beside a carbine");
		GunSpec stocked = GunItem.spec(main);
		helper.assertTrue(stocked.hipSpread() < bare.hipSpread() && stocked.aimSpread() < bare.aimSpread(),
				"The Wooden Stock does not steady the Parley Revolver");
		helper.assertFalse(GunItem.carbine(other), "A Parley Revolver without a stock is a carbine");
		GunItem.remove(main, "wooden_stock");
		helper.assertTrue(GunItem.oneHanded(main) && GunItem.dual(player), "Without its stock the Parley Revolver is not one-handed again");
		helper.succeed();
	}

	/**
	 * An empty Fencepost Thumper with three Grenades in the inventory loads one at a time: halfway through the second
	 * grenade's time only the first is in, and all three once its reload ends, none left. A shot then spends one and lobs
	 * a Grenade owned by the shooter, which bursts on the pig seven blocks off; the floor is whole.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 200)
	public void fencepostThumperLoadsAGrenadeAtATime(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = GunsGameTests.pig(helper, new BlockPos(1, 2, 8));
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
		pig.setHealth(200.0F);
		GunSpec spec = JugcraftGuns.SPECS.get("fencepost_thumper");
		ServerPlayer shooter = GunsGameTests.shooter(helper, "fencepost_thumper", 0, pig, GameType.SURVIVAL);
		shooter.getInventory().setItem(1, new ItemStack(PetroItems.GRENADE, 3));
		helper.assertTrue(GunShots.reload(shooter), "The empty Fencepost Thumper did not start to reload");
		helper.runAfterDelay(spec.shellStart() + spec.shellEach() + spec.shellEach() / 2, () -> helper.assertTrue(
				GunItem.loaded(shooter.getMainHandItem()) == 1, "Halfway through loading its second grenade the Fencepost Thumper held "
						+ GunItem.loaded(shooter.getMainHandItem()) + ", not one"));
		helper.runAfterDelay(spec.reloadTicks(3) + 3, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 3, "The Fencepost Thumper loaded "
					+ GunItem.loaded(shooter.getMainHandItem()) + " grenades from three, not three");
			helper.assertTrue(GunShots.count(shooter.getInventory(), PetroItems.GRENADE) == 0, "The reload left grenades in the inventory");
			helper.assertTrue(GunShots.fire(shooter), "The loaded Fencepost Thumper did not fire");
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 2, "The shot did not spend a grenade");
			AABB arena = new AABB(helper.absolutePos(new BlockPos(0, 0, 0))).expandTowards(16, 6, 16);
			helper.assertTrue(helper.getLevel().getEntitiesOfClass(GrenadeEntity.class, arena, grenade -> grenade.getOwner() == shooter).size() == 1,
					"The shot lobbed no grenade of the shooter's");
			helper.succeedWhen(() -> {
				helper.assertTrue(pig.getHealth() < 200.0F, "The grenade has not burst on the pig yet");
				for (int x = 0; x <= 15; x++) {
					for (int z = 0; z <= 15; z++) {
						helper.assertBlockPresent(Blocks.STONE, new BlockPos(x, 1, z));
					}
				}
			});
		});
	}

	/**
	 * A Lantern Musket shot passes through the two pigs in its line, each taking its damage, and stops at the wall: the
	 * pig behind it is untouched. The shot spends a charge. Then an empty Lantern Musket loads its three charges from a
	 * full cell once its reload ends, and the cell keeps the rest.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 200)
	public void lanternMusketDrivesThroughItsLine(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob near = raised(helper, new BlockPos(1, 2, 4));
		Mob behind = raised(helper, new BlockPos(1, 2, 7));
		Mob walled = raised(helper, new BlockPos(1, 2, 12));
		for (int x = 0; x <= 3; x++) {
			for (int y = 2; y <= 4; y++) {
				helper.setBlock(new BlockPos(x, y, 10), Blocks.STONE);
			}
		}
		GunSpec spec = JugcraftGuns.SPECS.get("lantern_musket");
		ServerPlayer shooter = GunsGameTests.shooter(helper, "lantern_musket", spec.capacity(), near, GameType.SURVIVAL);
		helper.assertTrue(GunShots.fire(shooter), "The loaded Lantern Musket did not fire");
		helper.assertTrue(took(near, spec.damage()), "The shot took " + lost(near) + " from the near pig, not " + spec.damage());
		helper.assertTrue(took(behind, spec.damage()), "The shot took " + lost(behind) + " from the pig behind it, not " + spec.damage());
		helper.assertTrue(lost(walled) == 0.0F, "The shot passed the wall");
		helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == spec.capacity() - 1, "The shot did not spend a charge");

		GunItem.setLoaded(shooter.getMainHandItem(), 0);
		ItemStack cell = new ItemStack(JugcraftGuns.ENERGY_CELL);
		Chargeable.setEnergy(cell, EnergyCellItem.CAPACITY);
		shooter.getInventory().setItem(1, cell);
		int charge = JugcraftGuns.charge(JugcraftGuns.GUNS.get("lantern_musket"));
		helper.assertTrue(GunShots.reload(shooter), "The empty Lantern Musket did not start to reload");
		helper.runAfterDelay(spec.reload() - 3, () -> helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 0,
				"The Lantern Musket's core was in before its reload's time"));
		helper.runAfterDelay(spec.reload() + 2, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == spec.capacity(), "The Lantern Musket loaded "
					+ GunItem.loaded(shooter.getMainHandItem()) + " charges, not " + spec.capacity());
			long left = EnergyCellItem.CAPACITY - (long) spec.capacity() * charge;
			helper.assertTrue(Chargeable.energy(cell) == left, "The reload left " + Chargeable.energy(cell) + " JE in the cell, not " + left);
			helper.succeed();
		});
	}

	/** A still pig of 20 health on a stone pillar, so it stands level with a shooter's eye. */
	private static Mob raised(GameTestHelper helper, BlockPos pillar) {
		helper.setBlock(pillar, Blocks.STONE);
		Mob pig = GunsGameTests.pig(helper, pillar.above());
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0);
		pig.setHealth(20.0F);
		return pig;
	}

	private static float lost(Mob mob) {
		return mob.getMaxHealth() - mob.getHealth();
	}

	private static boolean took(Mob mob, float damage) {
		return Math.abs(lost(mob) - damage) < 1.0E-3F;
	}
}
