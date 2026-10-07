package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.FacePaint;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * Client game test for face paint: the player, seen from the front in third person against a spruce wall, wearing each
 * design in turn (painted on the server and sent to the client, which draws it on the face). CI job {@code client}.
 */
public class FacePaintClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 6, y - 2, z - 6, x + 6, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 6, y, z - 6, x + 6, y + 6, z + 8));
			// A wall behind the player, who faces south towards the camera.
			server.runCommand("fill %d %d %d %d %d %d minecraft:spruce_planks".formatted(x - 4, y, z - 2, x + 4, y + 4, z - 2));
			server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 0 0", x + 0.5, y, z + 0.5));
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();
			for (FacePaint.Design design : FacePaint.Design.values()) {
				server.runOnServer(minecraft -> {
					for (ServerPlayer player : minecraft.getPlayerList().getPlayers()) {
						FacePaint.paint(player, design);
					}
				});
				context.waitTicks(10);
				context.takeScreenshot("jugcraft_face_paint_" + design.getSerializedName());
			}
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
		}
	}
}
