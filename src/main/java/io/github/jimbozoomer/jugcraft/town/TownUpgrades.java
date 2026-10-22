package io.github.jimbozoomer.jugcraft.town;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.phys.AABB;

/** Explicit, resumable blueprint migrations. Never call the terrain builder for an existing chunk. */
public final class TownUpgrades {
    public static final int CURRENT = 3;
    private static final Map<Integer, TownData> HISTORY = new HashMap<>();
    private TownUpgrades() {}
    public static void register() {
        // Entity NBT may arrive after the chunk upgrade has already completed.
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (!(entity instanceof ItemFrame frame) || !frame.isPermanentlyInvulnerable() || level != level.getServer().overworld()) return;
            BlockPos origin = TownState.get(level).origin();
            if (origin == null || version(level, ChunkPos.containing(frame.blockPosition()).pack()) < 2) return;
            var map = frame.getItem().get(DataComponents.MAP_ID);
            if (map == null || map.id() != TownState.get(level).townMap()) return;
            for (var board : historical(1).mapBoards) {
                if (frame.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(origin.offset(board.pos()))) < 1) {
                    frame.discard(); break;
                }
            }
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;
            var level = server.overworld();
            BlockPos origin = TownState.get(level).origin();
            if (origin == null || TownState.get(level).townMap() < 0) return;
            for (var board : historical(1).mapBoards) {
                var at = origin.offset(board.pos());
                if (version(level, ChunkPos.containing(at).pack()) >= 2 && level.isPositionEntityTicking(at)) removeOldMap(level,at);
            }
        });
    }
    private static void removeOldMap(ServerLevel level, BlockPos at) {
        for (var frame : level.getEntitiesOfClass(ItemFrame.class, new AABB(at).inflate(.6), f ->
                f.isPermanentlyInvulnerable() && Objects.equals(f.getItem().get(DataComponents.MAP_ID), new MapId(TownState.get(level).townMap())))) frame.discard();
    }

    public static final class Progress extends SavedData {
        public static final Codec<Progress> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.optionalFieldOf("origin").forGetter(s -> s.origin),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("revisions", Map.of()).forGetter(s -> s.revisions),
            Codec.INT.optionalFieldOf("requested", 1).forGetter(s -> s.requested),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("conflicts", Map.of()).forGetter(s -> s.conflicts)
        ).apply(i, Progress::new));
        private Optional<BlockPos> origin;
        private final Map<String, Integer> revisions, conflicts;
        private int requested;
        public Progress() { this(Optional.empty(), Map.of(), 1, Map.of()); }
        private Progress(Optional<BlockPos> origin, Map<String,Integer> revisions, int requested, Map<String,Integer> conflicts) {
            this.origin = origin; this.revisions = new HashMap<>(revisions);
            this.requested = requested; this.conflicts = new HashMap<>(conflicts);
        }
    }
    private static final SavedDataType<Progress> TYPE = new SavedDataType<>(Jugcraft.id("town_upgrades"), Progress::new, Progress.CODEC, null);

    public static Progress progress(ServerLevel level) {
        Progress p = level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
        Optional<BlockPos> origin = Optional.ofNullable(TownState.get(level).origin());
        if (!p.origin.equals(origin)) {
            p.origin = origin; p.revisions.clear(); p.conflicts.clear(); p.requested = 1; p.setDirty();
        }
        return p;
    }
    public static int version(ServerLevel level, long chunk) {
        return progress(level).revisions.getOrDefault(Long.toString(chunk), 1);
    }
    public static void markCurrent(ServerLevel level, long chunk) {
        Progress p = progress(level); p.revisions.put(Long.toString(chunk), CURRENT); p.setDirty();
    }
    public static boolean pending(ServerLevel level, long chunk) {
        return TownState.get(level).built(chunk) && progress(level).requested >= CURRENT && version(level, chunk) < CURRENT;
    }
    public static int request(CommandSourceStack source) {
        ServerLevel level = source.getServer().overworld();
        if (TownState.get(level).origin() == null) { source.sendFailure(Component.literal("This world has no spawn town.")); return 0; }
        // Fail before changing saved progress if a historical blueprint is missing or corrupt.
        for (int v = 1; v < CURRENT; v++) historical(v);
        Progress p = progress(level); p.requested = CURRENT; p.setDirty();
        TownBuilder.queueLoaded(level);
        source.sendSuccess(() -> Component.literal("Town upgrade requested. Loaded chunks update one per tick; other chunks resume when visited. Edited blocks are preserved. Use /jugcraft town upgrade for progress."), true);
        return 1;
    }
    public static int status(CommandSourceStack source) {
        ServerLevel level = source.getServer().overworld();
        BlockPos origin = TownState.get(level).origin();
        if (origin == null) { source.sendFailure(Component.literal("This world has no spawn town.")); return 0; }
        int pending = 0, loaded = 0;
        for (int x = origin.getX() >> 4; x <= (origin.getX() + TownData.get().size - 1) >> 4; x++) {
            for (int z = origin.getZ() >> 4; z <= (origin.getZ() + TownData.get().size - 1) >> 4; z++) {
                long key = new ChunkPos(x,z).pack();
                if (TownState.get(level).built(key) && version(level,key) < CURRENT) {
                    pending++; if (level.getChunkSource().getChunkNow(x,z) != null) loaded++;
                }
            }
        }
        Progress p = progress(level);
        String message = "Town revision " + CURRENT + ": " + pending + " built chunks need upgrading (" + loaded
            + " loaded). " + p.conflicts.values().stream().mapToInt(Integer::intValue).sum() + " edited/protected cells preserved. "
            + (p.requested >= CURRENT ? "Upgrade enabled; unloaded chunks resume on visit." : "Preview only; use /jugcraft town upgrade apply.");
        source.sendSuccess(() -> Component.literal(message), false);
        return pending;
    }
    public static TownData historical(int version) { return HISTORY.computeIfAbsent(version, TownData::revision); }

    /** Applies one already-loaded chunk. Repeated application is a no-op. Conflicts remain untouched, with a saved count. */
    public static void applyChunk(ServerLevel level, ChunkPos chunk) {
        TownState town = TownState.get(level);
        BlockPos origin = town.origin();
        int version = version(level, chunk.pack());
        if (origin == null || !town.built(chunk.pack()) || version >= CURRENT
                || level.getChunkSource().getChunkNow(chunk.x(), chunk.z()) == null) return;
        TownData old = historical(version), next = TownData.get();
        if (old.size != next.size) throw new IllegalStateException("Town footprint changes require an explicit expansion migration");
        Map<BlockPos, BlockState> oldDecor = decorations(old, town.theme()), nextDecor = decorations(next, town.theme());
        Set<BlockPos> defenseCells = new HashSet<>();
        for (var spot : next.spots) if (spot.role().equals("flak_guard")) {
            for (int dx=-1; dx<=1; dx++) for (int dz=-1; dz<=1; dz++) for (int dy=-1; dy<=3; dy++)
                defenseCells.add(spot.pos().offset(dx,dy,dz));
        }
        int conflicts = 0;
        Set<BlockPos> placedSigns = new HashSet<>();
        for (int wx = chunk.x()<<4; wx < (chunk.x()<<4)+16; wx++) for (int wz = chunk.z()<<4; wz < (chunk.z()<<4)+16; wz++) {
            int x = wx-origin.getX(), z = wz-origin.getZ();
            // The blending ring and Styx's separately authored district own their terrain.
            if (old.mask(x,z) != TownData.INSIDE || next.mask(x,z) != TownData.INSIDE
                    || town.styxDistrict() && io.github.jimbozoomer.jugcraft.styx.StyxTownDistrict.contains(x,z)) continue;
            for (int y = Math.min(old.yMin,next.yMin); y < Math.max(old.yMin+old.height,next.yMin+next.height); y++) {
                BlockPos relative = new BlockPos(x,y,z), at = origin.offset(relative);
                if (defenseCells.contains(relative)) continue;
                int before = old.index(x,y,z), after = next.index(x,y,z);
                if (before == TownData.KEEP || after == TownData.KEEP) continue;
                BlockState expected = oldDecor.getOrDefault(relative,old.state(before));
                BlockState wanted = nextDecor.getOrDefault(relative,next.state(after));
                if (expected == wanted) continue;
                if (!replaceAuthored(level, at, expected, wanted)) conflicts++;
                else if (wanted.getBlock() instanceof net.minecraft.world.level.block.SignBlock) placedSigns.add(at);
            }
        }
        // Tower tops protrude into the terrain-blending ring. Migrate only the
        // tightly authored gun footprint there, never the surrounding landscape.
        // Old KEEP cells may receive supports only if they are actually empty.
        for (var relative : defenseCells) {
            BlockPos at = origin.offset(relative);
            if (!ChunkPos.containing(at).equals(chunk)) continue;
            if (town.styxDistrict() && io.github.jimbozoomer.jugcraft.styx.StyxTownDistrict.contains(relative.getX(), relative.getZ())) continue;
            int before=old.index(relative.getX(),relative.getY(),relative.getZ());
            int after=next.index(relative.getX(),relative.getY(),relative.getZ());
            if (after==TownData.KEEP) continue;
            BlockState expected=oldDecor.getOrDefault(relative,before==TownData.KEEP?Blocks.AIR.defaultBlockState():old.state(before));
            BlockState wanted=next.state(after);
            if (expected!=wanted && !replaceAuthored(level,at,expected,wanted)) conflicts++;
        }
        // Text is its own migration: preserve operator-edited signs, update both vanilla faces.
        for (var entry : next.signs) {
            BlockPos at = origin.offset(entry.pos());
            if (!ChunkPos.containing(at).equals(chunk) || !(level.getBlockEntity(at) instanceof SignBlockEntity sign)) continue;
            var previous = old.signs.stream().filter(s -> s.pos().equals(entry.pos())).findFirst();
            boolean untouched = true;
            for (var slot : SignTextSlot.values()) {
                var text = sign.getText(slot);
                for (int line=0; line<4; line++) {
                    String actual = text.getMessages(false).get(line).getString();
                    if (!actual.equals(entry.lines().get(line)) && !(previous.isPresent() && actual.equals(previous.get().lines().get(line)))
                            && !(previous.isEmpty() && placedSigns.contains(at) && actual.isEmpty())) untouched = false;
                }
            }
            if (untouched) TownWayfinding.writeSign(level, at, entry.lines()); else conflicts++;
        }
        // Remove only the old town-owned map at its authored anchor; leave players' frames/maps alone.
        for (var board : old.mapBoards) {
            BlockPos at = origin.offset(board.pos());
            if (!ChunkPos.containing(at).equals(chunk)) continue;
            removeOldMap(level,at);
        }
        TownsfolkCare.chunkBuilt(level, chunk);
        Progress p = progress(level);
        p.conflicts.put(Long.toString(chunk.pack()), conflicts);
        markCurrent(level,chunk.pack());
        if (conflicts > 0) Jugcraft.LOGGER.info("Town upgrade {}: preserved {} edited/protected cells in {}", CURRENT, conflicts, chunk);
    }

    /** Conservative compare-and-set. Containers are never cleared, even when their block type matches. */
    public static boolean replaceAuthored(ServerLevel level, BlockPos pos, BlockState expected, BlockState wanted) {
        if (level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4) == null) return false;
        BlockState actual = level.getBlockState(pos);
        if (actual == wanted) return true;
        if (actual != expected) return false;
        if (level.getBlockEntity(pos) != null) {
            if (level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.BannerBlockEntity banner) {
                // Plain authored tower flags can retire. Player-named or patterned
                // banners remain untouched, just like other edited town scenery.
                if (!wanted.isAir() || banner.getCustomName() != null || !banner.getPatterns().layers().isEmpty()) return false;
                BlockPos origin = TownState.get(level).origin();
                if (origin == null || historical(version(level,ChunkPos.containing(pos).pack())).sites.stream()
                        .filter(site -> site.kind().equals("tower_flag"))
                        .noneMatch(site -> java.util.Arrays.stream(site.blocks()).anyMatch(b -> origin.offset(b[0],b[1],b[2]).equals(pos)))) return false;
            } else {
            // Retired generated signposts may be removed only if their text still matches the old blueprint.
            if (!(level.getBlockEntity(pos) instanceof SignBlockEntity sign) || !wanted.isAir()) return false;
            BlockPos origin = TownState.get(level).origin();
            if (origin == null) return false;
            var entry = historical(version(level,ChunkPos.containing(pos).pack())).signs.stream().filter(s -> origin.offset(s.pos()).equals(pos)).findFirst();
            if (entry.isEmpty()) return false;
            for (var slot : SignTextSlot.values()) for (int i=0;i<4;i++)
                if (!sign.getText(slot).getMessages(false).get(i).getString().equals(entry.get().lines().get(i))) return false;
            }
        }
        TownBuilder.set(level,pos,wanted);
        return true;
    }
    private static Map<BlockPos,BlockState> decorations(TownData data,String theme) {
        Map<BlockPos,BlockState> result = new HashMap<>();
        for (var site : data.sites) {
            // Retired sites keep their saved identity, but the new blueprint now
            // owns these cells (including the solid gun pedestal).
            if (site.kind().equals("retired")) continue;
            for (var b : site.blocks())
                result.put(new BlockPos(b[0],b[1],b[2]),TownDecor.stateFor(data,site,theme.isEmpty()?TownDecor.theme():theme,b[3]));
        }
        return result;
    }
}
