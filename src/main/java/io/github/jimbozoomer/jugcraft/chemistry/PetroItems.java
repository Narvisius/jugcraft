package io.github.jimbozoomer.jugcraft.chemistry;

import io.github.jimbozoomer.jugcraft.materials.JugcraftRegistry;
import io.github.jimbozoomer.jugcraft.weapons.GrenadeItem;
import io.github.jimbozoomer.jugcraft.weapons.GrenadeLauncherItem;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

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

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
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
			output.accept(VANADIUM_CONTACT_BED);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
			output.accept(GRENADE);
			output.accept(GRENADE_LAUNCHER);
		});
	}
}
