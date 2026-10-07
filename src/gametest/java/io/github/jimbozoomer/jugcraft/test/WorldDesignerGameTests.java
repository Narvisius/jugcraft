package io.github.jimbozoomer.jugcraft.test;

import com.mojang.serialization.JsonOps;
import io.github.jimbozoomer.jugcraft.world.design.*;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;

public class WorldDesignerGameTests {
    private static DesignGrid flat() { return new DesignGrid(-512, -512, 32, 33, Collections.nCopies(33 * 33, 80f)); }

    @GameTest
    public void densityPreservesOutsideAndDeepCaves(GameTestHelper h) {
        DesignGrid grid = flat();
        DesignDensity density = new DesignDensity(grid, DensityFunctions.constant(-.75f), false);
        h.assertTrue(density.transform(-.75f, 600, 70, 0) == -.75f, "Outside density must stay exact");
        h.assertTrue(density.transform(-.75f, 0, 8, 0) == -.75f, "Deep caves must stay exact");
        h.assertTrue(density.transform(-.75f, 0, 79, 0) > 0 && density.transform(.75f, 0, 80, 0) < 0, "Height 80 must be the first air block");
        h.assertTrue(grid.weight(-480, 0) == .5f && grid.weight(-512, 0) == 0, "Border must feather over two cells");
        var malformed = DesignGrid.CODEC.encodeStart(JsonOps.INSTANCE, grid).getOrThrow().getAsJsonObject();
        malformed.addProperty("step", 7);
        h.assertTrue(DesignGrid.CODEC.parse(JsonOps.INSTANCE, malformed).error().isPresent(), "Non-power-of-two grids must fail before generation");
        h.succeed();
    }

    @GameTest
    public void biomePaintRoundTripsAndLeavesCavesAndOutsideAlone(GameTestHelper h) {
        var biomes = h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        var plains = biomes.getOrThrow(Biomes.PLAINS);
        var desert = biomes.getOrThrow(Biomes.DESERT);
        var source = new DesignBiomeSource(new FixedBiomeSource(plains), flat(), List.of(desert), Collections.nCopies(33 * 33, 1), BlockPos.ZERO, Optional.empty());
        var ops = RegistryOps.create(JsonOps.INSTANCE, h.getLevel().registryAccess());
        var json = BiomeSource.CODEC.encodeStart(ops, source).getOrThrow();
        var decoded = BiomeSource.CODEC.parse(ops, json).getOrThrow();
        var resolver = decoded.createResolver(null);
        h.assertTrue(resolver.getNoiseBiome(0, 20, 0).is(Biomes.DESERT), "Painted surface must use the selected biome");
        h.assertTrue(resolver.getNoiseBiome(0, -1, 0).is(Biomes.PLAINS), "Cave biome must come from the original source");
        h.assertTrue(resolver.getNoiseBiome(200, 20, 0).is(Biomes.PLAINS), "Outside must use the original source");
        json.getAsJsonObject().getAsJsonArray("cells").set(0, new com.google.gson.JsonPrimitive(2));
        h.assertTrue(BiomeSource.CODEC.parse(ops, json).error().isPresent(), "Invalid palette indices must be rejected");
        h.succeed();
    }

    @GameTest
    public void pinsChooseOneChunkAndCatalogUsesLiveRegistries(GameTestHelper h) throws java.io.IOException {
        DesignPlacement pin = new DesignPlacement(-32, 5);
        h.assertTrue(pin.isStructureChunk(null, -32, 5) && !pin.isStructureChunk(null, -31, 5), "Pin must use exactly one start chunk");
        var catalog = WorldDesigner.catalog(h.getLevel());
        h.assertTrue(catalog.getAsJsonArray("biomes").toString().contains("jugcraft:alpine_spawn"), "Catalog must contain installed custom biomes");
        h.assertTrue(catalog.getAsJsonArray("structures").toString().contains("jugcraft:village_alpine"), "Catalog must contain installed custom structures");
        h.assertTrue(catalog.getAsJsonObject("noiseSettings").has("noise_router"), "Catalog must contain the live Overworld settings");
        Files.writeString(FabricLoader.getInstance().getGameDir().resolve("designer-test-catalog.json"), catalog.toString());
        h.succeed();
    }
}
