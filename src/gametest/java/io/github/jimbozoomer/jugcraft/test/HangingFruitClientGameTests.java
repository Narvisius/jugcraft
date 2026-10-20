package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Three seed examples per form, accessible fruit displays and untouched natural-biome stops. */
public class HangingFruitClientGameTests extends TreeBatchTwoClientGameTests {
	private final Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
	@Override protected List<String> shapes() { return List.of("baobab", "giant_baobab", "date_palm", "desert_acacia", "small_palm", "palm"); }
	@Override protected List<String> biomes() { return List.of("outback", "oasis", "dry_river"); }
	@Override protected String worldName() { return "Baobab and Date Palm Inspection"; }
	@Override protected String exportName() { return "tree-batch-10-preview"; }
	@Override protected String screenshotPrefix() { return "jugcraft_tree10_"; }
	@Override protected BlockPos surveySpot(ServerLevel level, BlockPos first, ResourceKey<Biome> biome) {
		var expected = JugcraftAgriculture.block(biome.identifier().getPath().equals("outback") ? "baobab_pod" : "date_cluster");
		var visited = new HashSet<BlockPos>();
		for (int[] offset : new int[][] {{0, 0}, {256, 0}, {-256, 0}, {0, 256}, {0, -256}, {256, 256}, {-256, -256}, {256, -256}, {-256, 256}}) {
			var at = offset[0] == 0 && offset[1] == 0 ? first : BiomeClientGameTests.surfaceSpot(level, first.offset(offset[0], 0, offset[1]), biome);
			if (at == null || !visited.add(at)) continue;
			int fruit = 0;
			for (int dx = -64; dx <= 64; dx++) for (int dz = -64; dz <= 64; dz++) {
				int x = at.getX() + dx, z = at.getZ() + dz; level.getChunk(x >> 4, z >> 4);
				int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
				for (int y = top; y >= Math.max(level.getMinY(), top - 24); y--) {
					var p = new BlockPos(x, y, z);
					if (level.getBiome(p).is(biome) && level.getBlockState(p).is(expected)) {
						if (!level.getBlockState(p).canSurvive(level, p)) throw new AssertionError("Floating natural fruit at " + p);
						fruit++;
					}
				}
			}
			Jugcraft.LOGGER.info("[tree 10 natural fruit] {} at {}: {}", biome.identifier(), at, fruit);
			if (fruit > 0) return at;
		}
		return null; // Try another region; only a surviving natural fruit accepts a stop.
	}
	@Override protected void extraDisplays(ClientGameTestContext context, TestSingleplayerContext world, List<String> tour) {
		var wall = GARDEN.offset(0, 0, 75);
		world.getServer().runOnServer(server -> {
			var level = server.overworld();
			for (var p : BlockPos.betweenClosed(wall.offset(-16, -1, -15), wall.offset(75, -1, 24))) level.setBlock(p, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
			PineWoodClientGameTests.comparisonRow(level, wall, 0, new String[]{"jugcraft:baobab", "jugcraft:eucalyptus", "jugcraft:palm", "minecraft:oak", "minecraft:cherry", "jugcraft:bald_cypress"});
			for (int type = 0; type < 2; type++) for (int age = 0; age <= 2; age++) {
				var p = wall.offset(type * 20 + age * 5, 2, 16);
				var leaves = JugcraftAgriculture.block(type == 0 ? "baobab_leaves" : "palm_fronds").defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
				level.setBlock(p.above(), leaves, Block.UPDATE_CLIENTS);
				var state = JugcraftAgriculture.block(type == 0 ? "baobab_pod" : "date_cluster").defaultBlockState().setValue(HangingFruitBlock.AGE, age);
				level.setBlock(p, state, Block.UPDATE_CLIENTS); saved.put(p, state);
				PineWoodClientGameTests.label(level, p.offset(0, -2, 2), type == 0 ? "Baobab pod" : "Date cluster", age == 2 ? "Ripe: right-click" : "Growth stage " + age);
			}
		});
		for (String item : List.of("jugcraft:baobab_sapling 32", "jugcraft:palm_sapling 32", "minecraft:bone_meal 64", "minecraft:iron_axe", "minecraft:shears", "jugcraft:baobab_seeds 16", "jugcraft:dates 16"))
			world.getServer().runCommand("give @a " + item);
		shoot(context, world, wall.offset(12, 6, 30), wall.offset(12, 2, 0), screenshotPrefix() + "wood_and_fruit");
		shoot(context, world, wall.offset(7, 3, 22), wall.offset(7, 2, 16), screenshotPrefix() + "pods");
		shoot(context, world, wall.offset(27, 3, 22), wall.offset(27, 2, 16), screenshotPrefix() + "dates");
		for (boolean cutout : List.of(true, false)) {
			context.runOnClient(client -> client.options.cutoutLeaves().set(cutout));
			shoot(context, world, GARDEN.offset(56, 7, 20), GARDEN.offset(40, 6, 0), screenshotPrefix() + (cutout ? "cutout" : "solid"));
		}
		context.runOnClient(client -> client.options.cutoutLeaves().set(true));
		tour.add("Reachable pods, date clusters and wood comparison: /tp @s 1015 180 -902 180 0");
		tour.add("The ripe display fruit can be right-clicked by hand or while holding shears. After picking it, random ticks regrow it.");
		tour.add("Natural date palms share palm saplings (40% date palm, 60% ordinary palm). Four baobab saplings in a square grow a giant.");
		tour.add("The inspection save freezes growth for comparison. /gamerule minecraft:random_tick_speed 3 restores fruit ripening.");
	}
	@Override protected void validateReopened(ServerLevel level) {
		saved.forEach((pos, state) -> { if (!level.getBlockState(pos).equals(state)) throw new AssertionError("Fruit stage changed across save/reopen at " + pos); });
	}
}
