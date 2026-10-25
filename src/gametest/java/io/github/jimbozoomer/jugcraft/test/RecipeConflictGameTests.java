package io.github.jimbozoomer.jugcraft.test;

import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipes;
import io.github.jimbozoomer.jugcraft.chemistry.FluidTanks;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.material.Fluids;

/** Compare the repaired routes and their old competitors with every loaded crafting recipe. */
public class RecipeConflictGameTests {
    private static ItemStack stack(String id) {
        if (id.isEmpty()) return ItemStack.EMPTY;
        if (id.startsWith("#")) {
            var tag = TagKey.create(Registries.ITEM, Identifier.parse(id.substring(1)));
            return BuiltInRegistries.ITEM.stream().map(ItemStack::new).filter(s -> s.is(tag)).findFirst().orElseThrow();
        }
        var result = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)));
        if (result.isEmpty()) throw new IllegalArgumentException("Unregistered ingredient: " + id);
        return result;
    }

    @GameTest(maxTicks = 100)
    public void repairedCraftsAndTheirCounterpartsHaveOneOutput(GameTestHelper h) throws Exception {
        try (var in = RecipeConflictGameTests.class.getResourceAsStream("/recipe_conflict_cases.json")) {
            var cases = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray();
            h.assertTrue(cases.size() >= 80, "The complete conflict fixture must be loaded");
            for (var element : cases) {
                var row = element.getAsJsonObject();
                var base = new ArrayList<ItemStack>();
                for (var ingredient : row.getAsJsonArray("grid")) base.add(stack(ingredient.getAsString()));
                // Horizontal mirroring and translating the grid are part of actual vanilla matching.
                for (boolean mirror : List.of(false, true)) for (int dx = 0; dx < 3; dx++) for (int dy = 0; dy < 3; dy++) {
                    var grid = new ArrayList<ItemStack>();
                    for (int i = 0; i < 9; i++) grid.add(ItemStack.EMPTY);
                    boolean fits = true;
                    for (int y = 0; y < 3; y++) for (int x = 0; x < 3; x++) {
                        var value = base.get(y * 3 + x);
                        if (value.isEmpty()) continue;
                        if (x + dx >= 3 || y + dy >= 3) { fits = false; continue; }
                        grid.set((y + dy) * 3 + (mirror ? 2 - x - dx : x + dx), value);
                    }
                    if (!fits) continue;
                    var input = CraftingInput.of(3, 3, grid);
                    var matches = new ArrayList<String>();
                    for (var holder : h.getLevel().getServer().getRecipeManager().getRecipes()) {
                        if (holder.value() instanceof CraftingRecipe recipe && recipe.matches(input, h.getLevel())) {
                            var output = recipe.assemble(input);
                            matches.add(holder.id().identifier().toString());
                            h.assertTrue(output.is(stack(row.get("output").getAsString()).getItem())
                                    && output.getCount() == row.get("count").getAsInt(),
                                    row.get("recipe") + " unexpectedly also makes " + output + " via " + holder.id());
                        }
                    }
                    h.assertTrue(matches.size() == 1 && matches.contains(row.get("recipe").getAsString()),
                            row.get("recipe") + " must uniquely match, got " + matches);
                }
            }
        }
        h.succeed();
    }

    @GameTest(maxTicks = 40)
    public void compactReactorProductDoesNotChangeWithBatchSize(GameTestHelper h) {
        var server = h.getLevel().getServer();
        var kind = MachineKind.CHEMICAL_REACTOR;
        for (String fruit : List.of("lemon", "orange")) for (int count : List.of(4, 8, 64)) for (int water : List.of(250, 1000, 8000)) {
            var tanks = new FluidTanks(kind.fluidSpec(), (i, v) -> true, () -> {});
            tanks.input(0).fill(Fluids.WATER, water);
            var feed = stack("jugcraft:" + fruit);
            feed.setCount(count);
            var inputs = List.of(feed, ItemStack.EMPTY);
            var matches = FluidRecipes.entries(server, kind).stream().filter(e -> e.recipe().legacy()
                    && e.recipe().itemsMatch(inputs) && e.recipe().fluidsMatch(tanks)).toList();
            String expected = "jugcraft:chemical_reaction/citric_acid_" + fruit;
            h.assertTrue(matches.size() == 1 && matches.getFirst().id().toString().equals(expected),
                    "Only acid may match citrus at " + count + " fruit / " + water + " mB: " + matches);
            h.assertTrue(FluidRecipes.find(server, kind, inputs, tanks).orElseThrow() == matches.getFirst().recipe(),
                    "Compact machine lookup selects the unique acid recipe");
            var mash = FluidRecipes.entries(server, kind).stream().filter(e -> e.id().getPath().endsWith("heat_crop_mash")).findFirst().orElseThrow();
            feed.setCount(16);
            h.assertTrue(mash.recipe().itemsMatch(List.of(feed)), "Citrus still enters the controlled fermentation chain");
        }
        var tanks = new FluidTanks(kind.fluidSpec(), (i, v) -> true, () -> {});
        tanks.input(0).fill(Fluids.WATER, 1000);
        var wheat = stack("minecraft:wheat");
        wheat.setCount(8);
        var ethanol = FluidRecipes.find(server, kind, List.of(wheat, ItemStack.EMPTY), tanks).orElseThrow();
        h.assertTrue(ethanol.fluidResults().getFirst().amount() == 250
                && BuiltInRegistries.FLUID.getKey(ethanol.fluidResults().getFirst().fluid()).toString().equals("jugcraft:bioethanol"),
                "Non-citrus crops retain direct bioethanol yield");
        wheat.setCount(7);
        h.assertTrue(FluidRecipes.find(server, kind, List.of(wheat, ItemStack.EMPTY), tanks).isEmpty(), "Insufficient crops cannot start fermentation");
        h.succeed();
    }
}
