package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Color-separated loaded networks. Transparent fluix connects any color; different dyes never connect directly. */
public final class DigitalCabling {
    private DigitalCabling() {}
    public static void register() {
        for (String kind : new String[]{"glass", "covered", "smart", "dense_covered", "dense_smart"}) for (int i = 0; i <= DyeColor.values().length; i++) {
            String color = i == DyeColor.values().length ? "fluix" : DyeColor.values()[i].getName();
            if (color.equals("fluix") && kind.equals("glass")) continue;
            String name = color + "_" + (kind.startsWith("dense_") ? kind.substring(6) + "_dense" : kind) + "_cable";
            var key = ResourceKey.create(Registries.BLOCK, Jugcraft.id("jimta_" + name));
            var properties = BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(.3f).noOcclusion().setId(key);
            float width = kind.startsWith("dense") ? 10 : kind.equals("glass") ? 4 : 6;
            var block = kind.endsWith("smart") ? new DigitalCableBlock.Smart(properties, color, width) : new DigitalCableBlock(properties, color, width);
            DigitalStorage.BLOCKS.put(name, Registry.register(BuiltInRegistries.BLOCK, key, block));
            var itemKey = ResourceKey.create(Registries.ITEM, key.identifier());
            Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
        }
    }
}
