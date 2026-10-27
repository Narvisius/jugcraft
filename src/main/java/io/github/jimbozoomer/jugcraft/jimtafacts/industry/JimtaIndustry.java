package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Consolidates overlapping legacy machines into native processors, keeping their supplied art. */
public final class JimtaIndustry {
    public static final Map<String, MachineBlock> MACHINES = new LinkedHashMap<>();
    public static final Map<String, Item> MATERIALS = new LinkedHashMap<>();
    private JimtaIndustry() {}

    public static void register() {
        try (var stream = JimtaIndustry.class.getClassLoader().getResourceAsStream("jugcraft/jimtafacts/industry.json")) {
            if (stream == null) throw new IllegalStateException("Missing Jimtafacts industry catalog");
            var entries = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : entries.entrySet()) {
                String id = entry.getKey();
                MachineKind kind = MachineKind.valueOf(entry.getValue().getAsJsonObject().get("machine").getAsString());
                if (id.equals(kind.id)) { MACHINES.put(id, JugcraftMachines.MACHINES.get(kind)); continue; }
                var key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(id));
                var machine = new JimtaIndustryMachine(BlockBehaviour.Properties.ofFullCopy(Blocks.FURNACE).setId(key),
                        kind);
                Registry.register(BuiltInRegistries.BLOCK, key, machine);
                var itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
                Registry.register(BuiltInRegistries.ITEM, itemKey,
                        new BlockItem(machine, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
                MACHINES.put(id, machine);
                ((FabricBlockEntityType) JugcraftMachines.MACHINE_ENTITY).addValidBlock(machine);
            }
        } catch (java.io.IOException e) { throw new IllegalStateException("Cannot read Jimtafacts industry catalog", e); }
        try (var stream = JimtaIndustry.class.getClassLoader().getResourceAsStream("jugcraft/jimtafacts/industry-materials.json")) {
            if (stream == null) throw new IllegalStateException("Missing industrial material catalog");
            for (var entry : JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray()) {
                String id = entry.getAsString();
                var key = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
                var properties=new Item.Properties().setId(key);
                MATERIALS.put(id, Registry.register(BuiltInRegistries.ITEM, key, id.endsWith("_turning_blank")?new IndustrialLathe.Blank(properties):id.endsWith("_cutting_blade")?new IndustrialCuttingBlade(properties,id.contains("diamond")?8:id.contains("steel")?5:2):new Item(properties)));
            }
        } catch (java.io.IOException e) { throw new IllegalStateException("Cannot read industrial materials", e); }
        Block[] blocks = MACHINES.values().stream().filter(b -> b instanceof JimtaIndustryMachine).toArray(Block[]::new);
        JugcraftMachines.registerAutomation(blocks);
        FluidStorage.SIDED.registerForBlocks((level, pos, state, entity, side) -> {
            var machine = MachineBlock.machineAt(level, pos, state);
            return machine == null ? null : machine.fluidFor(side);
        }, blocks);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register(output -> MACHINES.values().forEach(output::accept));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
                .register(output -> MATERIALS.values().forEach(output::accept));
    }
}
