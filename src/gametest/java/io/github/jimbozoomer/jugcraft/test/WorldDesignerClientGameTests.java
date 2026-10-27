package io.github.jimbozoomer.jugcraft.test;

import com.mojang.serialization.JsonOps;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.lair.FrostHornRite;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.KilnSealRite;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairInstance;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.town.TownData;
import io.github.jimbozoomer.jugcraft.town.TownPlanner;
import io.github.jimbozoomer.jugcraft.town.TownState;
import io.github.jimbozoomer.jugcraft.world.AlpineSpawn;
import io.github.jimbozoomer.jugcraft.world.design.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;

/**
 * Real terrain generation, custom biome paint, spawn/city planning, and codec persistence after reopening, and every lair
 * reachable by its rite in the designed world, created and reopened.
 */
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
            lairsOpen(context, world);
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
            lairsOpen(context, reopened);
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
    /**
     * The lairs in a designed world (07:147-148): every lair's level is there, because the preset names only the three vanilla
     * dimensions and the game adds Jugcraft's data-pack dimensions to any new or reopened world (CreateWorldScreen.java:249,
     * WorldOpenFlows.java:165-166). The Frost Horn blown on placed snow opens a Glacier Hall; the Kiln Seal pressed into the
     * Nether's magma opens a Cinder Kiln; closing each sends the player back to where they stood.
     */
    private static void lairsOpen(ClientGameTestContext context, TestSingleplayerContext world) {
        var server = world.getServer();
        BlockPos snow = new BlockPos(-300, 149, -300);   // over authored land (height 80), clear of verify()'s columns
        BlockPos magma = new BlockPos(8, 100, 8);
        AtomicReference<Vec3> home = new AtomicReference<>();
        AtomicReference<Vec3> stood = new AtomicReference<>();
        AtomicReference<LairInstance> hall = new AtomicReference<>();
        AtomicReference<LairInstance> kiln = new AtomicReference<>();
        try {
            server.runOnServer(minecraft -> {
                for (Lair lair : Lair.values()) {
                    check(minecraft.getLevel(lair.dimension) != null, "A designed world has no " + lair.dimension.identifier());
                }
                ServerPlayer player = minecraft.getPlayerList().getPlayers().getFirst();
                ServerLevel overworld = minecraft.overworld();
                home.set(player.position());
                overworld.setBlockAndUpdate(snow, Blocks.SNOW_BLOCK.defaultBlockState());
                player.teleportTo(overworld, snow.getX() + 0.5, snow.getY() + 1, snow.getZ() + 0.5, Set.of(), 180.0F, 0.0F, true);
            });
            // Placing the snow loads its chunk but only queues its entities' promotion to visible; ticks run it, so the whirl
            // the horn adds there is found (as the Nether half waits before pressing the seal).
            context.waitTicks(20);
            server.runOnServer(minecraft -> {
                ServerPlayer player = minecraft.getPlayerList().getPlayers().getFirst();
                ServerLevel overworld = minecraft.overworld();
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.FROST_HORN));
                stood.set(player.position());
                FrostHornRite.Call call = FrostHornRite.blow(overworld, player, player.getMainHandItem(), true);
                check(call.missing() == null && call.instance() != null && call.instance().lair == Lair.GLACIER_HALL && call.instance().isOpen(),
                        "The Frost Horn opened no Glacier Hall in a designed world: " + call);
                check(player.level() == minecraft.getLevel(Lair.GLACIER_HALL.dimension) && player.position().distanceTo(call.instance().arrival()) < 0.5,
                        "The horn did not take the player into the hall: " + player.level().dimension() + " " + player.position());
                check(FrostHornRite.whirl(overworld, snow.above()) != null, "No whirl where the horn was blown");
                hall.set(call.instance());
            });
            context.waitTicks(20);
            server.runOnServer(minecraft -> {
                ServerPlayer player = minecraft.getPlayerList().getPlayers().getFirst();
                ServerLevel overworld = minecraft.overworld();
                Lairs.close(minecraft, hall.get());
                check(!hall.get().isOpen() && player.level() == overworld && player.position().distanceTo(stood.get()) < 1.0E-6,
                        "Closing the hall did not send the player back: " + player.level().dimension() + " " + player.position());
                check(FrostHornRite.whirl(overworld, snow.above()) == null, "The whirl outlived its hall");
                player.setTicksFrozen(0);
                ServerLevel nether = minecraft.getLevel(Level.NETHER);
                check(nether != null, "A designed world has no Nether");
                nether.setBlockAndUpdate(magma, Blocks.MAGMA_BLOCK.defaultBlockState());
                nether.setBlockAndUpdate(magma.above(), Blocks.AIR.defaultBlockState());
                nether.setBlockAndUpdate(magma.above(2), Blocks.AIR.defaultBlockState());
                player.teleportTo(nether, magma.getX() + 0.5, magma.getY() + 1, magma.getZ() + 0.5, Set.of(), 180.0F, 0.0F, true);
            });
            context.waitTicks(20);
            server.runOnServer(minecraft -> {
                ServerPlayer player = minecraft.getPlayerList().getPlayers().getFirst();
                ServerLevel nether = minecraft.getLevel(Level.NETHER);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.KILN_SEAL));
                stood.set(player.position());
                // No forced flag: the Nether is always volcanic (KilnSealRite.volcanic).
                KilnSealRite.Press press = KilnSealRite.press(nether, player, magma, player.getMainHandItem());
                check(press.missing() == null && press.instance() != null && press.instance().lair == Lair.CINDER_KILN && press.instance().isOpen(),
                        "The Kiln Seal opened no Cinder Kiln from a designed world's Nether: " + press);
                check(player.level() == minecraft.getLevel(Lair.CINDER_KILN.dimension) && player.position().distanceTo(press.instance().arrival()) < 0.5,
                        "The seal did not take the player into the kiln: " + player.level().dimension() + " " + player.position());
                kiln.set(press.instance());
            });
            context.waitTicks(20);
            server.runOnServer(minecraft -> {
                ServerPlayer player = minecraft.getPlayerList().getPlayers().getFirst();
                ServerLevel nether = minecraft.getLevel(Level.NETHER);
                Lairs.close(minecraft, kiln.get());
                check(!kiln.get().isOpen() && player.level() == nether && player.position().distanceTo(stood.get()) < 1.0E-6,
                        "Closing the kiln did not send the player back: " + player.level().dimension() + " " + player.position());
                check(KilnSealRite.vent(nether, magma) == null, "The vent outlived its kiln");
                player.teleportTo(minecraft.overworld(), home.get().x, home.get().y, home.get().z, Set.of(), player.getYRot(), player.getXRot(), true);
            });
            context.waitTicks(20);
        } finally {
            server.runOnServer(minecraft -> Lairs.reset());
        }
    }
    private static void check(boolean pass, String message) { if (!pass) throw new AssertionError(message); }
}
