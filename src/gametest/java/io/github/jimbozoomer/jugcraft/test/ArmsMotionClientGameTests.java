package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.arms.ArmsMotion;
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
 * Client game test for the arms motion (batch 43): the motion files load; the player, seen from the front, holds each
 * kind's guard and is caught two ticks into a swing (a two-handed kind's at its blow); three blows in a row with a longsword move through its combo; and
 * the guard and a stroke on screen in first person, the flail's among them with its ball swinging (CI job {@code client}). The four mixins are applied as the game
 * starts, so a target 26.3 no longer has stops the game here.
 */
public class ArmsMotionClientGameTests implements FabricClientGameTest {
	private static final List<String> KINDS = List.of("longsword", "greatsword", "rapier", "flanged_mace", "war_hammer", "glaive",
			"halberd", "lance", "dagger", "sabre", "estoc", "battle_axe", "flail", "scythe", "quarterstaff", "pike", "zweihander",
			"maul", "executioner", "bill", "labrys", "battleblade", "war_fork", "kama", "war_pick", "twinblade", "nodachi", "earthbreaker",
			"katar", "moonblade", "kusarigama");

	@Override
	public void runTest(ClientGameTestContext context) {
		int loaded = context.computeOnClient(client -> ArmsMotion.loadedKinds());
		Jugcraft.LOGGER.info("[arms motion] {} of {} kinds' motion loaded", loaded, ArmsMotion.KINDS.size());
		if (loaded != ArmsMotion.KINDS.size()) {
			throw new AssertionError("Only " + loaded + " of " + ArmsMotion.KINDS.size() + " kinds' motion loaded");
		}
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x - 8, y - 1, z - 8, x + 8, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 8, x + 8, y + 6, z + 8));
			server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 0", x + 0.5, y, z + 0.5));
			context.waitTicks(20);

			// Third person, from the front: each kind's guard, then two ticks into a swing (a two-handed kind's: at its blow).
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			for (String kind : KINDS) {
				hold(context, server, kind);
				context.takeScreenshot("jugcraft_motion_" + kind + "_guard");
				strike(context, kind, "jugcraft_motion_" + kind + "_strike");
				context.waitTicks(25);
			}
			// A combo: three longsword blows in quick succession go forehand, backhand, thrust.
			hold(context, server, "longsword");
			for (int blow = 1; blow <= 3; blow++) {
				strike(context, "longsword", "jugcraft_motion_longsword_combo_" + blow);
				context.waitTicks(12);
			}
			// First person: the guard and a stroke on screen. The HUD has been hidden by this point in every CI run, and hiding
			// it also hides the hand, so it is shown with one press of its toggle (26.3's Options has no hideGui field to set).
			context.getInput().pressKey(options -> options.keyToggleGui);
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			for (String kind : List.of("longsword", "war_hammer", "glaive", "dagger", "scythe", "pike", "zweihander", "maul", "labrys",
					"kama", "flail")) {
				hold(context, server, kind);
				// Vanilla lowers the arm on screen until the attack cooldown, reset by the swap, recovers (28 ticks for a
				// war hammer).
				context.waitTicks(25);
				context.takeScreenshot("jugcraft_motion_" + kind + "_first_person_guard");
				strike(context, kind, "jugcraft_motion_" + kind + "_first_person_strike");
				context.waitTicks(25);
			}
		}
	}

	private static void hold(ClientGameTestContext context, TestServerContext server, String kind) {
		server.runCommand("item replace entity @p weapon.mainhand with jugcraft:steel_" + kind);
		context.waitTicks(15);
	}

	private static void strike(ClientGameTestContext context, String kind, String screenshot) {
		context.getInput().pressKey(options -> options.keyAttack);
		JugcraftArms.Heavy heavy = JugcraftArms.TWO_HANDED.get(kind);
		context.waitTicks(heavy != null ? heavy.strike() : 2);
		context.takeScreenshot(screenshot);
		String pose = context.computeOnClient(client -> ArmsMotion.describe(client.player));
		float swing = context.computeOnClient(client -> client.player.getSwingAnimation(1.0F));
		Jugcraft.LOGGER.info("[arms motion] {}: swing {} -> {}", screenshot, swing, pose);
	}
}
