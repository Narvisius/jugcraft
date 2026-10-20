package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/** A small, fully supported strand log. Validate the complete footprint before writing. */
public record DriftwoodFeature() implements Feature {
    public static final MapCodec<DriftwoodFeature> CODEC = MapCodec.unit(DriftwoodFeature::new);
    @Override public MapCodec<DriftwoodFeature> codec() { return CODEC; }

    @Override public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int sea = generator.getSeaLevel();
        if (origin.getY() < sea || origin.getY() > sea + 2) return false;
        boolean shore = false;
        // Fixed 9x9 check at water level; no chunk search or persistent ticking.
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            if (level.getFluidState(new BlockPos(origin.getX() + x, sea - 1, origin.getZ() + z)).is(FluidTags.WATER)) shore = true;
        }
        if (!shore) return false;
        Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int length = 3 + random.nextInt(4);
        for (int i = 0; i < length; i++) {
            BlockPos at = origin.relative(direction, i);
            var floor = level.getBlockState(at.below());
            if (!level.ensureCanWrite(at) || !level.isEmptyBlock(at)
                    || !(floor.is(Blocks.SAND) || floor.is(Blocks.GRAVEL))) return false;
        }
        var log = JugcraftAgriculture.block("stripped_dead_log").defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, direction.getAxis());
        for (int i = 0; i < length; i++) level.setBlock(origin.relative(direction, i), log, Block.UPDATE_CLIENTS);
        return true;
    }
}
