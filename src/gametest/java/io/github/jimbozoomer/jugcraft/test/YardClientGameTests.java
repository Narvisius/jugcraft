package io.github.jimbozoomer.jugcraft.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;

/**
 * The medium plant's transformer yard and switchyard in the game, with screenshots (transformer yard and switchyard
 * build, 20 October 2026): the plant built in a flat world, the transformer yard from above, MT-1A's front and MT-1C's
 * cabinet, the unit auxiliary transformers under the bus, the deluge valve house inside, the switchyard from above,
 * the breaker and disconnect, SAT-1 in its bay, and the yard at night. The shots are taken in the plant's frame
 * (tools/medium_plant.py site coordinates) turned into the world's as the plant test does.
 */
public class YardClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestFocus.skip("yards")) {
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
			// The transformer yard (along the turbine hall's east wall, x 224 to 282, z 31 to 66).
			shot(context, singleplayer, wx.applyAsInt(292), y + 26, wz.applyAsInt(18), wx.applyAsInt(250), y + 3, wz.applyAsInt(50), 0,
					"jugcraft_yard_overview");
			shot(context, singleplayer, wx.applyAsInt(232), y + 2, wz.applyAsInt(39), wx.applyAsInt(232), y + 3, wz.applyAsInt(47), 0,
					"jugcraft_yard_mt1a_front");
			shot(context, singleplayer, wx.applyAsInt(256), y + 2, wz.applyAsInt(43), wx.applyAsInt(256), y + 2, wz.applyAsInt(46), 0.3,
					"jugcraft_yard_mt1c_cabinet");
			shot(context, singleplayer, wx.applyAsInt(233), y + 3, wz.applyAsInt(66), wx.applyAsInt(240), y + 3, wz.applyAsInt(58), 0,
					"jugcraft_yard_uat");
			shot(context, singleplayer, wx.applyAsInt(251), y + 1, wz.applyAsInt(35), wx.applyAsInt(251), y + 1, wz.applyAsInt(32), 0.3,
					"jugcraft_yard_deluge_house");
			// The switchyard (x 287 to 320, z 34 to 84).
			shot(context, singleplayer, wx.applyAsInt(326), y + 20, wz.applyAsInt(28), wx.applyAsInt(300), y + 3, wz.applyAsInt(55), 0,
					"jugcraft_yard_switchyard");
			shot(context, singleplayer, wx.applyAsInt(293), y + 3, wz.applyAsInt(37), wx.applyAsInt(297), y + 4, wz.applyAsInt(47), 0,
					"jugcraft_yard_breaker_disconnect");
			shot(context, singleplayer, wx.applyAsInt(297), y + 3, wz.applyAsInt(64), wx.applyAsInt(297), y + 3, wz.applyAsInt(75), 0,
					"jugcraft_yard_sat");
			server.runCommand("time set midnight");
			server.runCommand("effect clear @p night_vision");
			context.waitTicks(40);
			shot(context, singleplayer, wx.applyAsInt(251), y + 3, wz.applyAsInt(46), wx.applyAsInt(248), y + 2, wz.applyAsInt(37), 0,
					"jugcraft_yard_night");
			server.runCommand("time set noon");
			if (!TestFocus.skip("reactorbuilding")) {
				reactorBuilding(context, singleplayer, wx, wz, y);
			}
		}
	}

	/**
	 * The reactor building inside (22 October 2026: the approved machines as compiled sites, tools/reactor_site.py),
	 * in its own light (no night vision): the operating floor from above, at the cavity, the head package, a steam
	 * generator's top, the loop floor by the primary shield, a coolant pump, down the upender pit, the polar crane, the
	 * west ladder; then opened for refuelling (the head on its stand, the crane and the refuelling machine over the
	 * cavity). Plant frame: the vessel's axis at (85.5, 65.5), the loop floor at y 0, the operating floor at 15.
	 */
	private static void reactorBuilding(ClientGameTestContext context, TestSingleplayerContext singleplayer, java.util.function.IntUnaryOperator wx,
			java.util.function.IntUnaryOperator wz, int y) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("effect clear @p night_vision");
		context.waitTicks(40);
		String[][] shots = {
				{"70", "28", "50", "85", "15", "65", "jugcraft_rb_overview"},
				{"72", "15", "60", "85", "12", "65", "jugcraft_rb_cavity"},
				{"78", "16", "73", "85", "12", "65", "jugcraft_rb_head"},
				{"84", "24", "47", "91", "24", "53", "jugcraft_rb_sg_top"},
				{"70", "1", "63", "85", "4", "65", "jugcraft_rb_loop_floor"},
				{"100", "1", "55", "95", "5", "60", "jugcraft_rb_pump"},
				{"98", "15", "63", "98", "-3", "65", "jugcraft_rb_upender_pit"},
				{"78", "15", "70", "85", "41", "65", "jugcraft_rb_crane"},
				{"69", "15", "75", "65", "13", "71", "jugcraft_rb_ladder"},
				{"62", "1", "65", "70", "2", "65", "jugcraft_rb_airlock"}};
		for (String[] s : shots) {
			shot(context, singleplayer, wx.applyAsInt(Integer.parseInt(s[0])), y + Integer.parseInt(s[1]), wz.applyAsInt(Integer.parseInt(s[2])),
					wx.applyAsInt(Integer.parseInt(s[3])), y + Integer.parseInt(s[4]), wz.applyAsInt(Integer.parseInt(s[5])), 0.3, s[6]);
		}
		// Opened for refuelling from the console: the building shows its refuelling picture.
		server.runOnServer(minecraft -> {
			var level = minecraft.overworld();
			for (var e : io.github.jimbozoomer.jugcraft.nuclear.EquipmentRegistry.of(level).anchors().entrySet()) {
				if (e.getValue().kind() == io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind.REACTOR_VESSEL
						&& level.getBlockEntity(e.getKey()) instanceof io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity v) {
					v.core.refuelOpen = true;
				}
			}
		});
		context.waitTicks(60);
		shot(context, singleplayer, wx.applyAsInt(70), y + 28, wz.applyAsInt(50), wx.applyAsInt(85), y + 15, wz.applyAsInt(65), 0, "jugcraft_rb_refuel_overview");
		shot(context, singleplayer, wx.applyAsInt(79), y + 17, wz.applyAsInt(81), wx.applyAsInt(72), y + 16, wz.applyAsInt(76), 0, "jugcraft_rb_refuel_head");
		shot(context, singleplayer, wx.applyAsInt(80), y + 17, wz.applyAsInt(60), wx.applyAsInt(85), y + 9, wz.applyAsInt(65), 0, "jugcraft_rb_refuel_core");
		server.runOnServer(minecraft -> {
			var level = minecraft.overworld();
			for (var e : io.github.jimbozoomer.jugcraft.nuclear.EquipmentRegistry.of(level).anchors().entrySet()) {
				if (level.getBlockEntity(e.getKey()) instanceof io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity v && v.kind().reactor()) {
					v.core.refuelOpen = false;
				}
			}
		});
		server.runCommand("effect give @p night_vision infinite 0 true");
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
