package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import com.mojang.serialization.Codec;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.Registry;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.*;

/** Supplied matter-ball/singularity ratios; saved fractional bucket credits prevent rounding duplication. */
public final class DigitalCondenser {
    public static DataComponentType<Long> MATTER;
    private DigitalCondenser() {}
    public static void register() {
        MATTER = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("jimta_condensed_matter"), DataComponentType.<Long>builder().persistent(io.github.jimbozoomer.jugcraft.jimtafacts.JimtaCodecs.longRange(0, 2_097_152L * FluidConstants.BUCKET)).networkSynchronized(ByteBufCodecs.VAR_LONG).build());
    }
    public static int capacity(DigitalNode node) {
        for (int size : new int[]{1, 4, 16, 64, 256}) if (node.getItem(0).is(DigitalStorage.ITEMS.get("cell_component_" + size + "k"))) return size * 8192;
        return 0;
    }
    public static boolean input(ItemStack item) {
        return !item.isEmpty() && item.getItem().canFitInsideContainerItems() && !item.has(DataComponents.CONTAINER) && !item.has(DataComponents.BUNDLE_CONTENTS) && !(item.getItem() instanceof FluidCells.Cell);
    }
    public static void tick(DigitalNode node) {
        var actor = node.actor(); if (actor == null || !node.permitted(actor)) return;
        long cap = capacity(node) * FluidConstants.BUCKET;
        int cost = node.emitBelow ? 256_000 : 256;
        var input = node.getItem(1);
        // Never void excess input silently. A full machine simply stops accepting material.
        int take = input(input) ? (int) Math.min(input.getCount(), Math.max(0, cap - node.condensed) / FluidConstants.BUCKET) : 0;
        if (take > 0) { input.shrink(take); node.condensed += take * FluidConstants.BUCKET; node.setChanged(); }
        if (node.condensed < cost * FluidConstants.BUCKET) return;
        var item = DigitalStorage.ITEMS.get(node.emitBelow ? "singularity" : "matter_ball"); var output = node.getItem(2);
        if (!output.isEmpty() && (!output.is(item) || output.getCount() >= output.getMaxStackSize())) return;
        if (output.isEmpty()) node.setItem(2, new ItemStack(item)); else output.grow(1);
        node.condensed -= cost * FluidConstants.BUCKET; node.setChanged();
    }
    static final class Port extends SnapshotParticipant<Long> implements Storage<FluidVariant> {
        private final DigitalNode node;
        Port(DigitalNode node) { this.node = node; }
        protected Long createSnapshot() { return node.condensed; }
        protected void readSnapshot(Long amount) { node.condensed = amount; }
        protected void onFinalCommit() { node.setChanged(); }
        public long insert(FluidVariant fluid, long maximum, TransactionContext tx) {
            var actor = node.actor();
            if (fluid.isBlank() || maximum <= 0 || actor == null || !node.permitted(actor) || actor.level().hasNeighborSignal(node.getBlockPos())) return 0;
            long amount = Math.min(maximum, Math.max(0, capacity(node) * FluidConstants.BUCKET - node.condensed));
            if (amount > 0) { updateSnapshots(tx); node.condensed += amount; }
            return amount;
        }
        public long extract(FluidVariant fluid, long maximum, TransactionContext tx) { return 0; }
        public boolean supportsExtraction() { return false; }
        public Iterator<StorageView<FluidVariant>> iterator() { return Collections.emptyIterator(); }
    }
    /** Only material input/result output are automated; the capacity component is manually removable. */
    static final class ItemPort implements Storage<net.fabricmc.fabric.api.transfer.v1.item.ItemVariant> {
        private final DigitalNode node;
        ItemPort(DigitalNode node) { this.node = node; }
        private boolean allowed() { var p = node.actor(); return p != null && node.permitted(p) && !node.getLevel().hasNeighborSignal(node.getBlockPos()); }
        public long insert(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant item, long max, TransactionContext tx) {
            if (!allowed() || max <= 0 || item.isBlank() || !input(item.toStack())) return 0;
            var old = node.getItem(1); if (!old.isEmpty() && !item.matches(old)) return 0;
            int count = (int) Math.min(max, item.toStack().getMaxStackSize() - old.getCount()); if (count <= 0) return 0;
            node.snapshotCells(tx); node.setItem(1, item.toStack(old.getCount() + count)); return count;
        }
        public long extract(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant item, long max, TransactionContext tx) {
            if (!allowed() || max <= 0 || item.isBlank() || !item.matches(node.getItem(2))) return 0;
            int count = (int) Math.min(max, node.getItem(2).getCount()); node.snapshotCells(tx); node.getItem(2).shrink(count); return count;
        }
        public Iterator<StorageView<net.fabricmc.fabric.api.transfer.v1.item.ItemVariant>> iterator() {
            if (!allowed() || node.getItem(2).isEmpty()) return Collections.emptyIterator();
            var sample = net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(node.getItem(2));
            return java.util.List.<StorageView<net.fabricmc.fabric.api.transfer.v1.item.ItemVariant>>of(new StorageView<>() {
                public boolean isResourceBlank() { return getResource().isBlank(); }
                public net.fabricmc.fabric.api.transfer.v1.item.ItemVariant getResource() { return allowed() && sample.matches(node.getItem(2)) ? sample : net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.blank(); }
                public long getAmount() { return isResourceBlank() ? 0 : node.getItem(2).getCount(); }
                public long getCapacity() { return 64; }
                public long extract(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant item, long max, TransactionContext tx) { return sample.equals(item) ? ItemPort.this.extract(item, max, tx) : 0; }
            }).iterator();
        }
    }
}
