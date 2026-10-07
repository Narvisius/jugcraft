package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.OfrendaBlock;
import io.github.jimbozoomer.jugcraft.agriculture.OfrendaBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.RestlessSpirit;
import io.github.jimbozoomer.jugcraft.agriculture.SugarSkullBlock;
import io.github.jimbozoomer.jugcraft.agriculture.WallDecorationBlock;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;

/**
 * Client game test for the Día de Muertos ofrenda: an ofrenda against an adobe-coloured wall hung with papel picado, its
 * tiers set with marigolds, candles, pan de muerto, sugar skulls, a honey bottle and a painting, marigolds in pots and lit
 * candles either side and a path of marigold petals leading to it; by day, up close, and at night with a restless spirit
 * welcomed among the offerings. CI job {@code client}.
 */
public class OfrendaClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 6, y - 3, z - 14, x + 14, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 6, y, z - 14, x + 14, y + 10, z + 8));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 3, y + 1, z + 1, 180, 15, "jugcraft_ofrenda");
			shoot(context, singleplayer, x + 3, y + 1, z - 3, 180, 25, "jugcraft_ofrenda_close");
			server.runCommand("time set midnight");
			context.waitTicks(10);
			shoot(context, singleplayer, x + 3, y + 1, z + 1, 180, 15, "jugcraft_ofrenda_night");
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

	private static ItemStack vanilla(String id) {
		return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(id)));
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// An adobe-coloured wall behind, hung with papel picado.
		for (int dx = -1; dx <= 7; dx++) {
			for (int dy = 0; dy <= 4; dy++) {
				level.setBlock(new BlockPos(x + dx, y + dy, z - 8), Blocks.TERRACOTTA.defaultBlockState(), Block.UPDATE_ALL);
			}
			level.setBlock(new BlockPos(x + dx, y + 3, z - 7), JugcraftAgriculture.block("papel_picado").defaultBlockState()
					.setValue(WallDecorationBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
		}
		// The ofrenda, set with all five kinds and a painting.
		BlockPos altar = new BlockPos(x + 3, y, z - 7);
		level.setBlock(altar, JugcraftAgriculture.block("ofrenda").defaultBlockState().setValue(OfrendaBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
		if (level.getBlockEntity(altar) instanceof OfrendaBlockEntity ofrenda) {
			for (ItemStack offering : List.of(new ItemStack(JugcraftAgriculture.item("sugar_skull")), vanilla("painting"),
					new ItemStack(JugcraftAgriculture.item("pan_de_muerto")), new ItemStack(JugcraftAgriculture.item("marigold")), vanilla("candle"),
					vanilla("honey_bottle"))) {
				ofrenda.offer(offering);
			}
		}
		// Marigolds in pots and lit candles either side; sugar skulls on the ground before it.
		for (int side : new int[] {-1, 1}) {
			level.setBlock(altar.offset(side, 0, 0), JugcraftAgriculture.block("potted_marigold").defaultBlockState(), Block.UPDATE_ALL);
			level.setBlock(altar.offset(2 * side, 0, 0), Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3)
					.setValue(CandleBlock.LIT, true), Block.UPDATE_ALL);
			level.setBlock(altar.offset(side, 0, 1), JugcraftAgriculture.block("sugar_skull").defaultBlockState()
					.setValue(SugarSkullBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
		}
		// A path of marigold petals leading to it.
		for (int dz = 1; dz <= 5; dz++) {
			level.setBlock(altar.offset(0, 0, dz), JugcraftAgriculture.block("marigold_petals").defaultBlockState(), Block.UPDATE_ALL);
		}
		// A spirit, welcomed among the offerings (posed, no AI); it shows only when it is dark enough to see.
		RestlessSpirit spirit = JugcraftAgriculture.RESTLESS_SPIRIT.create(level, EntitySpawnReason.COMMAND);
		if (spirit != null) {
			spirit.setNoAi(true);
			spirit.setPersistenceRequired();
			spirit.snapTo(altar.getX() + 0.5, altar.getY() + 1.4, altar.getZ() + 0.3, 0.0F, 0.0F);
			level.addFreshEntity(spirit);
			spirit.welcome(level, altar);
			spirit.reveal(level.getGameTime() + 100000L);
		}
	}
}
