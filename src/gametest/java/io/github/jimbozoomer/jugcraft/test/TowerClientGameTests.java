package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.client.TowerScreen;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneTier;
import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Client game test for the Drone Tower, with screenshots from a real client: the plinth and core, the tower
 * status screen, the Command Post (tier 1) and its command room, the hangar deck (tier 2) with drones in
 * their hangars, drones flying out of their hangars to build tier 3, and the finished tier 9 tower.
 */
public class TowerClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (GuideScreenshotGameTests.active()) {
			return;
		}
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
			server.runCommand("gamerule doDaylightCycle false");
			// A clear, flat site 81x81 round the core (x, y - 1, z + 40), 40 high.
			BlockPos core = new BlockPos(x, y - 1, z + 40);
			for (int dx = -40; dx <= 40; dx += 27) {
				int x0 = core.getX() + dx, x1 = Math.min(core.getX() + 40, x0 + 26);
				server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x0, y - 2, core.getZ() - 40, x1, y - 1, core.getZ() + 40));
				for (int h = 0; h < 40; h += 5) {
					server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x0, y + h, core.getZ() - 40, x1, y + h + 4, core.getZ() + 40));
				}
			}
			server.runOnServer(minecraft -> {
				ServerLevel level = minecraft.overworld();
				for (int dx = -7; dx <= 7; dx++) {
					for (int dz = -7; dz <= 7; dz++) {
						level.setBlock(core.offset(dx, 0, dz), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 3);
					}
				}
				level.setBlock(core, JugcraftTower.CORE.defaultBlockState(), 3);
				ServerPlayer player = minecraft.getPlayerList().getPlayers().getFirst();
				TowerCoreBlockEntity entity = (TowerCoreBlockEntity) level.getBlockEntity(core);
				entity.setOwner(player.getUUID());
				for (String module : JugcraftTower.MODULES) {
					entity.deposit(new ItemStack(JugcraftTower.MODULE_ITEMS.get(module), TowerCoreBlockEntity.MODULE_CAPACITY));
				}
				player.getAbilities().flying = true;
				player.onUpdateAbilities();
			});
			setHudHidden(context, true);

			// The plinth with the core, and the status screen before anything is built.
			server.runCommand("tp @p %d.5 %d %d.5 180 35".formatted(core.getX(), y + 6, core.getZ() + 14));
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_tower_plinth");
			context.setScreen(() -> new TowerScreen(core));
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_tower_screen_tier0");
			context.setScreen(() -> null);

			// UPGRADE: the core builds the Command Post itself.
			server.runOnServer(minecraft -> ((TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core))
					.tryUpgrade(minecraft.getPlayerList().getPlayers().getFirst()));
			context.waitTicks(80);
			server.runCommand("tp @p %d.5 %d %d.5 160 20".formatted(core.getX() - 8, y + 9, core.getZ() + 28));
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_tower_tier1_building");
			waitForTier(context, server, core, 1, 600);
			singleplayer.getConnection().waitForChunksRender();
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_tower_tier1");

			// The Energy Exchange (west edge) and the Storage Exchange (east edge).
			server.runCommand("tp @p %d.5 %d %d.5 -110 20".formatted(core.getX() - 32, y + 7, core.getZ() + 8));
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_tower_energy_exchange");
			server.runCommand("tp @p %d.5 %d %d.5 110 20".formatted(core.getX() + 32, y + 7, core.getZ() + 8));
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_tower_storage_exchange");

			// The command room inside the Command Post.
			server.runCommand("tp @p %d.5 %d %d.5 140 12".formatted(core.getX() + 1, y, core.getZ() + 4));
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_tower_command_room");
			server.runCommand("tp @p %d.5 %d %d.5 180 -4".formatted(core.getX(), y, core.getZ() + 4));
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_tower_video_wall");
			context.setScreen(() -> new TowerScreen(core));
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_tower_screen_tier1");
			context.setScreen(() -> null);

			// Tier 2 (built at once here), a full fleet of 32: 24 in the deck's hangars and 8 on the pads.
			server.runOnServer(minecraft -> {
				ServerLevel level = minecraft.overworld();
				TowerCoreBlockEntity entity = (TowerCoreBlockEntity) level.getBlockEntity(core);
				entity.buildInstantly(level, 2);
				ServerPlayer player = minecraft.getPlayerList().getPlayers().getFirst();
				DroneTerminalBlockEntity terminal = entity.terminal();
				terminal.energy().setAmount(DroneTerminalBlockEntity.ENERGY_CAPACITY);
				for (int i = 0; i < 32; i++) {
					terminal.linkDrone(player, i % 2 == 0 ? DroneTier.SURVEY_HEXACOPTER : DroneTier.COURIER_QUAD);
				}
			});
			singleplayer.getConnection().waitForChunksRender();
			server.runCommand("tp @p %d.5 %d %d.5 150 22".formatted(core.getX() - 14, y + 16, core.getZ() + 34));
			context.waitTicks(30);
			context.takeScreenshot("jugcraft_tower_tier2_deck");
			// The docked drones reach the client: every one of the 32 is in the terminal's view with its dock.
			BlockPos terminalPos = server.computeOnServer(minecraft -> ((TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core)).terminal().getBlockPos());
			String serverDocks = server.computeOnServer(minecraft -> {
				DroneTerminalBlockEntity terminal = ((TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core)).terminal();
				return terminal.fleet().size() + " drones, first docks " + terminal.dockOf(0) + " " + terminal.dockOf(31);
			});
			String clientDocks = context.computeOnClient(client -> client.level.getBlockEntity(terminalPos) instanceof DroneTerminalBlockEntity t
					&& t.view() != null ? t.view().docks.size() + " docks, first " + (t.view().docks.isEmpty() ? "-" : t.view().docks.getFirst())
					: "no view");
			System.out.println("[Jugcraft tower test] server: " + serverDocks + " | client: " + clientDocks);
			if (!clientDocks.startsWith("32 docks")) {
				throw new AssertionError("The client does not see the 32 docked drones. Server: " + serverDocks + ". Client: " + clientDocks);
			}
			server.runCommand("tp @p %d.5 %d %d.5 180 8".formatted(core.getX(), y + 14, core.getZ() + 26));
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_tower_deck_hangars");
			server.runCommand("tp @p %d.5 %d %d.5 45 18".formatted(core.getX() + 30, y + 18, core.getZ() - 30));
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_tower_deck_north_east");
			// Close up: a drone parked in its own hangar on the deck's north side.
			server.runCommand("tp @p %d.5 %d %d.5 0 14".formatted(core.getX() - 10, y + 12, core.getZ() - 23));
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_tower_hangar_close");
			server.runCommand("tp @p %d.5 %d %d.5 180 -10".formatted(core.getX() + 6, y + 2, core.getZ() + 22));
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_tower_under_deck");

			// UPGRADE to tier 3: drones fly out of their hangars and bring the new floor's tiles in.
			server.runOnServer(minecraft -> ((TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core))
					.tryUpgrade(minecraft.getPlayerList().getPlayers().getFirst()));
			server.runCommand("tp @p %d.5 %d %d.5 160 28".formatted(core.getX() - 10, y + 26, core.getZ() + 36));
			float progress = 0;
			int sounds = 0;
			for (int i = 0; i < 40 && progress <= 0; i++) {
				charge(server, core);
				context.waitTicks(20);
				sounds = Math.max(sounds, context.computeOnClient(client -> io.github.jimbozoomer.jugcraft.client.DroneSounds.playing()));
				if (i == 8) {
					context.takeScreenshot("jugcraft_tower_drones_building");
				}
				progress = server.computeOnServer(minecraft -> ((TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core)).buildProgress());
			}
			context.waitTicks(40);
			context.takeScreenshot("jugcraft_tower_tier3_tiles");
			System.out.println("[Jugcraft tower test] drone sounds playing at most: " + sounds);
			if (sounds <= 0) {
				throw new AssertionError("No drone rotor sound followed the drones flying tier 3 in");
			}
			if (progress <= 0) {
				String status = server.computeOnServer(minecraft -> ((TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core)).terminal()
						.status().stream().map(c -> c.getString()).collect(java.util.stream.Collectors.joining(" | ")));
				throw new AssertionError("No tier 3 tile was flown in after 40 seconds. Terminal: " + status);
			}

			// The finished tower (built at once here).
			server.runOnServer(minecraft -> {
				ServerLevel level = minecraft.overworld();
				((TowerCoreBlockEntity) level.getBlockEntity(core)).buildInstantly(level, 9);
			});
			context.runOnClient(client -> {
				client.options.fov().set(110);
			});
			server.runCommand("tp @p %d.5 %d %d.5 -163 0".formatted(core.getX() - 20, y + 105, core.getZ() + 66));
			singleplayer.getConnection().waitForChunksRender();
			context.waitTicks(100);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_tower_tier9");
			context.runOnClient(client -> client.options.fov().set(70));
			server.runCommand("tp @p %d.5 %d %d.5 150 10".formatted(core.getX() - 30, y + 30, core.getZ() + 50));
			singleplayer.getConnection().waitForChunksRender();
			context.waitTicks(30);
			context.takeScreenshot("jugcraft_tower_tier9_base");
			setHudHidden(context, false);
		}
	}

	private static void charge(TestServerContext server, BlockPos core) {
		server.runOnServer(minecraft -> {
			if (minecraft.overworld().getBlockEntity(core) instanceof TowerCoreBlockEntity entity && entity.terminal() != null) {
				entity.terminal().energy().setAmount(DroneTerminalBlockEntity.ENERGY_CAPACITY);
			}
		});
	}

	private static void waitForTier(ClientGameTestContext context, TestServerContext server, BlockPos core, int tier, int maxTicks) {
		for (int i = 0; i < maxTicks; i += 10) {
			int now = server.computeOnServer(minecraft -> minecraft.overworld().getBlockEntity(core) instanceof TowerCoreBlockEntity entity ? entity.tier() : -1);
			if (now >= tier) {
				return;
			}
			context.waitTicks(10);
		}
		throw new AssertionError("The tower did not reach tier " + tier + " in " + maxTicks + " ticks");
	}

	private static void setHudHidden(ClientGameTestContext context, boolean hidden) {
		context.runOnClient(client -> {
			if (client.gui.hud.isHidden() != hidden) {
				client.gui.hud.toggle();
			}
		});
	}
}
