package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

/** At most eight local columns per attempt; select a real moist cave floor, never surface terrain or solid rock. */
public record CaveFungalFloorPlacement() implements PlacementModifier {
    public static final MapCodec<CaveFungalFloorPlacement> CODEC = MapCodec.unit(CaveFungalFloorPlacement::new);
    @Override public MapCodec<CaveFungalFloorPlacement> codec() { return CODEC; }
    @Override public void modify(PlacementContext context, RandomSource random, BlockPos origin, Consumer<BlockPos> output) {
        var level = context.getLevel();
        for (int column = 0; column < 8; column++) {
            int x = column == 0 ? origin.getX() : (origin.getX() & ~15) + random.nextInt(16);
            int z = column == 0 ? origin.getZ() : (origin.getZ() & ~15) + random.nextInt(16);
            int top = Math.min(level.getMaxY() - 10, context.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 6);
            BlockPos selected = null;
            int candidates = 0;
            for (int y = level.getMinY() + 1; y <= top; y++) {
                var at = new BlockPos(x, y, z);
                if (!level.isEmptyBlock(at) || !FungalGrowthFeature.soil("glowcap_dome", level.getBlockState(at.below()))) continue;
                boolean headroom = true;
                for (int up = 1; up < 6; up++) if (!level.isEmptyBlock(at.above(up))) { headroom = false; break; }
                if (headroom && random.nextInt(++candidates) == 0) selected = at;
            }
            if (selected != null) { output.accept(selected); return; }
        }
    }
}
