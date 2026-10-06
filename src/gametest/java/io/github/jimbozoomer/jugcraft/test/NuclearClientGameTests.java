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
			uniforms(context, singleplayer, x, y, z);
			warehouse(context, singleplayer, x, y, z);
			plants(context, singleplayer, x, y, z);
		}
	}

	/**
	 * The warehouse pieces, close up: a storage rack with some bays stocked (the loads drawn from what is in it), high-bay
	 * lights under a roof, and a roller door shut, half way up, and rolled up.
	 */
	private static void warehouse(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z) {
		TestServerContext server = singleplayer.getServer();
		int bx = x + 70, bz = z;
		// A rack facing north (front to the south), its left bay at bx - 1.
		for (int r = 0; r < 3; r++) {
			for (int c = 0; c < 3; c++) {
				server.runCommand("setblock %d %d %d jugcraft:storage_rack[facing=north,part=%d]".formatted(bx - 1 + c, y + r, bz, r * 3 + c));
			}
		}
		String[] stock = {"minecraft:iron_ingot", "minecraft:paper", "minecraft:copper_ingot", "minecraft:redstone", "minecraft:glass",
				"minecraft:bricks", "minecraft:oak_planks", "minecraft:coal", "minecraft:bone_meal"};
		int[] filled = {0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13, 18, 19, 20, 21, 22, 23, 30, 31, 32, 42, 48, 49};
		for (int i = 0; i < filled.length; i++) {
			int slot = filled[i];
			server.runCommand("item replace block %d %d %d container.%d with %s 32".formatted(bx, y, bz, slot, stock[(slot / 6 + slot) % stock.length]));
		}
		// A roof over the rack with high-bay lights under it.
		server.runCommand("fill %d %d %d %d %d %d jugcraft:aluminum_cladding".formatted(bx - 4, y + 7, bz - 3, bx + 4, y + 7, bz + 4));
		server.runCommand("setblock %d %d %d jugcraft:high_bay_light[facing=north]".formatted(bx - 2, y + 6, bz + 2));
		server.runCommand("setblock %d %d %d jugcraft:high_bay_light[facing=north]".formatted(bx + 2, y + 6, bz + 2));
		// A roller door five wide and four high in a wall, facing north (into the building: its coil on the north side).
		int dx = bx + 10;
		server.runCommand("fill %d %d %d %d %d %d jugcraft:aluminum_cladding".formatted(dx - 4, y, bz, dx + 4, y + 5, bz));
		server.runCommand("fill %d %d %d %d %d %d jugcraft:roller_door[facing=north,open=false]".formatted(dx - 2, y, bz, dx + 2, y + 3, bz));
		context.waitTicks(20);
		setHudHidden(context, true);
		server.runCommand("gamemode spectator @p");
		context.waitTicks(5);
		shot(context, singleplayer, bx, y + 1, bz + 4, bx, y + 1, bz, 0.4, "jugcraft_warehouse_rack_front");
		shot(context, singleplayer, bx + 3, y + 1, bz + 3, bx, y + 1, bz, 0.6, "jugcraft_warehouse_rack_angle");
		shot(context, singleplayer, bx, y + 2, bz + 4, bx, y + 6, bz + 2, 0.0, "jugcraft_warehouse_high_bay");
		shot(context, singleplayer, dx, y + 1, bz + 6, dx, y + 1, bz, 0.4, "jugcraft_roller_door_shut");
		singleplayer.getServer().runOnServer(minecraft -> io.github.jimbozoomer.jugcraft.nuclear.RollerDoorBlock.toggle(minecraft.overworld(),
				new BlockPos(dx, y, bz)));
		context.waitTicks(12);
		context.takeScreenshot("jugcraft_roller_door_rolling");
		context.waitTicks(30);
		context.takeScreenshot("jugcraft_roller_door_open");
		shot(context, singleplayer, dx, y + 1, bz - 4, dx, y + 3, bz, 0.4, "jugcraft_roller_door_coil_inside");
		server.runCommand("gamemode creative @p");
		setHudHidden(context, false);
	}

	/**
	 * The admin building's wearables and plants, close up: the lab coat and the security jacket worn (armor stands)
	 * and hanging in their lockers, the coat rack, and the office plants in a row.
	 */
	private static void uniforms(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z) {
		TestServerContext server = singleplayer.getServer();
		int bx = x + 40, bz = z;
		// A backdrop wall, lit, so the lockers read as in a room.
		server.runCommand("fill %d %d %d %d %d %d jugcraft:painted_wall".formatted(bx - 6, y, bz - 3, bx + 14, y + 3, bz - 3));
		server.runCommand("fill %d %d %d %d %d %d jugcraft:vinyl_floor".formatted(bx - 6, y - 1, bz - 2, bx + 14, y - 1, bz + 6));
		server.runCommand(("summon armor_stand %d.5 %d %d.5 {Rotation:[0f,0f],ShowArms:1b,NoBasePlate:1b,"
				+ "equipment:{chest:{id:\"jugcraft:lab_coat\",count:1}}}").formatted(bx, y, bz));
		server.runCommand(("summon armor_stand %d.5 %d %d.5 {Rotation:[0f,0f],ShowArms:1b,NoBasePlate:1b,"
				+ "equipment:{chest:{id:\"jugcraft:security_jacket\",count:1}}}").formatted(bx + 3, y, bz));
		// Two-high lockers (open fronts south): a lab locker and a security locker, each with its coat on the rail.
		server.runCommand("setblock %d %d %d jugcraft:locker_open[facing=north,up=true,down=false]".formatted(bx + 6, y, bz - 2));
		server.runCommand("setblock %d %d %d jugcraft:locker_open[facing=north,up=false,down=true]".formatted(bx + 6, y + 1, bz - 2));
		server.runCommand("setblock %d %d %d jugcraft:locker_security[facing=north,up=true,down=false]".formatted(bx + 8, y, bz - 2));
		server.runCommand("setblock %d %d %d jugcraft:locker_security[facing=north,up=false,down=true]".formatted(bx + 8, y + 1, bz - 2));
		server.runCommand("setblock %d %d %d jugcraft:coat_rack[facing=north]".formatted(bx + 10, y + 1, bz - 2));
		for (int i = 0; i < 8; i++) {
			server.runCommand("setblock %d %d %d jugcraft:office_plant[facing=north,variant=%d]".formatted(bx - 6 + i, y, bz + 5, i));
		}
		context.waitTicks(20);
		setHudHidden(context, true);
		server.runCommand("gamemode spectator @p");
		context.waitTicks(5);
		shot(context, singleplayer, bx, y + 1, bz + 2, bx, y + 1, bz, 0.5, "jugcraft_uniform_lab_coat_front");
		shot(context, singleplayer, bx + 2, y + 2, bz + 3, bx, y + 1, bz, 0.5, "jugcraft_uniform_lab_coat_three_quarter");
		shot(context, singleplayer, bx, y + 1, bz - 2, bx, y + 1, bz, 0.5, "jugcraft_uniform_lab_coat_back");
		shot(context, singleplayer, bx + 3, y + 1, bz + 2, bx + 3, y + 1, bz, 0.5, "jugcraft_uniform_security_front");
		shot(context, singleplayer, bx + 3, y + 1, bz - 2, bx + 3, y + 1, bz, 0.5, "jugcraft_uniform_security_back");
		shot(context, singleplayer, bx + 6, y + 1, bz, bx + 6, y + 1, bz - 2, -0.2, "jugcraft_uniform_locker_lab");
		shot(context, singleplayer, bx + 8, y + 1, bz, bx + 8, y + 1, bz - 2, -0.2, "jugcraft_uniform_locker_security");
		shot(context, singleplayer, bx + 10, y + 1, bz, bx + 10, y + 1, bz - 2, -0.2, "jugcraft_uniform_coat_rack");
		shot(context, singleplayer, bx - 3, y + 1, bz + 2, bx - 3, y, bz + 5, 0.4, "jugcraft_plants_row_a");
		shot(context, singleplayer, bx, y + 1, bz + 2, bx + 1, y, bz + 5, 0.4, "jugcraft_plants_row_b");
		shot(context, singleplayer, bx - 5, y + 1, bz + 4, bx - 6, y, bz + 5, 0.3, "jugcraft_plant_close_0");
		shot(context, singleplayer, bx - 3, y + 1, bz + 4, bx - 4, y, bz + 5, 0.3, "jugcraft_plant_close_2");
		shot(context, singleplayer, bx + 1, y + 1, bz + 4, bx, y, bz + 5, 0.3, "jugcraft_plant_close_6");
		server.runCommand("gamemode creative @p");
		setHudHidden(context, false);
		server.runCommand("kill @e[type=!player]");
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

	/**
	 * The three pre-made plants, each built with /nuclear build far apart, with water put round their intake legs (the
	 * flat test world has no lake), the reactors started, and a tour: from the lake, inside the reactor building, the
	 * turbine hall, and the cooling towers steaming.
	 */
	private static void plants(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("effect give @p night_vision infinite 0 true");
		// Each plant: its id, where interesting things are (site coordinates, see tools/nuclear_plants.py) and a name.
		Object[][] plants = {
				{"nuclear_small_modular", "small", new int[][] {{77, 62}, {78, 113}, {78, 150}}},
				{"nuclear_medium_pwr", "medium", new int[][] {{85, 65}, {160, 60}, {185, 215}}},
				{"nuclear_large_epr", "large", new int[][] {{178, 92}, {282, 41}, {110, 261}}},
		};
		int along = 0;
		for (Object[] plant : plants) {
			String id = (String) plant[0];
			String name = (String) plant[1];
			int[][] spots = (int[][]) plant[2];
			BlockPos stake = new BlockPos(x + 600 + along, y, z - 200);
			along += 700;
			io.github.jimbozoomer.jugcraft.blueprint.Blueprint[] blueprint = new io.github.jimbozoomer.jugcraft.blueprint.Blueprint[1];
			server.runOnServer(minecraft -> {
				var level = minecraft.overworld();
				blueprint[0] = io.github.jimbozoomer.jugcraft.blueprint.Blueprint.get(id, false);
				var result = io.github.jimbozoomer.jugcraft.nuclear.PlantBuilder.build(level, blueprint[0], stake,
						net.minecraft.world.level.block.Rotation.NONE, null, true);
				// A lake round the intake legs: water beside every conduit block below the ground.
				for (int i = 0; i < blueprint[0].size(); i++) {
					var state = blueprint[0].state(i, net.minecraft.world.level.block.Rotation.NONE);
					BlockPos at = stake.offset(blueprint[0].offset(i, net.minecraft.world.level.block.Rotation.NONE));
					if (at.getY() < stake.getY() && state.is(io.github.jimbozoomer.jugcraft.nuclear.JugcraftNuclear.PIPES.get(
							io.github.jimbozoomer.jugcraft.nuclear.PipeKind.WATER))) {
						for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
							if (!level.getBlockState(at.relative(d)).is(state.getBlock())) {
								level.setBlock(at.relative(d), net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 3);
							}
						}
					}
				}
				// Start every reactor at 60%.
				for (BlockPos vessel : result.vessels()) {
					if (level.getBlockEntity(vessel) instanceof io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity v) {
						v.core.rodTarget = 40;
						v.core.rods = 40;
						v.core.fission = 0.6;
						v.core.decay = io.github.jimbozoomer.jugcraft.nuclear.ReactorCore.DECAY_SHARE * 0.6;
						v.core.temperature = 300;
						v.changed();
					}
				}
			});
			int sizeX = blueprint[0].sizeX, ax = blueprint[0].anchorX(), sizeZ = blueprint[0].sizeZ;
			// Site (sx, sy, sz) to the world: the plant reaches north from the stake, its east-west turned round.
			java.util.function.IntUnaryOperator wx = sx -> stake.getX() + sizeX - 1 - sx - ax;
			java.util.function.IntUnaryOperator wz = sz -> stake.getZ() - sz;
			setHudHidden(context, true);
			server.runCommand("gamemode spectator @p");
			// From the lake, high up: the whole plant.
			int mid = sizeX / 2;
			shotAt(context, singleplayer, wx.applyAsInt(mid + 40), y + 110, wz.applyAsInt(-140), wx.applyAsInt(mid), y + 20, wz.applyAsInt(sizeZ / 2),
					"jugcraft_plant_" + name + "_overview");
			// Let the towers find themselves and the plume start; the reactors run meanwhile.
			// Between the reactor and the towers, so both tick.
			server.runCommand("tp @p %d %d %d".formatted(wx.applyAsInt((spots[0][0] + spots[2][0]) / 2), y + 60,
					wz.applyAsInt((spots[0][1] + spots[2][1]) / 2)));
			context.waitTicks(260);
			shotAt(context, singleplayer, wx.applyAsInt(spots[2][0] + 160), y + 90, wz.applyAsInt(spots[2][1] + 120), wx.applyAsInt(spots[2][0]),
					y + 120, wz.applyAsInt(spots[2][1]), "jugcraft_plant_" + name + "_towers");
			shotAt(context, singleplayer, wx.applyAsInt(spots[2][0] + 300), y + 40, wz.applyAsInt(spots[2][1] - 60), wx.applyAsInt(spots[2][0]),
					y + 110, wz.applyAsInt(spots[2][1]), "jugcraft_plant_" + name + "_plume_far");
			// Inside the reactor building, looking down on the reactor equipment.
			shotAt(context, singleplayer, wx.applyAsInt(spots[0][0] + 8), y + 30, wz.applyAsInt(spots[0][1] + 12), wx.applyAsInt(spots[0][0]),
					y + 2, wz.applyAsInt(spots[0][1]), "jugcraft_plant_" + name + "_reactor_inside");
			// The turbine hall.
			shotAt(context, singleplayer, wx.applyAsInt(spots[1][0] - 30), y + 18, wz.applyAsInt(spots[1][1] + 6), wx.applyAsInt(spots[1][0] + 20),
					y + 3, wz.applyAsInt(spots[1][1]), "jugcraft_plant_" + name + "_turbines");
			if (name.equals("medium")) {
				// Rebuild it in place (as the showcase does when the plant has changed since a world was built), then
				// look round the warehouse: an aisle of racks, the high-bay lights, the stores office.
				singleplayer.getServer().runOnServer(minecraft -> io.github.jimbozoomer.jugcraft.nuclear.PlantShowcase.rebuild(minecraft.overworld(),
						blueprint[0], stake, minecraft.getPlayerList().getPlayers().get(0)));
				context.waitTicks(40);
				server.runCommand("effect clear @p night_vision");
				shotAt(context, singleplayer, wx.applyAsInt(291), y + 2, wz.applyAsInt(116), wx.applyAsInt(270), y + 2, wz.applyAsInt(116),
						"jugcraft_plant_medium_warehouse_aisle");
				shotAt(context, singleplayer, wx.applyAsInt(282), y + 2, wz.applyAsInt(117), wx.applyAsInt(282), y + 11, wz.applyAsInt(113),
						"jugcraft_plant_medium_warehouse_lights");
				shotAt(context, singleplayer, wx.applyAsInt(298), y + 1, wz.applyAsInt(132), wx.applyAsInt(292), y + 1, wz.applyAsInt(129),
						"jugcraft_plant_medium_warehouse_stores");
				shotAt(context, singleplayer, wx.applyAsInt(282), y + 6, wz.applyAsInt(150), wx.applyAsInt(282), y + 4, wz.applyAsInt(136),
						"jugcraft_plant_medium_warehouse_outside");
				server.runCommand("effect give @p night_vision infinite 0 true");
			}
			server.runCommand("gamemode creative @p");
			setHudHidden(context, false);
		}
		server.runCommand("effect clear @p night_vision");
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
