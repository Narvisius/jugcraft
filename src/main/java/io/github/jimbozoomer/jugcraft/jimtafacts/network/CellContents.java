package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** Immutable cell contents. Counts are separate from item stacks, so large cells never create oversized stacks. */
public record CellContents(List<Entry> entries) {
    public static final int MAX_TYPES = 63;
    public static final CellContents EMPTY = new CellContents(List.of());
    public static final Codec<CellContents> CODEC = Entry.CODEC.listOf(0, MAX_TYPES)
            .xmap(CellContents::new, CellContents::entries);

    public CellContents { entries = List.copyOf(entries); }

    public record Entry(ItemStack sample, int count) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemStack.CODEC.fieldOf("item").forGetter(Entry::sample),
                Codec.intRange(1, 2_097_152).fieldOf("count").forGetter(Entry::count)
        ).apply(i, Entry::new));
        public Entry { sample = sample.copyWithCount(1); }
        @Override public ItemStack sample() { return sample.copy(); }
    }

    public int amount() { return entries.stream().mapToInt(Entry::count).sum(); }
    public int count(ItemStack stack) {
        return entries.stream().filter(e -> ItemStack.isSameItemSameComponents(e.sample, stack))
                .mapToInt(Entry::count).sum();
    }
    public int accepted(ItemStack stack, int capacity) {
        if (stack.isEmpty() || stack.getItem() instanceof DigitalStorage.CellItem || stack.getItem() instanceof FluidCells.Cell) return 0;
        if (count(stack) == 0 && entries.size() >= MAX_TYPES) return 0;
        return Math.max(0, Math.min(stack.getCount(), capacity - amount()));
    }
    public CellContents add(ItemStack stack, int count) {
        if (count <= 0) return this;
        var next = new ArrayList<Entry>(entries);
        for (int i = 0; i < next.size(); i++) {
            var old = next.get(i);
            if (ItemStack.isSameItemSameComponents(old.sample, stack)) {
                next.set(i, new Entry(old.sample, Math.addExact(old.count, count)));
                return new CellContents(next);
            }
        }
        if (next.size() >= MAX_TYPES) throw new IllegalStateException("Cell type limit exceeded");
        next.add(new Entry(stack, count));
        return new CellContents(next);
    }
    public CellContents remove(ItemStack stack, int count) {
        var next = new ArrayList<Entry>();
        for (var old : entries) {
            int n = old.count;
            if (ItemStack.isSameItemSameComponents(old.sample, stack)) {
                int removed = Math.min(n, count); n -= removed; count -= removed;
            }
            if (n > 0) next.add(new Entry(old.sample, n));
        }
        return new CellContents(next);
    }
}
