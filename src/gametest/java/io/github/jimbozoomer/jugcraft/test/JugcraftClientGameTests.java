package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.client.HandbookScreen;
import io.github.jimbozoomer.jugcraft.client.MachineScreen;
import io.github.jimbozoomer.jugcraft.client.ProspectorScreen;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids;
import io.github.jimbozoomer.jugcraft.kinetic.BeltPulleyBlockEntity;
import io.github.jimbozoomer.jugcraft.kinetic.DynamoBlock;
import io.github.jimbozoomer.jugcraft.kinetic.ElectricMotorBlock;
import io.github.jimbozoomer.jugcraft.kinetic.ElectricMotorBlockEntity;
import io.github.jimbozoomer.jugcraft.kinetic.HandCrankBlock;
import io.github.jimbozoomer.jugcraft.kinetic.JugcraftKinetics;
import io.github.jimbozoomer.jugcraft.kinetic.ShaftBlock;
import io.github.jimbozoomer.jugcraft.kinetic.SteamEngineBlock;
import io.github.jimbozoomer.jugcraft.logistics.ConveyorBlock;
import io.github.jimbozoomer.jugcraft.logistics.ConveyorBlockEntity;
import io.github.jimbozoomer.jugcraft.logistics.ConveyorSlopeBlock;
import io.github.jimbozoomer.jugcraft.logistics.JugcraftLogistics;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.machine.LargeMachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.prospecting.OreSurvey;
import io.github.jimbozoomer.jugcraft.tools.ChargingStationBlock;
import io.github.jimbozoomer.jugcraft.tools.ChargingStationBlockEntity;
import io.github.jimbozoomer.jugcraft.tools.JugcraftTools;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Client game tests: a real game client with real rendering (CI runs it with Mesa). It builds a
 * showroom of every machine, opens a machine screen and the Engineer's Handbook, and saves
 * screenshots, so the looks can be checked from actual game renders rather than previews.
 */
public class JugcraftClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			int x = origin.getX();
			int y = origin.getY();
			int z = origin.getZ();
			TestServerContext server = singleplayer.getServer();
			// No command feedback in chat, so it does not cover the screenshots (the rule's name differs across
			// versions; whichever does not exist just fails).
			server.runCommand("gamerule sendCommandFeedback false");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			// A floor under the showroom and the scenes, with the air above it cleared. Set block by block on the
			// server: a fill command cannot reach chunks that are not loaded yet, and the multi-block row runs far
			// beyond the player.
			int end = x + 20 + largeRowLength() + 8;
			server.runOnServer(minecraft -> clearFloor(minecraft.overworld(), new BlockPos(x - 26, y, z - 10), new BlockPos(end, y + 14, z + 12)));
			server.runOnServer(minecraft -> buildShowroom(minecraft.overworld(), new BlockPos(x, y, z - 5)));

			// Hide the HUD, hand and chat so the screenshots show only the machines. In 26.3 this is the "toggle GUI"
			// key mapping (F1 by default); pressing the raw F1 key code did not reach it.
			context.getInput().pressKey(options -> options.keyToggleGui);

			// One-block machines, facing the camera, in two halves.
			server.runCommand("tp @p %d %d %d 180 25".formatted(x - 4, y, z - 2));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_machines_1");
			server.runCommand("tp @p %d %d %d 180 25".formatted(x + 3, y, z - 2));
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_machines_2");

			// A running kinetic line: steam engine, shafts, a gearbox with a hand crank, a dynamo and a crusher.
			// Its own row behind the multi-block camera (the showroom rows are at z - 5).
			server.runOnServer(minecraft -> buildKineticLine(minecraft.overworld(), new BlockPos(x, y, z + 2)));
			server.runCommand("tp @p %d %d %d 180 35".formatted(x + 2, y + 2, z + 7));
			context.waitTicks(60);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_kinetics");

			// An electric motor driving a pulley, belted up to a second pulley that drives a crusher through a shaft.
			server.runOnServer(minecraft -> buildBeltLine(minecraft.overworld(), new BlockPos(x + 12, y, z + 2)));
			server.runCommand("tp @p %d %d %d 180 5".formatted(x + 14, y + 1, z + 7));
			context.waitTicks(60);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_belts");

			// A conveyor line with items riding it, a splitter at the end and a chest on each of its outputs.
			server.runOnServer(minecraft -> buildConveyorLine(minecraft.overworld(), new BlockPos(x + 6, y, z + 2)));
			server.runCommand("tp @p %d %d %d 180 40".formatted(x + 8, y + 2, z + 5));
			context.waitTicks(10);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_conveyors");

			// Conveyor slopes: items climb onto a raised conveyor and come back down.
			server.runOnServer(minecraft -> buildSlopeLine(minecraft.overworld(), new BlockPos(x + 5, y, z + 4)));
			server.runCommand("tp @p %d %d %d 150 25".formatted(x + 11, y + 2, z + 8));
			context.waitTicks(12);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_conveyor_slopes");

			// Powered tools: three charging stations holding the drill, the chainsaw and the rocket pack.
			server.runOnServer(minecraft -> buildToolStations(minecraft.overworld(), new BlockPos(x - 8, y, z + 2)));
			server.runCommand("tp @p %d %d %d 180 10".formatted(x - 6, y, z + 6));
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_charging_stations");

			// The drill in hand (first person, with the hotbar showing the three tools), then the worn rocket pack
			// from behind.
			server.runCommand("item replace entity @p hotbar.0 with jugcraft:mining_drill");
			server.runCommand("item replace entity @p hotbar.1 with jugcraft:chainsaw");
			server.runCommand("item replace entity @p hotbar.2 with jugcraft:rocket_pack");
			server.runCommand("item replace entity @p armor.chest with jugcraft:rocket_pack");
			server.runCommand("tp @p %d %d %d 180 20".formatted(x - 6, y, z + 5));
			// Show the HUD again (the first hotbar slot, the drill, is selected in a new world).
			context.getInput().pressKey(options -> options.keyToggleGui);
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_drill_in_hand");
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_rocket_pack_worn");
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			context.getInput().pressKey(options -> options.keyToggleGui);
			server.runCommand("clear @p");

			// Power gear in the electric look: a line of glowing cables from a solar panel through a battery box, a
			// capacitor bank and a charging station to an electric motor turning a dynamo, and an electric pump.
			server.runOnServer(minecraft -> buildPowerGear(minecraft.overworld(), new BlockPos(x - 24, y, z + 2)));
			server.runCommand("tp @p %d %d %d 180 18".formatted(x - 17, y + 2, z + 9));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_power_gear");

			// Multi-block machines, ten blocks away, in views twelve blocks apart along the row (the wind turbine is
			// nine tall; the oil machines are at the far end).
			int views = (largeRowLength() + 11) / 12;
			for (int view = 0; view < views; view++) {
				server.runCommand("tp @p %d %d %d 180 8".formatted(x + 24 + view * 12, y + 3, z + 5));
				context.waitTicks(20);
				singleplayer.getConnection().waitForChunksRender();
				context.takeScreenshot("jugcraft_multiblocks_" + (view + 1));
			}

			// A machine screen: walk up to the crusher and use it.
			BlockPos crusher = new BlockPos(x - 7 + singleIndex(MachineKind.CRUSHER), y, z - 5);
			server.runCommand("tp @p %d %d %d 180 30".formatted(crusher.getX(), y, z - 3));
			context.waitTicks(10);
			context.getInput().lookAt(crusher);
			context.waitTick();
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(MachineScreen.class);
			context.takeScreenshot("jugcraft_machine_screen");
			context.setScreen(() -> null);

			// One screen in each of the other two themes (batch 22): the electric battery box and the lab's circuit
			// assembler.
			for (MachineKind kind : List.of(MachineKind.BATTERY_BOX, MachineKind.CIRCUIT_ASSEMBLER)) {
				BlockPos machine = new BlockPos(x - 7 + singleIndex(kind), y, z - 5);
				server.runCommand("tp @p %d %d %d 180 30".formatted(machine.getX(), y, z - 3));
				context.waitTicks(10);
				context.getInput().lookAt(machine);
				context.waitTick();
				context.getInput().pressKey(options -> options.keyUse);
				context.waitForScreen(MachineScreen.class);
				context.waitTicks(25);
				context.takeScreenshot("jugcraft_machine_screen_" + kind.id);
				context.setScreen(() -> null);
			}

			// The auto-crafter's screen, with a stick pattern in its grid.
			BlockPos crafter = new BlockPos(x - 7 + singleIndex(MachineKind.AUTO_CRAFTER), y, z - 5);
			server.runOnServer(minecraft -> {
				if (minecraft.overworld().getBlockEntity(crafter) instanceof MachineBlockEntity machine) {
					machine.setItem(0, new ItemStack(Items.OAK_PLANKS, 12));
					machine.setItem(3, new ItemStack(Items.OAK_PLANKS, 12));
				}
			});
			server.runCommand("tp @p %d %d %d 180 30".formatted(crafter.getX(), y, z - 3));
			context.waitTicks(10);
			context.getInput().lookAt(crafter);
			context.waitTick();
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(MachineScreen.class);
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_auto_crafter_screen");
			context.setScreen(() -> null);

			// The Engineer's Handbook: the first page and the crusher's page.
			context.setScreen(HandbookScreen::new);
			context.waitTicks(2);
			context.takeScreenshot("jugcraft_handbook");
			// The Progression chapter's first stage, and a machine page (Processing: the crusher).
			context.setScreen(() -> new HandbookScreen(0, 1));
			context.waitTicks(2);
			context.takeScreenshot("jugcraft_handbook_progression");
			context.setScreen(() -> new HandbookScreen(4, 1));
			context.waitTicks(2);
			context.takeScreenshot("jugcraft_handbook_machine_page");
			context.setScreen(() -> null);

			// The prospector: a real survey of the superflat ground under the showroom (it is only a few
			// blocks deep), with ores buried in its dirt so the screen has readings to show; wait for the valve-tube bars to warm up.
			server.runCommand("fill %d %d %d %d %d %d minecraft:iron_ore".formatted(x - 12, y - 3, z - 12, x + 12, y - 3, z + 12));
			server.runCommand("fill %d %d %d %d %d %d minecraft:copper_ore".formatted(x - 6, y - 2, z - 6, x + 6, y - 2, z + 6));
			server.runCommand("setblock %d %d %d minecraft:diamond_ore".formatted(x + 1, y - 2, z + 1));
			AtomicReference<List<OreSurvey.Reading>> readings = new AtomicReference<>(List.of());
			server.runOnServer(minecraft -> readings.set(OreSurvey.survey(minecraft.overworld(), new BlockPos(x, y, z),
					minecraft.overworld().getRandom())));
			context.setScreen(() -> new ProspectorScreen(readings.get()));
			context.waitTicks(60);
			context.takeScreenshot("jugcraft_prospector");
			context.setScreen(() -> null);
		}
	}

	/**
	 * Engine at {@code start} facing west (its back drives east), two shafts, a gearbox with a hand crank
	 * on top, a dynamo beyond it and a crusher on the gearbox's north side (behind it, seen from the camera).
	 */
	private static void buildKineticLine(ServerLevel level, BlockPos start) {
		level.setBlock(start.below(), Blocks.WATER.defaultBlockState(), 3);
		level.setBlock(start, JugcraftKinetics.STEAM_ENGINE.defaultBlockState().setValue(SteamEngineBlock.FACING, Direction.WEST), 3);
		Storage<ItemVariant> fuel = ItemStorage.SIDED.find(level, start, Direction.UP);
		try (Transaction transaction = Transaction.openOuter()) {
			fuel.insert(ItemVariant.of(Items.COAL), 8, transaction);
			transaction.commit();
		}
		BlockState shaft = JugcraftKinetics.IRON_SHAFT.defaultBlockState().setValue(ShaftBlock.AXIS, Direction.Axis.X);
		level.setBlock(start.east(), shaft, 3);
		level.setBlock(start.east(2), shaft, 3);
		BlockPos gearbox = start.east(3);
		level.setBlock(gearbox, JugcraftKinetics.BRASS_GEARBOX.defaultBlockState(), 3);
		level.setBlock(gearbox.above(), JugcraftKinetics.HAND_CRANK.defaultBlockState().setValue(HandCrankBlock.FACING, Direction.DOWN), 3);
		level.setBlock(gearbox.east(), JugcraftKinetics.DYNAMO.defaultBlockState().setValue(DynamoBlock.FACING, Direction.SOUTH), 3);
		level.setBlock(gearbox.north(), JugcraftMachines.MACHINES.get(MachineKind.CRUSHER).defaultBlockState()
				.setValue(MachineBlock.FACING, Direction.SOUTH), 3);
	}

	/**
	 * A charged electric motor facing east into a pulley; that pulley is belted to one three blocks above it, which
	 * drives a crusher through a shaft. All run along the x axis.
	 */
	private static void buildBeltLine(ServerLevel level, BlockPos start) {
		level.setBlock(start, JugcraftKinetics.ELECTRIC_MOTOR.defaultBlockState().setValue(ElectricMotorBlock.FACING, Direction.EAST), 3);
		if (level.getBlockEntity(start) instanceof ElectricMotorBlockEntity motor) {
			motor.energy().setAmount(ElectricMotorBlockEntity.CAPACITY);
		}
		BlockState pulley = JugcraftKinetics.BELT_PULLEY.defaultBlockState().setValue(ShaftBlock.AXIS, Direction.Axis.X);
		BlockPos lower = start.east();
		BlockPos upper = lower.above(3);
		level.setBlock(lower, pulley, 3);
		level.setBlock(upper, pulley, 3);
		BeltPulleyBlockEntity.connect(level, lower, upper);
		level.setBlock(upper.east(), JugcraftKinetics.IRON_SHAFT.defaultBlockState().setValue(ShaftBlock.AXIS, Direction.Axis.X), 3);
		level.setBlock(upper.east(2), JugcraftMachines.MACHINES.get(MachineKind.CRUSHER).defaultBlockState()
				.setValue(MachineBlock.FACING, Direction.SOUTH), 3);
	}

	/**
	 * Three conveyors running east with items on them, into a splitter with chests on its three outputs; a charged
	 * motor at the back drives them all.
	 */
	private static void buildConveyorLine(ServerLevel level, BlockPos start) {
		ItemStack[] cargo = {new ItemStack(Items.IRON_INGOT), new ItemStack(Items.COBBLESTONE), new ItemStack(Items.COAL),
				new ItemStack(Items.OAK_LOG)};
		for (int i = 1; i <= 4; i++) {
			BlockPos pos = start.east(i);
			level.setBlock(pos, (i == 4 ? JugcraftLogistics.CONVEYOR_SPLITTER : JugcraftLogistics.CONVEYOR).defaultBlockState()
					.setValue(ConveyorBlock.FACING, Direction.EAST), 3);
			if (i < 4 && level.getBlockEntity(pos) instanceof ConveyorBlockEntity conveyor) {
				for (int slot = 0; slot < 3; slot++) {
					conveyor.accept(cargo[(i + slot) % cargo.length].copy(), slot * 0.3F);
				}
			}
		}
		BlockPos splitter = start.east(4);
		for (BlockPos chest : new BlockPos[] {splitter.north(), splitter.east(), splitter.south()}) {
			level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 3);
		}
		level.setBlock(start, JugcraftKinetics.ELECTRIC_MOTOR.defaultBlockState().setValue(ElectricMotorBlock.FACING, Direction.EAST), 3);
		if (level.getBlockEntity(start) instanceof ElectricMotorBlockEntity motor) {
			motor.energy().setAmount(ElectricMotorBlockEntity.CAPACITY);
		}
	}

	/**
	 * A motor driving a conveyor east into an up slope, a raised conveyor, a down slope and a chest, with items on the
	 * first conveyor and both slopes.
	 */
	private static void buildSlopeLine(ServerLevel level, BlockPos start) {
		BlockState flat = JugcraftLogistics.CONVEYOR.defaultBlockState().setValue(ConveyorBlock.FACING, Direction.EAST);
		BlockState slope = JugcraftLogistics.CONVEYOR_SLOPE.defaultBlockState().setValue(ConveyorBlock.FACING, Direction.EAST);
		level.setBlock(start.east(1), flat, 3);
		level.setBlock(start.east(2), slope.setValue(ConveyorSlopeBlock.ASCENDING, true), 3);
		level.setBlock(start.east(3).above(), flat, 3);
		level.setBlock(start.east(4), slope.setValue(ConveyorSlopeBlock.ASCENDING, false), 3);
		level.setBlock(start.east(5), Blocks.CHEST.defaultBlockState(), 3);
		ItemStack[] cargo = {new ItemStack(Items.IRON_INGOT), new ItemStack(Items.OAK_LOG), new ItemStack(Items.COAL)};
		for (BlockPos pos : new BlockPos[] {start.east(1), start.east(2), start.east(3).above(), start.east(4)}) {
			if (level.getBlockEntity(pos) instanceof ConveyorBlockEntity conveyor) {
				for (int i = 0; i < 2; i++) {
					conveyor.accept(cargo[(pos.getX() + i) % cargo.length].copy(), 0.1F + i * 0.45F);
				}
			}
		}
		level.setBlock(start, JugcraftKinetics.ELECTRIC_MOTOR.defaultBlockState().setValue(ElectricMotorBlock.FACING, Direction.EAST), 3);
		if (level.getBlockEntity(start) instanceof ElectricMotorBlockEntity motor) {
			motor.energy().setAmount(ElectricMotorBlockEntity.CAPACITY);
		}
	}

	/** Three charging stations in a row, facing south, holding the mining drill, the chainsaw and the rocket pack. */
	private static void buildToolStations(ServerLevel level, BlockPos start) {
		Item[] tools = {JugcraftTools.MINING_DRILL, JugcraftTools.CHAINSAW, JugcraftTools.ROCKET_PACK};
		for (int i = 0; i < tools.length; i++) {
			BlockPos lower = start.east(i * 2);
			BlockState state = JugcraftTools.CHARGING_STATION.defaultBlockState().setValue(ChargingStationBlock.FACING, Direction.SOUTH);
			level.setBlock(lower, state, 3);
			level.setBlock(lower.above(), state.setValue(ChargingStationBlock.HALF, DoubleBlockHalf.UPPER), 3);
			if (level.getBlockEntity(lower) instanceof ChargingStationBlockEntity station) {
				station.setTool(new ItemStack(tools[i]));
			}
		}
	}

	/** Position of a one-block machine in the showroom row (multi-block machines skipped). */
	private static int singleIndex(MachineKind target) {
		int index = 0;
		for (MachineKind kind : MachineKind.values()) {
			if (kind == target) {
				return index;
			}
			if (!kind.isLarge()) {
				index++;
			}
		}
		throw new IllegalArgumentException(target.id);
	}

	/** Smooth stone one below {@code from}'s level across the area, and air from there up to {@code to}. */
	private static void clearFloor(ServerLevel level, BlockPos from, BlockPos to) {
		BlockState floor = Blocks.SMOOTH_STONE.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();
		for (int bx = from.getX(); bx <= to.getX(); bx++) {
			for (int bz = from.getZ(); bz <= to.getZ(); bz++) {
				level.setBlock(new BlockPos(bx, from.getY() - 1, bz), floor, 2);
				for (int by = from.getY(); by <= to.getY(); by++) {
					BlockPos pos = new BlockPos(bx, by, bz);
					if (!level.getBlockState(pos).isAir()) {
						level.setBlock(pos, air, 2);
					}
				}
			}
		}
	}

	/** Blocks a multi-block machine takes across its front (it extends to its right, +x when facing south). */
	private static int width(MachineKind kind) {
		return 1 + kind.footprint().offsets().stream().mapToInt(offset -> Math.abs(offset.getX())).max().orElse(0);
	}

	/** Length of the multi-block row: each machine's width plus a two-block gap. */
	private static int largeRowLength() {
		int length = 0;
		for (MachineKind kind : MachineKind.values()) {
			if (kind.isLarge()) {
				length += width(kind) + 2;
			}
		}
		return length;
	}

	/**
	 * Power gear in the electric look, along x from {@code start}: a solar panel, a charged battery box, a capacitor
	 * bank and a charging station holding a drill, joined by copper, silver and aluminum cables (one rising over the
	 * bank), then a charged electric motor turning a shaft into a dynamo, and an electric pump. All face south.
	 */
	private static void buildPowerGear(ServerLevel level, BlockPos start) {
		level.setBlock(start, JugcraftMachines.MACHINES.get(MachineKind.SOLAR_PANEL).defaultBlockState()
				.setValue(MachineBlock.FACING, Direction.SOUTH), 3);
		BlockPos battery = start.east(2);
		level.setBlock(battery, JugcraftMachines.MACHINES.get(MachineKind.BATTERY_BOX).defaultBlockState()
				.setValue(MachineBlock.FACING, Direction.SOUTH), 3);
		if (level.getBlockEntity(battery) instanceof MachineBlockEntity box && box.energyFor(null) instanceof SimpleEnergyStorage energy) {
			energy.setAmount(energy.getCapacity());
		}
		BlockPos bank = start.east(4);
		MachineBlock bankBlock = JugcraftMachines.MACHINES.get(MachineKind.CAPACITOR_BANK);
		BlockState bankState = bankBlock.defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH);
		level.setBlock(bank, bankState, 3);
		((LargeMachineBlock) bankBlock).setPlacedBy(level, bank, bankState, null, ItemStack.EMPTY);
		BlockPos station = start.east(7);
		BlockState stationState = JugcraftTools.CHARGING_STATION.defaultBlockState().setValue(ChargingStationBlock.FACING, Direction.SOUTH);
		level.setBlock(station, stationState, 3);
		level.setBlock(station.above(), stationState.setValue(ChargingStationBlock.HALF, DoubleBlockHalf.UPPER), 3);
		if (level.getBlockEntity(station) instanceof ChargingStationBlockEntity holder) {
			holder.setTool(new ItemStack(JugcraftTools.MINING_DRILL));
		}
		BlockPos motor = start.east(9);
		level.setBlock(motor, JugcraftKinetics.ELECTRIC_MOTOR.defaultBlockState().setValue(ElectricMotorBlock.FACING, Direction.EAST), 3);
		if (level.getBlockEntity(motor) instanceof ElectricMotorBlockEntity motorEntity) {
			motorEntity.energy().setAmount(ElectricMotorBlockEntity.CAPACITY);
		}
		level.setBlock(motor.east(), JugcraftKinetics.IRON_SHAFT.defaultBlockState().setValue(ShaftBlock.AXIS, Direction.Axis.X), 3);
		level.setBlock(motor.east(2), JugcraftKinetics.DYNAMO.defaultBlockState().setValue(DynamoBlock.FACING, Direction.SOUTH), 3);
		level.setBlock(start.east(13), JugcraftFluids.ELECTRIC_PUMP.defaultBlockState(), 3);
		// Cables last: copper over the battery, silver across the top of the bank, aluminum to the rest.
		List<BlockPos> copper = List.of(start.east(), start.east(3), start.east(3).above(), start.east(3).above(2));
		List<BlockPos> silver = List.of(start.east(4).above(2), start.east(5).above(2), start.east(6).above(2), start.east(6).above());
		List<BlockPos> aluminum = List.of(start.east(6), start.east(8), start.east(12));
		placeCables(level, JugcraftMachines.COPPER_CABLE, copper);
		placeCables(level, JugcraftMachines.SILVER_CABLE, silver);
		placeCables(level, JugcraftMachines.ALUMINUM_CABLE, aluminum);
		for (List<BlockPos> line : List.of(copper, silver, aluminum)) {
			for (BlockPos pos : line) {
				level.setBlock(pos, Block.updateFromNeighbourShapes(level.getBlockState(pos), level, pos), 3);
			}
		}
	}

	private static void placeCables(ServerLevel level, Block cable, List<BlockPos> positions) {
		for (BlockPos pos : positions) {
			level.setBlock(pos, cable.defaultBlockState(), 3);
		}
	}

	/**
	 * One-block machines side by side (in MachineKind order, from x - 7), all facing south towards the
	 * camera; multi-block machines in a second group 20 blocks east, each its own width plus two apart.
	 */
	private static void buildShowroom(ServerLevel level, BlockPos row) {
		int large = 20;
		for (MachineKind kind : MachineKind.values()) {
			MachineBlock block = JugcraftMachines.MACHINES.get(kind);
			BlockState state = block.defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH);
			if (kind.isLarge()) {
				BlockPos pos = row.offset(large, 0, 0);
				level.setBlock(pos, state, 3);
				((LargeMachineBlock) block).setPlacedBy(level, pos, state, null, ItemStack.EMPTY);
				large += width(kind) + 2;
			} else {
				level.setBlock(row.offset(-7 + singleIndex(kind), 0, 0), state, 3);
			}
		}
	}
}
