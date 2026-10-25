package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;

/** Removable real cards and filter samples; no hidden upgrade copy on a block item. */
public final class DigitalUpgrades {
    public static final List<String> ITEMS = List.of("basic_card", "advanced_card", "speed_card", "capacity_card", "fuzzy_card", "inverter_card", "redstone_card", "crafting_card", "equal_distribution_card");
    private static final Set<String> PORTS = Set.of("import_bus", "export_bus", "storage_bus", "fluid_import_bus", "fluid_export_bus", "fluid_storage_bus", "formation_plane", "annihilation_plane");
    private DigitalUpgrades() {}
    public static void register() {
        for (String name : ITEMS) {
            var key = ResourceKey.create(Registries.ITEM, Jugcraft.id("jimta_" + name));
            DigitalStorage.ITEMS.put(name, Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key))));
        }
    }
    public static boolean port(String kind) { return PORTS.contains(kind); }
    public static int count(DigitalNode node, String card) { int count = 0; for (int i = 9; i < 14; i++) if (node.getItem(i).is(DigitalStorage.ITEMS.get(card))) count++; return count; }
    public static boolean has(DigitalNode node, String card) { return count(node, card) != 0; }
    public static int batch(DigitalNode node) { return Math.min(64, 16 << Math.min(2, count(node, "speed_card"))); }
    public static boolean acceptsSlot(DigitalNode node, int slot, ItemStack stack) {
        if (slot < 9) return slot < filters(node) && !stack.isEmpty();
        if (slot >= 14 || stack.isEmpty()) return false;
        String name = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().replaceFirst("^jimta_", "");
        if (!ITEMS.contains(name) || name.equals("basic_card") || name.equals("advanced_card")) return false;
        if (name.equals("crafting_card") && !node.kind().equals("export_bus")) return false;
        if (name.equals("equal_distribution_card") && !node.kind().endsWith("export_bus")) return false;
        if (name.equals("fuzzy_card") && node.kind().startsWith("fluid_")) return false;
        return true;
    }
    public static int filters(DigitalNode node) { return Math.min(9, 1 + count(node, "capacity_card") * 4); }
    public static List<ItemStack> samples(DigitalNode node) {
        var result = new ArrayList<ItemStack>(); if (!node.filter().isEmpty()) result.add(node.filter());
        for (int i = 0; i < filters(node); i++) if (!node.getItem(i).isEmpty()) result.add(node.getItem(i).copyWithCount(1));
        return result;
    }
    public static boolean accepts(DigitalNode node, ItemStack sample) {
        var filters = samples(node); if (filters.isEmpty()) return true;
        boolean fuzzy = has(node, "fuzzy_card");
        boolean match = filters.stream().anyMatch(s -> fuzzy ? s.is(sample.getItem()) : ItemStack.isSameItemSameComponents(s, sample));
        return has(node, "inverter_card") != match;
    }
    public static boolean acceptsFluid(DigitalNode node, net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant fluid) {
        var filters = samples(node).stream().map(DigitalFluidAutomation::filter).filter(f -> !f.isBlank()).toList();
        if (filters.isEmpty()) return true;
        return has(node, "inverter_card") != filters.contains(fluid);
    }
    public static boolean paused(DigitalNode node) {
        boolean signal = node.getLevel().hasNeighborSignal(node.getBlockPos());
        return has(node, "redstone_card") ? !signal : signal;
    }
    public static void requestMissing(DigitalNode node, DigitalNetwork network, ItemStack sample) {
        if (!has(node, "crafting_card") || !(node.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) return;
        for (var pattern : DigitalPatterns.available(network, level)) if (ItemStack.isSameItemSameComponents(pattern.output(level), sample)) {
            for (var cpu : network.nodes()) if (DigitalProcessors.cpu(cpu.kind())) {
                if (cpu.orders.stream().anyMatch(o -> ItemStack.isSameItemSameComponents(o.pattern().output(level), sample))) return;
                if (cpu.request(pattern, 1)) return;
            }
        }
    }
}
