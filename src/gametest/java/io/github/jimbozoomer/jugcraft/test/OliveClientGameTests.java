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

/** An exported olive garden with real input harvesting, timber comparisons and untouched biome samples. */
public class OliveClientGameTests extends TreeBatchTwoClientGameTests {
	private final Map<BlockPos,BlockState> saved=new LinkedHashMap<>();
	@Override protected List<String> shapes(){return OliveGameTests.SHAPES;}
	@Override protected List<String> biomes(){return List.of("mediterranean_forest","lavender_field","scrubland");}
	@Override protected String worldName(){return "Olive Inspection";}
	@Override protected String exportName(){return "tree-batch-7-preview";}
	@Override protected String screenshotPrefix(){return "jugcraft_olive_";}
	@Override protected BlockPos surveySpot(ServerLevel level,BlockPos first,ResourceKey<Biome> biome) {
		for(int[] offset:new int[][]{{0,0},{256,0},{-256,0},{0,256},{0,-256},{256,256},{-256,-256}}){
			var at=offset[0]==0&&offset[1]==0?first:BiomeClientGameTests.surfaceSpot(level,first.offset(offset[0],0,offset[1]),biome);
			if(at==null)continue; var counts=survey(level,at,biome); Jugcraft.LOGGER.info("[olive natural sample] {} {} {}",biome.identifier(),at,counts);
			if(counts.getOrDefault(biome.identifier().getPath().equals("scrubland")?"leaf:jugcraft:wild_olive_leaves":"leaf:jugcraft:olive_leaves",0)>0)return at;
		}
		throw new AssertionError("No natural olive found in "+biome.identifier());
	}
	@Override protected void validateSurvey(String biome,Map<String,Integer> counts){
		if(counts.getOrDefault(biome.equals("scrubland")?"leaf:jugcraft:wild_olive_leaves":"leaf:jugcraft:olive_leaves",0)==0)throw new AssertionError("Olive missing in "+biome);
	}
	@Override protected void validateReopened(ServerLevel level){
		saved.forEach((p,state)->{var now=level.getBlockState(p); if(now.is(state.getBlock())&&now.hasProperty(LeavesBlock.DISTANCE))now=now.setValue(LeavesBlock.DISTANCE,state.getValue(LeavesBlock.DISTANCE)); if(!now.equals(state))throw new AssertionError("Olive state changed across save "+p);});
	}
	@Override protected void extraDisplays(ClientGameTestContext context,TestSingleplayerContext world,List<String> tour){
		var wood=GARDEN.offset(20,0,70); var wall=GARDEN.offset(0,0,125); var natural=wall.offset(34,1,0);
		world.getServer().runOnServer(server->{
			var level=server.overworld();
			for(var p:BlockPos.betweenClosed(wood.offset(-25,-1,-15),wall.offset(100,-1,20)))level.setBlock(p,Blocks.STONE.defaultBlockState(),Block.UPDATE_CLIENTS);
			PineWoodClientGameTests.comparisonRow(level,wood,0,new String[]{"jugcraft:olive","minecraft:birch","minecraft:oak","jugcraft:willow","jugcraft:chestnut"});
			PineWoodClientGameTests.comparisonRow(level,wood,24,new String[]{"jugcraft:olive","minecraft:crimson","minecraft:warped"});
			// Bamboo uses blocks instead of logs, so give it its own accurately named comparison.
			for(int x=0;x<3;x++)for(int y=1;y<4;y++)level.setBlock(wood.offset(12+x,y,24),new Block[]{Blocks.BAMBOO_BLOCK,Blocks.STRIPPED_BAMBOO_BLOCK,Blocks.BAMBOO_PLANKS}[x].defaultBlockState(),Block.UPDATE_CLIENTS);
			PineWoodClientGameTests.label(level,wood.offset(13,0,26),"Bamboo","Native reference");
			for(int stage=0;stage<4;stage++){
				var at=wall.offset(stage*8,2,0); var state=JugcraftAgriculture.block(stage==3?"wild_olive_leaves":"olive_leaves").defaultBlockState().setValue(LeavesBlock.PERSISTENT,true);
				if(stage<3)state=state.setValue(FruitingLeavesBlock.FRUIT,stage);
				for(int x=0;x<4;x++)for(int y=0;y<3;y++){var p=at.offset(x,y,0);level.setBlock(p,state,Block.UPDATE_CLIENTS);saved.put(p,state);}
				PineWoodClientGameTests.label(level,at.offset(1,-2,2),new String[]{"Picked","Unripe green","Ripe black","Wild / no fruit"}[stage],"Display only");
			}
			level.setBlock(natural.west(),JugcraftAgriculture.block("olive_log").defaultBlockState(),Block.UPDATE_CLIENTS);
			level.setBlock(natural,JugcraftAgriculture.block("olive_leaves").defaultBlockState().setValue(LeavesBlock.DISTANCE,1).setValue(FruitingLeavesBlock.FRUIT,2),Block.UPDATE_CLIENTS);
			PineWoodClientGameTests.label(level,natural.offset(1,-1,2),"Pick with hand","Natural ripe leaf","Ticks paused");
			for(var p:BlockPos.betweenClosed(GARDEN.offset(-12,0,-92),GARDEN.offset(92,10,12))){
				var state=level.getBlockState(p);
				if(state.is(JugcraftAgriculture.block("olive_leaves"))&&FruitingLeavesBlock.canFruit(state,level,p)){
					var ripe=state.setValue(FruitingLeavesBlock.FRUIT,2);level.setBlock(p,ripe,Block.UPDATE_CLIENTS);saved.put(p.immutable(),ripe);
				}
			}
			var potAt=wall.offset(42,0,0);level.setBlock(potAt,Blocks.CAMPFIRE.defaultBlockState(),Block.UPDATE_CLIENTS);
			level.setBlock(potAt.above(),JugcraftAgriculture.block("cooking_pot").defaultBlockState(),Block.UPDATE_CLIENTS);
			var pot=(CookingPotBlockEntity)level.getBlockEntity(potAt.above());
			pot.setItem(0,new ItemStack(JugcraftAgriculture.item("mason_jar")));pot.setItem(1,new ItemStack(JugcraftAgriculture.item("olives"),4));pot.setItem(2,new ItemStack(JugcraftAgriculture.item("cider_vinegar")));
			PineWoodClientGameTests.label(level,potAt.offset(1,0,2),"Pickled olives","1 jar + 4 olives","1 cider vinegar");
			var player=server.getPlayerList().getPlayers().getFirst();
			for(String id:List.of("olive_sapling","olive_bush_sapling","olive_planks","olives","mason_jar","cider_vinegar","pickled_olives","cooking_pot"))player.getInventory().add(new ItemStack(JugcraftAgriculture.item(id),id.equals("pickled_olives")?1:16));
			player.getInventory().add(new ItemStack(Items.BONE_MEAL,64));
		});
		world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);});
		world.getServer().runCommand("tp @a "+(natural.getX()+.5)+" "+(natural.getY()-1)+" "+(natural.getZ()+2.5));
		context.waitTicks(10);context.getInput().lookAt(natural);context.waitTicks(3);context.getInput().pressKey(o->o.keyUse);context.waitTicks(10);
		world.getServer().runOnServer(server->{var level=server.overworld();if(level.getBlockState(natural).getValue(FruitingLeavesBlock.FRUIT)!=0)throw new AssertionError("Actual empty-hand input failed olive harvest");});
		context.getInput().pressKey(o->o.keyUse);context.waitTicks(3);
		world.getServer().runOnServer(server->{var level=server.overworld();if(level.getBlockState(natural).getValue(FruitingLeavesBlock.FRUIT)!=0)throw new AssertionError("Immediate repeat changed fruit");var ripe=level.getBlockState(natural).setValue(FruitingLeavesBlock.FRUIT,2);level.setBlock(natural,ripe,Block.UPDATE_CLIENTS);saved.put(natural,ripe);server.getPlayerList().getPlayers().getFirst().setGameMode(GameType.CREATIVE);});
		shoot(context,world,wood.offset(0,7,17),wood.offset(0,2,0),screenshotPrefix()+"wood");
		shoot(context,world,wood.offset(1,7,41),wood.offset(1,2,24),screenshotPrefix()+"wood_bamboo");
		shoot(context,world,wood.offset(-18,4,9),wood.offset(-18,2,0),screenshotPrefix()+"wood_close");
		for(boolean cutout:List.of(true,false)){context.runOnClient(c->c.options.cutoutLeaves().set(cutout));shoot(context,world,wall.offset(13,5,18),wall.offset(13,3,0),screenshotPrefix()+(cutout?"leaves_cutout":"leaves_solid"));}
		context.runOnClient(c->c.options.cutoutLeaves().set(true));
		for(int i=0;i<shapes().size();i++){var row=GARDEN.offset(i*40,0,0);shoot(context,world,row.offset(11,5,12),row.above(3),screenshotPrefix()+shapes().get(i)+"_close");}
		tour.add("Timber: /tp @s 1020 181 -917 180 0; bamboo and Nether timber comparisons farther south.");
		tour.add("Harvest demo: /tp @s 1034 180 -872 180 0. Empty-hand right-click the marked natural ripe leaf. Flat panels are decorative only.");
		tour.add("Grow one olive sapling for an ordinary olive; four in a 2x2 square grow an ancient olive. Wild Olive Bush Saplings remain fruitless.");
		tour.add("Pickled olives: one Mason Jar, four olives, one cider vinegar in a heated Cooking Pot, 300 ticks; returns the vinegar bottle. Four servings return the jar.");
		tour.add("Random ticks are paused for inspection. /gamerule minecraft:random_tick_speed 3 restores ripening, growth and decay. Evergreen olives work with seasons off. Later batches are unstarted.");
		tour.add("Eligible leaves on the authored gallery trees were set ripe for inspection. Natural biome samples were left untouched.");
	}
}
