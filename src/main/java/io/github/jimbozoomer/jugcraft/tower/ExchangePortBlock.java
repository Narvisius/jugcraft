package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.drone.CargoPackagerBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneDepots;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.energy.EnergyConnectable;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The Drone Tower's exchange ports, built with tier 1 on opposite edges of the landing field. The Energy
 * Exchange Port (in the substation, west) takes power from any cable or generator touching it into the
 * nearest depot's terminal; the Cargo Exchange Port (at the loading dock, east) takes items from pipes,
 * conveyors and hoppers into the depot's cargo packager, which every supply pickup draws from. Both work
 * for any depot within {@link #RANGE} blocks, so they can also be placed by hand elsewhere.
 */
public class ExchangePortBlock extends Block implements EnergyConnectable {
	public static final int RANGE = 48;
	public static Block ENERGY;
	public static Block CARGO;

	public ExchangePortBlock(Properties properties) {
		super(properties);
	}

	static void register() {
		ENERGY = block("energy_exchange_port", 4);
		CARGO = block("cargo_exchange_port", 0);
		EnergyStorage.SIDED.registerForBlocks((level, pos, state, entity, side) -> {
			DroneTerminalBlockEntity terminal = DroneDepots.nearest(level, pos, RANGE);
			return terminal == null ? null : terminal.energy();
		}, ENERGY);
		ItemStorage.SIDED.registerForBlocks((level, pos, state, entity, side) -> {
			DroneTerminalBlockEntity terminal = DroneDepots.nearest(level, pos, RANGE);
			CargoPackagerBlockEntity packager = terminal == null ? null : terminal.packagerEntity();
			return packager == null ? null : ContainerStorage.of(packager, null);
		}, CARGO);
	}

	private static Block block(String id, int light) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(id));
		Block block = Registry.register(BuiltInRegistries.BLOCK, key, new ExchangePortBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
				.setId(key).strength(4.0F, 12F).lightLevel(state -> light)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		JugcraftTower.BLOCKS.put(id, block);
		return block;
	}
}
