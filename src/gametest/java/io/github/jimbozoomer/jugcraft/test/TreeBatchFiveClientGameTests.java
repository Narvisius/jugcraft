package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.FruitingLeavesBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SeasonalLeavesBlock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;

/** Saved fruit-stage comparisons and natural biome stops, alongside three real seeds of each shape. */
public class TreeBatchFiveClientGameTests extends TreeBatchTwoClientGameTests {
	private final Map<BlockPos, BlockState> savedFruit = new LinkedHashMap<>();
	@Override protected List<String> shapes() { return TreeBatchFiveGameTests.SHAPES; }
	@Override protected List<String> biomes() { return List.of("snowy_forest", "lake_district", "highland", "woodland", "wetland"); }
	@Override protected String worldName() { return "Rowan and Elder Inspection"; }
	@Override protected String exportName() { return "tree-batch-5-preview"; }
	@Override protected String screenshotPrefix() { return "jugcraft_tree5_"; }
	@Override protected boolean includeSubmergedColumns() { return true; }
	@Override protected BlockPos surveySpot(ServerLevel level, BlockPos first, ResourceKey<Biome> biome) {
		if (!biome.identifier().getPath().equals("wetland")) return first;
		var checked = new java.util.HashSet<BlockPos>();
		// This is a content-presence inspection stop, not a statistical estimate of the placement share.
		for (int[] offset : new int[][] {{0,0}, {256,0}, {-256,0}, {0,256}, {0,-256},
				{256,256}, {-256,256}, {256,-256}, {-256,-256}}) {
			BlockPos at = offset[0] == 0 && offset[1] == 0 ? first
					: BiomeClientGameTests.surfaceSpot(level, first.offset(offset[0], 0, offset[1]), biome);
			if (at == null || !checked.add(at)) continue;
			var counts = survey(level, at, biome, true);
			io.github.jimbozoomer.jugcraft.Jugcraft.LOGGER.info("[tree 5 wetland sample] {}: {}", at, counts);
			if (counts.getOrDefault("leaf:jugcraft:elder_leaves", 0) > 0) return at;
		}
		// The closest Wetland in this seed is a tiny flooded coastal fragment (one dry column).
		// Inspect at most eight other regional anchors rather than demanding a shrub in that water.
		for (int[] offset : new int[][] {{4096,0}, {-4096,0}, {0,4096}, {0,-4096},
				{4096,4096}, {-4096,4096}, {4096,-4096}, {-4096,-4096}}) {
			var nearest = level.findClosestBiome3d(holder -> holder.is(biome), first.offset(offset[0], 0, offset[1]), 4096, 64, 64);
			BlockPos at = nearest == null ? null : BiomeClientGameTests.surfaceSpot(level, nearest.getFirst(), biome);
			if (at == null || !checked.add(at)) continue;
			var counts = survey(level, at, biome, true);
			io.github.jimbozoomer.jugcraft.Jugcraft.LOGGER.info("[tree 5 wetland regional sample] {}: {}", at, counts);
			if (counts.getOrDefault("leaf:jugcraft:elder_leaves", 0) > 0) return at;
		}
		throw new AssertionError("No natural elder in " + checked.size() + " distinct Wetland samples");
	}
	@Override protected void validateSurvey(String biome, Map<String, Integer> survey) {
		String tree = biome.equals("woodland") || biome.equals("wetland") ? "elder" : "rowan";
		if (survey.getOrDefault("leaf:jugcraft:" + tree + "_leaves", 0) == 0) throw new AssertionError("No natural " + tree + " in " + biome);
	}
	@Override protected void validateReopened(ServerLevel level) {
		savedFruit.forEach((pos, state) -> {
			if (!level.getBlockState(pos).equals(state)) throw new AssertionError("Season/fruit state changed across save at " + pos);
		});
	}
	@Override protected void extraDisplays(ClientGameTestContext context, TestSingleplayerContext world, List<String> tour) {
		BlockPos wall = GARDEN.offset(0, 0, 80);
		BlockPos fruitWall = wall.offset(0, 0, 50);
		world.getServer().runOnServer(server -> {
			var level = server.overworld();
			for (var p : BlockPos.betweenClosed(wall.offset(-12, -1, -12), wall.offset(65, -1, 72))) level.setBlock(p, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
			PineWoodClientGameTests.comparisonRow(level, wall, 0, new String[] {"jugcraft:fruitwood", "minecraft:cherry", "minecraft:jungle", "jugcraft:bald_cypress", "jugcraft:maple"});
			for (int t = 0; t < 2; t++) for (int season = 0; season < 3; season++) for (int fruit = 0; fruit < 3; fruit++) {
				String tree = shapes().get(t);
				var leaf = JugcraftAgriculture.block(tree + "_leaves").defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
				BlockPos pos = fruitWall.offset(t * 18 + fruit * 4, season * 3 + 1, -8);
				level.setBlock(pos, leaf, Block.UPDATE_CLIENTS);
				var state = leaf.setValue(SeasonalLeavesBlock.SEASON, SeasonalLeavesBlock.Foliage.values()[season]).setValue(FruitingLeavesBlock.FRUIT, fruit);
				level.setBlock(pos, state, Block.UPDATE_CLIENTS);
				savedFruit.put(pos, state);
				if (season == 0) PineWoodClientGameTests.label(level, pos.offset(0, -1, 2), tree, "Fruit stage " + fruit, "Green / Gold / Bare");
			}
		});
		shoot(context, world, wall.offset(12, 6, 25), wall.offset(12, 2, 0), screenshotPrefix() + "fruitwood");
		for (boolean cutout : List.of(true, false)) {
			context.runOnClient(client -> client.options.cutoutLeaves().set(cutout));
			shoot(context, world, fruitWall.offset(13, 6, 12), fruitWall.offset(13, 5, -8), screenshotPrefix() + (cutout ? "fruit_cutout" : "fruit_solid"));
		}
		context.runOnClient(client -> client.options.cutoutLeaves().set(true));
		for (int t = 0; t < 2; t++) for (int season = 0; season < 3; season++) {
			String tree = shapes().get(t);
			var look = SeasonalLeavesBlock.Foliage.values()[season];
			BlockPos at = GARDEN.offset(season * 40, 0, -180 - t * 50);
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				TreeBatchTwoGameTests.reset(level, at, Blocks.GRASS_BLOCK);
				for (var p : BlockPos.betweenClosed(at.offset(-16, -2, -16), at.offset(16, -2, 16))) level.setBlock(p, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
				if (!TreeBatchTwoGameTests.feature(level, tree).place(level, level.getChunkSource().getGenerator(), RandomSource.create(4096), at)) throw new AssertionError("Season display failed");
				for (var p : BlockPos.betweenClosed(at.offset(-12, 0, -12), at.offset(12, 15, 12))) {
					var state = level.getBlockState(p);
					if (state.is(JugcraftAgriculture.block(tree + "_leaves"))) {
						state = state.setValue(SeasonalLeavesBlock.SEASON, look);
						if (FruitingLeavesBlock.canFruit(state, level, p)) state = state.setValue(FruitingLeavesBlock.FRUIT, 2);
						level.setBlock(p, state, Block.UPDATE_CLIENTS);
					}
				}
				PineWoodClientGameTests.label(level, at.offset(0, 0, 10), tree, look.getSerializedName(), "Ripe exposed fruit");
			});
			shoot(context, world, at.offset(12, tree.equals("rowan") ? 6 : 3, 15), at.above(tree.equals("rowan") ? 4 : 1), screenshotPrefix() + tree + "_" + look.getSerializedName());
			tour.add(tree + " " + look.getSerializedName() + ": /tp @s " + at.getX() + " 181 " + (at.getZ() + 12) + " 180 0");
		}
		tour.add("Fruitwood comparison: /tp @s 1013 181 -900 180 0");
		tour.add("All 18 fruit/season combinations: /tp @s 1013 181 -850 180 0");
		tour.add("Random ticks are frozen for inspection. /gamerule minecraft:random_tick_speed 3 restores natural fruit ripening.");
	}
}
