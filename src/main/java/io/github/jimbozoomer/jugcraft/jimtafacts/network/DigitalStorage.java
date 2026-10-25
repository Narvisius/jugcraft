package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Owner-supplied network art, native saved cells and the shared JE power system. */
public final class DigitalStorage {
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    public static BlockEntityType<DigitalNode> NODE;
    public static DataComponentType<CellContents> CONTENTS;
    public static final String[] DEVICES = {"controller", "energy_acceptor", "drive", "me_chest", "terminal", "crafting_terminal", "interface", "import_bus", "export_bus", "pattern_provider", "crafting_unit", "energy_cell", "dense_energy_cell", "vibration_chamber", "charger", "inscriber", "cell_workbench", "io_port", "molecular_assembler", "condenser", "wireless_access_point"};
    private DigitalStorage() {}
    public static void register() {
        CONTENTS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("jimta_cell_contents"),
                DataComponentType.<CellContents>builder().persistent(CellContents.CODEC)
                        .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(CellContents.CODEC)).build());
        for (String name : DEVICES) block(name, true);
        for (String name : DigitalProcessors.BLOCKS) block(name, true);
        for (String name : DigitalLinks.BLOCKS) block(name, true);
        for (String name : DigitalUtilityNodes.BLOCKS) block(name, true);
        block("quantum_link", true);
        block("quartz_fiber", true);
        block("spatial_anchor", true);
        DigitalSpatialAnchors.register();
        for (String name : new String[]{"cable_interface", "cable_energy_acceptor", "cable_pattern_provider"}) part(name);
        block("fluid_terminal", true);
        block("storage_bus", true);
        block("pattern_encoding_terminal", true);
        block("crafting_monitor", true);
        block("formation_plane", true); block("annihilation_plane", true);
        for (String kind : new String[]{"storage_monitor", "conversion_monitor", "monitor", "semi_dark_monitor", "dark_monitor", "pattern_access_terminal", "toggle_bus", "inverted_toggle_bus"}) block(kind, true);
        for (String kind : new String[]{"level_emitter", "energy_level_emitter", "fluid_import_bus", "fluid_export_bus", "fluid_storage_bus"}) block(kind, true);
        block("fluix_glass_cable", false);
        DigitalCabling.register();
        DigitalFacades.register();
        DigitalMaterials.register();
        DigitalFixtures.register();
        DigitalMeteorites.register();
        DigitalTinyTnt.register();
        DigitalPaint.register();
        DigitalUtilityStorage.register();
        DigitalQuantum.register();
        NODE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("jimta_digital_node"),
                FabricBlockEntityTypeBuilder.create(DigitalNode::new, BLOCKS.entrySet().stream()
                        .filter(e -> e.getValue() instanceof DigitalNodeBlock).map(Map.Entry::getValue).toArray(Block[]::new)).build());
        EnergyStorage.SIDED.registerForBlockEntity((node, side) -> node.poweredSource() ? node.energy : null, NODE);
        net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.registerForBlockEntity((node, side) ->
                node.kind().equals("condenser") ? node.condenserStorage :
                node.kind().equals("interface") || node.kind().equals("me_chest") ? new FluidCells.Port(node, null) : null, NODE);
        net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.registerForBlockEntity((node, side) -> {
            var actor = node.actor();
            if(node.kind().equals("condenser"))return actor!=null&&node.permitted(actor)?new DigitalCondenser.ItemPort(node):null;
            if(node.kind().equals("io_port"))return actor!=null&&node.permitted(actor)?new DigitalCellIO.Port(node,side):null;
            if (!node.kind().equals("interface") || actor == null || !node.permitted(actor)) return null;
            var buffer = net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(node, side);
            return new net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.item.ItemVariant>() {
                public long insert(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant resource, long amount, net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext tx) {
                    var current = node.actor(); return current != null && node.permitted(current) ? buffer.insert(resource, amount, tx) : 0;
                }
                public boolean supportsExtraction() { return false; }
                public long extract(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant resource, long amount, net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext tx) { return 0; }
                public java.util.Iterator<net.fabricmc.fabric.api.transfer.v1.storage.StorageView<net.fabricmc.fabric.api.transfer.v1.item.ItemVariant>> iterator() { return java.util.Collections.emptyIterator(); }
            };
        }, NODE);
        for (int size : new int[]{1, 4, 16, 64, 256}) {
            String name = "item_storage_cell_" + size + "k";
            var key = ResourceKey.create(Registries.ITEM, Jugcraft.id("jimta_" + name));
            ITEMS.put(name, Registry.register(BuiltInRegistries.ITEM, key,
                    new CellItem(new Item.Properties().setId(key).stacksTo(1), size * 1024 * 8)));
            String fluidName = "fluid_storage_cell_" + size + "k";
            var fluidKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("jimta_" + fluidName));
            ITEMS.put(fluidName, Registry.register(BuiltInRegistries.ITEM, fluidKey, new FluidCells.Cell(new Item.Properties().setId(fluidKey).stacksTo(1), size)));
        }
        for (String name : new String[]{"certus_quartz_crystal", "charged_certus_quartz_crystal", "certus_quartz_dust", "fluix_crystal", "fluix_dust", "logic_processor", "calculation_processor", "engineering_processor", "logic_processor_press", "calculation_processor_press", "engineering_processor_press", "cell_component_1k", "cell_component_4k", "cell_component_16k", "cell_component_64k", "cell_component_256k", "blank_pattern", "crafting_pattern"}) {
            var key = ResourceKey.create(Registries.ITEM, Jugcraft.id("jimta_" + name));
            ITEMS.put(name, Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key))));
        }
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Jugcraft.id("jimta_compact_part"), DigitalPartConversionRecipe.SERIALIZER);
        DigitalPatterns.register();
        var processingKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("jimta_processing_pattern"));
        ITEMS.put("processing_pattern", Registry.register(BuiltInRegistries.ITEM, processingKey, new Item(new Item.Properties().setId(processingKey))));
        for (String name : new String[]{"stonecutting_pattern", "smithing_table_pattern"}) {
            var key = ResourceKey.create(Registries.ITEM, Jugcraft.id("jimta_" + name)); ITEMS.put(name, Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key))));
        }
        DigitalProcessing.register();
        DigitalJobs.register();
        DigitalWorkshops.register();
        WirelessTerminal.register();
        for (String name : new String[]{"wireless_terminal", "wireless_crafting_terminal"}) {
            var key = ResourceKey.create(Registries.ITEM, Jugcraft.id("jimta_" + name));
            ITEMS.put(name, Registry.register(BuiltInRegistries.ITEM, key, new WirelessTerminal(new Item.Properties().setId(key).stacksTo(1), name.contains("crafting"))));
        }
        FluidCells.register();
        DigitalCreativeCell.register();
        DigitalTerminal.register();
        DigitalControls.register();
        DigitalEquipment.register();
        DigitalProduction.register();
        DigitalUpgrades.register();
        DigitalCellCards.register();
        DigitalLinks.register();
        DigitalCondenser.register();
        PortableCells.register();
        DigitalSpatial.register();
        DigitalParts.registerPorts();
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register((world,player,pos,state,entity)->{
            // This listener is also consulted by Authority. Check identity only here, never recursively call permitted().
            if(world.isClientSide())return true;
            for(var node:DigitalParts.at(world,pos)){
                var owner=node.ownerId();if(owner!=null&&!owner.equals(player.getUUID())&&!io.github.jimbozoomer.jugcraft.party.JugcraftParties.sameParty(owner,player.getUUID()))return false;
            }
            return true;
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out -> BLOCKS.values().forEach(out::accept));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(out -> ITEMS.values().forEach(out::accept));
    }
    private static void block(String name, boolean node) {
        var id = Jugcraft.id("jimta_" + name);
        var key = ResourceKey.create(Registries.BLOCK, id);
        var properties = BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2).noOcclusion().setId(key);
        boolean powered = DigitalNode.poweredKind(name);
        Block block = name.equals("quantum_link") ? new DigitalQuantum.Core(properties) : name.equals("crank") ? new DigitalUtilityNodes.Crank(properties) : name.equals("light_detector") ? new DigitalUtilityNodes.Detector(properties) : name.endsWith("_plane") ? new DigitalPlanes.PlaneBlock(properties,name) : node ? DigitalLinks.signal(name) ? new DigitalLinks.SignalBlock(properties, name) : DigitalControls.controls(name) ? new DigitalControls.Emitter(properties, name) : powered ? new DigitalNodeBlock.Powered(properties, name) : name.equals("interface") || name.equals("condenser") ? new DigitalNodeBlock.Interface(properties, name) : new DigitalNodeBlock(properties, name) : new DigitalCableBlock(properties);
        BLOCKS.put(name, Registry.register(BuiltInRegistries.BLOCK, key, block));
        var itemKey = ResourceKey.create(Registries.ITEM, id);
        var itemProperties=new Item.Properties().setId(itemKey).useBlockDescriptionPrefix();
        Registry.register(BuiltInRegistries.ITEM, itemKey, DigitalParts.supported(name)?new DigitalParts.PartItem(block,itemProperties):new BlockItem(block,itemProperties));
    }
    private static void part(String name) {
        var id = Jugcraft.id("jimta_" + name); var key = ResourceKey.create(Registries.BLOCK, id);
        var props = BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2).noOcclusion().setId(key); String kind = name.substring(6);
        Block block = kind.equals("energy_acceptor") ? new DigitalNodeBlock.Powered(props, kind) : kind.equals("interface") ? new DigitalNodeBlock.Interface(props, kind) : new DigitalNodeBlock(props, kind);
        DigitalStorage.BLOCKS.put(name, Registry.register(BuiltInRegistries.BLOCK, key, block));
        var ik = ResourceKey.create(Registries.ITEM, id);var properties=new Item.Properties().setId(ik).useBlockDescriptionPrefix();Registry.register(BuiltInRegistries.ITEM, ik, DigitalParts.supported(name)?new DigitalParts.PartItem(block,properties):new BlockItem(block,properties));
    }
    public static class CellItem extends Item {
        public final int capacity;
        public CellItem(Properties properties, int capacity) { super(properties); this.capacity = capacity; }
        @Override public boolean canFitInsideContainerItems() { return false; }
        @Override public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<net.minecraft.network.chat.Component> out, TooltipFlag flag) {
            var contents = stack.getOrDefault(CONTENTS, CellContents.EMPTY);
            out.accept(net.minecraft.network.chat.Component.literal(contents.amount() + " / " + capacity + " items · " + contents.entries().size() + " / 63 types"));
            out.accept(net.minecraft.network.chat.Component.literal("Stored items stay inside this cell when removed."));
        }
    }
}
