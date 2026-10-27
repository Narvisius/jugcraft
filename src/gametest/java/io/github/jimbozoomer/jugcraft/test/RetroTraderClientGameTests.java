package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.world.RetroTrader;
import io.github.jimbozoomer.jugcraft.world.RetroTraderJam;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.entity.EntityTypeTest;

/**
 * The Retro Trader up close, in a real client: his look (overlay and modelled beanie), his face from near, and his
 * stomp-clap ({@link RetroTraderJam}) started as a completed trade and a closed screen would start it, the chance
 * forced. Screenshots: the look, a close-up of the face, a three-quarter view, then the routine on its first stomp,
 * its first clap, the finale's "ta-da" and after it (arms folded again). Run alone with
 * {@code ./gradlew runClientGameTest -PclientTests=RetroTraderClientGameTests}.
 */
public class RetroTraderClientGameTests implements FabricClientGameTest {
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
			server.runCommand("gamerule sendCommandFeedback false");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x - 8, y - 1, z - 8, x + 8, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 8, x + 8, y + 6, z + 8));
			context.getInput().pressKey(options -> options.keyToggleGui);

			// He faces south (+z), towards the camera.
			server.runCommand("summon minecraft:villager %d.5 %d %d.5 {NoAI:1b,Silent:1b,Rotation:[0f,0f]}".formatted(x, y, z));
			server.runOnServer(minecraft -> makeTrader(minecraft.overworld()));
			context.waitTicks(10);

			shoot(context, server, singleplayer, "tp @p %d.5 %d %s 180 5".formatted(x, y, (z + 3.2) + ""), "jugcraft_retro_trader_look");
			shoot(context, server, singleplayer, "tp @p %d.5 %s %s 180 0".formatted(x, (y + 0.15) + "", (z + 1.45) + ""), "jugcraft_retro_trader_face");
			shoot(context, server, singleplayer, "tp @p %s %d %s 135 8".formatted((x + 2.6) + "", y, (z + 2.9) + ""), "jugcraft_retro_trader_three_quarter");

			server.runCommand("tp @p %s %d %s 160 5".formatted((x + 1.4) + "", y, (z + 3.4) + ""));
			context.waitTicks(5);
			long started = server.computeOnServer(minecraft -> startJam(minecraft.overworld()));
			if (started < 0) {
				throw new AssertionError("The Retro Trader's stomp-clap did not start after a trade");
			}
			// RetroTraderJam's timeline, counted from the client's start (CLIENT_DELAY after the message).
			context.waitTicks(RetroTraderJam.CLIENT_DELAY + RetroTraderJam.INTRO + 2);
			context.takeScreenshot("jugcraft_retro_trader_jam_stomp");
			context.waitTicks(RetroTraderJam.BEAT - 2);
			context.takeScreenshot("jugcraft_retro_trader_jam_clap");
			context.waitTicks(RetroTraderJam.FINALE + 8 - RetroTraderJam.INTRO - RetroTraderJam.BEAT);
			context.takeScreenshot("jugcraft_retro_trader_jam_ta_da");
			context.waitTicks(RetroTraderJam.LENGTH - RetroTraderJam.FINALE);
			context.takeScreenshot("jugcraft_retro_trader_jam_after");
		}
	}

	private static void shoot(ClientGameTestContext context, TestServerContext server, TestSingleplayerContext singleplayer, String tp, String name) {
		server.runCommand(tp);
		context.waitTicks(15);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
	}

	private static List<? extends Villager> traders(ServerLevel level) {
		return level.getEntities(EntityTypeTest.forClass(Villager.class), Villager::isNoAi);
	}

	private static void makeTrader(ServerLevel level) {
		var profession = BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(RetroTrader.PROFESSION);
		for (Villager villager : traders(level)) {
			villager.setVillagerData(villager.getVillagerData().withProfession(profession).withLevel(2));
		}
	}

	/** Starts his jam as a completed trade and a closed screen do; returns the jam's game time, or -1. */
	private static long startJam(ServerLevel level) {
		RetroTraderJam.alwaysForTests = true;
		try {
			long started = -1;
			for (Villager villager : traders(level)) {
				RetroTraderJam.traded(villager);
				if (RetroTraderJam.stoppedTrading(villager)) {
					started = RetroTraderJam.lastJam(villager);
				}
			}
			return started;
		} finally {
			RetroTraderJam.alwaysForTests = false;
		}
	}
}
