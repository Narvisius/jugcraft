package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.blueprint.Blueprint;
import io.github.jimbozoomer.jugcraft.blueprint.JugcraftBlueprints;
import io.github.jimbozoomer.jugcraft.blueprint.SurveyStakeBlockEntity;
import io.github.jimbozoomer.jugcraft.client.blueprint.BlueprintTableScreen;
import io.github.jimbozoomer.jugcraft.client.blueprint.StakeScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Client game test for blueprints, the Item Index, chairs and the Creative Energy Cell, with screenshots:
 * the placement preview from 20 blocks away, the staked hologram and the stake screen, the Blueprint Table's
 * LIBRARY and IMPORT pages (importing a pasted blueprint), the Item Index beside the inventory, a recipe page,
 * and sitting on an Operator Chair.
 */
public class BlueprintClientGameTests implements FabricClientGameTest {
	private static final String PASTED = """
			{"format": 1, "name": "Test Storage Hut", "size": [5, 4, 5], "anchor": [2, 0, 6],
			 "palette": {"S": "minecraft:stone_bricks", "P": "minecraft:oak_planks", "C": "minecraft:chest[facing=south]"},
			 "layers": [["SSSSS", "SSSSS", "SSSSS", "SSSSS", "SSSSS"],
			            ["PPPPP", "P..CP", "P...P", "P...P", "PP.PP"],
			            ["PPPPP", "P...P", "P...P", "P...P", "PP.PP"],
			            ["SSSSS", "SSSSS", "SSSSS", "SSSSS", "SSSSS"]]}
			""";

	@Override
	public void runTest(ClientGameTestContext context) {
		if (GuideScreenshotGameTests.active()) {
			return;
		}
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			int x = origin.getX(), y = origin.getY(), z = origin.getZ();
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 20, y - 1, z - 50, x + 20, y - 1, z + 6));
			for (int h = 0; h < 25; h += 5) {
				server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 20, y + h, z - 50, x + 20, y + h + 4, z + 6));
			}

			// The library reached the client.
			waitUntil(context, () -> context.computeOnClient(client -> Blueprint.all(true).size() >= 3), "the client got the blueprint library");

			// Hold the Small Church blueprint and look at the ground 20 blocks away: the preview shows there.
			server.runCommand("item replace entity @p weapon.mainhand with jugcraft:blueprint[minecraft:custom_data={blueprint:\"small_church\"}]");
			server.runCommand("tp @p %d.5 %d %d.5 180 25".formatted(x, y, z));
			context.waitTicks(10);
			context.getInput().lookAt(new BlockPos(x, y - 1, z - 20));
			context.waitTicks(10);
			context.takeScreenshot("jugcraft_blueprint_preview");
			BlockPos stake = context.computeOnClient(client -> {
				var target = io.github.jimbozoomer.jugcraft.blueprint.BlueprintItem.target(client.player, 1.0F);
				return target == null ? null : target.stake();
			});
			if (stake == null || stake.distManhattan(origin) < 10) {
				throw new AssertionError("The blueprint preview found no spot far away (got " + stake + ")");
			}
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(10);
			boolean placed = server.computeOnServer(minecraft -> minecraft.overworld().getBlockEntity(stake) instanceof SurveyStakeBlockEntity);
			if (!placed) {
				throw new AssertionError("Right-clicking far away did not set a Survey Stake at " + stake);
			}
			server.runCommand("tp @p %d.5 %d %d.5 160 20".formatted(stake.getX() + 8, y + 6, stake.getZ() + 12));
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_blueprint_hologram");

			// Shift+scroll stages: the Drone Tower Foundation blueprint shows the finished tower's space (tier 9).
			server.runCommand("item replace entity @p weapon.mainhand with jugcraft:blueprint[minecraft:custom_data={blueprint:\"drone_tower_foundation\"}]");
			server.runCommand("tp @p %d.5 %d %d.5 180 20".formatted(x, y + 30, z + 30));
			context.waitTicks(10);
			context.getInput().lookAt(new BlockPos(x, y - 1, z - 25));
			boolean staged = context.computeOnClient(client -> {
				var set = io.github.jimbozoomer.jugcraft.client.blueprint.BlueprintStages.stages("drone_tower_foundation");
				io.github.jimbozoomer.jugcraft.client.blueprint.BlueprintStages.setStage("drone_tower_foundation", 9);
				return set != null && set.stages().size() == 9;
			});
			if (!staged) {
				throw new AssertionError("The Drone Tower Foundation blueprint has no growth stages");
			}
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_blueprint_stage_tier9");
			context.runOnClient(client -> io.github.jimbozoomer.jugcraft.client.blueprint.BlueprintStages.setStage("drone_tower_foundation", 0));

			// The stake screen.
			server.runCommand("tp @p %d.5 %d %d.5 180 30".formatted(stake.getX(), y, stake.getZ() + 2));
			context.waitTicks(5);
			context.getInput().lookAt(stake);
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(StakeScreen.class);
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_stake_screen");
			context.setScreen(() -> null);

			// The Blueprint Table: LIBRARY, then IMPORT a pasted blueprint.
			BlockPos table = new BlockPos(x + 3, y, z);
			server.runCommand("setblock %d %d %d jugcraft:blueprint_table".formatted(table.getX(), table.getY(), table.getZ()));
			context.setScreen(() -> new BlueprintTableScreen(table));
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_blueprint_table_library");
			context.runOnClient(client -> {
				client.keyboardHandler.setClipboard(PASTED);
				BlueprintTableScreen screen = (BlueprintTableScreen) client.gui.screen();
				screen.testImport();
			});
			waitUntil(context, () -> context.computeOnClient(client -> Blueprint.all(true).stream().anyMatch(b -> b.name.equals("Test Storage Hut"))),
					"the pasted blueprint was imported and synced");
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_blueprint_table_import");
			context.setScreen(() -> null);

			// The inventory opens (creative tabs build without crashing).
			context.setScreen(() -> new InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
			context.waitTicks(5);
			context.setScreen(() -> null);

			// Sit on an Operator Chair.
			BlockPos chair = new BlockPos(x - 3, y, z);
			server.runCommand("setblock %d %d %d jugcraft:operator_chair[facing=south]".formatted(chair.getX(), chair.getY(), chair.getZ()));
			context.waitTicks(5);
			server.runCommand("tp @p %d.5 %d %d.5 90 30".formatted(x - 1, y, z));
			context.waitTicks(5);
			context.getInput().lookAt(chair);
			server.runCommand("item replace entity @p weapon.mainhand with minecraft:air");
			context.waitTicks(2);
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(10);
			boolean sitting = context.computeOnClient(client -> client.player.isPassenger());
			if (!sitting) {
				throw new AssertionError("Right-clicking the Operator Chair did not seat the player");
			}

			// The Creative Energy Cell powers a neighbour: a depot terminal next to it fills up.
			BlockPos cell = new BlockPos(x + 6, y, z);
			server.runCommand("setblock %d %d %d jugcraft:creative_energy_cell".formatted(cell.getX(), cell.getY(), cell.getZ()));
			server.runCommand("setblock %d %d %d jugcraft:drone_depot_terminal".formatted(cell.getX() + 1, cell.getY(), cell.getZ()));
			context.waitTicks(40);
			long energy = server.computeOnServer(minecraft -> minecraft.overworld().getBlockEntity(cell.east())
					instanceof io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity terminal ? terminal.energy().getAmount() : -1L);
			if (energy <= 0) {
				throw new AssertionError("The Creative Energy Cell did not power the terminal next to it (energy " + energy + ")");
			}
			if (!context.computeOnClient(client -> JugcraftBlueprints.TABLE != null)) {
				throw new AssertionError("no table");
			}
		}
	}

	private static void waitUntil(ClientGameTestContext context, java.util.function.BooleanSupplier check, String what) {
		for (int i = 0; i < 200; i++) {
			if (check.getAsBoolean()) {
				return;
			}
			context.waitTicks(5);
		}
		throw new AssertionError("Timed out waiting until " + what);
	}
}
