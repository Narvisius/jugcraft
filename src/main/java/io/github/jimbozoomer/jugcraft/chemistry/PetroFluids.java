package io.github.jimbozoomer.jugcraft.chemistry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import org.jspecify.annotations.Nullable;

/**
 * The petroleum fluids of the Chemistry branch's oil line (docs/branches/CHEMISTRY.md). Each is a real fluid with a
 * source and a flowing form, a liquid block and a bucket, so it works with Jugcraft's pumps, pipes and tanks and
 * with any other mod's through Fabric's fluid API. Names and flow come from FLUIDS in tools/petro.py.
 */
public final class PetroFluids {
	/** One fluid. Filled in by {@link #register}; the fluid classes read it once registration is done. */
	public static final class Entry {
		private final String id;
		private final int tickDelay;
		private final int slope;
		private final int dropOff;
		private final int color;
		private FlowingFluid source;
		private FlowingFluid flowing;
		private LiquidBlock block;
		private Item bucket;

		Entry(String id, int tickDelay, int slope, int dropOff, int color) {
			this.id = id;
			this.color = color;
			this.tickDelay = tickDelay;
			this.slope = slope;
			this.dropOff = dropOff;
		}

		public String id() {
			return id;
		}

		/** ARGB colour for tank gauges (the middle shade of its texture). */
		public int color() {
			return color;
		}

		public FlowingFluid source() {
			return source;
		}

		public FlowingFluid flowing() {
			return flowing;
		}

		public LiquidBlock block() {
			return block;
		}

		public Item bucket() {
			return bucket;
		}

		int tickDelay() {
			return tickDelay;
		}

		int slope() {
			return slope;
		}

		int dropOff() {
			return dropOff;
		}
	}

	/** A gas: a fluid with no block or bucket (see {@link GasFluid}), and its gauge colour. */
	public record Gas(String id, Fluid fluid, int color) {
	}

	public static final Map<String, Entry> FLUIDS = new LinkedHashMap<>();
	public static final Map<String, Gas> GASES = new LinkedHashMap<>();
	/** Liquid crude oil: from reservoirs and oil sand; refined into fuels. */
	public static Entry CRUDE_OIL;
	/** Distillation fractions: light naphtha (for gasoline), diesel, and heavy fuel oil (for cracking). */
	public static Entry NAPHTHA;
	public static Entry DIESEL;
	public static Entry HEAVY_FUEL_OIL;
	/** Reformed naphtha: high-octane gasoline. */
	public static Entry GASOLINE;
	/** Water thickened with sand and a gelling agent: what the fracking rig pumps down the well. */
	public static Entry FRACKING_FLUID;
	/** What comes back up a fracked well with the oil: dirty water, to be treated. */
	public static Entry FLOWBACK_WATER;
	/** Vacuum distillation of heavy fuel oil: lubricant (machine upkeep). */
	public static Entry LUBRICANT;
	/** The lightest fraction: refinery gas (fuel gas, and later plastics). */
	public static Gas REFINERY_GAS;
	/** Salt dissolved in water, for the electrolytic cell. */
	public static Entry BRINE;
	/** Sodium hydroxide solution from brine electrolysis: the base for digesting bauxite. */
	public static Entry LYE;
	/** Gases from brine electrolysis: chlorine (at the anode) and hydrogen (at the cathode). */
	public static Gas CHLORINE;
	public static Gas HYDROGEN;
	/** Sulfur burnt and absorbed in water: the acid for fertilizer and leaching. */
	public static Entry SULFURIC_ACID;
	public static Entry HYDROCHLORIC_ACID;
	public static Entry DIGESTATE;
	public static Gas RAW_BIOGAS;
	public static Gas CLEAN_BIOGAS;
	/** Nitrogen chemistry (batch 12): air separated into nitrogen and oxygen; ammonia from them and hydrogen. */
	public static Gas NITROGEN;
	public static Gas OXYGEN;
	public static Gas AMMONIA;
	/** The scarce third part of air (batch 13): a shielding gas. */
	public static Gas ARGON;
	/** Cracked out of naphtha and polymerized into synthetic rubber (batch 14). */
	public static Gas BUTADIENE;
	/** Chlorine joined to refinery gas, polymerized into PVC (batch 15). */
	public static Gas VINYL_CHLORIDE;
	/** Ammonia burnt in oxygen and absorbed in water (the Ostwald process). */
	public static Entry NITRIC_ACID;
	public static Entry ETHYLENE_DICHLORIDE;
	/** Vanadium leached into sulfuric acid (batch 17): the flow battery's electrolyte. */
	public static Entry VANADIUM_ELECTROLYTE;
	/** Crops fermented in the chemical reactor (batch 26): fuel for the gas turbine and the advanced engine. */
	public static Entry BIOETHANOL;
	/** Refinery upgrades (batch 29): hydrotreated diesel, gasoline blended with bioethanol, and the sour gas left over. */
	public static Entry PREMIUM_DIESEL;
	public static Entry PREMIUM_GASOLINE;
	public static Gas HYDROGEN_SULFIDE;
	/** Hydroponics (batch 33): fertilizer dissolved in water, for the hydroponic bay. */
	public static Entry NUTRIENT_SOLUTION;
	/** Batch 43: liquid fuels. */
	public static Entry KEROSENE;
	public static Entry LIQUID_OXYGEN;
	/** The starter gas and acid factory: hydrogen burnt in chlorine in the Chemical Infuser. */
	public static Gas HYDROGEN_CHLORIDE;
	/** The starter gas and acid factory: sulfur burnt in oxygen in the Chemical Oxidizer. */
	public static Gas SULFUR_DIOXIDE;
	/** Coal burnt in oxygen in the Chemical Oxidizer: the carbon feed of methanation. */
	public static Gas CARBON_DIOXIDE;
	/** Carbon dioxide and hydrogen over the Chemical Infuser's nickel bed: the Gas Burning Generator's second fuel. */
	public static Gas METHANE;
	public static Gas BIOGAS;
	public static Gas RAW_SYNGAS;
	public static Gas CARBON_MONOXIDE;
	public static Gas SULFUR_TRIOXIDE;

	public static Gas DIRTY_SYNTHESIS_GAS;

	public static Entry GRAIN_MASH;
	public static Entry ETHANOL_BROTH;
	public static Entry STILLAGE;
	public static Entry ALUMINATE_SLURRY;

	public static Gas FERMENTATION_EXHAUST;

	private PetroFluids() {
	}

	/** The entry for a petroleum fluid (source or flowing), or null for any other fluid. */
	public static @Nullable Entry of(Fluid fluid) {
		for (Entry entry : FLUIDS.values()) {
			if (fluid == entry.source || fluid == entry.flowing) {
				return entry;
			}
		}
		return null;
	}

	/** ARGB gauge colour for a petroleum fluid or gas, or 0 if it is neither. */
	public static int gaugeColor(Fluid fluid) {
		Entry entry = of(fluid);
		if (entry != null) {
			return entry.color();
		}
		for (Gas gas : GASES.values()) {
			if (gas.fluid() == fluid) {
				return gas.color();
			}
		}
		return 0;
	}

	public static Entry CONCENTRATED_SULFURIC_ACID;
	public static Entry CHLOROFORM;
	public static Gas HYDROGEN_FLUORIDE;
	public static Gas CHLORODIFLUOROMETHANE;
	public static Gas TETRAFLUOROETHYLENE;

	public static Entry OXYGENATED_PROCESS_WATER;
	public static Entry HYDROGEN_PEROXIDE;
	public static Entry ELECTRONIC_SULFURIC_ACID;
	public static Entry TMAH_DEVELOPER;
	public static Entry SEMICONDUCTOR_PHOTORESIST;
	public static Entry SPENT_WAFER_MIXTURE;
	public static void register() {

		CRUDE_OIL = fluid("crude_oil", 20, 2, 2, 0xFF1E1711, MapColor.COLOR_BLACK);
		NAPHTHA = fluid("naphtha", 5, 4, 1, 0xFFBEAA64, MapColor.COLOR_YELLOW);
		DIESEL = fluid("diesel", 8, 3, 1, 0xFFAA6E19, MapColor.COLOR_ORANGE);
		HEAVY_FUEL_OIL = fluid("heavy_fuel_oil", 30, 2, 2, 0xFF261E12, MapColor.COLOR_BLACK);
		LUBRICANT = fluid("lubricant", 25, 2, 2, 0xFF8C7D28, MapColor.COLOR_YELLOW);
		GASOLINE = fluid("gasoline", 4, 4, 1, 0xFFC86446, MapColor.COLOR_RED);
		FRACKING_FLUID = fluid("fracking_fluid", 6, 3, 1, 0xFF8296A0, MapColor.COLOR_LIGHT_GRAY);
		FLOWBACK_WATER = fluid("flowback_water", 5, 4, 1, 0xFF645C48, MapColor.COLOR_BROWN);
		REFINERY_GAS = gas("refinery_gas", 0xFFB8C4D0);
		BRINE = fluid("brine", 5, 4, 1, 0xFFB4C8D2, MapColor.COLOR_LIGHT_BLUE);
		LYE = fluid("lye", 6, 4, 1, 0xFFDCDCC0, MapColor.SNOW);
		CHLORINE = gas("chlorine", 0xFFB4D25A);
		HYDROGEN = gas("hydrogen", 0xFFDCE6F0);
		SULFURIC_ACID = fluid("sulfuric_acid", 6, 4, 1, 0xFFD2C850, MapColor.COLOR_YELLOW);
		NITROGEN = gas("nitrogen", 0xFF96A8D6);
		OXYGEN = gas("oxygen", 0xFF78B4E0);
		AMMONIA = gas("ammonia", 0xFFB096D4);
		ARGON = gas("argon", 0xFFC8A0E6);
		BUTADIENE = gas("butadiene", 0xFFC8D0AA);
		VINYL_CHLORIDE = gas("vinyl_chloride", 0xFFD6DEB6);
		NITRIC_ACID = fluid("nitric_acid", 5, 4, 1, 0xFFE2D48C, MapColor.COLOR_YELLOW);
		VANADIUM_ELECTROLYTE = fluid("vanadium_electrolyte", 6, 4, 1, 0xFF2C3E96, MapColor.COLOR_BLUE);
		BIOETHANOL = fluid("bioethanol", 4, 4, 1, 0xFFE2D6A4, MapColor.SAND);
		PREMIUM_DIESEL = fluid("premium_diesel", 8, 3, 1, 0xFFE4C464, MapColor.GOLD);
		PREMIUM_GASOLINE = fluid("premium_gasoline", 4, 4, 1, 0xFFECA082, MapColor.COLOR_PINK);
		HYDROGEN_SULFIDE = gas("hydrogen_sulfide", 0xFFCCCC7E);
		NUTRIENT_SOLUTION = fluid("nutrient_solution", 5, 4, 1, 0xFF6EAA64, MapColor.COLOR_GREEN);
		KEROSENE = fluid("kerosene", 5, 4, 1, 0xFFE0E0C4, MapColor.SAND);
		LIQUID_OXYGEN = fluid("liquid_oxygen", 3, 4, 1, 0xFFA0C8EC, MapColor.COLOR_LIGHT_BLUE);
		HYDROCHLORIC_ACID = fluid("hydrochloric_acid", 5, 4, 1, 0xFFD2DCB4, MapColor.COLOR_LIGHT_GREEN);
		ETHYLENE_DICHLORIDE = fluid("ethylene_dichloride", 5, 4, 1, 0xFFCCD4A0, MapColor.COLOR_YELLOW);
		HYDROGEN_CHLORIDE = gas("hydrogen_chloride", 0xFFD4DCDA);
		SULFUR_DIOXIDE = gas("sulfur_dioxide", 0xFFDAD6B8);
		CARBON_DIOXIDE = gas("carbon_dioxide", 0xFFC4CAD4);
		METHANE = gas("methane", 0xFFAED4D0);
		BIOGAS = gas("biogas", 0xFFB8C2AA);
		RAW_SYNGAS = gas("raw_syngas", 0xFF9A9286);
		CARBON_MONOXIDE = gas("carbon_monoxide", 0xFFC2BED2);
		SULFUR_TRIOXIDE = gas("sulfur_trioxide", 0xFFE2DCBC);


		DIRTY_SYNTHESIS_GAS = gas("dirty_synthesis_gas", 0xFF625B4A);
		DIGESTATE = fluid("digestate", 12, 2, 2, 0xFF5D4E2F, MapColor.COLOR_BROWN);
		RAW_BIOGAS = gas("raw_biogas", 0xFF697446);
		CLEAN_BIOGAS = gas("clean_biogas", 0xFF76965E);

		GRAIN_MASH = fluid("grain_mash", 8, 3, 1, 0xFF77552B, MapColor.COLOR_BROWN);
		ETHANOL_BROTH = fluid("ethanol_broth", 8, 3, 1, 0xFF8B7240, MapColor.COLOR_BROWN);
		STILLAGE = fluid("stillage", 8, 3, 1, 0xFF5E452A, MapColor.COLOR_BROWN);
		ALUMINATE_SLURRY = fluid("aluminate_slurry", 8, 3, 1, 0xFF7D452E, MapColor.COLOR_BROWN);

		FERMENTATION_EXHAUST = gas("fermentation_exhaust", 0xFF928C76);

		SEAWATER = fluid("seawater", 8, 3, 1, 0xFF30687C, MapColor.COLOR_LIGHT_GRAY);
		MAGNESIUM_BRINE = fluid("magnesium_brine", 8, 3, 1, 0xFF5B7965, MapColor.COLOR_LIGHT_GRAY);
		MAGNESIUM_CHLORIDE_SOLUTION = fluid("magnesium_chloride_solution", 8, 3, 1, 0xFF77818B, MapColor.COLOR_LIGHT_GRAY);
		CRUDE_TITANIUM_CHLORIDE = fluid("crude_titanium_chloride", 8, 3, 1, 0xFF7D683B, MapColor.COLOR_LIGHT_GRAY);
		PURIFIED_TITANIUM_CHLORIDE = fluid("purified_titanium_chloride", 8, 3, 1, 0xFF6E8088, MapColor.COLOR_LIGHT_GRAY);
		SPENT_MINERAL_WASH = fluid("spent_mineral_wash", 8, 3, 1, 0xFF5A6948, MapColor.COLOR_LIGHT_GRAY);

		ETHYLENE = gas("ethylene", 0xFF648787);

		CONCENTRATED_SULFURIC_ACID = fluid("concentrated_sulfuric_acid", 8, 3, 1, 0xFF756E33, MapColor.COLOR_LIGHT_GRAY);
		CHLOROFORM = fluid("chloroform", 8, 3, 1, 0xFF567769, MapColor.COLOR_LIGHT_GRAY);
		HYDROGEN_FLUORIDE = gas("hydrogen_fluoride", 0xFF4E7B67);
		CHLORODIFLUOROMETHANE = gas("chlorodifluoromethane", 0xFF59708D);
		TETRAFLUOROETHYLENE = gas("tetrafluoroethylene", 0xFF69808A);

		OXYGENATED_PROCESS_WATER = fluid("oxygenated_process_water", 8, 3, 1, 0xFF4C6E77, MapColor.COLOR_LIGHT_GRAY);
		HYDROGEN_PEROXIDE = fluid("hydrogen_peroxide", 8, 3, 1, 0xFF5C7B8B, MapColor.COLOR_LIGHT_GRAY);
		ELECTRONIC_SULFURIC_ACID = fluid("electronic_sulfuric_acid", 8, 3, 1, 0xFF757B51, MapColor.COLOR_LIGHT_GRAY);
		TMAH_DEVELOPER = fluid("tmah_developer", 8, 3, 1, 0xFF537763, MapColor.COLOR_LIGHT_GRAY);
		SEMICONDUCTOR_PHOTORESIST = fluid("semiconductor_photoresist", 8, 3, 1, 0xFF7D4C2C, MapColor.COLOR_LIGHT_GRAY);
		SPENT_WAFER_MIXTURE = fluid("spent_wafer_mixture", 8, 3, 1, 0xFF534E67, MapColor.COLOR_LIGHT_GRAY);

		PHOSPHATE_SLURRY = fluid("phosphate_slurry", 8, 3, 1, 0xFF5B573E, MapColor.COLOR_LIGHT_GRAY);
		PHOSPHORIC_ACID = fluid("phosphoric_acid", 8, 3, 1, 0xFF60786C, MapColor.COLOR_LIGHT_GRAY);

		SYNTHETIC_CRUDE = fluid("synthetic_crude", 8, 3, 1, 0xFF423927, MapColor.COLOR_LIGHT_GRAY);
		TREATED_PROCESS_WATER = fluid("treated_process_water", 8, 3, 1, 0xFF4C655F, MapColor.COLOR_LIGHT_GRAY);
		RECOVERY_CONCENTRATE = fluid("recovery_concentrate", 8, 3, 1, 0xFF524656, MapColor.COLOR_LIGHT_GRAY);
		LIQUID_METHANE = fluid("liquid_methane", 8, 3, 1, 0xFF48656D, MapColor.COLOR_LIGHT_GRAY);
		LIQUID_HYDROGEN = fluid("liquid_hydrogen", 8, 3, 1, 0xFF51607C, MapColor.COLOR_LIGHT_GRAY);
		HYDRAZINE = fluid("hydrazine", 8, 3, 1, 0xFF606749, MapColor.COLOR_LIGHT_GRAY);
		DINITROGEN_TETROXIDE = fluid("dinitrogen_tetroxide", 8, 3, 1, 0xFF734830, MapColor.COLOR_LIGHT_GRAY);

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			for (Entry entry : FLUIDS.values()) {
				output.accept(entry.bucket());
			}
		});
	}

	public static Entry LIQUID_METHANE;
	public static Entry LIQUID_HYDROGEN;
	public static Entry HYDRAZINE;
	public static Entry DINITROGEN_TETROXIDE;
	public static Entry SYNTHETIC_CRUDE;
	public static Entry TREATED_PROCESS_WATER;
	public static Entry RECOVERY_CONCENTRATE;
	public static Entry PHOSPHATE_SLURRY;
	public static Entry PHOSPHORIC_ACID;
	public static Entry SEAWATER;
	public static Entry MAGNESIUM_BRINE;
	public static Entry MAGNESIUM_CHLORIDE_SOLUTION;
	public static Entry CRUDE_TITANIUM_CHLORIDE;
	public static Entry PURIFIED_TITANIUM_CHLORIDE;
	public static Entry SPENT_MINERAL_WASH;

	public static Gas ETHYLENE;

	private static Gas gas(String id, int color) {
		Gas gas = new Gas(id, Registry.register(BuiltInRegistries.FLUID, Jugcraft.id(id), new GasFluid()), color);
		GASES.put(id, gas);
		return gas;
	}

	/** Register an additional native liquid through the same pump, pipe, bucket and gauge implementation. */
	public static Entry registerOwnerLiquid(String id, int gauge) {
		if (FLUIDS.containsKey(id)) throw new IllegalStateException("Duplicate liquid " + id);
		return fluid(id, 5, 4, 1, gauge, MapColor.WATER);
	}

	private static Entry fluid(String id, int tickDelay, int slope, int dropOff, int gauge, MapColor color) {
		Entry entry = new Entry(id, tickDelay, slope, dropOff, gauge);
		entry.source = Registry.register(BuiltInRegistries.FLUID, Jugcraft.id(id), new OilFluid.Source(entry));
		entry.flowing = Registry.register(BuiltInRegistries.FLUID, Jugcraft.id("flowing_" + id), new OilFluid.Flowing(entry));
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id(id));
		entry.block = Registry.register(BuiltInRegistries.BLOCK, blockKey,
				new LiquidBlock(entry.source, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).mapColor(color).setId(blockKey)) {
				});
		ResourceKey<Item> bucketKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(id + "_bucket"));
		entry.bucket = Registry.register(BuiltInRegistries.ITEM, bucketKey,
				new BucketItem(entry.source, new Item.Properties().setId(bucketKey).stacksTo(1).craftRemainder(Items.BUCKET)));
		FLUIDS.put(id, entry);
		return entry;
	}
}
