package io.github.jimbozoomer.jugcraft.server;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Explicit crouch + ordinary axe felling. Bounded discovery and a server-wide per-tick break budget. */
public final class TreeFelling {
    public static final int MAX_LOGS = 256, RADIUS = 10, HEIGHT = 48;
    private record Job(ServerPlayer player, ServerLevel level, BlockPos root, ItemStack axe, Block block, ArrayDeque<BlockPos> remaining) {}
    private static final Map<UUID, Job> JOBS = new LinkedHashMap<>();
    private static final Set<UUID> BREAKING = new HashSet<>();
    private TreeFelling() {}
    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, entity) -> {
            if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer p) || BREAKING.contains(p.getUUID()) || JOBS.containsKey(p.getUUID())
                    || !p.isShiftKeyDown() || p.getAbilities().instabuild || !p.mayBuild() || !state.is(BlockTags.LOGS)) return;
            var axe = p.getMainHandItem();
            // Special powered/alchemical tools retain their own modes, charge costs and limits.
            if (!axe.is(ItemTags.AXES) || !axe.isDamageableItem()
                    || axe.getItem() instanceof io.github.jimbozoomer.jugcraft.tools.PoweredToolItem
                    || axe.getItem() instanceof io.github.jimbozoomer.jugcraft.concordance.assay.DarkMatterTool
                    || axe.getItem() instanceof io.github.jimbozoomer.jugcraft.concordance.assay.ExtendedMatterTool) return;
            var logs = discover(serverLevel, pos, state.getBlock());
            if (logs.isEmpty()) return;
            JOBS.put(p.getUUID(), new Job(p, serverLevel, pos.immutable(), axe, state.getBlock(), new ArrayDeque<>(logs)));
            p.sendOverlayMessage(Component.literal("Felling tree — " + (logs.size() + 1) + " logs. Switch tools to stop."));
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int budget = 64;
            for (var job : List.copyOf(JOBS.values())) {
                if (job.level.getServer() != server) continue;
                var p = job.player;
                if (!p.isAlive() || p.hasDisconnected() || p.level() != job.level || p.getMainHandItem() != job.axe || job.axe.isEmpty()
                        || p.distanceToSqr(job.root.getX(), job.root.getY(), job.root.getZ()) > 32 * 32) { JOBS.remove(p.getUUID()); continue; }
                for (int n = 0; n < 16 && budget > 0 && !job.remaining.isEmpty(); n++, budget--) {
                    var at = job.remaining.removeFirst();
                    if (p.getMainHandItem() != job.axe || job.axe.isEmpty()) { job.remaining.clear(); break; }
                    if (!job.level.hasChunkAt(at) || !job.level.getBlockState(at).is(job.block)) continue;
                    BREAKING.add(p.getUUID());
                    try {
                        if (!Authority.mayChange(job.level, p, at) || !p.gameMode.destroyBlock(at)) { job.remaining.clear(); break; }
                    } finally { BREAKING.remove(p.getUUID()); }
                }
                if (job.remaining.isEmpty()) JOBS.remove(p.getUUID());
                if (budget == 0) break;
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> JOBS.values().removeIf(j -> j.level.getServer() == server));
    }
    /** Only rooted trees with a non-persistent living canopy; never load chunks or follow roots downwards. */
    public static List<BlockPos> discover(ServerLevel level, BlockPos root, Block block) {
        if (!level.getBlockState(root.below()).is(BlockTags.DIRT)) return List.of();
        var seen = new HashSet<BlockPos>(); var queue = new ArrayDeque<BlockPos>(); var logs = new ArrayList<BlockPos>();
        seen.add(root); queue.add(root); int leaves = 0;
        while (!queue.isEmpty()) {
            var from = queue.removeFirst();
            for (int dx=-1; dx<=1; dx++) for (int dy=-1; dy<=1; dy++) for (int dz=-1; dz<=1; dz++) {
                if (dx==0 && dy==0 && dz==0) continue;
                var at = from.offset(dx,dy,dz);
                if (at.getY()<root.getY() || !seen.add(at)) continue;
                if (Math.abs(at.getX()-root.getX())>RADIUS || Math.abs(at.getZ()-root.getZ())>RADIUS || at.getY()-root.getY()>HEIGHT || !level.hasChunkAt(at)) return List.of();
                BlockState s = level.getBlockState(at);
                if (s.is(block)) { logs.add(at); queue.add(at); if (logs.size()>=MAX_LOGS) return List.of(); }
                else if (s.is(BlockTags.LEAVES) && (!s.hasProperty(LeavesBlock.PERSISTENT) || !s.getValue(LeavesBlock.PERSISTENT))) leaves++;
            }
        }
        return leaves >= 4 ? logs : List.of();
    }
}
