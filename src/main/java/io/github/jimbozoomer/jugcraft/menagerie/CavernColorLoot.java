package io.github.jimbozoomer.jugcraft.menagerie;

import com.mojang.serialization.MapCodec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Preserve the creature's supplied candy color in its real loot, including Looting counts. */
final class CavernColorLoot implements LootItemFunction {
    private static final CavernColorLoot INSTANCE=new CavernColorLoot();
    private static final MapCodec<CavernColorLoot> CODEC=MapCodec.unit(INSTANCE);
    static void register(){Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE,Jugcraft.id("cavern_gummy_colors"),CODEC);}
    @Override public MapCodec<CavernColorLoot> codec(){return CODEC;}
    @Override public ItemStack apply(ItemStack stack,LootContext context){
        if(stack.isEmpty()||!(context.getOptional(LootContextParams.THIS_ENTITY) instanceof MenagerieAnimal animal))return stack;
        var variant=animal.species().variants().get(animal.variant()).name();
        String color=variant.substring(variant.lastIndexOf('_')+1);
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem());
        if(!id.getNamespace().equals("jugcraft")||!id.getPath().endsWith("_red"))return stack;
        var item=Menagerie.ITEMS.get(id.getPath().substring(0,id.getPath().length()-3)+color);
        return item==null?stack:stack.transmuteCopy(item);
    }
}
