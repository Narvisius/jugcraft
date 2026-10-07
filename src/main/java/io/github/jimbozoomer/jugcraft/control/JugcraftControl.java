package io.github.jimbozoomer.jugcraft.control;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
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

/**
 * Control electronics (batch 36, docs/features/control-electronics.md): data cables, sensors, relays and the logic
 * controller; batch 37 adds the control monitor, the alarm and the remote. Numbers and data: tools/control_electronics.py.
 */
public final class JugcraftControl {
	public static Block DATA_CABLE;
	public static Block SENSOR;
	public static Block RELAY;
	public static Block LOGIC_CONTROLLER;
	public static BlockEntityType<LogicControllerBlockEntity> CONTROLLER_ENTITY;
	public static ExtendedMenuType<LogicControllerMenu, BlockPos> CONTROLLER_MENU;
	/** Batch 37: the control room. */
	public static Block CONTROL_MONITOR;
	public static Block ALARM;
	public static Item CONTROL_REMOTE;
	public static BlockEntityType<ControlMonitorBlockEntity> MONITOR_ENTITY;
	public static DataComponentType<GlobalPos> REMOTE_LINK;
	public static DataComponentType<Integer> REMOTE_CHANNEL;

	private JugcraftControl() {
	}

	public static void register() {
		DATA_CABLE = block("data_cable", DataCableBlock::new,
				BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.WOOL).noOcclusion());
		SENSOR = block("sensor", SensorBlock::new,
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.0F).noOcclusion());
		RELAY = block("relay", RelayBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.5F));
		LOGIC_CONTROLLER = block("logic_controller", LogicControllerBlock::new,
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F).noOcclusion());
		CONTROLLER_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("logic_controller"),
				FabricBlockEntityTypeBuilder.create(LogicControllerBlockEntity::new, LOGIC_CONTROLLER).build());
		CONTROLLER_MENU = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("logic_controller"),
				new ExtendedMenuType<>(LogicControllerMenu::new, BlockPos.STREAM_CODEC));
		CONTROL_MONITOR = block("control_monitor", ControlMonitorBlock::new,
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.5F).noOcclusion().lightLevel(state -> 6));
		ALARM = block("alarm", AlarmBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.0F).noOcclusion()
				.lightLevel(state -> state.getValue(AlarmBlock.POWERED) ? 12 : 0));
		MONITOR_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("control_monitor"),
				FabricBlockEntityTypeBuilder.create(ControlMonitorBlockEntity::new, CONTROL_MONITOR).build());
		REMOTE_LINK = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("remote_link"),
				DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build());
		REMOTE_CHANNEL = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("remote_channel"),
				DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());
		ResourceKey<Item> remoteKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("control_remote"));
		CONTROL_REMOTE = Registry.register(BuiltInRegistries.ITEM, remoteKey,
				new ControlRemoteItem(new Item.Properties().setId(remoteKey).stacksTo(1)));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> {
			output.accept(DATA_CABLE);
			output.accept(SENSOR);
			output.accept(RELAY);
			output.accept(LOGIC_CONTROLLER);
			output.accept(CONTROL_MONITOR);
			output.accept(ALARM);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> output.accept(CONTROL_REMOTE));
	}

	private static Block block(String path, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(path));
		Block block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(path));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		return block;
	}
}
