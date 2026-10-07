package io.github.jimbozoomer.jugcraft.test;

import com.mojang.datafixers.util.Pair;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.SeasonalLeavesBlock;
import io.github.jimbozoomer.jugcraft.season.JugcraftSeasons;
import io.github.jimbozoomer.jugcraft.season.SeasonCalendar;
import io.github.jimbozoomer.jugcraft.world.AlpineSpawn;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Alpine Spawn: its place in the Overworld climate table, its tags and seasons, its villages, and its larches. */
public class AlpineGameTests {
	private static final Logger LOGGER = LoggerFactory.getLogger("jugcraft-test");

	private static Holder<MultiNoiseBiomeSourceParameterList> overworldPreset(ServerLevel level) {
		return level.registryAccess().lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
				.getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD);
	}

	/** Vanilla's meadows: 100 cool and 80 temperate climate entries in 26.3 (part 1's CI log). */
	private static final int VANILLA_MEADOWS = 180;

	/**
	 * Every meadow became Alpine Spawn, and so did the cool plateau's forest and taiga, so Alpine Spawn has more entries
	 * than vanilla had meadows; forest and taiga elsewhere remain.
	 */
	@GameTest
	public void alpineSpawnTakesOverMeadowsAndCoolPlateaus(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<Pair<Climate.ParameterPoint, Holder<Biome>>> table = overworldPreset(level).value().parameters().values();
		long alpine = table.stream().filter(entry -> entry.getSecond().is(AlpineSpawn.BIOME)).count();
		long meadows = table.stream().filter(entry -> entry.getSecond().is(Biomes.MEADOW)).count();
		long forests = table.stream().filter(entry -> entry.getSecond().is(Biomes.FOREST)).count();
		long taigas = table.stream().filter(entry -> entry.getSecond().is(Biomes.TAIGA)).count();
		LOGGER.info("Overworld climate table: {} entries, {} Alpine Spawn, {} meadows left, {} forest and {} taiga left",
				table.size(), alpine, meadows, forests, taigas);
		helper.assertTrue(meadows == 0, meadows + " meadows are left");
		helper.assertTrue(alpine > VANILLA_MEADOWS, "Alpine Spawn has " + alpine + " entries, no more than vanilla's "
				+ VANILLA_MEADOWS + " meadows: the cool plateau's forest and taiga were not taken");
		helper.assertTrue(forests > 0 && taigas > 0, "Lowland forest or taiga is gone too");
		Climate.ParameterPoint cool = Climate.parameters(Climate.Parameter.span(-0.45F, -0.15F), Climate.Parameter.span(-1.0F, 1.0F),
				Climate.Parameter.span(0.0F, 1.0F), Climate.Parameter.span(-1.0F, 1.0F), Climate.Parameter.point(0.0F),
				Climate.Parameter.span(-1.0F, 1.0F), 0.0F);
		Climate.ParameterPoint temperate = Climate.parameters(Climate.Parameter.span(-0.15F, 0.2F), Climate.Parameter.span(-1.0F, 1.0F),
				Climate.Parameter.span(0.0F, 1.0F), Climate.Parameter.span(-1.0F, 1.0F), Climate.Parameter.point(0.0F),
				Climate.Parameter.span(-1.0F, 1.0F), 0.0F);
		helper.assertTrue(AlpineSpawn.replaces(Pair.of(cool, Biomes.MEADOW)), "A cool meadow is not replaced");
		helper.assertTrue(AlpineSpawn.replaces(Pair.of(temperate, Biomes.MEADOW)), "A temperate meadow is not replaced");
		helper.assertTrue(!AlpineSpawn.replaces(Pair.of(cool, Biomes.FOREST)), "Every cool forest is replaced, lowland ones too");
		helper.assertTrue(AlpineSpawn.takesPlateau(1, 2) && AlpineSpawn.takesPlateau(1, 3), "The cool plateau's forest or taiga is not taken");
		helper.assertTrue(!AlpineSpawn.takesPlateau(1, 4) && !AlpineSpawn.takesPlateau(2, 2) && !AlpineSpawn.takesPlateau(0, 3),
				"Plateau cells beyond the cool forest and taiga are taken");
		helper.succeed();
	}

	@GameTest
	public void alpineSpawnHasSeasonsAndMountainTags(GameTestHelper helper) {
		Registry<Biome> biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
		Holder<Biome> alpine = biomes.getOrThrow(AlpineSpawn.BIOME);
		helper.assertTrue(alpine.is(JugcraftSeasons.HAS_SEASONS), "Alpine Spawn has no seasonal colours");
		helper.assertTrue(alpine.is(JugcraftSeasons.HAS_WINTER_SNOW), "Alpine Spawn has no winter snow");
		helper.assertTrue(alpine.is(BiomeTags.IS_OVERWORLD) && alpine.is(BiomeTags.IS_MOUNTAIN), "Alpine Spawn is not an Overworld mountain biome");
		helper.assertTrue(alpine.value().getBaseTemperature() < biomes.getOrThrow(Biomes.MEADOW).value().getBaseTemperature(),
				"Alpine Spawn is not cooler than a meadow");
		helper.succeed();
	}

	/** Alpine villages: a village structure in Alpine Spawn only, on a grid much tighter than vanilla's. */
	@GameTest
	public void alpineVillagesAreCommon(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
				.getOrThrow(ResourceKey.create(Registries.STRUCTURE, Jugcraft.id("village_alpine")));
		helper.assertTrue(structure.is(StructureTags.VILLAGE), "The alpine village is not in #minecraft:village");
		helper.assertTrue(structure.value().biomes().contains(level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(AlpineSpawn.BIOME)),
				"The alpine village does not generate in Alpine Spawn");
		StructureSet set = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET)
				.getOrThrow(ResourceKey.create(Registries.STRUCTURE_SET, Jugcraft.id("alpine_villages"))).value();
		helper.assertTrue(set.placement() instanceof RandomSpreadStructurePlacement spread && spread.spacing() < 34,
				"Alpine villages are no closer together than vanilla's: " + set.placement());
		helper.assertTrue(structure.is(AlpineSpawn.VILLAGES), "The start search does not look for the alpine village");
		helper.succeed();
	}

	// ---------------------------------------------------------------- the larch

	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	private static SeasonalLeavesBlock.Foliage needles(GameTestHelper helper, BlockPos pos) {
		return helper.getBlockState(pos).getValue(SeasonalLeavesBlock.SEASON);
	}

	/**
	 * Larch needles follow the season on their random ticks, placed ones too: green in spring and summer, gold in autumn,
	 * bare in winter, and green with seasons off. Needles placed in winter start bare.
	 */
	@GameTest
	public void larchNeedlesFollowTheSeason(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		SeasonCalendar.Settings before = JugcraftSeasons.settings();
		BlockPos log = new BlockPos(2, 2, 2);
		BlockPos natural = log.east();
		BlockPos placed = log.west();
		helper.setBlock(log, block("larch_log"));
		BlockState state = block("larch_needles").defaultBlockState().setValue(LeavesBlock.DISTANCE, 1);
		helper.setBlock(natural, state.setValue(LeavesBlock.PERSISTENT, false));
		helper.setBlock(placed, state.setValue(LeavesBlock.PERSISTENT, true));
		SeasonCalendar.Mode[] modes = {SeasonCalendar.Mode.AUTUMN, SeasonCalendar.Mode.WINTER, SeasonCalendar.Mode.SPRING,
				SeasonCalendar.Mode.AUTUMN, SeasonCalendar.Mode.SUMMER, SeasonCalendar.Mode.OFF};
		SeasonalLeavesBlock.Foliage[] looks = {SeasonalLeavesBlock.Foliage.GOLD, SeasonalLeavesBlock.Foliage.BARE, SeasonalLeavesBlock.Foliage.GREEN,
				SeasonalLeavesBlock.Foliage.GOLD, SeasonalLeavesBlock.Foliage.GREEN, SeasonalLeavesBlock.Foliage.GREEN};
		try {
			for (int i = 0; i < modes.length; i++) {
				JugcraftSeasons.setMode(server, modes[i]);
				for (BlockPos pos : new BlockPos[] {natural, placed}) {
					helper.getBlockState(pos).randomTick(level, helper.absolutePos(pos), level.getRandom());
					helper.assertTrue(needles(helper, pos) == looks[i],
							"In " + modes[i] + " needles at " + pos + " should be " + looks[i] + ", found " + needles(helper, pos));
				}
			}
			JugcraftSeasons.setMode(server, SeasonCalendar.Mode.WINTER);
			BlockPos fresh = log.north();
			helper.setBlock(fresh, state.setValue(LeavesBlock.PERSISTENT, true));
			helper.assertTrue(needles(helper, fresh) == SeasonalLeavesBlock.Foliage.BARE, "Needles placed in winter are " + needles(helper, fresh));
		} finally {
			JugcraftSeasons.setMode(server, before.mode());
		}
		helper.assertTrue(helper.getBlockState(natural).is(BlockTags.LEAVES), "Larch needles should be leaves");
		helper.succeed();
	}

	/** Out-of-date needles (as world generation leaves them) catch up together: one random tick turns the whole crown. */
	@GameTest
	public void staleLarchNeedlesCatchUpTogether(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		SeasonCalendar.Settings before = JugcraftSeasons.settings();
		BlockState state = block("larch_needles").defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
		List<BlockPos> crown = new ArrayList<>();
		try {
			JugcraftSeasons.setMode(server, SeasonCalendar.Mode.SUMMER);
			for (int x = 1; x <= 4; x++) {
				for (int z = 1; z <= 4; z++) {
					BlockPos pos = new BlockPos(x, 3, z);
					helper.setBlock(pos, state);
					crown.add(pos);
				}
			}
			JugcraftSeasons.setMode(server, SeasonCalendar.Mode.WINTER);
			helper.assertTrue(crown.stream().allMatch(pos -> needles(helper, pos) == SeasonalLeavesBlock.Foliage.GREEN),
					"Needles changed before any tick");
			BlockPos first = crown.get(0);
			helper.getBlockState(first).randomTick(level, helper.absolutePos(first), level.getRandom());
		} finally {
			JugcraftSeasons.setMode(server, before.mode());
		}
		long bare = crown.stream().filter(pos -> needles(helper, pos) == SeasonalLeavesBlock.Foliage.BARE).count();
		helper.assertTrue(bare == crown.size(), "One tick turned " + bare + " of " + crown.size() + " touching needles bare");
		helper.succeed();
	}

	/** Across a year every block turns: a crown changes over the jitter's days, and each season day has one look per block. */
	@GameTest
	public void larchNeedlesTurnGradually(GameTestHelper helper) {
		BlockPos a = new BlockPos(0, 64, 0);
		int earliest = Integer.MAX_VALUE;
		int latest = Integer.MIN_VALUE;
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				BlockPos pos = a.offset(x, 0, z);
				int first = -1;
				for (int day = JugcraftAgriculture.LARCH_LEAVES.goldFrom() - SeasonalLeavesBlock.JITTER; day <= JugcraftAgriculture.LARCH_LEAVES.goldFrom() + SeasonalLeavesBlock.JITTER; day++) {
					if (JugcraftAgriculture.LARCH_LEAVES.on(day, pos) == SeasonalLeavesBlock.Foliage.GOLD) {
						first = day;
						break;
					}
				}
				helper.assertTrue(first > 0, "Needles at " + pos + " never turn gold");
				earliest = Math.min(earliest, first);
				latest = Math.max(latest, first);
			}
		}
		LOGGER.info("Larch needles in a 16x16 patch turn gold between season days {} and {}", earliest, latest);
		helper.assertTrue(latest - earliest >= SeasonalLeavesBlock.JITTER, "A crown turns all at once (" + earliest + " to " + latest + ")");
		helper.assertTrue(JugcraftAgriculture.LARCH_LEAVES.on(0, a) == SeasonalLeavesBlock.Foliage.GREEN, "Needles with seasons off are not green");
		helper.succeed();
	}

	/** A larch sapling grows a tall larch of logs and needles; grown in winter, its needles are bare from the start. */
	@GameTest
	public void larchSaplingsGrowLarches(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		SeasonCalendar.Settings before = JugcraftSeasons.settings();
		BlockPos sapling = new BlockPos(3, 2, 3);
		for (int dy = 0; dy <= 24; dy++) {
			if (!helper.getBlockState(sapling.above(dy)).isAir()) {
				sapling = sapling.above(dy + 1);
				break;
			}
		}
		helper.setBlock(sapling.below(), Blocks.DIRT);
		helper.setBlock(sapling, block("larch_sapling"));
		BlockPos absolute = helper.absolutePos(sapling);
		JugcraftSeasons.setMode(server, SeasonCalendar.Mode.WINTER);
		boolean grown;
		try {
			grown = JugcraftAgriculture.LARCH_GROWER.growTree(level, level.getChunkSource().getGenerator(), absolute,
					level.getBlockState(absolute), level.getRandom());
		} finally {
			JugcraftSeasons.setMode(server, before.mode());
		}
		helper.assertTrue(grown, "The larch sapling at " + sapling + " should grow into a tree");
		helper.assertBlockPresent(block("larch_log"), sapling);
		int needles = 0;
		int bare = 0;
		int height = 0;
		for (BlockPos pos : BlockPos.betweenClosed(absolute.offset(-4, 0, -4), absolute.offset(4, 16, 4))) {
			BlockState state = level.getBlockState(pos);
			if (state.is(block("larch_needles"))) {
				needles++;
				bare += state.getValue(SeasonalLeavesBlock.SEASON) == SeasonalLeavesBlock.Foliage.BARE ? 1 : 0;
			} else if (state.is(block("larch_log"))) {
				height = Math.max(height, pos.getY() - absolute.getY() + 1);
			}
		}
		LOGGER.info("A larch grown in winter: trunk {} blocks, {} needles, {} bare", height, needles, bare);
		helper.assertTrue(needles >= 10 && height >= 7, "Expected a tall larch, found a " + height + "-block trunk and " + needles + " needles");
		helper.assertTrue(bare == needles, bare + " of " + needles + " needles grown in winter are bare");
		helper.succeed();
	}

	/** Any axe strips a larch log; larch wood joins vanilla's wood tags, burns, and the sapling is a sapling. */
	@GameTest
	public void larchWoodWorksLikeWood(GameTestHelper helper) {
		BlockPos pos = new BlockPos(1, 2, 1);
		helper.setBlock(pos, block("larch_log").defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
		helper.useBlock(pos, player);
		BlockState stripped = helper.getBlockState(pos);
		helper.assertTrue(stripped.is(block("stripped_larch_log")) && stripped.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X,
				"An axe should strip the log along its axis, found " + stripped);
		helper.assertTrue(stripped.is(BlockTags.LOGS), "Larch logs should be logs");
		helper.assertTrue(block("larch_sapling").defaultBlockState().is(BlockTags.SAPLINGS), "The larch sapling should be a sapling");
		helper.assertTrue(new ItemStack(JugcraftAgriculture.item("larch_planks")).is(ItemTags.PLANKS), "Larch planks should be planks");
		helper.assertTrue(new ItemStack(JugcraftAgriculture.item("larch_fence")).is(ItemTags.WOODEN_FENCES), "Larch fences should be wooden fences");
		for (String wood : new String[] {"larch_log", "larch_planks", "larch_slab", "larch_fence_gate"}) {
			helper.assertTrue(new ItemStack(JugcraftAgriculture.item(wood)).has(DataComponents.COOKING_FUEL), wood + " should burn as furnace fuel");
		}
		helper.succeed();
	}
}
