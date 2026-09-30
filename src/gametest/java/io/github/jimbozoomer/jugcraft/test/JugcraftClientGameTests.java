package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.client.HandbookScreen;
import io.github.jimbozoomer.jugcraft.client.MachineScreen;
import io.github.jimbozoomer.jugcraft.client.ProspectorScreen;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.machine.LargeMachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.prospecting.OreSurvey;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client game tests: a real game client with real rendering (CI runs it with Mesa). It builds a
 * showroom of every machine, opens a machine screen and the Engineer's Handbook, and saves
 * screenshots, so the looks can be checked from actual game renders rather than previews.
 */
public class JugcraftClientGameTests implements FabricClientGameTest {
	/** GLFW key code of F1 (hide the HUD). */
	private static final int GLFW_KEY_F1 = 290;

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
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x - 10, y - 1, z - 10, x + 40, y - 1, z + 4));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 10, y, z - 10, x + 40, y + 6, z + 4));
			server.runOnServer(minecraft -> buildShowroom(minecraft.overworld(), new BlockPos(x, y, z - 5)));

			// Hide the HUD, hand and chat (F1) so the screenshots show only the machines.
			context.getInput().pressKey(GLFW_KEY_F1);

			// One-block machines, facing the camera, in two halves.
			server.runCommand("tp @p %d %d %d 180 25".formatted(x - 4, y, z - 2));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_machines_1");
			server.runCommand("tp @p %d %d %d 180 25".formatted(x + 3, y, z - 2));
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_machines_2");

			// Multi-block machines.
			server.runCommand("tp @p %d %d %d 180 0".formatted(x + 27, y, z + 1));
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_multiblocks");

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
	 * camera; multi-block machines in a second group 20 blocks east, three apart.
	 */
	private static void buildShowroom(ServerLevel level, BlockPos row) {
		int large = 0;
		for (MachineKind kind : MachineKind.values()) {
			MachineBlock block = JugcraftMachines.MACHINES.get(kind);
			BlockState state = block.defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH);
			if (kind.isLarge()) {
				BlockPos pos = row.offset(20 + large * 3, 0, 0);
				level.setBlock(pos, state, 3);
				((LargeMachineBlock) block).setPlacedBy(level, pos, state, null, ItemStack.EMPTY);
				large++;
			} else {
				level.setBlock(row.offset(-7 + singleIndex(kind), 0, 0), state, 3);
			}
		}
	}
}
