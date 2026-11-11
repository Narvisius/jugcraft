package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.client.EnergyExchangeRenderChecks;
import io.github.jimbozoomer.jugcraft.drone.DroneTier;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStationBlock;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStations;
import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.TowerData;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Actual endpoint packets, independent towers, changing energy needles, and unloaded-link behavior. */
public class EnergyExchangeClientGameTests implements FabricClientGameTest {
	/**
	 * How far south of the first Tri-Pillar the second one stands: a foundation reaches z -53..45 from its core, so the
	 * two never overlap. A multiple of 16, so both cores sit the same way in their chunks.
	 */
	static final int TOWER_SPACING = 112;
	/** The foundation's reach from its core (x -52..62, z -53..45). */
	private static final int WEST = -52, EAST = 62, NORTH = -53, SOUTH = 45;

	@Override public void runTest(ClientGameTestContext context) {
		if (TestFocus.skip("tower") || GuideScreenshotGameTests.active()) return;
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender(); var server = singleplayer.getServer();
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			BlockPos firstCore = firstCore(origin), secondCore = firstCore.south(TOWER_SPACING);
			BlockPos first = firstCore.offset(ExchangeStations.masterOffset(ExchangeStations.ENERGY, TowerData.CURRENT_LAYOUT));
			BlockPos second = secondCore.offset(ExchangeStations.masterOffset(ExchangeStations.ENERGY, TowerData.CURRENT_LAYOUT));
			server.runCommand("time set noon"); server.runCommand("weather clear");
			prepareTwoTowers(server, firstCore, secondCore);
			server.runOnServer(minecraft -> {
				var level = minecraft.overworld(); var player = minecraft.getPlayerList().getPlayers().getFirst();
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
			// Two whole foundations arrive at the client first: allow a minute.
			context.waitFor(client -> EnergyExchangeRenderChecks.ready(client, first, second), 1200);
			EnergyExchangeRenderChecks.actualSnapshotsAndIdentity(context, first, second);
			EnergyExchangeRenderChecks.needleAndSnapshotChannels(context);
			context.runOnClient(client -> {
				client.getWindow().setWindowed(1280, 720); client.options.guiScale().set(2); client.options.fov().set(70);
				if (!client.gui.hud.isHidden()) client.gui.hud.toggle();
			});
			BlockPos socket = server.computeOnServer(minecraft -> socket(minecraft.overworld(), first, Direction.EAST));
			server.runCommand("tp @p %s %s %s 90 6".formatted(socket.getX() + 3.8, socket.getY() - .5, socket.getZ() + .5));
			context.waitTicks(25); context.getInput().lookAt(socket.above()); context.waitTicks(10);
			context.takeScreenshot("jugcraft_power_exchange_live_panel");
			server.runOnServer(minecraft -> {
				var terminal = ((TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(firstCore)).terminal();
				terminal.energy().setAmount(150000); terminal.refreshClients();
			});
			context.waitFor(client -> EnergyExchangeRenderChecks.charge(client, first) > 65);
			context.waitTicks(5); context.takeScreenshot("jugcraft_power_exchange_charge_changed");
			server.runCommand("tp @p %s %s %s 90 12".formatted(socket.getX() + 10.5, socket.getY() + 2, socket.getZ() + 5));
			context.waitTicks(15); context.getInput().lookAt(socket.west(2)); context.waitTicks(10);
			context.takeScreenshot("jugcraft_power_exchange_engineered_assembly");
			EnergyExchangeRenderChecks.unloadedTerminalIsOffline(context, first);
		}
	}

	/**
	 * The marked socket of the station whose master is {@code master}, from the built blocks themselves. The camera
	 * positions round it are written for a station facing {@code facing} (the Tri-Pillar's exchanges both face east out
	 * of the intake face, layout 37).
	 */
	static BlockPos socket(ServerLevel level, BlockPos master, Direction facing) {
		BlockState state = level.getBlockState(master);
		if (!(state.getBlock() instanceof ExchangeStationBlock block) || state.getValue(ExchangeStationBlock.FACING) != facing) {
			throw new AssertionError("No exchange facing " + facing + " at " + master + ": " + state);
		}
		return block.footprint(state).partPos(master, facing, block.portPart(state));
	}

	/**
	 * The first tower's core, near the spawn: in the middle of its chunk (x and z 8 in it), so the station's master, its
	 * terminal and its packager each lie in a chunk of their own (the unload checks drop one chunk at a time).
	 */
	static BlockPos firstCore(BlockPos origin) {
		return new BlockPos(Math.floorDiv(origin.getX(), 16) * 16 - 40, origin.getY() - 1, Math.floorDiv(origin.getZ(), 16) * 16 - 24);
	}

	/**
	 * Gets the ground under both towers ready before they are built: the player flies above the gap between them (28 east and
	 * 40 south of the first core, 30 up), where the client's five chunks of view take in both cores, stations and terminals;
	 * both foundations are force-loaded, since an instant build skips any block whose chunk is not loaded.
	 */
	static void prepareTwoTowers(TestServerContext server, BlockPos firstCore, BlockPos secondCore) {
		server.runOnServer(minecraft -> {
			var player = minecraft.getPlayerList().getPlayers().getFirst();
			player.getAbilities().flying = true; player.onUpdateAbilities();
		});
		server.runCommand("tp @p %s %s %s".formatted(firstCore.getX() + 28.5, firstCore.getY() + 30, firstCore.getZ() + 40.5));
		int x0 = Math.min(firstCore.getX(), secondCore.getX()) + WEST, x1 = Math.max(firstCore.getX(), secondCore.getX()) + EAST;
		int z0 = Math.min(firstCore.getZ(), secondCore.getZ()) + NORTH, z1 = Math.max(firstCore.getZ(), secondCore.getZ()) + SOUTH;
		server.runCommand("forceload add %d %d %d %d".formatted(x0, z0, x1, z1));
		server.waitFor(minecraft -> {
			var level = minecraft.overworld();
			for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) {
				for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
					if (!level.getChunkSource().hasChunk(cx, cz)) return false;
				}
			}
			return true;
		}, 1200);
	}
}
