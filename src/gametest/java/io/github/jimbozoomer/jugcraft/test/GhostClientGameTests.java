package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.AuraCandleBlock;
import io.github.jimbozoomer.jugcraft.agriculture.AuraCandleBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CandleMix;
import io.github.jimbozoomer.jugcraft.agriculture.CandleScent;
import io.github.jimbozoomer.jugcraft.agriculture.CandleWax;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.RestlessSpirit;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client game test for ghost hunting: by day, the hunter's kit (a Spirit Lantern and a bottle of Ectoplasm in item
 * frames, a lit Ghostly candle before them); at night, a small graveyard of gravestones and grave mounds lit by soul
 * lanterns, with restless spirits revealed over their graves, photographed from among the graves (CI job
 * {@code client}).
 */
public class GhostClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 10, y - 3, z - 16, x + 14, y - 1, z + 4));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 10, y, z - 16, x + 14, y + 12, z + 4));
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			for (int i = 0; i < 2; i++) {
				server.runCommand("summon minecraft:item_frame %d %d %d {Facing:3b,Fixed:1b,Item:{id:\"jugcraft:%s\",count:1}}"
						.formatted(x + 9 + 2 * i, y + 1, z - 6, i == 0 ? "spirit_lantern" : "ectoplasm"));
			}
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();
			shoot(context, singleplayer, x + 10, y + 1, z - 2, 180, 25, "jugcraft_ghost_hunting_kit");

			// At night, spirits revealed over the graves.
			server.runCommand("time set 14500");
			server.runOnServer(minecraft -> haunt(minecraft.overworld(), origin));
			context.waitTicks(30);
			shoot(context, singleplayer, x, y + 1, z - 1, 180, 12, "jugcraft_restless_spirits");
		}
	}

	private static void set(ServerLevel level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state, Block.UPDATE_ALL);
	}

	private static BlockState facingSouth(String id) {
		return JugcraftAgriculture.block(id).defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH);
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// The kit: a spruce wall for the frames, and a tall lit Ghostly candle before it.
		for (int dx = 8; dx <= 12; dx++) {
			for (int dy = 0; dy <= 2; dy++) {
				set(level, new BlockPos(x + dx, y + dy, z - 7), Blocks.SPRUCE_PLANKS.defaultBlockState());
			}
		}
		CandleMix mix = CandleMix.first(CandleWax.BEESWAX, CandleScent.GHOSTLY.color, List.of(CandleScent.GHOSTLY), false, false);
		for (int i = 1; i < 3; i++) {
			mix = mix.dip(CandleWax.BEESWAX, CandleScent.GHOSTLY.color, List.of(CandleScent.GHOSTLY), false, false);
		}
		BlockPos candle = new BlockPos(x + 10, y, z - 5);
		set(level, candle, JugcraftAgriculture.block("aura_candle").defaultBlockState().setValue(AuraCandleBlock.DIPS, mix.dips())
				.setValue(AuraCandleBlock.LIT, true));
		if (level.getBlockEntity(candle) instanceof AuraCandleBlockEntity lit) {
			lit.setMix(mix);
		}
		// The graveyard: a row of gravestones and grave mounds facing the camera, soul lanterns between them.
		String[] graves = {"rounded_gravestone", "grave_mound", "cross_gravestone", "grave_mound", "obelisk_gravestone"};
		for (int i = 0; i < graves.length; i++) {
			set(level, new BlockPos(x - 4 + 2 * i, y, z - 9), facingSouth(graves[i]));
		}
		set(level, new BlockPos(x - 3, y, z - 8), Blocks.SOUL_LANTERN.defaultBlockState());
		set(level, new BlockPos(x + 3, y, z - 8), Blocks.SOUL_LANTERN.defaultBlockState());
	}

	/** Restless spirits over the graves, revealed for the rest of the test and holding still (no AI). */
	private static void haunt(ServerLevel level, BlockPos origin) {
		float[][] spirits = {{-4.0F, 1.3F, -9.0F, 15.0F}, {0.0F, 1.7F, -9.0F, 0.0F}, {4.0F, 1.2F, -9.0F, -15.0F}, {-2.0F, 0.8F, -5.5F, 25.0F}};
		for (float[] at : spirits) {
			RestlessSpirit spirit = JugcraftAgriculture.RESTLESS_SPIRIT.create(level, EntitySpawnReason.COMMAND);
			if (spirit == null) {
				continue;
			}
			BlockPos grave = BlockPos.containing(origin.getX() + at[0], origin.getY(), origin.getZ() + at[2]);
			spirit.setHome(grave);
			spirit.snapTo(origin.getX() + at[0] + 0.5, origin.getY() + at[1], origin.getZ() + at[2] + 0.5, at[3], 0.0F);
			spirit.setYHeadRot(at[3]);
			spirit.setYBodyRot(at[3]);
			spirit.setNoAi(true);
			spirit.setPersistenceRequired();
			level.addFreshEntity(spirit);
			spirit.reveal(level.getGameTime() + 24000);
		}
	}

	/** Stands the camera at (x, y, z) looking along yaw and pitch, waits for the world to draw, and takes a screenshot. */
	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, int yaw, int pitch,
			String name) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("setblock %d %d %d minecraft:barrier".formatted(x, y - 1, z));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f %d %d", x + 0.5, y, z + 0.5, yaw, pitch));
		context.waitTicks(20);
		singleplayer.getConnection().waitForChunksRender();
		context.waitTicks(20);
		context.takeScreenshot(name);
	}
}
