package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;

/** Source loot filters are retained; each selected scroll is a concrete native level-one working. */
public final class JimtaSpellLoot {
    public static void register(){additionalDrops();Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE,Jugcraft.id("jimtafacts_spell_scroll"),Scroll.CODEC);Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE,Jugcraft.id("jimtafacts_ring_enhancement"),Ring.CODEC);Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE,Jugcraft.id("jimtafacts_furled_map"),MapTarget.CODEC);}
    private static void additionalDrops(){
        net.fabricmc.fabric.api.loot.v3.LootTableEvents.MODIFY.register((key,table,source,registries)->{
            if(!source.isBuiltin()||!JimtaSpellbooks.enabled())return;
            String name=switch(key.identifier().toString()){case "minecraft:entities/blaze"->"blaze";case "minecraft:entities/ender_dragon"->"dragon";case "minecraft:entities/evoker"->"evoker";case "minecraft:entities/hoglin"->"hoglin";case "minecraft:entities/stray"->"stray";default->null;};if(name==null)return;
            var lootKey=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,Jugcraft.id("jimtafacts/spellbooks/entities/additional_"+name+"_loot"));
            registries.lookupOrThrow(net.minecraft.core.registries.Registries.LOOT_TABLE).get(lootKey).ifPresent(holder->table.withPool(LootPool.lootPool().setRolls(net.minecraft.core.Holder.direct(new net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue(1))).add(net.minecraft.world.level.storage.loot.entries.NestedLootTable.lootTableReference(holder))));
        });
    }
    public record Scroll(List<String> spells) implements LootItemFunction {
        static final MapCodec<Scroll> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Codec.STRING.listOf(1,128).fieldOf("spells").forGetter(Scroll::spells)).apply(i,Scroll::new));
        @Override public MapCodec<Scroll> codec(){return CODEC;}
        @Override public ItemStack apply(ItemStack stack,LootContext context){var available=spells.stream().filter(JimtaSpellcasting.SPELLS::containsKey).toList();if(available.isEmpty())return ItemStack.EMPTY;var name=available.get(context.getRandom().nextInt(available.size()));return stack.transmuteCopy(JimtaSpellbooks.ITEMS.get("scroll_"+name));}
    }
    public record Ring(String school) implements LootItemFunction {
        static final MapCodec<Ring> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Codec.STRING.optionalFieldOf("school","").forGetter(Ring::school)).apply(i,Ring::new));
        @Override public MapCodec<Ring> codec(){return CODEC;}
        @Override public ItemStack apply(ItemStack stack,LootContext context){JimtaSpellbookCurios.randomizeRing(stack,context.getRandom(),school);return stack;}
    }
    public record MapTarget(String destination) implements LootItemFunction {
        static final MapCodec<MapTarget> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Codec.STRING.fieldOf("destination").forGetter(MapTarget::destination)).apply(i,MapTarget::new));
        @Override public MapCodec<MapTarget> codec(){return CODEC;}
        @Override public ItemStack apply(ItemStack stack,LootContext context){if(JimtaExploration.DESTINATIONS.contains(destination))stack.set(JimtaExploration.TARGET,destination);return stack;}
    }
}
