package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.CakeBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HearthOvenBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HearthOvenBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.PieBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PieFilling;
import java.util.ArrayList;
import java.util.Arrays;
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
 * Client game test for the cakes the owner drew (tools/cakes.py): a two-tier bakery display of the seven cakes and the
 * Burnt Cake, whole below and with a quarter cut away above; the carrot cake whole and cut seen from the angle the owner
 * drew it at; three Hearth Ovens baking a cake (raw, baked, burnt); and a wall of the cakes' items in item frames. CI job
 * {@code client}.
 */
public class CakeClientGameTests implements FabricClientGameTest {
	private static final List<PieFilling> CAKES = Arrays.stream(PieFilling.values()).filter(f -> f.cake).toList();
	/** Items to a row of the wall. */
	private static final int WALL = 8;

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
			// Within the fill command's limit of 32768 blocks.
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 6, y - 3, z - 8, x + 26, y - 1, z + 24));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 6, y, z - 8, x + 26, y + 10, z + 24));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			List<String> items = new ArrayList<>(List.of("cake_batter"));
			for (PieFilling cake : CAKES) {
				items.add(cake.rawPie());
			}
			for (PieFilling cake : CAKES) {
				items.add(cake.slice());
			}
			items.add("burnt_cake");
			for (int i = 0; i < items.size(); i++) {
				server.runCommand("summon minecraft:item_frame %d %d %d {Facing:3b,Fixed:1b,Item:{id:\"jugcraft:%s\",count:1}}"
						.formatted(x + 1 + i % WALL, y + 3 - i / WALL, z + 19, items.get(i)));
			}
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 7, y + 3, z - 2, 0, 22, "jugcraft_cakes");
			shoot(context, singleplayer, x + 16, y + 3, z + 1, -45, 31, "jugcraft_cakes_drawn");
			shoot(context, singleplayer, x + 22, y + 1, z + 9, 0, 12, "jugcraft_cake_ovens");
			shoot(context, singleplayer, x + 4, y + 2, z + 23, 180, 5, "jugcraft_cake_items");
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

	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	private static void set(ServerLevel level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state, Block.UPDATE_ALL);
	}

	private static BlockState cake(String id, int bites) {
		return block(id).defaultBlockState().setValue(CakeBlock.FACING, Direction.NORTH).setValue(PieBlock.BITES, bites);
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// The bakery display, facing north towards the camera: the whole cakes on the lower shelf, the cut ones a step up
		// behind them.
		List<String> shown = new ArrayList<>();
		for (PieFilling cake : CAKES) {
			shown.add(cake.pie());
		}
		shown.add("burnt_cake");
		for (int i = 0; i < shown.size(); i++) {
			int at = x + 2 * i;
			set(level, new BlockPos(at, y, z + 5), Blocks.STRIPPED_OAK_WOOD.defaultBlockState());
			set(level, new BlockPos(at, y + 1, z + 5), cake(shown.get(i), 0));
			set(level, new BlockPos(at, y, z + 7), Blocks.STRIPPED_OAK_WOOD.defaultBlockState());
			set(level, new BlockPos(at, y + 1, z + 7), Blocks.STRIPPED_OAK_WOOD.defaultBlockState());
			set(level, new BlockPos(at, y + 2, z + 7), cake(shown.get(i), 1));
		}
		// The carrot cake whole and cut, side by side across the view the owner drew it from (from above its front right).
		set(level, new BlockPos(x + 21, y, z + 4), Blocks.STRIPPED_OAK_WOOD.defaultBlockState());
		set(level, new BlockPos(x + 21, y + 1, z + 4), cake("carrot_cake", 0));
		set(level, new BlockPos(x + 19, y, z + 6), Blocks.STRIPPED_OAK_WOOD.defaultBlockState());
		set(level, new BlockPos(x + 19, y + 1, z + 6), cake("carrot_cake", 1));
		// Three Hearth Ovens facing the camera, baking a red velvet cake still raw, an apple cake baked and a birthday cake burnt.
		Object[][] ovens = {{PieFilling.RED_VELVET_CAKE, 0}, {PieFilling.APPLE_CAKE, HearthOvenBlockEntity.BAKED},
				{PieFilling.BIRTHDAY_CAKE, HearthOvenBlockEntity.BURNT}};
		for (int i = 0; i < ovens.length; i++) {
			BlockPos pos = new BlockPos(x + 20 + 2 * i, y, z + 13);
			set(level, pos, block("hearth_oven").defaultBlockState().setValue(HearthOvenBlock.FACING, Direction.NORTH));
			if (level.getBlockEntity(pos) instanceof HearthOvenBlockEntity oven) {
				oven.set(0, 0, (PieFilling) ovens[i][0], (Integer) ovens[i][1]);
			}
		}
		// The wall the items hang on.
		for (int dx = 0; dx <= 9; dx++) {
			for (int dy = 0; dy <= 4; dy++) {
				set(level, new BlockPos(x + dx, y + dy, z + 18), Blocks.SPRUCE_PLANKS.defaultBlockState());
			}
		}
	}
}
