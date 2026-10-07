package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.ChestnutLeavesBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CranberryBushBlock;
import io.github.jimbozoomer.jugcraft.agriculture.GourdStemBlock;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Client game test for the festival crops: a gourd patch, a cranberry bog, chestnut trees with ripe burs,
 * a chestnut-wood market stall lit by Turnip Lanterns and every growth stage, built in a real client and
 * photographed by day and at night (CI job {@code client}).
 */
public class FestivalClientGameTests implements FabricClientGameTest {
	private static final String[] GOURDS = {"butternut_squash", "acorn_squash", "warty_gourd"};

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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 6, y - 1, z - 32, x + 42, y - 1, z + 6));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 6, y, z - 32, x + 42, y + 14, z + 6));
			server.runOnServer(minecraft -> buildFestival(minecraft.overworld(), origin));
			System.out.println("[festival test] cranberry bushes after building: "
					+ server.computeOnServer(minecraft -> countBushes(minecraft.overworld(), origin)));

			server.runCommand("gamerule sendCommandFeedback false");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("tp @p %d %d %d 180 30".formatted(x + 12, y, z + 2));
			context.waitTicks(220);
			singleplayer.getConnection().waitForChunksRender();
			System.out.println("[festival test] cranberry bushes after 220 ticks: "
					+ server.computeOnServer(minecraft -> countBushes(minecraft.overworld(), origin)));

			shoot(context, singleplayer, x + 20, y + 13, z + 6, 180, 40, "jugcraft_festival_harvest");
			shoot(context, singleplayer, x + 5, y + 3, z - 14, 180, 32, "jugcraft_gourd_patch");
			shoot(context, singleplayer, x + 19, y + 4, z - 11, 180, 38, "jugcraft_cranberry_bog");
			shoot(context, singleplayer, x + 33, y + 2, z - 7, 180, -8, "jugcraft_chestnut_trees");
			shoot(context, singleplayer, x + 12, y + 3, z + 3, 180, 38, "jugcraft_festival_stages");
			server.runCommand("time set midnight");
			shoot(context, singleplayer, x + 4, y + 2, z - 1, 180, 12, "jugcraft_turnip_lanterns");
		}
	}

	/** Stands the player at a spot (on an invisible barrier) and takes a screenshot. */
	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z,
			int yaw, int pitch, String name) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("setblock %d %d %d minecraft:barrier".formatted(x, y - 1, z));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f %d %d", x + 0.5, y, z + 0.5, yaw, pitch));
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
	}

	/** Cranberry bushes in the bog and the growth-stage trench (for the CI log). */
	private static int countBushes(ServerLevel level, BlockPos origin) {
		int count = 0;
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(14, 0, -26), origin.offset(24, 0, -2))) {
			if (level.getBlockState(pos).getBlock() instanceof CranberryBushBlock) {
				count++;
			}
		}
		return count;
	}

	private static void set(ServerLevel level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state, Block.UPDATE_ALL);
	}

	private static BlockState state(String id) {
		return JugcraftAgriculture.block(id).defaultBlockState();
	}

	private static void buildFestival(ServerLevel level, BlockPos origin) {
		// The gourd patch: rows of stems on farmland, each bent to the gourd it grew, with a few still growing.
		for (int row = 0; row < 3; row++) {
			int dz = -25 + row * 4;
			Direction side = row % 2 == 0 ? Direction.SOUTH : Direction.NORTH;
			for (int dx = 0; dx <= 10; dx++) {
				String gourd = GOURDS[(dx + row) % GOURDS.length];
				BlockPos stem = origin.offset(dx, 0, dz);
				set(level, stem.below(), Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
				GourdStemBlock block = (GourdStemBlock) JugcraftAgriculture.block(gourd + "_stem");
				boolean young = (dx * 7 + row) % 5 == 0;
				set(level, stem, block.defaultBlockState().setValue(GourdStemBlock.AGE, young ? 4 : GourdStemBlock.MAX_AGE));
				if (!young) {
					block.growGourd(level, stem, dx % 3 == 0 ? side.getOpposite() : side);
				}
			}
		}

		// The cranberry bog: a shallow pool over mud behind a low bank, full of ripe bushes and a few in flower.
		for (int dx = 14; dx <= 24; dx++) {
			for (int dz = -26; dz <= -16; dz++) {
				BlockPos pos = origin.offset(dx, 0, dz);
				boolean bank = dx == 14 || dx == 24 || dz == -26 || dz == -16;
				if (bank) {
					set(level, pos, Blocks.GRASS_BLOCK.defaultBlockState());
					continue;
				}
				set(level, pos.below(), Blocks.MUD.defaultBlockState());
				set(level, pos, Blocks.WATER.defaultBlockState());
			}
		}
		// Planted into the water, as a player plants cranberries.
		for (int dx = 15; dx <= 23; dx++) {
			for (int dz = -25; dz <= -17; dz++) {
				int age = (dx + dz) % 7 == 0 ? -1 : (dx * 3 + dz) % 5 == 0 ? 2 : 3;
				if (age >= 0) {
					set(level, origin.offset(dx, 0, dz), state("cranberry_bush").setValue(CranberryBushBlock.AGE, age));
				}
			}
		}

		// Two chestnut trees, their undersides hung with burs, most of them ripe.
		for (BlockPos trunk : new BlockPos[] {origin.offset(30, 0, -20), origin.offset(37, 0, -14)}) {
			set(level, trunk, state("chestnut_sapling"));
			JugcraftAgriculture.CHESTNUT_GROWER.growTree(level, level.getChunkSource().getGenerator(), trunk, level.getBlockState(trunk),
					level.getRandom());
			for (BlockPos pos : BlockPos.betweenClosed(trunk.offset(-4, 1, -4), trunk.offset(4, 12, 4))) {
				BlockState leaves = level.getBlockState(pos);
				if (leaves.is(JugcraftAgriculture.block("chestnut_leaves")) && !leaves.getValue(LeavesBlock.PERSISTENT)
						&& level.isEmptyBlock(pos.below())) {
					set(level, pos, leaves.setValue(ChestnutLeavesBlock.FRUIT, (pos.getX() + pos.getZ()) % 4 == 0 ? 1 : 2));
				}
			}
		}

		// A chestnut-wood market stall with gourds for sale, and Turnip Lanterns on fence posts along the path.
		BlockPos stall = origin.offset(0, 0, -12);
		for (int dx = 0; dx <= 6; dx++) {
			for (int dz = 0; dz <= 3; dz++) {
				set(level, stall.offset(dx, -1, dz), state("chestnut_planks"));
				set(level, stall.offset(dx, 3, dz), state("chestnut_slab").setValue(SlabBlock.TYPE, SlabType.BOTTOM));
			}
			set(level, stall.offset(dx, 0, 3), state("chestnut_stairs").setValue(StairBlock.FACING, Direction.NORTH));
			set(level, stall.offset(dx, 1, 3), state(GOURDS[dx % GOURDS.length]).setValue(HorizontalDirectionalBlock.FACING,
					Direction.values()[2 + dx % 4]));
		}
		for (int dx : new int[] {0, 6}) {
			for (int dz : new int[] {0, 3}) {
				for (int dy = 0; dy <= 2; dy++) {
					if (dz == 0 || dy == 2) {
						set(level, stall.offset(dx, dy, dz), state("chestnut_fence"));
					}
				}
			}
		}
		set(level, stall.offset(0, 0, 0), state("chestnut_log"));
		set(level, stall.offset(6, 0, 0), state("stripped_chestnut_log"));
		for (int dx = -2; dx <= 10; dx += 3) {
			BlockPos post = origin.offset(dx, 0, -6);
			set(level, post, state("chestnut_fence"));
			set(level, post.above(), state("turnip_lantern").setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
		}
		set(level, origin.offset(12, 0, -6), state("chestnut_fence_gate"));

		// Every growth stage, youngest on the left: turnips, gourd stems, cranberries in a trench, the sapling.
		for (int age = 0; age <= 7; age++) {
			BlockPos turnip = origin.offset(age, 0, -3);
			set(level, turnip.below(), Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
			set(level, turnip, ((CropBlock) JugcraftAgriculture.block("turnip_crop")).getStateForAge(age));
			BlockPos stem = origin.offset(9 + age, 0, -3);
			set(level, stem.below(), Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
			set(level, stem, state("butternut_squash_stem").setValue(GourdStemBlock.AGE, age));
		}
		for (int dx = 18; dx <= 23; dx++) {
			for (int dz = -4; dz <= -2; dz++) {
				BlockPos pos = origin.offset(dx, 0, dz);
				boolean inside = dx > 18 && dx < 23 && dz == -3;
				if (!inside) {
					set(level, pos, Blocks.GRASS_BLOCK.defaultBlockState());
				} else {
					set(level, pos.below(), Blocks.MUD.defaultBlockState());
					set(level, pos, Blocks.WATER.defaultBlockState());
					set(level, pos, state("cranberry_bush").setValue(CranberryBushBlock.AGE, dx - 19));
				}
			}
		}
		set(level, origin.offset(25, 0, -3), state("chestnut_sapling"));
		set(level, origin.offset(27, 0, -3), state("wild_turnip"));
	}
}
