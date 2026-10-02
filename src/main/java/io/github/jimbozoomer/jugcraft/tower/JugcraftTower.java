package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.materials.JugcraftRegistry;
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
			{"intake_funnel", "cube"}, {"hangar_pad", "light:6"}};
	/** Building materials that also come as stairs and slabs. */
	public static final String[] VARIANTS = {"reinforced_concrete", "hazard_plating", "steel_armor_plate", "aluminum_cladding",
			"tungsten_steel_frame", "carbon_composite_panel", "silicon_carbide_armor", "depleted_uranium_armor", "graphene_lattice"};
	public static final String[] MODULES = {"structural_module", "hangar_module", "armor_module", "avionics_module"};

	public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
	public static final Map<String, Item> MODULE_ITEMS = new LinkedHashMap<>();
	public static Block CORE;
	public static BlockEntityType<TowerCoreBlockEntity> CORE_ENTITY;
	public static net.minecraft.world.entity.EntityType<SeatEntity> SEAT;

	private JugcraftTower() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.TICKET_TYPE, Jugcraft.id("drone_tower"), TowerCoreBlockEntity.CHUNK_TICKET);
		for (String[] entry : BUILDING) {
			String id = entry[0];
			String kind = entry[1];
			BlockBehaviour.Properties props = properties(id, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
					.strength(id.contains("armor") || id.contains("tungsten") ? 6.0F : 3.0F, id.contains("uranium") ? 1200F : 12F));
			Block block = switch (kind) {
				case "pillar" -> new RotatedPillarBlock(props);
				case "glass" -> new TransparentBlock(properties(id, BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(3.0F, 12F))) {
				};
				default -> {
					if (id.equals("hangar_pad")) {
						yield new HangarPadBlock(props.lightLevel(state -> 6));
					}
					if (kind.startsWith("light:")) {
						int light = Integer.parseInt(kind.substring(6));
						yield new Block(props.lightLevel(state -> light));
					}
					yield new Block(props);
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
		furniture("console_desk", 4, new double[] {0, 0, 0, 16, 12, 16}, new double[] {1, 12, 2, 15, 16, 5});
		furniture("equipment_rack", 2, new double[] {1, 0, 1, 15, 16, 15});
		furniture("ceiling_light_panel", 15, new double[] {1, 14.5, 1, 15, 16, 15});
		furniture("cable_tray", 0, new double[] {0, 12, 4, 16, 16, 12});

		ResourceKey<net.minecraft.world.entity.EntityType<?>> seatKey = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id("seat"));
		SEAT = Registry.register(BuiltInRegistries.ENTITY_TYPE, seatKey, net.minecraft.world.entity.EntityType.Builder
				.<SeatEntity>of(SeatEntity::new, net.minecraft.world.entity.MobCategory.MISC).sized(0.001F, 0.001F).noSummon()
				.clientTrackingRange(10).build(seatKey));
		ExchangePortBlock.register();
		CORE = block("drone_tower_core", new TowerCoreBlock(properties("drone_tower_core",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(5.0F, 1200F).lightLevel(state -> 6))));
		BLOCKS.put("drone_tower_core", CORE);
		CORE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("drone_tower_core"),
				FabricBlockEntityTypeBuilder.create(TowerCoreBlockEntity::new, CORE).build());
		ItemStorage.SIDED.registerForBlockEntity((core, side) -> core.moduleStorage(), CORE_ENTITY);
		for (String module : MODULES) {
			MODULE_ITEMS.put(module, JugcraftRegistry.item(module));
		}

		List<Block> building = new ArrayList<>(BLOCKS.values());
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output -> {
			for (Block block : building) {
				if (block != CORE) {
					output.accept(block);
				}
			}
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(CORE));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> MODULE_ITEMS.values().forEach(output::accept));
		TowerUpgradePayload.register();
	}

	private static void furniture(String id, int light, double[]... shape) {
		BlockBehaviour.Properties props = properties(id, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.5F)
				.noOcclusion().lightLevel(state -> light));
		BLOCKS.put(id, block(id, new FurnitureBlock(props, shape)));
	}

	private static BlockBehaviour.Properties properties(String path, BlockBehaviour.Properties properties) {
		return properties.setId(ResourceKey.create(Registries.BLOCK, Jugcraft.id(path)));
	}

	private static Block block(String path, Block block) {
		Registry.register(BuiltInRegistries.BLOCK, Jugcraft.id(path), block);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(path));
		Item.Properties properties = new Item.Properties().setId(itemKey).useBlockDescriptionPrefix();
		BlockItem item = block instanceof TowerCoreBlock ? new BlockItem(block, properties) {
			@Override
			protected boolean canPlace(net.minecraft.world.item.context.BlockPlaceContext context, net.minecraft.world.level.block.state.BlockState state) {
				// Several Drone Towers per player per dimension, each outside the others' build radius.
				return super.canPlace(context, state) && (context.getPlayer() == null
						|| TowerCoreBlock.mayPlace(context.getLevel(), context.getPlayer(), context.getClickedPos()));
			}
		} : new BlockItem(block, properties);
		Registry.register(BuiltInRegistries.ITEM, itemKey, item);
		return block;
	}
}
