package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Loaded, connected CPU banks with real, transactional per-job ingredient escrow. */
public final class DigitalProcessors {
    public static final List<String> BLOCKS = List.of("1k_crafting_storage", "4k_crafting_storage", "16k_crafting_storage", "64k_crafting_storage", "256k_crafting_storage", "crafting_accelerator");
    private DigitalProcessors() {}
    public static boolean cpu(String kind) { return kind.equals("crafting_unit") || kind.endsWith("_crafting_storage"); }
    public static int queueLimit(String kind) {
        if (kind.equals("crafting_unit")) return 16;
        return switch (kind) { case "1k_crafting_storage" -> 16; case "4k_crafting_storage" -> 24; case "16k_crafting_storage" -> 32; case "64k_crafting_storage" -> 48; case "256k_crafting_storage" -> 64; default -> 0; };
    }
    public static int workPerSecond(DigitalNode cpu, DigitalNetwork network) {
        var active = new HashSet<BlockPos>();
        for (var n : network.nodes()) if (cpu(n.kind()) || n.kind().equals("crafting_accelerator")) active.add(n.getBlockPos());
        var visited = new HashSet<BlockPos>(); var queue = new ArrayDeque<BlockPos>(); queue.add(cpu.getBlockPos()); visited.add(cpu.getBlockPos()); int accelerators = 0;
        while (!queue.isEmpty() && visited.size() <= 64) {
            var at = queue.removeFirst();
            if (cpu.getLevel().getBlockEntity(at) instanceof DigitalNode node && node.kind().equals("crafting_accelerator")) accelerators++;
            for (var direction : net.minecraft.core.Direction.values()) { var next = at.relative(direction); if (active.contains(next) && visited.add(next)) queue.addLast(next); }
        }
        return Math.min(8, 1 + accelerators);
    }
    public static void tick(DigitalNode cpu, DigitalNetwork network, ServerLevel level) {
        cpu.reservations.releaseFinished(network);
        if (!cpu(cpu.kind()) || cpu.orders.isEmpty()) return;
        var available = DigitalPatterns.available(network, level);
        for (int n = 0, bound = workPerSecond(cpu, network); n < bound && !cpu.orders.isEmpty(); n++) {
            var order = cpu.orders.getFirst();
            boolean installed = available.contains(order.pattern());
            if (!installed) { cpu.processingStatus = "Pattern removed"; break; }
            DigitalReservations.reserve(cpu,order.root(),order.pattern().grid(),network);
            var jobNetwork=network.forJob(order.root());
            if (!DigitalPatterns.craft(order.pattern(), jobNetwork, level, true)) {
                cpu.processingStatus = DigitalDependencies.advance(order.pattern(), jobNetwork, level,order.root());
                break;
            }
            cpu.processingStatus = "Craft completed";
            if (order.remaining() == 1) cpu.orders.removeFirst(); else cpu.orders.set(0, new DigitalPatterns.Order(order.pattern(), order.remaining() - 1,order.root()));
            cpu.setChanged();
            DigitalJobLedger.get(cpu).finished(cpu,order.root());
            cpu.reservations.releaseFinished(network);
        }
    }
}
