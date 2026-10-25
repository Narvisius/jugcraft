package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

/** Deterministic bounded loaded-network routes: eight channels on ordinary cable, thirty-two on dense cable. */
final class DigitalChannels {
    private DigitalChannels() {}
    private static boolean passive(DigitalNode n) { return n.poweredSource() || Set.of("condenser", "charger", "inscriber", "cell_workbench", "io_port", "molecular_assembler","quartz_fiber","toggle_bus","inverted_toggle_bus").contains(n.kind()); }
    static List<DigitalNode> allocate(ServerLevel level, List<DigitalNode> nodes, Set<BlockPos> graph, DigitalNode origin, net.minecraft.server.level.ServerPlayer actor) {
        if (nodes.isEmpty()) return nodes;
        boolean powered = nodes.stream().anyMatch(n -> n.poweredSource() && n.energy.getAmount() > 0);
        var controllers = nodes.stream().filter(n -> n.kind().equals("controller")).map(DigitalNode::getBlockPos).toList();
        var roots = controllers.isEmpty() ? List.of(nodes.getFirst().getBlockPos()) : controllers;
        var usage = new HashMap<BlockPos, Integer>(); var active = new ArrayList<DigitalNode>(); int adhoc = 0;
        for (var node : nodes) {
            if (passive(node)) { active.add(node); continue; }
            if (controllers.isEmpty() && adhoc >= 8) continue;
            var queue = new ArrayDeque<BlockPos>(roots); var previous = new HashMap<BlockPos, BlockPos>(); roots.forEach(r -> previous.put(r, r));
            var destination = node.getBlockPos();
            while (!queue.isEmpty() && !previous.containsKey(destination)) {
                var at = queue.removeFirst(); var state = level.getBlockState(at);
                var neighbors = new ArrayList<BlockPos>(); for (var face : Direction.values())if(DigitalCableBlock.openFace(level,at,face)&&DigitalCableBlock.openFace(level,at.relative(face),face.getOpposite()))neighbors.add(at.relative(face)); neighbors.addAll(DigitalLinks.bridges(level, at, actor));
                for (var next : neighbors) {
                    if (!graph.contains(next) || previous.containsKey(next)) continue;
                    var nextState = level.getBlockState(next);
                    if (!DigitalCableBlock.compatible(state, nextState)) continue;
                    if (nextState.getBlock() instanceof DigitalCableBlock cable && usage.getOrDefault(next, 0) >= cable.capacity) continue;
                    if (nextState.getBlock() instanceof DigitalNodeBlock part && part.kind.equals("me_p2p_tunnel") && usage.getOrDefault(next, 0) >= 32) continue;
                    previous.put(next, at); queue.addLast(next);
                }
            }
            if (!previous.containsKey(destination)) continue;
            active.add(node); adhoc++;
            for (var at = destination;; at = previous.get(at)) {
                if (level.getBlockState(at).getBlock() instanceof DigitalCableBlock || level.getBlockState(at).getBlock() instanceof DigitalNodeBlock part && part.kind.equals("me_p2p_tunnel")) usage.merge(at, 1, Integer::sum);
                if(previous.get(at).equals(at))break;
            }
        }
        // The single-block native model reports total routed channels on the segment, rather than a fake animation.
        for (var pos : graph) {
            var state = level.getBlockState(pos); if (!state.hasProperty(DigitalCableBlock.CHANNELS)) continue;
            int used = powered ? usage.getOrDefault(pos, 0) : 0;
            if (((DigitalCableBlock) state.getBlock()).capacity == 32) used = (used + 3) / 4;
            if (state.getValue(DigitalCableBlock.CHANNELS) != used) level.setBlock(pos, state.setValue(DigitalCableBlock.CHANNELS, used), Block.UPDATE_CLIENTS);
        }
        return active.contains(origin) ? List.copyOf(active) : List.of();
    }
}
