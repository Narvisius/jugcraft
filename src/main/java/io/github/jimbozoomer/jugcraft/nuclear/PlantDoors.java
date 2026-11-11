package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * The plant doors' shared parts (doors round): the block entity of every PlantDoorBlock, and plant_door_part, a block
 * never placed whose states are the models client/PlantDoorRenderer.java draws while a leaf swings (each kind of
 * leaf shut, with its hardware, and the closer's two arm links; tools/plant_doors.py PARTS, in the same order as
 * leafPart here; the Tri-Pillar's maintenance door's leaves last, tools/maintenance_door.py). Also the steel and glass
 * doors' other versions in the creative tab: with exit hardware, with a badge reader.
 */
public final class PlantDoors {
	/** How many part models there are (tools/plant_doors.py PART_COUNT). */
	public static final int PARTS = 36;
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, PARTS - 1);
	/** The closer's arm links: the steel door's main arm and forearm, the lead door's. */
	public static final int STEEL_ARM = 28, LEAD_ARM = 30;

	public static BlockEntityType<PlantDoorBlockEntity> ENTITY;
	public static Block PART_BLOCK;

	private PlantDoors() {
	}

	/** After PlantInterior and StorageBlocks (the doors are theirs). */
	public static void register() {
		ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("plant_door"),
				FabricBlockEntityTypeBuilder.create(PlantDoorBlockEntity::new, PlantInterior.BLOCKS.get("steel_door"),
						PlantInterior.BLOCKS.get("glass_door"), StorageBlocks.LEAD_DOOR, PlantInterior.BLOCKS.get("restroom_stall_door"),
						PlantInterior.BLOCKS.get("restroom_stall_door_accessible"),
						io.github.jimbozoomer.jugcraft.tower.JugcraftTower.MAINTENANCE_DOOR).build());
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id("plant_door_part"));
		PART_BLOCK = Registry.register(BuiltInRegistries.BLOCK, key, new PartBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
				.noOcclusion().noCollision().noLootTable().setId(key)));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output -> {
			for (String id : new String[] {"steel_door", "glass_door"}) {
				Block door = PlantInterior.BLOCKS.get(id);
				output.accept(version(door, id, true, false));
				output.accept(version(door, id, false, true));
				output.accept(version(door, id, true, true));
			}
		});
	}

	/** A door item that places the door with exit hardware and / or a badge reader. */
	public static ItemStack version(Block door, String id, boolean exit, boolean reader) {
		ItemStack stack = new ItemStack(door);
		BlockItemStateProperties props = BlockItemStateProperties.EMPTY;
		if (exit) {
			props = props.with(PlantDoorBlock.EXIT, true);
		}
		if (reader) {
			props = props.with(PlantDoorBlock.READER, true);
		}
		stack.set(DataComponents.BLOCK_STATE, props);
		stack.set(DataComponents.ITEM_NAME, Component.translatable("block.jugcraft." + id + (exit ? "_exit" : "") + (reader ? "_reader" : "")));
		return stack;
	}

	/** The part model of a door's leaf (shut, hinge as the door's): see tools/plant_doors.py part_index. */
	public static int leafPart(BlockState state) {
		int hinge = state.getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT ? 0 : 1;
		int paired = state.getValue(PlantDoorBlock.PAIRED) ? 1 : 0;
		Block block = state.getBlock();
		if (block instanceof RestroomBlocks.StallDoor) {
			int latched = state.getValue(RestroomBlocks.LATCHED) ? 1 : 0;
			int accessible = block == PlantInterior.BLOCKS.get("restroom_stall_door_accessible") ? 1 : 0;
			return 20 + hinge * 4 + latched * 2 + accessible;
		}
		if (block instanceof LeadDoorBlock) {
			return 16 + hinge * 2 + paired;
		}
		if (block instanceof io.github.jimbozoomer.jugcraft.tower.MaintenanceDoorBlock) {
			return 32 + hinge * 2 + paired;
		}
		int exit = state.getValue(PlantDoorBlock.EXIT) ? 1 : 0;
		int base = block == PlantInterior.BLOCKS.get("glass_door") ? 8 : 0;
		return base + hinge * 4 + paired * 2 + exit;
	}

	/** A block that is never placed: its states name the doors' part models. */
	public static class PartBlock extends Block {
		public PartBlock(Properties properties) {
			super(properties);
			registerDefaultState(stateDefinition.any().setValue(PART, 0));
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			builder.add(PART);
		}
	}
}
