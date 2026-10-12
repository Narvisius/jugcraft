package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.client.FormMachineScreen;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineUpgrades;
import io.github.jimbozoomer.jugcraft.machine.form.FormMachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.form.MachineLifecycle;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

/**
 * Client game test for the shared industrial machine foundation (docs/features/industrial-machine-foundation.md): the
 * test mod's rig form, opened by using it, shows the formed-machine screen in its family's theme while a batch runs
 * (the tool socket locked, reserved room in the output tanks, the state and progress in the terminal), and then with
 * an input nothing there uses, named in the terminal. CI job {@code client}.
 */
public class IndustrialFoundationClientGameTests implements FabricClientGameTest {
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
			server.runCommand("time set 6000");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x - 6, y - 1, z - 8, x + 6, y - 1, z + 4));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 6, y, z - 8, x + 6, y + 6, z + 4));
			BlockPos rig = new BlockPos(x, y, z - 4);
			server.runOnServer(minecraft -> {
				ServerLevel level = minecraft.overworld();
				level.setBlockAndUpdate(rig, TestForms.RIG.defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
				TestForms.RIG.setPlacedBy(level, rig, level.getBlockState(rig), null, ItemStack.EMPTY);
				FormMachineBlockEntity machine = (FormMachineBlockEntity) level.getBlockEntity(rig);
				((SimpleEnergyStorage) machine.energy()).setAmount(machine.energy().getCapacity());
				machine.container().setItem(TestForms.RIG_FORM.firstUpgradeSlot(), new ItemStack(MachineUpgrades.EFFICIENCY));
			});
			context.waitTicks(10);

			server.runCommand("tp @p %d %d %d 180 25".formatted(x, y, z - 2));
			context.waitTicks(10);
			context.getInput().lookAt(rig);
			context.waitTick();
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(FormMachineScreen.class);

			// A batch: coal and water in, the bed in its socket; shot while it works.
			server.runOnServer(minecraft -> {
				FormMachineBlockEntity machine = (FormMachineBlockEntity) minecraft.overworld().getBlockEntity(rig);
				machine.container().setItem(0, new ItemStack(Items.COAL, 7));
				machine.container().setItem(TestForms.RIG_FORM.firstSocketSlot(), new ItemStack(Items.IRON_BLOCK));
				machine.tanks().input(0).fill(Fluids.WATER, 1750);
			});
			context.waitTicks(9);
			boolean working = server.computeOnServer(minecraft ->
					((FormMachineBlockEntity) minecraft.overworld().getBlockEntity(rig)).status().state().working());
			check(working, "The rig should be working when the screen is shot");
			context.takeScreenshot("jugcraft_form_screen");

			// Lava in the water tank: the terminal names it as something nothing here uses.
			server.runOnServer(minecraft -> {
				FormMachineBlockEntity machine = (FormMachineBlockEntity) minecraft.overworld().getBlockEntity(rig);
				machine.togglePause();
				machine.cancel();
				machine.container().setItem(0, ItemStack.EMPTY);
				machine.tanks().input(0).drain(machine.tanks().input(0).millibuckets());
				machine.tanks().input(0).fill(Fluids.LAVA, 1000);
				machine.togglePause();
			});
			context.waitTicks(5);
			MachineLifecycle state = server.computeOnServer(minecraft ->
					((FormMachineBlockEntity) minecraft.overworld().getBlockEntity(rig)).status().state());
			check(state == MachineLifecycle.WAITING_INPUT, "The rig should be waiting for usable input, not " + state);
			context.takeScreenshot("jugcraft_form_screen_unused_input");
			context.setScreen(() -> null);
		}
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}
}
