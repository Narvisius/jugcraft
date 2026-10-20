package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.creatures.chima.Chima;
import io.github.jimbozoomer.jugcraft.creatures.chima.ChimaMod;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;

/**
 * Client game test for Chima the lion cub (docs/features/chima.md): its five looks in a row on the grass (its own,
 * pumpkin, zombie, skeleton, shark) facing the camera, and a tame one sitting in its collar; from the front, from behind
 * (the tails and their stars) and up close. CI job {@code client}.
 */
public class ChimaClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			// Hide the HUD, hand and chat whatever an earlier test in this client left: CI shares the client tests out
			// between parallel jobs, and only the first job's opening test hides them.
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
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("gamerule minecraft:spawn_mobs false");
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 10, y - 3, z - 12, x + 12, y - 1, z + 10));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 10, y, z - 12, x + 12, y + 10, z + 10));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 2, y, z + 1, 180, 15, "jugcraft_chima_looks");
			shoot(context, singleplayer, x + 2, y, z - 12, 0, 18, "jugcraft_chima_tails");
			shoot(context, singleplayer, x + 1, y, z - 2, 180, 25, "jugcraft_chima_close");
			server.runCommand("kill @e[type=jugcraft:chima]");
		}
	}

	/** Stands the camera at (x, y, z) looking along yaw and pitch, and waits for the world to draw. */
	private static void place(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, int yaw, int pitch) {
		TestServerContext server = singleplayer.getServer();
		// Only into air, so standing on the ground leaves no hole in it for the later pictures.
		server.runCommand("fill %d %d %d %d %d %d minecraft:barrier replace minecraft:air".formatted(x, y - 1, z, x, y - 1, z));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f %d %d", x + 0.5, y, z + 0.5, yaw, pitch));
		context.waitTicks(20);
		singleplayer.getConnection().waitForChunksRender();
	}

	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, int yaw, int pitch,
			String name) {
		place(context, singleplayer, x, y, z, yaw, pitch);
		context.waitTicks(20);
		context.takeScreenshot(name);
	}

	/** A cub standing still at (x, y, z) facing the camera (south), in a look, tamed to {@code owner} if given. */
	private static void chima(ServerLevel level, double x, int y, double z, Chima.Skin skin, ServerPlayer owner) {
		Chima chima = ChimaMod.CHIMA.create(level, EntitySpawnReason.COMMAND);
		if (chima == null) {
			return;
		}
		chima.setNoAi(true);
		chima.setSkin(skin);
		if (owner != null) {
			chima.tameTo(owner);
		}
		chima.snapTo(x, y, z, 0.0F, 0.0F);
		chima.setYHeadRot(0.0F);
		chima.setYBodyRot(0.0F);
		level.addFreshEntity(chima);
	}

	/** The five looks in a row in front of the camera, and at the end of the row a tame one sitting. */
	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		Chima.Skin[] skins = Chima.Skin.values();
		for (int i = 0; i < skins.length; i++) {
			chima(level, x + 1.5 + (i - 2) * 1.6, y, z - 5.5, skins[i], null);
		}
		List<ServerPlayer> players = level.getServer().getPlayerList().getPlayers();
		chima(level, x + 6.3, y, z - 5.5, Chima.Skin.NORMAL, players.isEmpty() ? null : players.get(0));
	}
}
