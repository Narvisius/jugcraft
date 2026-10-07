package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.RestlessSpirit;
import io.github.jimbozoomer.jugcraft.agriculture.SpiritBoard;
import io.github.jimbozoomer.jugcraft.agriculture.SpiritBoardBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SpiritBoardBlockEntity;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;

/**
 * Client game test for the Spirit Board: four boards on a table, one facing each way, the planchette on YES, on NO, on
 * GOODBYE and on M (each must read the right way up from the side it faces); a close look at one board from above, its
 * planchette on a letter; and a séance at night: a board between lit candles, a revealed restless spirit drifting over
 * it. CI job {@code client}.
 */
public class SpiritBoardClientGameTests implements FabricClientGameTest {
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
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 4, y + 1, z - 2, 180, 42, "jugcraft_spirit_boards");
			shoot(context, singleplayer, x + 1, y + 1, z - 4, 180, 68, "jugcraft_spirit_board");
			server.runCommand("time set midnight");
			context.waitTicks(10);
			shoot(context, singleplayer, x + 11, y + 1, z - 2, 180, 25, "jugcraft_seance");
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

	/** A board on the table at {@code pos} facing {@code facing}, its planchette resting on {@code stop}. */
	private static void board(ServerLevel level, BlockPos pos, Direction facing, char stop) {
		level.setBlock(pos.below(), Blocks.DARK_OAK_PLANKS.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(pos, JugcraftAgriculture.block("spirit_board").defaultBlockState().setValue(SpiritBoardBlock.FACING, facing),
				Block.UPDATE_ALL);
		if (level.getBlockEntity(pos) instanceof SpiritBoardBlockEntity board) {
			board.setPlanchette(stop, stop, 0L, 1);
		}
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// Four boards in a row, each facing a different way (the camera looks north at them from the south).
		board(level, new BlockPos(x + 1, y, z - 5), Direction.SOUTH, 'M');
		board(level, new BlockPos(x + 3, y, z - 5), Direction.EAST, SpiritBoard.YES);
		board(level, new BlockPos(x + 5, y, z - 5), Direction.NORTH, SpiritBoard.NO);
		board(level, new BlockPos(x + 7, y, z - 5), Direction.WEST, SpiritBoard.GOODBYE);
		// The séance: a board between four lit candles, a spirit over it.
		BlockPos seance = new BlockPos(x + 11, y, z - 5);
		board(level, seance, Direction.SOUTH, 'S');
		for (int[] at : new int[][] {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
			BlockPos stand = seance.offset(at[0], -1, at[1]);
			level.setBlock(stand, Blocks.DARK_OAK_PLANKS.defaultBlockState(), Block.UPDATE_ALL);
			level.setBlock(stand.above(), Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true),
					Block.UPDATE_ALL);
		}
		RestlessSpirit spirit = JugcraftAgriculture.RESTLESS_SPIRIT.create(level, EntitySpawnReason.COMMAND);
		if (spirit != null) {
			spirit.setNoAi(true);
			spirit.setPersistenceRequired();
			spirit.setHome(seance.below());
			spirit.snapTo(seance.getX() + 0.5, seance.getY() + 1.2, seance.getZ() - 1.5, 0.0F, 0.0F);
			level.addFreshEntity(spirit);
			spirit.reveal(level.getGameTime() + 100000L);
		}
	}
}
