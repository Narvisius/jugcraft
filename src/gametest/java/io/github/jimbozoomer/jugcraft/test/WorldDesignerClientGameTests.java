package io.github.jimbozoomer.jugcraft.test;

import com.mojang.serialization.JsonOps;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.town.TownData;
import io.github.jimbozoomer.jugcraft.town.TownPlanner;
import io.github.jimbozoomer.jugcraft.town.TownState;
import io.github.jimbozoomer.jugcraft.world.AlpineSpawn;
import io.github.jimbozoomer.jugcraft.world.design.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** Real terrain generation, custom biome paint, spawn/city planning, and codec persistence after reopening. */
public class WorldDesignerClientGameTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        final TestWorldSave save;
        try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(creator -> {
            creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            creator.setWorldType(new WorldCreationUiState.WorldTypeEntry(creator.getSettings().worldgenLoadContext()
                    .lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL)));
            creator.setSeed("jugcraft-designer");
            creator.updateDimensions((registries, dimensions) -> {
                var heights = new ArrayList<Float>(); var cells = new ArrayList<Integer>();
                for (int z = 0; z < 33; z++) for (int x = 0; x < 33; x++) { heights.add(x < 16 ? 80f : 128f); cells.add(x < 16 ? 1 : 2); }
                var grid = new DesignGrid(-512, -512, 32, 33, heights);
                var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
                var original = registries.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD).value();
                var settings = NoiseGeneratorSettings.DIRECT_CODEC.encodeStart(ops, original).getOrThrow().getAsJsonObject();
                var router = settings.getAsJsonObject("noise_router");
                var land = new com.google.gson.JsonObject(); land.addProperty("type", "jugcraft:design_density");
                land.add("grid", DesignGrid.CODEC.encodeStart(ops, grid).getOrThrow()); land.add("input", router.get("final_density"));
                var surface = new com.google.gson.JsonObject(); surface.addProperty("type", "jugcraft:design_density"); surface.addProperty("surface", true);
                surface.add("grid", land.get("grid")); surface.add("input", router.get("chunk_surface_level"));
                router.add("final_density", land); router.add("chunk_surface_level", surface);
                if (settings.has("aquifers")) settings.getAsJsonObject("aquifers").add("surface_level", surface);
                var noise = NoiseGeneratorSettings.DIRECT_CODEC.parse(ops, settings).getOrThrow();
                var biomes = registries.lookupOrThrow(Registries.BIOME);
                var source = new DesignBiomeSource(dimensions.overworld().getBiomeSource(), grid,
                        List.of(biomes.getOrThrow(Biomes.DESERT), biomes.getOrThrow(AlpineSpawn.BIOME)), cells,
                        new BlockPos(-128, 0, 0), Optional.of(new BlockPos(256, 0, 256)));
                var generator = new NoiseBasedChunkGenerator(source, Holder.direct(noise));
                // Round-trip the exact generator codec that level.dat persists, before creating any chunks.
                var decoded = ChunkGenerator.CODEC.parse(ops, ChunkGenerator.CODEC.encodeStart(ops, generator).getOrThrow()).getOrThrow();
                return dimensions.replaceOverworldGenerator(registries, decoded);
            });
        }).create()) {
            verify(world);
            world.getServer().runCommand("jugcraft design export");
            world.getServer().runCommand("gamerule minecraft:send_command_feedback false");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            });
            world.getServer().runCommand("setblock -40 159 40 minecraft:barrier");
            world.getServer().runCommand("tp @a -40 160 40 -90 45");
            context.waitTicks(100);
            context.waitFor(client -> !client.level.getBlockState(new BlockPos(-40, 76, 40)).isAir(), 400);
            world.getConnection().waitForChunksRender();
            context.getInput().lookAt(new BlockPos(0, 100, 0));
            context.waitTicks(10);
            context.runOnClient(client -> {
                check(!client.level.getBlockState(new BlockPos(-40, 76, 40)).isAir(), "Terrain did not reach the client");
                Jugcraft.LOGGER.info("[designer-check] Client camera at {} looking {}/{}", client.player.position(), client.player.getYRot(), client.player.getXRot());
                if (!client.gui.hud.isHidden()) client.gui.hud.toggle();
            });
            context.takeScreenshot("jugcraft_world_designer_terrain");
            context.runOnClient(client -> { if (client.gui.hud.isHidden()) client.gui.hud.toggle(); });
            save = world.getWorldSave();
        }
        try (TestSingleplayerContext reopened = save.open()) {
            verify(reopened);
            Jugcraft.LOGGER.info("[designer-check] Authored heights, custom biome paint, spawn, city and generator survived save/reopen");
        }
    }
    private static void verify(TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> {
            var level = server.overworld(); var generator = level.getChunkSource().getGenerator();
            check(generator.getBiomeSource() instanceof DesignBiomeSource, "Authored biome source missing");
            var spawn = level.getRespawnData().pos();
            check(spawn.getX() == -128 && spawn.getZ() == 0 && spawn.getY() >= 80, "Spawn did not use the design: " + spawn);
            check(TownPlanner.originAround(256, 127, 256).equals(TownState.get(level).origin()), "City did not use the design");
            int low = generator.getBaseHeight(-200, -200, Heightmap.Types.OCEAN_FLOOR_WG, level, level.getChunkSource().randomState());
            int high = generator.getBaseHeight(200, -200, Heightmap.Types.OCEAN_FLOOR_WG, level, level.getChunkSource().randomState());
            check(Math.abs(low - 80) <= 1 && Math.abs(high - 128) <= 1, "Authored terrain mismatch: " + low + "/" + high);
            level.getChunk(-200 >> 4, -200 >> 4); level.getChunk(200 >> 4, -200 >> 4);
            check(level.getBiome(new BlockPos(-200, 90, -200)).is(Biomes.DESERT), "Desert paint did not reach a generated chunk");
            check(level.getBiome(new BlockPos(200, 140, -200)).is(AlpineSpawn.BIOME), "Custom biome paint did not reach a generated chunk");
            check(!level.getBlockState(new BlockPos(-200, 76, -200)).isAir(), "Authored land is missing below its surface");
            check(level.getBlockState(new BlockPos(-200, 110, -200)).isAir(), "Old terrain remained above authored surface");
            Jugcraft.LOGGER.info("[designer-check] Base surfaces {}/{}, spawn {}, city {} ({} blocks)", low, high, spawn, TownState.get(level).origin(), TownData.get().size);
        });
    }
    private static void check(boolean pass, String message) { if (!pass) throw new AssertionError(message); }
}
