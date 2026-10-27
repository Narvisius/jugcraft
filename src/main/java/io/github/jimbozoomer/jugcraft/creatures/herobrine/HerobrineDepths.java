package io.github.jimbozoomer.jugcraft.creatures.herobrine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/** Permanent, bounded cave discoveries. Uses the shared recorded/rechecked construction queue. */
public final class HerobrineDepths {
    public static final List<String> CAVES = List.of("torch_trail", "shelter", "watching_room", "sealed_stairs",
            "pillar_grid", "broken_bridge", "balcony", "buried_shrine", "empty_hall");
    public static final List<String> WATER = List.of("glowstone_ring", "glowstone_cross", "glowstone_pyramid", "glowstone_trail");
    private HerobrineDepths() { }

    /** Three-dimensional spacing permits discoveries on different cave levels, not one exclusion column. */
    public static boolean spaced(ServerLevel level, BlockPos at, boolean water) {
        return HerobrineMarks.get(level.getServer()).marks().values().stream().noneMatch(mark -> {
            if (!mark.type().equals("underground") && !mark.type().equals("underwater")) return false;
            long dx = (long) mark.at().getX() - at.getX(), dz = (long) mark.at().getZ() - at.getZ();
            return Math.abs(mark.at().getY() - at.getY()) < 20 && dx * dx + dz * dz < (water ? 64L * 64 : 48L * 48);
        });
    }

    public static List<String> variantsAt(int y, int minY) {
        if (y < minY + 32) return List.of("buried_shrine", "empty_hall", "pillar_grid", "sealed_stairs");
        if (y < 0) return List.of("watching_room", "pillar_grid", "broken_bridge", "balcony", "empty_hall");
        return List.of("torch_trail", "shelter", "watching_room", "sealed_stairs", "broken_bridge");
    }

    /** At most six local candidates, each examining a 25-block vertical band; never loads chunks. */
    public static HerobrineMarkPlans.@Nullable Plan find(ServerLevel level, BlockPos near, boolean water) {
        for (int attempt = 0; attempt < 6; attempt++) {
            int dx = level.getRandom().nextInt(49) - 24, dz = level.getRandom().nextInt(49) - 24;
            BlockPos column = near.offset(dx, 0, dz);
            if (!HerobrineMarkPlans.loaded(level, column.offset(-8, -16, -8), column.offset(8, 16, 16))) continue;
            if (water && attempt < 3) column = new BlockPos(column.getX(),
                    level.getHeight(Heightmap.Types.OCEAN_FLOOR, column.getX(), column.getZ()), column.getZ());
            for (int offset = 12; offset >= -12; offset--) {
                BlockPos feet = column.above(offset);
                if (feet.getY() < level.getMinY() + 8 || feet.getY() > level.getMaxY() - 16) continue;
                if (!level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)) continue;
                if (water ? !sourceWater(level.getBlockState(feet)) : !level.getBlockState(feet).isAir()) continue;
                if (!water && level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, feet.getX(), feet.getZ()) < feet.getY() + 8) continue;
                if (!spaced(level, feet, water)) continue;
                List<String> variants = water ? WATER : variantsAt(feet.getY(), level.getMinY());
                HerobrineMarkPlans.Plan plan = plan(level, feet, variants.get(level.getRandom().nextInt(variants.size())),
                        Rotation.values()[level.getRandom().nextInt(4)]);
                if (plan == null && !water) plan = plan(level, feet, "torch_trail",
                        Rotation.values()[level.getRandom().nextInt(4)]);
                if (plan != null) return plan;
                break;
            }
        }
        return null;
    }

    /** Feet is the entry/floor level. North is the open entrance, every room has a two-block doorway. */
    public static HerobrineMarkPlans.@Nullable Plan plan(ServerLevel level, BlockPos feet, String variant) {
        return plan(level, feet, variant, Rotation.NONE);
    }

    public static HerobrineMarkPlans.@Nullable Plan plan(ServerLevel level, BlockPos feet, String variant, Rotation rotation) {
        boolean water = WATER.contains(variant);
        if (!water && !CAVES.contains(variant)) return null;
        if (!HerobrineMarkPlans.loaded(level, feet.offset(-8, -8, -8), feet.offset(8, 12, 16))) return null;
        Layout b = new Layout(feet);
        if (water) underwater(b, variant);
        else switch (variant) {
            case "torch_trail" -> {
                for (int z = 0; z <= 12; z += 3) {
                    b.put((z / 3) % 2 == 0 ? -1 : 1, 0, z, Blocks.REDSTONE_TORCH.defaultBlockState());
                }
                b.put(0, 0, 13, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
                b.put(0, 1, 13, Blocks.SKELETON_SKULL.defaultBlockState());
            }
            case "shelter", "watching_room", "empty_hall", "buried_shrine" -> {
                int width = variant.equals("empty_hall") ? 5 : 3;
                int length = variant.equals("empty_hall") ? 12 : 8;
                b.room(width, length, variant.equals("empty_hall") ? 6 : 4);
                if (variant.equals("shelter")) {
                    b.put(-2, 0, 5, Blocks.CRAFTING_TABLE.defaultBlockState());
                    b.put(-2, 0, 6, Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
                    b.put(2, 0, 5, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
                    b.put(2, 0, 6, Blocks.SPRUCE_SLAB.defaultBlockState());
                } else if (variant.equals("watching_room")) {
                    b.put(0, 0, 5, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
                    b.put(0, 2, 8, Blocks.GLASS.defaultBlockState());
                } else if (variant.equals("buried_shrine")) {
                    for (int x = -1; x <= 1; x++) b.put(x, 0, 5, Blocks.CHISELED_DEEPSLATE.defaultBlockState());
                    b.put(0, 1, 5, Blocks.SKELETON_SKULL.defaultBlockState());
                    b.put(-2, 0, 5, Blocks.REDSTONE_TORCH.defaultBlockState());
                    b.put(2, 0, 5, Blocks.REDSTONE_TORCH.defaultBlockState());
                } else {
                    for (int z = 3; z < length; z += 4) for (int x : new int[]{-3, 3})
                        for (int y = 0; y < 6; y++) b.put(x, y, z, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                    b.put(0, 0, length - 2, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
                }
            }
            case "pillar_grid" -> {
                for (int x = -3; x <= 3; x += 3) for (int z = 0; z <= 6; z += 3)
                    for (int y = 0; y < 5; y++) b.put(x, y, z, (y == 4 ? Blocks.CHISELED_STONE_BRICKS : Blocks.STONE_BRICKS).defaultBlockState());
                b.put(0, 5, 3, Blocks.REDSTONE_TORCH.defaultBlockState());
            }
            case "sealed_stairs" -> {
                for (int z = 0; z <= 6; z++) for (int x = -1; x <= 1; x++) {
                    b.put(x, z, z, Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
                    for (int y = 0; y < z; y++) b.put(x, y, z, Blocks.STONE_BRICKS.defaultBlockState());
                    for (int y = 1; y <= 3; y++) b.put(x, z + y, z, Blocks.AIR.defaultBlockState());
                }
                for (int x = -2; x <= 2; x++) for (int y = 6; y <= 10; y++)
                    b.put(x, y, 7, Blocks.DEEPSLATE_BRICKS.defaultBlockState());
            }
            case "broken_bridge", "balcony" -> {
                int length = variant.equals("balcony") ? 4 : 10;
                for (int z = 0; z <= length; z++) for (int x = -2; x <= 2; x++) {
                    if (variant.equals("broken_bridge") && z > 7 && x == 0) continue;
                    b.put(x, -1, z, (z % 3 == 0 ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS).defaultBlockState());
                    if (Math.abs(x) == 2 && z % 2 == 0) b.put(x, 0, z, Blocks.COBBLESTONE_WALL.defaultBlockState());
                }
                b.put(-2, 1, length, Blocks.REDSTONE_TORCH.defaultBlockState());
                b.put(2, 1, length, Blocks.REDSTONE_TORCH.defaultBlockState());
            }
            default -> throw new IllegalArgumentException(variant);
        }
        b.rotate(rotation);
        if (variant.equals("torch_trail") && !b.followFloor(level)) return null;
        return b.finish(level, water);
    }

    private static void underwater(Layout b, String variant) {
        if (variant.equals("glowstone_pyramid")) {
            for (int y = 0; y <= 3; y++) for (int x = y - 3; x <= 3 - y; x++) for (int z = y - 3; z <= 3 - y; z++)
                b.put(x, y, z, (Math.abs(x) == 3 - y || Math.abs(z) == 3 - y ? Blocks.GLOWSTONE : Blocks.MOSSY_COBBLESTONE).defaultBlockState());
        } else if (variant.equals("glowstone_ring")) {
            for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++)
                if (x * x + z * z >= 10 && x * x + z * z <= 20) b.put(x, 0, z, Blocks.GLOWSTONE.defaultBlockState());
        } else if (variant.equals("glowstone_cross")) {
            for (int n = -4; n <= 4; n++) { b.put(n, 0, 0, Blocks.GLOWSTONE.defaultBlockState()); b.put(0, 0, n, Blocks.GLOWSTONE.defaultBlockState()); }
        } else {
            for (int z = 0; z <= 12; z += 2) b.put(0, 0, z, Blocks.GLOWSTONE.defaultBlockState());
            for (int x = -2; x <= 2; x++) b.put(x, 0, 12, Blocks.GLOWSTONE.defaultBlockState());
        }
    }

    private static boolean sourceWater(BlockState state) {
        return state.is(Blocks.WATER) && state.getFluidState().is(FluidTags.WATER) && state.getFluidState().isSource();
    }

    private static final class Layout {
        final BlockPos at;
        final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        Layout(BlockPos at) { this.at = at.immutable(); }
        void put(int x, int y, int z, BlockState state) { blocks.put(at.offset(x, y, z), state); }
        void rotate(Rotation rotation) {
            if (rotation == Rotation.NONE) return;
            Map<BlockPos, BlockState> rotated = new LinkedHashMap<>();
            blocks.forEach((p, state) -> rotated.put(at.offset(p.subtract(at).rotate(rotation)), state.rotate(rotation)));
            blocks.clear(); blocks.putAll(rotated);
        }
        boolean followFloor(ServerLevel level) {
            // A small trail follows uneven cave ground; it must not need a perfectly flat thirteen-block corridor.
            Map<BlockPos, Integer> offsets = new LinkedHashMap<>();
            Map<BlockPos, BlockState> adjusted = new LinkedHashMap<>();
            for (var entry : blocks.entrySet()) {
                BlockPos column = new BlockPos(entry.getKey().getX(), at.getY(), entry.getKey().getZ());
                if (!HerobrineMarkPlans.loaded(level, column.below(3), column.above(4))) return false;
                if (!offsets.containsKey(column)) {
                    Integer found = null;
                    for (int dy = 2; dy >= -2; dy--) {
                        BlockPos base = column.above(dy);
                        if (HerobrineMarkPlans.open(level.getBlockState(base))
                                && level.getBlockState(base.below()).isFaceSturdy(level, base.below(), Direction.UP)) { found = dy; break; }
                    }
                    if (found == null) return false;
                    offsets.put(column, found);
                }
                adjusted.put(entry.getKey().above(offsets.get(column)), entry.getValue());
            }
            blocks.clear(); blocks.putAll(adjusted);
            return true;
        }
        void room(int half, int length, int height) {
            for (int x = -half; x <= half; x++) for (int z = 0; z <= length; z++) for (int y = -1; y <= height; y++) {
                boolean shell = y == -1 || y == height || Math.abs(x) == half || z == 0 || z == length;
                BlockState stone = ((x + z + y) % 5 == 0 ? Blocks.MOSSY_STONE_BRICKS : Blocks.STONE_BRICKS).defaultBlockState();
                put(x, y, z, shell ? stone : Blocks.AIR.defaultBlockState());
            }
            for (int x = -1; x <= 0; x++) for (int y = 0; y < 3; y++) put(x, y, 0, Blocks.AIR.defaultBlockState());
            put(-half + 1, 0, length - 1, Blocks.REDSTONE_TORCH.defaultBlockState());
        }
        HerobrineMarkPlans.@Nullable Plan finish(ServerLevel level, boolean water) {
            BlockPos min = at, max = at;
            for (BlockPos p : blocks.keySet()) {
                min = new BlockPos(Math.min(min.getX(), p.getX()), Math.min(min.getY(), p.getY()), Math.min(min.getZ(), p.getZ()));
                max = new BlockPos(Math.max(max.getX(), p.getX()), Math.max(max.getY(), p.getY()), Math.max(max.getZ(), p.getZ()));
            }
            if (min.getY() < level.getMinY() + 3 || max.getY() + HerobrineMarkPlans.MARGIN >= level.getMaxY()
                    || !HerobrineMarkPlans.loaded(level, min, max) || !HerobrineMarkPlans.wild(level, min, max)) return null;
            boolean approved = HerobrineMarks.approved(level, min, max);
            List<HerobrineMarkPlans.Step> steps = new ArrayList<>();
            for (var entry : blocks.entrySet()) {
                BlockPos p = entry.getKey();
                BlockState before = level.getBlockState(p), after = entry.getValue();
                if (water) {
                    if (!sourceWater(before)) return null;
                    if (!blocks.containsKey(p.below()) && !level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) return null;
                } else {
                    // Retain an existing cave floor instead of replacing it for cosmetic reasons.
                    if (p.getY() == at.getY() - 1 && before.isFaceSturdy(level, p, Direction.UP)) continue;
                    if (before.equals(after)) continue;
                    if (!HerobrineMarkPlans.open(before) && (!approved || !HerobrineMarkPlans.diggable(level, p))) return null;
                    if (after.isAir()) {
                        if (before.isAir()) continue;
                        for (Direction d : Direction.values()) if (!level.getBlockState(p.relative(d)).getFluidState().isEmpty()) return null;
                    }
                }
                steps.add(new HerobrineMarkPlans.Step(p, before.getBlock(), after));
            }
            if (steps.isEmpty()) return null;
            if (!water) {
                for (var entry : blocks.entrySet()) {
                    BlockPos p = entry.getKey();
                    if (entry.getValue().isAir()) continue;
                    if (p.getY() != min.getY() && !entry.getValue().is(Blocks.REDSTONE_TORCH)
                            && !entry.getValue().is(Blocks.SKELETON_SKULL)) continue;
                    BlockState support = blocks.get(p.below());
                    if (support != null && !support.isAir()) continue;
                    if (!level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) {
                        // Bridges have a supported root; their deck intentionally spans open space.
                        boolean deck = p.getY() == at.getY() - 1 && level.getBlockState(at.below(2)).isFaceSturdy(level, at.below(2), Direction.UP);
                        if (!deck) return null;
                    }
                }
            }
            // Excavate first, then foundations/walls, then supported decor. Never excavate into a liquid.
            steps.sort(Comparator.comparingInt((HerobrineMarkPlans.Step s) -> s.state().isAir() ? 0 : 1)
                    .thenComparingInt(s -> s.pos().getY()));
            return new HerobrineMarkPlans.Plan(water ? HerobrineMarkPlans.Type.UNDERWATER : HerobrineMarkPlans.Type.UNDERGROUND,
                    at, steps, Map.of(), List.of());
        }
    }
}
