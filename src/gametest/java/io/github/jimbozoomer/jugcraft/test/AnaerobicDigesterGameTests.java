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
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The Anaerobic Digester (docs/features/industrial-anaerobic-digester.md), the substantial first bulk digester: it
 * stands whole in every orientation with pipes, conveyors and cables reaching it only at its declared ports; sixteen
 * pieces of waste and a bucket of water digest into 1,000 mB of raw biogas and two bone meal over 400 ticks at 80% of
 * the reactor's draw; four batches run side by side; every part lights while it works; it runs only its own recipe;
 * and it is built from its bill.
 */
public class AnaerobicDigesterGameTests {
	private static final String ARENA = "jugcraft-test:arms_arena";
	private static final int PARTS = 216;
	/** One lane of the chemical reactor family's 96 JE/t in the bulk profile (80%, rounded up). */
	private static final long DRAW = 77;
	private static final int TICKS = 400;

	/**
	 * In every orientation it fills its 216 blocks, and each block's faces answer pipes, conveyors and cables only where
	 * the form declares a port. The auger takes rotten flesh, not cobblestone; the inlet takes water, not hydrogen;
	 * nothing goes in at the gas header.
	 */
	@GameTest(structure = ARENA)
	public void itStandsWholeAndOnlyItsPortsAnswer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MachineForm form = IndustrialForms.ANAEROBIC_DIGESTER_FORM;
		helper.assertTrue(form.footprint().size() == PARTS && IndustrialForms.ANAEROBIC_DIGESTER.partProperty().getPossibleValues().size() == PARTS,
				"216 blocks, numbered 0 to 215");
		Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
		// One copy per orientation, each in its own corner of the arena.
		BlockPos[] controllers = {new BlockPos(6, 1, 1), new BlockPos(14, 1, 6), new BlockPos(1, 1, 14), new BlockPos(9, 1, 9)};
		for (int i = 0; i < facings.length; i++) {
			Direction facing = facings[i];
			FormMachineBlockEntity machine = place(helper, controllers[i], facing);
			BlockPos controller = machine.getBlockPos();
			for (int part = 0; part < form.footprint().size(); part++) {
				BlockPos at = form.footprint().partPos(controller, facing, part);
				BlockState state = level.getBlockState(at);
				helper.assertTrue(state.is(IndustrialForms.ANAEROBIC_DIGESTER) && IndustrialForms.ANAEROBIC_DIGESTER.part(state) == part
						&& state.getValue(MachineBlock.FACING) == facing, facing + ": part " + part + " is " + state);
				for (Direction side : Direction.values()) {
					FormPort port = form.port(part, FormSide.of(side, facing));
					boolean fluid = FluidStorage.SIDED.find(level, at, side) != null;
					boolean items = ItemStorage.SIDED.find(level, at, side) != null;
					boolean power = EnergyStorage.SIDED.find(level, at, side) != null;
					helper.assertTrue(fluid == (port != null && port.kind().fluid()) && items == (port != null && port.kind().item())
							&& power == (port != null && port.kind() == FormPort.Kind.ENERGY_IN),
							facing + ": part " + part + " " + side + " answers fluid " + fluid + ", items " + items + ", power " + power);
				}
			}
			Storage<ItemVariant> auger = itemPort(helper, machine, "feed_in");
			Storage<FluidVariant> water = port(helper, machine, "water_in");
			try (Transaction transaction = Transaction.openOuter()) {
				helper.assertTrue(auger.insert(ItemVariant.of(Items.COBBLESTONE), 1, transaction) == 0, facing + ": the auger refuses cobblestone");
				helper.assertTrue(water.insert(FluidVariant.of(PetroFluids.HYDROGEN.fluid()), FluidConstants.BUCKET, transaction) == 0,
						facing + ": the inlet refuses hydrogen");
				helper.assertTrue(port(helper, machine, "biogas_out").insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction) == 0,
						facing + ": nothing goes in at the gas header");
				helper.assertTrue(auger.insert(ItemVariant.of(Items.ROTTEN_FLESH), 16, transaction) == 16
						&& water.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction) == FluidConstants.BUCKET,
						facing + ": the auger takes rotten flesh and the inlet water");
				transaction.abort();
			}
		}
		helper.succeed();
	}

	/**
	 * Sixteen rotten flesh and a bucket of water: 400 paid ticks at 77 JE (80% of 96, rounded up), then 1,000 mB of
	 * biogas leaving at the header flange and two bone meal at the digestate chute.
	 */
	@GameTest(structure = ARENA, maxTicks = 600)
	public void wasteDigestsIntoBiogasAndBoneMeal(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(6, 1, 1), Direction.NORTH);
		long[] supplied = supply(helper, machine);
		try (Transaction transaction = Transaction.openOuter()) {
			itemPort(helper, machine, "feed_in").insert(ItemVariant.of(Items.ROTTEN_FLESH), 16, transaction);
			transaction.commit();
		}
		insert(helper, port(helper, machine, "water_in"), Fluids.WATER, 1000);
		helper.succeedWhen(() -> {
			helper.assertTrue(machine.lane(0) == null && amount(machine.tanks().output(0), PetroFluids.BIOGAS.fluid()) == 1000,
					"1,000 mB of biogas made");
			long used = used(machine, supplied);
			helper.assertTrue(used == TICKS * DRAW, "Paid " + used + " JE, not " + TICKS * DRAW);
			helper.assertTrue(extract(helper, port(helper, machine, "biogas_out"), PetroFluids.BIOGAS.fluid()) == 1000,
					"The biogas leaves at the header flange");
			helper.assertTrue(extractItems(helper, itemPort(helper, machine, "digestate_out"), Items.BONE_MEAL) == 2,
					"Two bone meal leave at the digestate chute");
		});
	}

	/** Sixty-four wheat and four buckets of water: all four lanes digest at once. */
	@GameTest(structure = ARENA, maxTicks = 100)
	public void fourBatchesRunSideBySide(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(6, 1, 1), Direction.NORTH);
		supply(helper, machine);
		machine.container().setItem(0, new ItemStack(Items.WHEAT, 64));
		machine.tanks().input(0).fill(Fluids.WATER, 4000);
		helper.runAfterDelay(10, () -> {
			helper.assertTrue(machine.lane(0) != null && machine.lane(1) != null && machine.lane(2) != null && machine.lane(3) != null,
					"All four lanes digest together");
			helper.succeed();
		});
	}

	/** While it works every part shows lit, but only the controller gives off light; paused, it goes dark. */
	@GameTest(structure = ARENA, maxTicks = 200)
	public void everyPartLightsWhileItWorks(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(6, 1, 1), Direction.NORTH);
		supply(helper, machine);
		machine.container().setItem(0, new ItemStack(Items.ROTTEN_FLESH, 64));
		machine.tanks().input(0).fill(Fluids.WATER, 4000);
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
	 * It runs exactly its one digestion recipe, and the Chemical Reactor never makes biogas. Its construction: four Steel
	 * Tanks, two electric motors, a steel plate, a basic circuit and a Machine Casing.
	 */
	@GameTest(structure = ARENA)
	public void itRunsItsOwnRecipeAndIsBuiltFromItsBill(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FormMachineBlockEntity machine = place(helper, new BlockPos(6, 1, 1), Direction.NORTH);
		List<FluidRecipes.Entry> own = machine.recipes(level.getServer());
		helper.assertTrue(own.size() == 3 && own.get(0).id().equals(Jugcraft.id("chemical_reaction/digester_biogas"))
				&& own.get(0).recipe().time() == TICKS, "Original digestion remains first beside expanded routes: " + own.stream().map(FluidRecipes.Entry::id).toList());
		long reactor = FluidRecipes.recipes(level.getServer(), MachineKind.CHEMICAL_REACTOR).stream()
				.filter(recipe -> recipe.fluidResults().stream().anyMatch(result -> result.fluid() == PetroFluids.BIOGAS.fluid()))
				.count();
		helper.assertTrue(reactor == 0, "The Chemical Reactor makes no biogas");

		RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
		ItemStack tank = stack("steel_tank");
		ItemStack motor = stack("electric_motor");
		CraftingInput digester = CraftingInput.of(3, 3, List.of(tank, stack("steel_plate"), tank, motor, stack("basic_circuit"), motor,
				tank, stack("machine_casing"), tank));
		Optional<RecipeHolder<CraftingRecipe>> built = crafting.getRecipeFor(digester, level);
		helper.assertTrue(built.isPresent() && built.get().value().assemble(digester).is(IndustrialForms.ANAEROBIC_DIGESTER.asItem()),
				"Four Steel Tanks, two motors, a plate, a circuit and a casing make the Anaerobic Digester");
		helper.succeed();
	}

	// ------------------------------------------------------------------ helpers

	private static FormMachineBlockEntity place(GameTestHelper helper, BlockPos relative, Direction facing) {
		helper.setBlock(relative, IndustrialForms.ANAEROBIC_DIGESTER.defaultBlockState().setValue(MachineBlock.FACING, facing));
		IndustrialForms.ANAEROBIC_DIGESTER.setPlacedBy(helper.getLevel(), helper.absolutePos(relative), helper.getBlockState(relative), null,
				ItemStack.EMPTY);
		return helper.getBlockEntity(relative, FormMachineBlockEntity.class);
	}

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

	private static BlockPos portPos(FormMachineBlockEntity machine, FormPort port) {
		MachineForm form = machine.form();
		Direction facing = machine.getBlockState().getValue(MachineBlock.FACING);
		return form.cellPos(machine.getBlockPos(), facing, form.cellIndex(port.column(), port.row(), port.layer()));
	}

	private static FormPort named(FormMachineBlockEntity machine, String name) {
		return machine.form().ports().stream().filter(p -> p.name().equals(name)).findFirst().orElseThrow();
	}

	private static Storage<FluidVariant> port(GameTestHelper helper, FormMachineBlockEntity machine, String name) {
		FormPort port = named(machine, name);
		Direction facing = machine.getBlockState().getValue(MachineBlock.FACING);
		Storage<FluidVariant> storage = FluidStorage.SIDED.find(helper.getLevel(), portPos(machine, port), port.side().world(facing));
		helper.assertTrue(storage != null, "No storage at port " + name);
		return storage;
	}

	private static Storage<ItemVariant> itemPort(GameTestHelper helper, FormMachineBlockEntity machine, String name) {
		FormPort port = named(machine, name);
		Direction facing = machine.getBlockState().getValue(MachineBlock.FACING);
		Storage<ItemVariant> storage = ItemStorage.SIDED.find(helper.getLevel(), portPos(machine, port), port.side().world(facing));
		helper.assertTrue(storage != null, "No item storage at port " + name);
		return storage;
	}

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

	private static int extract(GameTestHelper helper, Storage<FluidVariant> storage, Fluid fluid) {
		try (Transaction transaction = Transaction.openOuter()) {
			long moved = storage.extract(FluidVariant.of(fluid), 16 * FluidConstants.BUCKET, transaction);
			transaction.commit();
			return (int) (moved * 1000 / FluidConstants.BUCKET);
		}
	}

	private static int amount(FluidTank tank, Fluid fluid) {
		return !tank.variant.isBlank() && tank.variant.isOf(fluid) ? tank.millibuckets() : 0;
	}

	private static int extractItems(GameTestHelper helper, Storage<ItemVariant> storage, Item item) {
		try (Transaction transaction = Transaction.openOuter()) {
			long moved = storage.extract(ItemVariant.of(item), 64, transaction);
			transaction.commit();
			return (int) moved;
		}
	}

	private static Item item(String path) {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id(path));
	}

	private static ItemStack stack(String path) {
		return new ItemStack(item(path));
	}
}
