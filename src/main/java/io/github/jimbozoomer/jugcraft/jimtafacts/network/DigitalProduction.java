package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;

/** Reusable printing dies and intermediate components from the supplied production chain. */
public final class DigitalProduction {
    public static final String[] ITEMS = {"printed_logic_processor", "printed_calculation_processor", "printed_engineering_processor", "printed_silicon", "silicon_press", "formation_core", "annihilation_core", "item_cell_housing", "fluid_cell_housing", "ender_dust", "sky_dust", "fluix_pearl", "wireless_receiver", "wireless_booster", "singularity"};
    private DigitalProduction() {}
    public static void register() {
        for (String name : ITEMS) {
            var key = ResourceKey.create(Registries.ITEM, Jugcraft.id("jimta_" + name));
            DigitalStorage.ITEMS.put(name, Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key))));
        }
    }
    /** Returns true once a printing recipe matched, even if power/output space blocks it. */
    public static boolean print(DigitalNode node, DigitalNetwork network) {
        var top = node.getItem(0); var middle = node.getItem(1); var bottom = node.getItem(2);
        if (top.is(DigitalStorage.ITEMS.get("silicon_press")) && middle.is(io.github.jimbozoomer.jugcraft.materials.JugcraftMaterials.SILICON) && bottom.isEmpty()) {
            DigitalWorkshops.produce(node, network, new ItemStack(DigitalStorage.ITEMS.get("printed_silicon")), 4, 100, 1); return true;
        }
        for (String kind : List.of("logic", "calculation", "engineering")) {
            Item material = kind.equals("logic") ? Items.GOLD_INGOT : kind.equals("engineering") ? Items.DIAMOND : DigitalStorage.ITEMS.get("certus_quartz_crystal");
            if (top.is(DigitalStorage.ITEMS.get(kind + "_processor_press")) && middle.is(material) && bottom.isEmpty()) {
                DigitalWorkshops.produce(node, network, new ItemStack(DigitalStorage.ITEMS.get("printed_" + kind + "_processor")), 4, 100, 1); return true;
            }
            if (top.is(DigitalStorage.ITEMS.get("printed_" + kind + "_processor")) && middle.is(Items.REDSTONE) && bottom.is(DigitalStorage.ITEMS.get("printed_silicon"))) {
                DigitalWorkshops.produce(node, network, new ItemStack(DigitalStorage.ITEMS.get(kind + "_processor")), 4, 200, 0, 1, 2); return true;
            }
        }
        return false;
    }
}
