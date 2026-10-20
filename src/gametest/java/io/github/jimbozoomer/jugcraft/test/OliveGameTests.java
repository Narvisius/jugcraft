package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.machine.MachineRecipes;
import io.github.jimbozoomer.jugcraft.season.JugcraftSeasons;
import io.github.jimbozoomer.jugcraft.season.SeasonCalendar;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;

public class OliveGameTests {
	static final List<String> SHAPES = List.of("olive", "olive_bees", "ancient_olive", "olive_bush");
	private static final BlockPos AT = new BlockPos(47000, 100, -47000);
	static Block block(String id) { return JugcraftAgriculture.block(id); }

	@GameTest(maxTicks = 400)
	public void oliveShapesAreConnectedVariedAndWithinBounds(GameTestHelper helper) {
		var level=helper.getLevel();
		for (String shape: SHAPES) {
			var hashes=new HashSet<Long>();
			for (long seed: TreeBatchFourBCGameTests.SEEDS) {
				TreeBatchTwoGameTests.reset(level,AT,Blocks.GRASS_BLOCK);
				helper.assertTrue(TreeBatchTwoGameTests.feature(level,shape).place(level,level.getChunkSource().getGenerator(),RandomSource.create(seed),AT), "Failed olive " + shape);
				var size=TreeBatchTwoGameTests.measure(level,AT); hashes.add(size.hash());
				Jugcraft.LOGGER.info("[olive shape] {} seed {}: {}",shape,seed,size);
				helper.assertTrue(shape.equals("olive_bush") ? size.height()>=2 && size.height()<=3 && size.width()<=5 && size.logs()==1
						: shape.equals("ancient_olive") ? size.height()>=6 && size.height()<=8 && size.width()>=9 && size.width()<=11
						: size.height()>=5 && size.height()<=7 && size.width()>=6 && size.width()<=9, "Olive shape outside envelope " + size);
				var leaves=new HashSet<BlockPos>(); var reached=new HashSet<BlockPos>(); var queue=new ArrayDeque<BlockPos>();
				for (var p:BlockPos.betweenClosed(AT.offset(-12,0,-12),AT.offset(12,12,12))) {
					var state=level.getBlockState(p);
					if(state.is(BlockTags.LOGS)) { helper.assertTrue(state.is(block("olive_log")),"Wrong timber"); queue.add(p.immutable()); }
					if(state.is(BlockTags.LEAVES)) {
						helper.assertTrue(shape.equals("olive_bush")||p.getY()>=AT.getY()+2,"Olive crown hides its low fork");
						helper.assertTrue(state.is(block(shape.equals("olive_bush")?"wild_olive_leaves":"olive_leaves")),"Wrong olive foliage");
						helper.assertTrue(!state.getValue(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.DISTANCE)<7,"Detached generated foliage");
						leaves.add(p.immutable());
					}
				}
				while(!queue.isEmpty()) { var p=queue.remove(); for(var direction:Direction.values()) {var next=p.relative(direction); if(leaves.contains(next)&&reached.add(next)) queue.add(next);} }
				helper.assertTrue(!leaves.isEmpty()&&reached.containsAll(leaves),"Disconnected olive leaves " + shape);
			}
			helper.assertTrue(hashes.size()>=3,"Repeated olive geometry");
			TreeBatchTwoGameTests.reset(level,AT,Blocks.STONE);
			helper.assertTrue(!TreeBatchTwoGameTests.feature(level,shape).place(level,level.getChunkSource().getGenerator(),RandomSource.create(1),AT),"Olive grew on stone");
		}
		TreeBatchTwoGameTests.reset(level,AT,Blocks.GRASS_BLOCK); helper.succeed();
	}

	@GameTest(maxTicks=200)
	public void oliveSaplingsGrowSingleAndAncientAndRestoreBlockedSquares(GameTestHelper helper) {
		var level=helper.getLevel(); var at=AT.offset(100,0,0);
		for(String tree:List.of("olive","olive_bush")) {
			TreeBatchTwoGameTests.reset(level,at,Blocks.GRASS_BLOCK);
			var sapling=(SaplingBlock)block(tree+"_sapling"); var ready=sapling.defaultBlockState().setValue(SaplingBlock.STAGE,1);
			level.setBlock(at,ready,Block.UPDATE_CLIENTS); sapling.advanceTree(level,at,ready,RandomSource.create(2));
			helper.assertTrue(TreeBatchTwoGameTests.measure(level,at).logs()>0,"Olive sapling failed");
			TreeBatchTwoGameTests.reset(level,at,Blocks.GRASS_BLOCK); level.setBlock(at,ready,Block.UPDATE_CLIENTS);
			level.setBlock(at.above(),Blocks.DIAMOND_BLOCK.defaultBlockState(),Block.UPDATE_CLIENTS);
			sapling.advanceTree(level,at,ready,RandomSource.create(2));
			helper.assertTrue(level.getBlockState(at).equals(ready)&&level.getBlockState(at.above()).is(Blocks.DIAMOND_BLOCK),"Blocked sapling overwritten");
		}
		TreeBatchTwoGameTests.reset(level,at,Blocks.GRASS_BLOCK);
		var sapling=(SaplingBlock)block("olive_sapling"); var square=List.of(at,at.east(),at.south(),at.east().south());
		for(int i=0;i<4;i++) level.setBlock(square.get(i),sapling.defaultBlockState().setValue(SaplingBlock.STAGE,i%2),Block.UPDATE_CLIENTS);
		var before=square.stream().map(level::getBlockState).toList(); var trigger=square.get(3);
		level.setBlock(at.east().above(3),Blocks.DIAMOND_BLOCK.defaultBlockState(),Block.UPDATE_CLIENTS);
		sapling.advanceTree(level,trigger,level.getBlockState(trigger),RandomSource.create(2));
		for(int i=0;i<4;i++) helper.assertTrue(level.getBlockState(square.get(i)).equals(before.get(i)),"Blocked ancient olive lost a sapling");
		level.setBlock(at.east().above(3),Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS);
		sapling.advanceTree(level,trigger,level.getBlockState(trigger),RandomSource.create(2));
		for(var p:square) helper.assertTrue(level.getBlockState(p).is(block("olive_log")),"Ancient olive has no 2x2 trunk");
		TreeBatchTwoGameTests.reset(level,at,Blocks.GRASS_BLOCK); helper.succeed();
	}

	@GameTest
	public void olivesHarvestOnceRegrowYearRoundAndWildBushesNeverFruit(GameTestHelper helper) {
		var level=helper.getLevel(); var relative=new BlockPos(3,3,3); var pos=helper.absolutePos(relative);
		var leaves=(OliveLeavesBlock)block("olive_leaves"); var natural=leaves.defaultBlockState().setValue(LeavesBlock.DISTANCE,1);
		var player=helper.makeMockPlayer(GameType.SURVIVAL); player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
		var area=new AABB(pos).inflate(3); var previous=JugcraftSeasons.settings();
		try {
			for(var mode:SeasonCalendar.Mode.values()) {
				JugcraftSeasons.setMode(level.getServer(),mode);
				level.setBlock(pos.west(),block("olive_log").defaultBlockState(),Block.UPDATE_CLIENTS); level.setBlock(pos.below(),Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS);
				level.setBlock(pos,natural,Block.UPDATE_CLIENTS); var random=RandomSource.create(10701);
				for(int i=0;i<512;i++) level.getBlockState(pos).randomTick(level,pos,random);
				helper.assertTrue(level.getBlockState(pos).getValue(FruitingLeavesBlock.FRUIT)==2,"Olives failed ripening in " + mode);
				level.getEntitiesOfClass(ItemEntity.class,area).forEach(ItemEntity::discard); helper.useBlock(relative,player);
				int count=level.getEntitiesOfClass(ItemEntity.class,area,e->e.getItem().is(JugcraftAgriculture.item("olives"))).stream().mapToInt(e->e.getItem().getCount()).sum();
				helper.assertTrue(count>=1&&count<=2&&level.getBlockState(pos).getValue(FruitingLeavesBlock.FRUIT)==0,"Right click failed");
				helper.useBlock(relative,player);
				helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class,area,e->e.getItem().is(JugcraftAgriculture.item("olives"))).stream().mapToInt(e->e.getItem().getCount()).sum()==count,"Duplicate harvest");
				for(int i=0;i<512;i++) level.getBlockState(pos).randomTick(level,pos,random);
				helper.assertTrue(level.getBlockState(pos).getValue(FruitingLeavesBlock.FRUIT)==2,"No regrowth");
				level.setBlock(pos,natural.setValue(LeavesBlock.PERSISTENT,true),Block.UPDATE_CLIENTS);
				for(int i=0;i<512;i++) level.getBlockState(pos).randomTick(level,pos,random);
				helper.assertTrue(level.getBlockState(pos).getValue(FruitingLeavesBlock.FRUIT)==0&&!leaves.pick(level,pos),"Placed leaves farm olives");
				var wild=block("wild_olive_leaves").defaultBlockState().setValue(LeavesBlock.DISTANCE,1); level.setBlock(pos,wild,Block.UPDATE_CLIENTS);
				for(int i=0;i<512;i++) level.getBlockState(pos).randomTick(level,pos,random);
				helper.assertTrue(level.getBlockState(pos).equals(wild)&&!wild.hasProperty(FruitingLeavesBlock.FRUIT),"Wild bush began fruiting");
			}
		} finally {JugcraftSeasons.setMode(level.getServer(),previous.mode());}
		level.setBlock(pos,natural.setValue(LeavesBlock.DISTANCE,7),Block.UPDATE_CLIENTS); level.getBlockState(pos).randomTick(level,pos,RandomSource.create(1));
		helper.assertTrue(level.getBlockState(pos).isAir(),"Detached leaves did not decay"); helper.succeed();
	}

	@GameTest
	public void oliveWoodAndLeafLootKeepCultivatedAndWildRoutesRenewable(GameTestHelper helper) {
		var level=helper.getLevel(); var pos=new BlockPos(3,3,3); var player=helper.makeMockPlayer(GameType.SURVIVAL); var plank=block("olive_planks").asItem();
		for(String form:List.of("log","wood")) for(var axis:Direction.Axis.values()) {
			helper.setBlock(pos,block("olive_"+form).defaultBlockState().setValue(RotatedPillarBlock.AXIS,axis)); var axe=new ItemStack(Items.IRON_AXE); player.setItemInHand(InteractionHand.MAIN_HAND,axe); helper.useBlock(pos,player);
			helper.assertTrue(helper.getBlockState(pos).is(block("stripped_olive_"+form))&&helper.getBlockState(pos).getValue(RotatedPillarBlock.AXIS)==axis&&axe.getDamageValue()==1,"Stripping failed");
		}
		for(String form:List.of("olive_log","olive_wood","stripped_olive_log","stripped_olive_wood")) {
			PineWoodGameTests.craft(helper,plank,4,1,1,block(form).asItem()); var saw=MachineRecipes.find(level,MachineKind.SAWMILL,new ItemStack(block(form))).orElseThrow();
			helper.assertTrue(saw.output().create().is(plank)&&saw.output().create().getCount()==6,"Sawmill yield");
		}
		PineWoodGameTests.craft(helper,block("olive_slab").asItem(),6,3,1,plank,plank,plank);
		PineWoodGameTests.craft(helper,block("olive_stairs").asItem(),4,3,3,plank,null,null,plank,plank,null,plank,plank,plank);
		PineWoodGameTests.craft(helper,block("olive_fence").asItem(),3,3,2,plank,Items.STICK,plank,plank,Items.STICK,plank);
		PineWoodGameTests.craft(helper,block("olive_fence_gate").asItem(),1,3,2,Items.STICK,plank,Items.STICK,Items.STICK,plank,Items.STICK);
		var silk=new ItemStack(Items.IRON_AXE); silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),1);
		for(String tree:List.of("olive","olive_bush")) {
			var farm=MachineRecipes.find(level,MachineKind.TREE_FARM,new ItemStack(block(tree+"_sapling"))).orElseThrow();
			helper.assertTrue(farm.output().create().is(block("olive_log").asItem())&&farm.output().create().getCount()==(tree.equals("olive")?6:1),"Tree farm yield");
			var state=block(tree.equals("olive")?"olive_leaves":"wild_olive_leaves").defaultBlockState(); if(tree.equals("olive"))state=state.setValue(FruitingLeavesBlock.FRUIT,2);
			int saplings=0;
			for(int i=0;i<512;i++) for(var drop:Block.getDrops(state,level,helper.absolutePos(pos),null)) {
				if(drop.is(block(tree+"_sapling").asItem())) saplings++;
				if(tree.equals("olive_bush")) helper.assertTrue(!drop.is(JugcraftAgriculture.item("olives")),"Wild bush dropped fruit");
			}
			helper.assertTrue(saplings>0,"Saplings unavailable");
			for(var tool:List.of(silk,new ItemStack(Items.SHEARS))) {var drops=Block.getDrops(state,level,helper.absolutePos(pos),null,null,tool); helper.assertTrue(drops.size()==1&&drops.getFirst().is(state.getBlock().asItem()),"Silk/shears duplicate fruit");}
		}
		helper.succeed();
	}

	@GameTest(maxTicks=600)
	public void olivesPickleWithOneVinegarBottleAndReturnTheirJar(GameTestHelper helper) {
		var at=new BlockPos(3,2,3); helper.setBlock(at.below(),Blocks.CAMPFIRE); helper.setBlock(at,block("cooking_pot"));
		var pot=helper.getBlockEntity(at,CookingPotBlockEntity.class);
		pot.setItem(0,new ItemStack(JugcraftAgriculture.item("mason_jar"))); pot.setItem(1,new ItemStack(JugcraftAgriculture.item("olives"),4)); pot.setItem(2,new ItemStack(JugcraftAgriculture.item("cider_vinegar")));
		helper.succeedWhen(()->{
			var result=pot.getItem(CookingPotBlockEntity.RESULT);
			helper.assertTrue(result.is(JugcraftAgriculture.item("pickled_olives"))&&PreserveJarItem.servings(result)==PreserveJarItem.SERVINGS,"No full olive jar");
			helper.assertTrue(pot.getItem(0).isEmpty()&&pot.getItem(1).isEmpty()&&pot.getItem(2).isEmpty(),"Ingredients not consumed exactly");
			helper.assertTrue(pot.getItem(CookingPotBlockEntity.RESULT+1).is(Items.GLASS_BOTTLE)&&pot.getItem(CookingPotBlockEntity.RESULT+1).getCount()==1,"Vinegar bottle lost/duplicated");
			var player=helper.makeMockServerPlayerInLevel(); player.setGameMode(GameType.SURVIVAL); var eating=result.copy();
			for(int i=0;i<PreserveJarItem.SERVINGS;i++){player.getFoodData().setFoodLevel(2); eating=eating.finishUsingItem(helper.getLevel(),player); helper.assertTrue(player.getFoodData().getFoodLevel()==4,"Wrong olive serving");}
			helper.assertTrue(eating.is(JugcraftAgriculture.item("mason_jar")),"Last serving lost jar");
		});
	}
}
