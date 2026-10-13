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
 * Client game test for the owner's square pies and tarts (tools/pies_and_tarts.py): two bakery displays, the five pies and
 * the five tarts, whole below and with a quarter cut away above; the strawberry pie and the blueberry tart whole and cut
 * seen from the angle the owner drew them at; three Hearth Ovens baking (raw, baked, burnt); and a wall of their items in
 * item frames (raw, baked, sliced). CI job {@code client}.
 */
public class PieTartClientGameTests implements FabricClientGameTest {
	/** The pies and tarts: the fillings set down at a height of their own that are not cakes. */
	private static final List<PieFilling> BAKES = Arrays.stream(PieFilling.values()).filter(f -> f.height > 0 && !f.cake).toList();
	/** Items to a row of the wall: a row each of the raw bakes, the bakes and the slices. */
	private static final int WALL = 10;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			// Hide the HUD, hand and chat so the screenshots show only the bakes, whatever an earlier test in this client left.
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 6, y - 3, z - 8, x + 26, y - 1, z + 24));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 6, y, z - 8, x + 26, y + 10, z + 24));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			List<String> items = new ArrayList<>();
			for (PieFilling bake : BAKES) {
				items.add(bake.rawPie());
			}
			for (PieFilling bake : BAKES) {
				items.add(bake.pie());
			}
			for (PieFilling bake : BAKES) {
				items.add(bake.slice());
			}
			for (int i = 0; i < items.size(); i++) {
				server.runCommand("summon minecraft:item_frame %d %d %d {Facing:3b,Fixed:1b,Item:{id:\"jugcraft:%s\",count:1}}"
						.formatted(x + 1 + i % WALL, y + 3 - i / WALL, z + 19, items.get(i)));
			}
			context.waitTicks(10);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 4, y + 3, z + 1, 0, 32, "jugcraft_pies");
			shoot(context, singleplayer, x + 16, y + 3, z + 1, 0, 32, "jugcraft_tarts");
			shoot(context, singleplayer, x + 5, y + 2, z + 10, -45, 34, "jugcraft_pies_and_tarts_drawn");
			// From a dip in the ground, eye level with the ovens' mouths, to see the bakes inside.
			shoot(context, singleplayer, x + 16, y - 1, z + 10, 0, 8, "jugcraft_pie_tart_ovens");
			shoot(context, singleplayer, x + 5, y + 2, z + 24, 180, 5, "jugcraft_pie_tart_items");
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

	private static BlockState bake(String id, int bites) {
		return block(id).defaultBlockState().setValue(CakeBlock.FACING, Direction.NORTH).setValue(PieBlock.BITES, bites);
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// Two bakery displays facing north towards the camera, the pies' and the tarts': the whole bakes on the lower shelf,
		// the cut ones a step up behind them, in the page's order from the camera's left (west is on its right).
		List<PieFilling> pies = BAKES.stream().filter(b -> !b.pie().endsWith("_tart")).toList();
		List<PieFilling> tarts = BAKES.stream().filter(b -> b.pie().endsWith("_tart")).toList();
		for (int row = 0; row < 2; row++) {
			List<PieFilling> shown = row == 0 ? pies : tarts;
			for (int i = 0; i < shown.size(); i++) {
				int at = x + 12 * row + 2 * (shown.size() - 1 - i);
				set(level, new BlockPos(at, y, z + 5), Blocks.STRIPPED_OAK_WOOD.defaultBlockState());
				set(level, new BlockPos(at, y + 1, z + 5), bake(shown.get(i).pie(), 0));
				set(level, new BlockPos(at, y, z + 7), Blocks.STRIPPED_OAK_WOOD.defaultBlockState());
				set(level, new BlockPos(at, y + 1, z + 7), Blocks.STRIPPED_OAK_WOOD.defaultBlockState());
				set(level, new BlockPos(at, y + 2, z + 7), bake(shown.get(i).pie(), 1));
			}
		}
		// The two the owner drew large, whole and cut, side by side across the view the owner drew them from (from above
		// their front right): the strawberry pie whole, cut, then the blueberry tart whole, cut.
		String[][] drawn = {{"strawberry_pie", "0"}, {"strawberry_pie", "1"}, {"blueberry_tart", "0"}, {"blueberry_tart", "1"}};
		for (int i = 0; i < drawn.length; i++) {
			BlockPos pos = new BlockPos(x + 9 - i, y, z + 11 + i);
			set(level, pos, Blocks.STRIPPED_OAK_WOOD.defaultBlockState());
			set(level, pos.above(), bake(drawn[i][0], Integer.parseInt(drawn[i][1])));
		}
		// Three Hearth Ovens facing the camera, baking (from its left) a whipped pumpkin pie still raw, a blueberry tart baked
		// and a plum pie burnt, and the dip the camera stands in to look into them.
		set(level, new BlockPos(x + 16, y - 1, z + 10), Blocks.AIR.defaultBlockState());
		Object[][] ovens = {{PieFilling.WHIPPED_PUMPKIN_PIE, 0}, {PieFilling.BLUEBERRY_TART, HearthOvenBlockEntity.BAKED},
				{PieFilling.PLUM_PIE, HearthOvenBlockEntity.BURNT}};
		for (int i = 0; i < ovens.length; i++) {
			BlockPos pos = new BlockPos(x + 18 - 2 * i, y, z + 13);
			set(level, pos, block("hearth_oven").defaultBlockState().setValue(HearthOvenBlock.FACING, Direction.NORTH));
			if (level.getBlockEntity(pos) instanceof HearthOvenBlockEntity oven) {
				oven.set(0, 0, (PieFilling) ovens[i][0], (Integer) ovens[i][1]);
			}
		}
		// The wall the items hang on.
		for (int dx = 0; dx <= WALL + 1; dx++) {
			for (int dy = 0; dy <= 4; dy++) {
				set(level, new BlockPos(x + dx, y + dy, z + 18), Blocks.SPRUCE_PLANKS.defaultBlockState());
			}
		}
	}
}
