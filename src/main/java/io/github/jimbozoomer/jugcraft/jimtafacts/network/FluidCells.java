package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.*;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;

/** Removable fluid cells use Fabric fluid units; no fluid is represented by fake inventory items. */
public final class FluidCells {
    public static DataComponentType<Contents> CONTENTS;
    private FluidCells() {}
    public record Entry(FluidVariant fluid, long amount) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(FluidVariant.CODEC.fieldOf("fluid").forGetter(Entry::fluid), io.github.jimbozoomer.jugcraft.jimtafacts.JimtaCodecs.longRange(1, 171_798_691_840L).fieldOf("amount").forGetter(Entry::amount)).apply(i, Entry::new));
    }
    public record Contents(List<Entry> entries) {
        public static final Contents EMPTY = new Contents(List.of());
        public static final Codec<Contents> CODEC = Entry.CODEC.listOf(0, 63).xmap(Contents::new, Contents::entries);
        public Contents { entries = List.copyOf(entries); }
        public long used() { return entries.stream().mapToLong(Entry::amount).sum(); }
        public long amount(FluidVariant fluid) { return entries.stream().filter(e -> e.fluid.equals(fluid)).mapToLong(Entry::amount).sum(); }
        public Contents change(FluidVariant fluid, long delta) {
            var result = new ArrayList<Entry>(); long amount = delta;
            for (var e : entries) { if (e.fluid.equals(fluid)) amount += e.amount; else result.add(e); }
            if (amount > 0) result.add(new Entry(fluid, amount));
            return new Contents(result);
        }
    }
    public static class Cell extends Item {
        public final long capacity;
        public Cell(Properties p, int size) { super(p); capacity = size * 1024L * FluidConstants.BUCKET; }
        @Override public boolean canFitInsideContainerItems() { return false; }
        @Override public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<net.minecraft.network.chat.Component> out, TooltipFlag flag) {
            var contents = stack.getOrDefault(CONTENTS, Contents.EMPTY);
            out.accept(net.minecraft.network.chat.Component.literal(contents.used() / (FluidConstants.BUCKET / 1000) + " mB · " + contents.entries().size() + " / 63 fluids"));
            out.accept(net.minecraft.network.chat.Component.literal("Capacity: " + capacity / FluidConstants.BUCKET + " buckets. Use an ME Interface for pipes."));
        }
    }
    public static void register() {
        CONTENTS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("jimta_fluid_cell_contents"), DataComponentType.<Contents>builder()
                .persistent(Contents.CODEC).networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(Contents.CODEC)).build());
    }
    public static boolean filtered(ItemStack cell,FluidVariant fluid){
        var filters=cell.getOrDefault(DigitalWorkshops.FILTERS,List.<ItemStack>of());
        return !filters.isEmpty()&&filters.stream().noneMatch(sample->DigitalFluidAutomation.filter(sample).equals(fluid));
    }
    private static long transfer(DigitalNode node, FluidVariant fluid, long maximum, TransactionContext tx, boolean insert) {
        long left = maximum;
        for (int i = 0; i < node.cellSlots() && left > 0; i++) {
            var stack = node.getItem(i);
            if(DigitalCreativeCell.creative(stack)){
                if(!insert)left-=Math.min(left,stack.getOrDefault(CONTENTS,Contents.EMPTY).amount(fluid));
                continue;
            }
            if (!(stack.getItem() instanceof Cell type)) continue;
            if(insert&&filtered(stack,fluid))continue;
            var old = stack.getOrDefault(CONTENTS, Contents.EMPTY);
            long n = insert ? (old.entries.size() >= 63 && old.amount(fluid) == 0 ? 0 : Math.min(left, Math.max(0, type.capacity - old.used()))) : Math.min(left, old.amount(fluid));
            if (n <= 0) continue;
            node.snapshotCells(tx); stack.set(CONTENTS, old.change(fluid, insert ? n : -n)); left -= n;
        }
        return maximum - left;
    }
    public static final class Port implements Storage<FluidVariant> {
        private final DigitalNode node;
        private final ServerPlayer viewer;
        private final DigitalNetwork resolved;
        public Port(DigitalNode node, ServerPlayer viewer) { this.node = node; this.viewer = viewer; this.resolved = null; }
        private Port(DigitalNetwork network) { this.node = network.nodes().getFirst(); this.viewer = network.actor(); this.resolved = network; }
        private DigitalNetwork network() {
            var actor = viewer != null ? viewer : node.actor();
            if (resolved != null) return actor != null && resolved.nodes().stream().allMatch(n -> n.permitted(actor)) ? resolved : new DigitalNetwork(List.of(), false);
            return actor != null && node.permitted(actor) ? DigitalNetwork.find(node, actor) : new DigitalNetwork(List.of(), false);
        }
        public long insert(FluidVariant fluid, long maximum, TransactionContext tx) { return move(fluid, maximum, tx, true); }
        public long extract(FluidVariant fluid, long maximum, TransactionContext tx) { return move(fluid, maximum, tx, false); }
        private long move(FluidVariant fluid, long maximum, TransactionContext parent, boolean insert) {
            if (fluid.isBlank() || maximum <= 0) return 0;
            var network = network(); if (!network.online()) return 0;
            long budget = Math.min(maximum, 4 * FluidConstants.BUCKET);
            try (Transaction tx = parent.openNested()) {
                long left = budget;
                for (var n : network.nodes()) { left -= transfer(n, fluid, left, tx, insert); if (left == 0) break; }
                if (left > 0) for (var endpoint : DigitalFluidAutomation.endpoints(network)) {
                    if (!endpoint.accepts(fluid)) continue;
                    left -= insert ? endpoint.storage().insert(fluid, left, tx) : endpoint.storage().extract(fluid, left, tx);
                    if (left == 0) break;
                }
                if (left > 0) for (var remote : network.remote()) { left -= new Port(remote).move(fluid, left, tx, insert); if (left == 0) break; }
                long moved = budget - left, energy = (moved + FluidConstants.BUCKET / 10 - 1) / (FluidConstants.BUCKET / 10);
                for (var n : network.nodes()) if (n.poweredSource()) { energy -= n.energy.extract(energy, tx); if (energy == 0) break; }
                if (moved > 0 && energy == 0) { tx.commit(); return moved; }
                return 0;
            }
        }
        public Map<FluidVariant, Long> amounts() {
            var network = network(); var result = new LinkedHashMap<FluidVariant, Long>();
            if (!network.online()) return result;
            var seen = new java.util.HashSet<net.minecraft.core.GlobalPos>();
            for (var segment : network.segments()) {
                for (var n : segment.nodes()) for (int i = 0; i < n.cellSlots(); i++) if (n.getItem(i).getItem() instanceof Cell||DigitalCreativeCell.creative(n.getItem(i)))
                    for (var e : n.getItem(i).getOrDefault(CONTENTS, Contents.EMPTY).entries) result.merge(e.fluid, e.amount, (a, b) -> Long.MAX_VALUE - a < b ? Long.MAX_VALUE : a + b);
                for (var endpoint : DigitalFluidAutomation.endpoints(segment)) {
                    if (!seen.add(DigitalExternal.identity(endpoint.node()))) continue;
                    int scanned = 0;
                    for (var view : endpoint.storage()) {
                        if (++scanned > 64) break;
                        if (view.isResourceBlank() || view.getAmount() <= 0 || !endpoint.accepts(view.getResource())) continue;
                        result.merge(view.getResource(), view.getAmount(), (a, b) -> Long.MAX_VALUE - a < b ? Long.MAX_VALUE : a + b);
                    }
                }
            }
            return result;
        }
        public Iterator<StorageView<FluidVariant>> iterator() {
            var views = new ArrayList<StorageView<FluidVariant>>();
            var totals = amounts();
            for (var fluid : totals.keySet()) views.add(new StorageView<>() {
                public long extract(FluidVariant resource, long amount, TransactionContext tx) { return fluid.equals(resource) ? Port.this.extract(resource, amount, tx) : 0; }
                public boolean isResourceBlank() { return false; }
                public FluidVariant getResource() { return fluid; }
                public long getAmount() {
                    return Port.this.amounts().getOrDefault(fluid, 0L);
                }
                public long getCapacity() { return getAmount(); }
            });
            return views.iterator();
        }
    }
}
