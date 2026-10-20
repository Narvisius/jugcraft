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
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class HollyGameTests {
	private static final BlockPos AT = new BlockPos(44000, 100, -44000);
	static Block block(String name) { return JugcraftAgriculture.block(name); }
	@GameTest(maxTicks = 400)
	public void hollyIsAnUprightConnectedTreeAndItsSaplingsAreRenewable(GameTestHelper helper) {
		var level = helper.getLevel(); var hashes = new HashSet<Long>();
		var settings = JugcraftSeasons.settings();
		try {
			for (var mode : List.of(SeasonCalendar.Mode.SUMMER, SeasonCalendar.Mode.WINTER, SeasonCalendar.Mode.OFF)) {
				JugcraftSeasons.setMode(level.getServer(), mode);
				for (long seed : TreeBatchFourBCGameTests.SEEDS) {
					TreeBatchTwoGameTests.reset(level, AT, Blocks.GRASS_BLOCK);
					helper.assertTrue(TreeBatchTwoGameTests.feature(level, "holly").place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), AT), "Holly feature failed");
					var size = TreeBatchTwoGameTests.measure(level, AT);
					Jugcraft.LOGGER.info("[holly shape] {} seed {}: {}", mode, seed, size);
					helper.assertTrue(size.height() >= 5 && size.height() <= 7 && size.width() <= 7 && size.logs() >= 4 && size.leaves() >= 20, "Holly outside intended size " + size);
					var leaves = new HashSet<BlockPos>(); var reached = new HashSet<BlockPos>(); var queue = new ArrayDeque<BlockPos>();
					for (var p : BlockPos.betweenClosed(AT.offset(-10,0,-10), AT.offset(10,12,10))) {
						var state = level.getBlockState(p);
						if (state.is(BlockTags.LOGS)) { helper.assertTrue(state.is(block("holly_log")), "Wrong wood"); queue.add(p.immutable()); }
						if (state.is(BlockTags.LEAVES)) {
							helper.assertTrue(state.is(block("holly_leaves")) && !state.getValue(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.DISTANCE) < 7, "Wrong or unsupported leaves");
							helper.assertTrue(state.getValue(SeasonalLeavesBlock.SEASON) == JugcraftAgriculture.HOLLY_LEAVES.on(JugcraftSeasons.today(), p), "Worldgen ignored calendar");
							leaves.add(p.immutable());
						}
					}
					while (!queue.isEmpty()) { var current = queue.removeFirst(); for (var d : Direction.values()) { var next = current.relative(d); if (leaves.contains(next) && reached.add(next)) queue.addLast(next); } }
					helper.assertTrue(reached.containsAll(leaves), "Detached foliage island"); hashes.add(size.hash());
				}
			}
		} finally { JugcraftSeasons.setMode(level.getServer(), settings.mode()); }
		helper.assertTrue(hashes.size() >= 3, "Tree lacks seed variation");
		var sapling = (SaplingBlock) block("holly_sapling"); var ready = sapling.defaultBlockState().setValue(SaplingBlock.STAGE,1);
		for (var soil : List.of(Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.COARSE_DIRT)) {
			TreeBatchTwoGameTests.reset(level,AT,soil); level.setBlock(AT,ready,Block.UPDATE_CLIENTS);
			sapling.advanceTree(level,AT,ready,RandomSource.create(2)); helper.assertTrue(level.getBlockState(AT).is(block("holly_log")),"Sapling failed on normal soil");
		}
		for (var soil : List.of(Blocks.SAND, Blocks.STONE, Blocks.GRAVEL)) {
			TreeBatchTwoGameTests.reset(level,AT,soil); helper.assertTrue(!ready.canSurvive(level,AT),"Invalid soil accepted");
		}
		TreeBatchTwoGameTests.reset(level,AT,Blocks.GRASS_BLOCK); level.setBlock(AT,ready,Block.UPDATE_CLIENTS); level.setBlock(AT.above(),Blocks.DIAMOND_BLOCK.defaultBlockState(),Block.UPDATE_CLIENTS);
		sapling.advanceTree(level,AT,ready,RandomSource.create(2)); helper.assertTrue(level.getBlockState(AT).equals(ready) && level.getBlockState(AT.above()).is(Blocks.DIAMOND_BLOCK),"Growth destroyed obstruction");
		TreeBatchTwoGameTests.reset(level,AT,Blocks.GRASS_BLOCK); helper.succeed();
	}

	@GameTest
	public void shearsClipOneSprigAndRegrowInEveryCalendarMode(GameTestHelper helper) {
		var level=helper.getLevel(); var relative=new BlockPos(3,3,3); var pos=helper.absolutePos(relative);
		var player=helper.makeMockPlayer(GameType.SURVIVAL); var shears=new ItemStack(Items.SHEARS); player.setItemInHand(InteractionHand.MAIN_HAND,shears);
		var natural=block("holly_leaves").defaultBlockState().setValue(LeavesBlock.DISTANCE,1); var area=new AABB(pos).inflate(3);
		level.setBlock(pos.west(),block("holly_log").defaultBlockState(),Block.UPDATE_CLIENTS);
		var before=JugcraftSeasons.settings();
		try {
			for (var mode:SeasonCalendar.Mode.values()) {
				JugcraftSeasons.setMode(level.getServer(),mode); level.setBlock(pos,natural,Block.UPDATE_CLIENTS);
				int wear=shears.getDamageValue(); helper.useBlock(relative,player);
				helper.assertTrue(level.getBlockState(pos).getValue(HollyLeavesBlock.CLIPPED) && shears.getDamageValue()==wear+1,"Clip did not consume availability/durability in "+mode);
				var drops=level.getEntitiesOfClass(ItemEntity.class,area,e->e.getItem().is(JugcraftAgriculture.item("holly_sprig")));
				helper.assertTrue(drops.stream().mapToInt(e->e.getItem().getCount()).sum()==1,"Clip should yield one sprig");
				helper.useBlock(relative,player);
				helper.assertTrue(shears.getDamageValue()==wear+1 && level.getEntitiesOfClass(ItemEntity.class,area,e->e.getItem().is(JugcraftAgriculture.item("holly_sprig"))).stream().mapToInt(e->e.getItem().getCount()).sum()==1,"Duplicate clip");
				var random=RandomSource.create(10661); for(int i=0;i<512;i++) level.getBlockState(pos).randomTick(level,pos,random);
				helper.assertTrue(!level.getBlockState(pos).getValue(HollyLeavesBlock.CLIPPED),"No year-round regrowth");
				helper.assertTrue(level.getBlockState(pos).getValue(SeasonalLeavesBlock.SEASON)==JugcraftAgriculture.HOLLY_LEAVES.on(JugcraftSeasons.today(),pos),"Season did not catch up");
				drops.forEach(ItemEntity::discard);
			}
		} finally { JugcraftSeasons.setMode(level.getServer(),before.mode()); }
		int wear=shears.getDamageValue();
		level.setBlock(pos,natural.setValue(LeavesBlock.PERSISTENT,true),Block.UPDATE_CLIENTS); helper.useBlock(relative,player);
		helper.assertTrue(shears.getDamageValue()==wear && level.getEntitiesOfClass(ItemEntity.class,area,e->e.getItem().is(JugcraftAgriculture.item("holly_sprig"))).isEmpty(),"Placed leaf yielded sprig");
		level.setBlock(pos,natural.setValue(LeavesBlock.PERSISTENT,true).setValue(HollyLeavesBlock.CLIPPED,true),Block.UPDATE_CLIENTS);
		for(int i=0;i<512;i++) level.getBlockState(pos).randomTick(level,pos,RandomSource.create(i));
		helper.assertTrue(level.getBlockState(pos).getValue(HollyLeavesBlock.CLIPPED),"Placed leaf regrew");
		level.setBlock(pos,natural.setValue(LeavesBlock.DISTANCE,7),Block.UPDATE_CLIENTS); helper.useBlock(relative,player);
		helper.assertTrue(shears.getDamageValue()==wear,"Detached leaves clipped"); level.getBlockState(pos).randomTick(level,pos,RandomSource.create(1));
		helper.assertTrue(level.getBlockState(pos).isAir(),"Detached leaves did not decay"); helper.succeed();
	}

	@GameTest
	public void woodRecipesAndLeafLootKeepTheAcquisitionRouteRenewable(GameTestHelper helper) {
		var level=helper.getLevel(); var player=helper.makeMockPlayer(GameType.SURVIVAL); var pos=new BlockPos(3,3,3); var planks=block("holly_planks").asItem();
		for(String form:List.of("log","wood")) for(var axis:Direction.Axis.values()) {
			helper.setBlock(pos,block("holly_"+form).defaultBlockState().setValue(RotatedPillarBlock.AXIS,axis)); var axe=new ItemStack(Items.IRON_AXE); player.setItemInHand(InteractionHand.MAIN_HAND,axe); helper.useBlock(pos,player);
			helper.assertTrue(helper.getBlockState(pos).is(block("stripped_holly_"+form)) && helper.getBlockState(pos).getValue(RotatedPillarBlock.AXIS)==axis && axe.getDamageValue()==1,"Stripping failed");
		}
		for(String form:List.of("holly_log","holly_wood","stripped_holly_log","stripped_holly_wood")) {
			PineWoodGameTests.craft(helper,planks,4,1,1,block(form).asItem()); var saw=MachineRecipes.find(level,MachineKind.SAWMILL,new ItemStack(block(form))).orElseThrow();
			helper.assertTrue(saw.output().create().is(planks) && saw.output().create().getCount()==6,"Sawmill yield");
		}
		PineWoodGameTests.craft(helper,block("holly_slab").asItem(),6,3,1,planks,planks,planks);
		PineWoodGameTests.craft(helper,block("holly_stairs").asItem(),4,3,3,planks,null,null,planks,planks,null,planks,planks,planks);
		PineWoodGameTests.craft(helper,block("holly_fence").asItem(),3,3,2,planks,Items.STICK,planks,planks,Items.STICK,planks);
		PineWoodGameTests.craft(helper,block("holly_fence_gate").asItem(),1,3,2,Items.STICK,planks,Items.STICK,Items.STICK,planks,Items.STICK);
		var farm=MachineRecipes.find(level,MachineKind.TREE_FARM,new ItemStack(block("holly_sapling"))).orElseThrow();
		helper.assertTrue(farm.output().create().is(block("holly_log").asItem()) && farm.output().create().getCount()==6 && farm.byproducts().stream().anyMatch(p->p.chance()==1 && p.result().create().is(block("holly_sapling").asItem())),"Farm lost wood/sapling");
		var silk=new ItemStack(Items.IRON_AXE); silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),1);
		for(boolean clipped:List.of(false,true)) for(var season:SeasonalLeavesBlock.Foliage.values()) {
			var state=block("holly_leaves").defaultBlockState().setValue(HollyLeavesBlock.CLIPPED,clipped).setValue(SeasonalLeavesBlock.SEASON,season);
			for(var tool:List.of(silk,new ItemStack(Items.SHEARS))) { var drops=Block.getDrops(state,level,AT,null,null,tool); helper.assertTrue(drops.size()==1 && drops.getFirst().is(block("holly_leaves").asItem()),"Shears/Silk duplicate sprigs"); }
			int saplings=0; for(int i=0;i<512;i++) for(var drop:Block.getDrops(state,level,AT,null)) { helper.assertTrue(drop.is(Items.STICK)||drop.is(block("holly_sapling").asItem()),"Unexpected sprig on break"); if(drop.is(block("holly_sapling").asItem())) saplings++; }
			helper.assertTrue(saplings>0,"Sapling unavailable");
		}
		var sprig=JugcraftAgriculture.item("holly_sprig"); PineWoodGameTests.craft(helper,block("holly_wreath").asItem(),1,3,3,sprig,sprig,sprig,sprig,Items.STRING,sprig,sprig,sprig,sprig);
		helper.assertTrue(!new ItemStack(sprig).has(net.minecraft.core.component.DataComponents.FOOD),"Decorative holly is food"); helper.succeed();
	}

	@GameTest
	public void wreathHangsOnWallsAndDoorsAndDropsOnceWithItsSupport(GameTestHelper helper) {
		var level=helper.getLevel(); var player=helper.makeMockPlayer(GameType.SURVIVAL); var wall=new BlockPos(3,3,3);
		helper.setBlock(wall,Blocks.OAK_PLANKS); player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(block("holly_wreath"),8));
		for(var direction:Direction.Plane.HORIZONTAL) {
			var absolute=helper.absolutePos(wall); player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(absolute),direction,absolute,false)));
			var state=helper.getBlockState(wall.relative(direction)); helper.assertTrue(state.is(block("holly_wreath")) && state.getValue(AutumnWreathBlock.FACING)==direction,"Wrong wreath placement");
		}
		var absolute=helper.absolutePos(wall); int count=player.getMainHandItem().getCount(); player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(absolute),Direction.UP,absolute,false)));
		helper.assertTrue(helper.getBlockState(wall.above()).isAir() && player.getMainHandItem().getCount()==count,"Wreath stood on floor");
		var north=wall.north(); var old=helper.getBlockState(north); player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(JugcraftAgriculture.item("red_mum"),2)); helper.useBlock(north,player);
		helper.assertTrue(helper.getBlockState(north).equals(old) && player.getMainHandItem().getCount()==2,"Mum recolored holly");
		level.destroyBlock(absolute,false);
		for(var direction:Direction.Plane.HORIZONTAL) helper.assertTrue(helper.getBlockState(wall.relative(direction)).isAir(),"Unsupported wreath remained");
		int drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(absolute).inflate(3),e->e.getItem().is(block("holly_wreath").asItem())).stream().mapToInt(e->e.getItem().getCount()).sum(); helper.assertTrue(drops==4,"Support loss duplicated or lost wreaths: "+drops);
		var door=new BlockPos(6,2,4); helper.setBlock(door.below(),Blocks.STONE); helper.setBlock(door,Blocks.OAK_DOOR.defaultBlockState()); helper.setBlock(door.above(),Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER));
		player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(block("holly_wreath"))); var doorPos=helper.absolutePos(door.above()); player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(doorPos),Direction.NORTH,doorPos,false)));
		helper.assertTrue(DoorWreathGameTests.at(helper,doorPos,false).wreathState().is(block("holly_wreath")),"Door placement failed"); helper.succeed();
	}

	// Door movement now has stronger coverage in DoorWreathGameTests: same physical face, including the inner face.
}
