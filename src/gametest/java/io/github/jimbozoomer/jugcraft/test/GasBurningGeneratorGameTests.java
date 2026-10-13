package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.FluidFuels;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.machine.MachineUpgrades;
import io.github.jimbozoomer.jugcraft.machine.form.FormCell;
import io.github.jimbozoomer.jugcraft.machine.form.FormFuel;
import io.github.jimbozoomer.jugcraft.machine.form.FormMachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.form.FormPort;
import io.github.jimbozoomer.jugcraft.machine.form.FormSide;
import io.github.jimbozoomer.jugcraft.machine.form.IndustrialForms;
import io.github.jimbozoomer.jugcraft.machine.form.MachineForm;
import io.github.jimbozoomer.jugcraft.machine.form.MachineLifecycle;
import io.github.jimbozoomer.jugcraft.machine.form.MachineStatus;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;

/**
 * The Gas Burning Generator (docs/features/industrial-gas-burning-generator.md), package 2's second form and the first
 * generator form: it stands whole in every orientation, its open corners left open, with fuel reaching it only at its
 * inlet and power leaving only at its cabinet socket; it burns hydrogen a millibucket a tick into exactly 128 JE; it
 * makes only what its store has room for and keeps the rest in hand, through saving and loading; paused, it burns
 * nothing; its power fills a battery at its socket and no other; every part lights while it works; and it burns only
 * its own fuel, for less than electrolysis spends making it, from the plan's bill of materials.
 */
public class GasBurningGeneratorGameTests {
	private static final String ARENA = "jugcraft-test:arms_arena";
	/** JE a millibucket of hydrogen gives, and JE a tick the generator makes from it. */
	private static final int PER_MB = 128;
	private static final int PER_TICK = 128;
	private static final int PARTS = 50;

	/**
	 * In every orientation it fills its fifty blocks and leaves its open positions free, and each block's faces answer
	 * pipes, cables and conveyors only where the form declares a port: hydrogen goes in at the inlet and nothing else
	 * does, and the socket only gives power.
	 */
	@GameTest(structure = ARENA)
	public void itStandsWholeAndOnlyItsPortsAnswer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MachineForm form = IndustrialForms.GAS_BURNING_GENERATOR_FORM;
		helper.assertTrue(form.generator() && form.positions() == 80 && form.footprint().size() == PARTS
				&& IndustrialForms.GAS_BURNING_GENERATOR.partProperty().getPossibleValues().size() == PARTS,
				"Fifty blocks in a 4x5x4 envelope, numbered 0 to 49");
		Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
		// One copy per orientation, each clear of the others inside the arena.
		BlockPos[] controllers = {new BlockPos(4, 1, 1), new BlockPos(14, 1, 4), new BlockPos(1, 1, 14), new BlockPos(10, 1, 10)};
		for (int i = 0; i < facings.length; i++) {
			Direction facing = facings[i];
			FormMachineBlockEntity machine = place(helper, controllers[i], facing);
			BlockPos controller = machine.getBlockPos();
			for (int part = 0; part < PARTS; part++) {
				BlockPos at = form.footprint().partPos(controller, facing, part);
				BlockState state = level.getBlockState(at);
				helper.assertTrue(state.is(IndustrialForms.GAS_BURNING_GENERATOR) && IndustrialForms.GAS_BURNING_GENERATOR.part(state) == part
						&& state.getValue(MachineBlock.FACING) == facing, facing + ": part " + part + " is " + state);
				for (Direction side : Direction.values()) {
					FormPort port = form.port(part, FormSide.of(side, facing));
					boolean fluid = FluidStorage.SIDED.find(level, at, side) != null;
					boolean items = ItemStorage.SIDED.find(level, at, side) != null;
					EnergyStorage power = EnergyStorage.SIDED.find(level, at, side);
					helper.assertTrue(fluid == (port != null && port.kind() == FormPort.Kind.FLUID_IN) && !items
							&& (power != null) == (port != null && port.kind() == FormPort.Kind.ENERGY_OUT),
							facing + ": part " + part + " " + side + " answers fluid " + fluid + ", items " + items + ", power " + (power != null));
					helper.assertTrue(power == null || power.supportsExtraction() && !power.supportsInsertion(),
							facing + ": the socket only gives power");
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
				helper.assertTrue(inlet.insert(FluidVariant.of(PetroFluids.REFINERY_GAS.fluid()), FluidConstants.BUCKET, transaction) == 0,
						facing + ": not even the gas turbine's refinery gas");
				helper.assertTrue(inlet.insert(FluidVariant.of(PetroFluids.HYDROGEN.fluid()), FluidConstants.BUCKET, transaction) == FluidConstants.BUCKET,
						facing + ": the inlet takes hydrogen");
				transaction.abort();
			}
		}
		helper.succeed();
	}

	/** Hydrogen burns a millibucket a tick, each made into exactly 128 JE with nothing left in hand. */
	@GameTest(structure = ARENA, maxTicks = 100)
	public void hydrogenBurnsAMillibucketATickInto128JE(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(6, 1, 2), Direction.NORTH);
		insert(helper, port(helper, machine, "fuel_in"), PetroFluids.HYDROGEN.fluid(), 1000);
		long[] earlier = new long[2];
		helper.runAfterDelay(20, () -> {
			int burnt = 1000 - machine.tanks().input(0).millibuckets();
			long made = machine.energy().getAmount();
			helper.assertTrue(burnt > 0 && made == (long) burnt * PER_MB && machine.burn() == 0,
					"Burnt " + burnt + " mB for " + made + " JE, with " + machine.burn() + " JE in hand");
			MachineStatus status = machine.status();
			Component title = status.title(machine.form());
			helper.assertTrue(status.state() == MachineLifecycle.PROCESSING && machine.comparatorSignal() == 15
					&& title.getContents() instanceof TranslatableContents words && words.getKey().equals("container.jugcraft.form.state.generating"),
					"Generating at its full rate: " + status);
			earlier[0] = made;
			earlier[1] = burnt;
		});
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(machine.energy().getAmount() - earlier[0] == 10L * PER_TICK
					&& 1000 - machine.tanks().input(0).millibuckets() - earlier[1] == 10, "128 JE and a millibucket a tick");
			helper.succeed();
		});
	}

	/**
	 * It makes only what its store has room for. With room for 64 JE it burns one millibucket, makes 64 JE and keeps the
	 * other 64 in hand, through saving and loading; when 1,000 JE are drawn it makes those, the 64 first, and burns no
	 * more than they need.
	 */
	@GameTest(structure = ARENA, maxTicks = 100)
	public void aFullStoreKeepsTheRestInHand(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FormMachineBlockEntity machine = place(helper, new BlockPos(6, 1, 2), Direction.NORTH);
		SimpleEnergyStorage store = (SimpleEnergyStorage) machine.energy();
		long capacity = store.getCapacity();
		store.setAmount(capacity - 64);
		insert(helper, port(helper, machine, "fuel_in"), PetroFluids.HYDROGEN.fluid(), 1000);
		helper.runAfterDelay(5, () -> {
			MachineStatus status = machine.status();
			helper.assertTrue(store.getAmount() == capacity && machine.tanks().input(0).millibuckets() == 999 && machine.burn() == 64,
					"One millibucket burnt, 64 JE made and 64 in hand, not " + (store.getAmount() - capacity + 64) + " made, "
							+ machine.tanks().input(0).millibuckets() + " mB left, " + machine.burn() + " in hand");
			helper.assertTrue(status.state() == MachineLifecycle.IDLE && status.reason() == MachineStatus.Reason.POWER_FULL
					&& machine.comparatorSignal() == 0, "Idle on a full store: " + status);
			CompoundTag saved = machine.saveWithoutMetadata(level.registryAccess());
			FormMachineBlockEntity copy = new FormMachineBlockEntity(machine.getBlockPos(), machine.getBlockState());
			copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
			helper.assertTrue(copy.burn() == 64 && copy.energy().getAmount() == capacity && copy.tanks().input(0).millibuckets() == 999,
					"The JE in hand are saved and loaded");
			store.setAmount(capacity - 1000);
		});
		helper.runAfterDelay(30, () -> {
			int burnt = 1000 - machine.tanks().input(0).millibuckets();
			helper.assertTrue(store.getAmount() == capacity && 64 + 1000 == (long) burnt * PER_MB - machine.burn() && machine.burn() < PER_MB,
					"Made 1,064 JE from " + burnt + " mB, with " + machine.burn() + " JE in hand");
			helper.succeed();
		});
	}

	/** Paused, it burns nothing and makes nothing; resumed, it carries on. */
	@GameTest(structure = ARENA, maxTicks = 100)
	public void pausedItBurnsNothing(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(6, 1, 2), Direction.NORTH);
		machine.togglePause();
		insert(helper, port(helper, machine, "fuel_in"), PetroFluids.HYDROGEN.fluid(), 1000);
		helper.runAfterDelay(10, () -> {
			helper.assertTrue(machine.tanks().input(0).millibuckets() == 1000 && machine.energy().getAmount() == 0
					&& machine.status().reason() == MachineStatus.Reason.PAUSED, "Paused: " + machine.status());
			machine.togglePause();
		});
		helper.runAfterDelay(20, () -> {
			helper.assertTrue(machine.energy().getAmount() > 0 && machine.tanks().input(0).millibuckets() < 1000, "Resumed: " + machine.status());
			helper.succeed();
		});
	}

	/**
	 * Its power leaves only at the cabinet socket: a battery box against the socket fills, one against the front of
	 * the ignition box stays empty, and every JE made is in the generator or the first battery.
	 */
	@GameTest(structure = ARENA, maxTicks = 100)
	public void powerLeavesOnlyAtTheSocket(GameTestHelper helper) {
		BlockPos relative = new BlockPos(6, 1, 2);
		FormMachineBlockEntity machine = place(helper, relative, Direction.NORTH);
		MachineForm form = machine.form();
		FormPort socket = form.ports().stream().filter(p -> p.kind() == FormPort.Kind.ENERGY_OUT).findFirst().orElseThrow();
		BlockPos socketBlock = form.cellPos(machine.getBlockPos(), Direction.NORTH, form.cellIndex(socket.column(), socket.row(), socket.layer()));
		BlockPos fed = relative.offset(socketBlock.subtract(machine.getBlockPos())).relative(socket.side().world(Direction.NORTH));
		BlockPos unfed = relative.relative(Direction.NORTH);
		Block battery = JugcraftMachines.MACHINES.get(MachineKind.BATTERY_BOX);
		// Each battery's front, where it gives power out, faces away from the generator.
		helper.setBlock(fed, battery.defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
		helper.setBlock(unfed, battery.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
		insert(helper, port(helper, machine, "fuel_in"), PetroFluids.HYDROGEN.fluid(), 1000);
		helper.runAfterDelay(40, () -> {
			ServerLevel level = helper.getLevel();
			EnergyStorage filled = EnergyStorage.SIDED.find(level, helper.absolutePos(fed), Direction.UP);
			EnergyStorage empty = EnergyStorage.SIDED.find(level, helper.absolutePos(unfed), Direction.UP);
			int burnt = 1000 - machine.tanks().input(0).millibuckets();
			helper.assertTrue(filled != null && filled.getAmount() > 0, "The battery at the socket fills");
			helper.assertTrue(empty != null && empty.getAmount() == 0, "The battery at the front gets nothing");
			helper.assertTrue(machine.energy().getAmount() + filled.getAmount() == (long) burnt * PER_MB - machine.burn(),
					"Every JE made from " + burnt + " mB is in the generator or the battery");
			helper.succeed();
		});
	}

	/** While it works every part shows lit, so its lamps and sight glasses light, but only the controller gives off light. */
	@GameTest(structure = ARENA, maxTicks = 100)
	public void everyPartLightsWhileItGenerates(GameTestHelper helper) {
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
	 * It burns only the fuel on its form: hydrogen at the Fuel Cell's 128 JE/mB, made into the owner's 128 JE/t. The
	 * gas turbine, whose energy figures it shares, still never burns hydrogen. Hydrogen burns for less than the
	 * cheapest electrolysis spends on it (a Separator with four efficiency cards, 168 JE/mB), so no loop. And it is
	 * built from the plan's bill: four steel plates, a steel gear, an electric motor, a basic circuit, a Tinplate Tank
	 * and a Machine Casing.
	 */
	@GameTest
	public void itsFuelItsBalanceAndItsBill(GameTestHelper helper) {
		MachineForm form = IndustrialForms.GAS_BURNING_GENERATOR_FORM;
		FormFuel hydrogen = form.fuel(PetroFluids.HYDROGEN.fluid());
		helper.assertTrue(form.fuels().size() == 1 && hydrogen != null && hydrogen.jePerMb() == PER_MB && hydrogen.jePerTick() == PER_TICK
				&& PER_MB == FluidFuels.jePerMb(MachineKind.FUEL_CELL, PetroFluids.HYDROGEN.fluid()), "Hydrogen at 128 JE/mB and 128 JE/t");
		helper.assertTrue(form.fuel(PetroFluids.REFINERY_GAS.fluid()) == null && FluidFuels.jePerMb(MachineKind.GAS_TURBINE, PetroFluids.HYDROGEN.fluid()) == 0,
				"It never burns the turbine's fuels, nor the turbine hydrogen");
		long cheapestDraw = new MachineUpgrades.Effect(0, MachineUpgrades.MAX_EFFECTIVE).use(MachineKind.ELECTROLYTIC_CELL.usePerTick);
		// Water (500 mB in 800 ticks) and brine (250 mB in 400 ticks) both give hydrogen at 1.6 ticks a millibucket.
		helper.assertTrue(cheapestDraw * 800 / 500 > PER_MB, "Electrolysis spends " + cheapestDraw * 800 / 500 + " JE/mB at best: no loop");

		RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
		ItemStack plate = stack("steel_plate");
		CraftingInput generator = CraftingInput.of(3, 3, List.of(plate, stack("steel_gear"), plate, stack("electric_motor"),
				stack("basic_circuit"), stack("fluid_tank"), plate, stack("machine_casing"), plate));
		Optional<RecipeHolder<CraftingRecipe>> built = crafting.getRecipeFor(generator, helper.getLevel());
		helper.assertTrue(built.isPresent() && built.get().value().assemble(generator).is(IndustrialForms.GAS_BURNING_GENERATOR.asItem()),
				"The plan's bill makes the Gas Burning Generator");
		helper.succeed();
	}

	// ------------------------------------------------------------------ helpers

	/** Places the whole generator as its item would, facing {@code facing}, its controller at {@code relative}. */
	private static FormMachineBlockEntity place(GameTestHelper helper, BlockPos relative, Direction facing) {
		helper.setBlock(relative, IndustrialForms.GAS_BURNING_GENERATOR.defaultBlockState().setValue(MachineBlock.FACING, facing));
		IndustrialForms.GAS_BURNING_GENERATOR.setPlacedBy(helper.getLevel(), helper.absolutePos(relative), helper.getBlockState(relative), null,
				ItemStack.EMPTY);
		return helper.getBlockEntity(relative, FormMachineBlockEntity.class);
	}

	/** The pipe-facing storage of the named port, found the way a pipe finds it. */
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
