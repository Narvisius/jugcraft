package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import io.github.jimbozoomer.jugcraft.menagerie.Ecology;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Full normal-world tour, with separate planted shapes, harvest samples and natural biome stops. */
public class WalnutTreeClientGameTests extends TreeBatchTwoClientGameTests {
    private static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
    private final Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
    @Override protected List<String> shapes() { return WalnutTreeGameTests.SHAPES; }
    @Override protected List<String> biomes() { return List.of("dense_forest","woodland","basin"); }
    @Override protected String worldName() { return "Walnut and Bristlecone Inspection"; }
    @Override protected String exportName() { return "tree-batch-13-preview"; }
    @Override protected String screenshotPrefix() { return "jugcraft_tree13_"; }
    @Override protected Block gardenSoil(String shape) { return shape.equals("bristlecone") ? Blocks.GRAVEL : Blocks.GRASS_BLOCK; }
    private String target(String biome) { return biome.equals("basin") ? "leaf:jugcraft:pine_needles" : "leaf:jugcraft:walnut_leaves"; }
    @Override protected BlockPos surveySpot(ServerLevel level, BlockPos first, ResourceKey<Biome> biome) {
        for (int[] offset : new int[][]{{0,0},{256,0},{-256,0},{0,256},{0,-256},{512,0},{0,512}}) {
            var p=offset[0]==0&&offset[1]==0 ? first : BiomeClientGameTests.surfaceSpot(level,first.offset(offset[0],0,offset[1]),biome);
            if(p==null)continue;
            var counts=survey(level,p,biome);
            Jugcraft.LOGGER.info("[batch 13 natural] {} {} {}",biome.identifier(),p,counts);
            if(counts.getOrDefault(target(biome.identifier().getPath()),0)>0)return p;
        }
        return null;
    }
    @Override protected void validateSurvey(String biome, Map<String,Integer> counts) {
        check(counts.getOrDefault(target(biome),0)>0,"Missing natural batch 13 trees in "+biome);
        check(counts.getOrDefault("log:jugcraft:ecology_walnut_log",0)==0,"Old walnut worldgen remains");
    }
    @Override protected void extraDisplays(ClientGameTestContext context, TestSingleplayerContext world, List<String> tour) {
        world.getServer().runOnServer(server->{
            var level=server.overworld();
            for(var p:BlockPos.betweenClosed(GARDEN.offset(-18,-1,16),GARDEN.offset(90,-1,34)))
                level.setBlock(p,Blocks.STONE_BRICKS.defaultBlockState(),Block.UPDATE_CLIENTS);
            for(int i=0;i<3;i++) {
                var leaf=GARDEN.offset(i*5,1,28);
                level.setBlock(leaf.east(),JugcraftAgriculture.block("walnut_log").defaultBlockState(),Block.UPDATE_ALL);
                var state=JugcraftAgriculture.block("walnut_leaves").defaultBlockState()
                        .setValue(LeavesBlock.DISTANCE,1).setValue(FruitingLeavesBlock.FRUIT,i);
                level.setBlock(leaf,state,Block.UPDATE_ALL); saved.put(leaf,state);
                PineWoodClientGameTests.label(level,leaf.offset(0,-1,2),new String[]{"Resting leaves","Green husks","Ripe walnuts"}[i],
                        i==2?"Right-click to harvest":"Natural leaf sample");
            }
            for(int i=0;i<2;i++) {
                var p=GARDEN.offset(20+i*5,1,28);
                level.setBlock(p.east(),JugcraftAgriculture.block("walnut_log").defaultBlockState(),Block.UPDATE_ALL);
                var state=JugcraftAgriculture.block("walnut_leaves").defaultBlockState().setValue(LeavesBlock.DISTANCE,1)
                        .setValue(SeasonalLeavesBlock.SEASON,i==0?SeasonalLeavesBlock.Foliage.GOLD:SeasonalLeavesBlock.Foliage.BARE);
                level.setBlock(p,state,Block.UPDATE_ALL);
                // onPlace follows today's calendar; freeze these labelled comparison states afterwards.
                level.setBlock(p,state,Block.UPDATE_CLIENTS); saved.put(p,state);
                PineWoodClientGameTests.label(level,p.offset(0,-1,2),i==0?"Autumn gold":"Winter twigs","Season comparison");
            }
            String[] forms={"walnut_log","stripped_walnut_log","walnut_planks","walnut_cabinet","walnut_shelf","walnut_trapdoor"};
            for(int i=0;i<forms.length;i++){
                var p=GARDEN.offset(40+i*5,0,28); var state=Ecology.block(forms[i]).defaultBlockState();
                level.setBlock(p,state,Block.UPDATE_ALL); saved.put(p,state);
                PineWoodClientGameTests.label(level,p.offset(0,0,2),"Recovered "+forms[i].replace("walnut_",""),"Saved IDs preserved");
            }
            var legacy=GARDEN.offset(75,1,28);
            level.setBlock(legacy.east(),JugcraftAgriculture.block("walnut_log").defaultBlockState(),Block.UPDATE_ALL);
            var state=Ecology.block("walnut_leaves").defaultBlockState().setValue(LeavesBlock.DISTANCE,1).setValue(FruitingLeavesBlock.FRUIT,2);
            level.setBlock(legacy,state,Block.UPDATE_ALL);saved.put(legacy,state);
            PineWoodClientGameTests.label(level,legacy.offset(0,-1,2),"Recovered ripe leaves","Same nut + husk harvest");
        });
        for(String item:List.of("walnut_sapling 16","ecology_walnut_sapling 16","walnut_planks 64","ecology_walnut 16",
                "roasted_walnuts 16","walnut_husk 16","walnut_mooncake 16","ecology_walnut_door 8","ecology_walnut_boat",
                "ecology_walnut_sign 8","ecology_walnut_hanging_sign 8","cooking_pot"))
            world.getServer().runCommand("give @a jugcraft:"+item);
        for(String item:List.of("bone_meal 64","shears","iron_axe","campfire","wheat 16","sugar 16","egg 16"))
            world.getServer().runCommand("give @a minecraft:"+item);
        shoot(context,world,GARDEN.offset(10,3,35),GARDEN.offset(10,1,28),screenshotPrefix()+"harvest_and_seasons");
        shoot(context,world,GARDEN.offset(56,4,37),GARDEN.offset(56,1,28),screenshotPrefix()+"recovered_family");
        tour.add("Harvest and seasonal samples: /tp @s 1010 180 -966 180 0. Right-click ripe natural leaves for 1-2 walnuts and a husk.");
        tour.add("Recovered family samples: /tp @s 1055 180 -966 180 0. Existing blocks retain their IDs; timber drops and recipes share the approved family.");
        tour.add("Walnut: Dense Forest 10%, Woodland 4% of selections; one fifth large. Both old and new saplings grow the same 4:1 mixture.");
        tour.add("Basin bristlecone: one attempt per four chunks on supported gravel/stone/andesite, no dirt pedestal. Existing pine timber and saplings.");
        tour.add("Smelt/smoke/campfire-cook a raw Walnut into Roasted Walnuts. Cooking Pot over lit campfire: 2 wheat + sugar + egg + 2 roasted walnuts -> 4 Walnut Mooncakes.");
        tour.add("Craft Walnut Husk into Brown Dye; the husk is consumed. Raw/roasted walnuts feed squirrels.");
        tour.add("Random ticks are frozen for inspection. /gamerule minecraft:random_tick_speed 3 enables regrowth, decay and seasonal catch-up.");
    }
    @Override protected void validateReopened(ServerLevel level) {
        saved.forEach((p,s)->check(level.getBlockState(p).equals(s),"Sample changed across save/reopen: "+p));
    }
}
