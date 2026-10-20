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

/** Real tree features, harvestable displays, and untouched natural stops in a normal world. */
public class TropicalTreeClientGameTests extends TreeBatchTwoClientGameTests {
    private final Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
    @Override protected List<String> shapes() { return TropicalTreeGameTests.SHAPES; }
    @Override protected List<String> biomes() { return List.of("tropics", "jacaranda_glade", "overgrown_beach", "karst_pinnacles", "shrine_springs", "lush_grassland", "dune_beach", "gravel_beach"); }
    @Override protected String worldName() { return "Tropical Trees and Coastal Shrubs Inspection"; }
    @Override protected String exportName() { return "tree-batch-11-preview-r4"; }
    @Override protected String screenshotPrefix() { return "jugcraft_tree11_"; }
    private String expected(String biome) { return switch (biome) {
        case "tropics", "jacaranda_glade" -> "leaf:jugcraft:flame_tree_leaves";
        case "overgrown_beach" -> "leaf:jugcraft:sea_grape_leaves";
        case "karst_pinnacles" -> "leaf:jugcraft:osmanthus_leaves";
        case "shrine_springs" -> "leaf:jugcraft:banyan_leaves";
        case "lush_grassland" -> "leaf:jugcraft:mango_leaves";
        default -> "log:jugcraft:stripped_dead_log";
    }; }
    @Override protected BlockPos surveySpot(ServerLevel level, BlockPos first, ResourceKey<Biome> biome) {
        var visited = new HashSet<BlockPos>();
        for (int[] offset : new int[][] {{0, 0}, {256, 0}, {-256, 0}, {0, 256}, {0, -256}, {256, 256}, {-256, -256}, {256, -256}, {-256, 256}}) {
            var at = offset[0] == 0 && offset[1] == 0 ? first : BiomeClientGameTests.surfaceSpot(level, first.offset(offset[0], 0, offset[1]), biome);
            if (at == null || !visited.add(at)) continue;
            var counts = survey(level, at, biome);
            int count = counts.getOrDefault(expected(biome.identifier().getPath()), 0);
            Jugcraft.LOGGER.info("[tree 11 natural] {} at {} target blocks {}", biome.identifier(), at, count);
            if (count > 0) return at;
        }
        // Thin coast strips need independent beach locations, not repeatedly sampling the same
        // small inland dune. Keep a finite search; never plant specimens in a natural survey.
        if (biome.identifier().getPath().equals("dune_beach") || biome.identifier().getPath().equals("gravel_beach")) {
            for (int ring = 1; ring <= 4; ring++) for (int[] direction : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}}) {
                var from = first.offset(direction[0] * ring * 2048, 0, direction[1] * ring * 2048);
                var near = level.findClosestBiome3d(holder -> holder.is(biome), from, 4096, 64, 64);
                if (near == null) continue;
                var at = BiomeClientGameTests.surfaceSpot(level, near.getFirst(), biome);
                if (at == null || !visited.add(at)) continue;
                var counts = survey(level, at, biome);
                int count = counts.getOrDefault(expected(biome.identifier().getPath()), 0);
                Jugcraft.LOGGER.info("[tree 11 natural coast] {} at {} target blocks {}", biome.identifier(), at, count);
                if (count > 0) return at;
            }
            return first; // The tour reports an honest empty sample if the bounded search found none.
        }
        return null; // Continue the bounded regional survey; every biome must still pass validation.
    }
    @Override protected void validateSurvey(String biome, Map<String, Integer> survey) {
        if (!biome.equals("dune_beach") && !biome.equals("gravel_beach") && survey.getOrDefault(expected(biome), 0) == 0)
            throw new AssertionError("Missing natural species in " + biome);
    }
    @Override protected void extraDisplays(ClientGameTestContext context, TestSingleplayerContext world, List<String> tour) {
        var wall = GARDEN.offset(0, 0, 75);
        world.getServer().runOnServer(server -> {
            var level = server.overworld();
            for (var p : BlockPos.betweenClosed(wall.offset(-16, -1, -15), wall.offset(95, -1, 32))) level.setBlock(p, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            PineWoodClientGameTests.comparisonRow(level, wall, 0, new String[]{"jugcraft:fig", "jugcraft:mango", "minecraft:oak", "jugcraft:olive", "minecraft:acacia"});
            int rootCount = 0;
            for (var p : BlockPos.betweenClosed(GARDEN.offset(102, 0, -98), GARDEN.offset(138, 18, 18))) {
                var state = level.getBlockState(p);
                if (state.is(Blocks.HANGING_ROOTS)) { saved.put(p.immutable(), state); rootCount++; }
            }
            if (rootCount == 0) throw new AssertionError("No aerial roots in the banyan comparison row");
            int index = 0;
            for (String tree : List.of("sea_grape", "osmanthus", "mango")) {
                for (int fruit = 0; fruit < 3; fruit++) {
                    var p = wall.offset(index * 24 + fruit * 6, 2, 17);
                    level.setBlock(p.north(), (tree.equals("mango") ? JugcraftAgriculture.block("mango_log") : tree.equals("osmanthus") ? JugcraftAgriculture.block("olive_log") : Blocks.OAK_LOG).defaultBlockState(), Block.UPDATE_CLIENTS);
                    var state = JugcraftAgriculture.block(tree + "_leaves").defaultBlockState().setValue(LeavesBlock.DISTANCE, 1).setValue(FruitingLeavesBlock.FRUIT, fruit);
                    level.setBlock(p, state, Block.UPDATE_CLIENTS); saved.put(p, state);
                    PineWoodClientGameTests.label(level, p.offset(0, -2, 2), tree.replace('_', ' '), fruit == 2 ? "Ripe: right-click" : "Growth stage " + fruit);
                }
                index++;
            }
            // A labelled shore specimen shows all log lengths without pretending to be natural terrain.
            for (int length = 3; length <= 6; length++) {
                var p = wall.offset(75, 0, (length - 3) * 7);
                for (int i = 0; i < length; i++) {
                    level.setBlock(p.offset(i, -1, 0), Blocks.SAND.defaultBlockState(), Block.UPDATE_CLIENTS);
                    level.setBlock(p.offset(i, 0, 0), JugcraftAgriculture.block("stripped_dead_log").defaultBlockState().setValue(RotatedPillarBlock.AXIS, net.minecraft.core.Direction.Axis.X), Block.UPDATE_CLIENTS);
                }
                PineWoodClientGameTests.label(level, p.offset(0, 0, 2), "Driftwood specimen", length + " blocks; no stump");
            }
        });
        for (String item : List.of("jugcraft:flame_tree_sapling 16", "jugcraft:sea_grape_sapling 16", "jugcraft:osmanthus_sapling 16", "jugcraft:banyan_sapling 16", "jugcraft:mango_sapling 16", "minecraft:bone_meal 64", "minecraft:shears", "minecraft:iron_axe", "jugcraft:sea_grape_jelly", "jugcraft:mango_chutney", "jugcraft:osmanthus_mooncake 4"))
            world.getServer().runCommand("give @a " + item);
        shoot(context, world, GARDEN.offset(132, 3, 14), GARDEN.offset(120, 4, 0), screenshotPrefix() + "banyan_branches");
        shoot(context, world, wall.offset(24, 7, 32), wall.offset(24, 2, 0), screenshotPrefix() + "wood_and_harvests");
        for (int index = 0; index < 3; index++) shoot(context, world, wall.offset(index * 24 + 6, 3, 26), wall.offset(index * 24 + 6, 2, 17), screenshotPrefix() + "harvest_" + index);
        for (boolean cutout : List.of(true, false)) {
            context.runOnClient(client -> client.options.cutoutLeaves().set(cutout));
            shoot(context, world, GARDEN.offset(140, 8, 22), GARDEN.offset(120, 6, 0), screenshotPrefix() + (cutout ? "cutout" : "solid"));
        }
        context.runOnClient(client -> client.options.cutoutLeaves().set(true));
        tour.add("Wood comparison and reachable fruit/flower stages: /tp @s 1024 180 -900 180 0");
        tour.add("Ripe display leaves are natural-state leaves supported by logs. Right-click the ripe block by hand or with shears. Placed decorative leaves do not yield harvests.");
        tour.add("Driftwood length specimens: /tp @s 1078 180 -899 180 0; authored displays, not natural shore finds.");
        tour.add("The save freezes growth. /gamerule minecraft:random_tick_speed 3 restores fruit/flower regrowth and normal leaf decay.");
        tour.add("Natural beach stop counts show whether driftwood was found; zero is an honest empty rare-feature sample.");
    }
    @Override protected void validateReopened(ServerLevel level) {
        saved.forEach((pos, state) -> { if (!level.getBlockState(pos).equals(state)) throw new AssertionError("Harvest display changed across save/reopen " + pos); });
    }
}
