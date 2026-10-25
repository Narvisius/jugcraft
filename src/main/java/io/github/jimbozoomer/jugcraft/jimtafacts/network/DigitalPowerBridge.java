package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;

/** Quartz fiber shares JE between its two ends, while deliberately keeping storage/channels separated. */
public final class DigitalPowerBridge {
    private DigitalPowerBridge() {}
    static void tick(DigitalNode bridge, ServerPlayer actor) {
        if (!bridge.kind().equals("quartz_fiber") || bridge.getLevel().hasNeighborSignal(bridge.getBlockPos())) return;
        var face = bridge.getBlockState().getValue(DigitalNodeBlock.FACING);
        var a = side(bridge.getBlockPos().relative(face), actor); var b = bridge.host==null?side(bridge.getBlockPos().relative(face.getOpposite()), actor):DigitalNetwork.findLocal(bridge,actor);
        if (a == null || b == null || a.truncated() || b.truncated() || a.nodes().stream().anyMatch(b.nodes()::contains)) return;
        long ac = capacity(a), bc = capacity(b); if (ac == 0 || bc == 0) return;
        double difference = amount(a) / (double) ac - amount(b) / (double) bc;
        var from = difference > 0 ? a : b; var to = difference > 0 ? b : a;
        long limit = Math.min(4096, (long) (Math.abs(difference) * Math.min(ac, bc) / 2));
        if (limit < 1) return;
        try (var tx = Transaction.openOuter()) {
            long moved = 0; for (var node : to.nodes()) if (node.poweredSource()) { moved += node.energy.insert(limit - moved, tx); if (moved == limit) break; }
            if (moved > 0 && from.pay(moved, tx)) tx.commit();
        }
    }
    private static long amount(DigitalNetwork n) { return n.nodes().stream().filter(DigitalNode::poweredSource).mapToLong(v -> v.energy.getAmount()).sum(); }
    private static long capacity(DigitalNetwork n) { return n.nodes().stream().filter(DigitalNode::poweredSource).mapToLong(v -> v.energy.getCapacity()).sum(); }
    private static DigitalNetwork side(BlockPos start, ServerPlayer actor) {
        var level = actor.level(); var seen = new HashSet<BlockPos>(); var queue = new ArrayDeque<BlockPos>(); queue.add(start);
        while (!queue.isEmpty() && seen.size() < DigitalNetwork.MAX_NODES) {
            var pos = queue.removeFirst(); if (!seen.add(pos) || !level.isLoaded(pos) || !Authority.mayChange(level, actor, pos)) continue;
            var state = level.getBlockState(pos);
            if (level.getBlockEntity(pos) instanceof DigitalNode node) return node.kind().equals("quartz_fiber") ? null : DigitalNetwork.findLocal(node, actor);
            if(level.getBlockEntity(pos) instanceof DigitalFacades.Covers covers)for(var part:covers.parts.nodes())if(part.permitted(actor))return DigitalNetwork.findLocal(part,actor);
            if (!(state.getBlock() instanceof DigitalCableBlock)) continue;
            for (var direction : Direction.values()) { var next = pos.relative(direction); if (level.isLoaded(next) && DigitalCableBlock.openFace(level,pos,direction)&&DigitalCableBlock.openFace(level,next,direction.getOpposite())&&DigitalCableBlock.compatible(state, level.getBlockState(next))) queue.addLast(next); }
        }
        return null;
    }
}
