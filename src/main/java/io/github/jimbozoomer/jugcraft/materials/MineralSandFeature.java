package io.github.jimbozoomer.jugcraft.materials;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/** Shallow sediment lenses. Their physical blocks, not a regenerating counter, are the finite reserve. */
public record MineralSandFeature(BlockState sand, TagKey<Biome> surfaceBiomes, int family,
        int minLength, int maxLength, int minWidth, int maxWidth,
        int minThickness, int maxThickness, int minCover, int maxCover) implements Feature {
    public static final MapCodec<MineralSandFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockState.CODEC.fieldOf("sand").forGetter(MineralSandFeature::sand),
            TagKey.codec(Registries.BIOME).fieldOf("surface_biomes").forGetter(MineralSandFeature::surfaceBiomes),
            Codec.intRange(0, 3).fieldOf("family").forGetter(MineralSandFeature::family),
            Codec.intRange(16, 40).fieldOf("min_length").forGetter(MineralSandFeature::minLength),
            Codec.intRange(16, 40).fieldOf("max_length").forGetter(MineralSandFeature::maxLength),
            Codec.intRange(10, 24).fieldOf("min_width").forGetter(MineralSandFeature::minWidth),
            Codec.intRange(10, 24).fieldOf("max_width").forGetter(MineralSandFeature::maxWidth),
            Codec.intRange(1, 6).fieldOf("min_thickness").forGetter(MineralSandFeature::minThickness),
            Codec.intRange(1, 6).fieldOf("max_thickness").forGetter(MineralSandFeature::maxThickness),
            Codec.intRange(0, 4).fieldOf("min_cover").forGetter(MineralSandFeature::minCover),
            Codec.intRange(0, 4).fieldOf("max_cover").forGetter(MineralSandFeature::maxCover)
    ).apply(i, MineralSandFeature::new));

    @Override public MapCodec<MineralSandFeature> codec() { return CODEC; }

    @Override public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!level.getLevel().dimension().equals(Level.OVERWORLD)) return false;
        if (minLength > maxLength || minWidth > maxWidth || minThickness > maxThickness || minCover > maxCover) return false;
        // A fixed chunk-centered anchor keeps the maximum rotated footprint inside the writable 3x3 region.
        int cx = (origin.getX() & ~15) + 8, cz = (origin.getZ() & ~15) + 8;
        BlockPos center = ground(level, cx, cz);
        if (!level.getBiome(center.above()).is(surfaceBiomes) || !naturalGround(level.getBlockState(center))) return false;
        if (family == 2 && level.getBiome(center.above()).is(Biomes.RIVER) && !forestBank(level, center)) return false;
        double length = between(random, minLength, maxLength), width = between(random, minWidth, maxWidth);
        int thickness = between(random, minThickness, maxThickness), cover = between(random, minCover, maxCover);
        double angle = random.nextDouble() * Math.PI;
        if (family == 0 || family == 3) {
            // Orient long axes along the shore when there is a clear water gradient.
            int east = wet(level, cx + 12, cz) - wet(level, cx - 12, cz);
            int south = wet(level, cx, cz + 12) - wet(level, cx, cz - 12);
            if (east != 0 || south != 0) angle = Math.atan2(south, east) + Math.PI / 2;
        }
        double sin = Math.sin(angle), cos = Math.cos(angle), phase = random.nextDouble() * Math.PI * 2;
        int salt = random.nextInt();
        var structures = level instanceof WorldGenRegion region
                ? level.getLevel().structureManager().forWorldGenRegion(region) : level.getLevel().structureManager();
        var planned = new ArrayList<BlockPos>();
        int footprint = 0, valid = 0, sediment = 0, exposed = 0, low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
        // Every check completes before the first write. No unbounded searches, terrain raising or fluid filling.
        for (int dx = -23; dx <= 23; dx++) for (int dz = -23; dz <= 23; dz++) {
            double u = (dx * cos + dz * sin) / (length / 2), v = (-dx * sin + dz * cos) / (width / 2);
            double edge = 1 + .11 * Math.sin(u * 7 + phase) * Math.cos(v * 6 - phase);
            double radius = u * u + v * v;
            if (radius > edge || radius > 1.08) continue;
            footprint++;
            BlockPos top = ground(level, cx + dx, cz + dz);
            if (!level.ensureCanWrite(top) || !level.getBiome(top.above()).is(surfaceBiomes)) continue;
            BlockState surface = level.getBlockState(top);
            if (!naturalGround(surface) || Math.abs(top.getY() - center.getY()) > 4) continue;
            int water = 0;
            while (water < 3 && level.getBlockState(top.above(water + 1)).is(Blocks.WATER)) water++;
            BlockState above = level.getBlockState(top.above(water + 1));
            if (water > 2 || !(above.isAir() || above.is(Blocks.SNOW) || above.canBeReplaced())) continue;
            if (!above.getFluidState().isEmpty()) continue;
            int depth = Math.max(1, (int) Math.round(thickness * Math.sqrt(Math.max(.04, 1 - radius * .85))));
            // One gently wandering exposure ribbon is connected to the buried lens, never an unrelated marker.
            boolean ribbon = Math.abs(v - .35 * Math.sin(u * 4 + phase)) < .17 && Math.abs(u) < .8;
            int buried = ribbon ? 0 : cover;
            List<BlockPos> column = new ArrayList<>();
            boolean safe = true;
            for (int d = 0; d <= buried + depth; d++) {
                BlockPos p = top.below(d);
                if (!level.ensureCanWrite(p) || !naturalGround(level.getBlockState(p))
                        || level.getBlockEntity(p) != null || structures.getStructureWithPieceAt(p, holder -> true).isValid()) {
                    safe = false; break;
                }
                if (d >= buried && d < buried + depth) {
                    // Thin, discontinuous host-sediment bands break up the bed without introducing other minerals.
                    if (d > buried && Math.floorMod(dx * 13 + dz * 7 + salt, 11) == 0) continue;
                    column.add(p);
                }
            }
            if (!safe) continue;
            valid++;
            if (sediment(surface)) sediment++;
            low = Math.min(low, top.getY()); high = Math.max(high, top.getY());
            if (buried == 0 && water == 0) exposed++;
            planned.addAll(column);
        }
        // Reject steep/fragmented/rock-only sites rather than carving artificial sand into cliffs.
        if (valid * 2 < footprint || sediment * 5 < valid || high - low > 6 || exposed < 4 || planned.size() < 80) return false;
        planned.forEach(pos -> level.setBlock(pos, sand, 2));
        return true;
    }

    private static int between(RandomSource random, int min, int max) { return min + random.nextInt(max - min + 1); }

    private static BlockPos ground(WorldGenLevel level, int x, int z) {
        return new BlockPos(x, level.getHeight(level instanceof WorldGenRegion
                ? Heightmap.Types.OCEAN_FLOOR_WG : Heightmap.Types.OCEAN_FLOOR, x, z) - 1, z);
    }

    private static int wet(WorldGenLevel level, int x, int z) {
        return level.getBlockState(ground(level, x, z).above()).is(Blocks.WATER) ? 1 : 0;
    }

    private static boolean forestBank(WorldGenLevel level, BlockPos center) {
        for (int[] offset : new int[][]{{20, 0}, {-20, 0}, {0, 20}, {0, -20}}) {
            var b = level.getBiome(ground(level, center.getX() + offset[0], center.getZ() + offset[1]).above());
            if (b.is(ConventionalBiomeTags.IS_FOREST) || b.is(ConventionalBiomeTags.IS_TAIGA)) return true;
        }
        return false;
    }

    private static boolean sediment(BlockState state) {
        return state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.GRAVEL)
                || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MUD);
    }

    private static boolean naturalGround(BlockState state) {
        return state.getFluidState().isEmpty() && (sediment(state) || state.is(BlockTags.STONE_ORE_REPLACEABLES)
                || state.is(Blocks.SANDSTONE) || state.is(Blocks.RED_SANDSTONE));
    }
}
