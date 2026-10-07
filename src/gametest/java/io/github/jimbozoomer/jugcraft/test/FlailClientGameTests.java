package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.arms.FlailHeads;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Client game test for the flail's swinging head (docs/features/arms-restyle.md, "The flail's head swings"; 5 October
 * 2026, the owner: "flails should have an animated ball that actually flails around"): the heads load; held on guard,
 * the ball (and the Bonecarved Flail's skull) hangs below the handle's eye; through a strike (from the front, 3 and 8
 * ticks in) it stays on its chain and swings; walking off, it trails behind but never sinks into the holder's hip or
 * arm; a quick turn swings it out; armor stands hold flails in either hand; the inventory's paper doll draws it without
 * flinging it; and on screen, in first person, the guard and the strike (CI job {@code client}). Kept short: it runs in
 * the busiest client shard.
 */
public class FlailClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		int loaded = context.computeOnClient(client -> FlailHeads.loadedItems());
		Jugcraft.LOGGER.info("[flail] {} flail heads loaded", loaded);
		check(loaded >= 3, "Only " + loaded + " flail heads loaded (the bronze, steel and Bonecarved Flails' at least)");
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

			// Third person, from the front: the guard, the ball (the Bonecarved Flail's skull) hanging below the eye.
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			for (String flail : List.of("bronze_flail", "steel_flail", "bonecarved_flail")) {
				server.runCommand("item replace entity @p weapon.mainhand with jugcraft:" + flail);
				context.waitTicks(20);
				context.takeScreenshot("jugcraft_flail_" + flail.replace("_flail", "") + "_guard");
				float hang = context.computeOnClient(client -> FlailHeads.hang(client.player));
				String chain = context.computeOnClient(client -> FlailHeads.describe(client.player));
				Jugcraft.LOGGER.info("[flail] {} guard: hang {} ({})", flail, hang, chain);
				check(hang > 0.7F, "On guard the " + flail + "'s ball does not hang below its eye: " + chain);
				check(context.computeOnClient(client -> FlailHeads.withinReach(client.player)), "The " + flail + "'s chain parted: " + chain);
			}
			server.runCommand("item replace entity @p weapon.mainhand with jugcraft:steel_flail");
			context.waitTicks(20);

			// A strike, caught 3 and 8 ticks after the attack key: mid-swing, the ball trailing, then whipping round.
			int last = 0;
			for (int tick : new int[] {3, 8}) {
				if (last == 0) {
					context.getInput().pressKey(options -> options.keyAttack);
				}
				context.waitTicks(tick - last);
				last = tick;
				context.takeScreenshot("jugcraft_flail_strike_" + tick);
				String chain = context.computeOnClient(client -> FlailHeads.describe(client.player));
				Jugcraft.LOGGER.info("[flail] strike +{} ticks: {}", tick, chain);
				check(context.computeOnClient(client -> FlailHeads.withinReach(client.player)), "The flail's chain parted mid-strike: " + chain);
			}
			context.waitTicks(20);

			// Walking off from the guard, seen from behind: the ball trails back past the hip and the arm, and keeps out of
			// them (its spikes may graze, but never sink a pixel in), the holder's legs swinging as they walk.
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(10);
			context.computeOnClient(client -> FlailHeads.bodyClearance(client.player));
			context.getInput().holdKey(options -> options.keyUp);
			context.waitTicks(6);
			context.takeScreenshot("jugcraft_flail_walk");
			context.waitTicks(14);
			context.getInput().releaseKey(options -> options.keyUp);
			float clearance = context.computeOnClient(client -> FlailHeads.bodyClearance(client.player));
			Jugcraft.LOGGER.info("[flail] walking: the ball came within {} blocks of the body (beyond its spikes)", clearance);
			check(!Float.isNaN(clearance), "The flail's ball was never kept clear of the holder's body: its pose was not read");
			check(clearance > -1.0F / 16.0F, "Walking, the flail's ball sank into the holder's body by " + -clearance + " blocks");
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			// Back where the walk began (a jump that far starts the chain again, hanging), to settle before the turn.
			server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 0", x + 0.5, y, z + 0.5));
			context.waitTicks(15);

			// A quick turn of the whole body, on the spot: the ball swings out behind it.
			server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 120 0", x + 0.5, y, z + 0.5));
			context.waitTicks(2);
			context.takeScreenshot("jugcraft_flail_turn");
			context.waitTicks(20);

			// Armor stands: a bronze flail in the main hand (the right), a steel one in the off hand (the left).
			server.runCommand(String.format(Locale.ROOT, "summon minecraft:armor_stand %.1f %d %.1f {ShowArms:1b,NoBasePlate:1b,Rotation:[0f,0f],"
					+ "equipment:{mainhand:{id:\"jugcraft:bronze_flail\",count:1}}}", x - 1.0, y, z - 2.5));
			server.runCommand(String.format(Locale.ROOT, "summon minecraft:armor_stand %.1f %d %.1f {ShowArms:1b,NoBasePlate:1b,Rotation:[0f,0f],"
					+ "equipment:{offhand:{id:\"jugcraft:steel_flail\",count:1}}}", x + 2.0, y, z - 2.5));
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 10", x + 0.5, y, z + 2.5));
			server.runCommand("item replace entity @p weapon.mainhand with minecraft:air");
			// The HUD as an earlier test left it, put back at the end; hidden for the scenery shot.
			boolean hudWasHidden = context.computeOnClient(client -> client.gui.hud.isHidden());
			setHudHidden(context, true);
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_flail_stands");
			server.runCommand("kill @e[type=minecraft:armor_stand]");

			// The inventory's paper doll holding a flail: drawn as the world left the chain, never flung.
			server.runCommand("gamemode survival @p");
			server.runCommand("item replace entity @p weapon.mainhand with jugcraft:steel_flail");
			context.waitTicks(10);
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitTicks(15);
			context.takeScreenshot("jugcraft_flail_inventory");
			context.setScreen(() -> null);
			server.runCommand("gamemode creative @p");

			// First person: the guard (the handle held up across the view, the ball hanging in sight) and a strike, with the
			// HUD shown (hiding it also hides the hand).
			server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 0", x + 0.5, y, z + 0.5));
			server.runCommand("item replace entity @p weapon.mainhand with jugcraft:bronze_flail");
			setHudHidden(context, false);
			context.waitTicks(25);
			context.takeScreenshot("jugcraft_flail_first_person_guard");
			float hang = context.computeOnClient(client -> FlailHeads.hangFirstPerson(client.player.getMainArm() == HumanoidArm.RIGHT ? 0 : 1));
			Jugcraft.LOGGER.info("[flail] first person guard: hang {}", hang);
			check(hang > 0.7F, "On screen the flail's ball does not hang below its eye: " + hang);
			context.getInput().pressKey(options -> options.keyAttack);
			context.waitTicks(3);
			context.takeScreenshot("jugcraft_flail_first_person_strike");
			String strike = context.computeOnClient(client -> FlailHeads.describeFirstPerson(0));
			Jugcraft.LOGGER.info("[flail] first person strike: {}", strike);
			setHudHidden(context, hudWasHidden);
		}
	}

	/** Hides or shows the HUD, hand and chat (F1) whatever state an earlier test left it in (as DroneClientGameTests). */
	private static void setHudHidden(ClientGameTestContext context, boolean hidden) {
		context.runOnClient(client -> {
			if (client.gui.hud.isHidden() != hidden) {
				client.gui.hud.toggle();
			}
		});
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
