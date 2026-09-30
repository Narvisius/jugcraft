package io.github.jimbozoomer.jugcraft.fluid;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
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
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Registers the physical fluid branch: bronze pipes, the tinplate tank and the electric pump,
 * and exposes them (plus the steam generator's water tank) through Fabric's
 * {@link FluidStorage#SIDED} lookup so other mods' pipes and tanks work with them too.
 * Must run after {@link JugcraftMachines#register()}.
 */
public final class JugcraftFluids {
	public static Block BRONZE_FLUID_PIPE;
	public static Block FLUID_TANK;
	public static Block ELECTRIC_PUMP;
	public static BlockEntityType<FluidTankBlockEntity> TANK_ENTITY;
	public static BlockEntityType<ElectricPumpBlockEntity> PUMP_ENTITY;

	private JugcraftFluids() {
	}

	public static void register() {
		BRONZE_FLUID_PIPE = block("bronze_fluid_pipe", new FluidPipeBlock(properties("bronze_fluid_pipe",
				BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1.0F).sound(SoundType.COPPER).noOcclusion())));
		FLUID_TANK = block("fluid_tank", new FluidTankBlock(properties("fluid_tank",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F).noOcclusion())));
		ELECTRIC_PUMP = block("electric_pump", new ElectricPumpBlock(properties("electric_pump",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F).noOcclusion())));

		TANK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("fluid_tank"),
				FabricBlockEntityTypeBuilder.create(FluidTankBlockEntity::new, FLUID_TANK).build());
		PUMP_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("electric_pump"),
				FabricBlockEntityTypeBuilder.create(ElectricPumpBlockEntity::new, ELECTRIC_PUMP).build());

		FluidStorage.SIDED.registerForBlockEntity((tank, side) -> tank.storage, TANK_ENTITY);
		FluidStorage.SIDED.registerForBlockEntity(ElectricPumpBlockEntity::fluidFor, PUMP_ENTITY);
		FluidStorage.SIDED.registerForBlocks((level, pos, state, entity, side) -> {
			MachineBlockEntity machine = MachineBlock.machineAt(level, pos, state);
			return machine == null ? null : machine.fluidFor(side);
		}, JugcraftMachines.MACHINES.values().toArray(Block[]::new));
		EnergyStorage.SIDED.registerForBlockEntity((pump, side) -> pump.energy(), PUMP_ENTITY);

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> {
			output.accept(BRONZE_FLUID_PIPE);
			output.accept(FLUID_TANK);
			output.accept(ELECTRIC_PUMP);
		});
	}

	private static BlockBehaviour.Properties properties(String path, BlockBehaviour.Properties properties) {
		return properties.setId(ResourceKey.create(Registries.BLOCK, Jugcraft.id(path)));
	}

	private static Block block(String path, Block block) {
		Registry.register(BuiltInRegistries.BLOCK, Jugcraft.id(path), block);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(path));
		Registry.register(BuiltInRegistries.ITEM, itemKey,
				new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		return block;
	}
}
