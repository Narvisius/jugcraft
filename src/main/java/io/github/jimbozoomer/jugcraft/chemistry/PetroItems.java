package io.github.jimbozoomer.jugcraft.chemistry;

import io.github.jimbozoomer.jugcraft.materials.JugcraftRegistry;
import io.github.jimbozoomer.jugcraft.weapons.GrenadeItem;
import io.github.jimbozoomer.jugcraft.weapons.GrenadeLauncherItem;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import io.github.jimbozoomer.jugcraft.weapons.DescribedItem;
import net.minecraft.world.item.Items;

/** Plain items of the oil line (ITEMS in tools/petro.py). */
public final class PetroItems {
	/** Used up by the catalytic cracker, one per bucket of heavy fuel oil. */
	public static Item CRACKING_CATALYST;
	/** The residue of vacuum distillation (in the distillation tower): binds gravel into asphalt. */
	public static Item ASPHALT_BINDER;
	/** Polymerized refinery gas: the metal press flattens each into a plastic sheet. */
	public static Item PLASTIC_PELLETS;
	/** Pressed plastic, for parts of later machines (first used in the diesel engine). */
	public static Item PLASTIC_SHEET;
	/** Bauxite digested in hot lye (the Bayer process): smelted into aluminum in the electrolytic cell. */
	public static Item ALUMINA;
	/** Superphosphate: ripens the crops in a 5x5 area ({@link FertilizerItem}). */
	public static Item FERTILIZER;
	/** The Kroll process: raw titanium chlorinated with coke and reduced, a porous sponge the arc furnace melts. */
	public static Item TITANIUM_SPONGE;
	/** A lithium cell in an aluminum can: crafted from lithium carbonate, built into the lithium battery bank. */
	public static Item LITHIUM_CELL;
	/** Rare earths alloyed with iron in the alloy smelter: for the magnet dynamo and magnet motor. */
	public static Item NEODYMIUM_MAGNET;
	/** Electronics (batch 7): a doped silicon crystal pulled in the arc furnace (batch 24), sawn into wafers. */
	public static Item SILICON_BOULE;
	public static Item SILICON_WAFER;
	/** Wafers etched in the lithography station: four chips each. */
	public static Item MICROCHIP;
	public static Item RUBBER;
	public static Item GASKET;
	public static Item PVC_RESIN;
	public static Item SOAP;
	/** Explosive weapons (batch 18): nitrated cotton, the grenade's charge; grenades and their launcher. */
	public static Item GUNCOTTON;
	public static Item GRENADE;
	public static Item GRENADE_LAUNCHER;
	/** Power (batch 19): fitted in the advanced engine's slot for half as much power again. */
	public static Item TURBOCHARGER;
	/** The Chemical Infuser's reusable methanation catalyst: held in its bed socket, never used up. */
	public static Item NICKEL_CATALYST_BED;
	public static Item VANADIUM_PENTOXIDE;
	public static Item GASIFICATION_RESIDUE;
	public static Item AMMONIUM_NITRATE;
	/** Boron (docs/features/boric-acid.md): boric acid from borax, and the reactor's control rod assembly made with it. */
	public static Item BORIC_ACID;
	public static Item CONTROL_ROD_ASSEMBLY;
	/** Food chemistry (docs/features/sour-candy.md): citric acid, a candy flavour, and glucose syrup, which crafts into sugar. */
	public static Item CITRIC_ACID;
	public static Item GLUCOSE_SYRUP;
	/** PVAc wood glue (docs/features/pvac-glue.md): binds books and makes sticky pistons. */
	public static Item PVAC_GLUE;
	/** Boron in solar cells (docs/features/boron-solar.md): the Advanced Solar Panel's doped silicon. */
	public static Item BORON_DOPED_SILICON;
	/** Silicone rubber (docs/features/silicone.md): pressed into heat-proof gaskets. */
	public static Item SILICONE;
	public static Item RUTHENIUM_POWDER;
	public static Item RUTHENIUM_CATALYST_BED;
	/** Bone meal doses ammonium nitrate gives each crop. */
	public static final int AMMONIUM_NITRATE_DOSES = 3;
	public static Item VANADIUM_CONTACT_BED;

	public static Item VANADIUM_IRON_CONCENTRATE;
	public static Item VANADIUM_OXIDE;
	public static Item MIXED_GAS_RESIDUE;
	public static Item OXIDIZER_BURNER;
	public static Item DIGESTATE_FIBER;

	public static Item ALUMINUM_HYDROXIDE;
	public static Item BAUXITE_RESIDUE;

	public static Item MAGNESIUM_HYDROXIDE;
	public static Item DRY_MAGNESIUM_CHLORIDE;
	public static Item MAGNESIUM_INGOT;
	public static Item POLYMER_HOUSING;
	public static Item POLYMER_FITTING;
	public static Item CABLE_INSULATION;
	public static Item FLEXIBLE_HOSE;
	public static Item POLYMER_VEHICLE_PANEL;
	public static Item HOUSING_MOLD;
	public static Item FITTING_MOLD;
	public static Item INSULATION_MOLD;
	public static Item GASKET_MOLD;
	public static Item HOSE_MOLD;
	public static Item PANEL_MOLD;

	public static Item HDPE_PELLETS;
	public static Item LDPE_PELLETS;
	public static Item LDPE_FILM;
	public static Item PVC_PROFILE;
	public static Item HDPE_PROCESS_INSERT;
	public static Item LDPE_PROCESS_INSERT;
	public static Item SHEET_EXTRUSION_DIE;
	public static Item FILM_EXTRUSION_DIE;
	public static Item PROFILE_EXTRUSION_DIE;
	public static Item HOSE_EXTRUSION_DIE;
	public static Item INSULATION_EXTRUSION_DIE;
	public static Item HDPE_OFFCUT;
	public static Item LDPE_OFFCUT;
	public static Item PVC_OFFCUT;
	public static Item RUBBER_OFFCUT;

	private PetroItems() {
	}

	public static Item FLUORITE_CONCENTRATE;
	public static Item CALCIUM_SULFATE;
	public static Item PTFE_POWDER;
	public static Item PTFE_PROCESS_INSERT;
	public static Item PTFE_PASTE_CHARGE;
	public static Item PTFE_SEAL_BLANK;
	public static Item PTFE_FITTING_BLANK;
	public static Item PTFE_LINER_BLANK;
	public static Item PTFE_SEAL;
	public static Item PTFE_FITTING;
	public static Item PTFE_LINER;
	public static Item PTFE_OFFCUT;
	public static Item GYPSUM_BINDER;

	// Advanced ceramics and precision components; stable IDs, owner package 1.
	public static Item CLAY_POWDER;
	public static Item PORCELAIN_BATCH;
	public static Item PORCELAIN_CHARGE;
	public static Item PORCELAIN_BLANK;
	public static Item FIRED_PORCELAIN_BLANK;
	public static Item PORCELAIN_INSULATOR;
	public static Item ALUMINA_CHARGE;
	public static Item ALUMINA_BLANK;
	public static Item FIRED_ALUMINA_BLANK;
	public static Item ALUMINA_PLATE;
	public static Item CERAMIC_BEARING_SET;
	public static Item ZIRCON_CONCENTRATE;
	public static Item ZIRCONIA_PRECURSOR;
	public static Item ZIRCONIA_POWDER;
	public static Item ZIRCONIA_CHARGE;
	public static Item ZIRCONIA_BLANK;
	public static Item FIRED_ZIRCONIA_BLANK;
	public static Item ZIRCONIA_INSERT;
	public static Item STEEL_SHAFT_BLANK;
	public static Item PRECISION_SHAFT;
	public static Item PRECISION_BEARING;
	public static Item OPTICAL_BLANK;
	public static Item POLISHED_LENS;
	public static Item ABRASIVE_HEAD;
	public static Item POLISHING_HEAD;

	// Chemical and structural alloy components, owner package 2.
	public static Item CHROMITE_CONCENTRATE;
	public static Item STAINLESS_FITTING;
	public static Item STAINLESS_VESSEL;
	public static Item STAINLESS_VALVE_BLANK;
	public static Item PRECISION_STAINLESS_VALVE;
	public static Item LIGHTWEIGHT_FRAME;
	public static Item ALLOY_PANEL;
	public static Item GALENA_CONCENTRATE;
	public static Item LEAD_ELECTRODE;
	public static Item LEAD_OXIDE_ELECTRODE;
	public static Item BATTERY_SEPARATOR;
	public static Item LEAD_ACID_CELL;
	public static Item ACTIVATED_SPODUMENE;
	public static Item LITHIUM_FLUORIDE;
	public static Item FLUOROPHOSPHATE_PRECURSOR;
	public static Item LIPF6_SALT;
	public static Item CARBONATE_SOLVENT_CHARGE;
	public static Item LITHIUM_ELECTROLYTE_CHARGE;
	public static Item LITHIUM_CATHODE;
	public static Item CARBON_ANODE;
	public static Item VANADYL_SULFATE;
	public static Item FLOW_CELL_STACK;
	public static Item LITHIUM_CELL_ASSEMBLY;
	public static Item DRY_LEAD_CELL;
	public static Item LFP_CATHODE_MIX;


	public static Item HIGH_PURITY_SILICON;
	public static Item HIGH_PURITY_BOULE;
	public static Item ROUGH_PROCESS_WAFER;
	public static Item POLISHED_PROCESS_WAFER;
	public static Item CLEAN_PROCESS_WAFER;
	public static Item COATED_PROCESS_WAFER;
	public static Item EXPOSED_PROCESS_WAFER;
	public static Item DEVELOPED_PROCESS_WAFER;
	public static Item ETCHED_PROCESS_WAFER;
	public static Item GENERAL_CHIP_PATTERN;
	public static Item ADVANCED_CHIP;
	public static Item CRYOGENIC_INSULATION;
	public static Item METHANE_COOLING_MODULE;
	public static Item HYDROGEN_COOLING_MODULE;
	public static Item METHANE_TANK;
	public static Item HYDROGEN_TANK;
	public static Item HYDRAZINE_TANK;
	public static Item N2O4_TANK;
	public static Item METHALOX_MOTOR;
	public static Item HYDROLOX_MOTOR;
	public static Item HYDRAZINE_THRUSTER;
	public static Item STORABLE_BIPROPELLANT_MOTOR;
	public static Item PROPELLANT_RESIDUE;
	public static Item COBALT_CONCENTRATE;
	public static Item COBALT_INGOT;
	public static Item COBALT_CATALYST_BED;
	public static Item STABILIZED_PROCESS_RESIDUE;
	public static Item GROUND_PHOSPHATE;
	public static Item WET_SUPERPHOSPHATE;
	public static Item TRIPLE_SUPERPHOSPHATE;
	public static Item NITROGEN_FERTILIZER;
	public static Item MIXED_FARM_BLEND;
	public static Item ORGANIC_FERTILIZER_CARRIER;
	public static Item GYPSUM_RICH_RESIDUE;
	public static Item WASHED_GYPSUM;

	public static Item NOVOLAC_RESIN;
	public static Item DNQ_ADDITIVE;
	public static Item DEVELOPER_CHARGE;

	public static void register() {

		CRACKING_CATALYST = JugcraftRegistry.item("cracking_catalyst");
		ASPHALT_BINDER = JugcraftRegistry.item("asphalt_binder");
		PLASTIC_PELLETS = JugcraftRegistry.item("plastic_pellets");
		PLASTIC_SHEET = JugcraftRegistry.item("plastic_sheet");
		ALUMINA = JugcraftRegistry.item("alumina");
		FERTILIZER = JugcraftRegistry.item("fertilizer", FertilizerItem::new);
		TITANIUM_SPONGE = JugcraftRegistry.item("titanium_sponge");
		LITHIUM_CELL = JugcraftRegistry.item("lithium_cell");
		NEODYMIUM_MAGNET = JugcraftRegistry.item("neodymium_magnet");
		SILICON_BOULE = JugcraftRegistry.item("silicon_boule");
		SILICON_WAFER = JugcraftRegistry.item("silicon_wafer");
		MICROCHIP = JugcraftRegistry.item("microchip");
		RUBBER = JugcraftRegistry.item("rubber");
		GASKET = JugcraftRegistry.item("gasket");
		PVC_RESIN = JugcraftRegistry.item("pvc_resin");
		SOAP = JugcraftRegistry.item("soap", SoapItem::new);
		GUNCOTTON = JugcraftRegistry.item("guncotton");
		GRENADE = JugcraftRegistry.item("grenade", GrenadeItem::new);
		GRENADE_LAUNCHER = JugcraftRegistry.item("grenade_launcher", GrenadeLauncherItem::new);
		TURBOCHARGER = JugcraftRegistry.item("turbocharger", properties -> new Item(properties.stacksTo(1)));
		NICKEL_CATALYST_BED = JugcraftRegistry.item("nickel_catalyst_bed", properties -> new Item(properties.stacksTo(1)));
		VANADIUM_PENTOXIDE = JugcraftRegistry.item("vanadium_pentoxide");
		VANADIUM_CONTACT_BED = JugcraftRegistry.item("vanadium_contact_bed", properties -> new Item(properties.stacksTo(1)));
		GASIFICATION_RESIDUE = JugcraftRegistry.item("gasification_residue");
		// A nitrogen fertilizer: superphosphate's 5x5 area, three doses a crop instead of two.
		AMMONIUM_NITRATE = JugcraftRegistry.item("ammonium_nitrate",
				properties -> new FertilizerItem(properties, FertilizerItem.RADIUS, AMMONIUM_NITRATE_DOSES));
		BORIC_ACID = JugcraftRegistry.item("boric_acid");
		CONTROL_ROD_ASSEMBLY = JugcraftRegistry.item("control_rod_assembly", properties -> new Item(properties.stacksTo(16)));
		CITRIC_ACID = JugcraftRegistry.item("citric_acid");
		GLUCOSE_SYRUP = JugcraftRegistry.item("glucose_syrup",
				properties -> new Item(properties.stacksTo(16).craftRemainder(Items.GLASS_BOTTLE)));
		PVAC_GLUE = JugcraftRegistry.item("pvac_glue");
		BORON_DOPED_SILICON = JugcraftRegistry.item("boron_doped_silicon");
		SILICONE = JugcraftRegistry.item("silicone");
		RUTHENIUM_POWDER = JugcraftRegistry.item("ruthenium_powder");
		RUTHENIUM_CATALYST_BED = JugcraftRegistry.item("ruthenium_catalyst_bed", properties -> new Item(properties.stacksTo(1)));

		VANADIUM_IRON_CONCENTRATE = JugcraftRegistry.item("vanadium_iron_concentrate");
		VANADIUM_OXIDE = JugcraftRegistry.item("vanadium_oxide");
		MIXED_GAS_RESIDUE = JugcraftRegistry.item("mixed_gas_residue");
		OXIDIZER_BURNER = JugcraftRegistry.item("oxidizer_burner");
		DIGESTATE_FIBER = JugcraftRegistry.item("digestate_fiber");
		ALUMINUM_HYDROXIDE = JugcraftRegistry.item("aluminum_hydroxide");
		BAUXITE_RESIDUE = JugcraftRegistry.item("bauxite_residue");

		MAGNESIUM_HYDROXIDE = JugcraftRegistry.item("magnesium_hydroxide");
		DRY_MAGNESIUM_CHLORIDE = JugcraftRegistry.item("dry_magnesium_chloride");
		MAGNESIUM_INGOT = JugcraftRegistry.item("magnesium_ingot");
		POLYMER_HOUSING = JugcraftRegistry.item("polymer_housing");
		POLYMER_FITTING = JugcraftRegistry.item("polymer_fitting");
		CABLE_INSULATION = JugcraftRegistry.item("cable_insulation");
		FLEXIBLE_HOSE = JugcraftRegistry.item("flexible_hose");
		POLYMER_VEHICLE_PANEL = JugcraftRegistry.item("polymer_vehicle_panel");
		HOUSING_MOLD = JugcraftRegistry.item("housing_mold");
		FITTING_MOLD = JugcraftRegistry.item("fitting_mold");
		INSULATION_MOLD = JugcraftRegistry.item("insulation_mold");
		GASKET_MOLD = JugcraftRegistry.item("gasket_mold");
		HOSE_MOLD = JugcraftRegistry.item("hose_mold");
		PANEL_MOLD = JugcraftRegistry.item("panel_mold");

		HDPE_PELLETS = JugcraftRegistry.item("hdpe_pellets");
		LDPE_PELLETS = JugcraftRegistry.item("ldpe_pellets");
		LDPE_FILM = JugcraftRegistry.item("ldpe_film");
		PVC_PROFILE = JugcraftRegistry.item("pvc_profile");
		HDPE_PROCESS_INSERT = JugcraftRegistry.item("hdpe_process_insert");
		LDPE_PROCESS_INSERT = JugcraftRegistry.item("ldpe_process_insert");
		SHEET_EXTRUSION_DIE = JugcraftRegistry.item("sheet_extrusion_die");
		FILM_EXTRUSION_DIE = JugcraftRegistry.item("film_extrusion_die");
		PROFILE_EXTRUSION_DIE = JugcraftRegistry.item("profile_extrusion_die");
		HOSE_EXTRUSION_DIE = JugcraftRegistry.item("hose_extrusion_die");
		INSULATION_EXTRUSION_DIE = JugcraftRegistry.item("insulation_extrusion_die");
		HDPE_OFFCUT = JugcraftRegistry.item("hdpe_offcut");
		LDPE_OFFCUT = JugcraftRegistry.item("ldpe_offcut");
		PVC_OFFCUT = JugcraftRegistry.item("pvc_offcut");
		RUBBER_OFFCUT = JugcraftRegistry.item("rubber_offcut");

		FLUORITE_CONCENTRATE = JugcraftRegistry.item("fluorite_concentrate");
		CALCIUM_SULFATE = JugcraftRegistry.item("calcium_sulfate");
		PTFE_POWDER = JugcraftRegistry.item("ptfe_powder");
		PTFE_PROCESS_INSERT = JugcraftRegistry.item("ptfe_process_insert");
		PTFE_PASTE_CHARGE = JugcraftRegistry.item("ptfe_paste_charge");
		PTFE_SEAL_BLANK = JugcraftRegistry.item("ptfe_seal_blank");
		PTFE_FITTING_BLANK = JugcraftRegistry.item("ptfe_fitting_blank");
		PTFE_LINER_BLANK = JugcraftRegistry.item("ptfe_liner_blank");
		PTFE_SEAL = JugcraftRegistry.item("ptfe_seal");
		PTFE_FITTING = JugcraftRegistry.item("ptfe_fitting");
		PTFE_LINER = JugcraftRegistry.item("ptfe_liner");
		PTFE_OFFCUT = JugcraftRegistry.item("ptfe_offcut");
		GYPSUM_BINDER = JugcraftRegistry.item("gypsum_binder");

		CLAY_POWDER = JugcraftRegistry.item("clay_powder");
		PORCELAIN_BATCH = JugcraftRegistry.item("porcelain_batch");
		PORCELAIN_CHARGE = JugcraftRegistry.item("porcelain_charge");
		PORCELAIN_BLANK = JugcraftRegistry.item("porcelain_blank");
		FIRED_PORCELAIN_BLANK = JugcraftRegistry.item("fired_porcelain_blank");
		PORCELAIN_INSULATOR = JugcraftRegistry.item("porcelain_insulator");
		ALUMINA_CHARGE = JugcraftRegistry.item("alumina_charge");
		ALUMINA_BLANK = JugcraftRegistry.item("alumina_blank");
		FIRED_ALUMINA_BLANK = JugcraftRegistry.item("fired_alumina_blank");
		ALUMINA_PLATE = JugcraftRegistry.item("alumina_plate");
		CERAMIC_BEARING_SET = JugcraftRegistry.item("ceramic_bearing_set");
		ZIRCON_CONCENTRATE = JugcraftRegistry.item("zircon_concentrate");
		ZIRCONIA_PRECURSOR = JugcraftRegistry.item("zirconia_precursor");
		ZIRCONIA_POWDER = JugcraftRegistry.item("zirconia_powder");
		ZIRCONIA_CHARGE = JugcraftRegistry.item("zirconia_charge");
		ZIRCONIA_BLANK = JugcraftRegistry.item("zirconia_blank");
		FIRED_ZIRCONIA_BLANK = JugcraftRegistry.item("fired_zirconia_blank");
		ZIRCONIA_INSERT = JugcraftRegistry.item("zirconia_insert");
		STEEL_SHAFT_BLANK = JugcraftRegistry.item("steel_shaft_blank");
		PRECISION_SHAFT = JugcraftRegistry.item("precision_shaft");
		PRECISION_BEARING = JugcraftRegistry.item("precision_bearing");
		OPTICAL_BLANK = JugcraftRegistry.item("optical_blank");
		POLISHED_LENS = JugcraftRegistry.item("polished_lens");
		ABRASIVE_HEAD = JugcraftRegistry.item("abrasive_head", properties -> new Item(properties.stacksTo(1)));
		POLISHING_HEAD = JugcraftRegistry.item("polishing_head", properties -> new Item(properties.stacksTo(1)));

		CHROMITE_CONCENTRATE = JugcraftRegistry.item("chromite_concentrate");
		STAINLESS_FITTING = JugcraftRegistry.item("stainless_fitting");
		STAINLESS_VESSEL = JugcraftRegistry.item("stainless_vessel");
		STAINLESS_VALVE_BLANK = JugcraftRegistry.item("stainless_valve_blank");
		PRECISION_STAINLESS_VALVE = JugcraftRegistry.item("precision_stainless_valve");
		LIGHTWEIGHT_FRAME = JugcraftRegistry.item("lightweight_frame");
		ALLOY_PANEL = JugcraftRegistry.item("alloy_panel");
		GALENA_CONCENTRATE = JugcraftRegistry.item("galena_concentrate");
		LEAD_ELECTRODE = JugcraftRegistry.item("lead_electrode");
		LEAD_OXIDE_ELECTRODE = JugcraftRegistry.item("lead_oxide_electrode");
		BATTERY_SEPARATOR = JugcraftRegistry.item("battery_separator");
		LEAD_ACID_CELL = JugcraftRegistry.item("lead_acid_cell");
		ACTIVATED_SPODUMENE = JugcraftRegistry.item("activated_spodumene");
		LITHIUM_FLUORIDE = JugcraftRegistry.item("lithium_fluoride");
		FLUOROPHOSPHATE_PRECURSOR = JugcraftRegistry.item("fluorophosphate_precursor");
		LIPF6_SALT = JugcraftRegistry.item("lipf6_salt");
		CARBONATE_SOLVENT_CHARGE = JugcraftRegistry.item("carbonate_solvent_charge");
		LITHIUM_ELECTROLYTE_CHARGE = JugcraftRegistry.item("lithium_electrolyte_charge");
		LITHIUM_CATHODE = JugcraftRegistry.item("lithium_cathode");
		CARBON_ANODE = JugcraftRegistry.item("carbon_anode");
		VANADYL_SULFATE = JugcraftRegistry.item("vanadyl_sulfate");
		FLOW_CELL_STACK = JugcraftRegistry.item("flow_cell_stack");
		LITHIUM_CELL_ASSEMBLY = JugcraftRegistry.item("lithium_cell_assembly");
		DRY_LEAD_CELL = JugcraftRegistry.item("dry_lead_cell");
		LFP_CATHODE_MIX = JugcraftRegistry.item("lfp_cathode_mix");

		HIGH_PURITY_SILICON = JugcraftRegistry.item("high_purity_silicon");
		HIGH_PURITY_BOULE = JugcraftRegistry.item("high_purity_boule");
		ROUGH_PROCESS_WAFER = JugcraftRegistry.item("rough_process_wafer");
		POLISHED_PROCESS_WAFER = JugcraftRegistry.item("polished_process_wafer");
		CLEAN_PROCESS_WAFER = JugcraftRegistry.item("clean_process_wafer");
		COATED_PROCESS_WAFER = JugcraftRegistry.item("coated_process_wafer");
		EXPOSED_PROCESS_WAFER = JugcraftRegistry.item("exposed_process_wafer");
		DEVELOPED_PROCESS_WAFER = JugcraftRegistry.item("developed_process_wafer");
		ETCHED_PROCESS_WAFER = JugcraftRegistry.item("etched_process_wafer");
		GENERAL_CHIP_PATTERN = JugcraftRegistry.item("general_chip_pattern", properties -> new Item(properties.stacksTo(1)));
		ADVANCED_CHIP = JugcraftRegistry.item("advanced_chip");
		NOVOLAC_RESIN = JugcraftRegistry.item("novolac_resin");
		DNQ_ADDITIVE = JugcraftRegistry.item("dnq_additive");
		DEVELOPER_CHARGE = JugcraftRegistry.item("developer_charge");
		GROUND_PHOSPHATE = JugcraftRegistry.item("ground_phosphate");
		WET_SUPERPHOSPHATE = JugcraftRegistry.item("wet_superphosphate");
		TRIPLE_SUPERPHOSPHATE = JugcraftRegistry.item("triple_superphosphate", properties -> new SpecializedFertilizerItem(properties, SpecializedFertilizerItem.Blend.PHOSPHATE, "triple_superphosphate"));
		NITROGEN_FERTILIZER = JugcraftRegistry.item("nitrogen_fertilizer", properties -> new SpecializedFertilizerItem(properties, SpecializedFertilizerItem.Blend.NITROGEN, "nitrogen_fertilizer"));
		MIXED_FARM_BLEND = JugcraftRegistry.item("mixed_farm_blend", properties -> new SpecializedFertilizerItem(properties, SpecializedFertilizerItem.Blend.MIXED, "mixed_farm_blend"));
		ORGANIC_FERTILIZER_CARRIER = JugcraftRegistry.item("organic_fertilizer_carrier");
		GYPSUM_RICH_RESIDUE = JugcraftRegistry.item("gypsum_rich_residue");
		WASHED_GYPSUM = JugcraftRegistry.item("washed_gypsum");
		COBALT_CONCENTRATE = JugcraftRegistry.item("cobalt_concentrate");
		COBALT_INGOT = JugcraftRegistry.item("cobalt_ingot");
		COBALT_CATALYST_BED = JugcraftRegistry.item("cobalt_catalyst_bed", properties -> new Item(properties.stacksTo(1)));
		STABILIZED_PROCESS_RESIDUE = JugcraftRegistry.item("stabilized_process_residue");
		CRYOGENIC_INSULATION = JugcraftRegistry.item("cryogenic_insulation", DescribedItem::new);
		METHANE_COOLING_MODULE = JugcraftRegistry.item("methane_cooling_module", properties -> new DescribedItem(properties.stacksTo(1)));
		HYDROGEN_COOLING_MODULE = JugcraftRegistry.item("hydrogen_cooling_module", properties -> new DescribedItem(properties.stacksTo(1)));
		METHANE_TANK = JugcraftRegistry.item("methane_tank", DescribedItem::new);
		HYDROGEN_TANK = JugcraftRegistry.item("hydrogen_tank", DescribedItem::new);
		HYDRAZINE_TANK = JugcraftRegistry.item("hydrazine_tank", DescribedItem::new);
		N2O4_TANK = JugcraftRegistry.item("n2o4_tank", DescribedItem::new);
		METHALOX_MOTOR = JugcraftRegistry.item("methalox_motor", DescribedItem::new);
		HYDROLOX_MOTOR = JugcraftRegistry.item("hydrolox_motor", DescribedItem::new);
		HYDRAZINE_THRUSTER = JugcraftRegistry.item("hydrazine_thruster", DescribedItem::new);
		STORABLE_BIPROPELLANT_MOTOR = JugcraftRegistry.item("storable_bipropellant_motor", DescribedItem::new);
		PROPELLANT_RESIDUE = JugcraftRegistry.item("propellant_residue", DescribedItem::new);

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
			output.accept(HIGH_PURITY_SILICON);
			output.accept(HIGH_PURITY_BOULE);
			output.accept(ROUGH_PROCESS_WAFER);
			output.accept(POLISHED_PROCESS_WAFER);
			output.accept(CLEAN_PROCESS_WAFER);
			output.accept(COATED_PROCESS_WAFER);
			output.accept(EXPOSED_PROCESS_WAFER);
			output.accept(DEVELOPED_PROCESS_WAFER);
			output.accept(ETCHED_PROCESS_WAFER);
			output.accept(GENERAL_CHIP_PATTERN);
			output.accept(ADVANCED_CHIP);
			output.accept(CRYOGENIC_INSULATION);
			output.accept(METHANE_COOLING_MODULE);
			output.accept(HYDROGEN_COOLING_MODULE);
			output.accept(METHANE_TANK);
			output.accept(HYDROGEN_TANK);
			output.accept(HYDRAZINE_TANK);
			output.accept(N2O4_TANK);
			output.accept(METHALOX_MOTOR);
			output.accept(HYDROLOX_MOTOR);
			output.accept(HYDRAZINE_THRUSTER);
			output.accept(STORABLE_BIPROPELLANT_MOTOR);
			output.accept(PROPELLANT_RESIDUE);
			output.accept(COBALT_CONCENTRATE);
			output.accept(COBALT_INGOT);
			output.accept(COBALT_CATALYST_BED);
			output.accept(STABILIZED_PROCESS_RESIDUE);
			output.accept(GROUND_PHOSPHATE);
			output.accept(WET_SUPERPHOSPHATE);
			output.accept(TRIPLE_SUPERPHOSPHATE);
			output.accept(NITROGEN_FERTILIZER);
			output.accept(MIXED_FARM_BLEND);
			output.accept(ORGANIC_FERTILIZER_CARRIER);
			output.accept(GYPSUM_RICH_RESIDUE);
			output.accept(WASHED_GYPSUM);

			output.accept(NOVOLAC_RESIN);
			output.accept(DNQ_ADDITIVE);
			output.accept(DEVELOPER_CHARGE);
			output.accept(CHROMITE_CONCENTRATE);
			output.accept(STAINLESS_FITTING);
			output.accept(STAINLESS_VESSEL);
			output.accept(STAINLESS_VALVE_BLANK);
			output.accept(PRECISION_STAINLESS_VALVE);
			output.accept(LIGHTWEIGHT_FRAME);
			output.accept(ALLOY_PANEL);
			output.accept(GALENA_CONCENTRATE);
			output.accept(LEAD_ELECTRODE);
			output.accept(LEAD_OXIDE_ELECTRODE);
			output.accept(BATTERY_SEPARATOR);
			output.accept(LEAD_ACID_CELL);
			output.accept(ACTIVATED_SPODUMENE);
			output.accept(LITHIUM_FLUORIDE);
			output.accept(FLUOROPHOSPHATE_PRECURSOR);
			output.accept(LIPF6_SALT);
			output.accept(CARBONATE_SOLVENT_CHARGE);
			output.accept(LITHIUM_ELECTROLYTE_CHARGE);
			output.accept(LITHIUM_CATHODE);
			output.accept(CARBON_ANODE);
			output.accept(VANADYL_SULFATE);
			output.accept(FLOW_CELL_STACK);
			output.accept(LITHIUM_CELL_ASSEMBLY);
			output.accept(DRY_LEAD_CELL);
			output.accept(LFP_CATHODE_MIX);


			output.accept(CLAY_POWDER);
			output.accept(PORCELAIN_BATCH);
			output.accept(PORCELAIN_CHARGE);
			output.accept(PORCELAIN_BLANK);
			output.accept(FIRED_PORCELAIN_BLANK);
			output.accept(PORCELAIN_INSULATOR);
			output.accept(ALUMINA_CHARGE);
			output.accept(ALUMINA_BLANK);
			output.accept(FIRED_ALUMINA_BLANK);
			output.accept(ALUMINA_PLATE);
			output.accept(CERAMIC_BEARING_SET);
			output.accept(ZIRCON_CONCENTRATE);
			output.accept(ZIRCONIA_PRECURSOR);
			output.accept(ZIRCONIA_POWDER);
			output.accept(ZIRCONIA_CHARGE);
			output.accept(ZIRCONIA_BLANK);
			output.accept(FIRED_ZIRCONIA_BLANK);
			output.accept(ZIRCONIA_INSERT);
			output.accept(STEEL_SHAFT_BLANK);
			output.accept(PRECISION_SHAFT);
			output.accept(PRECISION_BEARING);
			output.accept(OPTICAL_BLANK);
			output.accept(POLISHED_LENS);
			output.accept(ABRASIVE_HEAD);
			output.accept(POLISHING_HEAD);

			output.accept(FLUORITE_CONCENTRATE);
			output.accept(CALCIUM_SULFATE);
			output.accept(PTFE_POWDER);
			output.accept(PTFE_PROCESS_INSERT);
			output.accept(PTFE_PASTE_CHARGE);
			output.accept(PTFE_SEAL_BLANK);
			output.accept(PTFE_FITTING_BLANK);
			output.accept(PTFE_LINER_BLANK);
			output.accept(PTFE_SEAL);
			output.accept(PTFE_FITTING);
			output.accept(PTFE_LINER);
			output.accept(PTFE_OFFCUT);
			output.accept(GYPSUM_BINDER);

			output.accept(HDPE_PELLETS);
			output.accept(LDPE_PELLETS);
			output.accept(LDPE_FILM);
			output.accept(PVC_PROFILE);
			output.accept(HDPE_PROCESS_INSERT);
			output.accept(LDPE_PROCESS_INSERT);
			output.accept(SHEET_EXTRUSION_DIE);
			output.accept(FILM_EXTRUSION_DIE);
			output.accept(PROFILE_EXTRUSION_DIE);
			output.accept(HOSE_EXTRUSION_DIE);
			output.accept(INSULATION_EXTRUSION_DIE);
			output.accept(HDPE_OFFCUT);
			output.accept(LDPE_OFFCUT);
			output.accept(PVC_OFFCUT);
			output.accept(RUBBER_OFFCUT);
			output.accept(MAGNESIUM_HYDROXIDE);
			output.accept(DRY_MAGNESIUM_CHLORIDE);
			output.accept(MAGNESIUM_INGOT);
			output.accept(POLYMER_HOUSING);
			output.accept(POLYMER_FITTING);
			output.accept(CABLE_INSULATION);
			output.accept(FLEXIBLE_HOSE);
			output.accept(POLYMER_VEHICLE_PANEL);
			output.accept(HOUSING_MOLD);
			output.accept(FITTING_MOLD);
			output.accept(INSULATION_MOLD);
			output.accept(GASKET_MOLD);
			output.accept(HOSE_MOLD);
			output.accept(PANEL_MOLD);
			output.accept(NICKEL_CATALYST_BED);
			output.accept(VANADIUM_CONTACT_BED);
			output.accept(VANADIUM_IRON_CONCENTRATE);
			output.accept(VANADIUM_OXIDE);
			output.accept(MIXED_GAS_RESIDUE);
			output.accept(OXIDIZER_BURNER);
			output.accept(DIGESTATE_FIBER);
			output.accept(ALUMINUM_HYDROXIDE);
			output.accept(BAUXITE_RESIDUE);

			output.accept(CRACKING_CATALYST);
			output.accept(ASPHALT_BINDER);
			output.accept(PLASTIC_PELLETS);
			output.accept(PLASTIC_SHEET);
			output.accept(ALUMINA);
			output.accept(FERTILIZER);
			output.accept(TITANIUM_SPONGE);
			output.accept(LITHIUM_CELL);
			output.accept(NEODYMIUM_MAGNET);
			output.accept(SILICON_BOULE);
			output.accept(SILICON_WAFER);
			output.accept(MICROCHIP);
			output.accept(GUNCOTTON);
			output.accept(TURBOCHARGER);
			output.accept(NICKEL_CATALYST_BED);
			output.accept(VANADIUM_PENTOXIDE);
			output.accept(GASIFICATION_RESIDUE);
			output.accept(AMMONIUM_NITRATE);
			output.accept(BORIC_ACID);
			output.accept(CONTROL_ROD_ASSEMBLY);
			output.accept(CITRIC_ACID);
			output.accept(GLUCOSE_SYRUP);
			output.accept(PVAC_GLUE);
			output.accept(BORON_DOPED_SILICON);
			output.accept(SILICONE);
			output.accept(RUTHENIUM_POWDER);
			output.accept(RUTHENIUM_CATALYST_BED);
			output.accept(VANADIUM_CONTACT_BED);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
			output.accept(GRENADE);
			output.accept(GRENADE_LAUNCHER);
		});
	}
}
