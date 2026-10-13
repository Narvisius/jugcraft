package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.chemistry.PetroItems;
import io.github.jimbozoomer.jugcraft.guns.GunItem;
import io.github.jimbozoomer.jugcraft.guns.GunShots;
import io.github.jimbozoomer.jugcraft.guns.GunSpec;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import io.github.jimbozoomer.jugcraft.weapons.GrenadeEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * In-game tests for the heavy weapons (slice 8C, docs/features/guns.md), through the server's entry points as
 * {@link GunsGameTests} does: the Trench Lobber lobs a Grenade that bursts on what it is aimed at and breaks no block;
 * the Thresher fires only once its barrels have spun up, and stops when they run down; the Stoker's burst singes and
 * sets alight the creature in its jet, not one beside it or out of its reach, and sets no block alight; a blaze powder
 * loads four bursts, and the Lobber loads grenades from the inventory. Shooters face south (+z).
 */
public class HeavyGunsGameTests {
	/**
	 * The heavy weapons fire their own ammunition: the Lobber the field chemistry branch's Grenade, the Stoker blaze
	 * powder, four bursts to a powder; and the flame is fire, counts each burst and pushes nothing back.
	 */
	@GameTest
	public void heavyWeaponsFireTheirOwnAmmunition(GameTestHelper helper) {
		GunSpec lobber = JugcraftGuns.SPECS.get("trench_lobber");
		GunSpec thresher = JugcraftGuns.SPECS.get("thresher");
		GunSpec stoker = JugcraftGuns.SPECS.get("stoker");
		helper.assertTrue(JugcraftGuns.ammo(lobber) == PetroItems.GRENADE, "The Trench Lobber does not fire Grenades");
		helper.assertTrue(JugcraftGuns.ammo(stoker) == Items.BLAZE_POWDER, "The Stoker does not burn blaze powder");
		helper.assertTrue(JugcraftGuns.ammo(thresher) == JugcraftGuns.ROUNDS.get("rifle_round"), "The Thresher does not fire rifle rounds");
		helper.assertTrue(JugcraftGuns.perItem(stoker) == 4 && JugcraftGuns.perItem(lobber) == 1 && JugcraftGuns.perItem(thresher) == 1,
				"A blaze powder is not four bursts, or a grenade or round not one");
		helper.assertTrue(JugcraftGuns.shot(JugcraftGuns.GUNS.get("trench_lobber")).equals(JugcraftGuns.GRENADE)
				&& JugcraftGuns.shot(JugcraftGuns.GUNS.get("stoker")).equals(JugcraftGuns.FLAME)
				&& JugcraftGuns.shot(JugcraftGuns.GUNS.get("thresher")).equals("bullet"), "A heavy weapon fires the wrong thing");
		helper.assertTrue(JugcraftGuns.spinUp(JugcraftGuns.GUNS.get("thresher")) > 0 && thresher.auto(),
				"The Thresher's barrels do not spin up");
		Holder<DamageType> flame = helper.getLevel().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
				.getOrThrow(JugcraftGuns.FLAME_DAMAGE);
		helper.assertTrue(flame.is(DamageTypeTags.IS_FIRE) && flame.is(DamageTypeTags.BYPASSES_COOLDOWN)
				&& flame.is(DamageTypeTags.NO_KNOCKBACK), "The flame is not fire, or does not count each burst, or knocks back");
		helper.succeed();
	}

	/**
	 * A Trench Lobber shot spends a grenade and lobs a Grenade, owned by the shooter, that bursts on the pig seven blocks
	 * off: the pig is hurt, the shooter is out of the burst, and the floor is whole.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 60)
	public void lobberLobsAGrenade(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = GunsGameTests.pig(helper, new BlockPos(1, 2, 8));
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
		pig.setHealth(200.0F);
		ServerPlayer shooter = GunsGameTests.shooter(helper, "trench_lobber", 6, pig, GameType.SURVIVAL);
		float shooterHealth = shooter.getHealth();
		helper.assertTrue(GunShots.fire(shooter), "The loaded Trench Lobber did not fire");
		helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 5, "The shot did not spend one grenade");
		AABB arena = new AABB(helper.absolutePos(new BlockPos(0, 0, 0))).expandTowards(16, 6, 16);
		helper.assertTrue(helper.getLevel().getEntitiesOfClass(GrenadeEntity.class, arena, grenade -> grenade.getOwner() == shooter).size() == 1,
				"The shot lobbed no grenade of the shooter's");
		helper.succeedWhen(() -> {
			helper.assertTrue(pig.getHealth() < 200.0F, "The grenade has not burst on the pig yet");
			helper.assertTrue(shooter.getHealth() == shooterHealth, "The burst seven blocks off hurt the shooter");
			for (int x = 0; x <= 15; x++) {
				for (int z = 0; z <= 15; z++) {
					helper.assertBlockPresent(Blocks.STONE, new BlockPos(x, 1, z));
				}
			}
		});
	}

	/**
	 * The Thresher fires only once its trigger has been held, the barrels spinning, for its spin-up: a shot refused at
	 * once and part way, fired once spun up; and after the trigger has been let go a moment, refused again.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 60)
	public void thresherSpinsUpBeforeItFires(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = GunsGameTests.pig(helper, new BlockPos(1, 2, 4));
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
		pig.setHealth(200.0F);
		GunSpec spec = JugcraftGuns.SPECS.get("thresher");
		int spinUp = JugcraftGuns.spinUp(JugcraftGuns.GUNS.get("thresher"));
		ServerPlayer shooter = GunsGameTests.shooter(helper, "thresher", spec.capacity(), pig, GameType.SURVIVAL);
		helper.assertFalse(GunShots.fire(shooter), "The Thresher fired before its barrels turned");
		// The client says the trigger is held each tick; the first word starts the count.
		for (int tick = 1; tick <= spinUp; tick++) {
			int at = tick;
			helper.runAfterDelay(at, () -> {
				GunShots.spin(shooter);
				if (at == spinUp / 2) {
					helper.assertFalse(GunShots.fire(shooter), "The Thresher fired half way through its spin-up");
				}
				if (at == spinUp) {
					helper.assertTrue(GunShots.fire(shooter), "The Thresher did not fire once its barrels had spun up");
					helper.assertTrue(pig.getHealth() < 200.0F, "The Thresher's shot missed the pig");
					helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == spec.capacity() - 1, "The shot did not spend a round");
				}
			});
		}
		// Let go: no word for longer than a gap, and the barrels have stopped.
		helper.runAfterDelay(spinUp + 8, () -> {
			helper.assertFalse(GunShots.fire(shooter), "The Thresher fired after its trigger was let go");
			helper.succeed();
		});
	}

	/**
	 * A Stoker burst singes the pig four blocks ahead by the burst's damage and sets it alight; a pig beside the shooter
	 * (out of the jet) and one twelve blocks ahead (out of its reach) are untouched; no block catches fire.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 40)
	public void stokerSetsCreaturesAlightNotBlocks(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob ahead = GunsGameTests.pig(helper, new BlockPos(1, 2, 5));
		Mob beside = GunsGameTests.pig(helper, new BlockPos(5, 2, 1));
		Mob far = GunsGameTests.pig(helper, new BlockPos(1, 2, 13));
		GunSpec spec = JugcraftGuns.SPECS.get("stoker");
		ServerPlayer shooter = GunsGameTests.shooter(helper, "stoker", spec.capacity(), ahead, GameType.SURVIVAL);
		float max = ahead.getHealth();
		helper.assertTrue(GunShots.fire(shooter), "The loaded Stoker did not fire");
		helper.assertTrue(Math.abs(max - ahead.getHealth() - spec.damage()) < 1.0E-3F,
				"The burst took " + (max - ahead.getHealth()) + " from the pig ahead, not " + spec.damage());
		helper.assertTrue(ahead.getRemainingFireTicks() > 0, "The burst did not set the pig ahead alight");
		helper.assertTrue(beside.getHealth() == beside.getMaxHealth() && beside.getRemainingFireTicks() <= 0,
				"The burst reached the pig beside the shooter");
		helper.assertTrue(far.getHealth() == far.getMaxHealth() && far.getRemainingFireTicks() <= 0,
				"The burst reached the pig beyond its range");
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
	 * A reload takes ammunition by the item: three blaze powders fill the empty Stoker with twelve bursts; one more tops
	 * up a Stoker one burst short, the rest of that powder lost; the Trench Lobber loads the two grenades there are.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 200)
	public void reloadsTakeAmmunitionByTheItem(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = GunsGameTests.pig(helper, new BlockPos(1, 2, 8));
		GunSpec stoker = JugcraftGuns.SPECS.get("stoker");
		GunSpec lobber = JugcraftGuns.SPECS.get("trench_lobber");
		ServerPlayer shooter = GunsGameTests.shooter(helper, "stoker", 0, pig, GameType.SURVIVAL);
		shooter.getInventory().add(new ItemStack(Items.BLAZE_POWDER, 3));
		helper.assertTrue(GunShots.reload(shooter), "The empty Stoker did not start to reload");
		int second = stoker.reload() + 2;
		helper.runAfterDelay(second, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 12, "Three blaze powders loaded "
					+ GunItem.loaded(shooter.getMainHandItem()) + " bursts, not twelve");
			helper.assertTrue(GunShots.count(shooter.getInventory(), Items.BLAZE_POWDER) == 0, "The reload left blaze powder");
			GunItem.setLoaded(shooter.getMainHandItem(), stoker.capacity() - 1);
			shooter.getInventory().add(new ItemStack(Items.BLAZE_POWDER, 2));
			helper.assertTrue(GunShots.reload(shooter), "The Stoker one burst short did not start to reload");
		});
		int third = second + stoker.reload() + 2;
		helper.runAfterDelay(third, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == stoker.capacity(), "The top-up did not fill the Stoker");
			helper.assertTrue(GunShots.count(shooter.getInventory(), Items.BLAZE_POWDER) == 1, "The top-up did not take one whole powder");
			ItemStack gun = new ItemStack(JugcraftGuns.GUNS.get("trench_lobber"));
			shooter.setItemInHand(InteractionHand.MAIN_HAND, gun);
			shooter.getInventory().add(new ItemStack(PetroItems.GRENADE, 2));
			helper.assertTrue(GunShots.reload(shooter), "The empty Trench Lobber did not start to reload");
		});
		helper.runAfterDelay(third + lobber.reload() + 2, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 2, "The Trench Lobber loaded "
					+ GunItem.loaded(shooter.getMainHandItem()) + " grenades, not the two there were");
			helper.assertTrue(GunShots.count(shooter.getInventory(), PetroItems.GRENADE) == 0, "The Lobber's reload left a grenade");
			helper.succeed();
		});
	}
}
