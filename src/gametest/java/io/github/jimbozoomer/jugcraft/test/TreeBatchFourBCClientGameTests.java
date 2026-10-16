package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.SeasonalLeavesBlock;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;

/** Natural biome stops, repeated shapes, water depths, seasonal crowns and complete timber samples. */
public class TreeBatchFourBCClientGameTests extends TreeBatchTwoClientGameTests {
	@Override protected List<String> shapes() { return TreeBatchFourBCGameTests.SHAPES; }
	@Override protected List<String> biomes() { return List.of("bayou", "lush_swamp", "swamp_woods", "webwood", "flower_isle"); }
	@Override protected String worldName() { return "Cypress and Magnolia Inspection"; }
	@Override protected String exportName() { return "tree-batch-4bc-preview"; }
	@Override protected String screenshotPrefix() { return "jugcraft_tree4bc_"; }
	@Override protected boolean includeSubmergedColumns() { return true; }
	@Override protected void validateSurvey(String biome, Map<String, Integer> survey) {
		String leaves = biome.equals("flower_isle") ? "magnolia_leaves" : "bald_cypress_needles";
		if (survey.getOrDefault("leaf:jugcraft:" + leaves, 0) == 0) throw new AssertionError("No new foliage in " + biome);
		if (biome.equals("bayou") && survey.getOrDefault("wet_log:jugcraft:bald_cypress_log", 0) == 0)
			throw new AssertionError("No natural cypress timber over wet Bayou columns");
	}
	@Override protected void extraDisplays(ClientGameTestContext context, TestSingleplayerContext world, List<String> tour) {
		BlockPos wall = GARDEN.offset(0, 0, 80);
		world.getServer().runOnServer(server -> {
			var level = server.overworld();
			for (var p : BlockPos.betweenClosed(wall.offset(-15, -1, -10), wall.offset(65, -1, 20)))
				level.setBlock(p, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
			PineWoodClientGameTests.comparisonRow(level, wall, 0, new String[] {"jugcraft:bald_cypress", "jugcraft:magnolia",
					"jugcraft:willow", "jugcraft:cedar", "minecraft:oak", "minecraft:pale_oak", "jugcraft:chestnut"});
			for (int i = 0; i < shapes().size(); i++) {
				String tree = shapes().get(i);
				var state = JugcraftAgriculture.block(TreeBatchFourBCGameTests.leaf(tree)).defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
				for (var p : BlockPos.betweenClosed(wall.offset(i * 8, 2, -8), wall.offset(i * 8 + 4, 4, -8))) level.setBlock(p, state, Block.UPDATE_CLIENTS);
				level.setBlock(wall.offset(i * 8 + 2, 0, -8), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
				level.setBlock(wall.offset(i * 8 + 2, 1, -8), JugcraftAgriculture.block(tree + "_sapling").defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		});
		shoot(context, world, wall.offset(14, 7, 28), wall.offset(14, 2, 0), screenshotPrefix() + "woods");
		for (boolean cutout : List.of(true, false)) {
			context.runOnClient(client -> client.options.cutoutLeaves().set(cutout));
			shoot(context, world, wall.offset(6, 3, 1), wall.offset(6, 3, -8), screenshotPrefix() + (cutout ? "cutout" : "solid"));
		}
		context.runOnClient(client -> client.options.cutoutLeaves().set(true));
		for (int depth = 0; depth <= 3; depth++) {
			int water = depth;
			BlockPos at = GARDEN.offset(depth * 40, 0, -160);
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				TreeBatchFourBCGameTests.plot(level, at, water);
				for (var p : BlockPos.betweenClosed(at.offset(-16, -2, -16), at.offset(16, -2, 16))) level.setBlock(p, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
				if (water > 0) for (var p : BlockPos.betweenClosed(at.offset(-13, 0, -13), at.offset(13, water - 1, 13)))
					if (Math.abs(p.getX() - at.getX()) == 13 || Math.abs(p.getZ() - at.getZ()) == 13)
						level.setBlock(p, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
				// Seed 3 has no low limb hiding the roots; seeds 1/2/3 remain together in the main garden.
				if (!TreeBatchTwoGameTests.feature(level, "bald_cypress").place(level, level.getChunkSource().getGenerator(), RandomSource.create(3), at))
					throw new AssertionError("Cypress water display failed at depth " + water);
				PineWoodClientGameTests.label(level, at.offset(0, 0, 15), "Cypress roots", water + " blocks of water");
			});
			shoot(context, world, at.offset(8, 5, 10), at.above(1), screenshotPrefix() + "roots_" + depth);
			if (depth == 3) shoot(context, world, at.offset(4, 0, 5), at, screenshotPrefix() + "roots_underwater");
			tour.add("Cypress at water depth " + depth + ": /tp @s " + at.getX() + " 184 " + (at.getZ() + 16) + " 180 15");
		}
		for (int i = 0; i < 3; i++) {
			var season = SeasonalLeavesBlock.Foliage.values()[i];
			BlockPos at = GARDEN.offset(i * 40, 0, -220);
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				TreeBatchFourBCGameTests.plot(level, at, 0);
				for (var p : BlockPos.betweenClosed(at.offset(-16, -2, -16), at.offset(16, -2, 16))) level.setBlock(p, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
				if (!TreeBatchTwoGameTests.feature(level, "bald_cypress").place(level, level.getChunkSource().getGenerator(), RandomSource.create(4096), at))
					throw new AssertionError("Season display failed");
				for (var p : BlockPos.betweenClosed(at.offset(-12, 0, -12), at.offset(12, 24, 12))) {
					var state = level.getBlockState(p);
					if (state.hasProperty(SeasonalLeavesBlock.SEASON)) level.setBlock(p, state.setValue(SeasonalLeavesBlock.SEASON, season), Block.UPDATE_CLIENTS);
				}
				PineWoodClientGameTests.label(level, at.offset(0, 0, 12), "Bald cypress", season.getSerializedName());
			});
			shoot(context, world, at.offset(20, 9, 24), at.above(7), screenshotPrefix() + "season_" + season.getSerializedName());
		}
		tour.add("Wood and leaf samples: /tp @s 1014 183 -900 180 10");
		tour.add("Cypress seasonal row: /tp @s 1040 180 -1208 180 0");
		BlockPos bareWall = GARDEN.offset(40, 0, 110);
		world.getServer().runOnServer(server -> {
			var level = server.overworld();
			for (var p : BlockPos.betweenClosed(bareWall.offset(-12, -1, -12), bareWall.offset(12, -1, 12)))
				level.setBlock(p, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
			var green = JugcraftAgriculture.block("bald_cypress_needles").defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
			var bare = green.setValue(SeasonalLeavesBlock.SEASON, SeasonalLeavesBlock.Foliage.BARE);
			for (var p : BlockPos.betweenClosed(bareWall.offset(-3, 0, 0), bareWall.offset(-1, 3, 0))) {
				if (p.getY() == bareWall.getY()) level.setBlock(p, JugcraftAgriculture.block("bald_cypress_log").defaultBlockState(), Block.UPDATE_CLIENTS);
				else {
					level.setBlock(p, green, Block.UPDATE_CLIENTS);
					level.setBlock(p, bare, Block.UPDATE_CLIENTS);
				}
			}
			level.setBlock(bareWall.offset(3, 0, 0), JugcraftAgriculture.block("bald_cypress_log").defaultBlockState(), Block.UPDATE_CLIENTS);
			level.setBlock(bareWall.offset(3, 1, 0), green, Block.UPDATE_CLIENTS);
			level.setBlock(bareWall.offset(3, 1, 0), bare, Block.UPDATE_CLIENTS);
			PineWoodClientGameTests.label(level, bareWall.offset(0, 0, 3), "Bare cypress", "Joined twigs", "Wall / single block");
		});
		shoot(context, world, bareWall.offset(6, 4, 10), bareWall.offset(0, 2, 0), screenshotPrefix() + "bare_blocks");
		tour.add("Bare cypress block and tiled wall: /tp @s 1040 181 -882 180 0");
		tour.add("Season displays are frozen for comparison. /gamerule minecraft:random_tick_speed 3 restores ordinary updates.");
	}
}
