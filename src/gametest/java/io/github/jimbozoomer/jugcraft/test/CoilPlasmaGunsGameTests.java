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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * In-game tests for coil and plasma (slice 10B, docs/features/guns.md), through the server's entry points as
 * {@link EnergyGunsGameTests} does: the three load from Energy Cells as slice 8D's do; the Solenoid Rifle's heavy beam
 * passes through every creature in its line and stops at a block; the Votive Rifle burns for as long as the trigger is
 * held; the Glowmouth is loaded a charge at a time, and its arc finds a creature wide of its aim and leaps on. The
 * creatures stand on pillars, level with the shooter's eye. Shooters face south (+z).
 */
public class CoilPlasmaGunsGameTests {
	/** Coil and plasma, in the order the creative tab shows them. */
	static final List<String> GUNS = List.of("solenoid_rifle", "votive_rifle", "glowmouth");
	/** What the two rifles take: the owner made each a part for every attachment but the barrel's. */
	static final List<String> RIFLE_ATTACHMENTS = List.of("extended_magazine", "speed_magazine", "light_stock",
			"weighted_stock", "wooden_stock", "light_grip", "iron_bayonet", "steel_bayonet", "diamond_bayonet",
			"netherite_bayonet", "long_scope", "medium_scope", "reflex_sight", "tactical_grip", "laser_sight");

	/**
	 * Each loads its charge from Energy Cells, a magazine's worth from at most one cell, and its recipe loads (with the
	 * guns and the machines on). The Solenoid and Votive Rifles fire beams, the Glowmouth an arc; only the Votive Rifle
	 * fires while the trigger is held, and only the Glowmouth is loaded a charge at a time. The Solenoid Rifle reaches
	 * the farthest of the energy weapons. The two rifles take every attachment but the barrel's; the Glowmouth, made with
	 * none, takes nothing.
	 */
	@GameTest
	public void coilAndPlasmaRunOnCells(GameTestHelper helper) {
		for (String name : GUNS) {
			GunSpec spec = JugcraftGuns.SPECS.get(name);
			GunItem gun = JugcraftGuns.GUNS.get(name);
			helper.assertTrue(spec != null && gun != null && gun.spec() == spec, name + " is not registered with its numbers");
			helper.assertTrue(JugcraftGuns.ammo(spec) == JugcraftGuns.ENERGY_CELL && JugcraftGuns.charge(gun) > 0
					&& JugcraftGuns.charge(gun) * spec.capacity() <= EnergyCellItem.CAPACITY,
					name + " does not load its magazine's charge from one Energy Cell");
			helper.assertTrue(spec.pellets() == 1 && spec.auto() == name.equals("votive_rifle"),
					name + (spec.auto() ? " fires while the trigger is held" : " fires once a pull"));
			helper.assertTrue(spec.byShell() == name.equals("glowmouth"),
					name + (spec.byShell() ? " is loaded a charge at a time" : " is loaded a magazine at a time"));
			helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(name))).isPresent(),
					"The " + name + " recipe does not load");
		}
		helper.assertTrue(JugcraftGuns.shot(JugcraftGuns.GUNS.get("solenoid_rifle")).equals(JugcraftGuns.BEAM)
				&& JugcraftGuns.shot(JugcraftGuns.GUNS.get("votive_rifle")).equals(JugcraftGuns.BEAM)
				&& JugcraftGuns.shot(JugcraftGuns.GUNS.get("glowmouth")).equals(JugcraftGuns.ARC), "A coil or plasma gun fires the wrong thing");
		int solenoid = JugcraftGuns.SPECS.get("solenoid_rifle").range();
		for (String other : JugcraftGuns.CHARGE.keySet()) {
			helper.assertTrue(other.equals("solenoid_rifle") || JugcraftGuns.SPECS.get(other).range() < solenoid,
					"The " + other + " reaches as far as the Solenoid Rifle");
		}
		helper.assertTrue(JugcraftGuns.ACCEPTS.get("solenoid_rifle").equals(RIFLE_ATTACHMENTS)
				&& JugcraftGuns.ACCEPTS.get("votive_rifle").equals(RIFLE_ATTACHMENTS),
				"The Solenoid or Votive Rifle does not take its attachments");
		helper.assertTrue(!JugcraftGuns.ACCEPTS.containsKey("glowmouth"), "The Glowmouth takes an attachment");
		helper.succeed();
	}

	/**
	 * A Solenoid Rifle shot passes through the two pigs in its line, each taking its damage, and stops at the wall: the
	 * pig behind it is untouched. The shot spends a charge. Then an empty Solenoid Rifle loads its five charges from a
	 * full cell, which keeps the rest.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 200)
	public void solenoidDrivesThroughItsLine(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob near = raised(helper, new BlockPos(1, 2, 4));
		Mob behind = raised(helper, new BlockPos(1, 2, 7));
		Mob walled = raised(helper, new BlockPos(1, 2, 12));
		for (int x = 0; x <= 3; x++) {
			for (int y = 2; y <= 4; y++) {
				helper.setBlock(new BlockPos(x, y, 10), Blocks.STONE);
			}
		}
		GunSpec spec = JugcraftGuns.SPECS.get("solenoid_rifle");
		ServerPlayer shooter = GunsGameTests.shooter(helper, "solenoid_rifle", spec.capacity(), near, GameType.SURVIVAL);
		helper.assertTrue(GunShots.fire(shooter), "The loaded Solenoid Rifle did not fire");
		helper.assertTrue(took(near, spec.damage()), "The shot took " + lost(near) + " from the near pig, not " + spec.damage());
		helper.assertTrue(took(behind, spec.damage()), "The shot took " + lost(behind) + " from the pig behind it, not " + spec.damage());
		helper.assertTrue(lost(walled) == 0.0F, "The shot passed the wall");
		helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == spec.capacity() - 1, "The shot did not spend a charge");

		GunItem.setLoaded(shooter.getMainHandItem(), 0);
		ItemStack cell = cell(EnergyCellItem.CAPACITY);
		shooter.getInventory().setItem(1, cell);
		int charge = JugcraftGuns.charge(JugcraftGuns.GUNS.get("solenoid_rifle"));
		helper.assertTrue(GunShots.reload(shooter), "The empty Solenoid Rifle did not start to reload");
		helper.runAfterDelay(spec.reload() + 2, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == spec.capacity(), "The Solenoid Rifle loaded "
					+ GunItem.loaded(shooter.getMainHandItem()) + " charges, not " + spec.capacity());
			long left = EnergyCellItem.CAPACITY - (long) spec.capacity() * charge;
			helper.assertTrue(Chargeable.energy(cell) == left, "The reload left " + Chargeable.energy(cell) + " JE in the cell, not " + left);
			helper.assertTrue(shooter.getInventory().getItem(1) == cell, "The reload took the cell");
			helper.succeed();
		});
	}

	/**
	 * The Votive Rifle fires for as long as the trigger is held: two shots at once (a player's trigger banks two) and a
	 * third an interval later, each burning through both pigs in its line and spending a charge.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 40)
	public void votiveRifleBurnsWhileHeld(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob near = raised(helper, new BlockPos(1, 2, 4));
		Mob behind = raised(helper, new BlockPos(1, 2, 7));
		GunSpec spec = JugcraftGuns.SPECS.get("votive_rifle");
		ServerPlayer shooter = GunsGameTests.shooter(helper, "votive_rifle", spec.capacity(), near, GameType.SURVIVAL);
		helper.assertTrue(GunShots.fire(shooter) && GunShots.fire(shooter), "The Votive Rifle did not fire twice at once");
		helper.runAfterDelay(spec.interval(), () -> {
			helper.assertTrue(GunShots.fire(shooter), "The Votive Rifle did not fire again an interval later");
			float three = 3.0F * spec.damage();
			helper.assertTrue(took(near, three), "Three shots took " + lost(near) + " from the near pig, not " + three);
			helper.assertTrue(took(behind, three), "Three shots took " + lost(behind) + " from the pig behind it, not " + three);
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == spec.capacity() - 3, "The three shots did not spend three charges");
			helper.succeed();
		});
	}

	/**
	 * An empty Glowmouth with a cell of 2,000 JE loads two of its 750 JE charges, one at a time (halfway through the
	 * second's time it holds one), and stops with 500 JE left in the cell, too little for a third.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 200)
	public void glowmouthLoadsAChargeAtATime(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = GunsGameTests.pig(helper, new BlockPos(1, 2, 8));
		GunSpec spec = JugcraftGuns.SPECS.get("glowmouth");
		ServerPlayer shooter = GunsGameTests.shooter(helper, "glowmouth", 0, pig, GameType.SURVIVAL);
		ItemStack cell = cell(2_000);
		shooter.getInventory().setItem(1, cell);
		helper.assertTrue(GunShots.reload(shooter), "The empty Glowmouth did not start to reload");
		// Halfway through the second charge's time, only the first is in.
		helper.runAfterDelay(spec.shellStart() + spec.shellEach() + spec.shellEach() / 2, () -> helper.assertTrue(
				GunItem.loaded(shooter.getMainHandItem()) == 1, "Halfway through loading its second charge the Glowmouth held "
						+ GunItem.loaded(shooter.getMainHandItem()) + ", not one"));
		helper.runAfterDelay(spec.reloadTicks(2) + 3, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 2, "The Glowmouth loaded "
					+ GunItem.loaded(shooter.getMainHandItem()) + " charges from 2,000 JE, not two of 750");
			helper.assertTrue(Chargeable.energy(cell) == 500, "The reload left " + Chargeable.energy(cell) + " JE in the cell, not 500");
			helper.succeed();
		});
	}

	/**
	 * A pig stands six blocks ahead and a block to the side, its near edge about four degrees off the aim. The
	 * Glowmouth's arc, which seeks twelve degrees off its aim from the hip, finds it, strikes it and leaps to the pig two
	 * blocks beside it, which takes its share; the pig far off to the side, out of its cone and too far from either to
	 * leap to, is untouched.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 40)
	public void glowmouthArcLeapsFromWideOfItsAim(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob ahead = raised(helper, new BlockPos(2, 2, 7));
		Mob beside = raised(helper, new BlockPos(4, 2, 7));
		Mob aside = raised(helper, new BlockPos(12, 2, 3));
		GunSpec spec = JugcraftGuns.SPECS.get("glowmouth");
		// shooter() aims level with the pig and straight ahead (south), so the pig's middle is a block to the side.
		ServerPlayer shooter = GunsGameTests.shooter(helper, "glowmouth", spec.capacity(), ahead, GameType.SURVIVAL);
		helper.assertTrue(GunShots.fire(shooter), "The loaded Glowmouth did not fire");
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
