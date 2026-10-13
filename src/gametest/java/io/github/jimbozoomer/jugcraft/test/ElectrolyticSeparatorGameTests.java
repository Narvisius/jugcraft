package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipes;
import io.github.jimbozoomer.jugcraft.chemistry.FluidTank;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
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
 * The Electrolytic Separator (docs/features/industrial-electrolytic-separator.md), package 2's first real form: it
 * stands whole in every orientation with pipes and cables reaching it only at its declared ports; water and ordinary
 * brine split at the owner's selected starter baseline, to the exact energy; a full lye tank stops brine and leaves
 * water running; the oxygen/chlorine tank holds one gas at a time; every part lights while it works; it runs only its
 * own recipes while the Electrolytic Cell keeps its own; and it is built from 20 steel plates in all.
 */
public class ElectrolyticSeparatorGameTests {
	private static final String ARENA = "jugcraft-test:arms_arena";
	/** The Electrolytic Cell family's working draw. */
	private static final long DRAW = 256;
	private static final int WATER_TICKS = 800;
	private static final int BRINE_TICKS = 400;
	/** Output tanks, in the form's order. */
	private static final int ANODE_GAS = 0;
	private static final int HYDROGEN = 1;
	private static final int LYE = 2;

	/**
	 * In every orientation it fills its twelve blocks, and each block's faces answer pipes, cables and conveyors only
	 * where the form declares a port: water and brine go in at the back, lava does not; nothing goes in at an outlet.
	 */
	@GameTest(structure = ARENA)
	public void itStandsWholeAndOnlyItsPortsAnswer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MachineForm form = IndustrialForms.ELECTROLYTIC_SEPARATOR_FORM;
		helper.assertTrue(form.footprint().size() == 12 && IndustrialForms.ELECTROLYTIC_SEPARATOR.partProperty().getPossibleValues().size() == 12,
				"Twelve blocks, numbered 0 to 11");
		Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
		for (int i = 0; i < facings.length; i++) {
			Direction facing = facings[i];
			FormMachineBlockEntity machine = place(helper, new BlockPos(2 + 4 * i, 1, 6), facing);
			BlockPos controller = machine.getBlockPos();
			for (int part = 0; part < form.footprint().size(); part++) {
				BlockPos at = form.footprint().partPos(controller, facing, part);
				BlockState state = level.getBlockState(at);
				helper.assertTrue(state.is(IndustrialForms.ELECTROLYTIC_SEPARATOR) && IndustrialForms.ELECTROLYTIC_SEPARATOR.part(state) == part
						&& state.getValue(MachineBlock.FACING) == facing, facing + ": part " + part + " is " + state);
				for (Direction side : Direction.values()) {
					FormPort port = form.port(part, FormSide.of(side, facing));
					boolean fluid = FluidStorage.SIDED.find(level, at, side) != null;
					boolean items = ItemStorage.SIDED.find(level, at, side) != null;
					boolean power = EnergyStorage.SIDED.find(level, at, side) != null;
					helper.assertTrue(fluid == (port != null && port.kind().fluid()) && !items
							&& power == (port != null && port.kind() == FormPort.Kind.ENERGY_IN),
							facing + ": part " + part + " " + side + " answers fluid " + fluid + ", items " + items + ", power " + power);
				}
			}
			Storage<FluidVariant> feed = port(helper, machine, "feed_in");
			Storage<FluidVariant> hydrogen = port(helper, machine, "hydrogen_out");
			try (Transaction transaction = Transaction.openOuter()) {
				helper.assertTrue(feed.insert(FluidVariant.of(Fluids.LAVA), FluidConstants.BUCKET, transaction) == 0,
						facing + ": the feed refuses lava");
				helper.assertTrue(hydrogen.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction) == 0,
						facing + ": nothing goes in at an outlet");
				helper.assertTrue(feed.insert(FluidVariant.of(PetroFluids.BRINE.source()), FluidConstants.BUCKET, transaction) == FluidConstants.BUCKET,
						facing + ": the feed takes brine");
				transaction.abort();
			}
			try (Transaction transaction = Transaction.openOuter()) {
				helper.assertTrue(feed.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction) == FluidConstants.BUCKET,
						facing + ": the feed takes water");
				transaction.commit();
			}
			helper.assertTrue(amount(machine.tanks().input(0), Fluids.WATER) == 1000, facing + ": the bucket reached the feed tank");
		}
		helper.succeed();
	}

	/**
	 * A bucket of water through the back inlet: 800 paid ticks at 256 JE, 204,800 JE in all, then 500 mB of hydrogen
	 * at the left collar and 250 mB of oxygen at the right, and nothing in the lye tank.
	 */
	@GameTest(maxTicks = 900)
	public void waterSplitsIntoHydrogenAndOxygen(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(2, 1, 2), Direction.NORTH);
		long[] supplied = supply(helper, machine);
		insert(helper, port(helper, machine, "feed_in"), Fluids.WATER, 1000);
		helper.runAfterDelay(3, () -> helper.assertTrue(machine.lane(0) != null && machine.reservedMb(HYDROGEN) == 500
				&& machine.reservedMb(ANODE_GAS) == 250, "The batch started with room for both gases reserved"));
		helper.succeedWhen(() -> {
			helper.assertTrue(machine.lane(0) == null, "Still splitting");
			helper.assertTrue(amount(machine.tanks().output(HYDROGEN), PetroFluids.HYDROGEN.fluid()) == 500
					&& amount(machine.tanks().output(ANODE_GAS), PetroFluids.OXYGEN.fluid()) == 250
					&& machine.tanks().output(LYE).millibuckets() == 0, "500 hydrogen, 250 oxygen and no lye");
			long used = used(machine, supplied);
			helper.assertTrue(used == WATER_TICKS * DRAW, "Paid " + used + " JE, not " + WATER_TICKS * DRAW);
			helper.assertTrue(extract(helper, port(helper, machine, "hydrogen_out"), PetroFluids.HYDROGEN.fluid()) == 500
					&& extract(helper, port(helper, machine, "anode_gas_out"), PetroFluids.OXYGEN.fluid()) == 250,
					"Each gas leaves at its own collar");
		});
	}

	/**
	 * A bucket of brine: 400 paid ticks at 256 JE, 102,400 JE, then 250 mB of chlorine, 250 mB of hydrogen and 500 mB of
	 * lye, the lye leaving at the return on the right.
	 */
	@GameTest(maxTicks = 500)
	public void brineGivesChlorineHydrogenAndLye(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(2, 1, 2), Direction.NORTH);
		long[] supplied = supply(helper, machine);
		insert(helper, port(helper, machine, "feed_in"), PetroFluids.BRINE.source(), 1000);
		helper.succeedWhen(() -> {
			helper.assertTrue(machine.lane(0) == null, "Still splitting");
			helper.assertTrue(amount(machine.tanks().output(ANODE_GAS), PetroFluids.CHLORINE.fluid()) == 250
					&& amount(machine.tanks().output(HYDROGEN), PetroFluids.HYDROGEN.fluid()) == 250
					&& amount(machine.tanks().output(LYE), PetroFluids.LYE.source()) == 500, "250 chlorine, 250 hydrogen and 500 lye");
			long used = used(machine, supplied);
			helper.assertTrue(used == BRINE_TICKS * DRAW, "Paid " + used + " JE, not " + BRINE_TICKS * DRAW);
			helper.assertTrue(extract(helper, port(helper, machine, "lye_out"), PetroFluids.LYE.source()) == 500, "The lye leaves at its return");
		});
	}

	/**
	 * Lye is kept, not vented: with the lye tank full, brine waits and names the full tank, spending nothing, while water
	 * (which makes no lye) still runs. Emptying the lye tank lets brine go on.
	 */
	@GameTest(maxTicks = 200)
	public void aFullLyeTankStopsBrineButNotWater(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(2, 1, 2), Direction.NORTH);
		long[] supplied = supply(helper, machine);
		int tank = machine.form().tanks().inputTanks().size() + LYE;
		machine.tanks().output(LYE).fill(PetroFluids.LYE.source(), machine.form().profile().bufferMb());
		machine.tanks().input(0).fill(PetroFluids.BRINE.source(), 1000);
		helper.runAfterDelay(10, () -> {
			MachineStatus status = machine.status();
			helper.assertTrue(status.state() == MachineLifecycle.OUTPUT_BLOCKED && status.reason() == MachineStatus.Reason.OUTPUT_FULL
					&& status.index() == tank, "Brine waits on the full lye tank: " + status);
			helper.assertTrue(machine.lane(0) == null && amount(machine.tanks().input(0), PetroFluids.BRINE.source()) == 1000
					&& used(machine, supplied) == 0, "Nothing taken, nothing spent");
			machine.tanks().input(0).drain(1000);
			machine.tanks().input(0).fill(Fluids.WATER, 1000);
		});
		helper.runAfterDelay(20, () -> {
			helper.assertTrue(machine.lane(0) != null && machine.status().state().working(),
					"Water runs beside the full lye tank: " + machine.status());
			machine.cancel();
			machine.tanks().input(0).drain(1000);
			machine.tanks().output(LYE).drain(machine.form().profile().bufferMb());
			machine.tanks().input(0).fill(PetroFluids.BRINE.source(), 1000);
		});
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(machine.lane(0) != null && machine.status().state().working(), "Brine goes on once the lye is gone: " + machine.status());
			helper.succeed();
		});
	}

	/**
	 * Oxygen and chlorine share one tank, one gas at a time: oxygen left from water makes brine wait, naming oxygen, until
	 * the oxygen is drawn off.
	 */
	@GameTest(maxTicks = 200)
	public void theOxygenChlorineTankHoldsOneGasAtATime(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(2, 1, 2), Direction.NORTH);
		supply(helper, machine);
		machine.tanks().output(ANODE_GAS).fill(PetroFluids.OXYGEN.fluid(), 250);
		machine.tanks().input(0).fill(PetroFluids.BRINE.source(), 1000);
		helper.runAfterDelay(10, () -> {
			MachineStatus status = machine.status();
			helper.assertTrue(status.state() == MachineLifecycle.OUTPUT_BLOCKED && status.reason() == MachineStatus.Reason.OUTPUT_OTHER_FLUID
					&& status.subject() == BuiltInRegistries.FLUID.getId(PetroFluids.OXYGEN.fluid()), "Brine waits on the oxygen: " + status);
			helper.assertTrue(extract(helper, port(helper, machine, "anode_gas_out"), PetroFluids.OXYGEN.fluid()) == 250, "The oxygen is drawn off");
		});
		helper.runAfterDelay(20, () -> {
			helper.assertTrue(machine.lane(0) != null, "Brine runs once the tank is clear: " + machine.status());
			helper.succeed();
		});
	}

	/**
	 * While it works every part shows lit, so the lamp and the level strips light on the whole machine, but only the
	 * controller gives off light; paused, it goes dark once {@link FormMachineBlockEntity#LIT_HOLD} ticks have passed.
	 */
	@GameTest(maxTicks = 200)
	public void everyPartLightsWhileItWorks(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(2, 1, 2), Direction.NORTH);
		supply(helper, machine);
		insert(helper, port(helper, machine, "feed_in"), Fluids.WATER, 1000);
		helper.runAfterDelay(10, () -> {
			assertLit(helper, machine, true);
			machine.togglePause();
		});
		helper.runAfterDelay(15 + FormMachineBlockEntity.LIT_HOLD, () -> {
			assertLit(helper, machine, false);
			helper.succeed();
		});
	}

	/**
	 * The Separator runs exactly its two capability recipes, at the selected baseline; the Electrolytic Cell keeps its
	 * own, now as slow on brine (400 ticks), and never sees the Separator's. And its construction: 4 steel plates and
	 * two Steel Tanks of 8 plates each make 20, with copper cable and a basic circuit.
	 */
	@GameTest
	public void itRunsItsOwnRecipesAndCostsTwentyPlates(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FormMachineBlockEntity machine = place(helper, new BlockPos(2, 1, 2), Direction.NORTH);
		List<FluidRecipes.Entry> own = machine.recipes(level.getServer());
		helper.assertTrue(own.size() == 2 && own.get(0).id().equals(Jugcraft.id("electrolysis/separator_brine"))
				&& own.get(0).recipe().time() == BRINE_TICKS && own.get(1).id().equals(Jugcraft.id("electrolysis/separator_water"))
				&& own.get(1).recipe().time() == WATER_TICKS, "Its two recipes: " + own.stream().map(FluidRecipes.Entry::id).toList());
		List<Integer> cell = FluidRecipes.recipes(level.getServer(), MachineKind.ELECTROLYTIC_CELL).stream()
				.filter(recipe -> !recipe.fluids().isEmpty()).map(recipe -> recipe.time()).sorted().toList();
		helper.assertTrue(cell.equals(List.of(BRINE_TICKS, WATER_TICKS)), "The cell keeps its brine (400 ticks) and water (800 ticks) recipes: " + cell);

		RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
		ItemStack plate = stack("steel_plate");
		CraftingInput separator = CraftingInput.of(3, 3, List.of(plate, stack("copper_cable"), plate, stack("steel_tank"), stack("basic_circuit"),
				stack("steel_tank"), plate, stack("machine_casing"), plate));
		Optional<RecipeHolder<CraftingRecipe>> built = crafting.getRecipeFor(separator, level);
		helper.assertTrue(built.isPresent() && built.get().value().assemble(separator).is(IndustrialForms.ELECTROLYTIC_SEPARATOR.asItem()),
				"Four plates, two Steel Tanks, copper cable, a basic circuit and a casing make the Separator");
		CraftingInput steelTank = CraftingInput.of(3, 3, List.of(plate, plate, plate, plate, stack("fluid_tank"), plate, plate, plate, plate));
		Optional<RecipeHolder<CraftingRecipe>> tank = crafting.getRecipeFor(steelTank, level);
		helper.assertTrue(tank.isPresent() && tank.get().value().assemble(steelTank).is(item("steel_tank")),
				"A Steel Tank is eight plates around a tank: 4 + 2 x 8 = 20 plates in all");
		helper.succeed();
	}

	// ------------------------------------------------------------------ helpers

	/** Places the whole Separator as its item would, facing {@code facing}, its controller at {@code relative}. */
	private static FormMachineBlockEntity place(GameTestHelper helper, BlockPos relative, Direction facing) {
		helper.setBlock(relative, IndustrialForms.ELECTROLYTIC_SEPARATOR.defaultBlockState().setValue(MachineBlock.FACING, facing));
		IndustrialForms.ELECTROLYTIC_SEPARATOR.setPlacedBy(helper.getLevel(), helper.absolutePos(relative), helper.getBlockState(relative), null,
				ItemStack.EMPTY);
		return helper.getBlockEntity(relative, FormMachineBlockEntity.class);
	}

	/** Every part lit (or dark), and light given off by the controller alone. */
	private static void assertLit(GameTestHelper helper, FormMachineBlockEntity machine, boolean lit) {
		MachineForm form = machine.form();
		Direction facing = machine.getBlockState().getValue(MachineBlock.FACING);
		for (int part = 0; part < form.footprint().size(); part++) {
			BlockState state = helper.getLevel().getBlockState(form.footprint().partPos(machine.getBlockPos(), facing, part));
			int light = lit && part == 0 ? 13 : 0;
			helper.assertTrue(state.getValue(MachineBlock.LIT) == lit && state.getLightEmission() == light,
					"Part " + part + ": lit " + state.getValue(MachineBlock.LIT) + ", light " + state.getLightEmission() + ", not " + lit + ", " + light);
		}
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

	/** A steady supply: tops the store up every tick and counts what the machine took ({@link #used}). */
	private static long[] supply(GameTestHelper helper, FormMachineBlockEntity machine) {
		SimpleEnergyStorage store = (SimpleEnergyStorage) machine.energy();
		long[] supplied = {0};
		store.setAmount(store.getCapacity());
		helper.onEachTick(() -> {
			supplied[0] += store.getCapacity() - store.getAmount();
			store.setAmount(store.getCapacity());
		});
		return supplied;
	}

	private static long used(FormMachineBlockEntity machine, long[] supplied) {
		return supplied[0] + machine.energy().getCapacity() - machine.energy().getAmount();
	}

	private static void insert(GameTestHelper helper, Storage<FluidVariant> storage, Fluid fluid, int mb) {
		try (Transaction transaction = Transaction.openOuter()) {
			long moved = storage.insert(FluidVariant.of(fluid), mb * FluidConstants.BUCKET / 1000, transaction);
			helper.assertTrue(moved == mb * FluidConstants.BUCKET / 1000, "Only " + moved + " droplets of " + mb + " mB went in");
			transaction.commit();
		}
	}

	/** Millibuckets of {@code fluid} drawn out of a port (all of it there is). */
	private static int extract(GameTestHelper helper, Storage<FluidVariant> storage, Fluid fluid) {
		try (Transaction transaction = Transaction.openOuter()) {
			long moved = storage.extract(FluidVariant.of(fluid), 16 * FluidConstants.BUCKET, transaction);
			transaction.commit();
			return (int) (moved * 1000 / FluidConstants.BUCKET);
		}
	}

	/** Millibuckets of {@code fluid} in a tank (0 when it holds anything else). */
	private static int amount(FluidTank tank, Fluid fluid) {
		return !tank.variant.isBlank() && tank.variant.isOf(fluid) ? tank.millibuckets() : 0;
	}

	private static Item item(String path) {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id(path));
	}

	private static ItemStack stack(String path) {
		return new ItemStack(item(path));
	}
}
