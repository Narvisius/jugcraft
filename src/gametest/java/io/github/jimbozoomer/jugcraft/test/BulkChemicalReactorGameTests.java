package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.FertilizerItem;
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
import net.minecraft.resources.Identifier;
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
 * The Bulk Chemical Reactor (docs/features/industrial-bulk-chemical-reactor.md), the Chemical Reactor enlarged: it stands
 * whole in every orientation with pipes, conveyors and cables reaching it only at its declared ports; 500 mB of hydrogen
 * chloride dissolves in a bucket of water into a bucket of hydrochloric acid for 100 ticks at 90% of the reactor's draw;
 * two batches run side by side; an ore leaches in 250 mB of the acid into four washed ores; every part lights while it
 * works; it runs only its own recipes; and it is built from its bill.
 */
public class BulkChemicalReactorGameTests {
	private static final String ARENA = "jugcraft-test:arms_arena";
	private static final int PARTS = 64;
	/** One lane of the chemical reactor family's 96 JE/t in the expanded profile (90%, rounded up). */
	private static final long DRAW = 87;
	private static final int TICKS = 100;

	/**
	 * In every orientation it fills its sixty-four blocks, and each block's faces answer pipes, conveyors and cables only
	 * where the form declares a port. The hatch takes iron ore, not cobblestone; the side inlet takes water, not
	 * hydrogen; the lid inlet takes hydrogen chloride, not water; nothing goes in at the acid outlet.
	 */
	@GameTest(structure = ARENA)
	public void itStandsWholeAndOnlyItsPortsAnswer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MachineForm form = IndustrialForms.BULK_CHEMICAL_REACTOR_FORM;
		helper.assertTrue(form.footprint().size() == PARTS && IndustrialForms.BULK_CHEMICAL_REACTOR.partProperty().getPossibleValues().size() == PARTS,
				"Sixty-four blocks, numbered 0 to 63");
		Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
		// One copy per orientation, each clear of the others inside the arena.
		BlockPos[] controllers = {new BlockPos(4, 1, 1), new BlockPos(14, 1, 4), new BlockPos(1, 1, 14), new BlockPos(10, 1, 10)};
		for (int i = 0; i < facings.length; i++) {
			Direction facing = facings[i];
			FormMachineBlockEntity machine = place(helper, controllers[i], facing);
			BlockPos controller = machine.getBlockPos();
			for (int part = 0; part < form.footprint().size(); part++) {
				BlockPos at = form.footprint().partPos(controller, facing, part);
				BlockState state = level.getBlockState(at);
				helper.assertTrue(state.is(IndustrialForms.BULK_CHEMICAL_REACTOR) && IndustrialForms.BULK_CHEMICAL_REACTOR.part(state) == part
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
			Storage<FluidVariant> liquid = port(helper, machine, "liquid_in");
			Storage<FluidVariant> gas = port(helper, machine, "gas_in");
			try (Transaction transaction = Transaction.openOuter()) {
				helper.assertTrue(hatch.insert(ItemVariant.of(Items.COBBLESTONE), 1, transaction) == 0, facing + ": the hatch refuses cobblestone");
				helper.assertTrue(liquid.insert(FluidVariant.of(PetroFluids.HYDROGEN.fluid()), FluidConstants.BUCKET, transaction) == 0,
						facing + ": the side inlet refuses hydrogen");
				helper.assertTrue(gas.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction) == 0,
						facing + ": the lid inlet refuses water");
				helper.assertTrue(port(helper, machine, "product_out").insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction) == 0,
						facing + ": nothing goes in at the outlet");
				transaction.abort();
			}
			try (Transaction transaction = Transaction.openOuter()) {
				helper.assertTrue(hatch.insert(ItemVariant.of(Items.IRON_ORE), 4, transaction) == 4
						&& liquid.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction) == FluidConstants.BUCKET
						&& gas.insert(FluidVariant.of(PetroFluids.HYDROGEN_CHLORIDE.fluid()), FluidConstants.BUCKET / 2, transaction) == FluidConstants.BUCKET / 2,
						facing + ": the hatch takes iron ore, the side inlet water and the lid inlet hydrogen chloride");
				transaction.abort();
			}
		}
		helper.succeed();
	}

	/**
	 * A bucket of water and 500 mB of hydrogen chloride: 100 paid ticks at 87 JE (90% of 96, rounded up), then a bucket of
	 * hydrochloric acid leaving at the outlet low at the front.
	 */
	@GameTest(structure = ARENA, maxTicks = 200)
	public void hydrogenChlorideDissolvesIntoHydrochloricAcid(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(6, 1, 2), Direction.NORTH);
		long[] supplied = supply(helper, machine);
		insert(helper, port(helper, machine, "liquid_in"), Fluids.WATER, 1000);
		insert(helper, port(helper, machine, "gas_in"), PetroFluids.HYDROGEN_CHLORIDE.fluid(), 500);
		helper.succeedWhen(() -> {
			helper.assertTrue(machine.lane(0) == null && amount(machine.tanks().output(0), PetroFluids.HYDROCHLORIC_ACID.source()) == 1000,
					"A bucket of hydrochloric acid made");
			helper.assertTrue(machine.tanks().input(0).millibuckets() == 0 && machine.tanks().input(1).millibuckets() == 0,
					"The water and the gas used up");
			long used = used(machine, supplied);
			helper.assertTrue(used == TICKS * DRAW, "Paid " + used + " JE, not " + TICKS * DRAW);
			helper.assertTrue(extract(helper, port(helper, machine, "product_out"), PetroFluids.HYDROCHLORIC_ACID.source()) == 1000,
					"The acid leaves at the outlet low at the front");
		});
	}

	/** Two buckets of water and a bucket of the gas: both lanes run at once, and two buckets of acid come out together. */
	@GameTest(structure = ARENA, maxTicks = 200)
	public void twoBatchesRunSideBySide(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(6, 1, 2), Direction.NORTH);
		long[] supplied = supply(helper, machine);
		machine.tanks().input(0).fill(Fluids.WATER, 2000);
		machine.tanks().input(1).fill(PetroFluids.HYDROGEN_CHLORIDE.fluid(), 1000);
		helper.runAfterDelay(5, () -> helper.assertTrue(machine.lane(0) != null && machine.lane(1) != null,
				"Both lanes started together"));
		helper.succeedWhen(() -> {
			helper.assertTrue(machine.lane(0) == null && machine.lane(1) == null
					&& amount(machine.tanks().output(0), PetroFluids.HYDROCHLORIC_ACID.source()) == 2000, "Two buckets of acid made");
			helper.assertTrue(used(machine, supplied) == 2 * TICKS * DRAW, "Paid " + used(machine, supplied) + " JE for two batches");
		});
	}

	/** An iron ore and 250 mB of hydrochloric acid: four washed iron ores out of the chute at the front, as sulfuric acid gives. */
	@GameTest(structure = ARENA, maxTicks = 300)
	public void hydrochloricAcidLeachesOre(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(6, 1, 2), Direction.NORTH);
		supply(helper, machine);
		try (Transaction transaction = Transaction.openOuter()) {
			itemPort(helper, machine, "feed_in").insert(ItemVariant.of(Items.IRON_ORE), 1, transaction);
			transaction.commit();
		}
		machine.tanks().input(0).fill(PetroFluids.HYDROCHLORIC_ACID.source(), 250);
		helper.succeedWhen(() -> {
			helper.assertTrue(machine.lane(0) == null && machine.tanks().input(0).millibuckets() == 0, "The acid used");
			helper.assertTrue(extractItems(helper, itemPort(helper, machine, "items_out"), item("washed_iron_ore")) == 4,
					"Four washed iron ores leave at the chute");
		});
	}

	/**
	 * The contact process's absorption: 250 mB of sulfur trioxide in 500 mB of water make 500 mB of sulfuric acid, 100
	 * ticks at 87 JE.
	 */
	@GameTest(structure = ARENA, maxTicks = 300)
	public void sulfurTrioxideDissolvesIntoSulfuricAcid(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(6, 1, 2), Direction.NORTH);
		long[] supplied = supply(helper, machine);
		insert(helper, port(helper, machine, "liquid_in"), Fluids.WATER, 500);
		insert(helper, port(helper, machine, "gas_in"), PetroFluids.SULFUR_TRIOXIDE.fluid(), 250);
		helper.succeedWhen(() -> {
			helper.assertTrue(machine.lane(0) == null && amount(machine.tanks().output(0), PetroFluids.SULFURIC_ACID.source()) == 500,
					"500 mB of sulfuric acid made");
			helper.assertTrue(used(machine, supplied) == TICKS * DRAW, "Paid " + used(machine, supplied) + " JE");
		});
	}

	/**
	 * The contact bed's vanadium: four washed iron ores roasted with 500 mB of lye give a vanadium pentoxide at the chute,
	 * and that oxide on two bricks crafts the vanadium contact bed.
	 */
	@GameTest(structure = ARENA, maxTicks = 400)
	public void washedIronOreGivesTheContactBedsVanadium(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(6, 1, 2), Direction.NORTH);
		supply(helper, machine);
		try (Transaction transaction = Transaction.openOuter()) {
			itemPort(helper, machine, "feed_in").insert(ItemVariant.of(item("washed_iron_ore")), 4, transaction);
			transaction.commit();
		}
		machine.tanks().input(0).fill(PetroFluids.LYE.source(), 500);
		helper.succeedWhen(() -> {
			helper.assertTrue(machine.lane(0) == null && machine.tanks().input(0).millibuckets() == 0, "The lye used");
			helper.assertTrue(extractItems(helper, itemPort(helper, machine, "items_out"), item("vanadium_pentoxide")) == 1,
					"A vanadium pentoxide leaves at the chute");
			RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
			ItemStack brick = new ItemStack(Items.BRICK);
			CraftingInput bed = CraftingInput.of(3, 1, List.of(brick, stack("vanadium_pentoxide"), brick));
			Optional<RecipeHolder<CraftingRecipe>> built = crafting.getRecipeFor(bed, helper.getLevel());
			helper.assertTrue(built.isPresent() && built.get().value().assemble(bed).is(item("vanadium_contact_bed")),
					"The oxide on two bricks makes the contact bed");
		});
	}

	/**
	 * Ammonium nitrate: 250 mB of nitric acid and 125 mB of ammonia give four ammonium nitrate at the chute, 100 ticks at
	 * 87 JE. It is a three-dose fertilizer, and it packs a grenade in place of guncotton.
	 */
	@GameTest(structure = ARENA, maxTicks = 300)
	public void nitricAcidAndAmmoniaGiveAmmoniumNitrate(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(6, 1, 2), Direction.NORTH);
		long[] supplied = supply(helper, machine);
		insert(helper, port(helper, machine, "liquid_in"), PetroFluids.NITRIC_ACID.source(), 250);
		insert(helper, port(helper, machine, "gas_in"), PetroFluids.AMMONIA.fluid(), 125);
		helper.succeedWhen(() -> {
			helper.assertTrue(machine.lane(0) == null && machine.tanks().input(0).millibuckets() == 0
					&& machine.tanks().input(1).millibuckets() == 0, "The acid and the ammonia used");
			helper.assertTrue(used(machine, supplied) == TICKS * DRAW, "Paid " + used(machine, supplied) + " JE");
			helper.assertTrue(extractItems(helper, itemPort(helper, machine, "items_out"), item("ammonium_nitrate")) == 4,
					"Four ammonium nitrate leave at the chute");
			helper.assertTrue(item("ammonium_nitrate") instanceof FertilizerItem, "Ammonium nitrate is a fertilizer");
			RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
			ItemStack plate = stack("steel_plate");
			CraftingInput grenade = CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, new ItemStack(Items.IRON_NUGGET), ItemStack.EMPTY,
					plate, stack("ammonium_nitrate"), plate, ItemStack.EMPTY, plate, ItemStack.EMPTY));
			ItemStack made = crafting.getRecipeFor(grenade, helper.getLevel()).map(holder -> holder.value().assemble(grenade))
					.orElse(ItemStack.EMPTY);
			helper.assertTrue(made.is(item("grenade")) && made.getCount() == 4, "Ammonium nitrate packs 4 grenades, not " + made);
		});
	}

	/** While it works every part shows lit, but only the controller gives off light; paused, it goes dark. */
	@GameTest(structure = ARENA, maxTicks = 200)
	public void everyPartLightsWhileItWorks(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(6, 1, 2), Direction.NORTH);
		supply(helper, machine);
		machine.tanks().input(0).fill(Fluids.WATER, 4000);
		machine.tanks().input(1).fill(PetroFluids.HYDROGEN_CHLORIDE.fluid(), 2000);
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
	 * It runs its absorption and its chloride leaching, and the Chemical Reactor runs neither. Its construction: a
	 * Chemical Reactor, a Steel Tank, a steel fluid pipe, an electric motor, a basic circuit and 4 steel plates.
	 */
	@GameTest(structure = ARENA)
	public void itRunsItsOwnRecipesAndIsBuiltFromItsBill(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FormMachineBlockEntity machine = place(helper, new BlockPos(6, 1, 2), Direction.NORTH);
		List<FluidRecipes.Entry> own = machine.recipes(level.getServer());
		List<Identifier> ids = own.stream().map(FluidRecipes.Entry::id).toList();
		helper.assertTrue(ids.contains(Jugcraft.id("chemical_reaction/bulk_hydrochloric_acid"))
				&& ids.contains(Jugcraft.id("chemical_reaction/bulk_chloride_iron_ore"))
				&& ids.stream().allMatch(id -> id.getPath().startsWith("chemical_reaction/bulk_")), "Its recipes: " + ids);
		long reactor = FluidRecipes.recipes(level.getServer(), MachineKind.CHEMICAL_REACTOR).stream()
				.filter(recipe -> recipe.fluidResults().stream().anyMatch(result -> result.fluid() == PetroFluids.HYDROCHLORIC_ACID.source())
						|| recipe.fluids().stream().anyMatch(input -> input.fluid() == PetroFluids.HYDROCHLORIC_ACID.source()))
				.count();
		helper.assertTrue(reactor == 0, "The Chemical Reactor neither makes nor uses hydrochloric acid");

		RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
		ItemStack plate = stack("steel_plate");
		CraftingInput bulk = CraftingInput.of(3, 3, List.of(plate, stack("steel_fluid_pipe"), plate, stack("electric_motor"),
				stack("chemical_reactor"), stack("basic_circuit"), plate, stack("steel_tank"), plate));
		Optional<RecipeHolder<CraftingRecipe>> built = crafting.getRecipeFor(bulk, level);
		helper.assertTrue(built.isPresent() && built.get().value().assemble(bulk).is(IndustrialForms.BULK_CHEMICAL_REACTOR.asItem()),
				"A Chemical Reactor, a Steel Tank, a pipe, a motor, a circuit and four plates make the Bulk Chemical Reactor");
		helper.succeed();
	}

	// ------------------------------------------------------------------ helpers

	private static FormMachineBlockEntity place(GameTestHelper helper, BlockPos relative, Direction facing) {
		helper.setBlock(relative, IndustrialForms.BULK_CHEMICAL_REACTOR.defaultBlockState().setValue(MachineBlock.FACING, facing));
		IndustrialForms.BULK_CHEMICAL_REACTOR.setPlacedBy(helper.getLevel(), helper.absolutePos(relative), helper.getBlockState(relative), null,
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
