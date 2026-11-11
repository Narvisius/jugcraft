package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
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
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The Drone Tower (docs/features/drone-tower.md): its building materials (usable for any big structure), the
 * command room's furniture, the Tower Core and the tower modules. IDs and kinds match tools/tower.py
 * (checked by tools/check_mod_data.py).
 */
public final class JugcraftTower {
	/** Building materials: id and kind (cube, pillar, glass, light:level). Same order as tools/tower.py BUILDING. */
	public static final String[][] BUILDING = {
			{"reinforced_concrete", "cube"}, {"steel_girder", "pillar"}, {"hazard_plating", "cube"}, {"steel_armor_plate", "cube"},
			{"blast_glass", "glass"}, {"hangar_bay_door", "cube"}, {"aluminum_cladding", "cube"}, {"tungsten_steel_frame", "cube"},
			{"carbon_composite_panel", "cube"}, {"silicon_carbide_armor", "cube"}, {"depleted_uranium_armor", "cube"},
			{"graphene_lattice", "cube"}, {"superconducting_conduit", "light:5"}, {"red_light_strip", "light:8"},
			{"warning_light", "light:13"}, {"acoustic_wall_panel", "cube"}, {"access_floor_tile", "cube"},
			{"carpet_tile", "cube"}, {"concrete_column", "pillar"}, {"transformer_casing", "cube"}, {"cooling_fin", "pillar"},
			{"ceramic_insulator", "pillar"}, {"copper_busbar", "pillar"}, {"armored_conduit", "pillar"}, {"dock_plating", "cube"},
			{"intake_funnel", "cube"}, {"hangar_pad", "light:6"}, {"amber_light_strip", "light:12"}};
	/** Building materials that also come as stairs and slabs. */
	public static final String[] VARIANTS = {"reinforced_concrete", "hazard_plating", "steel_armor_plate", "aluminum_cladding",
			"tungsten_steel_frame", "carbon_composite_panel", "silicon_carbide_armor", "depleted_uranium_armor", "graphene_lattice"};
	private static final java.util.Set<String> CONTINUOUS_PANELS = java.util.Set.of("reinforced_concrete", "aluminum_cladding",
			"carbon_composite_panel", "silicon_carbide_armor", "depleted_uranium_armor", "graphene_lattice", "steel_armor_plate",
			"transformer_casing", "acoustic_wall_panel", "access_floor_tile");

	public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
	/** The blocks a broken Tower Core held for its tower, carried by its item (TowerCoreBlockEntity#store). */
	public static net.minecraft.core.component.DataComponentType<Map<Item, Integer>> TOWER_STORE;
	public static Block CORE;
	public static BlockEntityType<TowerCoreBlockEntity> CORE_ENTITY;
	/** The tower's loading screen: the core's first layer, the Cargo Exchange's tower storage (TowerLoadMenu). */
	public static net.minecraft.world.inventory.MenuType<TowerLoadMenu> LOAD_MENU;
	/** The tower storage at the Cargo Exchange: a big scrolling chest of anything (TowerStorageMenu). */
	public static net.minecraft.world.inventory.MenuType<TowerStorageMenu> STORAGE_MENU;
	public static BlockEntityType<io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity> CONSOLE_ENTITY;
	public static Block MASTER_CONSOLE;
	public static BlockEntityType<io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity> MASTER_CONSOLE_ENTITY;
	/** The Tri-Pillar's giant round holo table (tools/holo_command_table.py). */
	public static Block HOLO_COMMAND_TABLE;
	public static BlockEntityType<io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity> HOLO_COMMAND_TABLE_ENTITY;
	/** The Tri-Pillar's master console redone (work list item 6), its chair and the raised floor it stands on. */
	public static Block TOWER_CONTROL_CONSOLE;
	public static BlockEntityType<io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity> TOWER_CONTROL_CONSOLE_ENTITY;
	public static BlockEntityType<ConsoleChairBlock.ConsoleChairBlockEntity> CONSOLE_CHAIR_ENTITY;
	/** The Tri-Pillar's curved LED video walls on the rotunda's cut pillar faces (work list item 7). */
	public static Block CURVED_VIDEO_WALL;
	public static BlockEntityType<io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity> CURVED_VIDEO_WALL_ENTITY;
	public static Block DRONE_DISPLAY_CASE;
	public static BlockEntityType<io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity> DRONE_DISPLAY_CASE_ENTITY;
	public static Block CRYSTAL_TREE;
	public static BlockEntityType<io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity> CRYSTAL_TREE_ENTITY;
	/** The flight-ops trees (layout 45): the Ember Oak and the Drift Isle, one block entity type drawing both. */
	public static Block EMBER_OAK, DRIFT_ISLE;
	public static BlockEntityType<io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity> OPS_TREE_ENTITY;
	public static Block HOLO_PROJECTOR;
	public static Block MAINTENANCE_DOOR;
	/** The neon light lines (work list item 11): the lines in a wall's material, the floor lines, the white tube. */
	public static final java.util.List<Block> NEON_LINES = new ArrayList<>(), NEON_FLOORS = new ArrayList<>();
	public static Block NEON_TUBE;
	/** The light the neon lines give, and the tube's (tools/neon_lines.py LINE_LIGHT, TUBE_LIGHT). */
	public static final int NEON_LIGHT = 12, NEON_TUBE_LIGHT = 14;
	public static BlockEntityType<io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity> HOLO_PROJECTOR_ENTITY;
	/** The supply cradles' raked struts (work list item 12; tools/cradle_struts.py). */
	public static Block CRADLE_STRUTS;
	public static BlockEntityType<io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity> CRADLE_STRUTS_ENTITY;
	/** The map room's Big Map Table (work list item 16; tools/big_map_table.py, MapScan). */
	public static Block BIG_MAP_TABLE;
	public static BlockEntityType<BigMapTableBlockEntity> BIG_MAP_TABLE_ENTITY;
	public static Block LONG_COFFEE_TABLE;
	public static BlockEntityType<io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity> LONG_COFFEE_TABLE_ENTITY;
	public static Block DRONE_PARTS_SHELVING;
	public static BlockEntityType<io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity> DRONE_PARTS_SHELVING_ENTITY;
	public static Block AISLE_CONTAINMENT_DOOR;
	public static BlockEntityType<AisleContainmentDoorBlockEntity> AISLE_CONTAINMENT_DOOR_ENTITY;
	/** The automatic sliding doors (Justin, 8 November 2026): the entrance's and the rooms', one block entity type for all. */
	public static BlockEntityType<SlidingDoorBlockEntity> SLIDING_DOOR_ENTITY;
	/** The Tri-Pillar hall's live fittings (HallFixtureBlock): touch consoles, info totem, holo emitters, drone stands, service arm. */
	public static BlockEntityType<io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity> HALL_ENTITY;
	public static final String[] HALL_FIXTURES = {"touch_console", "tower_info_totem", "holo_emitter", "drone_display_stand", "drone_service_arm"};
	public static net.minecraft.world.entity.EntityType<SeatEntity> SEAT;
	/** The tower's crew (tower/crew/TowerCrew): people in uniform at the posts of tools/tripillar_crew.py. */
	public static net.minecraft.world.entity.EntityType<io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew> TOWER_CREW;
	public static net.minecraft.world.entity.EntityType<io.github.jimbozoomer.jugcraft.tower.crew.CrewShip> CREW_SHIP;
	/** Drop Ship (Landed): the creative menu's way to set one down to stay. */
	public static net.minecraft.world.item.Item CREW_SHIP_ITEM;
	/** The drop ship's sounds: coming in, its thrusters burning, idling on the ground or in a hover, lifting off; its
	 * doors and ramp. */
	public static net.minecraft.sounds.SoundEvent SHIP_ENTRY, SHIP_THRUST, SHIP_IDLE, SHIP_LIFTOFF, SHIP_RAMP, SHIP_GUN_MOTOR;

	private JugcraftTower() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.TICKET_TYPE, Jugcraft.id("drone_tower"), TowerCoreBlockEntity.CHUNK_TICKET);
		TOWER_STORE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("tower_store"),
				net.minecraft.core.component.DataComponentType.<Map<Item, Integer>>builder().persistent(TowerCoreBlockEntity.STORE_CODEC)
						.networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.fromCodec(TowerCoreBlockEntity.STORE_CODEC)).build());
		for (String[] entry : BUILDING) {
			String id = entry[0];
			String kind = entry[1];
			BlockBehaviour.Properties props = properties(id, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
					.strength(id.contains("armor") || id.contains("tungsten") ? 6.0F : 3.0F, id.contains("uranium") ? 1200F : 12F));
			if (java.util.Set.of("tungsten_steel_frame", "armored_conduit", "copper_busbar", "cooling_fin", "ceramic_insulator").contains(id)) {
				props.noOcclusion();
			}
			Block block = switch (kind) {
				case "pillar" -> id.equals("steel_girder") ? new TowerGirderBlock(props.noOcclusion()) : new RotatedPillarBlock(props);
				case "glass" -> new TransparentBlock(properties(id, BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(3.0F, 12F))) {
				};
				default -> {
					if (id.equals("hangar_pad")) {
						yield new HangarPadBlock(props.lightLevel(state -> 6));
					}
					if (kind.startsWith("light:")) {
						int light = Integer.parseInt(kind.substring(6));
						if (id.equals("warning_light")) yield new TowerBeaconBlock(props.lightLevel(state -> light));
						yield new Block(props.lightLevel(state -> light));
					}
					yield CONTINUOUS_PANELS.contains(id) ? new TowerPanelBlock(props) : new Block(props);
				}
			};
			BLOCKS.put(id, block(id, block));
		}
		for (String base : VARIANTS) {
			Block full = BLOCKS.get(base);
			BLOCKS.put(base + "_stairs", block(base + "_stairs",
					new StairBlock(full.defaultBlockState(), properties(base + "_stairs", BlockBehaviour.Properties.ofFullCopy(full))) {
					}));
			BLOCKS.put(base + "_slab", block(base + "_slab",
					new SlabBlock(properties(base + "_slab", BlockBehaviour.Properties.ofFullCopy(full)))));
		}
		furniture("operator_chair", 0, new double[] {2, 0, 2, 14, 9, 14}, new double[] {3, 9, 12, 13, 16, 14});
		BLOCKS.put("console_desk", block("console_desk", new ConsoleDeskBlock(properties("console_desk",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.5F).noOcclusion().lightLevel(state -> 4)))));
		CONSOLE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("console_desk"),
				FabricBlockEntityTypeBuilder.create((pos, state) -> new io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity(
						CONSOLE_ENTITY, pos, state), BLOCKS.get("console_desk")).build());
		MASTER_CONSOLE = block("tower_master_console", new MasterConsoleBlock(properties("tower_master_console",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3F).noOcclusion().lightLevel(state -> 5))));
		BLOCKS.put("tower_master_console", MASTER_CONSOLE);
		MASTER_CONSOLE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("tower_master_console"),
				FabricBlockEntityTypeBuilder.create((pos, state) -> new io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity(
						MASTER_CONSOLE_ENTITY, pos, state), MASTER_CONSOLE).build());
		// the holograms (Holograms): no recipe, the tower's own; only a creative player places or breaks one
		HOLO_COMMAND_TABLE = block("holo_command_table", new HoloCommandTableBlock(properties("holo_command_table",
				Holograms.protect(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(state -> 5)))));
		BLOCKS.put("holo_command_table", HOLO_COMMAND_TABLE);
		HOLO_COMMAND_TABLE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("holo_command_table"),
				FabricBlockEntityTypeBuilder.create((pos, state) -> new io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity(
						HOLO_COMMAND_TABLE_ENTITY, pos, state), HOLO_COMMAND_TABLE).build());
		TOWER_CONTROL_CONSOLE = block("tower_control_console", new TowerControlConsoleBlock(properties("tower_control_console",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3F).noOcclusion().lightLevel(state -> 5))));
		BLOCKS.put("tower_control_console", TOWER_CONTROL_CONSOLE);
		TOWER_CONTROL_CONSOLE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("tower_control_console"),
				FabricBlockEntityTypeBuilder.create((pos, state) -> new io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity(
						TOWER_CONTROL_CONSOLE_ENTITY, pos, state), TOWER_CONTROL_CONSOLE).build());
		BLOCKS.put("raised_floor", block("raised_floor", new RaisedFloorBlock(properties("raised_floor",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.5F).noOcclusion()
						.sound(net.minecraft.world.level.block.SoundType.WOOL)))));
		BLOCKS.put("console_chair", block("console_chair", new ConsoleChairBlock(properties("console_chair",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.5F).noOcclusion()))));
		CONSOLE_CHAIR_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("console_chair"),
				FabricBlockEntityTypeBuilder.create(ConsoleChairBlock.ConsoleChairBlockEntity::new, BLOCKS.get("console_chair")).build());
		CURVED_VIDEO_WALL = block("curved_video_wall", new CurvedVideoWallBlock(properties("curved_video_wall",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3F).noOcclusion().lightLevel(state -> 7)
						.pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE))));
		BLOCKS.put("curved_video_wall", CURVED_VIDEO_WALL);
		// its other cells: no item (the wall's item places them all)
		BLOCKS.put("curved_video_wall_part", Registry.register(BuiltInRegistries.BLOCK, Jugcraft.id("curved_video_wall_part"),
				new CurvedVideoWallPartBlock(properties("curved_video_wall_part", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
						.strength(3F).noOcclusion().dynamicShape().lightLevel(state -> 7).pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE)))));
		CURVED_VIDEO_WALL_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("curved_video_wall"),
				FabricBlockEntityTypeBuilder.create((pos, state) -> new io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity(
						CURVED_VIDEO_WALL_ENTITY, pos, state), CURVED_VIDEO_WALL).build());
		DRONE_DISPLAY_CASE = block("drone_display_case", new DroneDisplayCaseBlock(properties("drone_display_case",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3F).noOcclusion()
						.pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE))));
		BLOCKS.put("drone_display_case", DRONE_DISPLAY_CASE);
		// its other cells: no item (the case's item places them all); the light hood's cells light the case and round it
		BLOCKS.put("drone_display_case_part", Registry.register(BuiltInRegistries.BLOCK, Jugcraft.id("drone_display_case_part"),
				new DroneDisplayCasePartBlock(properties("drone_display_case_part", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
						.strength(3F).noOcclusion().dynamicShape()
						.lightLevel(state -> state.getValue(DroneDisplayCasePartBlock.OY) == DroneDisplayCaseBlock.HEIGHT - 1 ? 12 : 0)
						.pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE)))));
		DRONE_DISPLAY_CASE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("drone_display_case"),
				FabricBlockEntityTypeBuilder.create((pos, state) -> new io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity(
						DRONE_DISPLAY_CASE_ENTITY, pos, state), DRONE_DISPLAY_CASE).build());
		CRYSTAL_TREE = block("crystal_tree", new CrystalTreeBlock(properties("crystal_tree",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3F).noOcclusion().lightLevel(state -> 9)
						.pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE))));
		BLOCKS.put("crystal_tree", CRYSTAL_TREE);
		// its other cells: no item (the tree's item places them all); the trunk's cells glow with its crystals' light
		BLOCKS.put("crystal_tree_part", Registry.register(BuiltInRegistries.BLOCK, Jugcraft.id("crystal_tree_part"),
				new CrystalTreePartBlock(properties("crystal_tree_part", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
						.strength(3F).noOcclusion().dynamicShape()
						.lightLevel(state -> state.getValue(CrystalTreePartBlock.OY) >= 1 ? 12 : 0)
						.pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE)))));
		CRYSTAL_TREE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("crystal_tree"),
				FabricBlockEntityTypeBuilder.create((pos, state) -> new io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity(
						CRYSTAL_TREE_ENTITY, pos, state), CRYSTAL_TREE).build());
		// the flight-ops trees (Justin, 5 November 2026; tools/ops_trees.py): the Ember Oak's master and trunk glow with its
		// embers, the Drift Isle's master and island cells with its lift beam; the beds' other cells give no light
		EMBER_OAK = block("ember_oak", new OpsTreeBlock(OpsTreeBlock.Kind.EMBER_OAK, properties("ember_oak",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3F).noOcclusion().lightLevel(state -> 10)
						.pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE))));
		BLOCKS.put("ember_oak", EMBER_OAK);
		BLOCKS.put("ember_oak_part", Registry.register(BuiltInRegistries.BLOCK, Jugcraft.id("ember_oak_part"),
				new OpsTreePartBlock(OpsTreeBlock.Kind.EMBER_OAK, properties("ember_oak_part", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
						.strength(3F).noOcclusion().dynamicShape()
						.lightLevel(state -> state.getValue(OpsTreePartBlock.OY) >= 1 ? 13 : 0)
						.pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE)))));
		DRIFT_ISLE = block("drift_isle", new OpsTreeBlock(OpsTreeBlock.Kind.DRIFT_ISLE, properties("drift_isle",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3F).noOcclusion().lightLevel(state -> 9)
						.pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE))));
		BLOCKS.put("drift_isle", DRIFT_ISLE);
		BLOCKS.put("drift_isle_part", Registry.register(BuiltInRegistries.BLOCK, Jugcraft.id("drift_isle_part"),
				new OpsTreePartBlock(OpsTreeBlock.Kind.DRIFT_ISLE, properties("drift_isle_part", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
						.strength(3F).noOcclusion().dynamicShape()
						.lightLevel(state -> state.getValue(OpsTreePartBlock.OY) == OpsTreeBlock.ISLE_DY ? 6 : 0)
						.pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE)))));
		OPS_TREE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("ops_tree"),
				FabricBlockEntityTypeBuilder.create((pos, state) -> new io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity(
						OPS_TREE_ENTITY, pos, state), EMBER_OAK, DRIFT_ISLE).build());
		// the atrium dome's cells (tools/atrium_dome.py): placed by the tower only, no item; concrete to break
		BLOCKS.put("dome_shell", Registry.register(BuiltInRegistries.BLOCK, Jugcraft.id("dome_shell"),
				new DomeShellBlock(properties("dome_shell", BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.STONE)
						.sound(net.minecraft.world.level.block.SoundType.STONE).strength(3F, 9F).noOcclusion()
						.pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE)))));
		// the lobby's Holo Projector (work list item 15); its other cells: no item (the projector's item places them all)
		HOLO_PROJECTOR = block("holo_projector", new HoloProjectorBlock(properties("holo_projector",
				Holograms.protect(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(state -> 11)))));
		BLOCKS.put("holo_projector", HOLO_PROJECTOR);
		BLOCKS.put("holo_projector_part", Registry.register(BuiltInRegistries.BLOCK, Jugcraft.id("holo_projector_part"),
				new HoloProjectorPartBlock(properties("holo_projector_part", Holograms.protect(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
						.noOcclusion().dynamicShape().lightLevel(state -> 7))))));
		HOLO_PROJECTOR_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("holo_projector"),
				FabricBlockEntityTypeBuilder.create((pos, state) -> new io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity(
						HOLO_PROJECTOR_ENTITY, pos, state), HOLO_PROJECTOR).build());
		// the supply cradles' raked struts (work list item 12); their other cells: no item (the struts' item places them all)
		CRADLE_STRUTS = block("cradle_struts", new CradleStrutsBlock(properties("cradle_struts",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(5F, 12F).noOcclusion()
						.pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE))));
		BLOCKS.put("cradle_struts", CRADLE_STRUTS);
		BLOCKS.put("cradle_struts_part", Registry.register(BuiltInRegistries.BLOCK, Jugcraft.id("cradle_struts_part"),
				new CradleStrutsPartBlock(properties("cradle_struts_part", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
						.strength(5F, 12F).noOcclusion().dynamicShape().pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE)))));
		CRADLE_STRUTS_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("cradle_struts"),
				FabricBlockEntityTypeBuilder.create((pos, state) -> new io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity(
						CRADLE_STRUTS_ENTITY, pos, state), CRADLE_STRUTS).build());
		// the map room's Big Map Table (work list item 16); its other cells: no item (the table's item places them all)
		BIG_MAP_TABLE = block("big_map_table", new BigMapTableBlock(properties("big_map_table",
				Holograms.protect(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()))));
		BLOCKS.put("big_map_table", BIG_MAP_TABLE);
		BLOCKS.put("big_map_table_part", Registry.register(BuiltInRegistries.BLOCK, Jugcraft.id("big_map_table_part"),
				new BigMapTablePartBlock(properties("big_map_table_part", Holograms.protect(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
						.noOcclusion().dynamicShape())))));
		BIG_MAP_TABLE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("big_map_table"),
				FabricBlockEntityTypeBuilder.create(BigMapTableBlockEntity::new, BIG_MAP_TABLE).build());
		// the light box over the map room's doors (as the data hall's)
		BLOCKS.put("map_room_sign", block("map_room_sign", new io.github.jimbozoomer.jugcraft.nuclear.MultiFurnitureBlock(
				properties("map_room_sign", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.5F).noOcclusion()
						.lightLevel(state -> 10)), 4, 1, true)));
		LONG_COFFEE_TABLE = block("long_coffee_table", new LongCoffeeTableBlock(properties("long_coffee_table",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2F).noOcclusion()
						.pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE))));
		BLOCKS.put("long_coffee_table", LONG_COFFEE_TABLE);
		// its other cells: no item (the table's item places them all)
		BLOCKS.put("long_coffee_table_part", Registry.register(BuiltInRegistries.BLOCK, Jugcraft.id("long_coffee_table_part"),
				new LongCoffeeTablePartBlock(properties("long_coffee_table_part", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
						.strength(2F).noOcclusion().dynamicShape().pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE)))));
		LONG_COFFEE_TABLE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("long_coffee_table"),
				FabricBlockEntityTypeBuilder.create((pos, state) -> new io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity(
						LONG_COFFEE_TABLE_ENTITY, pos, state), LONG_COFFEE_TABLE).build());
		// the workshop's Drone Parts Shelving (work list item 9); its other cells: no item (the shelving's item places them all)
		DRONE_PARTS_SHELVING = block("drone_parts_shelving", new DronePartsShelvingBlock(properties("drone_parts_shelving",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2F).noOcclusion()
						.pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE))));
		BLOCKS.put("drone_parts_shelving", DRONE_PARTS_SHELVING);
		BLOCKS.put("drone_parts_shelving_part", Registry.register(BuiltInRegistries.BLOCK, Jugcraft.id("drone_parts_shelving_part"),
				new DronePartsShelvingPartBlock(properties("drone_parts_shelving_part", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
						.strength(2F).noOcclusion().dynamicShape().pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE)))));
		DRONE_PARTS_SHELVING_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("drone_parts_shelving"),
				FabricBlockEntityTypeBuilder.create((pos, state) -> new io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity(
						DRONE_PARTS_SHELVING_ENTITY, pos, state), DRONE_PARTS_SHELVING).build());
		// the data hall's containment (work list item 13): the aisle doors (their other cells: no item), the roof panels, the sign
		AISLE_CONTAINMENT_DOOR = block("aisle_containment_door", new AisleContainmentDoorBlock(properties("aisle_containment_door",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2F).noOcclusion()
						.pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE))));
		BLOCKS.put("aisle_containment_door", AISLE_CONTAINMENT_DOOR);
		BLOCKS.put("aisle_containment_door_part", Registry.register(BuiltInRegistries.BLOCK, Jugcraft.id("aisle_containment_door_part"),
				new AisleContainmentDoorPartBlock(properties("aisle_containment_door_part", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
						.strength(2F).noOcclusion().dynamicShape().pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE)))));
		AISLE_CONTAINMENT_DOOR_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("aisle_containment_door"),
				FabricBlockEntityTypeBuilder.create(AisleContainmentDoorBlockEntity::new, AISLE_CONTAINMENT_DOOR).build());
		// the automatic sliding doors, like the aisle doors (Justin, 8 November 2026): the entrance's and the rooms' (glass, steel,
		// double, single); one part block for all their other cells (no item)
		java.util.List<Block> sliding = new java.util.ArrayList<>();
		for (SlidingDoorStyle style : SlidingDoorStyle.values()) {
			Block door = block(style.id(), new SlidingDoorBlock(style, properties(style.id(), BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
					.strength(2F).noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE))));
			BLOCKS.put(style.id(), door);
			sliding.add(door);
		}
		BLOCKS.put("sliding_door_part", Registry.register(BuiltInRegistries.BLOCK, Jugcraft.id("sliding_door_part"),
				new SlidingDoorPartBlock(properties("sliding_door_part", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
						.strength(2F).noOcclusion().dynamicShape().pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE)))));
		SLIDING_DOOR_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("sliding_door"),
				FabricBlockEntityTypeBuilder.create(SlidingDoorBlockEntity::new, sliding.toArray(new Block[0])).build());
		// a roof panel: its rail on the racks' top front edge, the panel and the LED bar over the aisle's half (y 0 to 0.64 px)
		furniture("aisle_containment_roof", 12, new double[] {0, 0, 0, 16, 0.64, 16});
		BLOCKS.put("data_hall_sign", block("data_hall_sign", new io.github.jimbozoomer.jugcraft.nuclear.MultiFurnitureBlock(
				properties("data_hall_sign", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.5F).noOcclusion()
						.lightLevel(state -> 10)), 4, 1, true)));
		furniture("equipment_rack", 3, new double[] {1, 0, 1, 15, 16, 15});
		hallFixtures();
		// the ceiling lights, with their night modes (the night lighting sheet; tower/TowerNightLights)
		lamp("ceiling_light_panel", new double[] {2, 14.25, 4, 14, 16, 12});
		lamp("linear_ceiling_light", new double[] {.6, 14.2, 5.5, 15.4, 16, 10.5});
		BLOCKS.put("wall_task_light", block("wall_task_light", new SurfaceTaskLightBlock(properties("wall_task_light",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.5F).noOcclusion().lightLevel(state -> 14)))));
		BLOCKS.put("cable_tray", block("cable_tray", new TowerCableTrayBlock(properties("cable_tray",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.5F).noOcclusion()))));
		furniture("tower_service_grating", 0, new double[] {0, 0, 0, 16, 3, 16});
		furniture("tower_service_railing", 0, new double[] {0, 0, 13, 16, 16, 16});
		furniture("tower_vent", 0, new double[] {0, 0, 0, 16, 16, 16});
		BLOCKS.put("tower_door_mechanism", block("tower_door_mechanism", new TowerDoorMechanismBlock(
				properties("tower_door_mechanism", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3F)))));
		BLOCKS.put("tower_ladder", block("tower_ladder", new net.minecraft.world.level.block.LadderBlock(
				properties("tower_ladder", BlockBehaviour.Properties.ofFullCopy(Blocks.LADDER).strength(2.5F))) { }));

		ResourceKey<net.minecraft.world.entity.EntityType<?>> seatKey = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id("seat"));
		SEAT = Registry.register(BuiltInRegistries.ENTITY_TYPE, seatKey, net.minecraft.world.entity.EntityType.Builder
				.<SeatEntity>of(SeatEntity::new, net.minecraft.world.entity.MobCategory.MISC).sized(0.001F, 0.001F).noSummon()
				.clientTrackingRange(10).build(seatKey));
		// the crew: a player's size, sitting as a player sits (the seat holds them by the player's attachment point)
		ResourceKey<net.minecraft.world.entity.EntityType<?>> crewKey = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id("tower_crew"));
		TOWER_CREW = Registry.register(BuiltInRegistries.ENTITY_TYPE, crewKey, net.minecraft.world.entity.EntityType.Builder
				.<io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew>of(io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew::new,
						net.minecraft.world.entity.MobCategory.MISC)
				.sized(0.6F, 1.8F).eyeHeight(1.62F).vehicleAttachment(new net.minecraft.world.phys.Vec3(0.0, 0.6, 0.0))
				.clientTrackingRange(10).build(crewKey));
		net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(TOWER_CREW,
				io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.attributes());
		// the crew drop ship: one entity on its landing point, its flight drawn from its timeline (seen from far off)
		ResourceKey<net.minecraft.world.entity.EntityType<?>> shipKey = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id("crew_ship"));
		CREW_SHIP = Registry.register(BuiltInRegistries.ENTITY_TYPE, shipKey, net.minecraft.world.entity.EntityType.Builder
				.<io.github.jimbozoomer.jugcraft.tower.crew.CrewShip>of(io.github.jimbozoomer.jugcraft.tower.crew.CrewShip::new,
						net.minecraft.world.entity.MobCategory.MISC)
				.sized(4.0F, 4.0F).noSummon().fireImmune().clientTrackingRange(20).updateInterval(20).build(shipKey));
		ResourceKey<net.minecraft.world.item.Item> shipItemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("crew_drop_ship"));
		CREW_SHIP_ITEM = Registry.register(BuiltInRegistries.ITEM, shipItemKey, new io.github.jimbozoomer.jugcraft.tower.crew.CrewShipItem(
				new net.minecraft.world.item.Item.Properties().setId(shipItemKey).stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));
		// the creative menu only
		net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.COMBAT)
				.register(output -> output.accept(CREW_SHIP_ITEM));
		SHIP_ENTRY = shipSound("crew_ship.entry");
		SHIP_THRUST = shipSound("crew_ship.thrust");
		SHIP_IDLE = shipSound("crew_ship.idle");
		SHIP_LIFTOFF = shipSound("crew_ship.liftoff");
		SHIP_RAMP = shipSound("crew_ship.ramp");
		SHIP_GUN_MOTOR = shipSound("crew_ship.door_gun_motor");
		io.github.jimbozoomer.jugcraft.tower.crew.CrewShipSeatPayload.register();   // right-click a landed ship's seat to sit
		io.github.jimbozoomer.jugcraft.tower.crew.TowerCrewCare.register();
		io.github.jimbozoomer.jugcraft.tower.crew.PlasmaRifle.register();     // security's rifle, its bolt, sounds and item
		io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBaton.register();     // the gate guards' baton, its sounds and item
		io.github.jimbozoomer.jugcraft.tower.squad.JugcraftSquads.register(); // drop pod squads: designator, pods, legionaries, requisitions
		ExchangePortBlock.register();
		ExchangeStations.register();
		CORE = block("drone_tower_core", new TowerCoreBlock(properties("drone_tower_core",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(5.0F, 1200F).lightLevel(state -> 6))));
		BLOCKS.put("drone_tower_core", CORE);
		CORE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("drone_tower_core"),
				FabricBlockEntityTypeBuilder.create(TowerCoreBlockEntity::new, CORE).build());
		ItemStorage.SIDED.registerForBlockEntity((core, side) -> core.blockStorage(), CORE_ENTITY);

		// the pillar doorways' Maintenance Door (work list item 10): a plant door (the plant doors' kit), placed as a pair
		ResourceKey<Block> doorKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("maintenance_door"));
		MAINTENANCE_DOOR = Registry.register(BuiltInRegistries.BLOCK, doorKey, new MaintenanceDoorBlock(
				io.github.jimbozoomer.jugcraft.nuclear.PlantInterior.STEEL, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_DOOR).strength(3.0F)
						.noOcclusion().lightLevel(MaintenanceDoorBlock::light).setId(doorKey)));
		ResourceKey<Item> doorItemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("maintenance_door"));
		Registry.register(BuiltInRegistries.ITEM, doorItemKey, new net.minecraft.world.item.DoubleHighBlockItem(MAINTENANCE_DOOR,
				new Item.Properties().setId(doorItemKey).useBlockDescriptionPrefix()));
		BLOCKS.put("maintenance_door", MAINTENANCE_DOOR);
		// the neon light lines (work list item 11; tools/neon_lines.py): in the amber boxes' and the end rods' places, real 3D
		// neon (tools/neon_3d.py); a groove or channel ends against its neighbour's face, so they let their neighbours draw it
		for (String mat : new String[] {"concrete", "steel", "carbon"}) {
			String id = "neon_line_" + mat;
			Block line = block(id, new NeonLineBlock(properties(id, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
					.strength(3.0F, 12F).noOcclusion().lightLevel(state -> NEON_LIGHT))));
			BLOCKS.put(id, line);
			NEON_LINES.add(line);
		}
		// the faced walls (Justin's first-floor look-over, 10 November 2026; tools/faced_walls.py MATS): a room's painted wall
		// with the hall's material on its face onto the hall, and the same with the Neon Line along that face
		for (String mat : new String[] {"concrete"}) {
			String wall = "faced_wall_" + mat, faced = "faced_line_" + mat;
			BLOCKS.put(wall, block(wall, new FacedWallBlock(properties(wall, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
					.strength(3.0F, 12F)))));
			BLOCKS.put(faced, block(faced, new FacedLineBlock(properties(faced, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
					.strength(3.0F, 12F).noOcclusion().lightLevel(state -> NEON_LIGHT)))));
		}
		for (String mat : new String[] {"concrete", "steel", "epoxy", "dock"}) {
			String id = "neon_floor_" + mat;
			BlockBehaviour.Properties props = properties(id, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F, 12F)
					.noOcclusion().lightLevel(state -> NEON_LIGHT));
			Block floor = block(id, mat.equals("concrete") || mat.equals("steel") ? new NeonFloorBlock.Phased(props) : new NeonFloorBlock(props));
			BLOCKS.put(id, floor);
			NEON_FLOORS.add(floor);
		}
		// the Inset Floor Light (redesign 2): where the layout had a floor line on its own
		for (String mat : new String[] {"concrete", "steel", "epoxy", "dock"}) {
			String id = "inset_light_" + mat;
			BlockBehaviour.Properties props = properties(id, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F, 12F)
					.lightLevel(state -> NEON_LIGHT));
			BLOCKS.put(id, block(id, mat.equals("concrete") || mat.equals("steel") ? new InsetFloorLightBlock.Phased(props) : new InsetFloorLightBlock(props)));
		}
		NEON_TUBE = block("neon_tube", new NeonTubeBlock(properties("neon_tube", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
				.strength(1.0F, 6F).noOcclusion().lightLevel(state -> NEON_TUBE_LIGHT))));
		BLOCKS.put("neon_tube", NEON_TUBE);
		// the upper floor's room kits (work list item 22 part B)
		RoomKit.register();
		RetiredBlocks.register();                                // (the Lounge TV: placed ones become Flat Screen TVs)
		List<Block> building = new ArrayList<>(BLOCKS.values());
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output -> {
			for (Block block : building) {
				// (the big pieces' *_part blocks have no item: an empty stack in a tab crashes the creative inventory; a
				// retired block isn't offered)
				if (block != CORE && block.asItem() != net.minecraft.world.item.Items.AIR && !RetiredBlocks.retired(block)) {
					output.accept(block);
				}
			}
			output.accept(MaintenanceDoorBlock.version(MAINTENANCE_DOOR, true, false));
			output.accept(MaintenanceDoorBlock.version(MAINTENANCE_DOOR, false, true));
			output.accept(MaintenanceDoorBlock.version(MAINTENANCE_DOOR, true, true));
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(CORE));
		TowerUpgradePayload.register();
		TowerPlacement.register();
		LOAD_MENU = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("tower_load"),
				new net.fabricmc.fabric.api.menu.v1.ExtendedMenuType<>(TowerLoadMenu::new, TowerLoadMenu.Opening.STREAM_CODEC));
		STORAGE_MENU = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("tower_storage"),
				new net.fabricmc.fabric.api.menu.v1.ExtendedMenuType<>(TowerStorageMenu::new, TowerStorageMenu.Opening.STREAM_CODEC));
		MapScan.register();
		io.github.jimbozoomer.jugcraft.world.ReviewWorldSettings.register();
	}

	/** The Tri-Pillar hall's live fittings (models: tools/tripillar_fixtures.py), and their block entity type. */
	private static void hallFixtures() {
		BLOCKS.put("touch_console", block("touch_console", new HallFixtureBlock(fixture("touch_console", 6), true,
				new double[] {3, 0, 3, 13, 12, 13}, new double[] {2, 12, 2, 14, 17, 14})));
		BLOCKS.put("tower_info_totem", block("tower_info_totem", new HallFixtureBlock(fixture("tower_info_totem", 7), false,
				new double[] {2, 0, 5, 14, 16, 11})));
		BLOCKS.put("holo_emitter", block("holo_emitter", new HallFixtureBlock.Emitter(Holograms.protect(fixture("holo_emitter", 9)),
				new double[] {2, 0, 2, 14, 2, 14})));
		BLOCKS.put("drone_display_stand", block("drone_display_stand", new HallFixtureBlock.Stand(fixture("drone_display_stand", 4),
				new double[] {4, 0, 4, 12, 3, 12})));
		// the service arm collides with its base, riser and turret (client/ServiceArm draws the robot; it stops while a
		// player stands in its keep-clear square)
		BLOCKS.put("drone_service_arm", block("drone_service_arm", new HallFixtureBlock(fixture("drone_service_arm", 0), false,
				new double[] {2, 0, 2, 14, 16, 14})));
		Block[] fixtures = java.util.Arrays.stream(HALL_FIXTURES).map(BLOCKS::get).toArray(Block[]::new);
		HALL_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("hall_fixture"),
				FabricBlockEntityTypeBuilder.create((pos, state) -> new io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity(
						HALL_ENTITY, pos, state), fixtures).build());
	}

	private static BlockBehaviour.Properties fixture(String id, int light) {
		return properties(id, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.5F).noOcclusion().lightLevel(state -> light));
	}

	private static void lamp(String id, double[]... shape) {
		BlockBehaviour.Properties props = properties(id, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.5F).noOcclusion());
		BLOCKS.put(id, block(id, new TowerLightBlock(props, shape)));
	}

	private static void furniture(String id, int light, double[]... shape) {
		BlockBehaviour.Properties props = properties(id, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.5F)
				.noOcclusion().lightLevel(state -> light));
		BLOCKS.put(id, block(id, new FurnitureBlock(props, shape)));
	}

	private static BlockBehaviour.Properties properties(String path, BlockBehaviour.Properties properties) {
		return properties.setId(ResourceKey.create(Registries.BLOCK, Jugcraft.id(path)));
	}

	/** Whether a block is one of the neon lines in a wall's material (the atrium's slab edge carries the status ticker).
	 * (Not a faced wall's line, {@link FacedLineBlock}: that is a block of its own with its own shape.) */
	public static boolean isNeonLine(net.minecraft.world.level.block.state.BlockState state) {
		return NEON_LINES.contains(state.getBlock());
	}

	static Block block(String path, Block block) {
		Registry.register(BuiltInRegistries.BLOCK, Jugcraft.id(path), block);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(path));
		Item.Properties properties = new Item.Properties().setId(itemKey).useBlockDescriptionPrefix();
		BlockItem item = block instanceof TowerCoreBlock ? new BlockItem(block, properties) {
			@Override
			protected boolean canPlace(net.minecraft.world.item.context.BlockPlaceContext context, net.minecraft.world.level.block.state.BlockState state) {
				// The whole tower must fit in the world (its mast under the build limit); several Drone Towers per player
				// per dimension, each outside the others' build radius; and the player confirms the spot first (the
				// warning with the finished tower's size: TowerPlacement).
				return super.canPlace(context, state) && TowerCoreBlock.mayPlaceHere(context.getLevel(), context.getPlayer(), context.getClickedPos())
						&& (context.getPlayer() == null || TowerCoreBlock.mayPlace(context.getLevel(), context.getPlayer(), context.getClickedPos()))
						&& TowerPlacement.confirmed(context);
			}

			@Override
			public void appendHoverText(net.minecraft.world.item.ItemStack stack, TooltipContext context,
					net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip,
					net.minecraft.world.item.TooltipFlag flag) {
				tooltip.accept(net.minecraft.network.chat.Component.translatable("tooltip.jugcraft.drone_tower_core")
						.withStyle(net.minecraft.ChatFormatting.GRAY));
				Map<Item, Integer> held = stack.get(TOWER_STORE);
				if (held != null && !held.isEmpty()) {
					tooltip.accept(net.minecraft.network.chat.Component.translatable("tooltip.jugcraft.drone_tower_core.store",
							TowerCosts.total(held), held.size()).withStyle(net.minecraft.ChatFormatting.GOLD));
				}
			}
		} : Holograms.ITEMS.contains(path) ? Holograms.item(block, properties) : new BlockItem(block, properties);
		Registry.register(BuiltInRegistries.ITEM, itemKey, item);
		return block;
	}

	private static net.minecraft.sounds.SoundEvent shipSound(String path) {
		net.minecraft.resources.Identifier id = Jugcraft.id(path);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, net.minecraft.sounds.SoundEvent.createVariableRangeEvent(id));
	}
}
