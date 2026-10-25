package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Incremental dependency planning. Only real stored results unlock the next step. */
public final class DigitalDependencies {
    private DigitalDependencies() {}
    private record Step(DigitalPatterns.Pattern craft, DigitalProcessing.Binding machine, String status) {
        static Step waiting(String text) { return new Step(null, null, text); }
        boolean work() { return craft != null || machine != null; }
    }
    private static final class Stock {
        final List<ItemStack> samples = new ArrayList<>();
        final List<Long> amounts = new ArrayList<>();
        Stock(List<CellContents.Entry> entries) {
            for (var entry : entries) {
                int i = index(entry.sample());
                if (i < 0) { samples.add(entry.sample()); amounts.add((long) entry.count()); }
                else amounts.set(i, Math.min(Integer.MAX_VALUE, amounts.get(i) + entry.count()));
            }
        }
        Stock(Stock other) { samples.addAll(other.samples); amounts.addAll(other.amounts); }
        int index(ItemStack sample) {
            for (int i = 0; i < samples.size(); i++) if (ItemStack.isSameItemSameComponents(samples.get(i), sample)) return i;
            return -1;
        }
        long take(ItemStack sample, long amount) {
            int i = index(sample); if (i < 0) return 0;
            long used = Math.min(amount, amounts.get(i)); amounts.set(i, amounts.get(i) - used); return used;
        }
    }
    /** Called after an ordinary atomic crafting attempt fails; at most one real dependency step per pass. */
    public static String advance(DigitalPatterns.Pattern requested, DigitalNetwork network, ServerLevel level) {
        return advance(requested,network,level,UUID.randomUUID());
    }
    public static String advance(DigitalProcessing.Pattern requested, DigitalNetwork network, ServerLevel level) {
        return advance(requested,network,level,UUID.randomUUID());
    }
    public static String advance(DigitalPatterns.Pattern requested,DigitalNetwork network,ServerLevel level,UUID root){return advance(requested,requested.grid(),network,level,root);}
    public static String advance(DigitalProcessing.Pattern requested,DigitalNetwork network,ServerLevel level,UUID root){return advance(requested,requested.inputs(),network,level,root);}
    private static String advance(Object requested, List<ItemStack> ingredients, DigitalNetwork network, ServerLevel level,UUID root) {
        if (!network.online()) return "Waiting for power";
        var crafts = DigitalPatterns.available(network, level);
        var machines = DigitalProcessing.available(network);
        var path = new HashSet<Object>(); path.add(requested);
        Step step = inputs(ingredients, new Stock(network.contents()), network, level, crafts, machines, path, new int[]{128});
        if (step == null) return "Waiting for power or result room";
        if (step.craft != null) return DigitalPatterns.craft(step.craft, network, level)
                ? "Intermediate crafted; continuing next pass" : "Intermediate needs power or result room";
        if (step.machine != null) return DigitalProcessing.requestDependency(step.machine, root)
                ? "Dependency machine batch queued" : "Dependency machine queue is full";
        return step.status;
    }
    private static Step inputs(List<ItemStack> ingredients, Stock stock, DigitalNetwork network, ServerLevel level,
                               List<DigitalPatterns.Pattern> crafts, List<DigitalProcessing.Binding> machines,
                               Set<Object> path, int[] budget) {
        for (var ingredient : ingredients) {
            if (ingredient.isEmpty()) continue;
            if (stock.take(ingredient, ingredient.getCount()) == ingredient.getCount()) continue;
            return missing(ingredient, stock, network, level, crafts, machines, path, budget);
        }
        return null;
    }
    private static Step missing(ItemStack item, Stock stock, DigitalNetwork network, ServerLevel level,
                                List<DigitalPatterns.Pattern> crafts, List<DigitalProcessing.Binding> machines,
                                Set<Object> path, int[] budget) {
        if (--budget[0] < 0 || path.size() >= 16) return Step.waiting("Dependency depth / work limit reached");
        // Never enqueue the same dependency repeatedly while its real machine is still working.
        for (var node : network.nodes()) for (var job : node.processing)
            if (!path.contains(job.pattern()) && job.pattern().outputs().stream().anyMatch(s -> ItemStack.isSameItemSameComponents(s, item)))
                return Step.waiting("Waiting for real dependency output: " + item.getHoverName().getString());
        Step blocked = Step.waiting("Missing base ingredient: " + item.getHoverName().getString());
        for (var candidate : crafts) {
            if (!ItemStack.isSameItemSameComponents(candidate.output(level), item) || !path.add(candidate)) continue;
            Step child;
            try { child = inputs(candidate.grid(), new Stock(stock), network, level, crafts, machines, path, budget); }
            finally { path.remove(candidate); }
            if (child == null) return new Step(candidate, null, "");
            if (child.work()) return child;
            blocked = child;
            if (budget[0] < 0) return blocked;
        }
        for (var binding : machines) {
            var candidate = binding.pattern();
            if (candidate.outputs().stream().noneMatch(s -> ItemStack.isSameItemSameComponents(s, item)) || !path.add(candidate)) continue;
            // An undelivered parent may be reordered. Never interrupt physical work already handed to a machine.
            if (binding.provider().processing.size()>=16||!binding.provider().processing.isEmpty()&&binding.provider().processing.getFirst().delivered()) {
                path.remove(candidate);blocked=Step.waiting("Dependency provider is busy");continue;
            }
            Step child;
            try { child = inputs(candidate.inputs(), new Stock(stock), network, level, crafts, machines, path, budget); }
            finally { path.remove(candidate); }
            if (child == null) return new Step(null, binding, "");
            if (child.work()) return child;
            blocked = child;
            if (budget[0] < 0) return blocked;
        }
        return blocked;
    }
}
