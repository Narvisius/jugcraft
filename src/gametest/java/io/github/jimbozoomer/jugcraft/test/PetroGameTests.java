package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.FoamSprayerItem;
import io.github.jimbozoomer.jugcraft.chemistry.ConstructionChemistry;
import net.minecraft.world.entity.EquipmentSlot;
import io.github.jimbozoomer.jugcraft.weapons.Warhead;
import io.github.jimbozoomer.jugcraft.weapons.Flash;
import io.github.jimbozoomer.jugcraft.weapons.FieldChemistry;
import io.github.jimbozoomer.jugcraft.weapons.ChemicalCloud;
import io.github.jimbozoomer.jugcraft.chemistry.FertilizerItem;
import io.github.jimbozoomer.jugcraft.chemistry.FluidFuels;
import io.github.jimbozoomer.jugcraft.chemistry.OilReservoirs;
import io.github.jimbozoomer.jugcraft.chemistry.PetroBlocks;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.chemistry.PetroItems;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.farming.JugcraftFarming;
import io.github.jimbozoomer.jugcraft.gear.GrappleHook;
import io.github.jimbozoomer.jugcraft.gear.JugcraftGear;
import io.github.jimbozoomer.jugcraft.gear.JugcraftGrapple;
import io.github.jimbozoomer.jugcraft.gear.PneumaticGrappleItem;
import io.github.jimbozoomer.jugcraft.gear.ScubaTankItem;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import io.github.jimbozoomer.jugcraft.farming.SprinklerBlockEntity;
import io.github.jimbozoomer.jugcraft.fluid.ElectricPumpBlockEntity;
import io.github.jimbozoomer.jugcraft.fluid.FluidTankBlockEntity;
import io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids;
import io.github.jimbozoomer.jugcraft.fluid.GasCylinderItem;
import io.github.jimbozoomer.jugcraft.fluid.StoredFluid;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import io.github.jimbozoomer.jugcraft.fluid.TankGaugeBlock;
import io.github.jimbozoomer.jugcraft.kinetic.DynamoBlockEntity;
import io.github.jimbozoomer.jugcraft.kinetic.FlywheelBlock;
import io.github.jimbozoomer.jugcraft.kinetic.FlywheelBlockEntity;
import io.github.jimbozoomer.jugcraft.kinetic.JugcraftKinetics;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.machine.LargeMachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.prospecting.OreSurvey;
import java.util.List;
import io.github.jimbozoomer.jugcraft.solar.JugcraftSolar;
import io.github.jimbozoomer.jugcraft.solar.SolarReceiverBlockEntity;
import io.github.jimbozoomer.jugcraft.weapons.Blast;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
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
import net.minecraft.world.level.GameType;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.material.Fluid;
import io.github.jimbozoomer.jugcraft.fluid.FluidNetworks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

	/**
	 * The settling plant's input tank takes what its jobs use (water for oil sand, flowback water) but not lava, and
	 * its output tank takes nothing in.
	 */
	@GameTest
	public void settlingPlantTanksOnlyTakeWhatTheyUse(GameTestHelper helper) {
		MachineBlockEntity plant = place(helper, MachineKind.FLOWBACK_TREATMENT_UNIT, new BlockPos(4, 1, 2));
		Storage<FluidVariant> tanks = FluidStorage.SIDED.find(helper.getLevel(), helper.absolutePos(new BlockPos(4, 1, 2)), Direction.NORTH);
		helper.assertTrue(tanks != null, "The settling plant has no fluid storage");
		try (Transaction transaction = Transaction.openOuter()) {
			long water = tanks.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction);
			long lava = tanks.insert(FluidVariant.of(Fluids.LAVA), FluidConstants.BUCKET, transaction);
			long oil = tanks.insert(FluidVariant.of(PetroFluids.CRUDE_OIL.source()), FluidConstants.BUCKET, transaction);
			helper.assertTrue(water == FluidConstants.BUCKET, "The settling plant took " + water + " droplets of water");
			helper.assertTrue(lava == 0 && oil == 0, "The settling plant took lava (" + lava + ") or crude oil (" + oil + ")");
			transaction.abort();
		}
		helper.assertTrue(plant.tanks().input(0).isResourceBlank(), "An aborted insert left fluid behind");
		helper.succeed();
	}

	/** With water, the settling plant turns a block of oil sand into 500 mB of crude oil and a block of sand. */
	@GameTest(maxTicks = 300)
	public void settlingPlantWashesOilFromOilSand(GameTestHelper helper) {
		MachineBlockEntity plant = place(helper, MachineKind.FLOWBACK_TREATMENT_UNIT, new BlockPos(4, 1, 2));
		plant.tanks().input(0).fill(Fluids.WATER, 1000);
		plant.setItem(0, new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("oil_sand"))));
		helper.succeedWhen(() -> {
			int oil = plant.tanks().output(0).millibuckets();
			helper.assertTrue(oil == 500 && plant.tanks().output(0).variant.isOf(PetroFluids.CRUDE_OIL.source()),
					"The settling plant holds " + oil + " mB of oil");
			helper.assertTrue(plant.tanks().input(0).millibuckets() == 750, "Water left: " + plant.tanks().input(0).millibuckets());
			helper.assertTrue(plant.getItem(1).is(Items.SAND), "Output slot holds " + plant.getItem(1));
		});
	}

	/** The settling plant's filter press squeezes a block of mud into four clay balls and 250 mB of water. */
	@GameTest(maxTicks = 200)
	public void settlingPlantPressesMudIntoClay(GameTestHelper helper) {
		MachineBlockEntity plant = place(helper, MachineKind.FLOWBACK_TREATMENT_UNIT, new BlockPos(4, 1, 2));
		plant.setItem(0, new ItemStack(Items.MUD));
		helper.succeedWhen(() -> {
			helper.assertTrue(plant.getItem(1).is(Items.CLAY_BALL) && plant.getItem(1).getCount() == 4,
					"Output slot holds " + plant.getItem(1));
			helper.assertTrue(plant.tanks().output(0).has(Fluids.WATER, 250), "Water: " + plant.tanks().output(0).millibuckets());
			helper.assertTrue(plant.getItem(0).isEmpty(), "The mud was not used");
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

	/**
	 * Heavy fuel oil piped into the distillation tower boils under vacuum: 400 mB of lubricant in its fifth tank and two
	 * asphalt binder in its slot.
	 */
	@GameTest(maxTicks = 300)
	public void towerVacuumDistilsHeavyFuelOil(GameTestHelper helper) {
		MachineBlockEntity tower = place(helper, MachineKind.DISTILLATION_TOWER, new BlockPos(4, 1, 2));
		tower.tanks().input(0).fill(PetroFluids.HEAVY_FUEL_OIL.source(), 1000);
		helper.succeedWhen(() -> {
			helper.assertTrue(tower.tanks().output(4).has(PetroFluids.LUBRICANT.source(), 400), "No lubricant");
			helper.assertTrue(tower.getItem(0).is(PetroItems.ASPHALT_BINDER) && tower.getItem(0).getCount() == 2,
					"The tower holds " + tower.getItem(0));
			helper.assertTrue(tower.tanks().input(0).isResourceBlank(), "Heavy fuel oil left over");
		});
	}

	/**
	 * Naphtha piped into the catalytic cracker is reformed over one catalyst: 900 mB of gasoline in its fourth tank and
	 * 100 mB of refinery gas in its gas tank.
	 */
	@GameTest(maxTicks = 300)
	public void crackerReformsNaphtha(GameTestHelper helper) {
		MachineBlockEntity cracker = place(helper, MachineKind.CATALYTIC_CRACKER, new BlockPos(4, 1, 2));
		cracker.tanks().input(0).fill(PetroFluids.NAPHTHA.source(), 1000);
		cracker.setItem(0, new ItemStack(PetroItems.CRACKING_CATALYST, 2));
		helper.succeedWhen(() -> {
			helper.assertTrue(cracker.tanks().output(3).has(PetroFluids.GASOLINE.source(), 900), "No gasoline");
			helper.assertTrue(cracker.tanks().output(2).has(PetroFluids.REFINERY_GAS.fluid(), 100), "No refinery gas");
			helper.assertTrue(cracker.getItem(0).getCount() == 1, "The cracker used " + (2 - cracker.getItem(0).getCount()) + " catalysts");
		});
	}

	/** The chemical reactor mixes two sand and a dried kelp into a bucket of water: a bucket of fracking fluid. */
	@GameTest(maxTicks = 200)
	public void reactorMixesFrackingFluid(GameTestHelper helper) {
		MachineBlockEntity reactor = place(helper, MachineKind.CHEMICAL_REACTOR, new BlockPos(4, 1, 2));
		reactor.tanks().input(0).fill(Fluids.WATER, 1000);
		reactor.setItem(0, new ItemStack(Items.SAND, 2));
		reactor.setItem(1, new ItemStack(Items.DRIED_KELP));
		helper.succeedWhen(() -> {
			helper.assertTrue(reactor.tanks().output(0).has(PetroFluids.FRACKING_FLUID.source(), 1000), "No fracking fluid");
			helper.assertTrue(reactor.getItem(0).isEmpty() && reactor.getItem(1).isEmpty(), "The reactor kept its sand or kelp");
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

	/** The settling plant turns a bucket of flowback water into 750 mB of clean water and a salt. */
	@GameTest(maxTicks = 200)
	public void treatmentCleansFlowback(GameTestHelper helper) {
		MachineBlockEntity unit = place(helper, MachineKind.FLOWBACK_TREATMENT_UNIT, new BlockPos(4, 1, 2));
		unit.tanks().input(0).fill(PetroFluids.FLOWBACK_WATER.source(), 1000);
		helper.succeedWhen(() -> {
			helper.assertTrue(unit.tanks().output(0).has(Fluids.WATER, 750), "Water: " + unit.tanks().output(0).millibuckets());
			helper.assertTrue(unit.getItem(1).is(BuiltInRegistries.ITEM.getValue(Jugcraft.id("salt"))), "No salt: " + unit.getItem(1));
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

	/** Batch 29: the hydrotreater turns diesel and hydrogen into premium diesel (base tank) and hydrogen sulfide (top). */
	@GameTest(maxTicks = 300)
	public void hydrotreaterMakesPremiumDiesel(GameTestHelper helper) {
		MachineBlockEntity hydrotreater = place(helper, MachineKind.HYDROTREATER, new BlockPos(4, 1, 2));
		hydrotreater.tanks().input(0).fill(PetroFluids.DIESEL.source(), 1000);
		hydrotreater.tanks().input(1).fill(PetroFluids.HYDROGEN.fluid(), 100);
		helper.succeedWhen(() -> {
			helper.assertTrue(hydrotreater.tanks().output(0).has(PetroFluids.PREMIUM_DIESEL.source(), 1000), "No premium diesel");
			helper.assertTrue(hydrotreater.tanks().output(1).has(PetroFluids.HYDROGEN_SULFIDE.fluid(), 100), "No hydrogen sulfide");
			helper.assertTrue(hydrotreater.tanks().input(0).millibuckets() == 0 && hydrotreater.tanks().input(1).millibuckets() == 0,
					"Left over: " + hydrotreater.tanks().input(0).millibuckets() + " diesel, " + hydrotreater.tanks().input(1).millibuckets() + " hydrogen");
		});
	}

	/** Batch 29: 900 mB of gasoline blended with 100 mB of bioethanol make a bucket of premium gasoline. */
	@GameTest(maxTicks = 200)
	public void hydrotreaterBlendsPremiumGasoline(GameTestHelper helper) {
		MachineBlockEntity hydrotreater = place(helper, MachineKind.HYDROTREATER, new BlockPos(4, 1, 2));
		hydrotreater.tanks().input(0).fill(PetroFluids.GASOLINE.source(), 900);
		hydrotreater.tanks().input(1).fill(PetroFluids.BIOETHANOL.source(), 100);
		helper.succeedWhen(() -> helper.assertTrue(hydrotreater.tanks().output(0).has(PetroFluids.PREMIUM_GASOLINE.source(), 1000),
				"No premium gasoline"));
	}

	/** Batch 29: the chemical reactor recovers a sulfur dust from 200 mB of hydrogen sulfide (the Claus process). */
	@GameTest(maxTicks = 200)
	public void reactorRecoversSulfur(GameTestHelper helper) {
		MachineBlockEntity reactor = place(helper, MachineKind.CHEMICAL_REACTOR, new BlockPos(4, 1, 2));
		reactor.tanks().input(0).fill(PetroFluids.HYDROGEN_SULFIDE.fluid(), 200);
		helper.succeedWhen(() -> {
			ItemStack out = reactor.getItem(MachineKind.CHEMICAL_REACTOR.outputSlot());
			helper.assertTrue(out.is(BuiltInRegistries.ITEM.getValue(Jugcraft.id("sulfur_dust"))) && out.getCount() == 1, "Got " + out);
			helper.assertTrue(reactor.tanks().input(0).millibuckets() == 0, "Gas left over");
		});
	}

	/** Batch 29: premium diesel is worth 320 JE a mB in the diesel generator, a quarter more than diesel. */
	@GameTest(maxTicks = 200)
	public void premiumDieselBurnsBetter(GameTestHelper helper) {
		BlockPos master = new BlockPos(4, 1, 2);
		MachineBlockEntity generator = place(helper, MachineKind.DIESEL_GENERATOR, master);
		SimpleEnergyStorage energy = (SimpleEnergyStorage) EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(master), Direction.UP);
		energy.setAmount(0);
		generator.tanks().input(0).fill(PetroFluids.PREMIUM_DIESEL.source(), 1000);
		helper.runAfterDelay(40, () -> {
			int burnt = 1000 - generator.tanks().input(0).millibuckets();
			long expected = (long) burnt * FluidFuels.PREMIUM_DIESEL;
			helper.assertTrue(burnt > 0, "Burnt no premium diesel");
			helper.assertTrue(energy.getAmount() <= expected && energy.getAmount() > expected - MachineKind.DIESEL_OUTPUT,
					"Energy " + energy.getAmount() + " for " + burnt + " mB");
			helper.succeed();
		});
	}

	/** A gas turbine (master at {@code master}) running on gasoline with lubricant, its buffer emptied. */
	private static MachineBlockEntity runningTurbine(GameTestHelper helper, BlockPos master) {
		MachineBlockEntity turbine = place(helper, MachineKind.GAS_TURBINE, master);
		((SimpleEnergyStorage) EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(master), Direction.UP)).setAmount(0);
		turbine.tanks().input(0).fill(PetroFluids.GASOLINE.source(), 4000);
		turbine.tanks().input(1).fill(PetroFluids.LUBRICANT.source(), 100);
		return turbine;
	}

	/** A heat recovery unit at {@code pos}, its buffer emptied, with water and lubricant (or without water). */
	private static SimpleEnergyStorage recoveryUnit(GameTestHelper helper, BlockPos pos, boolean water) {
		MachineBlockEntity unit = place(helper, MachineKind.HEAT_RECOVERY_UNIT, pos);
		SimpleEnergyStorage energy = (SimpleEnergyStorage) EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(pos), Direction.UP);
		energy.setAmount(0);
		if (water) {
			unit.tanks().input(0).fill(Fluids.WATER, 4000);
		}
		unit.tanks().input(1).fill(PetroFluids.LUBRICANT.source(), 100);
		return energy;
	}

	/**
	 * Batch 29: a heat recovery unit in front of a running gas turbine makes 30% of the turbine's output again from its
	 * exhaust, boiling water as it goes; one without water makes nothing.
	 */
	@GameTest(maxTicks = 200)
	public void heatRecoveryUnitUsesTurbineExhaust(GameTestHelper helper) {
		BlockPos master = new BlockPos(5, 1, 3);
		runningTurbine(helper, master);
		SimpleEnergyStorage turbine = (SimpleEnergyStorage) EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(master), Direction.UP);
		SimpleEnergyStorage dry = recoveryUnit(helper, new BlockPos(5, 1, 5), false);
		SimpleEnergyStorage unit = recoveryUnit(helper, new BlockPos(5, 1, 2), true);
		MachineBlockEntity unitEntity = helper.getBlockEntity(new BlockPos(5, 1, 2), MachineBlockEntity.class);
		helper.runAfterDelay(60, () -> {
			long made = turbine.getAmount();
			long share = made * MachineKind.RECOVERY_PERCENT / 100;
			long slack = 2L * MachineKind.TURBINE_OUTPUT * MachineKind.RECOVERY_PERCENT / 100 + 1;
			helper.assertTrue(made > 0, "The turbine made nothing");
			helper.assertTrue(unit.getAmount() <= share && unit.getAmount() >= share - slack,
					"Recovered " + unit.getAmount() + " JE of the turbine's " + made);
			int boiled = 4000 - unitEntity.tanks().input(0).millibuckets();
			helper.assertTrue(Math.abs(boiled - unit.getAmount() / MachineKind.RECOVERY_JE_PER_WATER) <= 1,
					"Boiled " + boiled + " mB of water for " + unit.getAmount() + " JE");
			helper.assertTrue(dry.getAmount() == 0, "A unit without water made " + dry.getAmount() + " JE");
			helper.succeed();
		});
	}

	/** Batch 29: two heat recovery units on one turbine share its exhaust heat; together they never make more than one. */
	@GameTest(maxTicks = 200)
	public void heatRecoveryUnitsShareOneTurbine(GameTestHelper helper) {
		BlockPos master = new BlockPos(5, 1, 3);
		runningTurbine(helper, master);
		SimpleEnergyStorage turbine = (SimpleEnergyStorage) EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(master), Direction.UP);
		SimpleEnergyStorage front = recoveryUnit(helper, new BlockPos(5, 1, 2), true);
		SimpleEnergyStorage back = recoveryUnit(helper, new BlockPos(5, 1, 5), true);
		helper.runAfterDelay(60, () -> {
			long share = turbine.getAmount() * MachineKind.RECOVERY_PERCENT / 100;
			long both = front.getAmount() + back.getAmount();
			helper.assertTrue(both > 0 && both <= share, "Two units recovered " + both + " JE of a one-turbine share of " + share);
			helper.succeed();
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

	/**
	 * PVC (batch 15): the synthesis converter joins 250 mB of refinery gas and 250 mB of chlorine into 250 mB of vinyl
	 * chloride, and the polymerization reactor turns 500 mB of it into four PVC resin.
	 */
	@GameTest(maxTicks = 300)
	public void chlorineBecomesPvc(GameTestHelper helper) {
		MachineBlockEntity converter = place(helper, MachineKind.SYNTHESIS_CONVERTER, new BlockPos(5, 1, 1));
		converter.tanks().input(0).fill(PetroFluids.REFINERY_GAS.fluid(), 250);
		converter.tanks().input(1).fill(PetroFluids.CHLORINE.fluid(), 250);
		MachineBlockEntity polymerizer = place(helper, MachineKind.POLYMERIZATION_REACTOR, new BlockPos(5, 1, 4));
		polymerizer.tanks().input(0).fill(PetroFluids.VINYL_CHLORIDE.fluid(), 500);
		helper.succeedWhen(() -> {
			helper.assertTrue(converter.tanks().output(0).has(PetroFluids.VINYL_CHLORIDE.fluid(), 250),
					"Vinyl chloride: " + converter.tanks().output(0).millibuckets());
			ItemStack resin = polymerizer.getItem(0);
			helper.assertTrue(resin.is(PetroItems.PVC_RESIN) && resin.getCount() == 4, "PVC: " + resin);
		});
	}

	/** Soap (batch 15): two rotten flesh boiled in 250 mB of lye make four soap; a bar washes a player's effects off. */
	@GameTest(maxTicks = 300)
	public void lyeMakesSoapThatWashesEffectsOff(GameTestHelper helper) {
		MachineBlockEntity reactor = place(helper, MachineKind.CHEMICAL_REACTOR, new BlockPos(4, 1, 2));
		reactor.tanks().input(0).fill(PetroFluids.LYE.source(), 250);
		reactor.setItem(0, new ItemStack(Items.ROTTEN_FLESH, 2));
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL); // A creative player's items are never used up.
		player.addEffect(new MobEffectInstance(MobEffects.POISON, 600));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PetroItems.SOAP, 2));
		PetroItems.SOAP.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(player.getActiveEffects().isEmpty(), "The soap left " + player.getActiveEffects());
		helper.assertTrue(player.getMainHandItem().getCount() == 1, "The soap was not used up: " + player.getMainHandItem());
		helper.succeedWhen(() -> {
			ItemStack soap = reactor.getItem(reactor.kind().outputSlot());
			helper.assertTrue(soap.is(PetroItems.SOAP) && soap.getCount() == 4, "Soap: " + soap);
		});
	}

	/**
	 * Flow battery (batch 17): it holds 1,000 JE for each mB of vanadium electrolyte in it, takes nothing else into its
	 * tank and lets none out; the chemical reactor makes the electrolyte from asphalt binder and sulfuric acid.
	 */
	@GameTest(maxTicks = 300)
	public void flowBatteryHoldsWhatItsElectrolyteAllows(GameTestHelper helper) {
		BlockPos master = new BlockPos(4, 1, 1);
		MachineBlockEntity battery = placeUnpowered(helper, MachineKind.FLOW_BATTERY, master);
		EnergyStorage energy = battery.energyFor(null);
		helper.assertTrue(energy.getCapacity() == 0, "An empty flow battery holds " + energy.getCapacity());
		Storage<FluidVariant> tank = FluidStorage.SIDED.find(helper.getLevel(), helper.absolutePos(master), Direction.UP);
		try (Transaction transaction = Transaction.openOuter()) {
			long water = tank.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction);
			long electrolyte = tank.insert(FluidVariant.of(PetroFluids.VANADIUM_ELECTROLYTE.source()), FluidConstants.BUCKET,
					transaction);
			helper.assertTrue(water == 0, "The flow battery took " + water / 81 + " mB of water");
			helper.assertTrue(electrolyte == FluidConstants.BUCKET, "The flow battery took " + electrolyte / 81 + " mB");
			transaction.commit();
		}
		helper.assertTrue(energy.getCapacity() == 1_000 * MachineKind.FLOW_BATTERY_JE_PER_MB,
				"A bucket of electrolyte gave " + energy.getCapacity() + " JE of room");
		try (Transaction transaction = Transaction.openOuter()) {
			long taken = tank.extract(FluidVariant.of(PetroFluids.VANADIUM_ELECTROLYTE.source()), FluidConstants.BUCKET,
					transaction);
			helper.assertTrue(taken == 0, "The electrolyte could be pumped out: " + taken / 81 + " mB");
		}
		((SimpleEnergyStorage) energy).setAmount(Long.MAX_VALUE);
		helper.assertTrue(energy.getAmount() == 1_000_000, "The battery was charged to " + energy.getAmount());
		helper.assertTrue(battery.reservoir().amount == FluidConstants.BUCKET, "The electrolyte is gone");

		MachineBlockEntity reactor = place(helper, MachineKind.CHEMICAL_REACTOR, new BlockPos(4, 1, 4));
		reactor.tanks().input(0).fill(PetroFluids.SULFURIC_ACID.source(), 1_000);
		reactor.setItem(0, new ItemStack(PetroItems.ASPHALT_BINDER, 2));
		helper.succeedWhen(() -> helper.assertTrue(
				reactor.tanks().output(0).has(PetroFluids.VANADIUM_ELECTROLYTE.source(), 1_000),
				"Electrolyte: " + reactor.tanks().output(0).millibuckets()));
	}

	/**
	 * Explosive weapons (batch 18): a grenade's blast hurts a zombie in the open, but not one behind a stone wall, and
	 * breaks no block, armor stand or dropped item; cotton and nitric acid make guncotton.
	 */
	@GameTest(maxTicks = 300)
	public void grenadeBlastHurtsButBreaksNothing(GameTestHelper helper) {
		helper.setBlock(new BlockPos(5, 1, 3), Blocks.GLASS);
		helper.setBlock(new BlockPos(3, 1, 3), Blocks.GRASS_BLOCK);
		for (int y = 1; y <= 3; y++) {
			for (int z = 2; z <= 6; z++) {
				helper.setBlock(new BlockPos(6, y, z), Blocks.STONE);
			}
		}
		Mob exposed = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(4, 1, 6));
		Mob sheltered = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(7, 1, 4));
		Entity stand = helper.spawn(EntityTypes.ARMOR_STAND, new BlockPos(2, 1, 4));
		ItemEntity diamond = helper.spawnItem(Items.DIAMOND, 4.5F, 1.2F, 3.5F);
		float full = exposed.getHealth();
		int hurt = Blast.detonate(helper.getLevel(), helper.absoluteVec(new Vec3(4.5, 1.5, 4.5)), null, null);
		helper.assertTrue(exposed.getHealth() < full, "The zombie in the open was not hurt: " + exposed.getHealth());
		helper.assertTrue(sheltered.getHealth() == sheltered.getMaxHealth(), "The wall did not shield: " + sheltered.getHealth());
		helper.assertTrue(hurt == 1, "The blast hurt " + hurt + " things");
		helper.assertTrue(stand.isAlive() && diamond.isAlive(), "The blast broke the armor stand or the dropped item");
		helper.assertBlockPresent(Blocks.GLASS, new BlockPos(5, 1, 3));
		helper.assertBlockPresent(Blocks.GRASS_BLOCK, new BlockPos(3, 1, 3));
		helper.assertBlockPresent(Blocks.STONE, new BlockPos(6, 1, 4));

		MachineBlockEntity reactor = place(helper, MachineKind.CHEMICAL_REACTOR, new BlockPos(2, 1, 1));
		reactor.tanks().input(0).fill(PetroFluids.NITRIC_ACID.source(), 250);
		reactor.setItem(0, new ItemStack(JugcraftFarming.COTTON, 2));
		helper.succeedWhen(() -> {
			ItemStack guncotton = reactor.getItem(reactor.kind().outputSlot());
			helper.assertTrue(guncotton.is(PetroItems.GUNCOTTON) && guncotton.getCount() == 2, "Guncotton: " + guncotton);
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

	/**
	 * Turbocharger and flywheel (batch 19): a turbocharged advanced engine with coolant spins a flywheel faster than an
	 * engine without one could, and uses its coolant; a charged flywheel drives a dynamo from its front, and one with
	 * nothing to drive runs down by friction.
	 */
	@GameTest(maxTicks = 100)
	public void turbochargerAndFlywheel(GameTestHelper helper) {
		BlockPos master = new BlockPos(4, 1, 1);
		BlockPos wheelPos = master.south();
		helper.setBlock(wheelPos, JugcraftKinetics.FLYWHEEL.defaultBlockState().setValue(FlywheelBlock.FACING, Direction.SOUTH));
		FlywheelBlockEntity wheel = helper.getBlockEntity(wheelPos, FlywheelBlockEntity.class);
		MachineBlockEntity engine = placeUnpowered(helper, MachineKind.ADVANCED_ENGINE, master);
		helper.assertTrue(engine.canPlaceItem(0, new ItemStack(PetroItems.TURBOCHARGER))
				&& !engine.canPlaceItem(0, new ItemStack(Items.IRON_INGOT)), "The engine's slot takes the wrong items");
		engine.setItem(0, new ItemStack(PetroItems.TURBOCHARGER));
		engine.tanks().input(0).fill(PetroFluids.GASOLINE.source(), 1000);
		engine.tanks().input(1).fill(Fluids.WATER, 1000);

		BlockPos driverPos = new BlockPos(1, 1, 4);
		BlockPos dynamoPos = driverPos.south();
		helper.setBlock(driverPos, JugcraftKinetics.FLYWHEEL.defaultBlockState().setValue(FlywheelBlock.FACING, Direction.SOUTH));
		helper.setBlock(dynamoPos, JugcraftKinetics.MAGNET_DYNAMO);
		FlywheelBlockEntity driver = helper.getBlockEntity(driverPos, FlywheelBlockEntity.class);
		DynamoBlockEntity dynamo = helper.getBlockEntity(dynamoPos, DynamoBlockEntity.class);
		driver.setStored(100_000);

		BlockPos idlePos = new BlockPos(7, 1, 6);
		helper.setBlock(idlePos, JugcraftKinetics.FLYWHEEL.defaultBlockState().setValue(FlywheelBlock.FACING, Direction.UP));
		FlywheelBlockEntity idle = helper.getBlockEntity(idlePos, FlywheelBlockEntity.class);
		idle.setStored(100_000);

		helper.runAfterDelay(20, () -> {
			helper.assertTrue(wheel.stored() > 20L * MachineKind.ADVANCED_ENGINE_OUTPUT,
					"The turbocharged engine gave the flywheel only " + wheel.stored() + " KE in 20 ticks");
			helper.assertTrue(engine.tanks().input(1).millibuckets() < 1000, "The turbocharger used no coolant");
			helper.assertTrue(dynamo.energy().getAmount() > 0 && driver.stored() < 100_000, "The flywheel drove nothing");
			helper.assertTrue(idle.stored() < 100_000 && idle.stored() > 90_000,
					"Friction left an idle flywheel with " + idle.stored() + " KE");
			helper.succeed();
		});
	}

	/**
	 * Joined tanks and the tank gauge (batch 20): two stacked tinplate tanks and a glass tank beside the lower one act
	 * as one 48-bucket tank of one fluid, filled from the bottom and drained from the top; a gauge on the lower tank
	 * shows the whole group's level in eighths.
	 */
	@GameTest(maxTicks = 100)
	public void joinedTanksAndGauge(GameTestHelper helper) {
		BlockPos low = new BlockPos(2, 1, 2);
		BlockPos high = low.above();
		BlockPos glass = low.east();
		helper.setBlock(low, JugcraftFluids.FLUID_TANK);
		helper.setBlock(high, JugcraftFluids.FLUID_TANK);
		helper.setBlock(glass, JugcraftFluids.GLASS_TANK);
		Storage<FluidVariant> group = FluidStorage.SIDED.find(helper.getLevel(), helper.absolutePos(high), Direction.UP);
		try (Transaction transaction = Transaction.openOuter()) {
			long water = group.insert(FluidVariant.of(Fluids.WATER), 40 * FluidConstants.BUCKET, transaction);
			helper.assertTrue(water == 40 * FluidConstants.BUCKET, "The group took " + water / FluidConstants.BUCKET + " buckets");
			long lava = group.insert(FluidVariant.of(Fluids.LAVA), FluidConstants.BUCKET, transaction);
			helper.assertTrue(lava == 0, "The water tanks took lava");
			transaction.commit();
		}
		long full = FluidTankBlockEntity.CAPACITY;
		helper.assertTrue(helper.getBlockEntity(low, FluidTankBlockEntity.class).storage.amount == full
				&& helper.getBlockEntity(glass, FluidTankBlockEntity.class).storage.amount == full
				&& helper.getBlockEntity(high, FluidTankBlockEntity.class).storage.amount == 8 * FluidConstants.BUCKET,
				"The group did not fill from the bottom");
		try (Transaction transaction = Transaction.openOuter()) {
			long taken = group.extract(FluidVariant.of(Fluids.WATER), 10 * FluidConstants.BUCKET, transaction);
			helper.assertTrue(taken == 10 * FluidConstants.BUCKET, "Took " + taken / FluidConstants.BUCKET + " buckets");
			transaction.commit();
		}
		helper.assertTrue(helper.getBlockEntity(high, FluidTankBlockEntity.class).storage.amount == 0,
				"The group did not drain from the top");
		BlockPos gauge = low.north();
		helper.setBlock(gauge, JugcraftFluids.TANK_GAUGE.defaultBlockState().setValue(TankGaugeBlock.FACING, Direction.NORTH));
		helper.succeedWhen(() -> {
			// 30 of 48 buckets: five eighths.
			int level = helper.getBlockState(gauge).getValue(TankGaugeBlock.LEVEL);
			helper.assertTrue(level == 5, "The gauge shows " + level + " eighths");
		});
	}

	/** How long the heliostat test waits for sky light to settle under the roof it places. */
	private static final int ROOF_SETTLE_TICKS = 100;

	/** Runs {@code then} once the receiver at {@code absolute} counts {@code expected} heliostats; fails after {@code left} ticks. */
	private static void awaitHeliostatCount(GameTestHelper helper, BlockPos absolute, int expected, int left, Runnable then) {
		int count = SolarReceiverBlockEntity.countHeliostats(helper.getLevel(), absolute);
		if (count == expected) {
			then.run();
		} else if (left <= 0) {
			helper.assertTrue(false, "The receiver counted " + count + " heliostats under open sky");
		} else {
			helper.runAfterDelay(1, () -> awaitHeliostatCount(helper, absolute, expected, left - 1, then));
		}
	}

	/**
	 * Solar thermal (batch 21): a receiver counts the heliostats under open sky in the field below it (not one that is
	 * roofed over), makes nothing without water, and with water makes 12 JE/t a heliostat in daylight (half in rain),
	 * boiling water for it. The test world's time and weather are not fixed, so daylight is read from the level.
	 */
	@GameTest(maxTicks = 320)
	public void heliostatsHeatASolarReceiver(GameTestHelper helper) {
		BlockPos receiverPos = new BlockPos(4, 5, 4);
		helper.setBlock(receiverPos, JugcraftSolar.SOLAR_RECEIVER);
		for (BlockPos mirror : List.of(new BlockPos(2, 1, 2), new BlockPos(6, 1, 6), new BlockPos(1, 2, 6), new BlockPos(6, 1, 1))) {
			helper.setBlock(mirror, JugcraftSolar.HELIOSTAT);
		}
		helper.setBlock(new BlockPos(6, 2, 1), Blocks.STONE);
		BlockPos absolute = helper.absolutePos(receiverPos);
		SolarReceiverBlockEntity receiver = helper.getBlockEntity(receiverPos, SolarReceiverBlockEntity.class);
		// Sky light (what "open sky" reads) catches up with the new roof some ticks after it is placed, later on a busy
		// server: wait until the count settles (at most ROOF_SETTLE_TICKS) rather than a fixed delay.
		awaitHeliostatCount(helper, absolute, 3, ROOF_SETTLE_TICKS, () -> {
			helper.assertTrue(receiver.lastOutput() == 0 && receiver.energy().getAmount() == 0, "It made power without water");
			// The receiver counted on its first tick, before the roof's sky light settled, and counts again only when the
			// game time is a multiple of SCAN_INTERVAL: give it water after its next count, so the test does not depend
			// on the game time it starts at.
			int nextScan = (int) (SolarReceiverBlockEntity.SCAN_INTERVAL - helper.getLevel().getGameTime() % SolarReceiverBlockEntity.SCAN_INTERVAL);
			helper.runAfterDelay(nextScan + 1, () -> {
				receiver.water().variant = FluidVariant.of(Fluids.WATER);
				receiver.water().amount = 4 * FluidConstants.BUCKET;
				helper.runAfterDelay(5, () -> {
					ServerLevel level = helper.getLevel();
					boolean sun = level.isBrightOutside() && level.canSeeSky(absolute.above());
					int expected = sun ? (level.isRaining() ? 18 : 36) : 0;
					helper.assertTrue(receiver.lastOutput() == expected,
							"The receiver made " + receiver.lastOutput() + " JE/t, expected " + expected);
					helper.assertTrue((expected > 0) == (receiver.water().amount < 4 * FluidConstants.BUCKET),
							"Water boiled " + (4 * FluidConstants.BUCKET - receiver.water().amount) / 81 + " mB at " + expected + " JE/t");
					helper.succeed();
				});
			});
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

	/** The chemical reactor dissolves two salt in a bucket of water to make a bucket of brine. */
	@GameTest(maxTicks = 200)
	public void reactorMixesBrine(GameTestHelper helper) {
		MachineBlockEntity reactor = place(helper, MachineKind.CHEMICAL_REACTOR, new BlockPos(4, 1, 2));
		reactor.tanks().input(0).fill(Fluids.WATER, 1000);
		reactor.setItem(0, new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("salt")), 2));
		helper.succeedWhen(() -> helper.assertTrue(reactor.tanks().output(0).has(PetroFluids.BRINE.source(), 1000), "No brine"));
	}

	/**
	 * The electrolytic cell splits a bucket of brine into 250 mB of chlorine, 250 mB of hydrogen and 500 mB of lye after
	 * 400 ticks, the same as the Electrolytic Separator.
	 */
	@GameTest(maxTicks = 600)
	public void cellSplitsBrine(GameTestHelper helper) {
		BlockPos master = new BlockPos(4, 1, 2);
		MachineBlockEntity cell = place(helper, MachineKind.ELECTROLYTIC_CELL, master);
		cell.tanks().input(0).fill(PetroFluids.BRINE.source(), 1000);
		// A bucket takes 102,400 JE, more than the cell's 60,000 JE battery holds: keep it charged, as a cable would.
		SimpleEnergyStorage energy = (SimpleEnergyStorage) EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(master), Direction.UP);
		helper.succeedWhen(() -> {
			energy.setAmount(energy.getCapacity());
			helper.assertTrue(cell.tanks().output(0).has(PetroFluids.CHLORINE.fluid(), 250), "Chlorine: " + cell.tanks().output(0).millibuckets());
			helper.assertTrue(cell.tanks().output(1).has(PetroFluids.HYDROGEN.fluid(), 250), "Hydrogen: " + cell.tanks().output(1).millibuckets());
			helper.assertTrue(cell.tanks().output(2).has(PetroFluids.LYE.source(), 500), "Lye: " + cell.tanks().output(2).millibuckets());
		});
	}

	/**
	 * The electrolytic cell also splits plain water, slowly: a bucket gives 500 mB of hydrogen (middle row) and 250 mB
	 * of oxygen (top row) after 800 ticks.
	 */
	@GameTest(maxTicks = 1000)
	public void cellSplitsWater(GameTestHelper helper) {
		BlockPos master = new BlockPos(4, 1, 2);
		MachineBlockEntity cell = place(helper, MachineKind.ELECTROLYTIC_CELL, master);
		cell.tanks().input(0).fill(Fluids.WATER, 1000);
		// A bucket takes 204,800 JE, more than the cell's 60,000 JE battery holds: keep it charged, as a cable would.
		// (succeedWhen runs this check every tick until it passes.)
		SimpleEnergyStorage energy = (SimpleEnergyStorage) EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(master), Direction.UP);
		helper.succeedWhen(() -> {
			energy.setAmount(energy.getCapacity());
			helper.assertTrue(cell.tanks().output(1).has(PetroFluids.HYDROGEN.fluid(), 500), "Hydrogen: " + cell.tanks().output(1).millibuckets());
			helper.assertTrue(cell.tanks().output(0).has(PetroFluids.OXYGEN.fluid(), 250), "Oxygen: " + cell.tanks().output(0).millibuckets());
			helper.assertTrue(cell.tanks().output(2).isResourceBlank(), "Something went into the lye tank");
			helper.assertTrue(cell.tanks().input(0).isResourceBlank(), "Water left over");
		});
	}

	/**
	 * Batch 26, the best ore route: an iron ore dissolved in 250 mB of sulfuric acid gives four washed iron ore (the
	 * ore washer gives three).
	 */
	@GameTest(maxTicks = 300)
	public void reactorLeachesOreFourTimes(GameTestHelper helper) {
		MachineBlockEntity reactor = place(helper, MachineKind.CHEMICAL_REACTOR, new BlockPos(4, 1, 2));
		reactor.tanks().input(0).fill(PetroFluids.SULFURIC_ACID.source(), 250);
		reactor.setItem(0, new ItemStack(Items.IRON_ORE));
		helper.succeedWhen(() -> {
			ItemStack out = reactor.getItem(reactor.kind().outputSlot());
			helper.assertTrue(out.is(BuiltInRegistries.ITEM.getValue(Jugcraft.id("washed_iron_ore"))) && out.getCount() == 4,
					"The reactor made " + out);
			helper.assertTrue(reactor.tanks().input(0).isResourceBlank(), "Acid left over");
		});
	}

	/**
	 * Batch 26: eight crops ferment in a bucket of water into 250 mB of bioethanol, which the gas turbine and the advanced
	 * engine burn.
	 */
	@GameTest(maxTicks = 300)
	public void reactorFermentsBioethanol(GameTestHelper helper) {
		MachineBlockEntity reactor = place(helper, MachineKind.CHEMICAL_REACTOR, new BlockPos(4, 1, 2));
		reactor.tanks().input(0).fill(Fluids.WATER, 1000);
		reactor.setItem(0, new ItemStack(Items.SUGAR_CANE, 8));
		helper.assertTrue(FluidFuels.jePerMb(MachineKind.GAS_TURBINE, PetroFluids.BIOETHANOL.source()) == 192,
				"The gas turbine does not burn bioethanol at 192 JE/mB");
		helper.assertTrue(FluidFuels.jePerMb(MachineKind.ADVANCED_ENGINE, PetroFluids.BIOETHANOL.source()) == 256,
				"The advanced engine does not burn bioethanol at 256 KE/mB");
		helper.assertTrue(FluidFuels.jePerMb(MachineKind.DIESEL_GENERATOR, PetroFluids.BIOETHANOL.source()) == 0,
				"The diesel generator burns bioethanol");
		helper.succeedWhen(() -> {
			helper.assertTrue(reactor.tanks().output(0).has(PetroFluids.BIOETHANOL.source(), 250),
					"Bioethanol: " + reactor.tanks().output(0).millibuckets());
			helper.assertTrue(reactor.getItem(0).isEmpty(), "The reactor kept its sugar cane");
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

	/**
	 * Argon piped into the arc furnace's controller shields the melt and doubles its speed: a silicon boule (400 ticks
	 * without) comes out well within 300 ticks.
	 */
	@GameTest(maxTicks = 300)
	public void argonSpeedsUpTheArcFurnace(GameTestHelper helper) {
		// A solid 3x3x3 of casing with the controller in the middle of its north face, facing out.
		for (int x = 2; x <= 4; x++) {
			for (int y = 1; y <= 3; y++) {
				for (int z = 1; z <= 3; z++) {
					helper.setBlock(new BlockPos(x, y, z), JugcraftMachines.ARC_FURNACE_CASING);
				}
			}
		}
		BlockPos controller = new BlockPos(3, 2, 1);
		helper.setBlock(controller, JugcraftMachines.MACHINES.get(MachineKind.ARC_FURNACE).defaultBlockState()
				.setValue(MachineBlock.FACING, Direction.NORTH));
		MachineBlockEntity furnace = helper.getBlockEntity(controller, MachineBlockEntity.class);
		EnergyStorage storage = EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(controller), Direction.NORTH);
		((SimpleEnergyStorage) storage).setAmount(storage.getCapacity());
		furnace.setItem(0, new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("silicon")), 4));
		furnace.setItem(1, new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("phosphate"))));
		feedGas(helper, controller, PetroFluids.ARGON.fluid(), 1_000);
		helper.succeedWhen(() -> {
			ItemStack output = furnace.getItem(MachineKind.ARC_FURNACE.outputSlot());
			helper.assertTrue(output.is(PetroItems.SILICON_BOULE), "Arc furnace output: " + output);
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

	/**
	 * Batch 27: a scuba tank used on a gas holder of oxygen fills to its 8,000 mB and leaves the rest in the holder.
	 */
	@GameTest
	public void scubaTankFillsFromAGasHolder(GameTestHelper helper) {
		BlockPos master = new BlockPos(5, 1, 1);
		placeUnpowered(helper, MachineKind.GAS_HOLDER, master);
		BlockPos at = helper.absolutePos(master);
		Storage<FluidVariant> holder = FluidStorage.SIDED.find(helper.getLevel(), at, Direction.UP);
		try (Transaction transaction = Transaction.openOuter()) {
			holder.insert(FluidVariant.of(PetroFluids.OXYGEN.fluid()), 10 * FluidConstants.BUCKET, transaction);
			transaction.commit();
		}
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftGear.SCUBA_TANK));
		ItemStack tank = player.getItemInHand(InteractionHand.MAIN_HAND);
		tank.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false)));
		helper.assertTrue(ScubaTankItem.oxygen(tank) == ScubaTankItem.CAPACITY, "The tank holds " + ScubaTankItem.oxygen(tank) + " mB");
		long left = StorageUtil.simulateExtract(holder, FluidVariant.of(PetroFluids.OXYGEN.fluid()), Long.MAX_VALUE, null);
		helper.assertTrue(left == 2_000 * FluidNetworks.DROPLETS_PER_MB, "The holder has " + left / FluidNetworks.DROPLETS_PER_MB + " mB left");
		helper.succeed();
	}

	/**
	 * Batch 35: a gas cylinder fills from a gas holder up to its 8,000 mB, empties into a fuel cell when used sneaking,
	 * refuses liquids and a second gas, and tops up a scuba tank in the other hand with its oxygen.
	 */
	@GameTest
	public void gasCylinderCarriesGas(GameTestHelper helper) {
		BlockPos master = new BlockPos(5, 1, 1);
		placeUnpowered(helper, MachineKind.GAS_HOLDER, master);
		BlockPos at = helper.absolutePos(master);
		Storage<FluidVariant> holder = FluidStorage.SIDED.find(helper.getLevel(), at, Direction.UP);
		FluidVariant hydrogen = FluidVariant.of(PetroFluids.HYDROGEN.fluid());
		try (Transaction transaction = Transaction.openOuter()) {
			holder.insert(hydrogen, 10 * FluidConstants.BUCKET, transaction);
			transaction.commit();
		}
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftFluids.GAS_CYLINDER));
		player.getItemInHand(InteractionHand.MAIN_HAND).useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false)));
		StoredFluid held = GasCylinderItem.contents(player.getItemInHand(InteractionHand.MAIN_HAND));
		helper.assertTrue(held != null && held.variant().equals(hydrogen)
				&& held.amount() == GasCylinderItem.CAPACITY * FluidNetworks.DROPLETS_PER_MB, "The cylinder holds " + held);
		long left = StorageUtil.simulateExtract(holder, hydrogen, Long.MAX_VALUE, null);
		helper.assertTrue(left == 2_000 * FluidNetworks.DROPLETS_PER_MB, "The holder has " + left / FluidNetworks.DROPLETS_PER_MB + " mB left");

		Storage<FluidVariant> cylinder = GasCylinderItem.storage(
				ContainerItemContext.withConstant(player.getItemInHand(InteractionHand.MAIN_HAND)));
		helper.assertTrue(StorageUtil.simulateInsert(cylinder, FluidVariant.of(PetroFluids.OXYGEN.fluid()), FluidConstants.BUCKET, null) == 0,
				"A hydrogen cylinder took oxygen");
		Storage<FluidVariant> empty = GasCylinderItem.storage(ContainerItemContext.withConstant(new ItemStack(JugcraftFluids.GAS_CYLINDER)));
		helper.assertTrue(StorageUtil.simulateInsert(empty, FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, null) == 0,
				"The cylinder took water");

		BlockPos cellPos = new BlockPos(1, 1, 5);
		helper.setBlock(cellPos, JugcraftMachines.MACHINES.get(MachineKind.FUEL_CELL).defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
		MachineBlockEntity cell = helper.getBlockEntity(cellPos, MachineBlockEntity.class);
		BlockPos cellAt = helper.absolutePos(cellPos);
		player.setShiftKeyDown(true);
		player.getItemInHand(InteractionHand.MAIN_HAND).useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(cellAt), Direction.UP, cellAt, false)));
		helper.assertTrue(GasCylinderItem.contents(player.getItemInHand(InteractionHand.MAIN_HAND)) == null,
				"The cylinder kept " + GasCylinderItem.contents(player.getItemInHand(InteractionHand.MAIN_HAND)));
		helper.assertTrue(cell.tanks().input(0).millibuckets() == GasCylinderItem.CAPACITY,
				"The fuel cell got " + cell.tanks().input(0).millibuckets() + " mB");
		player.setShiftKeyDown(false);

		FluidVariant oxygen = FluidVariant.of(PetroFluids.OXYGEN.fluid());
		player.setItemInHand(InteractionHand.MAIN_HAND, GasCylinderItem.filled(new ItemStack(JugcraftFluids.GAS_CYLINDER), oxygen,
				3_000 * FluidNetworks.DROPLETS_PER_MB));
		player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(JugcraftGear.SCUBA_TANK));
		player.getItemInHand(InteractionHand.MAIN_HAND).use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		int scuba = ScubaTankItem.oxygen(player.getItemInHand(InteractionHand.OFF_HAND));
		helper.assertTrue(scuba == 3_000, "The scuba tank got " + scuba + " mB");
		helper.assertTrue(GasCylinderItem.contents(player.getItemInHand(InteractionHand.MAIN_HAND)) == null, "The oxygen cylinder is not empty");
		helper.succeed();
	}

	/** Places a powered one-block machine facing north. */
	private static MachineBlockEntity placeSingle(GameTestHelper helper, MachineKind kind, BlockPos pos) {
		helper.setBlock(pos, JugcraftMachines.MACHINES.get(kind).defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
		SimpleEnergyStorage energy = (SimpleEnergyStorage) EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(pos), Direction.UP);
		energy.setAmount(energy.getCapacity());
		return helper.getBlockEntity(pos, MachineBlockEntity.class);
	}

	/**
	 * Batch 35: the ammonia chiller freezes a bucket of water into ice for 5 mB of ammonia, and presses four packed ice
	 * into blue ice; it refuses lava.
	 */
	@GameTest(maxTicks = 400)
	public void ammoniaChillerMakesIce(GameTestHelper helper) {
		MachineBlockEntity freezer = placeSingle(helper, MachineKind.AMMONIA_CHILLER, new BlockPos(1, 1, 1));
		MachineBlockEntity packer = placeSingle(helper, MachineKind.AMMONIA_CHILLER, new BlockPos(4, 1, 1));
		Storage<FluidVariant> inlet = FluidStorage.SIDED.find(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), Direction.UP);
		try (Transaction transaction = Transaction.openOuter()) {
			long ammonia = inlet.insert(FluidVariant.of(PetroFluids.AMMONIA.fluid()), FluidConstants.BUCKET, transaction);
			long water = inlet.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction);
			long lava = inlet.insert(FluidVariant.of(Fluids.LAVA), FluidConstants.BUCKET, transaction);
			helper.assertTrue(ammonia == FluidConstants.BUCKET && water == FluidConstants.BUCKET,
					"The chiller took " + ammonia / 81 + " mB ammonia and " + water / 81 + " mB water");
			helper.assertTrue(lava == 0, "The chiller took lava");
			transaction.commit();
		}
		packer.tanks().input(0).fill(PetroFluids.AMMONIA.fluid(), 100);
		packer.setItem(0, new ItemStack(Items.PACKED_ICE, 4));
		helper.succeedWhen(() -> {
			int out = MachineKind.AMMONIA_CHILLER.outputSlot();
			helper.assertTrue(freezer.getItem(out).is(Items.ICE), "Freezer output is " + freezer.getItem(out));
			helper.assertTrue(freezer.tanks().input(0).millibuckets() == 995 && freezer.tanks().input(1).millibuckets() == 0,
					"Freezer tanks: " + freezer.tanks().input(0).millibuckets() + " ammonia, " + freezer.tanks().input(1).millibuckets() + " water");
			helper.assertTrue(packer.getItem(out).is(Items.BLUE_ICE) && packer.getItem(0).isEmpty(), "Packer output is " + packer.getItem(out));
		});
	}

	/**
	 * Batch 43 (liquid fuels): the cryogenic liquefier condenses a bucket of oxygen into 250 mB of liquid oxygen; RP-1
	 * kerosene burns in the gas turbine and the advanced engine.
	 */
	@GameTest(maxTicks = 300)
	public void cryogenicLiquefierMakesLiquidOxygen(GameTestHelper helper) {
		MachineBlockEntity liquefier = placeSingle(helper, MachineKind.CRYOGENIC_LIQUEFIER, new BlockPos(1, 1, 1));
		liquefier.tanks().input(0).fill(PetroFluids.OXYGEN.fluid(), 1000);
		helper.assertTrue(io.github.jimbozoomer.jugcraft.chemistry.FluidFuels.jePerMb(MachineKind.GAS_TURBINE,
				PetroFluids.KEROSENE.source()) == io.github.jimbozoomer.jugcraft.chemistry.FluidFuels.KEROSENE,
				"Kerosene does not burn in the gas turbine");
		helper.assertTrue(io.github.jimbozoomer.jugcraft.chemistry.FluidFuels.jePerMb(MachineKind.ADVANCED_ENGINE,
				PetroFluids.KEROSENE.source()) == io.github.jimbozoomer.jugcraft.chemistry.FluidFuels.ADVANCED_KEROSENE,
				"Kerosene does not burn in the advanced engine");
		helper.succeedWhen(() -> {
			int lox = liquefier.tanks().output(0).millibuckets();
			helper.assertTrue(lox == 250 && liquefier.tanks().output(0).variant.isOf(PetroFluids.LIQUID_OXYGEN.source()),
					"The liquefier holds " + lox + " mB of " + liquefier.tanks().output(0).variant);
			helper.assertTrue(liquefier.tanks().input(0).millibuckets() == 0, "Oxygen left: " + liquefier.tanks().input(0).millibuckets());
		});
	}

	/** Batch 30: the pneumatic grapple fills with nitrogen from a gas holder, up to its 4,000 mB. */
	@GameTest
	public void grappleFillsFromAGasHolder(GameTestHelper helper) {
		BlockPos master = new BlockPos(5, 1, 1);
		placeUnpowered(helper, MachineKind.GAS_HOLDER, master);
		BlockPos at = helper.absolutePos(master);
		Storage<FluidVariant> holder = FluidStorage.SIDED.find(helper.getLevel(), at, Direction.UP);
		try (Transaction transaction = Transaction.openOuter()) {
			holder.insert(FluidVariant.of(PetroFluids.NITROGEN.fluid()), 10 * FluidConstants.BUCKET, transaction);
			transaction.commit();
		}
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftGrapple.PNEUMATIC_GRAPPLE));
		ItemStack grapple = player.getItemInHand(InteractionHand.MAIN_HAND);
		grapple.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false)));
		helper.assertTrue(PneumaticGrappleItem.nitrogen(grapple) == JugcraftGrapple.CAPACITY, "The grapple holds " + PneumaticGrappleItem.nitrogen(grapple) + " mB");
		long left = StorageUtil.simulateExtract(holder, FluidVariant.of(PetroFluids.NITROGEN.fluid()), Long.MAX_VALUE, null);
		helper.assertTrue(left == 6_000 * FluidNetworks.DROPLETS_PER_MB, "The holder has " + left / FluidNetworks.DROPLETS_PER_MB + " mB left");
		helper.succeed();
	}

	/** A survival player holding a grapple with {@code nitrogen} mB, standing at {@code pos}. */
	private static ServerPlayer grappler(GameTestHelper helper, int nitrogen, Vec3 pos) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		Vec3 world = helper.absoluteVec(pos);
		player.setPos(world.x, world.y, world.z);
		ItemStack grapple = new ItemStack(JugcraftGrapple.PNEUMATIC_GRAPPLE);
		PneumaticGrappleItem.setNitrogen(grapple, nitrogen);
		player.setItemInHand(InteractionHand.MAIN_HAND, grapple);
		return player;
	}

	/** Batch 30: a shot costs 25 mB and puts a hook out; using it again lets go; an empty grapple does not fire. */
	@GameTest
	public void grappleFiresAndLetsGo(GameTestHelper helper) {
		ServerPlayer player = grappler(helper, 100, new Vec3(2.5, 1, 2.5));
		ItemStack grapple = player.getMainHandItem();
		JugcraftGrapple.PNEUMATIC_GRAPPLE.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(GrappleHook.active(player) != null, "No hook out");
		helper.assertTrue(PneumaticGrappleItem.nitrogen(grapple) == 75, "Nitrogen left: " + PneumaticGrappleItem.nitrogen(grapple));
		JugcraftGrapple.PNEUMATIC_GRAPPLE.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(GrappleHook.active(player) == null, "The hook is still out");
		helper.assertTrue(PneumaticGrappleItem.nitrogen(grapple) == 75, "Letting go used nitrogen");
		ServerPlayer empty = grappler(helper, JugcraftGrapple.SHOT_COST - 1, new Vec3(4.5, 1, 4.5));
		JugcraftGrapple.PNEUMATIC_GRAPPLE.use(helper.getLevel(), empty, InteractionHand.MAIN_HAND);
		helper.assertTrue(GrappleHook.active(empty) == null, "An empty grapple fired");
		helper.succeed();
	}

	/** A hook flying from {@code from} along {@code motion}, owned by {@code owner}. */
	private static GrappleHook launch(GameTestHelper helper, ServerPlayer owner, Vec3 from, Vec3 motion) {
		GrappleHook hook = new GrappleHook(helper.getLevel(), owner, owner.getMainHandItem().copyWithCount(1));
		Vec3 world = helper.absoluteVec(from);
		hook.setPos(world.x, world.y, world.z);
		hook.setDeltaMovement(motion);
		helper.getLevel().addFreshEntity(hook);
		return hook;
	}

	/** Batch 30: a hook that bites a wall reels its owner toward it and takes away their fall distance. */
	@GameTest(maxTicks = 100)
	public void grappleReelsItsOwnerToAWall(GameTestHelper helper) {
		for (int y = 1; y <= 4; y++) {
			for (int z = 0; z <= 4; z++) {
				helper.setBlock(new BlockPos(7, y, z), Blocks.STONE);
			}
		}
		ServerPlayer player = grappler(helper, 100, new Vec3(1.5, 1, 2.5));
		player.fallDistance = 10;
		GrappleHook hook = launch(helper, player, new Vec3(3.0, 2.5, 2.5), new Vec3(1.5, 0, 0));
		helper.succeedWhen(() -> {
			helper.assertTrue(hook.isAnchored(), "The hook has not bitten");
			helper.assertTrue(player.getDeltaMovement().x > 0.5, "The owner is not reeled in: " + player.getDeltaMovement());
			helper.assertTrue(player.fallDistance == 0, "Fall distance " + player.fallDistance);
		});
	}

	/** Batch 30: a hooked pig is dragged toward the owner; an iron golem is too heavy and the hook lets go. */
	@GameTest(maxTicks = 100)
	public void grappleDragsLightMobsOnly(GameTestHelper helper) {
		ServerPlayer player = grappler(helper, 100, new Vec3(1.5, 1, 1.5));
		Mob pig = helper.spawn(EntityTypes.PIG, new BlockPos(6, 1, 1));
		Mob golem = helper.spawn(EntityTypes.IRON_GOLEM, new BlockPos(6, 1, 5));
		golem.setNoAi(true);
		GrappleHook onPig = launch(helper, player, new Vec3(3.0, 1.5, 1.5), new Vec3(1.5, 0, 0));
		GrappleHook onGolem = launch(helper, player, new Vec3(3.0, 1.5, 5.5), new Vec3(1.5, 0, 0));
		helper.succeedWhen(() -> {
			helper.assertTrue(onPig.hookedId() == pig.getId() || pig.getX() < helper.absoluteVec(new Vec3(5, 0, 0)).x,
					"The pig was not hooked");
			helper.assertTrue(pig.getX() < helper.absoluteVec(new Vec3(5, 0, 0)).x, "The pig was not dragged: " + pig.position());
			helper.assertTrue(onGolem.isRemoved() && onGolem.hookedId() < 0, "The golem was hooked");
		});
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

	/** Batch 31: chlorine hurts a pig, not a pig in a gas mask (whose filter wears), a sealed scuba set or a zombie. */
	@GameTest
	public void chlorineHurtsWhatBreathesUnlessMasked(GameTestHelper helper) {
		Mob bare = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(3, 1, 3));
		Mob masked = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(4, 1, 3));
		Mob diver = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(3, 1, 4));
		Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(4, 1, 4));
		masked.setItemSlot(EquipmentSlot.HEAD, new ItemStack(FieldChemistry.GAS_MASK));
		ItemStack tank = new ItemStack(JugcraftGear.SCUBA_TANK);
		ScubaTankItem.setOxygen(tank, 1000);
		diver.setItemSlot(EquipmentSlot.HEAD, new ItemStack(JugcraftGear.SCUBA_MASK));
		diver.setItemSlot(EquipmentSlot.CHEST, tank);
		ChemicalCloud cloud = ChemicalCloud.spawn(helper.getLevel(), helper.absoluteVec(new Vec3(4.0, 1.5, 4.0)),
				ChemicalCloud.Kind.CHLORINE, null);
		helper.assertTrue(bare.getHealth() < bare.getMaxHealth(), "The bare pig was not hurt: " + bare.getHealth());
		helper.assertTrue(masked.getHealth() == masked.getMaxHealth(), "The masked pig was hurt: " + masked.getHealth());
		helper.assertTrue(masked.getItemBySlot(EquipmentSlot.HEAD).getDamageValue() == 1, "The filter did not wear");
		helper.assertTrue(diver.getHealth() == diver.getMaxHealth(), "The diver was hurt: " + diver.getHealth());
		helper.assertTrue(ScubaTankItem.oxygen(diver.getItemBySlot(EquipmentSlot.CHEST)) == 1000 - FieldChemistry.SCUBA_GAS_OXYGEN,
				"Oxygen left: " + ScubaTankItem.oxygen(diver.getItemBySlot(EquipmentSlot.CHEST)));
		helper.assertTrue(zombie.getHealth() == zombie.getMaxHealth(), "The zombie was hurt: " + zombie.getHealth());
		helper.assertTrue(!cloud.shouldBeSaved(), "Clouds should not be saved");
		helper.succeed();
	}

	/** Batch 31: a mob hunting something inside smoke loses its target. */
	@GameTest(maxTicks = 40)
	public void smokeHidesFromMobs(GameTestHelper helper) {
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 1, 2));
		Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(9, 1, 2));
		zombie.setTarget(pig);
		ChemicalCloud.spawn(helper.getLevel(), helper.absoluteVec(new Vec3(2.5, 1.5, 2.5)), ChemicalCloud.Kind.SMOKE, null);
		helper.succeedWhen(() -> helper.assertTrue(zombie.getTarget() == null, "The zombie still sees the pig"));
	}

	/** Batch 31: thermite burns what stands in it and lights no block. */
	@GameTest(maxTicks = 60)
	public void thermiteBurnsButLightsNothing(GameTestHelper helper) {
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(3, 1, 3));
		helper.setBlock(new BlockPos(4, 1, 3), Blocks.OAK_PLANKS);
		Warhead.THERMITE.detonate(helper.getLevel(), helper.absoluteVec(new Vec3(3.5, 2.5, 3.5)), null, null);
		helper.assertTrue(pig.getHealth() < pig.getMaxHealth() && pig.getRemainingFireTicks() > 0,
				"The pig was not burnt: " + pig.getHealth() + ", fire " + pig.getRemainingFireTicks());
		helper.runAfterDelay(40, () -> {
			for (int x = 1; x <= 5; x++) {
				for (int z = 1; z <= 5; z++) {
					BlockPos pos = new BlockPos(x, 1, z);
					helper.assertTrue(!helper.getBlockState(pos).is(Blocks.FIRE), "Fire at " + pos);
				}
			}
			helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(4, 1, 3));
			helper.succeed();
		});
	}

	/** Batch 31: a flashbang staggers a mob that sees it, not one behind a wall, and hurts nothing. */
	@GameTest
	public void flashbangStaggersWhatSeesIt(GameTestHelper helper) {
		for (int y = 1; y <= 3; y++) {
			for (int z = 1; z <= 6; z++) {
				helper.setBlock(new BlockPos(6, y, z), Blocks.STONE);
			}
		}
		Mob seen = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(2, 1, 6));
		Mob hidden = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(8, 1, 3));
		int dazzled = Flash.detonate(helper.getLevel(), helper.absoluteVec(new Vec3(4.5, 1.5, 3.5)), null);
		helper.assertTrue(seen.hasEffect(MobEffects.SLOWNESS), "The zombie in sight was not staggered");
		helper.assertTrue(!hidden.hasEffect(MobEffects.SLOWNESS), "The wall did not shield the flash");
		helper.assertTrue(dazzled == 1 && seen.getHealth() == seen.getMaxHealth(), "Dazzled " + dazzled + ", health " + seen.getHealth());
		helper.succeed();
	}

	/** Batch 31: the antidote clears poison and keeps speed; the stimulant hastes; the first aid kit heals. */
	@GameTest(maxTicks = 40)
	public void medicinesWork(GameTestHelper helper) {
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 1, 2));
		pig.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
		pig.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 0));
		new ItemStack(FieldChemistry.ANTIDOTE).finishUsingItem(helper.getLevel(), pig);
		helper.assertTrue(!pig.hasEffect(MobEffects.POISON), "The antidote left the poison");
		helper.assertTrue(pig.hasEffect(MobEffects.SPEED), "The antidote cleared speed");
		new ItemStack(FieldChemistry.STIMULANT).finishUsingItem(helper.getLevel(), pig);
		helper.assertTrue(pig.hasEffect(MobEffects.HASTE), "The stimulant gave no haste");
		pig.setHealth(2.0F);
		new ItemStack(FieldChemistry.FIRST_AID_KIT).finishUsingItem(helper.getLevel(), pig);
		// Instant health works on the pig's next tick.
		helper.succeedWhen(() -> helper.assertTrue(pig.getHealth() >= 9.0F, "The first aid kit healed to " + pig.getHealth()));
	}

	/** Batch 32: foam fills open space (air and water) but no solid block or space a mob stands in; canisters run down; cement sets foam. */
	@GameTest
	public void foamFillsOpenSpaceAndCementSetsIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
		helper.setBlock(new BlockPos(2, 1, 2), Blocks.WATER);
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(4, 1, 2));
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		player.setPos(helper.absoluteVec(new Vec3(0.5, 1, 7.5)));
		BlockPos start = helper.absolutePos(new BlockPos(3, 1, 2));
		List<BlockPos> filled = FoamSprayerItem.fill(level, player, start, ConstructionChemistry.SPRAY_BLOCKS);
		helper.assertTrue(!filled.isEmpty() && filled.size() <= ConstructionChemistry.SPRAY_BLOCKS, "Filled " + filled.size());
		helper.assertTrue(filled.contains(helper.absolutePos(new BlockPos(2, 1, 2))), "The foam did not fill the water");
		helper.assertTrue(!filled.contains(helper.absolutePos(new BlockPos(3, 1, 3))), "The foam replaced stone");
		helper.assertTrue(filled.stream().noneMatch(pos -> pig.getBoundingBox().intersects(new net.minecraft.world.phys.AABB(pos))),
				"The foam filled the pig's space");
		helper.assertTrue(filled.stream().allMatch(pos -> pos.distSqr(start) <= ConstructionChemistry.SPRAY_RADIUS
				* ConstructionChemistry.SPRAY_RADIUS), "The foam spread too far");

		player.getInventory().add(new ItemStack(ConstructionChemistry.FOAM_CANISTER));
		helper.assertTrue(FoamSprayerItem.foamLeft(player) == ConstructionChemistry.CANISTER_FOAM, "Foam: " + FoamSprayerItem.foamLeft(player));
		FoamSprayerItem.useFoam(player, 12);
		helper.assertTrue(FoamSprayerItem.foamLeft(player) == ConstructionChemistry.CANISTER_FOAM - 12, "Foam left: " + FoamSprayerItem.foamLeft(player));
		FoamSprayerItem.useFoam(player, ConstructionChemistry.CANISTER_FOAM - 12);
		helper.assertTrue(FoamSprayerItem.foamLeft(player) == 0, "The empty canister was not used up");

		BlockPos foam = new BlockPos(5, 1, 5);
		helper.setBlock(foam, ConstructionChemistry.CONSTRUCTION_FOAM);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ConstructionChemistry.CEMENT, 2));
		ConstructionChemistry.CEMENT.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(helper.absolutePos(foam)), Direction.UP, helper.absolutePos(foam), false)));
		helper.assertBlockPresent(ConstructionChemistry.CONCRETE, foam);
		helper.assertTrue(player.getMainHandItem().getCount() == 1, "Cement left: " + player.getMainHandItem());
		helper.assertTrue(ConstructionChemistry.BLASTPROOF_CONCRETE.getExplosionResistance() >= 1200.0F,
				"Blast-proof concrete is not blast-proof");
		helper.succeed();
	}
}
