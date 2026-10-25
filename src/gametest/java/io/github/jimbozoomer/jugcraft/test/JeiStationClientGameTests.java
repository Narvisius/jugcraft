package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.compat.JugcraftJeiStations;
import java.util.*;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Real JEI registration, ingredient search, catalysts, layout creation, and rendered recipe pages. */
public class JeiStationClientGameTests implements FabricClientGameTest {
    public void runTest(ClientGameTestContext c) {
        if (FabricLoader.getInstance().isModLoaded("jei")) WithJei.run(c);
        else try (var world = c.worldBuilder().create()) { c.waitTicks(20); }
    }
    // Fabric loads the test entrypoint even without JEI. Keep its verifier away from JEI API types.
    public static class WithJei {
    private static volatile IJeiRuntime runtime;
    public static class Probe implements IModPlugin {
        public Identifier getPluginUid() { return Jugcraft.id("jei_station_test"); }
        public void onRuntimeAvailable(IJeiRuntime value) { runtime = value; }
        public void onRuntimeUnavailable() { runtime = null; }
    }
    static void run(ClientGameTestContext c) {
        try (var world = c.worldBuilder().create()) {
            for (int i = 0; i < 120 && runtime == null; i++) c.waitTicks(10);
            check(runtime != null, "JEI did not start");
            c.runOnClient(client -> {
                var manager = runtime.getRecipeManager();
                var focusFactory = runtime.getJeiHelpers().getFocusFactory();
                var stations = JugcraftJeiStations.load();
                try (var reader = client.getResourceManager().getResource(Jugcraft.id("recipe_view.json")).orElseThrow().openAsReader()) {
                    var catalog = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                    for (var group : catalog.getAsJsonArray("stations")) {
                        var data = group.getAsJsonObject();
                        var type = manager.getRecipeType(Jugcraft.id("station/" + data.get("type").getAsString()), JugcraftJeiStations.Recipe.class).orElseThrow();
                        check(manager.createRecipeLookup(type).includeHidden().get().count() == data.getAsJsonArray("recipes").size(), "JEI dropped a bundled recipe in " + type);
                    }
                } catch (java.io.IOException error) { throw new AssertionError(error); }
                for (var station : stations) {
                    var category = manager.getRecipeCategory(station.type());
                    check(category != null, "Missing category " + station.type().getUid());
                    var recipes = manager.createRecipeLookup(station.type()).includeHidden().get().toList();
                    check(recipes.size() == station.recipes().size() && !recipes.isEmpty(), "Missing recipes " + station.type().getUid());
                    var catalysts = manager.createCraftingStationLookup(station.type()).includeHidden().getItemStack().toList();
                    for (var item : station.catalysts()) check(catalysts.stream().anyMatch(s -> s.is(item)), "Missing station " + item);
                    for (var recipe : recipes) {
                        check(recipe.inputs().stream().allMatch(s -> !s.isEmpty()), "Empty ingredient alternatives");
                        check(recipe.outputs().stream().noneMatch(ItemStack::isEmpty), "Empty recipe output");
                        check(manager.createRecipeLayoutDrawable(category, recipe, focusFactory.getEmptyFocusGroup()).isPresent(), "Recipe layout rejected");
                    }
                }
                for (var pair : List.of(new String[]{"pot_cooking", "tomato_soup"}, new String[]{"cutting", "bacon"},
                        new String[]{"hearth_oven", "apple_pie"}, new String[]{"cider_barrel", "aged_cider"})) {
                    var type = manager.getRecipeType(Jugcraft.id("station/" + pair[0]), JugcraftJeiStations.Recipe.class).orElseThrow();
                    var item = new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id(pair[1])));
                    var focus = focusFactory.createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, item);
                    check(manager.createRecipeLookup(type).limitFocus(List.of(focus)).get().findAny().isPresent(), "JEI recipe search failed for " + pair[1]);
                }
                for (String kind : List.of("enrichment", "fuel_fabrication")) check(manager.getRecipeType(Jugcraft.id(kind)).isPresent(), "Missing fuel processor " + kind);
            });
            for (String kind : List.of("pot_cooking", "cutting", "household_construction", "candy_kettle", "nouveau_imbuement_chamber")) {
                c.runOnClient(client -> runtime.getRecipesGui().showTypes(List.of(runtime.getRecipeManager().getRecipeType(Jugcraft.id("station/" + kind)).orElseThrow())));
                c.waitTicks(8); c.takeScreenshot("jugcraft_jei_" + kind);
                c.runOnClient(client -> client.gui.setScreen(null));
            }
        }
    }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
