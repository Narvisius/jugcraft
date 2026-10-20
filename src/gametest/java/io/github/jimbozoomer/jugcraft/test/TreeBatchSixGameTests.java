package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.machine.MachineRecipes;
import io.github.jimbozoomer.jugcraft.season.JugcraftSeasons;
import io.github.jimbozoomer.jugcraft.season.SeasonCalendar;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;

public class TreeBatchSixGameTests {
	static final List<String> SHAPES = List.of("juniper", "juniper_column", "juniper_bush", "gorse", "sagebrush");
	static final List<String> BIOMES = List.of("canyon", "dryland", "xeric_shrubland", "dead_forest", "scrubland", "cold_desert",
			"field", "prairie", "heathland", "shrubland", "steppe", "tundra");
	private static final BlockPos AT = new BlockPos(42000, 100, -42000);
	static Block block(String name) { return JugcraftAgriculture.block(name); }
	static String tree(String shape) { return shape.startsWith("juniper") ? "juniper" : shape; }
	static String foliage(String shape) { return shape.startsWith("juniper") ? "juniper_needles" : shape + "_leaves"; }
	static Block timber(String tree) { return tree.equals("gorse") ? Blocks.OAK_LOG : block(tree.equals("sagebrush") ? "dead_log" : "juniper_log"); }

	@GameTest(maxTicks = 400)
	public void fiveShapesStayLowVaryAndKeepEveryLeafConnected(GameTestHelper helper) {
		var level = helper.getLevel();
		for (String shape : SHAPES) {
			var hashes = new HashSet<Long>();
			for (long seed : TreeBatchFourBCGameTests.SEEDS) {
				TreeBatchTwoGameTests.reset(level, AT, Blocks.GRASS_BLOCK);
				helper.assertTrue(TreeBatchTwoGameTests.feature(level, shape).place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), AT), "Shape failed " + shape);
				var size = TreeBatchTwoGameTests.measure(level, AT);
				Jugcraft.LOGGER.info("[tree 6 shape] {} seed {}: {}", shape, seed, size);
				boolean bush = shape.equals("juniper_bush") || shape.equals("gorse") || shape.equals("sagebrush");
				helper.assertTrue(size.logs() >= 1 && size.leaves() >= 4, "Empty shape " + shape);
				helper.assertTrue(bush ? size.logs() == 1 && size.height() <= 2 && size.width() <= (shape.equals("sagebrush") ? 3 : 5)
						: shape.equals("juniper_column") ? size.height() >= 5 && size.height() <= 7 && size.width() <= 5
						: size.height() >= 4 && size.height() <= 6 && size.width() <= 9, "Size outside intended envelope " + shape + " " + size);
				var leaves = new HashSet<BlockPos>();
				var reachable = new HashSet<BlockPos>();
				var queue = new ArrayDeque<BlockPos>();
				for (var p : BlockPos.betweenClosed(AT.offset(-12,0,-12), AT.offset(12,12,12))) {
					var state = level.getBlockState(p);
					if (state.is(BlockTags.LOGS)) { helper.assertTrue(state.is(timber(tree(shape))), "Wrong stem"); queue.add(p.immutable()); }
					if (state.is(BlockTags.LEAVES)) {
						helper.assertTrue(state.is(block(foliage(shape))), "Wrong foliage"); leaves.add(p.immutable());
						helper.assertTrue(!state.getValue(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.DISTANCE) < 7, "Unsupported or persistent foliage");
					}
				}
				while (!queue.isEmpty()) {
					var current = queue.removeFirst();
					for (var direction : Direction.values()) {
						var next = current.relative(direction);
						if (leaves.contains(next) && reachable.add(next)) queue.addLast(next);
					}
				}
				helper.assertTrue(reachable.containsAll(leaves), "Floating foliage island in " + shape);
				hashes.add(size.hash());
			}
			helper.assertTrue(hashes.size() >= 3, "No seeded variation " + shape);
		}
		TreeBatchTwoGameTests.reset(level, AT, Blocks.GRASS_BLOCK);
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void saplingsAndFeaturesAcceptCorrectSoilsAndProtectObstacles(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.assertTrue(JugcraftAgriculture.JUNIPER_GROWER.getMinimumHeight(level).orElseThrow() == 1, "Bone meal height check must allow the shortest bush form");
		for (String name : List.of("juniper", "gorse", "sagebrush")) {
			var sapling = (SaplingBlock) block(name + "_sapling");
			var ready = sapling.defaultBlockState().setValue(SaplingBlock.STAGE, 1);
			var soils = name.equals("juniper") ? List.of(Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.SAND, Blocks.RED_SAND, Blocks.TERRACOTTA,
					net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("red_terracotta")))
					: List.of(Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.COARSE_DIRT);
			var forms = new HashSet<String>();
			for (Block soil : soils) for (long seed : TreeBatchFourBCGameTests.SEEDS) {
				TreeBatchTwoGameTests.reset(level, AT, soil);
				helper.assertTrue(ready.canSurvive(level, AT), "Sapling rejects intended soil " + name + " " + soil);
				level.setBlock(AT, ready, Block.UPDATE_CLIENTS);
				sapling.advanceTree(level, AT, ready, RandomSource.create(seed));
				var size = TreeBatchTwoGameTests.measure(level, AT);
				helper.assertTrue(size.logs() > 0 && size.leaves() > 0, "Sapling failed " + name + " " + soil);
				forms.add(size.logs() == 1 ? "bush" : size.height() >= 7 ? "column" : "tree");
			}
			if (name.equals("juniper")) helper.assertTrue(forms.size() == 3, "Juniper saplings did not grow all forms");
			TreeBatchTwoGameTests.reset(level, AT, Blocks.GRASS_BLOCK);
			level.setBlock(AT, ready, Block.UPDATE_CLIENTS);
			level.setBlock(AT.above(), Blocks.DIAMOND_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
			sapling.advanceTree(level, AT, ready, RandomSource.create(1));
			helper.assertTrue(level.getBlockState(AT).equals(ready) && level.getBlockState(AT.above()).is(Blocks.DIAMOND_BLOCK), "Obstructed growth destroyed a block");
			for (Block bad : List.of(Blocks.STONE, Blocks.GRAVEL, Blocks.POWDER_SNOW)) {
				TreeBatchTwoGameTests.reset(level, AT, bad);
				helper.assertTrue(!ready.canSurvive(level, AT), "Invalid soil accepted");
				for (String shape : SHAPES) if (tree(shape).equals(name)) helper.assertTrue(!TreeBatchTwoGameTests.feature(level, shape)
						.place(level, level.getChunkSource().getGenerator(), RandomSource.create(1), AT), "Feature accepted invalid soil " + shape);
			}
		}
		TreeBatchTwoGameTests.reset(level, AT, Blocks.GRASS_BLOCK);
		helper.succeed();
	}

	@GameTest
	public void berriesRightClickRegrowOnEdgesInEveryCalendarMode(GameTestHelper helper) {
		var level = helper.getLevel();
		var relative = new BlockPos(3,3,3);
		var pos = helper.absolutePos(relative);
		var player = helper.makeMockPlayer(GameType.SURVIVAL);
		var leaf = block("juniper_needles");
		var natural = leaf.defaultBlockState().setValue(LeavesBlock.DISTANCE, 1);
		var area = new AABB(pos).inflate(3);
		for (Direction d : Direction.values()) level.setBlock(pos.relative(d), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
		level.setBlock(pos.west(), block("juniper_log").defaultBlockState(), Block.UPDATE_CLIENTS);
		level.setBlock(pos.east(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		var before = JugcraftSeasons.settings();
		try {
			for (var mode : SeasonCalendar.Mode.values()) {
				JugcraftSeasons.setMode(level.getServer(), mode);
				level.setBlock(pos, natural, Block.UPDATE_CLIENTS);
				var random = RandomSource.create(6601);
				for (int i=0; i<512; i++) level.getBlockState(pos).randomTick(level,pos,random);
				helper.assertTrue(level.getBlockState(pos).getValue(FruitingLeavesBlock.FRUIT) == 2, "No ripe berries in " + mode);
				helper.useBlock(relative,player);
				var drops = level.getEntitiesOfClass(ItemEntity.class,area,e -> e.getItem().is(JugcraftAgriculture.item("juniper_berries")));
				int count = drops.stream().mapToInt(e -> e.getItem().getCount()).sum();
				helper.assertTrue(count >= 1 && count <= 2 && drops.stream().allMatch(e -> e.blockPosition().equals(pos.east())), "Harvest missing or trapped in soil");
				helper.assertTrue(level.getBlockState(pos).equals(natural), "Harvest did not reset berries");
				helper.useBlock(relative,player);
				helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class,area,e -> e.getItem().is(JugcraftAgriculture.item("juniper_berries"))).stream().mapToInt(e -> e.getItem().getCount()).sum() == count, "Duplicate harvest");
				for (int i=0; i<512; i++) level.getBlockState(pos).randomTick(level,pos,random);
				helper.assertTrue(level.getBlockState(pos).getValue(FruitingLeavesBlock.FRUIT) == 2, "No regrowth");
				drops.forEach(ItemEntity::discard);
			}
		} finally { JugcraftSeasons.setMode(level.getServer(), before.mode()); }
		for (boolean placed : List.of(true,false)) {
			level.setBlock(pos,natural.setValue(LeavesBlock.PERSISTENT,placed),Block.UPDATE_CLIENTS);
			level.setBlock(pos.east(),(placed ? Blocks.AIR : Blocks.STONE).defaultBlockState(),Block.UPDATE_CLIENTS);
			for (int i=0;i<512;i++) level.getBlockState(pos).randomTick(level,pos,RandomSource.create(i));
			helper.assertTrue(level.getBlockState(pos).getValue(FruitingLeavesBlock.FRUIT)==0,"Placed/enclosed leaves grew fruit");
		}
		level.setBlock(pos,natural.setValue(LeavesBlock.DISTANCE,7),Block.UPDATE_CLIENTS);
		level.getBlockState(pos).randomTick(level,pos,RandomSource.create(1));
		helper.assertTrue(level.getBlockState(pos).isAir(),"Detached foliage did not decay");
		helper.succeed();
	}

	@GameTest(maxTicks = 300)
	public void allJuniperFormsGrowPickableFruitAndLeavesHaveRenewableLoot(GameTestHelper helper) {
		var level=helper.getLevel();
		for(String shape:SHAPES.subList(0,3)) {
			TreeBatchTwoGameTests.reset(level,AT,Blocks.COARSE_DIRT);
			helper.assertTrue(TreeBatchTwoGameTests.feature(level,shape).place(level,level.getChunkSource().getGenerator(),RandomSource.create(4096),AT),"Feature failed");
			int ripe=0; var random=RandomSource.create(6602);
			for(var p:BlockPos.betweenClosed(AT.offset(-10,0,-10),AT.offset(10,10,10))) {
				var state=level.getBlockState(p);
				if(!state.is(block("juniper_needles")) || !FruitingLeavesBlock.canFruit(state,level,p)) continue;
				for(int i=0;i<512;i++) level.getBlockState(p).randomTick(level,p,random);
				if(level.getBlockState(p).getValue(FruitingLeavesBlock.FRUIT)==2) ripe++;
			}
			helper.assertTrue(ripe>=4,"Generated form has no accessible ripe foliage " + shape);
		}
		var silk=new ItemStack(Items.IRON_AXE);
		silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),1);
		for(String name:List.of("juniper","gorse","sagebrush")) {
			var state=block(foliage(name)).defaultBlockState();
			if(name.equals("juniper")) state=state.setValue(FruitingLeavesBlock.FRUIT,2);
			for(var tool:List.of(silk,new ItemStack(Items.SHEARS))) {
				var drops=Block.getDrops(state,level,AT,null,null,tool);
				helper.assertTrue(drops.size()==1 && drops.getFirst().is(state.getBlock().asItem()),"Silk/shears fruit duplication");
			}
			int saplings=0;
			if(name.equals("juniper")) state=state.setValue(FruitingLeavesBlock.FRUIT,0);
			for(int i=0;i<1024;i++) for(var drop:Block.getDrops(state,level,AT,null)) {
				helper.assertTrue(drop.is(block(name+"_sapling").asItem()) || drop.is(Items.STICK),"Picked foliage duplicates fruit");
				if(drop.is(block(name+"_sapling").asItem())) saplings++;
			}
			helper.assertTrue(saplings>0,"Sapling not renewable");
		}
		TreeBatchTwoGameTests.reset(level,AT,Blocks.GRASS_BLOCK);
		helper.succeed();
	}

	@GameTest
	public void woodRecipesStrippingFarmsFlowersAndSpicesUseExistingSystems(GameTestHelper helper) {
		var level=helper.getLevel(); var player=helper.makeMockPlayer(GameType.SURVIVAL); var pos=new BlockPos(3,3,3);
		var plank=block("juniper_planks").asItem();
		for(String form:List.of("log","wood")) for(var axis:Direction.Axis.values()) {
			helper.setBlock(pos,block("juniper_"+form).defaultBlockState().setValue(RotatedPillarBlock.AXIS,axis));
			var axe=new ItemStack(Items.IRON_AXE); player.setItemInHand(InteractionHand.MAIN_HAND,axe); helper.useBlock(pos,player);
			helper.assertTrue(helper.getBlockState(pos).is(block("stripped_juniper_"+form)) && helper.getBlockState(pos).getValue(RotatedPillarBlock.AXIS)==axis && axe.getDamageValue()==1,"Stripping failed");
		}
		for(String form:List.of("juniper_log","juniper_wood","stripped_juniper_log","stripped_juniper_wood")) {
			PineWoodGameTests.craft(helper,plank,4,1,1,block(form).asItem());
			var saw=MachineRecipes.find(level,MachineKind.SAWMILL,new ItemStack(block(form))).orElseThrow();
			helper.assertTrue(saw.output().create().is(plank) && saw.output().create().getCount()==6,"Wrong sawmill yield");
		}
		PineWoodGameTests.craft(helper,block("juniper_stairs").asItem(),4,3,3,plank,null,null,plank,plank,null,plank,plank,plank);
		PineWoodGameTests.craft(helper,block("juniper_slab").asItem(),6,3,1,plank,plank,plank);
		PineWoodGameTests.craft(helper,block("juniper_fence").asItem(),3,3,2,plank,Items.STICK,plank,plank,Items.STICK,plank);
		PineWoodGameTests.craft(helper,block("juniper_fence_gate").asItem(),1,3,2,Items.STICK,plank,Items.STICK,Items.STICK,plank,Items.STICK);
		for(String name:List.of("juniper","gorse","sagebrush")) {
			var farm=MachineRecipes.find(level,MachineKind.TREE_FARM,new ItemStack(block(name+"_sapling"))).orElseThrow();
			helper.assertTrue(farm.output().create().is(timber(name).asItem()) && farm.output().create().getCount()==(name.equals("juniper")?6:1),"Wrong farm timber/yield");
			helper.assertTrue(farm.byproducts().stream().anyMatch(p -> p.chance()==1 && p.result().create().is(block(name+"_sapling").asItem())),"Farm lost sapling");
		}
		helper.assertTrue(block("gorse_leaves").defaultBlockState().is(BlockTags.FLOWERS),"Gorse not available to bees");
		helper.assertTrue(net.minecraft.world.entity.animal.bee.Bee.attractsBees(block("gorse_leaves").defaultBlockState()),"Bees reject gorse flowers");
		helper.assertTrue(new ItemStack(JugcraftAgriculture.item("juniper_berries")).is(ItemTags.FOX_FOOD),"Fox food tag missing");
		PineWoodGameTests.craft(helper,JugcraftAgriculture.item("mulling_spices"),2,3,1,Items.SUGAR,JugcraftAgriculture.item("juniper_berries"),Items.COCOA_BEANS);
		PineWoodGameTests.craft(helper,JugcraftAgriculture.item("mulling_spices"),2,3,1,Items.SUGAR,Items.SWEET_BERRIES,Items.COCOA_BEANS);
		helper.succeed();
	}

	@GameTest
	public void steppeHasLowShrubsAndKeepsItsPlainsCropTag(GameTestHelper helper) {
		var registry=helper.getLevel().registryAccess();
		var biome=registry.lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Jugcraft.id("steppe"))).value();
		var features=new HashSet<String>();
		for(var step:biome.getGenerationSettings().features()) for(var feature:step) feature.unwrapKey().ifPresent(key -> features.add(key.identifier().toString()));
		helper.assertTrue(features.contains("jugcraft:trees_steppe") && registry.lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Jugcraft.id("steppe"))).is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags.IS_PLAINS),"Steppe lost shrubs or crops");
		// The flat game-test world excludes custom biomes from its source. The rendered normal-world test checks dynamic crop/fruit injection.
		helper.succeed();
	}
}
