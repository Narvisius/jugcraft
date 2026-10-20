package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipes;
import io.github.jimbozoomer.jugcraft.chemistry.FluidTank;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.form.FormCell;
import io.github.jimbozoomer.jugcraft.machine.form.FormMachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.form.FormPort;
import io.github.jimbozoomer.jugcraft.machine.form.FormSide;
import io.github.jimbozoomer.jugcraft.machine.form.IndustrialForms;
import io.github.jimbozoomer.jugcraft.machine.form.MachineForm;
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
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The Bulk Chemical Infuser (docs/features/industrial-bulk-chemical-infuser.md), the Chemical Infuser's 4x6x6 variant:
 * it stands whole in every orientation with pipes and cables reaching it only at its five ports; it runs the Infuser's
 * recipes four batches at a time at 80% of the draw a batch; one catalyst bed in its socket serves every batch; every
 * part lights while it works; and it is built from a Chemical Infuser and its bill.
 */
public class BulkChemicalInfuserGameTests {
	private static final String ARENA = "jugcraft-test:arms_arena";
	private static final int PARTS = 110;
	/** The synthesis family's 128 JE/t at the bulk profile's 80%, rounded up (OperatingProfile.lanePerTick). */
	private static final long DRAW = 103;
	private static final int HCL_TICKS = 75;
	private static final int METHANATION_TICKS = 200;
	private static final int PRODUCT = 0;
	private static final int WATER = 1;

	/**
	 * In every orientation it fills its 110 blocks and leaves its open positions free, and each block's faces answer
	 * pipes and cables only where the form declares a port. The left inlet takes hydrogen, not water; nothing goes in at
	 * either outlet.
	 */
	@GameTest(structure = ARENA)
	public void itStandsWholeAndOnlyItsPortsAnswer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MachineForm form = IndustrialForms.BULK_CHEMICAL_INFUSER_FORM;
		helper.assertTrue(form.positions() == 144 && form.footprint().size() == PARTS
				&& IndustrialForms.BULK_CHEMICAL_INFUSER.partProperty().getPossibleValues().size() == PARTS, "110 blocks in a 4x6x6 envelope");
		Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
		// One copy per orientation, each clear of the others inside the arena.
		BlockPos[] controllers = {new BlockPos(5, 1, 1), new BlockPos(14, 1, 4), new BlockPos(1, 1, 14), new BlockPos(8, 1, 9)};
		for (int i = 0; i < facings.length; i++) {
			Direction facing = facings[i];
			FormMachineBlockEntity machine = place(helper, controllers[i], facing);
			BlockPos controller = machine.getBlockPos();
			for (int part = 0; part < form.footprint().size(); part++) {
				BlockPos at = form.footprint().partPos(controller, facing, part);
				BlockState state = level.getBlockState(at);
				helper.assertTrue(state.is(IndustrialForms.BULK_CHEMICAL_INFUSER) && IndustrialForms.BULK_CHEMICAL_INFUSER.part(state) == part
						&& state.getValue(MachineBlock.FACING) == facing, facing + ": part " + part + " is " + state);
				for (Direction side : Direction.values()) {
					FormPort port = form.port(part, FormSide.of(side, facing));
					boolean fluid = FluidStorage.SIDED.find(level, at, side) != null;
					boolean power = EnergyStorage.SIDED.find(level, at, side) != null;
					helper.assertTrue(fluid == (port != null && port.kind().fluid()) && power == (port != null && port.kind() == FormPort.Kind.ENERGY_IN),
							facing + ": part " + part + " " + side + " answers fluid " + fluid + ", power " + power);
				}
			}
			for (int cell = 0; cell < form.positions(); cell++) {
				if (form.cell(cell) == FormCell.ACCESS) {
					helper.assertTrue(level.getBlockState(form.cellPos(controller, facing, cell)).isAir(), facing + ": open position " + cell + " stays open");
				}
			}
			Storage<FluidVariant> left = port(helper, machine, "hydrogen_in");
			try (Transaction transaction = Transaction.openOuter()) {
				helper.assertTrue(left.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction) == 0, facing + ": the left inlet refuses water");
				helper.assertTrue(left.insert(FluidVariant.of(PetroFluids.HYDROGEN.fluid()), FluidConstants.BUCKET, transaction) == FluidConstants.BUCKET,
						facing + ": the left inlet takes hydrogen");
				for (String outlet : List.of("product_out", "water_out")) {
					helper.assertTrue(port(helper, machine, outlet).insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction) == 0,
							facing + ": nothing goes in at " + outlet);
				}
				transaction.abort();
			}
		}
		helper.succeed();
	}

	/**
	 * A bucket each of hydrogen and chlorine runs four hydrogen chloride batches side by side, each 75 paid ticks at the
	 * bulk profile's 103 JE: 2,000 mB of hydrogen chloride for 30,900 JE, where four Infuser batches cost 38,400.
	 */
	@GameTest(structure = ARENA, maxTicks = 300)
	public void hydrogenChlorideRunsFourBatchesAtOnce(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(5, 1, 1), Direction.NORTH);
		long[] supplied = supply(helper, machine);
		insert(helper, port(helper, machine, "hydrogen_in"), PetroFluids.HYDROGEN.fluid(), 1000);
		insert(helper, port(helper, machine, "reactant_in"), PetroFluids.CHLORINE.fluid(), 1000);
		helper.runAfterDelay(5, () -> helper.assertTrue(machine.lane(0) != null && machine.lane(1) != null && machine.lane(2) != null
				&& machine.lane(3) != null, "All four batches started together"));
		helper.succeedWhen(() -> {
			helper.assertTrue(machine.lane(0) == null && machine.lane(1) == null && machine.lane(2) == null && machine.lane(3) == null, "Still reacting");
			helper.assertTrue(amount(machine.tanks().output(PRODUCT), PetroFluids.HYDROGEN_CHLORIDE.fluid()) == 2000
					&& machine.tanks().input(0).millibuckets() == 0 && machine.tanks().input(1).millibuckets() == 0,
					"The gases used up into 2,000 mB of hydrogen chloride");
			long used = used(machine, supplied);
			helper.assertTrue(used == 4 * HCL_TICKS * DRAW, "Paid " + used + " JE, not " + 4 * HCL_TICKS * DRAW);
			helper.assertTrue(extract(helper, port(helper, machine, "product_out"), PetroFluids.HYDROGEN_CHLORIDE.fluid()) == 2000,
					"The hydrogen chloride leaves at the product collar");
		});
	}

	/**
	 * One nickel bed in its socket serves four methanation batches at once: 1,600 mB of hydrogen and 400 mB of carbon
	 * dioxide give 400 mB of methane and 1,600 mB of water, and the bed is still there.
	 */
	@GameTest(structure = ARENA, maxTicks = 400)
	public void oneBedServesEveryBatch(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(5, 1, 1), Direction.NORTH);
		long[] supplied = supply(helper, machine);
		int socket = machine.form().firstSocketSlot();
		machine.container().setItem(socket, stack("nickel_catalyst_bed"));
		insert(helper, port(helper, machine, "hydrogen_in"), PetroFluids.HYDROGEN.fluid(), 1600);
		insert(helper, port(helper, machine, "reactant_in"), PetroFluids.CARBON_DIOXIDE.fluid(), 400);
		helper.runAfterDelay(5, () -> helper.assertTrue(machine.lane(0) != null && machine.lane(3) != null && machine.socketLocked(0),
				"Four batches started on the one bed"));
		helper.succeedWhen(() -> {
			helper.assertTrue(machine.lane(0) == null && machine.lane(3) == null, "Still reacting");
			helper.assertTrue(amount(machine.tanks().output(PRODUCT), PetroFluids.METHANE.fluid()) == 400
					&& amount(machine.tanks().output(WATER), Fluids.WATER) == 1600, "400 mB of methane and 1,600 mB of water");
			long used = used(machine, supplied);
			helper.assertTrue(used == 4 * METHANATION_TICKS * DRAW, "Paid " + used + " JE, not " + 4 * METHANATION_TICKS * DRAW);
			helper.assertTrue(machine.container().getItem(socket).is(item("nickel_catalyst_bed")), "The bed is still there");
			helper.assertTrue(extract(helper, port(helper, machine, "water_out"), Fluids.WATER) == 1600, "The water leaves at the drain");
		});
	}

	/** While it works every part shows lit, but only the controller gives off light; paused, it goes dark. */
	@GameTest(structure = ARENA, maxTicks = 200)
	public void everyPartLightsWhileItWorks(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(5, 1, 1), Direction.NORTH);
		supply(helper, machine);
		machine.tanks().input(0).fill(PetroFluids.HYDROGEN.fluid(), 4000);
		machine.tanks().input(1).fill(PetroFluids.CHLORINE.fluid(), 4000);
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
	 * It runs exactly the Chemical Infuser's recipes, in four lanes, and is built from a Chemical Infuser, two Steel Tanks,
	 * steel plates, steel fluid pipe and a basic circuit.
	 */
	@GameTest(structure = ARENA)
	public void itRunsTheInfusersRecipesAndIsBuiltFromItsBill(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FormMachineBlockEntity machine = place(helper, new BlockPos(5, 1, 1), Direction.NORTH);
		helper.setBlock(new BlockPos(12, 1, 10), IndustrialForms.CHEMICAL_INFUSER.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
		IndustrialForms.CHEMICAL_INFUSER.setPlacedBy(level, helper.absolutePos(new BlockPos(12, 1, 10)), helper.getBlockState(new BlockPos(12, 1, 10)),
				null, ItemStack.EMPTY);
		FormMachineBlockEntity small = helper.getBlockEntity(new BlockPos(12, 1, 10), FormMachineBlockEntity.class);
		List<?> own = machine.recipes(level.getServer()).stream().map(FluidRecipes.Entry::id).sorted().toList();
		List<?> infusers = small.recipes(level.getServer()).stream().map(FluidRecipes.Entry::id).sorted().toList();
		helper.assertTrue(!own.isEmpty() && own.equals(infusers), "Its recipes " + own + " are the Infuser's " + infusers);
		helper.assertTrue(machine.form().profile().lanes() == 4 && machine.form().profile().energyPercent() == 80, "Four lanes at 80%");
		RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
		ItemStack tank = stack("steel_tank");
		ItemStack plate = stack("steel_plate");
		CraftingInput bill = CraftingInput.of(3, 3, List.of(tank, new ItemStack(IndustrialForms.CHEMICAL_INFUSER), tank, plate,
				stack("steel_fluid_pipe"), plate, plate, stack("basic_circuit"), plate));
		Optional<RecipeHolder<CraftingRecipe>> built = crafting.getRecipeFor(bill, level);
		helper.assertTrue(built.isPresent() && built.get().value().assemble(bill).is(IndustrialForms.BULK_CHEMICAL_INFUSER.asItem()),
				"Its bill makes the Bulk Chemical Infuser");
		helper.succeed();
	}

	// ------------------------------------------------------------------ helpers

	private static FormMachineBlockEntity place(GameTestHelper helper, BlockPos relative, Direction facing) {
		helper.setBlock(relative, IndustrialForms.BULK_CHEMICAL_INFUSER.defaultBlockState().setValue(MachineBlock.FACING, facing));
		IndustrialForms.BULK_CHEMICAL_INFUSER.setPlacedBy(helper.getLevel(), helper.absolutePos(relative), helper.getBlockState(relative), null,
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

	private static Item item(String path) {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id(path));
	}

	private static ItemStack stack(String path) {
		return new ItemStack(item(path));
	}
}
