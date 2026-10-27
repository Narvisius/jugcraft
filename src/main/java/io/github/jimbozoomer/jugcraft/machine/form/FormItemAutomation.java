package io.github.jimbozoomer.jugcraft.machine.form;

import java.util.Iterator;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.logistics.PneumaticExtractorBlock;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HopperBlock;

/** Bounded native-port activity for Auto logistics. Companion transfers use separate protected inventory views. */
public final class FormItemAutomation extends SnapshotParticipant<Integer> {
    private final FormMachineBlockEntity machine;
    private int pending, connected;
    private long lastInput = Long.MIN_VALUE, lastOutput = Long.MIN_VALUE, nextCheck;
    public FormItemAutomation(FormMachineBlockEntity machine) { this.machine = machine; }
    private void moved(boolean input, long amount, TransactionContext tx) {
        if (amount <= 0) return;
        updateSnapshots(tx); pending |= input ? 1 : 2;
    }
    protected Integer createSnapshot() { return pending; }
    protected void readSnapshot(Integer value) { pending = value; }
    protected void onFinalCommit() {
        if (machine.getLevel() != null) {
            long now = machine.getLevel().getGameTime();
            if ((pending & 1) != 0) lastInput = now;
            if ((pending & 2) != 0) lastOutput = now;
        }
        pending = 0;
    }
    public Storage<ItemVariant> observe(Storage<ItemVariant> inner, boolean input) { return new Storage<>() {
        public boolean supportsInsertion() { return inner.supportsInsertion(); }
        public boolean supportsExtraction() { return inner.supportsExtraction(); }
        public long getVersion() { return inner.getVersion(); }
        public long insert(ItemVariant item, long amount, TransactionContext tx) {
            long moved = inner.insert(item, amount, tx); moved(input, moved, tx); return moved;
        }
        public long extract(ItemVariant item, long amount, TransactionContext tx) {
            long moved = inner.extract(item, amount, tx); moved(input, moved, tx); return moved;
        }
        public Iterator<StorageView<ItemVariant>> iterator() {
            var iterator = inner.iterator();
            return new Iterator<>() {
                public boolean hasNext() { return iterator.hasNext(); }
                public StorageView<ItemVariant> next() {
                    var view = iterator.next(); return new StorageView<>() {
                        public long extract(ItemVariant item, long amount, TransactionContext tx) {
                            long moved = view.extract(item, amount, tx); moved(input, moved, tx); return moved;
                        }
                        public boolean isResourceBlank() { return view.isResourceBlank(); }
                        public ItemVariant getResource() { return view.getResource(); }
                        public long getAmount() { return view.getAmount(); }
                        public long getCapacity() { return view.getCapacity(); }
                        public StorageView<ItemVariant> getUnderlyingView() { return view.getUnderlyingView(); }
                    };
                }
            };
        }
    }; }
    public boolean external(boolean input) {
        var level = machine.getLevel(); if (level == null || level.isClientSide()) return false;
        long now = level.getGameTime();
        if (now >= nextCheck) {
            nextCheck = now + 40; connected = 0;
            var form = machine.form(); var facing = machine.getBlockState().getValue(MachineBlock.FACING);
            for (var port : form.ports()) {
                if (!port.kind().item()) continue;
                var at = form.cellPos(machine.getBlockPos(), facing, form.cellIndex(port.column(),port.row(),port.layer()));
                var side = port.side().world(facing); var adjacent = at.relative(side);
                if (!level.hasChunkAt(at) || !level.hasChunkAt(adjacent)) continue;
                var state = level.getBlockState(adjacent); boolean takes = port.kind() == FormPort.Kind.ITEM_IN;
                if (state.getBlock() instanceof HopperBlock && state.getValue(HopperBlock.ENABLED)) {
                    if (takes && state.getValue(HopperBlock.FACING) == side.getOpposite()) connected |= 1;
                    if (!takes && side == Direction.DOWN) connected |= 2;
                }
                if (!takes && state.getBlock() instanceof PneumaticExtractorBlock
                    && state.getValue(PneumaticExtractorBlock.FACING) == side.getOpposite()) {
                    boolean loaded = true;
                    for (var direction : Direction.values()) if (!level.hasChunkAt(adjacent.relative(direction))) loaded = false;
                    if (loaded && !level.hasNeighborSignal(adjacent)) connected |= 2;
                }
            }
        }
        long last = input ? lastInput : lastOutput;
        return (connected & (input ? 1 : 2)) != 0 || last != Long.MIN_VALUE && now >= last && now-last < 200;
    }
}
