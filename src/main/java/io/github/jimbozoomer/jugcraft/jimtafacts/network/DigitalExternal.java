package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;

/** External storage remains in its original container. Connected duplicate buses are de-duplicated. */
public final class DigitalExternal {
    private DigitalExternal() {}
    static GlobalPos identity(DigitalNode bus) {
        var level = bus.getLevel(); var target = bus.getBlockPos().relative(bus.getBlockState().getValue(DigitalNodeBlock.FACING)); var state = level.getBlockState(target);
        if (state.getBlock() instanceof MachineBlock machine) target = machine.masterPos(target, state);
        else if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) { var other = ChestBlock.getConnectedBlockPos(target, state); if (other.asLong() < target.asLong()) target = other; }
        return GlobalPos.of(level.dimension(), target);
    }
    public record Endpoint(Storage<ItemVariant> storage, DigitalNode node) {
        public boolean accepts(ItemStack item) { return DigitalUpgrades.accepts(node, item); }
    }
    public static List<Endpoint> endpoints(DigitalNetwork network) {
        ServerPlayer player = network.actor(); if (player == null) return List.of();
        var level = player.level(); var found = new LinkedHashMap<BlockPos, Endpoint>();
        var identities = Collections.newSetFromMap(new IdentityHashMap<Storage<ItemVariant>, Boolean>());
        for (var node : network.nodes()) {
            if (!node.kind().equals("storage_bus") || !node.permitted(player) || DigitalUpgrades.paused(node)) continue;
            Direction facing = node.getBlockState().getValue(DigitalNodeBlock.FACING);
            BlockPos target = node.getBlockPos().relative(facing);
            if (!level.isLoaded(target) || !Authority.mayChange(level, player, target)
                    || !ClaimTransfers.allowed(level, node.getBlockPos(), target)
                    || level.getBlockEntity(target) instanceof DigitalNode || level.getBlockEntity(target) instanceof DigitalFacades.Covers) continue;
            var state = level.getBlockState(target); BlockPos identity = target;
            if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                var other = ChestBlock.getConnectedBlockPos(target, state);
                if (!level.isLoaded(other) || !Authority.mayChange(level, player, other) || !ClaimTransfers.allowed(level, target, other)) continue;
                if (other.asLong() < identity.asLong()) identity = other;
            } else if (state.getBlock() instanceof MachineBlock machine) identity = machine.masterPos(target, state);
            if (found.containsKey(identity) || !level.isLoaded(identity) || !Authority.mayChange(level, player, identity)) continue;
            var storage = ItemStorage.SIDED.find(level, target, facing.getOpposite());
            if (storage == null || !identities.add(storage)) continue;
            found.put(identity.immutable(), new Endpoint(storage, node));
            // Fail closed for an oversized external inventory network, rather than show partial stock as complete.
            if (found.size() > 32) return List.of();
        }
        return List.copyOf(found.values());
    }
    public static List<CellContents.Entry> contents(List<Endpoint> endpoints) {
        var entries = new ArrayList<CellContents.Entry>();
        for (var endpoint : endpoints) {
            int scanned = 0;
            for (var view : endpoint.storage()) {
                if (++scanned > 512) break;
                if (view.isResourceBlank() || view.getAmount() <= 0) continue;
                var sample = view.getResource().toStack();
                if (endpoint.accepts(sample)) entries.add(new CellContents.Entry(sample, (int) Math.min(Integer.MAX_VALUE, view.getAmount())));
            }
        }
        return entries;
    }
}
