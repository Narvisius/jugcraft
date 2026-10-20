package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Batch 14: real orchard features, reachable fruit stages and untouched natural biome stops. */
public class OrchardWorldgenClientGameTests extends TreeBatchTwoClientGameTests {
    private final Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
    @Override protected List<String> shapes() { return OrchardWorldgenGameTests.SHAPES; }
    @Override protected List<String> biomes() { return List.of("orchard", "subtropics", "mediterranean_forest"); }
    @Override protected String worldName() { return "Orchard Fruit Inspection"; }
    @Override protected String exportName() { return "tree-batch-14-preview"; }
    @Override protected String screenshotPrefix() { return "jugcraft_tree14_"; }
    private List<String> expected(String biome) {
        return biome.equals("orchard") ? List.of("apple", "pear", "peach") : List.of("lemon", "orange");
    }
    private boolean hasSpecies(String biome, Map<String, Integer> counts) {
        return expected(biome).stream().allMatch(tree -> counts.getOrDefault("leaf:jugcraft:" + tree + "_leaves", 0) > 0);
    }
    @Override protected BlockPos surveySpot(ServerLevel level, BlockPos first, ResourceKey<Biome> biome) {
        var visited = new HashSet<BlockPos>();
        for (int[] offset : new int[][] {{0,0},{256,0},{-256,0},{0,256},{0,-256},{256,256},{-256,-256},{256,-256},{-256,256}}) {
            var at = offset[0] == 0 && offset[1] == 0 ? first : BiomeClientGameTests.surfaceSpot(level, first.offset(offset[0],0,offset[1]), biome);
            if (at == null || !visited.add(at)) continue;
            var counts = survey(level, at, biome);
            Jugcraft.LOGGER.info("[batch 14 natural] {} at {}: {}", biome.identifier(), at, counts);
            if (hasSpecies(biome.identifier().getPath(), counts)) return at;
        }
        return null;
    }
    @Override protected void validateSurvey(String biome, Map<String, Integer> counts) {
        if (!hasSpecies(biome, counts)) throw new AssertionError("Missing orchard species in natural " + biome);
    }
    @Override protected void extraDisplays(ClientGameTestContext context, TestSingleplayerContext world, List<String> tour) {
        var display = GARDEN.offset(0,0,40);
        world.getServer().runOnServer(server -> {
            var level = server.overworld();
            // Connect the tree rows to a safe walking area for the fruit and processing displays.
            for (var p : BlockPos.betweenClosed(GARDEN.offset(-20,-1,15), GARDEN.offset(180,-1,64)))
                level.setBlock(p, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            int index=0;
            for (String shape : shapes()) {
                String tree=OrchardWorldgenGameTests.tree(shape);
                for (int fruit=0;fruit<3;fruit++) {
                    var p=display.offset(index*36+fruit*7,2,0);
                    for(int support=0;support<3;support++) level.setBlock(p.north().below(support),OrchardWorldgenGameTests.log(shape).defaultBlockState(),Block.UPDATE_CLIENTS);
                    var state=JugcraftAgriculture.block(tree+"_leaves").defaultBlockState()
                            .setValue(LeavesBlock.DISTANCE,1).setValue(FruitingLeavesBlock.FRUIT,fruit);
                    level.setBlock(p,state,Block.UPDATE_CLIENTS);saved.put(p,state);
                    PineWoodClientGameTests.label(level,p.offset(0,-2,3),tree,fruit==2?"Ripe: right-click":"Growth stage "+fruit);
                }
                index++;
            }
            level.setBlock(display.offset(5,0,15),JugcraftAgriculture.block("cider_press").defaultBlockState(),Block.UPDATE_CLIENTS);
            PineWoodClientGameTests.label(level,display.offset(5,0,17),"Pear cider press","Add pears by hand","Empty hand: crank","Bottle the juice");
            level.setBlock(display.offset(18,-1,15),Blocks.CAMPFIRE.defaultBlockState(),Block.UPDATE_CLIENTS);
            level.setBlock(display.offset(18,0,15),JugcraftAgriculture.block("candy_kettle").defaultBlockState(),Block.UPDATE_CLIENTS);
            PineWoodClientGameTests.label(level,display.offset(18,0,17),"Orange candy","Milk + sugar + orange","Tray at 116 C: fudge","Orange: Jump Boost");
        });
        for (String tree : List.of("apple","pear","peach","lemon","orange")) {
            world.getServer().runCommand("give @a jugcraft:"+tree+"_sapling 16");
            world.getServer().runCommand("give @a "+(tree.equals("apple")?"minecraft:":"jugcraft:")+tree+" 16");
        }
        for(String item:List.of("minecraft:bone_meal 64","minecraft:glass_bottle 32","minecraft:sugar 32","minecraft:milk_bucket","jugcraft:candy_tray 16","minecraft:iron_axe","minecraft:shears"))
            world.getServer().runCommand("give @a "+item);
        for(int index=0;index<5;index++)shoot(context,world,display.offset(index*36+7,3,12),display.offset(index*36+7,2,0),screenshotPrefix()+"fruit_stages_"+index);
        for(boolean cutout:List.of(true,false)) {
            context.runOnClient(client->client.options.cutoutLeaves().set(cutout));
            shoot(context,world,GARDEN.offset(60,7,17),GARDEN.offset(40,4,0),screenshotPrefix()+(cutout?"cutout":"solid"));
        }
        context.runOnClient(client->client.options.cutoutLeaves().set(true));
        tour.add("Reachable fruit stages: /tp @s 1007 180 -949 180 0. Each species shows resting, blossom and ripe leaves, supported by its actual timber.");
        tour.add("Right-click ripe leaves by hand or with shears. These displays have air below and are natural-state leaves; decorative player-placed leaves do not yield fruit.");
        tour.add("Pear cider press and orange candy kettle: /tp @s 1011 180 -941 180 0. Pears use the existing cider process and make sweet cider; orange is a short Jump Boost candy flavour.");
        tour.add("Saplings, fruit, bottles and tools are supplied. /gamerule minecraft:random_tick_speed 3 restores fruit growth and leaf decay; growth is frozen for inspection.");
        tour.add("Apple bee nests use a 2% decorator, so an individual tree need not have a nest. Natural biome stop counts are from untouched terrain.");
    }
    @Override protected void validateReopened(ServerLevel level) {
        saved.forEach((pos,state)->{if(!level.getBlockState(pos).equals(state))throw new AssertionError("Fruit display changed across save/reopen: "+pos);});
    }
}
