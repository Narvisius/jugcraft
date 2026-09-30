package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.energy.EnergyNetworks;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.fluid.ElectricPumpBlockEntity;
import io.github.jimbozoomer.jugcraft.fluid.FluidTankBlockEntity;
import io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids;
import io.github.jimbozoomer.jugcraft.kinetic.BeltPulleyBlockEntity;
import io.github.jimbozoomer.jugcraft.kinetic.DynamoBlockEntity;
import io.github.jimbozoomer.jugcraft.kinetic.ElectricMotorBlock;
import io.github.jimbozoomer.jugcraft.kinetic.ElectricMotorBlockEntity;
import io.github.jimbozoomer.jugcraft.kinetic.HandCrankBlock;
import io.github.jimbozoomer.jugcraft.kinetic.HandCrankBlockEntity;
import io.github.jimbozoomer.jugcraft.kinetic.JugcraftKinetics;
import io.github.jimbozoomer.jugcraft.kinetic.ShaftBlock;
import io.github.jimbozoomer.jugcraft.kinetic.SteamEngineBlock;
import io.github.jimbozoomer.jugcraft.logistics.ConveyorBlock;
import io.github.jimbozoomer.jugcraft.logistics.ConveyorBlockEntity;
import io.github.jimbozoomer.jugcraft.logistics.ItemSorterBlockEntity;
import io.github.jimbozoomer.jugcraft.logistics.JugcraftLogistics;
import io.github.jimbozoomer.jugcraft.logistics.PneumaticExtractorBlock;
import io.github.jimbozoomer.jugcraft.machine.Footprint;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.machine.LargeMachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.machine.MachineRecipe;
import io.github.jimbozoomer.jugcraft.machine.MachineRecipes;
import io.github.jimbozoomer.jugcraft.machine.MachineUpgrades;
import io.github.jimbozoomer.jugcraft.machine.SideConfig;
import io.github.jimbozoomer.jugcraft.prospecting.OreSurvey;
import io.github.jimbozoomer.jugcraft.storage.JugcraftStorage;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * In-game tests run by `./gradlew build` on a headless server (Fabric game test API).
 * Each builds a small setup in an empty test area and waits for the expected result.
 */
public class JugcraftGameTests {
	private static Item item(String path) {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id(path));
	}

	private static void charge(GameTestHelper helper, BlockPos relative, Direction side) {
		EnergyStorage storage = EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(relative), side);
		helper.assertTrue(storage instanceof SimpleEnergyStorage, "No energy storage at " + relative + " on " + side);
		((SimpleEnergyStorage) storage).setAmount(storage.getCapacity());
	}

	private static BlockState machine(MachineKind kind) {
		return JugcraftMachines.MACHINES.get(kind).defaultBlockState();
	}

	/** Recipes are data-driven: the crushing recipe for tin ore loads from data/jugcraft/recipe/crushing. */
	@GameTest
	public void crushingRecipesLoad(GameTestHelper helper) {
		MachineRecipe recipe = MachineRecipes.find(helper.getLevel(), MachineKind.CRUSHER, new ItemStack(item("tin_ore")))
				.orElseThrow(() -> helper.assertionException("No crushing recipe for tin ore"));
		ItemStack output = recipe.output().create();
		helper.assertTrue(output.is(item("raw_tin")) && output.getCount() == 2, "Tin ore should crush into 2 raw tin, got " + output);
		helper.succeed();
	}

	/** A powered crusher turns one ore into two raw ore. */
	@GameTest(maxTicks = 400)
	public void crusherDoublesOre(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 1, 2);
		helper.setBlock(pos, machine(MachineKind.CRUSHER));
		charge(helper, pos, Direction.UP);
		MachineBlockEntity crusher = helper.getBlockEntity(pos, MachineBlockEntity.class);
		crusher.setItem(0, new ItemStack(item("tin_ore")));
		helper.succeedWhen(() -> {
			ItemStack output = crusher.getItem(MachineKind.CRUSHER.outputSlot());
			helper.assertTrue(output.is(item("raw_tin")) && output.getCount() == 2, "Crusher output is " + output);
		});
	}

	/** Coal generator -> copper cables -> electric furnace smelts raw iron. */
	@GameTest(maxTicks = 600)
	public void cablesCarryPower(GameTestHelper helper) {
		BlockPos generator = new BlockPos(1, 1, 3);
		BlockPos furnace = new BlockPos(5, 1, 3);
		helper.setBlock(generator, machine(MachineKind.COAL_GENERATOR));
		for (int x = 2; x <= 4; x++) {
			helper.setBlock(new BlockPos(x, 1, 3), JugcraftMachines.COPPER_CABLE);
		}
		helper.setBlock(furnace, machine(MachineKind.ELECTRIC_FURNACE));
		helper.getBlockEntity(generator, MachineBlockEntity.class).setItem(0, new ItemStack(Items.COAL));
		MachineBlockEntity electricFurnace = helper.getBlockEntity(furnace, MachineBlockEntity.class);
		electricFurnace.setItem(0, new ItemStack(Items.RAW_IRON));
		helper.succeedWhen(() -> {
			ItemStack output = electricFurnace.getItem(MachineKind.ELECTRIC_FURNACE.outputSlot());
			helper.assertTrue(output.is(Items.IRON_INGOT), "Electric furnace output is " + output);
		});
	}

	/** An electric pump on water pushes it through bronze pipes into a tank. */
	@GameTest(maxTicks = 200)
	public void pumpFillsTankThroughPipes(GameTestHelper helper) {
		helper.setBlock(new BlockPos(1, 1, 3), Blocks.WATER);
		BlockPos pump = new BlockPos(1, 2, 3);
		helper.setBlock(pump, JugcraftFluids.ELECTRIC_PUMP);
		helper.getBlockEntity(pump, ElectricPumpBlockEntity.class).energy().setAmount(ElectricPumpBlockEntity.ENERGY_CAPACITY);
		for (int x = 2; x <= 3; x++) {
			helper.setBlock(new BlockPos(x, 2, 3), JugcraftFluids.BRONZE_FLUID_PIPE);
		}
		BlockPos tank = new BlockPos(4, 2, 3);
		helper.setBlock(tank, JugcraftFluids.FLUID_TANK);
		FluidTankBlockEntity tankEntity = helper.getBlockEntity(tank, FluidTankBlockEntity.class);
		helper.succeedWhen(() -> helper.assertTrue(tankEntity.storage.amount > 0, "Tank is still empty"));
	}

	/** The 3x2x6 alloy smelter: places all 36 blocks, takes power only at its socket, makes bronze. */
	@GameTest(maxTicks = 400)
	public void alloySmelterMakesBronze(GameTestHelper helper) {
		BlockPos master = new BlockPos(6, 1, 3);
		LargeMachineBlock block = (LargeMachineBlock) JugcraftMachines.MACHINES.get(MachineKind.ALLOY_SMELTER);
		BlockState state = block.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH);
		helper.setBlock(master, state);
		block.setPlacedBy(helper.getLevel(), helper.absolutePos(master), helper.getBlockState(master), null, ItemStack.EMPTY);
		Footprint footprint = MachineKind.ALLOY_SMELTER.footprint();
		for (int part = 0; part < footprint.size(); part++) {
			BlockPos at = footprint.partPos(helper.absolutePos(master), Direction.NORTH, part);
			helper.assertTrue(helper.getLevel().getBlockState(at).is(block), "Part " + part + " is missing");
		}
		helper.assertTrue(EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(master), Direction.NORTH) == null,
				"The alloy smelter must not take power at its front");
		// The socket is on the outer (west) side of the lower right front block, two to the right of the master.
		charge(helper, master.west(2), Direction.WEST);
		MachineBlockEntity smelter = helper.getBlockEntity(master, MachineBlockEntity.class);
		smelter.setItem(0, new ItemStack(Items.COPPER_INGOT, 3));
		smelter.setItem(1, new ItemStack(item("tin_ingot")));
		helper.succeedWhen(() -> {
			ItemStack output = smelter.getItem(MachineKind.ALLOY_SMELTER.outputSlot());
			helper.assertTrue(output.is(item("bronze_ingot")) && output.getCount() == 4, "Alloy smelter output is " + output);
		});
	}

	/** Breaking any block of a multi-block machine removes the whole machine (here the nine-block wind turbine). */
	@GameTest(maxTicks = 40)
	public void breakingOnePartRemovesTheMachine(GameTestHelper helper) {
		BlockPos base = new BlockPos(3, 1, 3);
		LargeMachineBlock block = (LargeMachineBlock) JugcraftMachines.MACHINES.get(MachineKind.WIND_TURBINE);
		helper.setBlock(base, block.defaultBlockState());
		block.setPlacedBy(helper.getLevel(), helper.absolutePos(base), helper.getBlockState(base), null, ItemStack.EMPTY);
		helper.assertBlockPresent(block, base.above(8));
		helper.destroyBlock(base.above(4));
		helper.succeedWhen(() -> {
			for (int y = 0; y < 9; y++) {
				helper.assertBlockNotPresent(block, base.above(y));
			}
		});
	}

	private static int count(ChestBlockEntity chest, Item item) {
		int total = 0;
		for (int slot = 0; slot < chest.getContainerSize(); slot++) {
			ItemStack stack = chest.getItem(slot);
			if (stack.is(item)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	private static ChestBlockEntity chest(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, Blocks.CHEST);
		return helper.getBlockEntity(pos, ChestBlockEntity.class);
	}

	private static void extractor(GameTestHelper helper, BlockPos pos, Direction intake) {
		helper.setBlock(pos, JugcraftLogistics.PNEUMATIC_EXTRACTOR.defaultBlockState().setValue(PneumaticExtractorBlock.FACING, intake));
	}

	/** Chest -> extractor -> brass item pipes -> chest. */
	@GameTest(maxTicks = 200)
	public void extractorMovesItemsThroughPipes(GameTestHelper helper) {
		ChestBlockEntity source = chest(helper, new BlockPos(1, 1, 3));
		source.setItem(0, new ItemStack(Items.IRON_INGOT, 5));
		extractor(helper, new BlockPos(2, 1, 3), Direction.WEST);
		for (int x = 3; x <= 4; x++) {
			helper.setBlock(new BlockPos(x, 1, 3), JugcraftLogistics.BRASS_ITEM_PIPE);
		}
		ChestBlockEntity target = chest(helper, new BlockPos(5, 1, 3));
		helper.succeedWhen(() -> helper.assertTrue(count(target, Items.IRON_INGOT) == 5,
				"Target chest has " + count(target, Items.IRON_INGOT) + " iron"));
	}

	/** A sorter set to iron takes the iron; everything else goes to the plain chest on the same pipes. */
	@GameTest(maxTicks = 300)
	public void sorterRoutesMatchingItems(GameTestHelper helper) {
		ChestBlockEntity source = chest(helper, new BlockPos(1, 1, 1));
		source.setItem(0, new ItemStack(Items.IRON_INGOT, 4));
		source.setItem(1, new ItemStack(Items.GOLD_INGOT, 4));
		extractor(helper, new BlockPos(2, 1, 1), Direction.WEST);
		for (int x = 3; x <= 5; x++) {
			helper.setBlock(new BlockPos(x, 1, 1), JugcraftLogistics.BRASS_ITEM_PIPE);
		}
		BlockPos sorter = new BlockPos(4, 1, 2);
		helper.setBlock(sorter, JugcraftLogistics.ITEM_SORTER.defaultBlockState().setValue(PneumaticExtractorBlock.FACING, Direction.SOUTH));
		helper.getBlockEntity(sorter, ItemSorterBlockEntity.class).setItem(0, new ItemStack(Items.IRON_INGOT));
		ChestBlockEntity sorted = chest(helper, new BlockPos(4, 1, 3));
		ChestBlockEntity overflow = chest(helper, new BlockPos(6, 1, 1));
		helper.succeedWhen(() -> {
			helper.assertTrue(count(sorted, Items.IRON_INGOT) == 4 && count(sorted, Items.GOLD_INGOT) == 0,
					"Sorted chest: " + count(sorted, Items.IRON_INGOT) + " iron, " + count(sorted, Items.GOLD_INGOT) + " gold");
			helper.assertTrue(count(overflow, Items.GOLD_INGOT) == 4 && count(overflow, Items.IRON_INGOT) == 0,
					"Overflow chest: " + count(overflow, Items.GOLD_INGOT) + " gold, " + count(overflow, Items.IRON_INGOT) + " iron");
		});
	}

	/** A crusher set to eject pushes its results into the chest below (its default output face). */
	@GameTest(maxTicks = 400)
	public void machineEjectsIntoChest(GameTestHelper helper) {
		ChestBlockEntity below = chest(helper, new BlockPos(2, 1, 2));
		BlockPos pos = new BlockPos(2, 2, 2);
		helper.setBlock(pos, machine(MachineKind.CRUSHER));
		charge(helper, pos, Direction.UP);
		MachineBlockEntity crusher = helper.getBlockEntity(pos, MachineBlockEntity.class);
		helper.assertTrue(crusher.clickSideButton(SideConfig.EJECT_BUTTON), "Eject button was refused");
		crusher.setItem(0, new ItemStack(item("tin_ore")));
		helper.succeedWhen(() -> helper.assertTrue(count(below, item("raw_tin")) == 2,
				"Chest below has " + count(below, item("raw_tin")) + " raw tin"));
	}

	// ------------------------------------------------------------------ ore processing

	/** A powered single-input machine processes one item; returns the machine for checking. */
	private static MachineBlockEntity processing(GameTestHelper helper, BlockPos pos, MachineKind kind, ItemStack input) {
		helper.setBlock(pos, machine(kind));
		charge(helper, pos, Direction.UP);
		MachineBlockEntity machine = helper.getBlockEntity(pos, MachineBlockEntity.class);
		machine.setItem(0, input);
		return machine;
	}

	/** Byproducts load from data: pulverizing tin ore sometimes yields tungsten dust. */
	@GameTest
	public void pulverizerByproductsLoad(GameTestHelper helper) {
		MachineRecipe recipe = MachineRecipes.find(helper.getLevel(), MachineKind.PULVERIZER, new ItemStack(item("tin_ore")))
				.orElseThrow(() -> helper.assertionException("No pulverizing recipe for tin ore"));
		helper.assertTrue(recipe.byproducts().size() == 1, "Expected one byproduct, got " + recipe.byproducts());
		MachineRecipe.Byproduct byproduct = recipe.byproducts().getFirst();
		helper.assertTrue(byproduct.result().create().is(item("tungsten_dust")) && byproduct.chance() > 0.0F,
				"Tin ore byproduct is " + byproduct);
		helper.succeed();
	}

	/** The pulverizer grinds one ore into two dusts. */
	@GameTest(maxTicks = 400)
	public void pulverizerGrindsOre(GameTestHelper helper) {
		MachineBlockEntity pulverizer = processing(helper, new BlockPos(2, 1, 2), MachineKind.PULVERIZER, new ItemStack(item("tin_ore")));
		helper.succeedWhen(() -> {
			ItemStack output = pulverizer.getItem(MachineKind.PULVERIZER.outputSlot());
			helper.assertTrue(output.is(item("tin_dust")) && output.getCount() == 2, "Pulverizer output is " + output);
		});
	}

	/** The ore washer, fed by a water source below, turns one ore into three washed ores. */
	@GameTest(maxTicks = 500)
	public void oreWasherTriplesOre(GameTestHelper helper) {
		helper.setBlock(new BlockPos(2, 1, 2), Blocks.WATER);
		MachineBlockEntity washer = processing(helper, new BlockPos(2, 2, 2), MachineKind.ORE_WASHER, new ItemStack(item("tin_ore")));
		helper.succeedWhen(() -> {
			ItemStack output = washer.getItem(MachineKind.ORE_WASHER.outputSlot());
			helper.assertTrue(output.is(item("washed_tin_ore")) && output.getCount() == 3, "Ore washer output is " + output);
		});
	}

	/** Without water the ore washer waits instead of washing. */
	@GameTest(maxTicks = 300)
	public void oreWasherNeedsWater(GameTestHelper helper) {
		MachineBlockEntity washer = processing(helper, new BlockPos(2, 1, 2), MachineKind.ORE_WASHER, new ItemStack(item("tin_ore")));
		helper.runAtTickTime(260, () -> {
			helper.assertTrue(washer.getItem(MachineKind.ORE_WASHER.outputSlot()).isEmpty(), "A dry ore washer made something");
			helper.succeed();
		});
	}

	/** Dust smelts back into an ingot in the electric furnace. */
	@GameTest(maxTicks = 300)
	public void dustSmeltsIntoIngot(GameTestHelper helper) {
		MachineBlockEntity furnace = processing(helper, new BlockPos(2, 1, 2), MachineKind.ELECTRIC_FURNACE, new ItemStack(item("tin_dust")));
		helper.succeedWhen(() -> {
			ItemStack output = furnace.getItem(MachineKind.ELECTRIC_FURNACE.outputSlot());
			helper.assertTrue(output.is(item("tin_ingot")), "Electric furnace output is " + output);
		});
	}

	/** The sawmill cuts a log into six planks. */
	@GameTest(maxTicks = 300)
	public void sawmillCutsLogs(GameTestHelper helper) {
		MachineBlockEntity sawmill = processing(helper, new BlockPos(2, 1, 2), MachineKind.SAWMILL, new ItemStack(Items.OAK_LOG));
		helper.succeedWhen(() -> {
			ItemStack output = sawmill.getItem(MachineKind.SAWMILL.outputSlot());
			helper.assertTrue(output.is(Items.OAK_PLANKS) && output.getCount() == 6, "Sawmill output is " + output);
		});
	}

	/** The sieve sifts gravel into flint. */
	@GameTest(maxTicks = 300)
	public void sieveSiftsGravel(GameTestHelper helper) {
		MachineBlockEntity sieve = processing(helper, new BlockPos(2, 1, 2), MachineKind.SIEVE, new ItemStack(Items.GRAVEL));
		helper.succeedWhen(() -> {
			ItemStack output = sieve.getItem(MachineKind.SIEVE.outputSlot());
			helper.assertTrue(output.is(Items.FLINT), "Sieve output is " + output);
		});
	}

	// ------------------------------------------------------------------ steel tier

	/** Places a multi-block machine facing north and builds all its parts, as a player placing it would. */
	private static MachineBlockEntity large(GameTestHelper helper, BlockPos master, MachineKind kind) {
		LargeMachineBlock block = (LargeMachineBlock) JugcraftMachines.MACHINES.get(kind);
		helper.setBlock(master, block.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
		block.setPlacedBy(helper.getLevel(), helper.absolutePos(master), helper.getBlockState(master), null, ItemStack.EMPTY);
		return helper.getBlockEntity(master, MachineBlockEntity.class);
	}

	/** The coke oven bakes coal into coke with no power at all. */
	@GameTest(maxTicks = 800)
	public void cokeOvenBakesCoke(GameTestHelper helper) {
		MachineBlockEntity oven = large(helper, new BlockPos(2, 1, 2), MachineKind.COKE_OVEN);
		oven.setItem(0, new ItemStack(Items.COAL));
		helper.succeedWhen(() -> {
			ItemStack output = oven.getItem(MachineKind.COKE_OVEN.outputSlot());
			helper.assertTrue(output.is(item("coke")), "Coke oven output is " + output);
		});
	}

	/** The steel foundry refines one iron ingot with one coke into one steel ingot, unpowered. */
	@GameTest(maxTicks = 600)
	public void steelFoundryMakesSteel(GameTestHelper helper) {
		MachineBlockEntity foundry = large(helper, new BlockPos(2, 1, 2), MachineKind.STEEL_FOUNDRY);
		foundry.setItem(0, new ItemStack(Items.IRON_INGOT));
		foundry.setItem(1, new ItemStack(item("coke")));
		helper.succeedWhen(() -> {
			ItemStack output = foundry.getItem(MachineKind.STEEL_FOUNDRY.outputSlot());
			helper.assertTrue(output.is(item("steel_ingot")), "Steel foundry output is " + output);
		});
	}

	/** Cables draw a connection only where power really goes in: never to unpowered machines, only to a socket. */
	@GameTest(maxTicks = 20)
	public void cablesConnectOnlyWherePowerGoesIn(GameTestHelper helper) {
		// Cables first, so placing the machines updates their connections.
		BlockPos besideOven = new BlockPos(3, 1, 1);
		BlockPos besideFront = new BlockPos(7, 1, 2);
		BlockPos besideSocket = new BlockPos(4, 1, 3);
		for (BlockPos cable : new BlockPos[] {besideOven, besideFront, besideSocket}) {
			helper.setBlock(cable, JugcraftMachines.COPPER_CABLE);
		}
		// The 2x2 coke oven fills x 1..2, z 1..2 from its master at (2,1,1).
		large(helper, new BlockPos(2, 1, 1), MachineKind.COKE_OVEN);
		// Alloy smelter at (7,1,3), 3 wide to the west: its front faces north towards (7,1,2); its socket block is
		// (5,1,3), with the socket facing west towards the cable at (4,1,3).
		large(helper, new BlockPos(7, 1, 3), MachineKind.ALLOY_SMELTER);
		helper.assertTrue(EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 1)), Direction.WEST) == null,
				"An unpowered machine must not expose energy");
		helper.assertTrue(!helper.getBlockState(besideOven).getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(Direction.WEST)),
				"A cable must not connect to the coke oven");
		helper.assertTrue(!helper.getBlockState(besideFront).getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(Direction.SOUTH)),
				"A cable must not connect to the alloy smelter's front");
		helper.assertTrue(helper.getBlockState(besideSocket).getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(Direction.EAST)),
				"A cable must connect to the alloy smelter's power socket");
		helper.succeed();
	}

	// ------------------------------------------------------------------ machine control

	/** Four speed upgrades make the crusher three times as fast (160 ticks -> 54). */
	@GameTest(maxTicks = 120)
	public void speedUpgradesShortenProcessing(GameTestHelper helper) {
		MachineBlockEntity crusher = processing(helper, new BlockPos(2, 1, 2), MachineKind.CRUSHER, new ItemStack(item("tin_ore")));
		crusher.setItem(MachineKind.CRUSHER.slots, new ItemStack(MachineUpgrades.SPEED, 4));
		helper.runAtTickTime(80, () -> {
			ItemStack output = crusher.getItem(MachineKind.CRUSHER.outputSlot());
			helper.assertTrue(output.is(item("raw_tin")), "An upgraded crusher should be done by tick 80, output is " + output);
			helper.succeed();
		});
	}

	/** Four efficiency upgrades cut the energy one operation uses to about 41%. */
	@GameTest(maxTicks = 400)
	public void efficiencyUpgradesSaveEnergy(GameTestHelper helper) {
		MachineBlockEntity crusher = processing(helper, new BlockPos(2, 1, 2), MachineKind.CRUSHER, new ItemStack(item("tin_ore")));
		crusher.setItem(MachineKind.CRUSHER.slots + 1, new ItemStack(MachineUpgrades.EFFICIENCY, 4));
		EnergyStorage energy = EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2)), Direction.UP);
		long start = energy.getAmount();
		helper.succeedWhen(() -> {
			helper.assertTrue(!crusher.getItem(MachineKind.CRUSHER.outputSlot()).isEmpty(), "Not done yet");
			long used = start - energy.getAmount();
			// Unupgraded: 16 JE/t x 160 ticks = 2,560 JE. Upgraded: 7 JE/t x 160 = 1,120 JE.
			helper.assertTrue(used <= 1_200, "Used " + used + " JE for one operation");
		});
	}

	/** In "high" redstone mode a machine waits until it receives a signal. */
	@GameTest(maxTicks = 500)
	public void redstoneHighWaitsForSignal(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 1, 2);
		MachineBlockEntity crusher = processing(helper, pos, MachineKind.CRUSHER, new ItemStack(item("tin_ore")));
		crusher.clickSideButton(SideConfig.REDSTONE_BUTTON);
		helper.assertTrue(crusher.sides().redstone() == SideConfig.Redstone.HIGH, "One click should select HIGH");
		helper.runAtTickTime(200, () -> {
			helper.assertTrue(crusher.getItem(MachineKind.CRUSHER.outputSlot()).isEmpty(), "Ran without a redstone signal");
			helper.setBlock(pos.east(), Blocks.REDSTONE_BLOCK);
		});
		helper.succeedWhen(() -> {
			ItemStack output = crusher.getItem(MachineKind.CRUSHER.outputSlot());
			helper.assertTrue(output.is(item("raw_tin")), "Crusher output is " + output);
		});
	}

	/** Comparators read a battery's charge; upgrade slots are never offered to hoppers or pipes. */
	@GameTest
	public void comparatorAndUpgradeSlots(GameTestHelper helper) {
		BlockPos batteryPos = new BlockPos(1, 1, 1);
		helper.setBlock(batteryPos, machine(MachineKind.BATTERY_BOX));
		MachineBlockEntity battery = helper.getBlockEntity(batteryPos, MachineBlockEntity.class);
		helper.assertTrue(battery.comparatorSignal() == 0, "Empty battery signal is " + battery.comparatorSignal());
		charge(helper, batteryPos, null); // The battery box's sided storages are wrappers; the unsided one is the real battery.
		helper.assertTrue(battery.comparatorSignal() == 15, "Full battery signal is " + battery.comparatorSignal());

		BlockPos crusherPos = new BlockPos(3, 1, 3);
		helper.setBlock(crusherPos, machine(MachineKind.CRUSHER));
		MachineBlockEntity crusher = helper.getBlockEntity(crusherPos, MachineBlockEntity.class);
		for (Direction side : Direction.values()) {
			for (int slot : crusher.getSlotsForFace(side)) {
				helper.assertTrue(slot < MachineKind.CRUSHER.slots, "Upgrade slot " + slot + " exposed on " + side);
			}
		}
		helper.assertTrue(!crusher.canPlaceItem(MachineKind.CRUSHER.slots, new ItemStack(Items.COBBLESTONE)),
				"Upgrade slots must only take upgrades");
		helper.succeed();
	}

	// ------------------------------------------------------------------ transmitter tiers

	/** Pushes once from a large source through the given cables into an arc furnace; returns the JE moved. */
	private static long pushThrough(GameTestHelper helper, int z, net.minecraft.world.level.block.Block... cables) {
		for (int i = 0; i < cables.length; i++) {
			helper.setBlock(new BlockPos(2 + i, 1, z), cables[i]);
		}
		BlockPos furnace = new BlockPos(2 + cables.length, 1, z);
		helper.setBlock(furnace, machine(MachineKind.ARC_FURNACE));
		SimpleEnergyStorage source = new SimpleEnergyStorage(100_000, 0, 100_000, () -> {
		});
		source.setAmount(100_000);
		return EnergyNetworks.pushToNeighbors(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, z)), source, 100_000,
				java.util.List.of(Direction.EAST));
	}

	/** Silver cable carries 1,024 JE/t, copper 256; a mixed network runs at its slowest cable. */
	@GameTest
	public void cableTiersSetTheRate(GameTestHelper helper) {
		long silver = pushThrough(helper, 1, JugcraftMachines.SILVER_CABLE, JugcraftMachines.SILVER_CABLE);
		long copper = pushThrough(helper, 3, JugcraftMachines.COPPER_CABLE, JugcraftMachines.COPPER_CABLE);
		long mixed = pushThrough(helper, 5, JugcraftMachines.SILVER_CABLE, JugcraftMachines.COPPER_CABLE);
		helper.assertTrue(silver > 256 && silver <= 1_024, "Silver network moved " + silver);
		helper.assertTrue(copper == 256, "Copper network moved " + copper);
		helper.assertTrue(mixed == 256, "Mixed network moved " + mixed);
		helper.succeed();
	}

	/** The high-pressure extractor moves 32 items every 4 ticks; by tick 10 the brass one could move at most 16. */
	@GameTest(maxTicks = 40)
	public void highPressureExtractorIsFaster(GameTestHelper helper) {
		ChestBlockEntity source = chest(helper, new BlockPos(1, 1, 2));
		source.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
		helper.setBlock(new BlockPos(2, 1, 2), JugcraftLogistics.HIGH_PRESSURE_EXTRACTOR.defaultBlockState()
				.setValue(PneumaticExtractorBlock.FACING, Direction.WEST));
		ChestBlockEntity target = chest(helper, new BlockPos(3, 1, 2));
		helper.runAtTickTime(10, () -> {
			int moved = count(target, Items.COBBLESTONE);
			helper.assertTrue(moved >= 32, "High-pressure extractor moved " + moved + " by tick 10");
			helper.succeed();
		});
	}

	// ------------------------------------------------------------------ storage

	/** The 2x2 capacitor bank holds 4,000,000 JE, takes power at its sides and gives it out of its front. */
	@GameTest
	public void capacitorBankOutputsFromItsFront(GameTestHelper helper) {
		BlockPos master = new BlockPos(4, 1, 3);
		large(helper, master, MachineKind.CAPACITOR_BANK);
		EnergyStorage front = EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(master.above()), Direction.NORTH);
		EnergyStorage side = EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(master.west()), Direction.WEST);
		helper.assertTrue(front != null && front.supportsExtraction() && !front.supportsInsertion(), "Front must only give power");
		helper.assertTrue(side != null && side.supportsInsertion() && !side.supportsExtraction(), "Sides must only take power");
		helper.assertTrue(front.getCapacity() == 4_000_000, "Capacity is " + front.getCapacity());
		helper.succeed();
	}

	/** The steel tank holds exactly 128 buckets of one fluid. */
	@GameTest
	public void steelTankHolds128Buckets(GameTestHelper helper) {
		BlockPos master = new BlockPos(4, 1, 2);
		large(helper, master, MachineKind.STEEL_TANK);
		Storage<FluidVariant> tank = FluidStorage.SIDED.find(helper.getLevel(), helper.absolutePos(master.south()), Direction.UP);
		helper.assertTrue(tank != null, "No fluid storage on the tank's back block");
		try (Transaction transaction = Transaction.openOuter()) {
			long inserted = tank.insert(FluidVariant.of(Fluids.WATER), 200 * FluidConstants.BUCKET, transaction);
			helper.assertTrue(inserted == 128 * FluidConstants.BUCKET, "Inserted " + inserted / FluidConstants.BUCKET + " buckets");
			long lava = tank.insert(FluidVariant.of(Fluids.LAVA), FluidConstants.BUCKET, transaction);
			helper.assertTrue(lava == 0, "A water tank accepted lava");
			transaction.commit();
		}
		helper.succeed();
	}

	/** A crate holds 32 stacks of one item type, refuses others, and shows its fill on a comparator. */
	@GameTest
	public void crateHoldsOneItemType(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 1, 2);
		helper.setBlock(pos, JugcraftStorage.ITEM_CRATE);
		Storage<ItemVariant> crate = ItemStorage.SIDED.find(helper.getLevel(), helper.absolutePos(pos), Direction.UP);
		try (Transaction transaction = Transaction.openOuter()) {
			long cobble = crate.insert(ItemVariant.of(Items.COBBLESTONE), 5_000, transaction);
			long dirt = crate.insert(ItemVariant.of(Items.DIRT), 10, transaction);
			helper.assertTrue(cobble == 32 * 64, "Crate took " + cobble + " cobblestone");
			helper.assertTrue(dirt == 0, "Crate took a second item type");
			transaction.commit();
		}
		helper.assertTrue(StorageUtil.getRedstoneSignal(crate) == 15, "A full crate should signal 15");
		helper.succeed();
	}

	// ------------------------------------------------------------------ mining & prospecting

	/** The prospector's survey reports ore placed nearby, with a signal of 1-5 and a depth band, and no positions. */
	@GameTest
	public void surveyFindsNearbyOre(GameTestHelper helper) {
		for (int x = 1; x <= 4; x++) {
			for (int z = 1; z <= 4; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.COPPER_ORE);
			}
		}
		List<OreSurvey.Reading> readings = OreSurvey.survey(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 2)),
				helper.getLevel().getRandom());
		OreSurvey.Reading copper = readings.stream()
				.filter(reading -> reading.icon().equals(Identifier.parse("minecraft:copper_ore"))).findFirst().orElse(null);
		helper.assertTrue(copper != null, "No copper reading in " + readings);
		helper.assertTrue(copper.signal() >= 1 && copper.signal() <= 5, "Signal out of range: " + copper.signal());
		helper.assertTrue(copper.depth() >= 0 && copper.depth() <= 2, "Depth band out of range: " + copper.depth());
		helper.succeed();
	}

	/**
	 * The ore drill mines ore blocks in the layer below it, puts them in its result slots, fills stone ore
	 * holes with stone and deepslate ore holes with deepslate, and leaves other blocks alone.
	 */
	@GameTest(maxTicks = 400)
	public void oreDrillMinesOreBelow(GameTestHelper helper) {
		BlockPos master = new BlockPos(4, 2, 4);
		BlockPos iron = new BlockPos(5, 1, 3);
		BlockPos deepIron = new BlockPos(2, 1, 6);
		BlockPos dirt = new BlockPos(4, 1, 4);
		helper.setBlock(iron, Blocks.IRON_ORE);
		helper.setBlock(deepIron, Blocks.DEEPSLATE_IRON_ORE);
		helper.setBlock(dirt, Blocks.DIRT);
		MachineBlockEntity drill = large(helper, master, MachineKind.ORE_DRILL);
		charge(helper, master, Direction.EAST);
		helper.succeedWhen(() -> {
			helper.assertBlockPresent(Blocks.STONE, iron);
			helper.assertBlockPresent(Blocks.DEEPSLATE, deepIron);
			helper.assertBlockPresent(Blocks.DIRT, dirt);
			int ore = 0;
			int deep = 0;
			for (int slot = 0; slot < MachineKind.ORE_DRILL.slots; slot++) {
				ItemStack stack = drill.getItem(slot);
				ore += stack.is(Items.IRON_ORE) ? stack.getCount() : 0;
				deep += stack.is(Items.DEEPSLATE_IRON_ORE) ? stack.getCount() : 0;
			}
			helper.assertTrue(ore == 1 && deep == 1, "Drill holds " + ore + " iron ore and " + deep + " deepslate iron ore");
		});
	}

	// ------------------------------------------------------------------ renewables

	/**
	 * A cobblestone generator with water below it and lava above it (walled in so neither can spread) makes
	 * cobblestone; one with only water makes none.
	 */
	@GameTest(maxTicks = 200)
	public void cobblestoneGeneratorNeedsWaterAndLava(GameTestHelper helper) {
		BlockPos both = new BlockPos(2, 1, 2);
		BlockPos waterOnly = new BlockPos(6, 1, 2);
		for (BlockPos pos : List.of(both, waterOnly)) {
			helper.setBlock(pos, machine(MachineKind.COBBLESTONE_GENERATOR));
			charge(helper, pos, Direction.NORTH);
			helper.setBlock(pos.below(), Blocks.WATER);
		}
		for (Direction side : Direction.Plane.HORIZONTAL) {
			helper.setBlock(both.above().relative(side), Blocks.GLASS);
		}
		helper.setBlock(both.above(2), Blocks.GLASS);
		helper.setBlock(both.above(), Blocks.LAVA);
		MachineBlockEntity generator = helper.getBlockEntity(both, MachineBlockEntity.class);
		MachineBlockEntity idle = helper.getBlockEntity(waterOnly, MachineBlockEntity.class);
		helper.runAtTickTime(150, () -> {
			ItemStack made = generator.getItem(MachineKind.COBBLESTONE_GENERATOR.outputSlot());
			helper.assertTrue(made.is(Items.COBBLESTONE) && made.getCount() >= 3, "Generator made " + made);
			helper.assertTrue(idle.getItem(0).isEmpty(), "A generator without lava made " + idle.getItem(0));
			helper.succeed();
		});
	}

	/** The tree farm grows an oak sapling into six oak logs and gives the sapling back. */
	@GameTest(maxTicks = 600)
	public void treeFarmGrowsLogs(GameTestHelper helper) {
		MachineBlockEntity farm = processing(helper, new BlockPos(2, 1, 2), MachineKind.TREE_FARM, new ItemStack(Items.OAK_SAPLING));
		helper.succeedWhen(() -> {
			ItemStack logs = farm.getItem(MachineKind.TREE_FARM.outputSlot());
			helper.assertTrue(logs.is(Items.OAK_LOG) && logs.getCount() == 6, "Tree farm output is " + logs);
			ItemStack sapling = farm.getItem(MachineKind.TREE_FARM.outputSlot() + 1);
			helper.assertTrue(sapling.is(Items.OAK_SAPLING), "The sapling did not come back: " + sapling);
		});
	}

	/** Water falling past the wheel side of a water wheel turns it; the wheel stores power. */
	@GameTest(maxTicks = 200)
	public void waterWheelTurnsInFlowingWater(GameTestHelper helper) {
		BlockPos master = new BlockPos(3, 1, 2);
		MachineBlockEntity wheel = large(helper, master, MachineKind.WATER_WHEEL);
		// Facing north, the wheel is on the west side. A source above that column falls past both blocks.
		helper.setBlock(master.west().above(2), Blocks.WATER);
		helper.succeedWhen(() -> {
			long stored = wheel.energyFor(null).getAmount();
			helper.assertTrue(stored >= 1_000, "Water wheel stored only " + stored + " JE");
		});
	}

	// ------------------------------------------------------------------ kinetic power

	/** A steam engine facing west (its back, the output, is east) with coal and a water source below it. */
	private static BlockPos steamEngine(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos.below(), Blocks.WATER);
		helper.setBlock(pos, JugcraftKinetics.STEAM_ENGINE.defaultBlockState().setValue(SteamEngineBlock.FACING, Direction.WEST));
		Storage<ItemVariant> fuel = ItemStorage.SIDED.find(helper.getLevel(), helper.absolutePos(pos), Direction.UP);
		helper.assertTrue(fuel != null, "The steam engine takes no fuel");
		try (Transaction transaction = Transaction.openOuter()) {
			helper.assertTrue(fuel.insert(ItemVariant.of(Items.COAL), 4, transaction) == 4, "The steam engine refused coal");
			transaction.commit();
		}
		return pos;
	}

	/** A steam engine turns a shaft line that runs a crusher with no JE at all; the shafts show as turning. */
	@GameTest(maxTicks = 400)
	public void steamEngineDrivesCrusherThroughShafts(GameTestHelper helper) {
		BlockPos engine = steamEngine(helper, new BlockPos(1, 1, 2));
		BlockState shaft = JugcraftKinetics.IRON_SHAFT.defaultBlockState().setValue(ShaftBlock.AXIS, Direction.Axis.X);
		helper.setBlock(engine.east(), shaft);
		helper.setBlock(engine.east(2), shaft);
		BlockPos crusherPos = engine.east(3);
		helper.setBlock(crusherPos, machine(MachineKind.CRUSHER));
		MachineBlockEntity crusher = helper.getBlockEntity(crusherPos, MachineBlockEntity.class);
		crusher.setItem(0, new ItemStack(item("tin_ore")));
		helper.succeedWhen(() -> {
			helper.assertTrue(helper.getBlockState(engine.east()).getValue(ShaftBlock.TURNING), "The shaft is not turning");
			helper.assertTrue(crusher.getItem(MachineKind.CRUSHER.outputSlot()).is(item("raw_tin")),
					"Crusher output is " + crusher.getItem(MachineKind.CRUSHER.outputSlot()));
		});
	}

	/** A gearbox splits one engine between a dynamo (which makes JE) and a machine on another side. */
	@GameTest(maxTicks = 200)
	public void gearboxBranchesToDynamoAndMachine(GameTestHelper helper) {
		BlockPos engine = steamEngine(helper, new BlockPos(1, 1, 2));
		BlockPos gearbox = engine.east();
		helper.setBlock(gearbox, JugcraftKinetics.BRASS_GEARBOX);
		helper.setBlock(gearbox.east(), JugcraftKinetics.DYNAMO);
		helper.setBlock(gearbox.south(), machine(MachineKind.ELECTRIC_FURNACE));
		DynamoBlockEntity dynamo = helper.getBlockEntity(gearbox.east(), DynamoBlockEntity.class);
		MachineBlockEntity furnace = helper.getBlockEntity(gearbox.south(), MachineBlockEntity.class);
		helper.succeedWhen(() -> {
			helper.assertTrue(dynamo.energy().getAmount() > 0, "The dynamo made no JE");
			helper.assertTrue(furnace.energyFor(null).getAmount() > 0, "The furnace got no power");
		});
	}

	/** A hand crank on top of a dynamo charges it while it turns. */
	@GameTest(maxTicks = 100)
	public void handCrankChargesDynamo(GameTestHelper helper) {
		BlockPos dynamoPos = new BlockPos(2, 1, 2);
		helper.setBlock(dynamoPos, JugcraftKinetics.DYNAMO);
		helper.setBlock(dynamoPos.above(), JugcraftKinetics.HAND_CRANK.defaultBlockState().setValue(HandCrankBlock.FACING, Direction.DOWN));
		helper.getBlockEntity(dynamoPos.above(), HandCrankBlockEntity.class).addTurns(HandCrankBlockEntity.TICKS_PER_CRANK);
		DynamoBlockEntity dynamo = helper.getBlockEntity(dynamoPos, DynamoBlockEntity.class);
		helper.succeedWhen(() -> helper.assertTrue(dynamo.energy().getAmount() >= 100,
				"The dynamo holds only " + dynamo.energy().getAmount() + " JE"));
	}

	/** A charged electric motor facing a crusher runs it on rotation alone, using its own JE. */
	@GameTest(maxTicks = 300)
	public void electricMotorDrivesCrusher(GameTestHelper helper) {
		BlockPos motorPos = new BlockPos(1, 1, 2);
		helper.setBlock(motorPos, JugcraftKinetics.ELECTRIC_MOTOR.defaultBlockState().setValue(ElectricMotorBlock.FACING, Direction.EAST));
		ElectricMotorBlockEntity motor = helper.getBlockEntity(motorPos, ElectricMotorBlockEntity.class);
		motor.energy().setAmount(ElectricMotorBlockEntity.CAPACITY);
		helper.setBlock(motorPos.east(), machine(MachineKind.CRUSHER));
		MachineBlockEntity crusher = helper.getBlockEntity(motorPos.east(), MachineBlockEntity.class);
		crusher.setItem(0, new ItemStack(item("tin_ore")));
		helper.succeedWhen(() -> {
			helper.assertTrue(crusher.getItem(MachineKind.CRUSHER.outputSlot()).is(item("raw_tin")),
					"Crusher output is " + crusher.getItem(MachineKind.CRUSHER.outputSlot()));
			helper.assertTrue(motor.energy().getAmount() < ElectricMotorBlockEntity.CAPACITY, "The motor used no JE");
		});
	}

	/** A belt between two pulleys carries a hand crank's rotation to a dynamo under the other pulley. */
	@GameTest(maxTicks = 200)
	public void beltCarriesRotation(GameTestHelper helper) {
		BlockState pulley = JugcraftKinetics.BELT_PULLEY.defaultBlockState().setValue(ShaftBlock.AXIS, Direction.Axis.Y);
		BlockPos first = new BlockPos(1, 2, 2);
		BlockPos second = new BlockPos(5, 2, 2);
		helper.setBlock(first, pulley);
		helper.setBlock(second, pulley);
		helper.setBlock(second.below(), JugcraftKinetics.DYNAMO);
		BlockPos a = helper.absolutePos(first);
		BlockPos b = helper.absolutePos(second);
		helper.assertTrue(BeltPulleyBlockEntity.cannotLink(helper.getLevel(), a, b) == null,
				"The pulleys cannot link: " + BeltPulleyBlockEntity.cannotLink(helper.getLevel(), a, b));
		BeltPulleyBlockEntity.connect(helper.getLevel(), a, b);
		helper.setBlock(first.above(), JugcraftKinetics.HAND_CRANK.defaultBlockState().setValue(HandCrankBlock.FACING, Direction.DOWN));
		helper.getBlockEntity(first.above(), HandCrankBlockEntity.class).addTurns(HandCrankBlockEntity.TICKS_PER_CRANK);
		DynamoBlockEntity dynamo = helper.getBlockEntity(second.below(), DynamoBlockEntity.class);
		helper.succeedWhen(() -> helper.assertTrue(dynamo.energy().getAmount() >= 100,
				"The dynamo behind the belt holds only " + dynamo.energy().getAmount() + " JE"));
	}

	/** Pulleys on different levels of their axis, or too far apart, refuse a belt. */
	@GameTest
	public void beltRefusesBadPulleys(GameTestHelper helper) {
		BlockState pulley = JugcraftKinetics.BELT_PULLEY.defaultBlockState().setValue(ShaftBlock.AXIS, Direction.Axis.Y);
		helper.setBlock(new BlockPos(1, 1, 1), pulley);
		helper.setBlock(new BlockPos(3, 2, 1), pulley);
		String reason = BeltPulleyBlockEntity.cannotLink(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)),
				helper.absolutePos(new BlockPos(3, 2, 1)));
		helper.assertTrue("axis".equals(reason), "Pulleys at different heights linked: " + reason);
		helper.succeed();
	}

	// ------------------------------------------------------------------ conveyors

	/** A charged electric motor at {@code pos} facing east, driving whatever is east of it. */
	private static void motorFacingEast(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, JugcraftKinetics.ELECTRIC_MOTOR.defaultBlockState().setValue(ElectricMotorBlock.FACING, Direction.EAST));
		helper.getBlockEntity(pos, ElectricMotorBlockEntity.class).energy().setAmount(ElectricMotorBlockEntity.CAPACITY);
	}

	private static ConveyorBlockEntity conveyor(GameTestHelper helper, BlockPos pos, Direction facing, boolean splitter) {
		helper.setBlock(pos, (splitter ? JugcraftLogistics.CONVEYOR_SPLITTER : JugcraftLogistics.CONVEYOR).defaultBlockState()
				.setValue(ConveyorBlock.FACING, facing));
		return helper.getBlockEntity(pos, ConveyorBlockEntity.class);
	}

	/** Items put in at the back of a driven two-conveyor line (as a pipe would) end up in the chest at its end. */
	@GameTest(maxTicks = 200)
	public void conveyorCarriesItemsIntoChest(GameTestHelper helper) {
		conveyor(helper, new BlockPos(2, 1, 2), Direction.EAST, false);
		conveyor(helper, new BlockPos(3, 1, 2), Direction.EAST, false);
		ChestBlockEntity chest = chest(helper, new BlockPos(4, 1, 2));
		motorFacingEast(helper, new BlockPos(1, 1, 2));
		Storage<ItemVariant> belt = ItemStorage.SIDED.find(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2)), Direction.UP);
		helper.assertTrue(belt != null, "The conveyor takes no items");
		try (Transaction transaction = Transaction.openOuter()) {
			helper.assertTrue(belt.insert(ItemVariant.of(Items.COBBLESTONE), 16, transaction) == 16, "The conveyor refused cobblestone");
			transaction.commit();
		}
		helper.succeedWhen(() -> helper.assertTrue(count(chest, Items.COBBLESTONE) == 16,
				"The chest holds " + count(chest, Items.COBBLESTONE) + " cobblestone"));
	}

	/** Without a drive, items stay on the belt. */
	@GameTest(maxTicks = 100)
	public void conveyorNeedsRotation(GameTestHelper helper) {
		ConveyorBlockEntity belt = conveyor(helper, new BlockPos(2, 1, 2), Direction.EAST, false);
		ChestBlockEntity chest = chest(helper, new BlockPos(3, 1, 2));
		helper.assertTrue(belt.accept(new ItemStack(Items.COBBLESTONE), 0.5F), "The conveyor refused an item");
		helper.runAtTickTime(60, () -> {
			helper.assertTrue(count(chest, Items.COBBLESTONE) == 0, "An undriven conveyor moved an item");
			helper.assertTrue(belt.items().size() == 1, "The item left the belt");
			helper.succeed();
		});
	}

	/** An item entity dropped on a driven conveyor is picked up and carried off. */
	@GameTest(maxTicks = 200)
	public void conveyorPicksUpDroppedItems(GameTestHelper helper) {
		conveyor(helper, new BlockPos(2, 1, 2), Direction.EAST, false);
		ChestBlockEntity chest = chest(helper, new BlockPos(3, 1, 2));
		motorFacingEast(helper, new BlockPos(1, 1, 2));
		helper.spawnItem(Items.IRON_INGOT, 2.5F, 1.5F, 2.5F);
		helper.succeedWhen(() -> helper.assertTrue(count(chest, Items.IRON_INGOT) == 1, "The dropped ingot did not arrive"));
	}

	/** A splitter sends one stack left, one straight on and one right. */
	@GameTest(maxTicks = 200)
	public void splitterTakesTurns(GameTestHelper helper) {
		ConveyorBlockEntity splitter = conveyor(helper, new BlockPos(2, 1, 2), Direction.EAST, true);
		ChestBlockEntity left = chest(helper, new BlockPos(2, 1, 1));
		ChestBlockEntity ahead = chest(helper, new BlockPos(3, 1, 2));
		ChestBlockEntity right = chest(helper, new BlockPos(2, 1, 3));
		for (float progress : new float[] {0.75F, 0.5F, 0.25F}) {
			helper.assertTrue(splitter.accept(new ItemStack(Items.COBBLESTONE), progress), "The splitter refused an item");
		}
		motorFacingEast(helper, new BlockPos(1, 1, 2));
		helper.succeedWhen(() -> {
			for (ChestBlockEntity chest : List.of(left, ahead, right)) {
				helper.assertTrue(count(chest, Items.COBBLESTONE) == 1, "Split unevenly: " + count(left, Items.COBBLESTONE) + " left, "
						+ count(ahead, Items.COBBLESTONE) + " ahead, " + count(right, Items.COBBLESTONE) + " right");
			}
		});
	}

	// ------------------------------------------------------------------ auto-crafter

	private static MachineBlockEntity autoCrafter(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, machine(MachineKind.AUTO_CRAFTER));
		charge(helper, pos, Direction.UP);
		return helper.getBlockEntity(pos, MachineBlockEntity.class);
	}

	/** Planks laid out as a stick recipe craft until one plank is left in each slot as the pattern. */
	@GameTest(maxTicks = 300)
	public void autoCrafterKeepsItsPattern(GameTestHelper helper) {
		MachineBlockEntity crafter = autoCrafter(helper, new BlockPos(2, 1, 2));
		crafter.setItem(0, new ItemStack(Items.OAK_PLANKS, 3));
		crafter.setItem(3, new ItemStack(Items.OAK_PLANKS, 3));
		helper.succeedWhen(() -> {
			ItemStack sticks = crafter.getItem(MachineKind.AUTO_CRAFTER.outputSlot());
			helper.assertTrue(sticks.is(Items.STICK) && sticks.getCount() == 8, "Crafted " + sticks);
			helper.assertTrue(crafter.getItem(0).getCount() == 1 && crafter.getItem(3).getCount() == 1,
					"The pattern was not kept: " + crafter.getItem(0) + ", " + crafter.getItem(3));
		});
	}

	/** Container remainders (the glass bottles from a honey block) go to the remainder slot. */
	@GameTest(maxTicks = 200)
	public void autoCrafterKeepsRemainders(GameTestHelper helper) {
		MachineBlockEntity crafter = autoCrafter(helper, new BlockPos(2, 1, 2));
		for (int slot : new int[] {0, 1, 3, 4}) {
			crafter.setItem(slot, new ItemStack(Items.HONEY_BOTTLE, 2));
		}
		helper.succeedWhen(() -> {
			helper.assertTrue(crafter.getItem(MachineKind.AUTO_CRAFTER.outputSlot()).is(Items.HONEY_BLOCK), "No honey block");
			ItemStack bottles = crafter.getItem(MachineKind.AUTO_CRAFTER.outputSlot() + 1);
			helper.assertTrue(bottles.is(Items.GLASS_BOTTLE) && bottles.getCount() == 4, "Remainder slot holds " + bottles);
		});
	}

	/** Pipes and hoppers only top up grid slots that already hold that item; they never set the pattern. */
	@GameTest
	public void autoCrafterAutomationFollowsPattern(GameTestHelper helper) {
		MachineBlockEntity crafter = autoCrafter(helper, new BlockPos(2, 1, 2));
		crafter.setItem(4, new ItemStack(Items.OAK_PLANKS, 1));
		Storage<ItemVariant> storage = ItemStorage.SIDED.find(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2)), Direction.UP);
		try (Transaction transaction = Transaction.openOuter()) {
			long planks = storage.insert(ItemVariant.of(Items.OAK_PLANKS), 10, transaction);
			long cobble = storage.insert(ItemVariant.of(Items.COBBLESTONE), 10, transaction);
			helper.assertTrue(planks == 10, "Accepted " + planks + " planks into the pattern slot");
			helper.assertTrue(cobble == 0, "Accepted cobblestone into an empty grid slot");
			transaction.abort();
		}
		helper.succeed();
	}
}
