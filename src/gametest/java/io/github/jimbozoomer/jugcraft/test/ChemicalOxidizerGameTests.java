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

/**
 * The Chemical Oxidizer (docs/features/industrial-chemical-oxidizer.md), package 2's solid-feed oxidation form: it
 * stands whole in every orientation with pipes, conveyors and cables reaching it only at its declared ports; a sulfur
 * dust burns with 250 mB of oxygen into 250 mB of sulfur dioxide at the plan's 9,600 JE; it says what it waits for and
 * stops on a full product tank; every part lights while it works; it runs only its own recipe while the Chemical Reactor
 * keeps its own; and it is built from the plan's starter bill.
 */
public class ChemicalOxidizerGameTests {
	private static final String ARENA = "jugcraft-test:arms_arena";
	private static final int PARTS = 12;
	/** The chemical reactor family's working draw. */
	private static final long DRAW = 96;
	private static final int TICKS = 100;

	/**
	 * In every orientation it fills its twelve blocks, and each block's faces answer pipes, conveyors and cables only
	 * where the form declares a port. The hopper takes sulfur dust, not cobblestone; the oxidant inlet takes oxygen, not
	 * hydrogen; nothing goes in at the gas outlet.
	 */
	@GameTest(structure = ARENA)
	public void itStandsWholeAndOnlyItsPortsAnswer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MachineForm form = IndustrialForms.CHEMICAL_OXIDIZER_FORM;
		helper.assertTrue(form.footprint().size() == PARTS && IndustrialForms.CHEMICAL_OXIDIZER.partProperty().getPossibleValues().size() == PARTS,
				"Twelve blocks, numbered 0 to 11");
		Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
		for (int i = 0; i < facings.length; i++) {
			Direction facing = facings[i];
			FormMachineBlockEntity machine = place(helper, new BlockPos(2 + 4 * i, 1, 6), facing);
			BlockPos controller = machine.getBlockPos();
			for (int part = 0; part < form.footprint().size(); part++) {
				BlockPos at = form.footprint().partPos(controller, facing, part);
				BlockState state = level.getBlockState(at);
				helper.assertTrue(state.is(IndustrialForms.CHEMICAL_OXIDIZER) && IndustrialForms.CHEMICAL_OXIDIZER.part(state) == part
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
			Storage<ItemVariant> hopper = itemPort(helper, machine, "feed_in");
			Storage<FluidVariant> oxidant = port(helper, machine, "oxidant_in");
			Storage<FluidVariant> product = port(helper, machine, "product_out");
			try (Transaction transaction = Transaction.openOuter()) {
				helper.assertTrue(hopper.insert(ItemVariant.of(Items.COBBLESTONE), 1, transaction) == 0, facing + ": the hopper refuses cobblestone");
				helper.assertTrue(oxidant.insert(FluidVariant.of(PetroFluids.HYDROGEN.fluid()), FluidConstants.BUCKET, transaction) == 0,
						facing + ": the oxidant inlet refuses hydrogen");
				helper.assertTrue(product.insert(FluidVariant.of(PetroFluids.OXYGEN.fluid()), FluidConstants.BUCKET, transaction) == 0,
						facing + ": nothing goes in at the outlet");
				transaction.abort();
			}
			try (Transaction transaction = Transaction.openOuter()) {
				helper.assertTrue(hopper.insert(ItemVariant.of(item("sulfur_dust")), 4, transaction) == 4
						&& oxidant.insert(FluidVariant.of(PetroFluids.OXYGEN.fluid()), FluidConstants.BUCKET, transaction) == FluidConstants.BUCKET,
						facing + ": the hopper takes sulfur dust and the inlet oxygen");
				transaction.commit();
			}
			helper.assertTrue(machine.container().getItem(0).is(item("sulfur_dust")) && machine.container().getItem(0).getCount() == 4
					&& amount(machine.tanks().input(0), PetroFluids.OXYGEN.fluid()) == 1000, facing + ": both reached the machine");
		}
		helper.succeed();
	}

	/**
	 * One sulfur dust and 250 mB of oxygen: 100 paid ticks at 96 JE, the plan's 9,600 JE, then 250 mB of sulfur dioxide
	 * leaving at the collar on top of the hood.
	 */
	@GameTest(maxTicks = 200)
	public void sulfurBurnsIntoSulfurDioxide(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(2, 1, 2), Direction.NORTH);
		long[] supplied = supply(helper, machine);
		try (Transaction transaction = Transaction.openOuter()) {
			itemPort(helper, machine, "feed_in").insert(ItemVariant.of(item("sulfur_dust")), 1, transaction);
			transaction.commit();
		}
		insert(helper, port(helper, machine, "oxidant_in"), PetroFluids.OXYGEN.fluid(), 250);
		helper.runAfterDelay(3, () -> helper.assertTrue(machine.lane(0) != null && machine.reservedMb(0) == 250,
				"The batch started with room for its gas reserved"));
		helper.succeedWhen(() -> {
			helper.assertTrue(machine.lane(0) == null, "Still burning");
			helper.assertTrue(amount(machine.tanks().output(0), PetroFluids.SULFUR_DIOXIDE.fluid()) == 250
					&& machine.container().getItem(0).isEmpty() && machine.tanks().input(0).millibuckets() == 0,
					"The dust and oxygen used, 250 mB of sulfur dioxide made");
			long used = used(machine, supplied);
			helper.assertTrue(used == TICKS * DRAW, "Paid " + used + " JE, not " + TICKS * DRAW);
			helper.assertTrue(extract(helper, port(helper, machine, "product_out"), PetroFluids.SULFUR_DIOXIDE.fluid()) == 250,
					"The sulfur dioxide leaves at the hood's collar");
		});
	}

	/** Charcoal burns the same way, with 250 mB of oxygen, into 250 mB of carbon dioxide: methanation's carbon feed. */
	@GameTest(maxTicks = 200)
	public void charcoalBurnsIntoCarbonDioxide(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(2, 1, 2), Direction.NORTH);
		long[] supplied = supply(helper, machine);
		try (Transaction transaction = Transaction.openOuter()) {
			helper.assertTrue(itemPort(helper, machine, "feed_in").insert(ItemVariant.of(Items.CHARCOAL), 1, transaction) == 1,
					"The hopper takes charcoal");
			transaction.commit();
		}
		insert(helper, port(helper, machine, "oxidant_in"), PetroFluids.OXYGEN.fluid(), 250);
		helper.succeedWhen(() -> {
			helper.assertTrue(machine.lane(0) == null && amount(machine.tanks().output(0), PetroFluids.CARBON_DIOXIDE.fluid()) == 250,
					"250 mB of carbon dioxide made");
			helper.assertTrue(used(machine, supplied) == TICKS * DRAW, "Paid " + used(machine, supplied) + " JE");
		});
	}

	/**
	 * Sulfur alone waits, naming the oxygen it needs and spending nothing; with the product tank full, it waits on the
	 * full tank; it goes on once the gas is drawn off.
	 */
	@GameTest(maxTicks = 100)
	public void itSaysWhatItIsWaitingFor(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(2, 1, 2), Direction.NORTH);
		long[] supplied = supply(helper, machine);
		machine.container().setItem(0, stack("sulfur_dust"));
		helper.runAfterDelay(10, () -> {
			MachineStatus status = machine.status();
			helper.assertTrue(status.state() == MachineLifecycle.WAITING_INPUT && status.reason() == MachineStatus.Reason.MISSING_FLUID
					&& status.index() == 0 && status.subject() == BuiltInRegistries.FLUID.getId(PetroFluids.OXYGEN.fluid()),
					"Sulfur alone waits for oxygen: " + status);
			helper.assertTrue(machine.lane(0) == null && used(machine, supplied) == 0, "Nothing started, nothing spent");
			machine.tanks().output(0).fill(PetroFluids.SULFUR_DIOXIDE.fluid(), machine.form().profile().bufferMb());
			machine.tanks().input(0).fill(PetroFluids.OXYGEN.fluid(), 250);
		});
		helper.runAfterDelay(20, () -> {
			MachineStatus status = machine.status();
			int tank = machine.form().tanks().inputTanks().size();
			helper.assertTrue(status.state() == MachineLifecycle.OUTPUT_BLOCKED && status.reason() == MachineStatus.Reason.OUTPUT_FULL
					&& status.index() == tank, "It waits on the full product tank: " + status);
			helper.assertTrue(machine.lane(0) == null && used(machine, supplied) == 0, "Still nothing taken or spent");
			machine.tanks().output(0).drain(machine.form().profile().bufferMb());
		});
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(machine.lane(0) != null && machine.status().state().working(),
					"It goes on once the gas is drawn off: " + machine.status());
			helper.succeed();
		});
	}

	/** While it works every part shows lit, but only the controller gives off light; paused, it goes dark. */
	@GameTest(maxTicks = 200)
	public void everyPartLightsWhileItWorks(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(2, 1, 2), Direction.NORTH);
		supply(helper, machine);
		ItemStack sulfur = stack("sulfur_dust");
		sulfur.setCount(4);
		machine.container().setItem(0, sulfur);
		machine.tanks().input(0).fill(PetroFluids.OXYGEN.fluid(), 1000);
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
	 * The Oxidizer runs exactly its one capability recipe and the Chemical Reactor never makes sulfur dioxide. Its
	 * construction: 4 steel plates, a furnace, an electric motor, glass, a basic circuit and a Machine Casing.
	 */
	@GameTest
	public void itRunsItsOwnRecipeAndIsBuiltFromTheStarterBill(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FormMachineBlockEntity machine = place(helper, new BlockPos(2, 1, 2), Direction.NORTH);
		List<FluidRecipes.Entry> own = machine.recipes(level.getServer());
		helper.assertTrue(own.size() == 2 && own.get(0).id().equals(Jugcraft.id("chemical_reaction/oxidizer_carbon_dioxide"))
				&& own.get(1).id().equals(Jugcraft.id("chemical_reaction/oxidizer_sulfur_dioxide"))
				&& own.stream().allMatch(entry -> entry.recipe().time() == TICKS), "Its two recipes: " + own.stream().map(FluidRecipes.Entry::id).toList());
		long reactor = FluidRecipes.recipes(level.getServer(), MachineKind.CHEMICAL_REACTOR).stream()
				.filter(recipe -> recipe.fluidResults().stream().anyMatch(result -> result.fluid() == PetroFluids.SULFUR_DIOXIDE.fluid()))
				.count();
		helper.assertTrue(reactor == 0, "The Chemical Reactor does not make sulfur dioxide");

		RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
		ItemStack plate = stack("steel_plate");
		CraftingInput oxidizer = CraftingInput.of(3, 3, List.of(plate, new ItemStack(Items.FURNACE), plate, stack("electric_motor"),
				stack("basic_circuit"), new ItemStack(Items.GLASS), plate, stack("machine_casing"), plate));
		Optional<RecipeHolder<CraftingRecipe>> built = crafting.getRecipeFor(oxidizer, level);
		helper.assertTrue(built.isPresent() && built.get().value().assemble(oxidizer).is(IndustrialForms.CHEMICAL_OXIDIZER.asItem()),
				"Four plates, a furnace, a motor, a basic circuit, glass and a casing make the Oxidizer");
		helper.succeed();
	}

	// ------------------------------------------------------------------ helpers

	private static FormMachineBlockEntity place(GameTestHelper helper, BlockPos relative, Direction facing) {
		helper.setBlock(relative, IndustrialForms.CHEMICAL_OXIDIZER.defaultBlockState().setValue(MachineBlock.FACING, facing));
		IndustrialForms.CHEMICAL_OXIDIZER.setPlacedBy(helper.getLevel(), helper.absolutePos(relative), helper.getBlockState(relative), null,
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
