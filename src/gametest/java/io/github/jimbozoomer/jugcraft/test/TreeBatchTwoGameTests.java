package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.SeasonalLeavesBlock;
import io.github.jimbozoomer.jugcraft.season.JugcraftSeasons;
import io.github.jimbozoomer.jugcraft.season.SeasonCalendar;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/** Real feature placement, seeded variation, seasons, soil, clearance and single/2x2 sapling growth. */
public class TreeBatchTwoGameTests {
	static final List<String> SHAPES = List.of("cottonwood", "japanese_maple", "live_oak", "pine",
			"big_jacaranda", "mallee", "giant_eucalyptus");
	private static final BlockPos AT = new BlockPos(18000, 100, -18000);

	static PlacedFeature feature(ServerLevel level, String name) {
		return level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE)
				.getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, Jugcraft.id(name + "_checked"))).value();
	}

	static void reset(ServerLevel level, BlockPos at, Block soil) {
		for (BlockPos pos : BlockPos.betweenClosed(at.offset(-16, 0, -16), at.offset(16, 48, 16))) {
			if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
		for (BlockPos pos : BlockPos.betweenClosed(at.offset(-16, -1, -16), at.offset(16, -1, 16))) {
			level.setBlock(pos, soil.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	record Size(int height, int width, int logs, int leaves, int moss, long hash) { }

	static Size measure(ServerLevel level, BlockPos at) {
		int top = 0, minX = 99, maxX = -99, minZ = 99, maxZ = -99, logs = 0, leaves = 0, moss = 0;
		long hash = 1;
		for (BlockPos pos : BlockPos.betweenClosed(at.offset(-16, 0, -16), at.offset(16, 48, 16))) {
			BlockState state = level.getBlockState(pos);
			boolean log = state.is(BlockTags.LOGS), leaf = state.is(BlockTags.LEAVES);
			if (log) logs++;
			if (leaf) leaves++;
			if (state.is(JugcraftAgriculture.block("shroud_moss"))) moss++;
			if (log || leaf) {
				top = Math.max(top, pos.getY() - at.getY() + 1);
				minX = Math.min(minX, pos.getX() - at.getX()); maxX = Math.max(maxX, pos.getX() - at.getX());
				minZ = Math.min(minZ, pos.getZ() - at.getZ()); maxZ = Math.max(maxZ, pos.getZ() - at.getZ());
				hash = 31 * hash + pos.subtract(at).asLong() + (log ? 1 : 2);
			}
		}
		return new Size(top, Math.max(maxX - minX + 1, maxZ - minZ + 1), logs, leaves, moss, hash);
	}

	@GameTest(maxTicks = 400)
	public void shapesVaryReproduceAndUseSeasonalLeaves(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var before = JugcraftSeasons.settings();
		try {
			JugcraftSeasons.setMode(level.getServer(), SeasonCalendar.Mode.AUTUMN);
			for (String shape : SHAPES) {
				var sizes = new ArrayList<String>();
				var hashes = new HashSet<Long>();
				int moss = 0;
				for (long seed : new long[] {1, 2, 3, 4096, 8192, 65537, -7, 987654321, 314159265, -271828182, Long.MAX_VALUE, Long.MIN_VALUE}) {
					reset(level, AT, Blocks.DIRT);
					helper.assertTrue(feature(level, shape).place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), AT),
							shape + " failed in open space, seed " + seed);
					Size size = measure(level, AT);
					helper.assertTrue(size.logs() >= 2 && size.leaves() >= 8, shape + " missing its trunk or crown: " + size);
					int[] limits = switch (shape) {
						case "cottonwood" -> new int[] {12, 18, 19};
						case "japanese_maple" -> new int[] {3, 6, 11};
						case "live_oak" -> new int[] {6, 9, 19};
						case "pine" -> new int[] {6, 10, 13};
						case "big_jacaranda" -> new int[] {10, 14, 17};
						case "mallee" -> new int[] {4, 7, 11};
						case "giant_eucalyptus" -> new int[] {28, 42, 9};
						default -> throw new AssertionError(shape);
					};
					helper.assertTrue(size.height() >= limits[0] && size.height() <= limits[1] && size.width() <= limits[2],
							shape + " left its intended size envelope: " + size);
					hashes.add(size.hash()); moss += size.moss();
					sizes.add(size.height() + "h/" + size.width() + "w/" + size.logs() + "logs/" + size.leaves() + "leaves");
					for (BlockPos pos : BlockPos.betweenClosed(AT.offset(-16, 0, -16), AT.offset(16, 48, 16))) {
						BlockState state = level.getBlockState(pos);
						if (state.hasProperty(SeasonalLeavesBlock.SEASON)) helper.assertTrue(
								state.getValue(SeasonalLeavesBlock.SEASON) == SeasonalLeavesBlock.Foliage.GOLD,
								shape + " placed out-of-season foliage");
					}
					reset(level, AT, Blocks.DIRT);
					feature(level, shape).place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), AT);
					helper.assertTrue(measure(level, AT).equals(size), shape + " is not reproducible for seed " + seed);
				}
				Jugcraft.LOGGER.info("[tree batch 2] {}: {}", shape, sizes);
				helper.assertTrue(hashes.size() >= 3, shape + " repeats the same shape across seeds");
				if (shape.equals("live_oak")) helper.assertTrue(moss > 0, "Live oaks have no shroud moss");
			}
		} finally {
			JugcraftSeasons.setMode(level.getServer(), before.mode());
			reset(level, AT, Blocks.DIRT);
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 200)
	public void soilAndObstructionsAreRespected(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = AT.offset(80, 0, 0);
		for (String shape : SHAPES) {
			reset(level, at, Blocks.STONE);
			helper.assertTrue(!feature(level, shape).place(level, level.getChunkSource().getGenerator(), RandomSource.create(4), at),
					shape + " grew on stone");
			reset(level, at, Blocks.DIRT);
			level.setBlock(at.above(), Blocks.DIAMOND_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
			helper.assertTrue(!feature(level, shape).place(level, level.getChunkSource().getGenerator(), RandomSource.create(4), at),
					shape + " ignored a solid trunk obstruction");
			helper.assertTrue(level.getBlockState(at.above()).is(Blocks.DIAMOND_BLOCK), "Tree replaced an obstruction");
		}
		for (String shape : List.of("mallee", "desert_acacia", "small_palm")) for (Block sand : List.of(Blocks.SAND, Blocks.RED_SAND)) {
			reset(level, at, sand);
			helper.assertTrue(feature(level, shape).place(level, level.getChunkSource().getGenerator(), RandomSource.create(6), at),
					shape + " failed on " + BuiltInRegistries.BLOCK.getKey(sand));
			helper.assertTrue(measure(level, at).logs() > 0, shape + " placed no wood on sand");
		}
		reset(level, at, Blocks.DIRT);
		helper.succeed();
	}

	@GameTest(maxTicks = 200)
	public void saplingsGrowAndBlockedSquaresKeepEveryOriginalState(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = AT.offset(160, 0, 0);
		for (String tree : List.of("pine", "eucalyptus", "redwood", "mahogany")) {
			reset(level, at, Blocks.DIRT);
			SaplingBlock sapling = (SaplingBlock) JugcraftAgriculture.block(tree + "_sapling");
			BlockState ready = sapling.defaultBlockState().setValue(SaplingBlock.STAGE, 1);
			level.setBlock(at, ready, Block.UPDATE_CLIENTS);
			sapling.advanceTree(level, at, ready, RandomSource.create(2));
			helper.assertTrue(measure(level, at).logs() >= 4, tree + " single sapling did not grow");
			if (tree.equals("pine")) continue;
			reset(level, at, Blocks.DIRT);
			List<BlockPos> square = List.of(at, at.east(), at.south(), at.east().south());
			for (int i = 0; i < square.size(); i++) level.setBlock(square.get(i),
					sapling.defaultBlockState().setValue(SaplingBlock.STAGE, i % 2), Block.UPDATE_CLIENTS);
			List<BlockState> original = square.stream().map(level::getBlockState).toList();
			// East edge blocks a 2x2 trunk while leaving a lone western sapling's lower trunk free.
			level.setBlock(at.east().above(3), Blocks.DIAMOND_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
			BlockPos trigger = square.get(3);
			sapling.advanceTree(level, trigger, level.getBlockState(trigger), RandomSource.create(2));
			for (int i = 0; i < square.size(); i++) helper.assertTrue(level.getBlockState(square.get(i)).equals(original.get(i)),
					tree + " changed blocked sapling " + i);
			level.setBlock(at.east().above(3), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			sapling.advanceTree(level, trigger, level.getBlockState(trigger), RandomSource.create(2));
			for (BlockPos pos : square) helper.assertTrue(level.getBlockState(pos).is(JugcraftAgriculture.block(tree + "_log")),
					tree + " did not grow a 2x2 trunk after the obstruction was removed");
			if (tree.equals("eucalyptus")) helper.assertTrue(measure(level, at).height() >= 28, "Eucalyptus square grew a small tree");
		}
		reset(level, at, Blocks.DIRT);
		helper.succeed();
	}
}
