package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.client.EnergyExchangeRenderChecks;
import io.github.jimbozoomer.jugcraft.drone.DroneTier;
import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

/** Actual endpoint packets, independent towers, changing energy needles, and unloaded-link behavior. */
public class EnergyExchangeClientGameTests implements FabricClientGameTest {
	@Override public void runTest(ClientGameTestContext context) {
		if (TestFocus.skip("tower") || GuideScreenshotGameTests.active()) return;
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender(); var server = singleplayer.getServer();
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			BlockPos firstCore = origin.offset(-45, -1, 16), secondCore = origin.offset(45, -1, 16);
			BlockPos first = firstCore.offset(-22, 1, 4), second = secondCore.offset(-22, 1, 4);
			server.runCommand("time set noon"); server.runCommand("weather clear");
			server.runOnServer(minecraft -> {
				var level = minecraft.overworld(); var player = minecraft.getPlayerList().getPlayers().getFirst();
				player.getAbilities().flying = true; player.onUpdateAbilities();
				for (BlockPos at : new BlockPos[] {firstCore, secondCore}) {
					level.setBlock(at, JugcraftTower.CORE.defaultBlockState(), Block.UPDATE_ALL);
					var core = (TowerCoreBlockEntity) level.getBlockEntity(at);
					core.setOwner(at.equals(firstCore) ? player.getUUID() : java.util.UUID.randomUUID()); core.buildInstantly(level, 1);
					var terminal = core.terminal(); terminal.rescanNow();
					terminal.energy().setAmount(at.equals(firstCore) ? 100000 : 170000);
					if (at.equals(firstCore)) for (int i = 0; i < 4; i++) {
						if (!terminal.linkDrone(player, DroneTier.COURIER_QUAD)) throw new AssertionError("Live panel setup could not link its four real idle drones");
					}
					terminal.refreshClients();
				}
			});
			context.waitFor(client -> EnergyExchangeRenderChecks.ready(client, first, second));
			EnergyExchangeRenderChecks.actualSnapshotsAndIdentity(context, first, second);
			EnergyExchangeRenderChecks.needleAndSnapshotChannels(context);
			context.runOnClient(client -> {
				client.getWindow().setWindowed(1280, 720); client.options.guiScale().set(2); client.options.fov().set(70);
				if (!client.gui.hud.isHidden()) client.gui.hud.toggle();
			});
			server.runCommand("tp @p %s %s %s -90 6".formatted(firstCore.getX() - 24.8, firstCore.getY() + 1.5, firstCore.getZ() + .5));
			context.waitTicks(25); context.getInput().lookAt(firstCore.offset(-22, 3, 0)); context.waitTicks(10);
			context.takeScreenshot("jugcraft_power_exchange_live_panel");
			server.runOnServer(minecraft -> {
				var terminal = ((TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(firstCore)).terminal();
				terminal.energy().setAmount(150000); terminal.refreshClients();
			});
			context.waitFor(client -> EnergyExchangeRenderChecks.charge(client, first) > 65);
			context.waitTicks(5); context.takeScreenshot("jugcraft_power_exchange_charge_changed");
			server.runCommand("tp @p %s %s %s -90 12".formatted(firstCore.getX() - 31.5, firstCore.getY() + 4, firstCore.getZ() + 5));
			context.waitTicks(15); context.getInput().lookAt(firstCore.offset(-20, 2, 0)); context.waitTicks(10);
			context.takeScreenshot("jugcraft_power_exchange_engineered_assembly");
			EnergyExchangeRenderChecks.unloadedTerminalIsOffline(context, first);
		}
	}
}
