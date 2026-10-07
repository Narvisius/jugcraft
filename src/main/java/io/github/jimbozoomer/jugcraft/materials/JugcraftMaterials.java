package io.github.jimbozoomer.jugcraft.materials;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Jugcraft's shared base materials. IDs here are permanent once released. Keep this
 * list in sync with tools/materials.py, which generates the matching data files.
 */
public final class JugcraftMaterials {
	public static MetalFamily TIN;
	public static MetalFamily ZINC;
	public static MetalFamily LEAD;
	public static MetalFamily SILVER;
	public static MetalFamily NICKEL;
	public static MetalFamily TUNGSTEN;
	public static MetalFamily TITANIUM;
	public static MetalFamily URANIUM;
	/** The Earth school's green metal (docs/features/thallite.md); its ingot carries a lore line. */
	public static MetalFamily THALLITE;
	public static MetalFamily BRONZE;
	public static MetalFamily ALUMINUM;
	public static MetalFamily BRASS;
	public static MetalFamily INVAR;
	public static MetalFamily SOLDER;
	public static MetalFamily STEEL;

	public static MineralFamily SALT;
	public static MineralFamily PHOSPHATE;
	public static MineralFamily LEPIDOLITE;
	public static MineralFamily MONAZITE;

	public static Block BAUXITE;
	public static Block OIL_SAND;
	public static Block TINCAL;

	public static Item BITUMEN;
	/** Glass chemistry (batch 16). */
	public static Item BORAX;
	public static Item BOROSILICATE_GLASS;
	public static Item OPTICAL_FIBRE;
	public static Item FERROBORON;
	public static Item SULFUR_DUST;
	public static Item SILICON;
	public static Item LITHIUM_CARBONATE;
	public static Item RARE_EARTH_OXIDE;
	public static Item COKE;

	private JugcraftMaterials() {
	}

	public static void register() {
		TIN = MetalFamily.builder("tin").mined().build();
		ZINC = MetalFamily.builder("zinc").mined().build();
		LEAD = MetalFamily.builder("lead").mined().build();
		SILVER = MetalFamily.builder("silver").mined().build();
		NICKEL = MetalFamily.builder("nickel").mined().build();
		TUNGSTEN = MetalFamily.builder("tungsten").mined().build();
		URANIUM = MetalFamily.builder("uranium").mined().build();
		TITANIUM = MetalFamily.builder("titanium").mined().build();
		THALLITE = MetalFamily.builder("thallite").mined().lore().build();
		BRONZE = MetalFamily.builder("bronze").extraItem("bronze_blend").build();
		ALUMINUM = MetalFamily.builder("aluminum").build();
		BRASS = MetalFamily.builder("brass").build();
		INVAR = MetalFamily.builder("invar").build();
		SOLDER = MetalFamily.builder("solder").build();
		STEEL = MetalFamily.builder("steel").build();

		SALT = MineralFamily.register("salt");
		PHOSPHATE = MineralFamily.register("phosphate");
		LEPIDOLITE = MineralFamily.register("lepidolite");
		MONAZITE = MineralFamily.register("monazite");

		BAUXITE = JugcraftRegistry.block("bauxite", Blocks.GRANITE);
		OIL_SAND = JugcraftRegistry.block("oil_sand", Blocks.SAND);
		TINCAL = JugcraftRegistry.block("tincal", Blocks.SANDSTONE);

		BITUMEN = JugcraftRegistry.item("bitumen");
		SULFUR_DUST = JugcraftRegistry.item("sulfur_dust");
		SILICON = JugcraftRegistry.item("silicon");
		LITHIUM_CARBONATE = JugcraftRegistry.item("lithium_carbonate");
		RARE_EARTH_OXIDE = JugcraftRegistry.item("rare_earth_oxide");
		COKE = JugcraftRegistry.item("coke");
		BORAX = JugcraftRegistry.item("borax");
		BOROSILICATE_GLASS = JugcraftRegistry.item("borosilicate_glass");
		OPTICAL_FIBRE = JugcraftRegistry.item("optical_fibre");
		FERROBORON = JugcraftRegistry.item("ferroboron");

		registerCreativeTabs();
	}

	private static void registerCreativeTabs() {
		MetalFamily[] metals = {TIN, ZINC, LEAD, SILVER, NICKEL, TUNGSTEN, URANIUM, TITANIUM, THALLITE, BRONZE, ALUMINUM, BRASS, INVAR, SOLDER, STEEL};
		MineralFamily[] minerals = {SALT, PHOSPHATE, LEPIDOLITE, MONAZITE};

		List<ItemLike> natural = new ArrayList<>();
		List<ItemLike> ingredients = new ArrayList<>();
		List<ItemLike> building = new ArrayList<>();

		for (MetalFamily metal : metals) {
			if (metal.ore != null) {
				natural.add(metal.ore);
				natural.add(metal.deepslateOre);
				natural.add(metal.rawBlock);
				ingredients.add(metal.raw);
			}
			ingredients.addAll(metal.extras);
			ingredients.add(metal.ingot);
			ingredients.add(metal.nugget);
			building.add(metal.storageBlock);
		}
		for (MineralFamily mineral : minerals) {
			natural.add(mineral.ore);
			natural.add(mineral.deepslateOre);
			ingredients.add(mineral.item);
			building.add(mineral.storageBlock);
		}
		natural.add(BAUXITE);
		natural.add(OIL_SAND);
		natural.add(TINCAL);
		ingredients.addAll(List.of(BITUMEN, SULFUR_DUST, SILICON, LITHIUM_CARBONATE, RARE_EARTH_OXIDE, COKE, BORAX,
				BOROSILICATE_GLASS, OPTICAL_FIBRE, FERROBORON));

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(output -> natural.forEach(output::accept));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> ingredients.forEach(output::accept));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output -> building.forEach(output::accept));
	}
}
