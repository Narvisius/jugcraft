package io.github.jimbozoomer.jugcraft.vehiclebay;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class VehicleBays {
    public static VehicleBayBlock MECHA, TANK;
    public static BlockEntityType<VehicleBayBlockEntity> ENTITY;
    public static ExtendedMenuType<VehicleBayMenu, BlockPos> MENU;
    public static void register() {
        MECHA = block("mecha_workbench", 6);
        TANK = block("tank_workbench", 3);
        ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("vehicle_bay"),
            FabricBlockEntityTypeBuilder.create(VehicleBayBlockEntity::new, MECHA, TANK).build());
        MENU = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("vehicle_bay"),
            new ExtendedMenuType<>((id, inv, pos) -> new VehicleBayMenu(id, inv), BlockPos.STREAM_CODEC.cast()));
        EnergyStorage.SIDED.registerForBlocks((level, pos, state, entity, side) -> {
            var block = (VehicleBayBlock) state.getBlock();
            if (!block.acceptsPower(state, side)) return null;
            var at = block.masterPos(pos, state);
            return level.isLoaded(at) && level.getBlockEntity(at) instanceof VehicleBayBlockEntity bay ? bay.energy : null;
        }, MECHA, TANK);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> { out.accept(MECHA); out.accept(TANK); });
    }
    private static VehicleBayBlock block(String id, int height) {
        var key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(id));
        var block = Registry.register(BuiltInRegistries.BLOCK, key, new VehicleBayBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(4).noOcclusion().setId(key), height));
        var itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
        Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
        return block;
    }
    private VehicleBays() { }
}
