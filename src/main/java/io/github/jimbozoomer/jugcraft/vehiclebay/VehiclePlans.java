package io.github.jimbozoomer.jugcraft.vehiclebay;

import java.util.ArrayList;
import java.util.List;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

/** The existing vehicle bills of materials, without requiring a memorized crafting grid. */
public final class VehiclePlans {
    public static final String[] IDS = {"diesel_walker", "armoured_walker", "howitzer_walker", "landship"};
    public record Need(String id, int count) {
        public boolean matches(ItemStack stack) {
            return id.startsWith("#") ? stack.is(TagKey.create(Registries.ITEM, Identifier.parse(id.substring(1))))
                : stack.is(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)));
        }
        public ItemStack preview() {
            if (id.startsWith("#")) return new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id(id.contains("gears") ? "steel_gear" : "steel_plate")), count);
            return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)), count);
        }
    }
    private static Need n(String id, int count) { return new Need(id.contains(":") ? id : "jugcraft:" + id, count); }
    public static ItemStack product(int plan) { return new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id(IDS[Math.clamp(plan, 0, 3)]))); }
    public static int plan(ItemStack stack) {
        for (int i = 0; i < IDS.length; i++) if (stack.is(product(i).getItem())) return i;
        return -1;
    }
    public static List<Need> needs(int plan, int armor, int drive, boolean refit, VehicleFitout old) {
        List<Need> out = new ArrayList<>();
        if (refit) out.add(n("minecraft:iron_nugget", 4));
        else switch (plan) {
            case 0 -> out.addAll(List.of(n("riveted_rust_plate", 2), n("diesel_engine", 1), n("mining_drill", 1),
                n("#c:plates/steel", 1), n("minecraft:anvil", 1), n("minecraft:piston", 2)));
            case 1 -> out.addAll(List.of(n("#c:plates/steel", 4), n("steel_block", 1), n("diesel_walker", 1), n("minecraft:piston", 2)));
            case 2 -> out.addAll(List.of(n("steel_block", 2), n("minecraft:dispenser", 1), n("#c:plates/steel", 2), n("diesel_walker", 1), n("minecraft:piston", 2)));
            case 3 -> out.addAll(List.of(n("#c:plates/steel", 2), n("imperial_crest", 1), n("diesel_engine", 2), n("minecraft:dispenser", 1), n("belt", 2), n("#c:gears/steel", 1)));
            default -> throw new IllegalArgumentException("Unknown vehicle plan");
        }
        if (armor == 1 && (!refit || old.armor() != armor)) out.add(n("#c:plates/steel", 8));
        if (drive != 0 && (!refit || old.drive() != drive)) {
            out.add(n("#c:gears/steel", 2)); out.add(n("minecraft:redstone", 4));
        }
        return out;
    }
    private VehiclePlans() { }
}
