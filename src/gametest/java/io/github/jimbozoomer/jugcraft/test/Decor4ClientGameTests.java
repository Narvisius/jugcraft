package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.ApothecaryShelfBlock;
import io.github.jimbozoomer.jugcraft.agriculture.BubblingCauldronBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CrystalBallBlock;
import io.github.jimbozoomer.jugcraft.agriculture.GrimoireStandBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.WitchsBroomBlock;
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
 * Client game test for the witch's cottage: a back wall hung with four Apothecary Shelves (each set out differently),
 * Bubbling Cauldrons (a green brew over a campfire, a purple one, water and empty), a Crystal Ball on a stump (one
 * resting, one being gazed into), a Grimoire Stand and a Witch's Broom; photographed by day and at night (CI job
 * {@code client}).
 */
public class Decor4ClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 8, y - 3, z - 20, x + 24, y - 1, z + 10));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 20, x + 24, y + 12, z + 10));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 6, y + 3, z + 2, 180, 20, "jugcraft_witchs_cottage");
			shoot(context, singleplayer, x + 6, y + 3, z - 3, 180, 8, "jugcraft_apothecary_shelves");
			shoot(context, singleplayer, x + 3, y + 3, z - 1, 180, 35, "jugcraft_bubbling_cauldrons");
			shoot(context, singleplayer, x + 9, y + 2, z - 2, 180, 25, "jugcraft_crystal_balls_and_grimoire");
			server.runCommand("time set midnight");
			shoot(context, singleplayer, x + 6, y + 3, z + 2, 180, 20, "jugcraft_witchs_cottage_night");
			shoot(context, singleplayer, x + 3, y + 3, z - 1, 180, 35, "jugcraft_bubbling_cauldrons_night");
		}
	}

	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, int yaw, int pitch,
			String name) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("setblock %d %d %d minecraft:barrier".formatted(x, y - 1, z));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f %d %d", x + 0.5, y, z + 0.5, yaw, pitch));
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
	}

	private static void set(ServerLevel level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state, Block.UPDATE_ALL);
	}

	private static BlockState state(String id) {
		return JugcraftAgriculture.block(id).defaultBlockState();
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// A plank floor and a dark back wall.
		for (int dx = 0; dx <= 12; dx++) {
			for (int dz = -8; dz <= -3; dz++) {
				set(level, new BlockPos(x + dx, y - 1, z + dz), Blocks.SPRUCE_PLANKS.defaultBlockState());
			}
			for (int dy = 0; dy <= 5; dy++) {
				set(level, new BlockPos(x + dx, y + dy, z - 9), Blocks.DARK_OAK_PLANKS.defaultBlockState());
			}
		}
		// Four shelves on the wall, each set out its own way.
		for (int i = 0; i < ApothecaryShelfBlock.ARRANGEMENTS; i++) {
			set(level, new BlockPos(x + 3 + i * 2, y + 2, z - 8), state("apothecary_shelf").setValue(ApothecaryShelfBlock.FACING, Direction.SOUTH)
					.setValue(ApothecaryShelfBlock.ARRANGEMENT, i));
		}
		// Cauldrons: a green brew over a campfire, a purple brew, water and an empty one.
		set(level, new BlockPos(x + 1, y, z - 5), Blocks.CAMPFIRE.defaultBlockState());
		set(level, new BlockPos(x + 1, y + 1, z - 5), state("bubbling_cauldron").setValue(BubblingCauldronBlock.CONTENTS, BubblingCauldronBlock.Brew.GREEN));
		set(level, new BlockPos(x + 3, y, z - 5), state("bubbling_cauldron").setValue(BubblingCauldronBlock.CONTENTS, BubblingCauldronBlock.Brew.PURPLE));
		set(level, new BlockPos(x + 4, y, z - 6), state("bubbling_cauldron").setValue(BubblingCauldronBlock.CONTENTS, BubblingCauldronBlock.Brew.ORANGE));
		set(level, new BlockPos(x + 5, y, z - 5), state("bubbling_cauldron").setValue(BubblingCauldronBlock.CONTENTS, BubblingCauldronBlock.Brew.WATER));
		// Two crystal balls on stumps, one being gazed into, and the grimoire between them.
		for (int i = 0; i < 2; i++) {
			BlockPos stump = new BlockPos(x + 8 + i * 3, y, z - 5);
			set(level, stump, Blocks.DARK_OAK_LOG.defaultBlockState());
			set(level, stump.above(), state("crystal_ball").setValue(CrystalBallBlock.GAZING, i == 1));
		}
		set(level, new BlockPos(x + 9, y, z - 5), state("grimoire_stand").setValue(GrimoireStandBlock.FACING, Direction.SOUTH)
				.setValue(GrimoireStandBlock.PAGE, GrimoireStandBlock.Spread.PUMPKIN));
		// The broom leaning by the wall.
		set(level, new BlockPos(x + 7, y, z - 7), state("witchs_broom").setValue(WitchsBroomBlock.FACING, Direction.SOUTH));
	}
}
