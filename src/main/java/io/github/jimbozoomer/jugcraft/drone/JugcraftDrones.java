package io.github.jimbozoomer.jugcraft.drone;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.materials.JugcraftRegistry;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
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
 * Registers the Drone Depot: terminal, landing platform, landing pad plates, supply pickup plates,
 * control screen panels, hologram table, control room decoration, cargo packager, the drone items
 * (tiers 1 to 9) and their parts. IDs are listed in {@code tools/drones.py}.
 */
public final class JugcraftDrones {
	public static final String FEATURE = "drones";
	/** Plain crafting parts, in the order of tools/drones.py DRONE_PARTS. */
	public static final String[] PARTS = {"drone_motor", "wooden_propeller_set", "aluminum_propeller_set",
			"rotor_assembly", "aluminum_rotor_assembly", "ducted_fan", "flight_controller", "lead_acid_pack",
			"cargo_winch", "neodymium_drone_motor", "tilt_rotor_nacelle", "composite_rotor_set", "composite_rotor_assembly",
			"hydrogen_lift_cell", "ion_emitter", "superconducting_tape", "stirling_cryocooler", "superconducting_motor",
			"superconducting_lift_fan"};

	/** Rotor loops for flying drones (client side, see DroneSounds): small drones, and tiers 5-9. */
	public static net.minecraft.sounds.SoundEvent DRONE_HUM;
	public static net.minecraft.sounds.SoundEvent DRONE_HUM_HEAVY;
	public static Block TERMINAL;
	public static Block LANDING_PLATFORM;
	public static Block LANDING_PAD;
	public static Block CARGO_PACKAGER;
	public static Block SUPPLY_PICKUP;
	public static Block CONTROL_SCREEN;
	public static Block HOLO_TABLE;
	/** Control room decoration: wall panel, floor tile, ceiling light and window. */
	public static BlockEntityType<DroneTerminalBlockEntity> TERMINAL_ENTITY;
	public static BlockEntityType<CargoPackagerBlockEntity> PACKAGER_ENTITY;
	public static BlockEntityType<DepotDisplayBlockEntity> SCREEN_ENTITY;
	public static BlockEntityType<DepotDisplayBlockEntity> HOLO_ENTITY;
	public static final Map<DroneTier, Item> DRONES = new EnumMap<>(DroneTier.class);
	public static final List<Item> PART_ITEMS = new ArrayList<>();

	private JugcraftDrones() {
	}

	public static boolean enabled() {
		return JugcraftConfig.isFeatureEnabled(FEATURE);
	}

	public static void register() {
		DRONE_HUM = sound("drone.hum", 32);
		DRONE_HUM_HEAVY = sound("drone.hum_heavy", 48);
		TERMINAL = block("drone_depot_terminal", new DroneTerminalBlock(properties("drone_depot_terminal",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F).noOcclusion())));
		LANDING_PLATFORM = block("landing_platform", new LandingPlatformBlock(properties("landing_platform",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F).lightLevel(state -> 4)), PlatformLayout.Cell.PLATFORM));
		// Pad plates go on top of the platform; a formed pad's middle plate (the charger port) glows.
		LANDING_PAD = block("landing_pad", new LandingPadBlock(properties("landing_pad",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F).noOcclusion()
						.lightLevel(state -> state.getValue(LandingPadBlock.PART) == LandingPadBlock.CHARGER_PART ? 7 : 0))));
		// Supply pickup plates go on the platform too; a formed pickup's hatch plate glows faintly.
		SUPPLY_PICKUP = block("supply_pickup", new SupplyPickupBlock(properties("supply_pickup",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F).noOcclusion()
						.lightLevel(state -> state.getValue(SupplyPickupBlock.PART) == SupplyPickupBlock.HATCH_PART ? 4 : 0))));
		// Control room screen panels (six form one wall display); they glow like monitors.
		CONTROL_SCREEN = block("control_screen", new ControlScreenBlock(properties("control_screen",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F).noOcclusion()
						.lightLevel(state -> state.getValue(ControlScreenBlock.PART) > 0 ? 9 : 4))));
		// Hologram table sections (nine form one table); the projector section glows.
		HOLO_TABLE = block("holo_table", new HoloTableBlock(properties("holo_table",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F).noOcclusion()
						.lightLevel(state -> state.getValue(HoloTableBlock.PART) == HoloTableBlock.PROJECTOR_PART ? 10 : 2))));
		CARGO_PACKAGER = block("cargo_packager", new CargoPackagerBlock(properties("cargo_packager",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F))));

		TERMINAL_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("drone_depot_terminal"),
				FabricBlockEntityTypeBuilder.create(DroneTerminalBlockEntity::new, TERMINAL).build());
		SCREEN_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("control_screen"),
				FabricBlockEntityTypeBuilder.create((pos, state) -> new DepotDisplayBlockEntity(SCREEN_ENTITY, pos, state), CONTROL_SCREEN).build());
		HOLO_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("holo_table"),
				FabricBlockEntityTypeBuilder.create((pos, state) -> new DepotDisplayBlockEntity(HOLO_ENTITY, pos, state), HOLO_TABLE).build());
		PACKAGER_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("cargo_packager"),
				FabricBlockEntityTypeBuilder.create(CargoPackagerBlockEntity::new, CARGO_PACKAGER).build());

		EnergyStorage.SIDED.registerForBlockEntity((terminal, side) -> terminal.energy(), TERMINAL_ENTITY);
		ItemStorage.SIDED.registerForBlockEntity((packager, side) -> ContainerStorage.of(packager, side), PACKAGER_ENTITY);

		for (String part : PARTS) {
			PART_ITEMS.add(JugcraftRegistry.item(part));
		}
		for (DroneTier tier : DroneTier.values()) {
			// Every tier has an item; tiers 5-9 have no recipe yet (creative only), see DroneTier.
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(tier.id()));
			DRONES.put(tier, Registry.register(BuiltInRegistries.ITEM, key,
					new DroneItem(tier, new Item.Properties().setId(key).stacksTo(16))));
		}

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> {
			output.accept(TERMINAL);
			output.accept(CONTROL_SCREEN);
			output.accept(HOLO_TABLE);
			output.accept(CARGO_PACKAGER);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> DRONES.values().forEach(output::accept));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> PART_ITEMS.forEach(output::accept));
		DroneDepots.register();
		TerminalModePayload.register();
		DroneDevCommands.register();
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

	private static net.minecraft.sounds.SoundEvent sound(String name, float range) {
		net.minecraft.resources.Identifier id = Jugcraft.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, net.minecraft.sounds.SoundEvent.createFixedRangeEvent(id, range));
	}
}
