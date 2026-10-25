package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;

/** Real tank endpoints, bounded transfers, and the same ownership/claims checks as item automation. */
public final class DigitalFluidAutomation {
    private DigitalFluidAutomation() {}
    public record Endpoint(Storage<FluidVariant> storage, DigitalNode node) {
        public boolean accepts(FluidVariant fluid) { return DigitalUpgrades.acceptsFluid(node, fluid); }
    }
    public static FluidVariant filter(ItemStack sample) {
        return sample.getItem() instanceof BucketItem bucket && bucket.getContent() != Fluids.EMPTY ? FluidVariant.of(bucket.getContent()) : FluidVariant.blank();
    }
    private static Storage<FluidVariant> adjacent(DigitalNode node, ServerPlayer player) {
        var level = player.level(); var face = node.getBlockState().getValue(DigitalNodeBlock.FACING); var target = node.getBlockPos().relative(face);
        if (!node.permitted(player) || !level.isLoaded(target) || level.getBlockEntity(target) instanceof DigitalNode || level.getBlockEntity(target) instanceof DigitalFacades.Covers
                || !Authority.mayChange(level, player, target) || !ClaimTransfers.allowed(level, node.getBlockPos(), target)) return null;
        return FluidStorage.SIDED.find(level, target, face.getOpposite());
    }
    public static List<Endpoint> endpoints(DigitalNetwork network) {
        var player = network.actor(); if (player == null) return List.of();
        var found = new LinkedHashMap<BlockPos, Endpoint>(); var identities = Collections.newSetFromMap(new IdentityHashMap<Storage<FluidVariant>, Boolean>());
        for (var node : network.nodes()) {
            if (!node.kind().equals("fluid_storage_bus") || DigitalUpgrades.paused(node)) continue;
            var target = node.getBlockPos().relative(node.getBlockState().getValue(DigitalNodeBlock.FACING));
            if (!player.level().isLoaded(target)) continue;
            var state = player.level().getBlockState(target);
            var identity = state.getBlock() instanceof MachineBlock machine ? machine.masterPos(target, state) : target;
            if (found.containsKey(identity) || !player.level().isLoaded(identity) || !Authority.mayChange(player.level(), player, identity)) continue;
            var storage = adjacent(node, player);
            if (storage == null || storage instanceof FluidCells.Port || !identities.add(storage)) continue;
            found.put(identity.immutable(), new Endpoint(storage, node));
            if (found.size() > 32) return List.of();
        }
        return List.copyOf(found.values());
    }
    public static Map<FluidVariant, Long> amounts(DigitalNetwork network) {
        var result = new LinkedHashMap<FluidVariant, Long>();
        for (var endpoint : endpoints(network)) {
            int scanned = 0;
            for (var view : endpoint.storage()) {
                if (++scanned > 64) break;
                if (view.isResourceBlank() || view.getAmount() <= 0 || !endpoint.accepts(view.getResource())) continue;
                result.merge(view.getResource(), view.getAmount(), (a, b) -> Long.MAX_VALUE - a < b ? Long.MAX_VALUE : a + b);
            }
        }
        return result;
    }
    public static void tick(DigitalNode node, ServerPlayer player) {
        boolean importing = node.kind().equals("fluid_import_bus");
        if (!importing && !node.kind().equals("fluid_export_bus")) return;
        var storage = adjacent(node, player); if (storage == null || storage instanceof FluidCells.Port) return;
        var port = new FluidCells.Port(node, player);
        long budget = FluidConstants.BUCKET * DigitalUpgrades.batch(node) / 16;
        if (importing) {
            int scanned = 0;
            for (var view : storage) {
                if (++scanned > 64) break;
                if (view.isResourceBlank() || view.getAmount() <= 0 || !DigitalUpgrades.acceptsFluid(node, view.getResource())) continue;
                try (var tx = Transaction.openOuter()) {
                    long n = port.insert(view.getResource(), Math.min(budget, view.getAmount()), tx);
                    if (n > 0 && view.extract(view.getResource(), n, tx) == n) { tx.commit(); return; }
                }
            }
        } else {
            var samples = DigitalUpgrades.samples(node).stream().map(DigitalFluidAutomation::filter).filter(f -> !f.isBlank()).distinct().toList();
            if (DigitalUpgrades.has(node, "inverter_card")) samples = portAmounts(port).stream().filter(f -> DigitalUpgrades.acceptsFluid(node, f)).limit(9).toList();
            long each = DigitalUpgrades.has(node, "equal_distribution_card") ? Math.max(1, budget / Math.max(1, samples.size())) : budget;
            for (var filter : samples) {
                if (budget <= 0) break;
                try (var tx = Transaction.openOuter()) {
                    long n = port.extract(filter, Math.min(each, budget), tx);
                    if (n > 0 && storage.insert(filter, n, tx) == n) { tx.commit(); budget -= n; }
                }
            }
        }
    }
    private static List<FluidVariant> portAmounts(Storage<FluidVariant> port) {
        var result = new ArrayList<FluidVariant>(); int scanned = 0;
        for (var view : port) { if (++scanned > 512) break; if (!view.isResourceBlank() && view.getAmount() > 0 && !result.contains(view.getResource())) result.add(view.getResource()); }
        return result;
    }
}
