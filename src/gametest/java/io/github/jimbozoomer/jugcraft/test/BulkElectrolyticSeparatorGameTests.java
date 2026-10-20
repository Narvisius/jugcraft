package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipes;
import io.github.jimbozoomer.jugcraft.chemistry.FluidTank;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineUpgrades;
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
 * The Bulk Electrolytic Separator (docs/features/industrial-bulk-electrolytic-separator.md), the Electrolytic
 * Separator's 4x4x6 variant: it stands whole in every orientation with pipes and cables reaching it only at its five
 * ports; it splits the Separator's brine four batches at a time at 80% of the draw a batch; it takes no upgrade cards,
 * so its hydrogen stays above the electrolysis floor; every part lights while it works; and it is built from an
 * Electrolytic Separator and its bill.
 */
public class BulkElectrolyticSeparatorGameTests {
	private static final String ARENA = "jugcraft-test:arms_arena";
	private static final int PARTS = 81;
	/** The Electrolytic Cell family's 256 JE/t at the bulk profile's 80%, rounded up (OperatingProfile.lanePerTick). */
	private static final long DRAW = 205;
	private static final int BRINE_TICKS = 400;
	private static final int WATER_TICKS = 800;
	/** The plan's electrolysis floor, JE per mB of hydrogen (docs/features/industrial-electrolysis-floor.md). */
	private static final long FLOOR = 256;
	private static final int ANODE_GAS = 0;
	private static final int HYDROGEN = 1;
	private static final int LYE = 2;

	/**
	 * In every orientation it fills its 81 blocks and leaves its open positions free, and each block's faces answer
	 * pipes and cables only where the form declares a port. The inlet takes brine and water, not oxygen; nothing goes in
	 * at the three outlets.
	 */
	@GameTest(structure = ARENA)
	public void itStandsWholeAndOnlyItsPortsAnswer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MachineForm form = IndustrialForms.BULK_ELECTROLYTIC_SEPARATOR_FORM;
		helper.assertTrue(form.positions() == 96 && form.footprint().size() == PARTS
				&& IndustrialForms.BULK_ELECTROLYTIC_SEPARATOR.partProperty().getPossibleValues().size() == PARTS, "81 blocks in a 4x4x6 envelope");
		Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
		// One copy per orientation, each clear of the others inside the arena.
		BlockPos[] controllers = {new BlockPos(5, 1, 1), new BlockPos(14, 1, 5), new BlockPos(1, 1, 14), new BlockPos(9, 1, 9)};
		for (int i = 0; i < facings.length; i++) {
			Direction facing = facings[i];
			FormMachineBlockEntity machine = place(helper, controllers[i], facing);
			BlockPos controller = machine.getBlockPos();
			for (int part = 0; part < form.footprint().size(); part++) {
				BlockPos at = form.footprint().partPos(controller, facing, part);
				BlockState state = level.getBlockState(at);
				helper.assertTrue(state.is(IndustrialForms.BULK_ELECTROLYTIC_SEPARATOR) && IndustrialForms.BULK_ELECTROLYTIC_SEPARATOR.part(state) == part
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
			Storage<FluidVariant> inlet = port(helper, machine, "feed_in");
			try (Transaction transaction = Transaction.openOuter()) {
				helper.assertTrue(inlet.insert(FluidVariant.of(PetroFluids.OXYGEN.fluid()), FluidConstants.BUCKET, transaction) == 0,
						facing + ": the inlet refuses oxygen");
				for (String outlet : List.of("anode_gas_out", "hydrogen_out", "lye_out")) {
					helper.assertTrue(port(helper, machine, outlet).insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction) == 0,
							facing + ": nothing goes in at " + outlet);
				}
				helper.assertTrue(inlet.insert(FluidVariant.of(PetroFluids.BRINE.source()), FluidConstants.BUCKET, transaction) == FluidConstants.BUCKET,
						facing + ": the inlet takes brine");
				transaction.abort();
			}
			try (Transaction transaction = Transaction.openOuter()) {
				helper.assertTrue(inlet.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction) == FluidConstants.BUCKET,
						facing + ": the inlet takes water");
				transaction.abort();
			}
		}
		helper.succeed();
	}

	/**
	 * Four buckets of brine run four batches side by side, each 400 paid ticks at the bulk profile's 205 JE: 1,000 mB of
	 * chlorine, 1,000 mB of hydrogen and 2,000 mB of lye for 328,000 JE, where four Separator batches cost 409,600.
	 */
	@GameTest(structure = ARENA, maxTicks = 600)
	public void brineSplitsFourBatchesAtOnce(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(5, 1, 1), Direction.NORTH);
		long[] supplied = supply(helper, machine);
		insert(helper, port(helper, machine, "feed_in"), PetroFluids.BRINE.source(), 4000);
		helper.runAfterDelay(5, () -> helper.assertTrue(machine.lane(0) != null && machine.lane(1) != null && machine.lane(2) != null
				&& machine.lane(3) != null, "All four batches started together"));
		helper.succeedWhen(() -> {
			helper.assertTrue(machine.lane(0) == null && machine.lane(1) == null && machine.lane(2) == null && machine.lane(3) == null, "Still splitting");
			helper.assertTrue(amount(machine.tanks().output(ANODE_GAS), PetroFluids.CHLORINE.fluid()) == 1000
					&& amount(machine.tanks().output(HYDROGEN), PetroFluids.HYDROGEN.fluid()) == 1000
					&& amount(machine.tanks().output(LYE), PetroFluids.LYE.source()) == 2000 && machine.tanks().input(0).millibuckets() == 0,
					"The brine used up into 1,000 chlorine, 1,000 hydrogen and 2,000 lye");
			long used = used(machine, supplied);
			helper.assertTrue(used == 4 * BRINE_TICKS * DRAW, "Paid " + used + " JE, not " + 4 * BRINE_TICKS * DRAW);
			helper.assertTrue(extract(helper, port(helper, machine, "hydrogen_out"), PetroFluids.HYDROGEN.fluid()) == 1000
					&& extract(helper, port(helper, machine, "anode_gas_out"), PetroFluids.CHLORINE.fluid()) == 1000
					&& extract(helper, port(helper, machine, "lye_out"), PetroFluids.LYE.source()) == 2000,
					"Each product leaves at its own outlet");
		});
	}

	/**
	 * It has no upgrade slots, so no card changes its draw: hydrogen from water costs 328 JE/mB (800 ticks at 205 JE for
	 * 500 mB), above the 256 JE/mB floor and a fifth below an uncarded Separator's 409.6.
	 */
	@GameTest(structure = ARENA)
	public void itTakesNoCardsAndStaysAboveTheFloor(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FormMachineBlockEntity machine = place(helper, new BlockPos(5, 1, 1), Direction.NORTH);
		helper.assertTrue(machine.form().upgradeSlots() == 0 && machine.upgrades().equals(MachineUpgrades.Effect.NONE)
				&& IndustrialForms.ELECTROLYTIC_SEPARATOR_FORM.upgradeSlots() > 0, "No upgrade slots, where the Separator has them");
		FluidRecipes.Entry water = machine.recipes(level.getServer()).stream()
				.filter(entry -> entry.id().equals(Jugcraft.id("electrolysis/separator_water"))).findFirst().orElseThrow();
		long perMb = water.recipe().time() * machine.form().profile().lanePerTick(256) / 500;
		helper.assertTrue(water.recipe().time() == WATER_TICKS && perMb == 328 && perMb >= FLOOR,
				"Hydrogen from water costs " + perMb + " JE/mB, not 328 at or above the floor");
		helper.succeed();
	}

	/** While it works every part shows lit, but only the controller gives off light; paused, it goes dark. */
	@GameTest(structure = ARENA, maxTicks = 200)
	public void everyPartLightsWhileItWorks(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(5, 1, 1), Direction.NORTH);
		supply(helper, machine);
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
	 * It runs exactly the Electrolytic Separator's recipes, in four lanes, and is built from an Electrolytic Separator,
	 * two Steel Tanks, steel plates, copper cable and a basic circuit.
	 */
	@GameTest(structure = ARENA)
	public void itRunsTheSeparatorsRecipesAndIsBuiltFromItsBill(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FormMachineBlockEntity machine = place(helper, new BlockPos(5, 1, 1), Direction.NORTH);
		helper.setBlock(new BlockPos(12, 1, 10), IndustrialForms.ELECTROLYTIC_SEPARATOR.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
		IndustrialForms.ELECTROLYTIC_SEPARATOR.setPlacedBy(level, helper.absolutePos(new BlockPos(12, 1, 10)), helper.getBlockState(new BlockPos(12, 1, 10)),
				null, ItemStack.EMPTY);
		FormMachineBlockEntity small = helper.getBlockEntity(new BlockPos(12, 1, 10), FormMachineBlockEntity.class);
		List<?> own = machine.recipes(level.getServer()).stream().map(FluidRecipes.Entry::id).sorted().toList();
		List<?> separators = small.recipes(level.getServer()).stream().map(FluidRecipes.Entry::id).sorted().toList();
		helper.assertTrue(!own.isEmpty() && own.equals(separators), "Its recipes " + own + " are the Separator's " + separators);
		helper.assertTrue(machine.form().profile().lanes() == 4 && machine.form().profile().energyPercent() == 80, "Four lanes at 80%");
		RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
		ItemStack tank = stack("steel_tank");
		ItemStack plate = stack("steel_plate");
		CraftingInput bill = CraftingInput.of(3, 3, List.of(tank, new ItemStack(IndustrialForms.ELECTROLYTIC_SEPARATOR), tank, plate,
				stack("copper_cable"), plate, plate, stack("basic_circuit"), plate));
		Optional<RecipeHolder<CraftingRecipe>> built = crafting.getRecipeFor(bill, level);
		helper.assertTrue(built.isPresent() && built.get().value().assemble(bill).is(IndustrialForms.BULK_ELECTROLYTIC_SEPARATOR.asItem()),
				"Its bill makes the Bulk Electrolytic Separator");
		helper.succeed();
	}

	// ------------------------------------------------------------------ helpers

	private static FormMachineBlockEntity place(GameTestHelper helper, BlockPos relative, Direction facing) {
		helper.setBlock(relative, IndustrialForms.BULK_ELECTROLYTIC_SEPARATOR.defaultBlockState().setValue(MachineBlock.FACING, facing));
		IndustrialForms.BULK_ELECTROLYTIC_SEPARATOR.setPlacedBy(helper.getLevel(), helper.absolutePos(relative), helper.getBlockState(relative), null,
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

	private static Storage<FluidVariant> port(GameTestHelper helper, FormMachineBlockEntity machine, String name) {
		MachineForm form = machine.form();
		Direction facing = machine.getBlockState().getValue(MachineBlock.FACING);
		FormPort port = form.ports().stream().filter(p -> p.name().equals(name)).findFirst().orElseThrow();
		BlockPos at = form.cellPos(machine.getBlockPos(), facing, form.cellIndex(port.column(), port.row(), port.layer()));
		Storage<FluidVariant> storage = FluidStorage.SIDED.find(helper.getLevel(), at, port.side().world(facing));
		helper.assertTrue(storage != null, "No storage at port " + name);
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
