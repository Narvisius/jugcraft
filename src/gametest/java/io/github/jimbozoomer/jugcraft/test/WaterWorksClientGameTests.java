package io.github.jimbozoomer.jugcraft.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;

/**
 * The medium plant's water works in the game, with screenshots (towers and pump houses build, 19 October 2026): the
 * plant built in a flat world, its cooling towers seen from afar, the outlet and the local panel LP-CT1A close up, the
 * stair tower and the shell door, the walkway inside over the eliminators; the lake intake from the lake, its pump hall
 * and LCP-1; the circulating water pump house, its hall and LCP-CW; and the towers' obstruction lights at night. The
 * shots are taken in the plant's frame (tools/medium_plant.py site coordinates) turned into the world's as the plant
 * test does (the plant reaches north from its stake, east-west turned round).
 */
public class WaterWorksClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestFocus.skip("waterworks")) {
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
			server.runCommand("gamerule doMobSpawning false");
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			BlockPos stake = origin.offset(300, 0, -100);
			int[] size = new int[2];
			server.runOnServer(minecraft -> {
				var level = minecraft.overworld();
				var blueprint = io.github.jimbozoomer.jugcraft.blueprint.Blueprint.get("nuclear_medium_pwr", false);
				io.github.jimbozoomer.jugcraft.nuclear.PlantBuilder.build(level, blueprint, stake, net.minecraft.world.level.block.Rotation.NONE,
						null, true);
				size[0] = blueprint.sizeX;
				size[1] = blueprint.anchorX();
			});
			int sx = size[0], ax = size[1];
			java.util.function.IntUnaryOperator wx = x -> stake.getX() + sx - 1 - x - ax;
			java.util.function.IntUnaryOperator wz = z -> stake.getZ() - z;
			int y = stake.getY();
			setHudHidden(context, true);
			server.runCommand("gamemode spectator @p");
			server.runCommand("effect give @p night_vision infinite 0 true");
			context.waitTicks(10);
			// Tower 1's axis is at (120, 270) of the plant, its outlet toward the plant (north there).
			shot(context, singleplayer, wx.applyAsInt(185), y + 60, wz.applyAsInt(150), wx.applyAsInt(120), y + 70, wz.applyAsInt(270), 0,
					"jugcraft_ww_towers_far");
			shot(context, singleplayer, wx.applyAsInt(128), y + 6, wz.applyAsInt(196), wx.applyAsInt(116), y + 8, wz.applyAsInt(214), 0,
					"jugcraft_ww_tower_outlet");
			shot(context, singleplayer, wx.applyAsInt(114), y + 1, wz.applyAsInt(209), wx.applyAsInt(114), y + 1, wz.applyAsInt(213), 0.6,
					"jugcraft_ww_tower_panel");
			shot(context, singleplayer, wx.applyAsInt(110), y + 14, wz.applyAsInt(200), wx.applyAsInt(116), y + 12, wz.applyAsInt(220), 0,
					"jugcraft_ww_tower_stair_door");
			shot(context, singleplayer, wx.applyAsInt(120), y + 2, wz.applyAsInt(255), wx.applyAsInt(120), y + 10, wz.applyAsInt(280), 0,
					"jugcraft_ww_tower_inside_low");
			// The lake intake (its shore on the plant's, its hall's middle at x 170): from out over the lake, the pump hall,
			// the electrical room's LCP-1.
			shot(context, singleplayer, wx.applyAsInt(170), y + 7, wz.applyAsInt(-32), wx.applyAsInt(170), y + 3, wz.applyAsInt(0), 0,
					"jugcraft_ww_intake_lake");
			shot(context, singleplayer, wx.applyAsInt(176), y + 2, wz.applyAsInt(5), wx.applyAsInt(160), y + 2, wz.applyAsInt(5), 0.2,
					"jugcraft_ww_intake_hall");
			shot(context, singleplayer, wx.applyAsInt(183), y + 2, wz.applyAsInt(4), wx.applyAsInt(186), y + 2, wz.applyAsInt(4), 0.1,
					"jugcraft_ww_intake_lcp");
			// The circulating water pump house between the towers: from the car park, inside the hall, LCP-CW.
			shot(context, singleplayer, wx.applyAsInt(185), y + 10, wz.applyAsInt(180), wx.applyAsInt(185), y + 6, wz.applyAsInt(212), 0,
					"jugcraft_ww_circ_outside");
			shot(context, singleplayer, wx.applyAsInt(196), y + 6, wz.applyAsInt(200), wx.applyAsInt(186), y + 3, wz.applyAsInt(209), 0,
					"jugcraft_ww_circ_hall");
			shot(context, singleplayer, wx.applyAsInt(168), y + 2, wz.applyAsInt(207), wx.applyAsInt(165), y + 2, wz.applyAsInt(207), 0.1,
					"jugcraft_ww_circ_lcp");
			server.runCommand("time set midnight");
			context.waitTicks(40);
			shot(context, singleplayer, wx.applyAsInt(150), y + 120, wz.applyAsInt(150), wx.applyAsInt(120), y + 150, wz.applyAsInt(270), 0,
					"jugcraft_ww_tower_beacons_night");
			server.runCommand("time set noon");
			server.runCommand("effect clear @p night_vision");
		}
	}

	private static void setHudHidden(ClientGameTestContext context, boolean hidden) {
		context.runOnClient(client -> {
			if (client.gui.hud.isHidden() != hidden) {
				client.gui.hud.toggle();
			}
		});
	}

	/** A spectator camera at (fx, fy + dy, fz) looking at a block. */
	private static void shot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int fx, int fy, int fz, int lx, int ly, int lz,
			double dy, String name) {
		singleplayer.getServer().runCommand("tp @p %d.5 %.2f %d.5".formatted(fx, fy + dy - 1.62 + 0.5, fz));
		context.waitTicks(5);
		singleplayer.getConnection().waitForChunksRender();
		context.getInput().lookAt(new BlockPos(lx, ly, lz));
		context.waitTicks(30);
		context.takeScreenshot(name);
	}
}
