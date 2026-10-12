package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.FluidMachineSpec;
import io.github.jimbozoomer.jugcraft.chemistry.FluidTank;
import io.github.jimbozoomer.jugcraft.chemistry.FluidTanks;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.machine.LargeMachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.machine.form.FormMachineBlock;
import io.github.jimbozoomer.jugcraft.machine.form.FormMachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.form.FormMachineMenu;
import io.github.jimbozoomer.jugcraft.machine.form.FormPort;
import io.github.jimbozoomer.jugcraft.machine.form.FormSide;
import io.github.jimbozoomer.jugcraft.machine.form.MachineForm;
import io.github.jimbozoomer.jugcraft.machine.form.MachineLifecycle;
import io.github.jimbozoomer.jugcraft.machine.form.MachineStatus;
import io.github.jimbozoomer.jugcraft.machine.form.WorkLane;
import java.util.List;
import java.util.function.Supplier;
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
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The shared industrial machine foundation (docs/features/industrial-machine-foundation.md), package 1 of the factory
 * plan: form descriptors check themselves; a test form places only when every structural and clearance position is
 * free, in all four orientations, and breaks whole without touching anything else or duplicating anything; ports are
 * the only way in; batches escrow their inputs, reserve their outputs, need their tools, wait for power, survive saving
 * and cancel cleanly; the bulk profile runs four lanes; the original machines keep their recipes and their saves load.
 * The test forms and their recipes live in the test mod ({@link TestForms}), not in the game.
 */
public class IndustrialFoundationGameTests {
	private static final String ARENA = "jugcraft-test:arms_arena";
	/** The rig's unmodified working draw: the chemical reactor family's 96 JE per tick. */
	private static final long RIG_DRAW = 96;
	/** The test split recipe's work, and the rig's warm-up. */
	private static final int SPLIT_TICKS = 20;

	/**
	 * The descriptors: sizes, parts, rotation, and every kind of mistake refused when the form is built, a generator
	 * form's included.
	 */
	@GameTest
	public void formDescriptorsCheckThemselves(GameTestHelper helper) {
		MachineForm rig = TestForms.RIG_FORM;
		helper.assertTrue(rig.positions() == 12 && rig.footprint().size() == 8 && rig.clearanceCells().length == 1,
				"The rig is 2x2x3: 12 positions, 8 parts and 1 clearance, not " + rig.positions() + "/" + rig.footprint().size());
		MachineForm hall = TestForms.HALL_FORM;
		helper.assertTrue(hall.positions() == MachineForm.MAX_POSITIONS && hall.footprint().size() == 136
				&& hall.clearanceCells().length == 16, "The hall fills a 6x6x6 envelope with 136 parts and 16 clearance positions");
		helper.assertTrue(hall.footprint().offsets().getFirst().equals(Vec3i.ZERO), "Part 0 is the controller");
		helper.assertTrue(TestForms.RIG.partProperty().getPossibleValues().size() == 8
				&& TestForms.HALL.partProperty().getPossibleValues().size() == 136, "Each form block numbers only its own parts");
		helper.assertTrue(hall.containerSize() == 2 && rig.containerSize() == 5, "Slots: inputs, outputs, sockets, upgrades");

		// The oxygen port's block (column 1, row 0, layer 1) is one up and to the viewer's right of the controller.
		int cell = rig.cellIndex(1, 0, 1);
		BlockPos origin = BlockPos.ZERO;
		helper.assertTrue(rig.cellPos(origin, Direction.NORTH, cell).equals(new BlockPos(-1, 1, 0)), "Facing north, right is west");
		helper.assertTrue(rig.cellPos(origin, Direction.EAST, cell).equals(new BlockPos(0, 1, -1)), "Facing east, right is north");
		helper.assertTrue(rig.cellPos(origin, Direction.SOUTH, cell).equals(new BlockPos(1, 1, 0)), "Facing south, right is east");
		helper.assertTrue(rig.cellPos(origin, Direction.WEST, cell).equals(new BlockPos(0, 1, 1)), "Facing west, right is south");
		helper.assertTrue(FormSide.RIGHT.world(Direction.EAST) == Direction.NORTH && FormSide.of(Direction.NORTH, Direction.EAST) == FormSide.RIGHT,
				"Faces turn with the machine");

		rejects(helper, "a seven-block axis", () -> bad().layer("C######", "#######").layer("#######", "#######").build());
		rejects(helper, "a one-block axis", () -> bad().layer("C#", "##").build());
		rejects(helper, "no controller", () -> bad().layer("##", "##").layer("##", "##").build());
		rejects(helper, "a controller off the ground", () -> bad().layer("##", "##").layer("C#", "##").build());
		rejects(helper, "two controllers", () -> bad().layer("CC", "##").layer("##", "##").build());
		rejects(helper, "a floating part", () -> bad().layer("C#", "##").layer("..", "..").layer("#.", "..").build());
		rejects(helper, "an empty column", () -> bad().layer("C#.", "##.").layer("##.", "##.").build());
		rejects(helper, "a port facing its own block", () -> bad().layer("C#", "##").layer("##", "##").inputTank("water")
				.port("in", FormPort.Kind.FLUID_IN, 0, 0, 0, 0, FormSide.RIGHT).build());
		rejects(helper, "a port facing a clearance", () -> bad().layer("C#", "##").layer("~#", "##").inputTank("water")
				.port("in", FormPort.Kind.FLUID_IN, 0, 0, 0, 0, FormSide.TOP).build());
		rejects(helper, "two ports on one face", () -> bad().layer("C#", "##").layer("##", "##").inputTank("water")
				.port("a", FormPort.Kind.FLUID_IN, 0, 0, 0, 0, FormSide.LEFT).port("b", FormPort.Kind.FLUID_IN, 0, 0, 0, 0, FormSide.LEFT).build());
		rejects(helper, "a port to a missing tank", () -> bad().layer("C#", "##").layer("##", "##").inputTank("water")
				.port("in", FormPort.Kind.FLUID_IN, 1, 0, 0, 0, FormSide.LEFT).build());
		rejects(helper, "two tanks with one role", () -> bad().layer("C#", "##").layer("##", "##").inputTank("water").outputTank("water").build());
		rejects(helper, "seven tanks", () -> bad().layer("C#", "##").layer("##", "##").inputTank("a").inputTank("b").inputTank("c")
				.inputTank("d").outputTank("e").outputTank("f").outputTank("g").build());
		rejects(helper, "no capability", () -> MachineForm.builder(TestForms.id("bad"), MachineKind.CHEMICAL_REACTOR)
				.layer("C#", "##").layer("##", "##").build());
		rejects(helper, "a family without fluid recipes", () -> MachineForm.builder(TestForms.id("bad"), MachineKind.CRUSHER)
				.capability(TestForms.CAPABILITY).layer("C#", "##").layer("##", "##").build());

		// Generator forms: a generator family, one fuel tank and nothing else to fill, power out and never in.
		MachineForm burner = generator().build();
		helper.assertTrue(burner.generator() && burner.fuels().size() == 1 && burner.capabilities().isEmpty(), "A generator form builds");
		rejects(helper, "a generator of a processing family", () -> MachineForm.builder(TestForms.id("bad"), MachineKind.CHEMICAL_REACTOR)
				.layer("C#", "##").layer("##", "##").inputTank("fuel").fuel(Jugcraft.id("hydrogen"), 128, 128)
				.port("in", FormPort.Kind.FLUID_IN, 0, 0, 0, 0, FormSide.LEFT).port("out", FormPort.Kind.ENERGY_OUT, 0, 0, 1, 0, FormSide.BACK).build());
		rejects(helper, "a generator with a product tank", () -> generator().outputTank("smoke").build());
		rejects(helper, "a generator with upgrades", () -> generator().upgrades().build());
		rejects(helper, "a generator taking power", () -> generator().port("power", FormPort.Kind.ENERGY_IN, 0, 1, 0, 0, FormSide.FRONT).build());
		rejects(helper, "a fuel listed twice", () -> generator().fuel(Jugcraft.id("hydrogen"), 128, 128).build());
		rejects(helper, "a fuel making more a tick than the store holds", () -> generator().fuel(Jugcraft.id("methane"), 448, 1_000_000).build());
		rejects(helper, "a generator whose power cannot leave", () -> MachineForm.builder(TestForms.id("bad"), MachineKind.GAS_TURBINE)
				.layer("C#", "##").layer("##", "##").inputTank("fuel").fuel(Jugcraft.id("hydrogen"), 128, 128)
				.port("in", FormPort.Kind.FLUID_IN, 0, 0, 0, 0, FormSide.LEFT).build());
		rejects(helper, "a processing form giving power out", () -> bad().layer("C#", "##").layer("##", "##")
				.port("out", FormPort.Kind.ENERGY_OUT, 0, 0, 0, 0, FormSide.LEFT).build());
		helper.succeed();
	}

	/**
	 * Placing with the item checks every structural and clearance position first: a stone in the clearance or in a
	 * part's place stops it, and nothing at all is placed; once clear, the rig fills its parts and leaves the
	 * clearance free.
	 */
	@GameTest
	public void placingChecksEveryPositionFirst(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		player.setYRot(0); // Looking south, so the machine faces north, towards the player.
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TestForms.RIG));
		BlockPos floor = new BlockPos(3, 0, 3);
		BlockPos controller = floor.above();
		BlockPos clearance = controller.above(2);
		BlockPos backRight = controller.offset(-1, 0, 1);

		helper.setBlock(clearance, Blocks.STONE);
		helper.useBlock(floor, player, hit(helper, floor));
		helper.assertTrue(helper.getBlockState(controller).isAir() && rigBlocks(helper) == 0, "A blocked clearance stops the placement");
		helper.setBlock(clearance, Blocks.AIR);
		helper.setBlock(backRight, Blocks.STONE);
		helper.useBlock(floor, player, hit(helper, floor));
		helper.assertTrue(helper.getBlockState(controller).isAir() && rigBlocks(helper) == 0, "A blocked part stops the placement");
		helper.setBlock(backRight, Blocks.AIR);

		helper.useBlock(floor, player, hit(helper, floor));
		BlockState placed = helper.getBlockState(controller);
		helper.assertTrue(placed.is(TestForms.RIG) && placed.getValue(MachineBlock.FACING) == Direction.NORTH && TestForms.RIG.part(placed) == 0,
				"The controller stands where the player clicked, facing them: " + placed);
		MachineForm form = TestForms.RIG_FORM;
		for (int part = 0; part < form.footprint().size(); part++) {
			BlockState at = helper.getLevel().getBlockState(form.footprint().partPos(helper.absolutePos(controller), Direction.NORTH, part));
			helper.assertTrue(at.is(TestForms.RIG) && TestForms.RIG.part(at) == part, "Part " + part + " is " + at);
		}
		helper.assertTrue(rigBlocks(helper) == form.footprint().size(), "Exactly the rig's eight parts were placed");
		helper.assertTrue(helper.getBlockState(clearance).isAir(), "The clearance stays free");
		helper.assertTrue(helper.getBlockEntity(controller, FormMachineBlockEntity.class) != null, "Only the controller has the block entity");
		helper.succeed();
	}

	/**
	 * In every orientation each part is in place, and pipes, cables and conveyors find the machine only at its ports:
	 * water goes in at the water port, lava does not, and no other face of any part answers.
	 */
	@GameTest(structure = ARENA)
	public void portsAreTheOnlyWayInInEveryOrientation(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MachineForm form = TestForms.RIG_FORM;
		Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
		for (int i = 0; i < facings.length; i++) {
			Direction facing = facings[i];
			FormMachineBlockEntity machine = place(helper, TestForms.RIG, new BlockPos(2 + 4 * i, 1, 6), facing);
			BlockPos controller = machine.getBlockPos();
			for (int part = 0; part < form.footprint().size(); part++) {
				BlockPos at = form.footprint().partPos(controller, facing, part);
				BlockState state = level.getBlockState(at);
				helper.assertTrue(state.is(TestForms.RIG) && TestForms.RIG.part(state) == part && state.getValue(MachineBlock.FACING) == facing,
						facing + ": part " + part + " is " + state);
				for (Direction side : Direction.values()) {
					FormPort port = form.port(part, FormSide.of(side, facing));
					boolean fluid = FluidStorage.SIDED.find(level, at, side) != null;
					boolean items = ItemStorage.SIDED.find(level, at, side) != null;
					boolean power = EnergyStorage.SIDED.find(level, at, side) != null;
					helper.assertTrue(fluid == (port != null && port.kind().fluid()) && items == (port != null && port.kind().item())
							&& power == (port != null && port.kind() == FormPort.Kind.ENERGY_IN),
							facing + ": part " + part + " " + side + " answers fluid " + fluid + ", items " + items + ", power " + power
							+ " for port " + (port == null ? "none" : port.name()));
				}
			}
			Storage<FluidVariant> water = FluidStorage.SIDED.find(level, form.cellPos(controller, facing, form.cellIndex(0, 1, 0)),
					FormSide.LEFT.world(facing));
			try (Transaction transaction = Transaction.openOuter()) {
				helper.assertTrue(water.insert(FluidVariant.of(Fluids.LAVA), FluidConstants.BUCKET, transaction) == 0,
						"The water port refuses what no process here uses");
				helper.assertTrue(water.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction) == FluidConstants.BUCKET,
						"The water port takes water");
				transaction.commit();
			}
			helper.assertTrue(machine.tanks().input(0).millibuckets() == 1000, facing + ": the bucket reached the water tank");
		}
		helper.succeed();
	}

	/**
	 * Breaking the far top corner of the filled 6x6x6 hall removes all 136 parts and drops its item once, with its
	 * slots' contents; the blocks around it and in its open access space are untouched.
	 */
	@GameTest(structure = ARENA)
	public void breakingAnyPartRemovesTheWholeFormOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MachineForm form = TestForms.HALL_FORM;
		FormMachineBlockEntity machine = place(helper, TestForms.HALL, new BlockPos(8, 1, 2), Direction.NORTH);
		BlockPos controller = machine.getBlockPos();
		machine.container().setItem(0, new ItemStack(Items.COAL, 5));
		BlockPos inside = form.cellPos(controller, Direction.NORTH, form.cellIndex(2, 2, 1));
		BlockPos beside = form.cellPos(controller, Direction.NORTH, form.cellIndex(0, 2, 0)).relative(Direction.EAST);
		BlockPos aboveChimney = form.cellPos(controller, Direction.NORTH, form.cellIndex(2, 0, 5)).above();
		for (BlockPos marker : List.of(inside, beside, aboveChimney)) {
			level.setBlockAndUpdate(marker, Blocks.GLASS.defaultBlockState());
		}
		helper.assertTrue(level.getBlockState(form.cellPos(controller, Direction.NORTH, form.cellIndex(5, 5, 4))).is(TestForms.HALL),
				"The far top corner is part of the hall");
		level.destroyBlock(form.cellPos(controller, Direction.NORTH, form.cellIndex(5, 5, 4)), true);
		for (int part = 0; part < form.footprint().size(); part++) {
			BlockPos at = form.footprint().partPos(controller, Direction.NORTH, part);
			helper.assertTrue(level.getBlockState(at).isAir(), "Part " + part + " is still there");
		}
		for (BlockPos marker : List.of(inside, beside, aboveChimney)) {
			helper.assertTrue(level.getBlockState(marker).is(Blocks.GLASS), "Breaking the hall damaged " + marker);
		}
		List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(controller).inflate(10));
		helper.assertTrue(count(drops, TestForms.HALL.asItem()) == 1, "The hall drops once, not " + count(drops, TestForms.HALL.asItem()));
		helper.assertTrue(count(drops, Items.COAL) == 5, "Its slots drop once: " + count(drops, Items.COAL) + " coal");
		helper.succeed();
	}

	/**
	 * A batch moves its water into escrow and reserves room for its hydrogen and oxygen at the start, warms then works for
	 * its 20 paid ticks at 96 JE each, and finishes with exactly its results.
	 */
	@GameTest(maxTicks = 200)
	public void aBatchEscrowsReservesAndFinishes(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, TestForms.RIG, new BlockPos(2, 1, 2), Direction.NORTH);
		power(machine);
		long start = machine.energy().getAmount();
		machine.tanks().input(0).fill(Fluids.WATER, 1000);
		helper.runAfterDelay(3, () -> {
			WorkLane lane = machine.lane(0);
			helper.assertTrue(lane != null && machine.tanks().input(0).millibuckets() == 0, "The water moved into the running batch");
			helper.assertTrue(machine.reservedMb(0) == 500 && machine.reservedMb(1) == 250, "Room for both results is reserved");
			helper.assertTrue(machine.status().state() == MachineLifecycle.WARMING, "A batch from cold warms first: " + machine.status());
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(machine.lane(0) == null, "Still running");
			helper.assertTrue(amount(machine.tanks().output(0), PetroFluids.HYDROGEN.fluid()) == 500
					&& amount(machine.tanks().output(1), PetroFluids.OXYGEN.fluid()) == 250, "Results: 500 hydrogen and 250 oxygen");
			helper.assertTrue(machine.reservedMb(0) == 0 && start - machine.energy().getAmount() == SPLIT_TICKS * RIG_DRAW,
					"Paid " + (start - machine.energy().getAmount()) + " JE, not " + SPLIT_TICKS * RIG_DRAW);
		});
	}

	/**
	 * With coal and water in but no tool, the rig waits for its bed and names it; with the bed in it runs, the bed cannot
	 * be taken out at the screen or through any face meanwhile, and it is still there afterwards.
	 */
	@GameTest(maxTicks = 200)
	public void aToolIsNeededAndHeldWhileItWorks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MachineForm form = TestForms.RIG_FORM;
		FormMachineBlockEntity machine = place(helper, TestForms.RIG, new BlockPos(2, 1, 2), Direction.NORTH);
		power(machine);
		machine.container().setItem(0, new ItemStack(Items.COAL));
		machine.tanks().input(0).fill(Fluids.WATER, 250);
		int socket = form.firstSocketSlot();
		helper.runAfterDelay(3, () -> {
			MachineStatus status = machine.status();
			helper.assertTrue(status.state() == MachineLifecycle.WAITING_TOOL && status.reason() == MachineStatus.Reason.TOOL_REQUIRED
					&& status.index() == 0 && status.subject() == BuiltInRegistries.ITEM.getId(Items.IRON_BLOCK), "Waiting for the bed: " + status);
			helper.assertTrue(machine.lane(0) == null && machine.tanks().input(0).millibuckets() == 250, "Nothing was spent waiting");
			machine.container().setItem(socket, new ItemStack(Items.IRON_BLOCK));
		});
		helper.runAfterDelay(6, () -> {
			helper.assertTrue(machine.lane(0) != null && machine.socketLocked(0), "The batch runs and holds its bed");
			Player player = helper.makeMockPlayer(GameType.SURVIVAL);
			FormMachineMenu menu = (FormMachineMenu) machine.createMenu(0, player.getInventory(), player);
			helper.assertFalse(menu.getSlot(socket).mayPickup(player), "The bed cannot be taken out at the screen while it works");
			BlockPos controller = machine.getBlockPos();
			for (int part = 0; part < form.footprint().size(); part++) {
				for (Direction side : Direction.values()) {
					Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, form.footprint().partPos(controller, Direction.NORTH, part), side);
					if (storage != null) {
						try (Transaction transaction = Transaction.openOuter()) {
							helper.assertTrue(storage.extract(ItemVariant.of(Items.IRON_BLOCK), 1, transaction) == 0,
									"Part " + part + " " + side + " gave up the bed");
						}
					}
				}
			}
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(machine.lane(0) == null, "Still running");
			helper.assertTrue(machine.container().getItem(form.firstOutputSlot()).is(Items.CHARCOAL) && machine.container().getItem(0).isEmpty(),
					"The coal became charcoal");
			helper.assertTrue(machine.container().getItem(socket).is(Items.IRON_BLOCK) && !machine.socketLocked(0), "The bed is still there, free");
			helper.assertTrue(amount(machine.tanks().output(0), PetroFluids.HYDROGEN.fluid()) == 250, "And 250 hydrogen");
		});
	}

	/** The status names the missing amount, an input nothing here uses, and a full or foreign output, spending nothing. */
	@GameTest(maxTicks = 200)
	public void theStatusNamesWhatToFix(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, TestForms.RIG, new BlockPos(2, 1, 2), Direction.NORTH);
		power(machine);
		long start = machine.energy().getAmount();
		FluidTank water = machine.tanks().input(0);
		FluidTank hydrogen = machine.tanks().output(0);
		water.fill(Fluids.WATER, 400);
		helper.runAfterDelay(2, () -> {
			MachineStatus status = machine.status();
			helper.assertTrue(status.state() == MachineLifecycle.WAITING_INPUT && status.reason() == MachineStatus.Reason.MISSING_FLUID
					&& status.index() == 0 && status.amount() == 600 && status.subject() == BuiltInRegistries.FLUID.getId(Fluids.WATER),
					"Needs 600 mB more water: " + status);
			water.drain(400);
			water.fill(Fluids.LAVA, 500);
		});
		helper.runAfterDelay(4, () -> {
			MachineStatus status = machine.status();
			helper.assertTrue(status.reason() == MachineStatus.Reason.UNUSED_FLUID && status.subject() == BuiltInRegistries.FLUID.getId(Fluids.LAVA),
					"Nothing here uses lava: " + status);
			water.drain(500);
			water.fill(Fluids.WATER, 1000);
			hydrogen.fill(PetroFluids.OXYGEN.fluid(), 100);
		});
		helper.runAfterDelay(6, () -> {
			MachineStatus status = machine.status();
			helper.assertTrue(status.state() == MachineLifecycle.OUTPUT_BLOCKED && status.reason() == MachineStatus.Reason.OUTPUT_OTHER_FLUID
					&& status.index() == 1, "The hydrogen tank holds another gas: " + status);
			hydrogen.drain(100);
			hydrogen.fill(PetroFluids.HYDROGEN.fluid(), 1600);
		});
		helper.runAfterDelay(8, () -> {
			MachineStatus status = machine.status();
			helper.assertTrue(status.state() == MachineLifecycle.OUTPUT_BLOCKED && status.reason() == MachineStatus.Reason.OUTPUT_FULL
					&& status.index() == 1, "1,600 + 500 mB would not fit in 2,000: " + status);
			helper.assertTrue(machine.lane(0) == null && water.millibuckets() == 1000 && machine.energy().getAmount() == start,
					"Nothing was taken or paid while blocked");
			hydrogen.drain(100);
		});
		helper.succeedWhen(() -> helper.assertTrue(amount(hydrogen, PetroFluids.HYDROGEN.fluid()) == 2000 && machine.lane(0) == null,
				"With exactly enough room it runs and fills the tank"));
	}

	/** Without power a running batch waits, keeping its escrow and paid progress; with power back it finishes. */
	@GameTest(maxTicks = 200)
	public void withoutPowerABatchWaitsAndKeepsItsWork(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, TestForms.RIG, new BlockPos(2, 1, 2), Direction.NORTH);
		SimpleEnergyStorage energy = (SimpleEnergyStorage) machine.energy();
		energy.setAmount(10 * RIG_DRAW);
		machine.tanks().input(0).fill(Fluids.WATER, 1000);
		helper.runAfterDelay(20, () -> {
			WorkLane lane = machine.lane(0);
			MachineStatus status = machine.status();
			helper.assertTrue(lane != null && lane.progress() == 10, "Ten paid ticks, then it stopped: " + (lane == null ? "no lane" : lane.progress()));
			helper.assertTrue(status.state() == MachineLifecycle.WAITING_ENERGY && status.amount() == RIG_DRAW, "Waiting for power: " + status);
			helper.assertTrue(machine.tanks().input(0).millibuckets() == 0 && machine.reservedMb(0) == 500, "The escrow and reservation hold");
			energy.setAmount(10 * RIG_DRAW);
		});
		helper.succeedWhen(() -> helper.assertTrue(machine.lane(0) == null && energy.getAmount() == 0
				&& amount(machine.tanks().output(0), PetroFluids.HYDROGEN.fluid()) == 500, "It finished on exactly the energy owed"));
	}

	/**
	 * A batch saved while warming loads with the same paid progress, escrow and reservations, and nothing taken twice;
	 * a batch whose recipe no longer exists finishes as it was started.
	 */
	@GameTest(maxTicks = 200)
	public void batchesSurviveSavingAndLoading(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FormMachineBlockEntity machine = place(helper, TestForms.RIG, new BlockPos(2, 1, 2), Direction.NORTH);
		power(machine);
		machine.tanks().input(0).fill(Fluids.WATER, 1000);
		helper.runAfterDelay(4, () -> {
			WorkLane lane = machine.lane(0);
			helper.assertTrue(lane != null && lane.cold() && machine.status().state() == MachineLifecycle.WARMING, "Warming when saved");
			CompoundTag saved = machine.saveWithoutMetadata(level.registryAccess());
			FormMachineBlockEntity copy = new FormMachineBlockEntity(machine.getBlockPos(), machine.getBlockState());
			copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
			WorkLane loaded = copy.lane(0);
			helper.assertTrue(loaded != null && loaded.progress() == lane.progress() && loaded.cold() && loaded.duration() == SPLIT_TICKS,
					"The batch loads with its paid progress");
			helper.assertTrue(copy.tanks().input(0).millibuckets() == 0 && copy.reservedMb(0) == 500 && copy.reservedMb(1) == 250
					&& loaded.fluids().size() == 1 && loaded.fluids().getFirst().mb() == 1000, "Its water is in escrow once, not back in the tank");
			helper.assertTrue(copy.energy().getAmount() == machine.energy().getAmount(), "Energy is saved");

			// A batch of a recipe that has since gone: it finishes with what it was started for.
			TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
			out.putLong("energy", 30_000);
			out.store("lane0", WorkLane.CODEC, new WorkLane(TestForms.id("gone"), List.of(),
					List.of(new WorkLane.FluidPart(0, FluidVariant.of(Fluids.WATER), 1000)), List.of(),
					List.of(new WorkLane.FluidPart(1, FluidVariant.of(PetroFluids.OXYGEN.fluid()), 123)), 10, -1, false, 4));
			FormMachineBlockEntity orphan = place(helper, TestForms.RIG, new BlockPos(5, 1, 2), Direction.NORTH);
			orphan.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
		});
		helper.succeedWhen(() -> {
			FormMachineBlockEntity orphan = helper.getBlockEntity(new BlockPos(5, 1, 2), FormMachineBlockEntity.class);
			helper.assertTrue(orphan.lane(0) == null && amount(orphan.tanks().output(1), PetroFluids.OXYGEN.fluid()) == 123,
					"The batch of a removed recipe finished with its own results");
			helper.assertTrue(machine.lane(0) == null && amount(machine.tanks().output(0), PetroFluids.HYDROGEN.fluid()) == 500,
					"The original batch finished once");
		});
	}

	/**
	 * Cancelling returns a batch's untransformed inputs and no energy; a batch whose inputs no longer fit back keeps
	 * running instead of losing them.
	 */
	@GameTest(maxTicks = 200)
	public void cancellingReturnsOnlyTheUntransformedInputs(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, TestForms.RIG, new BlockPos(2, 1, 2), Direction.NORTH);
		power(machine);
		FluidTank water = machine.tanks().input(0);
		water.fill(Fluids.WATER, 1000);
		helper.runAfterDelay(6, () -> {
			helper.assertTrue(machine.lane(0) != null && water.millibuckets() == 0, "Running");
			machine.togglePause();
			long paid = machine.energy().getAmount();
			int[] result = machine.cancel();
			helper.assertTrue(result[0] == 1 && result[1] == 0 && machine.lane(0) == null, "One batch cancelled");
			helper.assertTrue(amount(water, Fluids.WATER) == 1000 && machine.energy().getAmount() == paid && machine.reservedMb(0) == 0
					&& machine.tanks().output(0).millibuckets() == 0, "The water came back; the energy and the results did not");
			machine.togglePause();
		});
		helper.runAfterDelay(10, () -> {
			helper.assertTrue(machine.lane(0) != null, "Resumed and restarted");
			water.fill(Fluids.LAVA, 500);
			int[] result = machine.cancel();
			helper.assertTrue(result[0] == 0 && result[1] == 1 && machine.lane(0) != null, "With lava in its tank the water cannot go back");
			water.drain(500);
		});
		helper.succeedWhen(() -> helper.assertTrue(machine.lane(0) == null
				&& amount(machine.tanks().output(0), PetroFluids.HYDROGEN.fluid()) == 500, "The kept batch finished"));
	}

	/**
	 * The hall's bulk profile runs a lane per set of inputs, each with its own reservation, at 80 percent of the draw
	 * each (77 JE per tick, rounded up); with room for only three results the fourth set of inputs stays in the tank.
	 */
	@GameTest(structure = ARENA, maxTicks = 200)
	public void bulkLanesRunSideBySideWithTheirOwnRoom(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, TestForms.HALL, new BlockPos(8, 1, 2), Direction.NORTH);
		power(machine);
		long start = machine.energy().getAmount();
		FluidTank hydrogen = machine.tanks().output(0);
		hydrogen.fill(PetroFluids.HYDROGEN.fluid(), 14_500);
		machine.tanks().input(0).fill(Fluids.WATER, 4000);
		helper.runAfterDelay(3, () -> {
			int running = 0;
			for (int lane = 0; lane < 4; lane++) {
				if (machine.lane(lane) != null) {
					running++;
				}
			}
			helper.assertTrue(running == 3 && machine.reservedMb(0) == 1500 && machine.tanks().input(0).millibuckets() == 1000,
					"Three lanes fit their hydrogen; the fourth set of water waits in the tank: " + running + " running");
			helper.assertTrue(machine.status().state().working(), "The machine reports the running lanes: " + machine.status());
		});
		helper.succeedWhen(() -> {
			MachineStatus status = machine.status();
			helper.assertTrue(amount(hydrogen, PetroFluids.HYDROGEN.fluid()) == 16_000 && status.reason() == MachineStatus.Reason.OUTPUT_FULL,
					"Full after three batches, then blocked: " + status);
			helper.assertTrue(start - machine.energy().getAmount() == 3L * SPLIT_TICKS * 77,
					"Three lanes paid 77 JE a tick each: " + (start - machine.energy().getAmount()));
			helper.assertTrue(machine.tanks().input(0).millibuckets() == 1000, "The blocked batch took nothing");
		});
	}

	/**
	 * Anything standing in a clearance stops the machine at once (the part beside it asks for a check), which keeps its
	 * work and resumes once the space is clear; a part replaced by something else unforms it too.
	 */
	@GameTest(maxTicks = 200)
	public void aBlockedClearanceStopsTheMachineUntilCleared(GameTestHelper helper) {
		FormMachineBlockEntity machine = place(helper, TestForms.RIG, new BlockPos(2, 1, 2), Direction.NORTH);
		power(machine);
		machine.tanks().input(0).fill(Fluids.WATER, 1000);
		BlockPos clearance = new BlockPos(2, 3, 2);
		helper.runAfterDelay(5, () -> helper.setBlock(clearance, Blocks.STONE));
		int[] frozen = new int[1];
		helper.runAfterDelay(8, () -> {
			MachineStatus status = machine.status();
			helper.assertTrue(status.state() == MachineLifecycle.UNFORMED && status.reason() == MachineStatus.Reason.CLEARANCE_BLOCKED
					&& status.index() == TestForms.RIG_FORM.clearanceCells()[0], "Unformed by the stone: " + status);
			helper.assertTrue(machine.lane(0) != null, "The batch is kept");
			frozen[0] = machine.lane(0).progress();
		});
		helper.runAfterDelay(12, () -> {
			helper.assertTrue(machine.lane(0).progress() == frozen[0], "No work while unformed");
			helper.setBlock(clearance, Blocks.AIR);
			BlockPos part = helper.absolutePos(new BlockPos(1, 2, 3));
			BlockState state = helper.getLevel().getBlockState(part);
			helper.assertTrue(machine.checkStructure(helper.getLevel(), machine.getBlockPos(), machine.getBlockState()) == null, "Formed again");
			// Another part's number (never the controller's, which would grow a second block entity).
			int number = state.getValue(TestForms.RIG.partProperty());
			helper.getLevel().setBlock(part, state.setValue(TestForms.RIG.partProperty(), number % 7 + 1), 2);
			MachineStatus broken = machine.checkStructure(helper.getLevel(), machine.getBlockPos(), machine.getBlockState());
			helper.assertTrue(broken != null && broken.reason() == MachineStatus.Reason.PART_MISSING, "A wrong part unforms it: " + broken);
			helper.getLevel().setBlock(part, state, 2);
		});
		helper.succeedWhen(() -> helper.assertTrue(machine.lane(0) == null
				&& amount(machine.tanks().output(0), PetroFluids.HYDROGEN.fluid()) == 500, "Finished once the clearance was free"));
	}

	/** Breaking a machine mid-batch drops its tool and its escrowed input once, and makes nothing. */
	@GameTest(maxTicks = 100)
	public void breakingMidBatchDropsTheEscrowOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FormMachineBlockEntity machine = place(helper, TestForms.RIG, new BlockPos(2, 1, 2), Direction.NORTH);
		power(machine);
		machine.container().setItem(0, new ItemStack(Items.COAL));
		machine.container().setItem(TestForms.RIG_FORM.firstSocketSlot(), new ItemStack(Items.IRON_BLOCK));
		machine.tanks().input(0).fill(Fluids.WATER, 250);
		helper.runAfterDelay(4, () -> {
			helper.assertTrue(machine.lane(0) != null && machine.container().getItem(0).isEmpty(), "The coal is in escrow");
			level.destroyBlock(helper.absolutePos(new BlockPos(1, 2, 3)), true);
			List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(machine.getBlockPos()).inflate(4));
			helper.assertTrue(count(drops, TestForms.RIG.asItem()) == 1 && count(drops, Items.COAL) == 1 && count(drops, Items.IRON_BLOCK) == 1
					&& count(drops, Items.CHARCOAL) == 0, "One rig, its coal and its bed, and no charcoal");
			helper.succeed();
		});
	}

	/**
	 * The original machines are untouched: a chemical reactor never runs the form-only test recipes, and tanks saved by an
	 * earlier layout load into the tanks with their roles, so a new input can never receive an old output.
	 */
	@GameTest(maxTicks = 100)
	public void originalMachinesKeepTheirRecipesAndSaves(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		LargeMachineBlock reactorBlock = (LargeMachineBlock) JugcraftMachines.MACHINES.get(MachineKind.CHEMICAL_REACTOR);
		BlockPos reactorPos = new BlockPos(2, 1, 2);
		helper.setBlock(reactorPos, reactorBlock.defaultBlockState());
		reactorBlock.setPlacedBy(level, helper.absolutePos(reactorPos), helper.getBlockState(reactorPos), null, ItemStack.EMPTY);
		MachineBlockEntity reactor = helper.getBlockEntity(reactorPos, MachineBlockEntity.class);
		((SimpleEnergyStorage) reactor.energyFor(null)).setAmount(MachineKind.CHEMICAL_REACTOR.capacity);
		reactor.tanks().input(0).fill(Fluids.WATER, 1000);

		// Layout 0 had one input and one output; layout 1 has three inputs and two outputs.
		FluidMachineSpec spec = new FluidMachineSpec(List.of(1000, 1000, 1000), List.of(1000, 1000), 0, 0).migratedFrom(0, 1, 0, 3);
		helper.assertTrue(spec.layout() == 1, "Migrating raises the layout");
		TagValueOutput old = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		old.store("tank0_fluid", FluidVariant.CODEC, FluidVariant.of(Fluids.WATER));
		old.putLong("tank0_amount", 500 * FluidConstants.BUCKET / 1000);
		old.store("tank1_fluid", FluidVariant.CODEC, FluidVariant.of(PetroFluids.SULFURIC_ACID.source()));
		old.putLong("tank1_amount", 300 * FluidConstants.BUCKET / 1000);
		FluidTanks tanks = new FluidTanks(spec, (tank, variant) -> true, () -> { });
		tanks.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), old.buildResult()));
		helper.assertTrue(amount(tanks.input(0), Fluids.WATER) == 500 && amount(tanks.output(0), PetroFluids.SULFURIC_ACID.source()) == 300
				&& tanks.input(1).millibuckets() == 0 && tanks.input(2).millibuckets() == 0 && tanks.output(1).millibuckets() == 0,
				"The old input loads into input 0 and the old output into output 0");
		TagValueOutput again = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		tanks.save(again);
		CompoundTag resaved = again.buildResult();
		FluidTanks reloaded = new FluidTanks(spec, (tank, variant) -> true, () -> { });
		reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), resaved));
		helper.assertTrue(resaved.getIntOr(FluidTanks.LAYOUT_KEY, 0) == 1 && amount(reloaded.output(0), PetroFluids.SULFURIC_ACID.source()) == 300,
				"Saved again with its layout, it loads by position");
		rejects(helper, "an old input mapped to an output", () -> {
			new FluidMachineSpec(List.of(1000), List.of(1000), 0, 0).migratedFrom(0, 1, 1);
			return null;
		});

		helper.runAfterDelay(60, () -> {
			helper.assertTrue(reactor.tanks().input(0).millibuckets() == 1000 && reactor.tanks().output(0).millibuckets() == 0,
					"The reactor never ran the form-only recipe");
			CompoundTag saved = reactor.saveWithoutMetadata(level.registryAccess());
			helper.assertFalse(saved.contains(FluidTanks.LAYOUT_KEY), "First-release layouts save exactly as before");
			MachineBlockEntity copy = new MachineBlockEntity(reactor.getBlockPos(), reactor.getBlockState());
			copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
			helper.assertTrue(amount(copy.tanks().input(0), Fluids.WATER) == 1000, "And load exactly as before");
			helper.succeed();
		});
	}

	// ------------------------------------------------------------------ helpers

	private static MachineForm.Builder bad() {
		return MachineForm.builder(TestForms.id("bad"), MachineKind.CHEMICAL_REACTOR).capability(TestForms.CAPABILITY);
	}

	/** A valid little generator form: a gas turbine's figures, hydrogen in on the left, power out at the back. */
	private static MachineForm.Builder generator() {
		return MachineForm.builder(TestForms.id("bad"), MachineKind.GAS_TURBINE).layer("C#", "##").layer("##", "##").inputTank("fuel")
				.fuel(Jugcraft.id("hydrogen"), 128, 128).port("in", FormPort.Kind.FLUID_IN, 0, 0, 0, 0, FormSide.LEFT)
				.port("out", FormPort.Kind.ENERGY_OUT, 0, 0, 1, 0, FormSide.BACK);
	}

	private static void rejects(GameTestHelper helper, String what, Supplier<?> build) {
		boolean refused = false;
		try {
			build.get();
		} catch (IllegalArgumentException expected) {
			refused = true;
		}
		helper.assertTrue(refused, "Expected " + what + " to be refused");
	}

	/** Places a whole form as its item would (all parts), facing {@code facing}, with its controller at {@code relative}. */
	private static FormMachineBlockEntity place(GameTestHelper helper, FormMachineBlock block, BlockPos relative, Direction facing) {
		helper.setBlock(relative, block.defaultBlockState().setValue(MachineBlock.FACING, facing));
		block.setPlacedBy(helper.getLevel(), helper.absolutePos(relative), helper.getBlockState(relative), null, ItemStack.EMPTY);
		return helper.getBlockEntity(relative, FormMachineBlockEntity.class);
	}

	private static void power(FormMachineBlockEntity machine) {
		((SimpleEnergyStorage) machine.energy()).setAmount(machine.energy().getCapacity());
	}

	/** Millibuckets of {@code fluid} in a tank (0 when it holds anything else). */
	private static int amount(FluidTank tank, net.minecraft.world.level.material.Fluid fluid) {
		return !tank.variant.isBlank() && tank.variant.isOf(fluid) ? tank.millibuckets() : 0;
	}

	private static int count(List<ItemEntity> drops, Item item) {
		int total = 0;
		for (ItemEntity drop : drops) {
			if (drop.getItem().is(item)) {
				total += drop.getItem().getCount();
			}
		}
		return total;
	}

	private static int rigBlocks(GameTestHelper helper) {
		int found = 0;
		for (int x = 0; x < 8; x++) {
			for (int y = 1; y < 6; y++) {
				for (int z = 0; z < 8; z++) {
					if (helper.getBlockState(new BlockPos(x, y, z)).is(TestForms.RIG)) {
						found++;
					}
				}
			}
		}
		return found;
	}

	private static BlockHitResult hit(GameTestHelper helper, BlockPos relative) {
		BlockPos absolute = helper.absolutePos(relative);
		return new BlockHitResult(Vec3.atCenterOf(absolute).add(0.0, 0.5, 0.0), Direction.UP, absolute, false);
	}
}
