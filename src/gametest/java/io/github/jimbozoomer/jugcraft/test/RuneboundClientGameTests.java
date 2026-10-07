package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.MeshItemModels;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.model.loading.v1.UnbakedModelDeserializer;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;

/**
 * Client game test for the Runebound arms' smooth meshes (tools/arms_mesh.py, client/MeshItemModels.java;
 * docs/features/arms-vii.md "Runebound meshes"): the four on a close rack of armor stands by day and at midnight (their
 * runes and crystals glowing), each held from the front by day, the Moonblade and Nodachi in first person by day and
 * the Staff at night, and a Moonblade with the enchantment glint on its mesh (CI job {@code client}). Before the shots
 * it checks that the mesh model loader is registered, that at least the four arms were baked as meshes and that no
 * mesh model fell back to its box model, so the meshes, not their box fallbacks, are what the shots show.
 */
public class RuneboundClientGameTests implements FabricClientGameTest {
	private static final List<String> ARMS = List.of("runebound_nodachi", "runebound_moonblade", "runebound_staff",
			"runebound_war_hammer");

	@Override
	public void runTest(ClientGameTestContext context) {
		boolean registered = context.computeOnClient(client -> UnbakedModelDeserializer.get(MeshItemModels.TYPE) != null);
		int baked = MeshItemModels.BAKED.get();
		int fallbacks = MeshItemModels.FALLBACKS.get();
		Jugcraft.LOGGER.info("[runebound client] mesh item model loader registered: {}, meshes baked: {}, box fallbacks: {}",
				registered, baked, fallbacks);
		if (!registered) {
			throw new AssertionError("The jugcraft:mesh model loader is not registered: the Runebound arms would draw as boxes");
		}
		if (baked < ARMS.size()) {
			throw new AssertionError("Only " + baked + " mesh item models were baked, expected at least " + ARMS.size());
		}
		if (fallbacks != 0) {
			throw new AssertionError(fallbacks + " mesh item models fell back to their box models (see the log)");
		}
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			hud(context, false);
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			int x = origin.getX();
			int y = origin.getY();
			int z = origin.getZ();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("gamerule minecraft:advance_time false");
			server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x - 8, y - 1, z - 12, x + 14, y - 1, z + 6));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 12, x + 14, y + 8, z + 6));
			context.waitTicks(10);

			// A close rack: the four on armor stands facing south before a dark wall, by day and at midnight.
			server.runCommand("fill %d %d %d %d %d %d minecraft:dark_oak_planks".formatted(x - 1, y, z - 7, x + 9, y + 4, z - 7));
			for (int i = 0; i < ARMS.size(); i++) {
				stand(server, x + 1.5 + i * 1.8, y, z - 4.5, ARMS.get(i));
			}
			context.waitTicks(20);
			shoot(context, singleplayer, x + 4, y + 1, z - 1, 180, 14, "jugcraft_runebound_rack");
			server.runCommand("time set midnight");
			shoot(context, singleplayer, x + 4, y + 1, z - 1, 180, 14, "jugcraft_runebound_rack_night");
			server.runCommand("time set noon");
			server.runCommand("kill @e[type=minecraft:armor_stand]");

			// Each held, from the front, by day.
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			for (String arm : ARMS) {
				ready(context, server, x + 4, y, z, "jugcraft:" + arm);
				context.takeScreenshot("jugcraft_runebound_held_" + arm.substring("runebound_".length()));
			}
			// The glint on the mesh: a Moonblade made to shine as an enchanted one does.
			ready(context, server, x + 4, y, z, "jugcraft:runebound_moonblade[minecraft:enchantment_glint_override=true]");
			context.takeScreenshot("jugcraft_runebound_held_glint");

			// In first person (the hand shows only with the HUD): two by day, the Staff at night.
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			hud(context, true);
			for (String arm : List.of("runebound_moonblade", "runebound_nodachi")) {
				ready(context, server, x + 4, y, z, "jugcraft:" + arm);
				context.takeScreenshot("jugcraft_runebound_first_person_" + arm.substring("runebound_".length()));
			}
			server.runCommand("time set midnight");
			ready(context, server, x + 4, y, z, "jugcraft:runebound_staff");
			context.takeScreenshot("jugcraft_runebound_first_person_staff_night");
			server.runCommand("time set noon");
			hud(context, false);
		}
	}

	/** Shows or hides the HUD (and with it the hand), whatever an earlier test in this client left. */
	private static void hud(ClientGameTestContext context, boolean shown) {
		context.runOnClient(client -> {
			if (client.gui.hud.isHidden() == shown) {
				client.gui.hud.toggle();
			}
		});
		context.waitTicks(2);
	}

	private static void ready(ClientGameTestContext context, TestServerContext server, int x, int y, int z, String stack) {
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 0", x + 0.5, y, z + 0.5));
		server.runCommand("item replace entity @p weapon.mainhand with " + stack);
		context.waitTicks(20);
	}

	private static void stand(TestServerContext server, double x, int y, double z, String item) {
		server.runCommand(String.format(Locale.ROOT, "summon minecraft:armor_stand %.1f %d %.1f {ShowArms:1b,NoBasePlate:1b,Rotation:[0f,0f],"
				+ "equipment:{mainhand:{id:\"jugcraft:%s\",count:1}}}", x, y, z, item));
	}

	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, int yaw, int pitch,
			String name) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("setblock %d %d %d minecraft:barrier".formatted(x, y - 1, z));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f %d %d", x + 0.5, y, z + 0.5, yaw, pitch));
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
		server.runCommand("setblock %d %d %d minecraft:air".formatted(x, y - 1, z));
	}
}
