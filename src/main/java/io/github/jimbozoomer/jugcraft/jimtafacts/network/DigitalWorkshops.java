package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.machine.GeneratorFuels;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;

/** Functional source workshops share JE, atomic transfers and the current recipe manager. */
public final class DigitalWorkshops {
    public static DataComponentType<Long> ENERGY;
    public static DataComponentType<List<ItemStack>> FILTERS;
    private DigitalWorkshops() {}
    public static void register() {
        ENERGY = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("jimta_stored_energy"), DataComponentType.<Long>builder().persistent(io.github.jimbozoomer.jugcraft.jimtafacts.JimtaCodecs.longRange(0, 1_600_000)).networkSynchronized(ByteBufCodecs.VAR_LONG).build());
        var codec = ItemStack.CODEC.listOf(0, 17);
        FILTERS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("jimta_cell_filters"), DataComponentType.<List<ItemStack>>builder().persistent(codec).networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(codec)).build());
    }
    public static boolean filtered(ItemStack cell, ItemStack sample) {
        var filters = cell.getOrDefault(FILTERS, List.<ItemStack>of());
        return !filters.isEmpty() && filters.stream().noneMatch(f -> sample.is(f.getItem()));
    }
    public static boolean hasInventory(String kind) { return Set.of("vibration_chamber", "charger", "inscriber", "cell_workbench", "io_port", "molecular_assembler", "condenser", "wireless_access_point").contains(kind); }
    public static boolean canInsert(String kind, int slot, ItemStack stack) {
        return switch (kind) {
            case "condenser" -> slot == 0 ? java.util.Arrays.stream(new int[]{1,4,16,64,256}).anyMatch(n -> stack.is(DigitalStorage.ITEMS.get("cell_component_" + n + "k"))) : slot == 1 && DigitalCondenser.input(stack);
            case "wireless_access_point" -> slot == 0 && stack.is(DigitalStorage.ITEMS.get("wireless_booster"));
            case "vibration_chamber" -> slot == 0 && GeneratorFuels.burnTicks(stack) > 0;
            case "charger" -> slot == 0 && (stack.is(Items.COMPASS) || stack.is(DigitalStorage.ITEMS.get("certus_quartz_crystal")) || stack.getItem() instanceof io.github.jimbozoomer.jugcraft.tools.Chargeable);
            case "inscriber" -> slot >= 0 && slot <= 3;
            case "cell_workbench" -> slot == 0 ? DigitalCellCards.configurable(stack) : slot > 0 && slot < 18;
            case "io_port" -> slot == 0 && DigitalCellIO.accepts(stack);
            case "molecular_assembler" -> slot == 0 && stack.has(DigitalPatterns.PATTERN);
            default -> false;
        };
    }
    public static void generateFuel(DigitalNode node) {
        if (!node.kind().equals("vibration_chamber") || node.energy.getCapacity() - node.energy.getAmount() < 160) return;
        if (node.burn <= 0) {
            var fuel = node.getItem(0); int ticks = GeneratorFuels.burnTicks(fuel);
            if (ticks <= 0) return;
            fuel.shrink(1); node.burn = ticks;
        }
        int ticks = Math.min(20, node.burn); node.burn -= ticks;
        node.energy.setAmount(node.energy.getAmount() + ticks * 8L); node.setChanged();
    }
    public static void configureCell(DigitalNode node) {
        if (!node.kind().equals("cell_workbench") || !DigitalCellCards.configurable(node.getItem(0))) return;
        // Filters remember item types, not nested container contents or other saved cell components.
        var filters = java.util.stream.IntStream.range(1, 18).mapToObj(node::getItem).filter(s -> !s.isEmpty()).map(s -> new ItemStack(s.getItem())).toList();
        var cell = node.getItem(0); var old = cell.getOrDefault(FILTERS, List.<ItemStack>of());
        if (filters.size() == old.size() && java.util.stream.IntStream.range(0, filters.size()).allMatch(i -> ItemStack.matches(old.get(i), filters.get(i)))) return;
        cell.set(FILTERS, filters); node.setChanged();
    }
    public static void work(DigitalNode node, DigitalNetwork network, ServerPlayer actor) {
        switch (node.kind()) {
            case "charger" -> {
                if (node.getItem(0).is(Items.COMPASS)) produce(node, network, new ItemStack(DigitalStorage.ITEMS.get("meteorite_compass")), 1, 1000, 0);
                else if (node.getItem(0).is(DigitalStorage.ITEMS.get("certus_quartz_crystal"))) produce(node, network, new ItemStack(DigitalStorage.ITEMS.get("charged_certus_quartz_crystal")), 1, 1000, 0);
                else if (node.getItem(0).getItem() instanceof io.github.jimbozoomer.jugcraft.tools.Chargeable) {
                    var stack = node.getItem(0);
                    long amount = Math.min(1024, io.github.jimbozoomer.jugcraft.tools.Chargeable.capacity(stack) - io.github.jimbozoomer.jugcraft.tools.Chargeable.energy(stack));
                    amount = Math.min(amount, network.nodes().stream().filter(DigitalNode::poweredSource).mapToLong(n -> n.energy.getAmount()).sum());
                    if (amount > 0) try (var tx = Transaction.openOuter()) {
                        if (!network.pay(amount, tx)) return;
                        node.snapshotCells(tx); io.github.jimbozoomer.jugcraft.tools.Chargeable.charge(stack, amount); tx.commit();
                    }
                }
            }
            case "inscriber" -> {
                var press = node.getItem(0); String process = null;
                if (DigitalProduction.print(node, network)) return;
                if (press.has(DigitalEquipment.PRESS_NAME) && !node.getItem(1).isEmpty()) {
                    var renamed = node.getItem(1).copyWithCount(1);
                    String name = press.get(DigitalEquipment.PRESS_NAME);
                    if (name.isEmpty()) renamed.remove(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
                    else renamed.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(name));
                    produce(node, network, renamed, 4, 400, 1); return;
                }
                for (String type : List.of("logic", "calculation", "engineering")) if (press.is(DigitalStorage.ITEMS.get(type + "_processor_press"))) process = type;
                if (process == null || !node.getItem(2).is(Items.REDSTONE) || !node.getItem(3).is(io.github.jimbozoomer.jugcraft.materials.JugcraftMaterials.SILICON)) return;
                Item material = process.equals("logic") ? Items.GOLD_INGOT : process.equals("engineering") ? Items.DIAMOND : Items.IRON_INGOT;
                if (node.getItem(1).is(material)) produce(node, network, new ItemStack(DigitalStorage.ITEMS.get(process + "_processor")), 4, 400, 1, 2, 3);
            }
            case "molecular_assembler" -> {
                var pattern = node.getItem(0).get(DigitalPatterns.PATTERN);
                if (pattern != null) DigitalPatterns.craft(pattern, network, actor.level(), true);
            }
            case "io_port" -> DigitalCellIO.tick(node,network,actor);
            default -> { }
        }
    }
    static void produce(DigitalNode node, DigitalNetwork network, ItemStack result, int output, int cost, int... inputs) {
        var previous = node.getItem(output);
        if (!previous.isEmpty() && (!ItemStack.isSameItemSameComponents(previous, result) || previous.getCount() >= previous.getMaxStackSize())) return;
        try (var tx = Transaction.openOuter()) {
            if (!network.pay(cost, tx)) return;
            node.snapshotCells(tx);
            for (int input : inputs) node.getItem(input).shrink(1);
            if (previous.isEmpty()) node.setItem(output, result); else previous.grow(result.getCount());
            tx.commit();
            node.renderWorkCycle();
        }
    }
}
