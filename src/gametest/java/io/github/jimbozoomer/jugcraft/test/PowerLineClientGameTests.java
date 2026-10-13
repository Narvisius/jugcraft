package io.github.jimbozoomer.jugcraft.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * Client game test for power lines, with screenshots: a distribution line (pedestal, the three wooden pole tiers and
 * a pad-mount transformer), the monopoles into a Unit Substation, the three lattice towers, the UHV and UHVDC towers
 * with a converter station, the superconducting pylons, and the placement preview.
 */
public class PowerLineClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestFocus.skip("powerline")) {
			return;
		}
		if (GuideScreenshotGameTests.active()) {
			return;
		}
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> {
					creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
					// A superflat world: the showcase needs 300 blocks of open, level ground.
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
				ServerPlayer player = minecraft.getPlayerList().getPlayers().getFirst();
				player.getAbilities().flying = true;
				player.onUpdateAbilities();
			});

			// Every structure, wired into working lines (powerline/PowerLineShowcase.java, what /powerlines showcase builds).
			server.runOnServer(minecraft -> {
				int built = io.github.jimbozoomer.jugcraft.powerline.PowerLineShowcase.build(minecraft.overworld(), origin);
				if (built < 26) {
					throw new AssertionError("the showcase built only " + built + " structures");
				}
			});
			context.waitTicks(40);
			setHudHidden(context, true);

			// A spectator camera: it stays exactly where it is put, however high.
			server.runCommand("gamemode spectator @p");
			context.waitTicks(5);
			// Overviews of each line.
			shot(context, singleplayer, x - 2, y + 7, z - 6, x + 2, y + 9, z - 24, "jugcraft_power_lines_distribution");
			shot(context, singleplayer, x - 5, y + 18, z - 30, x + 5, y + 18, z - 80, "jugcraft_power_lines_monopoles");
			shot(context, singleplayer, x - 20, y + 40, z - 95, x - 10, y + 32, z - 170, "jugcraft_power_lines_lattice");
			shot(context, singleplayer, x - 40, y + 45, z - 210, x - 20, y + 40, z - 300, "jugcraft_power_lines_uhv");
			shot(context, singleplayer, x + 70, y + 20, z - 255, x + 130, y + 6, z - 300, "jugcraft_power_lines_converter");
			shot(context, singleplayer, x - 100, y + 12, z - 8, x - 115, y + 18, z - 40, "jugcraft_power_lines_superconducting");
			// Close-ups: every face the right way up, nothing floating, wires landing where they should.
			shot(context, singleplayer, x - 22, y + 1, z - 21, x - 22, y + 1, z - 24, "jugcraft_power_lines_pedestal");
			shot(context, singleplayer, x + 25, y + 1, z - 16, x + 25, y, z - 19, "jugcraft_power_lines_pad_mount");
			shot(context, singleplayer, x - 14, y + 10, z - 20, x - 16, y + 12, z - 24, "jugcraft_power_lines_pole_top");
			shot(context, singleplayer, x - 7, y + 8, z - 10, x - 7, y + 10, z - 24, "jugcraft_power_lines_pole_span");
			shot(context, singleplayer, x + 2, y + 20, z - 60, x + 2, y + 24, z - 80, "jugcraft_power_lines_monopole_span");
			shot(context, singleplayer, x + 58, y + 6, z - 73, x + 53, y + 2, z - 80, "jugcraft_power_lines_substation");
			shot(context, singleplayer, x - 81, y + 2, z - 165, x - 85, y + 2, z - 165, "jugcraft_power_lines_leg_plate");
			shot(context, singleplayer, x + 44, y + 44, z - 162, x + 36, y + 41, z - 170, "jugcraft_power_lines_lattice_arm");
			shot(context, singleplayer, x - 60, y + 62, z - 292, x - 60, y + 60, z - 300, "jugcraft_power_lines_uhv_top");
			shot(context, singleplayer, x + 70, y + 69, z - 292, x + 70, y + 67, z - 300, "jugcraft_power_lines_uhvdc_top");
			shot(context, singleplayer, x - 110, y + 22, z - 31, x - 112, y + 21, z - 35, "jugcraft_power_lines_pylon_joint");
			shot(context, singleplayer, x - 116, y + 3, z - 2, x - 119, y + 2, z + 1, "jugcraft_power_lines_cryo_termination");

			// The placement preview: holding a wooden pole near the distribution line.
			server.runCommand("gamemode creative @p");
			setHudHidden(context, false);
			server.runCommand("item replace entity @p weapon.mainhand with jugcraft:wooden_pole");
			server.runCommand("tp @p %d.5 %d %d.5 90 35".formatted(x - 12, y, z - 34));
			context.waitTicks(10);
			context.getInput().lookAt(new BlockPos(x - 20, y - 1, z - 36));
			context.waitTicks(10);
			context.takeScreenshot("jugcraft_power_lines_preview");
		}
	}

	private static void setHudHidden(ClientGameTestContext context, boolean hidden) {
		context.runOnClient(client -> {
			if (client.gui.hud.isHidden() != hidden) {
				client.gui.hud.toggle();
			}
		});
	}

	private static void shot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int fx, int fy, int fz, int lx, int ly, int lz, String name) {
		singleplayer.getServer().runCommand("tp @p %d.5 %d %d.5".formatted(fx, fy, fz));
		context.waitTicks(5);
		singleplayer.getConnection().waitForChunksRender();
		context.getInput().lookAt(new BlockPos(lx, ly, lz));
		context.waitTicks(30);
		context.takeScreenshot(name);
	}
}
