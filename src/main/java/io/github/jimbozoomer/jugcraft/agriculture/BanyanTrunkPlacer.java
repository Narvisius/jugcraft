package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

/** Four connected spreading limbs beneath a banyan crown, with occasional shorter forks. */
public final class BanyanTrunkPlacer extends TrunkPlacer {
    public static final MapCodec<BanyanTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(i ->
            trunkPlacerParts(i).apply(i, BanyanTrunkPlacer::new));
    public static final TrunkPlacerType<BanyanTrunkPlacer> TYPE = new TrunkPlacerType<>(CODEC);

    public BanyanTrunkPlacer(int height, int randomA, int randomB) { super(height, randomA, randomB); }
    @Override protected TrunkPlacerType<?> type() { return TYPE; }

    @Override public List<FoliagePlacer.FoliageAttachment> placeTrunk(WorldGenLevel level,
            BiConsumer<BlockPos, BlockState> setter, RandomSource random, int height, BlockPos origin, TreeFeature tree) {
        var crowns = new ArrayList<FoliagePlacer.FoliageAttachment>();
        BlockPos top = null;
        for (int y = 0; y < height; y++) {
            if (!placeLog(level, setter, random, origin.above(y), tree)) break;
            top = origin.above(y);
        }
        if (top == null) return crowns;
        crowns.add(new FoliagePlacer.FoliageAttachment(top.above(), 0, false));
        Direction side = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        for (int branch = 0; branch < 4; branch++, side = side.getClockWise()) {
            int baseY = Math.max(2, height - 4 + random.nextInt(2));
            if (origin.getY() + baseY > top.getY()) continue;
            BlockPos end = origin.above(baseY), fork = null;
            int length = 3 + random.nextInt(2), placed = 0;
            final var axis = side.getAxis();
            for (int step = 0; step < length; step++) {
                BlockPos next = end.relative(side);
                if (!placeLog(level, setter, random, next, tree, s -> s.setValue(RotatedPillarBlock.AXIS, axis))) break;
                end = next; placed++;
                if (step == 1) {
                    fork = end;
                    if (!placeLog(level, setter, random, end.above(), tree)) break;
                    end = end.above();
                }
            }
            if (placed == 0) continue;
            // Every bend has an intervening log: no diagonal gaps or detached branch ends.
            int tipY = top.getY() - random.nextInt(2);
            while (end.getY() < tipY) {
                if (!placeLog(level, setter, random, end.above(), tree)) break;
                end = end.above();
            }
            crowns.add(new FoliagePlacer.FoliageAttachment(end.above(), -2, false));
            if (fork != null && random.nextBoolean()) {
                Direction turn = random.nextBoolean() ? side.getClockWise() : side.getCounterClockWise();
                final var forkAxis = turn.getAxis();
                for (int step = 0, forkLength = 1 + random.nextInt(2); step < forkLength; step++) {
                    BlockPos next = fork.relative(turn);
                    if (!placeLog(level, setter, random, next, tree, s -> s.setValue(RotatedPillarBlock.AXIS, forkAxis))) break;
                    fork = next;
                }
            }
        }
        return crowns;
    }
}
