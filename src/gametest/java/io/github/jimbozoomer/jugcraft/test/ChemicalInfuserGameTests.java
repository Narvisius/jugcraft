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
import net.minecraft.server.MinecraftServer;
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
 * The Chemical Infuser (docs/features/industrial-chemical-infuser.md), package 2's gas-reaction form: it stands whole in
 * every orientation with pipes and cables reaching it only at its declared ports; hydrogen and chlorine, each at its own
 * inlet, burn into hydrogen chloride at the plan's 9,600 JE a batch; a full product tank stops it; every part lights
 * while it works; it runs only its own recipe while the Synthesis Converter keeps its own; and it is built from the
 * plan's starter bill.
 */
public class ChemicalInfuserGameTests {
	private static final String ARENA = "jugcraft-test:arms_arena";
	private static final int PARTS = 18;
	/** The synthesis family's working draw. */
	private static final long DRAW = 128;
	private static final int TICKS = 75;
	private static final int METHANATION_TICKS = 200;
	/** Output tanks, in the form's order. */
	private static final int PRODUCT = 0;
	private static final int WATER = 1;

	/**
	 * In every orientation it fills its eighteen blocks, and each block's faces answer pipes, cables and conveyors only
	 * where the form declares a port. The left inlet takes hydrogen, not chlorine; the right takes chlorine, not
	 * hydrogen; nothing goes in at an outlet.
	 */
	@GameTest(structure = ARENA)
	public void itStandsWholeAndOnlyItsPortsAnswer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MachineForm form = IndustrialForms.CHEMICAL_INFUSER_FORM;
		helper.assertTrue(form.footprint().size() == PARTS && IndustrialForms.CHEMICAL_INFUSER.partProperty().getPossibleValues().size() == PARTS,
				"Eighteen blocks, numbered 0 to 17");
		Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
		// Each stands in its own five-block square: turned, it reaches up to two blocks from its controller.
		BlockPos[] controllers = {new BlockPos(3, 1, 3), new BlockPos(12, 1, 3), new BlockPos(3, 1, 12), new BlockPos(12, 1, 12)};
		for (int i = 0; i < facings.length; i++) {
			Direction facing = facings[i];
			FormMachineBlockEntity machine = place(helper, controllers[i], facing);
			BlockPos controller = machine.getBlockPos();
			for (int part = 0; part < form.footprint().size(); part++) {
				BlockPos at = form.footprint().partPos(controller, facing, part);
				BlockState state = level.getBlockState(at);
				helper.assertTrue(state.is(IndustrialForms.CHEMICAL_INFUSER) && IndustrialForms.CHEMICAL_INFUSER.part(state) == part
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
			Storage<FluidVariant> hydrogen = port(helper, machine, "hydrogen_in");
			Storage<FluidVariant> reactant = port(helper, machine, "reactant_in");
			Storage<FluidVariant> product = port(helper, machine, "product_out");
			try (Transaction transaction = Transaction.openOuter()) {
				helper.assertTrue(hydrogen.insert(FluidVariant.of(PetroFluids.CHLORINE.fluid()), FluidConstants.BUCKET, transaction) == 0,
						facing + ": the left inlet refuses chlorine");
				helper.assertTrue(reactant.insert(FluidVariant.of(PetroFluids.ARGON.fluid()), FluidConstants.BUCKET, transaction) == 0,
						facing + ": the right inlet refuses argon");
				helper.assertTrue(reactant.insert(FluidVariant.of(Fluids.LAVA), FluidConstants.BUCKET, transaction) == 0,
						facing + ": the right inlet refuses lava");
				helper.assertTrue(product.insert(FluidVariant.of(PetroFluids.HYDROGEN.fluid()), FluidConstants.BUCKET, transaction) == 0,
						facing + ": nothing goes in at an outlet");
				transaction.abort();
			}
			try (Transaction transaction = Transaction.openOuter()) {
				helper.assertTrue(hydrogen.insert(FluidVariant.of(PetroFluids.HYDROGEN.fluid()), FluidConstants.BUCKET, transaction) == FluidConstants.BUCKET
						&& reactant.insert(FluidVariant.of(PetroFluids.CHLORINE.fluid()), FluidConstants.BUCKET, transaction) == FluidConstants.BUCKET,
						facing + ": each inlet takes its gas");
				transaction.commit();
			}
			helper.assertTrue(amount(machine.tanks().input(0), PetroFluids.HYDROGEN.fluid()) == 1000
					&& amount(machine.tanks().input(1), PetroFluids.CHLORINE.fluid()) == 1000, facing + ": each bucket reached its tank");
		}
		helper.succeed();
	}

	/**
	 * 250 mB each of hydrogen and chlorine: 75 paid ticks at 128 JE, the plan's 9,600 JE, then 500 mB of hydrogen
	 * chloride at the product collar and nothing in the water tank.
	 */
	@GameTest(maxTicks = 200)
	public void hydrogenAndChlorineMakeHydrogenChloride(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(2, 1, 2), Direction.NORTH);
		long[] supplied = supply(helper, machine);
		insert(helper, port(helper, machine, "hydrogen_in"), PetroFluids.HYDROGEN.fluid(), 250);
		insert(helper, port(helper, machine, "reactant_in"), PetroFluids.CHLORINE.fluid(), 250);
		helper.runAfterDelay(3, () -> helper.assertTrue(machine.lane(0) != null && machine.reservedMb(PRODUCT) == 500,
				"The batch started with room for its product reserved"));
		helper.succeedWhen(() -> {
			helper.assertTrue(machine.lane(0) == null, "Still reacting");
			helper.assertTrue(amount(machine.tanks().output(PRODUCT), PetroFluids.HYDROGEN_CHLORIDE.fluid()) == 500
					&& machine.tanks().output(WATER).millibuckets() == 0
					&& machine.tanks().input(0).millibuckets() == 0 && machine.tanks().input(1).millibuckets() == 0,
					"Both gases used, 500 mB of hydrogen chloride made, no water");
			long used = used(machine, supplied);
			helper.assertTrue(used == TICKS * DRAW, "Paid " + used + " JE, not " + TICKS * DRAW);
			helper.assertTrue(extract(helper, port(helper, machine, "product_out"), PetroFluids.HYDROGEN_CHLORIDE.fluid()) == 500,
					"The hydrogen chloride leaves at the product collar");
		});
	}

	/**
	 * With the nickel catalyst bed in its socket, 400 mB of hydrogen and 100 mB of carbon dioxide give 100 mB of methane
	 * at the product collar and 400 mB of water at the drain, for the plan's 25,600 JE, and the bed is still there.
	 */
	@GameTest(maxTicks = 300)
	public void theNickelBedMakesMethaneAndWater(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(2, 1, 2), Direction.NORTH);
		long[] supplied = supply(helper, machine);
		int socket = machine.form().firstSocketSlot();
		machine.container().setItem(socket, stack("nickel_catalyst_bed"));
		insert(helper, port(helper, machine, "hydrogen_in"), PetroFluids.HYDROGEN.fluid(), 400);
		insert(helper, port(helper, machine, "reactant_in"), PetroFluids.CARBON_DIOXIDE.fluid(), 100);
		helper.runAfterDelay(3, () -> helper.assertTrue(machine.lane(0) != null && machine.socketLocked(0),
				"The batch started and holds the bed"));
		helper.succeedWhen(() -> {
			helper.assertTrue(machine.lane(0) == null, "Still reacting");
			helper.assertTrue(amount(machine.tanks().output(PRODUCT), PetroFluids.METHANE.fluid()) == 100
					&& amount(machine.tanks().output(WATER), Fluids.WATER) == 400, "100 mB of methane and 400 mB of water");
			long used = used(machine, supplied);
			helper.assertTrue(used == METHANATION_TICKS * DRAW, "Paid " + used + " JE, not " + METHANATION_TICKS * DRAW);
			helper.assertTrue(machine.container().getItem(socket).is(item("nickel_catalyst_bed")), "The bed was used up");
			helper.assertTrue(extract(helper, port(helper, machine, "product_out"), PetroFluids.METHANE.fluid()) == 100
					&& extract(helper, port(helper, machine, "water_out"), Fluids.WATER) == 400, "Methane and water leave at their collars");
		});
	}

	/**
	 * With the bed, 250 mB of the Anaerobic Digester's biogas and 400 mB of hydrogen upgrade into 250 mB of methane (the
	 * biogas's own 150 and 100 more from its carbon dioxide) and 400 mB of water, for methanation's 25,600 JE.
	 */
	@GameTest(maxTicks = 300)
	public void theNickelBedUpgradesBiogasIntoMethane(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(2, 1, 2), Direction.NORTH);
		long[] supplied = supply(helper, machine);
		machine.container().setItem(machine.form().firstSocketSlot(), stack("nickel_catalyst_bed"));
		insert(helper, port(helper, machine, "hydrogen_in"), PetroFluids.HYDROGEN.fluid(), 400);
		insert(helper, port(helper, machine, "reactant_in"), PetroFluids.BIOGAS.fluid(), 250);
		helper.succeedWhen(() -> {
			helper.assertTrue(machine.lane(0) == null && amount(machine.tanks().output(PRODUCT), PetroFluids.METHANE.fluid()) == 250
					&& amount(machine.tanks().output(WATER), Fluids.WATER) == 400, "250 mB of methane and 400 mB of water");
			helper.assertTrue(used(machine, supplied) == METHANATION_TICKS * DRAW, "Paid " + used(machine, supplied) + " JE");
		});
	}

	/**
	 * With the bed, 100 mB of carbon monoxide from the gas cleanup rack and 300 mB of hydrogen give 100 mB of methane and
	 * 200 mB of water (CO + 3 H2 -> CH4 + H2O), 150 ticks at 128 JE/t.
	 */
	@GameTest(maxTicks = 300)
	public void theNickelBedTurnsCarbonMonoxideIntoMethane(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(2, 1, 2), Direction.NORTH);
		long[] supplied = supply(helper, machine);
		machine.container().setItem(machine.form().firstSocketSlot(), stack("nickel_catalyst_bed"));
		insert(helper, port(helper, machine, "hydrogen_in"), PetroFluids.HYDROGEN.fluid(), 300);
		insert(helper, port(helper, machine, "reactant_in"), PetroFluids.CARBON_MONOXIDE.fluid(), 100);
		helper.succeedWhen(() -> {
			helper.assertTrue(machine.lane(0) == null && amount(machine.tanks().output(PRODUCT), PetroFluids.METHANE.fluid()) == 100
					&& amount(machine.tanks().output(WATER), Fluids.WATER) == 200, "100 mB of methane and 200 mB of water");
			helper.assertTrue(used(machine, supplied) == 150 * DRAW, "Paid " + used(machine, supplied) + " JE");
		});
	}

	/**
	 * With the vanadium contact bed, 250 mB of sulfur dioxide and 125 mB of oxygen give 250 mB of sulfur trioxide, the
	 * plan's 12,800 JE; the nickel bed does not serve, and the gases wait on the bed socket.
	 */
	@GameTest(maxTicks = 300)
	public void theVanadiumBedTurnsSulfurDioxideIntoSulfurTrioxide(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(2, 1, 2), Direction.NORTH);
		long[] supplied = supply(helper, machine);
		int socket = machine.form().firstSocketSlot();
		machine.container().setItem(socket, stack("nickel_catalyst_bed"));
		insert(helper, port(helper, machine, "hydrogen_in"), PetroFluids.OXYGEN.fluid(), 125);
		insert(helper, port(helper, machine, "reactant_in"), PetroFluids.SULFUR_DIOXIDE.fluid(), 250);
		helper.runAfterDelay(10, () -> {
			MachineStatus status = machine.status();
			helper.assertTrue(status.reason() == MachineStatus.Reason.TOOL_REQUIRED && machine.lane(0) == null && used(machine, supplied) == 0,
					"The nickel bed does not serve: " + status);
			machine.container().setItem(socket, stack("vanadium_contact_bed"));
		});
		helper.runAfterDelay(150, () -> {
			helper.assertTrue(machine.lane(0) == null && amount(machine.tanks().output(PRODUCT), PetroFluids.SULFUR_TRIOXIDE.fluid()) == 250,
					"250 mB of sulfur trioxide made");
			helper.assertTrue(used(machine, supplied) == 100 * DRAW, "Paid " + used(machine, supplied) + " JE");
			helper.assertTrue(machine.container().getItem(socket).is(item("vanadium_contact_bed")), "The bed was not used up");
			helper.succeed();
		});
	}

	/**
	 * The detailed PVC route's direct chlorination: 250 mB of refinery gas (its ethylene) and 250 mB of chlorine join, with
	 * no bed, into 500 mB of ethylene dichloride (the liquid at 2 mB a reaction amount), 60 ticks at 128 JE/t. The
	 * existing PVC chain's other steps are in place: the Catalytic Cracker cracks the dichloride into vinyl chloride and
	 * hydrogen chloride, and the Synthesis Converter reuses that hydrogen chloride with ethylene and oxygen.
	 */
	@GameTest(maxTicks = 200)
	public void ethyleneAndChlorineJoinIntoEthyleneDichloride(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(2, 1, 2), Direction.NORTH);
		long[] supplied = supply(helper, machine);
		insert(helper, port(helper, machine, "hydrogen_in"), PetroFluids.REFINERY_GAS.fluid(), 250);
		insert(helper, port(helper, machine, "reactant_in"), PetroFluids.CHLORINE.fluid(), 250);
		helper.succeedWhen(() -> {
			helper.assertTrue(machine.lane(0) == null
					&& amount(machine.tanks().output(PRODUCT), PetroFluids.ETHYLENE_DICHLORIDE.source()) == 500, "500 mB of ethylene dichloride");
			helper.assertTrue(used(machine, supplied) == 60 * DRAW, "Paid " + used(machine, supplied) + " JE");
			MinecraftServer server = helper.getLevel().getServer();
			boolean cracks = FluidRecipes.recipes(server, MachineKind.CATALYTIC_CRACKER).stream().anyMatch(recipe ->
					recipe.fluids().size() == 1 && recipe.fluids().get(0).fluid() == PetroFluids.ETHYLENE_DICHLORIDE.source()
							&& recipe.fluids().get(0).amount() == 500
							&& recipe.fluidResults().stream().anyMatch(r -> r.fluid() == PetroFluids.VINYL_CHLORIDE.fluid() && r.amount() == 250)
							&& recipe.fluidResults().stream().anyMatch(r -> r.fluid() == PetroFluids.HYDROGEN_CHLORIDE.fluid() && r.amount() == 250));
			helper.assertTrue(cracks, "The Catalytic Cracker cracks 500 mB of dichloride into 250 mB of vinyl chloride and 250 mB of hydrogen chloride");
			boolean reuses = FluidRecipes.recipes(server, MachineKind.SYNTHESIS_CONVERTER).stream().anyMatch(recipe ->
					recipe.fluids().stream().anyMatch(f -> f.fluid() == PetroFluids.HYDROGEN_CHLORIDE.fluid() && f.amount() == 500)
							&& recipe.fluidResults().stream().anyMatch(r -> r.fluid() == PetroFluids.ETHYLENE_DICHLORIDE.source() && r.amount() == 500));
			helper.assertTrue(reuses, "The Synthesis Converter reuses 500 mB of hydrogen chloride into 500 mB of dichloride");
		});
	}

	/** Without a bed, methane's gases wait and the screen names the bed socket; nothing is taken or spent. */
	@GameTest(maxTicks = 60)
	public void methanationWaitsForItsBed(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(2, 1, 2), Direction.NORTH);
		long[] supplied = supply(helper, machine);
		machine.tanks().input(0).fill(PetroFluids.HYDROGEN.fluid(), 400);
		machine.tanks().input(1).fill(PetroFluids.CARBON_DIOXIDE.fluid(), 100);
		helper.runAfterDelay(10, () -> {
			MachineStatus status = machine.status();
			helper.assertTrue(status.reason() == MachineStatus.Reason.TOOL_REQUIRED && machine.lane(0) == null && used(machine, supplied) == 0,
					"Waiting for the bed: " + status);
			machine.container().setItem(machine.form().firstSocketSlot(), stack("nickel_catalyst_bed"));
		});
		helper.runAfterDelay(20, () -> {
			helper.assertTrue(machine.lane(0) != null, "It starts once the bed is in: " + machine.status());
			helper.succeed();
		});
	}

	/**
	 * Hydrogen alone waits, naming the chlorine it needs and spending nothing; with the product tank full, the gases wait
	 * and the screen names the full tank.
	 */
	@GameTest(maxTicks = 100)
	public void itSaysWhatItIsWaitingFor(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(2, 1, 2), Direction.NORTH);
		long[] supplied = supply(helper, machine);
		machine.tanks().input(0).fill(PetroFluids.HYDROGEN.fluid(), 250);
		helper.runAfterDelay(10, () -> {
			MachineStatus status = machine.status();
			helper.assertTrue(status.state() == MachineLifecycle.WAITING_INPUT && status.reason() == MachineStatus.Reason.MISSING_FLUID
					&& status.index() == 1 && status.subject() == BuiltInRegistries.FLUID.getId(PetroFluids.CHLORINE.fluid()),
					"Hydrogen alone waits for chlorine: " + status);
			helper.assertTrue(machine.lane(0) == null && used(machine, supplied) == 0, "Nothing started, nothing spent");
			machine.tanks().output(PRODUCT).fill(PetroFluids.HYDROGEN_CHLORIDE.fluid(), machine.form().profile().bufferMb());
			machine.tanks().input(1).fill(PetroFluids.CHLORINE.fluid(), 250);
		});
		helper.runAfterDelay(20, () -> {
			MachineStatus status = machine.status();
			int tank = machine.form().tanks().inputTanks().size() + PRODUCT;
			helper.assertTrue(status.state() == MachineLifecycle.OUTPUT_BLOCKED && status.reason() == MachineStatus.Reason.OUTPUT_FULL
					&& status.index() == tank, "The gases wait on the full product tank: " + status);
			helper.assertTrue(machine.lane(0) == null && used(machine, supplied) == 0, "Still nothing taken or spent");
			machine.tanks().output(PRODUCT).drain(machine.form().profile().bufferMb());
		});
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(machine.lane(0) != null && machine.status().state().working(),
					"It goes on once the product is drawn off: " + machine.status());
			helper.succeed();
		});
	}

	/**
	 * While it works every part shows lit, so the lamp and the process band light on the whole machine, but only the
	 * controller gives off light; paused, it goes dark once {@link FormMachineBlockEntity#LIT_HOLD} ticks have passed.
	 */
	@GameTest(maxTicks = 200)
	public void everyPartLightsWhileItWorks(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, new BlockPos(2, 1, 2), Direction.NORTH);
		supply(helper, machine);
		machine.tanks().input(0).fill(PetroFluids.HYDROGEN.fluid(), 1000);
		machine.tanks().input(1).fill(PetroFluids.CHLORINE.fluid(), 1000);
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
	 * The Infuser runs exactly its one capability recipe; the Synthesis Converter keeps its own three and never sees
	 * the Infuser's. And its construction: 4 steel plates, 2 steel fluid pipes, a Tinplate Tank, copper cable and a
	 * basic circuit.
	 */
	@GameTest
	public void itRunsItsOwnRecipeAndIsBuiltFromTheStarterBill(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FormMachineBlockEntity machine = place(helper, new BlockPos(2, 1, 2), Direction.NORTH);
		List<FluidRecipes.Entry> own = machine.recipes(level.getServer()).stream()
                .filter(e -> !e.id().getPath().startsWith("gas_synthesis/synthetic_")).toList();
		helper.assertTrue(own.size() == 11 && own.get(0).id().equals(Jugcraft.id("gas_synthesis/infuser_biogas_upgrading"))
				&& own.get(0).recipe().time() == METHANATION_TICKS && own.get(0).recipe().tool().isPresent()
				&& own.get(1).id().equals(Jugcraft.id("gas_synthesis/infuser_carbon_dioxide_methanation"))
				&& own.get(1).recipe().time() == METHANATION_TICKS && own.get(1).recipe().tool().isPresent()
				&& own.get(2).id().equals(Jugcraft.id("gas_synthesis/infuser_carbon_monoxide_methanation"))
				&& own.get(2).recipe().time() == 150 && own.get(2).recipe().tool().isPresent()
				&& own.get(3).id().equals(Jugcraft.id("gas_synthesis/infuser_ethylene_dichloride")) && own.get(3).recipe().time() == 60
				&& own.get(3).recipe().tool().isEmpty()
				&& own.get(4).id().equals(Jugcraft.id("gas_synthesis/infuser_hydrogen_chloride")) && own.get(4).recipe().time() == TICKS
				&& own.get(4).recipe().tool().isEmpty()
				&& own.get(5).id().equals(Jugcraft.id("gas_synthesis/infuser_sulfur_trioxide")) && own.get(5).recipe().time() == 100
				&& own.get(5).recipe().tool().isPresent(), "Six original plus five expanded recipes: " + own.stream().map(FluidRecipes.Entry::id).toList());
		long converter = FluidRecipes.recipes(level.getServer(), MachineKind.SYNTHESIS_CONVERTER).stream()
				.filter(recipe -> recipe.fluidResults().stream().anyMatch(result -> result.fluid() == PetroFluids.HYDROGEN_CHLORIDE.fluid()))
				.count();
		helper.assertTrue(converter == 0, "The Synthesis Converter does not make hydrogen chloride");

		RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
		ItemStack plate = stack("steel_plate");
		ItemStack pipe = stack("steel_fluid_pipe");
		CraftingInput infuser = CraftingInput.of(3, 3, List.of(plate, pipe, plate, stack("copper_cable"), stack("fluid_tank"),
				stack("basic_circuit"), plate, pipe, plate));
		Optional<RecipeHolder<CraftingRecipe>> built = crafting.getRecipeFor(infuser, level);
		helper.assertTrue(built.isPresent() && built.get().value().assemble(infuser).is(IndustrialForms.CHEMICAL_INFUSER.asItem()),
				"Four plates, two steel fluid pipes, copper cable, a Tinplate Tank and a basic circuit make the Infuser");
		// Its catalyst bed: a nickel ingot between two fired bricks.
		ItemStack brick = new ItemStack(Items.BRICK);
		CraftingInput bed = CraftingInput.of(3, 1, List.of(brick, stack("nickel_ingot"), brick));
		Optional<RecipeHolder<CraftingRecipe>> bedRecipe = crafting.getRecipeFor(bed, level);
		helper.assertTrue(bedRecipe.isPresent() && bedRecipe.get().value().assemble(bed).is(item("nickel_catalyst_bed")),
				"A nickel ingot between two bricks makes the Nickel Catalyst Bed");
		helper.succeed();
	}

	// ------------------------------------------------------------------ helpers

	/** Places the whole Infuser as its item would, facing {@code facing}, its controller at {@code relative}. */
	private static FormMachineBlockEntity place(GameTestHelper helper, BlockPos relative, Direction facing) {
		helper.setBlock(relative, IndustrialForms.CHEMICAL_INFUSER.defaultBlockState().setValue(MachineBlock.FACING, facing));
		IndustrialForms.CHEMICAL_INFUSER.setPlacedBy(helper.getLevel(), helper.absolutePos(relative), helper.getBlockState(relative), null,
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
