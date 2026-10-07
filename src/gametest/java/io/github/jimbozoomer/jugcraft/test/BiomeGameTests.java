package io.github.jimbozoomer.jugcraft.test;

import com.mojang.datafixers.util.Pair;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.SeasonalLeavesBlock;
import io.github.jimbozoomer.jugcraft.biome.JugcraftDimensions;
import io.github.jimbozoomer.jugcraft.biome.JugcraftRegions;
import io.github.jimbozoomer.jugcraft.config.FeatureEnabledCondition;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.season.JugcraftSeasons;
import io.github.jimbozoomer.jugcraft.season.SeasonCalendar;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The biomes branch: Jugcraft regions, the seasonal-forest trees (batch 1), and the fields' plants and jacaranda (batch 2);
 * and the tree roster's batch 1: its shapes, the cedar's wood and the tree lists of the biomes it changed.
 */
public class BiomeGameTests {
	private static final Logger LOGGER = LoggerFactory.getLogger("jugcraft-test");

	private static Holder<MultiNoiseBiomeSourceParameterList> overworldPreset(ServerLevel level) {
		return level.registryAccess().lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
				.getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD);
	}

	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	// ---------------------------------------------------------------- regions

	/**
	 * The layouts were recorded from the Overworld biome builder, every rule placed its biome in each of its layouts (it
	 * matches some entry no earlier rule takes), and vanilla's own table lists every Jugcraft biome once, at the
	 * unreachable climate. Logs each layout's Jugcraft biomes, and vanilla's table by biome, climate bands and weirdness
	 * half (what the rules can match).
	 */
	@GameTest
	public void regionLayoutFollowsItsRules(GameTestHelper helper) {
		List<List<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>> layouts = JugcraftRegions.layouts();
		helper.assertTrue(layouts != null && layouts.size() == JugcraftRegions.LAYOUTS, "The layouts were not recorded");
		helper.assertTrue(!JugcraftRegions.rules().isEmpty(), "No region rules were read");
		Map<String, Integer> placed = new TreeMap<>();
		for (int layout = 0; layout < layouts.size(); layout++) {
			Map<String, Integer> counts = new TreeMap<>();
			for (Pair<Climate.ParameterPoint, ResourceKey<Biome>> entry : layouts.get(layout)) {
				if (JugcraftRegions.biomes().contains(entry.getSecond())) {
					counts.merge(entry.getSecond().identifier().getPath(), 1, Integer::sum);
					placed.merge(entry.getSecond().identifier().getPath(), 1, Integer::sum);
				}
			}
			LOGGER.info("Jugcraft layout {}: {} entries; Jugcraft biomes {}", layout, layouts.get(layout).size(), counts);
		}
		for (ResourceKey<Biome> biome : JugcraftRegions.biomes()) {
			helper.assertTrue(placed.getOrDefault(biome.identifier().getPath(), 0) > 0, "No climate entry became " + biome.identifier());
		}
		List<Pair<Climate.ParameterPoint, Holder<Biome>>> vanilla = overworldPreset(helper.getLevel()).value().parameters().values();
		Map<String, Integer> table = new TreeMap<>();
		List<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> entries = new ArrayList<>();
		for (Pair<Climate.ParameterPoint, Holder<Biome>> entry : vanilla) {
			ResourceKey<Biome> key = entry.getSecond().unwrapKey().orElseThrow();
			if (JugcraftRegions.biomes().contains(key)) {
				continue;
			}
			for (Pair<Climate.ParameterPoint, ResourceKey<Biome>> piece : JugcraftRegions.split(Pair.of(entry.getFirst(), key))) {
				entries.add(piece);
				table.merge(key.identifier().getPath() + " t" + JugcraftRegions.temperatureBand(piece.getFirst()) + " h"
						+ JugcraftRegions.humidityBand(piece.getFirst()) + " w" + JugcraftRegions.half(piece.getFirst().weirdness()), 1, Integer::sum);
			}
		}
		LOGGER.info("Vanilla Overworld table, cut at the band edges, by biome, temperature and humidity band, weirdness half: {}", table);
		List<String> idle = new ArrayList<>();
		for (JugcraftRegions.Rule rule : JugcraftRegions.rules()) {
			for (int layout = 0; layout < JugcraftRegions.LAYOUTS; layout++) {
				if ((rule.layouts() & 1 << layout) == 0) {
					continue;
				}
				int finalLayout = layout;
				long hits = entries.stream().filter(entry -> rule.matches(finalLayout, entry)
						&& JugcraftRegions.regional(finalLayout, entry).equals(rule.biome())).count();
				if (hits == 0) {
					idle.add(rule.biome().identifier().getPath() + " from " + rule.replaces().identifier().getPath() + " in layout " + layout);
				}
			}
		}
		helper.assertTrue(idle.isEmpty(), "Rules that place nothing: " + idle);
		for (ResourceKey<Biome> biome : JugcraftRegions.biomes()) {
			List<Pair<Climate.ParameterPoint, Holder<Biome>>> listed = vanilla.stream().filter(entry -> entry.getSecond().is(biome)).toList();
			helper.assertTrue(listed.size() == 1 && listed.get(0).getFirst().equals(JugcraftRegions.UNREACHABLE),
					biome.identifier() + " is listed in vanilla's table " + listed.size() + " times, not once at the unreachable climate");
		}
		helper.succeed();
	}

	/** About the configured share of the land is in Jugcraft regions, the same for a seed every time, and seeds differ. */
	@GameTest
	public void regionsSplitTheWorld(GameTestHelper helper) {
		long seed = JugcraftRegions.fingerprint(0x1234_5678_9ABCL);
		long next = JugcraftRegions.fingerprint(0x1234_5678_9ABDL);
		int inside = 0;
		int total = 0;
		int differ = 0;
		int[] byLayout = new int[JugcraftRegions.LAYOUTS];
		for (int x = -4096; x < 4096; x += 64) {
			for (int z = -4096; z < 4096; z += 64) {
				int layout = JugcraftRegions.regionOf(seed, x, z);
				helper.assertTrue(layout == JugcraftRegions.regionOf(seed, x, z), "Regions changed between lookups");
				helper.assertTrue(layout >= -1 && layout < JugcraftRegions.LAYOUTS, "No such layout: " + layout);
				if (layout >= 0) {
					inside++;
					byLayout[layout]++;
				}
				differ += layout != JugcraftRegions.regionOf(next, x, z) ? 1 : 0;
				total++;
			}
		}
		double share = (double) inside / total;
		LOGGER.info("Jugcraft regions: {} of {} samples ({}%) in a 128 km square, by layout {}; {} differ for the next seed",
				inside, total, Math.round(share * 100), java.util.Arrays.toString(byLayout), differ);
		for (int count : byLayout) {
			helper.assertTrue(count > inside / JugcraftRegions.LAYOUTS / 2, "A layout is rare: " + java.util.Arrays.toString(byLayout));
		}
		helper.assertTrue(share > JugcraftRegions.SHARE - 0.2 && share < JugcraftRegions.SHARE + 0.2,
				"Jugcraft regions cover " + share + " of the land, far from " + JugcraftRegions.SHARE);
		helper.assertTrue(differ > total / 5, "Another seed gives almost the same regions");
		helper.succeed();
	}

	// ---------------------------------------------------------------- trees

	/** Grows a tree on dirt from a sapling-like start; returns logs and leaves counted around it. */
	private static int[] grow(GameTestHelper helper, TreeGrower grower, BlockPos at, Block sapling, String log, String leaves) {
		ServerLevel level = helper.getLevel();
		helper.setBlock(at.below(), Blocks.DIRT);
		helper.setBlock(at, sapling);
		BlockPos absolute = helper.absolutePos(at);
		boolean grown = grower.growTree(level, level.getChunkSource().getGenerator(), absolute, level.getBlockState(absolute), level.getRandom());
		helper.assertTrue(grown, "The " + log + " tree did not grow");
		int logs = 0;
		int leaf = 0;
		for (BlockPos pos : BlockPos.betweenClosed(absolute.offset(-6, 0, -6), absolute.offset(6, 24, 6))) {
			BlockState state = level.getBlockState(pos);
			logs += state.is(block(log)) ? 1 : 0;
			leaf += leaves != null && state.is(block(leaves)) ? 1 : 0;
		}
		return new int[] {logs, leaf};
	}

	/** On top of whatever closes the test area above, so a tall tree has room. */
	private static BlockPos open(GameTestHelper helper, BlockPos start) {
		for (int dy = 0; dy <= 24; dy++) {
			if (!helper.getBlockState(start.above(dy)).isAir()) {
				return start.above(dy + 1);
			}
		}
		return start;
	}

	/** Maple, aspen, fir and willow saplings grow their trees; in autumn all but the fir come out in autumn colours. */
	@GameTest(maxTicks = 100)
	public void seasonalForestSaplingsGrow(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		SeasonCalendar.Settings before = JugcraftSeasons.settings();
		BlockPos at = open(helper, new BlockPos(3, 2, 3));
		String[][] trees = {{"maple", "maple_leaves"}, {"aspen", "aspen_leaves"}, {"fir", "fir_needles"}, {"willow", "willow_leaves"}};
		TreeGrower[] growers = {JugcraftAgriculture.MAPLE_GROWER, JugcraftAgriculture.ASPEN_GROWER, JugcraftAgriculture.FIR_GROWER,
				JugcraftAgriculture.WILLOW_GROWER};
		try {
			JugcraftSeasons.setMode(server, SeasonCalendar.Mode.AUTUMN);
			for (int i = 0; i < trees.length; i++) {
				String tree = trees[i][0];
				int[] counts = grow(helper, growers[i], at.offset(0, 0, 0), block(tree + "_sapling"), tree + "_log", trees[i][1]);
				int gold = 0;
				BlockPos absolute = helper.absolutePos(at);
				for (BlockPos pos : BlockPos.betweenClosed(absolute.offset(-6, 0, -6), absolute.offset(6, 24, 6))) {
					BlockState state = helper.getLevel().getBlockState(pos);
					if (state.is(block(trees[i][1])) && state.hasProperty(SeasonalLeavesBlock.SEASON)
							&& state.getValue(SeasonalLeavesBlock.SEASON) == SeasonalLeavesBlock.Foliage.GOLD) {
						gold++;
					}
				}
				LOGGER.info("A {} grown in autumn: {} logs, {} leaves ({} in autumn colours)", tree, counts[0], counts[1], gold);
				helper.assertTrue(counts[0] >= 4 && counts[1] >= 10, "A small " + tree + ": " + counts[0] + " logs, " + counts[1] + " leaves");
				helper.assertTrue(tree.equals("fir") ? gold == 0 : gold == counts[1], tree + ": " + gold + " of " + counts[1] + " leaves in autumn colours");
				for (BlockPos pos : BlockPos.betweenClosed(absolute.offset(-6, 0, -6), absolute.offset(6, 24, 6))) {
					helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
				}
			}
		} finally {
			JugcraftSeasons.setMode(server, before.mode());
		}
		helper.succeed();
	}

	/** A jacaranda sapling grows a forked trunk under a crown of blossom (fields and meadows). */
	@GameTest(maxTicks = 100)
	public void jacarandaSaplingsGrow(GameTestHelper helper) {
		BlockPos at = open(helper, new BlockPos(3, 2, 3));
		int[] counts = grow(helper, JugcraftAgriculture.JACARANDA_GROWER, at, block("jacaranda_sapling"), "jacaranda_log", "jacaranda_leaves");
		LOGGER.info("A jacaranda: {} logs, {} leaves", counts[0], counts[1]);
		helper.assertTrue(counts[0] >= 4 && counts[1] >= 10, "A small jacaranda: " + counts[0] + " logs, " + counts[1] + " leaves");
		helper.succeed();
	}

	/** Palm and cypress saplings (warm and dry lands) grow their trees. */
	@GameTest(maxTicks = 100)
	public void warmTreesGrow(GameTestHelper helper) {
		BlockPos at = open(helper, new BlockPos(3, 2, 3));
		String[][] trees = {{"palm", "palm_fronds"}, {"cypress", "cypress_leaves"}};
		TreeGrower[] growers = {JugcraftAgriculture.PALM_GROWER, JugcraftAgriculture.CYPRESS_GROWER};
		for (int i = 0; i < trees.length; i++) {
			int[] counts = grow(helper, growers[i], at, block(trees[i][0] + "_sapling"), trees[i][0] + "_log", trees[i][1]);
			LOGGER.info("A {}: {} logs, {} leaves", trees[i][0], counts[0], counts[1]);
			helper.assertTrue(counts[0] >= 4 && counts[1] >= 6, "A small " + trees[i][0] + ": " + counts[0] + " logs, " + counts[1] + " leaves");
			BlockPos absolute = helper.absolutePos(at);
			for (BlockPos pos : BlockPos.betweenClosed(absolute.offset(-6, 0, -6), absolute.offset(6, 24, 6))) {
				helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		helper.succeed();
	}

	/**
	 * The fewest logs each giant can grow, for bigTreesGrow. A trunk two blocks wide is four logs a level and one at its
	 * top: 4h - 3 for a trunk h tall. tools/trees.py's shortest trunks: a giant redwood's 22, a giant mahogany's 10 (its
	 * branches, a mega jungle tree's, may by chance be none).
	 */
	private static final Map<String, Integer> GIANT_FEWEST_LOGS = Map.of("redwood", 4 * 22 - 3, "mahogany", 4 * 10 - 3);

	/**
	 * Redwood, eucalyptus and mahogany saplings (big trees and rainforests) grow their trees; four redwood or mahogany
	 * saplings in a square grow a giant, its trunk two blocks wide.
	 */
	@GameTest(maxTicks = 100)
	public void bigTreesGrow(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = open(helper, new BlockPos(3, 2, 3));
		String[][] trees = {{"redwood", "redwood_needles"}, {"eucalyptus", "eucalyptus_leaves"}, {"mahogany", "mahogany_leaves"}};
		TreeGrower[] growers = {JugcraftAgriculture.REDWOOD_GROWER, JugcraftAgriculture.EUCALYPTUS_GROWER, JugcraftAgriculture.MAHOGANY_GROWER};
		for (int i = 0; i < trees.length; i++) {
			int[] counts = grow(helper, growers[i], at, block(trees[i][0] + "_sapling"), trees[i][0] + "_log", trees[i][1]);
			LOGGER.info("A {}: {} logs, {} leaves", trees[i][0], counts[0], counts[1]);
			helper.assertTrue(counts[0] >= 6 && counts[1] >= 10, "A small " + trees[i][0] + ": " + counts[0] + " logs, " + counts[1] + " leaves");
			clear(helper, at);
		}
		for (String tree : new String[] {"redwood", "mahogany"}) {
			Block sapling = block(tree + "_sapling");
			BlockPos absolute = helper.absolutePos(at);
			List<BlockPos> square = List.of(absolute, absolute.east(), absolute.south(), absolute.south().east());
			for (BlockPos pos : square) {
				level.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
				level.setBlock(pos, sapling.defaultBlockState().setValue(SaplingBlock.STAGE, 1), Block.UPDATE_ALL);
			}
			((SaplingBlock) sapling).advanceTree(level, absolute, level.getBlockState(absolute), level.getRandom());
			Block log = block(tree + "_log");
			boolean wide = square.stream().allMatch(pos -> level.getBlockState(pos).is(log));
			int logs = 0;
			for (BlockPos pos : BlockPos.betweenClosed(absolute.offset(-8, 0, -8), absolute.offset(9, 48, 9))) {
				logs += level.getBlockState(pos).is(log) ? 1 : 0;
			}
			LOGGER.info("A giant {} from four saplings: {} logs, trunk two wide: {}", tree, logs, wide);
			// Each giant is held to its own fewest: a giant redwood to 85 logs, a giant mahogany to 37 (CI has seen 37 and
			// 38). A lone sapling grows about 10.
			helper.assertTrue(wide && logs >= GIANT_FEWEST_LOGS.get(tree),
					"Four " + tree + " saplings grew no giant: " + logs + " logs (at least " + GIANT_FEWEST_LOGS.get(tree) + "), two wide " + wide);
			clear(helper, at);
		}
		helper.succeed();
	}

	/** Air in a box round a grown tree, big enough for a giant. */
	private static void clear(GameTestHelper helper, BlockPos at) {
		BlockPos absolute = helper.absolutePos(at);
		for (BlockPos pos : BlockPos.betweenClosed(absolute.offset(-8, 0, -8), absolute.offset(9, 48, 9))) {
			helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	// ---------------------------------------------------------------- the tree roster's batch 1

	/** One of the tree roster's batch-1 shapes: its log, its leaves ("" for none) and the height of its top log. */
	private record Shape(String id, String log, String leaves, int lowest, int highest) {
	}

	/**
	 * The tree roster's batch 1 (tools/trees.py; docs/features/trees-batch-1.md). Heights are of the top log above the
	 * soil, from vanilla's placers: a straight trunk's base + height_rand_a + height_rand_b exactly; a bending trunk's
	 * one more for its bend, and the fancy trunk's (the mossy maple's), loosely, to be narrowed from CI's logged sizes.
	 */
	private static final List<Shape> BATCH_ONE = List.of(
			new Shape("stunted_fir", "fir_log", "fir_needles", 4, 7),
			new Shape("bog_fir", "fir_log", "fir_needles", 4, 7),
			new Shape("subalpine_fir", "fir_log", "fir_needles", 10, 14),
			new Shape("fir_bush", "fir_log", "fir_needles", 1, 1),
			new Shape("tamarack", "larch_log", "larch_needles", 6, 9),
			new Shape("dead_snag", "dead_log", "", 3, 10),
			new Shape("dead_snag_bent", "dead_log", "", 4, 9),
			new Shape("willow_bush", "willow_log", "willow_leaves", 1, 2),
			new Shape("young_aspen", "aspen_log", "aspen_leaves", 4, 8),
			new Shape("cedar", "cedar_log", "cedar_leaves", 7, 10),
			new Shape("mossy_maple", "maple_log", "maple_leaves", 4, 16));

	private static PlacedFeature placedFeature(ServerLevel level, String id) {
		return level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE)
				.getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, Jugcraft.id(id))).value();
	}

	/** Air round a placed tree, and `soil` under its trunk. */
	private static void reset(ServerLevel level, BlockPos absolute, Block soil) {
		for (BlockPos pos : BlockPos.betweenClosed(absolute.offset(-8, 0, -8), absolute.offset(8, 24, 8))) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
		level.setBlock(absolute.below(), soil.defaultBlockState(), Block.UPDATE_ALL);
	}

	/**
	 * Every batch-1 shape, placed through its "_checked" feature on dirt from four seeds in autumn: its own logs reach the
	 * height its trunk placer gives; the leafy shapes have their leaves (all in autumn colours where they are seasonal,
	 * from the seasonal_leaves decorator), the snags none at all; the mossy maple lays moss carpet on its limbs and vines.
	 */
	@GameTest(maxTicks = 200)
	public void batchOneShapesGrow(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		SeasonCalendar.Settings before = JugcraftSeasons.settings();
		BlockPos absolute = helper.absolutePos(open(helper, new BlockPos(3, 2, 3)));
		int moss = 0;
		int vines = 0;
		try {
			JugcraftSeasons.setMode(server, SeasonCalendar.Mode.AUTUMN);
			for (Shape shape : BATCH_ONE) {
				PlacedFeature placed = placedFeature(level, shape.id() + "_checked");
				Block log = block(shape.log());
				Block leaves = shape.leaves().isEmpty() ? null : block(shape.leaves());
				boolean seasonal = leaves != null && leaves.defaultBlockState().hasProperty(SeasonalLeavesBlock.SEASON);
				List<String> sizes = new ArrayList<>();
				for (long seed = 1; seed <= 4; seed++) {
					reset(level, absolute, Blocks.DIRT);
					placed.place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), absolute);
					int top = 0;
					int logs = 0;
					int leaf = 0;
					int gold = 0;
					int anyLeaves = 0;
					for (BlockPos pos : BlockPos.betweenClosed(absolute.offset(-8, 0, -8), absolute.offset(8, 24, 8))) {
						BlockState state = level.getBlockState(pos);
						if (state.is(log)) {
							logs++;
							top = Math.max(top, pos.getY() - absolute.getY() + 1);
						}
						anyLeaves += state.is(BlockTags.LEAVES) ? 1 : 0;
						if (leaves != null && state.is(leaves)) {
							leaf++;
							gold += state.hasProperty(SeasonalLeavesBlock.SEASON)
									&& state.getValue(SeasonalLeavesBlock.SEASON) == SeasonalLeavesBlock.Foliage.GOLD ? 1 : 0;
						}
						if (shape.id().equals("mossy_maple")) {
							moss += state.is(Blocks.MOSS_CARPET) ? 1 : 0;
							vines += state.is(Blocks.VINE) ? 1 : 0;
						}
					}
					sizes.add(logs + " logs to " + top + ", " + leaf + " leaves");
					helper.assertTrue(top >= shape.lowest() && top <= shape.highest(),
							shape.id() + " (seed " + seed + "): its top log is " + top + " up, not " + shape.lowest() + "-" + shape.highest());
					if (leaves == null) {
						helper.assertTrue(anyLeaves == 0, shape.id() + " (seed " + seed + ") has " + anyLeaves + " leaves");
					} else {
						helper.assertTrue(leaf >= 4, shape.id() + " (seed " + seed + ") has only " + leaf + " leaves");
						helper.assertTrue(seasonal ? gold == leaf : gold == 0,
								shape.id() + " (seed " + seed + "): " + gold + " of " + leaf + " leaves in autumn colours");
					}
				}
				LOGGER.info("[trees] {}: {}", shape.id(), String.join("; ", sizes));
			}
		} finally {
			JugcraftSeasons.setMode(server, before.mode());
		}
		reset(level, absolute, Blocks.DIRT);
		LOGGER.info("[trees] mossy_maple, four trees: {} moss carpets, {} vines", moss, vines);
		helper.assertTrue(moss > 0 && vines > 0, "Four mossy maples laid " + moss + " moss carpets and " + vines + " vines");
		helper.succeed();
	}

	/**
	 * A batch-1 shape stands only where its sapling could: on stone none places a log; the dead snags, which stand where
	 * an oak sapling could, grow on coarse dirt (the Wasteland's, Cinder Barrens' and Rotted Expanse's tree ground) but
	 * not on calcite or tuff (their bare floors).
	 */
	@GameTest(maxTicks = 100)
	public void batchOneShapesNeedTheirSoil(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos absolute = helper.absolutePos(open(helper, new BlockPos(3, 2, 3)));
		for (Shape shape : BATCH_ONE) {
			Block[] soils = shape.log().equals("dead_log")
					? new Block[] {Blocks.STONE, Blocks.CALCITE, Blocks.TUFF, Blocks.COARSE_DIRT}
					: new Block[] {Blocks.STONE};
			for (Block soil : soils) {
				reset(level, absolute, soil);
				placedFeature(level, shape.id() + "_checked").place(level, level.getChunkSource().getGenerator(), RandomSource.create(7), absolute);
				int logs = 0;
				for (BlockPos pos : BlockPos.betweenClosed(absolute.offset(-8, 0, -8), absolute.offset(8, 24, 8))) {
					logs += level.getBlockState(pos).is(block(shape.log())) ? 1 : 0;
				}
				boolean grows = soil == Blocks.COARSE_DIRT;
				helper.assertTrue(grows ? logs > 0 : logs == 0, shape.id() + " on " + soil + ": " + logs + " logs");
			}
		}
		reset(level, absolute, Blocks.DIRT);
		helper.succeed();
	}

	/**
	 * Larch joined the fallen logs (tools/trees.py FALLEN): on a flat dirt floor, fallen_larch_tree leaves a larch stump
	 * and a larch log lying along the ground. Its log starts 2-3 blocks from the stump and lies at most 6 blocks long (log
	 * length 5-8, less 2), so it ends within 8 blocks: the box stays within the -8..+9 every other test here uses.
	 */
	@GameTest(maxTicks = 100)
	public void fallenLarchesLie(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos absolute = helper.absolutePos(open(helper, new BlockPos(3, 2, 3)));
		Block log = block("larch_log");
		for (BlockPos pos : BlockPos.betweenClosed(absolute.offset(-8, 0, -8), absolute.offset(8, 6, 8))) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
		for (BlockPos pos : BlockPos.betweenClosed(absolute.offset(-8, -1, -8), absolute.offset(8, -1, 8))) {
			level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
		placedFeature(level, "fallen_larch_tree").place(level, level.getChunkSource().getGenerator(), RandomSource.create(3), absolute);
		int lying = 0;
		for (BlockPos pos : BlockPos.betweenClosed(absolute.offset(-8, 0, -8), absolute.offset(8, 6, 8))) {
			BlockState state = level.getBlockState(pos);
			lying += state.is(log) && state.getValue(RotatedPillarBlock.AXIS) != Direction.Axis.Y ? 1 : 0;
		}
		boolean stump = level.getBlockState(absolute).is(log);
		LOGGER.info("[trees] A fallen larch: stump {}, {} lying logs", stump, lying);
		helper.assertTrue(stump && lying >= 3, "A fallen larch: stump " + stump + ", " + lying + " lying logs");
		for (BlockPos pos : BlockPos.betweenClosed(absolute.offset(-8, 0, -8), absolute.offset(8, 6, 8))) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
		helper.succeed();
	}

	/** The cedar (the tree roster's batch 1, a wood of its own): its sapling grows a cedar of cedar logs and leaves. */
	@GameTest(maxTicks = 100)
	public void cedarSaplingsGrow(GameTestHelper helper) {
		BlockPos at = open(helper, new BlockPos(3, 2, 3));
		int[] counts = grow(helper, JugcraftAgriculture.CEDAR_GROWER, at, block("cedar_sapling"), "cedar_log", "cedar_leaves");
		LOGGER.info("A cedar: {} logs, {} leaves", counts[0], counts[1]);
		helper.assertTrue(counts[0] >= 7 && counts[0] <= 10 && counts[1] >= 10, "A cedar of " + counts[0] + " logs and " + counts[1] + " leaves");
		clear(helper, at);
		helper.succeed();
	}

	/** Any axe strips a cedar log; cedar logs burn, its planks are planks, and its sapling and evergreen leaves are in vanilla's tags. */
	@GameTest
	public void cedarWoodWorksLikeWood(GameTestHelper helper) {
		BlockPos pos = new BlockPos(1, 2, 1);
		helper.setBlock(pos, block("cedar_log").defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
		helper.useBlock(pos, player);
		BlockState stripped = helper.getBlockState(pos);
		helper.assertTrue(stripped.is(block("stripped_cedar_log")) && stripped.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Z,
				"An axe should strip the cedar log along its axis, found " + stripped);
		// Vanilla's #minecraft:logs_that_burn by name (26.3's BlockTags has no constant of that name).
		TagKey<Block> logsThatBurn = TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace("logs_that_burn"));
		helper.assertTrue(block("cedar_log").defaultBlockState().is(logsThatBurn), "Cedar logs do not burn");
		helper.assertTrue(new ItemStack(JugcraftAgriculture.item("cedar_planks")).is(ItemTags.PLANKS), "Cedar planks are not planks");
		helper.assertTrue(block("cedar_sapling").defaultBlockState().is(BlockTags.SAPLINGS), "The cedar sapling is not a sapling");
		BlockState leaves = block("cedar_leaves").defaultBlockState();
		helper.assertTrue(leaves.is(BlockTags.LEAVES) && !leaves.hasProperty(SeasonalLeavesBlock.SEASON), "Cedar leaves are not evergreen leaves");
		helper.succeed();
	}

	/**
	 * TREES.md rule 4: jugcraft:feature_enabled's "or" loads a recipe when any of its switches is on, so the larch's and
	 * the chestnut's recipes (grown by two switches each) and the cedar's load.
	 */
	@GameTest
	public void woodRecipesFollowAnyOfTheirSwitches(GameTestHelper helper) {
		helper.assertTrue(new FeatureEnabledCondition("no_such_switch", List.of("biomes")).test(null) == JugcraftConfig.isFeatureEnabled("biomes"),
				"An \"or\" switch does not load the recipe");
		helper.assertTrue(!new FeatureEnabledCondition("no_such_switch", List.of("no_other_switch")).test(null),
				"No switch on, yet the recipe loads");
		ServerLevel level = helper.getLevel();
		for (String recipe : new String[] {"larch_planks", "larch_fence", "sawing/larch_logs", "chestnut_planks", "sawing/chestnut_logs",
				"cedar_planks", "cedar_wood", "stripped_cedar_wood", "cedar_stairs", "cedar_slab", "cedar_fence", "cedar_fence_gate",
				"sawing/cedar_logs", "tree_growing/cedar_sapling"}) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(recipe))).isPresent(), recipe + " loads");
		}
		helper.succeed();
	}

	/**
	 * The biomes whose tree lists the tree roster's batch 1 changed (tools/biomes.py; docs/features/trees-batch-1.md): all
	 * but the last are Jugcraft regions' Overworld biomes; the Rotted Expanse is an End biome.
	 */
	private static final List<String> BATCH_ONE_BIOMES = List.of("coniferous_forest", "snowy_coniferous_forest", "maple_woods",
			"seasonal_forest", "aspen_glade", "dead_forest", "tundra", "snowy_forest", "muskeg", "bog", "dead_swamp", "lush_swamp",
			"swamp_woods", "floodplain", "ghost_forest", "lush_river", "fen", "lake_district", "wetland", "wasteland", "burnt_forest",
			"dense_forest", "redwood_forest", "temperate_rainforest", "shield", "cinder_barrens", "hallowed_bog", "snowpetal_grove",
			"rotted_expanse");

	/**
	 * Every biome batch 1 changed places its own tree list ({@code jugcraft:trees_<biome>}), in the vegetation step and no other:
	 * the Cinder Barrens' list is new (it had no trees), the others' changed. Each is placed in its dimension, a Jugcraft
	 * region's Overworld biome or (the Rotted Expanse) an End biome, so the feature order tests sort its features too
	 * (PixelHollowsGameTests.overworldFeatureOrderHasNoCycle, dimensionBiomesArePlaced).
	 */
	@GameTest
	public void batchOneBiomesPlaceTheirTrees(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Registry<Biome> biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
		Registry<PlacedFeature> features = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
		int vegetation = GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
		for (String name : BATCH_ONE_BIOMES) {
			ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, Jugcraft.id(name));
			Holder<PlacedFeature> trees = features.getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, Jugcraft.id("trees_" + name)));
			var steps = biomes.getOrThrow(key).value().getGenerationSettings().features();
			List<Integer> found = new ArrayList<>();
			for (int step = 0; step < steps.size(); step++) {
				if (steps.get(step).contains(trees)) {
					found.add(step);
				}
			}
			helper.assertTrue(found.equals(List.of(vegetation)),
					name + " places jugcraft:trees_" + name + " in steps " + found + ", not in the vegetation step (" + vegetation + ") alone");
			boolean placed = name.equals("rotted_expanse") ? JugcraftDimensions.end().contains(key) : JugcraftRegions.biomes().contains(key);
			helper.assertTrue(placed, name + " is not placed in " + (name.equals("rotted_expanse") ? "the End" : "Jugcraft's regions"));
		}
		LOGGER.info("[trees] {} batch-1 biomes place their own trees in step {}", BATCH_ONE_BIOMES.size(), vegetation);
		helper.succeed();
	}

	/** The tropical plants (batch 5): hibiscus is a small flower with a potted form; a hydrangea takes two blocks and drops one. */
	@GameTest
	public void tropicalPlantsWork(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = new BlockPos(1, 2, 1);
		helper.setBlock(pos.below(), Blocks.GRASS_BLOCK);
		Block hibiscus = block("hibiscus");
		helper.assertTrue(hibiscus instanceof FlowerBlock, "Hibiscus is not a flower");
		helper.setBlock(pos, hibiscus);
		helper.assertTrue(helper.getBlockState(pos).canSurvive(level, helper.absolutePos(pos)), "Hibiscus cannot stand on grass");
		helper.assertTrue(block("potted_hibiscus") instanceof FlowerPotBlock pot && pot.getPotted() == hibiscus, "No potted hibiscus");
		BlockPos tall = new BlockPos(4, 2, 4);
		helper.setBlock(tall.below(), Blocks.GRASS_BLOCK);
		Block hydrangea = block("hydrangea");
		DoublePlantBlock.placeAt(level, hydrangea.defaultBlockState(), helper.absolutePos(tall), Block.UPDATE_ALL);
		helper.assertBlockPresent(hydrangea, tall.above());
		level.destroyBlock(helper.absolutePos(tall), true);
		helper.runAfterDelay(2, () -> {
			int drops = dropped(helper, hydrangea);
			LOGGER.info("Tropical plants: a broken hydrangea dropped {}", drops);
			helper.assertTrue(drops == 1, "A hydrangea dropped " + drops);
			helper.assertBlockNotPresent(hydrangea, tall.above());
			helper.succeed();
		});
	}

	/** Sea oats (batch 6) stand on sand as well as soil, but not on stone; they take two blocks and drop one. */
	@GameTest
	public void seaOatsGrowOnSand(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Block oats = block("sea_oats");
		BlockPos sand = new BlockPos(1, 2, 1);
		helper.setBlock(sand.below(), Blocks.SAND);
		helper.assertTrue(oats.defaultBlockState().canSurvive(level, helper.absolutePos(sand)), "Sea oats cannot stand on sand");
		BlockPos stone = new BlockPos(3, 2, 1);
		helper.setBlock(stone.below(), Blocks.STONE);
		helper.assertFalse(oats.defaultBlockState().canSurvive(level, helper.absolutePos(stone)), "Sea oats stand on stone");
		DoublePlantBlock.placeAt(level, oats.defaultBlockState(), helper.absolutePos(sand), Block.UPDATE_ALL);
		helper.assertBlockPresent(oats, sand.above());
		level.destroyBlock(helper.absolutePos(sand), true);
		helper.runAfterDelay(2, () -> {
			int drops = dropped(helper, oats);
			LOGGER.info("Sea oats: on sand, not on stone; broken, dropped {}", drops);
			helper.assertTrue(drops == 1, "Sea oats dropped " + drops);
			helper.succeed();
		});
	}

	/**
	 * The wonders' plants (batch 7): glowcaps glow and stand on bare stone; the glimmerbloom glows and the frost iris
	 * does not, both small flowers with potted forms; a patch of four snowpetals drops four.
	 */
	@GameTest
	public void glowingPlantsWork(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Block glowcap = block("glowcap");
		BlockPos stone = new BlockPos(1, 2, 1);
		helper.setBlock(stone.below(), Blocks.STONE);
		helper.assertTrue(glowcap.defaultBlockState().canSurvive(level, helper.absolutePos(stone)), "Glowcaps cannot stand on stone");
		helper.setBlock(stone, glowcap);
		helper.assertTrue(helper.getBlockState(stone).getLightEmission() == 10, "Glowcaps give light " + helper.getBlockState(stone).getLightEmission());
		String[] flowers = {"glimmerbloom", "frost_iris"};
		int[] light = {7, 0};
		for (int i = 0; i < flowers.length; i++) {
			BlockPos pos = new BlockPos(3 + i * 2, 2, 1);
			helper.setBlock(pos.below(), Blocks.GRASS_BLOCK);
			Block flower = block(flowers[i]);
			helper.assertTrue(flower instanceof FlowerBlock, flowers[i] + " is not a flower");
			helper.setBlock(pos, flower);
			helper.assertTrue(helper.getBlockState(pos).getLightEmission() == light[i], flowers[i] + " gives light "
					+ helper.getBlockState(pos).getLightEmission());
			helper.assertTrue(block("potted_" + flowers[i]) instanceof FlowerPotBlock pot && pot.getPotted() == flower, "No potted " + flowers[i]);
		}
		BlockPos bed = new BlockPos(4, 2, 4);
		helper.setBlock(bed.below(), Blocks.GRASS_BLOCK);
		Block snowpetals = block("snowpetals");
		helper.setBlock(bed, snowpetals.defaultBlockState().setValue(BlockStateProperties.FLOWER_AMOUNT, 4));
		level.destroyBlock(helper.absolutePos(bed), true);
		helper.runAfterDelay(2, () -> {
			int petals = dropped(helper, snowpetals);
			LOGGER.info("Glowing plants: glowcap light 10 on stone, glimmerbloom light 7; snowpetals of 4 dropped {}", petals);
			helper.assertTrue(petals == 4, "A snowpetal patch of four dropped " + petals);
			helper.succeed();
		});
	}

	/**
	 * The Nether and End biomes (batches 8 and 9). The game test server's Nether and End have a single fixed biome, so
	 * this builds their biome sources as a real world's are built (vanilla's Nether preset, {@code TheEndBiomeSource.create},
	 * which Fabric's biome API extends): every Jugcraft biome is among their biomes, and their features sort into one
	 * order (no feature order cycle). Where they are found in a real world is the client test's ({@code BiomeClientGameTests}).
	 */
	@GameTest
	public void dimensionBiomesArePlaced(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HolderGetter<Biome> biomeLookup = level.registryAccess().lookupOrThrow(Registries.BIOME);
		BiomeSource nether = MultiNoiseBiomeSource.createFromPreset(level.registryAccess()
				.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).getOrThrow(MultiNoiseBiomeSourceParameterLists.NETHER));
		BiomeSource end = TheEndBiomeSource.create(biomeLookup);
		Map<String, BiomeSource> sources = new LinkedHashMap<>();
		sources.put("nether", nether);
		sources.put("end", end);
		for (Map.Entry<String, BiomeSource> dimension : sources.entrySet()) {
			String name = dimension.getKey();
			List<ResourceKey<Biome>> biomes = name.equals("nether") ? JugcraftDimensions.nether() : JugcraftDimensions.end();
			helper.assertTrue(!biomes.isEmpty(), "No Jugcraft " + name + " biomes were placed");
			List<Holder<Biome>> possible = List.copyOf(dimension.getValue().possibleBiomes());
			for (ResourceKey<Biome> biome : biomes) {
				helper.assertTrue(possible.stream().anyMatch(holder -> holder.is(biome)), biome.identifier() + " is not among the " + name + "'s biomes");
			}
			FeatureSorter.buildFeaturesPerStep(possible, holder -> holder.value().getGenerationSettings().features(), true);
			LOGGER.info("The {}: {} biomes, with all {} of Jugcraft's; their features sort into one order", name, possible.size(), biomes.size());
		}
		helper.succeed();
	}

	private static int dropped(GameTestHelper helper, Block block) {
		AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16);
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(block.asItem()))
				.stream().mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	/**
	 * The wild plants (tools/plants.py) work like vanilla's: small flowers stand on grass and have a potted form and an
	 * item; tall lavender takes two blocks and drops itself once; a clover patch of four clumps drops four.
	 */
	@GameTest
	public void wildPlantsWork(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		String[] flowers = {"lavender", "goldenrod", "heather", "orange_cosmos"};
		for (int i = 0; i < flowers.length; i++) {
			BlockPos pos = new BlockPos(1 + i * 2, 2, 1);
			helper.setBlock(pos.below(), Blocks.GRASS_BLOCK);
			Block flower = block(flowers[i]);
			helper.assertTrue(flower instanceof FlowerBlock, flowers[i] + " is not a flower");
			helper.setBlock(pos, flower);
			helper.assertTrue(helper.getBlockState(pos).canSurvive(level, helper.absolutePos(pos)), flowers[i] + " cannot stand on grass");
			helper.assertTrue(block("potted_" + flowers[i]) instanceof FlowerPotBlock pot && pot.getPotted() == flower, "No potted " + flowers[i]);
			helper.assertTrue(flower.asItem() != Items.AIR, flowers[i] + " has no item");
		}
		BlockPos tall = new BlockPos(1, 2, 4);
		helper.setBlock(tall.below(), Blocks.GRASS_BLOCK);
		Block tallLavender = block("tall_lavender");
		DoublePlantBlock.placeAt(level, tallLavender.defaultBlockState(), helper.absolutePos(tall), Block.UPDATE_ALL);
		helper.assertBlockPresent(tallLavender, tall.above());
		level.destroyBlock(helper.absolutePos(tall), true);
		BlockPos bed = new BlockPos(4, 2, 4);
		helper.setBlock(bed.below(), Blocks.GRASS_BLOCK);
		Block clover = block("clover");
		helper.setBlock(bed, clover.defaultBlockState().setValue(BlockStateProperties.FLOWER_AMOUNT, 4));
		level.destroyBlock(helper.absolutePos(bed), true);
		helper.runAfterDelay(2, () -> {
			int lavender = dropped(helper, tallLavender);
			int clovers = dropped(helper, clover);
			LOGGER.info("Wild plants: tall lavender dropped {}, a clover patch of 4 dropped {}", lavender, clovers);
			helper.assertTrue(lavender == 1, "Tall lavender dropped " + lavender);
			helper.assertTrue(clovers == 4, "A clover patch of four dropped " + clovers);
			helper.succeed();
		});
	}

	/**
	 * The wetland plants (batch 3) work like their vanilla models: watergrass stands only in water, on a solid floor; duckweed
	 * floats only on water; a cattail takes two blocks and drops itself once.
	 */
	@GameTest
	public void wetlandPlantsWork(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Block watergrass = block("watergrass");
		BlockPos wet = new BlockPos(1, 2, 1);
		helper.setBlock(wet.below(), Blocks.SAND);
		helper.setBlock(wet, Blocks.WATER);
		helper.assertTrue(watergrass.defaultBlockState().canSurvive(level, helper.absolutePos(wet)), "Watergrass cannot stand in water on sand");
		BlockPos dry = new BlockPos(3, 2, 1);
		helper.setBlock(dry.below(), Blocks.SAND);
		helper.assertTrue(!watergrass.defaultBlockState().canSurvive(level, helper.absolutePos(dry)), "Watergrass stands in air");
		helper.setBlock(wet, watergrass);
		helper.assertTrue(level.getFluidState(helper.absolutePos(wet)).isSource(), "Watergrass does not hold its water");
		Block duckweed = block("duckweed");
		BlockPos pond = new BlockPos(5, 2, 1);
		helper.setBlock(pond, Blocks.WATER);
		helper.assertTrue(duckweed.defaultBlockState().canSurvive(level, helper.absolutePos(pond.above())), "Duckweed cannot float on water");
		helper.assertTrue(!duckweed.defaultBlockState().canSurvive(level, helper.absolutePos(dry.above())), "Duckweed floats on sand");
		BlockPos tall = new BlockPos(1, 2, 4);
		helper.setBlock(tall.below(), Blocks.GRASS_BLOCK);
		Block cattail = block("cattail");
		DoublePlantBlock.placeAt(level, cattail.defaultBlockState(), helper.absolutePos(tall), Block.UPDATE_ALL);
		helper.assertBlockPresent(cattail, tall.above());
		level.destroyBlock(helper.absolutePos(tall), true);
		helper.runAfterDelay(2, () -> {
			int cattails = dropped(helper, cattail);
			LOGGER.info("Wetland plants: watergrass and duckweed stand where they should; a cattail dropped {}", cattails);
			helper.assertTrue(cattails == 1, "A cattail dropped " + cattails);
			helper.succeed();
		});
	}

	/** The dead tree (no sapling grows it) is a bare trunk of dead wood with no leaves at all. */
	@GameTest
	public void deadTreesAreBare(GameTestHelper helper) {
		BlockPos at = open(helper, new BlockPos(3, 2, 3));
		int[] counts = grow(helper, JugcraftAgriculture.DEAD_TREE_GROWER, at, Blocks.OAK_SAPLING, "dead_log", null);
		int leaves = 0;
		BlockPos absolute = helper.absolutePos(at);
		for (BlockPos pos : BlockPos.betweenClosed(absolute.offset(-6, 0, -6), absolute.offset(6, 24, 6))) {
			leaves += helper.getLevel().getBlockState(pos).is(BlockTags.LEAVES) ? 1 : 0;
		}
		LOGGER.info("A dead tree: {} dead logs, {} leaves", counts[0], leaves);
		helper.assertTrue(counts[0] >= 4 && leaves == 0, "A dead tree of " + counts[0] + " logs and " + leaves + " leaves");
		helper.succeed();
	}

	/** Every seasonal tree's leaves are green in spring and summer, in autumn colours in autumn and bare in winter. */
	@GameTest
	public void seasonalLeavesFollowTheModes(GameTestHelper helper) {
		Map<String, SeasonalLeavesBlock.Schedule> schedules = Map.of("larch", JugcraftAgriculture.LARCH_LEAVES,
				"maple", JugcraftAgriculture.MAPLE_LEAVES, "aspen", JugcraftAgriculture.ASPEN_LEAVES);
		Map<SeasonCalendar.Mode, SeasonalLeavesBlock.Foliage> wanted = Map.of(SeasonCalendar.Mode.SPRING, SeasonalLeavesBlock.Foliage.GREEN,
				SeasonCalendar.Mode.SUMMER, SeasonalLeavesBlock.Foliage.GREEN, SeasonCalendar.Mode.AUTUMN, SeasonalLeavesBlock.Foliage.GOLD,
				SeasonCalendar.Mode.WINTER, SeasonalLeavesBlock.Foliage.BARE, SeasonCalendar.Mode.OFF, SeasonalLeavesBlock.Foliage.GREEN);
		for (Map.Entry<String, SeasonalLeavesBlock.Schedule> tree : schedules.entrySet()) {
			for (Map.Entry<SeasonCalendar.Mode, SeasonalLeavesBlock.Foliage> mode : wanted.entrySet()) {
				for (BlockPos pos : BlockPos.betweenClosed(0, 60, 0, 15, 61, 15)) {
					SeasonalLeavesBlock.Foliage look = tree.getValue().on(mode.getKey().day, pos);
					helper.assertTrue(look == mode.getValue(), tree.getKey() + " leaves at " + pos + " in " + mode.getKey() + ": " + look);
				}
			}
		}
		helper.succeed();
	}

	/** Any axe strips the new woods' logs; their planks, saplings and leaves are in vanilla's tags. */
	@GameTest
	public void seasonalForestWoodsWorkLikeWood(GameTestHelper helper) {
		BlockPos pos = new BlockPos(1, 2, 1);
		for (String wood : new String[] {"maple", "aspen", "fir", "dead"}) {
			helper.setBlock(pos, block(wood + "_log").defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
			Player player = helper.makeMockPlayer(GameType.SURVIVAL);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
			helper.useBlock(pos, player);
			BlockState stripped = helper.getBlockState(pos);
			helper.assertTrue(stripped.is(block("stripped_" + wood + "_log")) && stripped.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Z,
					"An axe should strip the " + wood + " log along its axis, found " + stripped);
			helper.assertTrue(new ItemStack(JugcraftAgriculture.item(wood + "_planks")).is(ItemTags.PLANKS), wood + " planks are not planks");
		}
		for (String tree : new String[] {"maple", "aspen", "fir"}) {
			helper.assertTrue(block(tree + "_sapling").defaultBlockState().is(BlockTags.SAPLINGS), tree + " sapling is not a sapling");
		}
		for (String leaves : new String[] {"maple_leaves", "aspen_leaves", "fir_needles"}) {
			helper.assertTrue(block(leaves).defaultBlockState().is(BlockTags.LEAVES), leaves + " are not leaves");
		}
		helper.succeed();
	}
}
