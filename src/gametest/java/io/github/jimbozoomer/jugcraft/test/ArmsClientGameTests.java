package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.weapons.JugcraftArms;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;

/**
 * Client game test for the arms (batch 42): every arm held by an armor stand (each kind's in-hand pose; a rack of
 * bronze, then one of steel), every arm in an item frame (the inventory sprite, also shot close up, four kinds a shot),
 * and the player holding a greatsword, a halberd and a lance in first person, a greatsword from the front, and parrying
 * with a longsword and charging with a lance while holding use (CI job {@code client}).
 */
public class ArmsClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			int x = origin.getX();
			int y = origin.getY();
			int z = origin.getZ();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x - 4, y - 1, z - 22, x + 68, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 4, y, z - 22, x + 68, y + 8, z + 8));
			context.waitTicks(10);

			// Bronze arms on a rack of armor stands facing south, steel arms on a second rack nearer the camera, and all of
			// them in item frames high on a wall behind.
			List<String> bronze = JugcraftArms.ITEMS.keySet().stream().filter(id -> id.startsWith("bronze_")).toList();
			List<String> steel = JugcraftArms.ITEMS.keySet().stream().filter(id -> id.startsWith("steel_")).toList();
			// Batch 42's nine kinds a metal, Arms II's eight (batch 45), Arms III's four (batch 46), Arms IV's five (batch 47),
			// Arms V's six (batch 48), Arms VI's two (batch 55; its bows and shields are ArmsVIClientGameTests') and Arms VIII's
			// four thrown arms (batch 59): the racks and the frame wall widen with them.
			int count = bronze.size();
			server.runCommand("fill %d %d %d %d %d %d minecraft:spruce_planks".formatted(x, y, z - 13, x + count + 6, y + 5, z - 13));
			for (int i = 0; i < count; i++) {
				stand(server, x + 1.5 + i * 1.5, y, z - 10.5, bronze.get(i));
				stand(server, x + 1.5 + i * 1.5, y, z - 3.5, steel.get(i));
				frame(server, x + 3 + i, y + 3, z - 12, bronze.get(i));
				frame(server, x + 3 + i, y + 4, z - 12, steel.get(i));
			}
			context.waitTicks(20);
			// Hide the HUD, hand and chat for the scenery shots, whatever an earlier test in this client left (as
			// OfrendaClientGameTests does).
			context.runOnClient(client -> {
				if (!client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});
			// Each rack in parts of seven stands, from far enough back to see each part whole.
			String[] parts = {"", "_ii", "_iii", "_iv", "_v", "_vi", "_vii"};
			for (int part = 0; part * 7 < count; part++) {
				int at = x + 6 + part * 21 / 2;
				shoot(context, singleplayer, at, y + 1, z - 5, 180, 12, "jugcraft_arms_bronze_rack" + parts[part]);
				shoot(context, singleplayer, at, y + 1, z + 2, 180, 12, "jugcraft_arms_steel_rack" + parts[part]);
			}
			shoot(context, singleplayer, x + 3 + count / 2, y + 3, z - 1, 180, -5, "jugcraft_arms_frames");
			// The frames close up, four kinds (bronze below, steel above) a shot, so each 16x16 icon shows large enough to
			// judge (docs/features/arms-icons-16.md). The bronze rack stands where the camera goes, so the racks go first
			// (with anything they drop).
			server.runCommand("kill @e[type=minecraft:armor_stand]");
			server.runCommand("kill @e[type=minecraft:item]");
			for (int group = 0; group * 4 < count; group++) {
				double centre = x + 3 + group * 4 + Math.min(4, count - group * 4) / 2.0;
				closeUp(context, singleplayer, centre, y + 2, z - 10.5, "jugcraft_arms_frames_close_" + (group + 1));
			}
			context.runOnClient(client -> {
				if (client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});

			// In the hand, first person (the hotbar shows), then the greatsword from the front.
			server.runCommand("tp @p %d %d %d 180 0".formatted(x + 7, y, z + 5));
			for (String arm : List.of("steel_greatsword", "steel_halberd", "steel_lance")) {
				server.runCommand("item replace entity @p weapon.mainhand with jugcraft:" + arm);
				context.waitTicks(30);
				context.takeScreenshot("jugcraft_arms_held_" + arm);
			}
			server.runCommand("item replace entity @p weapon.mainhand with jugcraft:steel_greatsword");
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(30);
			context.takeScreenshot("jugcraft_arms_greatsword_front");

			// Holding use: a longsword parries (from the front and in first person), a lance charges.
			server.runCommand("item replace entity @p weapon.mainhand with jugcraft:steel_longsword");
			context.waitTicks(10);
			context.getInput().holdKey(options -> options.keyUse);
			context.waitTicks(15);
			boolean blocking = context.computeOnClient(client -> client.player.isBlocking());
			Jugcraft.LOGGER.info("[arms client] holding use with a longsword: blocking {}", blocking);
			context.takeScreenshot("jugcraft_arms_parry_front");
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_arms_parry");
			context.getInput().releaseKey(options -> options.keyUse);
			server.runCommand("item replace entity @p weapon.mainhand with jugcraft:steel_lance");
			context.waitTicks(10);
			context.getInput().holdKey(options -> options.keyUse);
			context.waitTicks(15);
			boolean charging = context.computeOnClient(client -> client.player.isUsingItem());
			Jugcraft.LOGGER.info("[arms client] holding use with a lance: using {}", charging);
			context.takeScreenshot("jugcraft_arms_lance_charge");
			context.getInput().releaseKey(options -> options.keyUse);
		}
	}

	private static void stand(TestServerContext server, double x, int y, double z, String arm) {
		server.runCommand(String.format(Locale.ROOT, "summon minecraft:armor_stand %.1f %d %.1f {ShowArms:1b,NoBasePlate:1b,Rotation:[0f,0f],"
				+ "equipment:{mainhand:{id:\"jugcraft:%s\",count:1}}}", x, y, z, arm));
	}

	private static void frame(TestServerContext server, int x, int y, int z, String arm) {
		server.runCommand("summon minecraft:item_frame %d %d %d {Facing:3b,Fixed:1b,Item:{id:\"jugcraft:%s\",count:1}}".formatted(x, y, z, arm));
	}

	/** From about two and a half blocks before the frame wall, looking at the two rows of frames. */
	private static void closeUp(ClientGameTestContext context, TestSingleplayerContext singleplayer, double x, int y, double z,
			String name) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("setblock %d %d %d minecraft:barrier".formatted((int) Math.floor(x), y - 1, (int) Math.floor(z)));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.2f %d %.2f 180 -6", x, y, z));
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
	}

	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z,
			int yaw, int pitch, String name) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("setblock %d %d %d minecraft:barrier".formatted(x, y - 1, z));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f %d %d", x + 0.5, y, z + 0.5, yaw, pitch));
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
	}
}
