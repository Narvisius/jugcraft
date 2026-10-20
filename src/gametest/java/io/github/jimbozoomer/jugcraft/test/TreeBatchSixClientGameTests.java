package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.FruitingLeavesBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/** A saved batch-6 garden, harvest states, timber comparisons and twelve natural biome stops. */
public class TreeBatchSixClientGameTests extends TreeBatchTwoClientGameTests {
	private final Map<BlockPos, BlockState> savedStates = new LinkedHashMap<>();
	@Override protected List<String> shapes() { return TreeBatchSixGameTests.SHAPES; }
	@Override protected List<String> biomes() { return TreeBatchSixGameTests.BIOMES; }
	@Override protected String worldName() { return "Evergreen Scrub Inspection"; }
	@Override protected String exportName() { return "tree-batch-6-preview"; }
	@Override protected String screenshotPrefix() { return "jugcraft_tree6_"; }
	@Override protected boolean includeSubmergedColumns() { return true; }
	private boolean hasTarget(String name, Map<String,Integer> counts) {
		String leaf = name.equals("steppe") ? "sagebrush_leaves" : name.equals("heathland") ? "gorse_leaves" : "juniper_needles";
		return counts.getOrDefault("leaf:jugcraft:" + leaf, 0) > 0;
	}
	@Override protected BlockPos surveySpot(ServerLevel level, BlockPos first, ResourceKey<Biome> biome) {
		if (biome.identifier().getPath().equals("steppe")) {
			var features = new java.util.HashSet<String>();
			for (var step : level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME).getOrThrow(biome).value().getGenerationSettings().features())
				for (var feature : step) feature.unwrapKey().ifPresent(key -> features.add(key.identifier().toString()));
			if (!features.contains("jugcraft:trees_steppe") || !features.contains("jugcraft:patch_wild_corn")
					|| features.contains("jugcraft:patch_apple_tree") || features.contains("jugcraft:patch_peach_tree"))
				throw new AssertionError("Steppe crop/fruit injection is wrong: " + features);
		}
		var checked = new java.util.HashSet<BlockPos>();
		for (int[] offset : new int[][] {{0,0},{256,0},{-256,0},{0,256},{0,-256},{256,256},{-256,256},{256,-256},{-256,-256}}) {
			BlockPos at = offset[0] == 0 && offset[1] == 0 ? first : BiomeClientGameTests.surfaceSpot(level, first.offset(offset[0],0,offset[1]), biome);
			if (at == null || !checked.add(at)) continue;
			var counts = survey(level,at,biome,true);
			Jugcraft.LOGGER.info("[tree 6 natural sample] {} at {}: {}",biome.identifier(),at,counts);
			if (hasTarget(biome.identifier().getPath(),counts)) return at;
		}
		for (int[] offset : new int[][] {{4096,0},{-4096,0},{0,4096},{0,-4096}}) {
			var nearest=level.findClosestBiome3d(holder -> holder.is(biome),first.offset(offset[0],0,offset[1]),4096,64,64);
			BlockPos at=nearest==null?null:BiomeClientGameTests.surfaceSpot(level,nearest.getFirst(),biome);
			if(at==null || !checked.add(at)) continue;
			var counts=survey(level,at,biome,true);
			Jugcraft.LOGGER.info("[tree 6 regional sample] {} at {}: {}",biome.identifier(),at,counts);
			if(hasTarget(biome.identifier().getPath(),counts)) return at;
		}
		throw new AssertionError("No natural batch 6 vegetation in " + biome.identifier() + " after " + checked.size() + " samples");
	}
	@Override protected void validateSurvey(String biome, Map<String,Integer> counts) {
		if(!hasTarget(biome,counts)) throw new AssertionError("Missing natural vegetation: " + biome);
		// The survey assigns columns by their ground biome. A neighboring tree's canopy can
		// overhang Steppe without growing there (the CI sample had 20 apple leaves, no oak logs).
		// Apple and peach both have straight oak trunks; Steppe's shrubs use juniper/deadwood.
		// Keep the dynamic fruit-feature exclusions in surveySpot as the generation-policy check.
		if(biome.equals("steppe") && counts.getOrDefault("log:minecraft:oak_log", 0) > 0)
			throw new AssertionError("Oak trunk generated in Steppe's shrub-only survey: " + counts);
	}
	@Override protected void validateReopened(ServerLevel level) {
		savedStates.forEach((pos,state) -> {
			var actual = level.getBlockState(pos);
			// Native leaves recalculate their cached log distance as chunks/neighbours load.
			// Fruit, species, persistence and waterlogging must survive exactly; natural leaves must stay supported.
			if (!actual.is(state.getBlock()) || !actual.setValue(LeavesBlock.DISTANCE,state.getValue(LeavesBlock.DISTANCE)).equals(state)
					|| !actual.getValue(LeavesBlock.PERSISTENT) && actual.getValue(LeavesBlock.DISTANCE) >= LeavesBlock.DECAY_DISTANCE)
				throw new AssertionError("Saved berry/support state changed at " + pos + ": expected " + state + ", actual " + actual);
			if (!actual.equals(state)) Jugcraft.LOGGER.info("[tree 6 save] Leaf support settled at {}: {} -> {}",pos,state.getValue(LeavesBlock.DISTANCE),actual.getValue(LeavesBlock.DISTANCE));
		});
	}
	@Override protected void extraDisplays(ClientGameTestContext context, TestSingleplayerContext world, List<String> tour) {
		var wall=GARDEN.offset(0,0,80);
		var foliageWall=wall.offset(0,0,50);
		world.getServer().runOnServer(server -> {
			var level=server.overworld();
			for(var p:BlockPos.betweenClosed(wall.offset(-28,-1,-12),wall.offset(90,-1,72))) level.setBlock(p,Blocks.STONE.defaultBlockState(),Block.UPDATE_CLIENTS);
			PineWoodClientGameTests.comparisonRow(level,wall,0,new String[]{"jugcraft:juniper","minecraft:mangrove","minecraft:crimson","jugcraft:jacaranda","jugcraft:fruitwood","jugcraft:cypress"});
			for(int fruit=0;fruit<3;fruit++) {
				var pos=foliageWall.offset(fruit*5,2,0);
				var state=JugcraftAgriculture.block("juniper_needles").defaultBlockState().setValue(LeavesBlock.PERSISTENT,true).setValue(FruitingLeavesBlock.FRUIT,fruit);
				level.setBlock(pos,state,Block.UPDATE_CLIENTS); savedStates.put(pos,state);
				PineWoodClientGameTests.label(level,pos.offset(0,-2,2),"Juniper berries",List.of("Picked","Unripe","Ripe").get(fruit),"Right-click ripe");
			}
			for(int i=0;i<2;i++) {
				String name=i==0?"gorse":"sagebrush";
				level.setBlock(foliageWall.offset(20+i*5,2,0),JugcraftAgriculture.block(name+"_leaves").defaultBlockState().setValue(LeavesBlock.PERSISTENT,true),Block.UPDATE_CLIENTS);
				PineWoodClientGameTests.label(level,foliageWall.offset(20+i*5,0,2),name,"Evergreen foliage","");
			}
			// The authored garden starts with ripe, actually pickable fruit. Natural survey sites are untouched.
			for(int form=0;form<3;form++) for(int sample=0;sample<3;sample++) {
				var at=GARDEN.offset(form*40,0,-sample*40);
				for(var p:BlockPos.betweenClosed(at.offset(-12,0,-12),at.offset(12,12,12))) {
					var state=level.getBlockState(p);
					if(state.is(JugcraftAgriculture.block("juniper_needles")) && FruitingLeavesBlock.canFruit(state,level,p)) {
						var ripe=state.setValue(FruitingLeavesBlock.FRUIT,2); level.setBlock(p,ripe,Block.UPDATE_CLIENTS); savedStates.put(p.immutable(),ripe);
					}
				}
			}
		});
		shoot(context,world,wall.offset(4,7,34),wall.offset(4,2,0),screenshotPrefix()+"juniper_wood");
		shoot(context,world,wall.offset(-18,4,12),wall.offset(-18,2,0),screenshotPrefix()+"juniper_wood_close");
		for(boolean cutout:List.of(true,false)) {
			context.runOnClient(client -> client.options.cutoutLeaves().set(cutout));
			shoot(context,world,foliageWall.offset(12,5,15),foliageWall.offset(12,2,0),screenshotPrefix()+(cutout?"foliage_cutout":"foliage_solid"));
		}
		context.runOnClient(client -> client.options.cutoutLeaves().set(true));
		for(int i=0;i<shapes().size();i++) {
			String shape=shapes().get(i); var at=GARDEN.offset(i*40,0,0);
			shoot(context,world,at.offset(8,shape.contains("bush")||i>=3?3:5,11),at.above(i>=2?1:3),screenshotPrefix()+shape+"_close");
		}
		tour.add("Wood comparison: /tp @s 1022 181 -900 180 0");
		tour.add("Berry stages, gorse and sagebrush foliage: /tp @s 1012 181 -850 180 0");
		tour.add("Garden junipers have ripe pickable berries. Random ticks are frozen: /gamerule minecraft:random_tick_speed 3 restores regrowth and decay.");
		tour.add("Natural biome stops are unplanted samples, selected for target-species presence rather than population statistics. Batches 6b and 7 are not included.");
	}
}
