package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.season.JugcraftSeasons;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/** Bounded, connected walnut crowns and weathered Basin pines; validates the whole tree before writing. */
public record WalnutTree(String shape) implements Feature {
    public static final MapCodec<WalnutTree> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("shape").forGetter(WalnutTree::shape)).apply(i, WalnutTree::new));
    @Override public MapCodec<? extends Feature> codec() { return CODEC; }

    @Override public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!Set.of("walnut", "large_walnut", "bristlecone").contains(shape)) return false;
        boolean pine = shape.equals("bristlecone"), large = shape.equals("large_walnut");
        var soil = level.getBlockState(origin.below());
        if (pine ? !(soil.is(Blocks.GRAVEL) || soil.is(Blocks.STONE) || soil.is(Blocks.ANDESITE))
                : !Blocks.OAK_SAPLING.defaultBlockState().canSurvive(level, origin)) return false;
        // A thin gravel overhang is not a stable footing; never manufacture dirt under a rocky tree.
        if (pine && !level.getBlockState(origin.below(2)).isSolidRender()) return false;
        var plan = new LinkedHashMap<BlockPos, BlockState>();
        var logs = new LinkedHashSet<BlockPos>();
        int height = pine ? 4 + random.nextInt(4) : large ? 12 + random.nextInt(5) : 8 + random.nextInt(3);
        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        BlockPos tip = origin;
        for (int y = 0; y < height - (pine ? 1 : 2); y++) {
            tip = new BlockPos(tip.getX(), origin.getY() + y, tip.getZ());
            log(plan, logs, tip, Direction.Axis.Y, pine, random);
            if (pine && y > 0 && y % 2 == 0) {
                tip = tip.relative(facing);
                log(plan, logs, tip, facing.getAxis(), true, random);
                facing = random.nextBoolean() ? facing.getClockWise() : facing;
            }
        }
        var tips = new ArrayList<BlockPos>();
        tips.add(tip);
        int radius = pine ? 2 : large ? 5 + random.nextInt(2) : 3 + random.nextInt(2);
        for (int branch = 0; branch < (pine ? 2 : 4); branch++, facing = facing.getClockWise()) {
            int y = pine ? Math.max(1, height - 3) : height - (large ? 6 : 5) + random.nextInt(2);
            // Pine starts at its real bent trunk, not an imagined straight column.
            BlockPos end = pine ? tip : origin.above(y);
            int length = pine ? (branch == 0 ? 2 : 1 + random.nextInt(2)) : radius - 1;
            for (int n = 0; n < length; n++) {
                end = end.relative(facing);
                log(plan, logs, end, facing.getAxis(), pine, random);
                if (!pine && n % 2 == 0) {
                    end = end.above();
                    log(plan, logs, end, Direction.Axis.Y, false, random);
                }
            }
            tips.add(end);
        }
        BlockState leaf = JugcraftAgriculture.block(pine ? "pine_needles" : "walnut_leaves").defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, false).setValue(LeavesBlock.DISTANCE, 7);
        if (pine) {
            // Small clumps leave the connected, exposed limbs readable.
            for (BlockPos end : tips) for (int y = 0; y <= 1; y++)
                for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
                    if (Math.abs(x) + Math.abs(z) <= (y == 0 ? 2 : 1))
                        plan.putIfAbsent(end.offset(x, y, z), leaf);
        } else {
            int depth = large ? 5 : 4;
            for (int y = height - depth; y < height; y++) {
                int layer = y - (height - depth);
                int r = layer == depth - 1 ? radius - 2 : layer == 0 ? radius - 1 : radius;
                for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
                    if (x*x + z*z > r*r + 1 || x*x + z*z > (r-1)*(r-1) && random.nextInt(7) == 0) continue;
                    BlockPos p = origin.offset(x, y, z);
                    var state = leaf.setValue(SeasonalLeavesBlock.SEASON,
                            WalnutLeavesBlock.SCHEDULE.on(JugcraftSeasons.today(), p));
                    plan.putIfAbsent(p, state);
                }
            }
        }
        // Real face-connected distances, not geometric estimates; discard unsupported foliage.
        var queue = new ArrayDeque<BlockPos>(logs);
        var distances = new HashMap<BlockPos, Integer>();
        logs.forEach(p -> distances.put(p, 0));
        while (!queue.isEmpty()) {
            BlockPos p = queue.remove(); int d = distances.get(p);
            if (d >= 6) continue;
            for (Direction direction : Direction.values()) {
                BlockPos next = p.relative(direction);
                if (!distances.containsKey(next) && plan.containsKey(next)) {
                    distances.put(next, d + 1); queue.add(next);
                }
            }
        }
        plan.entrySet().removeIf(e -> !logs.contains(e.getKey()) && !distances.containsKey(e.getKey()));
        plan.replaceAll((p, s) -> logs.contains(p) ? s : s.setValue(LeavesBlock.DISTANCE, distances.get(p)));
        for (BlockPos p : plan.keySet()) {
            var existing = level.getBlockState(p);
            if (!level.ensureCanWrite(p) || !existing.getFluidState().isEmpty()
                    || !(existing.isAir() || existing.is(BlockTags.REPLACEABLE_BY_TREES) || existing.is(BlockTags.LEAVES))) return false;
        }
        plan.forEach((p, s) -> level.setBlock(p, s, Block.UPDATE_CLIENTS));
        return true;
    }

    private static void log(Map<BlockPos, BlockState> plan, Set<BlockPos> logs, BlockPos pos,
                            Direction.Axis axis, boolean pine, RandomSource random) {
        String name = pine ? (random.nextInt(10) < 7 ? "stripped_pine_log" : "pine_log") : "walnut_log";
        plan.put(pos, JugcraftAgriculture.block(name).defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
        logs.add(pos);
    }
}
