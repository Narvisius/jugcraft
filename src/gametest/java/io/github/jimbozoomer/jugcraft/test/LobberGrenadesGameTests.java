package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.chemistry.PetroItems;
import io.github.jimbozoomer.jugcraft.guns.GunItem;
import io.github.jimbozoomer.jugcraft.guns.GunShots;
import io.github.jimbozoomer.jugcraft.guns.GunSpec;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import io.github.jimbozoomer.jugcraft.weapons.ChemicalCloud;
import io.github.jimbozoomer.jugcraft.weapons.FieldChemistry;
import io.github.jimbozoomer.jugcraft.weapons.GrenadeEntity;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

/**
 * In-game tests for the Trench Lobber's grenades (slice 9G, docs/features/guns.md), through the server's entry points
 * as {@link GunsGameTests} does: it loads any grenade, one kind at a time, the one in the other hand first and else the
 * kind it holds while there is one; a reload of another kind puts the grenades it held back in the inventory; and it
 * lobs the kind it holds. Shooters face south (+z).
 */
public class LobberGrenadesGameTests {
	/**
	 * Only the grenade guns take grenades: the Trench Lobber and (slice 10A) the Bullfrog. A Lobber that has only held
	 * frag Grenades holds those and carries no record of its kind; one recorded as holding chlorine grenades holds those;
	 * and one whose record names an item no longer known, or one that is not a grenade, holds frag Grenades again.
	 */
	@GameTest
	public void onlyTheGrenadeGunsTakeGrenades(GameTestHelper helper) {
		List<String> grenadeGuns = List.of("trench_lobber", "bullfrog");
		JugcraftGuns.SPECS.forEach((name, spec) -> helper.assertTrue(JugcraftGuns.takesGrenades(spec) == grenadeGuns.contains(name),
				"The " + name + (grenadeGuns.contains(name) ? " does not take" : " takes") + " grenades"));
		GunSpec lobber = JugcraftGuns.SPECS.get("trench_lobber");
		ItemStack gun = new ItemStack(JugcraftGuns.GUNS.get("trench_lobber"));
		helper.assertTrue(JugcraftGuns.loadedAmmo(gun, lobber) == PetroItems.GRENADE && !gun.has(JugcraftGuns.LOADED_GRENADE),
				"A new Trench Lobber does not hold frag Grenades, or records a kind");
		JugcraftGuns.setLoadedAmmo(gun, lobber, FieldChemistry.CHLORINE_GRENADE);
		helper.assertTrue(JugcraftGuns.loadedAmmo(gun, lobber) == FieldChemistry.CHLORINE_GRENADE, "The Lobber does not hold chlorine grenades");
		JugcraftGuns.setLoadedAmmo(gun, lobber, PetroItems.GRENADE);
		helper.assertTrue(!gun.has(JugcraftGuns.LOADED_GRENADE), "A Lobber of frag Grenades records its kind");
		gun.set(JugcraftGuns.LOADED_GRENADE, "jugcraft:no_such_grenade");
		helper.assertTrue(JugcraftGuns.loadedAmmo(gun, lobber) == PetroItems.GRENADE, "A Lobber of an unknown grenade holds something else");
		gun.set(JugcraftGuns.LOADED_GRENADE, "minecraft:stone");
		helper.assertTrue(JugcraftGuns.loadedAmmo(gun, lobber) == PetroItems.GRENADE, "A Lobber holds stone");
		helper.succeed();
	}

	/**
	 * An empty Lobber, frag Grenades in the inventory and three chlorine grenades in the other hand: its reload loads the
	 * three chlorine grenades and leaves the frag Grenades.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 100)
	public void lobberLoadsTheGrenadeInTheOtherHand(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = GunsGameTests.pig(helper, new BlockPos(1, 2, 8));
		GunSpec lobber = JugcraftGuns.SPECS.get("trench_lobber");
		ServerPlayer shooter = GunsGameTests.shooter(helper, "trench_lobber", 0, pig, GameType.SURVIVAL);
		shooter.getInventory().add(new ItemStack(PetroItems.GRENADE, 4));
		shooter.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(FieldChemistry.CHLORINE_GRENADE, 3));
		helper.assertTrue(GunShots.reloadAmmo(shooter, shooter.getMainHandItem(), lobber) == FieldChemistry.CHLORINE_GRENADE,
				"The Lobber's reload would not load the chlorine grenades in the other hand");
		helper.assertTrue(GunShots.reload(shooter), "The empty Lobber did not start to reload");
		helper.runAfterDelay(lobber.reload() + 2, () -> {
			ItemStack gun = shooter.getMainHandItem();
			helper.assertTrue(GunItem.loaded(gun) == 3 && JugcraftGuns.loadedAmmo(gun, lobber) == FieldChemistry.CHLORINE_GRENADE,
					"The Lobber holds " + GunItem.loaded(gun) + " " + JugcraftGuns.loadedAmmo(gun, lobber) + ", not three chlorine grenades");
			helper.assertTrue(shooter.getOffhandItem().isEmpty(), "The reload left chlorine grenades in the other hand");
			helper.assertTrue(GunShots.count(shooter.getInventory(), PetroItems.GRENADE) == 4, "The reload took frag Grenades");
			helper.succeed();
		});
	}

	/**
	 * A Lobber of two frag Grenades, smoke grenades in the other hand: its reload puts the two frag Grenades back in the
	 * inventory and loads six smoke grenades. Then, one smoke grenade left in it and only the frag Grenades to hand, its
	 * reload loads those and puts the smoke grenade back: it keeps to its kind only while there is one.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 200)
	public void lobberSwapsOneKindForAnother(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = GunsGameTests.pig(helper, new BlockPos(1, 2, 8));
		GunSpec lobber = JugcraftGuns.SPECS.get("trench_lobber");
		ServerPlayer shooter = GunsGameTests.shooter(helper, "trench_lobber", 2, pig, GameType.SURVIVAL);
		shooter.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(FieldChemistry.SMOKE_GRENADE, 8));
		helper.assertTrue(GunShots.reload(shooter), "The Lobber of frag Grenades did not start to reload smoke grenades");
		int first = lobber.reload() + 2;
		helper.runAfterDelay(first, () -> {
			ItemStack gun = shooter.getMainHandItem();
			helper.assertTrue(GunItem.loaded(gun) == lobber.capacity() && JugcraftGuns.loadedAmmo(gun, lobber) == FieldChemistry.SMOKE_GRENADE,
					"The Lobber holds " + GunItem.loaded(gun) + " " + JugcraftGuns.loadedAmmo(gun, lobber) + ", not a full load of smoke grenades");
			helper.assertTrue(GunShots.count(shooter.getInventory(), PetroItems.GRENADE) == 2,
					"The two frag Grenades did not come back: " + GunShots.count(shooter.getInventory(), PetroItems.GRENADE));
			helper.assertTrue(GunShots.count(shooter.getInventory(), FieldChemistry.SMOKE_GRENADE) == 8 - lobber.capacity(),
					"The reload did not take a full load of smoke grenades");
			shooter.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
			GunItem.setLoaded(gun, 1);
			helper.assertTrue(GunShots.reloadAmmo(shooter, gun, lobber) == PetroItems.GRENADE,
					"With no smoke grenade to hand, the Lobber's reload would not load frag Grenades");
			helper.assertTrue(GunShots.reload(shooter), "The Lobber of one smoke grenade did not start to reload frag Grenades");
		});
		helper.runAfterDelay(first + lobber.reload() + 2, () -> {
			ItemStack gun = shooter.getMainHandItem();
			helper.assertTrue(GunItem.loaded(gun) == 2 && JugcraftGuns.loadedAmmo(gun, lobber) == PetroItems.GRENADE
					&& !gun.has(JugcraftGuns.LOADED_GRENADE),
					"The Lobber holds " + GunItem.loaded(gun) + " " + JugcraftGuns.loadedAmmo(gun, lobber) + ", not the two frag Grenades");
			helper.assertTrue(GunShots.count(shooter.getInventory(), FieldChemistry.SMOKE_GRENADE) == 1,
					"The smoke grenade did not come back: " + GunShots.count(shooter.getInventory(), FieldChemistry.SMOKE_GRENADE));
			helper.succeed();
		});
	}

	/**
	 * An empty Lobber that last held thermite grenades, frag Grenades ahead of thermite grenades in the inventory: its
	 * reload keeps to thermite and loads the two there are.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 100)
	public void lobberKeepsToItsKind(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = GunsGameTests.pig(helper, new BlockPos(1, 2, 8));
		GunSpec lobber = JugcraftGuns.SPECS.get("trench_lobber");
		ServerPlayer shooter = GunsGameTests.shooter(helper, "trench_lobber", 0, pig, GameType.SURVIVAL);
		JugcraftGuns.setLoadedAmmo(shooter.getMainHandItem(), lobber, FieldChemistry.THERMITE_GRENADE);
		// Each into the first free slot, so the frag Grenades come first in the inventory.
		shooter.getInventory().add(new ItemStack(PetroItems.GRENADE, 4));
		shooter.getInventory().add(new ItemStack(FieldChemistry.THERMITE_GRENADE, 2));
		helper.assertTrue(GunShots.reloadAmmo(shooter, shooter.getMainHandItem(), lobber) == FieldChemistry.THERMITE_GRENADE,
				"The Lobber's reload would not keep to thermite grenades");
		helper.assertTrue(GunShots.reload(shooter), "The empty Lobber did not start to reload");
		helper.runAfterDelay(lobber.reload() + 2, () -> {
			ItemStack gun = shooter.getMainHandItem();
			helper.assertTrue(GunItem.loaded(gun) == 2 && JugcraftGuns.loadedAmmo(gun, lobber) == FieldChemistry.THERMITE_GRENADE,
					"The Lobber holds " + GunItem.loaded(gun) + " " + JugcraftGuns.loadedAmmo(gun, lobber) + ", not two thermite grenades");
			helper.assertTrue(GunShots.count(shooter.getInventory(), PetroItems.GRENADE) == 4, "The reload took frag Grenades");
			helper.succeed();
		});
	}

	/**
	 * A Lobber of chlorine grenades lobs one at a pig seven blocks off: a chlorine grenade, owned by the shooter, and
	 * where it lands a chlorine cloud that hurts the pig.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 80)
	public void lobberLobsTheKindItHolds(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = GunsGameTests.pig(helper, new BlockPos(1, 2, 8));
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
		pig.setHealth(200.0F);
		GunSpec lobber = JugcraftGuns.SPECS.get("trench_lobber");
		ServerPlayer shooter = GunsGameTests.shooter(helper, "trench_lobber", 1, pig, GameType.SURVIVAL);
		JugcraftGuns.setLoadedAmmo(shooter.getMainHandItem(), lobber, FieldChemistry.CHLORINE_GRENADE);
		helper.assertTrue(GunShots.fire(shooter), "The Lobber of chlorine grenades did not fire");
		AABB arena = new AABB(helper.absolutePos(new BlockPos(0, 0, 0))).expandTowards(16, 6, 16);
		List<GrenadeEntity> lobbed = helper.getLevel().getEntitiesOfClass(GrenadeEntity.class, arena, grenade -> grenade.getOwner() == shooter);
		helper.assertTrue(lobbed.size() == 1 && lobbed.getFirst().getItem().is(FieldChemistry.CHLORINE_GRENADE),
				"The shot did not lob one chlorine grenade of the shooter's: " + lobbed.stream().map(GrenadeEntity::getItem).toList());
		helper.succeedWhen(() -> {
			helper.assertTrue(helper.getLevel().getEntitiesOfClass(ChemicalCloud.class, arena).stream()
					.anyMatch(cloud -> cloud.kind() == ChemicalCloud.Kind.CHLORINE), "No chlorine cloud hangs where the grenade landed");
			helper.assertTrue(pig.getHealth() < 200.0F, "The chlorine has not hurt the pig yet");
		});
	}
}
