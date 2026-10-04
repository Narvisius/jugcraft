package io.github.jimbozoomer.jugcraft.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;

/**
 * Client game test for nuclear stage 1, with screenshots: the 3D hazmat suit on an armor stand (front, back, side,
 * the respirator up close) and on a zombie, the suit on the player in third person, the shielding concrete, and the
 * Geiger counter in hand with the dose readout next to a check source.
 */
public class NuclearClientGameTests implements FabricClientGameTest {
	private static final String SUIT = "equipment:{head:{id:\"jugcraft:hazmat_hood\"},chest:{id:\"jugcraft:hazmat_coverall\"},"
			+ "legs:{id:\"jugcraft:hazmat_trousers\"},feet:{id:\"jugcraft:hazmat_boots\"}}";

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestFocus.skip("nuclear")) {
			return;
		}
		if (GuideScreenshotGameTests.active()) {
			return;
		}
		geigerFollowsTheRate();
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
			int x = origin.getX(), y = origin.getY(), z = origin.getZ();

			// An armor stand and a zombie in the suit, facing south; a small shielding concrete wall beside them.
			server.runCommand("summon armor_stand %d.5 %d %d.5 {Rotation:[0f,0f],ShowArms:1b,NoBasePlate:1b,%s}".formatted(x, y, z - 6, SUIT));
			server.runCommand("summon zombie %d.5 %d %d.5 {Rotation:[20f,0f],NoAI:1b,PersistenceRequired:1b,%s}".formatted(x + 3, y, z - 6, SUIT));
			server.runCommand("fill %d %d %d %d %d %d jugcraft:shielding_concrete".formatted(x - 6, y, z - 7, x - 3, y + 2, z - 7));
			context.waitTicks(20);
			setHudHidden(context, true);
			server.runCommand("gamemode spectator @p");
			context.waitTicks(5);

			shot(context, singleplayer, x, y + 1, z - 3, x, y + 1, z - 6, 0.6, "jugcraft_nuclear_suit_front");
			shot(context, singleplayer, x + 2, y + 2, z - 4, x, y + 1, z - 6, 0.6, "jugcraft_nuclear_suit_three_quarter");
			shot(context, singleplayer, x, y + 1, z - 9, x, y + 1, z - 6, 0.6, "jugcraft_nuclear_suit_back");
			shot(context, singleplayer, x - 3, y + 1, z - 6, x, y + 1, z - 6, 0.6, "jugcraft_nuclear_suit_side");
			shot(context, singleplayer, x, y + 1, z - 4, x, y + 2, z - 6, 0.4, "jugcraft_nuclear_suit_respirator");
			shot(context, singleplayer, x + 1, y + 1, z - 8, x, y + 1, z - 6, 1.1, "jugcraft_nuclear_suit_tank");
			shot(context, singleplayer, x + 3, y + 1, z - 2, x + 3, y + 1, z - 6, 0.6, "jugcraft_nuclear_suit_zombie");
			shot(context, singleplayer, x - 4, y + 2, z - 3, x - 5, y + 1, z - 7, 0.5, "jugcraft_nuclear_shielding_concrete");

			// The player in the suit, seen from behind (third person).
			server.runCommand("gamemode creative @p");
			server.runCommand("item replace entity @p armor.head with jugcraft:hazmat_hood");
			server.runCommand("item replace entity @p armor.chest with jugcraft:hazmat_coverall");
			server.runCommand("item replace entity @p armor.legs with jugcraft:hazmat_trousers");
			server.runCommand("item replace entity @p armor.feet with jugcraft:hazmat_boots");
			server.runCommand("tp @p %d.5 %d %d.5 200 10".formatted(x + 6, y, z - 2));
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(30);
			context.takeScreenshot("jugcraft_nuclear_player_third_person");
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));

			// The Geiger counter in hand with the dose readout, a check source on the ground in front (survival, so it counts).
			setHudHidden(context, false);
			server.runCommand("clear @p");
			server.runCommand("item replace entity @p hotbar.0 with jugcraft:geiger_counter");
			server.runCommand("item replace entity @p hotbar.1 with jugcraft:hazmat_hood");
			server.runCommand("item replace entity @p hotbar.2 with jugcraft:hazmat_coverall");
			server.runCommand("item replace entity @p hotbar.3 with jugcraft:hazmat_trousers");
			server.runCommand("item replace entity @p hotbar.4 with jugcraft:hazmat_boots");
			server.runCommand("item replace entity @p hotbar.5 with jugcraft:check_source");
			server.runCommand("item replace entity @p hotbar.6 with jugcraft:shielding_concrete");
			server.runCommand("tp @p %d.5 %d %d.5 180 25".formatted(x + 8, y, z + 4));
			server.runCommand("gamemode survival @p");
			server.runCommand("summon item %d.5 %d %d.5 {Item:{id:\"jugcraft:check_source\",count:1},PickupDelay:32767}".formatted(x + 8, y, z + 2));
			context.waitTicks(60);
			context.takeScreenshot("jugcraft_nuclear_geiger_hud");
			server.runCommand("gamemode creative @p");
			server.runCommand("clear @p");
			server.runCommand("kill @e[type=!player]");
			reactorUnit(context, singleplayer, x, y, z);
		}
	}

	/** The counter's live sound: counts follow the rate (never a fixed clip), and change as soon as the rate does. */
	private static void geigerFollowsTheRate() {
		io.github.jimbozoomer.jugcraft.client.GeigerSound.Counts counts = new io.github.jimbozoomer.jugcraft.client.GeigerSound.Counts();
		int second = 44_100;
		for (double cps : new double[] {2, 20, 200}) {
			long before = counts.counted;
			for (int i = 0; i < 60; i++) {
				counts.mix(second, cps);
			}
			double measured = (counts.counted - before) / 60.0;
			if (Math.abs(measured - cps) > cps * 0.25 + 0.5) {
				throw new AssertionError("at " + cps + " counts/s the counter made " + measured + " a second");
			}
		}
		long before = counts.counted;
		counts.mix(second, 0);
		if (counts.counted != before) {
			throw new AssertionError("no radiation, no counts");
		}
	}

	/** A whole running unit (/nuclear showcase), every piece close up, the console's screen and the fuel machines. */
	private static void reactorUnit(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z) {
		TestServerContext server = singleplayer.getServer();
		BlockPos base = new BlockPos(x, y, z - 8);
		BlockPos[] vessel = new BlockPos[1];
		server.runOnServer(minecraft -> {
			var level = minecraft.overworld();
			vessel[0] = io.github.jimbozoomer.jugcraft.nuclear.ReactorShowcase.build(level, base);
			// The fuel machines, beside the console.
			for (var entry : java.util.List.of(
					java.util.Map.entry(io.github.jimbozoomer.jugcraft.machine.MachineKind.GAS_CENTRIFUGE_CASCADE, base.offset(-12, 0, 2)),
					java.util.Map.entry(io.github.jimbozoomer.jugcraft.machine.MachineKind.FUEL_FABRICATOR, base.offset(-18, 0, 2)))) {
				var block = io.github.jimbozoomer.jugcraft.machine.JugcraftMachines.MACHINES.get(entry.getKey());
				var state = block.defaultBlockState();
				level.setBlock(entry.getValue(), state, 3);
				block.setPlacedBy(level, entry.getValue(), state, null, new net.minecraft.world.item.ItemStack(block));
			}
		});
		context.waitTicks(120);
		server.runOnServer(minecraft -> {
			var level = minecraft.overworld();
			var v = (io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity) level.getBlockEntity(vessel[0]);
			var unit = v.unit();
			if (unit == null || !unit.formed() || unit.loops() != 4) {
				throw new AssertionError("the showcase unit did not form: " + (unit == null ? "none" : unit.missing() + ", loops " + unit.loops()));
			}
			if (v.core.electric <= 0) {
				throw new AssertionError("the showcase unit makes no power (" + v.core.fission + " fission, " + v.core.temperature + " °C)");
			}
		});
		setHudHidden(context, true);
		server.runCommand("gamemode spectator @p");
		context.waitTicks(5);
		int bx = base.getX(), by = base.getY(), bz = base.getZ();
		shotAt(context, singleplayer, bx + 40, by + 34, bz + 18, bx + 6, by + 5, bz - 24, "jugcraft_nuclear_unit_overview");
		shotAt(context, singleplayer, bx + 3, by + 13, bz - 3, bx + 3, by + 10, bz - 13, "jugcraft_nuclear_vessel");
		shotAt(context, singleplayer, bx + 9, by + 24, bz - 7, bx + 3, by + 17, bz - 13, "jugcraft_nuclear_vessel_head");
		shotAt(context, singleplayer, bx - 5, by + 9, bz + 1, bx - 5, by + 8, bz - 17, "jugcraft_nuclear_steam_generator");
		shotAt(context, singleplayer, bx - 5, by + 4, bz - 6, bx - 5, by + 4, bz - 11, "jugcraft_nuclear_coolant_pump");
		shotAt(context, singleplayer, bx + 25, by + 11, bz - 7, bx + 25, by + 9, bz - 15, "jugcraft_nuclear_pressurizer");
		shotAt(context, singleplayer, bx + 34, by + 16, bz - 20, bx + 4, by + 4, bz - 35, "jugcraft_nuclear_turbine");
		shotAt(context, singleplayer, bx + 24, by + 9, bz - 25, bx + 20, by + 5, bz - 35, "jugcraft_nuclear_generator");
		shotAt(context, singleplayer, bx + 18, by + 7, bz - 50, bx + 8, by + 3, bz - 44, "jugcraft_nuclear_condenser");
		shotAt(context, singleplayer, bx + 32, by + 7, bz - 31, bx + 32, by + 4, bz - 40, "jugcraft_nuclear_transformer");
		shotAt(context, singleplayer, bx - 3, by + 3, bz - 24, bx - 3, by + 1, bz - 28, "jugcraft_nuclear_feedwater_pump");
		shotAt(context, singleplayer, bx - 5, by + 2, bz + 1, bx - 5, by + 1, bz - 3, "jugcraft_nuclear_console");
		shotAt(context, singleplayer, bx - 15, by + 5, bz + 9, bx - 15, by + 1, bz + 3, "jugcraft_nuclear_fuel_machines");
		// The console's screen.
		server.runCommand("gamemode creative @p");
		server.runCommand("tp @p %d.5 %d %d.5 180 20".formatted(bx - 5, by, bz + 1));
		context.waitTicks(20);
		setHudHidden(context, false);
		BlockPos console = base.offset(-6, 0, -2);
		context.runOnClient(client -> client.gui.setScreen(new io.github.jimbozoomer.jugcraft.client.ReactorConsoleScreen(console)));
		context.waitTicks(40);
		context.takeScreenshot("jugcraft_nuclear_console_screen");
		context.runOnClient(client -> client.gui.setScreen(null));
		// SCRAM it, then open the head for refuelling (the core lights the water blue).
		server.runOnServer(minecraft -> {
			var v = (io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity) minecraft.overworld().getBlockEntity(vessel[0]);
			v.core.scrammed = true;
		});
		context.waitTicks(60);
		context.runOnClient(client -> client.gui.setScreen(new io.github.jimbozoomer.jugcraft.client.ReactorConsoleScreen(console)));
		context.waitTicks(30);
		context.takeScreenshot("jugcraft_nuclear_console_scram");
		context.runOnClient(client -> client.gui.setScreen(null));
		server.runOnServer(minecraft -> {
			var v = (io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity) minecraft.overworld().getBlockEntity(vessel[0]);
			v.core.refuelOpen = true;
			v.changed();
		});
		setHudHidden(context, true);
		server.runCommand("gamemode spectator @p");
		shotAt(context, singleplayer, bx + 3, by + 22, bz - 6, bx + 3, by + 12, bz - 13, "jugcraft_nuclear_vessel_open");
		server.runCommand("gamemode creative @p");
		setHudHidden(context, false);
	}

	private static void shotAt(ClientGameTestContext context, TestSingleplayerContext singleplayer, int fx, int fy, int fz, int lx, int ly, int lz,
			String name) {
		singleplayer.getServer().runCommand("tp @p %d.5 %d %d.5".formatted(fx, fy, fz));
		context.waitTicks(5);
		singleplayer.getConnection().waitForChunksRender();
		context.getInput().lookAt(new BlockPos(lx, ly, lz));
		context.waitTicks(20);
		context.takeScreenshot(name);
	}

	private static void setHudHidden(ClientGameTestContext context, boolean hidden) {
		context.runOnClient(client -> {
			if (client.gui.hud.isHidden() != hidden) {
				client.gui.hud.toggle();
			}
		});
	}

	/** A spectator camera at (fx, fy + dy, fz) looking at a block; dy lifts the eye to the part being checked. */
	private static void shot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int fx, int fy, int fz, int lx, int ly, int lz,
			double dy, String name) {
		singleplayer.getServer().runCommand("tp @p %d.5 %.2f %d.5".formatted(fx, fy + dy - 1.62 + 0.5, fz));
		context.waitTicks(5);
		singleplayer.getConnection().waitForChunksRender();
		context.getInput().lookAt(new BlockPos(lx, ly, lz));
		context.waitTicks(20);
		context.takeScreenshot(name);
	}
}
