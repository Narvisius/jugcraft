package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tools.*;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** Handheld cell contents stay in the held item. Changing hands/items immediately invalidates the menu. */
public final class PortableCells {
    private PortableCells() {}
    public static void register() {
        for (int size : new int[]{1, 4, 16, 64, 256}) for (boolean fluid : new boolean[]{false, true}) {
            String name = "portable_" + (fluid ? "fluid" : "item") + "_cell_" + size + "k";
            var key = ResourceKey.create(Registries.ITEM, Jugcraft.id("jimta_" + name)); var p = new Item.Properties().setId(key).stacksTo(1);
            DigitalStorage.ITEMS.put(name, Registry.register(BuiltInRegistries.ITEM, key, fluid ? new Fluid(p, size) : new ItemsCell(p, size)));
        }
    }
    private static InteractionResult open(Player p, InteractionHand hand) {
        if (p instanceof ServerPlayer sp) {
            var held = p.getItemInHand(hand);
            sp.openMenu(new SimpleMenuProvider((id, inventory, player) -> DigitalTerminal.Menu.portable(id, inventory, held), held.getHoverName()));
        }
        return InteractionResult.SUCCESS;
    }
    public static final class ItemsCell extends DigitalStorage.CellItem implements Chargeable {
        public ItemsCell(Properties p, int size) { super(p, size * 1024 * 8); }
        public long baseCapacity() { return 32_000; }
        @Override public InteractionResult use(Level level, Player player, InteractionHand hand) { return open(player, hand); }
    }
    public static final class Fluid extends FluidCells.Cell implements Chargeable {
        public Fluid(Properties p, int size) { super(p, size); }
        public long baseCapacity() { return 32_000; }
        @Override public InteractionResult use(Level level, Player player, InteractionHand hand) { return open(player, hand); }
    }
    private record Snapshot(CellContents items, FluidCells.Contents fluids, Long energy) {}
    public static final class Held extends SnapshotParticipant<Snapshot> implements Storage<FluidVariant> {
        private final Player owner;
        private final ItemStack stack;
        public Held(Player owner, ItemStack stack) { this.owner = owner; this.stack = stack; }
        public boolean valid() { return owner.isAlive() && !owner.isSpectator() && !stack.isEmpty() && (owner.getMainHandItem() == stack || owner.getOffhandItem() == stack); }
        public boolean online() { return valid() && Chargeable.energy(stack) > 0; }
        public boolean fluid() { return stack.getItem() instanceof Fluid; }
        protected Snapshot createSnapshot() { return new Snapshot(stack.get(DigitalStorage.CONTENTS), stack.get(FluidCells.CONTENTS), stack.get(JugcraftTools.ENERGY)); }
        private <T> void restore(DataComponentType<T> type, T old) { if (old == null) stack.remove(type); else stack.set(type, old); }
        protected void readSnapshot(Snapshot snapshot) { restore(DigitalStorage.CONTENTS, snapshot.items); restore(FluidCells.CONTENTS, snapshot.fluids); restore(JugcraftTools.ENERGY, snapshot.energy); }
        protected void onFinalCommit() { owner.getInventory().setChanged(); }
        public List<CellContents.Entry> contents() { return online() && !fluid() ? stack.getOrDefault(DigitalStorage.CONTENTS, CellContents.EMPTY).entries() : List.of(); }
        public long item(ItemStack item, long maximum, TransactionContext tx, boolean insert) {
            if (!online() || !(stack.getItem() instanceof ItemsCell type) || maximum <= 0 || item.isEmpty()) return 0;
            long budget = Math.min(Math.min(maximum, 4096), Chargeable.energy(stack));
            var old = stack.getOrDefault(DigitalStorage.CONTENTS, CellContents.EMPTY);
            if (insert && DigitalWorkshops.filtered(stack, item)) return 0;
            int n = insert ? old.accepted(item.copyWithCount((int) budget), type.capacity) : (int) Math.min(budget, old.count(item));
            if (n <= 0) return 0;
            updateSnapshots(tx); stack.set(DigitalStorage.CONTENTS, insert ? old.add(item, n) : old.remove(item, n)); Chargeable.drain(stack, n); return n;
        }
        public long insert(FluidVariant fluid, long maximum, TransactionContext tx) { return fluid(fluid, maximum, tx, true); }
        public long extract(FluidVariant fluid, long maximum, TransactionContext tx) { return fluid(fluid, maximum, tx, false); }
        private long fluid(FluidVariant fluid, long maximum, TransactionContext tx, boolean insert) {
            if (!online() || !(stack.getItem() instanceof Fluid type) || fluid.isBlank() || maximum <= 0) return 0;
            if(insert&&FluidCells.filtered(stack,fluid))return 0;
            long quantum = FluidConstants.BUCKET / 10;
            long budget = Math.min(Math.min(maximum, FluidConstants.BUCKET), Math.min(10, Chargeable.energy(stack)) * quantum);
            var old = stack.getOrDefault(FluidCells.CONTENTS, FluidCells.Contents.EMPTY);
            long n = insert ? old.entries().size() >= 63 && old.amount(fluid) == 0 ? 0 : Math.min(budget, Math.max(0, type.capacity - old.used())) : Math.min(budget, old.amount(fluid));
            if (n <= 0) return 0;
            updateSnapshots(tx); stack.set(FluidCells.CONTENTS, old.change(fluid, insert ? n : -n)); Chargeable.drain(stack, (n + quantum - 1) / quantum); return n;
        }
        public Map<FluidVariant, Long> amounts() {
            var result = new LinkedHashMap<FluidVariant, Long>();
            if (online() && fluid()) for (var entry : stack.getOrDefault(FluidCells.CONTENTS, FluidCells.Contents.EMPTY).entries()) result.merge(entry.fluid(), entry.amount(), Long::sum);
            return result;
        }
        public Iterator<StorageView<FluidVariant>> iterator() {
            var result = new ArrayList<StorageView<FluidVariant>>();
            for (var fluid : amounts().keySet()) result.add(new StorageView<>() {
                public boolean isResourceBlank() { return false; }
                public FluidVariant getResource() { return fluid; }
                public long getAmount() { return amounts().getOrDefault(fluid, 0L); }
                public long getCapacity() { return stack.getItem() instanceof Fluid cell ? cell.capacity : 0; }
                public long extract(FluidVariant f, long amount, TransactionContext tx) { return f.equals(fluid) ? Held.this.extract(f, amount, tx) : 0; }
            });
            return result.iterator();
        }
    }
}
