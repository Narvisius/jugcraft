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

			// The Geiger counter in hand with the dose readout, a check source on the ground in front and a block of uranium
			// beside it; in creative, the way Justin plays (it must read there too).
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
			server.runCommand("summon item %d.5 %d %d.5 {Item:{id:\"jugcraft:check_source\",count:1},PickupDelay:32767}".formatted(x + 8, y, z + 2));
			server.runCommand("setblock %d %d %d jugcraft:uranium_block".formatted(x + 10, y, z + 2));
			context.waitTicks(60);
			double read = context.computeOnClient(client -> io.github.jimbozoomer.jugcraft.nuclear.Radiation.state(client.player).rate());
			if (read < 1) {
				throw new AssertionError("In creative, holding the counter by a check source and a uranium block, it reads only " + read + " mSv/h");
			}
			context.takeScreenshot("jugcraft_nuclear_geiger_hud");
			// Round 14: the counter's needle and the readout's gauge follow the rate (they stood still before): in the
			// CAUTION band here, LETHAL by spent fuel, SAFE away from it all.
			double needle = context.computeOnClient(client -> io.github.jimbozoomer.jugcraft.client.RadiationMeter.needle());
			double want = io.github.jimbozoomer.jugcraft.client.RadiationMeter.position(read);
			if (Math.abs(needle - want) > 0.08) {
				throw new AssertionError("The needle stands at " + needle + " for " + read + " mSv/h; it should be near " + want);
			}
			server.runCommand("summon item %d.5 %d %d.5 {Item:{id:\"jugcraft:spent_fuel_assembly\",count:1},PickupDelay:32767}".formatted(x + 8, y, z + 1));
			context.waitTicks(50);
			double lethal = context.computeOnClient(client -> io.github.jimbozoomer.jugcraft.client.RadiationMeter.needle());
			context.takeScreenshot("jugcraft_nuclear_geiger_lethal");
			if (lethal < io.github.jimbozoomer.jugcraft.client.RadiationMeter.position(io.github.jimbozoomer.jugcraft.client.RadiationMeter.LETHAL)) {
				throw new AssertionError("By a spent fuel assembly the needle should be in the LETHAL band: " + lethal);
			}
			server.runCommand("kill @e[type=item]");
			server.runCommand("item replace entity @p hotbar.5 with air");
			server.runCommand("setblock %d %d %d air".formatted(x + 10, y, z + 2));
			context.waitTicks(80);
			double safe = context.computeOnClient(client -> io.github.jimbozoomer.jugcraft.client.RadiationMeter.needle());
			context.takeScreenshot("jugcraft_nuclear_geiger_safe");
			if (safe > io.github.jimbozoomer.jugcraft.client.RadiationMeter.position(io.github.jimbozoomer.jugcraft.client.RadiationMeter.SAFE)) {
				throw new AssertionError("With nothing about the needle should fall back into the SAFE band: " + safe);
			}
			server.runCommand("setblock %d %d %d air".formatted(x + 10, y, z + 2));
			server.runCommand("gamemode creative @p");
			server.runCommand("clear @p");
			server.runCommand("kill @e[type=!player]");
			reactorUnit(context, singleplayer, x, y, z);
			uniforms(context, singleplayer, x, y, z);
			warehouse(context, singleplayer, x, y, z);
			tankKits(context, singleplayer, x, y, z);
			plants(context, singleplayer, x, y, z);
		}
	}

	/**
	 * The tank ratings (Justin, Oct 7): every kit placed in a row, each part filled with a liquid of its class (water,
	 * diesel, sulfuric acid, liquid oxygen; one flammable tank titled with a name tag), seen from the front-left, and its
	 * painted label close up; the tank screen of a titled, rated tank.
	 */
	private static void tankKits(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z) {
		TestServerContext server = singleplayer.getServer();
		String[] kits = {"general_small", "general_medium", "general_large", "flammable_small", "flammable_medium", "flammable_large",
				"corrosive_small", "corrosive_medium", "corrosive_large", "cryogenic"};
		String[] fluids = {"minecraft:water", "minecraft:water", "minecraft:water", "jugcraft:diesel", "jugcraft:gasoline", "jugcraft:crude_oil",
				"jugcraft:lye", "jugcraft:sulfuric_acid", "jugcraft:nitric_acid", "jugcraft:liquid_oxygen"};
		int x0 = x - 320, z0 = z + 40;
		java.util.List<BlockPos> outlets = new java.util.ArrayList<>();
		server.runCommand("effect give @p night_vision infinite 0 true");
		server.runCommand("gamemode spectator @p");
		setHudHidden(context, true);
		for (int i = 0; i < kits.length; i++) {
			String kit = kits[i];
			String fluid = fluids[i];
			BlockPos stake = new BlockPos(x0 + i * 26, y, z0);
			server.runCommand("tp @p %d.5 %d %d.5".formatted(stake.getX(), y + 2, stake.getZ() + 8));
			context.waitTicks(10);
			singleplayer.getConnection().waitForChunksRender();
			BlockPos[] outlet = new BlockPos[1];
			singleplayer.getServer().runOnServer(minecraft -> {
				net.minecraft.server.level.ServerLevel level = minecraft.overworld();
				net.minecraft.network.chat.Component refused = io.github.jimbozoomer.jugcraft.nuclear.TankKitItem.place(level, kit, stake,
						net.minecraft.world.level.block.Rotation.NONE, null);
				if (refused != null) {
					throw new AssertionError("The " + kit + " kit was refused: " + refused.getString());
				}
				io.github.jimbozoomer.jugcraft.blueprint.Blueprint bp = io.github.jimbozoomer.jugcraft.nuclear.TankKitItem.blueprint(kit);
				for (int k = 0; k < bp.size(); k++) {
					if (bp.state(k, net.minecraft.world.level.block.Rotation.NONE).getBlock() instanceof io.github.jimbozoomer.jugcraft.nuclear.StorageTankOutletBlock) {
						outlet[0] = stake.offset(bp.offset(k, net.minecraft.world.level.block.Rotation.NONE));
					}
				}
				if (!(level.getBlockEntity(outlet[0]) instanceof io.github.jimbozoomer.jugcraft.nuclear.StorageTankPortBlockEntity tank) || !tank.formed()) {
					throw new AssertionError("The " + kit + " kit's tank did not form");
				}
				net.minecraft.world.level.material.Fluid f = net.minecraft.core.registries.BuiltInRegistries.FLUID.getValue(
						net.minecraft.resources.Identifier.parse(fluid));
				try (var t = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
					long in = tank.fluid.insert(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(f), tank.fluid.capacity() * 3 / 5, t);
					t.commit();
					if (in <= 0) {
						throw new AssertionError("The " + kit + " tank refused " + fluid);
					}
				}
				if (kit.equals("flammable_large")) {
					tank.setTitle("Main Fuel Store");
				}
			});
			outlets.add(outlet[0]);
		}
		context.waitTicks(40);
		for (int i = 0; i < kits.length; i++) {
			BlockPos stake = new BlockPos(x0 + i * 26, y, z0);
			int depth = kits[i].endsWith("large") ? 15 : kits[i].endsWith("medium") ? 11 : 8;
			int tall = kits[i].endsWith("large") ? 10 : kits[i].endsWith("medium") ? 8 : kits[i].equals("cryogenic") ? 13 : 6;
			BlockPos middle = new BlockPos(stake.getX(), y + tall / 2, stake.getZ() - depth / 2 - 1);
			shotAt(context, singleplayer, stake.getX() - depth + 1, y + tall / 2 + 3, stake.getZ() + depth, middle.getX(), middle.getY(), middle.getZ(),
					"jugcraft_tank_kit_" + kits[i]);
			// The label, square on.
			shotAt(context, singleplayer, stake.getX(), y + tall / 2, stake.getZ() + Math.max(4, depth / 2 + 2), middle.getX(), middle.getY(), middle.getZ(),
					"jugcraft_tank_kit_" + kits[i] + "_label");
		}
		// The screen of the titled fuel store (from beside it, so its chunk is loaded on the client).
		BlockPos store = outlets.get(5);
		server.runCommand("tp @p %d.5 %d %d.5".formatted(store.getX() - 3, y + 1, store.getZ()));
		context.waitTicks(10);
		singleplayer.getConnection().waitForChunksRender();
		context.runOnClient(client -> io.github.jimbozoomer.jugcraft.nuclear.StorageTanks.openScreen.accept(store));
		context.waitTicks(10);
		context.takeScreenshot("jugcraft_tank_kit_titled_screen");
		context.runOnClient(client -> client.gui.setScreen(null));
		context.waitTicks(5);
		server.runCommand("effect clear @p night_vision");
		server.runCommand("gamemode creative @p");
		setHudHidden(context, false);
	}

	/**
	 * Round 14: the radwaste building west of the auxiliary building (site x 5..27, z 98..132; tools/radwaste_store.py):
	 * outside, the entry hall, the airlock (its beacons green, then the outer door open: the inner door's beacons red
	 * and the inner door won't open), the vault, the inspection corridor.
	 */
	private static void radwaste(ClientGameTestContext context, TestSingleplayerContext singleplayer, java.util.function.IntUnaryOperator wx,
			java.util.function.IntUnaryOperator wz, int y) {
		int[][] shots = {{16, 5, 146, 16, 3, 132}, {10, 2, 131, 16, 2, 122}, {16, 2, 126, 16, 2, 122}, {16, 2, 120, 16, 2, 117},
				{7, 5, 118, 15, 1, 106}, {6, 2, 122, 6, 2, 104}, {16, 2, 114, 16, 1, 104}};
		String[] names = {"outside", "hall", "airlock_outer", "airlock_chamber", "vault", "corridor", "vault_from_door"};
		for (int i = 0; i < shots.length; i++) {
			int[] t = shots[i];
			shotAt(context, singleplayer, wx.applyAsInt(t[0]), y + t[1], wz.applyAsInt(t[2]), wx.applyAsInt(t[3]), y + t[4], wz.applyAsInt(t[5]),
					"jugcraft_plant_medium_r14_radwaste_" + names[i]);
		}
		BlockPos outer = new BlockPos(wx.applyAsInt(16), y, wz.applyAsInt(122)), inner = new BlockPos(wx.applyAsInt(16), y, wz.applyAsInt(117));
		singleplayer.getServer().runOnServer(minecraft -> {
			var level = minecraft.overworld();
			var o = level.getBlockState(outer);
			var in = level.getBlockState(inner);
			if (!(o.getBlock() instanceof io.github.jimbozoomer.jugcraft.nuclear.LeadDoorBlock door) || !(in.getBlock() instanceof io.github.jimbozoomer.jugcraft.nuclear.LeadDoorBlock)) {
				throw new AssertionError("The radwaste airlock's doors are not at " + outer.toShortString() + " and " + inner.toShortString()
						+ ": " + o + ", " + in);
			}
			door.setOpen(null, level, o, outer, true);
			if (io.github.jimbozoomer.jugcraft.nuclear.LeadDoorBlock.blockedBy(level, inner, in) == null) {
				throw new AssertionError("With the outer door open the inner door should be interlocked");
			}
		});
		context.waitTicks(5);
		shotAt(context, singleplayer, wx.applyAsInt(16), y + 2, wz.applyAsInt(127), wx.applyAsInt(16), y + 2, wz.applyAsInt(117),
				"jugcraft_plant_medium_r14_radwaste_airlock_open");
		singleplayer.getServer().runOnServer(minecraft -> {
			var level = minecraft.overworld();
			var o = level.getBlockState(outer);
			((io.github.jimbozoomer.jugcraft.nuclear.LeadDoorBlock) o.getBlock()).setOpen(null, level, o, outer, false);
		});
	}

	/**
	 * Round 14: diesel room A's panels follow its generator. Stopped and settled, then started again: part way into the
	 * load sequence (the safety injection pump starting, the voltage and frequency dipping) and with it complete (full
	 * load, everything steady), then stopping. The panels' block entities must be there, and the readings right.
	 */
	private static void livePanels(ClientGameTestContext context, TestSingleplayerContext singleplayer, java.util.function.IntUnaryOperator wx,
			java.util.function.IntUnaryOperator wz, int y, int baseY) {
		BlockPos edg = new BlockPos(wx.applyAsInt(184), baseY, wz.applyAsInt(97));
		java.util.function.Consumer<Boolean> run = on -> singleplayer.getServer().runOnServer(minecraft -> {
			var level = minecraft.overworld();
			if (level.getBlockEntity(edg) instanceof io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity generator) {
				generator.setDieselRunning(level.getGameTime(), on);
			}
		});
		singleplayer.getServer().runOnServer(minecraft -> {
			int panels = 0;
			for (BlockPos at : BlockPos.betweenClosed(new BlockPos(Math.min(wx.applyAsInt(176), wx.applyAsInt(196)), baseY, Math.min(wz.applyAsInt(102), wz.applyAsInt(114))),
					new BlockPos(Math.max(wx.applyAsInt(176), wx.applyAsInt(196)), baseY + 1, Math.max(wz.applyAsInt(102), wz.applyAsInt(114))))) {
				if (minecraft.overworld().getBlockEntity(at) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity) {
					panels++;
				}
			}
			if (panels < 6) {
				throw new AssertionError("Diesel room A has " + panels + " live panels (block entities); it should have 6");
			}
		});
		run.accept(false);
		context.waitTicks(120);
		// Each panel has linked itself to this room's generator (the server links them; the players are told).
		String links = context.computeOnClient(client -> {
			StringBuilder wrong = new StringBuilder();
			int linked = 0;
			for (BlockPos at : BlockPos.betweenClosed(new BlockPos(Math.min(wx.applyAsInt(176), wx.applyAsInt(196)), baseY, Math.min(wz.applyAsInt(102), wz.applyAsInt(114))),
					new BlockPos(Math.max(wx.applyAsInt(176), wx.applyAsInt(196)), baseY + 1, Math.max(wz.applyAsInt(102), wz.applyAsInt(114))))) {
				if (client.level.getBlockEntity(at) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity panel) {
					if (edg.equals(panel.generator)) {
						linked++;
					} else {
						wrong.append(" ").append(at.toShortString()).append("->").append(panel.generator == null ? "none" : panel.generator.toShortString());
					}
				}
			}
			return linked == 6 ? "" : linked + " of 6 panels linked to the generator at " + edg.toShortString() + ";" + wrong;
		});
		if (!links.isEmpty()) {
			throw new AssertionError(links);
		}
		shotAt(context, singleplayer, wx.applyAsInt(187), y + 2, wz.applyAsInt(104), wx.applyAsInt(186), y + 1, wz.applyAsInt(108),
				"jugcraft_plant_medium_r14_panels_stopped");
		run.accept(true);
		// The screenshot comes about 10.3 s after the start: the safety injection pump starting.
		context.waitTicks(181);
		shotAt(context, singleplayer, wx.applyAsInt(187), y + 2, wz.applyAsInt(104), wx.applyAsInt(186), y + 1, wz.applyAsInt(108),
				"jugcraft_plant_medium_r14_panels_starting");
		context.waitTicks(700);
		java.util.function.Supplier<io.github.jimbozoomer.jugcraft.client.EdgTelemetry.Snapshot> read = () -> context.computeOnClient(client -> {
			var level = client.level;
			return io.github.jimbozoomer.jugcraft.client.EdgTelemetry.of(level.getBlockEntity(edg) instanceof io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity e
					? e : null, level.getGameTime());
		});
		var full = read.get();
		if (!full.breaker || full.steps != 8 || full.kw < 6600 || full.kw > 7000 || Math.abs(full.hz - 60) > 0.2 || Math.abs(full.kv - 4.16) > 0.05
				|| Math.abs(full.rpm - 514) > 3) {
			throw new AssertionError(String.format(java.util.Locale.ROOT, "At full load the panels read %.0f kW %.2f Hz %.3f kV %.0f rpm, breaker %s, %d steps",
					full.kw, full.hz, full.kv, full.rpm, full.breaker, full.steps));
		}
		int[][] shots = {{187, 2, 104, 186, 1, 108}, {188, 0, 105, 189, 1, 108}, {185, 0, 105, 184, 1, 108}, {189, 1, 106, 190, 1, 108},
				{183, 1, 106, 183, 1, 108}};
		String[] names = {"panels_running_wide", "panels_running_left", "panels_running_right", "panels_running_close_a",
				"panels_running_close_b"};
		for (int i = 0; i < shots.length; i++) {
			int[] t = shots[i];
			shotAt(context, singleplayer, wx.applyAsInt(t[0]), y + t[1], wz.applyAsInt(t[2]), wx.applyAsInt(t[3]), y + t[4], wz.applyAsInt(t[5]),
					"jugcraft_plant_medium_r14_" + names[i]);
		}
		run.accept(false);
		context.waitTicks(30);
		shotAt(context, singleplayer, wx.applyAsInt(187), y + 2, wz.applyAsInt(104), wx.applyAsInt(186), y + 1, wz.applyAsInt(108),
				"jugcraft_plant_medium_r14_panels_stopping");
		var stopped = read.get();
		if (stopped.breaker || stopped.kw > 1 || stopped.running) {
			throw new AssertionError("Stopped, the breaker should be open and the load gone: " + stopped.kw + " kW, breaker " + stopped.breaker);
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
		// Each bay's six slots (bays left to right along the bottom, then up): only liquids go in drums or jerrycans.
		String[][] bays = {
				{"minecraft:water_bucket", "minecraft:water_bucket", "minecraft:lava_bucket", "minecraft:water_bucket", "minecraft:lava_bucket", "minecraft:water_bucket"},
				{"minecraft:bucket", "minecraft:bucket", "minecraft:iron_ingot", "minecraft:diamond_pickaxe", "minecraft:glass", "minecraft:paper"},
				{"jugcraft:crude_oil_bucket", "jugcraft:diesel_bucket", "jugcraft:sulfuric_acid_bucket", "minecraft:milk_bucket",
						"jugcraft:vanadium_electrolyte_bucket", "jugcraft:brine_bucket"},
				{"minecraft:potion", "minecraft:honey_bottle", "minecraft:glass_bottle", "minecraft:potion", null, null},
				{"minecraft:bread", "minecraft:apple", "minecraft:cooked_beef", "minecraft:carrot", "minecraft:bread", null},
				{"minecraft:redstone", "minecraft:bricks", "minecraft:oak_planks", "minecraft:coal", "minecraft:bone_meal", "minecraft:string"},
				{"minecraft:copper_ingot", "minecraft:paper", null, null, null, null},
				{},
				{"minecraft:water_bucket", null, "minecraft:bucket", null, null, null}};
		for (int bay = 0; bay < bays.length; bay++) {
			for (int k = 0; k < bays[bay].length; k++) {
				if (bays[bay][k] != null) {
					server.runCommand("item replace block %d %d %d container.%d with %s 1".formatted(bx, y, bz, bay * 6 + k, bays[bay][k]));
				}
			}
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
		shot(context, singleplayer, bx, y, bz + 3, bx, y, bz, 0.2, "jugcraft_warehouse_rack_low");
		shot(context, singleplayer, bx, y + 1, bz + 3, bx, y + 1, bz, 0.0, "jugcraft_warehouse_rack_middle");
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
				// The warehouse racks are stocked first: the rebuild must put back what was in them.
				singleplayer.getServer().runOnServer(minecraft -> {
					net.minecraft.server.level.ServerLevel level = minecraft.overworld();
					net.minecraft.world.item.Item[] goods = {net.minecraft.world.item.Items.WATER_BUCKET, net.minecraft.world.item.Items.PAPER,
							net.minecraft.world.item.Items.IRON_INGOT, net.minecraft.world.item.Items.BREAD, net.minecraft.world.item.Items.LAVA_BUCKET,
							net.minecraft.world.item.Items.GLASS, net.minecraft.world.item.Items.BUCKET, net.minecraft.world.item.Items.POTION,
							net.minecraft.world.item.Items.OAK_PLANKS, net.minecraft.world.item.Items.MILK_BUCKET, net.minecraft.world.item.Items.STRING};
					java.util.List<BlockPos> racks = new java.util.ArrayList<>();
					for (int i = 0; i < blueprint[0].size(); i++) {
						BlockPos at = stake.offset(blueprint[0].offset(i, net.minecraft.world.level.block.Rotation.NONE));
						if (level.getBlockEntity(at) instanceof io.github.jimbozoomer.jugcraft.nuclear.StorageRackBlockEntity rack) {
							int k = racks.size();
							for (int slot = 0; slot < 10 + (k * 7) % 40; slot++) {
								rack.setItem(slot, new net.minecraft.world.item.ItemStack(goods[(k + slot / 3) % goods.length]));
							}
							rack.setChanged();
							racks.add(at);
						}
					}
					io.github.jimbozoomer.jugcraft.nuclear.PlantShowcase.rebuild(level, blueprint[0], stake, minecraft.getPlayerList().getPlayers().get(0));
					for (int k = 0; k < racks.size(); k++) {
						if (!(level.getBlockEntity(racks.get(k)) instanceof io.github.jimbozoomer.jugcraft.nuclear.StorageRackBlockEntity rack)
								|| !rack.getItem(0).is(goods[k % goods.length])) {
							throw new AssertionError("The rebuild lost what was in the storage rack at " + racks.get(k).toShortString());
						}
					}
					if (racks.isEmpty()) {
						throw new AssertionError("No storage racks found in the medium plant");
					}
				});
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
				// The storage tanks (formed and filled by the build): the condensate tank's stencil, outlet and float
				// gauge, inlet and fill line, ladder top and gate, roof; the diesel tank with its fire diamond and pipe.
				int[][] tankShots = {
						{232, 9, 150, 232, 7, 125}, {219, 3, 122, 226, 2, 125}, {214, 9, 125, 227, 8, 125}, {228, 4, 112, 232, 3, 119},
						{241, 17, 121, 236, 14, 125}, {242, 8, 125, 237, 7, 125}, {232, 25, 142, 232, 13, 125}, {200, 7, 146, 200, 5, 125},
						{206, 4, 112, 200, 1, 117}, {233, 15, 123, 236, 14, 125}, {239, 16, 128, 236, 14, 125}};
				String[] tankNames = {"cst_stencil", "cst_outlet", "cst_gauge", "cst_inlet", "cst_ladder_top", "cst_ladder_cage", "cst_roof",
						"diesel_tank", "diesel_pipe", "cst_gate_inside", "cst_gate_outside"};
				for (int i = 0; i < tankShots.length; i++) {
					int[] t = tankShots[i];
					shotAt(context, singleplayer, wx.applyAsInt(t[0]), y + t[1], wz.applyAsInt(t[2]), wx.applyAsInt(t[3]), y + t[4], wz.applyAsInt(t[5]),
							"jugcraft_plant_medium_tank_" + tankNames[i]);
				}
				// Round 12: the roof hatches opened (the condensate tank's and diesel room A's day tank's: the inside of the
				// tank and the liquid in it), the day tank and its fuel lines, the generator, the pipe through the wall.
				int baseY = y;
				singleplayer.getServer().runOnServer(minecraft -> {
					net.minecraft.server.level.ServerLevel level = minecraft.overworld();
					for (int[] h : new int[][] {{234, 13, 125}, {198, 6, 93}}) {
						BlockPos at = new BlockPos(wx.applyAsInt(h[0]), baseY + h[1], wz.applyAsInt(h[2]));
						net.minecraft.world.level.block.state.BlockState st = level.getBlockState(at);
						if (!(st.getBlock() instanceof io.github.jimbozoomer.jugcraft.nuclear.StorageTankHatchBlock)) {
							throw new AssertionError("No tank hatch at " + at.toShortString() + ": " + st);
						}
						level.setBlock(at, st.setValue(io.github.jimbozoomer.jugcraft.nuclear.StorageTankHatchBlock.OPEN, true), 3);
					}
					// The fuel line goes in under the wall (in the slab), from its trench outside, on along the floor.
					for (int[] c : new int[][] {{198, -1, 109}, {198, -1, 110}, {200, -1, 114}, {198, -1, 100}}) {
						BlockPos wall = new BlockPos(wx.applyAsInt(c[0]), baseY + c[1], wz.applyAsInt(c[2]));
						if (!(level.getBlockEntity(wall) instanceof io.github.jimbozoomer.jugcraft.wallrun.WallRunBlockEntity run) || !run.hasDisguise()) {
							throw new AssertionError("The buried fuel line at " + wall.toShortString() + " has no look: " + level.getBlockState(wall));
						}
					}
					BlockPos day = new BlockPos(wx.applyAsInt(195), baseY, wz.applyAsInt(93));
					if (!(level.getBlockEntity(day) instanceof io.github.jimbozoomer.jugcraft.nuclear.StorageTankPortBlockEntity tank) || !tank.formed()
							|| tank.fluid.amount <= 0) {
						throw new AssertionError("Diesel room A's day tank is not formed and filled: " + level.getBlockState(day));
					}
				});
				context.waitTicks(30);
				int[][] roundShots = {
						{235, 17, 125, 234, 6, 125}, {199, 9, 93, 198, 1, 93}, {191, 3, 105, 198, 3, 93}, {181, 4, 106, 190, 1, 100},
						{196, 2, 106, 191, 2, 101}, {183, 6, 100, 195, 2, 100}, {190, 5, 105, 185, 7, 100}, {186, 2, 94, 189, 0, 97},
						{202, 1, 114, 199, -1, 111}, {199, 1, 103, 198, -1, 99}, {196, 15, 96, 198, 12, 92}, {200, 2, 100, 194, 1, 97},
						{187, 3, 97, 196, 0, 97}, {195, 1, 116, 193, 0, 109}, {157, 1, 123, 156, 0, 117}, {118, 1, 70, 124, 0, 69},
						{35, 1, 140, 34, 0, 134}, {188, 0, 105, 189, 1, 108}, {185, 0, 105, 184, 1, 108}, {187, 2, 104, 186, 1, 108},
						{197, 5, 103, 195, 4, 100}, {196, 1, 105, 195, 1, 101}};
				String[] roundNames = {"cst_hatch_open", "day_tank_hatch_open", "day_tank", "edg_front", "edg_steps", "edg_top", "edg_exhaust",
						"edg_fuel_inlet", "fuel_trench_outside", "fuel_floor_inside", "day_tank_vent_roof", "edg_left_steps", "edg_left_walkway",
						"diesel_door_level", "control_door_level", "turbine_door_level", "aux_door_level", "panels_left", "panels_right",
						"panels_wide", "edg_air_intake", "edg_stringers"};
				for (int i = 0; i < roundShots.length; i++) {
					int[] t = roundShots[i];
					shotAt(context, singleplayer, wx.applyAsInt(t[0]), y + t[1], wz.applyAsInt(t[2]), wx.applyAsInt(t[3]), y + t[4], wz.applyAsInt(t[5]),
							"jugcraft_plant_medium_r12_" + roundNames[i]);
				}
				// Round 18: the rebuilt control building (site x 136..175, z 88..117; its floors walked at site y 0, 7, 14, 23):
				// the battery rooms with their cables up to the tray and across to the DC switchboards, the entrance, the
				// stairs, the cable spreading room, the control room and the top floor's air conditioning.
				int[][] ctrlShots = {
						{144, 0, 95, 139, 0, 90}, {141, 1, 93, 144, 3, 89}, {153, 0, 97, 148, 0, 89}, {142, 0, 115, 142, 0, 109},
						{140, 0, 112, 147, 0, 110}, {170, 0, 109, 169, 0, 116}, {164, 0, 113, 164, 3, 116}, {170, 0, 99, 171, 5, 93},
						{151, 7, 101, 151, 9, 90}, {151, 14, 103, 151, 16, 90}, {141, 14, 102, 148, 16, 91}, {156, 23, 102, 148, 24, 94},
						{142, 1, 126, 142, 3, 117}, {145, 14, 114, 141, 14, 112},
						// Framed glass (round 19): the vestibule's glass at its doors, the glass beside the speed gates, the
						// shift supervisor's glazing from the control room.
						{139, 0, 115, 141, 1, 113}, {142, 0, 111, 138, 1, 109}, {142, 14, 97, 141, 15, 100},
						// The speed gates (round 22): the bank from the hall, shut; from the side, up close.
						{142, 0, 112, 142, 1, 109}, {139, 0, 111, 142, 0, 110},
						// The control room (round 23): the reactor console in line with the boards, the reactor coolant
						// board up close, the operators' consoles.
						{151, 14, 96, 151, 15, 90}, {147, 14, 95, 147, 15, 90}, {149, 14, 101, 146, 14, 96},
						// The remote shutdown room (round 24): the panel from the mat, the room from the door, the
						// panel's screen and trip switches up close.
						{141, 1, 101, 141, 1, 98}, {137, 2, 101, 144, 0, 99}, {141, 1, 100, 141, 1, 98}};
				String[] ctrlNames = {"battery_1a", "battery_1a_riser", "dc_room_1a", "entrance_vestibule", "security_counter", "battery_1b",
						"dc_room_1b_board", "stairs", "cable_spreading", "control_room", "control_room_supervisor", "hvac_floor", "entrance_outside",
						"support_center", "vestibule_door_glass", "gate_glass", "supervisor_glass", "speed_gates", "speed_gates_side", "reactor_console", "rcs_board",
						"operator_consoles", "remote_shutdown_panel", "remote_shutdown_room", "remote_shutdown_screen"};
				for (int i = 0; i < ctrlShots.length; i++) {
					int[] t = ctrlShots[i];
					shotAt(context, singleplayer, wx.applyAsInt(t[0]), y + t[1], wz.applyAsInt(t[2]), wx.applyAsInt(t[3]), y + t[4], wz.applyAsInt(t[5]),
							"jugcraft_plant_medium_r23_ctrl_" + ctrlNames[i]);
				}
				// Walking up the exit lane from inside (out of spectator, so the gate sees you): its doors slide back into
				// their pedestals' slots and the lights go green.
				singleplayer.getServer().runCommand("gamemode creative @p");
				shotAt(context, singleplayer, wx.applyAsInt(143), y, wz.applyAsInt(108), wx.applyAsInt(143), y, wz.applyAsInt(112),
						"jugcraft_plant_medium_r23_ctrl_speed_gate_open");
				singleplayer.getServer().runCommand("gamemode spectator @p");
				// The remote shutdown panel with control transferred to it: the keys turned, the amber lamps lit, its
				// screen saying so; then handed back.
				singleplayer.getServer().runOnServer(minecraft -> {
					net.minecraft.server.level.ServerLevel level = minecraft.overworld();
					BlockPos at = new BlockPos(wx.applyAsInt(138), baseY, wz.applyAsInt(98));
					if (!(level.getBlockEntity(at) instanceof io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity panel)
							|| panel.kind() != io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind.REMOTE_SHUTDOWN_PANEL) {
						throw new AssertionError("No remote shutdown panel at " + at.toShortString() + ": " + level.getBlockState(at));
					}
					panel.linkShutdownPanel(level);
					if (panel.linked == null || !(level.getBlockEntity(panel.linked) instanceof io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity vessel)) {
						throw new AssertionError("The remote shutdown panel found no reactor");
					}
					vessel.core.remote = true;
					vessel.changed();
					panel.linkShutdownPanel(level);
				});
				context.waitTicks(20);
				shotAt(context, singleplayer, wx.applyAsInt(140), y + 1, wz.applyAsInt(100), wx.applyAsInt(140), y + 1, wz.applyAsInt(98),
						"jugcraft_plant_medium_r24_rsp_transferred");
				singleplayer.getServer().runOnServer(minecraft -> {
					net.minecraft.server.level.ServerLevel level = minecraft.overworld();
					BlockPos at = new BlockPos(wx.applyAsInt(138), baseY, wz.applyAsInt(98));
					if (level.getBlockEntity(at) instanceof io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity panel && panel.linked != null
							&& level.getBlockEntity(panel.linked) instanceof io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity vessel) {
						vessel.core.remote = false;
						vessel.changed();
						panel.linkShutdownPanel(level);
					}
				});
				// The switchgear room (round 25): its two load centres working. The buses part charged, train A's batteries
				// half down so its chargers draw through the CHARGER breaker (its ammeter reading, the chargers' meters
				// charging); the room from the west end, each lineup from the aisle, the CHARGER breaker up close; then
				// two breakers opened (green lamps, green flags) and the chargers' room.
				singleplayer.getServer().runOnServer(minecraft -> {
					net.minecraft.server.level.ServerLevel level = minecraft.overworld();
					int found = 0;
					for (int gx = 155; gx <= 167; gx++) {
						for (int gz = 87; gz <= 101; gz++) {
							if (level.getBlockEntity(new BlockPos(wx.applyAsInt(gx), baseY, wz.applyAsInt(gz))) instanceof io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity gear
									&& gear.bus != null) {
								gear.bus.setAmount(io.github.jimbozoomer.jugcraft.nuclear.Switchgear.CAPACITY * 87 / 100);
								gear.changed();
								found++;
							}
						}
					}
					if (found != 2) {
						throw new AssertionError("The switchgear room should have two load centres: " + found);
					}
					for (int gx = 136; gx <= 146; gx++) {
						BlockPos at = new BlockPos(wx.applyAsInt(gx), baseY, wz.applyAsInt(89));
						var bank = io.github.jimbozoomer.jugcraft.nuclear.StationBatteryBlock.battery(level, at);
						if (bank != null) {
							bank.energy.setAmount(io.github.jimbozoomer.jugcraft.nuclear.StationBatteryBlockEntity.CAPACITY / 2);
						}
					}
				});
				context.waitTicks(100);
				int[][] gearShots = {{157, 1, 94, 165, 0, 93}, {161, 1, 96, 161, 1, 89}, {161, 1, 91, 161, 1, 98}, {163, 1, 93, 163, 0, 90},
						{151, 1, 93, 150, 1, 89},
						// Round 25b: the feeder conduits up from the load centres' tops to the ceiling, and upstairs in the
						// cable spreading room, up through the floor and into the trays.
						{163, 2, 95, 164, 4, 89}, {162, 2, 92, 160, 4, 98}, {163, 8, 95, 164, 8, 89}, {160, 8, 93, 160, 9, 98},
						// Round 25b: the rebuilt emergency light on the west wall, its heads aimed down at the floor.
						{160, 2, 94, 157, 3, 92},
						// Round 26 (8a): the DC rooms' motor control centres, whole and up close.
						{153, 1, 97, 151, 1, 101}, {152, 1, 99, 152, 1, 101}, {161, 1, 111, 162, 1, 107}};
				String[] gearNames = {"switchgear_room", "switchgear_north", "switchgear_south", "switchgear_charger_breaker", "dc_1a_chargers",
						"conduits_north", "conduits_south", "spreading_conduits_north", "spreading_conduits_south", "emergency_light",
						"dc_1a_mcc", "dc_1a_mcc_close", "dc_1b_mcc"};
				for (int i = 0; i < gearShots.length; i++) {
					int[] t = gearShots[i];
					shotAt(context, singleplayer, wx.applyAsInt(t[0]), y + t[1], wz.applyAsInt(t[2]), wx.applyAsInt(t[3]), y + t[4], wz.applyAsInt(t[5]),
							"jugcraft_plant_medium_r25_" + gearNames[i]);
				}
				// Round 26 (8d): DC room 1A's 1A-1 group, charger, inverter and regulating transformer side by side (building x
				// 16..18 on the north wall), the transformer's conduit up from its top to the ladder tray overhead; the
				// transformer alive on no load (POWER ON, 480 V in, 120 V out, 0 A) while its inverter runs on its battery.
				shotAt(context, singleplayer, wx.applyAsInt(153), y + 1, wz.applyAsInt(94), wx.applyAsInt(153), y + 2, wz.applyAsInt(89),
						"jugcraft_plant_medium_r26_dc_1a_xf_group");
				// Round 26 (8e): DC room 1A's two 120 VAC vital panels on its east wall (building x 19, z 10 and 11), their
				// conduits up into the tray, POWER ON lit.
				shotAt(context, singleplayer, wx.applyAsInt(151), y + 1, wz.applyAsInt(98), wx.applyAsInt(155), y + 2, wz.applyAsInt(99),
						"jugcraft_plant_medium_r26_dc_1a_vital_panels");
				// Round 26 (8f): DC room 1A's ground detector and battery monitor at the end of the row (building x 19, z 1),
				// healthy (each side half the bus to ground), its screen and cell bars lit; then with its + BUS test held
				// (+ meter near zero, - meter the whole bus, + GND and ALARM lit, + GND FAULT on the screen).
				shotAt(context, singleplayer, wx.applyAsInt(155), y + 1, wz.applyAsInt(92), wx.applyAsInt(155), y + 2, wz.applyAsInt(89),
						"jugcraft_plant_medium_r26_dc_1a_ground_detector");
				singleplayer.getServer().runOnServer(minecraft -> {
					net.minecraft.server.level.ServerLevel level = minecraft.overworld();
					if (level.getBlockEntity(new BlockPos(wx.applyAsInt(155), baseY, wz.applyAsInt(89))) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity gd) {
						// Held for the shot: a press that lasts a minute.
						io.github.jimbozoomer.jugcraft.nuclear.GroundDetectorBlock.press(level, gd, 1, level.getGameTime() + 1200);
					}
				});
				context.waitTicks(10);
				shotAt(context, singleplayer, wx.applyAsInt(155), y + 1, wz.applyAsInt(92), wx.applyAsInt(155), y + 2, wz.applyAsInt(89),
						"jugcraft_plant_medium_r26_dc_1a_ground_test");
				// Round 26 (#9): Battery Room 1A (building x 1..9, z 1..8): its second string along the south wall, then its
				// west wall's exhaust fan over the louvre, hydrogen monitor and sensor head, hydrogen sign.
				shotAt(context, singleplayer, wx.applyAsInt(141), y + 1, wz.applyAsInt(91), wx.applyAsInt(141), y + 1, wz.applyAsInt(96),
						"jugcraft_plant_medium_r26_battery_room_1a_second_string");
				shotAt(context, singleplayer, wx.applyAsInt(142), y + 1, wz.applyAsInt(92), wx.applyAsInt(137), y + 2, wz.applyAsInt(92),
						"jugcraft_plant_medium_r26_battery_room_1a_fittings");
				// Round 26 (#10): the relay room (building x 1..18, z 19..28, its floor at building y 8): row A's protection
				// racks from the north aisle, row B's SSPS bays and nuclear instrument drawers, row C's relay panels' names,
				// the SOE recorder's corner, the header tray in over the door through its fire stop to the rows' crosses.
				int[][] relayShots = {{17, 8, 19, 13, 9, 21}, {4, 8, 24, 3, 9, 22}, {12, 8, 24, 12, 9, 22}, {13, 8, 23, 13, 9, 25},
						{15, 8, 27, 18, 8, 25}, {9, 8, 20, 8, 12, 18}};
				String[] relayNames = {"row_a", "ssps", "nis", "relay_panels", "soe", "header"};
				for (int i = 0; i < relayShots.length; i++) {
					int[] t = relayShots[i];
					shotAt(context, singleplayer, wx.applyAsInt(136 + t[0]), y + t[1], wz.applyAsInt(88 + t[2]), wx.applyAsInt(136 + t[3]), y + t[4],
							wz.applyAsInt(88 + t[5]), "jugcraft_plant_medium_r26_relay_room_" + relayNames[i]);
				}
				// Round 26 (#11): the Control Room HVAC Equipment Room (the top floor, walked at building y 24): the room from
				// the stair door, the HVAC panel, train A's filtration unit and air handler from their operating sides, its
				// chiller's panel, the dampers over its plenum, its intake and monitors, the south bay; then on EMERGENCY
				// (both trains on filtered air) the panel and the filtration unit again.
				int[][] hvacShots = {{33, 24, 15, 12, 25, 6}, {15, 24, 5, 15, 25, 1}, {6, 24, 6, 4, 25, 6}, {12, 24, 6, 10, 25, 6},
						{11, 24, 20, 8, 25, 20}, {9, 25, 5, 7, 28, 2}, {5, 24, 6, 5, 26, 1}, {14, 24, 24, 22, 25, 28}};
				String[] hvacNames = {"room", "panel", "efu_a", "ahu_a", "chiller_a", "dampers_a", "intake_a", "south_bay"};
				for (int i = 0; i < hvacShots.length; i++) {
					int[] t = hvacShots[i];
					shotAt(context, singleplayer, wx.applyAsInt(136 + t[0]), y + t[1], wz.applyAsInt(88 + t[2]), wx.applyAsInt(136 + t[3]), y + t[4],
							wz.applyAsInt(88 + t[5]), "jugcraft_plant_medium_r26_hvac_" + hvacNames[i]);
				}
				singleplayer.getServer().runOnServer(minecraft -> {
					net.minecraft.server.level.ServerLevel level = minecraft.overworld();
					for (BlockPos at : BlockPos.betweenClosed(new BlockPos(wx.applyAsInt(136 + 18), y + 22, wz.applyAsInt(88 + 4)),
							new BlockPos(wx.applyAsInt(136 + 12), y + 27, wz.applyAsInt(88 - 1)))) {
						if (level.getBlockEntity(at) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity hv
								&& io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.kind(hv.getBlockState()) == io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.CONTROL) {
							hv.mode = io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.EMERGENCY;
							io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.tick(hv, level, io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.CONTROL);
						}
					}
				});
				context.waitTicks(100);
				shotAt(context, singleplayer, wx.applyAsInt(136 + 15), y + 24, wz.applyAsInt(88 + 5), wx.applyAsInt(136 + 15), y + 25, wz.applyAsInt(88 + 1),
						"jugcraft_plant_medium_r26_hvac_panel_emergency");
				shotAt(context, singleplayer, wx.applyAsInt(136 + 6), y + 24, wz.applyAsInt(88 + 6), wx.applyAsInt(136 + 4), y + 25, wz.applyAsInt(88 + 6),
						"jugcraft_plant_medium_r26_hvac_efu_a_emergency");
				// Round 26 (#12): the Technical Support Center (the second floor, walked at building y 15): the room from the
				// NRC room's corner, the video wall, a console from behind its chair, the status boards, the communications
				// console, the NRC desk, the door with the frisker and area monitor; then on an ALERT (the reactor tripped
				// over its high temperature alarm) the video wall, the classification board and the boards again.
				int[][] tscShots = {{17, 15, 23, 1, 17, 26}, {7, 15, 25, 1, 17, 25}, {6, 15, 27, 3, 15, 27}, {6, 15, 25, 6, 17, 21},
						{17, 15, 26, 17, 15, 29}, {16, 15, 24, 19, 15, 23}, {12, 15, 25, 13, 16, 21}};
				String[] tscNames = {"room", "video_wall", "console", "boards", "comms", "nrc", "door"};
				java.util.concurrent.atomic.AtomicReference<BlockPos> tripped = new java.util.concurrent.atomic.AtomicReference<>();
				for (int i = 0; i < tscShots.length; i++) {
					int[] t = tscShots[i];
					shotAt(context, singleplayer, wx.applyAsInt(136 + t[0]), y + t[1], wz.applyAsInt(88 + t[2]), wx.applyAsInt(136 + t[3]), y + t[4],
							wz.applyAsInt(88 + t[5]), "jugcraft_plant_medium_r26_tsc_" + tscNames[i]);
				}
				singleplayer.getServer().runOnServer(minecraft -> {
					net.minecraft.server.level.ServerLevel level = minecraft.overworld();
					io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity hub = null;
					for (BlockPos at : BlockPos.betweenClosed(new BlockPos(wx.applyAsInt(136), y + 14, wz.applyAsInt(88 + 21)),
							new BlockPos(wx.applyAsInt(136 + 3), y + 20, wz.applyAsInt(88 + 29)))) {
						if (level.getBlockEntity(at) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity p
								&& io.github.jimbozoomer.jugcraft.nuclear.TscRoom.kind(p.getBlockState()) == io.github.jimbozoomer.jugcraft.nuclear.TscRoom.HUB) {
							hub = p;
						}
					}
					if (hub == null) {
						return;
					}
					BlockPos here = hub.getBlockPos();
					io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity vessel = null;
					for (var e : io.github.jimbozoomer.jugcraft.nuclear.EquipmentRegistry.of(level).anchors().entrySet()) {
						if (e.getValue().kind().reactor() && level.getBlockEntity(e.getKey()) instanceof io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity v
								&& (vessel == null || e.getKey().distSqr(here) < vessel.getBlockPos().distSqr(here))) {
							vessel = v;
						}
					}
					if (vessel != null) {
						tripped.set(vessel.getBlockPos());
						vessel.core.scrammed = true;
						vessel.core.temperature = io.github.jimbozoomer.jugcraft.nuclear.ReactorCore.ALARM_TEMP + 5;
					}
					io.github.jimbozoomer.jugcraft.nuclear.TscRoom.tick(hub, level, io.github.jimbozoomer.jugcraft.nuclear.TscRoom.HUB);
				});
				context.waitTicks(60);
				shotAt(context, singleplayer, wx.applyAsInt(136 + 7), y + 15, wz.applyAsInt(88 + 25), wx.applyAsInt(136 + 1), y + 17, wz.applyAsInt(88 + 25),
						"jugcraft_plant_medium_r26_tsc_video_wall_alert");
				shotAt(context, singleplayer, wx.applyAsInt(136 + 6), y + 16, wz.applyAsInt(88 + 25), wx.applyAsInt(136 + 1), y + 18, wz.applyAsInt(88 + 25),
						"jugcraft_plant_medium_r26_tsc_class_board_alert");
				shotAt(context, singleplayer, wx.applyAsInt(136 + 6), y + 15, wz.applyAsInt(88 + 25), wx.applyAsInt(136 + 6), y + 17, wz.applyAsInt(88 + 21),
						"jugcraft_plant_medium_r26_tsc_boards_alert");
				shotAt(context, singleplayer, wx.applyAsInt(136 + 6), y + 15, wz.applyAsInt(88 + 27), wx.applyAsInt(136 + 3), y + 15, wz.applyAsInt(88 + 27),
						"jugcraft_plant_medium_r26_tsc_console_alert");
				// The reactor reset (the TSC's ALERT stays declared until it is terminated) for the shots after these.
				singleplayer.getServer().runOnServer(minecraft -> {
					net.minecraft.server.level.ServerLevel level = minecraft.overworld();
					if (tripped.get() != null && level.getBlockEntity(tripped.get()) instanceof io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity v) {
						v.core.scrammed = false;
					}
				});
				singleplayer.getServer().runOnServer(minecraft -> {
					net.minecraft.server.level.ServerLevel level = minecraft.overworld();
					for (int gx = 155; gx <= 167; gx++) {
						for (int gz = 87; gz <= 92; gz++) {
							if (level.getBlockEntity(new BlockPos(wx.applyAsInt(gx), baseY, wz.applyAsInt(gz))) instanceof io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity gear
									&& gear.bus != null) {
								gear.breakers &= ~(1 << 5 | 1 << 6);
								gear.changed();
							}
						}
					}
					// MCC 1A's CHGR 1 opened too (round 26): its handle down, its green lamp lit.
					for (int gx = 150; gx <= 154; gx++) {
						if (level.getBlockEntity(new BlockPos(wx.applyAsInt(gx), baseY, wz.applyAsInt(101))) instanceof io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity mcc
								&& mcc.kind() == io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind.MCC_LINEUP) {
							mcc.breakers &= ~(1 << 4);
							mcc.changed();
						}
					}
				});
				context.waitTicks(40);
				shotAt(context, singleplayer, wx.applyAsInt(163), y + 1, wz.applyAsInt(93), wx.applyAsInt(163), y, wz.applyAsInt(90),
						"jugcraft_plant_medium_r25_switchgear_breakers_open");
				shotAt(context, singleplayer, wx.applyAsInt(152), y + 1, wz.applyAsInt(99), wx.applyAsInt(152), y + 1, wz.applyAsInt(101),
						"jugcraft_plant_medium_r26_dc_1a_mcc_chgr1_open");
				// Diesel room A's generator started (it turns over, fires and runs up to speed: the coupling and fan
				// turning, the gauges up, smoke at the stack); its day tank drawn down so the storage tank refills it, and
				// the day tank's screen showing it filling.
				singleplayer.getServer().runOnServer(minecraft -> {
					net.minecraft.server.level.ServerLevel level = minecraft.overworld();
					BlockPos edg = new BlockPos(wx.applyAsInt(184), baseY, wz.applyAsInt(97));
					if (!(level.getBlockEntity(edg) instanceof io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity generator)
							|| generator.kind() != io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind.EMERGENCY_DIESEL_GENERATOR) {
						throw new AssertionError("No diesel generator at " + edg.toShortString() + ": " + level.getBlockState(edg));
					}
					generator.running = true;
					generator.runningSince = level.getGameTime();
					generator.changed();
					BlockPos day = new BlockPos(wx.applyAsInt(195), baseY, wz.applyAsInt(93));
					if (level.getBlockEntity(day) instanceof io.github.jimbozoomer.jugcraft.nuclear.StorageTankPortBlockEntity tank) {
						try (var t = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
							tank.fluid.extract(tank.fluid.variant, 20 * net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET, t);
							t.commit();
						}
					}
				});
				context.waitTicks(100);
				context.runOnClient(client -> {
					if (io.github.jimbozoomer.jugcraft.client.DieselSounds.playing() < 1) {
						throw new AssertionError("The running diesel generator makes no sound");
					}
					// Sound through walls: in the room it is heard plainly; outside, through the concrete wall, muffled
					// to a tenth; from two rooms away (through the dividing wall too), not at all.
					net.minecraft.world.phys.Vec3 engine = new net.minecraft.world.phys.Vec3(wx.applyAsInt(190) + 0.5, baseY + 5.5, wz.applyAsInt(100) + 0.5);
					double inside = io.github.jimbozoomer.jugcraft.client.SoundOcclusion.walls(client.level,
							new net.minecraft.world.phys.Vec3(wx.applyAsInt(186) + 0.5, baseY + 1.6, wz.applyAsInt(105) + 0.5), engine);
					double outside = io.github.jimbozoomer.jugcraft.client.SoundOcclusion.walls(client.level,
							new net.minecraft.world.phys.Vec3(wx.applyAsInt(190) + 0.5, baseY + 1.6, wz.applyAsInt(114) + 0.5), engine);
					double otherRoom = io.github.jimbozoomer.jugcraft.client.SoundOcclusion.walls(client.level,
							new net.minecraft.world.phys.Vec3(wx.applyAsInt(214) + 0.5, baseY + 1.6, wz.applyAsInt(114) + 0.5), engine);
					if (inside > 0.3 || outside < 0.9 || otherRoom < 1.9) {
						throw new AssertionError("Walls between the generator and you: in its room " + inside + ", outside " + outside
								+ ", beyond the next room " + otherRoom);
					}
				});
				// The day tank, drawn down, is filling again from the storage tank through the buried line.
				singleplayer.getServer().runOnServer(minecraft -> {
					BlockPos day = new BlockPos(wx.applyAsInt(195), baseY, wz.applyAsInt(93));
					if (!(minecraft.overworld().getBlockEntity(day) instanceof io.github.jimbozoomer.jugcraft.nuclear.StorageTankPortBlockEntity tank)
							|| tank.inRate() <= 0) {
						throw new AssertionError("No fuel is reaching day tank DT-1A through the buried line");
					}
				});
				int[][] edgShots = {{182, 2, 100, 184, 1, 100}, {196, 3, 105, 193, 1, 100}, {200, 2, 101, 197, 2, 100}, {196, 2, 106, 193, 1, 103},
						{186, 23, 116, 185, 18, 100}};
				String[] edgNames = {"edg_running_front", "edg_running_coupling", "edg_running_fan", "edg_steps", "edg_running_smoke"};
				for (int i = 0; i < edgShots.length; i++) {
					int[] t = edgShots[i];
					shotAt(context, singleplayer, wx.applyAsInt(t[0]), y + t[1], wz.applyAsInt(t[2]), wx.applyAsInt(t[3]), y + t[4], wz.applyAsInt(t[5]),
							"jugcraft_plant_medium_r12_" + edgNames[i]);
				}
				livePanels(context, singleplayer, wx, wz, y, baseY);
				radwaste(context, singleplayer, wx, wz, y);
				// The tank screens: the day tank refilling, and the condensate tank.
				BlockPos dayOutlet = new BlockPos(wx.applyAsInt(195), y, wz.applyAsInt(93));
				BlockPos cstOutlet = new BlockPos(wx.applyAsInt(226), y + 1, wz.applyAsInt(125));
				// (Each from beside its tank: in a full run an earlier test may leave the render distance short, and the
				// screen reads the tank from the client's world.)
				server.runCommand("tp @p %d.5 %d %d.5".formatted(wx.applyAsInt(182), y + 2, wz.applyAsInt(100)));
				context.waitTicks(10);
				singleplayer.getConnection().waitForChunksRender();
				context.runOnClient(client -> io.github.jimbozoomer.jugcraft.nuclear.StorageTanks.openScreen.accept(dayOutlet));
				context.waitTicks(10);
				context.takeScreenshot("jugcraft_plant_medium_r12_day_tank_screen");
				context.runOnClient(client -> client.gui.setScreen(null));
				server.runCommand("tp @p %d.5 %d %d.5".formatted(wx.applyAsInt(226), y + 2, wz.applyAsInt(118)));
				context.waitTicks(10);
				singleplayer.getConnection().waitForChunksRender();
				context.runOnClient(client -> io.github.jimbozoomer.jugcraft.nuclear.StorageTanks.openScreen.accept(cstOutlet));
				context.waitTicks(10);
				context.takeScreenshot("jugcraft_plant_medium_r12_cst_screen");
				context.runOnClient(client -> client.gui.setScreen(null));
				context.waitTicks(5);
				// Holding a pipe: the runs hidden in the wall show through it.
				server.runCommand("item replace entity @p weapon.mainhand with jugcraft:steel_fluid_pipe");
				shotAt(context, singleplayer, wx.applyAsInt(201), y + 2, wz.applyAsInt(113), wx.applyAsInt(199), y - 1, wz.applyAsInt(109),
						"jugcraft_plant_medium_r12_wall_pipe_overlay");
				server.runCommand("item replace entity @p weapon.mainhand with air");
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
