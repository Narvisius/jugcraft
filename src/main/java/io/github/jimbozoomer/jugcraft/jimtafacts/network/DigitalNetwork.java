package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Re-resolved for every action. No chunk loading, cross-claim traversal or persistent phantom inventory. */
public record DigitalNetwork(List<DigitalNode> nodes, boolean truncated, ServerPlayer actor, List<DigitalNetwork> remote, UUID job) {
    public DigitalNetwork(List<DigitalNode> nodes,boolean truncated,ServerPlayer actor,List<DigitalNetwork> remote){this(nodes,truncated,actor,remote,null);}
    public DigitalNetwork(List<DigitalNode> nodes, boolean truncated, ServerPlayer actor) { this(nodes, truncated, actor, List.of()); }
    public DigitalNetwork(List<DigitalNode> nodes, boolean truncated) { this(nodes, truncated, null); }
    public static final int MAX_NODES = 256;
    public DigitalNetwork forJob(UUID root){return new DigitalNetwork(nodes,truncated,actor,remote,root);}
    public static DigitalNetwork find(DigitalNode origin, ServerPlayer player) {
        var local = findLocal(origin, player);
        return new DigitalNetwork(local.nodes(), local.truncated(), player, DigitalQuantum.networks(local));
    }
    static DigitalNetwork findLocal(DigitalNode origin, ServerPlayer player) {
        var world = player.level(); var found = new ArrayList<DigitalNode>();
        var visited = new HashSet<BlockPos>(); var graph = new HashSet<BlockPos>(); var queue = new ArrayDeque<BlockPos>();
        queue.add(origin.getBlockPos()); visited.add(origin.getBlockPos()); int traversed = 0;
        while (!queue.isEmpty()) {
            if (++traversed > MAX_NODES) return new DigitalNetwork(List.of(), true);
            var pos = queue.removeFirst();
            if (!world.isLoaded(pos) || !Authority.mayChange(world, player, pos)) continue;
            var state = world.getBlockState(pos);
            if (state.getBlock() instanceof DigitalNodeBlock) {
                if (!(world.getBlockEntity(pos) instanceof DigitalNode node) || !node.permitted(player)) continue;
                if (node.kind().equals("quartz_fiber")) continue;
                if (node.kind().equals("toggle_bus") && !world.hasNeighborSignal(pos) || node.kind().equals("inverted_toggle_bus") && world.hasNeighborSignal(pos)) continue;
                found.add(node);
            } else if (!(state.getBlock() instanceof DigitalCableBlock)) continue;
            else if(world.getBlockEntity(pos) instanceof DigitalFacades.Covers covers){
                for(var node:covers.parts.nodes())if(node.permitted(player))found.add(node);
                if(found.size()>MAX_NODES)return new DigitalNetwork(List.of(),true);
            }
            graph.add(pos);
            if(found.size()>MAX_NODES)return new DigitalNetwork(List.of(),true);
            for (var bridge : DigitalLinks.bridges(world, pos, player)) if (visited.add(bridge)) queue.addLast(bridge);
            for (Direction d : Direction.values()) {
                var next = pos.relative(d);
                if (!world.isLoaded(next) || visited.contains(next)) continue;
                if(!DigitalCableBlock.openFace(world,pos,d)||!DigitalCableBlock.openFace(world,next,d.getOpposite()))continue;
                var neighbor = world.getBlockState(next); var block = neighbor.getBlock();
                if ((block instanceof DigitalCableBlock || block instanceof DigitalNodeBlock) && DigitalCableBlock.compatible(state, neighbor) && visited.add(next)) queue.addLast(next);
            }
        }
        found.sort(Comparator.comparingLong(n -> n.getBlockPos().asLong()));
        return new DigitalNetwork(DigitalChannels.allocate(world, found, graph, origin, player), false, player);
    }
    public boolean online() { return !truncated && nodes.stream().anyMatch(n -> n.poweredSource() && n.energy.getAmount() > 0); }
    public List<DigitalNetwork> segments() { var all = new ArrayList<DigitalNetwork>(); all.add(this); all.addAll(remote); return all; }
    public List<CellContents.Entry> contents() {
        if (!online()) return List.of();
        var result = new ArrayList<CellContents.Entry>();
        if(job!=null)nodes.forEach(n->result.addAll(n.reservations.contents(job)));
        var seen = new HashSet<GlobalPos>();
        for (var segment : segments()) {
            segment.nodes().forEach(n -> result.addAll(n.contents()));
            result.addAll(DigitalExternal.contents(DigitalExternal.endpoints(segment).stream().filter(e -> seen.add(DigitalExternal.identity(e.node()))).toList()));
        }
        return List.copyOf(result);
    }
    boolean pay(long amount, TransactionContext tx) {
        for (var n : nodes) if (n.poweredSource()) { amount -= n.energy.extract(amount, tx); if (amount == 0) return true; }
        return false;
    }
    public long insert(ItemStack stack, long maximum, TransactionContext tx) { return transfer(stack, maximum, tx, true); }
    public long extract(ItemStack stack, long maximum, TransactionContext tx) { return transfer(stack, maximum, tx, false); }
    private long transfer(ItemStack stack, long maximum, TransactionContext parent, boolean insert) {
        if (!online() || maximum <= 0) return 0;
        long budget = Math.min(Math.min(maximum, 4096), nodes.stream().filter(DigitalNode::poweredSource).mapToLong(n -> Math.min(4096, n.energy.getAmount())).sum());
        try (var tx = parent.openNested()) {
            long left = budget;
            if(!insert&&job!=null)for(var node:nodes){left-=node.reservations.extract(job,stack,left,tx);if(left==0)break;}
            for (var node : nodes) { left -= insert ? node.insert(stack, left, tx) : node.extract(stack, left, tx); if (left == 0) break; }
            if (left > 0) for (var endpoint : DigitalExternal.endpoints(this)) {
                if (!endpoint.accepts(stack)) continue;
                var resource = net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(stack);
                left -= insert ? endpoint.storage().insert(resource, left, tx) : endpoint.storage().extract(resource, left, tx);
                if (left == 0) break;
            }
            if (left > 0) for (var linked : remote) { left -= linked.transfer(stack, left, tx, insert); if (left == 0) break; }
            long moved = budget - left;
            if (moved > 0 && pay(moved, tx)) { tx.commit(); return moved; }
            return 0;
        }
    }
}
