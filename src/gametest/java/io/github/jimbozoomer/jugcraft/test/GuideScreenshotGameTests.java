package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.client.DroneTerminalScreen;
import io.github.jimbozoomer.jugcraft.client.GuideBookScreen;
import io.github.jimbozoomer.jugcraft.client.TowerScreen;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneTier;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStationBlock;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStations;
import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Takes the screenshots for the Drone Tower guide books (tools/guide_books.py) in a real client, then opens a
 * book page to check it renders. Runs only when the JUGCRAFT_GUIDE_SHOTS environment variable is set
 * (9-guide-shots.bat), and then the other client tests step aside so it is quick. Screenshots are named
 * guide_&lt;name&gt;; tools/guide_shots.py crops them to 16:9 into textures/gui/guide/.
 *
 * <p>The tower is the Tri-Pillar: "tier1" shows the control building with its yard and the roof pads, "energy_port"
 * and "storage_port" the Energy and Cargo Exchanges beside the yard, "tier2" the pillar feet. "hall" (the hall round
 * the central holo table) and "pillar_bay" (a tier 2 hangar bay) are taken too, for pages that want them.
 */
public class GuideScreenshotGameTests implements FabricClientGameTest {
	/** The Tri-Pillar's foundation reaches x -52..62, z -53..45 from its core (every tier stands inside that). */
	private static final int WEST = -52, EAST = 62, NORTH = -53, SOUTH = 45;
	/** Chunks of view for the shots: the harness starts with five, too few for a tower this size. */
	private static final int VIEW = 16;

	public static boolean active() {
		return System.getenv("JUGCRAFT_GUIDE_SHOTS") != null;
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!active()) {
			return;
		}
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			int y = origin.getY();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule spawn_mobs false");
			// The test world is superflat. The core goes on the grass 70 blocks south of the spawn, so the foundation
			// (from 53 north of the core) starts south of where the player stands.
			BlockPos core = new BlockPos(origin.getX(), y - 1, origin.getZ() + 70);
			server.runOnServer(minecraft -> {
				ServerPlayer player = minecraft.getPlayerList().getPlayers().getFirst();
				player.getAbilities().flying = true;
				player.onUpdateAbilities();
			});
			setHudHidden(context, true);
			viewDistance(context, server, VIEW);
			// An instant build skips any block whose chunk is not loaded: keep the tower's whole ground loaded.
			forceload(server, core);

			// Picking the spot: holding the core shows the space the finished tower takes (shift+scroll to tier 9).
			server.runCommand("item replace entity @p weapon.mainhand with jugcraft:drone_tower_core");
			context.runOnClient(client -> io.github.jimbozoomer.jugcraft.client.TowerCorePreview.setStage(9));
			// The preview sits where the player looks; from high up and far back the whole tower's space shows.
			server.runCommand("tp @p %d.5 %d %d.5 180 20".formatted(core.getX(), y + 30, core.getZ() + 58));
			context.runOnClient(client -> client.options.fov().set(85));
			context.waitTicks(10);
			context.getInput().lookAt(core);
			context.waitTicks(10);
			shot(context, "core_preview");
			context.runOnClient(client -> client.options.fov().set(70));
			context.runOnClient(client -> io.github.jimbozoomer.jugcraft.client.TowerCorePreview.setStage(0));
			server.runCommand("item replace entity @p weapon.mainhand with minecraft:air");

			// The core, set into the ground, and the tower screen with the foundation's blocks loaded.
			server.runOnServer(minecraft -> {
				ServerLevel level = minecraft.overworld();
				level.setBlock(core, JugcraftTower.CORE.defaultBlockState(), 3);
				TowerCoreBlockEntity entity = (TowerCoreBlockEntity) level.getBlockEntity(core);
				entity.setOwner(minecraft.getPlayerList().getPlayers().getFirst().getUUID());
			});
			server.runCommand("tp @p %d.5 %d %d.5 facing %d %d %d".formatted(core.getX() + 4, y + 3, core.getZ() + 6, core.getX(), y - 1, core.getZ()));
			context.waitTicks(20);
			shot(context, "core_placed");
			server.runOnServer(minecraft -> ((TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core)).stockAll());
			context.setScreen(() -> new TowerScreen(core));
			context.waitTicks(10);
			shot(context, "tower_screen");
			context.setScreen(() -> null);

			// The foundation (the control building, its yard, the exchanges) and tier 1, the roof pads, seen from the south-east.
			server.runOnServer(minecraft -> ((TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core)).buildInstantly(minecraft.overworld(), 1));
			server.runCommand("tp @p %d.5 %d %d.5 facing %d %d %d".formatted(core.getX() + 78, y + 48, core.getZ() + 88, core.getX() + 5, y + 6, core.getZ() - 4));
			singleplayer.getConnection().waitForChunksRender();
			context.waitTicks(30);
			shot(context, "tier1");
			// The hall: the master console and, beyond it, the central holo table over the core.
			server.runCommand("tp @p %d.5 %d %d.5 facing %d.5 %d %d.5".formatted(core.getX(), y + 2, core.getZ() - 13, core.getX(), y + 1, core.getZ()));
			context.waitTicks(30);
			shot(context, "hall");

			BlockPos energyPort = server.computeOnServer(minecraft -> socket(minecraft.overworld(), core, true));
			BlockPos cargoPort = server.computeOnServer(minecraft -> socket(minecraft.overworld(), core, false));
			BlockPos terminal = server.computeOnServer(minecraft -> ((TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core)).terminal().getBlockPos());
			// Power: the Energy Exchange north of the yard, its socket facing west.
			look(server, energyPort, -1, 0);
			context.waitTicks(20);
			shot(context, "energy_port");
			// Building blocks: the Cargo Exchange south of the yard, its socket facing east.
			look(server, cargoPort, 1, 0);
			context.waitTicks(20);
			shot(context, "storage_port");

			// Creative: a Creative Energy Cell against the energy socket, a Creative Supply Crate in the yard by the terminal.
			BlockPos cell = server.computeOnServer(minecraft -> placeBeside(minecraft.overworld(), energyPort, "jugcraft:creative_energy_cell"));
			look(server, cell, -1, 0);
			context.waitTicks(20);
			shot(context, "creative_energy");
			BlockPos crate = server.computeOnServer(minecraft -> {
				ServerLevel level = minecraft.overworld();
				// On the yard's paving north of the Cargo Exchange, 11 blocks from the terminal (inside the crate's 48).
				BlockPos at = core.offset(60, 1, -6);
				level.setBlock(at, BuiltInRegistries.BLOCK.getValue(Identifier.parse("jugcraft:creative_supply_crate")).defaultBlockState(), 3);
				return at;
			});
			server.runCommand("tp @p %d.5 %d %d.5 facing %d.5 %d.5 %d.5".formatted(crate.getX() + 3, crate.getY() + 3, crate.getZ() + 6,
					crate.getX(), crate.getY(), crate.getZ()));
			context.waitTicks(20);
			shot(context, "creative_supply");

			// Drones linked at the terminal, then tier 2 (the pillar feet) with drones in their hangars.
			server.runOnServer(minecraft -> {
				ServerLevel level = minecraft.overworld();
				TowerCoreBlockEntity entity = (TowerCoreBlockEntity) level.getBlockEntity(core);
				entity.buildInstantly(level, 2);
				DroneTerminalBlockEntity depot = entity.terminal();
				depot.energy().setAmount(DroneTerminalBlockEntity.ENERGY_CAPACITY);
				ServerPlayer player = minecraft.getPlayerList().getPlayers().getFirst();
				for (int i = 0; i < 24; i++) {
					depot.linkDrone(player, i % 2 == 0 ? DroneTier.SURVEY_HEXACOPTER : DroneTier.COURIER_QUAD);
				}
			});
			server.runCommand("tp @p %d.5 %d %d.5 facing %d.5 %d %d.5".formatted(terminal.getX() - 4, terminal.getY() + 2, terminal.getZ(),
					terminal.getX(), terminal.getY(), terminal.getZ()));
			context.waitTicks(20);
			context.setScreen(() -> new DroneTerminalScreen(terminal));
			context.waitTicks(15);
			shot(context, "terminal");
			context.setScreen(() -> null);
			server.runCommand("tp @p %d.5 %d %d.5 facing %d %d %d".formatted(core.getX() - 60, y + 50, core.getZ() + 90, core.getX(), y + 22, core.getZ()));
			singleplayer.getConnection().waitForChunksRender();
			context.waitTicks(40);
			shot(context, "tier2");
			// A pillar foot's tier 2 bay on the south side, from outside its door.
			server.runCommand("tp @p %d %d %d facing %d %d %d".formatted(core.getX() - 1, y + 22, core.getZ() + 37, core.getX() - 1, y + 21, core.getZ() + 25));
			context.waitTicks(30);
			shot(context, "pillar_bay");

			// The finished tower, 262 blocks high.
			server.runOnServer(minecraft -> ((TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core)).buildInstantly(minecraft.overworld(), 9));
			context.runOnClient(client -> client.options.fov().set(80));
			server.runCommand("tp @p %d.5 %d %d.5 facing %d %d %d".formatted(core.getX() - 80, y + 120, core.getZ() + 190, core.getX(), y + 128, core.getZ()));
			singleplayer.getConnection().waitForChunksRender();
			context.waitTicks(100);
			singleplayer.getConnection().waitForChunksRender();
			shot(context, "tier9");
			context.runOnClient(client -> client.options.fov().set(70));

			// A book page, to check it renders (the screenshots above are not in this run's book yet).
			context.setScreen(() -> {
				GuideBookScreen screen = new GuideBookScreen("creative_tower_guide");
				screen.showPage(4);
				return screen;
			});
			context.waitTicks(10);
			context.takeScreenshot("guide_book_page");
			context.setScreen(() -> null);
			setHudHidden(context, false);
		}
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.takeScreenshot("guide_" + name);
	}

	/** Stands 6 blocks out from {@code target} on the side ({@code sx}, {@code sz}) and 3 up, looking at it. */
	private static void look(TestServerContext server, BlockPos target, int sx, int sz) {
		server.runCommand("tp @p %d.5 %d %d.5 facing %d.5 %d.5 %d.5".formatted(target.getX() + sx * 6 + 2, target.getY() + 3, target.getZ() + sz * 6 + 4,
				target.getX(), target.getY(), target.getZ()));
	}

	/** The marked socket of the tower's Energy Exchange (or Cargo Exchange), from the station's own blocks. */
	private static BlockPos socket(ServerLevel level, BlockPos core, boolean energy) {
		ExchangeStationBlock block = energy ? ExchangeStations.ENERGY : ExchangeStations.CARGO;
		TowerCoreBlockEntity tower = (TowerCoreBlockEntity) level.getBlockEntity(core);
		BlockPos master = core.offset(ExchangeStations.masterOffset(block, tower.layoutVersion()));
		var state = level.getBlockState(master);
		if (!state.is(block)) {
			throw new AssertionError("No " + (energy ? "Energy" : "Cargo") + " Exchange at " + master + ": " + state);
		}
		return block.footprint(state).partPos(master, state.getValue(ExchangeStationBlock.FACING), block.portPart(state));
	}

	/** Places a block in the first free cell next to {@code pos} (sides first) and returns where. */
	private static BlockPos placeBeside(ServerLevel level, BlockPos pos, String id) {
		Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
		for (Direction side : new Direction[] {Direction.WEST, Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.UP}) {
			BlockPos at = pos.relative(side);
			if (level.getBlockState(at).isAir()) {
				level.setBlock(at, block.defaultBlockState(), 3);
				return at;
			}
		}
		throw new AssertionError("No room next to " + pos + " for " + id);
	}

	/** Raises the client's and the server's view distance to {@code chunks} and waits until the server uses it for the player. */
	private static void viewDistance(ClientGameTestContext context, TestServerContext server, int chunks) {
		context.runOnClient(client -> {
			client.options.renderDistance().set(chunks);
			// The server also holds a player to the view distance the client last told it.
			client.options.broadcastOptions();
		});
		server.runOnServer(minecraft -> minecraft.getPlayerList().setViewDistance(chunks));
		server.waitFor(minecraft -> minecraft.getPlayerList().getPlayers().getFirst().requestedViewDistance() == chunks, 200);
	}

	/** Force-loads the tower's ground round {@code core} and waits until all of it is loaded. */
	private static void forceload(TestServerContext server, BlockPos core) {
		int x0 = core.getX() + WEST, x1 = core.getX() + EAST, z0 = core.getZ() + NORTH, z1 = core.getZ() + SOUTH;
		server.runCommand("forceload add %d %d %d %d".formatted(x0, z0, x1, z1));
		server.waitFor(minecraft -> {
			ServerLevel level = minecraft.overworld();
			for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) {
				for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
					if (!level.getChunkSource().hasChunk(cx, cz)) {
						return false;
					}
				}
			}
			return true;
		}, 1200);
	}

	private static void setHudHidden(ClientGameTestContext context, boolean hidden) {
		context.runOnClient(client -> {
			if (client.gui.hud.isHidden() != hidden) {
				client.gui.hud.toggle();
			}
		});
	}
}
