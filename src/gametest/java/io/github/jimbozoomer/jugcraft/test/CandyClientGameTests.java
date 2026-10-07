package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.CandyBase;
import io.github.jimbozoomer.jugcraft.agriculture.CandyFlavour;
import io.github.jimbozoomer.jugcraft.agriculture.CandyKettleBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CandyKettleBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;

/**
 * Client game test for the candy kitchen: four Candy Kettles on campfires, their thermometers facing the camera (glow
 * berry syrup at soft ball, chocolate cream at fudge, pink syrup at hard ball, and a batch burnt black, still on the
 * fire), and a wall of framed candy: a tray of fudge, rock candy, taffy, a lollipop, hard candy, fudge, a cream caramel,
 * toffee, classic and dyed candy corn, caramel and burnt sugar. CI job {@code client}.
 */
public class CandyClientGameTests implements FabricClientGameTest {
	/** The framed candies, as {@code id} and its components (SNBT), left to right and top to bottom. */
	private static final String[][] CANDIES = {
			{"candy_tray", "\"jugcraft:candy_batch\":{kind:\"fudge\",flavours:[\"chocolate\"],colors:[-1],pieces:8,poured:0L},\"minecraft:dyed_color\":5911064"},
			{"rock_candy", "\"minecraft:dyed_color\":10133247"},
			{"salt_water_taffy", "\"minecraft:dyed_color\":15769792"},
			{"lollipop", "\"minecraft:dyed_color\":5928191"},
			{"hard_candy", "\"minecraft:dyed_color\":13117500"},
			{"fudge", "\"minecraft:dyed_color\":5911064"},
			{"cream_caramel", ""},
			{"toffee", ""},
			{"candy_corn", ""},
			{"candy_corn", "\"minecraft:custom_model_data\":{colors:[3194928,8396960,1052688]}"},
			{"caramel", ""},
			{"burnt_sugar", ""}};

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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 6, y - 3, z - 14, x + 14, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 6, y, z - 14, x + 14, y + 10, z + 8));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			for (int i = 0; i < CANDIES.length; i++) {
				String components = CANDIES[i][1].isEmpty() ? "" : ",components:{" + CANDIES[i][1] + "}";
				server.runCommand("summon minecraft:item_frame %d %d %d {Facing:3b,Fixed:1b,Item:{id:\"jugcraft:%s\",count:1%s}}"
						.formatted(x + 1 + i % 6, y + 2 - i / 6, z - 9, CANDIES[i][0], components));
			}
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 4, y + 2, z + 2, 180, 25, "jugcraft_candy_kitchen");
			shoot(context, singleplayer, x + 4, y + 1, z - 2, 180, 30, "jugcraft_candy_kettles");
			shoot(context, singleplayer, x + 4, y + 1, z - 5, 180, 10, "jugcraft_candies");
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

	/** A kettle on a campfire (lit or not) at {@code pos}, facing the camera, holding a batch cooked to {@code degrees}. */
	private static void kettle(ServerLevel level, BlockPos pos, boolean lit, CandyBase base, int sugar, CandyFlavour flavour, DyeColor dye,
			int degrees) {
		level.setBlock(pos.below(), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, lit), Block.UPDATE_ALL);
		level.setBlock(pos, JugcraftAgriculture.block("candy_kettle").defaultBlockState().setValue(CandyKettleBlock.FACING, Direction.SOUTH),
				Block.UPDATE_ALL);
		if (level.getBlockEntity(pos) instanceof CandyKettleBlockEntity kettle) {
			kettle.setBase(base);
			for (int i = 0; i < sugar; i++) {
				kettle.addSugar();
			}
			if (flavour != null) {
				kettle.addFlavour(flavour);
			}
			if (dye != null) {
				kettle.addDye(dye);
			}
			kettle.setTemperature(degrees);
		}
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// The kettles, on a brick hearth; the burnt one still on a lit fire.
		for (int dx = 0; dx <= 8; dx++) {
			level.setBlock(new BlockPos(x + dx, y - 1, z - 5), Blocks.BRICKS.defaultBlockState(), Block.UPDATE_ALL);
		}
		kettle(level, new BlockPos(x + 1, y + 1, z - 5), false, CandyBase.SYRUP, 3, CandyFlavour.GLOW_BERRY, null, 117);
		kettle(level, new BlockPos(x + 3, y + 1, z - 5), false, CandyBase.CREAM, 4, CandyFlavour.CHOCOLATE, null, 118);
		kettle(level, new BlockPos(x + 5, y + 1, z - 5), false, CandyBase.SYRUP, 2, null, DyeColor.PINK, 128);
		kettle(level, new BlockPos(x + 7, y + 1, z - 5), true, CandyBase.SYRUP, 2, null, null, 182);
		// The wall the candy hangs on.
		for (int dx = 0; dx <= 7; dx++) {
			for (int dy = 0; dy <= 3; dy++) {
				level.setBlock(new BlockPos(x + dx, y + dy, z - 10), Blocks.SPRUCE_PLANKS.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
	}
}
