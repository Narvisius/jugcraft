package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.component.CustomData;

/** Flower-only memory: never copies workshop inventories, ownership, or linked targets. */
public final class BotanicalFlowerMemory {
    public static DataComponentType<CustomData> TYPE;
    private BotanicalFlowerMemory() {}
    static void register() {
        TYPE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
            Jugcraft.id("botanical_flower_memory"), DataComponentType.<CustomData>builder()
                .persistent(CustomData.CODEC).networkSynchronized(CustomData.STREAM_CODEC).build());
    }
}
