package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.SmoothFlight;
import io.github.jimbozoomer.jugcraft.agriculture.HotAirBalloon;
import io.github.jimbozoomer.jugcraft.agriculture.HotAirBalloonItem;
import io.github.jimbozoomer.jugcraft.agriculture.Pibal;
import io.github.jimbozoomer.jugcraft.agriculture.PibalItem;
import io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery;
import io.github.jimbozoomer.jugcraft.artillery.JugcraftTowerGuns;
import io.github.jimbozoomer.jugcraft.artillery.ObservationBalloon;
import io.github.jimbozoomer.jugcraft.artillery.SelfPropelledHowitzer;
import io.github.jimbozoomer.jugcraft.artillery.SiegeMortar;
import io.github.jimbozoomer.jugcraft.artillery.TowerGun;
import io.github.jimbozoomer.jugcraft.landship.JugcraftLandships;
import io.github.jimbozoomer.jugcraft.landship.Landship;
import io.github.jimbozoomer.jugcraft.walker.DieselWalker;
import io.github.jimbozoomer.jugcraft.walker.JugcraftWalkers;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Client game test for the big guns, the Landship, the Diesel Walker and the balloons after the 5 October 2026 art fixes
 * (docs/features/big-guns-art-fixes.md). Close views against the sky, where a hole would show sky and a flicker would
 * show as a speckled face: the Grand Mortar's barrel, plinth and muzzle; the Triple Battery and Bastion Autocannon low
 * down, their bores and mantlet slots; the siege mortar's deck and the howitzer from the front right; the Landship's
 * muzzle and turret ring; the Diesel Walker's hips; and the whole Observation Balloon envelope. Then it rides the
 * Observation Balloon up and checks the client eases after the server: the client's balloon (and so its rider) climbs
 * with it a tick at a time instead of standing still and jumping. Before the ride, the inventory with every war machine's
 * 16x16 icon (its map in tools/item_icons/). CI job {@code client}.
 */
public class BigGunsClientGameTests implements FabricClientGameTest {
	/** The items whose icons are maps in tools/item_icons/, in the order they fill the hotbar and the inventory. */
	private static final String[] ICONS = {"grand_mortar", "bastion_mortar", "fortress_rifle", "bastion_autocannon", "triple_battery",
			"siege_mortar", "self_propelled_howitzer", "flak_gun", "landship", "diesel_walker", "zeppelin", "observation_balloon",
			"range_finder", "great_shell", "heavy_shell", "flak_shell", "cannon_shell"};

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			context.runOnClient(client -> {
				if (!client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			int x = origin.getX();
			int y = origin.getY();
			int z = origin.getZ();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("time set 1000");
			server.runCommand("weather clear");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("gamerule minecraft:spawn_mobs false");
			fill(server, x - 30, y - 3, z - 40, x + 30, y - 1, z + 20, "minecraft:grass_block");
			fill(server, x - 30, y, z - 40, x + 30, y + 30, z + 20, "minecraft:air");
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(40);

			// The Grand Mortar on its tower, barrel raised, from about six blocks at its trunnions' height.
			shoot(context, singleplayer, x - 18, y + 7, z - 6, x - 13.5, y + 8.0, z - 13.5, "jugcraft_tower_guns_close");
			// The Triple Battery and the Bastion Autocannon low down, from in front: bores, sleeves and mantlet slots.
			shoot(context, singleplayer, x + 6, y + 5, z - 4, x + 6.5, y + 5.0, z - 13.5, "jugcraft_tower_guns_low");
			// The siege mortar and the self-propelled howitzer from the front right.
			shoot(context, singleplayer, x + 4, y + 3, z + 10, x - 6.0, y + 1.5, z + 2.0, "jugcraft_big_guns_close");
			// The Landship's muzzle and turret ring, and the Diesel Walker's hips, close.
			shoot(context, singleplayer, x + 19, y + 3, z + 14, x + 18.5, y + 2.4, z + 8.5, "jugcraft_landship_close");
			shoot(context, singleplayer, x + 24, y + 1, z + 3, x + 26.5, y + 2.0, z - 1.5, "jugcraft_diesel_walker_close");
			// The Observation Balloon on its anchor, the whole envelope in frame against the sky.
			shoot(context, singleplayer, x - 20, y + 3, z + 14, x - 20.5, y + 6.0, z + 0.5, "jugcraft_observation_balloon");

			// The icons, in the hotbar and the inventory's first rows (before the ride, whose checks can fail the test).
			for (String item : ICONS) {
				server.runCommand("give @p jugcraft:" + item);
			}
			context.waitTicks(5);
			context.setScreen(() -> new InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
			context.waitTicks(10);
			context.takeScreenshot("jugcraft_war_machine_icons");
			context.setScreen(() -> null);

			rise(context, singleplayer, origin);
		}
	}

	/**
	 * Fills a box in horizontal slabs of at most 32,768 blocks, vanilla's limit for one /fill (a bigger fill fails, and
	 * runCommand does not report it).
	 */
	private static void fill(TestServerContext server, int x0, int y0, int z0, int x1, int y1, int z1, String block) {
		int layers = Math.max(1, 32768 / ((x1 - x0 + 1) * (z1 - z0 + 1)));
		for (int bottom = y0; bottom <= y1; bottom += layers) {
			server.runCommand("fill %d %d %d %d %d %d %s".formatted(x0, bottom, z0, x1, Math.min(y1, bottom + layers - 1), z1, block));
		}
	}

	/** Stands the camera on a barrier at (x, y, z) facing the point given, and takes a screenshot once the world has drawn. */
	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, double fx, double fy,
			double fz, String name) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("fill %d %d %d %d %d %d minecraft:barrier replace minecraft:air".formatted(x, y - 1, z, x, y - 1, z));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f facing %.2f %.2f %.2f", x + 0.5, y, z + 0.5, fx, fy, fz));
		context.waitTicks(30);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
	}

	/**
	 * Rides the Observation Balloon up and samples, each tick of its steady climb, its height on the server and on the
	 * client. The client must ease after the server (a LinearInterpolationHandler), stay close behind it, and rise every
	 * tick rather than stand still and then jump; the pibal and the hot-air balloon must ease too.
	 */
	private static void rise(ClientGameTestContext context, TestSingleplayerContext singleplayer, BlockPos origin) {
		TestServerContext server = singleplayer.getServer();
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		int id = server.computeOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			ObservationBalloon balloon = new ObservationBalloon(JugcraftArtillery.BALLOON, level);
			balloon.snapTo(x + 0.5, y, z + 16.5, 0.0F, 0.0F);
			level.addFreshEntity(balloon);
			return balloon.getId();
		});
		server.runCommand(String.format(Locale.ROOT, "tp @p %d %d %d", x, y, z + 15));
		context.waitTicks(10);
		server.runCommand("execute as @p at @s run ride @s mount @e[type=jugcraft:observation_balloon,limit=1,sort=nearest,distance=..4]");
		context.waitTicks(20);
		boolean easing = context.computeOnClient(client -> {
			Entity balloon = client.level.getEntity(id);
			return balloon != null && SmoothFlight.isEasing(balloon);
		});
		double climb = JugcraftArtillery.BALLOON_CLIMB;
		List<double[]> samples = new ArrayList<>();
		for (int tick = 0; tick < 60; tick++) {
			context.waitTick();
			double serverY = server.computeOnServer(minecraft -> {
				Entity balloon = minecraft.overworld().getEntity(id);
				return balloon == null ? Double.NaN : balloon.getY();
			});
			double clientY = context.computeOnClient(client -> {
				Entity balloon = client.level.getEntity(id);
				return balloon == null ? Double.NaN : balloon.getY();
			});
			samples.add(new double[] {serverY, clientY});
		}
		StringBuilder log = new StringBuilder("[jugcraft big guns client test] observation balloon rise (server, client):");
		int stalls = 0;
		int jumps = 0;
		double worstLag = 0.0;
		int missing = 0;
		for (int i = 0; i < samples.size(); i++) {
			double[] s = samples.get(i);
			log.append(String.format(Locale.ROOT, " %.3f/%.3f", s[0], s[1]));
			if (Double.isNaN(s[0]) || Double.isNaN(s[1])) {
				missing++;
				continue;
			}
			worstLag = Math.max(worstLag, Math.abs(s[0] - s[1]));
			if (i > 0 && !Double.isNaN(samples.get(i - 1)[1])) {
				double step = s[1] - samples.get(i - 1)[1];
				if (step < 0.25 * climb) {
					stalls++;
				} else if (step > 2.0 * climb) {
					jumps++;
				}
			}
		}
		System.out.println(log);
		System.out.printf(Locale.ROOT, "[jugcraft big guns client test] easing %s, worst lag %.3f blocks, %d stalls, %d jumps%n", easing, worstLag,
				stalls, jumps);

		// The pibal and the hot-air balloon use the same handler.
		int pibal = server.computeOnServer(minecraft -> {
			Pibal released = PibalItem.release(minecraft.overworld(), Vec3.atCenterOf(origin.offset(-6, 1, 16)));
			return released == null ? -1 : released.getId();
		});
		int hot = server.computeOnServer(minecraft -> {
			HotAirBalloon placed = HotAirBalloonItem.setUp(minecraft.overworld(), origin.offset(10, -1, 16), HotAirBalloon.Kind.HARVEST, 0.0F, 400);
			return placed == null ? -1 : placed.getId();
		});
		context.waitTicks(10);
		boolean pibalEasing = context.computeOnClient(client -> {
			Entity e = client.level.getEntity(pibal);
			return e != null && SmoothFlight.isEasing(e);
		});
		boolean hotEasing = context.computeOnClient(client -> {
			Entity e = client.level.getEntity(hot);
			return e != null && SmoothFlight.isEasing(e);
		});
		System.out.printf(Locale.ROOT, "[jugcraft big guns client test] pibal easing %s, hot-air balloon easing %s%n", pibalEasing, hotEasing);
		server.runCommand("execute as @p run ride @s dismount");

		if (missing > 0) {
			throw new AssertionError("The Observation Balloon was missing on the server or the client in " + missing + " of " + samples.size()
					+ " samples, so its rise could not be checked");
		}
		if (!easing || !pibalEasing || !hotEasing) {
			throw new AssertionError("A balloon does not ease on the client: observation " + easing + ", pibal " + pibalEasing + ", hot-air " + hotEasing);
		}
		if (worstLag > 1.0) {
			throw new AssertionError("The client's Observation Balloon fell " + worstLag + " blocks behind the server's (its easing never ran?)");
		}
		if (stalls + jumps > samples.size() / 10) {
			throw new AssertionError("The client's Observation Balloon rose unevenly: " + stalls + " ticks standing still and " + jumps
					+ " jumps in " + samples.size() + " ticks of steady climb");
		}
	}

	/** Towers with the tower guns, the field guns, the Landship, the Diesel Walker and the balloon, each facing south. */
	private static void build(ServerLevel level, BlockPos origin) {
		tower(level, origin.offset(-14, 0, -14), "grand_mortar", 3);
		tower(level, origin.offset(2, 0, -14), "triple_battery", 2);
		tower(level, origin.offset(10, 0, -14), "bastion_autocannon", 2);
		SiegeMortar mortar = new SiegeMortar(JugcraftArtillery.SIEGE_MORTAR, level);
		mortar.snapTo(origin.getX() - 9.5, origin.getY(), origin.getZ() + 0.5, 0.0F, 0.0F);
		mortar.face(20.0F);
		level.addFreshEntity(mortar);
		SelfPropelledHowitzer howitzer = new SelfPropelledHowitzer(JugcraftArtillery.HOWITZER, level);
		howitzer.snapTo(origin.getX() - 2.5, origin.getY(), origin.getZ() + 2.5, 0.0F, 0.0F);
		howitzer.face(15.0F);
		level.addFreshEntity(howitzer);
		Landship landship = new Landship(JugcraftLandships.LANDSHIP, level);
		landship.snapTo(origin.getX() + 18.5, origin.getY(), origin.getZ() + 6.5, 0.0F, 0.0F);
		level.addFreshEntity(landship);
		DieselWalker walker = new DieselWalker(JugcraftWalkers.DIESEL_WALKER, level);
		walker.snapTo(origin.getX() + 26.5, origin.getY(), origin.getZ() - 2.5, 0.0F, 0.0F);
		level.addFreshEntity(walker);
		ObservationBalloon balloon = new ObservationBalloon(JugcraftArtillery.BALLOON, level);
		balloon.snapTo(origin.getX() - 20.5, origin.getY(), origin.getZ() + 0.5, 90.0F, 0.0F);
		level.addFreshEntity(balloon);
	}

	/** A stone-brick tower `height` blocks tall under a tower gun's footprint, centred on (x, z), with the gun on top. */
	private static void tower(ServerLevel level, BlockPos centre, String gun, int height) {
		var type = JugcraftTowerGuns.type(gun);
		int half = JugcraftTowerGuns.specOf(type).footprint() / 2;
		for (int dx = -half; dx <= half; dx++) {
			for (int dz = -half; dz <= half; dz++) {
				for (int dy = 0; dy < height; dy++) {
					level.setBlockAndUpdate(centre.offset(dx, dy, dz), Blocks.STONE_BRICKS.defaultBlockState());
				}
			}
		}
		TowerGun tower = new TowerGun(type, level);
		tower.snapTo(centre.getX() + 0.5, centre.getY() + height, centre.getZ() + 0.5, 0.0F, 0.0F);
		tower.face(0.0F);
		level.addFreshEntity(tower);
	}
}
