package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import com.mojang.serialization.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.saveddata.*;

/** Source AE crater/core equations and biome variants, generated only by the normal new-chunk structure pass. */
public final class DigitalMeteorites {
    public static final Identifier ID = Jugcraft.id("jimtafacts/meteorite");
    public static StructureType<Meteorite> TYPE;
    public static StructurePieceType PIECE;
    public static Block CUBE;
    public static BlockEntityType<Core> CORE;
    private DigitalMeteorites() {}
    public static void register() {
        TYPE = Registry.register(BuiltInRegistries.STRUCTURE_TYPE, ID, () -> Meteorite.CODEC);
        PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE, ID, (StructurePieceType.ContextlessType) Piece::new);
        var key = ResourceKey.create(Registries.BLOCK, Jugcraft.id("jimta_mysterious_cube"));
        CUBE = Registry.register(BuiltInRegistries.BLOCK, key, new RewardCore(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(10, 1000).setId(key)));
        DigitalStorage.BLOCKS.put("mysterious_cube", CUBE);
        var itemKey = ResourceKey.create(Registries.ITEM, key.identifier()); Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(CUBE, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
        CORE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("jimta_meteorite_core"), FabricBlockEntityTypeBuilder.create(Core::new, CUBE).build());
        var compass = ResourceKey.create(Registries.ITEM, Jugcraft.id("jimta_meteorite_compass"));
        DigitalStorage.ITEMS.put("meteorite_compass", Registry.register(BuiltInRegistries.ITEM, compass, new Compass(new Item.Properties().setId(compass).stacksTo(1))));
        // Chunk-load notification runs on the server thread, never mutating SavedData from worldgen workers.
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> { for (var be : chunk.getBlockEntities().values()) if (be instanceof Core) Index.get(level).remember(be.getBlockPos()); });
    }
    public static final class Core extends BlockEntity { public Core(BlockPos p, BlockState s) { super(CORE, p, s); } }
    public static final class RewardCore extends Block implements EntityBlock {
        RewardCore(Properties p) { super(p); }
        @Override public BlockEntity newBlockEntity(BlockPos p, BlockState s) { return new Core(p, s); }
        @Override protected void onPlace(BlockState s, Level level, BlockPos p, BlockState old, boolean moved) { if (level instanceof ServerLevel server) Index.get(server).remember(p); }
        @Override protected void affectNeighborsAfterRemoval(BlockState s, ServerLevel level, BlockPos pos, boolean moved) { super.affectNeighborsAfterRemoval(s, level, pos, moved); Index.get(level).forget(pos); }
    }
    public static final class Index extends SavedData {
        public static final int LIMIT = 65_536;
        private static final Codec<Index> CODEC = BlockPos.CODEC.listOf(0, LIMIT).xmap(Index::new, i -> new ArrayList<>(i.positions));
        private static final SavedDataType<Index> DATA = new SavedDataType<>(Jugcraft.id("jimta_meteorite_compass"), () -> new Index(List.of()), CODEC, null);
        private final Set<BlockPos> positions = new HashSet<>();
        private final Map<Long, Set<BlockPos>> buckets = new HashMap<>();
        private Index(List<BlockPos> initial) { for (var p : initial) add(p); }
        public static Index get(ServerLevel l) { return l.getDataStorage().computeIfAbsent(DATA); }
        private static long bucket(int x, int z) { return ChunkPos.pack(x, z); }
        private void add(BlockPos p) { p = p.immutable(); if (positions.add(p)) buckets.computeIfAbsent(bucket(p.getX() >> 8, p.getZ() >> 8), k -> new HashSet<>()).add(p); }
        public void remember(BlockPos p) { if (positions.size() < LIMIT && !positions.contains(p)) { add(p); setDirty(); } }
        public void forget(BlockPos p) { if (positions.remove(p)) { var key = bucket(p.getX() >> 8, p.getZ() >> 8); var b = buckets.get(key); if (b != null && b.remove(p) && b.isEmpty()) buckets.remove(key); setDirty(); } }
        public BlockPos nearest(ServerLevel level, BlockPos player) {
            BlockPos closest = null; double distance = 2048 * 2048; int visited = 0;
            // 17x17 spatial buckets, at most4096 candidates; no chunk loads/tickets or full world scans.
            int cx = player.getX() >> 8, cz = player.getZ() >> 8;
            for (int r = 0; r <= 8; r++) for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) != r) continue;
                var points = buckets.get(bucket(cx + x, cz + z)); if (points == null) continue;
                for (BlockPos p : List.copyOf(points)) {
                    if (++visited > 4096) return closest;
                    if (level.hasChunkAt(p) && !level.getBlockState(p).is(CUBE)) { forget(p); continue; }
                    double d = (double)(p.getX() - player.getX()) * (p.getX() - player.getX()) + (double)(p.getZ() - player.getZ()) * (p.getZ() - player.getZ());
                    if (d < distance) { distance = d; closest = p; }
                }
            }
            return closest;
        }
    }
    public static final class Compass extends Item {
        private static final Map<ServerPlayer, Cached> CACHE = new WeakHashMap<>();
        private record Cached(ServerLevel level, long at, BlockPos target) {}
        Compass(Properties p) { super(p); }
        @Override public void inventoryTick(ItemStack stack, ServerLevel level, Entity carrier, EquipmentSlot slot) {
            if (!(carrier instanceof ServerPlayer p) || (level.getGameTime() + p.getId()) % 40 != 0) return;
            Cached cached = CACHE.get(p);
            if (cached == null || cached.level != level || level.getGameTime() - cached.at >= 100) { cached = new Cached(level, level.getGameTime(), Index.get(level).nearest(level, p.blockPosition())); CACHE.put(p, cached); }
            var target = cached.target == null ? Optional.<GlobalPos>empty() : Optional.of(GlobalPos.of(level.dimension(), cached.target));
            var tracker = new LodestoneTracker(target, false); if (!tracker.equals(stack.get(DataComponents.LODESTONE_TRACKER))) stack.set(DataComponents.LODESTONE_TRACKER, tracker);
        }
        @Override public void appendHoverText(ItemStack s, TooltipContext c, net.minecraft.world.item.component.TooltipDisplay d, java.util.function.Consumer<Component> text, TooltipFlag f) {
            text.accept(Component.literal("Points to nearby meteorite reward cores in explored terrain."));
            text.accept(Component.literal("Fast spin: within6 blocks. Slow spin: no core within2048 blocks."));
        }
    }
    private static TagKey<Biome> tag(String path) { return TagKey.create(Registries.BIOME, Identifier.parse(path)); }
    public enum Crater {
        NONE(Blocks.AIR), NORMAL(Blocks.AIR), LAVA(Blocks.LAVA), OBSIDIAN(Blocks.OBSIDIAN), WATER(Blocks.WATER), SNOW(Blocks.SNOW_BLOCK), ICE(Blocks.ICE);
        final Block filler; Crater(Block b) { filler = b; }
    }
    public static final class Meteorite extends Structure {
        public static final MapCodec<Meteorite> CODEC = simpleCodec(Meteorite::new);
        public Meteorite(StructureSettings settings) { super(settings); }
        @Override public StructureType<?> type() { return TYPE; }
        @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
            var gate = new WorldgenRandom(new LegacyRandomSource(0)); gate.setLargeFeatureSeed(context.seed(), context.chunkPos().x(), context.chunkPos().z());
            if (!gate.nextBoolean()) return Optional.empty();
            return onTopOfChunkCenter(context, Heightmap.Types.OCEAN_FLOOR_WG, pieces -> generate(context, pieces));
        }
        private static void generate(GenerationContext c, StructurePiecesBuilder pieces) {
            var random = c.random(); int x = c.chunkPos().getMinBlockX() + random.nextInt(16), z = c.chunkPos().getMinBlockZ() + random.nextInt(16);
            float radius = 2 + random.nextFloat() * 6; int offset = (int)Math.ceil(radius) + 1, range = Math.max(1, (int)(radius * 2));
            Holder<Biome> biome = c.biomeResolver().getBiomesWithin(x, c.chunkGenerator().getSeaLevel(), z, 0).iterator().next();
            boolean ocean = biome.is(BiomeTags.IS_OCEAN); var height = ocean ? Heightmap.Types.OCEAN_FLOOR_WG : Heightmap.Types.WORLD_SURFACE_WG;
            double sum = 0, squares = 0; int min = Integer.MAX_VALUE, samples = 0;
            for (int dx = -range; dx <= range; dx++) for (int dz = -range; dz <= range; dz++) { int h = c.chunkGenerator().getBaseHeight(x + dx, z + dz, height, c.heightAccessor(), c.randomState()); sum += h; squares += (double)h * h; min = Math.min(min, h); samples++; }
            double mean = sum / samples; int y = (int)mean;
            if (squares / samples - mean * mean > 5) y = (int)(y - (mean - min) * .75);
            y = Math.max(c.heightAccessor().getMinY() + offset, y - offset); BlockPos center = new BlockPos(x, y, z);
            boolean lake = false; int sea = c.chunkGenerator().getSeaLevel(), top = sea - 1;
            for (int dx = -32; dx <= 32 && !lake; dx++) for (int dz = -32; dz <= 32; dz++) {
                double d = dx * dx + dz * dz, bottom = y - radius + 1;
                if (top > bottom + d * .0175 && top < bottom + d * .02 && c.chunkGenerator().getBaseHeight(x + dx, z + dz, Heightmap.Types.OCEAN_FLOOR_WG, c.heightAccessor(), c.randomState()) < sea) { lake = true; break; }
            }
            int fallout = biome.is(tag("c:is_sandy")) || biome.is(BiomeTags.IS_BEACH) ? 1 : biome.is(BiomeTags.IS_BADLANDS) ? 2 : biome.is(tag("c:is_cold")) ? 3 : 0;
            pieces.addPiece(new Piece(center, radius, choose(center, biome, random, sea, ocean), fallout, random.nextFloat() > .9f, lake));
        }
        private static Crater choose(BlockPos pos, Holder<Biome> holder, RandomSource r, int sea, boolean ocean) {
            if (ocean) return Crater.NONE; if (r.nextFloat() <= .5) return Crater.NORMAL;
            Biome b = holder.value(); float t = b.getBaseTemperature(); boolean snow = b.coldEnoughToSnow(pos, sea);
            if (t >= 1) { boolean lava = r.nextFloat() > .5; if (!b.hasPrecipitation()) return lava ? Crater.LAVA : Crater.NORMAL; if (!snow) { var kind = r.nextFloat() > .75 ? Crater.OBSIDIAN : Crater.LAVA; return lava ? kind : Crater.NORMAL; } }
            if (t < 1 && t >= .2) { boolean lake = r.nextFloat() > .25, lava = r.nextFloat() > .8; if (!b.hasPrecipitation()) return lava ? Crater.LAVA : Crater.NORMAL; if (!snow) { var kind = r.nextFloat() > .75 ? Crater.OBSIDIAN : Crater.LAVA; return lava ? kind : lake ? Crater.WATER : Crater.NORMAL; } return r.nextFloat() > .75 ? Crater.SNOW : lake ? Crater.WATER : Crater.NORMAL; }
            if (t < .2) { boolean lake = r.nextFloat() > .25, lava = r.nextFloat() > .95, frozen = r.nextFloat() > .25; if (!b.hasPrecipitation()) return lava ? Crater.LAVA : Crater.NORMAL; if (!snow) return lava ? Crater.LAVA : lake ? frozen ? Crater.ICE : Crater.WATER : Crater.NORMAL; return lava ? Crater.LAVA : lake ? Crater.SNOW : Crater.NORMAL; }
            return Crater.NORMAL;
        }
    }
    public static final class Piece extends StructurePiece {
        final BlockPos center; final float radius; final Crater crater; final int fallout; final boolean pure, lake;
        Piece(BlockPos center, float r, Crater crater, int fallout, boolean pure, boolean lake) {
            super(PIECE, 0, bounds(center)); this.center = center; radius = r; this.crater = crater; this.fallout = fallout; this.pure = pure; this.lake = lake;
        }
        Piece(CompoundTag tag) {
            super(PIECE, tag); center = BlockPos.of(tag.getLongOr("c", 0)); radius = Math.clamp(tag.getFloatOr("r", 2), 2, 8);
            crater = Crater.values()[Math.clamp(tag.getIntOr("t", 1), 0, Crater.values().length - 1)]; fallout = Math.clamp(tag.getIntOr("f", 0), 0, 3); pure = tag.getBooleanOr("p", false); lake = tag.getBooleanOr("l", false);
        }
        private static BoundingBox bounds(BlockPos p) { var c = ChunkPos.containing(p); return new BoundingBox(c.getMinBlockX() - 64, p.getY(), c.getMinBlockZ() - 64, c.getMaxBlockX() + 64, p.getY(), c.getMaxBlockZ() + 64); }
        @Override protected void addAdditionalSaveData(StructurePieceSerializationContext c, CompoundTag t) { t.putLong("c", center.asLong()); t.putFloat("r", radius); t.putInt("t", crater.ordinal()); t.putInt("f", fallout); t.putBoolean("p", pure); t.putBoolean("l", lake); }
        @Override public BlockPos getLocatorPosition() { return center; }
        @Override public void postProcess(WorldGenLevel level, StructureManager manager, ChunkGenerator generator, RandomSource random, BoundingBox bounds, ChunkPos chunk, BlockPos pivot) { new Placer(level, this, random, bounds).place(); }
    }
    private static final class Placer {
        final WorldGenLevel level; final Piece s; final RandomSource random; final BoundingBox bounds; final BlockState sky;
        Placer(WorldGenLevel l, Piece p, RandomSource r, BoundingBox b) { level = l; s = p; random = r; bounds = b; sky = block("sky_stone_block"); }
        BlockState block(String name) { return DigitalStorage.BLOCKS.get(name).defaultBlockState(); }
        double surface(int x, int z) { double dx = x - s.center.getX(), dz = z - s.center.getZ(); return s.center.getY() - s.radius + 1 + (s.fallout == 1 || s.fallout == 3 ? 2 : 0) + (dx * dx + dz * dz) * .02; }
        void put(BlockPos p, BlockState state) {
            if (p.getX() < bounds.minX() || p.getX() > bounds.maxX() || p.getZ() < bounds.minZ() || p.getZ() > bounds.maxZ() || p.getY() < level.getMinY() || p.getY() > level.getMaxY()) return;
            var current = level.getBlockState(p); if (current.is(Blocks.BEDROCK) || current == state || current.hasBlockEntity()) return;
            level.setBlock(p, state, Block.UPDATE_CLIENTS);
        }
        void place() {
            int y = s.center.getY();
            if (s.crater != Crater.NONE) for (int x = bounds.minX(); x <= bounds.maxX(); x++) for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                int first = Math.max(y - 5, (int)Math.floor(surface(x, z)) + 1);
                for (int j = first; j <= level.getMaxY(); j++) { var p = new BlockPos(x, j, z); var current = level.getBlockState(p); if (s.crater != Crater.NORMAL && j < y && current.isSolid()) put(p, s.crater.filler.defaultBlockState()); else put(p, Blocks.AIR.defaultBlockState()); }
            }
            String[] quartz = {"quartz_block", "damaged_budding_quartz", "chipped_budding_quartz", "flawed_budding_quartz", "flawless_budding_quartz"};
            for (int dx = -8; dx <= 8; dx++) for (int dz = -8; dz <= 8; dz++) for (int dy = -8; dy < 8; dy++) {
                if (dx * dx * .7 + dy * dy * (dy > 0 ? 1.4 : .8) + dz * dz * .7 >= s.radius * s.radius) continue;
                BlockPos p = s.center.offset(dx, dy, dz); if (p.getX() < bounds.minX() || p.getX() > bounds.maxX() || p.getZ() < bounds.minZ() || p.getZ() > bounds.maxZ()) continue;
                if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1 && Math.abs(dz) <= 1) {
                    if (dy == -1) { int grade = random.nextInt(quartz.length); put(p, block(quartz[grade])); if (grade != 0 && (dx != 0 || dz != 0) && random.nextFloat() <= .7) put(p.above(), block(new String[]{"small_quartz_bud", "medium_quartz_bud", "large_quartz_bud"}[random.nextInt(3)]).setValue(AmethystClusterBlock.FACING, Direction.UP)); }
                    else put(p, Blocks.AIR.defaultBlockState());
                } else put(p, sky);
            }
            put(s.center, CUBE.defaultBlockState());
            if (s.crater != Crater.NONE) decay();
            if (s.lake) lake();
        }
        void decay() {
            double crater = Math.pow(s.radius * 2 + 5, 2); int falls = 0;
            for (int x = Math.max(bounds.minX(), s.center.getX() - 30); x <= Math.min(bounds.maxX(), s.center.getX() + 30); x++) for (int z = Math.max(bounds.minZ(), s.center.getZ() - 30); z <= Math.min(bounds.maxZ(), s.center.getZ() + 30); z++) for (int y = Math.max(level.getMinY() + 1, s.center.getY() - 9); y < Math.min(level.getMaxY(), s.center.getY() + 30); y++) {
                var p = new BlockPos(x, y, z); var state = level.getBlockState(p); if (state.hasBlockEntity() || s.pure && state.is(s.crater.filler)) continue;
                double dx = x - s.center.getX(), dy = y - s.center.getY(), dz = z - s.center.getZ(), dist = dx * dx + dy * dy + dz * dz;
                if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1 && Math.abs(dz) <= 1) continue;
                if (state.canBeReplaced()) {
                    var above = level.getBlockState(p.above()); if (!above.isAir() && !above.hasBlockEntity()) { put(p, above); continue; }
                    var below = level.getBlockState(p.below()); if (falls < 100 * crater && !below.canBeReplaced() && !below.isAir() && crater * (random.nextDouble() * .6 + .2) - Math.abs(dist - crater * 1.7) > 0 && random.nextDouble() > .6) { fallout(p, false); falls++; }
                } else if (level.isEmptyBlock(p.above()) && random.nextDouble() > .4 && dist < crater * 1.6) fallout(p, true);
            }
        }
        void fallout(BlockPos p, boolean inset) {
            float a = random.nextFloat(); Block b = null;
            if (s.fallout == 0) {
                b = inset ? a > .9 ? Blocks.COBBLESTONE : a > .8 ? Blocks.STONE : a > .7 ? Blocks.GRASS_BLOCK : a > .6 ? DigitalStorage.BLOCKS.get("sky_stone_block") : a > .5 ? Blocks.GRAVEL : Blocks.AIR
                        : a > .9 ? Blocks.STONE : a > .8 ? Blocks.COBBLESTONE : a > .7 ? Blocks.DIRT : Blocks.GRAVEL;
            } else if (a > .9) b = s.fallout == 1 ? Blocks.SAND : s.fallout == 2 ? Blocks.TERRACOTTA : Blocks.SNOW_BLOCK;
            else if (inset && a > .8) b = Blocks.AIR;
            else { float other = inset ? a - .1f : a; if (s.fallout == 1 && other > .66) b = Blocks.GLASS; if (s.fallout == 3) b = other > .7 ? Blocks.SNOW : other > .5 ? Blocks.ICE : null; }
            if (b != null) put(p, b.defaultBlockState());
        }
        void lake() {
            int top = level.getSeaLevel() - 1;
            for (int x = bounds.minX(); x <= bounds.maxX(); x++) for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) for (int y = Math.max(level.getMinY(), s.center.getY() - 5); y <= Math.min(top, level.getMaxY()); y++) {
                var p = new BlockPos(x, y, z); double h = surface(x, z);
                if (y > h) { if (level.getBlockState(p).isAir()) { put(p, Blocks.WATER.defaultBlockState()); if (y == top) level.scheduleTick(p, Fluids.WATER, 0); } }
                else if (top + (top - y) * 2 + 2 > h) for (int depth = 0; depth < 20 && y - depth >= level.getMinY(); depth++) { var down = p.below(depth); var state = level.getBlockState(down); if (!state.isAir() && !(state.getFluidState().isEmpty() && state.canBeReplaced())) break; if (s.crater == Crater.LAVA && random.nextFloat() < .075) put(down, Blocks.MAGMA_BLOCK.defaultBlockState()); else fallout(down, false); }
            }
        }
    }
}
