package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.FertilizerItem;
import io.github.jimbozoomer.jugcraft.chemistry.FluidFuels;
import io.github.jimbozoomer.jugcraft.chemistry.OilReservoirs;
import io.github.jimbozoomer.jugcraft.chemistry.PetroBlocks;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.chemistry.PetroItems;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.farming.JugcraftFarming;
import io.github.jimbozoomer.jugcraft.farming.SprinklerBlockEntity;
import io.github.jimbozoomer.jugcraft.fluid.ElectricPumpBlockEntity;
import io.github.jimbozoomer.jugcraft.fluid.FluidTankBlockEntity;
import io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids;
import io.github.jimbozoomer.jugcraft.kinetic.DynamoBlockEntity;
import io.github.jimbozoomer.jugcraft.kinetic.JugcraftKinetics;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.machine.LargeMachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.prospecting.OreSurvey;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.material.Fluid;
import io.github.jimbozoomer.jugcraft.fluid.FluidNetworks;
import net.minecraft.world.level.material.Fluids;

/**
 * In-game tests for the Chemistry branch's oil line (docs/branches/CHEMISTRY.md). Like {@link JugcraftGameTests},
 * they run on a headless server during {@code ./gradlew build}.
 */
public class PetroGameTests {
	/** Crude oil is a real fluid: its bucket holds it and a tinplate tank stores it like any other fluid. */
	@GameTest
	public void crudeOilFillsTanks(GameTestHelper helper) {
		PetroFluids.Entry oil = PetroFluids.CRUDE_OIL;
		helper.assertTrue(oil.bucket() instanceof BucketItem bucket && bucket.getContent() == oil.source(),
				"The crude oil bucket does not hold crude oil");
		BlockPos tank = new BlockPos(1, 1, 1);
		helper.setBlock(tank, JugcraftFluids.FLUID_TANK);
		FluidTankBlockEntity entity = helper.getBlockEntity(tank, FluidTankBlockEntity.class);
		try (Transaction transaction = Transaction.openOuter()) {
			long inserted = entity.storage.insert(FluidVariant.of(oil.source()), FluidConstants.BUCKET, transaction);
			helper.assertTrue(inserted == FluidConstants.BUCKET, "The tank took " + inserted + " droplets of crude oil");
			transaction.commit();
		}
		helper.assertTrue(entity.storage.variant.isOf(oil.source()), "The tank holds " + entity.storage.variant);
		helper.succeed();
	}

	/** Crude oil never makes new source blocks: two sources with a gap between them leave the gap flowing. */
	@GameTest(maxTicks = 200)
	public void crudeOilMakesNoNewSources(GameTestHelper helper) {
		for (int x = 0; x < 5; x++) {
			for (int z = 0; z < 3; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
		helper.setBlock(new BlockPos(1, 2, 1), PetroFluids.CRUDE_OIL.block());
		helper.setBlock(new BlockPos(3, 2, 1), PetroFluids.CRUDE_OIL.block());
		helper.runAfterDelay(160, () -> {
			var gap = helper.getLevel().getFluidState(helper.absolutePos(new BlockPos(2, 2, 1)));
			helper.assertTrue(gap.getType() == PetroFluids.CRUDE_OIL.flowing(), "The gap holds " + gap + ", not flowing oil");
			helper.succeed();
		});
	}

	/**
	 * Reservoirs are fixed by the seed: the same chunk always reads the same, roughly one chunk in twelve holds pumpable
	 * oil, and a reservoir runs dry for good. A pumpjack can't draw on shale.
	 */
	@GameTest
	public void oilReservoirsAreSeededAndFinite(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		int conventional = 0;
		ChunkPos found = null;
		// A patch of chunks far from every test structure, so taking oil here disturbs nothing else.
		for (int cx = 4000; cx < 4040; cx++) {
			for (int cz = 4000; cz < 4040; cz++) {
				ChunkPos chunk = new ChunkPos(cx, cz);
				OilReservoirs.Reservoir reservoir = OilReservoirs.get(level, chunk);
				helper.assertTrue(reservoir.equals(OilReservoirs.get(level, chunk)), "Chunk " + chunk + " reads differently twice");
				if (reservoir.kind() == OilReservoirs.Kind.CONVENTIONAL) {
					conventional++;
					found = found == null ? chunk : found;
				}
			}
		}
		// 1,600 chunks at 1 in 12 is about 133; allow a wide margin.
		helper.assertTrue(conventional > 60 && conventional < 260, conventional + " of 1,600 chunks hold pumpable oil");
		OilReservoirs.Reservoir before = OilReservoirs.get(level, found);
		helper.assertTrue(OilReservoirs.extract(level, found, OilReservoirs.Kind.SHALE, 1000) == 0, "Took shale oil from a conventional reservoir");
		int got = OilReservoirs.extract(level, found, OilReservoirs.Kind.CONVENTIONAL, (int) before.capacity() + 5000);
		helper.assertTrue(got == before.remaining(), "Took " + got + " mB from a reservoir with " + before.remaining());
		helper.assertTrue(OilReservoirs.get(level, found).isDry(), "The reservoir is not dry after taking everything");
		helper.assertTrue(OilReservoirs.extract(level, found, OilReservoirs.Kind.CONVENTIONAL, 1000) == 0, "A dry reservoir gave more oil");
		helper.succeed();
	}

	/** The prospector reports oil under the surveyed chunks. */
	@GameTest
	public void surveyFindsOil(GameTestHelper helper) {
		BlockPos center = helper.absolutePos(new BlockPos(2, 2, 2));
		OilReservoirs.overrideForTest(ChunkPos.containing(center), OilReservoirs.Kind.CONVENTIONAL, 120_000);
		List<OreSurvey.Reading> readings = OreSurvey.survey(helper.getLevel(), center, helper.getLevel().getRandom());
		helper.assertTrue(readings.stream().anyMatch(reading -> reading.label().equals("prospector.jugcraft.oil")),
				"No oil reading in " + readings);
		helper.succeed();
	}

	/**
	 * A powered pumpjack over pumpable oil fills its tank with crude oil, and the reservoir under it goes down by what
	 * it pumped. (Tests share chunks, so this one only checks that oil flows and is taken from the reservoir.)
	 */
	@GameTest(maxTicks = 200)
	public void pumpjackPumpsOil(GameTestHelper helper) {
		BlockPos master = new BlockPos(2, 1, 1);
		ChunkPos chunk = ChunkPos.containing(helper.absolutePos(master));
		OilReservoirs.overrideForTest(chunk, OilReservoirs.Kind.CONVENTIONAL, 200_000);
		MachineBlockEntity pumpjack = place(helper, MachineKind.PUMPJACK, master);
		long before = OilReservoirs.get(helper.getLevel(), chunk).remaining();
		helper.succeedWhen(() -> {
			int oil = pumpjack.tanks().output(0).millibuckets();
			helper.assertTrue(oil >= 40 && pumpjack.tanks().output(0).variant.isOf(PetroFluids.CRUDE_OIL.source()),
					"The pumpjack holds " + oil + " mB");
			long after = OilReservoirs.get(helper.getLevel(), chunk).remaining();
			helper.assertTrue(after <= before - 40, "The reservoir went from " + before + " to " + after + " mB");
		});
	}

	/** Places a multi-block machine facing north with all its parts, charged full; returns its block entity. */
	private static MachineBlockEntity place(GameTestHelper helper, MachineKind kind, BlockPos master) {
		LargeMachineBlock block = (LargeMachineBlock) JugcraftMachines.MACHINES.get(kind);
		helper.setBlock(master, block.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
		block.setPlacedBy(helper.getLevel(), helper.absolutePos(master), helper.getBlockState(master), null, ItemStack.EMPTY);
		EnergyStorage storage = EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(master), Direction.UP);
		helper.assertTrue(storage instanceof SimpleEnergyStorage, "The " + kind.id + " takes no power");
		((SimpleEnergyStorage) storage).setAmount(storage.getCapacity());
		return helper.getBlockEntity(master, MachineBlockEntity.class);
	}

	/** Places and forms a multi-block machine that runs without power (no energy storage to fill). */
	private static MachineBlockEntity placeUnpowered(GameTestHelper helper, MachineKind kind, BlockPos master) {
		LargeMachineBlock block = (LargeMachineBlock) JugcraftMachines.MACHINES.get(kind);
		helper.setBlock(master, block.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
		block.setPlacedBy(helper.getLevel(), helper.absolutePos(master), helper.getBlockState(master), null, ItemStack.EMPTY);
		return helper.getBlockEntity(master, MachineBlockEntity.class);
	}

	/** The extractor's water tank takes water from outside but not lava, and its oil tank takes nothing in. */
	@GameTest
	public void extractorTanksOnlyTakeWhatTheyUse(GameTestHelper helper) {
		MachineBlockEntity extractor = place(helper, MachineKind.OIL_SAND_EXTRACTOR, new BlockPos(3, 1, 1));
		Storage<FluidVariant> tanks = FluidStorage.SIDED.find(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 1)), Direction.NORTH);
		helper.assertTrue(tanks != null, "The extractor has no fluid storage");
		try (Transaction transaction = Transaction.openOuter()) {
			long water = tanks.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction);
			long lava = tanks.insert(FluidVariant.of(Fluids.LAVA), FluidConstants.BUCKET, transaction);
			long oil = tanks.insert(FluidVariant.of(PetroFluids.CRUDE_OIL.source()), FluidConstants.BUCKET, transaction);
			helper.assertTrue(water == FluidConstants.BUCKET, "The extractor took " + water + " droplets of water");
			helper.assertTrue(lava == 0 && oil == 0, "The extractor took lava (" + lava + ") or crude oil (" + oil + ")");
			transaction.abort();
		}
		helper.assertTrue(extractor.tanks().input(0).isResourceBlank(), "An aborted insert left fluid behind");
		helper.succeed();
	}

	/** A powered oil sand extractor with water turns a block of oil sand into 500 mB of crude oil and a block of sand. */
	@GameTest(maxTicks = 300)
	public void extractorWashesOilFromOilSand(GameTestHelper helper) {
		MachineBlockEntity extractor = place(helper, MachineKind.OIL_SAND_EXTRACTOR, new BlockPos(3, 1, 1));
		extractor.tanks().input(0).fill(Fluids.WATER, 1000);
		extractor.setItem(0, new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("oil_sand"))));
		helper.succeedWhen(() -> {
			int oil = extractor.tanks().output(0).millibuckets();
			helper.assertTrue(oil == 500 && extractor.tanks().output(0).variant.isOf(PetroFluids.CRUDE_OIL.source()),
					"The extractor holds " + oil + " mB of oil");
			helper.assertTrue(extractor.tanks().input(0).millibuckets() == 750, "Water left: " + extractor.tanks().input(0).millibuckets());
			helper.assertTrue(extractor.getItem(1).is(Items.SAND), "Output slot holds " + extractor.getItem(1));
		});
	}

	/**
	 * A heavy pump on water pushes 1,000 mB a tick through steel pipes: five buckets in well under the time a bronze
	 * line (250 mB a tick) would need.
	 */
	@GameTest(maxTicks = 12)
	public void heavyPumpFillsFastThroughSteelPipes(GameTestHelper helper) {
		helper.setBlock(new BlockPos(1, 1, 3), Blocks.WATER);
		BlockPos pump = new BlockPos(1, 2, 3);
		helper.setBlock(pump, JugcraftFluids.HEAVY_PUMP);
		ElectricPumpBlockEntity entity = helper.getBlockEntity(pump, ElectricPumpBlockEntity.class);
		entity.energy().setAmount(entity.energy().getCapacity());
		for (int x = 2; x <= 3; x++) {
			helper.setBlock(new BlockPos(x, 2, 3), JugcraftFluids.STEEL_FLUID_PIPE);
		}
		BlockPos tank = new BlockPos(4, 2, 3);
		helper.setBlock(tank, JugcraftFluids.FLUID_TANK);
		FluidTankBlockEntity tankEntity = helper.getBlockEntity(tank, FluidTankBlockEntity.class);
		helper.succeedWhen(() -> helper.assertTrue(tankEntity.storage.amount >= 5 * FluidConstants.BUCKET,
				"The tank holds only " + tankEntity.storage.amount / 81 + " mB"));
	}

	/** A pipe line carries as much as its slowest pipe: one bronze pipe in a steel line holds it to 250 mB a tick. */
	@GameTest(maxTicks = 12)
	public void bronzePipeLimitsASteelLine(GameTestHelper helper) {
		helper.setBlock(new BlockPos(1, 1, 3), Blocks.WATER);
		BlockPos pump = new BlockPos(1, 2, 3);
		helper.setBlock(pump, JugcraftFluids.HEAVY_PUMP);
		ElectricPumpBlockEntity entity = helper.getBlockEntity(pump, ElectricPumpBlockEntity.class);
		entity.energy().setAmount(entity.energy().getCapacity());
		helper.setBlock(new BlockPos(2, 2, 3), JugcraftFluids.STEEL_FLUID_PIPE);
		helper.setBlock(new BlockPos(3, 2, 3), JugcraftFluids.BRONZE_FLUID_PIPE);
		BlockPos tank = new BlockPos(4, 2, 3);
		helper.setBlock(tank, JugcraftFluids.FLUID_TANK);
		FluidTankBlockEntity tankEntity = helper.getBlockEntity(tank, FluidTankBlockEntity.class);
		helper.runAfterDelay(10, () -> {
			long mb = tankEntity.storage.amount / 81;
			helper.assertTrue(mb > 0 && mb <= 10 * 250, "The tank got " + mb + " mB in 10 ticks through a bronze pipe");
			helper.succeed();
		});
	}

	/**
	 * The distillation tower splits a bucket of crude oil into its four fractions, and each comes out only at its own
	 * height: a tank against the front two blocks up (the diesel draw-off) fills with diesel and nothing else.
	 */
	@GameTest(maxTicks = 300)
	public void distillationTowerSplitsCrude(GameTestHelper helper) {
		BlockPos master = new BlockPos(4, 1, 2);
		MachineBlockEntity tower = place(helper, MachineKind.DISTILLATION_TOWER, master);
		BlockPos tank = master.above(2).north();
		helper.setBlock(tank, JugcraftFluids.FLUID_TANK);
		FluidTankBlockEntity diesel = helper.getBlockEntity(tank, FluidTankBlockEntity.class);
		tower.tanks().input(0).fill(PetroFluids.CRUDE_OIL.source(), 1000);
		helper.succeedWhen(() -> {
			helper.assertTrue(tower.tanks().output(0).has(PetroFluids.REFINERY_GAS.fluid(), 100), "No refinery gas");
			helper.assertTrue(tower.tanks().output(1).has(PetroFluids.NAPHTHA.source(), 250), "No naphtha");
			helper.assertTrue(tower.tanks().output(3).has(PetroFluids.HEAVY_FUEL_OIL.source(), 250), "No heavy fuel oil");
			helper.assertTrue(diesel.storage.variant.isOf(PetroFluids.DIESEL.source()) && diesel.storage.amount == 400 * 81,
					"The tank at the diesel draw-off holds " + diesel.storage.amount / 81 + " mB of " + diesel.storage.variant);
			helper.assertTrue(tower.tanks().output(2).isResourceBlank(), "Diesel stayed in the tower");
		});
	}

	/** The catalytic cracker turns heavy fuel oil, water and one catalyst into diesel, naphtha and refinery gas. */
	@GameTest(maxTicks = 300)
	public void crackerCracksHeavyFuelOil(GameTestHelper helper) {
		MachineBlockEntity cracker = place(helper, MachineKind.CATALYTIC_CRACKER, new BlockPos(4, 1, 2));
		cracker.tanks().input(0).fill(PetroFluids.HEAVY_FUEL_OIL.source(), 1000);
		cracker.tanks().input(1).fill(Fluids.WATER, 1000);
		cracker.setItem(0, new ItemStack(PetroItems.CRACKING_CATALYST, 2));
		helper.succeedWhen(() -> {
			helper.assertTrue(cracker.tanks().output(0).has(PetroFluids.DIESEL.source(), 500), "No diesel");
			helper.assertTrue(cracker.tanks().output(1).has(PetroFluids.NAPHTHA.source(), 300), "No naphtha");
			helper.assertTrue(cracker.tanks().output(2).has(PetroFluids.REFINERY_GAS.fluid(), 200), "No refinery gas");
			helper.assertTrue(cracker.getItem(0).getCount() == 1, "The cracker used " + (2 - cracker.getItem(0).getCount()) + " catalysts");
			helper.assertTrue(cracker.tanks().input(1).millibuckets() == 750, "Water left: " + cracker.tanks().input(1).millibuckets());
		});
	}

	/** The vacuum distillation unit turns a bucket of heavy fuel oil into 400 mB of lubricant and two asphalt binder. */
	@GameTest(maxTicks = 300)
	public void vacuumUnitMakesLubricantAndAsphalt(GameTestHelper helper) {
		MachineBlockEntity unit = place(helper, MachineKind.VACUUM_DISTILLATION_UNIT, new BlockPos(4, 1, 2));
		unit.tanks().input(0).fill(PetroFluids.HEAVY_FUEL_OIL.source(), 1000);
		helper.succeedWhen(() -> {
			helper.assertTrue(unit.tanks().output(0).has(PetroFluids.LUBRICANT.source(), 400), "No lubricant");
			helper.assertTrue(unit.getItem(0).is(PetroItems.ASPHALT_BINDER) && unit.getItem(0).getCount() == 2,
					"The unit holds " + unit.getItem(0));
			helper.assertTrue(unit.tanks().input(0).isResourceBlank(), "Heavy fuel oil left over");
		});
	}

	/** The catalytic reformer turns a bucket of naphtha into 900 mB of gasoline and 100 mB of refinery gas. */
	@GameTest(maxTicks = 300)
	public void reformerMakesGasoline(GameTestHelper helper) {
		MachineBlockEntity reformer = place(helper, MachineKind.CATALYTIC_REFORMER, new BlockPos(4, 1, 2));
		reformer.tanks().input(0).fill(PetroFluids.NAPHTHA.source(), 1000);
		helper.succeedWhen(() -> {
			helper.assertTrue(reformer.tanks().output(0).has(PetroFluids.GASOLINE.source(), 900), "No gasoline");
			helper.assertTrue(reformer.tanks().output(1).has(PetroFluids.REFINERY_GAS.fluid(), 100), "No refinery gas");
		});
	}

	/** The chemical mixer stirs two sand and a dried kelp into a bucket of water to make a bucket of fracking fluid. */
	@GameTest(maxTicks = 200)
	public void mixerMakesFrackingFluid(GameTestHelper helper) {
		MachineBlockEntity mixer = place(helper, MachineKind.CHEMICAL_MIXER, new BlockPos(4, 1, 2));
		mixer.tanks().input(0).fill(Fluids.WATER, 1000);
		mixer.setItem(0, new ItemStack(Items.SAND, 2));
		mixer.setItem(1, new ItemStack(Items.DRIED_KELP));
		helper.succeedWhen(() -> {
			helper.assertTrue(mixer.tanks().output(0).has(PetroFluids.FRACKING_FLUID.source(), 1000), "No fracking fluid");
			helper.assertTrue(mixer.getItem(0).isEmpty() && mixer.getItem(1).isEmpty(), "The mixer kept its sand or kelp");
		});
	}

	/**
	 * A powered fracking rig over shale pumps fracking fluid down and brings up crude oil, refinery gas and flowback
	 * water, taking the oil from the shale reservoir. (Tests share chunks, so this only checks that it flows.)
	 */
	@GameTest(maxTicks = 200)
	public void frackingRigFreesShaleOil(GameTestHelper helper) {
		BlockPos master = new BlockPos(4, 1, 1);
		ChunkPos chunk = ChunkPos.containing(helper.absolutePos(master));
		OilReservoirs.overrideForTest(chunk, OilReservoirs.Kind.SHALE, 400_000);
		MachineBlockEntity rig = place(helper, MachineKind.FRACKING_RIG, master);
		Storage<FluidVariant> tanks = FluidStorage.SIDED.find(helper.getLevel(), helper.absolutePos(master), Direction.NORTH);
		try (Transaction transaction = Transaction.openOuter()) {
			long accepted = tanks.insert(FluidVariant.of(PetroFluids.FRACKING_FLUID.source()), 2 * FluidConstants.BUCKET, transaction);
			helper.assertTrue(accepted == 2 * FluidConstants.BUCKET, "The rig took " + accepted / 81 + " mB of fracking fluid");
			transaction.commit();
		}
		long before = OilReservoirs.get(helper.getLevel(), chunk).remaining();
		helper.succeedWhen(() -> {
			int oil = rig.tanks().output(0).millibuckets();
			helper.assertTrue(oil >= 30 && rig.tanks().output(0).variant.isOf(PetroFluids.CRUDE_OIL.source()), "Crude oil: " + oil);
			helper.assertTrue(rig.tanks().output(1).variant.isOf(PetroFluids.REFINERY_GAS.fluid()), "No refinery gas");
			helper.assertTrue(rig.tanks().output(2).millibuckets() >= 15, "Flowback: " + rig.tanks().output(2).millibuckets());
			helper.assertTrue(OilReservoirs.get(helper.getLevel(), chunk).remaining() <= before - 40, "The shale gave nothing");
		});
	}

	/** The flowback treatment unit turns a bucket of flowback water into 750 mB of clean water and a salt. */
	@GameTest(maxTicks = 200)
	public void treatmentCleansFlowback(GameTestHelper helper) {
		MachineBlockEntity unit = place(helper, MachineKind.FLOWBACK_TREATMENT_UNIT, new BlockPos(4, 1, 2));
		unit.tanks().input(0).fill(PetroFluids.FLOWBACK_WATER.source(), 1000);
		helper.succeedWhen(() -> {
			helper.assertTrue(unit.tanks().output(0).has(Fluids.WATER, 750), "Water: " + unit.tanks().output(0).millibuckets());
			helper.assertTrue(unit.getItem(0).is(BuiltInRegistries.ITEM.getValue(Jugcraft.id("salt"))), "No salt: " + unit.getItem(0));
		});
	}

	/** The diesel generator burns 1 mB of diesel a tick for 256 JE, and refuses crude oil. */
	@GameTest(maxTicks = 200)
	public void dieselGeneratorBurnsDiesel(GameTestHelper helper) {
		BlockPos master = new BlockPos(4, 1, 2);
		MachineBlockEntity generator = place(helper, MachineKind.DIESEL_GENERATOR, master);
		SimpleEnergyStorage energy = (SimpleEnergyStorage) EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(master), Direction.UP);
		energy.setAmount(0);
		Storage<FluidVariant> tanks = FluidStorage.SIDED.find(helper.getLevel(), helper.absolutePos(master), Direction.NORTH);
		helper.assertTrue(tanks != null, "The generator has no fluid storage");
		try (Transaction transaction = Transaction.openOuter()) {
			long oil = tanks.insert(FluidVariant.of(PetroFluids.CRUDE_OIL.source()), FluidConstants.BUCKET, transaction);
			long diesel = tanks.insert(FluidVariant.of(PetroFluids.DIESEL.source()), FluidConstants.BUCKET, transaction);
			helper.assertTrue(oil == 0, "The generator took " + oil + " droplets of crude oil");
			helper.assertTrue(diesel == FluidConstants.BUCKET, "The generator took " + diesel + " droplets of diesel");
			transaction.commit();
		}
		helper.runAfterDelay(40, () -> {
			int left = generator.tanks().input(0).millibuckets();
			helper.assertTrue(left < 1000 && left >= 950, "Diesel left: " + left);
			helper.assertTrue(energy.getAmount() == (1000L - left) * FluidFuels.DIESEL, "Energy " + energy.getAmount() + " for " + (1000 - left) + " mB");
			helper.succeed();
		});
	}

	/** The gas turbine will not run without lubricant; with it, it burns gasoline at 384 JE/mB. */
	@GameTest(maxTicks = 200)
	public void gasTurbineNeedsLubricant(GameTestHelper helper) {
		BlockPos master = new BlockPos(5, 1, 2);
		MachineBlockEntity turbine = place(helper, MachineKind.GAS_TURBINE, master);
		SimpleEnergyStorage energy = (SimpleEnergyStorage) EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(master), Direction.UP);
		energy.setAmount(0);
		turbine.tanks().input(0).fill(PetroFluids.GASOLINE.source(), 1000);
		helper.runAfterDelay(20, () -> {
			helper.assertTrue(energy.getAmount() == 0, "Ran dry of lubricant: " + energy.getAmount() + " JE");
			helper.assertTrue(turbine.tanks().input(0).millibuckets() == 1000, "Burnt gasoline without lubricant");
			turbine.tanks().input(1).fill(PetroFluids.LUBRICANT.source(), 100);
			helper.runAfterDelay(60, () -> {
				int burnt = 1000 - turbine.tanks().input(0).millibuckets();
				helper.assertTrue(burnt > 0, "Burnt no gasoline");
				long expected = (long) burnt * FluidFuels.GASOLINE;
				helper.assertTrue(energy.getAmount() <= expected && energy.getAmount() > expected - MachineKind.TURBINE_OUTPUT,
						"Energy " + energy.getAmount() + " for " + burnt + " mB of gasoline");
				int lubricant = turbine.tanks().input(1).millibuckets();
				helper.assertTrue(lubricant < 100 && lubricant >= 95, "Lubricant left: " + lubricant);
				helper.succeed();
			});
		});
	}

	/** The polymerization reactor turns a bucket of refinery gas into four plastic pellets. */
	@GameTest(maxTicks = 300)
	public void reactorMakesPlasticPellets(GameTestHelper helper) {
		MachineBlockEntity reactor = place(helper, MachineKind.POLYMERIZATION_REACTOR, new BlockPos(4, 1, 2));
		reactor.tanks().input(0).fill(PetroFluids.REFINERY_GAS.fluid(), 1000);
		helper.succeedWhen(() -> {
			helper.assertTrue(reactor.getItem(0).is(PetroItems.PLASTIC_PELLETS) && reactor.getItem(0).getCount() == 4,
					"Pellets: " + reactor.getItem(0));
			helper.assertTrue(reactor.tanks().input(0).isResourceBlank(), "Gas left: " + reactor.tanks().input(0).millibuckets());
		});
	}

	/**
	 * Synthetic rubber (batch 14): the chemical reactor cracks a bucket of naphtha into 500 mB of butadiene, and the
	 * polymerization reactor turns 500 mB of butadiene into four rubber.
	 */
	@GameTest(maxTicks = 300)
	public void naphthaBecomesRubber(GameTestHelper helper) {
		MachineBlockEntity cracker = place(helper, MachineKind.CHEMICAL_REACTOR, new BlockPos(2, 1, 1));
		cracker.tanks().input(0).fill(PetroFluids.NAPHTHA.source(), 1000);
		MachineBlockEntity polymerizer = place(helper, MachineKind.POLYMERIZATION_REACTOR, new BlockPos(5, 1, 4));
		polymerizer.tanks().input(0).fill(PetroFluids.BUTADIENE.fluid(), 500);
		helper.succeedWhen(() -> {
			helper.assertTrue(cracker.tanks().output(0).has(PetroFluids.BUTADIENE.fluid(), 500),
					"Butadiene: " + cracker.tanks().output(0).millibuckets());
			ItemStack rubber = polymerizer.getItem(0);
			helper.assertTrue(rubber.is(PetroItems.RUBBER) && rubber.getCount() == 4, "Rubber: " + rubber);
		});
	}

	/** All three asphalt blocks speed up walking, and need a pickaxe. */
	@GameTest
	public void asphaltIsFasterToWalkOn(GameTestHelper helper) {
		for (Block block : List.of(PetroBlocks.ASPHALT, PetroBlocks.ASPHALT_SLAB, PetroBlocks.ASPHALT_ROAD_LINE)) {
			helper.assertTrue(block.getSpeedFactor() == PetroBlocks.ASPHALT_SPEED, block + " speed " + block.getSpeedFactor());
			helper.assertTrue(block.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE), block + " is not mined with a pickaxe");
		}
		helper.succeed();
	}

	/** The diesel engine turns a dynamo behind its upper right back block, burning diesel only for what it delivers. */
	@GameTest(maxTicks = 200)
	public void dieselEngineTurnsADynamo(GameTestHelper helper) {
		BlockPos master = new BlockPos(4, 1, 1);
		// Part 11 is one block right, one up and two back of the master; the shaft leaves its back face.
		BlockPos dynamoPos = master.offset(-1, 1, 3);
		helper.setBlock(dynamoPos, JugcraftKinetics.DYNAMO);
		DynamoBlockEntity dynamo = helper.getBlockEntity(dynamoPos, DynamoBlockEntity.class);
		MachineBlockEntity engine = placeUnpowered(helper, MachineKind.DIESEL_ENGINE, master);
		engine.tanks().input(0).fill(PetroFluids.DIESEL.source(), 1000);
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(dynamo.energy().getAmount() > 0, "The dynamo made no JE");
			int burnt = 1000 - engine.tanks().input(0).millibuckets();
			// The dynamo takes at most 128 KE/t (half a millibucket of diesel), not the engine's full 512: about
			// 20 mB in 40 ticks plus the 2 mB the engine holds in hand, far below the 80 mB of full output.
			long most = 40L * DynamoBlockEntity.RATE / FluidFuels.DIESEL + 3;
			helper.assertTrue(burnt > 0 && burnt <= most, "Diesel burnt in 40 ticks: " + burnt + " mB (at most " + most + ")");
			helper.succeed();
		});
	}

	/**
	 * The advanced combustion engine burns gasoline into a magnet dynamo behind its master block, only as fast as the
	 * dynamo takes it, and refuses heavy fuel oil.
	 */
	@GameTest(maxTicks = 100)
	public void advancedEngineTurnsAMagnetDynamo(GameTestHelper helper) {
		BlockPos master = new BlockPos(4, 1, 1);
		BlockPos dynamoPos = master.south();
		helper.setBlock(dynamoPos, JugcraftKinetics.MAGNET_DYNAMO);
		DynamoBlockEntity dynamo = helper.getBlockEntity(dynamoPos, DynamoBlockEntity.class);
		MachineBlockEntity engine = placeUnpowered(helper, MachineKind.ADVANCED_ENGINE, master);
		helper.assertTrue(FluidFuels.jePerMb(MachineKind.ADVANCED_ENGINE, PetroFluids.HEAVY_FUEL_OIL.source()) == 0,
				"The advanced engine burns heavy fuel oil");
		engine.tanks().input(0).fill(PetroFluids.GASOLINE.source(), 1000);
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(dynamo.energy().getAmount() > 0, "The dynamo made no JE");
			int burnt = 1000 - engine.tanks().input(0).millibuckets();
			// The magnet dynamo takes at most 512 KE/t: about 46 mB of gasoline in 40 ticks, plus what the engine holds.
			long most = 40L * DynamoBlockEntity.MAGNET.rate() / FluidFuels.ADVANCED_GASOLINE + 4;
			helper.assertTrue(burnt > 0 && burnt <= most, "Gasoline burnt in 40 ticks: " + burnt + " mB (at most " + most + ")");
			helper.succeed();
		});
	}

	/** The advanced solar panel places its pedestal and the 3x3 layer of cells above it, and holds 400,000 JE. */
	@GameTest
	public void advancedSolarPanelFormsAndStores(GameTestHelper helper) {
		BlockPos master = new BlockPos(3, 1, 3);
		MachineBlockEntity panel = placeUnpowered(helper, MachineKind.ADVANCED_SOLAR_PANEL, master);
		MachineBlock block = JugcraftMachines.MACHINES.get(MachineKind.ADVANCED_SOLAR_PANEL);
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				helper.assertTrue(helper.getBlockState(master.offset(x, 1, z)).is(block), "No cells at " + x + ", " + z);
			}
		}
		EnergyStorage storage = EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(master), Direction.NORTH);
		helper.assertTrue(storage != null && storage.supportsExtraction() && !storage.supportsInsertion(),
				"The pedestal must only give power");
		helper.assertTrue(storage.getCapacity() == 400_000, "Capacity is " + storage.getCapacity());
		helper.succeed();
	}

	/** The chemical mixer dissolves two salt in a bucket of water to make a bucket of brine. */
	@GameTest(maxTicks = 200)
	public void mixerMakesBrine(GameTestHelper helper) {
		MachineBlockEntity mixer = place(helper, MachineKind.CHEMICAL_MIXER, new BlockPos(4, 1, 2));
		mixer.tanks().input(0).fill(Fluids.WATER, 1000);
		mixer.setItem(0, new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("salt")), 2));
		helper.succeedWhen(() -> helper.assertTrue(mixer.tanks().output(0).has(PetroFluids.BRINE.source(), 1000), "No brine"));
	}

	/** The electrolytic cell splits a bucket of brine into 250 mB of chlorine, 250 mB of hydrogen and 500 mB of lye. */
	@GameTest(maxTicks = 400)
	public void cellSplitsBrine(GameTestHelper helper) {
		MachineBlockEntity cell = place(helper, MachineKind.ELECTROLYTIC_CELL, new BlockPos(4, 1, 2));
		cell.tanks().input(0).fill(PetroFluids.BRINE.source(), 1000);
		helper.succeedWhen(() -> {
			helper.assertTrue(cell.tanks().output(0).has(PetroFluids.CHLORINE.fluid(), 250), "Chlorine: " + cell.tanks().output(0).millibuckets());
			helper.assertTrue(cell.tanks().output(1).has(PetroFluids.HYDROGEN.fluid(), 250), "Hydrogen: " + cell.tanks().output(1).millibuckets());
			helper.assertTrue(cell.tanks().output(2).has(PetroFluids.LYE.source(), 500), "Lye: " + cell.tanks().output(2).millibuckets());
		});
	}

	/** The chemical reactor turns two sulfur dust and a bucket of water into a bucket of sulfuric acid. */
	@GameTest(maxTicks = 300)
	public void reactorMakesSulfuricAcid(GameTestHelper helper) {
		MachineBlockEntity reactor = place(helper, MachineKind.CHEMICAL_REACTOR, new BlockPos(4, 1, 2));
		reactor.tanks().input(0).fill(Fluids.WATER, 1000);
		reactor.setItem(0, new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("sulfur_dust")), 2));
		helper.succeedWhen(() -> {
			helper.assertTrue(reactor.tanks().output(0).has(PetroFluids.SULFURIC_ACID.source(), 1000), "No sulfuric acid");
			helper.assertTrue(reactor.getItem(0).isEmpty(), "The reactor kept its sulfur");
		});
	}

	/** The Bayer route: a bauxite digested in 250 mB of lye gives two alumina, and the electrolytic cell smelts two
	 * alumina with a coke anode into two aluminum ingots. */
	@GameTest(maxTicks = 400)
	public void bayerRouteMakesAluminum(GameTestHelper helper) {
		MachineBlockEntity reactor = place(helper, MachineKind.CHEMICAL_REACTOR, new BlockPos(2, 1, 1));
		reactor.tanks().input(0).fill(PetroFluids.LYE.source(), 250);
		reactor.setItem(0, new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("bauxite"))));
		MachineBlockEntity cell = place(helper, MachineKind.ELECTROLYTIC_CELL, new BlockPos(5, 1, 4));
		cell.setItem(0, new ItemStack(PetroItems.ALUMINA, 2));
		cell.setItem(1, new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("coke"))));
		helper.succeedWhen(() -> {
			helper.assertTrue(reactor.getItem(reactor.kind().outputSlot()).is(PetroItems.ALUMINA)
					&& reactor.getItem(reactor.kind().outputSlot()).getCount() == 2, "Alumina: " + reactor.getItem(reactor.kind().outputSlot()));
			ItemStack ingots = cell.getItem(cell.kind().outputSlot());
			helper.assertTrue(ingots.is(BuiltInRegistries.ITEM.getValue(Jugcraft.id("aluminum_ingot"))) && ingots.getCount() == 2,
					"Aluminum: " + ingots);
		});
	}

	/** The chemical reactor treats two phosphate with 250 mB of sulfuric acid to make four fertilizer. */
	@GameTest(maxTicks = 300)
	public void reactorMakesFertilizer(GameTestHelper helper) {
		MachineBlockEntity reactor = place(helper, MachineKind.CHEMICAL_REACTOR, new BlockPos(4, 1, 2));
		reactor.tanks().input(0).fill(PetroFluids.SULFURIC_ACID.source(), 250);
		reactor.setItem(0, new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("phosphate")), 2));
		helper.succeedWhen(() -> {
			ItemStack out = reactor.getItem(reactor.kind().outputSlot());
			helper.assertTrue(out.is(PetroItems.FERTILIZER) && out.getCount() == 4, "Fertilizer: " + out);
		});
	}

	/** The lithography station etches a wafer and two copper wire with 100 mB of sulfuric acid into four microchips. */
	@GameTest(maxTicks = 400)
	public void lithographyMakesMicrochips(GameTestHelper helper) {
		MachineBlockEntity station = place(helper, MachineKind.LITHOGRAPHY_STATION, new BlockPos(5, 1, 2));
		station.tanks().input(0).fill(PetroFluids.SULFURIC_ACID.source(), 1_000);
		station.setItem(0, new ItemStack(PetroItems.SILICON_WAFER));
		station.setItem(1, new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("copper_wire")), 2));
		helper.succeedWhen(() -> {
			ItemStack out = station.getItem(station.kind().outputSlot());
			helper.assertTrue(out.is(PetroItems.MICROCHIP) && out.getCount() == 4, "Microchips: " + out);
			helper.assertTrue(station.tanks().input(0).millibuckets() == 900, "Acid left: " + station.tanks().input(0).millibuckets());
		});
	}

	// ------------------------------------------------------------------ nitrogen chemistry (batch 12)

	/**
	 * The air separation unit needs no input: powered, it fills its first tank with nitrogen and its second with
	 * oxygen, four parts to one.
	 */
	@GameTest(maxTicks = 200)
	public void airSeparationMakesNitrogenAndOxygen(GameTestHelper helper) {
		MachineBlockEntity unit = place(helper, MachineKind.AIR_SEPARATION_UNIT, new BlockPos(4, 1, 2));
		helper.succeedWhen(() -> {
			int nitrogen = unit.tanks().output(0).millibuckets();
			int oxygen = unit.tanks().output(1).millibuckets();
			helper.assertTrue(unit.tanks().output(0).has(PetroFluids.NITROGEN.fluid(), 400), "Nitrogen: " + nitrogen);
			helper.assertTrue(unit.tanks().output(1).has(PetroFluids.OXYGEN.fluid(), 100), "Oxygen: " + oxygen);
			helper.assertTrue(nitrogen == 4 * oxygen, nitrogen + " mB nitrogen to " + oxygen + " mB oxygen");
			int argon = unit.tanks().output(2).millibuckets();
			helper.assertTrue(unit.tanks().output(2).has(PetroFluids.ARGON.fluid(), 1) && argon * 2 <= oxygen / 2 + 1,
					"Argon: " + argon + " mB to " + oxygen + " mB oxygen");
		});
	}

	/** Pipes the boost gas into a machine through the fluid API, as a pipe would. */
	private static void feedGas(GameTestHelper helper, BlockPos pos, Fluid gas, int mb) {
		Storage<FluidVariant> storage = FluidStorage.SIDED.find(helper.getLevel(), helper.absolutePos(pos), Direction.UP);
		helper.assertTrue(storage != null, "No fluid storage at " + pos);
		try (Transaction transaction = Transaction.openOuter()) {
			long in = storage.insert(FluidVariant.of(gas), mb * FluidNetworks.DROPLETS_PER_MB, transaction);
			helper.assertTrue(in == mb * FluidNetworks.DROPLETS_PER_MB, "Only " + in + " droplets of gas went in");
			transaction.commit();
		}
	}

	/**
	 * Oxygen blown into the steel foundry doubles its speed: with oxygen, a steel ingot comes out well before the 400
	 * ticks the foundry takes without, and some of the oxygen is used.
	 */
	@GameTest(maxTicks = 300)
	public void oxygenSpeedsUpTheSteelFoundry(GameTestHelper helper) {
		BlockPos master = new BlockPos(2, 1, 2);
		MachineBlockEntity foundry = placeUnpowered(helper, MachineKind.STEEL_FOUNDRY, master);
		foundry.setItem(0, new ItemStack(Items.IRON_INGOT));
		foundry.setItem(1, new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("coke"))));
		feedGas(helper, master, PetroFluids.OXYGEN.fluid(), 1_000);
		helper.succeedWhen(() -> {
			ItemStack output = foundry.getItem(MachineKind.STEEL_FOUNDRY.outputSlot());
			helper.assertTrue(output.is(BuiltInRegistries.ITEM.getValue(Jugcraft.id("steel_ingot"))), "Foundry output: " + output);
		});
	}

	/** Argon around the crystal grower's melt doubles its speed too. */
	@GameTest(maxTicks = 300)
	public void argonSpeedsUpTheCrystalGrower(GameTestHelper helper) {
		BlockPos master = new BlockPos(2, 1, 2);
		MachineBlockEntity grower = place(helper, MachineKind.CRYSTAL_GROWER, master);
		grower.setItem(0, new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("silicon")), 4));
		grower.setItem(1, new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("phosphate"))));
		feedGas(helper, master, PetroFluids.ARGON.fluid(), 1_000);
		helper.succeedWhen(() -> {
			ItemStack output = grower.getItem(MachineKind.CRYSTAL_GROWER.outputSlot());
			helper.assertTrue(output.is(PetroItems.SILICON_BOULE), "Grower output: " + output);
		});
	}

	/**
	 * The synthesis converter makes 200 mB of ammonia from 300 mB of hydrogen and 100 mB of nitrogen (Haber-Bosch), and
	 * 200 mB of nitric acid from 100 mB of ammonia, 200 mB of oxygen and 100 mB of water (Ostwald).
	 */
	@GameTest(maxTicks = 300)
	public void converterMakesAmmoniaAndNitricAcid(GameTestHelper helper) {
		MachineBlockEntity haber = place(helper, MachineKind.SYNTHESIS_CONVERTER, new BlockPos(5, 1, 1));
		haber.tanks().input(0).fill(PetroFluids.HYDROGEN.fluid(), 300);
		haber.tanks().input(1).fill(PetroFluids.NITROGEN.fluid(), 100);
		MachineBlockEntity ostwald = place(helper, MachineKind.SYNTHESIS_CONVERTER, new BlockPos(5, 1, 4));
		ostwald.tanks().input(0).fill(PetroFluids.AMMONIA.fluid(), 100);
		ostwald.tanks().input(1).fill(PetroFluids.OXYGEN.fluid(), 200);
		ostwald.tanks().input(2).fill(Fluids.WATER, 100);
		helper.succeedWhen(() -> {
			helper.assertTrue(haber.tanks().output(0).has(PetroFluids.AMMONIA.fluid(), 200),
					"Ammonia: " + haber.tanks().output(0).millibuckets());
			helper.assertTrue(haber.tanks().input(0).millibuckets() == 0 && haber.tanks().input(1).millibuckets() == 0,
					"The converter kept some hydrogen or nitrogen");
			helper.assertTrue(ostwald.tanks().output(0).has(PetroFluids.NITRIC_ACID.source(), 200),
					"Nitric acid: " + ostwald.tanks().output(0).millibuckets());
		});
	}

	/** Two phosphate in 250 mB of ammonia make six fertilizer in the chemical reactor (ammonium phosphate). */
	@GameTest(maxTicks = 300)
	public void reactorMakesAmmoniumPhosphate(GameTestHelper helper) {
		MachineBlockEntity reactor = place(helper, MachineKind.CHEMICAL_REACTOR, new BlockPos(4, 1, 2));
		reactor.tanks().input(0).fill(PetroFluids.AMMONIA.fluid(), 250);
		reactor.setItem(0, new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("phosphate")), 2));
		helper.succeedWhen(() -> {
			ItemStack out = reactor.getItem(reactor.kind().outputSlot());
			helper.assertTrue(out.is(PetroItems.FERTILIZER) && out.getCount() == 6, "Fertilizer: " + out);
		});
	}

	/** Nitric acid etches microchips too, using 50 mB where sulfuric acid takes 100. */
	@GameTest(maxTicks = 400)
	public void lithographyEtchesWithNitricAcid(GameTestHelper helper) {
		MachineBlockEntity station = place(helper, MachineKind.LITHOGRAPHY_STATION, new BlockPos(5, 1, 2));
		station.tanks().input(0).fill(PetroFluids.NITRIC_ACID.source(), 1_000);
		station.setItem(0, new ItemStack(PetroItems.SILICON_WAFER));
		station.setItem(1, new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("copper_wire")), 2));
		helper.succeedWhen(() -> {
			ItemStack out = station.getItem(station.kind().outputSlot());
			helper.assertTrue(out.is(PetroItems.MICROCHIP) && out.getCount() == 4, "Microchips: " + out);
			helper.assertTrue(station.tanks().input(0).millibuckets() == 950,
					"Nitric acid left: " + station.tanks().input(0).millibuckets());
		});
	}

	/** The 3x3x3 gas holder holds 1,024 buckets of one gas, through any of its blocks, and refuses liquids. */
	@GameTest
	public void gasHolderHoldsOnlyGas(GameTestHelper helper) {
		BlockPos master = new BlockPos(5, 1, 1);
		placeUnpowered(helper, MachineKind.GAS_HOLDER, master);
		// The top back corner block, two blocks from the master each way.
		Storage<FluidVariant> holder = FluidStorage.SIDED.find(helper.getLevel(),
				helper.absolutePos(master.west(2).above(2).south(2)), Direction.UP);
		helper.assertTrue(holder != null, "No fluid storage on the gas holder's far corner");
		try (Transaction transaction = Transaction.openOuter()) {
			long water = holder.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction);
			helper.assertTrue(water == 0, "The gas holder took water");
			long hydrogen = holder.insert(FluidVariant.of(PetroFluids.HYDROGEN.fluid()), 2_000 * FluidConstants.BUCKET, transaction);
			helper.assertTrue(hydrogen == 1_024 * FluidConstants.BUCKET, "Took " + hydrogen / FluidConstants.BUCKET + " buckets");
			long chlorine = holder.insert(FluidVariant.of(PetroFluids.CHLORINE.fluid()), FluidConstants.BUCKET, transaction);
			helper.assertTrue(chlorine == 0, "A hydrogen holder took chlorine");
			transaction.commit();
		}
		helper.succeed();
	}

	/** A sprinkler with water and fertilizer uses its water a pulse at a time and spreads fertilizer on the crop beside it. */
	@GameTest(maxTicks = 800)
	public void sprinklerWatersAndFertilizes(GameTestHelper helper) {
		BlockPos crop = new BlockPos(2, 1, 2);
		helper.setBlock(crop.below(), Blocks.FARMLAND);
		helper.setBlock(crop, Blocks.WHEAT);
		BlockPos pos = crop.east();
		helper.setBlock(pos, JugcraftFarming.SPRINKLER);
		SprinklerBlockEntity sprinkler = helper.getBlockEntity(pos, SprinklerBlockEntity.class);
		sprinkler.water().variant = FluidVariant.of(Fluids.WATER);
		sprinkler.water().amount = 1_000 * FluidConstants.BUCKET / 1_000;
		helper.assertTrue(sprinkler.addFertilizer(3) == 3, "The sprinkler did not take fertilizer");
		helper.succeedWhen(() -> {
			helper.assertTrue(sprinkler.fertilizer() == 2, "Fertilizer left: " + sprinkler.fertilizer());
			helper.assertTrue(((CropBlock) Blocks.WHEAT).getAge(helper.getBlockState(crop)) > 0, "The wheat did not grow");
			long used = 1_000 - sprinkler.water().amount * 1_000 / FluidConstants.BUCKET;
			helper.assertTrue(used >= SprinklerBlockEntity.FERTILIZE_PULSES * SprinklerBlockEntity.WATER_PER_PULSE,
					"Water used: " + used + " mB");
		});
	}

	/** Fertilizer grows every crop in the 5x5 area around where it is used. */
	@GameTest
	public void fertilizerGrowsTheCropsAround(GameTestHelper helper) {
		List<BlockPos> crops = new java.util.ArrayList<>();
		for (int x = 0; x < 3; x++) {
			for (int z = 0; z < 3; z++) {
				BlockPos soil = new BlockPos(1 + x * 2, 1, 1 + z * 2);
				helper.setBlock(soil, Blocks.FARMLAND);
				helper.setBlock(soil.above(), Blocks.WHEAT);
				crops.add(soil.above());
			}
		}
		// The centre crop is at (3, 2, 3); the corners are two blocks out, inside the 5x5 area.
		int grown = FertilizerItem.fertilize(helper.getLevel(), helper.absolutePos(new BlockPos(3, 2, 3)));
		helper.assertTrue(grown == crops.size(), "Grew " + grown + " of " + crops.size());
		for (BlockPos crop : crops) {
			helper.assertTrue(((CropBlock) Blocks.WHEAT).getAge(helper.getBlockState(crop)) > 0, "Did not grow at " + crop);
		}
		helper.succeed();
	}

	/** The fuel cell turns each millibucket of hydrogen into 128 JE and refuses other fluids. */
	@GameTest(maxTicks = 200)
	public void fuelCellBurnsHydrogen(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 1, 2);
		helper.setBlock(pos, JugcraftMachines.MACHINES.get(MachineKind.FUEL_CELL).defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
		MachineBlockEntity cell = helper.getBlockEntity(pos, MachineBlockEntity.class);
		SimpleEnergyStorage energy = (SimpleEnergyStorage) EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(pos), Direction.UP);
		Storage<FluidVariant> tanks = FluidStorage.SIDED.find(helper.getLevel(), helper.absolutePos(pos), Direction.NORTH);
		try (Transaction transaction = Transaction.openOuter()) {
			long diesel = tanks.insert(FluidVariant.of(PetroFluids.DIESEL.source()), FluidConstants.BUCKET, transaction);
			long hydrogen = tanks.insert(FluidVariant.of(PetroFluids.HYDROGEN.fluid()), FluidConstants.BUCKET, transaction);
			helper.assertTrue(diesel == 0, "The fuel cell took diesel");
			helper.assertTrue(hydrogen == FluidConstants.BUCKET, "The fuel cell took " + hydrogen + " droplets of hydrogen");
			transaction.commit();
		}
		helper.runAfterDelay(40, () -> {
			int burnt = 1000 - cell.tanks().input(0).millibuckets();
			helper.assertTrue(burnt > 0, "Burnt no hydrogen");
			helper.assertTrue(energy.getAmount() == (long) burnt * FluidFuels.HYDROGEN, "Energy " + energy.getAmount() + " for " + burnt + " mB");
			helper.succeed();
		});
	}

	/** The Kroll process: raw titanium, coke and 250 mB of chlorine in the chemical reactor make a titanium sponge. */
	@GameTest(maxTicks = 300)
	public void reactorMakesTitaniumSponge(GameTestHelper helper) {
		MachineBlockEntity reactor = place(helper, MachineKind.CHEMICAL_REACTOR, new BlockPos(4, 1, 2));
		reactor.tanks().input(0).fill(PetroFluids.CHLORINE.fluid(), 250);
		reactor.setItem(0, new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("raw_titanium"))));
		reactor.setItem(1, new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("coke"))));
		helper.succeedWhen(() -> {
			ItemStack out = reactor.getItem(reactor.kind().outputSlot());
			helper.assertTrue(out.is(PetroItems.TITANIUM_SPONGE), "Sponge: " + out);
			helper.assertTrue(reactor.tanks().input(0).isResourceBlank(), "Chlorine left: " + reactor.tanks().input(0).millibuckets());
		});
	}

	/** Leaching: a lepidolite in 250 mB of sulfuric acid gives two lithium carbonate, twice the blast furnace. */
	@GameTest(maxTicks = 300)
	public void reactorLeachesLithium(GameTestHelper helper) {
		MachineBlockEntity reactor = place(helper, MachineKind.CHEMICAL_REACTOR, new BlockPos(4, 1, 2));
		reactor.tanks().input(0).fill(PetroFluids.SULFURIC_ACID.source(), 250);
		reactor.setItem(0, new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("lepidolite"))));
		helper.succeedWhen(() -> {
			ItemStack out = reactor.getItem(reactor.kind().outputSlot());
			helper.assertTrue(out.is(BuiltInRegistries.ITEM.getValue(Jugcraft.id("lithium_carbonate"))) && out.getCount() == 2,
					"Lithium carbonate: " + out);
		});
	}
}
