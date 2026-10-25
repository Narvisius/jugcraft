package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

/** Stonecutting and smithing patterns pin a current recipe ID, never a client-authored output. */
public final class DigitalSpecialPatterns {
    private DigitalSpecialPatterns() {}
    public static DigitalPatterns.Pattern encode(int mode, List<ItemStack> grid, ItemStack wanted, ServerLevel level) {
        if (mode < 1 || mode > 2 || grid.size() != 9 || grid.getFirst().isEmpty()) return null;
        int count = mode == 1 ? 1 : 3;
        if (grid.subList(count, 9).stream().anyMatch(s -> !s.isEmpty())) return null;
        // Explicit output sample disambiguates stonecutting; smithing usually has a unique current recipe.
        int checked = 0;
        for (var holder : level.getServer().getRecipeManager().getRecipes()) {
            if (++checked > 32768) break;
            var output = assemble(mode, grid, holder.value(), level);
            if (!output.isEmpty() && (wanted.isEmpty() || ItemStack.isSameItemSameComponents(output, wanted))) return new DigitalPatterns.Pattern(grid, mode, holder.id().identifier().toString());
        }
        return null;
    }
    public static ItemStack output(DigitalPatterns.Pattern pattern, ServerLevel level) {
        var id = Identifier.tryParse(pattern.recipeId()); if (id == null) return ItemStack.EMPTY;
        int slots = pattern.mode() == 1 ? 1 : 3;
        if (pattern.grid().subList(slots, 9).stream().anyMatch(s -> !s.isEmpty())) return ItemStack.EMPTY;
        var recipe = level.getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, id)).orElse(null);
        return recipe == null ? ItemStack.EMPTY : assemble(pattern.mode(), pattern.grid(), recipe.value(), level);
    }
    private static ItemStack assemble(int mode, List<ItemStack> grid, Recipe<?> recipe, ServerLevel level) {
        if (mode == 1 && recipe instanceof StonecutterRecipe r) { var input = new SingleRecipeInput(grid.getFirst()); return r.matches(input, level) ? r.assemble(input) : ItemStack.EMPTY; }
        if (mode == 2 && recipe instanceof SmithingRecipe r) { var input = new SmithingRecipeInput(grid.get(0), grid.get(1), grid.get(2)); return r.matches(input, level) ? r.assemble(input) : ItemStack.EMPTY; }
        return ItemStack.EMPTY;
    }
    public static List<ItemStack> remainders(DigitalPatterns.Pattern pattern) {
        return pattern.grid().stream().filter(s -> !s.isEmpty()).map(s -> s.getItem().getCraftingRemainder()).filter(Objects::nonNull).map(net.minecraft.world.item.ItemStackTemplate::create).filter(s -> !s.isEmpty()).toList();
    }
}
