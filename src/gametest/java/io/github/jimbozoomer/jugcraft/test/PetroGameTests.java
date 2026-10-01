package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.OilReservoirs;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.chemistry.PetroItems;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.fluid.ElectricPumpBlockEntity;
import io.github.jimbozoomer.jugcraft.fluid.FluidTankBlockEntity;
import io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids;
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
import net.minecraft.world.level.block.Blocks;
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
}
