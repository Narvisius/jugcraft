package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

public class RainforestClientGameTests extends TreeBatchTwoClientGameTests {
    private final Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
    @Override protected List<String> shapes() { return RainforestGameTests.SHAPES; }
    @Override protected List<String> biomes() { return List.of("rainforest", "tropics", "lush_grassland", "eucalyptus_forest", "temperate_rainforest"); }
    @Override protected String worldName() { return "Rainforest and Upgraded Tap Inspection"; }
    @Override protected String exportName() { return "tree-batch-12-tap-preview"; }
    @Override protected String screenshotPrefix() { return "jugcraft_tree12_"; }
    private List<String> targets(String biome) {
        return switch (biome) {
            case "rainforest" -> List.of("leaf:jugcraft:tree_fern_fronds", "leaf:jugcraft:rubber_leaves", "leaf:jugcraft:banana_leaves");
            case "tropics", "lush_grassland" -> List.of("leaf:jugcraft:banana_leaves");
            default -> List.of("leaf:jugcraft:tree_fern_fronds");
        };
    }
    @Override protected BlockPos surveySpot(ServerLevel level, BlockPos first, ResourceKey<Biome> biome) {
        var seen = new HashSet<BlockPos>();
        for (int[] offset : new int[][]{{0,0},{256,0},{-256,0},{0,256},{0,-256},{256,256},{-256,-256},{512,0},{0,512}}) {
            var p = offset[0]==0 && offset[1]==0 ? first : BiomeClientGameTests.surfaceSpot(level, first.offset(offset[0],0,offset[1]),biome);
            if(p==null || !seen.add(p)) continue;
            var counts=survey(level,p,biome);
            Jugcraft.LOGGER.info("[tree 12 natural] {} at {} {}",biome.identifier(),p,counts);
            if(targets(biome.identifier().getPath()).stream().allMatch(t->counts.getOrDefault(t,0)>0)) return p;
        }
        return null; // Continue the bounded regional survey; every biome must still pass validation.
    }
    @Override protected void validateSurvey(String biome, Map<String,Integer> survey) {
        for(var target: targets(biome)) if(survey.getOrDefault(target,0)==0) throw new AssertionError("Missing " + target + " in " + biome);
    }
    @Override protected void extraDisplays(ClientGameTestContext context, TestSingleplayerContext world, List<String> tour) {
        world.getServer().runOnServer(server -> {
            var level=server.overworld();
            for(int sample=0;sample<3;sample++) {
                var tap=GARDEN.offset(40,1,-sample*40-1);
                if(!TreeTapBlock.productive(level,tap,Direction.NORTH)) throw new AssertionError("Gallery rubber tree cannot be tapped");
                int fill = new int[]{2, 19, 32}[sample];
                var state=JugcraftAgriculture.block("tree_tap").defaultBlockState().setValue(TreeTapBlock.FACING,Direction.NORTH)
                        .setValue(TreeTapBlock.UPGRADED, sample > 0).setValue(TreeTapBlock.LATEX, fill);
                level.setBlock(tap,state,Block.UPDATE_ALL); saved.put(tap,state);
                PineWoodClientGameTests.label(level,tap.offset(3,-1,0),sample==0?"Basic tap: 50%":sample==1?"Bucket: 4 + 75%":"Bucket: 8/8 full",
                        sample==0?"Use empty bucket":"Use bottle stack");
            }
            // Keep the harvest display outside the measured tree plots (radius 16).
            for (var p : BlockPos.betweenClosed(GARDEN.offset(117,-1,16), GARDEN.offset(124,-1,25))) {
                level.setBlock(p,Blocks.GRASS_BLOCK.defaultBlockState(),Block.UPDATE_CLIENTS);
                level.setBlock(p.below(),Blocks.STONE.defaultBlockState(),Block.UPDATE_CLIENTS);
            }
            var leaf=GARDEN.offset(120,1,22);
            level.setBlock(leaf.below(),JugcraftAgriculture.block("banana_stem").defaultBlockState(),Block.UPDATE_ALL);
            var state=JugcraftAgriculture.block("banana_leaves").defaultBlockState().setValue(LeavesBlock.DISTANCE,1).setValue(OrchardLeavesBlock.FRUIT,2);
            level.setBlock(leaf,state,Block.UPDATE_ALL); saved.put(leaf,state);
            PineWoodClientGameTests.label(level,leaf.offset(3,-1,0),"Ripe banana leaves","Right-click to pick");
        });
        for (String item : List.of("jugcraft:tree_fern_sapling 16","jugcraft:rubber_tree_sapling 16","jugcraft:tree_tap 8","minecraft:glass_bottle 64",
                "minecraft:bucket 8","jugcraft:latex_bottle 8","jugcraft:cider_vinegar 8","jugcraft:cooking_pot","minecraft:campfire","jugcraft:banana_pup 16","minecraft:bone_meal 64","minecraft:shears","minecraft:iron_axe"))
            world.getServer().runCommand("give @a " + item);
        for (int sample=0; sample<3; sample++)
            shoot(context,world,GARDEN.offset(42,2,-sample*40-4),GARDEN.offset(40,1,-sample*40-1),screenshotPrefix()+"tap_"+sample);
        shoot(context,world,GARDEN.offset(105,20,25),GARDEN.offset(80,12,0),screenshotPrefix()+"mahogany_full");
        for(boolean cutout:List.of(true,false)) {
            context.runOnClient(client->client.options.cutoutLeaves().set(cutout));
            shoot(context,world,GARDEN.offset(6,3,9),GARDEN.offset(0,3,0),screenshotPrefix()+(cutout?"fronds_cutout":"fronds_solid"));
        }
        context.runOnClient(client->client.options.cutoutLeaves().set(true));
        tour.add("Tap samples: north side of each rubber tree. Basic half-full z-1001; bucket 4 bottles + 75% z-1041; bucket 8/8 full z-1081.");
        tour.add("Full bucket: /tp @s 1040 180 -1085 0 0. Use a bottle stack to collect up to eight at once, limited by bottles held and inventory space.");
        tour.add("Right-click a placed basic tap with an empty bucket to upgrade without losing partial latex or delaying the pending fill tick. Or craft Tree Tap + Bucket side by side.");
        tour.add("Empty-hand right-click shows whole bottles, capacity and partial fill. Collecting preserves leftover fractional latex. Breaking loses contents but keeps the bucket upgrade.");
        tour.add("One tap per living tree, on its bottom three logs. Four loaded minutes per bottle. Placed logs and leafless trees do not produce latex.");
        tour.add("Upgraded capacity is eight bottles / 32 loaded minutes. Cooking Pot over lit campfire: 1 Latex Bottle + 1 Cider Vinegar -> 1 Rubber in 30 seconds; returns two Glass Bottles.");
        tour.add("Bananas use the existing orchard leaves and pups. Right-click ripe leaves; /gamerule minecraft:random_tick_speed 3 enables regrowth.");
        tour.add("Tree tap scheduled ticks continue even while random ticks are frozen. Initial tap levels may advance while inspecting.");
    }
    @Override protected void validateReopened(ServerLevel level) {
        saved.forEach((p,state)-> {
            var actual=level.getBlockState(p);
            boolean match=state.getBlock() instanceof TreeTapBlock ? actual.is(state.getBlock())
                    && actual.getValue(TreeTapBlock.FACING)==state.getValue(TreeTapBlock.FACING)
                    && actual.getValue(TreeTapBlock.UPGRADED)==state.getValue(TreeTapBlock.UPGRADED)
                    && actual.getValue(TreeTapBlock.LATEX)>=state.getValue(TreeTapBlock.LATEX) : actual.equals(state);
            if(!match) throw new AssertionError("Display changed across save/reopen " + p);
        });
    }
}
