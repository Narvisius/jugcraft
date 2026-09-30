package io.github.jimbozoomer.jugcraft.kinetic;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
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
 * Kinetic power (see {@link KineticNetworks}): shafts and gearboxes carry rotation from the hand crank
 * and steam engine to machines, which run on it directly, or to a dynamo, which turns it into JE.
 */
public final class JugcraftKinetics {
	public static Block IRON_SHAFT;
	public static Block BRASS_GEARBOX;
	public static Block HAND_CRANK;
	public static Block STEAM_ENGINE;
	public static Block DYNAMO;
	public static BlockEntityType<HandCrankBlockEntity> HAND_CRANK_ENTITY;
	public static BlockEntityType<SteamEngineBlockEntity> STEAM_ENGINE_ENTITY;
	public static BlockEntityType<DynamoBlockEntity> DYNAMO_ENTITY;

	private JugcraftKinetics() {
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
		IRON_SHAFT = block("iron_shaft", Blocks.IRON_BARS, ShaftBlock::new);
		BRASS_GEARBOX = block("brass_gearbox", Blocks.IRON_BLOCK, GearboxBlock::new);
		HAND_CRANK = block("hand_crank", Blocks.OAK_PLANKS, HandCrankBlock::new);
		STEAM_ENGINE = block("steam_engine", Blocks.IRON_BLOCK, SteamEngineBlock::new);
		DYNAMO = block("dynamo", Blocks.IRON_BLOCK, DynamoBlock::new);
		HAND_CRANK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("hand_crank"),
				FabricBlockEntityTypeBuilder.create(HandCrankBlockEntity::new, HAND_CRANK).build());
		STEAM_ENGINE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("steam_engine"),
				FabricBlockEntityTypeBuilder.create(SteamEngineBlockEntity::new, STEAM_ENGINE).build());
		DYNAMO_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("dynamo"),
				FabricBlockEntityTypeBuilder.create(DynamoBlockEntity::new, DYNAMO).build());
		ItemStorage.SIDED.registerForBlockEntity((engine, side) -> engine.fuel, STEAM_ENGINE_ENTITY);
		FluidStorage.SIDED.registerForBlockEntity((engine, side) -> engine.waterInlet, STEAM_ENGINE_ENTITY);
		EnergyStorage.SIDED.registerForBlockEntity((dynamo, side) -> dynamo.energy, DYNAMO_ENTITY);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> {
			output.accept(IRON_SHAFT);
			output.accept(BRASS_GEARBOX);
			output.accept(HAND_CRANK);
			output.accept(STEAM_ENGINE);
			output.accept(DYNAMO);
		});
	}
}
