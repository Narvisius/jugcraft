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
import net.minecraft.core.component.DataComponentType;
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
	public static Block STEEL_FLUID_PIPE;
	/** A steel pipe segment that a redstone signal closes (batch 8). */
	public static Block FLUID_VALVE;
	/** A steel pipe segment that only lets one fluid out into what it touches (batch 8). */
	public static Block FLUID_FILTER;
	public static Block HEAVY_PUMP;
	public static Block FLUID_TANK;
	public static Block ELECTRIC_PUMP;
	/** Batch 20: a borosilicate glass tank (it shows what it holds) and the tank gauge. */
	public static Block GLASS_TANK;
	public static Block TANK_GAUGE;
	public static BlockEntityType<FluidTankBlockEntity> TANK_ENTITY;
	public static BlockEntityType<ElectricPumpBlockEntity> PUMP_ENTITY;
	public static BlockEntityType<FluidFilterBlockEntity> FILTER_ENTITY;
	/** The fluid a broken tank carries as an item (batch 10; see {@link StoredFluid}). */
	public static DataComponentType<StoredFluid> STORED_FLUID;
	/** Batch 35: carries 8 buckets of one gas (see {@link GasCylinderItem}). */
	public static Item GAS_CYLINDER;

	private JugcraftFluids() {
	}

	public static void register() {
		STORED_FLUID = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("stored_fluid"),
				DataComponentType.<StoredFluid>builder().persistent(StoredFluid.CODEC)
						.networkSynchronized(StoredFluid.STREAM_CODEC).build());
		BRONZE_FLUID_PIPE = block("bronze_fluid_pipe", new FluidPipeBlock(properties("bronze_fluid_pipe",
				BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1.0F).sound(SoundType.COPPER).noOcclusion()),
				FluidPipeBlock.BRONZE_RATE_MB));
		STEEL_FLUID_PIPE = block("steel_fluid_pipe", new FluidPipeBlock(properties("steel_fluid_pipe",
				BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL).noOcclusion()),
				FluidPipeBlock.STEEL_RATE_MB));
		FLUID_VALVE = block("fluid_valve", new FluidValveBlock(properties("fluid_valve",
				BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL).noOcclusion()),
				FluidPipeBlock.STEEL_RATE_MB));
		FLUID_FILTER = block("fluid_filter", new FluidFilterBlock(properties("fluid_filter",
				BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL).noOcclusion()),
				FluidPipeBlock.STEEL_RATE_MB));
		FLUID_TANK = block("fluid_tank", new FluidTankBlock(properties("fluid_tank",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F).noOcclusion())));
		GLASS_TANK = block("glass_tank", new FluidTankBlock(properties("glass_tank",
				BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(1.5F).noOcclusion())));
		TANK_GAUGE = block("tank_gauge", new TankGaugeBlock(properties("tank_gauge",
				BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0F).sound(SoundType.METAL).noOcclusion())));
		ELECTRIC_PUMP = block("electric_pump", new ElectricPumpBlock(properties("electric_pump",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F).noOcclusion()), ElectricPumpBlockEntity.Tier.ELECTRIC));
		HEAVY_PUMP = block("heavy_pump", new ElectricPumpBlock(properties("heavy_pump",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(4.0F).noOcclusion()), ElectricPumpBlockEntity.Tier.HEAVY));

		TANK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("fluid_tank"),
				FabricBlockEntityTypeBuilder.create(FluidTankBlockEntity::new, FLUID_TANK, GLASS_TANK).build());
		PUMP_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("electric_pump"),
				FabricBlockEntityTypeBuilder.create(ElectricPumpBlockEntity::new, ELECTRIC_PUMP, HEAVY_PUMP).build());

		FILTER_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("fluid_filter"),
				FabricBlockEntityTypeBuilder.create(FluidFilterBlockEntity::new, FLUID_FILTER).build());

		FluidStorage.SIDED.registerForBlockEntity((tank, side) -> TankGroup.at(tank.getLevel(), tank.getBlockPos()), TANK_ENTITY);
		FluidStorage.SIDED.registerForBlockEntity(ElectricPumpBlockEntity::fluidFor, PUMP_ENTITY);
		FluidStorage.SIDED.registerForBlocks((level, pos, state, entity, side) -> {
			MachineBlockEntity machine = MachineBlock.machineAt(level, pos, state);
			return machine == null ? null : machine.fluidFor(side);
		}, JugcraftMachines.MACHINES.values().toArray(Block[]::new));
		EnergyStorage.SIDED.registerForBlockEntity((pump, side) -> pump.energy(), PUMP_ENTITY);

		ResourceKey<Item> cylinderKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("gas_cylinder"));
		GAS_CYLINDER = Registry.register(BuiltInRegistries.ITEM, cylinderKey,
				new GasCylinderItem(new Item.Properties().setId(cylinderKey).stacksTo(1)));
		FluidStorage.ITEM.registerForItems((stack, context) -> GasCylinderItem.storage(context), GAS_CYLINDER);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> output.accept(GAS_CYLINDER));

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> {
			output.accept(BRONZE_FLUID_PIPE);
			output.accept(STEEL_FLUID_PIPE);
			output.accept(FLUID_VALVE);
			output.accept(FLUID_FILTER);
			output.accept(FLUID_TANK);
			output.accept(GLASS_TANK);
			output.accept(TANK_GAUGE);
			output.accept(ELECTRIC_PUMP);
			output.accept(HEAVY_PUMP);
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
