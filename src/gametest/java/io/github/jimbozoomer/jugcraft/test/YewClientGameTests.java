package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.FruitingLeavesBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;

/** The shared comparison garden plus the yew's neighbours, fruit states and untouched Gloomweald. */
public class YewClientGameTests extends TreeBatchTwoClientGameTests {
	@Override protected List<String> shapes() { return List.of("yew", "ancient_yew"); }
	@Override protected List<String> biomes() { return List.of("gloomweald"); }
	@Override protected String worldName() { return "Yew Inspection"; }
	@Override protected String exportName() { return "tree-batch-3-preview"; }
	@Override protected String screenshotPrefix() { return "jugcraft_yew_"; }
	@Override protected void validateSurvey(String biome, Map<String, Integer> survey) {
		if (survey.getOrDefault("log:jugcraft:yew_log", 0) == 0) throw new AssertionError("No yew generated in the Gloomweald survey");
	}
	@Override protected void extraDisplays(ClientGameTestContext context, TestSingleplayerContext world, List<String> tour) {
		BlockPos wall = GARDEN.offset(0, 0, 80);
		world.getServer().runOnServer(server -> {
			var level = server.overworld();
			for (BlockPos pos : BlockPos.betweenClosed(wall.offset(-25, -1, -10), wall.offset(55, -1, 10)))
				level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
			PineWoodClientGameTests.comparisonRow(level, wall, 0, new String[] {"minecraft:dark_oak", "minecraft:mangrove", "jugcraft:yew",
					"jugcraft:cedar", "jugcraft:cypress", "jugcraft:redwood", "jugcraft:mahogany"});
			for (int fruit = 0; fruit <= 2; fruit++) {
				var state = JugcraftAgriculture.block("yew_needles").defaultBlockState().setValue(LeavesBlock.PERSISTENT, true)
						.setValue(FruitingLeavesBlock.FRUIT, fruit);
				for (BlockPos pos : BlockPos.betweenClosed(wall.offset(fruit * 4, 0, -8), wall.offset(fruit * 4 + 1, 1, -8)))
					level.setBlock(pos, state, Block.UPDATE_CLIENTS);
				PineWoodClientGameTests.label(level, wall.offset(fruit * 4, 0, -6), "Yew arils", List.of("Bare", "Unripe", "Ripe").get(fruit));
			}
		});
		tour.add("Yew wood beside dark oak, mangrove, cedar, cypress, redwood and mahogany: /tp @s 1010 183 -905 180 10");
		shoot(context, world, wall.offset(10, 8, 28), wall.offset(10, 2, 0), "jugcraft_yew_wood_comparison");
		shoot(context, world, wall.offset(5, 3, -2), wall.offset(5, 1, -8), "jugcraft_yew_arils");
	}
}
