package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.SeasonalLeavesBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.machine.MachineRecipes;
import io.github.jimbozoomer.jugcraft.season.JugcraftSeasons;
import io.github.jimbozoomer.jugcraft.season.SeasonCalendar;
import java.util.HashSet;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;

/** New foliage on established timber: real placement, growth, renewable drops and processing. */
public class TreeBatchFourGameTests {
	static final List<String> SHAPES = List.of("snowpetal_cherry", "shimmer_birch", "tall_shimmer_birch");
	private static final BlockPos AT = new BlockPos(30000, 100, -30000);
	private static Block block(String name) { return JugcraftAgriculture.block(name); }
	private static String tree(String shape) { return shape.replace("tall_", ""); }
	private static Block log(String tree) { return tree.equals("snowpetal_cherry") ? Blocks.CHERRY_LOG : Blocks.BIRCH_LOG; }

	@GameTest(maxTicks = 400)
	public void shapesVaryReproduceAndKeepCorrectTimberAndLeaves(GameTestHelper helper) {
		var level = helper.getLevel();
		for (String shape : SHAPES) {
			var hashes = new HashSet<Long>();
			for (long seed : new long[] {1, 2, 3, 4096, 8192, 65537, -7, 987654321, 314159265, -271828182, Long.MAX_VALUE, Long.MIN_VALUE}) {
				TreeBatchTwoGameTests.reset(level, AT, Blocks.DIRT);
				helper.assertTrue(TreeBatchTwoGameTests.feature(level, shape).place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), AT), shape + " failed");
				var size = TreeBatchTwoGameTests.measure(level, AT);
				int min = shape.startsWith("tall_") ? 11 : shape.equals("shimmer_birch") ? 6 : 7;
				int max = shape.startsWith("tall_") ? 15 : shape.equals("shimmer_birch") ? 8 : 13;
				int width = shape.equals("snowpetal_cherry") ? 19 : shape.startsWith("tall_") ? 7 : 5;
				helper.assertTrue(size.height() >= min && size.height() <= max && size.width() <= width, shape + " size " + size);
				for (BlockPos pos : BlockPos.betweenClosed(AT.offset(-16, 0, -16), AT.offset(16, 20, 16))) {
					var state = level.getBlockState(pos);
					if (state.is(BlockTags.LOGS)) helper.assertTrue(state.is(log(tree(shape))), "Wrong timber " + shape);
					if (state.is(BlockTags.LEAVES)) helper.assertTrue(state.is(block(tree(shape) + "_leaves")), "Wrong leaves " + shape);
				}
				hashes.add(size.hash());
				TreeBatchTwoGameTests.reset(level, AT, Blocks.DIRT);
				TreeBatchTwoGameTests.feature(level, shape).place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), AT);
				helper.assertTrue(size.equals(TreeBatchTwoGameTests.measure(level, AT)), shape + " is not reproducible");
				Jugcraft.LOGGER.info("[tree batch 4 shape] {} seed {}: {}", shape, seed, size);
			}
			helper.assertTrue(hashes.size() >= 3, shape + " lacks variation");
		}
		TreeBatchTwoGameTests.reset(level, AT, Blocks.DIRT);
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void saplingsGrowBothBirchFormsAndKeepTheirStateWhenBlocked(GameTestHelper helper) {
		var level = helper.getLevel();
		var at = AT.offset(80, 0, 0);
		for (String name : List.of("snowpetal_cherry", "shimmer_birch")) {
			var sapling = (SaplingBlock) block(name + "_sapling");
			var ready = sapling.defaultBlockState().setValue(SaplingBlock.STAGE, 1);
			int tall = 0;
			var seeds = RandomSource.create(9404);
			for (int i = 0; i < 40; i++) {
				TreeBatchTwoGameTests.reset(level, at, Blocks.GRASS_BLOCK);
				level.setBlock(at, ready, Block.UPDATE_CLIENTS);
				sapling.advanceTree(level, at, ready, RandomSource.create(seeds.nextLong()));
				helper.assertTrue(level.getBlockState(at).is(log(name)), name + " sapling did not grow");
				if (TreeBatchTwoGameTests.measure(level, at).height() >= 11) tall++;
			}
			if (name.equals("shimmer_birch")) helper.assertTrue(tall > 0 && tall < 20, "Shimmer growth did not include both forms: " + tall);
			Jugcraft.LOGGER.info("[tree batch 4 saplings] {}: {}/40 tall", name, tall);
			TreeBatchTwoGameTests.reset(level, at, Blocks.DIRT);
			level.setBlock(at, ready, Block.UPDATE_CLIENTS);
			level.setBlock(at.above(), Blocks.DIAMOND_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
			sapling.advanceTree(level, at, ready, RandomSource.create(9));
			helper.assertTrue(level.getBlockState(at).equals(ready), "Blocked growth consumed or changed sapling");
			helper.assertTrue(level.getBlockState(at.above()).is(Blocks.DIAMOND_BLOCK), "Growth replaced obstruction");
			TreeBatchTwoGameTests.reset(level, at, Blocks.STONE);
			helper.assertTrue(!ready.canSurvive(level, at), "Sapling survives on stone");
		}
		for (String shape : SHAPES) {
			TreeBatchTwoGameTests.reset(level, at, Blocks.STONE);
			helper.assertTrue(!TreeBatchTwoGameTests.feature(level, shape).place(level, level.getChunkSource().getGenerator(), RandomSource.create(1), at), "Tree grew on stone");
			TreeBatchTwoGameTests.reset(level, at, Blocks.DIRT);
			level.setBlock(at, Blocks.SNOW.defaultBlockState(), Block.UPDATE_CLIENTS);
			boolean placed = TreeBatchTwoGameTests.feature(level, shape).place(level, level.getChunkSource().getGenerator(), RandomSource.create(1), at);
			TreeBatchTwoGameTests.reset(level, at, Blocks.DIRT);
			level.setBlock(at, Blocks.SNOW.defaultBlockState(), Block.UPDATE_CLIENTS);
			var vanilla = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.PLACED_FEATURE)
					.getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.PLACED_FEATURE,
							net.minecraft.resources.Identifier.withDefaultNamespace(shape.equals("snowpetal_cherry") ? "cherry_checked" : "birch_checked"))).value();
			helper.assertTrue(placed == vanilla.place(level, level.getChunkSource().getGenerator(), RandomSource.create(1), at), "Snow clearance differs from vanilla");
			// Planting clears the occupied snow layer; surrounding snow must not block the trunk.
			TreeBatchTwoGameTests.reset(level, at, Blocks.DIRT);
			for (BlockPos pos : BlockPos.betweenClosed(at.offset(-8, 0, -8), at.offset(8, 0, 8)))
				if (!pos.equals(at)) level.setBlock(pos, Blocks.SNOW.defaultBlockState(), Block.UPDATE_CLIENTS);
			helper.assertTrue(TreeBatchTwoGameTests.feature(level, shape).place(level, level.getChunkSource().getGenerator(), RandomSource.create(1), at), "Tree failed in surrounding snow");
		}
		TreeBatchTwoGameTests.reset(level, at, Blocks.DIRT);
		helper.succeed();
	}

	@GameTest
	public void ownSaplingDropsAndVanillaTimberProcessingRemainRenewable(GameTestHelper helper) {
		var level = helper.getLevel();
		var at = helper.absolutePos(new BlockPos(3, 3, 3));
		for (String name : List.of("snowpetal_cherry", "shimmer_birch")) {
			var leaf = block(name + "_leaves");
			var sapling = block(name + "_sapling").asItem();
			var silk = new ItemStack(Items.IRON_AXE);
			silk.enchant(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
					.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), 1);
			for (var tool : List.of(new ItemStack(Items.SHEARS), silk)) {
				var drops = Block.getDrops(leaf.defaultBlockState(), level, at, null, null, tool);
				helper.assertTrue(drops.size() == 1 && drops.getFirst().is(leaf.asItem()) && drops.getFirst().getCount() == 1, "Preserved leaves duplicated drops");
			}
			int saplings = 0;
			for (int i = 0; i < 512; i++) for (var drop : Block.getDrops(leaf.defaultBlockState(), level, at, null)) {
				helper.assertTrue(drop.is(sapling) || drop.is(Items.STICK), "Incorrect leaf drop " + drop);
				if (drop.is(sapling)) saplings += drop.getCount();
			}
			helper.assertTrue(saplings > 0, "Missing renewable sapling drop");
			var recipe = MachineRecipes.find(level, MachineKind.TREE_FARM, new ItemStack(sapling)).orElseThrow();
			var output = recipe.output().create();
			helper.assertTrue(output.is(log(name).asItem()) && output.getCount() == 6, "Wrong tree-farm timber");
			helper.assertTrue(recipe.byproducts().stream().anyMatch(p -> p.chance() == 1 && p.result().create().is(sapling)
					&& p.result().create().getCount() == 1), "Tree farm lost its sapling");
			PineWoodGameTests.craft(helper, name.equals("snowpetal_cherry") ? Items.CHERRY_PLANKS : Items.BIRCH_PLANKS, 4, 1, 1, log(name).asItem());
			helper.assertTrue(MachineRecipes.find(level, MachineKind.SAWMILL, new ItemStack(log(name))).isPresent(), "Missing vanilla sawmill route");
			for (String suffix : List.of("log", "wood", "planks", "stairs", "slab", "fence"))
				helper.assertTrue(!BuiltInRegistries.BLOCK.containsKey(Jugcraft.id(name + "_" + suffix)), "Phantom wood-set registration");
		}
		helper.succeed();
	}

	@GameTest
	public void leavesStayEvergreenAndDecayWithoutTheirVanillaLogs(GameTestHelper helper) {
		var level = helper.getLevel();
		var before = JugcraftSeasons.settings();
		var at = helper.absolutePos(new BlockPos(3, 3, 3));
		try {
			for (var mode : SeasonCalendar.Mode.values()) {
				JugcraftSeasons.setMode(level.getServer(), mode);
				for (String name : List.of("snowpetal_cherry", "shimmer_birch")) {
					var leaves = block(name + "_leaves").defaultBlockState();
					helper.assertTrue(!leaves.hasProperty(SeasonalLeavesBlock.SEASON), "Evergreen acquired seasonal states");
					var unsupported = leaves.setValue(LeavesBlock.DISTANCE, 7).setValue(LeavesBlock.PERSISTENT, false);
					level.setBlock(at, unsupported, Block.UPDATE_CLIENTS);
					unsupported.randomTick(level, at, RandomSource.create(4));
					helper.assertTrue(level.getBlockState(at).isAir(), "Unsupported leaves failed to decay");
					var persistent = unsupported.setValue(LeavesBlock.PERSISTENT, true);
					level.setBlock(at, persistent, Block.UPDATE_CLIENTS);
					persistent.randomTick(level, at, RandomSource.create(4));
					helper.assertTrue(level.getBlockState(at).is(block(name + "_leaves")), "Placed leaves decayed");
				}
			}
		} finally { JugcraftSeasons.setMode(level.getServer(), before.mode()); }
		helper.succeed();
	}
}
