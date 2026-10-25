package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

/** Patterns contain ingredients, not trusted outputs; current recipes always decide the result. */
public final class DigitalPatterns {
    public static DataComponentType<Pattern> PATTERN;
    private DigitalPatterns() {}
    public static void register() {
        PATTERN = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("jimta_crafting_pattern"), DataComponentType.<Pattern>builder()
                .persistent(Pattern.CODEC).networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(Pattern.CODEC)).build());
    }
    public record Pattern(List<ItemStack> grid, int mode, String recipeId) {
        private static final Codec<Pattern> RECORD = RecordCodecBuilder.create(i -> i.group(ItemStack.OPTIONAL_CODEC.listOf(9, 9).fieldOf("grid").forGetter(Pattern::grid), Codec.intRange(0, 2).optionalFieldOf("mode", 0).forGetter(Pattern::mode), Codec.string(0, 256).optionalFieldOf("recipe", "").forGetter(Pattern::recipeId)).apply(i, Pattern::new));
        public static final Codec<Pattern> CODEC = Codec.either(RECORD, ItemStack.OPTIONAL_CODEC.listOf(9, 9)).xmap(e -> e.map(java.util.function.Function.identity(), Pattern::new), p -> com.mojang.datafixers.util.Either.left(p));
        public Pattern(List<ItemStack> grid) { this(grid, 0, ""); }
        public Pattern { grid = grid.stream().map(s -> s.isEmpty() ? ItemStack.EMPTY : s.copyWithCount(1)).toList(); }
        @Override public List<ItemStack> grid() { return grid.stream().map(ItemStack::copy).toList(); }
        @Override public boolean equals(Object other) { return other instanceof Pattern p && mode == p.mode && recipeId.equals(p.recipeId) && java.util.stream.IntStream.range(0, 9).allMatch(i -> ItemStack.matches(grid.get(i), p.grid.get(i))); }
        @Override public int hashCode() { int value = Objects.hash(mode, recipeId); for (var item : grid) value = 31 * value + ItemStack.hashItemAndComponents(item); return value; }
        public CraftingInput input() { return CraftingInput.of(3, 3, grid()); }
        public ItemStack output(ServerLevel level) { var input = input(); return mode == 0 ? level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY) : DigitalSpecialPatterns.output(this, level); }
    }
    public record Order(Pattern pattern, int remaining, UUID root) {
        public static final Codec<Order> CODEC = RecordCodecBuilder.create(i -> i.group(Pattern.CODEC.fieldOf("pattern").forGetter(Order::pattern), Codec.intRange(1, 64).fieldOf("remaining").forGetter(Order::remaining),net.minecraft.core.UUIDUtil.CODEC.optionalFieldOf("root",new UUID(0,0)).forGetter(Order::root)).apply(i, Order::new));
        public Order(Pattern pattern,int remaining){this(pattern,remaining,UUID.randomUUID());}
        public Order { if(root.equals(new UUID(0,0)))root=UUID.randomUUID(); }
    }
    public static List<Pattern> available(DigitalNetwork network, ServerLevel level) {
        var result = new ArrayList<Pattern>();
        for (var node : network.nodes()) if (node.kind().equals("pattern_provider")) {
            for (int i = 0; i < node.getContainerSize() && result.size() < 256; i++) {
                var p = node.getItem(i).get(PATTERN); if (p != null && !p.output(level).isEmpty() && !result.contains(p)) result.add(p);
            }
        }
        return List.copyOf(result);
    }
    public static boolean craft(Pattern pattern, DigitalNetwork network, ServerLevel level) {
        return craft(pattern, network, level, false);
    }
    /** Bounded, cycle-safe dependency crafting. A failed subtree rolls back every input and JE debit. */
    public static boolean craft(Pattern pattern, DigitalNetwork network, ServerLevel level, boolean dependencies) {
        try (Transaction tx = Transaction.openOuter()) {
            var patterns = dependencies ? available(network, level) : List.<Pattern>of();
            if (!craftTree(pattern, network, level, patterns, new HashSet<>(), new int[]{64}, tx)) return false;
            tx.commit(); return true;
        }
    }
    private static boolean craftTree(Pattern pattern, DigitalNetwork network, ServerLevel level, List<Pattern> patterns, Set<Pattern> path, int[] remaining, Transaction parent) {
        if (--remaining[0] < 0 || path.size() >= 16 || !path.add(pattern)) return false;
        var input = pattern.input(); var recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElse(null);
        try (Transaction tx = parent.openNested()) {
            if (pattern.mode() == 0 && recipe == null) return false;
            var output = pattern.output(level); if (output.isEmpty()) return false;
            for (var ingredient : pattern.grid()) {
                if (ingredient.isEmpty() || network.extract(ingredient, 1, tx) == 1) continue;
                boolean supplied = false;
                for (var candidate : patterns) {
                    if (!ItemStack.isSameItemSameComponents(ingredient, candidate.output(level))) continue;
                    if (craftTree(candidate, network, level, patterns, path, remaining, tx) && network.extract(ingredient, 1, tx) == 1) { supplied = true; break; }
                }
                if (!supplied) return false;
            }
            if (network.insert(output, output.getCount(), tx) != output.getCount()) return false;
            var remainders = pattern.mode() == 0 ? recipe.value().getRemainingItems(input) : DigitalSpecialPatterns.remainders(pattern);
            for (var remainder : remainders) if (!remainder.isEmpty() && network.insert(remainder, remainder.getCount(), tx) != remainder.getCount()) return false;
            tx.commit(); return true;
        } finally { path.remove(pattern); }
    }
}
