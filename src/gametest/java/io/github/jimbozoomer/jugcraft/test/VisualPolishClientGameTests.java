package io.github.jimbozoomer.jugcraft.test;

import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.HumanoidArm;

/** Close runtime views of the owner's reported art issues; screenshots require visual inspection. */
public class VisualPolishClientGameTests implements FabricClientGameTest {
	private static final List<String> WEAPONS = List.of("bronze_rapier", "steel_rapier", "gilded_rapier", "needle_rapier",
			"bronze_moonblade", "steel_moonblade", "runebound_moonblade", "soulreaver", "steel_nodachi", "runebound_nodachi", "bogfang");
	private static final List<String> WOODS = List.of("chestnut", "larch", "maple", "aspen", "fir", "dead", "jacaranda",
			"willow", "palm", "cypress", "redwood", "eucalyptus", "mahogany", "cedar", "cinnamon");

	@Override
	public void runTest(ClientGameTestContext context) {
		HumanoidArm originalHand = context.computeOnClient(client -> client.options.mainHand().get());
		try (var singleplayer = context.worldBuilder().adjustSettings(
				creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			var server = singleplayer.getServer();
			BlockPos origin = context.computeOnClient(client -> client.player.blockPosition().offset(0, 8, 0));
			int x = origin.getX(), y = origin.getY(), z = origin.getZ();
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x - 6, y - 1, z - 6, x + 47, y - 1, z + 10));
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamerule minecraft:advance_time false");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			for (int i = 0; i < WOODS.size(); i++) {
				String wood = WOODS.get(i);
				int at = x + 3 * i;
				for (int up = 0; up < 2; up++) {
					server.runCommand("setblock %d %d %d jugcraft:%s_log".formatted(at, y + up, z, wood));
					server.runCommand("setblock %d %d %d jugcraft:stripped_%s_log".formatted(at + 1, y + up, z, wood));
				}
				server.runCommand("setblock %d %d %d jugcraft:%s_log[axis=z]".formatted(at, y + 2, z, wood));
				server.runCommand("setblock %d %d %d jugcraft:stripped_%s_log[axis=z]".formatted(at + 1, y + 2, z, wood));
				server.runCommand("setblock %d %d %d jugcraft:%s_log[axis=x]".formatted(at, y + 3, z, wood));
				server.runCommand("setblock %d %d %d jugcraft:stripped_%s_log[axis=x]".formatted(at + 1, y + 3, z, wood));
			}
			context.runOnClient(client -> {
				client.options.setCameraType(CameraType.FIRST_PERSON);
				if (!client.gui.hud.isHidden()) client.gui.hud.toggle();
			});
			for (int group = 0; group < 5; group++) {
				server.runCommand("tp @p %d %d %d 180 0".formatted(x + group * 9 + 4, y + 1, z + 7));
				context.waitTicks(25);
				singleplayer.getConnection().waitForChunksRender();
				context.takeScreenshot("jugcraft_vanilla_logs_" + group);
			}
			server.runCommand("tp @p %d %d %d 0 0".formatted(x + 4, y, z + 4));
			context.runOnClient(client -> { if (client.gui.hud.isHidden()) client.gui.hud.toggle(); });
			for (HumanoidArm hand : HumanoidArm.values()) {
				context.runOnClient(client -> client.options.mainHand().set(hand));
				for (String weapon : WEAPONS) {
					server.runCommand("item replace entity @p weapon.mainhand with jugcraft:" + weapon);
					// Slow two-handed weapons need time to finish vanilla's equip/cooldown dip.
					context.waitTicks(45);
					context.takeScreenshot("jugcraft_polish_" + weapon + "_" + hand.getSerializedName());
					context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
					context.waitTicks(4);
					context.takeScreenshot("jugcraft_polish_" + weapon + "_" + hand.getSerializedName() + "_third");
					context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
				}
			}
		} finally {
			context.runOnClient(client -> {
				client.options.mainHand().set(originalHand);
				client.options.setCameraType(CameraType.FIRST_PERSON);
			});
		}
	}
}
