package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Supplied shaped upgrades retain equipment data; water recipes reject other potions. */
public final class BotanicalShapedRecipe extends CustomRecipe {
    private final ShapedRecipe shaped;
    private final boolean water;
    public static final RecipeSerializer<BotanicalShapedRecipe> ARMOR = serializer(false);
    public static final RecipeSerializer<BotanicalShapedRecipe> WATER = serializer(true);
    private static RecipeSerializer<BotanicalShapedRecipe> serializer(boolean water) {
        return new RecipeSerializer<>(ShapedRecipe.MAP_CODEC.xmap(r -> new BotanicalShapedRecipe(r, water), r -> r.shaped),
                ShapedRecipe.STREAM_CODEC.map(r -> new BotanicalShapedRecipe(r, water), r -> r.shaped));
    }
    private BotanicalShapedRecipe(ShapedRecipe shaped, boolean water) { this.shaped = shaped; this.water = water; }
    static void register() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Jugcraft.id("botanical_armor_upgrade"), ARMOR);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Jugcraft.id("botanical_water_shaped"), WATER);
    }
    @Override public boolean matches(CraftingInput input, Level level) {
        if (!shaped.matches(input, level)) return false;
        if (water) for (int i = 0; i < input.size(); i++) {
            var stack = input.getItem(i);
            if (stack.is(Items.POTION)) {
                var contents = stack.get(DataComponents.POTION_CONTENTS);
                if (contents == null || !contents.is(Potions.WATER)) return false;
            }
        }
        return true;
    }
    @Override public ItemStack assemble(CraftingInput input) {
        var result = shaped.assemble(input);
        if (!water) for (int i = 0; i < input.size(); i++) {
            var source = input.getItem(i);
            var id = BuiltInRegistries.ITEM.getKey(source.getItem());
            if (id.getNamespace().equals("jugcraft") && id.getPath().startsWith("jugtania_manasteel_")
                    && source.has(DataComponents.EQUIPPABLE)) return source.transmuteCopy(result.getItem(), result.getCount());
        }
        return result;
    }
    @Override public java.util.List<net.minecraft.world.item.crafting.display.RecipeDisplay> display() { return shaped.display(); }
    @Override public RecipeSerializer<BotanicalShapedRecipe> getSerializer() { return water ? WATER : ARMOR; }
}
