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
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.AABB;

/**
 * Client game test for Arms VIII (batch 59): the four thrown arms hanging in flight before the camera (their 3D models,
 * the javelin and harpoon point first, the francisca tumbling, the chakram spinning flat); the javelin wound back with the
 * real use key, from the front and in first person, and in flight as it leaves; and real throws: a steel javelin let go at
 * a still pig strikes it for its damage and comes down as itself, and a bronze chakram thrown past a pig comes back to the
 * thrower's inventory, each read back from the server (CI job {@code client}).
 */
public class ArmsVIIIClientGameTests implements FabricClientGameTest {
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
			server.runCommand("gamerule minecraft:advance_time false");
			server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x - 12, y - 1, z - 18, x + 12, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 12, y, z - 18, x + 12, y + 8, z + 8));
			context.waitTicks(10);

			// The four in flight, drifting slowly across, before the camera.
			server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 10", x + 0.5, y, z + 0.5));
			String[] arms = {"steel_javelin", "bronze_francisca", "steel_chakram", "bronze_harpoon"};
			for (int i = 0; i < arms.length; i++) {
				server.runCommand(String.format(Locale.ROOT, "summon jugcraft:thrown_arm %.1f %.1f %.1f {NoGravity:1b,Motion:[0.02d,0.0d,0.0d],"
						+ "Item:{id:\"jugcraft:%s\",count:1}}", x - 2.5 + i * 1.8, y + 1.2, z - 4.5, arms[i]));
			}
			context.waitTicks(15);
			context.takeScreenshot("jugcraft_arms_viii_in_flight");
			server.runCommand("kill @e[type=jugcraft:thrown_arm]");

			// The javelin wound back with the use key, from the front, then in first person.
			server.runCommand("item replace entity @p weapon.mainhand with jugcraft:steel_javelin");
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(20);
			context.getInput().holdKey(options -> options.keyUse);
			context.waitTicks(15);
			context.takeScreenshot("jugcraft_arms_viii_javelin_wind");
			context.getInput().releaseKey(options -> options.keyUse);
			context.waitTicks(3);
			context.takeScreenshot("jugcraft_arms_viii_javelin_loosed");
			server.runCommand("kill @e[type=jugcraft:thrown_arm]");
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			server.runCommand("item replace entity @p weapon.mainhand with jugcraft:bronze_francisca");
			context.waitTicks(20);
			// The GUI may be hidden by an earlier test in the shard, and hiding it hides the hand: show it for these shots.
			context.getInput().pressKey(options -> options.keyToggleGui);
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_arms_viii_first_person_francisca");
			context.getInput().holdKey(options -> options.keyUse);
			context.waitTicks(12);
			context.takeScreenshot("jugcraft_arms_viii_first_person_francisca_wind");
			context.getInput().releaseKey(options -> options.keyUse);
			context.getInput().pressKey(options -> options.keyToggleGui);
			context.waitTicks(10);
			server.runCommand("kill @e[type=jugcraft:thrown_arm]");
			server.runCommand("kill @e[type=minecraft:item]");

			// A real throw in survival: a steel javelin let go at a still pig 6 blocks ahead.
			server.runCommand("gamemode survival @p");
			server.runCommand("clear @p");
			server.runCommand(String.format(Locale.ROOT, "summon minecraft:pig %.1f %d %.1f {NoAI:1b,PersistenceRequired:1b}", x + 0.5, y, z - 5.5));
			server.runCommand("item replace entity @p weapon.mainhand with jugcraft:steel_javelin");
			float pitch = (float) Math.toDegrees(Math.atan2(1.62 - 0.45, 6.0));
			server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 %.1f", x + 0.5, y, z + 0.5, pitch));
			context.waitTicks(20);
			context.getInput().holdKey(options -> options.keyUse);
			context.waitTicks(15);
			context.getInput().releaseKey(options -> options.keyUse);
			context.waitTicks(30);
			context.takeScreenshot("jugcraft_arms_viii_javelin_struck");
			AABB area = new AABB(x - 12, y - 2, z - 18, x + 12, y + 8, z + 8);
			float[] javelin = server.computeOnServer(minecraft -> {
				List<Pig> pigs = minecraft.overworld().getEntitiesOfClass(Pig.class, area);
				boolean down = !minecraft.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area,
						item -> item.getItem().is(JugcraftArms.ITEMS.get("steel_javelin"))).isEmpty();
				return pigs.isEmpty() ? new float[] {-1.0F, 0.0F} : new float[] {pigs.getFirst().getMaxHealth() - pigs.getFirst().getHealth(), down ? 1.0F : 0.0F};
			});
			Jugcraft.LOGGER.info("[arms viii client] javelin: the pig took {}, the javelin came down {}", javelin[0], javelin[1] > 0.0F);
			float expected = JugcraftArms.thrown("javelin", "steel").damage();
			check(Math.abs(javelin[0] - expected) < 1.0E-3F && javelin[1] > 0.0F,
					"The javelin took " + javelin[0] + " from the pig (not " + expected + ") or did not come down (" + javelin[1] + ")");
			server.runCommand("kill @e[type=minecraft:pig]");
			server.runCommand("kill @e[type=minecraft:item]");

			// A bronze chakram thrown level past a pig: it comes back to the thrower's inventory.
			server.runCommand(String.format(Locale.ROOT, "summon minecraft:pig %.1f %d %.1f {NoAI:1b,PersistenceRequired:1b}", x + 0.5, y, z - 4.5));
			server.runCommand("item replace entity @p weapon.mainhand with jugcraft:bronze_chakram");
			server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 12", x + 0.5, y, z + 0.5));
			context.waitTicks(20);
			context.getInput().holdKey(options -> options.keyUse);
			context.waitTicks(10);
			context.getInput().releaseKey(options -> options.keyUse);
			context.waitTicks(4);
			context.takeScreenshot("jugcraft_arms_viii_chakram_out");
			context.waitTicks(56);
			int[] chakram = server.computeOnServer(minecraft -> {
				Inventory inventory = minecraft.getPlayerList().getPlayers().getFirst().getInventory();
				int count = 0;
				for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
					count += inventory.getItem(slot).is(JugcraftArms.ITEMS.get("bronze_chakram")) ? 1 : 0;
				}
				List<Pig> pigs = minecraft.overworld().getEntitiesOfClass(Pig.class, area);
				return new int[] {count, pigs.isEmpty() ? -1 : Math.round(pigs.getFirst().getMaxHealth() - pigs.getFirst().getHealth())};
			});
			Jugcraft.LOGGER.info("[arms viii client] chakram: back in the inventory {}, the pig took {}", chakram[0], chakram[1]);
			check(chakram[0] == 1, "The chakram is in the inventory " + chakram[0] + " times after its throw, not once");
		}
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}
}
