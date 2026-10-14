package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipes;
import io.github.jimbozoomer.jugcraft.chemistry.FluidTank;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.machine.form.FormMachineBlock;
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
 * The coal gasification line (docs/features/industrial-coal-gasification.md): the Coal Gasifier and the Gas Cleanup and
 * Separation Rack each stand whole in every orientation with pipes, conveyors and cables reaching them only at their
 * declared ports; coal or coke and steam become 500 mB of raw syngas, charcoal 400 mB; the rack divides 500 mB of it
 * into 250 mB of carbon monoxide and 250 mB of hydrogen; every part lights while they work; each runs only its own
 * recipes; and each is built from the plan's bill.
 */
public class CoalGasificationGameTests {
	private static final String ARENA = "jugcraft-test:arms_arena";
	/** One lane of the chemical reactor family's 96 JE/t in the expanded profile (90%, rounded up). */
	private static final long GASIFIER_DRAW = 87;
	private static final int GASIFIER_TICKS = 200;
	/** The synthesis family's 128 JE/t. */
	private static final long RACK_DRAW = 128;
	private static final int RACK_TICKS = 100;

	/**
	 * The Gasifier fills its 96 blocks in every orientation and answers only at its ports: the chute takes coal, not
	 * cobblestone; the inlet takes water, not hydrogen; nothing goes in at the header.
	 */
	@GameTest(structure = ARENA)
	public void theGasifierStandsWholeAndOnlyItsPortsAnswer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MachineForm form = IndustrialForms.COAL_GASIFIER_FORM;
		helper.assertTrue(form.footprint().size() == 96 && IndustrialForms.COAL_GASIFIER.partProperty().getPossibleValues().size() == 96,
				"Ninety-six blocks");
		Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
		// One copy per orientation, each clear of the others inside the arena.
		BlockPos[] controllers = {new BlockPos(4, 1, 1), new BlockPos(14, 1, 4), new BlockPos(1, 1, 14), new BlockPos(10, 1, 10)};
		for (int i = 0; i < facings.length; i++) {
			Direction facing = facings[i];
			FormMachineBlockEntity machine = place(helper, IndustrialForms.COAL_GASIFIER, controllers[i], facing);
			BlockPos controller = machine.getBlockPos();
			for (int part = 0; part < form.footprint().size(); part++) {
				BlockPos at = form.footprint().partPos(controller, facing, part);
				BlockState state = level.getBlockState(at);
				helper.assertTrue(state.is(IndustrialForms.COAL_GASIFIER) && IndustrialForms.COAL_GASIFIER.part(state) == part
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
			Storage<ItemVariant> chute = itemPort(helper, machine, "feed_in");
			Storage<FluidVariant> water = port(helper, machine, "water_in");
			try (Transaction transaction = Transaction.openOuter()) {
				helper.assertTrue(chute.insert(ItemVariant.of(Items.COBBLESTONE), 1, transaction) == 0, facing + ": the chute refuses cobblestone");
				helper.assertTrue(water.insert(FluidVariant.of(PetroFluids.HYDROGEN.fluid()), FluidConstants.BUCKET, transaction) == 0,
						facing + ": the inlet refuses hydrogen");
				helper.assertTrue(port(helper, machine, "syngas_out").insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction) == 0,
						facing + ": nothing goes in at the header");
				helper.assertTrue(itemPort(helper, machine, "residue_out").insert(ItemVariant.of(Items.COAL), 1, transaction) == 0,
						facing + ": nothing goes in at the residue chute");
				helper.assertTrue(chute.insert(ItemVariant.of(Items.COAL), 4, transaction) == 4
						&& water.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction) == FluidConstants.BUCKET,
						facing + ": the chute takes coal and the inlet water");
				transaction.abort();
			}
		}
		helper.succeed();
	}

	/**
	 * A coal and 500 mB of water: 200 paid ticks at 87 JE, then 500 mB of raw syngas leaving at the header flange and one
	 * gasification residue at the chute.
	 */
	@GameTest(structure = ARENA, maxTicks = 400)
	public void coalGasifiesIntoRawSyngas(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, IndustrialForms.COAL_GASIFIER, new BlockPos(6, 1, 2), Direction.NORTH);
		long[] supplied = supply(helper, machine);
		try (Transaction transaction = Transaction.openOuter()) {
			itemPort(helper, machine, "feed_in").insert(ItemVariant.of(Items.COAL), 1, transaction);
			transaction.commit();
		}
		insert(helper, port(helper, machine, "water_in"), Fluids.WATER, 500);
		helper.succeedWhen(() -> {
			helper.assertTrue(machine.lane(0) == null && amount(machine.tanks().output(0), PetroFluids.RAW_SYNGAS.fluid()) == 500,
					"500 mB of raw syngas made");
			long used = used(machine, supplied);
			helper.assertTrue(used == GASIFIER_TICKS * GASIFIER_DRAW, "Paid " + used + " JE, not " + GASIFIER_TICKS * GASIFIER_DRAW);
			helper.assertTrue(extract(helper, port(helper, machine, "syngas_out"), PetroFluids.RAW_SYNGAS.fluid()) == 500,
					"The raw syngas leaves at the header flange");
			helper.assertTrue(extractItems(helper, itemPort(helper, machine, "residue_out"), item("gasification_residue")) == 1,
					"One gasification residue leaves at the chute");
		});
	}

	/** Charcoal, the lower-yield alternative: 400 mB of raw syngas from 400 mB of water, and no residue. */
	@GameTest(structure = ARENA, maxTicks = 400)
	public void charcoalGivesLessSyngas(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, IndustrialForms.COAL_GASIFIER, new BlockPos(6, 1, 2), Direction.NORTH);
		supply(helper, machine);
		machine.container().setItem(0, new ItemStack(Items.CHARCOAL));
		machine.tanks().input(0).fill(Fluids.WATER, 400);
		helper.succeedWhen(() -> helper.assertTrue(machine.lane(0) == null
				&& amount(machine.tanks().output(0), PetroFluids.RAW_SYNGAS.fluid()) == 400
				&& machine.container().getItem(machine.form().firstOutputSlot()).isEmpty(), "400 mB of raw syngas and no residue from charcoal"));
	}

	/** While the Gasifier works every part shows lit, but only the controller gives off light; paused, it goes dark. */
	@GameTest(structure = ARENA, maxTicks = 200)
	public void everyGasifierPartLightsWhileItWorks(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, IndustrialForms.COAL_GASIFIER, new BlockPos(6, 1, 2), Direction.NORTH);
		supply(helper, machine);
		machine.container().setItem(0, new ItemStack(Items.COAL, 16));
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
	 * The Gasifier runs its three recipes (charcoal, coal, coke) and is built from the plan's bill: 4 steel plates, 2
	 * bricks, a furnace, a steel fluid pipe and a Machine Casing.
	 */
	@GameTest(structure = ARENA)
	public void theGasifierRunsItsOwnRecipesAndIsBuiltFromItsBill(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FormMachineBlockEntity machine = place(helper, IndustrialForms.COAL_GASIFIER, new BlockPos(6, 1, 2), Direction.NORTH);
		List<FluidRecipes.Entry> own = machine.recipes(level.getServer());
		helper.assertTrue(own.size() == 6 && own.get(0).id().equals(Jugcraft.id("chemical_reaction/gasifier_charcoal"))
				&& own.get(1).id().equals(Jugcraft.id("chemical_reaction/gasifier_coal"))
				&& own.get(2).id().equals(Jugcraft.id("chemical_reaction/gasifier_coke")),
				"Three original plus three expanded recipes: " + own.stream().map(FluidRecipes.Entry::id).toList());
		RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
		ItemStack plate = stack("steel_plate");
		ItemStack brick = new ItemStack(Items.BRICK);
		CraftingInput gasifier = CraftingInput.of(3, 3, List.of(plate, brick, plate, new ItemStack(Items.FURNACE), stack("machine_casing"),
				stack("steel_fluid_pipe"), plate, brick, plate));
		Optional<RecipeHolder<CraftingRecipe>> built = crafting.getRecipeFor(gasifier, level);
		helper.assertTrue(built.isPresent() && built.get().value().assemble(gasifier).is(IndustrialForms.COAL_GASIFIER.asItem()),
				"Four plates, two bricks, a furnace, a casing and a pipe make the Coal Gasifier");
		// Its residue, bound in place of gravel: with cement and water into concrete, with asphalt binder into asphalt.
		ItemStack residue = stack("gasification_residue");
		ItemStack cement = stack("cement");
		CraftingInput concrete = CraftingInput.of(3, 3, List.of(cement, residue, cement, residue, new ItemStack(Items.WATER_BUCKET), residue,
				cement, residue, cement));
		Optional<RecipeHolder<CraftingRecipe>> bound = crafting.getRecipeFor(concrete, level);
		ItemStack made = bound.map(holder -> holder.value().assemble(concrete)).orElse(ItemStack.EMPTY);
		helper.assertTrue(made.is(item("concrete")) && made.getCount() == 8, "Residue, cement and water make 8 concrete, not " + made);
		CraftingInput road = CraftingInput.of(3, 3, List.of(residue, residue, residue, residue, stack("asphalt_binder"), residue,
				residue, residue, residue));
		ItemStack paved = crafting.getRecipeFor(road, level).map(holder -> holder.value().assemble(road)).orElse(ItemStack.EMPTY);
		helper.assertTrue(paved.is(item("asphalt")) && paved.getCount() == 8, "Residue and asphalt binder make 8 asphalt, not " + paved);
		helper.succeed();
	}

	/**
	 * The rack fills its 60 blocks in every orientation and answers only at its ports: the inlet takes raw syngas, not
	 * water; nothing goes in at either tower's outlet.
	 */
	@GameTest(structure = ARENA)
	public void theRackStandsWholeAndOnlyItsPortsAnswer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MachineForm form = IndustrialForms.GAS_CLEANUP_RACK_FORM;
		helper.assertTrue(form.footprint().size() == 60 && IndustrialForms.GAS_CLEANUP_RACK.partProperty().getPossibleValues().size() == 60,
				"Sixty blocks");
		Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
		// One copy per orientation, each clear of the others inside the arena.
		BlockPos[] controllers = {new BlockPos(4, 1, 1), new BlockPos(14, 1, 4), new BlockPos(1, 1, 14), new BlockPos(10, 1, 10)};
		for (int i = 0; i < facings.length; i++) {
			Direction facing = facings[i];
			FormMachineBlockEntity machine = place(helper, IndustrialForms.GAS_CLEANUP_RACK, controllers[i], facing);
			BlockPos controller = machine.getBlockPos();
			for (int part = 0; part < form.footprint().size(); part++) {
				BlockPos at = form.footprint().partPos(controller, facing, part);
				BlockState state = level.getBlockState(at);
				helper.assertTrue(state.is(IndustrialForms.GAS_CLEANUP_RACK) && IndustrialForms.GAS_CLEANUP_RACK.part(state) == part
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
			Storage<FluidVariant> feed = port(helper, machine, "feed_in");
			try (Transaction transaction = Transaction.openOuter()) {
				helper.assertTrue(feed.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction) == 0, facing + ": the inlet refuses water");
				helper.assertTrue(feed.insert(FluidVariant.of(PetroFluids.RAW_SYNGAS.fluid()), FluidConstants.BUCKET, transaction) == FluidConstants.BUCKET,
						facing + ": the inlet takes raw syngas");
				for (String outlet : List.of("product_out", "hydrogen_out")) {
					helper.assertTrue(port(helper, machine, outlet).insert(FluidVariant.of(PetroFluids.RAW_SYNGAS.fluid()), FluidConstants.BUCKET, transaction) == 0,
							facing + ": nothing goes in at " + outlet);
				}
				transaction.abort();
			}
		}
		helper.succeed();
	}

	/** 500 mB of raw syngas: 100 paid ticks at 128 JE, then 250 mB of carbon monoxide and 250 mB of hydrogen at their towers. */
	@GameTest(structure = ARENA, maxTicks = 300)
	public void theRackDividesSyngasIntoCarbonMonoxideAndHydrogen(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, IndustrialForms.GAS_CLEANUP_RACK, new BlockPos(6, 1, 2), Direction.NORTH);
		long[] supplied = supply(helper, machine);
		insert(helper, port(helper, machine, "feed_in"), PetroFluids.RAW_SYNGAS.fluid(), 500);
		helper.succeedWhen(() -> {
			helper.assertTrue(machine.lane(0) == null && amount(machine.tanks().output(0), PetroFluids.CARBON_MONOXIDE.fluid()) == 250
					&& amount(machine.tanks().output(1), PetroFluids.HYDROGEN.fluid()) == 250, "250 mB each of carbon monoxide and hydrogen");
			long used = used(machine, supplied);
			helper.assertTrue(used == RACK_TICKS * RACK_DRAW, "Paid " + used + " JE, not " + RACK_TICKS * RACK_DRAW);
			helper.assertTrue(extract(helper, port(helper, machine, "product_out"), PetroFluids.CARBON_MONOXIDE.fluid()) == 250
					&& extract(helper, port(helper, machine, "hydrogen_out"), PetroFluids.HYDROGEN.fluid()) == 250,
					"Each gas leaves at the top of its tower");
		});
	}

	/**
	 * The rack runs its one recipe and is built from the plan's bill: 4 steel plates, a Steel Tank, 2 Tinplate Tanks, an
	 * electric motor and a basic circuit.
	 */
	@GameTest(structure = ARENA)
	public void theRackRunsItsOwnRecipeAndIsBuiltFromItsBill(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FormMachineBlockEntity machine = place(helper, IndustrialForms.GAS_CLEANUP_RACK, new BlockPos(6, 1, 2), Direction.NORTH);
		List<FluidRecipes.Entry> own = machine.recipes(level.getServer());
		helper.assertTrue(own.size() == 1 && own.get(0).id().equals(Jugcraft.id("gas_synthesis/rack_raw_syngas")),
				"Its one recipe: " + own.stream().map(FluidRecipes.Entry::id).toList());
		RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
		ItemStack plate = stack("steel_plate");
		ItemStack tin = stack("fluid_tank");
		CraftingInput rack = CraftingInput.of(3, 3, List.of(plate, stack("steel_tank"), plate, tin, stack("electric_motor"), tin,
				plate, stack("basic_circuit"), plate));
		Optional<RecipeHolder<CraftingRecipe>> built = crafting.getRecipeFor(rack, level);
		helper.assertTrue(built.isPresent() && built.get().value().assemble(rack).is(IndustrialForms.GAS_CLEANUP_RACK.asItem()),
				"Four plates, a Steel Tank, two Tinplate Tanks, a motor and a circuit make the rack");
		long reactor = FluidRecipes.recipes(level.getServer(), MachineKind.CHEMICAL_REACTOR).stream()
				.filter(recipe -> recipe.fluidResults().stream().anyMatch(result -> result.fluid() == PetroFluids.RAW_SYNGAS.fluid()))
				.count();
		helper.assertTrue(reactor == 0, "The Chemical Reactor makes no raw syngas");
		helper.succeed();
	}

	// ------------------------------------------------------------------ helpers

	private static FormMachineBlockEntity place(GameTestHelper helper, FormMachineBlock block, BlockPos relative, Direction facing) {
		helper.setBlock(relative, block.defaultBlockState().setValue(MachineBlock.FACING, facing));
		block.setPlacedBy(helper.getLevel(), helper.absolutePos(relative), helper.getBlockState(relative), null, ItemStack.EMPTY);
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
