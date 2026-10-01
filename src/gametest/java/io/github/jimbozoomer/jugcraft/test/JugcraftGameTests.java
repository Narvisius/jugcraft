package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.deposit.Deposits;
import io.github.jimbozoomer.jugcraft.deposit.JugcraftDeposits;
import io.github.jimbozoomer.jugcraft.electronics.JugcraftElectronics;
import io.github.jimbozoomer.jugcraft.electronics.NetworkTerminalBlock;
import io.github.jimbozoomer.jugcraft.energy.EnergyNetworks;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.farming.JugcraftFarming;
import io.github.jimbozoomer.jugcraft.fluid.ElectricPumpBlockEntity;
import io.github.jimbozoomer.jugcraft.fluid.FluidFilterBlockEntity;
import io.github.jimbozoomer.jugcraft.fluid.FluidTankBlockEntity;
import io.github.jimbozoomer.jugcraft.fluid.FluidValveBlock;
import io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids;
import io.github.jimbozoomer.jugcraft.fluid.StoredFluid;
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
import io.github.jimbozoomer.jugcraft.logistics.ConveyorSlopeBlock;
import io.github.jimbozoomer.jugcraft.logistics.ItemSorterBlockEntity;
import io.github.jimbozoomer.jugcraft.logistics.JugcraftLogistics;
import io.github.jimbozoomer.jugcraft.logistics.PneumaticExtractorBlock;
import io.github.jimbozoomer.jugcraft.machine.Footprint;
import io.github.jimbozoomer.jugcraft.machine.GeneratorFuels;
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
import io.github.jimbozoomer.jugcraft.tools.Chargeable;
import io.github.jimbozoomer.jugcraft.tools.ChargingStationBlock;
import io.github.jimbozoomer.jugcraft.tools.ChargingStationBlockEntity;
import io.github.jimbozoomer.jugcraft.tools.JugcraftTools;
import io.github.jimbozoomer.jugcraft.tools.MiningDrillItem;
import io.github.jimbozoomer.jugcraft.tools.RocketPackItem;
import io.github.jimbozoomer.jugcraft.tools.ToolUpgrades;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
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

	/** A powered fluid valve stops a pump's water short of the tank; once the signal goes, the water gets through. */
	@GameTest(maxTicks = 300)
	public void fluidValveClosesOnRedstone(GameTestHelper helper) {
		helper.setBlock(new BlockPos(1, 1, 3), Blocks.WATER);
		BlockPos pump = new BlockPos(1, 2, 3);
		helper.setBlock(pump, JugcraftFluids.ELECTRIC_PUMP);
		helper.getBlockEntity(pump, ElectricPumpBlockEntity.class).energy().setAmount(ElectricPumpBlockEntity.ENERGY_CAPACITY);
		helper.setBlock(new BlockPos(2, 2, 3), JugcraftFluids.STEEL_FLUID_PIPE);
		BlockPos valve = new BlockPos(3, 2, 3);
		helper.setBlock(valve, JugcraftFluids.FLUID_VALVE);
		BlockPos signal = valve.above();
		helper.setBlock(signal, Blocks.REDSTONE_BLOCK);
		helper.setBlock(new BlockPos(4, 2, 3), JugcraftFluids.STEEL_FLUID_PIPE);
		BlockPos tank = new BlockPos(5, 2, 3);
		helper.setBlock(tank, JugcraftFluids.FLUID_TANK);
		FluidTankBlockEntity tankEntity = helper.getBlockEntity(tank, FluidTankBlockEntity.class);
		java.util.concurrent.atomic.AtomicBoolean opened = new java.util.concurrent.atomic.AtomicBoolean();
		helper.runAfterDelay(60, () -> {
			helper.assertTrue(helper.getBlockState(valve).getValue(FluidValveBlock.POWERED), "The valve is not closed");
			helper.assertTrue(tankEntity.storage.amount == 0, "Water got past the closed valve");
			helper.setBlock(signal, Blocks.AIR);
			opened.set(true);
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(opened.get(), "The valve has not been opened yet");
			helper.assertTrue(tankEntity.storage.amount > 0, "The tank is still empty with the valve open");
		});
	}

	/**
	 * A fluid filter lets nothing into the tank it touches until it is set, and then only its fluid; a tank on an
	 * ordinary pipe of the same line fills all along.
	 */
	@GameTest(maxTicks = 300)
	public void fluidFilterLetsOnlyItsFluidOut(GameTestHelper helper) {
		helper.setBlock(new BlockPos(1, 1, 3), Blocks.WATER);
		BlockPos pump = new BlockPos(1, 2, 3);
		helper.setBlock(pump, JugcraftFluids.ELECTRIC_PUMP);
		helper.getBlockEntity(pump, ElectricPumpBlockEntity.class).energy().setAmount(ElectricPumpBlockEntity.ENERGY_CAPACITY);
		helper.setBlock(new BlockPos(2, 2, 3), JugcraftFluids.STEEL_FLUID_PIPE);
		BlockPos filter = new BlockPos(3, 2, 3);
		helper.setBlock(filter, JugcraftFluids.FLUID_FILTER);
		BlockPos open = new BlockPos(2, 2, 2);
		BlockPos filtered = new BlockPos(4, 2, 3);
		helper.setBlock(open, JugcraftFluids.FLUID_TANK);
		helper.setBlock(filtered, JugcraftFluids.FLUID_TANK);
		FluidTankBlockEntity openTank = helper.getBlockEntity(open, FluidTankBlockEntity.class);
		FluidTankBlockEntity filteredTank = helper.getBlockEntity(filtered, FluidTankBlockEntity.class);
		FluidFilterBlockEntity filterEntity = helper.getBlockEntity(filter, FluidFilterBlockEntity.class);
		java.util.concurrent.atomic.AtomicBoolean set = new java.util.concurrent.atomic.AtomicBoolean();
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(openTank.storage.amount > 0, "The tank on the ordinary pipe is empty");
			helper.assertTrue(filteredTank.storage.amount == 0, "An unset filter let water out");
			filterEntity.setFilter(FluidVariant.of(Fluids.LAVA));
		});
		helper.runAfterDelay(80, () -> {
			helper.assertTrue(filteredTank.storage.amount == 0, "A lava filter let water out");
			filterEntity.setFilter(FluidVariant.of(Fluids.WATER));
			set.set(true);
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(set.get(), "The filter is not set to water yet");
			helper.assertTrue(filteredTank.storage.amount > 0, "A water filter let no water out");
		});
	}

	/** The crop harvester harvests a ripe wheat crop in its field, keeps the wheat, and plants one of the seeds again. */
	@GameTest(maxTicks = 200)
	public void cropHarvesterHarvestsAndReplants(GameTestHelper helper) {
		BlockPos master = new BlockPos(4, 1, 0);
		LargeMachineBlock block = (LargeMachineBlock) JugcraftMachines.MACHINES.get(MachineKind.CROP_HARVESTER);
		helper.setBlock(master, block.defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
		block.setPlacedBy(helper.getLevel(), helper.absolutePos(master), helper.getBlockState(master), null, ItemStack.EMPTY);
		charge(helper, master.above(), Direction.UP);
		MachineBlockEntity harvester = helper.getBlockEntity(master, MachineBlockEntity.class);
		// The field starts the block in front (south); a ripe crop two blocks out and one to the side.
		BlockPos crop = new BlockPos(3, 1, 2);
		helper.setBlock(crop.below(), Blocks.FARMLAND);
		helper.setBlock(crop, ((CropBlock) Blocks.WHEAT).getStateForAge(7));
		helper.succeedWhen(() -> {
			BlockState replanted = helper.getBlockState(crop);
			helper.assertTrue(replanted.is(Blocks.WHEAT) && ((CropBlock) Blocks.WHEAT).getAge(replanted) == 0,
					"The crop is now " + replanted);
			boolean wheat = false;
			for (int slot = 0; slot < MachineKind.CROP_HARVESTER.slots; slot++) {
				wheat |= harvester.getItem(slot).is(Items.WHEAT);
			}
			helper.assertTrue(wheat, "The harvester kept no wheat");
		});
	}

	/**
	 * Cotton: the seeds plant the crop on farmland, a ripe crop drops cotton and seeds (an unripe one only a seed), cotton counts as a crop for
	 * fertilizer, and sifting coarse dirt can turn up the seeds.
	 */
	@GameTest
	public void cottonGrowsFromSeedsAndDropsCotton(GameTestHelper helper) {
		BlockPos crop = new BlockPos(2, 2, 2);
		helper.setBlock(crop.below(), Blocks.FARMLAND);
		helper.setBlock(crop, JugcraftFarming.COTTON_CROP);
		helper.assertTrue(helper.getBlockState(crop).is(BlockTags.CROPS), "Cotton is not in minecraft:crops");
		CropBlock cotton = (CropBlock) JugcraftFarming.COTTON_CROP;
		helper.assertTrue(cotton.asItem() == JugcraftFarming.COTTON_SEEDS, "Cotton seeds do not plant cotton");
		BlockState ripe = cotton.getStateForAge(cotton.getMaxAge());
		helper.setBlock(crop, ripe);
		List<ItemStack> drops = Block.getDrops(ripe, helper.getLevel(), helper.absolutePos(crop), null);
		helper.assertTrue(drops.stream().anyMatch(stack -> stack.is(JugcraftFarming.COTTON)), "A ripe crop dropped " + drops);
		helper.assertTrue(drops.stream().anyMatch(stack -> stack.is(JugcraftFarming.COTTON_SEEDS)), "A ripe crop dropped " + drops);
		List<ItemStack> unripe = Block.getDrops(cotton.getStateForAge(3), helper.getLevel(), helper.absolutePos(crop), null);
		helper.assertTrue(unripe.stream().noneMatch(stack -> stack.is(JugcraftFarming.COTTON))
				&& unripe.stream().anyMatch(stack -> stack.is(JugcraftFarming.COTTON_SEEDS)), "An unripe crop dropped " + unripe);
		MachineRecipe sifting = MachineRecipes.find(helper.getLevel(), MachineKind.SIEVE, new ItemStack(Items.COARSE_DIRT))
				.orElseThrow(() -> helper.assertionException("No sifting recipe for coarse dirt"));
		helper.assertTrue(sifting.byproducts().stream().anyMatch(b -> b.result().create().is(JugcraftFarming.COTTON_SEEDS)),
				"Sifting coarse dirt never gives cotton seeds");
		helper.succeed();
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

	/** The two-block crystal grower pulls a silicon boule from 4 silicon and a phosphate; the sawmill cuts it into 8 wafers. */
	@GameTest(maxTicks = 600)
	public void crystalGrowerPullsABoule(GameTestHelper helper) {
		BlockPos master = new BlockPos(2, 1, 2);
		MachineBlockEntity grower = large(helper, master, MachineKind.CRYSTAL_GROWER);
		charge(helper, master.above(), Direction.WEST);
		grower.setItem(0, new ItemStack(item("silicon"), 4));
		grower.setItem(1, new ItemStack(item("phosphate")));
		MachineRecipe wafers = MachineRecipes.find(helper.getLevel(), MachineKind.SAWMILL, new ItemStack(item("silicon_boule")))
				.orElseThrow(() -> helper.assertionException("No sawing recipe for a silicon boule"));
		ItemStack sawn = wafers.output().create();
		helper.assertTrue(sawn.is(item("silicon_wafer")) && sawn.getCount() == 8, "A boule saws into " + sawn);
		helper.succeedWhen(() -> {
			ItemStack output = grower.getItem(MachineKind.CRYSTAL_GROWER.outputSlot());
			helper.assertTrue(output.is(item("silicon_boule")), "Crystal grower output is " + output);
			helper.assertTrue(grower.getItem(0).isEmpty() && grower.getItem(1).isEmpty(), "The inputs were not used up");
		});
	}

	/** The circuit assembler bonds four microchips to an advanced circuit with gold: a processor. */
	@GameTest(maxTicks = 600)
	public void circuitAssemblerMakesAProcessor(GameTestHelper helper) {
		MachineBlockEntity assembler = processing(helper, new BlockPos(2, 1, 2), MachineKind.CIRCUIT_ASSEMBLER,
				new ItemStack(item("microchip"), 4));
		assembler.setItem(1, new ItemStack(item("advanced_circuit")));
		assembler.setItem(2, new ItemStack(Items.GOLD_INGOT));
		helper.succeedWhen(() -> {
			ItemStack output = assembler.getItem(MachineKind.CIRCUIT_ASSEMBLER.outputSlot());
			helper.assertTrue(output.is(item("processor")), "Circuit assembler output is " + output);
		});
	}

	/**
	 * The network terminal reads the network it is cabled to: four cables, a battery box and a capacitor bank whose two
	 * lower blocks both touch the cables. The bank counts once.
	 */
	@GameTest
	public void networkTerminalReadsItsNetwork(GameTestHelper helper) {
		BlockPos terminal = new BlockPos(1, 1, 2);
		helper.setBlock(terminal, JugcraftElectronics.NETWORK_TERMINAL);
		helper.assertTrue(NetworkTerminalBlock.read(helper.getLevel(), helper.absolutePos(terminal)) == null,
				"A terminal with no cable read a network");
		for (BlockPos cable : List.of(new BlockPos(2, 1, 2), new BlockPos(2, 1, 3), new BlockPos(3, 1, 3), new BlockPos(4, 1, 3))) {
			helper.setBlock(cable, JugcraftMachines.COPPER_CABLE);
		}
		helper.setBlock(new BlockPos(2, 1, 1), machine(MachineKind.BATTERY_BOX));
		((SimpleEnergyStorage) helper.getBlockEntity(new BlockPos(2, 1, 1), MachineBlockEntity.class).energyFor(null)).setAmount(100_000);
		MachineBlockEntity bank = large(helper, new BlockPos(4, 1, 4), MachineKind.CAPACITOR_BANK);
		((SimpleEnergyStorage) bank.energyFor(null)).setAmount(1_000_000);
		NetworkTerminalBlock.Reading reading = NetworkTerminalBlock.read(helper.getLevel(), helper.absolutePos(terminal));
		helper.assertTrue(reading != null, "The terminal found no network");
		helper.assertTrue(reading.cables() == 4, "Cables: " + reading.cables());
		helper.assertTrue(reading.devices() == 2, "Devices: " + reading.devices());
		helper.assertTrue(reading.stored() == 1_100_000, "Stored: " + reading.stored());
		helper.assertTrue(reading.capacity() == 4_400_000, "Capacity: " + reading.capacity());
		helper.succeed();
	}

	/**
	 * Loot tables load in Minecraft 26.x's format: the charging station drops once (from its lower half only) and salt
	 * ore drops two to four salt. With the older keys, which 26.x ignores, both halves dropped and the ore dropped one.
	 */
	@GameTest
	public void lootTablesKeepTheirConditionsAndCounts(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
		BlockState lower = JugcraftTools.CHARGING_STATION.defaultBlockState();
		BlockState upper = lower.setValue(ChargingStationBlock.HALF, DoubleBlockHalf.UPPER);
		helper.assertTrue(Block.getDrops(lower, helper.getLevel(), pos, null).size() == 1, "The lower half did not drop the station");
		helper.assertTrue(Block.getDrops(upper, helper.getLevel(), pos, null).isEmpty(), "The upper half dropped a second station");
		BlockState saltOre = BuiltInRegistries.BLOCK.getValue(Jugcraft.id("salt_ore")).defaultBlockState();
		for (int i = 0; i < 8; i++) {
			int salt = Block.getDrops(saltOre, helper.getLevel(), pos, null).stream().mapToInt(ItemStack::getCount).sum();
			helper.assertTrue(salt >= 2 && salt <= 4, "Salt ore dropped " + salt + " salt");
		}
		helper.succeed();
	}

	/**
	 * A broken tank drops with its fluid and a placed one takes it back: a tinplate tank with five buckets of water
	 * and a steel tank broken from any of its blocks both keep what they held, and an empty tank's item carries nothing.
	 */
	@GameTest(maxTicks = 40)
	public void tanksKeepTheirFluidWhenBroken(GameTestHelper helper) {
		BlockPos tank = new BlockPos(1, 1, 1);
		helper.setBlock(tank, JugcraftFluids.FLUID_TANK);
		FluidTankBlockEntity tankEntity = helper.getBlockEntity(tank, FluidTankBlockEntity.class);
		List<ItemStack> empty = Block.getDrops(helper.getBlockState(tank), helper.getLevel(), helper.absolutePos(tank), tankEntity);
		helper.assertTrue(empty.size() == 1 && !empty.get(0).has(JugcraftFluids.STORED_FLUID), "An empty tank dropped with fluid");
		try (Transaction transaction = Transaction.openOuter()) {
			tankEntity.storage.insert(FluidVariant.of(Fluids.WATER), 5 * FluidConstants.BUCKET, transaction);
			transaction.commit();
		}
		List<ItemStack> drops = Block.getDrops(helper.getBlockState(tank), helper.getLevel(), helper.absolutePos(tank), tankEntity);
		helper.assertTrue(drops.size() == 1, "The tank dropped " + drops.size() + " stacks");
		StoredFluid stored = drops.get(0).get(JugcraftFluids.STORED_FLUID);
		helper.assertTrue(stored != null && stored.variant().isOf(Fluids.WATER) && stored.amount() == 5 * FluidConstants.BUCKET,
				"The tank's item does not carry its water: " + stored);
		BlockPos placed = new BlockPos(3, 1, 1);
		helper.setBlock(placed, JugcraftFluids.FLUID_TANK);
		FluidTankBlockEntity placedEntity = helper.getBlockEntity(placed, FluidTankBlockEntity.class);
		placedEntity.applyComponentsFromItemStack(drops.get(0));
		helper.assertTrue(placedEntity.storage.variant.isOf(Fluids.WATER) && placedEntity.storage.amount == 5 * FluidConstants.BUCKET,
				"The placed tank did not take back its water");

		BlockPos base = new BlockPos(5, 1, 1);
		LargeMachineBlock steel = (LargeMachineBlock) JugcraftMachines.MACHINES.get(MachineKind.STEEL_TANK);
		helper.setBlock(base, steel.defaultBlockState());
		steel.setPlacedBy(helper.getLevel(), helper.absolutePos(base), helper.getBlockState(base), null, ItemStack.EMPTY);
		MachineBlockEntity steelEntity = helper.getBlockEntity(base, MachineBlockEntity.class);
		try (Transaction transaction = Transaction.openOuter()) {
			steelEntity.reservoir().insert(FluidVariant.of(Fluids.LAVA), 20 * FluidConstants.BUCKET, transaction);
			transaction.commit();
		}
		List<ItemStack> steelDrops = Block.getDrops(helper.getBlockState(base), helper.getLevel(), helper.absolutePos(base), steelEntity);
		StoredFluid lava = steelDrops.isEmpty() ? null : steelDrops.get(0).get(JugcraftFluids.STORED_FLUID);
		helper.assertTrue(steelDrops.size() == 1 && lava != null && lava.variant().isOf(Fluids.LAVA) && lava.amount() == 20 * FluidConstants.BUCKET,
				"The steel tank's item does not carry its lava: " + steelDrops);
		// Break another block of the 2x2 tank (not the master, which holds the fluid): the whole tank drops once.
		BlockPos other = null;
		for (BlockPos near : BlockPos.betweenClosed(base.offset(-1, 0, -1), base.offset(1, 0, 1))) {
			if (!near.equals(base) && helper.getBlockState(near).is(steel)) {
				other = near.immutable();
			}
		}
		helper.assertTrue(other != null, "The steel tank did not form");
		BlockPos broken = other;
		helper.destroyBlock(broken);
		helper.succeedWhen(() -> {
			helper.assertBlockNotPresent(steel, base);
			helper.assertBlockNotPresent(steel, broken);
			int tanks = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
					new net.minecraft.world.phys.AABB(helper.absolutePos(base)).inflate(3),
					item -> item.getItem().has(JugcraftFluids.STORED_FLUID) && item.getItem().get(JugcraftFluids.STORED_FLUID).amount() == 20 * FluidConstants.BUCKET).size();
			helper.assertTrue(tanks == 1, "Breaking the steel tank's top dropped " + tanks + " full tanks");
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

	/** The 3x2 lithium battery bank holds 32,000,000 JE and gives up to 16,384 JE/t out of all six front sockets. */
	@GameTest
	public void lithiumBatteryBankOutputsFromItsFront(GameTestHelper helper) {
		BlockPos master = new BlockPos(5, 1, 3);
		large(helper, master, MachineKind.LITHIUM_BATTERY_BANK);
		EnergyStorage front = EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(master.west().above()), Direction.NORTH);
		EnergyStorage side = EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(master.west(2)), Direction.WEST);
		EnergyStorage back = EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(master.above()), Direction.SOUTH);
		helper.assertTrue(front != null && front.supportsExtraction() && !front.supportsInsertion(), "Front must only give power");
		helper.assertTrue(side != null && side.supportsInsertion() && !side.supportsExtraction(), "Sides must only take power");
		helper.assertTrue(back != null && back.supportsInsertion(), "The back must take power");
		helper.assertTrue(front.getCapacity() == 32_000_000, "Capacity is " + front.getCapacity());
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

	/**
	 * A deposit drill standing on surface deposits takes one of each kind per cycle from those under it and one block
	 * round it, into a chest beside it, and leaves stone where each ran out; a deposit outside its reach stays full.
	 * Two cycles empty an iron and a coal deposit with 2 left each; mining them one after the other would take four,
	 * longer than the test allows. Picks get nothing from a deposit block.
	 */
	@GameTest(maxTicks = 1000)
	public void depositDrillEmptiesDepositsIntoAChest(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos master = new BlockPos(4, 2, 4);
		// The drill covers x 2..4, z 4..6 (it faces north); it reaches x 1..5, z 3..7, at y 1 and below.
		BlockPos iron = new BlockPos(3, 1, 5);
		BlockPos coal = new BlockPos(5, 1, 7);
		BlockPos outside = new BlockPos(0, 1, 5);
		helper.setBlock(iron, JugcraftDeposits.BLOCKS.get("iron_deposit"));
		helper.setBlock(coal, JugcraftDeposits.BLOCKS.get("coal_deposit"));
		helper.setBlock(outside, JugcraftDeposits.BLOCKS.get("iron_deposit"));
		helper.assertTrue(Block.getDrops(helper.getBlockState(iron), level, helper.absolutePos(iron), null).isEmpty(),
				"A deposit block drops something when broken");
		// Run the two deposits nearly dry so the test finishes quickly: 2 iron and 2 coal left.
		helper.assertTrue(Deposits.extract(level, helper.absolutePos(iron), Deposits.CAPACITY - 2) == Deposits.CAPACITY - 2,
				"Could not draw down the iron deposit");
		Deposits.extract(level, helper.absolutePos(coal), Deposits.CAPACITY - 2);
		helper.assertTrue(Deposits.remaining(level, helper.absolutePos(coal)) == 2, "The coal deposit does not hold 2");
		helper.setBlock(new BlockPos(4, 2, 3), Blocks.CHEST);
		large(helper, master, MachineKind.DEPOSIT_DRILL);
		charge(helper, master, Direction.EAST);
		helper.succeedWhen(() -> {
			helper.assertBlockPresent(Blocks.STONE, iron);
			helper.assertBlockPresent(Blocks.STONE, coal);
			helper.assertBlockPresent(JugcraftDeposits.BLOCKS.get("iron_deposit"), outside);
			helper.assertTrue(Deposits.remaining(level, helper.absolutePos(outside)) == Deposits.CAPACITY,
					"The deposit out of reach was drawn on");
			ChestBlockEntity chest = helper.getBlockEntity(new BlockPos(4, 2, 3), ChestBlockEntity.class);
			int rawIron = count(chest, Items.RAW_IRON);
			int coalItems = count(chest, Items.COAL);
			helper.assertTrue(rawIron == 2 && coalItems == 2, "The chest holds " + rawIron + " raw iron and " + coalItems + " coal");
		});
	}

	/**
	 * The iron deposit's worldgen feature, placed on a two-layer stone floor as the surface heightmap would place it
	 * (on the first air block), turns a disk of the top layer into iron deposit and leaves the layer below as stone.
	 */
	@GameTest
	public void depositFeatureReplacesTheTopLayer(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
				helper.setBlock(new BlockPos(x, 2, z), Blocks.STONE);
			}
		}
		ServerLevel level = helper.getLevel();
		PlacedFeature placed = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE)
				.getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, Jugcraft.id("iron_deposit"))).value();
		// Without its placement rules (rarity, heightmap, biome), the feature goes exactly where it is put.
		new PlacedFeature(placed.feature(), List.of()).place(level, level.getChunkSource().getGenerator(),
				RandomSource.create(42), helper.absolutePos(new BlockPos(4, 3, 4)));
		Block deposit = JugcraftDeposits.BLOCKS.get("iron_deposit");
		helper.assertBlockPresent(deposit, new BlockPos(4, 2, 4));
		helper.assertBlockPresent(Blocks.STONE, new BlockPos(4, 1, 4));
		int top = 0;
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				top += helper.getBlockState(new BlockPos(x, 2, z)).is(deposit) ? 1 : 0;
				helper.assertBlockNotPresent(deposit, new BlockPos(x, 1, z));
			}
		}
		helper.assertTrue(top >= 9, "Only " + top + " deposit blocks were placed");
		helper.succeed();
	}

	/** Breaking a deposit forgets what was taken from it, so a deposit placed there again is full. */
	@GameTest
	public void brokenDepositIsForgotten(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, JugcraftDeposits.BLOCKS.get("tin_deposit"));
		Deposits.extract(level, helper.absolutePos(pos), 400);
		helper.assertTrue(Deposits.remaining(level, helper.absolutePos(pos)) == Deposits.CAPACITY - 400, "Nothing was taken");
		helper.setBlock(pos, Blocks.AIR);
		helper.setBlock(pos, JugcraftDeposits.BLOCKS.get("tin_deposit"));
		helper.assertTrue(Deposits.remaining(level, helper.absolutePos(pos)) == Deposits.CAPACITY,
				"A new deposit block starts with " + Deposits.remaining(level, helper.absolutePos(pos)));
		helper.succeed();
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

	/**
	 * A magnet motor driving a magnet dynamo, which feeds the motor back: both run at the magnet rates, but at 95%
	 * each way the pair loses power every round and never gains any.
	 */
	@GameTest(maxTicks = 200)
	public void magnetMotorAndDynamoLoopLosesPower(GameTestHelper helper) {
		BlockPos motorPos = new BlockPos(1, 1, 2);
		helper.setBlock(motorPos, JugcraftKinetics.MAGNET_MOTOR.defaultBlockState().setValue(ElectricMotorBlock.FACING, Direction.EAST));
		helper.setBlock(motorPos.east(), JugcraftKinetics.MAGNET_DYNAMO);
		ElectricMotorBlockEntity motor = helper.getBlockEntity(motorPos, ElectricMotorBlockEntity.class);
		DynamoBlockEntity dynamo = helper.getBlockEntity(motorPos.east(), DynamoBlockEntity.class);
		helper.assertTrue(motor.stats() == ElectricMotorBlockEntity.MAGNET, "The magnet motor has copper stats");
		helper.assertTrue(dynamo.stats() == DynamoBlockEntity.MAGNET, "The magnet dynamo has copper stats");
		long start = ElectricMotorBlockEntity.MAGNET.capacity();
		motor.energy().setAmount(start);
		helper.runAfterDelay(100, () -> {
			long total = motor.energy().getAmount() + dynamo.energy().getAmount();
			helper.assertTrue(total < start, "The pair holds " + total + " JE of " + start);
			// At least 100 ticks of the motor's full 384 KE/t went round, losing about a tenth of each pass.
			helper.assertTrue(start - total >= 100L * 384 * 5 / 100, "Only " + (start - total) + " JE was lost");
			helper.succeed();
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

	/** Items climb an up slope onto a raised conveyor, come down a down slope and land in a chest. */
	@GameTest(maxTicks = 300)
	public void conveyorSlopesGoUpAndDown(GameTestHelper helper) {
		conveyor(helper, new BlockPos(2, 1, 2), Direction.EAST, false);
		helper.setBlock(new BlockPos(3, 1, 2), JugcraftLogistics.CONVEYOR_SLOPE.defaultBlockState()
				.setValue(ConveyorBlock.FACING, Direction.EAST).setValue(ConveyorSlopeBlock.ASCENDING, true));
		conveyor(helper, new BlockPos(4, 2, 2), Direction.EAST, false);
		helper.setBlock(new BlockPos(5, 1, 2), JugcraftLogistics.CONVEYOR_SLOPE.defaultBlockState()
				.setValue(ConveyorBlock.FACING, Direction.EAST).setValue(ConveyorSlopeBlock.ASCENDING, false));
		ChestBlockEntity chest = chest(helper, new BlockPos(6, 1, 2));
		motorFacingEast(helper, new BlockPos(1, 1, 2));
		helper.getBlockEntity(new BlockPos(2, 1, 2), ConveyorBlockEntity.class).accept(new ItemStack(Items.COBBLESTONE, 8), 0);
		helper.succeedWhen(() -> helper.assertTrue(count(chest, Items.COBBLESTONE) == 8,
				"The chest holds " + count(chest, Items.COBBLESTONE) + " cobblestone"));
	}

	// ------------------------------------------------------------------ powered tools

	private static ItemStack charged(Item item) {
		ItemStack stack = new ItemStack(item);
		Chargeable.setEnergy(stack, Chargeable.capacity(stack));
		return stack;
	}

	/** A survival mock player at {@code pos}, facing south (+z), holding {@code tool}. */
	private static ServerPlayer miner(GameTestHelper helper, ItemStack tool) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		player.setYRot(0);
		player.setXRot(0);
		player.setItemInHand(InteractionHand.MAIN_HAND, tool);
		return player;
	}

	/** A charging station on cables' power fills the tool on its cradle and lights up. */
	@GameTest(maxTicks = 100)
	public void chargingStationChargesTool(GameTestHelper helper) {
		BlockPos lower = new BlockPos(2, 1, 2);
		BlockState state = JugcraftTools.CHARGING_STATION.defaultBlockState();
		helper.setBlock(lower, state);
		helper.setBlock(lower.above(), state.setValue(ChargingStationBlock.HALF, DoubleBlockHalf.UPPER));
		charge(helper, lower.above(), Direction.WEST);
		ChargingStationBlockEntity station = helper.getBlockEntity(lower, ChargingStationBlockEntity.class);
		station.setTool(new ItemStack(JugcraftTools.MINING_DRILL));
		helper.succeedWhen(() -> {
			long energy = Chargeable.energy(station.tool());
			helper.assertTrue(energy >= ChargingStationBlockEntity.CHARGE_RATE * 10, "The drill holds only " + energy + " JE");
			helper.assertTrue(helper.getBlockState(lower).getValue(ChargingStationBlock.LIT), "The station is not lit while charging");
		});
	}

	/** An empty drill mines like a bare hand and gets no ore drops; a charged one is fast and correct. */
	@GameTest
	public void emptyDrillIsSlow(GameTestHelper helper) {
		BlockState stone = Blocks.STONE.defaultBlockState();
		ItemStack empty = new ItemStack(JugcraftTools.MINING_DRILL);
		ItemStack full = charged(JugcraftTools.MINING_DRILL);
		helper.assertTrue(empty.getDestroySpeed(stone) == 1.0F, "An empty drill mines at " + empty.getDestroySpeed(stone));
		helper.assertTrue(!empty.isCorrectToolForDrops(Blocks.IRON_ORE.defaultBlockState()), "An empty drill gets ore drops");
		helper.assertTrue(full.getDestroySpeed(stone) > 8.0F, "A charged drill mines at only " + full.getDestroySpeed(stone));
		helper.assertTrue(full.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()), "A charged drill cannot mine diamond ore");
		helper.succeed();
	}

	/** In 3x3 mode, breaking one block of a wall breaks the square around it, for the energy of nine blocks. */
	@GameTest
	public void drillMinesThreeByThree(GameTestHelper helper) {
		for (int x = 1; x <= 3; x++) {
			for (int y = 1; y <= 3; y++) {
				helper.setBlock(new BlockPos(x, y, 3), Blocks.STONE);
			}
		}
		ItemStack drill = charged(JugcraftTools.MINING_DRILL);
		drill.set(JugcraftTools.DRILL_MODE, MiningDrillItem.AREA);
		ServerPlayer player = miner(helper, drill);
		helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(2, 2, 3))), "The drill broke nothing");
		for (int x = 1; x <= 3; x++) {
			for (int y = 1; y <= 3; y++) {
				helper.assertBlockPresent(Blocks.AIR, new BlockPos(x, y, 3));
			}
		}
		long used = JugcraftTools.DRILL_CAPACITY - Chargeable.energy(player.getMainHandItem());
		helper.assertTrue(used == 9 * JugcraftTools.DRILL_ENERGY_PER_BLOCK, "The drill used " + used + " JE for nine blocks");
		helper.succeed();
	}

	/** Cutting the bottom log with the chainsaw fells the whole tree, branches too. */
	@GameTest
	public void chainsawFellsTree(GameTestHelper helper) {
		for (int y = 1; y <= 5; y++) {
			helper.setBlock(new BlockPos(2, y, 2), Blocks.OAK_LOG);
		}
		helper.setBlock(new BlockPos(3, 4, 3), Blocks.OAK_LOG);
		ServerPlayer player = miner(helper, charged(JugcraftTools.CHAINSAW));
		player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(2, 1, 2)));
		for (int y = 1; y <= 5; y++) {
			helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, y, 2));
		}
		helper.assertBlockPresent(Blocks.AIR, new BlockPos(3, 4, 3));
		helper.succeed();
	}

	/** A tick of rocket thrust costs its JE and cancels a fall. */
	@GameTest
	public void rocketPackThrustUsesEnergy(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemSlot(EquipmentSlot.CHEST, charged(JugcraftTools.ROCKET_PACK));
		player.fallDistance = 10;
		RocketPackItem.thrust(player);
		long left = Chargeable.energy(player.getItemBySlot(EquipmentSlot.CHEST));
		helper.assertTrue(left == RocketPackItem.CAPACITY - RocketPackItem.ENERGY_PER_TICK, "The pack holds " + left + " JE");
		helper.assertTrue(player.fallDistance == 0, "The fall was not cancelled");
		helper.succeed();
	}

	/** A capacity module doubles what a drill holds; a second triples it; a third does not fit. */
	@GameTest
	public void capacityModulesAddCharge(GameTestHelper helper) {
		ItemStack drill = new ItemStack(JugcraftTools.MINING_DRILL);
		for (int i = 1; i <= 2; i++) {
			helper.assertTrue(ToolUpgrades.fit(helper.getLevel(), drill, ToolUpgrades.Kind.CAPACITY) == ToolUpgrades.Result.FITTED,
					"Capacity module " + i + " did not fit");
		}
		helper.assertTrue(ToolUpgrades.fit(helper.getLevel(), drill, ToolUpgrades.Kind.CAPACITY) == ToolUpgrades.Result.FULL,
				"A third capacity module fitted");
		Chargeable.setEnergy(drill, Long.MAX_VALUE);
		helper.assertTrue(Chargeable.energy(drill) == 3 * JugcraftTools.DRILL_CAPACITY, "The drill holds " + Chargeable.energy(drill));
		helper.succeed();
	}

	/** Modules fit only the tools they are for, and silk touch and fortune exclude each other. */
	@GameTest
	public void modulesFitTheRightTools(GameTestHelper helper) {
		ItemStack chainsaw = new ItemStack(JugcraftTools.CHAINSAW);
		helper.assertTrue(ToolUpgrades.fit(helper.getLevel(), chainsaw, ToolUpgrades.Kind.RANGE) == ToolUpgrades.Result.WRONG_TOOL,
				"A range module fitted the chainsaw");
		ItemStack drill = new ItemStack(JugcraftTools.MINING_DRILL);
		helper.assertTrue(ToolUpgrades.fit(helper.getLevel(), drill, ToolUpgrades.Kind.SILK_TOUCH) == ToolUpgrades.Result.FITTED,
				"Silk touch did not fit the drill");
		helper.assertTrue(ToolUpgrades.fit(helper.getLevel(), drill, ToolUpgrades.Kind.FORTUNE) == ToolUpgrades.Result.CONFLICT,
				"Fortune fitted alongside silk touch");
		helper.assertTrue(!drill.getEnchantments().isEmpty(), "The drill has no enchantment after a silk touch module");
		helper.assertTrue(ToolUpgrades.fit(helper.getLevel(), new ItemStack(JugcraftTools.ROCKET_PACK), ToolUpgrades.Kind.OVERCLOCK)
				== ToolUpgrades.Result.WRONG_TOOL, "An overclock module fitted the rocket pack");
		helper.succeed();
	}

	/** With a range module, the drill's area mode breaks a 5x5 square for the energy of 25 blocks. */
	@GameTest
	public void rangeModuleMinesFiveByFive(GameTestHelper helper) {
		for (int x = 0; x <= 4; x++) {
			for (int y = 1; y <= 5; y++) {
				helper.setBlock(new BlockPos(x, y, 3), Blocks.STONE);
			}
		}
		ItemStack drill = charged(JugcraftTools.MINING_DRILL);
		drill.set(JugcraftTools.DRILL_MODE, MiningDrillItem.AREA);
		ToolUpgrades.fit(helper.getLevel(), drill, ToolUpgrades.Kind.RANGE);
		ServerPlayer player = miner(helper, drill);
		player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(2, 3, 3)));
		for (int x = 0; x <= 4; x++) {
			for (int y = 1; y <= 5; y++) {
				helper.assertBlockPresent(Blocks.AIR, new BlockPos(x, y, 3));
			}
		}
		long used = JugcraftTools.DRILL_CAPACITY - Chargeable.energy(player.getMainHandItem());
		helper.assertTrue(used == 25 * JugcraftTools.DRILL_ENERGY_PER_BLOCK, "The drill used " + used + " JE for 25 blocks");
		helper.succeed();
	}

	/** An overclock module speeds the drill up by half and doubles its JE per block. */
	@GameTest
	public void overclockModuleSpeedsUp(GameTestHelper helper) {
		ItemStack plain = charged(JugcraftTools.MINING_DRILL);
		ItemStack fast = charged(JugcraftTools.MINING_DRILL);
		ToolUpgrades.fit(helper.getLevel(), fast, ToolUpgrades.Kind.OVERCLOCK);
		BlockState stone = Blocks.STONE.defaultBlockState();
		float ratio = fast.getDestroySpeed(stone) / plain.getDestroySpeed(stone);
		helper.assertTrue(Math.abs(ratio - 1.5F) < 0.01F, "Overclocked speed ratio " + ratio);
		helper.assertTrue(((MiningDrillItem) fast.getItem()).energyPerBlock(fast) == 2 * JugcraftTools.DRILL_ENERGY_PER_BLOCK,
				"Overclocked JE per block " + ((MiningDrillItem) fast.getItem()).energyPerBlock(fast));
		helper.succeed();
	}

	/** Charcoal burns three quarters as long as coal in generators (tree farm wood power, slightly weakened). */
	@GameTest
	public void charcoalBurnsShorterThanCoal(GameTestHelper helper) {
		int coal = GeneratorFuels.burnTicks(new ItemStack(Items.COAL));
		int charcoal = GeneratorFuels.burnTicks(new ItemStack(Items.CHARCOAL));
		helper.assertTrue(coal == 1600 && charcoal == 1200, "Coal burns " + coal + " ticks and charcoal " + charcoal);
		helper.succeed();
	}

	// ------------------------------------------------------------------ advancements

	/** The quest line loads: its first and last steps exist, and the rocket pack step leads back to the root. */
	@GameTest
	public void advancementTreeLoads(GameTestHelper helper) {
		var advancements = helper.getLevel().getServer().getAdvancements();
		for (String id : List.of("root", "bronze", "steel", "charging_station", "upgrade", "rocket_pack")) {
			helper.assertTrue(advancements.get(Jugcraft.id(id)) != null, "Advancement jugcraft:" + id + " did not load");
		}
		var step = advancements.get(Jugcraft.id("rocket_pack"));
		int depth = 0;
		while (step != null && step.value().parent().isPresent() && depth < 20) {
			step = advancements.get(step.value().parent().get());
			depth++;
		}
		helper.assertTrue(step != null && step.id().equals(Jugcraft.id("root")), "The rocket pack step does not lead back to the root");
		helper.succeed();
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
