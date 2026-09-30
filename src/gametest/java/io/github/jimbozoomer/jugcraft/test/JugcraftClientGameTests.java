package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.client.HandbookScreen;
import io.github.jimbozoomer.jugcraft.client.MachineScreen;
import io.github.jimbozoomer.jugcraft.client.ProspectorScreen;
import io.github.jimbozoomer.jugcraft.kinetic.BeltPulleyBlockEntity;
import io.github.jimbozoomer.jugcraft.kinetic.DynamoBlock;
import io.github.jimbozoomer.jugcraft.kinetic.ElectricMotorBlock;
import io.github.jimbozoomer.jugcraft.kinetic.ElectricMotorBlockEntity;
import io.github.jimbozoomer.jugcraft.kinetic.HandCrankBlock;
import io.github.jimbozoomer.jugcraft.kinetic.JugcraftKinetics;
import io.github.jimbozoomer.jugcraft.kinetic.ShaftBlock;
import io.github.jimbozoomer.jugcraft.kinetic.SteamEngineBlock;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.machine.LargeMachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.prospecting.OreSurvey;
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
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

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
			server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x - 10, y - 1, z - 10, x + 64, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 10, y, z - 10, x + 64, y + 14, z + 8));
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

			// Multi-block machines, ten blocks away, in three views along the row (the wind turbine is nine tall).
			for (int view = 0; view < 3; view++) {
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
			context.setScreen(() -> new HandbookScreen(3, 1));
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

	/**
	 * One-block machines side by side (in MachineKind order, from x - 7), all facing south towards the
	 * camera; multi-block machines in a second group 20 blocks east, four apart.
	 */
	private static void buildShowroom(ServerLevel level, BlockPos row) {
		int large = 0;
		for (MachineKind kind : MachineKind.values()) {
			MachineBlock block = JugcraftMachines.MACHINES.get(kind);
			BlockState state = block.defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH);
			if (kind.isLarge()) {
				BlockPos pos = row.offset(20 + large * 4, 0, 0);
				level.setBlock(pos, state, 3);
				((LargeMachineBlock) block).setPlacedBy(level, pos, state, null, ItemStack.EMPTY);
				large++;
			} else {
				level.setBlock(row.offset(-7 + singleIndex(kind), 0, 0), state, 3);
			}
		}
	}
}
