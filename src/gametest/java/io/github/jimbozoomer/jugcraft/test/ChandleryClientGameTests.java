package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.AuraCandleBlock;
import io.github.jimbozoomer.jugcraft.agriculture.AuraCandleBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CandleMix;
import io.github.jimbozoomer.jugcraft.agriculture.CandleScent;
import io.github.jimbozoomer.jugcraft.agriculture.CandleWax;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.WaxPotBlockEntity;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client game test for the chandlery: a Wax Melting Pot of molten purple wax over a campfire beside a cold pot of set
 * tallow, and a table of lit Aura Candles of every size, in several colours and scents (one muddled, one unlit).
 * Photographed by day and at night (CI job {@code client}).
 */
public class ChandleryClientGameTests implements FabricClientGameTest {
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
			context.waitTicks(60);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 4, y + 2, z + 1, 180, 25, "jugcraft_chandlery");
			shoot(context, singleplayer, x + 5, y + 2, z - 3, 180, 35, "jugcraft_aura_candles");
			shoot(context, singleplayer, x + 1, y + 2, z - 3, 180, 50, "jugcraft_wax_melting_pot");
			server.runCommand("time set midnight");
			shoot(context, singleplayer, x + 5, y + 2, z - 3, 180, 35, "jugcraft_aura_candles_night");
			shoot(context, singleplayer, x + 4, y + 3, z + 3, 180, 30, "jugcraft_chandlery_night");
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

	private static void set(ServerLevel level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state, Block.UPDATE_ALL);
	}

	private static void candle(ServerLevel level, BlockPos pos, CandleMix mix, boolean lit) {
		set(level, pos, JugcraftAgriculture.block("aura_candle").defaultBlockState().setValue(AuraCandleBlock.DIPS, mix.dips())
				.setValue(AuraCandleBlock.LIT, lit));
		if (level.getBlockEntity(pos) instanceof AuraCandleBlockEntity candle) {
			candle.setMix(mix);
		}
	}

	/** A candle of {@code dips} layers of {@code wax} in {@code color}, carrying {@code scents}. */
	private static CandleMix mix(CandleWax wax, int dips, int color, CandleScent... scents) {
		CandleMix mix = CandleMix.first(wax, color, List.of(scents), false, false);
		for (int i = 1; i < dips; i++) {
			mix = mix.dip(wax, color, List.of(scents), false, false);
		}
		return mix;
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// The workshop: a pot of molten purple beeswax over a campfire, and a cold pot of set tallow beside it.
		BlockPos hot = new BlockPos(x + 1, y + 1, z - 6);
		set(level, hot.below(), Blocks.CAMPFIRE.defaultBlockState());
		set(level, hot, JugcraftAgriculture.block("wax_melting_pot").defaultBlockState());
		if (level.getBlockEntity(hot) instanceof WaxPotBlockEntity pot) {
			for (int i = 0; i < 3; i++) {
				pot.addWax(CandleWax.BEESWAX);
			}
			pot.meltAll();
			pot.addDye(DyeColor.PURPLE);
			pot.addScent(CandleScent.WARDING);
			pot.addScent(CandleScent.MOONLIGHT);
		}
		BlockPos cold = new BlockPos(x - 1, y, z - 6);
		set(level, cold, JugcraftAgriculture.block("wax_melting_pot").defaultBlockState());
		if (level.getBlockEntity(cold) instanceof WaxPotBlockEntity pot) {
			for (int i = 0; i < 5; i++) {
				pot.addWax(CandleWax.TALLOW);
			}
		}
		// The table of candles.
		for (int dx = 3; dx <= 9; dx++) {
			set(level, new BlockPos(x + dx, y, z - 6), Blocks.SPRUCE_PLANKS.defaultBlockState());
		}
		int top = y + 1;
		candle(level, new BlockPos(x + 3, top, z - 6), mix(CandleWax.TALLOW, 1, 0xEEE6D2), true);
		candle(level, new BlockPos(x + 4, top, z - 6), mix(CandleWax.BEESWAX, 2, 0xFF6A1A, CandleScent.EMBER), true);
		candle(level, new BlockPos(x + 5, top, z - 6), mix(CandleWax.BEESWAX, 3, 0x7A3FCF, CandleScent.WARDING, CandleScent.MOONLIGHT), true);
		candle(level, new BlockPos(x + 6, top, z - 6), mix(CandleWax.BEESWAX, 4, 0x3FA9C8, CandleScent.TIDE, CandleScent.SWIFTNESS), true);
		candle(level, new BlockPos(x + 7, top, z - 6), mix(CandleWax.TALLOW, 3, 0x5FBF3A, CandleScent.HARVEST), true);
		candle(level, new BlockPos(x + 8, top, z - 6), mix(CandleWax.BEESWAX, 2, 0xF27ACB, CandleScent.MENDING, CandleScent.LEAPING,
				CandleScent.DILIGENCE), true);
		candle(level, new BlockPos(x + 9, top, z - 6), mix(CandleWax.BEESWAX, 4, 0xE8B84A, CandleScent.DILIGENCE), false);
	}
}
