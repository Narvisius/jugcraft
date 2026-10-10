package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tower.FurnitureBlock;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/**
 * The pre-made plants' building kit (docs/features/nuclear.md, "Pre-made plants"): what the buildings are fitted out
 * with, inside and out. Doors that open by hand, a roller door that opens as one, building and room signs, server
 * racks, relay cabinets and pallet racking that join up when stacked, office furniture, safety railing, ducts, and
 * the plain finishes (suspended ceiling, vinyl and epoxy floors, painted block walls, painted tank plate, louvres).
 * Same ids as tools/plant_interior.py.
 */
public final class PlantInterior {
	/**
	 * Furniture: id, light, kind ("plain"; "stack" joins up when stacked; "row:group" joins side by side with the
	 * group; "variant:n" comes out as one of n at random), material ("metal", "wood", "wool", "plant"), then its
	 * hitbox (pixels, facing north).
	 */
	private static final Object[][] FURNITURE = {
			{"server_rack", 0, "stack", "metal", new double[] {1, 0, 1, 15, 16, 15.6}},
			{"relay_cabinet", 0, "stack", "metal", new double[] {1, 0, 2, 15, 16, 15.3}},
			{"pallet_rack", 0, "stack", "metal", new double[] {0, 0, 0, 16, 16, 16}},
			{"pallet_rack_drums", 0, "stack", "metal", new double[] {0, 0, 0, 16, 16, 16}},
			{"storage_rack", 0, "rack", "metal", null},
			{"high_bay_light", 15, "high_bay", "metal", new double[] {2.5, 7.8, 2.5, 13.5, 16, 13.5}},
			{"light_hanger", 0, "plain", "metal", new double[] {6.5, 0, 6.5, 9.5, 16, 9.5}},
			{"office_desk", 0, "plain", "wood", new double[] {0, 0, 1, 16, 12.5, 15}},
			{"filing_cabinet", 0, "plain", "metal", new double[] {2, 0, 2, 14, 15, 15}},
			{"whiteboard", 0, "variant:24", "metal", new double[] {0.5, 3, 0, 15.5, 15, 1.6}},
			{"water_cooler", 0, "plain", "metal", new double[] {4, 0, 4, 12, 16, 12}},
			{"guard_rail", 0, "plain", "metal", new double[] {0, 0, 0, 16, 16, 2}},
			{"locker", 0, "stack", "metal", new double[] {1, 0, 2, 15, 16, 15}},
			{"locker_open", 0, "locker:lab", "metal", new double[] {1, 0, 2, 15, 16, 15}},
			{"bookcase", 0, "stack", "wood", new double[] {1, 0, 1, 15, 16, 15}},
			{"fridge", 0, "stack", "metal", new double[] {1.5, 0, 2, 14.5, 16, 15}},
			{"fume_hood", 0, "stack", "metal", new double[] {0, 0, 1, 16, 16, 15.5}},
			{"reception_desk", 0, "row:reception:0:3", "wood", new double[] {0, 0, 0, 16, 16, 16}},
			{"sofa", 0, "row:sofa:0.42", "wool", new double[] {0, 0, 1, 16, 8, 16}},
			{"locker_bench", 0, "row:bench:0.4", "wood", new double[] {0, 0, 4, 16, 7, 12}},
			{"kitchen_counter", 0, "row:counter", "wood", new double[] {0, 0, 1, 16, 15, 16}},
			{"kitchen_sink", 0, "row:counter", "wood", new double[] {0, 0, 1, 16, 15, 16}},
			{"microwave_counter", 0, "row:counter", "wood", new double[] {0, 0, 1, 16, 15, 16}},
			{"coffee_counter", 0, "row:counter", "wood", new double[] {0, 0, 1, 16, 15, 16}},
			{"lab_bench", 0, "row:lab", "metal", new double[] {0, 0, 1, 16, 14.5, 16}},
			{"lab_sink", 0, "row:lab", "metal", new double[] {0, 0, 1, 16, 14.5, 16}},
			{"lab_analyzer", 0, "row:lab", "metal", new double[] {0, 0, 1, 16, 14.5, 16}},
			{"cctv_desk", 0, "plain", "metal", new double[] {0, 0, 1, 16, 12.5, 15}},
			{"coffee_table", 0, "plain", "wood", new double[] {1, 0, 3, 15, 7, 13}},
			{"cafe_table", 0, "variant:6", "wood", new double[] {2, 0, 2, 14, 12.5, 14}},
			{"cafe_chair", 0, "seat:0.44", "wood", new double[] {3.5, 0, 3.5, 12.5, 9, 13}},
			{"coat_rack", 0, "plain", "wood", new double[] {1, 0, 0, 15, 16, 4}},
			{"printer", 0, "plain", "metal", new double[] {1, 0, 2, 15, 14, 14}},
			{"wall_display", 0, "plain", "metal", new double[] {0, 4, 0, 16, 14, 1.2}},
			{"office_plant", 0, "variant:8", "plant", new double[] {4, 0, 4, 12, 16, 12}},
			{"framed_print", 0, "variant:16", "wood", new double[] {1, 3, 0, 15, 14, 1.6}},
			{"locker_security", 0, "locker:security", "metal", new double[] {1, 0, 2, 15, 16, 15}},
			{"office_desk_long", 0, "row:desk:0:6", "wood", new double[] {0, 0, 1, 16, 12.5, 15}},
			{"executive_desk", 0, "row:exec:0:3", "wood", new double[] {0, 0, 0.5, 16, 13, 15.5}},
			{"glass_desk", 0, "row:glass:0:4", "metal", new double[] {0, 0, 1, 16, 12.5, 15}},
			{"operator_console", 4, "row:opcon:0:4", "metal", new double[] {0, 0, 0.5, 16, 12, 15.5}},
			{"stretcher", 0, "row:stretcher:0.6", "metal", new double[] {0, 0, 1.5, 16, 10, 14.5}},
			{"cubicle_panel", 0, "row:cubicle", "wool", new double[] {0, 0, 7, 16, 16, 9}},
			{"aed_cabinet", 0, "plain", "metal", new double[] {3, 3, 0, 13, 14, 4}},
			{"fire_extinguisher", 0, "plain", "metal", new double[] {3, 0.5, 0, 13, 15.5, 4.6}},
			{"emergency_light", 0, "plain", "metal", new double[] {1.4, 0.8, 0, 14.6, 14.8, 12}},
			{"emergency_light_conduit", 0, "plain", "metal", new double[] {7.4, 0, 0, 9.6, 16, 1.8}},
			{"medicine_cabinet", 0, "plain", "metal", new double[] {1, 5, 0, 15, 16, 5}},
			{"vitals_monitor", 0, "plain", "metal", new double[] {4, 0, 4, 12, 16, 12}},
			{"filing_cabinet_open", 0, "plain", "metal", new double[] {2, 0, 2, 14, 15, 15}},
			{"drafting_table", 0, "plain", "wood", new double[] {0, 0, 1, 16, 16, 15}},
			{"blueprint_sheet", 0, "variant:6", "wood", new double[] {1, 2, 0, 15, 15, 0.6}},
			{"training_screen", 0, "multi:3:2:v", "metal", null},
			{"plant_map_screen", 0, "multi:3:2:v", "metal", null},
			{"status_screen", 0, "status", "metal", null},
			{"flat_tv", 0, "multi:3:2:v", "metal", null},
			{"oval_table", 0, "multi:5:3:h:3", "wood", null},
			{"lounge_table", 0, "multi:3:2:c", "glass", null},
			{"large_print", 0, "multi:2:1:v:12", "wood", null},
			{"filing_cabinet_tall", 0, "stack:tall_filing", "metal", new double[] {2, 0, 2, 14, 16, 15}},
			{"filing_cabinet_tall_open", 0, "stack:tall_filing", "metal", new double[] {2, 0, 2, 14, 16, 15}},
			{"office_copier", 0, "multi:2:2:b", "metal", null},
			{"glass_coffee_table", 0, "steam:4.8:9.8:6.8", "metal", new double[] {1, 0, 2, 15, 7.3, 14}},
			// The diesel generator rooms' control and protection panels (tools/plant_panels.py): two high, one or two wide.
			{"edg_engine_panel", 0, "live:1:2:p", "metal", null},
			{"edg_generator_panel", 0, "live:2:2:p", "metal", null},
			{"edg_voltage_regulator", 0, "live:1:2:p", "metal", null},
			{"edg_protection_panel", 0, "live:1:2:p", "metal", null},
			{"edg_load_sequencer", 0, "live:1:2:p", "metal", null},
			{"edg_battery_charger", 0, "live:1:2:p", "metal", null},
			{"edg_battery_rack", 0, "multi:1:2:p", "metal", null},
			// The control building's DC system (tools/plant_panels.py, round 18): the station battery (a four-block step
			// rack of 30 cells), its distribution switchboard, the vital inverter, and the cables from battery to switchboard.
			{"station_battery", 0, "battery:4:2:p", "metal", null},
			{"dc_switchboard", 0, "live:2:2:p", "metal", null},
			{"vital_inverter", 0, "multi:1:2:p", "metal", null},
			{"dc_cable_tray", 0, "plain", "metal", new double[] {0, 12, 4, 16, 16, 12}},
			{"dc_cable_riser", 0, "plain", "metal", new double[] {5, 0, 0, 11, 16, 3}},
			{"dc_cable_riser_top", 0, "plain", "metal", new double[] {0, 0, 0, 16, 16, 12}},
			// A load centre's feeder conduit, up from its hub through the ceiling into the trays above (round 25b).
			{"feeder_conduit", 0, "plain", "metal", new double[] {6.7, 0, 6.7, 9.3, 16, 9.3}},
			{"feeder_conduit_base", 0, "plain", "metal", new double[] {6.7, 0, 6.7, 9.3, 16, 9.3}},
			{"feeder_conduit_ceiling", 0, "plain", "metal", new double[] {6.7, 0, 6.7, 9.3, 16, 9.3}},
			{"feeder_conduit_floor", 0, "plain", "metal", new double[] {5.6, 0, 0, 10.4, 16, 10.4}},
			{"feeder_conduit_tray", 0, "plain", "metal", new double[] {6.5, 0, 6.5, 9.5, 8.8, 16}},
	};
	/** Plain full blocks: suspended ceiling, floors, walls, tank plate, louvres. */
	public static final String[] CUBES = {"ceiling_tile", "vinyl_floor", "epoxy_floor", "painted_wall", "tank_plate", "intake_louvre"};

	public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
	public static Block ROLLER_DOOR;
	public static Block BUILDING_SIGN;
	public static Block ROOM_SIGN;
	public static BlockEntityType<PlantSignBlockEntity> SIGN_ENTITY;
	public static BlockEntityType<StatusScreenBlockEntity> STATUS_ENTITY;
	public static BlockEntityType<LivePanelBlockEntity> LIVE_PANEL_ENTITY;
	public static BlockEntityType<StationBatteryBlockEntity> STATION_BATTERY_ENTITY;
	public static BlockEntityType<LockerBlockEntity> LOCKER_ENTITY;
	public static BlockEntityType<StorageRackBlockEntity> RACK_ENTITY;
	public static BlockEntityType<RollerDoorBlockEntity> ROLLER_ENTITY;
	public static BlockEntityType<SpeedGateBlockEntity> SPEED_GATE_ENTITY;
	public static Item LAB_COAT, SECURITY_JACKET;

	public static Item uniform(UniformItem.Kind kind) {
		return kind == UniformItem.Kind.LAB_COAT ? LAB_COAT : SECURITY_JACKET;
	}

	private static Item uniformItem(String name, UniformItem.Kind kind) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, new UniformItem(new Item.Properties().setId(key).stacksTo(1)
				.component(net.minecraft.core.component.DataComponents.EQUIPPABLE,
						net.minecraft.world.item.equipment.Equippable.builder(net.minecraft.world.entity.EquipmentSlot.CHEST)
								.setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER)
								.setAsset(ResourceKey.create(net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID, Jugcraft.id("uniform")))
								.build()), kind));
	}

	/** Doors that open by hand, with steel door sounds. */
	public static final BlockSetType STEEL = new BlockSetType("jugcraft_steel", true, true, false, BlockSetType.PressurePlateSensitivity.EVERYTHING,
			SoundType.METAL, SoundEvents.IRON_DOOR_CLOSE, SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundEvents.IRON_TRAPDOOR_OPEN,
			SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF, SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON, SoundEvents.STONE_BUTTON_CLICK_OFF,
			SoundEvents.STONE_BUTTON_CLICK_ON);

	private PlantInterior() {
	}

	public static void register() {
		LAB_COAT = uniformItem("lab_coat", UniformItem.Kind.LAB_COAT);
		SECURITY_JACKET = uniformItem("security_jacket", UniformItem.Kind.SECURITY_JACKET);
		for (Object[] f : FURNITURE) {
			String id = (String) f[0];
			int light = (Integer) f[1];
			String kind = (String) f[2];
			double[] shape = (double[]) f[4];
			Block like = switch ((String) f[3]) {
				case "wood" -> Blocks.OAK_PLANKS;
				case "wool" -> Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.GRAY);
				case "plant" -> Blocks.AZALEA;
				default -> Blocks.IRON_BLOCK;
			};
			BlockBehaviour.Properties props = BlockBehaviour.Properties.ofFullCopy(like).strength(1.5F).noOcclusion().lightLevel(state -> light);
			block(id, p -> {
				String[] k = kind.split(":");
				if (kind.equals("stack")) {
					return new StackingFurnitureBlock(p, shape);
				} else if (kind.equals("high_bay")) {
					return new HighBayLightBlock(p, shape);
				} else if (kind.equals("rack")) {
					return new StorageRackBlock(p);
				} else if (k[0].equals("locker")) {
					return new LockerBlock(p, k[1].equals("lab") ? UniformItem.Kind.LAB_COAT : UniformItem.Kind.SECURITY_JACKET, shape);
				} else if (k[0].equals("stack")) {
					return new StackingFurnitureBlock(p, shape).group(k[1]);
				} else if (kind.equals("status")) {
					return new StatusScreenBlock(p);
				} else if (k[0].equals("live")) {
					return new LivePanelBlock(p, Integer.parseInt(k[1]), Integer.parseInt(k[2]));
				} else if (k[0].equals("battery")) {
					// A station battery that holds charge (round 25).
					return new StationBatteryBlock(p, Integer.parseInt(k[1]), Integer.parseInt(k[2]));
				} else if (k[0].equals("multi")) {
					// "b": a machine standing on the floor (rising a little into the row above); "p": a panel standing on the
					// floor, full height in both rows.
					return new MultiFurnitureBlock(p, Integer.parseInt(k[1]), Integer.parseInt(k[2]), !k[3].equals("h") && !k[3].equals("c"),
							k.length > 4 ? Integer.parseInt(k[4]) : 1, k[3].equals("b") || k[3].equals("p"), k[3].equals("c"),
							k[3].equals("p") ? 15.6 : 7);
				} else if (k[0].equals("steam")) {
					return new SteamFurnitureBlock(p, Double.parseDouble(k[1]), Double.parseDouble(k[2]), Double.parseDouble(k[3]), shape);
				} else if (k[0].equals("seat")) {
					return new SeatFurnitureBlock(p, Double.parseDouble(k[1]), shape);
				} else if (k[0].equals("row")) {
					return RowFurnitureBlock.create(p, k[1], k.length > 2 ? Double.parseDouble(k[2]) : 0, k.length > 3 ? Integer.parseInt(k[3]) : 0, shape);
				} else if (kind.startsWith("variant:")) {
					return VariantFurnitureBlock.create(p, Integer.parseInt(kind.substring(8)), shape);
				}
				return new FurnitureBlock(p, shape);
			}, props);
		}
		// The desk with a coffee on it steams (its mug: tools/plant_office2.py MUG_AT).
		((RowFurnitureBlock) BLOCKS.get("office_desk_long")).steam(2, 11.8, 15.6, 7.8);
		((VariantFurnitureBlock) BLOCKS.get("cafe_table")).steam(5, 11.7, 15.3, 6.2);
		block("conference_table", ConferenceTableBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(1.5F).noOcclusion());
		for (String id : CUBES) {
			BlockBehaviour.Properties props = BlockBehaviour.Properties.ofFullCopy(id.contains("tank") || id.contains("louvre")
					? Blocks.IRON_BLOCK : Blocks.STONE).strength(1.5F, 6F);
			block(id, Block::new, props);
		}
		block("hvac_duct", RotatedPillarBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.5F, 6F));
		for (String id : new String[] {"steel_door", "glass_door"}) {
			ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(id));
			Block door = Registry.register(BuiltInRegistries.BLOCK, key, new DoorBlock(STEEL, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_DOOR)
					.strength(3.0F).noOcclusion().setId(key)) {
			});
			ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
			Registry.register(BuiltInRegistries.ITEM, itemKey, new DoubleHighBlockItem(door, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
			BLOCKS.put(id, door);
		}
		// The speed gate: its lane block (the item) and its pedestals (no item: they come with the gates).
		block("speed_gate", SpeedGateBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F, 12F).noOcclusion()
				.sound(net.minecraft.world.level.block.SoundType.METAL));
		ResourceKey<Block> pedestalKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("speed_gate_pedestal"));
		BLOCKS.put("speed_gate_pedestal", Registry.register(BuiltInRegistries.BLOCK, pedestalKey, new SpeedGatePedestalBlock(
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F, 12F).noOcclusion().setId(pedestalKey))));
		SPEED_GATE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("speed_gate"),
				FabricBlockEntityTypeBuilder.create(SpeedGateBlockEntity::new, BLOCKS.get("speed_gate")).build());
		// Framed glass: a storefront partition that meets the glass doors flush (FramedGlassBlock).
		block("framed_glass", FramedGlassBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS_PANE).strength(1.0F, 6F).noOcclusion());
		ROLLER_DOOR = block("roller_door", RollerDoorBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F, 12F).noOcclusion());
		BUILDING_SIGN = block("building_sign", PlantSignBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.0F).noOcclusion()
				.noCollision());
		ROOM_SIGN = block("room_sign", PlantSignBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(0.5F).noOcclusion()
				.noCollision());
		SIGN_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("plant_sign"),
				FabricBlockEntityTypeBuilder.create(PlantSignBlockEntity::new, BUILDING_SIGN, ROOM_SIGN).build());
		STATUS_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("status_screen"),
				FabricBlockEntityTypeBuilder.create(StatusScreenBlockEntity::new, BLOCKS.get("status_screen")).build());
		LIVE_PANEL_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("live_panel"),
				FabricBlockEntityTypeBuilder.create(LivePanelBlockEntity::new, BLOCKS.entrySet().stream()
						.filter(e -> e.getValue() instanceof LivePanelBlock).map(java.util.Map.Entry::getValue).toArray(Block[]::new)).build());
		STATION_BATTERY_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("station_battery"),
				FabricBlockEntityTypeBuilder.create(StationBatteryBlockEntity::new, BLOCKS.get("station_battery")).build());
		// The DC system holds charge (round 25): a cable into a station battery charges it or draws on it; a cable into a
		// battery charger is its AC supply.
		io.github.jimbozoomer.jugcraft.energy.EnergyStorage.SIDED.registerForBlocks((level, pos, state, entity, side) ->
				StationBatteryBlock.storage(level, pos), BLOCKS.get("station_battery"));
		io.github.jimbozoomer.jugcraft.energy.EnergyStorage.SIDED.registerForBlocks((level, pos, state, entity, side) ->
				LivePanelBlockEntity.chargerInput(level, pos), BLOCKS.get("edg_battery_charger"));
		RACK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("storage_rack"),
				FabricBlockEntityTypeBuilder.create(StorageRackBlockEntity::new, BLOCKS.get("storage_rack")).build());
		ROLLER_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("roller_door"),
				FabricBlockEntityTypeBuilder.create(RollerDoorBlockEntity::new, ROLLER_DOOR).build());
		LOCKER_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("locker"),
				FabricBlockEntityTypeBuilder.create(LockerBlockEntity::new, BLOCKS.get("locker_open"), BLOCKS.get("locker_security")).build());
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
			output.accept(LAB_COAT);
			output.accept(SECURITY_JACKET);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output -> BLOCKS.values().stream()
				.filter(b -> b.asItem() != net.minecraft.world.item.Items.AIR).forEach(output::accept));
	}

	private static Block block(String name, java.util.function.Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(name));
		Block block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(name));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		BLOCKS.put(name, block);
		return block;
	}
}
