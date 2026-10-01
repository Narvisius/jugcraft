package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.client.DroneTerminalScreen;
import io.github.jimbozoomer.jugcraft.client.GuideBookScreen;
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
 */
public class GuideScreenshotGameTests implements FabricClientGameTest {
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
			int x = origin.getX();
			int y = origin.getY();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamerule doDaylightCycle false");
			server.runCommand("gamerule doMobSpawning false");
			BlockPos core = new BlockPos(x, y - 1, origin.getZ() + 40);
			// A clear, flat grass site 81x81 round the core.
			for (int dx = -40; dx <= 40; dx += 27) {
				int x0 = core.getX() + dx, x1 = Math.min(core.getX() + 40, x0 + 26);
				server.runCommand("fill %d %d %d %d %d %d minecraft:dirt".formatted(x0, y - 4, core.getZ() - 40, x1, y - 2, core.getZ() + 40));
				server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x0, y - 1, core.getZ() - 40, x1, y - 1, core.getZ() + 40));
				for (int h = 0; h < 40; h += 5) {
					server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x0, y + h, core.getZ() - 40, x1, y + h + 4, core.getZ() + 40));
				}
			}
			server.runOnServer(minecraft -> {
				ServerPlayer player = minecraft.getPlayerList().getPlayers().getFirst();
				player.getAbilities().flying = true;
				player.onUpdateAbilities();
			});
			setHudHidden(context, true);

			// Picking the spot: the foundation blueprint's preview of the finished tower (shift+scroll to tier 9).
			server.runCommand("item replace entity @p weapon.mainhand with jugcraft:blueprint[minecraft:custom_data={blueprint:\"drone_tower_foundation\"}]");
			server.runCommand("tp @p %d.5 %d %d.5 facing %d %d %d".formatted(core.getX() - 30, y + 30, core.getZ() + 55, core.getX(), y + 10, core.getZ()));
			context.waitTicks(10);
			context.runOnClient(client -> io.github.jimbozoomer.jugcraft.client.blueprint.BlueprintStages.setStage("drone_tower_foundation", 9));
			// The preview sits where the player looks; from high up and far back the whole tower's space shows.
			server.runCommand("tp @p %d.5 %d %d.5 180 20".formatted(core.getX(), y + 30, core.getZ() + 58));
			context.runOnClient(client -> client.options.fov().set(85));
			context.waitTicks(10);
			context.getInput().lookAt(core);
			context.waitTicks(10);
			shot(context, "blueprint_preview");
			context.runOnClient(client -> client.options.fov().set(70));
			context.runOnClient(client -> io.github.jimbozoomer.jugcraft.client.blueprint.BlueprintStages.setStage("drone_tower_foundation", 0));
			server.runCommand("item replace entity @p weapon.mainhand with minecraft:air");

			// The plinth and the core.
			server.runOnServer(minecraft -> {
				ServerLevel level = minecraft.overworld();
				for (int dx = -7; dx <= 7; dx++) {
					for (int dz = -7; dz <= 7; dz++) {
						level.setBlock(core.offset(dx, 0, dz), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 3);
					}
				}
				level.setBlock(core, JugcraftTower.CORE.defaultBlockState(), 3);
				TowerCoreBlockEntity entity = (TowerCoreBlockEntity) level.getBlockEntity(core);
				entity.setOwner(minecraft.getPlayerList().getPlayers().getFirst().getUUID());
				for (String module : JugcraftTower.MODULES) {
					entity.deposit(new ItemStack(JugcraftTower.MODULE_ITEMS.get(module), TowerCoreBlockEntity.MODULE_CAPACITY));
				}
			});
			server.runCommand("tp @p %d.5 %d %d.5 facing %d %d %d".formatted(core.getX() + 6, y + 7, core.getZ() + 13, core.getX(), y - 1, core.getZ()));
			context.waitTicks(20);
			shot(context, "plinth");
			context.setScreen(() -> new TowerScreen(core));
			context.waitTicks(10);
			shot(context, "tower_screen");
			context.setScreen(() -> null);

			// Tier 1, the Command Post with its exchanges.
			server.runOnServer(minecraft -> ((TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core)).buildInstantly(minecraft.overworld(), 1));
			singleplayer.getConnection().waitForChunksRender();
			server.runCommand("tp @p %d.5 %d %d.5 facing %d %d %d".formatted(core.getX() - 16, y + 18, core.getZ() + 38, core.getX(), y + 2, core.getZ()));
			context.waitTicks(30);
			shot(context, "tier1");

			BlockPos energyPort = find(server, core, "energy_exchange_port");
			BlockPos cargoPort = find(server, core, "cargo_exchange_port");
			BlockPos terminal = server.computeOnServer(minecraft -> ((TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core)).terminal().getBlockPos());
			// Power: the Energy Exchange Port on the west edge.
			look(server, energyPort, -1, 0);
			context.waitTicks(20);
			shot(context, "energy_port");
			// Building blocks: the Cargo Exchange Port on the east edge.
			look(server, cargoPort, 1, 0);
			context.waitTicks(20);
			shot(context, "storage_port");

			// Creative: a Creative Energy Cell against the energy port, a Creative Supply Crate just off the field.
			BlockPos cell = server.computeOnServer(minecraft -> placeBeside(minecraft.overworld(), energyPort, "jugcraft:creative_energy_cell"));
			look(server, cell, -1, 0);
			context.waitTicks(20);
			shot(context, "creative_energy");
			BlockPos crate = server.computeOnServer(minecraft -> {
				ServerLevel level = minecraft.overworld();
				BlockPos at = new BlockPos(core.getX() + 3, core.getY() + 1, core.getZ() + 22);
				level.setBlock(at, net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(Identifier.parse("jugcraft:creative_supply_crate")).defaultBlockState(), 3);
				return at;
			});
			server.runCommand("tp @p %d.5 %d %d.5 facing %d.5 %d.5 %d.5".formatted(crate.getX() + 3, crate.getY() + 3, crate.getZ() + 6,
					crate.getX(), crate.getY(), crate.getZ()));
			context.waitTicks(20);
			shot(context, "creative_supply");

			// Drones linked at the terminal, then tier 2 with drones in their hangars.
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
			context.setScreen(() -> new DroneTerminalScreen(terminal));
			context.waitTicks(15);
			shot(context, "terminal");
			context.setScreen(() -> null);
			singleplayer.getConnection().waitForChunksRender();
			server.runCommand("tp @p %d.5 %d %d.5 facing %d %d %d".formatted(core.getX() - 18, y + 20, core.getZ() + 40, core.getX(), y + 8, core.getZ()));
			context.waitTicks(40);
			shot(context, "tier2");

			// The finished tower.
			server.runOnServer(minecraft -> ((TowerCoreBlockEntity) minecraft.overworld().getBlockEntity(core)).buildInstantly(minecraft.overworld(), 9));
			context.runOnClient(client -> client.options.fov().set(110));
			server.runCommand("tp @p %d.5 %d %d.5 -163 0".formatted(core.getX() - 20, y + 105, core.getZ() + 66));
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

	/** The nearest block with this id (jugcraft:...) within 45 blocks of the core. */
	private static BlockPos find(TestServerContext server, BlockPos core, String id) {
		return server.computeOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse("jugcraft:" + id));
			BlockPos best = null;
			for (BlockPos pos : BlockPos.betweenClosed(core.offset(-45, -2, -45), core.offset(45, 12, 45))) {
				if (level.getBlockState(pos).is(block) && (best == null || pos.distSqr(core) < best.distSqr(core))) {
					best = pos.immutable();
				}
			}
			if (best == null) {
				throw new AssertionError("No " + id + " near the tower");
			}
			return best;
		});
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

	private static void setHudHidden(ClientGameTestContext context, boolean hidden) {
		context.runOnClient(client -> {
			if (client.gui.hud.isHidden() != hidden) {
				client.gui.hud.toggle();
			}
		});
	}
}
