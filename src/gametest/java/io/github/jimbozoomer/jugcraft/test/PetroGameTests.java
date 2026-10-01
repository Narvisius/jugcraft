package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.OilReservoirs;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
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
}
