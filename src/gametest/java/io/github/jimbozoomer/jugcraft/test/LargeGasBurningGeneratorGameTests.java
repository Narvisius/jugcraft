package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
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
 * The Large Gas Burning Generator (docs/features/industrial-large-gas-burning-generator.md), the Gas Burning Generator's
 * 5x6x5 variant: it stands whole in every orientation with pipes and cables reaching it only at its two ports; it burns
 * hydrogen at 128 JE a millibucket and 512 JE a tick and methane at 504 and 1,024, four times as fast as the Gas Burning
 * Generator, and nothing else; every part lights while it runs; and it is built from a Gas Burning Generator and its bill.
 */
public class LargeGasBurningGeneratorGameTests {
	private static final String ARENA = "jugcraft-test:arms_arena";
	private static final int PER_MB = 128;
	private static final int PER_TICK = 512;
	private static final int METHANE_PER_MB = 504;
	private static final int METHANE_PER_TICK = 1_024;

	/**
	 * In every orientation it fills its blocks and leaves its open positions free, and its faces answer pipes, cables and
	 * conveyors only at its inlet and socket: bioethanol goes in and nothing else, and the socket only gives power.
	 */
	@GameTest(structure = ARENA)
	public void itStandsWholeAndOnlyItsPortsAnswer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MachineForm form = IndustrialForms.LARGE_GAS_BURNING_GENERATOR_FORM;
		int parts = form.footprint().size();
		helper.assertTrue(form.generator() && form.positions() == 150 && parts == 101
				&& IndustrialForms.LARGE_GAS_BURNING_GENERATOR.partProperty().getPossibleValues().size() == parts, "101 blocks in a 5x6x5 envelope");
		Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
		BlockPos[] controllers = {new BlockPos(6, 1, 1), new BlockPos(14, 1, 5), new BlockPos(1, 1, 14), new BlockPos(8, 1, 8)};
		for (int i = 0; i < facings.length; i++) {
			Direction facing = facings[i];
			FormMachineBlockEntity machine = place(helper, controllers[i], facing);
			BlockPos controller = machine.getBlockPos();
			for (int part = 0; part < parts; part++) {
				BlockPos at = form.footprint().partPos(controller, facing, part);
				BlockState state = level.getBlockState(at);
				helper.assertTrue(state.is(IndustrialForms.LARGE_GAS_BURNING_GENERATOR) && IndustrialForms.LARGE_GAS_BURNING_GENERATOR.part(state) == part
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
				helper.assertTrue(inlet.insert(FluidVariant.of(PetroFluids.OXYGEN.fluid()), FluidConstants.BUCKET, transaction) == 0,
						facing + ": no oxygen");
				helper.assertTrue(inlet.insert(FluidVariant.of(PetroFluids.BIOETHANOL.source()), FluidConstants.BUCKET, transaction) == 0,
						facing + ": no bioethanol, the Bio Generator's fuel");
				helper.assertTrue(inlet.insert(FluidVariant.of(PetroFluids.HYDROGEN.fluid()), FluidConstants.BUCKET, transaction) == FluidConstants.BUCKET,
						facing + ": the inlet takes hydrogen");
				transaction.abort();
			}
		}
		helper.succeed();
	}

	/**
	 * Hydrogen burns at 512 JE a tick, every millibucket burnt being 128 JE made or still in hand, and the generator reads
	 * as generating.
	 */
	@GameTest(structure = ARENA, maxTicks = 100)
	public void hydrogenBurnsAt512JEATick(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(6, 1, 2), Direction.NORTH);
		insert(helper, port(helper, machine, "fuel_in"), PetroFluids.HYDROGEN.fluid(), 1000);
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
					"Not 512 JE a tick: " + (machine.energy().getAmount() - earlier[0]) + " in ten ticks");
			helper.succeed();
		});
	}

	/** Methane burns at 1,024 JE a tick, every millibucket burnt being 504 JE, through the same inlet. */
	@GameTest(structure = ARENA, maxTicks = 100)
	public void methaneBurnsAt1024JEATick(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(6, 1, 2), Direction.NORTH);
		insert(helper, port(helper, machine, "fuel_in"), PetroFluids.METHANE.fluid(), 1000);
		long[] earlier = new long[1];
		helper.runAfterDelay(20, () -> {
			int burnt = 1000 - machine.tanks().input(0).millibuckets();
			long made = machine.energy().getAmount();
			helper.assertTrue(burnt > 0 && made + machine.burn() == (long) burnt * METHANE_PER_MB,
					"Burnt " + burnt + " mB for " + made + " JE, with " + machine.burn() + " JE in hand");
			earlier[0] = made;
		});
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(machine.energy().getAmount() - earlier[0] == 10L * METHANE_PER_TICK,
					"Not 1,024 JE a tick: " + (machine.energy().getAmount() - earlier[0]) + " in ten ticks");
			helper.succeed();
		});
	}

	/** While it runs every part shows lit, but only the controller gives off light. */
	@GameTest(structure = ARENA, maxTicks = 100)
	public void everyPartLightsWhileItRuns(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(6, 1, 2), Direction.NORTH);
		insert(helper, port(helper, machine, "fuel_in"), PetroFluids.HYDROGEN.fluid(), 1000);
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
	 * Its fuels are the Gas Burning Generator's, four times as fast: hydrogen no better (it would beat electrolysis's cost)
	 * and methane an eighth better, still below the Combined Cycle Power Plant; and a Gas Burning Generator, two Steel
	 * Tanks, steel plates, a steel gear and a basic circuit build it.
	 */
	@GameTest
	public void itsFuelsAndItsBill(GameTestHelper helper) {
		MachineForm form = IndustrialForms.LARGE_GAS_BURNING_GENERATOR_FORM;
		MachineForm small = IndustrialForms.GAS_BURNING_GENERATOR_FORM;
		FormFuel hydrogen = form.fuel(PetroFluids.HYDROGEN.fluid());
		FormFuel methane = form.fuel(PetroFluids.METHANE.fluid());
		helper.assertTrue(form.fuels().size() == 2 && hydrogen != null && hydrogen.jePerMb() == PER_MB && hydrogen.jePerTick() == PER_TICK
				&& methane != null && methane.jePerMb() == METHANE_PER_MB && methane.jePerTick() == METHANE_PER_TICK,
				"Hydrogen at 128 JE/mB and 512 JE/t, methane at 504 and 1,024");
		FormFuel smallHydrogen = small.fuel(PetroFluids.HYDROGEN.fluid());
		FormFuel smallMethane = small.fuel(PetroFluids.METHANE.fluid());
		helper.assertTrue(hydrogen.jePerMb() == smallHydrogen.jePerMb() && hydrogen.jePerTick() == 4 * smallHydrogen.jePerTick()
				&& methane.jePerMb() * 8 == smallMethane.jePerMb() * 9 && methane.jePerTick() == 4 * smallMethane.jePerTick(),
				"Four times as fast, hydrogen no better and methane an eighth better");
		FormFuel plant = IndustrialForms.COMBINED_CYCLE_PLANT_FORM.fuel(PetroFluids.METHANE.fluid());
		helper.assertTrue(methane.jePerMb() < plant.jePerMb() && methane.jePerTick() < plant.jePerTick(),
				"The Combined Cycle Power Plant still gets more from methane, faster");
		RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
		ItemStack tank = stack("steel_tank");
		ItemStack plate = stack("steel_plate");
		CraftingInput bill = CraftingInput.of(3, 3, List.of(tank, new ItemStack(IndustrialForms.GAS_BURNING_GENERATOR), tank, plate,
				stack("steel_gear"), plate, plate, stack("basic_circuit"), plate));
		Optional<RecipeHolder<CraftingRecipe>> built = crafting.getRecipeFor(bill, helper.getLevel());
		helper.assertTrue(built.isPresent() && built.get().value().assemble(bill).is(IndustrialForms.LARGE_GAS_BURNING_GENERATOR.asItem()),
				"Its bill makes the Large Gas Burning Generator");
		helper.succeed();
	}

	// ------------------------------------------------------------------ helpers

	private static FormMachineBlockEntity place(GameTestHelper helper, BlockPos relative, Direction facing) {
		helper.setBlock(relative, IndustrialForms.LARGE_GAS_BURNING_GENERATOR.defaultBlockState().setValue(MachineBlock.FACING, facing));
		IndustrialForms.LARGE_GAS_BURNING_GENERATOR.setPlacedBy(helper.getLevel(), helper.absolutePos(relative), helper.getBlockState(relative), null,
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
