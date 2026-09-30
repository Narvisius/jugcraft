package io.github.jimbozoomer.jugcraft.tools;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Powered tools, the first dieselpunk gear (see docs/ART_DIRECTION.md): the mining drill, the chainsaw
 * and the rocket pack, which hold JE in an item component instead of wearing out, and the charging
 * station that fills them from cables.
 */
public final class JugcraftTools {
	public static final long DRILL_CAPACITY = 100_000;
	public static final long DRILL_ENERGY_PER_BLOCK = 60;
	public static final long CHAINSAW_CAPACITY = 100_000;
	public static final long CHAINSAW_ENERGY_PER_BLOCK = 40;
	/** Blocks the drill mines fast: everything a pickaxe or shovel mines (data/jugcraft/tags/block/mineable/drill.json). */
	public static final TagKey<Block> MINEABLE_WITH_DRILL = TagKey.create(Registries.BLOCK, Jugcraft.id("mineable/drill"));
	/** Blocks the chainsaw cuts fast: everything an axe cuts, plus leaves. */
	public static final TagKey<Block> MINEABLE_WITH_CHAINSAW = TagKey.create(Registries.BLOCK, Jugcraft.id("mineable/chainsaw"));
	/** Diamond-tier drops, faster than netherite. Durability is unused: the tools are unbreakable and run on JE. */
	private static final ToolMaterial POWERED = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1, 14.0F, 3.0F, 10,
			TagKey.create(Registries.ITEM, Jugcraft.id("repairs_powered_tools")));

	public static DataComponentType<Long> ENERGY;
	public static DataComponentType<Integer> DRILL_MODE;
	public static Item MINING_DRILL;
	public static Item CHAINSAW;
	public static Item ROCKET_PACK;
	public static Block CHARGING_STATION;
	public static BlockEntityType<ChargingStationBlockEntity> CHARGING_STATION_ENTITY;

	private JugcraftTools() {
	}

	public static void register() {
		ENERGY = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("energy"),
				DataComponentType.<Long>builder().persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG).build());
		DRILL_MODE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("drill_mode"),
				DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

		MINING_DRILL = item("mining_drill", properties -> new MiningDrillItem(powered(properties)
				.tool(POWERED, MINEABLE_WITH_DRILL, 3.0F, -2.8F, 0.0F).component(DRILL_MODE, MiningDrillItem.SINGLE),
				DRILL_CAPACITY, DRILL_ENERGY_PER_BLOCK));
		CHAINSAW = item("chainsaw", properties -> new ChainsawItem(powered(properties)
				.tool(POWERED, MINEABLE_WITH_CHAINSAW, 6.0F, -3.0F, 5.0F), CHAINSAW_CAPACITY, CHAINSAW_ENERGY_PER_BLOCK));
		ROCKET_PACK = item("rocket_pack", properties -> new RocketPackItem(properties.stacksTo(1).rarity(Rarity.UNCOMMON)
				.component(ENERGY, 0L)
				.component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST)
						.setEquipSound(SoundEvents.ARMOR_EQUIP_IRON)
						.setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, Jugcraft.id("rocket_pack"))).build())));

		ResourceKey<Block> stationKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("charging_station"));
		CHARGING_STATION = Registry.register(BuiltInRegistries.BLOCK, stationKey, new ChargingStationBlock(
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F).sound(SoundType.METAL).noOcclusion()
						.lightLevel(state -> state.getValue(ChargingStationBlock.LIT) ? 7 : 0).setId(stationKey)));
		ResourceKey<Item> stationItem = ResourceKey.create(Registries.ITEM, Jugcraft.id("charging_station"));
		Registry.register(BuiltInRegistries.ITEM, stationItem,
				new BlockItem(CHARGING_STATION, new Item.Properties().setId(stationItem).useBlockDescriptionPrefix()));
		CHARGING_STATION_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("charging_station"),
				FabricBlockEntityTypeBuilder.create(ChargingStationBlockEntity::new, CHARGING_STATION).build());
		// Cables reach the station through either half.
		EnergyStorage.SIDED.registerForBlocks((level, pos, state, entity, side) ->
				level.getBlockEntity(ChargingStationBlock.lowerPos(state, pos)) instanceof ChargingStationBlockEntity station
						? station.energy : null, CHARGING_STATION);

		PayloadTypeRegistry.serverboundPlay().register(RocketThrustPayload.TYPE, RocketThrustPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(RocketThrustPayload.TYPE, (payload, context) -> RocketPackItem.thrust(context.player()));

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			for (Item item : new Item[] {MINING_DRILL, CHAINSAW, ROCKET_PACK}) {
				output.accept(item);
				ItemStack full = new ItemStack(item);
				Chargeable.setEnergy(full, ((Chargeable) item).capacity());
				output.accept(full);
			}
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(CHARGING_STATION));
	}

	/** One of a kind, unbreakable (they run on JE, not durability), starting empty. */
	private static Item.Properties powered(Item.Properties properties) {
		return properties.stacksTo(1).rarity(Rarity.UNCOMMON).component(ENERGY, 0L)
				.component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
				.component(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.UNBREAKABLE, true));
	}

	private static Item item(String name, Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
	}
}
