package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.CarvedPumpkinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CarvedPumpkinBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CarvingTemplates;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.Pumpkling;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCarving;
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
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client game test for the Pumpkling (fall addition 25): in a pumpkin patch, three Pumpklings wearing three stencils'
 * faces (one lit, one lit by a soul torch, one unlit and sitting) beside a carved pumpkin not yet woken and a Wisp in a
 * Jar; up close; and at nightfall, the lit faces glowing. CI job {@code client}.
 */
public class PumpklingClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			// Hide the HUD, hand and chat whatever an earlier test in this client left: CI shares the client tests out
			// between parallel jobs, and only the first job's opening test hides them.
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
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("gamerule minecraft:spawn_mobs false");
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 8, y - 3, z - 14, x + 12, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 14, x + 12, y + 10, z + 8));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 3, y, z + 1, 180, 22, "jugcraft_pumpklings");
			shoot(context, singleplayer, x + 3, y, z - 1, 180, 30, "jugcraft_pumpkling_close");
			server.runCommand("time set 13800");
			context.waitTicks(10);
			shoot(context, singleplayer, x + 3, y + 1, z + 2, 180, 25, "jugcraft_pumpklings_nightfall");
			server.runCommand("kill @e[type=jugcraft:pumpkling]");
			server.runCommand("time set noon");
		}
	}

	/** Stands the camera at (x, y, z) looking along yaw and pitch, and waits for the world to draw. */
	private static void place(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, int yaw, int pitch) {
		TestServerContext server = singleplayer.getServer();
		// Only into air, so standing on the ground leaves no hole in it for the later pictures.
		server.runCommand("fill %d %d %d %d %d %d minecraft:barrier replace minecraft:air".formatted(x, y - 1, z, x, y - 1, z));
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

	private static PumpkinCarving face(int stencil) {
		return PumpkinCarving.BLANK.withFace(0, CarvingTemplates.ALL.get(stencil % CarvingTemplates.ALL.size()).face());
	}

	/** A Pumpkling standing still at (x, y, z) facing {@code yaw}, wearing a stencil's face, lit (soul or not) or sitting. */
	private static void pumpkling(ServerLevel level, double x, int y, double z, float yaw, int stencil, boolean lit, boolean soul, boolean sitting) {
		Pumpkling pumpkling = JugcraftAgriculture.PUMPKLING.create(level, EntitySpawnReason.COMMAND);
		if (pumpkling == null) {
			return;
		}
		pumpkling.setNoAi(true);
		BlockState state = JugcraftAgriculture.block("hand_carved_pumpkin").defaultBlockState().setValue(CarvedPumpkinBlock.LIT, lit)
				.setValue(CarvedPumpkinBlock.SOUL, soul);
		pumpkling.setHead(Pumpkling.headOf(state, face(stencil)));
		pumpkling.setSitting(sitting);
		pumpkling.snapTo(x, y, z, yaw, 0.0F);
		pumpkling.setYHeadRot(yaw);
		pumpkling.setYBodyRot(yaw);
		level.addFreshEntity(pumpkling);
	}

	/**
	 * A pumpkin patch: pumpkins on the vine among the grass, a carved pumpkin not yet woken on a hay bale beside a Wisp in
	 * a Jar; three Pumpklings facing the camera (south, yaw 0).
	 */
	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		for (int[] at : new int[][] {{-1, -6}, {1, -8}, {6, -7}, {8, -5}, {4, -10}, {-2, -9}, {7, -10}}) {
			level.setBlock(new BlockPos(x + at[0], y, z + at[1]), Blocks.PUMPKIN.defaultBlockState(), Block.UPDATE_ALL);
		}
		for (int[] at : new int[][] {{0, -5}, {2, -7}, {5, -6}, {7, -8}, {3, -9}}) {
			level.setBlock(new BlockPos(x + at[0], y, z + at[1]), Blocks.SHORT_GRASS.defaultBlockState(), Block.UPDATE_ALL);
		}
		level.setBlock(new BlockPos(x + 6, y, z - 4), Blocks.HAY_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
		BlockPos carved = new BlockPos(x + 6, y + 1, z - 4);
		PumpkinCarving face = face(3);
		level.setBlock(carved, JugcraftAgriculture.block("hand_carved_pumpkin").defaultBlockState().setValue(CarvedPumpkinBlock.FACING, Direction.SOUTH)
				.setValue(CarvedPumpkinBlock.GLOW, face.glow()), Block.UPDATE_ALL);
		if (level.getBlockEntity(carved) instanceof CarvedPumpkinBlockEntity pumpkin) {
			pumpkin.setCarving(face, null);
		}
		level.setBlock(new BlockPos(x + 5, y, z - 4), JugcraftAgriculture.block("wisp_in_a_jar").defaultBlockState(), Block.UPDATE_ALL);
		pumpkling(level, x + 2.5, y, z - 3.0, 10.0F, 0, true, false, false);
		pumpkling(level, x + 3.9, y, z - 3.6, -15.0F, 1, true, true, false);
		pumpkling(level, x + 1.2, y, z - 4.2, 25.0F, 2, false, false, true);
	}
}
