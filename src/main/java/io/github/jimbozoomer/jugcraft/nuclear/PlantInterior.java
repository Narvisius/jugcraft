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
			{"server_rack", 3, "stack", "metal", new double[] {1, 0, 1, 15, 16, 15.6}},
			{"relay_cabinet", 2, "stack", "metal", new double[] {1, 0, 2, 15, 16, 15.3}},
			{"pallet_rack", 0, "stack", "metal", new double[] {0, 0, 0, 16, 16, 16}},
			{"pallet_rack_drums", 0, "stack", "metal", new double[] {0, 0, 0, 16, 16, 16}},
			{"office_desk", 4, "plain", "wood", new double[] {0, 0, 1, 16, 12.5, 15}},
			{"filing_cabinet", 0, "plain", "metal", new double[] {2, 0, 2, 14, 15, 15}},
			{"whiteboard", 0, "variant:24", "metal", new double[] {0.5, 3, 0, 15.5, 15, 1.6}},
			{"water_cooler", 0, "plain", "metal", new double[] {4, 0, 4, 12, 16, 12}},
			{"guard_rail", 0, "plain", "metal", new double[] {0, 0, 0, 16, 16, 2}},
			{"turnstile", 3, "plain", "metal", new double[] {8.5, 0, 0.5, 15.5, 16, 15.5}},
			{"locker", 0, "stack", "metal", new double[] {1, 0, 2, 15, 16, 15}},
			{"locker_open", 0, "stack", "metal", new double[] {1, 0, 2, 15, 16, 15}},
			{"bookcase", 0, "stack", "wood", new double[] {1, 0, 1, 15, 16, 15}},
			{"fridge", 0, "stack", "metal", new double[] {1.5, 0, 2, 14.5, 16, 15}},
			{"fume_hood", 3, "stack", "metal", new double[] {0, 0, 1, 16, 16, 15.5}},
			{"reception_desk", 3, "row:reception", "wood", new double[] {0, 0, 0, 16, 16, 16}},
			{"sofa", 0, "row:sofa", "wool", new double[] {0, 0, 1, 16, 8, 16}},
			{"locker_bench", 0, "row:bench", "wood", new double[] {0, 0, 4, 16, 7, 12}},
			{"kitchen_counter", 0, "row:counter", "wood", new double[] {0, 0, 1, 16, 15, 16}},
			{"kitchen_sink", 0, "row:counter", "wood", new double[] {0, 0, 1, 16, 15, 16}},
			{"coffee_counter", 2, "row:counter", "wood", new double[] {0, 0, 1, 16, 15, 16}},
			{"lab_bench", 0, "row:lab", "metal", new double[] {0, 0, 1, 16, 14.5, 16}},
			{"lab_sink", 0, "row:lab", "metal", new double[] {0, 0, 1, 16, 14.5, 16}},
			{"lab_analyzer", 4, "row:lab", "metal", new double[] {0, 0, 1, 16, 14.5, 16}},
			{"cctv_desk", 6, "plain", "metal", new double[] {0, 0, 1, 16, 12.5, 15}},
			{"coffee_table", 0, "plain", "wood", new double[] {1, 0, 3, 15, 7, 13}},
			{"cafe_table", 0, "plain", "wood", new double[] {2, 0, 2, 14, 12.5, 14}},
			{"cafe_chair", 0, "plain", "wood", new double[] {3.5, 0, 3.5, 12.5, 9, 13}},
			{"coat_rack", 0, "plain", "wood", new double[] {1, 0, 0, 15, 16, 4}},
			{"printer", 3, "plain", "metal", new double[] {1, 0, 2, 15, 14, 14}},
			{"wall_display", 5, "plain", "metal", new double[] {0, 4, 0, 16, 14, 1.2}},
			{"office_plant", 0, "variant:8", "plant", new double[] {4, 0, 4, 12, 16, 12}},
			{"framed_print", 0, "variant:12", "wood", new double[] {1, 3, 0, 15, 14, 1}},
	};
	/** Plain full blocks: suspended ceiling, floors, walls, tank plate, louvres. */
	public static final String[] CUBES = {"ceiling_tile", "vinyl_floor", "epoxy_floor", "painted_wall", "tank_plate", "intake_louvre"};

	public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
	public static Block ROLLER_DOOR;
	public static Block BUILDING_SIGN;
	public static Block ROOM_SIGN;
	public static BlockEntityType<PlantSignBlockEntity> SIGN_ENTITY;

	/** Doors that open by hand, with steel door sounds. */
	public static final BlockSetType STEEL = new BlockSetType("jugcraft_steel", true, true, false, BlockSetType.PressurePlateSensitivity.EVERYTHING,
			SoundType.METAL, SoundEvents.IRON_DOOR_CLOSE, SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundEvents.IRON_TRAPDOOR_OPEN,
			SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF, SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON, SoundEvents.STONE_BUTTON_CLICK_OFF,
			SoundEvents.STONE_BUTTON_CLICK_ON);

	private PlantInterior() {
	}

	public static void register() {
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
				if (kind.equals("stack")) {
					return new StackingFurnitureBlock(p, shape);
				} else if (kind.startsWith("row:")) {
					return new RowFurnitureBlock(p, kind.substring(4), shape);
				} else if (kind.startsWith("variant:")) {
					return VariantFurnitureBlock.create(p, Integer.parseInt(kind.substring(8)), shape);
				}
				return new FurnitureBlock(p, shape);
			}, props);
		}
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
		ROLLER_DOOR = block("roller_door", RollerDoorBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F, 12F).noOcclusion());
		BUILDING_SIGN = block("building_sign", PlantSignBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.0F).noOcclusion()
				.noCollision());
		ROOM_SIGN = block("room_sign", PlantSignBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(0.5F).noOcclusion()
				.noCollision());
		SIGN_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("plant_sign"),
				FabricBlockEntityTypeBuilder.create(PlantSignBlockEntity::new, BUILDING_SIGN, ROOM_SIGN).build());
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output -> BLOCKS.values().forEach(output::accept));
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
