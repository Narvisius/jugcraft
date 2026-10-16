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
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;

/** Rooted native trees, seasonal needles and the complete renewable timber routes. */
public class TreeBatchFourBCGameTests {
	static final List<String> SHAPES = List.of("bald_cypress", "magnolia");
	static final long[] SEEDS = {1, 2, 3, 4096, 8192, 65537, -7, 987654321, 314159265, -271828182, Long.MAX_VALUE, Long.MIN_VALUE};
	private static final BlockPos AT = new BlockPos(34000, 100, -34000);
	static Block block(String name) { return JugcraftAgriculture.block(name); }
	static String leaf(String tree) { return tree.equals("bald_cypress") ? "bald_cypress_needles" : "magnolia_leaves"; }

	static void plot(net.minecraft.server.level.ServerLevel level, BlockPos at, int depth) {
		TreeBatchTwoGameTests.reset(level, at, depth == 0 ? Blocks.GRASS_BLOCK : Blocks.MUD);
		if (depth > 0) for (BlockPos pos : BlockPos.betweenClosed(at.offset(-12, 0, -12), at.offset(12, depth - 1, 12)))
			level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
	}

	@GameTest(maxTicks = 400)
	public void seededTreesHaveConnectedTimberDistinctCrownsAndCompactRoots(GameTestHelper helper) {
		var level = helper.getLevel();
		for (String tree : SHAPES) {
			var hashes = new HashSet<Long>();
			int moss = 0;
			for (long seed : SEEDS) {
				plot(level, AT, 0);
				helper.assertTrue(TreeBatchTwoGameTests.feature(level, tree).place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), AT), tree + " failed, seed " + seed);
				var size = TreeBatchTwoGameTests.measure(level, AT);
				Jugcraft.LOGGER.info("[tree 4bc shape] {} seed {}: {}", tree, seed, size);
				helper.assertTrue(size.height() >= (tree.equals("magnolia") ? 6 : 10) && size.height() <= (tree.equals("magnolia") ? 9 : 19)
						&& size.width() <= (tree.equals("magnolia") ? 9 : 15) && size.leaves() >= (tree.equals("magnolia") ? 30 : 20), tree + " outside crown envelope: " + size);
				var logs = new HashSet<BlockPos>();
				for (BlockPos pos : BlockPos.betweenClosed(AT.offset(-16, 0, -16), AT.offset(16, 22, 16))) {
					var state = level.getBlockState(pos);
					if (state.is(BlockTags.LOGS)) {
						helper.assertTrue(state.is(block(tree + "_log")), "Foreign timber in " + tree);
						logs.add(pos.immutable());
						if (tree.equals("bald_cypress") && pos.getY() <= AT.getY() + 1)
							helper.assertTrue(Math.abs(pos.getX() - AT.getX()) <= 3 && Math.abs(pos.getZ() - AT.getZ()) <= 3, "Root extends beyond compact footprint");
					}
					if (state.is(BlockTags.LEAVES)) helper.assertTrue(state.is(block(leaf(tree))), "Foreign leaves in " + tree);
				}
				// Native branching can join diagonally; every log must belong to the grounded trunk/root network.
				var connected = new HashSet<BlockPos>();
				for (var p : logs) if (p.getY() == AT.getY()) connected.add(p);
				boolean changed;
				do {
					changed = false;
					for (var p : logs) if (!connected.contains(p)) {
						for (var near : BlockPos.betweenClosed(p.offset(-1, -1, -1), p.offset(1, 1, 1))) if (connected.contains(near)) {
							connected.add(p); changed = true; break;
						}
					}
				} while (changed);
				helper.assertTrue(connected.size() == logs.size(), "Floating branch or roots in " + tree);
				hashes.add(size.hash()); moss += size.moss();
				plot(level, AT, 0);
				TreeBatchTwoGameTests.feature(level, tree).place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), AT);
				helper.assertTrue(size.equals(TreeBatchTwoGameTests.measure(level, AT)), "Seed changed its shape");
			}
			helper.assertTrue(hashes.size() >= 3, tree + " lacks variation");
			if (tree.equals("bald_cypress")) helper.assertTrue(moss > 0, "No shroud moss");
		}
		plot(level, AT, 0);
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void cypressRootsWorkInShallowWaterAndGrowthProtectsObstacles(GameTestHelper helper) {
		var level = helper.getLevel();
		var at = AT.offset(80, 0, 0);
		for (int depth = 0; depth <= 3; depth++) {
			int successes = 0;
			for (long seed : SEEDS) {
				plot(level, at, depth);
				boolean grew = TreeBatchTwoGameTests.feature(level, "bald_cypress").place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), at);
				if (grew) successes++;
				for (BlockPos p : BlockPos.betweenClosed(at.offset(-12, -1, -12), at.offset(12, -1, 12)))
					helper.assertTrue(level.getBlockState(p).is(depth == 0 ? Blocks.GRASS_BLOCK : Blocks.MUD), "Root replaced the soil bed");
				if (depth > 0) helper.assertTrue(level.getBlockState(at.offset(10, 0, 10)).is(Blocks.WATER), "Tree removed surrounding water");
			}
			Jugcraft.LOGGER.info("[tree 4bc roots] depth {}: {}/12", depth, successes);
			helper.assertTrue(successes >= 9, "Poor cypress placement at depth " + depth + ": " + successes);
		}
		for (String tree : SHAPES) {
			var sapling = (SaplingBlock) block(tree + "_sapling");
			var ready = sapling.defaultBlockState().setValue(SaplingBlock.STAGE, 1);
			for (long seed : SEEDS) {
				plot(level, at, 0);
				level.setBlock(at, ready, Block.UPDATE_CLIENTS);
				sapling.advanceTree(level, at, ready, RandomSource.create(seed));
				helper.assertTrue(TreeBatchTwoGameTests.measure(level, at).logs() > 4, "Sapling failed " + tree + " seed " + seed);
			}
			plot(level, at, 0);
			level.setBlock(at, ready, Block.UPDATE_CLIENTS);
			level.setBlock(at.above(), Blocks.DIAMOND_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
			sapling.advanceTree(level, at, ready, RandomSource.create(1));
			helper.assertTrue(level.getBlockState(at).equals(ready), "Blocked sapling consumed");
			helper.assertTrue(level.getBlockState(at.above()).is(Blocks.DIAMOND_BLOCK), "Obstacle replaced");
			helper.assertTrue(TreeBatchTwoGameTests.measure(level, at).logs() == 0, "Failed growth left partial roots");
			TreeBatchTwoGameTests.reset(level, at, Blocks.STONE);
			helper.assertTrue(!ready.canSurvive(level, at), "Sapling survives on stone");
			// Natural generation uses the checked placement, which enforces the sapling's soil predicate.
			helper.assertTrue(!TreeBatchTwoGameTests.feature(level, tree).place(level, level.getChunkSource().getGenerator(), RandomSource.create(1), at), "Natural tree grew on stone");
		}
		plot(level, at, 0);
		helper.succeed();
	}

	@GameTest
	public void fullWoodSetsStripCraftAndReturnTheirOwnSaplings(GameTestHelper helper) {
		var level = helper.getLevel();
		var pos = new BlockPos(3, 3, 3);
		var player = helper.makeMockPlayer(GameType.SURVIVAL);
		for (String tree : SHAPES) {
			for (String form : List.of("log", "wood")) for (var axis : Direction.Axis.values()) {
				helper.setBlock(pos, block(tree + "_" + form).defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
				var axe = new ItemStack(Items.IRON_AXE);
				player.setItemInHand(InteractionHand.MAIN_HAND, axe); helper.useBlock(pos, player);
				helper.assertTrue(helper.getBlockState(pos).is(block("stripped_" + tree + "_" + form))
						&& helper.getBlockState(pos).getValue(RotatedPillarBlock.AXIS) == axis && axe.getDamageValue() == 1, "Strip route failed");
			}
			var plank = block(tree + "_planks").asItem();
			for (String form : List.of(tree + "_log", tree + "_wood", "stripped_" + tree + "_log", "stripped_" + tree + "_wood"))
				PineWoodGameTests.craft(helper, plank, 4, 1, 1, block(form).asItem());
			PineWoodGameTests.craft(helper, block(tree + "_stairs").asItem(), 4, 3, 3, plank, null, null, plank, plank, null, plank, plank, plank);
			PineWoodGameTests.craft(helper, block(tree + "_slab").asItem(), 6, 3, 1, plank, plank, plank);
			PineWoodGameTests.craft(helper, block(tree + "_fence").asItem(), 3, 3, 2, plank, Items.STICK, plank, plank, Items.STICK, plank);
			PineWoodGameTests.craft(helper, block(tree + "_fence_gate").asItem(), 1, 3, 2, Items.STICK, plank, Items.STICK, Items.STICK, plank, Items.STICK);
			PineWoodGameTests.craft(helper, Items.CRAFTING_TABLE, 1, 2, 2, plank, plank, plank, plank);
			var sapling = block(tree + "_sapling").asItem();
			var farm = MachineRecipes.find(level, MachineKind.TREE_FARM, new ItemStack(sapling)).orElseThrow();
			helper.assertTrue(farm.output().create().is(block(tree + "_log").asItem()) && farm.output().create().getCount() == 6, "Wrong farm timber");
			helper.assertTrue(farm.byproducts().stream().anyMatch(p -> p.chance() == 1 && p.result().create().is(sapling)), "Farm lost sapling");
			var saw = MachineRecipes.find(level, MachineKind.SAWMILL, new ItemStack(block(tree + "_log"))).orElseThrow();
			helper.assertTrue(saw.output().create().is(plank) && saw.output().create().getCount() == 6, "Wrong sawmill output");
			int drops = 0;
			for (int i = 0; i < 512; i++) for (var drop : Block.getDrops(block(leaf(tree)).defaultBlockState(), level, helper.absolutePos(pos), null)) {
				helper.assertTrue(drop.is(sapling) || drop.is(Items.STICK), "Foreign leaf drop");
				if (drop.is(sapling)) drops++;
			}
			helper.assertTrue(drops > 0, "No renewable saplings");
			var silk = new ItemStack(Items.IRON_AXE);
			silk.enchant(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
					.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), 1);
			for (var tool : List.of(silk, new ItemStack(Items.SHEARS))) {
				var loot = Block.getDrops(block(leaf(tree)).defaultBlockState(), level, helper.absolutePos(pos), null, null, tool);
				helper.assertTrue(loot.size() == 1 && loot.getFirst().is(block(leaf(tree)).asItem()), "Preserved leaf drop incorrect");
			}
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void seasonalCypressAndEvergreenMagnoliaGrowAndDecayCorrectly(GameTestHelper helper) {
		var level = helper.getLevel();
		var before = JugcraftSeasons.settings();
		var at = AT.offset(160, 0, 0);
		try {
			for (var mode : SeasonCalendar.Mode.values()) {
				JugcraftSeasons.setMode(level.getServer(), mode);
				plot(level, at, 0);
				helper.assertTrue(TreeBatchTwoGameTests.feature(level, "bald_cypress").place(level, level.getChunkSource().getGenerator(), RandomSource.create(1), at), "Seasonal tree failed");
				for (var p : BlockPos.betweenClosed(at.offset(-8, 0, -8), at.offset(8, 22, 8))) {
					var state = level.getBlockState(p);
					if (state.is(block("bald_cypress_needles"))) helper.assertTrue(state.getValue(SeasonalLeavesBlock.SEASON)
							== JugcraftAgriculture.BALD_CYPRESS_LEAVES.on(JugcraftSeasons.today(), p), "Wrong generated season");
				}
				for (String tree : SHAPES) {
					var p = at.offset(14, 5, 14);
					var state = block(leaf(tree)).defaultBlockState();
					if (tree.equals("magnolia")) helper.assertTrue(!state.hasProperty(SeasonalLeavesBlock.SEASON), "Magnolia became deciduous");
					level.setBlock(p, state.setValue(LeavesBlock.PERSISTENT, false).setValue(LeavesBlock.DISTANCE, 7), Block.UPDATE_CLIENTS);
					level.getBlockState(p).randomTick(level, p, RandomSource.create(2));
					helper.assertTrue(level.getBlockState(p).isAir(), "Unsupported leaves did not decay");
					level.setBlock(p, state.setValue(LeavesBlock.PERSISTENT, true), Block.UPDATE_CLIENTS);
					level.getBlockState(p).randomTick(level, p, RandomSource.create(2));
					helper.assertTrue(level.getBlockState(p).is(block(leaf(tree))), "Placed leaves decayed");
				}
			}
		} finally { JugcraftSeasons.setMode(level.getServer(), before.mode()); }
		plot(level, at, 0);
		helper.succeed();
	}
}
