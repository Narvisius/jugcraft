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
			return packager == null && (station == null || station.tower() == null) ? null : station.cargoStorage();
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
		bind(level, core, ENERGY, core.getBlockPos().offset(masterOffset(ENERGY, core.layoutVersion())), facing(ENERGY, core.layoutVersion()));
		bind(level, core, CARGO, core.getBlockPos().offset(masterOffset(CARGO, core.layoutVersion())), facing(CARGO, core.layoutVersion()));
	}

	/** The way a tower's station faces (its port's side): out of the building's intake face, east, from layout 37. */
	public static Direction facing(ExchangeStationBlock block, int version) {
		return version >= TowerData.TRIPILLAR_STATIONS_LAYOUT || !block.energy ? Direction.EAST : Direction.WEST;
	}

	/** Wider fronts keep their marked external connection centred at the original world position. */
	public static net.minecraft.core.Vec3i masterOffset(ExchangeStationBlock block, boolean large) {
		return block.energy ? new net.minecraft.core.Vec3i(-22, 1, large ? 4 : 2)
				: new net.minecraft.core.Vec3i(20, 1, large ? -4 : -2);
	}

	/**
	 * The station's master cell for a tower of layout {@code version}. The Tri-Pillar tower (tools/tower_tripillar.py)
	 * moved the same formed stations out beside its supply yard, east of the control building, within reach of the
	 * depot terminal; from layout 37 they stand inside, in the building's east point (tools/tripillar_stations.py).
	 */
	public static net.minecraft.core.Vec3i masterOffset(ExchangeStationBlock block, int version) {
		if (version >= TowerData.TRIPILLAR_STATIONS_LAYOUT) {
			// in the east point, side by side behind the intake face (tools/tripillar_stations.MASTERS)
			return block.energy ? new net.minecraft.core.Vec3i(45, 1, -9) : new net.minecraft.core.Vec3i(45, 1, 1);
		}
		if (version >= TowerData.TRIPILLAR_LAYOUT) {
			return block.energy ? new net.minecraft.core.Vec3i(56, 1, -26) : new net.minecraft.core.Vec3i(58, 1, -4);
		}
		return masterOffset(block, version >= TowerData.LARGE_INPUT_LAYOUT);
	}

	private static void bind(ServerLevel level, TowerCoreBlockEntity core, ExchangeStationBlock block, BlockPos pos, Direction facing) {
		if (level.isLoaded(pos) && level.getBlockState(pos).is(block)
				&& level.getBlockState(pos).getValue(ExchangeStationBlock.FACING) == facing
				&& level.getBlockEntity(pos) instanceof ExchangeStationBlockEntity station) {
			station.bind(core);
		}
	}
}
