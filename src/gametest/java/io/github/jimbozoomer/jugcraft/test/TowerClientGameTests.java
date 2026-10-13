package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.client.TowerScreen;
import io.github.jimbozoomer.jugcraft.client.DroneTerminalScreen;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneTier;
import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.gui.components.Button;
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
		if (TestFocus.skip("tower")) {
			return;
		}
		if (GuideScreenshotGameTests.active()) {
			return;
		}
		assertDoorClearanceAtEverySpeed();
		if ("true".equals(System.getenv("JUGCRAFT_TOWER_VISUAL_REVIEW"))) {
			context.runOnClient(client -> {
				client.getWindow().setWindowed(1280, 720);
				client.options.guiScale().set(2);
			});
		}
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			int x = origin.getX();
			int y = origin.getY();
			int z = origin.getZ();
			TestServerContext server = singleplayer.getServer();
			io.github.jimbozoomer.jugcraft.client.TowerRenderChecks.independentProgressCaches(context);
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamerule doDaylightCycle false");
			// Clear the site; the tower must build its own paving and backing, without a gallery stone slab.
			BlockPos core = new BlockPos(x, y - 1, z + 40);
			for (int dx = -40; dx <= 40; dx += 27) {
				int x0 = core.getX() + dx, x1 = Math.min(core.getX() + 40, x0 + 26);
				server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x0, y - 2, core.getZ() - 40, x1, y - 1, core.getZ() + 40));
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
			int firstTierTicks = server.computeOnServer(minecraft -> {
				var first = ((TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core)).layout().tier(1);
				return (first.place.length + first.air.length + TowerCoreBlockEntity.CORE_BLOCKS_PER_TICK - 1)
						/ TowerCoreBlockEntity.CORE_BLOCKS_PER_TICK + 100;
			});
			waitForTier(context, server, core, 1, firstTierTicks);
			assertConstructedApron(server, core);
			singleplayer.getConnection().waitForChunksRender();
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_tower_tier1");
			server.runCommand("tp @p %d.5 %d %d.5 150 34".formatted(core.getX() + 8, y + 4, core.getZ() + 17));
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_tower_ground_pad_detail");

			// The Energy Exchange (west edge) and the Storage Exchange (east edge).
			server.runCommand("tp @p %d.5 %d %d.5 -110 20".formatted(core.getX() - 32, y + 7, core.getZ() + 8));
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_tower_energy_exchange");
			server.runCommand("tp @p %d.5 %d %d.5 110 20".formatted(core.getX() + 32, y + 7, core.getZ() + 8));
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_tower_storage_exchange");

			// The command room inside the Command Post.
			server.runCommand("tp @p %s %s %s 140 12".formatted(core.getX() + 4.5, core.getY() + 1, core.getZ() + 5.8));
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_tower_command_room");
			io.github.jimbozoomer.jugcraft.client.TowerRenderChecks.hologramFitsTable(context, core);
			server.runCommand("tp @p %s %s %s 180 -4".formatted(core.getX() + .5, core.getY() + 1, core.getZ() + 6.4));
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_tower_video_wall");
			context.setScreen(() -> new TowerScreen(core));
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_tower_screen_tier1");
			context.setScreen(() -> null);
			masterControls(context, server, core);

			// Tier 2: all 32 T1 ground slots and all 24 T2 hangars, with exact tier allocation.
			server.runOnServer(minecraft -> {
				ServerLevel level = minecraft.overworld();
				TowerCoreBlockEntity entity = (TowerCoreBlockEntity) level.getBlockEntity(core);
				entity.buildInstantly(level, 2);
				ServerPlayer player = minecraft.getPlayerList().getPlayers().getFirst();
				DroneTerminalBlockEntity terminal = entity.terminal();
				terminal.energy().setAmount(DroneTerminalBlockEntity.ENERGY_CAPACITY);
				for (int i = 0; i < 32; i++) {
					terminal.linkDrone(player, DroneTier.COURIER_QUAD);
				}
				for (int i = 0; i < 24; i++) terminal.linkDrone(player, DroneTier.SURVEY_HEXACOPTER);
			});
			singleplayer.getConnection().waitForChunksRender();
			server.runCommand("tp @p %d.5 %d %d.5 150 22".formatted(core.getX() - 14, y + 16, core.getZ() + 34));
			context.waitTicks(30);
			context.takeScreenshot("jugcraft_tower_tier2_deck");
			// The docked drones reach the client: every one of the 56 has its own dock.
			BlockPos terminalPos = server.computeOnServer(minecraft -> ((TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core)).terminal().getBlockPos());
			String serverDocks = server.computeOnServer(minecraft -> {
				DroneTerminalBlockEntity terminal = ((TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core)).terminal();
				return terminal.fleet().size() + " drones, first docks " + terminal.dockOf(0) + " " + terminal.dockOf(55);
			});
			String clientDocks = context.computeOnClient(client -> client.level.getBlockEntity(terminalPos) instanceof DroneTerminalBlockEntity t
					&& t.view() != null ? t.view().docks.size() + " docks, first " + (t.view().docks.isEmpty() ? "-" : t.view().docks.getFirst())
					: "no view");
			System.out.println("[Jugcraft tower test] server: " + serverDocks + " | client: " + clientDocks);
			if (!clientDocks.startsWith("56 docks")) {
				throw new AssertionError("The client does not see the 56 docked drones. Server: " + serverDocks + ". Client: " + clientDocks);
			}
			terminalScreens(context, server, terminalPos);
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
			server.runCommand("tp @p %d.5 %d %d.5 -90 10".formatted(core.getX() - 23, y + 14, core.getZ() - 10));
			int sounds = 0;
			boolean delivered = false;
			for (int i = 0; i < 40 && (!delivered || sounds <= 0); i++) {
				charge(server, core);
				context.waitTicks(20);
				sounds = Math.max(sounds, context.computeOnClient(client -> io.github.jimbozoomer.jugcraft.client.DroneSounds.playing()));
				if (i == 8) {
					context.takeScreenshot("jugcraft_tower_drones_building");
				}
				delivered = server.computeOnServer(minecraft -> {
					var entity = (TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core);
					return entity.tier() >= 3 || entity.filledTiles().stream().anyMatch(index -> entity.layout().tier(3).tiles.get(index).blocks().length > 0);
				});
			}
			context.waitTicks(40);
			sounds = Math.max(sounds, context.computeOnClient(client -> io.github.jimbozoomer.jugcraft.client.DroneSounds.playing()));
			context.takeScreenshot("jugcraft_tower_tier3_tiles");
			System.out.println("[Jugcraft tower test] drone sounds playing at most: " + sounds);
			if (sounds <= 0) {
				throw new AssertionError("No drone rotor sound followed the drones flying tier 3 in");
			}
			if (!delivered) {
				String status = server.computeOnServer(minecraft -> ((TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core)).terminal()
						.status().stream().map(c -> c.getString()).collect(java.util.stream.Collectors.joining(" | ")));
				throw new AssertionError("No tier 3 tile was flown in after 40 seconds. Terminal: " + status);
			}

			// The finished tower (built at once here).
			// The game-test harness starts with five chunks of visibility: the full-height
			// camera is farther away than that, so raise both ends before moving it.
			context.runOnClient(client -> {
				client.options.renderDistance().set(14);
				client.options.fov().set(110);
				// ChunkMap also clamps to the player's last advertised view distance.
				client.options.broadcastOptions();
			});
			server.runOnServer(minecraft -> {
				minecraft.getPlayerList().setViewDistance(14);
			});
			// Exercise the same operator command used to review the installed redesign.
			server.runCommand("execute as @p at @s run dronetest build industrial 9");
			server.runOnServer(minecraft -> {
				var tower = (TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core);
				if (tower.tier() != 9 || tower.layoutVersion() != io.github.jimbozoomer.jugcraft.tower.TowerData.INDUSTRIAL_LAYOUT) {
					throw new AssertionError("The industrial review command did not build the redesigned tower to tier 9");
				}
			});
			try {
				server.waitFor(minecraft -> minecraft.getPlayerList().getPlayers().getFirst().requestedViewDistance() == 14, 200);
			} catch (AssertionError timeout) {
				throw new AssertionError("The finished-tower camera options were not advertised: "
						+ towerCameraDistances(context, server), timeout);
			}
			System.out.println("[Jugcraft tower test] finished camera visibility: " + towerCameraDistances(context, server));
			BlockPos towerCenter = core.offset(0, 107, 0);
			server.runCommand("tp @p %s %s %s facing %s %s %s".formatted(
					core.getX() + .5 - 80, core.getY() + 106.0, core.getZ() + .5 + 140,
					towerCenter.getX() + .5, towerCenter.getY() + .5, towerCenter.getZ() + .5));
			context.getInput().lookAt(towerCenter);
			waitForTowerChunks(context, server, core);
			context.waitTicks(100);
			context.getInput().lookAt(towerCenter);
			context.waitTicks(5);
			assertTowerCamera(context, core, towerCenter);
			context.takeScreenshot("jugcraft_tower_tier9");
			context.runOnClient(client -> client.options.fov().set(70));
			BlockPos baseCenter = core.offset(0, 15, 0);
			server.runCommand("tp @p %s %s %s facing %s %s %s".formatted(
					core.getX() + .5 - 30, core.getY() + 31.0, core.getZ() + .5 + 50,
					baseCenter.getX() + .5, baseCenter.getY() + .5, baseCenter.getZ() + .5));
			context.getInput().lookAt(baseCenter);
			waitForTowerChunks(context, server, core);
			context.waitTicks(40);
			context.getInput().lookAt(baseCenter);
			context.waitTicks(5);
			assertTowerCamera(context, core, baseCenter);
			context.takeScreenshot("jugcraft_tower_tier9_base");
			server.runCommand("tp @p %s %s %s 140 -12".formatted(core.getX() + 4.5, core.getY() + 1, core.getZ() + 5.8));
			context.waitTicks(20);
			io.github.jimbozoomer.jugcraft.client.TowerRenderChecks.hologramFitsTable(context, core);
			context.takeScreenshot("jugcraft_tower_command_room_complete");
			reviewCloseups(context, server, core);
			setHudHidden(context, false);
		}
	}

	/** Native in-game review of the revised working areas, including real connected networks and night light. */
	private static void reviewCloseups(ClientGameTestContext context, TestServerContext server, BlockPos core) {
		context.runOnClient(client -> client.options.fov().set(85));
		reviewPhoto(context, server, core, 47.5, 24, 52.5, core.offset(0, 0, 12), "jugcraft_tower_constructed_apron_daylight");
		context.runOnClient(client -> client.options.fov().set(70));
		reviewPhoto(context, server, core, .5, 1, 18.5, core.offset(0, 3, 7), "jugcraft_tower_clear_control_room_entrance");
		context.runOnClient(client -> client.options.fov().set(85));
		reviewPhoto(context, server, core, .5, 1, 6.7, core.offset(0, 2, 3), "jugcraft_tower_master_console_live_readout");
		context.runOnClient(client -> client.options.fov().set(45));
		reviewPhoto(context, server, core, 3.7, 1, -1.5, core.offset(6, 2, -2), "jugcraft_tower_wall_computer_live_readout");
		context.runOnClient(client -> client.options.fov().set(70));
		server.runOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			BlockPos power = core.offset(-22, 2, 0), cargo = core.offset(20, 2, 0);
			for (int distance = 1; distance <= 2; distance++) {
				level.setBlock(power.west(distance), io.github.jimbozoomer.jugcraft.machine.JugcraftMachines.COPPER_CABLE.defaultBlockState(), 3);
				level.setBlock(cargo.east(distance), io.github.jimbozoomer.jugcraft.logistics.JugcraftLogistics.BRASS_ITEM_PIPE.defaultBlockState(), 3);
			}
			// Calculate the same neighboring faces as normal placement; default pipe states have no arms.
			for (int distance = 1; distance <= 2; distance++) {
				BlockPos tube = cargo.east(distance);
				level.setBlock(tube, net.minecraft.world.level.block.Block.updateFromNeighbourShapes(level.getBlockState(tube), level, tube), 3);
			}
			if (!level.getBlockState(cargo.east()).getValue(net.minecraft.world.level.block.PipeBlock.WEST))
				throw new AssertionError("Cargo photo fixture lacks the real machine-side item-pipe arm");
			// A short supported supply connection occupies the marked approach,
			// with the surrounding service apron left clear for an operator.
			level.setBlock(power.west(3).below(), JugcraftTower.BLOCKS.get("steel_armor_plate").defaultBlockState(), 3);
			level.setBlock(power.west(3), io.github.jimbozoomer.jugcraft.energy.CreativeEnergyCellBlock.BLOCK.defaultBlockState(), 3);
			level.setBlock(cargo.east(3).below(), JugcraftTower.BLOCKS.get("steel_armor_plate").defaultBlockState(), 3);
			level.setBlock(cargo.east(3), Blocks.CHEST.defaultBlockState(), 3);
			var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(cargo.east(3));
			chest.setItem(0, new ItemStack(net.minecraft.world.item.Items.STONE_BRICKS, 16));
		});
		context.waitTicks(40);
		reviewPhoto(context, server, core, -27.5, 2, 4.5, core.offset(-22, 2, 0), "jugcraft_tower_power_socket_connected");
		reviewPhoto(context, server, core, 26.5, 2, 4.5, core.offset(20, 2, 0), "jugcraft_tower_cargo_socket_connected");
		io.github.jimbozoomer.jugcraft.client.TowerUtilityClientChecks.cableSockets(context, server, core);
		reviewPhoto(context, server, core, 24.5, 144, 26.5, core.offset(12, 139, 12), "jugcraft_tower_upper_supply_supports");
		reviewPhoto(context, server, core, 35.5, 194, 38.5, core.offset(0, 191, 0), "jugcraft_tower_closed_shoulders_crown");
		server.runCommand("time set midnight");
		context.waitTicks(40);
		reviewPhoto(context, server, core, 39.5, 10, 46.5, core.offset(19, 0, 29), "jugcraft_tower_sparse_ground_lights_night");
		reviewPhoto(context, server, core, 4.5, 1, 5.8, core.offset(0, 3, -3), "jugcraft_tower_operations_room_night");
		reviewPhoto(context, server, core, 6.5, 3, 23.5, core.offset(6, 5, 13), "jugcraft_tower_under_deck_task_lights_night");
		reviewPhoto(context, server, core, .5, 16, 45.5, core.offset(0, 2, 20), "jugcraft_tower_service_apron_night");
		server.runCommand("time set noon");
	}

	private static void assertConstructedApron(TestServerContext server, BlockPos core) {
		server.runOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			var tower = (TowerCoreBlockEntity) level.getBlockEntity(core); var first = tower.layout().tier(1);
			for (int dx : new int[] {-40, 40}) for (int dz : new int[] {-40, 40}) for (int dy : new int[] {-1, 0}) {
				BlockPos offset = new BlockPos(dx, dy, dz);
				int[] placed = java.util.Arrays.stream(first.place).filter(b -> offset.equals(new BlockPos(b[0], b[1], b[2])))
						.findFirst().orElseThrow(() -> new AssertionError("Apron corner is absent from the actual tower design: " + offset));
				if (!level.getBlockState(core.offset(offset)).is(((TowerCoreBlockEntity) level.getBlockEntity(core)).layout().state(placed[3]).getBlock()))
					throw new AssertionError("Tower did not construct its apron and backing: " + offset);
			}
			var floorLight = JugcraftTower.BLOCKS.get("red_light_strip");
			long frozenLights = java.util.Arrays.stream(io.github.jimbozoomer.jugcraft.tower.TowerData
					.get(io.github.jimbozoomer.jugcraft.tower.TowerData.APRON_LAYOUT).tier(1).place)
					.filter(b -> b[1] == 0 && io.github.jimbozoomer.jugcraft.tower.TowerData
							.get(io.github.jimbozoomer.jugcraft.tower.TowerData.APRON_LAYOUT).state(b[3]).is(floorLight)).count();
			long actualLights = java.util.Arrays.stream(first.place).filter(b -> b[1] == 0
					&& level.getBlockState(core.offset(b[0], b[1], b[2])).is(floorLight)).count();
			if (actualLights <= 0 || actualLights >= frozenLights)
				throw new AssertionError("Actual tier-one ground lights were not reduced: " + actualLights + " versus saved design " + frozenLights);
			System.out.println("[Jugcraft tower test] actual bottom floor lights: " + actualLights + " (saved layout six: " + frozenLights + ")");
		});
	}

	/** Ordinary in-world right-click, owner mode control, then the real tower upgrade widget. */
	private static void masterControls(ClientGameTestContext context, TestServerContext server, BlockPos core) {
		BlockPos console = core.offset(0, 1, 4);
		server.runOnServer(minecraft -> {
			var player = minecraft.getPlayerList().getPlayers().getFirst();
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, ItemStack.EMPTY);
		});
		server.runCommand("tp @p %s %s %s".formatted(core.getX() + .5, core.getY() + 1, core.getZ() + 6.4));
		context.waitTicks(10); context.getInput().lookAt(console); context.waitTicks(3);
		context.getInput().pressKey(options -> options.keyUse);
		context.waitFor(client -> client.gui.screen() instanceof DroneTerminalScreen);
		context.waitTicks(3); context.takeScreenshot("jugcraft_tower_master_console_controls");
		var before = server.computeOnServer(minecraft -> ((TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core)).terminal().mode());
		context.clickScreenButton(modeButtonLabel(context)); context.waitTicks(6);
		if (!server.computeOnServer(minecraft -> ((TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core)).terminal().mode() != before)) {
			throw new AssertionError("Normal master-console use did not carry its clicked part into the real mode controls");
		}
		context.clickScreenButton(modeButtonLabel(context)); context.waitTicks(6);
		if (!server.computeOnServer(minecraft -> ((TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core)).terminal().mode() == before)) {
			throw new AssertionError("Owner could not restore the terminal mode through its master console");
		}
		context.clickScreenButton("TOWER CONTROL"); context.waitFor(client -> client.gui.screen() instanceof TowerScreen);
		context.waitTicks(3); context.takeScreenshot("jugcraft_tower_master_console_upgrade_screen");
		context.clickScreenButton("UPGRADE DRONE TOWER"); context.waitTicks(6);
		if (!server.computeOnServer(minecraft -> ((TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core)).building() == 2)) {
			throw new AssertionError("The master's tower controls did not start the real tier-two build");
		}
		context.setScreen(() -> null);
	}
	private static String modeButtonLabel(ClientGameTestContext context) {
		return context.computeOnClient(client -> client.gui.screen().children().stream()
				.filter(child -> child instanceof Button button && button.getMessage().getString().startsWith("▸ "))
				.map(child -> ((Button) child).getMessage().getString()).findFirst().orElseThrow());
	}

	/** Bounded local camera settle; distant terrain is intentionally not awaited. */
	private static void reviewPhoto(ClientGameTestContext context, TestServerContext server, BlockPos core,
			double dx, double dy, double dz, BlockPos target, String name) {
		server.runCommand("tp @p %s %s %s facing %s %s %s".formatted(
				core.getX() + dx, core.getY() + dy, core.getZ() + dz,
				target.getX() + .5, target.getY() + .5, target.getZ() + .5));
		context.waitTicks(20);
		context.getInput().lookAt(target);
		context.waitTicks(5);
		assertTowerCamera(context, core, target);
		context.takeScreenshot(name);
	}

	/** Wait for the tower itself, rather than the hundreds of distant terrain chunks requested for fog. */
	private static void waitForTowerChunks(ClientGameTestContext context, TestServerContext server, BlockPos core) {
		var samples = server.computeOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			var tower = (TowerCoreBlockEntity) level.getBlockEntity(core);
			Map<String, BlockPos> sections = new LinkedHashMap<>();
			for (var tier : tower.layout().tiers) {
				for (int[] block : tier.place) {
					BlockPos pos = core.offset(block[0], block[1], block[2]);
					if (!level.getBlockState(pos).isAir()) {
						String section = Math.floorDiv(pos.getX(), 16) + "/" + Math.floorDiv(pos.getY(), 16)
								+ "/" + Math.floorDiv(pos.getZ(), 16);
						sections.putIfAbsent(section, pos);
					}
				}
			}
			Map<BlockPos, net.minecraft.world.level.block.state.BlockState> expected = new LinkedHashMap<>();
			for (BlockPos pos : sections.values()) expected.put(pos, level.getBlockState(pos));
			expected.put(core, level.getBlockState(core));
			return expected;
		});
		try {
			context.waitFor(client -> samples.entrySet().stream()
					.allMatch(sample -> client.level.getBlockState(sample.getKey()).equals(sample.getValue())), 600);
		} catch (AssertionError timeout) {
			String missing = context.computeOnClient(client -> samples.entrySet().stream()
					.filter(sample -> !client.level.getBlockState(sample.getKey()).equals(sample.getValue()))
					.limit(12).map(sample -> sample.getKey() + " expected=" + sample.getValue()
							+ " actual=" + client.level.getBlockState(sample.getKey()))
					.collect(java.util.stream.Collectors.joining("; ")));
			throw new AssertionError("The finished tower did not arrive: " + towerCameraDistances(context, server)
					+ ". Missing section samples (up to 12): " + missing, timeout);
		}
	}

	private static String towerCameraDistances(ClientGameTestContext context, TestServerContext server) {
		String clientDistances = context.computeOnClient(client -> "client=" + client.options.renderDistance().get()
				+ ", effective=" + client.options.getEffectiveRenderDistance() + ", camera=" + client.player.position());
		return clientDistances + server.computeOnServer(minecraft -> ", server=" + minecraft.getPlayerList().getViewDistance()
				+ ", player request=" + minecraft.getPlayerList().getPlayers().getFirst().requestedViewDistance());
	}

	private static void assertTowerCamera(ClientGameTestContext context, BlockPos core, BlockPos target) {
		context.runOnClient(client -> {
			if (!client.level.getBlockState(core).is(JugcraftTower.CORE)) {
				throw new AssertionError("The finished tower is outside the client's loaded chunks");
			}
			var direction = net.minecraft.world.phys.Vec3.atCenterOf(target).subtract(client.player.getEyePosition()).normalize();
			if (client.player.getLookAngle().dot(direction) < .999) {
				throw new AssertionError("The finished-tower camera is not aimed at " + target);
			}
		});
	}

	private static void assertDoorClearanceAtEverySpeed() {
		for (int speed : new int[] {4, 20, 80}) {
			var path = new io.github.jimbozoomer.jugcraft.drone.FlightPath(
					new double[][] {{0, 1, 0}, {12, 1, 0}, {0, 1, 0}}, new double[] {8, 8}, speed,
					new double[] {0, -4}, new double[] {0, -4});
			for (double width : new double[] {3, 6, 7}) {
				double toPlane = width == 3 ? 2 : 3.5;
				double radius = width == 3 ? 1.2 : 2.4;
				double crossing = (toPlane - radius) * 20 / speed;
				if (io.github.jimbozoomer.jugcraft.client.DroneDepotRenderer.slidingDoorOpening(path, crossing, toPlane, width) < .999) {
					throw new AssertionError("Door obstructs the drone envelope: " + speed + " blocks/s, " + width + "m bay");
				}
				if (io.github.jimbozoomer.jugcraft.client.DroneDepotRenderer.slidingDoorOpening(path,
						path.totalTicks() - io.github.jimbozoomer.jugcraft.drone.FlightScheduler.RECHARGE_TICKS + 12, toPlane, width) != 0) {
					throw new AssertionError("Door did not close after the drone returned");
				}
			}
		}
	}

	private static void charge(TestServerContext server, BlockPos core) {
		server.runOnServer(minecraft -> {
			if (minecraft.overworld().getBlockEntity(core) instanceof TowerCoreBlockEntity entity && entity.terminal() != null) {
				entity.terminal().energy().setAmount(DroneTerminalBlockEntity.ENERGY_CAPACITY);
			}
		});
	}

	/** Real widgets and rendering with a full fleet, including a material list longer than one page. */
	private static void terminalScreens(ClientGameTestContext context, TestServerContext server, BlockPos pos) {
		context.setScreen(() -> new DroneTerminalScreen(pos));
		context.waitTicks(3);
		context.runOnClient(client -> {
			var screen = client.gui.screen();
			for (var child : screen.children()) {
				if (child instanceof Button button && button.visible) {
					if (button.getX() < 0 || button.getY() < 0 || button.getX() + button.getWidth() > screen.width
							|| button.getY() + button.getHeight() > screen.height) {
						throw new AssertionError("Terminal button is outside the GUI: " + button.getMessage().getString());
					}
				}
			}
		});
		context.takeScreenshot("jugcraft_tower_terminal_overview");
		context.clickScreenButton("FLEET");
		context.waitTicks(2);
		context.takeScreenshot("jugcraft_tower_terminal_fleet");
		context.clickScreenButton("POWER");
		context.waitTicks(2);
		context.takeScreenshot("jugcraft_tower_terminal_power");
		context.clickScreenButton("JOBS");
		Map<String, Integer> original = context.computeOnClient(client -> {
			var view = ((DroneTerminalBlockEntity) client.level.getBlockEntity(pos)).view();
			return new LinkedHashMap<>(view.missing);
		});
		try {
			context.runOnClient(client -> {
				var view = ((DroneTerminalBlockEntity) client.level.getBlockEntity(pos)).view();
				view.missing.clear();
				String[] ids = {"reinforced_concrete", "steel_girder", "aluminum_cladding", "steel_armor_plate",
						"tower_service_grating", "tower_service_railing", "tower_ladder", "tower_vent",
						"superconducting_conduit", "depleted_uranium_armor", "carbon_composite_panel", "graphene_lattice"};
				for (int i = 0; i < ids.length; i++) view.missing.put("jugcraft:" + ids[i], 1200 + i * 37);
			});
			context.waitTicks(2);
			context.takeScreenshot("jugcraft_tower_terminal_jobs_first");
			context.runOnClient(client -> {
				var screen = (DroneTerminalScreen) client.gui.screen();
				for (int i = 0; i < 20; i++) {
					if (!screen.mouseScrolled(screen.width / 2.0 + 25, screen.height / 2.0, 0, -1)) {
						throw new AssertionError("The long missing-material list did not consume the mouse wheel");
					}
				}
			});
			context.waitTicks(2);
			context.takeScreenshot("jugcraft_tower_terminal_jobs_last");
		} finally {
			context.runOnClient(client -> {
				if (client.level.getBlockEntity(pos) instanceof DroneTerminalBlockEntity terminal && terminal.view() != null) {
					terminal.view().missing.clear();
					terminal.view().missing.putAll(original);
				}
			});
			context.setScreen(() -> null);
			server.runOnServer(minecraft -> {
				if (minecraft.overworld().getBlockEntity(pos) instanceof DroneTerminalBlockEntity terminal) terminal.refreshClients();
			});
		}
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
