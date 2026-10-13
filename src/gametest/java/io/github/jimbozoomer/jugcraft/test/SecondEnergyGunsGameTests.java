package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.guns.EnergyCellItem;
import io.github.jimbozoomer.jugcraft.guns.GunItem;
import io.github.jimbozoomer.jugcraft.guns.GunShots;
import io.github.jimbozoomer.jugcraft.guns.GunSpec;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import io.github.jimbozoomer.jugcraft.tools.Chargeable;
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

/**
 * In-game tests for the second energy weapons (slice 9C, docs/features/guns.md), through the server's entry points as
 * {@link EnergyGunsGameTests} does: they load from Energy Cells as slice 8D's do; the Spikedriver's heavy beam passes
 * through every creature in its line and stops at a block, and an Extended Magazine holds half as many charges again;
 * the Seam Cutter burns for as long as the trigger is held; the Caisson Pistol's arc finds a creature further off its
 * aim than the Stormlock's would, and leaps on. The creatures stand on pillars, level with the shooter's eye. Shooters
 * face south (+z).
 */
public class SecondEnergyGunsGameTests {
	/** The second energy weapons, in the order the creative tab shows them. */
	static final List<String> GUNS = List.of("spikedriver", "seam_cutter", "caisson_pistol");

	/**
	 * Each loads its charge from Energy Cells, a magazine's worth from at most one cell, and its recipe loads (with the
	 * guns and the machines on). The Spikedriver and the Seam Cutter fire beams, the Caisson Pistol an arc; only the
	 * Seam Cutter fires while the trigger is held. The Spikedriver, made with magazines and stocks to swap, takes them;
	 * the other two, made with none, take nothing.
	 */
	@GameTest
	public void secondEnergyWeaponsRunOnCells(GameTestHelper helper) {
		for (String name : GUNS) {
			GunSpec spec = JugcraftGuns.SPECS.get(name);
			GunItem gun = JugcraftGuns.GUNS.get(name);
			helper.assertTrue(spec != null && gun != null && gun.spec() == spec, name + " is not registered with its numbers");
			helper.assertTrue(JugcraftGuns.ammo(spec) == JugcraftGuns.ENERGY_CELL && JugcraftGuns.charge(gun) > 0
					&& JugcraftGuns.charge(gun) * spec.capacity() <= EnergyCellItem.CAPACITY,
					name + " does not load its magazine's charge from one Energy Cell");
			helper.assertTrue(spec.pellets() == 1 && spec.auto() == name.equals("seam_cutter"),
					name + (spec.auto() ? " fires while the trigger is held" : " fires once a pull"));
			helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(name))).isPresent(),
					"The " + name + " recipe does not load");
		}
		helper.assertTrue(JugcraftGuns.shot(JugcraftGuns.GUNS.get("spikedriver")).equals(JugcraftGuns.BEAM)
				&& JugcraftGuns.shot(JugcraftGuns.GUNS.get("seam_cutter")).equals(JugcraftGuns.BEAM)
				&& JugcraftGuns.shot(JugcraftGuns.GUNS.get("caisson_pistol")).equals(JugcraftGuns.ARC), "A second energy weapon fires the wrong thing");
		helper.assertTrue(JugcraftGuns.ACCEPTS.get("spikedriver").equals(List.of("extended_magazine", "speed_magazine", "light_stock",
				"weighted_stock", "wooden_stock")), "The Spikedriver does not take its magazines and stocks");
		helper.assertTrue(!JugcraftGuns.ACCEPTS.containsKey("seam_cutter") && !JugcraftGuns.ACCEPTS.containsKey("caisson_pistol"),
				"The Seam Cutter or the Caisson Pistol takes an attachment");
		helper.succeed();
	}

	/**
	 * A Spikedriver shot passes through the two pigs in its line, each taking its damage, and stops at the wall: the pig
	 * behind it is untouched. The shot spends a charge. Fitted with an Extended Magazine, an empty Spikedriver loads nine
	 * charges, not six, from a full cell after the magazine's longer reload, and the cell keeps the rest.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 200)
	public void spikedriverDrivesThroughItsLine(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob near = raised(helper, new BlockPos(1, 2, 4));
		Mob behind = raised(helper, new BlockPos(1, 2, 7));
		Mob walled = raised(helper, new BlockPos(1, 2, 12));
		for (int x = 0; x <= 3; x++) {
			for (int y = 2; y <= 4; y++) {
				helper.setBlock(new BlockPos(x, y, 10), Blocks.STONE);
			}
		}
		GunSpec spec = JugcraftGuns.SPECS.get("spikedriver");
		ServerPlayer shooter = GunsGameTests.shooter(helper, "spikedriver", spec.capacity(), near, GameType.SURVIVAL);
		helper.assertTrue(GunShots.fire(shooter), "The loaded Spikedriver did not fire");
		helper.assertTrue(took(near, spec.damage()), "The shot took " + lost(near) + " from the near pig, not " + spec.damage());
		helper.assertTrue(took(behind, spec.damage()), "The shot took " + lost(behind) + " from the pig behind it, not " + spec.damage());
		helper.assertTrue(lost(walled) == 0.0F, "The shot passed the wall");
		helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == spec.capacity() - 1, "The shot did not spend a charge");

		ItemStack fitted = new ItemStack(JugcraftGuns.GUNS.get("spikedriver"));
		GunItem.fit(fitted, "extended_magazine");
		shooter.setItemInHand(InteractionHand.MAIN_HAND, fitted);
		ItemStack cell = cell(EnergyCellItem.CAPACITY);
		shooter.getInventory().setItem(1, cell);
		GunSpec extended = GunItem.spec(fitted);
		int charge = JugcraftGuns.charge(JugcraftGuns.GUNS.get("spikedriver"));
		helper.assertTrue(extended.capacity() == 9 && extended.reload() > spec.reload(),
				"The Extended Magazine holds " + extended.capacity() + " charges, not nine, or does not take longer to load");
		helper.assertTrue(GunShots.reload(shooter), "The empty Spikedriver did not start to reload");
		helper.runAfterDelay(spec.reload() + 2, () -> helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 0,
				"The Extended Magazine loaded in the Spikedriver's own reload time"));
		helper.runAfterDelay(extended.reload() + 2, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 9,
					"The Spikedriver loaded " + GunItem.loaded(shooter.getMainHandItem()) + " charges into the Extended Magazine, not nine");
			helper.assertTrue(Chargeable.energy(cell) == EnergyCellItem.CAPACITY - 9L * charge,
					"The reload left " + Chargeable.energy(cell) + " JE in the cell, not " + (EnergyCellItem.CAPACITY - 9L * charge));
			helper.assertTrue(shooter.getInventory().getItem(1) == cell, "The reload took the cell");
			helper.succeed();
		});
	}

	/**
	 * The Seam Cutter fires for as long as the trigger is held: two shots at once (a player's trigger banks two) and a
	 * third an interval later, each burning through both pigs in its line and spending a charge.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 40)
	public void seamCutterBurnsWhileHeld(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob near = raised(helper, new BlockPos(1, 2, 4));
		Mob behind = raised(helper, new BlockPos(1, 2, 7));
		GunSpec spec = JugcraftGuns.SPECS.get("seam_cutter");
		ServerPlayer shooter = GunsGameTests.shooter(helper, "seam_cutter", spec.capacity(), near, GameType.SURVIVAL);
		helper.assertTrue(GunShots.fire(shooter) && GunShots.fire(shooter), "The Seam Cutter did not fire twice at once");
		helper.runAfterDelay(spec.interval(), () -> {
			helper.assertTrue(GunShots.fire(shooter), "The Seam Cutter did not fire again an interval later");
			float three = 3.0F * spec.damage();
			helper.assertTrue(took(near, three), "Three shots took " + lost(near) + " from the near pig, not " + three);
			helper.assertTrue(took(behind, three), "Three shots took " + lost(behind) + " from the pig behind it, not " + three);
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == spec.capacity() - 3, "The three shots did not spend three charges");
			helper.succeed();
		});
	}

	/**
	 * A pig stands six blocks ahead and a block to the side, its near edge about four degrees off the aim. The Stormlock's
	 * arc, which seeks two degrees off its aim from the hip, finds nothing and strikes no pig. The Caisson Pistol's,
	 * fired the same way, finds that pig, strikes it and leaps to the pig two blocks beside it, which takes its share;
	 * the pig far off to the side, out of its cone and too far from either to leap to, is untouched.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 40)
	public void caissonArcSeeksWider(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob ahead = raised(helper, new BlockPos(2, 2, 7));
		Mob beside = raised(helper, new BlockPos(4, 2, 7));
		Mob aside = raised(helper, new BlockPos(12, 2, 3));
		GunSpec stormlock = JugcraftGuns.SPECS.get("stormlock_rifle");
		GunSpec spec = JugcraftGuns.SPECS.get("caisson_pistol");
		helper.assertTrue(stormlock.hipSpread() < 3.0F && spec.hipSpread() > 5.0F,
				"The Stormlock's cone is not under three degrees from the hip, or the Caisson Pistol's not over five");
		// shooter() aims level with the pig and straight ahead (south), so the pig's middle is a block to the side.
		ServerPlayer shooter = GunsGameTests.shooter(helper, "stormlock_rifle", stormlock.capacity(), ahead, GameType.SURVIVAL);
		helper.assertTrue(GunShots.fire(shooter), "The loaded Stormlock did not fire");
		helper.assertTrue(lost(ahead) == 0.0F && lost(beside) == 0.0F, "The Stormlock's arc found the pig off its aim");
		ItemStack pistol = new ItemStack(JugcraftGuns.GUNS.get("caisson_pistol"));
		GunItem.setLoaded(pistol, spec.capacity());
		shooter.setItemInHand(InteractionHand.MAIN_HAND, pistol);
		helper.assertTrue(GunShots.fire(shooter), "The loaded Caisson Pistol did not fire");
		helper.assertTrue(took(ahead, spec.damage()), "The arc took " + lost(ahead) + " from the pig off its aim, not " + spec.damage());
		float share = spec.damage() * JugcraftGuns.ARC_SHARE;
		helper.assertTrue(took(beside, share), "The leap took " + lost(beside) + " from the pig beside it, not " + share);
		helper.assertTrue(lost(aside) == 0.0F, "The arc reached the pig out of its cone");
		helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == spec.capacity() - 1, "The shot did not spend a charge");
		helper.succeed();
	}

	/** An Energy Cell holding this much charge. */
	private static ItemStack cell(long energy) {
		ItemStack cell = new ItemStack(JugcraftGuns.ENERGY_CELL);
		Chargeable.setEnergy(cell, energy);
		return cell;
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
