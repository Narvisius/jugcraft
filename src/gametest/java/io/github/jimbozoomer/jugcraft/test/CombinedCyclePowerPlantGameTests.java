package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.FluidFuels;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The Combined Cycle Power Plant (docs/features/industrial-factory-payoffs.md), the factory's big power station: it
 * stands whole in every orientation with pipes and cables reaching it only at its two ports; it burns methane at 528 JE
 * a millibucket and raw syngas at 256, both at 2,048 JE a tick, and hydrogen at 128 and 1,024; it outpowers every other
 * fuel generator several times over; every part lights while it runs; and it is built from its bill.
 */
public class CombinedCyclePowerPlantGameTests {
	private static final String ARENA = "jugcraft-test:arms_arena";
	private static final int METHANE_PER_MB = 528;
	private static final int SYNGAS_PER_MB = 256;
	private static final int HYDROGEN_PER_MB = 128;
	private static final int FULL_RATE = 2_048;
	private static final int HYDROGEN_RATE = 1_024;

	/**
	 * In every orientation it fills its 88 blocks, and its faces answer pipes, cables and conveyors only at its inlet
	 * and socket: methane, raw syngas and hydrogen go in and nothing else, and the socket only gives power.
	 */
	@GameTest(structure = ARENA)
	public void itStandsWholeAndOnlyItsPortsAnswer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MachineForm form = IndustrialForms.COMBINED_CYCLE_PLANT_FORM;
		int parts = form.footprint().size();
		helper.assertTrue(form.generator() && form.positions() == 120 && parts == 88
				&& IndustrialForms.COMBINED_CYCLE_PLANT.partProperty().getPossibleValues().size() == parts,
				"88 blocks in a 6x4x5 envelope, not " + parts + " of " + form.positions());
		// Each orientation in turn at the arena's middle, cleared before the next.
		BlockPos middle = new BlockPos(8, 1, 8);
		for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
			FormMachineBlockEntity machine = place(helper, middle, facing);
			BlockPos controller = machine.getBlockPos();
			for (int part = 0; part < parts; part++) {
				BlockPos at = form.footprint().partPos(controller, facing, part);
				BlockState state = level.getBlockState(at);
				helper.assertTrue(state.is(IndustrialForms.COMBINED_CYCLE_PLANT) && IndustrialForms.COMBINED_CYCLE_PLANT.part(state) == part
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
			Storage<FluidVariant> inlet = port(helper, machine, "fuel_in");
			try (Transaction transaction = Transaction.openOuter()) {
				helper.assertTrue(inlet.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction) == 0, facing + ": no water");
				helper.assertTrue(inlet.insert(FluidVariant.of(PetroFluids.BIOGAS.fluid()), FluidConstants.BUCKET, transaction) == 0,
						facing + ": no raw biogas, the Bio Generator's fuel");
				helper.assertTrue(inlet.insert(FluidVariant.of(PetroFluids.METHANE.fluid()), FluidConstants.BUCKET, transaction) == FluidConstants.BUCKET,
						facing + ": the inlet takes methane");
				transaction.abort();
			}
			try (Transaction transaction = Transaction.openOuter()) {
				helper.assertTrue(inlet.insert(FluidVariant.of(PetroFluids.RAW_SYNGAS.fluid()), FluidConstants.BUCKET, transaction) == FluidConstants.BUCKET,
						facing + ": the inlet takes raw syngas");
				transaction.abort();
			}
			for (int part = 0; part < parts; part++) {
				level.setBlock(form.footprint().partPos(controller, facing, part), Blocks.AIR.defaultBlockState(),
						Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
			}
		}
		helper.succeed();
	}

	/** Methane burns at 2,048 JE a tick, every millibucket burnt being 528 JE made or still in hand. */
	@GameTest(structure = ARENA, maxTicks = 100)
	public void methaneBurnsAt2048JEATick(GameTestHelper helper) {
		burns(helper, PetroFluids.METHANE.fluid(), METHANE_PER_MB, FULL_RATE);
	}

	/** The Coal Gasifier's raw syngas burns whole at 2,048 JE a tick, 256 JE a millibucket. */
	@GameTest(structure = ARENA, maxTicks = 100)
	public void rawSyngasBurnsAt2048JEATick(GameTestHelper helper) {
		burns(helper, PetroFluids.RAW_SYNGAS.fluid(), SYNGAS_PER_MB, FULL_RATE);
	}

	/** Hydrogen burns at 1,024 JE a tick, at the Fuel Cell's 128 JE a millibucket. */
	@GameTest(structure = ARENA, maxTicks = 100)
	public void hydrogenBurnsAt1024JEATick(GameTestHelper helper) {
		burns(helper, PetroFluids.HYDROGEN.fluid(), HYDROGEN_PER_MB, HYDROGEN_RATE);
	}

	/**
	 * The big power spike: on methane it makes eight times the Gas Burning Generator's 256 JE a tick and four times the Gas
	 * Turbine's 512, and gets more from each millibucket than any other generator; on hydrogen it stays below what the
	 * cheapest electrolysis spends, 168 JE a millibucket.
	 */
	@GameTest
	public void itIsTheBigPowerSpike(GameTestHelper helper) {
		MachineForm plant = IndustrialForms.COMBINED_CYCLE_PLANT_FORM;
		FormFuel methane = plant.fuel(PetroFluids.METHANE.fluid());
		FormFuel gasGenerator = IndustrialForms.GAS_BURNING_GENERATOR_FORM.fuel(PetroFluids.METHANE.fluid());
		helper.assertTrue(methane != null && gasGenerator != null && methane.jePerTick() == 8 * gasGenerator.jePerTick()
				&& methane.jePerMb() > gasGenerator.jePerMb(), "Eight times the gas generator's rate, and more per millibucket");
		helper.assertTrue(methane.jePerTick() >= 4 * 512, "At least four times the Gas Turbine's 512 JE/t");
		helper.assertTrue(MachineKind.GAS_TURBINE.maxOutput >= methane.jePerTick(), "Its family can send all of it out");
		FormFuel hydrogen = plant.fuel(PetroFluids.HYDROGEN.fluid());
		helper.assertTrue(hydrogen != null && hydrogen.jePerMb() < 168, "Hydrogen below electrolysis's cost: no power loop");
		helper.succeed();
	}

	/**
	 * The factory's big milestones load as advancements, and only those: the Digester, the vanadium contact bed and this
	 * power station (a challenge); the smaller machines earn none of their own.
	 */
	@GameTest
	public void theBigMilestonesAreAdvancements(GameTestHelper helper) {
		var advancements = helper.getLevel().getServer().getAdvancements();
		for (String id : List.of("anaerobic_digester", "vanadium_contact_bed", "combined_cycle_power_plant")) {
			helper.assertTrue(advancements.get(Jugcraft.id(id)) != null, id + " loads");
		}
		for (String id : List.of("industrial_fermenter", "gas_cleanup_rack", "bio_generator", "chemical_oxidizer")) {
			helper.assertTrue(advancements.get(Jugcraft.id(id)) == null, id + " earns no advancement of its own");
		}
		helper.succeed();
	}

	/** While it runs every part shows lit, but only the controller gives off light. */
	@GameTest(structure = ARENA, maxTicks = 100)
	public void everyPartLightsWhileItRuns(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(8, 1, 8), Direction.NORTH);
		insert(helper, port(helper, machine, "fuel_in"), PetroFluids.METHANE.fluid(), 1000);
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
	 * Its bill: a Gas Turbine and a Heat Recovery Unit (the two halves of a combined cycle), two Steel Tanks, an advanced
	 * circuit, a Machine Casing and three steel plates.
	 */
	@GameTest
	public void itsBill(GameTestHelper helper) {
		RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
		ItemStack tank = stack("steel_tank");
		ItemStack plate = stack("steel_plate");
		CraftingInput bill = CraftingInput.of(3, 3, List.of(tank, stack("advanced_circuit"), tank, stack("gas_turbine"),
				stack("machine_casing"), stack("heat_recovery_unit"), plate, plate, plate));
		Optional<RecipeHolder<CraftingRecipe>> built = crafting.getRecipeFor(bill, helper.getLevel());
		helper.assertTrue(built.isPresent() && built.get().value().assemble(bill).is(IndustrialForms.COMBINED_CYCLE_PLANT.asItem()),
				"Its bill makes the Combined Cycle Power Plant");
		helper.succeed();
	}

	/** Burns {@code fluid}: at {@code perTick} JE a tick, every millibucket burnt {@code perMb} JE made or in hand. */
	private static void burns(GameTestHelper helper, Fluid fluid, int perMb, int perTick) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(8, 1, 8), Direction.NORTH);
		insert(helper, port(helper, machine, "fuel_in"), fluid, 2000);
		long[] earlier = new long[1];
		helper.runAfterDelay(10, () -> {
			int burnt = 2000 - machine.tanks().input(0).millibuckets();
			long made = machine.energy().getAmount();
			helper.assertTrue(burnt > 0 && made + machine.burn() == (long) burnt * perMb,
					"Burnt " + burnt + " mB for " + made + " JE, with " + machine.burn() + " JE in hand");
			helper.assertTrue(machine.status().state() == MachineLifecycle.PROCESSING, "Generating: " + machine.status());
			earlier[0] = made;
		});
		helper.runAfterDelay(20, () -> {
			helper.assertTrue(machine.energy().getAmount() - earlier[0] == 10L * perTick,
					"Not " + perTick + " JE a tick: " + (machine.energy().getAmount() - earlier[0]) + " in ten ticks");
			helper.succeed();
		});
	}

	// ------------------------------------------------------------------ helpers

	private static FormMachineBlockEntity place(GameTestHelper helper, BlockPos relative, Direction facing) {
		helper.setBlock(relative, IndustrialForms.COMBINED_CYCLE_PLANT.defaultBlockState().setValue(MachineBlock.FACING, facing));
		IndustrialForms.COMBINED_CYCLE_PLANT.setPlacedBy(helper.getLevel(), helper.absolutePos(relative), helper.getBlockState(relative), null,
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
