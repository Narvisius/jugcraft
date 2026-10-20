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
 * The Bulk Fermenter (docs/features/industrial-bulk-fermenter.md), the Industrial Fermenter's 5x5x6 variant: it stands
 * whole in every orientation with pipes, conveyors and cables reaching it only at its declared ports; it ferments as the
 * smaller one does, 375 mB of bioethanol and 250 mB of carbon dioxide from eight crops and a bucket of water, at the bulk
 * profile's 77 JE/t; four batches run at once; every part lights while it works; it runs the Fermenter's recipe; and it is
 * built from its bill.
 */
public class BulkFermenterGameTests {
	private static final String ARENA = "jugcraft-test:arms_arena";
	private static final int PARTS = 150;
	/** The chemical reactor family's 96 JE/t at the bulk profile's 80%, rounded up. */
	private static final long DRAW = 77;
	private static final int TICKS = 100;

	/**
	 * In every orientation it fills its 150 blocks, and each block's faces answer pipes, conveyors and cables only
	 * where the form declares a port. The hatch takes wheat, not cobblestone; the water inlet takes water, not hydrogen;
	 * nothing goes in at either outlet.
	 */
	@GameTest(structure = ARENA)
	public void itStandsWholeAndOnlyItsPortsAnswer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MachineForm form = IndustrialForms.BULK_FERMENTER_FORM;
		helper.assertTrue(form.footprint().size() == PARTS && IndustrialForms.BULK_FERMENTER.partProperty().getPossibleValues().size() == PARTS,
				"150 blocks, numbered 0 to 149");
		Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
		// One copy per orientation, each clear of the others inside the arena.
		BlockPos[] controllers = {new BlockPos(6, 1, 1), new BlockPos(14, 1, 6), new BlockPos(1, 1, 14), new BlockPos(9, 1, 9)};
		for (int i = 0; i < facings.length; i++) {
			Direction facing = facings[i];
			FormMachineBlockEntity machine = place(helper, controllers[i], facing);
			BlockPos controller = machine.getBlockPos();
			for (int part = 0; part < form.footprint().size(); part++) {
				BlockPos at = form.footprint().partPos(controller, facing, part);
				BlockState state = level.getBlockState(at);
				helper.assertTrue(state.is(IndustrialForms.BULK_FERMENTER) && IndustrialForms.BULK_FERMENTER.part(state) == part
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
			Storage<ItemVariant> hatch = itemPort(helper, machine, "feed_in");
			Storage<FluidVariant> water = port(helper, machine, "water_in");
			try (Transaction transaction = Transaction.openOuter()) {
				helper.assertTrue(hatch.insert(ItemVariant.of(Items.COBBLESTONE), 1, transaction) == 0, facing + ": the hatch refuses cobblestone");
				helper.assertTrue(water.insert(FluidVariant.of(PetroFluids.HYDROGEN.fluid()), FluidConstants.BUCKET, transaction) == 0,
						facing + ": the water inlet refuses hydrogen");
				for (String outlet : List.of("product_out", "co2_out")) {
					helper.assertTrue(port(helper, machine, outlet).insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction) == 0,
							facing + ": nothing goes in at " + outlet);
				}
				transaction.abort();
			}
			try (Transaction transaction = Transaction.openOuter()) {
				helper.assertTrue(hatch.insert(ItemVariant.of(Items.WHEAT), 8, transaction) == 8
						&& water.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction) == FluidConstants.BUCKET,
						facing + ": the hatch takes wheat and the inlet water");
				transaction.commit();
			}
			helper.assertTrue(machine.container().getItem(0).is(Items.WHEAT) && machine.container().getItem(0).getCount() == 8
					&& amount(machine.tanks().input(0), Fluids.WATER) == 1000, facing + ": both reached the machine");
		}
		helper.succeed();
	}

	/**
	 * Eight wheat and a bucket of water: 100 paid ticks at the bulk profile's 77 JE, 7,700 JE, then 375 mB of
	 * bioethanol, as the Industrial Fermenter makes, leaving at the broth outlet and 250 mB of carbon dioxide at the upper flange.
	 */
	@GameTest(structure = ARENA, maxTicks = 200)
	public void wheatFermentsIntoBioethanolAndCarbonDioxide(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(6, 1, 1), Direction.NORTH);
		long[] supplied = supply(helper, machine);
		try (Transaction transaction = Transaction.openOuter()) {
			itemPort(helper, machine, "feed_in").insert(ItemVariant.of(Items.WHEAT), 8, transaction);
			transaction.commit();
		}
		insert(helper, port(helper, machine, "water_in"), Fluids.WATER, 1000);
		helper.succeedWhen(() -> {
			helper.assertTrue(machine.lane(0) == null, "Still fermenting");
			helper.assertTrue(amount(machine.tanks().output(0), PetroFluids.BIOETHANOL.source()) == 375
					&& amount(machine.tanks().output(1), PetroFluids.CARBON_DIOXIDE.fluid()) == 250
					&& machine.container().getItem(0).isEmpty() && machine.tanks().input(0).millibuckets() == 0,
					"The wheat and water used, 375 mB of bioethanol and 250 mB of carbon dioxide made");
			long used = used(machine, supplied);
			helper.assertTrue(used == TICKS * DRAW, "Paid " + used + " JE, not " + TICKS * DRAW);
			helper.assertTrue(extract(helper, port(helper, machine, "product_out"), PetroFluids.BIOETHANOL.source()) == 375,
					"The bioethanol leaves at the broth outlet");
			helper.assertTrue(extract(helper, port(helper, machine, "co2_out"), PetroFluids.CARBON_DIOXIDE.fluid()) == 250,
					"The carbon dioxide leaves at the upper flange");
		});
	}

	/** Thirty-two wheat and four buckets of water start all four lanes together. */
	@GameTest(structure = ARENA, maxTicks = 100)
	public void fourBatchesRunSideBySide(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(6, 1, 1), Direction.NORTH);
		supply(helper, machine);
		machine.container().setItem(0, new ItemStack(Items.WHEAT, 32));
		machine.tanks().input(0).fill(Fluids.WATER, 4000);
		helper.runAfterDelay(10, () -> {
			helper.assertTrue(machine.lane(0) != null && machine.lane(1) != null && machine.lane(2) != null && machine.lane(3) != null,
					"All four lanes ferment together");
			helper.succeed();
		});
	}

	/** While it works every part shows lit, but only the controller gives off light; paused, it goes dark. */
	@GameTest(structure = ARENA, maxTicks = 200)
	public void everyPartLightsWhileItWorks(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(6, 1, 1), Direction.NORTH);
		supply(helper, machine);
		machine.container().setItem(0, new ItemStack(Items.WHEAT, 32));
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
	 * It runs the Industrial Fermenter's two recipes, vents its carbon dioxide like the smaller one, and is built from an
	 * Industrial Fermenter, two Steel Tanks, steel plates, an electric motor and a basic circuit.
	 */
	@GameTest(structure = ARENA)
	public void itRunsTheFermentersRecipeAndIsBuiltFromItsBill(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FormMachineBlockEntity machine = place(helper, new BlockPos(6, 1, 1), Direction.NORTH);
		List<FluidRecipes.Entry> own = machine.recipes(level.getServer());
		helper.assertTrue(own.size() == 2 && own.get(0).id().equals(Jugcraft.id("chemical_reaction/fermenter_bioethanol"))
				&& own.get(1).id().equals(Jugcraft.id("chemical_reaction/ferment_crop_mash")),
				"The Fermenter's two recipes, bioethanol first: " + own.stream().map(FluidRecipes.Entry::id).toList());
		helper.assertTrue(machine.form().vents(1) && machine.form().profile().lanes() == 4, "Four lanes, and its gas vents");
		RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
		ItemStack tank = stack("steel_tank");
		ItemStack plate = stack("steel_plate");
		CraftingInput bill = CraftingInput.of(3, 3, List.of(tank, new ItemStack(IndustrialForms.INDUSTRIAL_FERMENTER), tank, plate,
				stack("electric_motor"), plate, plate, stack("basic_circuit"), plate));
		Optional<RecipeHolder<CraftingRecipe>> built = crafting.getRecipeFor(bill, level);
		helper.assertTrue(built.isPresent() && built.get().value().assemble(bill).is(IndustrialForms.BULK_FERMENTER.asItem()),
				"Its bill makes the Bulk Fermenter");
		helper.succeed();
	}

	// ------------------------------------------------------------------ helpers

	private static FormMachineBlockEntity place(GameTestHelper helper, BlockPos relative, Direction facing) {
		helper.setBlock(relative, IndustrialForms.BULK_FERMENTER.defaultBlockState().setValue(MachineBlock.FACING, facing));
		IndustrialForms.BULK_FERMENTER.setPlacedBy(helper.getLevel(), helper.absolutePos(relative), helper.getBlockState(relative), null,
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
