package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.machine.MachineRecipes;
import java.util.*;
import io.github.jimbozoomer.jugcraft.season.JugcraftSeasons;
import io.github.jimbozoomer.jugcraft.season.SeasonCalendar;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.level.block.*;

public class DryCountryTreeGameTests {
	static final List<String> SHAPES=List.of("umbrella_pine","stone_pine","pinyon","wattle","wattle_bush","tamarisk","joshua_tree");
	private static final BlockPos AT=new BlockPos(49000,100,-49000);
	static Block block(String id){return JugcraftAgriculture.block(id);}
	static String timber(String shape){return shape.contains("pine")||shape.equals("pinyon")?"pine":shape.startsWith("wattle")?"minecraft:acacia":shape.equals("joshua_tree")?"yucca":"tamarisk";}
	static String leaves(String shape){return shape.contains("pine")||shape.equals("pinyon")?"stone_pine_needles":shape.startsWith("wattle")?"wattle_leaves":shape.equals("joshua_tree")?"joshua_tree_fronds":"tamarisk_leaves";}

	@GameTest(maxTicks=400)
	public void allSevenShapesAreVariedConnectedAndWithinTheirBounds(GameTestHelper helper){
		var level=helper.getLevel();
		for(String shape:SHAPES){
			var hashes=new HashSet<Long>();
			for(long seed:TreeBatchFourBCGameTests.SEEDS){
				TreeBatchTwoGameTests.reset(level,AT,Blocks.GRASS_BLOCK);
				helper.assertTrue(TreeBatchTwoGameTests.feature(level,shape).place(level,level.getChunkSource().getGenerator(),RandomSource.create(seed),AT),"Placement failed: "+shape);
				var size=TreeBatchTwoGameTests.measure(level,AT);hashes.add(size.hash());
				Jugcraft.LOGGER.info("[dry-country shape] {} seed {}: {}",shape,seed,size);
				int minH=shape.equals("umbrella_pine")?9:shape.equals("stone_pine")?6:shape.equals("wattle_bush")?2:4;
				int maxH=shape.equals("umbrella_pine")?11:shape.equals("stone_pine")?10:shape.equals("wattle_bush")?3:6;
				int minW=shape.equals("umbrella_pine")?7:shape.equals("stone_pine")||shape.equals("tamarisk")||shape.equals("joshua_tree")?5:4;
				int maxW=shape.equals("umbrella_pine")?9:shape.equals("wattle_bush")?5:shape.equals("pinyon")||shape.equals("wattle")?6:7;
				helper.assertTrue(size.height()>=minH&&size.height()<=maxH&&size.width()>=minW&&size.width()<=maxW,"Outside planned bounds: "+shape+" "+size);
				var foliage=new HashSet<BlockPos>();var reached=new HashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();
				for(var p:BlockPos.betweenClosed(AT.offset(-12,0,-12),AT.offset(12,14,12))){
					var state=level.getBlockState(p);
					if(state.is(BlockTags.LOGS)){
						helper.assertTrue(state.is(timber(shape).equals("minecraft:acacia")?Blocks.ACACIA_LOG:block(timber(shape)+"_log")),"Wrong timber "+shape);queue.add(p.immutable());
					}
					if(state.is(BlockTags.LEAVES)){
						helper.assertTrue(state.is(block(leaves(shape)))&&!state.getValue(LeavesBlock.PERSISTENT)&&state.getValue(LeavesBlock.DISTANCE)<7,"Wrong or detached foliage "+shape+" "+p);foliage.add(p.immutable());
					}
				}
				while(!queue.isEmpty()){var p=queue.remove();for(var d:Direction.values()){var next=p.relative(d);if(foliage.contains(next)&&reached.add(next))queue.add(next);}}
				helper.assertTrue(!foliage.isEmpty()&&reached.containsAll(foliage),"Disconnected crown "+shape);
				if(shape.equals("wattle_bush"))helper.assertTrue(size.logs()==1,"Bush timber yield changed");
			}
			helper.assertTrue(hashes.size()>=3,"Insufficient shape variation "+shape);
		}
		TreeBatchTwoGameTests.reset(level,AT,Blocks.GRASS_BLOCK);helper.succeed();
	}

	@GameTest(maxTicks=300)
	public void saplingsGrowOnTheirSoilsAndDoNotOverwriteObstructions(GameTestHelper helper){
		var level=helper.getLevel();var at=AT.offset(100,0,0);
		for(String tree:List.of("stone_pine","wattle","tamarisk","joshua_tree")){
			var sapling=(SaplingBlock)block(tree+"_sapling");var ready=sapling.defaultBlockState().setValue(SaplingBlock.STAGE,1);
			for(Block soil:tree.equals("wattle")?List.of(Blocks.GRASS_BLOCK,Blocks.COARSE_DIRT):List.of(Blocks.GRASS_BLOCK,Blocks.COARSE_DIRT,Blocks.SAND,Blocks.RED_SAND,Blocks.TERRACOTTA)){
				TreeBatchTwoGameTests.reset(level,at,soil);helper.assertTrue(ready.canSurvive(level,at),"Sapling rejects "+soil+": "+tree);
				level.setBlock(at,ready,Block.UPDATE_CLIENTS);sapling.advanceTree(level,at,ready,RandomSource.create(3));
				helper.assertTrue(TreeBatchTwoGameTests.measure(level,at).logs()>0,"Sapling failed on "+soil+": "+tree);
			}
			for(Block soil:List.of(Blocks.STONE,Blocks.CALCITE)){
				TreeBatchTwoGameTests.reset(level,at,soil);helper.assertTrue(!ready.canSurvive(level,at),"Sapling accepts bare rock "+tree);
			}
			TreeBatchTwoGameTests.reset(level,at,Blocks.GRASS_BLOCK);level.setBlock(at,ready,Block.UPDATE_CLIENTS);level.setBlock(at.above(),Blocks.DIAMOND_BLOCK.defaultBlockState(),Block.UPDATE_CLIENTS);
			sapling.advanceTree(level,at,ready,RandomSource.create(3));helper.assertTrue(level.getBlockState(at).equals(ready)&&level.getBlockState(at.above()).is(Blocks.DIAMOND_BLOCK),"Blocked sapling damaged blocks "+tree);
		}
		TreeBatchTwoGameTests.reset(level,at,Blocks.GRASS_BLOCK);helper.succeed();
	}

	@GameTest
	public void bothNewWoodsStripCraftAndSawAndAllSaplingsHaveRenewableFarmRoutes(GameTestHelper helper){
		var level=helper.getLevel();var pos=new BlockPos(3,3,3);var player=helper.makeMockPlayer(GameType.SURVIVAL);
		for(String wood:List.of("tamarisk","yucca")){
			var plank=block(wood+"_planks").asItem();
			for(String form:List.of("log","wood"))for(var axis:Direction.Axis.values()){
				helper.setBlock(pos,block(wood+"_"+form).defaultBlockState().setValue(RotatedPillarBlock.AXIS,axis));var axe=new ItemStack(Items.IRON_AXE);player.setItemInHand(InteractionHand.MAIN_HAND,axe);helper.useBlock(pos,player);
				helper.assertTrue(helper.getBlockState(pos).is(block("stripped_"+wood+"_"+form))&&helper.getBlockState(pos).getValue(RotatedPillarBlock.AXIS)==axis&&axe.getDamageValue()==1,"Stripping failed "+wood);
			}
			for(String form:List.of(wood+"_log",wood+"_wood","stripped_"+wood+"_log","stripped_"+wood+"_wood")){
				PineWoodGameTests.craft(helper,plank,4,1,1,block(form).asItem());var saw=MachineRecipes.find(level,MachineKind.SAWMILL,new ItemStack(block(form))).orElseThrow();helper.assertTrue(saw.output().create().is(plank)&&saw.output().create().getCount()==6,"Wrong sawmill result");
			}
			PineWoodGameTests.craft(helper,block(wood+"_slab").asItem(),6,3,1,plank,plank,plank);
			PineWoodGameTests.craft(helper,block(wood+"_stairs").asItem(),4,3,3,plank,null,null,plank,plank,null,plank,plank,plank);
			PineWoodGameTests.craft(helper,block(wood+"_fence").asItem(),3,3,2,plank,Items.STICK,plank,plank,Items.STICK,plank);
			PineWoodGameTests.craft(helper,block(wood+"_fence_gate").asItem(),1,3,2,Items.STICK,plank,Items.STICK,Items.STICK,plank,Items.STICK);
		}
		for(String tree:List.of("stone_pine","wattle","tamarisk","joshua_tree")){
			var farm=MachineRecipes.find(level,MachineKind.TREE_FARM,new ItemStack(block(tree+"_sapling"))).orElseThrow();var wood=timber(tree);
			helper.assertTrue(farm.output().create().is((wood.equals("minecraft:acacia")?Blocks.ACACIA_LOG:block(wood+"_log")).asItem())&&farm.output().create().getCount()==(tree.equals("wattle")?1:6),"Wrong farm output "+tree);
			int seeds=0;var state=block(leaves(tree)).defaultBlockState();
			for(int i=0;i<512;i++)for(var drop:Block.getDrops(state,level,helper.absolutePos(pos),null))if(drop.is(block(tree+"_sapling").asItem()))seeds++;
			helper.assertTrue(seeds>0,"No renewable sapling "+tree);
			var drops=Block.getDrops(state,level,helper.absolutePos(pos),null,null,new ItemStack(Items.SHEARS));helper.assertTrue(drops.size()==1&&drops.getFirst().is(state.getBlock().asItem()),"Shears loot wrong "+tree);
		}
		helper.succeed();
	}
	@GameTest
	public void pineNutsHarvestOnceRegrowYearRoundAndRejectDecorativeLeaves(GameTestHelper helper) {
		var level=helper.getLevel(); var relative=new BlockPos(3,3,3); var pos=helper.absolutePos(relative);
		var leaves=(StonePineLeavesBlock)block("stone_pine_needles"); var natural=leaves.defaultBlockState().setValue(LeavesBlock.DISTANCE,1);
		var player=helper.makeMockPlayer(GameType.SURVIVAL); player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
		var area=new AABB(pos).inflate(3); var previous=JugcraftSeasons.settings();
		try {
			for(var mode:SeasonCalendar.Mode.values()) {
				JugcraftSeasons.setMode(level.getServer(),mode);
				level.setBlock(pos.west(),block("pine_log").defaultBlockState(),Block.UPDATE_CLIENTS); level.setBlock(pos.below(),Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS);
				level.setBlock(pos,natural,Block.UPDATE_CLIENTS); var random=RandomSource.create(10701);
				for(int i=0;i<512;i++) level.getBlockState(pos).randomTick(level,pos,random);
				helper.assertTrue(level.getBlockState(pos).getValue(FruitingLeavesBlock.FRUIT)==2,"Pine nuts failed ripening in " + mode);
				level.getEntitiesOfClass(ItemEntity.class,area).forEach(ItemEntity::discard); helper.useBlock(relative,player);
				int count=level.getEntitiesOfClass(ItemEntity.class,area,e->e.getItem().is(JugcraftAgriculture.item("pine_nuts"))).stream().mapToInt(e->e.getItem().getCount()).sum();
				helper.assertTrue(count>=1&&count<=2&&level.getBlockState(pos).getValue(FruitingLeavesBlock.FRUIT)==0,"Right click failed");
				helper.useBlock(relative,player);
				helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class,area,e->e.getItem().is(JugcraftAgriculture.item("pine_nuts"))).stream().mapToInt(e->e.getItem().getCount()).sum()==count,"Duplicate harvest");
				for(int i=0;i<512;i++) level.getBlockState(pos).randomTick(level,pos,random);
				helper.assertTrue(level.getBlockState(pos).getValue(FruitingLeavesBlock.FRUIT)==2,"No regrowth");
				level.setBlock(pos,natural.setValue(LeavesBlock.PERSISTENT,true),Block.UPDATE_CLIENTS);
				for(int i=0;i<512;i++) level.getBlockState(pos).randomTick(level,pos,random);
				helper.assertTrue(level.getBlockState(pos).getValue(FruitingLeavesBlock.FRUIT)==0&&!leaves.pick(level,pos),"Placed leaves farm pine nuts");
			}
		} finally {JugcraftSeasons.setMode(level.getServer(),previous.mode());}
		level.setBlock(pos,natural.setValue(LeavesBlock.DISTANCE,7),Block.UPDATE_CLIENTS); level.getBlockState(pos).randomTick(level,pos,RandomSource.create(1));
		helper.assertTrue(level.getBlockState(pos).isAir(),"Detached leaves did not decay"); helper.succeed();
	}

    @GameTest
    public void pineNutsRoastAndFeedSquirrels(GameTestHelper helper) {
        var level=helper.getLevel();var input=new SingleRecipeInput(new ItemStack(JugcraftAgriculture.item("pine_nuts")));
        var result=level.getServer().getRecipeManager().getRecipeFor(RecipeType.CAMPFIRE_COOKING,input,level)
                .orElseThrow(()->helper.assertionException("Pine nuts have no campfire recipe")).value().assemble(input);
        helper.assertTrue(result.is(JugcraftAgriculture.item("roasted_pine_nuts"))&&result.getCount()==1,"Wrong roasted nuts");
        var food=TagKey.create(Registries.ITEM,Jugcraft.id("squirrel_food"));
        helper.assertTrue(input.item().is(food)&&result.is(food),"Squirrel food missing");
        helper.succeed();
    }
}
