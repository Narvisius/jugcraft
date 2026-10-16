package io.github.jimbozoomer.jugcraft.test;

import java.util.List;
import java.util.Map;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;

/** Three seeded examples of each form, plus all four naturally generated wonder-biome stops. */
public class TreeBatchFourClientGameTests extends TreeBatchTwoClientGameTests {
	@Override protected List<String> shapes() { return TreeBatchFourGameTests.SHAPES; }
	@Override protected List<String> biomes() { return List.of("snowpetal_grove", "frostlight_garden", "starlit_wood", "glimmer_grove"); }
	@Override protected String worldName() { return "Tree Batch 4 Inspection"; }
	@Override protected String exportName() { return "tree-batch-4-preview"; }
	@Override protected String screenshotPrefix() { return "jugcraft_tree4_"; }
	@Override protected void extraDisplays(ClientGameTestContext context, TestSingleplayerContext world, List<String> tour) {
		BlockPos wall = GARDEN.offset(0, 0, 45);
		world.getServer().runOnServer(server -> {
			var level = server.overworld();
			for (BlockPos pos : BlockPos.betweenClosed(wall.offset(-8, -1, -5), wall.offset(20, -1, 12)))
				level.setBlock(pos, Blocks.SNOW_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
			var trees = List.of("snowpetal_cherry", "shimmer_birch");
			for (int i = 0; i < trees.size(); i++) {
				String name = trees.get(i);
				var leaves = JugcraftAgriculture.block(name + "_leaves").defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
				for (BlockPos pos : BlockPos.betweenClosed(wall.offset(i * 8, 2, 0), wall.offset(i * 8 + 4, 4, 0)))
					level.setBlock(pos, leaves, Block.UPDATE_CLIENTS);
				level.setBlock(wall.offset(i * 8 + 2, 0, 0), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
				level.setBlock(wall.offset(i * 8 + 2, 1, 0), JugcraftAgriculture.block(name + "_sapling").defaultBlockState(), Block.UPDATE_CLIENTS);
				PineWoodClientGameTests.label(level, wall.offset(i * 8 + 2, 0, 3), name.replace('_', ' '), i == 0 ? "Vanilla cherry timber" : "Vanilla birch timber");
			}
		});
		for (boolean cutout : List.of(true, false)) {
			context.runOnClient(client -> client.options.cutoutLeaves().set(cutout));
			shoot(context, world, wall.offset(6, 3, 10), wall.offset(6, 3, 0), screenshotPrefix() + (cutout ? "cutout_leaves" : "solid_leaves"));
		}
		context.runOnClient(client -> {
			client.options.cutoutLeaves().set(true);
			if (client.particleEngine.createParticle(JugcraftAgriculture.SNOWPETAL, wall.getX() + 2, wall.getY() + 3, wall.getZ() + 1, 0, 0, 0) == null)
				throw new AssertionError("Snowpetal particle provider is missing");
		});
		world.getServer().runCommand("time set midnight");
		shoot(context, world, GARDEN.offset(20, 7, 24), GARDEN.above(5), screenshotPrefix() + "snowpetal_night");
		world.getServer().runCommand("time set noon");
		tour.add("Leaf and sapling comparison against snow: /tp @s 1006 180 -945 180 0");
	}
	@Override protected void validateSurvey(String biome, Map<String, Integer> survey) {
		String tree = biome.equals("snowpetal_grove") ? "snowpetal_cherry" : "shimmer_birch";
		if (survey.getOrDefault("leaf:jugcraft:" + tree + "_leaves", 0) == 0)
			throw new AssertionError("No " + tree + " generated in the " + biome + " survey");
	}
}
