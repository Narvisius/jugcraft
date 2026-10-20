package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** One ordinary-world export for all three dry-country batches, with real input and save/reopen checks. */
public class DryCountryTreeClientGameTests extends TreeBatchTwoClientGameTests {
	private final Map<BlockPos,BlockState> saved=new LinkedHashMap<>();
	@Override protected List<String> shapes(){return DryCountryTreeGameTests.SHAPES;}
	@Override protected List<String> biomes(){return List.of("alpine_spawn","mediterranean_forest","heathland","dryland","canyon","eucalyptus_forest","gilded_shrubland","wasteland","oasis","dry_river","lush_desert","xeric_shrubland");}
	@Override protected String worldName(){return "Dry Country Tree Inspection";}
	@Override protected String exportName(){return "tree-batch-8abc-preview";}
	@Override protected String screenshotPrefix(){return "jugcraft_dry_trees_";}
	private String expectedLeaf(String biome){return switch(biome){case "eucalyptus_forest","gilded_shrubland"->"wattle_leaves";case "wasteland","oasis","dry_river"->"tamarisk_leaves";case "lush_desert","xeric_shrubland"->"joshua_tree_fronds";default->"stone_pine_needles";};}
	@Override protected BlockPos surveySpot(ServerLevel level,BlockPos first,ResourceKey<Biome> biome){
		for(int[] offset:new int[][]{{0,0},{256,0},{-256,0},{0,256},{0,-256},{256,256},{-256,-256}}){
			var at=offset[0]==0&&offset[1]==0?first:BiomeClientGameTests.surfaceSpot(level,first.offset(offset[0],0,offset[1]),biome);
			if(at==null)continue;var counts=survey(level,at,biome);Jugcraft.LOGGER.info("[dry-country natural sample] {} {} {}",biome.identifier(),at,counts);
			if(counts.getOrDefault("leaf:jugcraft:"+expectedLeaf(biome.identifier().getPath()),0)>0)return at;
		}
		return null; // Continue the bounded regional survey; every biome must still pass validation.
	}
	@Override protected void validateSurvey(String biome,Map<String,Integer> counts){if(counts.getOrDefault("leaf:jugcraft:"+expectedLeaf(biome),0)==0)throw new AssertionError("Missing tree in "+biome);}
	@Override protected void validateReopened(ServerLevel level){saved.forEach((pos,state)->{var now=level.getBlockState(pos);if(now.is(state.getBlock())&&now.hasProperty(LeavesBlock.DISTANCE))now=now.setValue(LeavesBlock.DISTANCE,state.getValue(LeavesBlock.DISTANCE));if(!now.equals(state))throw new AssertionError("Dry-country display changed after save "+pos);});}
	@Override protected void extraDisplays(ClientGameTestContext context,TestSingleplayerContext world,List<String> tour){
		var timber=GARDEN.offset(20,0,65);var wall=GARDEN.offset(0,0,125);var fruit=wall.offset(38,1,0);
		world.getServer().runOnServer(server->{
			var level=server.overworld();for(var p:BlockPos.betweenClosed(timber.offset(-25,-1,-10),wall.offset(100,-1,20)))level.setBlock(p,Blocks.STONE.defaultBlockState(),Block.UPDATE_CLIENTS);
			PineWoodClientGameTests.comparisonRow(level,timber,0,new String[]{"jugcraft:tamarisk","jugcraft:fruitwood","jugcraft:jacaranda","minecraft:crimson"});
			PineWoodClientGameTests.comparisonRow(level,timber,24,new String[]{"jugcraft:yucca","jugcraft:palm","jugcraft:cedar","minecraft:spruce"});
			for(int i=0;i<6;i++){
				var state=JugcraftAgriculture.block(i<3?"stone_pine_needles":i==3?"wattle_leaves":i==4?"tamarisk_leaves":"joshua_tree_fronds").defaultBlockState().setValue(LeavesBlock.PERSISTENT,true);
				if(i<3)state=state.setValue(FruitingLeavesBlock.FRUIT,i);
				for(int x=0;x<4;x++)for(int y=0;y<3;y++){var p=wall.offset(i*6+x,1+y,0);level.setBlock(p,state,Block.UPDATE_CLIENTS);saved.put(p,state);}
				PineWoodClientGameTests.label(level,wall.offset(i*6+1,0,2),i<3?"Pine cone stage "+i:i==3?"Wattle":i==4?"Tamarisk":"Joshua fronds","Display only");
			}
			level.setBlock(fruit.west(),JugcraftAgriculture.block("pine_log").defaultBlockState(),Block.UPDATE_CLIENTS);
			level.setBlock(fruit,JugcraftAgriculture.block("stone_pine_needles").defaultBlockState().setValue(LeavesBlock.DISTANCE,1).setValue(FruitingLeavesBlock.FRUIT,2),Block.UPDATE_CLIENTS);
			PineWoodClientGameTests.label(level,fruit.offset(1,-1,2),"Pick pine nuts","Empty hand","Natural ripe leaf");
			level.setBlock(fruit.offset(5,-1,0),Blocks.CAMPFIRE.defaultBlockState(),Block.UPDATE_CLIENTS);
			PineWoodClientGameTests.label(level,fruit.offset(5,-1,2),"Roast pine nuts","Use campfire");
			for(var p:BlockPos.betweenClosed(GARDEN.offset(-12,0,-92),GARDEN.offset(92,14,12))){var state=level.getBlockState(p);if(state.is(JugcraftAgriculture.block("stone_pine_needles"))&&FruitingLeavesBlock.canFruit(state,level,p))level.setBlock(p,state.setValue(FruitingLeavesBlock.FRUIT,2),Block.UPDATE_CLIENTS);}
			var player=server.getPlayerList().getPlayers().getFirst();for(String id:List.of("stone_pine_sapling","wattle_sapling","tamarisk_sapling","joshua_tree_sapling","tamarisk_planks","yucca_planks","pine_nuts","roasted_pine_nuts"))player.getInventory().add(new ItemStack(JugcraftAgriculture.item(id),16));player.getInventory().add(new ItemStack(Items.BONE_MEAL,64));
			player.setGameMode(GameType.SURVIVAL);player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
		});
		world.getServer().runCommand("tp @a "+(fruit.getX()+.5)+" "+(fruit.getY()-1)+" "+(fruit.getZ()+2.5));context.waitTicks(10);context.getInput().lookAt(fruit);context.waitTicks(3);context.getInput().pressKey(o->o.keyUse);context.waitTicks(10);
		world.getServer().runOnServer(server->{var level=server.overworld();if(level.getBlockState(fruit).getValue(FruitingLeavesBlock.FRUIT)!=0)throw new AssertionError("Empty-hand pine-nut harvest failed");});
		context.getInput().pressKey(o->o.keyUse);context.waitTicks(3);
		world.getServer().runOnServer(server->{var level=server.overworld();if(level.getBlockState(fruit).getValue(FruitingLeavesBlock.FRUIT)!=0)throw new AssertionError("Immediate repeat changed fruit");var ripe=level.getBlockState(fruit).setValue(FruitingLeavesBlock.FRUIT,2);level.setBlock(fruit,ripe,Block.UPDATE_CLIENTS);saved.put(fruit,ripe);server.getPlayerList().getPlayers().getFirst().setGameMode(GameType.CREATIVE);});
		shoot(context,world,timber.offset(0,7,17),timber.offset(0,2,0),screenshotPrefix()+"tamarisk_wood");
		shoot(context,world,timber.offset(0,7,41),timber.offset(0,2,24),screenshotPrefix()+"yucca_wood");
		for(boolean cutout:List.of(true,false)){context.runOnClient(c->c.options.cutoutLeaves().set(cutout));shoot(context,world,wall.offset(16,5,20),wall.offset(16,2,0),screenshotPrefix()+(cutout?"leaves_cutout":"leaves_solid"));}context.runOnClient(c->c.options.cutoutLeaves().set(true));
		for(int i=0;i<shapes().size();i++){var at=GARDEN.offset(i*40,0,0);shoot(context,world,at.offset(10,6,12),at.above(3),screenshotPrefix()+shapes().get(i)+"_close");}
		tour.add("Timber comparisons: /tp @s 1020 181 -918 180 0. Yucca row is 24 blocks farther south.");
		tour.add("Harvest: /tp @s 1038 180 -872 180 0. Empty-hand right-click the marked natural leaf; flat panels are decorative only.");
		tour.add("Pine nuts roast on the campfire. Stone Pine Saplings grow three nut-pine forms; Wattle Saplings grow trees and bushes. Tamarisk and Joshua saplings grow on dry sand/dirt soils.");
		tour.add("Random ticks are paused. /gamerule minecraft:random_tick_speed 3 restores natural growth, regrowth and decay. Natural biome stops were not planted by the test. Batch 9 is not included.");
	}
}
