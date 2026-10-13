package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.PlacedDishBlock;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client game test for the owner's milkshakes (tools/milkshakes.py): the seven set down in a row on a counter, facing the
 * camera, in the page's order; the strawberry and banana milkshakes close up from the angle the owner drew them at; and
 * the milkshakes' items (their 3D glasses) in item frames, two rows of four with the Milk Bottle they start from. CI job
 * {@code client}.
 */
public class MilkshakeClientGameTests implements FabricClientGameTest {
	private static final List<String> SHAKES = List.of("strawberry_milkshake", "banana_milkshake", "plum_milkshake", "apple_milkshake",
			"blueberry_milkshake", "pumpkin_milkshake", "chocolate_milkshake");

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			// Hide the HUD, hand and chat so the screenshots show only the glasses, whatever an earlier test in this client left.
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
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			// Within the fill command's limit of 32768 blocks.
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 6, y - 3, z - 8, x + 20, y - 1, z + 18));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 6, y, z - 8, x + 20, y + 10, z + 18));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			List<String> items = new java.util.ArrayList<>(SHAKES);
			items.add("milk_bottle");
			// Two rows of four, read from the camera's left as it faces them (north, so east is on its right).
			for (int i = 0; i < items.size(); i++) {
				server.runCommand("summon minecraft:item_frame %d %d %d {Facing:3b,Fixed:1b,Item:{id:\"jugcraft:%s\",count:1}}"
						.formatted(x + 1 + i % 4, y + 3 - i / 4, z + 11, items.get(i)));
			}
			context.waitTicks(10);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 3, y + 2, z + 1, 0, 25, "jugcraft_milkshakes");
			shoot(context, singleplayer, x + 10, y + 1, z + 2, -45, 32, "jugcraft_milkshakes_drawn");
			shoot(context, singleplayer, x + 2, y + 1, z + 13, 180, -9, "jugcraft_milkshake_items");
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
		context.waitTicks(10);
		context.takeScreenshot(name);
	}

	private static void set(ServerLevel level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state, Block.UPDATE_ALL);
	}

	/** A milkshake set down facing north, as for a player standing to its north. */
	private static BlockState shake(String id) {
		return JugcraftAgriculture.block(id).defaultBlockState().setValue(PlacedDishBlock.FACING, Direction.NORTH);
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// The seven on a counter, facing north towards the camera, in the page's order from its left (it faces south, so west
		// is on its right).
		for (int i = 0; i < SHAKES.size(); i++) {
			int at = x + SHAKES.size() - 1 - i;
			set(level, new BlockPos(at, y, z + 4), Blocks.SMOOTH_QUARTZ.defaultBlockState());
			set(level, new BlockPos(at, y + 1, z + 4), shake(SHAKES.get(i)));
		}
		// The strawberry and banana milkshakes, seen from above their front right as the owner drew them (they face north,
		// so from the north-west), side by side across the view.
		set(level, new BlockPos(x + 12, y, z + 3), Blocks.SMOOTH_QUARTZ.defaultBlockState());
		set(level, new BlockPos(x + 12, y + 1, z + 3), shake("strawberry_milkshake"));
		set(level, new BlockPos(x + 11, y, z + 4), Blocks.SMOOTH_QUARTZ.defaultBlockState());
		set(level, new BlockPos(x + 11, y + 1, z + 4), shake("banana_milkshake"));
		// The wall the items hang on.
		for (int dx = 0; dx <= 9; dx++) {
			for (int dy = 0; dy <= 4; dy++) {
				set(level, new BlockPos(x + dx, y + dy, z + 10), Blocks.SPRUCE_PLANKS.defaultBlockState());
			}
		}
	}
}
