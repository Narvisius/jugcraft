package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.CuttingRecipe;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;

/** Checks all loaded recipes, so an imported or vanilla competitor cannot silently win. */
public class FoodRecipeConflictGameTests {
    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)));
    }

    private static void output(GameTestHelper h, List<ItemStack> matches, String id, int count) {
        h.assertTrue(matches.size() == 1, "Exactly one matching recipe for " + id + ": " + matches);
        h.assertTrue(matches.getFirst().is(stack(id).getItem()) && matches.getFirst().getCount() == count,
                "Expected output and unchanged yield: " + id + " x" + count);
    }

    private static List<ItemStack> crafts(GameTestHelper h, String... ingredients) {
        var slots = new ArrayList<ItemStack>();
        for (String id : ingredients) slots.add(stack(id));
        while (slots.size() < 9) slots.add(ItemStack.EMPTY);
        var input = CraftingInput.of(3, 3, slots);
        var matches = new ArrayList<ItemStack>();
        for (var holder : h.getLevel().getServer().getRecipeManager().getRecipes()) {
            if (holder.value() instanceof CraftingRecipe recipe && recipe.matches(input, h.getLevel()))
                matches.add(recipe.assemble(input));
        }
        return matches;
    }

    @GameTest(maxTicks = 40)
    public void foodCraftingHasOneOutputAndConservesWholeIngredientYields(GameTestHelper h) {
        output(h, crafts(h, "jugcraft:popcorn", "jugcraft:popcorn", "jugcraft:caramel"), "jugcraft:caramel_popcorn", 1);
        output(h, crafts(h, "jugcraft:caramel_popcorn"), "jugcraft:popcorn_ball", 1);
        output(h, crafts(h, "jugcraft:marigold"), "jugcraft:marigold_petals", 4);
        output(h, crafts(h, "jugcraft:marigold_petals", "jugcraft:marigold_petals", "jugcraft:marigold_petals", "jugcraft:marigold_petals"), "minecraft:orange_dye", 1);
        output(h, crafts(h, "jugcraft:pumpkin_slice", "jugcraft:pumpkin_slice", "jugcraft:pumpkin_slice", "jugcraft:pumpkin_slice"), "minecraft:pumpkin_seeds", 5);
        output(h, crafts(h, "minecraft:pumpkin"), "minecraft:pumpkin_seeds", 4);
        output(h, crafts(h, "jugcraft:cake_slice", "jugcraft:cake_slice", "jugcraft:cake_slice", "jugcraft:cake_slice", "jugcraft:cake_slice", "jugcraft:cake_slice", "jugcraft:cake_slice"), "jugcraft:cavern_cake_layer", 4);
        for (String ingredient : List.of("jugcraft:marigold_petals", "jugcraft:pumpkin_slice", "jugcraft:cake_slice"))
            h.assertTrue(crafts(h, ingredient).isEmpty(), "One piece cannot produce a whole batch: " + ingredient);
        h.succeed();
    }

    @GameTest(maxTicks = 40)
    public void knifeOperationsHaveUniqueOutputs(GameTestHelper h) {
        for (String[] row : List.of(new String[]{"minecraft:cake", "jugcraft:cake_slice", "7"},
                new String[]{"minecraft:pumpkin", "jugcraft:pumpkin_slice", "4"},
                new String[]{"minecraft:dead_bush", "jugcraft:landscape_dead_branch", "1"},
                new String[]{"jugcraft:landscape_dead_branch", "minecraft:stick", "2"})) {
            var knife = stack("jugcraft:iron_knife");
            var input = stack(row[0]);
            var matches = new ArrayList<ItemStack>();
            for (var holder : h.getLevel().getServer().getRecipeManager().getRecipes()) {
                if (holder.value() instanceof CuttingRecipe recipe && recipe.ingredient().test(input) && recipe.tool().test(knife))
                    matches.addAll(recipe.cut());
            }
            output(h, matches, row[1], Integer.parseInt(row[2]));
            var actual = CuttingRecipe.find(h.getLevel().getServer(), input, knife).orElseThrow();
            output(h, actual.cut(), row[1], Integer.parseInt(row[2]));
            h.assertTrue(CuttingRecipe.find(h.getLevel().getServer(), input, stack("minecraft:stick")).isEmpty(), "Wrong tool cannot process " + row[0]);
        }
        h.succeed();
    }

    @GameTest(maxTicks = 40)
    public void eggsHaveOnePreparationPerStation(GameTestHelper h) {
        var input = new SingleRecipeInput(stack("minecraft:egg"));
        for (String type : List.of("minecraft:smelting", "minecraft:smoking", "minecraft:campfire_cooking", "jugcraft:household_pan")) {
            var matches = new ArrayList<ItemStack>();
            for (var holder : h.getLevel().getServer().getRecipeManager().getRecipes()) {
                if (holder.value() instanceof AbstractCookingRecipe recipe
                        && BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType()).toString().equals(type)
                        && recipe.matches(input, h.getLevel())) matches.add(recipe.assemble(input));
            }
            output(h, matches, type.equals("jugcraft:household_pan") ? "jugcraft:wildlife_cooked_egg" : "jugcraft:fried_egg", 1);
        }
        h.succeed();
    }
}
