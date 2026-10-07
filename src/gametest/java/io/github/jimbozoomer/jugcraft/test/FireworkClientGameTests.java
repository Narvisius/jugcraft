package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.FireworkShape;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.ShowLauncherBlock;
import io.github.jimbozoomer.jugcraft.agriculture.ShowLauncherBlockEntity;
import io.github.jimbozoomer.jugcraft.client.SpookyBursts;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/**
 * Client game test for spooky fireworks: a loaded Show Launcher by day (its rockets peeking out of the tubes, the dial on
 * its front), then at midnight the four pictures burst side by side in the sky (drawn straight on this client, facing
 * the camera), and a real finale fired from the launcher, its rockets bursting where they reach (CI job {@code client}).
 */
public class FireworkClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 8, y - 3, z - 16, x + 16, y - 1, z + 6));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 16, x + 16, y + 30, z + 6));
			context.waitTicks(10);
			BlockPos launcher = new BlockPos(x + 4, y, z - 8);
			server.runOnServer(minecraft -> build(minecraft.overworld(), launcher));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			shoot(context, singleplayer, x + 4, y + 1, z - 6, 180, 45, "jugcraft_show_launcher");

			// At midnight, the four pictures side by side, burst straight on this client facing the camera.
			server.runCommand("time set midnight");
			place(context, singleplayer, x + 4, y + 1, z + 4, 180, -22);
			context.runOnClient(client -> {
				FireworkShape[] shapes = FireworkShape.values();
				for (int i = 0; i < shapes.length; i++) {
					SpookyBursts.burst(client.level, new Vec3(x - 8.0 + i * 8.0 + 0.5, y + 11.0, z - 14.0), shapes[i], false);
				}
			});
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_spooky_fireworks");

			// A real finale: one rocket from every tube, bursting where each reaches.
			server.runOnServer(minecraft -> {
				if (minecraft.overworld().getBlockEntity(launcher) instanceof ShowLauncherBlockEntity show) {
					show.start();
				}
			});
			context.waitTicks(38);
			context.takeScreenshot("jugcraft_fireworks_finale");
		}
	}

	/** Stands the camera at (x, y, z) looking along yaw and pitch, and waits for the world to draw. */
	private static void place(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, int yaw, int pitch) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("setblock %d %d %d minecraft:barrier".formatted(x, y - 1, z));
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

	private static ItemStack rocket(FireworkShape shape) {
		ItemStack stack = new ItemStack(JugcraftAgriculture.item(shape.item()), 4);
		stack.set(DataComponents.FIREWORKS, new Fireworks(1, List.of()));
		return stack;
	}

	/** A Show Launcher set for a finale, its tubes loaded with every picture and a vanilla rocket in the middle. */
	private static void build(ServerLevel level, BlockPos pos) {
		level.setBlock(pos, JugcraftAgriculture.block("show_launcher").defaultBlockState().setValue(ShowLauncherBlock.FACING, Direction.SOUTH)
				.setValue(ShowLauncherBlock.MODE, ShowLauncherBlock.Mode.FINALE), Block.UPDATE_ALL);
		if (level.getBlockEntity(pos) instanceof ShowLauncherBlockEntity launcher) {
			FireworkShape[] shapes = FireworkShape.values();
			for (int tube = 0; tube < ShowLauncherBlockEntity.TUBES; tube++) {
				launcher.setItem(tube, tube == 4 ? new ItemStack(Items.FIREWORK_ROCKET, 4) : rocket(shapes[tube % shapes.length]));
			}
		}
	}
}
