package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.client.TowerScreen;
import io.github.jimbozoomer.jugcraft.client.DroneTerminalScreen;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneTier;
import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.TowerData;
import java.util.LinkedHashMap;
import java.util.List;
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
 * Client game test for the Tri-Pillar Drone Tower, with screenshots from a real client: the plinth and core, the tower
 * status screen, the core building the foundation (the control building, its yard and the two exchanges), the six roof
 * pads (tier 1) with their drones, the hall's live desks, master console, wall screens and holo tables, the pillar feet
 * (tier 2) with drones in their bays, drones flying out to build tier 3, and the finished tier 9 tower.
 */
public class TowerClientGameTests implements FabricClientGameTest {
	/** Where the camera stands in the hall, north of the master console, looking at the central table (relative to the core). */
	private static final double[] HALL = {.5, 1, -12.5};

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
			// r178 (Justin, 7 November 2026: "the ship still doesn't have collision ... I'm trying to walk on it"): a drop ship
			// set down is solid on the client too, where the player moves (in single player the client's ship and the
			// server's share an id, so one set of ships kept only the server's)
			BlockPos shipAt = new BlockPos(x - 80, y, z);
			net.minecraft.world.phys.Vec3 shipFoot = net.minecraft.world.phys.Vec3.atBottomCenterOf(shipAt);
			server.runOnServer(minecraft -> {
				var level = minecraft.overworld();
				var ship = JugcraftTower.CREW_SHIP.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
				ship.park(shipFoot, 0F, level.getGameTime());
				level.addFreshEntity(ship);
			});
			context.waitTicks(40);
			boolean[] hull = context.computeOnClient(client -> {
				for (var e : client.level.entitiesForRendering()) {
					if (e instanceof io.github.jimbozoomer.jugcraft.tower.crew.CrewShip ship) {
						return new boolean[] {true, io.github.jimbozoomer.jugcraft.tower.crew.CrewShipHull.solidAt(ship, shipFoot.add(-9.42, 5.0, -5)),
								!io.github.jimbozoomer.jugcraft.tower.crew.CrewShipHull.solidAt(ship, shipFoot.add(0, 4.5, 5))};
					}
				}
				return new boolean[] {false, false, false};
			});
			if (!hull[0]) throw new AssertionError("The parked drop ship never reached the client");
			if (!hull[1] || !hull[2]) throw new AssertionError("The parked drop ship is not solid on the client (flank solid " + hull[1] + ", bay open " + hull[2] + ")");
			server.runOnServer(minecraft -> minecraft.overworld().getEntitiesOfClass(io.github.jimbozoomer.jugcraft.tower.crew.CrewShip.class,
					new net.minecraft.world.phys.AABB(shipAt).inflate(48)).forEach(net.minecraft.world.entity.Entity::discard));
			// The core in the (flat) ground, far enough south that the foundation (z -53..45 round it) leaves the spawn
			// point outside; the hall floor is the ground's level.
			BlockPos core = new BlockPos(x, y - 1, z + 64);
			server.runOnServer(minecraft -> {
				ServerPlayer player = minecraft.getPlayerList().getPlayers().getFirst();
				player.getAbilities().flying = true;
				player.onUpdateAbilities();
				player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(JugcraftTower.CORE.asItem()));
			});
			setHudHidden(context, true);

			// Holding the core: the amber ghost of the foundation where it would go, then of the finished tower.
			server.runCommand("tp @p %d.5 %d %d.5 facing %d.5 %d %d.5".formatted(core.getX(), y + 9, core.getZ() + 20, core.getX(), y - 1, core.getZ()));
			context.waitTicks(10);
			context.getInput().lookAt(core);
			context.waitTicks(5);
			context.runOnClient(client -> io.github.jimbozoomer.jugcraft.client.TowerCorePreview.setStage(0));
			context.takeScreenshot("jugcraft_tripillar_core_preview");
			server.runCommand("tp @p %d.5 %d %d.5".formatted(core.getX(), y + 40, core.getZ() + 120));
			context.waitTicks(10);
			context.getInput().lookAt(core);
			context.runOnClient(client -> io.github.jimbozoomer.jugcraft.client.TowerCorePreview.setStage(9));
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_tripillar_core_preview_tier9");
			context.runOnClient(client -> io.github.jimbozoomer.jugcraft.client.TowerCorePreview.setStage(0));
			server.runOnServer(minecraft -> {
				ServerLevel level = minecraft.overworld();
				ServerPlayer player = minecraft.getPlayerList().getPlayers().getFirst();
				player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				level.setBlock(core, JugcraftTower.CORE.defaultBlockState(), 3);
				TowerCoreBlockEntity entity = (TowerCoreBlockEntity) level.getBlockEntity(core);
				entity.setOwner(player.getUUID());
			});

			// The core alone in the ground, and the status screen before anything is in (the next step is the foundation).
			server.runCommand("tp @p %d.5 %d %d.5 180 35".formatted(core.getX(), y + 6, core.getZ() + 14));
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_tripillar_core");
			context.setScreen(() -> new TowerScreen(core));
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_tripillar_screen_foundation");
			context.setScreen(() -> null);
			// Part of it loaded (a few kinds in full, one half in), then all of it.
			server.runOnServer(minecraft -> {
				var entity = (TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core);
				var price = new java.util.ArrayList<>(entity.layout().cost(entity.layout().foundation).entrySet());
				for (int i = 0; i < price.size(); i += 3) {
					int n = i == 0 ? price.get(i).getValue() / 2 : price.get(i).getValue();
					while (n > 0) {
						n -= Math.max(1, entity.deposit(new ItemStack(price.get(i).getKey(), Math.min(64, n))));
					}
				}
			});
			context.waitTicks(3);
			context.setScreen(() -> new TowerScreen(core));
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_tripillar_screen_foundation_loading");
			context.setScreen(() -> null);
			server.runOnServer(minecraft -> ((TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core)).stockAll());
			context.waitTicks(3);
			context.setScreen(() -> new TowerScreen(core));
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_tripillar_screen_foundation_ready");
			context.setScreen(() -> null);

			// UPGRADE: the core builds the foundation itself (the control building, its yard and the exchanges).
			int foundationTicks = server.computeOnServer(minecraft -> {
				var entity = (TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core);
				entity.tryUpgrade(minecraft.getPlayerList().getPlayers().getFirst());
				if (!entity.buildingFoundation()) {
					throw new AssertionError("UPGRADE did not start the foundation: state " + entity.foundationState());
				}
				TowerData.Tier foundation = entity.layout().foundation;
				// Cells already empty cost only a look (up to 4096 a tick); every block placed costs one of the tick's 64.
				return (foundation.air.length + 4095) / 4096 + (foundation.place.length + TowerCoreBlockEntity.TRIPILLAR_BLOCKS_PER_TICK - 1)
						/ TowerCoreBlockEntity.TRIPILLAR_BLOCKS_PER_TICK + 200;
			});
			context.waitTicks(200);
			reviewPhoto(context, server, core, -40.5, 35, 50.5, core.offset(0, 6, 0), "jugcraft_tripillar_foundation_building");
			waitForFoundation(context, server, core, foundationTicks);
			assertFoundationStands(server, core);
			singleplayer.getConnection().waitForChunksRender();
			context.waitTicks(20);
			reviewPhoto(context, server, core, -40.5, 35, 50.5, core.offset(0, 6, 0), "jugcraft_tripillar_foundation");
			// The cargo hall (layout 40): the Case Packer, the pack spot, the drones' lane up to the goods lift; and the exchanges.
			// Until the finished tower the client sees five chunks round the camera: every camera stays within 63 blocks of the core.
			reviewPhoto(context, server, core, 29.5, 3, 8.5, core.offset(TriPillarArena.CASE_PACKER), "jugcraft_tripillar_cargo_hall");
			reviewPhoto(context, server, core, 48.5, 2, -30.5, core.offset(57, 3, -30), "jugcraft_tripillar_energy_exchange");
			reviewPhoto(context, server, core, 63.5, 2, 4.5, core.offset(56, 3, 0), "jugcraft_tripillar_cargo_exchange");
			reviewPhoto(context, server, core, HALL[0], HALL[1], HALL[2], core.offset(0, 3, 0), "jugcraft_tripillar_hall_foundation");
			context.setScreen(() -> new TowerScreen(core));
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_tripillar_screen_tier0");
			context.setScreen(() -> null);

			// UPGRADE again: the core puts the six roof pads (tier 1) on the control building.
			int firstTierTicks = server.computeOnServer(minecraft -> {
				var entity = (TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core);
				entity.tryUpgrade(minecraft.getPlayerList().getPlayers().getFirst());
				if (entity.building() != 1) {
					throw new AssertionError("UPGRADE did not start the roof pads: building " + entity.building());
				}
				var first = entity.layout().tier(1);
				return (first.place.length + first.air.length + TowerCoreBlockEntity.TRIPILLAR_BLOCKS_PER_TICK - 1)
						/ TowerCoreBlockEntity.TRIPILLAR_BLOCKS_PER_TICK + 100;
			});
			waitForTier(context, server, core, 1, firstTierTicks);
			// A drone on each of the 24 roof docks: the tier's whole fleet.
			BlockPos terminalPos = core.offset(TriPillarArena.TERMINAL);
			server.runOnServer(minecraft -> {
				DroneTerminalBlockEntity terminal = ((TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core)).terminal();
				ServerPlayer player = minecraft.getPlayerList().getPlayers().getFirst();
				if (terminal == null || !terminal.getBlockPos().equals(terminalPos)) {
					throw new AssertionError("The tower's terminal is not in the yard: " + terminal);
				}
				for (int i = 0; i < 24; i++) {
					if (!terminal.linkDrone(player, DroneTier.COURIER_QUAD)) {
						throw new AssertionError("Roof drone " + i + " did not link");
					}
				}
				if (terminal.linkDrone(player, DroneTier.COURIER_QUAD)) {
					throw new AssertionError("A 25th drone linked to a tier 1 tower of 24");
				}
				for (int i = 0; i < 24; i++) {
					if (terminal.dockOf(i).y != core.getY() + TriPillarArena.ROOF_Y + 1) {
						throw new AssertionError("Drone " + i + " is not docked on a roof pad: " + terminal.dockOf(i));
					}
				}
			});
			waitForClientDocks(context, server, terminalPos, 24);
			reviewPhoto(context, server, core, 44.5, 32, 44.5, core.offset(26, 21, 28), "jugcraft_tripillar_roof_pads");
			reviewPhoto(context, server, core, .5, 50, 60.5, core.offset(0, 20, 0), "jugcraft_tripillar_tier1");

			// The hall: its desks, master console and wall screens show the depot, the central table the tower.
			reviewPhoto(context, server, core, HALL[0], HALL[1], HALL[2], core.offset(0, 3, 0), "jugcraft_tripillar_hall");
			io.github.jimbozoomer.jugcraft.client.TowerRenderChecks.hologramFitsTable(context, core);
			context.takeScreenshot("jugcraft_tripillar_hall_hologram");
			reviewPhoto(context, server, core, .5, 1, -4.0, core.offset(0, 5, 14), "jugcraft_tripillar_hall_video_wall");
			context.setScreen(() -> new TowerScreen(core));
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_tripillar_screen_tier1");
			context.setScreen(() -> null);
			masterControls(context, server, core);

			// Tier 2 (the pillar feet): its 20 bays, with the 24 drones on the roof the tier's full fleet of 44.
			server.runOnServer(minecraft -> {
				ServerLevel level = minecraft.overworld();
				TowerCoreBlockEntity entity = (TowerCoreBlockEntity) level.getBlockEntity(core);
				entity.buildInstantly(level, 2);
				ServerPlayer player = minecraft.getPlayerList().getPlayers().getFirst();
				DroneTerminalBlockEntity terminal = entity.terminal();
				terminal.energy().setAmount(DroneTerminalBlockEntity.ENERGY_CAPACITY);
				for (int i = 0; i < 20; i++) {
					if (!terminal.linkDrone(player, DroneTier.SURVEY_HEXACOPTER)) {
						throw new AssertionError("Bay drone " + i + " did not link");
					}
				}
				if (entity.tier() != 2 || terminal.fleet().size() != 44 || terminal.maxDrones() != 44) {
					throw new AssertionError("Tier 2 does not hold its 44 drones: tier " + entity.tier() + ", " + terminal.fleet().size() + "/" + terminal.maxDrones());
				}
			});
			singleplayer.getConnection().waitForChunksRender();
			waitForClientDocks(context, server, terminalPos, 44);
			terminalScreens(context, server, terminalPos);
			reviewPhoto(context, server, core, -1.5, 23, 38.5, core.offset(-2, 22, 26), "jugcraft_tripillar_pillar_foot_bay");
			reviewPhoto(context, server, core, 30.5, 40, 60.5, core.offset(0, 24, 0), "jugcraft_tripillar_tier2");
			io.github.jimbozoomer.jugcraft.client.TowerRenderChecks.cradleStruts(context, core);

			// UPGRADE to tier 3: drones fly out of their bays and bring the new floors' tiles in.
			server.runOnServer(minecraft -> ((TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core))
					.tryUpgrade(minecraft.getPlayerList().getPlayers().getFirst()));
			// Over pillar A's foot, by the supply cradle every flight starts at.
			server.runCommand("tp @p %s %s %s facing %s %s %s".formatted(core.getX() + .5, core.getY() + 44.0, core.getZ() + 40.5,
					core.getX() + .5, core.getY() + 38.5, core.getZ() + 10.5));
			int sounds = 0;
			boolean delivered = false;
			for (int i = 0; i < 40 && (!delivered || sounds <= 0); i++) {
				charge(server, core);
				context.waitTicks(20);
				sounds = Math.max(sounds, context.computeOnClient(client -> io.github.jimbozoomer.jugcraft.client.DroneSounds.playing()));
				if (i == 8) {
					context.takeScreenshot("jugcraft_tripillar_drones_building");
				}
				delivered = server.computeOnServer(minecraft -> {
					var entity = (TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core);
					return entity.tier() >= 3 || entity.filledTiles().stream().anyMatch(index -> entity.layout().tier(3).tiles.get(index).blocks().length > 0);
				});
			}
			context.waitTicks(40);
			sounds = Math.max(sounds, context.computeOnClient(client -> io.github.jimbozoomer.jugcraft.client.DroneSounds.playing()));
			context.takeScreenshot("jugcraft_tripillar_tier3_tiles");
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
			// The operator command used to review the tower, from the control building's roof (within its three chunks of the core).
			server.runCommand("tp @p %s %s %s".formatted(core.getX() + .5, core.getY() + TriPillarArena.ROOF_Y + 1.0, core.getZ() - 24.5));
			server.runCommand("execute as @p at @s run dronetest build 9");
			server.runOnServer(minecraft -> {
				var tower = (TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core);
				if (tower.tier() != 9 || tower.layoutVersion() != TowerData.CURRENT_LAYOUT) {
					throw new AssertionError("The review command did not build the Tri-Pillar to tier 9: tier " + tower.tier() + ", layout " + tower.layoutVersion());
				}
			});
			try {
				server.waitFor(minecraft -> minecraft.getPlayerList().getPlayers().getFirst().requestedViewDistance() == 14, 200);
			} catch (AssertionError timeout) {
				throw new AssertionError("The finished-tower camera options were not advertised: "
						+ towerCameraDistances(context, server), timeout);
			}
			System.out.println("[Jugcraft tower test] finished camera visibility: " + towerCameraDistances(context, server));
			BlockPos towerCenter = core.offset(0, 125, 0);
			server.runCommand("tp @p %s %s %s facing %s %s %s".formatted(
					core.getX() + .5 - 90, core.getY() + 120.0, core.getZ() + .5 + 140,
					towerCenter.getX() + .5, towerCenter.getY() + .5, towerCenter.getZ() + .5));
			context.getInput().lookAt(towerCenter);
			waitForTowerChunks(context, server, core);
			context.waitTicks(100);
			context.getInput().lookAt(towerCenter);
			context.waitTicks(5);
			assertTowerCamera(context, core, towerCenter);
			context.takeScreenshot("jugcraft_tripillar_tier9");
			context.runOnClient(client -> client.options.fov().set(70));
			BlockPos baseCenter = core.offset(0, 20, 0);
			server.runCommand("tp @p %s %s %s facing %s %s %s".formatted(
					core.getX() + .5 - 60, core.getY() + 45.0, core.getZ() + .5 + 75,
					baseCenter.getX() + .5, baseCenter.getY() + .5, baseCenter.getZ() + .5));
			context.getInput().lookAt(baseCenter);
			waitForTowerChunks(context, server, core);
			context.waitTicks(40);
			context.getInput().lookAt(baseCenter);
			context.waitTicks(5);
			assertTowerCamera(context, core, baseCenter);
			context.takeScreenshot("jugcraft_tripillar_tier9_base");
			reviewPhoto(context, server, core, HALL[0], HALL[1], HALL[2], core.offset(0, 3, 0), "jugcraft_tripillar_hall_complete");
			io.github.jimbozoomer.jugcraft.client.TowerRenderChecks.hologramFitsTable(context, core);
			context.takeScreenshot("jugcraft_tripillar_hall_hologram_complete");
			reviewCloseups(context, server, core);
			setHudHidden(context, false);
		}
	}

	/** Native in-game review of the working areas, by day and at night. */
	private static void reviewCloseups(ClientGameTestContext context, TestServerContext server, BlockPos core) {
		context.runOnClient(client -> client.options.fov().set(85));
		reviewPhoto(context, server, core, 60.5, 14, 8.5, core.offset(52, 1, -12), "jugcraft_tripillar_yard_daylight");
		context.runOnClient(client -> client.options.fov().set(70));
		// the Tower Control Console (item 6): from the rotunda, from behind the TOWER CONTROL chair, at its switch box
		reviewPhoto(context, server, core, 4.5, 1, -6.0, core.offset(-1, 2, -10), "jugcraft_tripillar_control_console");
		reviewPhoto(context, server, core, .5, 1.19, -11.6, core.offset(0, 2, -8), "jugcraft_tripillar_control_console_operator");
		reviewPhoto(context, server, core, -.6, 1.19, -10.45, core.offset(-1, 1, -9), "jugcraft_tripillar_control_console_switches");
		// the Curved Video Walls (item 7) on the rotunda's cut faces (item 19): A (OPERATIONS) from behind the console, B (FLEET
		// BOARD) and C (LOGISTICS) across the rotunda, A's end post and plinth up close, the ticker running on over A, the new
		// riser ladder in pillar C's foot
		reviewPhoto(context, server, core, .5, 1.19, -7.0, core.offset(0, 5, 14), "jugcraft_tripillar_video_wall_operations");
		reviewPhoto(context, server, core, -6.5, 1, 4.5, core.offset(12, 5, -8), "jugcraft_tripillar_video_wall_fleet");
		reviewPhoto(context, server, core, 7.5, 1, 4.5, core.offset(-12, 5, -8), "jugcraft_tripillar_video_wall_logistics");
		reviewPhoto(context, server, core, 6.0, 1, 9.6, core.offset(9, 2, 12), "jugcraft_tripillar_video_wall_closeup");
		reviewPhoto(context, server, core, .5, 1, 3.5, core.offset(0, 10, 14), "jugcraft_tripillar_video_wall_ticker");
		reviewPhoto(context, server, core, -18.5, 1, -11.5, core.offset(-21, 3, -14), "jugcraft_tripillar_riser_ladder");
		// the Drone Display Cases (item 1) past the gates: T4 and T1 from the visitors' path, the drone through the glass, the
		// ticker rail turning the light hood's corner
		reviewPhoto(context, server, core, -6.5, 1, -32.5, core.offset(-14, 2, -38), "jugcraft_tripillar_display_case_t4");
		reviewPhoto(context, server, core, 7.5, 1, -32.5, core.offset(13, 2, -38), "jugcraft_tripillar_display_case_t1");
		reviewPhoto(context, server, core, -13.5, 1, -33.6, core.offset(-14, 1, -38), "jugcraft_tripillar_display_case_inside");
		reviewPhoto(context, server, core, -9.0, 1, -32.0, core.offset(-12, 4, -36), "jugcraft_tripillar_display_case_ticker");
		// the Crystal Tree (item 2): from the lobby, under its crown, its roots, its crystals up close
		reviewPhoto(context, server, core, 4.0, 1, -33.5, core.offset(14, 4, -27), "jugcraft_tripillar_crystal_tree");
		reviewPhoto(context, server, core, 12.3, 1.2, -24.1, core.offset(14, 6, -27), "jugcraft_tripillar_crystal_tree_crown");
		reviewPhoto(context, server, core, 11.6, 2.0, -24.0, core.offset(14, 1, -27), "jugcraft_tripillar_crystal_tree_roots");
		reviewPhoto(context, server, core, 12.0, 1.4, -25.0, core.offset(13, 3, -26), "jugcraft_tripillar_crystal_tree_crystals");
		// the lounge round the Long Coffee Table (item 3): from the lobby, from its open end, the north line of the ticker
		// as the north sofa reads it, the plant's mugs and the T1 model from the south
		reviewPhoto(context, server, core, -6.4, 1, -29.6, core.offset(-14, 1, -27), "jugcraft_tripillar_lounge");
		reviewPhoto(context, server, core, -10.6, 1, -27.0, core.offset(-15, 1, -27), "jugcraft_tripillar_long_coffee_table");
		reviewPhoto(context, server, core, -14.5, 1, -31.4, core.offset(-15, 1, -28), "jugcraft_tripillar_long_coffee_table_ticker");
		reviewPhoto(context, server, core, -13.2, 1, -23.4, core.offset(-14, 1, -27), "jugcraft_tripillar_long_coffee_table_mugs");
		// the service bay's robot arm (item 8) at work over the T4, which lies side-on to it: from the south-east, side-on at
		// the wing's height (clear air under the arm), from the parts wall, and its base with the keep-clear square
		reviewPhoto(context, server, core, 29.4, 1.45, -24.6, core.offset(26, 1, -30), "jugcraft_tripillar_service_arm");
		reviewPhoto(context, server, core, 26.6, 1.2, -25.9, core.offset(26, 2, -30), "jugcraft_tripillar_service_arm_side");
		reviewPhoto(context, server, core, 28.7, 2.0, -32.6, core.offset(26, 2, -30), "jugcraft_tripillar_service_arm_north");
		reviewPhoto(context, server, core, 29.6, 1.0, -27.6, core.offset(27, 1, -30), "jugcraft_tripillar_service_arm_base");
		// the workshop's Drone Parts Shelving (item 9): both units from the bay, unit a's batteries charging up close (the
		// gauges, slot lights and both screens live), its hardware bins, and its end frame with the extinguisher
		reviewPhoto(context, server, core, 20.6, 1.15, -29.4, core.offset(25, 2, -35), "jugcraft_tripillar_parts_shelving");
		reviewPhoto(context, server, core, 23.75, 0.5, -32.85, core.offset(23, 1, -35), "jugcraft_tripillar_parts_shelving_batteries");
		reviewPhoto(context, server, core, 23.55, 0.9, -32.95, core.offset(23, 2, -35), "jugcraft_tripillar_parts_shelving_bins");
		reviewPhoto(context, server, core, 26.35, 0.45, -32.4, core.offset(25, 1, -35), "jugcraft_tripillar_parts_shelving_extinguisher");
		// the data hall (item 13): from the operators' floor through its glass wall, down the contained cold aisle, the east
		// containment door, the UPS lineup and CRAC-2 (their screens live), CRAC-1 and the FM-200 cylinders
		reviewPhoto(context, server, core, -27.5, 1.0, -0.5, core.offset(-46, 3, -1), "jugcraft_tripillar_data_hall");
		reviewPhoto(context, server, core, -40.6, 1.0, -0.45, core.offset(-48, 2, -1), "jugcraft_tripillar_data_hall_cold_aisle");
		reviewPhoto(context, server, core, -37.6, 1.0, 1.75, core.offset(-40, 2, -1), "jugcraft_tripillar_data_hall_door");
		reviewPhoto(context, server, core, -38.4, 1.0, 3.6, core.offset(-44, 1, 7), "jugcraft_tripillar_data_hall_ups");
		reviewPhoto(context, server, core, -38.4, 1.0, -4.4, core.offset(-44, 1, -8), "jugcraft_tripillar_data_hall_fm200");
		// the Holo Command Table (item 5): from the south at its FLEET station, up close at a screen, and over it from the north-west
		reviewPhoto(context, server, core, .5, 1, 9.5, core.offset(0, 2, 2), "jugcraft_tripillar_holo_command_table");
		reviewPhoto(context, server, core, .5, 1, 6.9, core.offset(0, 1, 4), "jugcraft_tripillar_holo_command_table_station");
		reviewPhoto(context, server, core, -9.5, 4, -9.5, core.offset(0, 1, 0), "jugcraft_tripillar_holo_command_table_above");
		context.runOnClient(client -> client.options.fov().set(45));
		reviewPhoto(context, server, core, -4.5, 1, -33.5, core.offset(-5, 1, -36), "jugcraft_tripillar_desk_live_readout");
		// the remade desks (item 14): from behind the chair at the flight director's, power's, the data hall's and the map
		// table's desks (each its role's three live pages), and the west operator rows from the data hall's glass
		reviewPhoto(context, server, core, -21.05, 1, 8.5, core.offset(-20, 1, 8), "jugcraft_tripillar_desk_flight_director");
		reviewPhoto(context, server, core, 22.05, 1, 8.5, core.offset(20, 1, 8), "jugcraft_tripillar_desk_power");
		reviewPhoto(context, server, core, 32.05, 1, 8.5, core.offset(30, 1, 8), "jugcraft_tripillar_desk_data_hall");
		context.runOnClient(client -> client.options.fov().set(70));
		reviewPhoto(context, server, core, -34.5, 1.8, 10.5, core.offset(-20, 1, 10), "jugcraft_tripillar_desks_west");
		reviewPhoto(context, server, core, 44.5, 32, 44.5, core.offset(26, 21, 28), "jugcraft_tripillar_roof_pads_complete");
		reviewPhoto(context, server, core, -1.5, 23, 38.5, core.offset(-2, 22, 26), "jugcraft_tripillar_pillar_foot_bay_complete");
		// Every hall fitting in place and working.
		reviewPhoto(context, server, core, 7.5, 11, -7.5, core.offset(0, 13, 0), "jugcraft_tripillar_hologram_close");
		reviewPhoto(context, server, core, 32.5, 2, -24.5, core.offset(27, 2, -30), "jugcraft_tripillar_service_bay");
		// the Holo Projector (item 15; it replaced the floating holo panels): the head watching whoever comes through the
		// gates, up close under it from the front-left (its eyes on the camera), and from the lounge, out of its reach, at
		// rest facing the entrance with its ring of words turning (those behind it fainter)
		reviewPhoto(context, server, core, .5, 1, -38.5, core.offset(0, 6, -33), "jugcraft_tripillar_holo_head");
		reviewPhoto(context, server, core, -3.5, 1, -37.0, core.offset(0, 6, -33), "jugcraft_tripillar_holo_head_close");
		reviewPhoto(context, server, core, -22.5, 2, -33.5, core.offset(0, 6, -33), "jugcraft_tripillar_holo_head_idle");
		reviewPhoto(context, server, core, 4.5, 1.5, -29.5, core.offset(0, 2, -41), "jugcraft_tripillar_entrance_gates");
		// the Maintenance Doors (item 10): pillar A's riser room off the hall, its door off the roof under the hood, pillar
		// B's on the tier-3 deck by the lift core, and the active leaf's lever and NOTICE up close
		reviewPhoto(context, server, core, 4.6, 1, 32.5, core.offset(1, 2, 26), "jugcraft_tripillar_maintenance_door_hall");
		reviewPhoto(context, server, core, 15.5, 21, 13.0, core.offset(9, 22, 19), "jugcraft_tripillar_maintenance_door_roof");
		reviewPhoto(context, server, core, 1.6, 37, -7.4, core.offset(6, 38, -3), "jugcraft_tripillar_maintenance_door_deck");
		reviewPhoto(context, server, core, 0.9, 1, 28.4, core.offset(0, 2, 26), "jugcraft_tripillar_maintenance_door_notice");
		// the neon light lines (item 11): pillar A's foot (its band and corner tubes), the atrium's edge with the ticker on
		// it, the lobby's floor lines, a line and a tube up close
		reviewPhoto(context, server, core, .5, 1, 37.5, core.offset(0, 3, 26), "jugcraft_tripillar_neon_pillar_foot");
		reviewPhoto(context, server, core, .5, 1, 3.5, core.offset(0, 9, 14), "jugcraft_tripillar_neon_atrium_edge");
		reviewPhoto(context, server, core, 3.5, 1, -27.5, core.offset(-8, 0, -38), "jugcraft_tripillar_neon_floor_lines");
		reviewPhoto(context, server, core, 3.6, 1.3, 28.4, core.offset(5, 3, 26), "jugcraft_tripillar_neon_closeup");
		// the joined girders (item 12): the struts under pillar A's supply cradle (tier 2), a foot on the chamfer, a head under
		// the deck, the bracing; the entrance canopy's posts, a post's base plate
		reviewPhoto(context, server, core, -2.0, 21.0, 40.0, core.offset(-10, 31, 27), "jugcraft_tripillar_cradle_struts");
		reviewPhoto(context, server, core, -5.4, 29.0, 31.2, core.offset(-9, 29, 25), "jugcraft_tripillar_cradle_struts_foot");
		reviewPhoto(context, server, core, -9.4, 32.4, 30.9, core.offset(-12, 35, 28), "jugcraft_tripillar_cradle_struts_head");
		reviewPhoto(context, server, core, -6.6, 29.2, 32.6, core.offset(-10, 31, 27), "jugcraft_tripillar_cradle_struts_bracing");
		reviewPhoto(context, server, core, 4.5, 1.0, -62.0, core.offset(-1, 4, -53), "jugcraft_tripillar_canopy_posts");
		reviewPhoto(context, server, core, -6.2, 1.0, -55.3, core.offset(-8, 1, -53), "jugcraft_tripillar_canopy_post_base");
		// the map room (item 16): from the gallery through its glass front (the sign over the doors), inside at the table,
		// over the map, and a corner panel's page up close
		server.runCommand("tp @p %s %s %s".formatted(core.getX() + 1.5, core.getY() + 12.0, core.getZ() - 26.5));
		io.github.jimbozoomer.jugcraft.client.TowerRenderChecks.bigMapTable(context, core);
		reviewPhoto(context, server, core, 1.0, 12.0, -14.5, core.offset(1, 14, -34), "jugcraft_tripillar_map_room");
		reviewPhoto(context, server, core, 9.5, 12.0, -26.5, core.offset(1, 13, -35), "jugcraft_tripillar_map_room_table");
		reviewPhoto(context, server, core, -4.5, 12.0, -27.0, core.offset(4, 12, -38), "jugcraft_tripillar_map_room_map");
		reviewPhoto(context, server, core, 7.9, 12.0, -27.3, core.offset(6, 12, -29), "jugcraft_tripillar_map_room_panel");
		// the hall's small table bearing the tower diorama (redesign 3): from its south-east corner, up close from its edge
		server.runCommand("tp @p %s %s %s".formatted(core.getX() - 21.1, core.getY() + 1.0, core.getZ() - 22.3));
		io.github.jimbozoomer.jugcraft.client.TowerRenderChecks.towerDiorama(context, core);
		reviewPhoto(context, server, core, -21.1, 1.0, -22.3, core.offset(-24, 3, -25), "jugcraft_tripillar_tower_diorama");
		reviewPhoto(context, server, core, -21.6, 1.0, -23.4, core.offset(-24, 2, -25), "jugcraft_tripillar_tower_diorama_close");
		// the ground floor's rooms (item 22, part A; the restroom stands where this older lounge view looked from): the
		// security office, the crew cafe, the locker room and its shower stalls, the battery and UPS room, the restroom
		// and the accessible washroom
		reviewPhoto(context, server, core, 25.5, 1.0, 3.5, core.offset(28, 2, 0), "jugcraft_tripillar_room_security_front");
		reviewPhoto(context, server, core, 34.5, 1.0, 2.5, core.offset(31, 1, -4), "jugcraft_tripillar_room_security");
		// (the battery room's doors are in its west wall, on the hall, since layout 49)
		reviewPhoto(context, server, core, 17.5, 1.0, 23.5, core.offset(24, 2, 24), "jugcraft_tripillar_room_battery_front");
		reviewPhoto(context, server, core, 16.5, 1.0, 31.4, core.offset(-6, 1, 40), "jugcraft_tripillar_room_cafe");
		reviewPhoto(context, server, core, -29.5, 1.0, 22.6, core.offset(-28, 1, 37), "jugcraft_tripillar_room_lockers");
		reviewPhoto(context, server, core, -30.0, 1.0, 31.5, core.offset(-25, 1, 36), "jugcraft_tripillar_room_showers");
		reviewPhoto(context, server, core, 32.0, 1.0, 26.0, core.offset(25, 1, 33), "jugcraft_tripillar_room_battery");
		reviewPhoto(context, server, core, -22.4, 1.0, -32.6, core.offset(-27, 1, -34), "jugcraft_tripillar_room_restroom");
		reviewPhoto(context, server, core, -25.6, 1.0, -38.5, core.offset(-22, 1, -43), "jugcraft_tripillar_room_washroom");
		// the upper floor's rooms (item 22, part B), from where the approved mockups looked: the briefing room, the manager's
		// office, the engineering lab, first aid and crew rest, the simulators, the crew lounge, the two
		// offices, the training room (its lesson screen), the meeting room, the men's and the women's rooms
		reviewPhoto(context, server, core, 23.6, 12.0, -23.6, core.offset(14, 13, -34), "jugcraft_tripillar_room_briefing");
		reviewPhoto(context, server, core, 35.5, 12.0, -23.5, core.offset(27, 12, -31), "jugcraft_tripillar_room_manager");
		reviewPhoto(context, server, core, 29.6, 12.0, 4.4, core.offset(40, 12, -15), "jugcraft_tripillar_room_lab");
		reviewPhoto(context, server, core, 31.6, 12.0, 12.8, core.offset(39, 12, 8), "jugcraft_tripillar_room_first_aid");
		reviewPhoto(context, server, core, 31.6, 12.0, 15.4, core.offset(36, 12, 19), "jugcraft_tripillar_room_crew_rest");
		reviewPhoto(context, server, core, -15.0, 12.0, 7.0, core.offset(-30, 12, 17), "jugcraft_tripillar_room_simulators");
		reviewPhoto(context, server, core, -17.0, 12.0, 31.5, core.offset(6, 12, 41), "jugcraft_tripillar_room_lounge");
		reviewPhoto(context, server, core, 24.5, 12.0, 23.5, core.offset(30, 12, 36), "jugcraft_tripillar_room_offices_se");
		reviewPhoto(context, server, core, -23.5, 12.0, 23.5, core.offset(-30, 12, 36), "jugcraft_tripillar_room_offices_sw");
		reviewPhoto(context, server, core, -50.0, 12.0, -1.0, core.offset(-39, 13, 0), "jugcraft_tripillar_room_training");
		reviewPhoto(context, server, core, -30.0, 12.0, -6.6, core.offset(-34, 12, -15), "jugcraft_tripillar_room_meeting");
		reviewPhoto(context, server, core, -20.4, 12.0, -33.6, core.offset(-23, 12, -38), "jugcraft_tripillar_room_men");
		reviewPhoto(context, server, core, -17.4, 12.0, -33.6, core.offset(-13, 12, -39), "jugcraft_tripillar_room_women");
		reviewPhoto(context, server, core, 24.5, 38, 24.5, core.offset(0, 30, 0), "jugcraft_tripillar_lantern");
		server.runCommand("time set midnight");
		context.waitTicks(40);
		reviewPhoto(context, server, core, HALL[0], HALL[1], HALL[2], core.offset(0, 3, 0), "jugcraft_tripillar_hall_night");
		reviewPhoto(context, server, core, 60.5, 14, 8.5, core.offset(52, 1, -12), "jugcraft_tripillar_yard_night");
		reviewPhoto(context, server, core, 44.5, 32, 44.5, core.offset(26, 21, 28), "jugcraft_tripillar_roof_pads_night");
		reviewPhoto(context, server, core, .5, 1, -38.5, core.offset(0, 6, -33), "jugcraft_tripillar_holo_head_night");
		reviewPhoto(context, server, core, 15.5, 21, 13.0, core.offset(9, 22, 19), "jugcraft_tripillar_maintenance_door_roof_night");
		reviewPhoto(context, server, core, .5, 1, 37.5, core.offset(0, 3, 26), "jugcraft_tripillar_neon_pillar_foot_night");
		reviewPhoto(context, server, core, .5, 22, 44.0, core.offset(0, 28, 27), "jugcraft_tripillar_neon_tubes_night");
		server.runCommand("time set noon");
	}

	/** The core built its whole foundation: (almost) every block stands, the terminal is the owner's and the hall's displays show it. */
	private static void assertFoundationStands(TestServerContext server, BlockPos core) {
		server.runOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			var tower = (TowerCoreBlockEntity) level.getBlockEntity(core);
			TowerData data = tower.layout();
			int standing = 0;
			for (int[] b : data.foundation.place) {
				if (level.getBlockState(core.offset(b[0], b[1], b[2])).is(data.state(b[3]).getBlock())) {
					standing++;
				}
			}
			if (standing * 100L < data.foundation.place.length * 99L) {
				throw new AssertionError("The control building does not stand: " + standing + " of " + data.foundation.place.length + " blocks");
			}
			ServerPlayer player = minecraft.getPlayerList().getPlayers().getFirst();
			BlockPos terminal = core.offset(TriPillarArena.TERMINAL);
			if (!(level.getBlockEntity(terminal) instanceof DroneTerminalBlockEntity depot) || !player.getUUID().equals(depot.owner())) {
				throw new AssertionError("The yard has no terminal of the tower's owner");
			}
			for (BlockPos table : List.of(TriPillarArena.CENTRAL_TABLE, TriPillarArena.LESSON_SCREEN, TriPillarArena.MASTER_CONSOLE)) {
				if (!(level.getBlockEntity(core.offset(table)) instanceof io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity display)
						|| !terminal.equals(display.terminal())) {
					throw new AssertionError("The hall's display at " + table + " is not pinned to the tower's terminal");
				}
			}
			System.out.println("[Jugcraft tower test] foundation: " + standing + " of " + data.foundation.place.length + " blocks");
		});
	}

	/** Ordinary in-world right-click on the hall's master console, owner mode control, then the real tower upgrade widget. */
	private static void masterControls(ClientGameTestContext context, TestServerContext server, BlockPos core) {
		BlockPos console = core.offset(TriPillarArena.MASTER_CONSOLE);
		server.runOnServer(minecraft -> {
			var player = minecraft.getPlayerList().getPlayers().getFirst();
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, ItemStack.EMPTY);
		});
		// The console faces north, towards its operator.
		server.runCommand("tp @p %s %s %s".formatted(console.getX() + .5, console.getY(), console.getZ() - 2.4));
		context.waitTicks(10); context.getInput().lookAt(console); context.waitTicks(3);
		context.getInput().pressKey(options -> options.keyUse);
		context.waitFor(client -> client.gui.screen() instanceof DroneTerminalScreen);
		context.waitTicks(3); context.takeScreenshot("jugcraft_tripillar_master_console_controls");
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
		context.waitTicks(3); context.takeScreenshot("jugcraft_tripillar_master_console_upgrade_screen");
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
			List<TowerData.Tier> steps = new java.util.ArrayList<>(tower.layout().tiers);
			if (tower.layout().foundation != null) {
				steps.add(tower.layout().foundation);
			}
			for (var tier : steps) {
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
				throw new AssertionError("The tower is outside the client's loaded chunks");
			}
			var direction = net.minecraft.world.phys.Vec3.atCenterOf(target).subtract(client.player.getEyePosition()).normalize();
			if (client.player.getLookAngle().dot(direction) < .999) {
				throw new AssertionError("The tower camera is not aimed at " + target);
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
		context.takeScreenshot("jugcraft_tripillar_terminal_overview");
		context.clickScreenButton("FLEET");
		context.waitTicks(2);
		context.takeScreenshot("jugcraft_tripillar_terminal_fleet");
		context.clickScreenButton("POWER");
		context.waitTicks(2);
		context.takeScreenshot("jugcraft_tripillar_terminal_power");
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
			context.takeScreenshot("jugcraft_tripillar_terminal_jobs_first");
			context.runOnClient(client -> {
				var screen = (DroneTerminalScreen) client.gui.screen();
				for (int i = 0; i < 20; i++) {
					if (!screen.mouseScrolled(screen.width / 2.0 + 25, screen.height / 2.0, 0, -1)) {
						throw new AssertionError("The long missing-material list did not consume the mouse wheel");
					}
				}
			});
			context.waitTicks(2);
			context.takeScreenshot("jugcraft_tripillar_terminal_jobs_last");
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

	/** The client sees {@code count} docked drones at the terminal (the server's view reaches it). */
	private static void waitForClientDocks(ClientGameTestContext context, TestServerContext server, BlockPos terminalPos, int count) {
		try {
			context.waitFor(client -> client.level.getBlockEntity(terminalPos) instanceof DroneTerminalBlockEntity t
					&& t.view() != null && t.view().docks.size() == count, 200);
		} catch (AssertionError timeout) {
			String serverDocks = server.computeOnServer(minecraft -> {
				var terminal = (DroneTerminalBlockEntity) minecraft.overworld().getBlockEntity(terminalPos);
				return terminal.fleet().size() + " drones, first and last docks " + terminal.dockOf(0) + " " + terminal.dockOf(Math.max(0, terminal.fleet().size() - 1));
			});
			String clientDocks = context.computeOnClient(client -> client.level.getBlockEntity(terminalPos) instanceof DroneTerminalBlockEntity t
					&& t.view() != null ? t.view().docks.size() + " docks, first " + (t.view().docks.isEmpty() ? "-" : t.view().docks.getFirst())
					: "no view");
			throw new AssertionError("The client does not see the " + count + " docked drones. Server: " + serverDocks + ". Client: " + clientDocks, timeout);
		}
		System.out.println("[Jugcraft tower test] the client sees all " + count + " docked drones");
	}

	private static void waitForFoundation(ClientGameTestContext context, TestServerContext server, BlockPos core, int maxTicks) {
		for (int i = 0; i < maxTicks; i += 10) {
			int now = server.computeOnServer(minecraft -> minecraft.overworld().getBlockEntity(core) instanceof TowerCoreBlockEntity entity
					? entity.foundationState() : -1);
			if (now == 2) {
				return;
			}
			context.waitTicks(10);
		}
		float progress = server.computeOnServer(minecraft -> ((TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core)).buildProgress());
		throw new AssertionError("The core did not finish the foundation in " + maxTicks + " ticks (progress " + progress + ")");
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
