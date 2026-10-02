package io.github.jimbozoomer.jugcraft.solar;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Solar power, second tier (batch 21): the solar tracker, a panel that follows the sun across the sky, and solar
 * thermal power: a field of heliostat mirrors focusing sunlight on a solar receiver, which boils water for power.
 */
public final class JugcraftSolar {
	public static Block SOLAR_TRACKER;
	public static Block HELIOSTAT;
	public static Block SOLAR_RECEIVER;
	public static BlockEntityType<SolarTrackerBlockEntity> SOLAR_TRACKER_ENTITY;
	public static BlockEntityType<HeliostatBlockEntity> HELIOSTAT_ENTITY;
	public static BlockEntityType<SolarReceiverBlockEntity> SOLAR_RECEIVER_ENTITY;

	private JugcraftSolar() {
	}

	private static Block block(String name, Block copy, Function<BlockBehaviour.Properties, Block> factory) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id(name));
		Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey,
				factory.apply(BlockBehaviour.Properties.ofFullCopy(copy).setId(blockKey).noOcclusion()));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(name));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		return block;
	}

	public static void register() {
		SOLAR_TRACKER = block("solar_tracker", Blocks.IRON_BLOCK, SolarTrackerBlock::new);
		HELIOSTAT = block("heliostat", Blocks.IRON_BLOCK, HeliostatBlock::new);
		SOLAR_RECEIVER = block("solar_receiver", Blocks.IRON_BLOCK, SolarReceiverBlock::new);
		SOLAR_TRACKER_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("solar_tracker"),
				FabricBlockEntityTypeBuilder.create(SolarTrackerBlockEntity::new, SOLAR_TRACKER).build());
		HELIOSTAT_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("heliostat"),
				FabricBlockEntityTypeBuilder.create(HeliostatBlockEntity::new, HELIOSTAT).build());
		SOLAR_RECEIVER_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("solar_receiver"),
				FabricBlockEntityTypeBuilder.create(SolarReceiverBlockEntity::new, SOLAR_RECEIVER).build());
		EnergyStorage.SIDED.registerForBlockEntity((tracker, side) -> tracker.energy, SOLAR_TRACKER_ENTITY);
		EnergyStorage.SIDED.registerForBlockEntity((receiver, side) -> receiver.energy, SOLAR_RECEIVER_ENTITY);
		FluidStorage.SIDED.registerForBlockEntity((receiver, side) -> receiver.water, SOLAR_RECEIVER_ENTITY);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> {
			output.accept(SOLAR_TRACKER);
			output.accept(HELIOSTAT);
			output.accept(SOLAR_RECEIVER);
		});
	}
}
