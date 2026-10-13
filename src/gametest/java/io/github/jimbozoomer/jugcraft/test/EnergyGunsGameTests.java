package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.guns.EnergyCellItem;
import io.github.jimbozoomer.jugcraft.guns.GunItem;
import io.github.jimbozoomer.jugcraft.guns.GunShots;
import io.github.jimbozoomer.jugcraft.guns.GunSpec;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import io.github.jimbozoomer.jugcraft.tools.Chargeable;
import io.github.jimbozoomer.jugcraft.tools.ChargingStationBlock;
import io.github.jimbozoomer.jugcraft.tools.ChargingStationBlockEntity;
import io.github.jimbozoomer.jugcraft.tools.JugcraftTools;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * In-game tests for the energy weapons (slice 8D, docs/features/guns.md), through the server's entry points as
 * {@link GunsGameTests} does: they run on Energy Cells, which fill at the Charging Station; a reload draws each round's
 * charge from the cells, pooled, and leaves the cells; the Beam Pistol's beam passes through every creature in its
 * line and stops at a block; the Stormlock's arc leaps from its mark to the nearest creatures close by, each taking a
 * share of the last's damage; the Linesman's arc finds a creature off its aim, within its cone. The creatures stand
 * on pillars, level with the shooter's eye. Shooters face south (+z).
 */
public class EnergyGunsGameTests {
	/** Craft the actual survival recipe: rechargeable cells must never form an oversized result stack. */
	@GameTest
	public void cellRecipeMakesOneEmptyUnstackableCell(GameTestHelper helper) {
		var level = helper.getLevel();
		ItemStack cable = new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("copper_cable")));
		ItemStack brass = new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("brass_ingot")));
		CraftingInput input = CraftingInput.of(3, 3, List.of(
				ItemStack.EMPTY, cable, ItemStack.EMPTY,
				new ItemStack(Items.GLASS_PANE), new ItemStack(Items.REDSTONE), new ItemStack(Items.GLASS_PANE),
				ItemStack.EMPTY, brass, ItemStack.EMPTY));
		ItemStack made = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level)
				.orElseThrow(() -> helper.assertionException("The Energy Cell's survival recipe is missing"))
				.value().assemble(input);
		helper.assertTrue(made.is(JugcraftGuns.ENERGY_CELL) && made.getCount() == 1 && made.getMaxStackSize() == 1,
				"The Energy Cell recipe must make one unstackable cell, got " + made);
		helper.assertTrue(Chargeable.energy(made) == 0 && Chargeable.capacity(made) == EnergyCellItem.CAPACITY,
				"A newly crafted cell must be empty and retain its rechargeable capacity");
		helper.succeed();
	}

	/**
	 * The energy weapons load from the Energy Cell, a chargeable item, each round its charge: the Beam Pistol fires a
	 * beam, the other two arcs; their damage pushes nothing back, counts each shot, and is neither a projectile nor fire.
	 */
	@GameTest
	public void energyWeaponsRunOnCells(GameTestHelper helper) {
		helper.assertTrue(JugcraftGuns.ENERGY_CELL instanceof EnergyCellItem cell && cell.baseCapacity() == EnergyCellItem.CAPACITY,
				"The Energy Cell is not a chargeable item of its capacity");
		for (String name : JugcraftGuns.CHARGE.keySet()) {
			GunItem gun = JugcraftGuns.GUNS.get(name);
			helper.assertTrue(gun != null && JugcraftGuns.ammo(gun.spec()) == JugcraftGuns.ENERGY_CELL && JugcraftGuns.charge(gun) > 0,
					name + " does not load charge from Energy Cells");
			helper.assertTrue(JugcraftGuns.charge(gun) * gun.spec().capacity() <= EnergyCellItem.CAPACITY,
					name + "'s magazine takes more than a cell holds");
		}
		helper.assertTrue(JugcraftGuns.shot(JugcraftGuns.GUNS.get("beam_pistol")).equals(JugcraftGuns.BEAM)
				&& JugcraftGuns.shot(JugcraftGuns.GUNS.get("stormlock_rifle")).equals(JugcraftGuns.ARC)
				&& JugcraftGuns.shot(JugcraftGuns.GUNS.get("linesman")).equals(JugcraftGuns.ARC), "An energy weapon fires the wrong thing");
		helper.assertTrue(JugcraftGuns.charge(JugcraftGuns.GUNS.get("garrison_rifle")) == 0, "A gun that fires rounds draws charge");
		Holder<DamageType> zap = helper.getLevel().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(JugcraftGuns.ZAP_DAMAGE);
		helper.assertTrue(zap.is(DamageTypeTags.NO_KNOCKBACK) && zap.is(DamageTypeTags.BYPASSES_COOLDOWN)
				&& !zap.is(DamageTypeTags.IS_PROJECTILE) && !zap.is(DamageTypeTags.IS_FIRE),
				"A zap knocks back, does not count each shot, or is a projectile or fire");
		helper.succeed();
	}

	/**
	 * A reload draws its rounds' charge from the cells, the first first, and leaves them: two cells of 1,000 and 10,000 JE
	 * fill an empty Beam Pistol (eight rounds of 400), leaving them at 0 and 7,800. With every cell spent a reload does
	 * not start; a cell of 1,500 JE loads two of the Stormlock's 750 JE charges, one at a time, and is spent.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 200)
	public void reloadsDrawChargeFromCells(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob pig = GunsGameTests.pig(helper, new BlockPos(1, 2, 8));
		GunSpec pistol = JugcraftGuns.SPECS.get("beam_pistol");
		GunSpec rifle = JugcraftGuns.SPECS.get("stormlock_rifle");
		ServerPlayer shooter = GunsGameTests.shooter(helper, "beam_pistol", 0, pig, GameType.SURVIVAL);
		ItemStack low = cell(1_000);
		ItemStack full = cell(10_000);
		ItemStack spare = cell(0);
		shooter.getInventory().setItem(1, low);
		shooter.getInventory().setItem(2, full);
		shooter.getInventory().setItem(3, spare);
		GunItem gun = JugcraftGuns.GUNS.get("beam_pistol");
		helper.assertTrue(GunShots.stocked(shooter.getInventory(), gun, pistol) == 27, "The cells' 11,000 JE are not 27 rounds of 400");
		helper.assertTrue(GunShots.reload(shooter), "The empty Beam Pistol did not start to reload");
		int second = pistol.reload() + 2;
		helper.runAfterDelay(second, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == pistol.capacity(), "The Beam Pistol loaded "
					+ GunItem.loaded(shooter.getMainHandItem()) + " rounds, not " + pistol.capacity());
			helper.assertTrue(Chargeable.energy(low) == 0 && Chargeable.energy(full) == 7_800, "The reload left the cells at "
					+ Chargeable.energy(low) + " and " + Chargeable.energy(full) + " JE, not 0 and 7,800");
			helper.assertTrue(shooter.getInventory().getItem(1) == low && shooter.getInventory().getItem(2) == full,
					"The reload took the cells");
			Chargeable.setEnergy(full, 0);
			shooter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftGuns.GUNS.get("stormlock_rifle")));
			helper.assertFalse(GunShots.reload(shooter), "The Stormlock started to reload on spent cells");
			Chargeable.setEnergy(spare, 1_500);
			helper.assertTrue(GunShots.reload(shooter), "The empty Stormlock did not start to reload");
		});
		helper.runAfterDelay(second + rifle.reloadTicks(2) + 3, () -> {
			helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == 2, "The Stormlock loaded "
					+ GunItem.loaded(shooter.getMainHandItem()) + " charges from 1,500 JE, not two of 750");
			helper.assertTrue(Chargeable.energy(spare) == 0, "The Stormlock's reload left " + Chargeable.energy(spare) + " JE in the cell");
			helper.succeed();
		});
	}

	/**
	 * A Beam Pistol shot passes through the two pigs in its line, each taking the shot's damage, and stops at the wall:
	 * the pig behind it is untouched. The shot spends a round.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 40)
	public void beamPassesThroughCreatures(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob near = raised(helper, new BlockPos(1, 2, 4));
		Mob behind = raised(helper, new BlockPos(1, 2, 7));
		Mob walled = raised(helper, new BlockPos(1, 2, 12));
		for (int x = 0; x <= 3; x++) {
			for (int y = 2; y <= 4; y++) {
				helper.setBlock(new BlockPos(x, y, 10), Blocks.STONE);
			}
		}
		GunSpec spec = JugcraftGuns.SPECS.get("beam_pistol");
		ServerPlayer shooter = GunsGameTests.shooter(helper, "beam_pistol", spec.capacity(), near, GameType.SURVIVAL);
		helper.assertTrue(GunShots.fire(shooter), "The loaded Beam Pistol did not fire");
		helper.assertTrue(took(near, spec.damage()), "The beam took " + lost(near) + " from the near pig, not " + spec.damage());
		helper.assertTrue(took(behind, spec.damage()), "The beam took " + lost(behind) + " from the pig behind it, not " + spec.damage());
		helper.assertTrue(lost(walled) == 0.0F, "The beam passed the wall");
		helper.assertTrue(GunItem.loaded(shooter.getMainHandItem()) == spec.capacity() - 1, "The shot did not spend a round");
		helper.succeed();
	}

	/**
	 * A Stormlock shot strikes the pig it is aimed at, then leaps to the nearest pig two blocks off and from that one to
	 * the next, three blocks on, each taking its share of the one before; then it stops, so the pig nine blocks away
	 * is untouched.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 40)
	public void arcLeapsBetweenCreatures(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob mark = raised(helper, new BlockPos(1, 2, 6));
		Mob first = raised(helper, new BlockPos(3, 2, 6));
		Mob second = raised(helper, new BlockPos(3, 2, 9));
		Mob far = raised(helper, new BlockPos(12, 2, 6));
		GunSpec spec = JugcraftGuns.SPECS.get("stormlock_rifle");
		ServerPlayer shooter = GunsGameTests.shooter(helper, "stormlock_rifle", spec.capacity(), mark, GameType.SURVIVAL);
		helper.assertTrue(GunShots.fire(shooter), "The loaded Stormlock did not fire");
		float share = JugcraftGuns.ARC_SHARE;
		helper.assertTrue(took(mark, spec.damage()), "The arc took " + lost(mark) + " from its mark, not " + spec.damage());
		helper.assertTrue(took(first, spec.damage() * share), "The first leap took " + lost(first) + ", not " + spec.damage() * share);
		helper.assertTrue(took(second, spec.damage() * share * share),
				"The second leap took " + lost(second) + ", not " + spec.damage() * share * share);
		helper.assertTrue(lost(far) == 0.0F, "The arc reached the pig nine blocks away");
		helper.succeed();
	}

	/**
	 * The Linesman's arc finds the pig eight degrees off its aim (within its cone from the hip) and strikes it; a pig far
	 * off to the side, out of its cone and too far from the first to leap to, is untouched.
	 */
	@GameTest(structure = GunsGameTests.ARENA, maxTicks = 40)
	public void linesmanFindsCreaturesInItsCone(GameTestHelper helper) {
		GunsGameTests.floor(helper);
		Mob ahead = raised(helper, new BlockPos(1, 2, 7));
		Mob aside = raised(helper, new BlockPos(8, 2, 3));
		GunSpec spec = JugcraftGuns.SPECS.get("linesman");
		ServerPlayer shooter = GunsGameTests.shooter(helper, "linesman", spec.capacity(), ahead, GameType.SURVIVAL);
		shooter.setYRot(8.0F);
		shooter.setYHeadRot(8.0F);
		helper.assertTrue(8.0F < spec.hipSpread(), "The test's eight degrees are not inside the Linesman's cone");
		helper.assertTrue(GunShots.fire(shooter), "The loaded Linesman did not fire");
		helper.assertTrue(took(ahead, spec.damage()), "The arc took " + lost(ahead) + " from the pig off its aim, not " + spec.damage());
		helper.assertTrue(lost(aside) == 0.0F, "The arc reached the pig out of its cone");
		helper.succeed();
	}

	/** A Charging Station fills an Energy Cell on its cradle, as it does a powered tool. */
	@GameTest(maxTicks = 100)
	public void chargingStationFillsACell(GameTestHelper helper) {
		BlockPos lower = new BlockPos(2, 1, 2);
		BlockState state = JugcraftTools.CHARGING_STATION.defaultBlockState();
		helper.setBlock(lower, state);
		helper.setBlock(lower.above(), state.setValue(ChargingStationBlock.HALF, DoubleBlockHalf.UPPER));
		EnergyStorage storage = EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(lower.above()), Direction.WEST);
		helper.assertTrue(storage instanceof SimpleEnergyStorage, "The Charging Station takes no energy from the west");
		((SimpleEnergyStorage) storage).setAmount(storage.getCapacity());
		ChargingStationBlockEntity station = helper.getBlockEntity(lower, ChargingStationBlockEntity.class);
		station.setTool(new ItemStack(JugcraftGuns.ENERGY_CELL));
		helper.succeedWhen(() -> {
			long energy = Chargeable.energy(station.tool());
			helper.assertTrue(energy >= ChargingStationBlockEntity.CHARGE_RATE * 10L, "The cell holds only " + energy + " JE");
		});
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
