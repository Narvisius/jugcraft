package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.powerline.LineKind;
import io.github.jimbozoomer.jugcraft.powerline.LinePlacement;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;

/**
 * Client game test for the MBS placement preview (client/MbsPreview.java), with screenshots: holding a storage tank
 * kit (from afar, close up at the foundation's edge, and with a pillar in the way), a piece of plant equipment, a
 * lattice tower, a wooden pole beside another (its wire), a wall screen, the oval table and a coal generator, each
 * aimed at the ground in front.
 */
public class MbsPreviewClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestFocus.skip("blueprints")) {
			return;
		}
		if (GuideScreenshotGameTests.active()) {
			return;
		}
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> {
					creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
					for (WorldCreationUiState.WorldTypeEntry entry : creator.getNormalPresetList()) {
						if (entry.preset().is(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT)) {
							creator.setWorldType(entry);
						}
					}
				}).create()) {
			singleplayer.getConnection().waitForChunksRender();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamerule doDaylightCycle false");
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			int x = origin.getX(), y = origin.getY(), z = origin.getZ();
			server.runOnServer(minecraft -> {
				var player = minecraft.getPlayerList().getPlayers().getFirst();
				player.getAbilities().flying = true;
				player.onUpdateAbilities();
			});
			setHudHidden(context, true);

			// A medium storage tank kit: the whole tank, painted, on its foundation.
			hold(server, "jugcraft:tank_kit_general_medium");
			shot(context, singleplayer, x, y + 5, z + 18, x, y - 1, z, "jugcraft_mbs_preview_tank");
			shot(context, singleplayer, x + 3, y + 1, z + 3, x, y - 1, z, "jugcraft_mbs_preview_tank_closeup");
			// A pillar where the tank would stand: red, the pillar's cells marked.
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x + 1, y, z - 5, x + 1, y + 2, z - 5));
			shot(context, singleplayer, x, y + 5, z + 18, x, y - 1, z, "jugcraft_mbs_preview_tank_blocked");
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x + 1, y, z - 5, x + 1, y + 2, z - 5));

			// Plant equipment: the emergency diesel generator.
			hold(server, "jugcraft:emergency_diesel_generator");
			shot(context, singleplayer, x + 6, y + 5, z + 12, x, y - 1, z, "jugcraft_mbs_preview_diesel");

			// Power lines: a lattice tower from afar; a wooden pole beside one already standing, with its wire.
			hold(server, "jugcraft:lattice_tower");
			shot(context, singleplayer, x + 10, y + 14, z + 50, x, y - 1, z, "jugcraft_mbs_preview_lattice");
			server.runOnServer(minecraft -> LinePlacement.place(minecraft.overworld(), new BlockPos(x - 12, y, z - 4), LineKind.WOODEN_POLE, null));
			hold(server, "jugcraft:wooden_pole");
			shot(context, singleplayer, x + 4, y + 4, z + 14, x, y - 1, z, "jugcraft_mbs_preview_pole_wire");

			// Furniture and a machine, close to.
			hold(server, "jugcraft:status_screen");
			shot(context, singleplayer, x, y + 1, z + 6, x, y - 1, z, "jugcraft_mbs_preview_screen");
			hold(server, "jugcraft:oval_table");
			shot(context, singleplayer, x + 2, y + 3, z + 6, x, y - 1, z, "jugcraft_mbs_preview_table");
			hold(server, "jugcraft:coal_generator");
			shot(context, singleplayer, x + 3, y + 2, z + 6, x, y - 1, z, "jugcraft_mbs_preview_machine");
		}
	}

	private static void hold(TestServerContext server, String item) {
		server.runCommand("item replace entity @p weapon.mainhand with " + item);
	}

	private static void setHudHidden(ClientGameTestContext context, boolean hidden) {
		context.runOnClient(client -> {
			if (client.gui.hud.isHidden() != hidden) {
				client.gui.hud.toggle();
			}
		});
	}

	/** From (fx, fy, fz), aiming at the block (lx, ly, lz): the preview stands where the item would place. */
	private static void shot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int fx, int fy, int fz, int lx, int ly, int lz, String name) {
		singleplayer.getServer().runCommand("tp @p %d.5 %d %d.5".formatted(fx, fy, fz));
		context.waitTicks(5);
		singleplayer.getConnection().waitForChunksRender();
		context.getInput().lookAt(new BlockPos(lx, ly, lz));
		context.waitTicks(25);
		context.takeScreenshot(name);
	}
}
