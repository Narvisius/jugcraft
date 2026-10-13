package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

/** The two new complete MBS. Legacy exchange-port registrations remain unchanged. */
public final class ExchangeStations {
	public static ExchangeStationBlock ENERGY;
	public static ExchangeStationBlock CARGO;
	public static BlockEntityType<ExchangeStationBlockEntity> ENTITY;

	private ExchangeStations() { }

	static void register() {
		ENERGY = register("energy_exchange", true);
		CARGO = register("cargo_exchange", false);
		ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("exchange_station"),
				FabricBlockEntityTypeBuilder.create(ExchangeStationBlockEntity::new, ENERGY, CARGO).build());
		EnergyStorage.SIDED.registerForBlocks((level, pos, state, entity, side) -> {
			if (!ENERGY.isPort(state, side)) return null;
			ExchangeStationBlockEntity station = ENERGY.controller(level, pos, state);
			var terminal = station == null ? null : station.terminal();
			return terminal == null ? null : station.energyStorage();
		}, ENERGY);
		ItemStorage.SIDED.registerForBlocks((level, pos, state, entity, side) -> {
			if (!CARGO.isPort(state, side)) return null;
			ExchangeStationBlockEntity station = CARGO.controller(level, pos, state);
			var terminal = station == null ? null : station.terminal();
			var packager = terminal == null ? null : terminal.packagerEntity();
			return packager == null ? null : station.cargoStorage();
		}, CARGO);
	}

	private static ExchangeStationBlock register(String id, boolean energy) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(id));
		ExchangeStationBlock block = Registry.register(BuiltInRegistries.BLOCK, key,
				new ExchangeStationBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(key)
						.strength(4, 12).noOcclusion().pushReaction(PushReaction.IMMOVEABLE), energy));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block,
				new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		JugcraftTower.BLOCKS.put(id, block);
		return block;
	}

	/** Called only after tier 1 is fully present; never writes/replaces world geometry. */
	public static void bindBuilt(ServerLevel level, TowerCoreBlockEntity core) {
		if (core.layoutVersion() < 2) return;
		boolean large = core.layoutVersion() >= 5;
		bind(level, core, ENERGY, core.getBlockPos().offset(masterOffset(ENERGY, large)), Direction.WEST);
		bind(level, core, CARGO, core.getBlockPos().offset(masterOffset(CARGO, large)), Direction.EAST);
	}

	/** Wider fronts keep their marked external connection centred at the original world position. */
	public static net.minecraft.core.Vec3i masterOffset(ExchangeStationBlock block, boolean large) {
		return block.energy ? new net.minecraft.core.Vec3i(-22, 1, large ? 4 : 2)
				: new net.minecraft.core.Vec3i(20, 1, large ? -4 : -2);
	}

	private static void bind(ServerLevel level, TowerCoreBlockEntity core, ExchangeStationBlock block, BlockPos pos, Direction facing) {
		if (level.isLoaded(pos) && level.getBlockState(pos).is(block)
				&& level.getBlockState(pos).getValue(ExchangeStationBlock.FACING) == facing
				&& level.getBlockEntity(pos) instanceof ExchangeStationBlockEntity station) {
			station.bind(core);
		}
	}
}
