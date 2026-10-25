package io.github.jimbozoomer.jugcraft.test;

import com.mojang.serialization.JsonOps;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.materials.MineralSandFeature;
import io.github.jimbozoomer.jugcraft.world.design.DesignBiomeSource;
import io.github.jimbozoomer.jugcraft.world.design.DesignGrid;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelResource;

/** Production-rate decoration, terrain safety and persisted depletion; exports a natural inspection save. */
public class MineralSandClientGameTests implements FabricClientGameTest {
    private record Sample(Set<BlockPos> reserve, List<BlockPos> exposed, int eligible) { }
    @Override public void runTest(ClientGameTestContext context) {
        if (!JugcraftConfig.isFeatureEnabled("mineral_sands")) { disabled(context); return; }
        designed(context);
        natural(context, "jugcraft-mineral-sands-1", true);
        natural(context, "jugcraft-mineral-sands-2", false);
    }
    private static MineralSandFeature feature(ServerLevel level, int family) {
        return (MineralSandFeature) level.registryAccess().lookupOrThrow(Registries.FEATURE)
                .getValue(Jugcraft.id("ore_" + MineralSandGameTests.names().get(family)));
    }
    private static void normal(net.minecraft.client.gui.screens.worldselection.WorldCreationUiState creator, String seed) {
        creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
        creator.setWorldType(new WorldCreationUiState.WorldTypeEntry(creator.getSettings().worldgenLoadContext()
                .lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL)));
        creator.setSeed(seed);
    }
    private static Sample sample(ServerLevel level, BlockPos center, int radius, int family, String label) {
        Set<BlockPos> reserve = new HashSet<>(); List<BlockPos> exposed = new ArrayList<>(); int eligible = 0;
        int minX = (center.getX() >> 4) - radius, minZ = (center.getZ() >> 4) - radius;
        for (int x = minX; x < minX + radius * 2; x++) for (int z = minZ; z < minZ + radius * 2; z++) level.getChunk(x, z);
        var cursor = new BlockPos.MutableBlockPos(); var sand = MineralSandGameTests.sands().get(family);
        for (int cx = minX; cx < minX + radius * 2; cx++) for (int cz = minZ; cz < minZ + radius * 2; cz++) {
            int sx = cx * 16 + 8, sz = cz * 16 + 8;
            if (level.getBiome(new BlockPos(sx, level.getHeight(Heightmap.Types.OCEAN_FLOOR, sx, sz), sz)).is(feature(level, family).surfaceBiomes())) eligible++;
            for (int x = cx * 16; x < cx * 16 + 16; x++) for (int z = cz * 16; z < cz * 16 + 16; z++) {
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                int ground = Integer.MIN_VALUE;
                for (int y = top; y >= Math.max(level.getMinY(), top - 40); y--) if (level.getBlockState(cursor.set(x, y, z)).is(sand)) {
                    reserve.add(cursor.immutable());
                    // Decoration has since added trees. Survey the sediment surface beneath their canopy,
                    // not the water/tree height or the buried ore's three-dimensional biome.
                    if (ground == Integer.MIN_VALUE) ground = terrainSurface(level, x, z);
                    var configured = feature(level, family);
                    // Later pond/soil/tree decoration can move the apparent surface at a 3D biome edge.
                    // Its generation-time surface must occur within the lens's maximum burial envelope.
                    boolean eligibleSurface = false;
                    for (int sy = y + 1; sy <= y + configured.maxCover() + configured.maxThickness() + 1; sy++)
                        eligibleSurface |= level.getBiome(new BlockPos(x, sy, z)).is(configured.surfaceBiomes());
                    check(eligibleSurface, "No eligible generation surface above " + cursor + "; current ground " + ground);
                    var above = level.getBlockState(cursor.above());
                    if (above.isAir() || above.canBeReplaced() && above.getFluidState().isEmpty()) exposed.add(cursor.immutable());
                }
            }
        }
        exposed.sort(Comparator.<BlockPos>comparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ));
        Jugcraft.LOGGER.info("[mineral-sands] {} {}: {} chunks, {} eligible centers, {} reserve blocks, {} visible blocks", label,
                MineralSandGameTests.names().get(family), radius * radius * 4, eligible, reserve.size(), exposed.size());
        return new Sample(Set.copyOf(reserve), List.copyOf(exposed), eligible);
    }
    private static int terrainSurface(ServerLevel level, int x, int z) {
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        for (int y = top - 1; y >= Math.max(level.getMinY(), top - 64); y--) {
            var state = level.getBlockState(new BlockPos(x, y, z));
            if (state.is(io.github.jimbozoomer.jugcraft.machine.OreDrilling.MINERAL_SANDS)
                    || state.is(net.minecraft.tags.BlockTags.DIRT) || state.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD)
                    || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.GRAVEL)
                    || state.is(Blocks.MUD) || state.is(Blocks.CLAY) || state.is(Blocks.SANDSTONE) || state.is(Blocks.RED_SANDSTONE)) return y + 1;
        }
        throw new AssertionError("No sediment surface below canopy at " + x + " " + z);
    }
    private static void natural(ClientGameTestContext context, String seed, boolean export) {
        List<Sample> samples = new ArrayList<>(); List<BlockPos> depleted = new ArrayList<>(), cameras = new ArrayList<>(); Path source; TestWorldSave save;
        String[] targets = {"dune_beach", "outback", "lake_district", "gravel_beach"};
        try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(c -> {
            normal(c, seed); c.setName(export ? "Mineral Sands Field Trip" : "Mineral Sands Sampling");
        }).create()) {
            for (int i = 0; i < 4; i++) {
                final int family = i;
                BlockPos center = world.getServer().computeOnServer(server -> {
                    var found = server.overworld().findClosestBiome3d(b -> b.is(Jugcraft.id(targets[family])), BlockPos.ZERO.atY(family == 3 ? 64 : 80), 16384, 32, 64);
                    check(found != null, "No target biome " + targets[family]); return found.getFirst();
                });
                Sample result = survey(context, world, center, family, seed);
                samples.add(result);
                if (export) {
                    BlockPos view = result.exposed.get(result.exposed.size() / 2);
                    world.getServer().runCommand("gamerule minecraft:advance_time false");
                    world.getServer().runCommand("gamerule minecraft:advance_weather false");
                    world.getServer().runCommand("time set noon"); world.getServer().runCommand("weather clear");
                    cameras.add(camera(context, world, view, MineralSandGameTests.names().get(i)));
                }
                BlockPos removed = result.exposed.getFirst(); depleted.add(removed);
                world.getServer().runOnServer(server -> {
                    var level = server.overworld(); var player = server.getPlayerList().getPlayers().getFirst();
                    // The drill's transaction removes one physical reserve and inserts one feed unit.
                    level.setBlock(removed, Blocks.SAND.defaultBlockState(), 3);
                    player.getInventory().add(new ItemStack(MineralSandGameTests.sands().get(family)));
                });
            }
            if (export) {
                BlockPos spawn = cameras.getFirst();
                world.getServer().runCommand("tp @a " + xyz(spawn));
                world.getServer().runCommand("setworldspawn " + xyz(spawn));
                world.getServer().runCommand("gamerule minecraft:respawn_radius 0");
                world.getServer().runCommand("give @a minecraft:iron_shovel");
                world.getServer().runCommand("give @a jugcraft:prospector");
                context.waitTicks(20);
                writeTour(seed, samples, depleted, cameras);
            }
            source = world.getServer().computeOnServer(server -> server.getWorldPath(LevelResource.ROOT)); save = world.getWorldSave();
        }
        try (TestSingleplayerContext reopened = save.open()) {
            reopened.getServer().runOnServer(server -> {
                var level = server.overworld(); var inventory = server.getPlayerList().getPlayers().getFirst().getInventory();
                for (int i = 0; i < 4; i++) {
                    check(!level.getBlockState(depleted.get(i)).is(MineralSandGameTests.sands().get(i)), "Depleted reserve regenerated");
                    check(inventory.countItem(MineralSandGameTests.sands().get(i).asItem()) == 1, "Extracted feed changed on reload");
                    for (BlockPos pos : samples.get(i).reserve) if (!pos.equals(depleted.get(i)))
                        check(level.getBlockState(pos).is(MineralSandGameTests.sands().get(i)), "Unmined reserve changed at " + pos);
                }
            });
        }
        if (export) copySave(source, Path.of("mineral-sands-inspection", "Mineral Sands Field Trip"));
        Jugcraft.LOGGER.info("[mineral-sands] {}: all four depleted blocks, inventory units and remaining reserves survived reopening", seed);
    }
    private static Sample survey(ClientGameTestContext context, TestSingleplayerContext world, BlockPos center, int family, String seed) {
        var server = world.getServer();
        Sample found = server.computeOnServer(s -> sample(s.overworld(), center, 10, family, seed));
        // Yield between regions so temporary chunk tickets expire and distant chunks can unload.
        // A single uninterrupted server callback accumulates every surveyed chunk until shutdown.
        context.waitTicks(5);
        if (found.exposed.isEmpty()) found = server.computeOnServer(s -> sample(s.overworld(), center, 18, family, seed + " expanded"));
        Set<net.minecraft.world.level.ChunkPos> surveyed = new HashSet<>();
        surveyed.add(new net.minecraft.world.level.ChunkPos(center.getX() >> 4, center.getZ() >> 4));
        // Rare coastal fragments offer few eligible anchors. Keep the search bounded while
        // alternating gravel shores and the entire production biome tag, including inland beds.
        for (int region = 1; found.exposed.isEmpty() && region <= 32; region++) {
            context.waitTicks(5);
            final int attempt = region;
            final boolean shore = family == 3 && region % 2 == 0;
            var next = server.computeOnServer(s -> s.overworld().findClosestBiome3d(b -> family == 2
                            ? b.is(Jugcraft.id("basin")) || b.is(Jugcraft.id("lake_district"))
                            : shore ? b.is(Jugcraft.id("gravel_beach"))
                            : b.is(feature(s.overworld(), family).surfaceBiomes()),
                    center.offset(attempt * 2048, 0, attempt * -2048).atY(shore ? 64 : center.getY()), 16384, 32, 64));
            if (next != null && surveyed.add(new net.minecraft.world.level.ChunkPos(next.getFirst().getX() >> 4, next.getFirst().getZ() >> 4)))
                found = server.computeOnServer(s -> sample(s.overworld(), next.getFirst(), 14, family, seed + " region " + attempt));
        }
        check(!found.reserve.isEmpty() && !found.exposed.isEmpty(), "No visible natural " + MineralSandGameTests.names().get(family) + " deposit on " + seed);
        BlockPos bed = found.exposed.get(found.exposed.size() / 2);
        context.waitTicks(5);
        return server.computeOnServer(s -> sample(s.overworld(), bed, 4, family, seed + " complete inspection bed"));
    }
    private static BlockPos camera(ClientGameTestContext context, TestSingleplayerContext world, BlockPos view, String name) {
        BlockPos camera = world.getServer().computeOnServer(s -> {
            var level = s.overworld(); var player = s.getPlayerList().getPlayers().getFirst();
            // Inspection-only foliage window. Preserve the natural ground and every mineral block.
            int leaves = 0;
            for (BlockPos p : BlockPos.betweenClosed(view.offset(-6, 1, -6), view.offset(6, 14, 6)))
                if (level.getBlockState(p).is(net.minecraft.tags.BlockTags.LEAVES)) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 2); leaves++;
                }
            Jugcraft.LOGGER.info("[mineral-sands] {} inspection foliage window: {} leaves cleared; terrain and reserve preserved", name, leaves);
            for (int[] offset : new int[][]{{8, 9, 12}, {4, 4, 4}, {-4, 4, -4}, {2, 2, 2}, {-2, 2, -2}, {0, 2, 0}}) {
                var pos = view.offset(offset[0], offset[1], offset[2]);
                if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) continue;
                var eye = new net.minecraft.world.phys.Vec3(pos.getX() + .5, pos.getY() + 1.62, pos.getZ() + .5);
                var hit = level.clip(new net.minecraft.world.level.ClipContext(eye, net.minecraft.world.phys.Vec3.atCenterOf(view),
                        net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player));
                if (hit.getBlockPos().equals(view)) return pos;
            }
            return view.above(2);
        });
        world.getServer().runCommand("gamemode spectator @a");
        world.getServer().runCommand("tp @a " + xyz(camera));
        world.getServer().runOnServer(s -> { var p = s.getPlayerList().getPlayers().getFirst(); p.getAbilities().flying = true; p.onUpdateAbilities(); });
        context.waitTicks(60); world.getConnection().waitForChunksRender(); context.getInput().lookAt(view); context.waitTicks(10);
        context.runOnClient(c -> { if (!c.gui.hud.isHidden()) c.gui.hud.toggle(); }); context.waitTicks(2);
        context.takeScreenshot("mineral-sands-" + name);
        context.runOnClient(c -> { if (c.gui.hud.isHidden()) c.gui.hud.toggle(); });
        world.getServer().runCommand("gamemode creative @a");
        return camera;
    }
    private static void designed(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(c -> {
            normal(c, "jugcraft-mineral-sands-designed"); c.setGenerateStructures(false);
            c.updateDimensions((registries, dimensions) -> {
                var grid = new DesignGrid(-768, -768, 32, 49, Collections.nCopies(49 * 49, 96f));
                var cells = new ArrayList<Integer>();
                for (int z = 0; z < 49; z++) for (int x = 0; x < 49; x++) cells.add(1 + (x < 24 ? 0 : 1) + (z < 24 ? 0 : 2));
                var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
                var settings = NoiseGeneratorSettings.DIRECT_CODEC.encodeStart(ops,
                        registries.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD).value()).getOrThrow().getAsJsonObject();
                var router = settings.getAsJsonObject("noise_router");
                var land = new com.google.gson.JsonObject(); land.addProperty("type", "jugcraft:design_density");
                land.add("grid", DesignGrid.CODEC.encodeStart(ops, grid).getOrThrow()); land.add("input", router.get("final_density"));
                var surface = new com.google.gson.JsonObject(); surface.addProperty("type", "jugcraft:design_density"); surface.addProperty("surface", true);
                surface.add("grid", land.get("grid")); surface.add("input", router.get("chunk_surface_level"));
                router.add("final_density", land); router.add("chunk_surface_level", surface);
                if (settings.has("aquifers")) settings.getAsJsonObject("aquifers").add("surface_level", surface);
                var biomes = registries.lookupOrThrow(Registries.BIOME);
                var source = new DesignBiomeSource(new FixedBiomeSource(biomes.getOrThrow(Biomes.LUSH_CAVES)), grid,
                        List.of(biomes.get(Jugcraft.id("dune_beach")).orElseThrow(), biomes.get(Jugcraft.id("outback")).orElseThrow(),
                                biomes.get(Jugcraft.id("basin")).orElseThrow(), biomes.get(Jugcraft.id("gravel_beach")).orElseThrow()), cells,
                        new BlockPos(-384, 0, -384), Optional.empty());
                var generator = new NoiseBasedChunkGenerator(source, Holder.direct(NoiseGeneratorSettings.DIRECT_CODEC.parse(ops, settings).getOrThrow()));
                return dimensions.replaceOverworldGenerator(registries, ChunkGenerator.CODEC.parse(ops, ChunkGenerator.CODEC.encodeStart(ops, generator).getOrThrow()).getOrThrow());
            });
        }).create()) {
            world.getServer().runOnServer(server -> {
                var level = server.overworld();
                for (int i = 0; i < 4; i++) {
                    var center = new BlockPos(i % 2 == 0 ? -384 : 384, 96, i < 2 ? -384 : 384);
                    var result = sample(level, center, 10, i, "Designed surface over lush caves");
                    check(!result.reserve.isEmpty() && !result.exposed.isEmpty(), "Painted family failed " + i);
                    for (int other = 0; other < 4; other++) if (other != i) check(!feature(level, other).place(level,
                            level.getChunkSource().getGenerator(), RandomSource.create(400 + other), center), "Wrong family crossed painted region");
                }
                fixtures(level);
            });
        }
    }
    private static void fixtures(ServerLevel level) {
        var configured = feature(level, 0); var center = new BlockPos(-376, 200, -376);
        for (String scenario : List.of("flat", "water", "hollow", "roof", "chest", "steep")) {
            for (BlockPos p : BlockPos.betweenClosed(center.offset(-24, -12, -24), center.offset(24, 12, 24)))
                level.setBlock(p, (p.getY() <= 200 ? Blocks.SANDSTONE : Blocks.AIR).defaultBlockState(), 2);
            for (BlockPos p : BlockPos.betweenClosed(center.offset(-24, 0, -24), center.offset(24, 0, 24))) level.setBlock(p, Blocks.SAND.defaultBlockState(), 2);
            if (scenario.equals("water")) fill(level, center.offset(-24, 1, -24), center.offset(24, 4, 24), Blocks.WATER);
            if (scenario.equals("hollow")) fill(level, center.offset(-24, -10, -24), center.offset(24, -1, 24), Blocks.AIR);
            if (scenario.equals("roof")) fill(level, center.offset(-24, 3, -24), center.offset(24, 3, 24), Blocks.OAK_PLANKS);
            if (scenario.equals("steep")) for (int x = -24; x <= 24; x++) fill(level, center.offset(x, 1, -24), center.offset(x, 1 + Math.max(0, x + 24) / 2, 24), Blocks.SANDSTONE);
            if (scenario.equals("chest")) { level.setBlock(center, Blocks.CHEST.defaultBlockState(), 2); }
            boolean placed = configured.place(level, level.getChunkSource().getGenerator(), RandomSource.create(710), center);
            check(placed == scenario.equals("flat"), "Unsafe terrain placement: " + scenario + " = " + placed);
            if (scenario.equals("chest")) check(level.getBlockState(center).is(Blocks.CHEST), "Block entity overwritten");
            Jugcraft.LOGGER.info("[mineral-sands] safety fixture {}: placed {}", scenario, placed);
        }
    }
    private static void fill(ServerLevel level, BlockPos from, BlockPos to, net.minecraft.world.level.block.Block block) {
        for (BlockPos p : BlockPos.betweenClosed(from, to)) level.setBlock(p, block.defaultBlockState(), 2);
    }
    private static void disabled(ClientGameTestContext context) {
        TestWorldSave save; List<BlockPos> positions = new ArrayList<>();
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getServer().runOnServer(server -> {
                var level = server.overworld(); var player = server.getPlayerList().getPlayers().getFirst();
                for (int i = 0; i < 4; i++) {
                    var placed = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).getValue(Jugcraft.id("ore_" + MineralSandGameTests.names().get(i)));
                    for (var b : level.registryAccess().lookupOrThrow(Registries.BIOME).listElements().toList())
                        check(!level.getChunkSource().getGenerator().getBiomeGenerationSettings(b).hasFeature(placed), "Disabled decoration still enabled");
                    BlockPos pos = player.blockPosition().offset(i + 2, 0, 0); positions.add(pos);
                    level.setBlock(pos, MineralSandGameTests.sands().get(i).defaultBlockState(), 2);
                    player.getInventory().add(new ItemStack(MineralSandGameTests.sands().get(i), 7));
                }
            }); save = world.getWorldSave();
        }
        try (TestSingleplayerContext world = save.open()) {
            world.getServer().runOnServer(server -> {
                for (int i = 0; i < 4; i++) {
                    check(server.overworld().getBlockState(positions.get(i)).is(MineralSandGameTests.sands().get(i)), "Disabled saved block lost");
                    check(server.getPlayerList().getPlayers().getFirst().getInventory().countItem(MineralSandGameTests.sands().get(i).asItem()) == 7, "Disabled saved feed lost");
                }
            });
        }
        Jugcraft.LOGGER.info("[mineral-sands] Disabled decoration; all four block and inventory families survived reopening");
    }
    private static void writeTour(String seed, List<Sample> samples, List<BlockPos> removed, List<BlockPos> cameras) {
        var text = new StringBuilder("Mineral Sands Field Trip\nSeed: " + seed + "\nAll deposits generated at production rates in a normal world. One block per family was extracted for the persistence test; the four units are in your inventory. Other reserves and ground are untouched. Small leaf windows were cleared around the tour targets for visibility; these are inspection preparation, not a worldgen clearing.\nUse /gamemode spectator before the tour teleports to fly freely and inspect the shallow buried lenses; /gamemode creative to return.\n\n");
        for (int i = 0; i < 4; i++) {
            var sample = samples.get(i); var view = sample.exposed.get(sample.exposed.size() / 2);
            text.append(MineralSandGameTests.names().get(i)).append(": /tp @s ").append(xyz(cameras.get(i))).append('\n');
            text.append("Surface target: ").append(xyz(view)).append("; sampled finite reserve: ").append(sample.reserve.size()).append(" blocks across the surveyed region.\n");
            text.append("Depleted test block: ").append(xyz(removed.get(i))).append("\n\n");
        }
        try { Files.createDirectories(Path.of("mineral-sands-inspection")); Files.writeString(Path.of("mineral-sands-inspection", "TOUR.txt"), text); }
        catch (IOException e) { throw new RuntimeException(e); }
    }
    private static void copySave(Path source, Path destination) {
        try (var files = Files.walk(source)) {
            for (Path file : files.toList()) {
                Path target = destination.resolve(source.relativize(file).toString());
                if (Files.isDirectory(file)) Files.createDirectories(target);
                else if (!file.getFileName().toString().equals("session.lock")) Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) { throw new RuntimeException(e); }
    }
    private static String xyz(BlockPos pos) { return pos.getX() + " " + pos.getY() + " " + pos.getZ(); }
    private static void check(boolean pass, String message) { if (!pass) throw new AssertionError(message); }
}
