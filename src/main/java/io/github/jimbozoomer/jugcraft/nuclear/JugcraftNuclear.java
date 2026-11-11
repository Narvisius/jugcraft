package io.github.jimbozoomer.jugcraft.nuclear;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Nuclear power (docs/features/nuclear.md), stage 1: the shared radiation system, the hazmat suit, the Geiger counter,
 * shielding concrete; the fuel (enriched uranium, fuel assemblies, spent fuel; the Gas Centrifuge Cascade and Fuel
 * Fabricator are machines); the reactor unit's working equipment (placed whole, drawn by the client), the three
 * nuclear pipes, the link card and the console's network action. IDs match tools/nuclear.py.
 */
public final class JugcraftNuclear {
	public static final String[] HAZMAT = {"hazmat_hood", "hazmat_coverall", "hazmat_trousers", "hazmat_boots"};
	public static AttachmentType<RadiationState> RADIATION;
	public static DataComponentType<Float> RADIOACTIVITY;
	public static final TagKey<Block> SHIELDING = TagKey.create(Registries.BLOCK, Jugcraft.id("radiation_shielding"));
	public static final Item[] HAZMAT_ITEMS = new Item[4];
	public static Item GEIGER_COUNTER;
	public static Item CHECK_SOURCE;
	public static Block SHIELDING_CONCRETE;
	/** The Geiger counter's sound: made live on the client from the dose rate (client/GeigerSound.java), no recording. */
	public static SoundEvent GEIGER;
	/** The emergency diesel generator's air start, its running loop (client/DieselSounds.java) and its stop. */
	public static SoundEvent EDG_START, EDG_RUN, EDG_STOP, CO2_HORN, CO2_DISCHARGE;
	// Fuel.
	public static Item ENRICHED_URANIUM;
	public static Item FUEL_ASSEMBLY;
	public static Item SPENT_FUEL;
	public static Item LINK_CARD;
	/** How used up a spent assembly is (1 = spent, 2 = damaged in an overheat). */
	public static DataComponentType<Float> BURNUP;
	/** The vessel a link card is set to. */
	public static DataComponentType<net.minecraft.core.BlockPos> LINKED_VESSEL;
	// Equipment and pipes.
	public static Block FRAME;
	public static final java.util.Map<EquipmentKind, Block> EQUIPMENT = new java.util.EnumMap<>(EquipmentKind.class);
	public static final java.util.Map<EquipmentKind, Item> EQUIPMENT_ITEMS = new java.util.EnumMap<>(EquipmentKind.class);
	public static final java.util.Map<PipeKind, Block> PIPES = new java.util.EnumMap<>(PipeKind.class);
	public static net.minecraft.world.level.block.entity.BlockEntityType<EquipmentBlockEntity> EQUIPMENT_ENTITY;
	// Cooling towers.
	public static Block COOLING_TOWER;
	/** The fill packs inside a cooling tower, and the buried conduit. */
	public static Block COOLING_FILL;
	public static Block CONDUIT_TRENCH;
	public static net.minecraft.world.level.block.entity.BlockEntityType<CoolingTowerBlockEntity> COOLING_TOWER_ENTITY;
	/** The pump houses' controllers (towers and pump houses build: WaterWorksBlock). */
	public static Block WATER_WORKS;
	public static net.minecraft.world.level.block.entity.BlockEntityType<WaterWorksBlockEntity> WATER_WORKS_ENTITY;
	/** The transformer yard's and the switchyard's controllers (transformer yard and switchyard build: YardBlock). */
	public static Block YARD;
	public static net.minecraft.world.level.block.entity.BlockEntityType<YardBlockEntity> YARD_ENTITY;
	/** The reactor building's controller (its inside, a compiled site: ReactorBuildingBlock). */
	public static Block REACTOR_BUILDING;
	public static net.minecraft.world.level.block.entity.BlockEntityType<ReactorBuildingBlockEntity> REACTOR_BUILDING_ENTITY;
	/** The yards' sounds: a breaker operating, a disconnect's arc, the transformers' hum, the deluge's water-motor gong. */
	public static SoundEvent YARD_BREAKER, YARD_ARC, YARD_HUM, YARD_GONG;
	/** A cooling tower's steam plume (always drawn, however far: the plume is what you see of a plant from afar). */
	public static net.minecraft.core.particles.SimpleParticleType COOLING_PLUME;

	private JugcraftNuclear() {
	}

	public static void register() {
		RADIATION = AttachmentRegistry.<RadiationState>builder()
				.persistent(RadiationState.CODEC)
				.syncWith(RadiationState.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
				.buildAndRegister(Jugcraft.id("radiation"));
		RADIOACTIVITY = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("radioactivity"),
				DataComponentType.<Float>builder().persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT).build());
		GEIGER = Registry.register(BuiltInRegistries.SOUND_EVENT, Jugcraft.id("geiger_counter"),
				SoundEvent.createVariableRangeEvent(Jugcraft.id("geiger_counter")));
		EDG_START = Registry.register(BuiltInRegistries.SOUND_EVENT, Jugcraft.id("diesel.start"), SoundEvent.createFixedRangeEvent(Jugcraft.id("diesel.start"), 40));
		EDG_RUN = Registry.register(BuiltInRegistries.SOUND_EVENT, Jugcraft.id("diesel.run"), SoundEvent.createFixedRangeEvent(Jugcraft.id("diesel.run"), 40));
		EDG_STOP = Registry.register(BuiltInRegistries.SOUND_EVENT, Jugcraft.id("diesel.stop"), SoundEvent.createFixedRangeEvent(Jugcraft.id("diesel.stop"), 32));
		// The diesel rooms' CO2 system (round 31 part 3, Co2System): its alarm horn and the discharge.
		CO2_HORN = Registry.register(BuiltInRegistries.SOUND_EVENT, Jugcraft.id("diesel.co2_horn"), SoundEvent.createFixedRangeEvent(Jugcraft.id("diesel.co2_horn"), 48));
		CO2_DISCHARGE = Registry.register(BuiltInRegistries.SOUND_EVENT, Jugcraft.id("diesel.co2_discharge"),
				SoundEvent.createFixedRangeEvent(Jugcraft.id("diesel.co2_discharge"), 48));

		EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		for (int i = 0; i < 4; i++) {
			EquipmentSlot slot = slots[i];
			HAZMAT_ITEMS[i] = item(HAZMAT[i], p -> new HazmatItem(p.stacksTo(1).durability(400)
					.component(net.minecraft.core.component.DataComponents.EQUIPPABLE, Equippable.builder(slot)
							.setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER)
							.setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, Jugcraft.id("hazmat"))).build())));
		}
		GEIGER_COUNTER = item("geiger_counter", p -> new NuclearItem(p.stacksTo(1), "tooltip.jugcraft.geiger_counter"));
		CHECK_SOURCE = item("check_source", p -> new NuclearItem(p.stacksTo(1).rarity(Rarity.UNCOMMON).component(RADIOACTIVITY, 2F), "tooltip.jugcraft.check_source"));

		BURNUP = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("burnup"),
				DataComponentType.<Float>builder().persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT).build());
		LINKED_VESSEL = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("linked_vessel"),
				DataComponentType.<net.minecraft.core.BlockPos>builder().persistent(net.minecraft.core.BlockPos.CODEC)
						.networkSynchronized(net.minecraft.core.BlockPos.STREAM_CODEC).build());
		ENRICHED_URANIUM = item("enriched_uranium_cylinder", p -> new NuclearItem(p.stacksTo(16).component(RADIOACTIVITY, 0.5F),
				"tooltip.jugcraft.enriched_uranium_cylinder"));
		FUEL_ASSEMBLY = item("leu_fuel_assembly", p -> new NuclearItem(p.stacksTo(64).component(RADIOACTIVITY, 1F),
				"tooltip.jugcraft.leu_fuel_assembly"));
		SPENT_FUEL = item("spent_fuel_assembly", p -> new NuclearItem(p.stacksTo(16).rarity(Rarity.RARE)
				.component(RADIOACTIVITY, EquipmentBlockEntity.SPENT_RADIOACTIVITY), "tooltip.jugcraft.spent_fuel_assembly"));
		LINK_CARD = item("reactor_link_card", p -> new NuclearItem(p.stacksTo(1), "tooltip.jugcraft.reactor_link_card"));

		ResourceKey<Block> frameKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("nuclear_equipment_frame"));
		FRAME = Registry.register(BuiltInRegistries.BLOCK, frameKey, new EquipmentFrameBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
				.strength(3.0F, 12.0F).noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE).setId(frameKey)));
		for (EquipmentKind kind : EquipmentKind.values()) {
			ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(kind.id));
			Block block = Registry.register(BuiltInRegistries.BLOCK, key, new EquipmentAnchorBlock(kind, BlockBehaviour.Properties
					.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F, 12.0F).noOcclusion()
					.pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE).setId(key)));
			EQUIPMENT.put(kind, block);
			ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(kind.id));
			EQUIPMENT_ITEMS.put(kind, Registry.register(BuiltInRegistries.ITEM, itemKey,
					new EquipmentItem(block, kind, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix().stacksTo(16))));
		}
		EQUIPMENT_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("nuclear_equipment"),
				net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(EquipmentBlockEntity::new,
						EQUIPMENT.values().toArray(Block[]::new)).build());
		for (PipeKind kind : PipeKind.values()) {
			ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(kind.id));
			Block block = Registry.register(BuiltInRegistries.BLOCK, key, new NuclearPipeBlock(kind, BlockBehaviour.Properties
					.ofFullCopy(kind == PipeKind.WATER ? Blocks.STONE : Blocks.IRON_BLOCK).strength(2.0F, 6.0F).noOcclusion().setId(key)));
			PIPES.put(kind, block);
			ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(kind.id));
			Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		}
		// The condenser takes cooling water on any face; the transformer gives power out of any face (it also pushes).
		net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.registerForBlocks((level, pos, state, entity, side) -> {
			EquipmentBlockEntity equipment = equipmentAt(level, pos);
			return equipment == null ? null : equipment.water;
		}, net.minecraft.util.Util.make(new java.util.ArrayList<Block>(EQUIPMENT.values()), l -> l.add(FRAME)).toArray(Block[]::new));
		io.github.jimbozoomer.jugcraft.energy.EnergyStorage.SIDED.registerForBlocks((level, pos, state, entity, side) -> {
			EquipmentBlockEntity equipment = equipmentAt(level, pos);
			if (equipment != null && equipment.bus != null) {
				// A load centre or a motor control centre: its incoming side and its feeders' terminals.
				return equipment.kind() == EquipmentKind.MCC_LINEUP ? MotorControlCenter.storageAt(level, pos, equipment)
						: Switchgear.storageAt(level, pos, equipment);
			}
			return equipment == null ? null : equipment.output;
		}, net.minecraft.util.Util.make(new java.util.ArrayList<Block>(EQUIPMENT.values()), l -> l.add(FRAME)).toArray(Block[]::new));
		ConsoleActionPayload.register();
		ControlBoard.register();

		ResourceKey<Block> towerKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("cooling_tower_controller"));
		COOLING_TOWER = Registry.register(BuiltInRegistries.BLOCK, towerKey, new CoolingTowerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
				.strength(3.0F, 12.0F).setId(towerKey)));
		ResourceKey<Item> towerItem = ResourceKey.create(Registries.ITEM, Jugcraft.id("cooling_tower_controller"));
		Registry.register(BuiltInRegistries.ITEM, towerItem, new BlockItem(COOLING_TOWER, new Item.Properties().setId(towerItem).useBlockDescriptionPrefix()));
		COOLING_TOWER_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("cooling_tower_controller"),
				net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(CoolingTowerBlockEntity::new, COOLING_TOWER).build());
		ResourceKey<Block> worksKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("water_works_controller"));
		WATER_WORKS = Registry.register(BuiltInRegistries.BLOCK, worksKey, new WaterWorksBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
				.strength(3.0F, 12.0F).setId(worksKey)));
		ResourceKey<Item> worksItem = ResourceKey.create(Registries.ITEM, Jugcraft.id("water_works_controller"));
		Registry.register(BuiltInRegistries.ITEM, worksItem, new BlockItem(WATER_WORKS, new Item.Properties().setId(worksItem).useBlockDescriptionPrefix()));
		WATER_WORKS_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("water_works_controller"),
				net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(WaterWorksBlockEntity::new, WATER_WORKS).build());
		ResourceKey<Block> yardKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("yard_controller"));
		YARD = Registry.register(BuiltInRegistries.BLOCK, yardKey, new YardBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
				.strength(3.0F, 12.0F).setId(yardKey)));
		ResourceKey<Item> yardItem = ResourceKey.create(Registries.ITEM, Jugcraft.id("yard_controller"));
		Registry.register(BuiltInRegistries.ITEM, yardItem, new BlockItem(YARD, new Item.Properties().setId(yardItem).useBlockDescriptionPrefix()));
		YARD_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("yard_controller"),
				net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(YardBlockEntity::new, YARD).build());
		ResourceKey<Block> rbKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("reactor_building_controller"));
		REACTOR_BUILDING = Registry.register(BuiltInRegistries.BLOCK, rbKey, new ReactorBuildingBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
				.strength(3.0F, 12.0F).setId(rbKey)));
		ResourceKey<Item> rbItem = ResourceKey.create(Registries.ITEM, Jugcraft.id("reactor_building_controller"));
		Registry.register(BuiltInRegistries.ITEM, rbItem, new BlockItem(REACTOR_BUILDING, new Item.Properties().setId(rbItem).useBlockDescriptionPrefix()));
		REACTOR_BUILDING_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("reactor_building_controller"),
				net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(ReactorBuildingBlockEntity::new,
						REACTOR_BUILDING).build());
		YARD_BREAKER = Registry.register(BuiltInRegistries.SOUND_EVENT, Jugcraft.id("yard.breaker"), SoundEvent.createFixedRangeEvent(Jugcraft.id("yard.breaker"), 64));
		YARD_ARC = Registry.register(BuiltInRegistries.SOUND_EVENT, Jugcraft.id("yard.arc"), SoundEvent.createFixedRangeEvent(Jugcraft.id("yard.arc"), 96));
		YARD_HUM = Registry.register(BuiltInRegistries.SOUND_EVENT, Jugcraft.id("yard.hum"), SoundEvent.createFixedRangeEvent(Jugcraft.id("yard.hum"), 18));
		YARD_GONG = Registry.register(BuiltInRegistries.SOUND_EVENT, Jugcraft.id("yard.gong"), SoundEvent.createFixedRangeEvent(Jugcraft.id("yard.gong"), 40));
		COOLING_FILL = block("cooling_tower_fill", p -> new Block(p), BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).strength(1.0F, 3.0F));
		CONDUIT_TRENCH = block("conduit_trench", ConduitTrenchBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).strength(2.0F, 6.0F));
		COOLING_PLUME = Registry.register(BuiltInRegistries.PARTICLE_TYPE, Jugcraft.id("cooling_plume"),
				net.fabricmc.fabric.api.particle.v1.FabricParticleTypes.simple(true));

		ResourceKey<Block> concreteKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("shielding_concrete"));
		SHIELDING_CONCRETE = Registry.register(BuiltInRegistries.BLOCK, concreteKey,
				new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(net.minecraft.world.level.material.MapColor.COLOR_GRAY).strength(4.0F, 60F).setId(concreteKey)));
		ResourceKey<Item> concreteItem = ResourceKey.create(Registries.ITEM, Jugcraft.id("shielding_concrete"));
		Registry.register(BuiltInRegistries.ITEM, concreteItem, new BlockItem(SHIELDING_CONCRETE,
				new Item.Properties().setId(concreteItem).useBlockDescriptionPrefix()));

		ServerTickEvents.END_SERVER_TICK.register(RadiationField::tick);
		ServerTickEvents.END_SERVER_TICK.register(Radiation::tick);
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, ok) -> Shielding.clear());
		PlantInterior.register();
		StorageBlocks.register();
		PlantDoors.register();             // the doors' block entity and part models (doors round)
		StorageTanks.register();
		PlantBuilder.register();
		PlantShowcase.register();
		Sites.register();                  // the compiled sites: the medium plant's cooling towers and pump houses
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				Commands.literal("radiation").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.literal("dose").then(Commands.argument("mSv", DoubleArgumentType.doubleArg(0)).executes(context -> {
							Radiation.setDose(context.getSource().getPlayerOrException(), DoubleArgumentType.getDouble(context, "mSv"));
							return 1;
						})))
						.then(Commands.literal("check").executes(context -> {
							var player = context.getSource().getPlayerOrException();
							RadiationState state = Radiation.state(player);
							context.getSource().sendSuccess(() -> Component.literal(String.format("Dose %.1f mSv, rate %.1f mSv/h, suit %d/4",
									state.dose(), Radiation.rateAt(player), Radiation.hazmatPieces(player))), false);
							return 1;
						}))));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
			for (Item item : HAZMAT_ITEMS) {
				output.accept(item);
			}
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			output.accept(GEIGER_COUNTER);
			output.accept(CHECK_SOURCE);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output -> output.accept(SHIELDING_CONCRETE));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> {
			EQUIPMENT_ITEMS.values().forEach(output::accept);
			output.accept(COOLING_TOWER);
			output.accept(WATER_WORKS);
			output.accept(YARD);
			output.accept(COOLING_FILL);
			output.accept(CONDUIT_TRENCH);
			PIPES.values().forEach(b -> output.accept(b.asItem()));
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
			output.accept(ENRICHED_URANIUM);
			output.accept(FUEL_ASSEMBLY);
			output.accept(SPENT_FUEL);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> output.accept(LINK_CARD));
	}

	/** The equipment a block (anchor or frame) belongs to. */
	public static EquipmentBlockEntity equipmentAt(net.minecraft.world.level.Level level, BlockPos pos) {
		BlockPos anchor = EquipmentRegistry.of(level).anchorOf(pos);
		return anchor != null && level.getBlockEntity(anchor) instanceof EquipmentBlockEntity e ? e : null;
	}

	/** A block with its item. */
	private static Block block(String name, java.util.function.Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(name));
		Block block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(name));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		return block;
	}

	private static Item item(String name, java.util.function.Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
	}
}
