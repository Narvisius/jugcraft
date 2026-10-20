package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.FluidFuels;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.form.FormCell;
import io.github.jimbozoomer.jugcraft.machine.form.FormFuel;
import io.github.jimbozoomer.jugcraft.machine.form.FormMachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.form.FormPort;
import io.github.jimbozoomer.jugcraft.machine.form.FormSide;
import io.github.jimbozoomer.jugcraft.machine.form.IndustrialForms;
import io.github.jimbozoomer.jugcraft.machine.form.MachineForm;
import io.github.jimbozoomer.jugcraft.machine.form.MachineLifecycle;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The Large Bio Generator (docs/features/industrial-large-bio-generator.md), the Bio Generator's 5x6x5 variant: it stands
 * whole in every orientation with pipes and cables reaching it only at its two ports; it burns bioethanol at 240 JE a
 * millibucket and 384 JE a tick and biogas at 335 and 256, a quarter better and four times as fast as the Bio Generator,
 * and nothing else; every part lights while it runs; and it is built from a Bio Generator and its bill.
 */
public class LargeBioGeneratorGameTests {
	private static final String ARENA = "jugcraft-test:arms_arena";
	private static final int PER_MB = 240;
	private static final int PER_TICK = 384;
	private static final int BIOGAS_PER_MB = 335;
	private static final int BIOGAS_PER_TICK = 256;

	/**
	 * In every orientation it fills its blocks and leaves its open positions free, and its faces answer pipes, cables and
	 * conveyors only at its inlet and socket: bioethanol goes in and nothing else, and the socket only gives power.
	 */
	@GameTest(structure = ARENA)
	public void itStandsWholeAndOnlyItsPortsAnswer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MachineForm form = IndustrialForms.LARGE_BIO_GENERATOR_FORM;
		int parts = form.footprint().size();
		helper.assertTrue(form.generator() && form.positions() == 150 && parts == 150
				&& IndustrialForms.LARGE_BIO_GENERATOR.partProperty().getPossibleValues().size() == parts, "150 blocks filling a 5x6x5 envelope");
		Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
		BlockPos[] controllers = {new BlockPos(6, 1, 1), new BlockPos(14, 1, 5), new BlockPos(1, 1, 14), new BlockPos(8, 1, 8)};
		for (int i = 0; i < facings.length; i++) {
			Direction facing = facings[i];
			FormMachineBlockEntity machine = place(helper, controllers[i], facing);
			BlockPos controller = machine.getBlockPos();
			for (int part = 0; part < parts; part++) {
				BlockPos at = form.footprint().partPos(controller, facing, part);
				BlockState state = level.getBlockState(at);
				helper.assertTrue(state.is(IndustrialForms.LARGE_BIO_GENERATOR) && IndustrialForms.LARGE_BIO_GENERATOR.part(state) == part
						&& state.getValue(MachineBlock.FACING) == facing, facing + ": part " + part + " is " + state);
				for (Direction side : Direction.values()) {
					FormPort port = form.port(part, FormSide.of(side, facing));
					boolean fluid = FluidStorage.SIDED.find(level, at, side) != null;
					boolean items = ItemStorage.SIDED.find(level, at, side) != null;
					EnergyStorage power = EnergyStorage.SIDED.find(level, at, side);
					helper.assertTrue(fluid == (port != null && port.kind() == FormPort.Kind.FLUID_IN) && !items
							&& (power != null) == (port != null && port.kind() == FormPort.Kind.ENERGY_OUT),
							facing + ": part " + part + " " + side + " answers fluid " + fluid + ", items " + items + ", power " + (power != null));
					helper.assertTrue(power == null || power.supportsExtraction() && !power.supportsInsertion(), facing + ": the socket only gives power");
				}
			}
			for (int cell = 0; cell < form.positions(); cell++) {
				if (form.cell(cell) == FormCell.ACCESS) {
					helper.assertTrue(level.getBlockState(form.cellPos(controller, facing, cell)).isAir(), facing + ": open position " + cell + " stays open");
				}
			}
			Storage<FluidVariant> inlet = port(helper, machine, "fuel_in");
			try (Transaction transaction = Transaction.openOuter()) {
				helper.assertTrue(inlet.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction) == 0, facing + ": no water");
				helper.assertTrue(inlet.insert(FluidVariant.of(PetroFluids.HYDROGEN.fluid()), FluidConstants.BUCKET, transaction) == 0,
						facing + ": no hydrogen, the gas generator's fuel");
				helper.assertTrue(inlet.insert(FluidVariant.of(PetroFluids.DIESEL.source()), FluidConstants.BUCKET, transaction) == 0,
						facing + ": no diesel, the Diesel Generator's fuel");
				helper.assertTrue(inlet.insert(FluidVariant.of(PetroFluids.BIOETHANOL.source()), FluidConstants.BUCKET, transaction) == FluidConstants.BUCKET,
						facing + ": the inlet takes bioethanol");
				transaction.abort();
			}
		}
		helper.succeed();
	}

	/**
	 * Bioethanol burns at 384 JE a tick, every millibucket burnt being 240 JE made or still in hand, and the generator
	 * reads as generating.
	 */
	@GameTest(structure = ARENA, maxTicks = 100)
	public void bioethanolBurnsAt384JEATick(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(6, 1, 2), Direction.NORTH);
		insert(helper, port(helper, machine, "fuel_in"), PetroFluids.BIOETHANOL.source(), 1000);
		long[] earlier = new long[1];
		helper.runAfterDelay(20, () -> {
			int burnt = 1000 - machine.tanks().input(0).millibuckets();
			long made = machine.energy().getAmount();
			helper.assertTrue(burnt > 0 && made + machine.burn() == (long) burnt * PER_MB,
					"Burnt " + burnt + " mB for " + made + " JE, with " + machine.burn() + " JE in hand");
			helper.assertTrue(machine.status().state() == MachineLifecycle.PROCESSING, "Generating: " + machine.status());
			earlier[0] = made;
		});
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(machine.energy().getAmount() - earlier[0] == 10L * PER_TICK,
					"Not 384 JE a tick: " + (machine.energy().getAmount() - earlier[0]) + " in ten ticks");
			helper.succeed();
		});
	}

	/** The Anaerobic Digester's biogas burns at 256 JE a tick, every millibucket burnt being 335 JE, through the same inlet. */
	@GameTest(structure = ARENA, maxTicks = 100)
	public void biogasBurnsAt256JEATick(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(6, 1, 2), Direction.NORTH);
		insert(helper, port(helper, machine, "fuel_in"), PetroFluids.BIOGAS.fluid(), 1000);
		long[] earlier = new long[1];
		helper.runAfterDelay(20, () -> {
			int burnt = 1000 - machine.tanks().input(0).millibuckets();
			long made = machine.energy().getAmount();
			helper.assertTrue(burnt > 0 && made + machine.burn() == (long) burnt * BIOGAS_PER_MB,
					"Burnt " + burnt + " mB for " + made + " JE, with " + machine.burn() + " JE in hand");
			earlier[0] = made;
		});
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(machine.energy().getAmount() - earlier[0] == 10L * BIOGAS_PER_TICK,
					"Not 256 JE a tick: " + (machine.energy().getAmount() - earlier[0]) + " in ten ticks");
			helper.succeed();
		});
	}
	/** The Anaerobic Digester's biogas burns at 256 JE a tick, every millibucket burnt being 335 JE, through the same inlet. */
	@GameTest(structure = ARENA, maxTicks = 100)
	public void cleanBiogasBurnsAt256JEATick(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(6, 1, 2), Direction.NORTH);
		insert(helper, port(helper, machine, "fuel_in"), PetroFluids.CLEAN_BIOGAS.fluid(), 1000);
		long[] earlier = new long[1];
		helper.runAfterDelay(20, () -> {
			int burnt = 1000 - machine.tanks().input(0).millibuckets();
			long made = machine.energy().getAmount();
			helper.assertTrue(burnt > 0 && made + machine.burn() == (long) burnt * BIOGAS_PER_MB,
					"Burnt " + burnt + " mB for " + made + " JE, with " + machine.burn() + " JE in hand");
			earlier[0] = made;
		});
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(machine.energy().getAmount() - earlier[0] == 10L * BIOGAS_PER_TICK,
					"Not 256 JE a tick: " + (machine.energy().getAmount() - earlier[0]) + " in ten ticks");
			helper.succeed();
		});
	}

	/** While it runs every part shows lit, but only the controller gives off light. */
	@GameTest(structure = ARENA, maxTicks = 100)
	public void everyPartLightsWhileItRuns(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(6, 1, 2), Direction.NORTH);
		insert(helper, port(helper, machine, "fuel_in"), PetroFluids.BIOETHANOL.source(), 1000);
		helper.runAfterDelay(10, () -> {
			MachineForm form = machine.form();
			for (int part = 0; part < form.footprint().size(); part++) {
				BlockState state = helper.getLevel().getBlockState(form.footprint().partPos(machine.getBlockPos(), Direction.NORTH, part));
				int light = part == 0 ? 13 : 0;
				helper.assertTrue(state.getValue(MachineBlock.LIT) && state.getLightEmission() == light,
						"Part " + part + ": lit " + state.getValue(MachineBlock.LIT) + ", light " + state.getLightEmission());
			}
			helper.succeed();
		});
	}

	/**
	 * Its fuels are the Bio Generator's, each a quarter better (bioethanol still below the advanced engine's 256) and four
	 * times as fast; and a Bio Generator, two Steel Tanks, steel plates, a steel gear and a basic circuit build it.
	 */
	@GameTest
	public void itsFuelsAndItsBill(GameTestHelper helper) {
		MachineForm form = IndustrialForms.LARGE_BIO_GENERATOR_FORM;
		MachineForm small = IndustrialForms.BIO_GENERATOR_FORM;
		FormFuel ethanol = form.fuel(PetroFluids.BIOETHANOL.source());
		FormFuel biogas = form.fuel(PetroFluids.BIOGAS.fluid());
		helper.assertTrue(form.fuels().size() == 3 && ethanol != null && ethanol.jePerMb() == PER_MB && ethanol.jePerTick() == PER_TICK
				&& biogas != null && biogas.jePerMb() == BIOGAS_PER_MB && biogas.jePerTick() == BIOGAS_PER_TICK,
				"Bioethanol at 240 JE/mB and 384 JE/t, biogas at 335 and 256");
		FormFuel smallEthanol = small.fuel(PetroFluids.BIOETHANOL.source());
		FormFuel smallBiogas = small.fuel(PetroFluids.BIOGAS.fluid());
		helper.assertTrue(ethanol.jePerMb() * 4 == smallEthanol.jePerMb() * 5 && ethanol.jePerTick() == 4 * smallEthanol.jePerTick()
				&& biogas.jePerMb() * 4 / 5 == smallBiogas.jePerMb() && biogas.jePerTick() == 4 * smallBiogas.jePerTick(),
				"A quarter better than the Bio Generator and four times as fast");
		helper.assertTrue(ethanol.jePerMb() < FluidFuels.ADVANCED_BIOETHANOL, "Still below the advanced engine for bioethanol");
		RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
		ItemStack tank = stack("steel_tank");
		ItemStack plate = stack("steel_plate");
		CraftingInput bill = CraftingInput.of(3, 3, List.of(tank, new ItemStack(IndustrialForms.BIO_GENERATOR), tank, plate, stack("steel_gear"),
				plate, plate, stack("basic_circuit"), plate));
		Optional<RecipeHolder<CraftingRecipe>> built = crafting.getRecipeFor(bill, helper.getLevel());
		helper.assertTrue(built.isPresent() && built.get().value().assemble(bill).is(IndustrialForms.LARGE_BIO_GENERATOR.asItem()),
				"Its bill makes the Large Bio Generator");
		helper.succeed();
	}

	// ------------------------------------------------------------------ helpers

	private static FormMachineBlockEntity place(GameTestHelper helper, BlockPos relative, Direction facing) {
		helper.setBlock(relative, IndustrialForms.LARGE_BIO_GENERATOR.defaultBlockState().setValue(MachineBlock.FACING, facing));
		IndustrialForms.LARGE_BIO_GENERATOR.setPlacedBy(helper.getLevel(), helper.absolutePos(relative), helper.getBlockState(relative), null,
				ItemStack.EMPTY);
		return helper.getBlockEntity(relative, FormMachineBlockEntity.class);
	}

	private static Storage<FluidVariant> port(GameTestHelper helper, FormMachineBlockEntity machine, String name) {
		MachineForm form = machine.form();
		Direction facing = machine.getBlockState().getValue(MachineBlock.FACING);
		FormPort port = form.ports().stream().filter(p -> p.name().equals(name)).findFirst().orElseThrow();
		BlockPos at = form.cellPos(machine.getBlockPos(), facing, form.cellIndex(port.column(), port.row(), port.layer()));
		Storage<FluidVariant> storage = FluidStorage.SIDED.find(helper.getLevel(), at, port.side().world(facing));
		helper.assertTrue(storage != null, "No storage at port " + name);
		return storage;
	}

	private static void insert(GameTestHelper helper, Storage<FluidVariant> storage, Fluid fluid, int mb) {
		try (Transaction transaction = Transaction.openOuter()) {
			long moved = storage.insert(FluidVariant.of(fluid), mb * FluidConstants.BUCKET / 1000, transaction);
			helper.assertTrue(moved == mb * FluidConstants.BUCKET / 1000, "Only " + moved + " droplets of " + mb + " mB went in");
			transaction.commit();
		}
	}

	private static ItemStack stack(String path) {
		return new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id(path)));
	}
}
